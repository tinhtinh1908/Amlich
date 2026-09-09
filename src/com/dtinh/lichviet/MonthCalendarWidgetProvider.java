package com.dtinh.lichviet;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.Set;

public final class MonthCalendarWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_PREVIOUS = "com.dtinh.lichviet.widget.MONTH_PREVIOUS";
    private static final String ACTION_TODAY = "com.dtinh.lichviet.widget.MONTH_TODAY";
    private static final String ACTION_NEXT = "com.dtinh.lichviet.widget.MONTH_NEXT";
    private static final String EXTRA_WIDGET_ID = "appWidgetId";
    private static final String PREFERENCES = "month_widget";
    private static final String OFFSET_PREFIX = "offset_";
    private static final int MONTH_LIMIT = 1_200;

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

    private static final int[] WEEKDAY_IDS = {
        R.id.month_weekday_1, R.id.month_weekday_2, R.id.month_weekday_3,
        R.id.month_weekday_4, R.id.month_weekday_5, R.id.month_weekday_6,
        R.id.month_weekday_7
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] widgetIds) {
        LauncherIconController.arm(context);
        for (int widgetId : widgetIds) {
            render(context, manager, widgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        LauncherIconController.arm(context);

        String action = intent.getAction();
        if (isNavigationAction(action)) {
            handleNavigation(context, intent, action);
            return;
        }

        if (isRefreshAction(action)) {
            refreshAllWidgets(context);
        }
    }

    @Override
    public void onDeleted(Context context, int[] widgetIds) {
        SharedPreferences.Editor editor = preferences(context).edit();
        for (int widgetId : widgetIds) {
            editor.remove(offsetKey(widgetId));
        }
        editor.apply();
        super.onDeleted(context, widgetIds);
    }

    private static void handleNavigation(Context context, Intent intent, String action) {
        int widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return;
        }

        int offset = preferences(context).getInt(offsetKey(widgetId), 0);
        if (ACTION_PREVIOUS.equals(action)) {
            offset--;
        } else if (ACTION_NEXT.equals(action)) {
            offset++;
        } else {
            offset = 0;
        }

        offset = Math.max(-MONTH_LIMIT, Math.min(MONTH_LIMIT, offset));
        preferences(context).edit().putInt(offsetKey(widgetId), offset).apply();
        render(context, AppWidgetManager.getInstance(context), widgetId);
    }

    private static void render(Context context, AppWidgetManager manager, int widgetId) {
        Calendar displayedMonth = Calendar.getInstance();
        displayedMonth.set(Calendar.DAY_OF_MONTH, 1);
        displayedMonth.add(Calendar.MONTH, preferences(context).getInt(offsetKey(widgetId), 0));

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.month_calendar_widget);
        int selectedMode = ThemeManager.getMode(context);
        int resolvedMode = ThemeManager.resolvedMode(context);
        ThemeManager.WidgetStyle colors = ThemeManager.monthWidgetStyle(
                context, selectedMode, resolvedMode);
        BackgroundImageManager.applyToWidget(
                context, manager, widgetId, views,
                R.id.month_widget_background_image, R.id.month_widget_frame,
                colors.background);
        views.setTextColor(R.id.month_widget_title, colors.primary);
        views.setInt(R.id.month_widget_prev, "setBackgroundResource", colors.headerButton);
        views.setInt(R.id.month_widget_today, "setBackgroundResource", colors.headerButton);
        views.setInt(R.id.month_widget_next, "setBackgroundResource", colors.headerButton);
        views.setTextColor(R.id.month_widget_prev, colors.headerSecondary);
        views.setTextColor(R.id.month_widget_today, colors.headerAccent);
        views.setTextColor(R.id.month_widget_next, colors.headerSecondary);
        views.setInt(R.id.month_widget_divider, "setBackgroundColor", colors.muted);
        for (int index = 0; index < WEEKDAY_IDS.length; index++) {
            views.setTextColor(WEEKDAY_IDS[index],
                    index == WEEKDAY_IDS.length - 1 ? colors.sunday : colors.secondary);
        }
        views.setTextViewText(
                R.id.month_widget_title,
                "Tháng " + (displayedMonth.get(Calendar.MONTH) + 1)
                        + ", " + displayedMonth.get(Calendar.YEAR));

        Calendar cellDate = (Calendar) displayedMonth.clone();
        int mondayOffset = (cellDate.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        cellDate.add(Calendar.DAY_OF_MONTH, -mondayOffset);

        Calendar today = Calendar.getInstance();
        Set<String> noteKeys = NoteRepository.snapshotKeys(context);
        for (int index = 0; index < DAY_IDS.length; index++) {
            bindCell(views, index, cellDate, displayedMonth, today, noteKeys, colors);
            cellDate.add(Calendar.DAY_OF_MONTH, 1);
        }

        views.setOnClickPendingIntent(
                R.id.month_widget_prev,
                navigationIntent(context, widgetId, ACTION_PREVIOUS, 1));
        views.setOnClickPendingIntent(
                R.id.month_widget_today,
                navigationIntent(context, widgetId, ACTION_TODAY, 2));
        views.setOnClickPendingIntent(
                R.id.month_widget_next,
                navigationIntent(context, widgetId, ACTION_NEXT, 3));

        manager.updateAppWidget(widgetId, views);
    }

    private static void bindCell(
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

        boolean inDisplayedMonth =
                date.get(Calendar.MONTH) == displayedMonth.get(Calendar.MONTH)
                        && year == displayedMonth.get(Calendar.YEAR);
        boolean isToday = sameDate(date, today);
        boolean isSunday = date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;

        LunarCalendar.LunarDate lunar = LunarCalendar.fromSolar(day, month, year);
        String holiday = HolidayUtil.getHoliday(day, month, lunar);
        boolean hasNote = NoteRepository.has(noteKeys, date);

        views.setTextViewText(DAY_IDS[index], Integer.toString(day));
        views.setTextViewText(
                LUNAR_IDS[index],
                HolidayUtil.getShortLabel(day, month, lunar) + (hasNote ? " •" : ""));

        int dayColor = isToday
                ? colors.onAccent
                : !inDisplayedMonth
                ? colors.muted
                : isSunday ? colors.sunday : colors.primary;

        int lunarColor = !inDisplayedMonth
                ? colors.muted
                : !holiday.isEmpty()
                ? colors.sunday
                : hasNote ? colors.accent : colors.secondary;

        views.setTextColor(DAY_IDS[index], dayColor);
        views.setTextColor(LUNAR_IDS[index], lunarColor);
        views.setInt(
                DAY_IDS[index],
                "setBackgroundResource",
                isToday ? colors.todayCircle : 0);
    }

    private static PendingIntent navigationIntent(
            Context context, int widgetId, String action, int actionId) {
        Intent intent = new Intent(context, MonthCalendarWidgetProvider.class)
                .setAction(action)
                .putExtra(EXTRA_WIDGET_ID, widgetId)
                .setData(Uri.parse("lichviet://month-widget/" + widgetId + "/" + actionId));

        int requestCode = widgetId * 10 + actionId;
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static boolean isNavigationAction(String action) {
        return ACTION_PREVIOUS.equals(action)
                || ACTION_TODAY.equals(action)
                || ACTION_NEXT.equals(action);
    }

    private static boolean isRefreshAction(String action) {
        return Intent.ACTION_DATE_CHANGED.equals(action)
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

    private static void refreshAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] widgetIds = manager.getAppWidgetIds(
                new ComponentName(context, MonthCalendarWidgetProvider.class));
        for (int widgetId : widgetIds) {
            render(context, manager, widgetId);
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    private static String offsetKey(int widgetId) {
        return OFFSET_PREFIX + widgetId;
    }

}
