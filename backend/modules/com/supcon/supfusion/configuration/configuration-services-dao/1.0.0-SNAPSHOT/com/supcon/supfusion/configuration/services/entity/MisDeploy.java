package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Date;

/**
 * 实体配置：模块
 *
 *
 * @author cwn
 * @version $Id$
 */
@Data
@javax.persistence.Entity
//@Table(name = MisDeploy.TABLE_NAME)
public class MisDeploy extends AbstractAuditUniqueIdEntity implements Serializable {
    private static final long serialVersionUID = 7635333342655285764L;
    public static final String TABLE_NAME = "ec_mis_deploy";
    private MsModule msModule;
    private Date createTime;
    private String msName;
    private Module module;
    private String moduleName;

    @ManyToOne
    @JoinColumn(name = "MS_CODE", referencedColumnName = "CODE")
    @Fetch(FetchMode.SELECT)
    public MsModule getMsModule() {
        return msModule;
    }

    @ManyToOne
    @JoinColumn(name = "MODULE_CODE", referencedColumnName = "ARTIFACT")
    @Fetch(FetchMode.SELECT)
    public Module getModule() {
        return module;
    }


    @Override
    protected String _getEntityName() {
        // TODO Auto-generated method stub
        return getClass().getName();
    }




}