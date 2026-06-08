package com.supcon.supfusion.configuration.workflow.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.supcon.supfusion.configuration.services.entity.SpecialPermission;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;



/**
 * 特殊权限角色关联表
 * @author zhangbobin
 * @date   2015年10月13日
 */
@Entity
@Table(name = RolePSpecialPermission.TABLE_NAME)
public class RolePSpecialPermission extends AbstractAuditUniqueIdEntity implements Serializable {

	private static final long serialVersionUID = 6888077898689262174L;

	public static final String TABLE_NAME = "base_role_specialpermission";

	private RolePermission rolePermission;
	private SpecialPermission specialPermission;
	private String content;
	private String configString;

	@ManyToOne
	@JoinColumn(name="ROLEPERMISSION_ID")
	public RolePermission getRolePermission() {
		return rolePermission;
	}

	public void setRolePermission(RolePermission rolePermission) {
		this.rolePermission = rolePermission;
	}
	
	
	
	@ManyToOne(targetEntity=SpecialPermission.class)
	@JoinColumn(name="SPECIAL_PERMISSION_CODE")
    @Fetch(FetchMode.SELECT)
	public SpecialPermission getSpecialPermission() {
		return specialPermission;
	}

	public void setSpecialPermission(SpecialPermission specialPermission) {
		this.specialPermission = specialPermission;
	}
	


	@Override
	protected String _getEntityName() {
		return RolePSpecialPermission.class.getName();
	}
	
	@Column(name="CONTENT", length = 2000)
	public String getContent() {
		return content;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	
	@Lob
	@Column(name="CONFIG_STRING")
	public String getConfigString() {
		return configString;
	}

	public void setConfigString(String configString) {
		this.configString = configString;
	}
	
	


}
