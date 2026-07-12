package com.mapletct.ftmes.bpi.application;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public final class UuidV5 {
    private UuidV5() {
    }

    public static UUID from(UUID namespace, String name) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            ByteBuffer namespaceBytes = ByteBuffer.allocate(16)
                    .putLong(namespace.getMostSignificantBits())
                    .putLong(namespace.getLeastSignificantBits());
            sha1.update(namespaceBytes.array());
            byte[] digest = sha1.digest(name.getBytes(StandardCharsets.UTF_8));
            digest[6] &= 0x0f;
            digest[6] |= 0x50;
            digest[8] &= 0x3f;
            digest[8] |= 0x80;
            ByteBuffer uuid = ByteBuffer.wrap(digest, 0, 16);
            return new UUID(uuid.getLong(), uuid.getLong());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is required for UUIDv5", exception);
        }
    }
}
