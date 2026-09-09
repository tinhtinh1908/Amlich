package com.dtinh.lichviet;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import java.util.Calendar;

public abstract class CalendarWidgetProvider extends AppWidgetProvider {
    private static final String[] WEEKDAYS = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};

    protected abstract int layoutId();

    protected abstract Class<? extends CalendarWidgetProvider> providerClass();

    @Override
    public final void onUpdate(
            Context context, AppWidgetManager manager, int[] widgetIds) {
        LauncherIconController.arm(context);
        for (int widgetId : widgetIds) {
            render(context, manager, widgetId);
        }
    }

    @Override
    public final void onAppWidgetOptionsChanged(
            Context context,
            AppWidgetManager manager,
            int widgetId,
            Bundle options) {
        render(context, manager, widgetId);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (!isRefreshAction(intent.getAction())) {
            return;
        }

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] widgetIds = manager.getAppWidgetIds(
                new ComponentName(context, providerClass()));
        onUpdate(context, manager, widgetIds);
    }

    private void render(Context context, AppWidgetManager manager, int widgetId) {
        Calendar today = Calendar.getInstance();
        int day = today.get(Calendar.DAY_OF_MONTH);
        int month = today.get(Calendar.MONTH) + 1;
        int year = today.get(Calendar.YEAR);

        LunarCalendar.LunarDate lunar = LunarCalendar.fromSolar(day, month, year);
        String holiday = HolidayUtil.getHoliday(day, month, lunar);
        String note = NoteRepository.get(context, today);
        boolean vertical = layoutId() == R.layout.calendar_widget_vertical;

        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId());
        views.setTextViewText(
                R.id.widget_weekday,
                WEEKDAYS[today.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY]);
        views.setTextViewText(R.id.widget_day, Integer.toString(day));
        String monthText = vertical ? "THÁNG " + month : "THÁNG " + month + ", " + year;
        String lunarText = "Âm " + lunar.day + "/" + lunar.month + (lunar.leap ? "N" : "");
        views.setTextViewText(R.id.widget_month_year, monthText);
        views.setTextViewText(R.id.widget_lunar, lunarText);

        int detailColor;
        String detail;
        if (!holiday.isEmpty()) {
            detail = HolidayUtil.getShortLabel(day, month, lunar);
            detailColor = context.getColor(R.color.widget_sunday);
        } else if (!note.isEmpty()) {
            detail = note;
            detailColor = context.getColor(R.color.widget_blue);
        } else {
            detail = LunarCalendar.yearCanChi(lunar.year);
            detailColor = context.getColor(R.color.widget_secondary);
        }

        views.setTextViewText(R.id.widget_holiday, detail);
        views.setTextColor(R.id.widget_holiday, detailColor);
        manager.updateAppWidget(widgetId, views);
    }

    private static boolean isRefreshAction(String action) {
        return Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action);
    }
}
