/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.ApplicationEvent
 */
package com.supcon.supfusion.framework.cloud.common.events;

import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import org.springframework.context.ApplicationEvent;

public class TenantDestroyEvent
extends ApplicationEvent {
    private static final long serialVersionUID = -1075746250854405690L;
    private final TenantInfo tenant;

    public TenantDestroyEvent(Object source, TenantInfo tenant) {
        super(source);
        this.tenant = tenant;
    }

    public TenantInfo getTenant() {
        return this.tenant;
    }
}

