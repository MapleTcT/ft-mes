/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.openapi;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.validation.annotation.Validated;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月2日 下午1:54:04
 */
@Data
@ApiModel(value = "TaskSubmitRequestVO", description = "待办提交参数模型")
@Validated
public class TaskSubmitRequestVO extends BaseRequestVO {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    /**
     * 流程编号
     */
    @ApiModelProperty(value = "待办ID", name = "taskId", dataType = "String", example = "123456", required = true)
    @Pattern(regexp = "^[1-9]\\d+", message = "非法参数taskId")
    private String taskId;
    /**
     * 表单JSON数据
     */
    @ApiModelProperty(value = "表单JSON数据", name = "formData", dataType = "String", example = "{\"id\": 1, \"name\": \"zhangsan\"}")
    private String formData;
    /**
     * 待办提交附带的信息
     */
    @ApiModelProperty(value = "提交备注等信息", name = "audit", example = "{\"seqKey\": \"K1236\", \"value\": \"分支值,决定流程走向\"}")
    @Valid
    private AuditRequestVO audit;
    /**
     * 提交备注
     */
    @ApiModelProperty(value = "备注", name = "comment", example = "同意请假")
    private String comment;
    /**
     * 指派者列表
     */
    @ApiModelProperty(value = "指派者列表", name = "assigns", example = "[{\"taskDefKey\": \"111223\", \"assignees\": [\"999\",\"101\"], \"type\": \"1\"}]")
    private List<AssigneeRequestVO> assigns;
    
}
