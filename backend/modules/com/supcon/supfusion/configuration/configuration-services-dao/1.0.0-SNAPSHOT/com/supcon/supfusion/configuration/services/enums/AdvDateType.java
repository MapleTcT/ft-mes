/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.enums;


import com.supcon.supfusion.configuration.services.utils.DateUtil;

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
		switch (this) {
			case YESTERDAY:
				return DateUtil.getFormatDate(YESTERDAY, x_value);
			case TODAY:
				return DateUtil.getFormatDate(TODAY, x_value);
			case TOMORROW:
				return DateUtil.getFormatDate(TOMORROW, x_value);
			case NEXT_SEVEN_DAYS:
				return DateUtil.getFormatDate(NEXT_SEVEN_DAYS, x_value);
			case LAST_SEVEN_DAYS:
				return DateUtil.getFormatDate(LAST_SEVEN_DAYS, x_value);
			case LAST_WEEK:
				return DateUtil.getFormatDate(LAST_WEEK, x_value);
			case NEXT_WEEK:
				return DateUtil.getFormatDate(NEXT_WEEK, x_value);
			case THIS_WEEK:
				return DateUtil.getFormatDate(THIS_WEEK, x_value);
			case LAST_MONTH:
				return DateUtil.getFormatDate(LAST_MONTH, x_value);
			case NEXT_MONTH:
				return DateUtil.getFormatDate(NEXT_MONTH, x_value);
			case THIS_MONTH:
				return DateUtil.getFormatDate(THIS_MONTH, x_value);
			case LAST_YEAR:
				return DateUtil.getFormatDate(LAST_YEAR, x_value);
			case NEXT_YEAR:
				return DateUtil.getFormatDate(NEXT_YEAR, x_value);
			case THIS_YEAR:
				return DateUtil.getFormatDate(THIS_YEAR, x_value);
			case LAST_X_HOURS:
				return DateUtil.getFormatDate(LAST_X_HOURS, x_value);
			case NEXT_X_HOURS:
				return DateUtil.getFormatDate(NEXT_X_HOURS, x_value);
			case LAST_X_DAYS:
				return DateUtil.getFormatDate(LAST_X_DAYS, x_value);
			case NEXT_X_DAYS:
				return DateUtil.getFormatDate(NEXT_X_DAYS, x_value);
			case LAST_X_WEEKS:
				return DateUtil.getFormatDate(LAST_X_WEEKS, x_value);
			case NEXT_X_WEEKS:
				return DateUtil.getFormatDate(NEXT_X_WEEKS, x_value);
			case LAST_X_MONTHS:
				return DateUtil.getFormatDate(LAST_X_MONTHS, x_value);
			case NEXT_X_MONTHS:
				return DateUtil.getFormatDate(NEXT_X_MONTHS, x_value);
			case LAST_X_YEARS:
				return DateUtil.getFormatDate(LAST_X_YEARS, x_value);
			case NEXT_X_YEARS:
				return DateUtil.getFormatDate(NEXT_X_YEARS, x_value);
			case X_MONTH_BEFORE:
				return DateUtil.getFormatDate(X_MONTH_BEFORE, x_value);
			default:
				return DateUtil.getFormatDate();
		}
	}

}
