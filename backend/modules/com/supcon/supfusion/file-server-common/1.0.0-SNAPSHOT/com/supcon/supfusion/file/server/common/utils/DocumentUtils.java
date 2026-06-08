/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.file.server.common.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * 根据文件大小成最合适的单位显示
 * 
 * @author fangzhibin
 * @version $Id$
 */
public class DocumentUtils {

	public static final String FILE_TYPE_IMAGE_DIRECTORY = "/bap/static/foundation/images/icon/";
	private static Map<String, String> FILE_TYPE_IMAGE = new HashMap<String, String>();
	static {
		FILE_TYPE_IMAGE.put(".jpg", "image");
		FILE_TYPE_IMAGE.put(".jpeg", "image");
		FILE_TYPE_IMAGE.put(".gif", "image");
		FILE_TYPE_IMAGE.put(".bmp", "image");
		FILE_TYPE_IMAGE.put(".png", "image");
		FILE_TYPE_IMAGE.put(".PNG", "image");
		FILE_TYPE_IMAGE.put(".doc", "msword");
		FILE_TYPE_IMAGE.put(".docx", "msword");
		FILE_TYPE_IMAGE.put(".xls", "msexcel");
		FILE_TYPE_IMAGE.put(".xlsx", "msexcel");
		FILE_TYPE_IMAGE.put(".rar", "archive");
		FILE_TYPE_IMAGE.put(".zip", "archive");
		FILE_TYPE_IMAGE.put(".jar", "archive");
		FILE_TYPE_IMAGE.put(".tar", "archive");
		FILE_TYPE_IMAGE.put(".7z", "archive");
		FILE_TYPE_IMAGE.put(".ppt", "mspowerpoint");
		FILE_TYPE_IMAGE.put(".pptx", "mspowerpoint");
		FILE_TYPE_IMAGE.put(".html", "ie");
		FILE_TYPE_IMAGE.put(".htm", "ie");
		FILE_TYPE_IMAGE.put(".pdf", "pdf");
		FILE_TYPE_IMAGE.put(".exe", "system");
		FILE_TYPE_IMAGE.put(".bat", "system");
		FILE_TYPE_IMAGE.put(".txt", "text");
		FILE_TYPE_IMAGE.put(".properties", "text");
		FILE_TYPE_IMAGE.put(".xml", "xml");
		FILE_TYPE_IMAGE.put("unknown", "unknown");
	}
	/**
	 * KB值
	 */
	private static final long KB_SIZE = 1024;
	/**
	 * MB值
	 */
	private static final long MB_SIZE = KB_SIZE * 1024;
	/**
	 * GB值
	 */
	private static final long GB_SIZE = MB_SIZE * 1024;

	/**
	 * 显示的文件大小格式
	 */
	private static final NumberFormat FORMAT = new DecimalFormat("#,###,###.##");
	
	/**
	 * 根据文件大小成最合适的单位显示
	 * 
	 * @param input
	 * @return 转换后的大小，保留两位小数
	 */
	public static String sizeConversion(Long input) {
		String retStr;
		if (GB_SIZE - input <= 100 * MB_SIZE || input >= GB_SIZE) {
			retStr = FORMAT.format(input * 1.0 / GB_SIZE) + "G";
		} else if (MB_SIZE - input <= 100 * KB_SIZE || input >= MB_SIZE) {
			retStr = FORMAT.format(input * 1.0 / MB_SIZE) + "M";
		} else if (KB_SIZE - input <= 100 || input >= KB_SIZE) {
			retStr = FORMAT.format(input * 1.0 / KB_SIZE) + "K";
		} else {
			retStr = input + "byte";
		}
		return retStr;
	}
	
	/**
	 * 根据文件类型，取得对应的图标
	 * 
	 * @param
	 * @return
	 */
	public static String getIcon(String fileName) {
		if (fileName.indexOf(".") > 0) {
			String type = fileName.substring(fileName.lastIndexOf("."));
			if (FILE_TYPE_IMAGE.containsKey(type)) {
				return FILE_TYPE_IMAGE.get(type);
			}
		}
		//return "unknown";
		return "";
	}
}
