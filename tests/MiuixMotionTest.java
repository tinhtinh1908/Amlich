package com.dtinh.lichviet;

public final class MiuixMotionTest {
    public static void main(String[] strArr) {
        MiuixMotion miuixMotion = new MiuixMotion(0.7d, 987.0d, 0.25d);
        if (miuixMotion.getInterpolation(0.0f) != 0.0f || miuixMotion.getInterpolation(1.0f) != 1.0f) {
            throw new AssertionError("Spring endpoints");
        }
        float fMax = 0.0f;
        for (int i = 0; i <= 1000; i++) {
            float interpolation = miuixMotion.getInterpolation(i / 1000.0f);
            if (!Float.isFinite(interpolation) || interpolation < -0.01f || interpolation > 1.12f) {
                throw new AssertionError("Unstable motion " + interpolation);
            }
            fMax = Math.max(fMax, interpolation);
        }
        if (fMax < 1.0f) {
            throw new AssertionError("Release did not settle");
        }
        System.out.println("Miuix spring endpoint and stability checks passed");
    }
}
