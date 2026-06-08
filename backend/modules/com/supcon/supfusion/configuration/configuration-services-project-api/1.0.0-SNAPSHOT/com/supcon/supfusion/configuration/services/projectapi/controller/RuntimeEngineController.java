package com.supcon.supfusion.configuration.services.projectapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.configuration.services.entity.Entity;
import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.configuration.services.entity.ModuleRelation;
import com.supcon.supfusion.configuration.services.entity.UploadInfo;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.openapi.utils.DtoUtils;
import com.supcon.supfusion.configuration.services.openapi.vo.EntityVO;
import com.supcon.supfusion.configuration.services.openapi.vo.ModuleVO;
import com.supcon.supfusion.configuration.services.openapi.vo.ResponseMsg;
import com.supcon.supfusion.configuration.services.openapi.vo.UploadInfoVO;
import com.supcon.supfusion.configuration.services.openapi.wrapper.EntityWrapper;
import com.supcon.supfusion.configuration.services.openapi.wrapper.ModuleWrapper;
import com.supcon.supfusion.configuration.services.openapi.wrapper.UploadInfoWrapper;
import com.supcon.supfusion.configuration.services.projectapi.services.ProjImportExportAdmin;
import com.supcon.supfusion.configuration.services.service.EntityService;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.configuration.services.service.ModuleService;
import com.supcon.supfusion.configuration.services.utils.EcUtils;
import com.supcon.supfusion.configuration.services.utils.PropertyHolder;
import com.supcon.supfusion.configuration.services.utils.UnZipFile;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/1/14
 */
@Slf4j
@Controller
public class RuntimeEngineController extends ConfigurationBaseController {
    private static final String ZIP_NAME = "ProjectConfig";
    private static final String HTML_HEAD = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />"
            + "<meta http-equiv=\"pragma\" content=\"no-cache\" /><meta http-equiv=\"cache-control\" content=\"no-cache\" />"
            + "<meta http-equiv=\"expires\" content=\"-1\" /><title>console</title><style>*{font-size:12px;}</style></head><body>";

    // ~ 所需要的service =======================================================
    @Resource
    private EntityService entityService;
    private static final EntityWrapper entityWrapper = new EntityWrapper();
    @Autowired
    private ModuleService moduleService;
    @Autowired
    private InternationalService internationalService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private ProjImportExportAdmin projImportExportAdmin;
    private static final UploadInfoWrapper uploadInfoWrapper = new UploadInfoWrapper();
    @Value("${configuration-services.uploadLogPath:logs/appUpload}")
    private String logDirectoryPath;
    /**
     * 进入微服务模块管理主框架
     *
     * @return
     */
    @RequestMapping(value = "/ec/engine/msManage")
    public String msManage(ModelMap map) throws Exception {
        HttpSession session = getRequest().getSession();
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        boolean isDev = PropertyHolder.isDev();
        String isMsService ="false";
        map.addAttribute("isMsService", isMsService);
        map.addAttribute("isDev", isDev);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "project/engine/manage";
    }

    @ResponseBody
    @RequestMapping(value = "/ec/engine/entities")
    public Page<EntityVO> list(Integer pageNo, Integer pageSize, boolean exportFlag) {
        Page<Entity> entities = new Page<Entity>(pageNo, pageSize);
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        Module module = new Module();
        List<Entity> listentities = null;
        List<ModuleRelation> relations = null;
        String names = null;
        if (getRequest().getParameter("module.code") != null && !"".equals(getRequest().getParameter("module.code"))) {
            module.setCode(getRequest().getParameter("module.code"));
        }
        if (null == module || null ==module.getCode()) {
            entities = entityService.findEntities(entities);
        }else{
            entities = entityService.findEntities(entities, module);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return entityWrapper.e2vPage(entities);
    }

    @RequestMapping(value = "/ec/engine/view")
    public String edit(ModelMap map, @Nullable @RequestParam("moduleCode") String moduleCode, @Nullable @RequestParam("isView") boolean isView, @Nullable @RequestParam("entity.code") String entityCode) throws Exception {
        Map<String, Object> responseMap = new HashMap<String, Object>();
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        Entity entity = null;
        if (!StringUtils.isEmpty(entityCode)) {
            entity = entityService.getEntity(entityCode);
        }
        responseMap.put("isRead", false);
        Module module = null;
        if (null != entity && entity.getModule() != null) {
            module = moduleService.getModule(entity.getModule().getCode());
        } else {
            module = moduleService.getModule(moduleCode);
        }
        responseMap.put("isProject", true);
        map.addAttribute("responseMap", responseMap);
        map.addAttribute("module", module);
        map.addAttribute("entity", entity);
        map.addAttribute("isView", isView);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "project/engine/edit_entity";
    }
    @RequestMapping(value = "/ec/engine/entityInfo")
    public String entityInfo(ModelMap map, @Nullable @RequestParam("moduleCode") String moduleCode, @Nullable @RequestParam("isView") boolean isView, @Nullable @RequestParam("entity.code") String entityCode) throws Exception {
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        Map<String, Object> responseMap = new HashMap<String, Object>();
        Entity entity = null;
        if (!StringUtils.isEmpty(entityCode)) {
            entity = entityService.getEntity(entityCode);
        }
        responseMap.put("isRead", false);
        Module module = null;
        if (null != entity && entity.getModule() != null) {
            module = moduleService.getModule(entity.getModule().getCode());
        } else {
            module = moduleService.getModule(moduleCode);
        }
        responseMap.put("isProject", true);
        map.addAttribute("responseMap", responseMap);
        map.addAttribute("module", module);
        map.addAttribute("entity", entity);
        map.addAttribute("isView", isView);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "project/engine/entityInfo";
    }
    @RequestMapping(value = "/ec/engine/config")
    public String config(ModelMap map, @RequestParam("entity.code") String entityCode) throws Exception {
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        Entity entity = entityService.getEntity(entityCode);
        map.addAttribute("entity", entity);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "project/engine/config";
    }


    /**
     * 实体保存结果页面
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/engine/saveEntity")
    public String save(HttpServletRequest request) throws Exception {
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        Entity entity = DtoUtils.getEntity(request);
        entity.setProjFlag(true);
        entityService.saveEntity(entity);

        ResponseMsg response = new ResponseMsg(true);
        ObjectMapper mapper = new ObjectMapper();
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return mapper.writeValueAsString(response);
    }
    @ResponseBody
    @RequestMapping(value = "/ec/engine/backup")
    public String backup(@RequestParam("module.code") String moduleCode) throws IOException {
        getResponse().setContentType("text/html;charset=UTF-8");
        PrintWriter out = getResponse().getWriter();
        out.write(HTML_HEAD);
        out.write("<style>a:link,a:visited{color:#0f78bc;}a:hover{color:#FF0000;}a:active{color:#FF0000;}</style>");
        out.write(InternationalResource.get("ec.module.generate.startCompress") + "<br />");
        out.flush();
        ArrayList<String> param=new ArrayList<String>();
        String[] moduleCodes=moduleCode.split(",");
        for(String md:moduleCodes){
            param.add(md);
        }
        try{
            projImportExportAdmin.exportProj(param);
        }catch (Exception e){
            log.error(e.getMessage(),e);
            out.write(e.getMessage()+ ", <br />"+InternationalResource.get("ec.project.upload.error2")+logDirectoryPath.substring(0,logDirectoryPath.lastIndexOf("/"))+"/appConfig.log" + "<br />");
            out.flush();
            return null;
        }

        out.write(InternationalResource.get("ec.module.generate.endCompress") + "<br />");
        out.flush();
        out.write("<a href=\"down-zip?module.code=" + moduleCode + "\">"
                + InternationalResource.get("ec.engine.generate.successTips") + "</a>");
        out.flush();
        out.write("</body></html>");
        out.flush();
        return null;
    }

    @ResponseBody
    @RequestMapping(value = "/ec/engine/down-zip")
    public ResponseEntity downloadZip(@RequestParam("module.code") String moduleCode) throws Exception {
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        Module module = moduleService.getModule(moduleCode);
        InputStream is = new FileInputStream(new File(PropertyHolder.get().getProjPath() + File.separator + ZIP_NAME+ ".zip"));
        HttpHeaders headers = new HttpHeaders();
        String downloadFileName="";
        if(module==null||module.getProjectVersion()==null){
            downloadFileName = "ProjectConfig.zip";
        }else{
            downloadFileName= module.getCode().split("_")[0]+"_"+module.getProjectVersion()+ "_Project.zip";
        }
        headers.setContentDispositionFormData("attachment", new String(downloadFileName.getBytes("UTF-8"),"ISO-8859-1"));
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("application/octet-stream;charset=ISO8859-1")).body(new InputStreamResource(is));
    }

    @RequestMapping(value = "/ec/engine/backupModule")
    public String backupModule(ModelMap map){
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        List<Module> modules = modelService.getAllModule();
        map.addAttribute("modules",modules);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "project/engine/backupModule";
    }
    @RequestMapping(value = "/ec/engine/upload")
    public String upload(){
        return "project/engine/upload";
    }
    @ResponseBody
    @RequestMapping(value = "/ec/engine/uploadBatchModuleQuery")
    public Page<UploadInfoVO> uploadBatchModuleQuery(String filePath){
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        Date startDate=new Date();
        log.info("Starting time of batch upload extraction module information:"+new Date());
        String uploaded = PropertyHolder.get().getGeneratePath() + File.separator + "uploaded" + File.separator + filePath;
        Map<String, Object> returnMag = getUploadInfoPage(uploaded);
//        String message = (String)returnMag.get("message");
        StringBuffer allMessage = new StringBuffer();
        Page<UploadInfo> uploadInfos = new Page<>();
     /*   if(uploadInfos.getResult().size()>0){
            uploadInfos.getResult().get(0).setErrorMsg(message);
        }*/
        Date date1=new Date();
        log.info("It takes time to upload and extract module information in batches:"+((date1.getTime()-startDate.getTime())/1000)+"秒");
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return uploadInfoWrapper.e2vPage(uploadInfos);
    }

    public Map<String,Object> getUploadInfoPage(String uploaded){
        Map<String,Object> returnMsg = new HashMap<String, Object>();
        StringBuffer sbMessage = new StringBuffer();//记录错误信息
        Set<String> moduleRepeatSet = new HashSet<String>();
        File uploadFile = new File(uploaded);
        File receiveFile = null;
        File bigZip  = null;
        Boolean isExistIllegalFile = false;
        if(uploadFile.listFiles().length>0){
            bigZip = uploadFile.listFiles()[0];
        }
        //这里加个空文件的判断
        boolean isEmpty = isEmptyModule(bigZip);
        if(isEmpty){
            throw new EcException("上载zip包为空");
        }
        UnZipFile.unzip(bigZip, uploadFile);
        try {
            if (uploadFile.isDirectory()) {
                File[] fileLists = uploadFile.listFiles();
                if (fileLists.length > 0) {
                    for (Integer listCount = 0; listCount < fileLists.length; listCount++) {
                        receiveFile = fileLists[listCount];
                        if (receiveFile.getName().endsWith(".zip")) {
                            boolean isValidModule = false;
                            ZipFile zf = new ZipFile(receiveFile);
                            InputStream in = new BufferedInputStream(
                                    new FileInputStream(receiveFile));
                            Charset gbk = Charset.forName("gbk");
                            ZipInputStream zin = null;
                            ZipEntry ze;

                        }
                    }
                }
            }
        }catch (IOException e) {
                EcUtils.uploadFullLogger.info("uploadBatchException:"+e.getMessage());
                EcUtils.uploadLogger.info("uploadBatchException:"+e.getMessage());
        }
        return returnMsg;
    }
    private boolean isEmptyModule(File file ){
        boolean isEmpty = false;
        InputStream in;
        try {
            in = new BufferedInputStream(
                    new FileInputStream(file));
            Charset gbk = Charset.forName("gbk");
            ZipInputStream zin = new ZipInputStream(in, gbk);
            ZipEntry ze;
            while ((ze = zin.getNextEntry()) == null && !isEmpty) {
                isEmpty = true;
            }
        } catch (IOException e1) {
            // TODO Auto-generated catch bloc!
            e1.printStackTrace();
        }
        return isEmpty;
    }
    private boolean isSingleModule(File file ){
        boolean isSingle = false;
        InputStream in;
        try {
            in = new BufferedInputStream(
                    new FileInputStream(file));
            Charset gbk = Charset.forName("gbk");
            ZipInputStream zin = new ZipInputStream(in, gbk);
            ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                log.info("当前循环到"+ze.toString());
                if ("pom.xml".equals(ze.getName())) {
                    isSingle = true;
                    break;
                }
            }
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return isSingle;
    }

    @RequestMapping(value = "/ec/engine/uploadBatchModules")
    public String uploadBatchModules(ModelMap map, @RequestParam("receiveFile") MultipartFile multipartFile){
        String isMsService =getRequest().getParameter("isMsService");
        //这里开始把temp目录下的文件转移到bap的workspace的目录下
        Date startDate=new Date();
        log.info(InternationalResource.get("ec.module.uploadBatch.StartCopyUpload")+new Date());
        String filePath = "up" + System.currentTimeMillis();
        String uploaded = PropertyHolder.get().getGeneratePath() + File.separator + "projUploaded" + File.separator + filePath;
        File uploadedFile = new File(uploaded);
        if (!uploadedFile.exists()) {
            uploadedFile.mkdirs();
        }
        InputStream ins = null;
        try {
            File receiveFile = new File(multipartFile.getOriginalFilename());
            ins = multipartFile.getInputStream();
            inputStreamToFile(ins, receiveFile);
            FileUtils.copyFileToDirectory(receiveFile, uploadedFile);
            receiveFile.delete();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                ins.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        Date date1=new Date();
        log.info(InternationalResource.get("ec.module.uploadBatch.StartCopyUploadTime")+((date1.getTime()-startDate.getTime())/1000)+"秒");
        map.addAttribute("filePath", filePath);
        map.addAttribute("isMsService", getRequest().getParameter("isMsService"));
        return "project/engine/uploadBatchModules";
    }

    @RequestMapping(value = "/ec/engine/receive")
    public String receive(@RequestParam("receiveFile") MultipartFile multipartFile,@RequestParam("uploadFilter") List<String> uploadFilter) throws Exception {
        log.info("工程期配置开始解析");
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        getResponse().setContentType("text/html;charset=UTF-8");
//        List<String> uploadFilter =new ArrayList<>();
        PrintWriter out = getResponse().getWriter();
        out.write(HTML_HEAD);
        File receiveFile = new File(multipartFile.getOriginalFilename());
        InputStream ins = null;
        ins = multipartFile.getInputStream();
        inputStreamToFile(ins, receiveFile);
        if (null != receiveFile) {
            String deploySuccessMsg = "<script type=\"text/javascript\">try{parent.ec.module.refreshModuleTree();parent.$( '#frame3_preStep_Btn' ).attr('canclick','true').removeClass( 'cui-btn-gray' );}catch(e){}</script>";
            out.write(InternationalResource.get("ec.engine.generate.startRestore") + "<br />");
            out.flush();
            try {
                EcUtils.generateInfoMap.set("new_" + receiveFile.getName());
                projImportExportAdmin.importProj(receiveFile,uploadFilter);
            }catch(EcException e){
//                out.write(InternationalResource.get("ec.engine.wrongzipfile") + "<br />");
                out.write(e.getMessage()+ "<br />");
                out.write("<span style='color:red'>"
                        + InternationalResource.get("ec.module.generate.uploadFailed") + "</span><br />");
                out.flush();
                log.info(e.getMessage());
                throw e;
            }catch(Exception e){
                out.write("<span style='color:red'>"
                        + InternationalResource.get("ec.module.generate.uploadFailed") + "</span><br />");
                out.flush();
                log.info(e.getMessage());
                throw e;
            }
            finally{
                EcUtils.generateInfoMap.remove();
            }
            out.write(InternationalResource.get("ec.engine.generate.restored") + "<br />");
            out.write(deploySuccessMsg);
        }else {
            out.write("<span style='color:red'>"
                    + InternationalResource.get("ec.module.generate.uploadFailed") + "</span><br />");
            out.flush();
        }
        out.write("</body></html>");
        out.flush();
        log.info("工程期配置结束解析");
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return null;
    }
    /**
     * 左侧树增加分类节点
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/engine/datalist")
    public List<ModuleVO> datalist(String code, String category) throws Exception {
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        List<Module> modules = moduleService.findAllModules();
        if ((StringUtils.isNotEmpty(category) && !"null".equals(category)) && (StringUtils.isEmpty(code) || "null".equals(code))) {//getcategorysubNode
            List<String> values = internationalService.getI18nKey(category);
            if(values.size()>0){
                modules = moduleService.getMsModuleByCatetorys(values,modules);
            }else{
                modules = moduleService.getMsModuleByCatetory(category,modules);
            }
        } else {//getlist
            modules = moduleService.findAllMsModules("");
            if (null != modules && !modules.isEmpty()) {
                List<Module> modulesList = new ArrayList<Module>();
                List<String> categoryList = new ArrayList<String>();
                Map<String,String> categoryMap = new HashMap<String, String>();
                for(Module curModule: modules){
                    String categoryTmp = InternationalResource.get(curModule.getCategory());
                    if(StringUtils.isNotEmpty(categoryTmp)){
                        if (categoryMap.get(categoryTmp)==null) {
                            categoryMap.put(categoryTmp,curModule.getCategory());
                        }else{
                            String value =categoryMap.get(categoryTmp);
                            categoryMap.put(categoryTmp,value+","+curModule.getCategory());
                        }
                    }
                }
                for (Module curModule: modules) {
                    String categoryTmp = InternationalResource.get(curModule.getCategory());
                    if (StringUtils.isNotEmpty(categoryTmp)) {
                        if (!categoryList.contains(categoryTmp)) {
                            List<String> values = new ArrayList<String>();
                            values = Arrays.asList(categoryMap.get(categoryTmp).split(","));
                            categoryList.add(categoryTmp);
                            CategoryModule categoryModule = new CategoryModule();
                            categoryModule.setName(categoryTmp);
                            categoryModule.setCategory(categoryTmp);
                            categoryModule.setIsParent(true);
                            categoryModule.setOpen(true);
                            List<Module> subModList =new ArrayList<Module>();
                            if(values.size()>0){
                                subModList = moduleService.getMsModuleByCatetorys(values,modules);
                            }else{
                                subModList = moduleService.getMsModuleByCatetory(categoryTmp,modules);
                            }
                            categoryModule.setChildren(subModList);
                            modulesList.add(categoryModule);
                        }else{

                        }
                    } else {
                        modulesList.add(curModule);
                    }
                }
                modules = modulesList;
            }
        }
        ModuleWrapper wrapper = new ModuleWrapper();
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return wrapper.e2vList(modules);
    }
    class CategoryModule extends Module {
        private Boolean isParent = false;
        private Boolean open = false;
        public Boolean getOpen() {
            return open;
        }
        public void setOpen(Boolean open) {
            this.open = open;
        }
        private List<Module> children = null;

        public Boolean getIsParent() {
            return isParent;
        }
        public void setIsParent(Boolean isParent) {
            this.isParent = isParent;
        }
        public List<Module> getChildren() {
            return children;
        }
        public void setChildren(List<Module> children) {
            this.children = children;
        }
    }

    private static void inputStreamToFile(InputStream ins, File file) {
        try {
            OutputStream os = new FileOutputStream(file);
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = ins.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
            ins.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
