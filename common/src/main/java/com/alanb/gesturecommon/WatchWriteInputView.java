package com.alanb.gesturecommon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class WatchWriteInputView extends View
{
    public enum TouchEvent
    {
        AREA1, AREA2, AREA3, AREA4, AREA_OTHER, DROP, END, MULTITOUCH
    }

    private final double TOUCH_SIZE_RATIO = 0.4;
    private Builder m_prefs;
    private TouchEvent prev_e = TouchEvent.AREA_OTHER;
    private boolean multi_occurred = false;

    public interface OnTouchListener
    {
        public void onTouch(MotionEvent motionEvent);
    }
    public interface OnTouchEventListener
    {
        public void onTouchEvent(TouchEvent te);
    }

    public static class Builder
    {
        private Context context = null;
        private OnTouchEventListener listener = null;
        private WatchWriteInputView.OnTouchListener t_listener = null;
        private Drawable background = null;

        public Builder(Context context)
        {
            this.context = context;
        }

        public void setOnTouchEventListener(OnTouchEventListener l)
        {
            this.listener = l;
        }

        public void setOnTouchListener(WatchWriteInputView.OnTouchListener l)
        {
            this.t_listener = l;
        }

        public void setBackground(int id)
        {
            background = context.getResources().getDrawable(id, context.getTheme());
        }

        public WatchWriteInputView build()
        {
            if (listener == null)
                return null;
            if (background == null)
                return null;
            return new WatchWriteInputView(this);
        }
    }

    public WatchWriteInputView(Context context)
    {
        super(context);
    }

    public WatchWriteInputView(Builder builder)
    {
        super(builder.context);
        this.m_prefs = builder;
        this.setBackground(m_prefs.background);
        this.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
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
        if (cur_e != prev_e)
        {
            this.m_prefs.listener.onTouchEvent(cur_e);
        }
        prev_e = cur_e;

        if (this.m_prefs.t_listener != null)
        {
            this.m_prefs.t_listener.onTouch(motionEvent);
        }
        return true;
    }
}