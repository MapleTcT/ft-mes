package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.configuration.services.annotation.International;
import com.supcon.supfusion.configuration.services.i18n.DispalyNameInternationalSerialzer;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 数据分组实体
 *
 * @author fangzhibin
 */
@Data
@javax.persistence.Entity
//@Table(name = DataGroup.TABLE_NAME)
public class DataGroup extends AbstractAuditUniqueCodeEntity implements Serializable {

    private static final long serialVersionUID = 5998773847562664544L;

    public static final String TABLE_NAME = "ec_data_group";
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
    @ManyToOne(fetch=FetchType.EAGER,cascade=CascadeType.ALL)
    @JoinColumn(name = "TARGETMODEL_CODE", referencedColumnName = "code")
    @Fetch(FetchMode.SELECT)
//    @Transient
    private Model targetModel;// 当前dataGroup的关联模型

    @ManyToOne
    @JoinColumn(name = "VIEW_CODE", referencedColumnName = "code")
    @Fetch(FetchMode.SELECT)
    @Index(name = "idx_DATA_GROUP")
    private View view;

    private String name;

    @International
    @JsonSerialize(using = DispalyNameInternationalSerialzer.class)
    private String displayName;

    @Column(columnDefinition = "INTEGER", length = 1)
    private Boolean isMultiple = false;

//    @OneToMany(mappedBy = "dataGroup",fetch = FetchType.EAGER)
//    @Fetch(FetchMode.SELECT)
//    @OrderBy(clause = "code asc")
    @Transient
    private Set<DataClassific> dataClassifics = new LinkedHashSet<DataClassific>();

    private Long sort;

    private Boolean projFlag;

    private String layoutName;

    private String moduleCode;

    private String entityCode;


    public Boolean getIsMultiple() {
        return isMultiple == null ? false : isMultiple;
    }


    @Override
    protected String _getEntityName() {
        return DataGroup.class.getName();
    }


    public Model getTargetModel() {
        return targetModel;
    }


    public View getView() {
        return view;
    }


    public Set<DataClassific> getDataClassifics() {
        return dataClassifics;
    }
}
