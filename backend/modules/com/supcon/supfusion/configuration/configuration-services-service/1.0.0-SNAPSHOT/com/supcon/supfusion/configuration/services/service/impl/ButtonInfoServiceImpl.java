/**
 * 
 */
package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.entity.ButtonInfo;
import com.supcon.supfusion.configuration.services.dao.ButtonInfoDaoImpl;
import com.supcon.supfusion.configuration.services.service.ButtonInfoService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author rockey
 * 
 */
@ServiceApiService("buttonInfoService")
@Transactional
public class ButtonInfoServiceImpl implements ButtonInfoService {

	@Autowired
	private ButtonInfoDaoImpl buttonInfoDao;

	public ButtonInfo load(String code) {
		
		return buttonInfoDao.load(code);
	}

	public void save(ButtonInfo entity) {
		buttonInfoDao.save(entity);
	}

}
