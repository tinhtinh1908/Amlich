package com.dtinh.lichviet;


public final class VerticalDateWidgetProvider extends CalendarWidgetProvider {
    @Override
    protected int layoutId() {
        return R.layout.calendar_widget_vertical;
    }

    @Override
    protected Class<? extends CalendarWidgetProvider> providerClass() {
        return VerticalDateWidgetProvider.class;
    }
}
