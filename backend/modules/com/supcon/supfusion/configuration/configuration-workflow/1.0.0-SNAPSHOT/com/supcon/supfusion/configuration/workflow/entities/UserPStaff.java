package com.supcon.supfusion.configuration.workflow.entities;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.base.entities.UserPermission;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;


@Entity
@Data
@Table(name = UserPStaff.TABLE_NAME)
public class UserPStaff extends AbstractAuditUniqueIdEntity implements Serializable {

	private static final long serialVersionUID = -360286670181146703L;
	public static final String TABLE_NAME = "base_userpstaff";

	private UserPermission userPermission;
	private Staff staff;

	@ManyToOne
	@JoinColumn(name="USERPERMISSION_ID")
	public UserPermission getUserPermission() {
		return userPermission;
	}

	public void setUserPermission(UserPermission userPermission) {
		this.userPermission = userPermission;
	}

	@ManyToOne(targetEntity= Staff.class)
	@JoinColumn(name="STAFF_ID")
    @Fetch(FetchMode.SELECT)
	public Staff getStaff() {
		return staff;
	}

	public void setStaff(Staff staff) {
		this.staff = staff;
	}

	@Override
	protected String _getEntityName() {
		return UserPStaff.class.getName();
	}

}
