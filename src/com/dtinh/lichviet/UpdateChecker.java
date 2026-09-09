package com.dtinh.lichviet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** Checks the latest public GitHub Release without requiring a token. */
final class UpdateChecker {
    private static final String API_URL = "https://api.github.com/repos/tinhtinh1908/Amlich/releases/latest";
    private static final String RELEASES_URL = "https://github.com/tinhtinh1908/Amlich/releases/latest";
    private static final String PREFS = "update_checker";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_LAST_SUCCESSFUL_DAY = "last_successful_day";
    private static final int MAX_RESPONSE_CHARS = 256 * 1024;
    private static final AtomicBoolean CHECK_IN_PROGRESS = new AtomicBoolean(false);

    private UpdateChecker() {
    }

    static void check(final Activity activity) {
        if (!isEnabled(activity)) {
            return;
        }
        final SharedPreferences preferences = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        final int today = localDayKey();
        if (preferences.getInt(KEY_LAST_SUCCESSFUL_DAY, 0) == today) {
            return;
        }
        if (!CHECK_IN_PROGRESS.compareAndSet(false, true)) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final ReleaseInfo release = loadLatestRelease(activity.getApplicationContext());
                    if (release == null) {
                        return;
                    }
                    // Only throttle a completed request. Offline and transient GitHub
                    // failures can therefore be retried on the next app launch.
                    preferences.edit().putInt(KEY_LAST_SUCCESSFUL_DAY, today).apply();
                    if (!isEnabled(activity)
                            || compareVersions(release.version, currentVersion(activity)) <= 0) {
                        return;
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (activity.isFinishing() || activity.isDestroyed()) {
                                return;
                            }
                            showUpdateDialog(activity, release);
                        }
                    });
                } finally {
                    CHECK_IN_PROGRESS.set(false);
                }
            }
        }, "LichViet-UpdateChecker").start();
    }

    static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, true);
    }

    static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    private static int localDayKey() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.YEAR) * 10_000
                + (now.get(Calendar.MONTH) + 1) * 100
                + now.get(Calendar.DAY_OF_MONTH);
    }

    private static ReleaseInfo loadLatestRelease(Context context) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(API_URL).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            connection.setRequestProperty("User-Agent", "LichViet/" + currentVersion(context));
            connection.setInstanceFollowRedirects(true);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            JSONObject root = new JSONObject(readLimited(connection.getInputStream()));
            String version = normalizeVersion(root.optString("tag_name", ""));
            if (TextUtils.isEmpty(version)) {
                return null;
            }

            String downloadUrl = "";
            JSONArray assets = root.optJSONArray("assets");
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.optJSONObject(i);
                    if (asset == null) {
                        continue;
                    }
                    String name = asset.optString("name", "").toLowerCase(Locale.US);
                    if (name.endsWith(".apk")) {
                        downloadUrl = asset.optString("browser_download_url", "");
                        break;
                    }
                }
            }
            String releaseUrl = root.optString("html_url", RELEASES_URL);
            String targetUrl = isTrustedGitHubUrl(downloadUrl) ? downloadUrl : releaseUrl;
            if (!isTrustedGitHubUrl(targetUrl)) {
                targetUrl = RELEASES_URL;
            }
            return new ReleaseInfo(version, targetUrl);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readLimited(InputStream inputStream) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                if (result.length() + read > MAX_RESPONSE_CHARS) {
                    throw new IllegalStateException("GitHub response is too large");
                }
                result.append(buffer, 0, read);
            }
            return result.toString();
        }
    }

    private static String currentVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionName == null ? "0" : normalizeVersion(info.versionName);
        } catch (Exception ignored) {
            return "0";
        }
    }

    static int compareVersions(String first, String second) {
        String[] left = normalizeVersion(first).split("\\.");
        String[] right = normalizeVersion(second).split("\\.");
        int count = Math.max(left.length, right.length);
        for (int i = 0; i < count; i++) {
            int leftPart = i < left.length ? parseVersionPart(left[i]) : 0;
            int rightPart = i < right.length ? parseVersionPart(right[i]) : 0;
            if (leftPart != rightPart) {
                return leftPart < rightPart ? -1 : 1;
            }
        }
        return 0;
    }

    private static String normalizeVersion(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        int suffix = normalized.indexOf('-');
        if (suffix >= 0) {
            normalized = normalized.substring(0, suffix);
        }
        return normalized;
    }

    private static int parseVersionPart(String value) {
        int result = 0;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character < '0' || character > '9') {
                break;
            }
            if (result > (Integer.MAX_VALUE - (character - '0')) / 10) {
                return Integer.MAX_VALUE;
            }
            result = (result * 10) + (character - '0');
        }
        return result;
    }

    private static boolean isTrustedGitHubUrl(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        Uri uri = Uri.parse(value);
        String host = uri.getHost();
        return "https".equalsIgnoreCase(uri.getScheme())
                && host != null
                && (host.equalsIgnoreCase("github.com") || host.toLowerCase(Locale.US).endsWith(".github.com"));
    }

    private static void showUpdateDialog(final Activity activity, final ReleaseInfo release) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.update_available_title)
                .setMessage(activity.getString(R.string.update_available_message, release.version))
                .setNegativeButton(R.string.update_later, null)
                .setPositiveButton(R.string.update_download, (dialog, which) -> {
                    try {
                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(release.url)));
                    } catch (Exception ignored) {
                        try {
                            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)));
                        } catch (Exception ignoredAgain) {
                            // A device without a browser must not crash the calendar.
                        }
                    }
                })
                .show();
    }

    private static final class ReleaseInfo {
        final String version;
        final String url;

        ReleaseInfo(String version, String url) {
            this.version = version;
            this.url = url;
        }
    }
}
