package com.supcon.supfusion.custon.property.webapi.vo.response;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.custon.property.common.enums.FieldType;
import com.supcon.supfusion.custon.property.common.enums.ShowFormat;
import com.supcon.supfusion.custon.property.webapi.vo.AssociatedPropertyVO;
import com.supcon.supfusion.custon.property.webapi.vo.PropertyVO;
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
public class ModelMappingResponseVO extends VO {
    private String associatedPropertyCode;
    private AssociatedPropertyVO associatedProperty;
    @ApiModelProperty("相关的属性类型")
    private Integer associatedType;
    @ApiModelProperty("关联编码")
    private String relatedKey;
    @ApiModelProperty("浮点数精度")
    private Integer precision;
    @ApiModelProperty("多选系统编码")
    private Boolean multable;
    private String description;
    private String displayName;
    private String displayNameInternational;
    private Boolean enableCustom;
    private FieldType fieldType;
    private ShowFormat format;
    private JSONObject fillContent;
    private Long id;
    private Boolean nullable;
    private String refViewCode;
    private String moduleCode;
    private PropertyVO property;
}
