
/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * <p>
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.base.entities.Deployment;
import com.supcon.supfusion.base.entities.MenuInfo;
import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.base.enums.MenuOperateType;
import com.supcon.supfusion.base.enums.OperateTarget;
import com.supcon.supfusion.base.services.*;
import com.supcon.supfusion.configuration.services.dao.*;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.*;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.service.*;
import com.supcon.supfusion.configuration.services.utils.*;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.scheduler.server.api.dto.SchedulerJobDTO;
import com.supcon.supfusion.scheduler.server.api.service.ISchedulerJobApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 如果方法名没有声明unvalid,均带有valid=true条件.
 *
 * @author songjiawei
 */
@ServiceApiService("ec_ModuleService")
@Transactional
@Slf4j
public class ModuleServiceImpl extends BaseServiceImpl<Module> implements ModuleService {

    private static final String MIS = "Mis";
    private static Map<String, Set<String>> keys = new ConcurrentHashMap<String, Set<String>>();

    @Autowired
    private ModuleDaoImpl moduleDao;
    @Autowired
    private CustomCodeDaoImpl customCodeDao;
    @Autowired
    private ViewService viewService;
    @Autowired
    private EntityService entityService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private SqlService sqlService;
    @Autowired
    private PropertyDaoImpl propertyDao;
    @Autowired
    private DataGridService dataGridService;
    @Autowired
    private MenuInfoService menuInfoService;
    @Autowired
    private MenuOperateService menuOperateService;
    @Autowired
    private ProcessService processService;
    @Autowired
    private ScriptService scriptService;

    //add by yubo20171221  jdbcTemplate
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SystemCodeService systemCodeService;
    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private DeployInfoDaoImpl deployInfoDao;
    @Autowired
    private FieldService fieldService;
    @Autowired
    private EventService eventService;
    @Autowired
    private ButtonService buttonService;
    @Autowired
    private CustomerConditionService customerConditionService;
    @Autowired
    private ModuleReferenceService moduleReferenceService;
    @Autowired
    private DataGroupDaoImpl dataGroupDao;
    @Autowired
    private DataClassificDaoImpl dataClassificDao;
    @Autowired
    ConditionService conditionService;
    @Autowired
    private ModuleRelationDaoImpl moduleRelationDao;
    @Autowired
    private ModuleReferenceDao moduleReferenceDao;
    @Autowired
    private PropertyKeyService propertyKeyService;
    @Autowired
    private ButtonDaoImpl buttonDao;

    @javax.annotation.Resource(name = "sessionFactory")
    private SessionFactory ecSessionFactory;

	@Autowired
    private MneCodeDataDealService mneCodeDataDealService;

	@Autowired
	private ModuleService moduleService;
    @Autowired
    private BAPModuleXmlImportService bapModuleXmlEcImportService;

    @Autowired
	private InternationalService internationalService;
	@Autowired
	private CompanyService companyService;
	@Autowired
	private ModuleCompanyRefDaoImpl moduleCompanyRefDao;
	@Value("${bap.suposinit:true}")
	private Boolean supos;
	@Autowired
	private SchedulerService schedulerService;
	@Autowired
	private ISchedulerJobApiService iSchedulerJobApiService;

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Module getModule(String code, boolean full) {
		Module module = getModule(code);
		if (full && null != module) {
			for (Entity entity : module.getEntities()) {
				if (!Hibernate.isInitialized(entity)) {
					Hibernate.initialize(entity);
				}
				Set<Model> models = entity.getModels();
				for (Model model : models) {
					if (!Hibernate.isInitialized(model)) {
						Hibernate.initialize(model);
					}
					final Set<Property> properties = model.getProperties();
					if (null != properties && !properties.isEmpty()) {
						for (Property property : properties) {
							if (!Hibernate.isInitialized(property)) {
								Hibernate.initialize(property);
							}
						}
					}
				}
				Set<View> views = entity.getViews();
				if (null != views && !views.isEmpty()) {
					for (View view : views) {
						if (!Hibernate.isInitialized(view)) {
							Hibernate.initialize(view);
						}
						if (!Hibernate.isInitialized(view.getAssModel()))
							Hibernate.initialize(view.getAssModel());
						List<DataGrid> dataGrids = view.getDataGrids();
						if (dataGrids != null && !dataGrids.isEmpty()) {
							for (DataGrid dg : dataGrids) {
								if (!Hibernate.isInitialized(dg))
									Hibernate.initialize(dg);
							}
						}
					}
				}
			}
		}
		return module;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Module> findModule(List<String> codes){
		return moduleDao.createQuery("from Module where code in (:codes) and valid=true").setParameterList("codes", codes).list();
	}


	@Override
	@Transactional(timeout = -1)
	public String reallyDeleteModule(Module module) {
		module = getModule(module.getCode());
		Set<String> descSet = new LinkedHashSet<String>();
		List<ModuleRelation> assModules = getBeAssociated(module, false);
		//删除模块时查询是否已被服务关联
		getMsModuleRel(module,descSet);
		checkAppRelation(module, descSet);
		if (assModules != null && assModules.size() > 0) {
			//删除对象关联了模块，请先删除对应的模块！ add by yubo20171219
			for (ModuleRelation moduleRelation : assModules) {
				descSet.add(InternationalResource.get("ec.module.delete.moduledependent",new Object[]{InternationalResource.get(moduleRelation.getModule().getName())} ));
			}
		}
		List<ModuleReference> references = listModuleReferences(module, false);
		if (references != null && references.size() > 0) {
			for (ModuleReference moduleReference : references) {
				descSet.add(InternationalResource.get("ec.module.delete.modulerefrenced",new Object[]{InternationalResource.get(moduleReference.getModule().getName())}));
			}
		}

		String msg = deleteMenuInfo(module);
		if (!StringUtils.isEmpty(msg)) {
			descSet.add(msg);
		}
		// 依赖、引用提前提示，防止下面步骤超时不提示
		if(!descSet.isEmpty()){
			return JsonUtils.setToJson(descSet);
		}

		systemCodeService.deleteSystemEntityAndCode(module.getArtifact());

		Hibernate.initialize(module.getEntities());
		try {
			//删除工作流待办
			if(null != module.getEntities()){
				for(Entity entity : module.getEntities()){
					List<Deployment> deploymentList = processService.findDeployments(entity.getCode());
					if(null != deploymentList && !deploymentList.isEmpty()) {
						for(Deployment deployment:deploymentList){
							processService.deleteFlow(deployment, true, false);
						}
					}
				}
			}
			deleteEC(module.getCode().toString());
			deleteRUNTIME(module.getCode().toString());
			deleteProject(module.getCode());
			deletePortlets(module.getCode());
			//删除自定义字段
			deleteModelMapping(module.getCode().toString());
			deleteViewMapping(module.getCode().toString());
			//删除generate和customFile相关文件
			//File generateFile = new File(PropertyHolder.get().getGeneratePath() + File.separator + module.getCode());

			log.info(PropertyHolder.get().getGeneratePath() + File.separator + module.getCode()+"开始删除");
			modelService.deleteModuleFile(module);
			moduleRegistryService.deleteModule(module.getCode());
			log.info(PropertyHolder.get().getGeneratePath() + File.separator + module.getCode()+"删除完成");
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			log.error(e1.getMessage());
		}
		return "";
	}


	private void checkAppRelation(Module module,Set<String> descSet) {
		try {
			List<String> list = moduleDao.createNativeQuery("SELECT NAME FROM SUPOS_APP WHERE MODULES LIKE ?", "%"+module.getCode()+"%").list();
			if (null != list && list.size() > 0 && list.get(0) != null) {
				String name = String.valueOf(list.get(0));
				descSet.add(InternationalResource.get("ec.module.deploy.beenintroductedbyapp", new Object[]{name}));
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	 /**
     * 删除文件夹
     * @param folderPath 文件夹完整绝对路径 ,"Z:/xuyun/save"
     */
    private boolean delFolder(String folderPath) {
    	boolean flag = false;
        try {
            delAllFile(folderPath); //删除完里面所有内容
            String filePath = folderPath;
            filePath = filePath.toString();
            File myFilePath = new File(filePath);
            myFilePath.delete(); //删除空文件夹
            flag = true;
        } catch (Exception e) {
            flag = false;
        }finally{
        	return flag;
        }
    }

    /**
     * 删除指定文件夹下所有文件
     * @param path 文件夹完整绝对路径
     */
    private boolean delAllFile(String path) {
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return flag;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        String[] tempList = file.list();
        File temp = null;
        if(tempList !=null){
        for (int i = 0; i < tempList.length; i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                delAllFile(path + "/" + tempList[i]);//先删除文件夹里面的文件
                delFolder(path + "/" + tempList[i]);//再删除空文件夹
                flag = true;
            }
        }}
        return flag;
    }

	private void deleteEC(String code) throws Exception {

		for (ECEnum ecEnum : ECEnum.values()) {
			//String sql = "delete from " + ecEnum + " where code like ? escape '\\' ";
			//sessionFactoryUtil.executeDelete(sql, code);
			String sql = "delete from " + ecEnum + " where code like ? ";
			jdbcTemplate.update(sql, code+"%");
		}
		//String sql1 = "delete from EC_ADV_QUERY_CONDITION_ITEM where CONDITION_ID in (select id from EC_ADV_QUERY_CONDITION where VIEW_CODE like ?  escape '\\' ) ";
		String sql1 = "delete from ec_adv_query_condition_item where CONDITION_ID in (select id from ec_adv_query_condition where VIEW_CODE like ?  ) ";
		//sessionFactoryUtil.executeDelete(sql1, code);
		jdbcTemplate.update(sql1, code+"%");
		sql1 = "delete from ec_adv_query_condition  where VIEW_CODE like ? ";
		//sessionFactoryUtil.executeDelete(sql1, code);
		jdbcTemplate.update(sql1, code+"%");
		//sessionFactoryUtil.close();
	}

	private void deleteRUNTIME(String code) throws Exception {

		for (RuntimeEnum runtimeEnum : RuntimeEnum.values()) {
			String sql = "delete from " + runtimeEnum + " where code like ?  ";
			//sessionFactoryUtil.executeDelete(sql, code);
			jdbcTemplate.update(sql, code+"%");
		}
		String sql1 = "delete from RUNTIME_ADV_QUERY_JSON  where VIEW_CODE like ?  ";
		//sessionFactoryUtil.executeDelete(sql1, code);
		jdbcTemplate.update(sql1, code+"%");
		sql1 = "delete from action_view  where VIEW_CODE like ?";
		//sessionFactoryUtil.executeDelete(sql1, code);
		jdbcTemplate.update(sql1, code+"%");
		//sessionFactoryUtil.close();
	}
	private void deleteProject(String code) throws Exception {
		for (ProjectEnum projEnum : ProjectEnum.values()) {
			String sql = "delete from " + projEnum + " where code like ?  ";
			// sessionFactoryUtil.executeDelete(sql, code);
			jdbcTemplate.update(sql, code + "%");
		}
		String sql1 = "delete from PROJECT_ADV_QUERY_JSON  where VIEW_CODE like ?  ";
		// sessionFactoryUtil.executeDelete(sql1, code);
		jdbcTemplate.update(sql1, code + "%");
		// sessionFactoryUtil.close();
	}

	private void deleteModelMapping(String code){
		String sql = "delete from BASE_CP_MODEL_MAPPING where MODEL_CODE like ?  ";
		//SessionFactoryUtil.getInstance().executeDelete(sql, code);
		jdbcTemplate.update(sql, code+"%");
	}

	private void deleteViewMapping(String code){
		String sql = "delete from BASE_CP_VIEW_MAPPING where PROPERTY_CODE like ?  ";
		//SessionFactoryUtil.getInstance().executeDelete(sql, code);
		jdbcTemplate.update(sql, code+"%");
	}
	/**
	 * 刪除portlet
	 * @param moduleCode
	 */
	private void deletePortlets(String moduleCode){
		String sql = "delete from EC_PORTLET where MODULE_CODE = ?";
		jdbcTemplate.update(sql, moduleCode);
	}

	private String deleteMenuInfo(Module module) {
		return menuInfoService.deleteRbacAllByModuleCode(module.getArtifact());
	}

	@Override
	@Transactional
	public void saveModule(Module module) {
		if (!checkArtifactUnique(module)) {
			throw new EcException(EcException.Code.UNIQUECODE);
		}
		//新增校验关键字
		if (StringUtils.isEmpty(module.getCode())){
			if (!propertyKeyService.checkDBKey(module.getArtifact()) || !propertyKeyService.checkJavaKey(module.getArtifact())
					|| !propertyKeyService.checkPropertyKey(module.getArtifact())) {
				log.error("不允许使用关键字：" + module.getArtifact());
				throw new EcException(EcException.Code.KEY);
			}
		}
		if (null == module.getCode() || module.getCode().length() == 0) {
			String code = module.getArtifact() + "_" + module.getProjectVersion();
			module.setCode(code);
			String artifact = module.getArtifact();
			String newInternationName = "key=reg.moduleName." + artifact + "$&#" + module.getName().split("&#")[1];
			module.setName(newInternationName);
			if(module.getCategory().startsWith("key=appConfig.")){
				String categoryKey = module.getCategory().replace("key=appConfig","key="+artifact);
				module.setCategory(categoryKey);
			}
		} else {
			if(StringUtils.isEmpty(module.getAcronym())){
				module.setAcronym(module.getArtifact());
			}
		}
		moduleDao.save(module);
		//生成升级文件临时目录
		FileUtils.createNewTempFile(module);
	}

	@Autowired
	private ModuleRegistryService moduleRegistryService;

	@Override
	@Transactional
	public void saveModule(Module module, Map map){
		saveModule(module);
		saveModuleRelation(module, map);
		saveModuleReference(module);
		saveModuleCompanyRef(module);
		moduleRegistryService.registryModule(module.getCode());
	}



	@Override
	@Transactional
	public void saveModuleRelation(Module module, Map map){
		String[] strs = (String[]) map.get("moduleSelectsAddIds");
		if(strs != null && strs.length > 0){
			String idStr = strs[0];
			if(idStr != null && idStr.length() > 0){
				String[] ids = idStr.split(",");
				for(String id : ids){
					if(id != null && id.length() > 0){
						getMsModuleRelByCode(module,id);
						Module target = getModule(id);
						if(moduleReferenceService.isModuleReference(module.getCode(), id, module.getModuleReferenceDeleteIds())){
//							throw new BAPException(InternationalResource.get(target.getName()) + " " + InternationalResource.get("ec.module.alreadyReference"));
							throw new EcException(InternationalResource.get(target.getName()) + InternationalResource.get("ec.view.alreadyRefer") + InternationalResource.get(module.getName()));
						}
						ModuleRelation relation = getRelationWithNoValid(module, target);
						relation.setCode(module.getCode()+target.getCode());
						relation.setTarget(target);
						relation.setModule(module);
						relation.setValid(true);
						saveRelation(relation);
					}
					saveMsModuleRelByCode(module,id);
				}
			}
		}
		String[] dels = (String[]) map.get("moduleSelectsDeleteIds");
		if(dels != null && dels.length > 0){
			String iddels = dels[0];
			module.setModuleRelationDeleteIds(iddels);
			if(iddels != null && iddels.length() > 0){
				String[] ids = iddels.split(",");
				Set<String> msgs = checkModuleRelationDelete(module, Arrays.asList(ids));
				if (msgs != null && !msgs.isEmpty()) {
					StringBuilder sb = new StringBuilder();
					for (String msg : msgs) {
						sb.append("<li>").append(msg).append("</li>");
					}
					throw new EcException(InternationalResource.get("ec.module.relation.delete") + sb);
				}
				for(String id : ids){
					if(id != null && id.length() > 0){
						Module target = getModule(id);
						ModuleRelation relation = getRelation(module, target);
						deleteRelationPhysical(relation);
					}
					deleteMsRelByCode(module,id);
				}
			}
		}
	}

	@Override
	@Transactional
	public void saveModuleReference(Module module){
		String idStr = module.getModuleReferenceAddIds();
		if(idStr != null && idStr.length() > 0){
			String[] ids = idStr.split(",");
			for(String id : ids){
				if(id != null && id.length() > 0){
					Module target = getModule(id);
					if(checkModuleRelation(id, module.getCode())){
//						throw new BAPException(InternationalResource.get(target.getName()) + " " + InternationalResource.get("ec.module.alreadyRelation"));
						throw new EcException(InternationalResource.get(target.getName()) + InternationalResource.get("ec.view.alreadyRelat") + InternationalResource.get(module.getName()));
					}
					//模块之间不能相互引用
					if(checkModuleReference(id, module.getCode())){
						throw new EcException(InternationalResource.get(target.getName())+ InternationalResource.get("ec.view.alreadyRefer")+ InternationalResource.get(module.getName()));
					}
					ModuleReference reference = moduleReferenceService.getReferenceWithNoValid(module, target);
					reference.setCode(module.getCode()+target.getCode());
					reference.setTarget(target);
					reference.setModule(module);
					reference.setValid(true);
					moduleReferenceService.save(reference);
				}
			}
		}
		String idDels = module.getModuleReferenceDeleteIds();
		if(idDels != null && idDels.length() > 0){
			String[] ids = idDels.split(",");
			for(String id : ids){
				if(id != null && id.length() > 0){
					Module target = getModule(id);
					ModuleReference reference = moduleReferenceService.getReferenceWithValid(module, target);
					moduleReferenceService.deleteReferencePhysical(reference);
				}
			}
		}
	}

	private Boolean checkModuleRelation(String targetId, String moduleCode) {
		List<ModuleRelation> relations = moduleRelationDao.findByHql(
				"From ModuleRelation where module.code = ? and valid = true",
				targetId);
		if (null != relations && relations.size() > 0) {
			for (ModuleRelation mrelation : relations) {
				if (null != targetId
						&& moduleCode.equals(mrelation.getTarget().getCode())) {
					return true;
				}
			}
		}
//		List<ModuleRelation> moduleRelations = moduleRelationDao.findByHql(
//				"From ModuleRelation where target.code = ? and valid = true",
//				targetId);
//		if (null != moduleRelations && moduleRelations.size() > 0) {
//			for (ModuleRelation relation : moduleRelations) {
//				if (null != targetId
//						&& delIds.indexOf("," + relation.getTarget().getCode()) == -1) {	//不在这次操作的依赖模块的删除列表里面
//					return true;
//				}
//			}
//		}
		return false;
	}

	private Boolean checkModuleReference(String addCode, String moduleCode) {
		if(null !=moduleCode){
			List<ModuleReference> moduleReference = moduleRelationDao.findByHql(
					"From ModuleReference where module.code = ? and valid = true",
					addCode);
			if (null != moduleReference && moduleReference.size() > 0) {
				for (ModuleReference relation : moduleReference) {
					if(moduleCode.equals(relation.getTarget().getCode())){
						return true;
					}
				}
			}
	}
		return false;
	}

	/*
	 * code唯一性判断
	 */
	private boolean checkArtifactUnique(Module module) {
		String sql = "select code,artifact from ec_module";
		String sql2 = "select code,artifact from runtime_module";
		List<Object[]> list = moduleDao.createNativeQuery(sql).list();
		Map<String, String> artifacts = new HashMap<String, String>();
		if(null != list && !list.isEmpty()){
			for(Object[] objects : list){
				if(null != objects[0]){
					artifacts.put(objects[0].toString(), objects[1].toString());
				}
			}
		}
		List<Object[]> list2 = moduleDao.createNativeQuery(sql2).list();
		if(null != list2 && !list2.isEmpty()){
			for(Object[] objects : list2){
				if(null != objects[0]){
					artifacts.put(objects[0].toString(), objects[1].toString());
				}
			}
		}
		boolean existMore = false;
		if(!artifacts.isEmpty()){
			if (StringUtils.isEmpty(module.getCode())) {//新建
				for(Entry<String, String> entry : artifacts.entrySet()){
					if(entry.getValue().equalsIgnoreCase(module.getArtifact())){
						existMore = true;
						break;
					}
				}
			} else { //修改
				for(Entry<String, String> entry : artifacts.entrySet()){
					if(entry.getValue().equalsIgnoreCase(module.getArtifact()) && !entry.getKey().equals(module.getCode())){
						existMore = true;
						break;
					}
				}

			}
		}
		return !existMore;
	}

	@Override
	public List<Module> findAllModules() {
		// return moduleDao.createQuery("from Module where valid = true").list();
		return moduleDao.findByHql("from Module where valid = 1 and (isHide=false or isHide is null) order by category desc");
	}

	@Override
	public List<Module> findAllMsModules(String proto) {
		String condition =" from Module where ";
		if("proto".equals(proto)){
			condition+="  (isProto is null or isProto=false) and ";
		}
		condition+="  valid = true and (isHide=false or isHide is null)  order by category desc ";
		// return moduleDao.createQuery("from Module where valid = true").list();
		return moduleDao.findByHql(condition);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Module getModule(String code) {
		// return moduleDao.findEntityByProperty("code", code);
		Module module = moduleDao.load(code);
		return module;
	}

	@Transactional
	@Override
	public void publishRefMenu(View mainListView, List<View> refViews) {
		MenuInfo mi = menuInfoService.get(mainListView.getCode());
		if (null != mi) {
			for (View view : refViews) {
				this.publishRefMenu(view, mi);
			}
		}
	}

	@Override
	public void publishRefMenu(View refView, MenuInfo menuInfo) {
		if (null != refView && null != refView.getIsPermission() && refView.getIsPermission()) {
			String permissionCode = (null == refView.getPermissionCode()) ? refView.getName() : refView.getPermissionCode();
			String operateCode = refView.getAssModel().getCode() + "_" + permissionCode.trim();
			String name = (null == refView.getRefOperateName()) ? refView.getTitle() : refView.getRefOperateName();
			menuInfoService.publishRefMenuOperate(operateCode, name, refView.getEntity().getCode(), refView.getCode(), refView.getUrl(), refView.getAssModel().getIsConfigSpecial(), menuInfo);
		}
	}

	@Transactional
	@Override
	public void publishListPtMenu(DataGrid dataGrid, MenuInfo menuInfo) {
		if (null != dataGrid && null != dataGrid.getIsPermission() && dataGrid.getIsPermission()) {
			String permissionCode = (null == dataGrid.getPermissionCode()) ? dataGrid.getName() : dataGrid.getPermissionCode();
			String operateCode = dataGrid.getTargetModel().getCode() + "_" + permissionCode.trim();
			String name = (null == dataGrid.getOperateName()) ? dataGrid.getView().getTitle() : dataGrid.getOperateName();
			menuInfoService.publishRefMenuOperate(operateCode, name, dataGrid.getTargetModel().getEntity().getCode(), dataGrid.getView().getCode(), null, dataGrid.getView().getAssModel().getIsConfigSpecial(),  menuInfo);
		}
	}

	private List<ButtonInfo> generateButtonInfo(View view) {
		List<ButtonInfo> list = new ArrayList<ButtonInfo>();
		List<Button> buttons = buttonService.getButtons(view.getCode());
		for (Button button : buttons) {
			if (null != button.getOperateType() && button.getOperateType() != OperateType.SEPARATE) {
				ButtonInfo bi = null;
				String buttonId = null;
				String buttonCode = null;
				String url = null;
				String iconCls = null;
				String buttonName = null;
				String actionName = null;
				String viewSelect = null;
				String nameSapce = null;
				buttonId = (String) button.getName();
				iconCls = (String) button.getButtonStyle();
				if(button.getButtonOperationCode()!=null){
					buttonCode=button.getButtonOperationCode();
				}else{
					if (null != iconCls) {
						buttonCode = view.getName() + "_" + buttonId + "_" + iconCls + "_" + view.getCode();
					} else {
						buttonCode = view.getName() + "_" + buttonId + "_" + view.getCode();
					}
					button.setButtonOperationCode(buttonCode);
					buttonDao.save(button);
					buttonDao.flush();
				}
				StringBuilder sb = new StringBuilder();
				if (null != button.getIsPermission() && button.getIsPermission()) {
					bi = new ButtonInfo();
					if (button.getOperateType() == OperateType.CUSTOM) {
						url = (String) button.getOperateUrl();
						if (null != url) {
							nameSapce = url.substring(0, url.lastIndexOf("/"));
							int endIndex = url.lastIndexOf(".");
							if(endIndex > 0){
								actionName = url.substring(url.lastIndexOf("/") + 1, endIndex);
							}else{
								actionName = url.substring(url.lastIndexOf("/") + 1);
							}
						}
					} else {
						Boolean flag = false;
						if (button.getOperateType() == OperateType.ADD || button.getOperateType() == OperateType.MODIFY) {
							if (null != button.getViewSelect() && button.getViewSelect().getCode().length() > 0)
								viewSelect = button.getViewSelect().getCode();
							actionName = viewService.getView(viewSelect).getName();
							url = viewService.getView(viewSelect).getUrl();
							nameSapce = url.substring(0, url.lastIndexOf("/"));
						} else if (button.getOperateType() == OperateType.MOVE) {
							actionName = "move";
							sb.append("/");
							sb.append(view.getAssModel().getEntity().getModule().getArtifact());
							sb.append("/");
							sb.append(view.getAssModel().getEntity().getEntityName());
							sb.append("/");
							sb.append(view.getAssModel().getModelName().substring(0, 1).toLowerCase() + view.getAssModel().getModelName().substring(1));
							sb.append("/");
							sb.append(view.getName()).append("Drag");
							flag = true;
						} else if (button.getOperateType() == OperateType.IMPORT) {
							actionName = "import";
							sb.append("/");
							sb.append(view.getAssModel().getEntity().getModule().getArtifact());
							sb.append("/");
							sb.append(view.getAssModel().getEntity().getEntityName());
							sb.append("/");
							sb.append(view.getAssModel().getModelName().substring(0, 1).toLowerCase() + view.getAssModel().getModelName().substring(1));
							sb.append("/");
							sb.append(view.getName()).append("ImportExcel");
							flag = true;
						} else {
							actionName = "delete";
							sb.append("/");
							sb.append(view.getAssModel().getEntity().getModule().getArtifact());
							sb.append("/");
							sb.append(view.getAssModel().getEntity().getEntityName());
							sb.append("/");
							sb.append(view.getAssModel().getModelName().toLowerCase());
							sb.append("/");
							sb.append(actionName);
							nameSapce = sb.substring(0, sb.lastIndexOf("/"));
							flag = true;
						}
						// 微服务模块url不需要.action
						if (flag) {
							if (StringUtils.isEmpty(view.getEntity().getModule().getType()) && !MIS.equals(view.getEntity().getModule().getType())) {
								sb.append(".action");
							}else{
								sb.insert(0, "/msService");
							}
							url = sb.toString();
						}
					}
					buttonName = (String) button.getDisplayName();
					bi.setCode(buttonCode);
					bi.setName(buttonName);
					bi.setIconCls(iconCls);
					bi.setUrl(url);
					bi.setAction(actionName);
					bi.setNameSpace(nameSapce);
					String entityCode = view.getEntity().getCode();
					String viewCode = view.getCode();
					bi.setViewCode(viewCode);
					bi.setEntityCode(entityCode);
					// buttonInfoService.save(bi);
					list.add(bi);
					button.setIsPublished(true);
					buttonService.saveButton(button);
				}
			}
		}

		// 批量控件打印设置和批量控件打印按钮
		if (view.getType() == ViewType.LIST && view.getIsBatchControlPrint() != null
				&& view.getIsBatchControlPrint()) {

			// # BAP-XA-DBZY zhanghd 2017-08-18 start comments
			// 列表页面的打印模板设定在【工程期配置（本期功能）、实体配置中设定（二期功能）】
			// ButtonInfo bcsBtn = new ButtonInfo();
			// bcsBtn.setName("ec.view.batchControlPrintSet");
			// bcsBtn.setCode(view.getCode() + "_batch_controlPrintSet");
			// bcsBtn.setIconCls("dysz");
			// bcsBtn.setNameSpace(view.getUrl().substring(0, view.getUrl().lastIndexOf("/")));
			// bcsBtn.setAction("loadBatchXmlRefer.action");
			// bcsBtn.setUrl(bcsBtn.getNameSpace() + "/" + bcsBtn.getAction());
			// bcsBtn.setViewCode(view.getCode());
			// bcsBtn.setEntityCode(view.getEntity().getCode());
			// list.add(bcsBtn);
			// # BAP-XA-DBZY zhanghd 2017-08-18 end comments

			ButtonInfo bcBtn = new ButtonInfo();
			bcBtn.setName("ec.view.batchControlPrint");
			bcBtn.setCode(view.getCode() + "_batch_controlPrint");
			bcBtn.setIconCls("dy");
			bcBtn.setNameSpace(view.getUrl().substring(0, view.getUrl().lastIndexOf("/")));
			if (MIS.equals(view.getEntity().getModule().getType())) {
				bcBtn.setAction("loadBatchXmlRefAndData");
			} else {
				bcBtn.setAction("loadBatchXmlRefAndData.action");
			}
			bcBtn.setUrl(bcBtn.getNameSpace() + "/" + bcBtn.getAction());
			bcBtn.setViewCode(view.getCode());
			bcBtn.setEntityCode(view.getEntity().getCode());
			list.add(bcBtn);
		} else {
			// # BAP-XA-DBZY zhanghd 2017-08-18 start add
			// 如果列表中没有选择批量打印删除相关数据
			Entity entity = view.getEntity();
			MenuOperate mpPrintSet = menuOperateService.getFlowList(entity.getCode());
			if (null != mpPrintSet) {
				MenuOperate newOp = new MenuOperate();
				List<MenuOperate> checkList = menuOperateService.getByCode(
						view.getCode() + "_batch_controlPrintSet", mpPrintSet.getCid());
				if (null != checkList && checkList.size() > 0) {
					newOp = (MenuOperate) checkList.get(0);
					// reason:打印模板在工程期配置、不在也业务模块中配置
					menuOperateService.delete(newOp.getId());
				}
			}
			buttonService.deleteButton(view.getCode() + "_batch_controlPrintSet");
			MenuOperate mpPrint = menuOperateService.getFlowList(entity.getCode());
			if (null != mpPrint) {
				MenuOperate newOp = new MenuOperate();
				List<MenuOperate> checkList = menuOperateService.getByCode(
						view.getCode() + "_batch_controlPrint", mpPrint.getCid());
				if (null != checkList && checkList.size() > 0) {
					newOp = (MenuOperate) checkList.get(0);
					// reason:打印模板在工程期配置、不在也业务模块中配置
					menuOperateService.delete(newOp.getId());
				}
			}
			buttonService.deleteButton(view.getCode() + "_batch_controlPrint");
			// # BAP-XA-DBZY zhanghd 2017-08-18 end add
		}
		return list;
	}

	private List<ButtonInfo> generateButtonInfo(View view, List<DataGrid> dataGrids) {
		List<ButtonInfo> list = new ArrayList<ButtonInfo>();
		List<Button> buttons = new ArrayList<Button>();
		for(DataGrid dataGrid : dataGrids){
			if(dataGrid.getDataGridType() == 1){
				buttons.addAll(buttonService.getButtonsByDataGridCode(dataGrid.getCode()));
			}
		}
		for (Button button : buttons) {
			if (null != button.getOperateType() && button.getOperateType() != OperateType.SEPARATE) {
				ButtonInfo bi = null;
				String buttonId = null;
				String buttonCode = null;
				String url = null;
				String iconCls = null;
				String buttonName = null;
				String actionName = null;
				String viewSelect = null;
				String nameSapce = null;
				buttonId = (String) button.getName();
				iconCls = (String) button.getButtonStyle();
				if(button.getButtonOperationCode()!=null){
					buttonCode=button.getButtonOperationCode();
				}else{
					if (null != iconCls) {
						buttonCode = view.getName() + "_" + buttonId + "_" + iconCls + "_" + view.getCode();
					} else {
						buttonCode = view.getName() + "_" + buttonId + "_" + view.getCode();
					}
					button.setButtonOperationCode(buttonCode);
					buttonDao.save(button);
					buttonDao.flush();
				}
				StringBuilder sb = new StringBuilder();
				if (null != button.getIsPermission() && button.getIsPermission()) {
					bi = new ButtonInfo();
					if (button.getOperateType() == OperateType.CUSTOM) {
						url = (String) button.getOperateUrl();
						if (null != url) {
							nameSapce = url.substring(0, url.lastIndexOf("/"));
							int endIndex = url.lastIndexOf(".");
							if(endIndex > 0){
								actionName = url.substring(url.lastIndexOf("/") + 1, endIndex);
							}else{
								actionName = url.substring(url.lastIndexOf("/") + 1);
							}
						}
					} else {
						Boolean flag = false;
						if (button.getOperateType() == OperateType.ADD || button.getOperateType() == OperateType.MODIFY) {
							if (null != button.getViewSelect() && button.getViewSelect().getCode().length() > 0)
								viewSelect = button.getViewSelect().getCode();
							actionName = viewService.getView(viewSelect).getName();
							url = viewService.getView(viewSelect).getUrl();
							nameSapce = url.substring(0, url.lastIndexOf("/"));
						} else if (button.getOperateType() == OperateType.MOVE) {
							actionName = "move";
							sb.append("/");
							sb.append(view.getAssModel().getEntity().getModule().getArtifact());
							sb.append("/");
							sb.append(view.getAssModel().getEntity().getEntityName());
							sb.append("/");
							sb.append(view.getAssModel().getModelName().substring(0, 1).toLowerCase() + view.getAssModel().getModelName().substring(1));
							sb.append("/");
							sb.append(view.getName()).append("Drag");
						} else if (button.getOperateType() == OperateType.IMPORT) {
							actionName = "import";
							sb.append("/");
							sb.append(view.getAssModel().getEntity().getModule().getArtifact());
							sb.append("/");
							sb.append(view.getAssModel().getEntity().getEntityName());
							sb.append("/");
							sb.append(view.getAssModel().getModelName().substring(0, 1).toLowerCase() + view.getAssModel().getModelName().substring(1));
							sb.append("/");
							sb.append(view.getName()).append("ImportExcel");
						} else {
							actionName = "delete";
							sb.append("/");
							sb.append(view.getAssModel().getEntity().getModule().getArtifact());
							sb.append("/");
							sb.append(view.getAssModel().getEntity().getEntityName());
							sb.append("/");
							sb.append(view.getAssModel().getModelName().toLowerCase());
							sb.append("/");
							sb.append(actionName);
							nameSapce = sb.substring(0, sb.lastIndexOf("/"));
						}
						// 微服务模块url不需要.action
						if (flag) {
							if (StringUtils.isEmpty(view.getEntity().getModule().getType()) && !MIS.equals(view.getEntity().getModule().getType())) {
								sb.append(".action");
							}
							url = sb.toString();
						}
					}
					buttonName = (String) button.getDisplayName();
					bi.setCode(buttonCode);
					bi.setName(buttonName);
					bi.setIconCls(iconCls);
					bi.setUrl(url);
					bi.setAction(actionName);
					bi.setNameSpace(nameSapce);
					String entityCode = view.getEntity().getCode();
					String viewCode = view.getCode();
					bi.setViewCode(viewCode);
					bi.setEntityCode(entityCode);
					// buttonInfoService.save(bi);
					list.add(bi);
					button.setIsPublished(true);
					buttonService.saveButton(button);
				}
			}
		}

		return list;
	}

    @Override
    @Transactional
    public void publishMenu(View listView, Long parentMenuId, String menuName) {
        Entity entity = listView.getEntity();

        String menuCode = listView.getCode();
        //IMenuInfo mi = menuInfoService.get(menuCode);
        MenuInfo mi = new MenuInfo();
        if (listView.getMobile()) {
            mi = (MenuInfo) menuInfoService.get(menuCode.replace("__mobile__", ""));
        } else {
            mi = (MenuInfo) menuInfoService.get(menuCode);
        }
        List<DataGrid> dataGrids = null;
        // List<ButtonInfo> buttonList = viewService.findButtonOperate(listView.getCode());
        List<ButtonInfo> buttonList = new ArrayList<ButtonInfo>();
        if (listView.getShowType() == ShowType.LAYOUT || listView.getShowType() == ShowType.LAYOUT2) {
            ExtraView ev = listView.getExtraView();
            if (null != ev && ev.getConfig() != null && ev.getConfig().length() > 0) {
                Map configMap = ev.getConfigMap();
                Map<String, Map> layout = (Map<String, Map>) configMap.get("layout");
                if (null != layout && !layout.isEmpty()) {
                    List<ButtonInfo> listButtonList = null;
                    List<ButtonInfo> treeButtonList = null;
                    for (Entry<String, Map> part : layout.entrySet()) {
                        Map<String, String> partElements = part.getValue();
                        if (null != partElements) {
                            if ("tree".equals(partElements.get("ctype"))) {
                                String viewCode = partElements.get("treeView");
                                if (viewCode != null) {
                                    View partView = viewService.getView(viewCode);
                                    // treeButtonList = viewService.findButtonOperate(partView.getCode());
                                    treeButtonList = this.generateButtonInfo(partView);
                                }
                            }

                            for (Entry<String, String> partElement : partElements.entrySet()) {
                                if (partElement.getKey().equals("vcode")) {
                                    if (null != partElement.getValue()) {
                                        View partView = viewService.getView(partElement.getValue());
                                        String name = partView.getName();
                                        String viewCode = partView.getCode();
                                        boolean isShadowView = partView.getIsShadow();
                                        for (; partView.getIsShadow(); partView = partView.getShadowView())
                                            ;
                                        // listButtonList = viewService.findButtonOperate(partView.getCode());
                                        listButtonList = this.generateButtonInfo(partView);
                                        if (isShadowView) {
                                            for (ButtonInfo button : listButtonList) {
                                                String buttonCode = button.getCode();
                                                buttonCode = buttonCode.substring(buttonCode.indexOf("_"), buttonCode.lastIndexOf("_") + 1);
                                                button.setCode(name + buttonCode + name);
                                                button.setViewCode(viewCode);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (null != listButtonList && !listButtonList.isEmpty()) {
                        buttonList.addAll(listButtonList);
                    }
                    if (null != treeButtonList && !treeButtonList.isEmpty()) {
                        buttonList.addAll(treeButtonList);
                    }
                    if (buttonList.size() > 0) { // 按钮的code需拼上布局视图的code
                        for (ButtonInfo bi : buttonList) {
                            bi.setCode(listView.getCode() + "_" + bi.getCode());
                        }
                    }
                }

            }
        } else if (listView.getType() == ViewType.EXTRA) {        //无框架增强型视图的菜单发布单独处理
            dataGrids = dataGridService.findDataGrids(Restrictions.eq("view.code", listView.getCode()), Restrictions.eq("valid", true), Restrictions.eq("dataGridType", 1));
            List<ButtonInfo> list = generateButtonInfo(listView, dataGrids);
            if (list != null && !list.isEmpty()) {
                buttonList.addAll(list);
            }
        } else {
            List<ButtonInfo> list = generateButtonInfo(listView);
            if (list != null && !list.isEmpty()) {
                buttonList.addAll(list);
            }
        }
        Module module = entity.getModule();
        Map<String, Object> menuInfoMap = new HashMap<String, Object>();
        List<Map<String, Object>> operateMap = new ArrayList<Map<String, Object>>();
        // 菜单信息组织
        if (null != mi) {
            menuInfoMap.put("id", mi.getId());
        }

        menuInfoMap.put("parentId", parentMenuId);
        menuInfoMap.put("name", menuName);
        menuInfoMap.put("moduleCode", module.getCode());
        if (listView.getMobile()) {
            menuInfoMap.put("code", menuCode.replace("__mobile__", ""));
        } else {
            menuInfoMap.put("code", menuCode);
        }
        // menuInfoMap.put("action",view.getName());
        if (listView.getCustomFlag()) {
            if (null != listView.getUrl()) {
                menuInfoMap.put("url", listView.getUrl());
                if (listView.getUrl().indexOf("/") > -1) {
                    menuInfoMap.put("namespace", listView.getUrl().substring(0, listView.getUrl().lastIndexOf("/")));
                }
            }
        } else if (!listView.getCustomFlag() && null != listView.getAssModel()) {
            if (MIS.equals(entity.getModule().getType())) {
                menuInfoMap.put("url",
                        "/msService/" + module.getArtifact() + "/" + entity.getEntityName() + "/" + StringUtils.firstLetterToLower(listView.getAssModel().getModelName())
                                + "/" + listView.getName().replace("__mobile__", ""));
            } else {
                menuInfoMap.put("url",
                        "/" + module.getArtifact() + "/" + entity.getEntityName() + "/" + StringUtils.firstLetterToLower(listView.getAssModel().getModelName())
                                + "/" + listView.getName().replace("__mobile__", "") + ".action");
            }
            menuInfoMap.put("namespace",
                    "/" + module.getArtifact() + "/" + entity.getEntityName() + "/" + StringUtils.firstLetterToLower(listView.getAssModel().getModelName()));
        }
        menuInfoMap.put("entityCode", entity.getCode());
        Long companyId = getCurrentCompanyId();
        menuInfoMap.put("companyId", companyId);
        // 菜单操作组织
        for (ButtonInfo button : buttonList) {
            Map<String, Object> opMap = new HashMap<String, Object>();
            opMap.put("action", button.getAction());
            opMap.put("iconCls", "cui-btn-" + button.getIconCls());
            opMap.put("name", button.getName());
            opMap.put("nameSpace", button.getNameSpace());
            opMap.put("url", button.getUrl());
            opMap.put("entityCode", button.getEntityCode());
            opMap.put("viewCode", button.getViewCode());
            opMap.put("opCode", button.getCode());
            operateMap.add(opMap);
        }

        this.publishMenuInfo(menuInfoMap, operateMap, listView, dataGrids);
    }

    @Transactional
    public void publishMenuInfo(Map<String, Object> menuInfoMap, List<Map<String, Object>> operateMap, View view, List<DataGrid> dataGrids) {
        MenuInfo menuInfo;
        MenuInfo parent = null;
        String entityCode = "";
        if (menuInfoMap.get("entityCode") != null) {
            entityCode = menuInfoMap.get("entityCode").toString();
        }
        if (menuInfoMap.get("id") != null) {
            menuInfo = menuInfoService.load(Long.valueOf(menuInfoMap.get("id").toString()));
        } else {
            menuInfo = new MenuInfo();
//			menuInfo.setId(IDGenerator.newInstance().generate().longValue());
        }
        if (menuInfoMap.get("name") != null) {
            menuInfo.setName(menuInfoMap.get("name").toString());
        }
        if (menuInfoMap.get("moduleCode") != null){
			menuInfo.setModuleCode(menuInfoMap.get("moduleCode").toString());
		}
        String layRec = "", fullPathName = "";
        if (menuInfoMap.get("parentId") != null) {
            Long parentId = Long.valueOf(menuInfoMap.get("parentId").toString());
            parent = menuInfoService.load(parentId);
            parent.setLeaf(false);
            // 如果上次菜单的cssClass为空，则将上级菜单的cssClass样式更改为"icon-folder"
            String cssClass = parent.getCssClass();
            if (!"icon-folder".equals(cssClass)) {
                parent.setCssClass("icon-folder");
            }
            menuInfoService.save(parent);
            menuInfo.setParentId(parentId);
            menuInfo.setParentCode(parent.getCode());
            MenuInfo m = parent;
            layRec = m.getLayRec();
            fullPathName = m.getFullPathName();
            Integer layNo = m.getLayNo();
            if (null != layNo) {
                menuInfo.setLayNo(layNo + 1);
            }
        } else {
            menuInfo.setLayNo(1);
        }
        if (menuInfoMap.get("code") != null) {
            menuInfo.setCode(menuInfoMap.get("code").toString());
        }
        if (menuInfoMap.get("action") != null) {
            menuInfo.setAction(menuInfoMap.get("action").toString());
        }
        if (menuInfoMap.get("url") != null) {
            menuInfo.setUrl(menuInfoMap.get("url").toString());
        }
        if (menuInfoMap.get("namespace") != null) {
            menuInfo.setNamespace(menuInfoMap.get("namespace").toString());
        }
        if (menuInfoMap.get("entityCode") != null) {
            menuInfo.setEcEntityCode(menuInfoMap.get("entityCode").toString());
            menuInfo.setEntityCode(menuInfoMap.get("entityCode").toString());
        }
        if (!"".equals(entityCode)) {
            menuInfo.setEntityCode(entityCode);
        }
        if (menuInfoMap.get("companyId") != null) {
            menuInfo.setCid(Long.valueOf(menuInfoMap.get("companyId").toString()));
        }
        if (menuInfoMap.get("moduleCode") != null) {
            menuInfo.setModuleCode(menuInfoMap.get("moduleCode").toString());
        }
        menuInfo.setLeaf(true);
        // 将菜单的cssClass设置为"icon-set"
        menuInfo.setCssClass("icon-set");
        if (menuInfo.getId() == null) {
            String menuHql = "from MenuInfo mo where mo.valid=true and mo.code=?";
            MenuInfo checkList = menuInfoService.get(menuInfo.getCode());
            if (checkList != null) {
                throw new EcException(EcException.Code.UNIQUECODE);
            }
        }
        menuInfoService.save(menuInfo);

        Long id = menuInfo.getId();
        String newLayRec = layRec + "-" + id.toString();
        String newFullPathName = fullPathName + "/" + OrchidUtils.getMainDisplayValue(menuInfo);
        if (null == menuInfo.getParentId() || menuInfo.getParentId() == -1L) {
            newLayRec = id.toString();
            newFullPathName = OrchidUtils.getMainDisplayValue(menuInfo);
            menuInfo.setLayNo(1);
        }
        menuInfo.setLayRec(newLayRec);
//        menuInfo.setFullPathName(newFullPathName);
        menuInfo.setLayNo(newLayRec.split("-").length);
        menuInfoService.save(menuInfo);
//List<MenuOperate> list = menuOperateService.findMenuOperates(Restrictions.eq("valid", true), Restrictions.eq("menuInfo.id", menuInfo.getId()), Restrictions.eq("code", menuInfo.getCode() + "_self"));
        // 根据code和menuInfoID获取
        List<MenuOperate> list =
                menuOperateService.findMenuOperatesByCodeOrMenuInfoId(menuInfo.getCode() + "_self", menuInfo.getId());

        MenuOperate menuOperate = null;
        if (list != null && list.size() > 0) {
            menuOperate = list.get(0);
        } else {
            menuOperate = new MenuOperate();
        }
        menuOperate.setName("foundation.common.query");

        menuOperate.setEntityCode(menuInfo.getEntityCode());

        if (view.getUsedForWorkFlow() != null && view.getUsedForWorkFlow()) {
            menuOperate.setPowerFlag(true);
            menuOperateService.updateOtherMenuOperate(menuOperate);
        } else {
            menuOperate.setPowerFlag(false);
        }


        menuOperate.setMenuInfo(menuInfo);
        menuOperate.setCid(menuInfo.getCid());
        //主列表视图对应的操作
        menuOperate.setCode(menuInfo.getCode() + "_self");
        String url = menuInfo.getUrl();
        String action = url;
        String actionStr = url;
        String[] arrs = url.split("/");
        if (arrs.length > 1) {
            actionStr = arrs[arrs.length - 1];
        }
        String[] a = action.split(",");
        action = actionStr;
        if (a.length > 1) {
            action = a[0];
        }
        menuOperate.setAction(action);
        menuOperate.setNamespace(menuInfo.getNamespace());
        menuOperate.setUrl(menuInfo.getUrl());
        menuOperate.setTarget(OperateTarget.SELF);
        menuOperate.setValid(true);
        menuOperate.setCid(menuInfo.getCid());
        menuOperate.setIsQuery(true);
        menuOperate.setViewCode(view.getCode().replace("__mobile__", ""));
        if (menuOperate.getId() == null) {
            menuOperate.setEnableAssignPos(true);
            menuOperate.setEnableAssignStaff(true);
            menuOperate.setEnableGroupRestrict(false);
            menuOperate.setEnableNoRestrict(true);
            menuOperate.setEnablePosRestrict(true);
            menuOperate.setForDataPermission(false);
            if (view.getEntity().getWorkflowEnabled() != null && view.getEntity().getWorkflowEnabled()) {
                menuOperate.setEnableDealerPermission(true);
            }

        }
        //设置其他限制
        if (menuOperateService.checkWhetherIsConfiged(view.getCode())) {
            menuOperate.setEnableOtherRestrict(true);
        } else {
            menuOperate.setEnableOtherRestrict(false);
        }

        //设置特殊资源标志位
        if (view.getAssModel().getIsConfigSpecial()) {
            menuOperate.setEnableSpecialPermission(true);
        } else {
            menuOperate.setEnableSpecialPermission(false);
        }

        menuOperateService.save(menuOperate);
        //configOtherRestrictInit(menuInfo,view);

        List<String> opCodes = new ArrayList<String>();//视图中所有的权限code
        opCodes.add(menuOperate.getCode());
        List<String> buttonOperationCodeList = new ArrayList<String>();
        if (view.getShowType() != ShowType.LAYOUT && view.getShowType() != ShowType.LAYOUT2) {
            Map cmap = null;
            /*if (null != view.getExtraView()) {
                cmap = view.getExtraView().getConfigMap();
            }*/
			if(view.getIsShadow()){
				if(null !=view.getShadowView().getExtraView()){
					cmap = view.getShadowView().getExtraView().getConfigMap();
				}else{
					throw new EcException(InternationalResource.get("ec.view.unconfig"));
				}

			}else{
				if(null !=view.getExtraView()){
					cmap = view.getExtraView().getConfigMap();
				}else{
					throw new EcException(InternationalResource.get("ec.view.unconfig"));
				}
			}
            if (null != cmap && null != cmap.get("layout")) {
                Map l = (Map) cmap.get("layout");
                List<Map> sections = (List) l.get("sections");
                if (null != sections && !sections.isEmpty()) {
                    for (Map<String, Object> section : sections) {
                        if (null != section.get("regionType") && section.get("regionType").toString().equals("BUTTON")) {
                            List<Map<String, Object>> cells = (List<Map<String, Object>>) section.get("cells");
                            for (Map<String, Object> cell : cells) {
                                if (cell.get("buttonoperationcode") != null) {
                                    buttonOperationCodeList.add((String) cell.get("buttonoperationcode"));
                                }
                            }
                        }
                    }
                }
            }
        } else if (view.getShowType() == ShowType.LAYOUT || view.getShowType() == ShowType.LAYOUT2) {
            Map cmap = view.getExtraView().getConfigMap();
            if (null != cmap && null != cmap.get("layout")) {
                Map l = (Map) cmap.get("layout");
                // 列表
                Map center = (Map) l.get("center");
                if (null != center && null != center.get("vcode")) {
                    String viewCode = (String) center.get("vcode");
                    if (null != viewService.getView(viewCode)) {
                        Map cmaplayout = viewService.getView(viewCode).getExtraView().getConfigMap();
                        if (null != cmaplayout && null != cmaplayout.get("layout")) {
                            Map layout = (Map) cmaplayout.get("layout");
                            List<Map> sections = (List) layout.get("sections");
                            if (null != sections && !sections.isEmpty()) {
                                for (Map<String, Object> section : sections) {
                                    if (null != section.get("regionType") && section.get("regionType").toString().equals("BUTTON")) {
                                        List<Map<String, Object>> cells = (List<Map<String, Object>>) section.get("cells");
                                        for (Map<String, Object> cell : cells) {
                                            if (cell.get("buttonoperationcode") != null) {
                                                buttonOperationCodeList.add((String) cell.get("buttonoperationcode"));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // 树
                Map west = (Map) l.get("west");
                if (null != west && null != west.get("treeView") && "tree".equals(west.get("ctype"))) {
                    String viewCode = (String) west.get("treeView");
                    if (null != viewService.getView(viewCode)) {
                        Map cmaplayout = viewService.getView(viewCode).getExtraView().getConfigMap();
                        if (null != cmaplayout && null != cmaplayout.get("layout")) {
                            Map layout = (Map) cmaplayout.get("layout");
                            List<Map> sections = (List) layout.get("sections");
                            if (null != sections && !sections.isEmpty()) {
                                for (Map<String, Object> section : sections) {
                                    if (null != section.get("regionType") && section.get("regionType").toString().equals("BUTTON")) {
                                        List<Map<String, Object>> cells = (List<Map<String, Object>>) section.get("cells");
                                        for (Map<String, Object> cell : cells) {
                                            if (cell.get("buttonoperationcode") != null) {
                                                buttonOperationCodeList.add((String) cell.get("buttonoperationcode"));
                                            } else if (cell.get("cellCode") != null){
												List<Button> buttons = buttonService.findButtons(Restrictions.eq("cellCode", (String) cell.get("cellCode")), Restrictions.eq("valid", true), Restrictions.isNotNull("buttonOperationCode"));
												for (Button button : buttons) {
													buttonOperationCodeList.add(button.getButtonOperationCode());
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
        }

        // 插入按钮操作
        for (Map<String, Object> opMap : operateMap) {
            boolean operateboolean = false;
            if (buttonOperationCodeList != null) {
                for (String buttoncode : buttonOperationCodeList) {
                    if (opMap != null && (((String) opMap.get("opCode")).contains(buttoncode) || ((String) opMap.get("opCode")).contains("_batch_controlPrint") || ((String) opMap.get("opCode")).contains("_retrial"))) {
                        operateboolean = true;
                        break;
                    }
                }
            }
            action = (opMap.get("action") != null) ? opMap.get("action").toString() : "";
            String opCode = (opMap.get("opCode") != null) ? opMap.get("opCode").toString() : "";
            String iconCls = (opMap.get("iconCls") != null) ? opMap.get("iconCls").toString() : "";
            String name = (opMap.get("name") != null) ? opMap.get("name").toString() : "";
            String nameSpace = (opMap.get("nameSpace") != null) ? opMap.get("nameSpace").toString() : "";
            url = (opMap.get("url") != null) ? opMap.get("url").toString() : "";

            String opEntityCode = (opMap.get("entityCode") != null) ? opMap.get("entityCode").toString() : "";
            if (operateboolean || (buttonOperationCodeList != null && buttonOperationCodeList.size() == 0) || buttonOperationCodeList == null) {
                // Long
                // viewId=(opMap.get("viewId")!=null&&!opMap.get("viewId").toString().equals(""))?Long.valueOf(opMap.get("viewId").toString()):-1L;
                String opHql = "from MenuOperate mo where mo.menuInfo=? and mo.valid=true and mo.code=?";
//                List<MenuOperate> opList = menuOperateService.findMenuOperates(Restrictions.eq("valid", true), Restrictions.eq("menuInfo.id", menuInfo.getId()), Restrictions.eq("code", opCode));
                // 根据code和menuInfoID获取
                List<MenuOperate> opList = menuOperateService.findMenuOperatesByCodeOrMenuInfoId(opCode, menuInfo.getId());
                if (!CollectionUtils.isEmpty(opList)) {
                    menuOperate = menuOperateService.load(opList.get(0).getId());
                } else {
                    menuOperate = new MenuOperate();
                }
                menuOperate.setAction(action);
                menuOperate.setCid(menuInfo.getCid());
                menuOperate.setCode(opCode);
                menuOperate.setEntityCode(opEntityCode);
                menuOperate.setNamespace(nameSpace);
                menuOperate.setUrl(url);
                menuOperate.setName(name);
                menuOperate.setIconCls(iconCls);
                menuOperate.setViewCode(view.getCode().replace("__mobile__", ""));
                menuOperate.setIsQuery(false);
                menuOperate.setValid(true);
                menuOperate.setTarget(OperateTarget.SELF);
                menuOperate.setMenuInfo(menuInfo);
                menuOperate.setForDataPermission(false);
                // if (null == powerOperate) {
                // menuOperate.setMenuInfo(menuInfo);
                // } else {
                // MenuInfo mm = (MenuInfo) powerOperate.getMenuInfo();
                // menuOperate.setMenuInfo(mm);
                // }
                menuOperateService.save(menuOperate);
                opCodes.add(opCode);
            }
        }
        if (null != dataGrids) {
            for (DataGrid dataGrid : dataGrids) {
                publishRefMenuOperate(dataGrid, menuInfo);
                String permissionCode = (null == dataGrid.getPermissionCode()) ? dataGrid.getName() : dataGrid.getPermissionCode();
                String operateCode = dataGrid.getTargetModel().getCode() + "_" + permissionCode.trim();
                opCodes.add(operateCode);
            }
        }

        //删除视图中没有的权限
        List<MenuOperate> allOps = menuOperateService.getAllOperateByMenu(menuInfo, getCurrentCompany());
         List<String> opCodesInDB = new ArrayList<String>();//不包括工作流的权限和控件打印权限
        for (MenuOperate op : allOps) {
            if (op.getMenuOperateType() != MenuOperateType.FLOWOPERATE && op.getMenuOperateType() != MenuOperateType.ACTIVEOPERATE
                    && op.getCode() != null && !op.getCode().endsWith("_controlPrint") && !op.getCode().endsWith("_retrial")) {
                if (((MenuOperate) op).getEntityCode() != null && !((MenuOperate) op).getEntityCode().equals("")) {//通过菜单手工添加的操作不作处理
                    opCodesInDB.add(op.getCode());
                }
            }
        }
        List<String> opCodesNeedDeleted = new ArrayList<String>();
        for (String code : opCodesInDB) {
            if (!opCodes.contains(code)) {
                if (view.getMobile() && code.contains("__mobile__")) {
                    opCodesNeedDeleted.add(code);
                } else if (!view.getMobile() && !code.contains("__mobile__")) {
                    opCodesNeedDeleted.add(code);
                }
            }
        }
        if (opCodesNeedDeleted.size() > 0) {
            for (String code : opCodesNeedDeleted) {
                menuOperateService.deleteMenuOperateByPhysical(code);
            }
        }

    }

    @Transactional
    public void publishRefMenuOperate(DataGrid dataGrid, MenuInfo menuInfo) {
        if (null != dataGrid && null != menuInfo) {
            String permissionCode = (null == dataGrid.getPermissionCode()) ? dataGrid.getName() : dataGrid.getPermissionCode();
            String operateCode = dataGrid.getTargetModel().getCode() + "_" + permissionCode.trim();
            String opHql = "from MenuOperate mo where mo.menuInfo=? and mo.valid=true and mo.code=?";
            List<MenuOperate> opList = menuOperateService.findMenuOperates(Restrictions.eq("valid", true), Restrictions.eq("menuInfo.id", menuInfo.getId()), Restrictions.eq("code", operateCode));
            MenuOperate menuOperate = null;
            if (opList != null && opList.size() > 0) {
                menuOperate = opList.get(0);
            }
            if (null == menuOperate) {
                menuOperate = new MenuOperate();
                menuOperate.setCode(operateCode);
                menuOperate.setEntityCode(dataGrid.getTargetModel().getEntity().getCode());
                menuOperate.setCid(getCurrentCompanyId());
                menuOperate.setTarget(OperateTarget.SELF);
                // menuOperate.setUrl(refView.getOperateUrl());
                menuOperate.setEnableAssignPos(true);
                menuOperate.setEnableAssignStaff(true);
                menuOperate.setEnableGroupRestrict(false);
                menuOperate.setEnableNoRestrict(true);
                menuOperate.setEnablePosRestrict(true);
                menuOperate.setForDataPermission(true);
                menuOperate.setViewCode(dataGrid.getView().getCode());
                menuOperate.setIsQuery(true);
            }
            //设置其他限制
            if (menuOperateService.checkWhetherIsConfiged(dataGrid.getView().getCode())) {
                menuOperate.setEnableOtherRestrict(true);
            } else {
                menuOperate.setEnableOtherRestrict(false);
            }
            //设置特殊资源标志位
            if (dataGrid.getView().getAssModel().getIsConfigSpecial()) {
                menuOperate.setEnableSpecialPermission(true);
            } else {
                menuOperate.setEnableSpecialPermission(false);
            }
            menuOperate.setMenuInfo(menuInfo);
            menuOperate.setName(StringUtils.isEmpty(dataGrid.getOperateName()) ? dataGrid.getView().getTitle() : dataGrid.getOperateName());
            menuOperate.setValid(true);
            menuOperateService.save(menuOperate);
        }
    }

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object getObjectValue(Object o, String name, String val) throws Exception {
		Class type = PropertyUtils.getPropertyType(o, name);
		if (type.isEnum())
			return Enum.valueOf(type, val);
		return ConvertUtils.convert(val, type);
	}

	/**
	 * Attempts to locate the specified field on the class.
	 *
	 * @param clazz
	 *            the class definition containing the field
	 * @param fieldName
	 *            the name of the field to locate
	 *
	 * @return the Field (never null)
	 *
	 * @throws IllegalStateException
	 *             if field could not be found
	 */
	public java.lang.reflect.Field getField(Class<?> clazz, String fieldName) throws IllegalStateException {
		Assert.notNull(clazz, "Class required");
		Assert.hasText(fieldName, "Field name required");

		try {
			return clazz.getDeclaredField(fieldName);
		} catch (NoSuchFieldException nsf) {
			// Try superclass
			if (clazz.getSuperclass() != null) {
				return getField(clazz.getSuperclass(), fieldName);
			}

			throw new IllegalStateException("Could not locate field '" + fieldName + "' on class " + clazz);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void processProperty(Object obj, Element e) throws Exception {
		for (Iterator it = e.elementIterator(); it.hasNext();) {
			Element subE = (Element) it.next();
			String propertyName = subE.getName();
			if (subE.elements().isEmpty()) {
				if (subE.getTextTrim().length() > 0) {
					// 直接放入属性
					PropertyUtils.setProperty(obj, propertyName, getObjectValue(obj, propertyName, subE.getTextTrim()));
				}
			} else {
				// 还有下级节点
				Class propertyType;
				Object property = null;
				try {
					property = PropertyUtils.getProperty(obj, propertyName);
				} catch (NoSuchMethodException ex) {
					continue;
				}
				if (null == property) {
					propertyType = PropertyUtils.getPropertyType(obj, propertyName);
					if (propertyType == Set.class) {
						property = new HashSet();
					} else if (propertyType == List.class) {
						property = new ArrayList();
					} else {
						if (propertyType == MenuInfo.class)
							property = new MenuInfo();
						else
							property = propertyType.newInstance();
					}
				} else {
					propertyType = property.getClass();
				}
				if (property instanceof Collection) {
					java.lang.reflect.Field f = obj.getClass().getDeclaredField(propertyName);
					f.setAccessible(true);
					ParameterizedType pt = (ParameterizedType) f.getGenericType();
					Class gc = ((Class) pt.getActualTypeArguments()[0]);
					for (Object propertyE : subE.elements()) {
						Object p = gc.newInstance();
						processProperty(p, (Element) propertyE);
						((Collection) property).add(p);
					}
				} else {
					processProperty(property, subE);
				}
				PropertyUtils.setProperty(obj, propertyName, property);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void resImportMenuInfo(MenuInfo mi, Element e) throws Exception {
		String code = XmlUtils.getTagContent(e.asXML(), "code");
		MenuInfo parent = (MenuInfo) menuInfoService.get(code);
		if (parent == null) {
			parent = new MenuInfo();
		}
		for (Iterator menuInfoIt = e.elementIterator(); menuInfoIt.hasNext();) {
			Element menuInfoPropertyE = (Element) menuInfoIt.next();
			String menuInfoPropertyName = menuInfoPropertyE.getName();
			if ("parent".equals(menuInfoPropertyName)) {
				if (menuInfoPropertyE.elements().size() > 0) {
					resImportMenuInfo(parent, menuInfoPropertyE);
				}
			} else {
				// MenuInfo简单属性
				String menuInfoPropertyValue = menuInfoPropertyE.getTextTrim();
				if (menuInfoPropertyValue.length() > 0)
					PropertyUtils.setProperty(parent, menuInfoPropertyName, getObjectValue(parent, menuInfoPropertyName, menuInfoPropertyValue));

			}
		}
		if (mi.getCode() != null && !mi.getCode().equals(parent.getCode())) {
			mi.setParent(parent);
		}
	}

	@Transactional(timeout = -1)
	public Module importXml(String xml, Boolean uploadWorkFlow, boolean filter) {
		return bapModuleXmlEcImportService.importXml(xml, uploadWorkFlow, ecSessionFactory, filter, "ec");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.supcon.orchid.entityconf.services.ModuleService#dealMneCodeByModule(java.lang.String)
	 */
	@Override
	public void dealMneCodeByModule(String moduleCode) {
		if (null != moduleCode && moduleCode.length() > 0) {
			Module module = getModule(moduleCode);
			if (module != null) {
				List<Model> models = modelService.findModels(module);
				if (null != models && !models.isEmpty()) {
					for (Model model : models) {
						if (null != model.getIsMneCode() && model.getIsMneCode()) {
							mneCodeDataDealService.generateMneCodeData(model);
						}
					}
				}
			}
		} else {
			return;
		}
	}

	public void saveRelation(ModuleRelation relation) {
		checkModuleRelation(relation.getModule().getCode(), relation.getTarget().getCode(), null, null);
		moduleRelationDao.flush();
		moduleRelationDao.clear();
		moduleRelationDao.save(relation);
	}

	/**
	 * 建立module-->target的关联时，检查是否已经存在target-->module的关联
	 *
	 * @param moduleCode
	 * @param targetCode
	 * @return true 可以建立关联 false 不可以建
	 */
	private boolean checkModuleRelation(String moduleCode, String targetCode, List<ModuleRelation> relationList, Deque<String> stack) {
		if (relationList == null || relationList.size() == 0) {
			relationList = getAllRelation();
			stack = new LinkedList<String>();
			if (relationList != null && relationList.size() > 0) {
				return checkModuleRelation(targetCode, moduleCode, relationList, stack);
			} else {
				return true;
			}
		} else {
			for (ModuleRelation relation : relationList) {
				if (relation.getCode().startsWith(moduleCode)) {
					stack.push(moduleCode);
					if (relation.getCode().endsWith(targetCode)) {
						stack.push(targetCode);
						StringBuffer sb = new StringBuffer();
						while (stack.size() > 0) {
							sb.append(stack.pollLast()).append("-->");
						}
						throw new EcException(EcException.Code.MOUDLE_CYCLE_DEPEND, sb.substring(0, sb.length() - 3));
					}
					checkModuleRelation(relation.getTarget().getCode(), targetCode, relationList, stack);
					stack.pop();
				}
			}
		}
		return true;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<ModuleRelation> getRelations(Module module) {
		return moduleRelationDao.findByHql("From ModuleRelation where module = ?0 and valid = true", module);
	}

	public ModuleRelation getRelation(Module module, Module target) {
		return getRelation(module, target, true);
	}

	public ModuleRelation getRelation(Module module, Module target, boolean valid) {
		List<ModuleRelation> relations = null;
		if (valid) {
			relations = moduleRelationDao.findByHql("From ModuleRelation where module = ? and target = ? and valid = true", module, target);
		} else {
			relations = moduleRelationDao.findByHql("From ModuleRelation where module = ? and target = ?", module, target);
		}
		if (relations != null && relations.size() == 1) {
			return relations.get(0);
		} else {
			return null;
		}
	}

	public ModuleRelation getRelationWithNoValid(Module module, Module target) {
		List<ModuleRelation> relations = moduleRelationDao.findByHql("From ModuleRelation where module = ? and target = ?", module, target);
		if (relations != null && relations.size() == 1) {
			return relations.get(0);
		} else {
			return new ModuleRelation();
		}
	}

	public void deleteRelation(ModuleRelation relation) {
		moduleRelationDao.delete(relation);
	}

	@Override
	public List<ModuleRelation> getBeAssociated(Module target, boolean needValid) {
		if (needValid) {
			return moduleRelationDao.findByHql("From ModuleRelation where target = ?0 and valid = true", target);
		} else {
			return moduleRelationDao.findByHql("From ModuleRelation where target = ?0", target);
		}
	}

	private List<ModuleReference> listModuleReferences(Module target, boolean needValid){
		if (needValid) {
			return moduleReferenceDao.findByHql("From ModuleReference where target = ?0 and valid = true", target);
		} else {
			return moduleReferenceDao.findByHql("From ModuleReference where target = ?0", target);
		}
	}


	public void getMsModuleRel(Module module, Set<String> descSet){
		if("Mis".equals(module.getType())){
			List<String> list = moduleDao.createNativeQuery("SELECT e.NAME FROM EC_MSMODULE e LEFT JOIN EC_MSMODULE_RELATION r ON  e.CODE=r.MSMODULE_CODE WHERE r.code = ?",module.getCode()).list();
			if(null != list && list.size()>0){
				String relation =list.get(0)==null?"":list.get(0).toString();
				if(null!=relation &&!"".equals(relation) ){
					descSet.add(InternationalResource.get("ec.module.deploy.beenintroducted",new Object[]{relation}));
				}
			}
		}
	}
	//同一模块以及依赖的模块只能存在于一个微服务
	public void getMsModuleRelByCode(Module module, String id){
		if("Mis".equals(module.getType())){
			Module idModule =getModule(id);
			//获取服务相关模块
			List<String> list = moduleDao.createNativeQuery(" SELECT name FROM  EC_MSMODULE WHERE code  IN  (SELECT msmodule_code FROM  EC_MSMODULE_RELATION  WHERE code = ? )",module.getCode()).list();
			if(null != list && list.size()>0){
				String moduleName =list.get(0);
				List<String> list1 = moduleDao.createNativeQuery(" SELECT name FROM  EC_MSMODULE WHERE code  IN (SELECT msmodule_code FROM EC_MSMODULE_RELATION WHERE code =?  and MSMODULE_CODE NOT in (SELECT MSMODULE_CODE FROM EC_MSMODULE_RELATION WHERE code =?))",id,module.getCode()).list();
				if(null != list1 && list1.size()>0){
					String reName =list1.get(0);
					throw new EcException(EcException.Code.LEAD_INTO, InternationalResource.get(module.getName()),moduleName, InternationalResource.get(idModule.getName()),reName);
				}else{
					getTargetRel(module,id,moduleName,idModule);
				}
			}
		}
	}
	//引入模块时，模块依赖的模块也要进行一个模块不能同时被两个微服务引用的判断
	public void getTargetRel(Module module, String id, String moduleName, Module idModule){
		Map<String, StringBuffer> modeMap =new HashMap<String, StringBuffer>();
		List<Object[]> list = moduleDao.createNativeQuery("SELECT name,code FROM EC_MODULE where code in (select target_module_code as relcode from EC_MODULE_RELATION   where  MODULE_CODE= ? )",id).list();
		if(null!=list&&list.size()>0) {
			for(Object[] objects : list){
				List<String> list1 = moduleDao.createNativeQuery(" SELECT name FROM  EC_MSMODULE WHERE code  IN (SELECT msmodule_code FROM EC_MSMODULE_RELATION WHERE code =?  and MSMODULE_CODE NOT in (SELECT MSMODULE_CODE FROM EC_MSMODULE_RELATION WHERE code =?))",objects[1].toString(),module.getCode()).list();
				if(null != list1 && list1.size()>0){
					String reName =list1.get(0);
					throw new EcException(EcException.Code.DEPEND_INTO, InternationalResource.get(module.getName()),moduleName, InternationalResource.get(idModule.getName()), InternationalResource.get(objects[0].toString()),reName);
				}
				getTargetRel(module,objects[1].toString(),moduleName,idModule);
			}
		}
	}

	//修改模块依赖时，要同步修改EC_MSMODULE_RELATION
	public void saveMsModuleRelByCode(Module module, String id){
		if("Mis".equals(module.getType())){
			List<String> list = moduleDao.createNativeQuery(" SELECT msmodule_code FROM  EC_MSMODULE_RELATION  WHERE code = ? ",module.getCode()).list();
			List<String> list1 = moduleDao.createNativeQuery(" SELECT msmodule_code FROM  EC_MSMODULE_RELATION  WHERE code = ? ",id).list();
			//模块挂于微服务下
			if(null != list && list.size()>0){
				if(list1.size()<1){
					String msCode =list.get(0);
					moduleDao.createNativeQuery("INSERT INTO  EC_MSMODULE_RELATION (CODE,EC_ENV,VERSION,MSMODULE_CODE,VALID) VALUES (?,'product',0,?,1)",id,msCode).executeUpdate();
				}
			}
		}
	}
	//刪除模块关联
	public void deleteMsRelByCode(Module module, String id){
		if("Mis".equals(module.getType())){
			moduleDao.createNativeQuery("delete from EC_MSMODULE_RELATION where code =?",id).executeUpdate();
		}
	}

	@Override
	public Set<Module> getAllAssociated(Set<Module> target, boolean needValid) {
		List<Module> moduleList = new ArrayList<Module>();
		Set<Module> modules = new HashSet<Module>();
		moduleList = moduleRelationDao.createQuery("Select relation.module From ModuleRelation relation where relation.target in (:target) and relation.valid = (:valid)").setParameterList("target", target).setParameter("valid", needValid).list();
		if(moduleList.size() > 0){
			modules.addAll(moduleList);
			getAllAssociated(modules,needValid);
			target.addAll(modules);
		}
		return target;
	}

	public List<Module> getAllAssociated(Module target, boolean needValid) {
		Set<Module> modules = new HashSet<Module>();
		modules.add(target);
		modules = getAllAssociated(modules,needValid);
		modules.remove(target);
		return new ArrayList<Module>(modules);
	}

	public List<ModuleRelation> getAssociated(Module module, boolean needValid) {
		if (needValid) {
			return moduleRelationDao.findByHql("From ModuleRelation where module = ? and valid = true", module);
		} else {
			return moduleRelationDao.findByHql("From ModuleRelation where module = ?", module);
		}
	}

	public void deleteRelationPhysical(ModuleRelation relation) {
		moduleRelationDao.deletePhysical(relation);
	}

	public List<ModuleRelation> getAllRelation() {
		return moduleRelationDao.findByHql("From ModuleRelation where  valid = true");
	}

	public List<Map<String, String>> getAllRelationForUpload(List<UploadInfo> firstimportUploadInfos) {
		List<Map<String, String>> moduleRelations = new ArrayList<Map<String, String>>();
		for(UploadInfo uploadInfo : firstimportUploadInfos){
			String targetModules = uploadInfo.getRelations();
			// 引用模块
			if (!StringUtils.isEmpty(uploadInfo.getReferences())) {
				targetModules += "," + uploadInfo.getReferences();
			}
			if(targetModules != null && !"".equals(targetModules)){
				for(String module : targetModules.split(",")){
					if (StringUtils.isEmpty(module)) {
						continue;
					}
					Map<String, String> moduleMap = new HashMap<String, String>();
					moduleMap.put("moduleCode", uploadInfo.getModuleCode());
					moduleMap.put("targetModuleCode", module);
					moduleRelations.add(moduleMap);
				}
			}
		}
		return moduleRelations;
	}


	@Override
	public List<Entity> listEntity(Module module, String filePath) throws Exception {
		String uploaded = PropertyHolder.get().getGeneratePath() + File.separator + "uploaded" + File.separator + filePath;
		List<Entity> returnList = new ArrayList<Entity>();
		// 处理module.xml
		File moduleFile = new File(uploaded + File.separator + "service/src/main/resources/META-INF/bap/"
				+ "module.xml");
		if (moduleFile.exists()) {
			String xml =  org.apache.commons.io.FileUtils.readFileToString(moduleFile, "UTF-8");
			if (!StringUtils.isEmpty(xml)) {
				// 未选择模块导入时,如果模块已存在,直接报错；选择模块，导入模块与选择不一致，报错
				Module tempModule = null;

				String code = XmlUtils.getTagContent(xml, "code");
				if(module != null){//选择了模块
					if(!module.getCode().equals(code)){
						throw new EcException(EcException.Code.NOSAME_MODULE_IMPORT);
					}
				}else{
					tempModule = moduleDao.load(code);
				}

				if (tempModule != null) {
					throw new EcException(EcException.Code.MODULE_IMPORTED_EXISTS);
				}

				Document doc = DocumentHelper.parseText(xml);
				Element root = doc.getRootElement();
				for (Iterator moduleIt = root.elementIterator(); moduleIt.hasNext();) {
					Element modulePropertyE = (Element) moduleIt.next();
					String modulePropertyName = modulePropertyE.getName();
					if ("entities".equals(modulePropertyName)) {
						for (Iterator entitiesIt = modulePropertyE.elementIterator("entity"); entitiesIt.hasNext();) {
							Element entityE = (Element) entitiesIt.next();
							Entity entity = new Entity();
							entity.setCode(XmlUtils.getTagContent(entityE.asXML(), "code"));
							entity.setName(XmlUtils.getTagContent(entityE.asXML(), "name"));
							entity.setEntityName(XmlUtils.getTagContent(entityE.asXML(), "entityName"));
							entity.setPrefix(XmlUtils.getTagContent(entityE.asXML(), "prefix"));
							entity.setWorkflowEnabled(XmlUtils.getTagContent(entityE.asXML(), "workflowEnabled").equals("true")?true:false);
							entity.setGroupEnabled(XmlUtils.getTagContent(entityE.asXML(), "crossCompanyFlag").equals("true")?true:false);
							entity.setIsBase(XmlUtils.getTagContent(entityE.asXML(), "isBase").equals("true")?true:false);
							returnList.add(entity);
						}
					}
				}
			}
		}
		return returnList;
	}

	@Override
	public List<Module> getModuleByCatetorys(List<String> catetorys) {
		String hql = " from Module as m where m.type is null and m.category = ?";
		List<Module> module = moduleDao.createQuery(hql).setParameterList("catetorys", catetorys).list();
		return module;

	}

	@Override
	public List<Module> getModuleByCatetory(String catetory, List<Module> modules) {
		List<Module> result = new ArrayList<Module>();
		for(Module m:modules){
			if(null != m.getCategory()&& m.getCategory().equals(catetory)){
				result.add(m);
			}
		}
		return result;

	}

	@Override
	public List<Module> getModuleByCatetorys(List<String> values, List<Module> modules) {
		List<Module> result = new ArrayList<Module>();
		for(Module m:modules){
			for(String catetory:values){
				if(null != m.getCategory()&& m.getCategory().equals(catetory)){
					result.add(m);
					break;
				}
			}
		}
		return result;

	}

	@Override
	@Transactional(propagation= Propagation.NOT_SUPPORTED)
	public String saveFile(File f, String moduleCode) throws Exception {
		String code = "-1";
		String filePath = "up" + System.currentTimeMillis();
		String uploaded = PropertyHolder.get().getGeneratePath() + File.separator + "uploaded" + File.separator + filePath;
		File uploadedFile = new File(uploaded);
		if (!uploadedFile.exists()) {
			uploadedFile.mkdirs();
		}
		UnZipFile.unzip(f, uploadedFile);
		File moduleFile = new File(uploaded + File.separator + "service/src/main/resources/META-INF/bap/"
				+ "module.xml");
		if (moduleFile.exists()) {
			String xml =  org.apache.commons.io.FileUtils.readFileToString(moduleFile, "UTF-8");
			if (!StringUtils.isEmpty(xml)) {
				// 未选择模块导入时,如果模块已存在,直接报错；选择模块，导入模块与选择不一致，报错
				Module tempModule = null;

				code = XmlUtils.getTagContent(xml, "code");
				if(moduleCode != "migrateModule"){//选择了模块
					if(!moduleCode.equals(code)){
						org.apache.commons.io.FileUtils.deleteDirectory(uploadedFile);
						return "ec.exception.NOSAME_MODULE_IMPORT.toMessageKey()";
					}
				}else{
					tempModule = moduleDao.load(code);
				}
				if (tempModule != null) {
					return "ec.exception.MODULE_IMPORTED_EXISTS.toMessageKey()";
				}
			}
		}
		return filePath + "," + code;
	}

	@Override
	public List<Module> getMsModuleByCatetorys(List<String> catetorys) {
		String hql = " from Module as m where m.type='Mis' and  m.category in (:catetorys)";
		List<Module> module = moduleDao.createQuery(hql).setParameterList("catetorys", catetorys).list();
		return module;
	}

	@Override
	public List<Module> getMsModuleByCatetory(String catetory) {
		String hql = " from Module as m where  m.category = ?";
		List<Module> module = moduleDao.findByHql(hql, new Object[] { catetory });
		return module;
	}

	@Override
	public List<Module> getMsModuleByCatetory(String catetory,
											  List<Module> modules) {
		List<Module> result = new ArrayList<Module>();
		for(Module m:modules){
			if(null != m.getCategory() && m.getCategory().equals(catetory)){
				result.add(m);
			}
		}
		return result;
	}

	@Override
	public List<Module> getMsModuleByCatetorys(List<String> values,
											   List<Module> modules) {
		List<Module> result = new ArrayList<Module>();
		for(Module m:modules){
			for(String catetory:values){
				if(null != m.getCategory()&& m.getCategory().equals(catetory)){
					result.add(m);
					break;
				}
			}
		}
		return result;
	}

	private Map<String, Integer> findModuleEntityCount(){
		Map<String, Integer> entityCounts = new HashMap<String, Integer>();
		String sql = "SELECT MODULE_CODE,COUNT(CODE) FROM " + Entity.TABLE_NAME + " WHERE VALID =1 GROUP BY MODULE_CODE";
		List<Object[]> moduleEntitys = moduleDao.createNativeQuery(sql).list();
		for(int i =0;i<moduleEntitys.size();i++){
			Object[] obj = moduleEntitys.get(i);
			String moduleCode = (String) (null != obj[0] ? obj[0] : "");
			Integer count = new Integer((null != obj[1] ? obj[1] : 0) + "");
			entityCounts.put(moduleCode, count);
		}
		return entityCounts;
	}


	/**
	 *
	* @Title: sortUploads
	* @Description: TODO(对新上载的模块包和已经存在的模块包进行排序合并)
	* @param @param firstimportUploadInfos
	* @param @param uploadInfos
	* @param @return    参数
	* @throws
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UploadInfo> sortUploads(List<UploadInfo> firstimportUploadInfos, List<UploadInfo> existUploadInfos) {
		List<Map<String, String>> moduleRelations = getAllRelationForUpload(firstimportUploadInfos);
		Map<Integer, List<UploadInfo>> uploadLevel = new HashMap<Integer, List<UploadInfo>>();
		Map<String, Integer> entityCounts = findModuleEntityCount();
		List<UploadInfo> uploadInfos = new ArrayList<UploadInfo>();
		uploadLevel.put(0, new ArrayList<UploadInfo>(firstimportUploadInfos));
		uploadLevel.put(1, new ArrayList<UploadInfo>());
		uploadLevel.put(2, new ArrayList<UploadInfo>());
		uploadLevel.put(3, new ArrayList<UploadInfo>());
		uploadLevel.put(4, new ArrayList<UploadInfo>());
		uploadLevel.put(5, new ArrayList<UploadInfo>());
		uploadLevel.put(6, new ArrayList<UploadInfo>());
		uploadLevel.put(7, new ArrayList<UploadInfo>());
		uploadLevel.put(8, new ArrayList<UploadInfo>());
		uploadLevel.put(9, new ArrayList<UploadInfo>());
		uploadLevel.put(10, new ArrayList<UploadInfo>());		//模块依赖层级不会超过4层，保险起见，写到11层
		firstimportUploadInfos.clear();
		//把新上载的模块进行等级分割，依赖层次越高的level越高（类似设备的level就比较高）
		for(Entry<Integer, List<UploadInfo>> uploads : uploadLevel.entrySet()){
			List<UploadInfo> uploadList = uploads.getValue();
			Set<String> moduleCodes = new HashSet<String>();
			for(UploadInfo uploadInfo : uploadList){
				moduleCodes.add(uploadInfo.getModuleCode());
			}
			Iterator<UploadInfo> uploadIterator = uploadList.iterator();
			while(uploadIterator.hasNext()){
				UploadInfo upload = uploadIterator.next();
				for(Map moduleRelation : moduleRelations){
					if(upload != null && upload.getModuleCode() != null && upload.getModuleCode().equals(moduleRelation.get("moduleCode"))){
						if(moduleCodes.contains(moduleRelation.get("targetModuleCode"))){
							upload.setEntityNum(entityCounts.get(upload.getModuleCode()));
							uploadLevel.get(uploads.getKey()+1).add(upload);
							uploadIterator.remove();
							break;
						}
					}
				}
			}
		}
		for(Integer level : uploadLevel.keySet()){
			List<UploadInfo> uploads  = uploadLevel.get(level);
			if(!(null == uploads || uploads.size() < 1)){
				// 根据实体数量排序，实体少的模块顺序在前
				Collections.sort(uploads, new Comparator<UploadInfo>() {
					@Override
					public int compare(UploadInfo o1, UploadInfo o2) {
						if(null == o1 || null == o2 || null == o2.getEntityNum() || null == o1.getEntityNum()){
							return 0;
						}
						return o2.getEntityNum() - o1.getEntityNum();
					}
				});
			}
		}
		//把已经储存好的upload进行level标记
		for(Entry<Integer, List<UploadInfo>> uploads : uploadLevel.entrySet()){
			List<UploadInfo> uploadList = uploads.getValue();
			Iterator<UploadInfo> uploadIterator = uploadList.iterator();
			while(uploadIterator.hasNext()){
				UploadInfo upload = uploadIterator.next();
				upload.setLevel(uploads.getKey());
				uploadInfos.add(upload);
			}
		}
		//对已经存在的模块包进行实体数量的标记
		Iterator<UploadInfo> existUploadInfoIterator = existUploadInfos.iterator();
		while(existUploadInfoIterator.hasNext()){
			UploadInfo upload = existUploadInfoIterator.next();
			upload.setEntityNum(entityCounts.get(upload.getModuleCode()));
			upload.setLevel(11);
			uploadInfos.add(upload);
		}
		return uploadInfos;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<DeployInfo> getDeployListBytaskId(String taskId) {
		String hql = "from DeployInfo where logfilePrefix=?0";
		return moduleDao.findByHql(hql, new String[] { taskId });
	}

	@Value("${configuration-services.workspace:''}")
	private String workspacePath;
	@Autowired
	private UploadInfoManager uploadManager;
	@Autowired
	private UploadInfoService uploadInfoService;
	@Autowired
	private ImportTemplateService importTemplateService;
	@Autowired
	private CustomCodeService customCodeService;

	@Override
	@Transactional(timeout = -1, propagation = Propagation.REQUIRED)
	public String executeUploadBatch(UploadInfo up) throws IOException, InterruptedException, XMLStreamException {
		List<SchedulerJobDTO> schedulerJobDTOS = null;
		long moduleUploadStart = System.currentTimeMillis();
		//"开始上载,模块名为：{}"
		EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.startupload"),up.getModuleName());
		EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.startupload"),up.getModuleName());
		File f = new File("");
		boolean filter = false;
		String version = "";
		UploadInfo uploadInfo = up;
		uploadInfo.setUploadDate(new Date());
		Module module = null;
		//记录各个阶段的时间
		long timeA = 0;//寻找模块文件耗时
		long timeB = 0;//解压模块文件耗时
		long timeC = 0;//解析module文件耗时
		long timeD = 0;//导入模板模块文件耗时
		long timeE = 0;//拷贝模块文件耗时
		long timeF = 0;//导入系统编码模块文件耗时
		long timeG = 0;//portlet模块文件耗时
		long timeH = 0;//处理国际化模块文件耗时
		long timeI = 0;//导入国际化模块文件耗时
		long timeJ = 0;//解压模块文件耗时
		long timeK = 0;//插入记录时间
		long timeL = 0;//导入调度模板
		Long totalTime = 0L;//总耗时
		DateFormat bf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String moduleName = up.getModuleName();
		String relations = "";
		List<String> relationLists = new ArrayList<String>(); // 所有依赖模块编码
		List<Object[]> msServiceList = new ArrayList<Object[]>(); // 依赖模块所在服务
		try {
			Date startDate=new Date();
			String base = PropertyHolder.get().getGeneratePath() + File.separator + "unziped";
			timeA = System.currentTimeMillis();
			File file = new File(base + File.separator + up.getModuleCode());
			Map uploadInfoTaskMap = EcUtils.dealFileTasksQueue.poll(60, TimeUnit.MINUTES);
			if(!uploadInfoTaskMap.containsKey(up.getModuleCode())){
				throw new EcException(InternationalResource.get("ec.model.upload.unzipfiletimeout"));
			}
			uploadManager.updatetaskProggress(UploadTaskProggress.UPLAODTASK_UNZIP);
			timeB = System.currentTimeMillis();
			//"解压模块文件耗时{}秒"
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.unzipmodulefiles"),(timeB-timeA)*1.0/1000);
			EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.unzipmodulefiles"),(timeB-timeA)*1.0/1000);

			// 处理module.xml
			File moduleFile = new File(base + File.separator + up.getModuleCode() + File.separator + "service/src/main/resources/META-INF/bap/"
					+ "module.xml");
			if(!moduleFile.exists()){
				moduleFile = new File(base + File.separator + up.getModuleCode() + File.separator + "service/src/generated/resources/META-INF/bap/"
						+ "module.xml");
				if(!moduleFile.exists()){
					moduleFile = new File(base + File.separator + up.getModuleCode() + File.separator + "src/main/resources/META-INF/bap/"
							+ "module.xml");
				}
			}
			boolean isNewGenerate = false;
			if (moduleFile.exists()) {
				String xml = org.apache.commons.io.FileUtils.readFileToString(moduleFile, "UTF-8");
				if (!StringUtils.isEmpty(xml)) {
					// 未选择模块导入时,如果模块已存在,直接报错
					Module tempModule = null;
					String tempModuleCode = null;
					String tempModuleName = null;
					String languagePath = "OSGI-INF";
					try {
						String code = XmlUtils.getTagContent(xml, "code");
						tempModuleCode =code;
						String name = XmlUtils.getTagContent(xml, "name");
						tempModuleName = name;
						isNewGenerate = Boolean.parseBoolean(XmlUtils.getTagContent(xml, "isNewGenerate"));
						tempModule = getModule(code);
						module = tempModule;//这里是为了最后捕获错误可以找到上载的是哪个包
						if(null == tempModule){
							String artifact = XmlUtils.getTagContent(xml, "artifact");
							List modules = getModuleByArtifact(artifact);
							if(null != modules && !modules.isEmpty()){
								tempModule = (Module)modules.get(0);
							}
						}
						if(((Map)XmlUtils.convert(xml)).containsKey("type") && MIS.equals(((Map)XmlUtils.convert(xml)).get("type"))){
							languagePath = "LANG-INF";
						}
						// 模块注册
						moduleRegistryService.registryModule(code);
						// 处理国际化文件
						String artifact = XmlUtils.getTagContent(xml, "artifact");
						File dir = new File(base + File.separator + up.getModuleCode() + File.separator + "service" + File.separator + "src" + File.separator
								+ "main" + File.separator + "resources" + File.separator + languagePath + File.separator + "l10n" + File.separator);
						if(!dir.exists()){
							dir = new File(base + File.separator + up.getModuleCode() + File.separator + "service" + File.separator + "src" + File.separator
									+ "generated" + File.separator + "resources" + File.separator + languagePath + File.separator + "l10n" + File.separator);
						}
						if(dir.exists()){
							File files[] = dir.listFiles();
							if (null != files) {
								try {
									internationalService.initInternational(code, files);
								} catch (Exception e) {
									log.error(e.getMessage(), e);
									EcUtils.uploadFullLogger.info("<span style='color:red'>国际化上载失败: " + e.getMessage() + "</span>");
									EcUtils.uploadLogger.info("<span style='color:red'>国际化上载失败: " + e.getMessage() + "</span>");
								}
							}
						}
						timeH = System.currentTimeMillis();
						//"处理国际化模块文件耗时{}秒"
						EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.dealinternationalfiles"),(timeH-timeB)*1.0/1000);
						EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.dealinternationalfiles"),(timeH-timeB)*1.0/1000);
						if(PropertyHolder.isProduct()){//工程环境下才上载custom目录下的国际化文件
							File customDir = new File(base + File.separator + up.getModuleCode() + File.separator + "service" + File.separator + "src" + File.separator
									+ "main" + File.separator + "resources" + File.separator + languagePath + File.separator + "custom" + File.separator);
							if(!customDir.exists()){
								customDir = new File(base + File.separator + up.getModuleCode() + File.separator + "service" + File.separator + "src" + File.separator
										+ "generated" + File.separator + "resources" + File.separator + languagePath + File.separator + "custom" + File.separator);
							}
							if(customDir.exists()){
								File customFiles[] = customDir.listFiles();
								if (null != customFiles) {
									try {
										internationalService.initInternational(code, customFiles);
									} catch (Exception e) {
										log.error(e.getMessage(), e);
										EcUtils.uploadFullLogger.info("<span style='color:red'>国际化上载失败: " + e.getMessage() + "</span>");
										EcUtils.uploadLogger.info("<span style='color:red'>国际化上载失败: " + e.getMessage() + "</span>");
									}
								}
							}
							timeI = System.currentTimeMillis();
							//"导入国际化模块文件耗时{}秒"
							EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.importinternationalfiles"),(timeI-timeH)*1.0/1000);
							EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.importinternationalfiles"),(timeI-timeH)*1.0/1000);
						} else {
							timeI = timeH;
						}
//						internationalService.loadAllInternatinalResource();
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
					if (null != tempModule) {
						throw new EcException(EcException.Code.MODULE_IMPORTED_EXISTS);
					}
					try {
						relations = XmlUtils.getTagContent(xml, "relations");
						if (relations != null && relations.length() > 0) {
							String[] moduleCodes = relations.split(",");
							for (String moduleCode : moduleCodes) {
								if (moduleCode != null && moduleCode.length() > 0) {
									Module target = getModule(moduleCode);
									module = target;
									if (target == null) {
										throw new EcException(EcException.Code.DEPENDENT_MODULE_NOT_EXISTS);
									}
								}
							}
						}

						String references = XmlUtils.getTagContent(xml, "references");
						if (references != null && references.length() > 0) {
							String[] moduleCodes = references.split(",");
							for (String moduleCode : moduleCodes) {
								if (moduleCode != null && moduleCode.length() > 0) {
									Module target = getModule(moduleCode);
									module = target;
									if (target == null) {
										throw new EcException(internationalService.getI18nValue("ec.exceptions.DEPENDENT_MODULE_NOT_EXISTS"));
									}
								}
							}
						}
					} catch (Exception e) {
						//此处根据moduleName的国际化key值去国际化文件中查找中文名,为了安全写了很多的try catch
						File zh_CN_File = new File(base + File.separator + up.getModuleCode() + File.separator + "service/src/main/resources/" + languagePath + "/l10n/"
								+ "package_zh_CN.properties");
						if(zh_CN_File.exists()){
							FileInputStream fis = null;
							InputStreamReader reader = null;
							try {
								Properties prop = new Properties();
								fis = new FileInputStream(zh_CN_File);
								reader = new InputStreamReader(fis,"utf-8");
								prop.load(reader);
								module = new Module();
								module.setCode(tempModuleCode);
								module.setName(prop.getProperty(tempModuleName));
							} catch (Exception e2) {
								throw new EcException("读取国际化文件出错！");
							}finally{
								try {
									if(null!=reader){
										reader.close();
									}
								} catch (Exception e3) {
									throw new EcException("读取国际化文件出错！");
								}
								try {
									if(null!=fis){
										fis.close();
									}
								} catch (Exception e3) {
									throw new EcException("读取国际化文件出错！");
								}

							}
						}
						throw new EcException(internationalService.getI18nValue("ec.exceptions.DEPENDENT_MODULE_NOT_EXISTS"));
//						throw new EcException(EcException.Code.DEPENDENT_MODULE_NOT_EXISTS);
					}
					try {
						//正在解析module文件
						EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.parsingmodulefile"));
						EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.parsingmodulefile"));
						module = importXml(xml, true, filter);
						//module适用范围默认是全部公司,如果已经存在关联关系，就不关联了
						if (!this.existCompanyRef(module)){
							this.saveModuleCompanyRefAllCompany(module);
						}
					} catch (Exception e1) {
						log.error(e1.getMessage(),e1);
						throw e1;
					}
					try {
						relations = XmlUtils.getTagContent(xml, "relations");
						if (relations != null && relations.length() > 0) {
							String[] moduleCodes = relations.split(",");
							for (String moduleCode : moduleCodes) {
								if (moduleCode != null && moduleCode.length() > 0) {
									Module target = getModule(moduleCode);
									ModuleRelation relation = getRelation(module, target);
									if (relation == null) {
										relation = new ModuleRelation();
									}
									relation.setModule(module);
									relation.setTarget(target);
									relation.setCode(module.getCode() + target.getCode());
									saveRelation(relation);

									if (MIS.equals(module.getType())) {
										// 查询所有依赖模块
										relationLists = getAllRelationModules(target, relationLists);
									}
								}
							}
						}

					} catch (Exception e) {
						throw new EcException(e);
					}

					// 微服务上载如果所有依赖模块不在同一服务下，则不允许上载
					String relationSql = "SELECT CODE, MSMODULE_CODE FROM EC_MSMODULE_RELATION WHERE VALID = 1 AND CODE IN ( :relationModuleList )";
					if (MIS.equals(module.getType()) && !relationLists.isEmpty()) {
						msServiceList = dataGroupDao.createNativeQuery(relationSql).setParameterList("relationModuleList", relationLists).list();
						if (msServiceList != null && !msServiceList.isEmpty()) {
							String tmpMsServiceName = msServiceList.get(0)[1].toString();
							for (Object[] msService : msServiceList) {
								if (!tmpMsServiceName.equals(msService[1].toString())) {
									throw new EcException(up.getModuleName() + "(" + module.getCode() + ")" + InternationalResource.get("ec.model.upload.MultipleServices"));
								}
								relationLists.remove(msService[0].toString());
							}
						}
					}

					// 保存模块引用关系
					try {
						String references = XmlUtils.getTagContent(xml, "references");
						if (references != null && references.length() > 0) {
							String[] moduleCodes = references.split(",");
							for (String moduleCode : moduleCodes) {
								if (moduleCode != null && moduleCode.length() > 0) {
									Module target = getModule(moduleCode);
									ModuleReference reference = moduleReferenceService.getReferenceWithNoValid(module, target);
									if (reference == null) {
										reference = new ModuleReference();
									}
									reference.setModule(module);
									reference.setTarget(target);
									reference.setCode(module.getCode() + target.getCode());
									reference.setValid(true);
									moduleReferenceService.save(reference);
								}
							}
						}
					} catch (Exception e) {
						throw new EcException(EcException.Code.DEPENDENT_MODULE_NOT_EXISTS);
					}
				}
			} else {
				throw new EcException("module.xml does not exist");
			}
			uploadManager.updatetaskProggress(UploadTaskProggress.UPLAODTASK_IMPORTMODULEXML);
			timeC = System.currentTimeMillis();
			//解析module文件耗时{}秒
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.parsingmodulefiletime"),(timeC-timeI)*1.0/1000);
			EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.parsingmodulefiletime"),(timeC-timeI)*1.0/1000);

			File appFile = new File(base + File.separator + up.getModuleCode() + File.separator + "service" + File.separator + "src"
					+ File.separator + "main" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap"
					+ File.separator + "app.xml");
			if (appFile.exists()) {
				log.info("开始导入app");
				String moduleXml = "";
				if(moduleFile.exists()){
					moduleXml = org.apache.commons.io.FileUtils.readFileToString(moduleFile, "UTF-8");
				}
				String xml = org.apache.commons.io.FileUtils.readFileToString(appFile, "UTF-8");
				if (!StringUtils.isEmpty(xml)) {
					importApp(xml,moduleXml);
				}
			}
			File importTemplateFile = new File(base + File.separator + up.getModuleCode() + File.separator + "service" + File.separator + "src"
					+ File.separator + "generated" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap"
					+ File.separator + "importTemplate.xml");
			if (!importTemplateFile.exists()) {
				importTemplateFile = new File(base + File.separator + up.getModuleCode() + File.separator + "service" + File.separator + "src"
						+ File.separator + "main" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap"
						+ File.separator + "importTemplate.xml");
			}
			if (importTemplateFile.exists()) {
				String xml = org.apache.commons.io.FileUtils.readFileToString(importTemplateFile, "UTF-8");
				if (!StringUtils.isEmpty(xml)) {
					try {
						String code = XmlUtils.getTagContent(xml, "code");
						if(!code.isEmpty()){
							importTemplateService.importXml(xml);
						}
					}catch(Exception e){
						log.error(e.getMessage(),e);
						throw new EcException(EcException.Code.IMPORT_TEMPLATE_FILE_DEAL_ERROR);
					}
				}
			}
			uploadManager.updatetaskProggress(UploadTaskProggress.UPLAODTASK_IMPORTTEMPLATE);
			timeD= System.currentTimeMillis();
			//导入模板模块文件耗时{}秒
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.importtemplatefile"),(timeD-timeC)*1.0/1000);
			EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.importtemplatefile"),(timeD-timeC)*1.0/1000);



			File scriptFile = new File(workspacePath + File.separatorChar + "scripts");// 本地脚本目录
			File srcScriptFile = new File(base + File.separator + up.getModuleCode() + File.separator + "scripts");// zip中的脚本

			// 拷贝文件
			if(module != null){
				File generateFile = new File(PropertyHolder.get().getGeneratePath() + File.separator + module.getCode());
				if (!generateFile.exists())
					generateFile.mkdirs();
				if (!scriptFile.exists()) {
					scriptFile.mkdirs();
				}
				try {
					// 拷贝第三方jar包
					dealThreeJarFile(base, module.getCode());
					org.apache.commons.io.FileUtils.copyDirectory(file, generateFile);
					if (srcScriptFile.exists()) {
						if (srcScriptFile.isDirectory()) {
							File[] files = srcScriptFile.listFiles();
							if (files == null || files.length == 0) {
								// TODO
							} else {
								for (File file2 : files) {
									if (file2.isDirectory()) {
										org.apache.commons.io.FileUtils.copyDirectoryToDirectory(file2, scriptFile);
									} else {
										org.apache.commons.io.FileUtils.copyFileToDirectory(file2, scriptFile);
									}
								}
							}
						}
					}
				} catch (IOException e) {
					// 拷贝文件发生异常时，打出log，不做处理
					log.warn(e.getMessage(), e);
					EcUtils.uploadFullLogger.info(e.getMessage());
					EcUtils.uploadLogger.info(e.getMessage());
				}
			}
			timeE = System.currentTimeMillis();
			//"拷贝模块文件耗时{}秒"
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.copymodulefile"),(timeE-timeD)*1.0/1000);
			EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.copymodulefile"),(timeE-timeD)*1.0/1000);
			// 导入系统编码信息
			File systemCodeFile = new File(base + File.separator + up.getModuleCode() + File.separator + "service/src/main/resources/META-INF/bap/"
					+ "systemcode.xml");
			if (!systemCodeFile.exists()) {
				systemCodeFile = new File(base + File.separator + up.getModuleCode() + File.separator + "service/src/generated/resources/META-INF/bap/"
						+ "systemcode.xml");
			}
			if (systemCodeFile.exists()) {
				try {
					systemCodeService.initializeSystemCode(systemCodeFile.toURI().toURL());
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					EcUtils.uploadFullLogger.info("<span style='color:red'>系统编码上载失败: " + e.getMessage() + "</span>");
					EcUtils.uploadLogger.info("<span style='color:red'>系统编码上载失败: " + e.getMessage() + "</span>");
				}
			}
			uploadManager.updatetaskProggress(UploadTaskProggress.UPLAODTASK_IMPORTSYSTEMCODE);
			timeF = System.currentTimeMillis();
			//导入系统编码模块文件耗时{}秒
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.importsystemcode"),(timeF-timeE)*1.0/1000);
			EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.importsystemcode"),(timeF-timeE)*1.0/1000);
			//处理portlet
			File portletFile = new File(base + File.separator + up.getModuleCode() + File.separator + "service" + File.separator + "src"
					+ File.separator + "generated" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap"
					+ File.separator + "module-portlet.xml");
			if (!portletFile.exists()) {
				portletFile = new File(base + File.separator + up.getModuleCode() + File.separator + "service" + File.separator + "src"
						+ File.separator + "main" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap"
						+ File.separator + "module-portlet.xml");
			}
			if (portletFile.exists()) {
				String xml = org.apache.commons.io.FileUtils.readFileToString(portletFile, "UTF-8");
				if (!StringUtils.isEmpty(xml)) {
					try {
//						portletService.initializePortlet(xml);
					}catch(Exception e){
						log.error(e.getMessage(),e);
						throw new EcException(EcException.Code.IMPORT_PORTLET_FILE_ERROR);
					}
				}
			}
			uploadManager.updatetaskProggress(UploadTaskProggress.UPLAODTASK_IMPORTPORTLET);
			timeG = System.currentTimeMillis();
			//portlet模块文件耗时{}秒
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.portletmodule"),(timeG-timeF)*1.0/1000);
			EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.portletmodule"),(timeG-timeF)*1.0/1000);

			UnZipFile.unzip(f, file);
			// 国际化初始化后进行菜单助记码
			menuInfoService.batchDealMenuInfoMne(module.getCode(), module.getArtifact());
			timeJ = System.currentTimeMillis();
			//"解压模块文件耗时{}秒"
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.unzipmodulefiles"),(timeJ-timeG)*1.0/1000);
			EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.unzipmodulefiles"),(timeJ-timeG)*1.0/1000);
			if(!isNewGenerate){
				// 处理自定义代码
				dealCustomCode(module,null, base, file);
			}else{
				dealPomCustomCode(module);
			}
			timeK = System.currentTimeMillis();
			//导入调度模板
			File scheduleJobFile = new File(base + File.separator + up.getModuleCode() + File.separator + "service" + File.separator + "src"
					+ File.separator + "generated" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap"
					+ File.separator + "schedulerJob.xml");
			if (!scheduleJobFile.exists()) {
				scheduleJobFile = new File(base + File.separator + up.getModuleCode() + File.separator + "service" + File.separator + "src"
						+ File.separator + "main" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap"
						+ File.separator + "schedulerJob.xml");
			}
			if (scheduleJobFile.exists()) {
				String xml = org.apache.commons.io.FileUtils.readFileToString(scheduleJobFile, "UTF-8");
				if (!StringUtils.isEmpty(xml)) {
					try {
						String code = XmlUtils.getTagContent(xml, "code");
						if(!code.isEmpty()){
                            schedulerJobDTOS = schedulerService.importXml(xml);
						}
					}catch(Exception e){
						log.error(e.getMessage(),e);
//						throw new EcException(EcException.Code.SCHEDULER_TEMPLATE_FILE_DEAL_ERROR);
					}
				}
			}
			timeL = System.currentTimeMillis();
			if(module != null){
				//EC_ENTITY_MODIFY_INFO中插入数据
//				ecGenerateInfoService.saveEntityModifyInfo(null, module.getCode());
//				dataGroupDao.createNativeQuery("UPDATE EC_MODULE SET IS_READ_ONLY=0 WHERE CODE=?",module.getCode()).executeUpdate();
				//开始保存模块信息数据的最后修改时间
//				ModuleGenerateInfo generateInfo = moduleGenerateInfoService.getModuleGenerateInfoByModuleCode(module.getCode());
//				if(generateInfo!=null){
//					generateInfo.setLastModifyTime(new Date());
//					moduleGenerateInfoService.save(generateInfo,Thread.currentThread());
//				}else{
//					generateInfo = new ModuleGenerateInfo();
//					generateInfo.setLastModifyTime(new Date());
//					generateInfo.setModuleCode(module.getCode());
//					moduleGenerateInfoService.create(generateInfo,Thread.currentThread());
//				}
//				if (MIS.equals(module.getType())) {
//					String msServiceCode = "BasicMs";
//					if (msServiceList != null && !msServiceList.isEmpty()) {
//						msServiceCode = msServiceList.get(0)[1].toString();
//					}
//					// 插入微服务模块信息，与依赖模块挂在相同服务下 TODO
//					String addMsModuleRelationSql = "insert into EC_MSMODULE_RELATION (CODE, MSMODULE_CODE, VALID, EC_ENV, CREATE_TIME, CREATE_STAFF_ID, VERSION) values (?,?,1,?,?,?,0)";
//					msModuleRelationDAO.createNativeQuery(addMsModuleRelationSql, module.getCode(), msServiceCode, module.getEcEnv().name(), new Date(), uploadInfo.getUploadStaff().getId()).executeUpdate();
//					// 存在依赖模块未关联服务的，将模块挂在其他依赖模块服务下
//					for (String relationCode : relationLists) {
//						msModuleRelationDAO.createNativeQuery(addMsModuleRelationSql, relationCode, msServiceCode, module.getEcEnv().name(), new Date(), uploadInfo.getUploadStaff().getId()).executeUpdate();
//					}
//				}
				//最后没有报错插入上载记录
				uploadInfo.setModuleCode(module.getCode());
				uploadInfo.setModuleName(InternationalResource.get(module.getName()));}
			uploadInfo.setUploadState("success");
			totalTime = (System.currentTimeMillis()-moduleUploadStart)/1000;
			uploadInfo.setUploada(((timeA-moduleUploadStart)*1.0/1000)+"");
			uploadInfo.setUploadb(((timeB-timeA)*1.0/1000)+"");
			uploadInfo.setUploadc(((timeC-timeI)*1.0/1000)+"");
			uploadInfo.setUploadd(((timeD-timeC)*1.0/1000)+"");
			uploadInfo.setUploade(((timeE-timeD)*1.0/1000)+"");
			uploadInfo.setUploadf(((timeF-timeE)*1.0/1000)+"");
			uploadInfo.setUploadg(((timeG-timeF)*1.0/1000)+"");
			uploadInfo.setUploadh(((timeH-timeB)*1.0/1000)+"");
			uploadInfo.setUploadi(((timeI-timeH)*1.0/1000)+"");
			uploadInfo.setUploadj(((timeJ-timeG)*1.0/1000)+"");
			uploadInfo.setUploadk(((timeK-timeJ)*1.0/1000)+"");
			uploadInfo.setUploadl(((timeL-timeK)*1.0/1000)+"");
			uploadInfo.setTotalTime(totalTime.toString());
			uploadInfoService.save(uploadInfo);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			//捕获所有的报错在这里插入一条上载报错的记录
			if(null!=module){
				uploadInfo.setModuleCode(module.getCode());
				uploadInfo.setModuleName(InternationalResource.get(module.getName()));
			}
			totalTime = (System.currentTimeMillis()-moduleUploadStart)/1000;
			timeK = System.currentTimeMillis();
			uploadInfo.setUploadState("failed");
			uploadInfo.setUploada(((timeA-moduleUploadStart)*1.0/1000)+"");
			uploadInfo.setUploadb(((timeB-timeA)*1.0/1000)+"");
			uploadInfo.setUploadc(((timeC-timeI)*1.0/1000)+"");
			uploadInfo.setUploadd(((timeD-timeC)*1.0/1000)+"");
			uploadInfo.setUploade(((timeE-timeD)*1.0/1000)+"");
			uploadInfo.setUploadf(((timeF-timeE)*1.0/1000)+"");
			uploadInfo.setUploadg(((timeG-timeF)*1.0/1000)+"");
			uploadInfo.setUploadh(((timeH-timeB)*1.0/1000)+"");
			uploadInfo.setUploadi(((timeI-timeH)*1.0/1000)+"");
			uploadInfo.setUploadj(((timeJ-timeG)*1.0/1000)+"");
			uploadInfo.setUploadk(((timeK-timeJ)*1.0/1000)+"");
			uploadInfo.setUploadl(((timeL-timeK)*1.0/1000)+"");
			uploadInfo.setTotalTime(totalTime.toString());
			uploadInfoService.save(uploadInfo);
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.enduploadnew"),moduleName,e.getMessage());
			EcUtils.uploadLogger.info("<span style='color:red'>" + InternationalResource.get("ec.model.upload.enduploadnew"),moduleName,e.getMessage() + "</span>");
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.uploadtime"),moduleName,(timeK-moduleUploadStart)*1.0/1000);
			//EcUtils.uploadLogger.info("{}模块文件总耗时{}秒",moduleName,(timeK-moduleUploadStart)*1.0/1000);
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.enduploadnamenew"),moduleName);
			EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.enduploadnamenew"),moduleName);
			throw e;
		}
		EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.enduploadnamenall"),moduleName);
		EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.enduploadnamenall"),moduleName);
		timeJ = System.currentTimeMillis();
		EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.uploadtime"),moduleName,(timeJ-moduleUploadStart)*1.0/1000);
		//EcUtils.uploadLogger.info("{}模块文件总耗时{}秒",moduleName,(timeJ-moduleUploadStart)*1.0/1000);
		uploadManager.updatetaskProggress(UploadTaskProggress.UPLAODTASK_IMPORTOTHER);
		//因为无法回滚，最后再调用调度的feign接口
		if(!ObjectUtils.isEmpty(schedulerJobDTOS)){
			iSchedulerJobApiService.scheduleAdd(schedulerJobDTOS);
		}
		return "success";
	}

	@Override
	@Transactional(timeout = -1, propagation = Propagation.REQUIRED)
	public String executeUploadBatchExist(UploadInfo up) throws IOException, InterruptedException, XMLStreamException, EcException {
		long moduleUploadStart = System.currentTimeMillis();
		List<SchedulerJobDTO> schedulerJobDTOS = null;
		String localLanguage = EcUtils.uploadTask.get("localLanguage");
		EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.startuploadexist"),up.getModuleName(),up.getModuleCode());
		EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.startuploadexist"),up.getModuleName());
		Module module  =  null;
		//记录各个阶段的时间
		long timeA = 0;//寻找模块文件耗时
		long timeB = 0;//解压模块文件耗时
		//将国际化处理往前调
		long timeH = 0;//处理国际化模块文件耗时
		long timeI = 0;//导入国际化模块文件耗时
		long timeC = 0;//解析module文件耗时
		long timeD = 0;//导入模板模块文件耗时
		long timeE = 0;//拷贝模块文件耗时
		long timeF = 0;//导入系统编码模块文件耗时
		long timeG = 0;//portlet模块文件耗时
		long timeJ = 0;//解压模块文件耗时
		long timeK = 0;//插入自定义代码时间
		long timeL = 0;//插入调度模板使用的时间
		Long totalTime = 0L;//总耗时
		DateFormat bf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String relations = "";
		List<String> relationLists = new ArrayList<String>(); // 所有依赖模块编码
		List<Object[]> msServiceList = new ArrayList<Object[]>(); // 依赖模块所在服务
		try {
			module = getModule(up.getModuleCode());
		} catch (StaleObjectStateException e) {
			EcUtils.uploadFullLogger.info("executeUploadBatchExist中检测到不存在的moduleCode{}",up.getModuleCode());
			EcUtils.uploadLogger.info("executeUploadBatchExist中检测到不存在的moduleCode{}",up.getModuleCode());
		}

		String version = up.getOldVersion();
		Boolean uploadMetaData = up.getIsMetadata();
		Boolean uploadCustomCode= up.getIsCustomcode();
		Boolean uploadWorkFlow= up.getIsFlow();
		boolean filter = up.getIsFilterMethod();
		Boolean uploadImportTemplate= up.getIsImportTemplate();
		Boolean uploadschedulerJob= up.getIsUploadschedulerJob();

		UploadInfo uploadInfo = up;
		uploadInfo.setUploadDate(new Date());
		uploadInfo.setUploadStaff( getCurrentUser()!=null ? getCurrentUser().getStaff() : (up.getUploadStaff() != null ? up.getUploadStaff() : null));
		boolean isChangeVersion = false;
		EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.starttime")+bf.format(new Date()));
		EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.starttime")+bf.format(new Date()));
		try {
			String base = PropertyHolder.get().getGeneratePath() + File.separator + "unziped";
			String uploaded = PropertyHolder.get().getGeneratePath() + File.separator + "uploaded" + File.separator + up.getUploadFileName();
			File file = new File(base + File.separator + module.getCode());
			timeA = System.currentTimeMillis();
			Map uploadInfoTaskMap = EcUtils.dealFileTasksQueue.poll(60, TimeUnit.MINUTES);
			if(!uploadInfoTaskMap.containsKey(up.getModuleCode())){
				throw new EcException(InternationalResource.get("ec.model.upload.unzipfiletimeout"));
			}
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.unziped"),up.getModuleCode());
			EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.unziped"),up.getModuleCode());
			uploadManager.updatetaskProggress(UploadTaskProggress.UPLAODTASK_UNZIP);
			//clearGabage(10000);
			timeB = System.currentTimeMillis();
			//"解压模块文件耗时{}秒"
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.unzipmodulefiles"),(timeB-timeA)*1.0/1000);
			EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.unzipmodulefiles"),(timeB-timeA)*1.0/1000);
			String languagePath = "OSGI-INF";
			if(null != module.getType() && MIS.equals(module.getType())){
				languagePath = "LANG-INF";
			}
			// 模块注册
			moduleRegistryService.registryModule(module.getCode());
			// 处理国际化文件
			File dir = new File(base + File.separator + module.getCode() + File.separator + "service" + File.separator + "src"
					+ File.separator + "generated" + File.separator + "resources" + File.separator + languagePath + File.separator + "l10n"
					+ File.separator);
			if(!dir.exists()){
				dir = new File(base + File.separator + module.getCode() + File.separator + "service" + File.separator + "src"
						+ File.separator + "main" + File.separator + "resources" + File.separator + languagePath + File.separator + "l10n"
						+ File.separator);
			}
			if(dir.exists()){
				File files[] = dir.listFiles();
				if (null != files) {
					try {
						internationalService.initInternational(module.getCode(), files);
					} catch (Exception e) {
						log.error(e.getMessage(), e);
						EcUtils.uploadFullLogger.info("<span style='color:red'>国际化上载失败: " + e.getMessage() + "</span>");
						EcUtils.uploadLogger.info("<span style='color:red'>国际化上载失败: " + e.getMessage() + "</span>");
					}
				}
			}
			timeH = System.currentTimeMillis();
			//"处理国际化模块文件耗时{}秒({})"
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.dealinternationalfiles")+"({})",(timeH-timeB)*1.0/1000,up.getModuleCode());
			EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.dealinternationalfiles")+"({})",(timeH-timeB)*1.0/1000,up.getModuleCode());
			if(PropertyHolder.isProduct()){//工程环境下才上载custom目录下的国际化文件
				File customDir = new File(base + File.separator + module.getCode() + File.separator + "service" + File.separator + "src" + File.separator
						+ "generated" + File.separator + "resources" + File.separator + languagePath + File.separator + "custom" + File.separator);
				if(!customDir.exists()){
					customDir = new File(base + File.separator + module.getCode() + File.separator + "service" + File.separator + "src" + File.separator
							+ "main" + File.separator + "resources" + File.separator + languagePath + File.separator + "custom" + File.separator);
				}
				if(customDir.exists()){
					File customFiles[] = customDir.listFiles();
					if (null != customFiles) {
						for (int i = 0; i < customFiles.length; i++) {
							try {
								internationalService.initInternational(module.getCode(), customFiles);
							} catch (Exception e) {
								log.error(e.getMessage(), e);
								EcUtils.uploadFullLogger.info("<span style='color:red'>国际化上载失败: " + e.getMessage() + "</span>");
								EcUtils.uploadLogger.info("<span style='color:red'>国际化上载失败: " + e.getMessage() + "</span>");
							}
						}
					}
				}
				timeI = System.currentTimeMillis();
				//"导入国际化模块文件耗时{}秒({})"
				EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.importinternationalfiles")+"({})",(timeI-timeH)*1.0/1000,up.getModuleCode());
				EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.importinternationalfiles")+"({})",(timeI-timeH)*1.0/1000,up.getModuleCode());
			} else {
				timeI = timeH;
			}
//			internationalService.loadAllInternatinalResource();
			// 处理module.xml
			File moduleFile = new File(base + File.separator + module.getCode() + File.separator + "service" + File.separator + "src"
					+ File.separator + "generated" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap"
					+ File.separator + "module.xml");
			if (!moduleFile.exists()) {
				moduleFile = new File(base + File.separator + module.getCode() + File.separator + "service" + File.separator + "src"
						+ File.separator + "main" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap"
						+ File.separator + "module.xml");
				if(!moduleFile.exists()){
					moduleFile = new File(base + File.separator + module.getCode() + File.separator + "src"
							+ File.separator + "main" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap"
							+ File.separator + "module.xml");
				}
			}
			//clearGabage(10000);
			boolean isNewGenerate = false;
			if (moduleFile.exists()) {
				String xml = org.apache.commons.io.FileUtils.readFileToString(moduleFile, "UTF-8");
				if (!StringUtils.isEmpty(xml)) {
					try {
						String code = XmlUtils.getTagContent(xml, "code");
						String projectVersion = XmlUtils.getTagContent(xml, "projectVersion");
						if(null != version && !projectVersion.equals(module.getProjectVersion())){
							isChangeVersion = true;
						}
						isNewGenerate = Boolean.parseBoolean(XmlUtils.getTagContent(xml, "isNewGenerate"));
						if (null != module.getCode() && !module.getCode().equals(code)) {
							throw new Exception();
						}
					} catch (Exception e) {
						throw new EcException(EcException.Code.NOSAME_MODULE_IMPORT);
					}
					try {
						relations = XmlUtils.getTagContent(xml, "relations");
						if (relations != null && relations.length() > 0) {
							// 导入已存在模块，需校验模块本身和依赖模块所处服务
							if (MIS.equals(module.getType())) {
								relationLists.add(module.getCode());
							}
							String[] moduleCodes = relations.split(",");
							for (String moduleCode : moduleCodes) {
								if (moduleCode != null && moduleCode.length() > 0) {
									Module target = getModule(moduleCode);
									ModuleRelation relation = getRelation(module, target, false);
									if (relation == null) {
										relation = new ModuleRelation();
									}
									relation.setModule(module);
									relation.setTarget(target);
									relation.setCode(module.getCode() + target.getCode());
									relation.setValid(true);
									saveRelation(relation);
									if (MIS.equals(module.getType())) {
										// 查询所有依赖模块
										relationLists = getAllRelationModules(target, relationLists);
									}
								}
							}
						}
					} catch (Exception e) {
						throw new EcException(EcException.Code.DEPENDENT_MODULE_NOT_EXISTS);
					}

					// 微服务上载如果本模块和所有依赖模块不在同一服务下，则不允许上载
					if (MIS.equals(module.getType()) && !relationLists.isEmpty()) {
						String relationSql = "SELECT CODE, MSMODULE_CODE FROM EC_MSMODULE_RELATION WHERE VALID = 1 AND CODE IN ( :relationModuleList )";
						msServiceList = dataGroupDao.createNativeQuery(relationSql).setParameterList("relationModuleList", relationLists).list();
						if (msServiceList != null && !msServiceList.isEmpty()) {
							String tmpMsServiceName = msServiceList.get(0)[1].toString();
							for (Object[] msService : msServiceList) {
								if (!tmpMsServiceName.equals(msService[1].toString())) {
									throw new EcException(up.getModuleName() + "(" + module.getCode() + ")" + InternationalResource.get("ec.model.upload.MultipleServices"));
								}
								relationLists.remove(msService[0].toString());
							}
						}
					}

					// 保存模块引用关系
					try {
						String references = XmlUtils.getTagContent(xml, "references");
						if (references != null && references.length() > 0) {
							String[] moduleCodes = references.split(",");
							for (String moduleCode : moduleCodes) {
								if (moduleCode != null && moduleCode.length() > 0) {
									Module target = getModule(moduleCode);
									ModuleReference reference = moduleReferenceService.getReferenceWithNoValid(module, target);
									if (reference == null) {
										reference = new ModuleReference();
									}
									reference.setModule(module);
									reference.setTarget(target);
									reference.setCode(module.getCode() + target.getCode());
									reference.setValid(true);
									moduleReferenceService.save(reference);
								}
							}
						}
					} catch (Exception e) {
						throw new EcException(EcException.Code.DEPENDENT_MODULE_NOT_EXISTS);
					}
					if (uploadMetaData != null && uploadMetaData) {
						//正在解析module文件
						EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.parsingmodulefile"));
						EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.parsingmodulefile"));
						importXml(xml, uploadWorkFlow, filter);
						//module适用范围默认是全部公司,如果已经存在关联关系，就不关联了
						if (!this.existCompanyRef(module)){
							this.saveModuleCompanyRefAllCompany(module);
						}
					}
				}
			}else{
				throw new EcException("module.xml does not exist");
			}
			uploadManager.updatetaskProggress(UploadTaskProggress.UPLAODTASK_IMPORTMODULEXML);
			timeC = System.currentTimeMillis();
			//解析module文件耗时{}秒({})

			File appFile = new File(base + File.separator + up.getModuleCode() + File.separator + "service" + File.separator + "src"
					+ File.separator + "main" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap"
					+ File.separator + "app.xml");
			if (appFile.exists()) {
				log.info("开始导入app");
				String moduleXml = "";
				if(moduleFile.exists()){
					moduleXml = org.apache.commons.io.FileUtils.readFileToString(moduleFile, "UTF-8");
				}
				String xml = org.apache.commons.io.FileUtils.readFileToString(appFile, "UTF-8");
				if (!StringUtils.isEmpty(xml)) {
					importApp(xml,moduleXml);
				}
			}

			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.parsingmodulefiletime")+"({})",(timeC-timeI)*1.0/1000,up.getModuleCode());
			EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.parsingmodulefiletime")+"({})",(timeC-timeI)*1.0/1000,up.getModuleCode());
			if(uploadImportTemplate){//处理导入导出模板配置
				File importTemplateFile = new File(base + File.separator + module.getCode() + File.separator + "service" + File.separator + "src"
						+ File.separator + "generated" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap"
						+ File.separator + "importTemplate.xml");
				if (!importTemplateFile.exists()) {
					importTemplateFile = new File(base + File.separator + module.getCode() + File.separator + "service" + File.separator + "src"
							+ File.separator + "main" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap"
							+ File.separator + "importTemplate.xml");
				}
				if (importTemplateFile.exists()) {
					String xml = org.apache.commons.io.FileUtils.readFileToString(importTemplateFile, "UTF-8");
					if (!StringUtils.isEmpty(xml)) {
						try {
							String code = XmlUtils.getTagContent(xml, "code");
							if(!code.isEmpty()&& (uploadMetaData != null && uploadMetaData)){
								importTemplateService.importXml(xml);
							}
						}catch(Exception e){
							log.error(e.getMessage(),e);
							throw new EcException(EcException.Code.IMPORT_TEMPLATE_FILE_DEAL_ERROR);
						}
					}
				}
			}
			uploadManager.updatetaskProggress(UploadTaskProggress.UPLAODTASK_IMPORTTEMPLATE);
			timeD= System.currentTimeMillis();
			//导入模板模块文件耗时{}秒({})
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.importtemplatefile")+"({})",(timeD-timeC)*1.0/1000,up.getModuleCode());
			EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.importtemplatefile")+"({})",(timeD-timeC)*1.0/1000,up.getModuleCode());
			File scriptFile = new File(workspacePath + File.separatorChar + "scripts");
			File srcScriptFile = new File(base + File.separator + module.getCode() + File.separator + "scripts");
			// 拷贝文件
			File generateFile = new File(PropertyHolder.get().getGeneratePath() + File.separator + module.getCode());
			if (!generateFile.exists())
				generateFile.mkdirs();

			if (!scriptFile.exists()) {
				scriptFile.mkdirs();
			}
			try {
				// 拷贝第三方jar包
				dealThreeJarFile(base, module.getCode());
				org.apache.commons.io.FileUtils.copyDirectory(file, generateFile);
				if (srcScriptFile.exists() && srcScriptFile.isDirectory()) {
					File[] files = srcScriptFile.listFiles();
					if (files != null && files.length > 0) {
						for (File tmp : files) {
							if (tmp.isDirectory()) {
								org.apache.commons.io.FileUtils.copyDirectoryToDirectory(tmp, scriptFile);
							} else {
								org.apache.commons.io.FileUtils.copyFileToDirectory(tmp, scriptFile);
							}
						}
					}
				}
				File scrFile = new File(PropertyHolder.get().getGeneratePath() + File.separator + module.getCode() + File.separator + "scripts");
				if (scrFile.exists()) {
					scrFile.delete();
				}
			} catch (IOException e) {
				// 拷贝文件发生异常时，打出log，不做处理
				log.warn(e.getMessage(), e);
			}
			//clearGabage(10000);
			timeE = System.currentTimeMillis();
			//拷贝模块文件耗时{}秒
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.copymodulefile")+"({})",(timeE-timeD)*1.0/1000,up.getModuleCode());
			EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.copymodulefile")+"({})",(timeE-timeD)*1.0/1000,up.getModuleCode());
			if (uploadMetaData != null && uploadMetaData) {
				// 导入系统编码信息
				File systemCodeFile = new File(base + File.separator + module.getCode() + File.separator + "service" + File.separator + "src"
						+ File.separator + "generated" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap"
						+ File.separator + "systemcode.xml");
				if (!systemCodeFile.exists()) {
					systemCodeFile = new File(base + File.separator + module.getCode() + File.separator + "service" + File.separator + "src"
							+ File.separator + "main" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap"
							+ File.separator + "systemcode.xml");
				}
				if (systemCodeFile.exists()) {
					EcUtils.uploadFullLogger.info("开始上载系统编码...");
					systemCodeService.initializeSystemCode(systemCodeFile.toURI().toURL());
					EcUtils.uploadFullLogger.info("系统编码上载成功");
				}
				uploadManager.updatetaskProggress(UploadTaskProggress.UPLAODTASK_IMPORTSYSTEMCODE);
				timeF = System.currentTimeMillis();
				//"导入系统编码模块文件耗时{}秒({})"
				EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.importsystemcode")+"({})",(timeF-timeE)*1.0/1000,up.getModuleCode());
				EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.importsystemcode")+"({})",(timeF-timeE)*1.0/1000,up.getModuleCode());
				//处理portlet
				File portletFile = new File(base + File.separator + module.getCode() + File.separator + "service" + File.separator + "src"
						+ File.separator + "generated" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap"
						+ File.separator + "module-portlet.xml");
				if (!portletFile.exists()) {
					portletFile = new File(base + File.separator + module.getCode() + File.separator + "service" + File.separator + "src"
							+ File.separator + "main" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap"
							+ File.separator + "module-portlet.xml");
				}
				if (portletFile.exists()) {
					String xml = org.apache.commons.io.FileUtils.readFileToString(portletFile, "UTF-8");
					if (!StringUtils.isEmpty(xml)) {
						try {
//							portletService.initializePortlet(xml);
						}catch(Exception e){
							log.error(e.getMessage(),e);
							throw new EcException(EcException.Code.IMPORT_PORTLET_FILE_ERROR);
						}
					}
				}
				uploadManager.updatetaskProggress(UploadTaskProggress.UPLAODTASK_IMPORTPORTLET);
				timeG = System.currentTimeMillis();
				//portlet模块文件耗时{}秒({})
				EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.portletmodule")+"({})",(timeG-timeF)*1.0/1000,up.getModuleCode());
				EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.portletmodule")+"({})",(timeG-timeF)*1.0/1000,up.getModuleCode());

				// 国际化初始化后进行菜单助记码
				menuInfoService.batchDealMenuInfoMne(module.getCode(), module.getArtifact());
			}
			if (uploadCustomCode != null && uploadCustomCode ) {
				// 处理自定义代码
				if(!isNewGenerate){
					dealCustomCode(module,null,base, file);
				}else{
					dealPomCustomCode(module);
				}
			}
			timeK = System.currentTimeMillis();
			//"处理自定义代码耗时{}秒({})
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.processingcustomcode")+"({})",(timeK-timeG)*1.0/1000,up.getModuleCode());
			EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.processingcustomcode")+"({})",(timeK-timeG)*1.0/1000,up.getModuleCode());

			//导入调度模板
			File scheduleJobFile = new File(base + File.separator + up.getModuleCode() + File.separator + "service" + File.separator + "src" + File.separator + "generated" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap" + File.separator + "schedulerJob.xml");
			if (!scheduleJobFile.exists()) {
				scheduleJobFile = new File(base + File.separator + up.getModuleCode() + File.separator + "service" + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "bap" + File.separator + "schedulerJob.xml");
			}
			if (scheduleJobFile.exists()) {
				String xml = org.apache.commons.io.FileUtils.readFileToString(scheduleJobFile, "UTF-8");
				if (!StringUtils.isEmpty(xml)) {
					try {
						String code = XmlUtils.getTagContent(xml, "code");
						if (!code.isEmpty()) {
							schedulerJobDTOS = schedulerService.importXml(xml);
						}
					} catch (Exception e) {
						log.error(e.getMessage(), e);
						//						throw new EcException(EcException.Code.SCHEDULER_TEMPLATE_FILE_DEAL_ERROR);
					}
				}
			}

			timeL = System.currentTimeMillis();
			//EC_ENTITY_MODIFY_INFO中插入数据
//			ecGenerateInfoService.saveEntityModifyInfo(null, module.getCode());
//			dataGroupDao.createNativeQuery("UPDATE EC_MODULE SET IS_READ_ONLY=0 WHERE CODE=?",module.getCode()).executeUpdate();
			List<Module> needModifyModuleGenes = new ArrayList<Module>();
			needModifyModuleGenes.add(module);
			//开始批量保存模块信息数据的最后修改时间
			if(isChangeVersion){
				List<Module> associatedModules = getAllAssociated(module,true);//找到相关联的模块
				needModifyModuleGenes.addAll(associatedModules);
			}
//			Iterator<Module> needModifyModuleGeneIterator = needModifyModuleGenes.iterator();
//			while(needModifyModuleGeneIterator.hasNext()){
//				Module moduleCode = needModifyModuleGeneIterator.next();
//				ModuleGenerateInfo generateInfo = moduleGenerateInfoService.getModuleGenerateInfoByModuleCode(moduleCode.getCode());
//				if(generateInfo!=null){
//					generateInfo.setLastModifyTime(new Date());
//					moduleGenerateInfoService.save(generateInfo,Thread.currentThread());
//				}else{
//					generateInfo = new ModuleGenerateInfo();
//					generateInfo.setLastModifyTime(new Date());
//					generateInfo.setModuleCode(moduleCode.getCode());
//					moduleGenerateInfoService.create(generateInfo,Thread.currentThread());
//				}
//			}

//			if (MIS.equals(module.getType())) {
//				String msServiceCode = "BasicMs";
//				if (msServiceList != null && !msServiceList.isEmpty()) {
//					msServiceCode = msServiceList.get(0)[1].toString();
//				}
//				// 插入微服务模块信息，与依赖模块挂在相同服务下
//				String addMsModuleRelationSql = "insert into EC_MSMODULE_RELATION (CODE, MSMODULE_CODE, VALID, EC_ENV, CREATE_TIME, CREATE_STAFF_ID, VERSION) values (?,?,1,?,?,?,0)";
//				for (String relationCode : relationLists) {
//					msModuleRelationDAO.createNativeQuery(addMsModuleRelationSql, relationCode, msServiceCode, module.getEcEnv().name(), new Date(), uploadInfo.getUploadStaff().getId()).executeUpdate();
//				}
//			}

			uploadInfo.setUploadState("success");
			totalTime = (System.currentTimeMillis()-moduleUploadStart)/1000;
			uploadInfo.setUploada(((timeA-moduleUploadStart)*1.0/1000)+"");
			uploadInfo.setUploadb(((timeB-timeA)*1.0/1000)+"");
			uploadInfo.setUploadc(((timeC-timeI)*1.0/1000)+"");
			uploadInfo.setUploadd(((timeD-timeC)*1.0/1000)+"");
			uploadInfo.setUploade(((timeE-timeD)*1.0/1000)+"");
			uploadInfo.setUploadf(((timeF-timeE)*1.0/1000)+"");
			uploadInfo.setUploadg(((timeG-timeF)*1.0/1000)+"");
			uploadInfo.setUploadh(((timeH-timeB)*1.0/1000)+"");
			uploadInfo.setUploadi(((timeI-timeH)*1.0/1000)+"");
			uploadInfo.setUploadk(((timeK-timeG)*1.0/1000)+"");
			uploadInfo.setUploadl(((timeL-timeK)*1.0/1000)+"");
			uploadInfo.setTotalTime(totalTime.toString());
			uploadInfoService.save(uploadInfo);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			uploadInfo.setUploadState("failed");
			totalTime = (System.currentTimeMillis()-moduleUploadStart)/1000;
			timeK = System.currentTimeMillis();
			uploadInfo.setUploada(((timeA-moduleUploadStart)*1.0/1000)+"");
			uploadInfo.setUploadb(((timeB-timeA)*1.0/1000)+"");
			uploadInfo.setUploadc(((timeC-timeI)*1.0/1000)+"");
			uploadInfo.setUploadd(((timeD-timeC)*1.0/1000)+"");
			uploadInfo.setUploade(((timeE-timeD)*1.0/1000)+"");
			uploadInfo.setUploadf(((timeF-timeE)*1.0/1000)+"");
			uploadInfo.setUploadg(((timeG-timeF)*1.0/1000)+"");
			uploadInfo.setUploadh(((timeH-timeB)*1.0/1000)+"");
			uploadInfo.setUploadi(((timeI-timeH)*1.0/1000)+"");
			uploadInfo.setUploadk(((timeK-timeG)*1.0/1000)+"");
			uploadInfo.setUploadl(((timeL-timeK)*1.0/1000)+"");
			uploadInfo.setTotalTime(totalTime.toString());
			uploadInfoService.save(uploadInfo);
			//结束上载已存在模块,模块名为：{},报错信息为{}
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.endupload"),up.getModuleName(), e.toString());
			EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.endupload"),up.getModuleName(), e.toString());
			EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.uploadtime"),up.getModuleName(),(timeK-moduleUploadStart)*1.0/1000);
			//EcUtils.uploadLogger.info("{}模块文件总耗时{}秒",up.getModuleName(),(timeK-moduleUploadStart)*1.0/1000);
			throw e;
		}
		EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.enduploadname"),up.getModuleName());
		EcUtils.uploadLogger.info(InternationalResource.get("ec.model.upload.enduploadname"),up.getModuleName());
		timeK = System.currentTimeMillis();
		EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.uploadtime"),up.getModuleName(),(timeK-moduleUploadStart)*1.0/1000);
		//EcUtils.uploadLogger.info("{}模块文件总耗时{}秒",up.getModuleName(),(timeK-moduleUploadStart)*1.0/1000);
		uploadManager.updatetaskProggress(UploadTaskProggress.UPLAODTASK_IMPORTOTHER);
		//因为无法回滚，最后再调用调度的feign接口
		if(!ObjectUtils.isEmpty(schedulerJobDTOS)){
			iSchedulerJobApiService.scheduleAdd(schedulerJobDTOS);
		}
		return "success";
	}

	@Override
	public List<Module> getModuleByArtifact(String artifact) {
		String hql = " from Module as m where m.artifact = ?0";
		List<Module> module = moduleDao.findByHql(hql, new Object[] { artifact });
		return module;
	}

	@Override
	public String checkModifyModulesState(String moduleCodes) {
		return null;
//		StringBuffer message = new StringBuffer();
//		String[] moduleCode = moduleCodes.split(",");
//		boolean isExistModule = false;
//		if(moduleCode != null && moduleCode.length>0){
//			for(String code : moduleCode){
//				ModuleGenerateInfo generateInfo = moduleGenerateInfoService.getModuleGenerateInfoByModuleCode(code);
//				DeployInfo deployInfo = deployInfoService.getLastestGenerateInfo(code);
//				if(generateInfo!=null && deployInfo!=null){
//					Date modifiedModuleTime = generateInfo.getLastModifyTime();
//					Date deployInfoTime = deployInfo.getCreateTime();
//					if(deployInfoTime.compareTo(modifiedModuleTime)<0){
//						Module module = getModule(code);
//						message.append(",").append(InternationalResource.get(module.getName())+"("+code+")");
//						isExistModule = true;
//					}
//				}
//			}
//			if(isExistModule){
//				message.append(InternationalResource.get("ec.module.download.ungenerate"));
//			}
//		}
//		return !message.toString().equals("") ? message.toString().substring(1) : message.toString();
	}

	@Override
	public void saveModuleCompanyRef(Module module) {
		if (supos){
			return;
		}
		//删除所有该模块数据
		String delSql = "delete from " + ModuleCompanyRef.TABLE_NAME + " where MODULE_CODE = ?";
		jdbcTemplate.update(delSql,module.getCode());
		String companyIds_str = module.getCompanyIds();
		if (ObjectUtils.isEmpty(companyIds_str)){
			throw new EcException(InternationalResource.get("ec.module.company.not.null"));
		}
		List<Long> companyIds = Arrays.asList(companyIds_str.split(",")).stream().map(cid -> Long.parseLong(cid)).collect(Collectors.toList());
		if (ObjectUtils.isEmpty(companyIds)){
			throw new EcException(InternationalResource.get("ec.module.company.not.null"));
		}
		List<Object[]> moduleCompanyRefs = companyIds.stream().map(cid -> new Object[]{IDGenerator.newInstance().generate().longValue(),module.getCode(),cid}).collect(Collectors.toList());
		String sql = "insert into " + ModuleCompanyRef.TABLE_NAME + " (ID,MODULE_CODE,COMPANY_ID) values (?,?,?)";
		jdbcTemplate.batchUpdate(sql,moduleCompanyRefs);
	}

	@Override
	public void saveModuleCompanyRefAllCompany(Module module) {
		List<Company> companies = companyService.getAllCompanies();
		if (ObjectUtils.isEmpty(companies)){
			return;
		}
		Object[] objects = {IDGenerator.newInstance().generate().longValue(), module.getCode(), -1L};
		String sql = "insert into " + ModuleCompanyRef.TABLE_NAME + " (ID,MODULE_CODE,COMPANY_ID) values (?,?,?)";
		jdbcTemplate.update(sql,objects);
	}

	@Override
	public boolean existCompanyRef(Module module) {
		List<ModuleCompanyRef> companyRefs = moduleCompanyRefDao.findByCriteria(Restrictions.eq("moduleCode", module.getCode()));
		log.info("--------------判断："+ !ObjectUtils.isEmpty(companyRefs));
		return !ObjectUtils.isEmpty(companyRefs);
	}

	@Override
	public List<Long> findCompaniesByModuleCode(String moduleCode) {
		List<ModuleCompanyRef> moduleCompanyRefList = moduleCompanyRefDao.findByCriteria(Restrictions.eq("moduleCode", moduleCode));
		if (!ObjectUtils.isEmpty(moduleCompanyRefList)){
			return moduleCompanyRefList.stream().map(ModuleCompanyRef::getCompanyId).collect(Collectors.toList());
		}
		return new ArrayList<>();
	}

	private List<String> getAllRelationModules(Module relationModule, List<String> relationLists) {
		if (relationLists == null) {
			relationLists = new ArrayList<String>();
		}
		if (relationModule == null) {
			return relationLists;
		}
		if (!relationLists.contains(relationModule.getCode())) {
			relationLists.add(relationModule.getCode());
		}
		List<ModuleRelation> tmpRelations = getRelations(relationModule);
		for (ModuleRelation tmpRelation : tmpRelations) {
			if (tmpRelation != null) {
				getAllRelationModules(tmpRelation.getTarget(), relationLists);
			}
		}
		return relationLists;
	}

	/**
	 * 处理菜单方法，若当前环境没有该菜单则不拼接，若当前环境有对应菜单则不重复添加
	 * @param menustr
	 * @param oldmenustr
	 * @return
	 */
	private String dealAppMenu(String menustr,String oldmenustr) {
		if("null".equals(oldmenustr)){
			oldmenustr = "";
		}
		String[] menus = menustr.split(",");
		String[] oldmenus = oldmenustr.split(",");
		for(String menu:menus){
			log.info("app上载菜单处理:"+menu+"   "+oldmenustr);
			boolean hasmenu = false;
			for (String m:oldmenus){
				if(m.equals(menu)){
					hasmenu = true;
					break;
				}
			}
			if(!hasmenu && null != menuInfoService.getMenuInfoByCode(menu) ){
				if(oldmenustr==null ||oldmenustr.equals("") || oldmenustr.equals("null")){
					oldmenustr=menu;
				}else{
					oldmenustr = menu+","+oldmenustr;
				}
				log.info("oldmenustr:"+oldmenustr);
			}
		}
		if(oldmenustr.endsWith(",")){
			oldmenustr.substring(0,oldmenustr.length()-1);
		}
		return oldmenustr;
	}
	private void importApp(String xml,String moduleXml) {
		String insertSql = "insert into supos_app (code, name, app_type, memory, modules, main_app_code, menus) values (?, ?, ?, ?, ?, ?, ?)";
		String slaveInsertSql = "insert into supos_app (code, name, app_type, main_app_code, menus) values (?, ?, ?, ?, ?)";
		String mergeSql = "update supos_app set name=?, app_type=?, memory=?, modules=?, main_app_code=?, menus=? where code=?";
		String selectSql = "select code, name, app_type, memory, modules, main_app_code, menus from supos_app where code=?";
		String selectAppByModuleSql = "select code, name, app_type, memory, modules, main_app_code, menus from supos_app where modules like ? and app_type = 0";
		String updateModulesMenus = "update supos_app set  modules=?, menus=?,main_app_code=? where code=?";
		Document document= null;
		String relationstr = null;
		try {
			relationstr = XmlUtils.getTagContent(moduleXml, "relations");
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		List<Map<String, Object>> relationsAppList =new ArrayList<>();
		String[] relations = new String[0];
		if (relationstr != null && !relationstr.isEmpty() && !relationstr.equals("")) {
			relations = relationstr.split(",");
			for (String relation : relations) {
				relationsAppList = jdbcTemplate.queryForList(selectAppByModuleSql, new Object[]{'%' + relation + '%'});
				if (null != relationsAppList && relationsAppList.size() > 0) {
					break;
				}
			}
		}
		try {
			document = DocumentHelper.parseText(xml);
		} catch (DocumentException e) {
			e.printStackTrace();
			return;
		}
		Element root=document.getRootElement();
		Iterator iterator=root.elementIterator();
		//表示主app未上载
		Boolean mainAppUnupload= false;
		String realMainAppCode ="";
		while(iterator.hasNext()) {
			Element element = (Element) iterator.next();
			String code = element.elementText("code");
			String name = element.elementText("name");
			if (!StringUtils.isEmpty(code) && !StringUtils.isEmpty(code)) {
				String appType = StringUtils.isEmpty(element.elementText("appType"))?"":element.elementText("appType");
				Long memory = StringUtils.isEmpty(element.elementText("memory"))?null:Long.valueOf(element.elementText("memory"));
				String modules = StringUtils.isEmpty(element.elementText("modules"))?"":element.elementText("modules");
				String mainAppCode = StringUtils.isEmpty(element.elementText("mainAppCode"))?"":element.elementText("mainAppCode");
				String menus = StringUtils.isEmpty(element.elementText("menus"))?"":element.elementText("menus");
				List<Map<String, Object>> appList = jdbcTemplate.queryForList(selectSql, new Object[]{code});
				List<Map<String, Object>> moduleAppList = jdbcTemplate.queryForList(selectAppByModuleSql, new Object[]{'%'+modules+'%'});
				if (moduleAppList != null && !moduleAppList.isEmpty() && (appList==null ||appList.isEmpty() || !appList.get(0).get("code").equals(moduleAppList.get(0).get("code"))) && "0".equals(appType)) {
					//若当前用户上载的服务已经被别的app引用则在引用的app中新增菜单
					Map<String, Object> appMap = moduleAppList.get(0);
					menus=dealAppMenu(menus, String.valueOf( appMap.get("menus")));
					mainAppUnupload = true;
					realMainAppCode = String.valueOf(appMap.get("code"));
					jdbcTemplate.update(updateModulesMenus, appMap.get("modules"), menus,mainAppCode, appMap.get("code"));
				}else{
					//r如果模块依赖的模块已经关联app
					if(relationsAppList !=null && !relationsAppList.isEmpty()){
						Map<String, Object> appMap = relationsAppList.get(0);
						if ("0".equals(appType)&& String.valueOf(appMap.get("app_type")).equals(appType)) {
							String moduleCode = String.valueOf(appMap.get("modules"));
							if (!StringUtils.isEmpty(modules) && !StringUtils.isEmpty(moduleCode) && !moduleCode.contains(modules)) {
								modules = moduleCode + "," + modules;
							}else{
								modules = moduleCode;
							}

							if(relations.length>0){
								for(String relation:relations){
									if(!modules.contains(relation)){
										modules = relation + "," + modules;
									}
								}
							}
						}else if(!String.valueOf(appMap.get("app_type")).equals(appType)){
							log.error("app上载失败，原因是 "+code+" 中app主从状态与上载模块app主从状态不一致，请手动添加app");
							break;
						}
						//处理菜单 查当前环境是否有待上载的菜单
						menus=dealAppMenu(menus, String.valueOf( appMap.get("menus")));

						jdbcTemplate.update(updateModulesMenus, modules, menus,mainAppCode, appMap.get("code"));
					}else if (appList != null && !appList.isEmpty()) {
						Map<String, Object> appMap = appList.get(0);
						if ("0".equals(appType)&& String.valueOf(appMap.get("app_type")).equals(appType)) {
							String moduleCode = String.valueOf(appMap.get("modules"));
							if (!StringUtils.isEmpty(modules) && !StringUtils.isEmpty(moduleCode) && !moduleCode.contains(modules)) {
								modules = moduleCode + "," + modules;
							}else{
								modules = moduleCode;
							}
							if(relations.length>0){
								for(String relation:relations){
									if(!modules.contains(relation)){
										modules = relation + "," + modules;
									}
								}
							}
						}else if("1".equals(appType) && mainAppUnupload && String.valueOf(appMap.get("app_type")).equals(appType)){
//							log.info("从app："+code+"与主app的关联 关系不一致，从app上载失败");
							mainAppCode = realMainAppCode;
						}else if(!String.valueOf(appMap.get("app_type")).equals(appType)){
							log.error("app上载失败，原因是 "+code+" 中app主从状态与上载模块app主从状态不一致，请手动添加app");
							break;
						}
						menus=dealAppMenu(menus, String.valueOf( appMap.get("menus")));
						jdbcTemplate.update(updateModulesMenus, modules, menus, mainAppCode, code);
					} else {
						if("1".equals(appType) && mainAppUnupload){
							mainAppCode = realMainAppCode;
						}
                        menus=dealAppMenu(menus, "");
						if(relations.length>0){
							for(String relation:relations){
								if(!modules.contains(relation)){
									modules = relation + "," + modules;
								}
							}
						}
						if("1".equals(appType)){
							jdbcTemplate.update(slaveInsertSql, code, name, appType, mainAppCode, menus);

						}else{
							jdbcTemplate.update(insertSql, code, name, appType, memory, modules, mainAppCode, menus);
						}
					}
				}
			}
		}
	}

	private void dealThreeJarFile(String unZipPath, String moduleCode) {
		// 第三方jar包路径
		File threeJarFile = new File(unZipPath + File.separator + moduleCode + File.separator + "maven");
		// 不存在第三方jar包，则直接返回
		if (!threeJarFile.exists()) {
			return;
		}
		File mavenFile = new File(PropertyHolder.get().getRepositoryPath());
		File workSpaceMavenFile = new File(PropertyHolder.get().getWorkspacePath() + "/maven/" + moduleCode);
		// 如果第三方jar包存在，则处理第三方jar包
		try {
			if (!mavenFile.exists()) {
				mavenFile.mkdirs();
			}
			// 拷贝第三方jar包到maven仓库
			org.apache.commons.io.FileUtils.copyDirectory(threeJarFile, mavenFile);
			// 拷贝第三方jar包到workSpace
			org.apache.commons.io.FileUtils.copyDirectory(threeJarFile, workSpaceMavenFile);
		} catch (Exception e) {
			// 拷贝文件发生异常时，打出log，不做处理
			log.error(e.getMessage(), e);
		} finally {
			// 拷贝完成后删除模块里的第三方jar包
			try {
				org.apache.commons.io.FileUtils.deleteDirectory(threeJarFile);
			} catch (Exception e) {
				// 拷贝文件发生异常时，打出log，不做处理
				log.error(e.getMessage(), e);
			}
		}
	}

	private void dealCustomCode(Module module,List<String> entityCodes, String base, File directory) throws IOException {
		// 解决qcbug-6425:【与BAP集成】上载X6基础模块时，会有自定义代码没有上载成功的情况
		// 在重新加载module时，要清掉1级缓存中的module对象
		dataGroupDao.flush();
		dataGroupDao.clear();
		module = getModule(module.getCode(), true);
		List<Entity> entities = entityService.findEntities(module);
		Set<CustomCode> ccs = new HashSet<CustomCode>();
		if (!entities.isEmpty()) {
			for (Entity entity : entities) {
				if(entityCodes!=null&&!entityCodes.contains(entity.getCode())){
					continue;
				}
				if(null == entity.getModule()){
					entity.setModule(module);
				}
				Set<View> allEntityViews=entity.getViews();
				List<Model> models = modelService.findModels(entity);
				if (!models.isEmpty()) {
					for (Model model : models) {
						if(null == model.getEntity()){
							model.setEntity(entity);
						}
						List<View> views = new ArrayList<View>();
						for(View v:allEntityViews){
							if(v.getAssModel().getCode().equals(model.getCode())){
								views.add(v);
							}
						}

						//List<View> views =viewMaps.get(model.getCode());//原先的代码，有效率问题 List<View> views =viewService.findViews(model);
						String coreBasePath = getBasePath(base, entity, "core");
						String serviceBasePath = getBasePath(base, entity, "service");

						/*
						 * @author qianyong
						 * @本版本注释测试用例的代码
						 * */
						//String testBasePath = getTestBasePath(base, entity, "test");
						String customPrefix = null;
						if(model.getEcVersion() != null && model.getEcVersion().equals("1.0")) {
							customPrefix = model.getModelName();
						} else {
							customPrefix = model.getJpaName();
						}
						String RegisterPrefix=module.getArtifact().substring(0, 1).toUpperCase() + module.getArtifact().substring(1)+"RegisterNacosLicense";
						String entityPath = coreBasePath + "entities" + File.separator + customPrefix + ".java";
						String servicePath = coreBasePath + "services" + File.separator + customPrefix + "Service.java";
						String cxfservicePath = coreBasePath + "services" + File.separator + customPrefix + "CxfService.java";
						String daoPath = serviceBasePath + "daos" + File.separator + customPrefix + "Dao.java";
						String daoimplPath = serviceBasePath + "daos" + File.separator + "impl" + File.separator + customPrefix
								+ "DaoImpl.java";
						String serviceimplPath = serviceBasePath + "services" + File.separator + "impl" + File.separator
								+ customPrefix + "ServiceImpl.java";
						String RegisterNacosLicensePath = serviceBasePath + "services" + File.separator + "impl" + File.separator
								+ RegisterPrefix + ".java";
						String cxfserviceimplPath = serviceBasePath + "services" + File.separator + "impl" + File.separator
								+ customPrefix + "CxfServiceImpl.java";
						String varproviderPath = serviceBasePath + "services" + File.separator + "impl" + File.separator
								+ customPrefix + "VariablesProvider.java";
						String actionPath = serviceBasePath + "actions" + File.separator + customPrefix + "Action.java";
						//String serviceTestPath = testBasePath + "services/testing" + File.separator + customPrefixServiceTests.java";
						//String serviceTestingResourcesDataPath = getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "test"
						//		+ File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator
						//		+ "META-INF" + File.separator + "basedata" + File.separator + customPrefix + ".xml";

						ccs.addAll(fetchCustomCode(entityPath, "java", model.getCode(), entity.getCode(), module.getCode()));
						ccs.addAll(fetchCustomCode(daoPath, "java", model.getCode(), entity.getCode(), module.getCode()));
						ccs.addAll(fetchCustomCode(daoimplPath, "java", model.getCode(), entity.getCode(), module.getCode()));
						ccs.addAll(fetchCustomCode(servicePath, "java", model.getCode(), entity.getCode(), module.getCode()));
						ccs.addAll(fetchCustomCode(serviceimplPath, "java", model.getCode(), entity.getCode(), module.getCode()));
						ccs.addAll(fetchCustomCode(RegisterNacosLicensePath, "java", model.getCode(), entity.getCode(), module.getCode()));
						ccs.addAll(fetchCustomCode(cxfservicePath, "java", model.getCode(), entity.getCode(), module.getCode()));
						ccs.addAll(fetchCustomCode(cxfserviceimplPath, "java", model.getCode(), entity.getCode(), module.getCode()));
						ccs.addAll(fetchCustomCode(varproviderPath, "java", model.getCode(), entity.getCode(), module.getCode()));
						ccs.addAll(fetchCustomCode(actionPath, "java", model.getCode(), entity.getCode(), module.getCode()));
						//ccs.addAll(fetchCustomCode(serviceTestPath, "java", model.getCode(), entity.getCode(), module.getCode()));
						//ccs.addAll(fetchCustomCode(serviceTestingResourcesDataPath, "html", model.getCode(), entity.getCode(),
						//	module.getCode()));

						org.apache.commons.io.FileUtils.deleteQuietly(new File(entityPath));
						org.apache.commons.io.FileUtils.deleteQuietly(new File(coreBasePath + "entities" + File.separator + customPrefix
								+ "DealInfo.java"));
						org.apache.commons.io.FileUtils.deleteQuietly(new File(coreBasePath + "entities" + File.separator + customPrefix
								+ "GroupInfo.java"));
						org.apache.commons.io.FileUtils
								.deleteQuietly(new File(coreBasePath + "entities" + File.separator + customPrefix + "MneCode.java"));
						org.apache.commons.io.FileUtils.deleteQuietly(new File(daoPath));
						org.apache.commons.io.FileUtils.deleteQuietly(new File(daoimplPath));
						org.apache.commons.io.FileUtils.deleteQuietly(new File(servicePath));
						org.apache.commons.io.FileUtils.deleteQuietly(new File(serviceimplPath));
						org.apache.commons.io.FileUtils.deleteQuietly(new File(RegisterNacosLicensePath));
						org.apache.commons.io.FileUtils.deleteQuietly(new File(cxfservicePath));
						org.apache.commons.io.FileUtils.deleteQuietly(new File(cxfserviceimplPath));
						org.apache.commons.io.FileUtils.deleteQuietly(new File(varproviderPath));
						org.apache.commons.io.FileUtils.deleteQuietly(new File(actionPath));
						//FileUtils.deleteQuietly(new File(serviceTestPath));
						//FileUtils.deleteQuietly(new File(serviceTestingResourcesDataPath));

						if (!views.isEmpty()) {
							String viewBasePath = getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "service" + File.separator
									+ "src" + File.separator + "main" + File.separator + "resources" + File.separator + "views"
									+ File.separator + model.getEntity().getModule().getArtifact() + File.separator
									+ model.getEntity().getEntityName() + File.separator + firstLatterToLowerCase(model.getModelName())
									+ File.separator;

							for (View v : views) {
								String fileName = viewBasePath + v.getName();
								if((v.getMobile() != null && v.getMobile()) && (v.getMobileEnableFlag() != null && v.getMobileEnableFlag())){
									fileName = viewBasePath + v.getName().replace(View.MOBILE_VIEW_SUFFIX, "") + "-mobile";
								}
								ccs.addAll(fetchCustomCode(fileName + ".ftl", "html", model.getCode(), entity.getCode(),
										module.getCode()));
								ccs.addAll(fetchCustomCode(fileName + ".ftl", "java", model.getCode(), entity.getCode(),
										module.getCode()));
								if (null != v.getCustomFlag() && !v.getCustomFlag()) {
									org.apache.commons.io.FileUtils.deleteQuietly(new File(fileName + ".ftl"));
								}
							}

						}

						// 新的微服务模块加入controller等java文件的自定义代码
						if (MIS.equals(module.getType())) {
							String apiBasePath = getBasePath(base, entity, "api");
							String controllerPath = serviceBasePath + "controllers" + File.separator + customPrefix + "Controller.java";
							String providerPath = serviceBasePath + "provider" + File.separator + customPrefix + "Client.java";
							String providerImplPath = serviceBasePath + "provider" + File.separator + "wrapper" + File.separator
									+ customPrefix + "Wrapper.java";
							String dtoPath = apiBasePath + "DTO" + File.separator + customPrefix + "DTO.java";
							String clientPath = apiBasePath + "client" + File.separator + "I" + customPrefix + "Client.java";
							String fallbackPath = apiBasePath + "client" + File.separator + "fallback" + File.separator
									+ customPrefix + "ClientFallBack.java";
							ccs.addAll(fetchCustomCode(controllerPath, "java", model.getCode(), entity.getCode(), module.getCode()));
							ccs.addAll(fetchCustomCode(providerPath, "java", model.getCode(), entity.getCode(), module.getCode()));
							ccs.addAll(fetchCustomCode(providerImplPath, "java", model.getCode(), entity.getCode(), module.getCode()));
							ccs.addAll(fetchCustomCode(dtoPath, "java", model.getCode(), entity.getCode(), module.getCode()));
							ccs.addAll(fetchCustomCode(clientPath, "java", model.getCode(), entity.getCode(), module.getCode()));
							ccs.addAll(fetchCustomCode(fallbackPath, "java", model.getCode(), entity.getCode(), module.getCode()));
						}
					}
				}
			}
		}
		// 读取配置文件中的

		String serviceSpringPath = getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "service" + File.separator + "src"
				+ File.separator + "main" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "spring"
				+ File.separator + "module-context.xml";
		String modulePomPath = getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "pom.xml";
		String corePomPath = getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "core" + File.separator + "pom.xml";
		String servicePomPath = getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "service" + File.separator + "pom.xml";
		String serviceOsgiPath = getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "service" + File.separator + "src"
				+ File.separator + "main" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "spring"
				+ File.separator + "osgi-context.xml";
		//String testPomPath = getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "test" + File.separator + "pom.xml";
		// String serviceTestingResourcesDataPath = getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "test" +
		// File.separator + "src" + File.separator + "test" +
		// File.separator
		// + "resources" + File.separator + "META-INF" + File.separator + "basedata" + File.separator +
		// module.getArtifact()+".xml";
		//String serviceTestingResourcesTestPath = getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "test" + File.separator
		//		+ "src" + File.separator + "test" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "test"
		//		+ File.separator + "test-context.xml";

		String strutsPath = getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "service" + File.separator + "src"
				+ File.separator + "main" + File.separator + "resources" + File.separator + "struts.xml";

		ccs.addAll(fetchCustomCode(strutsPath, "html", "", "", module.getCode()));

		ccs.addAll(fetchCustomCode(modulePomPath, "html", "", "", module.getCode()));
		ccs.addAll(fetchCustomCode(corePomPath, "html", "", "", module.getCode()));
		ccs.addAll(fetchCustomCode(servicePomPath, "html", "", "", module.getCode()));
		ccs.addAll(fetchCustomCode(serviceSpringPath, "html", "", "", module.getCode()));
		ccs.addAll(fetchCustomCode(serviceOsgiPath, "html", "", "", module.getCode()));
		//ccs.addAll(fetchCustomCode(testPomPath, "html", "", "", module.getCode()));
		// ccs.addAll(fetchCustomCode(serviceTestingResourcesDataPath, "html", "", "", module.getCode()));
		//ccs.addAll(fetchCustomCode(serviceTestingResourcesTestPath, "html", "", "", module.getCode()));
		org.apache.commons.io.FileUtils.deleteQuietly(new File(corePomPath));
		org.apache.commons.io.FileUtils.deleteQuietly(new File(servicePomPath));
		org.apache.commons.io.FileUtils.deleteQuietly(new File(serviceSpringPath));
		org.apache.commons.io.FileUtils.deleteQuietly(new File(serviceOsgiPath));
		org.apache.commons.io.FileUtils.deleteQuietly(new File(strutsPath));
		org.apache.commons.io.FileUtils.deleteQuietly(new File(getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "core" + File.separator
				+ "template.mf"));
		org.apache.commons.io.FileUtils.deleteQuietly(new File(getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "service" + File.separator
				+ "template.mf"));
		org.apache.commons.io.FileUtils.deleteQuietly(new File(getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "core" + File.separator + "target"));
		org.apache.commons.io.FileUtils
				.deleteQuietly(new File(getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "service" + File.separator + "target"));
		org.apache.commons.io.FileUtils.deleteQuietly(new File(getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "core" + File.separator + "bin"));
		org.apache.commons.io.FileUtils.deleteQuietly(new File(getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "service" + File.separator + "bin"));
		org.apache.commons.io.FileUtils
				.deleteQuietly(new File(getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "core" + File.separator + ".settings"));
		org.apache.commons.io.FileUtils.deleteQuietly(new File(getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "service" + File.separator
				+ ".settings"));
		org.apache.commons.io.FileUtils
				.deleteQuietly(new File(getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "core" + File.separator + ".classpath"));
		org.apache.commons.io.FileUtils.deleteQuietly(new File(getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "service" + File.separator
				+ ".classpath"));
		org.apache.commons.io.FileUtils.deleteQuietly(new File(getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "core" + File.separator + ".project"));
		org.apache.commons.io.FileUtils.deleteQuietly(new File(getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "service" + File.separator
				+ ".project"));


		//TODO customCodeService.batchSave(ccs.toArray(new CustomCode[ccs.size()]));
		customCodeService.batchDelete(module.getCode());
		List<CustomCode> lists=new ArrayList<CustomCode>();
		lists.addAll(ccs);
		customCodeService.batchSave(lists);

		//customCodeService.save(ccs.toArray(new CustomCode[ccs.size()]));

		// 新增文件复制到workspace
		org.apache.commons.io.FileUtils.deleteQuietly(new File(getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "module.xml"));
		org.apache.commons.io.FileUtils.deleteQuietly(new File(getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "pom.xml"));
		//FileUtils.deleteQuietly(new File(getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "test"));
		org.apache.commons.io.FileUtils.deleteQuietly(new File(getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + ".project"));
		org.apache.commons.io.FileUtils.deleteQuietly(new File(getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + ".settings"));

		File dist = new File(PropertyHolder.get().getWorkspacePath() + File.separator + "customFile" + File.separator + module.getCode());
		if(directory!=null){
			org.apache.commons.io.FileUtils.copyDirectory(directory, dist);
		}


	}

	private void dealPomCustomCode(Module module) throws IOException {
		Set<CustomCode> ccs = new HashSet<CustomCode>();
		String modulePomPath = getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "pom.xml";
		String corePomPath = getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "core" + File.separator + "pom.xml";
		String servicePomPath = getBasePath(PropertyHolder.get().getGeneratePath() + File.separator + "unziped", module) + "service" + File.separator + "pom.xml";

		ccs.addAll(fetchCustomCode(modulePomPath, "html", "", "", module.getCode()));
		ccs.addAll(fetchCustomCode(corePomPath, "html", "", "", module.getCode()));
		ccs.addAll(fetchCustomCode(servicePomPath, "html", "", "", module.getCode()));

		customCodeService.batchDelete(module.getCode());
		List<CustomCode> lists=new ArrayList<CustomCode>();
		lists.addAll(ccs);
		customCodeService.batchSave(lists);
	}

	private Set<CustomCode> fetchCustomCode(String path, String type, String modelCode, String entityCode, String moduleCode)
			throws IOException {
		File f = new File(path);
		if (!f.exists()) {
			return Collections.EMPTY_SET;
		}
		String source = org.apache.commons.io.FileUtils.readFileToString(f, "UTF-8");
		String start, end;
		if ("html".equals(type) || "xml".equals(type)) {
			start = "<!-- ";
			end = " -->";
		} else {
			start = "/\\* ";
			end = " \\*/";
		}
		String s = start + "CUSTOM CODE START";
		String e = start + "CUSTOM CODE END" + end;
		Pattern p = Pattern.compile(s + "\\((.+),(.+),(.*),(.+)\\)" + end + "([\\s\\S]*?)" + e);
		Matcher m = p.matcher(source);
		Set<CustomCode> ccs = new HashSet<CustomCode>();
		log.info("=====解析自定义代码，文件：" + path + "===========");
		while (m.find()) {
			String customCode=m.group(5).trim();
			if("// 自定义代码".equals(customCode) || "<!-- 自定义代码 -->".equals(customCode)){
				continue;
			}
			CustomCode cc = new CustomCode();
			if (null == modelCode || modelCode.length() == 0) {
				cc.setCode(m.group(4) + "_" + m.group(1) + "_" + m.group(2));
			} else {
				cc.setCode(m.group(4) + "_" + m.group(3) + "_" + m.group(1) + "_" + m.group(2));
			}
			cc.setType(m.group(1));
			cc.setSubType(m.group(2));
			cc.setModelCode(m.group(3));
			cc.setModuleCode(m.group(4));
			cc.setCustomCode(customCode);
			cc.setEntityCode(entityCode);
			ccs.add(cc);
		}
		return ccs;
	}

	private String getBasePath(String base, Module module) {
		StringBuilder builder = new StringBuilder();
		builder.append(base);
		builder.append(File.separator).append(module.getCode()).append(File.separator);
		return builder.toString();
	}

	private String getBasePath(String base, Entity entity, String type) {
		StringBuilder builder = new StringBuilder();
		builder.append(getBasePath(base, entity.getModule())).append(type).append(File.separator).append("src").append(File.separator)
				.append("main").append(File.separator).append("java").append(File.separator).append("com").append(File.separator)
				.append("supcon").append(File.separator).append("orchid").append(File.separator + entity.getModule().getArtifact())
				.append(File.separator);
		return builder.toString();
	}

	private String firstLatterToLowerCase(String key) {
		char fl = ((String) key).charAt(0);
		return Character.toLowerCase(fl) + ((String) key).substring(1);
	}
	
	public List<Long> getModuleCompanyRef(String moduleId) {
		List<Long> list =new ArrayList<Long>();
		List<Object[]> moduleRefList = moduleCompanyRefDao.createNativeQuery(" SELECT COMPANY_ID,MODULE_CODE FROM module_company_ref WHERE MODULE_CODE=(SELECT code FROM " + Module.TABLE_NAME + " WHERE ARTIFACT= ?)  ",moduleId).list();
		if(null!=moduleRefList && moduleRefList.size()>0) {
			for(Object[] moduleRef :moduleRefList) {					
				list.add(Long.parseLong(moduleRef[0].toString()));
			}
		}		
		return list;
	}

	/**
	 * 删除模块依赖时，存在实际依赖不允许删除
	 * @param module
	 * @param relationCodes
	 * @return
	 */
	private Set checkModuleRelationDelete(Module module, List<String> relationCodes) {
		Set<String> msgs = new HashSet<>();
		List<Property> properties = findPropertiesRelation(module.getCode(), relationCodes);
		for(Property property : properties){
			String msg = InternationalResource.get(module.getName())+ "模块-"
					+ InternationalResource.get(property.getModel().getEntity().getName()) + "实体-"
					+ InternationalResource.get(property.getModel().getName()) + "模型-"
					+ InternationalResource.get(property.getDisplayName()) + "字段";
			msgs.add(msg);
		}
		Set<View> viewSet = new HashSet<View>();
		viewSet.addAll(findFieldsRelation(module.getCode(), relationCodes));
		viewSet.addAll(findExtraViewRelation(module.getCode(), relationCodes));

		for(View view : viewSet){
			String msg = InternationalResource.get(module.getName())+ "模块-"
					+ InternationalResource.get(view.getEntity().getName()) + "实体-"
					+ InternationalResource.get(view.getDisplayName()) + (view.getCode().endsWith("__mobile__")?"移动视图":"视图");
			msgs.add(msg);
		}

		return msgs;
	}

	private List<Property> findPropertiesRelation(String moduleCode, List<String> relationCodes) {
		List<Property> properties = new ArrayList();
		List<Object> args = new LinkedList<Object>();
		args.add(DbColumnType.OBJECT);
		args.add(moduleCode);
		List propertyList = propertyDao.createQuery("from Property where valid=true and type = ?0 and associatedProperty.moduleCode in (:relationCodes) and moduleCode = ?1", args.toArray()).setParameterList("relationCodes", relationCodes).list();
		if (propertyList != null && !propertyList.isEmpty()) {
			properties.addAll(propertyList);
		}

		args = new LinkedList<Object>();
		args.add(DbColumnType.SYSTEMCODE);
		args.add(moduleCode);
		List<Property> propertyAll = propertyDao.createQuery("from Property where valid=true and type = ?0 and moduleCode = ?1", args.toArray()).list();
		if(null != propertyAll && !propertyAll.isEmpty()){
			relationCodes.forEach(relationCode -> {
				String code = relationCode.split("_")[0] + "_";
				for(Property property : propertyAll){
					if (property.getFillcontent().contains(code)) {
						properties.add(property);
					}
				}
			});
		}
		return properties;
	}

	private Set<View> findFieldsRelation(String moduleCode, List<String> relationCodes) {
		Set<View> viewSet = new HashSet<View>();
		String fieldHql = "from Field where moduleCode = ?0 and valid=true";
		List<Object> args = new LinkedList<Object>();
		args.add(moduleCode);
		List<Field> fields = moduleDao.createQuery(fieldHql, args.toArray()).list();
		if (fields != null && !fields.isEmpty()) {
			relationCodes.forEach(relationCode -> {
				for (Field field : fields) {
					if (StringUtils.isEmpty(field.getConfig())) {
						continue;
					}
					if (field.getConfig().contains("<referenceview><![[CDATA[[" + relationCode)
							|| field.getConfig().contains("<linkView><![[CDATA[[" + relationCode)
							||  field.getConfig().contains("<allowviewcode><![[CDATA[[" + relationCode)) {
						if (field.getView() != null) {
							viewSet.add(field.getView());
						}
						if (field.getDataGrid() != null) {
							viewSet.add(field.getDataGrid().getView());
						}
					}
				}
			});
		}
		return viewSet;
	}

	private Set<View> findExtraViewRelation(String moduleCode, List<String> relationCodes) {
		Set<View> viewSet = new HashSet<View>();
		String extraViewHql = "from ExtraView where view.valid=true and view.moduleCode = ?0";
		List<Object> args = new LinkedList<Object>();
		args.add(moduleCode);
		List<ExtraView> extraViews = moduleDao.createQuery(extraViewHql, args.toArray()).list();
		if (extraViews != null && !extraViews.isEmpty()) {
			relationCodes.forEach(relationCode -> {
				for (ExtraView extraView : extraViews) {
					if (StringUtils.isEmpty(extraView.getConfig())) {
						continue;
					}
					if ((extraView.getConfig().contains("<tree_model><![[CDATA[[" + relationCode)
							|| extraView.getConfig().contains("<vcode><![[CDATA[[" + relationCode)
							|| extraView.getConfig().contains("<treeView><![[CDATA[[" + relationCode))) {
						if (extraView.getView() != null) {
							viewSet.add(extraView.getView());
						}
					}
				}
			});
		}
		return viewSet;
	}

	@Override
	public List<String> findModuleRelationAndReferenceCode(String moduleCode) {
		List<String> codes = new ArrayList<>();
		String moduleRelationSql = "SELECT MODULE_CODE FROM ec_module_relation WHERE TARGET_MODULE_CODE = ?";
		String moduleReferenceSql = "SELECT MODULE_CODE FROM ec_module_relation WHERE TARGET_MODULE_CODE = ?";
		List<String> relationCodes = moduleDao.createNativeQuery(moduleRelationSql, moduleCode).list();
		List<String> referenceCodes = moduleDao.createNativeQuery(moduleReferenceSql, moduleCode).list();
		if(null != relationCodes){
			codes.addAll(relationCodes);
		}
		if(null != referenceCodes){
			codes.addAll(referenceCodes);
		}
		codes.add(moduleCode);
		return codes;
	}
}