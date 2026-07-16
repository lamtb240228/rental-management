package com.example.rental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class RefreshSessionIntegrationTests {
    private static final String COOKIE_NAME = "rental_refresh";
    private static final String INITIAL_PASSWORD = "InitialPassword123!";

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
    void loginSetsHttpOnlyCookieAndPersistsOnlyTheTokenHash() throws Exception {
        Credentials credentials = credentials("login-cookie");
        register(credentials);

        Session login = login(credentials);
        String setCookie = login.result().getResponse().getHeader(HttpHeaders.SET_COOKIE);

        assertThat(setCookie)
            .contains(COOKIE_NAME + "=")
            .contains("HttpOnly")
            .contains("Path=/api/auth")
            .contains("SameSite=Strict")
            .doesNotContain("Secure");
        assertThat(login.accessToken()).isNotBlank();
        assertThat(login.result().getResponse().getContentAsString()).doesNotContain(login.refreshToken());

        String persistedHash = jdbcTemplate.queryForObject(
            "select token_hash from refresh_sessions where token_hash = ?",
            String.class,
            sha256(login.refreshToken())
        );
        assertThat(persistedHash).isEqualTo(sha256(login.refreshToken())).hasSize(64);
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from refresh_sessions where token_hash = ?",
            Integer.class,
            login.refreshToken()
        )).isZero();
    }

    @Test
    void refreshRotatesOnceAndReuseRevokesTheWholeFamily() throws Exception {
        Session original = register(credentials("rotation"));
        String familyId = familyId(original.refreshToken());

        Session rotated = refresh(original.refreshToken(), 200);
        assertThat(rotated.refreshToken()).isNotEqualTo(original.refreshToken());
        assertThat(familyId(rotated.refreshToken())).isEqualTo(familyId);
        assertThat(jdbcTemplate.queryForObject(
            "select replaced_by_session_id from refresh_sessions where token_hash = ?",
            Long.class,
            sha256(original.refreshToken())
        )).isNotNull();
        assertThat(activeFamilySessions(familyId)).isEqualTo(1);

        MvcResult reuse = refreshResult(original.refreshToken());
        assertUnauthorizedAndCookieCleared(reuse);
        assertThat(activeFamilySessions(familyId)).isZero();

        assertUnauthorizedAndCookieCleared(refreshResult(rotated.refreshToken()));
    }

    @Test
    void forgedExpiredAndRevokedRefreshTokensAreRejected() throws Exception {
        assertUnauthorizedAndCookieCleared(refreshResult("forged-refresh-token-value"));

        Session expired = register(credentials("expired"));
        jdbcTemplate.update(
            """
                update refresh_sessions
                set created_at = current_timestamp - interval '2 days',
                    expires_at = current_timestamp - interval '1 day'
                where token_hash = ?
                """,
            sha256(expired.refreshToken())
        );
        assertUnauthorizedAndCookieCleared(refreshResult(expired.refreshToken()));

        Session revoked = register(credentials("revoked"));
        jdbcTemplate.update(
            "update refresh_sessions set revoked_at = current_timestamp where token_hash = ?",
            sha256(revoked.refreshToken())
        );
        assertUnauthorizedAndCookieCleared(refreshResult(revoked.refreshToken()));
    }

    @Test
    void logoutRevokesTheCurrentFamilyAndClearsTheCookie() throws Exception {
        Session session = register(credentials("logout"));
        String familyId = familyId(session.refreshToken());

        MvcResult logout = mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie(session.refreshToken())))
            .andExpect(status().isNoContent())
            .andReturn();

        assertCookieCleared(logout);
        assertThat(activeFamilySessions(familyId)).isZero();
        assertUnauthorizedAndCookieCleared(refreshResult(session.refreshToken()));
    }

    @Test
    void logoutAllRevokesEveryFamilyForTheAccount() throws Exception {
        Credentials credentials = credentials("logout-all");
        Session first = register(credentials);
        Session second = login(credentials);
        long userId = userId(credentials.email());

        assertThat(activeUserSessions(userId)).isEqualTo(2);
        MvcResult logout = mockMvc.perform(post("/api/auth/logout-all").cookie(refreshCookie(first.refreshToken())))
            .andExpect(status().isNoContent())
            .andReturn();

        assertCookieCleared(logout);
        assertThat(activeUserSessions(userId)).isZero();
        assertUnauthorizedAndCookieCleared(refreshResult(second.refreshToken()));
    }

    @Test
    void lockedAccountCannotRefreshAndItsSessionsAreRevoked() throws Exception {
        Credentials credentials = credentials("locked");
        Session session = register(credentials);
        long userId = userId(credentials.email());
        jdbcTemplate.update("update user_accounts set status = 'LOCKED' where id = ?", userId);

        assertUnauthorizedAndCookieCleared(refreshResult(session.refreshToken()));
        assertThat(activeUserSessions(userId)).isZero();
    }

    @Test
    void changingPasswordRevokesAllSessionsAndChangesTheCredential() throws Exception {
        Credentials credentials = credentials("password-change");
        Session first = register(credentials);
        login(credentials);
        long userId = userId(credentials.email());
        String newPassword = "ChangedPassword456!";

        MvcResult changed = mockMvc.perform(post("/api/auth/change-password")
                .header(HttpHeaders.AUTHORIZATION, bearer(first.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new PasswordChange(INITIAL_PASSWORD, newPassword))))
            .andExpect(status().isNoContent())
            .andReturn();

        assertCookieCleared(changed);
        assertThat(activeUserSessions(userId)).isZero();
        loginExpecting(credentials, 401);
        login(new Credentials(credentials.email(), newPassword));
    }

    @Test
    void concurrentRefreshCannotCreateTwoUsableSuccessors() throws Exception {
        Session original = register(credentials("concurrent"));
        String familyId = familyId(original.refreshToken());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<MvcResult> first = executor.submit(() -> concurrentRefresh(original.refreshToken(), start));
            Future<MvcResult> second = executor.submit(() -> concurrentRefresh(original.refreshToken(), start));
            start.countDown();

            List<MvcResult> results = List.of(
                first.get(20, TimeUnit.SECONDS),
                second.get(20, TimeUnit.SECONDS)
            );
            long successes = results.stream().filter(result -> result.getResponse().getStatus() == 200).count();
            long rejected = results.stream().filter(result -> result.getResponse().getStatus() == 401).count();

            assertThat(successes).isLessThanOrEqualTo(1);
            assertThat(successes + rejected).isEqualTo(2);
            assertThat(activeFamilySessions(familyId)).isLessThanOrEqualTo(1);
            if (successes == 1 && rejected == 1) {
                assertThat(activeFamilySessions(familyId)).isZero();
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private MvcResult concurrentRefresh(String rawToken, CountDownLatch start) throws Exception {
        assertTrue(start.await(10, TimeUnit.SECONDS));
        return refreshResult(rawToken);
    }

    private Session register(Credentials credentials) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new Registration(
                    credentials.email(),
                    credentials.password(),
                    "Refresh Session Test",
                    null
                ))))
            .andExpect(status().isOk())
            .andReturn();
        return session(result);
    }

    private Session login(Credentials credentials) throws Exception {
        MvcResult result = loginExpecting(credentials, 200);
        return session(result);
    }

    private MvcResult loginExpecting(Credentials credentials, int expectedStatus) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(credentials)))
            .andExpect(status().is(expectedStatus))
            .andReturn();
    }

    private Session refresh(String rawToken, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie(rawToken)))
            .andExpect(status().is(expectedStatus))
            .andReturn();
        return session(result);
    }

    private MvcResult refreshResult(String rawToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie(rawToken))).andReturn();
    }

    private Session session(MvcResult result) throws Exception {
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return new Session(
            data.path("accessToken").asText(),
            refreshToken(result),
            result
        );
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

    private void assertUnauthorizedAndCookieCleared(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(error.path("status").asInt()).isEqualTo(401);
        assertThat(error.path("message").asText()).isEqualTo("Refresh session is invalid or expired");
        assertCookieCleared(result);
    }

    private void assertCookieCleared(MvcResult result) {
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
            .contains(COOKIE_NAME + "=")
            .contains("Max-Age=0")
            .contains("HttpOnly")
            .contains("Path=/api/auth")
            .contains("SameSite=Strict");
    }

    private long userId(String email) {
        Long id = jdbcTemplate.queryForObject(
            "select id from user_accounts where email = ? and deleted_at is null",
            Long.class,
            email
        );
        assertThat(id).isNotNull();
        return id;
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

    private record Session(String accessToken, String refreshToken, MvcResult result) {
    }
}
