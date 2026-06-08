/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 

 * All rights reserved.
 */
package com.supcon.supfusion.configuration.workflow.openapi;


import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.base.services.*;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.utils.SqlParser;
import com.supcon.supfusion.configuration.services.utils.StringUtils;
import com.supcon.supfusion.base.entities.Deployment;
import com.supcon.supfusion.base.entities.MenuInfo;
import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.service.EntityService;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.configuration.workflow.service.ProcessServiceFlow;
import com.supcon.supfusion.configuration.workflow.core.WorkflowBaseController;
import com.supcon.supfusion.configuration.workflow.service.WorkflowProcessService;
import com.supcon.supfusion.configuration.workflow.service.WorkflowTaskService;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

import static org.springframework.context.i18n.LocaleContextHolder.getLocale;


@Slf4j
@Controller
public class WorkFlowController extends WorkflowBaseController {

	@Autowired
	private ProcessServiceFlow processService;
	@Autowired
	private WorkflowProcessService workflowProcessService;
	@Autowired
	private DataPermissionService dataPermissionService;
	@Autowired
	private PositionService positionService;
	@Autowired
	private MenuInfoService menuInfoService;
	@Autowired
	private EntityService entityService;
	@Autowired
	private ModelService modelService;
	@Autowired
	private WorkflowTaskService taskService;
	private InternationalResource internationalResource;
	@Autowired
	private CustomMenuOperateService customMenuOperateService;


	@RequestMapping(value = "/ec/workflow/flowEditH5")
	public String flowEdit(ModelMap map, HttpServletRequest request) {
		Deployment dp = null;
		Long deploymentId = Long.valueOf(request.getParameter("deploymentId"));
		String flowKey = request.getParameter("flowKey");
		String flowVersion = request.getParameter("flowVersion");
		Integer version = 0;
		if (!StringUtils.isEmpty(request.getParameter("version")) && !"null".equals(request.getParameter("version"))) {
			version = Integer.valueOf(request.getParameter("version"));
		}
		if (null != deploymentId) {
			dp = processService.getDeployment(deploymentId);
		}
		if (ObjectUtils.isEmpty(flowVersion)) {
			flowVersion = "0";
		}
		if (null != flowKey && flowVersion != null && !"".equals(flowKey) && !"".equals(flowVersion)) {
			List<String> activeList = new ArrayList<String>();
			if (!flowVersion.equals("0")) {
				activeList = menuInfoService.getMenuOperateByFlowKey(flowKey, flowVersion);
			}

			int processVersion = 0;
			if (null != dp) {
				if (dp.getVersion() != null) {
					version = dp.getVersion();
				}
				processVersion = dp.getProcessVersion();
			}

			String activeArr = "";
			for (String ac : activeList) {
				activeArr += "," + ac;
			}
			if (!"".equals(activeArr)) {
				activeArr = activeArr.substring(1);
			}
			String powerXml = null;
			String selectStaffs = null;
			if (null != dp) {
				if (!flowVersion.equals("0")) {
					powerXml = dataPermissionService.getFlowPower(flowKey, flowVersion);
				} else {
					powerXml = "";
				}
				if (dp.getPublishFlag()!=null&&!dp.getPublishFlag()) {
					powerXml = processService.powerXml(dp, powerXml);
				}
				selectStaffs=processService.getTranstionSelectStaffs(dp.getId());
			}
			map.addAttribute("dp", dp);
			map.addAttribute("activeList", activeList);
			map.addAttribute("processVersion", processVersion);
			map.addAttribute("activeArr", activeArr);
			map.addAttribute("powerXml", powerXml);
			map.addAttribute("selectStaffs", selectStaffs);
			map.addAttribute("deploymentId", deploymentId);
			map.addAttribute("flowKey", flowKey);
			map.addAttribute("flowVersion", flowVersion);
			map.addAttribute("namekey", request.getParameter("namekey"));
			map.addAttribute("flowName", request.getParameter("flowName"));
			map.addAttribute("des", request.getParameter("des"));
			map.addAttribute("entityCode", request.getParameter("entityCode"));
			map.addAttribute("moduleCode", request.getParameter("moduleCode"));
			String moduleCode = request.getParameter("moduleCode");
			map.addAttribute("i18nModuleCode", moduleCode.substring(0,moduleCode.indexOf("_")));
			map.addAttribute("flowId", request.getParameter("flowId"));
			map.addAttribute("menuId", request.getParameter("menuId"));
			map.addAttribute("flowEditFlag", request.getParameter("flowEditFlag"));
			map.addAttribute("superviseNamesMultiIDs", request.getParameter("superviseNamesMultiIDs"));
			map.addAttribute("requiredTime", request.getParameter("requiredTime"));
			map.addAttribute("mobileinitiate", request.getParameter("mobileinitiate"));
			map.addAttribute("mobileapprove", request.getParameter("mobileapprove"));
			map.addAttribute("allowInvalid", request.getParameter("allowInvalid"));
			map.addAttribute("graduallyReject", request.getParameter("graduallyReject"));
			map.addAttribute("recallAble", request.getParameter("recallAble"));
			map.addAttribute("recallRemainTime", request.getParameter("recallRemainTime"));
			map.addAttribute("signature", request.getParameter("signature"));
			map.addAttribute("mainViewViewCode", request.getParameter("mainViewViewCode"));
			map.addAttribute("env", request.getParameter("env"));
			map.addAttribute("version", version);
			map.addAttribute("groupRestrict", false);
		}
//		systemVersion = processService.getSystemVersion();
		return "workflow/flowEditH5";
	}

	@ResponseBody
	@RequestMapping(value = "/ec/workflow/getSupervises")
	public List<Staff> getSupervises(Long deploymentId) {
		List<Staff> staffList = null;

		if (null != deploymentId) {
			staffList=processService.getSupervises(deploymentId);
		}

		return staffList;
	}

	/**
	 * 判断processKey是否在别的实体中有重复
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/ec/workflow/repeat")
	public HashMap repeart(HttpServletRequest request) {
		HashMap<String, Object> responseMap = new HashMap<>();
		String processKey = request.getParameter("processKey");
		String entityCode = request.getParameter("entityCode");
		Long deploymentId = null;
		if (!StringUtils.isEmpty(request.getParameter("deploymentId"))) {
			deploymentId = Long.valueOf(request.getParameter("deploymentId"));
		}
		if (!StringUtils.isEmpty(processKey)) {
			boolean value = false;
			String errorMsg = "";
			if (null != deploymentId) {//修改
				value = processService.repeat(processKey, entityCode);
				if (value) {
					errorMsg = InternationalResource.get("ec.entity.wf.repeat");
					responseMap.put("dealSuccessFlag", false);
					responseMap.put("errorMsg", errorMsg);
				}
			} else {//新增
				value = processService.repeat(processKey, null);
				if (value) {//如果有重复，则返回false
					responseMap.put("dealSuccessFlag", false);
					errorMsg = InternationalResource.get("ec.entity.wf.repeat");
					responseMap.put("errorMsg", errorMsg);
				}
			}

		} else {
			responseMap.put("dealSuccessFlag", false);
			responseMap.put("errorMsg", "processKey is null");
		}
		return responseMap;
	}


	/**
	 * 流程保存
	 *
	 * @return*/
	@ResponseBody
	@RequestMapping(value = "/ec/workflow/flowSave", produces = MediaType.APPLICATION_XML_VALUE)
	public String flowSave(HttpServletRequest request) {

		String deploymentIdStr = request.getParameter("deploymentId");
		String str ="";

		Deployment deployment = null;
		if (!StringUtils.isEmpty(deploymentIdStr)) {
			deployment = processService.getDeployment(Long.parseLong(deploymentIdStr));
			if (!deployment.getVersion().equals(Integer.valueOf(request.getParameter("version")))) {
				str = "<versionConflict version='" + deployment.getVersion() + "'/>";
				return str;
			}
		}
		if (null == deployment) {
			deployment = new Deployment();
			deployment.setVersion(0);
		}
		if (null != request.getParameter("flowXML") && !"".equals(request.getParameter("flowXML"))) {
			deployment.setTempProcessXml(request.getParameter("flowXML"));
		}
		if (null != request.getParameter("entityCode")) {
			deployment.setEntityCode(request.getParameter("entityCode"));
		}
		Boolean crossCompanyFlag = false;
		if(null!=request.getParameter("crossCompanyFlag")){
			crossCompanyFlag=Boolean.valueOf(request.getParameter("crossCompanyFlag"));
		}
		deployment.setCrossCompanyFlag(crossCompanyFlag);
		deployment.setCid(getCurrentCompanyId());
		deployment.setProcessKey(request.getParameter("flowKey"));
		if (null != request.getParameter("namekey")) {
			deployment.setName(request.getParameter("namekey"));
		}
		if (null != request.getParameter("requiredTime")) {
			if (BigDecimal.valueOf(Double .parseDouble(request.getParameter("requiredTime"))).compareTo(new BigDecimal(0)) == 0) {
				deployment.setRequiredTime(null);
			} else {
				deployment.setRequiredTime(BigDecimal.valueOf(Double.parseDouble(request.getParameter("requiredTime"))));
			}
		}
		if (null != request.getParameter("mobilequery")) {
			if(request.getParameter("mobilequery").equals("true")){
				deployment.setMobilequery(true);
			}else{
				deployment.setMobilequery(false);
			}
		}
		if (null != request.getParameter("mobileinitiate")) {
			if(request.getParameter("mobileinitiate").equals("true")){
				deployment.setMobileinitiate(true);
			}else{
				deployment.setMobileinitiate(false);
			}
		}
		if (null != request.getParameter("mobileapprove")) {
			if(request.getParameter("mobileapprove").equals("true")){
				deployment.setMobileapprove(true);
			}else{
				deployment.setMobileapprove(false);
			}
		}
		if (null != request.getParameter("recallAble")) {
			if(request.getParameter("recallAble").equals("true")){
				deployment.setRecallAble(true);
			}else{
				deployment.setRecallAble(false);
			}
		}
		if(null != request.getParameter("signature")) {
			deployment.setSignatureEnable(Boolean.valueOf(request.getParameter("signature")));
		}

		if (null != request.getParameter("recallRemainTime") && !request.getParameter("recallRemainTime").equals("")) {
			deployment.setRecallRemainTime(Long.parseLong(request.getParameter("recallRemainTime")));
		}else{
			deployment.setRecallRemainTime(null);
		}

		if (null != request.getParameter("mainViewViewCode") && !request.getParameter("mainViewViewCode").isEmpty()) {
			deployment.setMainViewViewCode(request.getParameter("mainViewViewCode"));
		}

		if (!ObjectUtils.isEmpty(request.getParameter("menuId")) ) {
			deployment.setMenuInfoId(Long.valueOf(request.getParameter("menuId")));
			MenuInfo menuInfo = menuInfoService.load(Long.valueOf(request.getParameter("menuId")));
			if(null != menuInfo){
				deployment.setMenuCode(menuInfo.getCode());
			}
		}
		if (null != request.getParameter("des")) {
			deployment.setDescription(request.getParameter("des"));
		}
		if (null != request.getParameter("entryUrl")) {
			deployment.setEntryUrl(request.getParameter("entryUrl"));
		}
		if (null != request.getParameter("operatePowers")) {
			deployment.setOperatePowers(request.getParameter("operatePowers"));
		}
		if (!StringUtils.isEmpty(request.getParameter("flowEditFlag"))) {
			deployment.setFlowEditFlag(Integer.parseInt(request.getParameter("flowEditFlag")) == 1);
		}
		if (deployment.getId() == null) {
			deployment.setCreateStaffId(getCurrentStaff().getId());
			deployment.setCreateTime(new Date());
		} else {
			deployment.setModifyStaffId(getCurrentStaff().getId());
			deployment.setModifyTime(new Date());
		}

		// 保存设置允许管理员作废
		if (null != request.getParameter("allowInvalid")) {
			deployment.setAllowInvalid("true".equals(request.getParameter("allowInvalid"))? true : false);
		}

		// 保存设置逐级驳回
		if (null != request.getParameter("graduallyReject")) {
			deployment.setGraduallyReject("true".equals(request.getParameter("graduallyReject"))? true : false);
		}

		if (null != request.getParameter("keyDescs")) {
			deployment.setKeyDescs(request.getParameter("keyDescs"));
		}
		try{
			processService.saveDeployment(deployment,
					request.getParameter("publishPower"),
					request.getParameter("activeArr"),
					request.getParameter("updatePowerString"),
					request.getParameter("superviseNamesMultiIDs"),
					request.getParameter("selectStaffs"),
					request.getParameter("linkRangeChage"));

			str = "<flow id='" + deployment.getDeploymentId() + "' deploymentId='" + deployment.getId() + "' version='"
					+ deployment.getVersion() + "' processVersion='"+deployment.getProcessVersion()+"' />";
		}catch(Exception e){
			String message ="";
			if(e instanceof EcException){
				String exceptionKey=((EcException) e).getMessageKey();
				message=InternationalResource.get(exceptionKey);
			}else{
				message=e.getMessage();
			}
			log.error(message);
			str = "<error mess='" + message + "'/>";
		}
		return str;
	}


	@ResponseBody
	@RequestMapping(value = "/ec/workflow/getFlow", produces = MediaType.APPLICATION_XML_VALUE)
	public String getFlow(Long deploymentId) {
		String flowXML = null;
		if (null != deploymentId) {
			Deployment deployment = processService.getDeployment(deploymentId);

			if (deployment.getTempProcessXml()!=null && deployment.getTempProcessXml().length()>0) {
				flowXML = deployment.getTempProcessXml();
			} else {
				flowXML = deployment.getProcessXml();
			}
			if(flowXML==null||flowXML.equals("")){
				flowXML=deployment.getProcessXml();
			}
			if(flowXML!=null&&!flowXML.equals("")){
				//BugFree-19734:删除分发后，聚合前，会签后的迁移线选人
				flowXML = processService.handleFlowXml(flowXML);
				flowXML = processService.analyticXml(flowXML, getLocale().toString(), "runtime");
			}
		}
		return flowXML;
	}

	@ResponseBody
	@RequestMapping(value = "/ec/workflow/flowPublish", produces = MediaType.APPLICATION_XML_VALUE)
	public String flowPublish(String des, String superviseNamesMultiIDs,
							  BigDecimal requiredTime, Boolean signature,
							  String mainViewViewCode, Long menuId, String flowKey,
							  String entityCode, String namekey, String mobilequery,
							  String linkRangeChage, String flowXML, String entryUrl,
							  String activeArr, String selectStaffs, String keyDescs,
							  String updatePowerString, String menuOperateStr, String operatePowers,
							  Long preDeploymentId, Long deploymentId, int version) {
		String env = getRequest().getParameter("env");
		String mobileinitiate = getRequest().getParameter("mobileinitiate");
		String crossCompanyFlag = getRequest().getParameter("crossCompanyFlag");
		String recallRemainTime = getRequest().getParameter("recallRemainTime");
		String mobileapprove = getRequest().getParameter("mobileapprove");
		String recallAble = getRequest().getParameter("recallAble");
		String allowInvalid = getRequest().getParameter("allowInvalid");
		String graduallyReject = getRequest().getParameter("graduallyReject");
		String publishType = getRequest().getParameter("publishType");
		Deployment deployment = null;
		if (null != deploymentId) {
			deployment = processService.getDeployment(deploymentId);
			if (deployment.getVersion() != version) {
				String str = "<versionConflict version='" + deployment.getVersion() + "'/>";
				return str;
			}
		}
		Deployment preDeployment = null;
		if (preDeploymentId != null) {
			preDeployment = processService.getDeployment(preDeploymentId);
			preDeployment.setFlowEditFlag(true);
			preDeployment.setModifyTime(new Date());
			preDeployment.setOperatePowers("");
			preDeployment.setTempProcessXml("");
			preDeployment.setPublishFlag(true);
			if (null == preDeployment.getDeploymentId()) {
				preDeployment.setValid(false);
			}
		}
		if (null == deployment) {
			deployment = new Deployment();
			deployment.setProcessKey(flowKey);
			deployment.setCid(getCurrentCompanyId());
		}
		if (null != flowXML && !"".equals(flowXML)) {
			deployment.setProcessXml(flowXML);
		}
		if (null != entityCode) {
			deployment.setEntityCode(entityCode);
		}

		if (null != des) {
			deployment.setDescription(des);
		}
		if (null != entryUrl) {
			deployment.setEntryUrl(entryUrl);
		}

		if (null != menuId) {
			deployment.setMenuInfoId(menuId);
			MenuInfo menuInfo = menuInfoService.load(menuId);
			if(null != menuInfo){
				deployment.setMenuCode(menuInfo.getCode());
			}
		}

		if (null != namekey) {
			deployment.setName(namekey);
		}
		if (null != keyDescs) {
			deployment.setKeyDescs(keyDescs);
		}
		if (null != requiredTime) {
			if (requiredTime.compareTo(new BigDecimal(0)) == 0) {
				deployment.setRequiredTime(null);
			} else {
				deployment.setRequiredTime(requiredTime);
			}
		}
		if (null != mobilequery) {
			if(mobilequery.equals("true")){
				deployment.setMobilequery(true);
			}else{
				deployment.setMobilequery(false);
			}
		}
		if (null != mobileinitiate) {
			if(mobileinitiate.equals("true")){
				deployment.setMobileinitiate(true);
			}else{
				deployment.setMobileinitiate(false);
			}
		}
		if (null != crossCompanyFlag) {
			if(crossCompanyFlag.equals("true")){
				deployment.setCrossCompanyFlag(true);
			}else{
				deployment.setCrossCompanyFlag(false);
			}
		}
		boolean isMobileApproveChanged = false;
		if (null != mobileapprove) {
			isMobileApproveChanged = !mobileapprove.equals(deployment.getMobileapprove() == null ? "false" : deployment.getMobileapprove().toString());
			if(mobileapprove.equals("true")){
				deployment.setMobileapprove(true);
			}else{
				deployment.setMobileapprove(false);
			}
		}
		if (null != recallAble) {
			if(recallAble.equals("true")){
				deployment.setRecallAble(true);
			}else{
				deployment.setRecallAble(false);
			}
		}

		if(null != signature) {//保存电子签名属性
			deployment.setSignatureEnable(signature);
		}

		if (null != recallRemainTime && !recallRemainTime.equals("")) {
			deployment.setRecallRemainTime(Long.parseLong(recallRemainTime));
		}else{
			deployment.setRecallRemainTime(null);
		}

		if (null != mainViewViewCode && !mainViewViewCode.isEmpty()) {
			deployment.setMainViewViewCode(mainViewViewCode);
		}

		if (null != allowInvalid) {
			if(allowInvalid.equals("true")){
				deployment.setAllowInvalid(true);
			}else{
				deployment.setAllowInvalid(false);
			}
		}
		if (null != graduallyReject) {
			if(graduallyReject.equals("true")){
				deployment.setGraduallyReject(true);
			}else{
				deployment.setGraduallyReject(false);
			}
		}
		deployment.setOperatePowers("");
		if (deployment.getId() == null) {
			deployment.setCreateStaffId(getCurrentStaff().getId());
		} else {
			deployment.setModifyStaffId(getCurrentStaff().getId());
		}
		if (preDeployment != null) {
			deployment.setCreateTime(preDeployment.getCreateTime());
		}
		deployment.setPublishFlag(true);
		deployment.setFlowEditFlag(true);
		deployment.setTempProcessXml("");
		String str = null;
//		User user = (User) session.get("user");
		try {
			List<Map<String, String>> addpermissions = null;
			List<Map<String, String>> delpermissions = null;
			String startActivityCode = getStartActivityCode(activeArr);
			// 设置发布时间
			deployment.setPublishTime(new Date());
			if (deployment.getId() != null && publishType != null && "modify".equals(publishType)) {
				workflowProcessService.update(deployment, operatePowers, menuOperateStr, activeArr, updatePowerString,superviseNamesMultiIDs,selectStaffs,linkRangeChage,env);
			} else {
				deployment.setIsCurrentVersion(true);
				workflowProcessService.deploy(preDeployment, deployment, true, operatePowers, menuOperateStr, activeArr, updatePowerString,superviseNamesMultiIDs,selectStaffs,linkRangeChage,env);
			}
			// 刷新待办的mobileApprove字段
			taskService.dealMobileApprovePending(deployment.getProcessKey(), deployment.getProcessVersion(), deployment.getMobileapprove(), isMobileApproveChanged);
			//  工作流发布后生成操作URL
			customMenuOperateService.generateWFOperateUrl(deployment.getId(), entityCode, menuId);
			str = "<flow id='" + deployment.getDeploymentId() + "' deploymentId='" + deployment.getId() + "' version='"
					+ deployment.getVersion() + "' processVersion='"+deployment.getProcessVersion()+"' />";
		} catch (Exception e) {
			String message ="";
			if(e instanceof EcException){
				String exceptionKey=((EcException) e).getMessageKey();
				message= InternationalResource.get(exceptionKey);
			}else{
				message=e.getMessage();
			}
			log.error(e.getMessage(), e);
			str = "<error mess='" + message + "'/>";
		}

		return str;
	}

	private String getStartActivityCode(String actives){
		String[] taskArr = actives.split(",");
		String startActivityCode = "";

		for (String task : taskArr) {
			if (task.toUpperCase().startsWith("START")) {
				startActivityCode = task;
			}
		}
		return startActivityCode;
	}

	@ResponseBody
	@RequestMapping("/ec/workflow/delete")
	public HashMap delete(HttpServletRequest request) {
		HashMap<String, Object> responseMap = new HashMap<>();
		String deploymentIds = request.getParameter("deploymentIds");
		if (!StringUtils.isEmpty(deploymentIds)) {
			try {
				processService.deleteFlow(deploymentIds);
				responseMap.put("dealSuccessFlag", true);
			} catch (Exception e) {
				throw new RuntimeException("delete flow failed",e);
			}
		}

		return responseMap;
	}

	@ResponseBody
	@RequestMapping("/ec/workflow/setToCurrentVersion")
	public Map setToCurrentVersion(Long deploymentId) {
		Map<String, Object> responseMap = new HashMap<>();
		if (deploymentId != null) {
			Deployment d = processService.getDeployment(deploymentId);
			processService.currentVersion(d.getProcessKey(), d.getProcessVersion());
			responseMap.put("dealSuccessFlag", true);
		} else {
			responseMap.put("dealSuccessFlag", false);
		}
		return responseMap;
	}

	@RequestMapping("/ec/workflow/referenceListFrame")
	public String referenceListFrame() {
		return "workflow/referenceListFrame";
	}

	@ResponseBody
	@RequestMapping("/ec/workflow/queryflowList")
	public Page queryflowList(String flowName, @Nullable String entityName, @Nullable String entityCode, @Nullable String moduleCode) {

		Map<String,Object> parameterMap = new LinkedHashMap<String,Object>();
		StringBuilder recordSql = new StringBuilder();
		StringBuilder pageSql = new StringBuilder();
		recordSql
				.append("select distinct(wf.ID),wf.DEPLOYMENT_ID,wf.ENTITY_CODE,wf.NAME,wf.PROCESS_VERSION,wf.IS_CURRENT_VERSION ");
		recordSql
				.append(" from wf_deployment wf inner join runtime_entity ec on wf.entity_code = ec.code where 1=1 ");
		pageSql
				.append("select count(wf.id) ");
		pageSql
				.append(" from wf_deployment wf inner join runtime_entity ec on wf.entity_code = ec.code where 1=1 ");
		if (flowName != null && flowName.length() > 0) {
			recordSql.append(" and (wf.name like :flowName or wf.name_zh_cn like :names)");
			pageSql.append(" and (wf.name like :flowName or wf.name_zh_cn like :names)");
			parameterMap.put("flowName", "%" + SqlParser.filtrateSQLLike(flowName) + "%");
			parameterMap.put("names", "%" + SqlParser.filtrateSQLLike(flowName) + "%");
		}
		if (entityName != null && entityName.length() > 0) {
			recordSql.append(" and (ec.name like :entityName or ec.value_zh_cn like :ecnames ) ");
			pageSql.append(" and (ec.name like :entityName or ec.value_zh_cn like :ecnames) ");
			parameterMap.put("entityName", "%" + SqlParser.filtrateSQLLike(entityName) + "%");
			parameterMap.put("ecnames", "%" + SqlParser.filtrateSQLLike(entityName) + "%");
		}
		if (entityCode != null && entityCode.length() > 0) {
			recordSql.append(" and wf.entity_code = :entityCode");
			pageSql.append(" and wf.entity_code = :entityCode");
			parameterMap.put("entityCode", entityCode);
		}
		if (moduleCode != null && moduleCode.length() > 0) {
			recordSql.append(" and ec.module_code = :moduleCode");
			pageSql.append(" and ec.module_code = :moduleCode");
			parameterMap.put("moduleCode", moduleCode);
		}
		recordSql
				.append(" and ec.valid=1 and wf.valid=1 and (wf.process_xml IS NOT NULL or wf.temp_process_xml IS NOT NULL) order by wf.id DESC,wf.process_version DESC ");
		pageSql.append(" and ec.valid=1 and wf.valid=1 and (wf.process_xml IS NOT NULL or wf.temp_process_xml IS NOT NULL) ");
		Page<Map<String, Object>> records = processService.findRecordPageByParams(new Page<Map<String, Object>>(1, 20), recordSql.toString(), pageSql.toString(), parameterMap);
		if (records != null && !records.getResult().isEmpty()) {
			for (Map<String, Object> record : records.getResult()) {
				String key = String.valueOf(record.get("NAME"));
				String name = InternationalResource.get(key);
				record.put("NAME", name);
			}
		}
		return records;
	}

	@ResponseBody
	@RequestMapping("/ec/workflow/foundationListJSON")
	public List referenceListJSON() {
		List<Map<String, Object>> entityAndModules = processService.getModuleAndEntitys();
		for (Map<String, Object> item : entityAndModules) {
			if (item.get("name") != null) {
				item.put("name", InternationalResource.get((String) item.get("name")));
			}
		}
		return entityAndModules;
	}

}
