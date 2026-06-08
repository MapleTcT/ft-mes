package com.supcon.supfusion.configuration.services.entity;


import com.supcon.supfusion.configuration.services.enums.FieldType;
import com.supcon.supfusion.configuration.services.enums.ShowFormat;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.*;
import java.io.Serializable;

/**
 * @author wuqi
 */
@Data
@Entity
@Table(name = CustomPropertyViewMapping.TABLE_NAME, uniqueConstraints = {@UniqueConstraint(columnNames = {"PROPERTY_CODE", "ASSOCIATED_CODE", "PROPERTY_LAYREC"}, name = "IDX_CPVM_SELECT")})
public class CustomPropertyViewMapping extends AbstractIdEntity implements Serializable {

    private static final long serialVersionUID = -789831577970060193L;

    public static final String TABLE_NAME = "base_cp_view_mapping";

    @Column
//    @BAPInternational(replace = false)
    private String displayName; // 显示名称

    @Column
    @Enumerated(EnumType.STRING)
    private FieldType fieldType; // 显示类型

    @Column
    @Enumerated(EnumType.STRING)
    private ShowFormat format; // 显示格式

    @Column
    private Boolean nullable = true; // 是否可空

    @Column
    private Boolean showCustom = false; // 显示/隐藏自定义字段

    @Column
    private Integer colspan = 1; // 合并列

    @Column
    private Integer textareaRow = 3; // textarea行数

    private Integer sort;


    @OneToOne(targetEntity = Property.class)
    @JoinColumn(name = "PROPERTY_CODE", referencedColumnName = "CODE")
    @Fetch(FetchMode.SELECT)
    private Property property;

    @Column(name = "PROPERTY_LAYREC")
    private String propertyLayRec; // 记录字段的层级关系，用“.”分隔，用于列表/参照视图和Datagrid

    @Column(name = "ASSOCIATED_CODE")
    private String associatedCode; // 关联字段

    @Lob
    private String customStyle;

    @Lob
    private String customScript;

    @Transient
    private View refView; // 参照视图

    @Transient
    private String refViewModelCode;

    @Transient
    private String relatedKey; // 建立两个自定义字段之间的关联关系，relatedKey值相同，则关系建立

    // 树型PT所需字段
    @Transient
    private String _code;

    @Transient
    private String _parentCode;

    @Transient
    private Boolean isParent = false;

    @Transient
    private String layRec;

    @Transient
    private Boolean multable;

    @Transient
    private String excelFormat;

    @Column
    private Boolean readonly;

    @Column
    private String align;

    @Column
    private Integer precision;

    @Column
    private Integer length;


    @Override
    protected String _getEntityName() {
        return getClass().getName();
    }


    public Property getProperty() {
        return property;
    }

}
