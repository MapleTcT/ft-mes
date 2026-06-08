package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.configuration.services.annotation.International;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.enums.FieldType;
import com.supcon.supfusion.configuration.services.enums.ShowFormat;
import com.supcon.supfusion.configuration.services.i18n.DispalyNameInternationalSerialzer;
import com.supcon.supfusion.configuration.services.utils.Inflector;
import com.supcon.supfusion.configuration.services.utils.SerializeUitls;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.apache.commons.lang.StringEscapeUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.Entity;
import javax.persistence.*;
import java.io.Serializable;
import java.util.Map;

@Data
@Entity
//@Table(name = Property.TABLE_NAME)
public class Property extends AbstractAuditUniqueCodeEntity implements Serializable {
	private static final long serialVersionUID = 6890487959703921154L;
	public static final String TABLE_NAME = "ec_property";
	private String name;
	@Transient
	protected EcEnv ecEnv = EcEnv.product;
	private String moduleCode;

	private String entityCode;
	@International
	@JsonSerialize(using = DispalyNameInternationalSerialzer.class)
	private String displayName;

	// private Boolean isFk;// 是否做外键使用
	// private String fkTargetEntity;// 外键指向的Entity名，非表名.但是没有前缀
	// private String fkTargetColumn;// 外键指向的字段名,属性名,非表内字段名
	// private Property fkTargetProperty;
	// private Integer fkType;// 0 - 1->1 ; 1 - 1->N ; 2 - N->N
	// private Property fkThisProperty;//如果fkType =
	// 1,即1->N的关系时，需要建立关联表，此属性记录当前entity中哪个属性参与关联
	@Enumerated(EnumType.STRING)
	private DbColumnType type;// 数据库类型
	@Enumerated(EnumType.STRING)
	private ShowFormat format;// 显示格式
	@Enumerated(EnumType.STRING)
	private FieldType fieldType; // 显示类型

	private Boolean isIndex = false;// 是否索引
	private Boolean nullable = false;// 是否可空
	private Integer maxLength;// 最大长度
	private Integer decimalNum;// 小数位数,当类型是浮点数时可用
	private Boolean multable = false;// 是否可以多选

	private Boolean isIgnoreAudit = false; // 是否忽略数据日志
	private Boolean isUnique = false;// 是否唯一
	private Boolean isInherent = false;// 是否是固有字段
	private Boolean isPk = false;// 是否是主键
	@Lob
	private String fillcontent;// 填充值
	@Transient
	private String fillcontentEscapeHtml;
	@Transient
	private Map<String, Object> fillcontentJson;// 填充值
	private String attributes;// 编码配置
	@Transient
	private String attributesEscapeHtml;
	private String description;
	@ManyToOne
	@JoinColumn(name = "MODEL_CODE", referencedColumnName = "code")
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	private Model model;
	private Boolean isUsedForList = false;// 是否可用于列表
	private Boolean isMainDisplay = false;// 是否主显示字段
	@Column(name = "IS_SENSITIVE")
	private Boolean sensitive=false;
	private String defaultValue;
	private Boolean isUsedMneCode = false;
	@Column(length = 1)
	private Boolean isControl = false;// 是否受控
	@Column(length = 1)
	private Boolean isBussinessKey = false; // 是否业务主键

	private String picWidth; // 图片字段宽

	private String picHeight;// 图片字段高

	private Boolean stretch; // 图片字是否拉伸

	private Boolean isUsedForSearch = false; // 字段用于全文检索（建索引）
	private Boolean isMainAssociated = false; // 关联到主模型的属性
	private Boolean noAnalyzer = false; // 是否分词（字符类型）

	private Boolean seniorSystemCode = false; // 是否高级系统编码
	private String columnName;
	@Transient
	private String orgColumnName;
	private Boolean isMneWholeLikeQuery = false;

	private String fetchMode = "SELECT";
	@Transient
	private Boolean isTreeSystemCode = false;

	@Transient
	private Integer showWidth;

	private Boolean isCustom = false; // 是否是自定义字段

	private Boolean isEngine; // 是否已被工程修改

	private Boolean projFlag;

	private Boolean projCustomInUse;// 该自定义字段是否在工程期启用

	private Boolean isGroupObject;
	
	private Boolean onlyLeaf = false;
	
	private Integer sort; // 排序字段
	private Boolean isHidden = false; // 是否是隐藏字段

	// 关联字段
	public static final int ONE_TO_ONE = 1;
	public static final int MANY_TO_ONE = 2;
	public static final int ONE_TO_MANY = 3;
	public static final int MANY_TO_MANY = 4;

	private Integer associatedType;// 1 - 1->1 ; 2 - N->1 ; 3 - 1->N ; 4 - N->N
	@ManyToOne
	@JoinColumn(name = "ASSOCIATED_PROPERTY_CODE", referencedColumnName = "code")
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	private Property associatedProperty;

	private Long counterRuleId;

	@Override
	protected String _getEntityName() {
		return getClass().getName();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((getCode() == null) ? 0 : getCode().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Property other = (Property) obj;
		if (getCode() == null) {
			if (other.getCode() != null) {
				return false;
			}
		} else if (!getCode().equals(other.getCode())) {
			return false;
		}
		return true;
	}

	@Column(name="COLUMN_NAME")
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

	@Override
	public String toString() {
		return "Property [code=" + getCode() + ", name=" + name + ", displayName=" + displayName + ", type=" + type + "]";
	}

	@Override
	public Integer getVersion() {
		return null == super.getVersion() ? 0 : super.getVersion();
	}
	@Lob
	public String getFillcontent() {
		return null == fillcontent ? fillcontent : fillcontent.trim();
	}

	public String getFillcontentEscapeHtml() {
		return StringEscapeUtils.escapeHtml(fillcontent);
	}

	@Lob
	public String getAttributes() {
		return attributes;
	}

	public String getAttributesEscapeHtml() {
		return StringEscapeUtils.escapeHtml(attributes);
	}

	@Transient
	public Map<String, Object> getFillcontentJson() {
		if (fillcontent != null) {
			return SerializeUitls.deserializeJson(fillcontent);
		}
		return null;
	}

	@Transient
	public Map<String, Object> getFillcontentMap(){
		if (this.fillcontent == null) {
			return null;
		}
		return SerializeUitls.deserializeJson(fillcontent);
	}

	@Transient
	public Map<String, Object> getAttributesMap(){
		if (this.attributes == null) {
			return null;
		}
		return SerializeUitls.deserializeJson(this.attributes);
	}
}
