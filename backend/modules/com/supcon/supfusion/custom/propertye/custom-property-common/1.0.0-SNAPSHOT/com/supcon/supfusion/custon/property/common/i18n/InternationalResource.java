package com.supcon.supfusion.custon.property.common.i18n;

import com.supcon.supfusion.framework.cloud.i18n.context.support.RemoteBundleMessageSource;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Component
public class InternationalResource {

	@Autowired
	private MessageResourceWrapper messageResourceWrapper;

	public String getI18nValue(String key) {
		return this.getI18nValue(key, null, null);
	}

	public String getI18nValue(String key, Object[] args) {
		return this.getI18nValue(key, args, null);
	}
	public String getI18nValue(String key, Locale locale) {
		return this.getI18nValue(key, null, locale);
	}

	public String getI18nValue(String key, Object[] args, Locale language) {
		if (StringUtils.isEmpty(key)) {
			return key;
		}
		return messageResourceWrapper.getMessageNotBlankWithArgument(key, args, language);

	}
	public void refreshInternationalization() {
		messageResourceWrapper.initiativeRefreshCache();
	}

}