package com.alanb.gesturetextinput;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class TouchFeedbackFrameLayout extends FrameLayout
{
    private TouchFeedbackView m_feedback_view;
    private ViewGroup m_parent_view = null;

    public TouchFeedbackFrameLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        m_feedback_view = new TouchFeedbackView(context);
        m_feedback_view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void attachFeedbackTo(ViewGroup view)
    {
        if (m_parent_view != null)
            m_parent_view.removeView(m_feedback_view);
        m_parent_view = view;
        view.addView(m_feedback_view);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent)
    {
        m_feedback_view.setCursorPos(motionEvent);
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent)
    {
        m_feedback_view.setCursorPos(motionEvent);

        for (int ci=0; ci<this.getChildCount(); ci++)
        {
            Rect hitRect = new Rect();
            (this.getChildAt(ci)).getHitRect(hitRect);
            MotionEvent child_event = MotionEvent.obtain(motionEvent);
            child_event.setLocation(child_event.getX() - hitRect.left, child_event.getY() - hitRect.top);
            (this.getChildAt(ci)).dispatchTouchEvent(child_event);
        }

        return true;
    }

    public void setCursor(float x, float y, int action)
    {
        m_feedback_view.setCursorPos(x, y, action);
    }
}
