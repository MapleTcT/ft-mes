package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.*;

import java.util.List;

public interface SqlModelService {

	SqlModel getSqlModel(String modelCode);

	void deleteDBView(String viewName);

	List<Property> getProperties(Model model);

	void addSqlModel(Model model);

	void checkSqlModel(Model model);

	void deleteSqlModel(String modelCode);
	/**
	 * 获取SQL模型删除字段,校验删除字段是否引用
	 * @param model
	 */
//	Set<Property> getDelSqlProperties(Model model);

}
