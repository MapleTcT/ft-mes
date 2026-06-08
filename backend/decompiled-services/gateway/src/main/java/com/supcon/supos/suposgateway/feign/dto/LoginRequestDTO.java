/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supos.suposgateway.feign.dto;

public class LoginRequestDTO {
    private String userName;
    private String password;
    private Long companyId;

    public String getUserName() {
        return this.userName;
    }

    public String getPassword() {
        return this.password;
    }

    public Long getCompanyId() {
        return this.companyId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LoginRequestDTO)) {
            return false;
        }
        LoginRequestDTO other = (LoginRequestDTO)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$userName = this.getUserName();
        String other$userName = other.getUserName();
        if (this$userName == null ? other$userName != null : !this$userName.equals(other$userName)) {
            return false;
        }
        String this$password = this.getPassword();
        String other$password = other.getPassword();
        if (this$password == null ? other$password != null : !this$password.equals(other$password)) {
            return false;
        }
        Long this$companyId = this.getCompanyId();
        Long other$companyId = other.getCompanyId();
        return !(this$companyId == null ? other$companyId != null : !((Object)this$companyId).equals(other$companyId));
    }

    protected boolean canEqual(Object other) {
        return other instanceof LoginRequestDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $userName = this.getUserName();
        result = result * 59 + ($userName == null ? 43 : $userName.hashCode());
        String $password = this.getPassword();
        result = result * 59 + ($password == null ? 43 : $password.hashCode());
        Long $companyId = this.getCompanyId();
        result = result * 59 + ($companyId == null ? 43 : ((Object)$companyId).hashCode());
        return result;
    }

    public String toString() {
        return "LoginRequestDTO(userName=" + this.getUserName() + ", password=" + this.getPassword() + ", companyId=" + this.getCompanyId() + ")";
    }
}

