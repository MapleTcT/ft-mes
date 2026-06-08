package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractIdEntity;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;

@Data
@Entity
@Table(name = Script.TABLE_NAME)
public class Script extends AbstractIdEntity implements Serializable {
	private static final long serialVersionUID = 3365369168404339135L;

	public static final String TABLE_NAME = "sc_script";

	// private Long entityId;
	private String entityCode;
	private String name;
	private String description;
	private String scriptCode;// CODE,便于迁移
	private String code;// transient,save into file
	@Transient
	private String log;// transient，SVN commit log.

	@Override
	protected String _getEntityName() {
		return getClass().getName();
	}
}