package com.alanb.gesturetextinput;

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
    private KeyNode m_rootNode, m_curNode;
    private ArrayList<TouchEvent> m_touchArray;
    private TextView m_inputText;
    private final boolean upperTouchFeedback = true;

    public class TouchEvent
    {
        final static int DROP = -1;
        final static int END = -2;
        public final int val;
        public TouchEvent(int v)
        {
            if (-2 <= v && v < 4)
            {
                this.val = v;
            }
            else
            {
                this.val = -2;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_1d_input);

        m_rootNode = KeyNode.generateKeyTree(this, R.raw.key_value_oned);
        m_curNode = m_rootNode;
        m_touchArray = new ArrayList<>();

        m_inputText = (TextView) findViewById(R.id.o_input_text);
        m_touchAreaAll = (LinearLayout) findViewById(R.id.o_char_touch);

        m_viewTexts = new ArrayList<>();
        m_viewTexts.add((TextView) findViewById(R.id.o_char_indi_1));
        m_viewTexts.add((TextView) findViewById(R.id.o_char_indi_2));
        m_viewTexts.add((TextView) findViewById(R.id.o_char_indi_3));
        m_viewTexts.add((TextView) findViewById(R.id.o_char_indi_4));

        for (int ci=0; ci<4; ci++)
        {
            m_viewTexts.get(ci).setText(m_rootNode.getNextNode(ci).getShowStr());
        }

        m_touchAreaAll.setOnTouchListener(new View.OnTouchListener() {
            private TouchEvent prev_e = new TouchEvent(TouchEvent.DROP);
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                TouchEvent cur_e;
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                        || motionEvent.getAction() == MotionEvent.ACTION_MOVE)
                {
                    double xrel = motionEvent.getX() / view.getWidth();
                    double yrel = motionEvent.getY() / view.getHeight();
                    if (0 <= xrel && xrel <= 1 && 0 <= yrel && yrel <= 1)
                    {
                        cur_e = new TouchEvent((int)(xrel * 4.0));
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
                if (cur_e.val != prev_e.val)
                {
                    OneDActivity.this.processTouch(cur_e);
                    prev_e = cur_e;
                }

                Log.d(TAG, "touch area onTouch");
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
    }

    public void updateShowText()
    {
        if (m_curNode.isLeaf())
        {
            KeyNode np = m_curNode.getParent();
            if (np != null)
            {
                for (int ci=0; ci < np.getNextNodeNum(); ci++)
                {
                    if (np.getNextNode(ci) == m_curNode)
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
            for (int ci=0; ci<min(m_curNode.getNextNodeNum(), 4); ci++)
            {
                m_viewTexts.get(ci).setText(m_curNode.getNextNode(ci).getShowStr());
                m_viewTexts.get(ci).setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }

    public void processTouch(TouchEvent te)
    {
        if (te.val == TouchEvent.DROP || te.val == TouchEvent.END)
        {
            if (m_curNode != m_rootNode && m_curNode != null)
            {
                if (m_curNode.getAct() == KeyNode.Act.DELETE)
                {
                    Log.d(TAG, "Delete one character");
                    CharSequence cs = m_inputText.getText();
                    m_inputText.setText(cs.subSequence(0, max(0, cs.length() - 1)));
                }
                else if (m_curNode.getCharVal() != null)
                {
                    Log.d(TAG, "input char = " + m_curNode.getCharVal());
                    m_inputText.setText(m_inputText.getText() + String.valueOf(m_curNode.getCharVal()));
                }
            }
            m_curNode = m_rootNode;
            updateShowText();
            if (te.val == TouchEvent.END)
            {
                m_touchArray.clear();
            }
            else
            {
                m_touchArray.add(te);
            }
        }
        else
        {
            KeyNode next_node = null;
            if (m_touchArray.size() <= 0 || m_touchArray.get(m_touchArray.size()-1).val != TouchEvent.DROP)
            {
                Log.d(TAG, "Touch = " + te.val);
                if (m_touchArray.size() >= 2)
                {
                    int dx1 = te.val - m_touchArray.get(m_touchArray.size()-1).val;
                    int dx2 = m_touchArray.get(m_touchArray.size()-1).val - m_touchArray.get(m_touchArray.size()-2).val;
                    if (dx1/abs(dx1) == dx2/abs(dx2))
                    {
                        m_touchArray.set(m_touchArray.size()-1, te);
                        next_node = m_curNode.getParent().getNextNode(te.val);
                    }
                    else
                    {
                        m_touchArray.add(te);
                        next_node = m_curNode.getNextNode(te.val);
                    }
                }
                else
                {
                    m_touchArray.add(te);
                    next_node = m_curNode.getNextNode(te.val);
                }
            }
            if (next_node != null)
            {
                m_curNode = next_node;
                updateShowText();
            }
            else
            {
                m_touchArray.add(new TouchEvent(TouchEvent.DROP));
                Log.d(TAG, "Touch drop: end reached");
            }
        }
    }
}
