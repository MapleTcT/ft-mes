package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;

@Data
@javax.persistence.Entity
//@Table(name = ModuleRelation.TABLE_NAME)
public class ModuleRelation extends AbstractAuditUniqueCodeEntity implements Serializable {

    private static final long serialVersionUID = -5153034072057039773L;

    public static final String TABLE_NAME = "ec_module_relation";

    @ManyToOne
    @JoinColumn(name = "TARGET_MODULE_CODE", referencedColumnName = "CODE")
    @Fetch(FetchMode.SELECT)
    private Module target;

    @ManyToOne
    @JoinColumn(name = "MODULE_CODE", referencedColumnName = "CODE")
    @Fetch(FetchMode.SELECT)
    private Module module;

    private Boolean projFlag;


    @Override
    protected String _getEntityName() {
        return "ModuleRelation";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ModuleRelation)) {
            return false;
        } else {
            if (((ModuleRelation) obj).getCode() == null) {
                return false;
            } else if (((ModuleRelation) obj).getModule() == null) {
                return false;
            } else if (((ModuleRelation) obj).getTarget() == null) {
                return false;
            } else if (getCode() == null) {
                return false;
            } else if (this.module == null) {
                return false;
            } else if (this.target == null) {
                return false;
            } else if (!this.module.equals(((ModuleRelation) obj).getModule())) {
                return false;
            } else if (!this.target.equals(((ModuleRelation) obj).getTarget())) {
                return false;
            } else if (!getCode().equals(((ModuleRelation) obj).getCode())) {
                return false;
            }
        }
        return true;
    }


    public Module getTarget() {
        return target;
    }


    public Module getModule() {
        return module;
    }

}
