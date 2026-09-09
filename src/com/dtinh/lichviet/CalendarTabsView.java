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
    private final TextView title, yearTab, monthTab;
    private int year;
    private boolean showingYear;

    public CalendarTabsView(Activity activity, CalendarMonthView month, Bundle state) {
        super(activity);
        this.activity = activity;
        this.month = month;
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
        yearPage.setBackgroundColor(palette.night ? Color.BLACK : palette.background);
        content.addView(yearPage, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), 0, dp(8), 0);
        yearPage.addView(header, new LayoutParams(-1, dp(72)));
        title = text("", 30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setContentDescription("Chọn năm");
        title.setOnClickListener(v -> chooseYear());
        header.addView(title, new LayoutParams(0, -1, 1));
        TextView previous = button("‹", "Năm trước");
        previous.setOnClickListener(v -> changeYear(-1));
        header.addView(previous, new LayoutParams(dp(48), -1));
        TextView next = button("›", "Năm sau");
        next.setOnClickListener(v -> changeYear(1));
        header.addView(next, new LayoutParams(dp(48), -1));
        TextView settings = button("⋮", "Cài đặt");
        settings.setOnClickListener(v -> activity.startActivity(new Intent(activity, SettingsActivity.class)));
        header.addView(settings, new LayoutParams(dp(48), -1));
        scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        yearPage.addView(scroll, new LayoutParams(-1, 0, 1));
        months = new LinearLayout(activity);
        months.setOrientation(VERTICAL);
        months.setPadding(dp(12), dp(12), dp(12), dp(24));
        scroll.addView(months, new ScrollView.LayoutParams(-1, -2));
        LinearLayout navigation = new LinearLayout(activity);
        navigation.setPadding(dp(16), dp(4), dp(16), dp(4));
        yearTab = text("▦\nNăm", 15);
        monthTab = text("▤\nTháng", 15);
        yearTab.setGravity(Gravity.CENTER);
        monthTab.setGravity(Gravity.CENTER);
        yearTab.setContentDescription("Xem lịch năm");
        monthTab.setContentDescription("Xem lịch tháng");
        yearTab.setOnClickListener(v -> {
            if (!showingYear) { year = Math.max(1900, Math.min(2100, month.getSelectedYear())); rebuild(); showYear(true); }
        });
        monthTab.setOnClickListener(v -> showYear(false));
        navigation.addView(yearTab, new LayoutParams(0, dp(60), 1));
        navigation.addView(monthTab, new LayoutParams(0, dp(60), 1));
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
        yearTab.setTextColor(visible ? palette.accent : palette.secondary);
        monthTab.setTextColor(visible ? palette.secondary : palette.accent);
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
        picker.setWrapSelectorWheel(false);
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        new AlertDialog.Builder(activity).setTitle("Chọn năm").setView(picker)
            .setNegativeButton("Hủy", null).setPositiveButton("Xem", (d, w) -> {
                picker.clearFocus(); year = picker.getValue(); rebuild(); scroll.scrollTo(0, 0);
            }).show();
    }
    private void rebuild() {
        title.setText(Integer.toString(year)); months.removeAllViews();
        for (int r = 0; r < 4; r++) {
            LinearLayout row = new LinearLayout(activity);
            months.addView(row, new LayoutParams(-1, -2));
            for (int c = 0; c < 3; c++) {
                final int m = r * 3 + c;
                MiniMonth mini = new MiniMonth(m);
                LinearLayout.LayoutParams lp = new LayoutParams(0, dp(196), 1);
                lp.setMargins(dp(3), dp(4), dp(3), dp(12));
                row.addView(mini, lp);
                mini.setContentDescription("Tháng " + (m + 1) + " năm " + year + ". Chạm để xem chi tiết");
                mini.setOnClickListener(v -> {
                    Calendar date = Calendar.getInstance(); date.clear(); date.set(year, m, 1, 12, 0);
                    month.showDate(date.getTimeInMillis()); showYear(false);
                });
            }
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
            float cell = getWidth() / 7f;
            paint.setTextAlign(Paint.Align.LEFT); paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            paint.setTextSize(Math.min(dp(21), getWidth() / 4.2f));
            paint.setColor(current ? palette.accent : palette.primary);
            canvas.drawText("Thg " + (monthIndex + 1), dp(2), dp(29), paint);
            paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(Math.min(dp(10), cell * .7f));
            String[] weekdays = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
            paint.setColor(palette.secondary);
            for (int i = 0; i < 7; i++) canvas.drawText(weekdays[i], cell * (i + .5f), dp(54), paint);
            Calendar date = Calendar.getInstance(); date.clear(); date.set(year, monthIndex, 1, 12, 0);
            int start = (date.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            int count = date.getActualMaximum(Calendar.DAY_OF_MONTH);
            paint.setTextSize(Math.min(dp(12), cell * .8f));
            for (int day = 1; day <= count; day++) {
                int position = start + day - 1, col = position % 7, row = position / 7;
                float x = cell * (col + .5f), baseline = dp(77 + row * 21);
                boolean isToday = current && day == today.get(Calendar.DAY_OF_MONTH);
                if (isToday) {
                    paint.setColor(palette.accent);
                    canvas.drawRoundRect(new android.graphics.RectF(x - cell * .48f, baseline - dp(14), x + cell * .48f,
                        baseline + dp(4)), dp(5), dp(5), paint);
                }
                paint.setColor(isToday ? Color.WHITE : col >= 5 ? palette.accent : palette.primary);
                canvas.drawText(Integer.toString(day), x, baseline, paint);
            }
        }
    }
}
