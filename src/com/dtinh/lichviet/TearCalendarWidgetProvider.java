package com.dtinh.lichviet;

/** Compact tear-calendar card; shares date updates, photos and click behavior. */
public final class TearCalendarWidgetProvider extends CalendarWidgetProvider {
    @Override protected int layoutId() { return R.layout.calendar_widget_tear; }
    @Override protected Class<? extends CalendarWidgetProvider> providerClass() {
        return TearCalendarWidgetProvider.class;
    }
}
