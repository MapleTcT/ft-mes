package com.supcon.supfusion.auditlog.manager;

import java.util.List;

/**
 * @author caokele
 */
public interface I18nServiceAdapter {

    /**
     * 根据值模糊搜索key
     * @param value 国际化值
     * @return 国际化key列表
     */
    List<String> searchKeys(String value);

    /**
     * 根据key搜索value
     * @param value
     * @return
     */
    String searchValue(String value);
}
