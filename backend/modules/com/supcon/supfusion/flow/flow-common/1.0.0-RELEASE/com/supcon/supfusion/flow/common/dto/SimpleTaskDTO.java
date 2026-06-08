/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * bap专用
 * @author: zhuangmh
 * @date: 2020年9月29日 下午2:00:45
 */
@Data
@AllArgsConstructor
public class SimpleTaskDTO {

    private String taskId;
    
    private String processId;
}
