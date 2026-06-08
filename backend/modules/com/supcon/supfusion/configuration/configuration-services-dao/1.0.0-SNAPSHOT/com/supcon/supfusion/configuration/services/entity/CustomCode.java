package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Index;

import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;

@Data
@NoArgsConstructor
@javax.persistence.Entity
@Table(name = CustomCode.TABLE_NAME)
public class CustomCode extends AbstractAuditUniqueCodeEntity implements Serializable{
    private static final long serialVersionUID = 8124940880891138996L;
    public static final String TABLE_NAME = "ec_custom_code";
    @Index(name = "Idx_ECCUSTOMCODE_ModuleCode")
    private String moduleCode;
    private String entityCode;
    @Index(name = "Idx_ECCUSTOMCODE_MODELCODE")
    private String modelCode;
    private String type;
    private String subType;
    @Lob
    private String customCode;
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
    public CustomCode(String entityCode, String modelCode, String customCode, String type, String subType) {
        this.entityCode = entityCode;
        this.modelCode = modelCode;
        this.customCode = customCode;
        this.type = type;
        this.subType = subType;
    }

    @Override
    protected String _getEntityName() {
        return CustomCode.class.getName();
    }

}
