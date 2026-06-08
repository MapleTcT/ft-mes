package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.io.Serializable;


@Data
@javax.persistence.Entity
//@Table(name = CustomerCondition.TABLE_NAME)
public class CustomerCondition extends AbstractAuditUniqueCodeEntity implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1882586341173770762L;
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
    public static final String TABLE_NAME = "ec_customer_condition";
    private String moduleCode;
    private String entityCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "VIEW_CODE")
    @Fetch(FetchMode.SELECT)
    private View view;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "DATAGRID_CODE")
    @Fetch(FetchMode.SELECT)
    private DataGrid dataGrid;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "DATACLASSIFIC_CODE")
    @Fetch(FetchMode.SELECT)
    private DataClassific dataClassific;

    @Column(length = 4000, nullable = true)
    private String jsonCondition;

    @Column(length = 1000, nullable = true, name = "CONDITION_SQL")
    private String sql;

    private Boolean projFlag;

    @Override
    protected String _getEntityName() {
        // TODO Auto-generated method stub
        return CustomerCondition.class.getName();
    }
}
