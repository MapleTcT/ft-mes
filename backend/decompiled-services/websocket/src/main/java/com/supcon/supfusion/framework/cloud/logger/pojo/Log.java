/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.logger.pojo;

import java.io.Serializable;

public class Log
implements Serializable {
    private static final long serialVersionUID = -2558805952147148895L;
    protected String id;
    protected String type;
    protected String desc;
    protected String serviceId;
    protected String serverIp;
    protected String remoteIp;
    protected String userAgent;
    protected String requestUri;
    protected String requestParam;
    protected String requestMethod;
    protected String methodClass;
    protected String methodName;
    protected String methodParams;

    public String toString() {
        return "Log(id=" + this.getId() + ", type=" + this.getType() + ", desc=" + this.getDesc() + ", serviceId=" + this.getServiceId() + ", serverIp=" + this.getServerIp() + ", remoteIp=" + this.getRemoteIp() + ", userAgent=" + this.getUserAgent() + ", requestUri=" + this.getRequestUri() + ", requestParam=" + this.getRequestParam() + ", requestMethod=" + this.getRequestMethod() + ", methodClass=" + this.getMethodClass() + ", methodName=" + this.getMethodName() + ", methodParams=" + this.getMethodParams() + ")";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Log)) {
            return false;
        }
        Log other = (Log)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$type = this.getType();
        String other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
            return false;
        }
        String this$desc = this.getDesc();
        String other$desc = other.getDesc();
        if (this$desc == null ? other$desc != null : !this$desc.equals(other$desc)) {
            return false;
        }
        String this$serviceId = this.getServiceId();
        String other$serviceId = other.getServiceId();
        if (this$serviceId == null ? other$serviceId != null : !this$serviceId.equals(other$serviceId)) {
            return false;
        }
        String this$serverIp = this.getServerIp();
        String other$serverIp = other.getServerIp();
        if (this$serverIp == null ? other$serverIp != null : !this$serverIp.equals(other$serverIp)) {
            return false;
        }
        String this$remoteIp = this.getRemoteIp();
        String other$remoteIp = other.getRemoteIp();
        if (this$remoteIp == null ? other$remoteIp != null : !this$remoteIp.equals(other$remoteIp)) {
            return false;
        }
        String this$userAgent = this.getUserAgent();
        String other$userAgent = other.getUserAgent();
        if (this$userAgent == null ? other$userAgent != null : !this$userAgent.equals(other$userAgent)) {
            return false;
        }
        String this$requestUri = this.getRequestUri();
        String other$requestUri = other.getRequestUri();
        if (this$requestUri == null ? other$requestUri != null : !this$requestUri.equals(other$requestUri)) {
            return false;
        }
        String this$requestParam = this.getRequestParam();
        String other$requestParam = other.getRequestParam();
        if (this$requestParam == null ? other$requestParam != null : !this$requestParam.equals(other$requestParam)) {
            return false;
        }
        String this$requestMethod = this.getRequestMethod();
        String other$requestMethod = other.getRequestMethod();
        if (this$requestMethod == null ? other$requestMethod != null : !this$requestMethod.equals(other$requestMethod)) {
            return false;
        }
        String this$methodClass = this.getMethodClass();
        String other$methodClass = other.getMethodClass();
        if (this$methodClass == null ? other$methodClass != null : !this$methodClass.equals(other$methodClass)) {
            return false;
        }
        String this$methodName = this.getMethodName();
        String other$methodName = other.getMethodName();
        if (this$methodName == null ? other$methodName != null : !this$methodName.equals(other$methodName)) {
            return false;
        }
        String this$methodParams = this.getMethodParams();
        String other$methodParams = other.getMethodParams();
        return !(this$methodParams == null ? other$methodParams != null : !this$methodParams.equals(other$methodParams));
    }

    protected boolean canEqual(Object other) {
        return other instanceof Log;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
        String $desc = this.getDesc();
        result = result * 59 + ($desc == null ? 43 : $desc.hashCode());
        String $serviceId = this.getServiceId();
        result = result * 59 + ($serviceId == null ? 43 : $serviceId.hashCode());
        String $serverIp = this.getServerIp();
        result = result * 59 + ($serverIp == null ? 43 : $serverIp.hashCode());
        String $remoteIp = this.getRemoteIp();
        result = result * 59 + ($remoteIp == null ? 43 : $remoteIp.hashCode());
        String $userAgent = this.getUserAgent();
        result = result * 59 + ($userAgent == null ? 43 : $userAgent.hashCode());
        String $requestUri = this.getRequestUri();
        result = result * 59 + ($requestUri == null ? 43 : $requestUri.hashCode());
        String $requestParam = this.getRequestParam();
        result = result * 59 + ($requestParam == null ? 43 : $requestParam.hashCode());
        String $requestMethod = this.getRequestMethod();
        result = result * 59 + ($requestMethod == null ? 43 : $requestMethod.hashCode());
        String $methodClass = this.getMethodClass();
        result = result * 59 + ($methodClass == null ? 43 : $methodClass.hashCode());
        String $methodName = this.getMethodName();
        result = result * 59 + ($methodName == null ? 43 : $methodName.hashCode());
        String $methodParams = this.getMethodParams();
        result = result * 59 + ($methodParams == null ? 43 : $methodParams.hashCode());
        return result;
    }

    public Log() {
    }

    public Log(String id, String type, String desc, String serviceId, String serverIp, String remoteIp, String userAgent, String requestUri, String requestParam, String requestMethod, String methodClass, String methodName, String methodParams) {
        this.id = id;
        this.type = type;
        this.desc = desc;
        this.serviceId = serviceId;
        this.serverIp = serverIp;
        this.remoteIp = remoteIp;
        this.userAgent = userAgent;
        this.requestUri = requestUri;
        this.requestParam = requestParam;
        this.requestMethod = requestMethod;
        this.methodClass = methodClass;
        this.methodName = methodName;
        this.methodParams = methodParams;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDesc() {
        return this.desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getServiceId() {
        return this.serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServerIp() {
        return this.serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public String getRemoteIp() {
        return this.remoteIp;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    public String getUserAgent() {
        return this.userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getRequestUri() {
        return this.requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public String getRequestParam() {
        return this.requestParam;
    }

    public void setRequestParam(String requestParam) {
        this.requestParam = requestParam;
    }

    public String getRequestMethod() {
        return this.requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getMethodClass() {
        return this.methodClass;
    }

    public void setMethodClass(String methodClass) {
        this.methodClass = methodClass;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodParams() {
        return this.methodParams;
    }

    public void setMethodParams(String methodParams) {
        this.methodParams = methodParams;
    }
}

