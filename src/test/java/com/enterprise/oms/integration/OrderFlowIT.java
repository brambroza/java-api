package com.enterprise.oms.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterprise.oms.shared.infrastructure.outbox.OutboxEvent;
import com.enterprise.oms.shared.infrastructure.outbox.OutboxEventRepository;
import com.enterprise.oms.shared.infrastructure.outbox.OutboxRelay;
import com.enterprise.oms.support.RecordingEventPublisher;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** End-to-end order lifecycle through the HTTP API on embedded H2 (Flyway + ddl validate). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderFlowIT {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired OutboxEventRepository outbox;
    @Autowired OutboxRelay relay;
    @Autowired RecordingEventPublisher publisher;

    private String customerId;
    private String productId;

    @BeforeEach
    void seed() throws Exception {
        publisher.clear();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        customerId = read(mvc.perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"cust-%s@example.com","fullName":"Test Customer"}""".formatted(suffix)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Correlation-ID"))
                .andReturn()).get("id").asString();
        productId = read(mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
                {"sku":"SKU-%s","name":"Widget","price":125.50,"currency":"THB","initialStock":5}""".formatted(suffix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price.amount").value(125.50))
                .andReturn()).get("id").asString();
    }

    @Test
    void placesOrderIdempotentlyAndReservesStock() throws Exception {
        String key = "key-" + UUID.randomUUID();
        String body = orderBody(3);

        MvcResult first = mvc.perform(post("/api/v1/orders").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.total.amount").value(376.50))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andReturn();
        String orderNumber = read(first).get("orderNumber").asString();
        String orderId = read(first).get("id").asString();

        mvc.perform(get("/api/v1/inventory/{id}", productId))
                .andExpect(jsonPath("$.quantityReserved").value(3))
                .andExpect(jsonPath("$.available").value(2));

        // replay with the same key: same order, no second reservation
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber));
        mvc.perform(get("/api/v1/inventory/{id}", productId)).andExpect(jsonPath("$.quantityReserved").value(3));

        // same key, different payload
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody(1)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));

        // insufficient stock: business rule, nothing reserved
        mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "key-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody(10)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.correlationId").exists());
        mvc.perform(get("/api/v1/inventory/{id}", productId)).andExpect(jsonPath("$.quantityReserved").value(3));

        // transactional outbox: OrderPlaced recorded with the order, then relayed
        List<OutboxEvent> pending = outbox.findByAggregateIdOrderByOccurredAtAsc(orderId);
        assertThat(pending).extracting(OutboxEvent::getEventType).containsExactly("OrderPlacedEvent");
        assertThat(pending.getFirst().getPublishedAt()).isNull();
        assertThat(relay.publishPending()).isGreaterThanOrEqualTo(1);
        assertThat(publisher.messages()).anySatisfy(m -> {
            assertThat(m.eventType()).isEqualTo("OrderPlacedEvent");
            assertThat(m.aggregateId()).isEqualTo(orderId);
            assertThat(m.payload()).contains(orderNumber);
        });
        assertThat(outbox.findById(pending.getFirst().getId()).orElseThrow().getPublishedAt()).isNotNull();

        // cancel releases the reservation
        mvc.perform(post("/api/v1/orders/{id}/cancel", orderId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"changed my mind"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("changed my mind"));
        mvc.perform(get("/api/v1/inventory/{id}", productId))
                .andExpect(jsonPath("$.quantityReserved").value(0))
                .andExpect(jsonPath("$.available").value(5));

        mvc.perform(post("/api/v1/orders/{id}/confirm", orderId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("ILLEGAL_STATUS_TRANSITION"));
    }

    @Test
    void fulfilmentCommitsStockAndExposesLookups() throws Exception {
        String orderId = read(mvc.perform(post("/api/v1/orders").header("Idempotency-Key", "key-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody(2)))
                .andExpect(status().isCreated()).andReturn()).get("id").asString();

        mvc.perform(post("/api/v1/orders/{id}/confirm", orderId)).andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(post("/api/v1/orders/{id}/pay", orderId)).andExpect(jsonPath("$.status").value("PAID"));
        mvc.perform(post("/api/v1/orders/{id}/ship", orderId)).andExpect(jsonPath("$.status").value("SHIPPED"));
        mvc.perform(post("/api/v1/orders/{id}/deliver", orderId)).andExpect(jsonPath("$.status").value("DELIVERED"));

        mvc.perform(get("/api/v1/inventory/{id}", productId))
                .andExpect(jsonPath("$.quantityOnHand").value(3))
                .andExpect(jsonPath("$.quantityReserved").value(0));

        mvc.perform(get("/api/v1/orders").param("customerId", customerId).param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(orderId))
                .andExpect(jsonPath("$.content[0].items", hasSize(1)));

        String orderNumber = read(mvc.perform(get("/api/v1/orders/{id}", orderId)).andReturn()).get("orderNumber").asString();
        mvc.perform(get("/api/v1/orders/by-number/{n}", orderNumber)).andExpect(jsonPath("$.id").value(orderId));
    }

    @Test
    void mapsErrorsToProblemDetails() throws Exception {
        mvc.perform(get("/api/v1/orders/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.correlationId").exists());

        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku":"bad sku!","name":"","price":-1,"currency":"THBX","initialStock":-5}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors", hasSize(5)));

        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(orderBody(1)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(PROBLEM_JSON));

        mvc.perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"dup@example.com","fullName":"One"}""")).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"DUP@example.com","fullName":"Two"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CUSTOMER_EMAIL_EXISTS"));

        mvc.perform(get("/api/v1/customers/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(PROBLEM_JSON));
    }

    @Test
    void exposesOperationalEndpoints() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database.vendor").value("h2"))
                .andExpect(jsonPath("$.database.dialect").value("H2Dialect"));
        mvc.perform(get("/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/api/v1/orders']").exists());
        mvc.perform(get("/api/v1/products").header("X-Correlation-ID", "trace-abc-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", "trace-abc-123"));
    }

    private String orderBody(int quantity) {
        return """
                {"customerId":"%s","lines":[{"productId":"%s","quantity":%d}]}""".formatted(customerId, productId, quantity);
    }

    private JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }
}
