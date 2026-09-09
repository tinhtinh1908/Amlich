package com.dtinh.lichviet;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.animation.PathInterpolator;
import android.widget.EditText;
import android.widget.TextView;

final class UiKit {
    static final float CARD_RADIUS = 24f;
    static final float FIELD_RADIUS = 16f;
    static final long MOTION_DURATION_MS = 280L;
    static final PathInterpolator MOTION_EASING =
            new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f);

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

        Palette(Context context) {
            night = ThemeManager.isDark(context);
            if (night) {
                background = Color.rgb(18, 19, 23);
                surface = Color.rgb(29, 31, 37);
                surfaceSoft = Color.rgb(38, 40, 47);
                divider = Color.rgb(52, 55, 64);
                primary = Color.rgb(245, 247, 250);
                secondary = Color.rgb(168, 172, 182);
                muted = Color.rgb(86, 90, 100);
                accent = Color.rgb(91, 150, 247);
                accentSoft = Color.rgb(43, 62, 92);
                danger = Color.rgb(244, 116, 116);
            } else {
                background = Color.rgb(247, 248, 252);
                surface = Color.WHITE;
                surfaceSoft = Color.rgb(239, 242, 247);
                divider = Color.rgb(226, 230, 237);
                primary = Color.rgb(24, 27, 34);
                secondary = Color.rgb(113, 118, 130);
                muted = Color.rgb(194, 198, 207);
                accent = Color.rgb(66, 133, 244);
                accentSoft = Color.rgb(225, 235, 252);
                danger = Color.rgb(221, 75, 75);
            }
        }
    }

    static GradientDrawable pageBackground(Palette palette) {
        return rounded(palette.background, 0);
    }

    static int dialogSurface(Palette palette) {
        return palette.surface;
    }

    static void animateIn(View view, float translationDp) {
        view.animate().cancel();
        view.setAlpha(0.0f);
        view.setTranslationY(dp(view.getContext(), translationDp));
        view.post(() -> view.animate()
                .alpha(1.0f)
                .translationY(0.0f)
                .setDuration(MOTION_DURATION_MS)
                .setInterpolator(MOTION_EASING)
                .start());
    }

    @SuppressWarnings("deprecation")
    static void applySystemBars(Activity activity, Palette palette) {
        applySystemBars(activity, palette, false);
    }

    @SuppressWarnings("deprecation")
    static void applySystemBars(Activity activity, Palette palette, boolean forceDarkBars) {
        Window window = activity.getWindow();
        window.setDecorFitsSystemWindows(false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setStatusBarContrastEnforced(false);
        window.setNavigationBarContrastEnforced(false);
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        window.setBackgroundDrawable(new ColorDrawable(palette.background));

        // PhoneWindow#getInsetsController crashes on some HyperOS builds when
        // called before DecorView has been attached. getDecorView() creates it,
        // then the controller is queried from that concrete view.
        View decorView = window.getDecorView();
        WindowInsetsController controller = decorView.getWindowInsetsController();
        if (controller != null) {
            int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            boolean darkSurface = forceDarkBars || palette.night;
            controller.setSystemBarsAppearance(darkSurface ? 0 : lightBars, lightBars);
        } else {
            int lightBars = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            boolean darkSurface = forceDarkBars || palette.night;
            decorView.setSystemUiVisibility(darkSurface ? 0 : lightBars);
        }
    }

    static TextView text(
            Context context,
            CharSequence value,
            float sizeSp,
            int color,
            boolean mediumWeight) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        view.setTypeface(Typeface.create(
                mediumWeight ? "sans-serif-medium" : "sans-serif",
                Typeface.NORMAL));
        return view;
    }

    static TextView button(
            Context context,
            CharSequence label,
            int textColor,
            int backgroundColor,
            float radiusDp) {
        TextView button = text(context, label, 12f, textColor, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(rounded(backgroundColor, dp(context, radiusDp)));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    static EditText field(
            Context context,
            Palette palette,
            int hintResource,
            boolean singleLine) {
        EditText field = new EditText(context);
        field.setHint(hintResource);
        field.setHintTextColor(palette.secondary);
        field.setTextColor(palette.primary);
        field.setTextSize(15f);
        field.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        field.setSingleLine(singleLine);

        int horizontal = dp(context, FIELD_RADIUS);
        int vertical = dp(context, 12f);
        field.setPadding(horizontal, vertical, horizontal, vertical);
        field.setBackground(stroked(
                palette.surfaceSoft,
                palette.divider,
                dp(context, FIELD_RADIUS),
                dp(context, 1f)));
        return field;
    }

    static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(radiusPx);
        return background;
    }

    static GradientDrawable stroked(
            int fillColor,
            int strokeColor,
            float radiusPx,
            int strokeWidthPx) {
        GradientDrawable background = rounded(fillColor, radiusPx);
        background.setStroke(strokeWidthPx, strokeColor);
        return background;
    }

    static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
