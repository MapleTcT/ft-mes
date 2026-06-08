/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年9月28日 下午2:35:23
 */
@Data
@JsonInclude(value = Include.NON_NULL)
public class AssigneeVO extends VO {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public AssigneeVO(String seqKey, String name, String taskDefKey) {
        this.seqKey = seqKey;
        this.name = name;
        this.taskDefKey = taskDefKey;
    }
    /**
     * 原始迁移线ID
     */
    @ApiModelProperty(value = "原始迁移线ID", name = "seqKey", example = "Ak1112")
    private String seqKey;
    /**
     * 节点名称
     */
    @ApiModelProperty(value = "节点名称", name = "name", example = "测试任务")
    private String name;
    
    @ApiModelProperty(value = "目标节点key", name = "taskDefKey", example = "USERTASK_7845112")
    private String taskDefKey;
}
