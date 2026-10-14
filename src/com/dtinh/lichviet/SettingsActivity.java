package com.dtinh.lichviet;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionSet;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class SettingsActivity extends ThemedActivity {
    private static final int REQUEST_BACKGROUND_IMAGE = 4102;
    private PopupWindow selectionPopup;

    private interface Selection {
        void apply(int i);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        UiKit.Palette palette = new UiKit.Palette(this);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackground(UiKit.pageBackground(palette));
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
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
        TextView textViewText4 = UiKit.text(this, AppColors.NAMES[ThemeManager.getColorScheme(this)], 14.0f, palette.secondary, false);
        setPreferenceArrow(textViewText4, palette.secondary);
        preferenceRow(linearLayoutPreferenceGroup, "Màu sắc", textViewText4, palette).setOnClickListener(view4 -> {
            showColorPicker(textViewText4);
        });
        TextView textViewButton = UiKit.button(this, BackgroundImageManager.hasBackground(this) ? "Đổi ảnh" : "Chọn ảnh", palette.accent, palette.accentSoft, 16.0f);
        textViewButton.setOnClickListener(view5 -> {
            openBackgroundPicker();
        });
        preferenceRow(linearLayoutPreferenceGroup, "Ảnh nền lịch", textViewButton, palette);
        if (BackgroundImageManager.hasBackground(this)) {
            MiuixSwitch miuixSwitch = new MiuixSwitch(this);
            miuixSwitch.setChecked(BackgroundImageManager.isFrostedEnabled(this));
            miuixSwitch.setContentDescription("Nền mờ");
            miuixSwitch.setOnCheckedChangeListener((compoundButton, z) -> {
                BackgroundImageManager.setFrostedEnabled(this, z);
            });
            preferenceRow(linearLayoutPreferenceGroup, "Nền mờ", miuixSwitch, palette);
            TextView textViewButton2 = UiKit.button(this, "Xóa ảnh", palette.danger, palette.surfaceSoft, 16.0f);
            textViewButton2.setOnClickListener(view6 -> {
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
        UiKit.animateChildren(linearLayout);
    }

    private LinearLayout preferenceGroup(LinearLayout linearLayout, String str, UiKit.Palette palette) {
        TextView textViewText = UiKit.text(this, str, 13.0f, palette.secondary, true);
        textViewText.setPadding(UiKit.dp(this, 12.0f), 0, 0, 0);
        linearLayout.addView(textViewText, row(-1, -2, 20));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
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
        startActivityForResult(new Intent("android.intent.action.OPEN_DOCUMENT").addCategory("android.intent.category.OPENABLE").setType("image/*").addFlags(65), REQUEST_BACKGROUND_IMAGE);
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i != REQUEST_BACKGROUND_IMAGE || i2 != -1 || intent == null || intent.getData() == null) {
            return;
        }
        if (BackgroundImageManager.setBackground(this, intent.getData(), intent.getFlags())) {
            recreate();
        } else {
            Toast.makeText(this, "Không thể đọc ảnh đã chọn", 0).show();
        }
    }

    private void showColorPicker(View view) {
        showSelectionPicker(view, AppColors.NAMES, ThemeManager.getColorScheme(this), true, i -> {
            ThemeManager.setColorScheme(this, i);
            recreate();
        });
    }

    private void showChoicePicker(View view, String[] strArr, int i, Selection selection) {
        showSelectionPicker(view, strArr, i, false, selection);
    }

    private void showSelectionPicker(View view, String[] strArr, int i, boolean z, Selection selection) {
        UiKit.Palette palette = new UiKit.Palette(this);
        if (this.selectionPopup != null) {
            this.selectionPopup.dismiss();
        }
        PopupWindow popupWindow = new PopupWindow(this);
        this.selectionPopup = popupWindow;
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(0, 0, 0, 0);
        linearLayout.setBackground(UiKit.rounded(palette.surface, UiKit.dp(this, MiuixMetrics.CARD_RADIUS)));
        ScrollView scrollView = new ScrollView(this);
        scrollView.setVerticalScrollBarEnabled(false);
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(1);
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
            if (z) {
                UiKit.Palette palette2 = new UiKit.Palette(i2, palette.night);
                View view2 = new View(this);
                view2.setImportantForAccessibility(2);
                view2.setBackground(UiKit.rounded(palette2.accent, UiKit.dp(this, 10.0f)));
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(UiKit.dp(this, 20.0f), UiKit.dp(this, MiuixMetrics.CARD_RADIUS));
                layoutParams.setMarginEnd(UiKit.dp(this, 12.0f));
                linearLayout3.addView(view2, layoutParams);
            }
            TextView textViewText = UiKit.text(this, strArr[i2], 16.0f, z2 ? palette.accent : palette.primary, z2);
            textViewText.setImportantForAccessibility(2);
            linearLayout3.addView(textViewText, new LinearLayout.LayoutParams(0, -2, 1.0f));
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(R.drawable.ic_check);
            imageView.setImageTintList(ColorStateList.valueOf(palette.accent));
            imageView.setVisibility(z2 ? 0 : 4);
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
        int iMin = Math.min(UiKit.dp(this, z ? 240.0f : 272.0f), width - UiKit.dp(this, 32.0f));
        linearLayout.addView(scrollView, new LinearLayout.LayoutParams(-1, -2));
        linearLayout.measure(View.MeasureSpec.makeMeasureSpec(iMin, 1073741824), View.MeasureSpec.makeMeasureSpec(0, 0));
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
            TransitionSet transitionSet = new TransitionSet();
            transitionSet.addTransition(new Fade(1));
            transitionSet.addTransition(new ChangeBounds());
            transitionSet.setDuration(200L);
            popupWindow.setEnterTransition(transitionSet);
            popupWindow.setExitTransition(new Fade(2).setDuration(150L));
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
