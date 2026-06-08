package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.io.Serializable;

@Data
@javax.persistence.Entity
@Table(name = DealInfo.TABLE_NAME)
public class DealInfo extends AbstractAuditUniqueCodeEntity implements Serializable {

    private static final long serialVersionUID = 8529835767406417967L;
    public static final String TABLE_NAME = "wf_deal_info";

    private Long mainObj;
    private Integer sort;
    private String activityName;
    private DealInfoType dealInfoType;
    private String entityCode;
    private String processKey;
    private Integer processVersion;
    private Long userId;
    private String taskDescription;
    private String outcomeDes;
    private String instanceId;
    private String assignStaff;
    private String assignStaffId;

    @Column(name = "MAIN_OBJ")
    public Long getMainObj() {
        return mainObj;
    }

    public void setMainObj(Long mainObj) {
        this.mainObj = mainObj;
    }


    protected Staff staff;

    protected Long tableInfoId;// 表单id


    @Override
    protected String _getEntityName() {
        return DealInfo.class.getName();
    }

    @JoinColumn(name = "STAFF")
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = Staff.class)
    @Fetch(FetchMode.SELECT)
    public Staff getStaff() {
        return staff;
    }

}