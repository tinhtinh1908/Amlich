package com.dtinh.lichviet;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;

/** Single source of truth for app and widget appearance. */
final class ThemeManager {
    static final int AUTO = 0;
    static final int LIGHT = 1;
    static final int DARK = 2;
    static final int DEFAULT_GLASS_OPACITY = 50;

    private static final String PREFS = "appearance";
    private static final String KEY_MODE = "theme_mode";
    private static final String KEY_FONT = "calendar_font";
    private static final String[] SYSTEM_FONTS = {null, "serif", "sans-serif-condensed", "monospace"};
    private static final int[] BUNDLED_FONTS = {
            R.font.breeze, R.font.inter, R.font.be_vietnam, R.font.manrope, R.font.lora, R.font.nunito
    };

    private ThemeManager() {}

    static int getMode(Context context) {
        int mode = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_MODE, AUTO);
        return mode >= AUTO && mode <= DARK ? mode : AUTO;
    }

    static int getColorScheme(Context context) {
        return AppColors.normalize(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("color_scheme", 0));
    }
    static void setColorScheme(Context context, int scheme) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("color_scheme", AppColors.normalize(scheme)).apply();
        refreshWidgets(context);
    }

    static boolean isSoftGlassEnabled(Context context) {
        return Build.VERSION.SDK_INT >= 33 && context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("soft_glass", false);
    }

    static void setSoftGlassEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("soft_glass", enabled).apply();
        refreshWidgets(context);
    }

    static int getSoftGlassOpacity(Context context) {
        int opacity = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt("soft_glass_opacity", DEFAULT_GLASS_OPACITY);
        return Math.max(0, Math.min(100, opacity));
    }

    static void setSoftGlassOpacity(Context context, int opacity) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("soft_glass_opacity", opacity).apply();
    }

    static int getFont(Context context) {
        int index = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_FONT, 0);
        return index >= 0 && index < SYSTEM_FONTS.length + BUNDLED_FONTS.length ? index : 0;
    }

    static void setFont(Context context, int index) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        if (index > 0 && index < SYSTEM_FONTS.length + BUNDLED_FONTS.length) editor.putInt(KEY_FONT, index);
        else editor.remove(KEY_FONT);
        editor.apply();
    }

    static Typeface font(Context context, int index, Typeface defaultFont, int style) {
        if (index <= 0) return defaultFont;
        if (index < SYSTEM_FONTS.length) return Typeface.create(SYSTEM_FONTS[index], style);
        int bundled = index - SYSTEM_FONTS.length;
        return bundled < BUNDLED_FONTS.length
                ? Typeface.create(context.getResources().getFont(BUNDLED_FONTS[bundled]), style)
                : defaultFont;
    }

    static boolean isMinimal(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("minimal_layout", false);
    }

    static void setMinimal(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("minimal_layout", enabled).apply();
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
        refresh(context, TearCalendarWidgetProvider.class);
        refresh(context, HorizontalDateWidgetProvider.class);
        refresh(context, BlurEnabledWidget.class);
        refresh(context, MonthCalendarWidgetProvider.class);
        refresh(context, CompactMonthCalendarWidgetProvider.class);
        refresh(context, WideMonthCalendarWidgetProvider.class);
        refresh(context, WideWeekCalendarWidgetProvider.class);
        refresh(context, WideDayCalendarWidgetProvider.class);
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

    /** Shared widget contrast colors; backgrounds are rendered by each provider. */
    static WidgetStyle monthWidgetStyle(Context context, int resolvedMode) {
        WidgetStyle plain = widgetStyleForMode(context, resolvedMode);
        if (!BackgroundImageManager.hasBackground(context)) return plain;
        return new WidgetStyle(plain.background, Color.WHITE, 0xffdae0ea, 0xff768090,
                0xffff7e87, 0xff639ef7, Color.WHITE);
    }

    static WidgetStyle widgetStyleForMode(Context context, int mode) {
        int[] colors = AppColors.recipe(getColorScheme(context), mode == DARK);
        return new WidgetStyle(mode == DARK ? R.drawable.widget_background_dark : R.drawable.widget_background_light,
                colors[4], colors[5], colors[6], colors[9], colors[7], colors[10]);
    }

    static final class WidgetStyle {
        final int background, primary, secondary, muted, sunday, accent, onAccent;
        WidgetStyle(int background, int primary, int secondary, int muted,
                    int sunday, int accent, int onAccent) {
            this.background = background; this.primary = primary;
            this.secondary = secondary; this.muted = muted;
            this.sunday = sunday; this.accent = accent; this.onAccent = onAccent;
        }
    }
}
