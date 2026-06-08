/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.configuration.services.enums.AdvDateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 
 * 
 * @author zhuyuyin
 * @version $Id$
 */
public class DateUtil {

	private static final Logger logger = LoggerFactory.getLogger(DateUtil.class);

	public static final String START_DATETIME_FORMAT = "yyyy-MM-dd 0:00:00";
	public static final String END_DATETIME_FORMAT = "yyyy-MM-dd 23:59:59";
	public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public static final String DATE_FORMAT = "yyyy-MM-dd";

	public static List<Date> getFormatDate(AdvDateType dateType, Serializable x_value) {
		Calendar calendar = Calendar.getInstance();
		calendar.setFirstDayOfWeek(Calendar.SUNDAY);
		List<Date> dates = new ArrayList<Date>();
		if (dateType != null) {
			int xvalue = 0;
			if (null != x_value) {
				xvalue = Integer.parseInt(x_value.toString());
			}
			if (dateType.equals(AdvDateType.THIS_TIME)) {
				dates.add(calendar.getTime());
			} else if (dateType.equals(AdvDateType.YESTERDAY) || dateType.equals(AdvDateType.TODAY)
					|| dateType.equals(AdvDateType.TOMORROW)) {
				if (dateType.equals(AdvDateType.TOMORROW)) {
					calendar = addCalendar(calendar, null, null, 1, null);
				} else if (dateType.equals(AdvDateType.YESTERDAY)) {
					calendar = addCalendar(calendar, null, null, -1, null);
				}
				calendar = setCalendar(calendar, null, null, null, 0, 0, 0);
				dates.add(calendar.getTime());
				calendar = setCalendar(calendar, null, null, null, 23, 59, 59);
				dates.add(calendar.getTime());
			} else if (dateType.equals(AdvDateType.NEXT_SEVEN_DAYS)) {// 今后七天 不包含当天
				calendar = addCalendar(calendar, null, null, 1, null);
				calendar = setCalendar(calendar, null, null, null, 0, 0, 0);
				dates.add(calendar.getTime());
				calendar = addCalendar(calendar, null, null, 6, null);
				calendar = setCalendar(calendar, null, null, null, 23, 59, 59);
				dates.add(calendar.getTime());
			} else if (dateType.equals(AdvDateType.LAST_SEVEN_DAYS)) {// 最近七天 包含当天
				calendar = setCalendar(calendar, null, null, null, 23, 59, 59);
				dates.add(calendar.getTime());
				calendar = addCalendar(calendar, null, null, -6, null);
				calendar = setCalendar(calendar, null, null, null, 0, 0, 0);
				dates.add(0, calendar.getTime());
			} else if (dateType.equals(AdvDateType.NEXT_WEEK) || dateType.equals(AdvDateType.LAST_WEEK)
					|| dateType.equals(AdvDateType.THIS_WEEK)) {
				if (dateType.equals(AdvDateType.NEXT_WEEK)) {
					calendar = addCalendar(calendar, null, null, null, 1);
				} else if (dateType.equals(AdvDateType.LAST_WEEK)) {
					calendar = addCalendar(calendar, null, null, null, -1);
				}
				calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
				calendar = setCalendar(calendar, null, null, null, 0, 0, 0);
				dates.add(calendar.getTime());
				calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek() + 6);
				calendar = setCalendar(calendar, null, null, null, 23, 59, 59);
				dates.add(calendar.getTime());
			} else if (dateType.equals(AdvDateType.NEXT_MONTH) || dateType.equals(AdvDateType.LAST_MONTH)
					|| dateType.equals(AdvDateType.THIS_MONTH)) {
				if (dateType.equals(AdvDateType.NEXT_MONTH)) {
					calendar = addCalendar(calendar, null, 1, null, null);
				} else if (dateType.equals(AdvDateType.LAST_MONTH)) {
					calendar = addCalendar(calendar, null, -1, null, null);
				}
				calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
				calendar = setCalendar(calendar, null, null, null, 0, 0, 0);
				dates.add(calendar.getTime());
				calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
				calendar = setCalendar(calendar, null, null, null, 23, 59, 59);
				dates.add(calendar.getTime());
			} else if (dateType.equals(AdvDateType.NEXT_YEAR) || dateType.equals(AdvDateType.LAST_YEAR)
					|| dateType.equals(AdvDateType.THIS_YEAR)) {
				if (dateType.equals(AdvDateType.NEXT_YEAR)) {
					calendar = addCalendar(calendar, 1, null, null, null);
				} else if (dateType.equals(AdvDateType.LAST_YEAR)) {
					calendar = addCalendar(calendar, -1, null, null, null);
				}
				calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMinimum(Calendar.DAY_OF_YEAR));
				calendar = setCalendar(calendar, null, null, null, 0, 0, 0);
				dates.add(calendar.getTime());
				calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR));
				calendar = setCalendar(calendar, null, null, null, 23, 59, 59);
				dates.add(calendar.getTime());
			} else if (dateType.equals(AdvDateType.LAST_X_HOURS)) {// 最近X小时
				dates.add(calendar.getTime());
				calendar.add(Calendar.HOUR_OF_DAY, -xvalue);
				dates.add(0, calendar.getTime());
			} else if (dateType.equals(AdvDateType.NEXT_X_HOURS)) {// 今后X小时
				dates.add(calendar.getTime());
				calendar.add(Calendar.HOUR_OF_DAY, xvalue);
				dates.add(calendar.getTime());
			} else if (dateType.equals(AdvDateType.LAST_X_DAYS)) {// 最近X天
				calendar = setCalendar(calendar, null, null, null, 23, 59, 59);
				dates.add(calendar.getTime());
				calendar = addCalendar(calendar, null, null, -xvalue + 1, null);
				calendar = setCalendar(calendar, null, null, null, 0, 0, 0);
				dates.add(0, calendar.getTime());
			} else if (dateType.equals(AdvDateType.NEXT_X_DAYS)) {// 今后X天
				calendar = setCalendar(calendar, null, null, null, 0, 0, 0);
				calendar = addCalendar(calendar, null, null, 1, null);
				dates.add(calendar.getTime());
				calendar = addCalendar(calendar, null, null, xvalue - 1, null);
				calendar = setCalendar(calendar, null, null, null, 23, 59, 59);
				dates.add(calendar.getTime());
			} else if (dateType.equals(AdvDateType.LAST_X_WEEKS)) {// 最近X周
				calendar = setCalendar(calendar, null, null, null, 23, 59, 59);
				dates.add(calendar.getTime());
				calendar = addCalendar(calendar, null, null, null, -xvalue);
				calendar = setCalendar(calendar, null, null, null, 0, 0, 0);
				dates.add(0, calendar.getTime());
			} else if (dateType.equals(AdvDateType.NEXT_X_WEEKS)) {// 今后X周
				calendar = setCalendar(calendar, null, null, null, 0, 0, 0);
				calendar = addCalendar(calendar, null, null, 1, null);
				dates.add(calendar.getTime());
				calendar = addCalendar(calendar, null, null, null, xvalue);
				calendar = setCalendar(calendar, null, null, null, 23, 59, 59);
				dates.add(calendar.getTime());
			} else if (dateType.equals(AdvDateType.LAST_X_MONTHS)) {// 最近X月
				calendar = setCalendar(calendar, null, null, null, 23, 59, 59);
				dates.add(calendar.getTime());
				calendar = addCalendar(calendar, null, -xvalue, null, null);
				calendar = setCalendar(calendar, null, null, null, 0, 0, 0);
				dates.add(0, calendar.getTime());
			} else if (dateType.equals(AdvDateType.NEXT_X_MONTHS)) {// 今后X月
				calendar = setCalendar(calendar, null, null, null, 0, 0, 0);
				calendar = addCalendar(calendar, null, null, 1, null);
				dates.add(calendar.getTime());
				calendar = addCalendar(calendar, null, xvalue, null, null);
				calendar = setCalendar(calendar, null, null, null, 23, 59, 59);
				dates.add(calendar.getTime());
			} else if (dateType.equals(AdvDateType.LAST_X_YEARS)) {// 最近X年
				calendar = setCalendar(calendar, null, null, null, 23, 59, 59);
				dates.add(calendar.getTime());
				calendar = addCalendar(calendar, -xvalue, null, null, null);
				calendar = setCalendar(calendar, null, null, null, 0, 0, 0);
				dates.add(0, calendar.getTime());
			} else if (dateType.equals(AdvDateType.NEXT_X_YEARS)) {// 今后X年
				calendar = setCalendar(calendar, null, null, null, 0, 0, 0);
				calendar = addCalendar(calendar, null, null, 1, null);
				dates.add(calendar.getTime());
				calendar = addCalendar(calendar, xvalue, null, null, null);
				calendar = setCalendar(calendar, null, null, null, 23, 59, 59);
				dates.add(calendar.getTime());
			} else if (dateType.equals(AdvDateType.X_MONTH_BEFORE)) {// X月以前
				calendar = addCalendar(calendar, null, -xvalue, null, null);
				calendar = setCalendar(calendar, null, null, null, 23, 59, 59);
				dates.add(calendar.getTime());
			}
		} else {
			dates.add(calendar.getTime());
		}
		return dates;
	}

	public static List<Date> getFormatDate() {
		List<Date> dates = new ArrayList<Date>();
		dates.add(new Date());
		return dates;
	}

	public static Calendar setCalendar(Calendar calendar, Serializable year, Serializable month, Serializable day, Serializable hour,
			Serializable minute, Serializable second) {
		if (calendar != null) {
			if (null != year) {
				calendar.set(Calendar.YEAR, Integer.parseInt(year.toString()));
			}
			if (null != month) {
				calendar.set(Calendar.MONTH, Integer.parseInt(month.toString()));
			}
			if (null != day) {
				calendar.set(Calendar.DATE, Integer.parseInt(day.toString()));
			}
			if (null != hour) {
				calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour.toString()));
			}
			if (null != minute) {
				calendar.set(Calendar.MINUTE, Integer.parseInt(minute.toString()));
			}
			if (null != second) {
				calendar.set(Calendar.SECOND, Integer.parseInt(second.toString()));
			}
			calendar.set(Calendar.MILLISECOND, 0);
			return calendar;
		} else {
			return Calendar.getInstance();
		}
	}

	/**
	 * 设置日期
	 * 当前日期向前或向后
	 *
	 * @param calendar
	 * @param year
	 * @param month
	 * @param day
	 * @return Calendar
	 */
	public static Calendar addCalendar(Calendar calendar, Serializable year, Serializable month, Serializable day, Serializable week) {
		if (calendar != null) {
			if (null != year) {
				calendar.add(Calendar.YEAR, Integer.parseInt(year.toString()));
			}
			if (null != month) {
				calendar.add(Calendar.MONTH, Integer.parseInt(month.toString()));
			}
			if (null != day) {
				calendar.add(Calendar.DATE, Integer.parseInt(day.toString()));
			}
			if (null != week) {
				calendar.add(Calendar.WEEK_OF_YEAR, Integer.parseInt(week.toString()));
			}
			return calendar;
		} else {
			return Calendar.getInstance();
		}
	}
}
