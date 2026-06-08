package com.supcon.supfusion.systemconfig.common.constants;

/**
 * @author lifangyuan
 */
public class Constants {

    private Constants() {
    }

    //------------------------系统分类错误消息------------------------
    /**
     * 配置分类code必填
     */
    public static final String CATALOG_CODE = "catalog code is require";

    /**
     * 配置分类系统配置
     */
    public static final String CATALOG_SYSTEM_TYPE = "catalog type must more then 0";


    /**
     * 配置分类系统APP配置
     */
    public static final String CATALOG_APP_TYPE = "catalog type must small then 3";

    /**
     * 配置分类名称必填
     */
    public static final String CATALOG_NAME = "catalog name is require";

    /**
     * 配置分类顺序必填
     */
    public static final String CATALOG_ORDER = "catalog order is require";

    /**
     * 配置分类appCode必填
     */
    public static final String CATALOG_APPCODE = "catalog appCode is require";

    /**
     * 添加配置分类必须不为空
     */
    public static final String CATALOG_IS_NULL = "catalog can not null";

    /**
     * 添加配置分类必须有一个值
     */
    public static final String CATALOG_LIST = "catalog list must has one value";

    /**
     * 配置项code
     */
    public static final String CONFIG_CODE = "config code is require";

    /**
     * 配置项名称
     */
    public static final String CONFIG_NAME = "config name is require";

    /**
     * 配置项名称
     */
    public static final String CONFIG_ORDER = "config order is require";

    /**
     * 配置项名称
     */
    public static final String CONFIG_TYPE = "config type is require";


    /**
     * 下拉选项标签
     */
    public static final String OPTIONALVALUE_LABEL = "option label is require";

    /**
     * 配置项名称
     */
    public static final String OPTIONALVALUE_VALUE = "option value is require";

    /**
     * 配置项名称
     */
    public static final String OPTIONALVALUE_ORDER = "option order is require";

    /**
     * app配置appcode
     */
    public static final String APP = "app";
    /**
     * 系统配置appcode
     */
    public static final String SYSTEM = "system";

    /**
     * 数字类型
     */
    public static final String NUMBER = "number";

    /**
     * xml文本必须不为空
     */
    public static final String XML_CONTENT_IS_NULL = "xml content can not null";

    /**
     * xml文本必须有一个值
     */
    public static final String XML_CONTENT_LIST = "xml content list must has one value";

    public static final String LINUX = "linux";

    public static final String WINDOWS = "windows";

}
