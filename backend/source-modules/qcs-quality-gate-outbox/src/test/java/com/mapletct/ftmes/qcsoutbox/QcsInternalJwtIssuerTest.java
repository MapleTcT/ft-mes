package com.mapletct.ftmes.qcsoutbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QcsInternalJwtIssuerTest {

    @Test
    public void issuesShortLivedScopedHs256Token() throws Exception {
        QcsQualityGateOutboxProperties properties = properties();
        ObjectMapper mapper = new ObjectMapper();
        Instant now = Instant.parse("2026-07-21T00:00:00Z");
        String token = new QcsInternalJwtIssuer(
            properties, mapper, Clock.fixed(now, ZoneOffset.UTC))
            .issue(QcsQualityGateProjectorTest.record(1, QcsQualityGateProjectorTest.acceptedInspections()));

        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
        Map<String, Object> header = decode(mapper, parts[0]);
        Map<String, Object> claims = decode(mapper, parts[1]);
        assertEquals("HS256", header.get("alg"));
        assertEquals("ft-mes-adapter", claims.get("iss"));
        assertEquals("qcs-quality-gate-outbox", claims.get("sub"));
        assertEquals("1000", claims.get("tenant_id"));
        assertEquals(1784592000, ((Number) claims.get("iat")).intValue());
        assertEquals(1784592120, ((Number) claims.get("exp")).intValue());
        assertEquals("bpi-service", ((List<?>) claims.get("aud")).get(0));
        assertEquals("BPI_INTEGRATION_INGEST", ((List<?>) claims.get("roles")).get(0));
        assertEquals("PLANT-01", ((List<?>) claims.get("plant_ids")).get(0));
        assertEquals("LINE-S07-01", ((List<?>) claims.get("line_ids")).get(0));

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
            properties.getInternalJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expected = mac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        byte[] actual = Base64.getUrlDecoder().decode(parts[2]);
        assertTrue(java.util.Arrays.equals(expected, actual));
    }

    static QcsQualityGateOutboxProperties properties() {
        QcsQualityGateOutboxProperties properties = new QcsQualityGateOutboxProperties();
        properties.setInternalJwtSecret("0123456789abcdef0123456789abcdef");
        properties.setInternalJwtIssuer("ft-mes-adapter");
        properties.setInternalJwtAudience("bpi-service");
        properties.setInternalJwtSubject("qcs-quality-gate-outbox");
        properties.setInternalTokenTtlSeconds(120L);
        return properties;
    }

    private static Map<String, Object> decode(ObjectMapper mapper, String value) throws Exception {
        return mapper.readValue(
            Base64.getUrlDecoder().decode(value),
            new TypeReference<Map<String, Object>>() { });
    }
}
