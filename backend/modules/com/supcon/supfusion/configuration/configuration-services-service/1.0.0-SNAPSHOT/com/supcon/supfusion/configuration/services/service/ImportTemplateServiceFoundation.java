package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.ImportTemplate;

/**
 * 供runtime使用的导入导出模板Service接口
 * @author zhengjiefeng
 *
 */
public interface ImportTemplateServiceFoundation {

	ImportTemplate getImportTemplateByCode(String code);

	void saveImportTemplate(ImportTemplate importTemplate);

}
