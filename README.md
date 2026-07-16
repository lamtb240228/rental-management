# Rental Management System

Ứng dụng quản lý dãy phòng trọ theo kiến trúc modular monolith: Spring Boot REST API, React SPA và PostgreSQL. Hệ thống tập trung vào vận hành khu trọ quy mô nhỏ đến vừa, với dữ liệu được phân tách theo chủ trọ.

## Công nghệ

- Backend: Java 21, Spring Boot 3.5, Spring Security/JWT, JPA/Hibernate, Flyway, PostgreSQL 16, Springdoc OpenAPI và Actuator.
- Frontend: Node.js 24, React 19, TypeScript, Vite, Tailwind CSS, TanStack Query, Axios, React Hook Form và Zod.
- Kiểm thử: JUnit 5, MockMvc, Testcontainers, Vitest, React Testing Library và Playwright.
- Triển khai: Docker Compose, image backend nhiều stage và Nginx unprivileged phục vụ SPA/reverse proxy.

## Phạm vi hiện có

| Nhóm | Chủ trọ | Người thuê | Quản trị hệ thống |
| --- | --- | --- | --- |
| Xác thực | Đăng ký, đăng nhập, khôi phục phiên, đăng xuất, đổi mật khẩu, hồ sơ | Đăng nhập, khôi phục phiên, đăng xuất, đổi mật khẩu, hồ sơ | Đăng nhập, khôi phục phiên, đăng xuất, đổi mật khẩu, hồ sơ |
| Khu trọ/phòng | Tạo, xem, sửa, ngừng hoạt động | Xem phòng đang thuê | Số liệu tổng quan |
| Người thuê | Tạo, xem, sửa, lịch sử thuê | Xem hồ sơ của mình | Danh sách tài khoản |
| Hợp đồng | Tạo, xem, chấm dứt | Xem hợp đồng của mình | Số liệu tổng quan |
| Điện/nước | Ghi và sửa chỉ số, tính lượng dùng/chi phí | Xem chỉ số liên quan | — |
| Hóa đơn/thanh toán | Tạo hóa đơn, hủy hóa đơn chưa thu, ghi nhận thu tiền | Xem hóa đơn; API đã trả lịch sử thanh toán, UI còn trong backlog | Số liệu tổng quan |
| Bảo trì | Xem, cập nhật trạng thái/kết quả | Gửi và theo dõi yêu cầu | Tổng yêu cầu chờ xử lý |
| Tài khoản | — | — | Khóa/mở khóa tài khoản |

Backend kiểm tra vai trò và quyền sở hữu dữ liệu; frontend có route guard tương ứng. Các ràng buộc quan trọng về hợp đồng đang hoạt động, chỉ số đã lên hóa đơn, quan hệ tenant–room và thanh toán đồng thời được bảo vệ ở service/database.

Đánh giá đầy đủ và phần còn thiếu nằm trong:

- [Phân tích hiện trạng](docs/current-state-analysis.md)
- [Nghiên cứu sản phẩm](docs/product-research.md)
- [Backlog P0–P3](docs/product-backlog.md)
- [Thiết kế database](docs/database-design.md)
- [Database schema](docs/database-schema.md)
- [Bảo mật xác thực và phiên đăng nhập](docs/authentication-security.md)

## Phiên đăng nhập an toàn

P0-03 sử dụng access token ngắn hạn kết hợp refresh session có thể thu hồi:

- JWT access token mặc định sống 15 phút, chỉ được giữ trong memory của frontend và gửi bằng `Authorization: Bearer ...`; frontend chủ động xóa khóa token legacy khỏi `localStorage` và `sessionStorage`.
- Refresh token là opaque value 256 bit sinh bằng `SecureRandom`. Raw token chỉ đi qua cookie HttpOnly; PostgreSQL chỉ lưu SHA-256 hash trong bảng `refresh_sessions` do migration V7 tạo.
- Refresh session có thời hạn tuyệt đối mặc định 7 ngày. Mỗi lần refresh rotate token trong transaction và giữ cùng family; reuse token cũ revoke toàn bộ family để không tạo hai successor hợp lệ.
- Cookie `rental_refresh` có `HttpOnly`, `SameSite=Strict`, host-only và `Path=/api/auth`; profile production bắt buộc thêm `Secure`. Auth response dùng `Cache-Control: no-store`.
- Frontend khôi phục phiên qua cookie sau khi reload, gộp các 401 đồng thời vào một refresh promise, chỉ retry mỗi request một lần và chỉ phát tín hiệu login/logout không chứa credential giữa các tab.

Các endpoint của vòng đời phiên:

| Method | Endpoint | Hành vi |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Trả access token và tạo refresh cookie/session mới |
| `POST` | `/api/auth/register` | Giữ hành vi tự đăng nhập, tạo refresh cookie/session mới |
| `POST` | `/api/auth/refresh` | Kiểm tra cookie, rotate refresh token và trả access token mới |
| `POST` | `/api/auth/logout` | Revoke family hiện tại và xóa cookie |
| `POST` | `/api/auth/logout-all` | Revoke mọi refresh session của account và xóa cookie |
| `POST` | `/api/auth/change-password` | Đổi mật khẩu, revoke mọi refresh session và xóa cookie |
| `GET` | `/api/auth/me` | Trả profile theo access token hiện tại |

Chi tiết schema, concurrency, cookie policy và failure behavior nằm trong [tài liệu bảo mật xác thực](docs/authentication-security.md). Thiết kế tham chiếu [Spring Security stateless session management](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html), [OWASP Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html) và [RFC 9700 về refresh-token rotation/replay](https://www.rfc-editor.org/rfc/rfc9700.html).

## Cấu hình môi trường

Yêu cầu: Docker Desktop, Java 21 và Node.js 24.

Tạo file cấu hình cục bộ từ mẫu:

```powershell
Copy-Item .env.example .env
```

Trước khi chạy, bắt buộc thay:

- `POSTGRES_ADMIN_PASSWORD`: mật khẩu bootstrap/operations của PostgreSQL; không cấp cho backend.
- `DB_APP_PASSWORD`: mật khẩu riêng của role ứng dụng không có quyền superuser.
- `JWT_SECRET`: chuỗi ngẫu nhiên tối thiểu 32 ký tự.
- Khi production chưa có ADMIN thật: `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD` (12–72 ký tự, có hoa/thường/số/ký hiệu) và `BOOTSTRAP_ADMIN_FULL_NAME`. Sau lần khởi động tạo ADMIN thành công, xóa ba biến bootstrap khỏi environment rồi recreate riêng backend.

Các mặc định session có thể được override qua environment:

- `JWT_EXPIRATION_MINUTES`: thời hạn access token, mặc định `15`.
- `REFRESH_TOKEN_EXPIRATION_DAYS`: thời hạn tuyệt đối của refresh session, mặc định `7`.
- `REFRESH_COOKIE_SAME_SITE`: chỉ nhận `Strict` hoặc `Lax`, mặc định `Strict`; không nới chính sách cookie nếu chưa đánh giá lại CSRF và topology triển khai.

Không commit `.env`. Database/JWT và bootstrap ADMIN lần đầu fail-closed nếu thiếu hoặc sai cấu hình. Các origin localhost trong file mẫu chỉ dành cho local và bắt buộc phải thay bằng domain thật khi triển khai.
Các biến hệ điều hành chung `DEBUG`/`TRACE` bị bỏ qua để tránh log request ngoài ý muốn; chỉ dùng `RENTAL_DEBUG`/`RENTAL_TRACE` khi chẩn đoán local. Profile production luôn cưỡng chế tắt hai chế độ này.

Namespace Docker mặc định đã được tách riêng để không đụng project khác:

| Tài nguyên | Giá trị |
| --- | --- |
| Compose project | `rental-management-datn1` |
| Database | `rental_management_datn1_db` |
| PostgreSQL bootstrap admin | `rental_management_admin` |
| PostgreSQL application/table/sequence owner | `rental_management_app` (non-superuser) |
| Host port | `45432` |
| Volume | `rental-management-datn1_postgres_data` |
| Network | `rental-management-datn1_rental_network` |

Không dùng `docker compose down -v` khi xử lý lỗi. Xem [hướng dẫn backup/restore và chuyển dữ liệu](docs/database-operations.md).

## Chạy local để phát triển

Từ thư mục gốc:

```powershell
docker compose config --quiet
docker compose up -d postgres
docker compose ps
```

Chạy backend:

```powershell
Set-Location backend
.\mvnw.cmd spring-boot:run
```

Các URL backend:

- API: `http://localhost:8080/api`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

Chạy frontend ở terminal khác:

```powershell
Set-Location frontend
npm.cmd ci
npm.cmd run dev
```

Frontend: `http://localhost:5173`.

### Dữ liệu demo

`DEMO_DATA_ENABLED=true` chỉ dành cho development/test. Khi bật, ba tài khoản seed được kích hoạt:

| Vai trò | Email | Mật khẩu |
| --- | --- | --- |
| LANDLORD | `demo@rental.local` | `Password123!` |
| ADMIN | `admin@rental.local` | `Password123!` |
| TENANT | `tenant@rental.local` | `Password123!` |

Migration mới khóa các tài khoản biết trước này theo mặc định. Profile `prod` luôn tắt demo và cưỡng chế khóa lại các tài khoản demo ở mỗi lần khởi động, kể cả khi volume trước đó từng chạy ở development; API quản trị cũng từ chối thay đổi trạng thái các account này khi demo bị tắt. Vì không được sửa V2–V4 đã chạy, database mới vẫn chứa các record mẫu nhận diện được; chúng thuộc tài khoản bị khóa và được loại khỏi danh sách tài khoản/thống kê ADMIN khi demo tắt. Record không bị xóa để giữ tương thích Flyway, nhưng các mã `*-DEMO-*` vẫn được xem là dữ liệu dành riêng. Production phải dùng database/volume riêng và tuyệt đối không kích hoạt lại tài khoản demo.

## Luồng vận hành cốt lõi

```text
đăng nhập
  -> tạo khu trọ/phòng
  -> tạo người thuê
  -> tạo hợp đồng
  -> ghi chỉ số điện/nước
  -> tạo hóa đơn
  -> ghi nhận một hoặc nhiều lần thanh toán
  -> tiếp nhận và xử lý bảo trì
```

## Chạy bằng container

Stack production cơ bản gồm PostgreSQL, backend và frontend/Nginx. Production thật nên dùng file env, Compose project và volume tách khỏi development để dữ liệu demo/kiểm thử không đi chung database:

```powershell
Copy-Item .env.example .env.production
# Điền hai mật khẩu DB khác nhau, JWT secret, ADMIN bootstrap lần đầu,
# POSTGRES_DB, origin và port production trong .env.production.

docker compose --env-file .env.production -p rental-management-datn1-prod --profile app config --quiet
docker compose --env-file .env.production -p rental-management-datn1-prod --profile app up -d --build
docker compose --env-file .env.production -p rental-management-datn1-prod --profile app ps
```

Lần khởi động production đầu tiên sẽ thất bại nếu chưa có ADMIN active và thiếu bộ biến bootstrap. Sau khi đăng nhập ADMIN thành công, bỏ ba biến `BOOTSTRAP_ADMIN_*` khỏi secret store/file env và recreate service `backend`; tài khoản đã tạo vẫn được giữ. Không dùng email demo cho bootstrap. PostgreSQL init cũng từ chối nếu admin role và app role trùng tên hoặc dùng cùng mật khẩu.

Giao diện mặc định ở `http://localhost:8081`; health tổng hợp ở `http://localhost:8081/healthz`. Backend không publish port trực tiếp trong profile này và frontend chuyển tiếp `/api` qua network nội bộ.

Trước khi triển khai Internet:

- đặt `CORS_ALLOWED_ORIGINS` đúng domain;
- đặt `CONTAINER_CORS_ALLOWED_ORIGINS` bằng public origin của frontend container;
- đặt TLS tại reverse proxy/load balancer biên;
- giữ profile `prod` để cookie refresh luôn có `Secure`; credentialed CORS chỉ dùng origin được cấu hình rõ ràng, không dùng wildcard;
- nếu Nginx nằm sau load balancer, chỉ cấu hình `real_ip_header`/`set_real_ip_from` cho CIDR của proxy tin cậy; cấu hình mặc định chủ động bỏ forwarded IP do client gửi;
- dùng secret riêng cho từng môi trường và giới hạn quyền truy cập file env;
- thực hiện backup/restore drill;
- dùng storage bền vững, quan sát health/metrics và thiết lập cảnh báo;
- giữ `DEMO_DATA_ENABLED=false`.

Swagger bị tắt trong profile production; health không lộ chi tiết, metrics yêu cầu vai trò ADMIN.

## Kiểm thử

Backend:

```powershell
Push-Location backend
.\mvnw.cmd clean test
Pop-Location
```

Frontend:

```powershell
Push-Location frontend
npm.cmd ci
npm.cmd run lint
npm.cmd run typecheck
npm.cmd run test:run
npm.cmd run build
Pop-Location
```

E2E cần PostgreSQL và backend development đang chạy với demo data:

```powershell
Push-Location frontend
npx.cmd playwright install
npm.cmd run test:e2e
Pop-Location
```

Playwright có project Chromium, Firefox, WebKit và Pixel 7. Không coi E2E thành công nếu browser binary chưa được cài.

Cổng kiểm tra P0-03 phải bao phủ migration V1–V7 trên PostgreSQL Testcontainers; login/cookie/hash-at-rest; refresh rotation, expiry, forgery, reuse và concurrent refresh; logout/logout-all/change-password/account lock; frontend memory-only storage, bootstrap refresh, single-flight 401, retry tối đa một lần, cross-tab logout và late-response race. Chỉ đóng checkpoint khi backend clean test, frontend lint/typecheck/unit/build, E2E authentication phù hợp, `npm audit`, Docker Compose config, `git diff --check` và secret scan đều đạt.

## Quy tắc migration và dữ liệu

- Flyway là nguồn sự thật; Hibernate dùng `ddl-auto: validate`.
- Không sửa migration đã chạy. Mọi thay đổi schema/seed tiếp theo phải dùng version mới.
- Không xóa volume để “sửa” kết nối database.
- Sao lưu trước thay đổi dữ liệu và kiểm tra archive bằng restore thử.
- Không đưa dữ liệu cá nhân thật, token, password hoặc dump database vào Git.

## Ghi chú phạm vi

Kiến trúc modular monolith hiện tại được giữ nguyên. Các hạng mục như đặt cọc, bàn giao/trả phòng, đơn giá dịch vụ theo thời gian, hóa đơn định kỳ, biên nhận PDF, audit log, thông báo và file đính kèm nằm trong backlog triển khai tiếp theo; hệ thống chưa được tuyên bố là hoàn tất thương mại cho đến khi các acceptance criteria P1/P2 tương ứng được đóng.

Project có sử dụng AI hỗ trợ phát triển để khảo sát, kiểm thử và triển khai mẫu. Mọi thay đổi vẫn cần code review, kiểm thử và kiểm soát vận hành trước khi đưa vào production.
