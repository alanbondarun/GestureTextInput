package com.alanb.gesturetextinput;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.alanb.gesturecommon.EditDistCalculator;
import com.alanb.gesturecommon.KeyNode;
import com.alanb.gesturecommon.MathUtils;
import com.alanb.gesturecommon.NanoTimer;
import com.alanb.gesturecommon.OneDInputView;
import com.alanb.gesturecommon.TaskPhraseLoader;
import com.alanb.gesturecommon.TaskRecordWriter;
import com.alanb.gesturecommon.TouchFeedbackFrameLayout;
import com.alanb.gesturecommon.OneDInputView.TouchEvent;

import java.util.ArrayList;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class OneDActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();
    private ArrayList<TextView> m_viewTexts;
    private KeyNode m_rootNode;
    // DO NOT modify this directly; use updateCurNode() instead
    private KeyNode m_curNode;
    private ArrayList<TouchEvent> m_touchArray;

    private String m_inputStr = "";
    private TextView m_inputTextView;
    private final boolean upperTouchFeedback = true;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_1d_input);

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

        m_touchArray = new ArrayList<>();

        m_inputTextView = (TextView) findViewById(R.id.o_input_text);

        m_viewTexts = new ArrayList<>();
        m_viewTexts.add((TextView) findViewById(R.id.o_char_indi_1));
        m_viewTexts.add((TextView) findViewById(R.id.o_char_indi_2));
        m_viewTexts.add((TextView) findViewById(R.id.o_char_indi_3));
        m_viewTexts.add((TextView) findViewById(R.id.o_char_indi_4));

        updateNode(m_rootNode, true);

        TouchFeedbackFrameLayout feedbackFrameLayout = (TouchFeedbackFrameLayout)
                findViewById(R.id.o_touch_point_area);
        if (upperTouchFeedback)
        {
            FrameLayout upperFrame = (FrameLayout) findViewById(R.id.o_upper_frame);
            feedbackFrameLayout.attachFeedbackTo(upperFrame);
        }
        else
        {
            feedbackFrameLayout.attachFeedbackTo(feedbackFrameLayout);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        OneDInputView inputView = (OneDInputView) inflater.inflate(R.layout.oned_touch_area, feedbackFrameLayout, false);
        inputView.setOnTouchEventListener(m_onedTouchEventListener);
        inputView.setOnTouchListener(m_onedTouchListener);
        feedbackFrameLayout.addView(inputView);

        m_phraseTimer = new NanoTimer();
        initTask();
    }

    private void initTask()
    {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_pref_key), MODE_PRIVATE);
        m_taskMode = prefs.getInt(getString(R.string.prefkey_task_mode),
                getResources().getInteger(R.integer.pref_task_mode_default)) == 0;
        m_timedActions = new ArrayList<>();

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
            // update only for non-leaf node
            for (int ci=0; ci<min(node.getNextNodeNum(), 4); ci++)
            {
                String raw_str = node.getNextNode(ci).getShowStr();
                StringBuilder builder = new StringBuilder();
                for (int cj = 0; cj < raw_str.length(); cj += MAX_CHAR_PER_LINE)
                {
                    if (cj > 0)
                        builder.append("\n");
                    builder.append(raw_str.substring(cj,
                            Math.min(cj + MAX_CHAR_PER_LINE, raw_str.length())));
                }
                m_viewTexts.get(ci).setText(builder.toString());
                m_viewTexts.get(ci).setBackgroundColor(Color.TRANSPARENT);
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
        final double stopSpeedThresh = 0.5;
        final double angleThresh = Math.PI * 0.35;
        int m_dir = 0;
        @Override
        public void onTouch(MotionEvent e)
        {
            int lastIndex = e.getHistorySize() - 1;
            if (e.getAction() == MotionEvent.ACTION_MOVE && lastIndex >= 0)
            {
                if ((Math.hypot(e.getX() - e.getHistoricalX(lastIndex), e.getY() - e.getHistoricalY(lastIndex)) /
                        (e.getEventTime() - e.getHistoricalEventTime(lastIndex))) <= stopSpeedThresh)
                {
                    updateViews();
                }
                else
                {
                    int cdir = 0;
                    double aa = MathUtils.vectorAngle(e.getX() - e.getHistoricalX(lastIndex),
                            e.getY() - e.getHistoricalY(lastIndex), 1, 0);
                    if (Math.abs(aa) <= angleThresh)
                    {
                        cdir = 1;
                    }
                    else if (Math.abs(Math.PI - aa) <= angleThresh)
                    {
                        cdir = -1;
                    }

                    if (cdir*m_dir < 0)
                    {
                        updateViews();
                    }
                    m_dir = cdir;
                }
            }
            else
            {
                m_dir = 0;
            }
        }
    };

    public OneDInputView.OnTouchEventListener m_onedTouchEventListener =
            new OneDInputView.OnTouchEventListener()
    {
        @Override
        public void onTouchEvent(TouchEvent te)
        {
            if (te.val == TouchEvent.END)
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
            else if (te.val == TouchEvent.DROP)
            {
                m_touchArray.add(te);
            }
            else if (te.val == TouchEvent.MULTITOUCH)
            {
                updateNode(m_rootNode, true);
                m_touchArray.clear();
                m_touchArray.add(te);
            }
            else
            {
                KeyNode next_node = null;
                if (isValidTouchSequence(m_touchArray))
                {
                    Log.d(TAG, "Touch = " + te.val);
                    if (m_touchArray.size() >= 2)
                    {
                        int dx1 = te.val - m_touchArray.get(m_touchArray.size()-1).val;
                        int dx2 = m_touchArray.get(m_touchArray.size()-1).val - m_touchArray.get(m_touchArray.size()-2).val;
                        if (dx1/abs(dx1) == dx2/abs(dx2))
                        {
                            next_node = m_curNode.getParent().getNextNode(te.val);
                        }
                        else
                        {
                            next_node = m_curNode.getNextNode(te.val);
                        }
                    }
                    else
                    {
                        next_node = m_curNode.getNextNode(te.val);
                    }

                    KeyNode sibling_node = m_curNode.getParent();
                    if (sibling_node != null)
                    {
                        sibling_node = sibling_node.getNextNode(te.val);
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
                        m_touchArray.add(new TouchEvent(TouchEvent.DROP));
                        Log.d(TAG, "Touch drop: end reached");
                    }
                }
            }
        }
    };

    private boolean isValidTouchSequence(ArrayList<TouchEvent> events)
    {
        if (events.size() <= 0 ||
                (events.get(events.size()-1).val != TouchEvent.DROP &&
                        events.get(events.size()-1).val != TouchEvent.MULTITOUCH))
            return true;
        if (events.size() == 1 && events.get(0).val == TouchEvent.DROP)
        {
            events.clear();
            return true;
        }
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
    }
}
