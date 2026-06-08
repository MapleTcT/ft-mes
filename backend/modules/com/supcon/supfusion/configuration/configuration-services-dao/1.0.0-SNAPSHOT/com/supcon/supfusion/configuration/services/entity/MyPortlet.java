package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@javax.persistence.Entity
@Table(name = MyPortlet.TABLE_NAME)
public class MyPortlet extends AbstractAuditUniqueIdEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String TABLE_NAME = "ec_my_portlet";

    @Column(name = "USER_ID", unique = true)
    private Long userId;
    @Lob
    private String config;

    @Transient
    private Map configMap = new HashMap();

    @Override
    protected String _getEntityName() {
        return MyPortlet.class.getName();
    }


}
