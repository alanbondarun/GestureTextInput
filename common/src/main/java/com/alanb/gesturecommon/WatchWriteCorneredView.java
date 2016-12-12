package com.alanb.gesturecommon;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class WatchWriteCorneredView extends LinearLayout
{
    public interface OnTouchListener
    {
        void onTouch(MotionEvent motionEvent);
    }
    public interface OnTouchEventListener
    {
        void onTouchEvent(TouchEvent te);
    }

    private static final double TOUCH_SIZE_RATIO = 0.4;
    private static final double CENTER_DZONE_RATIO = 0.28;
    private TouchEvent prev_e = TouchEvent.AREA_OTHER;
    private boolean multi_occurred = false;

    private OnTouchListener m_onTouchListener;
    private OnTouchEventListener m_onTouchEventlistener;

    public WatchWriteCorneredView(Context context, AttributeSet set)
    {
        super(context, set);
    }

    public void setOnTouchEventListener(OnTouchEventListener l)
    {
        this.m_onTouchEventlistener = l;
    }

    public void setOnTouchListener(WatchWriteCorneredView.OnTouchListener l)
    {
        this.m_onTouchListener = l;
    }

    public static TouchEvent getTouchEventFromPos(double xrel, double yrel, int action, int multi)
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
                        if (xrel + yrel <= 1 - CENTER_DZONE_RATIO)
                            return TouchEvent.AREA1;
                        return TouchEvent.AREA_OTHER;
                    }
                    else if (xrel >= 1.0 - TOUCH_SIZE_RATIO)
                    {
                        if (xrel - yrel >= CENTER_DZONE_RATIO)
                            return TouchEvent.AREA2;
                        return TouchEvent.AREA_OTHER;
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
                        if (yrel - xrel >= CENTER_DZONE_RATIO)
                            return TouchEvent.AREA3;
                        return TouchEvent.AREA_OTHER;
                    }
                    else if (xrel >= 1.0 - TOUCH_SIZE_RATIO)
                    {
                        if (xrel + yrel >= 1 + CENTER_DZONE_RATIO)
                            return TouchEvent.AREA4;
                        return TouchEvent.AREA_OTHER;
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
    public boolean onTouchEvent(MotionEvent motionEvent)
    {
        TouchEvent cur_e;
        if (multi_occurred && (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                || motionEvent.getAction() == MotionEvent.ACTION_MOVE))
        {
            cur_e = TouchEvent.MULTITOUCH;
        }
        else
        {
            multi_occurred = false;
            if (motionEvent.getPointerCount() >= 2)
            {
                // multi-touch occurred, cancel the input
                multi_occurred = true;
                cur_e = TouchEvent.MULTITOUCH;
            }
            else
            {
                cur_e = getTouchEventFromPos(motionEvent.getX() / getWidth(), motionEvent.getY() / getHeight(),
                        motionEvent.getAction(), motionEvent.getPointerCount());
            }
        }
        if (cur_e != prev_e && this.m_onTouchEventlistener != null)
        {
            this.m_onTouchEventlistener.onTouchEvent(cur_e);
            prev_e = cur_e;
        }

        if (this.m_onTouchListener != null)
        {
            this.m_onTouchListener.onTouch(motionEvent);
        }
        return true;
    }
}
