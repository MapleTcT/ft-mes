/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年8月19日 上午10:36:07
 */
@Data
@AllArgsConstructor
public class DiagramDTO {
    /**
     * app id
     */
    private String appId;
    /**
     * 流程实例名称
     */
    private String processName;
    /**
     * 流程编号
     */
    private String processKey;
}
