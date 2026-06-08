/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.security;

import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.base.entities.Role;
import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.base.entities.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

/**
 * BAP平台认证通过后，会将本对象保存到 {@link SecurityContext} 里，方便其他业务获取当前登录用户的信息
 * 
 * @author yaowei
 *
 */
public class OrchidAuthenticationToken extends UsernamePasswordAuthenticationToken {
	protected final Log logger=LogFactory.getLog(getClass());
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4089795041109324533L;

	private Company currentCompany;
	
	private Staff staff;
	
	private Collection<Role> roles;
	
	private Collection<GrantedAuthority> userPermission;
	
	private Company mobileCompany = null;
	
	private Boolean isMobileRequest = false; //是否是移动端Request
	
	private List<Map<String, String>> ipWhiteList;

	public OrchidAuthenticationToken(Object user, Object credentials, Object details, Company currentCompany, Staff staff,
									 Collection<Role> roles, Collection<GrantedAuthority> permissions) {
		super(user, credentials, ((UserDetails) user).getAuthorities());
		super.setDetails(details);
		this.currentCompany = currentCompany;
		this.staff = staff;
		this.roles = Collections.unmodifiableCollection(roles);
		this.userPermission = Collections.unmodifiableCollection(permissions);
	}

	public Company getCurrentCompany() {
		if(null == mobileCompany){
			return currentCompany;
		}else{
			return mobileCompany;
		}
	}
	
	/**
	 * 返回这个用户所拥有的所有功能权限
	 * 
	 * @return
	 */
	public Collection<GrantedAuthority> getUserPermissions() {
		return this.userPermission;
	}
	
	/**
	 * 返回该用户所对应的角色，可能为null。
	 * 
	 * @return
	 */
	public Staff getStaff() {
		return this.staff;
	}
	
	
	/**
	 * 返回这个用户所拥有的所有角色。这个列表无法修改，只能枚举访问
	 * 
	 * @return
	 */
	public Collection<Role> getRoles() {
		return this.roles;
	}
	

	/**
	 * 
	 * @return
	 */
	public User getCurrentUser() {
		return (User) getPrincipal();
	}
	
	/**
	 * 判断该用户是否是指定编号的角色。
	 * 
	 * @param roleCode
	 * @return
	 */
	public boolean hasRole(String roleCode) {
		for(Role role : getRoles()) {
			if(role.getCode().equals(roleCode)) return true;
		}
		return false;
	}	
	

}
