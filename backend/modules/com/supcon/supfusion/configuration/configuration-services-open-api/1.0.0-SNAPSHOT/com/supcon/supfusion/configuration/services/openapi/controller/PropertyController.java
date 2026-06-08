/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 

 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.openapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.configuration.services.service.ModuleService;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.enums.FieldType;
import com.supcon.supfusion.configuration.services.enums.ShowFormat;
import com.supcon.supfusion.configuration.services.utils.ConditionUtil;
import com.supcon.supfusion.configuration.services.entity.Entity;
import com.supcon.supfusion.configuration.services.entity.Model;
import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.configuration.services.entity.Property;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.openapi.utils.DtoUtils;
import com.supcon.supfusion.configuration.services.openapi.utils.ResponseUtils;
import com.supcon.supfusion.configuration.services.openapi.vo.PropertyVO;
import com.supcon.supfusion.configuration.services.openapi.vo.ResponseMsg;
import com.supcon.supfusion.configuration.services.openapi.wrapper.PropertyWrapper;
import com.supcon.supfusion.configuration.services.service.EntityService;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.configuration.services.service.SpecialPermissionService;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.Restrictions;
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
 * 字段管理Action类<br>
 * 
 * @author songjiawei
 * @version 1.0
 */
@Slf4j
@Controller
public class PropertyController extends ConfigurationBaseController {

	private static final PropertyWrapper propertyWrapper = new PropertyWrapper();

	// ~ 页面上的变量 ===========================================================
	@Resource
	private ModelService modelService;
	
	@Resource
	private SpecialPermissionService specialPermissionService;
	@Resource
	private EntityService entityService;
	@Resource
	private ModuleService moduleService;
	@Value("${bap.company.single:false}")
	private Boolean isSingleMode = false;
	private static final Map<String, String> columnTypes = new LinkedHashMap<String, String>();
	private static final Map<String, String> formats = new LinkedHashMap<String, String>();
	private static final Map<String, String> fieldTypes = new LinkedHashMap<String, String>();
	static {
		for (DbColumnType type : DbColumnType.values()) {
			if("ITEMINDEX".equals(type.name())){
				continue;
			}
			columnTypes.put(type.toString(), type.getValue());
		}

		EnumMap<ShowFormat, String> showFormats = new EnumMap<ShowFormat, String>(ShowFormat.class);
		showFormats.put(ShowFormat.TEXT, "ec.property.text");
		showFormats.put(ShowFormat.EMAIL, "ec.property.email");
		showFormats.put(ShowFormat.MONEY, "ec.property.currency");
		showFormats.put(ShowFormat.URL, "ec.property.url");
		showFormats.put(ShowFormat.IP, "ec.property.ip");
		
		showFormats.put(ShowFormat.PERCENT, "ec.property.percent");
		// showFormats.put(ShowFormat.MONEY, "ec.property.currency");
		showFormats.put(ShowFormat.SYSTEMCODE, "ec.property.systemcode");
		// showFormats.put(ShowFormat.ENUMERATE, "ec.property.enumerate");
		showFormats.put(ShowFormat.SELECT, "ec.property.select");
		showFormats.put(ShowFormat.CHECKBOX, "ec.property.checkbox");

		showFormats.put(ShowFormat.THOUSAND, "ec.property.thousand");
		showFormats.put(ShowFormat.TEN_THOUSAND, "ec.property.ten_thousand");
		
		showFormats.put(ShowFormat.Y, "2000");
		showFormats.put(ShowFormat.YM, "2000-05");
		showFormats.put(ShowFormat.YMD, "2000-05-01");
		showFormats.put(ShowFormat.YMD_H, "2000-05-01 06");
		showFormats.put(ShowFormat.YMD_HM, "2000-05-01 06:09");
		showFormats.put(ShowFormat.YMD_HMS, "2000-05-01 06:09:06");
		showFormats.put(ShowFormat.HM, "06:09");
		showFormats.put(ShowFormat.HMS, "06:09:06");

		showFormats.put(ShowFormat.SELECTCOMP, "ec.property.selectcomp");
		showFormats.put(ShowFormat.PICTURE, "ec.property.picture");
		showFormats.put(ShowFormat.OFFICE, "ec.property.officeplugin");
		showFormats.put(ShowFormat.RADIO, "ec.property.radio");
		showFormats.put(ShowFormat.SELECTTAGNUMBER, "ec.property.selecttagnumber");
		showFormats.put(ShowFormat.LAYER, "ec.property.layer");
		//showFormats.put(ShowFormat.COLOR, "ec.property.color");
		showFormats.put(ShowFormat.HEX, "HEX");
		showFormats.put(ShowFormat.RGBA, "RGBA");
		showFormats.put(ShowFormat.HSLA, "HSLA");
		for (ShowFormat f : ShowFormat.values()) {
			if (null != f.toString() && !("" .equals(f.toString()))) {
				formats.put(f.toString(), showFormats.get(f));
			}
		}
		
		EnumMap<FieldType, String> fieldTypeEnums = new EnumMap<FieldType, String>(FieldType.class);
		fieldTypeEnums.put(FieldType.TEXTFIELD, "ec.view.property.text");
		fieldTypeEnums.put(FieldType.PASSWORDFIELD, "ec.view.property.password");
		fieldTypeEnums.put(FieldType.TEXTAREA, "ec.view.property.longtext");
		fieldTypeEnums.put(FieldType.SELECT, "ec.view.property.select");
		fieldTypeEnums.put(FieldType.DATE, "ec.view.property.date");
		fieldTypeEnums.put(FieldType.DATETIME, "ec.view.property.datetime");
		//fieldTypeEnums.put(FieldType.MULTSELECT, "ec.view.property.multiselect");
		//fieldTypeEnums.put(FieldType.OUTERSELECT, "ec.view.property.outerselect");
		//fieldTypeEnums.put(FieldType.AUTOCOMPLETE, "ec.view.property.auto");
		fieldTypeEnums.put(FieldType.RICHTEXT, "ec.view.property.richtext");
		fieldTypeEnums.put(FieldType.RADIO, "ec.view.property.radioField");
		fieldTypeEnums.put(FieldType.CHECKBOX, "ec.view.property.checkboxField");
		fieldTypeEnums.put(FieldType.LABEL, "ec.view.property.label");
		fieldTypeEnums.put(FieldType.SELECTCOMP, "ec.view.property.selectcomp");
		fieldTypeEnums.put(FieldType.DATAGRID, "ec.view.property.datagrid");
		fieldTypeEnums.put(FieldType.EASYTABLE, "ec.view.property.easytable");
		fieldTypeEnums.put(FieldType.PROPERTYATTACHMENT, "ec.view.property.attachment");
		fieldTypeEnums.put(FieldType.PICTURE, "ec.property.picture");
		fieldTypeEnums.put(FieldType.OFFICE, "ec.view.property.officeplugin");
		fieldTypeEnums.put(FieldType.SELECTTAGNUMBER, "ec.property.selecttagnumber");
		fieldTypeEnums.put(FieldType.LAYER, "ec.property.layer");
		fieldTypeEnums.put(FieldType.TIME, "ec.property.time");
		fieldTypeEnums.put(FieldType.COLOR, "ec.property.color");
		// fieldTypeEnums.put(FieldType.DATATABLE, "数据列表");
		// fieldTypeEnums.put(FieldType.DATATABLE, "数据列表");

		for (FieldType type : FieldType.values()) {
			fieldTypes.put(type.toString(), fieldTypeEnums.get(type));
		}
	}

	/**
	 * 进入字段管理列表
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/property/list")
	public Page<PropertyVO> list(@RequestParam("model.code") String modelCode, boolean showInherent, boolean showCustom,Integer pageNo, Integer pageSize) throws Exception {
		Model model = new Model();
		model.setCode(modelCode);
		Page<Property> properties=new Page<Property>(pageNo,pageSize);
		modelService.findProperties(properties, model, showInherent, showCustom);
		// List<Property> list=properties.getResult();

		return propertyWrapper.e2vPage(properties);
	}

	/**
	 * 进入字段选择
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/property/list-select")
	public List<PropertyVO> listselect(String modelCode, HttpServletRequest request) throws Exception {

		Model sourceModelCode = new Model();
		if (modelCode != null) {
			sourceModelCode = modelService.getModel(modelCode);
		}

		Model model = modelService.getModel(request.getParameter("model.code"));
		List<Property> listProperties = null;
		if(model != null && sourceModelCode != null && sourceModelCode.getCode() != null && !sourceModelCode.getIsMain() && !sourceModelCode.getEntity().equals(model.getEntity())){
			if (null != sourceModelCode.getType() && Model.TYPE_SQL == sourceModelCode.getType()) {
				listProperties = modelService.findProperties(Restrictions.eq("valid", true), Restrictions.eq("model.code", model.getCode()),
					Restrictions.or(Restrictions.and(Restrictions.eq("isUnique", true), Restrictions.eq("nullable", false)), Restrictions.eq("isPk", true)));
			} else {
				listProperties = modelService.findProperties(Restrictions.eq("valid", true), Restrictions.eq("model.code", model.getCode()),
						Restrictions.or(Restrictions.and(Restrictions.eq("isUnique", true), Restrictions.eq("nullable", false)), Restrictions.eq("isPk", true)),Restrictions.like("code", "%_id"));
			}
		} else if (model != null && model.getCode() != null) {
			if (model.getIsMain() && null != sourceModelCode && model.getEntity().equals(sourceModelCode.getEntity())) {
				listProperties = modelService.findProperties(Restrictions.eq("valid", true), Restrictions.eq("model.code", model.getCode()),
						Restrictions.eq("nullable", false), Restrictions.eq("isPk", true));
			} else {
				listProperties = modelService.findProperties(Restrictions.eq("valid", true), Restrictions.eq("model.code", model.getCode()),
						Restrictions.or(Restrictions.and(Restrictions.eq("isUnique", true), Restrictions.eq("nullable", false)), Restrictions.eq("isPk", true)));
			}
		}
		return propertyWrapper.e2vList(listProperties);
	}
	
	/**
	 * 选择所有字段
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/property/list-all-select")
	public List<PropertyVO> listAllSelect(HttpServletRequest request) throws Exception {
		String modelCode = request.getParameter("model.code");
		Model model = modelService.getModel(modelCode);
		if (null == model || null == model.getCode()) {
			return null;
		}
		List<Property> listProperties = modelService.findProperties(Restrictions.eq("valid", true), Restrictions.eq("model.code", model.getCode()));
		return propertyWrapper.e2vList(listProperties);
	}

	/**
	 * 进入字段选择
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/property/main-display-select")
	public List<PropertyVO> mainDisplayselect(@RequestParam("model.code") String modelCode) throws Exception {
		List<Property> mainDisplayProperties = modelService.findProperties(Restrictions.eq("valid", true), Restrictions.eq("model.code", modelCode),
				Restrictions.eq("isMainDisplay", true));
		if (mainDisplayProperties != null && mainDisplayProperties.size() > 0) {
			return propertyWrapper.e2vList(mainDisplayProperties);
		}
		return null;
	}
	
	/**
	 * 进入字段管理编辑页面
	 * 
	 * @return
	 */
	@RequestMapping(value = "/ec/property/edit")
	public String edit(ModelMap map, @RequestParam("entity.code") String entityCode, @Nullable @RequestParam("property.code") String propertyCode, @Nullable @RequestParam("model.code") String modelCode) throws Exception {
		Entity entity = entityService.getEntity(entityCode);
		Module module = entity.getModule();
		Property property = null;
		Model model = null;
		if (null != propertyCode && !"".equals(propertyCode)) {
			property = modelService.getProperty(propertyCode);
			model = modelService.getModel(property.getModel().getCode());
		} else {
			model = modelService.getModel(modelCode);
		}
		Map<String, Object> responseMap = new HashMap<String, Object>();
		responseMap.put("isProject", false);
		map.addAttribute("entity", entity);
		map.addAttribute("module", module);
		map.addAttribute("property", property);
		map.addAttribute("model", model);
		map.addAttribute("columnTypes", columnTypes);
		map.addAttribute("fieldTypes", fieldTypes);
		map.addAttribute("formats", formats);
		map.addAttribute("fieldTypesJson", internationalMapJson(fieldTypes));
		map.addAttribute("formatsJson", internationalMapJson(formats));
		map.addAttribute("modelCode", model.getCode());
		return "property/edit";
	}

	/**
	 * 进入编码格式配置页面
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/property/fill_content")
	public String fill_content(Property property) throws Exception {
		ResponseUtils.render(getResponse(), ResponseUtils.JSON_TYPE, property.getFillcontent());
		return null;
	}

	/**
	 * 进入编码格式配置页面
	 * 
	 * @return
	 */
	@RequestMapping({"/ec/property/summary_config", "/ec/property/code_config", "/static/property/code_config"})
	public String code_config(ModelMap map, @RequestParam("model.code") String modelCode) throws Exception {
		if(modelCode !=null) {
			Model model = modelService.getModelWithProperties(modelCode);
			map.addAttribute("model", model);
		}
		if ("/ec/property/summary_config".equals(getRequest().getRequestURI())) {
			return "property/summary_config";
		}
		return "property/code_config";
	}

	/**
	 * 获取字段显示名称
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping({"/ec/property/getDisplayNames", "/static/property/getDisplayNames"})
	public ResponseMsg getDisplayNames(@RequestParam("model.code") String modelCode, String propertiesNames) throws Exception {
		String language = null;
		try {
			language = getUserLanguage();
		} catch (Exception e) {
			language = InternationalResource.getDefaultLanguage();
			log.error("获取用户语言出错!", e);
		}
		List<String> displayNames = new ArrayList<>();
		if(modelCode!=null && propertiesNames != null && !propertiesNames.isEmpty()) {
			List<Property> propertyList = modelService.findProperties(propertiesNames, modelCode);
			for(Property p : propertyList) {
				displayNames.add(InternationalResource.get(p.getDisplayName()));
			}
		}
		ResponseMsg response = new ResponseMsg(true);
		response.setData(displayNames);
		return response;
	}

	/**
	 * 进入字段关联保存结果页面
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/property/save")
	public ResponseMsg save(HttpServletRequest request) throws Exception {
		Property property = DtoUtils.getPropertyVO(request);
		modelService.saveProperty(property);
		ResponseMsg response = new ResponseMsg(true);
		return response;
	}




	/**
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/property/formatColumnName")
	public Map formatColumnName(HttpServletRequest request) throws Exception {
		String name = request.getParameter("property.name");
		Property property=new Property();
		property.setName(name);

		String columnName = property.getColumnName();
		Map<String, Object> responseMap = new HashMap<String, Object>();
		responseMap.put("columnName", columnName);
		return responseMap;
	}
	
	/**
	 * 进入字段关联保存结果页面
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/property/delete")
	public ResponseMsg delete(HttpServletRequest request) throws Exception {
		Property propertyDTO = DtoUtils.getPropertyVO(request);
		return deleteChoise(propertyDTO, true);
	}
	
	/**
	 * 进入字段关联保存结果页面
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/property/ordinaryDelete")
	public ResponseMsg ordinaryDelete(Property property) throws Exception {
		return deleteChoise(property, false);
	}
	
	/**
	 * 根据菜单目录Code获取菜单字段(排序)
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/ec/property/sortitem")
	public String infoSetColOrder(ModelMap map, @RequestParam("model.code")String modelCode, @RequestParam("openType")String openType) {
		Model model = null;
		if (modelCode != null) {
			model = modelService.getModel(modelCode);
		}
		if (model != null && model.getCode() != null) {
			List<Property> propertyList = modelService.findProperties(Restrictions.eq("valid", true), Restrictions.eq("model.code", model.getCode()),Restrictions.eq("isCustom", false), Restrictions.eq("isHidden", false));
			map.addAttribute("propertyList", propertyList);
			Map<String,String> Parameters=new HashMap<String,String>();
			Parameters.put("openType",openType);
			map.addAttribute("Parameters", Parameters);
		}
		return "property/propertySortitem";
	}
	/**
	 * 字段排序 save
	 * @author mkp
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/property/sortitem/orderModelColSave")
	public Map orderModelColSave(String orderModelCol) {
		Map<String, Object> responseMap = new HashMap<String, Object>();
		if (orderModelCol != null) {
			modelService.saveOrderModeltCol(orderModelCol);
			responseMap.put("dealSuccessFlag", true);
		}
		return responseMap;
	}
	
	private ResponseMsg deleteChoise(Property property, boolean flag) throws JsonProcessingException {
		ResponseMsg response = new ResponseMsg(true);
		ObjectMapper mapper = new ObjectMapper();
		if (null == property) {
			response.setSuccess(false);
			response.setExceptionMsg(InternationalResource.get("ec.action.modelAction.notFound"));
		} else {
			if(specialPermissionService.checkWhetherIsExist(property.getCode()))  {
				response.setSuccess(false);
				response.setExceptionMsg(InternationalResource.get("ec.action.related.SpecialPermission"));
//				return mapper.writeValueAsString(response);
				return response;
			}
			//add by yubo20171214
			if(flag) {
				String responseMsg = modelService.deletePropertyPhysical(property.getCode(), false,false);
				//
				if(responseMsg!=null&&responseMsg.length()!=0&&(!("".equals(responseMsg)))){
					response.setSuccess(false);
					response.setExceptionMsg(responseMsg);
//					return mapper.writeValueAsString(response);
					return response;
				}
				//specialPermissionService.deleteSpecialPermission(property.getCode(),moduleCode, true);
			} else {
				String responseMsg = modelService.deleteProperty(property);
				if(responseMsg!=null && responseMsg.length()!=0&&(!("".equals(responseMsg)))){
					response.setSuccess(false);
					response.setExceptionMsg(responseMsg);
//					return mapper.writeValueAsString(response);
					return response;
				}
				//specialPermissionService.deleteSpecialPermission(property.getCode(),moduleCode, false);
			}
		}
//		return mapper.writeValueAsString(response);
		return response;
	}
	
	@RequestMapping(value = "/ec/property/editCustomProps")
	public String editCustomProps(ModelMap map,@Nullable @RequestParam("model.code") String modelCode) {
		Model model = new Model();
		model.setCode(modelCode);
		map.addAttribute("model", model);
		return "property/editCustomProps";
	}

	@ResponseBody
	@RequestMapping(value = "/ec/property/saveCustomProps")
	public Map saveCustomProps(@RequestParam("model.code")String modelCode, Integer charParamAmount, Integer intParamAmount, Integer floatParamAmount, Integer dateParamAmount,
								  Integer codeParamAmount, Integer objParamAmount, String colPrefix) {
		Map<String, Object> responseMap = new HashMap<String, Object>();
		if (modelCode != null) {
			modelService.createCustomProps(modelCode, charParamAmount, intParamAmount, floatParamAmount, dateParamAmount, codeParamAmount, objParamAmount, colPrefix, getCurrentStaff().getId());
			responseMap.put("dealSuccessFlag", true);
		} else {
			responseMap.put("dealSuccessFlag", false);
		}
		return responseMap;
	}
	
	/**
	 * 生成固有字段
	 * 
	 * @return
	 */
	@RequestMapping(value = "/ec/property/addInherent")
	public void addInherent(@RequestParam("model.code")String modelCode) throws Exception {
		Model model = new Model();
		model.setCode(modelCode);
		modelService.createInherentProperties(model);
	}
	
	/**
	 * 处理字段显示类型与显示格式
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/property/init-property")
	public String modifyPropertyFieldType(String moduleCode) throws JsonProcessingException {
		ResponseMsg response = new ResponseMsg(true);
		ObjectMapper mapper = new ObjectMapper();
		try {
			modelService.modifyPropertyFieldType(moduleCode);
			response.setSuccess(true);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			response.setSuccess(false);
		}
		return mapper.writeValueAsString(response);
	}
	
	private String internationalMapJson(Map<String, String> map){
		Map<String, String> internal = new HashMap<String, String>();
		for(Map.Entry<String, String> entry : map.entrySet()) {
			internal.put(entry.getKey(), InternationalResource.get(entry.getValue()));
		}
		return ConditionUtil.serialize(internal);
	}


}