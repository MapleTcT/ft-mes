package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;

import java.io.Serializable;

@Data
@javax.persistence.Entity
public class SqlModelColumn extends AbstractAuditUniqueCodeEntity implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String columnName; //列名
	private int columnType; //类型
	private String columnTypeName; //类型名称
	private int columnDisplaySize; //最大字符个数


	@Override
	protected String _getEntityName() {
		return getClass().getName();
	}

}
