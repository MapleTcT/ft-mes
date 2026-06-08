/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年9月28日 下午2:35:23
 */
@Data
public class AssigneeVO extends VO {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public AssigneeVO(String id, String name, String taskDefKey) {
        this.id = id;
        this.name = name;
        this.taskDefKey = taskDefKey;
    }
    /**
     * 原始迁移线ID
     */
    @ApiModelProperty(value = "原始迁移线ID", name = "id", example = "Ak1112")
    private String id;
    /**
     * 节点名称
     */
    @ApiModelProperty(value = "节点名称", name = "name", example = "测试任务")
    private String name;
    /**
     * 节点ID
     */
    @ApiModelProperty(value = "节点ID", name = "taskDefKey", example = "T12222")
    private String taskDefKey;
}
