package com.alanb.gesturecommon;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class OneDInputView extends LinearLayout
{
    public interface OnTouchListener
    {
        void onTouch(MotionEvent motionEvent);
    }
    public interface OnTouchEventListener
    {
        void onTouchEvent(TouchEvent te);
    }

    private final float AREA_WEIGHT = 1;
    private final float DEADZONE_WEIGHT = 0.2f;
    private final float TOTAL_WEIGHT = (AREA_WEIGHT*4 + DEADZONE_WEIGHT*3);

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

    private TouchEvent prev_e = TouchEvent.DROP;
    private boolean multi_occurred = false;

    public TouchEvent generateTouchEvent(MotionEvent motionEvent)
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
                // multi-touch detected, cancel the input
                multi_occurred = true;
                cur_e = TouchEvent.MULTITOUCH;
            }
            else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                    || motionEvent.getAction() == MotionEvent.ACTION_MOVE)
            {
                double xrel = motionEvent.getX() / m_touchW;
                double yrel = motionEvent.getY() / m_touchH;
                if (0 <= xrel && xrel <= 1 && 0 <= yrel && yrel <= 1)
                {
                    double dquo = Math.floor(xrel / ((AREA_WEIGHT + DEADZONE_WEIGHT) / TOTAL_WEIGHT));
                    double drem = xrel - dquo * ((AREA_WEIGHT + DEADZONE_WEIGHT) / TOTAL_WEIGHT);
                    if (drem < AREA_WEIGHT / TOTAL_WEIGHT)
                    {
                        if ((Math.round(dquo)) == 0)
                            cur_e = TouchEvent.AREA1;
                        else if ((Math.round(dquo)) == 1)
                            cur_e = TouchEvent.AREA2;
                        else if ((Math.round(dquo)) == 2)
                            cur_e = TouchEvent.AREA3;
                        else if ((Math.round(dquo)) == 3)
                            cur_e = TouchEvent.AREA4;
                        else
                            cur_e = TouchEvent.AREA_OTHER;
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
        return cur_e;
    }

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

        TouchEvent cur_e = generateTouchEvent(motionEvent);
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
