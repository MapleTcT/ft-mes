/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.InitializingBean
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Component
 */
package com.supcon.supos.suposgateway.utils;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SnowFlake
implements InitializingBean {
    private final long epochSeconds = 1565020800L;
    private long sequenceBits = 21L;
    private long datacenterIdBits = 5L;
    private long workerIdBits = 5L;
    private final long maxDatacenterId = -1L << (int)this.datacenterIdBits ^ 0xFFFFFFFFFFFFFFFFL;
    private final long maxWorkerId = -1L << (int)this.workerIdBits ^ 0xFFFFFFFFFFFFFFFFL;
    private final long maxSequence = -1L << (int)this.sequenceBits ^ 0xFFFFFFFFFFFFFFFFL;
    private final long maxDeltaSeconds = -1L << (int)(63L - this.datacenterIdBits - this.workerIdBits - this.sequenceBits) ^ 0xFFFFFFFFFFFFFFFFL;
    private final long workerIdShift = this.sequenceBits;
    private final long datacenterIdShift = this.sequenceBits + this.workerIdBits;
    private final long timestampShift = this.datacenterIdShift + this.datacenterIdBits;
    @Value(value="${snow-flake.datacenterId:0}")
    private long datacenterId;
    @Value(value="${snow-flake.workerId:0}")
    private long workerId;
    private long sequence = 0L;
    private long lastSecond = -1L;

    public void afterPropertiesSet() {
        this.checkInitParams();
    }

    private void checkInitParams() {
        if (this.datacenterId > this.maxDatacenterId || this.datacenterId < 0L) {
            throw new IllegalArgumentException("'datacenterId' can't be greater than " + this.maxDatacenterId + " or less than 0");
        }
        if (this.workerId > this.maxWorkerId || this.workerId < 0L) {
            throw new IllegalArgumentException("'workerId' can't be greater than " + this.maxWorkerId + " or less than 0");
        }
    }

    public synchronized long getId() {
        long currentSecond = this.getCurrentSecond();
        if (currentSecond < this.lastSecond) {
            long offset = this.lastSecond - currentSecond;
            if (offset > 1L) {
                throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate id for %d seconds", offset));
            }
            try {
                this.wait(offset << 1);
                currentSecond = this.getCurrentSecond();
                if (currentSecond < this.lastSecond) {
                    throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate id for %d seconds", offset));
                }
            }
            catch (Exception var6) {
                throw new RuntimeException(var6);
            }
        }
        if (currentSecond == this.lastSecond) {
            this.sequence = this.sequence + 1L & this.maxSequence;
            if (this.sequence == 0L) {
                currentSecond = this.getNextSecond();
            }
        } else {
            this.sequence = 0L;
        }
        this.lastSecond = currentSecond;
        return currentSecond - 1565020800L << (int)this.timestampShift | this.datacenterId << (int)this.datacenterIdShift | this.workerId << (int)this.workerIdShift | this.sequence;
    }

    private long getNextSecond() {
        long currentSecond;
        while ((currentSecond = this.getCurrentSecond()) <= this.lastSecond) {
        }
        return currentSecond;
    }

    private long getCurrentSecond() {
        long currentSecond = TimeUnit.MILLISECONDS.toSeconds(this.getCurrentTimestamp());
        if (currentSecond - 1565020800L > this.maxDeltaSeconds) {
            throw new RuntimeException(String.format("Timestamp bits is exhausted. Refusing to generate id for %d seconds", currentSecond));
        }
        return currentSecond;
    }

    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
}

