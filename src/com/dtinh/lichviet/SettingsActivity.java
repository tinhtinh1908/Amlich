package com.dtinh.lichviet;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Build;
import android.provider.MediaStore;
import android.transition.Fade;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public final class SettingsActivity extends ThemedActivity {
    private static final int REQUEST_BACKGROUND_IMAGE = 4102;
    private PopupWindow selectionPopup;
    private Dialog appearancePicker;
    private TextView backgroundAction;

    private interface Selection {
        void apply(int i);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        UiKit.Palette palette = new UiKit.Palette(this);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setId(R.id.settings_scroll);
        scrollView.setFillViewport(true);
        scrollView.setBackground(UiKit.pageBackground(palette));
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int iDp = UiKit.dp(this, 16.0f);
        linearLayout.setPadding(iDp, iDp, iDp, UiKit.dp(this, 24.0f));
        scrollView.addView(linearLayout, new FrameLayout.LayoutParams(-1, -2));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setGravity(16);
        BackButton backButton = new BackButton(palette.surface, palette.primary);
        backButton.setContentDescription("Quay lại");
        backButton.setOnClickListener(view -> {
            finish();
        });
        linearLayout2.addView(backButton, new LinearLayout.LayoutParams(UiKit.dp(this, 36.0f), UiKit.dp(this, 36.0f)));
        TextView textViewText = UiKit.text(this, "Cài đặt", 22.0f, palette.primary, true);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.setMarginStart(UiKit.dp(this, 16.0f));
        linearLayout2.addView(textViewText, layoutParams);
        linearLayout.addView(linearLayout2, row(-1, UiKit.dp(this, 48.0f), 0));
        LinearLayout supportGroup = preferenceGroup(linearLayout, getString(R.string.support_section), palette);
        TextView supportAction = UiKit.text(this, getString(R.string.support_view), 14, palette.accent, false);
        Drawable supportArrow = getDrawable(R.drawable.ic_chevron_right).mutate();
        supportArrow.setTint(palette.secondary);
        supportArrow.setBounds(0, 0, UiKit.dp(this, 14), UiKit.dp(this, 14));
        supportAction.setCompoundDrawablesRelative(null, null, supportArrow, null);
        supportAction.setCompoundDrawablePadding(UiKit.dp(this, 6));
        preferenceRow(supportGroup, getString(R.string.support_title), supportAction, palette)
                .setOnClickListener(view -> SupportDialog.show(this));
        LinearLayout linearLayoutPreferenceGroup = preferenceGroup(linearLayout, "Giao diện", palette);
        TextView textViewText2 = UiKit.text(this, ThemeManager.isMinimal(this) ? "Lịch xé" : "Tháng / năm", 14.0f, palette.secondary, false);
        setPreferenceArrow(textViewText2, palette.secondary);
        preferenceRow(linearLayoutPreferenceGroup, "Chế độ hiển thị", textViewText2, palette).setOnClickListener(view2 -> {
            showChoicePicker(textViewText2, new String[]{"Lịch tháng / năm", "Lịch xé từng ngày"}, ThemeManager.isMinimal(this) ? 1 : 0, i -> {
                ThemeManager.setMinimal(this, i == 1);
                recreate();
            });
        });
        String[] strArr = {"Theo hệ thống", "Sáng", "Tối"};
        TextView textViewText3 = UiKit.text(this, strArr[ThemeManager.getMode(this)], 14.0f, palette.secondary, false);
        setPreferenceArrow(textViewText3, palette.secondary);
        preferenceRow(linearLayoutPreferenceGroup, "Sáng / tối", textViewText3, palette).setOnClickListener(view3 -> {
            showChoicePicker(textViewText3, strArr, ThemeManager.getMode(this), i -> {
                ThemeManager.setMode(this, i);
                recreate();
            });
        });
        MiuixSwitch softGlassSwitch = new MiuixSwitch(this);
        softGlassSwitch.setContentDescription(getString(R.string.enable_soft_glass));
        softGlassSwitch.setChecked(ThemeManager.isSoftGlassEnabled(this));
        View glassRow = preferenceRow(linearLayoutPreferenceGroup, getString(R.string.enable_soft_glass),
                softGlassSwitch, palette);
        glassRow.setVisibility(Build.VERSION.SDK_INT >= 33 ? View.VISIBLE : View.GONE);
        glassRow.setOnClickListener(view -> softGlassSwitch.toggle());
        LinearLayout opacityRow = glassOpacityRow(palette);
        opacityRow.setVisibility(softGlassSwitch.isChecked() ? View.VISIBLE : View.GONE);
        linearLayoutPreferenceGroup.addView(opacityRow);
        addFontPreferences(linearLayoutPreferenceGroup, palette);
        TextView textViewText4 = UiKit.text(this, AppColors.NAMES[ThemeManager.getColorScheme(this)], 14.0f, palette.secondary, false);
        setPreferenceArrow(textViewText4, palette.secondary);
        preferenceRow(linearLayoutPreferenceGroup, "Màu sắc", textViewText4, palette).setOnClickListener(view4 -> {
            showColorPicker();
        });
        backgroundAction = UiKit.button(this, BackgroundImageManager.hasBackground(this) ? "Đổi ảnh" : "Chọn ảnh",
                palette.accent, palette.accentSoft, 16f);
        backgroundAction.setOnClickListener(view -> openBackgroundPicker());
        preferenceRow(linearLayoutPreferenceGroup, "Ảnh nền lịch", backgroundAction, palette);
        boolean hasBackground = BackgroundImageManager.hasBackground(this);
        View frostedRow = hasBackground && !ThemeManager.isMinimal(this)
                ? addFrostedPreference(linearLayoutPreferenceGroup, palette) : null;
        if (frostedRow != null) {
            frostedRow.setVisibility(softGlassSwitch.isChecked() ? View.GONE : View.VISIBLE);
        }
        softGlassSwitch.setOnCheckedChangeListener((button, enabled) -> {
            ThemeManager.setSoftGlassEnabled(this, enabled);
            opacityRow.setVisibility(enabled ? View.VISIBLE : View.GONE);
            if (frostedRow != null) frostedRow.setVisibility(enabled ? View.GONE : View.VISIBLE);
        });
        if (hasBackground) {
            TextView textViewButton2 = UiKit.button(this, "Xóa ảnh", palette.danger, palette.surfaceSoft, 16.0f);
            textViewButton2.setOnClickListener(view6 -> {
                if (!backgroundAction.isEnabled()) return;
                BackgroundImageManager.clearBackground(this);
                recreate();
            });
            preferenceRow(linearLayoutPreferenceGroup, "Khôi phục nền", textViewButton2, palette);
        }
        LinearLayout linearLayoutPreferenceGroup2 = preferenceGroup(linearLayout, "Dữ liệu", palette);
        TextView textViewButton3 = UiKit.button(this, "Sao lưu", palette.accent, palette.accentSoft, 14.0f);
        UiKit.setIcon(textViewButton3, R.drawable.ic_backup, palette.accent, 16);
        textViewButton3.setOnClickListener(view7 -> {
            XiaomiNotesBackup.showBackupDialog(this);
        });
        preferenceRow(linearLayoutPreferenceGroup2, "Ghi chú", textViewButton3, palette);
        LinearLayout linearLayoutPreferenceGroup3 = preferenceGroup(linearLayout, "Ứng dụng", palette);
        MiuixSwitch miuixSwitch2 = new MiuixSwitch(this);
        miuixSwitch2.setChecked(UpdateChecker.isEnabled(this));
        miuixSwitch2.setContentDescription("Kiểm tra cập nhật");
        miuixSwitch2.setOnCheckedChangeListener((compoundButton2, z2) -> {
            UpdateChecker.setEnabled(this, z2);
            if (z2) {
                UpdateChecker.check(this);
            }
        });
        preferenceRow(linearLayoutPreferenceGroup3, "Kiểm tra cập nhật", miuixSwitch2, palette);
        TextView textViewText5 = UiKit.text(this, "Âm lịch Việt Nam · " + versionName(), 13.0f, palette.secondary, false);
        textViewText5.setGravity(17);
        linearLayout.addView(textViewText5, row(-1, -2, 24));
        TextView textViewText6 = UiKit.text(this, "Copyright © 2026 DTINH. All rights reserved.", 11.0f, palette.muted, false);
        textViewText6.setGravity(17);
        linearLayout.addView(textViewText6, row(-1, -2, 8));
        setContentView(scrollView);
        UiKit.applySystemBars(this, palette);
        linearLayout.setOnApplyWindowInsetsListener((view8, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            linearLayout.setPadding(iDp + insets.left, iDp + insets.top, iDp + insets.right, UiKit.dp(this, 24.0f) + insets.bottom);
            return windowInsets;
        });
        linearLayout.requestApplyInsets();
        if (bundle == null) UiKit.animateIn(scrollView, 5.0f);
    }

    private View addFrostedPreference(LinearLayout group, UiKit.Palette palette) {
        String label = getString(R.string.background_frost);
        MiuixSwitch toggle = new MiuixSwitch(this);
        toggle.setChecked(BackgroundImageManager.isFrostedEnabled(this));
        toggle.setContentDescription(label);
        toggle.setOnCheckedChangeListener((button, enabled) -> BackgroundImageManager.setFrostedEnabled(this, enabled));
        View row = preferenceRow(group, label, toggle, palette);
        row.setOnClickListener(view -> toggle.toggle());
        return row;
    }

    private void addFontPreferences(LinearLayout group, UiKit.Palette palette) {
        String[] fonts = getResources().getStringArray(R.array.calendar_font_choices);
        int current = ThemeManager.getFont(this);
        TextView value = UiKit.text(this, fonts[current], 14f, palette.secondary, false);
        setPreferenceArrow(value, palette.secondary);
        preferenceRow(group, getString(R.string.calendar_font), value, palette)
                .setOnClickListener(view -> showAppearancePicker(R.string.calendar_font, fonts, current, true, index -> {
                    ThemeManager.setFont(this, index);
                    recreate();
                }));
    }

    private LinearLayout glassOpacityRow(UiKit.Palette palette) {
        String label = getString(R.string.soft_glass_opacity);
        int opacity = ThemeManager.getSoftGlassOpacity(this);
        int sidePadding = UiKit.dp(this, 16f);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(sidePadding, UiKit.dp(this, 6f), sidePadding, UiKit.dp(this, 4f));

        LinearLayout heading = new LinearLayout(this);
        heading.setBaselineAligned(false);
        heading.addView(UiKit.text(this, label, 16f, palette.primary, false),
                new LinearLayout.LayoutParams(0, -2, 1f));
        TextView value = UiKit.text(this, getString(R.string.soft_glass_opacity_percent, opacity),
                14f, palette.secondary, false);
        heading.addView(value);
        row.addView(heading, new LinearLayout.LayoutParams(-1, -2));

        SeekBar slider = new SeekBar(this);
        slider.setContentDescription(label);
        slider.setMax(100);
        slider.setProgress(opacity);
        slider.setPadding(0, 0, 0, 0);
        ColorStateList accent = ColorStateList.valueOf(palette.accent);
        slider.setThumbTintList(accent);
        slider.setProgressTintList(accent);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private boolean dragging;
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                value.setText(getString(R.string.soft_glass_opacity_percent, progress));
                if (fromUser && !dragging) ThemeManager.setSoftGlassOpacity(SettingsActivity.this, progress);
            }

            @Override public void onStartTrackingTouch(SeekBar bar) { dragging = true; }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                dragging = false;
                ThemeManager.setSoftGlassOpacity(SettingsActivity.this, bar.getProgress());
            }
        });
        row.addView(slider, new LinearLayout.LayoutParams(-1, UiKit.dp(this, 48f)));
        return row;
    }

    private LinearLayout preferenceGroup(LinearLayout linearLayout, String str, UiKit.Palette palette) {
        TextView textViewText = UiKit.text(this, str, 13.0f, palette.secondary, true);
        textViewText.setPadding(UiKit.dp(this, 12.0f), 0, 0, 0);
        linearLayout.addView(textViewText, row(-1, -2, 20));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(LinearLayout.VERTICAL);
        linearLayout2.setClipToOutline(true);
        linearLayout2.setBackground(UiKit.rounded(palette.surface, UiKit.dp(this, MiuixMetrics.CARD_RADIUS)));
        linearLayout.addView(linearLayout2, row(-1, -2, 8));
        return linearLayout2;
    }

    private void setPreferenceArrow(TextView textView, int i) {
        Drawable drawableMutate = getDrawable(R.drawable.ic_dropdown).mutate();
        drawableMutate.setTint(i);
        int iDp = UiKit.dp(this, 16.0f);
        drawableMutate.setBounds(0, 0, UiKit.dp(this, 10f), iDp);
        textView.setCompoundDrawablesRelative(null, null, drawableMutate, null);
        textView.setCompoundDrawablePadding(UiKit.dp(this, 6.0f));
    }

    private LinearLayout preferenceRow(LinearLayout linearLayout, String str, View view, UiKit.Palette palette) {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(UiKit.dp(this, 16.0f), UiKit.dp(this, 8.0f), UiKit.dp(this, 16.0f), UiKit.dp(this, 8.0f));
        linearLayout2.setMinimumHeight(UiKit.dp(this, 56.0f));
        linearLayout2.setContentDescription(str);
        linearLayout2.setBackground(UiKit.interactiveBackground(this, palette.surface, MiuixMetrics.CARD_RADIUS));
        UiKit.bindPressMotion(linearLayout2);
        linearLayout2.addView(UiKit.text(this, str, 16.0f, palette.primary, false), new LinearLayout.LayoutParams(0, -2, 1.0f));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, UiKit.dp(this, 36.0f));
        layoutParams.setMarginStart(UiKit.dp(this, 8.0f));
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            int screenDp = (int) (getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density);
            textView.setMaxWidth(UiKit.dp(this, Math.min(156f, (screenDp - 64f) * .52f)));
            textView.setSingleLine(true);
            textView.setHorizontallyScrolling(false);
            textView.setAutoSizeTextTypeUniformWithConfiguration(11, 14, 1, 2);
            textView.setGravity(17);
            textView.setPadding(UiKit.dp(this, 8.0f), 0, UiKit.dp(this, 8.0f), 0);
        }
        linearLayout2.addView(view, layoutParams);
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(-1, -2));
        return linearLayout2;
    }

    private void openBackgroundPicker() {
        Intent gallery = new Intent(Intent.ACTION_PICK).setPackage("com.miui.gallery").setType("image/*");
        try {
            startActivityForResult(gallery, REQUEST_BACKGROUND_IMAGE);
            return;
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            // Other devices use Android's Photos and Albums picker.
        }
        try {
            Intent picker = new Intent(MediaStore.ACTION_PICK_IMAGES).setType("image/*");
            startActivityForResult(picker, REQUEST_BACKGROUND_IMAGE);
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            Toast.makeText(this, R.string.background_picker_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_BACKGROUND_IMAGE || resultCode != RESULT_OK
                || data == null || data.getData() == null) return;
        backgroundAction.setEnabled(false);
        new Thread(() -> {
            boolean applied = BackgroundImageManager.setBackground(getApplicationContext(), data.getData());
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                backgroundAction.setEnabled(true);
                if (applied) {
                    recreate();
                } else {
                    Toast.makeText(this, "Không thể đọc ảnh đã chọn", Toast.LENGTH_SHORT).show();
                }
            });
        }, "calendar-background").start();
    }

    private void showColorPicker() {
        showAppearancePicker(R.string.appearance_colors, AppColors.NAMES, ThemeManager.getColorScheme(this),
                false, index -> {
                    ThemeManager.setColorScheme(this, index);
                    recreate();
                });
    }

    private void showAppearancePicker(int titleId, String[] choices, int current, boolean fonts, Selection selection) {
        UiKit.Palette palette = new UiKit.Palette(this);
        if (appearancePicker != null) appearancePicker.dismiss();
        Dialog dialog = new Dialog(this);
        appearancePicker = dialog;
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        int inset = UiKit.dp(this, 16f);
        sheet.setPadding(inset, inset, inset, inset);
        sheet.setBackground(UiKit.rounded(palette.surface, UiKit.dp(this, MiuixMetrics.DIALOG_RADIUS)));
        TextView title = UiKit.text(this, getString(titleId), 20f, palette.primary, true);
        title.setAccessibilityHeading(true);
        sheet.addView(title, row(-1, -2, 0));

        ScrollView scroll = new ScrollView(this);
        ScrollView.LayoutParams gridParams = new ScrollView.LayoutParams(-1, -2);
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(grid, gridParams);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, -2, 1f);
        scrollParams.topMargin = UiKit.dp(this, 12f);
        sheet.addView(scroll, scrollParams);

        for (int start = 0; start < choices.length; start += 2) {
            LinearLayout gridRow = new LinearLayout(this);
            gridRow.setBaselineAligned(false);
            grid.addView(gridRow, row(-1, -2, start == 0 ? 0 : 8));
            for (int column = 0; column < 2; column++) {
                int index = start + column;
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, -1, 1f);
                if (column == 1) cellParams.setMarginStart(UiKit.dp(this, 8f));
                if (index >= choices.length) {
                    gridRow.addView(new View(this), cellParams);
                    continue;
                }
                View cell = appearanceCell(choices[index], index, index == current, fonts, palette);
                cell.setOnClickListener(view -> {
                    dialog.dismiss();
                    if (index != current) selection.apply(index);
                });
                gridRow.addView(cell, cellParams);
            }
        }

        if (fonts) {
            TextView reset = UiKit.button(this, getString(R.string.calendar_font_reset),
                    palette.accent, palette.accentSoft, MiuixMetrics.BUTTON_RADIUS);
            reset.setTextSize(14f);
            reset.setEnabled(current != 0);
            reset.setAlpha(current == 0 ? 0.52f : 1f);
            reset.setOnClickListener(view -> {
                dialog.dismiss();
                selection.apply(0);
            });
            sheet.addView(reset, row(-1, UiKit.dp(this, 44f), 12));
        }

        dialog.setContentView(sheet);
        dialog.setOnDismissListener(ignored -> {
            if (appearancePicker == dialog) appearancePicker = null;
        });
        UiKit.configureDialog(dialog, palette);
        dialog.show();
        WindowMetrics metrics = getWindowManager().getCurrentWindowMetrics();
        Insets safe = metrics.getWindowInsets().getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        int width = metrics.getBounds().width() - safe.left - safe.right - UiKit.dp(this, 24f);
        int height = metrics.getBounds().height() - safe.top - safe.bottom - UiKit.dp(this, 24f);
        Window window = dialog.getWindow();
        if (window != null) {
            int dialogWidth = Math.min(width, UiKit.dp(this, 520f));
            sheet.measure(View.MeasureSpec.makeMeasureSpec(dialogWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST));
            window.setLayout(dialogWidth, Math.min(sheet.getMeasuredHeight(), height));
        }
    }

    private View appearanceCell(String label, int index, boolean selected, boolean fonts, UiKit.Palette palette) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(fonts ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        cell.setGravity(fonts ? Gravity.CENTER_VERTICAL : Gravity.CENTER_VERTICAL | Gravity.START);
        int horizontal = UiKit.dp(this, 8f);
        int vertical = UiKit.dp(this, 6f);
        cell.setPadding(horizontal, vertical, horizontal, vertical);
        cell.setMinimumHeight(UiKit.dp(this, fonts ? 69f : 53f));
        cell.setFocusable(true);
        cell.setSelected(selected);
        cell.setContentDescription(label);
        float radius = UiKit.dp(this, 14f);
        Drawable fill = selected ? UiKit.stroked(palette.accentSoft, palette.accent, radius, UiKit.dp(this, 1.5f))
                : UiKit.rounded(palette.surfaceSoft, radius);
        cell.setBackground(new RippleDrawable(ColorStateList.valueOf(Color.argb(38, 128, 128, 128)), fill, null));
        UiKit.bindPressMotion(cell);

        if (fonts) {
            TextView preview = UiKit.text(this, getString(R.string.appearance_font_sample), 20f, palette.primary, false);
            preview.setTypeface(ThemeManager.font(this, index, Typeface.DEFAULT, Typeface.NORMAL));
            preview.setSingleLine(true);
            preview.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            cell.addView(preview, row(-1, -2, 0));
        } else {
            View swatch = new View(this);
            int side = UiKit.dp(this, 21f);
            swatch.setBackground(UiKit.rounded(new UiKit.Palette(index, palette.night).accent, side / 2f));
            swatch.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams swatchParams = new LinearLayout.LayoutParams(side, side);
            swatchParams.setMarginEnd(UiKit.dp(this, 8f));
            cell.addView(swatch, swatchParams);
        }
        TextView name = UiKit.text(this, label, 13f, palette.primary, selected);
        name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        cell.addView(name, fonts ? row(-1, -2, 3) : new LinearLayout.LayoutParams(0, -2, 1f));
        return cell;
    }

    private void showChoicePicker(View view, String[] strArr, int i, Selection selection) {
        UiKit.Palette palette = new UiKit.Palette(this);
        if (this.selectionPopup != null) {
            this.selectionPopup.dismiss();
        }
        PopupWindow popupWindow = new PopupWindow(this);
        this.selectionPopup = popupWindow;
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(0, 0, 0, 0);
        linearLayout.setBackground(UiKit.rounded(palette.surface, UiKit.dp(this, MiuixMetrics.CARD_RADIUS)));
        ScrollView scrollView = new ScrollView(this);
        scrollView.setVerticalScrollBarEnabled(false);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(linearLayout2, new FrameLayout.LayoutParams(-1, -2));
        int i2 = 0;
        while (i2 < strArr.length) {
            int i3 = i2;
            boolean z2 = i2 == i;
            LinearLayout linearLayout3 = new LinearLayout(this);
            linearLayout3.setGravity(16);
            linearLayout3.setPadding(UiKit.dp(this, 20.0f), UiKit.dp(this, i2 == 0 ? 20.0f : 12.0f), UiKit.dp(this, 20.0f), UiKit.dp(this, i2 == strArr.length - 1 ? 20.0f : 12.0f));
            linearLayout3.setMinimumHeight(UiKit.dp(this, 56.0f));
            linearLayout3.setBackground(UiKit.interactiveBackground(this, palette.surface, MiuixMetrics.POPUP_RADIUS));
            linearLayout3.setClickable(true);
            linearLayout3.setFocusable(true);
            linearLayout3.setSelected(z2);
            linearLayout3.setContentDescription(strArr[i2] + (z2 ? ", đã chọn" : ""));
            UiKit.bindPressMotion(linearLayout3);
            TextView textViewText = UiKit.text(this, strArr[i2], 16.0f, z2 ? palette.accent : palette.primary, z2);
            textViewText.setImportantForAccessibility(2);
            linearLayout3.addView(textViewText, new LinearLayout.LayoutParams(0, -2, 1.0f));
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(R.drawable.ic_check);
            imageView.setImageTintList(ColorStateList.valueOf(palette.accent));
            imageView.setVisibility(z2 ? View.VISIBLE : View.INVISIBLE);
            imageView.setImportantForAccessibility(2);
            LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(UiKit.dp(this, 20.0f), UiKit.dp(this, MiuixMetrics.CARD_RADIUS));
            layoutParams2.setMarginStart(UiKit.dp(this, 12.0f));
            linearLayout3.addView(imageView, layoutParams2);
            linearLayout3.setOnClickListener(view3 -> {
                popupWindow.dismiss();
                if (i3 != i) {
                    selection.apply(i3);
                }
            });
            linearLayout2.addView(linearLayout3, new LinearLayout.LayoutParams(-1, -2));
            i2++;
        }
        int width = getWindow().getDecorView().getWidth();
        int height = getWindow().getDecorView().getHeight();
        if (width <= 0) {
            width = getResources().getDisplayMetrics().widthPixels;
        }
        if (height <= 0) {
            height = getResources().getDisplayMetrics().heightPixels;
        }
        int iMin = Math.min(UiKit.dp(this, 272.0f), width - UiKit.dp(this, 32.0f));
        linearLayout.addView(scrollView, new LinearLayout.LayoutParams(-1, -2));
        linearLayout.measure(View.MeasureSpec.makeMeasureSpec(iMin, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int iMin2 = Math.min(linearLayout.getMeasuredHeight(), (height * 3) / 5);
        popupWindow.setContentView(linearLayout);
        popupWindow.setWidth(iMin);
        popupWindow.setHeight(iMin2);
        popupWindow.setBackgroundDrawable(UiKit.rounded(palette.surface, UiKit.dp(this, MiuixMetrics.POPUP_RADIUS)));
        popupWindow.setElevation(UiKit.dp(this, 16.0f));
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setClippingEnabled(true);
        popupWindow.setInputMethodMode(2);
        if (ValueAnimator.areAnimatorsEnabled()) {
            popupWindow.setEnterTransition(new Fade(Fade.IN).setDuration(160L));
            popupWindow.setExitTransition(new Fade(Fade.OUT).setDuration(120L));
        } else {
            popupWindow.setEnterTransition(null);
            popupWindow.setExitTransition(null);
        }
        popupWindow.setOnDismissListener(() -> {
            if (this.selectionPopup == popupWindow) {
                this.selectionPopup = null;
            }
        });
        popupWindow.showAsDropDown(view, view.getWidth() - iMin, UiKit.dp(this, 4.0f), 3);
    }

    @Override
    protected void onDestroy() {
        BackupUiDialog.dismiss(this);
        SupportDialog.dismiss(this);
        if (appearancePicker != null) appearancePicker.dismiss();
        if (this.selectionPopup != null) {
            this.selectionPopup.dismiss();
        }
        super.onDestroy();
    }

    private LinearLayout.LayoutParams row(int i, int i2, int i3) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(i, i2);
        layoutParams.topMargin = UiKit.dp(this, i3);
        return layoutParams;
    }

    private String versionName() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName == null ? "" : packageInfo.versionName;
        } catch (Exception e) {
            return "";
        }
    }

    private final class BackButton extends View {
        private final Drawable icon;

        BackButton(int i, int i2) {
            super(SettingsActivity.this);
            this.icon = SettingsActivity.this.getDrawable(R.drawable.ic_back).mutate();
            this.icon.setTint(i2);
            setBackground(UiKit.interactiveBackground(SettingsActivity.this, i, MiuixMetrics.BUTTON_RADIUS));
            UiKit.bindPressMotion(this);
            setClickable(true);
            setFocusable(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float f = getResources().getDisplayMetrics().density;
            int iRound = Math.round(24.0f * f);
            int width = (getWidth() - iRound) / 2;
            int height = (getHeight() - iRound) / 2;
            this.icon.setBounds(width, height, width + iRound, height + iRound);
            this.icon.draw(canvas);
        }
    }
}
