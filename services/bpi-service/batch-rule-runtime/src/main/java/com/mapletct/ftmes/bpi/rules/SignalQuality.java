package com.mapletct.ftmes.bpi.rules;

public enum SignalQuality {
    GOOD(1.0),
    UNCERTAIN(0.5),
    SUBSTITUTED(0.3),
    BAD(0.0),
    STALE(0.0);

    private final double factor;

    SignalQuality(double factor) {
        this.factor = factor;
    }

    public double factor() {
        return factor;
    }
}
