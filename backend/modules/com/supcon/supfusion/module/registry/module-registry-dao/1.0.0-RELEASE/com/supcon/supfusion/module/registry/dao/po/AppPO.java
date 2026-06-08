/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.module.registry.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;

import lombok.Data;

@Data
@TableName("mod_module_app_rel")
public class AppPO extends BaseEntity {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 唯一ID
     */
    private Long id;
    /**
     * appId
     */
    private String appId;
    /**
     * 模块ID
     */
    private String moduleId;
}
