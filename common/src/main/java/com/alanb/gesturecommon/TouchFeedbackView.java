package com.alanb.gesturecommon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class TouchFeedbackView extends View
{
    private boolean m_activated = false;
    private float m_posx=100, m_posy=100, m_radius=50;
    private int m_color;
    private ShapeDrawable m_cursor;

    public void setCursorPos(MotionEvent motionEvent)
    {
        setCursorPos(motionEvent.getX(), motionEvent.getY(), motionEvent.getAction());
    }

    public void setCursorPos(float x, float y, int action)
    {
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)
        {
            m_activated = true;
            m_posx = x;
            m_posy = y;
        }
        else
        {
            m_activated = false;
        }

        invalidate();
    }

    public void setCursorRatio(float x, float y, int action)
    {
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)
        {
            m_activated = true;
            m_posx = x * this.getWidth();
            m_posy = y * this.getHeight();
        }
        else
        {
            m_activated = false;
        }

        invalidate();
    }

    public void setPointColor(int color) { m_color = color; }
    public void setRadius(float radius) { m_radius = radius; }

    public TouchFeedbackView(Context context)
    {
        this(context, null);
    }
    public TouchFeedbackView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        m_cursor = new ShapeDrawable(new OvalShape());
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if (m_activated)
        {
            m_cursor.getPaint().setColor(m_color);
            m_cursor.setBounds((int) (m_posx - m_radius), (int) (m_posy - m_radius),
                    (int) (m_posx + m_radius), (int) (m_posy + m_radius));
            m_cursor.draw(canvas);
        }
    }
}
