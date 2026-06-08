package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;

import javax.persistence.Table;
import java.io.Serializable;

@Data
@javax.persistence.Entity
@Table(name = SelectionRange.TABLE_NAME)
public class SelectionRange extends AbstractAuditUniqueIdEntity implements Serializable {

	private static final long serialVersionUID = 2026936237315597643L;
	
	public static final String TABLE_NAME = "selection_ranges";
	
	private String fieldCode;
	
	private String rangeIds;
	
	private String groupName;
	
	private Long rangeId;
	
	private String type;
	
	//private Long sort;
	
	private Double sort;
	
	private String rangeName;


	@Override
	protected String _getEntityName() {
		return SelectionRange.class.getName();
	}

}
