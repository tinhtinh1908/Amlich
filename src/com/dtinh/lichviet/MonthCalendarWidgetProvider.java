package com.dtinh.lichviet;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;
import java.util.Calendar;
import java.util.Set;

/** Compact 3x3 calendar. Full navigation is available by opening any date. */
public class MonthCalendarWidgetProvider extends AppWidgetProvider {
    protected boolean isCompact() { return false; }
    protected boolean isWeek() { return false; }
    protected boolean isDay() { return false; }
    protected boolean isWideMonth() { return false; }
    private static final int[] DAY_IDS = {
        R.id.month_day_01,
        R.id.month_day_02,
        R.id.month_day_03,
        R.id.month_day_04,
        R.id.month_day_05,
        R.id.month_day_06,
        R.id.month_day_07,
        R.id.month_day_08,
        R.id.month_day_09,
        R.id.month_day_10,
        R.id.month_day_11,
        R.id.month_day_12,
        R.id.month_day_13,
        R.id.month_day_14,
        R.id.month_day_15,
        R.id.month_day_16,
        R.id.month_day_17,
        R.id.month_day_18,
        R.id.month_day_19,
        R.id.month_day_20,
        R.id.month_day_21,
        R.id.month_day_22,
        R.id.month_day_23,
        R.id.month_day_24,
        R.id.month_day_25,
        R.id.month_day_26,
        R.id.month_day_27,
        R.id.month_day_28,
        R.id.month_day_29,
        R.id.month_day_30,
        R.id.month_day_31,
        R.id.month_day_32,
        R.id.month_day_33,
        R.id.month_day_34,
        R.id.month_day_35,
        R.id.month_day_36,
        R.id.month_day_37,
        R.id.month_day_38,
        R.id.month_day_39,
        R.id.month_day_40,
        R.id.month_day_41,
        R.id.month_day_42
    };

    private static final int[] LUNAR_IDS = {
        R.id.month_lunar_01,
        R.id.month_lunar_02,
        R.id.month_lunar_03,
        R.id.month_lunar_04,
        R.id.month_lunar_05,
        R.id.month_lunar_06,
        R.id.month_lunar_07,
        R.id.month_lunar_08,
        R.id.month_lunar_09,
        R.id.month_lunar_10,
        R.id.month_lunar_11,
        R.id.month_lunar_12,
        R.id.month_lunar_13,
        R.id.month_lunar_14,
        R.id.month_lunar_15,
        R.id.month_lunar_16,
        R.id.month_lunar_17,
        R.id.month_lunar_18,
        R.id.month_lunar_19,
        R.id.month_lunar_20,
        R.id.month_lunar_21,
        R.id.month_lunar_22,
        R.id.month_lunar_23,
        R.id.month_lunar_24,
        R.id.month_lunar_25,
        R.id.month_lunar_26,
        R.id.month_lunar_27,
        R.id.month_lunar_28,
        R.id.month_lunar_29,
        R.id.month_lunar_30,
        R.id.month_lunar_31,
        R.id.month_lunar_32,
        R.id.month_lunar_33,
        R.id.month_lunar_34,
        R.id.month_lunar_35,
        R.id.month_lunar_36,
        R.id.month_lunar_37,
        R.id.month_lunar_38,
        R.id.month_lunar_39,
        R.id.month_lunar_40,
        R.id.month_lunar_41,
        R.id.month_lunar_42
    };

    private static final int[] CELL_IDS = {
        R.id.month_cell_01, R.id.month_cell_02, R.id.month_cell_03, R.id.month_cell_04, R.id.month_cell_05, R.id.month_cell_06, R.id.month_cell_07, R.id.month_cell_08, R.id.month_cell_09, R.id.month_cell_10, R.id.month_cell_11, R.id.month_cell_12, R.id.month_cell_13, R.id.month_cell_14, R.id.month_cell_15, R.id.month_cell_16, R.id.month_cell_17, R.id.month_cell_18, R.id.month_cell_19, R.id.month_cell_20, R.id.month_cell_21, R.id.month_cell_22, R.id.month_cell_23, R.id.month_cell_24, R.id.month_cell_25, R.id.month_cell_26, R.id.month_cell_27, R.id.month_cell_28, R.id.month_cell_29, R.id.month_cell_30, R.id.month_cell_31, R.id.month_cell_32, R.id.month_cell_33, R.id.month_cell_34, R.id.month_cell_35, R.id.month_cell_36, R.id.month_cell_37, R.id.month_cell_38, R.id.month_cell_39, R.id.month_cell_40, R.id.month_cell_41, R.id.month_cell_42
    };

    private static final int[] MARK_IDS = {
        R.id.month_mark_01, R.id.month_mark_02, R.id.month_mark_03, R.id.month_mark_04, R.id.month_mark_05, R.id.month_mark_06, R.id.month_mark_07, R.id.month_mark_08, R.id.month_mark_09, R.id.month_mark_10, R.id.month_mark_11, R.id.month_mark_12, R.id.month_mark_13, R.id.month_mark_14, R.id.month_mark_15, R.id.month_mark_16, R.id.month_mark_17, R.id.month_mark_18, R.id.month_mark_19, R.id.month_mark_20, R.id.month_mark_21, R.id.month_mark_22, R.id.month_mark_23, R.id.month_mark_24, R.id.month_mark_25, R.id.month_mark_26, R.id.month_mark_27, R.id.month_mark_28, R.id.month_mark_29, R.id.month_mark_30, R.id.month_mark_31, R.id.month_mark_32, R.id.month_mark_33, R.id.month_mark_34, R.id.month_mark_35, R.id.month_mark_36, R.id.month_mark_37, R.id.month_mark_38, R.id.month_mark_39, R.id.month_mark_40, R.id.month_mark_41, R.id.month_mark_42
    };

    private static final int[] WEEKDAY_IDS = {
        R.id.month_weekday_1, R.id.month_weekday_2, R.id.month_weekday_3,
        R.id.month_weekday_4, R.id.month_weekday_5, R.id.month_weekday_6,
        R.id.month_weekday_7
    };


    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        LauncherIconController.arm(context);
        for (int id : ids) render(context, manager, id);
    }
    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                                   int id, Bundle options) {
        render(context, manager, id);
    }
    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (isRefreshAction(intent.getAction())) refreshAllWidgets(context);
    }
    private void render(Context context, AppWidgetManager manager, int id) {
        Calendar today = Calendar.getInstance();
        Calendar month = (Calendar) today.clone(); month.set(Calendar.DAY_OF_MONTH, 1);
        RemoteViews views = new RemoteViews(context.getPackageName(),
                isDay() ? R.layout.widget_wide_day
                        : isWeek() ? R.layout.widget_wide_week : R.layout.month_calendar_widget);
        boolean glass = WidgetUi.useNativeGlass(context, manager, id);
        int mode = ThemeManager.resolvedMode(context);
        ThemeManager.WidgetStyle plain = ThemeManager.widgetStyleForMode(context, mode);
        ThemeManager.WidgetStyle colors = glass
                ? new ThemeManager.WidgetStyle(android.R.color.transparent,
                WidgetUi.GLASS_TEXT, WidgetUi.GLASS_TEXT, 0xffcbd5e4,
                WidgetUi.CALENDAR_RED, context.getColor(R.color.widget_today_marker), WidgetUi.GLASS_TEXT)
                : plain;
        boolean horizontalTime = isDay() || isWeek();
        if (glass) {
            views.setViewVisibility(R.id.month_widget_background_image, View.GONE);
            views.setImageViewResource(R.id.month_widget_background_image, 0);
        } else {
            int width = WidgetUi.width(manager, id, horizontalTime ? 320 : 220);
            int height = WidgetUi.height(manager, id, isDay() ? 64 : isWeek() ? 110 : 220);
            boolean photo = BackgroundImageManager.applyToWidget(context, manager, id, views,
                    R.id.month_widget_background_image, R.id.widget_content_surface, plain.background,
                    width, height, 72);
            if (photo) {
                colors = ThemeManager.monthWidgetStyle(context, mode);
            } else if (!horizontalTime) {
                WidgetUi.surface(views, R.id.month_widget_background_image, width, height,
                        WidgetUi.colors(context)[1], 24);
            }
            views.setInt(R.id.widget_content_surface, "setBackgroundResource",
                    photo && horizontalTime ? android.R.color.transparent : colors.background);
        }
        if (glass) views.setInt(R.id.widget_content_surface, "setBackgroundResource", android.R.color.transparent);
        if (Build.VERSION.SDK_INT >= 31) {
            views.setViewOutlinePreferredRadiusDimen(android.R.id.background, R.dimen.widget_probe_corner);
        }
        views.setBoolean(android.R.id.background, "setClipToOutline", true);
        if (isDay() || isWeek() || isWideMonth()) {
            bindSummary(views, today, colors);
        }
        if (isDay()) {
            views.setOnClickPendingIntent(android.R.id.background, WidgetUi.open(context, today));
            manager.updateAppWidget(id, views);
            return;
        }
        if (isWideMonth()) views.setViewVisibility(R.id.wide_month_summary, View.VISIBLE);
        if (isCompact()) {
            int padding = Math.round(10 * context.getResources().getDisplayMetrics().density);
            views.setViewPadding(R.id.month_widget_root, padding, padding, padding, padding);
            if (Build.VERSION.SDK_INT >= 31) {
                views.setViewLayoutHeight(R.id.month_weekday_row, 16, android.util.TypedValue.COMPLEX_UNIT_DIP);
            }
        }
        for (int i = 0; i < WEEKDAY_IDS.length; i++) {
            views.setTextColor(WEEKDAY_IDS[i], i == 6 ? colors.sunday : colors.secondary);
        }
        Calendar date = (Calendar) (isWeek() ? today : month).clone();
        int offset = (date.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        date.add(Calendar.DAY_OF_MONTH, -offset);
        int monthEnd = offset + month.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (!isWeek()) {
            int rows = (monthEnd + 6) / 7;
            views.setViewVisibility(R.id.month_row_5, !isWideMonth() || rows >= 5 ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.month_row_6, rows == 6 ? View.VISIBLE : View.GONE);
        }
        Set<String> notes = NoteRepository.snapshotKeys(context);
        int cellCount = isWeek() ? WEEKDAY_IDS.length : DAY_IDS.length;
        for (int i = 0; i < cellCount; i++) {
            if (isWideMonth()) {
                boolean inMonth = i >= offset && i < monthEnd;
                views.setViewVisibility(CELL_IDS[i], inMonth ? View.VISIBLE : View.INVISIBLE);
                if (!inMonth) {
                    date.add(Calendar.DAY_OF_MONTH, 1);
                    continue;
                }
            }
            String description = bindCell(views, i, date, month, today, notes, colors);
            boolean selected = sameDate(date, today);
            views.setViewVisibility(MARK_IDS[i], selected ? View.VISIBLE : View.INVISIBLE);
            if (selected) {
                // Launcher hosts can reuse a view that still has the previous bitmap.
                views.setImageViewResource(MARK_IDS[i], 0);
                views.setInt(MARK_IDS[i], "setBackgroundResource", R.drawable.widget_month_today_marker);
            }
            views.setOnClickPendingIntent(CELL_IDS[i], WidgetUi.open(context, date));
            views.setContentDescription(CELL_IDS[i], description);
            date.add(Calendar.DAY_OF_MONTH, 1);
        }
        manager.updateAppWidget(id, views);
    }
    private String bindCell(
            RemoteViews views,
            int index,
            Calendar date,
            Calendar displayedMonth,
            Calendar today,
            Set<String> noteKeys,
            ThemeManager.WidgetStyle colors) {
        int day = date.get(Calendar.DAY_OF_MONTH);
        int month = date.get(Calendar.MONTH) + 1;
        int year = date.get(Calendar.YEAR);

        boolean inDisplayedMonth = isWeek() || (date.get(Calendar.MONTH) == displayedMonth.get(Calendar.MONTH)
                && year == displayedMonth.get(Calendar.YEAR));
        boolean isToday = sameDate(date, today);
        boolean isSunday = date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;

        LunarCalendar.LunarDate lunar = LunarCalendar.fromSolar(day, month, year);
        String holiday = HolidayUtil.getHoliday(day, month, lunar);
        boolean hasNote = NoteRepository.has(noteKeys, date);

        views.setTextViewText(DAY_IDS[index], Integer.toString(day));
        boolean lunarInsideMarker = isToday && !isWeek();
        String lunarLabel = isCompact() || isWeek() || lunarInsideMarker
                ? (lunar.day == 1 ? "1/" + lunar.month : Integer.toString(lunar.day))
                : HolidayUtil.getShortLabel(day, month, lunar);
        views.setTextViewText(LUNAR_IDS[index], lunarLabel + (hasNote ? " •" : ""));

        int dayColor = isToday
                ? WidgetUi.GLASS_TEXT
                : !inDisplayedMonth
                ? colors.muted
                : isSunday ? colors.sunday : colors.primary;

        int lunarColor = lunarInsideMarker
                ? WidgetUi.GLASS_TEXT
                : !inDisplayedMonth
                ? colors.muted
                : isSunday || !holiday.isEmpty()
                ? colors.sunday
                : hasNote ? colors.accent : colors.secondary;

        views.setTextColor(DAY_IDS[index], dayColor);
        views.setTextColor(LUNAR_IDS[index], lunarColor);
        views.setInt(
                DAY_IDS[index],
                "setBackgroundResource",
                0);
        return day + "/" + month + "/" + year + ", âm " + lunar.day + "/" + lunar.month
                + (holiday.isEmpty() ? "" : ", " + holiday) + (hasNote ? ", có ghi chú" : "");
    }

    private void bindSummary(RemoteViews views, Calendar today, ThemeManager.WidgetStyle colors) {
        int day = today.get(Calendar.DAY_OF_MONTH);
        int month = today.get(Calendar.MONTH) + 1;
        LunarCalendar.LunarDate lunar = LunarCalendar.fromSolar(day, month, today.get(Calendar.YEAR));
        views.setTextViewText(R.id.wide_calendar_lunar, "Âm lịch " + lunar.day + " tháng " + lunar.month
                + (lunar.leap ? " nhuận" : ""));
        if (isWideMonth()) {
            views.setTextViewText(R.id.wide_calendar_caption, "THÁNG " + month + " · " + today.get(Calendar.YEAR));
            views.setTextColor(R.id.wide_calendar_caption, colors.secondary);
            views.setTextViewText(R.id.wide_calendar_day, Integer.toString(day));
            views.setTextViewText(R.id.wide_month_detail, LunarCalendar.dayCanChi(lunar.julianDay));
            views.setTextColor(R.id.wide_month_detail, colors.secondary);
        }
        String weekday = WidgetUi.weekday(today);
        String solar = day + " tháng " + month;
        boolean sunday = today.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
        if (isDay()) {
            views.setTextViewText(R.id.wide_calendar_caption, weekday);
            views.setTextColor(R.id.wide_calendar_caption, sunday ? colors.sunday : colors.secondary);
        }
        views.setTextViewText(R.id.wide_calendar_weekday, isDay() ? solar + ", " + today.get(Calendar.YEAR)
                : isWeek() ? weekday + ", " + solar : weekday);
        views.setTextColor(R.id.wide_calendar_day, colors.primary);
        views.setTextColor(R.id.wide_calendar_weekday, sunday ? colors.sunday : colors.primary);
        views.setTextColor(R.id.wide_calendar_lunar, colors.secondary);
    }

    private static boolean isRefreshAction(String action) {
        return "miui.appwidget.action.APPWIDGET_UPDATE".equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_CONFIGURATION_CHANGED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action);
    }

    private static boolean sameDate(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    private void refreshAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] widgetIds = manager.getAppWidgetIds(
                new ComponentName(context, getClass()));
        for (int widgetId : widgetIds) {
            render(context, manager, widgetId);
        }
    }

}
