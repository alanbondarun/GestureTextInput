package com.alanb.gesturetextinput;

public class NanoTimer
{
    private long start = 0;
    private long finish = 0;
    private boolean running = false;

    public void begin() { this.start = System.nanoTime(); this.finish = this.start; this.running = true; }
    public void check() { this.finish = System.nanoTime(); }
    public void end() { this.running = false; }
    public boolean running() { return running; }
    public long getDiff()
    {
        if (this.running)
            return System.nanoTime() - this.start;
        return this.finish - this.start;
    }
    public double getDiffInSeconds()
    {
        return getDiff() / (double)(1.0e9);
    }
}
