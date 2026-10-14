package com.dtinh.lichviet;

public final class MinimalLayoutTest {
    private static void check(boolean z, String str) {
        if (!z) {
            throw new AssertionError(str);
        }
    }

    public static void main(String[] strArr) {
        int i = 0;
        for (float f : new float[]{1.0f, 1.5f, 2.0f, 2.75f, 3.0f, 4.0f}) {
            for (int i2 : new int[]{280, 320, 360, 393, 412, 600, 800}) {
                for (int i3 : new int[]{320, 400, 480, 560, 640, 720, 900}) {
                    int iRound = Math.round(i2 * f);
                    int iRound2 = Math.round(i3 * f);
                    for (boolean z : new boolean[]{false, true}) {
                        MinimalLayout minimalLayout = new MinimalLayout(iRound, iRound2, f, z);
                        check(minimalLayout.header.bottom < minimalLayout.paper.top, "Header must clear paper");
                        check(minimalLayout.paper.bottom <= iRound2, "Paper must clear bottom inset");
                        int i4 = minimalLayout.paper.top;
                        for (int i5 = 0; i5 < minimalLayout.rows.length; i5++) {
                            MinimalLayout.Box box = minimalLayout.rows[i5];
                            check(box.left >= 0 && box.right <= iRound && box.top >= 0 && box.bottom <= iRound2, "Viewport bounds");
                            check(box.width() > 0 && box.height() >= 0, "Positive geometry");
                            check(box.top >= i4, "Rows must not overlap");
                            check(box.left >= minimalLayout.paper.left && box.right <= minimalLayout.paper.right && box.top >= minimalLayout.paper.top && box.bottom <= minimalLayout.paper.bottom, "Paper containment");
                            if (i5 != 3 || z) {
                                check(box.height() > 0, "Visible row cannot collapse");
                            }
                            i4 = box.bottom;
                        }
                        check(minimalLayout.header.height() >= Math.round(35.0f * f), "Header touch target");
                        check(minimalLayout.rows[1].height() > minimalLayout.rows[4].height(), "Primary day stays largest");
                        i++;
                    }
                    check(new MinimalLayout(iRound, iRound2, f, false).rows[1].height() >= new MinimalLayout(iRound, iRound2, f, true).rows[1].height(), "Unused holiday space returns to date");
                }
            }
        }
        System.out.println("Calendar layout tests passed: " + i + " viewport configurations");
    }
}
