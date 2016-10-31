package com.alanb.gesturetextinput;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class TouchFeedbackView extends View
{
    boolean m_activated = false;
    float m_posx=100, m_posy=100;

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

    public TouchFeedbackView(Context context)
    {
        super(context, null);
    }
    public TouchFeedbackView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if (m_activated)
        {
            ShapeDrawable cursor = new ShapeDrawable(new OvalShape());
            cursor.getPaint().setColor(Color.argb(85, 0, 0, 0));
            cursor.setBounds((int) (m_posx - 50), (int) (m_posy - 50),
                    (int) (m_posx + 50), (int) (m_posy + 50));
            cursor.draw(canvas);
        }
    }
}
