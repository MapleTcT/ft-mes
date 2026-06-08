/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.openapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.supcon.supfusion.configuration.services.utils.PropertyHolder;
import com.supcon.supfusion.configuration.services.utils.DbUtils;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.enums.FieldType;
import com.supcon.supfusion.configuration.services.enums.ShowFormat;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.utils.StringUtils;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.openapi.utils.DtoUtils;
import com.supcon.supfusion.configuration.services.openapi.vo.ModelVO;
import com.supcon.supfusion.configuration.services.openapi.vo.ResponseMsg;
import com.supcon.supfusion.configuration.services.openapi.wrapper.ModelWrapper;
import com.supcon.supfusion.configuration.services.service.*;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 模型管理Action类<br>
 * 
 * @author songjiawei
 * @version 1.0
 */
@Slf4j
@Controller
//@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "${spring.application.name}" + HttpConstants.URL_SPLITER + "v1/ec/scheduler")
public class ModelController extends ConfigurationBaseController {

	private static final ModelWrapper modelWrapper = new ModelWrapper();

	// ~ 所需要的service =======================================================
	@Resource
	private ModelService modelService;
	@Resource
	private EntityService entityService;
	@Resource
	private SpecialPermissionService specialPermissionService;
	@Resource
	private ViewService viewService;
	@Resource
	private PropertyService propertyService;

	
	@Value("${bap.company.single:false}")
	private Boolean isSingleMode = false;
	
	/**
	 * 进入模型管理主框架
	 * 
	 * @return
	 */
	@RequestMapping(value = "/ec/model/manage")
	public String manage(ModelMap map, @RequestParam("entity.code") String entityCode) throws Exception {
		Entity entity = entityService.getEntity(entityCode);
		map.addAttribute("entity", entity);
		return "model/manage";
	}
	/**
	 * 左边树列表
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping({"/ec/model/list", "/ec/model/list-select"})
	public List<ModelVO> list(@RequestParam("entity.code") String entityCode) throws Exception {
		Entity entity = new Entity();
		entity.setCode(entityCode);
		List<Model> models = modelService.findModels(entity);
		for (Model model : models) {
			if (null != model.getType() && Model.TYPE_SQL == model.getType()) {
				model.setIconSkin(Model.ICONSKIN_SQL);
			}
		}
		return modelWrapper.e2vList(models);
	}

	/**
	 * 进入模型管理编辑页面
	 * 
	 * @return
	 */
	@RequestMapping("/ec/model/edit")
	public String edit(ModelMap map, @RequestParam("entity.code") String entityCode,
					   @Nullable @RequestParam("model.code") String modelCode) throws Exception {
		Entity entity = entityService.getEntity(entityCode);
		Module module=entity.getModule();
		Map<String, Object> objectProperties = null;
		List<SpecialPermission>  specialAuthorityList = null;
		String  specialPermissionPreview = null;
		Map<String, Object> responseMap = new HashMap<String, Object>();
		Model model = null;
		if (!StringUtils.isEmpty(modelCode)) {
			model = modelService.getModel(modelCode);
			model.setEntity(entity);
			model.setProperties(null);
			objectProperties=specialPermissionService.getObjectProperties(model);
			specialAuthorityList=specialPermissionService.findAllSpecialPermissionByModelCode(model.getCode());
			specialPermissionPreview=specialPermissionService.generatePreview(model.getCode());
			responseMap.put("isProject", false);
//            map.addAttribute("model", model);
            map.addAttribute("objectProperties", objectProperties);
            map.addAttribute("specialAuthorityList", specialAuthorityList);
            map.addAttribute("specialPermissionPreview", specialPermissionPreview);
		} else {
			responseMap.put("firstIsMain", modelService.firstIsMain(entity));
		}
		map.addAttribute("model", model);
		map.addAttribute("entity", entity);
		map.addAttribute("module", module);
		map.addAttribute("responseMap", responseMap);
		return "model/edit";
	}
	
	
	
	
	
	/**
	 * 进入模型管理保存结果页面
	 * 
	 * @return
	 */
	/**
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/model/save")
	public ResponseMsg save(String xmlString, HttpServletRequest request) throws Exception {
		Model model = DtoUtils.getModelVO(request);
		prepare(model);
		if (xmlString != null && xmlString.length() > 0) {
			model.setIsConfigSpecial(true);
		} else {
			model.setSpecialPerTemplateSQL(null);
			model.setIsConfigSpecial(false);
		}
		modelService.saveModel(model);
		if (xmlString != null && xmlString.length() > 0) {
			specialPermissionService.saveXmlSource(xmlString, model.getCode());
		} else {
			specialPermissionService.deleteSpcialPermissionByModelCode(model.getCode());
		}
		ResponseMsg response = new ResponseMsg(true);
		return response;
	}



	/**
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/model/formatTableName")
	public Map formatTableName(HttpServletRequest request) throws Exception {
		Model model = new Model();
		String code = request.getParameter("entity.code");
		Entity entity = entityService.getEntity(code);
		entity.setCode(code);
		model.setEntity(entity);

		String modelName = request.getParameter("model.modelName");
		model.setModelName(modelName);

		prepare(model);
		String tableName = model.getTableName();
		Map<String, Object> responseMap = new HashMap<String, Object>();
		responseMap.put("tableName", tableName);
		return responseMap;
	}

	/**
	 * 模型管理删除结果页面
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/model/delete")
	public ResponseMsg delete(HttpServletRequest request) throws Exception {
		Model model = new Model();
		model.setCode(request.getParameter("model.code"));
		if(!StringUtils.isEmpty(request.getParameter("model.version"))){
			model.setVersion(Integer.parseInt(request.getParameter("model.version")));
		}
		return deleteChoise(true, model);
	}
	
	/**
	 * 模型管理删除结果页面
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/model/ordinaryDelete")
	public ResponseMsg ordinaryDelete(Model model) throws Exception {
		return deleteChoise(false, model);
	}
	
	private ResponseMsg deleteChoise(boolean flag, Model model) throws JsonProcessingException {
		ResponseMsg response = new ResponseMsg(true);
		if (null == model) {
			response.setExceptionMsg(InternationalResource.get("ec.action.modelAction.notFound"));
		} else {
			if(flag) {
				//add by yubo20171214
				String responseMsg = modelService.deleteModelPhysical(model.getCode(), false);
				if(responseMsg!=null && responseMsg.length()!=0&&(!("".equals(responseMsg)))){
					response.setSuccess(false);
					response.setExceptionMsg(responseMsg);
				}
			} else {
				String responseMsg = modelService.deleteModel(model);
				if(responseMsg!=null && responseMsg.length()!=0&&(!("".equals(responseMsg)))){
					response.setSuccess(false);
					response.setExceptionMsg(responseMsg);
				}
			}
		}
//		ObjectMapper mapper = new ObjectMapper();
//		return mapper.writeValueAsString(response);
		return response;
	}


	/**
	 * 获取model
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/model/get")
	public ModelVO get(@RequestParam("model.code") String modelCode) throws Exception {
		return modelWrapper.e2v(modelService.getModel(modelCode));
	}
	
	public void prepare(Model model) throws Exception {
		if (model != null && null != model.getCode() && model.getCode().length() > 0) {
			String orgTableName = model.getOrgTableName();
			String modelCode = model.getCode();
			Model modelPO = modelService.getModel(modelCode);
			model.setOrgTableName(orgTableName);
			model.setDataType(modelPO.getDataType());
		}
	}

}
