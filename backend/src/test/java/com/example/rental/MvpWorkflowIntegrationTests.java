package com.example.rental;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.rental.user.entity.UserAccount;
import com.example.rental.user.entity.UserStatus;
import com.example.rental.user.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class MvpWorkflowIntegrationTests {
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
    private UserAccountRepository userAccountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void landlordCanAccessEveryCoreMvpModule() throws Exception {
        String token = login("demo@rental.local", "Password123!");

        JsonNode contracts = getData("/api/contracts", token);
        JsonNode invoices = getData("/api/invoices", token);
        JsonNode tenants = getData("/api/tenants", token);
        long roomId = contracts.get(0).get("roomId").asLong();
        long invoiceId = invoices.get(0).get("id").asLong();
        long tenantId = tenants.get(0).get("id").asLong();

        authorizedGet("/api/properties", token);
        authorizedGet("/api/tenants/" + tenantId + "/contracts", token);
        authorizedGet("/api/rooms/" + roomId + "/utility-readings", token);
        authorizedGet("/api/invoices/" + invoiceId + "/payments", token);
        authorizedGet("/api/maintenance-requests", token);
        authorizedGet("/api/dashboard/summary", token);
    }

    @Test
    void tenantPortalIsAvailableAndLandlordDataIsForbidden() throws Exception {
        String token = login("tenant@rental.local", "Password123!");

        mockMvc.perform(get("/api/tenant-portal/summary").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.tenant.fullName").value("Demo Tenant"))
            .andExpect(jsonPath("$.data.activeContract.contractCode").value("CT-DEMO-001"));

        mockMvc.perform(get("/api/properties").header("Authorization", bearer(token)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.error").value("Forbidden"))
            .andExpect(jsonPath("$.path").value("/api/properties"));
    }

    @Test
    void tenantPortalDoesNotExposeUtilityReadingsAfterTheTenancyPeriod() throws Exception {
        jdbcTemplate.update("""
            insert into utility_readings(
                room_id, billing_year, billing_month,
                electricity_old_reading, electricity_new_reading, electricity_unit_price,
                water_old_reading, water_new_reading, water_unit_price
            )
            select room.id, 2035, 12, 500, 510, 3500, 40, 41, 20000
            from rooms room
            join properties property on property.id = room.property_id
            where property.name = 'Demo Property'
              and room.room_number = '101'
              and not exists (
                  select 1 from utility_readings existing
                  where existing.room_id = room.id
                    and existing.billing_year = 2035
                    and existing.billing_month = 12
                    and existing.deleted_at is null
              )
            """);

        JsonNode summary = getData("/api/tenant-portal/summary", login("tenant@rental.local", "Password123!"));
        boolean leaked = false;
        for (JsonNode reading : summary.get("utilityReadings")) {
            leaked |= reading.get("billingYear").asInt() == 2035 && reading.get("billingMonth").asInt() == 12;
        }
        assertFalse(leaked, "A tenant must not see readings outside their occupancy period");
    }

    @Test
    void adminCanLockAndUnlockTenantAccount() throws Exception {
        String adminToken = login("admin@rental.local", "Password123!");
        String tenantToken = login("tenant@rental.local", "Password123!");
        JsonNode users = getData("/api/admin/users", adminToken);
        long tenantAccountId = findUserId(users, "tenant@rental.local");

        updateStatus(adminToken, tenantAccountId, "LOCKED");
        mockMvc.perform(get("/api/tenant-portal/summary").header("Authorization", bearer(tenantToken)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"))
            .andExpect(jsonPath("$.path").value("/api/tenant-portal/summary"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("tenant@rental.local", "Password123!")))
            .andExpect(status().isUnauthorized());

        updateStatus(adminToken, tenantAccountId, "ACTIVE");
        assertTokenRejected(tenantToken);
        login("tenant@rental.local", "Password123!");
    }

    @Test
    void inactiveAndDeletedAccountsCannotReuseExistingTokens() throws Exception {
        String inactiveEmail = "inactive-token-test@rental.local";
        String inactiveToken = register(inactiveEmail);
        UserAccount inactiveUser = findUser(inactiveEmail);
        inactiveUser.setStatus(UserStatus.INACTIVE);
        userAccountRepository.saveAndFlush(inactiveUser);

        assertTokenRejected(inactiveToken);

        String deletedEmail = "deleted-token-test@rental.local";
        String deletedToken = register(deletedEmail);
        UserAccount deletedUser = findUser(deletedEmail);
        deletedUser.softDelete();
        userAccountRepository.saveAndFlush(deletedUser);

        assertTokenRejected(deletedToken);
    }

    @Test
    void registrationNormalizesEmailAndRejectsNormalizedDuplicate() throws Exception {
        String token = register("  Normalized.Email@Rental.Local  ");
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value("normalized.email@rental.local"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("normalized.email@rental.local")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void actuatorMetricsRequiresAdminWhileHealthRemainsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());

        String tenantToken = login("tenant@rental.local", "Password123!");
        mockMvc.perform(get("/actuator/metrics").header("Authorization", bearer(tenantToken)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.path").value("/actuator/metrics"));

        String adminToken = login("admin@rental.local", "Password123!");
        mockMvc.perform(get("/actuator/metrics").header("Authorization", bearer(adminToken)))
            .andExpect(status().isOk());
    }

    private void updateStatus(String token, long userId, String statusValue) throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/status", userId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + statusValue + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value(statusValue));
    }

    private void authorizedGet(String path, String token) throws Exception {
        mockMvc.perform(get(path).header("Authorization", bearer(token)))
            .andExpect(status().isOk());
    }

    private JsonNode getData(String path, String token) throws Exception {
        String content = mockMvc.perform(get(path).header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(content).get("data");
    }

    private String login(String email, String password) throws Exception {
        String content = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email, password)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(content).get("data").get("accessToken").asText();
    }

    private String register(String email) throws Exception {
        String content = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(content).get("data").get("accessToken").asText();
    }

    private String registerBody(String email) throws Exception {
        return objectMapper.writeValueAsString(new RegisterPayload(
            email,
            "UniquePassword123!",
            "Security Test Account",
            null
        ));
    }

    private UserAccount findUser(String email) {
        return userAccountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
            .orElseThrow(() -> new IllegalStateException("Registered user was not found: " + email));
    }

    private void assertTokenRejected(String token) throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.path").value("/api/auth/me"));
    }

    private String loginBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginPayload(email, password));
    }

    private long findUserId(JsonNode users, String email) {
        for (JsonNode user : users) {
            if (email.equals(user.get("email").asText())) {
                return user.get("id").asLong();
            }
        }
        throw new IllegalStateException("Seeded user was not found: " + email);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record LoginPayload(String email, String password) {
    }

    private record RegisterPayload(String email, String password, String fullName, String phone) {
    }
}
