package com.mapletct.ftmes.qcsoutbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Component
@ConditionalOnProperty(name = "qcs.bpi.outbox.enabled", havingValue = "true")
public class QcsInternalJwtIssuer {

    private final QcsQualityGateOutboxProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public QcsInternalJwtIssuer(
            QcsQualityGateOutboxProperties properties,
            ObjectMapper objectMapper) {
        this(properties, objectMapper, Clock.systemUTC());
    }

    QcsInternalJwtIssuer(
            QcsQualityGateOutboxProperties properties,
            ObjectMapper objectMapper,
            Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public String issue(QcsQualityGateOutboxRecord record) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusSeconds(properties.getInternalTokenTtlSeconds());
        try {
            Map<String, Object> header = new LinkedHashMap<String, Object>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            Map<String, Object> claims = new LinkedHashMap<String, Object>();
            claims.put("iss", properties.getInternalJwtIssuer());
            claims.put("sub", properties.getInternalJwtSubject());
            claims.put("aud", Collections.singletonList(properties.getInternalJwtAudience()));
            claims.put("iat", issuedAt.getEpochSecond());
            claims.put("exp", expiresAt.getEpochSecond());
            claims.put("jti", UUID.randomUUID().toString());
            claims.put("tenant_id", record.getTenantId());
            claims.put("roles", Collections.singletonList("BPI_INTEGRATION_INGEST"));
            claims.put("plant_ids", Collections.singletonList(record.getPlantId()));
            claims.put("line_ids", Collections.singletonList(record.getLineId()));
            String content = encode(objectMapper.writeValueAsBytes(header)) + "."
                + encode(objectMapper.writeValueAsBytes(claims));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                properties.getInternalJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return content + "." + encode(mac.doFinal(content.getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException error) {
            throw new PermanentQcsOutboxException("Unable to sign the internal BPI token", error);
        } catch (Exception error) {
            throw new PermanentQcsOutboxException("Unable to serialize the internal BPI token", error);
        }
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
