package com.supcon.supfusion.custon.property.webapi.vo;

import com.supcon.supfusion.custon.property.common.enums.DbColumnType;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotBlank;

/**
 * @author zhang yafei
 */

@Getter
@Setter
@ApiModel
@ToString
public class PropertyVO extends VO {

    @ApiModelProperty("字段唯一表示code")
    @NotBlank(message = "字段唯一表示不能为空")
    private String code;

    @ApiModelProperty("字段名称")
    private String name;

    @ApiModelProperty("字段类型")
    private DbColumnType type;

    private String fillcontent;

}
