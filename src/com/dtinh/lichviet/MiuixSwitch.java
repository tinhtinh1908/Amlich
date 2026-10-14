package com.dtinh.lichviet;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.widget.CompoundButton;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.HapticFeedbackConstants;

final class MiuixSwitch extends CompoundButton {
    private UiKit.Palette palette;
    private final Paint paint;
    private final RectF track;
    private float progress;
    private ValueAnimator animator;
    private float downX, initialProgress;
    private boolean dragging;

    MiuixSwitch(Context context) {
        super(context);
        this.paint = new Paint(1);
        this.track = new RectF();
        this.palette = new UiKit.Palette(context);
        setButtonDrawable((Drawable) null);
        setClickable(true);
        setFocusable(true);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(resolveSize(UiKit.dp(getContext(), 62.0f), i), resolveSize(UiKit.dp(getContext(), 48.0f), i2));
    }

    @Override
    public void setChecked(boolean z) {
        boolean z2 = z != isChecked();
        super.setChecked(z);
        if (this.palette == null) {
            this.progress = z ? 1.0f : 0.0f;
            return;
        }
        if (this.animator != null) {
            this.animator.cancel();
        }
        float f = z ? 1.0f : 0.0f;
        if ((!z2 && Math.abs(progress - f) < .001f) || !isAttachedToWindow() || !ValueAnimator.areAnimatorsEnabled()) {
            this.progress = f;
            invalidate();
            return;
        }
        this.animator = ValueAnimator.ofFloat(this.progress, f);
        this.animator.setDuration(220L);
        this.animator.setInterpolator(UiKit.RELEASE_SPRING);
        this.animator.addUpdateListener(valueAnimator -> {
            this.progress = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            postInvalidateOnAnimation();
        });
        this.animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float f = getResources().getDisplayMetrics().density;
        float f2 = MiuixMetrics.SWITCH_WIDTH * f;
        float f3 = MiuixMetrics.SWITCH_HEIGHT * f;
        this.track.set((getWidth() - f2) / 2.0f, (getHeight() - f3) / 2.0f, (getWidth() + f2) / 2.0f, (getHeight() + f3) / 2.0f);
        float fMax = Math.max(0.0f, Math.min(1.0f, this.progress));
        this.paint.setColor(blend(this.palette.night ? 0xff505050 : 0xffe6e6e6, this.palette.accent, fMax));
        this.paint.setAlpha(isEnabled() ? 255 : 100);
        canvas.drawRoundRect(this.track, f3 / 2.0f, f3 / 2.0f, this.paint);
        this.paint.setColor(-1);
        this.paint.setAlpha(isEnabled() ? 255 : 150);
        canvas.drawCircle(this.track.left + (14.0f * f) + (21.0f * f * fMax), this.track.centerY(), 10.0f * f * (isPressed() || dragging ? 1.127f : 1f), this.paint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX(); initialProgress = progress; dragging = false;
                if (animator != null) animator.cancel();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!dragging && Math.abs(event.getX() - downX) > ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
                    dragging = true; getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (dragging) {
                    progress = Math.max(0, Math.min(1, initialProgress + (event.getX() - downX) / UiKit.dp(getContext(), 21)));
                    invalidate(); return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (dragging) {
                    boolean checked = progress >= .5f;
                    dragging = false; setPressed(false);
                    MotionEvent cancelled = MotionEvent.obtain(event);
                    cancelled.setAction(MotionEvent.ACTION_CANCEL);
                    super.onTouchEvent(cancelled); cancelled.recycle();
                    if (checked != isChecked()) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    setChecked(checked); getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                dragging = false; setChecked(isChecked());
                break;
            default: break;
        }
        boolean handled = super.onTouchEvent(event);
        invalidate(); return handled;
    }

    private static int blend(int i, int i2, float f) {
        return Color.rgb(Math.round(Color.red(i) + ((Color.red(i2) - Color.red(i)) * f)), Math.round(Color.green(i) + ((Color.green(i2) - Color.green(i)) * f)), Math.round(Color.blue(i) + ((Color.blue(i2) - Color.blue(i)) * f)));
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return "android.widget.Switch";
    }

    @Override
    protected void onDetachedFromWindow() {
        if (this.animator != null) {
            this.animator.cancel();
        }
        this.progress = isChecked() ? 1.0f : 0.0f;
        super.onDetachedFromWindow();
    }
}
