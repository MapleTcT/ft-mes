/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.module.registry;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author: zhuangmh
 * @date: 2020年7月11日 下午6:04:51
 */
public enum ModuleErrorEnum implements ErrorDefinition {
    MODULE_INIT_ERROR(100114000, "模块基础数据初始化失败"),
    
    MODULE_NOT_FOUND_ERROR(100114001, "模块不存在"),
    
    MODULE_HAS_EXIST_ERROR(100114002, "模块已存在");
    /**
     * 异常码
     */
    private int code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;
    
    ModuleErrorEnum(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /**
     * @see com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition#getCode()
     */
    @Override
    public Integer getCode() {
        return this.code;
    }

    /**
     * @see com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition#getMessage()
     */
    @Override
    public String getMessage() {
        return this.defaultMessage;
    }

    /**
     * @see com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition#getInfo()
     */
    @Override
    public String getInfo() {
        return null;
    }

}
