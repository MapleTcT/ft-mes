/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.

 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.entities.SystemEntity;
import com.supcon.supfusion.base.enums.SystemDisplayType;
import com.supcon.supfusion.base.services.BaseServiceImpl;
import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.configuration.services.dao.*;
import com.supcon.supfusion.configuration.services.entity.Property;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.*;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.service.*;
import com.supcon.supfusion.configuration.services.utils.*;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import flexjson.JSONDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.StaleObjectStateException;
import org.hibernate.criterion.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.File;
import java.sql.*;
import java.util.Date;
import java.util.*;

@Slf4j
@ServiceApiService
@Transactional
public class ModelServiceImpl extends BaseServiceImpl<Model> implements ModelService, ResourceLoaderAware, InitializingBean {


	// ~ Instance fields =======================================================
	@Autowired
	private ModelDaoImpl modelDao;
	@Autowired
	private DataSource dataSource;
	@Autowired
	private PropertyDaoImpl propertyDao;
	@Autowired
	private FastQueryJsonDaoImpl fastQueryJsonDao;
	@Autowired
	private AdvQueryJsonDaoImpl advQueryJsonDao;
	@Autowired
	private PropertyService propertyService;
	@Autowired
	private ModuleService moduleService;
	@Autowired
	private EntityService entityService;
	@Autowired
	private ViewService viewService;
	@Autowired
	private SpecialPermissionService specialPermissionService;
	@Autowired
	private DataGridService dataGridService;

	private ResourceLoader resourceLoader;
	private Set<String> javaKeyWords;
	private Set<String> dbKeyWords;
	@Autowired
	private ViewDaoImpl viewDao;
	@Autowired
	private DataGridDaoImpl dataGridDao;
	@Autowired
	private FieldService fieldService;
	@Autowired
	private PropertyKeyService propertyKeyService;
	@Autowired
	private InternationalService internationalService;
	@Resource
	private CustomPropertyModelMappingDaoImpl customPropertyModelMappingDao;
	@Resource
	private CustomPropertyViewMappingDaoImpl customPropertyViewMappingDao;

	private static final String CACHE_MODEL_PROPERTY_PREFIX = "model_property";
	@Autowired
	private JdbcTemplate template;
	@Autowired
	private SqlModelService sqlModelService;
	@Autowired
	private EntityDaoImpl entityDao;
	@Autowired
	private ModuleDaoImpl moduleDao;

	private static String dbName;
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

//			cache = cacheAdmin.getCache(CACHE_NAME);
	}

	/*
	 * private void loadKeyWords(String type, Set<String> set) throws IOException {
	 * Resource resource = this.resourceLoader.getResource("classpath:META-INF/orchid/keyfile-" + type + ".txt");
	 * if (null != resource) {
	 * String content = FileUtils.readFileToString(resource.getFile(), "utf-8");
	 * if (!StringUtils.isBlank(content)) {
	 * for (String s : content.split(",")) {
	 * if ("db".equals(type))
	 * set.add(s.trim().toUpperCase());
	 * else
	 * set.add(s.trim());
	 * }
	 *
	 * }
	 * }
	 * }
	 */

	/*
	 * @Override
	 * public Model getModel(long id) {
	 * return modelDao.findEntityByHql("from Model where id = ? and valid = true", id);
	 * }
	 */

	@Override
	public Model getModelWithProperties(String code) {
		Model model = modelDao.findEntityByHql("from Model where code = ? and valid = true", code);
		List<Property> propertiesList = propertyService.getProperties(model.getCode());
		HashSet<Property> properties = new HashSet<>(propertiesList);
		model.setProperties(properties);
		//此方法无法获取property
//		Hibernate.initialize(model.getProperties());
		return model;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Cacheable(value = "", key = "#code")
	public Model getModel(String code) {
		if (org.apache.commons.lang3.StringUtils.isEmpty(code)) {
			return null;
		}
		Model model = modelDao.load(code);
		if (null != model) {
			if (!model.isValid()) {
				return null;
			}
		} else {
			return model;
		}
		return model;
	}

	@Transactional
	private Model getModelByNoValid(String code) {
		return modelDao.load(code);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public boolean firstIsMain(Entity entity) {
		List<Object> parameters = new LinkedList<Object>();
		String hql = "";
		hql = "select count(m.code) as totalCoual from Model as m where m.entity=? and  m.isMain = true and valid=true";
		parameters.add(entity);
		Object[] params = new Object[parameters.size()];
		List<Long> modelCount = modelDao.findByHql(hql, parameters.toArray(params));
		if (modelCount.get(0) == 0) {
			return true;
		}
		return false;
	}

	public void checkModelInfo(Model model) {
		if (!checkNameUnique(model)) {
			throw new EcException(EcException.Code.UNIQUE_MODEL_NAME);
		}
		if (!checkIsMainUnique(model)) {
			if (model.getIsMain()) {
				throw new EcException(EcException.Code.UNIQUEISMAIN);
			}
		}
		if (!checkCodeUnique(model)) {
			throw new EcException(EcException.Code.UNIQUECODE);
		}
		if (!checkModelNamekey(model)) {
			throw new EcException(EcException.Code.KEY);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void saveModel(Model model) {
		Entity entity = entityService.getEntity(model.getEntity().getCode());
		String orgTableName = model.getOrgTableName();
		boolean isNew = false;
		if (null != entity && null != entity.getModule()) {
			Module module = moduleService.getModule(entity.getModule().getCode());
			checkModelInfo(model);
			if (null == model.getCode() || model.getCode().length() == 0) {
				isNew = true;
				String code = model.getEntity().getCode() + "_" + model.getModelName();
				model.setCode(code);
				model.setEntity(entity);
				model.setModuleCode(entity.getModule().getCode());
				model.setTableName(model.getTableName());
				if (!(model.getType() != null && model.getType() == Model.TYPE_SQL)) {
					createInherentProperties(model);
				}
			}

			if ("2.0".equals(model.getEcVersion()) && (model.getTableName()).length() > 27) {
				throw new EcException(EcException.Code.TOO_LONG);
			}
			if(null!=module&&null==model.getJpaName()){
				model.setJpaName(firstLetterToUpper(module.getArtifact()) + model.getModelName());
			}
		}
		modelDao.save(model);
		if (null != model.getExtendsModelName()) {
			Set<Property> pro = getModel(model.getExtendsModelName().getCode()).getProperties();

			for (Property copyPro : pro) {
				if (!copyPro.getIsInherent()) {
					List<Property> pros = propertyDao.createQuery("from Property where valid=true and code=?",
							model.getEntity().getCode() + "_" + model.getModelName() + "_" + copyPro.getName()).list();
					if (null != pros && pros.size() == 0) {
						Property ac1 = new Property();
						ac1.setName(copyPro.getName());
						ac1.setCode(model.getEntity().getCode() + "_" + model.getModelName() + "_" + copyPro.getName());
						ac1.setDisplayName(copyPro.getDisplayName());
						ac1.setIsInherent(copyPro.getIsInherent());
						ac1.setType(copyPro.getType());
						ac1.setFormat(copyPro.getFormat());
						ac1.setIsUsedForList(true);
						ac1.setNullable(copyPro.getNullable());
						ac1.setVersion(copyPro.getVersion());

						ac1.setAssociatedProperty(copyPro.getAssociatedProperty());
						ac1.setAssociatedType(copyPro.getAssociatedType());
						ac1.setModel(model);

						ac1.setModuleCode(model.getCode());
						ac1.setEntityCode(model.getEntity().getCode());
						propertyDao.save(ac1);
					}

				}
			}
		}
		// TODO 需要更加优化的方法
		//modelDao.flush();
		model.setOrgTableName(orgTableName);
		//	FileUtils.updateXml(model, isNew);
		if (null == model.getType() || Model.TYPE_SQL != model.getType()) {
			try {
				String dbName = getDbName();
				ModelSyncDBUtils.modelSyncToDb(entity, model, isNew, template, dbName);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		saveModuleGenerateInfo(model);
	}

	private String firstLetterToUpper(String str) {
		if (null == str)
			return null;
		if (str.length() > 0) {
			return Character.toUpperCase(str.charAt(0)) + str.substring(1);
		}
		return "";
	}
	
	/*
	 * 判断同一实体下模型名称唯一
	 */
	private boolean checkNameUnique(Model model) {
		List<Object> parameters = new LinkedList<Object>();
		StringBuffer sql = new StringBuffer("select count(CODE) as totalCoual from ");
		sql.append(Model.TABLE_NAME).append(" where lower(MODEL_NAME) = ? and VALID = 1");
		if (StringUtils.isEmpty(model.getCode())) {
			parameters.add(model.getModelName().toLowerCase());
		} else {
			sql.append(" and lower(CODE) != ?");
			parameters.add(model.getModelName().toLowerCase());
			parameters.add(model.getCode().toLowerCase());
		}
		Object[] params = new Object[parameters.size()];
		List<Number> modelCount = modelDao.createNativeQuery(sql.toString(), parameters.toArray(params)).list();
		if (modelCount.get(0).intValue() == 0) {
			return true;
		}
		return false;
	}

	/*
	 * 判断主模型唯一
	 */
	private boolean checkIsMainUnique(Model model) {
		List<Object> parameters = new LinkedList<Object>();
		String hql = "";
		Entity entity = model.getEntity();
		if (model.getCode() == null || model.getCode().length() == 0) {
			hql = "select count(m.code) as totalCoual from Model as m where m.entity=? and  m.isMain = true and valid=true";
			parameters.add(entity);
		} else {
			hql = "select count(m.code) as totalCoual from Model as m where m.entity=? and m.isMain = true and m.code !=? and valid=true";
			parameters.add(entity);
			parameters.add(model.getCode());
		}
		Object[] params = new Object[parameters.size()];
		List<Long> modelCount = modelDao.findByHql(hql, parameters.toArray(params));
		if (modelCount.get(0) == 0) {
			return true;
		}
		return false;
	}

	/*
	 * code唯一性判断
	 */
	private boolean checkCodeUnique(Model model) {
		List<Object> parameters = new LinkedList<Object>();
		String hql = "";
		if (model.getCode() == null || model.getCode().length() == 0) {
			hql = "select count(m.code) as totalCoual from Model as m where code = ?0";
			parameters.add(model.getCode());
		} else {
			hql = "select count(m.code) as totalCoual from Model as m where m.code = ?0 and m.code !=?1";
			parameters.add(model.getCode());
			parameters.add(model.getCode());
		}
		Object[] params = new Object[parameters.size()];
		List<Long> modelCount = modelDao.findByHql(hql, parameters.toArray(params));
		if (modelCount.get(0) == 0) {
			return true;
		}
		return false;
	}

	@Override
	public String deleteModel(Model model) {
		Set descSet = new HashSet<String>();
		List<View> viewList = viewDao.findByCriteria(Restrictions.eq("assModel", model), Restrictions.eq("valid", true));
		if (viewList != null && viewList.size() > 0) {
			for (View view : viewList) {
				//descSet.add(InternationalResource.get(view.getAssModel().getName())+"模型被"+view.getName()+"视图关联！");
				descSet.add("被"+InternationalResource.get(view.getEntity().getName())+"实体中"+view.getName()+"视图关联！");
			}
			return JsonUtils.setToJson(descSet);
			//throw new BAPException(BAPException.Code.REFERENCE_VIEW);
		}
		String hql = " from Property as p where p.model.code != ?0 and p.associatedProperty.model.code = ?1";
		List<Property> properties = modelDao.findByHql(hql, new Object[] { model.getCode(), model.getCode() });
		if (null != properties && !properties.isEmpty()) {
			for (Property property : properties) {
				descSet.add(property.getName()+"字段被"+InternationalResource.get(property.getModel().getName())+"模型关联！");
			}
			return JsonUtils.setToJson(descSet);
			//throw new BAPException(BAPException.Code.ASS_BY_MODEL);
		}
		List<Property> list = propertyDao.findByHql("from Property as p where p.model.code = ?0", model.getCode());
		if (null != list && !list.isEmpty()) {
			for (Property p : list) {
				List<DataGrid> dataGrids = dataGridService.findDataGridsByProperty(p);
				if (null != dataGrids && !dataGrids.isEmpty()) {
					for (DataGrid dg : dataGrids) {
						dataGridService.deleteDataGrid(dg.getCode());
					}
				}
				deleteProperty(p);
			}
		}
		if (null != model.getType() && Model.TYPE_SQL == model.getType()) {
			sqlModelService.deleteSqlModel(model.getCode());
			sqlModelService.deleteDBView(model.getTableName());
		}
		Model persistedModel = modelDao.get(model.getCode());
		if (persistedModel == null || !Objects.equals(persistedModel.getVersion(), model.getVersion())) {
			throw new StaleObjectStateException(Model.class.getName(), model.getCode());
		}
		persistedModel.setValid(false);
		persistedModel.setDeleteTime(new Date());
		modelDao.update(persistedModel);

		//开始保存模块信息数据的最后修改时间
//		ModuleGenerateInfo generateInfo = moduleGenerateInfoService.getModuleGenerateInfoByModuleCode(model.getModuleCode());
//		if(generateInfo!=null){
//			generateInfo.setLastModifyTime(new Date());
//			moduleGenerateInfoService.save(generateInfo,Thread.currentThread());
//		}else{
//			generateInfo = new ModuleGenerateInfo();
//			generateInfo.setLastModifyTime(new Date());
//			generateInfo.setModuleCode(model.getModuleCode());
//			moduleGenerateInfoService.create(generateInfo,Thread.currentThread());
//		}
		return "";
	}

	@Override
	public String deleteModelPhysical(String modelCode, Boolean deleteType) {
		Model model = getModelByNoValid(modelCode);
		Set<String> descSet = new HashSet<String>();
		if (!deleteType) {
			if (null == model) {
				throw new StaleObjectStateException(Model.class.getName(), modelCode);
			}
		}
		if (null != model) {
			try {
				descSet = this.checkDeleteModel(model);
				if(null != descSet && !descSet.isEmpty()){
					return JsonUtils.setToJson(descSet);
				}
			} catch (Exception e) {
				log.error(e.getMessage(),e);
				throw new EcException("校验失败，详细错误信息请查看日志");
			}

			List<Property> list = propertyDao.findByHql("from Property as p where p.model.code = ?0", model.getCode());
			if (null != list && !list.isEmpty()) {
				for (Property p : list) {
					List<DataGrid> dataGrids = dataGridService.findDataGridsByProperty(p);
					if (null != dataGrids && !dataGrids.isEmpty()) {
						for (DataGrid dg : dataGrids) {
							dataGridService.deleteDataGridPhysical(dg);
						}
					}
					deletePropertyPhysical(p.getCode(), true);
					modelDao.clear();
					modelDao.flush();
				}
			}
			modelDao.deletePhysical(model);
			if (null != model.getType() && Model.TYPE_SQL == model.getType()) {
				sqlModelService.deleteSqlModel(model.getCode());
				sqlModelService.deleteDBView(model.getTableName());
			}
			log.info("删除"+model.getName()+"模型文件开始");
			this.deleteModuleFile(model);
			log.info("删除"+model.getName()+"模型文件结束");

			//开始保存模块信息数据的最后修改时间
//			ModuleGenerateInfo generateInfo = moduleGenerateInfoService.getModuleGenerateInfoByModuleCode(model.getModuleCode());
//			if(generateInfo!=null){
//				generateInfo.setLastModifyTime(new Date());
//				moduleGenerateInfoService.save(generateInfo,Thread.currentThread());
//			}else{
//				generateInfo = new ModuleGenerateInfo();
//				generateInfo.setLastModifyTime(new Date());
//				generateInfo.setModuleCode(model.getModuleCode());
//				moduleGenerateInfoService.create(generateInfo,Thread.currentThread());
//			}
		}
		return "";
	}

	/**
	 * 删除相应文件
	 * @param ecEntity
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public void deleteModuleFile(AbstractAuditUniqueCodeEntity ecEntity) {
		try {
			if (ecEntity instanceof Module) {
				Module module = (Module) ecEntity;
				File file = new File(PropertyHolder.get().getWorkspacePath() + File.separator + "generate" + File.separator + module.getCode());
				FileUtils.deleteDirectory(file);
				String templatePath = PropertyHolder.get().getViewPath() + File.separator + "\\views\\" + module.getArtifact();
				String customTempaltePath = PropertyHolder.get().getCustomFilePath() + File.separator + "\\views\\" + module.getArtifact();
				FileUtils.deleteDirectory(new File(templatePath));
				FileUtils.deleteDirectory(new File(customTempaltePath));
			} else if (ecEntity instanceof Entity || ecEntity instanceof Model || ecEntity instanceof View) {
				Set<Model> models = new HashSet<Model>();
				Module module = null;
				Entity entity = null;
				Model model = null;
				View view = null;
				if (ecEntity instanceof Entity) {
					entity = (Entity) ecEntity;
					models.addAll(findModels(entity));
					module = entity.getModule();
				} else if (ecEntity instanceof Model) {
					model = (Model) ecEntity;
					models.add(model);
					module = model.getEntity().getModule();
					entity = model.getEntity();
				} else {
					view = (View) ecEntity;
					entity = view.getEntity();
					module = view.getEntity().getModule();
					model = view.getAssModel();
				}
				if (null == module) {
					return;
				}
				// 模型文件目录
				String moduleFilePath = PropertyHolder.get().getWorkspacePath() + File.separator + "generate" + File.separator + module.getCode();
				String apiBasePath = moduleFilePath + File.separator + "api\\src\\main\\java\\com\\supcon\\orchid\\" + module.getArtifact();
				String coreBasePath = moduleFilePath + File.separator + "core\\src\\main\\java\\com\\supcon\\orchid\\" + module.getArtifact();
				String serviceBasePath = moduleFilePath + File.separator + "service\\src\\main\\java\\com\\supcon\\orchid\\" + module.getArtifact();
				String coreResourcesPath = moduleFilePath + File.separator + "core\\src\\main\\resources\\";
				String serviceResourcesPath = moduleFilePath + File.separator + "service\\src\\main\\resources\\";
				String viewsPath = moduleFilePath + File.separator + "service\\src\\main\\resources\\views\\" + module.getArtifact() + File.separator
						+ entity.getEntityName();
				if(module.getIsNewGenerate()){
					coreBasePath = moduleFilePath + File.separator + "core\\src\\generated\\java\\com\\supcon\\orchid\\" + module.getArtifact();
					serviceBasePath = moduleFilePath + File.separator + "service\\src\\generated\\java\\com\\supcon\\orchid\\" + module.getArtifact();
					coreResourcesPath = moduleFilePath + File.separator + "core\\src\\generated\\resources\\";
					serviceResourcesPath = moduleFilePath + File.separator + "service\\src\\generated\\resources\\";
					viewsPath = moduleFilePath + File.separator + "service\\src\\generated\\resources\\views\\" + module.getArtifact() + File.separator
							+ entity.getEntityName();
				}
				String templatePath = PropertyHolder.get().getViewPath() + File.separator + "\\views\\" + module.getArtifact() + File.separator + entity.getEntityName();
				String customTempaltePath = PropertyHolder.get().getCustomFilePath() + File.separator + "\\views\\" + module.getArtifact() + File.separator
						+ entity.getEntityName();
				String[] entitySubfixs = new String[] { "Convertor", "Acl", "GroupInfo", "DealInfo", "Supervision", "PayCloseAttention", "MneCode", "EditEntity" };
				String javaSub = ".java";
				List<String> fileNames = new ArrayList<String>();
				for (Model mod : models) {
					String fileName = "2.0".equals(mod.getEcVersion()) ? mod.getJpaName() : mod.getModelName();
					fileNames.add(fileName);
				}
				List<File> deleteFiles = new ArrayList<File>();

				// 删除api下的DTO和client，以及service.provider下的远程调用实现类
				File apiDtoFile = new File(apiBasePath + File.separator + "DTO");
				File apiClientFile = new File(apiBasePath + File.separator + "client");
				File apiFallBackFile = new File(apiBasePath + File.separator + "client" + File.separator + "fallback");
				File apiProviderFile = new File(serviceBasePath + File.separator + "provider");
				File apiProviderWrapperFile = new File(serviceBasePath + File.separator + "provider" + File.separator + "wrapper");
				for (String fileName : fileNames) {
					if (apiDtoFile.exists() && apiDtoFile.isDirectory()) {
						deleteFiles.add(new File(apiDtoFile.getAbsolutePath() + File.separator + fileName + "DTO" + javaSub));
						deleteFiles.add(new File(apiDtoFile.getAbsolutePath() + File.separator + fileName + "SupervisionDTO" + javaSub));
					}
					if (apiClientFile.exists() && apiClientFile.isDirectory()) {
						deleteFiles.add(new File(apiClientFile.getAbsolutePath() + File.separator + "I" + fileName + "Client" + javaSub));
					}
					if (apiFallBackFile.exists() && apiFallBackFile.isDirectory()) {
						deleteFiles.add(new File(apiFallBackFile.getAbsolutePath() + File.separator + fileName + "ClientFallBack" + javaSub));
					}
					if (apiProviderFile.exists() && apiProviderFile.isDirectory()) {
						deleteFiles.add(new File(apiProviderFile.getAbsolutePath() + File.separator + fileName + "Client" + javaSub));
					}
					if (apiProviderWrapperFile.exists() && apiProviderWrapperFile.isDirectory()) {
						deleteFiles.add(new File(apiProviderWrapperFile.getAbsolutePath() + File.separator + fileName + "Wrapper" + javaSub));
					}
				}
				
				// 删除core下的entity文件
				File entyityFile = new File(coreBasePath + File.separator + "entities");
				if (entyityFile.exists() && entyityFile.isDirectory() && !fileNames.isEmpty()) {
					for (String fileName : fileNames) {
						deleteFiles.add(new File(entyityFile.getAbsolutePath() + File.separator + fileName + javaSub));
						for (String subfix : entitySubfixs) {
							deleteFiles.add(new File(entyityFile.getAbsolutePath() + File.separator + fileName + subfix + javaSub));
						}
					}
				}
				// 删除core下的service文件
				String[] serviceSubfixs = new String[] { "Service", "CxfService", "ImportService", "WSService" };
				File serviceFile = new File(coreBasePath + File.separator + "services");
				if (serviceFile.exists() && serviceFile.isDirectory() && !fileNames.isEmpty()) {
					for (String fileName : fileNames) {
						for (String subfix : serviceSubfixs) {
							deleteFiles.add(new File(serviceFile.getAbsolutePath() + File.separator + fileName + subfix + javaSub));
						}
					}
				}
				// 删除core下的resources文件
				File coreResouceFile = new File(coreResourcesPath + File.separator + "com\\supcon\\orchid\\" + module.getArtifact() + "\\entities");
				if (coreResouceFile.exists() && coreResouceFile.isDirectory() && !fileNames.isEmpty()) {
					File[] fs= coreResouceFile.listFiles();
					if(fs != null){
						for (File file : fs) {
							if (file.isFile()) {
								for (String fileName : fileNames) {
									if (file.getName().startsWith(fileName + "-") && file.getName().endsWith("-validation-bap.xml")) {
										deleteFiles.add(file);
									}
								}
							}
						}
					}
				}
				// 删除service目录下的action文件
				String actionPath = serviceBasePath + File.separator + "actions";
				String daoPath = serviceBasePath + File.separator + "daos";
				String controllerPath = serviceBasePath + File.separator + "controllers";
				String daoImplPath = serviceBasePath + File.separator + "daos\\impl";
				String serviceImplPath = serviceBasePath + File.separator + "services\\impl";
				for (String fileName : fileNames) {
					deleteFiles.add(new File(actionPath + File.separator + fileName + "Action.java"));
					deleteFiles.add(new File(daoPath + File.separator + fileName + "Dao.java"));
					deleteFiles.add(new File(daoImplPath + File.separator + fileName + "DaoImpl.java"));
					deleteFiles.add(new File(controllerPath + File.separator + fileName + "Controller.java"));
					for (String subfix : serviceSubfixs) {
						deleteFiles.add(new File(serviceImplPath + File.separator + fileName + subfix + "Impl.java"));
					}
					deleteFiles.add(new File(serviceImplPath + File.separator + fileName + "VariablesProvider.java"));
				}
				// 删除service下resource的文件
				String validationPath = serviceResourcesPath + File.separator + "com\\supcon\\orchid\\" + module.getArtifact() + "\\actions";
				File validationFile = new File(validationPath);
				if (validationFile.exists() && validationFile.isDirectory()
						&& !fileNames.isEmpty()) {
					if (validationFile != null) {
						File[] fs = validationFile.listFiles();
						if (fs != null) {
							for (File file : fs) {
								if (file != null && file.isFile()) {
									for (String fileName : fileNames) {
										if (file.getName().startsWith(fileName + "Action-")&& file.getName().endsWith("-validation-bap.xml")) {
											deleteFiles.add(file);
										} else if (file.getName().startsWith(fileName + "-")&& file.getName().endsWith("-validation-bap.xml")) {
											deleteFiles.add(file);
										}
									}
								}
							}
						}
					}
				}
				// 删除视图文件
				if (ecEntity instanceof Entity) {
					deleteFiles.add(new File(viewsPath));
					deleteFiles.add(new File(templatePath));
					deleteFiles.add(new File(customTempaltePath));
				} else if (ecEntity instanceof Model) {
					String fileName = model.getModelName();
					deleteFiles.add(new File(viewsPath + File.separator + fileName));
					deleteFiles.add(new File(templatePath + File.separator + fileName));
					deleteFiles.add(new File(customTempaltePath + File.separator + fileName));
				} else if (ecEntity instanceof View) {
					String fileName = model.getModelName();
					deleteFiles.add(new File(viewsPath + File.separator + fileName + File.separator + view.getName() + ".ftl"));
					deleteFiles.add(new File(viewsPath + File.separator + fileName + File.separator + view.getName() + "-mobile.ftl"));
					deleteFiles.add(new File(templatePath + File.separator + fileName + File.separator + view.getName() + ".ftl"));
					deleteFiles.add(new File(templatePath + File.separator + fileName + File.separator + view.getName() + "-mobile.ftl"));
					deleteFiles.add(new File(customTempaltePath + File.separator + fileName + File.separator + view.getName() + ".ftl"));
					deleteFiles.add(new File(customTempaltePath + File.separator + fileName + File.separator + view.getName() + "-mobile.ftl"));
				}

				if (!deleteFiles.isEmpty()) {
					for (File file : deleteFiles) {
						if (file.exists()) {
							FileUtils.deleteDirectory(file);
						}
					}
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}


	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Model> findModels(Entity entity) {
		return modelDao.findByHql("from Model where entity = ?0 and valid = true", entity);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Model> findModels(Module module) {
		return modelDao.findByHql("from Model where entity.module = ? and valid = true", module);

	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Page<Model> findModels(Page<Model> page, Entity entity) {
		return modelDao.findByPage(page, Restrictions.eq("entity", entity), Restrictions.eq("valid", true));
	}

	/*
	 * @Override
	 * public Property getProperty(long id) {
	 * return propertyDao.findEntityByHql("from Property where id = ? and valid = true", id);
	 * }
	 */

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Property getProperty(String code) {
		// return propertyDao.findEntityByHql("from Property where code = ? and valid = true", code);
		Property p = null;
		List<Property> list = propertyDao.findByCriteria(Restrictions.eq("code", code), Restrictions.eq("valid", true));
		if (null != list && !list.isEmpty()) {
			p = list.get(0);
		}
		return p;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Property getPropertyWithModel(String code) {
		Property property = propertyDao.load(code);
		if (null != property) {
			Hibernate.initialize(property.getModel());
			Hibernate.initialize(property.getModel().getEntity());
			Hibernate.initialize(property.getModel().getEntity().getModule());
			if (property.getAssociatedProperty() != null) {
				Hibernate.initialize(property.getAssociatedProperty());
				Hibernate.initialize(property.getAssociatedProperty().getModel());
				Hibernate.initialize(property.getAssociatedProperty().getModel().getProperties());
				for (Property p : property.getAssociatedProperty().getModel().getProperties()) {
					if (p.getAssociatedProperty() != null) {
						Hibernate.initialize(p.getAssociatedProperty());
						Hibernate.initialize(p.getAssociatedProperty().getModel());
						Hibernate.initialize(p.getAssociatedProperty().getModel().getEntity());
						Hibernate.initialize(p.getAssociatedProperty().getModel().getEntity().getModule());
					}
				}
			}
		}
		return property;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	private Property getPropertyNoValid(String code) {
		return propertyDao.load(code);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public void saveProperty(Property property) {
		String orgColumnName = property.getOrgColumnName();
		if (!checkPropertyNameUnique(property)) {
			throw new EcException(InternationalResource.get("ec.property.code.unique"));
		}
		if(!checkColumnNameUnique(property)){
			throw new EcException(InternationalResource.get("ec.property.columnName.unique"));
		}
		if("BAP_GROUP_ID".equals(orgColumnName)&& (property.getIsGroupObject()==null ||(property.getIsGroupObject()!=null && !property.getIsGroupObject()))){
			throw new EcException(InternationalResource.get("ec.property.column.key"));
		}
		// 修改时,不能对名称进行修改,不对关字段进行判断
		if ((null == property.getCode() || property.getCode().length() == 0) && !property.getIsInherent() && !checkPropertyNamekey(property)) {
			throw new EcException(EcException.Code.KEY);
		}
		if (property.getType() == DbColumnType.OFFICE && !checkPropertyTypeUnique(property)) {
			throw new EcException(EcException.Code.UNIQUEOFFICE);
		}
		if (property.getType() == DbColumnType.SUMMARY && !checkPropertyTypeUnique(property)) {
			throw new EcException(EcException.Code.UNIQUE_SUMMARY);
		}
		if (DbColumnType.OBJECT.equals(property.getType())) {
			if((null == property.getCode() || property.getCode().length() == 0) && checkObjPropertyCode(property)){
				throw new EcException(EcException.Code.UNIQUENAME);
			}
			if(checkObjPropertyMainAssociated(property)){
				property.setIsMainAssociated(false);
				// 后期修改如果做抛异常处理，则需要处理displayname，将前缀和后缀去除
				// displayName=BEAM.propertydisplayName.randon1440815116608
				// displayName=key=BEAM.propertydisplayName.randon1440815116608$&#zh_CN=执行部门1
				String displayName = property.getDisplayName();
				if (StringUtils.isEmpty(displayName) || !displayName.startsWith("key=")) {
					throw new EcException(EcException.Code.UNIQUE_MAINASSOCIATED);
				}
				int splitLos = displayName.indexOf("$&#");
				property.setDisplayName(displayName.subSequence(4, splitLos).toString());
				throw new EcException(EcException.Code.UNIQUE_MAINASSOCIATED);
			}
		}
		boolean isNew = false;
		Model model = getModel(property.getModel().getCode());
		// 创建字段前，判断模型对应业务表是否被创建；如果没有创建，先创建
		if(!tableIsExist(model.getTableName().toUpperCase(), template)){
			try {
				String dbName = getDbName();
				ModelSyncDBUtils.modelSyncToDb(entityService.getEntity(model.getEntity().getCode()), model, true, template, dbName);
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		}
		if (null == property.getCode() || property.getCode().length() == 0) {
			String code = property.getModel().getCode() + "_" + property.getName();
			property.setCode(code);
			property.setModuleCode(model.getModuleCode());
			property.setEntityCode(model.getEntity().getCode());
			isNew = true;
		}

		if ((property.getColumnName()).length() > 28) {
			throw new EcException(EcException.Code.TOO_LONG);
		}

		// 业务主键唯一
		if (property.getIsBussinessKey()) {
			property.setNullable(false);
			// property.setIsUnique(true);
		}
		if (DbColumnType.MONEY.equals(property.getType())) {
			if (property.getDecimalNum() == null) {
				property.setDecimalNum(6);
				property.setMaxLength(null);
			}
		}
		if(DbColumnType.PICTURE.equals(property.getType())){
			property.setIsUsedForList(false);
		}
		// 当为关联属性时，强制为用于列表
		if (DbColumnType.OBJECT.equals(property.getType())) {
			propertyDao.merge(property);
			if (property.getIsCustom() == null || !property.getIsCustom()) {
				property.setIsUsedForList(Boolean.TRUE);
			}
			if (!property.getIsInherent() && !property.getIsCustom()) {
				// 主模型有关联属性时，建立业务中心关联
				property.setModel(getModel(property.getModel().getCode()));
				if(null == property.getIsCustom() || !property.getIsCustom()){
					if (property.getAssociatedProperty() != null && property.getAssociatedProperty().getCode() != null) {
						Property associated = getProperty(property.getAssociatedProperty().getCode());
						property.setAssociatedProperty(associated);
						if (associated != null && null != associated.getModel()) {
							Model assoModel = getModel(associated.getModel().getCode());
							if(!model.getCode().equals(assoModel.getCode()) && assoModel.getModelName().equalsIgnoreCase(model.getModelName())){
								throw new EcException(EcException.Code.SAME_MODEL_NAME_ERROR);
							}
							associated.setModel(assoModel);
						}
						//由业务中心只关联主模型改为都可以关联  bypl
						if (property.getModel().getIsMain() || true) {

							List<View> views = new ArrayList<View>();
							String code = property.getModel().getCode() + "_" + property.getCode() + "_" + property.getAssociatedProperty().getCode();

							code = property.getModel().getCode() + "_" + property.getAssociatedProperty().getCode() + "_" + property.getCode();
						}
					} else {
						throw new EcException(EcException.Code.ASS_PROPERTY_NOT_SELECTED,property.getCode());
					}
				}
			}
		}
		// 高级系统编码默认值处理
		if (property.getType().equals(DbColumnType.SYSTEMCODE) && property.getSeniorSystemCode() != null && property.getSeniorSystemCode()
				&& property.getDefaultValue() != null && property.getDefaultValue().length() > 0) {
			property.setDefaultValue(property.getDefaultValue().substring(property.getDefaultValue().indexOf("/") + 1, property.getDefaultValue().length()));
		}

		property = propertyDao.merge(property);

		List<Property> propertyList = findProperties(property.getModel());
		// 处理业务主键
		if (property.getIsBussinessKey()) {
			if (propertyList != null && propertyList.size() > 1) {
				for (Property p : propertyList) {
					if ((p.getIsBussinessKey() != null && !p.getIsBussinessKey()) || p.getCode().equals(property.getCode())) {
						continue;
					} else {
						p.setIsBussinessKey(false);
						propertyDao.merge(p);
						break;
					}
				}
			}
		} else {
			boolean flag = false;
			for (Property p : propertyList) {
				if (p.getIsBussinessKey() != null && p.getIsBussinessKey()) {
					flag = true;
					break;
				}
			}
			if (!flag && propertyList.size() > 0) {
				for (Property prop : propertyList) {
					if (model.getIsMain() && model.getEntity().getWorkflowEnabled() && !model.getEntity().getIsBase()) {
						if ("tableNo".equals(prop.getName())) {
							prop.setIsBussinessKey(true);
							propertyDao.merge(prop);
							break;
						}
					} else {
						if (prop.getIsPk()) {
							prop.setIsBussinessKey(true);
							propertyDao.merge(prop);
							break;
						}
					}
				}
			}
		}

		// 主显示字段处理
		if (property.getIsMainDisplay()) {
			if (propertyList != null && propertyList.size() > 1) {
				for (Property item : propertyList) {
					if ((item.getIsMainDisplay() != null && !item.getIsMainDisplay()) || item.getCode().equals(property.getCode())) {
						continue;
					} else {
						item.setIsMainDisplay(false);
						propertyDao.merge(item);
						break;
					}
				}
			}
		} else {
			boolean flag = false;
			for (Property item : propertyList) {
				if (item.getIsMainDisplay()) {
					flag = true;
					break;
				}
			}
			if (!flag && propertyList.size() > 0) {
				//没有设置主显示字段时默认为主键
				for (Property prop : propertyList) {
					if (prop.getIsPk()) {
						prop.setIsMainDisplay(true);
						propertyDao.merge(prop);
						break;
					}
				}
			}
		}

		// 主关联字段处理
		if (property.getType() == DbColumnType.OBJECT && property.getIsMainAssociated()) {
			if (propertyList != null && propertyList.size() > 1) {
				for (Property item : propertyList) {
					if (item.getType() == DbColumnType.OBJECT
							&& ((item.getIsMainAssociated() != null && !item.getIsMainAssociated()) || item.getCode().equals(property.getCode()))) {
						continue;
					} else {
						item.setIsMainAssociated(false);
						propertyDao.merge(item);
						break;
					}
				}
			}
		}
		boolean oldIsMneCode = model.getIsMneCode();
		// 是否助记码处理
		if (property.getIsUsedMneCode()) {
			model.setIsMneCode(true);
			modelDao.save(model);
		} else {
			boolean flag = false;
			for (Property item : propertyList) {
				if (item.getIsUsedMneCode()) {
					flag = true;
					modelDao.save(model);
					break;
				}
			}
			if (flag && propertyList.size() > 0) {
				model.setIsMneCode(true);
			} else {
				model.setIsMneCode(false);
			}
			modelDao.save(model);
		}
		boolean isMneCode = model.getIsMneCode();
		if(oldIsMneCode != isMneCode){
			FileUtils.updateMneCodeTable(model);
			try {
				String tableName = model.getTableName().toUpperCase()+ "_MC";
				Boolean tableIsExist = FieldSyncDBUtils.tableIsExist(tableName, template);
				if(!tableIsExist){
					ModelSyncDBUtils.createMneCodeTableOfModel(template, model, dbName);
				}
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		}
		property.setOrgColumnName(orgColumnName);
		propertyDao.flush();
		propertyDao.clear();
		if (DbColumnType.BAPCODE == property.getType()) {
			try {
				propertyService.addCounterRule(property);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		try {
			String dbName = getDbName();
			FieldSyncDBUtils.fieldSyncToDb(property, model, isNew, template, dbName);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
	}
	public boolean tableIsExist(String tableName, JdbcTemplate template) {
		boolean retBool = false;
		String tarSql = "";
		String dbName = getDbName();
		if (dbName.startsWith("sqlserver")) {
			tarSql = "select count(1) from sys.tables where name='" + tableName + "'";
		} else if (dbName.startsWith("oracle")) {
			tarSql = "select count(1) from user_tables where table_name='" + tableName + "'";
		}else if (dbName.startsWith("mysql") || dbName.startsWith("mariadb")) {
			tarSql = "select count(1) from information_schema.tables t where t.TABLE_SCHEMA='"+ DbUtils.getCurrentDBName() +"' and table_name ='" + tableName + "'";
		}
		if (!"".equals(tarSql)) {
			int retInt = template.queryForObject(tarSql, Integer.class);
			retBool = ((1 == retInt) ? true : false);
		}
		return retBool;
	}

	public String getDbName() {
		if (null == dbName) {
			Connection conn = null;
			try {
				conn = template.getDataSource().getConnection();
				dbName = conn.getMetaData().getDatabaseProductName().toLowerCase();
				if (dbName.startsWith("oracle"))
					dbName = "oracle";
				if (dbName.startsWith("mysql"))
					dbName = "mysql";
				if (dbName.startsWith("microsoft sql server"))
					dbName = "sqlserver";
				if (dbName.startsWith("mariadb"))
					dbName = "mariadb";
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
			} finally {
				if (null != conn) {
					try {
						conn.close();
					} catch (SQLException e) {
						log.error(e.getMessage(), e);
					}
				}
			}
		}
		return dbName;
	}

	/**
	 * 检查对象型字段code的唯一性 for fix
	 * @param property
	 */
	public boolean checkObjPropertyCode(Property property){
		String hql = "select p.code from Property as p, Property p2 where p.valid = true and p.associatedProperty.model = p2.model "
				+ " and p.model != ?0 and p.name = ?1 and p.entityCode = ?2 and p2 = ?3 ";
		return propertyDao.createQuery(hql,property.getModel(),property.getName(),property.getEntityCode(),property.getAssociatedProperty()).list().size() > 0;
	}

	/**
	 * 获取主列表视图
	 *
	 * @param views
	 */
	private View getMainListView(List<View> views) {
		Iterator<View> it = views.iterator();
		View view = null;
		while (it.hasNext()) {
			View v = it.next();
			if (v.getUsedForWorkFlow() != null && v.getUsedForWorkFlow()) {
				view = v;
				break;
			} else if (v.getType() != ViewType.LIST || v.getShowType().equals(ShowType.LAYOUT) || v.getShowType().equals(ShowType.PART)) {
				it.remove();
			}
		}
		if (null == view && !views.isEmpty()) {
			return views.get(0);
		}
		return view;
	}

	/**
	 * 判断同一实体下模型名称唯一
	 * @param property
	 * @return
	 */
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	private boolean checkPropertyNameUnique(Property property) {

		List<Object> parameters = new LinkedList<Object>();
		String hql = "";
		Model model = getModel(property.getModel().getCode());
		if (property.getCode() == null || property.getCode().length() == 0) {
			hql = "select count(p.code) as totalCoual from Property as p where p.model=? and  p.name = ?";
			parameters.add(model);
			parameters.add(property.getName());
		} else {
			hql = "select count(p.code) as totalCoual from Property as p where p.model=? and p.name = ? and p.code !=?";
			parameters.add(model);
			parameters.add(property.getName());
			parameters.add(property.getCode());
		}
		Object[] params = new Object[parameters.size()];
		List<Long> modelCount = modelDao.findByHql(hql, parameters.toArray(params));
		if (modelCount.get(0) == 0) {
			return true;
		}
		return false;
	}

	/*
	 * 判断同一实体下模型字段名称唯一
	 */
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	private boolean checkColumnNameUnique(Property property) {

		List<Object> parameters = new LinkedList<Object>();
		String hql = "";
		Model model = getModel(property.getModel().getCode());
		if (property.getCode() == null || property.getCode().length() == 0) {
			hql = "select count(p.code) as totalCoual from Property as p where p.model=? and  p.columnName = ?";
			parameters.add(model);
			parameters.add(property.getColumnName());
		} else {
			hql = "select count(p.code) as totalCoual from Property as p where p.model=? and p.columnName = ? and p.code !=?";
			parameters.add(model);
			parameters.add(property.getColumnName());
			parameters.add(property.getCode());
		}
		Object[] params = new Object[parameters.size()];
		List<Long> modelCount = modelDao.findByHql(hql, parameters.toArray(params));
		if (modelCount.get(0) == 0) {
			return true;
		}
		return false;
	}

	/*
	 * 判断同一实体下模型名称唯一
	 */
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	private boolean checkPropertyTypeUnique(Property property) {
		List<Object> parameters = new LinkedList<Object>();
		String hql = "";
		Model model = getModel(property.getModel().getCode());
		if (property.getCode() == null || property.getCode().length() == 0) {
			hql = "select count(p.code) as totalCoual from Property as p where p.valid=true and p.model=? and  p.type = ?";
			parameters.add(model);
			parameters.add(property.getType());
		} else {
			hql = "select count(p.code) as totalCoual from Property as p where p.valid=true and p.model=? and p.type = ? and p.code !=?";
			parameters.add(model);
			parameters.add(property.getType());
			parameters.add(property.getCode());
		}
		Object[] params = new Object[parameters.size()];
		List<Long> modelCount = modelDao.findByHql(hql, parameters.toArray(params));
		if (modelCount.get(0) == 0) {
			return true;
		}
		return false;
	}

	/*
	 * 判断关键字冲突
	 *
	 * @see com.supcon.orchid.entityconf.services.ModelService#deleteProperty(com .supcon.orchid.ec.entities.Property)
	 */
	private boolean checkPropertyNamekey(Property property) {
		String name = property.getName();// java关键字
		String columnName = property.getColumnName();// 数据库
		if (!propertyKeyService.checkPropertyKey(name)) {
			log.error("不允许使用关键字：" + name);
			return false;
		}
		// for (String item : javaKeyWords) {
		// if (name.equalsIgnoreCase(item)) {
		// return false;
		// }
		// }
		if (!propertyKeyService.checkDBKey(columnName) || !propertyKeyService.checkDBKey(name)) {
			log.error("不允许使用关键字：" + name);
			return false;
		}
		// for (String item : dbKeyWords) {
		// if (columnName.equalsIgnoreCase(item) || name.equalsIgnoreCase(item)) {
		// return false;
		// }
		// }
		return true;
	}

	private boolean checkModelNamekey(Model model) {
		String name = model.getModelName();// java关键字
		if (!propertyKeyService.checkJavaKey(name) || !propertyKeyService.checkDBKey(name) || !propertyKeyService.checkBapKey(name)) {
			log.error("不允许使用关键字：" + name);
			return false;
		}
		return true;
	}

	@Override
	public String deleteProperty(Property property) {
		Set<String> descSet = new HashSet<String>();
		List<DataGrid> dataGrids = dataGridService.findDataGridsByProperty(property);
		if (null != dataGrids && !dataGrids.isEmpty()) {
			for(DataGrid dataGrid :dataGrids){
				try {
					descSet.add(dataGrid.getOrgProperty().getName()+"字段被DataGrid:"+dataGrid.getName()+"配置引用！");
				} catch (Exception e) {
					log.info("当前dataGrid异常"+dataGrid.getCode());
				}
			}
			if (null != descSet && !descSet.isEmpty()) {
				return JsonUtils.setToJson(descSet);
			}
			//throw new BAPException(BAPException.Code.ASS_BY_DATAGRID);
		}
		List<Field> fields = fieldService.getFieldByPropertyCode(property.getCode());
		if (null != fields && !fields.isEmpty()) {
			for (Field field : fields) {
				try {
					descSet.add( "被"+InternationalResource.get(field.getView().getEntity().getName())+"实体中的" + field.getView().getName() + "视图配置引用！");
				} catch (Exception e) {
					log.info("当前field异常"+field.getCode());
				}
				//descSet.add(field.getProperty().getName() + "字段被" + field.getView().getName() + "视图配置引用！");
			}
			if (null != descSet && !descSet.isEmpty()) {
				return JsonUtils.setToJson(descSet);
			}
			//throw new BAPException(BAPException.Code.USED_BY_VIEW);
		}
		property.setValid(false);
		property.setDeleteTime(new Date());
		propertyDao.update(property);
		if (DbColumnType.BAPCODE == property.getType()) {
			propertyService.deleteCounter(property);
		}
//		cache.remove(CACHE_MODEL_PROPERTY_PREFIX + property.getCode());
		//开始保存模块信息数据的最后修改时间
//		ModuleGenerateInfo generateInfo = moduleGenerateInfoService.getModuleGenerateInfoByModuleCode(property.getModuleCode());
//		if(generateInfo!=null){
//			generateInfo.setLastModifyTime(new Date());
//			moduleGenerateInfoService.save(generateInfo,Thread.currentThread());
//		}else{
//			generateInfo = new ModuleGenerateInfo();
//			generateInfo.setLastModifyTime(new Date());
//			generateInfo.setModuleCode(property.getModuleCode());
//			moduleGenerateInfoService.create(generateInfo,Thread.currentThread());
//		}
		return "";
	}

	public void deleteProperty(String propertyCode) {
		Property property = getProperty(propertyCode);
		deleteProperty(property);
	}

	@Override
	public String deletePropertyPhysical(String propertyCode, Boolean deleteType) {
		return this.deletePropertyPhysical(propertyCode, deleteType,true);
	}


	@Override
	public String deletePropertyPhysical(String propertyCode, Boolean deleteType,Boolean ignoreCheck) {
		Property property = getPropertyNoValid(propertyCode);
		Set<String> descSet = new HashSet<String>();
		if (!deleteType) {
			if (null == property) {
				throw new StaleObjectStateException(Property.class.getName(), propertyCode);
			}
		}
		if (null != property) {
			if(null == ignoreCheck || !ignoreCheck){
				if(property.getIsInherent()){
					descSet.add(property.getName()+"是固有字段不可删除!");
					return JsonUtils.setToJson(descSet);
				}
				try {
					Set<String> msgs = this.checkDeleteProperty(property);
					if(null != msgs && !msgs.isEmpty()){
						return JsonUtils.setToJson(msgs);
					}
				} catch (Exception e) {
					log.error(e.getMessage(),e);
					throw new EcException("校验失败，详细错误信息请查看日志");
				}
			}
			propertyDao.deletePhysical(propertyDao.load(propertyCode));
			propertyDao.flush();
//			cache.remove(CACHE_MODEL_PROPERTY_PREFIX + property.getCode());
			//开始保存模块信息数据的最后修改时间
//			ModuleGenerateInfo generateInfo = moduleGenerateInfoService.getModuleGenerateInfoByModuleCode(property.getModuleCode());
//			if(generateInfo!=null){
//				generateInfo.setLastModifyTime(new Date());
//				moduleGenerateInfoService.save(generateInfo,Thread.currentThread());
//			}else{
//				generateInfo = new ModuleGenerateInfo();
//				generateInfo.setLastModifyTime(new Date());
//				generateInfo.setModuleCode(property.getModuleCode());
//				moduleGenerateInfoService.create(generateInfo,Thread.currentThread());
//			}
		}
		return "";
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Property> findProperties(Model model) {
		// return propertyDao.findByHql("from Property where model = ? and valid = true", model);
		if (model != null && model.getCode() != null) {
			DetachedCriteria detachedCriteria = DetachedCriteria.forClass(Property.class);
			detachedCriteria.add(Restrictions.eq("model.code", model.getCode()));
			detachedCriteria.add(Restrictions.eq("valid", true));
			return propertyDao.findByCriteria(detachedCriteria.addOrder(Order.asc("sort")));
		}
		return null;
	}


	@Override
	public List<Property> findProperties(String propertiesNames, String modelCode) {
		String[] pnames = propertiesNames.split("\\.");
		List<Property> retList = new ArrayList<>();
		Property tmp = null;
		String tmpModelCode = modelCode;
		for (int i = 1; i < pnames.length; i++) {
			tmp = propertyDao.findEntityByCriteria(Restrictions.eq("valid", true), Restrictions.eq("name", pnames[i]),
					Restrictions.eq("model.code", tmpModelCode));
			if (tmp != null) {
				retList.add(tmp);
				if (tmp.getAssociatedProperty() != null) {
					tmpModelCode = tmp.getAssociatedProperty().getModel().getCode();
				}
			}
		}
		return retList;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Property findMainDisplayProperty(Model model) {
		List<Property> retList = propertyDao.findByHql("from Property where model = ? and valid = true and isMainDisplay = true", model);
		if (retList != null && retList.size() > 0) {
			return retList.get(0);
		} else {
			retList = findProperties(model);
			if (retList != null && retList.size() > 0) {
				return retList.get(0);
			}
		}
		return null;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Page<Property> findProperties(Page<Property> page, Model model, Boolean showInherent, Boolean showCustom) {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(Property.class);
		detachedCriteria.add(Restrictions.eq("model", model));
		detachedCriteria.add(Restrictions.eq("valid", true));
		if (null == showInherent || showInherent == false) {
			detachedCriteria.add(Restrictions.eq("isInherent", false));
		}
		if (showCustom == null || !showCustom) {
			detachedCriteria.add(Restrictions.or(Restrictions.eq("isCustom", false), Restrictions.isNull("isCustom")));
		}
		return propertyDao.findByPage(page, detachedCriteria.addOrder(Order.asc("sort")));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Property> findByUpdateProperties() {
		return propertyDao.findByHql("from Property where isInherent = true and type <> ? and valid = true", DbColumnType.OBJECT);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<AssociatedInfo> findAssociatedInfos(List<Model> models, int... associatedTypes) {
		List<Object> args = new ArrayList<Object>();
		String hql = "from Property p where p.valid = true and p.associatedProperty is not null and p.model in (:models)";
		List<AssociatedInfo> assos = new ArrayList<AssociatedInfo>();
		if (associatedTypes != null && associatedTypes.length > 0) {
			String subHql = "";
			for (int associatedType : associatedTypes) {
				if (subHql.length() > 0) {
					subHql += " or ";
				}
				subHql += " p.associatedType in(:associatedTypes) ";
				args.add(associatedType);
			}
			hql += " and (" + subHql + ")";
		}

		Query query = propertyDao.createQuery(hql);
		query.setParameterList("models", models);
		if (args != null && !args.isEmpty()) {
			query.setParameterList("associatedTypes", args);
		}
		@SuppressWarnings("unchecked")
		List<Property> properties = query.list();

		AssociatedInfo asso = null;
		for (Property p : properties) {
			asso = new AssociatedInfo();
			asso.setIsMainAssociated(p.getIsMainAssociated());
			asso.setOriginalProperty(p);
			asso.setTargetProperty(p.getAssociatedProperty());
			asso.setType(p.getAssociatedType());
			assos.add(asso);
		}
		return assos;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<AssociatedInfo> findInherentAssociatedInfos(int associatedType) {
		List<AssociatedInfo> assos = new ArrayList<AssociatedInfo>();
		AssociatedInfo asso = null;
		List<Property> properties = null;
		if (AssociatedInfo.ONE_TO_MANY == associatedType) {
			String hql = "from Property p where p.valid=?0 and p.model.inherentCommonFlag = ?1 and p.type=?2 and (p.name = ?3 or p.name = ?4)";
			properties = propertyDao.findByHql(hql, Boolean.TRUE, Boolean.TRUE, DbColumnType.OBJECT, "mainObj", "linkId");
			for (Property p : properties) {
				asso = new AssociatedInfo();
				asso.setOriginalProperty(p.getAssociatedProperty());
				asso.setTargetProperty(p);
				asso.setType(Property.ONE_TO_MANY);
				assos.add(asso);
			}
		} else {
			String hql = "from Property p where p.valid=?0 and p.model.inherentCommonFlag = ?1 and p.type=?2 and p.name != ?3 and p.name != ?4";
			properties = propertyDao.findByHql(hql, Boolean.TRUE, Boolean.TRUE, DbColumnType.OBJECT, "mainObj", "linkId");
			for (Property p : properties) {
				asso = new AssociatedInfo();
				asso.setOriginalProperty(p);
				asso.setTargetProperty(p.getAssociatedProperty());
				asso.setType(Property.MANY_TO_ONE);
				assos.add(asso);
			}
		}
		propertyDao.flush();
		propertyDao.clear();

		return assos;
	}


	/**
	 * 根据指定的properties来创建所需要的模型关联信息。
	 *
	 * 主要逻辑从 {@link #findAssociatedInfos(Model, int...)} 拷贝
	 * @param propertyMap
	 * @param model
	 * @param associatedTypes
	 * @return
	 */
	public static List<AssociatedInfo> createAssociatedInfosFromProperties(Map<String, Property> propertyMap, Model model, int... associatedTypes) {
		List<AssociatedInfo> assos = new ArrayList<AssociatedInfo>();
		String modelCode = model.getCode();
		for(Map.Entry<String, Property> entry : propertyMap.entrySet()) {
			Property p = entry.getValue();
			Property associatedProperty = p.getAssociatedProperty();
			Model propertyModel = p.getModel();
			if(p.isValid() && null != associatedProperty){
				Model associatedPropertyModel = associatedProperty.getModel();
				if ( (/*null != model && */modelCode.equals(propertyModel.getCode())) //如果这里抛出NPE，说明传入数据中并没有整理成完整的数据。
						|| (/*null != associatedPropertyModel && */modelCode.equals(associatedPropertyModel.getCode()))) {

					for(int associatedType : associatedTypes) {
						if(null != p.getAssociatedProperty() && p.getAssociatedType() == associatedType) {
							assos.add(createAssociatedInfo(model, p));
						}
						if (associatedType == AssociatedInfo.ONE_TO_MANY && p.getAssociatedType() == AssociatedInfo.MANY_TO_ONE) { // 添加反向关联的信息
							assos.add(createAssociatedInfo(model, p));
						} else if (associatedType == AssociatedInfo.MANY_TO_ONE && p.getAssociatedType() == AssociatedInfo.ONE_TO_MANY) {// 添加反向关联的信息
							assos.add(createAssociatedInfo(model, p));
						}
					}
				}
			}
		}
		return assos;
	}

	/**
	 * 根据指定的properties来创建所需要的模型关联信息。
	 *
	 * 主要逻辑从 {@link #findAssociatedInfos(Model, int...)} 拷贝
	 *
	 * @param propertyMap
	 * @param dataGrid
	 * @param associatedTypes
	 * @return
	 */
	public static List<AssociatedInfo> createAssociatedInfosFromProperties(Map<String, Property> propertyMap, DataGrid dataGrid, int... associatedTypes) {
		List<AssociatedInfo> assos = new ArrayList<AssociatedInfo>();
		Model model = dataGrid.getTargetModel();
		String modelCode = model.getCode();
		for(Map.Entry<String, Property> entry : propertyMap.entrySet()) {
			Property p = entry.getValue();
			Property associatedProperty = p.getAssociatedProperty();
			Model propertyModel = p.getModel();
			if(p.isValid() && null != associatedProperty){
				Model associatedPropertyModel = associatedProperty.getModel();
				if ( (/*null != model && */modelCode.equals(propertyModel.getCode())) //如果这里抛出NPE，说明传入数据中并没有整理成完整的数据。
						|| (modelCode.equals(associatedPropertyModel.getCode()) && dataGrid.getCode().startsWith(modelCode))) {

					for(int associatedType : associatedTypes) {
						if(null != p.getAssociatedProperty() && p.getAssociatedType() == associatedType) {
							assos.add(createAssociatedInfo(model, p));
						}
						if (associatedType == AssociatedInfo.ONE_TO_MANY && p.getAssociatedType() == AssociatedInfo.MANY_TO_ONE) { // 添加反向关联的信息
							assos.add(createAssociatedInfo(model, p));
						} else if (associatedType == AssociatedInfo.MANY_TO_ONE && p.getAssociatedType() == AssociatedInfo.ONE_TO_MANY) {// 添加反向关联的信息
							assos.add(createAssociatedInfo(model, p));
						}
					}
				}
			}
		}
		return assos;
	}

	/*
	 * (non-Javadoc)
	 * @see com.supcon.orchid.entityconf.services.ModelService#findAssociatedInfos(com.supcon.supfusion.configuration.services.pojo.Model, int[])
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<AssociatedInfo> findAssociatedInfos(Model model, int... associatedTypes) {
		List<Object> args = new ArrayList<Object>();
		String hql = "from Property p where p.valid=true and ((p.associatedProperty is not null and p.model = ? and p.model.valid=true) or (p.associatedProperty is not null and p.associatedProperty.model = ? and p.associatedProperty.model.valid=true))";
		args.add(model);
		args.add(model);
		if (associatedTypes != null && associatedTypes.length > 0) {
			String subHql = "";
			for (int associatedType : associatedTypes) {
				if (subHql.length() > 0) {
					subHql += " or ";
				}
				subHql += " p.associatedType=? ";
				args.add(associatedType);
				if (associatedType == AssociatedInfo.ONE_TO_MANY) {
					subHql += " or p.associatedType=? ";
					args.add(AssociatedInfo.MANY_TO_ONE);
				} else if (associatedType == AssociatedInfo.MANY_TO_ONE) {
					subHql += " or p.associatedType=? ";
					args.add(AssociatedInfo.ONE_TO_MANY);
				}
			}
			hql += " and (" + subHql + ")";
		}

		List<Property> properties = propertyDao.findByHql(hql, args.toArray());
		List<AssociatedInfo> assos = new ArrayList<AssociatedInfo>();
		for (Property p : properties) {
			assos.add(createAssociatedInfo(model, p));
		}
		return assos;
	}

	/**
	 *
	 * @param model
	 * @param p
	 * @return
	 */
	private static AssociatedInfo createAssociatedInfo(Model model, Property p) {
		AssociatedInfo asso = new AssociatedInfo();
		asso.setIsMainAssociated(p.getIsMainAssociated());
		if (model.getCode().equals(p.getModel().getCode()) || p.getModel().getCode().equals(p.getAssociatedProperty().getModel().getCode())) {
			asso.setOriginalProperty(p);
			asso.setTargetProperty(p.getAssociatedProperty());
			asso.setType(p.getAssociatedType());
		} else {
			asso.setTargetProperty(p);
			asso.setOriginalProperty(p.getAssociatedProperty());
			if (p.getAssociatedType() == Property.ONE_TO_MANY) {
				asso.setType(Property.MANY_TO_ONE);
			} else if (p.getAssociatedType() == Property.MANY_TO_ONE) {
				asso.setType(Property.ONE_TO_MANY);
			} else {
				asso.setType(p.getAssociatedType());
			}
		}
		asso.setCode(asso.getOriginalProperty().getCode()+"->"+asso.getTargetProperty().getCode()); // 用于后面的排序
		return asso;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<AssociatedInfo> findAssociatedInfoNotIncludeBackAsso(Model model, int... associatedTypes) {
		List<Object> args = new ArrayList<Object>();
		String hql = "from Property p where p.valid=true and p.associatedProperty is not null and p.model = ?0";
		args.add(model);
		if (associatedTypes != null && associatedTypes.length > 0) {
			String subHql = "";
			for (int i=0; i<associatedTypes.length; i++) {
				if (subHql.length() > 0) {
					subHql += " or ";
				}
				subHql += " p.associatedType=?" + (i+1) + " ";
				args.add(associatedTypes[i]);
			}
			hql += " and (" + subHql + ")";
		}

		List<Property> properties = propertyDao.findByHql(hql, args.toArray());
		List<AssociatedInfo> assos = new ArrayList<AssociatedInfo>();
		AssociatedInfo asso = null;
		for (Property p : properties) {
			asso = new AssociatedInfo();
			asso.setIsMainAssociated(p.getIsMainAssociated());
			asso.setOriginalProperty(p);
			asso.setTargetProperty(p.getAssociatedProperty());
			asso.setType(p.getAssociatedType());
			assos.add(asso);
		}
		return assos;
	}

	@Override
	public Set<Model> findRelationModels(String modelCode, int... associatedTypes) {
		Set<Model> models = new HashSet<Model>();
		String hql = "from Property p where p.valid = true and p.associatedProperty.model.code = ? and p.associatedProperty.valid = true and p.model.code <> ?";
		List<Object> assoTypes = new ArrayList<Object>();
		if (associatedTypes != null && associatedTypes.length > 0) {
			hql += " and p.associatedType in (:assoTypes)";
			for (int t : associatedTypes) {
				assoTypes.add(t);
			}
		}
		Query query = propertyDao.createQuery(hql, new Object[] { modelCode, modelCode });
		if (assoTypes.size() > 0) {
			query.setParameterList("assoTypes", assoTypes);
		}
		List<Property> props = query.list();
		if (props != null && props.size() > 0) {
			for (Property p : props) {
				models.add(p.getModel());
			}
		}
		return models;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<AssociatedInfo> findAssociatedInfos(Property property) {
		List<Property> properties = propertyDao.findByHql("from Property p where p.valid=true and p.associatedProperty is not null and p.code = ?0",
				property.getCode());
		List<AssociatedInfo> assos = new ArrayList<AssociatedInfo>();
		AssociatedInfo asso = null;
		for (Property p : properties) {
			asso = new AssociatedInfo();
			asso.setIsMainAssociated(p.getIsMainAssociated());
			asso.setOriginalProperty(p);
			asso.setTargetProperty(p.getAssociatedProperty());
			asso.setType(p.getAssociatedType());
			assos.add(asso);
		}
		return assos;
	}

	@Override
	public void createInherentProperties(Model model) {
		// List<Property> properties = new ArrayList<Property>(6);
		// String[] names = new String[] { "CREATE_STAFF_ID", "MODIFY_STAFF_ID",
		// "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME" };
		if (null == model.getName()) {
			model = getModel(model.getCode());
		}

		String[] names = new String[] {};
		String[] propertyNames = new String[] {};
		Property[] propertyAssProperty = new Property[] {};
		DbColumnType[] propertyType = new DbColumnType[] {};
		FieldType[] propertyShowType = new FieldType[] {};
		ShowFormat[] propertyShowFormat = new ShowFormat[] {};
		Integer[] assType = new Integer[] {};
		if (model.getIsMain() && model.getEntity().getWorkflowEnabled()) {
			if (model.getDataType() == 1) {
				names = new String[] { "ec.common.ID", "ec.common.TABLE_INFO_ID", "ec.common.department", "ec.common.position", "ec.common.group", "ec.common.creator",
						"ec.common.modifier", "ec.common.deleter", "ec.common.effector", "ec.common.createTime", "ec.common.modifyTime",
						"ec.common.deleteTime", "ec.common.effectTime", "ec.common.status", "ec.common.extraCol", "ec.common.tableNo", "ec.common.positionLayRec",
						"ec.common.ownerDepartment", "ec.common.ownerPosition", "ec.common.ownerStaff", "ec.common.version", "ec.common.effectiveState", "ec.common.sort" };
				// 21
				propertyNames = new String[] { "id", "tableInfoId", "createDepartment", "createPosition", "groupId", "createStaff", "modifyStaff",
						"deleteStaff", "effectStaff", "createTime", "modifyTime", "deleteTime", "effectTime", "status", "extraCol", "tableNo",
						"positionLayRec", "ownerDepartment", "ownerPosition", "ownerStaff", "version", "effectiveState","sort" };
				propertyType = new DbColumnType[] { DbColumnType.LONG, DbColumnType.LONG, DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.LONG,
						DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.DATETIME, DbColumnType.DATETIME,
						DbColumnType.DATETIME, DbColumnType.DATETIME, DbColumnType.INTEGER, DbColumnType.LONGTEXT, DbColumnType.TEXT, DbColumnType.TEXT,
						DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.INTEGER, DbColumnType.INTEGER, DbColumnType.LONG };
				propertyShowType = new FieldType[] { FieldType.TEXTFIELD, FieldType.TEXTFIELD, FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.TEXTFIELD,
						FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.DATETIME, FieldType.DATETIME,
						FieldType.DATETIME, FieldType.DATETIME, FieldType.TEXTFIELD, FieldType.TEXTAREA, FieldType.TEXTFIELD, FieldType.TEXTFIELD,
						FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.TEXTFIELD, FieldType.TEXTFIELD, FieldType.TEXTFIELD };
				propertyShowFormat = new ShowFormat[] { ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP, ShowFormat.TEXT,
						ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP, ShowFormat.YMD_HMS, ShowFormat.YMD_HMS,
						ShowFormat.YMD_HMS, ShowFormat.YMD_HMS, ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.SELECTCOMP,
						ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP, ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT };
				assType = new Integer[] { null, null, 1, 1, null, 1, 1, 1, 1, null, null, null, null, null, null, null, null, 1, 1, 1, null, null ,null};
				propertyAssProperty = new Property[] { null, null, getProperty("base_department_id"), getProperty("base_position_id"), null,
						getProperty("base_staff_id"), getProperty("base_staff_id"), getProperty("base_staff_id"), getProperty("base_staff_id"), null, null,
						null, null, null, null, null, null, getProperty("base_department_id"), getProperty("base_position_id"), getProperty("base_staff_id"),
						null, null ,null };

			} else if (model.getDataType() == 2) {
				// 21
				names = new String[] { "ec.common.ID", "ec.common.TABLE_INFO_ID", "ec.common.department", "ec.common.position", "ec.common.group", "ec.common.creator",
						"ec.common.modifier", "ec.common.deleter", "ec.common.effector", "ec.common.createTime", "ec.common.modifyTime",
						"ec.common.deleteTime", "ec.common.effectTime", "ec.common.status", "ec.common.extraCol", "ec.common.tableNo", "ec.common.positionLayRec",
						"ec.common.ownerDepartment", "ec.common.ownerPosition", "ec.common.ownerStaff", "ec.common.version", "ec.common.layno",
						"ec.common.layrec", "ec.common.fullPathName", "ec.common.effectiveState", "ec.common.sort"};
				// 21
				propertyNames = new String[] { "id", "tableInfoId", "createDepartment", "createPosition", "groupId", "createStaff", "modifyStaff",
						"deleteStaff", "effectStaff", "createTime", "modifyTime", "deleteTime", "effectTime", "status", "extraCol", "tableNo",
						"positionLayRec", "ownerDepartment", "ownerPosition", "ownerStaff", "version", "layNo", "layRec", "fullPathName", "effectiveState","sort" };
				propertyType = new DbColumnType[] { DbColumnType.LONG, DbColumnType.LONG, DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.LONG,
						DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.DATETIME, DbColumnType.DATETIME,
						DbColumnType.DATETIME, DbColumnType.DATETIME, DbColumnType.INTEGER, DbColumnType.LONGTEXT, DbColumnType.TEXT, DbColumnType.TEXT,
						DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.INTEGER, DbColumnType.INTEGER, DbColumnType.TEXT,
						DbColumnType.TEXT, DbColumnType.INTEGER, DbColumnType.LONG };
				propertyShowType = new FieldType[] { FieldType.TEXTFIELD, FieldType.TEXTFIELD, FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.TEXTFIELD,
						FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.DATETIME, FieldType.DATETIME,
						FieldType.DATETIME, FieldType.DATETIME, FieldType.TEXTFIELD, FieldType.TEXTAREA, FieldType.TEXTFIELD, FieldType.TEXTFIELD,
						FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.TEXTFIELD, FieldType.TEXTFIELD, FieldType.TEXTFIELD,
						FieldType.TEXTFIELD, FieldType.TEXTFIELD, FieldType.TEXTFIELD };
				propertyShowFormat = new ShowFormat[] { ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP, ShowFormat.TEXT,
						ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP, ShowFormat.YMD_HMS, ShowFormat.YMD_HMS,
						ShowFormat.YMD_HMS, ShowFormat.YMD_HMS, ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.SELECTCOMP,
						ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP, ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT };
				assType = new Integer[] { null, null, 1, 1, null, 1, 1, 1, 1, null, null, null, null, null, null, null, null, 1, 1, 1, null, null, null, null,
						null, null };
				propertyAssProperty = new Property[] { null, null, getProperty("base_department_id"), getProperty("base_position_id"), null,
						getProperty("base_staff_id"), getProperty("base_staff_id"), getProperty("base_staff_id"), getProperty("base_staff_id"), null, null,
						null, null, null, null, null, null, getProperty("base_department_id"), getProperty("base_position_id"), getProperty("base_staff_id"),
						null, null, null, null, null, null };

			}
		}
		if (model.getIsMain() && model.getEntity().getIsBase()) {
			if (model.getDataType() == 1) {
				names = new String[] { "ec.common.ID", "ec.common.creator", "ec.common.modifier", "ec.common.deleter", "ec.common.effector", "ec.common.createTime",
						"ec.common.modifyTime", "ec.common.deleteTime", "ec.common.effectTime", "ec.common.status", "ec.common.extraCol",
						"ec.common.ownerDepartment", "ec.common.ownerPosition", "ec.common.ownerStaff", "ec.common.version", "ec.property.valid", "ec.common.sort" };
				propertyNames = new String[] { "id", "createStaff", "modifyStaff", "deleteStaff", "effectStaff", "createTime", "modifyTime", "deleteTime",
						"effectTime", "status", "extraCol", "ownerDepartment", "ownerPosition", "ownerStaff", "version", "valid", "sort" };
				propertyType = new DbColumnType[] { DbColumnType.LONG, DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.OBJECT,
						DbColumnType.DATETIME, DbColumnType.DATETIME, DbColumnType.DATETIME, DbColumnType.DATETIME, DbColumnType.INTEGER,
						DbColumnType.LONGTEXT, DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.INTEGER, DbColumnType.BOOLEAN, DbColumnType.LONG};
				propertyShowType = new FieldType[] { FieldType.TEXTFIELD, FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.SELECTCOMP,
						FieldType.SELECTCOMP, FieldType.DATETIME, FieldType.DATETIME, FieldType.DATETIME, FieldType.DATETIME, FieldType.TEXTFIELD,
						FieldType.TEXTAREA, FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.TEXTFIELD, FieldType.SELECT, FieldType.TEXTFIELD };
				propertyShowFormat = new ShowFormat[] { ShowFormat.TEXT, ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP,
						ShowFormat.SELECTCOMP, ShowFormat.YMD_HMS, ShowFormat.YMD_HMS, ShowFormat.YMD_HMS, ShowFormat.YMD_HMS, ShowFormat.TEXT,
						ShowFormat.TEXT, ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP, ShowFormat.TEXT, ShowFormat.SELECT, ShowFormat.TEXT};
				assType = new Integer[] { null, 1, 1, 1, 1, null, null, null, null, null, null, 1, 1, 1, null, null, null };
				propertyAssProperty = new Property[] { null, getProperty("base_staff_id"), getProperty("base_staff_id"), getProperty("base_staff_id"),
						getProperty("base_staff_id"), null, null, null, null, null, null, getProperty("base_department_id"), getProperty("base_position_id"),
						getProperty("base_staff_id"), null, null, null };
			} else if (model.getDataType() == 2) {// 树结构增加两个字段no，rec
				names = new String[] { "ec.common.ID", "ec.common.creator", "ec.common.modifier", "ec.common.deleter", "ec.common.effector", "ec.common.createTime",
						"ec.common.modifyTime", "ec.common.deleteTime", "ec.common.effectTime", "ec.common.status", "ec.common.extraCol",
						"ec.common.ownerDepartment", "ec.common.ownerPosition", "ec.common.ownerStaff", "ec.common.version", "ec.common.layno",
						"ec.common.layrec", "ec.common.fullPathName", "ec.property.valid", "ec.common.sort" };
				propertyNames = new String[] { "id", "createStaff", "modifyStaff", "deleteStaff", "effectStaff", "createTime", "modifyTime", "deleteTime",
						"effectTime", "status", "extraCol", "ownerDepartment", "ownerPosition", "ownerStaff", "version", "layNo", "layRec", "fullPathName", "valid", "sort" };
				propertyType = new DbColumnType[] { DbColumnType.LONG, DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.OBJECT,
						DbColumnType.DATETIME, DbColumnType.DATETIME, DbColumnType.DATETIME, DbColumnType.DATETIME, DbColumnType.INTEGER,
						DbColumnType.LONGTEXT, DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.OBJECT, DbColumnType.INTEGER, DbColumnType.INTEGER,
						DbColumnType.TEXT, DbColumnType.TEXT , DbColumnType.BOOLEAN, DbColumnType.LONG};
				propertyShowType = new FieldType[] { FieldType.TEXTFIELD, FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.SELECTCOMP,
						FieldType.SELECTCOMP, FieldType.DATETIME, FieldType.DATETIME, FieldType.DATETIME, FieldType.DATETIME, FieldType.TEXTFIELD,
						FieldType.TEXTAREA, FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.SELECTCOMP, FieldType.TEXTFIELD, FieldType.TEXTFIELD,
						FieldType.TEXTFIELD, FieldType.TEXTFIELD, FieldType.SELECT , FieldType.TEXTFIELD};
				propertyShowFormat = new ShowFormat[] { ShowFormat.TEXT, ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP,
						ShowFormat.SELECTCOMP, ShowFormat.YMD_HMS, ShowFormat.YMD_HMS, ShowFormat.YMD_HMS, ShowFormat.YMD_HMS, ShowFormat.TEXT,
						ShowFormat.TEXT, ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP, ShowFormat.SELECTCOMP, ShowFormat.TEXT, ShowFormat.TEXT,
						ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.SELECT, ShowFormat.TEXT };
				assType = new Integer[] { null, 1, 1, 1, 1, null, null, null, null, null, null, 1, 1, 1, null, null, null, null, null, null };
				propertyAssProperty = new Property[] { null, getProperty("base_staff_id"), getProperty("base_staff_id"), getProperty("base_staff_id"),
						getProperty("base_staff_id"), null, null, null, null, null, null, getProperty("base_department_id"), getProperty("base_position_id"),
						getProperty("base_staff_id"), null, null, null, null, null, null };
			}

		}
		if (!model.getIsMain()) {
			if (model.getEntity().getWorkflowEnabled() != null && model.getEntity().getWorkflowEnabled()) {
				if (model.getDataType() == 1) {
					names = new String[] { "ec.common.ID", "ec.common.TABLE_INFO_ID", "ec.common.version", "ec.common.sort" };
					propertyNames = new String[] { "id", "tableInfoId", "version", "sort" };
					propertyType = new DbColumnType[] { DbColumnType.LONG, DbColumnType.LONG, DbColumnType.INTEGER, DbColumnType.LONG };
					propertyShowType = new FieldType[] { FieldType.TEXTFIELD, FieldType.TEXTFIELD, FieldType.TEXTFIELD, FieldType.TEXTFIELD };
					propertyShowFormat = new ShowFormat[] { ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT };
				} else if (model.getDataType() == 2) {
					names = new String[] { "ec.common.ID", "ec.common.TABLE_INFO_ID", "ec.common.version", "ec.common.layno", "ec.common.layrec", "ec.common.fullPathName", "ec.common.sort" };
					propertyNames = new String[] { "id", "tableInfoId", "version", "layNo", "layRec", "fullPathName", "sort" };
					propertyType = new DbColumnType[] { DbColumnType.LONG, DbColumnType.LONG, DbColumnType.INTEGER,  DbColumnType.INTEGER, DbColumnType.TEXT,
							DbColumnType.TEXT, DbColumnType.LONG };
					propertyShowType = new FieldType[] { FieldType.TEXTFIELD, FieldType.TEXTFIELD, FieldType.TEXTFIELD,  FieldType.TEXTFIELD, FieldType.TEXTFIELD,
							FieldType.TEXTFIELD, FieldType.TEXTFIELD };
					propertyShowFormat = new ShowFormat[] { ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT,  ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT };
				}
			} else {
				if (model.getDataType() == 1) {
					names = new String[] { "ec.common.ID", "ec.common.version","ec.common.sort" };
					propertyNames = new String[] { "id", "version", "sort" };
					propertyType = new DbColumnType[] { DbColumnType.LONG, DbColumnType.INTEGER, DbColumnType.LONG };
					propertyShowType = new FieldType[] { FieldType.TEXTFIELD, FieldType.TEXTFIELD, FieldType.TEXTFIELD };
					propertyShowFormat = new ShowFormat[] { ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT};
				} else if (model.getDataType() == 2) {
					names = new String[] { "ec.common.ID", "ec.common.version", "ec.common.layno", "ec.common.layrec", "ec.common.fullPathName", "ec.common.sort" };
					propertyNames = new String[] { "id", "version", "layNo", "layRec", "fullPathName", "sort" };
					propertyType = new DbColumnType[] { DbColumnType.LONG, DbColumnType.INTEGER, DbColumnType.INTEGER, DbColumnType.TEXT,
							DbColumnType.TEXT, DbColumnType.LONG };
					propertyShowType = new FieldType[] { FieldType.TEXTFIELD, FieldType.TEXTFIELD, FieldType.TEXTFIELD, FieldType.TEXTFIELD,
							FieldType.TEXTFIELD, FieldType.TEXTFIELD };
					propertyShowFormat = new ShowFormat[] { ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT, ShowFormat.TEXT };
				}
			}
		}
		Property property;
		for (int i = 0; i < names.length; i++) {
			property = new Property();
			property.setModel(model);
			property.setName(propertyNames[i]);
			property.setCode(model.getCode() + "_" + propertyNames[i]);
			property.setDisplayName(names[i]);
			property.setValid(true);
			if("valid".equals(property.getName()) || "ownerStaff".equals(property.getName())){
				property.setIsIndex(true);
			} else {
				property.setIsIndex(false);
			}
			property.setIsInherent(true);
			property.setNullable(true);
			property.setIsUnique(false);
			property.setIsPk(false);
			property.setIsUsedForList(true);
			property.setType(propertyType[i]);
			if (model.getIsMain()) {
				property.setAssociatedType(assType[i]);

				property.setAssociatedProperty(propertyAssProperty[i]);
			}
			if (i == 0) {
				property.setIsPk(true);
				property.setNullable(false);
				property.setDescription("Primary Key");
			}
			property.setFormat(propertyShowFormat[i]);
			property.setFieldType(propertyShowType[i]);

			if ("tableNo".equals(propertyNames[i])) { // 单据编号
				property.setIsBussinessKey(true);
				property.setIsUnique(true);
				property.setIsIndex(true);
				property.setNullable(false);
			}
			property.setModuleCode(model.getModuleCode());
			property.setEntityCode(model.getEntity().getCode());
			// properties.add(property);
			if("layRec".equals(property.getName()) || "positionLayRec".equals(property.getName()) || "fullPathName".equals(property.getName())){
				property.setMaxLength(2000);
			}
			//固有字段（OA字段、版本、ID、所属组、岗位路径、表单ID）默认隐藏
			if("effectiveState".equals(property.getName()) || "extraCol".equals(property.getName()) || "version".equals(property.getName())
					|| "id".equals(property.getName()) || "groupId".equals(property.getName()) || "positionLayRec".equals(property.getName())
					|| "tableInfoId".equals(property.getName())){
				property.setIsHidden(true);
			}
			property.setColumnName(property.getColumnName());
			propertyDao.save(property);
		}
		// return properties;
	}


	/**
	 * get property has bussinessKey by model
	 * @param model
	 * @return
	 */
	@Override
	public Property getBussinessProperty(Model model) {
		if (model != null) {
			return getBussinessProperty(model.getCode());
		}
		return null;
	}

	/**
	 * get property has bussinessKey by modelCode
	 *
	 * @see Property
	 * @param modelCode
	 * @return
	 */
	@Override
	public Property getBussinessProperty(String modelCode) {
		List<Property> list = propertyDao.findByHql("from Property as p where p.isBussinessKey=true and p.model.code =?0", modelCode);
		if (list != null && !list.isEmpty()) {
			return list.get(0);
		} else {
			Property property = propertyDao.findEntityByHql("from Property as p where p.name=?0 and p.model.code =?1", "id", modelCode);
			if (property != null) {
				property.setIsBussinessKey(true);
				propertyDao.save(property);
				return property;
			}
			return null;
		}
	}

	@Override
	public Property getIdProperty(String modelCode) {
		//List<Property> list = propertyDao.findByHql("from Property as p where p.name = ? and p.model.code = ?", "id", modelCode);
		List<Property> list = propertyDao.findByCriteria(Restrictions.eq("name", "id"), Restrictions.eq("model.code", modelCode));
		if (list != null && !list.isEmpty()) {
			return list.get(0);
		}
		return null;
	}


	/**
	 * 得到主显示字段
	 *
	 * @see Property
	 * @param modelCode
	 * @return
	 */
	@Override
	public Property getMainDisplayProperty(String modelCode) {
		List<Property> list = propertyDao.findByHql("from Property as p where p.isMainDisplay=true and p.model.code =?0", modelCode);
		if (list != null && !list.isEmpty()) {
			return list.get(0);
		} else {
			return null;
		}
	}

	@Override
	public Boolean getCustomProptyNullAble(String propertyCode) {
		String hql="from  CustomPropertyModelMapping a  where a.property.code=?0";
		List<CustomPropertyModelMapping>  customProps=customPropertyModelMappingDao.findByHql(hql, new Object[]{propertyCode});
		if(null!=customProps&&customProps.size()>0) {
			CustomPropertyModelMapping cp=customProps.get(0);
			return cp.getNullable();
		}
		return true;
	}

	/**
	 * 得到业务主键字段
	 *
	 * @see Property
	 * @param modelCode
	 * @return
	 */
	@Override
	public Property getBussinessKeyProperty(String modelCode) {
		List<Property> list = propertyDao.findByHql("from Property as p where p.isBussinessKey=true and p.model.code =?0", modelCode);
		if (list != null && !list.isEmpty()) {
			return list.get(0);
		} else {
			return null;
		}
	}


	/**
	 * 得到主键字段
	 *
	 * @see Property
	 * @param modelCode
	 * @return
	 */
	@Override
	public Property getPKProperty(String modelCode) {
		List<Property> list = propertyDao.findByHql("from Property as p where p.isPk=true and p.model.code =?0", modelCode);
		if (list != null && !list.isEmpty()) {
			return list.get(0);
		} else {
			return null;
		}
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	private void backupProperty() {
		if (DbUtils.getDbName().equals("sqlserver")) {
			template.execute("SELECT * INTO EC_PROPERTY_bk" + DateUtil.getNoFormatDateString(null) + " from EC_PROPERTY");
		} else {
			template.execute("create table EC_PROPERTY_bk" + DateUtil.getNoFormatDateString(null) + " as select * from EC_PROPERTY");
		}
		// // 处理前备份涉及的表
		// SQLQuery query = propertyDao.createNativeQuery("create table EC_PROPERTY_bk" +
		// DateUtil.getNoFormatDateString(null)
		// + " as select * from EC_PROPERTY");
		// query.executeUpdate();
	}

	/**
	 * 处理EC_PROPERTY 添加显示格式与显示类型
	 * 处理后显示类型与显示格式都恢复为默认
	 */
	@Override
	@Transactional
	public void modifyPropertyFieldType(String moduleCode) throws Exception {
		// 处理前备份涉及的表
		backupProperty();

		List<Property> properties = null;
		if (moduleCode != null && !moduleCode.isEmpty()) {
			properties = propertyDao.findByCriteria(Restrictions.eq("valid", true), Restrictions.like("model.code", moduleCode + "_", MatchMode.START));
		} else {
			properties = propertyDao.findByCriteria(Restrictions.eq("valid", true));
		}
		if (properties != null && !properties.isEmpty()) {
			Iterator<Property> iterator = properties.iterator();
			while (iterator.hasNext()) {
				Property property = iterator.next();
				if (property.getType() != null && property.getFieldType() == null) {
					if (property.getType().equals(DbColumnType.TEXT)) {
						property.setFieldType(FieldType.TEXTFIELD);
						property.setFormat(ShowFormat.TEXT);
					} else if (property.getType().equals(DbColumnType.INTEGER)) {
						property.setFieldType(FieldType.TEXTFIELD);
						property.setFormat(ShowFormat.TEXT);
					} else if (property.getType().equals(DbColumnType.DECIMAL)) {
						property.setFieldType(FieldType.TEXTFIELD);
						property.setFormat(ShowFormat.TEXT);
					} else if (property.getType().equals(DbColumnType.DATE)) {
						property.setFieldType(FieldType.DATE);
						property.setFormat(ShowFormat.YMD);
					} else if (property.getType().equals(DbColumnType.DATETIME)) {
						property.setFieldType(FieldType.DATETIME);
						property.setFormat(ShowFormat.YMD_HMS);
					} else if (property.getType().equals(DbColumnType.BOOLEAN)) {
						property.setFieldType(FieldType.CHECKBOX);
						property.setFormat(ShowFormat.CHECKBOX);
					} else if (property.getType().equals(DbColumnType.LONG)) {
						property.setFieldType(FieldType.TEXTFIELD);
						property.setFormat(ShowFormat.TEXT);
					} else if (property.getType().equals(DbColumnType.LONGTEXT)) {
						property.setFieldType(FieldType.TEXTAREA);
						property.setFormat(ShowFormat.TEXT);
					} else if (property.getType().equals(DbColumnType.OBJECT)) {
						property.setFieldType(FieldType.SELECTCOMP);
						property.setFormat(ShowFormat.SELECTCOMP);
					} else if (property.getType().equals(DbColumnType.MONEY)) {
						property.setFieldType(FieldType.TEXTFIELD);
						property.setFormat(ShowFormat.THOUSAND);
					} else if (property.getType().equals(DbColumnType.PASSWORD)) {
						property.setFieldType(FieldType.PASSWORDFIELD);
						property.setFormat(ShowFormat.TEXT);
					} else if (property.getType().equals(DbColumnType.SYSTEMCODE)) {
						property.setFieldType(FieldType.SELECTCOMP);
						property.setFormat(ShowFormat.SELECTCOMP);
					} else if (property.getType().equals(DbColumnType.ENUMERATE)) {
						if (property.getFormat() == ShowFormat.CHECKBOX) {
							property.setFieldType(FieldType.CHECKBOX);
						} else if (property.getFormat() == ShowFormat.RADIO) {
							property.setFieldType(FieldType.RADIO);
						} else if (property.getFormat() == ShowFormat.SELECT) {
							property.setFieldType(FieldType.SELECT);
						} else {
							// property.setFormat(ShowFormat.CHECKBOX);
							property.setFieldType(FieldType.CHECKBOX);
						}
					}
				}
				propertyDao.update(property);
			}
		}

		modifyBusinessKey();
	}

	/**
	 * 处理业务主键 如模型中没有业务主键则设主键为业务主键
	 */
	@Override
	@Transactional
	public void modifyBusinessKey() throws Exception {
		List<Model> models = modelDao.loadAll();
		if (models != null && !models.isEmpty()) {
			Iterator<Model> iterator = models.iterator();
			while (iterator.hasNext()) {
				Model model = iterator.next();
				if (null == this.getBussinessProperty(model)) {
					for (Property property : model.getProperties()) {
						if (property.getIsPk()) {
							property.setIsBussinessKey(true);
							propertyDao.update(property);
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * 根据条件查询Model
	 *
	 * @param criterions
	 * @return
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Model> findModels(Criterion... criterions) {
		return modelDao.findByCriteria(criterions);
	}

	/**
	 * 根据条件查询Property
	 *
	 * @param criterions
	 * @return
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Property> findProperties(Criterion... criterions) {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(Property.class);
		for (Criterion criterion : criterions) {
			detachedCriteria.add(criterion);
		}
		return propertyDao.findByCriteria(detachedCriteria.addOrder(Order.asc("sort")).addOrder(Order.asc("id")));
	}

	/**
	 * @param modelDao
	 * the modelDao to set
	 */
	public void setModelDao(ModelDaoImpl modelDao) {
		this.modelDao = modelDao;
	}

	/**
	 * @param propertyDao
	 *            the propertyDao to set
	 */
	public void setPropertyDao(PropertyDaoImpl propertyDao) {
		this.propertyDao = propertyDao;
	}

	/**
	 * @param dataGridService
	 *            the dataGridService to set
	 */
	public void setDataGridService(DataGridService dataGridService) {
		this.dataGridService = dataGridService;
	}

	/**
	 * @param viewDao
	 *            the viewDao to set
	 */
	public void setViewDao(ViewDaoImpl viewDao) {
		this.viewDao = viewDao;
	}

	@Override
	public void modifyMainAsso() throws Exception {
		Set<String> dealed = new HashSet<>();
		List<Property> list = propertyDao.findByCriteria(Restrictions.isNotNull("associatedProperty"), Restrictions.isNull("isMainAssociated"));
		for (Property p : list) {
			if (!dealed.contains(p.getModel().getCode() + "||" + p.getAssociatedProperty().getCode())
					&& p.getAssociatedProperty().getEntityCode().equals(p.getEntityCode())) {
				p.setIsMainAssociated(true);
			} else {
				p.setIsMainAssociated(false);
			}
			propertyDao.save(p);
			dealed.add(p.getModel().getCode() + "||" + p.getAssociatedProperty().getCode());
			// saveProperty(p);
		}
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Property findPKProperty(String modelCode) {
		Property property = null;
		if (null != modelCode) {
			property = propertyDao.findEntityByHql("from Property where model.code = ? and valid = true and isPk = true", modelCode);
		}
		return property;
	}

	private JSONDeserializer<Map> deserializer = new JSONDeserializer();

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Boolean  checkWhetherIsTreeSystemCode(Property p)  {
		if(p!=null)  {
			String fillContent = p.getFillcontent();
			Map<String,Object> map = p.getFillcontentJson();
			if(map!=null)  {
				for (Map.Entry<String, Object> entry : map.entrySet()) {
					if(entry.getKey()!=null&& "fillContent".equals(entry.getKey()))  {
						if(entry.getValue()!=null)  {
							String systemEntityCode=entry.getValue().toString();
							if(systemEntityCode!=null&&systemEntityCode.length()>0)  {
								SystemEntity entity=propertyDao.findEntityByHql("from SystemEntity entity where entity.code = ? and entity.valid = true ", systemEntityCode);
								if(entity!=null&&entity.getType().equals(SystemDisplayType.tree.toString().toLowerCase()))  {
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * @author wuqi
	 * 字母顺序比较器
	 *
	 */
	private class LetterComparator implements Comparator<String> {

		@Override
		public int compare(String s1, String s2) {
			if (s1.length() == s2.length()) {
				for (int i = 0; i < s1.length(); i++) {
					char c1 = s1.charAt(i);
					char c2 = s2.charAt(i);
					if (c1 == c2) {
						if (i < s1.length() - 1) {
							continue;
						} else {
							return 0;
						}
					} else {
						return c1 > c2 ? -1 : 1;
					}
				}
			} else {
				return s1.length() > s2.length() ? -1 : 1;
			}
			return 0;
		}
	}

	@Override
	public void createCustomProps(String modelCode, Integer charParamAmount, Integer intParamAmount, Integer floatParamAmount, Integer dateParamAmount,
								  Integer codeParamAmount, Integer objParamAmount, String colPrefix, Long staffId) {
		Model model = getModel(modelCode);
		if (model == null) {
			throw new EcException(EcException.Code.OBJECT_NULL);
		}
		List<Property> propList = new ArrayList<Property>();
		LetterComparator comparator = new LetterComparator();
		String namePrefix = colPrefix != null && colPrefix.length() > 0 ? colPrefix.toLowerCase() : "";
		colPrefix = (colPrefix != null && colPrefix.length() > 0 ? colPrefix + "_" : "").toUpperCase();

		if (charParamAmount != null && charParamAmount > 0) { // 字符串
			String[] suffixArr = new String[charParamAmount];
			getSuffixArr(suffixArr, colPrefix + "CHARPARAM", model, comparator);
			for (String suffix : suffixArr) {
				Property p = new Property();
				p.setColumnName(colPrefix + "CHARPARAM" + suffix);
				String name;
				if (!"".equals(namePrefix)) {
					name = namePrefix + "Charparam" + suffix.toLowerCase();
				} else {
					name = "charparam" + suffix.toLowerCase();
				}
				p.setName(name);
				p.setCode(model.getCode() + "_" + name);
				p.setType(DbColumnType.TEXT);
				p.setFieldType(FieldType.TEXTFIELD);
				p.setFormat(ShowFormat.TEXT);
				p.setMaxLength(2000);
				p.setDisplayName("自定义字段" + colPrefix + "CP" + suffix);
				propList.add(p);
			}
		}

		if (intParamAmount != null && intParamAmount > 0) { // 整数
			String[] suffixArr = new String[intParamAmount];
			getSuffixArr(suffixArr, colPrefix + "BIGINTPARAM", model, comparator);
			for (String suffix : suffixArr) {
				Property p = new Property();
				p.setColumnName(colPrefix + "BIGINTPARAM" + suffix);
				String name;
				if (!"".equals(namePrefix)) {
					name = namePrefix + "Bigintparam" + suffix.toLowerCase();
				} else {
					name = "bigintparam" + suffix.toLowerCase();
				}
				p.setName(name);
				p.setCode(model.getCode() + "_" + name);
				p.setType(DbColumnType.INTEGER);
				p.setFieldType(FieldType.TEXTFIELD);
				p.setFormat(ShowFormat.TEXT);
				p.setDisplayName("自定义字段" + colPrefix + "IP" + suffix);
				propList.add(p);
			}
		}

		if (floatParamAmount != null && floatParamAmount > 0) { // 浮点数
			String[] suffixArr = new String[floatParamAmount];
			getSuffixArr(suffixArr, colPrefix + "NUMBERPARAM", model, comparator);
			for (String suffix : suffixArr) {
				Property p = new Property();
				p.setColumnName(colPrefix + "NUMBERPARAM" + suffix);
				String name;
				if (!"".equals(namePrefix)) {
					name = namePrefix + "Numberparam" + suffix.toLowerCase();
				} else {
					name = "numberparam" + suffix.toLowerCase();
				}
				p.setName(name);
				p.setCode(model.getCode() + "_" + name);
				p.setType(DbColumnType.DECIMAL);
				p.setFieldType(FieldType.TEXTFIELD);
				p.setFormat(ShowFormat.TEXT);
				p.setDecimalNum(6);
				p.setDisplayName("自定义字段" + colPrefix + "FP" + suffix);
				propList.add(p);
			}
		}

		if (dateParamAmount != null && dateParamAmount > 0) { // 日期时间
			String[] suffixArr = new String[dateParamAmount];
			getSuffixArr(suffixArr, colPrefix + "DATEPARAM", model, comparator);
			for (String suffix : suffixArr) {
				Property p = new Property();
				p.setColumnName(colPrefix + "DATEPARAM" + suffix);
				String name = "";
				if (!"".equals(namePrefix)) {
					name = namePrefix + "Dateparam" + suffix.toLowerCase();
				} else {
					name = "dateparam" + suffix.toLowerCase();
				}
				p.setName(name);
				p.setCode(model.getCode() + "_" + name);
				p.setType(DbColumnType.DATETIME);
				p.setFieldType(FieldType.DATETIME);
				p.setFormat(ShowFormat.YMD_HMS);
				p.setDisplayName("自定义字段" + colPrefix + "DP" + suffix);
				propList.add(p);
			}
		}

		if (codeParamAmount != null && codeParamAmount > 0) { // 系统编码
			String[] suffixArr = new String[codeParamAmount];
			getSuffixArr(suffixArr, colPrefix + "SCPARAM", model, comparator);
			for (String suffix : suffixArr) {
				Property p = new Property();
				p.setColumnName(colPrefix + "SCPARAM" + suffix);
				String name;
				if (!"".equals(namePrefix)) {
					name = namePrefix + "Scparam" + suffix.toLowerCase();
				} else {
					name = "scparam" + suffix.toLowerCase();
				}
				p.setName(name);
				p.setCode(model.getCode() + "_" + name);
				p.setType(DbColumnType.SYSTEMCODE);
				p.setFieldType(FieldType.SELECTCOMP);
				p.setFormat(ShowFormat.SELECTCOMP);
				p.setMaxLength(2000);
				p.setDisplayName("自定义字段" + colPrefix + "SCP" + suffix);
				propList.add(p);
			}
		}

		if (objParamAmount != null && objParamAmount > 0) { // 对象
			String[] suffixArr = new String[objParamAmount];
			getSuffixArr(suffixArr, colPrefix + "OBJPARAM", model, comparator);
			for (String suffix : suffixArr) {
				Property p = new Property();
				p.setColumnName(colPrefix + "OBJPARAM" + suffix);
				String name;
				if (!"".equals(namePrefix)) {
					name = namePrefix + "Objparam" + suffix.toLowerCase();
				} else {
					name = "objparam" + suffix.toLowerCase();
				}
				p.setName(name);
				p.setCode(model.getCode() + "_" + name);
				p.setType(DbColumnType.OBJECT);
				p.setFieldType(FieldType.SELECTCOMP);
				p.setFormat(ShowFormat.SELECTCOMP);
				p.setDisplayName("自定义字段" + colPrefix + "OP" + suffix);
				propList.add(p);
			}
		}

		Map<String, String> i18Parms = new HashMap<>();
		List<String> i18Keys = internationalService.initI18nKeys(model.getEntity().getModule().getArtifact(),propList.size());
//		for (Property prop : propList) {
		for (int i = 0; i < propList.size(); i++) {
			Property prop = propList.get(i);
			if ((prop.getColumnName()).length() > 28) {
				throw new EcException(EcException.Code.TOO_LONG);
			}
			prop.setIsCustom(true);
			prop.setCreateTime(new Date());
			prop.setModel(model);
			prop.setEntityCode(model.getEntity().getCode());
			prop.setModuleCode(model.getEntity().getModule().getCode());
			prop.setIsUsedForList(false); // 列表视图不允许拖出来
			prop.setNullable(true);
			prop.setFetchMode(null);
			prop.setCreateStaffId(staffId);
			// 字段显示名称的国际化key
//			String displayNameKey = model.getEntity().getModule().getArtifact() + ".propertydisplayName.radion" + UUID.randomUUID().toString().replace("-", "");
//			String displayNameKey = internationalService.initI18nKey(model.getEntity().getModule().getArtifact());
//			String displayNameValue = prop.getDisplayName();
			//Map map = new HashMap();
			//map.put("zh_CN", displayNameValue);
			if(null!=i18Keys && i18Keys.size()==propList.size()){
				i18Parms.put(i18Keys.get(i), prop.getDisplayName());
			}
//			String displayName="key=" + displayNameKey+"$&#zh_CN="+displayNameValue;

//			internationalService.addInternational(displayName);
			prop.setDisplayName(i18Keys.get(i));
			propertyDao.save(prop);
		}
		internationalService.messageResourceAddOrUpdateList(i18Parms,model.getEntity().getModule().getArtifact(),"zh_CN");
		propertyDao.flush();
		//FileUtils.updateXml(propList);
		//开始保存模块信息数据的最后修改时间
//		ModuleGenerateInfo generateInfo = moduleGenerateInfoService.getModuleGenerateInfoByModuleCode(model.getModuleCode());
//		if(generateInfo!=null){
//			generateInfo.setLastModifyTime(new Date());
//			moduleGenerateInfoService.save(generateInfo,Thread.currentThread());
//		}else{
//			generateInfo = new ModuleGenerateInfo();
//			generateInfo.setLastModifyTime(new Date());
//			generateInfo.setModuleCode(model.getModuleCode());
//			moduleGenerateInfoService.create(generateInfo,Thread.currentThread());
//		}
		try {
			FieldSyncDBUtils.customFieldSyncToDb(propList, template);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * @author mkp
	 * 保存自定义字段顺序
	 * @param orderModeltCol
	 *
	 */
	@Override
	public void saveOrderModeltCol(String orderModeltCol) {
		String[] orderParams=orderModeltCol.split(";");
		String moduleCode = "";
		int index = 0 ;//计数器
		for(String param:orderParams){
			String[] p=param.split(",");
			if(p.length>1){
				String id=p[0].toString();
				int sort=Integer.valueOf(p[1]);
				Property dm=propertyDao.load(id);
				dm.setSort(sort);
				propertyDao.save(dm);
//				cache.remove(CACHE_MODEL_PROPERTY_PREFIX + dm.getCode());
				if(index==0){
					moduleCode = dm.getModuleCode();
				}
				index++;
			}
		}
	}

	/**
	 * @author wuqi
	 * 获取自定义字段COLUMN_NAME的后缀名
	 * @param suffixArr
	 * @param colPrefix
	 * @param model
	 * @param comparator
	 */
	private void getSuffixArr(String[] suffixArr, String colPrefix, Model model, LetterComparator comparator) {
		List<Property> list = propertyDao.findByHql("from Property p where p.columnName like ? and p.model = ? and p.valid = true", new Object[] { colPrefix + "%",
				model });
		String startSuffix = null;
		List<String> sufixList = new ArrayList<String>();
		if (list != null && list.size() > 0) {
			for (Property p : list) {
				String pSuffix = p.getColumnName().toUpperCase().substring((colPrefix).length());
				if (pSuffix.matches("^[A-Z]+$")) {
					sufixList.add(pSuffix);
				}
			}
			if (sufixList.size() > 0) {
				Collections.sort(sufixList, comparator);
				startSuffix = sufixList.get(0);
			}
		}
		int[] start;
		if (startSuffix == null) {
			start = new int[] { 64 };
		} else {
			int len = startSuffix.length();
			start = new int[len];
			while (len-- > 0) {
				start[len] = startSuffix.charAt(len);
			}
		}
		int length = suffixArr.length;
		while (length-- > 0) {
			int s = start.length - 1;
			while (start[s] + 1 > 90) {
				start[s--] = 65;
				if (s < 0) {
					int[] newArr = new int[start.length + 1];
					System.arraycopy(start, 0, newArr, 0, start.length);
					newArr[newArr.length - 1] = 65;
					start = newArr;
					break;
				}
			}
			if (s > -1) {
				start[s]++;
			}
			suffixArr[length] = intArrToString(start);
		}
	}

	private String intArrToString(int[] start) {
		StringBuilder str = new StringBuilder();
		int i = 0;
		while (i < start.length) {
			str.append((char) start[i++]);
		}
		return str.toString();
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Property> findPropertiesByModel(Model model) {
		// return propertyDao.findByHql("from Property where model = ? and valid = true", model);
		if (model != null && model.getCode() != null) {
			DetachedCriteria detachedCriteria = DetachedCriteria.forClass(Property.class);
			detachedCriteria.add(Restrictions.eq("model.code", model.getCode()));
			detachedCriteria.add(Restrictions.eq("valid", true));
			detachedCriteria.add(Restrictions.not(Restrictions.eq("type", DbColumnType.SYSTEMCODE)));
			detachedCriteria.add(Restrictions.not(Restrictions.eq("type", DbColumnType.OBJECT)));
			return propertyDao.findByCriteria(detachedCriteria.addOrder(Order.asc("sort")));
		}
		return null;
	}


	/**
	 * 检查对象型字段主关联属性的唯一性
	 * @param property
	 */
	public boolean checkObjPropertyMainAssociated(Property property){
		boolean isUnique = false;
		if (property.getIsMainAssociated()) {
			String hql = "select p.code from Property p where p.valid = true "
					+ " and p.isMainAssociated = true and p.model = ? and p.code != ?";
			Integer size = propertyDao.createQuery(hql,property.getModel(), property.getCode()).list().size();
			if (size > 0) {
				isUnique = true;
			}
		}
		return isUnique;
	}

	/**
	 * 检查模型是否可被删除</br>
	 * 检查项 1.是否被其它模型字段关联  2.是否被视图关联  3.是否被DataGrid关联  4.是否被多选控件关联
	 * @param model
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<String> checkDeleteModel(Model model){
		List<String> msgs = new LinkedList<String>();
		List<Object> args = new LinkedList<Object>();
		String modelCode = model.getCode();
		//判断模型中的字段是否被其它模型中字段关联
		String propertyHql = "from Property where valid=true and associatedProperty.model.code=? and type=? and model.code != ?";
		args.add(modelCode);
		args.add(DbColumnType.OBJECT);
		args.add(modelCode);
		List<Property> props = propertyDao.findByHql(propertyHql, args.toArray());
		if (null != props && !props.isEmpty()) {
			for (Property prop : props) {
				StringBuilder sb = new StringBuilder(InternationalResource.get(prop.getModel().getEntity().getModule().getName())).append("模块-");
				sb.append(InternationalResource.get(prop.getModel().getEntity().getName())).append("实体-");
				sb.append(InternationalResource.get(prop.getModel().getName())).append("模型-");
				sb.append(prop.getName()).append("字段");
				msgs.add(sb.toString());
			}
		}

		Set<View> viewSet = new HashSet<View>();
		//判断是否被视图关联
		DetachedCriteria viewCriteria = DetachedCriteria.forClass(View.class);
		viewCriteria.add(Restrictions.eq("assModel.code", modelCode));
		List<View> views = viewDao.findByCriteria(viewCriteria);
		viewSet.addAll(views);

		DetachedCriteria dgCriteria = DetachedCriteria.forClass(DataGrid.class);
		dgCriteria.add(Restrictions.eq("targetModel.code", modelCode));
		List<DataGrid> dgs = dataGridDao.findByCriteria(dgCriteria);
		for(DataGrid dg : dgs){
			viewSet.add(dg.getView());
		}
		for(View view : viewSet){
			String msg = InternationalResource.get(view.getEntity().getModule().getName())+ "模块-"
					+ InternationalResource.get(view.getEntity().getName()) + "实体-"
					+ InternationalResource.get(view.getDisplayName()) + (view.getCode().endsWith("__mobile__")?"移动视图":"视图");
			msgs.add(msg);
		}

		return StringUtils.sort(msgs);
	}

	/**
	 * 检查字段是否可以删除 </br>
	 * 检查范围：视图、其它字段
	 * @param property 字段
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<String> checkDeleteProperty(Property property){
		List<String> msgs = new LinkedList<String>();
		String moduleCode = property.getModuleCode();
		String moduleRelationSql = "SELECT MODULE_CODE FROM ec_module_relation WHERE TARGET_MODULE_CODE = ?";
		List<String> relationCodes = modelDao.createNativeQuery(moduleRelationSql, moduleCode).list();
		if(null == relationCodes){
			relationCodes = new ArrayList<String>();
		}
		relationCodes.add(moduleCode);
		// 判断是否被其它字段关联
		String propertyHql = "from Property where valid=true and associatedProperty.code=? and type=? and moduleCode in (:relationCodes)";
		List<Object> args = new LinkedList<Object>();
		args.add(property.getCode());
		args.add(DbColumnType.OBJECT);
		List<Property> props = propertyDao.createQuery(propertyHql, args.toArray()).setParameterList("relationCodes", relationCodes).list();
		if (null != props && !props.isEmpty()) {
			for (Property prop : props) {
				StringBuilder sb = new StringBuilder(InternationalResource.get(prop.getModel().getEntity().getModule().getName())).append("模块-");
				sb.append(InternationalResource.get(prop.getModel().getEntity().getName())).append("实体-");
				sb.append(InternationalResource.get(prop.getModel().getName())).append("模型-");
				sb.append(prop.getName()).append("字段");
				msgs.add(sb.toString());
			}
		}

		Set<View> viewSet = new HashSet<View>();

		// 判断是否被组态期视图关联
		String fieldSql = "from Field where valid=true and fullPropertyCode like ? and moduleCode in (:relationCodes)";
		args = new LinkedList<Object>();
		if(property.getType().equals(DbColumnType.OBJECT)){//对象类型
			fieldSql += " OR FULL_PROPERTY_CODE LIKE ?";
			args.add("%" + property.getCode() + "||%");
			args.add("%" + property.getCode() + "-%");//多选控件
		} else {
			args.add("%" + property.getCode());
		}
		List<Field> fields = modelDao.createQuery(fieldSql, args.toArray()).setParameterList("relationCodes", relationCodes).list();
		if(null != fields && !fields.isEmpty()){
			for(Field field : fields){
				if (field.getView() != null) {
					viewSet.add(field.getView());
				}
				if (field.getDataGrid() != null) {
					viewSet.add(field.getDataGrid().getView());
				}
				if (field.getFastQueryJson() != null) {
					viewSet.add(field.getFastQueryJson().getView());
				}
				if (field.getAdvQueryJson() != null) {
					viewSet.add(field.getAdvQueryJson().getView());
				}
			}
		}
		for(View view : viewSet){
			String msg = InternationalResource.get(view.getEntity().getModule().getName())+ "模块-"
					+ InternationalResource.get(view.getEntity().getName()) + "实体-"
					+ InternationalResource.get(view.getDisplayName()) + (view.getCode().endsWith("__mobile__")?"移动视图":"视图");
			msgs.add(msg);
		}
		
		String importTemplateSql="SELECT value FROM ec_import_template where code=?";
		List<Map<String,Object>> templateList=template.queryForList(importTemplateSql, property.getModel().getCode());
		if(templateList.size()>0) {
			String template=templateList.get(0).get("value").toString();
			if(template.contains("<propertyCode><![CDATA["+property.getCode()+"]]></propertyCode>")) {
				msgs.add(InternationalResource.get("ec.configMenu.importTemplateConfig"));
			}
		}
		return StringUtils.sort(msgs);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<AssociatedInfo> findAssociatedInfosForTemplate(Model model, Boolean showCustom, int... associatedTypes) {
		List<Object> args = new ArrayList<Object>();
		String hql = "";
		if(showCustom){
			hql = "from Property p where p.valid=true and ((p.associatedProperty is not null and p.model = ? and p.model.valid=true and p.isCustom=true) or (p.associatedProperty is not null and p.associatedProperty.model = ? and p.associatedProperty.model.valid=true))";
		}else{
			hql = "from Property p where p.valid=true and ((p.associatedProperty is not null and p.model = ? and p.model.valid=true and p.isCustom=false) or (p.associatedProperty is not null and p.associatedProperty.model = ? and p.associatedProperty.model.valid=true))";
		}
		args.add(model);
		args.add(model);
		if (associatedTypes != null && associatedTypes.length > 0) {
			String subHql = "";
			for (int associatedType : associatedTypes) {
				if (subHql.length() > 0) {
					subHql += " or ";
				}
				subHql += " p.associatedType=? ";
				args.add(associatedType);
				if (associatedType == AssociatedInfo.ONE_TO_MANY) {
					subHql += " or p.associatedType=? ";
					args.add(AssociatedInfo.MANY_TO_ONE);
				} else if (associatedType == AssociatedInfo.MANY_TO_ONE) {
					subHql += " or p.associatedType=? ";
					args.add(AssociatedInfo.ONE_TO_MANY);
				}
			}
			hql += " and (" + subHql + ")";
		}

		List<Property> properties = propertyDao.findByHql(hql, args.toArray());
		List<AssociatedInfo> assos = new ArrayList<AssociatedInfo>();
		for (Property p : properties) {
			assos.add(createAssociatedInfo(model, p));
		}
		return assos;
	}

	/**
	 * 保存模块信息数据的最后修改时间
	 * @param model
	 */
	public void saveModuleGenerateInfo(Model model) {
//		ModuleGenerateInfo generateInfo = moduleGenerateInfoService.getModuleGenerateInfoByModuleCode(model.getModuleCode());
//		if(generateInfo!=null){
//			generateInfo.setLastModifyTime(new Date());
//			moduleGenerateInfoService.save(generateInfo,Thread.currentThread());
//		}else{
//			generateInfo = new ModuleGenerateInfo();
//			generateInfo.setLastModifyTime(new Date());
//			generateInfo.setModuleCode(model.getModuleCode());
//			moduleGenerateInfoService.create(generateInfo,Thread.currentThread());
//		}
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Property findPropertyByCode(String propertyCode) {
		List<Property> list = propertyDao.findByHql("from Property as p where p.code =?0", propertyCode);
		if (!list.isEmpty()) {
			return list.get(0);
		} else {
			return null;
		}
	}
	@Override
	public void deleteCustomPropertyModelMappingsForImport(String moduleCode) {
		Query query = customPropertyModelMappingDao.createQuery("delete CustomPropertyModelMapping c where c.model.code like ?0", moduleCode+"%");
		query.executeUpdate();
	}

	@Override
	public List<CustomPropertyModelMapping> findCustomPropertyModelMappingsForExport(
			String modelCode) {
		List<CustomPropertyModelMapping> list = customPropertyModelMappingDao
				.findByHql(
						"from CustomPropertyModelMapping c where c.model.code = ? and c.id is not null order by c.sort,c.enableCustom desc",
						new Object[] { modelCode });
		return list;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void saveCustomPropertyModelMapping(CustomPropertyModelMapping modelMapping) {
		CustomPropertyModelMapping m = customPropertyModelMappingDao.findEntityByHql("from CustomPropertyModelMapping c where c.property = ?0",
				new Object[] { modelMapping.getProperty() });
		if (m != null) {
			m.setDisplayName(modelMapping.getDisplayName());
			m.setFieldType(modelMapping.getFieldType());
			m.setFormat(modelMapping.getFormat());
			m.setFillContent(modelMapping.getFillContent());
			m.setMultable(modelMapping.getMultable());
			m.setSeniorSystemCode(modelMapping.getSeniorSystemCode());
			m.setAssociatedProperty(modelMapping.getAssociatedProperty());
			m.setAssociatedType(modelMapping.getAssociatedType());
			m.setNullable(modelMapping.getNullable());
			m.setDescription(modelMapping.getDescription());
			m.setEnableCustom(modelMapping.getEnableCustom());
			m.setRefView(modelMapping.getRefView());
			m.setRelatedKey(modelMapping.getRelatedKey());
			m.setPrecision(modelMapping.getPrecision());
			//同时更新数据库
			/*if (null != modelMapping.getAssociatedProperty()) {
				m.getProperty().setAssociatedProperty(modelMapping.getAssociatedProperty());
			}
			if(null!=modelMapping.getAssociatedType()) {
				m.getProperty().setAssociatedType(modelMapping.getAssociatedType());
			}
			if(null!=modelMapping.getFillContent()) {
				m.getProperty().setFillcontent(modelMapping.getFillContent());
			}*/
		} else {
			m = modelMapping;
			//同时更新数据库
			/*if (null != modelMapping.getAssociatedProperty()) {
				m.getProperty().setAssociatedProperty(modelMapping.getAssociatedProperty());
			}
			if(null!=modelMapping.getAssociatedType()) {
				m.getProperty().setAssociatedType(modelMapping.getAssociatedType());
			}
			if(null!=modelMapping.getFillContent()) {
				m.getProperty().setFillcontent(modelMapping.getFillContent());
			}*/
		}

		customPropertyModelMappingDao.save(m);

		ProjectFlagHolder.getInstance().getProjFlag().set(true);
		Property p=propertyDao.load(m.getProperty().getCode());
		if(null!=modelMapping.getAssociatedProperty()){
			Property associated = getProperty(modelMapping.getAssociatedProperty().getCode());
			if (associated != null && null != associated.getModel()) {
				Model assoModel = getModel(associated.getModel().getCode());
				Model model = getModel(p.getModel().getCode());
				if(!model.getCode().equals(assoModel.getCode()) && assoModel.getModelName().equalsIgnoreCase(model.getModelName())){
					throw new EcException(EcException.Code.SAME_MODEL_NAME_ERROR);
				}
			}
		}
		if(p!=null){
			p.setDisplayName(m.getDisplayName());
			p.setFieldType(m.getFieldType());
			p.setFormat(m.getFormat());
			p.setFillcontent(m.getFillContent());
			p.setMultable(m.getMultable());
			p.setSeniorSystemCode(m.getSeniorSystemCode());
			p.setAssociatedProperty(m.getAssociatedProperty());
			p.setAssociatedType(m.getAssociatedType());
			p.setNullable(m.getNullable());
			p.setDescription(m.getDescription());
			p.setProjCustomInUse(m.getEnableCustom());
			p.setIsUsedForList(m.getEnableCustom());
			p.setIsUsedForSearch(m.getEnableCustom());
			p.setProjFlag(m.getEnableCustom());
			p.setFillcontent(m.getFillContent());
			propertyDao.save(p);
			propertyDao.flush();
		}
		ProjectFlagHolder.getInstance().getProjFlag().set(false);

		//如果模型内的字段启用时的状态为不可空，则需要向viewmapping内写入数据
		if(Boolean.FALSE.equals(modelMapping.getNullable()) && Boolean.TRUE.equals(modelMapping.getEnableCustom())){
			CustomPropertyViewMapping viewMapping = customPropertyViewMappingDao.findEntityByHql(
					"from CustomPropertyViewMapping c where c.property = ?0 and c.associatedCode = ?1", new Object[] { modelMapping.getProperty(), modelMapping.getProperty().getEntityCode()+"_edit"});
			if(viewMapping == null){
				viewMapping = generateCustomPropertyViewMapping(modelMapping.getProperty(), modelMapping.getEnableCustom());
				customPropertyViewMappingDao.save(viewMapping);
			}
		}

		if("sysbase_1.0".equals(modelMapping.getProperty().getModuleCode())){
			CustomPropertyViewMapping viewMapping = customPropertyViewMappingDao.findEntityByHql(
					"from CustomPropertyViewMapping c where c.property = ?0 and c.associatedCode = ?1", new Object[] { modelMapping.getProperty(), modelMapping.getProperty().getEntityCode()+"_edit"});
			if(viewMapping == null){
				viewMapping = generateCustomPropertyViewMapping(modelMapping.getProperty(), modelMapping.getEnableCustom());
			}
			viewMapping.setShowCustom(modelMapping.getEnableCustom());
			viewMapping.setNullable(modelMapping.getNullable());
			customPropertyViewMappingDao.save(viewMapping);
		}
	}

	public CustomPropertyViewMapping generateCustomPropertyViewMapping(Property p, Boolean enabled) {

		CustomPropertyViewMapping v = new CustomPropertyViewMapping();
		v.setDisplayName(p.getDisplayName());
		v.setFieldType(p.getFieldType());
		v.setFormat(p.getFormat());
		v.setMultable(p.getMultable());
		v.setNullable(Boolean.FALSE);
		v.setShowCustom(enabled);
		v.setProperty(p);
		v.setAssociatedCode(p.getEntityCode()+"_edit");
		v.setPropertyLayRec(null);
		return v;
	}

	@Override
	public String getPropertyColumnNameByTableName(String entityCode, String tableName, String propertyName, Boolean isObjectType) {
		if (entityCode != null && entityCode.length() > 0 && tableName != null && tableName.length() > 0) {
			List<Model> modelList = modelDao.findByHql("from Model m where m.entity.code = ?0 and m.tableName = ?1 and m.valid = true", new Object[] {
					entityCode, tableName });
			if (modelList != null && modelList.size() > 0) {
				Model model = modelList.get(0);
				Set<Property> props = model.getProperties();
				for (Property p : props) {
					if (propertyName.equals(p.getName())) {
						return p.getColumnName();
					}
				}
			}
		}
		if (isObjectType != null && isObjectType) {
			return com.supcon.supfusion.configuration.services.utils.Inflector.getInstance().columnize(propertyName) + "_ID";
		}
		return Inflector.getInstance().columnize(propertyName);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<AssociatedInfo> findAssociatedInfos2(Model model, int... associatedTypes) {
		List<Object> args = new ArrayList<Object>();
		String hql = "from Property p where p.valid=true and ((p.associatedProperty is not null and p.model = ? and p.model.valid=true) or (p.associatedProperty is not null and p.associatedProperty.model = ? and p.associatedProperty.model.valid=true))";
		args.add(model);
		args.add(model);
		if (associatedTypes != null && associatedTypes.length > 0) {
			String subHql = "";
			for (int associatedType : associatedTypes) {
				if (subHql.length() > 0) {
					subHql += " or ";
				}
				subHql += " p.associatedType=? ";
				args.add(associatedType);
				if (associatedType == AssociatedInfo.ONE_TO_MANY) {
					subHql += " or p.associatedType=? ";
					args.add(AssociatedInfo.MANY_TO_ONE);
				} else if (associatedType == AssociatedInfo.MANY_TO_ONE) {
					subHql += " or p.associatedType=? ";
					args.add(AssociatedInfo.ONE_TO_MANY);
				}
			}
			hql += " and (" + subHql + ")";
		}

		List<Property> properties = propertyDao.findByHql(hql, args.toArray());
		List<AssociatedInfo> assos = new ArrayList<AssociatedInfo>();
		AssociatedInfo asso = null;
		for (Property p : properties) {
			asso = new AssociatedInfo();
			asso.setIsMainAssociated(p.getIsMainAssociated());
			if (model.getCode().equals(p.getModel().getCode()) || p.getModel().getCode().equals(p.getAssociatedProperty().getModel().getCode())) {
				asso.setOriginalProperty(p);
				asso.setTargetProperty(p.getAssociatedProperty());
				asso.setType(p.getAssociatedType());
				assos.add(asso);
			} else {
				asso.setTargetProperty(p);
				asso.setOriginalProperty(p.getAssociatedProperty());
				if (p.getAssociatedType() == Property.ONE_TO_MANY) {
					asso.setType(Property.MANY_TO_ONE);
				} else if (p.getAssociatedType() == Property.MANY_TO_ONE) {
					asso.setType(Property.ONE_TO_MANY);
				} else {
					asso.setType(p.getAssociatedType());
				}
				assos.add(asso);
			}
		}
		return assos;
	}

	@Override
	public List<String> getRunningCustomProperties(String entityCode){
		List<String> list = null;
		String sql = "select property_code from BASE_CP_MODEL_MAPPING where model_code = ? and enable_custom = 1";
		list = propertyDao.createNativeQuery(sql, entityCode).list();
		return list;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public String getMainDisplayValue(String code, Object value) {
		String val = "";
		CustomPropertyModelMapping m = customPropertyModelMappingDao.findEntityByHql(
				"from CustomPropertyModelMapping c where c.property.code = ? and c.property.valid = true", new Object[] { code });
		if (m != null && m.getProperty() != null) {
			if (DbColumnType.OBJECT.equals(m.getProperty().getType())) { // 关联对象类型
				if (m.getAssociatedProperty() != null) {
					String tableName = m.getAssociatedProperty().getModel().getTableName();
					String columnName = m.getAssociatedProperty().getColumnName();
					String mainDisplayColumnName = null;
					String pkColumnName = null;
					for (Property prop : m.getAssociatedProperty().getModel().getProperties()) {
						if (prop.getIsMainDisplay() != null && prop.getIsMainDisplay()) {
							mainDisplayColumnName = prop.getColumnName();
						}
						if (prop.getIsPk() != null && prop.getIsPk()) {
							pkColumnName = prop.getColumnName();
						}
					}
					mainDisplayColumnName = mainDisplayColumnName == null ? pkColumnName : mainDisplayColumnName;
					List rs = propertyDao.createNativeQuery(
							"select " + mainDisplayColumnName + " from " + tableName + " where " + columnName + " = ? and VALID = 1", new Object[] { value })
							.list();
					if (rs != null && rs.size() > 0) {
						val = String.valueOf(rs.get(0));
					}
				}
			} else if (DbColumnType.SYSTEMCODE.equals(m.getProperty().getType())) { // 关联系统编码
				String ids = value.toString();
				if (ids.length() > 0) {
					String[] idArr = ids.split(",");
					List<String> idList = new ArrayList<String>();
					for (String id : idArr) {
						if (m.getSeniorSystemCode()) { // S2高级系统编码
							Map<String, Object> jsonMap = m.getFillContentMap();
							String systemEntityId = (String) jsonMap.get("fillContent");
							idList.add(systemEntityId + "/" + id);
						} else {
							idList.add(id);
						}
					}
					final int PERTIME = 999;
					int count = idList.size() % PERTIME == 0 ? idList.size() / PERTIME : idList.size() / PERTIME + 1;
					List<String> valList = new ArrayList<String>();
					for (int i = 0; i < count; i++) {
						valList.addAll(propertyDao.createNativeQuery("select VALUE from BASE_SYSTEMCODE where ID in(:ids) and VALID = 1")
								.setParameterList("ids", idList.subList(PERTIME * i, PERTIME * i + (i < count - 1 ? PERTIME : (idList.size() % PERTIME))))
								.list());
					}

					StringBuilder sb = new StringBuilder();
					if (valList != null && valList.size() > 0) {
						for (String key : valList) {
							String s=InternationalResource.get(key);
							sb.append(",").append(s);
						}
					}
					val = sb.length() > 0 ? sb.substring(1) : "";
				}
			}
		}
		return val;
	}

	@Override
	@Transactional(readOnly=true, propagation=Propagation.SUPPORTS)
	public List<Entity> getEntities(String moduleCode) {
		return entityDao.findByHql("from Entity e where e.module.code = ? and e.valid = true and e.module.valid = true", new Object[] { moduleCode });
	}
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Module> getAllModule(String... env) {
		return moduleDao.findByHql("from Module where valid=true and (isHide=false or isHide is null)");
	}
	@Override
	public Entity getEntity(String entityCode) {
		return entityDao.load(entityCode);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Module getModule(String moduleCode) {
		return moduleDao.findEntityByHql("from Module where code = ? and valid=true", moduleCode);
	}
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public CustomPropertyModelMapping getAssociatedCustomPropertyModelMapping(String propertyCode) {
		String hql = "from CustomPropertyModelMapping c where property_code = ? and enable_custom = 1 and associated_property_code is not null";
		List<CustomPropertyModelMapping> list = customPropertyModelMappingDao.findByHql(hql, new Object[] {propertyCode});
		if (null != list && list.size() > 0) {
			return list.get(0);
		}
		return null;
	}

}
