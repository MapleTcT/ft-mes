package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.base.entities.SystemCode;
import com.supcon.supfusion.configuration.services.annotation.International;
import com.supcon.supfusion.configuration.services.i18n.NameInternationalSerialzer;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@javax.persistence.Entity
//@Table(name = Entity.TABLE_NAME)
public class Entity extends AbstractAuditUniqueCodeEntity implements Serializable {

	private static final long serialVersionUID = 3047829161895387082L;

	public static final String TABLE_NAME = "ec_entity";
	@Transient
	protected EcEnv ecEnv = EcEnv.product;
	@International
	@JsonSerialize(using = NameInternationalSerialzer.class)
	private String name;// 中文
	private String entityName;// 英文
	private Boolean workflowEnabled = false;
	private String description;
	private String prefix;// 单据编号头
	private Boolean groupEnabled = false;// 是否启用组权限
	private Boolean payCloseAttention = false;// 是否启用关注
	// private String moduleCode;
//	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "MODULE_CODE", referencedColumnName = "code")
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	private Module module;
	private Boolean isBase = false;// 是否基础类型
	private Boolean inherentCommonFlag = false;// 是否固有公用模型
	private Boolean isInherentedBase = false;// 是否固有基础类型
	private Boolean isControl = false;// 是否受控
	private Boolean crossCompanyFlag = false;// 是否跨公司
//	@OneToMany(mappedBy = "entity")
//	@Fetch(FetchMode.SELECT)
//	@Where(clause = "valid = 1")
//	@OrderBy(clause = "code asc")
	@Transient
	private Set<Model> models = new HashSet<Model>();
	@JsonIgnore
//	@OneToMany(mappedBy = "entity")
//	@Fetch(FetchMode.SELECT)
//	@Where(clause = "valid = 1")
//	@OrderBy(clause = "code asc")
	@Transient
	private Set<View> views = new HashSet<View>();
	private Boolean mobile = false;// 移动支持
	private Boolean enableAclRestrict = false;// 是否启用ACL限制

	private Boolean enableAudit = false; // 是否启用日志

	private Boolean enableRest = false; // 是否启用REST接口

	private Boolean enableWs = false; // 是否启用webservice接口
	private Boolean enableFieldsPermissionConf = false; // 是否启用字段权限配置

	@JsonIgnore
	@ManyToOne(fetch = FetchType.EAGER, targetEntity = SystemCode.class)
	@JoinColumn(name = "TYPE", nullable = true)
	@NotFound(action = NotFoundAction.IGNORE)
	private SystemCode entityType;
	private Boolean projFlag;
	@JsonIgnore
//	@OneToMany(mappedBy = "entity", cascade = { CascadeType.ALL }, targetEntity = PrintTemplate.class)
//	@Fetch(FetchMode.SELECT)
//	@Where(clause = "valid = 1")
//	@OrderBy(clause = "code asc")
	@Transient
	private List<PrintTemplate> printTemplates = new ArrayList<PrintTemplate>();

	@Override
	public Integer getVersion() {
		return null == super.getVersion() ? 0 : super.getVersion();
	}
	@Override
	protected String _getEntityName() {
		return getClass().getName();
	}
}