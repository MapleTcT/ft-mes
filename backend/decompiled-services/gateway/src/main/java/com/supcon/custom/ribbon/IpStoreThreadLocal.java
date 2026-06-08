/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.custom.ribbon;

public class IpStoreThreadLocal {
    private static final ThreadLocal<String> ipStore = new ThreadLocal();

    public static String getCurrentRemoteIp() {
        return ipStore.get();
    }

    public static void storeCurrentRemoteIp(String remoteIp) {
        ipStore.set(remoteIp);
    }

    public static void resetRemoteIp() {
        ipStore.remove();
    }
}

