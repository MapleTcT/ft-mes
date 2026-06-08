package com.supcon.supfusion.rbac.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public enum PermissionErrorEnum implements ErrorDefinition {

    PERMISSION_FRESH_ERROR(100105600, "权限刷新失败！","rbac.PERMISSION_FRESH_ERROR"),
    ASSIGN_ERROR(100105601, "请指定操作的岗位/部门/人员！","rbac.ASSIGN_ERROR"),
    SET_MENU_I18N_ERROR(100105602, "设置菜单的国际化值失败", "rbac.SET_MENU_I18N_ERROR")
    ;

    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

    private String key;

    PermissionErrorEnum(Integer code, String defaultMessage, String key) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.key = key;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return defaultMessage;
    }

    @Override
    public String getInfo() {
        return key;
    }

    @Override
    public String getSimpleMessage() {
        return defaultMessage;
    }
}
