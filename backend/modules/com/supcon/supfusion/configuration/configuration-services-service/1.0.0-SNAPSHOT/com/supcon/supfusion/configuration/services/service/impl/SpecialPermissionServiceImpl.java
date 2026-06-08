package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.enums.ViewType;
import com.supcon.supfusion.base.services.BaseServiceImpl;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.dao.ModelDaoImpl;
import com.supcon.supfusion.configuration.services.dao.PropertyDaoImpl;
import com.supcon.supfusion.configuration.services.dao.SpecialPermissionDaoImpl;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.configuration.services.service.SpecialPermissionService;
import com.supcon.supfusion.configuration.services.service.ViewService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import flexjson.JSONDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;


@Slf4j
@ServiceApiService
@Transactional
public class SpecialPermissionServiceImpl extends BaseServiceImpl<SpecialPermission> implements SpecialPermissionService {

	@Autowired
	private SpecialPermissionDaoImpl specialPermissionDao;
	
	@Autowired
	private PropertyDaoImpl propertyDao;
	
	@Autowired
	private ModelDaoImpl modelDao;
	

	@Autowired
	private ViewService viewService;
	
	
	@Autowired
	private ModelService modelService;

	@Override
	public void saveSpecialPermission(SpecialPermission specialPermission) {
		specialPermissionDao.save(specialPermission);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void saveXmlSource(String xmlSource, String modelCode) {
		List<Map<String, Object>> datas = new ArrayList<Map<String, Object>>();
		try {
			org.dom4j.Document doc = (org.dom4j.Document) DocumentHelper.parseText(xmlSource);
			Element root = doc.getRootElement();
			Iterator elements = root.elementIterator();
			for (Iterator iterator = elements; iterator.hasNext();) {
				Element element = (Element) iterator.next();
				Iterator innerIntertor = element.elementIterator();
				Map<String, Object> map = new HashMap<String, Object>();
				for (Iterator iterator2 = innerIntertor; innerIntertor.hasNext();) {
					Element type2 = (Element) iterator2.next();
					map.put(type2.getName(), type2.getText());
				}
				datas.add(map);
			}

		} catch (Exception e) {
			log.error(e.getMessage());
		}
		List<SpecialPermission> specialPermissionList = new ArrayList<SpecialPermission>();
		Model model = modelService.getModel(modelCode);
		int orderNo = 1;
		if (datas != null && datas.size() > 0) {
			for (Map<String, Object> map : datas) {
				if (map != null) {
					SpecialPermission sa = new SpecialPermission();
					for (Map.Entry<String, Object> entry : map.entrySet()) {
						if (entry.getKey() != null && entry.getKey().equals("rank")) {
							if (entry.getValue() != null) {
								sa.setRank(Integer.valueOf(entry.getValue().toString()));
							}
						}
						sa.setModelCode(modelCode);
						if (entry.getKey() != null && entry.getKey().equals("columnType")) {
							sa.setType(entry.getValue().toString());
						}
						if (entry.getKey() != null && entry.getKey().equals("specialPermissionCode")) {
							if (entry.getValue() != null) {
								sa.setCode(String.valueOf(entry.getValue().toString()));
							}
						}
						if (entry.getKey() != null && entry.getKey().equals("version")) {
							if (entry.getValue() != null) {
								sa.setVersion(Integer.valueOf(entry.getValue().toString()).intValue());
							}
						}
						if (entry.getKey() != null && entry.getKey().equals("relation")) {
							sa.setRelation(entry.getValue().toString());
						}
						if (entry.getKey() != null && entry.getKey().equals("isTree")) {
							if (entry.getValue().toString().equals("true")) {
								sa.setIsTree(true);
							}
						}
						if (entry.getKey() != null && entry.getKey().equals("propertyCode")) {
							sa.setProperty(propertyDao.load(entry.getValue().toString()));
						}
						if (entry.getKey() != null && entry.getKey().equals("refViewCode")) {
							sa.setRefView(viewService.getView(entry.getValue().toString()));
						}
						if (entry.getKey() != null && entry.getKey().equals("modelCode")) {
							sa.setTargetModelCode(entry.getValue().toString());
						}
						sa.setModuleCode(model.getModuleCode());
						sa.setEntityCode(model.getEntity().getCode());
					}
					if (sa.getCode() == null) {
						sa.setCode(sa.getModuleCode() + "_" + sa.getProperty().getCode());
					}
					sa.setOrderNo(orderNo);
					specialPermissionList.add(sa);
					specialPermissionDao.save(sa);
					orderNo++;
				}
			}
			if (specialPermissionList != null && specialPermissionList.size() > 0) {
				deleteNotExistSpecialPermissionList(specialPermissionList, modelCode);
				generateTemplateSQL(specialPermissionList, modelCode);
			}
		}

	}
	
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public String getSystemEntityCodeByProperty(Property p)  {
		if(p!=null)  {
			String fillContent = p.getFillcontent();
			Map<String,Object> map = (fillContent !=null && fillContent.startsWith("{")) ? deserializer.deserialize(fillContent) : Collections.EMPTY_MAP;
			if(map!=null)  {
				for (Map.Entry<String, Object> entry : map.entrySet()) {
					if(entry.getKey()!=null&&entry.getKey().equals("fillContent"))  {
						if(entry.getValue()!=null)  {
							String systemEntityCode=entry.getValue().toString();
							return  systemEntityCode;
						}
					}
				}
			}
		}
		return "";
	}

	private JSONDeserializer<Map> deserializer = new JSONDeserializer();

	@Override
	public List<SpecialPermission> findAllSpecialPermissionByModelCode(String modelCode) {
		List<SpecialPermission> specialPermissionList = new ArrayList<SpecialPermission>();
		String hql="from SpecialPermission s  where s.valid=1 and s.modelCode=?  order by s.orderNo asc";
		specialPermissionList = specialPermissionDao.findByHql(hql, new Object[]{modelCode});
		for(SpecialPermission sp:specialPermissionList)  {
			List<View> refViews = viewService.findViewsByAssModelCode(sp.getTargetModelCode(), ViewType.REFERENCE, ViewType.REFTREE);
			sp.setRelateRefViews(refViews);
			if(sp.getTargetModelCode()!=null&&sp.getTargetModelCode().length()>0)  {
				Model tempModel=modelService.getModel(sp.getTargetModelCode());
				if(tempModel!=null)  {
					sp.setTargetModelName(InternationalResource.get(tempModel.getName()));
				}
			}
		}
		return specialPermissionList;
	}
	
	@Override
	public List<SpecialPermission> findSpecialPermissionsByCode(String modelCode, String key) {
		List<SpecialPermission> specialPermissionList = new ArrayList<SpecialPermission>();
		StringBuilder sb=new StringBuilder();
		sb.append("from SpecialPermission s  where s.valid=1 ");
		if(key.equals("moduleCode"))  {
			sb.append("and s.moduleCode=?");
		}else if(key.equals("entityCode")) {
			sb.append("and s.entityCode=?");
		}
		specialPermissionList = specialPermissionDao.findByHql(sb.toString(), new Object[]{modelCode});
		return specialPermissionList;
	}
	
	
	
	
	public void deleteNotExistSpecialPermissionList(List<SpecialPermission> specialList, String modelCode) {
		String hql="from SpecialPermission s  where s.valid=true  and s.modelCode=?0";
		List<SpecialPermission> allSpecialList=specialPermissionDao.findByHql(hql, new Object[]{modelCode});
		String[] ids=new String[]{};
		List<String> idData=new ArrayList<String>();
		if(allSpecialList.size()>0)  {
			for(SpecialPermission sp:allSpecialList)  {
				  boolean flag=false;
				  for(SpecialPermission nowRes:specialList)  {
					   if(nowRes.getProperty().getCode().equals(sp.getProperty().getCode())) {
						   flag=true;
						   break;
					   }
				  }
				  if(!flag) {
					  idData.add(sp.getCode());
				  }
			}
		}
		if(idData.size()>0)  {
			ids=(String[])idData.toArray(new String[idData.size()]);
			String deleteHql="delete from SpecialPermission s  where s.valid=1 and s.code in(:ids)";
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("ids",ids);
			specialPermissionDao.bulkExecute(deleteHql, map);
			//删除关联权限关联用户配置
			String userSpecialPermissionDeleteHql="delete from UserPSpecialPermission s  where  s.specialPermission.code in(:ids)";
			specialPermissionDao.bulkExecute(userSpecialPermissionDeleteHql, map);
			//删除关联权限关联角色配置
			String urolePSpecialPermissionDeleteHql="delete from RolePSpecialPermission s  where  s.specialPermission.code in(:ids)";
			specialPermissionDao.bulkExecute(urolePSpecialPermissionDeleteHql, map);
		}
		
	}
	
	
	public  void  generateTemplateSQL(List<SpecialPermission> list, String modelCode)  {
		Model model = modelService.getModel(modelCode);
		String modelAlias = "\"" + firstLatterToLowerCase(model.getModelName()) + "\"";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			SpecialPermission sp = list.get(i);
			Property pro = propertyDao.load(sp.getProperty().getCode());
			String columnName = pro.getColumnName();
			String relation = sp.getRelation();
			Integer leftRank = sp.getRank() - 1;
			Integer rightRank = sp.getRank() - 1;
			String placeHolder = sp.getProperty().getCode();
			String leftSysmbol = "";
			String rightSysmbol = "";
			// 之前的一个对象控制等级,如果和前面的对象等级一致,则合并括号
			Integer previousRank = null;
			if (i == 0) {
				previousRank = list.get(i).getRank() - 1;
			} else {
				previousRank = list.get(i - 1).getRank() - 1;
			}
			if (previousRank.intValue() == leftRank.intValue()) {
				if (i == 0) {
					while (leftRank > 0) {
						leftSysmbol += "(";
						leftRank--;
					}
				} else {
					while (leftRank > 0) {
						// 去除右括号
						if (sb.length() > 0) {
							sb.deleteCharAt(sb.length() - 1);
						}
						leftRank--;
					}
					leftSysmbol = "";
				}
			} else {
				while (leftRank > 0) {
					leftSysmbol += "(";
					leftRank--;
				}
			}
			if (sb.toString().equals("")) {
				sb.append("$").append(placeHolder).append("$").append("  ").append(leftSysmbol).append(modelAlias).append(".").append(columnName)
						.append(" in ").append("$").append(placeHolder).append("$").append("  ");
			} else {
				sb.append("$").append(placeHolder).append("$").append("  ").append(relation).append("  ").append(leftSysmbol).append(modelAlias).append(".")
						.append(columnName).append(" in ").append("$").append(placeHolder).append("$").append("  ");
			}
			while (rightRank > 0) {
				rightSysmbol += ")";
				rightRank--;
			}
			sb.append(rightSysmbol);
		}
		model.setSpecialPerTemplateSQL(sb.toString());
		modelDao.update(model);
	}
	
	
	public static String firstLatterToLowerCase(String key) {
		char fl = ((String) key).charAt(0);
		return Character.toLowerCase(fl) + ((String) key).substring(1);
	}
	
	

	@Override
	public String generatePreview(String modelCode) {
		String result="";
		List<SpecialPermission> specialPermissionList = new ArrayList<SpecialPermission>();
		String hql="from SpecialPermission s  where s.valid=1 and s.modelCode=?  order by s.orderNo asc";
		specialPermissionList = specialPermissionDao.findByHql(hql, new Object[]{modelCode});
		if(specialPermissionList!=null&&specialPermissionList.size()>0)  {
			result=generatePreviewString(specialPermissionList,modelCode);
		}
		return result;
	}
	
	
	
	public String generatePreviewString(List<SpecialPermission> list, String modelCode)  {
		Model model = modelService.getModel(modelCode);
		StringBuilder sb = new StringBuilder();
		if(list !=null){
		for (int i = 0; i < list.size(); i++) {
			SpecialPermission sp = list.get(i);
			Property pro = propertyDao.load(sp.getProperty().getCode());
			String displayName = InternationalResource.get(pro.getDisplayName());
			String modelName = "";
			if (sp.getTargetModelCode() != null && sp.getTargetModelCode().length() > 0) {
				Model tempModel = modelService.getModel(sp.getTargetModelCode());
				if (tempModel != null) {
					modelName = "[" + InternationalResource.get(modelService.getModel(sp.getTargetModelCode()).getName()) + "]";
				}
			}
			String relation = sp.getRelation();
			Integer leftRank = sp.getRank() - 1;
			Integer rightRank = sp.getRank() - 1;
			String leftSysmbol = "";
			String rightSysmbol = "";
			// 之前的一个对象控制等级,如果和前面的对象等级一致,则合并括号
			Integer previousRank = null;
			if (i == 0) {
				previousRank = list.get(i).getRank() - 1;
			} else {
				previousRank = list.get(i - 1).getRank() - 1;
			}
			if (previousRank.intValue() == leftRank.intValue()) {
				if (i == 0) {
					while (leftRank > 0) {
						leftSysmbol += "(";
						leftRank--;
					}
				} else {
					while (leftRank > 0) {
						// 去除右括号
						if (sb.length() > 0) {
							sb.deleteCharAt(sb.length() - 1);
						}
						leftRank--;
					}
					leftSysmbol = "";
				}
			} else {
				while (leftRank > 0) {
					leftSysmbol += "(";
					leftRank--;
				}
			}
			if (sb.toString().equals("")) {
				sb.append("  ").append(leftSysmbol).append(" ").append(displayName).append("  ").append(modelName).append("  ");
			} else {
				sb.append("  ").append(relation).append("  ").append(leftSysmbol).append(" ").append(displayName).append("  ").append(modelName).append("  ");
			}
			while (rightRank > 0) {
				rightSysmbol += ")";
				rightRank--;
			}
			sb.append(rightSysmbol);
		}}
		if (list != null && list.size() > 0) {
			if (model.getIsAndRelation()) {
				sb.insert(0, "AND (");
			} else {
				sb.insert(0, "OR (");
			}
			sb.append(" )");
		}
		return sb.toString();
	}
	
	
	
	public Map<String, Object> getObjectProperties(Model model) {
		Map<String, Object> subs = new HashMap<String, Object>();
		// 获取实体属性
		List<Property> properties = modelService.findProperties(model);
		List<AssociatedInfo> associatedInfos = new ArrayList<AssociatedInfo>();
		List<AssociatedInfo> origAssociatedInfos = modelService.findAssociatedInfos(model);
		if (origAssociatedInfos != null) {
			for (AssociatedInfo asso : origAssociatedInfos) {
				// 以当前模型做为源模型的一对多关联
				if (null == asso.getOriginalProperty().getAssociatedProperty() && null != asso.getTargetProperty()
						&& null != asso.getTargetProperty().getAssociatedProperty()
						&& asso.getTargetProperty().getAssociatedProperty().getCode().equals(asso.getOriginalProperty().getCode())) {
					continue;
				}
				// 关联字段移除
				for (int i = 0; i < properties.size(); i++) {
					if (properties.get(i).getDisplayName().equals(asso.getOriginalProperty().getDisplayName())) {
						properties.remove(i);
						break;
					}
				}

				asso.getOriginalProperty().setModel(model);
				if (model.getInherentCommonFlag() != null
						&& model.getInherentCommonFlag()
						&& ("mainObj".equalsIgnoreCase(asso.getOriginalProperty().getName()) || "linkId".equalsIgnoreCase(asso.getOriginalProperty().getName()))) {
					continue;
				}

				List<View> refViews = viewService.findViewsByAssModelCode(asso.getTargetProperty().getModel().getCode(), ViewType.REFERENCE, ViewType.REFTREE);
				List<Map<String, Object>> refViewData = new ArrayList<Map<String, Object>>();
				if (refViews != null && refViews.size() > 0) {
					for (View view : refViews) {
						Map<String, Object> map = new HashMap<String, Object>();
						map.put("displayCode", InternationalResource.get(view.getDisplayName()));
						map.put("code", view.getCode());
						map.put("url", view.getUrl());
						refViewData.add(map);

					}
				}
				asso.setRefViewInfo(refViewData);
				associatedInfos.add(asso);
			}
		}
		for (Iterator<Property> it = properties.iterator(); it.hasNext();) {
			Property p = it.next();
			if(p!=null)  {
				if (!(p.getType() == DbColumnType.SYSTEMCODE)||p.getIsCustom()) {
					it.remove();
					properties.remove(p);
				} else {
					if (modelService.checkWhetherIsTreeSystemCode(p)) {
						p.setIsTreeSystemCode(true);
					}
				}
			}
		}
		subs.put("properties", properties);
		subs.put("associatedInfos", associatedInfos);
		return subs;
	}

	@Override
	public boolean checkWhetherIsExist(String propertyCode) {
		String hql="from  SpecialPermission  s  where  s.property.code=?   and  s.valid=true";
		List<SpecialPermission> results=specialPermissionDao.findByHql(hql, new Object[]{propertyCode});
		if(results.size()>0)  {
			return  true;
		}
		return false;
	}


	@Override
	public void deleteSpcialPermissionByModelCode(String modelCode) {
		String hql="delete  from  SpecialPermission  s  where  s.modelCode=?0";
		specialPermissionDao.bulkExecute(hql, new Object[]{modelCode});
	}
	
	@Override
	public Map<String, Object> getMainDisplayMap(Serializable mainDisplayName, Serializable businessKeyName,
                                                 List<Serializable> mainDisplayKeys) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SpecialPermission load(String specialPermissionCode) {
		return specialPermissionDao.load(specialPermissionCode);
	}

}
