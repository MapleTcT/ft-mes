/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.signature.dao.utils;

import java.text.DateFormat;
import java.util.Date;
import java.util.regex.Pattern;


/**
 * 导入验证工具类.
 * 
 * @author tanzhengyang
 * 
 */
public class ValidateUtils {
	// ~ Instance fields =======================================================

	/**
	 * 电话号码验证正则表达式(不带区号)
	 */
	public static final Pattern TEL_REGEX_PATTERN = Pattern
			.compile("^[1-9]\\d{6,7}");
	/**
	 * 电话号码验证正则表达式(带区号)
	 */
	public static final Pattern TEL_WITH_CODE_REGEX_PATTERN = Pattern
			.compile("^[0]\\d{10,11}");

	/**
	 * 手机号码验证正则表达式
	 */
	public static final Pattern CELL_PHONE_REGEX_PATTERN = Pattern
			.compile("^[1][\\d]{10}");

	/**
	 * 邮箱验证正则表达式
	 */
	public static final Pattern MAIL_REGEX_PATTERN = Pattern
			.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");

	/**
	 * 邮编验证正则表达式
	 */
	public static final Pattern POST_CODE_REGEX_PATTERN = Pattern
			.compile("^[1-9]\\d{5}");

	// ~ Constructor ===========================================================

	// ~ Methods ===============================================================
	/**
	 * 判断是否为空
	 */
	public static boolean isNULL(String content) {

		if (content != null && content.length() > 0) {
			return true;
		}
		return false;
	}

	/**
	 * 判断是否为Long
	 */
	public static boolean isLong(String content) {
		try {
			if (content != null && content.length() > 0) {
				Long.valueOf(content);//这个效率比parseLong高
				return true;
			}
		} catch (Exception e) {
		}
		return false;
	}

	/**
	 * 判断是否为int
	 */
	public static boolean isInt(String content) {
		try {
			if (content != null && content.length() > 0) {
				Integer.parseInt(content);
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * 判断是否为电话号码(带区号)<br>
	 * 格式：0106767676，共11位或者12位，必须是0开头
	 */
	public static boolean isTelNoWithCode(String content) {
		try {
			if (content != null && content.length() > 0) {
				return TEL_WITH_CODE_REGEX_PATTERN.matcher(content).matches();
			}
		} catch (Exception e) {
		}
		return true;
	}

	/**
	 * 判断是否为电话号码(不带区号)<br>
	 * 格式：6767676, 号码位数必须是7-8位,头一位不能是"0"
	 */
	public static boolean isTelNo(String content) {
		try {
			if (content != null && content.length() > 0) {
				return TEL_REGEX_PATTERN.matcher(content).matches();
			}
		} catch (Exception e) {
		}
		return true;
	}

	/**
	 * 判断是否为手机<br>
	 * 手机号码验证,11位，不知道详细的手机号码段，只是验证开头必须是1和位数
	 */
	public static boolean isCellPhone(String content) {
		try {
			if (content != null && content.length() > 0) {
				return CELL_PHONE_REGEX_PATTERN.matcher(content).matches();
			}
		} catch (Exception e) {
		}
		return true;
	}

	/**
	 * 判断是否为email<br>
	 * 用户名和网站名称必须>=1位字符 地址结尾必<br>
	 * 须是2位以上，如：cn,com,org
	 */
	public static boolean isEMail(String content) {
		try {
			if (content != null && content.length() > 0) {
				return MAIL_REGEX_PATTERN.matcher(content).matches();
			}
		} catch (Exception e) {
		}
		return true;
	}

	/**
	 * 判断是否为email<br>
	 * 检查邮政编码(中国),6位，第一位必须是非0开头，其他5位数字为0-9
	 */
	public static boolean isPostCode(String content) {
		try {
			if (content != null && content.length() > 0) {
				return POST_CODE_REGEX_PATTERN.matcher(content).matches();
			}
		} catch (Exception e) {
		}
		return true;
	}

	/**
	 * 判断是否为日期
	 */
	public static boolean isDate(String content, DateFormat format) {
		try {
			if (content != null && content.length() > 0) {
				String res;
		        long lt = new Long(content);
		        Date date = new Date(lt);
		        res = format.format(date);
				format.setLenient(false);
				format.parse(res);
				return true;
			}
		} catch (Exception e) {
		}
		
		
		return false;
	}
	
	
}
