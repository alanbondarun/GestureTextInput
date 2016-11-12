package com.alanb.gesturetextinput;

/**
 * Created by alanb on 2016-11-13.
 */

public class MathUtils
{
    public static boolean fequal(double f1, double f2)
    {
        final double e = 1e-7F;
        double abs_1 = Math.abs(f1);
        double abs_2 = Math.abs(f2);
        double diff = Math.abs(f1 - f2);

        if (f1 == f2)
        {
            return true;
        }
        else if (f1 == 0 || f2 == 0 || diff < Double.MIN_VALUE)
        {
            return diff < e * Double.MIN_VALUE;
        }
        else
        {
            return diff / Math.min(abs_1 + abs_2, Double.MAX_VALUE) < e;
        }
    }
}
