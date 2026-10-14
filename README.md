# Vui lòng ghi nguồn khi lấy
- Ứng dụng được làm hoàn toàn bằng các mô hình AI
# Âm lịch Việt Nam
- Chạy offline hoàn toàn không cần internet, sẽ không có quảng cáo xuất hiện trong app.
- Tương thích và hỗ trợ tốt cho các thiết bị Xiaomi, các thiết bị khác vẫn có thể sử dụng nhưng không đầy đủ chức năng.
# Copyright © 2026 DTINH. All rights reserved.

## Giao diện native

Giữ giao diện Android Views hiện có. Chỉ tham khảo thiết kế Miuix để chỉnh
màu sắc, bố cục, bo góc và chuyển động; không nhúng thư viện Compose Miuix.
Hai chế độ lịch, cài đặt, ghi chú, sao lưu mã hóa và widget được giữ lại.

Build với JDK 17, Android SDK 34 và Build Tools 34.0.0:

```sh
./gradlew assembleDebug
./gradlew assembleRelease
```

Thiết kế native tham khảo Miuix 0.9.4: góc squircle, kích thước component,
kiểu chữ và màu mặc định được mô phỏng bằng Java/Android Views. Các thông số
và nguồn đối chiếu nằm tại `licenses/miuix-native-design.md`. Không thêm
Compose hay thư viện Miuix vào dependency của app.

## Phiên bản 2.5 (versionCode 48)

- Lịch tháng/năm và lịch xé dùng giao diện Android Views thống nhất.
- Lịch xé có nền ảnh và hiệu ứng một tờ giấy cong/rơi, không có vụn giấy.
- Widget 1×2, 2×1, lịch tháng 3×3 và lịch xé 2×2 hỗ trợ ảnh nền.
- Giữ ghi chú, sao lưu mã hóa, đổi màu và mục ủng hộ VietQR.

Kiểm tra logic độc lập với emulator:

```sh
python3 tools/run_tests.py
```

Bản release không có cấu hình ký. Có thể build bằng `./gradlew assembleRelease`
hoặc dùng SDK/JDK đã cài, không cần tải Gradle:

```sh
python3 tools/build_release.py --sdk /path/to/android-sdk --jdk /path/to/jdk-17
```

APK chưa ký nằm trong `build/outputs/apk/release/`. Phải ký APK trước khi cài;
muốn cập nhật bản đã phát hành thì dùng cùng khóa ký cũ. Script không dùng khóa
test, không bật debuggable và lấy package/phiên bản từ `build.gradle`.
