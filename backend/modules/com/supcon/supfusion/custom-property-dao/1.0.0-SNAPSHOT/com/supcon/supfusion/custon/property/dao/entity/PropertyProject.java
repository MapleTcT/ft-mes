package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.custon.property.common.enums.DbColumnType;
import com.supcon.supfusion.custon.property.common.enums.EcEnv;
import com.supcon.supfusion.custon.property.common.enums.FieldType;
import com.supcon.supfusion.custon.property.common.enums.ShowFormat;
import com.supcon.supfusion.custon.property.common.i18n.DispalyNameInternationalSerialzer;
import com.supcon.supfusion.custon.property.dao.entity.base.LogicBasePO;
import com.supcon.supfusion.custon.property.dao.utils.Inflector;
import lombok.Data;

@Data
@TableName(value = "project_property", autoResultMap=true)
public class PropertyProject extends LogicBasePO {

	private static final long serialVersionUID = 6890487959703921154L;

	@TableId
	private String code;

	private String name;
	private EcEnv ecEnv = EcEnv.product;

	private String moduleCode;

	private String entityCode;

	@JsonSerialize(using = DispalyNameInternationalSerialzer.class)
	private String displayName;

	// 1,即1->N的关系时，需要建立关联表，此属性记录当前entity中哪个属性参与关联
	private DbColumnType type;// 数据库类型

	private ShowFormat format;// 显示格式

	private FieldType fieldType; // 显示类型  TEXTFIELD 普通文本 TEXTAREA 长文本

	private Boolean isIndex = false;// 是否索引

	private Boolean nullable = false;// 是否可空

	private Integer maxLength;// 最大长度

	private Integer decimalNum;// 小数位数,当类型是浮点数时可用

	private Boolean multable = false;// 是否可以多选

	private Boolean isIgnoreAudit = false; // 是否忽略数据日志

	private Boolean isUnique = false;// 是否唯一

	private Boolean isInherent = false;// 是否是固有字段

	private Boolean isPk = false;// 是否是主键

	private String fillcontent;// 填充值

	private String attributes;// 编码配置

	private String description;

	private Boolean isUsedForList = false;// 是否可用于列表

	private Boolean isMainDisplay = false;// 是否主显示字段

	@TableField(value = "is_sensitive")
	private Boolean sensitive;

	private String defaultValue;

	private Boolean isUsedMneCode = false;

	private Boolean isControl = false;// 是否受控

	private Boolean isBussinessKey = false; // 是否业务主键

	private String picWidth; // 图片字段宽

	private String picHeight;// 图片字段高

	private Boolean stretch; // 图片字是否拉伸

	private Boolean isUsedForSearch = false; // 字段用于全文检索（建索引）

	private Boolean isMainAssociated = false; // 关联到主模型的属性

	private Boolean noAnalyzer = false; // 是否分词（字符类型）

	private Boolean seniorSystemCode = false; // 是否高级系统编码

	private String columnName;

	private Boolean isMneWholeLikeQuery = false; // 助记码查询是否使用全模糊查询，默认不使用

	private String fetchMode = "SELECT";

	private Boolean isCustom = false; // 是否是自定义字段

	private Boolean isEngine; // 是否已被工程修改

	private Boolean projFlag;

	private Boolean projCustomInUse;// 该自定义字段是否在工程期启用

	private Boolean isGroupObject;

	private Boolean onlyLeaf = false;

	private Integer sort; // 排序字段

	private Boolean isHidden = false; // 是否是隐藏字段

	private Long counterRuleId;
	// 关联字段
	public static final int ONE_TO_ONE = 1;
	public static final int MANY_TO_ONE = 2;
	public static final int ONE_TO_MANY = 3;
	public static final int MANY_TO_MANY = 4;

	private Integer associatedType;// 1 - 1->1 ; 2 - N->1 ; 3 - 1->N ; 4 - N->N

	private String associatedPropertyCode;

	private String modelCode;


	public String getColumnName() {
		if(columnName != null && columnName.length() > 0){
			return this.columnName.toUpperCase();
		}
		// return DbUtils.createColumnName(getName());
		if(getIsInherent() && (DbColumnType.OBJECT).equals(getType())) {
			return Inflector.getInstance().columnize(getName()) + "_ID";
		}
		if(!getIsInherent() && (DbColumnType.OBJECT).equals(getType()) && "mainPosition".equals(getName())) {
			return Inflector.getInstance().columnize(getName()) + "_ID";
		}
		if(getName() == null){
			return null;
		}
		return Inflector.getInstance().columnize(getName());
	}

}
