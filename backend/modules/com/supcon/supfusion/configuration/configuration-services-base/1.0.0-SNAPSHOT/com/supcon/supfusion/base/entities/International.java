package com.supcon.supfusion.base.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/20
 */
@Data
@NoArgsConstructor
public class International implements Serializable {

    private static final long serialVersionUID = -7490626048898672066L;

    public static Map<String, String> languageMap = new HashMap<>();

    static {
        languageMap.put("zh_CN", "中文");
        languageMap.put("en_US", "英文");
        languageMap.put("zh_HK", "中文");
    }

    public static final String I18N_KEY = "i18n_key";
    public static final String MODULE_CODE = "moduleCode";
    public static final String I18N_VALUE = "i18n_value";

    public International(String key, String value, String language) {
        this.key = key;
        this.value = value;
        this.language = language;
    }

    private String key;
    private String value;
    private String language;

    public String getLanguageKey() {
        return this.language;
    }
}
