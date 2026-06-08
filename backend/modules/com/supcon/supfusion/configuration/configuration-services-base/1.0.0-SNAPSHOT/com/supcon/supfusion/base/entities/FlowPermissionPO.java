package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.base.enums.DataPermissionType;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;


@Entity
@Table(name = FlowPermissionPO.TABLE_NAME)
public class FlowPermissionPO implements Serializable /*extends AbstractAuditEntity implements Serializable, IAuditEntity, IDataPermission*/ {
    private static final long serialVersionUID = 3231984137432950763L;
    public static final String TABLE_NAME = "rbac_flow_permission";
    @Id
    @GenericGenerator(
            name = "SnowFlakeIDGenerator",
            strategy = "com.supcon.supfusion.framework.scaffold.hibernate.id.SnowFlakeIDGenerator"
    )
    @GeneratedValue(
            generator = "SnowFlakeIDGenerator"
    )
    private Long id;

    private Integer version;


    private String entityCode;

    private Integer purviewDistribution;

    private Integer purviewState;
    private String memo;
    private Boolean unlimitedPower;

    private Boolean groupPowerFlag;

    private Boolean assignStaffFlag;

    private Boolean assignPosFlag;

    private Boolean positionPowerFlag;

    @Enumerated(EnumType.STRING)
    @Column(name = "FLOW_PERMISSION_TYPE")
    private DataPermissionType dataPermissionType;
    private Long typeId;

    private String activityCode;

    private String flowVersion;
    private String flowKey;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getEntityCode() {
        return entityCode;
    }

    public void setEntityCode(String entityCode) {
        this.entityCode = entityCode;
    }

    public Integer getPurviewDistribution() {
        return purviewDistribution;
    }

    public void setPurviewDistribution(Integer purviewDistribution) {
        this.purviewDistribution = purviewDistribution;
    }

    public Integer getPurviewState() {
        return purviewState;
    }

    public void setPurviewState(Integer purviewState) {
        this.purviewState = purviewState;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Boolean getUnlimitedPower() {
        return unlimitedPower;
    }

    public void setUnlimitedPower(Boolean unlimitedPower) {
        this.unlimitedPower = unlimitedPower;
    }

    public Boolean getGroupPowerFlag() {
        return groupPowerFlag;
    }

    public void setGroupPowerFlag(Boolean groupPowerFlag) {
        this.groupPowerFlag = groupPowerFlag;
    }

    public Boolean getAssignStaffFlag() {
        return assignStaffFlag;
    }

    public void setAssignStaffFlag(Boolean assignStaffFlag) {
        this.assignStaffFlag = assignStaffFlag;
    }

    public Boolean getAssignPosFlag() {
        return assignPosFlag;
    }

    public void setAssignPosFlag(Boolean assignPosFlag) {
        this.assignPosFlag = assignPosFlag;
    }

    public boolean getPositionPowerFlag() {
        return positionPowerFlag;
    }

    public void setPositionPowerFlag(Boolean positionPowerFlag) {
        this.positionPowerFlag = positionPowerFlag;
    }

    public DataPermissionType getDataPermissionType() {
        return dataPermissionType;
    }

    public void setDataPermissionType(DataPermissionType dataPermissionType) {
        this.dataPermissionType = dataPermissionType;
    }

    public Long getTypeId() {
        return typeId;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }

    public String getActivityCode() {
        return activityCode;
    }

    public void setActivityCode(String activityCode) {
        this.activityCode = activityCode;
    }

    public String getFlowVersion() {
        return flowVersion;
    }

    public void setFlowVersion(String flowVersion) {
        this.flowVersion = flowVersion;
    }

    public String getFlowKey() {
        return flowKey;
    }

    public void setFlowKey(String flowKey) {
        this.flowKey = flowKey;
    }
}
