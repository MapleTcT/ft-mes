package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.base.enums.DataPermissionType;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 流程督办信息。
 * 
 * @author qy
 * 
 */
@Data
@Entity
@Table(name = TransitionStaff.TABLE_NAME)
public class TransitionStaff extends AbstractAuditUniqueIdEntity implements Serializable {
	private static final long serialVersionUID = -5425438079328401144L;
	public static final String TABLE_NAME = "wf_transition_staff";
	private Long deploymentId;//流程id
	private String outcome;//迁移线code
	@Enumerated(EnumType.STRING)
	private DataPermissionType type;//类型 人员、岗位、部门、角色
	private Long typeId;//类型id，人员、岗位、部门、角色
	private String groupName;
	private Double sort;
	
	@Override
	protected String _getEntityName() {
		return TransitionStaff.class.getName();
	}

	
}
