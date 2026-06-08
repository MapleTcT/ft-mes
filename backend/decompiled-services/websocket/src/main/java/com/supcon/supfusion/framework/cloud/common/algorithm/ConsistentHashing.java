/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.util.Assert
 */
package com.supcon.supfusion.framework.cloud.common.algorithm;

import com.supcon.supfusion.framework.cloud.common.thread.ConcurrentTreeMap;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import org.springframework.util.Assert;

public final class ConsistentHashing {
    private static final String DEFAULT_VIRTUAL_DELIMETER = ":";
    private final List<Object> physicalNodes = new LinkedList<Object>();
    private final ConcurrentTreeMap<BigInteger, Node> hashRing = new ConcurrentTreeMap();
    private int virtualNodeCount = 0;

    public ConsistentHashing() {
    }

    public ConsistentHashing(int virtualNodeCount) {
        this();
        Assert.state((virtualNodeCount >= 0 ? 1 : 0) != 0, (String)"The virtual node count must large than or equals to 0");
        this.virtualNodeCount = virtualNodeCount;
    }

    public ConsistentHashing(Object[] nodes, int virtualNodeCount) {
        this(virtualNodeCount);
        Assert.notEmpty((Object[])nodes, (String)"The array of physical node must not be empty");
        this.addNode(nodes);
    }

    public void setVirtualNodeCount(int virtualNodeCount) {
        Assert.state((virtualNodeCount >= 0 ? 1 : 0) != 0, (String)"The virtaul node count must large than or equals to 0");
        this.virtualNodeCount = virtualNodeCount;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addNode(Object node) {
        Assert.notNull((Object)node, (String)"The physical node must not be null");
        int index = 0;
        ConsistentHashing consistentHashing = this;
        synchronized (consistentHashing) {
            this.physicalNodes.add(node);
            index = this.physicalNodes.size() - 1;
        }
        Node _node = new Node(index, node);
        if (this.virtualNodeCount > 0) {
            for (int i = 0; i < this.virtualNodeCount; ++i) {
                String key = String.valueOf(node) + DEFAULT_VIRTUAL_DELIMETER + Integer.toString(i);
                BigInteger _key = this.md5Key(key);
                this.hashRing.put(_key, _node);
            }
        } else {
            String key = String.valueOf(node) + DEFAULT_VIRTUAL_DELIMETER + Integer.toString(0);
            BigInteger _key = this.md5Key(key);
            this.hashRing.put(_key, _node);
        }
    }

    public void addNodes(Object[] nodes) {
        Assert.notEmpty((Object[])nodes, (String)"The array of physical node must not be empty");
        for (Object node : nodes) {
            this.addNode(node);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void removeNode(Object node) {
        Assert.notNull((Object)node, (String)"The physical node must not be null");
        this.physicalNodes.set(this.physicalNodes.indexOf(node), null);
        HashSet keys = new HashSet();
        this.hashRing.forEach((k, v) -> {
            if (v.getValue().equals(node)) {
                keys.add(k);
            }
        });
        ConsistentHashing consistentHashing = this;
        synchronized (consistentHashing) {
            keys.stream().forEach(key -> {
                Node cfr_ignored_0 = (Node)this.hashRing.remove(key);
            });
        }
    }

    public Object get(Object node) {
        BigInteger key = this.md5Key(String.valueOf(node));
        SortedMap<BigInteger, Node> nodes = this.hashRing.tailMap(key);
        BigInteger index = nodes.isEmpty() ? this.hashRing.firstKey() : nodes.firstKey();
        Node _node = this.hashRing.get(index);
        return this.physicalNodes.get(_node.getIndex());
    }

    private BigInteger md5Key(String key) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(key.getBytes(StandardCharsets.UTF_8));
            byte[] encryptted = md5.digest();
            return new BigInteger(1, encryptted);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Can not found algorithm by named 'MD5'");
        }
    }

    private class Node {
        private int index;
        private Object value;

        Node(int index, Object value) {
            this.index = index;
            this.value = value;
        }

        public Object getValue() {
            return this.value;
        }

        public int getIndex() {
            return this.index;
        }
    }
}

