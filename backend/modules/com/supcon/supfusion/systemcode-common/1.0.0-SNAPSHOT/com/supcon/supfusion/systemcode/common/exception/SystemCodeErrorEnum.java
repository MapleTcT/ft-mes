package com.supcon.supfusion.systemcode.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 错误码和错误信息的枚举类
 * @author root
 *
 */
public enum SystemCodeErrorEnum implements ErrorDefinition {
	NECESSARY_PARAM(100111000, "必填参数不可以为空","必填参数不可以为空"),
	SYSTEM_ENTITY_CODE_IS_EXISTS(100111001, "编码已存在!", "systemCode.CODE_IS_EXISTS"),
	SYSTEM_CODE_VALUE_IS_EXISTS(100111002, "编码已存在!","systemCode.CODE_IS_EXISTS"),
	SYSTEM_DELETE_DATA_IS_NOT_EMPTY(100111003, "请选择系统编码!","systemCode.SYSTEM_DELETE_DATA_IS_NOT_EMPTY"),
	CODE_INPUT_FORMAT_ERROR(100111004, "输入编码格式错误，编码为字母，数字，下划线组合，不能超过100个字符!","systemCode.CODE_INPUT_FORMAT_ERROR"),
	SYSTEM_DELETE_BY_MODULE_ID_IS_NOT_EMPTY(100111005, "请选择模块!","systemCode.MODULE_ID_PARAM_NECESSARY"),
	COMPANY_ID_IS_NOT_FOUND(100111005, "未获取到当前用户的登陆公司ID","systemCode.COMPANY_ID_IS_NOT_FOUND");
    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

    private String key;

    SystemCodeErrorEnum(Integer code, String defaultMessage, String key) {
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
