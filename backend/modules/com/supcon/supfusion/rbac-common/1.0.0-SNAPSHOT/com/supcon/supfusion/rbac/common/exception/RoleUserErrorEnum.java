package com.supcon.supfusion.rbac.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 错误码和错误信息的枚举类
 * @author root
 *
 */
public enum RoleUserErrorEnum implements ErrorDefinition {
	NO_ROLE_FIND(100105100, "找不到角色！","rbac.NO_ROLE_FIND"),
	NO_RESTRICTED_SCOPE(100105101, "没有提供下载数据范围！","rbac.NO_RESTRICTED_SCOPE"),
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

	RoleUserErrorEnum(Integer code, String defaultMessage,String key) {
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
