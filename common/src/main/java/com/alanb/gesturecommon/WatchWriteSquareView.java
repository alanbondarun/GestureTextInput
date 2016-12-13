package com.alanb.gesturecommon;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class WatchWriteSquareView extends WatchWriteInputView
{
    private static final double TOUCH_SIZE_RATIO = 0.4;

    public WatchWriteSquareView(Context context, AttributeSet set)
    {
        super(context, set);
    }

    public static TouchEvent getTouchEvent(double xrel, double yrel, int action, int multi)
    {
        if (multi >= 2)
        {
            return TouchEvent.MULTITOUCH;
        }
        if (action == MotionEvent.ACTION_DOWN
                || action == MotionEvent.ACTION_MOVE)
        {
            if (0 <= xrel && xrel <= 1 && 0 <= yrel && yrel <= 1)
            {
                if (yrel <= TOUCH_SIZE_RATIO)
                {
                    if (xrel <= TOUCH_SIZE_RATIO)
                    {
                        return TouchEvent.AREA1;
                    }
                    else if (xrel >= 1.0 - TOUCH_SIZE_RATIO)
                    {
                        return TouchEvent.AREA2;
                    }
                    else
                    {
                        return TouchEvent.AREA_OTHER;
                    }
                }
                else if (yrel >= 1.0 - TOUCH_SIZE_RATIO)
                {
                    if (xrel <= TOUCH_SIZE_RATIO)
                    {
                        return TouchEvent.AREA3;
                    }
                    else if (xrel >= 1.0 - TOUCH_SIZE_RATIO)
                    {
                        return TouchEvent.AREA4;
                    }
                    else
                    {
                        return TouchEvent.AREA_OTHER;
                    }
                }
                else
                {
                    return TouchEvent.AREA_OTHER;
                }
            }
            else
            {
                return TouchEvent.DROP;
            }
        }
        else
        {
            return TouchEvent.END;
        }
    }

    @Override
    public TouchEvent getTouchEventFromPos(double xrel, double yrel, int action, int multi)
    {
        return WatchWriteSquareView.getTouchEvent(xrel, yrel, action, multi);
    }
}
