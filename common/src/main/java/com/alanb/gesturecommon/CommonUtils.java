package com.alanb.gesturecommon;

import android.content.Context;
import android.os.Build;

public class CommonUtils
{
    public static int getColorVersion(Context context, int id)
    {
        return context.getResources().getColor(id);
    }
}
