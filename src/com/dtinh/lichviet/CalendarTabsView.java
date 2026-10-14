package com.dtinh.lichviet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Calendar;

public final class CalendarTabsView extends FrameLayout {
    private final Activity activity;
    private final CalendarMonthView month;
    private UiKit.Palette palette;
    private UiKit.Palette contentPalette;
    private boolean photoBackground;
    private final YearPageView yearPage;
    private final LinearLayout months;
    private final TextView title;
    private final LinearLayout toolbar;
    private final TextView monthTab;
    private final TextView yearTab;
    private final TextView todayButton;
    private final TextView settingsButton;
    private int year;
    private boolean showingYear;

    public CalendarTabsView(Activity activity, CalendarMonthView calendarMonthView, Bundle bundle) {
        super(activity);
        this.activity = activity;
        this.month = calendarMonthView;
        reloadPalettes();
        this.year = Math.max(1900, Math.min(2100, bundle == null ? Calendar.getInstance().get(1) : bundle.getInt("overview_year", 2026)));
        FrameLayout frameLayout = new FrameLayout(activity);
        addView(frameLayout, new FrameLayout.LayoutParams(-1, -1));
        frameLayout.addView(calendarMonthView, new FrameLayout.LayoutParams(-1, -1));
        this.yearPage = new YearPageView(activity);
        frameLayout.addView(this.yearPage, new FrameLayout.LayoutParams(-1, -1));
        this.title = UiKit.button(activity, "", this.contentPalette.primary, this.contentPalette.surface, 16.0f);
        this.title.setTextSize(14.0f);
        this.title.setSingleLine(true);
        this.title.setContentDescription("Chọn năm");
        this.title.setOnClickListener(view -> {
            chooseYear();
        });
        this.months = new LinearLayout(activity);
        this.months.setOrientation(1);
        this.yearPage.addView(this.months, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        boolean compact = activity.getResources().getDisplayMetrics().widthPixels / activity.getResources().getDisplayMetrics().density < 360;
        this.toolbar = new LinearLayout(activity);
        this.toolbar.setGravity(16);
        this.toolbar.setPadding(dp(12.0f), 0, dp(12.0f), 0);
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setPadding(dp(5.0f), dp(5.0f), dp(5.0f), dp(5.0f));
        linearLayout.setBackground(UiKit.rounded(this.contentPalette.background, dp(MiuixMetrics.TAB_OUTER_RADIUS)));
        this.monthTab = UiKit.button(activity, "Tháng", this.contentPalette.accent, this.contentPalette.accentSoft, 12.0f);
        this.yearTab = UiKit.button(activity, "Năm", this.contentPalette.secondary, 0, 12.0f);
        for (TextView textView : new TextView[]{this.monthTab, this.yearTab}) {
            textView.setSingleLine(true);
            textView.setTextSize(14.0f);
            textView.setPadding(dp(8.0f), 0, dp(8.0f), 0);
            textView.setAutoSizeTextTypeUniformWithConfiguration(12, 14, 1, 2);
        }
        linearLayout.addView(this.monthTab, new LinearLayout.LayoutParams(dp(compact ? 60.0f : 64.0f), dp(30.0f)));
        LinearLayout.LayoutParams yearTabParams = new LinearLayout.LayoutParams(dp(compact ? 60.0f : 64.0f), dp(30.0f));
        yearTabParams.setMarginStart(dp(5f));
        linearLayout.addView(this.yearTab, yearTabParams);
        this.toolbar.addView(linearLayout, new LinearLayout.LayoutParams(-2, dp(40.0f)));
        this.toolbar.addView(new View(activity), new LinearLayout.LayoutParams(0, 1, 1.0f));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(compact ? 88.0f : 104.0f), dp(36.0f));
        layoutParams.setMarginEnd(dp(8.0f));
        this.toolbar.addView(this.title, layoutParams);
        this.settingsButton = UiKit.button(activity, "", this.contentPalette.primary, this.contentPalette.surface, 16.0f);
        UiKit.setIcon(this.settingsButton, R.drawable.ic_settings, this.contentPalette.primary, 18);
        this.settingsButton.setPadding(0, 0, 0, 0);
        this.settingsButton.setContentDescription("Cài đặt");
        this.settingsButton.setOnClickListener(view2 -> {
            activity.startActivity(new Intent(activity, (Class<?>) SettingsActivity.class));
        });
        this.toolbar.addView(this.settingsButton, new LinearLayout.LayoutParams(dp(36.0f), dp(36.0f)));
        addView(this.toolbar, new FrameLayout.LayoutParams(-1, dp(48.0f), 48));
        this.monthTab.setOnClickListener(view3 -> {
            showYear(false);
        });
        this.yearTab.setOnClickListener(view4 -> {
            if (!this.showingYear) {
                this.year = Math.max(1900, Math.min(2100, calendarMonthView.getSelectedYear()));
                rebuild();
                showYear(true);
            }
        });
        this.todayButton = UiKit.button(activity, "Hôm nay", this.contentPalette.accent, this.contentPalette.accentSoft, 16.0f);
        this.todayButton.setTextSize(14.0f);
        this.todayButton.setSingleLine(true);
        this.todayButton.setAutoSizeTextTypeUniformWithConfiguration(12, 14, 1, 2);
        this.todayButton.setOnClickListener(view5 -> {
            calendarMonthView.selectToday();
        });
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(dp(80.0f), dp(36.0f));
        layoutParams2.setMarginEnd(dp(8.0f));
        this.toolbar.addView(this.todayButton, this.toolbar.getChildCount() - 1, layoutParams2);
        rebuild();
        showYear(bundle != null && bundle.getBoolean("overview_visible"));
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
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

    public void refreshTheme() {
        reloadPalettes();
        this.yearPage.refreshBackground();
        this.title.setTextColor(this.contentPalette.primary);
        restyleControls();
        rebuild();
    }

    private void reloadPalettes() {
        this.palette = new UiKit.Palette(this.activity);
        this.photoBackground = BackgroundImageManager.hasBackground(this.activity);
        this.contentPalette = this.photoBackground ? UiKit.photoPalette(this.activity) : this.palette;
    }

    public int dp(float f) {
        return Math.round(f * getResources().getDisplayMetrics().density);
    }

    public void showYear(boolean z) {
        boolean z2 = this.showingYear != z;
        this.showingYear = z;
        if (z2) {
            UiKit.transitionPages(z ? this.month : this.yearPage, z ? this.yearPage : this.month, z ? 1 : -1);
        } else {
            this.yearPage.setVisibility(z ? 0 : 8);
            this.month.setVisibility(z ? 8 : 0);
        }
        this.todayButton.setVisibility(z ? 8 : 0);
        this.title.setVisibility(z ? 0 : 8);
        restyleControls();
        requestLayout();
    }

    public void saveState(Bundle bundle) {
        bundle.putInt("overview_year", this.year);
        bundle.putBoolean("overview_visible", this.showingYear);
        bundle.putLong("selected_date", this.month.getSelectedDateMillis());
    }

    public void refreshToday() {
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
        this.title.setText("Năm " + this.year);
        this.months.removeAllViews();
        int iRound = Math.round((getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels) / getResources().getDisplayMetrics().density);
        int i = iRound < 330 ? 2 : iRound < 720 ? 3 : 4;
        for (int i2 = 0; i2 < ((12 + i) - 1) / i; i2++) {
            LinearLayout linearLayout = new LinearLayout(this.activity);
            this.months.addView(linearLayout, new LinearLayout.LayoutParams(-1, 0, 1.0f));
            for (int i3 = 0; i3 < i && (i2 * i) + i3 < 12; i3++) {
                int i4 = (i2 * i) + i3;
                MiniMonth miniMonth = new MiniMonth(i4);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -1, 1.0f);
                layoutParams.setMargins(dp(3.0f), dp(3.0f), dp(3.0f), dp(5.0f));
                miniMonth.setBackground(UiKit.interactiveBackground(this.activity, this.contentPalette.surface, MiuixMetrics.CARD_RADIUS));
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

    private final class YearPageView extends LinearLayout {
        private final Paint bgPaint;
        private Bitmap backgroundImage;

        YearPageView(Context context) {
            super(context);
            this.bgPaint = new Paint(1);
            setOrientation(1);
            setWillNotDraw(false);
        }

        @Override
        protected void onSizeChanged(int i, int i2, int i3, int i4) {
            super.onSizeChanged(i, i2, i3, i4);
            if (i != i3 || i2 != i4) {
                reload(i, i2);
            }
        }

        void refreshBackground() {
            reload(getWidth(), getHeight());
            invalidate();
        }

        private void reload(int i, int i2) {
            Bitmap bitmapLoad = BackgroundImageManager.hasBackground(getContext()) ? BackgroundImageManager.load(getContext(), i, i2) : null;
            if (this.backgroundImage != null && this.backgroundImage != bitmapLoad && !this.backgroundImage.isRecycled()) {
                this.backgroundImage.recycle();
            }
            this.backgroundImage = bitmapLoad;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            this.bgPaint.setShader(null);
            this.bgPaint.setStyle(Paint.Style.FILL);
            if (this.backgroundImage == null || this.backgroundImage.isRecycled()) {
                this.bgPaint.setColor(CalendarTabsView.this.contentPalette.background);
                canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), this.bgPaint);
            } else {
                BackgroundImageManager.drawCenterCrop(canvas, this.backgroundImage, new RectF(0.0f, 0.0f, getWidth(), getHeight()), this.bgPaint);
                this.bgPaint.setColor(Color.argb(112, 0, 0, 0));
                canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), this.bgPaint);
            }
            super.onDraw(canvas);
        }

        @Override
        protected void onDetachedFromWindow() {
            if (this.backgroundImage != null && !this.backgroundImage.isRecycled()) {
                this.backgroundImage.recycle();
                this.backgroundImage = null;
            }
            super.onDetachedFromWindow();
        }
    }

    private void restyleControls() {
        this.title.setBackground(UiKit.interactiveBackground(this.activity, this.contentPalette.surface, 16.0f));
        this.title.setTextColor(this.palette.primary);
        styleTab(this.monthTab, !this.showingYear);
        styleTab(this.yearTab, this.showingYear);
        this.monthTab.setSelected(!this.showingYear);
        this.yearTab.setSelected(this.showingYear);
        ((LinearLayout) this.monthTab.getParent()).setBackground(UiKit.rounded(this.contentPalette.background, dp(MiuixMetrics.TAB_OUTER_RADIUS)));
        this.settingsButton.setBackground(UiKit.interactiveBackground(this.activity, this.contentPalette.surface, 16.0f));
        UiKit.setIcon(this.settingsButton, R.drawable.ic_settings, this.palette.primary, 18);
        this.todayButton.setTextColor(this.contentPalette.accent);
        this.todayButton.setBackground(UiKit.interactiveBackground(this.activity, this.contentPalette.accentSoft, 16.0f));
    }

    private void styleTab(TextView textView, boolean z) {
        textView.setTextColor(z ? this.contentPalette.primary : this.palette.secondary);
        textView.setBackground(UiKit.interactiveBackground(this.activity, z ? this.contentPalette.surface : 0, MiuixMetrics.TAB_RADIUS));
    }

    private final class MiniMonth extends View {
        private final int monthIndex;
        private final Paint paint;

        MiniMonth(int i) {
            super(CalendarTabsView.this.activity);
            this.paint = new Paint(1);
            this.monthIndex = i;
            setFocusable(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int i;
            super.onDraw(canvas);
            UiKit.Palette palette = (!CalendarTabsView.this.photoBackground || CalendarTabsView.this.contentPalette.night) ? CalendarTabsView.this.contentPalette : CalendarTabsView.this.palette;
            Calendar calendar = Calendar.getInstance();
            boolean z = calendar.get(1) == CalendarTabsView.this.year && calendar.get(2) == this.monthIndex;
            float fDp = CalendarTabsView.this.dp(6.0f);
            float width = (getWidth() - (2.0f * fDp)) / 7.0f;
            float fMin = Math.min(CalendarTabsView.this.dp(14.0f), getWidth() / 8.0f);
            float f = fDp + fMin;
            this.paint.setTextAlign(Paint.Align.LEFT);
            this.paint.setTypeface(Typeface.create("sans-serif-medium", 0));
            this.paint.setTextSize(fMin);
            this.paint.setColor(z ? palette.accent : palette.primary);
            canvas.drawText("Tháng " + (this.monthIndex + 1), fDp, f, this.paint);
            this.paint.setTextAlign(Paint.Align.CENTER);
            this.paint.setTypeface(Typeface.DEFAULT);
            this.paint.setTextSize(Math.min(CalendarTabsView.this.dp(8.0f), width * 0.65f));
            float fDp2 = f + CalendarTabsView.this.dp(14.0f);
            String[] strArr = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
            int i2 = 0;
            while (i2 < 7) {
                this.paint.setColor(i2 == 6 ? palette.danger : palette.secondary);
                canvas.drawText(strArr[i2], fDp + (width * (i2 + 0.5f)), fDp2, this.paint);
                i2++;
            }
            Calendar calendar2 = Calendar.getInstance();
            calendar2.clear();
            calendar2.set(CalendarTabsView.this.year, this.monthIndex, 1, 12, 0);
            int i3 = (calendar2.get(7) + 5) % 7;
            int actualMaximum = calendar2.getActualMaximum(5);
            float fDp3 = fDp2 + CalendarTabsView.this.dp(6.0f);
            float fMax = Math.max(1.0f, ((getHeight() - fDp) - fDp3) / 6.0f);
            this.paint.setTextSize(Math.min(CalendarTabsView.this.dp(10.0f), Math.min(width * 0.68f, fMax * 0.62f)));
            Paint.FontMetrics fontMetrics = this.paint.getFontMetrics();
            int i4 = 1;
            while (i4 <= actualMaximum) {
                int i5 = (i3 + i4) - 1;
                int i6 = i5 % 7;
                int i7 = i5 / 7;
                float f2 = fDp + (width * (i6 + 0.5f));
                float f3 = fDp3 + ((i7 + 0.5f) * fMax);
                boolean z2 = z && i4 == calendar.get(5);
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
                canvas.drawText(Integer.toString(i4), f2, f3 - ((fontMetrics.ascent + fontMetrics.descent) / 2.0f), this.paint);
                i4++;
            }
        }
    }
}
