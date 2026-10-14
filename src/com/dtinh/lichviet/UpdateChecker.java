package com.dtinh.lichviet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.text.TextUtils;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONObject;

final class UpdateChecker {
    private static final String API_URL = "https://api.github.com/repos/tinhtinh1908/Amlich/releases/latest";
    private static final String RELEASES_URL = "https://github.com/tinhtinh1908/Amlich/releases/latest";
    private static final String PREFS = "update_checker";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_LAST_SUCCESSFUL_DAY = "last_successful_day";
    private static final int MAX_RESPONSE_CHARS = 262144;
    private static final AtomicBoolean CHECK_IN_PROGRESS = new AtomicBoolean(false);

    private UpdateChecker() {
    }

    static void check(final Activity activity) {
        if (!isEnabled(activity)) {
            return;
        }
        final SharedPreferences sharedPreferences = activity.getSharedPreferences(PREFS, 0);
        final int iLocalDayKey = localDayKey();
        if (sharedPreferences.getInt(KEY_LAST_SUCCESSFUL_DAY, 0) == iLocalDayKey || !CHECK_IN_PROGRESS.compareAndSet(false, true)) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                AtomicBoolean atomicBoolean;
                try {
                    final ReleaseInfo releaseInfoLoadLatestRelease = UpdateChecker.loadLatestRelease(activity.getApplicationContext());
                    if (releaseInfoLoadLatestRelease == null) {
                        atomicBoolean = UpdateChecker.CHECK_IN_PROGRESS;
                        return;
                    }
                    sharedPreferences.edit().putInt(UpdateChecker.KEY_LAST_SUCCESSFUL_DAY, iLocalDayKey).apply();
                    if (!UpdateChecker.isEnabled(activity) || UpdateChecker.compareVersions(releaseInfoLoadLatestRelease.version, UpdateChecker.currentVersion(activity)) <= 0) {
                        atomicBoolean = UpdateChecker.CHECK_IN_PROGRESS;
                    } else {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!activity.isFinishing() && !activity.isDestroyed()) {
                                    UpdateChecker.showUpdateDialog(activity, releaseInfoLoadLatestRelease);
                                }
                            }
                        });
                        atomicBoolean = UpdateChecker.CHECK_IN_PROGRESS;
                    }
                } finally {
                    UpdateChecker.CHECK_IN_PROGRESS.set(false);
                }
            }
        }, "LichViet-UpdateChecker").start();
    }

    static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS, 0).getBoolean(KEY_ENABLED, true);
    }

    static void setEnabled(Context context, boolean z) {
        context.getSharedPreferences(PREFS, 0).edit().putBoolean(KEY_ENABLED, z).apply();
    }

    private static int localDayKey() {
        Calendar calendar = Calendar.getInstance();
        return (calendar.get(1) * 10000) + ((calendar.get(2) + 1) * 100) + calendar.get(5);
    }

    public static ReleaseInfo loadLatestRelease(Context context) {
        HttpURLConnection httpURLConnection = null;
        try {
            httpURLConnection = (HttpURLConnection) new URL(API_URL).openConnection();
            httpURLConnection.setConnectTimeout(8000);
            httpURLConnection.setReadTimeout(10000);
            httpURLConnection.setRequestProperty("Accept", "application/vnd.github+json");
            httpURLConnection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            httpURLConnection.setRequestProperty("User-Agent", "LichViet/" + currentVersion(context));
            httpURLConnection.setInstanceFollowRedirects(true);
            if (httpURLConnection.getResponseCode() != 200) {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                return null;
            }
            JSONObject jSONObject = new JSONObject(readLimited(httpURLConnection.getInputStream()));
            String strNormalizeVersion = normalizeVersion(jSONObject.optString("tag_name", ""));
            if (TextUtils.isEmpty(strNormalizeVersion)) {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                return null;
            }
            String strOptString = "";
            JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("assets");
            if (jSONArrayOptJSONArray != null) {
                for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                    JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
                    if (jSONObjectOptJSONObject != null && jSONObjectOptJSONObject.optString("name", "").toLowerCase(Locale.US).endsWith(".apk")) {
                        strOptString = jSONObjectOptJSONObject.optString("browser_download_url", "");
                        break;
                    }
                }
            }
            String strOptString2 = isTrustedGitHubUrl(strOptString) ? strOptString : jSONObject.optString("html_url", RELEASES_URL);
            if (!isTrustedGitHubUrl(strOptString2)) {
                strOptString2 = RELEASES_URL;
            }
            ReleaseInfo releaseInfo = new ReleaseInfo(strNormalizeVersion, strOptString2);
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            return releaseInfo;
        } catch (Exception e) {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            return null;
        } catch (Throwable th) {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            throw th;
        }
    }

    private static String readLimited(InputStream inputStream) throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        try {
            StringBuilder sb = new StringBuilder();
            char[] cArr = new char[4096];
            while (true) {
                int i = bufferedReader.read(cArr);
                if (i != -1) {
                    if (sb.length() + i > MAX_RESPONSE_CHARS) {
                        throw new IllegalStateException("GitHub response is too large");
                    }
                    sb.append(cArr, 0, i);
                } else {
                    String string = sb.toString();
                    bufferedReader.close();
                    return string;
                }
            }
        } catch (Throwable th) {
            try {
                bufferedReader.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    public static String currentVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName == null ? "0" : normalizeVersion(packageInfo.versionName);
        } catch (Exception e) {
            return "0";
        }
    }

    static int compareVersions(String str, String str2) {
        String[] strArrSplit = normalizeVersion(str).split("\\.");
        String[] strArrSplit2 = normalizeVersion(str2).split("\\.");
        int iMax = Math.max(strArrSplit.length, strArrSplit2.length);
        int i = 0;
        while (i < iMax) {
            int versionPart = i < strArrSplit.length ? parseVersionPart(strArrSplit[i]) : 0;
            int versionPart2 = i < strArrSplit2.length ? parseVersionPart(strArrSplit2[i]) : 0;
            if (versionPart != versionPart2) {
                return versionPart < versionPart2 ? -1 : 1;
            }
            i++;
        }
        return 0;
    }

    private static String normalizeVersion(String str) {
        String strTrim = str == null ? "" : str.trim();
        if (strTrim.startsWith("v") || strTrim.startsWith("V")) {
            strTrim = strTrim.substring(1);
        }
        int iIndexOf = strTrim.indexOf(45);
        if (iIndexOf >= 0) {
            strTrim = strTrim.substring(0, iIndexOf);
        }
        return strTrim;
    }

    private static int parseVersionPart(String str) {
        char cCharAt;
        int i = 0;
        for (int i2 = 0; i2 < str.length() && (cCharAt = str.charAt(i2)) >= '0' && cCharAt <= '9'; i2++) {
            if (i > (Integer.MAX_VALUE - (cCharAt - '0')) / 10) {
                return Integer.MAX_VALUE;
            }
            i = (i * 10) + (cCharAt - '0');
        }
        return i;
    }

    private static boolean isTrustedGitHubUrl(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        Uri uri = Uri.parse(str);
        String host = uri.getHost();
        return "https".equalsIgnoreCase(uri.getScheme()) && host != null && (host.equalsIgnoreCase("github.com") || host.toLowerCase(Locale.US).endsWith(".github.com"));
    }

    public static void showUpdateDialog(Activity activity, ReleaseInfo releaseInfo) {
        AlertDialog alertDialogCreate = new AlertDialog.Builder(activity).setTitle(R.string.update_available_title).setMessage(activity.getString(R.string.update_available_message, new Object[]{releaseInfo.version})).setNegativeButton(R.string.update_later, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.update_download, (dialogInterface, i) -> {
            try {
                activity.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(releaseInfo.url)));
            } catch (Exception e) {
                try {
                    activity.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(RELEASES_URL)));
                } catch (Exception e2) {
                }
            }
        }).create();
        alertDialogCreate.setOnShowListener(dialogInterface2 -> {
            UiKit.styleAlert(alertDialogCreate, activity);
        });
        alertDialogCreate.show();
    }

    private static final class ReleaseInfo {
        final String version;
        final String url;

        ReleaseInfo(String str, String str2) {
            this.version = str;
            this.url = str2;
        }
    }
}
