package com.supcon.supfusion.auth.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * Ip黑白名单相关错误码和错误信息的枚举类
 *
 * @author caokele
 */
public enum IpBlackWhiteErrorEnum implements ErrorDefinition {
    ERROR_IP_FORMAT(100106201, "IP格式错误", "ipBlackWhite.errorIpFormat"),
    ERROR_CONTROL_TYPE_FORMAT(100106202, "管控模式格式错误", "ipBlackWhite.errorControlTypeFormat"),
    IP_EXIST(100106203, "该IP已在列表中", "ipBlackWhite.ipExist"),
    CONFLICT_CONTROL_TYPE_BLACK(100106204, "列表管控模式已选黑名单，无法添加白名单IP", "ipBlackWhite.conflictControlTypeBlack"),
    CONFLICT_CONTROL_TYPE_WHITE(100106205, "列表管控模式已选白名单，无法添加黑名单IP", "ipBlackWhite.conflictControlTypeWhite"),
    NOT_EXIST(100106206, "IP黑白名单不存在", "ipBlackWhite.notExist"),
    ACCESS_IP_FORBIDDEN(100106207, "IP访问被拒绝", "ipBlackWhite.accessIpForbidden"),
    ONLY_ONE_CONTROL_TYPE(100106207, "同时只允许存在一种管控模式", "ipBlackWhite.onlyOneControlType");

    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

    private String key;


    IpBlackWhiteErrorEnum(Integer code, String defaultMessage, String key) {
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
