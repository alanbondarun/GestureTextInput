package com.alanb.gesturetextinput;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.alanb.gesturecommon.WatchWriteInputView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

import static java.lang.Math.max;

public class WatchCooperatingActivity extends AppCompatActivity
{
    private final String TAG = this.getClass().getName();
    private GoogleApiClient mGoogleApiClient;

    private ArrayList<WatchWriteInputView.TouchEvent> m_gestureTouchAreas;
    private KeyNode m_rootNode;
    // DO NOT modify this directly; use updateCurNode() instead
    private KeyNode m_curNode;

    private int m_pref_layout;

    private String m_inputStr = "";
    private TextView m_inputTextView;
    private ArrayList<TextView> m_viewTexts;
    private TouchFeedbackFrameLayout m_feedbackFrameLayout;

    private boolean m_taskMode;
    private TaskPhraseLoader m_taskLoader;
    private TextView m_taskTextView;
    private String m_taskStr = null;

    private int m_inc_fixed_num = 0;
    private int m_fix_num = 0;
    private int m_canceled_num = 0;

    private NanoTimer m_phraseTimer;
    private TaskRecordWriter m_taskRecordWriter = null;
    private ArrayList<TaskRecordWriter.TimedAction> m_timedActions;

    private GoogleApiClient.ConnectionCallbacks connectionCallbacks =
            new GoogleApiClient.ConnectionCallbacks()
    {
        @Override
        public void onConnected(@Nullable Bundle bundle)
        {
            Log.d(TAG, "connected");
            Wearable.DataApi.addListener(mGoogleApiClient, dataListener);
        }

        @Override
        public void onConnectionSuspended(int i)
        {
            Log.d(TAG, "connection suspended");
        }
    };

    private GoogleApiClient.OnConnectionFailedListener failedListener =
            new GoogleApiClient.OnConnectionFailedListener()
    {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
        {
            Log.d(TAG, "connection failed");
        }
    };

    private DataApi.DataListener dataListener = new DataApi.DataListener()
    {
        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer)
        {
            for (DataEvent event: dataEventBuffer)
            {
                if (event.getType() == DataEvent.TYPE_CHANGED)
                {
                    DataItem item = event.getDataItem();
                    if (item.getUri().getPath().compareTo("/touchevent") == 0)
                    {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                        String touch_str = dataMap.getString(getResources().getString(R.string.wear_touch_key));
                        Log.d(TAG, "Touch Received = " + touch_str);
                        processTouchEvent(WatchWriteInputView.TouchEvent.valueOf(touch_str));
                    }
                    else if (item.getUri().getPath().compareTo("/touchpos") == 0)
                    {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                        float xpos = dataMap.getFloat(getResources().getString(R.string.wear_xpos_key));
                        float ypos = dataMap.getFloat(getResources().getString(R.string.wear_ypos_key));
                        int action = dataMap.getInt(getResources().getString(R.string.wear_action_key));
                        m_feedbackFrameLayout.setCursor(xpos * m_feedbackFrameLayout.getWidth(),
                                ypos * m_feedbackFrameLayout.getHeight(), action);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_coop);

        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_pref_key), MODE_PRIVATE);
        m_pref_layout = prefs.getInt(getString(R.string.prefkey_watch_layout),
                getResources().getInteger(R.integer.pref_watch_layout_default));
        switch (m_pref_layout)
        {
            case 0:
                m_rootNode = KeyNode.generateKeyTree(this, R.raw.key_value_watch_2area);
                break;
            case 1:
                m_rootNode = KeyNode.generateKeyTree(this, R.raw.key_value_watch_3area);
                break;
            case 2:
                m_rootNode = KeyNode.generateKeyTree(this, R.raw.key_value_watch_3area_opt);
                break;
            case 3:
                m_rootNode = KeyNode.generateKeyTree(this, R.raw.key_value_watch_3area_opt_2);
                break;
        }

        m_gestureTouchAreas = new ArrayList<>();

        m_inputTextView = (TextView) findViewById(R.id.c_input_text);

        m_viewTexts = new ArrayList<>();
        m_viewTexts.add((TextView) findViewById(R.id.c_char_indi_1));
        m_viewTexts.add((TextView) findViewById(R.id.c_char_indi_2));
        m_viewTexts.add((TextView) findViewById(R.id.c_char_indi_3));
        m_viewTexts.add((TextView) findViewById(R.id.c_char_indi_4));

        updateViews(m_rootNode);

        m_feedbackFrameLayout = (TouchFeedbackFrameLayout) findViewById(R.id.c_touch_frame);
        m_feedbackFrameLayout.attachFeedbackTo(m_feedbackFrameLayout);

        GoogleApiClient.Builder g_builder = new GoogleApiClient.Builder(this);
        g_builder.addApi(Wearable.API);
        g_builder.addConnectionCallbacks(connectionCallbacks);
        g_builder.addOnConnectionFailedListener(failedListener);
        mGoogleApiClient = g_builder.build();

        m_phraseTimer = new NanoTimer();
        initTask();
    }

    private void initTask()
    {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_pref_key), MODE_PRIVATE);
        m_taskMode = prefs.getInt(getString(R.string.prefkey_task_mode),
                getResources().getInteger(R.integer.pref_task_mode_default)) == 0;
        m_timedActions = new ArrayList<>();

        m_taskTextView = (TextView) findViewById(R.id.c_task_text);
        if (m_taskMode)
        {
            m_taskLoader = new TaskPhraseLoader(this);
            try
            {
                m_taskRecordWriter = new TaskRecordWriter(this, this.getClass());
            }
            catch (java.io.IOException e)
            {
                e.printStackTrace();
            }
            prepareTask();
        }
        else
        {
            m_taskTextView.setVisibility(View.INVISIBLE);
        }
    }

    private void prepareTask()
    {
        if (!m_taskMode)
            return;
        m_taskStr = m_taskLoader.next();
        m_taskTextView.setText(m_taskStr);
        m_inc_fixed_num = 0;
        m_fix_num = 0;
        m_canceled_num = 0;
        m_timedActions.clear();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause()
    {
        Wearable.DataApi.removeListener(mGoogleApiClient, dataListener);
        mGoogleApiClient.disconnect();
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (m_taskRecordWriter != null)
        {
            m_taskRecordWriter.close();
        }
    }

    private void processTouchEvent(WatchWriteInputView.TouchEvent te)
    {
        if (te == WatchWriteInputView.TouchEvent.END)
        {
            if (!m_phraseTimer.running())
                m_phraseTimer.begin();
            if (m_curNode.getAct() == KeyNode.Act.DELETE)
            {
                m_phraseTimer.check();
                Log.d(TAG, "Delete one character");
                m_inputStr = m_inputStr.substring(0, max(0, m_inputStr.length()-1));
                m_inc_fixed_num++;
                m_fix_num++;
                m_timedActions.add(new TaskRecordWriter.TimedAction(m_phraseTimer.getDiffInSeconds(), "del"));
            }
            else if (m_curNode.getAct() == KeyNode.Act.DONE)
            {
                Log.d(TAG, "Input Done");
                doneTask();
            }
            else if (m_curNode.getCharVal() != null)
            {
                m_phraseTimer.check();
                Log.d(TAG, "Input Result: " + m_curNode.getCharVal());
                m_inputStr += String.valueOf(m_curNode.getCharVal());
                m_timedActions.add(new TaskRecordWriter.TimedAction(m_phraseTimer.getDiffInSeconds(), m_curNode.getCharVal().toString()));
            }
            else
            {
                m_phraseTimer.check();
                m_timedActions.add(new TaskRecordWriter.TimedAction(m_phraseTimer.getDiffInSeconds(), "cancel"));
                m_canceled_num++;
            }

            // initialization for next touch(or gesture) input
            m_gestureTouchAreas.clear();
            updateViews(m_rootNode);
        }
        else if (te == WatchWriteInputView.TouchEvent.DROP)
        {
            m_gestureTouchAreas.add(te);
        }
        else if (te == WatchWriteInputView.TouchEvent.MULTITOUCH)
        {
            updateViews(m_rootNode);
            m_gestureTouchAreas.clear();
            m_gestureTouchAreas.add(te);
        }
        else if (te != WatchWriteInputView.TouchEvent.AREA_OTHER)
        {
            if (isValidTouchSequence(m_gestureTouchAreas))
            {
                KeyNode next_node = null;
                KeyNode sibling_node = m_curNode.getParent();
                switch (te)
                {
                    case AREA1:
                        next_node = m_curNode.getNextNode(0);
                        if (sibling_node != null)
                            sibling_node = sibling_node.getNextNode(0);
                        break;
                    case AREA2:
                        next_node = m_curNode.getNextNode(1);
                        if (sibling_node != null)
                            sibling_node = sibling_node.getNextNode(1);
                        break;
                    case AREA3:
                        next_node = m_curNode.getNextNode(2);
                        if (sibling_node != null)
                            sibling_node = sibling_node.getNextNode(2);
                        break;
                    case AREA4:
                        next_node = m_curNode.getNextNode(3);
                        if (sibling_node != null)
                            sibling_node = sibling_node.getNextNode(3);
                        break;
                }
                if (next_node != null)
                {
                    updateViews(next_node);
                    m_gestureTouchAreas.add(te);
                }
                else if (sibling_node != null)
                {
                    updateViews(sibling_node);
                    if (m_gestureTouchAreas.size() >= 1)
                        m_gestureTouchAreas.remove(m_gestureTouchAreas.size()-1);
                    m_gestureTouchAreas.add(te);
                }
                else
                {
                    m_gestureTouchAreas.add(WatchWriteInputView.TouchEvent.DROP);
                    Log.d(TAG, "Touch drop: end reached");
                }
            }
        }
    }

    private boolean isValidTouchSequence(ArrayList<WatchWriteInputView.TouchEvent> events)
    {
        if (events.size() <= 0 ||
                (events.get(events.size()-1) != WatchWriteInputView.TouchEvent.DROP &&
                        events.get(events.size()-1) != WatchWriteInputView.TouchEvent.MULTITOUCH))
            return true;
        return (events.size() == 1 && events.get(0) == WatchWriteInputView.TouchEvent.DROP);
    }

    private void doneTask()
    {
        if (!m_taskMode)
            return;

        EditDistCalculator.EditInfo info = EditDistCalculator.calc(m_taskStr, m_inputStr);

        double time_before_last = m_phraseTimer.getDiffInSeconds();
        m_phraseTimer.check();
        m_timedActions.add(new TaskRecordWriter.TimedAction(m_phraseTimer.getDiffInSeconds(), "done"));
        m_phraseTimer.end();

        if (m_taskRecordWriter != null)
        {
            m_taskRecordWriter.write(m_taskRecordWriter.new InfoBuilder()
                    .setInputTime(time_before_last)
                    .setInputStr(m_inputStr)
                    .setPresentedStr(m_taskStr)
                    .setLayoutNum(m_pref_layout)
                    .setNumC(info.num_correct)
                    .setNumIf(m_inc_fixed_num)
                    .setNumF(m_fix_num)
                    .setNumInf(info.num_delete + info.num_insert+ info.num_modify)
                    .setNumCancel(m_canceled_num)
                    .setTimedActions(m_timedActions));
        }

        m_inputStr = "";
        prepareTask();
    }

    private void updateViews(KeyNode node)
    {
        m_inputTextView.setText(m_inputStr + getString(R.string.end_of_input));
        if (node.isLeaf())
        {
            KeyNode np = node.getParent();
            if (np != null)
            {
                for (int ci=0; ci < np.getNextNodeNum(); ci++)
                {
                    if (np.getNextNode(ci) == node)
                    {
                        m_viewTexts.get(ci).setBackgroundColor(ContextCompat.getColor(
                                getApplicationContext(), R.color.colorTouchBackground));
                    }
                    else
                    {
                        m_viewTexts.get(ci).setBackgroundColor(Color.TRANSPARENT);
                    }
                }
            }
        }
        else
        {
            for (int ci = 0; ci < 4; ci++)
            {
                String raw_str = node.getNextNode(ci).getShowStr();
                StringBuilder builder = new StringBuilder();

                int line_chars = 3;
                if (node.getNextNode(ci).getCharVal() == null)
                    line_chars = 5;
                for (int cj = 0; cj < raw_str.length(); cj += line_chars)
                {
                    if (cj > 0)
                        builder.append("\n");
                    builder.append(raw_str.substring(cj, Math.min(cj+line_chars, raw_str.length())));
                }
                m_viewTexts.get(ci).setText(builder.toString());
                m_viewTexts.get(ci).setBackgroundColor(Color.TRANSPARENT);
            }
        }
        this.m_curNode = node;
    }
}
