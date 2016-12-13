package com.alanb.gesturecommon;

import android.os.AsyncTask;

public class BreakTask extends AsyncTask<Integer, Void, Void>
{
    public interface TaskEndListener
    {
        public void onFinish();
    }

    private TaskEndListener mListener;

    public void setTaskEndListener(TaskEndListener listener)
    {
        this.mListener = listener;
    }

    @Override
    protected Void doInBackground(Integer... params)
    {
        if (params.length != 1)
        {
            return null;
        }

        int waitTime  = params[0];
        try
        {
            Thread.sleep(waitTime);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void v)
    {
        mListener.onFinish();
    }
}
