package com.supcon.supfusion.auditlog.service.bo.excel;

import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/**
 * Excel导入状态
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ExcelStatusBO {

    /**
     * 导入任务id
     */
    @ApiModelProperty(value = "导入任务id")
    private String id;
    /**
     * 状态
     */
    @ApiModelProperty(value = "Excel导入状态, 1进行中, 2成功, 3失败")
    private Integer status;

    /**
     * 是否有错误文件
     */
    @ApiModelProperty(value = "是否有错误文件")
    private Boolean hasErrorFile = false;
    /**
     * 错误文件
     */
    @ApiModelProperty(value = "错误文件名")
    private String errorFile;

    /**
     * 错误消息
     */
    @ApiModelProperty(value = "错误信息")
    private String errorMessage;
}
