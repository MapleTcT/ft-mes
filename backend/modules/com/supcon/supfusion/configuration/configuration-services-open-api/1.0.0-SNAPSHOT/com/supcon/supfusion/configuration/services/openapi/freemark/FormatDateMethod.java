package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.configuration.services.utils.DateUtils;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FormatDateMethod implements TemplateMethodModelEx {
	/***
	 * @param list
	 *           参数1:日期值
	 *           参数2：时间格式 date(默认)，dateTime(包括时分秒)，dateTimeMin(包含时分)，dateTimeHour（只含小时）
	 * 
	 ***/
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		String type="date";
		if (arguments.get(0) == null) {
			return null;
		}
		String dateStr = arguments.get(0).toString();
		if (arguments.get(1) != null) {
			type=arguments.get(1).toString();
		}
		if(type.equals("dateTime")||type.equals("dateTimeMin")||type.equals("dateTimeHour")){
			return DateUtils.formatDateTime(DateUtils.parseDateTime(dateStr));
		}
		return DateUtils.formatDate(DateUtils.parseDate(dateStr));

	}

}