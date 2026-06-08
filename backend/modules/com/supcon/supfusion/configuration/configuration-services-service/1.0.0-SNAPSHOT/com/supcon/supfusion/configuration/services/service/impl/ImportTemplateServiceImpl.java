package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.services.BaseServiceImpl;
import com.supcon.supfusion.configuration.services.dao.CustomPropertyModelMappingDaoImpl;
import com.supcon.supfusion.configuration.services.dao.ImportTemplateDaoImpl;
import com.supcon.supfusion.configuration.services.dao.ViewDaoImpl;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.service.FieldService;
import com.supcon.supfusion.configuration.services.service.ImportTemplateService;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * ec,proj用
 * @author zhengjiefeng
 *
 */
@Slf4j
@ServiceApiService("ec_ImportTemplateService")
@Transactional
public class ImportTemplateServiceImpl extends BaseServiceImpl<ImportTemplate> implements ImportTemplateService,InitializingBean {
	@Autowired
	private ImportTemplateDaoImpl importTemplateDao;
	@Autowired
	private ModelService modelService;
	@Autowired
	private FieldService fieldService;
//	@Autowired
//	private ModuleGenerateInfoService moduleGenerateInfoService;
	@Autowired
	private ViewDaoImpl viewDao;
	
	@Override
	public ImportTemplate getImportTemplateByCode(String code) {
		ImportTemplate importTemplate = importTemplateDao.findEntityByHql("from ImportTemplate where  code=?0",code);
		return importTemplate;
	}
	@Override
	public ImportTemplate getImportTemplateByHql(String hql, String param){
		ImportTemplate importTemplate = (ImportTemplate) importTemplateDao.createQuery(hql,param).uniqueResult();
		return importTemplate;
	}
	@Override
	public void saveImportTemplate(ImportTemplate importTemplate) {
		importTemplateDao.save(importTemplate);
		Model model = modelService.getModel(importTemplate.getCode());
	}
	
	@Override
	public void deleteImportTemplate(ImportTemplate importTemplate) {
		importTemplateDao.delete(importTemplate);
	}
	
	@Override
	public List<String> getRunningCustomProperties(String entityCode){
		List<String> list = null;
		String sql = "select property_code from BASE_CP_MODEL_MAPPING where model_code = ? and enable_custom = 1";
		list = importTemplateDao.createNativeQuery(sql, entityCode).list();
		return list;
	}

	@Override
	@Transactional(timeout = -1)
	public void importXml(String xml) throws DocumentException {
		try {
			Document document= DocumentHelper.parseText(xml);
			Element root=document.getRootElement();
			Iterator iterator=root.elementIterator();
			while(iterator.hasNext()){
				Element element=(Element) iterator.next();
				String code=element.elementText("code");
				ImportTemplate importTemplate=importTemplateDao.get(code);
				if(importTemplate==null){
					importTemplate=new ImportTemplate();
				}

				importTemplate.setCode(code);
				importTemplate.setProjFlag(Boolean.valueOf(element.elementText("projFlag")));

				String value=null;
				Element valueElement=element.element("value");
				if(valueElement!=null){
					Element listElement=valueElement.element("list");
					if(listElement!=null){
						value="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+listElement.asXML();
					}
				}
				importTemplate.setValue(value);
				importTemplateDao.save(importTemplate);
			}
		} catch (DocumentException e) {
			log.error(e.getMessage(),e);
			throw new DocumentException();
		}
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<Map<String, String>> getRequireData(String modelCode,Boolean getAllProperties,Boolean showCustom) {
		Model targetModel = modelService.getModel(modelCode);
		Boolean isBase = targetModel.getEntity().getIsBase();
		Set<Property> ps = new HashSet<Property>();
		List<Map<String, String>> columns = new ArrayList<Map<String, String>>();
		Map<String, String> fieldNameMap = new HashMap<String, String>();
		ps.addAll(targetModel.getProperties());
		Set<String> key = new HashSet<String>();
		List<String> customPropMaps = getEnabledCustomPropCodes(targetModel.getCode(),ps);

		// 获取实体属性
		List<Property> allProperties = modelService.findProperties(targetModel);
		Iterator<Property> allPropertyIter=allProperties.iterator();
		while(allPropertyIter.hasNext()){
			Property property=allPropertyIter.next();
			if(property.getIsHidden()){
				allPropertyIter.remove();
			}

			String sql = "select property_code from BASE_CP_MODEL_MAPPING where model_code = ? and enable_custom = 1";
			List<String> runningCustomPropertyCode = viewDao.createNativeQuery(sql, targetModel.getCode()).list();
			Property idProp = null;
			Boolean isWrongBk = false;
			Iterator<Property> psIter=ps.iterator();
			// 模型属性中的配置信息
			while (psIter.hasNext()) {
				Property p=psIter.next();
				if("id".equals(p.getName())){
					idProp = p;
				}else if("extraCol".equals(p.getName())){       //未启用大字段时，不进行excel导出
					 if(null == targetModel.getIsExtraCol() || !targetModel.getIsExtraCol()){
						continue;
					}
				}
				if(DbColumnType.valueOf("COLOR").equals(p.getType())){
					psIter.remove();
					continue;
				}
				if(p.getIsBussinessKey() && "version".equals(p.getName())){
					isWrongBk = true;
				}
				if(p.getIsCustom()){
					if(!showCustom){
						psIter.remove();
						continue;
					}
					if(runningCustomPropertyCode == null || runningCustomPropertyCode.size()==0){
						continue;
					}else{
						if(!runningCustomPropertyCode.contains(p.getCode())){
							continue;
						}
					}
				}
				
				if (!(p.getMultable() != null && p.getMultable())
						&& (!p.getIsInherent()|| "sort".equals(p.getName()) || "tableNo".equals(p.getName()) || "ownerPosition".equals(p.getName()) || "ownerStaff".equals(p.getName()))) {
					if (p.getType().equals(DbColumnType.BINARY) || p.getType().equals(DbColumnType.PICTURE)
							|| p.getType().equals(DbColumnType.PROPERTYATTACHMENT) || p.getType().equals(DbColumnType.OFFICE) || p.getType().equals(DbColumnType.PASSWORD)
							||p.getType().equals(DbColumnType.SUMMARY)|| p.getType().equals(DbColumnType.TAGNUMBER) || property.getMultable()) {
						continue;
					}
					Boolean isCustomProp=false;
					if (p.getIsCustom()) {
						if (null != customPropMaps && customPropMaps.size() > 0 && !customPropMaps.contains(p.getCode())) {
							continue;
						}
						isCustomProp=true;
					}
					if (!getAllProperties) {
						// 自定义字段特殊处理
						if (p.getIsCustom() != null && p.getIsCustom()) {
							if (null != p.getCode()) {
								if (modelService.getCustomProptyNullAble(p.getCode())) {
									continue;
								}
							}
						} else {
							if (p.getNullable()) {
								if (!(!isBase && ("ownerPosition".equals(p.getName()) || "ownerStaff".equals(p.getName())))) {
									continue;
								}
							}
						}
					}
					String tempKey = p.getName();
					/*if (!getAllProperties && tempKey.equals("tableNo")) {
						// 用于导入时去掉单据编号
						continue;
					}*/
					if( "ownerPosition".equals(p.getName()) && p.getModel().getEntity().getIsBase() ){//基础模型，拥有者岗位不为必须导出
						continue;
					}
					String tempPropertyCode = p.getCode();
					String tempColumnType = String.valueOf(p.getType());
					String tempDisplayName = p.getDisplayName();
					String layRec = p.getName();
					String propPrecision = "";
					String propShowFormat = "";
					String multable = p.getMultable().toString();
					String seniorsystemcode = p.getSeniorSystemCode().toString();
					String tempDisplayText = InternationalResource.get(p.getDisplayName());
					if (p.getFormat() != null) {
						propShowFormat = p.getFormat().toString();
					}
					if (p.getDecimalNum() != null) {
						propPrecision = p.getDecimalNum().toString();
					}
					Map<String, String> fieldInfo = new HashMap<String, String>();
					Map<String, String> fieldInfo2 = new HashMap<String, String>();
					if (p.getType().equals(DbColumnType.OBJECT) && null != p.getAssociatedProperty()) {
						if (!key.contains(p.getCode())) {
							Property bussiness = modelService.getBussinessProperty(p.getAssociatedProperty().getModel().getCode());
							if("id".equals(bussiness.getName()) && p.getIsMainAssociated()){
								continue;
							}
							if("version".equals(bussiness)){ //FIXME 什么意思？
								bussiness = modelService.getIdProperty(p.getAssociatedProperty().getModel().getCode());
							}	
							key.add(p.getCode());
							//非自定义字段对象类型处理
							//if (!p.getIsCustom()) {
								tempKey += "." + bussiness.getName();
								tempDisplayText += "." + InternationalResource.get(bussiness.getDisplayName());
								layRec = p.getAssociatedProperty().getModel().getTableName() + "," + p.getAssociatedProperty().getColumnName()
										+ "," + p.getModel().getTableName() + "," + p.getColumnName() + "-" + bussiness.getName();
								
								tempPropertyCode += "||" + bussiness.getCode();
								tempDisplayName += "," + bussiness.getDisplayName();
								tempColumnType = String.valueOf(bussiness.getType());
								if (bussiness.getFormat() != null) {
									propShowFormat = bussiness.getFormat().toString();
								}
								if (bussiness.getDecimalNum() != null) {
									propPrecision = bussiness.getDecimalNum().toString();
								}
								multable = bussiness.getMultable().toString();
								seniorsystemcode = bussiness.getSeniorSystemCode().toString();
								fieldInfo.put("modelCode", p.getAssociatedProperty().getModel().getCode());
								fieldInfo.put("assPropertyName", p.getName());
							//}
							//自定义字段对象类型用于导入处理
							/*if (!getAllProperties && isCustomProp) {
								fieldInfo.put("customPropImportCode", tempPropertyCode+"||" + bussiness.getCode());
							}*/
							
							//if(!p.getIsCustom()){
								//Obj字段的主显示字段同时导出
								List<Property> properties = modelService.findProperties(p.getAssociatedProperty().getModel());
								for(Property prop : properties){
									if(prop.getIsMainDisplay() && !prop.getIsInherent()){
										String tempKey2 = p.getName() + "." + prop.getName();
										String tempDisplayText2 = InternationalResource.get(p.getDisplayName()) + "." + InternationalResource.get(prop.getDisplayName());
										String layRec2 = p.getAssociatedProperty().getModel().getTableName() + "," + p.getAssociatedProperty().getColumnName()
												+ "," + p.getModel().getTableName() + "," + p.getColumnName() + "-" + prop.getName();
										
										String tempPropertyCode2 = p.getCode() + "||" + prop.getCode();
										String tempDisplayName2 = p.getDisplayName() + "," + prop.getDisplayName();
										String tempColumnType2 = String.valueOf(prop.getType());
										String propShowFormat2 = null;
										if (bussiness.getFormat() != null) {
											propShowFormat2 = prop.getFormat().toString();
										}
										String propPrecision2 = null;
										if (bussiness.getDecimalNum() != null) {
											propPrecision2 = prop.getDecimalNum().toString();
										}
										String multable2 = prop.getMultable().toString();
										String seniorsystemcode2 = prop.getSeniorSystemCode().toString();
										fieldInfo2.put("modelCode", p.getAssociatedProperty().getModel().getCode());
										fieldInfo2.put("assPropertyName", p.getName());
										
										fieldInfo2.put("columnType", tempColumnType2);
										fieldInfo2.put("propertyCode", tempPropertyCode2);
										fieldInfo2.put("key", tempKey2);
										fieldInfo2.put("name", tempKey2);
										fieldInfo2.put("layRec", layRec2);
										fieldInfo2.put("displayName", tempDisplayName2);
										fieldInfo2.put("columnName", p.getColumnName());
										fieldInfo2.put("displayText", tempDisplayText2);
										fieldInfo2.put("propShowFormat", propShowFormat2);
										fieldInfo2.put("propPrecision", propPrecision2);
										fieldInfo2.put("multable", multable2);
										fieldInfo2.put("seniorsystemcode", seniorsystemcode2);
										fieldInfo2.put("nullAble", String.valueOf(p.getNullable()));
									}
								}
							//}
						}
					}
					fieldInfo.put("columnType", tempColumnType);
					fieldInfo.put("propertyCode", tempPropertyCode);
					fieldInfo.put("key", tempKey);
					fieldInfo.put("name", tempKey);
					fieldInfo.put("layRec", layRec);
					fieldInfo.put("displayName", tempDisplayName);
					fieldInfo.put("columnName", p.getColumnName());
					fieldInfo.put("displayText", tempDisplayText);
					fieldInfo.put("propShowFormat", propShowFormat);
					fieldInfo.put("propPrecision", propPrecision);
					fieldInfo.put("multable", multable);
					fieldInfo.put("seniorsystemcode", seniorsystemcode);
					if ("ownerPosition".equals(p.getName()) || "ownerStaff".equals(p.getName())) {
						fieldInfo.put("nullAble", String.valueOf(Boolean.FALSE));
					} else {
						if(isCustomProp) {
							fieldInfo.put("nullAble", String.valueOf(modelService.getCustomProptyNullAble(p.getCode())));
						}else {
							fieldInfo.put("nullAble", String.valueOf(p.getNullable()));
						}
					}
					fieldInfo.put("isCustom", isCustomProp.toString());
					fieldNameMap.put(p.getCode(), p.getDisplayName());
					boolean flag = false;
					for (Map<String, String> old : columns) {
						if (old.get("propertyCode").equals(fieldInfo.get("propertyCode")) || old.get("propertyCode").startsWith(fieldInfo.get("propertyCode") + "||")) {
							flag = true;
							break;
						}
					}
					if (!flag&&!tempPropertyCode.endsWith("_ownerStaff") && !tempPropertyCode.endsWith("_staff")) {
						columns.add(fieldInfo);
						if(fieldInfo2 != null && fieldInfo2.size() >0){
							columns.add(fieldInfo2);
						}
					}
				}
			}
			if(isWrongBk){
				String tempKey = idProp.getName();
				String tempPropertyCode = idProp.getCode();
				String tempColumnType = String.valueOf(idProp.getType());
				String tempDisplayName = idProp.getDisplayName();
				String layRec = idProp.getName();
				String propPrecision = "";
				String propShowFormat = "";
				String multable = idProp.getMultable().toString();
				String seniorsystemcode = idProp.getSeniorSystemCode().toString();
				String tempDisplayText = InternationalResource.get(idProp.getDisplayName());
				if (idProp.getFormat() != null) {
					propShowFormat = idProp.getFormat().toString();
				}
				if (idProp.getDecimalNum() != null) {
					propPrecision = idProp.getDecimalNum().toString();
				}
				
				Map<String, String> fieldInfo = new HashMap<String, String>();
				fieldInfo.put("columnType", tempColumnType);
				fieldInfo.put("propertyCode", tempPropertyCode);
				fieldInfo.put("key", tempKey);
				fieldInfo.put("name", tempKey);
				fieldInfo.put("layRec", layRec);
				fieldInfo.put("displayName", tempDisplayName);
				fieldInfo.put("columnName", idProp.getColumnName());
				fieldInfo.put("displayText", tempDisplayText);
				fieldInfo.put("propShowFormat", propShowFormat);
				fieldInfo.put("propPrecision", propPrecision);
				fieldInfo.put("multable", multable);
				fieldInfo.put("seniorsystemcode", seniorsystemcode);
				fieldInfo.put("nullAble", String.valueOf(idProp.getNullable()));
				fieldInfo.put("isCustom", "false");
				fieldNameMap.put(idProp.getCode(), idProp.getDisplayName());
				
				columns.add(fieldInfo);
			}
		}
		return columns;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		
	}

	@Autowired
	private CustomPropertyModelMappingDaoImpl customPropertyModelMappingDao;

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<String>  getEnabledCustomPropCodes(String modelCode,Set<Property>  props)  {
		List<String>  customPropCodes=new ArrayList<String>();
		List<CustomPropertyModelMapping> list = customPropertyModelMappingDao.findByHql(
				"from CustomPropertyModelMapping c where c.model.code = ? and c.enableCustom = true and c.property.valid = true order by c.id", new Object[] { modelCode });
		Collections.sort(list, new Comparator<CustomPropertyModelMapping>() {
			@Override
			public int compare(CustomPropertyModelMapping o1, CustomPropertyModelMapping o2) {
				if (o1.getSort() != null && o2.getSort() != null) {
					return o1.getSort() - o2.getSort();
				}else if (o1.getId() != null && o2.getId() != null) {
					Long l=o1.getId() - o2.getId();
					if(l>0){
						return 1;
					}if(l<0){
						return -1;
					}else{
						return 0;
					}
				} else if (o1.getSort() != null && o2.getSort() == null) {
					return -1;
				} else if (o1.getSort() == null && o2.getSort() != null) {
					return 1;
				} else {
					return 0;
				}
			}
		});
		//将自定义字段中设置的内容放到property中
		for (CustomPropertyModelMapping cp : list) {
			for (Property p : props) {
				if (null != cp.getProperty() && p.getCode().equals(cp.getProperty().getCode())) {
					if (null != cp.getAssociatedProperty()) {
						p.setAssociatedProperty(cp.getAssociatedProperty());
					}
					if (null != cp.getAssociatedType()) {
						p.setAssociatedType(cp.getAssociatedType());
					}
					if (null != cp.getFillContent()) {
						p.setFillcontent(cp.getFillContent());
					}
					if (null != cp.getSeniorSystemCode()) {
						p.setSeniorSystemCode(cp.getSeniorSystemCode());
					}
					if (null != cp.getMultable()) {
						p.setMultable(cp.getMultable());
					}
					if (null != cp.getNullable()) {
						p.setNullable(cp.getNullable());
					}
				}
			}
			customPropCodes.add(cp.getProperty().getCode());
		}
		return customPropCodes;
	}
	@Override
	public List<ImportTemplate> getImportTemplateListByModuleCode(String moduleCode) {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(ImportTemplate.class);
		detachedCriteria.add(Restrictions.like("code", moduleCode, MatchMode.START));
		List<ImportTemplate> list=importTemplateDao.findByCriteria(detachedCriteria);
		return list;
	}
	@Override
	@Transactional(timeout = -1)
	public synchronized void saveImportTemplateList(List<ImportTemplate> list) throws DocumentException {
		for(ImportTemplate it:list){
			ImportTemplate importTemplate=importTemplateDao.get(it.getCode());
			if(importTemplate==null){
				importTemplate=new ImportTemplate();
			}

			importTemplate.setCode(it.getCode());
			importTemplate.setEcEnv(EcEnv.product);
			importTemplate.setProjFlag(it.getProjFlag());
			importTemplate.setValue(it.getValue());
			importTemplateDao.save(importTemplate);
		}
	}
}