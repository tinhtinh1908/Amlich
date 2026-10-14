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
        boolean tear = layoutId() == R.layout.calendar_widget_tear;
        boolean vertical = layoutId() == R.layout.calendar_widget_vertical;

        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId());
        views.setTextViewText(
                R.id.widget_weekday,
                WEEKDAYS[today.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY]);
        views.setTextViewText(R.id.widget_day, Integer.toString(day));
        String monthText = "Tháng " + month + (vertical ? "" : " · " + year);
        String lunarText = "Âm " + lunar.day + "/" + lunar.month + (lunar.leap ? "N" : "");
        views.setTextViewText(R.id.widget_month_year, monthText);
        views.setTextViewText(R.id.widget_lunar, lunarText);

        int[] colors = WidgetUi.colors(context);
        int width = WidgetUi.width(manager, widgetId, tear ? 160 : vertical ? 80 : 180);
        int height = WidgetUi.height(manager, widgetId, tear ? 180 : vertical ? 160 : 80);
        if (vertical && width < 76) views.setTextViewText(R.id.widget_month_year, "Th. " + month);
        if (!vertical && !tear && width < 180) views.setTextViewText(R.id.widget_month_year, month + " / " + year);
        boolean photo = BackgroundImageManager.applyToWidget(context, manager, widgetId, views,
                R.id.widget_surface, R.id.widget_root,
                ThemeManager.isDark(context) ? R.drawable.widget_background_dark : R.drawable.widget_background_light,
                width, height, 112);
        if (photo) {
            // White primary text and light accents remain legible over a darkened photograph.
            colors[4] = android.graphics.Color.WHITE;
            colors[5] = 0xffe1e5ec;
            colors[7] = 0xffa9ceff;
            colors[9] = 0xffffadb4;
        } else {
            if (tear) WidgetUi.tearSurface(context, views, width, height, colors);
            else WidgetUi.surface(context, views, R.id.widget_root, R.id.widget_surface, width, height, colors[1]);
        }
        views.setTextColor(R.id.widget_weekday, colors[5]);
        views.setTextColor(R.id.widget_day, today.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ? colors[9] : photo ? colors[4] : colors[7]);
        views.setTextColor(R.id.widget_month_year, colors[5]);
        views.setTextColor(R.id.widget_lunar, colors[4]);
        float fontScale = context.getResources().getConfiguration().fontScale;
        float dateWidth = vertical || tear ? width - 20 : 56;
        float daySize = Math.min(dateWidth * .70f / Math.max(1f, fontScale),
                tear ? (height - 68) * .90f : vertical ? (height - 70) * .5f : height * .52f);
        WidgetUi.size(views, R.id.widget_day, Math.max(18, Math.min(64, daySize)));
        WidgetUi.size(views, R.id.widget_lunar, width >= (vertical ? 100 : 210) ? 14 : 12);
        if (tear) {
            views.setTextViewText(R.id.widget_month_year, "THÁNG " + month + " · " + year);
            views.setTextColor(R.id.widget_month_year, photo ? colors[4] : colors[10]);
            views.setTextViewText(R.id.widget_lunar, lunar.day + "/" + lunar.month + (lunar.leap ? " nhuận" : ""));
            WidgetUi.size(views, R.id.widget_lunar, 18);
            views.setTextColor(R.id.widget_lunar_label, colors[5]);
            views.setViewVisibility(R.id.widget_lunar_label, height < 120 ? android.view.View.GONE : android.view.View.VISIBLE);
            String[] weekdays = {"Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};
            views.setTextViewText(R.id.widget_weekday, weekdays[today.get(Calendar.DAY_OF_WEEK) - 1]);
            views.setTextViewText(R.id.widget_day_cycle, LunarCalendar.dayCanChi(lunar.julianDay) + " · " + LunarCalendar.yearCanChi(lunar.year));
            boolean auspicious = TraditionalCalendar.isAuspiciousDay(lunar);
            views.setTextViewText(R.id.widget_guardian, (auspicious ? "Hoàng đạo\n" : "Hắc đạo\n") + TraditionalCalendar.dayGuardian(lunar));
            views.setTextViewText(R.id.widget_term, "Tiết khí\n" + LunarCalendar.solarTerm(lunar));
            views.setTextColor(R.id.widget_term, colors[4]);
            views.setTextColor(R.id.widget_day_cycle, colors[5]);
            views.setTextColor(R.id.widget_guardian, auspicious ? colors[7] : colors[5]);
        }
        views.setOnClickPendingIntent(R.id.widget_root, WidgetUi.open(context, today));
        int detailColor;
        String detail;
        if (!holiday.isEmpty()) {
            detail = HolidayUtil.getShortLabel(day, month, lunar);
            detailColor = colors[9];
        } else if (!note.isEmpty()) {
            detail = note;
            detailColor = colors[7];
        } else {
            detail = tear ? "Tháng " + LunarCalendar.monthCanChi(lunar.month, lunar.year) : LunarCalendar.yearCanChi(lunar.year);
            detailColor = colors[5];
        }

        views.setTextViewText(R.id.widget_holiday, detail);
        views.setTextColor(R.id.widget_holiday, detailColor);
        if (tear) views.setContentDescription(R.id.widget_root,
                day + "/" + month + "/" + year + ", âm " + lunar.day + "/" + lunar.month
                + ", " + LunarCalendar.dayCanChi(lunar.julianDay) + ", " + TraditionalCalendar.dayGuardian(lunar));
        manager.updateAppWidget(widgetId, views);
    }

    private static boolean isRefreshAction(String action) {
        return Intent.ACTION_CONFIGURATION_CHANGED.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action);
    }
}
