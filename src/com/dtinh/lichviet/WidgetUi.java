package com.dtinh.lichviet;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import java.util.Calendar;

/** Small RemoteViews primitives shared by all widgets; colors follow app preferences. */
final class WidgetUi {
    static int[] colors(Context context) {
        return AppColors.recipe(ThemeManager.getColorScheme(context), ThemeManager.isDark(context));
    }
    static int width(AppWidgetManager manager, int id, int fallback) {
        Bundle options = manager.getAppWidgetOptions(id);
        return Math.max(1, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, fallback));
    }
    static int height(AppWidgetManager manager, int id, int fallback) {
        return Math.max(1, manager.getAppWidgetOptions(id).getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, fallback));
    }
    static void surface(Context context, RemoteViews views, int root, int image,
                        int width, int height, int color) {
        // Bound the bitmap payload; all text remains accessible native TextViews.
        float scale = Math.min(1.5f, 480f / Math.max(width, height));
        int w = Math.max(1, Math.round(width * scale));
        int h = Math.max(1, Math.round(height * scale));
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        MiuixSurfaceDrawable surface = new MiuixSurfaceDrawable(color, 24 * scale);
        surface.setBounds(0, 0, w, h);
        surface.draw(new Canvas(bitmap));
        views.setInt(root, "setBackgroundResource", android.R.color.transparent);
        views.setViewVisibility(image, View.VISIBLE);
        views.setImageViewBitmap(image, bitmap);
    }
    static void tearSurface(Context context, RemoteViews views, int width, int height, int[] colors) {
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
        views.setInt(R.id.widget_root, "setBackgroundResource", android.R.color.transparent);
        views.setViewVisibility(R.id.widget_surface, View.VISIBLE);
        views.setImageViewBitmap(R.id.widget_surface, image);
    }
    static Bitmap circle(int color) {
        Bitmap bitmap = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); paint.setColor(color);
        new Canvas(bitmap).drawCircle(30, 30, 30, paint);
        return bitmap;
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
