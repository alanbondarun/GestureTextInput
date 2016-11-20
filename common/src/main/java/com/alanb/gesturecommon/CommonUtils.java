package com.alanb.gesturecommon;

import android.content.Context;
import android.os.Build;

public class CommonUtils
{
    public static int getColorVersion(Context context, int id)
    {
        final int version = Build.VERSION.SDK_INT;
        if (version >= Build.VERSION_CODES.M)
        {
            return context.getColor(id);
        }
        else
        {
            return context.getResources().getColor(id);
        }
    }
}
