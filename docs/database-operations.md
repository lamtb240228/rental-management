# PostgreSQL và Docker Operations

## 1. Namespace chính thức

| Tài nguyên | Giá trị mặc định mới |
| --- | --- |
| Compose project | `rental-management-datn1` |
| Service | `postgres` |
| Container do Compose tạo | `rental-management-datn1-postgres-1` |
| PostgreSQL database | `rental_management_datn1_db` |
| PostgreSQL bootstrap/operations admin | `rental_management_admin` |
| PostgreSQL application/table/sequence owner | `rental_management_app` — `NOSUPERUSER` |
| Host port | `45432` |
| Docker volume | `rental-management-datn1_postgres_data` |
| Docker network | `rental-management-datn1_rental_network` |

`container_name` cố định đã được bỏ. Tên container do Compose sinh từ project/service, tránh một tên global bị hai project tranh chấp. Có thể chạy một instance khác bằng cách đổi `COMPOSE_PROJECT_NAME`, `POSTGRES_PORT` và database trong file `.env` riêng. Nếu backend chạy trực tiếp ngoài Compose, đồng thời cập nhật `SPRING_DATASOURCE_URL` tới database/port của instance đó.

Port `55432` từng được cân nhắc nhưng nằm trong dải cổng WinNAT/Hyper-V bị Windows giữ trên máy audit. `45432` đã được kiểm tra và Docker bind thành công.

## 2. Nguyên nhân xung đột cũ

Hai cấu hình khác nhau từng dùng chung Compose project `rental-management` và service `postgres`:

| Nguồn | Container | User | Database | Volume |
| --- | --- | --- | --- | --- |
| Cấu hình cũ trên lịch sử branch `main` | `rental_management_postgres` | `postgres` | `rental_management` | `rental-management_rental_management_postgres_data` |
| Cấu hình baseline branch `test` | `rental-management-postgres` | `rental` | `rental_management` | `rental-management_postgres_data` |

Container đang chạy có label Compose project/service giống cấu hình hiện tại dù tên, image, user và volume khác. Vì vậy `docker compose up` có thể recreate cùng logical service nhưng attach một volume khác.

Kết quả kiểm tra dữ liệu:

- `rental-management_rental_management_postgres_data`: Flyway V1-V3 của schema khác, 4 base table.
- `rental-management_postgres_data`: Flyway V1-V4 của Rental Management MVP, 14 base table nếu tính `flyway_schema_history`.

Hai volume cũ không bị xóa.

## 3. Lưu ý quan trọng về image PostgreSQL

`POSTGRES_DB`, `POSTGRES_USER` và `POSTGRES_PASSWORD` chỉ được image PostgreSQL sử dụng khi data directory còn trống. Đổi các biến này không tự đổi tên database/user trong một volume đã khởi tạo.

Vì vậy không được chỉ sửa `.env` rồi cho rằng dữ liệu cũ sẽ xuất hiện trong database mới. Phải dùng một trong hai cách:

1. `pg_dump`/`pg_restore` sang database/volume mới — cách khuyến nghị.
2. Đổi database/role có kiểm soát trong cùng cluster sau khi đã backup và dừng toàn bộ client.

Không dùng `docker compose down -v`, `docker volume rm` hoặc xóa thư mục data để xử lý lỗi kết nối.

## 4. Migration dữ liệu đã thực hiện trong lần đổi tên

Quy trình đã dùng ngày 16/07/2026:

1. Copy read-only volume đúng `rental-management_postgres_data` sang một volume tạm.
2. Khởi động PostgreSQL 16 từ bản copy, không expose port.
3. Tạo custom-format archive bằng `pg_dump -Fc --no-owner --no-acl`.
4. Kiểm tra archive bằng `pg_restore --list`.
5. Khởi động stack mới với volume mới, database `rental_management_datn1_db` và user `rental_management_app`.
6. Restore bằng `pg_restore --clean --if-exists --no-owner --no-acl --exit-on-error`.
7. Xác nhận Flyway history vẫn là V1-V4 và backend báo schema version 4 đã up-to-date, không chạy lại migration.
8. Xóa duy nhất container/volume tạm do quy trình tạo; giữ nguyên hai volume nguồn.
9. Sau bước hardening, Flyway chạy migration mới `V5__secure_demo_accounts.sql` đúng một lần để khóa các tài khoản demo biết trước. V1-V4 không bị sửa hoặc chạy lại.
10. `V6__allow_cancelled_invoice_reissue.sql` thay duy nhất partial unique index của hóa đơn để hóa đơn `CANCELLED` có thể được lập lại; V1 không bị sửa.

### Tách role bootstrap và role ứng dụng

Official PostgreSQL image luôn tạo `POSTGRES_USER` với quyền cluster superuser. Vì vậy backend không dùng trực tiếp biến này:

- `POSTGRES_ADMIN_USER`/`POSTGRES_ADMIN_PASSWORD` chỉ phục vụ bootstrap và operations; không được truyền vào container backend.
- `DB_APP_USER`/`DB_APP_PASSWORD` là datasource/Flyway role, không có `SUPERUSER`, `CREATEDB`, `CREATEROLE`, `REPLICATION` hoặc `BYPASSRLS`.
- Script `docker/postgres/init/01-create-app-role.sh` tạo role ứng dụng khi volume còn trống và cấp quyền trên database/schema của project.
- Init từ chối app/admin trùng username hoặc password và xác minh lại privilege của app role. Healthcheck kiểm tra lại hai credential không trùng và đăng nhập bằng app role qua hostname mạng container để buộc password authentication; không dùng loopback `trust` hoặc chỉ gọi `pg_isready`.

Volume cũ trên máy audit từng khởi tạo `rental_management_app` làm bootstrap superuser nên PostgreSQL không cho hạ quyền trực tiếp role OID bootstrap. Quy trình đã thực hiện an toàn là tạo admin mới, đổi bootstrap role cũ thành `rental_management_legacy_bootstrap`, chuyển ownership 14 bảng cùng sequence sang một role ứng dụng mới, rồi đặt legacy bootstrap thành `NOLOGIN`. Kết quả đã kiểm tra: backend role `rolsuper=false`, toàn bộ bảng public thuộc `rental_management_app`, database thuộc `rental_management_admin`. Không có record nghiệp vụ nào bị xóa.

Vòng kiểm tra cuối còn phát hiện `.env` local của volume đã khởi tạo dùng trùng credential admin/app. Mật khẩu admin đã được xoay riêng đồng bộ trong PostgreSQL và file `.env` bị Git ignore, không in ra log hoặc tài liệu. Kiểm tra qua network xác nhận app credential bị từ chối đối với admin role, trong khi từng credential đúng vẫn đăng nhập được; healthcheck hiện cũng fail nếu hai biến password trùng nhau.

Archive được lưu ngoài Git repository tại:

```text
../rental-management-backups/rental_management_pre_rename_20260716-105119.dump
```

Thông tin kiểm tra:

```text
format: PostgreSQL custom archive
source PostgreSQL: 16.14
size: 54,830 bytes
SHA-256: BA3981DAA15500797FB112F41FA4D010F52918C7EF2B6522F03860301A5E2EAF
```

Archive có thể chứa dữ liệu cá nhân và password hash. Không commit, gửi qua kênh công khai hoặc lưu không mã hóa trên môi trường production.

## 5. Khởi động và kiểm tra

Tạo `.env` từ `.env.example`, thay các placeholder bằng secret cục bộ rồi chạy:

```powershell
docker compose config --quiet
docker compose up -d postgres
docker compose ps
```

Healthcheck Compose đã xác thực app role và database qua TCP. Có thể smoke query bổ sung:

```powershell
docker compose exec postgres psql `
  -h 127.0.0.1 `
  -U rental_management_app `
  -d rental_management_datn1_db `
  -c "SELECT current_database(), current_user;"
```

Trên volume mới, `flyway_schema_history` chỉ xuất hiện sau khi backend chạy. Mở terminal khác, từ thư mục `backend` chạy:

```powershell
Push-Location backend
.\mvnw.cmd spring-boot:run
Pop-Location
```

Sau khi backend startup thành công, kiểm tra Flyway:

```powershell
docker compose exec postgres psql `
  -U rental_management_app `
  -d rental_management_datn1_db `
  -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Backend đọc file `.env` ở repository root thông qua `spring.config.import`.

Expected Flyway log sau khi source hiện tại đã áp dụng V6:

```text
Successfully validated 6 migrations
Current version of schema "public": 6
Schema "public" is up to date. No migration necessary.
```

## 6. Backup thủ công an toàn

Tạo archive trong container rồi copy ra thư mục backup; cách này tránh shell làm biến đổi binary stream:

```powershell
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$file = "rental_management_$stamp.dump"
$container = docker compose ps -q postgres

docker exec $container pg_dump `
  -U rental_management_app `
  -d rental_management_datn1_db `
  -Fc --no-owner --no-acl `
  -f "/tmp/$file"

docker exec $container pg_restore --list "/tmp/$file"
docker cp "${container}:/tmp/$file" "..\rental-management-backups\$file"
docker exec $container rm -f "/tmp/$file"
Get-FileHash -Algorithm SHA256 "..\rental-management-backups\$file"
```

Production cần:

- Lịch backup tự động và cảnh báo khi thất bại.
- Mã hóa archive, bucket/host tách biệt và least-privilege access.
- Retention theo ngày/tuần/tháng và kiểm tra dung lượng.
- Restore drill định kỳ; backup chưa từng restore không được xem là đã kiểm chứng.
- WAL/PITR nếu RPO yêu cầu nhỏ hơn chu kỳ `pg_dump`.

## 7. Restore vào database tách biệt

Không chạy ví dụ restore với project mặc định hoặc database dev/live. Tạo một target dùng một lần, có project name, database, port và volume riêng; `--clean` chỉ được dùng sau khi xác nhận đây là target disposable và không có backend ghi vào.

```powershell
$project = "rental-management-datn1-restorecheck"
$database = "rental_management_restorecheck_db"
$envFile = ".env.restorecheck"
$archive = "C:\path\to\verified.dump"

Copy-Item .env.example $envFile
# Trong .env.restorecheck: đặt POSTGRES_DB=$database, POSTGRES_PORT=45434,
# hai password DB khác nhau, JWT secret và BOOTSTRAP_ADMIN_* dành riêng cho restore drill.

docker compose --env-file $envFile -p $project config --quiet
docker compose --env-file $envFile -p $project up -d postgres
$container = docker compose --env-file $envFile -p $project ps -q postgres

# Xác minh container thật sự thuộc project restorecheck trước khi tiếp tục.
docker inspect --format '{{ index .Config.Labels "com.docker.compose.project" }}' $container
docker cp $archive "${container}:/tmp/restore.dump"
docker exec $container pg_restore --list /tmp/restore.dump
docker exec $container pg_restore `
  -U rental_management_app `
  -d $database `
  --clean --if-exists --no-owner --no-acl --exit-on-error --single-transaction `
  /tmp/restore.dump
docker exec $container rm -f /tmp/restore.dump

# Chỉ sau restore mới khởi động backend để Flyway áp dụng migration còn thiếu và validate schema.
docker compose --env-file $envFile -p $project --profile app up -d backend
```

Không dùng quy trình trên để restore đè production. Cutover production cần dừng writer, tạo pre-restore backup, restore vào cluster/database mới trống, kiểm thử rồi mới chuyển traffic. Archive cũ không được `--clean` trực tiếp trên schema mới vì object không có trong archive có thể bị bỏ sót.

Sau restore phải kiểm tra:

- SHA-256 archive đúng với manifest.
- Số migration và checksum Flyway hợp lệ.
- Số bảng và record trọng yếu.
- Backend `ddl-auto: validate` khởi động thành công.
- Login/authorization và các API read/write chính.

Chỉ khi restore drill đã được nghiệm thu và xác nhận đúng project disposable mới cleanup bằng `docker compose --env-file .env.restorecheck -p rental-management-datn1-restorecheck down -v`. Không dùng lệnh này với project chính.

## 8. Rollback

Nếu stack mới lỗi:

1. Dừng backend để không có ghi mới.
2. Không xóa stack/volume mới bằng `-v`.
3. Giữ archive và ghi nhận mọi dữ liệu phát sinh sau thời điểm dump.
4. Có thể khởi động lại cluster nguồn bằng cấu hình cũ trên một port không xung đột, hoặc tạo dump mới từ stack mới để forward-fix.
5. Chỉ chuyển client về cluster nguồn sau khi xác nhận mốc dữ liệu và chấp nhận RPO.

Việc xóa các container/volume legacy là thao tác cleanup riêng, chỉ thực hiện sau nhiều vòng backup/restore verification và cần xác nhận rõ từ người sở hữu dữ liệu.
