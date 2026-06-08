/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.supcon.orchid.foundation.services.CompanyService
 *  com.supcon.orchid.foundation.services.CookieService
 *  com.supcon.orchid.foundation.services.RoleService
 *  com.supcon.orchid.foundation.services.StaffService
 *  com.supcon.orchid.foundation.services.SystemCodeService
 *  com.supcon.orchid.foundation.services.UserService
 *  com.supcon.orchid.i18n.InternationalResource
 *  com.supcon.orchid.orm.entities.ICompany
 *  com.supcon.orchid.orm.entities.IStaff
 *  com.supcon.orchid.orm.entities.IUser
 *  com.supcon.orchid.services.IUserFieldPermissionService
 *  com.supcon.orchid.workflow.engine.entities.WorkFlowVar
 *  com.supcon.orchid.workflow.engine.services.TaskService
 *  com.supcon.supfusion.framework.cloud.common.context.RpcContext
 *  com.supcon.supfusion.framework.cloud.common.context.UserContext
 *  javax.servlet.http.HttpServletRequest
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.util.Assert
 */
package com.supcon.orchid.entityconf.controller;

import com.supcon.orchid.foundation.services.CompanyService;
import com.supcon.orchid.foundation.services.CookieService;
import com.supcon.orchid.foundation.services.RoleService;
import com.supcon.orchid.foundation.services.StaffService;
import com.supcon.orchid.foundation.services.SystemCodeService;
import com.supcon.orchid.foundation.services.UserService;
import com.supcon.orchid.i18n.InternationalResource;
import com.supcon.orchid.orm.entities.ICompany;
import com.supcon.orchid.orm.entities.IStaff;
import com.supcon.orchid.orm.entities.IUser;
import com.supcon.orchid.services.IUserFieldPermissionService;
import com.supcon.orchid.workflow.engine.entities.WorkFlowVar;
import com.supcon.orchid.workflow.engine.services.TaskService;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

public abstract class AbstractBaseControllerSupport {
    String activityName;
    Boolean superEdit;
    Long deploymentId;
    Long pendingId;
    WorkFlowVar workFlowVar;
    @Autowired
    HttpServletRequest request;
    @Autowired
    UserService userService;
    @Autowired
    StaffService staffService;
    @Autowired
    CompanyService companyService;
    @Autowired
    RoleService roleService;
    @Autowired
    CookieService cookieService;
    @Autowired
    TaskService taskService;
    @Autowired
    SystemCodeService systemCodeService;
    @Autowired
    private IUserFieldPermissionService userFieldPermissionService;

    public int findFieldPermission(String modelCode, String propertyKey) {
        return this.userFieldPermissionService.findFieldPermission(modelCode, propertyKey, null);
    }

    public Locale getLocale() {
        String[] arr = this.getLang().split("_");
        return new Locale(arr[0], arr[1].toUpperCase());
    }

    public String getLang() {
        return this.getUserLanguage().toLowerCase();
    }

    public String getUserLanguage() {
        IUser user = this.getCurrentUser();
        if (user != null && user.getLanguage() != null) {
            return this.getCurrentUser().getLanguage();
        }
        return InternationalResource.getDefaultLanguage();
    }

    public IUser getCurrentUser() {
        UserContext userContext = this.getUserContext();
        return this.userService.getIUserByUserContext(userContext.getUserId(), userContext.getUserName(), this.getCurrentLanguage());
    }

    public IStaff getCurrentStaff() {
        UserContext userContext = this.getUserContext();
        return this.staffService.getIStaffByUserContext(userContext.getStaffId(), userContext.getStaffName(), userContext.getStaffCode());
    }

    public ICompany getCurrentCompany() {
        UserContext userContext = this.getUserContext();
        return this.companyService.getICompanyByUserContext(userContext.getCompanyId(), userContext.getCompanyCode(), userContext.getCompanyName(), userContext.getCompanyType());
    }

    public Long getCurrentCompanyId() {
        ICompany iCompany = this.getCurrentCompany();
        if (null != iCompany) {
            return (Long)this.getCurrentCompany().getId();
        }
        return null;
    }

    public String getCurrentLanguage() {
        Locale locale = RpcContext.getContext().getLanguage();
        if (null == locale) {
            return "zh_CN";
        }
        return locale.toString();
    }

    private UserContext getUserContext() {
        Assert.notNull((Object)UserContext.getUserContext(), (String)"UserContext cannot be empty");
        UserContext userContext = UserContext.getUserContext();
        return userContext;
    }
}

