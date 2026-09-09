package com.dtinh.lichviet;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


public final class NoteEditorDialog {
    private static final int NOTE_LIMIT = 240;


    private static final Locale VIETNAMESE_LOCALE = new Locale("vi", "VN");

    public interface Listener {
        void onNoteChanged();
    }

    private NoteEditorDialog() {
    }

    public static void show(final Context context, Calendar calendar, final Listener listener) {
        final Calendar calendar2 = (Calendar) calendar.clone();
        String str = NoteRepository.get(context, calendar2);
        UiKit.Palette palette = new UiKit.Palette(context);
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(1);
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setPadding(UiKit.dp(context, 12.0f), 0, UiKit.dp(context, 12.0f), UiKit.dp(context, 12.0f));
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(UiKit.dp(context, 22.0f), UiKit.dp(context, 10.0f), UiKit.dp(context, 22.0f), UiKit.dp(context, 20.0f));
        linearLayout.setBackground(UiKit.rounded(palette.surface, UiKit.dp(context, 28.0f)));
        frameLayout.addView(linearLayout, new FrameLayout.LayoutParams(-1, -2, 80));
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setGravity(17);
        View view = new View(context);
        view.setBackground(UiKit.rounded(palette.divider, UiKit.dp(context, 2.0f)));
        linearLayout2.addView(view, new LinearLayout.LayoutParams(UiKit.dp(context, 38.0f), UiKit.dp(context, 4.0f)));
        linearLayout.addView(linearLayout2, new LinearLayout.LayoutParams(-1, UiKit.dp(context, 18.0f)));
        linearLayout.addView(UiKit.text(context, context.getString(R.string.note_title), 20.0f, palette.primary, true), matchWrap());
        View viewText = UiKit.text(context, capitalize(new SimpleDateFormat("EEEE, d 'tháng' M, yyyy", VIETNAMESE_LOCALE).format(calendar2.getTime())), 12.0f, palette.secondary, true);
        LinearLayout.LayoutParams layoutParamsMatchWrap = matchWrap();
        layoutParamsMatchWrap.topMargin = UiKit.dp(context, 5.0f);
        linearLayout.addView(viewText, layoutParamsMatchWrap);
        LunarCalendar.LunarDate lunarDateFromSolar = LunarCalendar.fromSolar(calendar2.get(5), calendar2.get(2) + 1, calendar2.get(1));
        View viewText2 = UiKit.text(context, "Âm " + lunarDateFromSolar.day + "/" + lunarDateFromSolar.month + "  ·  " + LunarCalendar.yearCanChi(lunarDateFromSolar.year), 11.0f, palette.accent, true);
        LinearLayout.LayoutParams layoutParamsMatchWrap2 = matchWrap();
        layoutParamsMatchWrap2.topMargin = UiKit.dp(context, 4.0f);
        linearLayout.addView(viewText2, layoutParamsMatchWrap2);
        final EditText editText = new EditText(context);
        editText.setHint(R.string.note_hint);
        editText.setHintTextColor(palette.secondary);
        editText.setTextColor(palette.primary);
        editText.setTextSize(15.0f);
        editText.setGravity(8388659);
        editText.setInputType(147457);
        editText.setMinLines(4);
        editText.setMaxLines(6);
        editText.setPadding(UiKit.dp(context, 16.0f), UiKit.dp(context, 14.0f), UiKit.dp(context, 16.0f), UiKit.dp(context, 14.0f));
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(NOTE_LIMIT)});
        editText.setBackground(UiKit.stroked(palette.surfaceSoft, palette.divider, UiKit.dp(context, 16.0f), UiKit.dp(context, 1.0f)));
        editText.setText(str);
        editText.setSelection(editText.length());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, UiKit.dp(context, 118.0f));
        layoutParams.topMargin = UiKit.dp(context, 18.0f);
        linearLayout.addView(editText, layoutParams);
        final TextView textViewText = UiKit.text(context, editText.length() + "/" + NOTE_LIMIT, 10.0f, palette.secondary, false);
        textViewText.setGravity(8388613);
        LinearLayout.LayoutParams layoutParamsMatchWrap3 = matchWrap();
        layoutParamsMatchWrap3.topMargin = UiKit.dp(context, 6.0f);
        linearLayout.addView(textViewText, layoutParamsMatchWrap3);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                textViewText.setText(charSequence.length() + "/" + NoteEditorDialog.NOTE_LIMIT);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        LinearLayout linearLayout3 = new LinearLayout(context);
        linearLayout3.setGravity(16);
        LinearLayout.LayoutParams layoutParamsMatchWrap4 = matchWrap();
        layoutParamsMatchWrap4.topMargin = UiKit.dp(context, 15.0f);
        linearLayout.addView(linearLayout3, layoutParamsMatchWrap4);
        if (!str.isEmpty()) {
            TextView textViewAction = action(context, context.getString(R.string.note_delete), palette.danger, palette.surfaceSoft, false);
            textViewAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view2) {
                    NoteRepository.put(context, calendar2, "");
                    Listener listener2 = listener;
                    if (listener2 != null) {
                        listener2.onNoteChanged();
                    }
                    dialog.dismiss();
                }
            });
            linearLayout3.addView(textViewAction, new LinearLayout.LayoutParams(-2, UiKit.dp(context, 44.0f)));
        }
        linearLayout3.addView(new View(context), new LinearLayout.LayoutParams(0, 1, 1.0f));
        TextView textViewAction2 = action(context, context.getString(R.string.note_cancel), palette.secondary, 0, false);
        textViewAction2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                dialog.dismiss();
            }
        });
        linearLayout3.addView(textViewAction2, new LinearLayout.LayoutParams(-2, UiKit.dp(context, 44.0f)));
        TextView textViewAction3 = action(context, context.getString(R.string.note_save), -1, palette.accent, true);
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, UiKit.dp(context, 44.0f));
        layoutParams2.leftMargin = UiKit.dp(context, 8.0f);
        linearLayout3.addView(textViewAction3, layoutParams2);
        textViewAction3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                NoteRepository.put(context, calendar2, editText.getText().toString());
                Listener listener2 = listener;
                if (listener2 != null) {
                    listener2.onNoteChanged();
                }
                dialog.dismiss();
            }
        });
        dialog.setContentView(frameLayout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
            window.setGravity(80);
            window.addFlags(2);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.48f;
            window.setAttributes(attributes);
            window.setSoftInputMode(21);
            window.setWindowAnimations(0);
        }
        dialog.show();
        if (window != null) {
            window.setLayout(-1, -2);
        }
        linearLayout.setAlpha(0.0f);
        linearLayout.setTranslationY(UiKit.dp(context, 28.0f));
        linearLayout.animate().alpha(1.0f).translationY(0.0f).setDuration(220L).setInterpolator(new DecelerateInterpolator()).start();
        editText.requestFocus();
    }

    private static TextView action(Context context, String str, int i, int i2, boolean z) {
        TextView textViewText = UiKit.text(context, str, 12.0f, i, true);
        textViewText.setGravity(17);
        textViewText.setPadding(UiKit.dp(context, z ? 22.0f : 16.0f), 0, UiKit.dp(context, z ? 22.0f : 16.0f), 0);
        if (i2 != 0) {
            textViewText.setBackground(UiKit.rounded(i2, UiKit.dp(context, 15.0f)));
        }
        textViewText.setClickable(true);
        textViewText.setFocusable(true);
        return textViewText;
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private static String capitalize(String str) {
        if (str == null || str.length() == 0) {
            return "";
        }
        return str.substring(0, 1).toUpperCase(VIETNAMESE_LOCALE) + str.substring(1);
    }
}
