package com.supcon.supfusion.signature.base.enums;

public enum SignatureColumn {
	/** 电子签名日志对应模块 CODE字段 */
	MODULE_CODE,
	/**模块名称*/
	MODULE_NAME,
	/** 电子签名日志对应实体 CODE字段*/
	ENTITY_CODE,
	/** 电子签名日志对应模型 CODE*/
	MODEL_CODE,
	/**电子签名日志对应按钮CODE*/
	BUTTON_CODE,
	/** 电子签名日志所含数据日志对应表单ID*/
	TABLE_ID,
	/** 电子签名日志对应签名用户ID*/
	USER_ID,
	/** 电子签名日志对应UUID*/
	UUID,
	/**签名状态（失败，成功）**/
	STATUS,
	/**签名开始时间**/
	START_TIME,
	/**签名结束时间**/
	END_TIME,
	/**签名日志对应员工ID**/
	STAFF_ID,
	/**签名日志迁移点ID*/
	TRANSITION_ID,
	/**活动ID*/
	TASK_ID,
	/**流程ID*/
	PROCESS_ID,
	/**签名类型*/
	SIGNATURE_TYPE,
	/**用户登录IP*/
	IP_ADDRESS,
	/** 业务主键*/
	BUSINESS_KEY,
	/** 实体名称*/
	ENTITY_NAME,
	/** 模型名称*/
	MODEL_NAME,
	/**用户名*/
	USER_NAME,
	/**第一签名人*/
	FIRST_STAFF_NAME,
	/**第一签用户名*/
	FIRST_USER_NAME,
	/**第一签名人id*/
	FIRST_STAFF_ID,
	/**第一签名时间*/
	FIRST_SIGN_TIME,
	/**首签原因*/
	FIRST_REASON,
	/**第二签名人*/
	SECOND_STAFF_NAME,
	/**第二签名人id*/
	SECOND_STAFF_ID,
	/**第二签名时间*/
	SECOND_SIGN_TIME,
	/**第二签用户名*/
	SECOND_USER_NAME,
	/**次签原因*/
	SECOND_REASON




}
