package com.supcon.supfusion.custon.property.webapi.vo.response;

import com.supcon.supfusion.custon.property.common.enums.AlignType;
import com.supcon.supfusion.custon.property.common.enums.FieldType;
import com.supcon.supfusion.custon.property.common.enums.ShowFormat;
import com.supcon.supfusion.custon.property.common.enums.ViewType;
import com.supcon.supfusion.custon.property.webapi.vo.PropertyVO;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;


/**
 * @author zhang yafei
 */
@Getter
@Setter
@ApiModel
@ToString
public class ViewMappingResponseVO extends VO {
    @ApiModelProperty("关联字段")
    private String associatedCode;
    @ApiModelProperty("该字段跨多少列")
    private Integer colspan;
    @ApiModelProperty("国际化key")
    private String displayName;
    @ApiModelProperty("显示字段")
    private String displayNameInternational;
    @ApiModelProperty("显示类型")
    private FieldType fieldType;
    @ApiModelProperty("格式化类型")
    private ShowFormat format;
    @ApiModelProperty("唯一主键")
    private Long id;
    @ApiModelProperty("是否为符目录")
    private Boolean isParent;
    @ApiModelProperty("")
    private String layRec;
    @ApiModelProperty("可否为空")
    private Boolean nullable;
    @ApiModelProperty("")
    private String propertyLayRec;
    @ApiModelProperty("是否显示")
    private Boolean showCustom;
    @ApiModelProperty("")
    private Integer textareaRow;
    @ApiModelProperty("")
    private String code;
    @ApiModelProperty("浮点数精度")
    private Integer precision;
    @ApiModelProperty("长度")
    private Integer length;
    @ApiModelProperty("父节点code")
    private String parentCode;
    @ApiModelProperty("对齐方式")
    private AlignType align;
    @ApiModelProperty("是否只读")
    private Boolean readonly;
    @ApiModelProperty("视图类型")
    private ViewType viewType;
    @ApiModelProperty("字段信息")
    private PropertyVO property;
    private List<ViewMappingResponseVO> list;
}
