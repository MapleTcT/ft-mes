package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@javax.persistence.Entity
@Table(name = Supervision.TABLE_NAME)
public class Supervision extends AbstractAuditUniqueCodeEntity implements Serializable {

	private static final long serialVersionUID = 8529835767406417967L;
	public static final String TABLE_NAME = "wf_supervision";

	private Long tableInfoId;
	private Staff staff;


	@Override
	protected String _getEntityName() {
		return Supervision.class.getName();
	}

	@ManyToOne(fetch = FetchType.EAGER, targetEntity = Staff.class)
	@JoinColumn(name = "staff", nullable = false)
	@Fetch(FetchMode.SELECT)
	public Staff getStaff() {
		return staff;
	}

	@Index(name = "IDX_SUPERVISION_DITABLEID")
	public Long getTableInfoId() {
		return tableInfoId;
	}
}