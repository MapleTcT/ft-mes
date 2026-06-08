/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.openapi.freemark;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 
 * 
 * @author zhuyuyin
 * @version $Id$
 */
@Component
public class FormatDateTimeMethod implements TemplateMethodModelEx {
	private static final String DEFAULT_DATEZERO_FORMAT = "yyyy-MM-dd 0:00:00";
	private static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd 23:59:59";

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		Date date = new Date();
		String parttern;
		if (arguments != null && arguments.get(0) != null) {
			parttern = arguments.get(0).toString();
			if (parttern.equals("today")) {
				parttern = new SimpleDateFormat(DEFAULT_DATETIME_FORMAT).format(date);
			} else if (parttern.equals("beforeYear")) {
				Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.YEAR, -1);
				parttern = new SimpleDateFormat(DEFAULT_DATETIME_FORMAT).format(calendar.getTime());
			} else if (parttern.equals("currentTime")) {
				parttern = new SimpleDateFormat(DEFAULT_DATETIME_FORMAT).format(date);
			} else if (parttern.equals("firstday")) {
				Calendar calendar = Calendar.getInstance();
				int day = calendar.getActualMinimum(Calendar.DAY_OF_MONTH);
				calendar.set(Calendar.DATE, day);
				parttern = new SimpleDateFormat(DEFAULT_DATEZERO_FORMAT).format(calendar.getTime());
			} else if (parttern.equals("lastday")) {
				Calendar calendar = Calendar.getInstance();
				int day = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
				calendar.set(Calendar.DATE, day);
				parttern = new SimpleDateFormat(DEFAULT_DATE_FORMAT).format(calendar.getTime());
			} else if (parttern.equals("nextsevenday")) {
				Calendar calendar = Calendar.getInstance();
				/*
				 * int day = calendar.get(Calendar.DAY_OF_MONTH);
				 * calendar.set(Calendar.DATE, day+7);
				 */
				calendar.add(Calendar.DATE, 7);
				parttern = new SimpleDateFormat(DEFAULT_DATETIME_FORMAT).format(calendar.getTime());
			}
		} else {
			parttern = new SimpleDateFormat(DEFAULT_DATETIME_FORMAT).format(date);
		}
		return parttern;
	}

}
