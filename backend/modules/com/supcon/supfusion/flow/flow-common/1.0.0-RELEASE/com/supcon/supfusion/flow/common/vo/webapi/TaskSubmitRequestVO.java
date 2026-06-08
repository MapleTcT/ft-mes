/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月2日 下午1:54:04
 */
@Data
@ApiModel(value = "待办提交参数模型")
public class TaskSubmitRequestVO extends VO {
    /**
     * 反序列化时的版本比较
     */
    private static final long serialVersionUID = 1L;

    /**
     * 流程编号
     */
    @NotEmpty(message = "待办ID")
    @ApiModelProperty(value = "待办ID", name = "taskId", dataType = "String", example = "123456", required = true)
    private String taskId;
    /**
     * 表单JSON数据
     */
    @ApiModelProperty(value = "表单JSON数据", name = "formData", dataType = "String", example = "{\"id\": 1, \"name\": \"zhangsan\"}")
    private String formData;
    /**
     * 待办提交附带的信息
     */
    @ApiModelProperty(value = "提交备注等信息", name = "audit", example = "{\"id\": \"K1236\", \"name\": \"分支名称\", \"value\": \"分支值,决定流程走向\", "
            + "\"comment\": \"备注\", \"type\": \"分支线类型  0: 普通迁移线 1: 驳回线\"}")
    private AuditVO audit;
    /**
     * 提交备注
     */
    @ApiModelProperty(value = "备注", name = "comment", example = "请假超过5天, 不同意")
    private String comment;
    /**
     * 指派者列表
     */
    @ApiModelProperty(value = "指派者列表", name = "assigns", example = "[{\"taskDefKey\": \"111223\", \"assignees\": [\"999\",\"101\"]}]")
    private List<AssigneeRequestVO> assigns;
    
}
