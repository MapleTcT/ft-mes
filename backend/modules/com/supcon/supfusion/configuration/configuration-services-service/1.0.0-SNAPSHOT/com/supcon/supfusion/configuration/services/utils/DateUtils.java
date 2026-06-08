/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.configuration.services.exceptions.EcException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * 日期时间包装工具类.
 * 
 * @author jiawei
 * 
 */
public class DateUtils {
	// ~ Instance fields =======================================================
	public final static String FULL_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss.S";
	public final static String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public final static String DATE_FORMAT = "yyyy-MM-dd";
	public final static String DATETIME_HOUR_FORMAT = "yyyy-MM-dd HH";
	public final static String DATETIME_MIN_FORMAT = "yyyy-MM-dd HH:mm";
	public final static String TIME_FORMAT = "HH:mm:ss";

	// ~ Constructor ===========================================================

	// ~ Methods ===============================================================
	public static Date getNowDateTime() {
		return new Date();
	}

	public static Date getNowDate() {
		return parseDate(formatDate(getNowDateTime()));
	}

	/**
	 * 获取当前时间前N天的DateTime
	 * 
	 * @param days
	 * @return
	 */
	public static Date getPrevDateTime(int days) {
		return getPrevDateTime(new Date(), days);
	}

	public static Date getPrevDateTime(Date d, int days) {
		return new Date(d.getTime() - days * 3600000L * 24L);
	}

	public static Date getNextDateTime(Date d, int days) {
		return new Date(d.getTime() + days * 3600000L * 24L);
	}

	/**
	 * 获取当前时间后N天的DateTime
	 * 
	 * @param days
	 * @return
	 */
	public static Date getNextDateTime(int days) {
		return getNextDateTime(new Date(), days);
	}

	/**
	 * 获取当前时间前N毫秒的DateTime
	 * 
	 * @param days
	 * @return
	 */
	public static Date getPrevDateTime(long millis) {
		return new Date(System.currentTimeMillis() - millis);
	}

	/**
	 * 获取当前时间后N毫秒的DateTime
	 * 
	 * @param days
	 * @return
	 */
	public static Date getNextDateTime(long millis) {
		return new Date(System.currentTimeMillis() + millis);
	}

	public static String format(Date date, String formatStr) {
		return new SimpleDateFormat(formatStr).format(date);
	}

	/**
	 * 转换 到 "yyyy-MM-dd HH:mm:ss"
	 * 
	 * @param date
	 * @return
	 */
	public static String formatDateTime(Date date) {
		return new SimpleDateFormat(DATETIME_FORMAT).format(date);
	}

	/**
	 * 转换 到 "yyyy-MM-dd HH:mm"
	 * 
	 * @param date
	 * @return
	 */
	public static String formatDateTimeMin(Date date) {
		return new SimpleDateFormat(DATETIME_MIN_FORMAT).format(date);
	}

	/**
	 * 转换 到 "yyyy-MM-dd HH"
	 * 
	 * @param date
	 * @return
	 */
	public static String formatDateTimeHour(Date date) {
		return new SimpleDateFormat(DATETIME_HOUR_FORMAT).format(date);
	}

	/**
	 * Convert date and time to string like "yyyy-MM-dd HH:mm:ss".
	 * 
	 * @param d
	 * @return
	 */
	public static String formatDateTime(long d) {
		return new SimpleDateFormat(DATETIME_FORMAT).format(d);
	}

	/**
	 * Convert date to String like "yyyy-MM-dd".
	 */
	public static String formatDate(Date d) {
		return new SimpleDateFormat(DATE_FORMAT).format(d);
	}

	/**
	 * Parse date like "yyyy-MM-dd".
	 */
	public static Date parseDate(String d) {
		try {
			return new SimpleDateFormat(DATE_FORMAT).parse(d);
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Parse date and time like "yyyy-MM-dd hh:mm:ss".
	 */
	public static Date parseDateTime(String str) {
		try {
			String formatStr = null;
			if (str.length() == DATETIME_FORMAT.length()) {
				formatStr = DATETIME_FORMAT;
			} else {
				formatStr = DATETIME_FORMAT.substring(0, str.length()).trim();
			}
			return new SimpleDateFormat(formatStr).parse(str);
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * 
	 * 获取当前星期（中国, 如：星期日,星期一,星期二）
	 */
	public static String getWeekCS() {
		// Calendar c = GregorianCalendar.getInstance();
		// c.setFirstDayOfWeek(Calendar.SUNDAY);
		//
		// return s[c.get(Calendar.DAY_OF_WEEK) - 1];
		String[] s = { "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六" };
		return s[getWeek(new Date()) - 1];
	}

	public static int getWeek(Date d) {
		Calendar c = GregorianCalendar.getInstance();
		c.setTime(d);
		c.setFirstDayOfWeek(Calendar.SUNDAY);
		return c.get(Calendar.DAY_OF_WEEK);
	}

	/** * 获取当前日期（中国,yyyy年MM月dd日） */
	public static String getDateCS() {
		Date currentTime = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日");
		String dateString = formatter.format(currentTime);
		return dateString;
	}

	/** *获取当前时间的长字符串形式 "yyyy-MM-dd HH:mm:ss"必须大写MM(月份) */
	public static String getLongStr() {
		Date currentTime = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateString = formatter.format(currentTime);
		return dateString;
	}

	/**
	 * 
	 * 获取二个时间间隔，精确到分
	 */
	public static String getCountTime(Date startDate, Date endDate) {
		Long a = (Long) (startDate.getTime() / 1000);
		Long b = (Long) (endDate.getTime() / 1000);
		Long c = b - a;
		Double day = Math.floor(c / (60 * 60 * 24));
		Double hour = Math.floor((c - day * 60 * 60 * 24) / (60 * 60));
		Double min = Math.floor((c - day * 60 * 60 * 24 - hour * 60 * 60) / 60);

		String str = "";
		if (day > 0) {
			str += String.valueOf(day.intValue()) + "天";
		}
		if (hour > 0) {
			str += String.valueOf(hour.intValue()) + "小时";
		}
		if (min > 0) {
			str += String.valueOf(min.intValue()) + "分";
		}
		return str;
	}

	public static int compareTime(Date d, String time) {
		try {
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(d);
			Date s = new SimpleDateFormat(TIME_FORMAT).parse(time);
			Calendar sc = new GregorianCalendar();
			sc.setTime(s);
			sc.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
					calendar.get(Calendar.DATE));
			return calendar.compareTo(sc);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	public static Date saveDayTime(Date d, String time){
		try {
            Calendar calendar = new GregorianCalendar();
		calendar.setTime(d);
		Date s = new SimpleDateFormat(TIME_FORMAT).parse(time);
		Calendar sc = new GregorianCalendar();
		sc.setTime(s);
		sc.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
				calendar.get(Calendar.DATE));
		return sc.getTime();
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	
	public static int compareTime(Date d, String time, int field, int amount) {
		try {
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(d);
			Date s = new SimpleDateFormat(TIME_FORMAT).parse(time);
			Calendar sc = new GregorianCalendar();
			sc.setTime(s);
			sc.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
					calendar.get(Calendar.DATE));
			sc.add(field, amount);
			return calendar.compareTo(sc);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	public static final int R_DAY = 1;
	public static final int R_HOUR = 2;
	public static final int R_MINUTE = 3;
	public static final int R_SECOND = 4;

	public static float sub(Date start, Date end, int r, int nf) {
		long startM = start.getTime();
		long endM = end.getTime();
		// long result = startM - endM;
		float f = startM - endM;
		switch (r) {
		case R_DAY:
			f /= (1000 * 60 * 60 * 24);
			break;
		case R_HOUR:
			f /= (1000 * 60 * 60);
			break;
		case R_MINUTE:
			f /= (1000 * 60);
			break;
		case R_SECOND:
			f /= (1000);
			break;
		}
		// BigDecimal bd = new BigDecimal(f);
		// bd.setScale(nf, RoundingMode.HALF_UP);
		return new BigDecimal(f).setScale(nf, RoundingMode.HALF_UP)
				.floatValue();
	}

	// private static final DecimalFormat df = new DecimalFormat("#.00");

	public static float sub(Date start, String end, int r) {
		try {
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(start);
			Date s = new SimpleDateFormat(TIME_FORMAT).parse(end);
			Calendar sc = new GregorianCalendar();
			sc.setTime(s);
			sc.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
					calendar.get(Calendar.DATE));
			return sub(start, sc.getTime(), r, 2);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws ParseException {
		System.out.println(sub(parseDateTime("2011-11-13 17:30:00"),
				parseDateTime("2011-11-12 08:30:00"), DateUtils.R_DAY, 2));
	}
}