package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Map;

@Data
@javax.persistence.Entity
@Table(name = Layout.TABLE_NAME)
public class Layout extends AbstractAuditUniqueCodeEntity implements Serializable {

	private static final long serialVersionUID = -6088191920187170373L;
	public static final String TABLE_NAME = "ec_layout";
	@Transient
	protected EcEnv ecEnv = EcEnv.product;
	private String name;
	private String image;
	private String description;
	@Column(length = 4000)
	private String content;
	@SuppressWarnings("rawtypes")
	@Transient
	private Map configMap;

	
	@SuppressWarnings("rawtypes")
	public Map getConfigMap() {
		return configMap;
	}

	@SuppressWarnings("rawtypes")
	public void setConfigMap(Map configMap) {
		this.configMap = configMap;
	}

	@Override
	protected String _getEntityName() {
		return Layout.class.getName();
	}

}