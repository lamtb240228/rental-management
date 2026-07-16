# Nghiên cứu sản phẩm quản lý dãy trọ

Ngày rà soát: 16/07/2026.

## 1. Phạm vi và nguyên tắc

Nghiên cứu tập trung vào hệ thống quản lý dãy phòng trọ tại Việt Nam và các quy trình property/tenant management có thể áp dụng trực tiếp. Kết quả chỉ được dùng để xác định nghiệp vụ và ưu tiên sản phẩm; không sao chép mã nguồn, giao diện hoặc tài sản có bản quyền.

Phạm vi sản phẩm nên giữ gọn quanh chuỗi nghiệp vụ:

```text
Khu trọ/phòng
  -> người thuê và thành viên ở cùng
  -> hợp đồng, cọc và bàn giao
  -> điện nước/dịch vụ
  -> hóa đơn, công nợ và thanh toán
  -> trả phòng
  -> bảo trì và lịch sử vận hành
```

Không mở rộng P0/P1 sang môi giới, quản lý đầu tư, HOA, kế toán kép hoặc bất động sản thương mại.

## 2. Nguồn tham khảo

| Nguồn | Nội dung liên quan | Thời điểm nguồn |
| --- | --- | --- |
| [EasyTro](https://easytro.vn/) | Trạng thái phòng, người ở cùng, cọc, điện/nước/dịch vụ, hóa đơn, công nợ, VietQR, Zalo và phân quyền | Nội dung hiển thị gần nhất 08/06/2026; truy cập 16/07/2026 |
| [Resident](https://resident.vn/) | Hóa đơn hàng loạt, QR gạch nợ, phiếu thu/chi, tiền cọc/tiền thừa, phân quyền nhân viên và lịch sử thao tác | Truy cập 16/07/2026 |
| [TenantCloud - Account portals](https://support.tenantcloud.com/en/articles/12781485-account-portals-guide) | Portal cho landlord/manager, tenant và service professional | 10/03/2026 |
| [TenantCloud - Recurring transactions](https://support.tenantcloud.com/en/articles/11908555-how-do-i-set-the-number-of-days-early-to-post-the-recurring-transactions) | Phát hành khoản thu định kỳ trước hạn và lịch tạo giao dịch | 26/03/2026 |
| [TenantCloud - Move-in/move-out inspections](https://support.tenantcloud.com/en/articles/11934038-move-in-move-out-inspections) | Biên bản bàn giao/trả phòng có tình trạng, ảnh/video và chữ ký | 09/04/2026 |
| [TenantCloud - End a lease](https://support.tenantcloud.com/en/articles/11942679-how-do-i-end-a-lease) | Dừng khoản thu định kỳ, xử lý hóa đơn, kiểm tra trả phòng và kết thúc hợp đồng | 23/02/2026 |
| [Buildium](https://www.buildium.com/) | Leasing, payment/accounting, maintenance, resident portal, communication và document | Truy cập 16/07/2026 |
| [Buildium Resident Center](https://www.buildium.com/blog/tenant-portal-app-easy-for-rent-payments-and-maintenance/) | Thanh toán, maintenance và trao đổi tập trung qua portal | 06/04/2026 |
| [DoorLoop - Work order](https://support.doorloop.com/en/articles/6322778-create-a-work-order) | Assignee/vendor, ưu tiên, hạn, file, vật tư, nhân công và lịch sử work order | 02/06/2026 |
| [MicroRealEstate](https://github.com/microrealestate/microrealestate) | OSS MIT tham khảo cho property/tenant, hợp đồng mẫu, rent tracking, document/PDF/email và Docker Compose | Repository cập nhật 11/02/2026; release alpha gần nhất 14/11/2024 |
| [Open Condo](https://github.com/open-condo-software/condo) | OSS MIT tham khảo cho property, resident, ticket, payment, invoice, worker và object storage | Repository cập nhật 11/05/2026 |

Các trang SaaS là mô tả của nhà cung cấp, không phải kiểm chứng độc lập. Các repository OSS chỉ là nguồn tham khảo mô hình; project hiện tại tiếp tục giữ kiến trúc modular monolith đã chọn.

## 3. Vai trò phù hợp

| Vai trò | Phạm vi đề xuất |
| --- | --- |
| System Admin | Quản trị nền tảng SaaS, trạng thái tài khoản và sức khỏe hệ thống; không mặc nhiên đọc dữ liệu nghiệp vụ của khách hàng |
| Landlord | Toàn quyền trong dữ liệu khu trọ thuộc mình |
| Manager/Staff | Quyền được gán theo khu trọ và nhóm nghiệp vụ |
| Cashier/Accountant | Hóa đơn, công nợ, thanh toán và phiếu thu trong phạm vi được giao |
| Tenant chính | Hợp đồng, hóa đơn, thanh toán, thông báo và yêu cầu bảo trì của phòng đang thuê |
| Thành viên ở cùng | Hồ sơ cư trú và quyền portal hạn chế nếu được cấp tài khoản |
| Technician | Chỉ xem yêu cầu được giao và thông tin cần thiết để tiếp cận/xử lý phòng |

MVP hiện có `ADMIN`, `LANDLORD`, `TENANT`. `STAFF` và `TECHNICIAN` là P2 trừ khi có khách hàng thực tế cần phân công vận hành ngay.

## 4. Quy trình nghiệp vụ chuẩn

### 4.1. Vòng đời thuê phòng

```text
AVAILABLE
  -> RESERVED (nếu có đặt chỗ)
  -> hợp đồng DRAFT
  -> ký/kích hoạt + nhận cọc
  -> bàn giao + chỉ số đầu kỳ
  -> OCCUPIED
  -> gia hạn hoặc thông báo chấm dứt
  -> đối soát công nợ
  -> kiểm tra trả phòng
  -> cấn trừ/hoàn cọc
  -> AVAILABLE hoặc MAINTENANCE
```

Không nên đặt phòng về `AVAILABLE` ngay khi người dùng nhập một ngày kết thúc trong tương lai. Chuyển trạng thái phải theo thời điểm hiệu lực hoặc một tác vụ đã được kiểm soát.

### 4.2. Chu kỳ hóa đơn

```text
Chốt chỉ số
  -> kiểm tra tính liên tục
  -> snapshot đơn giá và dịch vụ
  -> tạo hóa đơn idempotent theo hợp đồng/kỳ
  -> phát hành
  -> gửi thông báo
  -> thanh toán một/nhiều lần
  -> biên nhận
  -> đối soát công nợ/quá hạn
```

Hóa đơn đã phát hành không được sửa âm thầm. Sai sót cần hủy/điều chỉnh có lý do và audit trail. Payment cần idempotency hoặc transaction reference duy nhất, khóa đồng thời và cập nhật invoice trong cùng transaction.

### 4.3. Bảo trì

```text
PENDING
  -> ACCEPTED/ASSIGNED
  -> IN_PROGRESS
  -> WAITING_FOR_PARTS hoặc WAITING_FOR_CONFIRMATION
  -> COMPLETED

PENDING/IN_PROGRESS -> CANCELLED (có lý do)
```

Timeline cần lưu người thao tác, thời điểm, ghi chú, ảnh và chi phí. Với phạm vi MVP, state machine hiện tại có thể giữ bốn trạng thái nhưng phải chặn mở lại trạng thái kết thúc và kiểm tra tenant thực sự liên quan tới phòng.

## 5. Đối chiếu baseline trước triển khai

### A. Đã có

- Đăng ký/đăng nhập JWT, tải hồ sơ hiện tại và phân quyền controller theo ba role.
- Khu trọ, phòng, người thuê, hợp đồng, chỉ số điện nước, hóa đơn/item, thanh toán và maintenance request.
- Dashboard cơ bản, admin summary và tenant portal.
- Owner scoping theo landlord trong phần lớn repository/service.
- PostgreSQL/Flyway, soft delete, validation cơ bản, OpenAPI và health endpoint.

### B. Có nhưng chưa hoàn chỉnh

- Khóa/mở account chưa thu hồi hiệu lực access token đã cấp.
- Lifecycle contract/room/tenant/property còn có thể phá invariant bằng update trực tiếp.
- Utility reading chưa kiểm tra kỳ trước và chưa khóa/liên kết chắc chắn sau khi lập hóa đơn.
- Invoice tạo thủ công; thiếu recurring generation, service price snapshot, locking và receipt.
- Payment có lịch sử nhưng client còn được chọn trạng thái và thiếu quy trình refund/adjustment.
- Maintenance thiếu assignment/technician/timeline/file và kiểm tra tenant-room đầy đủ.
- Dashboard, tenant portal, error/loading/empty state, responsive và test coverage mới ở mức prototype.

### C. Chưa có nhưng cần thiết

- Cọc theo sổ giao dịch; bàn giao/trả phòng; gia hạn/chấm dứt có hiệu lực.
- Bảng giá điện/nước/dịch vụ theo thời gian và hóa đơn định kỳ idempotent.
- Phiếu thu/biên nhận PDF.
- Search/filter/page/sort cho danh sách lớn.
- Audit log append-only cho quyền, hợp đồng và tài chính.
- Thông báo in-app/email nền tảng; tài liệu/ảnh có kiểm soát quyền.
- Backup/restore đã diễn tập, production profile, Docker image, CI và monitoring.
- Chính sách dữ liệu cá nhân, masking CCCD và thời hạn lưu dữ liệu.

### D. Nâng cao để làm sau

- VietQR/payment gateway và webhook đối soát.
- Zalo OA/SMS, e-signature và hóa đơn điện tử sau thẩm định pháp lý.
- Subscription/branding/quota cho SaaS.
- IoT/OCR công tơ, AI, accounting nâng cao và vendor marketplace.

## 6. Baseline bảo mật và vận hành

Sản phẩm thương mại nên dùng [OWASP ASVS](https://owasp.org/www-project-application-security-verification-standard/) Level 2 làm checklist tối thiểu. Các hướng dẫn áp dụng trực tiếp:

- [OWASP Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html): deny-by-default và kiểm tra quyền trên mọi request/object.
- [OWASP Multi-Tenant Security](https://cheatsheetseries.owasp.org/cheatsheets/Multi_Tenant_Security_Cheat_Sheet.html): tenant context bắt buộc và test cross-landlord IDOR.
- [OWASP Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html): tránh access/refresh token dài hạn trong `localStorage`; cân nhắc cookie `HttpOnly`, `Secure`, `SameSite` hoặc BFF.
- [OWASP File Upload](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html): allowlist, kiểm tra nội dung, giới hạn kích thước, tên ngẫu nhiên, object storage và signed URL.
- [OWASP Logging](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html): log auth/authorization/thay đổi tài chính nhưng không log secret hoặc dữ liệu cá nhân thô.
- [Docker Compose secrets](https://docs.docker.com/reference/compose-file/secrets/): chỉ cấp secret cho service cần sử dụng và không commit giá trị thật.
- [PostgreSQL Backup and Restore](https://www.postgresql.org/docs/current/backup.html): `pg_dump` định kỳ, lưu ngoài host và diễn tập restore; bổ sung WAL/PITR nếu RPO yêu cầu.

[Buildium Security Policy](https://www.buildium.com/security-policy/) và [DoorLoop Security](https://support.doorloop.com/en/articles/6341976-doorloop-security-features) cho thấy MFA, RBAC, encryption, backup và activity history là kỳ vọng cơ bản của thị trường.

Project xử lý CCCD/hộ chiếu, liên hệ, hợp đồng và thanh toán. [Luật Bảo vệ dữ liệu cá nhân 91/2025/QH15](https://vanban.chinhphu.vn/?classid=1&docid=214590&pageid=27160&typegroup=) được ban hành ngày 26/06/2025 và có hiệu lực từ 01/01/2026; trước khi triển khai thực tế cần rà soát pháp lý chuyên môn về mục đích thu thập, tối thiểu hóa dữ liệu, quyền chủ thể dữ liệu, retention và quy trình sự cố.

## 7. Cập nhật sau hardening

Các gap ở mục 5 là snapshot dùng để lập backlog. Working tree ngày 16/07/2026 đã xử lý token account bị khóa/xóa, bootstrap ADMIN và quarantine demo production, lifecycle/row-lock chính, continuity/linkage điện nước, tenant-room maintenance, tenant reading theo thời gian cư trú, payment locking, masking CCCD, production profile/Docker image, auth throttling/CSP, backup/restore runbook và health check. Các mục refresh/revoke/distributed rate limit, recurring invoice, service pricing, deposit/handover, receipt PDF, audit log, notification/file, scheduled off-site backup/monitoring và privacy governance vẫn còn trong backlog.
