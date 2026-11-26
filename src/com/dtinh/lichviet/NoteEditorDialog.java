package com.dtinh.lichviet;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class NoteEditorDialog {
    private static final Locale VIETNAMESE_LOCALE = Locale.forLanguageTag("vi-VN");

    public interface Listener {
        void onNoteChanged();
    }

    private NoteEditorDialog() {}

    public static void show(Context context, Calendar calendar, Listener listener) {
        Calendar date = (Calendar) calendar.clone();
        String savedNote = NoteRepository.get(context, date);
        UiKit.Palette palette = new UiKit.Palette(context);
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout margin = new FrameLayout(context);
        margin.setPadding(UiKit.dp(context, 12f), 0, UiKit.dp(context, 12f), UiKit.dp(context, 12f));
        ScrollView scroll = new ScrollView(context);
        scroll.setClipToOutline(true);
        scroll.setBackground(UiKit.rounded(palette.surface, UiKit.dp(context, MiuixMetrics.DIALOG_RADIUS)));
        margin.addView(scroll, new FrameLayout.LayoutParams(-1, -2));

        LinearLayout sheet = new LinearLayout(context);
        sheet.setOrientation(LinearLayout.VERTICAL);
        int inset = UiKit.dp(context, 18f);
        sheet.setPadding(inset, inset, inset, UiKit.dp(context, 14f));
        scroll.addView(sheet, new FrameLayout.LayoutParams(-1, -2));

        TextView title = UiKit.text(context, context.getString(R.string.note_title), 20f, palette.primary, true);
        title.setAccessibilityHeading(true);
        sheet.addView(title, fullRow(context, 0f));
        String solar = new SimpleDateFormat("EEEE dd/MM/yyyy", VIETNAMESE_LOCALE).format(date.getTime());
        solar = solar.substring(0, 1).toUpperCase(VIETNAMESE_LOCALE) + solar.substring(1);
        LunarCalendar.LunarDate lunar = LunarCalendar.fromSolar(date.get(Calendar.DAY_OF_MONTH),
                date.get(Calendar.MONTH) + 1, date.get(Calendar.YEAR));
        String subtitle = context.getString(R.string.note_date_context, solar, lunar.day, lunar.month);
        if (lunar.leap) subtitle += context.getString(R.string.note_lunar_leap);
        sheet.addView(UiKit.text(context, subtitle, 13f, palette.secondary, false), fullRow(context, 6f));

        LinearLayout editor = new LinearLayout(context);
        editor.setOrientation(LinearLayout.VERTICAL);
        editor.setAddStatesFromChildren(true);
        editor.setBackground(UiKit.fieldBackground(context, palette));
        sheet.addView(editor, fullRow(context, 14f));

        EditText input = UiKit.field(context, palette, R.string.note_hint, false);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setBackground(null);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setTextSize(16f);
        input.setMinLines(4);
        input.setMaxLines(6);
        input.setPadding(UiKit.dp(context, 14f), UiKit.dp(context, 12f), UiKit.dp(context, 14f), UiKit.dp(context, 6f));
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(NoteRepository.MAX_LENGTH)});
        input.setText(savedNote);
        input.setSelection(input.length());
        editor.addView(input, fullRow(context, 0f));

        TextView counter = UiKit.text(context, count(context, input.length()), 11f, palette.secondary, false);
        counter.setGravity(Gravity.END);
        counter.setPadding(0, 0, UiKit.dp(context, 14f), UiKit.dp(context, 10f));
        editor.addView(counter, fullRow(context, 0f));
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable text) {}

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                counter.setText(count(context, text.length()));
            }
        });

        LinearLayout actions = new LinearLayout(context);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        sheet.addView(actions, fullRow(context, 12f));
        int actionHeight = UiKit.dp(context, 48f);
        if (!savedNote.isEmpty()) {
            TextView delete = action(context, R.string.note_delete, palette.danger, 0);
            delete.setOnClickListener(view -> {
                NoteRepository.put(context, date, "");
                if (listener != null) listener.onNoteChanged();
                dialog.dismiss();
            });
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(-2, actionHeight);
            deleteParams.setMarginEnd(UiKit.dp(context, 8f));
            actions.addView(delete, deleteParams);
        }

        TextView cancel = action(context, R.string.note_cancel, palette.primary, palette.surfaceSoft);
        cancel.setOnClickListener(view -> dialog.dismiss());
        actions.addView(cancel, new LinearLayout.LayoutParams(0, actionHeight, 1f));
        TextView save = action(context, R.string.note_save, palette.onAccent, palette.accent);
        save.setOnClickListener(view -> {
            NoteRepository.put(context, date, input.getText().toString());
            if (listener != null) listener.onNoteChanged();
            dialog.dismiss();
        });
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, actionHeight, 1f);
        saveParams.setMarginStart(UiKit.dp(context, 8f));
        actions.addView(save, saveParams);

        dialog.setContentView(margin);
        Window window = dialog.getWindow();
        UiKit.configureDialog(dialog, palette);
        if (window != null) {
            window.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        input.requestFocus();
        dialog.show();
        if (window != null) {
            int available = context.getResources().getDisplayMetrics().widthPixels;
            window.setLayout(Math.min(available, UiKit.dp(context, 540f)), -2);
        }
    }

    private static TextView action(Context context, int label, int color, int fill) {
        TextView button = UiKit.button(context, context.getString(label), color, fill, MiuixMetrics.BUTTON_RADIUS);
        button.setTextSize(15f);
        button.setPadding(UiKit.dp(context, 12f), 0, UiKit.dp(context, 12f), 0);
        button.setSingleLine(true);
        return button;
    }

    private static LinearLayout.LayoutParams fullRow(Context context, float topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = UiKit.dp(context, topMargin);
        return params;
    }

    private static String count(Context context, int length) {
        return context.getString(R.string.note_character_count, length, NoteRepository.MAX_LENGTH);
    }
}
