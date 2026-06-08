/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.thread;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ConcurrentTreeSet<E>
extends AbstractSet<E>
implements NavigableSet<E> {
    private final TreeSet<E> set;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock rLock = this.lock.readLock();
    private final Lock wLock = this.lock.writeLock();

    public ConcurrentTreeSet() {
        this.set = new TreeSet();
    }

    public ConcurrentTreeSet(Comparator<? super E> comparator) {
        this.set = new TreeSet<E>(comparator);
    }

    @Override
    public Iterator<E> iterator() {
        return this.set.iterator();
    }

    @Override
    public int size() {
        return this.set.size();
    }

    @Override
    public boolean add(E e) {
        this.wLock.lock();
        try {
            boolean bl = this.set.add(e);
            return bl;
        }
        finally {
            this.wLock.unlock();
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        this.wLock.lock();
        try {
            boolean bl = this.set.addAll(c);
            return bl;
        }
        finally {
            this.wLock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        this.rLock.lock();
        try {
            boolean bl = this.set.contains(o);
            return bl;
        }
        finally {
            this.rLock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        return this.set.isEmpty();
    }

    @Override
    public boolean remove(Object o) {
        this.wLock.lock();
        try {
            boolean bl = this.set.remove(o);
            return bl;
        }
        finally {
            this.wLock.unlock();
        }
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        this.wLock.lock();
        try {
            boolean bl = this.set.removeIf(filter);
            return bl;
        }
        finally {
            this.wLock.unlock();
        }
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        this.rLock.lock();
        try {
            this.set.forEach(action);
        }
        finally {
            this.rLock.unlock();
        }
    }

    @Override
    public void clear() {
        this.wLock.lock();
        try {
            this.set.clear();
        }
        finally {
            this.wLock.unlock();
        }
    }

    @Override
    public Comparator<? super E> comparator() {
        return this.set.comparator();
    }

    @Override
    public E first() {
        this.rLock.lock();
        try {
            E e = this.set.first();
            return e;
        }
        finally {
            this.rLock.unlock();
        }
    }

    @Override
    public E last() {
        this.rLock.lock();
        try {
            E e = this.set.last();
            return e;
        }
        finally {
            this.rLock.unlock();
        }
    }

    @Override
    public E lower(E e) {
        this.rLock.lock();
        try {
            E e2 = this.set.lower(e);
            return e2;
        }
        finally {
            this.rLock.unlock();
        }
    }

    @Override
    public E floor(E e) {
        this.rLock.lock();
        try {
            E e2 = this.set.floor(e);
            return e2;
        }
        finally {
            this.rLock.unlock();
        }
    }

    @Override
    public E ceiling(E e) {
        this.rLock.lock();
        try {
            E e2 = this.set.ceiling(e);
            return e2;
        }
        finally {
            this.rLock.unlock();
        }
    }

    @Override
    public E higher(E e) {
        this.rLock.lock();
        try {
            E e2 = this.set.higher(e);
            return e2;
        }
        finally {
            this.rLock.unlock();
        }
    }

    @Override
    public E pollFirst() {
        this.rLock.lock();
        try {
            E e = this.set.pollFirst();
            return e;
        }
        finally {
            this.rLock.unlock();
        }
    }

    @Override
    public E pollLast() {
        this.rLock.lock();
        try {
            E e = this.set.pollLast();
            return e;
        }
        finally {
            this.rLock.unlock();
        }
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return this.set.descendingSet();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return this.set.descendingIterator();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        this.rLock.lock();
        try {
            NavigableSet<E> navigableSet = this.set.subSet(fromElement, fromInclusive, toElement, toInclusive);
            return navigableSet;
        }
        finally {
            this.rLock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        this.rLock.lock();
        try {
            NavigableSet<E> navigableSet = this.set.headSet(toElement, inclusive);
            return navigableSet;
        }
        finally {
            this.rLock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        this.rLock.lock();
        try {
            NavigableSet<E> navigableSet = this.set.tailSet(fromElement, inclusive);
            return navigableSet;
        }
        finally {
            this.rLock.unlock();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        this.rLock.lock();
        try {
            SortedSet<E> sortedSet = this.set.subSet(fromElement, toElement);
            return sortedSet;
        }
        finally {
            this.rLock.unlock();
        }
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        this.rLock.lock();
        try {
            SortedSet<E> sortedSet = this.set.headSet(toElement);
            return sortedSet;
        }
        finally {
            this.rLock.unlock();
        }
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        this.rLock.lock();
        try {
            SortedSet<E> sortedSet = this.set.tailSet(fromElement);
            return sortedSet;
        }
        finally {
            this.rLock.unlock();
        }
    }
}

