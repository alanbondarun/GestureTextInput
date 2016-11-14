package com.alanb.gesturecommon;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class WatchWriteInputView extends View
{
    public enum TouchEvent
    {
        AREA1, AREA2, AREA3, AREA4, AREA_OTHER, DROP, END, MULTITOUCH
    }

    public interface OnTouchListener
    {
        void onTouch(MotionEvent motionEvent);
    }
    public interface OnTouchEventListener
    {
        void onTouchEvent(TouchEvent te);
    }

    private final double TOUCH_SIZE_RATIO = 0.4;
    private TouchEvent prev_e = TouchEvent.AREA_OTHER;
    private boolean multi_occurred = false;

    private OnTouchListener m_onTouchListener;
    private OnTouchEventListener m_onTouchEventlistener;

    public WatchWriteInputView(Context context, AttributeSet set)
    {
        super(context, set);
    }

    public void setOnTouchEventListener(OnTouchEventListener l)
    {
        this.m_onTouchEventlistener = l;
    }

    public void setOnTouchListener(WatchWriteInputView.OnTouchListener l)
    {
        this.m_onTouchListener = l;
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
            else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                    || motionEvent.getAction() == MotionEvent.ACTION_MOVE)
            {
                double xrel = motionEvent.getX() / this.getWidth();
                double yrel = motionEvent.getY() / this.getHeight();
                if (0 <= xrel && xrel <= 1 && 0 <= yrel && yrel <= 1)
                {
                    if (yrel <= TOUCH_SIZE_RATIO)
                    {
                        if (xrel <= TOUCH_SIZE_RATIO)
                        {
                            cur_e = TouchEvent.AREA1;
                        }
                        else if (xrel >= 1.0 - TOUCH_SIZE_RATIO)
                        {
                            cur_e = TouchEvent.AREA2;
                        }
                        else
                        {
                            cur_e = TouchEvent.AREA_OTHER;
                        }
                    }
                    else if (yrel >= 1.0 - TOUCH_SIZE_RATIO)
                    {
                        if (xrel <= TOUCH_SIZE_RATIO)
                        {
                            cur_e = TouchEvent.AREA3;
                        }
                        else if (xrel >= 1.0 - TOUCH_SIZE_RATIO)
                        {
                            cur_e = TouchEvent.AREA4;
                        }
                        else
                        {
                            cur_e = TouchEvent.AREA_OTHER;
                        }
                    }
                    else
                    {
                        cur_e = TouchEvent.AREA_OTHER;
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
