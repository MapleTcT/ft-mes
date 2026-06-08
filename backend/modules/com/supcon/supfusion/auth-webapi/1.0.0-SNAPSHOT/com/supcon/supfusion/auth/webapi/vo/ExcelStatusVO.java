package com.supcon.supfusion.auth.webapi.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

/**
 * Excel导入状态
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ExcelStatusVO extends VO {

    /**
     * 导入任务id
     */
    private Long id;

    /**
     * 状态
     */
    private Integer status;


    /**
     * 成功数目
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Integer addNum;

    /**
     * 更新数目
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Integer updateNum;

    /**
     * 是否有错误文件
     */
    private Boolean hasErrorFile = false;

    /**
     * 错误消息
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String errorMessage;
}
