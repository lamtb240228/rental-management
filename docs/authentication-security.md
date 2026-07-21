# Authentication and session security

Tài liệu này mô tả mô hình phiên đăng nhập của Rental Management sau P0 Authentication Session Hardening. Phạm vi chỉ bao gồm xác thực tài khoản của hệ thống quản lý dãy phòng trọ; đây không phải một OAuth authorization server.

## Mô hình token

- Access token là JWT ký HMAC, mặc định hết hạn sau 15 phút.
- Access token chỉ tồn tại trong memory của frontend và được gửi bằng `Authorization: Bearer ...`.
- Frontend không lưu access token hoặc refresh token trong `localStorage` hay `sessionStorage`.
- Refresh token là giá trị opaque sinh từ `SecureRandom` với ít nhất 256 bit ngẫu nhiên.
- Raw refresh token chỉ được gửi trong cookie HttpOnly. Database chỉ lưu SHA-256 hash của token.
- Mỗi lần login/register tạo một session family mới, mặc định hết hạn tuyệt đối sau 7 ngày.
- Mỗi lần refresh tạo một token kế nhiệm trong cùng family, revoke token trước đó và giữ nguyên thời điểm hết hạn tuyệt đối của family.
- V8 thêm `user_accounts.auth_version BIGINT NOT NULL DEFAULT 0` với check không âm. Access JWT bắt buộc có claim `authVersion` bằng version hiện tại của account; token thiếu claim hoặc mang version cũ bị từ chối.

JWT access token vẫn được kiểm tra cùng trạng thái và `auth_version` hiện tại trên mỗi request. Đổi mật khẩu, logout-all hợp lệ, revoke-all bảo mật, chuyển account sang `LOCKED`/`INACTIVE` hoặc soft-delete đều tăng version và revoke toàn bộ refresh session. Vì vậy access token đã cấp bị từ chối ngay ở request kế tiếp; unlock không giảm version và không làm token cũ sống lại. Chỉnh sửa hồ sơ thông thường không tăng version. Account `LOCKED`, `INACTIVE` hoặc soft-deleted không được xác thực hoặc refresh.

## Transaction và thứ tự khóa

Các thao tác authentication dùng cùng thứ tự để tránh deadlock và đóng race TOCTOU:

1. Hash refresh token nếu có và chỉ lookup owner ID; login lookup target user ID.
2. Khóa pessimistic-write các row `user_accounts` theo ID tăng dần.
3. Sau khi giữ user lock, đọc lại account và refresh-session, rồi kiểm tra email, password, trạng thái, soft-delete, expiration, revocation và replacement.
4. Sau cùng mới insert, rotate hoặc revoke refresh-session trong cùng transaction.

Không có đường code authentication nào khóa refresh-session trước rồi mới khóa user. Login kiểm tra lại password trên entity đã khóa; change-password và admin status change giữ cùng user lock, nên refresh/login không thể vượt qua một thay đổi credential đã commit.

## Luồng API

### Login và register

1. Backend xác thực hoặc tạo tài khoản.
2. Backend tạo một refresh session mới, chỉ lưu token hash.
3. Response JSON giữ `accessToken`, `tokenType` và profile để tương thích API hiện tại.
4. Raw refresh token được đặt vào cookie HttpOnly; auth response dùng `Cache-Control: no-store`.

### Refresh

`POST /api/auth/refresh` đọc cookie, khóa pessimistic user trước rồi đọc lại và khóa token hiện tại để thực hiện rotation trong transaction:

1. Token phải tồn tại, chưa hết hạn, chưa revoke và thuộc account đang active.
2. Backend tạo successor với token hash mới.
3. Token cũ được đánh dấu `last_used_at`, `revoked_at` và `replaced_by_session_id`.
4. Response trả access token mới và thay cookie bằng raw successor.

Frontend chỉ retry một request thất bại tối đa một lần. Các 401 đồng thời trong cùng tab dùng chung một refresh promise. Khi Web Locks khả dụng, các tab dùng chung một mutex để tránh đồng thời rotate cookie dùng chung.

### Reuse detection

Nếu một refresh token đã rotate được sử dụng lại, backend xem đây là khả năng token bị sao chép. Toàn bộ family bị revoke và request nhận JSON 401 chung. Chính sách fail-closed này cũng áp dụng cho concurrent loser ở trình duyệt không hỗ trợ mutex xuyên tab: không bao giờ có hai successor hợp lệ cùng lúc, nhưng người dùng có thể phải đăng nhập lại.

### Logout

- `POST /api/auth/logout` revoke logical session family được cookie hiện tại nhận diện và xóa cookie.
- `POST /api/auth/logout-all` chỉ revoke mọi refresh session và tăng `auth_version` khi cookie nhận diện một token active, chưa hết hạn và chưa bị thay thế. Token forged/expired/revoked nhận 401 và xóa cookie nhưng không thay đổi family khác; predecessor đã rotate chỉ làm family lịch sử của chính nó bị revoke để xử lý race an toàn.
- Đổi mật khẩu hoặc chuyển account khỏi trạng thái `ACTIVE` revoke toàn bộ refresh session.
- Thao tác logout phía frontend xóa access token memory và cache account ngay lập tức. Nếu request lỗi mạng, frontend lưu tombstone `logout-pending` cùng session-generation ngẫu nhiên không được backend chấp nhận như credential, chặn bootstrap refresh và retry logout khi khởi động hoặc khi trình duyệt online. Tombstone chỉ được xóa có điều kiện cho đúng generation sau response logout thành công hoặc một login mới do người dùng chủ động thực hiện; tín hiệu trễ của generation cũ bị bỏ qua.
- Login/register/refresh/logout dùng chung Web Lock khi trình duyệt hỗ trợ. Login/register chờ request logout local đang chạy kết thúc (logout có timeout 10 giây) trước khi gửi request tạo cookie mới, tránh response logout cũ xóa refresh cookie vừa cấp. Không có Web Locks, backend vẫn fail-closed cho refresh concurrent bằng user/session row lock; race cross-tab có thể kết thúc phiên nhưng không tạo hai successor hợp lệ. E2E chuyên biệt cho fallback này vẫn là backlog vận hành.

Logout một thiết bị chỉ revoke refresh family hiện tại; access token đã phát hành cho family đó không gắn session ID nên có thể còn dùng được đến tối đa 15 phút. Đây là residual risk đã chấp nhận ở P0 để giữ access JWT stateless. Các thao tác toàn cục nhạy cảm nêu trên dùng `auth_version` để vô hiệu access token ngay; nếu sau này cần single-device invalidation tức thời phải thêm session-bound claim/check, không tái sử dụng `auth_version` toàn account.

## Schema `refresh_sessions`

| Cột | Mục đích |
| --- | --- |
| `id` | Khóa chính |
| `user_account_id` | Account sở hữu session |
| `token_hash` | SHA-256 hash duy nhất; không lưu raw token |
| `family_id` | Nhận diện logical login/device session |
| `created_at` | Thời điểm token được tạo |
| `expires_at` | Absolute expiry của family |
| `last_used_at` | Lần token được dùng để rotate |
| `revoked_at` | Thời điểm token bị vô hiệu hóa |
| `replaced_by_session_id` | Successor sau rotation |

Index phục vụ lookup token hash, revoke theo user/family và cleanup theo expiration. Record đã rotate được giữ lại ít nhất đến khi family hết hạn để phát hiện reuse.

## Cookie policy

Cookie không chứa ID, email, role hoặc dữ liệu người dùng.

| Thuộc tính | Development localhost | Production |
| --- | --- | --- |
| `HttpOnly` | Luôn bật | Luôn bật |
| `Secure` | Tắt để hỗ trợ HTTP localhost | Bắt buộc bật |
| `SameSite` | `Strict` | `Strict` mặc định |
| `Path` | `/api/auth` | `/api/auth` |
| `Domain` | Không đặt, host-only | Không đặt, host-only |
| `Max-Age` | Thời gian còn lại đến absolute expiry | Thời gian còn lại đến absolute expiry |

Nếu tương lai cần frontend/API thật sự cross-site và `SameSite=None`, phải đồng thời bật `Secure` và thiết kế CSRF protection riêng; không chỉ dựa vào CORS.

## Bảo vệ Origin/Referer cho endpoint dùng cookie

Ba endpoint thay đổi trạng thái dựa trên refresh cookie là `POST /api/auth/refresh`, `POST /api/auth/logout` và `POST /api/auth/logout-all`. Backend kiểm tra nguồn request trong security filter trước khi request đến controller hoặc service:

1. Nếu có header `Origin`, giá trị phải là một HTTP(S) origin khớp chính xác với `app.cors.allowed-origins` sau khi chuẩn hóa scheme, host và default port.
2. Nếu không có `Origin`, backend trích origin từ `Referer` và áp dụng cùng allowlist.
3. Wildcard, opaque origin `null`, giá trị `Origin` có user-info/path/query, sibling subdomain không nằm trong allowlist, nhiều header nguồn, hoặc request thiếu cả `Origin` và `Referer` đều nhận JSON 403 với `Cache-Control: no-store`.
4. Request bị từ chối dừng ngay trong filter, vì vậy không rotate token và không revoke session/family.

Filter so khớp decoded application path theo cùng biểu diễn với Spring MVC, không so raw `requestURI`; percent-encoded unreserved character như `%72efresh` vì vậy không thể né kiểm tra nguồn. Spring Security firewall tiếp tục chặn các path encoding nguy hiểm khác trước controller.

Chính sách no-Origin là **fail-closed**. Client không phải trình duyệt (CLI, mobile native, integration nội bộ) phải chủ động gửi một header `Origin` nằm trong allowlist; backend không tự suy đoán hoặc miễn kiểm tra theo `User-Agent`. Allowlist này cũng là nguồn duy nhất cho credentialed CORS, không có wildcard hay cơ chế suffix/subdomain matching.

Login/register không dùng refresh cookie làm credential cho hành động hiện tại; change-password dùng access JWT. Chúng tiếp tục tuân theo CORS và authorization tương ứng, nhưng không nằm trong filter ba endpoint cookie-auth ở trên.

## CORS, lỗi và dữ liệu nhạy cảm

- Credentialed CORS và filter chống cross-site dùng chung danh sách origin được cấu hình rõ ràng; wildcard bị từ chối.
- 401 và 403 tiếp tục dùng cấu trúc JSON `ErrorResponse` hiện tại.
- Login không dùng thông báo khác nhau để tiết lộ account có tồn tại hoặc đang bị khóa.
- Token không được ghi vào URL, log, exception hoặc `toString()` của DTO.
- HTTP layer frontend chuyển raw `AxiosError` thành `ApplicationError` chỉ gồm `status`, `code`, `message` và `requestId`; request config, body, header `Authorization`, password và token không được chuyển ra UI/logging/monitoring.
- Cross-tab chỉ truyền event login/logout và session-generation correlation marker; marker không phải token, không được backend xác thực và không truyền profile.
- Khi nâng cấp, frontend chỉ xóa khóa token legacy khỏi cả localStorage và sessionStorage. Tombstone logout và event bus là metadata không nhạy cảm, không phải credential.

## Nguồn tham khảo chính thức

- [Spring Security — Stateless session management](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html)
- [Spring Security — CORS](https://docs.spring.io/spring-security/reference/6.5/servlet/integrations/cors.html)
- [Spring Framework — Credentialed CORS](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc-cors.html)
- [Spring Framework — ResponseCookie builder](https://docs.spring.io/spring-framework/docs/6.2.17/javadoc-api/org/springframework/http/ResponseCookie.ResponseCookieBuilder.html)
- [Spring Data JPA — Locking](https://docs.spring.io/spring-data/jpa/reference/3.5/jpa/locking.html)
- [OWASP Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [OWASP CSRF Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html)
- [RFC 9700 — OAuth 2.0 Security Best Current Practice](https://www.rfc-editor.org/rfc/rfc9700.html)
