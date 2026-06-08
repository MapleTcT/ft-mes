package com.supcon.supfusion.configuration.services.openapi.freemark;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FileIconMethod implements TemplateMethodModelEx {

	public static final String FILE_TYPE_IMAGE_DIRECTORY = "/bap/static/foundation/images/icon/";
	private static Map<String, String> FILE_TYPE_IMAGE = new HashMap<String, String>();
	static {
		FILE_TYPE_IMAGE.put(".jpg", "image.gif");
		FILE_TYPE_IMAGE.put(".jpeg", "image.gif");
		FILE_TYPE_IMAGE.put(".gif", "image.gif");
		FILE_TYPE_IMAGE.put(".bmp", "image.gif");
		FILE_TYPE_IMAGE.put(".doc", "msword.gif");
		FILE_TYPE_IMAGE.put(".xls", "msexcel.gif");
		FILE_TYPE_IMAGE.put(".rar", "archive.gif");
		FILE_TYPE_IMAGE.put(".zip", "archive.gif");
		FILE_TYPE_IMAGE.put(".jar", "archive.gif");
		FILE_TYPE_IMAGE.put(".tar", "archive.gif");
		FILE_TYPE_IMAGE.put(".ppt", "mspowerpoint.gif");
		FILE_TYPE_IMAGE.put(".html", "ie.gif");
		FILE_TYPE_IMAGE.put(".htm", "ie.gif");
		FILE_TYPE_IMAGE.put(".pdf", "pdf.gif");
		FILE_TYPE_IMAGE.put(".exe", "system.gif");
		FILE_TYPE_IMAGE.put(".bat", "system.gif");
		FILE_TYPE_IMAGE.put(".txt", "text.gif");
		FILE_TYPE_IMAGE.put(".xml", "xml.gif");
		FILE_TYPE_IMAGE.put("unknown", "unknown.gif");
	}

	/***
	 * @param list
	 *           参数1:日期值
	 *           参数2：时间格式 date(默认)，dateTime(包括时分秒)，dateTimeMin(包含时分)，dateTimeHour（只含小时）
	 * 
	 ***/
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		String type="unknown";
		if (arguments.get(0) != null) {
			type=arguments.get(0).toString();
		}
		for (Map.Entry<String, String> entry : FILE_TYPE_IMAGE.entrySet()) {
			if (type.toLowerCase().endsWith(entry.getKey())) {
				return FILE_TYPE_IMAGE_DIRECTORY + entry.getValue();
			}
		}
		return FILE_TYPE_IMAGE_DIRECTORY + "unknown.gif";

	}

}