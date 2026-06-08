/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.utils;


import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Administrator
 * 
 */
public class MneCodeGenterate {

	/**
	 * 生成所有的助记码数据
	 * 
	 * @param property
	 *            传入的需要生成的助记码数据
	 * @return
	 */
	public static List<String> mneCodeTupleGenerate(String property) {
		if(property.length() > 10){ // 助记码最多十位 防止内存溢出
			property = property.substring(0, 10);
		}
		property=property.toLowerCase();
		List<String> mneCodeList = new ArrayList<String>();
		List<String> pinYinList = getPingYin(property);
		List<String> pinYinHeadList = getPinYinHeadChar(property);
		if(pinYinList!=null && !pinYinList.isEmpty()){
			mneCodeList.addAll(pinYinList);
		}
		if(pinYinHeadList!=null && !pinYinHeadList.isEmpty()){
			mneCodeList.addAll(pinYinHeadList);
		}
		return mneCodeList;
	}

	// 将汉字转换为全拼
	private static List<String> getPingYin(String src) {
		List<Object> list = new ArrayList<Object>();
		char[] t1 = null;
		t1 = src.toCharArray();
		String[] t2 = new String[t1.length];
		HanyuPinyinOutputFormat t3 = new HanyuPinyinOutputFormat();
		t3.setCaseType(HanyuPinyinCaseType.LOWERCASE);
		t3.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		t3.setVCharType(HanyuPinyinVCharType.WITH_V);
		int t0 = t1.length;
		try {
			for (int i = 0; i < t0; i++) {
				// 判断是否为汉字字符
				List<String> pyString = new ArrayList<String>();
				String t4 = ""; // 多音字拼音字母相同处理
				if (Character.toString(t1[i]).matches("[\\u4E00-\\u9FA5]+")) {
					t2 = PinyinHelper.toHanyuPinyinStringArray(t1[i], t3);

					for (int j = 0; j < t2.length; j++) {
						// 判断该拼音 为多音
						if (t4.indexOf(t2[j]) < 0) {
							t4 += t2[j];
							pyString.add(t2[j]);
						}
					}
				} else {
					t4 += Character.toString(t1[i]);
					pyString.add(Character.toString(t1[i]));
				}
				// 如果只输入一个汉字，无需调用递归直接返回该多音组合
				if (t0 == 1) {
					return pyString;
				}
				list.add(pyString);
			}
			return getPinYinList(list);
		} catch (BadHanyuPinyinOutputFormatCombination e1) {
			e1.printStackTrace();
		}
		return null;
	}

	// 返回中文的首字母
	private static List<String> getPinYinHeadChar(String str) {
		List<Object> list = new ArrayList<Object>();
		int t0 = str.toCharArray().length;
		for (int j = 0; j < str.length(); j++) {
			List<String> pyString = new ArrayList<String>();
			char word = str.charAt(j);
			String convert = "";
			String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(word);
			if (pinyinArray != null) {
				// 如果存在多音
				for (int k = 0; k < pinyinArray.length; k++) {
					if (convert.indexOf(String.valueOf(pinyinArray[k].charAt(0))) < 0) {
						convert += pinyinArray[k].charAt(0);
						pyString.add(Character.toString(pinyinArray[k].charAt(0)));
					}
				}
			} else {
				convert += word;
				pyString.add(Character.toString(word));
			}
			if (t0 == 1) {
				return pyString;
			}
			list.add(pyString);
		}
		return getPinYinList(list);
	}

	// 将字符串转移为ASCII码
	@SuppressWarnings("unused")
	private static String getCnASCII(String cnStr) {
		StringBuffer strBuf = new StringBuffer();
		byte[] bGBK = cnStr.getBytes();
		for (int i = 0; i < bGBK.length; i++) {
			strBuf.append(Integer.toHexString(bGBK[i] & 0xff));
		}
		return strBuf.toString();
	}

	// 递归方法，生成多音组合
	@SuppressWarnings("unchecked")
	private static List<String> getPinYinList(List<Object> list) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		List<String> pinYinList = new ArrayList<String>();
		for (int i = 0; i < list.size() - 1; i++) {
			List<String> strList1 = (List<String>) list.get(0);
			List<String> strList2 = (List<String>) list.get(1);
			for (int j = 0; j < strList1.size(); j++) {
				for (int k = 0; k < strList2.size(); k++) {
					StringBuffer sb = new StringBuffer();
					sb = sb.append(strList1.get(j) + strList2.get(k));
					pinYinList.add(sb.toString());
				}
			}
			list.set(0, pinYinList);
			list.remove(1);
			getPinYinList(list);
		}
		return (List<String>) list.get(0);
	}

}
