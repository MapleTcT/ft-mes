/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年11月28日 上午10:34:14
 */
@Data
@Api(value = "FlowChartResponseVO", description = "流程图模型")
public class FlowChartResponseVO implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * 当前环节ID
     */
    @ApiModelProperty(value = "当前环节ID", name = "activeKeys", dataType = "Set", example = "[\"UserTask_1606468192933\"]")
    private Set<String> activeKeys;
    
    @ApiModelProperty(value = "流程图JSON数据", name = "json", dataType = "String", example = "{}")
    private String json;
    
    @ApiModelProperty(value = "当前环节信息", name = "nodeInfo", dataType = "List", example = "[{\"recipient\": \"张三,李四\", \"taskDefKey\": \"UserTask_1606468192933\"}]")
    private List<NodeInfo> nodeInfo;
    
    @Data
    public class NodeInfo implements Serializable {
        
        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        /**
         * 显示人员名称,多个用逗号隔开
         */
        private String recipient;
        /**
         * 当前环节ID
         */
        private String taskDefKey;
    }
    
}
