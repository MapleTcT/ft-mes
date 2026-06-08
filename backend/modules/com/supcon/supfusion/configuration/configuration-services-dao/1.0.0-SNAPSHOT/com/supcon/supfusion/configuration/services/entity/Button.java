/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supcon.supfusion.configuration.services.enums.OperateType;
import com.supcon.supfusion.configuration.services.enums.RegionType;
import com.supcon.supfusion.configuration.services.utils.SerializeUitls;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 
 * 
 * @author zhuyuyin
 * @version $Id$
 */

@Getter
@Setter
@javax.persistence.Entity
//@Table(name = Button.TABLE_NAME)
public class Button extends AbstractAuditUniqueCodeEntity implements Serializable {
	private static final long serialVersionUID = -415440617645100336L;
	public static final String TABLE_NAME = "ec_button";
	@Transient
	protected EcEnv ecEnv = EcEnv.product;
	private String name;
	@Enumerated(EnumType.STRING)
	private OperateType operateType;
	@JsonIgnore
	@OneToOne
	@JoinColumn(name = "VIEWSELECT_CODE")
	@Fetch(FetchMode.SELECT)
	@Index(name = "idx_ECBUTTON_VIEWSELECT")
	@NotFound(action=NotFoundAction.IGNORE)
	private View viewSelect;
	private Boolean isConfirm = false;
	private String confirmContent;
	private String buttonStyle;
	private String buttonOperationCode;
	private Boolean isUseMore = false;
	private Boolean isPermission = false;
	private Boolean isCallback = false;
	private Boolean isCustomFunc = false;
	private Boolean isHide = false;
	private String operateUrl;
//	@BAPInternational(fieldName = "displayNameInternational", replace = false)
	private String displayName;
	private String cellCode;
	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "VIEW_CODE", referencedColumnName = "code")
	@Index(name = "idx_ECBUTTON_VIEW")
	@Fetch(FetchMode.SELECT)
	@NotFound(action= NotFoundAction.IGNORE)
	private View view;
	@JsonIgnore
	@OneToMany(mappedBy = "button", fetch=FetchType.EAGER, cascade = { CascadeType.ALL })
	@Fetch(FetchMode.SELECT)
	@org.hibernate.annotations.OrderBy(clause = "code asc")
	private Set<Event> events = new HashSet<Event>();
	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "DATAGRID_CODE", referencedColumnName = "code")
	@Index(name = "idx_ECBUTTON_DATAGRID")
	@Fetch(FetchMode.SELECT)
	private DataGrid dataGrid;
	@Enumerated(EnumType.STRING)
	private RegionType regionType;
	private String scriptCode;// 按钮对应的脚本
	private String config;
	private String permissionCode;
	private String buttonAlign;
	private Boolean isPublished;
	
	private  String signerId;
	private  String positionId;
	private  String roleId;
	private  String powerType;
	private  Boolean signatureEnabled=false;
	private  String  signatureType;
	private String  releaseFelid;
	private Boolean isSignatureConfig;
	private String moduleCode;
	private String entityCode;

	@SuppressWarnings("rawtypes")
	@Transient
	private Map configMap;
	private Boolean projFlag;

	@Override
	protected String _getEntityName() {
		return getClass().getName();
	}

	public void addEvents(Event event) {
		this.events.add(event);
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

	@SuppressWarnings("rawtypes")
	public Map getConfigMap() {
		if (this.configMap == null) {
			this.configMap = (Map) SerializeUitls.deserialize(getConfig());
		}
		return configMap;
	}

	@Column(columnDefinition = "INTEGER")
	public Boolean getIsCustomFunc() {
		return null == isCustomFunc ? false : isCustomFunc;
	}

}
