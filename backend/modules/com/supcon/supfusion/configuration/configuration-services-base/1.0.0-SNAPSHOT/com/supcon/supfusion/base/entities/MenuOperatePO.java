package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.base.enums.MenuOperateType;
import org.springframework.util.ObjectUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * <p>
 * 操作表
 * </p>
 *
 * @author fjh
 * @since 
 */
@Entity
@Table(name = "rbac_menuoperate")
public class MenuOperatePO implements Serializable {

	private static final long serialVersionUID = -5069668419250846917L;

	/**
     * 主键ID
     */
    private Long id;

    /**
     * 版本
     */
    private Integer version;


    /**
     * 公司ID
     */
    private Long cid;

    /**
     * 是否允许委托
     */
    private Boolean isAllowProxy;

    /**
     * 是否隐藏
     */
    private Boolean isHidden;

    /**
     * 是否三员菜单
     */
    private Boolean threeRole;

    /**
     * 视图编码
     */
    private String viewCode;

    /**
     * 是否查询操作
     */
    private Boolean isQuery;

    /**
     * 该操作的其他数据限制跟岗位限制等的或.与关系标志位,默认为AND
     */
    private Boolean isOrrelation;

    /**
     * 启用数据权限
     */
    private Boolean enableDataPermission;

    /**
     * 启用自定义权限
     */
    private Boolean enableCustomPermission;


    /**
     * 启用业务权限
     */
    private Boolean forFlowPermission ;

    /**
     * 无限制
     */
    private Boolean enableNorestrict;

    /**
     * 启用处理人
     */
    private Boolean enableDealerpermission;

    /**
     * 启用指定人员
     */
    private Boolean enableAssignstaff;

    /**
     * 启用指定岗位
     */
    private Boolean enableAssignpos;

    /**
     * 岗位限制
     */
    private Boolean enablePosrestrict;

    /**
     * 指定部门
     */
    private Boolean enableAssignDept;

    /**
     * 部门限制
     */
    private Boolean enableDeptrict;

    /**
     * 启用组限制
     */
    private Boolean enableGrouprestrict;

    /**
     * 实体编码
     */
    private String entityCode;

    /**
     * 忽视权限
     */
    private Boolean ignorePermission;

    /**
     * 是否是主列表视图的查询操作
     */
    private Boolean powerFlag;

    /**
     * 工作流版本
     */
    private String flowVersion;

    /**
     * 工作流KEY
     */
    private String flowKey;

    private Integer msgAssembled;

    private Long deploymentId;

    private String menuOperateType;


	/**
	 * 菜单ID
	 */
	private Long menuinfoId;
	/**
     * ID
     */
    private String iconCls;

	@Column(name="VALID")
	private Integer valid;

    /**
     * 排序
     */
    private Double sort;

    /**
     * 备注
     */
    private String memo;

    /**
     * 打开方式
     */
    private String target;

    /**
     * ACTION
     */
    private String action;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 地址
     */
    private String url;

    /**
     * 中文名
     */
    private String nameZhCn;

    /**
     * 名称
     */
    private String name;

    /**
     * 编码
     */
    private String code;

    /**
     * 所属应用名
     */
    private String app;

    private Boolean edited;

    /**
     * 默认操作标识，默认操作不可删除
     */
    @Column(name="DEFAULT_OPERATE")
    private Boolean defaultOperate;

    /**
     * 名称国际化值
     */
    private String nameDisplay;
    
    /**
     * 模块编码
     */
    private String moduleCode;
    
    @Column(name="MODULE_CODE")
    public String getModuleCode() {
		return moduleCode;
	}

	public void setModuleCode(String moduleCode) {
		this.moduleCode = moduleCode;
	}

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name="ROW_VERSION")
	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}
	
	@Column(name="CID")
	public Long getCid() {
		return cid;
	}

	public void setCid(Long cid) {
		this.cid = cid;
	}
	
	@Column(name="IS_ALLOW_PROXY")
	public Boolean getIsAllowProxy() {
		return isAllowProxy;
	}

	public void setIsAllowProxy(Boolean isAllowProxy) {
		this.isAllowProxy = isAllowProxy;
	}
	
	@Column(name="IS_HIDDEN")
	public Boolean getIsHidden() {
		return isHidden;
	}

	public void setIsHidden(Boolean isHidden) {
		this.isHidden = isHidden;
	}
	
	@Column(name="THREE_ROLE")
	public Boolean getThreeRole() {
		return threeRole;
	}

	public void setThreeRole(Boolean threeRole) {
		this.threeRole = threeRole;
	}
	
	@Column(name="VIEW_CODE")
	public String getViewCode() {
		return viewCode;
	}

	public void setViewCode(String viewCode) {
		this.viewCode = viewCode;
	}
	
	@Column(name="IS_QUERY")
	public Boolean getIsQuery() {
		return isQuery;
	}

	public void setIsQuery(Boolean isQuery) {
		this.isQuery = isQuery;
	}
	
	@Column(name="IS_ORRELATION")
	public Boolean getIsOrrelation() {
		return isOrrelation;
	}

	public void setIsOrrelation(Boolean isOrrelation) {
		this.isOrrelation = isOrrelation;
	}
	
	@Column(name="ENABLE_DATAPERMISSION")
	public Boolean getEnableDataPermission() {
		return enableDataPermission;
	}

	public void setEnableDataPermission(Boolean enableDataPermission) {
		this.enableDataPermission = enableDataPermission;
	}
	
	@Column(name="ENABLE_CUSTOMPERMISSION")
	public Boolean getEnableCustomPermission() {
		return enableCustomPermission;
	}

	public void setEnableCustomPermission(Boolean enableCustomPermission) {
		this.enableCustomPermission = enableCustomPermission;
	}
	
	@Column(name="FOR_FLOW_PERMISSION")
	public Boolean getForFlowPermission() {
		return forFlowPermission;
	}

	public void setForFlowPermission(Boolean forFlowPermission) {
		this.forFlowPermission = forFlowPermission;
	}
	
	@Column(name="ENABLE_NORESTRICT")
	public Boolean getEnableNorestrict() {
		return enableNorestrict;
	}

	public void setEnableNorestrict(Boolean enableNorestrict) {
		this.enableNorestrict = enableNorestrict;
	}
	
	@Column(name="ENABLE_DEALERPERMISSION")
	public Boolean getEnableDealerpermission() {
		return enableDealerpermission;
	}

	public void setEnableDealerpermission(Boolean enableDealerpermission) {
		this.enableDealerpermission = enableDealerpermission;
	}
	
	@Column(name="ENABLE_ASSIGNSTAFF")
	public Boolean getEnableAssignstaff() {
		return enableAssignstaff;
	}

	public void setEnableAssignstaff(Boolean enableAssignstaff) {
		this.enableAssignstaff = enableAssignstaff;
	}
	
	@Column(name="ENABLE_ASSIGNPOS")
	public Boolean getEnableAssignpos() {
		return enableAssignpos;
	}

	public void setEnableAssignpos(Boolean enableAssignpos) {
		this.enableAssignpos = enableAssignpos;
	}
	
	@Column(name="ENABLE_POSRESTRICT")
	public Boolean getEnablePosrestrict() {
		return enablePosrestrict;
	}

	public void setEnablePosrestrict(Boolean enablePosrestrict) {
		this.enablePosrestrict = enablePosrestrict;
	}
	
	@Column(name="ENABLE_ASSIGNDEPT")
	public Boolean getEnableAssignDept() {
		return enableAssignDept;
	}

	public void setEnableAssignDept(Boolean enableAssignDept) {
		this.enableAssignDept = enableAssignDept;
	}
	
	@Column(name="ENABLE_DEPTRICT")
	public Boolean getEnableDeptrict() {
		return enableDeptrict;
	}

	public void setEnableDeptrict(Boolean enableDeptrict) {
		this.enableDeptrict = enableDeptrict;
	}
	
	@Column(name="ENABLE_GROUPRESTRICT")
	public Boolean getEnableGrouprestrict() {
		return enableGrouprestrict;
	}

	public void setEnableGrouprestrict(Boolean enableGrouprestrict) {
		this.enableGrouprestrict = enableGrouprestrict;
	}
	
	@Column(name="ENTITY_CODE")
	public String getEntityCode() {
		return entityCode;
	}

	public void setEntityCode(String entityCode) {
		this.entityCode = entityCode;
	}
	
	@Column(name="IGNORE_PERMISSION")
	public Boolean getIgnorePermission() {
		return ignorePermission;
	}

	public void setIgnorePermission(Boolean ignorePermission) {
		this.ignorePermission = ignorePermission;
	}
	
	@Column(name="POWER_FLAG")
	public Boolean getPowerFlag() {
		return powerFlag;
	}

	public void setPowerFlag(Boolean powerFlag) {
		this.powerFlag = powerFlag;
	}
	
	@Column(name="FLOW_VERSION")
	public String getFlowVersion() {
		return flowVersion;
	}

	public void setFlowVersion(String flowVersion) {
		this.flowVersion = flowVersion;
	}
	
	@Column(name="FLOW_KEY")
	public String getFlowKey() {
		return flowKey;
	}

	public void setFlowKey(String flowKey) {
		this.flowKey = flowKey;
	}
	
	@Column(name="MSG_ASSEMBLED")
	public Integer getMsgAssembled() {
		return msgAssembled;
	}

	public void setMsgAssembled(Integer msgAssembled) {
		this.msgAssembled = msgAssembled;
	}
	
	@Column(name="DEPLOYMENT_ID")
	public Long getDeploymentId() {
		return deploymentId;
	}

	public void setDeploymentId(Long deploymentId) {
		this.deploymentId = deploymentId;
	}
	
	@Column(name="MENUOPERATETYPE")
	public String getMenuOperateType() {
		return menuOperateType;
	}

	public void setMenuOperateType(String menuOperateType) {
		if (!ObjectUtils.isEmpty(menuOperateType)) {
			this.menuOperateType = menuOperateType;
		} else {
			this.menuOperateType = null;
		}
	}
	
	@Column(name="MENUINFO_ID")
	public Long getMenuinfoId() {
		return menuinfoId;
	}

	public void setMenuinfoId(Long menuinfoId) {
		this.menuinfoId = menuinfoId;
	}
	
	@Column(name="ICON_CLS")
	public String getIconCls() {
		return iconCls;
	}

	public void setIconCls(String iconCls) {
		this.iconCls = iconCls;
	}
	
	@Column(name="SORT")
	public Double getSort() {
		return sort;
	}

	public void setSort(Double sort) {
		this.sort = sort;
	}
	
	@Column(name="MEMO")
	public String getMemo() {
		return memo;
	}

	public void setMemo(String memo) {
		this.memo = memo;
	}
	
	@Column(name="TARGET")
	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}
	
	@Column(name="ACTION_URL")
	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}
	
	@Column(name="NAMESPACE")
	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	
	@Column(name="URL")
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	@Column(name="NAME_ZH_CN")
	public String getNameZhCn() {
		return nameZhCn;
	}

	public void setNameZhCn(String nameZhCn) {
		this.nameZhCn = nameZhCn;
	}
	
	@Column(name="NAME")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Column(name="CODE")
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
	
	@Column(name="APP")
	public String getApp() {
		return app;
	}

	public void setApp(String app) {
		this.app = app;
	}
	
	@Column(name="DEFAULT_OPERATE")
	public Boolean getDefaultOperate() {
		return defaultOperate;
	}

	public void setDefaultOperate(Boolean defaultOperate) {
		this.defaultOperate = defaultOperate;
	}
	
	@Column(name="NAME_DISPLAY")
	public String getNameDisplay() {
		return nameDisplay;
	}

	public void setNameDisplay(String nameDisplay) {
		this.nameDisplay = nameDisplay;
	}

	public Integer getValid() {
		return valid;
	}

	public void setValid(Integer valid) {
		this.valid = valid;
	}

	@Column(name="EDITED")
	public Boolean getEdited() {
		return edited;
	}

	public void setEdited(Boolean edited) {
		this.edited = edited;
	}

	@Override
	public String toString() {
		return "MenuOperatePO{" +
				"id=" + id +
				", version=" + version +
				", cid=" + cid +
				", isAllowProxy=" + isAllowProxy +
				", isHidden=" + isHidden +
				", threeRole=" + threeRole +
				", viewCode='" + viewCode + '\'' +
				", isQuery=" + isQuery +
				", isOrrelation=" + isOrrelation +
				", enableDataPermission=" + enableDataPermission +
				", enableCustomPermission=" + enableCustomPermission +
				", forFlowPermission=" + forFlowPermission +
				", enableNorestrict=" + enableNorestrict +
				", enableDealerpermission=" + enableDealerpermission +
				", enableAssignstaff=" + enableAssignstaff +
				", enableAssignpos=" + enableAssignpos +
				", enablePosrestrict=" + enablePosrestrict +
				", enableAssignDept=" + enableAssignDept +
				", enableDeptrict=" + enableDeptrict +
				", enableGrouprestrict=" + enableGrouprestrict +
				", entityCode='" + entityCode + '\'' +
				", ignorePermission=" + ignorePermission +
				", powerFlag=" + powerFlag +
				", flowVersion='" + flowVersion + '\'' +
				", flowKey='" + flowKey + '\'' +
				", msgAssembled=" + msgAssembled +
				", deploymentId=" + deploymentId +
				", menuOperateType='" + menuOperateType + '\'' +
				", menuinfoId=" + menuinfoId +
				", iconCls='" + iconCls + '\'' +
				", sort=" + sort +
				", memo='" + memo + '\'' +
				", target='" + target + '\'' +
				", action='" + action + '\'' +
				", namespace='" + namespace + '\'' +
				", url='" + url + '\'' +
				", nameZhCn='" + nameZhCn + '\'' +
				", name='" + name + '\'' +
				", code='" + code + '\'' +
				", app='" + app + '\'' +
				", defaultOperate=" + defaultOperate +
				", nameDisplay='" + nameDisplay + '\'' +
				'}';
	}
}
