package com.supcon.supfusion.custon.property.webapi.vo;

import com.supcon.supfusion.custon.property.common.enums.AlignType;
import com.supcon.supfusion.custon.property.common.enums.FieldType;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;


/**
 * @author zhang yafei
 */
@Getter
@Setter
@ApiModel
@ToString
public class ViewMappingVO extends VO {

    @ApiModelProperty("国际化key")
    private String displayName;

    private Integer id;

    @ApiModelProperty("关联编码")
    @NotBlank(message = "associatedCode不能为空")
    private String associatedCode;

    private String propertyLayRec;

    @ApiModelProperty("是否启用")
    private Boolean showCustom;

    @ApiModelProperty("是否允许为空")
    private Boolean nullable;

    @ApiModelProperty("显示类型")
    private FieldType fieldType;

    @ApiModelProperty("格式化类型")
    private String format;

    @ApiModelProperty("对齐方式")
    private AlignType align;

    @ApiModelProperty("字段显示列数")
    private Integer colspan;

    @ApiModelProperty("浮点数精度")
    @Max(value = 6, message = "浮点数位数不能大于6位")
    @Min(value = 0, message = "浮点数位数不能小于0位")
    private Integer precision;

    @ApiModelProperty("字符长度")
    @Max(value = 2000, message = "字符长度不能大于2000位")
    @Min(value = 0, message = "字符长度不能小于0位")
    private Integer length;

    @ApiModelProperty("是否只读")
    private Boolean readonly;

    private PropertyVO property;
}
