/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.orchid.entityconf.services;

public interface FeignService {
    public <T> T newInstanceByUrl(Class<T> var1, String var2);

    public <T> T newInstanceByName(Class<T> var1, String var2);
}

