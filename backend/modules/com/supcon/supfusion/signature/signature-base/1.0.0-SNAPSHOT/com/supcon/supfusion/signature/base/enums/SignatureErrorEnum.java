package com.supcon.supfusion.signature.base.enums;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author zhang yafei
 */
public enum SignatureErrorEnum  implements ErrorDefinition {
    /**
     *100116000 ~ 100116999 业务编码号段
     *
     */
    BE_EMPTY_ERROR(100116101, "{0}不能为空","appconfig.beEmpty.error"),
    CODE_CANNOT_BE_EMPTY_ERROR(100116101, "code不能为空","appconfig.codeBeEmpty.error"),
    BUTTON_CODE_CANNOT_BE_EMPTY_ERROR(100116101, "buttonCode不能为空", "appconfig.buttonCodeBeEmpty.error"),
    SIGNATURE_TYPE_CANNOT_BE_EMPTY_ERROR(100116101, "signatureType不能为空", "appconfig.signatureTypeBeEmpty.error"),

    LEVEL_UNKNOWN_TYPE_ERROR(100116102, "未知的Level类型", "appconfig.levelUnknownType.error"),
    TIME_UNKNOWN_TYPE_ERROR(100116102, "未知的时间类型", "appconfig.timeUnknownType.error"),
    UNKNOWN_TYPE_ERROR(100116102, "未知的{0}类型:{1}", "appconfig.unknownType.error"),

    BUTTON_CODE_INVALID_ERROR(100116103, "buttonCode无效", "appconfig.buttonCodeInvalid.error"),
    CODE_INVALID_ERROR(100116003, "code无效", "appconfig.codeInvalid.error"),

    DATA_NOT_EXIST_ERROR(100116104,"{0}类型的数据不存在","appconfig.dataNotExist.error"),
    TIME_FORMAT_ERROR(100116105,"时间格式错误","")
    ;

    SignatureErrorEnum(Integer code, String defaultMessage,String info) {
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
