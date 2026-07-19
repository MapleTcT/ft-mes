package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Component
public final class PointCatalogCursorCodec {
    private static final String VERSION = "v1";
    private static final int MAX_CURSOR_LENGTH = 4_096;
    private static final int MAX_IDENTITY_LENGTH = 128;
    private static final byte[] DOMAIN = "bpi.point-catalog.cursor.v1|"
            .getBytes(StandardCharsets.UTF_8);

    private final byte[] secret;

    public PointCatalogCursorCodec(
            @Value("${bpi.security.internal-jwt-secret}") String internalJwtSecret) {
        this.secret = internalJwtSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String encode(Cursor cursor, String scopeFingerprint) {
        String payload = String.join(".",
                VERSION,
                cursor.snapshotId().toString(),
                encodePart(cursor.productId()),
                encodePart(cursor.deviceId()),
                encodePart(cursor.propertyId()),
                scopeFingerprint);
        return payload + "." + signature(payload);
    }

    public Cursor decode(String encoded, String expectedScopeFingerprint) {
        if (encoded == null || encoded.isBlank() || encoded.length() > MAX_CURSOR_LENGTH) {
            throw invalidCursor();
        }
        try {
            String[] parts = encoded.split("\\.", -1);
            if (parts.length != 7 || !VERSION.equals(parts[0])) {
                throw invalidCursor();
            }
            String payload = String.join(".", parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
            if (!constantTimeEquals(parts[6], signature(payload))
                    || !constantTimeEquals(parts[5], expectedScopeFingerprint)) {
                throw invalidCursor();
            }
            return new Cursor(
                    UUID.fromString(parts[1]),
                    decodePart(parts[2]),
                    decodePart(parts[3]),
                    decodePart(parts[4]));
        } catch (BpiValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidCursor();
        }
    }

    private String encodePart(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decodePart(String value) {
        String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        if (decoded.isBlank() || decoded.length() > MAX_IDENTITY_LENGTH) {
            throw invalidCursor();
        }
        return decoded;
    }

    private boolean constantTimeEquals(String actual, String expected) {
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
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
                "Point catalog cursor is invalid or does not match the requested scope.");
    }

    public record Cursor(
            UUID snapshotId,
            String productId,
            String deviceId,
            String propertyId) {
    }
}
