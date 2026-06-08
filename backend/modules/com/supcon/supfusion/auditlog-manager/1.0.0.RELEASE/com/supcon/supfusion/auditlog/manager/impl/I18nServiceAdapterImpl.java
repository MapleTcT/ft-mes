package com.supcon.supfusion.auditlog.manager.impl;

import com.supcon.supfusion.auditlog.manager.I18nServiceAdapter;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.i18n.service.api.MessageResourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author caokele
 */
@Slf4j
@Service
public class I18nServiceAdapterImpl implements I18nServiceAdapter {
    @Autowired
    private MessageResourceService messageResourceService;

    @Override
    public List<String> searchKeys(String value) {
        String language = RpcContext.getContext().getLanguage().toString();
        Map<String, String> map = messageResourceService.MessageResourceSearchOne(value, language);
        return new ArrayList<>(map.keySet());
    }

    @Override
    public String searchValue(String key) {
        String lang = RpcContext.getContext().getLanguage().toString();
        Map<String, String> map = messageResourceService.messageResourceGetByKeyOneLanguage(key, lang);
        if (CollectionUtils.isEmpty(map)){
            return null;
        }
        return map.get(key);
    }
}
