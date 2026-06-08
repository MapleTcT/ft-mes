package com.supcon.supfusion.configuration.services.i18n;

import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.i18n.service.api.MessageResourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
public class InternationalResource {

	@Autowired
	private InternationalService internationalService;

	private static MessageResourceWrapper messageResourceWrapper;

	private static InternationalResource internationalResource;

	private static String defaultLanguage = "zh_CN";

	private static MessageResourceService messageResourceService;

	public static String getDefaultLanguage() {
		return (null == defaultLanguage) ? "zh_CN" : defaultLanguage;
	}
	@Autowired(required = false)
	public void setMessageResourceWrapper(MessageResourceWrapper messageResourceWrapper) {
		InternationalResource.messageResourceWrapper = messageResourceWrapper;
	}

	@Autowired(required = false)
	public void setMessageResourceService(MessageResourceService messageResourceService) {
		InternationalResource.messageResourceService = messageResourceService;
	}
	public static String get(String key) {
		return get(key, (Object[]) null);
	}

	public static String get(String key, Object[] args) {
		if (null == key || internationalResource == null) {
			return key;
		}
		return internationalResource.internationalService.getI18nValue(key, args, defaultLanguage);
	}

	public static List<Object> getMessageKeys(String value, String... languages) {
		List<Object> keys = new ArrayList<Object>();

		if (keys.isEmpty()) {
			keys.add("");
		}

		return keys;
	}
	/**
	 * 此处忽略锁问题
	 *
	 * @param key
	 * @param language
	 * @return
	 */
	public static String get(String key, String language) {
		if (null == key) {
			return null;
		}
		if ("".equals(key)) {
			return "";
		}
		String message = key;
		Locale locale = getLocale(language);
		message = getMessage(key, null, locale);
		if ("".equals(message)) {
			return key;
		}
		return message;
	}

	private static Locale getLocale(String language) {
		if(null == language || language.trim().length() == 0){
			return Locale.SIMPLIFIED_CHINESE;
		}
		String[] strings = language.split("_");
		if (strings.length >= 2){
			return new Locale(strings[0].toLowerCase(),strings[1].toUpperCase());
		}
		return Locale.SIMPLIFIED_CHINESE;
	}
	/**
	 *  如果找不到，返回空字符串
	 */
	private static String getMessage(String key, Object[] args, Locale locale) {
		String message = "";
		message = messageResourceWrapper.getMessage(key);
		if (null != message) {
			if (args != null && args.length > 0) {
				message = MessageFormat.format(message, args);
			}
			return message;
		}
		Map<String, String> map = messageResourceService.messageResourceGetByKeyOneLanguage(key, locale.toString());
		if (null != map && !map.isEmpty()) {
			message = map.get(key);
			if (message != null && args != null && args.length > 0) {
				message = MessageFormat.format(message, args);
			}
		}
		return message == null ? "" : message;
	}
	public static List<String> getAllLanguages() {
		return new ArrayList<>(messageResourceService.getAllLanguage().keySet());
	}
	@PostConstruct
	public void init() {
		internationalResource = this;
		internationalResource.internationalService = this.internationalService;
	}

}