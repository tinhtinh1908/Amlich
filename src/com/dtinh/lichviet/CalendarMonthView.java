package com.dtinh.lichviet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.LruCache;
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
    private static final Locale VIETNAMESE_LOCALE = new Locale("vi", "VN");
    private static final String[] WEEKDAYS = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
    private int accent;
    private int accentSoft;
    private int onAccent;
    private int controlPrimary;
    private int controlSecondary;
    private float cellWidth;
    private float gridLeft;
    private List<DayCell> dayCells;
    private final LruCache<Integer, List<DayCell>> monthCells;
    private int layoutMonth;
    private long detailDate;
    private String detailWeekday;
    private String detailLunar;
    private String detailDayCycle;
    private String detailMonthCycle;
    private String detailHoliday;
    private String detailNote;
    private String detailSolarDate;
    private boolean drawingDetailText;
    private int detailTextAlpha;
    private final Typeface normalFont;
    private final Typeface boldFont;
    private boolean dragging;
    private float dragOffset;
    private Calendar dragMonth;
    private Calendar dragSelection;
    private int dragDirection;
    private ValueAnimator dragReturnAnimator;
    private final float density;
    private final RectF detailPanel;
    private final Calendar displayed;
    private float downX;
    private float downY;
    private float gridBottom;
    private float gridTop;
    private int insetBottom;
    private int insetTop;
    private final Paint.FontMetrics metrics;
    private boolean moved;
    private final RectF noteHitArea;
    private Set<String> noteKeys;
    private final Paint paint;
    private boolean glassPanels;
    private final Path surfacePath = new Path();
    private boolean notePressed;
    private float rowHeight;
    private final Calendar selected;
    private ValueAnimator selectionAnimator;
    private float selectionPulse;
    private ValueAnimator monthAnimator;
    private float monthProgress;
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
    private int visibleRows;
    private float weekTop;
    private long notesRevision;

    public void openDatePicker() {
        showDatePicker();
    }

    public void selectToday() {
        goToday();
    }

    public long getSelectedDateMillis() {
        return this.selected.getTimeInMillis();
    }

    public int getSelectedYear() {
        return this.selected.get(1);
    }

    public CalendarMonthView(Context context) {
        super(context);
        this.monthCells = new LruCache<>(6);
        this.layoutMonth = Integer.MIN_VALUE;
        this.detailDate = Long.MIN_VALUE;
        this.detailTextAlpha = 255;
        int font = ThemeManager.getFont(context);
        this.normalFont = ThemeManager.font(context, font, Typeface.DEFAULT, Typeface.NORMAL);
        this.boldFont = ThemeManager.font(context, font, Typeface.DEFAULT_BOLD, Typeface.BOLD);
        this.monthProgress = 1.0f;
        this.paint = new Paint(1);
        this.metrics = new Paint.FontMetrics();
        Calendar calendar = Calendar.getInstance();
        this.displayed = calendar;
        Calendar calendar2 = Calendar.getInstance();
        this.selected = calendar2;
        this.today = Calendar.getInstance();
        this.dayCells = new ArrayList(42);
        this.noteKeys = NoteRepository.snapshotKeys(context);
        this.notesRevision = NoteRepository.revision(context);
        this.detailPanel = new RectF();
        this.noteHitArea = new RectF();
        this.density = getResources().getDisplayMetrics().density;
        loadPalette();
        calendar.set(5, 1);
        normalize(calendar);
        normalize(calendar2);
        normalize(this.today);
        setFocusable(true);
        setContentDescription("Lịch tháng Việt Nam");
    }

    private void loadPalette() {
        boolean photo = BackgroundImageManager.hasBackground(getContext());
        UiKit.Palette palette = glassPanels
                ? UiKit.glassCalendarPalette(getContext(), photo) : new UiKit.Palette(getContext());
        this.surface = palette.surface;
        this.surfaceSoft = palette.surfaceSoft;
        this.textPrimary = palette.primary;
        this.textSecondary = palette.secondary;
        this.textMuted = palette.muted;
        this.controlPrimary = palette.primary;
        this.controlSecondary = palette.secondary;
        this.accent = palette.accent;
        this.accentSoft = palette.accentSoft;
        this.onAccent = palette.onAccent;
        this.sunday = palette.danger;
        if (photo && !glassPanels) {
            UiKit.Palette palettePhotoPalette = UiKit.photoPalette(getContext());
            this.surface = palettePhotoPalette.surface;
            this.surfaceSoft = palettePhotoPalette.surfaceSoft;
            this.textPrimary = palettePhotoPalette.primary;
            this.textSecondary = palettePhotoPalette.secondary;
            this.textMuted = palettePhotoPalette.muted;
            this.accent = palettePhotoPalette.accent;
            this.accentSoft = palettePhotoPalette.accentSoft;
            this.sunday = palettePhotoPalette.danger;
            if (!palettePhotoPalette.night) {
                this.controlPrimary = palette.primary;
                this.controlSecondary = Color.rgb(82, 88, 101);
            } else {
                this.controlPrimary = palettePhotoPalette.primary;
                this.controlSecondary = palettePhotoPalette.secondary;
            }
        }
    }

    public void refreshTheme() {
        this.detailDate = Long.MIN_VALUE;
        loadPalette();
        invalidate();
    }

    public void setSystemInsets(int i, int i2) {
        int iMax = Math.max(0, i);
        int iMax2 = Math.max(0, i2);
        if (iMax == this.insetTop && iMax2 == this.insetBottom) {
            return;
        }
        this.insetTop = iMax;
        this.insetBottom = iMax2;
        invalidate();
    }

    public void refreshToday() {
        Calendar calendar = Calendar.getInstance();
        normalize(calendar);
        long jRevision = NoteRepository.revision(getContext());
        boolean z = jRevision != this.notesRevision;
        if (z) {
            this.notesRevision = jRevision;
            this.noteKeys = NoteRepository.snapshotKeys(getContext());
            this.detailDate = Long.MIN_VALUE;
            this.monthCells.evictAll();
        }
        if (!sameDate(calendar, this.today)) {
            this.today = calendar;
            z = true;
        }
        if (z) {
            invalidate();
        }
    }

    public void showDateImmediately(long j) {
        if (this.selectionAnimator != null) {
            this.selectionAnimator.cancel();
        }
        if (this.monthAnimator != null) {
            this.monthAnimator.end();
        }
        this.selected.setTimeInMillis(j);
        normalize(this.selected);
        this.displayed.setTimeInMillis(this.selected.getTimeInMillis());
        this.displayed.set(5, 1);
        normalize(this.displayed);
        this.selectionPulse = 1.0f;
        invalidate();
    }

    public void showDate(long j) {
        Calendar calendar = (Calendar) this.displayed.clone();
        Calendar calendar2 = (Calendar) this.selected.clone();
        this.selected.setTimeInMillis(j);
        normalize(this.selected);
        this.displayed.setTimeInMillis(this.selected.getTimeInMillis());
        this.displayed.set(5, 1);
        normalize(this.displayed);
        startTransitionIfMonthChanged(calendar, calendar2);
        startSelectionAnimation();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        calculateLayout();
        boolean useGlass = glassPanels && canvas.isHardwareAccelerated();
        if (!useGlass) {
            drawSurface(canvas, new RectF(dp(12f), insetTop, getWidth() - dp(12f), gridBottom + dp(4f)),
                    dp(MiuixMetrics.CARD_RADIUS), surface);
        }
        drawWeekdays(canvas);
        drawMonthGridTransition(canvas);
        drawDetailPanel(canvas, selected, useGlass);
    }

    void setGlassPanels(boolean enabled) {
        if (glassPanels == enabled) return;
        glassPanels = enabled;
        loadPalette();
        invalidate();
    }

    void copyGlassPanelBounds(Rect grid, Rect detail) {
        calculateLayout();
        grid.set(Math.round(dp(12f)), insetTop, Math.round(getWidth() - dp(12f)), Math.round(gridBottom + dp(4f)));
        if (detailPanel.height() < dp(100f)) detail.setEmpty();
        else detailPanel.round(detail);
    }

    void copyNoteBounds(Rect bounds) {
        noteHitArea.round(bounds);
    }

    boolean isNotePressed() {
        return notePressed;
    }

    private void setNotePressed(boolean pressed) {
        if (notePressed == pressed) return;
        notePressed = pressed;
        postInvalidateOnAnimation();
    }

    private void drawMonthGridTransition(Canvas canvas) {
        if (this.dragging && this.dragMonth != null) {
            drawMonthGridLayer(canvas, this.displayed, this.selected, this.dragOffset);
            drawMonthGridLayer(canvas, this.dragMonth, this.dragSelection, this.dragOffset + (this.dragDirection * getWidth()));
        } else {
            if (!isMonthTransitionRunning()) {
                drawMonthGrid(canvas, this.displayed, this.selected);
                return;
            }
            float width = getWidth();
            float f = this.monthProgress;
            drawMonthGridLayer(canvas, this.previousDisplayed, this.previousSelected, (-this.monthDirection) * width * f);
            drawMonthGridLayer(canvas, this.displayed, this.selected, this.monthDirection * width * (1.0f - f));
        }
    }

    private void drawMonthGridLayer(Canvas canvas, Calendar calendar, Calendar calendar2, float f) {
        int iSave = canvas.save();
        canvas.clipRect(this.gridLeft, this.gridTop, getWidth() - this.gridLeft, this.gridBottom);
        canvas.translate(f, 0.0f);
        int i = this.visibleRows;
        float f2 = this.rowHeight;
        this.visibleRows = cachedCells(calendar).size() / 7;
        this.rowHeight = (this.gridBottom - this.gridTop) / this.visibleRows;
        drawMonthGrid(canvas, calendar, calendar2);
        this.visibleRows = i;
        this.rowHeight = f2;
        canvas.restoreToCount(iSave);
    }

    private boolean isMonthTransitionRunning() {
        return (this.previousDisplayed == null || this.previousSelected == null || this.monthProgress >= 1.0f) ? false : true;
    }

    private void drawMonthGrid(Canvas canvas, Calendar calendar, Calendar calendar2) {
        buildCells(calendar);
        drawDays(canvas, calendar2);
    }

    private void calculateLayout() {
        float width = getWidth();
        this.gridLeft = dp(20.0f);
        this.cellWidth = Math.max(1.0f, (width - (2.0f * this.gridLeft)) / 7.0f);
        float f = this.insetTop;
        float height = getHeight() - this.insetBottom;
        this.weekTop = f;
        this.gridTop = f + Math.min(dp(40.0f), (height - f) * 0.09f);
        int iMonthIndex = monthIndex(this.displayed);
        if (this.layoutMonth != iMonthIndex) {
            this.layoutMonth = iMonthIndex;
            this.visibleRows = Math.max(5, Math.min(6, ((((this.displayed.get(7) + 5) % 7) + this.displayed.getActualMaximum(5)) + 6) / 7));
        }
        float detailHeight = dp(216f);
        float detailBottom = height - dp(8f);
        float gridSpace = detailBottom - detailHeight - dp(12f) - gridTop;
        // Keep the compact five-row baseline for every month. Only cell height changes.
        float extraGridSpace = Math.max(0f, gridSpace - dp(300f));
        detailBottom -= Math.min(dp(56f), extraGridSpace * 0.34f);
        float detailTop = detailBottom - detailHeight;
        this.gridBottom = Math.max(gridTop + 6f, detailTop - dp(12f));
        this.rowHeight = (gridBottom - gridTop) / visibleRows;
        this.detailPanel.set(dp(12f), detailTop, width - dp(12f), detailBottom);
        if (detailPanel.height() < dp(100f)) {
            noteHitArea.setEmpty();
        } else {
            noteHitArea.set(detailPanel.left + dp(16f), detailPanel.top + dp(154f),
                    detailPanel.right - dp(16f), detailPanel.bottom - dp(14f));
        }
    }

    private void drawWeekdays(Canvas canvas) {
        int i = 0;
        while (i < 7) {
            setText(dp(14.0f), i == 6 ? this.sunday : this.textSecondary, 1, Paint.Align.CENTER);
            String str = WEEKDAYS[i];
            float f = this.cellWidth;
            canvas.drawText(str, this.gridLeft + (i * f) + (f / 2.0f), baselineCenter(this.weekTop, this.gridTop - this.weekTop), this.paint);
            i++;
        }
    }

    private List<DayCell> cachedCells(Calendar calendar) {
        int iMonthIndex = monthIndex(calendar);
        List<DayCell> arrayList = this.monthCells.get(Integer.valueOf(iMonthIndex));
        if (arrayList == null) {
            int rows = Math.max(5, ((((calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7)
                    + calendar.getActualMaximum(Calendar.DAY_OF_MONTH)) + 6) / 7);
            arrayList = new ArrayList<>(rows * 7);
            Calendar calendar2 = (Calendar) calendar.clone();
            calendar2.add(5, -((calendar2.get(7) + 5) % 7));
            for (int i = 0; i < rows * 7; i++) {
                Calendar calendar3 = (Calendar) calendar2.clone();
                arrayList.add(new DayCell(calendar3, calendar3.get(2) == calendar.get(2) && calendar3.get(1) == calendar.get(1), new RectF(), NoteRepository.has(this.noteKeys, calendar3)));
                calendar2.add(5, 1);
            }
            this.monthCells.put(Integer.valueOf(iMonthIndex), arrayList);
        }
        return arrayList;
    }

    private void buildCells(Calendar calendar) {
        this.dayCells = cachedCells(calendar);
        for (int i = 0; i < this.dayCells.size(); i++) {
            int i2 = i / 7;
            int i3 = i % 7;
            this.dayCells.get(i).bounds.set(this.gridLeft + (i3 * this.cellWidth), this.gridTop + (i2 * this.rowHeight), this.gridLeft + ((i3 + 1) * this.cellWidth), this.gridTop + ((i2 + 1) * this.rowHeight));
        }
    }

    private float dayCellScale() {
        return Math.min(1f, Math.min(rowHeight / dp(54f), (cellWidth - dp(6f)) / dp(42f)));
    }

    private float dayMarkerCenterY(float cellTop, float scale) {
        return cellTop + Math.max(0f, (rowHeight - dp(54f)) / 2f) + dp(20f) * scale;
    }

    private float dayMarkerRadius(boolean selectedDay, float scale) {
        float pulse = selectedDay ? (float) Math.sin(selectionPulse * Math.PI) * 2.2f : 0f;
        return dp(18f + pulse) * scale;
    }

    private void drawDays(Canvas canvas, Calendar calendar) {
        Iterator<DayCell> it;
        int i;
        int i2;
        Iterator<DayCell> it2 = this.dayCells.iterator();
        while (true) {
            Iterator<DayCell> it3 = it2;
            if (it3.hasNext()) {
                DayCell next = it3.next();
                Calendar calendar2 = next.date;
                calendar2.get(5);
                int i3 = calendar2.get(2) + 1;
                String str = next.holiday;
                boolean z = next.hasNote;
                boolean zSameDate = sameDate(calendar2, this.today);
                boolean zSameDate2 = sameDate(calendar2, calendar);
                float fCenterX = next.bounds.centerX();
                float fMin = dayCellScale();
                int iSave = canvas.save();
                canvas.clipRect(next.bounds.left + dp(2.0f), next.bounds.top, next.bounds.right - dp(2.0f), next.bounds.bottom);
                float fMax = Math.max(0f, (rowHeight - dp(54f)) / 2f);
                float fDp = dayMarkerCenterY(next.bounds.top, fMin);
                float fDp2 = dayMarkerRadius(zSameDate2, fMin);
                it = it3;
                if (zSameDate || zSameDate2) {
                    this.paint.setStyle(Paint.Style.FILL);
                    this.paint.setColor(zSameDate ? this.accent : this.accentSoft);
                    canvas.drawCircle(fCenterX, fDp, fDp2, this.paint);
                    if (!zSameDate) {
                        this.paint.setStyle(Paint.Style.STROKE);
                        this.paint.setStrokeWidth(dp(1.2f));
                        this.paint.setColor(this.accent);
                        canvas.drawCircle(fCenterX, fDp, fDp2, this.paint);
                    }
                }
                if (!next.inDisplayedMonth) {
                    i = this.textMuted;
                } else if (zSameDate) {
                    i = this.onAccent;
                } else {
                    i = calendar2.get(7) == 1 ? this.sunday : this.textPrimary;
                }
                setText(dp(19.5f) * fMin, i, 1, Paint.Align.CENTER);
                canvas.drawText(next.solarLabel, fCenterX, baselineCenter(fDp - (dp(18.0f) * fMin), dp(36.0f) * fMin), this.paint);
                if (!next.inDisplayedMonth) {
                    i2 = this.textMuted;
                } else if (!str.isEmpty()) {
                    i2 = this.sunday;
                } else {
                    i2 = zSameDate ? this.accent : this.textSecondary;
                }
                setText(dp(9.8f) * Math.max(0.75f, fMin), i2, !str.isEmpty() ? 1 : 0, Paint.Align.CENTER);
                float lunarBaseline = Math.min(next.bounds.top + fMax + dp(51f) * fMin,
                        next.bounds.bottom - dp(1f) - this.paint.descent());
                canvas.drawText(fitText(next.shortLabel, this.cellWidth - dp(8f)), fCenterX, lunarBaseline, this.paint);
                if (!str.isEmpty() && next.inDisplayedMonth) {
                    this.paint.setColor(zSameDate ? this.onAccent : this.sunday);
                    canvas.drawCircle((dp(18f) * fMin) + fCenterX, fDp - (dp(13.0f) * fMin), dp(2.0f) * fMin, this.paint);
                }
                if (z && next.inDisplayedMonth) {
                    this.paint.setColor(zSameDate ? this.onAccent : this.accent);
                    canvas.drawCircle(fCenterX - (dp(18f) * fMin), fDp - (dp(13.0f) * fMin), dp(2.0f) * fMin, this.paint);
                }
                canvas.restoreToCount(iSave);
                it2 = it;
            } else {
                return;
            }
        }
    }

    private void drawDetailPanel(Canvas canvas, Calendar calendar, boolean useGlass) {
        if (this.detailPanel.height() < dp(100.0f)) {
            return;
        }
        if (!useGlass) drawSurface(canvas, this.detailPanel, dp(MiuixMetrics.CARD_RADIUS), this.surface);
        int i = calendar.get(5);
        int i2 = calendar.get(2) + 1;
        int i3 = calendar.get(1);
        if (this.detailDate != calendar.getTimeInMillis()) {
            this.detailDate = calendar.getTimeInMillis();
            LunarCalendar.LunarDate lunarDateFromSolar = LunarCalendar.fromSolar(i, i2, i3);
            this.detailSolarDate = i + " tháng " + i2 + ", " + i3;
            this.detailHoliday = HolidayUtil.getHoliday(i, i2, lunarDateFromSolar).toUpperCase(VIETNAMESE_LOCALE);
            this.detailNote = NoteRepository.get(getContext(), calendar);
            this.detailWeekday = capitalize(new SimpleDateFormat("EEEE", VIETNAMESE_LOCALE).format(calendar.getTime()));
            this.detailLunar = lunarDateFromSolar.day + " tháng " + lunarDateFromSolar.month + (lunarDateFromSolar.leap ? " nhuận" : "") + " · " + LunarCalendar.yearCanChi(lunarDateFromSolar.year);
            this.detailDayCycle = LunarCalendar.dayCanChi(lunarDateFromSolar.julianDay);
            this.detailMonthCycle = LunarCalendar.monthCanChi(lunarDateFromSolar.month, lunarDateFromSolar.year);
        }
        String str = this.detailHoliday;
        String str2 = this.detailNote;
        float f = (this.selectionAnimator == null || !this.selectionAnimator.isRunning()) ? 1.0f : this.selectionPulse;
        this.detailTextAlpha = Math.round(255.0f * (0.35f + (0.65f * f)));
        int iSave = canvas.save();
        canvas.clipRect(this.detailPanel);
        this.drawingDetailText = true;
        float fDp = this.detailPanel.left + dp(16.0f);
        float fDp2 = this.detailPanel.right - dp(16.0f);
        float f2 = fDp2 - fDp;
        float f3 = this.detailPanel.top;
        float fCenterX = this.detailPanel.centerX();
        setText(dp(17.0f), this.controlPrimary, 1, Paint.Align.LEFT);
        canvas.drawText(this.detailWeekday, fDp, f3 + dp(28.0f), this.paint);
        setText(dp(14.0f), this.controlSecondary, 0, Paint.Align.RIGHT);
        canvas.drawText(fitText(this.detailSolarDate, f2 * 0.62f), fDp2, f3 + dp(28.0f), this.paint);
        setText(dp(16.0f), this.controlPrimary, 1, Paint.Align.CENTER);
        canvas.drawText(fitText(this.detailLunar, f2), fCenterX, f3 + dp(68.0f), this.paint);
        float f4 = fDp + (f2 * 0.25f);
        float f5 = fDp + (f2 * 0.75f);
        setText(dp(11.0f), this.controlSecondary, 0, Paint.Align.CENTER);
        canvas.drawText("Ngày", f4, f3 + dp(89.0f), this.paint);
        canvas.drawText("Tháng", f5, f3 + dp(89.0f), this.paint);
        setText(dp(15.0f), this.controlPrimary, 1, Paint.Align.CENTER);
        canvas.drawText(fitText(this.detailDayCycle, (f2 / 2.0f) - dp(8.0f)), f4, f3 + dp(110.0f), this.paint);
        canvas.drawText(fitText(this.detailMonthCycle, (f2 / 2.0f) - dp(8.0f)), f5, f3 + dp(110.0f), this.paint);
        if (!str.isEmpty()) {
            setText(dp(15.5f), this.sunday, 1, Paint.Align.CENTER);
            canvas.drawText(fitText(str, f2), fCenterX, f3 + dp(137.0f), this.paint);
        }
        this.drawingDetailText = false;
        canvas.restoreToCount(iSave);
        if (!useGlass) drawSurface(canvas, this.noteHitArea, dp(MiuixMetrics.BUTTON_RADIUS), this.surfaceSoft);
        setText(dp(14f), str2.isEmpty() && !useGlass ? this.controlSecondary : this.controlPrimary,
                str2.isEmpty() ? 0 : 1, Paint.Align.CENTER);
        String label = fitText(str2.isEmpty() ? "Thêm ghi chú" : str2, this.noteHitArea.width() - dp(32f));
        canvas.drawText(label, this.noteHitArea.centerX(),
                baselineCenter(this.noteHitArea.top, this.noteHitArea.height()), this.paint);
    }

    private void drawSurface(Canvas canvas, RectF rectF, float f, int i) {
        this.paint.setShader(null);
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(i);
        MiuixSurfaceDrawable.draw(canvas, surfacePath, rectF, f, this.paint);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (this.selectionAnimator != null) {
            this.selectionAnimator.cancel();
        }
        if (this.monthAnimator != null) {
            ValueAnimator valueAnimator = this.monthAnimator;
            this.monthAnimator = null;
            valueAnimator.cancel();
        }
        this.previousDisplayed = null;
        this.previousSelected = null;
        this.monthProgress = 1.0f;
        if (this.dragReturnAnimator != null) {
            this.dragReturnAnimator.cancel();
        }
        this.monthCells.evictAll();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case 0:
                if (this.dragReturnAnimator != null) {
                    this.dragReturnAnimator.cancel();
                }
                if (this.monthAnimator != null) {
                    this.monthAnimator.end();
                }
                this.dragging = false;
                this.dragOffset = 0.0f;
                this.dragMonth = null;
                this.downX = motionEvent.getX();
                this.downY = motionEvent.getY();
                setNotePressed(this.noteHitArea.contains(this.downX, this.downY));
                this.moved = false;
                if (this.downY >= this.weekTop && this.downY <= this.gridBottom) {
                    Calendar calendar = (Calendar) this.displayed.clone();
                    calendar.add(2, -1);
                    cachedCells(calendar);
                    calendar.add(2, 2);
                    cachedCells(calendar);
                    return true;
                }
                return true;
            case 1:
                setNotePressed(false);
                if (this.dragging) {
                    if (Math.abs(this.dragOffset) >= Math.min(dp(62.0f), getWidth() * 0.18f)) {
                        changeMonth(this.dragDirection);
                        this.dragging = false;
                        this.dragOffset = 0.0f;
                        this.dragMonth = null;
                    } else {
                        returnDrag();
                    }
                } else if (!this.moved) {
                    handleTap(motionEvent.getX(), motionEvent.getY());
                }
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
                return true;
            case 2:
                float x = motionEvent.getX() - this.downX;
                float y = motionEvent.getY() - this.downY;
                if (Math.abs(x) > dp(8.0f) || Math.abs(y) > dp(8.0f)) {
                    this.moved = true;
                    setNotePressed(false);
                }
                if (!this.dragging && this.downY >= this.weekTop && this.downY <= this.gridBottom && Math.abs(x) > dp(8.0f) && Math.abs(x) > Math.abs(y) * 1.35f) {
                    this.dragging = true;
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                if (this.dragging) {
                    int i = x < 0.0f ? 1 : -1;
                    if (this.dragMonth == null || i != this.dragDirection) {
                        this.dragDirection = i;
                        this.dragMonth = (Calendar) this.displayed.clone();
                        this.dragMonth.add(2, i);
                        this.dragSelection = (Calendar) this.dragMonth.clone();
                    }
                    this.dragOffset = Math.max(-getWidth(), Math.min(getWidth(), x));
                    postInvalidateOnAnimation();
                    return true;
                }
                return true;
            case 3:
                setNotePressed(false);
                if (this.dragging) {
                    returnDrag();
                }
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
                return true;
            default:
                return super.onTouchEvent(motionEvent);
        }
    }

    private void returnDrag() {
        this.dragReturnAnimator = ValueAnimator.ofFloat(this.dragOffset, 0.0f);
        this.dragReturnAnimator.setDuration(ValueAnimator.areAnimatorsEnabled() ? 160L : 0L);
        this.dragReturnAnimator.setInterpolator(UiKit.MOTION_EASING);
        this.dragReturnAnimator.addUpdateListener(valueAnimator -> {
            this.dragOffset = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            postInvalidateOnAnimation();
        });
        this.dragReturnAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                CalendarMonthView.this.dragging = false;
                CalendarMonthView.this.dragOffset = 0.0f;
                CalendarMonthView.this.dragMonth = null;
                CalendarMonthView.this.postInvalidateOnAnimation();
            }
        });
        this.dragReturnAnimator.start();
    }

    private void handleTap(float f, float f2) {
        buildCells(this.displayed);
        if (f >= this.detailPanel.centerX() && f < this.detailPanel.right && f2 >= this.detailPanel.top && f2 < this.detailPanel.top + dp(40.0f)) {
            openDatePicker();
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
                CalendarMonthView.this.refreshToday();
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
        valueAnimatorOfFloat.setDuration(ValueAnimator.areAnimatorsEnabled() ? 200L : 0L);
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
        if (this.monthAnimator != null) {
            this.monthAnimator.end();
        }
        this.previousDisplayed = (Calendar) calendar.clone();
        this.previousSelected = (Calendar) calendar2.clone();
        this.monthDirection = i < 0 ? -1 : 1;
        this.monthProgress = this.dragging ? Math.min(1.0f, Math.abs(this.dragOffset) / Math.max(1, getWidth())) : 0.0f;
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(this.monthProgress, 1.0f);
        this.monthAnimator = valueAnimatorOfFloat;
        valueAnimatorOfFloat.setDuration(ValueAnimator.areAnimatorsEnabled() ? Math.max(80, Math.round(220.0f * (1.0f - this.monthProgress))) : 0L);
        valueAnimatorOfFloat.setInterpolator(UiKit.MOTION_EASING);
        valueAnimatorOfFloat.addUpdateListener(valueAnimator -> {
            this.monthProgress = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            postInvalidateOnAnimation();
        });
        valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (CalendarMonthView.this.monthAnimator != animator) {
                    return;
                }
                CalendarMonthView.this.monthProgress = 1.0f;
                CalendarMonthView.this.previousDisplayed = null;
                CalendarMonthView.this.previousSelected = null;
                CalendarMonthView.this.monthAnimator = null;
                CalendarMonthView.this.postInvalidateOnAnimation();
            }
        });
        valueAnimatorOfFloat.start();
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
        if (this.drawingDetailText) {
            this.paint.setAlpha(Color.alpha(i) * this.detailTextAlpha / 255);
        }
        this.paint.setTypeface(i2 == 0 ? this.normalFont : this.boldFont);
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
        final boolean hasNote;
        final String holiday;
        final String shortLabel;
        final String solarLabel;

        DayCell(Calendar calendar, boolean z, RectF rectF, boolean z2) {
            this.date = calendar;
            this.inDisplayedMonth = z;
            this.bounds = rectF;
            this.hasNote = z2;
            int i = calendar.get(5);
            int i2 = calendar.get(2) + 1;
            LunarCalendar.LunarDate lunarDateFromSolar = LunarCalendar.fromSolar(i, i2, calendar.get(1));
            this.holiday = HolidayUtil.getHoliday(i, i2, lunarDateFromSolar);
            this.shortLabel = HolidayUtil.getShortLabel(i, i2, lunarDateFromSolar);
            this.solarLabel = Integer.toString(i);
        }
    }
}
