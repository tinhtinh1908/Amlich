package com.dtinh.lichviet;

import android.animation.ValueAnimator;
import androidx.activity.ComponentActivity;
import android.content.Context;
import android.os.Bundle;

abstract class ThemedActivity extends ComponentActivity {
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
