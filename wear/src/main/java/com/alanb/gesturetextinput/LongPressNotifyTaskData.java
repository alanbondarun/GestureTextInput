package com.alanb.gesturetextinput;

import android.support.wearable.view.DismissOverlayView;

public class LongPressNotifyTaskData
{
    public final long waitTime;
    public final DismissOverlayView dismissOverlayView;
    LongPressNotifyTaskData(long waitTime, DismissOverlayView dismissOverlayView)
    {
        this.waitTime = waitTime;;
        this.dismissOverlayView = dismissOverlayView;
    }
}
