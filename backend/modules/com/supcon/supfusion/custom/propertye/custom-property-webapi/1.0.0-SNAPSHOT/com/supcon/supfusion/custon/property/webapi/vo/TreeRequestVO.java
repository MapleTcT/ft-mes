package com.supcon.supfusion.custon.property.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;

/**
 * @author zhang yafei
 */
@Getter
@Setter
@ApiModel
@ToString
public class TreeRequestVO extends VO {

    @ApiModelProperty("父节点code")
    private String code;

    @ApiModelProperty("查询类型")
    @NotEmpty(message = "查询类型不能为空，module、entity、model 、view")
    private String type;


}
