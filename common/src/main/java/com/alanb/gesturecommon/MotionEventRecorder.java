package com.alanb.gesturecommon;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import java.io.File;
import java.io.FileOutputStream;

public class MotionEventRecorder
{
    private final String TAG = this.getClass().getName();
    private final String TABLE_TOP = "timestamp,posx,posy,action\n";
    private FileOutputStream m_recordStream = null;

    public MotionEventRecorder(Context context, Class<?> classname) throws java.io.IOException
    {
        String fname = context.getString(R.string.motion_record_prefix) + classname.getSimpleName() +
                context.getString(R.string.motion_record_ext);
        File recordFile = new File(context.getExternalFilesDir(null), fname);
        if (recordFile.exists())
        {
            m_recordStream = new FileOutputStream(recordFile, true);
        }
        else
        {
            m_recordStream = new FileOutputStream(recordFile);
            m_recordStream.write(TABLE_TOP.getBytes());
        }
    }

    public void write(MotionEvent event)
    {
        if (m_recordStream == null)
            return;

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(event.getEventTime());
        stringBuilder.append(",");
        stringBuilder.append(event.getX());
        stringBuilder.append(",");
        stringBuilder.append(event.getY());
        stringBuilder.append(",");
        stringBuilder.append(event.getAction());
        stringBuilder.append("\n");

        try
        {
            m_recordStream.write(stringBuilder.toString().getBytes());
        }
        catch (java.io.IOException e) { e.printStackTrace(); }
        Log.d(TAG, "motion record added: " + stringBuilder.toString());
    }

    public void close()
    {
        if (m_recordStream == null)
            return;
        try
        {
            m_recordStream.close();
        }
        catch (java.io.IOException e) { }
        m_recordStream = null;
    }
}
