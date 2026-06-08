package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.entities.MenuInfo;
import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.base.services.MenuInfoService;
import com.supcon.supfusion.base.services.MenuOperateService;
import com.supcon.supfusion.configuration.services.dao.DataGroupDaoImpl;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.ShowType;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.service.GenerateService;
import com.supcon.supfusion.configuration.services.service.ModuleService;
import com.supcon.supfusion.configuration.services.service.SystemCodeService;
import com.supcon.supfusion.configuration.services.service.ViewService;
import com.supcon.supfusion.configuration.services.utils.PropertyHolder;
import com.supcon.supfusion.configuration.services.utils.SerializeUitls;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/9/8
 */
@Slf4j
@ServiceApiService
public class GenerateServiceImpl implements GenerateService {

    @Autowired
    private ModuleService moduleService;
    @Autowired
    private ViewService viewService;
    @Autowired
    private DataGroupDaoImpl dataGroupDao;
    @Autowired
    private SystemCodeService systemCodeService;
    @Autowired
    private MenuInfoService menuInfoService;
    @Autowired
    private MenuOperateService menuOperateService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    //@Value("${entityconf.generatePath}")
    //private String tmpPath;
    @Override
    public void executeMigrateUpload(Module module, String[] entites, Boolean uploadMetaData, Boolean uploadCustomCode, Boolean uploadWorkFlow, boolean filter, String filePath, boolean uploadFile) throws IOException, XMLStreamException, Exception {

    }

    @Override
    public void executeMigrateUpload(String[] entites, boolean filter, String filePath, boolean uploadFile) throws Exception {

    }

    @Override
    public File compress(Module module, Boolean excludeCustomFile) throws Exception {
        File f = new File(PropertyHolder.get().getGeneratePath() + File.separator + module.getCode().split("_")[0]+"_"+module.getProjectVersion()+ ".zip");
        if (f.exists()) {
            f.delete();// delete if exist.
        }
        String path = PropertyHolder.get().getGeneratePath() + File.separator + module.getCode();
        String targetScriptPath = path + File.separator + "scripts"; // 生成源码中的scripts目录
        Zip zip = new Zip();
        Project p = new Project();
        zip.setProject(p);
        zip.setDestFile(f);
        FileSet fileSet = new FileSet();
        fileSet.setProject(p);
        File file = new File(path);
        File scriptFile = new File(PropertyHolder.get().getScriptsPath());
        boolean isNull = false;
        if (!scriptFile.exists()) {
            isNull = true;
            scriptFile.mkdirs();
        }
        File targetScriptFile = new File(targetScriptPath);
        if (!targetScriptFile.exists()) {
            targetScriptFile.mkdir(); // scripts目录不存在则创建
        } else {
            if (targetScriptFile.isDirectory()) {
                File[] files = targetScriptFile.listFiles();
                if (null != files) {
                    for (File f1 : files) {
                        f1.delete(); // scripts目录存在则删除其子目录和文件
                    }
                }
            }
        }

        if (!isNull) {
            if (scriptFile.isDirectory()) {
                File[] files2 = scriptFile.listFiles();
                if(files2 != null){
                    for (File f2 : files2) {
                        if (f2.isDirectory() && f2.getName().startsWith(module.getCode())) {
                            FileUtils.copyDirectoryToDirectory(f2, targetScriptFile);
                        }
                    }
                }
            }
        }

        // 微服务下载，将第三方jar包放入
        // 生成源码中的maven目录，即第三方jar包存放地方
        String targetMavenPath = path + File.separator + "maven";
        File targetMavenFile = new File(targetMavenPath);
        if (targetMavenFile.exists()) {
            if (targetMavenFile.isDirectory()) {
                File[] files = targetMavenFile.listFiles();
                if (null != files) {
                    for (File f1 : files) {
                        f1.delete(); // maven目录存在则删除其子目录和文件
                    }
                }
            }
        }
        String mavenPath = PropertyHolder.get().getWorkspacePath() + File.separator + "maven" + File.separator + module.getCode();
        File mavenFile = new File(mavenPath);
        if (mavenFile.exists()) {
            if (!targetMavenFile.exists()) {
                targetMavenFile.mkdir(); // maven目录不存在则创建
            }
            FileUtils.copyDirectory(mavenFile, targetMavenFile);
        }
        fileSet.setDir(file);
        if(PropertyHolder.isProduct()){
            excludeCustomFile = false;
        }
        if(null != excludeCustomFile && excludeCustomFile){
            //zip.setExcludes(module.getCode() + "/service/src/main/resources/" + OrchidConstants.L10N_CUSTOM_PATH + "/**");
            fileSet.setExcludes("**/" + "OSGI-INF/custom" + "/**");
        }
        //压缩文件中去除target的目录
        fileSet.setExcludes("**/core/target/**");
        fileSet.setExcludes("**/service/target/**");
        // 微服务模块删除api中的target
        fileSet.setExcludes("**/api/target/**");
        zip.add(fileSet);
        zip.execute();
        FileUtils.deleteDirectory(targetScriptFile);
        FileUtils.deleteDirectory(targetMavenFile);
        return zip.getDestFile();
    }

    @Override
    public File compressMul(String moduleCodes, Boolean excludeCustomFile, String isMis) throws IOException {
        String batchFileName = "supPlant";
        File f = new File(PropertyHolder.get().getGeneratePath() + File.separator + batchFileName +".zip");
        File generateFile = new File(PropertyHolder.get().getGeneratePath());
        if (f.exists()) {
            f.delete();// delete if exist.
        }
        String vxmPath = PropertyHolder.get().getGeneratePath() + File.separator + batchFileName;
        File vxmFile = new File(vxmPath);
        if (!vxmFile.exists()) {
            vxmFile.mkdir(); // scripts目录不存在则创建
        }else{
            for(File file : vxmFile.listFiles()){
                file.delete();
            }
        }
        if(null != moduleCodes && !"".equals(moduleCodes)){
            for(String moduleCode : moduleCodes.split(",")){
                Module module = moduleService.getModule(moduleCode);
                if(null != module){
                    File moduleZip = new File(PropertyHolder.get().getGeneratePath()+ File.separator + module.getCode().split("_")[0]+"_"+module.getProjectVersion()+ ".zip");
                    if(moduleZip.exists()){
                        FileUtils.moveFileToDirectory(moduleZip, vxmFile, false);
                    }else{
                        log.info("无法找到编码为{}的压缩文件"+moduleCode);
                        break;
                    }
                }else{
                    log.info("无法找到编码为{}的模块",moduleCode);
                }
            }
        }
        Zip zip = new Zip();
        Project p = new Project();
        zip.setProject(p);
        zip.setDestFile(f);
        FileSet fileSet = new FileSet();
        fileSet.setProject(p);
        fileSet.setDir(vxmFile);
        zip.add(fileSet);
        zip.execute();
        return zip.getDestFile();
    }

    @Override
    public void addAppBusinfo(UploadInfo up){
        String currentModuleCode = up.getModuleCode();
        String baseModuleCode = "base_1.0.0";
        // 如果当前上载的模块是移动应用模块 或者 移动应用模块未上传，则不处理
        Number count = (Number) dataGroupDao.createNativeQuery("SELECT COUNT(1) FROM EC_MODULE WHERE CODE = ?", baseModuleCode).uniqueResult();
        if (baseModuleCode.equals(currentModuleCode) || count.longValue() == 0) {
            return;
        }
        //String base = tmpPath + File.separator + "unziped";
        String base =PropertyHolder.get().getGeneratePath() + File.separator+ "unziped";
        // 处理app.xml文件
        File appFile = new File(base + File.separator + currentModuleCode + File.separator + "service/src/main/resources/META-INF/bap/"
                + "bap-app.xml");
        if(!appFile.exists()){
            appFile = new File(base + File.separator + currentModuleCode + File.separator + "service/src/generated/resources/META-INF/bap/"
                    + "bap-app.xml");
            if(!appFile.exists()){
                appFile = new File(base + File.separator + currentModuleCode + File.separator + "src/main/resources/META-INF/bap/"
                        + "bap-app.xml");
            }
        }
        if (!appFile.exists()) {
            return;
        }
        try {
            // 从文件中获取xml
            String xml = FileUtils.readFileToString(appFile,"UTF-8");
            if (StringUtils.isBlank(xml)) {
                return;
            }
            // 解析xml
            Document document = DocumentHelper.parseText(xml);
            // 获取根节点
            Element root= document.getRootElement();
            // 获取子节点
            Iterator iterator = root.elementIterator();
            Date now = new Date();
            Long staffId = 1000L;
            Long positionId = 1000L;
            Long departmentId = 1000L;
            while(iterator.hasNext()){
                Element element = (Element) iterator.next();
                // app编码
                String code = element.elementText("code");
                // 如果没有传app编码，则跳过
                if (StringUtils.isBlank(code)) {
                    continue;
                }
                // 判断app编码是否存在
                List<Number> appList = dataGroupDao.createNativeQuery("select ID from BASE_APP_BUSINFO where code = ? and valid = 1", code).list();
                if (!appList.isEmpty()) {
                    // 如果app编码存在，则跳过
                    continue;
                }
                // app名称
                String name = element.elementText("name");
                // 类型
                String appType = element.elementText("appType");
                if (StringUtils.isBlank(appType) || systemCodeService.load(appType) == null) {
                    appType = null;
                }
                // 所属模块
                String moduleCode = element.elementText("moduleCode");
                String moduleCodingDetails = null;
                if (StringUtils.isBlank(moduleCode)) {
                    moduleCode = null;
                } else {
                    Module appModule = moduleService.getModule(moduleCode);
                    if (appModule == null) {
                        moduleCode = null;
                    } else {
                        moduleCode = appModule.getName();
                        moduleCodingDetails = appModule.getCode();
                    }
                }
                // 请求url
                String url = element.elementText("url");
                // 图标
                String appIcon = element.elementText("appIcon");
                // 是否启用权限
                String powerFlagStr = element.elementText("powerFlag");
                boolean powerFlag;
                if (StringUtils.isBlank(powerFlagStr) || (!"true".equals(powerFlagStr) && !"false".equals(powerFlagStr))) {
                    powerFlag = false;
                } else {
                    powerFlag = Boolean.parseBoolean(powerFlagStr);
                }
                // 关联菜单编码
                MenuInfo menuInfo = null;
                String menuCode = element.elementText("menuCode");
                String menuCodingDetails = null;
                if (StringUtils.isNotBlank(menuCode)) {
                    menuInfo = (MenuInfo) menuInfoService.get(menuCode);
                    if (menuInfo != null) {
                        menuCode = menuInfo.getName();
                        menuCodingDetails = menuInfo.getCode();
                    }
                }
                // 是否隐藏
                String isHiddenStr = element.elementText("isHidden");
                boolean isHidden;
                if (StringUtils.isBlank(isHiddenStr) || (!"true".equals(isHiddenStr) && !"false".equals(isHiddenStr))) {
                    isHidden = false;
                } else {
                    isHidden = Boolean.parseBoolean(isHiddenStr);
                }
                // 排序
                String sortStr	 = element.elementText("sort");
                Long sort = null;
                if (StringUtils.isNotBlank(sortStr)) {
                    sort = Long.parseLong(sortStr);
                }
                // 备注
                String remark = element.elementText("remark");
                // 屏幕方向
                String screenType = element.elementText("screenType");
                // 企业id
                String cidStr = element.elementText("cid");
                Long cid = 1000L;
                if (StringUtils.isNotBlank(cidStr)) {
                    cid = Long.parseLong(cidStr);
                }
                // 获取最大id
                Number maxId = (Number) dataGroupDao.createNativeQuery("SELECT MAXID + 1 FROM SEQ_TABLE WHERE TABLENAME = 'BASE_APP_BUSINFO'").uniqueResult();
                Long appId = null;
                if (maxId == null) {
                    // SEQ_TABLE表中没有BASE_APP_BUSINFO
                    appId = 1000L;
                    dataGroupDao.createNativeQuery("INSERT INTO seq_table(TABLENAME, MAXID) VALUES('BASE_APP_BUSINFO', ?)", appId + 1).executeUpdate();
                } else {
                    appId = maxId.longValue();
                    dataGroupDao.createNativeQuery("UPDATE seq_table SET MAXID = MAXID + 1 WHERE TABLENAME = 'BASE_APP_BUSINFO'").executeUpdate();
                }
                String sql = "INSERT INTO " +
                        " base_app_businfo (ID," +
                        " CODE," +
                        " NAME," +
                        " APP_TYPE," +
                        " MODULE_CODE," +
                        " MODULE_CODING_DETAILS," +
                        " URL," +
                        " APP_ICON," +
                        " POWER_FLAG," +
                        " MENU_CODE," +
                        " MENU_CODING_DETAILS," +
                        " IS_HIDDEN," +
                        " SORT," +
                        " REMARK," +
                        " CREATE_TIME," +
                        " CREATE_STAFF_ID," +
                        " EFFECTIVE_STATE," +
                        " OWNER_DEPARTMENT_ID," +
                        " OWNER_POSITION_ID," +
                        " CREATE_POSITION_ID," +
                        " CREATE_DEPARTMENT_ID," +
                        " SCREEN_TYPE," +
                        " CID)" +
                        " VALUES (:id," +
                        " :code," +
                        " :name," +
                        " :appType," +
                        " :moduleCode," +
                        " :moduleCodingDetails," +
                        " :url," +
                        " :appIcon," +
                        " :powerFlag," +
                        " :menuCode," +
                        " :menuCodingDetails," +
                        " :isHidden," +
                        " " + (sort == null ? "null" : sort) + "," +
                        " :remark," +
                        " :time," +
                        " :staffId," +
                        " 0," +
                        " :departmentId," +
                        " :positionId," +
                        " :positionId," +
                        " :departmentId," +
                        " :screenType," +
                        " :cid)";
                dataGroupDao.createNativeQuery(sql).setParameter("id", appId)
                        .setParameter("code", code)
                        .setParameter("name", name)
                        .setParameter("appType", appType)
                        .setParameter("moduleCode", moduleCode)
                        .setParameter("moduleCodingDetails", moduleCodingDetails)
                        .setParameter("url", url)
                        .setParameter("appIcon", appIcon)
                        .setParameter("powerFlag", powerFlag)
                        .setParameter("menuCode", menuCode)
                        .setParameter("menuCodingDetails", menuCodingDetails)
                        .setParameter("isHidden", isHidden)
                        .setParameter("remark", remark)
                        .setParameter("cid", cid)
                        .setParameter("time", now)
                        .setParameter("staffId", staffId)
                        .setParameter("departmentId", departmentId)
                        .setParameter("positionId", positionId)
                        .setParameter("screenType", screenType)
                        .executeUpdate();
                // 根据菜单生成操作
                if (menuInfo != null && StringUtils.isNotBlank(moduleCodingDetails)) {
                    // 编码：模块编码_编码
                    String menuOperateCode = moduleCodingDetails + "_" + code;
                    MenuOperate menuOperate = menuOperateService.getMenuOperate(menuOperateCode);
                    if (menuOperate == null) {
                        // 名称：应用名称，url：请求链接
                        menuOperate = new MenuOperate();
                        menuOperate.setCode(menuOperateCode);
                        menuOperate.setName(name);
                        menuOperate.setUrl(url);
                        menuOperate.setMenuInfo(menuInfo);
                        menuOperate.setCid(cid);
                        menuOperateService.save(menuOperate);
                    }
                }
            }
        } catch ( Exception e) {
            log.error(e.getMessage(),e);
        }
    }

    @Override
    public void generateProjDataGridConfig(View view, List<Field> fields, List<Button> buttons, List<Event> events) {
        ExtraView extraView=view.getExtraView();
        if (view.getShowType() != ShowType.LAYOUT) {
            if (null != extraView) {
                String fullConfig = extraView.getFullConfig();
                if (null == fullConfig || fullConfig.trim().length() == 0) {
                    fullConfig = EcConfigServiceImpl.getEcFullConfig(view, events, fields, buttons);
                    extraView.setFullConfig(fullConfig);
                }
                extraView.setConfig(fullConfig);
                extraView.setConfigMap((Map) SerializeUitls.deserialize(extraView.getFullConfig()));
            }
        }
        prepareDataGrid(view.getDataGrids(), fields, buttons, events);
    }
    /**
     * 关联DataGrid的fields、buttons、events，并将fullConfig的内容复制到config，并转化为Map，赋值给configMap属性。
     *
     * @param dataGrids 待处理的 DataGrid 数组
     * @param fields 待关联的Field数组，通常为本模块的所有 fields
     * @param buttons 待关联的Button数组，通常为本模块的所有 buttons
     * @param events 待关联的Event数组，通常为本模块的所有 events
     */
    private void prepareDataGrid(List<DataGrid> dataGrids, List<Field> fields, List<Button> buttons, List<Event> events) {
        if (null != dataGrids && !dataGrids.isEmpty()) {
            for (DataGrid dataGrid : dataGrids) {
                if (null != dataGrid.getConfig() && dataGrid.getConfig().length() > 0) {
                    List<Field> dgFields = new ArrayList<Field>();
                    for (Field field : fields) {
                        if (null != field.getDataGrid() && field.getDataGrid().getCode().equals(dataGrid.getCode())) {
                            dgFields.add(field);
                        }
                    }
                    dataGrid.setFields(dgFields);

                    List<Button> dgButtons = new ArrayList<Button>();
                    for (Button button : buttons) {
                        if (null != button.getDataGrid() && button.getDataGrid().getCode().equals(dataGrid.getCode())) {
                            dgButtons.add(button);
                        }
                    }
                    dataGrid.setButtons(dgButtons);
                    List<Event> dgEvents = new ArrayList<Event>();
                    for (Event event : events) {
                        if (null != event.getLayoutCode() && event.getCode().startsWith(dataGrid.getCode())) {
                            dgEvents.add(event);
                        }
                    }
                    dataGrid.setEvents(dgEvents);
                    String config = dataGrid.getFullConfig();
                    if (null == config || config.isEmpty()) {
                        config = EcConfigServiceImpl.getEcFullConfig(dataGrid, events, fields, buttons);
                        dataGrid.setFullConfig(config);
                    }
                    dataGrid.setConfig(config);
                    dataGrid.setConfigMap((Map<String, Object>) SerializeUitls.deserialize(dataGrid.getFullConfig()));
                }
            }
        }
    }
    
    @Override
    public void deleteProjViewHtml(View view) {
    	if (null != view) {
    		deleteProjViewHtml(view.getName(), view.getAssModel().getModelName(), view.getEntity().getEntityName(), view.getEntity().getModule().getArtifact());
    	}
    }
    
    @Override
    public void deleteProjViewHtml(String viewName, String modelName, String entityName, String artifact) {
        String viewPath=PropertyHolder.get().getWorkspacePath()+File.separator+"customFile" + File.separator+"static" + File.separator
                + artifact + File.separator+ entityName + File.separator
                + firstLatterToLowerCase(modelName) + File.separator;
        File dir=new File(viewPath);
        if(dir != null){
            if(dir.exists()){
                Set<String> aqjName = new HashSet<String>();
                File[] fs=dir.listFiles();
                if(fs !=null){
                    for(File f:fs){
                        if(f.getName().equals(viewName+".html")||f.getName().startsWith(viewName+"-")){
                            f.delete();
                        } else if(aqjName.contains(f.getName())){
                            f.delete();
                        }
                    }
                    fs=dir.listFiles();
                    for(File f:fs){
                        if(f.getName().equals(viewName+".html")||f.getName().startsWith(viewName+"-")){
                            throw new EcException(InternationalResource.get("common.delete.failure"));
                        }
                    }
                }
            }
        }
        // 删除数据库
        String url = "/" + artifact + "/" + entityName 	+ "/" + firstLatterToLowerCase(modelName)
			+ "/proj/" + viewName;
        String sql = "delete from app_project_static where url = ? and tenant_id = ?";
        jdbcTemplate.update(sql, url, RpcContext.getContext().getTenantId());
    }
    public static String firstLatterToLowerCase(String key) {
        char fl = ((String) key).charAt(0);
        return Character.toLowerCase(fl) + ((String) key).substring(1);
    }
}
