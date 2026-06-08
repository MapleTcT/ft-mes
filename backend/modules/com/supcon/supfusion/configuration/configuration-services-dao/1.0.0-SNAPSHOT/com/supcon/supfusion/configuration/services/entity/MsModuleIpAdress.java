package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
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
 * @author yaowei
 * @version $Id$
 */
@Data
@javax.persistence.Entity
//@Table(name = MsModuleIpAdress.TABLE_NAME)
public class MsModuleIpAdress extends AbstractAuditUniqueCodeEntity implements Serializable {
    private static final long serialVersionUID = 7635333342655214403L;
    public static final String TABLE_NAME = "ec_msmodule_ipadress";
    private String ipadress;

    @ManyToOne
    @JoinColumn(name = "MSMODULE_CODE", referencedColumnName = "CODE")
    @Fetch(FetchMode.SELECT)
    private MsModule msModule;
    private String description;
    private Date publishTime;
    private Integer status; //状态


    @Override
    protected String _getEntityName() {
        // TODO Auto-generated method stub
        return getClass().getName();
    }


    public MsModule getMsModule() {
        return msModule;
    }
}