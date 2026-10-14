package com.dtinh.lichviet;

final class MinimalLayout {
    static final int MONTH = 0;
    static final int DAY = 1;
    static final int WEEKDAY = 2;
    static final int HOLIDAY = 3;
    static final int LUNAR = 4;
    static final int GUARDIAN = 5;
    static final int DETAIL = 6;
    static final int HOUR_TITLE = 7;
    static final int HOURS = 8;
    static final int NOTE = 9;
    final Box header;
    final Box paper;
    final Box[] rows = new Box[10];
    final int gap;

    static final class Box {
        final int left;
        final int top;
        final int right;
        final int bottom;

        Box(int i, int i2, int i3, int i4) {
            this.left = i;
            this.top = i2;
            this.right = i3;
            this.bottom = i4;
        }

        int width() {
            return this.right - this.left;
        }

        int height() {
            return this.bottom - this.top;
        }
    }

    MinimalLayout(int i, int i2, float f, boolean z) {
        int iMin = Math.min(Math.round(10.0f * f), Math.min(i, i2) / 24);
        int iMin2 = Math.min(i - (iMin * WEEKDAY), Math.round(520.0f * f));
        int i3 = (i - iMin2) / WEEKDAY;
        int i4 = i3 + iMin2;
        this.gap = Math.max(DAY, Math.min(Math.round(6.0f * f), i2 / 100));
        this.header = new Box(i3, iMin, i4, iMin + Math.min(Math.round(44.0f * f), i2 / NOTE));
        this.paper = new Box(i3, this.header.bottom + this.gap, i4, i2 - iMin);
        int iMin3 = Math.min(Math.round(12.0f * f), Math.min(this.paper.width(), this.paper.height()) / 18);
        int i5 = this.paper.top + iMin3;
        int i6 = this.paper.bottom - iMin3;
        int[] iArr = {30, 146, 30, 30, 96, 36, 44, 24, 86, 44};
        int i7 = MONTH;
        int length = iArr.length;
        for (int i8 = MONTH; i8 < length; i8 += DAY) {
            i7 += iArr[i8];
        }
        int i9 = MONTH;
        for (int i10 = MONTH; i10 < iArr.length; i10 += DAY) {
            int i11 = i5 + (((i6 - i5) * i9) / i7);
            i9 += iArr[i10];
            this.rows[i10] = new Box(i3 + iMin3, i11, i4 - iMin3, i5 + (((i6 - i5) * i9) / i7));
        }
    }
}
