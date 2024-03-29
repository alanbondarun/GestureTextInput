package com.alanb.gesturecommon;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

public abstract class WatchWriteInputView extends LinearLayout
{
    public interface OnTouchListener
    {
        void onTouch(MotionEvent motionEvent);
    }
    public interface OnTouchEventListener
    {
        void onTouchEvent(TouchEvent te);
    }

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

    public abstract TouchEvent getTouchEventFromPos(double xrel, double yrel, int action, int multi);

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
