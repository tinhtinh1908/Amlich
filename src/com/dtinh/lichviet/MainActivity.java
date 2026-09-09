package com.dtinh.lichviet;

import android.content.Intent;
import android.os.Bundle;



public final class MainActivity extends ThemedActivity {
    private CalendarMonthView calendarView;
    private int appliedThemeMode;
    private CalendarTabsView tabs;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        appliedThemeMode = ThemeManager.getMode(this);
        LauncherIconController.arm(this);
        CalendarMonthView calendarMonthView = new CalendarMonthView(this);
        this.calendarView = calendarMonthView;
        if (bundle != null && bundle.containsKey("selected_date")) {
            calendarView.showDate(bundle.getLong("selected_date"));
        }
        tabs = new CalendarTabsView(this, calendarView, bundle);
        setContentView(tabs);
        UiKit.applySystemBars(this, new UiKit.Palette(this),
                BackgroundImageManager.hasBackground(this));
        if (bundle == null) applyIntent(getIntent());
        UpdateChecker.check(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (tabs != null) tabs.saveState(out);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyIntent(intent);
    }

    private void applyIntent(Intent intent) {
        if (this.calendarView == null) {
            return;
        }
        if (XiaomiNotesBackup.handleIncomingShare(this, intent, new Runnable() {
            @Override
            public void run() {
                MainActivity.this.calendarView.invalidate();
            }
        })) {
            setIntent(new Intent(this, (Class<?>) MainActivity.class));
            return;
        }
        Long lResolveDateMillis = SystemCalendarCompat.resolveDateMillis(intent);
        if (lResolveDateMillis != null) {
            this.calendarView.showDate(lResolveDateMillis.longValue());
            if (tabs != null) tabs.showYear(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appliedThemeMode != ThemeManager.getMode(this)) {
            recreate();
            return;
        }
        LauncherIconController.arm(this);
        CalendarMonthView calendarMonthView = this.calendarView;
        if (calendarMonthView != null) {
            calendarMonthView.refreshTheme();
            calendarMonthView.refreshToday();
            if (tabs != null) tabs.refreshToday();
            UiKit.applySystemBars(this, new UiKit.Palette(this),
                    BackgroundImageManager.hasBackground(this));
        }
    }
}
