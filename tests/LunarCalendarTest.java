import com.dtinh.lichviet.LunarCalendar;
import com.dtinh.lichviet.HolidayUtil;

import java.util.Calendar;
import java.util.TimeZone;

public final class LunarCalendarTest {
    private static void expect(int solarDay, int solarMonth, int solarYear,
                               int lunarDay, int lunarMonth, int lunarYear) {
        LunarCalendar.LunarDate actual = LunarCalendar.fromSolar(solarDay, solarMonth, solarYear);
        if (actual.day != lunarDay || actual.month != lunarMonth || actual.year != lunarYear || actual.leap) {
            throw new AssertionError(solarDay + "/" + solarMonth + "/" + solarYear
                    + " expected " + lunarDay + "/" + lunarMonth + "/" + lunarYear
                    + " but was " + actual.shortText() + "/" + actual.year);
        }
    }

    public static void main(String[] args) {
        expect(10, 2, 2024, 1, 1, 2024);
        expect(29, 1, 2025, 1, 1, 2025);
        expect(17, 2, 2026, 1, 1, 2026);
        expect(7, 5, 2054, 30, 3, 2054);
        expect(8, 5, 2054, 1, 4, 2054);
        expect(9, 4, 2062, 30, 2, 2062);
        expect(10, 4, 2062, 1, 3, 2062);
        expectLeap(22, 3, 2023, 1, 2, 2023);
        LunarCalendar.LunarDate today = LunarCalendar.fromSolar(22, 7, 2026);
        expectHoliday(2, 9, 2026, "Quốc khánh Việt Nam");
        expectHoliday(18, 4, 2024, "Giỗ Tổ Hùng Vương");
        expectHoliday(17, 2, 2026, "Tết Nguyên đán");
        expectHoliday(24, 11, 2026, "Ngày Văn hóa Việt Nam");
        expectHoliday(24, 11, 2027, "Ngày Văn hóa Việt Nam");
        if (HolidayUtil.getHoliday(24, 11, LunarCalendar.fromSolar(24, 11, 2025))
                .contains("Ngày Văn hóa Việt Nam")) {
            throw new AssertionError("New observance must not appear before 2026");
        }
        if (!HolidayUtil.getShortLabel(24, 11, LunarCalendar.fromSolar(24, 11, 2026))
                .equals("Văn hóa VN")) {
            throw new AssertionError("Missing short label for Culture Day");
        }
        validateSupportedRange();
        System.out.println("22/7/2026 -> " + today.fullText());
        System.out.println("All lunar tests passed");
    }

    private static void validateSupportedRange() {
        Calendar date = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        date.clear();
        date.set(1900, Calendar.JANUARY, 1);
        Calendar end = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        end.clear();
        end.set(2101, Calendar.JANUARY, 1);
        while (date.before(end)) {
            LunarCalendar.LunarDate lunar = LunarCalendar.fromSolar(
                    date.get(Calendar.DAY_OF_MONTH),
                    date.get(Calendar.MONTH) + 1,
                    date.get(Calendar.YEAR));
            if (lunar.day < 1 || lunar.day > 30 || lunar.month < 1 || lunar.month > 12) {
                throw new AssertionError("Invalid lunar date for " + date.getTime()
                        + ": " + lunar.shortText() + "/" + lunar.year);
            }
            date.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private static void expectLeap(int solarDay, int solarMonth, int solarYear,
                                   int lunarDay, int lunarMonth, int lunarYear) {
        LunarCalendar.LunarDate actual = LunarCalendar.fromSolar(solarDay, solarMonth, solarYear);
        if (actual.day != lunarDay || actual.month != lunarMonth || actual.year != lunarYear || !actual.leap) {
            throw new AssertionError(solarDay + "/" + solarMonth + "/" + solarYear
                    + " expected leap " + lunarDay + "/" + lunarMonth + "/" + lunarYear
                    + " but was " + actual.shortText() + "/" + actual.year);
        }
    }

    private static void expectHoliday(int day, int month, int year, String expected) {
        LunarCalendar.LunarDate lunar = LunarCalendar.fromSolar(day, month, year);
        String actual = HolidayUtil.getHoliday(day, month, lunar);
        if (!actual.contains(expected)) {
            throw new AssertionError(day + "/" + month + "/" + year
                    + " expected holiday " + expected + " but was " + actual);
        }
    }
}
