package com.alanb.gesturetextinput;

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

    public static double vectorAngle(double x1, double y1, double x2, double y2)
    {
        return Math.acos((x1*x2 + y1*y2) / (Math.hypot(x1, y1) * Math.hypot(x2, y2)));
    }
}
