package com.supcon.supfusion.base.entities;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "rbac_menuinfo_company_ref")
public class MenuInfoCompanyRefPO implements Serializable   {
	
	private static final long serialVersionUID = -7949697512732669641L;

	/**
     * 主键ID
     */
    private Long id;
    
    /**
     * 菜单ID
     */
    private Long menuinfoId;

    /**
     * 公司ID
     */
    private Long companyId;

    /**
     * 公司名
     */
    private String companyName;

    private String appId;

    
    @Id
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name="MENUINFO_ID")
	public Long getMenuinfoId() {
		return menuinfoId;
	}
	public void setMenuinfoId(Long menuinfoId) {
		this.menuinfoId = menuinfoId;
	}
	
	@Column(name="COMPANY_ID")
	public Long getCompanyId() {
		return companyId;
	}
	public void setCompanyId(Long companyId) {
		this.companyId = companyId;
	}
	
	@Column(name="COMPANY_NAME")
	public String getCompanyName() {
		return companyName;
	}
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	@Column(name="APPID")
	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}
}
