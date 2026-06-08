package com.supcon.supfusion.printer.interapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrinterLabelVO extends VO {
    /**
     * 标签名称
     */
    @ApiModelProperty(value = "标签名称")
    private String labelName;
}
