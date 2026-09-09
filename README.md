# Lịch Việt 2.2.0 — Java thuần

Bản này dùng repo `tinhtinh1908/Am-lich-Viet-Nam` v1.8.4 làm nền Gradle ổn
định và nhập toàn bộ chức năng từ source v2.0.0. Rust/JNI/NDK đã được loại bỏ;
lõi âm lịch chạy hoàn toàn bằng Java.

## Thay đổi 2.2.0

- Thêm tab Năm với 12 tháng, xếp 3 cột như ảnh tham chiếu.
- Chạm một tháng để mở lịch tháng có lịch âm và thông tin ngày hiện có.
- Đổi năm bằng nút trước/sau hoặc chạm tiêu đề năm để chọn 1900–2100.
- Thứ Bảy, Chủ nhật màu xanh; hôm nay có nền xanh; hỗ trợ sáng/tối.
- Giữ tab, năm đang xem và ngày được chọn khi Activity được tạo lại.
- Bổ sung 24/11 – Ngày Văn hóa Việt Nam, từ năm 2026, dùng chung cho app
  và widget. Không tự gán các ngày hoán đổi/nghỉ bù đang được đề xuất.
- Nguồn: https://baochinhphu.vn/ngay-van-hoa-viet-nam.html

## Build trên GitHub Actions

Workflow `.github/workflows/android.yml` chạy khi push vào `main` hoặc chọn
Run workflow. Runner cài JDK 17, SDK 34, chạy kiểm thử lịch âm/ngày lễ,
build APK debug và Android lint. Tải APK trong Artifacts của lần chạy.
APK này ký bằng khóa debug của runner, không dùng khóa phát hành cũ và
không bảo đảm cài đè được bản đã cài. Khóa ký riêng không được đưa lên repo.

## Chức năng

- Lịch dương/âm, Can Chi và ngày lễ Việt Nam.
- Ghi chú ngoại tuyến; sao lưu/khôi phục mã hóa qua Xiaomi Notes.
- Icon lịch động 1–31 và cập nhật lúc nửa đêm.
- Widget ngày dọc, ngày ngang và lịch tháng 4×4.
- Widget hoạt động độc lập; chạm nền không mở ứng dụng.
- Widget tháng chỉ giữ ba nút: tháng trước, hôm nay và tháng sau.
- Giao diện sáng/tối thống nhất; xử lý riêng lỗi màu hộp thoại trên HyperOS.
- Vòng chọn ngày của widget 4×4 là hình tròn 24×24 dp để không chạm dòng âm lịch.
- Số ngày trong lưới dùng nét medium 18 dp; widget 4×4 dùng 14 sp. Dòng âm
  lịch được tăng nhẹ nhưng vẫn giữ đủ khoảng trống trong từng ô.
- Ghi chú của lưới tháng được đọc thành một snapshot, không truy cập bộ nhớ
  42–84 lần trong từng frame animation.
- Sửa các ngày biên Sóc có thể trả về ngày âm bằng 0.
- Tự kiểm tra GitHub Release ở lần mở đầu tiên trong ngày. Chỉ ghi nhận ngày
  sau khi kiểm tra thành công; khi ngoại tuyến, ứng dụng im lặng và thử lại ở
  lần mở sau có mạng.
- Có màn Cài đặt riêng cho sao lưu và bật/tắt kiểm tra cập nhật.
- Có ba chế độ giao diện dùng chung cho app và widget: Tự động Sáng/Tối,
  Sáng và Tối.
- Người dùng có thể chọn, đổi hoặc xóa ảnh nền lịch bằng trình chọn tài liệu
  hệ thống; ứng dụng chỉ giữ quyền đọc đúng ảnh đã chọn.
- Ảnh nền được crop giữa, lấy mẫu đúng kích thước và phủ lớp tối để chữ luôn
  dễ đọc. Chỉ widget lịch tháng 4×4 dùng ảnh này; hai widget ngày giữ nền chủ
  đề mặc định. Bitmap widget được giới hạn kích thước để tránh lỗi RemoteViews.
- Tùy chọn Nền mờ chuyển thẻ và nút sang lớp bán trong suốt để thấy ảnh phía
  sau; trạng thái này được áp dụng đồng bộ cho app và nút của widget 4×4.
- Ảnh nền có palette riêng theo chủ đề: Sáng dùng một lớp trắng mờ nhẹ,
  nút/thẻ trắng trong và chữ tối; Tối dùng lớp đen mờ và chữ sáng. Widget 4×4
  đổi đồng bộ.
- Chuyển tháng dùng một animator 280 ms: phần đầu trang cố định, lưới lịch
  trượt theo hướng và thẻ thông tin chuyển đồng bộ.
- App chạy edge-to-edge nhưng tự chừa đúng vùng status bar/navigation bar.
  Hộp chọn ngày có lớp làm mờ phủ toàn màn hình và nền riêng để không chồng chữ.
- Khai báo category `APP_CALENDAR` và Intent mở mốc thời gian để Android nhận
  diện đây là ứng dụng lịch; không yêu cầu quyền đọc/ghi Calendar Provider.

## Yêu cầu

- JDK 17
- Android SDK 34
- Gradle 8.9 trở lên

Không cần Rust, Cargo, NDK hoặc `cargo-ndk`.

## Build

```bash
./gradlew clean assembleRelease
```

## Phát hành và tự kiểm tra cập nhật

1. Tăng `versionCode` và `versionName` trong `build.gradle`.
2. Tạo GitHub Release mới tại repo `tinhtinh1908/Amlich`; tag nên trùng
   `versionName`, ví dụ `2.2.0`.
3. Đính kèm ít nhất một file có đuôi `.apk` vào Release và xuất bản Release.

Ứng dụng gọi endpoint Release mới nhất tối đa một lần thành công trong mỗi ngày
theo giờ địa phương. Nếu tag mới hơn phiên bản đang cài, hộp thoại cập nhật sẽ
mở trực tiếp file APK đầu tiên; nếu Release không có APK thì ứng dụng mở trang
Release. Draft và prerelease không được endpoint `latest` trả về.

## Dùng thay ứng dụng Lịch Xiaomi

Ứng dụng được Android nhận diện là ứng dụng lịch qua category chuẩn
`APP_CALENDAR`. Android không cung cấp vai trò “lịch mặc định” chính thức, vì
vậy không có nút API để tự đặt làm mặc định.

Không đổi `applicationId` thành `com.android.calendar`: APK bên thứ ba không có
khóa ký của Xiaomi nên không thể cập nhật đè gói hệ thống. Các tích hợp Xiaomi
gọi đích danh `com.android.calendar` cũng không thể chuyển sang app này bằng
Intent chuẩn.

Khóa ký và mật khẩu không nằm trong repository. APK release cần được ký ở bước
phát hành bằng khóa riêng.

## Thông tin gói

- Package: `com.dtinh.lichviet`
- Version: `2.2.0` (`versionCode 43`)
- Min SDK: 30
- Target SDK: 34
