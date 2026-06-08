/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.base.entities.SystemCode;
import com.supcon.supfusion.base.entities.SystemEntity;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import org.hibernate.criterion.DetachedCriteria;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author rockey
 * 
 */
public interface SystemCodeService {

	SystemCode load(String code);

	void deleteSystemEntityAndCode(String moduleCode);

	SystemEntity getSystemEntityByCode(String entityCode);

	List<SystemCode> getSystemCodeByEntity(String entityCode);
	
	List<SystemEntity> getSystemEntityLists(Company company);

	SystemCode getSystemCode(String systemCodeID);

	Map<String, String> getSystemCodeMap(String systemEntityCode);

	Map<String, String> getSystemCodeList(String systemEntityCode, Boolean senior);

	void initializeSystemCode(URL url) throws XMLStreamException, IOException;

	SystemEntity getSystemEntity(Long entityId);

	void saveSystemCode(SystemCode systemCode);

	void saveSystemCodeAndXml(SystemCode systemCode);

	void saveSystemCode(SystemCode systemCode,String strType);

    void saveSystemEntity(SystemEntity systemEntity);

	Page<SystemCode> getBySystemCodePage(Page<SystemCode> page, DetachedCriteria detachedCriteria);

	Set<SystemCode> getTreeList(Company currentCompany, String systemEntityCode, SystemCode systemCode);

	void deleteAndChildren(String systemCodeId, Integer systemCodeVersion);
}
