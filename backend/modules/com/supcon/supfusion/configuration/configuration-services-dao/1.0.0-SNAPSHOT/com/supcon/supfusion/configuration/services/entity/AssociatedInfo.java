package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;
import java.util.Map;


@Data
@javax.persistence.Entity
public class AssociatedInfo extends AbstractAuditUniqueCodeEntity implements Serializable {
    private static final long serialVersionUID = -5692224305381819918L;

    protected EcEnv ecEnv = EcEnv.product;
    public static final int ONE_TO_ONE = 1;
    public static final int MANY_TO_ONE = 2;
    public static final int ONE_TO_MANY = 3;
    public static final int MANY_TO_MANY = 4;


    private Integer type;// 1 - 1->1 ; 2 - N->1 ; 3 - 1->N ; 4 - N->N

    private Boolean isMainAssociated = false; // 关联到主模型的属性

    @ManyToOne
    @JoinColumn(name = "TARGET_PROPERTY_CODE", referencedColumnName = "code")
    private Property targetProperty;

    @ManyToOne
    @JoinColumn(name = "ORIGINAL_PROPERTY_CODE", referencedColumnName = "code")
    private Property originalProperty;// 对于这个属性，如果type=0的情况下，这个属性可以新建的，也就是可以没有ID的，在type>0的情况下，这个属性是存在的，直接选择的，用于在关联表中使用.

    private String description;

    private String propertyName; //关联属性名，如：createStaff(关联Staff),createDepartment(关联Department)

    @Transient
    private List<Map<String, Object>> refViewInfo;


    public Boolean getIsMainAssociated() {
        if (null == isMainAssociated) {
            isMainAssociated = false;
        }
        return isMainAssociated;
    }

    @Override
    protected String _getEntityName() {
        return getClass().getName();
    }


    public Property getTargetProperty() {
        return targetProperty;
    }


    public Property getOriginalProperty() {
        return originalProperty;
    }
}