package com.supcon.supfusion.configuration.services.projectapi.services.impl;

import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.base.utils.RuntimeFlagHolder;
import com.supcon.supfusion.configuration.services.entity.Entity;
import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.projectapi.services.ProjBuildTplService;
import com.supcon.supfusion.configuration.services.projectapi.services.ProjImportExportService;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.configuration.services.service.ViewService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/2/25
 */
@Slf4j
@Transactional
@ServiceApiService
public class ProjModuleInfoImExServiceImpl implements ProjImportExportService {
    private static final String FILE_NAME = "Module.xml";
    @Autowired
    private ProjBuildTplService projBuildTplService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private ViewService viewServiceFoundation;

    @Override
    @Transactional
    public void importProjConfig(String moduleCode, String path) {
        File f = new File(path+File.separator+FILE_NAME);
        Document doc;
        String projectVersion = null;
        // 创建解析工厂
        SAXParserFactory factory = SAXParserFactory.newInstance();
        // 创建解析器
        try {
            String xml = FileUtils.readFileToString(f);
            doc = DocumentHelper.parseText(xml);
            Element moduleE = doc.getRootElement();
            for (Iterator subEntityIT = moduleE.elementIterator(); subEntityIT.hasNext();) {
                Element subEntityE = (Element) subEntityIT.next();
                String subEntityName = subEntityE.getName();
                if ("projectVersion".equals(subEntityName)) {
                    projectVersion = subEntityE.getStringValue();
                }
            }
            SAXParser parser = factory.newSAXParser();
            // 得到读取器
            XMLReader reader = parser.getXMLReader();
            // 设置内容处理器
            BeanListHandler handler = new BeanListHandler();
            reader.setContentHandler(handler);
            // 读取xml文档
            reader.parse(path+File.separator+FILE_NAME);
//                String projectVersion = moduleHandler.item.get("projectVersion");
                if(null !=projectVersion && !projectVersion.isEmpty()){
                    Module module = modelService.getModule(moduleCode);
                    if(!module.getProjectVersion().equals(projectVersion)){
//                        throw new EcException(InternationalResource.get("ec.project.upload.versionerror",new Object[]{module.getCode(),module.getProjectVersion(),projectVersion}));
                        throw new EcException(InternationalResource.get("ec.project.upload.versionerror1")+module.getCode()+InternationalResource.get("ec.project.upload.versionerror2")+module.getProjectVersion()+InternationalResource.get("ec.project.upload.versionerror3")+projectVersion);
                    }
                }
            for(Map<String,String> map:handler.getResult()){
                RuntimeFlagHolder.getInstance().getRuntimeFlag().set(true);
                Entity entity= modelService.getEntity(map.get("code"));
                RuntimeFlagHolder.getInstance().getRuntimeFlag().set(false);
                entity.setPrefix(map.get("prefix"));
                if(map.get("crossCompanyFlag") !=null){
                    entity.setCrossCompanyFlag(Boolean.valueOf(map.get("crossCompanyFlag")));
                }
                entity.setProjFlag(true);
                viewServiceFoundation.saveEntity(entity);
            }
        } catch (ParserConfigurationException e) {
            throw new EcException(e);
        } catch (SAXException e) {
            throw new EcException(e);
        } catch (IOException | DocumentException e) {
            throw new EcException(e);
        }
    }

    @Override
    @Transactional
    public void exportProjConfig(String moduleCode, String path) {
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        Module module=modelService.getModule(moduleCode);
        module.setEntities(new HashSet(modelService.getEntities(moduleCode)));
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        Map<String,Object> map=new HashMap<String,Object>();
        map.put("module", module);
        projBuildTplService.buildTpl("proj_module.ftl", path, FILE_NAME,map);
    }

    class BeanListHandler extends DefaultHandler {
        private List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        private Map<String, String> item;
        private boolean entityFlag = false;
        private String nodeName;

        public List<Map<String, String>> getResult() {
            return result;
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {
            if (entityFlag) {
                nodeName = qName;
            }
            if (qName.equals("entity")) {
                item = new HashMap<String, String>();
                entityFlag = true;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            if (qName.equals("entity")) {
                result.add(item);
                entityFlag = false;
            }
            nodeName = null;
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            if (entityFlag && item != null && nodeName != null) {
                item.put(nodeName, new String(ch, start, length));
            }
        }
    }
}