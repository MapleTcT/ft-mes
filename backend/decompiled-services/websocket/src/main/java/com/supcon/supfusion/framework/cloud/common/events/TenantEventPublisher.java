/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.ApplicationEvent
 *  org.springframework.context.ApplicationEventPublisher
 *  org.springframework.context.ApplicationEventPublisherAware
 *  org.springframework.lang.NonNull
 */
package com.supcon.supfusion.framework.cloud.common.events;

import com.supcon.supfusion.framework.cloud.common.events.TenantAddEvent;
import com.supcon.supfusion.framework.cloud.common.events.TenantDestroyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.lang.NonNull;

public class TenantEventPublisher
implements ApplicationEventPublisherAware {
    private ApplicationEventPublisher applicationEventPublisher;

    public void publishAdd(@NonNull TenantAddEvent event) {
        this.applicationEventPublisher.publishEvent((ApplicationEvent)event);
    }

    public void publishDestory(@NonNull TenantDestroyEvent event) {
        this.applicationEventPublisher.publishEvent((ApplicationEvent)event);
    }

    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}

