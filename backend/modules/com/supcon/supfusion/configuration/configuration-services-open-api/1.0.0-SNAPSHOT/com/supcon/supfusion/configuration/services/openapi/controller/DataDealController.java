/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 

 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.openapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.enums.ShowType;
import com.supcon.supfusion.configuration.services.enums.ViewType;
import com.supcon.supfusion.configuration.services.utils.ConditionUtil;
import com.supcon.supfusion.configuration.services.utils.EcUtils;
import com.supcon.supfusion.configuration.services.utils.SerializeUitls;
import com.supcon.supfusion.configuration.services.entity.DataGrid;
import com.supcon.supfusion.configuration.services.entity.Property;
import com.supcon.supfusion.configuration.services.entity.View;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.openapi.vo.ResponseMsg;
import com.supcon.supfusion.configuration.services.service.*;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 数据处理Action类<br>
 * 
 * @author fangzhibin
 * @version 1.0
 */
@Slf4j
@Controller
//@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "${spring.application.name}" + HttpConstants.URL_SPLITER + "v1")
public class DataDealController extends ConfigurationBaseController {

	private static final long serialVersionUID = -5666736192237846554L;
	// ~ 所需要的service =======================================================
	@Resource
	private ViewService viewService;
	
	@Autowired
	private DataGridService dataGridService;
	
	@Autowired
	private ModelService modelService;

//	private Page<View> page;
	@Autowired
	private EcConfigService ecConfigService;
	@Autowired
	private ButtonService buttonService;
//	private String moduleCode;
	@Autowired
	private FieldService fieldService;
	@Autowired
	public void setViewService(ViewService viewService) {
		this.viewService = viewService;
	}
	
//	@Autowired
//	private TaskService taskService;

//	private String processKey;

//	private Integer processVersion;

	@RequestMapping(value = "/ec/view/datadeal")
	public void updateViewCode() {
		Page<View> page = new Page<View>();
		page.setPageSize(Integer.MAX_VALUE);
		viewService.findViews(page);
		List<View> views = page.getResult();
		for (View view : views) {
			try {
				view = viewService.getView(view.getCode(), true);
				view.setCode(view.getEntity().getCode() + "_" + view.getName());
				viewService.saveView(view);
			} catch (Exception e) {
			}
		}
		for (View view : views) {
			try {
				if (view == null || view.getExtraView() == null || view.getExtraView().getConfig() == null) {
					continue;
				}
				if(view.getShowType() != ShowType.LAYOUT) {
					if(view.getType() == ViewType.EDIT || view.getType() == ViewType.VIEW) {
						transformEditView(view);
					}
					if(view.getType() == ViewType.LIST || view.getType() == ViewType.REFERENCE) {
						transformListView(view);
					}
				} else {
					transformLayoutView(view);
				}
				viewService.saveExtraView(view.getExtraView(), null);
			} catch (Exception e) {
			}
		}
	}

	@ResponseBody
	@RequestMapping(value = "/ec/datadeal/ecenv")
	public String ecenv() throws Exception {
		ecConfigService.dealEcEnv();
		ResponseMsg response = new ResponseMsg(true);

		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}

	@RequestMapping(value = "/ec/view/inherentDeal")
	public void updateInherentData() {
		
		List<Property> properties = modelService.findByUpdateProperties();
		for(Property p : properties) {
			if("createStaffId".equals(p.getName())) {
				p.setName("createStaff");
				p.setAssociatedProperty(modelService.getProperty("base_staff_id"));
				p.setAssociatedType(Property.ONE_TO_ONE);
				p.setType(DbColumnType.OBJECT);
				modelService.saveProperty(p);
			}
			if("modifyStaffId".equals(p.getName())) {
				p.setName("modifyStaff");
				p.setAssociatedProperty(modelService.getProperty("base_staff_id"));
				p.setAssociatedType(Property.ONE_TO_ONE);
				p.setType(DbColumnType.OBJECT);
				modelService.saveProperty(p);
			}
			if("deleteStaffId".equals(p.getName())) {
				p.setName("deleteStaff");
				p.setAssociatedProperty(modelService.getProperty("base_staff_id"));
				p.setAssociatedType(Property.ONE_TO_ONE);
				p.setType(DbColumnType.OBJECT);
				modelService.saveProperty(p);
			}
			if("effectStaffId".equals(p.getName())) {
				p.setName("effectStaff");
				p.setAssociatedProperty(modelService.getProperty("base_staff_id"));
				p.setAssociatedType(Property.ONE_TO_ONE);
				p.setType(DbColumnType.OBJECT);
				modelService.saveProperty(p);
			}
			if("positionId".equals(p.getName())) {
				p.setName("position");
				p.setAssociatedProperty(modelService.getProperty("base_position_id"));		
				p.setAssociatedType(Property.ONE_TO_ONE);
				p.setType(DbColumnType.OBJECT);
				modelService.saveProperty(p);
			}
			if("ownerDepartmentId".equals(p.getName())) {
				p.setName("ownerDepartment");
				p.setAssociatedProperty(modelService.getProperty("base_department_id"));
				p.setAssociatedType(Property.ONE_TO_ONE);
				p.setType(DbColumnType.OBJECT);
				modelService.saveProperty(p);
			}
			if("ownerPositionId".equals(p.getName())) {
				p.setName("ownerPosition");
				p.setAssociatedProperty(modelService.getProperty("base_position_id"));
				p.setAssociatedType(Property.ONE_TO_ONE);
				p.setType(DbColumnType.OBJECT);
				modelService.saveProperty(p);
			}
			if("ownerStaffId".equals(p.getName())) {
				p.setName("ownerStaff");
				p.setAssociatedProperty(modelService.getProperty("base_staff_id"));
				p.setAssociatedType(Property.ONE_TO_ONE);
				p.setType(DbColumnType.OBJECT);
				modelService.saveProperty(p);
			}
			
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void transformLayoutView(View view) {
		Map configMap = (Map) SerializeUitls.deserialize(view.getExtraView().getConfig());
		if (configMap != null && !configMap.isEmpty()) {
			Map<String, Object> layMap = (Map<String, Object>) configMap.get("layout");
			if (null != layMap) {
				for (Map.Entry<String, Object> entry : layMap.entrySet()) {
					if (null != ((Map<String, Object>) entry.getValue()).get("vcode") && !"".equals(((Map<String, Object>) entry.getValue()).get("vcode"))) {
						String viewCode = (String) ((Map<String, Object>) entry.getValue()).get("vcode");
						try {
							View v = viewService.getView(viewCode);
							if(null != v) {
								((Map<String, Object>) entry.getValue()).put("vid", v.getCode());
							}
						}catch(Exception e){
							continue;
						}
					}
				}
			}
		}
		view.getExtraView().setConfig(ConditionUtil.serialize(configMap));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void transformEditView(View view) {
		Map configMap = (Map) SerializeUitls.deserialize(view.getExtraView().getConfig());

		if (configMap != null && !configMap.isEmpty()) {
			List<Map> tabs = (List<Map>) configMap.get("tabs");
			if (tabs != null && !tabs.isEmpty()) {
				for (Map tab : tabs) {
					List<Map> sections = (List<Map>) tab.get("sections");
					if (sections != null && !sections.isEmpty()) {
						for (Map section : sections) {
							Map content = (Map) section.get("content");
							if (content != null && !content.isEmpty()) {
								List<Map> forms = (List<Map>) content.get("form");
								if (forms != null && !forms.isEmpty()) {
									for (Map form : forms) {
										Map element = (Map) form.get("element");
										if (element == null) {
											continue;
										}
										String referenceview = (String) element.get("referenceview");
										if (referenceview != null && referenceview.length() > 0) {
											try {
												View v = viewService.getView(referenceview);
												if(null != v) {
													element.put("referenceview", v.getCode());
												}
											} catch (Exception e) {
												continue;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		List<DataGrid> dataGrid = dataGridService.getDataGridByView(view, false);
		if (null != dataGrid) {
			for (DataGrid dg : dataGrid) {
				Map dgConfigMap = (Map) SerializeUitls.deserialize(dg.getConfig());
				if (dgConfigMap != null && !dgConfigMap.isEmpty()) {
					List<Map> columns = (List<Map>) configMap.get("columns");
					if (columns != null && !columns.isEmpty()) {
						for (Map column : columns) {
							String referenceview = (String) column.get("referenceview");
							if (referenceview != null && referenceview.length() > 0) {
								try {
									View v = viewService.getView(referenceview);
									if(null != v) {
										column.put("referenceview", v.getCode());
									}
								} catch (Exception e) {
									continue;
								}
							}
						}
					}
				}
				dg.setConfig(ConditionUtil.serialize(dgConfigMap));
				dataGridService.save(dg, view, dg.getTargetModel().getCode());
			}
		}
		view.getExtraView().setConfig(ConditionUtil.serialize(configMap));
	}
									
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void transformListView(View view) {
		Map configMap = (Map) SerializeUitls.deserialize(view.getExtraView().getConfig());

		if (configMap != null && !configMap.isEmpty()) {
			List<Map> operateButtons = (List<Map>) configMap.get("operateButtons");
			if (operateButtons != null && !operateButtons.isEmpty()) {
				for (Map button : operateButtons) {
					String viewSelect = (String) button.get("viewselect");
					if (viewSelect != null && viewSelect.length() > 0) {
						try {
							View v = viewService.getView(viewSelect);
							if(null != v) {
								button.put("viewselect", v.getCode());
							}
						} catch (Exception e) {
							continue;
						}
					}
				}
			}
			
			List<Map> fastsections = (List<Map>) configMap.get("fastsections");
			if (fastsections != null && !fastsections.isEmpty()) {
				for (Map section : fastsections) {
					Map content = (Map) section.get("content");
					if (content != null && !content.isEmpty()) {
						List<Map> forms = (List<Map>) content.get("form");
						if (forms != null && !forms.isEmpty()) {
							for (Map form : forms) {
								Map element = (Map) form.get("element");
								if (element == null) {
									continue;
								}
								String referenceview = (String) element.get("referenceview");
								if (referenceview != null && referenceview.length() > 0) {
									try {
										View v = viewService.getView(referenceview);
										if(null != v) {
											element.put("referenceview", v.getCode());
										}
									} catch (Exception e) {
										continue;
									}
								}
							}
						}
					}
				}
			}
		}
		view.getExtraView().setConfig(ConditionUtil.serialize(configMap));	
	}

	@ResponseBody
	@RequestMapping(value = "/ec/view/modifyStructure")
	public String modifyConfigStructure(@RequestParam("moduleCode") String moduleCode) throws Exception {
		ResponseMsg response = new ResponseMsg(true);
		try {
			viewService.backupViewConfig();
			ecConfigService.modifyConfiguration(moduleCode);
			response.setData(InternationalResource.get("ec.common.saveandclosesuccessful"));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			response.setSuccess(false);
			response.setData(InternationalResource.get("ec.common.unsuccessfully"));
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}


	@ResponseBody
	@RequestMapping(value = "/ec/view/modifyFastQueryConfig")
	public String modifyFastQueryConfig(@RequestParam("moduleCode") String moduleCode) throws Exception {
		ResponseMsg response = new ResponseMsg(true);
		try {
			viewService.backupViewConfig();
			ecConfigService.dealFieldByFastQueryDateAndButton(moduleCode);
			response.setData(InternationalResource.get("ec.common.saveandclosesuccessful"));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			response.setSuccess(false);
			response.setData(InternationalResource.get("ec.common.unsuccessfully"));
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}

	@ResponseBody
	@RequestMapping(value = "/ec/view/modifyFieldCode")
	public String modifyFieldCode(@RequestParam("moduleCode") String moduleCode) throws Exception {
		ResponseMsg response = new ResponseMsg(true);
		try{
			fieldService.modifyFieldCode(moduleCode);
			response.setData(InternationalResource.get("ec.common.saveandclosesuccessful"));
		}catch (Exception e) {
			log.error(e.getMessage(), e);
			response.setSuccess(false);
			response.setData(InternationalResource.get("ec.common.unsuccessfully"));
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}
	
	/**
	 * 处理党群与人力模块可空非空样式
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/view/dealHrAndPartCss")
	public String dealHrAndPartCss() throws Exception {
		ResponseMsg response = new ResponseMsg(true);
		try{
			ecConfigService.dealHrAndPartCss();
			response.setData(InternationalResource.get("ec.common.saveandclosesuccessful"));
		}catch (Exception e) {
			log.error(e.getMessage(), e);
			response.setSuccess(false);
			response.setData(InternationalResource.get("ec.common.unsuccessfully"));
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}


	@ResponseBody
	@RequestMapping(value = "/ec/view/transformCustomerCondition")
	public String transformCustomerCondition(@RequestParam("moduleCode") String moduleCode) throws Exception {
		ResponseMsg response = new ResponseMsg(true);
		try{
			viewService.transformCustomerCondition(moduleCode);
			response.setData(InternationalResource.get("ec.common.saveandclosesuccessful"));
		}catch(Exception e){
			log.error(e.getMessage(), e);
			response.setSuccess(false);
			response.setData(InternationalResource.get("ec.common.unsuccessfully"));
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}


	@ResponseBody
	@RequestMapping(value = "/ec/property/modifyMainAsso")
	public String modifyMainAsso() throws Exception {
		ResponseMsg response = new ResponseMsg(true);
		try{
			modelService.modifyMainAsso();
			response.setData(InternationalResource.get("ec.common.saveandclosesuccessful"));
		}catch(Exception e){
			log.error(e.getMessage(), e);
			response.setSuccess(false);
			response.setData(InternationalResource.get("ec.common.unsuccessfully"));
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}
	
	/**
	 * 处理布局视图按钮code
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/property/dealLayout")
	public String dealLayoutMenuOperate() throws Exception {
		ResponseMsg response = new ResponseMsg(true);
		try{
			EcUtils.generateInfoMap.set("dealLayoutMenuOperate");
			viewService.dealLayoutMenuOperate();
			response.setData(InternationalResource.get("ec.common.saveandclosesuccessful"));
		}catch(Exception e){
			log.error(e.getMessage(), e);
			response.setSuccess(false);
			response.setData(InternationalResource.get("ec.common.unsuccessfully"));
		}finally{
			EcUtils.generateInfoMap.remove();
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}

	@ResponseBody
	@RequestMapping(value = "/ec/pending/dealMobileApprovePending")
	public String dealMobileApprovePending(@RequestParam("processKey") String processKey, @RequestParam("processVersion") Integer processVersion) throws JsonProcessingException {
		ResponseMsg response = new ResponseMsg(true);
//		try{
//			taskService.dealMobileApprovePending(processKey, processVersion);
//			json.put("success", true);
//			response.setData(InternationalResource.get("ec.common.saveandclosesuccessful"));
//		}catch(Exception e){
//			log.error(e.getMessage(), e);
//			response.setSuccess(false);
//			response.setData(InternationalResource.get("ec.common.unsuccessfully"));
//		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}


	@ResponseBody
	@RequestMapping(value = "/public/deal/pending")
	public String dealPendingDesc() throws JsonProcessingException {
		ResponseMsg response = new ResponseMsg(true);
//		try {
//			taskService.dealPending();
//			json.put("success", true);
//			response.setData(InternationalResource.get("ec.common.saveandclosesuccessful"));
//		} catch (Exception e) {
//			response.setSuccess(false);
//			response.setData(InternationalResource.get("ec.common.unsuccessfully"));
//		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}

	@ResponseBody
	@RequestMapping(value = "/public/deal/deployment")
	public String dealDeploymentMenu() throws JsonProcessingException {
		ResponseMsg response = new ResponseMsg(true);
//		try {
//			taskService.dealDeployment();
//			json.put("success", true);
//			response.setData(InternationalResource.get("ec.common.saveandclosesuccessful"));
//		} catch (Exception e) {
//			response.setSuccess(false);
//			response.setData(InternationalResource.get("ec.common.unsuccessfully"));
//		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}
	
}
