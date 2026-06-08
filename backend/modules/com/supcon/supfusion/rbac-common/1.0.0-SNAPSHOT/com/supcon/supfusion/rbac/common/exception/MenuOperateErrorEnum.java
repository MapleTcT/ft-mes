package com.supcon.supfusion.rbac.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public enum MenuOperateErrorEnum implements ErrorDefinition {

    UNIQUECODE(100105400, "编码重复！","rbac.UNIQUECODE"),
    MENU_DATA_ISEMPTY(100105401, "请选择菜单！","rbac.MENU_DATA_ISEMPTY"),
    OPERATE_HAS_GRANT_PERMISSION (100105402, "存在操作已被分配权限，删除失败！","rbac.OPERATE_HAS_GRANT_PERMISSION"),
    SAME_URL (100105403, "该类型URL已存在，请重新选择操作类型或修改地址！","rbac.SAME_URL"),
    OPERATE_CODE_EMPTY (100105404, "操作编码为空！","rbac.OPERATE_CODE_EMPTY"),
    URL_CONT_NOT_EMPTY (100105405, "操作关联的URL的地址不能为空！","rbac.URL_CONT_NOT_EMPTY"),
    METHOD_TYPE_CONT_NOT_EMPTY (100105406, "操作关联的URL的请求方式不能为空！","rbac.METHOD_TYPE_CONT_NOT_EMPTY"),
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

    MenuOperateErrorEnum(Integer code, String defaultMessage,String key) {
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
