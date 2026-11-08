# Hướng dẫn build & phát hành

## Build
1. Mở thư mục này bằng **Android Studio** (bản mới nhất, JDK 17). Studio sẽ tự tạo Gradle wrapper (cần `gradle-8.11.1`+) và đồng bộ dự án.
   Hoặc chạy `gradle wrapper --gradle-version 8.11.1` rồi `./gradlew bundleRelease`.
2. `applicationId`: `com.fws.livebetter` · `compileSdk/targetSdk`: 36 (Google Play yêu cầu target API 36 cho app mới/bản cập nhật từ 31/08/2026) · `minSdk`: 26.

## Việc BẮT BUỘC trước khi đăng
- [ ] Thay **AdMob App ID** (`manifestPlaceholders["admobAppId"]`) và **Banner ID** (`ADMOB_BANNER_ID`) trong `app/build.gradle.kts` — hiện là ID thử nghiệm của Google.
- [ ] Bật **GitHub Pages** cho thư mục `docs/`, rồi sửa `PRIVACY_URL` trong `app/build.gradle.kts` và dán URL vào Play Console (Chính sách quyền riêng tư).
- [ ] Sửa `YOUR_EMAIL@example.com` trong `docs/privacy-policy.html`.
- [ ] Tạo keystore, ký **AAB** (Play App Signing).
- [ ] Play Console → **Data safety**: khai báo Advertising ID/thiết bị (AdMob), không thu thập dữ liệu cá nhân khác; **Target audience: 18+** (không dành cho trẻ em); khai báo **Ads = Có**; **Health apps declaration**: ứng dụng thông tin tham khảo, không phải thiết bị y tế.
- [ ] Chụp ảnh màn hình thật và đặt vào `store/screenshots/`; feature graphic và icon 512×512 đã có trong `store/`.
- [ ] Kiểm tra form đồng ý UMP bằng thiết bị thử trong vùng EEA (cấu hình tại AdMob → Privacy & messaging).

## Cập nhật dữ liệu sách
`app/src/main/assets/items.json` (767 mục; mỗi dòng: `[id, chương, số thứ tự, tiêu đề, xếp hạng, nói đơn giản, chi phí, lợi ích]`) và `topics.json` (4 nhóm, 34 chủ đề, tên 8 ngôn ngữ). Số tiền nhân dân tệ được lưu dưới dạng token `⟦số⟧` / `⟦từ~đến⟧` và được `Money.kt` quy đổi khi hiển thị.
Tỷ giá nằm ở `Money.kt` (enum `Cur`). VND và USD theo tỷ giá bạn cung cấp; các đồng còn lại là ước tính tham khảo — nên cập nhật trước khi phát hành.

## In-App Update
Flexible: `MainActivity.setupInAppUpdate()`. Chỉ hoạt động khi app được cài từ Google Play (thử bằng *Internal app sharing* hoặc track nội bộ với 2 versionCode khác nhau).
