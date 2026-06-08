package com.supcon.supfusion.custon.property.webapi.vo.response;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author zhang yafei
 */
@Data
public class PropertyResponseVO extends VO {

    @ApiModelProperty("字段唯一表示code")
    @NotBlank(message = "字段唯一表示不能为空")
    private String code;

    @ApiModelProperty("字段名国际化key")
    private String displayName;

    @ApiModelProperty("字段显示名称")
    private String displayNameInternational;

}
