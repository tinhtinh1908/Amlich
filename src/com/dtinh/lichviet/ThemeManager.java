package com.dtinh.lichviet;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;

/** Single source of truth for app and widget appearance. */
final class ThemeManager {
    static final int AUTO = 0;
    static final int LIGHT = 1;
    static final int DARK = 2;

    private static final String PREFS = "appearance";
    private static final String KEY_MODE = "theme_mode";

    private ThemeManager() {}

    static int getMode(Context context) {
        int mode = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_MODE, AUTO);
        return mode >= AUTO && mode <= DARK ? mode : AUTO;
    }

    static int resolvedMode(Context context) {
        int mode = getMode(context);
        if (mode != AUTO) return mode;
        int night = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return night == Configuration.UI_MODE_NIGHT_YES ? DARK : LIGHT;
    }

    static boolean isDark(Context context) {
        int mode = getMode(context);
        if (mode == AUTO) return isSystemDark(context);
        return mode == DARK;
    }

    private static boolean isSystemDark(Context context) {
        int night = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return night == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Wraps an Activity base context before resources are first accessed.
     * HyperOS rejects applyOverrideConfiguration() from onCreate() because the
     * framework may have already initialized the Activity resources.
     */
    static Context wrapActivityContext(Context base) {
        int mode = getMode(base);
        if (mode == AUTO) return base;

        Configuration configuration = new Configuration(
                base.getResources().getConfiguration());
        int night = mode == LIGHT
                ? Configuration.UI_MODE_NIGHT_NO : Configuration.UI_MODE_NIGHT_YES;
        configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | night;
        return base.createConfigurationContext(configuration);
    }

    static void applyActivityTheme(Activity activity) {
        activity.setTheme(R.style.AppTheme);
    }

    static void setMode(Context context, int mode) {
        if (mode < AUTO || mode > DARK) mode = AUTO;
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (preferences.getInt(KEY_MODE, AUTO) == mode) return;
        preferences.edit().putInt(KEY_MODE, mode).apply();
        refreshWidgets(context);
    }

    static void refreshWidgets(Context context) {
        refresh(context, VerticalDateWidgetProvider.class);
        refresh(context, HorizontalDateWidgetProvider.class);
        refreshMonthWidget(context);
    }

    static void refreshMonthWidget(Context context) {
        refresh(context, MonthCalendarWidgetProvider.class);
    }

    private static void refresh(Context context, Class<?> provider) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, provider));
        if (ids.length == 0) return;
        Intent update = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .setComponent(new ComponentName(context, provider))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(update);
    }

    /** Photo colors are intentionally limited to the 4x4 month widget. */
    static WidgetStyle monthWidgetStyle(
            Context context, int selectedMode, int resolvedMode) {
        if (BackgroundImageManager.hasBackground(context)) {
            boolean dark = resolvedMode == DARK;
            return new WidgetStyle(
                    dark ? R.drawable.widget_background_dark
                            : R.drawable.widget_background_light,
                    BackgroundImageManager.isFrostedEnabled(context)
                            ? (dark ? R.drawable.widget_header_button_photo
                                    : R.drawable.widget_header_button_photo_light)
                            : (dark ? R.drawable.widget_header_button_dark
                                    : R.drawable.widget_header_button_light),
                    dark ? R.drawable.widget_today_circle_dark
                            : R.drawable.widget_today_circle_light,
                    Color.WHITE, Color.rgb(218, 224, 234),
                    Color.rgb(118, 128, 144), Color.rgb(255, 126, 135),
                    Color.rgb(99, 158, 247), Color.WHITE,
                    dark ? Color.rgb(218, 224, 234) : Color.rgb(82, 88, 101),
                    dark ? Color.rgb(99, 158, 247) : Color.rgb(66, 133, 244));
        }
        WidgetStyle resolved = widgetStyleForMode(resolvedMode);
        if (selectedMode != AUTO) return resolved;
        return new WidgetStyle(
                R.drawable.widget_background,
                R.drawable.widget_header_button,
                R.drawable.widget_today_circle,
                resolved.primary, resolved.secondary, resolved.muted,
                resolved.sunday, resolved.accent, resolved.onAccent,
                resolved.headerSecondary, resolved.headerAccent);
    }

    private static WidgetStyle widgetStyleForMode(int mode) {
        if (mode == DARK) {
            return new WidgetStyle(
                    R.drawable.widget_background_dark,
                    R.drawable.widget_header_button_dark,
                    R.drawable.widget_today_circle_dark,
                    Color.rgb(245, 247, 250), Color.rgb(168, 172, 182),
                    Color.rgb(86, 90, 100), Color.rgb(244, 116, 116),
                    Color.rgb(91, 150, 247), Color.WHITE,
                    Color.rgb(168, 172, 182), Color.rgb(91, 150, 247));
        }
        return new WidgetStyle(
                R.drawable.widget_background_light,
                R.drawable.widget_header_button_light,
                R.drawable.widget_today_circle_light,
                Color.rgb(23, 26, 34), Color.rgb(111, 116, 128),
                Color.rgb(194, 198, 207), Color.rgb(221, 75, 75),
                Color.rgb(66, 133, 244), Color.WHITE,
                Color.rgb(111, 116, 128), Color.rgb(66, 133, 244));
    }

    static final class WidgetStyle {
        final int background;
        final int headerButton;
        final int todayCircle;
        final int primary;
        final int secondary;
        final int muted;
        final int sunday;
        final int accent;
        final int onAccent;
        final int headerSecondary;
        final int headerAccent;

        WidgetStyle(int background, int headerButton, int todayCircle,
                    int primary, int secondary, int muted, int sunday,
                    int accent, int onAccent, int headerSecondary,
                    int headerAccent) {
            this.background = background;
            this.headerButton = headerButton;
            this.todayCircle = todayCircle;
            this.primary = primary;
            this.secondary = secondary;
            this.muted = muted;
            this.sunday = sunday;
            this.accent = accent;
            this.onAccent = onAccent;
            this.headerSecondary = headerSecondary;
            this.headerAccent = headerAccent;
        }
    }
}
