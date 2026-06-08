/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.lang.NonNull
 */
package com.supcon.supfusion.framework.cloud.common.events;

import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import java.util.Set;
import org.springframework.lang.NonNull;

public interface TenantInfoGetter {
    public TenantInfo get(String var1);

    public Set<TenantInfo> getAll();

    @NonNull
    public String name();

    public int order();
}

