package com.supcon.supfusion.custon.property.webapi.vo.response;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author zhang yafei
 */
@Data
public class RefViewResponseVO extends VO {
    @ApiModelProperty("视图code")
    private String code;

    @ApiModelProperty("视图国际化key")
    private String displayName;

    @ApiModelProperty("视图显示名称")
    private String displayNameInternational;

    @ApiModelProperty("视图名字")
    private String name;
}
