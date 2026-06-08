package com.supcon.supfusion.auth.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public enum DashboardConfigErrorEnum implements ErrorDefinition {

    DASHBOARDCONFIG_EXIST(1001060034, "配置存在", "userManager.dashBordExist");
    /**
     * 异常码
     */
    private Integer code;

    /**
     * 默认异常信息
     */
    private String defaultMessage;

    private String key;


    DashboardConfigErrorEnum(Integer code, String defaultMessage, String key) {
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
}
