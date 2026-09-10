package com.enterprise.oms.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Security enabled: JWT required on /api/**, role-based writes from the {@code roles} claim,
 * public health/docs. Tokens are really signed (HS256) and verified by the configured decoder.
 */
@SpringBootTest(properties = {
        "app.security.enabled=true",
        "app.security.jwt.secret=" + SecurityIT.SECRET
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIT {

    static final String SECRET = "local-development-secret-at-least-32-bytes-long";

    @Autowired MockMvc mvc;

    @Test
    void anonymousCallsAreRejectedOnTheApi() throws Exception {
        mvc.perform(get("/api/v1/products")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/products").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUsersCanReadButNeedRolesToWrite() throws Exception {
        mvc.perform(get("/api/v1/products").with(jwt())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/products").header(HttpHeaders.AUTHORIZATION, bearer("reader", List.of())))
                .andExpect(status().isOk());

        String product = """
                {"sku":"SEC-%s","name":"Secured","price":1,"currency":"THB","initialStock":1}"""
                .formatted(UUID.randomUUID().toString().substring(0, 8));

        mvc.perform(post("/api/v1/products").header(HttpHeaders.AUTHORIZATION, bearer("reader", List.of()))
                        .contentType(MediaType.APPLICATION_JSON).content(product))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/products").header(HttpHeaders.AUTHORIZATION, bearer("catalog-admin", List.of("CATALOG")))
                        .contentType(MediaType.APPLICATION_JSON).content(product))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").exists());

        mvc.perform(get("/actuator/metrics").header(HttpHeaders.AUTHORIZATION, bearer("sre", List.of("OPS"))))
                .andExpect(status().isOk());
    }

    static String bearer(String subject, List<String> roles) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim("roles", roles)
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        return "Bearer " + jwt.serialize();
    }
}
