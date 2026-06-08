/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.openapi;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月5日 下午2:37:42
 */
@Data
@ApiModel(value = "ProcessLogVO", description = "流程日志数据模型")
public class ProcessLogVO extends VO {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 待办名称
     */
    @ApiModelProperty(value = "待办任务名称", name = "taskName", dataType = "String", example = "审批")
    private String taskName;
    /**
     * 操作过程描述
     */
    @ApiModelProperty(value = "操作过程描述", name = "operateDesc", dataType = "String", example = "经由张三审批")
    private String operateDesc;
    /**
     * 
     */
    @ApiModelProperty(value = "操作者(人员名称)", name = "operator", dataType = "String", example = "test")
    private String operator;
    /**
     * 审批结果
     */
    @ApiModelProperty(value = "审批结果", name = "auditResult", dataType = "String", example = "同意")
    private String auditResult;
    /**
     * 备注
     */
    @ApiModelProperty(value = "备注", name = "comment", dataType = "String", example = "")
    private String comment;
    /**
     * 操作类型
     */
    @ApiModelProperty(value = "操作类型", name = "type", dataType = "String", example = "")
    private String type;
    /**
     * 操作时间
     */
    @ApiModelProperty(value = "操作时间", name = "createTime", example = "2020-11-10 11:10:09")
    private String createTime;
}
