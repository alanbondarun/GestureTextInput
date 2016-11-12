package com.alanb.gesturetextinput;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class OneDActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();
    private LinearLayout m_touchAreaAll;
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

    public class TouchEvent
    {
        final static int DROP = -1;
        final static int END = -2;
        final static int MULTITOUCH = -3;
        public final int val;
        public TouchEvent(int v)
        {
            if (-3 <= v && v < 4)
            {
                this.val = v;
            }
            else
            {
                this.val = -1;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_1d_input);

        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_pref_key), MODE_PRIVATE);
        int pref_layout = prefs.getInt(getString(R.string.prefkey_oned_layout),
                getResources().getInteger(R.integer.pref_oned_layout_default));
        switch (pref_layout)
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
        m_touchAreaAll = (LinearLayout) findViewById(R.id.o_char_touch);

        m_viewTexts = new ArrayList<>();
        m_viewTexts.add((TextView) findViewById(R.id.o_char_indi_1));
        m_viewTexts.add((TextView) findViewById(R.id.o_char_indi_2));
        m_viewTexts.add((TextView) findViewById(R.id.o_char_indi_3));
        m_viewTexts.add((TextView) findViewById(R.id.o_char_indi_4));

        updateViews(m_rootNode);

        m_touchAreaAll.setOnTouchListener(new View.OnTouchListener() {
            private TouchEvent prev_e = new TouchEvent(TouchEvent.DROP);
            private boolean multi_occurred = false;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                TouchEvent cur_e;
                if (multi_occurred && (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                        || motionEvent.getAction() == MotionEvent.ACTION_MOVE))
                {
                    cur_e = new TouchEvent(TouchEvent.MULTITOUCH);
                }
                else
                {
                    multi_occurred = false;
                    if (motionEvent.getPointerCount() >= 2)
                    {
                        // multi-touch detected, cancel the input
                        multi_occurred = true;
                        cur_e = new TouchEvent(TouchEvent.MULTITOUCH);
                    }
                    else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                            || motionEvent.getAction() == MotionEvent.ACTION_MOVE)
                    {
                        double xrel = motionEvent.getX() / view.getWidth();
                        double yrel = motionEvent.getY() / view.getHeight();
                        if (0 <= xrel && xrel <= 1 && 0 <= yrel && yrel <= 1)
                        {
                            cur_e = new TouchEvent((int) (xrel * 4.0));
                        }
                        else
                        {
                            cur_e = new TouchEvent(TouchEvent.DROP);
                        }
                    }
                    else
                    {
                        cur_e = new TouchEvent(TouchEvent.END);
                    }
                }
                if (cur_e.val != prev_e.val)
                {
                    OneDActivity.this.processTouch(cur_e);
                    prev_e = cur_e;
                }

                return true;
            }
        });

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

        initTask();
    }

    private void initTask()
    {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_pref_key), MODE_PRIVATE);
        m_taskMode = prefs.getInt(getString(R.string.prefkey_task_mode),
                getResources().getInteger(R.integer.pref_task_mode_default)) == 0;

        m_taskTextView = (TextView) findViewById(R.id.o_task_text);
        if (m_taskMode)
        {
            m_taskLoader = new TaskPhraseLoader(this);
            prepareTask();
        }
        else
        {
            m_taskTextView.setVisibility(View.INVISIBLE);
        }
    }

    private void prepareTask()
    {
        m_taskStr = m_taskLoader.next();
        m_taskTextView.setText(m_taskStr);
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
                Log.d(TAG, builder.toString());
                m_viewTexts.get(ci).setText(builder.toString());
                m_viewTexts.get(ci).setBackgroundColor(Color.TRANSPARENT);
            }
        }
        m_curNode = node;
    }

    public void processTouch(TouchEvent te)
    {
        if (te.val == TouchEvent.END)
        {
            if (m_curNode != m_rootNode && m_curNode != null)
            {
                if (m_curNode.getAct() == KeyNode.Act.DELETE)
                {
                    Log.d(TAG, "Delete one character");
                    m_inputStr = m_inputStr.substring(0, max(0, m_inputStr.length()-1));
                }
                else if (m_curNode.getCharVal() != null)
                {
                    Log.d(TAG, "input char = " + m_curNode.getCharVal());
                    m_inputStr += String.valueOf(m_curNode.getCharVal());
                }
            }
            updateViews(m_rootNode);
            m_touchArray.clear();
        }
        else if (te.val == TouchEvent.DROP)
        {
            m_touchArray.add(te);
        }
        else if (te.val == TouchEvent.MULTITOUCH)
        {
            updateViews(m_rootNode);
            m_touchArray.clear();
            m_touchArray.add(te);
        }
        else
        {
            KeyNode next_node = null;
            if (m_touchArray.size() <= 0 || m_touchArray.get(m_touchArray.size()-1).val >= 0)
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
            }

            KeyNode sibling_node = m_curNode.getParent();
            if (sibling_node != null)
            {
                sibling_node = sibling_node.getNextNode(te.val);
            }

            if (next_node != null)
            {
                updateViews(next_node);
                m_touchArray.add(te);
            }
            else if (sibling_node != null)
            {
                updateViews(sibling_node);
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
