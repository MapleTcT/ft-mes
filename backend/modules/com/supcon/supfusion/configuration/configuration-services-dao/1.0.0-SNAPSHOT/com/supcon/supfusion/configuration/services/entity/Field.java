/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supcon.supfusion.base.entities.SystemCode;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.enums.FieldType;
import com.supcon.supfusion.configuration.services.enums.RegionType;
import com.supcon.supfusion.configuration.services.enums.ShowFormat;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

/**
 *
 *
 * @author zhuyuyin
 * @version $Id$
 */
@Data
@javax.persistence.Entity
//@Table(name = Field.TABLE_NAME)
public class Field extends AbstractAuditUniqueCodeEntity implements Serializable {
	private static final long serialVersionUID = -8043218844705909935L;
	public static final String TABLE_NAME = "ec_field";
	@Transient
	protected EcEnv ecEnv = EcEnv.product;
	private String moduleCode;
	private String entityCode;
	@Column(name = "FIELD_KEY")
	private String key; // 编码
	private String name;// 显示名称
//	@BAPInternational(fieldName = "displayNameInternational", replace = false)
	private String displayName;
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PROPERTY_CODE", referencedColumnName = "code")
	@Index(name = "idx_ECFIELD_PROPERTY")
	@Fetch(FetchMode.SELECT)
	private Property property;// 关联字段
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "VIEW_CODE", referencedColumnName = "code")
	@Index(name = "idx_ECFIELD_VIEW")
	@Fetch(FetchMode.SELECT)
	private View view;// 关联视图
	@Enumerated(EnumType.STRING)
	private FieldType showType;// 显示类型
	@Enumerated(EnumType.STRING)
	private ShowFormat showFormat;// 显示格式
	private String layRec;// 层级关系
	private Boolean isHidden = false;// 是否隐藏
	private String none;
	private String cellCode;// 关联单元格code
	@JsonIgnore
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "field", cascade = { CascadeType.ALL })
	@Fetch(FetchMode.SELECT)
	@OrderBy(clause = "code asc")
	@Where(clause = "valid = 1")
	private Set<Event> events = new HashSet<>();
	@JsonIgnore
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "field", cascade = { CascadeType.ALL })
	@Fetch(FetchMode.SELECT)
	@Where(clause = "valid = 1")
	@OrderBy(clause = "code asc")
	//@Transient
	private Set<Validate> validates = new HashSet<Validate>();
	private String config;
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "DATAGRID_CODE", referencedColumnName = "code")
	@Index(name = "idx_ECFIELD_DATAGRID")
	@Fetch(FetchMode.SELECT)
	private DataGrid dataGrid;
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "FASTQUERYJSON_CODE", referencedColumnName = "code")
	@Index(name = "idx_ECFIELD_FASTQUERYJSON")
	@Fetch(FetchMode.SELECT)
	private FastQueryJson fastQueryJson;
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ADVQUERYJSON_CODE", referencedColumnName = "code")
	@Index(name = "idx_ECFIELD_ADVQUERYJSON")
	@Fetch(FetchMode.SELECT)
	private AdvQueryJson advQueryJson;
	@Enumerated(EnumType.STRING)
	private RegionType regionType;
	private String fullPropertyCode;
	@SuppressWarnings("rawtypes")
	@Transient
	private Map configMap;
	@Enumerated(EnumType.STRING)
	private DbColumnType columnType;
	private Boolean projFlag;

	@ManyToOne(fetch=FetchType.LAZY, optional=true, targetEntity = SystemCode.class)
	@JoinColumn(name="LAYER_TYPE", nullable=true)
	@Fetch(FetchMode.SELECT)
	private SystemCode layerType; // 图层类型

	public void addValidate(Validate validate) {
		validates.add(validate);
	}

	@Override
	protected String _getEntityName() {
		return getClass().getName();
	}

	@Lob
	public String getConfig() {
		if (config != null && !config.isEmpty()) {
			if (config.indexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>") == -1) {
				config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + config;
			}
		}
		return config;
	}

	@Override
	public String toString() {
		return _getEntityName() + " [code=" + getCode() + "]";
	}

}
