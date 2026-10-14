package com.dtinh.lichviet;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import android.view.Gravity;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** Donation details mirrored from the author's public website; no network lookup. */
final class SupportDialog {
    private static final WeakHashMap<Activity, WeakReference<Dialog>> OPEN = new WeakHashMap<>();

    static void dismiss(Activity activity) {
        WeakReference<Dialog> reference = OPEN.remove(activity);
        Dialog dialog = reference == null ? null : reference.get();
        if (dialog != null) dialog.dismiss();
    }

    static void show(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        dismiss(activity);
        UiKit.Palette colors = new UiKit.Palette(activity);
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setOwnerActivity(activity);
        ScrollView scroll = new ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackground(UiKit.rounded(colors.surface, UiKit.dp(activity, MiuixMetrics.DIALOG_RADIUS)));
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int inset = UiKit.dp(activity, 20);
        content.setPadding(inset, inset, inset, inset);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        TextView heading = UiKit.text(activity, activity.getString(R.string.support_title), 20, colors.primary, true);
        heading.setGravity(Gravity.CENTER);
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(new android.view.View(activity), new LinearLayout.LayoutParams(UiKit.dp(activity, 28), 1));
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView dismiss = UiKit.button(activity, "", colors.secondary, 0, 8);
        dismiss.setPadding(0, 0, 0, 0);
        UiKit.setIcon(dismiss, R.drawable.ic_close, colors.secondary, 16);
        dismiss.setContentDescription(activity.getString(R.string.support_close));
        dismiss.setOnClickListener(view -> dialog.dismiss());
        header.addView(dismiss, new LinearLayout.LayoutParams(UiKit.dp(activity, 28), UiKit.dp(activity, 28)));
        add(content, header, 0);
        TextView hint = UiKit.text(activity, activity.getString(R.string.support_qr_hint), 12, colors.secondary, false);
        hint.setGravity(Gravity.CENTER);
        add(content, hint, 6);

        ImageView qr = new ImageView(activity);
        BitmapDrawable image = (BitmapDrawable) activity.getDrawable(R.drawable.donate_vietqr).mutate();
        image.setFilterBitmap(true);
        qr.setImageDrawable(image);
        qr.setScaleType(ImageView.ScaleType.FIT_CENTER);
        qr.setBackground(UiKit.rounded(android.graphics.Color.WHITE, UiKit.dp(activity, 16)));
        qr.setClipToOutline(true);
        qr.setContentDescription(activity.getString(R.string.support_qr_description));
        int widthPixels = activity.getResources().getDisplayMetrics().widthPixels;
        int qrSize = Math.max(1, Math.min(UiKit.dp(activity, 224), widthPixels - UiKit.dp(activity, 88)));
        LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(qrSize, qrSize);
        qrParams.gravity = Gravity.CENTER_HORIZONTAL;
        qrParams.topMargin = UiKit.dp(activity, 14);
        content.addView(qr, qrParams);

        TextView bank = UiKit.text(activity, activity.getString(R.string.support_bank_name), 16, colors.primary, true);
        bank.setGravity(Gravity.CENTER);
        add(content, bank, 14);
        TextView owner = UiKit.text(activity, activity.getString(R.string.support_account_name), 14, colors.secondary, false);
        owner.setGravity(Gravity.CENTER);
        add(content, owner, 4);
        TextView number = UiKit.text(activity, activity.getString(R.string.support_account_number), 22, colors.primary, true);
        number.setGravity(Gravity.CENTER);
        number.setSingleLine(true);
        number.setHorizontallyScrolling(false);
        number.setAutoSizeTextTypeUniformWithConfiguration(16, 22, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        number.setContentDescription(activity.getString(R.string.support_account_label) + ": "
                + activity.getString(R.string.support_account_number));
        number.setOnClickListener(view -> copyAccount(activity));
        add(content, number, 10);

        LinearLayout actions = new LinearLayout(activity);
        TextView save = UiKit.button(activity, activity.getString(R.string.support_save_qr), colors.primary, colors.surfaceSoft, 16);
        TextView copy = UiKit.button(activity, activity.getString(R.string.support_copy_action), colors.onAccent, colors.accent, 16);
        save.setTextSize(14); copy.setTextSize(14);
        save.setOnClickListener(view -> saveQr(activity, save));
        copy.setOnClickListener(view -> copyAccount(activity));
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, UiKit.dp(activity, 40), 1);
        left.setMarginEnd(UiKit.dp(activity, 8));
        actions.addView(save, left);
        actions.addView(copy, new LinearLayout.LayoutParams(0, UiKit.dp(activity, 40), 1));
        add(content, actions, 16);
        TextView thanks = UiKit.text(activity, "Cảm ơn bạn đã ủng hộ!", 11, colors.secondary, false);
        thanks.setGravity(Gravity.CENTER);
        add(content, thanks, 12);
        dialog.setContentView(scroll);
        dialog.setOnDismissListener(ignored -> {
            WeakReference<Dialog> current = OPEN.get(activity);
            if (current != null && current.get() == dialog) OPEN.remove(activity);
        });
        UiKit.configureDialog(dialog, colors);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.CENTER);
            int available = activity.getResources().getDisplayMetrics().widthPixels - UiKit.dp(activity, 32);
            window.setLayout(Math.max(1, Math.min(available, UiKit.dp(activity, 360))), -2);
        }
        OPEN.put(activity, new WeakReference<>(dialog));
    }

    private static void saveQr(Activity activity, TextView button) {
        button.setEnabled(false);
        Context context = activity.getApplicationContext();
        new Thread(() -> {
            Uri output = null;
            boolean saved = false;
            try {
                ContentValues metadata = new ContentValues();
                metadata.put(MediaStore.Images.Media.DISPLAY_NAME, "AmLichViet-QR-ung-ho.png");
                metadata.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                metadata.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LichViet");
                metadata.put(MediaStore.Images.Media.IS_PENDING, 1);
                output = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, metadata);
                if (output == null) throw new IOException("Cannot create image");
                try (InputStream input = context.getResources().openRawResource(R.drawable.donate_vietqr);
                     OutputStream stream = context.getContentResolver().openOutputStream(output)) {
                    if (stream == null) throw new IOException("Cannot open image");
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) stream.write(buffer, 0, count);
                }
                metadata.clear();
                metadata.put(MediaStore.Images.Media.IS_PENDING, 0);
                if (context.getContentResolver().update(output, metadata, null, null) < 1)
                    throw new IOException("Cannot publish image");
                saved = true;
            } catch (IOException | RuntimeException error) {
                if (output != null) {
                    try { context.getContentResolver().delete(output, null, null); }
                    catch (RuntimeException ignored) { /* Only this task's incomplete image. */ }
                }
            }
            boolean success = saved;
            activity.runOnUiThread(() -> {
                if (activity.isDestroyed() || activity.isFinishing()) return;
                button.setEnabled(true);
                Toast.makeText(activity, success ? R.string.support_qr_saved : R.string.support_qr_save_failed,
                        Toast.LENGTH_SHORT).show();
            });
        }, "lich-viet-save-support-qr").start();
    }

    private static void copyAccount(Activity activity) {
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(activity.getString(R.string.support_account_label),
                    activity.getString(R.string.support_account_number)));
            Toast.makeText(activity, R.string.support_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private static void add(LinearLayout parent, android.view.View child, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = UiKit.dp(parent.getContext(), top);
        parent.addView(child, params);
    }
    private SupportDialog() {}
}
