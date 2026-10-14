package com.dtinh.lichviet;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.SpannableStringBuilder;
import android.text.style.ReplacementSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

final class UiKit {
    static final float CARD_RADIUS = MiuixMetrics.CARD_RADIUS;
    static final float FIELD_RADIUS = MiuixMetrics.BUTTON_RADIUS;
    static final long MOTION_DURATION_MS = 220;
    static final long PRESS_DURATION_MS = 70;
    static final long RELEASE_DURATION_MS = 220;
    static final MiuixMotion RELEASE_SPRING = new MiuixMotion(0.7d, 987.0d, 0.25d);
    static final PathInterpolator MOTION_EASING = new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f);

    private UiKit() {
    }

    static final class Palette {
        final boolean night;
        final int background;
        final int surface;
        final int surfaceSoft;
        final int divider;
        final int primary;
        final int secondary;
        final int muted;
        final int accent;
        final int accentSoft;
        final int danger;
        final int onAccent;

        Palette(Context context) {
            this(ThemeManager.getColorScheme(context), ThemeManager.isDark(context));
        }

        Palette(int i, boolean z) {
            this.night = z;
            int[] iArrRecipe = AppColors.recipe(i, this.night);
            this.background = iArrRecipe[0];
            this.surface = iArrRecipe[1];
            this.surfaceSoft = iArrRecipe[2];
            this.divider = iArrRecipe[3];
            this.primary = iArrRecipe[4];
            this.secondary = iArrRecipe[5];
            this.muted = iArrRecipe[6];
            this.accent = iArrRecipe[7];
            this.accentSoft = iArrRecipe[8];
            this.danger = iArrRecipe[9];
            this.onAccent = iArrRecipe[10];
        }

        private Palette(boolean z, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11) {
            this.night = z;
            this.background = i;
            this.surface = i2;
            this.surfaceSoft = i3;
            this.divider = i4;
            this.primary = i5;
            this.secondary = i6;
            this.muted = i7;
            this.accent = i8;
            this.accentSoft = i9;
            this.danger = i10;
            this.onAccent = i11;
        }
    }

    static Palette photoPalette(Context context) {
        int i;
        Palette palette = new Palette(context);
        boolean zIsFrostedEnabled = BackgroundImageManager.isFrostedEnabled(context);
        int i2 = palette.surface;
        if (zIsFrostedEnabled) {
            i = palette.night ? 190 : 222;
        } else {
            i = 255;
        }
        return new Palette(palette.night, palette.background, withAlpha(i2, i), withAlpha(palette.surfaceSoft, zIsFrostedEnabled ? 210 : 255), palette.divider, -1, -2236963, palette.muted, palette.accent, palette.accentSoft, palette.danger, palette.onAccent);
    }

    private static int withAlpha(int i, int i2) {
        return Color.argb(i2, Color.red(i), Color.green(i), Color.blue(i));
    }

    static Drawable pageBackground(Palette palette) {
        return rounded(palette.background, 0.0f);
    }

    static int dialogSurface(Palette palette) {
        return palette.surface;
    }

    static void animateIn(View view, float f) {
        animateContent(view, 0.0f, f, 0L);
    }

    static void animateContent(View view, float f, float f2, long j) {
        view.animate().withEndAction(null).cancel();
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        if (!ValueAnimator.areAnimatorsEnabled()) {
            view.setAlpha(1.0f);
            view.setTranslationX(0.0f);
            view.setTranslationY(0.0f);
        } else {
            view.setAlpha(0.65f);
            view.setTranslationX(dp(view.getContext(), f));
            view.setTranslationY(dp(view.getContext(), f2));
            view.animate().alpha(1.0f).translationX(0.0f).translationY(0.0f).setStartDelay(j).setDuration(220L).setInterpolator(MOTION_EASING).start();
        }
    }

    static void animateChildren(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View childAt = viewGroup.getChildAt(i);
            if (childAt.getVisibility() == 0) {
                animateContent(childAt, 0.0f, 5.0f, ((long) Math.min(i, 4)) * 18);
            }
        }
    }

    static void transitionPages(View view, View view2, int i) {
        view.animate().withEndAction(null).cancel();
        view2.animate().withEndAction(null).cancel();
        if (!ValueAnimator.areAnimatorsEnabled()) {
            view.setVisibility(8);
            view2.setVisibility(0);
            view.setAlpha(1.0f);
            view2.setAlpha(1.0f);
            view.setTranslationX(0.0f);
            view2.setTranslationX(0.0f);
            return;
        }
        view.setVisibility(0);
        view2.setVisibility(0);
        view2.setAlpha(0.4f);
        view2.setTranslationX(dp(view2.getContext(), 8.0f) * i);
        view.animate().alpha(0.0f).translationX((-dp(view.getContext(), 8.0f)) * i).setStartDelay(0L).setDuration(180L).setInterpolator(MOTION_EASING).withLayer().withEndAction(() -> {
            view.setVisibility(8);
            view.setAlpha(1.0f);
            view.setTranslationX(0.0f);
        }).start();
        view2.animate().alpha(1.0f).translationX(0.0f).translationY(0.0f).setStartDelay(0L).setDuration(220L).setInterpolator(MOTION_EASING).withLayer().start();
    }

    static void configureDialog(Dialog dialog, Palette palette) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(0));
        window.setWindowAnimations(ValueAnimator.areAnimatorsEnabled() ? R.style.MiuixDialogAnimation : 0);
        window.addFlags(2);
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.dimAmount = palette.night ? 0.6f : 0.3f;
        window.setAttributes(attributes);
    }

    static void styleAlert(AlertDialog alertDialog, Activity activity) {
        Palette palette = new Palette(activity);
        configureDialog(alertDialog, palette);
        Window window = alertDialog.getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(rounded(palette.surface, dp(activity, MiuixMetrics.DIALOG_RADIUS)));
        animateIn(window.getDecorView(), 4.0f);
        Button button = alertDialog.getButton(-1);
        Button[] buttonArr = {button, alertDialog.getButton(-2)};
        int length = buttonArr.length;
        for (int i = 0; i < length; i++) {
            Button button2 = buttonArr[i];
            if (button2 != null) {
                button2.setAllCaps(false);
                button2.setTextSize(15.0f);
                button2.setMinimumHeight(dp(activity, 44.0f));
                button2.setTextColor(button2 == button ? palette.onAccent : palette.primary);
                button2.setBackground(interactiveBackground(activity, button2 == button ? palette.accent : palette.surfaceSoft, FIELD_RADIUS));
                button2.setPadding(dp(activity, FIELD_RADIUS), 0, dp(activity, FIELD_RADIUS), 0);
                ViewGroup.LayoutParams layoutParams = button2.getLayoutParams();
                layoutParams.height = dp(activity, 44.0f);
                if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                    ((ViewGroup.MarginLayoutParams) layoutParams).setMargins(dp(activity, 4.0f), dp(activity, 4.0f), dp(activity, 4.0f), dp(activity, 4.0f));
                }
                button2.setLayoutParams(layoutParams);
                bindPressMotion(button2);
            }
        }
    }

    static void setIcon(TextView textView, int i, int i2, int i3) {
        setInlineIcon(textView, i, i2, i3);
    }

    private static void setInlineIcon(TextView textView, int i, int i2, int i3) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(textView.getText());
        InlineIconSpan[] inlineIconSpanArr = (InlineIconSpan[]) spannableStringBuilder.getSpans(0, spannableStringBuilder.length(), InlineIconSpan.class);
        for (int length = inlineIconSpanArr.length - 1; length >= 0; length--) {
            int spanStart = spannableStringBuilder.getSpanStart(inlineIconSpanArr[length]);
            int spanEnd = spannableStringBuilder.getSpanEnd(inlineIconSpanArr[length]);
            spannableStringBuilder.removeSpan(inlineIconSpanArr[length]);
            spannableStringBuilder.delete(spanStart, spanEnd);
        }
        if (textView.getContentDescription() == null && spannableStringBuilder.length() > 0) {
            textView.setContentDescription(spannableStringBuilder.toString());
        }
        Drawable drawableMutate = textView.getContext().getDrawable(i).mutate();
        drawableMutate.setTint(i2);
        int iDp = dp(textView.getContext(), i3);
        drawableMutate.setBounds(0, 0, iDp, iDp);
        InlineIconSpan inlineIconSpan = new InlineIconSpan(drawableMutate, iDp, dp(textView.getContext(), spannableStringBuilder.length() == 0 ? 0.0f : 6.0f));
        spannableStringBuilder.insert(0, (CharSequence) "￼");
        spannableStringBuilder.setSpan(inlineIconSpan, 0, 1, 33);
        textView.setCompoundDrawablesRelative(null, null, null, null);
        textView.setText(spannableStringBuilder);
    }

    private static final class InlineIconSpan extends ReplacementSpan {
        private final Drawable icon;
        private final int size;
        private final int gap;

        InlineIconSpan(Drawable drawable, int i, int i2) {
            this.icon = drawable;
            this.size = i;
            this.gap = i2;
        }

        @Override
        public int getSize(Paint paint, CharSequence charSequence, int i, int i2, Paint.FontMetricsInt fontMetricsInt) {
            if (fontMetricsInt != null) {
                Paint.FontMetricsInt fontMetricsInt2 = paint.getFontMetricsInt();
                int i3 = (fontMetricsInt2.ascent + fontMetricsInt2.descent) / 2;
                fontMetricsInt.ascent = Math.min(fontMetricsInt2.ascent, i3 - (this.size / 2));
                fontMetricsInt.descent = Math.max(fontMetricsInt2.descent, (i3 + this.size) - (this.size / 2));
                fontMetricsInt.top = fontMetricsInt.ascent;
                fontMetricsInt.bottom = fontMetricsInt.descent;
            }
            return this.size + this.gap;
        }

        @Override
        public void draw(Canvas canvas, CharSequence charSequence, int i, int i2, float f, int i3, int i4, int i5, Paint paint) {
            Paint.FontMetricsInt fontMetricsInt = paint.getFontMetricsInt();
            float f2 = i4 + ((fontMetricsInt.ascent + fontMetricsInt.descent) / 2.0f);
            int iSave = canvas.save();
            canvas.translate(f, f2 - (this.size / 2.0f));
            this.icon.draw(canvas);
            canvas.restoreToCount(iSave);
        }
    }

    static Drawable interactiveBackground(Context context, int i, float f) {
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(46, 128, 128, 128)), rounded(i, dp(context, f)), rounded(-1, dp(context, f)));
    }

    static void bindPressMotion(View view) {
        view.setOnTouchListener((view2, motionEvent) -> {
            if (!view2.isEnabled() || !ValueAnimator.areAnimatorsEnabled()) {
                return false;
            }
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked == 0) {
                view2.animate().withEndAction(null).cancel();
                view2.setAlpha(1.0f);
                view2.setTranslationX(0.0f);
                view2.setTranslationY(0.0f);
                view2.animate().scaleX(0.96f).scaleY(0.96f).setStartDelay(0L).setDuration(PRESS_DURATION_MS).setInterpolator(MOTION_EASING).start();
                return false;
            }
            if (actionMasked == 1 || actionMasked == 3) {
                view2.animate().withEndAction(null).cancel();
                view2.animate().scaleX(1.0f).scaleY(1.0f).setStartDelay(0L).setDuration(220L).setInterpolator(RELEASE_SPRING).start();
                return false;
            }
            return false;
        });
    }

    static void applySystemBars(Activity activity, Palette palette) {
        applySystemBars(activity, palette, false);
    }

    static void applySystemBars(Activity activity, Palette palette, boolean z) {
        Window window = activity.getWindow();
        window.setDecorFitsSystemWindows(false);
        window.setStatusBarColor(0);
        window.setNavigationBarColor(0);
        window.setStatusBarContrastEnforced(false);
        window.setNavigationBarContrastEnforced(false);
        window.clearFlags(4);
        window.setBackgroundDrawable(new ColorDrawable(palette.background));
        View decorView = window.getDecorView();
        WindowInsetsController windowInsetsController = decorView.getWindowInsetsController();
        if (windowInsetsController != null) {
            windowInsetsController.setSystemBarsAppearance(z || palette.night ? 0 : 24, 24);
        } else {
            decorView.setSystemUiVisibility(z || palette.night ? 0 : 8208);
        }
    }

    static TextView text(Context context, CharSequence charSequence, float f, int i, boolean z) {
        TextView textView = new TextView(context);
        textView.setText(charSequence);
        textView.setTextSize(f);
        textView.setTextColor(i);
        textView.setIncludeFontPadding(false);
        textView.setTypeface(Typeface.create(z ? "sans-serif-medium" : "sans-serif", 0));
        return textView;
    }

    static TextView button(Context context, CharSequence charSequence, int i, int i2, float f) {
        TextView textViewText = text(context, charSequence, MiuixMetrics.BODY_TEXT, i, false);
        textViewText.setAllCaps(false);
        textViewText.setMinHeight(dp(context, MiuixMetrics.BUTTON_MIN_HEIGHT));
        textViewText.setPadding(dp(context, 16.0f), 0, dp(context, 16.0f), 0);
        textViewText.setGravity(17);
        textViewText.setBackground(interactiveBackground(context, i2, f));
        bindPressMotion(textViewText);
        textViewText.setClickable(true);
        textViewText.setFocusable(true);
        return textViewText;
    }

    static EditText field(Context context, Palette palette, int i, boolean z) {
        EditText editText = new EditText(context);
        editText.setHint(i);
        editText.setHintTextColor(palette.secondary);
        editText.setTextColor(palette.primary);
        editText.setTextSize(MiuixMetrics.BODY_TEXT);
        editText.setTypeface(Typeface.create("sans-serif", 0));
        editText.setSingleLine(z);
        int iDp = dp(context, FIELD_RADIUS);
        int iDp2 = dp(context, 12.0f);
        editText.setPadding(iDp, iDp2, iDp, iDp2);
        editText.setBackground(fieldBackground(context, palette));
        tintField(editText, palette);
        return editText;
    }

    static void tintField(EditText field, Palette palette) {
        field.setHighlightColor(palette.accentSoft);
        Drawable cursor = field.getTextCursorDrawable();
        if (cursor != null) {
            cursor = cursor.mutate();
            cursor.setTint(palette.accent);
            field.setTextCursorDrawable(cursor);
        }
    }

    static Drawable fieldBackground(Context context, Palette palette) {
        int fill = palette.night ? palette.surface : 0xffe8e8e8;
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused},
                stroked(fill, palette.accent, dp(context, FIELD_RADIUS), dp(context, 2f)));
        states.addState(new int[]{}, rounded(fill, dp(context, FIELD_RADIUS)));
        return states;
    }

    static Drawable rounded(int color, float radius) {
        return new MiuixSurfaceDrawable(color, radius);
    }

    static Drawable stroked(int color, int stroke, float radius, int width) {
        return new MiuixSurfaceDrawable(color, radius, stroke, width);
    }

    static int dp(Context context, float f) {
        return Math.round(f * context.getResources().getDisplayMetrics().density);
    }
}
