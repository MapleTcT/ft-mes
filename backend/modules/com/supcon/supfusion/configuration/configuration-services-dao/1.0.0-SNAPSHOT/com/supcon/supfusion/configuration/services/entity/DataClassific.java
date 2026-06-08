package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 数据分类实体
 *
 * @author fangzhibin
 */
@Data
@javax.persistence.Entity
//@Table(name = DataClassific.TABLE_NAME)
public class DataClassific extends AbstractAuditUniqueCodeEntity implements Serializable {

    private static final long serialVersionUID = 4031229281556394293L;

    public static final String TABLE_NAME = "ec_data_classific";
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
    private String name;

//    @BAPInternational(fieldName = "displayNameInternational", replace = false)
    private String displayName;

    @Column(name = "DC_CONDITION")
    private String condition;

    @ManyToOne
    @JoinColumn(name = "DATA_GROUP_CODE", referencedColumnName = "code")
    @Fetch(FetchMode.SELECT)
    @Index(name = "ind_classific_group_code", columnNames = {"dataGroup"})
    private DataGroup dataGroup;

    private Long sort;

    @Column(columnDefinition = "INTEGER", length = 1)
    private Boolean isDefault;

    private Boolean projFlag;

    private String moduleCode;

    private String entityCode;

    public Boolean getIsDefault() {
        return isDefault == null ? false : isDefault;
    }

    @Override
    protected String _getEntityName() {
        return DataClassific.class.getName();
    }



    public DataGroup getDataGroup() {
        return dataGroup;
    }

}
