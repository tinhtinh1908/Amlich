package com.dtinh.lichviet;


public final class HorizontalDateWidgetProvider extends CalendarWidgetProvider {
    @Override
    protected int layoutId() {
        return R.layout.calendar_widget;
    }

    @Override
    protected Class<? extends CalendarWidgetProvider> providerClass() {
        return HorizontalDateWidgetProvider.class;
    }
}
