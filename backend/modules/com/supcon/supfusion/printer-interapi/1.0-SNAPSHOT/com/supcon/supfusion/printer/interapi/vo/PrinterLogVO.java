package com.supcon.supfusion.printer.interapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.printer.common.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrinterLogVO extends VO {

    /**
     * 模板id
     */
    @NotNull(message = Constants.PARAM_TEMPLATEID_NECESSARY)
    @ApiModelProperty(value = "打印模板id", required = true)
    private Long templateId;

    /**
     * 页面id
     */
    @NotBlank(message = Constants.PARAM_PAGEID_NECESSARY)
    @ApiModelProperty(value = "页面id", required = true)
    private String pageId;
}
