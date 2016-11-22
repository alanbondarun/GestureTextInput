package com.alanb.gesturecommon;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class OneDInputView extends LinearLayout
{
    public static class TouchEvent
    {
        public final static int DROP = -1;
        public final static int END = -2;
        public final static int MULTITOUCH = -3;
        public final int val;
        public TouchEvent(int v)
        {
            if (-3 <= v && v < 4)
            {
                this.val = v;
            }
            else
            {
                this.val = -1;
            }
        }
    }

    public interface OnTouchListener
    {
        void onTouch(MotionEvent motionEvent);
    }
    public interface OnTouchEventListener
    {
        void onTouchEvent(TouchEvent te);
    }

    private OnTouchListener m_onTouchListener;
    private OnTouchEventListener m_onTouchEventlistener;
    private double m_touchW = 0, m_touchH = 0;

    public OneDInputView(Context context, AttributeSet set)
    {
        super(context, set);
    }

    protected void setCustomTouchWidth(double w)
    {
        this.m_touchW = w;
    }

    protected void setCustomTouchHeight(double h)
    {
        this.m_touchH = h;
    }

    public void setOnTouchEventListener(OnTouchEventListener l)
    {
        this.m_onTouchEventlistener = l;
    }

    public void setOnTouchListener(OnTouchListener l)
    {
        this.m_onTouchListener = l;
    }

    private TouchEvent prev_e = new TouchEvent(TouchEvent.DROP);
    private boolean multi_occurred = false;

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent)
    {
        if (this.m_touchW <= 0)
        {
            this.m_touchW = getWidth();
        }
        if (this.m_touchH <= 0)
        {
            this.m_touchH = getHeight();
        }

        TouchEvent cur_e;
        if (multi_occurred && (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                || motionEvent.getAction() == MotionEvent.ACTION_MOVE))
        {
            cur_e = new TouchEvent(TouchEvent.MULTITOUCH);
        }
        else
        {
            multi_occurred = false;
            if (motionEvent.getPointerCount() >= 2)
            {
                // multi-touch detected, cancel the input
                multi_occurred = true;
                cur_e = new TouchEvent(TouchEvent.MULTITOUCH);
            }
            else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                    || motionEvent.getAction() == MotionEvent.ACTION_MOVE)
            {
                double xrel = motionEvent.getX() / m_touchW;
                double yrel = motionEvent.getY() / m_touchH;
                if (0 <= xrel && xrel <= 1 && 0 <= yrel && yrel <= 1)
                {
                    cur_e = new TouchEvent((int) (xrel * 4.0));
                }
                else
                {
                    cur_e = new TouchEvent(TouchEvent.DROP);
                }
            }
            else
            {
                cur_e = new TouchEvent(TouchEvent.END);
            }
        }
        if (cur_e.val != prev_e.val && this.m_onTouchEventlistener != null)
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
