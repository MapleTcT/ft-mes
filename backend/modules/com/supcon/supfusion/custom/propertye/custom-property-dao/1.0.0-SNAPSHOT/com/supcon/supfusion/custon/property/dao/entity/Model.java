package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.custon.property.common.enums.EcEnv;
import com.supcon.supfusion.custon.property.common.i18n.NameInternationalSerialzer;
import com.supcon.supfusion.custon.property.dao.entity.base.LogicBasePO;
import lombok.Data;

import javax.xml.bind.annotation.XmlTransient;

/**
 * 实体配置：模型
 *
 * @author songjiawei
 *
 */
@Data
@TableName(value = "ec_model",autoResultMap = true)
public class Model extends LogicBasePO {

	private static final long serialVersionUID = -5167596036941833535L;

	@TableId
	private String code;

	public static final int DATA_TYPE_NORMAL = 1;
	public static final int DATA_TYPE_TREE = 2;
	private EcEnv ecEnv = EcEnv.product;

	@JsonSerialize(using = NameInternationalSerialzer.class)
	private String name;

	private String modelName;// 生成表名:同一个module下面不能重复,不同module下面可以重复，因为最终生成的数据库表以module为前缀

	private String description;

	private Boolean isMain = false;// 是否主模型

	private Integer dataType;// 数据类型

	private Integer type;

	private Boolean isExtends = false;

	private String moduleCode;

	private String jpaName;

	private String ecVersion;

	private Boolean inherentCommonFlag = false;// 是否固有公用模型

	private String tableName;

	private Boolean enableSync = false;

	private Boolean enableOperationAudit = false;

	private Boolean enableDataAudit = false;

	private Boolean projFlag;

	@TableField(value = "model_sql")
	private String sql; // sql模型语句

	private String viewSql; // sql模型数据库视图语句 oracle;sqlserver;...

	private Boolean isErrorSql = false;
	public static final String ICONSKIN_SQL = "sql";

	private Boolean isExtraCol = false;// 是否生成大字段
	public static final int TYPE_BASE = 1;// 基础
	public static final int TYPE_INHERENT_BASE = 2;// 已存在的固有基础
	public static final int TYPE_NORMAL = 0;// 普通
	public static final int TYPE_SQL = 3; // SQL模型
	public static final int TYPE_SQLERROR = 4; // SQL模型错误SQL，无字段

	@XmlTransient
	public String entityClass;// 如果type == TYPE_INHERENT_BASE,则需录入此属性，记录完整类名。

	private Boolean isCache = false;// 是否启用缓存

	private Boolean isControl = false;// 是否受控

	private Boolean isMneCode = false; // 是否是用于助记码实体

	@TableField(value = "SPECIAL_AUTH_ISANDREL")
	private Boolean isAndRelation = false;

	private Boolean isConfigSpecial = false;

	@TableField(value = "SPECIALPER_TEMPLATE_SQL")
	private String specialPerTemplateSQL;

	private String entityCode;
//	@TableField(exist = false)
//	private Entity entity;

	private String extendsModelCode;
//	@TableField(exist = false)
//	private Model extendsModelName;
//	@TableField(exist = false)
//	private Set<Property> properties = new HashSet<Property>();


}
