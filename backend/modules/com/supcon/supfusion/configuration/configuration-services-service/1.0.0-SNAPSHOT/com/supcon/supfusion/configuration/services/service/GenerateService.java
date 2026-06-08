package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.*;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author songjiawei
 *
 */
public interface GenerateService {

	void executeMigrateUpload(Module module, String[] entites, Boolean uploadMetaData, Boolean uploadCustomCode, Boolean uploadWorkFlow, boolean filter, String filePath, boolean uploadFile) throws IOException, XMLStreamException, Exception;

	void executeMigrateUpload(String[] entites, boolean filter, String filePath, boolean uploadFile) throws Exception;

	File compress(Module module, Boolean excludeCustomFile) throws Exception;

	File compressMul(String moduleCodes, Boolean excludeCustomFile, String isMis) throws IOException;

	void addAppBusinfo(UploadInfo up);// 暂用

	void generateProjDataGridConfig(View view, List<Field> fields, List<Button> buttons, List<Event> events);
	
	void deleteProjViewHtml(View view);
	
	void deleteProjViewHtml(String viewName, String modelName, String entityName, String artifact);

}
