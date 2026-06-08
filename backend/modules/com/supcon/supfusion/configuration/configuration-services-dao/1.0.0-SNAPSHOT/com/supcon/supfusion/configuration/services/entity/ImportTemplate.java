package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractCodeEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.Transient;
import java.io.Serializable;

/**
 * 导入导出模板配置
 */
@Data
@javax.persistence.Entity
//@Table(name = ImportTemplate.TABLE_NAME)
public class ImportTemplate extends AbstractCodeEntity implements Serializable {
    private static final long serialVersionUID = -1078280457268639594L;
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
    public static final String TABLE_NAME = "ec_import_template";
    @Lob
    private String value;
    @Column(name = "PROJ_FLAG")
    private Boolean projFlag;

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    @Override
    protected String _getEntityName() {
        return ImportTemplate.class.getName();
    }

}
