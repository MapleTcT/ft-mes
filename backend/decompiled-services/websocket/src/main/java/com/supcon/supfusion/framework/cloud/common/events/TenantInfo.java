/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.events;

import com.supcon.supfusion.framework.cloud.common.events.TenantDatabaseInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantEventTypeEnum;
import java.io.Serializable;

public class TenantInfo
implements Serializable {
    private static final long serialVersionUID = -4031512980962159802L;
    public static final String topicName = "supOS_tenant";
    private TenantEventTypeEnum eventType;
    private String id;
    private Long instanceId;
    private String description;
    private TenantDatabaseInfo databaseInfo;

    public int hashCode() {
        return this.id.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof TenantInfo) {
            return ((TenantInfo)obj).getId().equals(this.id);
        }
        return false;
    }

    public static TenantInfoBuilder builder() {
        return new TenantInfoBuilder();
    }

    public void setEventType(TenantEventTypeEnum eventType) {
        this.eventType = eventType;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDatabaseInfo(TenantDatabaseInfo databaseInfo) {
        this.databaseInfo = databaseInfo;
    }

    public TenantEventTypeEnum getEventType() {
        return this.eventType;
    }

    public String getId() {
        return this.id;
    }

    public Long getInstanceId() {
        return this.instanceId;
    }

    public String getDescription() {
        return this.description;
    }

    public TenantDatabaseInfo getDatabaseInfo() {
        return this.databaseInfo;
    }

    public String toString() {
        return "TenantInfo(eventType=" + (Object)((Object)this.getEventType()) + ", id=" + this.getId() + ", instanceId=" + this.getInstanceId() + ", description=" + this.getDescription() + ", databaseInfo=" + this.getDatabaseInfo() + ")";
    }

    public TenantInfo() {
    }

    public TenantInfo(TenantEventTypeEnum eventType, String id, Long instanceId, String description, TenantDatabaseInfo databaseInfo) {
        this.eventType = eventType;
        this.id = id;
        this.instanceId = instanceId;
        this.description = description;
        this.databaseInfo = databaseInfo;
    }

    public static class TenantInfoBuilder {
        private TenantEventTypeEnum eventType;
        private String id;
        private Long instanceId;
        private String description;
        private TenantDatabaseInfo databaseInfo;

        TenantInfoBuilder() {
        }

        public TenantInfoBuilder eventType(TenantEventTypeEnum eventType) {
            this.eventType = eventType;
            return this;
        }

        public TenantInfoBuilder id(String id) {
            this.id = id;
            return this;
        }

        public TenantInfoBuilder instanceId(Long instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public TenantInfoBuilder description(String description) {
            this.description = description;
            return this;
        }

        public TenantInfoBuilder databaseInfo(TenantDatabaseInfo databaseInfo) {
            this.databaseInfo = databaseInfo;
            return this;
        }

        public TenantInfo build() {
            return new TenantInfo(this.eventType, this.id, this.instanceId, this.description, this.databaseInfo);
        }

        public String toString() {
            return "TenantInfo.TenantInfoBuilder(eventType=" + (Object)((Object)this.eventType) + ", id=" + this.id + ", instanceId=" + this.instanceId + ", description=" + this.description + ", databaseInfo=" + this.databaseInfo + ")";
        }
    }
}

