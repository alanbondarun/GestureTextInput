package com.alanb.gesturetextinput;

import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.Touch;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Iterator;

public class WatchWriteActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();

    public enum TouchEvent
    {
        AREA1, AREA2, AREA3, AREA4, DROP, END
    }

    private GestureOverlayView m_gestureView;
    private GestureLibrary m_gestureLib;
    private ArrayList<TouchEvent> m_gestureTouchAreas;
    private KeyNode m_rootNode, m_curNode;
    private boolean m_gesturePredictionNeeded = false;
    private TextView m_inputText;

    private View.OnTouchListener m_touchListener = new View.OnTouchListener()
    {
        private TouchEvent prev_e = TouchEvent.DROP;
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent)
        {
            TouchEvent cur_e;
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                    || motionEvent.getAction() == MotionEvent.ACTION_MOVE)
            {
                double xrel = motionEvent.getX() / view.getWidth();
                double yrel = motionEvent.getY() / view.getHeight();
                if (0 < xrel && xrel <= 0.5)
                {
                    if (0 < yrel && yrel <= 0.5)
                    {
                        cur_e = TouchEvent.AREA1;
                    }
                    else if (0.5 < yrel && yrel < 1)
                    {
                        cur_e = TouchEvent.AREA3;
                    }
                    else
                    {
                        cur_e = TouchEvent.DROP;
                    }
                }
                else if (0.5 < xrel && xrel < 1)
                {
                    if (0 < yrel && yrel <= 0.5)
                    {
                        cur_e = TouchEvent.AREA2;
                    }
                    else if (0.5 < yrel && yrel < 1)
                    {
                        cur_e = TouchEvent.AREA4;
                    }
                    else
                    {
                        cur_e = TouchEvent.DROP;
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
            if (cur_e != prev_e)
            {
                receiveTouchPos(cur_e);
            }
            prev_e = cur_e;

            return false;
        }
    };

    private GestureOverlayView.OnGesturePerformedListener m_gesturePerformedListener =
            new GestureOverlayView.OnGesturePerformedListener()
    {
        @Override
        public void onGesturePerformed(GestureOverlayView gestureOverlayView, Gesture gesture)
        {
            ArrayList<Prediction> pred_list = m_gestureLib.recognize(gesture);
            if (pred_list.size() > 0)
            {
                Log.d(TAG, "gesture predicted");
                for (int ci=0; ci<java.lang.Math.min(5, pred_list.size()); ci++)
                {
                    Log.d(TAG, ci + "th: " + pred_list.get(ci).name + ", score: " + pred_list.get(ci).score);
                }
            }
            else
            {
                Log.d(TAG, "gesture prediction failed");
            }
            recognizeInput(pred_list);
        }
    };

    private GestureOverlayView.OnGesturingListener m_onGesturingListener =
            new GestureOverlayView.OnGesturingListener()
    {
        @Override
        public void onGesturingStarted(GestureOverlayView gestureOverlayView)
        {
            m_gesturePredictionNeeded = true;
        }

        @Override
        public void onGesturingEnded(GestureOverlayView gestureOverlayView)
        {
            if (!m_gesturePredictionNeeded)
            {
                recognizeInput(null);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_write);

        m_rootNode = KeyNode.generateKeyTree(this, R.raw.key_value_watch);
        m_curNode = m_rootNode;

        m_gestureTouchAreas = new ArrayList<>();

        m_inputText = (TextView) findViewById(R.id.w_input_text);
        m_gestureLib = GestureLibraries.fromRawResource(this, R.raw.gestures);
        m_gestureLib.load();

        m_gestureView = (GestureOverlayView) findViewById(R.id.w_gesture_view);
        m_gestureView.setOnTouchListener(m_touchListener);
        m_gestureView.addOnGesturePerformedListener(m_gesturePerformedListener);
        m_gestureView.addOnGesturingListener(m_onGesturingListener);
    }

    public void receiveTouchPos(TouchEvent te)
    {
        if (te == TouchEvent.END)
        {
            Log.d(TAG, "Touch End");
        }
        if (te == TouchEvent.DROP)
        {
            m_gestureTouchAreas.add(te);
        }
        else if (te != TouchEvent.END)
        {
            if (m_gestureTouchAreas.size() <= 0 ||
                    m_gestureTouchAreas.get(m_gestureTouchAreas.size()-1) != TouchEvent.DROP)
            {
                m_gestureTouchAreas.add(te);
                Log.d(TAG, "Touch add: " + te);
            }
        }
    }

    public void recognizeInput(ArrayList<Prediction> pred_list)
    {
        Character result = null;
        if (pred_list == null)
        {
            result = calcInputChar(m_gestureTouchAreas);
        }
        else
        {
            for (Prediction p : pred_list)
            {
                // check if the given prediction contradicts the given touch area
                ArrayList<ArrayList<TouchEvent>> touch_list = calcMatchingEventList(p.name.split("-"));
                for (ArrayList<TouchEvent> tlist : touch_list)
                {
                    if (hasSubsequence(m_gestureTouchAreas, tlist))
                    {
                        result = calcInputChar(tlist);
                        break;
                    }
                }
                if (result != null)
                {
                    break;
                }
            }
        }
        if (result == null)
        {
            Log.d(TAG, "Input Failed");
        }
        else
        {
            Log.d(TAG, "Input Result: " + result);
            m_inputText.setText(m_inputText.getText() + String.valueOf(result));
        }

        // initialization for next touch(or gesture) input
        m_gestureTouchAreas.clear();
        m_gesturePredictionNeeded = false;
    }

    private ArrayList< ArrayList<TouchEvent> > calcMatchingEventList(String[] name_str_list)
    {
        ArrayList<ArrayList<TouchEvent>> seq_list = new ArrayList<>();
        TouchEvent[] valid_touches = {
                TouchEvent.AREA1, TouchEvent.AREA2, TouchEvent.AREA3, TouchEvent.AREA4
        };

        for (TouchEvent te: valid_touches)
        {
            ArrayList<TouchEvent> tseq = new ArrayList<>();
            tseq.add(te);
            seq_list.add(tseq);
        }

        for (String name: name_str_list)
        {
            ArrayList< ArrayList<TouchEvent> > next_seqs = new ArrayList<>();
            switch (name)
            {
                case "T":
                    for (ArrayList<TouchEvent> te: seq_list)
                    {
                        if (te.get(te.size()-1) == TouchEvent.AREA3)
                        {
                            te.add(TouchEvent.AREA1);
                            next_seqs.add(te);
                        }
                        else if (te.get(te.size()-1) == TouchEvent.AREA4)
                        {
                            te.add(TouchEvent.AREA2);
                            next_seqs.add(te);
                        }
                    }
                    break;
                case "B":
                    for (ArrayList<TouchEvent> te: seq_list)
                    {
                        if (te.get(te.size()-1) == TouchEvent.AREA1)
                        {
                            te.add(TouchEvent.AREA3);
                            next_seqs.add(te);
                        }
                        else if (te.get(te.size()-1) == TouchEvent.AREA2)
                        {
                            te.add(TouchEvent.AREA4);
                            next_seqs.add(te);
                        }
                    }
                    break;
                case "L":
                    for (ArrayList<TouchEvent> te: seq_list)
                    {
                        if (te.get(te.size()-1) == TouchEvent.AREA2)
                        {
                            te.add(TouchEvent.AREA1);
                            next_seqs.add(te);
                        }
                        else if (te.get(te.size()-1) == TouchEvent.AREA4)
                        {
                            te.add(TouchEvent.AREA3);
                            next_seqs.add(te);
                        }
                    }
                    break;
                case "R":
                    for (ArrayList<TouchEvent> te: seq_list)
                    {
                        if (te.get(te.size()-1) == TouchEvent.AREA1)
                        {
                            te.add(TouchEvent.AREA2);
                            next_seqs.add(te);
                        }
                        else if (te.get(te.size()-1) == TouchEvent.AREA3)
                        {
                            te.add(TouchEvent.AREA4);
                            next_seqs.add(te);
                        }
                    }
                    break;
                case "1D":
                    for (ArrayList<TouchEvent> te: seq_list)
                    {
                        if (te.get(te.size()-1) == TouchEvent.AREA3)
                        {
                            te.add(TouchEvent.AREA2);
                            next_seqs.add(te);
                        }
                    }
                    break;
                case "5D":
                    for (ArrayList<TouchEvent> te: seq_list)
                    {
                        if (te.get(te.size()-1) == TouchEvent.AREA1)
                        {
                            te.add(TouchEvent.AREA4);
                            next_seqs.add(te);
                        }
                    }
                    break;
                case "7D":
                    for (ArrayList<TouchEvent> te: seq_list)
                    {
                        if (te.get(te.size()-1) == TouchEvent.AREA2)
                        {
                            te.add(TouchEvent.AREA3);
                            next_seqs.add(te);
                        }
                    }
                    break;
                case "0D":
                    for (ArrayList<TouchEvent> te: seq_list)
                    {
                        if (te.get(te.size()-1) == TouchEvent.AREA4)
                        {
                            te.add(TouchEvent.AREA1);
                            next_seqs.add(te);
                        }
                    }
                    break;
            }
            seq_list = next_seqs;
        }

        return seq_list;
    }

    private Character calcInputChar(ArrayList<TouchEvent> touch_list)
    {
        KeyNode key = m_rootNode;
        Character ic = null;
        for (TouchEvent te: touch_list)
        {
            switch (te)
            {
                case AREA1:
                    key = key.getNextNode(0);
                    break;
                case AREA2:
                    key = key.getNextNode(1);
                    break;
                case AREA3:
                    key = key.getNextNode(2);
                    break;
                case AREA4:
                    key = key.getNextNode(3);
                    break;
                default:
                    ic = key.getCharVal();
                    key = null;
            }
            if (ic != null || key == null)
            {
                break;
            }
        }
        if (key != null && ic == null)
        {
            ic = key.getCharVal();
        }
        return ic;
    }

    private boolean hasSubsequence(ArrayList<TouchEvent> a_src, ArrayList<TouchEvent> a_sub)
    {
        int idx_src = 0;
        int idx_sub = 0;
        while (idx_src < a_src.size() && idx_sub < a_sub.size())
        {
            while (idx_src < a_src.size() && a_src.get(idx_src) != a_sub.get(idx_sub))
            {
                idx_src++;
            }
            if (idx_src < a_src.size())
            {
                idx_src++; idx_sub++;
            }
        }
        return idx_sub >= a_sub.size();
    }
}
