package com.alanb.gesturetextinput;

import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import static java.lang.Math.max;
import static java.lang.Math.subtractExact;

public class WatchWriteActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();
    private final double TOUCH_SIZE_RATIO = 0.4;

    public enum TouchEvent
    {
        AREA1, AREA2, AREA3, AREA4, AREA_OTHER, DROP, END, MULTITOUCH
    }

    private LinearLayout m_touchInputView;
    private ArrayList<TouchEvent> m_gestureTouchAreas;
    private KeyNode m_rootNode;
    // DO NOT modify this directly; use updateCurNode() instead
    private KeyNode m_curNode;
    private TextView m_inputText;
    private ArrayList<TextView> m_viewTexts;
    private final boolean upperTouchFeedback = true;

    private View.OnTouchListener m_touchListener = new View.OnTouchListener()
    {
        private TouchEvent prev_e = TouchEvent.AREA_OTHER;
        private boolean multi_occurred = false;
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent)
        {
            TouchEvent cur_e;
            if (multi_occurred && (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                    || motionEvent.getAction() == MotionEvent.ACTION_MOVE))
            {
                cur_e = TouchEvent.MULTITOUCH;
            }
            else
            {
                multi_occurred = false;
                if (motionEvent.getPointerCount() >= 2)
                {
                    // multi-touch occurred, cancel the input
                    multi_occurred = true;
                    cur_e = TouchEvent.MULTITOUCH;
                }
                else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                        || motionEvent.getAction() == MotionEvent.ACTION_MOVE)
                {
                    double xrel = motionEvent.getX() / view.getWidth();
                    double yrel = motionEvent.getY() / view.getHeight();
                    if (0 <= xrel && xrel <= 1 && 0 <= yrel && yrel <= 1)
                    {
                        if (yrel <= TOUCH_SIZE_RATIO)
                        {
                            if (xrel <= TOUCH_SIZE_RATIO)
                            {
                                cur_e = TouchEvent.AREA1;
                            }
                            else if (xrel >= 1.0 - TOUCH_SIZE_RATIO)
                            {
                                cur_e = TouchEvent.AREA2;
                            }
                            else
                            {
                                cur_e = TouchEvent.AREA_OTHER;
                            }
                        }
                        else if (yrel >= 1.0 - TOUCH_SIZE_RATIO)
                        {
                            if (xrel <= TOUCH_SIZE_RATIO)
                            {
                                cur_e = TouchEvent.AREA3;
                            }
                            else if (xrel >= 1.0 - TOUCH_SIZE_RATIO)
                            {
                                cur_e = TouchEvent.AREA4;
                            }
                            else
                            {
                                cur_e = TouchEvent.AREA_OTHER;
                            }
                        }
                        else
                        {
                            cur_e = TouchEvent.AREA_OTHER;
                        }
                    }
                    else
                    {
                        cur_e = TouchEvent.DROP;
                    }
                }
                else
                {
                    cur_e = TouchEvent.END;
                }
            }
            if (cur_e != prev_e)
            {
                processTouchPos(cur_e);
            }
            prev_e = cur_e;

            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_write);

        m_rootNode = KeyNode.generateKeyTree(this, R.raw.key_value_watch);

        m_gestureTouchAreas = new ArrayList<>();

        m_inputText = (TextView) findViewById(R.id.w_input_text);
        m_touchInputView = (LinearLayout) findViewById(R.id.w_char_touch);
        m_touchInputView.setOnTouchListener(m_touchListener);

        m_viewTexts = new ArrayList<>();
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_1));
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_2));
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_3));
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_4));

        updateCurNode(m_rootNode);

        TouchFeedbackFrameLayout feedbackFrameLayout = (TouchFeedbackFrameLayout)
                findViewById(R.id.w_touch_frame);
        if (upperTouchFeedback)
        {
            FrameLayout upperFrame = (FrameLayout) findViewById(R.id.w_upper_frame);
            feedbackFrameLayout.attachFeedbackTo(upperFrame);
        }
        else
        {
            feedbackFrameLayout.attachFeedbackTo(feedbackFrameLayout);
        }
    }

    public void processTouchPos(TouchEvent te)
    {
        if (te == TouchEvent.END)
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
        else if (te == TouchEvent.DROP)
        {
            m_gestureTouchAreas.add(te);
        }
        else if (te == TouchEvent.MULTITOUCH)
        {
            updateCurNode(m_rootNode);
            m_gestureTouchAreas.clear();
            m_gestureTouchAreas.add(te);
        }
        else if (te != TouchEvent.AREA_OTHER)
        {
            if (m_gestureTouchAreas.size() <= 0 ||
                    (m_gestureTouchAreas.get(m_gestureTouchAreas.size()-1) != TouchEvent.DROP &&
                    m_gestureTouchAreas.get(m_gestureTouchAreas.size()-1) != TouchEvent.MULTITOUCH))
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
                    m_gestureTouchAreas.add(TouchEvent.DROP);
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
