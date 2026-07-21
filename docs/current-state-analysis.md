# Phân tích hiện trạng hệ thống

Ngày audit: 16/07/2026
Baseline ban đầu: branch `test`, commit `889e059`.

Tài liệu này ghi lại trạng thái được quan sát trước khi triển khai các thay đổi tiếp theo. Nó không thay thế `requirements.md`, `database-design.md` hoặc `database-schema.md`.

## 1. Mục tiêu dự án

Rental Management System là ứng dụng quản lý dãy phòng trọ theo mô hình nhiều chủ trọ. Mỗi landlord chỉ được truy cập khu trọ, phòng, tenant, hợp đồng, hóa đơn, thanh toán và yêu cầu bảo trì thuộc phạm vi mình quản lý. Tenant có portal riêng; system administrator quản lý tài khoản và trạng thái nền tảng.

Kiến trúc hiện tại là modular monolith backend kết hợp React SPA và PostgreSQL. Đây là lựa chọn phù hợp với quy mô sản phẩm; chưa có lý do để chuyển sang microservice.

## 2. Công nghệ và convention

### Backend

- Java 21, Spring Boot 3.5.15 và Maven Wrapper.
- Spring MVC REST, Spring Data JPA/Hibernate và Jakarta Validation.
- Spring Security stateless JWT với method security.
- PostgreSQL 16 và Flyway; Hibernate dùng `ddl-auto: validate`.
- Springdoc OpenAPI và Actuator.
- JUnit 5, Spring Security Test và Testcontainers.
- Package-by-feature: `admin`, `auth`, `billing`, `contract`, `dashboard`, `maintenance`, `payment`, `property`, `tenant`, `tenantportal`, `user`, cùng `common`.
- Luồng phổ biến: Controller -> Service có transaction -> Repository -> Entity.
- DTO chủ yếu là Java record; constructor injection; response thành công bọc `ApiResponse<T>`.

### Frontend

- React 19, TypeScript strict, Vite và Tailwind CSS 4.
- React Router, TanStack Query, Axios, React Hook Form và Zod.
- Local shadcn-style UI primitives, Zustand, Lucide và date-fns.
- Vitest/React Testing Library và Playwright.
- Feature folders tương ứng với các module backend.

## 3. Module đã có

| Module | Backend | Frontend tại baseline | Đánh giá |
| --- | --- | --- | --- |
| Authentication | Register, login, `/me`, BCrypt, JWT | Login/register/logout, restore profile | Có nhưng session/security chưa đủ production |
| Admin | Summary, list user | Dashboard và list read-only | Chưa có workflow đầy đủ tại baseline |
| Property/Room | CRUD và owner scoping | Create/list; thiếu nhiều action tại baseline | Core đã có |
| Tenant | CRUD và owner scoping | Create/list tại baseline | Thiếu lịch sử và lifecycle |
| Contract | List/create/get/end | Create/list tại baseline | State machine chưa chặt |
| Utility | List/create/update | Chưa có UI tại baseline | Backend có nhưng thiếu invariant kỳ trước/invoice |
| Invoice | List/create/get | Create/list tại baseline | Thủ công; chưa recurring/PDF |
| Payment | List/create | Chưa có UI tại baseline | Thiếu concurrency/idempotency/receipt |
| Maintenance | Landlord list/create/status; tenant create | Landlord list/status; tenant create | Thiếu assignment/timeline/file |
| Dashboard | Summary count | Summary cards | Thiếu báo cáo doanh thu/công nợ sâu |
| Tenant portal | Contract/room/invoice/payment/utility/maintenance summary | Portal cơ bản | Có lỗi phạm vi thời gian dữ liệu |

Branch `example` là hậu duệ trực tiếp của baseline và đã có các commit bổ sung responsive UI, role route, utility UI, update workflows, payment, admin status, invoice cancel, integration test và E2E. Các thay đổi này phải được tái sử dụng thay vì viết lại.

## 4. Database hiện tại

Migration nguồn tại baseline:

1. `V1__init_core_schema.sql`
2. `V2__seed_demo_data.sql`
3. `V3__seed_demo_auth_accounts.sql`
4. `V4__seed_tenant_portal_demo_data.sql`

V1 tạo 13 bảng cốt lõi:

```text
roles
user_accounts
user_roles
properties
rooms
tenants
rental_contracts
contract_tenants
utility_readings
invoices
invoice_items
payments
maintenance_requests
```

Điểm tốt:

- Khóa ngoại và nhiều check constraint đã có.
- Unique room/property, identity/landlord, active contract/room, primary tenant, utility period, invoice period và transaction reference.
- Dữ liệu tài chính dùng `NUMERIC`/`BigDecimal`.
- Có audit timestamps và soft delete ở phần lớn bảng nghiệp vụ.

Điểm lệch giữa tài liệu và schema/code:

- Tài liệu mô tả nhiều trường chưa có trong V1 như signed date, payment due day, termination reason, late fee, maintenance category và một số timestamps.
- Tài liệu dùng contract status `EXPIRED`/`TERMINATED`; code/V1 dùng `ENDED`.
- Một số nullable/check/delete rule trong V1 không chặt như tài liệu vật lý.
- Utility có `invoice_id` nhưng workflow chưa liên kết/khóa reading một cách hoàn chỉnh.
- Demo seed V2-V4 luôn được resolve ở mọi môi trường, trong khi README công khai mật khẩu demo. Đây là blocker production.

Không sửa V1-V4 đã tồn tại. Mọi thay đổi schema tiếp theo phải là migration version mới hoặc tách location/profile theo kế hoạch tương thích đã được kiểm thử.

## 5. API và authorization

Backend có khoảng 38 endpoint tại baseline. Các controller nghiệp vụ có `@PreAuthorize` theo `LANDLORD`, `TENANT` hoặc `ADMIN`; phần lớn service/repository dùng landlord id trong query để giảm IDOR.

Các rủi ro còn lại:

- Access token đã phát chưa bị vô hiệu hóa ngay khi account chuyển `LOCKED`/`INACTIVE`; JWT filter chưa kiểm tra đầy đủ `UserDetails` trước khi đặt authentication.
- Public register luôn tạo landlord, chưa có rate limit, email verification hoặc invite/onboarding policy.
- Swagger public và actuator authorization chưa phân vai trò đủ chặt.
- JWT secret có fallback cố định; production có thể khởi động với secret đoán được.
- Chưa có refresh/logout/revoke, issuer/audience/jti/rotation.
- Frontend lưu token trong `localStorage`, thiếu response interceptor 401/403 và có nguy cơ cache chéo khi đổi tài khoản.
- 401/403 từ filter chưa luôn cùng schema với `ErrorResponse`.

## 6. Lỗi và invariant nghiệp vụ đáng chú ý

### Contract, room, property và tenant

- Update status trực tiếp có thể đặt room có contract active về `AVAILABLE`, hoặc đặt tenant/property liên quan active về `INACTIVE`.
- End contract bằng ngày tương lai có thể giải phóng phòng ngay.
- End contract chưa ép đối soát invoice, công nợ, cọc và biên bản trả phòng.
- Tạo invoice chưa yêu cầu contract active.

### Utility và invoice

- Chưa so khớp old reading kỳ này với new reading kỳ trước.
- Reading đã dùng lập invoice vẫn có thể sửa.
- Invoice chưa snapshot bảng giá dịch vụ theo thời gian và chưa có recurring generation idempotent.
- Tính trạng thái overdue chủ yếu xảy ra khi có request/mutation thay vì scheduler hoặc query policy rõ ràng.

### Payment

- Client có thể gửi payment status.
- Chưa có optimistic/pessimistic locking; hai thanh toán đồng thời có thể vượt remaining amount.
- Chưa có quy trình refund/adjustment và receipt bất biến.

### Maintenance và tenant portal

- Tenant có thể được ghép vào room không thực sự liên quan nếu chỉ cùng landlord.
- Portal có thể lấy reading ngoài khoảng thời gian tenant cư trú.
- State machine maintenance còn quá rộng và thiếu assignee/technician workflow.

### Frontend/UX

- Baseline chưa có role route; nhập URL trực tiếp có thể render màn hình sai role trước khi backend trả 403.
- Nhiều form reset trước khi mutation thành công và có thể làm mất dữ liệu nhập.
- Loading/error/empty/success/confirmation không thống nhất.
- CCCD hiển thị nguyên văn.
- List chưa search/filter/page/sort.

## 7. Xung đột Docker/PostgreSQL đã xác định

Compose tại baseline mong đợi:

- Compose project mặc định: `rental-management` (lấy từ tên thư mục).
- Service: `postgres`.
- Container cố định: `rental-management-postgres`.
- Database/user: `rental_management`/`rental`.
- Volume key: `postgres_data`, actual volume `rental-management_postgres_data`.
- Network mặc định: `rental-management_default`.
- Host port: `5432`.

Container đang chạy khi audit lại là tài nguyên của cấu hình cũ trên lịch sử nhánh `main`:

- Container: `rental_management_postgres`.
- Image/user/database: `postgres:16`, `postgres`, `rental_management`.
- Compose labels vẫn là project `rental-management`, service `postgres` và cùng config path.
- Volume: `rental-management_rental_management_postgres_data`.
- Flyway history: V1-V3 của một schema khác, chỉ có 4 base table.

Volume mà Compose baseline hiện trỏ tới (`rental-management_postgres_data`) chứa đúng project hiện tại:

- PostgreSQL 16.
- Flyway V1-V4: `init core schema`, ba demo seed migration.
- 14 base table nếu tính `flyway_schema_history`.

Nguyên nhân gốc là sự kết hợp của:

1. Compose project name mặc định giống nhau.
2. Service name giống nhau nên Compose nhận container cũ là cùng service.
3. `container_name` là global.
4. Tên DB nội bộ và cổng host 5432 quá chung.
5. Volume key đã đổi giữa hai cấu hình nhưng cùng project namespace.
6. `.env` local dùng `DB_*`, còn Spring chỉ đọc `SPRING_DATASOURCE_*`; Compose lại hard-code giá trị.

Không được chạy recreate thiếu kiểm soát vì có thể đổi volume được attach và tạo cảm giác mất dữ liệu. Cả hai volume cũ phải được giữ nguyên. Kế hoạch an toàn là dùng project/database/port mới, sao lưu volume đúng và restore vào volume mới.

## 8. Baseline kiểm thử đã chạy

| Lệnh | Kết quả |
| --- | --- |
| `backend\\mvnw.cmd test` trên target có artifact cũ | Từng fail do duplicate Flyway version từ `target/classes`; cho thấy cần clean build |
| `backend\\mvnw.cmd clean test` | Pass; Testcontainers PostgreSQL 16.14, Flyway V1-V4, 1 test |
| `npm.cmd run test -- --run` | Pass 2/2 test tại baseline |
| `npm.cmd run build` | Pass; cảnh báo main chunk khoảng 522 KB |
| `npm.cmd run test:e2e` | Fail 3/3 tại baseline: assertion heading cũ và thiếu Firefox/WebKit executables |
| Frontend lint | Không chạy được vì chưa có script/config lint |

Test coverage baseline chưa đủ để bảo vệ auth, cross-landlord authorization, state machine, invoice/payment concurrency và các write workflow.

## 9. Kết luận kiến trúc

Giữ nguyên modular monolith, feature packages, PostgreSQL/Flyway và React SPA. Trình tự ưu tiên:

1. Ổn định Git baseline và Docker/database namespace mà không mất dữ liệu.
2. Khóa các lỗi P0 về credential, account status, production config và authorization.
3. Bổ sung test cho write workflow và cross-landlord isolation.
4. Hoàn thiện lifecycle phòng/hợp đồng/cọc/bàn giao/trả phòng.
5. Hoàn thiện utility/service price, recurring invoice, payment/receipt và audit.
6. Bổ sung UX, pagination, notification/file và production operations.

## 10. Kết quả triển khai sau audit

Working tree đã fast-forward từ baseline `889e059` tới hậu duệ `4633b4a`, sau đó harden theo module nhỏ mà không đổi kiến trúc modular monolith:

- Namespace mới `rental-management-datn1`/`rental_management_datn1_db`, port loopback `45432`, volume/network riêng; hai volume legacy không bị xóa.
- Dữ liệu đúng được dump/verify/restore; backend thực tế đã áp dụng V5 rồi V6 đúng một lần và V1–V4 không bị sửa.
- PostgreSQL tách bootstrap admin khỏi backend role; credential local đã được xoay riêng, healthcheck buộc password authentication qua network; `rental_management_app` hiện `NOSUPERUSER`, sở hữu 14 bảng public (13 bảng nghiệp vụ cùng `flyway_schema_history`) và 11 sequence, đồng thời có quyền cần thiết trên schema `public` (schema thuộc database owner/admin).
- Token cũ bị từ chối sau lock/inactive/soft-delete; 401/403 dùng JSON thống nhất; auth DTO che password/token; config JWT/CORS fail-fast.
- Generic OS `DEBUG`/`TRACE` không còn tự bật verbose log; production luôn cưỡng chế tắt.
- Production có bootstrap ADMIN một lần, khóa/ẩn seed demo khỏi admin view, cấm API mở lại demo account khi demo bị tắt, tự từ chối datasource role đặc quyền và throttle endpoint auth ở Nginx.
- Các invariant room/property/tenant/contract, utility/invoice, cancel/reissue, maintenance tenant-room và concurrent payment được siết bằng validation, row lock thống nhất và V6 partial index; tenant portal lọc reading theo hợp đồng/thời gian cư trú.
- Frontend cô lập cache theo session, xử lý late-401/cross-tab, sửa stale/reset form, mask CCCD, xác nhận action và chỉ cho lập hóa đơn từ hợp đồng active.
- Có production Dockerfiles, Nginx reverse proxy, health checks, graceful shutdown, log rotation, backup/restore runbook và backlog chi tiết.

Các hạng mục chưa hoàn tất thương mại gồm refresh/revoke và rate limit phân tán ở tầng ứng dụng, CI/secret scan, deposit/handover, recurring pricing/invoice, PDF receipt, audit log, search/pagination, notification/file, scheduled off-site backup/alerting và privacy/legal review.

## 11. Xác minh sau hardening

Các kết quả dưới đây là lệnh đã chạy thực tế ngày 16/07/2026, không phải kết quả dự kiến:

| Kiểm tra | Kết quả |
| --- | --- |
| `backend\\mvnw.cmd clean test` | Pass 33/33; Testcontainers PostgreSQL 16.14; Flyway tạo schema mới từ V1 đến V6; có test concurrency, tenant reading time-scope và khóa bất biến demo account production |
| `npm.cmd run lint` | Pass |
| `npm.cmd run typecheck` | Pass |
| `npm.cmd run test:run` | Pass 6 file/8 test |
| `npm.cmd run build` | Pass; còn cảnh báo main JS khoảng 544 KB cần code-splitting ở P2 |
| `npm.cmd audit --audit-level=high` | Pass; 0 vulnerability tại thời điểm kiểm tra |
| `npm.cmd run test:e2e` | Pass 12/12 trên Chromium, Firefox, WebKit và cấu hình Pixel 7 |
| `docker compose config --quiet` và profile `app` | Pass |
| Build image backend/frontend | Pass từ source mới nhất |
| Script init PostgreSQL | Pass `sh -n`; đồng thời được kiểm chứng bằng fresh-volume production stack |

Một production stack cô lập đã được dựng bằng project `rental-management-datn1-prodcheck`, database `rental_management_prodcheck_db`, port PostgreSQL `45433` và frontend `8082`. Kết quả:

- PostgreSQL, backend và frontend đều đạt Docker healthcheck. PostgreSQL probe dùng hostname mạng container để buộc password authentication bằng app role, đồng thời kiểm tra credential tách biệt và privilege; frontend probe dùng `127.0.0.1` và xác nhận `index.html` phục vụ được.
- SPA và `/healthz` trả HTTP 200 qua Nginx; tài khoản demo trả 401; API production trả 400 khi ADMIN cố kích hoạt lại demo account; bootstrap ADMIN đăng nhập được, sau đó backend vẫn restart healthy khi ba biến bootstrap đã bị bỏ.
- Admin view ban đầu chỉ có một ADMIN thật và 0 property dù seed V2–V4 vẫn được giữ; sau register landlord/tạo property, summary đếm đúng 2 user, 1 landlord, 1 property và không hiển thị email demo.
- CORS origin được cấu hình trả 200 kèm `Access-Control-Allow-Origin`; origin lạ trả 403.
- CSP/security headers hiện diện; burst auth vượt ngưỡng trả 429; liveness/readiness trả 200 và metrics không xác thực trả 401.
- Flyway history có V1–V6, không có migration thất bại; partial unique index V6 hiện diện.
- Database thuộc `rental_management_admin`; 14/14 bảng public thuộc `rental_management_app`; app role có `rolsuper/rolcreatedb/rolcreaterole/rolreplication/rolbypassrls=false`; ba tài khoản demo đều `LOCKED`.
- Backend/frontend chạy root filesystem read-only, `cap_drop: ALL` và `no-new-privileges`.
- Backend production khởi động hoàn tất, có profile `prod`, không có dòng DEBUG hoặc plaintext password kiểm thử trong log.

Sau khi kiểm tra, đúng ba container, một network và một volume thuộc namespace `prodcheck` đã được tháo. Stack development, database mới đã restore và toàn bộ volume legacy không bị xóa.

## 12. Checkpoint P0 Authentication Session Hardening

Phần 10–11 ở trên là bằng chứng lịch sử của checkpoint `7b75e45`; các nhận định
“refresh/revoke chưa hoàn tất” và số liệu V1–V6 không còn mô tả branch feature
hiện tại. P0-03 tiếp tục trên `feature/p0-auth-session-hardening` mà không sửa
V1–V6 và không triển khai chức năng P1:

- JWT access token mặc định giảm còn 15 phút và chỉ được giữ trong memory của frontend.
- V7 bổ sung `refresh_sessions`; opaque refresh token 256 bit chỉ đi qua cookie HttpOnly, database chỉ lưu SHA-256 hash.
- Refresh rotation dùng transaction và thứ tự pessimistic lock ổn định `user_account` → `refresh_session`; predecessor reuse chỉ revoke đúng family và concurrent refresh không thể tạo hai successor hợp lệ.
- V8 bổ sung `user_accounts.auth_version`. Logout-all hợp lệ, đổi mật khẩu, khóa/vô hiệu/xóa mềm account tăng version để access JWT cũ bị từ chối ngay ở request tiếp theo; logout một thiết bị vẫn chỉ revoke family hiện tại.
- Login và change-password đọc lại credential/trạng thái dưới user lock. Token lịch sử, forged, expired hoặc revoked không thể dùng để revoke toàn bộ session của account.
- Ba endpoint dùng refresh cookie kiểm tra `Origin` hoặc fallback `Referer` chính xác theo CORS allowlist trên path MVC đã canonicalize; request thiếu/sai nguồn bị từ chối trước khi mutation.
- Frontend bootstrap phiên sau reload, single-flight các 401, retry tối đa một lần, offline-logout tombstone/retry, lỗi ứng dụng đã sanitize và đồng bộ generation giữa tab mà không truyền credential. Web Lock serialize request cookie-auth khi browser hỗ trợ; fallback không Web Locks fail-closed nhưng còn rủi ro buộc đăng nhập lại khi response cross-tab bị reorder.

Vòng tái kiểm độc lập và hardening ngày 21/07/2026 đã chạy thực tế:

| Kiểm tra | Kết quả |
| --- | --- |
| `.\\mvnw.cmd clean test` | Pass 62/62; PostgreSQL Testcontainers; bao phủ schema mới V1–V8, nâng cấp V7→V8 và 5 race concurrency bằng barrier |
| `npm ci`, `npm run lint`, `npm run typecheck` | Pass |
| `npm run test:run` | Pass 11 file/41 test |
| `npm run build` | Pass; còn cảnh báo bundle chính 554,31 KB thuộc tối ưu P2 |
| `npm audit` | Pass; 0 vulnerability tại thời điểm kiểm tra |
| `npm run test:e2e -- --project=webkit tests/e2e/auth-session.spec.ts` | Pass 4/4 khi lặp riêng nhóm WebKit |
| `npm run test:e2e` | Pass 28/28 trên Chromium, Firefox, WebKit và mobile Chrome |
| `docker compose config --quiet` và profile `app` | Pass |
| Database development | V8 được áp dụng riêng sau V1–V7, không chạy lại migration cũ; trước/sau migrate giữ nguyên 5 user và 28 refresh session. Sau E2E có 106 refresh-session và không record nào bị xóa; Flyway V1–V8 đều thành công, 0 migration lỗi, 15 bảng public gồm `flyway_schema_history` |

Chi tiết thiết kế, cookie, endpoint và nguồn chính thức nằm tại
[authentication-security.md](authentication-security.md).
