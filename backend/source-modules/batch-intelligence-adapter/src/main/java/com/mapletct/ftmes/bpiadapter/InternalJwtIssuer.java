package com.mapletct.ftmes.bpiadapter;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

@Component
public class InternalJwtIssuer {

    private final BpiAdapterProperties properties;
    private final Clock clock;

    @Autowired
    public InternalJwtIssuer(BpiAdapterProperties properties) {
        this(properties, Clock.systemUTC());
    }

    InternalJwtIssuer(BpiAdapterProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String issue(BpiActor actor) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.getInternalTokenTtl());
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(properties.getInternalJwtIssuer())
                .subject(actor.getSubject())
                .audience(properties.getInternalJwtAudience())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .jwtID(UUID.randomUUID().toString())
                .claim("tenant_id", actor.getTenantId())
                .claim("roles", new ArrayList<String>(actor.getRoles()))
                .claim("plant_ids", new ArrayList<String>(actor.getPlantIds()))
                .claim("line_ids", new ArrayList<String>(actor.getLineIds()))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(properties.getInternalJwtSecret().getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to sign internal BPI token", e);
        }
    }
}
