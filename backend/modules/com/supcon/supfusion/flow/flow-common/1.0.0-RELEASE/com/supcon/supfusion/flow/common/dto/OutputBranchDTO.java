/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年9月28日 下午3:08:59
 */
@Data
public class OutputBranchDTO {
    /**
     * 节点ID
     */
    private String id;
    /**
     * 输出分支名称
     */
    private String name;
    /**
     * 分支线类型 0: 普通迁移线 1: 驳回线
     */
    private int type;
    /**
     * 目标节点ID
     */
    private NodeDTO targetNode;
    /**
     * 是否指派
     */
    private Boolean assign;
}
