package com.supcon.supfusion.systemcode.manager;

import java.util.Locale;
import java.util.Map;

public interface I18nAdapter {
    /**
     * 获取本服务的国际化
     */
    String getLocalMessage(String code, Object[] args, Locale locale);

    /**
     * 获取全部服务的国际化
     */
    String getRemoteMessage(String code);

    /**
     * 国际化模糊搜索
     */
    Map<String, String> MessageResourceSearchOne(String value);
}
