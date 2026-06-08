package com.supcon.supfusion.configuration.workflow.entities;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.supcon.supfusion.base.entities.Position;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Index;


/**
 * 角色权限关联岗位表
 * 
 * @author rockey
 * 
 */

@Entity
@Data
@Table(name = RolePPosition.TABLE_NAME)
public class RolePPosition extends AbstractAuditUniqueIdEntity implements Serializable {

	private static final long serialVersionUID = -1871103898520994308L;

	public static final String TABLE_NAME = "base_rolepposition";

	private RolePermission rolePermission;
	private Position position;
	private boolean includeLower;// 是否包含下级

	@ManyToOne
	@Index(name = "INDEX_REPP_ROLEPERMISSION_ID")
	@JoinColumn(name = "ROLEPERMISSION_ID")
	public RolePermission getRolePermission() {
		return rolePermission;
	}

	public void setRolePermission(RolePermission rolePermission) {
		this.rolePermission = rolePermission;
	}

	@ManyToOne(targetEntity = Position.class)
	@Index(name = "INDEX_REPP_POSITION_ID")
	@JoinColumn(name = "POSITION_ID")
	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	@Column(columnDefinition = "INTEGER")
	public boolean isIncludeLower() {
		return includeLower;
	}

	public void setIncludeLower(boolean includeLower) {
		this.includeLower = includeLower;
	}


	@Override
	protected String _getEntityName() {
		return null;
	}
}
