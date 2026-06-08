/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.configuration.services.entity.View;


/**
 * 
 * 
 * @author zhuwei
 * @version $Id$
 */
public interface ActionViewService {

	/**
	 * 刷新模块下的action到ActionView中
	 * @param moduleCode
	 * @param env
	 */
	void refreshModuleActionView(Module module, String... env);

	void refreshSingleViewAction(View view, String... env);

	void deleteActionViewByViewcode(String viewCode);

}
