package com.dtinh.lichviet;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.util.Base64;
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

    public static void showBackupDialog(Activity activity) {
        if (isUsable(activity)) {
            BackupUiDialog.password(activity, true, secret -> createAndShare(activity, secret));
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
            wipe(cArr);
        } catch (Throwable th) {
            wipe(cArr);
            throw th;
        }
    }

    private static void showRestoreDialog(Activity activity, String backup, Runnable changed) {
        if (isUsable(activity)) {
            BackupUiDialog.password(activity, false,
                    secret -> decryptAndConfirm(activity, backup, secret, changed));
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

    public static void showRestoreConfirmation(Activity activity, Map<String, String> notes, Runnable changed) {
        BackupUiDialog.restoreConfirmation(activity, notes.size(), () -> {
            int imported = NoteRepository.mergeAll(activity, notes);
            if (changed != null) changed.run();
            Toast.makeText(activity, activity.getString(R.string.restore_success, imported), Toast.LENGTH_LONG).show();
        });
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
        LinkedHashMap linkedHashMap = new LinkedHashMap();
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
        char cCharAt;
        int iIndexOf = str.indexOf(PREFIX);
        if (iIndexOf < 0) {
            throw new IllegalArgumentException("Missing payload");
        }
        int length = iIndexOf + PREFIX.length();
        int i = length;
        while (i < str.length() && (((cCharAt = str.charAt(i)) >= 'A' && cCharAt <= 'Z') || ((cCharAt >= 'a' && cCharAt <= 'z') || ((cCharAt >= '0' && cCharAt <= '9') || cCharAt == '_' || cCharAt == '-')))) {
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

}
