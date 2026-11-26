package com.dtinh.lichviet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Build;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

final class MinimalCalendarView extends ViewGroup {
    private static final int MAX_TORN_PAGE_PIXELS = 1_800_000;
    private static final Locale VI = new Locale("vi", "VN");
    private final SimpleDateFormat weekdayFormat;
    private final Activity activity;
    private final CalendarMonthView calendar;
    private final UiKit.Palette colors;
    private final UiKit.Palette glassColors;
    private final TearCalendarGlassLayer glassLayer;
    private boolean glassEnabled;
    private final View paper;
    private final View separator;
    private final TextView settings;
    private final TextView today;
    private final TextView monthTitle;
    private final TextView solarDay;
    private final TextView weekday;
    private final TextView lunarDay;
    private final TextView lunarMonth;
    private final TextView dayCycle;
    private final TextView monthCycle;
    private final TextView yearCycle;
    private final TextView term;
    private final TextView guardian;
    private final TextView opposite;
    private final TextView holiday;
    private final TextView note;
    private final TextView hourTitle;
    private final TextView dayLabel;
    private final TextView monthLabel;
    private final TextView yearLabel;
    private final TextView lunarLabel;
    private final View[] hourBackgrounds;
    private final TextView[] hourNames;
    private final TextView[] hourRanges;
    private final List<Placement> placements;
    private Bitmap tornPage;
    private final Paint tearPaint;
    private ValueAnimator tearAnimator;
    private float touchX;
    private float touchY;
    private float tearProgress;
    private float tearDrift;
    private boolean tearing;
    private boolean verticalTear;
    private boolean tearCommitted;
    private long originalDate;
    private final RectF tornBounds;
    private final float[] tearMesh;
    private final int[] tearShades;
    private final ColorFilter shadowFilter;

    private Bitmap backgroundImage;
    private boolean backgroundDetached = true;
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF backgroundBounds = new RectF();
    private long backgroundRevision = Long.MIN_VALUE;
    private int backgroundWidth, backgroundHeight, backgroundGeneration;

    private int primaryText() { return glassEnabled ? glassColors.primary : backgroundImage == null ? colors.primary : 0xffffffff; }
    private int secondaryText() { return glassEnabled ? 0xffe0e5ed : backgroundImage == null ? colors.secondary : 0xffe0e5ed; }
    private int accentText() { return glassEnabled ? glassColors.accent : backgroundImage == null ? colors.accent : 0xffafcfff; }
    private int holidayText() { return glassEnabled ? glassColors.danger : backgroundImage == null ? colors.danger : 0xffffadb4; }
    private int paperFill() { return backgroundImage == null ? colors.surface : 0x55000000; }
    private int softFill() { return backgroundImage == null ? colors.surfaceSoft : 0x66000000; }
    private int controlFill() { return backgroundImage == null ? colors.accentSoft : 0x99000000; }
    private int dividerColor() { return glassEnabled ? 0x30ffffff : backgroundImage == null ? colors.divider : 0x30ffffff; }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateBackground();
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        backgroundDetached = false;
        updateBackground();
    }
    private void updateBackground() {
        if (backgroundDetached || !isAttachedToWindow() || getWidth() <= 0 || getHeight() <= 0) return;
        long revision = BackgroundImageManager.revision(getContext());
        int width = Math.min(1080, getWidth()), height = Math.min(1920, getHeight());
        if (revision == backgroundRevision && width == backgroundWidth && height == backgroundHeight) return;
        backgroundRevision = revision; backgroundWidth = width; backgroundHeight = height;
        final int generation = ++backgroundGeneration;
        if (!BackgroundImageManager.hasBackground(getContext())) {
            replaceBackground(null);
            return;
        }
        android.content.Context context = getContext().getApplicationContext();
        new Thread(() -> {
            Bitmap decoded = BackgroundImageManager.load(context, width, height);
            boolean posted = post(() -> {
                if (generation != backgroundGeneration || backgroundDetached || !isAttachedToWindow()) {
                    if (decoded != null) decoded.recycle();
                    return;
                }
                replaceBackground(decoded);
            });
            if (!posted && decoded != null) decoded.recycle();
        }, "tear-calendar-background").start();
    }
    private void replaceBackground(Bitmap image) {
        Bitmap old = backgroundImage; backgroundImage = image;
        if (old != null && old != image) old.recycle();
        restyleSurfaces();
        refresh();
        invalidate();
    }

    void refreshGlassPreference() {
        if (Build.VERSION.SDK_INT < 33 || glassLayer == null) return;
        boolean enabled = ThemeManager.isSoftGlassEnabled(activity);
        glassLayer.updateOpacity(ThemeManager.getSoftGlassOpacity(activity));
        glassLayer.setVisibility(enabled ? VISIBLE : GONE);
        if (enabled == glassEnabled) return;
        glassEnabled = enabled;
        restyleSurfaces();
        invalidate();
    }

    private void restyleSurfaces() {
        paper.setBackground(glassEnabled ? null : UiKit.rounded(paperFill(), dp(MiuixMetrics.CARD_RADIUS)));
        today.setTextColor(glassEnabled ? primaryText() : accentText());
        today.setBackground(glassEnabled ? null : UiKit.interactiveBackground(activity, controlFill(), 16));
        settings.setBackground(glassEnabled ? null : UiKit.interactiveBackground(activity, paperFill(), 16));
        UiKit.setIcon(settings, R.drawable.ic_settings, primaryText(), 18);
        weekday.setTextColor(glassEnabled ? primaryText() : accentText());
        weekday.setBackground(glassEnabled ? null : UiKit.rounded(controlFill(), dp(10)));
        separator.setBackgroundColor(dividerColor());
        for (TextView label : new TextView[]{monthTitle,dayLabel,monthLabel,yearLabel,lunarLabel,lunarMonth,hourTitle}) label.setTextColor(secondaryText());
        for (TextView value : new TextView[]{dayCycle,monthCycle,yearCycle,lunarDay,term,opposite,note}) value.setTextColor(primaryText());
        holiday.setTextColor(holidayText());
        for (int i = 0; i < hourBackgrounds.length; i++) {
            hourBackgrounds[i].setBackground(glassEnabled ? null : UiKit.rounded(softFill(), dp(6)));
            hourNames[i].setTextColor(primaryText()); hourRanges[i].setTextColor(secondaryText());
        }
        note.setBackground(glassEnabled ? null : UiKit.interactiveBackground(activity, softFill(), 16));
        if (Build.VERSION.SDK_INT >= 33 && glassLayer != null) glassLayer.updateBackdrop(glassColors.surface);
    }

    void drawCalendarBackdrop(Canvas canvas, boolean shadePhoto) {
        if (backgroundImage == null) {
            canvas.drawColor(glassEnabled ? glassColors.background : colors.background);
            return;
        }
        backgroundBounds.set(0, 0, getWidth(), getHeight());
        backgroundPaint.setAlpha(255);
        BackgroundImageManager.drawCenterCrop(canvas, backgroundImage, backgroundBounds, backgroundPaint);
        if (shadePhoto) {
            backgroundPaint.setColor(0x70000000);
            canvas.drawRect(backgroundBounds, backgroundPaint);
            backgroundPaint.setAlpha(255);
        }
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawCalendarBackdrop(canvas, !glassEnabled);
    }

    MinimalCalendarView(Activity activity, CalendarMonthView calendarMonthView) {
        super(activity);
        this.weekdayFormat = new SimpleDateFormat("EEEE", VI);
        this.hourBackgrounds = new View[6];
        this.hourNames = new TextView[6];
        this.hourRanges = new TextView[6];
        this.placements = new ArrayList();
        this.tearPaint = new Paint(3);
        this.tornBounds = new RectF();
        this.tearMesh = new float[PaperTearGeometry.VERTICES * 2];
        this.tearShades = new int[PaperTearGeometry.VERTICES];
        this.shadowFilter = new PorterDuffColorFilter(-16777216, PorterDuff.Mode.SRC_IN);
        this.activity = activity;
        this.calendar = calendarMonthView;
        this.colors = new UiKit.Palette(activity);
        this.glassColors = UiKit.glassCalendarPalette(activity, false);
        setWillNotDraw(false);
        this.paper = new View(activity);
        this.paper.setBackground(UiKit.rounded(paperFill(), dp(MiuixMetrics.CARD_RADIUS)));
        addView(this.paper);
        this.today = button("Hôm nay", accentText(), controlFill());
        this.today.setAutoSizeTextTypeUniformWithConfiguration(11, 16, 1, TypedValue.COMPLEX_UNIT_SP);
        this.today.setPadding(dp(8f), 0, dp(8f), 0);
        this.today.setOnClickListener(view -> {
            showDate(Calendar.getInstance());
        });
        this.settings = button("", primaryText(), paperFill());
        this.settings.setPadding(0, 0, 0, 0);
        UiKit.setIcon(this.settings, R.drawable.ic_settings, primaryText(), 18);
        this.settings.setContentDescription("Cài đặt");
        this.settings.setOnClickListener(view2 -> {
            activity.startActivity(new Intent(activity, (Class<?>) SettingsActivity.class));
        });
        setClickable(true);
        setContentDescription("Lịch xé. Kéo tờ lịch để xé sang ngày tiếp theo.");
        this.monthTitle = text("", 20, secondaryText(), true, 17, 1);
        this.monthTitle.setContentDescription("Chọn ngày");
        this.monthTitle.setClickable(true);
        this.monthTitle.setFocusable(true);
        this.monthTitle.setOnClickListener(view3 -> {
            chooseDate();
        });
        this.solarDay = text("", 150, primaryText(), true, 17, 1);
        this.solarDay.setTypeface(ThemeManager.font(activity, ThemeManager.getFont(activity),
                Typeface.create("sans-serif-condensed", Typeface.BOLD), Typeface.BOLD));
        this.weekday = text("", 22, accentText(), true, 17, 1);
        this.weekday.setBackground(UiKit.rounded(controlFill(), dp(10.0f)));
        this.holiday = text("", 16, holidayText(), true, 17, 2);
        this.dayLabel = text("Ngày", 14, secondaryText(), false, 8388627, 1);
        this.monthLabel = text("Tháng", 14, secondaryText(), false, 8388627, 1);
        this.yearLabel = text("Năm", 14, secondaryText(), false, 8388627, 1);
        this.dayCycle = text("", 18, primaryText(), true, 8388627, 1);
        this.monthCycle = text("", 18, primaryText(), true, 8388627, 1);
        this.yearCycle = text("", 18, primaryText(), true, 8388627, 1);
        this.lunarLabel = text("ÂM LỊCH", 13, secondaryText(), true, 17, 1);
        this.lunarDay = text("", 60, primaryText(), true, 17, 1);
        this.lunarMonth = text("", 15, secondaryText(), false, 17, 1);
        this.separator = new View(activity);
        this.separator.setBackgroundColor(dividerColor());
        addView(this.separator);
        this.guardian = text("", 17, accentText(), false, 17, 2);
        this.term = text("", 17, primaryText(), false, 17, 2);
        this.opposite = text("", 17, primaryText(), false, 17, 2);
        this.hourTitle = text("GIỜ HOÀNG ĐẠO", 14, secondaryText(), true, 17, 1);
        for (int i = 0; i < this.hourNames.length; i++) {
            this.hourBackgrounds[i] = new View(activity);
            this.hourBackgrounds[i].setBackground(UiKit.rounded(softFill(), dp(6.0f)));
            addView(this.hourBackgrounds[i]);
            this.hourNames[i] = text("", 16, primaryText(), true, 17, 1);
            this.hourRanges[i] = text("", 14, secondaryText(), false, 17, 1);
            this.hourRanges[i].setTypeface(ThemeManager.font(activity, ThemeManager.getFont(activity),
                    Typeface.DEFAULT, Typeface.NORMAL));
        }
        this.note = text("", 16, primaryText(), false, 17, 2);
        this.note.setBackground(UiKit.interactiveBackground(activity, softFill(), 16.0f));
        this.note.setClickable(true);
        this.note.setFocusable(true);
        UiKit.bindPressMotion(this.note);
        this.note.setOnClickListener(view4 -> {
            NoteEditorDialog.show(activity, selected(), this::refresh);
        });
        glassLayer = Build.VERSION.SDK_INT >= 33
                ? new TearCalendarGlassLayer(this, paper, today, settings, weekday, note, hourBackgrounds)
                : null;
        if (glassLayer != null) addView(glassLayer, 0);
        refreshGlassPreference();
        refresh();
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        setPadding(insets.left, insets.top, insets.right, insets.bottom);
        return WindowInsets.CONSUMED;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        setMeasuredDimension(size, size2);
        if (glassLayer != null) {
            glassLayer.measure(MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(size2, MeasureSpec.EXACTLY));
        }
        MinimalLayout minimalLayout = new MinimalLayout(Math.max(1, (size - getPaddingLeft()) - getPaddingRight()), Math.max(1, (size2 - getPaddingTop()) - getPaddingBottom()), getResources().getDisplayMetrics().density, this.holiday.getVisibility() != View.GONE);
        this.placements.clear();
        place(this.paper, minimalLayout.paper, 0.0f, 0.0f, 1.0f, 1.0f);
        int paddingLeft = getPaddingLeft() + minimalLayout.header.right;
        int paddingTop = getPaddingTop() + ((minimalLayout.header.top + minimalLayout.header.bottom) / 2);
        int iMin = Math.min(dp(36.0f), minimalLayout.header.height());
        this.placements.add(new Placement(this.settings, paddingLeft - iMin, paddingTop - iMin / 2, paddingLeft, paddingTop + iMin / 2));
        this.placements.add(new Placement(this.today, paddingLeft - dp(124.0f), paddingTop - (iMin / 2), paddingLeft - dp(44.0f), paddingTop + (iMin / 2)));
        place(this.monthTitle, minimalLayout.rows[0], 0.0f, 0.0f, 1.0f, 1.0f);
        place(this.solarDay, minimalLayout.rows[1], 0.0f, 0.0f, 1.0f, 1.0f);
        place(this.weekday, minimalLayout.rows[2], 0.0f, 0.0f, 1.0f, 1.0f);
        place(this.holiday, minimalLayout.rows[3], 0.0f, 0.0f, 1.0f, 1.0f);
        MinimalLayout.Box box = minimalLayout.rows[4];
        place(this.dayLabel, box, 0.02f, 0.08f, 0.2f, 0.36f);
        place(this.monthLabel, box, 0.02f, 0.36f, 0.2f, 0.64f);
        place(this.yearLabel, box, 0.02f, 0.64f, 0.2f, 0.92f);
        place(this.dayCycle, box, 0.22f, 0.08f, 0.61f, 0.36f);
        place(this.monthCycle, box, 0.22f, 0.36f, 0.61f, 0.64f);
        place(this.yearCycle, box, 0.22f, 0.64f, 0.61f, 0.92f);
        place(this.lunarLabel, box, 0.64f, 0.04f, 1.0f, 0.21f);
        place(this.lunarDay, box, 0.64f, 0.21f, 1.0f, 0.76f);
        place(this.lunarMonth, box, 0.62f, 0.76f, 1.0f, 0.96f);
        place(this.separator, box, 0.0f, 0.985f, 1.0f, 1.0f);
        place(this.guardian, minimalLayout.rows[5], 0.0f, 0.0f, 1.0f, 1.0f);
        place(this.term, minimalLayout.rows[6], 0.0f, 0.0f, 0.5f, 1.0f);
        place(this.opposite, minimalLayout.rows[6], 0.52f, 0.0f, 1.0f, 1.0f);
        place(this.hourTitle, minimalLayout.rows[7], 0.0f, 0.0f, 1.0f, 1.0f);
        for (int i3 = 0; i3 < this.hourNames.length; i3++) {
            int i4 = i3 % 3;
            int i5 = i3 / 3;
            float f = (i4 / 3.0f) + 0.007f;
            float f2 = ((i4 + 1) / 3.0f) - 0.007f;
            float f3 = (i5 / 2.0f) + 0.025f;
            float f4 = ((i5 + 1) / 2.0f) - 0.025f;
            float f5 = f4 - f3;
            place(this.hourBackgrounds[i3], minimalLayout.rows[8], f, f3, f2, f4);
            place(this.hourNames[i3], minimalLayout.rows[8], f + 0.012f, f3 + (f5 * 0.06f), f2 - 0.012f, f3 + (f5 * 0.52f));
            place(this.hourRanges[i3], minimalLayout.rows[8], f + 0.012f, f3 + (f5 * 0.52f), f2 - 0.012f, f3 + (f5 * 0.94f));
        }
        place(this.note, minimalLayout.rows[9], 0.0f, 0.12f, 1.0f, 1.0f);
        for (Placement placement : this.placements) {
            placement.view.measure(View.MeasureSpec.makeMeasureSpec(placement.right - placement.left, MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(placement.bottom - placement.top, MeasureSpec.EXACTLY));
        }
    }

    private void place(View view, MinimalLayout.Box box, float f, float f2, float f3, float f4) {
        int paddingLeft = getPaddingLeft() + box.left;
        int paddingTop = getPaddingTop() + box.top;
        this.placements.add(new Placement(view, paddingLeft + Math.round(box.width() * f), paddingTop + Math.round(box.height() * f2), paddingLeft + Math.round(box.width() * f3), paddingTop + Math.round(box.height() * f4)));
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (glassLayer != null) glassLayer.layout(0, 0, i3 - i, i4 - i2);
        for (Placement placement : this.placements) {
            placement.view.layout(placement.left, placement.top, placement.right, placement.bottom);
        }
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new ViewGroup.LayoutParams(-2, -2);
    }

    private Calendar selected() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(this.calendar.getSelectedDateMillis());
        return calendar;
    }

    void refresh() {
        updateBackground();
        Calendar calendarSelected = selected();
        int i = calendarSelected.get(5);
        int i2 = calendarSelected.get(2) + 1;
        int i3 = calendarSelected.get(1);
        LunarCalendar.LunarDate lunarDateFromSolar = LunarCalendar.fromSolar(i, i2, i3);
        this.monthTitle.setText("Tháng " + i2 + "  ·  " + i3);
        this.solarDay.setText(Integer.toString(i));
        this.solarDay.setContentDescription("Ngày " + i + " tháng " + i2 + " năm " + i3);
        String str = this.weekdayFormat.format(calendarSelected.getTime());
        this.weekday.setText(str.substring(0, 1).toUpperCase(VI) + str.substring(1));
        this.solarDay.setTextColor(calendarSelected.get(7) == 1 ? holidayText() : primaryText());
        this.lunarDay.setText(Integer.toString(lunarDateFromSolar.day));
        this.lunarMonth.setText("Tháng " + lunarDateFromSolar.month + (lunarDateFromSolar.leap ? " nhuận" : ""));
        this.dayCycle.setText(LunarCalendar.dayCanChi(lunarDateFromSolar.julianDay));
        this.monthCycle.setText(LunarCalendar.monthCanChi(lunarDateFromSolar.month, lunarDateFromSolar.year));
        this.yearCycle.setText(LunarCalendar.yearCanChi(lunarDateFromSolar.year));
        boolean zIsAuspiciousDay = TraditionalCalendar.isAuspiciousDay(lunarDateFromSolar);
        this.guardian.setText(labeledValue(zIsAuspiciousDay ? "Ngày hoàng đạo" : "Ngày hắc đạo", TraditionalCalendar.dayGuardian(lunarDateFromSolar), zIsAuspiciousDay ? accentText() : primaryText()));
        this.guardian.setTextColor(zIsAuspiciousDay ? accentText() : secondaryText());
        this.term.setText(labeledValue("Tiết khí", LunarCalendar.solarTerm(lunarDateFromSolar), primaryText()));
        this.opposite.setText(labeledValue("Chi xung ngày", TraditionalCalendar.opposingBranch(lunarDateFromSolar), primaryText()));
        String holiday = HolidayUtil.getHoliday(i, i2, lunarDateFromSolar);
        this.holiday.setText(holiday);
        this.holiday.setVisibility(holiday.isEmpty() ? View.GONE : View.VISIBLE);
        String str2 = NoteRepository.get(this.activity, calendarSelected);
        this.note.setText(str2.isEmpty() ? "Thêm ghi chú" : str2);
        this.note.setContentDescription("Ghi chú ngày " + i + "/" + i2 + ". " + (str2.isEmpty() ? "Chạm để thêm ghi chú" : str2));
        List<String> listAuspiciousHours = TraditionalCalendar.auspiciousHours(lunarDateFromSolar);
        for (int i4 = 0; i4 < this.hourNames.length; i4++) {
            String str3 = listAuspiciousHours.get(i4);
            int iIndexOf = str3.indexOf(10);
            String[] strArrSplit = str3.substring(iIndexOf + 1).replace("h", "").split("–");
            this.hourNames[i4].setText(str3.substring(0, iIndexOf));
            this.hourRanges[i4].setText(strArrSplit[0] + ":00 - " + strArrSplit[1] + ":00");
            this.hourNames[i4].setContentDescription("Giờ " + str3.substring(0, iIndexOf));
        }
        requestLayout();
    }

    private void chooseDate() {
        new WheelDatePickerDialog(this.activity, selected(), (i, i2, i3) -> {
            Calendar calendarSelected = selected();
            calendarSelected.set(5, 1);
            calendarSelected.set(i, i2, i3);
            showDate(calendarSelected);
        }).show();
    }

    private void showDate(Calendar calendar) {
        if (this.tearing) {
            finishTear(false);
        }
        long selectedDateMillis = this.calendar.getSelectedDateMillis();
        this.calendar.showDateImmediately(calendar.getTimeInMillis());
        refresh();
        if (selectedDateMillis == this.calendar.getSelectedDateMillis() || !ValueAnimator.areAnimatorsEnabled()) {
            return;
        }
        int i = calendar.getTimeInMillis() > selectedDateMillis ? 1 : -1;
        View[] viewArr = {this.monthTitle, this.solarDay, this.weekday, this.lunarDay, this.lunarMonth, this.dayCycle, this.monthCycle, this.yearCycle, this.term, this.guardian, this.opposite, this.holiday, this.note};
        for (int i2 = 0; i2 < viewArr.length; i2++) {
            UiKit.animateContent(viewArr[i2], 6 * i, 0.0f, ((long) Math.min(i2 / 4, 2)) * 10);
        }
        for (int i3 = 0; i3 < 6; i3++) {
            UiKit.animateContent(this.hourNames[i3], 4 * i, 0.0f, 20L);
            UiKit.animateContent(this.hourRanges[i3], 4 * i, 0.0f, 20L);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (this.tearAnimator != null) {
            return true;
        }
        if (motionEvent.getActionMasked() == 0) {
            this.touchX = motionEvent.getX();
            this.touchY = motionEvent.getY();
            if (this.tearing) {
                finishTear(this.tearCommitted);
            }
        } else if (motionEvent.getActionMasked() == 2 && this.touchY >= this.paper.getTop() && this.touchY <= this.paper.getBottom()) {
            float x = motionEvent.getX() - this.touchX;
            float y = motionEvent.getY() - this.touchY;
            if (!this.tearing && Math.max(Math.abs(x), Math.abs(y)) > dp(12.0f)) {
                this.verticalTear = Math.abs(y) >= Math.abs(x);
                if (beginTear() && getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
        }
        return this.tearing;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.tearAnimator != null) {
            return true;
        }
        switch (motionEvent.getActionMasked()) {
            case 0:
                this.touchX = motionEvent.getX();
                this.touchY = motionEvent.getY();
                return this.touchY >= ((float) this.paper.getTop()) && this.touchY <= ((float) this.paper.getBottom());
            case 1:
                if (this.tearing) {
                    settleTear(this.tearProgress >= 0.18f);
                } else {
                    performClick();
                }
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
                return true;
            case 2:
                if (!this.tearing && this.touchY >= this.paper.getTop() && this.touchY <= this.paper.getBottom()) {
                    float x = motionEvent.getX() - this.touchX;
                    float y = motionEvent.getY() - this.touchY;
                    if (Math.max(Math.abs(x), Math.abs(y)) > dp(12.0f)) {
                        this.verticalTear = Math.abs(y) >= Math.abs(x);
                        if (beginTear() && getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }
                    }
                }
                if (this.tearing) {
                    this.tearProgress = Math.max(0.0f, Math.min(1.0f, Math.abs(this.verticalTear ? motionEvent.getY() - this.touchY : motionEvent.getX() - this.touchX) / Math.max(1.0f, this.verticalTear ? this.paper.getHeight() * 0.65f : this.paper.getWidth() * 0.8f)));
                    this.tearDrift = this.verticalTear ? motionEvent.getX() - this.touchX : motionEvent.getY() - this.touchY;
                    postInvalidateOnAnimation();
                    return true;
                }
                return true;
            case 3:
                if (this.tearing) {
                    settleTear(false);
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

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private boolean beginTear() {
        Calendar calendarSelected = selected();
        calendarSelected.add(5, 1);
        if (calendarSelected.get(1) < 1900 || calendarSelected.get(1) > 2100) {
            return false;
        }
        this.originalDate = this.calendar.getSelectedDateMillis();
        this.tornBounds.set(this.paper.getLeft(), this.paper.getTop(), this.paper.getRight(), this.paper.getBottom());
        boolean animate = ValueAnimator.areAnimatorsEnabled() && paper.getWidth() > 0 && paper.getHeight() > 0;
        if (animate) {
            captureTornPage();
        } else if (tornPage != null) {
            tornPage.recycle();
            tornPage = null;
        }
        this.tearing = true;
        this.tearCommitted = false;
        this.tearProgress = 0.0f;
        this.tearDrift = 0.0f;
        this.calendar.showDateImmediately(calendarSelected.getTimeInMillis());
        refresh();
        return true;
    }

    private void captureTornPage() {
        int paperWidth = paper.getWidth(), paperHeight = paper.getHeight();
        float scale = Math.min(1f, (float) Math.sqrt(MAX_TORN_PAGE_PIXELS / (double) (paperWidth * (long) paperHeight)));
        int width = Math.max(1, Math.round(paperWidth * scale));
        int height = Math.max(1, Math.round(paperHeight * scale));
        if (tornPage == null || tornPage.getWidth() != width || tornPage.getHeight() != height) {
            if (tornPage != null) tornPage.recycle();
            tornPage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(tornPage);
        canvas.scale(width / (float) paperWidth, height / (float) paperHeight);
        canvas.translate(-paper.getLeft(), -paper.getTop());
        drawCalendarBackdrop(canvas, !glassEnabled);
        if (glassEnabled) drawTornPageSurfaces(canvas);
        super.dispatchDraw(canvas);
    }

    private void settleTear(final boolean z) {
        this.tearCommitted = z;
        if (!ValueAnimator.areAnimatorsEnabled()) {
            finishTear(z);
            return;
        }
        float[] fArr = new float[2];
        fArr[0] = this.tearProgress;
        fArr[1] = z ? 1.0f : 0.0f;
        this.tearAnimator = ValueAnimator.ofFloat(fArr);
        this.tearAnimator.setDuration(z ? Math.max(280, Math.round(680.0f * (1.0f - Math.min(1.0f, this.tearProgress)))) : 220L);
        this.tearAnimator.setInterpolator(z ? UiKit.MOTION_EASING : UiKit.RELEASE_SPRING);
        this.tearAnimator.addUpdateListener(valueAnimator -> {
            this.tearProgress = Math.max(0.0f, ((Float) valueAnimator.getAnimatedValue()).floatValue());
            postInvalidateOnAnimation();
        });
        this.tearAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (MinimalCalendarView.this.tearAnimator == animator) {
                    MinimalCalendarView.this.finishTear(z);
                }
            }
        });
        this.tearAnimator.start();
    }

    public void finishTear(boolean z) {
        ValueAnimator valueAnimator = this.tearAnimator;
        this.tearAnimator = null;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        if (!z && this.tearing) {
            this.calendar.showDateImmediately(this.originalDate);
            refresh();
        }
        this.tearing = false;
        this.tearProgress = 0.0f;
        invalidate();
    }

    // The old page is flattened only while it curls. Its bitmap uses the same photo and
    // stationary colors, while the newly revealed page stays on the live GPU glass layer.
    private void drawTornPageSurfaces(Canvas canvas) {
        int saved = canvas.save();
        drawTornSurface(canvas, paper, dp(MiuixMetrics.CARD_RADIUS), 0x78242424);
        drawTornSurface(canvas, weekday, weekday.getHeight() / 2f, 0x351c2538);
        drawTornSurface(canvas, note, note.getHeight() / 2f, 0x351c2538);
        for (View hour : hourBackgrounds) drawTornSurface(canvas, hour, dp(6f), 0x351c2538);
        canvas.restoreToCount(saved);
    }

    private void drawTornSurface(Canvas canvas, View view, float radius, int color) {
        backgroundPaint.setColor(color);
        canvas.drawRoundRect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom(),
                radius, radius, backgroundPaint);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (child == glassLayer && !canvas.isHardwareAccelerated()) return false;
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (!tearing || tornPage == null) return;
        float width = tornBounds.width(), height = tornBounds.height();
        float progress = Math.min(1, tearProgress);
        float release = Math.max(0, (progress - .42f) / .58f);
        PaperTearGeometry.fill(width, height, progress, tearDrift, tearMesh, tearShades);
        int saved = canvas.save();
        canvas.clipRect(0, tornBounds.top, getWidth(), getHeight());
        canvas.translate(tornBounds.left, tornBounds.top);
        float direction = tearDrift < 0 ? -1 : 1;
        canvas.rotate(direction * 9 * release * release, width * .5f, height * .35f);
        // The shadow and image share exactly the same continuous outline.
        int shadow = canvas.save();
        canvas.translate(dp(3) * progress, dp(8) * progress);
        tearPaint.setColorFilter(shadowFilter);
        tearPaint.setAlpha(Math.round(48 * Math.min(1, progress * 3)));
        canvas.drawBitmapMesh(tornPage, PaperTearGeometry.COLUMNS, PaperTearGeometry.ROWS,
                tearMesh, 0, null, 0, tearPaint);
        canvas.restoreToCount(shadow);
        tearPaint.setColorFilter(null);
        tearPaint.setAlpha(255);
        canvas.drawBitmapMesh(tornPage, PaperTearGeometry.COLUMNS, PaperTearGeometry.ROWS,
                tearMesh, 0, tearShades, 0, tearPaint);
        canvas.restoreToCount(saved);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setScrollable(true);
        accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
    }

    @Override
    public boolean performAccessibilityAction(int i, Bundle bundle) {
        if (i == 4096) {
            if (this.tearAnimator != null || this.tearing || !beginTear()) {
                return true;
            }
            settleTear(true);
            return true;
        }
        if (i == 8192) {
            return false;
        }
        return super.performAccessibilityAction(i, bundle);
    }

    @Override
    protected void onDetachedFromWindow() {
        backgroundDetached = true;
        backgroundGeneration++;
        backgroundRevision = Long.MIN_VALUE;
        if (backgroundImage != null) { backgroundImage.recycle(); backgroundImage = null; }
        if (this.tearing) {
            finishTear(this.tearCommitted);
        }
        if (tornPage != null) {
            tornPage.recycle();
            tornPage = null;
        }
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            childAt.animate().cancel();
            childAt.setAlpha(1.0f);
            childAt.setTranslationX(0.0f);
            childAt.setScaleX(1.0f);
            childAt.setScaleY(1.0f);
        }
        super.onDetachedFromWindow();
    }

    private CharSequence labeledValue(String str, String str2, int i) {
        SpannableString spannableString = new SpannableString(str + "\n" + str2);
        spannableString.setSpan(new RelativeSizeSpan(0.84f), 0, str.length(), 33);
        spannableString.setSpan(new ForegroundColorSpan(secondaryText()), 0, str.length(), 33);
        spannableString.setSpan(new StyleSpan(1), str.length() + 1, spannableString.length(), 33);
        spannableString.setSpan(new ForegroundColorSpan(i), str.length() + 1, spannableString.length(), 33);
        return spannableString;
    }

    private TextView text(String str, int i, int i2, boolean z, int i3, int i4) {
        TextView textViewText = UiKit.text(this.activity, str, i, i2, z);
        textViewText.setGravity(i3);
        textViewText.setMaxLines(i4);
        textViewText.setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_SIMPLE);
        textViewText.setAutoSizeTextTypeUniformWithConfiguration(4, i, 1, 2);
        addView(textViewText);
        return textViewText;
    }

    private TextView button(String str, int i, int i2) {
        TextView textViewButton = UiKit.button(this.activity, str, i, i2, 16.0f);
        textViewButton.setSingleLine(true);
        textViewButton.setAutoSizeTextTypeUniformWithConfiguration(11, 15, 1, 2);
        textViewButton.setContentDescription(str);
        addView(textViewButton);
        return textViewButton;
    }

    private int dp(float f) {
        return UiKit.dp(this.activity, f);
    }

    private static final class Placement {
        final View view;
        final int left;
        final int top;
        final int right;
        final int bottom;

        Placement(View view, int i, int i2, int i3, int i4) {
            this.view = view;
            this.left = i;
            this.top = i2;
            this.right = i3;
            this.bottom = i4;
        }
    }
}
