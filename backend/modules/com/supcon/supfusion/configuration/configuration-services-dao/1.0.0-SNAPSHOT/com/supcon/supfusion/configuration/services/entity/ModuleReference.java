package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;

/**
 * 引用模块
 *
 * @author fukun
 */
@Data
@javax.persistence.Entity
//@Table(name = ModuleReference.TABLE_NAME)
public class ModuleReference extends AbstractAuditUniqueCodeEntity implements Serializable {

    private static final long serialVersionUID = 2128807626659265235L;

    public static final String TABLE_NAME = "ec_module_reference";

    @ManyToOne
    @JoinColumn(name = "TARGET_MODULE_CODE", referencedColumnName = "CODE")
    @Fetch(FetchMode.SELECT)
    private Module target;

    @ManyToOne
    @JoinColumn(name = "MODULE_CODE", referencedColumnName = "CODE")
    @Fetch(FetchMode.SELECT)
    private Module module;


    public Module getTarget() {
        return target;
    }



    public Module getModule() {
        return module;
    }


    @Override
    protected String _getEntityName() {
        return "ModuleReference";
    }

}
