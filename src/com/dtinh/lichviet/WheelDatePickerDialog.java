package com.dtinh.lichviet;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class WheelDatePickerDialog extends Dialog {
    private final UiKit.Palette colors;
    private final Context context;
    private NumberPicker dayPicker;
    private final float density;
    private final Listener listener;
    private final YearListener yearListener;
    private final boolean yearOnly;
    private NumberPicker monthPicker;
    private TextView preview;
    private View cardView;
    private final Calendar value;
    private NumberPicker yearPicker;
    private static final Locale VIETNAMESE_LOCALE = new Locale("vi", "VN");
    private static final String[] MONTHS = {"Thg 1", "Thg 2", "Thg 3", "Thg 4", "Thg 5", "Thg 6", "Thg 7", "Thg 8", "Thg 9", "Thg 10", "Thg 11", "Thg 12"};

    public interface Listener {
        void onDateSelected(int i, int i2, int i3);
    }

    public interface YearListener {
        void onYearSelected(int i);
    }

    public WheelDatePickerDialog(Context context, Calendar calendar, Listener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
        this.yearListener = null;
        this.yearOnly = false;
        this.value = (Calendar) calendar.clone();
        this.density = context.getResources().getDisplayMetrics().density;
        this.colors = new UiKit.Palette(context);
    }

    public WheelDatePickerDialog(Context context, int i, YearListener yearListener) {
        super(context);
        this.context = context;
        this.listener = null;
        this.yearListener = yearListener;
        this.yearOnly = true;
        this.value = Calendar.getInstance();
        this.value.set(1, Math.max(1900, Math.min(2100, i)));
        this.density = context.getResources().getDisplayMetrics().density;
        this.colors = new UiKit.Palette(context);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(1);
        setCanceledOnTouchOutside(true);
        setContentView(buildContent());
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
            window.setDecorFitsSystemWindows(false);
            window.setStatusBarColor(0);
            window.setNavigationBarColor(0);
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
            window.addFlags(2);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.width = -1;
            attributes.height = -1;
            attributes.gravity = 17;
            attributes.dimAmount = this.colors.night ? 0.6f : 0.3f;
            window.setAttributes(attributes);
            View decorView = window.getDecorView();
            decorView.post(() -> {
                WindowInsetsController windowInsetsController = decorView.getWindowInsetsController();
                if (windowInsetsController == null) {
                    return;
                }
                windowInsetsController.setSystemBarsAppearance(this.colors.night ? 0 : 24, 24);
            });
        }
        UiKit.configureDialog(this, this.colors);
    }

    private View buildContent() {
        LinearLayout linearLayout = new LinearLayout(this.context);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(16.0f), dp(16.0f), dp(16.0f), dp(14.0f));
        linearLayout.setBackground(UiKit.rounded(UiKit.dialogSurface(this.colors), dp(MiuixMetrics.DIALOG_RADIUS)));
        this.cardView = linearLayout;
        this.preview = text("", 13.0f, this.colors.accent, 1);
        this.preview.setGravity(17);
        this.preview.setSingleLine(true);
        this.preview.setAutoSizeTextTypeUniformWithConfiguration(11, 13, 1, 2);
        LinearLayout.LayoutParams layoutParamsMatchWrap = matchWrap();
        layoutParamsMatchWrap.topMargin = 0;
        if (!this.yearOnly) {
            linearLayout.addView(this.preview, layoutParamsMatchWrap);
        }
        View view = new View(this.context);
        view.setBackgroundColor(this.colors.divider);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(1.0f));
        layoutParams.topMargin = dp(10.0f);
        layoutParams.bottomMargin = dp(8.0f);
        if (!this.yearOnly) {
            linearLayout.addView(view, layoutParams);
        }
        LinearLayout linearLayout2 = new LinearLayout(this.context);
        linearLayout2.setOrientation(0);
        if (this.yearOnly) {
            linearLayout2.addView(label("Năm"), weighted());
        } else {
            linearLayout2.addView(label("Ngày"), weighted());
            linearLayout2.addView(label("Tháng"), weighted());
            linearLayout2.addView(label("Năm"), weighted());
        }
        linearLayout.addView(linearLayout2, matchWrap());
        LinearLayout linearLayout3 = new LinearLayout(this.context);
        linearLayout3.setGravity(17);
        linearLayout3.setOrientation(0);
        float fMin = Math.min(96.0f, Math.max(80.0f, (this.context.getResources().getDisplayMetrics().heightPixels / this.density) * 0.28f));
        this.yearPicker = picker();
        if (this.yearOnly) {
            linearLayout3.addView(pickerColumn(this.yearPicker), weightedHeight(fMin));
        } else {
            this.dayPicker = picker();
            this.monthPicker = picker();
            linearLayout3.addView(pickerColumn(this.dayPicker), weightedHeight(fMin));
            linearLayout3.addView(pickerColumn(this.monthPicker), weightedHeight(fMin));
            linearLayout3.addView(pickerColumn(this.yearPicker), weightedHeight(fMin));
        }
        linearLayout.addView(linearLayout3, matchWrap());
        configurePickers();
        LinearLayout linearLayout4 = new LinearLayout(this.context);
        linearLayout4.setOrientation(0);
        linearLayout4.setGravity(17);
        LinearLayout.LayoutParams layoutParamsMatchWrap2 = matchWrap();
        layoutParamsMatchWrap2.topMargin = dp(12.0f);
        TextView textViewActionButton = actionButton("Hủy", this.colors.primary, this.colors.surfaceSoft);
        textViewActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                WheelDatePickerDialog.this.dismiss();
            }
        });
        TextView textViewActionButton2 = actionButton("Xong", this.colors.onAccent, this.colors.accent);
        textViewActionButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                if (WheelDatePickerDialog.this.yearOnly) {
                    WheelDatePickerDialog.this.yearListener.onYearSelected(WheelDatePickerDialog.this.yearPicker.getValue());
                } else {
                    WheelDatePickerDialog.this.listener.onDateSelected(WheelDatePickerDialog.this.yearPicker.getValue(), WheelDatePickerDialog.this.monthPicker.getValue(), WheelDatePickerDialog.this.dayPicker.getValue());
                }
                WheelDatePickerDialog.this.dismiss();
            }
        });
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(Math.min(dp(88.0f), (this.context.getResources().getDisplayMetrics().widthPixels - dp(72.0f)) / 2), dp(32.0f));
        layoutParams2.rightMargin = dp(4.0f);
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(Math.min(dp(88.0f), (this.context.getResources().getDisplayMetrics().widthPixels - dp(72.0f)) / 2), dp(32.0f));
        layoutParams3.leftMargin = dp(4.0f);
        linearLayout4.addView(textViewActionButton, layoutParams2);
        linearLayout4.addView(textViewActionButton2, layoutParams3);
        linearLayout.addView(linearLayout4, layoutParamsMatchWrap2);
        updatePreview();
        FrameLayout frameLayout = new FrameLayout(this.context);
        frameLayout.setBackgroundColor(0);
        frameLayout.setClickable(true);
        frameLayout.setOnClickListener(view2 -> {
            dismiss();
        });
        linearLayout.setClickable(true);
        linearLayout.setOnClickListener(view3 -> {
        });
        frameLayout.addView(linearLayout, new FrameLayout.LayoutParams(Math.min(dp(280.0f), this.context.getResources().getDisplayMetrics().widthPixels - dp(32.0f)), -2, 17));
        return frameLayout;
    }

    private void configurePickers() {
        this.yearPicker.setMinValue(1900);
        this.yearPicker.setMaxValue(2100);
        this.yearPicker.setWrapSelectorWheel(false);
        this.yearPicker.setValue(this.value.get(1));
        if (this.yearOnly) {
            this.yearPicker.setOnValueChangedListener((numberPicker, i, i2) -> {
                updatePreview();
            });
            return;
        }
        this.dayPicker.setMinValue(1);
        this.monthPicker.setMinValue(0);
        this.monthPicker.setMaxValue(11);
        this.monthPicker.setDisplayedValues(MONTHS);
        this.monthPicker.setValue(this.value.get(2));
        refreshDayMaximum(this.value.get(5));
        NumberPicker.OnValueChangeListener onValueChangeListener = (numberPicker2, i3, i4) -> {
            if (numberPicker2 == this.monthPicker || numberPicker2 == this.yearPicker) {
                refreshDayMaximum(this.dayPicker.getValue());
            }
            updatePreview();
        };
        this.dayPicker.setOnValueChangedListener(onValueChangeListener);
        this.monthPicker.setOnValueChangedListener(onValueChangeListener);
        this.yearPicker.setOnValueChangedListener(onValueChangeListener);
    }

    public void refreshDayMaximum(int i) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(this.yearPicker.getValue(), this.monthPicker.getValue(), 1);
        int actualMaximum = calendar.getActualMaximum(5);
        this.dayPicker.setMaxValue(actualMaximum);
        this.dayPicker.setValue(Math.min(i, actualMaximum));
    }

    public void updatePreview() {
        if (this.preview == null || this.yearPicker == null) {
            return;
        }
        if (this.yearOnly) {
            this.preview.setText("Năm " + this.yearPicker.getValue());
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(this.yearPicker.getValue(), this.monthPicker.getValue(), this.dayPicker.getValue());
        Locale locale = VIETNAMESE_LOCALE;
        String str = new SimpleDateFormat("EEEE", locale).format(calendar.getTime());
        if (str.length() > 0) {
            str = str.substring(0, 1).toUpperCase(locale) + str.substring(1);
        }
        this.preview.setText(str + ", " + this.dayPicker.getValue() + " tháng " + (this.monthPicker.getValue() + 1) + ", " + this.yearPicker.getValue());
    }

    private NumberPicker picker() {
        NumberPicker numberPicker = new NumberPicker(this.context);
        numberPicker.setMinimumWidth(0);
        numberPicker.setMinimumHeight(0);
        numberPicker.setDescendantFocusability(393216);
        numberPicker.setWrapSelectorWheel(true);
        numberPicker.setBackgroundColor(0);
        numberPicker.setTextColor(this.colors.primary);
        numberPicker.setTextSize(TypedValue.applyDimension(2, 16.0f, this.context.getResources().getDisplayMetrics()));
        numberPicker.setSelectionDividerHeight(0);
        tintChildren(numberPicker);
        return numberPicker;
    }

    private View pickerColumn(NumberPicker numberPicker) {
        FrameLayout frameLayout = new FrameLayout(this.context);
        frameLayout.setClipChildren(true);
        frameLayout.setClipToPadding(true);
        frameLayout.addView(numberPicker, new FrameLayout.LayoutParams(-1, -1));
        return frameLayout;
    }

    private void tintChildren(View view) {
        if (view instanceof EditText) {
            EditText editText = (EditText) view;
            editText.setTextColor(this.colors.primary);
            editText.setTextSize(16.0f);
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                tintChildren(viewGroup.getChildAt(i));
            }
        }
    }

    private TextView label(String str) {
        TextView textViewText = text(str, 11.0f, this.colors.secondary, 1);
        textViewText.setGravity(17);
        return textViewText;
    }

    private TextView actionButton(String str, int i, int i2) {
        TextView textViewButton = UiKit.button(this.context, str, i, i2, 12.0f);
        textViewButton.setMinHeight(dp(32.0f));
        textViewButton.setTextSize(12.0f);
        textViewButton.setSingleLine(true);
        return textViewButton;
    }

    private TextView text(String str, float f, int i, int i2) {
        TextView textView = new TextView(this.context);
        textView.setText(str);
        textView.setTextSize(f);
        textView.setTextColor(i);
        textView.setTypeface(Typeface.create(Typeface.DEFAULT, i2));
        textView.setIncludeFontPadding(false);
        return textView;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, -2, 1.0f);
    }

    private LinearLayout.LayoutParams weightedHeight(float f) {
        return new LinearLayout.LayoutParams(0, dp(f), 1.0f);
    }

    private int dp(float f) {
        return Math.round(f * this.density);
    }
}
