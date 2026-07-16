# Product Backlog — Rental Management System

## 1. Mục đích và phạm vi

Tài liệu này chuyển các yêu cầu trong README.md, docs/requirements.md, thiết kế dữ liệu hiện tại và kết quả audit source code thành backlog có thể triển khai theo từng module nhỏ.

Phạm vi sản phẩm được giữ ở mức quản lý dãy phòng trọ:

- Chủ trọ quản lý khu trọ, phòng, người thuê, hợp đồng, điện nước, dịch vụ, hóa đơn, thanh toán và bảo trì.
- Người thuê xem dữ liệu của mình, nhận thông báo và gửi yêu cầu sửa chữa.
- Quản trị viên quản lý tài khoản và vận hành hệ thống.
- Nhân viên hoặc kỹ thuật viên chỉ được bổ sung với quyền giới hạn theo khu trọ và công việc; không mở rộng thành hệ thống quản lý bất động sản tổng quát.

Backlog này là tài liệu kế hoạch, không phải tuyên bố các chức năng đã được hoàn thành.

## 2. Quy ước trạng thái và ước tính

### 2.1. Trạng thái A–D

| Trạng thái | Ý nghĩa |
| --- | --- |
| A | Đã có trên HEAD hiện tại ở mức đáp ứng phạm vi được mô tả; cần giữ bằng regression test |
| B | Đã có một phần nhưng còn thiếu nghiệp vụ, UI, kiểm thử hoặc độ an toàn để dùng thực tế |
| C | Chưa có trên HEAD hiện tại nhưng cần thiết cho sản phẩm có thể vận hành hoặc thương mại hóa |
| D | Chức năng nâng cao, có thể thực hiện sau khi P0–P2 ổn định |

### 2.2. Dấu vết trên branch example

Snapshot ban đầu được audit là branch `test` tại commit `889e059`. Branch `origin/example` là nhánh hậu duệ và có các commit mẫu tiếp tục triển khai MVP. Sau audit, working tree đã được fast-forward an toàn tới `4633b4a` để tái sử dụng các commit này. Các nhãn dưới đây là dấu vết lịch sử giúp tránh viết lại chức năng:

- **EXAMPLE-CÓ:** origin/example có implementation ứng viên tương đối đầy đủ cho phạm vi item.
- **EXAMPLE-MỘT PHẦN:** origin/example có một phần liên quan nhưng vẫn cần review và hoàn thiện.
- **EXAMPLE-KHÔNG:** chưa thấy implementation tương ứng trên origin/example.

Không được xem nhãn EXAMPLE-CÓ là bằng chứng sẵn sàng production. Implementation đã merge vẫn phải được review đồng thời ở backend `48247a0`, frontend `9073595` và test `47d5046` vì các API có phụ thuộc lẫn nhau.

### 2.3. Độ phức tạp

| Mức | Ước tính tương đối |
| --- | --- |
| S | Tối đa khoảng 3 ngày phát triển |
| M | Khoảng 4–7 ngày phát triển |
| L | Khoảng 2–3 tuần phát triển |
| XL | Trên 3 tuần hoặc cần chia thành nhiều sprint |

Ước tính bao gồm backend, frontend, migration mới, test và tài liệu; chưa bao gồm thời gian chờ hạ tầng hoặc nhà cung cấp bên thứ ba.

## 3. Nguyên tắc Definition of Done chung

Một item chỉ được chuyển sang Done khi thỏa các điều kiện phù hợp sau:

- Không sửa migration Flyway đã chạy; mọi thay đổi schema dùng migration mới.
- API cũ đang hoạt động được giữ tương thích hoặc có kế hoạch chuyển đổi rõ ràng.
- Authorization được kiểm tra ở backend; ẩn menu hoặc chặn route frontend không được xem là đủ.
- Request có validation; lỗi trả theo cấu trúc thống nhất, không lộ password, token, secret hoặc dữ liệu của landlord khác.
- UI có loading, empty, error, success và disabled state; responsive tối thiểu từ 320 px.
- Có unit test cho rule chính, integration test cho database/authorization và E2E cho happy path quan trọng.
- README, API docs và tài liệu vận hành được cập nhật.
- Build, lint, test và Docker Compose config phải chạy thật trước khi báo hoàn thành.
- Git diff không chứa secret, dữ liệu cá nhân thật, build artifact hoặc thay đổi ngoài phạm vi.

## 4. Tổng quan backlog

Bảng A–D dưới đây là phân loại tại thời điểm audit ban đầu. Tiến độ hardening trong working tree hiện tại được ghi ngay sau bảng; các item chỉ được xem là đóng hoàn toàn khi đáp ứng toàn bộ acceptance criteria chi tiết.

| ID | Hạng mục | A–D khi audit | Example | Priority | Complexity |
| --- | --- | --- | --- | --- | --- |
| P0-01 | Định danh PostgreSQL/Docker duy nhất và chuyển dữ liệu an toàn | B | EXAMPLE-KHÔNG | P0 | M |
| P0-02 | Chuẩn hóa env, secret và cấu hình production | B | EXAMPLE-KHÔNG | P0 | M |
| P0-03 | Xác thực và quản lý phiên an toàn | B | EXAMPLE-MỘT PHẦN | P0 | L |
| P0-04 | Phân quyền và cô lập dữ liệu theo chủ trọ | B | EXAMPLE-CÓ | P0 | L |
| P0-05 | Validation và xử lý lỗi thống nhất | B | EXAMPLE-MỘT PHẦN | P0 | M |
| P0-06 | Backup, restore, health check và giám sát tối thiểu | B | EXAMPLE-KHÔNG | P0 | L |
| P0-07 | Quality gate build, lint, test và CI | B | EXAMPLE-MỘT PHẦN | P0 | L |
| P1-01 | Tạo và xem khu trọ/phòng cơ bản | A | EXAMPLE-CÓ | P1 | S |
| P1-02 | Vòng đời khu trọ, tầng, phòng và đặt chỗ | B | EXAMPLE-MỘT PHẦN | P1 | L |
| P1-03 | Người thuê và thành viên ở cùng | B | EXAMPLE-CÓ | P1 | L |
| P1-04 | Vòng đời hợp đồng, gia hạn và chấm dứt | B | EXAMPLE-CÓ | P1 | L |
| P1-05 | Sổ tiền đặt cọc | C | EXAMPLE-KHÔNG | P1 | L |
| P1-06 | Bàn giao, nhận phòng và trả phòng | C | EXAMPLE-KHÔNG | P1 | L |
| P1-07 | Chỉ số và đơn giá điện nước | B | EXAMPLE-CÓ | P1 | L |
| P1-08 | Danh mục dịch vụ và đơn giá theo hiệu lực | C | EXAMPLE-KHÔNG | P1 | L |
| P1-09 | Tạo hóa đơn tháng định kỳ, chống trùng | B | EXAMPLE-MỘT PHẦN | P1 | XL |
| P1-10 | Công nợ, thanh toán và lịch sử giao dịch | B | EXAMPLE-CÓ | P1 | L |
| P1-11 | Phiếu thu, biên nhận và PDF | C | EXAMPLE-KHÔNG | P1 | L |
| P1-12 | Bảo trì và phân công xử lý | B | EXAMPLE-MỘT PHẦN | P1 | L |
| P1-13 | Cổng người thuê và thông báo trong ứng dụng | B | EXAMPLE-MỘT PHẦN | P1 | L |
| P2-01 | Quản lý tài khoản, nhân viên và kỹ thuật viên | B | EXAMPLE-MỘT PHẦN | P2 | XL |
| P2-02 | Tìm kiếm, lọc, phân trang và sắp xếp | C | EXAMPLE-KHÔNG | P2 | L |
| P2-03 | Dashboard và báo cáo vận hành | B | EXAMPLE-MỘT PHẦN | P2 | L |
| P2-04 | Tài liệu và hình ảnh liên quan | C | EXAMPLE-KHÔNG | P2 | XL |
| P2-05 | Nhật ký hoạt động và lịch sử thay đổi | C | EXAMPLE-KHÔNG | P2 | XL |
| P2-06 | UX responsive, accessibility và trạng thái giao diện | B | EXAMPLE-MỘT PHẦN | P2 | L |
| P3-01 | Nhắc việc và thông báo email/SMS theo sự kiện | D | EXAMPLE-KHÔNG | P3 | XL |
| P3-02 | Thanh toán trực tuyến và đối soát tự động | D | EXAMPLE-KHÔNG | P3 | XL |

### Tiến độ triển khai ngày 16/07/2026

- **P0-01:** đã tách namespace Docker/database, backup/restore dữ liệu đúng, giữ nguyên volume cũ và xác nhận Flyway V1–V6.
- **P0-02:** đã có env fail-closed, bootstrap ADMIN một lần, runtime từ chối DB role đặc quyền, role PostgreSQL non-superuser, xoay credential admin/app cục bộ tách biệt, healthcheck buộc password authentication, khóa bất biến tài khoản demo khi demo bị tắt, cấu hình production, CORS allowlist, log redaction và image Docker; secret manager/CI secret scan vẫn còn.
- **P0-03:** đã hoàn thành mô hình access JWT 15 phút chỉ giữ trong memory và opaque refresh session 7 ngày; migration V7 chỉ lưu token hash, rotation dùng row lock, reuse/concurrent loser revoke toàn family, logout/logout-all/change-password/account lock có revocation, cookie HttpOnly `SameSite=Strict` và `Secure` trong production. Frontend khôi phục phiên sau reload, single-flight refresh/retry một lần, đồng bộ login/logout không truyền token giữa các tab. Xem [tài liệu bảo mật xác thực](authentication-security.md).
- **P0-04/P0-05:** đã vô hiệu access token của account bị khóa/xóa, chuẩn hóa 401/403, cô lập cache frontend, throttle login/register tại Nginx production, lọc utility portal theo thời gian cư trú, bổ sung role/ownership test và nhiều invariant; distributed rate limit và ma trận IDOR đầy đủ vẫn còn.
- **P0-06:** đã diễn tập dump/restore, có health check và log rotation; backup scheduler, off-site retention, alerting và PITR vẫn còn.
- **P0-07:** backend clean test, frontend lint/typecheck/unit/build và E2E đa trình duyệt đã chạy; pipeline CI chưa được tạo.
- **P1-02/P1-04/P1-07/P1-09/P1-10/P1-12:** đã siết lifecycle, thống nhất room/contract/invoice/utility row-lock, continuity điện nước, invoice period/linkage/cancel-reissue, payment concurrency và tenant-room maintenance; các workflow cọc, bàn giao/trả phòng, gia hạn, recurring invoice và receipt vẫn chưa hoàn tất.

Các dòng **Trạng thái** trong từng item bên dưới mô tả snapshot audit ban đầu để giữ traceability. Khi wording baseline mâu thuẫn với danh sách tiến độ này, tiến độ ngày 16/07/2026 và source/test hiện tại là trạng thái mới hơn; acceptance criteria chưa đạt vẫn là backlog mở.

## 5. P0 — Bắt buộc để hệ thống chạy an toàn

### P0-01 — Định danh PostgreSQL/Docker duy nhất và chuyển dữ liệu an toàn

- **Trạng thái:** B — Docker Compose và PostgreSQL đã có, nhưng tên database, container, volume và project có nguy cơ xung đột.
- **Dấu vết origin/example:** EXAMPLE-KHÔNG.
- **Mục đích:** Bảo đảm project có namespace Docker riêng, backend kết nối đúng database và dữ liệu hiện hữu không bị xóa hoặc ghi đè.
- **Vai trò:** DevOps, developer, system administrator.
- **User story:** Là người triển khai, tôi muốn mọi tài nguyên Docker của Rental Management có tên riêng để có thể chạy cùng các project khác mà không xung đột.
- **Acceptance criteria:**
  - POSTGRES_DB, container, Compose project name, volume và network có định danh riêng nhất quán.
  - application.yml, application-test.yml, env, script và tài liệu không còn tham chiếu tên cũ ngoài phần hướng dẫn migrate.
  - Docker Compose config hợp lệ; PostgreSQL healthy; backend kết nối thành công.
  - Flyway chỉ áp dụng migration chưa chạy, không tạo lại V1–V4.
  - Có quy trình pg_dump/pg_restore hoặc đổi volume an toàn; không tự động xóa volume cũ.
- **Bảng database:** Không đổi bảng nghiệp vụ; kiểm tra flyway_schema_history và toàn bộ schema hiện hữu.
- **API:** Không thêm API nghiệp vụ; health endpoint phải phản ánh database readiness.
- **UI:** Không yêu cầu UI; tài liệu vận hành phải nêu rõ tên database và cách phục hồi.
- **Validation và lỗi:** Fail fast khi thiếu DB URL/user/password; phân biệt lỗi DNS, authentication, database không tồn tại và migration checksum.
- **Test:** docker compose config; healthcheck; backend startup; Flyway info/migrate; smoke query; thử backup/restore trên database tạm.
- **Priority:** P0.
- **Complexity:** M.

### P0-02 — Chuẩn hóa env, secret và cấu hình production

- **Trạng thái:** B tại baseline — đã có .env.example và biến JWT/database; hardening hiện tại đã thêm `envDir`, secret fail-closed, role DB giới hạn và profile production, nhưng secret manager/CI scan vẫn mở.
- **Dấu vết origin/example:** EXAMPLE-KHÔNG.
- **Mục đích:** Không hard-code secret, tách cấu hình development/test/production và bảo đảm cấu hình lỗi bị phát hiện sớm.
- **Vai trò:** DevOps, developer, system administrator.
- **User story:** Là người triển khai, tôi muốn cấu hình hệ thống qua biến môi trường để cùng một artifact có thể chạy an toàn ở nhiều môi trường.
- **Acceptance criteria:**
  - .env.example chỉ chứa placeholder an toàn; không commit .env thật.
  - JWT secret production đủ mạnh và không có fallback dùng được ngoài dev.
  - Frontend xác định rõ envDir hoặc có frontend/.env.example; VITE_API_BASE_URL được kiểm chứng khi build.
  - Có profile production với CORS allowlist, log level, datasource pool và actuator exposure tối thiểu.
  - Có kiểm tra secret scanning trong CI.
- **Bảng database:** P0-02 không tự thêm bảng; P0-03 hiện đã bổ sung `refresh_sessions` bằng migration V7 để quản lý refresh token server-side.
- **API:** Không thêm API; cấu hình CORS và base URL cho toàn bộ API.
- **UI:** Có trang lỗi cấu hình/build-time rõ ràng thay vì gọi nhầm localhost trong production.
- **Validation và lỗi:** Startup fail nếu secret ngắn, URL sai hoặc biến bắt buộc thiếu; không log giá trị secret.
- **Test:** test profile binding; CORS integration test; build frontend với API URL mẫu; secret scan; thử startup thiếu từng biến bắt buộc.
- **Priority:** P0.
- **Complexity:** M.

### P0-03 — Xác thực và quản lý phiên an toàn

- **Trạng thái:** A trong phạm vi P0-03 — mô hình refresh session, backend revocation/rotation, frontend memory-only/session recovery và regression tests đã được triển khai; brute-force protection phân tán vẫn thuộc hardening vận hành riêng, không mở lại vòng đời phiên này.
- **Dấu vết origin/example:** EXAMPLE-MỘT PHẦN — giữ auth hiện có và bổ sung role route, chưa giải quyết đầy đủ vòng đời phiên.
- **Mục đích:** Cung cấp phiên đăng nhập ổn định, không để token hết hạn hoặc đổi tài khoản làm lộ cache dữ liệu.
- **Vai trò:** Admin, landlord, tenant, staff, technician.
- **User story:** Là người dùng, tôi muốn đăng nhập/đăng xuất an toàn và được chuyển về màn hình phù hợp khi phiên hết hạn.
- **Acceptance criteria:**
  - Login/register/refresh chỉ trả access token và profile cần thiết; không trả password hash hoặc raw refresh token, và response không được cache.
  - Access JWT mặc định sống 15 phút, chỉ giữ trong memory; frontend không lưu access/refresh credential trong `localStorage` hoặc `sessionStorage`.
  - Opaque refresh token 256 bit chỉ truyền qua cookie HttpOnly; database chỉ lưu SHA-256 hash và absolute expiry mặc định 7 ngày.
  - Mỗi refresh rotate token trong một transaction có row lock. Token cũ không dùng lại được; reuse hoặc concurrent loser revoke toàn bộ family và trả JSON 401 chung.
  - Account `LOCKED`, `INACTIVE` hoặc soft-deleted không refresh được. Đổi mật khẩu hoặc khóa account revoke toàn bộ refresh session.
  - Logout revoke family hiện tại; logout-all revoke mọi family của account; cả hai xóa cookie và không làm lộ trạng thái account/token.
  - Cookie luôn `HttpOnly`, host-only, `Path=/api/auth`, mặc định `SameSite=Strict`, có `Secure` bắt buộc trong production; credentialed CORS từ chối wildcard origin.
  - Reload trang khôi phục phiên qua refresh endpoint. Các 401 đồng thời dùng chung refresh promise, mỗi request retry tối đa một lần và refresh failure kết thúc phiên, không tạo loop.
  - Cross-tab chỉ truyền tín hiệu login/logout không chứa token; logout và session epoch ngăn late response khôi phục phiên đã kết thúc.
  - 401 xóa phiên/cache theo user; 403 không xóa nhầm phiên; login failure không tiết lộ email/account có tồn tại.
  - Form login/register giữ validation theo field, trạng thái pending và thông báo thất bại dễ hiểu.
- **Bảng database:** `user_accounts`, `roles`, `user_roles`, `refresh_sessions` (V7); xem [physical schema](database-schema.md#24-refresh_sessions).
- **API:** `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/refresh`, `POST /api/auth/logout`, `POST /api/auth/logout-all`, `POST /api/auth/change-password`, `GET /api/auth/me`.
- **UI:** Login/register và route guard hiện có; bootstrap loading khi khôi phục phiên, expired-session/logout state và đồng bộ logout giữa các tab.
- **Validation và lỗi:** Chuẩn hóa email và password policy; generic JSON 401 cho credential/refresh không hợp lệ; validate TTL, token size, cookie name/path/SameSite; không ghi token vào URL, log, exception hoặc `toString()`.
- **Test:** Backend kiểm tra cookie/hash-at-rest, rotation/reuse/revocation, expiry/forgery, logout/logout-all, password/account revocation, production cookie và concurrent refresh trên PostgreSQL Testcontainers/Flyway V1–V7. Frontend kiểm tra memory-only/legacy cleanup, reload recovery, single-flight 401, retry guard, refresh failure, Web Locks, cross-tab signal và late-response race. Cổng đóng item gồm backend clean test; frontend lint/typecheck/unit/build; E2E auth phù hợp; `npm audit`; Compose config; diff/secret scan.
- **Tài liệu và nguồn:** [Authentication and session security](authentication-security.md), [Spring Security session management](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html), [OWASP Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html), [RFC 9700](https://www.rfc-editor.org/rfc/rfc9700.html).
- **Priority:** P0.
- **Complexity:** L.

### P0-04 — Phân quyền và cô lập dữ liệu theo chủ trọ

- **Trạng thái:** B tại baseline — backend có @PreAuthorize; working tree hiện đã có `RoleRoute`, nhưng ma trận IDOR/role đầy đủ và scoped staff/technician vẫn mở.
- **Dấu vết origin/example:** EXAMPLE-CÓ — có RoleRoute và backend ownership candidate; phải review toàn bộ endpoint trước khi dùng.
- **Mục đích:** Ngăn truy cập chéo landlord, tenant hoặc role ngoài quyền ở cả API và UI.
- **Vai trò:** Admin, landlord, tenant, staff, technician.
- **User story:** Là chủ trọ, tôi muốn chỉ tôi và nhân sự được cấp quyền mới thấy dữ liệu khu trọ của tôi.
- **Acceptance criteria:**
  - Mọi query/mutation nghiệp vụ lọc theo current user hoặc property membership ở backend.
  - ID của landlord khác trả 404 hoặc 403 theo policy thống nhất; không lộ dữ liệu qua list, detail, nested resource hoặc dashboard.
  - Frontend có route guard theo role; menu chỉ là lớp UX bổ sung.
  - Tenant chỉ thấy tenant record, hợp đồng, hóa đơn, payment, utility và maintenance của chính mình.
  - Admin action được audit; staff/technician dùng least privilege.
- **Bảng database:** user_accounts, roles, user_roles, properties, tenants và toàn bộ bảng có ownership gián tiếp; đề xuất property_memberships cho P2-01.
- **API:** Toàn bộ /api; đặc biệt /properties, /rooms, /tenants, /contracts, /invoices, /payments, /maintenance-requests, /tenant-portal và /admin.
- **UI:** RoleRoute, Forbidden, role-aware navigation, empty state khi không có quyền.
- **Validation và lỗi:** Không tin landlordId/userId từ request; chống IDOR; không trả khác biệt giúp dò record của tenant khác.
- **Test:** security integration test theo ma trận role × endpoint; test IDOR bằng hai landlord; tenant isolation; direct-navigation E2E.
- **Priority:** P0.
- **Complexity:** L.

### P0-05 — Validation và xử lý lỗi thống nhất

- **Trạng thái:** B — backend có GlobalExceptionHandler/ErrorResponse; frontend phần lớn chỉ hiện AxiosError hoặc coi lỗi như danh sách rỗng.
- **Dấu vết origin/example:** EXAMPLE-MỘT PHẦN.
- **Mục đích:** Bảo vệ tính đúng dữ liệu và giúp người dùng sửa lỗi mà không mất nội dung form.
- **Vai trò:** Tất cả người dùng, developer, support.
- **User story:** Là người vận hành, tôi muốn lỗi được chỉ đúng field và dữ liệu đã nhập được giữ lại để có thể sửa ngay.
- **Acceptance criteria:**
  - Error contract có code, message, validationErrors, timestamp/correlationId và không lộ stack trace.
  - Frontend parse error contract thống nhất; form chỉ reset sau khi mutation thành công.
  - Tất cả page có loading, empty, error, retry và mutation feedback.
  - Business conflict dùng 409; validation 400; unauthenticated 401; forbidden 403; missing 404.
  - Các mutation tài chính quan trọng có idempotency key hoặc unique constraint phù hợp.
- **Bảng database:** Các CHECK/UNIQUE hiện có; bổ sung constraint/index bằng migration mới theo từng module.
- **API:** Error schema dùng chung cho toàn bộ API; có thể bổ sung Idempotency-Key cho invoice/payment.
- **UI:** Shared FormError, FieldError, QueryState, toast/notification và retry action.
- **Validation và lỗi:** Trim/normalize input; giới hạn length; số tiền không âm; date range; không dùng truthy check làm sai giá trị 0 hợp lệ.
- **Test:** parameterized validation tests; exception-handler tests; frontend error mapping; E2E server validation và retry không mất form.
- **Priority:** P0.
- **Complexity:** M.

### P0-06 — Backup, restore, health check và giám sát tối thiểu

- **Trạng thái:** B tại baseline — hiện đã có runbook backup/restore và liveness/readiness; scheduler, off-site retention và cảnh báo vận hành vẫn mở.
- **Dấu vết origin/example:** EXAMPLE-KHÔNG.
- **Mục đích:** Có thể phục hồi sau sự cố và phát hiện sớm database/backend không sẵn sàng.
- **Vai trò:** System administrator, DevOps, product owner.
- **User story:** Là người vận hành, tôi muốn có backup được kiểm chứng và cảnh báo sức khỏe để giảm rủi ro mất dữ liệu.
- **Acceptance criteria:**
  - Có hướng dẫn backup/restore PostgreSQL, mã hóa file backup và retention policy.
  - Backup tự động không ghi đè bản gần nhất; restore được thử định kỳ trên database tách biệt.
  - Liveness không phụ thuộc DB; readiness kiểm tra DB/Flyway và dependency bắt buộc.
  - Log có request/correlation ID, không chứa token/password/CCCD đầy đủ.
  - Có metric tối thiểu cho HTTP error, latency, DB pool, JVM và backup failure.
- **Bảng database:** Toàn bộ schema; flyway_schema_history; không lưu backup binary trong database nghiệp vụ.
- **API:** /actuator/health/liveness, /actuator/health/readiness, /actuator/metrics với quyền truy cập hạn chế.
- **UI:** Không bắt buộc dashboard kỹ thuật ở MVP; có maintenance/unavailable page thân thiện.
- **Validation và lỗi:** Restore chỉ vào target đã xác nhận; kiểm tra checksum, PostgreSQL version và migration compatibility.
- **Test:** backup/restore drill; health endpoint khi DB up/down; log redaction; metric smoke test.
- **Priority:** P0.
- **Complexity:** L.

### P0-07 — Quality gate build, lint, test và CI

- **Trạng thái:** B tại baseline — working tree hiện pass backend/integration, lint/typecheck/unit/build và E2E đa trình duyệt; chưa có coverage threshold và CI gate.
- **Dấu vết origin/example:** EXAMPLE-MỘT PHẦN — có E2E landlord/tenant/admin và integration tests candidate.
- **Mục đích:** Ngăn regression và không cho artifact lỗi được đưa lên môi trường triển khai.
- **Vai trò:** Developer, reviewer, DevOps.
- **User story:** Là maintainer, tôi muốn mỗi thay đổi được build và kiểm thử tự động trước khi merge.
- **Acceptance criteria:**
  - Pipeline chạy Docker Compose config, backend test, frontend typecheck/lint/unit/build.
  - E2E chạy trên ít nhất Chromium desktop và một mobile viewport; cross-browser có thể chạy nightly.
  - Test quan trọng dùng PostgreSQL thật qua Testcontainers hoặc môi trường tách biệt.
  - Có coverage threshold thực tế cho service/business rules, không chỉ controller smoke test.
  - CI không dùng lệnh chỉ có trên Windows như npm.cmd khi runner là Linux.
- **Bảng database:** Test database/containers riêng; migration production được áp dụng nguyên trạng.
- **API:** Contract tests cho response/error schema và OpenAPI.
- **UI:** Component/form/route tests; visual responsive smoke tests cho màn hình chính.
- **Validation và lỗi:** Pipeline fail khi browser thiếu, test flaky quá ngưỡng, migration lỗi hoặc secret bị phát hiện.
- **Test:** Chính item này được nghiệm thu bằng một pipeline xanh từ checkout sạch; thêm test báo đỏ có chủ đích để xác minh gate.
- **Priority:** P0.
- **Complexity:** L.

## 6. P1 — Chức năng cốt lõi để sử dụng thực tế

### P1-01 — Tạo và xem khu trọ/phòng cơ bản

- **Trạng thái:** A — current HEAD đã hỗ trợ tạo/list property, tạo/list room và first-flow trong README; item này chủ yếu là baseline regression.
- **Dấu vết origin/example:** EXAMPLE-CÓ — có candidate mở rộng edit/deactivate.
- **Mục đích:** Giữ luồng dữ liệu nền tảng để landlord bắt đầu quản lý phòng.
- **Vai trò:** Landlord.
- **User story:** Là chủ trọ, tôi muốn tạo khu trọ, thêm phòng và xem danh sách phòng theo khu trọ.
- **Acceptance criteria:**
  - Chỉ landlord sở hữu được tạo/xem dữ liệu.
  - Số phòng duy nhất trong cùng khu trọ.
  - Diện tích, giá thuê, tiền cọc và sức chứa được lưu đúng.
  - Tạo thất bại không reset form; list có loading/error/empty.
- **Bảng database:** properties, rooms, user_accounts.
- **API:** GET/POST /api/properties; GET/POST /api/properties/{propertyId}/rooms.
- **UI:** Trang Khu trọ và phòng, property form, room form, room cards/table.
- **Validation và lỗi:** Tên/địa chỉ bắt buộc; area và maxOccupants lớn hơn 0; tiền không âm; duplicate room trả 409.
- **Test:** service ownership/duplicate; API create/list; form validation; E2E first-flow.
- **Priority:** P1.
- **Complexity:** S cho regression hardening còn lại.

### P1-02 — Vòng đời khu trọ, tầng, phòng và đặt chỗ

- **Trạng thái:** B tại baseline — working tree đã có update/deactivate property/room; RESERVED/booking, floor filtering và state machine đầy đủ vẫn chưa có.
- **Dấu vết origin/example:** EXAMPLE-MỘT PHẦN — có edit property/room và deactivate, chưa có đặt chỗ đầy đủ.
- **Mục đích:** Quản lý vòng đời phòng đúng thực tế mà không cần biến sản phẩm thành hệ thống bất động sản lớn.
- **Vai trò:** Landlord, staff.
- **User story:** Là chủ trọ, tôi muốn sửa thông tin, nhóm phòng theo tầng và chuyển trạng thái trống/đặt/đang thuê/bảo trì theo rule.
- **Acceptance criteria:**
  - Property và room được sửa, ngừng dùng bằng soft delete/status.
  - Tầng có thể biểu diễn bằng floor_number và bộ lọc; chỉ tạo bảng floors nếu cần metadata riêng.
  - Có state machine AVAILABLE → RESERVED → OCCUPIED hoặc MAINTENANCE/INACTIVE theo rule.
  - Reservation có người đặt, thời hạn, số tiền giữ chỗ tùy chọn và tự hết hạn/cancel.
  - Không thể đặt hoặc tạo hợp đồng cho phòng không phù hợp.
- **Bảng database:** properties, rooms; đề xuất room_reservations; migration mới nếu thêm RESERVED vào constraint.
- **API:** GET/PUT/DELETE /api/properties/{id}; GET/PUT/DELETE /api/rooms/{id}; đề xuất /api/rooms/{id}/reservations.
- **UI:** Property/room detail-edit, floor grouping, room status board, reservation form/timeline.
- **Validation và lỗi:** Chặn duplicate room, invalid transition, reservation overlap, deactivate property còn contract active; trả 409.
- **Test:** state machine unit test; overlap integration test; authorization; E2E reserve/cancel/maintenance.
- **Priority:** P1.
- **Complexity:** L.

### P1-03 — Người thuê và thành viên ở cùng

- **Trạng thái:** B tại baseline — working tree đã có edit và rental history cơ bản; quản lý thành viên ở cùng, self-service và lifecycle đầy đủ vẫn thiếu.
- **Dấu vết origin/example:** EXAMPLE-CÓ — có candidate edit tenant và rental history.
- **Mục đích:** Lưu hồ sơ người thuê chính và người ở cùng, phục vụ hợp đồng, liên hệ và vận hành.
- **Vai trò:** Landlord, staff, tenant.
- **User story:** Là chủ trọ, tôi muốn quản lý hồ sơ người thuê và những người ở cùng theo từng hợp đồng.
- **Acceptance criteria:**
  - CRUD/soft deactivate tenant; có ngày sinh, liên hệ, giấy tờ và địa chỉ.
  - Một contract có nhiều tenant và đúng một primary tenant.
  - Có move-in/move-out date và lịch sử hợp đồng của tenant.
  - Tenant account chỉ xem/sửa các trường self-service được cho phép.
  - CCCD được mask trên list và bảo vệ theo role.
- **Bảng database:** tenants, contract_tenants, rental_contracts, user_accounts.
- **API:** GET/POST /api/tenants; GET/PUT/DELETE /api/tenants/{id}; đề xuất GET /api/tenants/{id}/contracts và endpoint quản lý members nếu cần.
- **UI:** Tenant list/detail/form, occupant selector, rental history, self-profile.
- **Validation và lỗi:** CCCD active không trùng; email/phone format; date of birth hợp lệ; primary tenant phải thuộc tenantIds.
- **Test:** uniqueness/ownership; contract membership; masking; edit/history E2E.
- **Priority:** P1.
- **Complexity:** L.

### P1-04 — Vòng đời hợp đồng, gia hạn và chấm dứt

- **Trạng thái:** B tại baseline — working tree đã có create/list/end UI và invariant chính; detail/renew/amendment cùng đối soát trả phòng vẫn thiếu.
- **Dấu vết origin/example:** EXAMPLE-CÓ — có candidate create/end; gia hạn và lịch sử thay đổi vẫn cần hoàn thiện.
- **Mục đích:** Quản lý đầy đủ DRAFT, ACTIVE, ENDED, CANCELLED và giữ lịch sử không phá dữ liệu cũ.
- **Vai trò:** Landlord, staff, tenant read-only.
- **User story:** Là chủ trọ, tôi muốn lập, kích hoạt, gia hạn hoặc kết thúc hợp đồng và hệ thống tự đồng bộ trạng thái phòng.
- **Acceptance criteria:**
  - Tạo draft, kích hoạt có xác nhận, kết thúc/cancel theo transition hợp lệ.
  - Không có hai active contracts chồng thời gian trên cùng phòng.
  - endDate sau startDate; deposit 0 là hợp lệ.
  - Kích hoạt chuyển room sang OCCUPIED; kết thúc chỉ chuyển AVAILABLE nếu không còn contract/reservation phù hợp.
  - Gia hạn tạo revision/phụ lục hoặc thay đổi có history, không sửa mất điều khoản đã xuất hóa đơn.
- **Bảng database:** rental_contracts, contract_tenants, rooms; đề xuất contract_amendments hoặc contract_versions nếu cần audit điều khoản.
- **API:** GET/POST /api/contracts; GET /api/contracts/{id}; PATCH /api/contracts/{id}/activate, /end, /cancel; đề xuất POST /api/contracts/{id}/renew.
- **UI:** Contract list/detail, create wizard, activate/end/renew dialogs, tenant view.
- **Validation và lỗi:** Overlap, inactive room/tenant, primary tenant, invalid status transition, endDate và monetary bounds; conflict trả 409.
- **Test:** overlap/state/room synchronization transaction tests; authorization; E2E draft → active → end/renew.
- **Priority:** P1.
- **Complexity:** L.

### P1-05 — Sổ tiền đặt cọc

- **Trạng thái:** C — contract mới chỉ có deposit_amount, chưa có thu/hoàn/khấu trừ và số dư cọc.
- **Dấu vết origin/example:** EXAMPLE-KHÔNG.
- **Mục đích:** Theo dõi tiền cọc tách biệt với doanh thu tiền thuê và có bằng chứng khi trả phòng.
- **Vai trò:** Landlord, staff, tenant read-only.
- **User story:** Là chủ trọ, tôi muốn ghi nhận thu, hoàn và khấu trừ cọc để luôn biết số dư cọc của từng hợp đồng.
- **Acceptance criteria:**
  - Có ledger bất biến cho RECEIVED, REFUNDED, DEDUCTED, ADJUSTED.
  - Số dư được tính từ transaction, không chỉ từ contract.deposit_amount.
  - Mọi hoàn/khấu trừ có lý do, người thực hiện và liên kết handover/payment khi phù hợp.
  - Không hoàn/khấu trừ vượt số dư; tenant xem được lịch sử của mình.
- **Bảng database:** rental_contracts; đề xuất deposit_transactions, có thể liên kết payments và handover_records.
- **API:** GET/POST /api/contracts/{id}/deposit-transactions; endpoint refund/deduct có idempotency.
- **UI:** Deposit summary, transaction timeline, receive/refund/deduct dialogs.
- **Validation và lỗi:** Amount lớn hơn 0; currency VND; không vượt số dư; contract ownership; duplicate request trả cùng kết quả.
- **Test:** ledger balance unit test; concurrent refund integration test; authorization; PDF/tenant visibility smoke test.
- **Priority:** P1.
- **Complexity:** L.

### P1-06 — Bàn giao, nhận phòng và trả phòng

- **Trạng thái:** C.
- **Dấu vết origin/example:** EXAMPLE-KHÔNG.
- **Mục đích:** Chuẩn hóa bằng chứng tình trạng phòng, tài sản, chỉ số và khoản khấu trừ khi bắt đầu/kết thúc thuê.
- **Vai trò:** Landlord, staff, tenant.
- **User story:** Là chủ trọ và người thuê, chúng tôi muốn xác nhận biên bản nhận/trả phòng để giảm tranh chấp.
- **Acceptance criteria:**
  - Có MOVE_IN và MOVE_OUT record với thời điểm, người lập và người xác nhận.
  - Snapshot chỉ số điện/nước, chìa khóa, tài sản và tình trạng phòng.
  - MOVE_OUT tính đề xuất công nợ/cọc nhưng cần người có quyền xác nhận.
  - Hợp đồng chỉ kết thúc hoàn tất khi rule bàn giao bắt buộc được thỏa hoặc có lý do override được audit.
- **Bảng database:** đề xuất handover_records, handover_items, handover_confirmations; liên kết rental_contracts, rooms, utility_readings, deposit_transactions và documents.
- **API:** POST/GET /api/contracts/{id}/handovers; PATCH /api/handovers/{id}/confirm; endpoint item/media dùng P2-04.
- **UI:** Handover checklist, meter snapshot, comparison move-in/move-out, confirmation screen.
- **Validation và lỗi:** Một loại handover active cho mỗi phase; reading không nhỏ hơn trước; không xác nhận hai lần; optimistic locking.
- **Test:** checklist/rule unit test; transaction integration; concurrent confirmation; E2E move-in và move-out.
- **Priority:** P1.
- **Complexity:** L.

### P1-07 — Chỉ số và đơn giá điện nước

- **Trạng thái:** B tại baseline — working tree đã có landlord utility UI, continuity/locking và tenant time-scope; rate plan theo hiệu lực/batch entry vẫn thiếu.
- **Dấu vết origin/example:** EXAMPLE-CÓ — có UtilitiesPage và utilityApi candidate.
- **Mục đích:** Ghi chỉ số hàng tháng chính xác và cung cấp dữ liệu nguồn cho hóa đơn.
- **Vai trò:** Landlord, staff, tenant read-only.
- **User story:** Là chủ trọ, tôi muốn nhập chỉ số điện nước theo phòng và kỳ, hệ thống tự tính mức dùng và tiền.
- **Acceptance criteria:**
  - Mỗi room/kỳ có tối đa một reading.
  - Old reading mặc định từ kỳ gần nhất nhưng có thể sửa trước khi invoice khóa.
  - New reading không nhỏ hơn old; usage và amount được tính server-side.
  - Đơn giá có hiệu lực theo ngày/kỳ, không làm thay đổi hóa đơn lịch sử.
  - Reading đã gắn invoice không được sửa tùy tiện; phải điều chỉnh có audit.
- **Bảng database:** utility_readings; đề xuất utility_rate_plans hoặc utility_rates có effective_from/effective_to.
- **API:** GET/POST /api/rooms/{roomId}/utility-readings; PUT /api/utility-readings/{id}; API rate plans.
- **UI:** Property/room/period selector, batch meter entry, history/chart nhỏ, tenant usage view.
- **Validation và lỗi:** Unique room-period; month 1–12; nonnegative reading/rate; stale update và invoice lock trả 409.
- **Test:** usage/rate calculation; duplicate period; previous reading; ownership; E2E create/edit/view.
- **Priority:** P1.
- **Complexity:** L.

### P1-08 — Danh mục dịch vụ và đơn giá theo hiệu lực

- **Trạng thái:** C — invoice hỗ trợ item SERVICE nhưng chưa có danh mục hoặc cấu hình giá định kỳ.
- **Dấu vết origin/example:** EXAMPLE-KHÔNG.
- **Mục đích:** Quản lý phí internet, rác, xe, vệ sinh và dịch vụ nhỏ phổ biến mà không tạo hệ thống kế toán lớn.
- **Vai trò:** Landlord, staff, tenant read-only.
- **User story:** Là chủ trọ, tôi muốn cấu hình dịch vụ theo khu/phòng/hợp đồng để hóa đơn tháng tự lấy đúng giá.
- **Acceptance criteria:**
  - Service có mã, tên, cách tính FIXED/PER_PERSON/PER_UNIT và trạng thái.
  - Rate có thời gian hiệu lực; đổi giá không sửa hóa đơn lịch sử.
  - Gán service cho property, room hoặc contract với override rõ ràng.
  - Tenant thấy chi tiết dịch vụ được tính.
- **Bảng database:** đề xuất service_definitions, service_rates, contract_services hoặc room_services.
- **API:** CRUD /api/services; /api/contracts/{id}/services; preview service charge theo kỳ.
- **UI:** Service catalog, effective-rate form, contract service assignment, invoice preview.
- **Validation và lỗi:** Rate không âm; không chồng effective range cùng scope; quantity hợp lệ; inactive service không được gán mới.
- **Test:** price resolution priority/effective date; overlap constraint; ownership; invoice integration.
- **Priority:** P1.
- **Complexity:** L.

### P1-09 — Tạo hóa đơn tháng định kỳ, chống trùng

- **Trạng thái:** B — current HEAD tạo hóa đơn thủ công; chưa có scheduler/run, preview hàng loạt và idempotency hoàn chỉnh.
- **Dấu vết origin/example:** EXAMPLE-MỘT PHẦN — có auto-fill rent/utility và cancel unpaid invoice candidate.
- **Mục đích:** Tạo hóa đơn tháng đáng tin cậy từ hợp đồng, điện nước và dịch vụ, không tạo trùng khi retry.
- **Vai trò:** Landlord, staff, tenant read-only.
- **User story:** Là chủ trọ, tôi muốn preview và phát hành hóa đơn tháng hàng loạt để giảm nhập tay.
- **Acceptance criteria:**
  - Preview rent, utility và service items trước khi phát hành.
  - Unique contract-period; retry cùng run không tạo invoice/item trùng.
  - Chỉ active contract trong kỳ được lập hóa đơn; xử lý prorate phải có policy rõ.
  - DRAFT có thể sửa; phát hành chuyển UNPAID; chỉ invoice chưa có completed payment mới cancel.
  - Có manual run trước; scheduler tự động chỉ bật sau khi manual flow ổn định.
- **Bảng database:** invoices, invoice_items, utility_readings, rental_contracts; đề xuất invoice_runs và unique index contract_id + billing_year + billing_month cho invoice chính.
- **API:** GET /api/invoices/preview; POST /api/invoice-runs; GET/POST /api/invoices; GET/PATCH /api/invoices/{id}; endpoint issue/cancel.
- **UI:** Billing period wizard, preview/bulk selection, invoice detail/items, run result và retry failures.
- **Validation và lỗi:** Month/year, dueDate, discount không vượt subtotal, item description/quantity/rate; missing reading/service warning; duplicate trả 409 hoặc idempotent response.
- **Test:** total calculation; unique/idempotency/concurrent run; partial-period policy; authorization; E2E preview → issue.
- **Priority:** P1.
- **Complexity:** XL.

### P1-10 — Công nợ, thanh toán và lịch sử giao dịch

- **Trạng thái:** B tại baseline — working tree đã có landlord payment UI, tenant history và lock chống overpay; refund/adjustment/idempotency/receipt vẫn thiếu.
- **Dấu vết origin/example:** EXAMPLE-CÓ — có candidate thu tiền, partial/full payment và history.
- **Mục đích:** Theo dõi số phải thu, đã thu, còn nợ và giao dịch theo hóa đơn.
- **Vai trò:** Landlord, staff/cashier, tenant read-only.
- **User story:** Là người thu tiền, tôi muốn ghi nhận từng khoản thanh toán và hệ thống tự cập nhật trạng thái hóa đơn.
- **Acceptance criteria:**
  - Hỗ trợ nhiều payment trên một invoice và CASH/BANK_TRANSFER/CARD/OTHER.
  - Invoice status chuyển UNPAID/PARTIALLY_PAID/PAID theo completed payments.
  - Không cho completed total vượt invoice total; failed/cancelled/refunded không tính sai paidAmount.
  - Transaction reference có thể unique theo provider/scope.
  - Có debt aging và payment timeline; tenant chỉ thấy của mình.
- **Bảng database:** payments, invoices, user_accounts; có thể thêm payment_allocations nếu sau này một payment trả nhiều invoice.
- **API:** GET/POST /api/invoices/{invoiceId}/payments; đề xuất refund/cancel payment endpoint có audit.
- **UI:** Invoice debt summary, collect-payment dialog, transaction history, tenant payment tab.
- **Validation và lỗi:** Amount > 0 và không vượt dư nợ; paidAt hợp lệ; method/reference; idempotency; concurrent payment dùng lock/version.
- **Test:** partial/full/status unit test; concurrent overpayment integration; authorization; E2E collect and tenant view.
- **Priority:** P1.
- **Complexity:** L.

### P1-11 — Phiếu thu, biên nhận và PDF

- **Trạng thái:** C.
- **Dấu vết origin/example:** EXAMPLE-KHÔNG.
- **Mục đích:** Cung cấp chứng từ dễ in/gửi cho người thuê sau thanh toán.
- **Vai trò:** Landlord, staff/cashier, tenant.
- **User story:** Là người thuê, tôi muốn nhận biên nhận có mã xác minh và chi tiết khoản đã trả.
- **Acceptance criteria:**
  - Mỗi completed payment có receipt number duy nhất, dữ liệu snapshot và người nhận tiền.
  - PDF hiển thị landlord/property/tenant/invoice/payment, không chứa secret.
  - Biên nhận lịch sử không thay đổi khi tên/địa chỉ sau này được sửa.
  - Download chỉ cho owner/tenant liên quan; có thể verify bằng mã không lộ dữ liệu nhạy cảm.
- **Bảng database:** payments, invoices; đề xuất receipts hoặc receipt_snapshots; documents nếu lưu object key.
- **API:** GET /api/payments/{id}/receipt; GET /api/receipts/{id}.pdf; optional verify endpoint trả dữ liệu tối thiểu.
- **UI:** Receipt preview, print/download action, tenant receipt history.
- **Validation và lỗi:** Chỉ payment COMPLETED; receipt generation idempotent; font tiếng Việt; lỗi object storage có retry nhưng không tạo receipt trùng.
- **Test:** template unit/snapshot; authorization; PDF content extraction; idempotency; E2E download.
- **Priority:** P1.
- **Complexity:** L.

### P1-12 — Bảo trì và phân công xử lý

- **Trạng thái:** B — tenant gửi request và landlord update status đã có; assigned_to tồn tại nhưng chưa có workflow kỹ thuật viên, media, cost hoặc transition rule đầy đủ.
- **Dấu vết origin/example:** EXAMPLE-MỘT PHẦN.
- **Mục đích:** Theo dõi yêu cầu từ lúc gửi đến khi hoàn thành, có trách nhiệm và lịch sử rõ ràng.
- **Vai trò:** Tenant, landlord, staff, technician.
- **User story:** Là người thuê, tôi muốn theo dõi sửa chữa; là chủ trọ, tôi muốn phân công và biết tiến độ.
- **Acceptance criteria:**
  - State transition PENDING → IN_PROGRESS → COMPLETED hoặc CANCELLED theo rule.
  - Có assignee, priority, due date tùy chọn, resolution note và timestamps tự động.
  - Technician chỉ thấy request được phân công hoặc property được cấp quyền.
  - Tenant thấy tiến độ nhưng không thấy ghi chú nội bộ nhạy cảm.
  - COMPLETED yêu cầu resolution; reopen phải có lý do và audit.
- **Bảng database:** maintenance_requests, user_accounts; đề xuất maintenance_comments và liên kết documents.
- **API:** GET/POST /api/maintenance-requests; PATCH /{id}/status; đề xuất /assign, /comments.
- **UI:** Tenant request form/history, landlord kanban/list/detail, technician work queue.
- **Validation và lỗi:** Room/tenant ownership, transition, assignee role/scope, title/description limits; concurrent update dùng version.
- **Test:** state/assignment authorization; tenant isolation; notification trigger; E2E submit → assign → complete.
- **Priority:** P1.
- **Complexity:** L.

### P1-13 — Cổng người thuê và thông báo trong ứng dụng

- **Trạng thái:** B — portal đã xem room/contract/invoice/latest utility/maintenance; payment history có trong response nhưng chưa render, chưa có notification center.
- **Dấu vết origin/example:** EXAMPLE-MỘT PHẦN — portal đầy đủ hơn nhưng chưa có thông báo in-app tổng quát.
- **Mục đích:** Cho tenant tự xem thông tin liên quan và nhận nhắc việc mà không cần liên hệ thủ công.
- **Vai trò:** Tenant.
- **User story:** Là người thuê, tôi muốn xem hợp đồng, công nợ, thanh toán, điện nước, sửa chữa và thông báo của riêng mình.
- **Acceptance criteria:**
  - Portal có các tab tổng quan, hợp đồng, hóa đơn, payment, utility, maintenance và documents được phép.
  - “Cần thanh toán” là tổng dư nợ phù hợp, không lấy tùy tiện invoice đầu tiên.
  - Danh sách được sort/paginate đúng; loading/error không hiển thị số 0 gây hiểu nhầm.
  - Có notification in-app read/unread cho invoice phát hành, sắp đến hạn, payment và maintenance.
  - Không truy cập được dữ liệu tenant khác bằng ID hoặc cache cũ.
- **Bảng database:** tenants, rental_contracts, invoices, payments, utility_readings, maintenance_requests; đề xuất notifications, notification_recipients.
- **API:** /api/tenant-portal/summary và resource endpoints; GET/PATCH /api/notifications.
- **UI:** Tenant portal responsive, notification center/badge, detail pages.
- **Validation và lỗi:** Tenant phải liên kết user_account; dữ liệu không có active contract có empty state rõ; notification read chỉ đúng recipient.
- **Test:** aggregate/debt calculation; ownership; notification unread; responsive E2E tenant flow.
- **Priority:** P1.
- **Complexity:** L.

## 7. P2 — Hoàn thiện sản phẩm và khả năng thương mại hóa

### P2-01 — Quản lý tài khoản, nhân viên và kỹ thuật viên

- **Trạng thái:** B tại baseline — working tree đã có khóa/mở account và bootstrap ADMIN production; create/edit/assign scoped staff/technician và last-admin guard vẫn thiếu.
- **Dấu vết origin/example:** EXAMPLE-MỘT PHẦN — có khóa/mở account candidate, chưa có staff/technician scoped roles.
- **Mục đích:** Cho chủ trọ phân việc mà không chia sẻ tài khoản toàn quyền và cho admin vận hành account an toàn.
- **Vai trò:** Admin, landlord, staff, technician.
- **User story:** Là chủ trọ, tôi muốn mời nhân viên/kỹ thuật viên và giới hạn họ theo khu trọ/chức năng.
- **Acceptance criteria:**
  - Admin có create/update/lock/unlock và xem role/status account.
  - Landlord mời staff/technician, gán property scope và permission preset.
  - Không tự nâng quyền; không khóa admin cuối cùng hoặc chính mình nếu gây mất quyền quản trị.
  - Tenant account linking rõ ràng, không tạo trùng email.
  - Mọi thay đổi quyền được audit và thu hồi phiên khi cần.
- **Bảng database:** user_accounts, roles, user_roles; migration role constraint; đề xuất property_memberships, invitations.
- **API:** /api/admin/users, /api/roles; /api/properties/{id}/members; invitation accept/revoke endpoints.
- **UI:** Admin account management, landlord team screen, invitation flow, role/scope matrix.
- **Validation và lỗi:** Email unique, valid role transition, scope ownership, last-admin guard, locked account session revoke.
- **Test:** privilege escalation/IDOR; invitation expiry; lock/unlock; property scope; E2E admin and staff.
- **Priority:** P2.
- **Complexity:** XL.

### P2-02 — Tìm kiếm, lọc, phân trang và sắp xếp

- **Trạng thái:** C — current list API/UI chủ yếu tải toàn bộ mảng.
- **Dấu vết origin/example:** EXAMPLE-KHÔNG.
- **Mục đích:** Duy trì hiệu năng và khả năng sử dụng khi số phòng, tenant, invoice và giao dịch tăng.
- **Vai trò:** Admin, landlord, staff, technician, tenant.
- **User story:** Là người vận hành, tôi muốn tìm nhanh record theo từ khóa, trạng thái, kỳ và khu trọ.
- **Acceptance criteria:**
  - API dùng page/size/sort và filter whitelist; response có totalElements/totalPages.
  - Search phù hợp cho room number, tenant name/phone, contract/invoice code.
  - Filter theo property, room/status, period, debt, priority/assignee.
  - URL giữ query state để refresh/share nội bộ; mobile có filter drawer.
  - Có index dựa trên EXPLAIN và dữ liệu thực tế, không thêm index mù quáng.
- **Bảng database:** Index trên properties, rooms, tenants, rental_contracts, invoices, payments, maintenance_requests; có thể dùng PostgreSQL trigram sau khi đo.
- **API:** Chuẩn hóa query parameters cho các GET list hiện có.
- **UI:** Search box, filter chips/drawer, sortable headers, pagination, clear filters.
- **Validation và lỗi:** Giới hạn page size, sort field whitelist, normalize search; query quá rộng không gây timeout.
- **Test:** repository specification/filter; pagination boundary; ownership cùng filter; UI URL state; performance smoke test.
- **Priority:** P2.
- **Complexity:** L.

### P2-03 — Dashboard và báo cáo vận hành

- **Trạng thái:** B — có dashboard count và occupancy đơn giản; chưa có revenue, debt aging, cash flow hoặc drill-down.
- **Dấu vết origin/example:** EXAMPLE-MỘT PHẦN.
- **Mục đích:** Cung cấp thông tin đủ ra quyết định hàng ngày mà không xây hệ thống BI lớn.
- **Vai trò:** Landlord, admin, staff được cấp quyền.
- **User story:** Là chủ trọ, tôi muốn xem doanh thu, công nợ, lấp đầy và việc cần xử lý theo kỳ/khu trọ.
- **Acceptance criteria:**
  - KPI gồm occupancy đúng mẫu số, invoiced revenue, collected amount, outstanding/overdue, expiring contracts và pending maintenance.
  - Filter property/date range; số liệu click được để drill-down sang danh sách đã lọc.
  - Tiền dùng BigDecimal/server aggregation, timezone Asia/Ho_Chi_Minh được thống nhất.
  - Error/loading không hiển thị 0 như dữ liệu thật.
  - Export CSV cho báo cáo thiết yếu.
- **Bảng database:** rooms, rental_contracts, invoices, payments, maintenance_requests; có thể thêm materialized view sau khi đo hiệu năng.
- **API:** GET /api/dashboard/summary với filter; /api/reports/revenue, /debt-aging, /occupancy.
- **UI:** KPI cards, chart nhỏ, work queue, report table/export.
- **Validation và lỗi:** Date range/maximum period; ownership; currency/timezone; không double-count cancelled/refunded.
- **Test:** aggregation fixtures; timezone boundaries; authorization; reconcile totals với transaction samples; E2E filters/export.
- **Priority:** P2.
- **Complexity:** L.

### P2-04 — Tài liệu và hình ảnh liên quan

- **Trạng thái:** C — README hiện chủ động defer attachment.
- **Dấu vết origin/example:** EXAMPLE-KHÔNG.
- **Mục đích:** Lưu hợp đồng scan, CCCD theo quyền, ảnh bàn giao và ảnh sửa chữa an toàn.
- **Vai trò:** Landlord, staff, technician, tenant theo quyền.
- **User story:** Là người vận hành, tôi muốn đính kèm tài liệu/ảnh vào đúng nghiệp vụ và kiểm soát ai được tải.
- **Acceptance criteria:**
  - Metadata lưu DB; binary ở object storage, không lưu blob lớn trong PostgreSQL.
  - Có entity type/id, category, owner, uploader, checksum, size, content type và object key.
  - Upload dùng signed URL hoặc streaming an toàn; download luôn kiểm tra authorization.
  - Có virus scan/quarantine, giới hạn loại/size và lifecycle delete.
  - Dữ liệu giấy tờ nhạy cảm có retention và audit download.
- **Bảng database:** đề xuất documents, document_links hoặc attachments; liên kết tenants, contracts, handovers, maintenance_requests.
- **API:** POST upload-init/complete; GET metadata/download-url; DELETE soft-delete.
- **UI:** Attachment gallery/list, upload progress, preview, permission/error state.
- **Validation và lỗi:** MIME thực tế, extension, size, checksum, ownership, scan status; không tin filename/path từ client.
- **Test:** authorization/IDOR; invalid MIME/oversize; failed multipart cleanup; signed URL expiry; E2E upload/view.
- **Priority:** P2.
- **Complexity:** XL.

### P2-05 — Nhật ký hoạt động và lịch sử thay đổi

- **Trạng thái:** C — entity có created/updated timestamps nhưng chưa có business audit log bất biến.
- **Dấu vết origin/example:** EXAMPLE-KHÔNG.
- **Mục đích:** Truy vết ai thay đổi hợp đồng, tiền, quyền, trạng thái và cấu hình quan trọng.
- **Vai trò:** Admin, landlord, support/auditor được cấp quyền.
- **User story:** Là chủ hệ thống, tôi muốn biết ai đã thay đổi gì và khi nào để điều tra sai lệch.
- **Acceptance criteria:**
  - Audit event có actor, action, entity, entityId, timestamp, property scope, requestId và before/after đã redact.
  - Ghi trong cùng transaction hoặc outbox bảo đảm không mất event quan trọng.
  - Không cho sửa/xóa audit qua API thường.
  - Các event bắt buộc: role/account, contract, invoice, payment/refund, deposit, handover, maintenance và document access nhạy cảm.
  - Có filter và retention policy.
- **Bảng database:** đề xuất audit_logs; có thể thêm outbox_events nếu dùng async.
- **API:** GET /api/audit-logs với scope và pagination; không có update/delete thông thường.
- **UI:** Activity timeline trên detail và audit explorer cho admin/landlord.
- **Validation và lỗi:** Redact token/password/CCCD; giới hạn before/after; không log raw file content.
- **Test:** event emitted/rollback; authorization/scope; redaction; immutable repository; query/filter.
- **Priority:** P2.
- **Complexity:** XL.

### P2-06 — UX responsive, accessibility và trạng thái giao diện

- **Trạng thái:** B — đã có Tailwind responsive/mobile nav nhưng một số page render trùng, thiếu error/loading và accessibility.
- **Dấu vết origin/example:** EXAMPLE-MỘT PHẦN — có responsive improvements và mobile Playwright project candidate.
- **Mục đích:** Giúp chủ trọ và tenant sử dụng ổn định trên điện thoại, đồng thời giảm lỗi thao tác.
- **Vai trò:** Tất cả người dùng.
- **User story:** Là người dùng điện thoại, tôi muốn hoàn tất các nghiệp vụ chính mà không phải zoom, cuộn ngang khó dùng hoặc đoán trạng thái hệ thống.
- **Acceptance criteria:**
  - Không render trùng cards/table; layout hoạt động từ 320 px đến desktop.
  - Mọi icon button có accessible name; label liên kết input; focus visible và keyboard navigation.
  - Drawer/dialog có focus trap, Escape, body lock và focus return.
  - Shared loading/empty/error/success; mutation không mất dữ liệu; status được Việt hóa nhất quán.
  - Có 404, Forbidden, offline/network error và unsaved-changes warning phù hợp.
- **Bảng database:** Không có.
- **API:** Không thêm API; cần error contract P0-05 và pagination P2-02.
- **UI:** Design tokens/primitives, responsive tables/cards, toast, skeleton, ErrorBoundary, accessibility fixes.
- **Validation và lỗi:** Client validation đồng bộ rule server nhưng server là nguồn quyết định cuối; error có aria-live.
- **Test:** axe hoặc accessibility assertions; keyboard tests; viewport 320/375/tablet/desktop; visual smoke; Playwright mobile.
- **Priority:** P2.
- **Complexity:** L.

## 8. P3 — Chức năng nâng cao trong tương lai

### P3-01 — Nhắc việc và thông báo email/SMS theo sự kiện

- **Trạng thái:** D — notification in-app cơ bản thuộc P1-13; external delivery chưa cần cho MVP.
- **Dấu vết origin/example:** EXAMPLE-KHÔNG.
- **Mục đích:** Tự động nhắc hóa đơn, hợp đồng sắp hết hạn và cập nhật sửa chữa sau khi luồng nội bộ đã ổn định.
- **Vai trò:** Landlord, tenant, admin.
- **User story:** Là người thuê, tôi muốn nhận nhắc trước hạn qua kênh đã đồng ý để tránh quên thanh toán.
- **Acceptance criteria:**
  - Event template versioned; preference/consent theo user và channel.
  - Outbox + retry/backoff; provider callback cập nhật delivery status.
  - Không gửi trùng cùng event/recipient/channel; có quiet hours và opt-out.
  - Không đưa dữ liệu nhạy cảm vào SMS/email ngoài mức cần thiết.
- **Bảng database:** notifications, notification_preferences, notification_deliveries, outbox_events.
- **API:** User preferences; admin template/test; webhook delivery-status có signature validation.
- **UI:** Notification settings, delivery history, template preview cho admin.
- **Validation và lỗi:** Phone/email verified, provider rate limit, signature/replay protection, consent.
- **Test:** outbox/idempotency; provider mock; retry/dead-letter; opt-out; template localization.
- **Priority:** P3.
- **Complexity:** XL.

### P3-02 — Thanh toán trực tuyến và đối soát tự động

- **Trạng thái:** D — docs/requirements.md xác định online payment ngoài MVP; chỉ làm sau khi payment ledger P1-10 ổn định.
- **Dấu vết origin/example:** EXAMPLE-KHÔNG.
- **Mục đích:** Cho tenant thanh toán thuận tiện và tự đối soát mà vẫn giữ sổ giao dịch nội bộ là nguồn sự thật.
- **Vai trò:** Tenant, landlord, accountant/staff, admin.
- **User story:** Là người thuê, tôi muốn thanh toán hóa đơn online và nhận biên nhận sau khi giao dịch được xác nhận.
- **Acceptance criteria:**
  - Tạo payment intent theo dư nợ hiện tại; không tin amount từ client.
  - Webhook có signature, replay protection và idempotency.
  - Chỉ webhook/provider verification mới chuyển payment sang COMPLETED.
  - Có reconciliation report cho thiếu/thừa/không khớp và quy trình refund.
  - Không lưu card data; tuân thủ yêu cầu của provider được chọn.
- **Bảng database:** payments; đề xuất payment_intents, payment_provider_events, reconciliation_runs.
- **API:** Create intent, query status, signed webhook, reconciliation và refund endpoints.
- **UI:** Tenant checkout/status, landlord reconciliation, failed/pending payment recovery.
- **Validation và lỗi:** Invoice ownership/status, amount, currency, expired intent, duplicate/replayed webhook, provider timeout.
- **Test:** provider sandbox; signature/replay/idempotency; concurrent webhook; reconciliation fixtures; E2E success/failure/pending.
- **Priority:** P3.
- **Complexity:** XL.

## 9. Thứ tự triển khai đề xuất

1. Hoàn tất P0-01 đến P0-07 và tạo baseline CI xanh.
2. Review các commit trên origin/example; tái sử dụng có chọn lọc các phần property/tenant/contract/utility/payment/role guard thay vì viết lại.
3. Khóa first-flow P1-01 bằng regression test.
4. Triển khai P1 theo chuỗi phụ thuộc:
   - P1-02 và P1-03.
   - P1-04, P1-05 và P1-06.
   - P1-07 và P1-08.
   - P1-09, P1-10 và P1-11.
   - P1-12 và P1-13.
5. Chỉ bắt đầu P2 sau khi các transaction contract/invoice/payment đã có integration test ổn định.
6. P3 cần ADR riêng cho nhà cung cấp, chi phí, bảo mật và vận hành trước khi lập sprint.

## 10. Quyết định kiến trúc cần chốt theo từng giai đoạn

- **Đã chốt ở P0-03:** access JWT 15 phút chỉ giữ trong memory; opaque refresh token 7 ngày chỉ ở cookie HttpOnly và lưu hash trong database; rotation/reuse/revocation theo [tài liệu bảo mật xác thực](authentication-security.md).
- RESERVED là trạng thái vật lý của room hay trạng thái dẫn xuất từ room_reservations.
- Gia hạn hợp đồng tạo contract mới, amendment hay version.
- Payment/deposit/refund dùng các ledger tách biệt hay mô hình transaction chung.
- Cách tính prorate, ngày chốt điện nước, ngày phát hành và hạn thanh toán.
- Object storage, virus scanning và retention cho tài liệu.
- Quyền STAFF/TECHNICIAN theo preset hay permission chi tiết.
- Scheduler chỉ được bật sau khi invoice run thủ công có idempotency và quan sát đầy đủ.

Mọi quyết định trên cần được ghi thành ADR ngắn, có phương án tương thích dữ liệu và không sửa migration đã chạy.
