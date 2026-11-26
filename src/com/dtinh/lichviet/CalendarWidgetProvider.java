package com.dtinh.lichviet;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.widget.RemoteViews;

import java.util.Calendar;

public abstract class CalendarWidgetProvider extends AppWidgetProvider {
    private static final int HORIZONTAL_CORNER_DP = 18;
    private static final String[] WEEKDAYS = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};

    protected abstract int layoutId();

    private boolean isGlassPreview() {
        return layoutId() == R.layout.calendar_widget_blur_probe
                || layoutId() == R.layout.calendar_widget_vertical_glass
                || layoutId() == R.layout.calendar_widget_tear;
    }

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
        if (isGlassPreview()) {
            android.util.Log.i("AmlichBlurProbe", "widget=" + widgetId + " provider="
                    + providerClass().getSimpleName() + " optionKeys=" + options.keySet()
                    + " runtimeBlur=" + options.get("widget_support_blur")
                    + " capability=" + options.get("supportBackgroundBlur")
                    + " folderStyle=" + options.get("usingFolderGlassStyle"));
        }
        render(context, manager, widgetId);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        boolean miuiUpdate = isGlassPreview()
                && "miui.appwidget.action.APPWIDGET_UPDATE".equals(intent.getAction());
        if (!miuiUpdate && !isRefreshAction(intent.getAction())) {
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
        boolean vertical = layoutId() == R.layout.calendar_widget_vertical
                || layoutId() == R.layout.calendar_widget_vertical_glass;

        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId());
        boolean horizontal = layoutId() == R.layout.calendar_widget;
        boolean wideDate = horizontal || layoutId() == R.layout.calendar_widget_blur_probe;
        if (horizontal && Build.VERSION.SDK_INT >= 31) {
            views.setViewOutlinePreferredRadius(R.id.widget_root, HORIZONTAL_CORNER_DP,
                    android.util.TypedValue.COMPLEX_UNIT_DIP);
        }
        views.setTextViewText(R.id.widget_day, Integer.toString(day));
        String monthText = "Tháng " + month + (vertical ? "" : " · " + year);
        String lunarText = "Âm " + lunar.day + "/" + lunar.month + (lunar.leap ? "N" : "");
        views.setTextViewText(R.id.widget_month_year, monthText);
        views.setTextViewText(R.id.widget_lunar, lunarText);

        int[] colors = WidgetUi.colors(context);
        int width = WidgetUi.width(manager, widgetId, tear ? 160 : vertical ? 80 : 180);
        int height = WidgetUi.height(manager, widgetId, tear ? 180 : vertical ? 160 : 80);
        boolean nativeLayout = isGlassPreview();
        boolean glass = nativeLayout && providerClass() != BlurControlWidget.class
                && WidgetUi.useNativeGlass(context, manager, widgetId);
        int backgroundId = tear || !nativeLayout ? R.id.widget_root : android.R.id.background;
        int plainBackground = ThemeManager.isDark(context)
                ? R.drawable.widget_background_dark : R.drawable.widget_background_light;
        boolean photo = !glass && BackgroundImageManager.applyToWidget(context, manager,
                widgetId, views, R.id.widget_surface, R.id.widget_content_surface, plainBackground, width, height, 112);
        int outerBackground = nativeLayout ? R.drawable.widget_glass_probe_surface : android.R.color.transparent;
        views.setInt(backgroundId, "setBackgroundResource", outerBackground);
        if (nativeLayout) {
            if (Build.VERSION.SDK_INT >= 31) {
                views.setViewOutlinePreferredRadiusDimen(backgroundId, R.dimen.widget_probe_corner);
            }
            views.setBoolean(backgroundId, "setClipToOutline", true);
        }
        views.setInt(R.id.widget_content_surface, "setBackgroundResource",
                nativeLayout && !glass ? plainBackground : android.R.color.transparent);
        if (glass) {
            views.setViewVisibility(R.id.widget_surface, android.view.View.GONE);
            views.setImageViewResource(R.id.widget_surface, 0);
            colors[4] = WidgetUi.GLASS_TEXT;
            colors[5] = WidgetUi.GLASS_TEXT;
            colors[7] = WidgetUi.GLASS_TEXT;
        } else if (photo) {
            // White primary text and light accents remain legible over a darkened photograph.
            colors[4] = android.graphics.Color.WHITE;
            colors[5] = 0xffe1e5ec;
            colors[7] = 0xffa9ceff;
        } else if (tear) {
            WidgetUi.tearSurface(views, width, height, colors);
        } else {
            WidgetUi.surface(views, R.id.widget_surface, width, height, colors[1],
                    wideDate ? HORIZONTAL_CORNER_DP : 24);
        }
        int weekday = today.get(Calendar.DAY_OF_WEEK);
        boolean sunday = weekday == Calendar.SUNDAY;
        views.setTextViewText(R.id.widget_weekday, tear || wideDate || (vertical && width >= 64)
                ? WidgetUi.weekday(today) : WEEKDAYS[weekday - Calendar.SUNDAY]);
        views.setTextColor(R.id.widget_weekday, sunday ? WidgetUi.CALENDAR_RED : colors[5]);
        views.setTextColor(R.id.widget_day, sunday ? WidgetUi.CALENDAR_RED : photo ? colors[4] : colors[7]);
        views.setTextColor(R.id.widget_month_year, colors[5]);
        views.setTextColor(R.id.widget_lunar, colors[4]);
        float fontScale = context.getResources().getConfiguration().fontScale;
        float dateWidth = vertical || tear ? width - 20 : 64;
        float daySize = Math.min(dateWidth * .70f / Math.max(1f, fontScale),
                tear ? (height - 68) * .90f : vertical ? (height - 65) * .56f : (height - 12) * .56f);
        WidgetUi.size(views, R.id.widget_day, Math.max(18, Math.min(64, daySize)));
        WidgetUi.size(views, R.id.widget_lunar, vertical && width < 75 ? 12 : 14);
        if (tear) {
            views.setTextViewText(R.id.widget_month_year, "THÁNG " + month + " · " + year);
            views.setTextColor(R.id.widget_month_year, photo || glass ? colors[4] : colors[10]);
            views.setTextViewText(R.id.widget_lunar, lunar.day + "/" + lunar.month + (lunar.leap ? " nhuận" : ""));
            WidgetUi.size(views, R.id.widget_lunar, 18);
            views.setTextColor(R.id.widget_lunar_label, colors[5]);
            views.setTextViewText(R.id.widget_day_cycle, LunarCalendar.dayCanChi(lunar.julianDay) + " · " + LunarCalendar.yearCanChi(lunar.year));
            boolean auspicious = TraditionalCalendar.isAuspiciousDay(lunar);
            views.setTextViewText(R.id.widget_guardian, (auspicious ? "Hoàng đạo\n" : "Hắc đạo\n") + TraditionalCalendar.dayGuardian(lunar));
            views.setTextViewText(R.id.widget_term, "Tiết khí\n" + LunarCalendar.solarTerm(lunar));
            views.setTextColor(R.id.widget_term, colors[4]);
            views.setTextColor(R.id.widget_day_cycle, colors[5]);
            views.setTextColor(R.id.widget_guardian, auspicious ? colors[7] : colors[5]);
        }
        views.setOnClickPendingIntent(backgroundId, WidgetUi.open(context, today));
        int detailColor;
        String detail;
        if (!holiday.isEmpty()) {
            detail = HolidayUtil.getShortLabel(day, month, lunar);
            detailColor = WidgetUi.CALENDAR_RED;
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
