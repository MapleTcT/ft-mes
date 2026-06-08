package com.supcon.supfusion.systemconfig.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 部门相关错误码和错误信息的枚举类
 *
 * @author shidongsheng
 */
public enum SystemConfigErrorEnum implements ErrorDefinition {

    /**
     * 下拉值重复
     */
    OPTION_VALUE_REPEAT(100112000, "option valuse is repeat", "systemConfig.optionValueRepeat"),
    /**
     * 配置子分类重复
     */
    CATALOG_IS_EXIST(100112001, " appcode code is exist", "systemConfig.codeExist"),

    /**
     * 配置大类不存在
     */
    APP_CODE_NOT_EXIST(100112002, "config  is not exist", "systemConfig.appCodeNotExist"),

    /**
     * 不存在的配置类型
     */
    INSER_TYPE_EXIST(100112003, "not support insert type", "systemConfig.typeNotSupport"),

    /**
     * 系统配置类别和app配置类别不允许删除
     */
    APP_CODE_NOT_DELETE(100112004, "appcode[system,app] can not delete", "systemConfig.appCodeDelete"),

    /**
     * 系统配置类别和app配置类别获取详情
     */
    APP_CODE_NOT_SUPPORT(100112005, "appcode[system,app] can not support get ", "systemConfig.appCodeGet"),

    /**
     * 配置项已经存在
     */
    APP_CODE_EXIST(100112006, "config  is  exist", "systemConfig.appCodeExist"),

    /**
     * 系统配置类别和app配置类别不允许新增
     */
    APP_CODE_NOT_INSERT(100112007, "appcode[system,app] can not insert", "systemConfig.appCodeInsert"),

    /**
     * 配置子分类不存在
     */
    CATALOG_ISNOT_EXIST(100112008, " appcode code is not exist", "systemConfig.catlogNotExist"),

    /**
     * 模块code不存在
     */
    MODULE_CODE_NOT_EXIST(100112009, " module code is not exist", "systemConfig.moduleCodeNotExist");

    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

    private String key;


    public void setKey(String key) {
        this.key = key;
    }

    SystemConfigErrorEnum(Integer code, String defaultMessage, String key) {
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
