/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年8月28日 下午5:33:41
 */
@Data
public class AuditDTO {
    /**
     * 分支ID
     */
    private String id;
    /**
     * 排序
     */
    private int order;
    /**
     * 决定流程走向
     */
    private String value;
    /**
     * 分支名称
     */
    private String name;
    /**
     * 分支线类型 0: 普通迁移线 1: 驳回线
     */
    private int type;
    /**
     * 指向目标节点
     */
    private String targetDefKey;
    
}
