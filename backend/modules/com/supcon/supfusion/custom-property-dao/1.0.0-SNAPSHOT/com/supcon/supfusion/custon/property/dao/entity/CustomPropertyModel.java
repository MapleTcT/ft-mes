package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.custon.property.common.enums.FieldType;
import com.supcon.supfusion.custon.property.common.enums.ShowFormat;
import com.supcon.supfusion.custon.property.dao.entity.base.AbstractEntity;
import lombok.Data;


/**
 * @author wuqi
 */
@Data
@TableName(value = "base_cp_model_mapping",autoResultMap = true)
public class CustomPropertyModel extends AbstractEntity {

    private static final long serialVersionUID = -4142272239804897659L;


    @TableId
    private Long id;

    private String displayName; // 显示名称

    private FieldType fieldType; // 显示类型


    private ShowFormat format; // 显示格式

    private String fillContent; // 用于系统编码类型字段，存储系统编码code

    private Boolean multable = false; // 用于系统编码类型字段，表示是否是多选系统编码

    private Boolean seniorSystemCode = false; // 用于系统编码类型字段，表示是否是高级系统编码

    private Integer associatedType; // 关联关系：1->1 : 1，N->1 : 2

    private Boolean nullable = true; // 是否可空

    private Boolean enableCustom = false; // 是否启用/停用自定义字段

    private String description;

    private Integer sort;

    private String relatedKey; // 建立两个自定义字段之间的关联关系，relatedKey值相同，则关系建立

    private Integer precision;

    private String propertyCode;
//    @TableField(exist = false)
//    private Property property;

    private String modelCode;
//    @TableField(exist = false)
//    private Model model;

    private String referenceViewCode;
//    @TableField(exist = false)
//    private View refView; // 参照视图

    private String associatedPropertyCode;
//    @TableField(exist = false)
//    private Property associatedProperty; // 用于对象类型字段，记录关联字段
}
