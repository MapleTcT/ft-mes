/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.module.registry.util;
/**
 * @author: zhuangmh
 * @date: 2020年7月11日 下午5:02:34
 */
public class Constants {
    
    private Constants() {
        throw new IllegalStateException("Constants is utility class, do not instantiate");
    }
    
    /**
     * ##############################################################################################
     * 常用变量
     * ##############################################################################################
     */
    public static final int ENABLED = 1; // 启用
    public static final String SPLIT_DOT = "\\."; // 点号分隔符
    /**
     * ##############################################################################################
     * 数据库字段
     * ##############################################################################################
     */
    public static final String COLUMN_MODULE_ID = "module_id"; // 模块编号
    public static final String COLUMN_MODULE_NAME = "module_name"; // 模块名称
    public static final String COLUMN_MODULE_TYPE = "module_type"; // 模块类型
    public static final String COLUMN_TENANT_ID = "tenant_id"; // 租户ID
}
