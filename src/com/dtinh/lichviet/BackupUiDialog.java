package com.dtinh.lichviet;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.Editable;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** Native backup sheets; encryption and note merge remain in XiaomiNotesBackup. */
final class BackupUiDialog {
    interface PasswordListener { void confirm(char[] password); }
    private static final WeakHashMap<Activity, WeakReference<Dialog>> OPEN = new WeakHashMap<>();

    static void dismiss(Activity activity) {
        WeakReference<Dialog> reference = OPEN.remove(activity);
        Dialog dialog = reference == null ? null : reference.get();
        if (dialog != null) dialog.dismiss();
    }

    private static void unregister(Activity activity, Dialog dialog) {
        WeakReference<Dialog> current = OPEN.get(activity);
        if (current != null && current.get() == dialog) OPEN.remove(activity);
    }

    static void password(Activity activity, boolean creating, PasswordListener listener) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        UiKit.Palette colors = new UiKit.Palette(activity);
        Sheet sheet = new Sheet(activity, colors);
        sheet.heading(activity.getString(creating ? R.string.backup_dialog_title : R.string.restore_dialog_title));
        sheet.description(activity.getString(creating ? R.string.backup_compact_message : R.string.restore_compact_message));

        LinearLayout labelRow = new LinearLayout(activity);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = UiKit.text(activity, activity.getString(R.string.backup_password), 12, colors.secondary, false);
        labelRow.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        TextView visibility = UiKit.text(activity, "Hiện", 12, colors.accent, false);
        visibility.setGravity(Gravity.CENTER);
        visibility.setBackground(UiKit.interactiveBackground(activity, 0, 8));
        visibility.setContentDescription("Hiện mật khẩu");
        UiKit.bindPressMotion(visibility);
        labelRow.addView(visibility, new LinearLayout.LayoutParams(dp(activity, 44), dp(activity, 28)));
        sheet.add(labelRow, 12);
        EditText password = field(activity, colors, R.string.backup_password_hint);
        password.setImeOptions(creating ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE);
        sheet.add(password, 4);
        EditText repeated;
        if (creating) {
            sheet.add(UiKit.text(activity, activity.getString(R.string.backup_password_confirm), 12, colors.secondary, false), 12);
            repeated = field(activity, colors, R.string.backup_password_confirm_hint);
            repeated.setImeOptions(EditorInfo.IME_ACTION_DONE);
            sheet.add(repeated, 6);
            password.setNextFocusForwardId(repeated.getId());
        } else repeated = null;

        TextView error = UiKit.text(activity, "", 12, colors.danger, false);
        error.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);
        error.setVisibility(android.view.View.GONE);
        sheet.add(error, 8);
        TextWatcher clearError = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { error.setVisibility(android.view.View.GONE); }
            public void afterTextChanged(Editable s) {}
        };
        password.addTextChangedListener(clearError);
        if (repeated != null) repeated.addTextChangedListener(clearError);
        visibility.setOnClickListener(v -> {
            boolean show = password.getTransformationMethod() != null;
            transform(password, show);
            if (repeated != null) transform(repeated, show);
            visibility.setText(show ? "Ẩn" : "Hiện");
            visibility.setContentDescription(show ? "Ẩn mật khẩu" : "Hiện mật khẩu");
        });
        Runnable submit = () -> {
            EditText invalid = null;
            int message = 0;
            if (password.length() < 6) { invalid = password; message = R.string.backup_password_short; }
            else if (repeated != null && !TextUtils.equals(password.getText(), repeated.getText())) {
                invalid = repeated; message = R.string.backup_password_mismatch;
            }
            if (invalid != null) {
                error.setText(message); error.setVisibility(android.view.View.VISIBLE);
                invalid.requestFocus(); UiKit.animateIn(error, 3);
                return;
            }
            char[] secret = new char[password.length()];
            password.getText().getChars(0, secret.length, secret, 0);
            sheet.close();
            listener.confirm(secret);
        };
        sheet.actions(activity.getString(creating ? R.string.backup_create : R.string.restore_decrypt), submit);
        EditText last = repeated == null ? password : repeated;
        last.setOnEditorActionListener((v, action, event) -> {
            if (action != EditorInfo.IME_ACTION_DONE) return false;
            submit.run(); return true;
        });
        sheet.dialog.setOnDismissListener(dialog -> {
            password.getText().clear();
            if (repeated != null) repeated.getText().clear();
            unregister(activity, sheet.dialog);
        });
        sheet.show();
    }

    static void restoreConfirmation(Activity activity, int count, Runnable apply) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        Sheet sheet = new Sheet(activity, new UiKit.Palette(activity));
        sheet.heading(activity.getString(R.string.restore_confirm_title));
        sheet.description(activity.getString(R.string.restore_confirm_message, count));
        sheet.actions(activity.getString(R.string.restore_apply), () -> { sheet.close(); apply.run(); });
        sheet.show();
    }

    private static EditText field(Activity activity, UiKit.Palette colors, int hint) {
        EditText field = UiKit.field(activity, colors, hint, true);
        field.setId(android.view.View.generateViewId());
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setTransformationMethod(PasswordTransformationMethod.getInstance());
        // setInputType resets the Android password typeface; set this afterwards.
        field.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        field.setTextSize(16);
        field.setSelectAllOnFocus(false);
        field.setPadding(dp(activity, 14), dp(activity, 10), dp(activity, 14), dp(activity, 10));
        field.setMinimumHeight(dp(activity, 44));
        return field;
    }
    private static void transform(EditText field, boolean show) {
        int selection = field.getSelectionStart();
        field.setTransformationMethod(show ? null : PasswordTransformationMethod.getInstance());
        field.setSelection(Math.max(0, Math.min(selection, field.length())));
    }
    private static int dp(Activity activity, float value) { return UiKit.dp(activity, value); }

    private static final class Sheet {
        final Activity activity;
        final UiKit.Palette colors;
        final Dialog dialog;
        final LinearLayout content;
        final FrameLayout root;
        Sheet(Activity activity, UiKit.Palette colors) {
            dismiss(activity);
            this.activity = activity; this.colors = colors;
            dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setOwnerActivity(activity);
            root = new FrameLayout(activity);
            root.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));
            ScrollView scroll = new ScrollView(activity);
            scroll.setVerticalScrollBarEnabled(false);
            scroll.setBackground(UiKit.rounded(colors.surface, dp(activity, MiuixMetrics.DIALOG_RADIUS)));
            content = new LinearLayout(activity);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 20));
            scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
            int available = activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 24);
            int width = Math.max(1, Math.min(available, dp(activity, 420)));
            root.addView(scroll, new FrameLayout.LayoutParams(width, -2, Gravity.CENTER_HORIZONTAL));
            dialog.setContentView(root);
            dialog.setOnDismissListener(d -> unregister(activity, dialog));
        }
        void heading(String title) { add(UiKit.text(activity, title, 20, colors.primary, true), 0); }
        void description(String message) {
            TextView view = UiKit.text(activity, message, 13, colors.secondary, false);
            view.setLineSpacing(dp(activity, 2), 1);
            add(view, 8);
        }
        void add(android.view.View view, int topMargin) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.topMargin = dp(activity, topMargin);
            content.addView(view, params);
        }
        void actions(String positive, Runnable action) {
            LinearLayout row = new LinearLayout(activity);
            TextView cancel = UiKit.button(activity, activity.getString(R.string.note_cancel), colors.primary, colors.surfaceSoft, 16);
            TextView confirm = UiKit.button(activity, positive, colors.onAccent, colors.accent, 16);
            cancel.setTextSize(14); confirm.setTextSize(14);
            cancel.setOnClickListener(v -> close());
            confirm.setOnClickListener(v -> action.run());
            LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(activity, 40), 1);
            left.setMarginEnd(dp(activity, 8));
            row.addView(cancel, left);
            row.addView(confirm, new LinearLayout.LayoutParams(0, dp(activity, 40), 1));
            add(row, 16);
        }
        void close() {
            InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(root.getWindowToken(), 0);
            dialog.dismiss();
        }
        void show() {
            UiKit.configureDialog(dialog, colors);
            Window window = dialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.BOTTOM);
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            }
            dialog.show();
            if (window != null) window.setLayout(-1, -2);
            OPEN.put(activity, new WeakReference<>(dialog));
        }
    }
    private BackupUiDialog() {}
}
