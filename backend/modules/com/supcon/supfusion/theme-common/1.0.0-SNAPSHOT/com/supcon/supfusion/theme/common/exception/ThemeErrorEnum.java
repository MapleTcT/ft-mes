package com.supcon.supfusion.theme.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 错误码和错误信息的枚举类
 * @author root
 *
 */
public enum ThemeErrorEnum implements ErrorDefinition {
	UPLOAD_ERROR(100116002, "上传失败!", "theme.UPLOAD_ERROR");

	/**
	 * 异常码
	 */
	private Integer code;
	/**
	 * 默认异常信息
	 */
	private String defaultMessage;

	private String key;

	ThemeErrorEnum(Integer code, String defaultMessage, String key) {
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
