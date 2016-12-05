package com.alanb.gesturetextinput;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.alanb.gesturecommon.EditDistCalculator;
import com.alanb.gesturecommon.KeyNode;
import com.alanb.gesturecommon.MathUtils;
import com.alanb.gesturecommon.MotionEventRecorder;
import com.alanb.gesturecommon.NanoTimer;
import com.alanb.gesturecommon.OneDInputView;
import com.alanb.gesturecommon.TaskPhraseLoader;
import com.alanb.gesturecommon.TaskRecordWriter;
import com.alanb.gesturecommon.TouchEvent;
import com.alanb.gesturecommon.TouchFeedbackFrameLayout;
import com.alanb.gesturecommon.WatchWriteInputView;
import com.google.android.glass.widget.CardBuilder;

import java.util.ArrayList;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class GlassOneDActivity extends Activity
{
    private final String TAG = this.getClass().getName();
    private ArrayList<TextView> m_viewTexts;
    private KeyNode m_rootNode;
    // DO NOT modify this directly; use updateCurNode() instead
    private KeyNode m_curNode;
    private ArrayList<TouchEvent> m_touchArray;
    private TouchFeedbackFrameLayout m_feedbackFrameLayout;

    private String m_inputStr = "";
    private TextView m_inputTextView;
    private final int MAX_CHAR_PER_LINE = 5;

    private boolean m_taskMode;
    private TaskPhraseLoader m_taskLoader;
    private TextView m_taskTextView;
    private String m_taskStr = null;

    private int m_inc_fixed_num = 0;
    private int m_fix_num = 0;
    private int m_canceled_num = 0;

    private NanoTimer m_phraseTimer;
    private int m_pref_layout;
    private TaskRecordWriter m_taskRecordWriter = null;
    private ArrayList<TaskRecordWriter.TimedAction> m_timedActions;

    private MotionEventRecorder m_motionRecorder;

    @Override
    protected void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);

        View mView = buildView();
        setContentView(mView);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_pref_key), MODE_PRIVATE);
        m_pref_layout = prefs.getInt(getString(R.string.prefkey_oned_layout),
                getResources().getInteger(R.integer.pref_oned_layout_default));
        switch (m_pref_layout)
        {
            case 0:
                m_rootNode = KeyNode.generateKeyTree(this, R.raw.key_value_oned);
                break;
            case 1:
                m_rootNode = KeyNode.generateKeyTree(this, R.raw.key_value_oned_opt);
                break;
        }

        m_touchArray = new ArrayList<TouchEvent>();

        m_inputTextView = (TextView) findViewById(R.id.o_input_text);

        m_feedbackFrameLayout = (TouchFeedbackFrameLayout)
                findViewById(R.id.o_touch_point_area);
        m_feedbackFrameLayout.attachFeedbackTo(m_feedbackFrameLayout);

        LayoutInflater inflater = LayoutInflater.from(this);
        OneDInputView inputView = (GlassOneDInputView) inflater.inflate(R.layout.glass_oned_touch_area,
                m_feedbackFrameLayout, false);
        inputView.setOnTouchEventListener(m_onedTouchEventListener);
        inputView.setOnTouchListener(m_onedTouchListener);
        m_feedbackFrameLayout.addView(inputView);
        m_feedbackFrameLayout.getFeedbackView().setPointColor(Color.argb(80, 255, 255, 255));
        m_feedbackFrameLayout.getFeedbackView().setRadius(20.0f);

        m_viewTexts = new ArrayList<TextView>();
        m_viewTexts.add((TextView) findViewById(R.id.o_char_indi_1));
        m_viewTexts.add((TextView) findViewById(R.id.o_char_indi_2));
        m_viewTexts.add((TextView) findViewById(R.id.o_char_indi_3));
        m_viewTexts.add((TextView) findViewById(R.id.o_char_indi_4));

        updateNode(m_rootNode, true);

        m_phraseTimer = new NanoTimer();
        initTask();

        try
        {
            m_motionRecorder = new MotionEventRecorder(this, this.getClass());
        }
        catch (java.io.IOException e)
        {
            e.printStackTrace();
        }
    }

    private void initTask()
    {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_pref_key), MODE_PRIVATE);
        m_taskMode = prefs.getInt(getString(R.string.prefkey_task_mode),
                getResources().getInteger(R.integer.pref_task_mode_default)) == 0;
        m_timedActions = new ArrayList<TaskRecordWriter.TimedAction>();

        m_taskTextView = (TextView) findViewById(R.id.o_task_text);
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

    public void updateNode(KeyNode node, boolean refreshView)
    {
        m_curNode = node;
        if (refreshView)
            updateViews(node);
    }

    public void updateViews()
    {
        updateViews(m_curNode);
    }

    public void updateViews(KeyNode node)
    {
        m_inputTextView.setText(m_inputStr + getString(R.string.end_of_input));

        if (!node.isLeaf())
        {
            for (int ci = 0; ci < 4; ci++)
            {
                String raw_str = node.getNextNode(ci).getShowStr();
                m_viewTexts.get(ci).setText(raw_str);
            }
        }

        KeyNode np = node.getParent();
        for (int ci=0; ci < 4; ci++)
        {
            if (ci % 2 == 0)
            {
                if (np != null && np.getNextNode(ci) == node)
                    m_viewTexts.get(ci).setBackgroundColor(getResources().getColor(R.color.colorGlassBackground));
                else
                    m_viewTexts.get(ci).setBackgroundColor(getResources().getColor(R.color.colorGlassBackgroundWeak));
            }
            else
            {
                if (np != null && np.getNextNode(ci) == node)
                    m_viewTexts.get(ci).setBackgroundColor(getResources().getColor(R.color.colorGlassPink));
                else
                    m_viewTexts.get(ci).setBackgroundColor(getResources().getColor(R.color.colorGlassPinkWeak));
            }
        }
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
                    .setNumInf(info.num_delete + info.num_insert + info.num_modify)
                    .setNumCancel(m_canceled_num)
                    .setTimedActions(m_timedActions));
        }

        m_inputStr = "";
        prepareTask();
    }

    public OneDInputView.OnTouchListener m_onedTouchListener = new OneDInputView.OnTouchListener()
    {
        @Override
        public void onTouch(MotionEvent e)
        {
            m_feedbackFrameLayout.setCursorRatio(e.getX()/getResources().getInteger(R.integer.glass_touchpad_w),
                    e.getY()/getResources().getInteger(R.integer.glass_touchpad_h), e.getAction());
            if (m_motionRecorder != null)
                m_motionRecorder.write(e);
        }
    };

    public OneDInputView.OnTouchEventListener m_onedTouchEventListener =
            new OneDInputView.OnTouchEventListener()
            {
                @Override
                public void onTouchEvent(TouchEvent te)
                {
                    Log.d(TAG, "event: " + te.name());
                    if (te == TouchEvent.END)
                    {
                        if (!m_phraseTimer.running())
                            m_phraseTimer.begin();
                        if (m_curNode != m_rootNode && m_curNode != null)
                        {
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
                                Log.d(TAG, "input char = " + m_curNode.getCharVal());
                                m_inputStr += String.valueOf(m_curNode.getCharVal());
                                m_timedActions.add(new TaskRecordWriter.TimedAction(m_phraseTimer.getDiffInSeconds(), m_curNode.getCharVal().toString()));
                            }
                            else
                            {
                                m_phraseTimer.check();
                                m_timedActions.add(new TaskRecordWriter.TimedAction(m_phraseTimer.getDiffInSeconds(), "cancel"));
                                m_canceled_num++;
                            }
                        }
                        else
                        {
                            m_phraseTimer.check();
                            m_timedActions.add(new TaskRecordWriter.TimedAction(m_phraseTimer.getDiffInSeconds(), "cancel"));
                            m_canceled_num++;
                        }
                        updateNode(m_rootNode, true);
                        m_touchArray.clear();
                    }
                    else if (te == TouchEvent.DROP)
                    {
                        updateNode(m_rootNode, true);
                        m_touchArray.clear();
                        m_touchArray.add(te);
                    }
                    else if (te == TouchEvent.MULTITOUCH)
                    {
                        updateNode(m_rootNode, true);
                        m_touchArray.clear();
                        m_touchArray.add(te);
                    }
                    else if (te != TouchEvent.AREA_OTHER)
                    {
                        KeyNode next_node = null;
                        if (isValidTouchSequence(m_touchArray) && (m_touchArray.size() <= 0 || m_touchArray.get(m_touchArray.size()-1) != te))
                        {
                            Log.d(TAG, "Touch = " + te);
                            if (m_touchArray.size() >= 2)
                            {
                                int dx1 = te.ordinal() - m_touchArray.get(m_touchArray.size()-1).ordinal();
                                int dx2 = m_touchArray.get(m_touchArray.size()-1).ordinal() - m_touchArray.get(m_touchArray.size()-2).ordinal();
                                if (dx1/abs(dx1) == dx2/abs(dx2))
                                {
                                    next_node = m_curNode.getParent().getNextNode(te.ordinal());
                                }
                                else
                                {
                                    next_node = m_curNode.getNextNode(te.ordinal());
                                }
                            }
                            else
                            {
                                next_node = m_curNode.getNextNode(te.ordinal());
                            }

                            KeyNode sibling_node = m_curNode.getParent();
                            if (sibling_node != null)
                            {
                                sibling_node = sibling_node.getNextNode(te.ordinal());
                            }

                            if (next_node != null)
                            {
                                updateNode(next_node, false);
                                m_touchArray.add(te);
                            }
                            else if (sibling_node != null)
                            {
                                updateNode(sibling_node, true);
                                m_touchArray.add(te);
                            }
                            else
                            {
                                m_touchArray.add(TouchEvent.DROP);
                                Log.d(TAG, "Touch drop: end reached");
                            }
                        }
                    }
                    updateViews();
                }
            };

    private boolean isValidTouchSequence(ArrayList<TouchEvent> events)
    {
        if (events.size() <= 0 ||
                (events.get(events.size()-1) != TouchEvent.DROP &&
                        events.get(events.size()-1) != TouchEvent.MULTITOUCH))
            return true;
        return false;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (m_taskRecordWriter != null)
        {
            m_taskRecordWriter.close();
        }
        if (m_motionRecorder != null)
        {
            m_motionRecorder.close();
        }
    }

    private View buildView()
    {
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE);
        card.setEmbeddedLayout(R.layout.glass_oned_layout);
        return card.getView();
    }
}
