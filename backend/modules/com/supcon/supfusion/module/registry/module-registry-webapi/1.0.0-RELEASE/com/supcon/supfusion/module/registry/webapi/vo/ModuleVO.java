/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.module.registry.webapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年7月22日 下午7:29:15
 */
@Data
@AllArgsConstructor
@ApiModel("模块视图")
public class ModuleVO {
    /**
     * 模块ID
     */
    @ApiModelProperty(value = "模块ID", name = "moduleId", dataType = "String", example = "workflow(String)")
    private String moduleId;
    /**
     */
    @ApiModelProperty(value = "模块编号", name = "moduleCode", dataType = "String", example = "workflow_1.0(String)")
    private String moduleCode;
    /**
     * 模块名
     */
    @ApiModelProperty(value = "模块名称", name = "moduleName", dataType = "String", example = "工作流(String)")
    private String moduleName;
    /**
     * 模块类型 {@link com.supcon.supfusion.module.registry.ModuleTypeEnum}
     */
    @ApiModelProperty(value = "模块类型", name = "moduleType", dataType = "String", example = "system")
    private String moduleType;
}
