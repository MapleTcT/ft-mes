/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author zhuyuyin
 * @version $Id$
 */
@Getter
@Setter
@javax.persistence.Entity
//@Table(name = Event.TABLE_NAME)
public class Event extends AbstractAuditUniqueCodeEntity implements Serializable {
    private static final long serialVersionUID = -7099791205704038205L;
    public static final String TABLE_NAME = "ec_event";
    private String name;
    @Lob
    @Column(name = "EVENT_FUNCTION")
    private String function;
    @JsonIgnore
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "FIELD_CODE", referencedColumnName = "code")
    @Index(name = "idx_ECEVENT_FIELD")
    @Fetch(FetchMode.SELECT)
    private Field field;
    @JsonIgnore
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "BUTTON_CODE", referencedColumnName = "code")
    @Index(name = "idx_ECEVENT_BUTTON")
    @Fetch(FetchMode.SELECT)
    private Button button;
    private String layoutCode;
    private String tabCode;
    private String sectionCode;
    private Boolean projFlag;
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
    @Lob
    @Column(name = "EVENT_FUNCTION_ES5")
    private String function_es5;

    private String moduleCode;
    private String entityCode;

    @Override
    protected String _getEntityName() {
        return getClass().getName();
    }


    public Field getField() {
        return field;
    }


    public Button getButton() {
        return button;
    }
}
