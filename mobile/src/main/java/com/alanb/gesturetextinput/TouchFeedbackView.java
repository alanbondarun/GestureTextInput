package com.alanb.gesturetextinput;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.view.MotionEvent;
import android.view.View;

public class TouchFeedbackView extends View
{
    boolean m_activated = false;
    float m_posx=100, m_posy=100;

    public void setCursorPos(MotionEvent motionEvent)
    {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                || motionEvent.getAction() == MotionEvent.ACTION_MOVE)
        {
            m_activated = true;
            m_posx = motionEvent.getX();
            m_posy = motionEvent.getY();
        }
        else
        {
            m_activated = false;
        }

        invalidate();
    }

    public TouchFeedbackView(Context context)
    {
        super(context);
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if (m_activated)
        {
            ShapeDrawable cursor = new ShapeDrawable(new OvalShape());
            cursor.getPaint().setColor(Color.BLUE);
            cursor.setBounds((int) (m_posx - 50), (int) (m_posy - 50),
                    (int) (m_posx + 50), (int) (m_posy + 50));
            cursor.draw(canvas);
        }
    }
}
