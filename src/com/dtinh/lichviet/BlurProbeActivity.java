package com.dtinh.lichviet;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.util.Log;

public final class BlurProbeActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Class<?> provider = getIntent().getBooleanExtra("blur", true)
                ? BlurEnabledWidget.class : BlurControlWidget.class;
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        boolean requested = manager.requestPinAppWidget(new ComponentName(this, provider), null, null);
        Log.i("AmlichBlurProbe", "pin=" + provider.getSimpleName() + " requested=" + requested);
        finish();
    }
}
