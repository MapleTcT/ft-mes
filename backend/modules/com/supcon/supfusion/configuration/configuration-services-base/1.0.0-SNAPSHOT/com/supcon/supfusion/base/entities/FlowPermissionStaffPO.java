/**
 *
 */
package com.supcon.supfusion.base.entities;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

/**
 *
 *
 * @author rockey
 *
 */
@Entity
@Table(name = "rbac_flow_permission_staff")
public class FlowPermissionStaffPO implements Serializable {

    private static final long serialVersionUID = 3231984137432906763L;
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

    @Column(name = "FLOWPERMISSION_ID")
    private Long datapermissionId;


    //	private FlowPermissionPO dataPermission;
    private Long staffId;

    /*@ManyToOne(fetch = FetchType.LAZY)
    @Index(name = "index_dpstaff_datap_id")
    @JoinColumn(name = "flowpermission_id")
    public FlowPermissionPO getDataPermission() {
        return dataPermission;
    }

    public void setDataPermission(FlowPermissionPO dataPermission) {
        this.dataPermission = dataPermission;
    }*/
    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

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

    public Long getDatapermissionId() {
        return datapermissionId;
    }

    public void setDatapermissionId(Long datapermissionId) {
        this.datapermissionId = datapermissionId;
    }
}
