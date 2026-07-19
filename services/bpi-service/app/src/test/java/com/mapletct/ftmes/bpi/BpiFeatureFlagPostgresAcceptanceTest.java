package com.mapletct.ftmes.bpi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.ActorContext;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "BPI_TEST_DATABASE_URL", matches = ".+")
class BpiFeatureFlagPostgresAcceptanceTest {
    private static final String SECRET = "bpi-test-secret-0123456789abcdef";
    private static final String PLANT_ID = "PLANT-01";
    private static final String LINE_ID = "LINE-S07-01";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("BPI_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> env("BPI_TEST_DATABASE_USER", "postgres"));
        registry.add("spring.datasource.password", () -> env("BPI_TEST_DATABASE_PASSWORD", ""));
        registry.add("bpi.security.internal-jwt-secret", () -> SECRET);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired BpiPostgresRepository sharedRepository;

    private String tenantId;
    private String viewerToken;
    private String scopedAdminToken;
    private String tenantAdminToken;

    @BeforeEach
    void setUp() throws Exception {
        tenantId = "ADP_E2E_BPI_FLAGS_" + UUID.randomUUID().toString().replace("-", "");
        viewerToken = token("flag-viewer", List.of("BPI_VIEWER"), List.of(PLANT_ID), List.of(LINE_ID));
        scopedAdminToken = token("flag-line-admin", List.of("BPI_ADMIN"), List.of(PLANT_ID), List.of(LINE_ID));
        tenantAdminToken = token("flag-tenant-admin", List.of("BPI_ADMIN"), List.of("*"), List.of("*"));
    }

    @AfterEach
    void cleanupMarker() {
        if (tenantId == null) return;
        jdbc.update("DELETE FROM bpi.bpi_audit_events WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_api_idempotency WHERE tenant_id = ?", tenantId);
        jdbc.update("DELETE FROM bpi.bpi_feature_flags WHERE tenant_id = ?", tenantId);
    }

    @Test
    void governedOverridesAreScopedVersionedAuditedAndPhaseLocked() throws Exception {
        mockMvc.perform(get("/bpi/v1/feature-flags")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("plantId", PLANT_ID).param("lineId", LINE_ID)
                        .param("scopeType", "LINE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[?(@.flagKey == 'bpi.commands')].effectiveEnabled")
                        .value(hasItem(false)))
                .andExpect(jsonPath("$.data[?(@.flagKey == 'bpi.shadow-only')].effectiveEnabled")
                        .value(hasItem(true)))
                .andExpect(jsonPath("$.data[?(@.flagKey == 'bpi.wms-link')].enforcementStatus")
                        .value(hasItem("PHASE_LOCKED")));

        byte[] enableLine = command("LINE", "SET", true, "Enable commands for one governed pilot line");
        mockMvc.perform(post("/bpi/v1/feature-flags/{flagKey}", "bpi.commands")
                        .header("Authorization", "Bearer " + viewerToken)
                        .header("Idempotency-Key", "flag-viewer-denied-" + tenantId)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON).content(enableLine))
                .andExpect(status().isForbidden());

        String createKey = "flag-enable-line-" + tenantId;
        mockMvc.perform(post("/bpi/v1/feature-flags/{flagKey}", "bpi.commands")
                        .header("Authorization", "Bearer " + scopedAdminToken)
                        .header("Idempotency-Key", createKey)
                        .header("If-Match", "0")
                        .header("X-Trace-Id", tenantId + "_ENABLE")
                        .contentType(MediaType.APPLICATION_JSON).content(enableLine))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effectiveEnabled").value(true))
                .andExpect(jsonPath("$.data.effectiveScopeType").value("LINE"))
                .andExpect(jsonPath("$.data.overrideActive").value(true))
                .andExpect(jsonPath("$.data.overrideRevision").value(1));
        mockMvc.perform(post("/bpi/v1/feature-flags/{flagKey}", "bpi.commands")
                        .header("Authorization", "Bearer " + scopedAdminToken)
                        .header("Idempotency-Key", createKey)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON).content(enableLine))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.data.overrideRevision").value(1));

        assertThat(sharedRepository.featureEnabled(
                actor("flag-line-admin", Set.of(PLANT_ID), Set.of(LINE_ID)),
                PLANT_ID, LINE_ID, "bpi.commands")).isTrue();

        mockMvc.perform(post("/bpi/v1/feature-flags/{flagKey}", "bpi.commands")
                        .header("Authorization", "Bearer " + scopedAdminToken)
                        .header("Idempotency-Key", "flag-stale-" + tenantId)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(command("LINE", "SET", false, "Stale operator screen must not disable commands")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentRevision").value(1));

        mockMvc.perform(post("/bpi/v1/feature-flags/{flagKey}", "bpi.commands")
                        .header("Authorization", "Bearer " + scopedAdminToken)
                        .header("Idempotency-Key", "flag-disable-line-" + tenantId)
                        .header("If-Match", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(command("LINE", "SET", false, "Emergency stop for the governed pilot line")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effectiveEnabled").value(false))
                .andExpect(jsonPath("$.data.overrideRevision").value(2));

        mockMvc.perform(post("/bpi/v1/feature-flags/{flagKey}", "bpi.commands")
                        .header("Authorization", "Bearer " + scopedAdminToken)
                        .header("Idempotency-Key", "flag-inherit-line-" + tenantId)
                        .header("If-Match", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(command("LINE", "INHERIT", null, "Return the pilot line to parent scope policy")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effectiveEnabled").value(false))
                .andExpect(jsonPath("$.data.effectiveScopeType").value("GLOBAL"))
                .andExpect(jsonPath("$.data.overrideActive").value(false))
                .andExpect(jsonPath("$.data.overrideRevision").value(3));
        assertThat(sharedRepository.featureEnabled(
                actor("flag-line-admin", Set.of(PLANT_ID), Set.of(LINE_ID)),
                PLANT_ID, LINE_ID, "bpi.commands")).isFalse();

        mockMvc.perform(post("/bpi/v1/feature-flags/{flagKey}", "bpi.rule-management")
                        .header("Authorization", "Bearer " + scopedAdminToken)
                        .header("Idempotency-Key", "flag-tenant-denied-" + tenantId)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(command("TENANT", "SET", true, "Attempt a broad tenant rule management enable")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail", containsString("unrestricted tenant scope")));
        mockMvc.perform(post("/bpi/v1/feature-flags/{flagKey}", "bpi.rule-management")
                        .header("Authorization", "Bearer " + tenantAdminToken)
                        .header("Idempotency-Key", "flag-tenant-enable-" + tenantId)
                        .header("If-Match", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(command("TENANT", "SET", true, "Enable governed rule management for the tenant")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedScopeType").value("TENANT"))
                .andExpect(jsonPath("$.data.selectedScopeKey").value(tenantId))
                .andExpect(jsonPath("$.data.effectiveEnabled").value(true));

        for (String lockedFlag : List.of("bpi.ui", "bpi.shadow-only", "bpi.auto-confirm", "bpi.wms-link")) {
            mockMvc.perform(post("/bpi/v1/feature-flags/{flagKey}", lockedFlag)
                            .header("Authorization", "Bearer " + tenantAdminToken)
                            .header("Idempotency-Key", "flag-locked-" + lockedFlag + "-" + tenantId)
                            .header("If-Match", "0")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(command("LINE", "SET", !lockedFlag.equals("bpi.shadow-only"),
                                    "Phase locked flag must reject administrative mutation")))
                    .andExpect(status().isUnprocessableEntity());
        }

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_feature_flags
                 WHERE tenant_id = ? AND flag_key IN ('bpi.ui', 'bpi.shadow-only', 'bpi.auto-confirm', 'bpi.wms-link')
                """, Integer.class, tenantId)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_audit_events
                 WHERE tenant_id = ? AND object_type = 'FEATURE_FLAG'
                """, Integer.class, tenantId)).isEqualTo(4);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM bpi.bpi_api_idempotency
                 WHERE tenant_id = ? AND state = 'COMPLETED' AND response_status = 200
                """, Integer.class, tenantId)).isEqualTo(4);
    }

    private byte[] command(String scopeType, String mode, Boolean enabled, String reason) throws Exception {
        Map<String, Object> command = new java.util.LinkedHashMap<>();
        command.put("scopeType", scopeType);
        command.put("plantId", PLANT_ID);
        command.put("lineId", LINE_ID);
        command.put("mode", mode);
        if (enabled != null) command.put("enabled", enabled);
        command.put("reason", reason);
        return objectMapper.writeValueAsBytes(command);
    }

    private ActorContext actor(String subject, Set<String> plants, Set<String> lines) {
        return new ActorContext(tenantId, subject, Set.of("BPI_ADMIN"), plants, lines);
    }

    private String token(
            String subject,
            List<String> roles,
            List<String> plants,
            List<String> lines) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("ft-mes-adapter")
                .audience("bpi-service")
                .subject(subject)
                .claim("tenant_id", tenantId)
                .claim("roles", roles)
                .claim("plant_ids", plants)
                .claim("line_ids", lines)
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(600)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET));
        return jwt.serialize();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
