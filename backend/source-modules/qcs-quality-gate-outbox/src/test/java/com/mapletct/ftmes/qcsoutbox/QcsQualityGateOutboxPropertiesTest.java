package com.mapletct.ftmes.qcsoutbox;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class QcsQualityGateOutboxPropertiesTest {

    @Test
    public void enabledConfigurationAcceptsAnHttpOriginAndStrongSecret() {
        QcsQualityGateOutboxProperties properties = enabledProperties();

        properties.validate();

        assertTrue(properties.isEnabled());
    }

    @Test
    public void enabledConfigurationRejectsShortJwtSecret() {
        QcsQualityGateOutboxProperties properties = enabledProperties();
        properties.setInternalJwtSecret("too-short");

        assertInvalid(properties, "at least 32 characters");
    }

    @Test
    public void enabledConfigurationRejectsBpiUrlWithAPath() {
        QcsQualityGateOutboxProperties properties = enabledProperties();
        properties.setBpiBaseUrl("http://bpi-service:19091/internal/bpi");

        assertInvalid(properties, "must be an HTTP(S) origin");
    }

    private static QcsQualityGateOutboxProperties enabledProperties() {
        QcsQualityGateOutboxProperties properties = new QcsQualityGateOutboxProperties();
        properties.setEnabled(true);
        properties.setBpiBaseUrl("http://bpi-service:19091");
        properties.setInternalJwtSecret("qcs-outbox-test-secret-0123456789");
        return properties;
    }

    private static void assertInvalid(QcsQualityGateOutboxProperties properties, String messageFragment) {
        try {
            properties.validate();
            fail("Expected invalid QCS outbox configuration");
        } catch (IllegalStateException error) {
            assertTrue(error.getMessage().contains(messageFragment));
        }
    }
}
