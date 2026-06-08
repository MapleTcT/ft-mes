/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.module.registry.dto;

import java.io.Serializable;

import javax.validation.constraints.NotEmpty;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年9月3日 上午9:00:06
 */
@Data
public class AddModuleDTO implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 模块ID
     */
    @NotEmpty(message = "模块ID不能为空")
    private String moduleId;
    /**
     * 模块编号
     */
    private String moduleCode;
    /**
     * 模块名-国际化编码
     */
    @NotEmpty(message = "模块名称国际化编码不能为空")
    private String nameOfI18nCode;

}
