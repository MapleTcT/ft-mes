package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.base.services.MenuInfoService;
import com.supcon.supfusion.base.services.MenuOperateService;
import com.supcon.supfusion.configuration.services.dao.BackupDataGridDaoImpl;
import com.supcon.supfusion.configuration.services.entity.AssociatedInfo;
import com.supcon.supfusion.configuration.services.entity.Validate;
import com.supcon.supfusion.configuration.services.service.*;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.utils.SerializeUitls;
import com.supcon.supfusion.configuration.services.dao.DataGridDaoImpl;
import com.supcon.supfusion.configuration.services.dao.ModelDaoImpl;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

@Slf4j
@ServiceApiService("ec_DataGridService")
@Transactional
public class DataGridServiceImpl implements DataGridService {
	@Autowired
	private DataGridDaoImpl datagridDao;
	@Autowired
	private ModelDaoImpl modelDao;
	@Autowired
	BackupDataGridDaoImpl backupDataGridDao;
	@Autowired
	private ViewService viewService;
	@Autowired
	private ModelService modelService;
	@Autowired
	private EcConfigService ecConfigService;
	@Autowired
	private FieldService fieldService;
	@Autowired
	private ButtonService buttonService;
	@Autowired
	private CustomerConditionService customerConditionService;
	@Autowired
	private ModuleService moduleService;
	@Autowired
	private MenuOperateService menuOperateService;
	@Autowired
	private MenuInfoService menuInfoService;
	@Autowired
	private EventService eventService;

	//	@Autowired
//	private ModuleGenerateInfoService moduleGenerateInfoService;
	public static final String SPLIT_DOT = ".";

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public DataGrid getDataGrid(String code) {
		Assert.notNull(code);
		DataGrid dataGrid = datagridDao.get(code);
		if(null != dataGrid && null != dataGrid.getFields() && !dataGrid.getFields().isEmpty()){
			for(Field field : dataGrid.getFields()){
				if (!Hibernate.isInitialized(field)){
					Hibernate.initialize(field);
				}
			}
		}
		List<Field> fields = dataGrid.getFields();
		for(Field field : fields){
			if(null != field.getEvents() && !field.getEvents().isEmpty()){
				for(Event event : field.getEvents()){
					if (!Hibernate.isInitialized(event)){
						Hibernate.initialize(event);
					}
				}
			}
			if(null != field.getValidates() && !field.getValidates().isEmpty()){
				for(Validate validate : field.getValidates()){
					if (!Hibernate.isInitialized(validate)){
						Hibernate.initialize(validate);
					}
				}
			}
		}
		List<Button> buttons = dataGrid.getButtons();
		if(null != buttons && !buttons.isEmpty()){
			for(Button button : buttons){
				if (!Hibernate.isInitialized(button)){
					Hibernate.initialize(button);
				}
			}
		}
		if(buttons!= null){
		for(Button button : buttons){
			Hibernate.initialize(button.getEvents());
		}}
		return dataGrid;
	}

	@Override
	public void deleteDataGrid(String code) {
		Assert.notNull(code);
		buttonService.deleteButtonByDataGridCode(code);
		fieldService.deleteFieldByDataGrid(code);
		customerConditionService.deleteByObject(getDataGrid(code));
		datagridDao.delete(code);
	}

	@Override
	public void deleteDataGrid(DataGrid dataGrid) {
		Assert.notNull(dataGrid);
		buttonService.deleteButtonByDataGridCode(dataGrid.getCode());
		fieldService.deleteFieldByDataGrid(dataGrid.getCode());
		customerConditionService.deleteByObject(dataGrid);
		deleteBackupDataGridByDataGrid(dataGrid.getView().getCode());
		datagridDao.delete(dataGrid);
	}

	@Override
	public void deleteDataGridPhysical(DataGrid dataGrid) {
		Assert.notNull(dataGrid);
		buttonService.deleteButtonByDataGridCode(dataGrid.getCode());
		eventService.deleteEvent(dataGrid.getCode() + "_renderOver");
		eventService.deleteEvent(dataGrid.getCode() + "_ptPageInit");
		fieldService.deleteFieldByDataGrid(dataGrid.getCode());
		customerConditionService.deletePhysicalByObject(dataGrid);
		deleteBackupDataGridByDataGridPhysical(dataGrid.getView().getCode());
		datagridDao.deletePhysical(dataGrid);
	}

	@Override
	public void deleteDataGridPhysical(String code) {
		buttonService.deleteButtonByDataGridCode(code);
		fieldService.deleteFieldByDataGrid(code);
		DataGrid dg = getDataGrid(code);
		customerConditionService.deletePhysicalByObject(dg);
		deleteBackupDataGridByDataGridPhysical(dg.getView().getCode());
		datagridDao.deletePhysical(dg);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<DataGrid> getDataGridByViewCode(String viewCode) {
		List<DataGrid> dataGrids = datagridDao.getDataGridByViewCode(viewCode, false);
		return dataGrids;
	}

	@Override
	public Map<String, DataGrid> getDataGridMapByView(View view) {
		List<DataGrid> dataGridByView = getDataGridByView(view, false);
		Map<String, DataGrid> map = new HashMap(dataGridByView.size());
		for (DataGrid dataGrid : dataGridByView) {
			map.put(dataGrid.getCode(), dataGrid);
		}
		return map;
	}

	/**
	 * 
	 * @param view
	 * @param datagrid
	 * @param tagmodelId
	 * @exception 选择了DataGird后保存
	 * @throws NotUniqueException
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<DataGrid> getDataGridByView(View view, boolean noValid) {
		List<DataGrid> dataGrids = datagridDao.getDataGridByViewCode(view.getCode(), noValid);
		if(null != dataGrids && !dataGrids.isEmpty()){
			for(DataGrid dataGrid : dataGrids){
				if(null != dataGrid.getFields() && !dataGrid.getFields().isEmpty()){
					for(Field field : dataGrid.getFields()){
						if (!Hibernate.isInitialized(field)){
							Hibernate.initialize(field);
						}
					}
				}
				List<Field> fields = dataGrid.getFields();
				for(Field field : fields){
					Hibernate.initialize(field.getEvents());
					Hibernate.initialize(field.getValidates());
				}
				List<Button> buttons = dataGrid.getButtons();
				if(null != buttons && !buttons.isEmpty()){
					for(Button button : buttons){
						if (!Hibernate.isInitialized(button)){
							Hibernate.initialize(button);
						}
					}
				}
				if(null != buttons){
				for(Button button : buttons){
					Hibernate.initialize(button.getEvents());
				}}
			}
		}
		return dataGrids;
	}

	@Override
	public void save(DataGrid dg) {
		datagridDao.merge(dg);
	}

	/**
	 * 
	 * @param datagrid
	 * @param view
	 * @param tagmodelId
	 * @exception 选择了DataGird后保存
	 * @throws NotUniqueException
	 */
	@Override
	public void save(DataGrid datagrid, View view, String tagmodelCode) throws EcException {
		datagrid.setFullConfig(null);
		if (null == datagrid.getCode()) {
			datagrid.setView(view);
			datagrid.setTargetModel(modelDao.get(tagmodelCode));
			datagrid.setName("dg" + System.currentTimeMillis());
			datagrid.setCode(view.getCode() + datagrid.getName());
			datagrid.setModuleCode(view.getModuleCode());
			datagrid.setEntityCode(view.getEntity().getCode());
			datagrid.setVersion(0);
			datagridDao.create(datagrid);
		} else {
			// 修改
			datagridDao.update(datagrid);
		}
		datagridDao.save(datagrid);
	}

	/**
	 * 
	 * @param req
	 * @param datagirdId
	 * @param configJson
	 * @exception 选择了DataGird后保存
	 * @throws NotUniqueException
	 * @throws IOException
	 */
	@Override
	public void publish(DataGrid datagrid, Map<String, Object> argsMap) throws EcException, IOException {
		Assert.notNull(datagrid.getCode());
		// Assert.hasText(datagrid.getConfig());

		String fieldConfig = null, delCellIds = null, delEventIds = null, delValidateIds = null, btDelCellIds = null;
		if (null != argsMap && !argsMap.isEmpty()) {
			fieldConfig = argsMap.get("fieldConfig").toString();
			if (null != argsMap.get("delCellIds")) {
				delCellIds = argsMap.get("delCellIds").toString();
			}
			if (null != argsMap.get("delEventIds")) {
				delEventIds = argsMap.get("delEventIds").toString();
			}
			if (null != argsMap.get("delValidateIds")) {
				delValidateIds = argsMap.get("delValidateIds").toString();
			}
			if (null != argsMap.get("btDelCellIds")) {
				btDelCellIds = argsMap.get("btDelCellIds").toString();
			}
		}

		DataGrid datagrid2 = getFullDataGrid(datagrid.getCode());
		datagrid2.setConfig(datagrid.getConfig());
		datagrid2.setEx(datagrid.getEx());
		datagrid2.setDataGridName(datagrid.getDataGridName());
		if (datagrid.getDataGridType() != null) {
			datagrid2.setDataGridType(datagrid.getDataGridType());
		}
		String oldPermissionCode = datagrid2.getPermissionCode();
		if(datagrid.getIsPermission()){
			datagrid2.setIsPermission(true);
			if(null == datagrid.getOperateName() || datagrid.getOperateName().trim().length() == 0){
				throw new EcException("操作名称不允许为空");
			}
			if(null == datagrid.getPermissionCode() || datagrid.getPermissionCode().trim().length() == 0){
				datagrid2.setPermissionCode(datagrid.getName());
			}else{
				datagrid2.setPermissionCode(datagrid.getPermissionCode());
			}
			datagrid2.setOperateName(datagrid.getOperateName());
		}else{
	
			datagrid2.setIsPermission(false);
			datagrid2.setPermissionCode("");
			datagrid2.setOperateName("");
		}
		save(datagrid2, null, null);
		this.saveEvent(datagrid2);
		fieldService.saveFields(datagrid2, fieldConfig, delCellIds, delEventIds, delValidateIds);
		buttonService.saveButton(datagrid2, fieldConfig, btDelCellIds);
		
		BackupView bv = new BackupView();
		bv.setView(datagrid2.getView());
		String viewFieldConfig = ecConfigService.getViewFieldConfigByDataGrid(datagrid2);
		if (null != viewFieldConfig && viewFieldConfig.length() > 0) {
			bv.setFieldConfig(viewFieldConfig);
		}
		if (datagrid2.getView().getExtraView() != null) {
			bv.setConfig(datagrid2.getView().getExtraView().getConfig());
		}
		bv.setCode(datagrid2.getView().getCode() + "_" + new Timestamp(System.currentTimeMillis()));
		viewService.saveBackupView(bv);
		BackupDataGrid bd = new BackupDataGrid();
		bd.setCode(datagrid2.getCode() + "_" + new Timestamp(System.currentTimeMillis()));
		bd.setConfig(datagrid2.getConfig());
		bd.setDgFieldConfig(fieldConfig);
		bd.setBackupView(bv);
		saveBackupDataGrid(bd);
		//列表PT生成权限
		if(datagrid2.getDataGridType() == 1 && !datagrid2.getIsPermission()){
			String permissionCode = (null == oldPermissionCode) ? datagrid2.getName() : oldPermissionCode;
			menuOperateService.deleteMenuOperateByPhysical(datagrid2.getTargetModel().getCode() + "_" + permissionCode);
		}
	}

	private void saveEvent(DataGrid datagrid2) {
		// TODO Auto-generated method stub
		String config = datagrid2.getConfig();
		String code = datagrid2.getCode();
		String moduleCode = datagrid2.getModuleCode();
		String entityCode = datagrid2.getEntityCode();
		if(null != config && config.indexOf("<listProperty>") > 0 && config.indexOf("</listProperty>") > 0) {
			String listPropertyConfig = config.substring(config.indexOf("<listProperty>"), config.indexOf("</listProperty>") + 15);
			Map listPropertyMap = (Map) SerializeUitls.deserialize(listPropertyConfig);
			if(listPropertyMap.containsKey("renderOver") && null != listPropertyMap.get("renderOver")){
				Event e = eventService.getEvent(code + "_renderOver");
				if (null == e) {
					e = new Event();
					e.setVersion(0);
				}
				e.setCode(code + "_renderOver");
				e.setName(code + "_renderOver");
				e.setFunction(listPropertyMap.get("renderOver").toString());
				e.setFunction_es5(listPropertyMap.get("renderOver_es5").toString());
				//e.setButton(button);
				e.setModuleCode(moduleCode);
				e.setEntityCode(entityCode);
				eventService.saveEvent(e);
			}

			if(listPropertyMap.containsKey("ptPageInit") && null != listPropertyMap.get("ptPageInit")){
				Event e = eventService.getEvent(code + "_ptPageInit");
				if (null == e) {
					e = new Event();
					e.setVersion(0);
				}
				e.setCode(code + "_ptPageInit");
				e.setName(code + "_ptPageInit");
				e.setFunction(listPropertyMap.get("ptPageInit").toString());
				e.setFunction_es5(listPropertyMap.get("ptPageInit_es5").toString());
				//e.setButton(button);
				e.setModuleCode(moduleCode);
				e.setEntityCode(entityCode);
				eventService.saveEvent(e);
			}
		}
	}

	/**
	 * 
	 * @param id
	 *            datagrid的ID
	 * @exception 获取DataGrid的全部属性
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public DataGrid getFullDataGrid(String code) {
		Assert.notNull(code);
		DataGrid datagrid = getDataGrid(code);
		if (null != datagrid) {
			datagrid.setCode(code);
			datagrid.setTargetModel(modelService.getModel(datagrid.getTargetModel().getCode()));
			datagrid.setView(viewService.getView(datagrid.getView().getCode()));
			if(null != datagrid.getFields() && !datagrid.getFields().isEmpty()){
				for(Field field : datagrid.getFields()){
					if (!Hibernate.isInitialized(field)){
						Hibernate.initialize(field);
					}
				}
			}
			List<Field> fields = datagrid.getFields();
			for(Field field : fields){
				Hibernate.initialize(field.getEvents());
				Hibernate.initialize(field.getValidates());
			}
			List<Button> buttons = datagrid.getButtons();
			if(null != buttons && !buttons.isEmpty()){
				for(Button button : buttons){
					if (!Hibernate.isInitialized(button)){
						Hibernate.initialize(button);
					}
				}
			}
			if(null != buttons){
			for(Button button : buttons){
				Hibernate.initialize(button.getEvents());
			}
			}
		}
		return datagrid;
	}

	/**
	 * 
	 * @param tagmodelId
	 *            关联模型的ID
	 * @exception 根据关联模型的ID获取关联的字段信息
	 *                ,只取出1对多的关联关系的字段，1的模型字段的数据填充到多的一方 生成的数据格式是model1.id=model2.model1id content :
	 *                ori,获取的对象中key为源模型字段。tag，获取的对象中key为当前模型的字段
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Map<String, String> getAsskeyByTagmodelCode(Model model) {
		List<AssociatedInfo> assinfos = modelService.findAssociatedInfos(model, AssociatedInfo.MANY_TO_ONE, AssociatedInfo.ONE_TO_ONE);
		return createAsskeyWithAssociatedInfos(assinfos);
	}
	
	/**
	 * 根据关联模型的属性来创建n..1和1..1的关联信息
	 * 
	 * @param model 模型对象，properties属性不能为null。
	 * @return
	 */
	public static Map<String, String> getAsskeyByTagetModelWithoutDatabase(Map<String, Property> propertyMap, Model model) {
		List<AssociatedInfo> assinfos = ModelServiceImpl.createAssociatedInfosFromProperties(propertyMap, model, AssociatedInfo.MANY_TO_ONE, AssociatedInfo.ONE_TO_ONE);
		return createAsskeyWithAssociatedInfos(assinfos);
	}
	
	/**
	 * 根据关联模型的属性来创建n..1和1..1的关联信息
	 * 
	 * @param model 模型对象，properties属性不能为null。
	 * @return
	 */
	public static Map<String, String> getAsskeyByTagetModelWithoutDatabase(Map<String, Property> propertyMap, DataGrid datagrid) {
		List<AssociatedInfo> assinfos = ModelServiceImpl.createAssociatedInfosFromProperties(propertyMap, datagrid, AssociatedInfo.MANY_TO_ONE, AssociatedInfo.ONE_TO_ONE);
		return createAsskeyWithAssociatedInfos(assinfos);
	}

	/**
	 * 根据传入的关联信息创建Map对象, key为 originalProperty.name + '.id'， value 为 targetProperty.name 
	 * 
	 * 只创建 1..1 和 N..1 关联
	 * 
	 * @param assinfos
	 * @return
	 */
	private static Map<String, String> createAsskeyWithAssociatedInfos(List<AssociatedInfo> assinfos) {
		Map<String, String> assKey = new HashMap<String, String>();
		for (AssociatedInfo ass : assinfos) {
			if (ass.getType().equals(1) || ass.getType().equals(2)) {
				assKey.put(ass.getOriginalProperty().getName() + ".id", ass.getTargetProperty().getName());
			}
		}
		return assKey;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<DataGrid> getDataGridByViews(List<View> views) {
		List<DataGrid> dgs = datagridDao.createQuery("from DataGrid where view in (:views) and valid = true")
				.setParameterList("views", views).list();
		if (!dgs.isEmpty()) {
			for (DataGrid dg : dgs) {
				Hibernate.initialize(dg.getOrgProperty());
				if (null != dg.getOrgProperty()) {
					Hibernate.initialize(dg.getOrgProperty().getAssociatedProperty());
					if (null != dg.getOrgProperty().getAssociatedProperty()) {
                        Hibernate.initialize(dg.getOrgProperty().getAssociatedProperty().getModel());
                    }
				}
				for (View view : views) {
					if (view.getCode().equals(dg.getView().getCode())) {
						dg.setView(view);
						break;
					}
				}
			}
		}
		return dgs;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Page<DataGrid> findDataGrids(Page<DataGrid> page, Criterion... criterions) {
		// Criterion validCriterion = Restrictions.eq("valid", true);
		Criterion validCriterion = Restrictions.isNotNull("code");
		if (null == criterions) {
            criterions = new Criterion[] { validCriterion };
        } else {
			Criterion[] cs = new Criterion[criterions.length + 1];
			System.arraycopy(criterions, 0, cs, 0, criterions.length);
			cs[criterions.length] = validCriterion;
			criterions = cs;
		}
		return datagridDao.findByPage(page, criterions);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.supcon.orchid.entityconf.services.DataGridService#findDataGridsByProperty()
	 */
	@SuppressWarnings("unchecked")
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<DataGrid> findDataGridsByProperty(Property property) {
		List<DataGrid> dataGrids = Collections.EMPTY_LIST;
		if (null != property) {
			dataGrids = datagridDao.findByHql("from DataGrid dg where dg.orgProperty = ?0", property);
		}
		return dataGrids;
	}

	@Transactional
	@Override
	public String getDataGridFullConfig(DataGrid dataGrid) {
		return ecConfigService.getEcFullConfig(dataGrid);
	}

	@SuppressWarnings("rawtypes")
	@Transactional
	@Override
	public Map getDataGridFullConfigMap(DataGrid dataGrid) {
		String config = ecConfigService.getEcFullConfig(dataGrid);
		return (Map) SerializeUitls.deserialize(config);
	}

	@Override
	@Transactional
	public void saveBackupDataGrid(BackupDataGrid backupDataGrid) {
		backupDataGridDao.save(backupDataGrid);
	}

	@Override
	@Transactional
	public void deleteBackupDataGridByBackupView(String bvCode) {
		String hql = "update BackupDataGrid bd set bd.valid = false where bd.backupView.code = ?";
		backupDataGridDao.bulkExecute(hql, bvCode);
	}

	@Transactional
	private void deleteBackupDataGridByDataGrid(String viewCode) {
		String hql = "delete from BackupDataGrid bd where bd.view.code = ?0";
		backupDataGridDao.bulkExecute(hql, viewCode);
	}

	@Transactional
	private void deleteBackupDataGridByDataGridPhysical(String viewCode) {
		String hql = "delete from BackupDataGrid bd where bd.view.code = ?0";
		backupDataGridDao.bulkExecute(hql, viewCode);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public BackupDataGrid getBackupDataGrid(String dgCode, String bvCode) {
		String hql = "from BackupDataGrid bd where bd.code like ? and bd.backupView.code = ?";
		BackupDataGrid backupDataGrid = (BackupDataGrid) backupDataGridDao.findEntityByHql(hql, dgCode + "_%", bvCode);
		return backupDataGrid;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<BackupDataGrid> getBackupDataGridByBackupViewCode(String bvCode) {
		String hql = "from BackupDataGrid bd where bd.backupView.code = ?0";
		return backupDataGridDao.findByHql(hql, bvCode);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.supcon.orchid.entityconf.services.DataGridService#getDataGridByTargetModel(java.lang.String)
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<DataGrid> getDataGridByTargetModel(String code) {
		String hql = "from DataGrid bd where bd.targetModel.code = ?";
		return datagridDao.findByHql(hql, code);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public DataGrid loadDataGrid(String code) {
		return datagridDao.load(code);
	}

	/**
	 * 根据条件查询DataGrid
	 * 
	 * @param criterions
	 * @return
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<DataGrid> findDataGrids(Criterion... criterions) {
		return datagridDao.findByCriteria(criterions);
	}
}