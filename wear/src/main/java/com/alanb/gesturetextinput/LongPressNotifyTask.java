package com.alanb.gesturetextinput;

import android.os.AsyncTask;
import android.support.wearable.view.DismissOverlayView;

public class LongPressNotifyTask extends AsyncTask<LongPressNotifyTaskData, Void, Integer>
{
    private DismissOverlayView mDismissOverlayView;

    @Override
    protected Integer doInBackground(LongPressNotifyTaskData... data)
    {
        if (data.length != 1)
            return -1;

        mDismissOverlayView = data[0].dismissOverlayView;
        int state = 0;
        try
        {
            Thread.sleep(data[0].waitTime);
        }
        catch (InterruptedException e)
        {
            state = 1;
            Thread.currentThread().interrupt();
        }
        return state;
    }

    @Override
    protected void onPostExecute(Integer integer)
    {
        if (integer == 0)
            mDismissOverlayView.show();
    }
}