package com.dtinh.lichviet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Calendar;

/** Native year overview; the original lunar month view remains intact. */
public final class CalendarTabsView extends LinearLayout {
    private final Activity activity;
    private final CalendarMonthView month;
    private final UiKit.Palette palette;
    private final FrameLayout content;
    private final LinearLayout yearPage;
    private final LinearLayout months;
    private final ScrollView scroll;
    private final TextView title;
    private final NavButton yearTab, monthTab;
    private int year;
    private boolean showingYear;

    public CalendarTabsView(Activity activity, CalendarMonthView month, Bundle state) {
        super(activity);
        this.activity = activity;
        this.month = month;
        month.useExternalNavigation();
        palette = new UiKit.Palette(activity);
        year = Math.max(1900, Math.min(2100, state == null
                ? Calendar.getInstance().get(Calendar.YEAR) : state.getInt("overview_year", 2026)));
        setOrientation(VERTICAL);
        setBackgroundColor(palette.background);
        content = new FrameLayout(activity);
        addView(content, new LayoutParams(-1, 0, 1));
        content.addView(month, new FrameLayout.LayoutParams(-1, -1));
        yearPage = new LinearLayout(activity);
        yearPage.setOrientation(VERTICAL);
        yearPage.setBackgroundColor(palette.background);
        content.addView(yearPage, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(10), dp(14), dp(12));
        yearPage.addView(header, new LayoutParams(-1, dp(62)));
        TextView previous = button("‹", "Năm trước");
        previous.setBackground(UiKit.rounded(palette.surface, dp(15)));
        previous.setOnClickListener(v -> changeYear(-1));
        header.addView(previous, new LayoutParams(dp(40), dp(40)));
        title = text("", 22);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setContentDescription("Chọn năm");
        title.setOnClickListener(v -> chooseYear());
        header.addView(title, new LayoutParams(0, -1, 1));
        TextView next = button("›", "Năm sau");
        next.setBackground(UiKit.rounded(palette.surface, dp(15)));
        next.setOnClickListener(v -> changeYear(1));
        header.addView(next, new LayoutParams(dp(40), dp(40)));
        scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        yearPage.addView(scroll, new LayoutParams(-1, 0, 1));
        months = new LinearLayout(activity);
        months.setOrientation(VERTICAL);
        months.setPadding(dp(12), dp(12), dp(12), dp(24));
        scroll.addView(months, new ScrollView.LayoutParams(-1, -2));
        LinearLayout navigation = new LinearLayout(activity);
        navigation.setPadding(dp(10), dp(4), dp(10), dp(12));
        navigation.setGravity(Gravity.CENTER_VERTICAL);
        NavButton picker = new NavButton("Chọn ngày", 0);
        picker.setOnClickListener(v -> { showYear(false); month.openDatePicker(); });
        yearTab = new NavButton("Năm", 1);
        yearTab.setOnClickListener(v -> {
            if (!showingYear) {
                year = Math.max(1900, Math.min(2100, month.getSelectedYear()));
                rebuild(); showYear(true);
            }
        });
        NavButton today = new NavButton("Hôm nay", 2);
        today.setOnClickListener(v -> {
            month.selectToday();
            if (showingYear) {
                year = Calendar.getInstance().get(Calendar.YEAR); rebuild();
                scroll.post(() -> scroll.smoothScrollTo(0,
                        months.getChildAt(Calendar.getInstance().get(Calendar.MONTH) / 3).getTop()));
            }
        });
        monthTab = new NavButton("Tháng", 3);
        monthTab.setOnClickListener(v -> showYear(false));
        NavButton settings = new NavButton("Cài đặt", 4);
        settings.setOnClickListener(v -> activity.startActivity(new Intent(activity, SettingsActivity.class)));
        for (NavButton item : new NavButton[]{picker, yearTab, today, monthTab, settings}) {
            LayoutParams lp = new LayoutParams(0, dp(56), 1);
            lp.setMargins(dp(3), 0, dp(3), 0);
            navigation.addView(item, lp);
        }
        addView(navigation, new LayoutParams(-1, -2));
        rebuild();
        showYear(state != null && state.getBoolean("overview_visible"));
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        setPadding(bars.left, bars.top, bars.right, bars.bottom);
        month.setSystemInsets(0, 0);
        return WindowInsets.CONSUMED;
    }

    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size) {
        TextView t = new TextView(activity);
        t.setText(value); t.setTextSize(size); t.setTextColor(palette.primary);
        t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }
    private TextView button(String value, String description) {
        TextView t = text(value, 30); t.setGravity(Gravity.CENTER);
        t.setContentDescription(description); return t;
    }
    public void showYear(boolean visible) {
        showingYear = visible;
        yearPage.setVisibility(visible ? VISIBLE : GONE);
        month.setVisibility(visible ? GONE : VISIBLE);
        yearTab.setSelected(visible); monthTab.setSelected(!visible);
        yearTab.setActive(visible);
        monthTab.setActive(!visible);
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
        NumberPicker picker = new NumberPicker(activity);
        picker.setMinValue(1900); picker.setMaxValue(2100); picker.setValue(year);
        picker.setTextColor(palette.primary);
        picker.setWrapSelectorWheel(false);
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Chọn năm").setView(picker)
            .setNegativeButton("Hủy", null).setPositiveButton("Xem", (d, w) -> {
                picker.clearFocus(); year = picker.getValue(); rebuild(); scroll.scrollTo(0, 0);
            }).create();
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(
                UiKit.rounded(palette.surface, dp(24)));
    }
    private void rebuild() {
        title.setText("Năm " + year); months.removeAllViews();
        for (int r = 0; r < 4; r++) {
            LinearLayout row = new LinearLayout(activity);
            months.addView(row, new LayoutParams(-1, -2));
            for (int c = 0; c < 3; c++) {
                final int m = r * 3 + c;
                MiniMonth mini = new MiniMonth(m);
                LinearLayout.LayoutParams lp = new LayoutParams(0, dp(196), 1);
                lp.setMargins(dp(3), dp(4), dp(3), dp(8));
                mini.setBackground(UiKit.rounded(palette.surface, dp(16)));
                row.addView(mini, lp);
                mini.setContentDescription("Tháng " + (m + 1) + " năm " + year + ". Chạm để xem chi tiết");
                mini.setOnClickListener(v -> {
                    Calendar date = Calendar.getInstance(); date.clear(); date.set(year, m, 1, 12, 0);
                    month.showDate(date.getTimeInMillis()); showYear(false);
                });
            }
        }
    }
    /** Five equal touch targets; icons are drawn vectors, independent of system fonts. */
    private final class NavButton extends LinearLayout {
        private final TextView label;
        private final NavIcon icon;
        NavButton(String name, int kind) {
            super(activity);
            setOrientation(VERTICAL); setGravity(Gravity.CENTER);
            setClickable(true); setFocusable(true); setContentDescription(name);
            icon = new NavIcon(kind);
            addView(icon, new LayoutParams(dp(23), dp(23)));
            label = text(name, 10);
            label.setGravity(Gravity.CENTER); label.setSingleLine(true);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            LayoutParams textParams = new LayoutParams(-1, dp(18));
            textParams.topMargin = dp(3); addView(label, textParams);
            setActive(false);
        }
        void setActive(boolean active) {
            setSelected(active);
            int color = active ? palette.accent : palette.secondary;
            label.setTextColor(color); icon.color = color; icon.invalidate();
            setBackground(new android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(palette.accentSoft),
                    UiKit.rounded(active ? palette.accentSoft : palette.surface, dp(17)), null));
        }
    }
    private final class NavIcon extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int kind;
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
            paint.setColor(current ? palette.accent : palette.primary);
            canvas.drawText("Tháng " + (monthIndex + 1), dp(8), dp(28), paint);
            paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(Math.min(dp(10), cell * .7f));
            String[] weekdays = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
            paint.setColor(palette.secondary);
            for (int i = 0; i < 7; i++) {
                paint.setColor(i == 6 ? palette.danger : palette.secondary);
                canvas.drawText(weekdays[i], dp(4) + cell * (i + .5f), dp(54), paint);
            }
            Calendar date = Calendar.getInstance(); date.clear(); date.set(year, monthIndex, 1, 12, 0);
            int start = (date.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            int count = date.getActualMaximum(Calendar.DAY_OF_MONTH);
            paint.setTextSize(Math.min(dp(12), cell * .8f));
            for (int day = 1; day <= count; day++) {
                int position = start + day - 1, col = position % 7, row = position / 7;
                float x = dp(4) + cell * (col + .5f), baseline = dp(77 + row * 21);
                boolean isToday = current && day == today.get(Calendar.DAY_OF_MONTH);
                if (isToday) {
                    paint.setColor(palette.accent);
                    canvas.drawRoundRect(new android.graphics.RectF(x - cell * .48f, baseline - dp(14), x + cell * .48f,
                        baseline + dp(4)), dp(5), dp(5), paint);
                }
                paint.setColor(isToday ? Color.WHITE : col == 6 ? palette.danger : palette.primary);
                canvas.drawText(Integer.toString(day), x, baseline, paint);
            }
        }
    }
}
