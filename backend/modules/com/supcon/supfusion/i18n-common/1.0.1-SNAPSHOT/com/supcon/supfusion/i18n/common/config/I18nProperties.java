package com.supcon.supfusion.i18n.common.config;


import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.*;

@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "supfusion.i18n")
public class I18nProperties {
    /**
     * 环境 product  or  productDev
     */
    @Value("${supfusion.i18n.profiles:product}")
    private String profile = "product";

    /**
     * 同时上传excel人数限制
     */
    private Integer xlsxUploadMaxNum = 15;

    /**
     * 单个文件最大 值  单位 MB
     */
    private Integer xlsxUploadMaxSize = 15;

    /**
     * 国际化key 字符长度限制
     */
    private Integer i18nKeyLengthNumDefa = 255;

    /**
     * 国际化value 字符长度限制 (不超过多少字符)
     */
    private Integer i18nValueLengthNumDefa = 500;

    /**
     * 国际化自己的国际化资源路径
     */
    private String i18nResourcePath= "i18n/i18n/";

    /**
     * 国际化自己的国际化资源code
     */
    private String i18nResourceCode = "i18n";

    /**
     * 国际化自己的国际化资源版本
     */
    private String i18nResourceVersion = "i18n202008190930";

    /**
     * 系统默认模块的国际化资源路径
     */
    private String defaultResourcePath = "i18n/sys/";

    /**
     * 系统默认模块的国际化资源code
     */
    private String defaultResourceCode = "sys";

    /**
     * 系统默认模块的国际化资源版本
     */
    @Value("${supfusion.i18n.default-resource-version:sys202007211025}")
    private String defaultResourceVersion;

    /**
     * 系统默认 的时区
     */
    private String defaultUtc = "UTC+8";
    /**
     * 系统默认的 语言类型
     */
    private String defaultLanguage = "zh_CN";

    /**
     * 环境 资源文件的目录
     */
    private String fileStoragePath;
}
