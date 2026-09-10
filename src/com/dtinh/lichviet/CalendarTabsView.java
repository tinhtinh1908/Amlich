package com.dtinh.lichviet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Calendar;

/**
 * Root layout. The month view and the year overview share one full-screen
 * content area; the navigation row floats on top of it so a user background
 * photo shows through behind the buttons instead of a solid black bar.
 */
public final class CalendarTabsView extends FrameLayout {
    private final Activity activity;
    private final CalendarMonthView month;
    private UiKit.Palette palette;
    private UiKit.Palette contentPalette;
    private boolean photoBackground;
    private final FrameLayout content;
    private final YearPageView yearPage;
    private final LinearLayout months;
    private final ScrollView scroll;
    private final LinearLayout navigation;
    private final TextView title;
    private final NavArrow previousButton;
    private final NavArrow nextButton;
    private final NavButton pickerButton, modeButton, todayButton, settingsButton;
    private int year;
    private boolean showingYear;

    public CalendarTabsView(Activity activity, CalendarMonthView month, Bundle state) {
        super(activity);
        this.activity = activity;
        this.month = month;
        reloadPalettes();
        year = Math.max(1900, Math.min(2100, state == null
                ? Calendar.getInstance().get(Calendar.YEAR) : state.getInt("overview_year", 2026)));
        content = new FrameLayout(activity);
        addView(content, new LayoutParams(-1, -1));
        content.addView(month, new FrameLayout.LayoutParams(-1, -1));
        yearPage = new YearPageView(activity);
        content.addView(yearPage, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(10), dp(14), dp(12));
        yearPage.addView(header, new LinearLayout.LayoutParams(-1, dp(62)));
        previousButton = new NavArrow(false, "Năm trước");
        previousButton.setOnClickListener(v -> changeYear(-1));
        header.addView(previousButton, new LinearLayout.LayoutParams(dp(40), dp(40)));
        title = text("", 22);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setContentDescription("Năm đang xem");
        header.addView(title, new LinearLayout.LayoutParams(0, -1, 1));
        nextButton = new NavArrow(true, "Năm sau");
        nextButton.setOnClickListener(v -> changeYear(1));
        header.addView(nextButton, new LinearLayout.LayoutParams(dp(40), dp(40)));

        scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        yearPage.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        months = new LinearLayout(activity);
        months.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(months, new ScrollView.LayoutParams(-1, -2));

        navigation = new LinearLayout(activity);
        navigation.setPadding(dp(10), dp(6), dp(10), dp(12));
        navigation.setGravity(Gravity.CENTER_VERTICAL);
        pickerButton = new NavButton("Chọn ngày", 0);
        pickerButton.setOnClickListener(v -> {
            if (showingYear) chooseYear(); else month.openDatePicker();
        });
        modeButton = new NavButton("Năm", 1);
        modeButton.setOnClickListener(v -> {
            if (showingYear) {
                showYear(false);
            } else {
                year = Math.max(1900, Math.min(2100, month.getSelectedYear()));
                rebuild(); showYear(true);
            }
        });
        todayButton = new NavButton("Hôm nay", 2);
        todayButton.setOnClickListener(v -> month.selectToday());
        settingsButton = new NavButton("Cài đặt", 4);
        settingsButton.setOnClickListener(v -> activity.startActivity(new Intent(activity, SettingsActivity.class)));
        for (NavButton item : new NavButton[]{pickerButton, modeButton, todayButton, settingsButton}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(56), 1);
            lp.setMargins(dp(3), 0, dp(3), 0);
            navigation.addView(item, lp);
        }
        LayoutParams navParams = new LayoutParams(-1, -2, Gravity.BOTTOM);
        addView(navigation, navParams);

        rebuild();
        showYear(state != null && state.getBoolean("overview_visible"));
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        setPadding(bars.left, bars.top, bars.right, bars.bottom);
        return WindowInsets.CONSUMED;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // The navigation row overlays the content: keep the month detail
        // panel and the bottom of the year grid clear of it, and let the
        // background photo run uninterrupted behind the buttons.
        int clearance = getHeight() - navigation.getTop();
        month.setSystemInsets(0, clearance);
        months.setPadding(dp(12), dp(12), dp(12), dp(24) + clearance);
    }

    /** Re-reads theme and background photo state after returning from Settings. */
    public void refreshTheme() {
        reloadPalettes();
        yearPage.refreshBackground();
        title.setTextColor(contentPalette.primary);
        previousButton.invalidate();
        nextButton.invalidate();
        for (NavButton item : new NavButton[]{pickerButton, modeButton, todayButton, settingsButton}) {
            item.restyle();
        }
        rebuild();
    }

    private void reloadPalettes() {
        palette = new UiKit.Palette(activity);
        photoBackground = BackgroundImageManager.hasBackground(activity);
        contentPalette = photoBackground ? UiKit.photoPalette(activity) : palette;
    }

    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size) {
        TextView t = new TextView(activity);
        t.setText(value); t.setTextSize(size); t.setTextColor(contentPalette.primary);
        t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }
    /**
     * Chevron button drawn the same way as CalendarMonthView's month
     * arrows (rounded surface + hand-drawn chevron), so the year tab's
     * prev/next controls line up exactly with the month tab's.
     */
    private final class NavArrow extends View {
        private final boolean next;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        NavArrow(boolean next, String description) {
            super(activity);
            this.next = next;
            setContentDescription(description);
            setFocusable(true);
            setClickable(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            RectF bounds = new RectF(0f, 0f, w, h);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(contentPalette.surface);
            canvas.drawRoundRect(bounds, dp(15), dp(15), paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.8f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(contentPalette.primary);
            float cx = w / 2f;
            float cy = h / 2f;
            float f = next ? 1.0f : -1.0f;
            canvas.drawLine(cx - dp(3) * f, cy - dp(5), cx + dp(3) * f, cy, paint);
            canvas.drawLine(cx + dp(3) * f, cy, cx - dp(3) * f, cy + dp(5), paint);
        }
    }

    public void showYear(boolean visible) {
        showingYear = visible;
        yearPage.setVisibility(visible ? VISIBLE : GONE);
        month.setVisibility(visible ? GONE : VISIBLE);
        pickerButton.setDestination(visible ? "Chọn năm" : "Chọn ngày", visible ? 1 : 0);
        modeButton.setDestination(visible ? "Tháng" : "Năm", visible ? 3 : 1);
        todayButton.setVisibility(visible ? GONE : VISIBLE);
    }
    public void saveState(Bundle out) {
        out.putInt("overview_year", year);
        out.putBoolean("overview_visible", showingYear);
        out.putLong("selected_date", month.getSelectedDateMillis());
    }
    public void refreshToday() { for (int i = 0; i < months.getChildCount(); i++) {
        LinearLayout row = (LinearLayout) months.getChildAt(i);
        for (int j = 0; j < row.getChildCount(); j++) row.getChildAt(j).invalidate();
    } }
    private void changeYear(int delta) {
        year = Math.max(1900, Math.min(2100, year + delta)); rebuild(); scroll.scrollTo(0, 0);
    }
    private void chooseYear() {
        new WheelDatePickerDialog(activity, year, selectedYear -> {
            year = selectedYear;
            rebuild();
            scroll.scrollTo(0, 0);
        }).show();
    }

    private void rebuild() {
        title.setText("Năm " + year); months.removeAllViews();
        for (int r = 0; r < 4; r++) {
            LinearLayout row = new LinearLayout(activity);
            months.addView(row, new LinearLayout.LayoutParams(-1, -2));
            for (int c = 0; c < 3; c++) {
                final int m = r * 3 + c;
                MiniMonth mini = new MiniMonth(m);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(196), 1);
                lp.setMargins(dp(3), dp(4), dp(3), dp(8));
                mini.setBackground(UiKit.rounded(contentPalette.surface, dp(16)));
                row.addView(mini, lp);
                mini.setContentDescription("Tháng " + (m + 1) + " năm " + year + ". Chạm để xem chi tiết");
                mini.setOnClickListener(v -> {
                    Calendar date = Calendar.getInstance(); date.clear(); date.set(year, m, 1, 12, 0);
                    month.showDate(date.getTimeInMillis()); showYear(false);
                });
            }
        }
    }

    /**
     * Year overview page. Draws the user background photo (or the solid
     * theme background) behind its content, matching CalendarMonthView.
     */
    private final class YearPageView extends LinearLayout {
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Bitmap backgroundImage;

        YearPageView(Context context) {
            super(context);
            setOrientation(VERTICAL);
            setWillNotDraw(false);
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            if (width != oldWidth || height != oldHeight) reload(width, height);
        }

        void refreshBackground() {
            reload(getWidth(), getHeight());
            invalidate();
        }

        private void reload(int width, int height) {
            Bitmap replacement = BackgroundImageManager.hasBackground(getContext())
                    ? BackgroundImageManager.load(getContext(), width, height) : null;
            if (backgroundImage != null && backgroundImage != replacement
                    && !backgroundImage.isRecycled()) {
                backgroundImage.recycle();
            }
            backgroundImage = replacement;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            bgPaint.setShader(null);
            bgPaint.setStyle(Paint.Style.FILL);
            if (backgroundImage != null && !backgroundImage.isRecycled()) {
                BackgroundImageManager.drawCenterCrop(canvas, backgroundImage,
                        new RectF(0.0f, 0.0f, getWidth(), getHeight()), bgPaint);
                bgPaint.setColor(Color.argb(112, 0, 0, 0));
                canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), bgPaint);
            } else {
                bgPaint.setColor(contentPalette.background);
                canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), bgPaint);
            }
            super.onDraw(canvas);
        }

        @Override
        protected void onDetachedFromWindow() {
            if (backgroundImage != null && !backgroundImage.isRecycled()) {
                backgroundImage.recycle();
                backgroundImage = null;
            }
            super.onDetachedFromWindow();
        }
    }

    /** One mode switch; hidden actions give their space to the remaining buttons. */
    private final class NavButton extends LinearLayout {
        private final TextView label;
        private final NavIcon icon;
        private int kind;
        NavButton(String name, int kind) {
            super(activity);
            setOrientation(VERTICAL); setGravity(Gravity.CENTER);
            setClickable(true); setFocusable(true); setContentDescription(name);
            this.kind = kind;
            icon = new NavIcon(kind);
            addView(icon, new LinearLayout.LayoutParams(dp(23), dp(23)));
            label = text(name, 11);
            label.setGravity(Gravity.CENTER); label.setSingleLine(true);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(-1, dp(18));
            textParams.topMargin = dp(3); addView(label, textParams);
            applyStyle(kind);
        }
        void setDestination(String name, int kind) {
            this.kind = kind;
            label.setText(name);
            setContentDescription(name);
            icon.kind = kind;
            applyStyle(kind);
        }
        void restyle() {
            applyStyle(kind);
        }
        private void applyStyle(int kind) {
            int color;
            if (kind == 2) {
                // "Hôm nay" keeps its green identity in both themes.
                color = contentPalette.night ? Color.rgb(91, 214, 159) : Color.rgb(24, 143, 94);
            } else if (kind == 4) {
                color = contentPalette.secondary;
            } else {
                color = contentPalette.accent;
            }
            // Strip any alpha the palette may have baked in (photoPalette's
            // accent carries some when frosted) so all 4 buttons go through
            // the same, single alpha decision below instead of only the
            // accent-colored ones reacting to the setting.
            int rgb = Color.rgb(Color.red(color), Color.green(color), Color.blue(color));

            int contentColor = Color.WHITE;
            label.setTextColor(contentColor);
            icon.color = contentColor;
            icon.invalidate();

            // Solid, opaque fill by default (same idea as the "SAO LƯU"
            // button in Cài đặt). Only when "Nền mờ" is on AND there's a
            // background photo do the buttons pick up transparency so the
            // blurred photo shows through — this now applies to all 4
            // buttons uniformly, so the toggle actually has an effect.
            boolean frosted = photoBackground && BackgroundImageManager.isFrostedEnabled(activity);
            int fillAlpha = frosted ? 190 : 255;
            int fill = Color.argb(fillAlpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
            int ripple = Color.argb(90, 255, 255, 255);
            setBackground(new android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(ripple),
                    UiKit.rounded(fill, dp(17)), null));
        }
    }
    private final class NavIcon extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int kind;
        private int color;
        NavIcon(int kind) { super(activity); this.kind = kind;
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            int saved = c.save(); c.scale(getWidth()/24f, getHeight()/24f);
            p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.7f);
            p.setStrokeCap(Paint.Cap.ROUND);
            if (kind == 4) {
                for (int y = 6; y <= 18; y += 6) c.drawLine(4, y, 20, y, p);
            } else if (kind == 2) {
                c.drawCircle(12, 12, 8.5f, p); c.drawLine(12, 6.5f, 12, 12, p);
                c.drawLine(12, 12, 16, 14, p);
            } else {
                c.drawRoundRect(3, 4, 21, 21, 3, 3, p);
                c.drawLine(3, 9, 21, 9, p); c.drawLine(8, 2, 8, 6, p); c.drawLine(16, 2, 16, 6, p);
                p.setStyle(Paint.Style.FILL);
                if (kind == 1) {
                    for (int x = 7; x <= 17; x += 5)
                        for (int y = 13; y <= 18; y += 5) c.drawCircle(x, y, .9f, p);
                } else if (kind == 3) {
                    c.drawRoundRect(6, 12, 18, 14, .6f, .6f, p);
                    c.drawRoundRect(6, 16, 14, 18, .6f, .6f, p);
                } else {
                    c.drawCircle(12, 15, 2.3f, p);
                }
            }
            c.restoreToCount(saved);
        }
    }
    private final class MiniMonth extends View {
        private final int monthIndex;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        MiniMonth(int m) { super(activity); monthIndex = m; setFocusable(true); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            Calendar today = Calendar.getInstance();
            boolean current = today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) == monthIndex;
            float cell = (getWidth() - dp(8)) / 7f;
            paint.setTextAlign(Paint.Align.LEFT); paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            paint.setTextSize(Math.min(dp(16), getWidth() / 6f));
            paint.setColor(current ? contentPalette.accent : contentPalette.primary);
            canvas.drawText("Tháng " + (monthIndex + 1), dp(8), dp(28), paint);
            paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(Math.min(dp(10), cell * .7f));
            String[] weekdays = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
            paint.setColor(contentPalette.secondary);
            for (int i = 0; i < 7; i++) {
                paint.setColor(i == 6 ? contentPalette.danger : contentPalette.secondary);
                canvas.drawText(weekdays[i], dp(4) + cell * (i + .5f), dp(54), paint);
            }
            Calendar date = Calendar.getInstance(); date.clear(); date.set(year, monthIndex, 1, 12, 0);
            int start = (date.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            int count = date.getActualMaximum(Calendar.DAY_OF_MONTH);
            paint.setTextSize(Math.min(dp(11), cell * .68f));
            for (int day = 1; day <= count; day++) {
                int position = start + day - 1, col = position % 7, row = position / 7;
                float x = dp(4) + cell * (col + .5f), baseline = dp(77 + row * 21);
                boolean isToday = current && day == today.get(Calendar.DAY_OF_MONTH);
                if (isToday) {
                    paint.setColor(contentPalette.accent);
                    canvas.drawRoundRect(new RectF(x - cell * .48f, baseline - dp(14), x + cell * .48f,
                        baseline + dp(4)), dp(5), dp(5), paint);
                }
                paint.setColor(isToday ? Color.WHITE : col == 6 ? contentPalette.danger : contentPalette.primary);
                canvas.drawText(Integer.toString(day), x, baseline, paint);
            }
        }
    }
}
