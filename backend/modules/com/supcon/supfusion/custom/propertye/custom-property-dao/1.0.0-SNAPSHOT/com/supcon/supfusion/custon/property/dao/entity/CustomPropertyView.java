package com.supcon.supfusion.custon.property.dao.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.custon.property.common.enums.AlignType;
import com.supcon.supfusion.custon.property.common.enums.FieldType;
import com.supcon.supfusion.custon.property.common.enums.ShowFormat;
import com.supcon.supfusion.custon.property.dao.entity.base.AbstractEntity;
import lombok.Data;

/**
 * @author wuqi
 */
@Data
@TableName(value = "base_cp_view_mapping",autoResultMap = true)
public class CustomPropertyView extends AbstractEntity {

    private static final long serialVersionUID = -789831577970060193L;

    @TableId
    private Long id;

    private String displayName; // 显示名称

    private FieldType fieldType; // 显示类型

    private ShowFormat format; // 显示格式

    private Boolean nullable = true; // 是否可空

    private Boolean showCustom = false; // 显示/隐藏自定义字段

    private Integer colspan = 1; // 合并列

    private Integer textareaRow = 3; // textarea行数

    private Integer sort;

    @TableField(value = "property_layrec")
    private String propertyLayRec; // 记录字段的层级关系，用“.”分隔，用于列表/参照视图和Datagrid

    private String associatedCode; // 关联字段


    private String customStyle;


    private String customScript;

    private Boolean readonly;   //是否只读

    //对其方式,默认居中
    private AlignType align;

    private Integer precision;

    private Integer length;

    private String propertyCode;


//    @TableField(exist = false)
//    private Property property;


    public AlignType getAlign() {
        if (align == null){
            return AlignType.left;
        }
        return align;
    }
}
