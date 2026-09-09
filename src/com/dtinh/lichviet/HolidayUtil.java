package com.dtinh.lichviet;


public final class HolidayUtil {
    private HolidayUtil() {
    }

    public static String getHoliday(int i, int i2, LunarCalendar.LunarDate lunarDate) {
        // November is in the same Gregorian and Vietnamese lunar year.
        // The newly established observance starts in 2026.
        String strSolarHoliday = i == 24 && i2 == 11 && lunarDate.year < 2026
                ? "" : solarHoliday(i, i2);
        String strLunarHoliday = lunarDate.leap ? "" : lunarHoliday(lunarDate.day, lunarDate.month);
        if (strSolarHoliday.isEmpty() || strLunarHoliday.isEmpty()) {
            return !strSolarHoliday.isEmpty() ? strSolarHoliday : strLunarHoliday;
        }
        return strSolarHoliday + " · " + strLunarHoliday;
    }

    public static String getShortLabel(int i, int i2, LunarCalendar.LunarDate lunarDate) {
        if (!lunarDate.leap) {
            if (lunarDate.day == 1 && lunarDate.month == 1) {
                return "Tết";
            }
            if (lunarDate.day == 2 && lunarDate.month == 1) {
                return "Mùng 2 Tết";
            }
            if (lunarDate.day == 3 && lunarDate.month == 1) {
                return "Mùng 3 Tết";
            }
            if (lunarDate.day == 15 && lunarDate.month == 1) {
                return "Rằm tháng Giêng";
            }
            if (lunarDate.day == 10 && lunarDate.month == 3) {
                return "Giỗ Tổ";
            }
            if (lunarDate.day == 15 && lunarDate.month == 4) {
                return "Phật Đản";
            }
            if (lunarDate.day == 5 && lunarDate.month == 5) {
                return "Đoan Ngọ";
            }
            if (lunarDate.day == 15 && lunarDate.month == 7) {
                return "Vu Lan";
            }
            if (lunarDate.day == 15 && lunarDate.month == 8) {
                return "Trung Thu";
            }
            if (lunarDate.day == 23 && lunarDate.month == 12) {
                return "Táo quân";
            }
        }
        if (i == 1 && i2 == 1) {
            return "Tết DL";
        }
        if (i == 3 && i2 == 2) {
            return "Thành lập Đảng";
        }
        if (i == 27 && i2 == 2) {
            return "Thầy thuốc VN";
        }
        if (i == 8 && i2 == 3) {
            return "Quốc tế PN";
        }
        if (i == 26 && i2 == 3) {
            return "Thành lập Đoàn";
        }
        if (i == 30 && i2 == 4) {
            return "Thống nhất";
        }
        if (i == 1 && i2 == 5) {
            return "Quốc tế LĐ";
        }
        if (i == 7 && i2 == 5) {
            return "Điện Biên Phủ";
        }
        if (i == 19 && i2 == 5) {
            return "Sinh nhật Bác";
        }
        if (i == 1 && i2 == 6) {
            return "Thiếu nhi";
        }
        if (i == 21 && i2 == 6) {
            return "Báo chí VN";
        }
        if (i == 28 && i2 == 6) {
            return "Gia đình VN";
        }
        if (i == 27 && i2 == 7) {
            return "Thương binh LS";
        }
        if (i == 19 && i2 == 8) {
            return "Cách mạng T8";
        }
        if (i == 2 && i2 == 9) {
            return "Quốc khánh";
        }
        if (i == 10 && i2 == 10) {
            return "Giải phóng HN";
        }
        if (i == 13 && i2 == 10) {
            return "Doanh nhân VN";
        }
        if (i == 20 && i2 == 10) {
            return "Phụ nữ VN";
        }
        if (i == 9 && i2 == 11) {
            return "Pháp luật VN";
        }
        if (i == 20 && i2 == 11) {
            return "Nhà giáo VN";
        }
        if (i == 24 && i2 == 11 && lunarDate.year >= 2026) {
            return "Văn hóa VN";
        }
        if (i == 22 && i2 == 12) {
            return "QĐND VN";
        }
        if (lunarDate.leap || lunarDate.day != 15) {
            return lunarDate.day == 1 ? "1/" + lunarDate.month : Integer.toString(lunarDate.day);
        }
        return "Rằm";
    }

    private static String solarHoliday(int i, int i2) {
        if (i == 1 && i2 == 1) {
            return "Tết Dương lịch";
        }
        if (i == 3 && i2 == 2) {
            return "Ngày thành lập ĐCSVN";
        }
        if (i == 27 && i2 == 2) {
            return "Ngày Thầy thuốc Việt Nam";
        }
        if (i == 8 && i2 == 3) {
            return "Ngày Quốc tế Phụ nữ";
        }
        if (i == 26 && i2 == 3) {
            return "Ngày thành lập Đoàn TNCS Hồ Chí Minh";
        }
        if (i == 30 && i2 == 4) {
            return "Ngày Giải phóng miền Nam";
        }
        if (i == 1 && i2 == 5) {
            return "Ngày Quốc tế Lao động";
        }
        if (i == 7 && i2 == 5) {
            return "Ngày Chiến thắng Điện Biên Phủ";
        }
        if (i == 19 && i2 == 5) {
            return "Ngày sinh Chủ tịch Hồ Chí Minh";
        }
        if (i == 1 && i2 == 6) {
            return "Ngày Quốc tế Thiếu nhi";
        }
        if (i == 21 && i2 == 6) {
            return "Ngày Báo chí Cách mạng Việt Nam";
        }
        if (i == 28 && i2 == 6) {
            return "Ngày Gia đình Việt Nam";
        }
        if (i == 27 && i2 == 7) {
            return "Ngày Thương binh - Liệt sĩ";
        }
        if (i == 19 && i2 == 8) {
            return "Ngày Cách mạng Tháng Tám";
        }
        if (i == 2 && i2 == 9) {
            return "Quốc khánh Việt Nam";
        }
        if (i == 10 && i2 == 10) {
            return "Ngày Giải phóng Thủ đô";
        }
        if (i == 13 && i2 == 10) {
            return "Ngày Doanh nhân Việt Nam";
        }
        if (i == 20 && i2 == 10) {
            return "Ngày Phụ nữ Việt Nam";
        }
        if (i == 9 && i2 == 11) {
            return "Ngày Pháp luật Việt Nam";
        }
        if (i == 20 && i2 == 11) {
            return "Ngày Nhà giáo Việt Nam";
        }
        if (i == 24 && i2 == 11) {
            return "Ngày Văn hóa Việt Nam";
        }
        return (i == 22 && i2 == 12) ? "Ngày thành lập QĐND Việt Nam" : "";
    }

    private static String lunarHoliday(int i, int i2) {
        if (i == 1 && i2 == 1) {
            return "Tết Nguyên đán";
        }
        if (i == 2 && i2 == 1) {
            return "Mùng 2 Tết Nguyên đán";
        }
        if (i == 3 && i2 == 1) {
            return "Mùng 3 Tết Nguyên đán";
        }
        if (i == 15 && i2 == 1) {
            return "Rằm tháng Giêng";
        }
        if (i == 10 && i2 == 3) {
            return "Giỗ Tổ Hùng Vương";
        }
        if (i == 15 && i2 == 4) {
            return "Lễ Phật Đản";
        }
        if (i == 5 && i2 == 5) {
            return "Tết Đoan Ngọ";
        }
        if (i == 15 && i2 == 7) {
            return "Lễ Vu Lan";
        }
        if (i == 15 && i2 == 8) {
            return "Tết Trung Thu";
        }
        return (i == 23 && i2 == 12) ? "Ông Công Ông Táo" : "";
    }
}
