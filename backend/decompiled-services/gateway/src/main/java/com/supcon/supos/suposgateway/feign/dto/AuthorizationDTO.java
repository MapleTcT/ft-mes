/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.fastjson.annotation.JSONField
 */
package com.supcon.supos.suposgateway.feign.dto;

import com.alibaba.fastjson.annotation.JSONField;

public class AuthorizationDTO {
    @JSONField(name="access_token")
    private String accessToken;
    @JSONField(name="refresh_token")
    private String refreshToken;
    @JSONField(name="token_type")
    private String tokenType;
    @JSONField(name="expires_in")
    private Integer expiresIn;
    @JSONField(name="refresh_expires_in")
    private Integer refreshExpiresIn;
    private Long companyId;
    private String errorJSON;

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public String getTokenType() {
        return this.tokenType;
    }

    public Integer getExpiresIn() {
        return this.expiresIn;
    }

    public Integer getRefreshExpiresIn() {
        return this.refreshExpiresIn;
    }

    public Long getCompanyId() {
        return this.companyId;
    }

    public String getErrorJSON() {
        return this.errorJSON;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public void setRefreshExpiresIn(Integer refreshExpiresIn) {
        this.refreshExpiresIn = refreshExpiresIn;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public void setErrorJSON(String errorJSON) {
        this.errorJSON = errorJSON;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AuthorizationDTO)) {
            return false;
        }
        AuthorizationDTO other = (AuthorizationDTO)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$accessToken = this.getAccessToken();
        String other$accessToken = other.getAccessToken();
        if (this$accessToken == null ? other$accessToken != null : !this$accessToken.equals(other$accessToken)) {
            return false;
        }
        String this$refreshToken = this.getRefreshToken();
        String other$refreshToken = other.getRefreshToken();
        if (this$refreshToken == null ? other$refreshToken != null : !this$refreshToken.equals(other$refreshToken)) {
            return false;
        }
        String this$tokenType = this.getTokenType();
        String other$tokenType = other.getTokenType();
        if (this$tokenType == null ? other$tokenType != null : !this$tokenType.equals(other$tokenType)) {
            return false;
        }
        Integer this$expiresIn = this.getExpiresIn();
        Integer other$expiresIn = other.getExpiresIn();
        if (this$expiresIn == null ? other$expiresIn != null : !((Object)this$expiresIn).equals(other$expiresIn)) {
            return false;
        }
        Integer this$refreshExpiresIn = this.getRefreshExpiresIn();
        Integer other$refreshExpiresIn = other.getRefreshExpiresIn();
        if (this$refreshExpiresIn == null ? other$refreshExpiresIn != null : !((Object)this$refreshExpiresIn).equals(other$refreshExpiresIn)) {
            return false;
        }
        Long this$companyId = this.getCompanyId();
        Long other$companyId = other.getCompanyId();
        if (this$companyId == null ? other$companyId != null : !((Object)this$companyId).equals(other$companyId)) {
            return false;
        }
        String this$errorJSON = this.getErrorJSON();
        String other$errorJSON = other.getErrorJSON();
        return !(this$errorJSON == null ? other$errorJSON != null : !this$errorJSON.equals(other$errorJSON));
    }

    protected boolean canEqual(Object other) {
        return other instanceof AuthorizationDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $accessToken = this.getAccessToken();
        result = result * 59 + ($accessToken == null ? 43 : $accessToken.hashCode());
        String $refreshToken = this.getRefreshToken();
        result = result * 59 + ($refreshToken == null ? 43 : $refreshToken.hashCode());
        String $tokenType = this.getTokenType();
        result = result * 59 + ($tokenType == null ? 43 : $tokenType.hashCode());
        Integer $expiresIn = this.getExpiresIn();
        result = result * 59 + ($expiresIn == null ? 43 : ((Object)$expiresIn).hashCode());
        Integer $refreshExpiresIn = this.getRefreshExpiresIn();
        result = result * 59 + ($refreshExpiresIn == null ? 43 : ((Object)$refreshExpiresIn).hashCode());
        Long $companyId = this.getCompanyId();
        result = result * 59 + ($companyId == null ? 43 : ((Object)$companyId).hashCode());
        String $errorJSON = this.getErrorJSON();
        result = result * 59 + ($errorJSON == null ? 43 : $errorJSON.hashCode());
        return result;
    }

    public String toString() {
        return "AuthorizationDTO(accessToken=" + this.getAccessToken() + ", refreshToken=" + this.getRefreshToken() + ", tokenType=" + this.getTokenType() + ", expiresIn=" + this.getExpiresIn() + ", refreshExpiresIn=" + this.getRefreshExpiresIn() + ", companyId=" + this.getCompanyId() + ", errorJSON=" + this.getErrorJSON() + ")";
    }
}

