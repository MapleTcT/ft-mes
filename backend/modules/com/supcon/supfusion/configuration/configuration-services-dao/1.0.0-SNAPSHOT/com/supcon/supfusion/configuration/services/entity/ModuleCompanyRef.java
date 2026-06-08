package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@javax.persistence.Entity
@Table(name = ModuleCompanyRef.TABLE_NAME)
public class ModuleCompanyRef implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String TABLE_NAME = "module_company_ref";

    @Id
    private Long id;

    @Column(name = "COMPANY_ID")
    private Long companyId;

    @Column(name = "MODULE_CODE")
    private String moduleCode;
}
