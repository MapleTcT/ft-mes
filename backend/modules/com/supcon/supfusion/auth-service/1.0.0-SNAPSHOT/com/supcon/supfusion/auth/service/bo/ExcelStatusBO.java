package com.supcon.supfusion.auth.service.bo;

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
    private Long id;
    /**
     * 状态
     */
    private Integer status;

    /**
     * 是否有错误文件
     */
    private Boolean hasErrorFile = false;
    /**
     * 错误文件
     */
    private String errorFile;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 成功数目
     */
    private Integer addNum;

    /**
     * 失败数目
     */
    private Integer updateNum;

}
