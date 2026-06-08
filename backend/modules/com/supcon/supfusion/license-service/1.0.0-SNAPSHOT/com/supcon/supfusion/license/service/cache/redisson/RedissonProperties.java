package com.supcon.supfusion.license.service.cache.redisson;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.redis.redisson")
public class RedissonProperties {

    private int threads;
    private int nettyThreads;

    private int poolSize = 32;
    private int minIdleSize = 8;
    private int idleConnTimeout = 30000;
    private int failedSlaveCheckInterval = 180000;

    public RedissonProperties() {
        int processors = Runtime.getRuntime().availableProcessors();
        threads = processors * 2 + 1;
        nettyThreads = processors * 2 + 1;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public int getNettyThreads() {
        return nettyThreads;
    }

    public void setNettyThreads(int nettyThreads) {
        this.nettyThreads = nettyThreads;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getMinIdleSize() {
        return minIdleSize;
    }

    public void setMinIdleSize(int minIdleSize) {
        this.minIdleSize = minIdleSize;
    }

    public int getIdleConnTimeout() {
        return idleConnTimeout;
    }

    public void setIdleConnTimeout(int idleConnTimeout) {
        this.idleConnTimeout = idleConnTimeout;
    }

    public int getFailedSlaveCheckInterval() {
        return failedSlaveCheckInterval;
    }

    public void setFailedSlaveCheckInterval(int failedSlaveCheckInterval) {
        this.failedSlaveCheckInterval = failedSlaveCheckInterval;
    }



}
