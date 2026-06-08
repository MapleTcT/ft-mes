package com.supcon.supfusion.printer.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.printer.common.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EntityPageUrlVO extends VO {

    /**
     * 数据来源
     */
    @NotNull(message = Constants.PARAM_SOURCE_NECESSARY)
    @ApiModelProperty(value = "数据来源", required = true)
    private Integer source;

    @NotBlank(message = Constants.PARAM_ENTITY_SOURCE_NAME_NECESSARY)
    @ApiModelProperty(value = "实体来源名称", required = true)
    private String name;

    @NotBlank(message = Constants.PARAM_ENTITY_URL_NECESSARY)
    @ApiModelProperty(value = "实体对象加载页面地址", required = true)
    private String entityUrl;
}
