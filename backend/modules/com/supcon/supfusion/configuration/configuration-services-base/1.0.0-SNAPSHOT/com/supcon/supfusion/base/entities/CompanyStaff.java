/**
 * 
 */
package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.*;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.*;
import java.io.Serializable;

/**
 * @author rockey
 * 
 */
@Data
@Entity
@Immutable
@Table(name = CompanyStaff.TABLE_NAME)
public class CompanyStaff extends AbstractAuditUniqueIdEntity implements Serializable {
	

	public static final String TABLE_NAME = "base_companystaff";

	private static final long serialVersionUID = -5118069126559619651L;

	@ManyToOne(fetch= FetchType.EAGER, targetEntity=Company.class)
	@JoinColumn(name="CID",insertable=false,updatable=false)
	@Fetch(FetchMode.SELECT)
	private Company company;
	@ManyToOne
	@JoinColumn(name="STAFF_ID")
	@Fetch(FetchMode.SELECT)
	private Staff staff;

	@Override
	protected String _getEntityName() {
		return CompanyStaff.class.getName();
	}

}
