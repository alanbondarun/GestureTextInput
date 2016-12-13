package com.alanb.gesturetextinput;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.alanb.gesturecommon.CommonUtils;
import com.alanb.gesturecommon.MathUtils;
import com.alanb.gesturecommon.OneDInputView;
import com.alanb.gesturecommon.TouchEvent;

public class GlassOneDInputView extends OneDInputView
    implements View.OnAttachStateChangeListener
{
    private final String TAG = this.getClass().getName();
    private static final float SWIPE_POSDIFF_THRESH = 0.2f;

    public GlassOneDInputView(Context context, AttributeSet set)
    {
        super(context, set);
        setCustomTouchWidth(context.getResources().getInteger(R.integer.glass_touchpad_w));
        setCustomTouchHeight(context.getResources().getInteger(R.integer.glass_touchpad_h));
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override
    public void onViewAttachedToWindow(View v)
    {
        requestFocus();
    }

    @Override
    public void onViewDetachedFromWindow(View v)
    {
    }

    @Override
    public boolean dispatchGenericFocusedEvent(MotionEvent event)
    {
        if (isFocused())
        {
            super.onTouchEvent(event);
        }
        return super.dispatchGenericFocusedEvent(event);
    }

    @Override
    public TouchEvent generateTouchEvent(MotionEvent motionEvent)
    {
        TouchEvent parent_e = super.generateTouchEvent(motionEvent);
        double[] vel = CommonUtils.posDiffFromMotionEvents(motionEvent);
        if (Math.hypot(vel[0], vel[1]) >= SWIPE_POSDIFF_THRESH &&
                Math.abs(Math.atan2(vel[1], vel[0]) + Math.PI/2) <= (Math.PI/4 - 0.1))
        {
            parent_e = TouchEvent.DROP;
        }
        return parent_e;
    }
}
