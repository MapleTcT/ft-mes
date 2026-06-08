/*
package com.supcon.supfusion.configuration.services.projectapi.services.impl;

import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.configuration.services.entity.EcEnv;
import com.supcon.supfusion.configuration.services.entity.ImportTemplate;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.projectapi.services.ProjBuildTplService;
import com.supcon.supfusion.configuration.services.projectapi.services.ProjImportExportService;
import com.supcon.supfusion.configuration.services.service.ImportTemplateService;
import com.supcon.supfusion.configuration.services.utils.XmlUtils;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;

*/
/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/2/23
 *//*

@Slf4j
@Transactional
public class ProjImportTemplateServiceImpl implements ProjImportExportService {
    private static final String FILE_NAME = "ImportTemplate.xml";
    private static final String DIR_NAME = "ImportTemplate";

    @Autowired
    private ImportTemplateService importTemplateService;
    @Autowired
    private ImportTemplateService ImportTemplateServiceFoundation;
    @Autowired
    private ProjBuildTplService projBuildTplService;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, timeout = -1)
    public void importProjConfig(String moduleCode, String path) {
        File dir=new File(path+File.separator+DIR_NAME);
        if(!dir.exists()){
            return;
        }

        ProjectFlagHolder.getInstance().getProjFlag().set(true);

        File f=new File(path+File.separator+DIR_NAME+File.separator+moduleCode+File.separator+FILE_NAME);
        List<ImportTemplate> importTemplateList=new ArrayList<ImportTemplate>();
        if(f.exists()){
            try {
                String xml = FileUtils.readFileToString(f, "UTF-8");
                if (!StringUtils.isBlank(xml)) {
                    String code = XmlUtils.getTagContent(xml, "code");
                    if (!code.isEmpty()) {
                        try {
                            Document document= DocumentHelper.parseText(xml);
                            Element root=document.getRootElement();
                            Iterator<Element> iterator=root.elementIterator();
                            while(iterator.hasNext()){
                                ImportTemplate importTemplate=new ImportTemplate();
                                Element element=(Element) iterator.next();

                                importTemplate.setCode(element.elementText("code"));
                                importTemplate.setEcEnv(EcEnv.product);
                                importTemplate.setProjFlag(true);

                                String value=null;
                                Element valueElement=element.element("value");
                                if(valueElement!=null){
                                    Element listElement=valueElement.element("list");
                                    if(listElement!=null){
                                        value="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+listElement.asXML();
                                    }
                                }
                                importTemplate.setValue(value);
                                importTemplateList.add(importTemplate);
                            }
                        } catch (DocumentException e) {
                            log.error(e.getMessage(),e);
                            throw new DocumentException();
                        }

                        if(importTemplateList.size()>0){
                            importTemplateService.saveImportTemplateList(importTemplateList);
                            ImportTemplateServiceFoundation.saveImportTemplateList(importTemplateList);
                        }

                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new EcException(EcException.Code.IMPORT_TEMPLATE_FILE_DEAL_ERROR);
            }
        }

        ProjectFlagHolder.getInstance().getProjFlag().set(false);
    }

    @Override
    public void exportProjConfig(String moduleCode, String path) {
        File dir=new File(path+File.separator+DIR_NAME);
        if(!dir.exists()){
            if (!dir.mkdirs()) {
                throw new EcException("can not make ProjConfig's dir.");
            }
        }

        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        List<ImportTemplate> importTemplateList=importTemplateService.getImportTemplateListByModuleCode(moduleCode);
        Map<String,Object> importTemplateMap=new HashMap<String, Object>();
        importTemplateMap.put("importTemplateList", importTemplateList);
        projBuildTplService.buildTpl("proj_importTemplate.ftl", path+File.separator+DIR_NAME+File.separator+moduleCode, FILE_NAME,importTemplateMap);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
    }

}
*/
