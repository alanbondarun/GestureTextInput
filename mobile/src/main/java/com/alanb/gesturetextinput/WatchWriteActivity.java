package com.alanb.gesturetextinput;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alanb.gesturecommon.WatchWriteInputView;

import java.util.ArrayList;
import static java.lang.Math.max;

public class WatchWriteActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();

    private WatchWriteInputView m_touchInputView;
    private ArrayList<WatchWriteInputView.TouchEvent> m_gestureTouchAreas;
    private KeyNode m_rootNode;
    // DO NOT modify this directly; use updateCurNode() instead
    private KeyNode m_curNode;

    private String m_inputStr = "";
    private TextView m_inputTextView;
    private ArrayList<TextView> m_viewTexts;
    private final boolean upperTouchFeedback = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_write);

        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_pref_key), MODE_PRIVATE);
        int pref_layout = prefs.getInt(getString(R.string.prefkey_watch_layout),
                getResources().getInteger(R.integer.pref_watch_layout_default));
        switch (pref_layout)
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

        m_inputTextView = (TextView) findViewById(R.id.w_input_text);

        m_viewTexts = new ArrayList<>();
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_1));
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_2));
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_3));
        m_viewTexts.add((TextView) findViewById(R.id.w_char_indi_4));

        updateViews(m_rootNode);

        WatchWriteInputView.Builder builder = new WatchWriteInputView.Builder(this);
        builder.setOnTouchEventListener(m_wwTouchListener);
        builder.setBackground(R.drawable.w_touch_back);
        m_touchInputView = builder.build();

        ((LinearLayout)(findViewById(R.id.w_char_touch))).addView(m_touchInputView);

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

    public WatchWriteInputView.OnTouchEventListener m_wwTouchListener =
            new WatchWriteInputView.OnTouchEventListener()
    {
        @Override
        public void onTouchEvent(WatchWriteInputView.TouchEvent te)
        {
            if (te == WatchWriteInputView.TouchEvent.END)
            {
                if (m_curNode.getAct() == KeyNode.Act.DELETE)
                {
                    Log.d(TAG, "Delete one character");
                    m_inputStr = m_inputStr.substring(0, max(0, m_inputStr.length()-1));
                }
                else if (m_curNode.getCharVal() != null)
                {
                    Log.d(TAG, "Input Result: " + m_curNode.getCharVal());
                    m_inputStr += String.valueOf(m_curNode.getCharVal());
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
    };

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
                for (int cj = 0; cj < raw_str.length(); cj += 3)
                {
                    if (cj > 0)
                        builder.append("\n");
                    builder.append(raw_str.substring(cj, Math.min(cj+3, raw_str.length())));
                }
                m_viewTexts.get(ci).setText(builder.toString());
                m_viewTexts.get(ci).setBackgroundColor(Color.TRANSPARENT);
            }
        }
        this.m_curNode = node;
    }
}
