package com.supcon.supfusion.signature.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.signature.base.i18n.NameInternationalSerialzer;
import com.supcon.supfusion.signature.dao.entity.base.LogicBasePO;
import com.supcon.supfusion.signature.dao.enums.EcEnv;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@TableName(value = "ec_entity", autoResultMap=true)
public class Entity extends LogicBasePO {

	private static final long serialVersionUID = 3047829161895387082L;

	private EcEnv ecEnv = EcEnv.product;
	@TableId
	private String code;
	@JsonSerialize(using = NameInternationalSerialzer.class)
	private String name;// 中文
	private String entityName;// 英文
	private Boolean workflowEnabled = false;
	private String description;
	private String prefix;// 单据编号头
	private Boolean groupEnabled = false;// 是否启用组权限
	private Boolean payCloseAttention = false;// 是否启用关注
	private Boolean isBase = false;// 是否基础类型
	private Boolean inherentCommonFlag = false;// 是否固有公用模型
	private Boolean isInherentedBase = false;// 是否固有基础类型
	private Boolean isControl = false;// 是否受控
	private Boolean crossCompanyFlag = false;// 是否跨公司

	private Boolean mobile = false;// 移动支持
	private Boolean enableAclRestrict = false;// 是否启用ACL限制

	private Boolean enableAudit = false; // 是否启用日志

	private Boolean enableRest = false; // 是否启用REST接口

	private Boolean enableWs = false; // 是否启用webservice接口
	private Boolean enableFieldsPermissionConf = false; // 是否启用字段权限配置


	private Boolean projFlag;
	private String moduleCode;

	@TableField(exist = false)
	private Set<Model> models = new HashSet<Model>();

	@TableField(exist = false)
	private Set<View> views = new HashSet<View>();

	@TableField(exist = false)
	private Module module;
}