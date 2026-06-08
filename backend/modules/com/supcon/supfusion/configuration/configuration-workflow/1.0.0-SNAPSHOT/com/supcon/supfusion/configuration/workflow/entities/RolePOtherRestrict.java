package com.supcon.supfusion.configuration.workflow.entities;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.supcon.supfusion.configuration.services.entity.OtherRestrict;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;



/**
 * 其他限制角色关联表
 * @author zhangbobin
 * @date   2015年10月13日
 */
@Entity
@Data
@Table(name = RolePOtherRestrict.TABLE_NAME)
public class RolePOtherRestrict extends AbstractAuditUniqueIdEntity implements Serializable {
	private static final long serialVersionUID = -1318814242630967486L;

	public static final String TABLE_NAME = "base_role_otherrestrict_ref";

	private RolePermission rolePermission;
	private OtherRestrict otherRestrict;
	
	
	@ManyToOne
	@JoinColumn(name="ROLEPERMISSION_ID")
	public RolePermission getRolePermission() {
		return rolePermission;
	}

	public void setRolePermission(RolePermission rolePermission) {
		this.rolePermission = rolePermission;
	}

	
	@ManyToOne(targetEntity=OtherRestrict.class)
	@JoinColumn(name="OTHER_RESTRICT_CODE")
    @Fetch(FetchMode.SELECT)
	public OtherRestrict getOtherRestrict() {
		return otherRestrict;
	}

	public void setOtherRestrict(OtherRestrict otherRestrict) {
		this.otherRestrict = otherRestrict;
	}


	@Override
	protected String _getEntityName() {
		return RolePOtherRestrict.class.getName();
	}
	
	
	


}
