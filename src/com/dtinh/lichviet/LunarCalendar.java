package com.dtinh.lichviet;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;


public final class LunarCalendar {
    public static final double VIETNAM_TIME_ZONE = 7.0d;
    private static final String[] CAN = {"Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý"};
    private static final String[] CHI = {"Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi"};
    private static final Map<Integer, LunarDate> CACHE = new LinkedHashMap<Integer, LunarDate>(384, 0.75f, true) {
        private static final long serialVersionUID = 1;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, LunarDate> entry) {
            return size() > 384;
        }
    };

    private LunarCalendar() {
    }

    public static final class LunarDate {
        public final int day;
        public final int julianDay;
        public final boolean leap;
        public final int month;
        public final int year;

        LunarDate(int day, int month, int year, boolean leap, int julianDay) {
            this.day = day;
            this.month = month;
            this.year = year;
            this.leap = leap;
            this.julianDay = julianDay;
        }

        public String shortText() {
            return String.format(Locale.US, "%d/%d%s", day, month, leap ? " N" : "");
        }

        public String fullText() {
            return String.format(Locale.US, "Ngày %d tháng %d%s năm %d",
                    day, month, leap ? " nhuận" : "", year);
        }
    }

    public static LunarDate fromSolar(int day, int month, int year) {
        int cacheKey = (year * 512) + (month * 32) + day;
        synchronized (CACHE) {
            LunarDate cached = CACHE.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }
        LunarDate calculated = calculate(day, month, year);
        synchronized (CACHE) {
            LunarDate existing = CACHE.get(cacheKey);
            if (existing != null) {
                return existing;
            }
            CACHE.put(cacheKey, calculated);
        }
        return calculated;
    }

    private static LunarDate calculate(int solarDay, int solarMonth, int solarYear) {
        int dayNumber = jdFromDate(solarDay, solarMonth, solarYear);
        int moonIndex = (int) Math.floor((dayNumber - 2415021.076998695d) / 29.530588853d) + 1;
        int monthStart = getNewMoonDay(moonIndex, VIETNAM_TIME_ZONE);

        // The mean-lunation estimate can be more than one index away close to
        // midnight. Walk to the actual new-moon interval instead of falling
        // back only once. This prevents invalid lunar day 0 on 07/05/2054 and
        // 09/04/2062 in the supported 1900-2100 range.
        while (monthStart > dayNumber) {
            moonIndex--;
            monthStart = getNewMoonDay(moonIndex, VIETNAM_TIME_ZONE);
        }
        while (getNewMoonDay(moonIndex + 1, VIETNAM_TIME_ZONE) <= dayNumber) {
            moonIndex++;
            monthStart = getNewMoonDay(moonIndex, VIETNAM_TIME_ZONE);
        }

        int a11 = getLunarMonth11(solarYear, VIETNAM_TIME_ZONE);
        int b11 = a11;
        int lunarYear;
        if (a11 >= monthStart) {
            lunarYear = solarYear;
            a11 = getLunarMonth11(solarYear - 1, VIETNAM_TIME_ZONE);
        } else {
            lunarYear = solarYear + 1;
            b11 = getLunarMonth11(solarYear + 1, VIETNAM_TIME_ZONE);
        }

        int lunarDay = dayNumber - monthStart + 1;
        int monthOffset = (int) Math.floor((monthStart - a11) / 29.0d);
        int lunarMonth = monthOffset + 11;
        boolean leap = false;

        if (b11 - a11 > 365) {
            int leapMonthOffset = getLeapMonthOffset(a11, VIETNAM_TIME_ZONE);
            if (monthOffset >= leapMonthOffset) {
                lunarMonth = monthOffset + 10;
                leap = monthOffset == leapMonthOffset;
            }
        }

        if (lunarMonth > 12) {
            lunarMonth -= 12;
        }
        if (lunarMonth >= 11 && monthOffset < 4) {
            lunarYear--;
        }
        return new LunarDate(lunarDay, lunarMonth, lunarYear, leap, dayNumber);
    }

    public static String yearCanChi(int i) {
        return CAN[positiveMod(i + 6, 10)] + " " + CHI[positiveMod(i + 8, 12)];
    }

    public static String dayCanChi(int i) {
        return CAN[positiveMod(i + 9, 10)] + " " + CHI[positiveMod(i + 1, 12)];
    }

    public static String monthCanChi(int i, int i2) {
        return CAN[positiveMod((i2 * 12) + i + 3, 10)] + " " + CHI[positiveMod(i + 1, 12)];
    }

    private static int positiveMod(int i, int i2) {
        int i3 = i % i2;
        return i3 < 0 ? i3 + i2 : i3;
    }

    private static int jdFromDate(int i, int i2, int i3) {
        int i4 = (14 - i2) / 12;
        int i5 = (i3 + 4800) - i4;
        int i6 = i + (((((i2 + (i4 * 12)) - 3) * 153) + 2) / 5) + (i5 * 365) + (i5 / 4);
        int i7 = ((i6 - (i5 / 100)) + (i5 / 400)) - 32045;
        if (i7 < 2299161) {
            return i6 - 32083;
        }
        return i7;
    }

    private static int getNewMoonDay(int i, double d) {
        double d2;
        double d3 = i;
        double d4 = d3 / 1236.85d;
        double d5 = d4 * d4;
        double d6 = d5 * d4;
        double dSin = ((((29.53058868d * d3) + 2415020.75933d) + (1.178E-4d * d5)) - (1.55E-7d * d6)) + (Math.sin((((132.87d * d4) + 166.56d) - (0.009173d * d5)) * 0.017453292519943295d) * 3.3E-4d);
        double d7 = (((29.10535608d * d3) + 359.2242d) - (3.33E-5d * d5)) - (3.47E-6d * d6);
        double d8 = (385.81691806d * d3) + 306.0253d + (0.0107306d * d5) + (1.236E-5d * d6);
        double d9 = d8 * 2.0d;
        double d10 = ((((d3 * 390.67050646d) + 21.2964d) - (0.0016528d * d5)) - (2.39E-6d * d6)) * 2.0d;
        double dSin2 = ((((((((((((0.1734d - (3.93E-4d * d4)) * Math.sin(d7 * 0.017453292519943295d)) + (Math.sin((d7 * 2.0d) * 0.017453292519943295d) * 0.0021d)) - (Math.sin(d8 * 0.017453292519943295d) * 0.4068d)) + (Math.sin(d9 * 0.017453292519943295d) * 0.0161d)) - (Math.sin((3.0d * d8) * 0.017453292519943295d) * 4.0E-4d)) + (Math.sin(d10 * 0.017453292519943295d) * 0.0104d)) - (Math.sin((d7 + d8) * 0.017453292519943295d) * 0.0051d)) - (Math.sin((d7 - d8) * 0.017453292519943295d) * 0.0074d)) + (Math.sin((d10 + d7) * 0.017453292519943295d) * 4.0E-4d)) - (Math.sin((d10 - d7) * 0.017453292519943295d) * 4.0E-4d)) - (Math.sin((d10 + d8) * 0.017453292519943295d) * 6.0E-4d)) + (Math.sin((d10 - d8) * 0.017453292519943295d) * 0.001d) + (Math.sin((d9 + d7) * 0.017453292519943295d) * 5.0E-4d);
        if (d4 < -11.0d) {
            d2 = ((((8.39E-4d * d4) + 0.001d) + (d5 * 2.261E-4d)) - (8.45E-6d * d6)) - ((d4 * 8.1E-8d) * d6);
        } else {
            d2 = ((d4 * 2.65E-4d) - 2.78E-4d) + (d5 * 2.62E-4d);
        }
        return (int) Math.floor(((dSin + dSin2) - d2) + 0.5d + (d / 24.0d));
    }

    private static int getSunLongitude(int i, double d) {
        double d2 = ((((double) i) - 2451545.5d) - (d / 24.0d)) / 36525.0d;
        double d3 = d2 * d2;
        double d4 = (((35999.0503d * d2) + 357.5291d) - (1.559E-4d * d3)) - ((4.8E-7d * d2) * d3);
        double dSin = ((36000.76983d * d2) + 280.46645d + (3.032E-4d * d3) + (((1.9146d - (0.004817d * d2)) - (d3 * 1.4E-5d)) * Math.sin(d4 * 0.017453292519943295d)) + ((0.019993d - (d2 * 1.01E-4d)) * Math.sin(0.03490658503988659d * d4)) + (Math.sin(0.05235987755982989d * d4) * 2.9E-4d)) * 0.017453292519943295d;
        return (int) Math.floor(((dSin - (Math.floor(dSin / 6.283185307179586d) * 6.283185307179586d)) / 3.141592653589793d) * 6.0d);
    }

    private static int getLunarMonth11(int i, double d) {
        int iFloor = (int) Math.floor(((double) (jdFromDate(31, 12, i) - 2415021)) / 29.530588853d);
        int newMoonDay = getNewMoonDay(iFloor, d);
        return getSunLongitude(newMoonDay, d) >= 9 ? getNewMoonDay(iFloor - 1, d) : newMoonDay;
    }

    private static int getLeapMonthOffset(int i, double d) {
        int iFloor = (int) Math.floor(((((double) i) - 2415021.076998695d) / 29.530588853d) + 0.5d);
        int sunLongitude = getSunLongitude(getNewMoonDay(iFloor + 1, d), d);
        int i2 = 1;
        while (true) {
            i2++;
            int sunLongitude2 = getSunLongitude(getNewMoonDay(iFloor + i2, d), d);
            if (sunLongitude2 == sunLongitude || i2 >= 14) {
                break;
            }
            sunLongitude = sunLongitude2;
        }
        return i2 - 1;
    }
}
