/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.service;

import java.util.HashSet;
import java.util.Set;

public class ServiceInstanceStorage {
    private static Set<String> SERVICE_IDS = new HashSet<String>();

    public static synchronized void add(String serviceId) {
        SERVICE_IDS.add(serviceId);
    }

    public static synchronized void remove(String serviceId) {
        SERVICE_IDS.remove(serviceId);
    }

    public static Set<String> get() {
        return new HashSet<String>(SERVICE_IDS);
    }
}

