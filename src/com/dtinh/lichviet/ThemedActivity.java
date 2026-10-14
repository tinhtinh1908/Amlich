package com.dtinh.lichviet;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

abstract class ThemedActivity extends Activity {
    ThemedActivity() {
    }

    @Override
    protected final void attachBaseContext(Context context) {
        super.attachBaseContext(ThemeManager.wrapActivityContext(context));
    }

    @Override
    protected void onCreate(Bundle bundle) {
        ThemeManager.applyActivityTheme(this);
        super.onCreate(bundle);
        getWindow().setWindowAnimations(ValueAnimator.areAnimatorsEnabled() ? R.style.MiuixActivityAnimation : 0);
    }
}
