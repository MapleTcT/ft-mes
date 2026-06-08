package com.supcon.supfusion.base.i18n;

import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.i18n.service.api.MessageResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;

/**
 * <p>
 * 国际化资源存储类。重点在于启动时一次载入。<br/>
 * 由于国际化泛滥使用，从性能进一步考虑可能用Ehcache存储不是最高效。
 * </p>
 * <p>
 * 资源来源：
 * <li>1. 启动foundation的时候会从数据库中载入所有国际化资源；</li>
 * <li>2. 装载某个bundle时利用tracker去搜索包中的国际化资源文件并载入。</li>
 * </p>
 *
 * @author songjiawei
 */
@Component
public class InternationalBaseResource implements Serializable, InitializingBean, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(InternationalBaseResource.class);
    /*
     * 设置默认国际化语言
     */
    private static String defaultLanguage = "zh_CN";
    private static final String CUSTOM = "CUSTOM_";
    private final static String PREFIX = "LANGUAGE_";

    private static ApplicationContext applicationContext;

    public static void setDefaultLanguage(String language) {
        defaultLanguage = language;
    }

    public static String getDefaultLanguage() {
        return (null == defaultLanguage) ? "zh_CN" : defaultLanguage;
    }

    //远程获取国际化
    private static MessageResourceWrapper messageResourceWrapper;
    // entityconf服务获取国际化
    private static MessageResourceService messageResourceService;

    @Autowired(required = false)
    public void setMessageResourceWrapper(MessageResourceWrapper messageResourceWrapper) {
        InternationalBaseResource.messageResourceWrapper = messageResourceWrapper;
    }

    @Autowired(required = false)
    public void setMessageResourceService(MessageResourceService messageResourceService) {
        InternationalBaseResource.messageResourceService = messageResourceService;
    }

    /**
     * 此处忽略锁问题
     *
     * @param key
     * @return
     */
    public static String get(String key) {
        return get(key, defaultLanguage);
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

    /**
     * 根据国际化值模糊搜索国际化key，不区分大小写
     *
     * @param value    国际化值
     * @param language 语言
     * @return
     */
    public static Set<String> getKeysByValueLike(String value, String language) {
        Map<String, String> keyValueMap = messageResourceService.MessageResourceSearchOneMatchCase(value, language);
        return new HashSet<String>(keyValueMap.keySet());
    }

    private static Locale getLocale(String language) {
        if (null == language || language.trim().length() == 0) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        String[] strings = language.split("_");
        if (strings.length >= 2) {
            return new Locale(strings[0].toLowerCase(), strings[1].toUpperCase());
        }
        return Locale.SIMPLIFIED_CHINESE;
    }

    /**
     * 根据国际化值查询对应国际化key
     *
     * @param value
     * @param languages TODO
     * @return
     */
    @Deprecated
    public static List<Object> getMessageKeys(String value, String... languages) {
        return null;
    }

    /**
     * 此处忽略锁问题
     *
     * @param key
     * @param language
     * @return 如果找不到, 返回空字符串
     */
    public static String getByLanguage(String key, String language) {
        if (null == key) {
            return null;
        }
        if ("".equals(key)) {
            return "";
        }
        Locale locale = getLocale(language);
        String message = getMessage(key, null, locale);
        return message;
    }

    /**
     * 传参数的国际化值获取，采用默认语言“zh_CN”
     *
     * @param key
     * @param args 占位符对应的值
     * @return
     */
    public static String get(String key, Object... args) {
        return get(key, defaultLanguage, args);
    }

    /**
     * 传参数的国际化值获取
     *
     * @param key
     * @param language
     * @param args     占位符对应的值
     * @return
     */
    public static String get(String key, String language, Object... args) {
        if (null == key || null == language) {
            return null;
        }
        if ("".equals(key)) {
            return "";
        }
        //根据语言和主键查询国际化值
        Locale locale = getLocale(language);
        String message = getMessage(key, args, locale);
        if ("".equals(message)) {
            return key;
        }
        return message;
    }


    public static List<String> getAllLanguages() {
        return new ArrayList<>(messageResourceService.getAllLanguage().keySet());
    }


    @Override
    @Deprecated
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (InternationalBaseResource.applicationContext == null) {
            InternationalBaseResource.applicationContext = applicationContext;
        }
    }

    /**
     * 如果找不到，返回空字符串
     */
    private static String getMessage(String key, Object[] args, Locale locale) {
        String message = "";
        message = messageResourceWrapper.getMessageWithArgument(key, args, locale);
        if (null != message) {
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

    /**
     * 如果找不到，返回空字符串
     */
    public static String getMessageNotBlank(String key) {
        String message = "";
        message = messageResourceWrapper.getMessageNotBlank(key);
        return message;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}