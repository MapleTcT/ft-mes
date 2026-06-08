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
@Table(name = "base_datapmsposition")
public class DataPmsPosition extends AbstractIdEntity implements Serializable {

	private static final long serialVersionUID = 3231984137432906763L;
	@Column(name="DATAPERMISSION_ID")
	private Long dataPermissionId;
	private Boolean includeLower;
	private Long positionId;
	
	@Override
	protected String _getEntityName() {
		return DataPmsPosition.class.getName();
	}


}
