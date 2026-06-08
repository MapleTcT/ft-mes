/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.thread;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentTreeMap<K, V>
extends AbstractMap<K, V>
implements Map<K, V> {
    private final TreeMap<K, V> map;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock rLock = this.lock.readLock();
    private final Lock wLock = this.lock.writeLock();

    public ConcurrentTreeMap() {
        this.map = new TreeMap();
    }

    public ConcurrentTreeMap(Comparator<? super K> comparator) {
        this.map = new TreeMap(comparator);
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return this.map.entrySet();
    }

    @Override
    public V get(Object key) {
        this.rLock.lock();
        try {
            V v = this.map.get(key);
            return v;
        }
        finally {
            this.rLock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public V put(K key, V value) {
        this.wLock.lock();
        try {
            V v = this.map.put(key, value);
            return v;
        }
        finally {
            this.wLock.unlock();
        }
    }

    @Override
    public void clear() {
        this.wLock.lock();
        try {
            this.map.clear();
        }
        finally {
            this.wLock.unlock();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        this.rLock.lock();
        try {
            boolean bl = this.map.containsKey(key);
            return bl;
        }
        finally {
            this.rLock.unlock();
        }
    }

    public K firstKey() {
        this.rLock.lock();
        try {
            K k = this.map.firstKey();
            return k;
        }
        finally {
            this.rLock.unlock();
        }
    }

    public K lastKey() {
        this.rLock.lock();
        try {
            K k = this.map.lastKey();
            return k;
        }
        finally {
            this.rLock.unlock();
        }
    }

    public SortedMap<K, V> tailMap(K fromKey) {
        this.rLock.lock();
        try {
            SortedMap<K, V> sortedMap = this.map.tailMap(fromKey);
            return sortedMap;
        }
        finally {
            this.rLock.unlock();
        }
    }
}

