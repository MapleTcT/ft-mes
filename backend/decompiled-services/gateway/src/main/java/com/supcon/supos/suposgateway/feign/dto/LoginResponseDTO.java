/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supos.suposgateway.feign.dto;

import com.supcon.supos.suposgateway.feign.dto.CompanyResponseDTO;
import com.supcon.supos.suposgateway.feign.dto.UserResponseDTO;
import java.util.List;

public class LoginResponseDTO {
    private String ticket;
    private UserResponseDTO user;
    private CompanyResponseDTO currentCompany;
    private List<CompanyResponseDTO> companies;
    private String tenantId;
    private String username;
    private Long userId;
    private String status;

    public String getTicket() {
        return this.ticket;
    }

    public UserResponseDTO getUser() {
        return this.user;
    }

    public CompanyResponseDTO getCurrentCompany() {
        return this.currentCompany;
    }

    public List<CompanyResponseDTO> getCompanies() {
        return this.companies;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public String getUsername() {
        return this.username;
    }

    public Long getUserId() {
        return this.userId;
    }

    public String getStatus() {
        return this.status;
    }

    public LoginResponseDTO setTicket(String ticket) {
        this.ticket = ticket;
        return this;
    }

    public LoginResponseDTO setUser(UserResponseDTO user) {
        this.user = user;
        return this;
    }

    public LoginResponseDTO setCurrentCompany(CompanyResponseDTO currentCompany) {
        this.currentCompany = currentCompany;
        return this;
    }

    public LoginResponseDTO setCompanies(List<CompanyResponseDTO> companies) {
        this.companies = companies;
        return this;
    }

    public LoginResponseDTO setTenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public LoginResponseDTO setUsername(String username) {
        this.username = username;
        return this;
    }

    public LoginResponseDTO setUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public LoginResponseDTO setStatus(String status) {
        this.status = status;
        return this;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LoginResponseDTO)) {
            return false;
        }
        LoginResponseDTO other = (LoginResponseDTO)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$ticket = this.getTicket();
        String other$ticket = other.getTicket();
        if (this$ticket == null ? other$ticket != null : !this$ticket.equals(other$ticket)) {
            return false;
        }
        UserResponseDTO this$user = this.getUser();
        UserResponseDTO other$user = other.getUser();
        if (this$user == null ? other$user != null : !((Object)this$user).equals(other$user)) {
            return false;
        }
        CompanyResponseDTO this$currentCompany = this.getCurrentCompany();
        CompanyResponseDTO other$currentCompany = other.getCurrentCompany();
        if (this$currentCompany == null ? other$currentCompany != null : !((Object)this$currentCompany).equals(other$currentCompany)) {
            return false;
        }
        List<CompanyResponseDTO> this$companies = this.getCompanies();
        List<CompanyResponseDTO> other$companies = other.getCompanies();
        if (this$companies == null ? other$companies != null : !((Object)this$companies).equals(other$companies)) {
            return false;
        }
        String this$tenantId = this.getTenantId();
        String other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !this$tenantId.equals(other$tenantId)) {
            return false;
        }
        String this$username = this.getUsername();
        String other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) {
            return false;
        }
        Long this$userId = this.getUserId();
        Long other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !((Object)this$userId).equals(other$userId)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        return !(this$status == null ? other$status != null : !this$status.equals(other$status));
    }

    protected boolean canEqual(Object other) {
        return other instanceof LoginResponseDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $ticket = this.getTicket();
        result = result * 59 + ($ticket == null ? 43 : $ticket.hashCode());
        UserResponseDTO $user = this.getUser();
        result = result * 59 + ($user == null ? 43 : ((Object)$user).hashCode());
        CompanyResponseDTO $currentCompany = this.getCurrentCompany();
        result = result * 59 + ($currentCompany == null ? 43 : ((Object)$currentCompany).hashCode());
        List<CompanyResponseDTO> $companies = this.getCompanies();
        result = result * 59 + ($companies == null ? 43 : ((Object)$companies).hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        String $username = this.getUsername();
        result = result * 59 + ($username == null ? 43 : $username.hashCode());
        Long $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : ((Object)$userId).hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        return result;
    }

    public String toString() {
        return "LoginResponseDTO(ticket=" + this.getTicket() + ", user=" + this.getUser() + ", currentCompany=" + this.getCurrentCompany() + ", companies=" + this.getCompanies() + ", tenantId=" + this.getTenantId() + ", username=" + this.getUsername() + ", userId=" + this.getUserId() + ", status=" + this.getStatus() + ")";
    }
}

