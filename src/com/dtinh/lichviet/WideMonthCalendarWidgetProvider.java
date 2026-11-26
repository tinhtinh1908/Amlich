package com.dtinh.lichviet;

public final class WideMonthCalendarWidgetProvider extends MonthCalendarWidgetProvider {
    @Override protected boolean isWideMonth() { return true; }
    @Override protected boolean isCompact() { return true; }
}
