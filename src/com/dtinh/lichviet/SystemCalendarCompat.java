package com.dtinh.lichviet;

import android.content.Intent;
import android.net.Uri;


public final class SystemCalendarCompat {
    private SystemCalendarCompat() {
    }

    public static Long resolveDateMillis(Intent intent) {
        if (intent == null) {
            return null;
        }
        long beginTime = intent.getLongExtra("beginTime", Long.MIN_VALUE);
        if (beginTime != Long.MIN_VALUE) {
            return Long.valueOf(beginTime);
        }
        Uri data = intent.getData();
        if (data == null) {
            return null;
        }
        String lastPathSegment = data.getLastPathSegment();
        if ("time".equals(data.getHost()) || "time".equals(firstSegment(data))) {
            try {
                return Long.valueOf(Long.parseLong(lastPathSegment));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private static String firstSegment(Uri uri) {
        return uri.getPathSegments().isEmpty() ? "" : uri.getPathSegments().get(0);
    }
}
