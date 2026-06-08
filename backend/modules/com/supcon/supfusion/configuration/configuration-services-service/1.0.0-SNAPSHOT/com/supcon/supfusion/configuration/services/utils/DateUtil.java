/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.utils;

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

	public static List<Date> getFormatDate() {
		List<Date> dates = new ArrayList<Date>();
		dates.add(new Date());
		return dates;
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
			logger.error(e.getMessage(), e);
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
	 * @param operator
	 *            <code>Constants.DATE_BEFORE || Constants.DATE_AFTER || Constants.DATE_ON_OR_BEFORE || Constants.DATE_ON_OR_AFTER</code>
	 * @return List<Date>
	 */
	public static String getDate(String date) {
		SimpleDateFormat sf = new SimpleDateFormat(DATE_FORMAT);
        long lt = new Long(date);
        String res = sf.format(new Date(lt));
		return res;
	}

}
