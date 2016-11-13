package com.alanb.gesturetextinput;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

import static android.content.Context.MODE_APPEND;
import static android.content.Context.MODE_PRIVATE;

public class TaskRecordWriter
{
    private final String TAG = this.getClass().getName();
    private final String TABLE_TOP = "layout,WPM,CER,NER,numC,numIF,numF,numINF," +
            "presented_str,input_str\n";
    private FileOutputStream m_recordStream = null;

    public class InfoBuilder
    {
        int layout_num;
        double input_time;
        int num_c, num_if, num_f, num_inf;
        String pres_str, inp_str;
        public InfoBuilder() {}
        public InfoBuilder setLayoutNum(int n) { this.layout_num = n; return this; }
        public InfoBuilder setInputTime(double t) { this.input_time = t; return this; }
        public InfoBuilder setNumC(int n) { this.num_c = n; return this; }
        public InfoBuilder setNumIf(int n) { this.num_if = n; return this; }
        public InfoBuilder setNumF(int n) { this.num_f = n; return this; }
        public InfoBuilder setNumInf(int n) { this.num_inf = n; return this; }
        public InfoBuilder setPresentedStr(String s) { this.pres_str = s; return this; }
        public InfoBuilder setInputStr(String s) { this.inp_str = s; return this; }
    }

    public TaskRecordWriter(Context context, Class<?> classname) throws java.io.IOException
    {
        String fname = context.getString(R.string.task_record_prefix) + this.getClass().getSimpleName() +
                    context.getString(R.string.task_record_ext);
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

    public void write(InfoBuilder info)
    {
        if (m_recordStream == null)
            return;

        // TODO: check if 'info' is valid?
        double wpm = 0.0;
        if (info.inp_str.length() >= 2 && !MathUtils.fequal(info.input_time, 0))
            wpm = 12.0 * (info.inp_str.length() - 1) / info.input_time;

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(info.layout_num);
        stringBuilder.append(",");
        stringBuilder.append(wpm);
        stringBuilder.append(",");
        stringBuilder.append(((double)(info.num_if)) / (info.num_c + info.num_if + info.num_inf));
        stringBuilder.append(",");
        stringBuilder.append(((double)(info.num_inf)) / (info.num_c + info.num_if + info.num_inf));
        stringBuilder.append(",");
        stringBuilder.append(info.num_c);
        stringBuilder.append(",");
        stringBuilder.append(info.num_if);
        stringBuilder.append(",");
        stringBuilder.append(info.num_f);
        stringBuilder.append(",");
        stringBuilder.append(info.num_inf);
        stringBuilder.append(",");
        stringBuilder.append(info.pres_str);
        stringBuilder.append(",");
        stringBuilder.append(info.inp_str);
        stringBuilder.append("\n");

        try
        {
            m_recordStream.write(stringBuilder.toString().getBytes());
        }
        catch (java.io.IOException e) { }
        Log.d(TAG, "record added: " + stringBuilder.toString());
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
