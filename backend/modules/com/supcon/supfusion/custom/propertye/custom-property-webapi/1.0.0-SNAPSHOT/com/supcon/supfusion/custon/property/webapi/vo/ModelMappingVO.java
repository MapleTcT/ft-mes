package com.supcon.supfusion.custon.property.webapi.vo;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.custon.property.common.enums.FieldType;
import com.supcon.supfusion.custon.property.common.enums.ShowFormat;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * @author zhang yafei
 */
@Getter
@Setter
@ApiModel
@ToString
public class ModelMappingVO extends VO {

    @ApiModelProperty("是否启用")
    private Boolean enableCustom;

    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("是否允许为空")
    private Boolean nullable;

    @ApiModelProperty("显示类型")
    private FieldType fieldType;

    @ApiModelProperty("格式化类型")
    private ShowFormat format;

    @ApiModelProperty("关联编码")
    private String relatedKey;

    @ApiModelProperty("备注")
    private String description;

    @ApiModelProperty("系统编码是否多选")
    private Boolean multable;

    private String displayName;

    @ApiModelProperty("浮点数精度")
    @Max(value = 6, message = "浮点数位数不能大于6位")
    @Min(value = 0, message = "浮点数位数不能小于0位")
    private Integer precision;

    @ApiModelProperty("相关的属性")
    private String associatedPropertyCode;

    private JSONObject fillContent;

    @ApiModelProperty("参照视图code")
    private String refViewCode;

    @ApiModelProperty("相关的属性类型 一对一 : 1，多对一 : 2")
    private Integer associatedType;

    private PropertyVO property;
}
