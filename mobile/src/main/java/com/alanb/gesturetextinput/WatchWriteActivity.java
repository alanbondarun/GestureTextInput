package com.alanb.gesturetextinput;

import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class WatchWriteActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();

    public enum TouchEvent
    {
        AREA1, AREA2, AREA3, AREA4, DROP
    }

    private GestureOverlayView m_gestureView;
    private GestureLibrary m_gestureLib;
    private TouchEvent m_startPos;

    private View.OnTouchListener m_touchListener = new View.OnTouchListener()
    {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent)
        {
            TouchEvent cur_e = TouchEvent.DROP;
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
                cur_e = TouchEvent.DROP;
            }
            setStartPos(cur_e);

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
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_write);

        m_gestureLib = GestureLibraries.fromRawResource(this, R.raw.gestures);
        m_gestureLib.load();

        m_gestureView = (GestureOverlayView) findViewById(R.id.w_gesture_view);
        m_gestureView.setOnTouchListener(m_touchListener);
        m_gestureView.addOnGesturePerformedListener(m_gesturePerformedListener);
    }

    public void setStartPos(TouchEvent te)
    {
        if (m_startPos != te)
        {
            Log.d(TAG, "startpos=" + te);
        }
        m_startPos = te;
    }
}
