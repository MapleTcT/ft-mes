package com.mapletct.ftmes.contextoutbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mes.production-context.outbox")
public class ProductionContextOutboxProperties {

    private boolean enabled;
    private String topic = "mes.production.context.v1";
    private int batchSize = 100;
    private int maxAttempts = 20;
    private long claimTimeoutMs = 120_000L;
    private long baseRetryMs = 1_000L;
    private long maxRetryMs = 300_000L;
    private long sendTimeoutMs = 30_000L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getClaimTimeoutMs() {
        return claimTimeoutMs;
    }

    public void setClaimTimeoutMs(long claimTimeoutMs) {
        this.claimTimeoutMs = claimTimeoutMs;
    }

    public long getBaseRetryMs() {
        return baseRetryMs;
    }

    public void setBaseRetryMs(long baseRetryMs) {
        this.baseRetryMs = baseRetryMs;
    }

    public long getMaxRetryMs() {
        return maxRetryMs;
    }

    public void setMaxRetryMs(long maxRetryMs) {
        this.maxRetryMs = maxRetryMs;
    }

    public long getSendTimeoutMs() {
        return sendTimeoutMs;
    }

    public void setSendTimeoutMs(long sendTimeoutMs) {
        this.sendTimeoutMs = sendTimeoutMs;
    }
}
