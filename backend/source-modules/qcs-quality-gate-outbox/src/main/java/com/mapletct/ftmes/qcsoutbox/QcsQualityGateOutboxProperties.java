package com.mapletct.ftmes.qcsoutbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.net.URI;

@ConfigurationProperties(prefix = "qcs.bpi.outbox")
public class QcsQualityGateOutboxProperties {

    private boolean enabled;
    private String topic = "qcs.batch.quality-gate.v1";
    private int batchSize = 50;
    private int maxAttempts = 20;
    private long claimTimeoutMs = 120_000L;
    private long baseRetryMs = 1_000L;
    private long maxRetryMs = 300_000L;
    private long sendTimeoutMs = 30_000L;
    private String bpiBaseUrl = "http://127.0.0.1:19091";
    private int bpiConnectTimeoutMs = 3_000;
    private int bpiReadTimeoutMs = 5_000;
    private String internalJwtSecret = "";
    private String internalJwtIssuer = "ft-mes-adapter";
    private String internalJwtAudience = "bpi-service";
    private String internalJwtSubject = "qcs-quality-gate-outbox";
    private long internalTokenTtlSeconds = 120L;
    private String kafkaBootstrapServers = "127.0.0.1:29092";
    private String kafkaClientId = "ft-mes-qcs-quality-gate-outbox";

    @PostConstruct
    public void validate() {
        if (!enabled) return;
        requireText(topic, "topic");
        requirePositive(batchSize, "batch-size");
        requirePositive(maxAttempts, "max-attempts");
        requirePositive(claimTimeoutMs, "claim-timeout-ms");
        requirePositive(baseRetryMs, "base-retry-ms");
        requirePositive(maxRetryMs, "max-retry-ms");
        requirePositive(sendTimeoutMs, "send-timeout-ms");
        requirePositive(bpiConnectTimeoutMs, "bpi-connect-timeout-ms");
        requirePositive(bpiReadTimeoutMs, "bpi-read-timeout-ms");
        requireText(kafkaBootstrapServers, "kafka-bootstrap-servers");
        requireText(kafkaClientId, "kafka-client-id");
        requireText(internalJwtIssuer, "internal-jwt-issuer");
        requireText(internalJwtAudience, "internal-jwt-audience");
        requireText(internalJwtSubject, "internal-jwt-subject");
        if (internalJwtSecret == null || internalJwtSecret.length() < 32) {
            throw new IllegalStateException("qcs.bpi.outbox.internal-jwt-secret must contain at least 32 characters");
        }
        if (internalTokenTtlSeconds <= 0 || internalTokenTtlSeconds > 900) {
            throw new IllegalStateException("qcs.bpi.outbox.internal-token-ttl-seconds must be between 1 and 900");
        }
        URI uri = URI.create(bpiBaseUrl);
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty() && !"/".equals(uri.getPath()))) {
            throw new IllegalStateException("qcs.bpi.outbox.bpi-base-url must be an HTTP(S) origin without path, credentials, query or fragment");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("qcs.bpi.outbox." + field + " is required");
        }
    }

    private static void requirePositive(long value, String field) {
        if (value <= 0) throw new IllegalStateException("qcs.bpi.outbox." + field + " must be positive");
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public long getClaimTimeoutMs() { return claimTimeoutMs; }
    public void setClaimTimeoutMs(long claimTimeoutMs) { this.claimTimeoutMs = claimTimeoutMs; }
    public long getBaseRetryMs() { return baseRetryMs; }
    public void setBaseRetryMs(long baseRetryMs) { this.baseRetryMs = baseRetryMs; }
    public long getMaxRetryMs() { return maxRetryMs; }
    public void setMaxRetryMs(long maxRetryMs) { this.maxRetryMs = maxRetryMs; }
    public long getSendTimeoutMs() { return sendTimeoutMs; }
    public void setSendTimeoutMs(long sendTimeoutMs) { this.sendTimeoutMs = sendTimeoutMs; }
    public String getBpiBaseUrl() { return bpiBaseUrl; }
    public void setBpiBaseUrl(String bpiBaseUrl) { this.bpiBaseUrl = bpiBaseUrl; }
    public int getBpiConnectTimeoutMs() { return bpiConnectTimeoutMs; }
    public void setBpiConnectTimeoutMs(int bpiConnectTimeoutMs) { this.bpiConnectTimeoutMs = bpiConnectTimeoutMs; }
    public int getBpiReadTimeoutMs() { return bpiReadTimeoutMs; }
    public void setBpiReadTimeoutMs(int bpiReadTimeoutMs) { this.bpiReadTimeoutMs = bpiReadTimeoutMs; }
    public String getInternalJwtSecret() { return internalJwtSecret; }
    public void setInternalJwtSecret(String internalJwtSecret) { this.internalJwtSecret = internalJwtSecret; }
    public String getInternalJwtIssuer() { return internalJwtIssuer; }
    public void setInternalJwtIssuer(String internalJwtIssuer) { this.internalJwtIssuer = internalJwtIssuer; }
    public String getInternalJwtAudience() { return internalJwtAudience; }
    public void setInternalJwtAudience(String internalJwtAudience) { this.internalJwtAudience = internalJwtAudience; }
    public String getInternalJwtSubject() { return internalJwtSubject; }
    public void setInternalJwtSubject(String internalJwtSubject) { this.internalJwtSubject = internalJwtSubject; }
    public long getInternalTokenTtlSeconds() { return internalTokenTtlSeconds; }
    public void setInternalTokenTtlSeconds(long internalTokenTtlSeconds) { this.internalTokenTtlSeconds = internalTokenTtlSeconds; }
    public String getKafkaBootstrapServers() { return kafkaBootstrapServers; }
    public void setKafkaBootstrapServers(String kafkaBootstrapServers) { this.kafkaBootstrapServers = kafkaBootstrapServers; }
    public String getKafkaClientId() { return kafkaClientId; }
    public void setKafkaClientId(String kafkaClientId) { this.kafkaClientId = kafkaClientId; }
}
