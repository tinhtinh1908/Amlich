package com.dtinh.lichviet;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** App preferences kept outside the calendar screen. */
public final class SettingsActivity extends ThemedActivity {
    private static final int REQUEST_BACKGROUND_IMAGE = 4102;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        UiKit.Palette colors = new UiKit.Palette(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(UiKit.pageBackground(colors));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int page = UiKit.dp(this, 18);
        root.setPadding(page, page, page, UiKit.dp(this, 30));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        View back = new BackButton(colors.surface, colors.primary);
        back.setContentDescription("Quay lại");
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(UiKit.dp(this, 40), UiKit.dp(this, 40)));
        TextView title = UiKit.text(this, "Cài đặt", 25, colors.primary, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.setMarginStart(UiKit.dp(this, 14));
        header.addView(title, titleParams);
        root.addView(header, row(-1, UiKit.dp(this, 50), 0));

        root.addView(sectionLabel("GIAO DIỆN", colors), row(-1, -2, 24));
        LinearLayout themeCard = card(colors);
        themeCard.setClickable(true);
        themeCard.setFocusable(true);
        themeCard.setContentDescription("Chọn chủ đề giao diện");
        themeCard.addView(UiKit.text(this, "Chủ đề", 16,
                colors.primary, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView selectedTheme = UiKit.text(this,
                themeLabel(ThemeManager.getMode(this)) + "  ›", 14,
                colors.accent, true);
        selectedTheme.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        themeCard.addView(selectedTheme, new LinearLayout.LayoutParams(-2, -2));
        themeCard.setOnClickListener(v -> showThemePicker());
        root.addView(themeCard, row(-1, -2, 8));

        LinearLayout backgroundCard = card(colors);
        backgroundCard.addView(UiKit.text(this, "Ảnh nền lịch", 16,
                colors.primary, true), new LinearLayout.LayoutParams(0, -2, 1));
        if (BackgroundImageManager.hasBackground(this)) {
            TextView removeBackground = UiKit.button(this, "XÓA",
                    colors.primary, colors.surfaceSoft, 12);
            removeBackground.setOnClickListener(v -> {
                BackgroundImageManager.clearBackground(this);
                recreate();
            });
            LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                    UiKit.dp(this, 64), UiKit.dp(this, 44));
            removeParams.setMarginStart(UiKit.dp(this, 8));
            backgroundCard.addView(removeBackground, removeParams);
        }
        TextView chooseBackground = UiKit.button(this,
                BackgroundImageManager.hasBackground(this) ? "ĐỔI ẢNH" : "CHỌN ẢNH",
                -1, colors.accent, 12);
        chooseBackground.setOnClickListener(v -> openBackgroundPicker());
        LinearLayout.LayoutParams chooseParams = new LinearLayout.LayoutParams(
                UiKit.dp(this, 92), UiKit.dp(this, 44));
        chooseParams.setMarginStart(UiKit.dp(this, 8));
        backgroundCard.addView(chooseBackground, chooseParams);
        root.addView(backgroundCard, row(-1, -2, 8));

        if (BackgroundImageManager.hasBackground(this)) {
            LinearLayout frostedCard = card(colors);
            frostedCard.addView(UiKit.text(this, "Nền mờ", 16,
                    colors.primary, true), new LinearLayout.LayoutParams(0, -2, 1));
            Switch frostedSwitch = new Switch(this);
            frostedSwitch.setContentDescription("Bật hoặc tắt nền mờ của nút widget 4x4");
            frostedSwitch.setChecked(BackgroundImageManager.isFrostedEnabled(this));
            tintSwitch(frostedSwitch, colors);
            frostedSwitch.setOnCheckedChangeListener((button, checked) ->
                    BackgroundImageManager.setFrostedEnabled(this, checked));
            frostedCard.addView(frostedSwitch, new LinearLayout.LayoutParams(-2, -2));
            root.addView(frostedCard, row(-1, -2, 8));
        }

        root.addView(sectionLabel("DỮ LIỆU", colors), row(-1, -2, 24));
        LinearLayout backupCard = card(colors);
        LinearLayout backupText = new LinearLayout(this);
        backupText.setOrientation(LinearLayout.VERTICAL);
        backupText.addView(UiKit.text(this, "Sao lưu ghi chú", 16, colors.primary, true));
        backupCard.addView(backupText, new LinearLayout.LayoutParams(0, -2, 1));
        TextView backup = UiKit.button(this, "SAO LƯU", -1, colors.accent, 12);
        backup.setOnClickListener(v -> XiaomiNotesBackup.showBackupDialog(this));
        LinearLayout.LayoutParams backupParams = new LinearLayout.LayoutParams(
                UiKit.dp(this, 92), UiKit.dp(this, 44));
        backupParams.setMarginStart(UiKit.dp(this, 12));
        backupCard.addView(backup, backupParams);
        root.addView(backupCard, row(-1, -2, 8));

        root.addView(sectionLabel("ỨNG DỤNG", colors), row(-1, -2, 24));
        LinearLayout updateCard = card(colors);
        LinearLayout updateText = new LinearLayout(this);
        updateText.setOrientation(LinearLayout.VERTICAL);
        updateText.addView(UiKit.text(this, "Kiểm tra cập nhật", 16, colors.primary, true));
        updateCard.addView(updateText, new LinearLayout.LayoutParams(0, -2, 1));
        Switch updateSwitch = new Switch(this);
        updateSwitch.setContentDescription("Bật hoặc tắt kiểm tra cập nhật");
        updateSwitch.setChecked(UpdateChecker.isEnabled(this));
        tintSwitch(updateSwitch, colors);
        updateSwitch.setOnCheckedChangeListener((button, checked) -> {
            UpdateChecker.setEnabled(this, checked);
            if (checked) UpdateChecker.check(this);
        });
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(-2, -2);
        switchParams.setMarginStart(UiKit.dp(this, 12));
        updateCard.addView(updateSwitch, switchParams);
        root.addView(updateCard, row(-1, -2, 8));

        TextView version = UiKit.text(this,
                "Âm lịch Việt Nam  ·  Phiên bản " + versionName(), 12,
                colors.primary, true);
        version.setGravity(Gravity.CENTER);
        root.addView(version, row(-1, UiKit.dp(this, 28), 26));
        TextView copyright = UiKit.text(this,
                "Copyright © 2026 DTINH. All rights reserved.", 10,
                colors.secondary, false);
        copyright.setGravity(Gravity.CENTER);
        root.addView(copyright, row(-1, UiKit.dp(this, 24), 0));

        setContentView(scroll);
        UiKit.applySystemBars(this, colors);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            root.setPadding(page, page + bars.top, page,
                    UiKit.dp(this, 30) + bars.bottom);
            return insets;
        });
        root.requestApplyInsets();
        UiKit.animateIn(root, 10.0f);
    }

    private void openBackgroundPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("image/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_BACKGROUND_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_BACKGROUND_IMAGE || resultCode != RESULT_OK
                || data == null || data.getData() == null) return;
        if (BackgroundImageManager.setBackground(this, data.getData(), data.getFlags())) {
            recreate();
        } else {
            Toast.makeText(this, "Không thể đọc ảnh đã chọn", Toast.LENGTH_SHORT).show();
        }
    }

    private LinearLayout card(UiKit.Palette colors) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        int p = UiKit.dp(this, 16);
        card.setPadding(p, p, p, p);
        card.setBackground(cardBackground(colors));
        return card;
    }

    private android.graphics.drawable.Drawable cardBackground(UiKit.Palette colors) {
        return UiKit.rounded(colors.surface, UiKit.dp(this, 20));
    }

    private void showThemePicker() {
        String[] labels = {"Tự động · Sáng/Tối", "Sáng", "Tối"};
        UiKit.Palette colors = new UiKit.Palette(this);
        Dialog dialog = new Dialog(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        int padding = UiKit.dp(this, 20);
        panel.setPadding(padding, padding, padding, UiKit.dp(this, 12));
        panel.setBackground(UiKit.rounded(colors.surface, UiKit.dp(this, 26)));
        panel.addView(UiKit.text(this, "Chủ đề", 21, colors.primary, true),
                new LinearLayout.LayoutParams(-1, UiKit.dp(this, 40)));

        int selectedMode = ThemeManager.getMode(this);
        for (int mode = ThemeManager.AUTO; mode <= ThemeManager.DARK; mode++) {
            final int selected = mode;
            LinearLayout option = new LinearLayout(this);
            option.setGravity(Gravity.CENTER_VERTICAL);
            option.setPadding(UiKit.dp(this, 14), 0, UiKit.dp(this, 14), 0);
            option.setClickable(true);
            option.setFocusable(true);
            option.setBackground(UiKit.rounded(
                    mode == selectedMode ? colors.accentSoft : colors.surfaceSoft,
                    UiKit.dp(this, 16)));
            TextView label = UiKit.text(this, labels[mode], 15, colors.primary, true);
            option.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
            TextView check = UiKit.text(this, mode == selectedMode ? "✓" : "", 18,
                    colors.accent, true);
            check.setGravity(Gravity.CENTER);
            option.addView(check, new LinearLayout.LayoutParams(
                    UiKit.dp(this, 28), UiKit.dp(this, 28)));
            option.setOnClickListener(v -> {
                dialog.dismiss();
                if (selected == ThemeManager.getMode(this)) return;
                ThemeManager.setMode(this, selected);
                recreate();
            });
            LinearLayout.LayoutParams optionParams = new LinearLayout.LayoutParams(
                    -1, UiKit.dp(this, 52));
            optionParams.topMargin = UiKit.dp(this, 8);
            panel.addView(option, optionParams);
        }

        TextView cancel = UiKit.text(this, "HỦY", 13, colors.accent, true);
        cancel.setGravity(Gravity.CENTER);
        cancel.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                UiKit.dp(this, 72), UiKit.dp(this, 44));
        cancelParams.gravity = Gravity.END;
        cancelParams.topMargin = UiKit.dp(this, 4);
        panel.addView(cancel, cancelParams);

        dialog.setContentView(panel);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    android.graphics.Color.TRANSPARENT));
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            android.view.WindowManager.LayoutParams params = window.getAttributes();
            params.width = getResources().getDisplayMetrics().widthPixels
                    - UiKit.dp(this, 36);
            params.dimAmount = 0.58f;
            window.setAttributes(params);
        }
        UiKit.animateIn(panel, 8.0f);
    }

    private String themeLabel(int mode) {
        if (mode == ThemeManager.LIGHT) return "Sáng";
        if (mode == ThemeManager.DARK) return "Tối";
        return "Tự động";
    }

    private TextView sectionLabel(String value, UiKit.Palette colors) {
        return UiKit.text(this, value, 11, colors.accent, true);
    }

    private void tintSwitch(Switch toggle, UiKit.Palette colors) {
        int[][] states = {
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        toggle.setThumbTintList(new ColorStateList(states,
                new int[]{colors.accent, colors.secondary}));
        toggle.setTrackTintList(new ColorStateList(states,
                new int[]{withAlpha(colors.accent, 112), withAlpha(colors.muted, 92)}));
    }

    private int withAlpha(int color, int alpha) {
        return android.graphics.Color.argb(alpha,
                android.graphics.Color.red(color),
                android.graphics.Color.green(color),
                android.graphics.Color.blue(color));
    }

    private LinearLayout.LayoutParams row(int width, int height, int topDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.topMargin = UiKit.dp(this, topDp);
        return params;
    }

    private String versionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName == null ? "" : info.versionName;
        } catch (Exception ignored) {
            return "";
        }
    }

    private final class BackButton extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int fillColor;
        private final int iconColor;

        BackButton(int fillColor, int iconColor) {
            super(SettingsActivity.this);
            this.fillColor = fillColor;
            this.iconColor = iconColor;
            setClickable(true);
            setFocusable(true);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float density = getResources().getDisplayMetrics().density;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(fillColor);
            canvas.drawRoundRect(new RectF(0, 0, getWidth(), getHeight()),
                    15 * density, 15 * density, paint);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.8f * density);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(iconColor);
            canvas.drawLine(cx + 3 * density, cy - 5 * density,
                    cx - 3 * density, cy, paint);
            canvas.drawLine(cx - 3 * density, cy,
                    cx + 3 * density, cy + 5 * density, paint);
        }
    }
}
