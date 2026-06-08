package com.supcon.supfusion.scheduler.manager.impl;

import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.i18n.service.api.MessageResourceService;
import com.supcon.supfusion.scheduler.manager.SchedulerI18nAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class SchedulerI18nAdapterImpl implements SchedulerI18nAdapter {

    @Autowired(required = false)
    MessageResourceWrapper messageResourceWrapper;
    @Autowired
    MessageSource messageSource;
    @Autowired
    MessageResourceService messageResourceService;

    @Override
    public String getLocalMessage(String code, Object[] args, Locale locale) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public String getRemoteMessage(String code) {
        try {
            return messageResourceWrapper.getMessageNotBlank(code);
        } catch (NoSuchMessageException e) {
            log.error(e.getMessage(), e);
        }
        return code;
    }

    @Override
    public Map<String, String> MessageResourceSearchOne(String value) {
        try {
            return messageResourceService.MessageResourceSearchOneMatchCase(value, LocaleContextHolder.getLocale().toString());
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public String MessageResourceGetByKeyOneLanguage(String key, String language) {
        try {
            Map<String, String> map = messageResourceService.MessageResourceGetByKeyOneLanguage(key, language);
            String value;
            if (!ObjectUtils.isEmpty(map)) {
                value = map.get(key);
            } else {
                value = key;
            }
            return value;
        } catch (Exception e) {
            log.error(e.getMessage());
            return key;
        }
    }

    @Override
    public Result messageResourceAddOrUpdateOne(Map<String, Object> map) {
        Result result = new Result();
        try {
            result = messageResourceService.messageResourceAddOrUpdateOne(map);
            messageResourceWrapper.initiativeRefreshCache();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return result;
    }

    @Override
    public Result messageResourceDeleteKeys(String[] keys) {
        return messageResourceService.messageResourceDeleteKeys(keys);
    }

    @Override
    public String initI18nKey(String moduleCode) {
        String key = messageResourceService.initI18nKey(moduleCode);
        return key;
    }

    //    @Override
    //    public String messageResourceGetByKey(String key) {
    //        Locale locale = LocaleContextHolder.getLocale();
    //        if (!ObjectUtils.isEmpty(this.getRemoteMessage(key, null, locale))) {
    //            return this.getRemoteMessage(key, null, locale);
    //        } else if (!ObjectUtils.isEmpty(this.MessageResourceGetByKeyOneLanguage(key, locale.toString()))) {
    //            return this.MessageResourceGetByKeyOneLanguage(key, locale.toString());
    //        }
    //        return key;
    //    }
}
