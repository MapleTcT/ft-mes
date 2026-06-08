package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.entity.FastQueryJson;
import com.supcon.supfusion.configuration.services.entity.Field;
import com.supcon.supfusion.configuration.services.dao.FastQueryJsonDaoImpl;
import com.supcon.supfusion.configuration.services.service.EventService;
import com.supcon.supfusion.configuration.services.service.FastQueryJsonService;
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
@ServiceApiService("ec_FastQueryJsonService")
@Transactional
public class FastQueryJsonServiceImpl implements FastQueryJsonService {
	@Autowired
	private FastQueryJsonDaoImpl fastQueryJsonDao;
	@Autowired
	private FieldService fieldService;
	@Autowired
	private EventService eventService;
	/**
	 * 根据条件查询FastQueryJson
	 * 
	 * @param criterions
	 * @return
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<FastQueryJson> findFastQueryJsons(Criterion... criterions) {
		return fastQueryJsonDao.findByCriteria(criterions);
	}
	
	/**
	 * 新布局里面快速查询保存字段
	 * 
	 * @param fastQueryJson
	 * @param fieldConfig
	 */
	@Override
	@Transactional
	public void saveFields(FastQueryJson fastQueryJson, String fieldConfig){
		fieldService.saveFields(fastQueryJson, fieldConfig, null, null, null);
		eventService.saveEvent(fastQueryJson, fieldConfig);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FastQueryJson getFastQueryJson(String code) {
//		fastQueryJsonDao.flush();
//		fastQueryJsonDao.clear();
		return fastQueryJsonDao.load(code);
	}
	
	@Override
	@Transactional
	public void deletePhysical(FastQueryJson fqj) {
		deleteField(fqj,true);
		fastQueryJsonDao.deletePhysical(fqj);
	}
	
	@Override
	@Transactional
	public void deleteField(FastQueryJson fqj, boolean flag) {
		List<Field> fqjFields = fieldService.getFieldByFastQueryJsonCode(fqj.getCode());
		if(flag){
			for(Field field : fqjFields){
				fieldService.deleteField(field);
			}
		}else{
			String fqjConfig = fqj.getQueryConfig();
			if(fqjConfig != null && fqjConfig.length() > 0){
				for(Field field : fqjFields){
					if(field.getCellCode() != null && !"".equals(field.getCellCode()) && !fqjConfig.contains("<cellCode><![CDATA["+field.getCellCode()+"]]></cellCode>")){
						fieldService.deleteField(field);
					}
				}
			}
		}
	}
}