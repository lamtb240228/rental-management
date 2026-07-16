package com.example.rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class BusinessInvariantIntegrationTests {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

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

    private String landlordToken;

    @BeforeEach
    void loginLandlord() throws Exception {
        landlordToken = login("demo@rental.local", "Password123!");
    }

    @Test
    void activeContractPreventsInconsistentRoomTenantAndPropertyUpdates() throws Exception {
        RentalFixture fixture = createFixture("ACTIVE");

        assertStatus(jsonRequest(
            put("/api/rooms/{id}", fixture.roomId()),
            roomBody(fixture.suffix(), "AVAILABLE")
        ), 400);
        assertStatus(jsonRequest(
            put("/api/tenants/{id}", fixture.tenantId()),
            tenantBody(fixture.suffix(), "INACTIVE")
        ), 400);
        assertStatus(jsonRequest(
            put("/api/properties/{id}", fixture.propertyId()),
            propertyBody(fixture.suffix(), "INACTIVE")
        ), 400);

        assertEquals("OCCUPIED", getData("/api/rooms/" + fixture.roomId()).get("status").asText());
        assertEquals("ACTIVE", getData("/api/tenants/" + fixture.tenantId()).get("status").asText());
        assertEquals("ACTIVE", getData("/api/properties/" + fixture.propertyId()).get("status").asText());
    }

    @Test
    void activeContractCreationRequiresOperationalEntitiesAndCurrentDates() throws Exception {
        RentalFixture dateFixture = createFixture("DRAFT");
        assertStatus(jsonRequest(
            post("/api/contracts"),
            contractBody(
                dateFixture.roomId(),
                dateFixture.tenantId(),
                "CT-FUTURE-" + dateFixture.suffix(),
                LocalDate.now().plusDays(1),
                null,
                "ACTIVE"
            )
        ), 400);
        assertStatus(jsonRequest(
            post("/api/contracts"),
            contractBody(
                dateFixture.roomId(),
                dateFixture.tenantId(),
                "CT-EXPIRED-" + dateFixture.suffix(),
                LocalDate.now().minusDays(20),
                LocalDate.now().minusDays(1),
                "ACTIVE"
            )
        ), 400);

        RentalFixture inactiveTenantFixture = createFixture("DRAFT");
        assertStatus(jsonRequest(
            put("/api/tenants/{id}", inactiveTenantFixture.tenantId()),
            tenantBody(inactiveTenantFixture.suffix(), "INACTIVE")
        ), 200);
        assertStatus(jsonRequest(
            post("/api/contracts"),
            contractBody(
                inactiveTenantFixture.roomId(),
                inactiveTenantFixture.tenantId(),
                "CT-INACTIVE-TENANT-" + inactiveTenantFixture.suffix(),
                LocalDate.now().minusDays(1),
                null,
                "ACTIVE"
            )
        ), 400);

        RentalFixture inactivePropertyFixture = createFixture("DRAFT");
        assertStatus(jsonRequest(
            put("/api/properties/{id}", inactivePropertyFixture.propertyId()),
            propertyBody(inactivePropertyFixture.suffix(), "INACTIVE")
        ), 200);
        assertStatus(jsonRequest(
            post("/api/contracts"),
            contractBody(
                inactivePropertyFixture.roomId(),
                inactivePropertyFixture.tenantId(),
                "CT-INACTIVE-PROPERTY-" + inactivePropertyFixture.suffix(),
                LocalDate.now().minusDays(1),
                null,
                "ACTIVE"
            )
        ), 400);

        RentalFixture occupiedRoomFixture = createFixture("DRAFT");
        assertStatus(jsonRequest(
            put("/api/rooms/{id}", occupiedRoomFixture.roomId()),
            roomBody(occupiedRoomFixture.suffix(), "OCCUPIED")
        ), 200);
        assertStatus(jsonRequest(
            post("/api/contracts"),
            contractBody(
                occupiedRoomFixture.roomId(),
                occupiedRoomFixture.tenantId(),
                "CT-OCCUPIED-ROOM-" + occupiedRoomFixture.suffix(),
                LocalDate.now().minusDays(1),
                null,
                "ACTIVE"
            )
        ), 400);
    }

    @Test
    void endingContractRejectsInvalidDatesAndSynchronizesOccupancy() throws Exception {
        RentalFixture fixture = createFixture("ACTIVE");

        MvcResult sameAsStart = mockMvc.perform(patch("/api/contracts/{id}/end", fixture.contractId())
                .param("endDate", fixture.startDate().toString())
                .header("Authorization", bearer(landlordToken)))
            .andReturn();
        assertStatus(sameAsStart, 400);

        MvcResult futureEnd = mockMvc.perform(patch("/api/contracts/{id}/end", fixture.contractId())
                .param("endDate", LocalDate.now().plusDays(1).toString())
                .header("Authorization", bearer(landlordToken)))
            .andReturn();
        assertStatus(futureEnd, 400);

        MvcResult validEnd = mockMvc.perform(patch("/api/contracts/{id}/end", fixture.contractId())
                .param("endDate", LocalDate.now().toString())
                .header("Authorization", bearer(landlordToken)))
            .andReturn();
        assertStatus(validEnd, 200);
        assertEquals("ENDED", responseData(validEnd).get("status").asText());
        assertEquals("AVAILABLE", getData("/api/rooms/" + fixture.roomId()).get("status").asText());

        Date moveOutDate = jdbcTemplate.queryForObject(
            "select move_out_date from contract_tenants where contract_id = ? and tenant_id = ?",
            Date.class,
            fixture.contractId(),
            fixture.tenantId()
        );
        assertNotNull(moveOutDate);
        assertEquals(LocalDate.now(), moveOutDate.toLocalDate());

        MvcResult repeatedEnd = mockMvc.perform(patch("/api/contracts/{id}/end", fixture.contractId())
                .header("Authorization", bearer(landlordToken)))
            .andReturn();
        assertStatus(repeatedEnd, 400);
    }

    @Test
    void invoiceRequiresActiveContractAndLinksMatchingUtilityReading() throws Exception {
        RentalFixture draftFixture = createFixture("DRAFT");
        assertStatus(jsonRequest(
            post("/api/invoices"),
            invoiceBody(draftFixture, 2030, 1, "INV-DRAFT-" + draftFixture.suffix(), false, true, 100)
        ), 400);

        RentalFixture activeFixture = createFixture("ACTIVE");
        assertStatus(jsonRequest(
            post("/api/invoices"),
            invoiceBody(activeFixture, 2030, 1, "INV-FUTURE-" + activeFixture.suffix(), false, true, 1_000)
        ), 400);
        int year = 2024;
        int month = 2;
        MvcResult readingResult = jsonRequest(
            post("/api/rooms/{roomId}/utility-readings", activeFixture.roomId()),
            utilityReadingBody(year, month)
        );
        assertStatus(readingResult, 200);
        long readingId = responseData(readingResult).get("id").asLong();

        assertStatus(jsonRequest(
            post("/api/invoices"),
            invoiceBody(activeFixture, year, month, "INV-MISSING-UTILITY-" + activeFixture.suffix(), false, true, 1_000)
        ), 400);

        assertStatus(jsonRequest(
            post("/api/invoices"),
            invoiceBody(activeFixture, year, month, "INV-WRONG-" + activeFixture.suffix(), true, false, 1_000)
        ), 400);
        assertNull(jdbcTemplate.queryForObject(
            "select invoice_id from utility_readings where id = ?",
            Long.class,
            readingId
        ));

        MvcResult invoiceResult = jsonRequest(
            post("/api/invoices"),
            invoiceBody(activeFixture, year, month, "INV-UTILITY-" + activeFixture.suffix(), true, true, 1_000)
        );
        assertStatus(invoiceResult, 200);
        long invoiceId = responseData(invoiceResult).get("id").asLong();
        assertEquals(invoiceId, jdbcTemplate.queryForObject(
            "select invoice_id from utility_readings where id = ?",
            Long.class,
            readingId
        ));

        ObjectNode changedReading = utilityReadingBody(year, month);
        changedReading.put("electricityNewReading", 111);
        assertStatus(jsonRequest(put("/api/utility-readings/{id}", readingId), changedReading), 400);

        MvcResult cancelled = mockMvc.perform(patch("/api/invoices/{id}/cancel", invoiceId)
                .header("Authorization", bearer(landlordToken)))
            .andReturn();
        assertStatus(cancelled, 200);
        assertEquals("CANCELLED", responseData(cancelled).get("status").asText());
        assertNull(jdbcTemplate.queryForObject(
            "select invoice_id from utility_readings where id = ?",
            Long.class,
            readingId
        ));

        MvcResult replacement = jsonRequest(
            post("/api/invoices"),
            invoiceBody(activeFixture, year, month, "INV-REISSUED-" + activeFixture.suffix(), true, true, 1_000)
        );
        assertStatus(replacement, 200);
        long replacementId = responseData(replacement).get("id").asLong();
        assertEquals(replacementId, jdbcTemplate.queryForObject(
            "select invoice_id from utility_readings where id = ?",
            Long.class,
            readingId
        ));
    }

    @Test
    void existingSeedInvoiceProtectsUnlinkedUtilityReading() throws Exception {
        Long readingId = jdbcTemplate.queryForObject("""
            select reading.id
            from utility_readings reading
            join rooms room on room.id = reading.room_id
            join properties property on property.id = room.property_id
            where property.name = 'Demo Property'
              and room.room_number = '101'
              and reading.billing_year = 2026
              and reading.billing_month = 6
              and reading.deleted_at is null
            """, Long.class);
        assertNotNull(readingId);
        assertNull(jdbcTemplate.queryForObject(
            "select invoice_id from utility_readings where id = ?",
            Long.class,
            readingId
        ));

        ObjectNode seedReading = objectMapper.createObjectNode();
        seedReading.put("billingYear", 2026);
        seedReading.put("billingMonth", 6);
        seedReading.put("electricityOldReading", 1_200);
        seedReading.put("electricityNewReading", 1_300);
        seedReading.put("electricityUnitPrice", 3_500);
        seedReading.put("waterOldReading", 80);
        seedReading.put("waterNewReading", 86);
        seedReading.put("waterUnitPrice", 20_000);

        assertStatus(jsonRequest(put("/api/utility-readings/{id}", readingId), seedReading), 400);
    }

    @Test
    void utilityReadingsRemainContinuousAcrossAdjacentPeriods() throws Exception {
        RentalFixture fixture = createFixture("ACTIVE");
        int year = 2024;

        MvcResult firstResult = jsonRequest(
            post("/api/rooms/{roomId}/utility-readings", fixture.roomId()),
            utilityReadingBody(year, 5)
        );
        assertStatus(firstResult, 200);
        long firstReadingId = responseData(firstResult).get("id").asLong();

        assertStatus(jsonRequest(
            post("/api/rooms/{roomId}/utility-readings", fixture.roomId()),
            utilityReadingBody(year, 6)
        ), 400);

        ObjectNode nextReading = utilityReadingBody(year, 6);
        nextReading.put("electricityOldReading", 110);
        nextReading.put("electricityNewReading", 125);
        nextReading.put("waterOldReading", 23);
        nextReading.put("waterNewReading", 25);
        MvcResult nextResult = jsonRequest(
            post("/api/rooms/{roomId}/utility-readings", fixture.roomId()),
            nextReading
        );
        assertStatus(nextResult, 200);
        long nextReadingId = responseData(nextResult).get("id").asLong();

        ObjectNode invalidFirstUpdate = utilityReadingBody(year, 5);
        invalidFirstUpdate.put("electricityNewReading", 111);
        assertStatus(jsonRequest(
            put("/api/utility-readings/{id}", firstReadingId),
            invalidFirstUpdate
        ), 400);

        ObjectNode invalidNextUpdate = nextReading.deepCopy();
        invalidNextUpdate.put("electricityOldReading", 109);
        assertStatus(jsonRequest(
            put("/api/utility-readings/{id}", nextReadingId),
            invalidNextUpdate
        ), 400);
    }

    @Test
    void maintenanceTenantMustOccupyTheSelectedRoom() throws Exception {
        JsonNode properties = getData("/api/properties");
        JsonNode demoProperty = findByText(properties, "name", "Demo Property");
        JsonNode rooms = getData("/api/properties/" + demoProperty.get("id").asLong() + "/rooms");
        long occupiedRoomId = findByText(rooms, "roomNumber", "101").get("id").asLong();
        long otherRoomId = findByText(rooms, "roomNumber", "102").get("id").asLong();
        JsonNode tenant = findByText(getData("/api/tenants"), "identityNumber", "DEMO123456");

        ObjectNode invalidRequest = maintenanceBody(otherRoomId, tenant.get("id").asLong(), "Wrong room");
        assertStatus(jsonRequest(post("/api/maintenance-requests"), invalidRequest), 400);

        ObjectNode validRequest = maintenanceBody(occupiedRoomId, tenant.get("id").asLong(), "Occupied room");
        assertStatus(jsonRequest(post("/api/maintenance-requests"), validRequest), 200);
    }

    @Test
    void concurrentCompletedPaymentsCannotOverpayInvoice() throws Exception {
        RentalFixture fixture = createFixture("ACTIVE");
        MvcResult invoiceResult = jsonRequest(
            post("/api/invoices"),
            invoiceBody(fixture, 2024, 1, "INV-CONCURRENT-" + fixture.suffix(), false, true, 100)
        );
        assertStatus(invoiceResult, 200);
        long invoiceId = responseData(invoiceResult).get("id").asLong();
        assertStatus(jsonRequest(
            post("/api/rooms/{roomId}/utility-readings", fixture.roomId()),
            utilityReadingBody(2024, 1)
        ), 400);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> first = executor.submit(() -> createConcurrentPayment(invoiceId, "PAY-A-" + fixture.suffix(), start));
            Future<Integer> second = executor.submit(() -> createConcurrentPayment(invoiceId, "PAY-B-" + fixture.suffix(), start));
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            statuses.add(first.get(20, TimeUnit.SECONDS));
            statuses.add(second.get(20, TimeUnit.SECONDS));
            Collections.sort(statuses);
            assertEquals(List.of(200, 400), statuses);
        } finally {
            executor.shutdownNow();
        }

        JsonNode invoice = getData("/api/invoices/" + invoiceId);
        assertEquals(0, invoice.get("paidAmount").decimalValue().compareTo(new BigDecimal("70.00")));
        assertEquals("PARTIALLY_PAID", invoice.get("status").asText());
        assertEquals(1, getData("/api/invoices/" + invoiceId + "/payments").size());
    }

    @Test
    void concurrentAdjacentUtilityReadingsCannotBreakContinuity() throws Exception {
        RentalFixture fixture = createFixture("ACTIVE");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> first = executor.submit(() -> createConcurrentReading(
                fixture.roomId(), 2024, 7, 100, 110, start
            ));
            Future<Integer> second = executor.submit(() -> createConcurrentReading(
                fixture.roomId(), 2024, 8, 100, 120, start
            ));
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            statuses.add(first.get(20, TimeUnit.SECONDS));
            statuses.add(second.get(20, TimeUnit.SECONDS));
            Collections.sort(statuses);
            assertEquals(List.of(200, 400), statuses);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentContractEndAndReplacementKeepRoomStatusConsistent() throws Exception {
        RentalFixture fixture = createFixture("ACTIVE");
        int tenantSuffix = SEQUENCE.incrementAndGet();
        MvcResult tenantResult = jsonRequest(post("/api/tenants"), tenantBody(tenantSuffix, "ACTIVE"));
        assertStatus(tenantResult, 200);
        long replacementTenantId = responseData(tenantResult).get("id").asLong();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Integer> statuses = new ArrayList<>();
        try {
            Future<Integer> end = executor.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return mockMvc.perform(patch("/api/contracts/{id}/end", fixture.contractId())
                        .param("endDate", LocalDate.now().toString())
                        .header("Authorization", bearer(landlordToken)))
                    .andReturn().getResponse().getStatus();
            });
            Future<Integer> replacement = executor.submit(() -> {
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return jsonRequest(
                    post("/api/contracts"),
                    contractBody(
                        fixture.roomId(),
                        replacementTenantId,
                        "CT-REPLACEMENT-" + fixture.suffix(),
                        LocalDate.now(),
                        null,
                        "ACTIVE"
                    )
                ).getResponse().getStatus();
            });
            start.countDown();
            statuses.add(end.get(20, TimeUnit.SECONDS));
            statuses.add(replacement.get(20, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        assertTrue(statuses.contains(200));
        String roomStatus = getData("/api/rooms/" + fixture.roomId()).get("status").asText();
        if (statuses.stream().allMatch(status -> status == 200)) {
            assertEquals("OCCUPIED", roomStatus);
        } else {
            assertEquals(List.of(200, 400), statuses.stream().sorted().toList());
            assertEquals("AVAILABLE", roomStatus);
        }
    }

    private int createConcurrentPayment(long invoiceId, String transactionReference, CountDownLatch start) throws Exception {
        assertTrue(start.await(10, TimeUnit.SECONDS));
        ObjectNode payment = objectMapper.createObjectNode();
        payment.put("amount", 70);
        payment.put("paymentMethod", "BANK_TRANSFER");
        payment.put("paymentStatus", "COMPLETED");
        payment.put("transactionReference", transactionReference);
        return jsonRequest(post("/api/invoices/{invoiceId}/payments", invoiceId), payment)
            .getResponse()
            .getStatus();
    }

    private int createConcurrentReading(
        long roomId,
        int year,
        int month,
        int oldReading,
        int newReading,
        CountDownLatch start
    ) throws Exception {
        assertTrue(start.await(10, TimeUnit.SECONDS));
        ObjectNode reading = utilityReadingBody(year, month);
        reading.put("electricityOldReading", oldReading);
        reading.put("electricityNewReading", newReading);
        return jsonRequest(post("/api/rooms/{roomId}/utility-readings", roomId), reading)
            .getResponse()
            .getStatus();
    }

    private RentalFixture createFixture(String contractStatus) throws Exception {
        int suffix = SEQUENCE.incrementAndGet();

        MvcResult propertyResult = jsonRequest(post("/api/properties"), propertyBody(suffix, "ACTIVE"));
        assertStatus(propertyResult, 200);
        long propertyId = responseData(propertyResult).get("id").asLong();

        MvcResult roomResult = jsonRequest(post("/api/properties/{propertyId}/rooms", propertyId), roomBody(suffix, "AVAILABLE"));
        assertStatus(roomResult, 200);
        long roomId = responseData(roomResult).get("id").asLong();

        MvcResult tenantResult = jsonRequest(post("/api/tenants"), tenantBody(suffix, "ACTIVE"));
        assertStatus(tenantResult, 200);
        long tenantId = responseData(tenantResult).get("id").asLong();

        LocalDate startDate = LocalDate.of(2020, 1, 1);
        ObjectNode contract = contractBody(
            roomId,
            tenantId,
            "CT-INVARIANT-" + suffix,
            startDate,
            null,
            contractStatus
        );

        MvcResult contractResult = jsonRequest(post("/api/contracts"), contract);
        assertStatus(contractResult, 200);
        long contractId = responseData(contractResult).get("id").asLong();
        return new RentalFixture(suffix, propertyId, roomId, tenantId, contractId, startDate);
    }

    private ObjectNode contractBody(
        long roomId,
        long tenantId,
        String contractCode,
        LocalDate startDate,
        LocalDate endDate,
        String status
    ) {
        ObjectNode contract = objectMapper.createObjectNode();
        contract.put("roomId", roomId);
        contract.put("contractCode", contractCode);
        contract.put("startDate", startDate.toString());
        if (endDate != null) {
            contract.put("endDate", endDate.toString());
        }
        contract.put("monthlyRent", 1_000);
        contract.put("depositAmount", 100);
        contract.put("status", status);
        contract.putArray("tenantIds").add(tenantId);
        contract.put("primaryTenantId", tenantId);
        return contract;
    }

    private ObjectNode propertyBody(int suffix, String status) {
        ObjectNode property = objectMapper.createObjectNode();
        property.put("name", "Invariant Property " + suffix);
        property.put("addressLine", suffix + " Test Street");
        property.put("ward", "Test Ward");
        property.put("district", "Test District");
        property.put("provinceCity", "Ho Chi Minh City");
        property.put("description", "Business invariant integration test");
        property.put("status", status);
        return property;
    }

    private ObjectNode roomBody(int suffix, String status) {
        ObjectNode room = objectMapper.createObjectNode();
        room.put("roomNumber", "INV-" + suffix);
        room.put("floorNumber", 1);
        room.put("area", 20);
        room.put("monthlyRent", 1_000);
        room.put("defaultDeposit", 100);
        room.put("maxOccupants", 2);
        room.put("status", status);
        room.put("description", "Business invariant integration test");
        return room;
    }

    private ObjectNode tenantBody(int suffix, String status) {
        ObjectNode tenant = objectMapper.createObjectNode();
        tenant.put("fullName", "Invariant Tenant " + suffix);
        tenant.put("dateOfBirth", "1995-01-01");
        tenant.put("phone", "0900" + "%06d".formatted(suffix));
        tenant.put("email", "invariant-" + suffix + "@example.test");
        tenant.put("identityNumber", "INV-ID-" + suffix);
        tenant.put("permanentAddress", "Test address");
        tenant.put("status", status);
        return tenant;
    }

    private ObjectNode utilityReadingBody(int year, int month) {
        ObjectNode reading = objectMapper.createObjectNode();
        reading.put("billingYear", year);
        reading.put("billingMonth", month);
        reading.put("electricityOldReading", 100);
        reading.put("electricityNewReading", 110);
        reading.put("electricityUnitPrice", 3_500);
        reading.put("waterOldReading", 20);
        reading.put("waterNewReading", 23);
        reading.put("waterUnitPrice", 20_000);
        return reading;
    }

    private ObjectNode invoiceBody(
        RentalFixture fixture,
        int year,
        int month,
        String invoiceNumber,
        boolean withUtilities,
        boolean matchingUtilities,
        int rent
    ) {
        ObjectNode invoice = objectMapper.createObjectNode();
        invoice.put("contractId", fixture.contractId());
        invoice.put("invoiceNumber", invoiceNumber);
        invoice.put("billingYear", year);
        invoice.put("billingMonth", month);
        invoice.put("dueDate", LocalDate.now().plusDays(10).toString());
        invoice.put("discountAmount", 0);
        ArrayNode items = invoice.putArray("items");
        addInvoiceItem(items, "RENT", "Rent", 1, rent);
        if (withUtilities) {
            addInvoiceItem(items, "ELECTRICITY", "Electricity", matchingUtilities ? 10 : 9, 3_500);
            addInvoiceItem(items, "WATER", "Water", 3, 20_000);
        }
        return invoice;
    }

    private void addInvoiceItem(ArrayNode items, String type, String description, int quantity, int unitPrice) {
        ObjectNode item = items.addObject();
        item.put("itemType", type);
        item.put("description", description);
        item.put("quantity", quantity);
        item.put("unitPrice", unitPrice);
    }

    private ObjectNode maintenanceBody(long roomId, long tenantId, String title) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("roomId", roomId);
        request.put("tenantId", tenantId);
        request.put("title", title);
        request.put("description", "Integration test request");
        request.put("priority", "MEDIUM");
        return request;
    }

    private JsonNode getData(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path).header("Authorization", bearer(landlordToken))).andReturn();
        assertStatus(result, 200);
        return responseData(result);
    }

    private MvcResult jsonRequest(MockHttpServletRequestBuilder request, JsonNode body) throws Exception {
        return mockMvc.perform(request
                .header("Authorization", bearer(landlordToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andReturn();
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private JsonNode findByText(JsonNode array, String field, String value) {
        for (JsonNode item : array) {
            if (value.equals(item.path(field).asText())) {
                return item;
            }
        }
        throw new IllegalStateException("Seeded record not found: " + field + "=" + value);
    }

    private String login(String email, String password) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("email", email);
        request.put("password", password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
            .andReturn();
        assertStatus(result, 200);
        return responseData(result).get("accessToken").asText();
    }

    private void assertStatus(MvcResult result, int expectedStatus) throws Exception {
        assertEquals(expectedStatus, result.getResponse().getStatus(), result.getResponse().getContentAsString());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record RentalFixture(
        int suffix,
        long propertyId,
        long roomId,
        long tenantId,
        long contractId,
        LocalDate startDate
    ) {
    }
}
