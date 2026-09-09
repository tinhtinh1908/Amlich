package com.dtinh.lichviet;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Size;
import android.view.View;
import android.widget.RemoteViews;

import java.io.IOException;

/** Stores and renders the user-selected calendar background. */
final class BackgroundImageManager {
    private static final String PREFS = "calendar_background";
    private static final String KEY_URI = "image_uri";
    private static final String KEY_FROSTED = "frosted_controls";

    private BackgroundImageManager() {}

    static boolean hasBackground(Context context) {
        return !uriValue(context).isEmpty();
    }

    static boolean isFrostedEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_FROSTED, true);
    }

    static void setFrostedEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_FROSTED, enabled).apply();
        ThemeManager.refreshMonthWidget(context);
    }

    static boolean setBackground(Context context, Uri uri, int intentFlags) {
        if (uri == null) return false;
        try (ParcelFileDescriptor descriptor = context.getContentResolver()
                .openFileDescriptor(uri, "r")) {
            if (descriptor == null) return false;
        } catch (IOException | SecurityException ignored) {
            return false;
        }
        int flags = intentFlags & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            context.getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (SecurityException ignored) {
            // Some document providers grant a durable read URI without exposing
            // the persistable flag. The URI is still stored and validated below.
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_URI, uri.toString()).apply();
        ThemeManager.refreshMonthWidget(context);
        return true;
    }

    static void clearBackground(Context context) {
        String value = uriValue(context);
        if (!value.isEmpty()) {
            try {
                context.getContentResolver().releasePersistableUriPermission(
                        Uri.parse(value), Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
            }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(KEY_URI).apply();
        ThemeManager.refreshMonthWidget(context);
    }

    static Bitmap load(Context context, int targetWidth, int targetHeight) {
        String value = uriValue(context);
        if (value.isEmpty() || targetWidth <= 0 || targetHeight <= 0) return null;
        try {
            ImageDecoder.Source source = ImageDecoder.createSource(
                    context.getContentResolver(), Uri.parse(value));
            return ImageDecoder.decodeBitmap(source, (decoder, info, source1) -> {
                Size size = info.getSize();
                float scale = Math.min(1.0f, Math.max(
                        targetWidth / (float) size.getWidth(),
                        targetHeight / (float) size.getHeight()));
                int width = Math.max(1, Math.round(size.getWidth() * scale));
                int height = Math.max(1, Math.round(size.getHeight() * scale));
                decoder.setTargetSize(width, height);
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
        } catch (IOException | RuntimeException | OutOfMemoryError ignored) {
            return null;
        }
    }

    static void drawCenterCrop(Canvas canvas, Bitmap bitmap, RectF destination,
                               Paint paint) {
        if (bitmap == null || bitmap.isRecycled() || destination.isEmpty()) return;
        float sourceRatio = bitmap.getWidth() / (float) bitmap.getHeight();
        float targetRatio = destination.width() / destination.height();
        Rect source = new Rect();
        if (sourceRatio > targetRatio) {
            int width = Math.round(bitmap.getHeight() * targetRatio);
            int left = (bitmap.getWidth() - width) / 2;
            source.set(left, 0, left + width, bitmap.getHeight());
        } else {
            int height = Math.round(bitmap.getWidth() / targetRatio);
            int top = (bitmap.getHeight() - height) / 2;
            source.set(0, top, bitmap.getWidth(), top + height);
        }
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        paint.setFilterBitmap(true);
        canvas.drawBitmap(bitmap, source, destination, paint);
    }

    static boolean applyToWidget(Context context, AppWidgetManager manager,
                                 int widgetId, RemoteViews views, int imageViewId,
                                 int rootViewId, int fallbackBackground) {
        if (!hasBackground(context)) {
            views.setViewVisibility(imageViewId, View.GONE);
            views.setInt(rootViewId, "setBackgroundResource", fallbackBackground);
            return false;
        }
        Bundle options = manager.getAppWidgetOptions(widgetId);
        int widthDp = Math.max(120, options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 320));
        int heightDp = Math.max(80, options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 220));
        int width = Math.min(720, UiKit.dp(context, widthDp));
        int height = Math.min(720, UiKit.dp(context, heightDp));
        Bitmap source = load(context, width, height);
        if (source == null) {
            views.setViewVisibility(imageViewId, View.GONE);
            views.setInt(rootViewId, "setBackgroundResource", fallbackBackground);
            return false;
        }

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        RectF bounds = new RectF(0, 0, width, height);
        float radius = UiKit.dp(context, 24);
        Path clip = new Path();
        clip.addRoundRect(bounds, radius, radius, Path.Direction.CW);
        canvas.clipPath(clip);
        drawCenterCrop(canvas, source, bounds, paint);
        paint.setColor(Color.argb(126, 0, 0, 0));
        canvas.drawRect(bounds, paint);
        source.recycle();

        views.setInt(rootViewId, "setBackgroundResource", android.R.color.transparent);
        views.setViewVisibility(imageViewId, View.VISIBLE);
        views.setImageViewBitmap(imageViewId, result);
        return true;
    }

    private static String uriValue(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);
        String value = preferences.getString(KEY_URI, "");
        return value == null ? "" : value;
    }
}
