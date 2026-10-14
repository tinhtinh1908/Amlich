package com.dtinh.lichviet;

public final class MiuixMetricsTest {
    public static void main(String[] args) {
        int cases = 0;
        for (float width : new float[]{1, 16, 32, 40, 88, 144, 280, 360, 840}) {
            for (float height : new float[]{1, 16, 28, 32, 40, 56, 72, 216, 800}) {
                for (float radius : new float[]{-1, 0, 2, 8, 13, 16, 32, 120}) {
                    float tile = MiuixMetrics.cornerTile(width, height, radius);
                    float handle = MiuixMetrics.cornerHandle(tile);
                    if (!Float.isFinite(tile) || tile < 0 || tile > Math.min(width, height) / 2)
                        throw new AssertionError("Corner leaves surface bounds");
                    if (handle < 0 || handle > tile) throw new AssertionError("Invalid cubic control");
                    if (tile != MiuixMetrics.cornerTile(height, width, radius))
                        throw new AssertionError("Corner changes on rotation");
                    cases++;
                }
            }
        }
        if (Math.abs(MiuixMetrics.cornerTile(120, 40, 16) - 17.6f) > .0001f)
            throw new AssertionError("Miuix extension differs from upstream");
        if (Math.abs(MiuixMetrics.cornerHandle(17.6f) - 6.2832f) > .0001f)
            throw new AssertionError("Miuix control differs from upstream");
        System.out.println("Miuix continuous corners: " + cases + " bounded symmetric cases passed");
    }
}
