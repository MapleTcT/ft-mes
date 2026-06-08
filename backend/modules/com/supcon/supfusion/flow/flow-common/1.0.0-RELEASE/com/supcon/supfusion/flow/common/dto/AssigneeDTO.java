/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import java.io.Serializable;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年9月28日 下午2:35:23
 */
@Data
@AllArgsConstructor
public class AssigneeDTO implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * 原始迁移线ID
     */
    private String id;
    /**
     * 节点名称
     */
    private String name;
    /**
     * 节点ID
     */
    private String taskDefKey;
    /**
     * 被指派者
     */
    private Set<String> users;
}
