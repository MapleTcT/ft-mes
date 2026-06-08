/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 *
 *
 * @author fangzhibin
 * @version $Id$
 */
@Data
@javax.persistence.Entity
//@Table(name = EntityModifyInfo.TABLE_NAME)
public class EntityModifyInfo extends AbstractAuditUniqueIdEntity implements Serializable {

    private static final long serialVersionUID = 3047829161895387082L;

    public static final String TABLE_NAME = "ec_entity_modify_info";

    private String entityCode;
    private String moduleCode;

    private Date lastModifyTime;


    @Override
    protected String _getEntityName() {
        return getClass().getName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        EntityModifyInfo other = (EntityModifyInfo) obj;
        if (getId() == null) {
            if (other.getId() != null) {
                return false;
            }
        } else if (!getId().equals(other.getId())) {
            return false;
        }
        return true;
    }
}
