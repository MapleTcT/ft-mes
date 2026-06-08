/**
 * 
 */
package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractIdEntity;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.io.Serializable;


@Data
@Entity
@Immutable
@Table(name = "base_datapermissionstaff")
public class DataPermissionStaff extends AbstractIdEntity implements Serializable {

	private static final long serialVersionUID = 3231984137432906763L;
	@Column(name="DATAPERMISSION_ID")
	private Long dataPermissionId;
	private Long staffId;

	@Override
	protected String _getEntityName() {
		
		return DataPermissionStaff.class.getName();
	}
}
