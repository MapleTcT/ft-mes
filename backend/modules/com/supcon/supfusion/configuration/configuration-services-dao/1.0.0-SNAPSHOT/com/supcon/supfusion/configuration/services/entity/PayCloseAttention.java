package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.*;

@Data
@Entity
@Table(name = PayCloseAttention.TABLE_NAME)
public class PayCloseAttention extends AbstractAuditUniqueIdEntity {

	private static final long serialVersionUID = 8529835767406417967L;
	public static final String TABLE_NAME = "wf_pay_close_attention";

	@Override
	protected String _getEntityName() {
		return PayCloseAttention.class.getName();
	}

	@ManyToOne(fetch = FetchType.EAGER, targetEntity = Staff.class)
	@JoinColumn(name = "staff", nullable = false)
	@Fetch(FetchMode.SELECT)
	public Staff staff;
}