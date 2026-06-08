package com.supcon.supfusion.signature.services.service;

import com.supcon.supfusion.signature.dao.entity.*;
import com.supcon.supfusion.signature.dao.enums.ViewType;
import java.util.List;

/**
 * <pre>
 * config ==> CUSTOME:{action:"",html:""}
 * </pre>
 * 
 * @author jiawei
 * 
 */
public interface ViewService {


	/**
	 * 获取编辑视图列表.
	 * 
	 * @param entity
	 *            实体
	 * @param viewTypes
	 *            视图类型
	 * @return 编辑视图列表
	 */
	List<View> findViews(Entity entity, ViewType... viewTypes);

	/**
	 *
	 * @param entityCode 视图code
	 * @param viewTypes 视图类型
	 * @return
	 */
	List<View> findViews(String entityCode, ViewType... viewTypes);
	

   
}
