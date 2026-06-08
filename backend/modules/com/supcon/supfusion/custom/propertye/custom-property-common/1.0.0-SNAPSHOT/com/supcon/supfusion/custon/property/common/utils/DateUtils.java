/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.custon.property.common.utils;

import com.supcon.supfusion.custon.property.common.constants.Constants;
import com.supcon.supfusion.custon.property.common.enums.AdvDateType;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class DateUtils {

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

	/**
	 * 设置日期
	 * 
	 * @param calendar
	 * @param year
	 * @param month
	 * @param day
	 * @param hour
	 * @param minute
	 * @param second
	 * @return Calendar
	 */
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

	/**
	 * 按时间操作符获取Date List
	 * 
	 * @param date
	 * @param operator
	 *            <code>Constants.DATE_BEFORE || Constants.DATE_AFTER || Constants.DATE_ON_OR_BEFORE || Constants.DATE_ON_OR_AFTER</code>
	 * @return List<Date>
	 */
	public static List<Date> getDateList(String date, String operator) {
		List<Date> dates = new ArrayList<Date>();
		SimpleDateFormat sf = new SimpleDateFormat(DATETIME_FORMAT);
		if (date != null && date.length() > 10 && date.length() < 19) {
			date = date.substring(0, 10);
		} else if (date != null && date.length() >= 19) {
			date = date.substring(0, 19);
		}
		try {
			if (operator.equals(Constants.DATE_BEFORE) || operator.equals(Constants.DATE_ON_OR_AFTER)) {
				dates.add(sf.parse(date + " 0:00:00"));
			} else if (operator.equals(Constants.DATE_AFTER) || operator.equals(Constants.DATE_ON_OR_BEFORE)) {
				dates.add(sf.parse(date + " 23:59:59"));
			} else if (operator.equals(Constants.DATE_ON)) {
				dates.add(sf.parse(date + " 0:00:00"));
				dates.add(sf.parse(date + " 23:59:59"));
			}
		} catch (ParseException e) {
			log.error(e.getMessage(), e);
		}
		return dates;
	}

	/**
	 * 根据传入参数数组获取配置文件节点code
	 * 
	 * @param names
	 * @return Map<String,String> names为null时返回cellCode
	 */
	public static Map<String, String> getEcConfigNodeCode(String... names) {
		String timestamp = System.currentTimeMillis() + "_" + Math.round(Math.random() * 10000);
		Map<String, String> codeMap = new HashMap<String, String>();
		if (names == null || names.length == 0) {
			codeMap.put("cell", "cell_" + timestamp);
			return codeMap;
		} else {
			for (String name : names) {
				codeMap.put(name, name + "_" + timestamp);
			}
			return codeMap;
		}
	}

	/**
	 * 根据传入参数数组获取配置文件节点code
	 * 
	 * @param nodeName
	 *            节点名称
	 * @param timestamp
	 *            时间戳
	 * @param random
	 *            随机数
	 * @return String names为null时返回cellCode
	 */
	public static String getEcConfigNodeCode(String nodeName, long timestamp, long random) {
		if (nodeName != null) {
			return nodeName + "_" + timestamp + "_" + random;
		}
		return "cell" + "_" + timestamp + "_" + random;
	}

	/**
	 * 获取无格式的日期字符串
	 * 
	 * @param date
	 * @return 无格式的日期字符串 如: 2012-01-01 08:00:05 转为20120101080005
	 */
	public static String getNoFormatDateString(Date date) {
		if (date == null) {
			date = new Date();
		}
		StringBuffer sb = new StringBuffer();
		sb.append(date.getYear() + 1900);
		sb.append(((date.getMonth() + 1) >= 10) ? date.getMonth() + 1 : "0" + (date.getMonth() + 1));
		sb.append((date.getDate() >= 10) ? date.getDate() : "0" + date.getDate());
		sb.append((date.getHours() >= 10) ? date.getHours() : "0" + date.getHours());
		sb.append((date.getMinutes() >= 10) ? date.getMinutes() : "0" + date.getMinutes());
		sb.append((date.getSeconds() >= 10) ? date.getSeconds() : "0" + date.getSeconds());
		return sb.toString();
	}

	/**
	 * 根据格式返回时间
	 * @param date
	 * @param sf
	 * @return
	 */
	public static String getTime(Date date,SimpleDateFormat sf){
		if (date == null) {
			date = new Date();
		}
		return sf.format(date);
	}
	
	/**
	 * 按时间操作符获取Date List
	 * 
	 * @param date
	 * @param
	 *            <code>Constants.DATE_BEFORE || Constants.DATE_AFTER || Constants.DATE_ON_OR_BEFORE || Constants.DATE_ON_OR_AFTER</code>
	 * @return List<Date>
	 */
	public static String getDate(String date) {
		SimpleDateFormat sf = new SimpleDateFormat(DATE_FORMAT);
        long lt = new Long(date);
        String res = sf.format(new Date(lt));
		return res;
	}
	
	public static void main(String[] args) {
		System.out.println(getNoFormatDateString(null));
	}
}
