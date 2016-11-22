package com.alanb.gesturetextinput;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.alanb.gesturecommon.OneDInputView;

public class GlassOneDInputView extends OneDInputView
    implements View.OnAttachStateChangeListener
{
    private final String TAG = this.getClass().getName();

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
}
