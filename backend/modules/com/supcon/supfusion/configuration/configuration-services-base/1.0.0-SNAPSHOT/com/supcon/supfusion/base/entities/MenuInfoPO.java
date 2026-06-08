package com.supcon.supfusion.base.entities;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "rbac_menuinfo")
public class MenuInfoPO implements Serializable   {
	private static final long serialVersionUID = 2001349731108473987L;

	/**
     * 主键ID
     */
    private Long id;


	@Column(name="VALID")
	private Boolean valid;

    /**
     * 名称
     */
    private String name;

    /**
     * 编码
     */
    private String code;

    /**
     * 版本
     */
    private Integer version;

    /**
     * 父节点ID
     */
    private Long parentId;

    /**
     * 层级
     */
    private Integer layNo;

    /**
     * 层级
     */
    private String layRec;

    /**
     * 顺序
     */
    private Double sort;

    /**
     * 公司ID
     */
    private Long cid;

    /**
     * 全路径
     */
    private String fullPath;

    /**
     * 全路径 菜单名
     */
    private String fullPathName;

    /**
     * 密级
     * SystemCode:
     *  SECRET_CLASS/5 非密
     *  SECRET_CLASS/3 秘密
     *  SECRET_CLASS/2 机密
     *  SECRET_CLASS/6 内部资料
     *  SECRET_CLASS/7 核心商密
     */
    private String securityClass;

    /**
     * 绝对隐藏  1 时 菜单管理、权限管理、以及用户登录后所见的菜单中全部强制隐藏
     */
    private Boolean absoluteHidden;

    /**
     * 是否三员菜单
     */
    private Boolean threeRole;

    /**
     * 请求方式 0:链接页面，1：链接URL
     */
    private Integer showType;


    /**
     * 请求类型
     */
    private Integer requestType;

    /**
     * 隐藏类型
     */
    private Integer hiddenType;

    /**
     * 菜单类型
     */
    private Integer menuType;

    /**
     * 是否隐藏
     */
    private Boolean isHide;

    /**
     * 是否仅集团使用
     */
    private Boolean groupOnly;

    /**
     * SOURCE
     */
    private String source;

    /**
     * 实体编码
     */
    private String entityCode;

    /**
     * 模型CODE
     */
    private String moduleCode;

    /**
     * 是否默认系统
     */
    private Boolean systemDefault;

    /**
     * CSS_CLASS(菜单样式用)
     */
    private String cssClass;

    /**
     * ACTION
     */
    private String actionUrl;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 地址
     */
    private String url;

    /**
     * 打开方式
     */
    private String target;

    /**
     * 备注
     */
    private String memo;

    /**
     * 是否启用
     */
    private Boolean enable;

    /**
     * 是否是叶子节点
     */
    private Boolean leaf;

    /**
     * 是否修改过 修改过的菜单升级时不修改
     */
    private Boolean edited = false;

    /**
     * 资源类型 0是菜单
     */
    private Integer type;

    /**
     * 名称国际化值
     */
    private String nameDisplay;

    /**
     * 所属应用名
     */
    private String app;
    /**
     * 是否不受权限控制
     */
    private Boolean noRestrict=false;
    
    private String route;
    
    private Integer status;

    	
    @Id
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name="NAME")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Column(name="STATUS")
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	@Column(name="CODE")
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	
	@Column(name="VERSION")
	public Integer getVersion() {
		return version;
	}
	public void setVersion(Integer version) {
		this.version = version;
	}
	
	@Column(name="PARENT_ID")
	public Long getParentId() {
		return parentId;
	}
	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}
	
	@Column(name="LAY_NO")
	public Integer getLayNo() {
		return layNo;
	}
	public void setLayNo(Integer layNo) {
		this.layNo = layNo;
	}
	
	@Column(name="LAY_REC")
	public String getLayRec() {
		return layRec;
	}
	public void setLayRec(String layRec) {
		this.layRec = layRec;
	}
	
	@Column(name="SORT")
	public Double getSort() {
		return sort;
	}
	public void setSort(Double sort) {
		this.sort = sort;
	}
	
	@Column(name="CID")
	public Long getCid() {
		return cid;
	}
	public void setCid(Long cid) {
		this.cid = cid;
	}
	
	@Column(name="FULL_PATH")
	public String getFullPath() {
		return fullPath;
	}
	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}
	
	@Column(name="FULL_PATH_NAME")
	public String getFullPathName() {
		return fullPathName;
	}
	public void setFullPathName(String fullPathName) {
		this.fullPathName = fullPathName;
	}
	
	@Column(name="SECURITY_CLASS")
	public String getSecurityClass() {
		return securityClass;
	}
	public void setSecurityClass(String securityClass) {
		this.securityClass = securityClass;
	}
	
	@Column(name="ABSOLUTE_HIDDEN")
	public Boolean getAbsoluteHidden() {
		return absoluteHidden;
	}
	public void setAbsoluteHidden(Boolean absoluteHidden) {
		this.absoluteHidden = absoluteHidden;
	}
	
	@Column(name="THREE_ROLE")
	public Boolean getThreeRole() {
		return threeRole;
	}
	public void setThreeRole(Boolean threeRole) {
		this.threeRole = threeRole;
	}
	
	@Column(name="SHOW_TYPE")
	public Integer getShowType() {
		return showType;
	}
	public void setShowType(Integer showType) {
		this.showType = showType;
	}
	
	@Column(name="REQUEST_TYPE")
	public Integer getRequestType() {
		return requestType;
	}
	public void setRequestType(Integer requestType) {
		this.requestType = requestType;
	}
	
	@Column(name="HIDDEN_TYPE")
	public Integer getHiddenType() {
		return hiddenType;
	}
	public void setHiddenType(Integer hiddenType) {
		this.hiddenType = hiddenType;
	}
	
	@Column(name="MENU_TYPE")
	public Integer getMenuType() {
		return menuType;
	}
	public void setMenuType(Integer menuType) {
		this.menuType = menuType;
	}
	
	@Column(name="IS_HIDE")
	public Boolean getIsHide() {
		return isHide;
	}
	public void setIsHide(Boolean isHide) {
		this.isHide = isHide;
	}
	
	@Column(name="GROUP_ONLY")
	public Boolean getGroupOnly() {
		return groupOnly;
	}
	public void setGroupOnly(Boolean groupOnly) {
		this.groupOnly = groupOnly;
	}
	
	@Column(name="SOURCE")
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	
	@Column(name="ENTITY_CODE")
	public String getEntityCode() {
		return entityCode;
	}
	public void setEntityCode(String entityCode) {
		this.entityCode = entityCode;
	}
	
	@Column(name="MODULE_CODE")
	public String getModuleCode() {
		return moduleCode;
	}
	public void setModuleCode(String moduleCode) {
		this.moduleCode = moduleCode;
	}
	
	@Column(name="SYSTEM_DEFAULT")
	public Boolean getSystemDefault() {
		return systemDefault;
	}
	public void setSystemDefault(Boolean systemDefault) {
		this.systemDefault = systemDefault;
	}
	
	@Column(name="CSS_CLASS")
	public String getCssClass() {
		return cssClass;
	}
	public void setCssClass(String cssClass) {
		this.cssClass = cssClass;
	}
	
	@Column(name="ACTION_URL")
	public String getActionUrl() {
		return actionUrl;
	}
	public void setActionUrl(String actionUrl) {
		this.actionUrl = actionUrl;
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

	@Column(name="ROUTE")
	public String getRoute() {
		return route;
	}
	public void setRoute(String route) {
		this.route = route;
	}
	
	@Column(name="TARGET")
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	
	@Column(name="MEMO")
	public String getMemo() {
		return memo;
	}
	public void setMemo(String memo) {
		this.memo = memo;
	}
	
	@Column(name="ENABLE")
	public Boolean getEnable() {
		return enable;
	}
	public void setEnable(Boolean enable) {
		this.enable = enable;
	}
	
	@Column(name="LEAF")
	public Boolean getLeaf() {
		return leaf;
	}
	public void setLeaf(Boolean leaf) {
		this.leaf = leaf;
	}
	
	@Column(name="EDITED")
	public Boolean getEdited() {
		return edited;
	}
	public void setEdited(Boolean edited) {
		this.edited = edited;
	}
	
	@Column(name="TYPE")
	public Integer getType() {
		return type;
	}
	public void setType(Integer type) {
		this.type = type;
	}
	
	@Column(name="NAME_DISPLAY")
	public String getNameDisplay() {
		return nameDisplay;
	}
	public void setNameDisplay(String nameDisplay) {
		this.nameDisplay = nameDisplay;
	}
	
	@Column(name="APP")
	public String getApp() {
		return app;
	}
	public void setApp(String app) {
		this.app = app;
	}
	
	@Column(name="NO_RESTRICT")
	public Boolean getNoRestrict() {
		return noRestrict;
	}
	public void setNoRestrict(Boolean noRestrict) {
		this.noRestrict = noRestrict;
	}

	public Boolean getValid() {
		return valid;
	}

	public void setValid(Boolean valid) {
		this.valid = valid;
	}

	@Override
	public String toString() {
		return "MenuInfoPO{" +
				"id=" + id +
				", name='" + name + '\'' +
				", code='" + code + '\'' +
				", version=" + version +
				", parentId=" + parentId +
				", layNo=" + layNo +
				", layRec='" + layRec + '\'' +
				", sort=" + sort +
				", cid=" + cid +
				", fullPath='" + fullPath + '\'' +
				", fullPathName='" + fullPathName + '\'' +
				", securityClass='" + securityClass + '\'' +
				", absoluteHidden=" + absoluteHidden +
				", threeRole=" + threeRole +
				", showType=" + showType +
				", requestType=" + requestType +
				", hiddenType=" + hiddenType +
				", menuType=" + menuType +
				", isHide=" + isHide +
				", groupOnly=" + groupOnly +
				", source='" + source + '\'' +
				", entityCode='" + entityCode + '\'' +
				", moduleCode='" + moduleCode + '\'' +
				", systemDefault=" + systemDefault +
				", cssClass='" + cssClass + '\'' +
				", actionUrl='" + actionUrl + '\'' +
				", namespace='" + namespace + '\'' +
				", url='" + url + '\'' +
				", target='" + target + '\'' +
				", memo='" + memo + '\'' +
				", enable=" + enable +
				", leaf=" + leaf +
				", edited=" + edited +
				", type=" + type +
				", nameDisplay='" + nameDisplay + '\'' +
				", app='" + app + '\'' +
				", noRestrict=" + noRestrict +
				'}';
	}
}
