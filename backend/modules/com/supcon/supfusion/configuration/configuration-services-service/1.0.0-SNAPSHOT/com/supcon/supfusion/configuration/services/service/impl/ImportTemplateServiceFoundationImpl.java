package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.entity.EcEnv;
import com.supcon.supfusion.base.services.BaseServiceImpl;
import com.supcon.supfusion.configuration.services.dao.ImportTemplateDaoImpl;
import com.supcon.supfusion.configuration.services.entity.Event;
import com.supcon.supfusion.configuration.services.entity.ImportTemplate;
import com.supcon.supfusion.configuration.services.service.ImportTemplateServiceFoundation;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 
 * @author zhengjiefeng
 *
 */
@Slf4j
@Service("ec_ImportTemplateServiceFoundation")
@Transactional
public class ImportTemplateServiceFoundationImpl extends BaseServiceImpl<ImportTemplate> implements ImportTemplateServiceFoundation,InitializingBean {
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private ImportTemplateDaoImpl importTemplateDao;
	
	private ApplicationContext springContext;
	/*@Resource
	private IdGenerator idGenerator;*/
	
	@Override
	public void saveImportTemplate(ImportTemplate importTemplate) {
		importTemplateDao.save(importTemplate);
	}
	
	@Override
	public ImportTemplate getImportTemplateByCode(String code) {
		ImportTemplate importTemplate = importTemplateDao.findEntityByHql("from ImportTemplate where  code=?0",code);
		return importTemplate;
	}
	
	@Transactional(timeout = -1)
	public synchronized void saveImportTemplateList(List<ImportTemplate> list) throws DocumentException {
		for(ImportTemplate it:list){
			ImportTemplate importTemplate=importTemplateDao.get(it.getCode());
			if(importTemplate==null){
				importTemplate=new ImportTemplate();
			}
			
			importTemplate.setCode(it.getCode());
			importTemplate.setEcEnv(EcEnv.product);
			importTemplate.setProjFlag(it.getProjFlag());
			importTemplate.setValue(it.getValue());
			importTemplateDao.save(importTemplate);
		}
	}
	
	
	public List<String> getRunningCustomProperties(String entityCode){
		List<String> list = null;
		String sql = "select property_code from BASE_CP_MODEL_MAPPING where model_code = ? and enable_custom = 1";
		list = importTemplateDao.createNativeQuery(sql, entityCode).list();
		return list;
	}
	

	@Override
	public void afterPropertiesSet() throws Exception {
	}


	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.springContext=applicationContext;
	}

	public void handleEvent(Event event) {
		
	}


}
