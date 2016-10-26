package com.alanb.gesturetextinput;

import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GesturePoint;
import android.gesture.GestureStore;
import android.gesture.GestureStroke;
import android.gesture.Prediction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import static java.lang.Math.max;

public class PalmActivity extends AppCompatActivity
{
    private final String TAG = this.getClass().getName();
    private GestureOverlayView m_gestureView;
    private GestureLibrary m_gestureLib;
    private ArrayList<GesturePoint> m_curGPoints;
    private TextView m_inputText;

    private void predictGesture(ArrayList<GesturePoint> points)
    {
        Gesture gesture = new Gesture();
        gesture.addStroke(new GestureStroke(points));
        ArrayList<Prediction> pred_list = m_gestureLib.recognize(gesture);
        if (pred_list.size() > 0)
        {
            Log.d(TAG, "gesture predicted, stroke=" + gesture.getStrokesCount());
            for (int ci=0; ci<java.lang.Math.min(5, pred_list.size()); ci++)
            {
                Log.d(TAG, ci + "th: " + pred_list.get(ci).name + ", score: " +
                        pred_list.get(ci).score);
            }
            if (pred_list.get(0).name.equals("del"))
            {
                CharSequence cs = m_inputText.getText();
                m_inputText.setText(cs.subSequence(0, max(0, cs.length() - 1)));
            }
            else
            {
                m_inputText.setText(m_inputText.getText() + pred_list.get(0).name);
            }
        }
        else
        {
            Log.d(TAG, "gesture prediction failed");
        }
    }

    private View.OnTouchListener m_gestureViewTouchListener =
            new View.OnTouchListener()
    {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent)
        {
            if (motionEvent.getAction() == MotionEvent.ACTION_MOVE ||
                    motionEvent.getAction() == MotionEvent.ACTION_DOWN)
            {
                m_curGPoints.add(new GesturePoint(motionEvent.getX(), motionEvent.getY(),
                        System.currentTimeMillis()));
            }
            else
            {
                predictGesture(m_curGPoints);
                m_curGPoints.clear();
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palm);

        m_gestureLib = GestureLibraries.fromRawResource(this, R.raw.palm_gestures);
        m_gestureLib.load();

        m_curGPoints = new ArrayList<>();
        m_gestureView = (GestureOverlayView) findViewById(R.id.p_gesture_view);
        m_gestureView.setOnTouchListener(m_gestureViewTouchListener);
        m_inputText = (TextView) findViewById(R.id.p_input_text);
    }
}
