/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.thread;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashSet<E>
extends AbstractSet<E>
implements Set<E> {
    private static final Object VALUE = new Object();
    private ConcurrentHashMap<E, Object> concurrentHashMap;

    public ConcurrentHashSet() {
        this.concurrentHashMap = new ConcurrentHashMap();
    }

    public ConcurrentHashSet(int initialCapacity) {
        this.concurrentHashMap = new ConcurrentHashMap(initialCapacity);
    }

    @Override
    public int size() {
        return this.concurrentHashMap.size();
    }

    @Override
    public boolean isEmpty() {
        return this.concurrentHashMap.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.concurrentHashMap.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return ((ConcurrentHashMap.KeySetView)this.concurrentHashMap.keySet()).iterator();
    }

    @Override
    public E[] toArray() {
        return ((ConcurrentHashMap.CollectionView)((Object)this.concurrentHashMap.keySet())).toArray();
    }

    @Override
    public boolean add(E e) {
        return this.concurrentHashMap.put(e, VALUE) == null;
    }

    @Override
    public boolean remove(Object o) {
        return this.concurrentHashMap.remove(o) == VALUE;
    }

    @Override
    public void clear() {
        this.concurrentHashMap.clear();
    }
}

