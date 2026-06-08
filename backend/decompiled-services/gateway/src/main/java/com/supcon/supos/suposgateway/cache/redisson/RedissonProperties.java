/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.boot.context.properties.ConfigurationProperties
 */
package com.supcon.supos.suposgateway.cache.redisson;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="spring.redis.redisson")
public class RedissonProperties {
    private int threads;
    private int nettyThreads;
    private int poolSize = 32;
    private int minIdleSize = 8;
    private int idleConnTimeout = 30000;
    private int pingConnectionInterval = 10000;
    private boolean keepAlive = true;
    private int failedSlaveCheckInterval = 180000;

    public RedissonProperties() {
        int processors = Runtime.getRuntime().availableProcessors();
        this.threads = processors * 2 + 1;
        this.nettyThreads = processors * 2 + 1;
    }

    public int getThreads() {
        return this.threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public int getNettyThreads() {
        return this.nettyThreads;
    }

    public void setNettyThreads(int nettyThreads) {
        this.nettyThreads = nettyThreads;
    }

    public int getPoolSize() {
        return this.poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getMinIdleSize() {
        return this.minIdleSize;
    }

    public void setMinIdleSize(int minIdleSize) {
        this.minIdleSize = minIdleSize;
    }

    public int getIdleConnTimeout() {
        return this.idleConnTimeout;
    }

    public void setIdleConnTimeout(int idleConnTimeout) {
        this.idleConnTimeout = idleConnTimeout;
    }

    public int getFailedSlaveCheckInterval() {
        return this.failedSlaveCheckInterval;
    }

    public void setFailedSlaveCheckInterval(int failedSlaveCheckInterval) {
        this.failedSlaveCheckInterval = failedSlaveCheckInterval;
    }

    public int getPingConnectionInterval() {
        return this.pingConnectionInterval;
    }

    public void setPingConnectionInterval(int pingConnectionInterval) {
        this.pingConnectionInterval = pingConnectionInterval;
    }

    public boolean isKeepAlive() {
        return this.keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }
}

