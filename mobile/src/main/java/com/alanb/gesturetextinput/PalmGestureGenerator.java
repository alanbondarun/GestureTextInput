package com.alanb.gesturetextinput;

import android.gesture.Gesture;
import android.gesture.GesturePoint;
import android.gesture.GestureStore;
import android.gesture.GestureStroke;

import com.alanb.gesturetextinput.PalmActivity.LayoutDir;

import java.util.ArrayList;

public class PalmGestureGenerator
{
    // length traveled in units of pixels, per millisecond
    private static final float GESTURE_SPEED = 1.6f;

    private static final float SAMPLE_PER_SEC = 120f;
    private static GestureStore m_store = null;

    public static final String done_gesture_label = "done";
    private static final double[] done_gesture_center = { 500, 500 };
    private static final double done_geture_radius = 400;

    private static final float[][] gesture_vertices = {
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
            {100, 100, 500, 500, 900, 500},
            {900, 500, 500, 500, 100, 100},
    };
    public static final String[] gesture_labels = {
            "Q", "W", "E",
            "R", "T", "Y",
            "U", "I", "O",
            "A", "S", "D", "F", "G",
            "H", "J", "K", "L", "P",
            "Z", "X", "C",
            "V", "B", "N",
            "M", "spc", "del", done_gesture_label
    };
    public static final double[] gesture_angles = {
            Math.atan2(1, -2), Math.atan2(1, -1), Math.atan2(1, 0),
            Math.atan2(1, -1), Math.atan2(1, 0), Math.atan2(1, 1),
            Math.atan2(1, 0), Math.atan2(1, 1), Math.atan2(1, 2),
            Math.atan2(1, -1), Math.atan2(1, 0), Math.atan2(0, -1), Math.atan2(-1, -1), Math.atan2(-1, 0),
            Math.atan2(1, 0), Math.atan2(1, 1), Math.atan2(0, 1), Math.atan2(-1, 0), Math.atan2(-1, 1),
            Math.atan2(-1, -2), Math.atan2(-1, -1), Math.atan2(-1, 0),
            Math.atan2(-1, -1), Math.atan2(-1, 0), Math.atan2(-1, 1),
            Math.atan2(-1, 0), Math.atan2(-1, 1), Math.atan2(-1, 2), Math.atan2(1, -2)
    };
    public static final LayoutDir[][] gesture_layout_dir = {
            {LayoutDir.LU, LayoutDir.L}, {LayoutDir.LU, LayoutDir.N}, {LayoutDir.LU, LayoutDir.R},
            {LayoutDir.U, LayoutDir.L}, {LayoutDir.U, LayoutDir.N}, {LayoutDir.U, LayoutDir.R},
            {LayoutDir.RU, LayoutDir.L}, {LayoutDir.RU, LayoutDir.N}, {LayoutDir.RU, LayoutDir.R},
            {LayoutDir.L, LayoutDir.U}, {LayoutDir.L, LayoutDir.RU}, {LayoutDir.L, LayoutDir.N}, {LayoutDir.L, LayoutDir.D}, {LayoutDir.L, LayoutDir.RD},
            {LayoutDir.R, LayoutDir.LU}, {LayoutDir.R, LayoutDir.U}, {LayoutDir.R, LayoutDir.N}, {LayoutDir.R, LayoutDir.LD}, {LayoutDir.R, LayoutDir.D},
            {LayoutDir.LD, LayoutDir.L}, {LayoutDir.LD, LayoutDir.N}, {LayoutDir.LD, LayoutDir.R},
            {LayoutDir.D, LayoutDir.L}, {LayoutDir.D, LayoutDir.N}, {LayoutDir.D, LayoutDir.R},
            {LayoutDir.RD, LayoutDir.L}, {LayoutDir.RD, LayoutDir.N}, {LayoutDir.RD, LayoutDir.R}, {LayoutDir.L, LayoutDir.LU}
    };

    private static GestureStore createGestureLibFromSource()
    {
        GestureStore store = new GestureStore();
        for (int ci=0; ci<gesture_vertices.length; ci++)
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

        //addDoneGesture(store);
        return store;
    }

    private static void addDoneGesture(GestureStore store)
    {
        for (int ci = 1; ci >= -1; ci -= 2)
        {
            for (int cj = 1; cj >= -1; cj -= 2)
            {
                ArrayList<GesturePoint> points = new ArrayList<>();
                double ttime = 0;
                double tangle = 0;
                while (tangle < 2 * Math.PI)
                {
                    points.add(new GesturePoint((float)(done_gesture_center[0] + ci * done_geture_radius * Math.cos(tangle)),
                            (float)(done_gesture_center[0] + cj * done_geture_radius * Math.sin(tangle)),
                            (long)(ttime)));
                    tangle += (GESTURE_SPEED * 1000f / SAMPLE_PER_SEC) / done_geture_radius;
                    ttime += 1000f / SAMPLE_PER_SEC;
                }

                Gesture gesture = new Gesture();
                gesture.addStroke(new GestureStroke(points));
                store.addGesture(done_gesture_label, gesture);
            }
        }
    }

    public static GestureStore get()
    {
        if (m_store == null)
            m_store = createGestureLibFromSource();
        return m_store;
    }
}
