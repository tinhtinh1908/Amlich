package com.dtinh.lichviet;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Native continuous corners adapted from Miuix SquirclePath.kt.
 * Copyright 2026, compose-miuix-ui contributors. SPDX-License-Identifier: Apache-2.0.
 */
final class MiuixSurfaceDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final int fillColor, strokeColor;
    private final float radius, strokeWidth;
    private int opacity = 255;

    MiuixSurfaceDrawable(int color, float radius) { this(color, radius, 0, 0); }
    MiuixSurfaceDrawable(int color, float radius, int strokeColor, float strokeWidth) {
        this.fillColor = color;
        this.radius = Math.max(0, radius);
        this.strokeColor = strokeColor;
        this.strokeWidth = Math.max(0, strokeWidth);
    }
    @Override protected void onBoundsChange(Rect bounds) {
        float inset = strokeWidth / 2;
        buildPath(path, bounds.left + inset, bounds.top + inset,
                bounds.right - inset, bounds.bottom - inset, radius);
    }
    static void buildPath(Path path, float left, float top, float right, float bottom, float radius) {
        path.rewind();
        if (right <= left || bottom <= top) return;
        float tile = MiuixMetrics.cornerTile(right - left, bottom - top, radius);
        if (tile == 0) { path.addRect(left, top, right, bottom, Path.Direction.CW); return; }
        float handle = MiuixMetrics.cornerHandle(tile);
        path.moveTo(left + tile, top);
        path.lineTo(right - tile, top);
        path.cubicTo(right - handle, top, right, top + handle, right, top + tile);
        path.lineTo(right, bottom - tile);
        path.cubicTo(right, bottom - handle, right - handle, bottom, right - tile, bottom);
        path.lineTo(left + tile, bottom);
        path.cubicTo(left + handle, bottom, left, bottom - handle, left, bottom - tile);
        path.lineTo(left, top + tile);
        path.cubicTo(left, top + handle, left + handle, top, left + tile, top);
        path.close();
    }
    static void draw(Canvas canvas, Path path, RectF bounds, float radius, Paint paint) {
        buildPath(path, bounds.left, bounds.top, bounds.right, bounds.bottom, radius);
        canvas.drawPath(path, paint);
    }
    @Override public void draw(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fillColor);
        paint.setAlpha(Color.alpha(fillColor) * opacity / 255);
        canvas.drawPath(path, paint);
        if (strokeWidth > 0) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            paint.setColor(strokeColor);
            paint.setAlpha(Color.alpha(strokeColor) * opacity / 255);
            canvas.drawPath(path, paint);
        }
    }
    @Override public void setAlpha(int alpha) { opacity = alpha; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public void getOutline(Outline outline) {
        if (path.isEmpty()) { outline.setEmpty(); return; }
        outline.setPath(path);
        outline.setAlpha(Color.alpha(fillColor) * opacity / 65025f);
    }
}
