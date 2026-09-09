package com.dtinh.lichviet;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

/** Applies the selected theme before Android or an OEM reads Activity resources. */
abstract class ThemedActivity extends Activity {
    @Override
    protected final void attachBaseContext(Context base) {
        super.attachBaseContext(ThemeManager.wrapActivityContext(base));
    }

    @Override
    protected void onCreate(Bundle state) {
        ThemeManager.applyActivityTheme(this);
        super.onCreate(state);
    }
}
