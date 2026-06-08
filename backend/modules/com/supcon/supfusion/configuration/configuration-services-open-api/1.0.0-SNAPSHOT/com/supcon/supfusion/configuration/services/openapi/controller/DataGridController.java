/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 

 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.openapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.supcon.supfusion.base.entities.SystemCode;
import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.enums.FieldType;
import com.supcon.supfusion.configuration.services.enums.ShowFormat;
import com.supcon.supfusion.configuration.services.enums.ViewType;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.openapi.utils.DtoUtils;
import com.supcon.supfusion.configuration.services.openapi.vo.ResponseMsg;
import com.supcon.supfusion.configuration.services.service.*;
import com.supcon.supfusion.configuration.services.utils.ConditionUtil;
import com.supcon.supfusion.configuration.services.utils.SerializeUitls;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * 视图管理Action类<br>
 * 
 * @author tanzhengyang
 * @version 1.0
 */
@Slf4j
@Controller
//@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "${spring.application.name}" + HttpConstants.URL_SPLITER + "v1")
public class DataGridController extends ConfigurationBaseController {

	private static final long serialVersionUID = -5666736192237846554L;
	private static final String layerSystemEntityCode = "BASE_CONFIG_LAYERS";
	// ~ 所需要的service =======================================================
	@Resource
	private ViewService viewService;
	@Resource
	private ModelService modelService;
	@Resource
	private DataGridService dataGridService;
	@Autowired
	private SystemCodeService systemCodeService;

	@Resource
	private PropertyService propertyService;


	// ~ 页面上的变量 ===========================================================
	/** 关联模型 */
	@Value("${bap.company.single:false}")
	private Boolean isSingleMode = false;
	
	private static final Map<String, String> fieldTypes = new LinkedHashMap<String, String>();
	private static final Map<String, String> viewColumnTypes = new LinkedHashMap<String, String>();
	private static final Map<String, String> viewFormats = new LinkedHashMap<String, String>();
	static {

		EnumMap<DbColumnType, String> dbColumnTypes = new EnumMap<DbColumnType, String>(DbColumnType.class);
		dbColumnTypes.put(DbColumnType.TEXT, "ec.property.char");
		dbColumnTypes.put(DbColumnType.INTEGER, "ec.property.integer");
		dbColumnTypes.put(DbColumnType.LONG, "ec.property.longInt");
		dbColumnTypes.put(DbColumnType.LONGTEXT, "ec.property.longText");
		dbColumnTypes.put(DbColumnType.BOOLEAN, "ec.property.boolean");
		dbColumnTypes.put(DbColumnType.DATE, "ec.property.date");
		dbColumnTypes.put(DbColumnType.TIME, "ec.property.time");
		dbColumnTypes.put(DbColumnType.DATETIME, "ec.property.datetime");
		dbColumnTypes.put(DbColumnType.DECIMAL, "ec.property.decimal");
		// dbColumnTypes.put(DbColumnType.BINARY, "ec.property.binary");
		dbColumnTypes.put(DbColumnType.MONEY, "ec.property.money");
		dbColumnTypes.put(DbColumnType.OBJECT, "ec.property.object");
		dbColumnTypes.put(DbColumnType.PASSWORD, "ec.property.password");
		dbColumnTypes.put(DbColumnType.SYSTEMCODE, "ec.property.systemcode");
		dbColumnTypes.put(DbColumnType.ENUMERATE, "ec.property.enumerate");
		dbColumnTypes.put(DbColumnType.PROPERTYATTACHMENT, "ec.property.attachment");
		dbColumnTypes.put(DbColumnType.BAPCODE, "ec.property.bapcode");
		dbColumnTypes.put(DbColumnType.TAGNUMBER, "ec.property.tagnumber");
		dbColumnTypes.put(DbColumnType.LAYER, "ec.property.layer");
		dbColumnTypes.put(DbColumnType.COLOR, "ec.property.color");
		dbColumnTypes.put(DbColumnType.SUMMARY, "ec.property.summary");

		for (DbColumnType type : DbColumnType.values()) {
			viewColumnTypes.put(type.toString(), dbColumnTypes.get(type));
		}

		EnumMap<ShowFormat, String> showFormats = new EnumMap<ShowFormat, String>(ShowFormat.class);
		showFormats.put(ShowFormat.TEXT, "ec.property.text");
		showFormats.put(ShowFormat.EMAIL, "ec.property.email");
		showFormats.put(ShowFormat.URL, "ec.property.url");
		showFormats.put(ShowFormat.IP, "ec.property.ip");
		// showFormats.put(ShowFormat.MONEY, "ec.property.currency");
		showFormats.put(ShowFormat.SYSTEMCODE, "ec.property.systemcode");
		// showFormats.put(ShowFormat.ENUMERATE, "ec.property.enumerate");
		showFormats.put(ShowFormat.SELECT, "ec.property.select");
		showFormats.put(ShowFormat.CHECKBOX, "ec.property.checkbox");

		showFormats.put(ShowFormat.THOUSAND, "ec.property.thousand");
		showFormats.put(ShowFormat.TEN_THOUSAND, "ec.property.ten_thousand");
		showFormats.put(ShowFormat.PERCENT, "ec.property.percent");

		showFormats.put(ShowFormat.Y, "2000");
		showFormats.put(ShowFormat.YM, "2000-05");
		showFormats.put(ShowFormat.YMD, "2000-05-01");
		showFormats.put(ShowFormat.YMD_H, "2000-05-01 06");
		showFormats.put(ShowFormat.YMD_HM, "2000-05-01 06:09");
		showFormats.put(ShowFormat.YMD_HMS, "2000-05-01 06:09:06");
		showFormats.put(ShowFormat.HM, "06:09");
		showFormats.put(ShowFormat.HMS, "06:09:06");

		showFormats.put(ShowFormat.SELECTCOMP, "ec.property.selectcomp");

		showFormats.put(ShowFormat.RADIO, "ec.view.property.radio");
		
		showFormats.put(ShowFormat.SELECTTAGNUMBER, "ec.property.selecttagnumber");
		showFormats.put(ShowFormat.LAYER, "ec.property.layer");
//		showFormats.put(ShowFormat.COLOR, "ec.property.color");
		showFormats.put(ShowFormat.HEX, "HEX");
		showFormats.put(ShowFormat.RGBA, "RGBA");
		showFormats.put(ShowFormat.HSLA, "HSLA");
		for (ShowFormat f : ShowFormat.values()) {
			if (null != f.toString() && !("" .equals(f.toString()))) {
				if (showFormats.get(f) != null) {
					viewFormats.put(f.toString(), showFormats.get(f));
				}
			}
		}

		EnumMap<FieldType, String> fieldTypeEnums = new EnumMap<FieldType, String>(FieldType.class);
		fieldTypeEnums.put(FieldType.TEXTFIELD, "ec.view.property.text");
		fieldTypeEnums.put(FieldType.PASSWORDFIELD, "ec.view.property.password");
		fieldTypeEnums.put(FieldType.TEXTAREA, "ec.view.property.longtext");
		fieldTypeEnums.put(FieldType.SELECT, "ec.view.property.select");
		fieldTypeEnums.put(FieldType.DATE, "ec.view.property.date");
		fieldTypeEnums.put(FieldType.DATETIME, "ec.view.property.datetime");
		// fieldTypeEnums.put(FieldType.MULTSELECT, "ec.view.property.multiselect");
		// fieldTypeEnums.put(FieldType.OUTERSELECT, "ec.view.property.outerselect");
		// fieldTypeEnums.put(FieldType.AUTOCOMPLETE, "ec.view.property.auto");
		fieldTypeEnums.put(FieldType.RICHTEXT, "ec.view.property.richtext");
		fieldTypeEnums.put(FieldType.RADIO, "ec.view.property.radioField");
		fieldTypeEnums.put(FieldType.CHECKBOX, "ec.view.property.checkboxField");
		// fieldTypeEnums.put(FieldType.LABEL, "ec.view.property.label");
		fieldTypeEnums.put(FieldType.SELECTCOMP, "ec.view.property.selectcomp");
		// fieldTypeEnums.put(FieldType.DATAGRID, "ec.view.property.datagrid");
		fieldTypeEnums.put(FieldType.COLOR, "ec.property.color");
		// fieldTypeEnums.put(FieldType.DATATABLE, "数据列表");
		// fieldTypeEnums.put(FieldType.DATATABLE, "数据列表");
		
		fieldTypeEnums.put(FieldType.SELECTTAGNUMBER, "ec.property.selecttagnumber");
		fieldTypeEnums.put(FieldType.TIME, "ec.property.time");

		for (FieldType type : FieldType.values()) {
			fieldTypes.put(type.toString(), fieldTypeEnums.get(type));
		}
	}

	/**
	 * DataGrid中应用
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping({"/ec/view/datatable_config", "/ec/view/datatable_config_readonly"})
	public String datatableConfig(ModelMap map, HttpServletRequest request, @RequestParam("model.code") String modelCode, @RequestParam("dataGrid.code") String dataGridCode) {
		Boolean isReadOnlyMode = false;
		if(request.getRequestURI().equals("/ec/view/datatable_config_readonly")){
			isReadOnlyMode = true;
		}
		Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
		if(null!=isProj && isProj){
			ProjectFlagHolder.getInstance().getProjFlag().set(true);
		}
		String delCellIds = "";
		String btDelCellIds = "";
		Model m = modelService.getModelWithProperties(modelCode);
		Map<String, List<Property>> assMap = null;
		if (null != m) {
			assMap = new HashMap<String, List<Property>>();
			List<AssociatedInfo> list = modelService.findAssociatedInfos(m, 1, 2);
			List<Property> propertyList = new ArrayList<Property>();
			if (!list.isEmpty()) {
				for (AssociatedInfo ass : list) {
					for (Property p : m.getProperties()) {
						if (p.getIsUsedForList() && p.getAssociatedProperty() != null) {
							if (ass.getOriginalProperty().getCode().equals(p.getCode())) {
								Model model = null;
								List<AssociatedInfo> assos = modelService.findAssociatedInfos(modelService.getProperty(p.getCode()));
								if (assos != null) {
									for (AssociatedInfo asso : assos) {
										if ((asso.getType() == AssociatedInfo.ONE_TO_ONE || asso.getType() == AssociatedInfo.MANY_TO_ONE)
												&& asso.getOriginalProperty().getCode().equals(p.getCode())) {
											model = asso.getTargetProperty().getModel();
											break;
										}
									}
								}
								propertyList = modelService.findProperties(model);
								assMap.put(p.getCode(), propertyList);
							}
						} else if (p.getIsUsedForList() && p.getIsCustom() && DbColumnType.OBJECT.equals(p.getType())) {
							// 对象类型自定义字段
							CustomPropertyModelMapping customPropertyModelMapping = modelService.getAssociatedCustomPropertyModelMapping(p.getCode());
		        			if (null != customPropertyModelMapping) {
		        				Property associatedProperty = customPropertyModelMapping.getAssociatedProperty();
		        				p.setAssociatedProperty(customPropertyModelMapping.getAssociatedProperty());
		        				Model model = associatedProperty.getModel();
		        				propertyList = modelService.findProperties(model);
		        				assMap.put(p.getCode(), propertyList);
		        			}
						}
					}
				}
			}
		}
		List<Property> properties = propertyService.getProperties(m.getCode());
		DataGrid dataGrid = dataGridService.getDataGrid(dataGridCode);
		View view = dataGrid.getView();
		List<View> viewList = viewService.findViews(view.getEntity(), 1, 3, 0, ViewType.EDIT,ViewType.EXTRA);
		String dgConfig = dataGridService.getDataGridFullConfig(dataGrid);
		ExtraView ev = new ExtraView();
		/*
		 * if(dataGrid!=null && dataGrid.getConfig()!=null && dataGrid.getConfig().length() > 0 ){
		 * 
		 * dataGrid.setConfig(dgConfig);
		 * }
		 */
		Map<String, Property> propertyMap = null;
		if (null != dgConfig && dgConfig.length() > 0) {
			propertyMap = viewService.getPropertyMap(dgConfig);
		}
		ev.setConfigMap((Map) SerializeUitls.deserialize(dgConfig));
		
		// 一对多
		Map<String, Object> indirectMap = new HashMap<String, Object>();
		List<AssociatedInfo> assos = modelService.findAssociatedInfos(dataGrid.getTargetModel(), AssociatedInfo.ONE_TO_MANY);
		List<AssociatedInfo> directAssos = new ArrayList<AssociatedInfo>();
		List<Model> models = modelService.findModels(dataGrid.getTargetModel().getEntity());
		for (AssociatedInfo item : assos) {
			if (models.contains(item.getTargetProperty().getModel()) && item.getIsMainAssociated() != null && item.getIsMainAssociated()) {
				directAssos.add(item);
			}
		}
		Iterator<AssociatedInfo> it = directAssos.iterator();
		while (it.hasNext()) {
			AssociatedInfo item = it.next();
			if ("id".equalsIgnoreCase(item.getTargetProperty().getName())) {
				it.remove();
			} else {
				assos = modelService.findAssociatedInfoNotIncludeBackAsso(item.getTargetProperty().getModel(),
						AssociatedInfo.ONE_TO_ONE, AssociatedInfo.MANY_TO_ONE);
				if (assos == null || assos.isEmpty()) {
					it.remove();
				} else {
					Iterator<AssociatedInfo> ait = assos.iterator();
					while(ait.hasNext()) {
						AssociatedInfo ass = ait.next();
						if(ass.getIsMainAssociated() != null && ass.getIsMainAssociated()) {
							ait.remove();
						}
					}
					indirectMap.put("-" + item.getOriginalProperty().getCode() + "-" + item.getTargetProperty().getCode() + "-", assos);
				}
			}
		}
		Map<String, Object> multSelectInfo = Maps.newHashMap();
		multSelectInfo.put("directAssos", directAssos);
		String excludes = "*.companyStaffs,*.departmentWorks,*.positionWorks,*.models,*.entities,*.views,*.properties,*.class,*.createTime,*.createStaff,*.createStaffId,*.modifyStaff,*.modifyTime,*.modifyStaffId,*.deleteStaff,*.deleteStaffId,*.deleteTime,*.entity";
		multSelectInfo.put("directAssosJson", ConditionUtil.serialize(directAssos, null, excludes));
		multSelectInfo.put("indirectAssosJson", ConditionUtil.serialize(indirectMap, null, excludes));
		map.addAttribute("model", m);
		map.addAttribute("assMap", assMap);
		map.addAttribute("dataGrid", dataGrid);
		map.addAttribute("isProj",isProj);
		map.addAttribute("view", view);
		map.addAttribute("viewList", viewList);
		map.addAttribute("ev", ev);
		map.addAttribute("propertyMap", propertyMap);
		map.addAttribute("multSelectInfo", multSelectInfo);
		map.addAttribute("isReadOnlyMode", isReadOnlyMode);
		map.addAttribute("fieldTypes", fieldTypes);
		map.addAttribute("viewColumnTypes", viewColumnTypes);
		map.addAttribute("viewFormats", viewFormats);
		ProjectFlagHolder.getInstance().getProjFlag().set(false);
		return "datagrid/config-datatable";
	}
	
	/**
	 * DataGrid中应用
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping({"/ec/view/datagrid_config", "/ec/view/datagrid_config_readonly"})
	public String datagridConfig(ModelMap map, @RequestParam("property.code") String propertyCode,
								 @RequestParam("dataGrid.code") String dataGridCode) {
		Boolean isReadOnlyMode = false;
		if(getRequest().equals("/ec/view/datagrid_config_readonly")){
			isReadOnlyMode = true;
		}
		Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
		if(null!=isProj && isProj){
			ProjectFlagHolder.getInstance().getProjFlag().set(true);
		}
		String delCellIds = "";
		String btDelCellIds = "";
		Property property = modelService.getProperty(propertyCode);
		Model model = modelService.getModelWithProperties(property.getModel().getCode());
		Map<String, List<Property>> assMap = null;
		if (null != model) {
			assMap = new HashMap<String, List<Property>>();
			List<AssociatedInfo> list = modelService.findAssociatedInfos(model, 1, 2);
			List<Property> propertyList = new ArrayList<Property>();
			if (!list.isEmpty()) {
				for (AssociatedInfo ass : list) {
					for (Property p : model.getProperties()) {
						if (p.getIsUsedForList() && p.getAssociatedProperty() != null) {
							if (ass.getOriginalProperty().getCode().equals(p.getCode())) {
								Model m = null;
								List<AssociatedInfo> assos = modelService.findAssociatedInfos(modelService.getProperty(p.getCode()));
								if (assos != null) {
									for (AssociatedInfo asso : assos) {
										if ((asso.getType() == AssociatedInfo.ONE_TO_ONE || asso.getType() == AssociatedInfo.MANY_TO_ONE)
												&& asso.getOriginalProperty().getCode().equals(p.getCode())) {
											m = asso.getTargetProperty().getModel();
											break;
										}
									}
								}
								propertyList = modelService.findProperties(m);
								assMap.put(p.getCode(), propertyList);
							}
						} else if (p.getIsUsedForList() && p.getIsCustom() && DbColumnType.OBJECT.equals(p.getType())) {
							// 对象类型自定义字段
							CustomPropertyModelMapping customPropertyModelMapping = modelService.getAssociatedCustomPropertyModelMapping(p.getCode());
		        			if (null != customPropertyModelMapping) {
		        				Property associatedProperty = customPropertyModelMapping.getAssociatedProperty();
		        				p.setAssociatedProperty(customPropertyModelMapping.getAssociatedProperty());
		        				Model m = associatedProperty.getModel();
		        				propertyList = modelService.findProperties(m);
		        				assMap.put(p.getCode(), propertyList);
		        			}
						}
					}
				}
			}
		}
		DataGrid dataGrid = dataGridService.getDataGrid(dataGridCode);
		String dgConfig = dataGridService.getDataGridFullConfig(dataGrid);
		ExtraView ev = new ExtraView();
		/*
		 * if(dataGrid!=null && dataGrid.getConfig()!=null && dataGrid.getConfig().length() > 0 ){
		 * 
		 * dataGrid.setConfig(dgConfig);
		 * }
		 */
		Map<String, Property> propertyMap = null;
		if (null != dgConfig && dgConfig.length() > 0) {
			propertyMap = viewService.getPropertyMap(dgConfig);
		}
		ev.setConfigMap((Map) SerializeUitls.deserialize(dgConfig));
		
		// 一对多
		Map<String, Object> indirectMap = new HashMap<String, Object>();
		List<AssociatedInfo> assos = modelService.findAssociatedInfos(dataGrid.getTargetModel(), AssociatedInfo.ONE_TO_MANY);
		List<AssociatedInfo> directAssos = new ArrayList<AssociatedInfo>();
		List<Model> models = modelService.findModels(dataGrid.getTargetModel().getEntity());
		for (AssociatedInfo item : assos) {
			if (models.contains(item.getTargetProperty().getModel()) && item.getIsMainAssociated() != null && item.getIsMainAssociated()) {
				directAssos.add(item);
			}
		}
		Iterator<AssociatedInfo> it = directAssos.iterator();
		while (it.hasNext()) {
			AssociatedInfo item = it.next();
			if ("id".equalsIgnoreCase(item.getTargetProperty().getName())) {
				it.remove();
			} else {
				assos = modelService.findAssociatedInfoNotIncludeBackAsso(item.getTargetProperty().getModel(),
						AssociatedInfo.ONE_TO_ONE, AssociatedInfo.MANY_TO_ONE);
				if (assos == null || assos.isEmpty()) {
					it.remove();
				} else {
					Iterator<AssociatedInfo> ait = assos.iterator();
					while(ait.hasNext()) {
						AssociatedInfo ass = ait.next();
						if(ass.getIsMainAssociated() != null && ass.getIsMainAssociated()) {
							ait.remove();
						}
					}
					indirectMap.put("-" + item.getOriginalProperty().getCode() + "-" + item.getTargetProperty().getCode() + "-", assos);
				}
			}
		}
		Map<String, Object> multSelectInfo = new HashMap<String, Object>();
		multSelectInfo.put("directAssos", directAssos);
		String excludes = "*.companyStaffs,*.departmentWorks,*.positionWorks,*.models,*.entities,*.views,*.properties,*.class,*.createTime,*.createStaff,*.createStaffId,*.modifyStaff,*.modifyTime,*.modifyStaffId,*.deleteStaff,*.deleteStaffId,*.deleteTime,*.entity";
		multSelectInfo.put("directAssosJson", ConditionUtil.serialize(directAssos, null, excludes));
		multSelectInfo.put("indirectAssosJson", ConditionUtil.serialize(indirectMap, null, excludes));

		// 图层类型
		List<SystemCode> layerTypes = systemCodeService.getSystemCodeByEntity(layerSystemEntityCode);

		map.addAttribute("property", property);
		map.addAttribute("model", model);
		map.addAttribute("assMap", assMap);
		map.addAttribute("dataGrid", dataGrid);
		map.addAttribute("ev", ev);
		map.addAttribute("propertyMap", propertyMap);
		map.addAttribute("multSelectInfo", multSelectInfo);
		map.addAttribute("layerTypes", layerTypes);
		map.addAttribute("isReadOnlyMode", isReadOnlyMode);
		map.addAttribute("isProj", false);
		map.addAttribute("fieldTypes", fieldTypes);
		map.addAttribute("viewColumnTypes", viewColumnTypes);
		map.addAttribute("viewFormats", viewFormats);
		map.addAttribute("isProj",isProj);
		ProjectFlagHolder.getInstance().getProjFlag().set(false);
		if(dataGrid.getCode().contains("_mobile_")){
			return "datagrid/config-datagrid-mobile";
		}
		return "datagrid/config-datagrid";
	}
	
	/**
	 * DataGrid中应用
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping({"/ec/view/easytable_config", "/ec/view/easytable_config_readonly"})
	public String easytableConfig(ModelMap map, HttpServletRequest request, @RequestParam("property") Property property,
								  @RequestParam("dataGrid") DataGrid dataGrid) {
		Boolean isReadOnlyMode = false;
		if(request.getRequestURI().equals("/ec/view/easytable_config_readonly")){
			isReadOnlyMode = true;
		}
		Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
		if(null!=isProj && isProj){
			ProjectFlagHolder.getInstance().getProjFlag().set(true);
		}
		String delCellIds = "";
		String btDelCellIds = "";
		property = modelService.getProperty(property.getCode());
		Model model = modelService.getModelWithProperties(property.getModel().getCode());
		Map<String, List<Property>> assMap = null;
		if (null != model) {
			assMap = new HashMap<String, List<Property>>();
			List<AssociatedInfo> list = modelService.findAssociatedInfos(model, 1, 2);
			List<Property> propertyList = new ArrayList<Property>();
			if (!list.isEmpty()) {
				for (AssociatedInfo ass : list) {
					for (Property p : model.getProperties()) {
						if (p.getIsUsedForList() && p.getAssociatedProperty() != null) {
							if (ass.getOriginalProperty().getCode().equals(p.getCode())) {
								Model m = null;
								List<AssociatedInfo> assos = modelService.findAssociatedInfos(modelService.getProperty(p.getCode()));
								if (assos != null) {
									for (AssociatedInfo asso : assos) {
										if ((asso.getType() == AssociatedInfo.ONE_TO_ONE || asso.getType() == AssociatedInfo.MANY_TO_ONE)
												&& asso.getOriginalProperty().getCode().equals(p.getCode())) {
											m = asso.getTargetProperty().getModel();
											break;
										}
									}
								}
								propertyList = modelService.findProperties(m);
								assMap.put(p.getCode(), propertyList);
							}
						} else if (p.getIsUsedForList() && p.getIsCustom() && DbColumnType.OBJECT.equals(p.getType())) {
							// 对象类型自定义字段
							CustomPropertyModelMapping customPropertyModelMapping = modelService.getAssociatedCustomPropertyModelMapping(p.getCode());
		        			if (null != customPropertyModelMapping) {
		        				Property associatedProperty = customPropertyModelMapping.getAssociatedProperty();
		        				p.setAssociatedProperty(customPropertyModelMapping.getAssociatedProperty());
		        				Model m = associatedProperty.getModel();
		        				propertyList = modelService.findProperties(m);
		        				assMap.put(p.getCode(), propertyList);
		        			}
						}
					}
				}
			}
		}
		dataGrid = dataGridService.getDataGrid(dataGrid.getCode());
		String dgConfig = dataGridService.getDataGridFullConfig(dataGrid);
		ExtraView ev = new ExtraView();
		/*
		 * if(dataGrid!=null && dataGrid.getConfig()!=null && dataGrid.getConfig().length() > 0 ){
		 * 
		 * dataGrid.setConfig(dgConfig);
		 * }
		 */
		Map<String, Property> propertyMap = null;
		if (null != dgConfig && dgConfig.length() > 0) {
			propertyMap = viewService.getPropertyMap(dgConfig);
		}
		ev.setConfigMap((Map) SerializeUitls.deserialize(dgConfig));
		
		// 一对多
		Map<String, Object> indirectMap = new HashMap<String, Object>();
		List<AssociatedInfo> assos = modelService.findAssociatedInfos(dataGrid.getTargetModel(), AssociatedInfo.ONE_TO_MANY);
		List<AssociatedInfo> directAssos = new ArrayList<AssociatedInfo>();
		List<Model> models = modelService.findModels(dataGrid.getTargetModel().getEntity());
		for (AssociatedInfo item : assos) {
			if (models.contains(item.getTargetProperty().getModel()) && item.getIsMainAssociated() != null && item.getIsMainAssociated()) {
				directAssos.add(item);
			}
		}
		Iterator<AssociatedInfo> it = directAssos.iterator();
		while (it.hasNext()) {
			AssociatedInfo item = it.next();
			if ("id".equalsIgnoreCase(item.getTargetProperty().getName())) {
				it.remove();
			} else {
				assos = modelService.findAssociatedInfoNotIncludeBackAsso(item.getTargetProperty().getModel(),
						AssociatedInfo.ONE_TO_ONE, AssociatedInfo.MANY_TO_ONE);
				if (assos == null || assos.isEmpty()) {
					it.remove();
				} else {
					Iterator<AssociatedInfo> ait = assos.iterator();
					while(ait.hasNext()) {
						AssociatedInfo ass = ait.next();
						if(ass.getIsMainAssociated() != null && ass.getIsMainAssociated()) {
							ait.remove();
						}
					}
					indirectMap.put("-" + item.getOriginalProperty().getCode() + "-" + item.getTargetProperty().getCode() + "-", assos);
				}
			}
		}
		Map<String, Object> multSelectInfo = new HashMap<String, Object>();
		multSelectInfo.put("directAssos", directAssos);
		String excludes = "*.companyStaffs,*.departmentWorks,*.positionWorks,*.models,*.entities,*.views,*.properties,*.class,*.createTime,*.createStaff,*.createStaffId,*.modifyStaff,*.modifyTime,*.modifyStaffId,*.deleteStaff,*.deleteStaffId,*.deleteTime,*.entity";
		multSelectInfo.put("directAssosJson", ConditionUtil.serialize(directAssos, null, excludes));
		multSelectInfo.put("indirectAssosJson", ConditionUtil.serialize(indirectMap, null, excludes));
		map.addAttribute("property", property);
		map.addAttribute("model", model);
		map.addAttribute("assMap", assMap);
		map.addAttribute("dataGrid", dataGrid);
		map.addAttribute("ev", ev);
		map.addAttribute("propertyMap", propertyMap);
		map.addAttribute("multSelectInfo", multSelectInfo);
		ProjectFlagHolder.getInstance().getProjFlag().set(false);
		return "datagrid/config-easytable";
	}
	
	/**
	 * 视图中选择datagrid的关联模型
	 * 
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping("/ec/view/datagrid_save")
	public ResponseMsg datagrid_save(@RequestParam("view.code") String viewCode,
									 @RequestParam("dataGridType") @Nullable Integer dataGridType,
									 @RequestParam(value = "model.code",required = false) @Nullable String modelCode,
									 @RequestParam("property.code") @Nullable String propertyCode){
		Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
		if(null!=isProj && isProj){
			ProjectFlagHolder.getInstance().getProjFlag().set(true);
		}
		View view = viewService.getView(viewCode);
//		Property property = modelService.getProperty(view.getEntity().getCode());
		Model model = modelService.getModel(modelCode);
		Map<String, Object> DataGirdsource = new HashMap<String, Object>();
		DataGrid dataGrid = new DataGrid();
		view = viewService.getView(view.getCode());
		if(null!=isProj && isProj){
			dataGrid.setProjFlag(true);
		}
		if(dataGridType != null){
			dataGrid.setDataGridType(dataGridType);
		}
		if(null == dataGridType || dataGridType == 0){
			Property property = modelService.getProperty(propertyCode);
			model = property.getModel();
			dataGrid.setOrgProperty(property);
		}
		dataGridService.save(dataGrid, view, model.getCode());
		Map<String, String> assoriKeys = dataGridService.getAsskeyByTagmodelCode(model);
		String hideKey = "'id','version'";
		if (assoriKeys != null) {
			for (Entry<String, String> assKey : assoriKeys.entrySet()) {
				hideKey += ",'" + assKey.getKey() + "'";
			}
		}
		DataGirdsource.put("hideKey", hideKey);
		dataGrid.setTargetModel(model);
		DataGirdsource.put("datagrid", dataGrid);
//		DataGirdsource.put("targetModel", model);
		ResponseMsg response = new ResponseMsg(true);
		response.setData(DataGirdsource);
		ProjectFlagHolder.getInstance().getProjFlag().set(false);
		return response;
	}

	/**
	 * DataGrid的字段属性获取
	 * 
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/view/datagridJson")
	public String getDatagrid(@RequestParam("dataGrid") DataGrid dataGrid) throws Exception {
		Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
		if(null!=isProj && isProj){
			ProjectFlagHolder.getInstance().getProjFlag().set(true);
		}
		dataGrid = dataGridService.getDataGrid(dataGrid.getCode());
		String dataGridConfig = ConditionUtil.serialize(dataGridService.getDataGridFullConfig(dataGrid));
		ProjectFlagHolder.getInstance().getProjFlag().set(false);
		return dataGridConfig;
	}

	/**
	 * DataGrid的字段属性的保存
	 * 
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/view/datagrid_publish")
	public ResponseMsg datagridPublish(@RequestParam("fieldConfig") String fieldConfig, @RequestParam("delCellIds") String delCellIds,
								  @RequestParam("delEventIds") String delEventIds, @RequestParam("delValidateIds") String delValidateIds,
								  @RequestParam("btDelCellIds") String btDelCellIds, @RequestParam(value = "hasCustomSection",required = false) Boolean hasCustomSection) throws Exception {
		Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
		if(null!=isProj && isProj){
			ProjectFlagHolder.getInstance().getProjFlag().set(true);
		}
		DataGrid dataGrid = DtoUtils.getDataGridVO(getRequest());
		Map<String, Object> argsMap = new HashMap<String, Object>();
		argsMap.put("fieldConfig", fieldConfig);
		argsMap.put("delCellIds", delCellIds);
		argsMap.put("delEventIds", delEventIds);
		argsMap.put("delValidateIds", delValidateIds);
		argsMap.put("btDelCellIds", btDelCellIds);
		
		if (!fieldConfig.contains("<fields><list><list-item>")) {
			throw new EcException(InternationalResource.get("ec.exceptions.CONFIGUREDATAGRID"));
		}
		Map<String, Object> fieldMap = (Map<String, Object>) SerializeUitls.deserialize(fieldConfig);
		List<Map<String, Object>> buttons = (List<Map<String, Object>>) fieldMap.get("buttons");
		if (buttons != null && buttons.size() > 0) {
			for (Map<String, Object> button : buttons) {
				if(null != button.get("operateType") && "CUSTOM".equals(button.get("operateType")) && null == button.get("events")){
					throw new EcException(InternationalResource.get(button.get("displayName").toString())+InternationalResource.get("com.supcon.orchid.container.exceptions.CONFIGUREFUNCTION"));
				}
			}
		}
		dataGridService.publish(dataGrid, argsMap);
		DataGrid dg = dataGridService.getDataGrid(dataGrid.getCode());
		View v = dg.getView();
		if(v.getExtraView()==null){
			throw new EcException(InternationalResource.get("ec.exceptions.SAVEVIEWFIRST"));
		}
		if (!v.getExtraView().getConfig().contains("<customSection><![CDATA[true]]></customSection>")) {
			v.setHasCustomSection(hasCustomSection);
//			viewService.saveView(v);
			viewService.modifyShadowViewCustomSection(v.getCode(), v.getHasCustomSection());
		}
		/*fieldService.saveFields(dataGrid, fieldConfig, delCellIds, delEventIds, delValidateIds);
		buttonService.saveButton(dataGrid, fieldConfig, btDelCellIds);*/
		ResponseMsg response = new ResponseMsg(true);
		ProjectFlagHolder.getInstance().getProjFlag().set(false);
		return response;
	}
	
	/**
	 * DataGrid的字段属性的保存
	 * 
	 * @return
	 * @throws Exception
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/view/easytable_publish")
	public String easytablePublish(@RequestParam("fieldConfig") String fieldConfig, @RequestParam("delCellIds") String delCellIds,
								   @RequestParam("delEventIds") String delEventIds, @RequestParam("delValidateIds") String delValidateIds,
								   @RequestParam("btDelCellIds") String btDelCellIds, @RequestParam("dataGrid") DataGrid dataGrid,
								   @RequestParam("hasCustomSection") Boolean hasCustomSection) throws IOException {
		Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
		if(null!=isProj && isProj){
			ProjectFlagHolder.getInstance().getProjFlag().set(true);
		}
		Map<String, Object> argsMap = new HashMap<String, Object>();
		argsMap.put("fieldConfig", fieldConfig);
		argsMap.put("delCellIds", delCellIds);
		argsMap.put("delEventIds", delEventIds);
		argsMap.put("delValidateIds", delValidateIds);
		argsMap.put("btDelCellIds", btDelCellIds);
		
		if (!fieldConfig.contains("<fields><list><list-item>")) {
			throw new EcException(InternationalResource.get("com.supcon.orchid.container.exceptions.CONFIGUREEASYTABLE"));
		}
		Map<String, Object> fieldMap = (Map<String, Object>) SerializeUitls.deserialize(fieldConfig);
		List<Map<String, Object>> buttons = (List<Map<String, Object>>) fieldMap.get("buttons");
		if (buttons != null && buttons.size() > 0) {
			for (Map<String, Object> button : buttons) {
				if(null != button.get("operateType") && "CUSTOM".equals(button.get("operateType")) && null == button.get("events")){
					throw new EcException(InternationalResource.get(button.get("displayName").toString())+InternationalResource.get("com.supcon.orchid.container.exceptions.CONFIGUREFUNCTION"));
				}
			}
		}
		dataGridService.publish(dataGrid, argsMap);
		DataGrid dg = dataGridService.getDataGrid(dataGrid.getCode());
		View v = dg.getView();
		if(v.getExtraView()==null){
			throw new EcException(InternationalResource.get("ec.exceptions.SAVEVIEWFIRST"));
		}
		if (!v.getExtraView().getConfig().contains("<customSection><![CDATA[true]]></customSection>")) {
			v.setHasCustomSection(hasCustomSection);
			viewService.saveView(v);
			viewService.modifyShadowViewCustomSection(v.getCode(), v.getHasCustomSection());
		}
		/*fieldService.saveFields(dataGrid, fieldConfig, delCellIds, delEventIds, delValidateIds);
		buttonService.saveButton(dataGrid, fieldConfig, btDelCellIds);*/
		ResponseMsg response = new ResponseMsg(true);

		ObjectMapper mapper = new ObjectMapper();
		ProjectFlagHolder.getInstance().getProjFlag().set(false);
		return mapper.writeValueAsString(response);
	}
}
