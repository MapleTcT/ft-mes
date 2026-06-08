/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年8月19日 下午8:35:13
 */
@Data
@AllArgsConstructor
public class LeaderTaskDtoAdapter {
    private String nodeId;
    private String processDefinitionId;
}
