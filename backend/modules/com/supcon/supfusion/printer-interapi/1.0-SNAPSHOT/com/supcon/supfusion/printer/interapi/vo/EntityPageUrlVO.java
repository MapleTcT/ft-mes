package com.supcon.supfusion.printer.interapi.vo;

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
     * 实体名
     */
    @NotBlank(message = Constants.PARAM_ENTITY_SOURCE_NAME_NECESSARY)
    @ApiModelProperty(value = "实体名称", required = true)
    private String name;

    /**
     * 实体iframe url
     */
    @NotBlank(message = Constants.PARAM_ENTITY_URL_NECESSARY)
    @ApiModelProperty(value = "实体url", required = true)
    private String entityUrl;

    /**
     * 来源
     */
    @NotNull(message = Constants.PARAM_SOURCE_NECESSARY)
    @ApiModelProperty(value = "实体注册来源", required = true)
    private Integer source;

    /**
     * 是否启用
     */
    private Boolean valid = true;
}
