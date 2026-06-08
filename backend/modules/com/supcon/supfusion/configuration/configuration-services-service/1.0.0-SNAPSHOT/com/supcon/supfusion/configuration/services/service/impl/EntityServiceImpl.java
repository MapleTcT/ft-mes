/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 

 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.services.BaseServiceImpl;
import com.supcon.supfusion.base.services.MenuInfoService;
import com.supcon.supfusion.base.services.ScriptService;
import com.supcon.supfusion.configuration.services.dao.*;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.enums.ECEnum;
import com.supcon.supfusion.configuration.services.enums.ViewType;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.service.*;
import com.supcon.supfusion.configuration.services.utils.DateUtil;
import com.supcon.supfusion.configuration.services.utils.FileUtils;
import com.supcon.supfusion.configuration.services.utils.JsonUtils;
import com.supcon.supfusion.configuration.services.utils.StringUtils;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.scaffold.hibernate.dao.IBaseDao;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

/**
 * 
 * @author songjiawei
 * 
 */
@Slf4j
@ServiceApiService("ec_EntityService")
@Transactional
public class EntityServiceImpl extends BaseServiceImpl<Entity> implements EntityService {
	@Autowired
	private EntityDaoImpl entityDao;

	@Autowired
	private PropertyDaoImpl propertyDao;
	@Autowired
	private ModelDaoImpl modelDao;
	@Autowired
	private ViewDaoImpl viewDao;
	@Autowired
	private DataGridDaoImpl dataGridDao;
	@Autowired
	private CustomCodeDaoImpl customCodeDao;
	@Autowired
	private ModuleService moduleService;
//	@Autowired
//	private ProcessService processSerivce;
	@Autowired
	private ScriptService scriptService;
	@Autowired
	private SessionFactory sessionFactory;
	@Autowired
	private MenuInfoService menuInfoService;
	@Autowired
	private ModelService modelService;
	@Autowired
	private ViewService viewService;
	@Autowired
	private DataGridService dataGridService;
	@Autowired
	private FieldService fieldService;
	@Autowired
	private SpecialPermissionDaoImpl specialPermissionDao;
	@Autowired
	private SpecialPermissionService specialPermissionService;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private FieldDaoImpl fieldDao;
	@Autowired
	private ExtraViewDaoImpl extraViewDao;
//	@Autowired
//	private ModuleGenerateInfoService moduleGenerateInfoService;
	@Override
	public void saveEntity(Entity entity) {
		if (!checkEntityNameUnique(entity)) {
			throw new EcException(EcException.Code.UNIQUECODE);
		}
		Entity oldEntity = null;
		if(StringUtils.isEmpty(entity.getCode())) {
			String code =  entity.getModule().getCode() + "_" + entity.getEntityName();
			entity.setCode(code);
		} else {
			oldEntity = entityDao.get(entity.getCode());
		}
		Boolean enableAclRestrict = null;
		Boolean payCloseAttention = null;
		Boolean groupEnable = null;
		if(null != oldEntity){
			enableAclRestrict = oldEntity.getEnableAclRestrict();
			payCloseAttention = oldEntity.getPayCloseAttention();
			groupEnable = oldEntity.getGroupEnabled();
		}
		entityDao.merge(entity);
		//开始保存模块信息数据的最后修改时间
//		ModuleGenerateInfo generateInfo = moduleGenerateInfoService.getModuleGenerateInfoByModuleCode(entity.getModule().getCode());
//		if(generateInfo!=null){
//			generateInfo.setLastModifyTime(new Date());
//			moduleGenerateInfoService.save(generateInfo,Thread.currentThread());
//		}else{
//			generateInfo = new ModuleGenerateInfo();
//			generateInfo.setLastModifyTime(new Date());
//			generateInfo.setModuleCode(entity.getModule().getCode());
//			moduleGenerateInfoService.create(generateInfo,Thread.currentThread());
//		}
		//启用Acl 或 启用关注 选项发生变更时
		if ((null != enableAclRestrict && !enableAclRestrict.equals(entity.getEnableAclRestrict()))
				|| (null != payCloseAttention && !payCloseAttention.equals(entity.getPayCloseAttention()))
				|| (null != groupEnable && !groupEnable.equals(entity.getGroupEnabled()))) {
			List<Model> models = modelService.findModels(entity);
			if (null != models && !models.isEmpty()) {
				for (Model model : models) {
					if (null != enableAclRestrict && !enableAclRestrict.equals(entity.getEnableAclRestrict())) {// Acl选项变更
						FileUtils.updateAclTable(model);
					}
					if (null != groupEnable && !groupEnable.equals(entity.getGroupEnabled())) {// 组限制选项变更
						FileUtils.updateGroupTable(model);
					}
					if (model.getIsMain()) {
						if (null != payCloseAttention && !payCloseAttention.equals(entity.getPayCloseAttention())) {// 关注
							FileUtils.updatePayCloseAttentionTable(model);
						}
					}
				}
			}
		}
	
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public void dealPayCloseAttention(Entity entity){
		if(null != entity && null != entity.getCode()){
			Entity oldEntity = getEntity(entity.getCode());
			if(null != oldEntity && !oldEntity.getPayCloseAttention().equals( entity.getPayCloseAttention())){
				String sql = "update ec_view SET MODIFY_TIME=? WHERE ENTITY_CODE=?";
				//修改该实体下面的视图修改时间，普通发布的时候，数据同步会重新同步这部分数据
				entityDao.createNativeQuery(sql).setParameter(0, new Date()).setParameter(1, entity.getCode()).executeUpdate();
			}
		}		
	}
	
	/*
	 * code唯一性判断
	 */
	private boolean checkEntityNameUnique(Entity entity) {
				
		List<Object> parameters = new LinkedList<Object>();
		StringBuffer sql = new StringBuffer("select count(CODE) as totalCoual from ");
		sql.append(Entity.TABLE_NAME).append(" where MODULE_CODE = ? and lower(ENTITY_NAME) = ? and VALID = 1");
		if (StringUtils.isEmpty(entity.getCode())) {
			parameters.add(entity.getModule().getCode());
			parameters.add(entity.getEntityName().toLowerCase());
		} else {
			sql.append(" and lower(CODE) != ?");
			parameters.add(entity.getModule().getCode());
			parameters.add(entity.getEntityName().toLowerCase());
			parameters.add(entity.getCode().toLowerCase());
		}
		Object[] params = new Object[parameters.size()];
		List<Number> moduleCount = entityDao.createNativeQuery(sql.toString(), parameters.toArray(params)).list();
		entityDao.flush();
		if (moduleCount.get(0).intValue() == 0) {
            return true;
        }

		return false;
	}

	@Override
	@Transactional
	public String deleteEntity(Entity entity) {
		String hql=" from Property as p where p.model.entity.code != ? and p.associatedProperty.model.entity.code = ?";
		List<Property> properties = entityDao.findByHql(hql, new Object[] {entity.getCode(), entity.getCode()});
		Set descSet = new HashSet<String>();
		if(null != properties && !properties.isEmpty()) {
			//add by yubo20171221
			for (Property property : properties) {
				descSet.add(InternationalResource.get("ec.entity.delete.entity",new Object[] {InternationalResource.get(
						property.getModel().getEntity().getName())}));

			}
			return JsonUtils.setToJson(descSet);
			//throw new BAPException(BAPException.Code.ASS_BY_ENTITY);
		}
//		List<Deployment> deploymentList = processSerivce.findDeployments(entity.getCode());
//		if(null != deploymentList && !deploymentList.isEmpty()) {
//			for(Deployment deployment:deploymentList){
//				processSerivce.deleteFlow(deployment, false, false);
//			}
//		} else {
			if(((Number) entityDao.createNativeQuery("select count(0) from BASE_ROLEPSTAFF  dps where exists (select dp.id from BASE_ROLEPERMISSION dp inner join BASE_MENUOPERATE mp on dp.MENUOPERATE_ID = mp.id where dps.ROLEPERMISSION_ID=dp.id and mp.ENTITY_CODE=?)", entity.getCode()).uniqueResult()).intValue() > 0) {
				entityDao.createNativeQuery("delete from BASE_ROLEPSTAFF where   exists (select dp.id from BASE_ROLEPERMISSION dp inner join BASE_MENUOPERATE mp on dp.MENUOPERATE_ID = mp.id where dps.ROLEPERMISSION_ID=dp.id and mp.ENTITY_CODE=?)", entity.getCode()).executeUpdate();
			}
			if(((Number) entityDao.createNativeQuery("select count(0) from BASE_ROLEPPOSITION dps where exists (select dp.id from BASE_ROLEPERMISSION dp inner join BASE_MENUOPERATE mp on dp.MENUOPERATE_ID = mp.id where dps.ROLEPERMISSION_ID=dp.id and mp.ENTITY_CODE=?)", entity.getCode()).uniqueResult()).intValue() > 0) {
				entityDao.createNativeQuery("delete from BASE_ROLEPPOSITION where exists (select dp.id from BASE_ROLEPERMISSION dp inner join BASE_MENUOPERATE mp on dp.MENUOPERATE_ID = mp.id where dps.ROLEPERMISSION_ID=dp.id and mp.ENTITY_CODE=?)", entity.getCode()).executeUpdate();
			}
			if(((Number) entityDao.createNativeQuery("select count(0) from BASE_ROLEPERMISSION dp where exists (select mp.id from BASE_MENUOPERATE mp where dp.MENUOPERATE_ID=mp.id and mp.ENTITY_CODE=?)", entity.getCode()).uniqueResult()).intValue() > 0) {
				entityDao.createNativeQuery("delete from BASE_ROLEPERMISSION where exists (select mp.id from BASE_MENUOPERATE mp where MENUOPERATE_ID=mp.id and mp.ENTITY_CODE=?)", entity.getCode()).executeUpdate();
			}
			if(((Number) entityDao.createNativeQuery("select count(0) from BASE_USERPSTAFF  dps where   exists (select dp.id from BASE_USERPERMISSION dp inner join BASE_MENUOPERATE mp on dp.MENUOPERATE_ID = mp.id where dps.USERPERMISSION_ID=dp.id and mp.ENTITY_CODE=?)", entity.getCode()).uniqueResult()).intValue() > 0) {
				entityDao.createNativeQuery("delete from BASE_USERPSTAFF where   exists (select dp.id from BASE_USERPERMISSION dp inner join BASE_MENUOPERATE mp on dp.MENUOPERATE_ID = mp.id where dps.USERPERMISSION_ID=dp.id and mp.ENTITY_CODE=?)", entity.getCode()).executeUpdate();
			}
			if(((Number) entityDao.createNativeQuery("select count(0) from BASE_USERPPOSITION dps where exists (select dp.id from BASE_USERPERMISSION dp inner join BASE_MENUOPERATE mp on dp.MENUOPERATE_ID = mp.id where dps.USERPERMISSION_ID=dp.id and mp.ENTITY_CODE=?)", entity.getCode()).uniqueResult()).intValue() > 0) {
				entityDao.createNativeQuery("delete from BASE_USERPPOSITION where exists (select dp.id from BASE_USERPERMISSION dp inner join BASE_MENUOPERATE mp on dp.MENUOPERATE_ID = mp.id where dps.USERPERMISSION_ID=dp.id and mp.ENTITY_CODE=?)", entity.getCode()).executeUpdate();
			}
			if(((Number) entityDao.createNativeQuery("select count(0) from BASE_USERPERMISSION dp where exists (select mp.id from BASE_MENUOPERATE mp where dp.MENUOPERATE_ID=mp.id and mp.ENTITY_CODE=?)", entity.getCode()).uniqueResult()).intValue() > 0) {
				entityDao.createNativeQuery("delete from BASE_USERPERMISSION where exists (select mp.id from BASE_MENUOPERATE mp where MENUOPERATE_ID=mp.id and mp.ENTITY_CODE=?)", entity.getCode()).executeUpdate();
			}
//		}
		//fix me:菜单有自己的缓存，这里先不处理
		menuInfoService.deleteMenuOperateByEntity(entity.getCode());
		String specialPermissionSql="from SpecialPermission s  where  s.entityCode= ?";
		List<SpecialPermission> specialPermissionList = entityDao.findByHql(specialPermissionSql, entity.getCode());
		if(specialPermissionList != null && !specialPermissionList.isEmpty()) {
			for(SpecialPermission s:specialPermissionList)  {
					specialPermissionDao.delete(s);
			}
		}
		String viewSql = "from  View as v where v.entity.code= ?";
		List<View> viewList = entityDao.findByHql(viewSql, entity.getCode());
		if(viewList != null && !viewList.isEmpty()) {
			for(View v : viewList) {
				deleteInfoByView(v);
				entityDao.flush();
				viewService.deleteView(v);
			}
		}
		String emSql = "from  Model as m where m.entity.code= ?";
		List<Model> emList = entityDao.findByHql(emSql, entity.getCode());
		if(emList != null && !emList.isEmpty()) {
			for(Model m : emList) {
				modelService.deleteModel(m);
			}
		}
		entityDao.delete(entity.getCode(), entity.getVersion());
		//开始保存模块信息数据的最后修改时间
//		ModuleGenerateInfo generateInfo = moduleGenerateInfoService.getModuleGenerateInfoByModuleCode(entity.getModule().getCode());
//		if(generateInfo!=null){
//			generateInfo.setLastModifyTime(new Date());
//			moduleGenerateInfoService.save(generateInfo,Thread.currentThread());
//		}else{
//			generateInfo = new ModuleGenerateInfo();
//			generateInfo.setLastModifyTime(new Date());
//			generateInfo.setModuleCode(entity.getModule().getCode());
//			moduleGenerateInfoService.create(generateInfo,Thread.currentThread());
//		}
		return "";
	}
	
	@Override
	@Transactional
	public void deleteEntity(String entityCode) {
		Entity entity = getEntity(entityCode);
		deleteEntity(entity);
	}

	@Override
	@Transactional
	public String deleteEntityPhysical(String entityCode, Boolean deleteType) {
		// add by yubo20171221
		Set<String> descSet = new HashSet<String>();
		Entity entity = getEntityByNoValid(entityCode);
		if(!deleteType) {
			if(null == entity) {
				throw new StaleObjectStateException(Entity.class.getName(), entityCode);
			}
		}
		if(null != entity) {
			try {
				descSet = this.checkDelete(entity);
				if(null != descSet && !descSet.isEmpty()){
					return JsonUtils.setToJson(descSet);
				}
			} catch (Exception e) {
				log.error(e.getMessage(),e);
				throw new EcException("校验失败，详细错误信息请查看日志");
			}
			
			log.info("删除"+ InternationalResource.get(entity.getName())+"实体文件开始");
			modelService.deleteModuleFile(entity);
			log.info("删除"+ InternationalResource.get(entity.getName())+"实体文件结束");
			
			//			List<Deployment> deploymentList = processSerivce.findDeployments(entityCode);
//			if(null != deploymentList && !deploymentList.isEmpty()) {
//				for(Deployment deployment:deploymentList){
//					processSerivce.deleteFlow(deployment, true, false);
//				}
//			} else {
				if(((Number) entityDao.createNativeQuery("select count(0) from BASE_ROLEPSTAFF  dps where exists (select dp.id from BASE_ROLEPERMISSION dp inner join BASE_MENUOPERATE mp on dp.MENUOPERATE_ID = mp.id where dps.ROLEPERMISSION_ID=dp.id and mp.ENTITY_CODE=?)", entity.getCode()).uniqueResult()).intValue() > 0) {
					entityDao.createNativeQuery("delete from BASE_ROLEPSTAFF where exists (select dp.id from BASE_ROLEPERMISSION dp inner join BASE_MENUOPERATE mp on dp.MENUOPERATE_ID = mp.id where BASE_ROLEPSTAFF.ROLEPERMISSION_ID=dp.id and mp.ENTITY_CODE=?)", entity.getCode()).executeUpdate();
				}
				if(((Number) entityDao.createNativeQuery("select count(0) from BASE_ROLEPPOSITION dps where exists (select dp.id from BASE_ROLEPERMISSION dp inner join BASE_MENUOPERATE mp on dp.MENUOPERATE_ID = mp.id where dps.ROLEPERMISSION_ID=dp.id and mp.ENTITY_CODE=?)", entity.getCode()).uniqueResult()).intValue() > 0) {
					entityDao.createNativeQuery("delete from BASE_ROLEPPOSITION where exists (select dp.id from BASE_ROLEPERMISSION dp inner join BASE_MENUOPERATE mp on dp.MENUOPERATE_ID = mp.id where BASE_ROLEPPOSITION.ROLEPERMISSION_ID=dp.id and mp.ENTITY_CODE=?)", entity.getCode()).executeUpdate();
				}
				if(((Number) entityDao.createNativeQuery("select count(0) from BASE_ROLEPERMISSION dp where exists (select mp.id from BASE_MENUOPERATE mp where dp.MENUOPERATE_ID=mp.id and mp.ENTITY_CODE=?)", entity.getCode()).uniqueResult()).intValue() > 0) {
					entityDao.createNativeQuery("delete from BASE_ROLEPERMISSION where exists (select mp.id from BASE_MENUOPERATE mp where MENUOPERATE_ID=mp.id and mp.ENTITY_CODE=?)", entity.getCode()).executeUpdate();
				}
				if(((Number) entityDao.createNativeQuery("select count(0) from BASE_USERPSTAFF dps where exists (select dp.id from BASE_USERPERMISSION dp inner join BASE_MENUOPERATE mp on dp.MENUOPERATE_ID = mp.id where dps.USERPERMISSION_ID=dp.id and mp.ENTITY_CODE=?)", entity.getCode()).uniqueResult()).intValue() > 0) {
					entityDao.createNativeQuery("delete from BASE_USERPSTAFF where exists (select dp.id from BASE_USERPERMISSION dp inner join BASE_MENUOPERATE mp on dp.MENUOPERATE_ID = mp.id where BASE_USERPSTAFF.USERPERMISSION_ID=dp.id and mp.ENTITY_CODE=?)", entity.getCode()).executeUpdate();
				}
				if(((Number) entityDao.createNativeQuery("select count(0) from BASE_USERPPOSITION dps where exists (select dp.id from BASE_USERPERMISSION dp inner join BASE_MENUOPERATE mp on dp.MENUOPERATE_ID = mp.id where dps.USERPERMISSION_ID=dp.id and mp.ENTITY_CODE=?)", entity.getCode()).uniqueResult()).intValue() > 0) {
					entityDao.createNativeQuery("delete from BASE_USERPPOSITION where exists (select dp.id from BASE_USERPERMISSION dp inner join BASE_MENUOPERATE mp on dp.MENUOPERATE_ID = mp.id where BASE_USERPPOSITION.USERPERMISSION_ID=dp.id and mp.ENTITY_CODE=?)", entity.getCode()).executeUpdate();
				}
				if(((Number) entityDao.createNativeQuery("select count(0) from BASE_USERPERMISSION dp where exists (select mp.id from BASE_MENUOPERATE mp where dp.MENUOPERATE_ID=mp.id and mp.ENTITY_CODE=?)", entity.getCode()).uniqueResult()).intValue() > 0) {
					entityDao.createNativeQuery("delete from BASE_USERPERMISSION where exists (select mp.id from BASE_MENUOPERATE mp where MENUOPERATE_ID=mp.id and mp.ENTITY_CODE=?)", entity.getCode()).executeUpdate();
				}
				entityDao.flush();
//			}
			//fix me:菜单有自己的缓存，这里先不处理
			entityDao.flush();
			entityDao.clear();
			menuInfoService.deleteMenuOperateByEntityPhysical(entity.getCode());
			try {
				deleteEcEntity(entity.getCode());
			} catch (Exception e) {
				log.error(e.getMessage());
			}
			
			//开始保存模块信息数据的最后修改时间
//			ModuleGenerateInfo generateInfo = moduleGenerateInfoService.getModuleGenerateInfoByModuleCode(entity.getModule().getCode());
//			if(generateInfo!=null){
//				generateInfo.setLastModifyTime(new Date());
//				moduleGenerateInfoService.save(generateInfo,Thread.currentThread());
//			}else{
//				generateInfo = new ModuleGenerateInfo();
//				generateInfo.setLastModifyTime(new Date());
//				generateInfo.setModuleCode(entity.getModule().getCode());
//				moduleGenerateInfoService.create(generateInfo,Thread.currentThread());
//			}
		}
		return "";
	}
	
	private void deleteEcEntity(String code) throws Exception {
		
		for (ECEnum ecEnum : ECEnum.values()) {
			if (ecEnum == ECEnum.ec_module) {
				continue;
			}
			if(ecEnum == ECEnum.ec_entity){
				String sql = "delete from " + ecEnum + " where code = ? ";
				jdbcTemplate.update(sql, code);
			}else{
				String sql = "delete from " + ecEnum + " where code like ? ";
				jdbcTemplate.update(sql, code+"%");
				if(ecEnum == ECEnum.ec_print_template){
					String sql1 = "delete from " + ECEnum.ec_print_template+ " where code like ? ";//删除工程期打印模板
					jdbcTemplate.update(sql1, code+"%");
				}
			}
		}
		String sql1 = "delete from EC_ADV_QUERY_CONDITION_ITEM where CONDITION_ID in (select id from EC_ADV_QUERY_CONDITION where VIEW_CODE like ?  ) ";
		jdbcTemplate.update(sql1, code+"%");
		sql1 = "delete from EC_ADV_QUERY_CONDITION  where VIEW_CODE like ? ";
		jdbcTemplate.update(sql1, code+"%");
		sql1 = "delete FROM EC_SPECIAL_PERMISSION   where ENTITY_CODE=?";
		jdbcTemplate.update(sql1, code);
	}
	

	/*@Override
	public Entity getEntity(long id) {
		// return entityDao.load(id);
		return entityDao.findEntityByHql("from Entity where id = ? and valid = true", id);
	}*/

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Entity getEntity(String code) {
		// return entityDao.findEntityByProperty("code", code);
		Entity entity = entityDao.get(code);
		if (null != entity && entity.isValid()) {
			return entity;
		}
		return null;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	private Entity getEntityByNoValid(String code) {
		// return entityDao.findEntityByProperty("code", code);
		return entityDao.findEntityByHql("from Entity where code = ?0 ", code);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Page<Entity> findEntities(Page<Entity> page, Criterion... criterions) {
		Criterion validCriterion = Restrictions.eq("valid", true);
		if (null == criterions) {
            criterions = new Criterion[] { validCriterion,
                    Restrictions.or(Restrictions.eq("inherentCommonFlag", false), Restrictions.isNull("inherentCommonFlag")) };
        } else {
			Criterion[] cs = new Criterion[criterions.length + 2];
			System.arraycopy(criterions, 0, cs, 0, criterions.length);
			cs[criterions.length] = validCriterion;
			cs[criterions.length + 1] = Restrictions.or(Restrictions.eq("inherentCommonFlag", false), Restrictions.isNull("inherentCommonFlag"));
			criterions = cs;
		}
		return entityDao.findByPage(page, criterions);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Page<Entity> findEntities(Page<Entity> page, Module module) {
		return findEntities(page, Restrictions.eq("module", module), Restrictions.eq("valid", true));
	}


//	@Override
//	public void publish(long entityId) {
//		Entity entity = getEntity(entityId);
//		if (null != entity) {
//			Set<Model> models = entity.getModels();
//			if (!models.isEmpty()) {
//				for (Model model : models) {
//					generateService.generateModel(model);
//					generateService.generateService(model);
//				}
//			}
//		}
//
//	}
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Entity> findEntities(Module module) {
		return entityDao.findByHql("from Entity where module = ? and valid = true order by code", module);
	}
	public void deleteAdcQueryCondition(Entity entity){
		HibernateTemplate template = new HibernateTemplate(sessionFactory);
		String sql="from AdvQueryCondition as aqc  where aqc.view.entity.code=?";
		//String sql="select aqc.id as ID from EC_ADV_QUERY_CONDITION aqc where aqc.VIEW_ID in (select v.id from EC_VIEW v where v.ENTITY_CODE =?)";
		List<AdvQueryCondition> list=entityDao.findByHql(sql, entity.getCode());
		for(AdvQueryCondition advQueryCondition:list ){
			String conSql="from AdvQueryConditionItem as aqci where aqci.condition.id=?";
			//String conSql="select aqci.id from EC_ADV_QUERY_CONDITION_ITEM aqci where  aqci.CONDITION_ID=?";
			List<AdvQueryConditionItem> topList=entityDao.findByHql(conSql, advQueryCondition.getId());
			for(AdvQueryConditionItem itemId:topList ){
				deleteAdcItem(itemId);
			}
			template.delete(advQueryCondition);
			//entityDao.createNativeQuery("delete from EC_ADV_QUERY_CONDITION_ITEM where CONDITION_ID=?",Long.valueOf(conditionId.toString())).executeUpdate();
		}
		for(AdvQueryCondition a:list){
			template.delete(a);
		}
	}
	public void deleteAdcItem(AdvQueryConditionItem item){
		HibernateTemplate template = new HibernateTemplate(sessionFactory);
		String conSql="from AdvQueryConditionItem as aqci where  aqci.parent.id=?";
		List<AdvQueryConditionItem> childrenItem=entityDao.findByHql(conSql, item.getId());
		if(null!=childrenItem&&childrenItem.size()>1){
			for(AdvQueryConditionItem childId:childrenItem){
				deleteAdcItem(childId);
			}
		}
		if(null!=childrenItem&&childrenItem.size()==1){
			template.delete(childrenItem.get(0));
		}
		
	}
	public void checkContraintProperty(Entity entity, String language){
		String sql=" from Property as p where p.model.entity.code!=? and p.associatedProperty.model.entity.code=?";
		Object[] params=new Object[2];
		params[0]=entity.getCode();
		params[1]=entity.getCode();
		List<Property> pList=entityDao.findByHql(sql, params);
		String contraintStr="";
		String propertyStr="";
		//List<String> internatioanalList=new ArrayList<String>();
		if(null!=pList&&pList.size()>0){
			for(Property p:pList){
				String inName=InternationalResource.get(p.getModel().getEntity().getName());
				String proName=InternationalResource.get(p.getName());
				contraintStr+=","+inName;
				propertyStr+=","+proName;
			}
		}		
		if(!"".equals(contraintStr)){
			throw new EcException(EcException.Code.CONTRAINT_BY_OTHER ,contraintStr.substring(1),propertyStr.substring(1));
		}
	}
	
	@Override
    @Transactional
	public void updateEntityProcessMobile(String entityCode){
		Object[] params=new Object[1];
		params[0]=entityCode;
		String contrainSql="update wf_deployment  set mobilequery=0,mobileinitiate=0 where entity_code=?";
		entityDao.createNativeQuery(contrainSql, params).executeUpdate();
		
	}
	
	@Transactional
	private void deleteInfoByModel(Entity entity){
		Object[] params=new Object[1];
		params[0]=entity.getCode();
		String contrainSql="update EC_PROPERTY set  ASSOCIATED_PROPERTY_CODE = null where MODEL_CODE in (select m.code from EC_MODEL m where m.ENTITY_CODE = ?) and ASSOCIATED_PROPERTY_CODE is not null";
		entityDao.createNativeQuery(contrainSql, params).executeUpdate();
		
	}
	
	@Transactional
	private void deleteInfoByView(View view) {
		Object[] bcParams= new Object[1];
		bcParams[0]=view.getCode();
		String bcSql="update EC_BUSINESS_CENTER set ASS_VIEW_CODE = null where ASS_VIEW_CODE = ?";
		entityDao.createNativeQuery(bcSql, bcParams).executeUpdate();
		Object[] shadowParams= new Object[1];
		shadowParams[0]=view.getCode();
		String shadowSql="update EC_VIEW set SHADOW_VIEW_CODE = null where SHADOW_VIEW_CODE = ?";
		entityDao.createNativeQuery(shadowSql, shadowParams).executeUpdate();
		Object[] refParams = new Object[1];
		refParams[0]=view.getCode();
		String refSql="update EC_VIEW set REFERENCE_VIEW_CODE = null where REFERENCE_VIEW_CODE = ?";
		entityDao.createNativeQuery(refSql, refParams).executeUpdate();
		Object[] btnParams = new Object[1];
		btnParams[0]=view.getCode();
		String btnSql="update EC_BUTTON set VIEWSELECT_CODE = null where VIEWSELECT_CODE = ?";
		entityDao.createNativeQuery(btnSql, btnParams).executeUpdate();
		// 视图是否在布局中被引用
		String sqlLayout = "update EC_EXTRA_VIEW set CONFIG = null where INSTR(CONFIG,'<vcode><![CDATA[" + view.getCode()
				+ "]]></vcode>') > 0";
		if (entityDao.getDBType() == IBaseDao.DBTYPE.MSSQL) {
			sqlLayout = "update EC_EXTRA_VIEW set CONFIG = null where CHARINDEX('<vcode><![CDATA[" + view.getCode()
					+ "]]></vcode>',CONFIG) > 0";
		} else if (entityDao.getDBType() == IBaseDao.DBTYPE.ORACLE) {
			sqlLayout = "update EC_EXTRA_VIEW set CONFIG = null where INSTR(CONFIG,'<vcode><![CDATA[" + view.getCode()
					+ "]]></vcode>') > 0";
		}
		entityDao.createNativeQuery(sqlLayout).executeUpdate();
	}
	

	private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
	
	@SuppressWarnings({ "unchecked", "deprecation" })
	private void sessionEvict(){
		Map<String, CollectionMetadata> roleMap = sessionFactory
				.getAllCollectionMetadata();
		for (String roleName : roleMap.keySet()) {
			if(roleName.startsWith(" com.supcon.supfsion.base.entities.")){
//				sessionFactory.evictCollection(roleName);
			}
		}
		Map<String, ClassMetadata> entityMap = sessionFactory
				.getAllClassMetadata();
		for (String entityName : entityMap.keySet()) {
			if(entityName.startsWith("com.supcon.supfusion.configuration.services.pojo.")){
//				sessionFactory.evictEntity(entityName);
			}
		}
	}
	
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public void migrateEntity(PrintWriter out, List<Entity> entities, Module targetModule) throws IOException {
		sessionEvict();
		if(beforeMigrate(entities,targetModule)){
			out.write("[" + DateUtil.getTime(new Date(), dateFormat) + "] 关联检查.........完成 ！ <br />");
			out.flush();
			Map<Entity,List<Model>> modelMap = new HashMap<Entity, List<Model>>();
			Map<Entity,List<View>> viewMap = new HashMap<Entity, List<View>>();
			Map<Entity,List<DataGrid>> dgMap = new HashMap<Entity, List<DataGrid>>();
			List<SpecialPermission> srcPermission = new ArrayList<SpecialPermission>();
			for(Entity entity : entities){
				srcPermission.addAll(specialPermissionService.findSpecialPermissionsByCode(entity.getCode(), "entityCode"));
				List<Model> srcModel = modelService.findModels(entity);
				List<View> views = viewService.findViewList(entity);
				List<DataGrid> dataGrids = dataGridService.findDataGrids(Restrictions.eq("valid", true),Restrictions.eq("entityCode", entity.getCode()));
				List<View> srcView = new ArrayList<View>();
				if (views != null && !views.isEmpty()) {
					for (int i = 0; i < views.size(); i++) {
						if (views.get(i).getType().equals(ViewType.EDIT)) {
							srcView.add(0, views.get(i));
						} else {
							srcView.add(views.get(i));
						}
					}
				}
//				List<Script> srcScript = scriptService.find(entity.getCode());
				Set<Entity> targetEntity = targetModule.getEntities();
				String entityCode = targetModule.getCode() + "_" + entity.getEntityName();
				Entity tempe = new Entity();
				tempe.setCode(entityCode);
				if(targetEntity!=null&&targetEntity.contains(tempe)){
					tempe = getEntity(entityCode);
					BeanUtils.copyProperties(entity, tempe,new String[]{"code","version","module","models","views"});
				}else{
					//add
					tempe.setCode(null);
					tempe.setModule(targetModule);
					BeanUtils.copyProperties(entity, tempe,new String[]{"code","module","models","views"});
				}
				saveEntity(tempe);
				out.write("[" + DateUtil.getTime(new Date(), dateFormat) + "] 复制实体:"+entity.getEntityName()+".............完成！ <br />");
				out.flush();
				modelMap.put(tempe, srcModel);
				viewMap.put(tempe, srcView);
				dgMap.put(tempe, dataGrids);
				//TODO migrate script
//				if(srcScript!=null&&srcScript.size()>0){
//					for(Script script : srcScript){
//						script = scriptService.get(script.getEntityCode(), script.getScriptCode());
//						Script newscript = new Script();
//						BeanUtils.copyProperties(script, newscript);
//						newscript.setEntityCode(tempe.getCode());
//						scriptService.save(newscript);
//					}
//				}
			}
			
			Map<Property, Property> hashPropery = new HashMap<Property, Property>();
			
			//TODO migrate model
			Map<Model, Model> hashModel = new HashMap<Model, Model>();
			
			if(modelMap!=null&&modelMap.size()>0){
				hashModel = migrateModel(hashPropery,out,modelMap);
			}
			
			//TODO migrate view
			Map<View, View> hashView = new HashMap<View, View>();
			if (viewMap != null && viewMap.size() > 0) {
				hashView = migrateView(viewMap, dgMap, hashModel, hashPropery);
			}
			//TODO migrate sepcialpermission
			if(srcPermission!=null&&srcPermission.size()>0){
				migrateSpecialPermission(hashModel,hashPropery,hashView,srcPermission);
			}
			
			
			out.write("[" + DateUtil.getTime(new Date(), dateFormat) + "] 复制视图.............完成！ <br />");
			out.flush();
			sessionEvict();
		}
	}
	
	private void migrateSpecialPermission(Map<Model, Model> hashModel, Map<Property, Property> hashPropery, Map<View, View> hashView, List<SpecialPermission> permissions){
		for(SpecialPermission sp : permissions){
			Model srcModel = new Model();
			srcModel.setCode(sp.getModelCode());
			Model tarModel = hashModel.get(srcModel);
			if(tarModel != null){
				srcModel = tarModel;
			}
			
			Model srcTargetModel = new Model();
			srcTargetModel.setCode(sp.getTargetModelCode());
			Model tarTargetModel = hashModel.get(srcTargetModel);
			if(tarTargetModel != null){
				srcTargetModel = tarTargetModel;
			}
			Property srcProperty = sp.getProperty();
			Property tarProperty = hashPropery.get(srcProperty);
			if(tarProperty != null){
				srcProperty = tarProperty;
			}
			View refView = sp.getRefView();
			if(refView!=null){
				View tarView = hashView.get(refView);
				if(tarView!=null){
					refView = tarView;
				}
			}
			if(tarModel != null){
			String tarPermissionCode = tarModel.getModuleCode() + "_" +  srcProperty.getCode();
			SpecialPermission tarPermission = specialPermissionDao.get(tarPermissionCode);
			if(tarPermission == null){
				tarPermission = new SpecialPermission();
			}
			BeanUtils.copyProperties(sp, tarPermission);
			tarPermission.setCode(tarPermissionCode);
			tarPermission.setEntityCode(srcModel.getEntity().getCode());
			tarPermission.setModuleCode(srcModel.getModuleCode());
			tarPermission.setProperty(srcProperty);
			tarPermission.setTargetModelCode(srcTargetModel.getCode());
			tarPermission.setModelCode(srcModel.getCode());
			tarPermission.setRefView(refView);
			specialPermissionService.saveSpecialPermission(tarPermission);
		}
		}
	}
	
	@Autowired
	private ButtonService buttonService;

	private Map<View, View> migrateView(Map<Entity, List<View>> viewMap, Map<Entity, List<DataGrid>> dgMap, Map<Model, Model> hashModel,
                                        Map<Property, Property> hashProperty) {
		Map<View, View> refView = new HashMap<View, View>();
		Map<View, View> shadowView = new HashMap<View, View>();
		Map<View, View> batchPrintView = new HashMap<View, View>();
		Iterator<Entry<Entity, List<View>>> it=null;
		if(viewMap!=null) {
            it = viewMap.entrySet().iterator();
        }
		Map<View, View> hashView = new HashMap<View, View>();
		List<Button> checkButton = new ArrayList<Button>();
		Map<String,String> viewCodeReplaceMap = new HashMap<String, String>();
		Map<String,String> dgCodeReplaceMap = new HashMap<String, String>();
		if (null != viewMap && !viewMap.isEmpty()) {
			for(Entry<Entity, List<View>> entry : viewMap.entrySet()){
				String entityCode = entry.getKey().getCode();
				for(View view : entry.getValue()){
					viewCodeReplaceMap.put(view.getCode(), entityCode + "_" + view.getName());
				}
			}
		}
		if (null != dgMap && !dgMap.isEmpty()) {
			for(Entry<Entity, List<DataGrid>> entry : dgMap.entrySet()){
				for(DataGrid dataGrid : entry.getValue()){
					String oldViewCode = dataGrid.getView().getCode();
					String newCode = viewCodeReplaceMap.get(oldViewCode);
					if(null != newCode){
						dgCodeReplaceMap.put(dataGrid.getCode(), newCode + dataGrid.getName());
					}
				}
			}
		}
		while (it!=null&&it.hasNext()) {
			Entry<Entity, List<View>> item = it.next();
			Entity tarEntity = item.getKey();
			List<View> srcView = item.getValue();
			List<View> tarViews = viewService.findViewList(tarEntity);
			if (srcView != null && srcView.size() > 0) {
				for (View view : srcView) {
					View tempv = new View();
					String viewCode = tarEntity.getCode() + "_" + view.getName();
					tempv.setCode(viewCode);
					tempv.setEntity(tarEntity);
					tempv.setName(view.getName());
					int tarIndex = tarViews.indexOf(tempv);
					View tarView = null;
					if (tarIndex >= 0) {
						// viewService.deleteViewForMigrate(viewCode);
						tarView = tarViews.get(tarIndex);
						tempv = tarView;
					} else {
						tempv.setType(view.getType());
						tempv.setTitle(view.getTitle());
						tempv.setVersion(tarView == null ? 0 : tarView.getVersion() + 1);
						tempv.setDisplayName(view.getDisplayName());
						Model assModel = hashModel.get(view.getAssModel());// new assModel
						assModel = modelService.getModel(assModel.getCode());
						tempv.setAssModel(assModel);
					}
					// ref
					View ref = view.getReference();
					if (ref != null) {
						refView.put(tempv, ref);
					}
					// shadow
					View shadow = view.getShadowView();
					if (shadow != null) {
						shadowView.put(tempv, shadow);
					}
					// batchPrint
					View batch = view.getBatchControlPrintSelectView();
					if (batch != null) {
						batchPrintView.put(tempv, batch);
					}
					viewService.migrateView(view, tempv, hashProperty, true, viewCodeReplaceMap, dgCodeReplaceMap);
					hashView.put(view, tempv);
					List<Button> btns = buttonService.getButtons(viewCode);
					if (btns != null && btns.size() > 0) {
						Iterator<Button> buttonIterator = btns.iterator();
						while (buttonIterator.hasNext()) {
							Button button = buttonIterator.next();
							if (button.getViewSelect() != null) {
								checkButton.add(button);
							}
						}
					}
				}
			}
		}

		if (refView != null && refView.size() > 0) {
			Iterator<Entry<View, View>> vIt = refView.entrySet().iterator();
			while (vIt.hasNext()) {
				Entry<View, View> item = vIt.next();
				View tar = item.getKey();
				View ref = item.getValue();
				// 参照视图同时复制，需要修改
				View ref2 = hashView.get(ref);
				if (ref2 != null) {
					ref = ref2;
				}
				tar = viewService.load(tar.getCode());
				tar.setReference(ref);
				viewService.mergeView(tar);
			}
		}

		if (shadowView != null && shadowView.size() > 0) {
			Iterator<Entry<View, View>> vIt = shadowView.entrySet().iterator();
			while (vIt.hasNext()) {
				Entry<View, View> item = vIt.next();
				View tar = item.getKey();
				View shadow = item.getValue();
				// 影子视图同时复制，需要修改
				View shadow2 = hashView.get(shadow);
				if (shadow2 != null) {
					shadow = shadow2;
				}
				tar = viewService.load(tar.getCode());
				tar.setShadowView(shadow);
				viewService.mergeView(tar);
			}
		}

		if (batchPrintView != null && batchPrintView.size() > 0) {
			Iterator<Entry<View, View>> vIt = batchPrintView.entrySet().iterator();
			while (vIt.hasNext()) {
				Entry<View, View> item = vIt.next();
				View tar = item.getKey();
				View batch = item.getValue();
				// 影子视图同时复制，需要修改
				View batch2 = hashView.get(batch);
				if (batch2 != null) {
					batch = batch2;
				}
				tar = viewService.load(tar.getCode());
				tar.setBatchControlPrintSelectView(batch);
				viewService.mergeView(tar);
			}

		}

		if (checkButton != null && checkButton.size() > 0) {
			Iterator<Button> iterator = checkButton.iterator();
			while (iterator.hasNext()) {
				Button btn = iterator.next();
				View selected = btn.getViewSelect();
				if (hashView != null) {
					if (hashView.containsKey(selected)) {
						btn.setViewSelect(hashView.get(selected));
						buttonService.mergeButton(btn);
					}
				}
			}
		}

		return hashView;
	}
	
	
	/**
	 * migrate model
	 * @param modelMap<newEntity,oldModel_list>
	 * @return map<oldModel,newModel>
	 */
	private Map<Model, Model> migrateModel(Map<Property, Property> hashPropery, PrintWriter out, Map<Entity,List<Model>> modelMap){
		Map<Model, Model> returnMap = new HashMap<Model, Model>();
		Map<Model,List<Property>> propertyMap = new HashMap<Model, List<Property>>();
		Iterator<Entry<Entity, List<Model>>> it = modelMap.entrySet().iterator();
		while(it.hasNext()){
			Entry<Entity, List<Model>> enset = it.next();
			Entity tarEntity = enset.getKey();
			List<Model> tarModels = modelService.findModels(tarEntity);
			List<Model> srcModels = enset.getValue();
			if(srcModels!=null&&srcModels.size()>0){
				for(Model model:srcModels){
					List<Property> srcProperty = modelService.findProperties(Restrictions.eq("valid", true), Restrictions.eq("model.code", model.getCode()));
					List<CustomCode> srcCustomCode = customCodeDao.findByProperty("modelCode", model.getCode());
					String modelCode = tarEntity.getCode()+"_"+model.getModelName();
					Model tempm = new Model();
					tempm.setCode(modelCode);
					if(tarModels!=null&&tarModels.contains(tempm)){
						Model tarModel = modelService.getModel(modelCode);
						tarModel.setName(model.getName());
						tarModel.setTableName(null);
						tarModel.setJpaName(null);
						tarModel.setDescription(model.getDescription());
						tarModel.setIsExtraCol(model.getIsExtraCol());
						tarModel.setIsCache(model.getIsCache());
						tarModel.setEnableSync(model.getEnableSync());
						tarModel.setIsControl(model.getIsControl());
						modelService.saveModel(tarModel);
						returnMap.put(model, tarModel);
						propertyMap.put(tarModel, srcProperty);
					}else{
						BeanUtils.copyProperties(model, tempm,new String[]{"properties","version"});
						tempm.setOrgTableName(null);
						tempm.setCode(null);
						tempm.setEntity(tarEntity);
						tempm.setTableName(null);
						tempm.setJpaName(null);
						tempm.setModuleCode(tarEntity.getModule().getCode());
						modelService.saveModel(tempm);
						returnMap.put(model, tempm);
						propertyMap.put(tempm, srcProperty);
					}
					out.write("[" + DateUtil.getTime(new Date(), dateFormat) + "] 复制模型:"+model.getModelName()+".............完成！ <br />");
					out.flush();
					//TODO migrate customercode
					if(srcCustomCode!=null&&srcCustomCode.size()>0){
						for(CustomCode cc : srcCustomCode){
							//target customcode
							CustomCode c = customCodeDao.findEntityByCriteria(Restrictions.eq("moduleCode", tarEntity.getModule().getCode()),Restrictions.eq("entityCode", tarEntity.getCode()),Restrictions.eq("modelCode", tempm.getCode()), Restrictions.eq("type", cc.getType()),
									Restrictions.eq("subType", cc.getSubType()));
							if(c!=null){//exist
								c.setCustomCode(cc.getCustomCode());
								customCodeDao.save(c);
							}else{
								c = new CustomCode(tarEntity.getCode(), tempm.getCode(), cc.getCustomCode(), cc.getType(), cc.getSubType());
								String ccCode = tarEntity.getModule().getCode()+"_"+tempm.getCode()+"_"+cc.getType()+"_"+cc.getSubType();
								c.setCode(ccCode);
								customCodeDao.save(c);
							}
						}
					}
					out.write("[" + DateUtil.getTime(new Date(), dateFormat) + "] 复制自定义代码.......完成！ <br />");
					out.flush();
				}
			}
		}
		//TODO migrate property
		if(propertyMap!=null&&propertyMap.size()>0){
			migrateProperty(propertyMap,hashPropery);
		}
		out.write("[" + DateUtil.getTime(new Date(), dateFormat) + "] 复制字段.............完成！ <br />");
		out.flush();
		
		return returnMap;
	}
	
	
	/**
	 * migrateProperty
	 * @param propertyMap <newModel,oldproperty_lsit>
	 */
	private void migrateProperty(Map<Model,List<Property>> propertyMap, Map<Property, Property> hashproperty){
		Iterator<Entry<Model, List<Property>>>  it = propertyMap.entrySet().iterator();
		List<Property> checkObject = new ArrayList<Property>();
		while(it.hasNext()){
			Entry<Model, List<Property>> item = it.next();
			Model tarModel = item.getKey();
			//tar_model = modelService.getModel(tar_model.getCode());
			List<Property> srcProperty = item.getValue();
			List<Property> tarProperty = modelService.findProperties(tarModel);
			if(srcProperty!=null&&srcProperty.size()>0){
				for(Property property:srcProperty){
					Property tempp = new Property();
					//String propertyCode = tar_model.getCode()+"_"+property.getName();
					tempp.setCode(null);
					tempp.setModel(tarModel);
					tempp.setModuleCode(tarModel.getModuleCode());
					tempp.setEntityCode(tarModel.getEntity().getCode());
					if(!property.getIsInherent()){
						if(tarProperty!=null&&tarProperty.contains(tempp)){
							tempp = tarProperty.get(tarProperty.indexOf(tempp));
							BeanUtils.copyProperties(property, tempp,new String[]{"code","version","model","moduleCode","entityCode"});
						}else{
							BeanUtils.copyProperties(property, tempp);
							tempp.setModel(tarModel);
							//String code = tempp.getModel().getCode() + "_" + tempp.getName();
							tempp.setModuleCode(tarModel.getModuleCode());
							tempp.setEntityCode(tarModel.getEntity().getCode());
							tempp.setCode(null);
						}
						if(tempp.getType().equals(DbColumnType.OBJECT) && !tempp.getIsCustom()){
							if(property.getAssociatedProperty().getModuleCode().equals(property.getModel().getModuleCode())){//不关联共同迁移的实体
								checkObject.add(tempp);//wait associated check 
							}else {
								if(!property.getAssociatedProperty().getModuleCode().equals(tarModel.getModuleCode()) && !"sysbase_1.0".equals(property.getAssociatedProperty().getModuleCode())){//不关联自己
									addRelate(tarModel.getModuleCode(),tempp.getAssociatedProperty().getModuleCode());
								}
								modelService.saveProperty(tempp);
							}
						}else{
							modelService.saveProperty(tempp);
						}
					}
					hashproperty.put(property, tempp);
				}
			}
		}
		
		if(checkObject!=null&&checkObject.size()>0){
			for(Property property : checkObject){
				if(hashproperty!=null&&hashproperty.containsKey(property.getAssociatedProperty())){
					property.setAssociatedProperty(hashproperty.get(property.getAssociatedProperty()));
				}else{
					String tarModuleCode = property.getAssociatedProperty().getModuleCode();
					if(!"sysbase_1.0".equals(tarModuleCode)){
						addRelate(property.getModel().getModuleCode(),property.getAssociatedProperty().getModuleCode());
					}
					
				}
				modelService.saveProperty(property);
			}
		}
	}
	
	
	private void addRelate(String moduleCode,String tarModuleCode){
		Module module = moduleService.getModule(moduleCode);
		Module tarModule = moduleService.getModule(tarModuleCode);
		ModuleRelation relation = moduleService.getRelation(module, tarModule, false);
		if (relation == null) {
			relation = new ModuleRelation();
		}
		relation.setModule(module);
		relation.setTarget(tarModule);
		relation.setCode(moduleCode + tarModuleCode);
		relation.setValid(true);
		moduleService.saveRelation(relation);
	}
	
	private boolean beforeMigrate(List<Entity> entities, Module targetModule){
		for(Entity entity : entities){
			List<Model> models = modelService.findModels(entity);
			if(models!=null && models.size()>0){
				for(Model model:models){
					List<Property> properties = modelService.findProperties(model);
					if(properties!=null&&properties.size()>0){
						for(Property p:properties){
							if(p.getType().equals(DbColumnType.OBJECT) && !p.getIsInherent() && !p.getIsCustom()){
								String entityCode = p.getAssociatedProperty().getEntityCode();
								Entity tempentity = new Entity();
								tempentity.setCode(entityCode);
								if(entities.contains(tempentity)){
									continue;
								}
								if(p.getAssociatedProperty().getModuleCode().equals(targetModule.getCode())){
									continue;
								}
								Property asp = modelService.getProperty(p.getAssociatedProperty().getCode());
								Module m = new Module();
								m.setCode(asp.getModuleCode());
								if(checkModule(m, targetModule)){
									continue;
								}
							}
						}
					}
				}
			}
		}
		return true;
	}
	
	private boolean checkModule(Module module, Module tarModule){
		List<ModuleRelation> relation = moduleService.getRelations(module);
		if(relation!=null&&relation.size()>0){
			for(ModuleRelation re:relation){
				if(re.getTarget().equals(tarModule)){
					throw new EcException(EcException.Code.MOUDLE_CYCLE_DEPEND,module.getCode()+"-->"+tarModule.getCode());
				}else{
					Module reModule = moduleService.getModule(re.getTarget().getCode(),false);
					return checkModule(reModule, tarModule);
				}
			}
		}
		return true;
	}

	@Override
	public Set<String> checkDelete(Entity entity){
		if(null == entity){
			return null;
		}
		String code = entity.getCode();
		//获取模块的依赖关系
		String moduleRelationSql = "SELECT MODULE_CODE FROM EC_MODULE_RELATION WHERE TARGET_MODULE_CODE = ?";
		List<String> relationCodes = modelDao.createNativeQuery(moduleRelationSql, entity.getModule().getCode()).list();
		if(null == relationCodes){
			relationCodes = new ArrayList<String>();
		}
		relationCodes.add(entity.getModule().getCode());
		long timestamp = System.currentTimeMillis();
		List<String> msgs = new LinkedList<String>();
		List<Object> propertyArgs = new LinkedList<Object>();
		String propertyHql = "from Property where valid=true and type = ?0 and associatedProperty.entityCode = ?1 and entityCode != ?1 and moduleCode in (:relationCodes)";
		propertyArgs.add(DbColumnType.OBJECT);
		propertyArgs.add(code);
		List<Property> properties = propertyDao.createQuery(propertyHql, propertyArgs.toArray()).setParameterList("relationCodes", relationCodes).list();
		if(null != properties && !properties.isEmpty()){
			for(Property property : properties){
				String msg = InternationalResource.get(property.getModel().getEntity().getModule().getName())+ "模块-"
						+ InternationalResource.get(property.getModel().getEntity().getName()) + "实体-"
						+ InternationalResource.get(property.getModel().getName()) + "模型-"
						+ InternationalResource.get(property.getName()) + "字段";
				msgs.add(msg);

			}
		}
		log.info("===========查询字段依赖耗时:" + (System.currentTimeMillis() - timestamp) + "ms");
		timestamp = System.currentTimeMillis();
		//检查视图
		Set<View> viewSet = new HashSet<View>();

		String fieldHql = "from Field where valid=true and entityCode != ?0 and moduleCode in (:relationCodes)";
		List<Object> fieldArgs = new LinkedList<Object>();
		fieldArgs.add(code);
		List<Field> fields = fieldDao.createQuery(fieldHql, fieldArgs.toArray()).setParameterList("relationCodes", relationCodes).list();
		if (fields != null && !fields.isEmpty()) {
			for (Field field : fields) {
				if (!StringUtils.isEmpty(field.getConfig()) && (field.getConfig().contains("<referenceview><![[CDATA[[" + code)
						|| field.getConfig().contains("<linkView><![[CDATA[[" + code)
						||  field.getConfig().contains("<allowviewcode><![[CDATA[[" + code))) {
					if (field.getView() != null) {
						viewSet.add(field.getView());
					}
					if (field.getDataGrid() != null) {
						viewSet.add(field.getDataGrid().getView());
					}
				}
			}
		}
		log.info("===========查询EC_FIELD耗时:" + (System.currentTimeMillis() - timestamp) + "ms");

		timestamp = System.currentTimeMillis();
		String extraViewHql = "from ExtraView where view.valid=true and view.entity.code != ?0 and view.moduleCode in (:relationCodes)";
		List<Object> extraViewArgs = new LinkedList<Object>();
		extraViewArgs.add(code);
		List<ExtraView> extraViews = extraViewDao.createQuery(extraViewHql, extraViewArgs.toArray()).setParameterList("relationCodes", relationCodes).list();
		if (extraViews != null && !extraViews.isEmpty()) {
			for (ExtraView extraView : extraViews) {
				if (!StringUtils.isEmpty(extraView.getConfig()) && ((extraView.getConfig().contains("<tree_model><![[CDATA[[" + code)
						|| extraView.getConfig().contains("<vcode><![[CDATA[[" + code)
						|| extraView.getConfig().contains("<treeView><![[CDATA[[" + code)))) {
					if (extraView.getView() != null) {
						viewSet.add(extraView.getView());
					}
				}
			}
		}
		log.info("===========查询EC_EXTRA_VIEW耗时:" + (System.currentTimeMillis() - timestamp) + "ms");

		for(View view : viewSet){
			String msg = InternationalResource.get(view.getEntity().getModule().getName())+ "模块-"
					+ InternationalResource.get(view.getEntity().getName()) + "实体-"
					+ InternationalResource.get(view.getDisplayName()) + (view.getCode().endsWith("__mobile__")?"移动视图":"视图");
			msgs.add(msg);
		}
		return StringUtils.sort(msgs);
	}

}
