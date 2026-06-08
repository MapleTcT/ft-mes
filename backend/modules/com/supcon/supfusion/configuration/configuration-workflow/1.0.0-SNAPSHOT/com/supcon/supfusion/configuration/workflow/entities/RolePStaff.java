package com.supcon.supfusion.configuration.workflow.entities;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;

/**
 * 角色权限关联人员表
 * 
 * @author tanzhengyang
 * 
 */
@Entity
@Data
@Table(name = RolePStaff.TABLE_NAME)
public class RolePStaff extends AbstractAuditUniqueIdEntity implements Serializable {

	private static final long serialVersionUID = -1871103898520994308L;

	public static final String TABLE_NAME = "base_rolepstaff";
	private RolePermission rolePermission;
	private Staff staff;

	@ManyToOne
	@JoinColumn(name = "ROLEPERMISSION_ID")
	public RolePermission getRolePermission() {
		return rolePermission;
	}

	public void setRolePermission(RolePermission rolePermission) {
		this.rolePermission = rolePermission;
	}

	@ManyToOne(targetEntity = Staff.class)
	@JoinColumn(name = "STAFF_ID")
	public Staff getStaff() {
		return staff;
	}

	public void setStaff(Staff staff) {
		this.staff = staff;
	}




	@Override
	protected String _getEntityName() {
		return RolePStaff.class.getName();
	}

}
