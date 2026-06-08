package com.supcon.supfusion.custon.property.webapi.vo.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author zhang yafei
 */

@Getter
@Setter
@ApiModel
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TreeResponseVO extends VO {

    @ApiModelProperty("节点code")
    private String code;

    @ApiModelProperty("是否为父节点")
    private Boolean isParent;

    @ApiModelProperty("节点name")
    private String name;

}
