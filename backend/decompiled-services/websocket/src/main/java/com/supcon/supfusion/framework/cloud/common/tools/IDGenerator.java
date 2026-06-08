/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.tools;

import com.supcon.supfusion.framework.cloud.common.util.InetUtils;
import java.net.UnknownHostException;
import java.util.Calendar;

public class IDGenerator {
    private final SnowFlakeIDGenerator snowFlakeIDGenerator;
    private static Object lock = new Object();
    private static volatile IDGenerator instance = null;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static IDGenerator newInstance() {
        if (instance == null) {
            Object object = lock;
            synchronized (object) {
                if (instance == null) {
                    instance = new IDGenerator();
                }
            }
        }
        return instance;
    }

    public IDGenerator() {
        long workerId = 1L;
        try {
            workerId = InetUtils.getAddressHashCode();
        }
        catch (UnknownHostException e) {
            throw new IllegalArgumentException("fail to get worker id by local host address", e);
        }
        this.snowFlakeIDGenerator = new SnowFlakeIDGenerator(workerId, 1L);
    }

    public IDGenerator(long centerId, long workerId) {
        this.snowFlakeIDGenerator = new SnowFlakeIDGenerator(workerId, centerId);
    }

    public Number generate() {
        return this.snowFlakeIDGenerator.generateKey();
    }

    private static final class SnowFlakeIDGenerator {
        private static final Long centerIdBits = 1L;
        private static final Long maxCenterId = -1L << (int)centerIdBits.longValue() ^ 0xFFFFFFFFFFFFFFFFL;
        private static final Long workerIdBits = 10L;
        private static final Long maxWorkerId = -1L << (int)workerIdBits.longValue() ^ 0xFFFFFFFFFFFFFFFFL;
        private static final Long sequenceBits = 4L;
        private static final Long maxSequence = -1L << (int)sequenceBits.longValue() ^ 0xFFFFFFFFFFFFFFFFL;
        private static final Long workderIdShift = sequenceBits;
        private static final Long centerIdShift = sequenceBits + workerIdBits;
        private static final Long timestampLeftShift = sequenceBits + workerIdBits + centerIdBits;
        private Long centerId = 0L;
        private Long workId = 0L;
        private Long since = 0L;
        private volatile Long sequence = 0L;
        private volatile Long lastTimestamp = -1L;

        private SnowFlakeIDGenerator(Long workId, Long centerId) {
            if (centerId > maxCenterId || centerId < 0L) {
                throw new IllegalArgumentException("the center id must large than 0 and less than " + maxCenterId);
            }
            if (workId > maxWorkerId || workId < 0L) {
                throw new IllegalArgumentException("the workder id must large than 0 and less than " + maxWorkerId);
            }
            this.centerId = centerId;
            this.workId = workId;
            Calendar calendar = Calendar.getInstance();
            calendar.set(2020, 1, 1, 0, 0, 0);
            this.since = calendar.getTimeInMillis();
        }

        private long tilNextMillis(long lastTimestamp) {
            long timestamp = this.timeGen();
            while (timestamp <= lastTimestamp) {
                timestamp = this.timeGen();
            }
            return timestamp;
        }

        private long timeGen() {
            return System.currentTimeMillis();
        }

        public synchronized Number generateKey() {
            long timestamp = this.timeGen();
            if (timestamp < this.lastTimestamp) {
                timestamp = this.tilNextMillis(this.lastTimestamp);
            }
            if (this.lastTimestamp == timestamp) {
                this.sequence = this.sequence + 1L & maxSequence;
                if (this.sequence == 0L) {
                    timestamp = this.tilNextMillis(this.lastTimestamp);
                }
            } else {
                this.sequence = 0L;
            }
            this.lastTimestamp = timestamp;
            return timestamp - this.since << (int)timestampLeftShift.longValue() | this.centerId << (int)centerIdShift.longValue() | this.workId << (int)workderIdShift.longValue() | this.sequence;
        }
    }
}

