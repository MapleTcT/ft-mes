/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年8月13日 下午1:16:45
 */
@Data
public class CandidateGroupDTO {
    
    public CandidateGroupDTO(String taskKey, String taskName) {
        this.taskKey = taskKey;
        this.taskName = taskName;
    }
    /**
     * 人工节点ID
     */
    private String taskKey;
    /**
     * 人工节点名称
     */
    private String taskName;
    /**
     * 部门组
     */
    private String depart;
    /**
     * 岗位组
     */
    private String position;
    /**
     * 角色组
     */
    private String role;
    
}
