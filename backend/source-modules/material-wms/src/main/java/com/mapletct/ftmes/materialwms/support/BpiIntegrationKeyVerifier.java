package com.mapletct.ftmes.materialwms.support;

import com.mapletct.ftmes.materialwms.domain.MaterialWmsBusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

@Component
public class BpiIntegrationKeyVerifier {

    private static final String DISABLED = "_DISABLED_";

    private final String configuredKey;

    public BpiIntegrationKeyVerifier(
            @Value("${material.wms.bpi-api-key:_DISABLED_}") String configuredKey) {
        this.configuredKey = configuredKey == null ? DISABLED : configuredKey.trim();
    }

    public void verifyIfBpi(String sourceSystem, String presentedKey) {
        if (!"BPI".equals(normalized(sourceSystem))) {
            return;
        }
        if (configuredKey.isEmpty() || DISABLED.equals(configuredKey)) {
            throw new MaterialWmsBusinessException(403, "BPI 完工入库接口未启用");
        }
        byte[] expected = configuredKey.getBytes(StandardCharsets.UTF_8);
        byte[] actual = presentedKey == null
            ? new byte[0] : presentedKey.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new MaterialWmsBusinessException(403, "BPI 完工入库接口认证失败");
        }
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
