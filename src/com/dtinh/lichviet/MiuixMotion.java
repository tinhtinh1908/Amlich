package com.dtinh.lichviet;

import android.animation.TimeInterpolator;

final class MiuixMotion implements TimeInterpolator {
    private final double damping;
    private final double frequency;
    private final double seconds;
    private final double end;

    MiuixMotion(double d, double d2, double d3) {
        this.damping = d;
        this.frequency = Math.sqrt(d2);
        this.seconds = d3;
        this.end = value(1.0d);
    }

    private double value(double d) {
        double d2 = d * this.seconds;
        double dSqrt = this.frequency * Math.sqrt(1.0d - (this.damping * this.damping));
        return 1.0d - (Math.exp(((-this.damping) * this.frequency) * d2) * (Math.cos(dSqrt * d2) + (((this.damping * this.frequency) / dSqrt) * Math.sin(dSqrt * d2))));
    }

    @Override
    public float getInterpolation(float f) {
        if (f <= 0.0f) {
            return 0.0f;
        }
        if (f >= 1.0f) {
            return 1.0f;
        }
        return (float) (value(f) / this.end);
    }
}
