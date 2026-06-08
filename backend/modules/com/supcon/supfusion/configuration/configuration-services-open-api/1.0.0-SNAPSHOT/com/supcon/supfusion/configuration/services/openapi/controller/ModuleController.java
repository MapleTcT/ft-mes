/**

 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.

 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.openapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.configuration.services.dao.ModuleCompanyRefDaoImpl;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.openapi.utils.DtoUtils;
import com.supcon.supfusion.configuration.services.openapi.vo.*;
import com.supcon.supfusion.configuration.services.openapi.wrapper.EntityWrapper;
import com.supcon.supfusion.configuration.services.openapi.wrapper.ModuleWrapper;
import com.supcon.supfusion.configuration.services.openapi.wrapper.UploadInfoBatchWrapper;
import com.supcon.supfusion.configuration.services.openapi.wrapper.UploadInfoWrapper;
import com.supcon.supfusion.configuration.services.service.*;
import com.supcon.supfusion.configuration.services.utils.EcUtils;
import com.supcon.supfusion.configuration.services.utils.PropertyHolder;
import com.supcon.supfusion.configuration.services.utils.UnZipFile;
import com.supcon.supfusion.configuration.services.utils.UploadLog;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import com.supcon.supfusion.rbac.api.IMenuInfoApiService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * 此Controller控制的CRUD及生成编译打包部署.
 *
 * @author songjiawei,wangting
 *
 */
@Slf4j
@Setter
@Getter
@Controller
public class ModuleController extends ConfigurationBaseController {

	private static final ModuleWrapper moduleWrapper = new ModuleWrapper();
	private static final EntityWrapper entityWrapper = new EntityWrapper();
	private static final UploadInfoWrapper uploadInfoWrapper = new UploadInfoWrapper();
	private static final UploadInfoBatchWrapper uploadInfoBatchWrapper = new UploadInfoBatchWrapper();


	private static final String HTML_HEAD = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />"
			+ "<meta http-equiv=\"pragma\" content=\"no-cache\" /><meta http-equiv=\"cache-control\" content=\"no-cache\" />"
			+ "<meta http-equiv=\"expires\" content=\"-1\" /><title>console</title><style>*{font-size:12px;}</style>"
			+ "</head><body>";
	private static final String VXMES = "VxMES.zip";
	private static final String SUPPLANT = "supPlant.zip";
	// ~ 所需要的service =======================================================
	@Autowired
	private ModuleService moduleService;
	@Resource
	private ModuleReferenceService moduleReferenceService;
	@Autowired
	private GenerateService generateService;
	@Autowired
	private EcDataSynchronizeService ecDataSynchronizeService;
	@Resource
	private FieldService fieldService;
	@Autowired
	private ModuleCompanyRefDaoImpl moduleCompanyRefDao;

	@Autowired
	private IMenuInfoApiService iMenuInfoApiService;
	@Autowired
	private InternationalService internationalService;

	@Autowired
	private ViewService viewService;
	@Value("${masterSlave.enabled:false}")
	private boolean masterSlaveEnabled;
	@Value("${bap.company.single:false}")
	private Boolean isSingleMode = false;
	@Value("${integration.supos.enabled:false}")
	private Boolean supos;
	private int sizeMax = 2;



	/**
	 * 进入模块管理主框架
	 *
	 * @return
	 */
	@RequestMapping(value = "/ec/module/manage")
	public String manage(ModelMap map, HttpServletResponse response) throws Exception {
		boolean isDev = PropertyHolder.isDev();
		String isMsService ="";
		map.addAttribute("isDev", isDev);
		map.addAttribute("isMsService", isMsService);
		return "module/manage";
	}

	/**
	 * 进入微服务模块管理主框架
	 *
	 * @return
	 */
	@RequestMapping(value = "/ec/module/msManage")
	public String msManage(ModelMap map) throws Exception {
		HttpSession session = getRequest().getSession();
		boolean isDev = PropertyHolder.isDev();
		String isMsService ="Mis";
		map.addAttribute("isDev", isDev);
		map.addAttribute("isMsService", isMsService);
		return "module/manage";
	}

	/**
	 * 进入模块管理编辑页面 当code不为空时，编辑页面否则为添加页面
	 *
	 * @return
	 */
	@RequestMapping(value = "/ec/module/edit")
	public String edit(ModelMap map, @Nullable @RequestParam("module.code") String moduleCode) throws Exception {
		String isMsService = getRequest().getParameter("isMsService");
		List<ModuleRelation> relations = null;
		List<ModuleReference> references = null;
		String ids = null;
		Module module = null;
		if (null != moduleCode && moduleCode.length() > 0) {
			module = moduleService.getModule(moduleCode);
			relations = moduleService.getRelations(module);
			references = moduleReferenceService.getReferences(module);
			ids = ",";
			for(ModuleRelation relation : relations){
				ids += relation.getTarget().getCode();
			}
			StringBuffer moduleReferencemultiselectIDs = new StringBuffer();
			StringBuffer moduleReferencemultiselectNames = new StringBuffer();
			for(ModuleReference reference : references){
				moduleReferencemultiselectIDs.append(reference.getTarget().getCode()).append(",");
				moduleReferencemultiselectNames.append(InternationalResource.get(reference.getTarget().getName())).append(",");
			}
			if(moduleReferencemultiselectIDs.length() > 0){
				module.setModuleReferencemultiselectIDs(moduleReferencemultiselectIDs.substring(0, moduleReferencemultiselectIDs.length() -1));
			}
			if(moduleReferencemultiselectIDs.length() > 0){
				module.setModuleReferencemultiselectNames(moduleReferencemultiselectNames.substring(0, moduleReferencemultiselectNames.length() - 1));
			}
			List<Long> companies = moduleService.findCompaniesByModuleCode(moduleCode);
			module.setCompanyIds(StringUtils.join(companies.stream().map(Objects::toString).collect(Collectors.toList()),","));
			map.addAttribute("module", module);
		}
		map.addAttribute("relations", relations);
		map.addAttribute("references", references);
		map.addAttribute("ids", ids);
		map.addAttribute("isMsService", isMsService);
		map.addAttribute("supos",supos);

		return "module/edit";
	}

	/**
	 * 进入模块管理列表页面 第一次进入加载进树
	 *
	 * @return
	 */
	@GetMapping(value = "/ec/module/list")
	@ResponseBody
	public List<ModuleVO> list() throws Exception {
		List<Module> modules = moduleService.findAllModules();
		return moduleWrapper.e2vList(modules);
	}



	/**
	 * 左侧树增加分类节点
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/module/datalist")
	public List<ModuleVO> datalist(String code, String category) throws Exception {

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
		return wrapper.e2vList(modules);
	}

	@SuppressWarnings("serial")
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

	/**
	 * 进入模块发布管理列表页面
	 *
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/module/listModules")
	public Page<ModuleVO> listModules() throws Exception {
		Page<Module> moduleList = new Page<Module>(100);
		List<Module> modules = moduleService.findAllModules();
		Module sysModule = new Module();
		sysModule.setCode("sysbase_1.0");
		modules.remove(sysModule);
		moduleList.setResult(modules);
		return moduleWrapper.e2vPage(moduleList);
	}

	/**
	 * 批量发布模块管理发布页面
	 *
	 * @return
	 */
	@RequestMapping(value = "/ec/module/batchDeployWait")
	public String batchDeployWait(ModelMap map) throws Exception {
		String isMsService =getRequest().getParameter("isMsService");
		boolean isProduct = PropertyHolder.isProduct();
		map.addAttribute("isMsService", isMsService);
		map.addAttribute("isProduct", isProduct);
		log.info("启动日志");
		return "module/batchDeployWait";
	}

	/**
	 * 模块管理编辑结果页面
	 *
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/module/save")
	public ResponseMsg save(HttpServletRequest request) throws Exception {
		try {
			Module module = DtoUtils.getModuleVO(request);
			Map map = getRequest().getParameterMap();
			String isMsService =getRequest().getParameter("isMsService");
			if(StringUtils.isNotEmpty(isMsService)){
				module.setType(isMsService);
			}
			moduleService.saveModule(module, map);
		} catch (IllegalArgumentException e) {
			if(e.getMessage().contains("invalid format:")){
				throw new Exception("版本格式错误");
			}else{
				throw e;
			}

		}
		ResponseMsg msg = new ResponseMsg(true);
		return msg;
	}




	/**
	 * 模块管理删除结果页面
	 *
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/module/delete")
	public ResponseMsg delete(Module module, HttpServletRequest request) throws Exception {
//		moduleService.sessionEvict();
		return deleteChoise(true, module, request);
	}

	/**
	 * 模块管理逻辑删除结果页面
	 *
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/module/ordinaryDelete")
	public ResponseMsg ordinaryDelete(Module module, HttpServletRequest request) throws Exception {
		return deleteChoise(false, module, request);
	}

	/**
	 * 模块管理逻辑删除结果页面
	 *
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/module/moduleReadOnly")
	public String moduleReadOnly(Module module) throws Exception {
		ResponseMsg response = new ResponseMsg();
		if(null == module){
			response.setSuccess(false);
			response.setExceptionMsg("ec.entity.nofindModule");
		} else {
			if(module.getIsReadOnly()){
				response.setSuccess(true);
			} else {
				response.setSuccess(false);
			}
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}

	private ResponseMsg deleteChoise(boolean flag, Module module, HttpServletRequest request) throws JsonProcessingException {
		module.setCode(request.getParameter("module.code"));
		ResponseMsg response = new ResponseMsg(true);
		if (null == module) {
			response.setSuccess(false);
			response.setExceptionMsg("ec.entity.nofindModule");
		} else {
			String responseMsg = moduleService.reallyDeleteModule(module);
			if(responseMsg!=null&&responseMsg.length()!=0&&(!("".equals(responseMsg)))){
				response.setSuccess(false);
				response.setExceptionMsg(responseMsg);
			} else {
				ecDataSynchronizeService.synchronizeEcDataFromDevToRumtime(module == null ? null : module.getCode());
			}
		}
		return response;
	}

	/**
	 * 模块管理发布页面
	 *
	 * @return
	 */
	@RequestMapping(value = "/ec/module/generatewait")
	public String generateWait() throws Exception {
		return "module/generatewait";
	}

	/**
	 * 模块上载等待页面
	 *
	 * @return
	 */
	@RequestMapping(value = "/ec/module/uploadwait")
	public String uploadWait() throws Exception {
		return "module/uploadwait";
	}

	@RequestMapping(value = "/ec/module/generateSelect")
	public String generateSelect(){
		return "module/generate";
	}

	@ResponseBody
	@RequestMapping(value = "/ec/module/syncMnecode")
	public ResponseMsg syncMnecode(HttpServletRequest request) throws Exception {
		Module module = new Module();
		module.setCode(request.getParameter("module.code"));

		if (module != null) {
			moduleService.dealMneCodeByModule(module.getCode());
		} else {
			moduleService.dealMneCodeByModule(null);
		}
		ObjectMapper mapper = new ObjectMapper();
		ResponseMsg response = new ResponseMsg(true);
//		return mapper.writeValueAsString(response);
		return response;
	}

	@ResponseBody
	@RequestMapping(value = "/ec/module/updateField")
	public ResponseMsg updateField(String entityCodes) throws Exception {
		if (entityCodes != null && entityCodes.length() > 0) {
			fieldService.updateFieldsByEntityCodes(entityCodes);
		}
		ObjectMapper mapper = new ObjectMapper();
		ResponseMsg response = new ResponseMsg(true);
//		return mapper.writeValueAsString(response);
		return response;
	}

	@ResponseBody
	@RequestMapping(value = "/ec/module/execScript")
	public String execScript(Module module) throws Exception {
		ResponseMsg response = new ResponseMsg(true);
		try {
//			deploymentAdmin.execScript(module.getCode(), module.getArtifact(), module.getProjectVersion());
			response.setSuccess(true);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			response.setSuccess(false);
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}

	@ResponseBody
	@RequestMapping(value = "/ec/module/list-select")
	public List<ModuleVO> listselect(HttpServletRequest request) throws Exception {
		//modules = moduleService.findAllModules();
		String moduleCode = request.getParameter("module.code");
		Module module = moduleService.getModule(moduleCode);

		List<ModuleRelation> list = moduleService.getRelations(module);
		List<ModuleReference> references = moduleReferenceService.getReferences(module);
		List<Module> modules = new ArrayList<Module>();
		for(ModuleRelation r : list){
			modules.add(r.getTarget());
		}
		for(ModuleReference reference : references){
			modules.add(reference.getTarget());
		}
		modules.add(moduleService.getModule("sysbase_1.0"));
		modules.add(module);
		return moduleWrapper.e2vList(modules);
	}

	@ResponseBody
	@RequestMapping(value = "/ec/module/down-source", produces = MediaType.TEXT_HTML_VALUE)
	public String downSource(String moduleCodes, Boolean excludeCustomFile, String isMsService) throws Exception {
		String value = "normal-font";
		boolean isBatch = false;//标记是否批量下载
		getResponse().setContentType("text/html;charset=UTF-8");
		PrintWriter out = getResponse().getWriter();
		String link = "<link rel=\"stylesheet\" type=\"text/css\" href=\"/bap/static/bap-fonts/"+value+"/font.css\">";
		out.write(HTML_HEAD.replace("</head><body>", link+"</head><body>"));
		out.write("<style>a:link,a:visited{color:#0f78bc;}a:hover{color:#FF0000;}a:active{color:#FF0000;}</style>");
		out.write(InternationalResource.get("ec.module.generate.startCompress") + "<br />");

		out.flush();
		if(null != moduleCodes && !"".equals(moduleCodes)){
			for(String moduleCode : moduleCodes.split(",")){
				Module module = moduleService.getModule(moduleCode);
				if (null != module){
					String moduleName = InternationalResource.get(module.getName());
					out.write("<div id=\"download\">"+InternationalResource.get("ec.module.download.zip") + moduleName+"</div>");
					out.flush();
					generateService.compress(module, true);
					out.write("<div id=\"download\">"+moduleName+InternationalResource.get("ec.module.download.complete")+"</div>");
					out.flush();
				}
			}
			if(moduleCodes.split(",").length>1){
				isBatch = true;
				generateService.compressMul(moduleCodes, excludeCustomFile, isMsService);
			}
		}
		out.write(InternationalResource.get("ec.module.generate.endCompress") + "<br />");
		out.flush();
		if(isBatch){
			out.write("<a id=\"downloadHref\" href=\"downBatch-zip?downFile=VXMES&isMsService=" + isMsService + " \">"
					+ InternationalResource.get("ec.module.generate.successTips") + "</a>");
		}else{
			out.write("<a id=\"downloadHref\" href=\"down-zip?module.code=" + moduleCodes + "\">"
					+ InternationalResource.get("ec.module.generate.successTips") + "</a>");
		}
		out.flush();
		//out.write("</body></html>");
		out.flush();
		return null;
	}

	@RequestMapping(value = "/ec/module/downloadModule")
	public String downloadModule(ModelMap map) throws Exception {
		String isMsService = getRequest().getParameter("isMsService");
		map.addAttribute("isMsService", isMsService);
		return "module/downloadBatchModules";
	}

	@ResponseBody
	@RequestMapping(value = "/ec/module/downloadModule-query")
	public Page<ModuleVO> downloadModuleQuery(Integer pageSize, Integer maxPageSize) throws Exception {
		Page<Module> moduleList = new Page<Module>(pageSize, maxPageSize);
		List<Module> allModules = moduleService.findAllModules();
		Module sysModule = new Module();
		sysModule.setCode("sysbase_1.0");
		allModules.remove(sysModule);
		moduleList.setResult(allModules);
		return moduleWrapper.e2vPage(moduleList);
	}

	@Value("${configuration-services.workspace:''}")
	private String workspacePath;

	@ResponseBody
	@RequestMapping(value = "/ec/module/down-zip")
	public ResponseEntity downloadZip(@RequestParam("module.code") String moduleCode) throws Exception {
		Module module = moduleService.getModule(moduleCode);
		InputStream is = new FileInputStream(new File(PropertyHolder.get().getGeneratePath() + File.separator + module.getCode().split("_")[0]+"_"+module.getProjectVersion()+ ".zip"));
		HttpHeaders headers = new HttpHeaders();
		String downloadFileName = module.getCode().split("_")[0]+"_"+module.getProjectVersion()+ ".zip";
		headers.setContentDispositionFormData("attachment", new String(downloadFileName.getBytes("UTF-8"),"ISO-8859-1"));
		return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("application/octet-stream;charset=ISO8859-1")).body(new InputStreamResource(is));
	}

	public String getFileName(String isMsService) {
		String fileName = VXMES;
		if ("Mis".equals(isMsService)) {
			fileName = SUPPLANT;
		}
		return fileName;
	}

	@RequestMapping(value = "/ec/module/downBatch-zip")
	public ResponseEntity downloadBatchZip(String isMsService) throws Exception {
		String fileName = getFileName(isMsService);
		InputStream is = new FileInputStream(new File(workspacePath + File.separator + "generate" + File.separator + fileName));
		HttpHeaders headers = new HttpHeaders();
		headers.setContentDispositionFormData("attachment", new String(fileName.getBytes("UTF-8"),"ISO-8859-1"));
		return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("application/octet-stream;charset=ISO8859-1")).body(new InputStreamResource(is));
	}

	@RequestMapping(value = "/ec/module/upload")
	public String upload(String isMsService, Module module) throws Exception {
		return "module/upload";
	}

	@RequestMapping(value = "/ec/module/uploadBatch")
	public String uploadBatch() throws Exception {
		return "module/uploadBatch";
	}

	@RequestMapping(value = "/ec/module/uploadBatchModules")
	public String uploadBatchModules(ModelMap map, @RequestParam("receiveFile") MultipartFile multipartFile){
		String isMsService =getRequest().getParameter("isMsService");
		//这里开始把temp目录下的文件转移到bap的workspace的目录下
		Date startDate=new Date();
		log.info(InternationalResource.get("ec.module.uploadBatch.StartCopyUpload")+new Date());
		String filePath = "up" + System.currentTimeMillis();
		String uploaded = PropertyHolder.get().getGeneratePath() + File.separator + "uploaded" + File.separator + filePath;
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
		return "module/uploadBatchModules";
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


	@ResponseBody
	@RequestMapping(value = "/ec/module/uploadBatchModuleQuery")
	public Page<UploadInfoVO> uploadBatchModuleQuery(String filePath, String isMsService){
		Date startDate=new Date();
		log.info("Starting time of batch upload extraction module information:"+new Date());
		String uploaded = PropertyHolder.get().getGeneratePath() + File.separator + "uploaded" + File.separator + filePath;
		Map<String, Object> returnMag = getUploadInfoPage(uploaded, isMsService);
		String message = (String)returnMag.get("message");
		StringBuffer allMessage = new StringBuffer();
		Page<UploadInfo> uploadInfos = (Page<UploadInfo>)returnMag.get("uploadInfoPage");
		if(uploadInfos.getResult().size()>0){
			uploadInfos.getResult().get(0).setErrorMsg(message);
		}
		Date date1=new Date();
		log.info("It takes time to upload and extract module information in batches:"+((date1.getTime()-startDate.getTime())/1000)+"秒");
		return uploadInfoWrapper.e2vPage(uploadInfos);
	}
	/**
	 *
	* @Title:
	* @Description: TODO(这里传输的数据不存在重复模块，先把前台传来的json数据封装到list对象中，再通过set对依赖存在的状款进行查询)
	* @param @return
	* @param @throws Exception    参数
	* @return String    返回类型
	* @throws
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/module/uploadBatchBeforeSubmit")
	public ResponseMsg uploadBatchBeforeSubmit(String batchUploadJson, String isMsService) throws Exception {
		String str = batchUploadJson;
		JSONArray  dataJson= null;
		dataJson=new JSONArray(str);
		Set<String> moduleSet = new HashSet<String>();
		Set<String> moduleRelationsSet = new HashSet<String>();
		Set<String> moduleReferencesSet = new HashSet<String>();
		StringBuffer errorMessage = new StringBuffer();
		Map<String, String> moduleMap = new HashMap<String, String>();//存放所有上传模块的国际化中文
		Map<String, String> moduleToRelationMap = new HashMap<String, String>();//存放所有模块对应的依赖模块
		Map<String, String> relationChMap = new HashMap<String, String>();//存放所有依赖模块的国际化中文
		Map<String, String> module2ReferencesMap = new HashMap<String, String>();	//存放引用模块
		List<Module> modules = null;
		List<String> relationList = new ArrayList<String>();
		if(dataJson!=null&&dataJson.length()>0)  {
			for (int i = 0; i < dataJson.length(); i++) {
				relationList.clear();
				JSONObject o = dataJson.getJSONObject(i);
				String moduleCode = o.getString("moduleCode") != null ? o.getString("moduleCode") : "";
				String modueName = o.getString("moduleName") != null ? o.getString("moduleName") : "";
				String moduleRelations = o.getString("relations") != null ? o.getString("relations") : "";
				String moduleReferences = o.getString("references") != null ? o.getString("references") : "";
				String relationsInternation = o.getString("relationsInternation") != null ? o.getString("relationsInternation") : "";
				if(null != moduleCode && !"".equals(moduleCode)){
					moduleSet.add(moduleCode);
					moduleMap.put(moduleCode,modueName);
					moduleToRelationMap.put(moduleCode,moduleRelations);
					module2ReferencesMap.put(moduleCode, moduleReferences);
				}
				//新版本在下面的if中加入&&!"".equals(moduleRelationsCh)
				if(!"".equals(moduleRelations)){
					String[] relations = moduleRelations.split(",");
					String[] relationsInternations = relationsInternation.split(",");
					for(int index = 0 ; index<relations.length ; index++ ){
						if(!moduleRelationsSet.contains(relations[index])){
							moduleRelationsSet.add(relations[index]);
							if(!relationChMap.containsKey(relations[index])){
								relationChMap.put(relations[index],relationsInternations.length>index ? relationsInternations[index] : "");
							}
						}
					}
				}
				if (!StringUtils.isEmpty(moduleReferences)) {
					String[] references = moduleReferences.split(",");
					for (int index = 0; index < references.length; index++) {
						if (!moduleReferencesSet.contains(references[index])) {
							moduleReferencesSet.add(references[index]);
						}
					}
				}
				if ("Mis".equals(isMsService)) {
					relationList.add(moduleCode);
					if (relationList.size() > 1) {
						errorMessage.append(modueName + "(" + moduleCode + ")" + InternationalResource.get("ec.model.upload.MultipleServices") + "<br/>");
					}
				}
			}
		}
		moduleRelationsSet.removeAll(moduleSet);
		moduleReferencesSet.removeAll(moduleSet);
		Iterator<String> iteratorRelation = moduleRelationsSet.iterator();
		if (moduleRelationsSet.size() > 0) {
			modules = moduleService.findModule(new ArrayList<>(moduleRelationsSet));
		}
		while(iteratorRelation.hasNext()){
			String moduleCode = iteratorRelation.next();
			for (Module module : modules) {
				if(moduleCode.equals(module.getCode())){
					iteratorRelation.remove();
					break;
				}
			}
		}
		Iterator<String> iteratorReference = moduleReferencesSet.iterator();
		if (moduleReferencesSet.size() > 0) {
			modules = moduleService.findModule(new ArrayList<>(moduleReferencesSet));
		}
		while(iteratorReference.hasNext()){
			String moduleCode = iteratorReference.next();
			for (Module module : modules) {
				if(moduleCode.equals(module.getCode())){
					iteratorReference.remove();
					break;
				}
			}
		}
		for (Entry<String, String> relationCode : moduleToRelationMap.entrySet()) {
			StringBuffer messageModuleError = new StringBuffer();
			StringBuffer messageModuleErrorTemp = new StringBuffer();
			for(String rela:moduleRelationsSet){
				if(relationCode.getValue().indexOf(rela) > -1){
					if(messageModuleError.length() == 0 ){
						messageModuleError.append(moduleMap.get(relationCode.getKey())+InternationalResource.get("ec.module.uploadBatch.dependencyModule"));
					}
					//messageModuleError.append((!relationChMap.get(rela).equals("") ? relationChMap.get(rela) : rela )+"、");
					messageModuleErrorTemp.append("、"+(!relationChMap.get(rela).equals("") ? relationChMap.get(rela) : rela ));
				}
			}
			if(messageModuleError.length() > 0){
				messageModuleError.append(messageModuleErrorTemp.substring(1));
				messageModuleError.append(InternationalResource.get("ec.module.uploadBatch.uploadTheDependencyModuleFirst")+"<br/>");
			}
			errorMessage.append(messageModuleError);
		}
		for (Entry<String, String> referenceCode : module2ReferencesMap.entrySet()) {
			StringBuffer messageModuleError = new StringBuffer();
			StringBuffer messageModuleErrorTemp = new StringBuffer();
			boolean hasReferenceError=false;
			for(String refer:moduleReferencesSet){
				if(referenceCode.getValue().indexOf(refer) > -1){
					if(!hasReferenceError){
						messageModuleError.append(moduleMap.get(referenceCode.getKey())+InternationalResource.get("ec.view.referenceModule"));
						hasReferenceError = true;
					}
					//messageModuleError.append((!relationChMap.get(rela).equals("") ? relationChMap.get(rela) : rela )+"、");
					messageModuleErrorTemp.append("、"+ refer );
				}
			}
			if(messageModuleError.length() > 0){
				messageModuleError.append(messageModuleErrorTemp.substring(1));
				messageModuleError.append(InternationalResource.get("ec.module.uploadBatch.uploadReferenceModuleFirst")+"<br/>");
			}
   			errorMessage.append(messageModuleError);
		}
		ResponseMsg response = new ResponseMsg(true);
		ObjectMapper mapper = new ObjectMapper();
 		if(!"".equals(errorMessage.toString())){
			response.setExceptionMsg(errorMessage.toString());
			return response;
		}
 		uploadManager.clearMap();
		if(uploadManager.getCurrentTasks().size()>0){
			UploadInfo upload =uploadManager.getCurrentTasks().get(0);
			String uploadUser = upload!=null ? (upload.getUploadStaff()!=null ? upload.getUploadStaff().getUserName() : null) : null;
			errorMessage.append(uploadUser + InternationalResource.get("ec.module.uploadBatch.waitWhileUploading")+"<br/>");
			response.setExceptionMsg(errorMessage.toString());
			return response;
		}
//		CopyOnWriteArrayList<DeploymentTask> currentTasks = manager.getCurrentTasks();
//		if(null == currentTasks || currentTasks.size() > 0){
//			String deployUser = currentTasks.get(0) != null ? (currentTasks.get(0)!=null ? currentTasks.get(0).getDeployUser() : "") : "";
//			errorMessage.append(deployUser + InternationalResource.get("ec.module.uploadBatch.waitWhileDeploy")+"<br/>");
//		}
		response.setExceptionMsg(errorMessage.toString());
		return response;
	}

//	@Autowired
	private ModuleDeploymentManager manager;
	@Autowired
	private UploadInfoManager uploadManager;
	@Autowired
	private UploadInfoBatchService uploadInfoBatchService;
//	@Autowired
//	private DeployInfoBatchService deployInfoBatchService;
	/**
	 *
	* @Title: uploadBatchProcess
	* @Description: TODO(这里传输的数据不存在重复模块，先把前台传来的json数据封装到list对象中，再通过set对依赖存在的状款进行查询)
	* @param @return
	* @param @throws Exception    参数
	* @return String    返回类型
	* @throws
	 */
	@RequestMapping(value = "/ec/module/uploadBatchProcess")
	public String uploadBatchProcess(ModelMap map, String batchUploadJson, String filePath) throws Exception {
		String str = batchUploadJson;
		JSONArray  dataJson= null;
		dataJson=new JSONArray(str);
		Set<String> moduleSet = new HashSet<String>();
		Set<String> moduleRelationsSet = new HashSet<String>();
		List<UploadInfo> uploadInfos  = new ArrayList<UploadInfo>();
		List<UploadInfo> firstimportUploadInfos  = new ArrayList<UploadInfo>();
		StringBuffer uploadBaptchUserModuleMessageSB  = new StringBuffer();//组织正在发布的信息
		StringBuffer uploadBaptchDesSB  = new StringBuffer();//组织批量表的发布信息
		UploadInfoBatch uploadInfoBatch = new UploadInfoBatch();
		uploadInfoBatch.setUploadStaff(getCurrentStaff());
		uploadInfoBatch.setUploadDate(new Date());
		int moduleLength = 0 ;
		if(dataJson!=null&&dataJson.length()>0)  {
			for (int i = 0; i < dataJson.length(); i++) {
			   UploadInfo up = new UploadInfo();
			   JSONObject o = dataJson.getJSONObject(i);
			   up.setModuleCode(o.getString("moduleCode") != null ? o.getString("moduleCode") : "");
			   moduleSet.add(o.getString("moduleCode") != null ? o.getString("moduleCode") : "");
			   up.setModuleName(o.getString("moduleName") != null ? o.getString("moduleName") : "");
			   uploadBaptchUserModuleMessageSB.append(",");
			   uploadBaptchUserModuleMessageSB.append(o.getString("moduleName") != null ? o.getString("moduleName") : "");
			   uploadBaptchDesSB.append(",");
			   uploadBaptchDesSB.append(o.getString("moduleName") != null ? o.getString("moduleName") : "");
			   moduleLength ++;
			   up.setIsMetadata(o.getString("isMetadata")!="" ? Boolean.valueOf(o.getString("isMetadata")) : false);
			   up.setIsCustomcode(o.getString("isCustomcode")!="" ? Boolean.valueOf(o.getString("isCustomcode")) : false);
			   up.setIsFlow(o.getString("isFlow")!="" ? Boolean.valueOf(o.getString("isFlow")) : false);
			   up.setIsImportTemplate(o.getString("isImportTemplate")!="" ? Boolean.valueOf(o.getString("isImportTemplate")) : false);
			   up.setIsUploadschedulerJob((o.getString("isUploadschedulerJob")!="" ? Boolean.valueOf(o.getString("isUploadschedulerJob")) : false));
			   up.setIsFirstImport(o.getString("isFirstImport")!="" ? Boolean.valueOf(o.getString("isFirstImport")) : false);
			   up.setUploadStaff(getCurrentStaff());
			   up.setIsFilterMethod(false);
			   String relationsAndReferences=null;
			   if(null==o.getString("relations")){
				   relationsAndReferences=o.getString("references");
			   }else{
				   relationsAndReferences=o.getString("relations");
					if(null!=o.getString("references")){
						relationsAndReferences+=","+o.getString("references");
					}
			   }
			   up.setRelations(relationsAndReferences);
			   // 添加引用模块
//			   up.setReferences(o.getString("references"));
			   up.setUploadInfoBatch(uploadInfoBatch);
			   if(filePath != null && !"".equals(filePath)){
				   up.setUploadFileName(filePath + File.separator + (o.getString("uploadFileName") != null ? o.getString("uploadFileName") : ""));
			   }
			   up.setOldVersion(o.getString("oldVersion") != null ? o.getString("oldVersion") : "");
			  /* if(up.getIsFirstImport()){
				   firstimportUploadInfos.add(up);
			   }else{
				   uploadInfos.add(up);
			   }*/
			   firstimportUploadInfos.add(up);
			}
		}
		//这时候已经对上载的包进行分类，分为第一次上载list和已经存在的模块list
		List<UploadInfo> sortUploadInfos = moduleService.sortUploads(firstimportUploadInfos,uploadInfos);
		uploadInfoBatch.setDescribe(uploadBaptchDesSB.toString().substring(1));
		uploadInfoBatchService.save(uploadInfoBatch);
		uploadManager.setTaskProggressSize(Long.valueOf(sortUploadInfos.size()));
		String uploadBaptchUser = getCurrentUser().getName();
		//本次上载模块 + uploadBaptchUserModuleMessageSB.toString().substring(1);
		String uploadBaptchUserModuleMessage = InternationalResource.get("ec.model.upload.uploadmodule") + uploadBaptchUserModuleMessageSB.toString().substring(1);
		//"共"+moduleLength+"个模块";
		if(moduleLength > 1){
			uploadBaptchUserModuleMessage += InternationalResource.get("foundation.inter.bcfbmkgong") + " " + moduleLength + " " + InternationalResource.get("ec.model.upload.modules");
		}else{
			uploadBaptchUserModuleMessage += InternationalResource.get("foundation.inter.bcfbmkgong") + " " + moduleLength + " " + InternationalResource.get("ec.model.upload.module");
		}
		//"剩余"+sortUploadInfos.size()+"分钟";
		String uploadBaptchUserLastTime = InternationalResource.get("ec.model.upload.remain") + " " + sortUploadInfos.size()+ " " + InternationalResource.get("ec.entity.wf.minute");
		String message = "";
		uploadInfoBatch.setModuleSize(sortUploadInfos.size());
		EcUtils.uploadTask.put("uploadTask", uploadInfoBatch.getId().toString());
		EcUtils.uploadTask.put("uploadTaskSize", Integer.valueOf(sortUploadInfos.size()).toString());
		EcUtils.uploadTask.put("uploadTaskState","success");
		EcUtils.uploadTask.put("uploadUser", uploadBaptchUser!=null?uploadBaptchUser:"");
		EcUtils.uploadTask.put("uploadBaptchUserModuleMessage", uploadBaptchUserModuleMessage);
		EcUtils.uploadTask.put("localLanguage", getUserLanguage());
		EcUtils.uploadTaskBatch.put("uploadTaskBatch", uploadInfoBatch);
		String uploadTaskId = EcUtils.uploadTask.get("uploadTask");
		MDC.put("uploadTask",EcUtils.uploadTask.get("uploadTask"));
		EcUtils.uploadTask.put("curCompany",getCurrentCompanyId()+"");
		EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.putcompany"),getCurrentCompanyId()+"");
		for(UploadInfo uploadInfo : sortUploadInfos){
			log.info(InternationalResource.get("ec.model.upload.modulename")+uploadInfo.getModuleCode()+InternationalResource.get("ec.model.upload.firstupload")+uploadInfo.getIsFirstImport()+InternationalResource.get("ec.model.upload.level")+uploadInfo.getLevel()+InternationalResource.get("ec.model.upload.entity"),uploadInfo.getEntityNum(),uploadInfo.getRelations());
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.modulename")+uploadInfo.getModuleCode()+InternationalResource.get("ec.model.upload.firstupload")+uploadInfo.getIsFirstImport()+InternationalResource.get("ec.model.upload.level")+uploadInfo.getLevel()+InternationalResource.get("ec.model.upload.entity"),uploadInfo.getEntityNum(),uploadInfo.getRelations());
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.metadata")+uploadInfo.getIsMetadata()+InternationalResource.get("ec.model.upload.customcode")+uploadInfo.getIsCustomcode()+InternationalResource.get("ec.model.upload.workflow")+uploadInfo.getIsFlow()+InternationalResource.get("ec.model.upload.excelimport")+uploadInfo.getIsImportTemplate());
		}
		uploadManager.batchPush(sortUploadInfos);
		map.addAttribute("uploadBaptchUser", uploadBaptchUser);
		map.addAttribute("uploadBaptchUserModuleMessage", uploadBaptchUserModuleMessage);
		map.addAttribute("uploadBaptchUserLastTime", uploadBaptchUserLastTime);
		map.addAttribute("uploadTaskId", uploadTaskId);
		return "module/uploadBatchProcess";
	}

	/**
	 *
	* @Title: uploadBatchProcessUsed
	* @Description: TODO(点击批量上载按钮的时候如果有人正在发布，直接显示日志页面)
	* @param @return
	* @param @throws Exception    参数
	* @return String    返回类型
	* @throws
	 */
	@RequestMapping(value = "/ec/module/uploadBatchProcessUsed")
	public String uploadBatchProcessUsed(ModelMap map) throws Exception {
		String uploadBaptchUser = EcUtils.uploadTask.get("uploadUser");
		String uploadBaptchUserModuleMessage = EcUtils.uploadTask.get("uploadBaptchUserModuleMessage");
		String uploadTaskId = EcUtils.uploadTask.get("uploadTask");
		/*uploadBaptchUser = "zhushizhang";
		uploadBaptchUserModuleMessage = "zhushizhang";
		uploadTaskId = "uploadTaskId";*/
		map.addAttribute("uploadBaptchUser", uploadBaptchUser);
		map.addAttribute("uploadBaptchUserModuleMessage", uploadBaptchUserModuleMessage);
		map.addAttribute("uploadTaskId", uploadTaskId);
		return "module/uploadBatchProcess";
	}

	/**
	 *
	* @Title: uploadBatchProcess
	* @Description: TODO(这里传输的数据不存在重复模块，先把前台传来的json数据封装到list对象中，再通过set对依赖存在的状款进行查询)
	* @param @return
	* @param @throws Exception    参数
	* @return String    返回类型
	* @throws
	 */
	@RequestMapping(value = "/ec/module/uploadBatchCancel")
	public void uploadBatchCancel() throws Exception {
		uploadManager.cancel(1);
	}

	@RequestMapping(value = "/ec/module/upload-down-log")
	public ResponseEntity downloadLog(String uploadTaskId) throws Exception {
		String fileName = getDownloadUploadFileName(uploadTaskId);
		InputStream is = new FileInputStream(uploadManager.getLogfile(fileName));
		HttpHeaders headers = new HttpHeaders();
		headers.setContentDispositionFormData("attachment", new String(fileName.getBytes("UTF-8"),"ISO-8859-1"));
		return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("application/octet-stream;charset=ISO8859-1")).body(new InputStreamResource(is));
	}

	public String getDownloadUploadFileName(String uploadTaskId) {
		String downloadUploadFileName = null;
		if (null != uploadTaskId) {
			downloadUploadFileName = uploadTaskId.toString() + "-full.log";
		}
		return downloadUploadFileName;
	}

	@RequestMapping(value = "/ec/module/tansferBatch")
	public String tansferBatch(File receiveFile){
		//这里开始把temp目录下的文件转移到bap的workspace的目录下
		Date startDate=new Date();
		log.info(InternationalResource.get("ec.module.uploadBatch.StartCopyUpload")+"："+new Date());
		String filePath = "up" + System.currentTimeMillis();
		String uploaded = PropertyHolder.get().getGeneratePath() + File.separator + "uploaded" + File.separator + filePath;
		File uploadedFile = new File(uploaded);
		if (!uploadedFile.exists()) {
			uploadedFile.mkdirs();
		}
		try {
			FileUtils.copyFileToDirectory(receiveFile, uploadedFile);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		Date date1=new Date();
		log.info(InternationalResource.get("ec.module.uploadBatch.StartCopyUploadTime")+"："+(date1.getTime()-startDate.getTime()));
		return "module/tansferBatch";
	}



	@RequestMapping(value = "/ec/module/migrate")
	public String migrate(){
		return "module/migrate";
	}


	@RequestMapping(value = "/ec/module/migrateupload")
	public String migrateupload(String selectedEntities, String filePath, Module module, boolean upload_filter,
								boolean uploadFile, boolean upload_metadata, boolean upload_customcode, boolean upload_flow) throws Exception {
		getResponse().setContentType("text/html");
//		getResponse().setCharacterEncoding("UTF-8");
		PrintWriter out = getResponse().getWriter();
		out.write(HTML_HEAD);

		String[] entites = selectedEntities.split(",");

		String[] filepath = filePath.split(",");
		String path = filepath[0];
		String moduleCode = filepath[1];

		if (module != null && null != module.getCode() && module.getCode().length() > 0 && !module.getCode().equals("undefined")
				&& !module.getCode().equals("-1")) {
			module = moduleService.getModule(module.getCode(), true);
		}
		String deploySuccessMsg = "<script type=\"text/javascript\">try{parent.ec.module.refreshModuleTree();parent.$( '#frame3_preStep_Btn' ).attr('canclick','true').removeClass( 'cui-btn-gray' );}catch(e){}</script>";
		if (null == module || module.getCode().equals("undefined") || module.getCode().equals("-1")) {
			// 新导入模块
			out.write(InternationalResource.get("ec.module.generate.uploaded") + "<br />"
					+ InternationalResource.get("ec.module.generate.unZip") + "<br />");
			out.flush();
			try {
				Boolean run = EcUtils.migMap.get(moduleCode);
				if(run!=null && run){
					out.write("<span style=\"color:red\">"+"当前模块正在进行迁移，请稍后再试！"+"</span>");
					out.flush();
					return null;
				}else{
					EcUtils.migMap.put(moduleCode, Boolean.TRUE);
				}
				generateService.executeMigrateUpload(entites, upload_filter,path,uploadFile);//TODO handle upload
			}finally{
				EcUtils.migMap.remove(moduleCode);
			}
			out.write(InternationalResource.get("ec.module.generate.unZiped") + "<br />");
			out.write(deploySuccessMsg);
			out.flush();
		} else {
			out.write(InternationalResource.get("ec.module.generate.uploaded") + "<br />"
					+ InternationalResource.get("ec.module.generate.unZip") + "<br />");
			out.flush();
			if (null != module)
				try {
					generateService.executeMigrateUpload(module,entites, upload_metadata, upload_customcode, upload_flow, upload_filter,path,uploadFile);
				}finally{
					EcUtils.migMap.remove(module.getCode());
				}
			if(module != null)
				deploySuccessMsg = "<script type=\"text/javascript\">try{parent.datatable_ec_module_entity_datatable.setRequestDataUrl('/ec/entity/list?module.code="+module.getCode()+"');parent.$( '#frame3_preStep_Btn' ).attr('canclick','true').removeClass( 'cui-btn-gray' );}catch(e){}</script>";
			out.write(InternationalResource.get("ec.module.generate.unZiped") + "<br />");
			out.write(deploySuccessMsg);
			out.flush();

		}
		File upFile = new File(PropertyHolder.get().getGeneratePath() + File.separator + "uploaded" + File.separator + path);
		if(upFile.exists() && upFile.isDirectory()){
			FileUtils.deleteDirectory(upFile);
		}
		return null;
	}

	@RequestMapping(value = "/ec/module/listEntity")
	public String listEntity(ModelMap map, Module module, File receiveFile){
		String moduleCode = "";
		if(module!=null&&!module.getCode().equals("-1")&&!module.getCode().equals("undefined")){
			moduleCode = module.getCode();
		}else{
			moduleCode = "migrateModule";
		}
		String errorMsg = null;
		String filePath = null;
		if (null != receiveFile) {
			try {
				errorMsg = moduleService.saveFile(receiveFile,moduleCode);
				if(errorMsg!=null && !errorMsg.startsWith("up")){
					errorMsg = InternationalResource.get(errorMsg);
					log.error(errorMsg);
				}else if(errorMsg!=null && errorMsg.startsWith("up")){
					filePath = errorMsg;
					errorMsg = null;
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				errorMsg = e.getMessage();
			}
		}
		map.addAttribute("errorMsg", errorMsg);
		map.addAttribute("filePath", filePath);
		return "module/tansfer";
	}

	@RequestMapping(value = "/ec/module/showEntity")
	public String showEntity(){
		return "module/entityListFramePage";
	}

	@ResponseBody
	@RequestMapping(value = "/ec/module/receive2")
	public Page<EntityVO> receive2(Module module, String filePath) throws Exception {
		Page<Entity> entities = new Page<Entity>(1,65535);
		if (module != null && null != module.getCode() && module.getCode().length() > 0 && !module.getCode().equals("undefined")
				&& !module.getCode().equals("-1")) {
		}else{
			module = null;
		}
		try{
			String[] filepath = filePath.split(",");
			String path = filepath[0];
			List<Entity> list_en = moduleService.listEntity(module,path);
			if(list_en!=null&&list_en.size()>0){
				entities.setResult(list_en);
				//TODO
			}
		}catch(Exception e1){
			log.error(e1.getMessage(), e1);
			entities.setResult(null);
		}

        return entityWrapper.e2vPage(entities);
	}


	/**
	 * 模型参照
	 *
	 * @return
	 */
	@RequestMapping(value = "/ec/module/ref")
	public String ref(ModelMap map) throws Exception {
		String isMsService =getRequest().getParameter("isMsService");
		map.addAttribute("isMsService", isMsService);
		return "module/ref";
	}

	/**
	 * 引用模块参照
	 *
	 * @return
	 */
	@RequestMapping(value = "/ec/module/moduleReference")
	public String moduleReference(ModelMap map) throws Exception {
		String isMsService =getRequest().getParameter("isMsService");
		map.addAttribute("isMsService", isMsService);
		return "module/moduleReference";
	}

	/**
	 * 模型参照2
	 *
	 * @return
	 */
	@RequestMapping(value = "/ec/module/ref2")
	public String ref2() throws Exception {
		return "module/ref2";
	}

	@ResponseBody
	@RequestMapping(value = "/ec/module/refQuery")
	public Page<ModuleVO> refQuery(Module module) throws Exception {
		String isMsService =getRequest().getParameter("isMsService");
		String serviceType =getRequest().getParameter("isMsService");
		List<Module> modules =new ArrayList<Module>();
		boolean point =false;
		if("".equals(serviceType)){
			point=true;
		}
		Page<Module> page = new Page<Module>();
		if(point){
			modules = moduleService.findAllModules();
		}
		else{
			modules = moduleService.findAllMsModules("proto");
		}
		if(module!=null && modules.contains(module)){
			modules.remove(module);
		}
		Module sysbaseModule = new Module();
		sysbaseModule.setCode("sysbase_1.0");		//选择依赖模块的时候，过滤掉系统基础模块
		modules.remove(sysbaseModule);
		page.setResult(modules);
		return moduleWrapper.e2vPage(page);
	}

	@ResponseBody
	@RequestMapping(value = "/ec/module/migQuery")
	public Page<ModuleVO> migQuery(Module module) throws Exception {
		Page<Module> page = new Page<Module>();
		Module sysModule = new Module();
		sysModule.setCode("sysbase_1.0");
		List<Module> modules = moduleService.findAllModules();
		if(module!=null && modules.contains(module)){
			modules.remove(module);
		}
		modules.remove(sysModule);
		page.setResult(modules);
		return moduleWrapper.e2vPage(page);
	}

	@ResponseBody
	@RequestMapping(value = "/ec/module/checkModifyState")
	public ResponseMsg checkModifyState(String moduleCodes) throws Exception {
		ResponseMsg response = new ResponseMsg(false);
		String message = moduleService.checkModifyModulesState(moduleCodes);
		if(null != message && !"".equals(message)){
			response.setSuccess(true);
			response.setExceptionMsg(message);
		}
		return response;
	}

	@ResponseBody
	@RequestMapping(value = "/ec/module/getUploadState")
	public ResponseMsg getUploadState() throws Exception {
		ResponseMsg response = new ResponseMsg(false);
		if(uploadManager.getCurrentTasks().size()>0){
			UploadInfo upload =uploadManager.getCurrentTasks().get(0);
			String uploadUser = upload!=null ? (upload.getUploadStaff()!=null ? upload.getUploadStaff().getUserName() : null) : null;
			response.setSuccess(true);
			response.setExceptionMsg(uploadUser + "正在上载,请稍候<br/>");
		}
		return response;
	}

	class DeployWorker implements Runnable {
		private CountDownLatch downLatch;
		private Writer writer;

		public DeployWorker(CountDownLatch downLatch, Writer writer) {
			this.downLatch = downLatch;
			this.writer = writer;
		}

		@Override
		public void run() {
			while (downLatch.getCount() > 0) {
				try {
					writer.write(".");
					writer.flush();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
				} catch (IOException e) {
				}
			}
		}
	}
	@Autowired
	private UploadInfoService uploadInfoService;


	/**
	 * 进入模块管理主框架
	 *
	 * @return
	 */
	@RequestMapping(value = "/ec/module/history")
	public String history() throws Exception {
		return "module/history";
	}

	/**
	 * 获取上载记录
	 *
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/module/upload/list-query")
	public Page<UploadInfoVO> uploadInfos_query(Integer pageNo, Integer pageSize) throws Exception {
		Page<UploadInfo> uploadPage = new Page<UploadInfo>(pageNo,pageSize);
		StringBuffer condition = new StringBuffer(" where 1=1 ");
		if(null != getRequest().getParameter("uploadInfo.uploadInfoBatch.id") && getRequest().getParameter("uploadInfo.uploadInfoBatch.id").trim().length() > 0){
			condition.append("and uploadInfo.uploadInfoBatch.id = "+getRequest().getParameter("uploadInfo.uploadInfoBatch.id")+"");
		}
		DecimalFormat format = new DecimalFormat("0");
		uploadPage = uploadInfoService.findUploadInfoPage(uploadPage,condition.toString());
		Iterator<UploadInfo> iterator = uploadPage.getResult().iterator();
		while(iterator.hasNext()){
			UploadInfo uploadInfoBatch = iterator.next();
			if(uploadInfoBatch.getUploadState() != null ){
				uploadInfoBatch.setUploadState(uploadInfoBatch.getUploadState().equals("成功") ? InternationalResource.get("ec.model.upload.modulesuccess") : InternationalResource.get("ec.model.upload.modulefialed"));
			}else{
				uploadInfoBatch.setUploadState(InternationalResource.get("ec.model.upload.modulefialed"));
			}
			if(null != uploadInfoBatch.getTotalTime()){
				uploadInfoBatch.setTotalTime(format.format(new BigDecimal(uploadInfoBatch.getTotalTime())));
			}
		}
		return uploadInfoWrapper.e2vPage(uploadPage);
	}
	/**
	 * 进入上载详细记录页面
	 *
	 * @return
	 */
	@RequestMapping(value = "/ec/module/uploadInfDetial")
	public String uploadInfDetial(ModelMap map, Long uploadBatchId) throws Exception {
		map.put("uploadBatchId", uploadBatchId);
		return "module/uploadRecord";
	}

	/**
	 * 获取上载记录
	 *
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/module/uploadInfoBatch/list-query")
	public Page<UploadInfoBatchVO> uploadInfoBatchs_query( HttpServletRequest request) throws Exception {
		Integer pageNo=1;
		if(null!=request.getParameter("page.pageNo")){
			pageNo=Integer.valueOf(request.getParameter("page.pageNo"));
		}
		Integer pageSize=20;
		if(null!=request.getParameter("page.pageSize")){
			pageSize=Integer.valueOf(request.getParameter("page.pageSize"));
		}
		DecimalFormat format = new DecimalFormat("0");
		Page<UploadInfoBatch> uploadInfoBatchs = new Page<UploadInfoBatch>(pageNo,pageSize);
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(UploadInfoBatch.class);
		//HttpServletRequest request = getRequest();
		if(null != request.getParameter("uploadInfoBatch.uploadStaff.id") && request.getParameter("uploadInfoBatch.uploadStaff.id").trim().length() > 0){
			detachedCriteria.add(Restrictions.eq("uploadStaff.id", Long.valueOf(request.getParameter("uploadInfoBatch.uploadStaff.id"))));
		}
		if(null != request.getParameter("uploadInfoBatch.uploadStaff.name") && request.getParameter("uploadInfoBatch.uploadStaff.name").trim().length() > 0){
			detachedCriteria.createAlias("uploadStaff", "s");
			detachedCriteria.add(Restrictions.like("s.name", request.getParameter("uploadInfoBatch.uploadStaff.name"),MatchMode.ANYWHERE).ignoreCase());
		}
		if(null != request.getParameter("uploadInfoBatch.moduleName") && request.getParameter("uploadInfoBatch.moduleName").trim().length() > 0){
			detachedCriteria.add(Restrictions.like("describe","%"+request.getParameter("uploadInfoBatch.moduleName")+"%").ignoreCase());
		}
		detachedCriteria.add(Restrictions.isNotNull("uploadDate"));
		uploadInfoBatchs = uploadInfoBatchService.findUploadInfoBatchPage(uploadInfoBatchs, detachedCriteria);
		Iterator<UploadInfoBatch> iterator = uploadInfoBatchs.getResult().iterator();
		while(iterator.hasNext()){
			UploadInfoBatch uploadInfoBatch = iterator.next();
			if(uploadInfoBatch.getUploadState() != null ){
				uploadInfoBatch.setUploadState(uploadInfoBatch.getUploadState().equals("success") ? InternationalResource.get("ec.model.upload.modulesuccess") : InternationalResource.get("ec.model.upload.modulefialed"));
			}else{
				uploadInfoBatch.setUploadState(InternationalResource.get("ec.model.upload.modulefialed"));
			}
			if(null != uploadInfoBatch.getTotalTime()){
				uploadInfoBatch.setTotalTime(format.format(new BigDecimal(uploadInfoBatch.getTotalTime())));
			}
		}
		return uploadInfoBatchWrapper.e2vPage(uploadInfoBatchs);
	}

	/**
	 * 进入批量上载记录主框架
	 *
	 * @return
	 */
	@RequestMapping(value = "/ec/module/uploadBatchHistory")
	public String uploadBatchHistory() throws Exception {
		return "module/uploadBatchDetail";
	}
	/**
	 * 获取批量上载记录
	 *
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/module/uploadBatch/list-query")
	public Page<UploadInfoVO> uploadInfoBatch_query(Integer pageNo, Integer pageSize) throws Exception {
		Page<UploadInfo> uploadPage = new Page<UploadInfo>(pageNo,pageSize);
		StringBuffer condition = new StringBuffer(" where 1=1 ");
		uploadPage = uploadInfoService.findUploadInfoPage(uploadPage, condition.toString());
		return uploadInfoWrapper.e2vPage(uploadPage);
	}

	/**
	 * 获取批量发布记录
	 *
	 * @return
	 */
//	@ResponseBody
//	@RequestMapping(value = "/ec/module/deployInfoBatch/list-query")
//	public Page<DeployInfoBatch> deployInfoBatchs_query() throws Exception {
//		Page<DeployInfoBatch> deployBatchPage = new Page<DeployInfoBatch>(20);
//		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(DeployInfoBatch.class);
//		HttpServletRequest request = getRequest();
//		if(null != request.getParameter("deployInfoBatch.deployStaff.id") && request.getParameter("deployInfoBatch.deployStaff.id").trim().length() > 0){
//			detachedCriteria.add(Restrictions.eq("deployStaff.id", Long.valueOf(request.getParameter("deployInfoBatch.deployStaff.id"))));
//		}
//		if(null != request.getParameter("deployInfoBatch.deployStaff.name") && request.getParameter("deployInfoBatch.deployStaff.name").trim().length() > 0){
//			detachedCriteria.createAlias("deployStaff", "s");
//			detachedCriteria.add(Restrictions.like("s.name", request.getParameter("deployInfoBatch.deployStaff.name"),MatchMode.ANYWHERE).ignoreCase());
//		}
//		if(null != request.getParameter("deployInfoBatch.moduleName") && request.getParameter("deployInfoBatch.moduleName").trim().length() > 0){
//			detachedCriteria.add(Restrictions.like("describe","%"+request.getParameter("deployInfoBatch.moduleName")+"%").ignoreCase());
//		}
//		deployBatchPage = deployInfoBatchService.findDeployInfoBatchPage(deployBatchPage, detachedCriteria);
//		if(deployBatchPage.getResult().size() == 0){
//
//		}
//		Iterator<DeployInfoBatch> iterator = deployBatchPage.getResult().iterator();
//		while(iterator.hasNext()){
//			DeployInfoBatch deployInfoBatch = iterator.next();
//			if(deployInfoBatch.getDeployState() != null ){
//				deployInfoBatch.setDeployState(deployInfoBatch.getDeployState().equals("FINISHED") ? InternationalResource.get("ec.model.upload.modulesuccess") : InternationalResource.get("ec.model.upload.modulefialed"));
//			}else{
//				deployInfoBatch.setDeployState(InternationalResource.get("ec.model.upload.modulefialed"));
//			}
//			deployInfoBatch.setTotalTime(deployInfoBatch.getTotalTime());
//		}
//		return deployBatchPage;
//	}

	/**
	 * 获取上载记录
	 *
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/module/upload/list-queryTest")
	public List<UploadInfoVO> uploadInfos_queryTest() throws Exception {
		StringBuffer condition = new StringBuffer(" where 1=1 ");
		condition.append("and uploadInfo.uploadInfoBatch.id = "+getRequest().getParameter("uploadInfo.uploadInfoBatch.id")+"");
		List<UploadInfo> list = uploadInfoService.findUploadInfo(condition.toString());
		return uploadInfoWrapper.e2vList(list);
	}
	/**
	 * 更新历史上载记录
	 *
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/module/upload/uploadinfoHistory")
	public Map uploadinfoHistory() throws Exception {
		Map<String, Object> responseMap = new HashMap<String, Object>();
		uploadInfoBatchService.updateHistoryData();
		responseMap.put("result","处理成功");
		return responseMap;
	}

	/**
	 * 更新历史发布记录
	 *
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/static/module/deploy/deployinfoHistory")
	public Map deployinfoHistory() throws Exception {
		Map<String, Object> responseMap = new HashMap<String, Object>();
//		deployInfoBatchService.updateHistoryData();
//		responseMap.put("result","处理成功");
		return responseMap;
	}

	@ResponseBody
	@RequestMapping(value = "/ec/module/uploadBatchHasTask")
	public ResponseMsg uploadBatchHasTask() throws Exception {
		StringBuffer errorMessage = new StringBuffer();
		ResponseMsg msg = new ResponseMsg(true);
		if(uploadManager.getCurrentTasks().size()>0){
			log.info("uploadManager.getCurrentTasks()长度为{}",uploadManager.getCurrentTasks().size());
			msg.setSuccess(false);
			msg.setData(1);//1代表上载
		}
//		CopyOnWriteArrayList<DeploymentTask> currentTasks = manager.getCurrentTasks();
//		if(null == currentTasks || currentTasks.size() > 0){
//			String deployUser = currentTasks.get(0) != null ? (currentTasks.get(0)!=null ? currentTasks.get(0).getDeployUser() : "") : "";
//			msg.setSuccess(false);
//			msg.setData(2);//2代表发布
//			msg.setExceptionMsg(deployUser+InternationalResource.get("ec.module.uploadBatch.waitWhileDeploy")+"！");
//		}
		return msg;
	}

	@ResponseBody
	@RequestMapping(value = "/ec/module/upload/progressiveLog")
	public UploadLog getProgressiveLog(long taskId, String progressiveType, long start) throws IOException {
		return uploadManager.getProgressiveLog(taskId, progressiveType, start);
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

	private static final String languagePathOld = "OSGI-INF";
	private static final String languagePathNew = "LANG-INF";

	public Map<String,Object> getUploadInfoPage(String uploaded, String type){
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
			Page<UploadInfo>  uploadInfoPage = new Page<UploadInfo>(1,1000);
			List<UploadInfo> result = new ArrayList<UploadInfo>();
			UploadInfo uploadInfo = new UploadInfo();
			uploadInfo.setModuleCode("");
			uploadInfo.setModuleName("");
			uploadInfo.setCategory("");
			uploadInfo.setOldVersion("");
			uploadInfo.setIsFirstImport(false);
			uploadInfo.setUploadState(InternationalResource.get("foundation.inter.failed"));
			uploadInfo.setRelations("");
			uploadInfo.setUploadFileName("");
			result.add(uploadInfo);
			uploadInfoPage.setResult(result);
			returnMsg.put("uploadInfoPage", uploadInfoPage);
			returnMsg.put("message", InternationalResource.get("ec.module.upload.fileTypeError"));
			return returnMsg;
		}
		//这里判断是否是单个模块包
		boolean isSingle = isSingleModule(bigZip);
		if(isSingle){
//			File newfile = new File(uploaded+File.separator+"temp.zip");//创建新名字的抽象文件
//			try {
//				FileUtils.copyFile(bigZip, newfile);
//			} catch (IOException e) {
//				log.error(e.getMessage(), e);
//			}
		}else{
			try{
				UnZipFile.unzip(bigZip, uploadFile);
			}catch(Exception e){
				Page<UploadInfo>  uploadInfoPage = new Page<UploadInfo>(1,1000);
				List<UploadInfo> result = new ArrayList<UploadInfo>();
				UploadInfo uploadInfo = new UploadInfo();
				uploadInfo.setModuleCode("");
				uploadInfo.setModuleName("");
				uploadInfo.setCategory("");
				uploadInfo.setOldVersion("");
				uploadInfo.setIsFirstImport(false);
				uploadInfo.setUploadState(InternationalResource.get("foundation.inter.failed"));
				uploadInfo.setRelations("");
				uploadInfo.setReferences("");
				uploadInfo.setUploadFileName("");
				result.add(uploadInfo);
				uploadInfoPage.setResult(result);
				returnMsg.put("uploadInfoPage", uploadInfoPage);
				returnMsg.put("message", InternationalResource.get("ec.module.upload.fileTypeError"));
				return returnMsg;
			}
		}
		Map<String, Map<String, Object>> mapProperties = new HashMap<String, Map<String, Object>>();
		//用来存放每个模块包中前3000行的国际化文件
		Map<String, Map<String, String>> mapL10Properties = new HashMap<String, Map<String, String>>();
		Map<String, Map<String, Object>> simpleProperties = new HashMap<String, Map<String, Object>>();
		Map<String, Map<String, String>> l10propertiesModuleMap=new HashMap<String, Map<String, String>>();
		Map<String, Map<String, String>> l10propertiesCategoryMap=new HashMap<String, Map<String, String>>();//模块分类
		try {
			if (uploadFile.isDirectory()) {
				boolean hasPom=false;//至少有一个zip包是正确的module包
				File[] fileLists = uploadFile.listFiles();
				if (fileLists.length > 0) {
					for (Integer listCount = 0; listCount < fileLists.length; listCount++) {
						receiveFile = fileLists[listCount];
						if(receiveFile.getName().endsWith(".zip")){
							Map<String, Object> properties = new HashMap<String, Object>();
							Map<String, String> l10properties = new HashMap<String, String>();
							Map<String, String> l10Usproperties = new HashMap<String, String>();
							Map<String, String> l10propertiesModule=new HashMap<String, String>();
							Map<String, String> l10propertiesCategory=new HashMap<String, String>();
							boolean isValidModule = false;
							ZipFile zf = new ZipFile(receiveFile);
							InputStream in = new BufferedInputStream(
									new FileInputStream(receiveFile));
							Charset gbk = Charset.forName("gbk");
							ZipInputStream zinSimple = new ZipInputStream(in, gbk);
							ZipEntry zeSimple;
							boolean ismoduleSimple = false;//用来标记是否是有生成module-simple.xml文件，没有的话按照老的方法兼容
							try {
								while ((zeSimple = zinSimple.getNextEntry()) != null) {
									if ("pom.xml".equals(zeSimple.getName())) {
										//用来校验是否是一个正确的module包
										isValidModule = true;
										hasPom=true;
									}
									if (zeSimple.getName()
											.indexOf(
													"service/src/main/resources/META-INF/bap/module-simple.xml") > -1) {
										ismoduleSimple = true;
										properties = getModuleProperties(zeSimple,zf,moduleRepeatSet,sbMessage);
										properties.put("ismoduleSimple",true);//用来标记是用module-simple的方式读取
										properties.put("fileName", receiveFile.getName());
									}
									continue;
								}
							} catch (Exception e) {
								log.error(e.getMessage(), e);
							} finally {
								if(null != zinSimple){
									zinSimple.close();
								}
							}

							if(ismoduleSimple && isValidModule){
								mapProperties.put(listCount.toString(), properties);
							}
							//如果模块包中不存在module-simple.xml就按照下面的逻辑进行读取
							if(!ismoduleSimple && isValidModule){
								ZipInputStream zin = null;
								ZipEntry ze;
								try {
									zin = new ZipInputStream(new BufferedInputStream(
											new FileInputStream(receiveFile)), gbk);

									while ((ze = zin.getNextEntry()) != null) {
										if (ze.getName()
												.indexOf(
														"service/src/main/resources/META-INF/bap/module.xml") > -1) {
											properties = getModuleProperties(ze,zf,moduleRepeatSet,sbMessage);
											properties.put("fileName", receiveFile.getName());
										}else if (ze.getName()
												.indexOf(
														"service/src/main/resources/" + languagePathOld + "/l10n/package_zh_CN.properties") > -1 || ze.getName()
												.indexOf(
														"service/src/main/resources/" + languagePathNew + "/l10n/package_zh_CN.properties") > -1) {
											// 由于有可能存在将老的bap模块上载到新的微服务模块配置中，新的微服务模块上载到老的配置中，所以国际化读取路径需要都读，否则校验提示会丢失国际化的信息
											l10properties = getL10Properties(ze,zf);
										}else if (ze.getName()
												.indexOf(
														"service/src/main/resources/" + languagePathOld + "/l10n/package_en_US.properties") > -1 || ze.getName()
												.indexOf(
														"service/src/main/resources/" + languagePathNew + "/l10n/package_en_US.properties") > -1) {
											l10Usproperties = getL10Properties(ze,zf);
										}else if (ze.getName()
												.indexOf(
														"service/src/main/resources/" + languagePathOld + "/l10n/reg_module.properties") > -1 || ze.getName()
												.indexOf(
														"service/src/main/resources/" + languagePathNew + "/l10n/reg_module.properties") > -1) {
											l10propertiesModule = getL10Properties(ze,zf);
										}else if (ze.getName()
												.indexOf(
														"service/src/main/resources/" + languagePathOld + "/l10n/reg_category.properties") > -1 || ze.getName()
												.indexOf(
														"service/src/main/resources/" + languagePathNew + "/l10n/reg_category.properties") > -1) {
											l10propertiesCategory = getL10Properties(ze,zf);
										}else if ("src/main/resources/META-INF/bap/module.xml".equals(ze.getName())) {
											properties = getModuleProperties(ze,zf,moduleRepeatSet,sbMessage);
											properties.put("fileName", receiveFile.getName());
										}else if (("src/main/resources/" + languagePathNew + "/l10n/package_zh_CN.properties").equals(ze.getName())) {
											l10properties = getL10Properties(ze,zf);
										}else if (("src/main/resources/" + languagePathNew + "/l10n/package_en_US.properties").equals(ze.getName())) {
											l10Usproperties = getL10Properties(ze,zf);}
										continue;
									}
								} finally {
									if(null != zin){
										zin.close();
									}
								}
								properties.put("ismoduleSimple",false);//用来标记是用module的方式读取(老的)
								mapProperties.put(listCount.toString(), properties);
								l10propertiesModuleMap.put(String.valueOf(properties.get("code")),l10propertiesModule);
								l10propertiesCategoryMap.put(String.valueOf(properties.get("code")),l10propertiesCategory);
//								if("en_US".equals(getCurrentLanguage())){
//									mapL10Properties.put(listCount.toString(), l10Usproperties);
//								}else{
									mapL10Properties.put(listCount.toString(), l10properties);
//								}

							}
						}else{
							receiveFile.delete();
						}
					}
				}
				if(!hasPom){
					sbMessage.append(InternationalResource.get("ec.model.upload.error")+"</br>");
				}
			}
		} catch (IOException e) {
			EcUtils.uploadFullLogger.info("uploadBatchException:"+e.getMessage());
			EcUtils.uploadLogger.info("uploadBatchException:"+e.getMessage());
		}
		//开发封装上载的信息类
		Page<UploadInfo>  uploadInfoPage = new Page<UploadInfo>(1,1000);
		List<UploadInfo> result = new ArrayList<UploadInfo>();
		//先获取当前版本中所有存在的模块
		List<Module> moduleList = new ArrayList<Module>();
		if (StringUtils.isEmpty(type)) {
			moduleList = moduleService.findAllModules();
		} else {
			// 微服务上载，查询微服务模块
			moduleList = moduleService.findAllMsModules("");
		}
		Set<String> moduleSet = new HashSet<String>();
		for(Module moduel : moduleList){
			moduleSet.add(moduel.getCode());
		}
		String errModuleCode = "";
		for(Entry<String, Map<String, Object>> entry : mapProperties.entrySet()){
			boolean ismoduleSimple = (boolean)entry.getValue().get("ismoduleSimple");
			String moduleCode = entry.getValue().get("code").toString();
			String moduleName = "";
			String category = "";
			if (ismoduleSimple) {//这里采用的是moduleSimple的方式进行读取
				Map i10Properties = (Map)entry.getValue().get("i10Properties");
				Map i10PropertiesDefault = (Map)i10Properties.get("zh_CN");
				moduleName = i10PropertiesDefault.get(entry.getValue().get("name")).toString();
			} else {//这里采用老的方式去处理
				//moduleName = mapL10Properties.get(entry.getKey()).get(entry.getValue().get("name"));
				if(null!=l10propertiesModuleMap.get(moduleCode)){
					moduleName=l10propertiesModuleMap.get(moduleCode).get(getUserLanguage());
				}
				if(null!=l10propertiesCategoryMap.get(moduleCode)){
					category=l10propertiesCategoryMap.get(moduleCode).get(getUserLanguage());
				}
			}
			if (org.apache.commons.lang3.StringUtils.isEmpty(moduleName)) {
				moduleName = entry.getValue().get("name").toString();
			}
			if (org.apache.commons.lang3.StringUtils.isEmpty(category)) {
				category = entry.getValue().get("category").toString();
			}
			// 增加微服务模块判断，微服务配置只能上载微服务模块，老的实体配置只能上载老的bap模块
			if ((StringUtils.isEmpty(type) && entry.getValue().get("type") != null) || (!StringUtils.isEmpty(type) && entry.getValue().get("type") == null)){
				errModuleCode += moduleName + "(" + entry.getValue().get("code").toString() + "),";
			}
			UploadInfo uploadInfo = new UploadInfo();
			if(moduleCode!= null){
				Boolean isFirstImport = false;
				if(moduleSet.contains(moduleCode)){
					Module module = moduleService.getModule(moduleCode);
					uploadInfo.setCurVersion(module.getProjectVersion());
				}else{
					isFirstImport = true;
				}
				uploadInfo.setModuleCode(moduleCode != null ? moduleCode : "");
				uploadInfo.setModuleName(moduleName);
				uploadInfo.setCategory(category);
				uploadInfo.setOldVersion(entry.getValue().get("projectVersion")!=null ? entry.getValue().get("projectVersion").toString() : "");
				uploadInfo.setIsFirstImport(isFirstImport);
				uploadInfo.setUploadState(InternationalResource.get("foundation.inter.success"));
				uploadInfo.setRelations(entry.getValue().get("relations")!=null ? entry.getValue().get("relations").toString() : "");
				uploadInfo.setRelationsInternation(entry.getValue().get("relationsInternation")!=null ? entry.getValue().get("relationsInternation").toString() : "");
				uploadInfo.setReferences(entry.getValue().get("references")!=null ? entry.getValue().get("references").toString() : "");
				uploadInfo.setUploadFileName(entry.getValue().get("fileName")!=null ? entry.getValue().get("fileName").toString() : "");
				result.add(uploadInfo);
			}
		}
		Collections.sort(result, new Comparator<UploadInfo>() {
			@Override
			public int compare(UploadInfo o1, UploadInfo o2) {
				int i = o1.getCategory().compareTo(o2.getCategory());
				return i;
			}
		});

		if (!StringUtils.isBlank(errModuleCode)) {
			errModuleCode = errModuleCode.substring(0, errModuleCode.length() - 1);
			if (StringUtils.isEmpty(type)) {
				// 为微服务模块，请在微服务配置中上载！
				sbMessage.append(errModuleCode + InternationalResource.get("ec.model.upload.isMsService") + "</br>");
			} else {
				// 为老的BAP模块，请在BAP配置中上载！
				sbMessage.append(errModuleCode + InternationalResource.get("ec.model.upload.isBAPService") + "</br>");
			}
		}

		if(isExistIllegalFile){
			sbMessage.append(InternationalResource.get("ec.model.upload.error")+"</br>");
		}
		if(null != result && result.size()>0){
			uploadInfoPage.setResult(result);
		}else{
			UploadInfo uploadInfo = new UploadInfo();
			uploadInfo.setModuleCode("");
			uploadInfo.setModuleName("");
			uploadInfo.setCategory("");
			uploadInfo.setOldVersion("");
			uploadInfo.setIsFirstImport(false);
			uploadInfo.setUploadState(InternationalResource.get("foundation.inter.failed"));
			uploadInfo.setRelations("");
			uploadInfo.setReferences("");
			uploadInfo.setUploadFileName("");
			result.add(uploadInfo);
			uploadInfoPage.setResult(result);
		}
		returnMsg.put("uploadInfoPage", uploadInfoPage);
		returnMsg.put("message", sbMessage.toString());
		log.info(mapProperties.toString());
		return returnMsg;
	}

	private Map<String, Object> getModuleProperties(ZipEntry ze, ZipFile zf, Set<String> moduleRepeatSet,StringBuffer sbMessage) throws IOException{
		Map<String, Object> properties = new HashMap<String, Object>();
		Map<String, Map<String,String>> i10Map = new HashMap<String, Map<String,String>>();
		long size = ze.getSize();
		if (size > 0) {
			BufferedReader br = new BufferedReader(
					new InputStreamReader(
							zf.getInputStream(ze)));
			String line;
			int count = 0;
			while ((line = br.readLine()) != null) {
				if (line.indexOf("<code>") > 0  && count < 10) {
					String code = line.trim().replace("<code>", "").replace("</code>", "");
					if(moduleRepeatSet.contains(code)){
						sbMessage.append(InternationalResource.get("ec.model.upload.modulecode")+code+" "+InternationalResource.get("ec.model.upload.modulerepeat") +"</br>");
					}else{
						moduleRepeatSet.add(code);
						properties.put("code", line.trim()
								.replace("<code>", "")
								.replace("</code>", ""));
					}
				} else if (line.indexOf("<name>") > 0  && count < 10) {
					properties
							.put("name", line.trim()
									.replace("<name>", "")
									.replace("</name>", ""));
				} else if (line.indexOf("<category>") > 0  && count < 10) {
					properties.put("category", line.trim()
							.replace("<category>", "")
							.replace("</category>", ""));
				} else if (line.indexOf("<projectVersion>") > 0  && count < 10) {
					properties
							.put("projectVersion",
									line.trim()
											.replace("<projectVersion>","")
											.replace("</projectVersion>",""));
				}else if (line.indexOf("<relations>") > 0 ) {
					String relations = line.trim().replace("<relations>","").replace("</relations>","");
					if(relations!=null && relations.length()>0){
						relations = relations.substring(0,relations.length()-1);
					}
					properties.put("relations",relations);
				} else if(line.indexOf("<references>") > 0){
					String references = line.trim().replace("<references>","").replace("</references>","");
					if(!StringUtils.isEmpty(references)){
						references = references.substring(0,references.length()-1);
					}
					properties.put("references",references);
				}
				else if (line.indexOf("<relationsInternation>") > 0 ) {
					String relations = line.trim().replace("<relationsInternation>","").replace("</relationsInternation>","");
					if(relations!=null && relations.length()>0){
						relations = relations.substring(0,relations.length()-1);
					}
					properties.put("relationsInternation",relations);
				}
				else if (line.indexOf("<package_zh_TW>") > 0 ) {
					String package_zh_TW = line.trim().replace("<package_zh_TW>","").replace("</package_zh_TW>","");
					Map<String,String> i10MapInner = new HashMap<String, String>();
					String[] i10s = package_zh_TW.split(",");
					for (String string : i10s) {
						String[] keyValue = string.split("=");
						if(keyValue.length>1){
							i10MapInner.put(keyValue[0], keyValue[1]);
						}

					}
					i10Map.put("zh_TW",i10MapInner);
				}
				else if (line.indexOf("<package_zh_CN>") > 0 ) {
					String package_zh_TW = line.trim().replace("<package_zh_CN>","").replace("</package_zh_CN>","");
					Map<String,String> i10MapInner = new HashMap<String, String>();
					String[] i10s = package_zh_TW.split(",");
					for (String string : i10s) {
						String[] keyValue = string.split("=");
						if(keyValue.length>1){
							i10MapInner.put(keyValue[0], keyValue[1]);
						}

					}
					i10Map.put("zh_CN",i10MapInner);
				}
				else if (line.indexOf("<package_en_US>") > 0 ) {
					String package_zh_TW = line.trim().replace("<package_en_US>","").replace("</package_en_US>","");
					Map<String,String> i10MapInner = new HashMap<String, String>();
					String[] i10s = package_zh_TW.split(",");
					for (String string : i10s) {
						String[] keyValue = string.split("=");
						if(keyValue.length>1){
							i10MapInner.put(keyValue[0], keyValue[1]);
						}

					}
					i10Map.put("en_US",i10MapInner);
				}
				if (line.indexOf("<type>Mis</type>") > -1) {
					properties
							.put("type", "MIS");
				}
				count++;
			}
			//把前面保存的国际化保存到properties中
			if(i10Map.size()>0){
				properties.put("i10Properties", i10Map);
				if(null != properties.get("relationsInternation") ){
					String relationsInternation = properties.get("relationsInternation").toString();
					StringBuilder sb = new StringBuilder();
					String language  = InternationalResource.getDefaultLanguage();
					Map<String,String> internation = i10Map.get(language);
					//开始循环国际化主键替换成对应的value的值
					for(String relation : relationsInternation.split(",")){
						if( null != relation && !"".equals(relation)){
							sb.append(internation.get(relation)).append(",");
						}
					}
					if(sb.length()>0){
						properties.put("relationsInternation", sb.toString());
					}
				}
			}
			br.close();
		}
		return properties;
	}

	private Map<String, String> getL10Properties(ZipEntry ze, ZipFile zf) throws IOException{
		Map<String, String> properties = new HashMap<String, String>();
		long size = ze.getSize();
		if (size > 0) {
			BufferedReader br = new BufferedReader(
					new InputStreamReader(
							zf.getInputStream(ze)));
			String line;
			int count = 0;
			while ((line = br.readLine()) != null
					&& count < 3000) {
				System.out.println(line);
				String[] propertiesLine = line.split("=");
				if(propertiesLine!=null && propertiesLine.length>1 && propertiesLine[0] !=null && propertiesLine[1] !=null ){
					properties.put(propertiesLine[0], propertiesLine[1]);
				}
				count++;
			}
			br.close();
		}
		return properties;
	}
	
	@ResponseBody
	@RequestMapping(value = "/ec/module/companyRef")
	public List<Long> getModuleCompanyRef(String moduleId) throws Exception {
		List<Long> refList =moduleService.getModuleCompanyRef(moduleId);
		return refList;
	}
}
