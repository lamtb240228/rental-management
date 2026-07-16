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

JWT access token vẫn được kiểm tra cùng trạng thái account hiện tại trên mỗi request. Account `LOCKED`, `INACTIVE` hoặc soft-deleted không được xác thực hoặc refresh.

## Luồng API

### Login và register

1. Backend xác thực hoặc tạo tài khoản.
2. Backend tạo một refresh session mới, chỉ lưu token hash.
3. Response JSON giữ `accessToken`, `tokenType` và profile để tương thích API hiện tại.
4. Raw refresh token được đặt vào cookie HttpOnly; auth response dùng `Cache-Control: no-store`.

### Refresh

`POST /api/auth/refresh` đọc cookie, khóa pessimistic row của token hiện tại và thực hiện rotation trong transaction:

1. Token phải tồn tại, chưa hết hạn, chưa revoke và thuộc account đang active.
2. Backend tạo successor với token hash mới.
3. Token cũ được đánh dấu `last_used_at`, `revoked_at` và `replaced_by_session_id`.
4. Response trả access token mới và thay cookie bằng raw successor.

Frontend chỉ retry một request thất bại tối đa một lần. Các 401 đồng thời trong cùng tab dùng chung một refresh promise. Khi Web Locks khả dụng, các tab dùng chung một mutex để tránh đồng thời rotate cookie dùng chung.

### Reuse detection

Nếu một refresh token đã rotate được sử dụng lại, backend xem đây là khả năng token bị sao chép. Toàn bộ family bị revoke và request nhận JSON 401 chung. Chính sách fail-closed này cũng áp dụng cho concurrent loser ở trình duyệt không hỗ trợ mutex xuyên tab: không bao giờ có hai successor hợp lệ cùng lúc, nhưng người dùng có thể phải đăng nhập lại.

### Logout

- `POST /api/auth/logout` revoke logical session family được cookie hiện tại nhận diện và xóa cookie.
- `POST /api/auth/logout-all` revoke mọi refresh session của account được cookie hiện tại nhận diện và xóa cookie.
- Đổi mật khẩu hoặc chuyển account khỏi trạng thái `ACTIVE` revoke toàn bộ refresh session.
- Thao tác logout phía frontend xóa access token memory và cache account ngay lập tức, kể cả khi request backend thất bại.

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

## CORS, lỗi và dữ liệu nhạy cảm

- Credentialed CORS chỉ cho phép origin được cấu hình rõ ràng; wildcard bị từ chối.
- 401 và 403 tiếp tục dùng cấu trúc JSON `ErrorResponse` hiện tại.
- Login không dùng thông báo khác nhau để tiết lộ account có tồn tại hoặc đang bị khóa.
- Token không được ghi vào URL, log, exception hoặc `toString()` của DTO.
- Cross-tab chỉ truyền event login/logout và nonce; không truyền token hoặc profile.
- Khi nâng cấp, frontend xóa khóa token legacy khỏi cả localStorage và sessionStorage.

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
