/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年9月12日 上午10:35:42
 */
@Data
@AllArgsConstructor
public class ImportExportDTO {
    /**
     * 导入导出任务ID
     */
    private String taskId;
    /**
     * app id
     */
    private String appId;
}
