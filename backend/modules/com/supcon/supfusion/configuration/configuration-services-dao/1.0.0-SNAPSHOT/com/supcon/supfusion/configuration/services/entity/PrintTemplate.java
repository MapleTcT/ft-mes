/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.hibernate.annotations.*;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;

/**
 * @author fangzhibin
 * @version $Id$
 */
@Data
@javax.persistence.Entity
@javax.persistence.Table(name = PrintTemplate.TABLE_NAME)
public class PrintTemplate extends AbstractAuditUniqueCodeEntity implements Serializable {
    private static final long serialVersionUID = -7436616994105114860L;

    private String template; // 模版内容

    private String description; // 描述
    public static final String TABLE_NAME = "ec_print_template";
    @Column(name = "VIEW_CODE")
    @Index(name = "idx_print_view_code", columnNames = {"viewCode"})
    private String viewCode;

    private Integer processVersion;

    private String processKey;
    //  BAP-XA-DBZY zhanghd start
//    @BAPInternational(fieldName = "templateNameInternational", replace = false)
    private String templateName;

    private String templateCode;

    private String modelCode;

    private Integer isPublish;

    private String templateRemark;

    private String templateScript;
    private Boolean projFlag;

    @ManyToOne
    @JoinColumn(name = "ENTITY_CODE", referencedColumnName = "CODE")
    @Fetch(FetchMode.SELECT)
    @Index(name = "ind_print_entity_code", columnNames = "entity")
    private Entity entity;

    private Boolean templateEnabled;//是否启用

    private Boolean extraParam;

    private Integer extraParamCount;

    private Integer extraPicParamCount;

    private String extraParamScript;


    public Entity getEntity() {
        return entity;
    }

    @Column(name = "VIEW_CODE")
    @Index(name = "idx_print_view_code", columnNames = {"viewCode"})
    public String getViewCode() {
        return viewCode;
    }


    /*
     * (non-Javadoc)
     *
     * @see com.supcon.orchid.orm.entities.EcCodeEntity#_getEntityName()
     */
    @Override
    protected String _getEntityName() {
        // TODO Auto-generated method stub
        return PrintTemplate.class.getName();
    }

}
