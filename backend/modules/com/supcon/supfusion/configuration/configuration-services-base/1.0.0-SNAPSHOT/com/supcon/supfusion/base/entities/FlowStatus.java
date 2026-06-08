/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractIdEntity;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * 
 * 状态表  用于单据排序
 * @author zhuyuyin
 * @version 1.0
 */
@Data
@Entity
@Immutable
@Table(name = FlowStatus.TABLE_NAME)
public class FlowStatus extends AbstractIdEntity implements Serializable {

	private static final long serialVersionUID = -3396132495753123357L;
	public static final String TABLE_NAME = "base_status";
	
	private boolean valid = true;
	private String code;
	private Integer sort;
	private String name;
	
	@Override
	protected String _getEntityName() {
		return FlowStatus.class.getName();
	}

}
