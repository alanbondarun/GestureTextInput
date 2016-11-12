package com.alanb.gesturetextinput;

import android.content.Context;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GesturePoint;
import android.gesture.GestureStore;
import android.gesture.GestureStroke;
import android.gesture.Prediction;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;

import static java.lang.Math.atan2;
import static java.lang.Math.max;

public class PalmActivity extends AppCompatActivity
{
    private enum LayoutDir
    {
        LU, U, RU, L, R, LD, D, RD, N
    }
    private static final int LayoutDirNum = LayoutDir.values().length;
    final LayoutDir[] layoutDirs = LayoutDir.values();

    private final boolean DEBUG_MODE = false;
    private final double FIRST_SAMPLE_DIST_THRESH = 50.0;
    private final int FIRST_DIR_SAMPLE = 7;
    private final int SECOND_DIR_SAMPLE = 25;
    private final int SECOND_DIR_PERIOD = 3;
    private ArrayList<Double> m_point_angles;
    private boolean m_group_selected = false;
    private final double ANGLE_THRESH = 22.5 * Math.PI / 180.0;

    private final String TAG = this.getClass().getName();
    private GestureOverlayView m_gestureView;
    private GestureLibrary m_gestureLib;
    private String m_inputStr = "";
    private TextView m_inputTextView;
    private boolean useTouchFeedback = false;
    private boolean upperTouchFeedback = true;
    private ArrayList< ArrayList<TextView> > m_charViewGroups;

    private final int nonSelectedColor = Color.TRANSPARENT;
    private int selectedColor;

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
            "M", "spc", "del"
    };
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
    private final LayoutDir[][] gesture_layout_dir = {
            {LayoutDir.LU, LayoutDir.L}, {LayoutDir.LU, LayoutDir.N}, {LayoutDir.LU, LayoutDir.R},
            {LayoutDir.U, LayoutDir.L}, {LayoutDir.U, LayoutDir.N}, {LayoutDir.U, LayoutDir.R},
            {LayoutDir.RU, LayoutDir.L}, {LayoutDir.RU, LayoutDir.N}, {LayoutDir.RU, LayoutDir.R},
            {LayoutDir.L, LayoutDir.U}, {LayoutDir.L, LayoutDir.RU}, {LayoutDir.L, LayoutDir.N}, {LayoutDir.L, LayoutDir.D}, {LayoutDir.L, LayoutDir.RD},
            {LayoutDir.R, LayoutDir.LU}, {LayoutDir.R, LayoutDir.U}, {LayoutDir.R, LayoutDir.N}, {LayoutDir.R, LayoutDir.LD}, {LayoutDir.R, LayoutDir.D},
            {LayoutDir.LD, LayoutDir.L}, {LayoutDir.LD, LayoutDir.N}, {LayoutDir.LD, LayoutDir.R},
            {LayoutDir.D, LayoutDir.L}, {LayoutDir.D, LayoutDir.N}, {LayoutDir.D, LayoutDir.R},
            {LayoutDir.RD, LayoutDir.L}, {LayoutDir.RD, LayoutDir.N}, {LayoutDir.RD, LayoutDir.R},
    };

    private final float GESTURE_SPEED = 1.6f;
    private final float SAMPLE_PER_SEC = 120f;
    private GestureStore m_gestureStore;

    private double angle_diff(double a, double b)
    {
        double diff = Math.abs(b - a);
        return Math.abs(diff - 2 * Math.PI * Math.round(diff / (2*Math.PI)));
    }

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

            Gesture gesture = new Gesture();
            gesture.addStroke(new GestureStroke(points));
            store.addGesture(gesture_labels[ci], gesture);
        }
        return store;
    }

    private String predictGesture(ArrayList<GesturePoint> points, GestureStore store)
    {
        Gesture gesture = new Gesture();
        gesture.addStroke(new GestureStroke(points));
        ArrayList<Prediction> pred_list = store.recognize(gesture);
        if (pred_list.size() > 0)
        {
            if (DEBUG_MODE)
            {
                Log.d(TAG, "gesture predicted, stroke=" + gesture.getStrokesCount());
                for (int ci = 0; ci < java.lang.Math.min(5, pred_list.size()); ci++)
                {
                    Log.d(TAG, ci + "th: " + pred_list.get(ci).name + ", score: " +
                            pred_list.get(ci).score);
                }
            }

            int ccand = 0;
            for (; ccand < pred_list.size(); ccand++)
            {
                // prune out the gesture prediction based on direction from start pos to finish pos
                double dx = points.get(points.size()-1).x - points.get(0).x;
                double dy = points.get(0).y - points.get(points.size()-1).y;

                int cidx = 0;
                while (cidx < gesture_labels.length && !gesture_labels[cidx].equals(pred_list.get(ccand).name))
                {
                    cidx++;
                }
                double target_angle = gesture_angles[cidx];
                double dangle = atan2(dy, dx);
                if (angle_diff(target_angle, dangle) < ANGLE_THRESH)
                {
                    // correct gesture detected
                    break;
                }
            }
            if (ccand >= pred_list.size() && DEBUG_MODE)
            {
                Log.d(TAG, "gesture prediction failed");
                return null;
            }
            else
            {
                return pred_list.get(ccand).name;
            }
        }

        if (DEBUG_MODE)
            Log.d(TAG, "gesture prediction failed");
        return null;
    }

    private void processInput(ArrayList<GesturePoint> points)
    {
        String input_str = predictGesture(points, m_gestureStore);
        if (input_str != null)
        {
            if (input_str.equals("del"))
            {
                m_inputStr = m_inputStr.substring(0, max(0, m_inputStr.length()-1));
            }
            else if (input_str.equals("spc"))
            {
                m_inputStr += " ";
            }
            else
            {
                m_inputStr += input_str.toLowerCase();
            }
            m_inputTextView.setText(m_inputStr + getString(R.string.end_of_input));
        }
    }

    private View.OnTouchListener m_gestureViewTouchListener =
            new View.OnTouchListener()
    {
        private ArrayList<GesturePoint> m_curGPoints = new ArrayList<>();
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent)
        {
            if (motionEvent.getAction() == MotionEvent.ACTION_MOVE ||
                    motionEvent.getAction() == MotionEvent.ACTION_DOWN)
            {
                m_curGPoints.add(new GesturePoint(motionEvent.getX(), motionEvent.getY(),
                        System.currentTimeMillis()));
                if (m_curGPoints.size() >= 2)
                {
                    double dx = motionEvent.getX() - m_curGPoints.get(0).x;
                    double dy = m_curGPoints.get(0).y - motionEvent.getY();
                    if (Math.hypot(dx, dy) >= FIRST_SAMPLE_DIST_THRESH)
                    {
                        m_point_angles.add(atan2(dy, dx));
                    }
                    if (m_point_angles.size() >= FIRST_DIR_SAMPLE && !m_group_selected)
                    {
                        // ASSUMPTION: angles do not change radically during the gesture...
                        double angle_sum = 0;
                        for (double a: m_point_angles)
                        {
                            if (a <= -Math.PI + ANGLE_THRESH)
                            {
                                angle_sum += (2 * Math.PI) + a;
                            }
                            else
                            {
                                angle_sum += a;
                            }
                        }
                        angle_sum /= m_point_angles.size();
                        angle_sum -= Math.floor(angle_sum / (2.0 * Math.PI)) * (2.0 * Math.PI);
                        // now angle_sum in range [0, 2*PI]
                        int angle_idx = ((int) Math.round(angle_sum * 8 / (2.0 * Math.PI))) % 8;

                        final LayoutDir[] dirs_cw = { LayoutDir.R, LayoutDir.RU, LayoutDir.U,
                                LayoutDir.LU, LayoutDir.L, LayoutDir.LD, LayoutDir.D, LayoutDir.RD};
                        highlightGroup(dirs_cw[angle_idx]);
                        m_group_selected = true;
                    }
                    if (m_curGPoints.size() >= SECOND_DIR_SAMPLE &&
                            (m_curGPoints.size() - SECOND_DIR_SAMPLE) % SECOND_DIR_PERIOD == 0)
                    {
                        String input_str = predictGesture(m_curGPoints, m_gestureStore);
                        if (input_str != null)
                        {
                            for (int ci=0; ci<gesture_labels.length; ci++)
                            {
                                if (gesture_labels[ci].equals(input_str))
                                {
                                    highlightCharacter(gesture_layout_dir[ci][0], gesture_layout_dir[ci][1]);
                                }
                            }
                        }
                    }
                }
            }
            else
            {
                processInput(m_curGPoints);
                m_curGPoints.clear();

                // initialization for next gesture input
                m_point_angles.clear();
                m_group_selected = false;
                highlightBasic();
            }
            return false;
        }
    };

    private LayoutDir intToLayoutDir(int i)
    {
        if (i < 0 || i >= LayoutDirNum)
            return LayoutDir.N;
        return layoutDirs[i];
    }

    private void highlightBasic()
    {
        final LayoutDir[] basic_dirs = { LayoutDir.LU, LayoutDir.LD, LayoutDir.RU, LayoutDir.RD, LayoutDir.N };
        for (LayoutDir dir1: layoutDirs)
        {
            boolean in_basic_dir = false;
            for (LayoutDir bdir: basic_dirs)
            {
                if (bdir.equals(dir1))
                {
                    in_basic_dir = true;
                    break;
                }
            }
            for (TextView tv: m_charViewGroups.get(dir1.ordinal()))
            {
                if (tv != null)
                {
                    if (in_basic_dir)
                        tv.setBackgroundColor(selectedColor);
                    else
                        tv.setBackgroundColor(nonSelectedColor);
                }
            }
        }
    }

    private void highlightGroup(LayoutDir dir)
    {
        for (LayoutDir dir1: layoutDirs)
        {
            for (TextView tv: m_charViewGroups.get(dir1.ordinal()))
            {
                if (tv != null)
                {
                    if (dir1 == dir)
                        tv.setBackgroundColor(selectedColor);
                    else
                        tv.setBackgroundColor(nonSelectedColor);
                }
            }
        }
    }

    private void highlightCharacter(LayoutDir dir1, LayoutDir dir2)
    {
        for (LayoutDir cd1: layoutDirs)
        {
            for (LayoutDir cd2: layoutDirs)
            {
                TextView tv = m_charViewGroups.get(cd1.ordinal()).get(cd2.ordinal());
                if (tv != null)
                {
                    if (cd1 == dir1 && cd2 == dir2)
                    {
                        tv.setBackgroundColor(selectedColor);
                    }
                    else
                    {
                        tv.setBackgroundColor(nonSelectedColor);
                    }
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palm);

        selectedColor = getColorVersion(this, R.color.colorTouchBackground);

        m_gestureLib = GestureLibraries.fromRawResource(this, R.raw.palm_gestures);
        m_gestureLib.load();

        m_gestureStore = createGestureLibFromSource();

        m_point_angles = new ArrayList<>();

        m_gestureView = (GestureOverlayView) findViewById(R.id.p_gesture_view);
        m_gestureView.setOnTouchListener(m_gestureViewTouchListener);

        m_inputTextView = (TextView) findViewById(R.id.p_input_text);
        m_inputTextView.setText(m_inputStr + getString(R.string.end_of_input));

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

        m_charViewGroups = setCharViewGroups();
        highlightBasic();
    }

    private int getColorVersion(Context context, int id)
    {
        final int version = Build.VERSION.SDK_INT;
        if (version >= Build.VERSION_CODES.M)
        {
            return context.getColor(id);
        }
        else
        {
            return context.getResources().getColor(id);
        }
    }

    private ArrayList< ArrayList<TextView> > setCharViewGroups()
    {
        String package_name = this.getPackageName();
        ArrayList< ArrayList<TextView> > views = new ArrayList<>();
        for (int ci=0; ci < LayoutDirNum; ci++)
        {
            ArrayList<TextView> tv = new ArrayList<>();
            String ci_layout_str = intToLayoutDir(ci).toString();
            for (int cj=0; cj < LayoutDirNum; cj++)
            {
                String cj_layout_str = intToLayoutDir(cj).toString();
                String layout_id_str = "p_char_view_" + ci_layout_str + "_" + cj_layout_str;
                int layout_id = getResources().getIdentifier(layout_id_str, "id", package_name);
                if (layout_id != 0)
                {
                    tv.add((TextView) findViewById(layout_id));
                }
                else
                {
                    tv.add(null);
                }
            }
            views.add(tv);
        }
        return views;
    }
}
