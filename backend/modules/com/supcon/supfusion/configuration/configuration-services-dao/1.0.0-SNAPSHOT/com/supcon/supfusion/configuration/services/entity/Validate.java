/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

import javax.persistence.CascadeType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.io.Serializable;

/**
 *
 *
 * @author zhuyuyin
 * @version 1.0
 */
@javax.persistence.Entity
//@Table(name = Validate.TABLE_NAME)
public class Validate extends AbstractAuditUniqueCodeEntity implements Serializable {
    private static final long serialVersionUID = -1528441220539565873L;
    public static final String TABLE_NAME = "ec_validate";
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
    private String type;//验证类型
    private String params;//内容 xml
    @JsonIgnore
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "FIELD_CODE", referencedColumnName = "code")
    @Index(name = "idx_ECVALIDATE_FIELD")
    @Fetch(FetchMode.SELECT)
    private Field field;//关联字段
    private Boolean projFlag;
    private String moduleCode;
    private String entityCode;

    @Override
    protected String _getEntityName() {
        // TODO Auto-generated method stub
        return getClass().getName();
    }


    public Field getField() {
        return field;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Boolean getProjFlag() {
        return projFlag;
    }

    public void setProjFlag(Boolean projFlag) {
        this.projFlag = projFlag;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getEntityCode() {
        return entityCode;
    }

    public void setEntityCode(String entityCode) {
        this.entityCode = entityCode;
    }

    public EcEnv getEcEnv() {
        return ecEnv;
    }

    public void setEcEnv(EcEnv ecEnv) {
        this.ecEnv = ecEnv;
    }
}
