package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.entity.AdvQueryJson;
import com.supcon.supfusion.configuration.services.entity.Field;
import com.supcon.supfusion.configuration.services.dao.AdvQueryJsonDaoImpl;
import com.supcon.supfusion.configuration.services.service.AdvQueryJsonService;
import com.supcon.supfusion.configuration.services.service.EventService;
import com.supcon.supfusion.configuration.services.service.FieldService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import org.hibernate.criterion.Criterion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 
 * @author fukun
 * 
 */
@ServiceApiService
@Transactional
public class AdvQueryJsonServiceImpl implements AdvQueryJsonService {
	@Autowired
	private AdvQueryJsonDaoImpl advQueryJsonDao;
	@Autowired
	private FieldService fieldService;
	@Autowired
	private EventService eventService;

	/**
	 * 根据条件查询AdvQueryJson
	 * 
	 * @param criterions
	 * @return
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<AdvQueryJson> findAdvQueryJsons(Criterion... criterions) {
		return advQueryJsonDao.findByCriteria(criterions);
	}
	
	/**
	 * 新布局里面高级查询保存字段
	 * 
	 * @param advQueryJson
	 * @param fieldConfig
	 */
	@Override
	@Transactional
	public void saveFields(AdvQueryJson advQueryJson, String fieldConfig){
		fieldService.saveFields(advQueryJson, fieldConfig, null, null, null);
		eventService.saveEvent(advQueryJson, fieldConfig);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public AdvQueryJson getAdvQueryJson(String code) {
		//advQueryJsonDao.flush();
		//advQueryJsonDao.clear();
		return advQueryJsonDao.load(code);
	}
	
	@Override
	@Transactional
	public void deletePhysical(AdvQueryJson aqj){
		deleteField(aqj,true);
		advQueryJsonDao.deletePhysical(aqj);
	}
	
	@Override
	@Transactional
	public void deleteField(AdvQueryJson aqj, boolean flag){
		List<Field> aqjFields = fieldService.getFieldByAdvQueryJsonCode(aqj.getCode());
		if(flag){
			for(Field field : aqjFields){
				fieldService.deleteField(field);
			}
		}else{
			String aqjConfig = aqj.getQueryConfig();
			if(aqjConfig != null && aqjConfig.length() > 0){
				for(Field field : aqjFields){
					if(field.getCellCode() != null && !field.getCellCode().equals("") && !aqjConfig.contains("<cellCode><![CDATA["+field.getCellCode()+"]]></cellCode>")){
						fieldService.deleteField(field);
					}
				}
			}
		}
	}
}