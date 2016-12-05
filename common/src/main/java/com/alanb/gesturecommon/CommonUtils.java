package com.alanb.gesturecommon;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;

public class CommonUtils
{
    public static int getColorVersion(Context context, int id)
    {
        return context.getResources().getColor(id);
    }

    public static double[] posDiffFromMotionEvents(MotionEvent e)
    {
        if (e.getHistorySize() < 1)
            return new double[]{0, 0};

        int lastIndex = e.getHistorySize() - 1;
        double lastInterval = (double)(e.getEventTime() - e.getHistoricalEventTime(lastIndex));
        return new double[]{
                (e.getX() - e.getHistoricalX(lastIndex)) / lastInterval,
                (e.getY() - e.getHistoricalY(lastIndex)) / lastInterval
        };
    }
}
