/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.module.registry.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年7月11日 下午4:47:24
 */
@Data
@TableName("mod_module_registry")
public class ModulePO extends BaseEntity {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 唯一ID
     */
    private Long id;
    /**
     * 模块ID
     */
    private String moduleId;
    /**
     * 模块编号
     */
    private String moduleCode;
    /**
     * 模块名(国际化code)
     */
    private String moduleName;
    /**
     * 模块类型 {@link com.supcon.supfusion.module.registry.ModuleTypeEnum}
     */
    private String moduleType;

}
