import com.dtinh.lichviet.LunarCalendar;
import com.dtinh.lichviet.TraditionalCalendar;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public final class TraditionalCalendarTest {
    private static LunarCalendar.LunarDate date(int d, int m, int y) {
        return LunarCalendar.fromSolar(d, m, y);
    }
    private static void expect(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        // 19/02/2026 = 3/1 Bính Ngọ, Giáp Tý, Thanh Long.
        LunarCalendar.LunarDate ty = date(19, 2, 2026);
        expect(ty.day == 3 && ty.month == 1, "Tết reference date");
        expect(TraditionalCalendar.isAuspiciousDay(ty), "Tý in lunar month 1");
        expect(TraditionalCalendar.dayGuardian(ty).equals("Thanh Long"), "Thanh Long reference");
        expect(TraditionalCalendar.opposingBranch(ty).equals("Ngọ"), "Tý opposes Ngọ");
        List<String> hours = TraditionalCalendar.auspiciousHours(ty);
        expect(hours.toString().equals("[Tý\n23–01h, Sửu\n01–03h, Mão\n05–07h, Ngọ\n11–13h, Thân\n15–17h, Dậu\n17–19h]"), "Giáp Tý six hours");
        expect(TraditionalCalendar.dayGuardian(date(20, 2, 2026)).equals("Minh Đường"), "Next guardian");
        expect(!TraditionalCalendar.isAuspiciousDay(date(21, 2, 2026)), "Thiên Hình is hắc đạo");
        expect(LunarCalendar.solarTerm(date(22, 3, 2026)).equals("Xuân phân"), "Spring equinox");
        expect(LunarCalendar.solarTerm(date(23, 6, 2026)).equals("Hạ chí"), "Summer solstice");
        expect(LunarCalendar.solarTerm(date(24, 9, 2026)).equals("Thu phân"), "Autumn equinox");
        expect(LunarCalendar.solarTerm(date(23, 12, 2026)).equals("Đông chí"), "Winter solstice");
        Calendar day = Calendar.getInstance(TimeZone.getTimeZone("UTC")); day.clear(); day.set(1900, 0, 1);
        Calendar end = (Calendar) day.clone(); end.set(2101, 0, 1);
        int good = 0, count = 0;
        while (day.before(end)) {
            LunarCalendar.LunarDate lunar = date(day.get(Calendar.DAY_OF_MONTH), day.get(Calendar.MONTH) + 1, day.get(Calendar.YEAR));
            List<String> dailyHours = TraditionalCalendar.auspiciousHours(lunar);
            expect(dailyHours.size() == 6, "Six double-hours " + day.getTime());
            expect(new java.util.HashSet<>(dailyHours).size() == 6, "Unique hours");
            expect(!LunarCalendar.solarTerm(lunar).isEmpty(), "Nonempty term");
            if (TraditionalCalendar.isAuspiciousDay(lunar)) good++;
            count++; day.add(Calendar.DAY_OF_MONTH, 1);
        }
        expect(good > count * .49 && good < count * .51, "Six of twelve guardians are hoàng đạo");
        System.out.println("Traditional calendar tests passed for " + count + " days");
    }
}
