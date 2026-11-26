# Âm lịch Việt Nam
- Ứng dụng được làm hoàn toàn bằng các mô hình AI.
- Chạy offline hoàn toàn không cần internet, sẽ không có quảng cáo xuất hiện trong app.
- Tương thích và hỗ trợ tốt cho các thiết bị Xiaomi, các thiết bị khác vẫn có thể sử dụng nhưng không đầy đủ chức năng.
# Copyright © 2026 DTINH. All rights reserved.

## Giấy phép

- **Phần mới của DTINH từ bản 2.6.** Bạn có thể dùng ứng dụng. Muốn sao chép,
  dùng lại hoặc đăng lại phần này, hãy xin phép DTINH bằng văn bản và ghi
  nguồn **Âm Lịch Việt Nam — DTINH** kèm liên kết repo. [Điều khoản đầy đủ](LICENSE)
- **Apache 2.0.** Áp dụng cho mã đã phát hành theo giấy phép này và một số
  thành phần như Miuix, Material Icons và phông Breeze. Có thể dùng lại theo
  điều kiện của giấy phép, gồm giữ thông báo bản quyền và giấy phép.
  [Apache 2.0](licenses/Apache-2.0.txt)
- **SIL Open Font License 1.1.** Áp dụng cho các phông như Be Vietnam Pro,
  Inter, Manrope, Lora và Nunito. Có thể dùng và phân phối lại cùng thông
  báo giấy phép, theo điều kiện ghi trong từng tệp.
  [Giấy phép phông chữ](assets/font-licenses/)

## Giao diện native

Giao diện chính dùng Android Views. Lớp kính mềm trong app dùng miuix-glass
trên Android 13 trở lên. Các máy cũ dùng giao diện Android Views.

Build với JDK 17 và Android SDK Platform 37.2:

```sh
./gradlew assembleDebug
./gradlew assembleRelease
```

Thiết kế native tham khảo Miuix 0.9.4: góc squircle, kích thước component,
kiểu chữ và màu mặc định được mô phỏng bằng Java/Android Views. Các thông số
và nguồn đối chiếu nằm tại `licenses/miuix-native-design.md`.

## Phiên bản 2.8 (versionCode 51)

- Thêm đủ 8 mẫu widget từ **Cài đặt → Thêm widget ra màn hình chính** trong ứng dụng.
- Khôi phục kính mềm và góc bo của widget trên launcher HyperOS hỗ trợ.
- Trên HyperOS, bấm **Quyền thêm widget** rồi chọn **Quyền khác → Lối tắt màn hình chính → Luôn cho phép**. Nếu máy dùng tiếng Anh, chọn **Other permissions → Home screen shortcuts → Always allow**.

## Phiên bản 2.7 (versionCode 50)

- Khôi phục danh sách widget Âm Lịch trong mục Widget Android trên HyperOS.

## Phiên bản 2.6 (versionCode 49)

- Tối ưu chuyển tab và hiệu ứng xé lịch.
- Đồng bộ kính mềm của widget với thiết lập trong app. Lịch tuần 4×2 và
  lịch ngày 4×1 hiển thị ảnh nền đã chọn khi dùng chế độ thường.

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
hoặc truyền đường dẫn SDK và JDK cho Gradle Wrapper:

```sh
python3 tools/build_release.py --sdk /path/to/android-sdk --jdk /path/to/jdk-17
```

APK chưa ký nằm trong `build/outputs/apk/release/`. Phải ký APK trước khi cài;
muốn cập nhật bản đã phát hành thì dùng cùng khóa ký cũ. Script không dùng khóa
test, không bật debuggable và lấy package/phiên bản từ `build.gradle`.
