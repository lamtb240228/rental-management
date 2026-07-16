# Rental Management System Requirements

## 1. Project Overview

Rental Management System là hệ thống quản lý phòng trọ dành cho chủ trọ.

Hệ thống giúp chủ trọ quản lý:

- Khu trọ
- Phòng
- Người thuê
- Hợp đồng thuê
- Chỉ số điện và nước
- Hóa đơn
- Thanh toán
- Yêu cầu sửa chữa

Mục tiêu của dự án là giảm việc quản lý bằng giấy tờ hoặc bảng tính và tập trung dữ liệu vào một hệ thống duy nhất.

## 2. User Roles

### 2.1. Landlord

Landlord là chủ trọ và là người dùng chính của hệ thống.

Landlord có thể:

- Quản lý thông tin khu trọ
- Thêm, sửa và xem phòng
- Thêm và quản lý người thuê
- Tạo và quản lý hợp đồng thuê
- Nhập chỉ số điện nước
- Tạo hóa đơn
- Ghi nhận thanh toán
- Tiếp nhận và xử lý yêu cầu sửa chữa

### 2.2. Tenant

Tenant là người đang thuê phòng.

Tenant có thể:

- Đăng nhập hệ thống
- Xem thông tin cá nhân
- Xem thông tin phòng đang thuê
- Xem hợp đồng của mình
- Xem hóa đơn
- Xem lịch sử thanh toán
- Gửi yêu cầu sửa chữa

### 2.3. Administrator

Administrator là người quản lý toàn bộ hệ thống.

Administrator có thể:

- Quản lý tài khoản chủ trọ
- Khóa hoặc mở khóa tài khoản
- Theo dõi trạng thái hệ thống
- Xem số liệu vận hành tổng hợp đã được giới hạn dữ liệu nhạy cảm

Administrator là quản trị nền tảng và không mặc nhiên được đọc/sửa dữ liệu nghiệp vụ chi tiết của mọi chủ trọ. Mọi quyền hỗ trợ truy cập dữ liệu khách hàng trong tương lai phải có scope, lý do, audit và cơ chế phê duyệt rõ ràng.

Chức năng Administrator chưa phải ưu tiên trong phiên bản MVP đầu tiên.

## 3. MVP Functional Requirements

### 3.1. Authentication

- Người dùng có thể đăng nhập bằng email và mật khẩu
- Người dùng có thể đăng xuất
- Hệ thống xác định quyền dựa trên vai trò
- Người dùng không được truy cập chức năng không thuộc quyền của mình
- Mật khẩu phải được mã hóa trước khi lưu vào database

### 3.2. Property Management

Landlord có thể:

- Tạo khu trọ
- Xem danh sách khu trọ
- Cập nhật thông tin khu trọ
- Ngừng sử dụng khu trọ

Thông tin khu trọ gồm:

- Tên khu trọ
- Địa chỉ
- Mô tả
- Trạng thái

### 3.3. Room Management

Landlord có thể:

- Thêm phòng vào khu trọ
- Xem danh sách phòng
- Cập nhật thông tin phòng
- Xem trạng thái phòng

Thông tin phòng gồm:

- Số hoặc tên phòng
- Diện tích
- Giá thuê
- Tiền đặt cọc mặc định
- Trạng thái phòng
- Số người tối đa

### 3.4. Tenant Management

Landlord có thể:

- Thêm người thuê
- Xem danh sách người thuê
- Cập nhật thông tin người thuê
- Xem lịch sử thuê phòng

Thông tin người thuê gồm:

- Họ và tên
- Ngày sinh
- Số điện thoại
- Email
- Số CCCD hoặc giấy tờ tùy thân
- Địa chỉ thường trú

### 3.5. Rental Contract Management

Landlord có thể:

- Tạo hợp đồng thuê
- Chọn phòng và người thuê
- Thiết lập ngày bắt đầu
- Thiết lập ngày kết thúc
- Thiết lập giá thuê
- Thiết lập tiền đặt cọc
- Kết thúc hợp đồng

### 3.6. Utility Management

Landlord có thể:

- Nhập chỉ số điện theo tháng
- Nhập chỉ số nước theo tháng
- Xem lượng điện nước đã sử dụng
- Thiết lập đơn giá điện và nước

### 3.7. Invoice Management

Landlord có thể:

- Tạo hóa đơn hàng tháng
- Thêm tiền phòng
- Thêm tiền điện
- Thêm tiền nước
- Thêm các khoản phí khác
- Xem trạng thái hóa đơn

Tenant có thể:

- Xem hóa đơn của mình
- Xem chi tiết các khoản phí

### 3.8. Payment Management

Landlord có thể:

- Ghi nhận khoản thanh toán
- Xem lịch sử thanh toán
- Xác định hóa đơn đã thanh toán hoặc chưa thanh toán

Tenant có thể:

- Xem lịch sử thanh toán của mình

### 3.9. Maintenance Request Management

Tenant có thể:

- Gửi yêu cầu sửa chữa
- Mô tả vấn đề
- Theo dõi trạng thái xử lý

Landlord có thể:

- Xem yêu cầu sửa chữa
- Cập nhật trạng thái xử lý
- Ghi chú kết quả xử lý

## 4. Business Rules

### 4.1. Property and Room

- Một landlord có thể quản lý nhiều khu trọ
- Một khu trọ thuộc về một landlord
- Một khu trọ có thể có nhiều phòng
- Một phòng chỉ thuộc về một khu trọ
- Số phòng không được trùng trong cùng một khu trọ

### 4.2. Room Status

Phòng có thể có một trong các trạng thái:

- AVAILABLE: phòng đang trống
- OCCUPIED: phòng đang có người thuê
- MAINTENANCE: phòng đang sửa chữa
- INACTIVE: phòng ngừng sử dụng

### 4.3. Tenant

- Một người thuê có thể có nhiều hợp đồng theo thời gian
- Một người thuê không được xem dữ liệu của người thuê khác
- Thông tin giấy tờ tùy thân không được trùng giữa các người thuê đang hoạt động

### 4.4. Rental Contract

- Một phòng không được có hai hợp đồng đang hoạt động cùng thời điểm
- Ngày kết thúc phải sau ngày bắt đầu
- Hợp đồng phải liên kết với một phòng
- Hợp đồng phải có ít nhất một người thuê
- Khi hợp đồng có hiệu lực, trạng thái phòng chuyển thành OCCUPIED
- Khi hợp đồng kết thúc và không còn hợp đồng hoạt động, trạng thái phòng chuyển thành AVAILABLE

### 4.5. Utility Reading

- Mỗi phòng chỉ có một bản ghi điện nước cho mỗi tháng
- Chỉ số mới không được nhỏ hơn chỉ số cũ
- Lượng sử dụng bằng chỉ số mới trừ chỉ số cũ

### 4.6. Invoice

- Mỗi hợp đồng chỉ có một hóa đơn chính cho mỗi tháng
- Tổng tiền hóa đơn bằng tổng tất cả khoản phí
- Hóa đơn chưa thanh toán có trạng thái UNPAID
- Hóa đơn thanh toán một phần có trạng thái PARTIALLY_PAID
- Hóa đơn thanh toán đủ có trạng thái PAID
- Hóa đơn quá hạn có thể có trạng thái OVERDUE

### 4.7. Payment

- Một hóa đơn có thể được thanh toán nhiều lần
- Tổng số tiền thanh toán không được âm
- Mỗi lần thanh toán phải lưu ngày thanh toán và số tiền
- Trạng thái hóa đơn được cập nhật dựa trên tổng số tiền đã thanh toán

### 4.8. Maintenance Request

Yêu cầu sửa chữa có thể có các trạng thái:

- PENDING
- IN_PROGRESS
- COMPLETED
- CANCELLED

## 5. Out of Scope for Initial MVP

Các chức năng sau chưa được triển khai trong phiên bản đầu tiên:

- Thanh toán trực tuyến
- Ứng dụng điện thoại
- Chat thời gian thực
- Trí tuệ nhân tạo
- Quản lý kế toán nâng cao
- Quản lý nhân viên
- Hợp đồng có chữ ký điện tử
- Hỗ trợ nhiều ngôn ngữ
- Tích hợp thiết bị điện nước thông minh
- Báo cáo và biểu đồ nâng cao
- Gửi thông báo SMS
