/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import flexjson.JSONDeserializer;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 获取TabView
 * 
 * @author zhuyuyin
 * @version 1.0
 */
@Component
@Slf4j
public class GetTabViewMethod implements TemplateMethodModelEx {
	private static final String PIMS_SYS_MANAGEMENT_ROLE="PIMS.sysmanagement.rolemanage";//管理组配置
	private static final String PIMS_SYS_MANAGEMENT_USER="PIMS.sysmanagement.usermanage";//用户管理
	private static final String PIMS_SYS_MANAGEMENT_ATS="PIMS.sysmanagement.ats";//自控投运率
	private static final String PIMS_SYS_MANAGEMENT_QMS="PIMS.sysmanagement.qms";//品质管理
	private static final String PIMS_SYS_MANAGEMENT_AAS="PIMS.sysmanagement.aas";//报警管理

	/*
	 * (non-Javadoc)
	 * 
	 * @see freemarker.template.TemplateMethodModel#exec(java.util.List)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		List<Map<String, String>> list = new LinkedList<Map<String, String>>();
		String userName="";
		String roleFullName="";
		try {
			if(null != arguments && !arguments.isEmpty()){
				String jsonString = (String) arguments.get(0);
				JSONDeserializer deserializer = new JSONDeserializer();
				list = (List<Map<String, String>>) deserializer.deserialize(jsonString);
				if(null == list){
					list =  new LinkedList<Map<String, String>>();
				}
				if(arguments.size() > 1){
					String extensionJson = (String) arguments.get(1);
					List<Map<String, String>> extensionList = (List<Map<String, String>>) deserializer.deserialize(extensionJson);
					if(null != extensionList && !extensionList.isEmpty()){
						list.addAll(extensionList);
					}
				}
				if(arguments.size()> 2){
					userName=(String)arguments.get(2);
				}
				if(arguments.size()> 3){
					roleFullName=(String)arguments.get(3);
				}
				
				
				if(!list.isEmpty()){
					for(Map<String,String> map : list){
						String key = map.get("label");
						if(null != key && key.trim().length() > 0){
							if(key.equals(PIMS_SYS_MANAGEMENT_USER)){
//								map.put("requestUrl", map.get("requestUrl")+"?srcsys=bap&type=user&id="+userName);
							}else if(key.equals(PIMS_SYS_MANAGEMENT_ROLE)){
								map.put("requestUrl", map.get("requestUrl").replace("{0}", roleFullName.replace("/", "-")));
							}else if(key.equals(PIMS_SYS_MANAGEMENT_ATS)){
//								map.put("requestUrl", map.get("requestUrl"));
							}else if(key.equals(PIMS_SYS_MANAGEMENT_QMS)){
//								map.put("requestUrl", map.get("requestUrl"));
							}else if(key.equals(PIMS_SYS_MANAGEMENT_AAS)){
//								map.put("requestUrl", map.get("requestUrl"));
							}
							map.put("label", InternationalResource.get(key));
						}
					}
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return list;
	}

}
