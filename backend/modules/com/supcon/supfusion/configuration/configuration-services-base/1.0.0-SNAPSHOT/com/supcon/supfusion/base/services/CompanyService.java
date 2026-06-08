/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.base.services;

import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import org.hibernate.criterion.DetachedCriteria;

import java.util.List;

/**
 * @author rockey
 * 
 */
public interface CompanyService {

	Company getCompanyByCode(String code);

	Company get(Long id);

	List<Company> getAllCompanies();

	Page<Company> getByPage(Page<Company> page, DetachedCriteria detachedCriteria);


}
