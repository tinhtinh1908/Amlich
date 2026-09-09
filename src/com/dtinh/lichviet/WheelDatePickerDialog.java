package com.dtinh.lichviet;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
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
        void onDateSelected(int year, int month, int day);
    }

    public interface YearListener {
        void onYearSelected(int year);
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

    public WheelDatePickerDialog(Context context, int year, YearListener listener) {
        super(context);
        this.context = context;
        this.listener = null;
        this.yearListener = listener;
        this.yearOnly = true;
        this.value = Calendar.getInstance();
        this.value.set(Calendar.YEAR, Math.max(1900, Math.min(2100, year)));
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
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.width = WindowManager.LayoutParams.MATCH_PARENT;
            attributes.height = WindowManager.LayoutParams.MATCH_PARENT;
            attributes.gravity = Gravity.CENTER;
            attributes.dimAmount = this.colors.night ? 0.68f : 0.42f;
            window.setAttributes(attributes);
            View decor = window.getDecorView();
            decor.post(() -> {
                WindowInsetsController controller = decor.getWindowInsetsController();
                if (controller == null) return;
                int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(this.colors.night ? 0 : light, light);
            });
        }
        if (this.cardView != null) UiKit.animateIn(this.cardView, 12.0f);
    }

    private View buildContent() {
        LinearLayout linearLayout = new LinearLayout(this.context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(dp(22.0f), dp(21.0f), dp(22.0f), dp(18.0f));
        linearLayout.setBackground(UiKit.rounded(
                UiKit.dialogSurface(this.colors), dp(28.0f)));
        this.cardView = linearLayout;
        linearLayout.addView(text(this.yearOnly ? "Chọn năm" : "Chọn ngày", 22.0f, this.colors.primary, 1), matchWrap());
        this.preview = text("", 13.0f, this.colors.accent, 1);
        LinearLayout.LayoutParams layoutParamsMatchWrap = matchWrap();
        layoutParamsMatchWrap.topMargin = dp(6.0f);
        linearLayout.addView(this.preview, layoutParamsMatchWrap);
        View view = new View(this.context);
        view.setBackgroundColor(this.colors.divider);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, dp(1.0f));
        layoutParams.topMargin = dp(18.0f);
        layoutParams.bottomMargin = dp(10.0f);
        linearLayout.addView(view, layoutParams);
        LinearLayout linearLayout2 = new LinearLayout(this.context);
        linearLayout2.setOrientation(LinearLayout.HORIZONTAL);
        if (this.yearOnly) {
            linearLayout2.addView(label("NĂM"), weighted());
        } else {
            linearLayout2.addView(label("NGÀY"), weighted());
            linearLayout2.addView(label("THÁNG"), weighted());
            linearLayout2.addView(label("NĂM"), weighted());
        }
        linearLayout.addView(linearLayout2, matchWrap());
        LinearLayout linearLayout3 = new LinearLayout(this.context);
        linearLayout3.setGravity(Gravity.CENTER);
        linearLayout3.setOrientation(LinearLayout.HORIZONTAL);
        this.yearPicker = picker();
        if (this.yearOnly) {
            linearLayout3.addView(this.yearPicker, weightedHeight(150.0f));
        } else {
            this.dayPicker = picker();
            this.monthPicker = picker();
            linearLayout3.addView(this.dayPicker, weightedHeight(150.0f));
            linearLayout3.addView(this.monthPicker, weightedHeight(150.0f));
            linearLayout3.addView(this.yearPicker, weightedHeight(150.0f));
        }
        linearLayout.addView(linearLayout3, matchWrap());
        configurePickers();
        LinearLayout linearLayout4 = new LinearLayout(this.context);
        linearLayout4.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams layoutParamsMatchWrap2 = matchWrap();
        layoutParamsMatchWrap2.topMargin = dp(12.0f);
        TextView textViewActionButton = actionButton("HỦY", this.colors.primary, this.colors.surfaceSoft);
        textViewActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                WheelDatePickerDialog.this.dismiss();
            }
        });
        TextView textViewActionButton2 = actionButton("XONG", -1, this.colors.accent);
        textViewActionButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                if (WheelDatePickerDialog.this.yearOnly) {
                    WheelDatePickerDialog.this.yearListener.onYearSelected(
                            WheelDatePickerDialog.this.yearPicker.getValue());
                } else {
                    WheelDatePickerDialog.this.listener.onDateSelected(
                            WheelDatePickerDialog.this.yearPicker.getValue(),
                            WheelDatePickerDialog.this.monthPicker.getValue(),
                            WheelDatePickerDialog.this.dayPicker.getValue());
                }
                WheelDatePickerDialog.this.dismiss();
            }
        });
        LinearLayout.LayoutParams layoutParamsWeightedHeight = weightedHeight(46.0f);
        layoutParamsWeightedHeight.rightMargin = dp(7.0f);
        LinearLayout.LayoutParams layoutParamsWeightedHeight2 = weightedHeight(46.0f);
        layoutParamsWeightedHeight2.leftMargin = dp(7.0f);
        linearLayout4.addView(textViewActionButton, layoutParamsWeightedHeight);
        linearLayout4.addView(textViewActionButton2, layoutParamsWeightedHeight2);
        linearLayout.addView(linearLayout4, layoutParamsMatchWrap2);
        updatePreview();
        FrameLayout overlay = new FrameLayout(this.context);
        overlay.setBackgroundColor(Color.TRANSPARENT);
        overlay.setClickable(true);
        overlay.setOnClickListener(view1 -> dismiss());
        linearLayout.setClickable(true);
        linearLayout.setOnClickListener(view1 -> { });
        int cardWidth = Math.min(dp(420.0f),
                this.context.getResources().getDisplayMetrics().widthPixels - dp(28.0f));
        FrameLayout.LayoutParams card = new FrameLayout.LayoutParams(
                cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        overlay.addView(linearLayout, card);
        return overlay;
    }

    private void configurePickers() {
        this.yearPicker.setMinValue(1900);
        this.yearPicker.setMaxValue(2100);
        this.yearPicker.setWrapSelectorWheel(false);
        this.yearPicker.setValue(this.value.get(Calendar.YEAR));
        if (this.yearOnly) {
            this.yearPicker.setOnValueChangedListener((picker, oldValue, newValue) -> updatePreview());
            return;
        }
        this.dayPicker.setMinValue(1);
        this.monthPicker.setMinValue(0);
        this.monthPicker.setMaxValue(11);
        this.monthPicker.setDisplayedValues(MONTHS);
        this.monthPicker.setValue(this.value.get(Calendar.MONTH));
        refreshDayMaximum(this.value.get(Calendar.DAY_OF_MONTH));
        NumberPicker.OnValueChangeListener changeListener = (picker, oldValue, newValue) -> {
            if (picker == this.monthPicker || picker == this.yearPicker) {
                refreshDayMaximum(this.dayPicker.getValue());
            }
            updatePreview();
        };
        this.dayPicker.setOnValueChangedListener(changeListener);
        this.monthPicker.setOnValueChangedListener(changeListener);
        this.yearPicker.setOnValueChangedListener(changeListener);
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
        if (this.preview == null || this.yearPicker == null) return;
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
        numberPicker.setDescendantFocusability(393216);
        numberPicker.setWrapSelectorWheel(true);
        numberPicker.setBackgroundColor(0);
        numberPicker.setTextColor(this.colors.primary);
        // HyperOS draws the platform divider in solid black in dark mode.
        // The selected row remains clear without that device-specific divider.
        numberPicker.setSelectionDividerHeight(0);
        tintChildren(numberPicker);
        return numberPicker;
    }

    private void tintChildren(View view) {
        if (view instanceof EditText) {
            EditText editText = (EditText) view;
            editText.setTextColor(this.colors.primary);
            editText.setTextSize(18.0f);
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                tintChildren(viewGroup.getChildAt(i));
            }
        }
    }

    private TextView label(String str) {
        TextView textViewText = text(str, 9.5f, this.colors.secondary, 1);
        textViewText.setGravity(17);
        return textViewText;
    }

    private TextView actionButton(String str, int i, int i2) {
        TextView textViewText = text(str, 12.0f, i, 1);
        textViewText.setGravity(17);
        textViewText.setBackground(UiKit.rounded(i2, dp(16.0f)));
        textViewText.setClickable(true);
        textViewText.setFocusable(true);
        return textViewText;
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
