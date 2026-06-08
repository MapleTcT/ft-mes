package com.supcon.supfusion.custon.property.common.enums;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author zhang yafei
 */
public enum CoustomPropertyErrorEnum implements ErrorDefinition {
    /**
     *
     */
    VCOLUMN_NUMBER_ERROR(100116105, "字段占的列数不能大于总列数！", "vcolumn.number.error"),
    VIEW_DOES_NOT_ERROR(100116104, "视图不存在", "view.does.not.error"),
    SAME_MODEL_NAME_ERROR(100116103, "相同模型名称错误", "same.model.name.error"),
    LEVEL_UNKNOWN_TYPE_ERROR(100116102, "未知的Level类型", "appconfig.levelUnknownType.error"),
    BE_EMPTY_ERROR(100116101, "{0}不能为空","appconfig.beEmpty.error")
    ;

    CoustomPropertyErrorEnum(Integer code, String defaultMessage,String info) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.info = info;
    }
    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

    /**
     * 国际化key
     */
    private String info;

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
        return info;
    }
}
