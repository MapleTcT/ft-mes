package com.supcon.supfusion.printer.interapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/**
 * 打印模板页面关联表entity
 * @author yuyimao
 * @date 2020/10/16 5:01 下午
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrinterTemplateRelationPageVO extends VO {

    /**
     * 模板编号
     */
    @ApiModelProperty(value = "模板编号")
    private Long templateId;

    /**
     * 页面编号
     */
    @ApiModelProperty(value = "页面编号")
    private String pageId;

    /**
     * 模型编号
     */
    @ApiModelProperty(value = "模型编号")
    private String modelCode;
}
