package com.supcon.supfusion.auth.common.constants;

/**
 * @author lifangyuan
 */

public enum UserTypeEnum {

    COMMON_USER(0, "普通用户"),
    SYSTEM_USER(1, "系统管理员");

    private Integer code;

    private String name;

    UserTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
