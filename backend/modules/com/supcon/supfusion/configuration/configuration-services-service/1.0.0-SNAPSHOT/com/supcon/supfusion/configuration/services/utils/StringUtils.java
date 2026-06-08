package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.configuration.services.utils.OrchidUtils;

import java.util.*;

public abstract class StringUtils {
	/**
	 * 助记码通配符
	 */
	public static final String MNE_WILD_CARD = "\\*";
	/**
	 * 数据库通配符
	 */
	public static final String SQL_WILD_CARD = "\\%";
	public static final String SQL_ESCAPE_CHAR = "\\&";

	/**
	 * 第一个字母小写
	 * 
	 * @param str
	 * @return
	 */
	public static String firstLetterToLower(String str) {
		if (null == str) {
			return null;
		}
		if (str.length() > 0) {
			return Character.toLowerCase(str.charAt(0)) + str.substring(1);
		}
		return "";
	}

	/**
	 * 字一个字母大写
	 * 
	 * @param str
	 * @return
	 */
	public static String firstLetterToUpper(String str) {
		if (null == str) {
			return null;
		}
		if (str.length() > 0) {
			return Character.toUpperCase(str.charAt(0)) + str.substring(1);
		}
		return "";
	}

	/**
	 * Base64编码
	 * 
	 * @param code
	 * @return
	 */
	public static String encodeBase64(String code) {
		return new String(OrchidUtils.encode(code.getBytes()));

	}

	public static boolean isEmpty(String str) {
		return (str == null || str.isEmpty());
	}

	/**
	 * 是否助记码通配符
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isWildCard(String str) {
		return null != str && str.equals(MNE_WILD_CARD);
	}

	/**
	 * 转义字符串  (%作为普通字符 * 为通配符)
	 * 
	 * @param str
	 * @return
	 */
	public static String escape(String str) {
		if(null != str && str.trim().length() > 0){
			// 将字符串中所有&转义
			str = str.replaceAll(SQL_ESCAPE_CHAR, SQL_ESCAPE_CHAR + SQL_ESCAPE_CHAR);
			// 将字符串中的所有%转义
			str = str.replaceAll(SQL_WILD_CARD, SQL_ESCAPE_CHAR + SQL_WILD_CARD);
			// 包含 * 则将所有 * 替换为 %
			str = str.replaceAll(MNE_WILD_CARD, SQL_WILD_CARD);
		}
		return str;
	}
	
	/**
	 * 将列表排序后转为Set
	 * @param list
	 * @return
	 */
	public static Set<String> sort(List<String> list){
		if(null == list || list.isEmpty()){
			return null;
		}
		Collections.sort(list, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		});
		return new LinkedHashSet<String>(list);
	}
	
	/**
	 * 将Set排序后返回新的Set
	 * @param list
	 * @return
	 */
	public static Set<String> sort(Set<String> set){
		if(null == set || set.isEmpty()){
			return null;
		}
		List<String> list = new ArrayList<String>();
		list.addAll(set);
		Collections.sort(list, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		});
		return new LinkedHashSet<String>(list);
	}
}
