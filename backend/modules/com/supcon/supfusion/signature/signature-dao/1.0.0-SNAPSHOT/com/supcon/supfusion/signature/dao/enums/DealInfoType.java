/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.signature.dao.enums;

/**
 * 处理意见类型
 * 
 *
 * @author yaowei
 * @since
 */
public enum DealInfoType {
	/**
	 * 普通
	 */
	NORMAL , 
	/**
	 * 转发
	 */
	FORWARD,
	
	/**
	 * 循环会签转发的处理人本身有活动权限，同时是预期委托的被委托人
	 */
	FORWARD_AND_EXPECTEDCONSIGN,
	/**
	 * 更改负责人
	 */
	MODIFYOWNERSTAFF,
	/**
	 * 待办迁移
	 */
	PENDINGTRANS,
	/**
	 * 待办新增
	 */
	PENDINGADD,
	/**
	 * 预期委托
	 */
	EXPECTEDCONSIGNOR,
	/**
	 * 预期委托撤回 
	 */
	EXPECTEDCONSIGNRECALL,
	
	/**
	 * 并代xxx处理（即处理人本身有权限，又是预期委托的被委托人）
	 */
	NORMAL_AND_EXPECTEDCONSIGN,
	/**
	 * 归档
	 */
	FILED,
	/**
	 * 委托处理
	 */
	ENTRUST,
	/**
	 * 挂起
	 */
	SUSPEND,
	/**
	 * 复原
	 */
	RESTORE,
	/**
	 * 作废
	 */
	INVALID,
	/**
	 * 委托
	 */
	CONSIGNOR,
	/**
	 * 撤回
	 */
	RECALL,
	
	/**
	 * 超级修改/录入
	 */
	SUPEREDIT,
	/**
	 * 导入
	 */
	IMPORT,
	/*
	* 驳回
	* */
	REJECT;
}
