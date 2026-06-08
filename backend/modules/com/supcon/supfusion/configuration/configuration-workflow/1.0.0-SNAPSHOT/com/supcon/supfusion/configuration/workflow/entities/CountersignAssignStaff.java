package com.supcon.supfusion.configuration.workflow.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractIdEntity;
import lombok.Data;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 流程督办信息。
 * 
 * @author qy
 * 
 */
@Data
@Entity
@Table(name = CountersignAssignStaff.TABLE_NAME)
public class CountersignAssignStaff extends AbstractIdEntity {
	private static final long serialVersionUID = -5425438079328401144L;
	public static final String TABLE_NAME = "wf_countersign_assign_staff";
	private Long deploymentId;//流程id
	private String outcome;//迁移线code
	private String assignStaff;
	private Long tableInfoId;

	@Override
	protected String _getEntityName() {
		return CountersignAssignStaff.class.getName();
	}

	
}
