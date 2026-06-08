package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.configuration.services.annotation.International;
import com.supcon.supfusion.configuration.services.enums.DialogType;
import com.supcon.supfusion.configuration.services.enums.ShowType;
import com.supcon.supfusion.configuration.services.enums.ViewType;
import com.supcon.supfusion.configuration.services.i18n.*;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 
 * @author songjiawei
 * 
 */
@Getter
@Setter
@javax.persistence.Entity
//@Table(name = View.TABLE_NAME)
public class View extends AbstractAuditUniqueCodeEntity implements Serializable {

	private static final long serialVersionUID = 7119612377276341004L;
	public static final String TABLE_NAME = "ec_view";
	public static final String MOBILE_VIEW_SUFFIX = "__mobile__";
	public static final String MS_SERVICE = "msService";
	public static final String PROJ_FLAG = "proj";
	@Transient
	protected EcEnv ecEnv = EcEnv.product;
	private String name;
	@International
	@JsonSerialize(using = DispalyNameInternationalSerialzer.class)
	private String displayName;
	@International
	@JsonSerialize(using = TitleInternationalSerialzer.class)
	private String title;
	@Enumerated(EnumType.STRING)
	private ViewType type;
	@Column(name = "OPEN_TYPE", nullable = true, length = 10)
	private String openType;
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "ASS_VIEW_CODE", referencedColumnName = "CODE")
	@Fetch(FetchMode.SELECT)
	private View assView;
	private Boolean usedForWorkFlow = false; // 是否主列表视图
	private Boolean mainView = false;// 是否主查看视图
	private Boolean mainRef = false;// 是否主参照视图
	private String description;
	@ManyToOne
	@JoinColumn(name = "ENTITY_CODE", referencedColumnName = "CODE")
	@Fetch(FetchMode.SELECT)
	private Entity entity;
	@ManyToOne
	@JoinColumn(name = "ASS_MODEL_CODE", referencedColumnName = "CODE")
	@Fetch(FetchMode.SELECT)
	private Model assModel;// 关联模型
	@OneToOne(fetch=FetchType.LAZY, mappedBy = "view")
	@Fetch(FetchMode.JOIN)
	private ExtraView extraView;
	@JsonIgnore
	@OneToMany(mappedBy = "view",fetch=FetchType.EAGER)
	@Fetch(FetchMode.SELECT)
	private List<FastQueryJson> fastQueryJson;
	@JsonIgnore
	@OneToMany(mappedBy = "view",fetch=FetchType.EAGER)
	@Fetch(FetchMode.SELECT)
	private List<AdvQueryJson> advQueryJson;
	@Transient
	private FastQueryJson fqj;
	@Transient
	private AdvQueryJson aqj;
	@Transient
	private DefaultAdvCond defaultAdvCond;
	private Boolean retrialFlag;// 是否支持弃审--只有启用工作流的查看视图才有弃审

	private String scriptCode;// 弃审对应的脚本
	private String url;
	private Boolean customFlag;
	@Enumerated(EnumType.STRING)
	private DialogType dialogType;
	private Integer height;
	private Integer width;
	@Enumerated(EnumType.STRING)
	private ShowType showType;// 显示方式:片段\单个\布局
	private String layoutCode;
	private Boolean closePageAfterSave = false;// 保存单据后，是否关闭页面
	private Boolean isControl = false;// 是否受控
	private Boolean isAudit = false;// 允许查看日志
	private Boolean hasAttachment = false;// 是否有附件
	private Boolean dealInfoShow = false;// 基础页面是否显示处理意见
	private String dealInfoGroup; // 处理意见分类
	private Boolean usedForTree = false;
	private Boolean includeChildren = false;
	private Boolean isReference;
	//@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "REFERENCE_VIEW_CODE", referencedColumnName = "code")
	@Fetch(FetchMode.SELECT)
	private View reference;
	private String assTreeModelCode;
	private String assTreeLayRec;
	private String assTreePath;
	private Integer dataGridType;//
	public static final int DATA_GRID_TYPE_NORMAL = 0;
	public static final int DATA_GRID_TYPE_EX = 1;

	@Transient
	private List<DataGrid> dataGrids;
	@Transient
	private List<Sql> sqls;
	@OneToOne(mappedBy = "view")
	@Fetch(FetchMode.JOIN)
	private ExtraQueryJson extraQueryJson;
	@ManyToOne
	@JoinColumn(name = "SHADOW_VIEW_CODE", referencedColumnName = "code")
	@Fetch(FetchMode.SELECT)
	private View shadowView;

	private Boolean isShadow = false;

	private Boolean isSign = false;
	private Boolean isHandSign = false;
	private Boolean isPrint = false;// 允许打印
	private Boolean isPermission = false;// 启用
	private Boolean controlPrint = false;
	@International
	@JsonSerialize(using = ControlInternationalSerialzer.class)
	private String controlName;// 控件打印操作名称
	private String controlCode;// 操作CODE后缀
	@International
	@JsonSerialize(using = ControlSetingInternationalSerialzer.class)
	private String controlSetingName;// 打印控件设置名称
	private String moduleCode;
	private String permissionCode;
	private String operateUrl;
	@International
	@JsonSerialize(using = RefOperateInternationalSerialzer.class)
	private String refOperateName;// 参照视图权限名称

	@Transient
	private List<Field> fields;
	@JsonIgnore
	@OneToMany(mappedBy = "view",fetch=FetchType.EAGER)
	@Where(clause = "valid = 1")
	@Fetch(FetchMode.SELECT)
	private List<Button> buttons;
	@Transient
	private List<Event> events;

	private Boolean onlyForQuery = false; // 仅用于查询，而不用于仅查待办
	private Boolean mobile = false;// 移动视图标志
	private Boolean mobileEnableFlag = false;// 移动视图启用标志
	@Transient
	private Boolean existMobileConfig = false;// 是否存在移动配置

	private Boolean isBatchControlPrint;// 是否允许批量控件打印
	@OneToOne(fetch=FetchType.LAZY, targetEntity = View.class)
	@JoinColumn(name = "BATCH_CONTROL_PRINT_VIEW_CODE")
	@Fetch(FetchMode.SELECT)
	private View batchControlPrintSelectView;// 批量控件打印视图配置
	private Boolean enableSimpleDealInfo;// 是否显示简单样式的处理意见

	private Boolean importFlag; // 导入导出配置标志 0用于导出 1用于导入

	private Boolean hasCustomSection = false; // 视图是否包含自定义字段区域

	private Boolean attachmentFlag = Boolean.FALSE; //附件标识，只有查看视图支持

	private Integer editViewType = 0; // 0表示编辑、查看视图普通类型，1表示编辑、查看视图增强型类型
	private Boolean projFlag;
	private Integer inheritType;// 继承类型 半继承 1 全继承 2
	private Date publishTime;
	@International
	@JsonSerialize(using = MenuInternationalSerialzer.class)
	private String menuName;
	private Long parentMenuId;
	private Boolean projEnabled;// 工程视图是否启用
	private String parentMenuCode;

	public static String fl(String s) {
		if (null != s) {
			return Character.toLowerCase(s.charAt(0)) + s.substring(1);
		}
		return null;
	}

	@Override
	protected String _getEntityName() {
		return getClass().getName();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((entity == null) ? 0 : entity.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		View other = (View) obj;
		if (entity == null) {
			if (other.entity != null) {
				return false;
			}
		} else if (!entity.equals(other.entity)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	@Override
	public Integer getVersion() {
		return null == super.getVersion() ? 0 : super.getVersion();
	}

}
