package com.supcon.supfusion.configuration.services.entity;

/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/2/24
 */

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.IValid;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.Date;

/**
 * EntityConfig模块中的实体实现此基类，实现了 IAuditEntity 和 IValid 两个接口的基础模型类。
 * <p>
 * 所有 IStaff 都是 Transient 的。
 * </p>
 *
 * @author fangzhibin
 *
 */
@MappedSuperclass
public abstract class AbstractEcAuditEntity extends EcCodeEntity implements  IValid {

    private static final long serialVersionUID = 1L;
    private Long createStaffId;
    private Long modifyStaffId;
    private Long deleteStaffId;
    @JsonIgnore
    private Staff createStaff;
    @JsonIgnore
    private Staff modifyStaff;
    @JsonIgnore
    private Staff deleteStaff;
    private Date createTime;
    private Date modifyTime;
    private Date deleteTime;
    private boolean valid = true;

    public void setCreateStaffId(Long createStaffId) {
        this.createStaffId = createStaffId;
    }


    @Column(name = "MODIFY_STAFF_ID")
    public Long getModifyStaffId() {
        return modifyStaffId;
    }
    public void setModifyStaffId(Long modifyStaffId) {
        this.modifyStaffId = modifyStaffId;
    }
    @Column(name = "DELETE_STAFF_ID")
    public Long getDeleteStaffId() {
        return deleteStaffId;
    }
    public void setDeleteStaffId(Long deleteStaffId) {
        this.deleteStaffId = deleteStaffId;
    }
    @Column(name = "CREATE_TIME")
    public Date getCreateTime() {
        return createTime;
    }
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    @Column(name = "MODIFY_TIME")
    public Date getModifyTime() {
        return modifyTime;
    }
    public void setModifyTime(Date modifyTime) {
        this.modifyTime = modifyTime;
    }
    @Column(name = "DELETE_TIME")
    public Date getDeleteTime() {
        return deleteTime;
    }
    public void setDeleteTime(Date deleteTime) {
        this.deleteTime = deleteTime;
    }


    @Override
    public void setValid(boolean isValid) {
        this.valid = isValid;
    }

}
