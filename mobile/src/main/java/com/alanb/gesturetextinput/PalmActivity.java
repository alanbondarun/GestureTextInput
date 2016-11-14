package com.alanb.gesturetextinput;

import android.content.Context;
import android.content.SharedPreferences;
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
    public enum LayoutDir
    {
        LU, U, RU, L, R, LD, D, RD, N
    }
    private static final int LayoutDirNum = LayoutDir.values().length;
    final LayoutDir[] layoutDirs = LayoutDir.values();

    private final boolean DEBUG_MODE = false;
    private final double FIRST_SAMPLE_DIST_THRESH = 50.0;
    private final int FIRST_DIR_SAMPLE = 7;
    private final int SECOND_DIR_SAMPLE = 15;
    private final int SECOND_DIR_PERIOD = 5;
    private ArrayList<Double> m_point_angles;
    private boolean m_group_selected = false;
    private final double ANGLE_THRESH = 35.0 * Math.PI / 180.0;
    private final double SCORE_THRESH = 1.0;

    private final String TAG = this.getClass().getName();
    private GestureOverlayView m_gestureView;
    private String m_inputStr = "";
    private TextView m_inputTextView;
    private boolean useTouchFeedback = false;
    private boolean upperTouchFeedback = true;
    private ArrayList< ArrayList<TextView> > m_charViewGroups;

    private final int nonSelectedColor = Color.TRANSPARENT;
    private int selectedColor;

    private boolean m_taskMode;
    private TaskPhraseLoader m_taskLoader;
    private TextView m_taskTextView;
    private String m_taskStr = null;

    private int m_inc_fixed_num = 0;
    private int m_fix_num = 0;
    private int m_canceled_num = 0;

    private NanoTimer m_phraseTimer;
    private NanoTimer m_predictTimer;
    private ArrayList<TaskRecordWriter.TimedAction> m_timedActions;

    private TaskRecordWriter m_taskRecordWriter = null;

    private double angle_diff(double a, double b)
    {
        double diff = Math.abs(b - a);
        return Math.abs(diff - 2 * Math.PI * Math.round(diff / (2*Math.PI)));
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
                if (pred_list.get(ccand).score < SCORE_THRESH)
                    continue;
                if (pred_list.get(ccand).name.equals("done"))
                    break;

                // prune out the gesture prediction based on direction from start pos to finish pos
                double dx = points.get(points.size()-1).x - points.get(0).x;
                double dy = points.get(0).y - points.get(points.size()-1).y;

                int cidx = 0;
                while (cidx < PalmGestureGenerator.gesture_labels.length &&
                        !PalmGestureGenerator.gesture_labels[cidx].equals(pred_list.get(ccand).name))
                {
                    cidx++;
                }
                double target_angle = PalmGestureGenerator.gesture_angles[cidx];
                double dangle = atan2(dy, dx);
                if (angle_diff(target_angle, dangle) < ANGLE_THRESH)
                {
                    // correct gesture detected
                    break;
                }
            }
            if (ccand >= pred_list.size())
            {
                Log.d(TAG, "gesture prediction failed");
                return null;
            }
            else
            {
                return pred_list.get(ccand).name;
            }
        }

        Log.d(TAG, "gesture prediction failed");
        return null;
    }

    private void processInput(ArrayList<GesturePoint> points)
    {
        m_predictTimer.begin();
        String input_str = predictGesture(points, PalmGestureGenerator.get());
        m_predictTimer.check();
        m_predictTimer.end();
        //Log.d(TAG, "predict time: " + m_predictTimer.getDiffInSeconds());

        if (!m_phraseTimer.running())
            m_phraseTimer.begin();

        if (input_str != null)
        {
            if (input_str.equals("del"))
            {
                m_phraseTimer.check();
                m_inputStr = m_inputStr.substring(0, max(0, m_inputStr.length()-1));
                m_inc_fixed_num++;
                m_fix_num++;
                m_timedActions.add(new TaskRecordWriter.TimedAction(m_phraseTimer.getDiffInSeconds(), "del"));
            }
            else if (input_str.equals("spc"))
            {
                m_phraseTimer.check();
                m_inputStr += " ";
                m_timedActions.add(new TaskRecordWriter.TimedAction(m_phraseTimer.getDiffInSeconds(), " "));
            }
            else if (input_str.equals("done"))
            {
                doneTask();
            }
            else
            {
                m_phraseTimer.check();
                m_inputStr += input_str.toLowerCase();
                m_timedActions.add(new TaskRecordWriter.TimedAction(m_phraseTimer.getDiffInSeconds(), input_str));
            }
            m_inputTextView.setText(m_inputStr + getString(R.string.end_of_input));
        }
    }

    private View.OnTouchListener m_gestureViewTouchListener =
            new View.OnTouchListener()
    {
        private boolean multi_occurred = false;
        private ArrayList<GesturePoint> m_curGPoints = new ArrayList<>();
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent)
        {
            if (multi_occurred)
            {
                highlightBasic();
                if (motionEvent.getPointerCount() <= 1 && !(motionEvent.getAction() == MotionEvent.ACTION_MOVE ||
                        motionEvent.getAction() == MotionEvent.ACTION_DOWN))
                {
                    m_phraseTimer.check();
                    m_timedActions.add(new TaskRecordWriter.TimedAction(m_phraseTimer.getDiffInSeconds(), "cancel"));
                    m_canceled_num++;
                    multi_occurred = false;
                }
            }
            else
            {
                if (motionEvent.getPointerCount() >= 2)
                {
                    multi_occurred = true;
                }
                else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE ||
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
                            for (double a : m_point_angles)
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

                            final LayoutDir[] dirs_cw = {LayoutDir.R, LayoutDir.RU, LayoutDir.U,
                                    LayoutDir.LU, LayoutDir.L, LayoutDir.LD, LayoutDir.D, LayoutDir.RD};
                            highlightGroup(dirs_cw[angle_idx]);
                            m_group_selected = true;
                        }
                        if (m_curGPoints.size() >= SECOND_DIR_SAMPLE &&
                                (m_curGPoints.size() - SECOND_DIR_SAMPLE) % SECOND_DIR_PERIOD == 0)
                        {
                            String input_str = predictGesture(m_curGPoints, PalmGestureGenerator.get());
                            if (input_str != null)
                            {
                                for (int ci = 0; ci < PalmGestureGenerator.gesture_labels.length; ci++)
                                {
                                    if (input_str.equals(PalmGestureGenerator.done_gesture_label))
                                    {
                                        highlightAll(true);
                                    }
                                    else if (PalmGestureGenerator.gesture_labels[ci].equals(input_str))
                                    {
                                        highlightCharacter(PalmGestureGenerator.gesture_layout_dir[ci][0],
                                                PalmGestureGenerator.gesture_layout_dir[ci][1]);
                                    }
                                }
                            }
                            else
                            {
                                highlightAll(false);
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
                    if (dir1 != LayoutDir.N && cd1 == dir1 && cd2 == dir2)
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

    private void highlightAll(boolean highlight)
    {
        for (LayoutDir dir1: layoutDirs)
        {
            for (TextView tv: m_charViewGroups.get(dir1.ordinal()))
            {
                if (tv != null)
                {
                    if (highlight)
                        tv.setBackgroundColor(selectedColor);
                    else
                        tv.setBackgroundColor(nonSelectedColor);
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

        m_phraseTimer = new NanoTimer();
        m_predictTimer = new NanoTimer();
        initTask();
    }

    private void initTask()
    {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_pref_key), MODE_PRIVATE);
        m_taskMode = prefs.getInt(getString(R.string.prefkey_task_mode),
                getResources().getInteger(R.integer.pref_task_mode_default)) == 0;
        m_timedActions = new ArrayList<>();

        m_taskTextView = (TextView) findViewById(R.id.p_task_text);
        if (m_taskMode)
        {
            try
            {
                m_taskRecordWriter = new TaskRecordWriter(this, this.getClass());
            }
            catch (java.io.IOException e)
            {
                e.printStackTrace();
            }
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
        if (!m_taskMode)
            return;
        m_taskStr = m_taskLoader.next();
        m_taskTextView.setText(m_taskStr);
        m_inc_fixed_num = 0;
        m_fix_num = 0;
        m_canceled_num = 0;
        m_timedActions.clear();
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
                    .setLayoutNum(0)
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
