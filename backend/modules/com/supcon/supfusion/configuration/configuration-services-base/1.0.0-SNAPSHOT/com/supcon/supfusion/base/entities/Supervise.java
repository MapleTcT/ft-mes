package com.supcon.supfusion.base.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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
@Table(name = Supervise.TABLE_NAME)
public class Supervise extends AbstractIdEntity implements Serializable {
	private static final long serialVersionUID = 4840968584806602154L;
	public static final String TABLE_NAME = "wf_supervise";
	@Column(name = "DEPLOYMENT_ID")
	private Long deploymentId;

	@ManyToOne(fetch= FetchType.EAGER,targetEntity=Staff.class)
	@JoinColumn(name="STAFF")
	private Staff staff;


	@Override
	protected String _getEntityName() {
		return Supervise.class.getName();
	}

	
}
