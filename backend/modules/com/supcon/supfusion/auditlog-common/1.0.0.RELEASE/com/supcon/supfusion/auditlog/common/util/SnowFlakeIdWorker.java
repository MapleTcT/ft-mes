package com.supcon.supfusion.auditlog.common.util;

public class SnowFlakeIdWorker {
    private long workerId;
    private long datacenterId;
    private long sequence;
    private long twepoch = 1597075200000L;
    private long workerIdBits = 2L;
    private long datacenterIdBits = 2L;
    private long sequenceBits = 8L;
    private long maxWorkerId;
    private long maxDatacenterId;
    private long workerIdShift;
    private long datacenterIdShift;
    private long timestampLeftShift;
    private long sequenceMask;
    private long lastTimestamp;
    private static SnowFlakeIdWorker snowFlakeIdWorker;
    private static final Object syncLock = new Object();

    public long getWorkerId() {
        return this.workerId;
    }

    public long getDatacenterId() {
        return this.datacenterId;
    }

    public long getTimestamp() {
        return System.currentTimeMillis();
    }

    public static SnowFlakeIdWorker getInstance() {
        if (null == snowFlakeIdWorker) {
            synchronized(syncLock) {
                if (null == snowFlakeIdWorker) {
                    long workerId = 1L;
                    long datacenterId = 1L;

                    try {
                        workerId = 1L;
                    } catch (Exception var7) {
                    }

                    snowFlakeIdWorker = new SnowFlakeIdWorker(workerId, datacenterId, 0L);
                }
            }
        }

        return snowFlakeIdWorker;
    }

    public SnowFlakeIdWorker(long workerId, long datacenterId, long sequence) {
        this.maxWorkerId = ~(-1L << (int)this.workerIdBits);
        this.maxDatacenterId = ~(-1L << (int)this.datacenterIdBits);
        this.workerIdShift = this.sequenceBits;
        this.datacenterIdShift = this.sequenceBits + this.workerIdBits;
        this.timestampLeftShift = this.sequenceBits + this.workerIdBits + this.datacenterIdBits;
        this.sequenceMask = ~(-1L << (int)this.sequenceBits);
        this.lastTimestamp = -1L;
        if (workerId <= this.maxWorkerId && workerId >= 0L) {
            if (datacenterId <= this.maxDatacenterId && datacenterId >= 0L) {
                this.workerId = workerId;
                this.datacenterId = datacenterId;
                this.sequence = sequence;
            } else {
                throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", this.maxDatacenterId));
            }
        } else {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", this.maxWorkerId));
        }
    }

    public synchronized long nextId() {
        long timestamp = this.timeGen();
        if (timestamp < this.lastTimestamp) {
            System.err.printf("clock is moving backwards. Rejecting requests until %d.", this.lastTimestamp);
            throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", this.lastTimestamp - timestamp));
        } else {
            if (this.lastTimestamp == timestamp) {
                this.sequence = this.sequence + 1L & this.sequenceMask;
                if (this.sequence == 0L) {
                    timestamp = this.tilNextMillis(this.lastTimestamp);
                }
            } else {
                this.sequence = 0L;
            }

            this.lastTimestamp = timestamp;
            return timestamp - this.twepoch << (int)this.timestampLeftShift | this.datacenterId << (int)this.datacenterIdShift | this.workerId << (int)this.workerIdShift | this.sequence;
        }
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp;
        for(timestamp = this.timeGen(); timestamp <= lastTimestamp; timestamp = this.timeGen()) {
        }

        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    public static void main(String[] args) {
        SnowFlakeIdWorker idWorker = new SnowFlakeIdWorker(1L, 1L, 0L);

        for(int i = 0; i < 1000; ++i) {
            long id = idWorker.nextId();
            System.out.println(Long.toBinaryString(id));
            System.out.println(id);
        }

    }
}

