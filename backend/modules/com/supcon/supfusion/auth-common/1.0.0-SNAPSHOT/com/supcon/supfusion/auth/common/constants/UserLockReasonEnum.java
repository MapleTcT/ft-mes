package com.supcon.supfusion.auth.common.constants;

/**
 * 用户锁定原因枚举类
 *
 * @author caokele
 */
public enum UserLockReasonEnum {

    ARTIFICIAL(0, "人为锁定"),
    USER_DIRECTORY(1, "用户目录禁用");

    private Integer code;

    private String name;

    UserLockReasonEnum(Integer code, String name) {
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
