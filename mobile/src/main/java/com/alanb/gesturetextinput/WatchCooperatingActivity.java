package com.alanb.gesturetextinput;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
    private TextView m_inputText;
    private ArrayList<TextView> m_viewTexts;
    private TouchFeedbackFrameLayout m_feedbackFrameLayout;

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

        m_rootNode = KeyNode.generateKeyTree(this, R.raw.key_value_watch);

        m_gestureTouchAreas = new ArrayList<>();

        m_inputText = (TextView) findViewById(R.id.c_input_text);

        m_viewTexts = new ArrayList<>();
        m_viewTexts.add((TextView) findViewById(R.id.c_char_indi_1));
        m_viewTexts.add((TextView) findViewById(R.id.c_char_indi_2));
        m_viewTexts.add((TextView) findViewById(R.id.c_char_indi_3));
        m_viewTexts.add((TextView) findViewById(R.id.c_char_indi_4));

        updateCurNode(m_rootNode);

        m_feedbackFrameLayout = (TouchFeedbackFrameLayout) findViewById(R.id.c_touch_frame);
        m_feedbackFrameLayout.attachFeedbackTo(m_feedbackFrameLayout);

        GoogleApiClient.Builder g_builder = new GoogleApiClient.Builder(this);
        g_builder.addApi(Wearable.API);
        g_builder.addConnectionCallbacks(connectionCallbacks);
        g_builder.addOnConnectionFailedListener(failedListener);
        mGoogleApiClient = g_builder.build();
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

    private void processTouchEvent(WatchWriteInputView.TouchEvent te)
    {
        if (te == WatchWriteInputView.TouchEvent.END)
        {
            if (m_curNode.getAct() == KeyNode.Act.DELETE)
            {
                Log.d(TAG, "Delete one character");
                CharSequence cs = m_inputText.getText();
                m_inputText.setText(cs.subSequence(0, max(0, cs.length() - 1)));
            }
            else if (m_curNode.getCharVal() != null)
            {
                Log.d(TAG, "Input Result: " + m_curNode.getCharVal());
                m_inputText.setText(m_inputText.getText() + String.valueOf(m_curNode.getCharVal()));
            }

            // initialization for next touch(or gesture) input
            m_gestureTouchAreas.clear();
            updateCurNode(m_rootNode);
        }
        else if (te == WatchWriteInputView.TouchEvent.DROP)
        {
            m_gestureTouchAreas.add(te);
        }
        else if (te == WatchWriteInputView.TouchEvent.MULTITOUCH)
        {
            updateCurNode(m_rootNode);
            m_gestureTouchAreas.clear();
            m_gestureTouchAreas.add(te);
        }
        else if (te != WatchWriteInputView.TouchEvent.AREA_OTHER)
        {
            if (m_gestureTouchAreas.size() <= 0 ||
                    (m_gestureTouchAreas.get(m_gestureTouchAreas.size()-1) != WatchWriteInputView.TouchEvent.DROP &&
                            m_gestureTouchAreas.get(m_gestureTouchAreas.size()-1) != WatchWriteInputView.TouchEvent.MULTITOUCH))
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
                    updateCurNode(next_node);
                    m_gestureTouchAreas.add(te);
                }
                else if (sibling_node != null)
                {
                    updateCurNode(sibling_node);
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

    private void updateCurNode(KeyNode node)
    {
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
                m_viewTexts.get(ci).setText(node.getNextNode(ci).getShowStr());
                m_viewTexts.get(ci).setBackgroundColor(Color.TRANSPARENT);
            }
        }
        this.m_curNode = node;
    }
}
