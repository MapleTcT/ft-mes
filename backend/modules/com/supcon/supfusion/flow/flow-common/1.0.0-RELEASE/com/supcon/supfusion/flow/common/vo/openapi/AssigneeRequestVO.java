/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.openapi;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * @author: zhuangmh
 * @date: 2020年9月28日 下午2:35:23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssigneeRequestVO extends VO {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * 节点ID
     */
    @ApiModelProperty(value = "节点ID", name = "taskDefKey", example = "F123456")
    private String taskDefKey;
    /**
     * 被指派者
     */
    @ApiModelProperty(value = "用户名", name = "users", example = "[\"test\"]")
    private Set<String> users;
}
