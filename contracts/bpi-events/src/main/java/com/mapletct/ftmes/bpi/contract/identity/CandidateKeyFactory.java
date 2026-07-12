package com.mapletct.ftmes.bpi.contract.identity;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/** Produces replay-stable UUIDv5 keys for START and END boundary candidates. */
public final class CandidateKeyFactory {

    public static final UUID BPI_CANDIDATE_NAMESPACE =
        UUID.fromString("072dd629-21bd-5d42-8385-815225bd5932");

    private CandidateKeyFactory() {
    }

    public static String startKey(
        String tenantId,
        String lineId,
        String ruleVersion,
        String contextOrderId,
        String firstQuorumEvidenceEventId
    ) {
        return uuidV5(BPI_CANDIDATE_NAMESPACE, canonicalName(
            tenantId,
            lineId,
            ruleVersion,
            contextOrderId,
            firstQuorumEvidenceEventId
        )).toString();
    }

    public static String endKey(
        String batchId,
        String ruleVersion,
        String firstEndQuorumEvidenceEventId
    ) {
        return uuidV5(BPI_CANDIDATE_NAMESPACE, canonicalName(
            batchId,
            ruleVersion,
            firstEndQuorumEvidenceEventId
        )).toString();
    }

    static UUID uuidV5(UUID namespace, String name) {
        if (namespace == null) {
            throw new IllegalArgumentException("namespace is required");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }

        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(uuidBytes(namespace));
            byte[] digest = sha1.digest(name.getBytes(StandardCharsets.UTF_8));
            digest[6] = (byte) ((digest[6] & 0x0f) | 0x50);
            digest[8] = (byte) ((digest[8] & 0x3f) | 0x80);
            ByteBuffer result = ByteBuffer.wrap(digest);
            return new UUID(result.getLong(), result.getLong());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is required by the Java runtime", exception);
        }
    }

    private static String canonicalName(String... segments) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < segments.length; index++) {
            String segment = segments[index];
            if (segment == null || segment.trim().isEmpty()) {
                throw new IllegalArgumentException("candidate key segment " + index + " is required");
            }
            if (segment.indexOf('|') >= 0) {
                throw new IllegalArgumentException("candidate key segments cannot contain '|'");
            }
            if (index > 0) {
                result.append('|');
            }
            result.append(segment);
        }
        return result.toString();
    }

    private static byte[] uuidBytes(UUID uuid) {
        return ByteBuffer.allocate(16)
            .putLong(uuid.getMostSignificantBits())
            .putLong(uuid.getLeastSignificantBits())
            .array();
    }
}
