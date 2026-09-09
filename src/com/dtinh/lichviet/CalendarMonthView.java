package com.dtinh.lichviet;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public final class CalendarMonthView extends View {
    public long getSelectedDateMillis() { return selected.getTimeInMillis(); }
    public int getSelectedYear() { return selected.get(Calendar.YEAR); }



    private static final Locale VIETNAMESE_LOCALE = new Locale("vi", "VN");
    private static final String[] WEEKDAYS = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
    private int accent;
    private int accentSoft;
    private int background;
    private Bitmap backgroundImage;
    private boolean customBackground;
    private boolean customBackgroundDark;
    private int controlDivider;
    private int controlPrimary;
    private int controlSecondary;
    private final RectF settingsButton;
    private float cellWidth;
    private final List<DayCell> dayCells;
    private final float density;
    private final RectF detailPanel;
    private final Calendar displayed;
    private int divider;
    private float downX;
    private float downY;
    private float gridBottom;
    private float gridTop;
    private int insetBottom;
    private int insetTop;
    private final Paint.FontMetrics metrics;
    private boolean moved;
    private final RectF nextDayButton;
    private final RectF nextMonthButton;
    private final RectF noteHitArea;
    private Set<String> noteKeys;
    private final Paint paint;
    private final RectF pickerButton;
    private final RectF previousDayButton;
    private final RectF previousMonthButton;
    private float rowHeight;
    private final Calendar selected;
    private ValueAnimator selectionAnimator;
    private float selectionPulse;
    private ValueAnimator monthAnimator;
    private float monthProgress = 1.0f;
    private int monthDirection;
    private Calendar previousDisplayed;
    private Calendar previousSelected;
    private int sunday;
    private int surface;
    private int surfaceSoft;
    private int textMuted;
    private int textPrimary;
    private int textSecondary;
    private Calendar today;
    private final RectF todayButton;
    private int visibleRows;
    private float weekTop;

    public CalendarMonthView(Context context) {
        super(context);
        this.paint = new Paint(1);
        this.metrics = new Paint.FontMetrics();
        Calendar calendar = Calendar.getInstance();
        this.displayed = calendar;
        Calendar calendar2 = Calendar.getInstance();
        this.selected = calendar2;
        this.today = Calendar.getInstance();
        this.dayCells = new ArrayList<>(42);
        this.noteKeys = NoteRepository.snapshotKeys(context);
        this.previousMonthButton = new RectF();
        this.nextMonthButton = new RectF();
        this.previousDayButton = new RectF();
        this.todayButton = new RectF();
        this.nextDayButton = new RectF();
        this.settingsButton = new RectF();
        this.pickerButton = new RectF();
        this.detailPanel = new RectF();
        this.noteHitArea = new RectF();
        this.density = getResources().getDisplayMetrics().density;
        loadPalette();
        calendar.set(5, 1);
        normalize(calendar);
        normalize(calendar2);
        normalize(this.today);
        setBackgroundColor(this.background);
        setFocusable(true);
        setContentDescription("Lịch tháng Việt Nam");
    }

    private void loadPalette() {
        UiKit.Palette palette = new UiKit.Palette(getContext());
        this.background = palette.background;
        this.surface = palette.surface;
        this.surfaceSoft = palette.surfaceSoft;
        this.divider = palette.divider;
        this.textPrimary = palette.primary;
        this.textSecondary = palette.secondary;
        this.textMuted = palette.muted;
        this.controlPrimary = palette.primary;
        this.controlSecondary = palette.secondary;
        this.controlDivider = palette.divider;
        this.accent = palette.accent;
        this.accentSoft = palette.accentSoft;
        this.sunday = palette.danger;
        this.customBackground = BackgroundImageManager.hasBackground(getContext());
        // The photo and calendar labels use their own high-contrast palette.
        // Cards and controls still follow the selected light/dark app theme.
        this.customBackgroundDark = palette.night;
        if (this.customBackground) {
            boolean frosted = BackgroundImageManager.isFrostedEnabled(getContext());
            if (this.customBackgroundDark) {
                this.background = frosted
                        ? Color.argb(132, 13, 16, 21) : Color.rgb(13, 16, 21);
                this.surface = frosted
                        ? Color.argb(158, 27, 31, 39) : Color.argb(232, 27, 31, 39);
                this.surfaceSoft = frosted
                        ? Color.argb(138, 37, 43, 54) : Color.argb(224, 37, 43, 54);
                this.divider = Color.argb(72, 255, 255, 255);
                this.textPrimary = Color.WHITE;
                this.textSecondary = Color.rgb(210, 218, 230);
                this.textMuted = Color.rgb(116, 128, 145);
                this.controlPrimary = this.textPrimary;
                this.controlSecondary = this.textSecondary;
                this.controlDivider = this.divider;
                this.accent = frosted
                        ? Color.argb(210, 99, 158, 247) : Color.rgb(99, 158, 247);
                this.accentSoft = frosted
                        ? Color.argb(150, 48, 70, 104) : Color.rgb(48, 70, 104);
                this.sunday = Color.rgb(255, 126, 135);
            } else {
                this.background = Color.rgb(247, 248, 252);
                this.surface = frosted
                        ? Color.argb(218, 255, 255, 255) : Color.rgb(255, 255, 255);
                this.surfaceSoft = frosted
                        ? Color.argb(205, 239, 242, 247) : Color.rgb(239, 242, 247);
                this.divider = Color.argb(96, 255, 255, 255);
                this.textPrimary = Color.WHITE;
                this.textSecondary = Color.rgb(218, 224, 234);
                this.textMuted = Color.rgb(142, 150, 164);
                this.controlPrimary = Color.rgb(24, 27, 34);
                this.controlSecondary = Color.rgb(82, 88, 101);
                this.controlDivider = Color.argb(78, 24, 27, 34);
                this.accent = frosted
                        ? Color.argb(214, 66, 133, 244) : Color.rgb(66, 133, 244);
                this.accentSoft = frosted
                        ? Color.argb(166, 225, 235, 252) : Color.rgb(225, 235, 252);
                this.sunday = Color.rgb(255, 126, 135);
            }
        }
    }

    public void refreshTheme() {
        loadPalette();
        reloadBackgroundImage(getWidth(), getHeight());
        setBackgroundColor(this.background);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (width != oldWidth || height != oldHeight) {
            reloadBackgroundImage(width, height);
        }
    }

    private void reloadBackgroundImage(int width, int height) {
        Bitmap replacement = this.customBackground
                ? BackgroundImageManager.load(getContext(), width, height) : null;
        if (this.backgroundImage != null && this.backgroundImage != replacement
                && !this.backgroundImage.isRecycled()) {
            this.backgroundImage.recycle();
        }
        this.backgroundImage = replacement;
    }

    public void setSystemInsets(int top, int bottom) {
        int safeTop = Math.max(0, top);
        int safeBottom = Math.max(0, bottom);
        if (safeTop == this.insetTop && safeBottom == this.insetBottom) return;
        this.insetTop = safeTop;
        this.insetBottom = safeBottom;
        invalidate();
    }

    public void refreshToday() {
        Calendar calendar = Calendar.getInstance();
        normalize(calendar);
        Set<String> latestNoteKeys = NoteRepository.snapshotKeys(getContext());
        boolean changed = !latestNoteKeys.equals(this.noteKeys);
        this.noteKeys = latestNoteKeys;
        if (!sameDate(calendar, this.today)) {
            this.today = calendar;
            changed = true;
        }
        if (changed) invalidate();
    }

    public void showDate(long j) {
        Calendar oldMonth = (Calendar) this.displayed.clone();
        Calendar oldSelection = (Calendar) this.selected.clone();
        this.selected.setTimeInMillis(j);
        normalize(this.selected);
        this.displayed.setTimeInMillis(this.selected.getTimeInMillis());
        this.displayed.set(5, 1);
        normalize(this.displayed);
        startTransitionIfMonthChanged(oldMonth, oldSelection);
        startSelectionAnimation();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        calculateLayout();
        drawCalendarBase(canvas);
        drawHeaderButtons(canvas);
        drawDetailPanel(canvas, this.selected);
        drawBottomControls(canvas);
    }

    private void drawCalendarBase(Canvas canvas) {
        this.paint.setStyle(Paint.Style.FILL);
        if (this.backgroundImage != null && !this.backgroundImage.isRecycled()) {
            BackgroundImageManager.drawCenterCrop(canvas, this.backgroundImage,
                    new RectF(0.0f, 0.0f, getWidth(), getHeight()), this.paint);
            this.paint.setColor(Color.argb(112, 0, 0, 0));
            canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), this.paint);
        } else {
            this.paint.setColor(this.background);
            canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), this.paint);
        }
        this.paint.setShader(null);

        drawHeaderTitle(canvas, this.displayed);
        drawWeekdays(canvas);
        drawMonthGridTransition(canvas);
    }

    private void drawMonthGridTransition(Canvas canvas) {
        if (!isMonthTransitionRunning()) {
            drawMonthGrid(canvas, this.displayed, this.selected);
            return;
        }
        float width = getWidth();
        float progress = this.monthProgress;
        drawMonthGridLayer(canvas, this.previousDisplayed, this.previousSelected,
                -this.monthDirection * width * progress);
        drawMonthGridLayer(canvas, this.displayed, this.selected,
                this.monthDirection * width * (1.0f - progress));
    }

    private void drawMonthGridLayer(Canvas canvas, Calendar month, Calendar selection,
                                    float translationX) {
        int save = canvas.save();
        canvas.clipRect(0.0f, this.gridTop, getWidth(), this.gridBottom);
        canvas.translate(translationX, 0.0f);
        drawMonthGrid(canvas, month, selection);
        canvas.restoreToCount(save);
    }

    private boolean isMonthTransitionRunning() {
        return this.previousDisplayed != null && this.previousSelected != null
                && this.monthProgress < 1.0f;
    }

    private void drawMonthGrid(Canvas canvas, Calendar month, Calendar selectedDate) {
        buildCells(month);
        drawDays(canvas, selectedDate);
    }

    private void calculateLayout() {
        float width = getWidth();
        this.cellWidth = width / 7.0f;
        float contentTop = this.insetTop;
        float contentBottom = getHeight() - this.insetBottom;
        float headerHeight = contentTop + dp(62.0f);
        this.weekTop = headerHeight;
        this.gridTop = headerHeight + dp(29.0f);
        Calendar calendar = (Calendar) this.displayed.clone();
        this.visibleRows = Math.max(5, Math.min(6, ((((calendar.get(7) + 5) % 7) + calendar.getActualMaximum(5)) + 6) / 7));
        float fMax = Math.max(dp(46.0f), Math.min(dp(58.0f),
                ((contentBottom - this.gridTop) - dp(294.0f)) / this.visibleRows));
        this.rowHeight = fMax;
        this.gridBottom = this.gridTop + (fMax * this.visibleRows);
        float height = contentBottom - dp(62.0f);
        float fM0dp2 = dp(50.0f);
        RectF rectF = new RectF(dp(78.0f), height, width - dp(78.0f), contentBottom - dp(12.0f));
        float fWidth = rectF.width() / 3.0f;
        this.previousDayButton.set(rectF.left, rectF.top, rectF.left + fWidth, rectF.bottom);
        float f = fWidth * 2.0f;
        this.todayButton.set(rectF.left + fWidth, rectF.top, rectF.left + f, rectF.bottom);
        this.nextDayButton.set(rectF.left + f, rectF.top, rectF.right, rectF.bottom);
        float f2 = fM0dp2 + height;
        this.pickerButton.set(dp(14.0f), height, dp(64.0f), f2);
        this.settingsButton.set(width - dp(64.0f), height, width - dp(14.0f), f2);
        float fM0dp3 = height - dp(10.0f);
        this.detailPanel.set(dp(12.0f), Math.max(this.gridBottom + dp(10.0f), fM0dp3 - dp(214.0f)), width - dp(12.0f), fM0dp3);
        float fM0dp4 = dp(40.0f);
        this.previousMonthButton.set(dp(14.0f), contentTop + dp(10.0f),
                dp(14.0f) + fM0dp4, contentTop + dp(10.0f) + fM0dp4);
        this.nextMonthButton.set(width - dp(54.0f), contentTop + dp(10.0f),
                width - dp(14.0f), contentTop + dp(10.0f) + fM0dp4);
    }

    private void drawHeaderTitle(Canvas canvas, Calendar calendar) {
        String str = "Tháng " + (calendar.get(2) + 1) + ", " + calendar.get(1);
        setText(dp(22.0f), this.textPrimary, 1, Paint.Align.CENTER);
        canvas.drawText(str, getWidth() / 2.0f,
                this.insetTop + dp(36.0f), this.paint);
    }

    private void drawHeaderButtons(Canvas canvas) {
        drawCircleButton(canvas, this.previousMonthButton, false);
        drawCircleButton(canvas, this.nextMonthButton, true);
    }

    private void drawCircleButton(Canvas canvas, RectF rectF, boolean z) {
        drawSurface(canvas, rectF, dp(15.0f), this.surface);
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(dp(1.8f));
        this.paint.setStrokeCap(Paint.Cap.ROUND);
        this.paint.setColor(this.controlPrimary);
        float fCenterX = rectF.centerX();
        float fCenterY = rectF.centerY();
        float f = z ? 1.0f : -1.0f;
        canvas.drawLine(fCenterX - (dp(3.0f) * f), fCenterY - dp(5.0f), fCenterX + (dp(3.0f) * f), fCenterY, this.paint);
        canvas.drawLine(fCenterX + (dp(3.0f) * f), fCenterY, fCenterX - (f * dp(3.0f)), fCenterY + dp(5.0f), this.paint);
        this.paint.setStyle(Paint.Style.FILL);
    }

    private void drawWeekdays(Canvas canvas) {
        int i = 0;
        while (i < 7) {
            setText(dp(11.5f), i == 6 ? this.sunday : this.textSecondary, 1, Paint.Align.CENTER);
            String str = WEEKDAYS[i];
            float f = this.cellWidth;
            canvas.drawText(str, (i * f) + (f / 2.0f),
                    baselineCenter(this.weekTop, dp(27.0f)), this.paint);
            i++;
        }
        this.paint.setColor(this.divider);
        this.paint.setStrokeWidth(dp(0.7f));
        canvas.drawLine(dp(15.0f), this.gridTop - dp(1.0f), getWidth() - dp(15.0f), this.gridTop - dp(1.0f), this.paint);
    }

    private void buildCells(Calendar calendar) {
        this.dayCells.clear();
        Calendar calendar2 = (Calendar) calendar.clone();
        calendar2.add(5, -((calendar2.get(7) + 5) % 7));
        int i = this.visibleRows * 7;
        for (int i2 = 0; i2 < i; i2++) {
            int i3 = i2 / 7;
            int i4 = i2 % 7;
            float f = this.cellWidth;
            float f2 = this.gridTop;
            float f3 = this.rowHeight;
            RectF rectF = new RectF(i4 * f, (i3 * f3) + f2, (i4 + 1) * f, f2 + ((i3 + 1) * f3));
            Calendar calendar3 = (Calendar) calendar2.clone();
            this.dayCells.add(new DayCell(calendar3, calendar3.get(2) == calendar.get(2) && calendar3.get(1) == calendar.get(1), rectF));
            calendar2.add(5, 1);
        }
    }

    private void drawDays(Canvas canvas, Calendar calendar) {
        Iterator<DayCell> it;
        int i;
        int i2;
        for (Iterator<DayCell> it2 = this.dayCells.iterator(); it2.hasNext(); it2 = it) {
            DayCell next = it2.next();
            Calendar calendar2 = next.date;
            int i3 = calendar2.get(5);
            int i4 = calendar2.get(2) + 1;
            LunarCalendar.LunarDate lunarDateFromSolar = LunarCalendar.fromSolar(i3, i4, calendar2.get(1));
            String holiday = HolidayUtil.getHoliday(i3, i4, lunarDateFromSolar);
            boolean zHas = NoteRepository.has(this.noteKeys, calendar2);
            boolean zSameDate = sameDate(calendar2, this.today);
            boolean zSameDate2 = sameDate(calendar2, calendar);
            float fCenterX = next.bounds.centerX();
            float fM0dp = next.bounds.top + dp(19.0f);
            float fM0dp2 = dp((zSameDate2 ? ((float) Math.sin(((double) this.selectionPulse) * 3.141592653589793d)) * 2.2f : 0.0f) + 18.0f);
            if (zSameDate) {
                it = it2;
                this.paint.setStyle(Paint.Style.FILL);
                this.paint.setColor(this.accent);
                canvas.drawCircle(fCenterX, fM0dp, fM0dp2, this.paint);
            } else {
                it = it2;
                if (zSameDate2) {
                    this.paint.setStyle(Paint.Style.FILL);
                    this.paint.setColor(this.accentSoft);
                    canvas.drawCircle(fCenterX, fM0dp, fM0dp2, this.paint);
                    this.paint.setStyle(Paint.Style.STROKE);
                    this.paint.setStrokeWidth(dp(1.2f));
                    this.paint.setColor(this.accent);
                    canvas.drawCircle(fCenterX, fM0dp, fM0dp2, this.paint);
                }
            }
            if (!next.inDisplayedMonth) {
                i = this.textMuted;
            } else if (zSameDate) {
                i = -1;
            } else {
                i = calendar2.get(7) == 1 ? this.sunday : this.textPrimary;
            }
            setText(dp(18.0f), i, 1, Paint.Align.CENTER);
            canvas.drawText(Integer.toString(i3), fCenterX,
                    baselineCenter(fM0dp - dp(18.0f), dp(36.0f)), this.paint);
            if (!next.inDisplayedMonth) {
                i2 = this.textMuted;
            } else if (!holiday.isEmpty()) {
                i2 = this.sunday;
            } else {
                i2 = zSameDate ? this.accent : this.textSecondary;
            }
            setText(dp(9.8f), i2, !holiday.isEmpty() ? 1 : 0, Paint.Align.CENTER);
            canvas.drawText(
                    fitText(HolidayUtil.getShortLabel(i3, i4, lunarDateFromSolar),
                            this.cellWidth - dp(5.0f)),
                    fCenterX, next.bounds.top + dp(45.0f), this.paint);
            if (!holiday.isEmpty() && next.inDisplayedMonth) {
                this.paint.setColor(zSameDate ? -1 : this.sunday);
                canvas.drawCircle(dp(15.0f) + fCenterX, fM0dp - dp(13.0f), dp(2.0f), this.paint);
            }
            if (zHas && next.inDisplayedMonth) {
                this.paint.setColor(zSameDate ? -1 : this.accent);
                canvas.drawCircle(fCenterX - dp(15.0f), fM0dp - dp(13.0f), dp(2.0f), this.paint);
            }
        }
    }

    private void drawDetailPanel(Canvas canvas, Calendar calendar) {
        this.noteHitArea.setEmpty();
        if (this.detailPanel.height() < dp(86.0f)) {
            return;
        }
        drawSurface(canvas, this.detailPanel, dp(22.0f), this.surface);
        int i = calendar.get(5);
        int i2 = calendar.get(2) + 1;
        int i3 = calendar.get(1);
        LunarCalendar.LunarDate lunarDateFromSolar = LunarCalendar.fromSolar(i, i2, i3);
        String holiday = HolidayUtil.getHoliday(i, i2, lunarDateFromSolar);
        String str = NoteRepository.get(getContext(), calendar);
        float fM0dp = this.detailPanel.left + dp(17.0f);
        float fM0dp2 = this.detailPanel.right - dp(17.0f);
        setText(dp(17.0f), this.controlPrimary, 1, Paint.Align.LEFT);
        canvas.drawText(capitalize(new SimpleDateFormat("EEEE", VIETNAMESE_LOCALE).format(calendar.getTime())), fM0dp, this.detailPanel.top + dp(30.0f), this.paint);
        setText(dp(12.5f), this.controlSecondary, 1, Paint.Align.RIGHT);
        canvas.drawText(i + " tháng " + i2 + ", " + i3, fM0dp2, this.detailPanel.top + dp(29.0f), this.paint);
        this.paint.setColor(this.controlDivider);
        this.paint.setStrokeWidth(dp(0.8f));
        canvas.drawLine(fM0dp, dp(47.0f) + this.detailPanel.top, fM0dp2, this.detailPanel.top + dp(47.0f), this.paint);
        String str2 = lunarDateFromSolar.day + " tháng " + lunarDateFromSolar.month + (lunarDateFromSolar.leap ? " nhuận" : "") + "  ·  " + LunarCalendar.yearCanChi(lunarDateFromSolar.year);
        setText(dp(17.0f), this.controlPrimary, 1, Paint.Align.LEFT);
        float f = fM0dp2 - fM0dp;
        canvas.drawText(fitText(str2, f), fM0dp, this.detailPanel.top + dp(76.0f), this.paint);
        String str3 = "Ngày " + LunarCalendar.dayCanChi(lunarDateFromSolar.julianDay) + "  ·  Tháng " + LunarCalendar.monthCanChi(lunarDateFromSolar.month, lunarDateFromSolar.year);
        setText(dp(11.5f), this.controlSecondary, 0, Paint.Align.LEFT);
        canvas.drawText(fitText(str3, f), fM0dp, this.detailPanel.top + dp(101.0f), this.paint);
        if (!holiday.isEmpty() && this.detailPanel.height() >= dp(148.0f)) {
            this.paint.setColor(this.accentSoft);
            RectF rectF = new RectF(fM0dp, this.detailPanel.top + dp(111.0f), fM0dp2, Math.min(this.detailPanel.bottom - dp(8.0f), this.detailPanel.top + dp(141.0f)));
            canvas.drawRoundRect(rectF, dp(13.0f), dp(13.0f), this.paint);
            setText(dp(11.2f), this.sunday, 1, Paint.Align.CENTER);
            canvas.drawText(fitText(holiday, rectF.width() - dp(20.0f)), rectF.centerX(), baselineCenter(rectF.top, rectF.height()), this.paint);
        }
        if (this.detailPanel.height() >= dp(holiday.isEmpty() ? 158.0f : 202.0f)) {
            this.noteHitArea.set(fM0dp, (this.detailPanel.bottom - dp(12.0f)) - dp(holiday.isEmpty() ? 68.0f : 52.0f), fM0dp2, this.detailPanel.bottom - dp(12.0f));
            drawSurface(canvas, this.noteHitArea, dp(14.0f), this.surfaceSoft);
            setText(dp(12.0f), str.isEmpty() ? this.controlSecondary : this.controlPrimary,
                    str.isEmpty() ? 0 : 1, Paint.Align.LEFT);
            if (str.isEmpty()) {
                str = "Chạm để thêm ghi chú";
            }
            canvas.drawText(fitText(str, this.noteHitArea.width() - dp(26.0f)), this.noteHitArea.left + dp(13.0f), baselineCenter(this.noteHitArea.top, this.noteHitArea.height()), this.paint);
        }
    }

    private void drawBottomControls(Canvas canvas) {
        RectF rectF = new RectF(this.previousDayButton.left, this.previousDayButton.top, this.nextDayButton.right, this.nextDayButton.bottom);
        drawSurface(canvas, rectF, dp(19.0f), this.surface);
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(dp(0.8f));
        this.paint.setColor(this.controlDivider);
        canvas.drawRoundRect(rectF, dp(19.0f), dp(19.0f), this.paint);
        setText(dp(22.0f), this.controlSecondary, 0, Paint.Align.CENTER);
        canvas.drawText("‹", this.previousDayButton.centerX(), baselineCenter(this.previousDayButton.top, this.previousDayButton.height()), this.paint);
        canvas.drawText("›", this.nextDayButton.centerX(), baselineCenter(this.nextDayButton.top, this.nextDayButton.height()), this.paint);
        setText(dp(11.5f), this.accent, 1, Paint.Align.CENTER);
        canvas.drawText("HÔM NAY", this.todayButton.centerX(), baselineCenter(this.todayButton.top, this.todayButton.height()), this.paint);
        drawSurface(canvas, this.pickerButton, dp(18.0f), this.accent);
        drawDatePickerIcon(canvas, this.pickerButton.centerX(), this.pickerButton.centerY());
        drawSurface(canvas, this.settingsButton, dp(18.0f), this.accent);
        drawSettingsIcon(canvas, this.settingsButton.centerX(), this.settingsButton.centerY());
    }

    private void drawSettingsIcon(Canvas canvas, float f, float f2) {
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(dp(2.0f));
        this.paint.setStrokeCap(Paint.Cap.ROUND);
        this.paint.setColor(-1);
        canvas.drawLine(f - dp(8.0f), f2 - dp(6.0f), f + dp(8.0f), f2 - dp(6.0f), this.paint);
        canvas.drawLine(f - dp(8.0f), f2, f + dp(8.0f), f2, this.paint);
        canvas.drawLine(f - dp(8.0f), f2 + dp(6.0f), f + dp(8.0f), f2 + dp(6.0f), this.paint);
        this.paint.setStyle(Paint.Style.FILL);
    }

    private void drawDatePickerIcon(Canvas canvas, float f, float f2) {
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(dp(1.8f));
        this.paint.setStrokeCap(Paint.Cap.ROUND);
        this.paint.setStrokeJoin(Paint.Join.ROUND);
        this.paint.setColor(-1);
        RectF rectF = new RectF(f - dp(9.0f), f2 - dp(9.0f), dp(9.0f) + f, dp(9.0f) + f2);
        canvas.drawRoundRect(rectF, dp(3.0f), dp(3.0f), this.paint);
        canvas.drawLine(rectF.left, f2 - dp(3.5f), rectF.right, f2 - dp(3.5f), this.paint);
        this.paint.setStyle(Paint.Style.FILL);
        setText(dp(8.5f), -1, 1, Paint.Align.CENTER);
        canvas.drawText("31", f, f2 + dp(5.0f), this.paint);
    }

    private void drawSurface(Canvas canvas, RectF bounds, float radius, int color) {
        this.paint.setShader(null);
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(color);
        canvas.drawRoundRect(bounds, radius, radius, this.paint);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (this.selectionAnimator != null) this.selectionAnimator.cancel();
        if (this.monthAnimator != null) {
            ValueAnimator animator = this.monthAnimator;
            this.monthAnimator = null;
            animator.cancel();
        }
        this.previousDisplayed = null;
        this.previousSelected = null;
        this.monthProgress = 1.0f;
        if (this.backgroundImage != null && !this.backgroundImage.isRecycled()) {
            this.backgroundImage.recycle();
            this.backgroundImage = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case 0:
                this.downX = motionEvent.getX();
                this.downY = motionEvent.getY();
                this.moved = false;
                return true;
            case 1:
                float x = motionEvent.getX() - this.downX;
                float y = motionEvent.getY();
                float f = this.downY;
                float f2 = y - f;
                if (f >= this.weekTop && f <= this.gridBottom && Math.abs(x) > dp(62.0f) && Math.abs(x) > Math.abs(f2) * 1.35f) {
                    changeMonth(x < 0.0f ? 1 : -1);
                    return true;
                }
                if (!this.moved) {
                    handleTap(motionEvent.getX(), motionEvent.getY());
                }
                return true;
            case 2:
                if (Math.abs(motionEvent.getX() - this.downX) > dp(12.0f) || Math.abs(motionEvent.getY() - this.downY) > dp(12.0f)) {
                    this.moved = true;
                }
                return true;
            default:
                return super.onTouchEvent(motionEvent);
        }
    }

    private void handleTap(float f, float f2) {
        if (this.previousMonthButton.contains(f, f2)) {
            changeMonth(-1);
            return;
        }
        if (this.nextMonthButton.contains(f, f2)) {
            changeMonth(1);
            return;
        }
        if (this.pickerButton.contains(f, f2)) {
            showDatePicker();
            return;
        }
        if (this.settingsButton.contains(f, f2)) {
            showSettings();
            return;
        }
        if (this.previousDayButton.contains(f, f2)) {
            changeSelectedDay(-1);
            return;
        }
        if (this.nextDayButton.contains(f, f2)) {
            changeSelectedDay(1);
            return;
        }
        if (this.todayButton.contains(f, f2)) {
            goToday();
            return;
        }
        if (this.noteHitArea.contains(f, f2)) {
            showNoteEditor();
            return;
        }
        for (DayCell dayCell : this.dayCells) {
            if (dayCell.bounds.contains(f, f2)) {
                Calendar calendar = (Calendar) this.displayed.clone();
                Calendar calendar2 = (Calendar) this.selected.clone();
                this.selected.setTimeInMillis(dayCell.date.getTimeInMillis());
                if (!dayCell.inDisplayedMonth) {
                    this.displayed.setTimeInMillis(dayCell.date.getTimeInMillis());
                    this.displayed.set(5, 1);
                    startMonthTransition(calendar, calendar2, monthIndex(this.displayed) > monthIndex(calendar) ? 1 : -1);
                }
                performClick();
                startSelectionAnimation();
                invalidate();
                return;
            }
        }
    }

    private void showDatePicker() {
        new WheelDatePickerDialog(getContext(), this.selected, new WheelDatePickerDialog.Listener() {
            @Override
            public void onDateSelected(int i, int i2, int i3) {
                Calendar calendar = (Calendar) CalendarMonthView.this.displayed.clone();
                Calendar calendar2 = (Calendar) CalendarMonthView.this.selected.clone();
                CalendarMonthView.this.selected.set(i, i2, i3);
                CalendarMonthView.normalize(CalendarMonthView.this.selected);
                CalendarMonthView.this.displayed.setTimeInMillis(CalendarMonthView.this.selected.getTimeInMillis());
                CalendarMonthView.this.displayed.set(5, 1);
                CalendarMonthView.this.startTransitionIfMonthChanged(calendar, calendar2);
                CalendarMonthView.this.startSelectionAnimation();
                CalendarMonthView.this.invalidate();
            }
        }).show();
    }

    private void showSettings() {
        Context context = getContext();
        if (context instanceof Activity) {
            context.startActivity(new android.content.Intent(context, SettingsActivity.class));
        }
    }

    private void goToday() {
        Calendar calendar = (Calendar) this.displayed.clone();
        Calendar calendar2 = (Calendar) this.selected.clone();
        Calendar calendar3 = Calendar.getInstance();
        this.today = calendar3;
        normalize(calendar3);
        this.selected.setTimeInMillis(this.today.getTimeInMillis());
        this.displayed.setTimeInMillis(this.today.getTimeInMillis());
        this.displayed.set(5, 1);
        startTransitionIfMonthChanged(calendar, calendar2);
        startSelectionAnimation();
        invalidate();
    }

    private void changeSelectedDay(int i) {
        Calendar calendar = (Calendar) this.displayed.clone();
        Calendar calendar2 = (Calendar) this.selected.clone();
        this.selected.add(5, i);
        this.displayed.setTimeInMillis(this.selected.getTimeInMillis());
        this.displayed.set(5, 1);
        startTransitionIfMonthChanged(calendar, calendar2);
        startSelectionAnimation();
        invalidate();
    }

    private void changeMonth(int i) {
        Calendar calendar = (Calendar) this.displayed.clone();
        Calendar calendar2 = (Calendar) this.selected.clone();
        this.displayed.add(2, i);
        this.displayed.set(5, 1);
        this.selected.setTimeInMillis(this.displayed.getTimeInMillis());
        startMonthTransition(calendar, calendar2, i <= 0 ? -1 : 1);
        startSelectionAnimation();
        invalidate();
    }

    private void showNoteEditor() {
        NoteEditorDialog.show(getContext(), this.selected, new NoteEditorDialog.Listener() {
            @Override
            public void onNoteChanged() {
                CalendarMonthView.this.noteKeys =
                        NoteRepository.snapshotKeys(CalendarMonthView.this.getContext());
                CalendarMonthView.this.startSelectionAnimation();
                CalendarMonthView.this.invalidate();
            }
        });
    }

    public void startSelectionAnimation() {
        ValueAnimator valueAnimator = this.selectionAnimator;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.selectionAnimator = valueAnimatorOfFloat;
        valueAnimatorOfFloat.setDuration(UiKit.MOTION_DURATION_MS);
        this.selectionAnimator.setInterpolator(UiKit.MOTION_EASING);
        this.selectionAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator2) {
                CalendarMonthView.this.selectionPulse = ((Float) valueAnimator2.getAnimatedValue()).floatValue();
                CalendarMonthView.this.postInvalidateOnAnimation();
            }
        });
        this.selectionAnimator.start();
    }


    public void startTransitionIfMonthChanged(Calendar calendar, Calendar calendar2) {
        int iMonthIndex = monthIndex(calendar);
        int iMonthIndex2 = monthIndex(this.displayed);
        if (iMonthIndex != iMonthIndex2) {
            startMonthTransition(calendar, calendar2, iMonthIndex2 > iMonthIndex ? 1 : -1);
        }
    }

    private void startMonthTransition(Calendar calendar, Calendar calendar2, int i) {
        if (this.monthAnimator != null) this.monthAnimator.end();
        this.previousDisplayed = (Calendar) calendar.clone();
        this.previousSelected = (Calendar) calendar2.clone();
        this.monthDirection = i < 0 ? -1 : 1;
        this.monthProgress = 0.0f;
        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.monthAnimator = animator;
        animator.setDuration(UiKit.MOTION_DURATION_MS);
        animator.setInterpolator(UiKit.MOTION_EASING);
        animator.addUpdateListener(valueAnimator -> {
            CalendarMonthView.this.monthProgress =
                    ((Float) valueAnimator.getAnimatedValue()).floatValue();
            CalendarMonthView.this.postInvalidateOnAnimation();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (CalendarMonthView.this.monthAnimator != animation) return;
                CalendarMonthView.this.monthProgress = 1.0f;
                CalendarMonthView.this.previousDisplayed = null;
                CalendarMonthView.this.previousSelected = null;
                CalendarMonthView.this.monthAnimator = null;
                CalendarMonthView.this.postInvalidateOnAnimation();
            }
        });
        animator.start();
    }

    private static int monthIndex(Calendar calendar) {
        return (calendar.get(1) * 12) + calendar.get(2);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void setText(float f, int i, int i2, Paint.Align align) {
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setTextSize(f);
        this.paint.setColor(i);
        this.paint.setTypeface(Typeface.create(Typeface.DEFAULT, i2));
        this.paint.setTextAlign(align);
    }

    private float baselineCenter(float f, float f2) {
        this.paint.getFontMetrics(this.metrics);
        return (f + (((f2 - this.metrics.bottom) + this.metrics.top) / 2.0f)) - this.metrics.top;
    }

    private String fitText(String str, float f) {
        if (this.paint.measureText(str) <= f) {
            return str;
        }
        int length = str.length();
        while (length > 1 && this.paint.measureText(str.substring(0, length) + "…") > f) {
            length--;
        }
        return str.substring(0, length) + "…";
    }

    private String capitalize(String str) {
        if (str == null || str.length() == 0) {
            return "";
        }
        return str.substring(0, 1).toUpperCase(VIETNAMESE_LOCALE) + str.substring(1);
    }


    private float dp(float f) {
        return f * this.density;
    }


    public static void normalize(Calendar calendar) {
        calendar.set(11, 12);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
    }

    private static boolean sameDate(Calendar calendar, Calendar calendar2) {
        return calendar.get(1) == calendar2.get(1) && calendar.get(6) == calendar2.get(6);
    }

    private static final class DayCell {
        final RectF bounds;
        final Calendar date;
        final boolean inDisplayedMonth;

        DayCell(Calendar calendar, boolean z, RectF rectF) {
            this.date = calendar;
            this.inDisplayedMonth = z;
            this.bounds = rectF;
        }
    }
}
