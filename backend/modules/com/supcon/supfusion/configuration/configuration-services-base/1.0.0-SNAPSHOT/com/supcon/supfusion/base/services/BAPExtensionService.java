/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.base.services;

import com.supcon.supfusion.base.entities.BAPExtension;
import org.hibernate.criterion.Criterion;

import java.util.List;

/**
 * 
 * 扩展点接口
 * @see
 * @author zhuyuyin
 * @version 1.0
 */
public interface BAPExtensionService {

	void save(BAPExtension entity);
	/**
	 * 根据code获取扩展点
	 * 
	 * @param code
	 * @return
	 */
	BAPExtension getExtension(String code);


}
