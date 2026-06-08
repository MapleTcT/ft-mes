package com.supcon.supfusion.rbac.manager;

import org.springframework.context.NoSuchMessageException;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;
import java.util.Map;

public interface II18nAdapter {
    /**获取本服务的国际化
     * Try to resolve the message. Treat as an error if the message can't be found.
     * @param code the code to lookup up, such as 'calculator.noRateSet'
     * @param args an array of arguments that will be filled in for params within
     * the message (params look like "{0}", "{1,date}", "{2,time}" within a message),
     * or {@code null} if none.
     * @param locale the locale in which to do the lookup
     * @return the resolved message
     * @throws NoSuchMessageException if the message wasn't found
     * @see java.text.MessageFormat
     */
    String getLocalMessage(String code, @Nullable Object[] args, Locale locale);

    /**获取全部服务的国际化
     * Try to resolve the message. Treat as an error if the message can't be found.
     * @param code the code to lookup up, such as 'calculator.noRateSet'
     * @param args an array of arguments that will be filled in for params within
     * the message (params look like "{0}", "{1,date}", "{2,time}" within a message),
     * or {@code null} if none.
     * @param locale the locale in which to do the lookup
     * @return the resolved message
     * @throws NoSuchMessageException if the message wasn't found
     * @see java.text.MessageFormat
     */
    String getRemoteMessage(String code, @Nullable Object[] args, Locale locale);

    String getRemoteMessageBlank(String code, @Nullable Object[] args, Locale locale);


    /**
     * @description: 国际化模糊搜索
     * @return:
     * @author: 袁阳
     * @date: 2020/8/18
     */
    Map<String, String> MessageResourceSearchOne(String value);

    String MessageResourceGetByKeyOneLanguage(String key, String language);

    String messageResourceGetByKey(String key);

    void refreshI18n();
}
