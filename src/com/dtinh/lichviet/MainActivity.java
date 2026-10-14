package com.dtinh.lichviet;

import android.content.Intent;
import android.os.Bundle;

public final class MainActivity extends ThemedActivity {
    private CalendarMonthView calendarView;
    private int appliedThemeMode;
    private int appliedColorScheme;
    private CalendarTabsView tabs;
    private MinimalCalendarView minimalView;
    private boolean appliedMinimal;
    private long appliedBackgroundRevision;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.appliedThemeMode = ThemeManager.getMode(this);
        this.appliedColorScheme = ThemeManager.getColorScheme(this);
        this.appliedBackgroundRevision = BackgroundImageManager.revision(this);
        this.calendarView = new CalendarMonthView(this);
        if (bundle != null && bundle.containsKey("selected_date")) {
            this.calendarView.showDate(bundle.getLong("selected_date"));
        }
        this.appliedMinimal = ThemeManager.isMinimal(this);
        if (this.appliedMinimal) {
            this.minimalView = new MinimalCalendarView(this, this.calendarView);
            setContentView(this.minimalView);
        } else {
            this.tabs = new CalendarTabsView(this, this.calendarView, bundle);
            setContentView(this.tabs);
        }
        UiKit.applySystemBars(this, new UiKit.Palette(this), !this.appliedMinimal && BackgroundImageManager.hasBackground(this));
        UiKit.animateIn(this.appliedMinimal ? this.minimalView : this.tabs, 8.0f);
        if (bundle == null) {
            applyIntent(getIntent());
        }
        UpdateChecker.check(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.tabs == null) {
            if (this.calendarView != null) {
                bundle.putLong("selected_date", this.calendarView.getSelectedDateMillis());
                return;
            }
            return;
        }
        this.tabs.saveState(bundle);
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
                MainActivity.this.calendarView.refreshToday();
                if (MainActivity.this.minimalView != null) {
                    MainActivity.this.minimalView.refresh();
                }
            }
        })) {
            setIntent(new Intent(this, (Class<?>) MainActivity.class));
            return;
        }
        Long lResolveDateMillis = SystemCalendarCompat.resolveDateMillis(intent);
        if (lResolveDateMillis != null) {
            this.calendarView.showDate(lResolveDateMillis.longValue());
            if (this.tabs != null) {
                this.tabs.showYear(false);
            }
            if (this.minimalView != null) {
                this.minimalView.refresh();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.appliedThemeMode != ThemeManager.getMode(this) || this.appliedColorScheme != ThemeManager.getColorScheme(this) || this.appliedMinimal != ThemeManager.isMinimal(this)) {
            recreate();
            return;
        }
        LauncherIconController.arm(this);
        if (this.minimalView != null) {
            this.minimalView.refresh();
        }
        CalendarMonthView calendarMonthView = this.calendarView;
        if (calendarMonthView != null) {
            long jRevision = BackgroundImageManager.revision(this);
            if (this.appliedBackgroundRevision != jRevision) {
                this.appliedBackgroundRevision = jRevision;
                calendarMonthView.refreshTheme();
                if (this.tabs != null) {
                    this.tabs.refreshTheme();
                }
            }
            calendarMonthView.refreshToday();
            if (this.tabs != null) {
                this.tabs.refreshToday();
            }
            UiKit.applySystemBars(this, new UiKit.Palette(this), !this.appliedMinimal && BackgroundImageManager.hasBackground(this));
        }
    }
    @Override protected void onDestroy() {
        BackupUiDialog.dismiss(this);
        super.onDestroy();
    }

}
