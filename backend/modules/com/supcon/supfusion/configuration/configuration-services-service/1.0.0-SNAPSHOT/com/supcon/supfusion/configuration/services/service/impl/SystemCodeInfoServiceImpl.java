/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.services.BaseServiceImpl;
import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.configuration.services.utils.StringUtils;
import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.base.entities.SystemEntity;
import com.supcon.supfusion.configuration.services.service.SystemCodeService;
import com.supcon.supfusion.configuration.services.entity.ModuleReference;
import com.supcon.supfusion.configuration.services.entity.ModuleRelation;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.dao.ModuleDaoImpl;
import com.supcon.supfusion.configuration.services.dao.ModuleReferenceDao;
import com.supcon.supfusion.configuration.services.dao.ModuleRelationDaoImpl;
import com.supcon.supfusion.configuration.services.service.SystemCodeInfoService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@ServiceApiService("ec_SystemCodeService")
@Transactional
public class SystemCodeInfoServiceImpl extends BaseServiceImpl implements SystemCodeInfoService {
	private static final Logger logger = LoggerFactory.getLogger(SystemCodeInfoService.class);

	@Autowired
	private ModuleDaoImpl moduleDao;
	@Autowired
	private ModuleRelationDaoImpl moduleRelationDao;
	@Autowired
	private ModuleReferenceDao moduleReferenceDao;
	
	@Autowired
	private SystemCodeService systemCodeService;
	@Autowired
	private InternationalService internationalService;

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Map<String, Map<String, Map<String, String>>> getSystemEntityMapByGroup(String moduleCode) {
		if(moduleCode == null){
			Map<String, Map<String, Map<String,String>>> groupMap = new LinkedHashMap<String, Map<String, Map<String,String>>>();
			List<SystemEntity> list = systemCodeService.getSystemEntityLists(getCurrentCompany());
			Set<String> moduleCodes = new HashSet<String>();
			if (list != null && !list.isEmpty()) {
				for(SystemEntity se : list){
					if (null != se.getModuleCode()) {
						moduleCodes.add(se.getModuleCode());
					}
				}
			}
			Map<String, Map<String,String>> map = new LinkedHashMap<String, Map<String,String>>();
			if (moduleCodes != null && !moduleCodes.isEmpty()) {
				for (String s : moduleCodes) {
					Module module = moduleDao.load(s);
					map = new HashMap<String, Map<String,String>>();
					if(null != module){
						if (list != null && !list.isEmpty()) {
							for(SystemEntity se : list){
								Map<String,String> map1 = new HashMap<String,String>();
								if (null != se.getModuleCode() && se.getModuleCode().equals(module.getCode())) {
									map1.put("type", se.getListType().name());
									map1.put("name", internationalService.getI18nValue(se.getName()));
									map1.put("id", se.getId().toString());
									map1.put("companyType", getCurrentCompany().getType());
									map.put(se.getCode(), map1);
								}
							}
						}
						if (map.size() > 0) {
							groupMap.put(internationalService.getI18nValue(module.getName()), map);
						}
					}
				}
			}
			if (list != null && !list.isEmpty()) {
				map = new LinkedHashMap<String, Map<String,String>>();
				for (SystemEntity se : list) {
					Map<String,String> map1 = new HashMap<String,String>();
					if (null == se.getModuleCode() || se.getModuleCode().isEmpty()) {
						map1.put("type", se.getListType().name());
						map1.put("name", internationalService.getI18nValue(se.getName()));
						map.put(se.getCode(), map1);
					}
				}
				if (map.size() > 0) {
					groupMap.put(internationalService.getI18nValue("ec.module.systemcode.other"), map);
				}
			}
			return groupMap;
		}
		Map<String, Map<String, Map<String, String>>> groupMap = new LinkedHashMap<String, Map<String, Map<String, String>>>();
		List<SystemEntity> list = systemCodeService.getSystemEntityLists(getCurrentCompany());
		Set<String> moduleCodes = new HashSet<String>();
		List<ModuleRelation> mrs = moduleRelationDao.findByHql("From ModuleRelation where module.code = ?0", moduleCode);
		for(ModuleRelation mr : mrs){
			moduleCodes.add(mr.getTarget().getCode());
		}
		List<ModuleReference> moduleReferences = moduleReferenceDao.findByHql("From ModuleReference where module.code = ?0 and valid = true", moduleCode);
		for(ModuleReference moduleReference : moduleReferences){
			moduleCodes.add(moduleReference.getTarget().getCode());
		}

		moduleCodes.add(moduleCode);
		moduleCodes.add("sysbase_1.0");
		Map<String, Map<String, String>> map = new LinkedHashMap<String, Map<String, String>>();
		if (moduleCodes != null && !moduleCodes.isEmpty()) {
			for (String s : moduleCodes) {
				if (StringUtils.isEmpty(s)) {
					continue;
				}
				Module module = moduleDao.load(s);
				map = new HashMap<String, Map<String, String>>();
				if(null != module){
					String moduleArtifact = module.getArtifact();
					if ("foundation".equals(module.getArtifact())) {
						moduleArtifact = "sys";
					}
					if (list != null && !list.isEmpty()) {
						for(SystemEntity se : list){
							Map<String, String> map1 = new HashMap<String, String>();
							if (null != se.getModuleCode() && se.getModuleCode().equals(moduleArtifact)) {
								map1.put("type", se.getType());
								map1.put("name", (se.getName() != null ? InternationalResource.get(se.getName()) : se.getCode()));
								map1.put("id", se.getId().toString());
								map1.put("companyType", getCurrentCompany().getType());
								map.put(se.getCode(), map1);
							}
						}
					}
					if (map.size() > 0) {
						groupMap.put(InternationalResource.get(module.getName()), map);
					}
				}
			}
		}
		if (list != null && !list.isEmpty()) {
			map = new LinkedHashMap<String, Map<String, String>>();
			for (SystemEntity se : list) {
				Map<String, String> map1 = new HashMap<String, String>();
				if (null == se.getModuleCode() || se.getModuleCode().isEmpty()) {
					map1.put("type", se.getType());
					map1.put("name", InternationalResource.get(se.getName()));
					map.put(se.getCode(), map1);
				}
			}
			if (map.size() > 0) {
				groupMap.put(InternationalResource.get("ec.module.systemcode.other"), map);
			}
		}
		return groupMap;
	}
	
}
