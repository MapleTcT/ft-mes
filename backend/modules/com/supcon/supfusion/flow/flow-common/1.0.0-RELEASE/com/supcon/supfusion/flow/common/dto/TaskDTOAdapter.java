/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import java.util.Set;

import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月3日 上午11:28:12
 */
@Data
public class TaskDTOAdapter {
    private String activityName;
    private String processDefinitionId;
    private String instanceId;
    private String taskName;
    private String processId;
    // 驳回,撤回,指派等固定用户
    private Set<String> specialUser;
    private Set<String> candidateUser;
    private String formNo;
    private String initiator;
    private String processName;
    private String processKey;
    private Integer version;
    private String source;
    private String formData;
    private Integer addInstance;
    private Integer readonly;
    private Integer enableComment;
    private Integer enableDelete;
    private Integer showlog;
    private String pageUrl;
    private boolean isMultiple;
    private String protocols;
}
