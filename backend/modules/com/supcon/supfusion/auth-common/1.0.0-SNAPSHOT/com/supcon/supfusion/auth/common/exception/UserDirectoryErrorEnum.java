package com.supcon.supfusion.auth.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 用户目录相关错误码和错误信息的枚举类
 *
 * @author caokele
 */
public enum UserDirectoryErrorEnum implements ErrorDefinition {
    USER_NOT_EXIST(100106101, "用户目录不存在", "userDirectory.notExist"),
    ERROR_USERNAME_OR_PWD(100106102, "用户名或密码错误", "userDirectory.errorUsernameOrPwd"),
    CONNECT_LDAP_FAILED(100106103, "连接服务器失败", "userDirectory.connectLdapFailed"),
    EMPTY_USERNAME_OR_PWD(100106104, "用户名或密码不允许为空", "userDirectory.emptyUsernameOrPwd"),
    EMPTY_AD_ENABLE(100106106, "只允许启用一个AD域", "userDirectory.ADenable"),
    FIELD_NOT_SUPPORT(100106105, "不支持该字段筛选", "userDirectory.fieldNotSupport"),
    AD_DELETE_NOT_SUPPORT(100106107, "不允许删除已启用的AD域", "userDirectory.deleteEnableNotSupport");

    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

    private String key;


    UserDirectoryErrorEnum(Integer code, String defaultMessage, String key) {
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
