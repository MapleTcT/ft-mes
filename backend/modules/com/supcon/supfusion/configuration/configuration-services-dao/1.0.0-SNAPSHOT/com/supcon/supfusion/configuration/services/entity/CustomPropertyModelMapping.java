package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.configuration.services.enums.FieldType;
import com.supcon.supfusion.configuration.services.enums.ShowFormat;
import com.supcon.supfusion.configuration.services.utils.SerializeUitls;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

import javax.persistence.Entity;
import javax.persistence.*;
import java.io.Serializable;
import java.util.Map;

//import com.supcon.orchid.ec.utils.ConditionUtil;

/**
 * @author wuqi
 */
@Data
@Entity
@Table(name = CustomPropertyModelMapping.TABLE_NAME)
public class CustomPropertyModelMapping extends AbstractIdEntity implements Serializable {

    private static final long serialVersionUID = -4142272239804897659L;

    public static final String TABLE_NAME = "base_cp_model_mapping";

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
    private String fillContent; // 用于系统编码类型字段，存储系统编码code

    @Column
    private Boolean multable = false; // 用于系统编码类型字段，表示是否是多选系统编码

    @Column
    private Boolean seniorSystemCode = false; // 用于系统编码类型字段，表示是否是高级系统编码

    @OneToOne(targetEntity = Property.class)
    @JoinColumn(name = "ASSOCIATED_PROPERTY_CODE", referencedColumnName = "CODE")
    @Fetch(FetchMode.SELECT)
    private Property associatedProperty; // 用于对象类型字段，记录关联字段

    @Column
    private Integer associatedType; // 关联关系：1->1 : 1，N->1 : 2

    @OneToOne(targetEntity = View.class)
    @JoinColumn(name = "REFERENCE_VIEW_CODE", referencedColumnName = "CODE")
    @Fetch(FetchMode.SELECT)
    private View refView; // 参照视图

    @Column
    private Boolean nullable = true; // 是否可空

    @Column
    private Boolean enableCustom = false; // 是否启用/停用自定义字段

    @OneToOne(targetEntity = Property.class)
    @JoinColumn(name = "PROPERTY_CODE", referencedColumnName = "CODE")
    @Fetch(FetchMode.SELECT)
    @Index(name = "IDX_CPMM_PROPERTY")
    private Property property;

    @ManyToOne(targetEntity = Model.class)
    @JoinColumn(name = "MODEL_CODE", referencedColumnName = "CODE")
    @Fetch(FetchMode.SELECT)
    @Index(name = "IDX_CPMM_MODEL")
    private Model model;


    @Column
    private String description;

    private Integer sort;

    @Column
    private String relatedKey; // 建立两个自定义字段之间的关联关系，relatedKey值相同，则关系建立

    private Integer precision;

    @Transient
	public Map<String, Object> getFillContentMap() {
		return SerializeUitls.deserializeJson(fillContent);
	}

    @Override
    protected String _getEntityName() {
        return getClass().getName();
    }



    public Property getAssociatedProperty() {
        return associatedProperty;
    }


    public View getRefView() {
        return refView;
    }


    public Property getProperty() {
        return property;
    }


    public Model getModel() {
        return model;
    }
}
