package com.example.rental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthenticationConcurrencyIntegrationTests {
    private static final String COOKIE_NAME = "rental_refresh";
    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String INITIAL_PASSWORD = "ConcurrentInitial123!";
    private static final String CHANGED_PASSWORD = "ConcurrentChanged456!";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void ancestorReuseRacingCurrentRefreshLeavesNoUsableSuccessor() throws Exception {
        Credentials credentials = credentials("ancestor-race");
        Session ancestor = register(credentials);
        Session current = refresh(ancestor.refreshToken());
        String familyId = familyId(ancestor.refreshToken());

        List<MvcResult> results = race(
            () -> refreshResult(ancestor.refreshToken()),
            () -> refreshResult(current.refreshToken())
        );

        assertThat(results).allSatisfy(result ->
            assertThat(result.getResponse().getStatus()).isIn(200, 401)
        );
        assertThat(activeFamilySessions(familyId)).isZero();
        for (MvcResult result : results) {
            if (result.getResponse().getStatus() == 200) {
                assertThat(refreshResult(session(result).refreshToken()).getResponse().getStatus()).isEqualTo(401);
            }
        }
    }

    @Test
    void refreshRacingLogoutAllNeverLeavesItsSuccessorActive() throws Exception {
        Credentials credentials = credentials("logout-all-race");
        Session rotating = register(credentials);
        Session independent = login(credentials);
        String rotatingFamilyId = familyId(rotating.refreshToken());
        long userId = userId(credentials.email());

        List<MvcResult> results = race(
            () -> refreshResult(rotating.refreshToken()),
            () -> logoutAllResult(rotating.refreshToken())
        );

        MvcResult refresh = results.get(0);
        MvcResult logoutAll = results.get(1);
        assertThat(refresh.getResponse().getStatus()).isIn(200, 401);
        assertThat(logoutAll.getResponse().getStatus()).isIn(204, 401);
        assertThat(activeFamilySessions(rotatingFamilyId)).isZero();

        if (logoutAll.getResponse().getStatus() == 204) {
            assertThat(activeUserSessions(userId)).isZero();
            assertThat(refreshResult(independent.refreshToken()).getResponse().getStatus()).isEqualTo(401);
        } else {
            assertThat(refreshResult(independent.refreshToken()).getResponse().getStatus()).isEqualTo(200);
        }
    }

    @Test
    void refreshRacingPasswordChangeLeavesNoSessionAndInvalidatesOldAccessImmediately() throws Exception {
        Credentials credentials = credentials("password-race");
        Session session = register(credentials);
        long userId = userId(credentials.email());

        List<MvcResult> results = race(
            () -> refreshResult(session.refreshToken()),
            () -> changePasswordResult(session.accessToken(), INITIAL_PASSWORD, CHANGED_PASSWORD)
        );

        assertThat(results.get(0).getResponse().getStatus()).isIn(200, 401);
        assertThat(results.get(1).getResponse().getStatus()).isEqualTo(204);
        assertThat(activeUserSessions(userId)).isZero();
        assertThat(meStatus(session.accessToken())).isEqualTo(401);
        assertThat(loginResult(credentials).getResponse().getStatus()).isEqualTo(401);
        assertThat(loginResult(new Credentials(credentials.email(), CHANGED_PASSWORD)).getResponse().getStatus())
            .isEqualTo(200);
    }

    @Test
    void oldPasswordLoginRacingPasswordChangeCannotLeaveUsableCredentials() throws Exception {
        Credentials credentials = credentials("login-password-race");
        Session existing = register(credentials);

        List<MvcResult> results = race(
            () -> loginResult(credentials),
            () -> changePasswordResult(existing.accessToken(), INITIAL_PASSWORD, CHANGED_PASSWORD)
        );

        MvcResult racedLogin = results.get(0);
        assertThat(racedLogin.getResponse().getStatus()).isIn(200, 401);
        assertThat(results.get(1).getResponse().getStatus()).isEqualTo(204);
        if (racedLogin.getResponse().getStatus() == 200) {
            Session issued = session(racedLogin);
            assertThat(meStatus(issued.accessToken())).isEqualTo(401);
            assertThat(refreshResult(issued.refreshToken()).getResponse().getStatus()).isEqualTo(401);
        }
        assertThat(loginResult(credentials).getResponse().getStatus()).isEqualTo(401);
        assertThat(loginResult(new Credentials(credentials.email(), CHANGED_PASSWORD)).getResponse().getStatus())
            .isEqualTo(200);
    }

    @Test
    void loginRacingAccountLockCannotIssueCredentialsThatReviveAfterUnlock() throws Exception {
        Credentials targetCredentials = credentials("account-lock-race");
        Session targetExisting = register(targetCredentials);
        Session administrator = login(new Credentials("admin@rental.local", "Password123!"));
        long targetUserId = userId(targetCredentials.email());
        long versionBefore = authVersion(targetUserId);

        List<MvcResult> results = race(
            () -> loginResult(targetCredentials),
            () -> updateStatusResult(administrator.accessToken(), targetUserId, "LOCKED")
        );

        MvcResult racedLogin = results.get(0);
        assertThat(racedLogin.getResponse().getStatus()).isIn(200, 401);
        assertThat(results.get(1).getResponse().getStatus()).isEqualTo(200);
        assertThat(authVersion(targetUserId)).isGreaterThan(versionBefore);
        assertThat(activeUserSessions(targetUserId)).isZero();
        assertThat(meStatus(targetExisting.accessToken())).isEqualTo(401);
        assertThat(refreshResult(targetExisting.refreshToken()).getResponse().getStatus()).isEqualTo(401);

        Session racedSession = racedLogin.getResponse().getStatus() == 200 ? session(racedLogin) : null;
        if (racedSession != null) {
            assertThat(meStatus(racedSession.accessToken())).isEqualTo(401);
            assertThat(refreshResult(racedSession.refreshToken()).getResponse().getStatus()).isEqualTo(401);
        }

        assertThat(updateStatusResult(administrator.accessToken(), targetUserId, "ACTIVE").getResponse().getStatus())
            .isEqualTo(200);
        assertThat(meStatus(targetExisting.accessToken())).isEqualTo(401);
        if (racedSession != null) {
            assertThat(meStatus(racedSession.accessToken())).isEqualTo(401);
        }
        assertThat(loginResult(targetCredentials).getResponse().getStatus()).isEqualTo(200);
    }

    private List<MvcResult> race(Callable<MvcResult> first, Callable<MvcResult> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<MvcResult> firstResult = executor.submit(() -> runAtBarrier(first, ready, start));
            Future<MvcResult> secondResult = executor.submit(() -> runAtBarrier(second, ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Both operations must reach the start barrier");
            start.countDown();
            return List.of(
                firstResult.get(20, TimeUnit.SECONDS),
                secondResult.get(20, TimeUnit.SECONDS)
            );
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private MvcResult runAtBarrier(
        Callable<MvcResult> operation,
        CountDownLatch ready,
        CountDownLatch start
    ) throws Exception {
        ready.countDown();
        assertTrue(start.await(10, TimeUnit.SECONDS), "Start barrier was not released");
        return operation.call();
    }

    private Session register(Credentials credentials) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new Registration(
                    credentials.email(),
                    credentials.password(),
                    "Concurrency Test",
                    null
                ))))
            .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return session(result);
    }

    private Session login(Credentials credentials) throws Exception {
        MvcResult result = loginResult(credentials);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return session(result);
    }

    private MvcResult loginResult(Credentials credentials) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(credentials)))
            .andReturn();
    }

    private Session refresh(String rawToken) throws Exception {
        MvcResult result = refreshResult(rawToken);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return session(result);
    }

    private MvcResult refreshResult(String rawToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .cookie(refreshCookie(rawToken)))
            .andReturn();
    }

    private MvcResult logoutAllResult(String rawToken) throws Exception {
        return mockMvc.perform(post("/api/auth/logout-all")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .cookie(refreshCookie(rawToken)))
            .andReturn();
    }

    private MvcResult changePasswordResult(
        String accessToken,
        String currentPassword,
        String newPassword
    ) throws Exception {
        return mockMvc.perform(post("/api/auth/change-password")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new PasswordChange(currentPassword, newPassword))))
            .andReturn();
    }

    private MvcResult updateStatusResult(String adminAccessToken, long userId, String status) throws Exception {
        return mockMvc.perform(patch("/api/admin/users/{id}/status", userId)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminAccessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new StatusUpdate(status))))
            .andReturn();
    }

    private int meStatus(String accessToken) throws Exception {
        return mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
            .andReturn()
            .getResponse()
            .getStatus();
    }

    private Session session(MvcResult result) throws Exception {
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return new Session(data.path("accessToken").asText(), refreshToken(result));
    }

    private String refreshToken(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotBlank();
        String prefix = COOKIE_NAME + "=";
        assertThat(setCookie).startsWith(prefix);
        int valueEnd = setCookie.indexOf(';', prefix.length());
        return setCookie.substring(prefix.length(), valueEnd < 0 ? setCookie.length() : valueEnd);
    }

    private Cookie refreshCookie(String rawToken) {
        return new Cookie(COOKIE_NAME, rawToken);
    }

    private long userId(String email) {
        Long id = jdbcTemplate.queryForObject(
            "select id from user_accounts where email = ?",
            Long.class,
            email
        );
        assertThat(id).isNotNull();
        return id;
    }

    private long authVersion(long userId) {
        Long version = jdbcTemplate.queryForObject(
            "select auth_version from user_accounts where id = ?",
            Long.class,
            userId
        );
        assertThat(version).isNotNull();
        return version;
    }

    private String familyId(String rawToken) {
        return jdbcTemplate.queryForObject(
            "select family_id::text from refresh_sessions where token_hash = ?",
            String.class,
            sha256(rawToken)
        );
    }

    private int activeFamilySessions(String familyId) {
        Integer count = jdbcTemplate.queryForObject(
            """
                select count(*)
                from refresh_sessions
                where family_id = cast(? as uuid)
                  and revoked_at is null
                  and expires_at > current_timestamp
                """,
            Integer.class,
            familyId
        );
        return count == null ? 0 : count;
    }

    private int activeUserSessions(long userId) {
        Integer count = jdbcTemplate.queryForObject(
            """
                select count(*)
                from refresh_sessions
                where user_account_id = ?
                  and revoked_at is null
                  and expires_at > current_timestamp
                """,
            Integer.class,
            userId
        );
        return count == null ? 0 : count;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Credentials credentials(String prefix) {
        return new Credentials(prefix + "-" + UUID.randomUUID() + "@example.test", INITIAL_PASSWORD);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Credentials(String email, String password) {
    }

    private record Registration(String email, String password, String fullName, String phone) {
    }

    private record PasswordChange(String currentPassword, String newPassword) {
    }

    private record StatusUpdate(String status) {
    }

    private record Session(String accessToken, String refreshToken) {
    }
}
