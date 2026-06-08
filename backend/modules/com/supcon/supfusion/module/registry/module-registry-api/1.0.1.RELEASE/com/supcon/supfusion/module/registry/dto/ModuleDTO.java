/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.module.registry.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: zhuangmh
 * @date: 2020年7月2日 下午3:18:17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = Include.NON_NULL)
public class ModuleDTO implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 模块ID, 全局唯一
     */
    private String moduleId;
    /**
     * 模块编号
     */
    private String moduleCode;
    /**
     * 模块名称, 支持国际化
     */
    private String moduleName;
    /**
     * 模块类型 {@link com.supcon.supfusion.module.registry.ModuleTypeEnum}
     */
    private String moduleType;
    /**
     * 排序
     */
    private Integer indexof;
    /**
     * @param moduleId
     * @param moduleName
     * @param moduleType
     * @param indexof
     */
    public ModuleDTO(String moduleId, String moduleName, String moduleType, Integer indexof) {
        this.moduleId = moduleId;
        this.moduleName = moduleName;
        this.moduleType = moduleType;
        this.indexof = indexof;
    }
    
}
