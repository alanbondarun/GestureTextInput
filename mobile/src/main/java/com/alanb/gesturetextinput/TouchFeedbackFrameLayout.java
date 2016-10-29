package com.alanb.gesturetextinput;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
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
}
