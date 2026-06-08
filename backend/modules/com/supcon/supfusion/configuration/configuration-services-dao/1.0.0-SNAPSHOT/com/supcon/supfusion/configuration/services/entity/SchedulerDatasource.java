package com.supcon.supfusion.configuration.services.entity;


import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;

import javax.persistence.Table;
import java.io.Serializable;

/**
 * 
 * @author sk
 * 
 */
@Data
@javax.persistence.Entity
@Table(name = SchedulerDatasource.TABLE_NAME)
public class SchedulerDatasource extends AbstractAuditUniqueCodeEntity implements Serializable {

	private static final long serialVersionUID = 7119612377276341004L;
	
	public static final String TABLE_NAME = "scheduler_datasource";
	
	private String moduleCode ; // 模块code
	
	private String datasourceAddress ; // 数据库地址


	private String datasourceName ; // 数据库名称
	
	private String datasourceType; // 数据库类型

//	@BAPInternational(fieldName = "nameInternational", replace = false)
	private String name ; // 数据源名称

	private String password ; // 数据库密码


	private String port ; // 数据库端口
	
	private String username; //数据库用户名



	@Override
	protected String _getEntityName() {
		return getClass().getName();
	}
}
