package com.dtinh.lichviet;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.RemoteViews;
import java.util.Calendar;

/** Small RemoteViews primitives shared by all widgets; colors follow app preferences. */
final class WidgetUi {
    static final int GLASS_TEXT = 0xffffffff;
    static final int CALENDAR_RED = 0xfff44336;
    private static final String[] WEEKDAYS = {"Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};

    static String weekday(Calendar date) {
        return WEEKDAYS[date.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY];
    }

    static int[] colors(Context context) {
        return AppColors.recipe(ThemeManager.getColorScheme(context), ThemeManager.isDark(context));
    }
    static boolean useNativeGlass(Context context, AppWidgetManager manager, int id) {
        if (Build.VERSION.SDK_INT < 31 || !ThemeManager.isSoftGlassEnabled(context)) return false;
        Bundle options = manager.getAppWidgetOptions(id);
        if (options.containsKey("widget_support_blur")) return options.getBoolean("widget_support_blur");

        // HyperOS releases that omit the widget option still report framework blur availability.
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo launcher = context.getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        WindowManager windows = context.getSystemService(WindowManager.class);
        return launcher != null && launcher.activityInfo != null
                && "com.miui.home".equals(launcher.activityInfo.packageName)
                && windows != null && windows.isCrossWindowBlurEnabled();
    }
    static int width(AppWidgetManager manager, int id, int fallback) {
        Bundle options = manager.getAppWidgetOptions(id);
        return Math.max(1, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, fallback));
    }
    static int height(AppWidgetManager manager, int id, int fallback) {
        return Math.max(1, manager.getAppWidgetOptions(id).getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, fallback));
    }
    static void surface(RemoteViews views, int image,
                        int width, int height, int color, int radiusDp) {
        // Bound the bitmap payload; all text remains accessible native TextViews.
        float scale = Math.min(1.5f, 480f / Math.max(width, height));
        int w = Math.max(1, Math.round(width * scale));
        int h = Math.max(1, Math.round(height * scale));
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        MiuixSurfaceDrawable surface = new MiuixSurfaceDrawable(color, radiusDp * scale);
        surface.setBounds(0, 0, w, h);
        surface.draw(new Canvas(bitmap));
        views.setViewVisibility(image, View.VISIBLE);
        views.setImageViewBitmap(image, bitmap);
    }
    static void tearSurface(RemoteViews views, int width, int height, int[] colors) {
        float scale = Math.min(2f, 480f / Math.max(width, height));
        int w = Math.max(1, Math.round(width * scale)), h = Math.max(1, Math.round(height * scale));
        Bitmap image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        android.graphics.Path outline = new android.graphics.Path();
        MiuixSurfaceDrawable.buildPath(outline, 0, 0, w, h, 12 * scale);
        canvas.clipPath(outline);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // Neutral paper, colored binding strip, and two punched binding holes.
        paint.setColor(colors[1]); canvas.drawRect(0, 0, w, h, paint);
        paint.setColor(colors[7]); canvas.drawRect(0, 0, w, 28 * scale, paint);
        paint.setColor(colors[10]);
        canvas.drawCircle(w * .23f, 5 * scale, 2 * scale, paint);
        canvas.drawCircle(w * .77f, 5 * scale, 2 * scale, paint);
        paint.setColor(colors[2]);
        android.graphics.Path fold = new android.graphics.Path();
        fold.moveTo(w - 14 * scale, h); fold.lineTo(w - 14 * scale, h - 14 * scale);
        fold.lineTo(w, h - 14 * scale); fold.close(); canvas.drawPath(fold, paint);
        views.setViewVisibility(R.id.widget_surface, View.VISIBLE);
        views.setImageViewBitmap(R.id.widget_surface, image);
    }
    static void size(RemoteViews views, int id, float sp) {
        views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, sp);
    }
    static PendingIntent open(Context context, Calendar date) {
        long millis = date.getTimeInMillis();
        Intent intent = new Intent(context, MainActivity.class)
                .setData(Uri.parse("lichviet://time/" + millis)).putExtra("beginTime", millis)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private WidgetUi() {}
}
