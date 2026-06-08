package com.supcon.supfusion.printer.interapi.vo;

import com.supcon.supfusion.printer.common.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ParamConditionVO {

    /**
     * 表单ID（页面id）
     */
    @NotBlank(message = Constants.PARAM_PAGEID_NECESSARY)
    @ApiModelProperty(value = "表单id（页面id）", required = true)
    private String id;

    /**
     * 透传条件
     */
    private String params;
    
    private String viewCode;
}