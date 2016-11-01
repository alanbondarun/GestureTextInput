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
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;

import static java.lang.Math.atan2;
import static java.lang.Math.max;

public class PalmActivity extends AppCompatActivity
{
    private final String TAG = this.getClass().getName();
    private GestureOverlayView m_gestureView;
    private GestureLibrary m_gestureLib;
    private ArrayList<GesturePoint> m_curGPoints;
    private TextView m_inputText;
    private boolean useTouchFeedback = false;
    private boolean upperTouchFeedback = true;

    private final float[][] gesture_vertices = {
            {900, 500, 500, 100, 100, 100},
            {900, 500, 500, 100},
            {900, 500, 500, 100, 900, 100},

            {900, 500, 900, 100, 500, 100},
            {500, 500, 500, 100},
            {500, 500, 500, 100, 900, 100},

            {100, 500, 500, 100, 100, 100},
            {100, 500, 500, 100},
            {100, 500, 500, 100, 900, 100},

            {900, 500, 500, 500, 500, 100},
            {900, 500, 500, 500, 900, 100},
            {900, 500, 500, 500},
            {900, 100, 500, 100, 500, 500},
            {900, 100, 500, 100, 900, 500},

            {500, 500, 900, 500, 500, 100},
            {500, 500, 900, 500, 900, 100},
            {500, 500, 900, 500},
            {500, 100, 900, 100, 500, 500},
            {500, 100, 900, 100, 900, 500},

            {900, 100, 500, 500, 100, 500},
            {900, 100, 500, 500},
            {900, 100, 500, 500, 900, 500},

            {900, 100, 900, 500, 500, 500},
            {900, 100, 900, 500},
            {500, 100, 500, 500, 900, 500},

            {100, 100, 500, 500, 100, 500},
            {100, 100, 500, 500},
            {100, 100, 500, 500, 900, 500}
    };
    private final String[] gesture_labels = {
            "Q", "W", "E",
            "R", "T", "Y",
            "U", "I", "O",
            "A", "S", "D", "F", "G",
            "H", "J", "K", "L", "P",
            "Z", "X", "C",
            "V", "B", "N",
            "M", ".", "del"
    };
    private final double ANGLE_THRESH = 22.5 * Math.PI / 180.0;
    private final double[] gesture_angles = {
            Math.atan2(1, -2), Math.atan2(1, -1), Math.atan2(1, 0),
            Math.atan2(1, -1), Math.atan2(1, 0), Math.atan2(1, 1),
            Math.atan2(1, 0), Math.atan2(1, 1), Math.atan2(1, 2),
            Math.atan2(1, -1), Math.atan2(1, 0), Math.atan2(0, -1), Math.atan2(-1, -1), Math.atan2(-1, 0),
            Math.atan2(1, 0), Math.atan2(1, 1), Math.atan2(0, 1), Math.atan2(-1, 0), Math.atan2(-1, 1),
            Math.atan2(-1, -2), Math.atan2(-1, -1), Math.atan2(-1, 0),
            Math.atan2(-1, -1), Math.atan2(-1, 0), Math.atan2(-1, 1),
            Math.atan2(-1, 0), Math.atan2(-1, 1), Math.atan2(-1, 2),
    };

    private final float GESTURE_SPEED = 1.6f;
    private final float SAMPLE_PER_SEC = 120f;
    private GestureStore m_gestureStore;

    private GestureStore createGestureLibFromSource()
    {
        GestureStore store = new GestureStore();
        for (int ci=0; ci<gesture_labels.length; ci++)
        {
            ArrayList<GesturePoint> points = new ArrayList<>();
            long tt = 0;
            for (int cj=0; cj<(gesture_vertices[ci].length / 2) - 1; cj++)
            {
                long tadd = 0;
                double tx = gesture_vertices[ci][cj*2];
                double ty = gesture_vertices[ci][cj*2 + 1];
                double dx = gesture_vertices[ci][cj*2 + 2] - tx;
                double dy = gesture_vertices[ci][cj*2 + 3] - ty;
                double dist = Math.sqrt(dx*dx + dy*dy);
                double udx = dx / dist;
                double udy = dy / dist;
                double tdd = 0;
                while (tdd/dist < 1)
                {
                    points.add(new GesturePoint((float)(tx), (float)(ty),
                            tt + (long)(tadd*(1000f/SAMPLE_PER_SEC))));
                    tadd++;
                    tx += (udx * GESTURE_SPEED * 1000f / SAMPLE_PER_SEC);
                    ty += (udy * GESTURE_SPEED * 1000f / SAMPLE_PER_SEC);
                    tdd += (GESTURE_SPEED * 1000f / SAMPLE_PER_SEC);
                }
                tt += (dist/GESTURE_SPEED);
            }

            Log.d(TAG, "points = " + points);
            Gesture gesture = new Gesture();
            gesture.addStroke(new GestureStroke(points));
            store.addGesture(gesture_labels[ci], gesture);
        }
        return store;
    }

    private void predictGesture(ArrayList<GesturePoint> points, GestureStore store)
    {
        Gesture gesture = new Gesture();
        gesture.addStroke(new GestureStroke(points));
        ArrayList<Prediction> pred_list = store.recognize(gesture);
        if (pred_list.size() > 0)
        {
            Log.d(TAG, "gesture predicted, stroke=" + gesture.getStrokesCount());
            for (int ci=0; ci<java.lang.Math.min(5, pred_list.size()); ci++)
            {
                Log.d(TAG, ci + "th: " + pred_list.get(ci).name + ", score: " +
                        pred_list.get(ci).score);
            }

            int ccand = 0;
            for (; ccand < pred_list.size(); ccand++)
            {
                // prune out the gesture prediction based on direction from start pos to finish pos
                double dx = points.get(points.size()-1).x - points.get(0).x;
                double dy = points.get(0).y - points.get(points.size()-1).y;

                int cidx = 0;
                while (cidx < gesture_labels.length && gesture_labels[cidx] != pred_list.get(ccand).name)
                {
                    cidx++;
                }
                double target_angle = gesture_angles[cidx];
                if (Math.abs(target_angle - atan2(dy, dx)) < ANGLE_THRESH)
                {
                    // correct gesture detected
                    break;
                }
            }
            if (ccand >= pred_list.size())
            {
                Log.d(TAG, "gesture prediction failed");
            }
            else if (pred_list.get(ccand).name.equals("del"))
            {
                CharSequence cs = m_inputText.getText();
                m_inputText.setText(cs.subSequence(0, max(0, cs.length() - 1)));
            }
            else
            {
                m_inputText.setText(m_inputText.getText() + pred_list.get(ccand).name);
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
                predictGesture(m_curGPoints, m_gestureStore);
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

        m_gestureStore = createGestureLibFromSource();

        m_curGPoints = new ArrayList<>();
        m_gestureView = (GestureOverlayView) findViewById(R.id.p_gesture_view);
        m_gestureView.setOnTouchListener(m_gestureViewTouchListener);
        m_inputText = (TextView) findViewById(R.id.p_input_text);

        if (useTouchFeedback)
        {
            TouchFeedbackFrameLayout feedbackFrameLayout = (TouchFeedbackFrameLayout)
                    findViewById(R.id.p_touch_point_area);
            if (upperTouchFeedback)
            {
                FrameLayout upperFrame = (FrameLayout) findViewById(R.id.p_upper_frame);
                feedbackFrameLayout.attachFeedbackTo(upperFrame);
            }
            else
            {
                feedbackFrameLayout.attachFeedbackTo(feedbackFrameLayout);
            }
        }
    }
}
