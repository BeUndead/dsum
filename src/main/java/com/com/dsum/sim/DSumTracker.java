package com.com.dsum.sim;

public class DSumTracker {

    private volatile double dsum = 0.0d;

    public void advance(final double delta) {
        this.dsum = dsum + delta;
    }

    public double getDSum() {
        return dsum;
    }

    public void setDSum(final double dsum) {
        this.dsum = dsum;
    }
}
