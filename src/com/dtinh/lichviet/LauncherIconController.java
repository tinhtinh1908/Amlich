package com.dtinh.lichviet;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class LauncherIconController {
    static final String ACTION_REFRESH = "com.dtinh.lichviet.action.REFRESH_LAUNCHER_DATE";
    private static final int ALARM_REQUEST_CODE = 2108;
    private static final long FALLBACK_WINDOW_MILLIS = 900000;
    private static final String BASE_COMPONENT = "LauncherBase";
    private static final String DATE_COMPONENT_PREFIX = "LauncherDate";
    private static final int LAST_DAY = 31;

    private LauncherIconController() {
    }

    public static void arm(Context context) {
        try {
            scheduleNextRefresh(context.getApplicationContext());
        } catch (RuntimeException e) {
        }
    }

    public static void refresh(Context context) {
        Context applicationContext = context.getApplicationContext();
        String strAliasForDay = aliasForDay(Calendar.getInstance().get(5));
        PackageManager packageManager = applicationContext.getPackageManager();
        if (Build.VERSION.SDK_INT >= 33) {
            Api33.apply(packageManager, applicationContext, strAliasForDay);
        } else {
            applySequentially(packageManager, applicationContext, strAliasForDay);
        }
        scheduleNextRefresh(applicationContext);
    }

    public static void applySequentially(PackageManager packageManager, Context context, String str) {
        setState(packageManager, component(context, str), 1);
        for (int i = 1; i <= LAST_DAY; i++) {
            String strAliasForDay = aliasForDay(i);
            if (!str.equals(strAliasForDay)) {
                setState(packageManager, component(context, strAliasForDay), 2);
            }
        }
        setState(packageManager, component(context, BASE_COMPONENT), 2);
    }

    private static void setState(PackageManager packageManager, ComponentName componentName, int i) {
        int componentEnabledSetting = packageManager.getComponentEnabledSetting(componentName);
        if (componentEnabledSetting == i) {
            return;
        }
        if (i == 2 && componentEnabledSetting == 0 && !componentName.getClassName().endsWith(BASE_COMPONENT)) {
            return;
        }
        try {
            packageManager.setComponentEnabledSetting(componentName, i, 1);
        } catch (RuntimeException e) {
        }
    }

    private static void scheduleNextRefresh(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
        if (alarmManager == null) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(5, 1);
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        PendingIntent broadcast = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, new Intent(context, (Class<?>) LauncherIconReceiver.class).setAction(ACTION_REFRESH), 201326592);
        long timeInMillis = calendar.getTimeInMillis();
        try {
            if (Build.VERSION.SDK_INT < LAST_DAY || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(0, timeInMillis, broadcast);
                return;
            }
        } catch (SecurityException e) {
        }
        try {
            alarmManager.setWindow(1, timeInMillis, FALLBACK_WINDOW_MILLIS, broadcast);
        } catch (RuntimeException e2) {
        }
    }

    public static ComponentName component(Context context, String str) {
        return new ComponentName(context, context.getPackageName() + "." + str);
    }

    public static String aliasForDay(int i) {
        return DATE_COMPONENT_PREFIX + String.format(Locale.US, "%02d", Integer.valueOf(i));
    }

    @TargetApi(33)
    private static final class Api33 {
        private Api33() {
        }

        static void apply(PackageManager packageManager, Context context, String str) {
            ArrayList arrayList = new ArrayList(32);
            addIfNeeded(packageManager, arrayList, LauncherIconController.component(context, str), 1, false);
            for (int i = 1; i <= LauncherIconController.LAST_DAY; i++) {
                String strAliasForDay = LauncherIconController.aliasForDay(i);
                if (!str.equals(strAliasForDay)) {
                    addIfNeeded(packageManager, arrayList, LauncherIconController.component(context, strAliasForDay), 2, false);
                }
            }
            addIfNeeded(packageManager, arrayList, LauncherIconController.component(context, LauncherIconController.BASE_COMPONENT), 2, true);
            if (arrayList.isEmpty()) {
                return;
            }
            try {
                packageManager.setComponentEnabledSettings(arrayList);
            } catch (RuntimeException e) {
                LauncherIconController.applySequentially(packageManager, context, str);
            }
        }

        private static void addIfNeeded(PackageManager packageManager, List<PackageManager.ComponentEnabledSetting> list, ComponentName componentName, int i, boolean z) {
            int componentEnabledSetting = packageManager.getComponentEnabledSetting(componentName);
            if (componentEnabledSetting == i) {
                return;
            }
            if (componentEnabledSetting == 0) {
                if (i == 1 && z) {
                    return;
                }
                if (i == 2 && !z) {
                    return;
                }
            }
            list.add(new PackageManager.ComponentEnabledSetting(componentName, i, 1));
        }
    }
}
