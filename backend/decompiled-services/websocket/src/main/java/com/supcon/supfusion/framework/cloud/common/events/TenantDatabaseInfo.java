/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.events;

import java.io.Serializable;

public class TenantDatabaseInfo
implements Serializable {
    private static final long serialVersionUID = -212983148499509936L;
    private String host;
    private int port;
    private String username;
    private String password;
    private String dbName;
    private String dbType;
    private String dbVersion;

    public static TenantDatabaseInfoBuilder builder() {
        return new TenantDatabaseInfoBuilder();
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public void setDbVersion(String dbVersion) {
        this.dbVersion = dbVersion;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getDbName() {
        return this.dbName;
    }

    public String getDbType() {
        return this.dbType;
    }

    public String getDbVersion() {
        return this.dbVersion;
    }

    public String toString() {
        return "TenantDatabaseInfo(host=" + this.getHost() + ", port=" + this.getPort() + ", username=" + this.getUsername() + ", dbName=" + this.getDbName() + ", dbType=" + this.getDbType() + ", dbVersion=" + this.getDbVersion() + ")";
    }

    public TenantDatabaseInfo() {
    }

    public TenantDatabaseInfo(String host, int port, String username, String password, String dbName, String dbType, String dbVersion) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.dbName = dbName;
        this.dbType = dbType;
        this.dbVersion = dbVersion;
    }

    public static class TenantDatabaseInfoBuilder {
        private String host;
        private int port;
        private String username;
        private String password;
        private String dbName;
        private String dbType;
        private String dbVersion;

        TenantDatabaseInfoBuilder() {
        }

        public TenantDatabaseInfoBuilder host(String host) {
            this.host = host;
            return this;
        }

        public TenantDatabaseInfoBuilder port(int port) {
            this.port = port;
            return this;
        }

        public TenantDatabaseInfoBuilder username(String username) {
            this.username = username;
            return this;
        }

        public TenantDatabaseInfoBuilder password(String password) {
            this.password = password;
            return this;
        }

        public TenantDatabaseInfoBuilder dbName(String dbName) {
            this.dbName = dbName;
            return this;
        }

        public TenantDatabaseInfoBuilder dbType(String dbType) {
            this.dbType = dbType;
            return this;
        }

        public TenantDatabaseInfoBuilder dbVersion(String dbVersion) {
            this.dbVersion = dbVersion;
            return this;
        }

        public TenantDatabaseInfo build() {
            return new TenantDatabaseInfo(this.host, this.port, this.username, this.password, this.dbName, this.dbType, this.dbVersion);
        }

        public String toString() {
            return "TenantDatabaseInfo.TenantDatabaseInfoBuilder(host=" + this.host + ", port=" + this.port + ", username=" + this.username + ", password=" + this.password + ", dbName=" + this.dbName + ", dbType=" + this.dbType + ", dbVersion=" + this.dbVersion + ")";
        }
    }
}

