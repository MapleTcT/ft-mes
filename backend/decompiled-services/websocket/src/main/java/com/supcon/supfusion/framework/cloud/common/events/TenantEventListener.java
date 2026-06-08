/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.events;

import com.supcon.supfusion.framework.cloud.common.events.TenantAddEvent;
import com.supcon.supfusion.framework.cloud.common.events.TenantDestroyEvent;

public interface TenantEventListener {
    public static final int MIN_ORDER = 0;

    public void onAdd(TenantAddEvent var1);

    public void onDestroy(TenantDestroyEvent var1);

    public int order();
}

