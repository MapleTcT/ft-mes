package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.configuration.services.annotation.International;
import com.supcon.supfusion.configuration.services.i18n.NameInternationalSerialzer;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * 实体配置：模块
 * 
 * 
 * @author yaowei
 * @version $Id$
 */
@Getter
@Setter
@javax.persistence.Entity
//@Table(name = Module.TABLE_NAME)
public class Module extends AbstractAuditUniqueCodeEntity implements Serializable {

	private static final long serialVersionUID = 7635333342655214403L;
	public static final String TABLE_NAME = "ec_module";
	@Transient
	protected EcEnv ecEnv = EcEnv.product;
	@International
	@JsonSerialize(using = NameInternationalSerialzer.class)
	private String name;
	private String artifact;
	private String projectVersion;// 当前版本
	@Transient
	private String lastVersion;// 上一个版本
	private String initialVersion;// 初始版本
	private String description;
	private String deployOrder;
	private Boolean isInherentedBase = false;// 是否固有基础类型
	private Boolean isNewGenerate = false;
	private Boolean projFlag;
	private Boolean isReadOnly = false;
	private Boolean isHide = false;
	@Transient
	private String iconSkin;
	@International
	private String category;
	private Date publishTime;
	@Transient
	private Integer deployType; //1:快速发布 2：普通发布
	@Transient
	private Integer level;		//模块依赖层级
	@Transient
	private Integer entitySize;		//实体数量
	@Transient
	private Boolean isRelation;		//是否是被依赖模块
	private String type = "Mis";    // 微服务类型:Mis，老的bap模块为null
	private String acronym;		//缩略名称，做为数据库前缀
	private Boolean isProto = false;
	private Boolean mainModule = false;
	@Transient
	private String companyIds;
	@Transient
	private String moduleRelationDeleteIds;
	@Transient
	private String moduleReferenceDeleteIds;
	@Transient
	private String moduleReferenceAddIds;
	@Transient
	private String moduleReferencemultiselectIDs;
	@Transient
	private String moduleReferencemultiselectNames;
	@Transient
	private Set<Entity> entities = new HashSet<Entity>();

	@Override
	protected String _getEntityName() {
		return getClass().getName();
	}

}