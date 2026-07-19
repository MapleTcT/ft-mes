package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Component
public final class DataQualityIncidentCursorCodec {
    private static final String VERSION = "v1";
    private static final String SEPARATOR = "\u001f";
    private static final int MAX_CURSOR_LENGTH = 2_048;
    private static final byte[] DOMAIN = "bpi.data-quality.incident.cursor.v1|"
            .getBytes(StandardCharsets.UTF_8);

    private final byte[] secret;

    public DataQualityIncidentCursorCodec(
            @Value("${bpi.security.internal-jwt-secret}") String internalJwtSecret) {
        this.secret = internalJwtSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String encode(Cursor cursor, String scopeFingerprint) {
        String payload = String.join(SEPARATOR,
                VERSION,
                cursor.snapshotAt().toString(),
                Long.toString(cursor.affectedBatchCount()),
                Integer.toString(cursor.severityRank()),
                cursor.lastSeen().toString(),
                cursor.id().toString(),
                scopeFingerprint);
        String value = payload + SEPARATOR + signature(payload);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public Cursor decode(String encoded, String expectedScopeFingerprint) {
        if (encoded == null || encoded.isBlank() || encoded.length() > MAX_CURSOR_LENGTH) {
            throw invalidCursor();
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = decoded.split(SEPARATOR, -1);
            if (parts.length != 8 || !VERSION.equals(parts[0])) throw invalidCursor();
            String payload = String.join(
                    SEPARATOR, parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
            if (!MessageDigest.isEqual(
                    parts[7].getBytes(StandardCharsets.UTF_8),
                    signature(payload).getBytes(StandardCharsets.UTF_8))
                    || !MessageDigest.isEqual(
                            parts[6].getBytes(StandardCharsets.UTF_8),
                            expectedScopeFingerprint.getBytes(StandardCharsets.UTF_8))) {
                throw invalidCursor();
            }
            Instant snapshotAt = Instant.parse(parts[1]);
            long affectedBatchCount = Long.parseLong(parts[2]);
            int severityRank = Integer.parseInt(parts[3]);
            Instant lastSeen = Instant.parse(parts[4]);
            if (affectedBatchCount < 0 || severityRank < 1 || severityRank > 4) {
                throw invalidCursor();
            }
            return new Cursor(
                    snapshotAt, affectedBatchCount, severityRank, lastSeen, UUID.fromString(parts[5]));
        } catch (BpiValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidCursor();
        }
    }

    private String signature(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            mac.update(DOMAIN);
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 is required for BPI cursors.", exception);
        }
    }

    private BpiValidationException invalidCursor() {
        return new BpiValidationException(
                "Data-quality incident cursor is invalid or does not match the requested scope.");
    }

    public record Cursor(
            Instant snapshotAt,
            long affectedBatchCount,
            int severityRank,
            Instant lastSeen,
            UUID id) {
    }
}
