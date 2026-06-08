package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@javax.persistence.Entity
//@Table(name = MsModuleRelation.TABLE_NAME)
public class MsModuleRelation extends AbstractAuditUniqueCodeEntity implements Serializable {

    private static final long serialVersionUID = -5153034072057039773L;

    public static final String TABLE_NAME = "ec_msmodule_relation";

    @ManyToOne
    @JoinColumn(name = "MSMODULE_CODE", referencedColumnName = "CODE")
    @Fetch(FetchMode.SELECT)
    private MsModule msModule;


    @Override
    protected String _getEntityName() {
        return "MsModuleRelation";
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MsModuleRelation)) {
            return false;
        } else {
            if (getCode() == null) {
                return false;
            } else if (((MsModuleRelation) obj).getMsModule() == null) {
                return false;
            } else if (this.getCode() == null) {
                return false;
            } else if (this.msModule == null) {
                return false;
            } else if (!this.msModule.equals(((MsModuleRelation) obj).getMsModule())) {
                return false;
            } else if (!this.getCode().equals(((MsModuleRelation) obj).getCode())) {
                return false;
            }
        }
        return true;
    }


    public MsModule getMsModule() {
        return msModule;
    }
}
