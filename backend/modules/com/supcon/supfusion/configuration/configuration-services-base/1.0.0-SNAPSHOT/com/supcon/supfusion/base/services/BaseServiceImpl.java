/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.base.services;


import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.base.entities.User;
import com.supcon.supfusion.base.enums.CompanyType;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import org.springframework.context.i18n.LocaleContextHolder;

public class BaseServiceImpl<T> {

	public String getUserLanguage() {
		return "zh".equals(LocaleContextHolder.getLocale().toString())?"zh_CN":LocaleContextHolder.getLocale().toString();
	}

	public Staff getCurrentStaff() {
		Staff staff = new Staff();
		staff.setId(UserContext.getUserContext().getStaffId());
		staff.setCode(UserContext.getUserContext().getStaffCode());
		staff.setName(UserContext.getUserContext().getStaffName());
		return staff;
	}

	public User getCurrentUser() {
		User user = new User();
		user.setId(UserContext.getUserContext().getUserId());
		user.setName(UserContext.getUserContext().getUserName());
		user.setStaff(getCurrentStaff());
		return user;
	}

	public Company getCurrentCompany() {
		Company company = new Company();
		company.setId(UserContext.getUserContext().getCompanyId());
		company.setCode(UserContext.getUserContext().getCompanyCode());
		company.setName(UserContext.getUserContext().getCompanyName());
		company.setType(CompanyType.UNIT.toString());
		return company;
	}

	public Long getCurrentCompanyId() {
		Company company = getCurrentCompany();
		if(null != company){
			return getCurrentCompany().getId();
		}else{
			return null;
		}
	}


}
