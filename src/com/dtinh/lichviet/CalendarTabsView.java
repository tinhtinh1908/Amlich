package com.dtinh.lichviet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Calendar;

public final class CalendarTabsView extends FrameLayout {
    private static final float TAB_HEIGHT_DP = 30f;
    private static final float TAB_GROUP_HEIGHT_DP = 40f;
    private static final float ACTION_HEIGHT_DP = 36f;
    private static final String[] WEEKDAYS = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
    private static final String[] DAY_LABELS = new String[32];
    static {
        for (int day = 1; day < DAY_LABELS.length; day++) DAY_LABELS[day] = Integer.toString(day);
    }
    private final Activity activity;
    private final CalendarMonthView month;
    private UiKit.Palette palette;
    private UiKit.Palette contentPalette;
    private UiKit.Palette glassPalette;
    private boolean photoBackground;
    private final LinearLayout yearPage;
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF backgroundBounds = new RectF();
    private Bitmap backgroundImage;
    private final LinearLayout months;
    private final TextView title;
    private final LinearLayout toolbar;
    private final Typeface miniMonthNormal;
    private final Typeface miniMonthHeading;
    private final TextView monthTab;
    private final TextView yearTab;
    private final TextView todayButton;
    private final CalendarGlassLayer glassLayer;
    private boolean glassEnabled;
    private final TextView settingsButton;
    private int year;
    private int gridYear;
    private int gridColumns;
    private int todayYear;
    private int todayMonth;
    private int todayDay;
    private boolean showingYear;

    public CalendarTabsView(Activity activity, CalendarMonthView calendarMonthView, Bundle bundle) {
        super(activity);
        this.activity = activity;
        setWillNotDraw(false);
        this.month = calendarMonthView;
        int font = ThemeManager.getFont(activity);
        this.miniMonthNormal = ThemeManager.font(activity, font, Typeface.DEFAULT, Typeface.NORMAL);
        this.miniMonthHeading = ThemeManager.font(activity, font,
                Typeface.create("sans-serif-medium", Typeface.NORMAL), Typeface.BOLD);
        reloadPalettes();
        this.year = Math.max(1900, Math.min(2100, bundle == null ? Calendar.getInstance().get(1) : bundle.getInt("overview_year", 2026)));
        FrameLayout frameLayout = new FrameLayout(activity);
        addView(frameLayout, new FrameLayout.LayoutParams(-1, -1));
        frameLayout.addView(calendarMonthView, new FrameLayout.LayoutParams(-1, -1));
        this.yearPage = new LinearLayout(activity);
        this.yearPage.setOrientation(LinearLayout.VERTICAL);
        frameLayout.addView(this.yearPage, new FrameLayout.LayoutParams(-1, -1));
        this.title = UiKit.button(activity, "", this.contentPalette.primary, this.contentPalette.surface, ACTION_HEIGHT_DP / 2f);
        this.title.setTextSize(14.0f);
        this.title.setSingleLine(true);
        this.title.setContentDescription("Chọn năm");
        this.title.setOnClickListener(view -> {
            chooseYear();
        });
        this.months = new LinearLayout(activity);
        this.months.setOrientation(LinearLayout.VERTICAL);
        this.yearPage.addView(this.months, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        boolean compact = activity.getResources().getDisplayMetrics().widthPixels / activity.getResources().getDisplayMetrics().density < 360;
        this.toolbar = new LinearLayout(activity);
        this.toolbar.setGravity(16);
        this.toolbar.setPadding(dp(12.0f), 0, dp(12.0f), 0);
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setPadding(dp(5.0f), dp(5.0f), dp(5.0f), dp(5.0f));
        this.monthTab = UiKit.button(activity, "Tháng", this.contentPalette.accent, this.contentPalette.accentSoft, TAB_HEIGHT_DP / 2f);
        this.yearTab = UiKit.button(activity, "Năm", this.contentPalette.secondary, 0, TAB_HEIGHT_DP / 2f);
        for (TextView textView : new TextView[]{this.monthTab, this.yearTab}) {
            textView.setSingleLine(true);
            textView.setTextSize(14.0f);
            textView.setPadding(dp(8.0f), 0, dp(8.0f), 0);
            textView.setAutoSizeTextTypeUniformWithConfiguration(12, 14, 1, 2);
        }
        linearLayout.addView(this.monthTab, new LinearLayout.LayoutParams(dp(compact ? 60.0f : 64.0f), dp(TAB_HEIGHT_DP)));
        LinearLayout.LayoutParams yearTabParams = new LinearLayout.LayoutParams(dp(compact ? 60.0f : 64.0f), dp(TAB_HEIGHT_DP));
        yearTabParams.setMarginStart(dp(5f));
        linearLayout.addView(this.yearTab, yearTabParams);
        this.toolbar.addView(linearLayout, new LinearLayout.LayoutParams(-2, dp(TAB_GROUP_HEIGHT_DP)));
        this.toolbar.addView(new View(activity), new LinearLayout.LayoutParams(0, 1, 1.0f));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(compact ? 88.0f : 104.0f), dp(ACTION_HEIGHT_DP));
        layoutParams.setMarginEnd(dp(8.0f));
        this.toolbar.addView(this.title, layoutParams);
        this.settingsButton = UiKit.button(activity, "", this.contentPalette.primary, this.contentPalette.surface, ACTION_HEIGHT_DP / 2f);
        UiKit.setIcon(this.settingsButton, R.drawable.ic_settings, this.contentPalette.primary, 18);
        this.settingsButton.setPadding(0, 0, 0, 0);
        this.settingsButton.setContentDescription("Cài đặt");
        this.settingsButton.setOnClickListener(view2 -> {
            activity.startActivity(new Intent(activity, (Class<?>) SettingsActivity.class));
        });
        this.toolbar.addView(this.settingsButton, new LinearLayout.LayoutParams(dp(ACTION_HEIGHT_DP), dp(ACTION_HEIGHT_DP)));
        addView(this.toolbar, new FrameLayout.LayoutParams(-1, dp(48.0f), 48));
        this.monthTab.setOnClickListener(view3 -> {
            showYear(false);
        });
        this.yearTab.setOnClickListener(view4 -> {
            if (!this.showingYear) {
                this.year = Math.max(1900, Math.min(2100, calendarMonthView.getSelectedYear()));
                refreshToday();
                rebuild();
                showYear(true);
            }
        });
        this.todayButton = UiKit.button(activity, "Hôm nay", this.contentPalette.accent, this.contentPalette.accentSoft, ACTION_HEIGHT_DP / 2f);
        this.todayButton.setTextSize(14.0f);
        this.todayButton.setSingleLine(true);
        this.todayButton.setAutoSizeTextTypeUniformWithConfiguration(12, 14, 1, 2);
        this.todayButton.setOnClickListener(view5 -> {
            calendarMonthView.selectToday();
        });
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(dp(80.0f), dp(ACTION_HEIGHT_DP));
        layoutParams2.setMarginEnd(dp(8.0f));
        this.toolbar.addView(this.todayButton, this.toolbar.getChildCount() - 1, layoutParams2);
        glassLayer = Build.VERSION.SDK_INT >= 33
                ? new CalendarGlassLayer(activity, this, month, months, todayButton, settingsButton, monthTab, yearTab, title)
                : null;
        if (glassLayer != null) addView(glassLayer, 0, new FrameLayout.LayoutParams(-1, -1));
        refreshGlassPreference();
        rebuild();
        showYear(bundle != null && bundle.getBoolean("overview_visible"));
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (i != i3 || i2 != i4) reloadBackgroundImage();
        if (i != i3) {
            post(this::rebuild);
        }
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        setPadding(insets.left, insets.top, insets.right, insets.bottom);
        return WindowInsets.CONSUMED;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        int iDp = dp(52.0f);
        this.month.setSystemInsets(iDp, 0);
        this.yearPage.setPadding(0, iDp, 0, 0);
        this.months.setPadding(dp(12.0f), dp(8.0f), dp(12.0f), dp(12.0f));
    }

    void refreshGlassPreference() {
        boolean enabled = glassLayer != null && ThemeManager.isSoftGlassEnabled(activity);
        if (glassLayer != null) {
            glassLayer.setVisibility(enabled ? VISIBLE : GONE);
            if (enabled) glassLayer.updateOpacity(ThemeManager.getSoftGlassOpacity(activity));
        }
        if (enabled == glassEnabled) return;
        glassEnabled = enabled;
        invalidate();
        month.setGlassPanels(enabled);
        if (enabled) glassLayer.setMonthVisible(!showingYear);
        restyleControls();
    }

    public void refreshTheme() {
        reloadPalettes();
        reloadBackgroundImage();
        this.title.setTextColor(this.contentPalette.primary);
        restyleControls();
        rebuild();
    }

    private void reloadPalettes() {
        this.palette = new UiKit.Palette(this.activity);
        this.photoBackground = BackgroundImageManager.hasBackground(this.activity);
        this.contentPalette = this.photoBackground ? UiKit.photoPalette(this.activity) : this.palette;
        this.glassPalette = UiKit.glassCalendarPalette(this.activity, photoBackground);
    }

    public int dp(float f) {
        return Math.round(f * getResources().getDisplayMetrics().density);
    }

    public void showYear(boolean z) {
        boolean z2 = this.showingYear != z;
        this.showingYear = z;
        if (z2) {
            UiKit.transitionPages(z ? this.month : this.yearPage, z ? this.yearPage : this.month,
                    z ? 1 : -1);
        } else {
            this.yearPage.setVisibility(z ? View.VISIBLE : View.GONE);
            this.month.setVisibility(z ? View.GONE : View.VISIBLE);
        }
        this.todayButton.setVisibility(z ? View.GONE : View.VISIBLE);
        if (Build.VERSION.SDK_INT >= 33 && glassEnabled) glassLayer.setMonthVisible(!z);
        this.title.setVisibility(z ? View.VISIBLE : View.GONE);
        restyleTabs();
    }

    public void saveState(Bundle bundle) {
        bundle.putInt("overview_year", this.year);
        bundle.putBoolean("overview_visible", this.showingYear);
        bundle.putLong("selected_date", this.month.getSelectedDateMillis());
    }

    public void refreshToday() {
        Calendar today = Calendar.getInstance();
        int currentYear = today.get(Calendar.YEAR);
        int currentMonth = today.get(Calendar.MONTH);
        int currentDay = today.get(Calendar.DAY_OF_MONTH);
        if (todayYear == currentYear && todayMonth == currentMonth && todayDay == currentDay) return;
        todayYear = currentYear;
        todayMonth = currentMonth;
        todayDay = currentDay;
        for (int i = 0; i < this.months.getChildCount(); i++) {
            LinearLayout linearLayout = (LinearLayout) this.months.getChildAt(i);
            for (int i2 = 0; i2 < linearLayout.getChildCount(); i2++) {
                linearLayout.getChildAt(i2).invalidate();
            }
        }
    }

    private void chooseYear() {
        new WheelDatePickerDialog(this.activity, this.year, i -> {
            if (this.year == i) {
                return;
            }
            this.year = i;
            rebuild();
            UiKit.animateChildren(this.months);
        }).show();
    }

    private void rebuild() {
        int iRound = Math.round((getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels) / getResources().getDisplayMetrics().density);
        int i = iRound < 330 ? 2 : iRound < 720 ? 3 : 4;
        if (gridYear == year && gridColumns == i) return;
        gridYear = year;
        gridColumns = i;
        this.title.setText("Năm " + this.year);
        this.months.removeAllViews();
        for (int i2 = 0; i2 < ((12 + i) - 1) / i; i2++) {
            LinearLayout linearLayout = new LinearLayout(this.activity);
            this.months.addView(linearLayout, new LinearLayout.LayoutParams(-1, 0, 1.0f));
            for (int i3 = 0; i3 < i && (i2 * i) + i3 < 12; i3++) {
                int i4 = (i2 * i) + i3;
                MiniMonth miniMonth = new MiniMonth(i4);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -1, 1.0f);
                layoutParams.setMargins(dp(3.0f), dp(3.0f), dp(3.0f), dp(5.0f));
                styleMiniMonth(miniMonth);
                UiKit.bindPressMotion(miniMonth);
                linearLayout.addView(miniMonth, layoutParams);
                miniMonth.setContentDescription("Tháng " + (i4 + 1) + " năm " + this.year + ". Chạm để xem chi tiết");
                miniMonth.setOnClickListener(view -> {
                    Calendar calendar = Calendar.getInstance();
                    calendar.clear();
                    calendar.set(this.year, i4, 1, 12, 0);
                    this.month.showDate(calendar.getTimeInMillis());
                    showYear(false);
                });
            }
        }
    }

    private void reloadBackgroundImage() {
        Bitmap old = backgroundImage;
        backgroundImage = photoBackground ? BackgroundImageManager.load(activity, getWidth(), getHeight()) : null;
        if (old != null && old != backgroundImage) old.recycle();
        if (Build.VERSION.SDK_INT >= 33 && glassLayer != null) glassLayer.invalidateBackdrop();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawCalendarBackdrop(canvas, !glassEnabled);
    }

    void drawCalendarBackdrop(Canvas canvas, boolean shadePhoto) {
        if (backgroundImage == null || backgroundImage.isRecycled()) {
            canvas.drawColor(glassEnabled ? glassPalette.background : palette.background);
            return;
        }
        backgroundBounds.set(0f, 0f, getWidth(), getHeight());
        BackgroundImageManager.drawCenterCrop(canvas, backgroundImage, backgroundBounds, backgroundPaint);
        if (shadePhoto) canvas.drawColor(Color.argb(112, 0, 0, 0));
    }

    @Override
    protected void onDetachedFromWindow() {
        if (backgroundImage != null) {
            backgroundImage.recycle();
            backgroundImage = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (backgroundImage == null && photoBackground && getWidth() > 0 && getHeight() > 0) {
            reloadBackgroundImage();
        }
    }

    private void restyleControls() {
        this.title.setBackground(glassEnabled ? null : toolbarRipple(contentPalette.surface, ACTION_HEIGHT_DP));
        this.title.setTextColor(glassEnabled ? glassPalette.primary : palette.primary);
        for (int index = 0; index < months.getChildCount(); index++) {
            LinearLayout row = (LinearLayout) months.getChildAt(index);
            for (int column = 0; column < row.getChildCount(); column++) {
                styleMiniMonth(row.getChildAt(column));
            }
        }
        restyleTabs();
        settingsButton.setBackground(glassEnabled ? null
                : toolbarRipple(contentPalette.surface, ACTION_HEIGHT_DP));
        UiKit.setIcon(settingsButton, R.drawable.ic_settings, glassEnabled ? glassPalette.primary : palette.primary, 18);
        todayButton.setTextColor(glassEnabled ? glassPalette.primary : contentPalette.accent);
        todayButton.setBackground(glassEnabled ? null
                : toolbarRipple(contentPalette.accentSoft, ACTION_HEIGHT_DP));
        if (Build.VERSION.SDK_INT >= 33 && glassEnabled) glassLayer.updatePalette(glassPalette.background, glassPalette.surface);
    }

    private void restyleTabs() {
        styleTab(monthTab, !showingYear);
        styleTab(yearTab, showingYear);
        monthTab.setSelected(!showingYear);
        yearTab.setSelected(showingYear);
    }

    private void styleMiniMonth(View monthCard) {
        monthCard.setBackground(glassEnabled ? null
                : UiKit.interactiveBackground(activity, contentPalette.surface, MiuixMetrics.CARD_RADIUS));
    }

    private void styleTab(TextView tab, boolean selected) {
        tab.setTextColor(glassEnabled ? (selected ? 0xff203145 : glassPalette.secondary)
                : (selected ? contentPalette.primary : palette.secondary));
        tab.setBackground(glassEnabled ? null
                : toolbarRipple(selected ? contentPalette.surface : Color.TRANSPARENT, TAB_HEIGHT_DP));
    }

    private GradientDrawable toolbarShape(int color, float heightDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(heightDp) / 2f);
        return shape;
    }

    private RippleDrawable toolbarRipple(int color, float heightDp) {
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(46, 128, 128, 128)),
                toolbarShape(color, heightDp), toolbarShape(Color.WHITE, heightDp));
    }

    private final class MiniMonth extends View {
        private final int monthIndex;
        private final int dayOffset;
        private final int daysInMonth;
        private final String label;
        private final Paint paint;
        private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();

        MiniMonth(int i) {
            super(CalendarTabsView.this.activity);
            this.paint = new Paint(1);
            this.monthIndex = i;
            label = "Tháng " + (i + 1);
            Calendar month = Calendar.getInstance();
            month.clear();
            month.set(year, i, 1, 12, 0);
            dayOffset = (month.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
            setFocusable(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int i;
            super.onDraw(canvas);
            UiKit.Palette palette = glassEnabled ? glassPalette
                    : (!photoBackground || contentPalette.night) ? contentPalette : CalendarTabsView.this.palette;
            boolean z = todayYear == year && todayMonth == monthIndex;
            float fDp = CalendarTabsView.this.dp(6.0f);
            float width = (getWidth() - (2.0f * fDp)) / 7.0f;
            float fMin = Math.min(CalendarTabsView.this.dp(14.0f), getWidth() / 8.0f);
            float f = fDp + fMin;
            this.paint.setTextAlign(Paint.Align.LEFT);
            this.paint.setTypeface(miniMonthHeading);
            this.paint.setTextSize(fMin);
            this.paint.setColor(z ? palette.accent : palette.primary);
            canvas.drawText(label, fDp, f, this.paint);
            this.paint.setTextAlign(Paint.Align.CENTER);
            this.paint.setTypeface(miniMonthNormal);
            this.paint.setTextSize(Math.min(CalendarTabsView.this.dp(8.0f), width * 0.65f));
            float fDp2 = f + CalendarTabsView.this.dp(14.0f);
            int i2 = 0;
            while (i2 < 7) {
                this.paint.setColor(i2 == 6 ? palette.danger : palette.secondary);
                canvas.drawText(WEEKDAYS[i2], fDp + (width * (i2 + 0.5f)), fDp2, this.paint);
                i2++;
            }
            float fDp3 = fDp2 + CalendarTabsView.this.dp(6.0f);
            float fMax = Math.max(1.0f, ((getHeight() - fDp) - fDp3) / 6.0f);
            this.paint.setTextSize(Math.min(CalendarTabsView.this.dp(10.0f), Math.min(width * 0.68f, fMax * 0.62f)));
            this.paint.getFontMetrics(fontMetrics);
            int i4 = 1;
            while (i4 <= daysInMonth) {
                int i5 = dayOffset + i4 - 1;
                int i6 = i5 % 7;
                int i7 = i5 / 7;
                float f2 = fDp + (width * (i6 + 0.5f));
                float f3 = fDp3 + ((i7 + 0.5f) * fMax);
                boolean z2 = z && i4 == todayDay;
                if (z2) {
                    this.paint.setColor(palette.accent);
                    canvas.drawCircle(f2, f3, Math.min(width, fMax) * 0.44f, this.paint);
                }
                Paint paint = this.paint;
                if (z2) {
                    i = palette.onAccent;
                } else {
                    i = i6 == 6 ? palette.danger : palette.primary;
                }
                paint.setColor(i);
                canvas.drawText(DAY_LABELS[i4], f2, f3 - ((fontMetrics.ascent + fontMetrics.descent) / 2.0f), this.paint);
                i4++;
            }
        }
    }
}
