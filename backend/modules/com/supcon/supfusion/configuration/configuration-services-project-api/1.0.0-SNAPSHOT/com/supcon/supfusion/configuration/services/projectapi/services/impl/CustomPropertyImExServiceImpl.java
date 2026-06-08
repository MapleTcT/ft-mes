package com.supcon.supfusion.configuration.services.projectapi.services.impl;

import com.supcon.supfusion.base.utils.RuntimeFlagHolder;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.FieldType;
import com.supcon.supfusion.configuration.services.enums.ShowFormat;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.projectapi.services.ProjBuildTplService;
import com.supcon.supfusion.configuration.services.projectapi.services.ProjImportExportService;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.configuration.services.service.ViewService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/3/2
 */
@Slf4j
@Transactional
@ServiceApiService
public class CustomPropertyImExServiceImpl implements ProjImportExportService {
    private static final String FILE_NAME = "CustomProperty.xml";
    private static final String DIR_NAME = "CustomProperty";
    @Autowired
    private ProjBuildTplService projBuildTplService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ModelService modelServiceFoundation;
    @Autowired
    private ViewService viewServiceFoundation;
    @Override
    public void importProjConfig(String moduleCode, String path) {
        RuntimeFlagHolder.getInstance().getRuntimeFlag().set(true);
        File dir=new File(path+File.separator+DIR_NAME);
        if(!dir.exists()){
            return;
        }
        // 创建解析工厂
        SAXParserFactory factory = SAXParserFactory.newInstance();
        // 创建解析器
        try {
            SAXParser parser = factory.newSAXParser();
            // 得到读取器
            XMLReader reader = parser.getXMLReader();
            // 设置内容处理器
            BeanListHandler handler = new BeanListHandler();
            reader.setContentHandler(handler);
            // 读取xml文档
            reader.parse(path+File.separator+DIR_NAME+File.separator+FILE_NAME);
            if(handler.getCpmList().size()>0){
                modelServiceFoundation.deleteCustomPropertyModelMappingsForImport(moduleCode);
            }
            if(handler.getCpvList().size()>0){
                viewServiceFoundation.deleteCustomPropertyViewMappingsForImport(moduleCode);
            }
            for(Map<String,String> map:handler.getCpmList()){
                CustomPropertyModelMapping item=new CustomPropertyModelMapping();
                item.setDisplayName(map.get("displayName"));
                item.setFieldType(FieldType.valueOf(map.get("fieldType")));
                item.setFormat(ShowFormat.valueOf(map.get("format")));
                item.setFillContent(map.get("fillContent"));
                item.setMultable("true".equals(map.get("multable")));
                item.setSeniorSystemCode("true".equals(map.get("seniorSystemCode")));
                item.setAssociatedProperty(modelServiceFoundation.getProperty(map.get("associatedProperty_code")));
                if(map.get("associatedType")!=null){
                    item.setAssociatedType(Integer.valueOf(map.get("associatedType")));
                }
                if(null != map.get("refView_code")){
                    item.setRefView(viewServiceFoundation.getView(map.get("refView_code")));
                }
                item.setNullable("true".equals(map.get("nullable")));
                item.setEnableCustom("true".equals(map.get("enableCustom")));
                item.setProperty(modelServiceFoundation.getProperty(map.get("property_code")));
                item.setModel(modelServiceFoundation.getModel(map.get("model_code")));
                item.setDescription(map.get("description"));
                item.setVersion(0);
                if(item.getProperty()==null){
                    continue;
                }
                if(map.get("sort")!=null){
                    item.setSort(Integer.valueOf(map.get("sort")));
                }
                item.setRelatedKey(map.get("relatedKey"));
                if(map.get("precision")!=null){
                    item.setPrecision(Integer.valueOf(map.get("precision")));
                }
                modelServiceFoundation.saveCustomPropertyModelMapping(item);
            }
            for(Map<String,String> map:handler.getCpvList()){
                CustomPropertyViewMapping item=new CustomPropertyViewMapping();
                item.setDisplayName(map.get("displayName"));
                item.setFieldType(FieldType.valueOf(map.get("fieldType")));
                item.setFormat(ShowFormat.valueOf(map.get("format")));
                item.setNullable("true".equals(map.get("nullable")));
                item.setShowCustom("true".equals(map.get("showCustom")));
                item.setVersion(0);
                if(map.get("colspan")!=null){
                    item.setColspan(Integer.valueOf(map.get("colspan")));
                }
                if(map.get("textareaRow")!=null){
                    item.setTextareaRow(Integer.valueOf(map.get("textareaRow")));
                }
                if(map.get("sort")!=null){
                    item.setSort(Integer.valueOf(map.get("sort")));
                }
                item.setProperty(modelServiceFoundation.getProperty(map.get("property_code")));
                if(item.getProperty()==null){
                    continue;
                }
                item.setPropertyLayRec(map.get("propertyLayRec"));
                item.setAssociatedCode(map.get("associatedCode"));
                item.setCustomStyle(map.get("customStyle"));
                item.setCustomScript(map.get("customScript"));
                item.setMultable("true".equals(map.get("multable")));
                if(map.get("sort")!=null){
                    item.setSort(Integer.valueOf(map.get("sort")));
                }
                item.setReadonly("true".equals(map.get("readonly")));
                item.setAlign(map.get("align"));
                if(map.get("precision")!=null){
                    item.setPrecision(Integer.valueOf(map.get("precision")));
                }
                if(map.get("length")!=null){
                    item.setLength(Integer.valueOf(map.get("length")));
                }
                viewServiceFoundation.saveCustomPropertyViewMapping(item);
            }
        } catch (ParserConfigurationException | SAXException | EcException | IOException e) {
            RuntimeFlagHolder.getInstance().getRuntimeFlag().set(false);
            log.error(e.getMessage());
        }
        RuntimeFlagHolder.getInstance().getRuntimeFlag().set(false);
    }

    @Override
    public void exportProjConfig(String moduleCode, String path) {
        RuntimeFlagHolder.getInstance().getRuntimeFlag().set(true);
        try {
        List<CustomPropertyModelMapping> cpmList=null;
        List<CustomPropertyViewMapping> cpvList=null;
        List<Entity> entities=modelServiceFoundation.getEntities(moduleCode);
        for(Entity entity:entities){
            List<Model> models = modelServiceFoundation.findModels(entity);
            for(Model model:models){
                if(cpmList==null){
                    cpmList= modelServiceFoundation.findCustomPropertyModelMappingsForExport(model.getCode());
                }else{
                    cpmList.addAll(modelServiceFoundation.findCustomPropertyModelMappingsForExport(model.getCode()));
                }
            }
            List<View> views = viewServiceFoundation.findViewList(entity);
            for(View view:views){
                if(cpvList==null){
                    cpvList= viewServiceFoundation.findCustomPropertyViewMappingsForExport(view.getCode());
                }else{
                    cpvList.addAll(viewServiceFoundation.findCustomPropertyViewMappingsForExport(view.getCode()));
                }
            }
        }
        Map<String,Object> map=new HashMap<String,Object>();
        map.put("cpmList", cpmList);
        map.put("cpvList", cpvList);
        projBuildTplService.buildTpl("proj_customProperty.ftl", path+File.separator+DIR_NAME, FILE_NAME,map);
        RuntimeFlagHolder.getInstance().getRuntimeFlag().set(false);
        }catch (Exception e){
            throw new EcException(moduleCode+ InternationalResource.get("ec.project.upload.error1")+e.getMessage());
        }finally{
            /*if(statelessSession!=null){
                statelessSession.close();
            }*/
            RuntimeFlagHolder.getInstance().getRuntimeFlag().set(false);
        }
    }

    class BeanListHandler extends DefaultHandler {
        private List<Map<String,String>> cpmList=new ArrayList<Map<String,String>>();
        private List<Map<String,String>> cpvList=new ArrayList<Map<String,String>>();
        private Map<String,String> item;
        private String nodeName;
        private boolean flag=false;
        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {
            if(flag){
                nodeName=qName;
            }
            if(qName.equals("CustomPropertyModelMapping")||qName.equals("CustomPropertyViewMapping")){
                item=new HashMap<String,String>();
                flag=true;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            nodeName=null;
            if(qName.equals("CustomPropertyModelMapping")){
                cpmList.add(item);
            }else if(qName.equals("CustomPropertyViewMapping")){
                cpvList.add(item);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            if(nodeName!=null&&item!=null){
                item.put(nodeName, new String(ch,start,length));
            }
        }

        public List<Map<String, String>> getCpmList() {
            return cpmList;
        }

        public List<Map<String, String>> getCpvList() {
            return cpvList;
        }

    }
}
