package com.dtinh.lichviet;

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
    private static final long FALLBACK_WINDOW_MILLIS = 15 * 60 * 1000L;
    private static final String BASE_COMPONENT = "LauncherBase";
    private static final String DATE_COMPONENT_PREFIX = "LauncherDate";
    private static final int LAST_DAY = 31;

    private LauncherIconController() {
    }

    public static void arm(Context context) {
        try {
            // Never switch the launcher alias while MainActivity is starting.
            // Changing the currently launched component can make HyperOS remove
            // the foreground task. Broadcast events perform the icon update;
            // opening the app only guarantees that the next midnight refresh is armed.
            scheduleNextRefresh(context.getApplicationContext());
        } catch (RuntimeException ignored) {
            // Icon refresh must never prevent the calendar UI from opening.
        }
    }

    public static void refresh(Context context) {
        Context applicationContext = context.getApplicationContext();
        String activeAlias = aliasForDay(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        PackageManager packageManager = applicationContext.getPackageManager();
        if (Build.VERSION.SDK_INT >= 33) {
            Api33.apply(packageManager, applicationContext, activeAlias);
        } else {
            applySequentially(packageManager, applicationContext, activeAlias);
        }
        scheduleNextRefresh(applicationContext);
    }


    private static void applySequentially(
            PackageManager packageManager, Context context, String activeAlias) {
        setState(packageManager, component(context, activeAlias),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        for (int i = 1; i <= LAST_DAY; i++) {
            String alias = aliasForDay(i);
            if (!activeAlias.equals(alias)) {
                setState(packageManager, component(context, alias),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            }
        }
        setState(packageManager, component(context, BASE_COMPONENT),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    private static void setState(PackageManager packageManager, ComponentName componentName, int i) {
        int componentEnabledSetting = packageManager.getComponentEnabledSetting(componentName);
        if (componentEnabledSetting == i) {
            return;
        }
        if (i == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                && componentEnabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                && !componentName.getClassName().endsWith(BASE_COMPONENT)) {
            return;
        }
        try {
            packageManager.setComponentEnabledSetting(
                    componentName, i, PackageManager.DONT_KILL_APP);
        } catch (RuntimeException ignored) {
            // Some launchers temporarily reject alias changes; retry next app start.
        }
    }

    private static void scheduleNextRefresh(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        PendingIntent broadcast = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                new Intent(context, LauncherIconReceiver.class).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long triggerAtMillis = calendar.getTimeInMillis();
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                    || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, broadcast);
                return;
            }
        } catch (SecurityException ignored) {
            // HyperOS can revoke exact alarms independently of the manifest permission.
        }

        // Keep the midnight refresh working without crashing app startup when exact
        // alarm access is unavailable. A short inexact window is sufficient for an icon.
        alarmManager.setWindow(
                AlarmManager.RTC,
                triggerAtMillis,
                FALLBACK_WINDOW_MILLIS,
                broadcast);
    }


    public static ComponentName component(Context context, String str) {
        return new ComponentName(context, context.getPackageName() + "." + str);
    }

    private static String aliasForDay(int day) {
        return DATE_COMPONENT_PREFIX + String.format(Locale.US, "%02d", day);
    }

    @android.annotation.TargetApi(33)
    private static final class Api33 {
        private Api33() {
        }

        static void apply(PackageManager packageManager, Context context, String activeAlias) {
            ArrayList<PackageManager.ComponentEnabledSetting> settings = new ArrayList<>(32);
            addIfNeeded(packageManager, settings,
                    LauncherIconController.component(context, activeAlias),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, false);
            for (int i = 1; i <= LauncherIconController.LAST_DAY; i++) {
                String alias = LauncherIconController.aliasForDay(i);
                if (!activeAlias.equals(alias)) {
                    addIfNeeded(packageManager, settings,
                            LauncherIconController.component(context, alias),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, false);
                }
            }
            addIfNeeded(packageManager, settings,
                    LauncherIconController.component(context, LauncherIconController.BASE_COMPONENT),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, true);
            if (settings.isEmpty()) {
                return;
            }
            try {
                packageManager.setComponentEnabledSettings(settings);
            } catch (RuntimeException ignored) {
                LauncherIconController.applySequentially(
                        packageManager, context, activeAlias);
            }
        }

        private static void addIfNeeded(PackageManager packageManager, List<PackageManager.ComponentEnabledSetting> list, ComponentName componentName, int i, boolean z) {
            int componentEnabledSetting = packageManager.getComponentEnabledSetting(componentName);
            if (componentEnabledSetting == i) {
                return;
            }
            if (componentEnabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                if (i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED && z) {
                    return;
                }
                if (i == PackageManager.COMPONENT_ENABLED_STATE_DISABLED && !z) {
                    return;
                }
            }
            list.add(new PackageManager.ComponentEnabledSetting(
                    componentName, i, PackageManager.DONT_KILL_APP));
        }
    }
}
