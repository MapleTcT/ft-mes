package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.ImportTemplate;
import org.dom4j.DocumentException;

import java.util.List;
import java.util.Map;

/**
 * ec,proj使用的导入导出模板service接口
 * @author zhengjiefeng
 *
 */
public interface ImportTemplateService {
	void saveImportTemplate(ImportTemplate importTemplate);
	
	void deleteImportTemplate(ImportTemplate importTemplate);

	ImportTemplate getImportTemplateByCode(String code);
	
	ImportTemplate getImportTemplateByHql(String hql, String param);

	List<Map<String, String>> getRequireData(String modelCode, Boolean getAllProperties, Boolean showCustom);

	/**
	 * 获取已启用的自定义字段对象
	 * @param entityCode
	 * @return
	 */
	List<String> getRunningCustomProperties(String entityCode);

	void importXml(String xml) throws DocumentException;
	/**
	 * 根据模型编码获取导入导出模板列表
	 * @param moduleCode
	 * @return
	 */
	List<ImportTemplate> getImportTemplateListByModuleCode(String moduleCode);

	void saveImportTemplateList(List<ImportTemplate> list) throws DocumentException;

}
