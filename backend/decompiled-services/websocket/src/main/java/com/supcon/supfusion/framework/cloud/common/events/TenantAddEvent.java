/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.ApplicationEvent
 */
package com.supcon.supfusion.framework.cloud.common.events;

import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import org.springframework.context.ApplicationEvent;

public class TenantAddEvent
extends ApplicationEvent {
    private static final long serialVersionUID = -1622617673758294213L;
    private final TenantInfo tenant;

    public TenantAddEvent(Object source, TenantInfo tenant) {
        super(source);
        this.tenant = tenant;
    }

    public TenantInfo getTenant() {
        return this.tenant;
    }
}

