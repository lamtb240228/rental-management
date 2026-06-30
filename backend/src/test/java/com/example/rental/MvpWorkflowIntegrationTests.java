package com.example.rental;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanLockAndUnlockTenantAccount() throws Exception {
        String adminToken = login("admin@rental.local", "Password123!");
        JsonNode users = getData("/api/admin/users", adminToken);
        long tenantAccountId = findUserId(users, "tenant@rental.local");

        updateStatus(adminToken, tenantAccountId, "LOCKED");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("tenant@rental.local", "Password123!")))
            .andExpect(status().isUnauthorized());

        updateStatus(adminToken, tenantAccountId, "ACTIVE");
        login("tenant@rental.local", "Password123!");
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
}
