package com.supcon.supfusion.portal.manager.impl;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.i18n.service.api.MessageResourceService;
import com.supcon.supfusion.portal.common.exception.PortalErrorEnum;
import com.supcon.supfusion.portal.common.exception.PortalException;
import com.supcon.supfusion.portal.manager.PortalAdapter;
import com.supcon.supfusion.rbac.api.IMenuInfoApiService;
import com.supcon.supfusion.rbac.api.dto.MenuInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

@Slf4j
@Service
public class PortalAdapterImpl implements PortalAdapter {

    @Autowired(required = false)
    private MessageResourceWrapper messageResourceWrapper;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private MessageResourceService messageResourceService;
    @Autowired
    private IMenuInfoApiService iMenuInfoApiService;

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
            return messageResourceWrapper.getMessage(code);
        } catch (NoSuchMessageException e) {
            log.error(e.getMessage(), e);
        }
        return null;
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
            throw new PortalException(PortalErrorEnum.ADD_I18N_ERROR, result.getMessage());
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
        if (StringUtils.isEmpty(key)) {
            throw new PortalException(PortalErrorEnum.ADD_I18N_ERROR);
        }
        return key;
    }

    @Override
    public List<MenuInfoDTO> findPermissionMenu(Long userId) {
        if (null == userId) {
            return null;
        }
        try {
            return iMenuInfoApiService.findPermissionMenu(userId);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
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
