package com.dtinh.lichviet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;


public final class XiaomiNotesBackup {
    private static final ExecutorService BACKGROUND = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "lich-viet-note-backup");
            thread.setPriority(4);
            return thread;
        }
    });
    private static final String FORMAT_MAGIC = "LichVietNotes";
    private static final int FORMAT_VERSION = 1;
    private static final int ITERATIONS = 180000;
    private static final int IV_BYTES = 12;
    private static final int MAX_IMPORT_NOTES = 5000;
    private static final int MAX_PLAIN_BYTES = 2000000;
    private static final int MAX_SHARED_CHARS = 700000;
    private static final String PREFIX = "LVB1:";
    private static final int SALT_BYTES = 16;
    private static final String XIAOMI_NOTES = "com.miui.notes";

    private XiaomiNotesBackup() {
    }

    public static void showBackupDialog(final Activity activity) {
        if (isUsable(activity)) {
            final PasswordForm passwordForm = passwordForm(activity, true);
            final AlertDialog alertDialogCreate = new AlertDialog.Builder(activity).setTitle(R.string.backup_dialog_title).setMessage(R.string.backup_dialog_message).setView(passwordForm.content).setNegativeButton(R.string.note_cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.backup_create, (DialogInterface.OnClickListener) null).create();
            alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public final void onShow(DialogInterface dialogInterface) {
                    XiaomiNotesBackup.lambda$showBackupDialog$1(activity, alertDialogCreate, passwordForm, dialogInterface);
                }
            });
            alertDialogCreate.show();
        }
    }

    static void lambda$showBackupDialog$1(final Activity activity, final AlertDialog alertDialog, final PasswordForm passwordForm, DialogInterface dialogInterface) {
        styleDialog(activity, alertDialog);
        alertDialog.getButton(-1).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                XiaomiNotesBackup.lambda$showBackupDialog$0(passwordForm, activity, alertDialog, view);
            }
        });
    }

    static void lambda$showBackupDialog$0(PasswordForm passwordForm, Activity activity, AlertDialog alertDialog, View view) {
        String string = passwordForm.password.getText().toString();
        String string2 = passwordForm.confirm.getText().toString();
        if (string.length() < 6) {
            passwordForm.password.setError(activity.getString(R.string.backup_password_short));
        } else if (!string.equals(string2)) {
            passwordForm.confirm.setError(activity.getString(R.string.backup_password_mismatch));
        } else {
            alertDialog.dismiss();
            createAndShare(activity, string.toCharArray());
        }
    }

    public static boolean handleIncomingShare(Activity activity, Intent intent, Runnable runnable) {
        if (intent == null || !"android.intent.action.SEND".equals(intent.getAction()) || !"text/plain".equals(intent.getType())) {
            return false;
        }
        try {
            CharSequence charSequenceExtra = intent.getCharSequenceExtra("android.intent.extra.TEXT");
            if (charSequenceExtra == null || charSequenceExtra.toString().indexOf(PREFIX) < 0) {
                Toast.makeText(activity, R.string.backup_invalid_share, Toast.LENGTH_LONG).show();
                return true;
            }
            String string = charSequenceExtra.toString();
            if (string.length() > MAX_SHARED_CHARS) {
                Toast.makeText(activity, R.string.backup_too_large, Toast.LENGTH_LONG).show();
                return true;
            }
            showRestoreDialog(activity, string, runnable);
            return true;
        } catch (RuntimeException e) {
            Toast.makeText(activity, R.string.backup_invalid_share, Toast.LENGTH_LONG).show();
            return true;
        }
    }

    private static void createAndShare(final Activity activity, final char[] cArr) {
        final Map<String, String> mapExportAll = NoteRepository.exportAll(activity);
        if (mapExportAll.isEmpty()) {
            wipe(cArr);
            Toast.makeText(activity, R.string.backup_no_notes, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(activity, R.string.backup_creating, Toast.LENGTH_SHORT).show();
            BACKGROUND.execute(new Runnable() {
                @Override
                public final void run() {
                    XiaomiNotesBackup.lambda$createAndShare$3(mapExportAll, cArr, activity);
                }
            });
        }
    }

    static void lambda$createAndShare$3(Map<String, String> map, char[] cArr, final Activity activity) {
        try {
            try {
                final String str = "LỊCH VIỆT BACKUP\nNgày tạo: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN")).format(new Date()) + "\nSố ghi chú: " + map.size() + "\nKhông chỉnh sửa dòng dữ liệu bên dưới.\n\n" + PREFIX + encrypt(serialize(map), cArr);
                if (str.length() > MAX_SHARED_CHARS) {
                    postToast(activity, R.string.backup_too_large);
                } else {
                    post(activity, new Runnable() {
                        @Override
                        public final void run() {
                            XiaomiNotesBackup.shareToNotes(activity, str);
                        }
                    });
                }
            } catch (Exception e) {
                postToast(activity, R.string.backup_create_failed);
            }
        } finally {
            wipe(cArr);
        }
    }

    private static void showRestoreDialog(final Activity activity, final String str, final Runnable runnable) {
        if (isUsable(activity)) {
            final PasswordForm passwordForm = passwordForm(activity, false);
            final AlertDialog alertDialogCreate = new AlertDialog.Builder(activity).setTitle(R.string.restore_dialog_title).setMessage(R.string.restore_dialog_message).setView(passwordForm.content).setNegativeButton(R.string.note_cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.restore_decrypt, (DialogInterface.OnClickListener) null).create();
            alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public final void onShow(DialogInterface dialogInterface) {
                    XiaomiNotesBackup.lambda$showRestoreDialog$5(activity, alertDialogCreate, passwordForm, str, runnable, dialogInterface);
                }
            });
            alertDialogCreate.show();
        }
    }

    static void lambda$showRestoreDialog$5(final Activity activity, final AlertDialog alertDialog, final PasswordForm passwordForm, final String str, final Runnable runnable, DialogInterface dialogInterface) {
        styleDialog(activity, alertDialog);
        alertDialog.getButton(-1).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                XiaomiNotesBackup.lambda$showRestoreDialog$4(passwordForm, activity, alertDialog, str, runnable, view);
            }
        });
    }

    static void lambda$showRestoreDialog$4(PasswordForm passwordForm, Activity activity, AlertDialog alertDialog, String str, Runnable runnable, View view) {
        String string = passwordForm.password.getText().toString();
        if (string.length() < 6) {
            passwordForm.password.setError(activity.getString(R.string.backup_password_short));
        } else {
            alertDialog.dismiss();
            decryptAndConfirm(activity, str, string.toCharArray(), runnable);
        }
    }

    private static void decryptAndConfirm(final Activity activity, final String str, final char[] cArr, final Runnable runnable) {
        Toast.makeText(activity, R.string.restore_decrypting, Toast.LENGTH_SHORT).show();
        BACKGROUND.execute(new Runnable() {
            @Override
            public final void run() {
                XiaomiNotesBackup.lambda$decryptAndConfirm$7(str, cArr, activity, runnable);
            }
        });
    }

    static void lambda$decryptAndConfirm$7(String str, char[] cArr, final Activity activity, final Runnable runnable) {
        try {
            try {
                final Map<String, String> mapDeserialize = deserialize(decrypt(extractPayload(str), cArr));
                post(activity, new Runnable() {
                    @Override
                    public final void run() {
                        XiaomiNotesBackup.showRestoreConfirmation(activity, mapDeserialize, runnable);
                    }
                });
            } catch (Exception e) {
                postToast(activity, R.string.restore_failed);
            }
        } finally {
            wipe(cArr);
        }
    }

    private static PasswordForm passwordForm(Activity activity, boolean z) {
        EditText editTextPasswordField;
        UiKit.Palette palette = new UiKit.Palette(activity);
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int iM4dp = dp(activity, 24.0f);
        linearLayout.setPadding(iM4dp, dp(activity, 8.0f), iM4dp, 0);
        linearLayout.addView(fieldLabel(activity, activity.getString(R.string.backup_password), palette));
        EditText editTextPasswordField2 = passwordField(activity, R.string.backup_password_hint, palette);
        linearLayout.addView(editTextPasswordField2);
        if (!z) {
            editTextPasswordField = null;
        } else {
            TextView textViewFieldLabel = fieldLabel(activity, activity.getString(R.string.backup_password_confirm), palette);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
            layoutParams.topMargin = dp(activity, 12.0f);
            linearLayout.addView(textViewFieldLabel, layoutParams);
            editTextPasswordField = passwordField(activity, R.string.backup_password_confirm_hint, palette);
            linearLayout.addView(editTextPasswordField);
        }
        return new PasswordForm(linearLayout, editTextPasswordField2, editTextPasswordField);
    }

    private static TextView fieldLabel(Activity activity, String str, UiKit.Palette palette) {
        TextView textViewText = UiKit.text(activity, str, 11.0f, palette.secondary, true);
        textViewText.setPadding(dp(activity, 2.0f), 0, 0, dp(activity, 6.0f));
        return textViewText;
    }

    private static EditText passwordField(Activity activity, int i, UiKit.Palette palette) {
        EditText editTextField = UiKit.field(activity, palette, i, true);
        editTextField.setSingleLine(true);
        editTextField.setInputType(129);
        editTextField.setSelectAllOnFocus(false);
        return editTextField;
    }


    public static void showRestoreConfirmation(final Activity activity, final Map<String, String> map, final Runnable runnable) {
        final AlertDialog alertDialogCreate = new AlertDialog.Builder(activity).setTitle(R.string.restore_confirm_title).setMessage(activity.getString(R.string.restore_confirm_message, new Object[]{Integer.valueOf(map.size())})).setNegativeButton(R.string.note_cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.restore_apply, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                XiaomiNotesBackup.lambda$showRestoreConfirmation$8(activity, map, runnable, dialogInterface, i);
            }
        }).create();
        alertDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public final void onShow(DialogInterface dialogInterface) {
                XiaomiNotesBackup.styleDialog(activity, alertDialogCreate);
            }
        });
        alertDialogCreate.show();
    }

    static void lambda$showRestoreConfirmation$8(Activity activity, Map<String, String> map, Runnable runnable, DialogInterface dialogInterface, int i) {
        int iMergeAll = NoteRepository.mergeAll(activity, map);
        if (runnable != null) {
            runnable.run();
        }
        Toast.makeText(activity, activity.getString(R.string.restore_success, new Object[]{Integer.valueOf(iMergeAll)}), Toast.LENGTH_LONG).show();
    }


    public static void styleDialog(Activity activity, AlertDialog alertDialog) {
        UiKit.Palette palette = new UiKit.Palette(activity);
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(UiKit.rounded(palette.surface, UiKit.dp(activity, 24.0f)));
            window.addFlags(2);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.56f;
            window.setAttributes(attributes);
        }
        if (window != null) {
            tintTextHierarchy(window.getDecorView(), palette.primary);
        }
        TextView textView2 = (TextView) alertDialog.findViewById(android.R.id.message);
        if (textView2 != null) {
            textView2.setTextColor(palette.secondary);
            textView2.setTextSize(14.0f);
        }
        Button button = alertDialog.getButton(-1);
        if (button != null) {
            button.setTextColor(-1);
            button.setAllCaps(false);
            button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            button.setBackground(UiKit.rounded(palette.accent, dp(activity, 14.0f)));
        }
        Button button2 = alertDialog.getButton(-2);
        if (button2 != null) {
            button2.setTextColor(palette.secondary);
            button2.setAllCaps(false);
            button2.setBackgroundColor(0);
        }
    }

    private static void tintTextHierarchy(View view, int textColor) {
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(textColor);
        }
        if (!(view instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            tintTextHierarchy(group.getChildAt(index), textColor);
        }
    }


    public static void shareToNotes(Activity activity, String str) {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("text/plain");
        intent.putExtra("android.intent.extra.SUBJECT", activity.getString(R.string.backup_note_subject));
        intent.putExtra("android.intent.extra.TEXT", str);
        intent.setPackage(XIAOMI_NOTES);
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            intent.setPackage(null);
            try {
                activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.backup_share_chooser)));
            } catch (RuntimeException e2) {
                Toast.makeText(activity, R.string.backup_share_failed, Toast.LENGTH_LONG).show();
            }
        } catch (RuntimeException e3) {
            Toast.makeText(activity, R.string.backup_share_failed, Toast.LENGTH_LONG).show();
        }
    }

    private static byte[] serialize(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        sb.append(FORMAT_MAGIC).append('\n').append(FORMAT_VERSION).append('\n').append(System.currentTimeMillis()).append('\n').append(map.size()).append('\n');
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(entry.getKey()).append('\t').append(Base64.encodeToString(entry.getValue().getBytes(StandardCharsets.UTF_8), 11)).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static Map<String, String> deserialize(byte[] bArr) throws Exception {
        if (bArr.length > MAX_PLAIN_BYTES) {
            throw new IllegalArgumentException("Oversized backup");
        }
        String[] strArrSplit = new String(bArr, StandardCharsets.UTF_8).split("\\n", -1);
        if (strArrSplit.length < 4 || !FORMAT_MAGIC.equals(strArrSplit[0]) || Integer.parseInt(strArrSplit[FORMAT_VERSION]) != FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported backup");
        }
        int i = Integer.parseInt(strArrSplit[3]);
        if (i < 0 || i > MAX_IMPORT_NOTES) {
            throw new IllegalArgumentException("Invalid note count");
        }
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
        for (int i2 = 4; i2 < strArrSplit.length && linkedHashMap.size() < i; i2 += FORMAT_VERSION) {
            int iIndexOf = strArrSplit[i2].indexOf(9);
            if (iIndexOf > 0) {
                String strSubstring = strArrSplit[i2].substring(0, iIndexOf);
                String str = new String(Base64.decode(strArrSplit[i2].substring(iIndexOf + FORMAT_VERSION), 11), StandardCharsets.UTF_8);
                if (isValidKey(strSubstring) && !str.trim().isEmpty()) {
                    linkedHashMap.put(strSubstring, str);
                }
            }
        }
        if (linkedHashMap.size() != i) {
            throw new IllegalArgumentException("Incomplete backup");
        }
        return linkedHashMap;
    }

    private static String encrypt(byte[] bArr, char[] cArr) throws Exception {
        byte[] bArr2 = new byte[SALT_BYTES];
        byte[] bArr3 = new byte[IV_BYTES];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(bArr2);
        secureRandom.nextBytes(bArr3);
        SecretKey secretKeyDeriveKey = deriveKey(cArr, bArr2, ITERATIONS);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(FORMAT_VERSION, secretKeyDeriveKey, new GCMParameterSpec(128, bArr3));
        byte[] bArrDoFinal = cipher.doFinal(bArr);
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(bArrDoFinal.length + 33);
        byteBufferAllocate.put((byte) 1).putInt(ITERATIONS).put(bArr2).put(bArr3).put(bArrDoFinal);
        return Base64.encodeToString(byteBufferAllocate.array(), 11);
    }

    private static byte[] decrypt(String str, char[] cArr) throws Exception {
        byte[] bArrDecode = Base64.decode(str, 11);
        if (bArrDecode.length < 49) {
            throw new IllegalArgumentException("Short backup");
        }
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArrDecode);
        if (byteBufferWrap.get() != FORMAT_VERSION) {
            throw new IllegalArgumentException("Version");
        }
        int i = byteBufferWrap.getInt();
        if (i < 10000 || i > 1000000) {
            throw new IllegalArgumentException("KDF");
        }
        byte[] bArr = new byte[SALT_BYTES];
        byte[] bArr2 = new byte[IV_BYTES];
        byteBufferWrap.get(bArr);
        byteBufferWrap.get(bArr2);
        byte[] bArr3 = new byte[byteBufferWrap.remaining()];
        byteBufferWrap.get(bArr3);
        SecretKey secretKeyDeriveKey = deriveKey(cArr, bArr, i);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(2, secretKeyDeriveKey, new GCMParameterSpec(128, bArr2));
        return cipher.doFinal(bArr3);
    }

    private static SecretKey deriveKey(char[] cArr, byte[] bArr, int i) throws Exception {
        PBEKeySpec pBEKeySpec = new PBEKeySpec(cArr, bArr, i, 256);
        try {
            return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(pBEKeySpec).getEncoded(), "AES");
        } finally {
            pBEKeySpec.clearPassword();
        }
    }

    private static String extractPayload(String str) {
        int iIndexOf = str.indexOf(PREFIX);
        if (iIndexOf < 0) {
            throw new IllegalArgumentException("Missing payload");
        }
        int length = iIndexOf + PREFIX.length();
        int i = length;
        while (i < str.length()) {
            char cCharAt = str.charAt(i);
            if (!((cCharAt >= 'A' && cCharAt <= 'Z') || (cCharAt >= 'a' && cCharAt <= 'z') || ((cCharAt >= '0' && cCharAt <= '9') || cCharAt == '_' || cCharAt == '-'))) {
                break;
            }
            i += FORMAT_VERSION;
        }
        if (i == length) {
            throw new IllegalArgumentException("Empty payload");
        }
        return str.substring(length, i);
    }

    private static boolean isValidKey(String str) {
        if (str == null || str.length() != 13 || !str.startsWith("note_")) {
            return false;
        }
        for (int i = 5; i < str.length(); i += FORMAT_VERSION) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static void wipe(char[] cArr) {
        if (cArr == null) {
            return;
        }
        for (int i = 0; i < cArr.length; i += FORMAT_VERSION) {
            cArr[i] = 0;
        }
    }

    private static void post(final Activity activity, final Runnable runnable) {
        try {
            activity.runOnUiThread(new Runnable() {
                @Override
                public final void run() {
                    XiaomiNotesBackup.lambda$post$10(activity, runnable);
                }
            });
        } catch (RuntimeException e) {
        }
    }

    static void lambda$post$10(Activity activity, Runnable runnable) {
        if (isUsable(activity)) {
            runnable.run();
        }
    }

    private static void postToast(final Activity activity, final int i) {
        post(activity, new Runnable() {
            @Override
            public final void run() {
                Toast.makeText(activity, i, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static boolean isUsable(Activity activity) {
        return (activity == null || activity.isFinishing() || activity.isDestroyed()) ? false : true;
    }


    static final class PasswordForm {
        final EditText confirm;
        final LinearLayout content;
        final EditText password;

        PasswordForm(LinearLayout linearLayout, EditText editText, EditText editText2) {
            this.content = linearLayout;
            this.password = editText;
            this.confirm = editText2;
        }
    }


    private static int dp(Activity activity, float f) {
        return Math.round(f * activity.getResources().getDisplayMetrics().density);
    }
}
