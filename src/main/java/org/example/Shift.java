package org.example;

public class Shift {
    long timeIn;
    long timeOut;
    Shift(long timeIn,long timeOut)
    {
        this.timeIn =timeIn;
        this.timeOut = timeOut;
    }
    public long getTimeIn()
    {
        return timeIn;
    }
}
