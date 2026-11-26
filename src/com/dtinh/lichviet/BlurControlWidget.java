package com.dtinh.lichviet;

public final class BlurControlWidget extends CalendarWidgetProvider {
    @Override
    protected int layoutId() {
        return R.layout.calendar_widget_blur_probe;
    }

    @Override
    protected Class<? extends CalendarWidgetProvider> providerClass() {
        return BlurControlWidget.class;
    }
}
