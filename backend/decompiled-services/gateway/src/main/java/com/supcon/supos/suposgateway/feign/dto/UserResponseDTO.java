/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supos.suposgateway.feign.dto;

public class UserResponseDTO {
    private Long id;
    private String userName;
    private Integer userType;

    public Long getId() {
        return this.id;
    }

    public String getUserName() {
        return this.userName;
    }

    public Integer getUserType() {
        return this.userType;
    }

    public UserResponseDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public UserResponseDTO setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public UserResponseDTO setUserType(Integer userType) {
        this.userType = userType;
        return this;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof UserResponseDTO)) {
            return false;
        }
        UserResponseDTO other = (UserResponseDTO)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        String this$userName = this.getUserName();
        String other$userName = other.getUserName();
        if (this$userName == null ? other$userName != null : !this$userName.equals(other$userName)) {
            return false;
        }
        Integer this$userType = this.getUserType();
        Integer other$userType = other.getUserType();
        return !(this$userType == null ? other$userType != null : !((Object)this$userType).equals(other$userType));
    }

    protected boolean canEqual(Object other) {
        return other instanceof UserResponseDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        String $userName = this.getUserName();
        result = result * 59 + ($userName == null ? 43 : $userName.hashCode());
        Integer $userType = this.getUserType();
        result = result * 59 + ($userType == null ? 43 : ((Object)$userType).hashCode());
        return result;
    }

    public String toString() {
        return "UserResponseDTO(id=" + this.getId() + ", userName=" + this.getUserName() + ", userType=" + this.getUserType() + ")";
    }
}

