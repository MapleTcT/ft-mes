/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.ttl.TransmittableThreadLocal
 *  javax.validation.constraints.NotBlank
 *  javax.validation.constraints.NotNull
 */
package com.supcon.supfusion.framework.cloud.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class UserContext
implements Serializable {
    private static final long serialVersionUID = 4820190224354434039L;
    private static final ThreadLocal<UserContext> USER_CONTEXT_THREAD_LOCAL = new TransmittableThreadLocal<UserContext>(){

        protected UserContext initialValue() {
            return new UserContext();
        }
    };
    @NotNull
    private Long userId;
    @NotBlank
    private String userName;
    @NotNull
    private Long staffId;
    @NotBlank
    private String staffName;
    @NotBlank
    private String staffCode;
    @NotNull
    private Long positionId;
    @NotBlank
    private String positionName;
    @NotBlank
    private String positionCode;
    @NotNull
    private Long positionCompanyId;
    @NotNull
    private Long departmentId;
    @NotBlank
    private String departmentName;
    @NotBlank
    private String departmentCode;
    @NotNull
    private Long companyId;
    @NotNull
    private Integer userType;
    @NotBlank
    private String companyName;
    @NotBlank
    private String companyCode;
    @NotBlank
    private String companyType;

    public static UserContext getUserContext() {
        return USER_CONTEXT_THREAD_LOCAL.get();
    }

    public static void removeUserContext() {
        USER_CONTEXT_THREAD_LOCAL.remove();
    }

    private UserContext() {
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public void setStaffCode(String staffCode) {
        this.staffCode = staffCode;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public void setPositionCode(String positionCode) {
        this.positionCode = positionCode;
    }

    public void setPositionCompanyId(Long positionCompanyId) {
        this.positionCompanyId = positionCompanyId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public void setUserType(Integer userType) {
        this.userType = userType;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public void setCompanyType(String companyType) {
        this.companyType = companyType;
    }

    public Long getUserId() {
        return this.userId;
    }

    public String getUserName() {
        return this.userName;
    }

    public Long getStaffId() {
        return this.staffId;
    }

    public String getStaffName() {
        return this.staffName;
    }

    public String getStaffCode() {
        return this.staffCode;
    }

    public Long getPositionId() {
        return this.positionId;
    }

    public String getPositionName() {
        return this.positionName;
    }

    public String getPositionCode() {
        return this.positionCode;
    }

    public Long getPositionCompanyId() {
        return this.positionCompanyId;
    }

    public Long getDepartmentId() {
        return this.departmentId;
    }

    public String getDepartmentName() {
        return this.departmentName;
    }

    public String getDepartmentCode() {
        return this.departmentCode;
    }

    public Long getCompanyId() {
        return this.companyId;
    }

    public Integer getUserType() {
        return this.userType;
    }

    public String getCompanyName() {
        return this.companyName;
    }

    public String getCompanyCode() {
        return this.companyCode;
    }

    public String getCompanyType() {
        return this.companyType;
    }
}

