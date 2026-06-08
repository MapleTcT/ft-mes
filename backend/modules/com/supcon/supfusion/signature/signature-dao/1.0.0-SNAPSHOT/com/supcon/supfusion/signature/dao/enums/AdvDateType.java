/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.signature.dao.enums;


import java.util.Date;
import java.util.List;

/**
 * 
 * 
 * @author zhuyuyin
 * @version $Id$
 */
public enum AdvDateType {
	/**
	 * 昨天
	 */
	YESTERDAY,
	/**
	 * 今天
	 */
	TODAY,
	/**
	 * 明天
	 */
	TOMORROW,
	/**
	 * 今后七天
	 */
	NEXT_SEVEN_DAYS,
	/**
	 * 最近七天
	 */
	LAST_SEVEN_DAYS,
	/**
	 * 下周
	 */
	NEXT_WEEK,
	/**
	 * 上周
	 */
	LAST_WEEK,
	/**
	 * 本周
	 */
	THIS_WEEK,
	/**
	 * 下月
	 */
	NEXT_MONTH,
	/**
	 * 上月
	 */
	LAST_MONTH,
	/**
	 * 本月
	 */
	THIS_MONTH,
	/**
	 * 明年
	 */
	NEXT_YEAR,
	/**
	 * 去年
	 */
	LAST_YEAR,
	/**
	 * 今年
	 */
	THIS_YEAR,
	/**
	 * 最近X小时
	 */
	LAST_X_HOURS,
	/**
	 * 今后X小时
	 */
	NEXT_X_HOURS,
	/**
	 * 最近X天
	 */
	LAST_X_DAYS,
	/**
	 * 今后X天
	 */
	NEXT_X_DAYS,
	/**
	 * 最近X周
	 */
	LAST_X_WEEKS,
	/**
	 * 今后X周
	 */
	NEXT_X_WEEKS,
	/**
	 * 最近X月
	 */
	LAST_X_MONTHS,
	/**
	 * 今后X月
	 */
	NEXT_X_MONTHS,
	/**
	 * 最近X年
	 */
	LAST_X_YEARS,
	/**
	 * 今后X年
	 */
	NEXT_X_YEARS,
	/**
	 * X个月以前
	 */
	X_MONTH_BEFORE,
	/**
	 * 当前时间
	 */
	THIS_TIME;

	/**
	 * 根据类型返回相应的List<Date>
	 * @param x_value 日期增减数 非负整数
	 * @return
	 */
	public List<Date> getFormatDate(int x_value) {
		return null;
	}

}
