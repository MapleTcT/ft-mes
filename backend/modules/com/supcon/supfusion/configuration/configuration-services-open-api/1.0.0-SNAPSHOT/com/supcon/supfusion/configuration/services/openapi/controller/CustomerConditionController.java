package com.supcon.supfusion.configuration.services.openapi.controller;

import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.ViewType;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.openapi.vo.ResponseMsg;
import com.supcon.supfusion.configuration.services.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


@Slf4j
@Controller
//@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "${spring.application.name}" + HttpConstants.URL_SPLITER + "v1")
public class CustomerConditionController extends ConfigurationBaseController {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2199622047887013888L;
	
	private DataGrid dataGrid;

	@Resource
	private ConditionService conditionService;
	
	@Resource
	private ViewService viewService;
	
	@Resource
	private CustomerConditionService customerConditionService;
	
	@Resource
	private DataGridService dataGridService;
	
	@Autowired
	private EcDataSynchronizeService ecDataSynchronizeService;
	private Boolean isProj = false;
	
	@ResponseBody
	@RequestMapping(value = "/ec/customerCon/saveCusCon")
	public ResponseMsg saveCusCon(HttpServletRequest request) throws Exception {
		isProj = Boolean.valueOf(request.getParameter("isProj"));
		if (isProj) {
			// 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
			ProjectFlagHolder.getInstance().getProjFlag().set(true);
		}
		String dataGridCode = request.getParameter("datagrid.code");
		String viewCode = request.getParameter("view.code");
		String jsonData = request.getParameter("dgQueryCond");
		String sqlData = request.getParameter("sqlCond");
		CustomerCondition ccon = new CustomerCondition();
		if(dataGridCode!=null && dataGridCode.length()>0){
			DataGrid dg = dataGridService.getDataGrid(dataGridCode);
			ccon.setDataGrid(dg);
			ccon.setView(dg.getView());
			ccon.setCode(dg.getView().getCode() + "_" + dg.getCode());
			ccon.setModuleCode(dg.getModuleCode());
			ccon.setEntityCode(dg.getEntityCode());
		}else if(viewCode!=null && viewCode.length()>0){
			View view = viewService.getView(viewCode);
			ccon.setView(view);
			ccon.setModuleCode(view.getModuleCode());
			ccon.setEntityCode(view.getEntity().getCode());
			String classificCode = request.getParameter("classific.code");
			if(classificCode != null && classificCode.length() > 0){
				DataClassific dataClassific = viewService.getDataClassific(classificCode);
				if(dataClassific != null){
					ccon.setDataClassific(dataClassific);
				}else{
					dataClassific = new DataClassific();
					dataClassific.setCode(classificCode);
				}
				ccon.setCode(view.getCode() + "_" + dataClassific.getCode());
			}else{
				ccon.setCode(view.getCode());
			}
		}
		ResponseMsg response = new ResponseMsg(true);
		if(sqlData!=null && sqlData.length() !=0){
			ccon.setSql(sqlData);
			ccon.setJsonCondition("");
			response.setData(sqlData);
		}else if(jsonData!=null && jsonData.length() !=0){
			AdvQueryCondition cond = conditionService.toSql(request.getParameter("dgQueryCond"), true);
			ccon.setJsonCondition(request.getParameter("dgQueryCond"));
			ccon.setSql("");
			response.setData(cond.getSql());
		}else{
			ccon.setJsonCondition("");
			ccon.setSql("");
			response.setData("");
		}
		if(isProj){
			ccon.setProjFlag(true);
		}
		customerConditionService.saveCustomerCondition(ccon);
		if(viewCode!=null && viewCode.length()>0){
			View view = viewService.getView(viewCode);
			if(view.getType()== ViewType.MNECODE){
				ecDataSynchronizeService.synchronizeEcDataFromDevToRumtime(view.getModuleCode());
			}
		}
		//ObjectMapper mapper = new ObjectMapper();
		ProjectFlagHolder.getInstance().getProjFlag().set(false);
		return response;
	}

	@ResponseBody
	@RequestMapping(value = "/ec/customerCon/getCustomerCondition")
	public ResponseMsg getCustomerCondition(HttpServletRequest request) throws Exception {
		isProj = Boolean.valueOf(request.getParameter("isProj"));
		if (isProj) {
			// 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
			ProjectFlagHolder.getInstance().getProjFlag().set(true);
		}
		String dataGridCode = request.getParameter("dataGridCode");
		String viewCode = request.getParameter("viewCode");
		String dataClassificCode = request.getParameter("dataClassificCode");
		CustomerCondition ccon = null;
		if(dataClassificCode != null && dataClassificCode.length() > 0){
			ccon = customerConditionService.getCustomerConditionByClassificCode(dataClassificCode);
		}else if(dataGridCode != null && dataGridCode.length()>0){
			ccon = customerConditionService.getCustomerConditionByDataGridCode(dataGridCode);
		}else if(viewCode != null && viewCode.length()>0){
			ccon = customerConditionService.getCustomerConditionByViewCode(viewCode);
		}
		ResponseMsg response = new ResponseMsg(true);
		if(ccon!=null && ccon.getJsonCondition()!=null && ccon.getJsonCondition().length()>0){
			response.setData(ccon.getJsonCondition());
		}else{
			response.setSuccess(false);
		}
		//ObjectMapper mapper = new ObjectMapper();
		ProjectFlagHolder.getInstance().getProjFlag().set(false);
		return response;
	}

	@ResponseBody
	@RequestMapping(value = "/ec/customerCon/getShowSql")
	public ResponseMsg getShowSql(HttpServletRequest request) throws Exception {
		isProj = Boolean.valueOf(request.getParameter("isProj"));
		if (isProj) {
			// 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
			ProjectFlagHolder.getInstance().getProjFlag().set(true);
		}
		String dataGridCode = request.getParameter("dataGridCode");
		String viewCode = request.getParameter("viewCode");
		String dataClassificCode = request.getParameter("dataClassificCode");
		CustomerCondition ccon = null;
		if(dataGridCode != null && dataGridCode.length()>0){
			ccon = customerConditionService.getCustomerConditionByDataGridCode(dataGridCode);
		}else if(viewCode != null && viewCode.length()>0){
			ccon = customerConditionService.getCustomerConditionByViewCode(viewCode);
		}else if(dataClassificCode != null && dataClassificCode.length() > 0){
			ccon = customerConditionService.getCustomerConditionByClassificCode(dataClassificCode);
		}
		ResponseMsg response = new ResponseMsg(true);
		if(ccon!=null && ccon.getSql()!=null && ccon.getSql().length()>0){
			response.setData(ccon.getSql());
		}else if(ccon!=null && ccon.getJsonCondition()!=null && ccon.getJsonCondition().length()>0){
			response.setSuccess(false);
			response.setData(conditionService.toSql(ccon.getJsonCondition(), true).getSql());
		}else{
			response.setSuccess(false);
		}
		//ObjectMapper mapper = new ObjectMapper();
		ProjectFlagHolder.getInstance().getProjFlag().set(false);
		return response;
	}

	@ResponseBody
	@RequestMapping(value = "/ec/customerCon/transtoSql")
	public ResponseMsg transtoSql(HttpServletRequest request) throws Exception {
		isProj = Boolean.valueOf(request.getParameter("isProj"));
		if (isProj) {
			// 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
			ProjectFlagHolder.getInstance().getProjFlag().set(true);
		}
		String jsonData = request.getParameter("dgQueryCond");
		ResponseMsg response = new ResponseMsg(true);
		if(jsonData!=null &&  jsonData.length()>0){
			response.setData(conditionService.toSql(jsonData, true).getSql());
		}else{
			response.setSuccess(false);
		}
		//ObjectMapper mapper = new ObjectMapper();
		ProjectFlagHolder.getInstance().getProjFlag().set(false);
		return response;
	}
}
