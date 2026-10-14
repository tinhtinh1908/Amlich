package com.dtinh.lichviet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Traditional almanac tables; month means lunar month, including leap months. */
public final class TraditionalCalendar {
    private static final String[] BRANCHES = {
        "Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi"
    };
    private static final String[] GUARDIANS = {
        "Thanh Long", "Minh Đường", "Thiên Hình", "Chu Tước", "Kim Quỹ", "Kim Đường",
        "Bạch Hổ", "Ngọc Đường", "Thiên Lao", "Huyền Vũ", "Tư Mệnh", "Câu Trần"
    };
    private static final String[] HOURS = {
        "110100101100", "001101001011", "110011010010",
        "101100110100", "001011001101", "010010110011"
    };

    private TraditionalCalendar() {}

    public static int dayBranch(LunarCalendar.LunarDate date) {
        return Math.floorMod(date.julianDay + 1, 12);
    }

    private static int guardianIndex(LunarCalendar.LunarDate date) {
        // Thanh Long starts at Tý in months 1/7, Dần in 2/8, etc.
        return Math.floorMod(dayBranch(date) - 2 * ((date.month - 1) % 6), 12);
    }

    public static boolean isAuspiciousDay(LunarCalendar.LunarDate date) {
        int index = guardianIndex(date);
        return index == 0 || index == 1 || index == 4 || index == 5 || index == 7 || index == 10;
    }

    public static String dayGuardian(LunarCalendar.LunarDate date) {
        return GUARDIANS[guardianIndex(date)];
    }

    public static String opposingBranch(LunarCalendar.LunarDate date) {
        return BRANCHES[(dayBranch(date) + 6) % 12];
    }

    public static List<String> auspiciousHours(LunarCalendar.LunarDate date) {
        String mask = HOURS[dayBranch(date) % 6];
        List<String> hours = new ArrayList<>(6);
        for (int branch = 0; branch < 12; branch++) {
            if (mask.charAt(branch) == '1') {
                int start = (branch * 2 + 23) % 24;
                int end = (branch * 2 + 1) % 24;
                hours.add(BRANCHES[branch] + "\n" + String.format(Locale.US, "%02d–%02dh", start, end));
            }
        }
        return hours;
    }
}
