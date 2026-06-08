package com.supcon.supfusion.scheduler.manager;

import com.supcon.supfusion.framework.cloud.common.result.Result;
import org.springframework.context.NoSuchMessageException;
import org.springframework.lang.Nullable;

import java.util.Locale;
import java.util.Map;

public interface SchedulerI18nAdapter {
    /**
     * 获取本服务的国际化
     * Try to resolve the message. Treat as an error if the message can't be found.
     *
     * @param code   the code to lookup up, such as 'calculator.noRateSet'
     * @param args   an array of arguments that will be filled in for params within
     *               the message (params look like "{0}", "{1,date}", "{2,time}" within a message),
     *               or {@code null} if none.
     * @param locale the locale in which to do the lookup
     * @return the resolved message
     * @throws NoSuchMessageException if the message wasn't found
     * @see java.text.MessageFormat
     */
    String getLocalMessage(String code, @Nullable Object[] args, Locale locale);

    /**
     * 获取全部服务的国际化
     * Try to resolve the message. Treat as an error if the message can't be found.
     *
     * @param code   the code to lookup up, such as 'calculator.noRateSet'
     * @param args   an array of arguments that will be filled in for params within
     *               the message (params look like "{0}", "{1,date}", "{2,time}" within a message),
     *               or {@code null} if none.
     * @param locale the locale in which to do the lookup
     * @return the resolved message
     * @throws NoSuchMessageException if the message wasn't found
     * @see java.text.MessageFormat
     */
    //    String getRemoteMessage(String code, @Nullable Object[] args, Locale locale);
    String getRemoteMessage(String code);

    /**
     * @description: 国际化模糊搜索
     * @return:
     */
    Map<String, String> MessageResourceSearchOne(String value);

    String MessageResourceGetByKeyOneLanguage(String key, String language);

    /**
     * @return
     * @Author kk.C
     * @Description 新增或修改单个国际化
     * @Date 2020/10/24 13:48
     * @Param
     **/
    Result messageResourceAddOrUpdateOne(Map<String, Object> map);

    /**
     * @Author kk.C
     * @Description 删除多个国际化
     * @Date 2020/10/24 15:29
     * @Param [keys]
     * @return com.supcon.supfusion.framework.cloud.common.result.Result
     **/
    Result messageResourceDeleteKeys(String[] keys);

    /**
     * @Author kk.C
     * @Description 选中某种类型是创建国际化key(根据模块名称创建国际化key值)  根据模块名称创建国际化key值
     * @Date 2020/10/24 16:17
     * @Param [moduleCode]
     * @return java.lang.String
     **/
    String initI18nKey(String moduleCode);

    //    String messageResourceGetByKey(String key);
}
