package com.mapletct.ftmes.bpiadapter;

import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InternalJwtIssuerTest {

    @Test
    public void signsBoundedInternalClaimsForBpiService() throws Exception {
        BpiAdapterProperties properties = new BpiAdapterProperties();
        properties.setInternalJwtSecret("0123456789abcdef0123456789abcdef");
        properties.setInternalTokenTtl(Duration.ofMinutes(10));
        Instant now = Instant.parse("2026-07-12T09:00:00Z");
        BpiActor actor = new BpiActor("user-1", "1000",
                new LinkedHashSet<String>(Arrays.asList("BPI_ADMIN", "BPI_OPERATOR")),
                Collections.singleton("PLANT-01"), Collections.singleton("LINE-S07-01"));

        String token = new InternalJwtIssuer(properties, Clock.fixed(now, ZoneOffset.UTC)).issue(actor);
        SignedJWT parsed = SignedJWT.parse(token);

        assertTrue(parsed.verify(new MACVerifier(properties.getInternalJwtSecret().getBytes(StandardCharsets.UTF_8))));
        assertEquals("ft-mes-adapter", parsed.getJWTClaimsSet().getIssuer());
        assertEquals(Collections.singletonList("bpi-service"), parsed.getJWTClaimsSet().getAudience());
        assertEquals("1000", parsed.getJWTClaimsSet().getStringClaim("tenant_id"));
        assertEquals(now.plusSeconds(600), parsed.getJWTClaimsSet().getExpirationTime().toInstant());
    }
}
