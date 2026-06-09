# Warehouse Mobile

App Android tối giản bằng Java thuần, Gradle Kotlin DSL, kết nối server sẵn có.

## Scope

- Đăng nhập bằng `/api/auth/login`
- Xem health server bằng `/api/health`
- Xem danh sách thông báo bằng `/api/notifications`
- Xem danh sách tài sản bằng `/api/assets`
- Xem danh sách checklist bằng `/api/checklists/results`

## Chạy thử

1. Mở Android Studio.
2. Chọn `File > Open...` và trỏ vào thư mục `mobile`.
3. Chờ Android Studio sync Gradle xong.
4. Chọn một device emulator hoặc cắm điện thoại thật, bật USB debugging.
5. Nếu chạy trên emulator, giữ `API_BASE_URL` là `http://10.0.2.2:4000/api/`.
6. Nếu chạy trên điện thoại thật, đổi `API_BASE_URL` trong [app/build.gradle.kts](app/build.gradle.kts) sang IP máy đang chạy server, ví dụ `http://192.168.1.10:4000/api/`.
7. Bấm `Run` trong Android Studio.

## Lưu ý

- Dự án này cố ý chỉ làm 3 chức năng để dễ nộp đồ án: đăng nhập, health check, và thông báo.
- Nếu server của bạn chạy port khác `4000`, đổi lại ngay trong [app/build.gradle.kts](app/build.gradle.kts).
- Lỗi AAR metadata của `RecyclerView` đã được xử lý bằng cách dùng `androidx.recyclerview:recyclerview:1.3.2` để giữ `compileSdk = 34`.
