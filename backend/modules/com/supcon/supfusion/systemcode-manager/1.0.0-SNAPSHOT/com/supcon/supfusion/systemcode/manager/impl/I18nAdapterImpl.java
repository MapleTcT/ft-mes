package com.supcon.supfusion.systemcode.manager.impl;

import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.i18n.context.SupfusionMessageSource;
import com.supcon.supfusion.i18n.service.api.MessageResourceService;
import com.supcon.supfusion.systemcode.manager.I18nAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class I18nAdapterImpl implements I18nAdapter {

//    @Autowired
//    MessageResourceWrapper messageResourceWrapper;

    @Autowired
    MessageSource messageSource;

    @Autowired
    MessageResourceService messageResourceService;

    @Override
    public String getLocalMessage(String code, Object[] args, Locale locale) {
        try {
            return messageSource.getMessage(code, args, locale);
        }catch (NoSuchMessageException e){
            log.error(e.getMessage(),e);
        }
        return null;
    }

    @Override
    public String getRemoteMessage(String code) {
        try {
            if (messageSource instanceof SupfusionMessageSource) {
                return ((SupfusionMessageSource) messageSource).getMessageNotBlank(code);
            }
            return messageSource.getMessage(code, null, code, getCurrentLocale());
        }catch (NoSuchMessageException e){
            log.error(e.getMessage(),e);
        }
        return null;
    }

    @Override
    public Map<String, String> MessageResourceSearchOne(String value) {
        return messageResourceService.MessageResourceSearchOne(value, LocaleContextHolder.getLocale().toString());
    }

    private Locale getCurrentLocale() {
        Locale locale = RpcContext.getContext().getLanguage();
        if (locale == null && RpcContext.getContext().getRequest() != null) {
            locale = RpcContext.getContext().getRequest().getLocale();
        }
        if (locale == null) {
            locale = LocaleContextHolder.getLocale();
        }
        return locale;
    }
}
