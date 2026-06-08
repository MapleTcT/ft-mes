package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.base.entities.Department;
import com.supcon.supfusion.base.entities.Position;
import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

import javax.persistence.Entity;
import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
//@Table(name = EntityTableInfo.TABLE_NAME)
public class EntityTableInfo extends AbstractAuditUniqueIdEntity implements Serializable {

    private static final long serialVersionUID = 3047829161895387082L;

    public static final String TABLE_NAME = "ec_table_info";
    /**
     * 单据编号
     */
    @Column(name = "TABLE_NO")
    private String tableNo;
    /**
     * 制单部门id
     */
    @Column(name = "CREATE_DEPARTMENT_ID")
    private Long createDepartmentId;
    /**
     * 制单部门对象
     */
    private Department createDepartment;


    /**
     * 生效人员
     */
    private Staff effectStaff;
    /**
     * 生效人员Id
     */
    @Column(name = COL_EFFECT_STAFF_ID)
    private Long effectStaffId;
    /**
     * 生效时间
     */
    @Column(name = COL_EFFECT_TIME)
    private Date effectTime;
    /**
     * 拥有岗位的Layrec
     */
    @Column(name = "POSITION_LAY_REC")
    private String positionLayRec;
    /**
     * 制单岗位
     */
    private Position createPosition;


    /**
     * 制单岗位id
     */
    @Column(name = "CREATE_POSITION_ID")
    private Long createPositionId;
    /**
     * 对应的模型
     */
    @Column(name = "TARGET_ENTITY_CODE")
    private String targetEntityCode;
    /**
     * 拥有者对象
     */
    private Staff ownerStaff;
    /**
     * 拥有者id
     */
    @Column(name = "OWNER_STAFF_ID")
    @Index(name = "idx_tableinfo_ostaffid")
    private Long ownerStaffId;
    /**
     * 拥有者岗位id
     */
    @Column(name = "OWNER_POSITION_ID")
    private Long ownerPositionId;


    private Position ownerPosition;
    /**
     * 拥有者部门id
     */
    @Column(name = "OWNER_DEPARTMENT_ID")
    private Long ownerDepartmentId;
    /**
     * 拥有者部门对象
     */
    private Department ownerDepartment;
    @Column(name = COL_STATUS)
    private Integer status;

    private String targetTableName;
    private String summary;

    private Long deploymentId;
    private String processKey; // 流程key
    private Integer processVersion;//版本
    private Integer effectiveState = 0; // 单据生效状态——普通单据生效：0，超级录入生效单据：1

    public static final String COL_STATUS = "STATUS";
    public static final String COL_TARGET_TABLE_NAME = "TARGET_TABLE_NAME";
    public static final String COL_TARGET_ENTITY_CODE = "TARGET_ENTITY_CODE";
    public static final String COL_EFFECT_STAFF_ID = "EFFECT_STAFF_ID";
    public static final String COL_EFFECT_TIME = "EFFECT_TIME";
    public static final int STATUS_INVALID = 0;
    public static final int STATUS_EFFECTED = 99;
    public static final int STATUS_RUNNING = 88;
    public static final int STATUS_SUSPEND = 77;


    @Override
    protected String _getEntityName() {
        return EntityTableInfo.class.getName();
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
        EntityTableInfo other = (EntityTableInfo) obj;
        if (getId() == null) {
            if (other.getId() != null) {
                return false;
            }
        } else if (!getId().equals(other.getId())) {
            return false;
        }
        return true;
    }

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Department.class)
    @JoinColumn(name = "CREATE_DEPARTMENT_ID", insertable = false, updatable = false)
    @Fetch(FetchMode.SELECT)
    public Department getCreateDepartment() {
        return createDepartment;
    }

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Staff.class)
    @JoinColumn(name = "EFFECT_STAFF_ID", insertable = false, updatable = false)
    @Fetch(FetchMode.SELECT)
    public Staff getEffectStaff() {
        return effectStaff;
    }

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Position.class)
    @JoinColumn(name = "CREATE_POSITION_ID", insertable = false, updatable = false)
    @Fetch(FetchMode.SELECT)
    public Position getCreatePosition() {
        return createPosition;
    }

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Staff.class)
    @JoinColumn(name = "OWNER_STAFF_ID", insertable = false, updatable = false)
    @Fetch(FetchMode.SELECT)
    public Staff getOwnerStaff() {
        return ownerStaff;
    }

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Position.class)
    @JoinColumn(name = "OWNER_POSITION_ID", insertable = false, updatable = false)
    @Fetch(FetchMode.SELECT)
    public Position getOwnerPosition() {
        return ownerPosition;
    }

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Department.class)
    @JoinColumn(name = "OWNER_DEPAETMENT_ID", insertable = false, updatable = false)
    @Fetch(FetchMode.SELECT)
    public Department getOwnerDepartment() {
        return ownerDepartment;
    }
}