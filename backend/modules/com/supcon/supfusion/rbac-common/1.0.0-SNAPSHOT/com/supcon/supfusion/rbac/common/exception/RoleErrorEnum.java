package com.supcon.supfusion.rbac.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 错误码和错误信息的枚举类
 * @author root
 *
 */
public enum RoleErrorEnum implements ErrorDefinition {
	NECESSARY_PARAM(100105000, "必填参数不可以为空","rbac.NECESSARY_PARAM"),
	CODE_KEYWORD_ERROR(100105001,"编码不允许为关键字！","rbac.CODE_KEYWORD_ERROR"),
	ASSO_WITH_ROLEUSER(100105002,"存在关联用户！不允许删除","rbac.ASSO_WITH_ROLEUSER"),
	HAS_CHILD_ROLE(100105003,"存在子角色","rbac.HAS_CHILD_ROLE"),
	UNIQUECODE(100105004,"编码重复！","rbac.UNIQUECODE"),
	UNIQUENAME(100105005,"名称重复！","rbac.UNIQUENAME"),
	ROLE_CANNOT_FIND(100105006,"编码无对应角色！","rbac.ROLE_CANNOT_FIND"),
	TAG_LENGTH_LIMIT_50(100105007,"标签名长度不能超过50","rbac.TAG_LENGTH_LIMIT_50"),
    ROLE_CODE_FORMAT_ERROR(100105008,"角色编码只支持英文数字下划线","rbac.ROLE_CODE_FORMAT_ERROR"),
    ROLE_CODE_LENGTH_TOO_LONG(100105009,"角色编码长度不能超过50","rbac.ROLE_CODE_LENGTH_TOO_LONG"),
    ROLE_NAME_LENGTH_TOO_LONG(100105010,"角色名称长度不能超过50","rbac.ROLE_NAME_LENGTH_TOO_LONG"),
    ROLE_DESCRIPTION_LENGTH_TOO_LONG(100105011,"角色描述长度不能超过255","rbac.ROLE_DESCRIPTION_LENGTH_TOO_LONG"),
    COMPANY_DONT_EXIST(100105012,"公司不存在","rbac.COMPANY_DONT_EXIST"),
    ASSO_WITH_ROLEPOSITION(100105013,"存在关联岗位！不允许删除","rbac.ASSO_WITH_ROLEPOSITION"),
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

	RoleErrorEnum(Integer code, String defaultMessage,String key) {
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
