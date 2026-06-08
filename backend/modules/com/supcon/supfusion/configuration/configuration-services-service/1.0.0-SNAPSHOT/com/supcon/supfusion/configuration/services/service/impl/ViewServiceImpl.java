/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.

 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.entities.Deployment;
import com.supcon.supfusion.base.entities.MenuInfo;
import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.base.entities.Task;
import com.supcon.supfusion.base.services.*;
import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.configuration.services.dao.*;
import com.supcon.supfusion.configuration.services.entity.Property;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.*;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.service.*;
import com.supcon.supfusion.configuration.services.service.rpc.MsModuleServiceApi;
import com.supcon.supfusion.configuration.services.utils.*;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.scaffold.hibernate.dao.IBaseDao;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import flexjson.JSONSerializer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.criterion.*;
import org.hibernate.query.Query;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 * @author songjiawei
 *
 */
@Data
@Slf4j
@ServiceApiService
public class ViewServiceImpl extends BaseServiceImpl<View> implements ViewService, InitializingBean {

	final Pattern pattern = Pattern.compile("<echartCode>(.*?)</echartCode>");
	public static final int DIRECTION_DEV_TO_RUNTIME = 1;
	public static final int DIRECTION_DEV_TO_PROJ = 2;
	public static final int DIRECTION_PROJ_TO_RUNTIME = 3;
	public static final int DIRECTION_NO_TO_RUNTIME = 4;
	public static final int DIRECTION_DEV_TO_RUNTIME_DEV_TO_PROJ = 5;
	public static final String MIS = "Mis";
	public static final String MS_SERVICE = "msService";
	public static final String PROJ_FLAG = "proj";
	private ThreadLocal<Map<String, Object>> dataLocal = new ThreadLocal<Map<String, Object>>();
	@Autowired
	private EntityDaoImpl entityDao;
	@Autowired
	private ViewDaoImpl viewDao;
	@Autowired
	private ModelDaoImpl modelDao;
	@Autowired
	private PropertyDaoImpl propertyDao;
	@Autowired
	private ButtonService buttonService;
	@Autowired
	private SqlService sqlService;
	@Autowired
	private DataGridService dataGridService;
	@Autowired
	private DataGridDaoImpl dataGridDao;
	@Autowired
	private BackupViewDaoImpl backupViewDao;
	@Autowired
	private DataGroupDaoImpl dataGroupDao;
	@Autowired
	private DataClassificDaoImpl dataClassificDao;
	@Autowired
	private ConditionService conditionService;
	@Autowired
	private FastQueryJsonDaoImpl fastQueryJsonDao;
	@Autowired
	private AdvQueryJsonDaoImpl advQueryJsonDao;
	@Autowired
	ExtraQueryJsonDaoImpl extraQueryJsonDao;
	@Autowired
	ExtraViewDaoImpl extraViewDao;
	@Autowired
	private FastQueryJsonService fastQueryJsonService;
	@Autowired
	private AdvQueryJsonService advQueryJsonService;
	@Autowired
	private EcConfigService ecConfigService;
	@Autowired
	private FieldService fieldService;
	@Autowired
	private ButtonInfoService buttonInfoService;
	@Autowired
	private ButtonDaoImpl buttonDao;
	@Autowired
	private EventService eventService;
	@Autowired
	private CustomerConditionService customerConditionService;
	@Autowired
	private MenuOperateService menuOperateService;
	@Autowired
	private MenuInfoService menuInfoService;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ModelService modelService;

	@Autowired
	private ModuleService moduleService;
	@Autowired
	private EntityService entityService;
	private int totalNum = 100;
	private int beforeDateNum = 7;

	@Autowired
	private PropertyKeyService propertyKeyService;
	@Autowired
	private InternationalService internationalService;

	@Autowired
	private ActionViewService actionViewService;
	@Autowired
	private CustomPropertyModelMappingDaoImpl customPropertyModelMappingDao;
	@Autowired
	private CustomPropertyViewMappingDaoImpl customPropertyViewMappingDao;
	@Autowired
	private StaffService staffService;

//	@Autowired
	private GenerateService generateService;
	@Autowired
	private EchartsService echartsService;

	@Autowired
	private ValidateService validateService;
	/*
	 * @Override
	 *
	 * @Transactional(readOnly = true)
	 * public View getView(long viewId) {
	 * return viewDao.findEntityByHql("from View where id = ? and valid = true", viewId);
	 * }
	 */

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public View getView(String code) {
		View view = viewDao.findEntityByHql("from View where code = ?0 and valid = true", code);
		return view;

	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	private View getViewByNoValid(String code) {
		return viewDao.findEntityByHql("from View where code = ?0", code);

	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public View getView(String viewCode, boolean full) {
		View view = getView(viewCode);
		log.debug("view" + view.getName());
		if (full) {
			if (!Hibernate.isInitialized(view.getExtraView())) {
                Hibernate.initialize(view.getExtraView());
            }
			if (!Hibernate.isInitialized(view.getDataGrids())) {
                Hibernate.initialize(view.getDataGrids());
            }
			if (!Hibernate.isInitialized(view.getFields())) {
                Hibernate.initialize(view.getFields());
            }
			List<Field> fields = view.getFields();
			if (!CollectionUtils.isEmpty(fields)) {
				for (Field field : fields) {
					if (!Hibernate.isInitialized(field.getEvents())) {
						Hibernate.initialize(field.getEvents());
					}
					if (!Hibernate.isInitialized(field.getValidates())) {
						Hibernate.initialize(field.getValidates());
					}
				}
			}

			if (!Hibernate.isInitialized(view.getButtons())) {
                Hibernate.initialize(view.getButtons());
            }
			List<Button> buttons = view.getButtons();
			if (!CollectionUtils.isEmpty(buttons)) {
				for (Button button : buttons) {
					if (!Hibernate.isInitialized(button.getEvents())) {
						Hibernate.initialize(button.getEvents());
					}
				}
			}
			Entity entity = view.getEntity();
			if (!Hibernate.isInitialized(entity)) {
				Hibernate.initialize(entity);
			}
			Model assModel = view.getAssModel();
			if (!Hibernate.isInitialized(assModel)) {
                Hibernate.initialize(assModel);
            }
			if (null!=assModel) {
				Set<Property> properties = assModel.getProperties();
				if (properties == null || properties.size() == 0) {
					List pros = modelService.findProperties(assModel);
					properties = new LinkedHashSet<>(pros.size());
					properties.addAll(pros);
					assModel.setProperties(properties);
				}
			}
//			if (!Hibernate.isInitialized(view.getFastQueryJson())) {
//				Hibernate.initialize(view.getFastQueryJson());
//			}
//			if (!Hibernate.isInitialized(view.getAdvQueryJson())) {
//				Hibernate.initialize(view.getAdvQueryJson());
//			}
		}
		return view;
	}

	/**
	 * 删除视图,并且删除相关配置如自定义条件等
	 *
	 * @param view
	 *            视图实例
	 */
	@SuppressWarnings("rawtypes")
	@Override
	@Transactional
	public String deleteView(View view) {
		if (view != null) {
			Set descSet = new HashSet<String>();
			List<View> shadowViews = viewDao.findByHql("from View v where v.shadowView.code = ?0", view.getCode());
			if (null != shadowViews && !shadowViews.isEmpty()) {
				for (View shadowView : shadowViews) {
					descSet.add(InternationalResource.get(
							"ec.view.delete.shadowViews",new Object[]{
							InternationalResource.get(shadowView.getName())}));

				}
				return JsonUtils.setToJson(descSet);
				// throw new BAPException(BAPException.Code.ASS_BY_SHADOWVIEW);
			}
			if (view.getShowType() == ShowType.PART) {
				// 视图是否在布局中被引用
				String sqlLayout = "SELECT V.NAME FROM ec_extra_view EV INNER JOIN EC_VIEW V ON V.CODE = EV.CODE WHERE V.SHOW_TYPE='LAYOUT2' and V.VALID = 1 AND INSTR(EV.CONFIG,'<vcode><![CDATA["
						+ view.getCode() + "]]></vcode>') > 0";
				if (viewDao.getDBType() == IBaseDao.DBTYPE.MSSQL) {
					sqlLayout = "SELECT V.NAME FROM ec_extra_view EV INNER JOIN EC_VIEW V ON V.CODE = EV.CODE WHERE V.SHOW_TYPE='LAYOUT2' and V.VALID = 1 AND CHARINDEX('<vcode><![CDATA["
							+ view.getCode() + "]]></vcode>',EV.CONFIG) > 0";
				} else if (viewDao.getDBType() == IBaseDao.DBTYPE.ORACLE) {
					sqlLayout = "SELECT V.NAME FROM ec_extra_view EV INNER JOIN ec_view V ON V.CODE = EV.CODE WHERE V.SHOW_TYPE='LAYOUT2' and V.VALID = 1 AND INSTR(EV.CONFIG,'<vcode><![CDATA["
							+ view.getCode() + "]]></vcode>') > 0";
				}
				List list = viewDao.createNativeQuery(sqlLayout).list();
				if (null != list && !list.isEmpty()) {
					StringBuffer layouts = new StringBuffer();
					for (Object o : list) {
						layouts.append((layouts.length() > 0 ? "," : "")).append(o);
					}
					// 删除对象在布局 {0}中使用,请先删除相关布局
					descSet.add("在布局" + layouts.toString() + "中使用,请先删除相关布局！");
					return JsonUtils.setToJson(descSet);
					// throw new BAPException(BAPException.Code.ASS_BY_LAYOUT, layouts.toString());
				}
			}
			List<View> assViews = viewDao.findByHql("from View v where v.assView.code = ?0", view.getCode());
			if (null != assViews && !assViews.isEmpty()) {
				// 删除对象关联了视图，请先删除对应的视图！
				for (View view2 : assViews) {
					descSet.add("被" + view2.getName() + "视图关联！");
				}
				return JsonUtils.setToJson(descSet);
				// throw new BAPException(BAPException.Code.ASS_BY_VIEW);
			}
			List<View> references = viewDao.findByHql("from View v where v.reference.code = ?0", view.getCode());
			if (null != references && !references.isEmpty()) {
				// 删除对象被其他视图所参考用于复制，请先确认该对象不被参考！
				for (View view2 : references) {
					descSet.add("被" + view2.getName() + "视图所参考复制！");
				}
				return JsonUtils.setToJson(descSet);
				// throw new BAPException(BAPException.Code.ASS_BY_REFERENCEVIEW);
			}
			List<Button> buttonsRef = buttonService.getButtonsByViewSelect(view.getCode());
			if (null != buttonsRef && !buttonsRef.isEmpty()) {
				// 删除对象被其他视图中按钮的打开视图引用，请先确认该对象不被引用！
				for (Button button : buttonsRef) {
					descSet.add("被" + button.getView().getName() + "视图中按钮的打开视图引用！");
				}
				return JsonUtils.setToJson(descSet);
				// throw new BAPException(BAPException.Code.ASS_BY_BUTTON);
			}
			List<Task> tasks = viewDao.findByHql("from Task where viewCode = ? and valid = true", view.getCode());
			if (null != tasks && !tasks.isEmpty()) {
				// 删除视图关联了流程活动，请先删除流程活动！
				for (Task task : tasks) {
					descSet.add("被" + task.getName() + "流程活动关联！");
				}
				return JsonUtils.setToJson(descSet);
				// throw new BAPException(BAPException.Code.ASS_BY_TASK);
			}
			buttonService.deleteButtonByViewCode(view.getCode());
			fieldService.deleteFieldByViewCode(view.getCode());
			eventService.deleteEventByView(view.getCode());
			List<DataGroup> dgs = findDataGroups(view);
			if (null != dgs && !dgs.isEmpty()) {
				for (DataGroup dg : dgs) {
					List<DataClassific> dcs = findDataClassifics(dg);
					if (null != dcs && !dcs.isEmpty()) {
						for (DataClassific dc : dcs) {
							customerConditionService.deleteByObject(dc);
							dataClassificDao.delete(dc);
						}
						dataClassificDao.flush();
					}
					dataGroupDao.delete(dg);
					dataGroupDao.flush();
				}
			}
			List<DataGrid> dgList = dataGridService.getDataGridByView(view, false);
			if (null != dgList && !dgList.isEmpty()) {
				for (DataGrid dg : dgList) {
					dataGridService.deleteDataGrid(dg.getCode());
				}
			}
			// 图表
			echartsService.delEchartsByViewCode(view.getCode());
			deleteBackupViewByViewCode(view.getCode());
			customerConditionService.deleteByObject(view);
			viewDao.delete(view.getCode(), view.getVersion());

		}
		return "";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.supcon.orchid.entityconf.services.ViewService#deleteView(java.lang.String
	 */
	public void deleteView(String viewCode) {
		View view = getView(viewCode);
		deleteView(view);

	}

	@SuppressWarnings("rawtypes")
	@Override
	@Transactional
	public String deleteViewPhysical(String viewCode, Boolean deleteType) {
		View view = getViewByNoValid(viewCode);
		Set<String> descSet = new HashSet<String>();
		if (null != view) {
			if (view.getInheritType() == null) {//工程期继承的视图删除时不需要判断依赖
				try {
					descSet = this.checkDeleteView(view);
					if (null != descSet && !descSet.isEmpty()) {
						return JsonUtils.setToJson(descSet);
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					throw new EcException("校验失败，详细错误信息请查看日志");
				}
				// 非工程期视图需要校验影子视图
				List<View> shadowViews = viewDao.findByHql("from View v where v.shadowView.code = ?0", view.getCode());
				if (null != shadowViews && !shadowViews.isEmpty()) {
					for (View shadowView : shadowViews) {
						descSet.add(InternationalResource.get(
								"ec.view.delete.shadowViews", new Object[]{
										InternationalResource.get(shadowView.getName())}));

					}
					return JsonUtils.setToJson(descSet);
				}
			}
			buttonService.deleteButtonByViewCode(view.getCode());
			fieldService.deleteFieldByViewCode(view.getCode());
			eventService.deleteEventByView(view.getCode());
			List<DataGroup> dgs = findDataGroupsNoValid(view);
			if (null != dgs && !dgs.isEmpty()) {
				for (DataGroup dg : dgs) {
					List<DataClassific> dcs = findDataClassificsNoValid(dg);
					if (null != dcs && !dcs.isEmpty()) {
						for (DataClassific dc : dcs) {
							customerConditionService.deletePhysicalByObject(dc);
							dataClassificDao.deletePhysical(dc);
						}
						dataClassificDao.flush();
					}
					dataGroupDao.deletePhysical(dg);
					dataGroupDao.flush();
				}
			}
			List<DataGrid> dgList = dataGridService.getDataGridByView(view, true);
			if (null != dgList && !dgList.isEmpty()) {
				for (DataGrid dg : dgList) {
					dataGridService.deleteDataGridPhysical(dg);
				}
			}
			// 图表
			echartsService.delEchartsByViewCode(view.getCode());
			List<Sql> sqls = sqlService.getSqls(viewCode);
			if (null != sqls && !sqls.isEmpty()) {
				for (Sql sql : sqls) {
					sqlService.deleteSql(sql);
				}
			}
			if (null != view.getFastQueryJson() && view.getFastQueryJson().size() > 0) {
				for (FastQueryJson fqjs : view.getFastQueryJson()) {
					fastQueryJsonService.deletePhysical(fqjs);
				}
			}
			if (null != view.getAdvQueryJson() && view.getAdvQueryJson().size() > 0) {
				for (AdvQueryJson aqj : view.getAdvQueryJson()) {
					advQueryJsonService.deletePhysical(aqj);
				}
			}
			if (null != view.getExtraQueryJson()) {
				extraQueryJsonDao.delete(view.getExtraQueryJson());
				extraQueryJsonDao.flush();
			}
			if (null != view.getExtraView()) {
				extraViewDao.delete(view.getExtraView());
				extraViewDao.flush();
			}
			deleteBackupViewByViewCodePhysical(view.getCode());
			customerConditionService.deletePhysicalByObject(view);
			if (view.getType() == ViewType.REFERENCE || view.getType() == ViewType.REFTREE) {
				// 删除菜单
				String hql = "from View where valid=true and code != ?0 and isPermission=true and permissionCode=?1 and entity.code=?2";
				List<View> viewList = viewDao.findByHql(hql, view.getCode(), view.getPermissionCode(), view.getEntity().getCode());
				if (null == viewList || viewList.isEmpty()) {
					menuOperateService.deleteMenuOperateByPhysical(view.getEntity().getCode() + "_" + view.getPermissionCode());
				}
			}
			actionViewService.deleteActionViewByViewcode(view.getCode());
			viewDao.deletePhysical(view);
			log.info("删除" + view.getName() + "视图文件开始");
			modelService.deleteModuleFile(view);
			log.info("删除" + view.getName() + "视图文件结束");
		}
		return "";
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	@Transactional
	public void saveView(View view) {
		view.setModifyTime(new Date());
		if (!checkNameUnique(view)) {
			throw new EcException(EcException.Code.UNIQUECODE);
		}
		if (StringUtils.isEmpty(view.getCode())) {
			if (!checkViewNamekey(view)) {
				throw new EcException(EcException.Code.VIEW_NAME_KEY);
			}
			view.setCode(view.getEntity().getCode() + "_" + view.getName());
		} else {
			// 修改关联模型时如果视图被选择为批量控件打印视图则不能操作
			if (null != view.getAssModel()) {
				Model assModel = modelService.getModel(view.getAssModel().getCode());
				if (null!=assModel && null != assModel.getModelName() && !"".equals(assModel.getModelName()) && !assModel.getModelName().equals(view.getAssModel().getModelName())
						&& !checkViewIsBatchControlPrint(view)) {
					throw new EcException(EcException.Code.USED_BY_BATCHCCONTROLPRINT);
				}
			}
		}
		if (!view.getIsReference()) {
			view.setReference(null);
		}
		if (view.getType() == ViewType.MNECODE) {
			List<View> mneCodeViewList = findViews(view.getEntity(), ViewType.MNECODE);
			if (mneCodeViewList.size() > 0 && !view.getCode().equals(mneCodeViewList.get(0).getCode())) {
				// 只能创建一个助记码视图
				throw new EcException(EcException.Code.MNECODEVIEW_ONLY);
			}
			view.setPublishTime(new Date());
			viewDao.flush();
			viewDao.clear();
		}

		if (null == view.getEntity().getModule()) {
			Entity entity = entityService.getEntity(view.getEntity().getCode());
			view.setEntity(entity);
		}

		if ((null == view.getModuleCode() || "".equals(view.getModuleCode()))&&view.getEntity() !=null) {
			view.setModuleCode(view.getEntity().getModule().getCode());
		}
		// view = getView(view.getId());
		if (null != view.getAssModel() && null == view.getAssModel().getModelName()) {
			Model assModel = modelService.getModel(view.getAssModel().getCode());
			view.setAssModel(assModel);
		}
		if (!view.getCustomFlag() && view.getEntity() != null && view.getEntity().getModule() != null && view.getAssModel() != null && view.getName() != null
				&& view.getEntity().getEntityName() != null && view.getEntity().getModule().getArtifact() != null && view.getAssModel().getModelName() != null) {
			if(null!= ProjectFlagHolder.getInstance().getProjFlag().get() && ProjectFlagHolder.getInstance().getProjFlag().get()){
				view.setUrl(String.format("/%s/%s/%s/%s/%s/%s", MS_SERVICE, view.getEntity().getModule().getArtifact(), view.getEntity().getEntityName(),
						View.fl(view.getAssModel().getModelName()),PROJ_FLAG, view.getName()));
			}else{
				view.setUrl(String.format("/%s/%s/%s/%s/%s", MS_SERVICE, view.getEntity().getModule().getArtifact(), view.getEntity().getEntityName(),
						View.fl(view.getAssModel().getModelName()), view.getName()));
			}
		}

		if (null != view.getOpenType() && "frame".equals(view.getOpenType()) && (ViewType.EDIT.equals(view.getType()) || ViewType.VIEW.equals(view.getType()))) {
			view.setWidth(null);
			view.setHeight(null);
		}
		if (view.getDisplayName().equals(view.getTitle())||null==view.getDisplayName()||view.getDisplayName().isEmpty()) {// 如key一样值也一样
			String displayName = internationalService.createNewInternational(view.getTitle());
			view.setDisplayName(displayName);
		} else {
			// messageKey一样 值不同
			String title = resolveMessageKey(view.getTitle());
			String displayName = resolveMessageKey(view.getDisplayName());
			if (title.equals(displayName)) {
				displayName = internationalService.createNewInternational(view.getDisplayName());
				view.setDisplayName(displayName);
			}

		}
		if (null != view.getRefOperateName()) {
			if (view.getRefOperateName().equals(view.getTitle())) {// 如key一样值也一样
				String refOperateName = internationalService.createNewInternational(view.getTitle());
				view.setRefOperateName(refOperateName);
			} else {
				// messageKey一样 值不同
				String title = resolveMessageKey(view.getTitle());
				String refOperateName = resolveMessageKey(view.getRefOperateName());
				if (title.equals(refOperateName)) {
					refOperateName = internationalService.createNewInternational(view.getRefOperateName());
					view.setRefOperateName(refOperateName);
				}

			}
		}
		if (null != view.getMenuName()) {
			if (view.getMenuName().equals(view.getTitle())) {// 如key一样值也一样
				String menuName = internationalService.createNewInternational(view.getTitle());
				view.setMenuName(menuName);
			} else {
				// messageKey一样 值不同
				String title = resolveMessageKey(view.getTitle());
				String menuName = resolveMessageKey(view.getMenuName());
				if (title.equals(menuName)) {
					menuName = internationalService.createNewInternational(view.getMenuName());
					view.setMenuName(menuName);
				}
			}
		}
		if (Boolean.TRUE.equals(ProjectFlagHolder.getInstance().getProjFlag().get()) && view.getProjEnabled() == null) {
			view.setProjEnabled(true);
		}
		if(view.getParentMenuId()!=null){
			MenuInfo mi=menuInfoService.load(view.getParentMenuId());
			if(mi!=null){
				view.setParentMenuCode(mi.getCode());
			}
		}
		viewDao.save(view);
		viewDao.flush();
		saveActionViewMapping(view);
		if (view.getIsShadow() != null && view.getIsShadow() && view.getShadowView() != null) { // 根据父视图设置影子视图的hasCustomSection属性
			view.setHasCustomSection(view.getShadowView().getHasCustomSection());
		}

		// 确保只有一个主查看视图
		if (view.getType() == ViewType.VIEW) {
			List<View> viewList = findViews(view.getEntity(), ViewType.VIEW);
			if (view.getMainView() != null && view.getMainView()) {
				if (viewList != null && viewList.size() > 1) {
					for (View item : viewList) {
						if ((item.getMainView() != null && !item.getMainView()) || item.getCode().equals(view.getCode())) {
							continue;
						} else {
							item.setMainView(false);
							viewDao.save(item);
						}
					}
				}
			} else {
				boolean flag = false;
				for (View item : viewList) {
					if (item.getMainView() != null && item.getMainView()) {
						flag = true;
						break;
					}
				}
				if (!flag) {
					viewList.get(0).setMainView(true);
					viewDao.save(viewList.get(0));
				}
			}
		}
		// 只有一个主列表视图
		if (view.getType() == ViewType.LIST) {
			if (ShowType.PART.equals(view.getShowType()) && view.getUsedForWorkFlow() != null && view.getUsedForWorkFlow()) {
				throw new EcException("列表片段不能设置为主列表视图");
			}
			List<View> viewList = findViews(view.getEntity(), ViewType.LIST);
			// Iterator<View> iterator = viewList.iterator();
			// while (iterator.hasNext()) {
			// View v = iterator.next();
			// if (v.getShowType().equals(ShowType.PART)) {
			// // viewList.remove(v);
			// iterator.remove();
			// }
			// }
			if (view.getUsedForWorkFlow() != null && view.getUsedForWorkFlow()) {
				if (viewList != null && !viewList.isEmpty()) {
					for (View item : viewList) {
						if ((item.getUsedForWorkFlow() != null && !item.getUsedForWorkFlow()) || item.getCode().equals(view.getCode())) {
							continue;
						} else {
							item.setUsedForWorkFlow(false);
							viewDao.save(item);
						}
					}
				}
			} else {
				if (null != viewList && !viewList.isEmpty()) {
					boolean flag = false;
					for (View item : viewList) {
						if (item.getUsedForWorkFlow() != null && item.getUsedForWorkFlow()) {
							flag = true;
							break;
						}
					}
					if (!flag) {
						for (View item : viewList) {
							if (ShowType.PART.equals(item.getShowType())) {
								continue;
							}
							item.setUsedForWorkFlow(true);
							viewDao.save(item);
							break;
						}
					}
				}
			}
		}
		// 只有一个主参照视图
		if (view.getType() == ViewType.REFERENCE) {
			if (view.getShowType().equals(ShowType.PART) && view.getMainRef() != null && view.getMainRef()) {
				throw new EcException("参照片段不能设置为主参照视图");
			}
			List<View> viewList = findViews(view.getEntity(), ViewType.REFERENCE);
			// Iterator<View> iterator = viewList.iterator();
			// while (iterator.hasNext()) {
			// View v = iterator.next();
			// if (v.getShowType().equals(ShowType.PART)) {
			// // viewList.remove(v);
			// iterator.remove();
			// }
			// }
			if (view.getMainRef() != null && view.getMainRef()) {
				if (viewList != null && !viewList.isEmpty()) {
					for (View item : viewList) {
						if ((item.getMainRef() != null && !item.getMainRef()) || item.getCode().equals(view.getCode())) {
							continue;
						} else {
							item.setMainRef(false);
							viewDao.save(item);
						}
					}
				}
			} else {
				if (null != viewList && !viewList.isEmpty()) {
					boolean flag = false;
					for (View item : viewList) {
						if (item.getMainRef() != null && item.getMainRef()) {
							flag = true;
							break;
						}
					}
					if (!flag) {
						for (View item : viewList) {
							if (item.getShowType().equals(ShowType.PART)) {
								continue;
							}
							item.setMainRef(true);
							viewDao.save(item);
							break;
						}
					}
				}
			}
		}
		if (view.getType() == ViewType.LIST || view.getType() == ViewType.REFERENCE) {
			ExtraView ev = view.getExtraView();
			if (null != ev) {
				if (view.getHasAttachment() != null && !view.getHasAttachment()) {
					Field attachmentField = fieldService.getField(view.getCode() + "_LISTPT_bapAttachmentInfo");
					if (null != attachmentField) {
						String cellCode = attachmentField.getCellCode();
						if (null != cellCode && cellCode.length() > 0) {
							ev = view.getExtraView();
							if (null != ev && ev.getConfig().length() > 0) {
								Map configMap = (Map) SerializeUitls.deserialize(ev.getConfig());
								if (configMap != null && !configMap.isEmpty()) {
									Map layout = (Map) configMap.get("layout");
									if (layout != null && !layout.isEmpty()) {
										List<Map<String, Object>> sections = (List<Map<String, Object>>) layout.get("sections");
										if (null != sections && !sections.isEmpty()) {
											for (Map<String, Object> section : sections) {
												if (null != section.get("regionType") && "LISTPT".equals(section.get("regionType").toString())) {
													List<Map<String, Object>> cells = (List<Map<String, Object>>) section.get("cells");
													for (Map<String, Object> cell : cells) {
														if (cell.get("cellCode").equals(cellCode)) {
															cells.remove(cell);
															break;
														}
													}
												}
											}
										}

									}
								}
								ev.setConfig(SerializeUitls.serializeAsXml(configMap));
							}
						}
						fieldService.deleteField(attachmentField);
					}
				} else {
					Field attachmentField = fieldService.getField(view.getCode() + "_LISTPT_bapAttachmentInfo");
					if (null == attachmentField) {
						String cellCode = "cell_" + System.currentTimeMillis() + "_" + Math.round(Math.random() * 10000);
						attachmentField = new Field();
						attachmentField.setCellCode(cellCode);
						attachmentField.setCode(view.getCode() + "_LISTPT_bapAttachmentInfo");
						attachmentField.setDisplayName("foundation.upload.attachment");
						attachmentField.setShowType(FieldType.ATTACHMENT);
						attachmentField.setShowFormat(ShowFormat.ATTACHMENT);
						attachmentField.setIsHidden(false);
						attachmentField.setKey("bapAttachmentInfo");
						attachmentField.setLayRec("");
						attachmentField.setRegionType(RegionType.LISTPT);
						attachmentField.setValid(true);
						attachmentField.setView(view);
						attachmentField.setModuleCode(view.getModuleCode());
						attachmentField.setEntityCode(view.getEntity().getCode());
						attachmentField
								.setConfig("<?xml version=\"1.0\" encoding=\"UTF-8\"?><config><field><isHidden><![CDATA[false]]></isHidden><textalign><![CDATA[center]]></textalign><multable><![CDATA[false]]></multable><cellCode><![CDATA["
										+ cellCode
										+ "]]></cellCode><isOrderBy><![CDATA[false]]></isOrderBy><width><![CDATA[250]]></width><showType><![CDATA[ATTACHMENT]]></showType><namekey><![CDATA[foundation.upload.attachment]]></namekey><key><![CDATA[bapAttachmentInfo]]></key><regionType><![CDATA[LISTPT]]></regionType></field></config>");
						fieldService.saveField(attachmentField);
						if (null != ev && ev.getConfig().length() >= 0) {
							Map configMap = (Map) SerializeUitls.deserialize(ev.getConfig());
							if (configMap != null && !configMap.isEmpty()) {
								Map layout = (Map) configMap.get("layout");
								if (layout != null && !layout.isEmpty()) {
									List<Map<String, Object>> sections = (List<Map<String, Object>>) layout.get("sections");
									if (null != sections && !sections.isEmpty()) {
										for (Map<String, Object> section : sections) {
											if (null != section.get("regionType") && "LISTPT".equals(section.get("regionType").toString())) {
												List<Map<String, Object>> cells = (List<Map<String, Object>>) section.get("cells");
												Map<String, Object> cell = new HashMap<String, Object>();
												cell.put("cellCode", cellCode);
												cell.put("regionType", "LISTPT");
												cells.add(cell);
												break;
											}
										}
									}

								}
							}
							ev.setConfig(SerializeUitls.serializeAsXml(configMap));
						}

					}
				}
				if(ev != null){
					String fullConfig = ev.getFullConfig();
					if (null == fullConfig || fullConfig.trim().length() == 0) {
						fullConfig = ecConfigService.getEcFullConfig(view);
						ev.setFullConfig(fullConfig);
					}
					ev.setConfig(fullConfig);
					ev.setConfigMap(null);
					saveExtraView(ev, null);
				}
			}
		}
		// 树参照视图 与 参照视图 生成权限 menuoperator
		if (view.getType() == ViewType.REFTREE || view.getType() == ViewType.REFERENCE) {
			if (null != view.getIsPermission() && view.getIsPermission()) {
				View mainListView = this.getMainListView(view.getEntity());
				if (null != mainListView) {
					List<View> refViews = new ArrayList<View>();
					refViews.add(view);
					moduleService.publishRefMenu(mainListView, refViews);
				}
			} else { // 删除原有操作
				String permissionCode = (null == view.getPermissionCode()) ? view.getName() : view.getPermissionCode();
				String hql = "from View where valid=true and code != ? and isPermission=true and permissionCode=? and assModel.code=?";
				List<View> viewList = viewDao.findByHql(hql, view.getCode(), permissionCode, view.getAssModel().getCode());
				if (null == viewList || viewList.isEmpty()) {
					menuOperateService.deleteMenuOperateByPhysical(view.getAssModel().getCode() + "_" + permissionCode);
				}
			}
		}
		if (view.getType() == ViewType.MNECODE) {
			ExtraView ev = view.getExtraView();
			if (ev != null) {
				String fullConfig = ev.getFullConfig();
				if (null == fullConfig || fullConfig.trim().length() == 0) {
					fullConfig = ecConfigService.getEcFullConfig(view);
					ev.setFullConfig(fullConfig);
				}
				ev.setConfig(fullConfig);
				ev.setConfigMap(null);
				saveExtraView(ev, null);
				Sql sql = getSql(view.getCode(), Sql.TYPE_USED_MNECODE);
				if (null == sql) {
                    sql = new Sql();
                    sql.setVersion(0);
                }
				sql.setViewCode(view.getCode());
				sql.setType(Sql.TYPE_USED_MNECODE);
				sql.setCode(view.getCode() + "_" + Sql.TYPE_USED_MNECODE);
				sql.setSql(buildMneCodeSql(view));
				saveSqls(sql);
				viewDao.createNativeQuery("update runtime_extra_view set CONFIG=?,FULL_CONFIG=? where CODE=?", fullConfig, fullConfig, ev.getCode())
						.executeUpdate();
				viewDao.createNativeQuery("update runtime_sql set QUERY_SQL=? where CODE=?", sql.getSql(), sql.getCode()).executeUpdate();
			}
		}

	}

	private String replaceFieldCode(String config, View srcView, String targetViewCode, ViewType targetViewType) {
		String retStr = config;
		if (config == null) {
			return null;
		}
		String oldStr = null;
		String newStr = null;
		if (ViewType.EDIT.equals(srcView.getType()) || ViewType.VIEW.equals(srcView.getType())) {
			if (!srcView.getType().equals(targetViewType)) {
				if (ViewType.EDIT.equals(srcView.getType())) {
					oldStr = srcView.getCode() + "_EDIT_";
					newStr = targetViewCode + "_VIEW_";
				} else {
					oldStr = srcView.getCode() + "_VIEW_";
					newStr = targetViewCode + "_EDIT_";
				}
			}
		}
		if (oldStr != null) {
			String regex = "<code><\\!\\[CDATA\\[" + oldStr;
			Pattern pattern = Pattern.compile(regex);
			Matcher match = pattern.matcher(config);
			while (match.find()) {
				String cell = match.group();
				retStr = retStr.replace(cell, "<code><![CDATA[" + newStr);
			}
		}
		return retStr;
	}

	private String regenerateCellCode(String config) {
		String retStr = config;
		AtomicInteger seq = new AtomicInteger(0);
		if (config == null) {
			return null;
		}
		String regex = "<cellCode><\\!\\[CDATA\\[cell_\\d{1,}_\\d{1,5}\\]\\]></cellCode>";
		Pattern pattern = Pattern.compile(regex);
		Matcher match = pattern.matcher(config);
		while (match.find()) {
			String cell = match.group();
			retStr = retStr.replace(cell, "<cellCode><![CDATA[cell_" + System.currentTimeMillis() + "_" + seq.addAndGet(1) + "]]></cellCode>");
		}
		return retStr;
	}

	private String regenerateLayoutCode(String config) {
		String retStr = config;
		AtomicInteger seq = new AtomicInteger(0);
		if (config == null) {
			return null;
		}
		// <layoutCode><![CDATA[layout_1365664597296_6444]]></layoutCode>
		String regex = "<layoutCode><\\!\\[CDATA\\[layout_\\d{1,}_\\d{1,}\\]\\]></layoutCode>";
		Pattern pattern = Pattern.compile(regex);
		Matcher match = pattern.matcher(config);
		while (match.find()) {
			String cell = match.group();
			retStr = retStr.replace(cell, "<layoutCode><![CDATA[layout_" + System.currentTimeMillis() + "_" + seq.addAndGet(1) + "]]></layoutCode>");
		}
		return retStr;
	}

	private String regenerateTabCode(String config) {
		String retStr = config;
		AtomicInteger seq = new AtomicInteger(0);
		if (config == null) {
			return null;
		}
		// <tabCode><![CDATA[tab_0]]></tabCode>
		String regex = "<tabCode><\\!\\[CDATA\\[tab_\\d{1,}\\]\\]></tabCode>";
		Pattern pattern = Pattern.compile(regex);
		Matcher match = pattern.matcher(config);
		while (match.find()) {
			String cell = match.group();
			retStr = retStr.replace(cell, "<tabCode><![CDATA[tab_" + seq.getAndAdd(1) + "]]></tabCode>");
		}
		return retStr;
	}

	private String regenerateSectionCode(String config) {
		String retStr = config;
		AtomicInteger seq = new AtomicInteger(0);
		if (config == null) {
			return null;
		}
		// <sectionCode><![CDATA[section_1365664597322_8975]]></sectionCode>
		String regex = "<sectionCode><\\!\\[CDATA\\[section_\\d{1,}_\\d{1,}\\]\\]></sectionCode>";
		Pattern pattern = Pattern.compile(regex);
		Matcher match = pattern.matcher(config);
		while (match.find()) {
			String cell = match.group();
			retStr = retStr.replace(cell, "<sectionCode><![CDATA[section_" + System.currentTimeMillis() + "_" + seq.addAndGet(1) + "]]></sectionCode>");
		}
		return retStr;
	}

	/**
	 * 视图复制处理自定义字段或关联模型字段
	 * @param config
	 * @param content
	 * @return
	 */
	private String dealViewConfig(String config, String content) {
		String retStr = config;
		if (config != null && config.length() > 0) {
			String regex = "<code><\\!\\[CDATA\\[" + content + "[0-9a-f]{8}_[0-9a-f]{4}_[0-9a-f]{4}_[0-9a-f]{4}_[0-9a-f]{12}\\]\\]></code>";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(config);
			while (matcher.find()) {
				String val = matcher.group();
				retStr = retStr.replace(val, "");
			}
		}
		return retStr;
	}

	private DataGrid copyDatagrid(DataGrid srcDg, View view, Map<String, String> viewCodeReplaceMap, Map<String, String> dgCodeReplaceMap) {
		dataGridDao.flush();
		dataGridDao.clear();
		DataGrid dg = new DataGrid();
		BeanUtils.copyProperties(srcDg, dg);
		String newDgName = "dg" + System.currentTimeMillis();
		String oldDgCode = srcDg.getView().getCode() + srcDg.getName();
		String newDgCode = view.getCode() + newDgName;
		String oldFieldCode = srcDg.getView().getCode() + "_" + srcDg.getView().getType() + "_" + oldDgCode;
		String newFieldCode = view.getCode() + "_" + view.getType() + "_" + newDgCode;
		// basicData_1.0_vendor_vendorView_EDIT_basicData_1_0_vendor_vendorViewdg1378706226657

		dg.setCode(view.getCode() + newDgName);
		dg.setView(view);
		dg.setValid(true);
		dg.setName(newDgName);
		dg.setVersion(0);
		String viewConfig = view.getExtraView().getConfig();
		// 替换dgcode
		while (viewConfig.indexOf("<![CDATA[" + oldDgCode + "]]>") != -1) {
			viewConfig = viewConfig.replace("<![CDATA[" + oldDgCode + "]]>", "<![CDATA[" + newDgCode + "]]>");
		}
		// 替换fieldcode
		while (viewConfig.indexOf("<![CDATA[" + oldFieldCode + "]]>") != -1) {
			viewConfig = viewConfig.replace("<![CDATA[" + oldFieldCode + "]]>", "<![CDATA[" + newFieldCode + "]]>");
		}
		// CRM_1.0_customerLink_editdg1365661452461

		String config = ecConfigService.getEcFullConfig(srcDg);
		log.warn("替换前配置：{0}", config);
		String targetConfig = regenerateTabCode(config);
		targetConfig = regenerateLayoutCode(targetConfig);
		targetConfig = regenerateSectionCode(targetConfig);
		targetConfig = regenerateCellCode(targetConfig);
		targetConfig = dealViewConfig(targetConfig, srcDg.getView().getCode() + srcDg.getName() + "_DATAGRID_CUSTOM_");
		// 替换datagrid中fieldcode
		// CRM_1.0_customerLink_editdg1365661452461_DATAGRID
		// while (targetConfig.indexOf("<![CDATA[" + oldDgCode + "_DATAGRID") != -1) {
		// targetConfig = targetConfig.replace("<![CDATA[" + oldDgCode + "_DATAGRID", "<![CDATA[" + newDgCode +
		// "_DATAGRID");
		// }
		if (null != viewCodeReplaceMap && !viewCodeReplaceMap.isEmpty()) {
			for (Entry<String, String> entry : viewCodeReplaceMap.entrySet()) {
				targetConfig = targetConfig.replace("[" + entry.getKey() + "]", "[" + entry.getValue() + "]");
			}
		}
		view.getExtraView().setConfig(viewConfig);
		log.warn("替换后配置：{0}", targetConfig);
		dg.setConfig(targetConfig);
		dataGridDao.save(dg);

		return dg;
	}

	/**
	 * 复制视图
	 */
	@Override
	@Transactional
	public void copyView(View srcView, View view, boolean needCopyExtraView) {
		if (!checkNameUnique(view)) {
			throw new EcException(EcException.Code.UNIQUECODE);
		}

		if (view.getType() == ViewType.MNECODE) {
			List<View> mneCodeViewList = findViews(view.getEntity(), ViewType.MNECODE);
			if (mneCodeViewList.size() > 0) {
				throw new EcException(EcException.Code.MNECODEVIEW_ONLY);
			}
			viewDao.flush();
			viewDao.clear();
		}

		if (null == view.getEntity().getModule()) {
			Entity entity = entityService.getEntity(view.getEntity().getCode());
			view.setEntity(entity);
		}

		View targetView = new View();
		if(getView(srcView.getCode())==null||getView(srcView.getCode()).getExtraView()==null){
			throw new EcException(InternationalResource.get("ec.view.copy.srcviewisnull"));
		}
		srcView = getView(srcView.getCode(), true);
		viewDao.evict(srcView);
		String targetConfig = null;
		String config = null;
		if (needCopyExtraView && srcView.getExtraView() != null) {
			config = this.getExtraViewFullConfig(srcView);
			log.debug("替换前配置：{}", config);
			targetConfig = replaceFieldCode(config, srcView, view.getEntity().getCode() + "_" + view.getName(), view.getType());
			targetConfig = regenerateTabCode(targetConfig);
			targetConfig = regenerateLayoutCode(targetConfig);
			targetConfig = regenerateSectionCode(targetConfig);
			targetConfig = regenerateCellCode(targetConfig);
			targetConfig = regenerateEchartsCode(targetConfig, srcView.getCode(), view.getEntity().getCode() + "_" + view.getName());
			if (ViewType.LIST.equals(srcView.getType()) || ViewType.REFERENCE.equals(srcView.getType())) {
				targetConfig = dealViewConfig(targetConfig, srcView.getCode() + "_LISTPT_ASSO_");
				targetConfig = dealViewConfig(targetConfig, srcView.getCode() + "_LISTPT_CUSTOM_");
			}
			log.debug("替换后配置：{}", targetConfig);
		}
		//解决 Found shared references to a collection异常
		BeanUtils.copyProperties(srcView, targetView, new String[] { "entity", "moduleCode","fastQueryJson","advQueryJson","buttons" });
		targetView.setEntity(view.getEntity() == null ? srcView.getEntity() : view.getEntity());
		targetView.setUsedForWorkFlow(false);
		targetView.setCode(null);
		targetView.setVersion(0);
		targetView.setName(view.getName());
		targetView.setTitle(view.getTitle());
		targetView.setDisplayName(view.getDisplayName());
		targetView.setMenuName(null);
		targetView.setParentMenuId(null);
		targetView.setParentMenuCode(null);
		if (targetView.getDisplayName().equals(targetView.getTitle())) {
			String displayName = internationalService.createNewInternational(targetView.getTitle());
			targetView.setDisplayName(displayName);
		} else {
			// messageKey一样 值不同
			String title = resolveMessageKey(view.getTitle());
			String displayName = resolveMessageKey(view.getDisplayName());
			if (title.equals(displayName)) {
				displayName = internationalService.createNewInternational(view.getDisplayName());
				view.setDisplayName(displayName);
			}

		}
		targetView.setType(view.getType());
		// 复制视图时 参照权限、批量控件打印、控件打印等操作为空
		// 批量打印
		targetView.setIsBatchControlPrint(false);
		targetView.setBatchControlPrintSelectView(null);
		// 参照权限
		targetView.setIsPermission(false);
		targetView.setPermissionCode(null);
		targetView.setOperateUrl(null);
		targetView.setRefOperateName(null);
		// 控件打印
		targetView.setControlPrint(false);
		targetView.setControlName(null);
		targetView.setControlCode(null);
		targetView.setControlSetingName(null);

		ExtraView ev = null;
		if (srcView.getExtraView() != null) {
			ev = new ExtraView();
			ev.setView(targetView);
			targetView.setExtraView(ev);
			ev.setCode(targetView.getCode());
			ev.setConfig(targetConfig);
		}

		view = targetView;
		if (view.getCode() == null || view.getCode().length() == 0) {
			view.setCode(view.getEntity().getCode() + "_" + view.getName());
			ev.setCode(view.getCode());
		}

		if (null == view.getModuleCode() || "".equals(view.getModuleCode())) {
			view.setModuleCode(view.getEntity().getModule().getCode());
		}
		// view = getView(view.getId());
		if (null != view.getAssModel() && null == view.getAssModel().getModelName()) {
			Model assModel = modelService.getModel(view.getAssModel().getCode());
			view.setAssModel(assModel);
		}
		if (!view.getCustomFlag() && view.getEntity() != null && view.getEntity().getModule() != null && view.getAssModel() != null && view.getName() != null
				&& view.getEntity().getEntityName() != null && view.getEntity().getModule().getArtifact() != null && view.getAssModel().getModelName() != null) {
			if(null!= ProjectFlagHolder.getInstance().getProjFlag().get() && ProjectFlagHolder.getInstance().getProjFlag().get()){
				view.setUrl(String.format("/%s/%s/%s/%s/%s/%s", MS_SERVICE, view.getEntity().getModule().getArtifact(), view.getEntity().getEntityName(),
						View.fl(view.getAssModel().getModelName()),PROJ_FLAG, view.getName()));
			}else{
				view.setUrl(String.format("/%s/%s/%s/%s/%s", MS_SERVICE, view.getEntity().getModule().getArtifact(), view.getEntity().getEntityName(),
						View.fl(view.getAssModel().getModelName()), view.getName()));
			}
		}
		view.setInheritType(null);
		view.setPublishTime(null);
		if (srcView.getMainRef()) {
			view.setMainRef(false);
		}
		if(view.getType() == ViewType.VIEW){	//解决视图复制的时候，查看视图复制了参考复制属性
			view.setReference(null);
			view.setIsReference(false);
		}
		viewDao.save(view);
		viewDao.flush();
		saveActionViewMapping(view);
		if (needCopyExtraView && srcView.getExtraView() != null) {
			view.setExtraView(ev);
		}

		List<DataGrid> dgs = null;
		List<DataGrid> targetDgs = new ArrayList<>();
		if (needCopyExtraView && view.getExtraView() != null) {
			if (view.getType() == ViewType.EDIT || view.getType() == ViewType.VIEW || view.getType() == ViewType.EXTRA) {
				dgs = dataGridService.getDataGridByView(srcView, false);
				if (dgs != null && !dgs.isEmpty()) {
					for (DataGrid dg : dgs) {
						targetDgs.add(copyDatagrid(dg, targetView, null, null));
					}
				}
				// 复制图表信息
//				copyEchartsByViewCode(srcView.getCode(), targetView.getCode());
			}
		}

		// 确保只有一个主查看视图
		if (needCopyExtraView && view.getType() == ViewType.VIEW) {
			List<View> viewList = findViews(view.getEntity(), ViewType.VIEW);
			if (view.getMainView() != null && view.getMainView()) {
				if (viewList != null && viewList.size() > 1) {
					for (View item : viewList) {
						if ((item.getMainView() != null && !item.getMainView()) || item.getCode().equals(view.getCode())) {
							continue;
						} else {
							item.setMainView(false);
							viewDao.save(item);
						}
					}
				}
			} else {
				boolean flag = false;
				for (View item : viewList) {
					if (item.getMainView() != null && item.getMainView()) {
						flag = true;
						break;
					}
				}
				if (!flag) {
					viewList.get(0).setMainView(true);
					viewDao.save(viewList.get(0));
				}
			}
		}
		// 只有一个主列表视图
		if (needCopyExtraView && view.getType() == ViewType.LIST) {
			List<View> viewList = findViews(view.getEntity(), ViewType.LIST);
			if (view.getUsedForWorkFlow() != null && view.getUsedForWorkFlow()) {
				if (viewList != null && viewList.size() > 1) {
					for (View item : viewList) {
						if ((item.getUsedForWorkFlow() != null && !item.getUsedForWorkFlow()) || item.getCode().equals(view.getCode())) {
							continue;
						} else {
							item.setUsedForWorkFlow(false);
							viewDao.save(item);
						}
					}
				}
			} else {
				boolean flag = false;
				for (View item : viewList) {
					if (item.getUsedForWorkFlow() != null && item.getUsedForWorkFlow()) {
						flag = true;
						break;
					}
				}
				if (!flag) {
					viewList.get(0).setUsedForWorkFlow(true);
					viewDao.save(viewList.get(0));
				}
			}
		} else {
			view.setUsedForWorkFlow(false);
		}
		// viewDao.flush();
		// viewDao.clear();
		// viewDao.saveExtraView(ev);
		if (needCopyExtraView && srcView.getExtraView() != null) {
			if (view.getShowType() != ShowType.LAYOUT&&view.getShowType() != ShowType.LAYOUT2) {
				if (ev != null && ev.getConfig() != null && ev.getConfig().length() > 0) {
					String viewConfig = ev.getConfig();
					if (viewConfig != null && viewConfig.length() > 0) {
						Map<String, Object> configMap = new EcExtraViewIntegrationUtils().ecSplitConfig(viewConfig);
						if (configMap.get("config") != null) {
							config = configMap.get("config").toString();
							ev.setConfig(config);
							saveExtraView(ev, null);
						}
						if (configMap.get("fieldConfig") != null) {
							String fieldConfig = configMap.get("fieldConfig").toString();
							if (view.getType() != ViewType.MNECODE) {
								buttonService.saveButton(view, fieldConfig, null);
							}
							fieldService.saveFields(view, fieldConfig, null, null, null);
							if (view.getType() != ViewType.MNECODE) {
								eventService.saveEvent(view, fieldConfig);
							}
						}
					}
					if (targetDgs != null && !targetDgs.isEmpty()) {
						viewDao.flush();
						viewDao.clear();
						for (int i = 0; i < targetDgs.size(); i++) {
							DataGrid dg = targetDgs.get(i);
							String dgConfig = dg.getConfig();
							if (dgConfig != null && dgConfig.length() > 0) {
								Map<String, Object> dgConfigMap = new EcExtraViewIntegrationUtils().ecSplitConfig(dgConfig);
								if (dgConfigMap.get("config") != null) {
									dg.setConfig(dgConfigMap.get("config").toString());
									dataGridDao.update(dg);
								}

								if (dgConfigMap.get("fieldConfig") != null) {
									String fieldConfig = dgConfigMap.get("fieldConfig").toString();
									fieldService.saveFields(dg, fieldConfig, null, null, null);
									buttonService.saveButton(dg, fieldConfig, null);
								}
								// 复制datagrid中的自定义条件
								List<CustomerCondition> ccs = customerConditionService.findCustomerConditions(
										Restrictions.like("code", dgs.get(i).getCode(), MatchMode.START), Restrictions.eq("valid", true));
								viewDao.flush();
								viewDao.clear();
								if (ccs != null && !ccs.isEmpty()) {
									for (CustomerCondition cc : ccs) {
										cc.setCode(cc.getCode().replace(dgs.get(i).getCode(), dg.getCode()));
										cc.setView(view);
										cc.setDataGrid(dg);
										customerConditionService.saveCustomerCondition(cc);
									}
								}
							}
						}
					}
				}
				if ((view.getShowType() != ShowType.LAYOUT&&view.getShowType() != ShowType.LAYOUT2)&&(view.getType() == ViewType.LIST || view.getType() == ViewType.REFERENCE || (view.getEditViewType() != null && view.getEditViewType() == 1))) {
					ExtraView targetExtraView = getExtraView(view);
					String extraViewConfig = targetExtraView.getConfig();
					if (srcView.getFastQueryJson().size() > 0) {
						for (FastQueryJson fqj : srcView.getFastQueryJson()) {
							if (fqj != null) {
								fastQueryJsonDao.evict(fqj);
								if (view.getEditViewType() != null && view.getEditViewType() == 1) {
									String srcCode = fqj.getCode();
									String targetCode = view.getCode() + System.currentTimeMillis();
									extraViewConfig = extraViewConfig.replaceAll(srcCode, targetCode);
									fqj.setCode(targetCode);
								} else {
									fqj.setCode(view.getCode());
								}
								fqj.setView(view);
								fqj.setFields(null);
								fastQueryJsonDao.save(fqj);
								if (view.getEditViewType() != null && view.getEditViewType() == 1) {
									String fqjConfig = fqj.getQueryConfig();
									if (fqjConfig != null && fqjConfig.length() > 0) {
										Map<String, Object> fqjConfigMap = new EcExtraViewIntegrationUtils().ecSplitByQueryConfig(fqjConfig);

										if (fqjConfigMap.get("fieldConfig") != null) {
											String fieldConfig = fqjConfigMap.get("fieldConfig").toString();
											fastQueryJsonService.saveFields(fqj, fieldConfig);
											buttonService.saveButton(fqj, fieldConfig, null);
										}
									}
								}
							}
						}
					}

					if (srcView.getAdvQueryJson().size() > 0) {
						for (AdvQueryJson aqj : srcView.getAdvQueryJson()) {
							if (aqj != null) {
								advQueryJsonDao.evict(aqj);
								if (view.getEditViewType() != null && view.getEditViewType() == 1) {
									String srcCode = aqj.getCode();
									Long currentTimes = System.currentTimeMillis();
									String targetCode = view.getCode() + currentTimes;
									aqj.setCode(targetCode);
									aqj.setName(view.getName() + currentTimes);
									extraViewConfig = extraViewConfig.replaceAll(srcCode, targetCode);
								} else {
									aqj.setCode(view.getCode());
								}
								aqj.setView(view);
								aqj.setFields(null);
								advQueryJsonDao.save(aqj);
								if (view.getEditViewType() != null && view.getEditViewType() == 1) {
									String aqjConfig = aqj.getQueryConfig();
									if (aqjConfig != null && aqjConfig.length() > 0) {
										Map<String, Object> aqjConfigMap = new EcExtraViewIntegrationUtils().ecSplitByQueryConfig(aqjConfig);

										if (aqjConfigMap.get("fieldConfig") != null) {
											String fieldConfig = aqjConfigMap.get("fieldConfig").toString();
											advQueryJsonService.saveFields(aqj, fieldConfig);
											buttonService.saveButton(aqj, fieldConfig, null);
										}
									}
								}
							}
						}
					}
					if (view.getEditViewType() != null && view.getEditViewType() == 1) {
						targetExtraView.setConfig(extraViewConfig);
						viewDao.mergeExtraView(targetExtraView);
					}
				}
			} else {
				saveExtraView(ev, null);
			}
		}
		if (needCopyExtraView) {
			// 复制视图中的自定义条件
			List<CustomerCondition> ccs = customerConditionService.findCustomerConditions(Restrictions.like("code", srcView.getCode(), MatchMode.START),
					Restrictions.eq("valid", true));
			// List<CustomerCondition> ccs = customerConditionService.findCustomerConditionsByCode(srcView.getCode());
			viewDao.flush();
			viewDao.clear();
			if (ccs != null && !ccs.isEmpty()) {
				for (CustomerCondition cc : ccs) {
					cc.setCode(cc.getCode().replace(srcView.getCode(), view.getCode()));
					cc.setView(view);
					customerConditionService.saveCustomerCondition(cc);
				}
			}

		}
		if(needCopyExtraView){
			List<DataGroup> srcDataGroups = findDataGroups(srcView);
			if (srcDataGroups != null && srcDataGroups.size() > 0) {
				for (DataGroup datagroup : srcDataGroups) {
					DataGroup newDataGroup = new DataGroup();
					newDataGroup.setName(datagroup.getName());
					newDataGroup.setDisplayName(datagroup.getDisplayName());
					newDataGroup.setIsMultiple(datagroup.getIsMultiple());
					newDataGroup.setView(view);
					newDataGroup.setCode(view.getCode() + "_" + newDataGroup.getName());
					saveDataGroup(newDataGroup);
					List<DataClassific> srcDataClassifics = findDataClassifics(datagroup);
					if (srcDataClassifics != null && srcDataClassifics.size() > 0) {
						for (DataClassific dclassific : srcDataClassifics) {
							DataClassific newdclassific = new DataClassific();
							newdclassific.setName(dclassific.getName());
							newdclassific.setDisplayName(dclassific.getDisplayName());
							newdclassific.setCondition(dclassific.getCondition());
							newdclassific.setDataGroup(newDataGroup);
							newdclassific.setCode(newDataGroup.getCode() + "_" + newdclassific.getName());
							CustomerCondition cc = customerConditionService.getCustomerCondition(dclassific);
							CustomerCondition newcc = new CustomerCondition();
							BeanUtils.copyProperties(cc, newcc);
							newcc.setView(view);
							newcc.setDataClassific(newdclassific);
							newcc.setCode(view.getCode() + "_" + newdclassific.getCode());
							saveDataClassific(newdclassific);
							customerConditionService.saveCustomerCondition(newcc);
						}
					}
				}
			}
		}
	}

	/**
	 * 一个实体下视图名称唯一
	 */
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	private boolean checkNameUnique(View view) {
		List<Object> parameters = new LinkedList<Object>();
		Entity entity = view.getEntity();
		// 对于已删除的视图，新建的视图不允许与其重名
		// 同时判断视图是否是新建，通过createStaff modifyStaff deleteStaff是否都为空来判断，主键是直接赋值的，不会为空
		String tempViewCode = null;
		StringBuffer sql = new StringBuffer("select CODE from ");
		String tableName="";
		if(ProjectFlagHolder.getInstance().getProjFlag().get()!=null &&ProjectFlagHolder.getInstance().getProjFlag().get()){
			tableName = "project_view";
		}else {
			tableName = View.TABLE_NAME;
		}
		sql.append(tableName).append(" where ENTITY_CODE = ? and lower(NAME) = ? and VALID = 1");
		if (StringUtils.isEmpty(view.getCode())) {
			tempViewCode = view.getEntity().getCode() + "_" + view.getName();
			parameters.add(entity.getCode());
			parameters.add(view.getName().toLowerCase());
		} else {
			tempViewCode = view.getCode();
			sql.append(" and lower(CODE) != ?");
			parameters.add(entity.getCode());
			parameters.add(view.getName().toLowerCase());
			parameters.add(view.getCode().toLowerCase());
		}
		Object[] params = new Object[parameters.size()];
		List<String> viewCodes = viewDao.createNativeQuery(sql.toString(), parameters.toArray(params)).list();
		if (null != viewCodes) {
			for (String viewCode : viewCodes) {
				if (tempViewCode != null && tempViewCode.equalsIgnoreCase(viewCode)) {
					return false;
				}
			}
		}
		return true;

	}

	private boolean checkViewNamekey(View view) {
		String name = view.getName();// java关键字
		if (!propertyKeyService.checkJavaKey(name) || !propertyKeyService.checkDBKey(name) || !propertyKeyService.checkBapKey(name)) {
			log.error("不允许使用关键字：" + name);
			return false;
		}
		return true;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Page<View> findViews(Page<View> page, Criterion... criterions) {
		Criterion validCriterion = Restrictions.eq("valid", true);
		if (null == criterions) {
            criterions = new Criterion[] { validCriterion };
        } else {
			Criterion[] cs = new Criterion[criterions.length + 1];
			System.arraycopy(criterions, 0, cs, 0, criterions.length);
			cs[criterions.length] = validCriterion;
			criterions = cs;
		}
		return viewDao.findByPage(page, criterions);
	}

	/**
	 *
	 * 校验配置的Datagrid或者DataTable是否配置了字段
	 *
	 */
	@Override
	@Transactional
	public void checkDataGridFildNull(List<DataGrid> dgList) {
		if (dgList != null) {
			String errorMsg = "";
			for (DataGrid dataGrid : dgList) {
				if (dataGrid.getConfig() == null || "".equals(dataGrid.getConfig())) {
					if (!"".equals(errorMsg)) {
						errorMsg += "、";
					}
					if (null != dataGrid.getTargetModel().getName() && !"".equals(dataGrid.getTargetModel().getName())) {
						errorMsg += InternationalResource.get(dataGrid.getTargetModel().getName());
					} else {
						errorMsg += dataGrid.getName();
					}

				}
			}
			if (errorMsg != null && !"".equals(errorMsg)) {
				throw new EcException(errorMsg + InternationalResource.get("ec.msModule.module.field"));
			}
		}
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<View> findAllViews(Criterion... criterions) {
		return viewDao.findByCriteria(criterions);
	}

	/**
	 * 根据提供的条件，查询视图
	 *
	 * @param entity
	 * @param valid
	 *            0只返回作废视图，1只返回有效视图，3全部
	 * @param showType
	 *            显示类型，0片断，1布局，2独立的页面,3非片断，4全部
	 * @param mobile
	 *            0返回非移动配置视图，1返回移动配置视图，3全部
	 * @param viewTypes
	 *            视图类型
	 * @return
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<View> findViews(Entity entity, int valid, int showType, int mobile, ViewType... viewTypes) {
		StringBuffer hql = new StringBuffer("from View v where v.entity = ? and v.valid = true and v.mobile != true");
		// 添加showtype过虑条件
		// 显示类型，显示类型，0片断，1布局，2独立的页面,3非片断，4全部
		if (showType == 3) {
			hql.append(" and v.showType != 'PART'");
		}
		Object[] params = new Object[] { entity };
		if (null != viewTypes && viewTypes.length > 0) {
			params = new Object[viewTypes.length + 1];
			params[0] = entity;
			hql.append(" and ( ");
			for (int i = 0; i < viewTypes.length; i++) {
				if (i > 0) {
                    hql.append(" or ");
                }
				hql.append("v.type = ?");
				params[i + 1] = viewTypes[i];
			}
			hql.append(" )");
		}
		List<View> views = viewDao.findByHql(hql.toString(), params);
		String hqlMobile = "select v1.code from View v, View v1 where v1.code || '__mobile__' = v.code and v1.valid = true and v.valid = true and v.entity = ?0";
		List<String> mobileViewCodes = viewDao.findByHql(hqlMobile, entity);
		for (View v : views) {
			if (mobileViewCodes.contains(v.getCode())) {
				v.setExistMobileConfig(true);
			}
		}
		return views;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Page<View> findViews(Page<View> page, Entity entity) {
		Page<View> views = findViews(page, Restrictions.eq("entity", entity),
				Restrictions.or(Restrictions.eq("mobile", Boolean.FALSE), Restrictions.isNull("mobile")));
		// 查询是否存在移动特有配置
		//DOTO
		String hql = "select v1.code from View v, View v1 where v1.code || '__mobile__' = v.code and v1.valid = true and v.valid = true and v1.entity = ?";
		List<String> mobileViewCodes = viewDao.findByHql(hql, entity);
		for (View v : views.getResult()) {
			if (mobileViewCodes.contains(v.getCode())) {
				v.setExistMobileConfig(true);
			}
		}
		return views;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<View> findViewList(Entity entity) {
		return viewDao.findByCriteria(Restrictions.eq("valid", true), Restrictions.eq("entity", entity));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<View> findViews(Entity entity, ViewType... viewTypes) {
		String hql = "from View v where v.entity.code = ? and v.valid = true and ((v.mobile != true and v.mobileEnableFlag = true) or (v.mobile is null or v.mobile = false))";
		Object[] params = new Object[] { entity };
		if (null != viewTypes && viewTypes.length > 0) {
			params = new Object[viewTypes.length + 1];
			params[0] = entity.getCode();
			hql += " and ( ";
			for (int i = 0; i < viewTypes.length; i++) {
				if (i > 0) {
                    hql += " or ";
                }
				hql += "v.type = ?";
				params[i + 1] = viewTypes[i];
			}
			hql += " )";
		}
		return viewDao.findByHql(hql, params);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<String> findEngineViewUrl(Entity entity, ViewType... viewTypes) {
		String sql = "SELECT URL FROM ec_view WHERE ENTITY_CODE = ? AND VALID = 1 AND ((MOBILE != 1 and MOBILE_ENABLE_FLAG = 1) OR (MOBILE IS NULL OR MOBILE = 0)) ";
		Object[] params = new Object[] { entity };
		if (null != viewTypes && viewTypes.length > 0) {
			params = new Object[viewTypes.length + 1];
			params[0] = entity.getCode();
			sql += " and ( ";
			for (int i = 0; i < viewTypes.length; i++) {
				if (i > 0) {
                    sql += " or ";
                }
				sql += " TYPE = ?";
				params[i + 1] = viewTypes[i].toString();
			}
			sql += " )";
		}
		return viewDao.createNativeQuery(sql, params).list();
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<View> findViews(Module module) {
		return viewDao.findByHql("from View v where v.entity.module = ? and v.valid = true", module);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<View> findViewsByModuleCode(String moduleCode) {
		return viewDao.findByHql("from View v where v.entity.module.code = ? and v.valid = true", moduleCode);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<View> findViewsByAssModelCode(String modelCode, ViewType... viewTypes) {
		String hql = "from View v where v.assModel.code = ? and v.valid = true and (v.mobile = false or v.mobile is null)";
		Object[] params = new Object[] { modelCode };
		if (null != viewTypes && viewTypes.length > 0) {
			params = new Object[viewTypes.length + 1];
			params[0] = modelCode;
			hql += " and ( ";
			for (int i = 0; i < viewTypes.length; i++) {
				if (i > 0) {
                    hql += " or ";
                }
				hql += "v.type = ?";
				params[i + 1] = viewTypes[i];
			}
			hql += " )";
		}
		return viewDao.findByHql(hql, params);
	}
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<View> findProjViewsByAssModelCode(String modelCode, ViewType... viewTypes) {
		String hql = "from View v where v.assModel.code = ? and v.valid = true and (v.mobile = false or v.mobile is null) and v.projFlag=1 ";
		Object[] params = new Object[] { modelCode };
		if (null != viewTypes && viewTypes.length > 0) {
			params = new Object[viewTypes.length + 1];
			params[0] = modelCode;
			hql += " and ( ";
			for (int i = 0; i < viewTypes.length; i++) {
				if (i > 0) {
					hql += " or ";
				}
				hql += "v.type = ?";
				params[i + 1] = viewTypes[i];
			}
			hql += " )";
		}
		return viewDao.findByHql(hql, params);
	}
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<View> findMobileViewsByAssModelCode(String modelCode, ViewType... viewTypes) {
		String hql = "from View v where v.assModel.code = ? and v.valid = true and (v.mobile = false or v.mobile is null)";
		Object[] params = new Object[] { modelCode };
		if (null != viewTypes && viewTypes.length > 0) {
			params = new Object[viewTypes.length + 1];
			params[0] = modelCode;
			hql += " and ( ";
			for (int i = 0; i < viewTypes.length; i++) {
				if (i > 0)
					hql += " or ";
				hql += "v.type = ?";
				params[i + 1] = viewTypes[i];
			}
			hql += " )";
		}
		List<View> viewList=viewDao.findByHql(hql, params);
		List<View> mobileViewList=new ArrayList<View>();
		if(null!=viewList&&!viewList.isEmpty()){
			// 查询是否存在移动特有配置
			String hql1 = "select v1.code from View v, View v1 where v1.code || '__mobile__' = v.code and v1.valid = true and v.valid = true and v.assModel.code = ?0";
			List<String> mobileViewCodes = viewDao.findByHql(hql1, modelCode);
			for (View view : viewList) {
				if (mobileViewCodes.contains(view.getCode())) {
					mobileViewList.add(view);
				}
			}
		}
		return mobileViewList;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<View> findViewsByAssModelCode(String modelCode, int editViewType, ViewType... viewTypes) {
		String hql = "from View v where v.assModel.code = ? and v.valid = true and (v.mobile = false or v.mobile is null) and v.editViewType=? ";
		Object[] params = new Object[] { modelCode, editViewType };
		if (null != viewTypes && viewTypes.length > 0) {
			params = new Object[viewTypes.length + 2];
			params[0] = modelCode;
			params[1] = editViewType;
			hql += " and ( ";
			for (int i = 0; i < viewTypes.length; i++) {
				if (i > 0) {
                    hql += " or ";
                }
				hql += "v.type = ?";
				params[i + 2] = viewTypes[i];
			}
			hql += " )";
		}
		return viewDao.findByHql(hql, params);
	}
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<View> findProjViewsByAssModelCode(String modelCode, int editViewType, ViewType... viewTypes) {
		String hql = "from View v where v.assModel.code = ? and v.valid = true and (v.mobile = false or v.mobile is null) and v.editViewType=? and v.projFlag=1 ";
		Object[] params = new Object[] { modelCode, editViewType };
		if (null != viewTypes && viewTypes.length > 0) {
			params = new Object[viewTypes.length + 2];
			params[0] = modelCode;
			params[1] = editViewType;
			hql += " and ( ";
			for (int i = 0; i < viewTypes.length; i++) {
				if (i > 0) {
					hql += " or ";
				}
				hql += "v.type = ?";
				params[i + 2] = viewTypes[i];
			}
			hql += " )";
		}
		return viewDao.findByHql(hql, params);
	}
	// @Override
	// @Transactional(readOnly = true)
	// public Sql getSql(Long viewId, int type) {
	// return sqlService.getSql(viewId, type);
	// }
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Sql getSql(String viewCode, int type) {
		return sqlService.getSql(viewCode, type);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Sql getSql(String viewCode, String datagridCode, int type) {
		return sqlService.getSql(viewCode, datagridCode, type);
	}

	@Override
	@Transactional
	public void saveViewConfig(ExtraView ev, Map map) {
		saveExtraView(ev, map);
		View view = getView(ev.getView().getCode());
		if(view.getExtraView() ==null){
			view.setExtraView(ev);
		}
		view.setHasCustomSection((Boolean)map.get("hasCustomSection"));
		// 移动视图，保存后才启用
		if (view.getMobile() != null && view.getMobile()) {
			if (view.getMobileEnableFlag() == null
					|| !view.getMobileEnableFlag()) {
				view.setMobileEnableFlag(Boolean.TRUE);
			}
		}
		saveView(view);
		modifyShadowViewCustomSection(view.getCode(),
				view.getHasCustomSection());
		dealPrintButton(view);
		saveViewJson(view);
	}

	@Override
	@Transactional
	public void saveExtraView(ExtraView ev, Map argsMap) {
		ev.setFullConfig(null);
		viewDao.saveExtraView(ev);
		View view = ev.getView();
		if (view == null || view.getType() == null) {
			view = getView(ev.getView().getCode());
		}

		if (view.getType() == ViewType.EDIT || view.getType() == ViewType.VIEW || view.getType() == ViewType.EXTRA) {
			if (view.getExtraView() != null) {
				String configMap = view.getExtraView().getConfig();
				List<DataGrid> dgList = dataGridService.getDataGridByViewCode(view.getCode());
				List<Field> fieldList = fieldService.findFields(view.getCode());
				if (configMap != null && configMap.length() > 0) {
					for (DataGrid dg : dgList) {
						if (!configMap.contains("<datagridCode><![CDATA[" + dg.getCode() + "]]></datagridCode>") && !view.getName().contains(View.MOBILE_VIEW_SUFFIX)) {
							dataGridService.deleteDataGridPhysical(dg);
						}
					}
					for (Field field : fieldList) {
						if (field.getCellCode() != null && !field.getCellCode().equals("")
								&& !configMap.contains("<cellCode><![CDATA[" + field.getCellCode() + "]]></cellCode>")) {
							fieldService.deleteField(field);
						}
					}
				}
				List<FastQueryJson> fqjList = view.getFastQueryJson();
				List<AdvQueryJson> aqjList = view.getAdvQueryJson();
				List<DataGroup> dataGroups = null;
				Set<String> layoutNames = new HashSet<String>();
				if(null!=fqjList){
					for (FastQueryJson fqjs : fqjList) {
						if (!configMap.contains("<fqjCode><![CDATA[" + fqjs.getCode() + "]]></fqjCode>")) {
							if (null != fqjs.getLayoutName() && !"".equals(fqjs.getLayoutName())) {
								layoutNames.add(fqjs.getLayoutName());
							}
							fastQueryJsonService.deletePhysical(fqjs);
						} else {
							fastQueryJsonService.deleteField(fqjs, false);
						}
					}
				}
				if(null!=aqjList){
					for (AdvQueryJson aqjs : aqjList) {
						if (!configMap.contains("<aqjCode><![CDATA[" + aqjs.getCode() + "]]></aqjCode>")) {
							if (null != aqjs.getLayoutName() && !"".equals(aqjs.getLayoutName())) {
								layoutNames.add(aqjs.getLayoutName());
							}
							advQueryJsonService.deletePhysical(aqjs);
						} else {
							advQueryJsonService.deleteField(aqjs, false);
						}
					}
				}
				if (layoutNames.size() > 0) {
					dataGroups = findDataGroupByLayoutName(view, layoutNames);
					for (DataGroup dataGroup : dataGroups) {
						List<DataClassific> dcs = findDataClassifics(dataGroup);
						deleteDataClassific(dcs);
						deleteDataGroup(dataGroup);
					}
				}
			}
		}
		viewDao.flush();
		if (argsMap != null) {
			FastQueryJson fqj = (FastQueryJson) argsMap.get("fqj");
			AdvQueryJson aqj = (AdvQueryJson) argsMap.get("aqj");
			String fieldConfig = (String) argsMap.get("fieldConfig");
			String delCellIds = (String) argsMap.get("delCellIds");
			String delEventIds = (String) argsMap.get("delEventIds");
			String delValidateIds = (String) argsMap.get("delValidateIds");
			String btDelCellIds = (String) argsMap.get("btDelCellIds");
			String fieldSelectionRange = (String) argsMap.get("fieldSelectionRange");
			Boolean needBackup = Boolean.valueOf(argsMap.get("needBackup").toString());
			if (needBackup) {
				if (ev.getConfig() != null) {
					List<BackupView> bvList = getBackupViews(view.getCode());
					if (null != bvList && !bvList.isEmpty() && bvList.size() > totalNum) {
						deleteBackupView(view.getCode(), new Date(new Date().getTime() - beforeDateNum * 3600000L * 24L));
					}
					BackupView bv = new BackupView();
					bv.setView(view);
					bv.setPublishDate(new Date());
					bv.setPublishStaff(getCurrentStaff());
					if (null != fieldConfig && fieldConfig.length() > 0) {
						bv.setFieldConfig(fieldConfig);
					}
					bv.setConfig(ev.getConfig());
					bv.setCode(view.getCode() + "_" + new Timestamp(System.currentTimeMillis()));
					saveBackupView(bv);
				}
			}
			if (fieldConfig != null) {
				if (view.getType() != ViewType.MNECODE) {
					buttonService.saveButton(view, fieldConfig, btDelCellIds);
				}
				fieldService.saveFields(view, fieldConfig, delCellIds, delEventIds, delValidateIds);
				if (view.getType() != ViewType.MNECODE) {
					eventService.saveEvent(view, fieldConfig);
				}
			}
			if ((view.getType() == ViewType.LIST || view.getType() == ViewType.REFERENCE) && view.getShowType() != ShowType.LAYOUT && fqj != null) {
				if (null == fqj.getCode() || fqj.getCode().length() == 0) {
					fqj.setCode(view.getCode());
				}
				saveFastQueryJson(fqj);
				List<FastQueryJson> viewFastQueryJsons = new ArrayList<FastQueryJson>();

				viewFastQueryJsons.add(fqj);
				view.setFastQueryJson(viewFastQueryJsons);
			}
			if ((view.getType() == ViewType.LIST || view.getType() == ViewType.REFERENCE) && !view.getMobile() && view.getShowType() != ShowType.LAYOUT && aqj != null) {
				if (null == aqj.getCode() || aqj.getCode().length() == 0) {
					aqj.setCode(view.getCode());
				}
				saveAdvQueryJson(aqj);
				List<AdvQueryJson> viewAdvQueryJsons = new ArrayList<AdvQueryJson>();
				viewAdvQueryJsons.add(aqj);
				view.setAdvQueryJson(viewAdvQueryJsons);
			}
			//列表视图添加init和rendover事件
			if ((view.getType() == ViewType.LIST || view.getType() == ViewType.REFERENCE) && view.getShowType() != ShowType.LAYOUT && aqj != null) {
				String config = view.getExtraView().getConfig();
				Map listPropertyMap = new HashMap();
				if(config.indexOf("<renderOver>") > 0 && config.indexOf("</renderOver>") > 0) {
					String listPropertyConfig = config.substring(config.indexOf("<renderOver>"), config.indexOf("</renderOver>") + 13);
					Object obj = SerializeUitls.deserialize(listPropertyConfig);
					if(null != obj){
						listPropertyMap.putAll((Map)obj);

					}
				}

				if(config.indexOf("<ptPageInit>") > 0 && config.indexOf("</ptPageInit>") > 0) {
					String listPropertyConfig = config.substring(config.indexOf("<ptPageInit>"), config.indexOf("</ptPageInit>") + 13);
					Object obj = SerializeUitls.deserialize(listPropertyConfig);
					if(null != obj){
						listPropertyMap.putAll((Map)obj);

					}
				}
				String viewCode = view.getCode();
				String moduleCode = view.getModuleCode();
				String entityCode = view.getEntity().getCode();
				Event eRenderOver = eventService.getEvent(viewCode + "_renderOver");
				if (null == eRenderOver) {
					eRenderOver = new Event();
				}
				eRenderOver.setCode(viewCode + "_renderOver");
				eRenderOver.setName(viewCode + "_renderOver");
				if(listPropertyMap.containsKey("renderOver") && null != listPropertyMap.get("renderOver")){
					eRenderOver.setFunction(listPropertyMap.get("renderOver").toString());
				}
				eRenderOver.setModuleCode(moduleCode);
				eRenderOver.setEntityCode(entityCode);
				eventService.saveEvent(eRenderOver);

				Event ePageInit = eventService.getEvent(viewCode + "_ptPageInit");
				if (null == ePageInit) {
					ePageInit = new Event();
				}
				ePageInit.setCode(viewCode + "_ptPageInit");
				ePageInit.setName(viewCode + "_ptPageInit");
				if(listPropertyMap.containsKey("ptPageInit") && null != listPropertyMap.get("ptPageInit")){
					ePageInit.setFunction(listPropertyMap.get("ptPageInit").toString());
				}
				ePageInit.setModuleCode(moduleCode);
				ePageInit.setEntityCode(entityCode);
				eventService.saveEvent(ePageInit);
			}
			if (fieldSelectionRange != null && fieldSelectionRange.length() > 0) {
				Map rangeMap = (Map) SerializeUitls.deserialize(fieldSelectionRange);
				List<Map> list = (List<Map>) rangeMap.get("ranges");
				String viewCode = view.getCode();
				String regionType = "EDIT";
				if (null != list && !list.isEmpty()) {
					for (Map map : list) {
						if (map.get("propertycode") != null) {
							String propertyCode = map.get("propertycode").toString();
							if (map.get("staffselecttype") != null) {
								String selectType = map.get("staffselecttype").toString();
								String fc = viewCode + "_" + regionType + "_" + "OTHER_" + propertyCode.replace("||", "_");
								if ("customer".equals(selectType)) {
									// 选人范围
									String ids = map.get("ids") == null ? null : map.get("ids").toString();
									// 分组名称
									String gns = map.get("gns") == null ? null : map.get("gns").toString();
									// 排序sort
									String ss = map.get("ss") == null ? null : map.get("ss").toString();
									// 主键
									String us = map.get("us") == null ? null : map.get("us").toString();
									// 范围名称
									String sns = map.get("sns") == null ? null : map.get("sns").toString();

									if (ids != null && ids.length() > 0) {
										String[] staffIds = ids.split(",");
										String[] groupNames = gns.split(",");
										String[] sorts = ss.split(",");
										String[] pks = us.split(",");
										String[] staffnames = sns.split(",");
										for (int i = 0; i < pks.length; i++) {
											SelectionRange range = null;
											if (pks[i] != null && !"-1".equals(pks[i]) && pks[i].length() > 0) {
												range = fieldService.getSelectionRangeById(Long.parseLong(pks[i]));
											} else {
												range = new SelectionRange();
												range.setVersion(0);
												if (staffIds[i] != null && staffIds[i].length() > 0) {
													range.setRangeId(Long.parseLong(staffIds[i]));
												}

												range.setGroupName(groupNames[i]);
												if (sorts[i] != null && sorts[i].length() > 0) {
													range.setSort(Double.parseDouble(sorts[i]));
												}
												range.setRangeName(staffnames[i]);
												range.setFieldCode(fc);
												range.setType(selectType);
												fieldService.saveSelectionRange(range);
											}

										}

										if (map.get("selectPeopleDel") != null) {
											String delIds = map.get("selectPeopleDel").toString();
											if (delIds != null && delIds.length() > 0) {
												String[] delids = delIds.split(",");
												for (String id : delids) {
													if (id != null && id.length() > 0) {
														fieldService.deleteSelectionRange(Long.parseLong(id));
													}
												}
											}
										}
									} else {
										fieldService.deleteSelectionRangeByField(fieldService.getField(fc));
										fieldService.clearCache(fc);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private List<DataGroup> findDataGroupByLayoutName(View view, Set<String> layoutNames) {
		String hql = "from DataGroup where view.code = :viewCode and layoutName in (:layouNames)";
		return dataGridDao.createQuery(hql).setParameter("viewCode", view.getCode()).setParameterList("layouNames", layoutNames).list();
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ExtraView getExtraView(View view) {
		return (ExtraView) viewDao.createQuery("from ExtraView where view = ?0", view).uniqueResult();
	}

	@Override
	@Transactional
	public void publish(ExtraView ev) {
		saveExtraView(ev, null);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public ExtraView defaultExtraView(View view) {
		ExtraView ev = new ExtraView();
		StringBuffer config = null;
		if (view.getCustomFlag() != null && view.getCustomFlag()) {
			StringWriter writer = new StringWriter();
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("view", view);
			// generateService.renderFtl("config-custom-default.ftl", writer, map);
			config = writer.getBuffer();
			try {
				writer.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
			ev.setConfigMap((Map) XmlUtils.convert(config.toString()));
		} else {
			config = new StringBuffer();
		}
		ev.setConfig(config.toString());
		return ev;
	}

	public void processEditAction(View view) {
		ExtraView ev = view.getExtraView();
		if (null != ev) {

		}
	}

	@Override
	@Transactional
	public void saveButtonOperate(View view, Button button) {
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
		bi = buttonInfoService.load(buttonCode);
		StringBuilder sb = new StringBuilder();
		if (null != button.getIsPermission() && button.getIsPermission()) {
			if (null == bi) {
				bi = new ButtonInfo();
			}
			if (button.getOperateType() == OperateType.CUSTOM) {
				url = (String) button.getOperateUrl();
				if (null != url) {
					nameSapce = url.substring(0, url.lastIndexOf("/"));
					actionName = url.substring(url.lastIndexOf("/") + 1, (url.lastIndexOf(".") == -1) ? url.length() : url.lastIndexOf("."));
				}
			} else {
				if (button.getOperateType() == OperateType.ADD || button.getOperateType() == OperateType.MODIFY) {
					if (null != button.getViewSelect() && button.getViewSelect().getCode().length() > 0) {
                        viewSelect = button.getViewSelect().getCode();
                    }
					View select = getView(viewSelect);
					actionName = select.getName();
					url = getView(viewSelect).getUrl();
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
					url = sb.toString();
				} else if (button.getOperateType() == OperateType.SORT) {
					actionName = "sort";
					sb.append("/");
					sb.append(view.getAssModel().getEntity().getModule().getArtifact());
					sb.append("/");
					sb.append(view.getAssModel().getEntity().getEntityName());
					sb.append("/");
					sb.append(view.getAssModel().getModelName().substring(0, 1).toLowerCase() + view.getAssModel().getModelName().substring(1));
					sb.append("/");
					sb.append(view.getName()).append("Sort");
					url = sb.toString();
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
					url = sb.toString();
				}
			}
			buttonName = (String) button.getDisplayName();
			{
				if (buttonCode != null && buttonCode.length() > 0) {
					ButtonInfo tmp = buttonInfoService.load(buttonCode);
					if (tmp != null) {
						bi = tmp;
					}
				}
			}
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
			buttonInfoService.save(bi);
		} else {
			String operateCode = null;
			if (view.getType() == ViewType.EDIT || view.getType() == ViewType.VIEW) {
				List<Button> buttonList = buttonDao.findByHql("from Button where permissionCode=? and view.entity.code=? and code!=?",
						button.getPermissionCode(), view.getEntity().getCode(), button.getCode());
				if (null == buttonList || buttonList.isEmpty()) {
					operateCode = button.getView().getEntity().getCode() + "_" + button.getPermissionCode();
				}
			} else {
				if (null != bi) {
					operateCode = bi.getCode();
				}
			}
			if (null != operateCode) {
				menuOperateService.deleteMenuOperateByPhysical(operateCode);
			}
		}
	}

	@Override
	@Transactional
	public void saveSql(Sql sql) {
		Assert.notNull(sql);
		sqlService.save(sql);
	}

	@Override
	@Transactional
	public void saveSqls(Sql... sqls) {
		if (null != sqls && sqls.length > 0) {
            for (Sql sql : sqls) {
                saveSql(sql);
            }
        }
	}

	@Override
	@Transactional
	public void saveFastQueryJson(FastQueryJson fastQueryJson) {
		Assert.notNull(fastQueryJson);
		viewDao.saveFastQueryJson(fastQueryJson);
	}

	@Override
	@Transactional
	public void saveAdvQueryJson(AdvQueryJson advQueryJson) {
		Assert.notNull(advQueryJson);
		viewDao.saveAdvQueryJson(advQueryJson);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public BackupView getBackupView(String code) {
		return backupViewDao.findEntityByHql("from BackupView where code = ?0", code);
	}

	@Override
	@Transactional
	public void restoreView(String code) {
		BackupView backupView = getBackupView(code);
		if (null == backupView) {
			throw new EcException(EcException.Code.OBJECT_NULL);
		}
		View view = getView(backupView.getView().getCode());
		ExtraView extraView = getExtraView(view);
		extraView.setConfig(backupView.getConfig());
		extraView.setFullConfig(null);
		viewDao.saveExtraView(extraView);
		dataGridEnable(view, backupView);
		fieldService.deleteFieldByViewCode(view.getCode());
		fieldService.saveFields(view, backupView.getFieldConfig(), null, null, null);
		buttonService.saveButton(view, backupView.getFieldConfig(), null);

		List<DataGrid> dataGrids = dataGridService.getDataGridByView(view, false);
		if (null != dataGrids && !dataGrids.isEmpty()) {
			for (DataGrid dg : dataGrids) {
				BackupDataGrid bd = dataGridService.getBackupDataGrid(dg.getCode(), backupView.getCode());
				dg.setConfig(bd.getConfig());
				dg.setFullConfig(null);
				dataGridService.save(dg);
				fieldService.deleteFieldByDataGrid(dg.getCode());
				fieldService.saveFields(dg, bd.getDgFieldConfig(), null, null, null);
			}
		} else {
			List<BackupDataGrid> bds = dataGridService.getBackupDataGridByBackupViewCode(backupView.getCode());
			if (null != bds && !bds.isEmpty()) {
				for (BackupDataGrid bd : bds) {
					DataGrid dg = new DataGrid();
					int index = bd.getCode().lastIndexOf("_");
					dg.setCode(bd.getCode().substring(0, index));
					dg.setConfig(bd.getConfig());
					dg.setEx(bd.getEx());
					dg.setView(bd.getView());
					dg.setOrgProperty(bd.getOrgProperty());
					dg.setTargetModel(bd.getTargetModel());
					dg.setName(bd.getName());
					dg.setValid(true);
					dg.setFullConfig(null);
					dataGridService.save(dg);
					fieldService.saveFields(dg, bd.getDgFieldConfig(), null, null, null);
				}
			}
		}

	}

	@Override
	@Transactional
	public void saveBackupView(BackupView backupView) {
		backupViewDao.save(backupView);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@Transactional
	public void saveBackupView(View view) {
		BackupView backupView = new BackupView();
		BackupView oldBackupView = getBackupView(view);
		if (null != oldBackupView) {
			List<BackupDataGrid> backupDataGrids = dataGridService.getBackupDataGridByBackupViewCode(oldBackupView.getCode());
			backupView.setView(view);
			backupView.setConfig(oldBackupView.getConfig());
			backupView.setFieldConfig(oldBackupView.getFieldConfig());
			backupView.setCode(view.getCode() + "_" + new Timestamp(System.currentTimeMillis()));
			backupView.setPublishDate(new Date());
			backupView.setPublishStaff(getCurrentStaff());
			saveBackupView(backupView);
			for (BackupDataGrid backupDataGrid : backupDataGrids) {
				BackupDataGrid newBackupDataGrid = new BackupDataGrid();
				newBackupDataGrid.setEx(backupDataGrid.getEx());
				newBackupDataGrid.setOrgProperty(backupDataGrid.getOrgProperty());
				newBackupDataGrid.setTargetModel(backupDataGrid.getTargetModel());
				newBackupDataGrid.setView(view);
				newBackupDataGrid.setConfig(backupDataGrid.getConfig());
				newBackupDataGrid.setDgFieldConfig(backupDataGrid.getDgFieldConfig());
				newBackupDataGrid.setName(backupDataGrid.getName());
				newBackupDataGrid.setBackupView(backupView);
				newBackupDataGrid.setCode(view.getCode() + backupDataGrid.getName() + "_" + new Timestamp(System.currentTimeMillis()));
				dataGridService.saveBackupDataGrid(newBackupDataGrid);
			}
		}
	}

	@Transactional
	private void deleteBackupView(String viewCode, Date date) {
		String findHql = "from BackupView bv where bv.view.code = ?0 and bv.createTime <= ?1";
		List<BackupView> bvList = backupViewDao.findByHql(findHql, new Object[] { viewCode, date });
		if (null != bvList && !bvList.isEmpty()) {
			for (BackupView bv : bvList) {
				String hql = "delete from BackupDataGrid bd where bd.backupView.code = ?0";
				backupViewDao.bulkExecute(hql, bv.getCode());

			}
		}
		String hql = "delete from BackupView bv where bv.view.code = ?0 and bv.createTime <= ?1";
		backupViewDao.bulkExecute(hql, viewCode, date);
	}

	@Transactional
	private void deleteBackupViewByViewCode(String viewCode) {
		String hql = "update BackupView bv set bv.valid = false where bv.view.code = ?0";
		String dgHql = "update BackupDataGrid bd set bd.valid = false where bd.view.code = ?0";
		backupViewDao.bulkExecute(hql, viewCode);
		backupViewDao.bulkExecute(dgHql, viewCode);
	}

	@Transactional
	private void deleteBackupViewByViewCodePhysical(String viewCode) {
		String hql = "delete from BackupView bv where bv.view.code = ?0";
		List<String> backupViewList = backupViewDao.findByHql("select bv.code from BackupView bv where bv.view.code = ?0", viewCode);
		if (!CollectionUtils.isEmpty(backupViewList)) {
			String dgHql = "delete from BackupDataGrid bd where bd.backupView.code in(select bv.code from BackupView bv where bv.view.code = ?0)";
			backupViewDao.bulkExecute(dgHql, viewCode);
		}
		backupViewDao.bulkExecute(hql, viewCode);
	}

	@Override
	@Transactional
	public void deleteBackupView(String bvCode) {
		String code = bvCode.split("@")[0];
		String version = bvCode.split("@")[1];
		dataGridService.deleteBackupDataGridByBackupView(bvCode);
		backupViewDao.delete(code, Integer.valueOf(version));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	private List<BackupView> getBackupViews(String viewCode) {
		return backupViewDao.findByHql("from BackupView bv where bv.view.code = ?0 and bv.valid = true", viewCode);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Page<BackupView> getBackupViewList(Page<BackupView> page, String viewCode) {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(BackupView.class);
		detachedCriteria.add(Restrictions.eq("view.code", viewCode));
		detachedCriteria.add(Restrictions.eq("valid", true));
		detachedCriteria.addOrder(Order.desc("createTime"));
		page = backupViewDao.findByPage(page, detachedCriteria);
		List<BackupView> bvs = page.getResult();

		/*for (BackupView bv : bvs) {
			bv.setCreateStaffId(staffService.get(bv.getCreateStaffId()).getId());
		}*/
		return page;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	private boolean checkDataGroupNameUnique(DataGroup dataGroup) {
		List<Object> parameters = new LinkedList<Object>();
		String hql = "";
		View view = getView(dataGroup.getView().getCode());
		if (dataGroup.getCode() == null || dataGroup.getCode().length() == 0) {
			hql = "select count(dg.code) as dgcount from DataGroup dg where dg.name=?0 and dg.view=?1";
			parameters.add(dataGroup.getName());
			parameters.add(view);
		} else {
			hql = "select count(dg.code) as dgcount from DataGroup as dg where dg.name=?0 and dg.view=?1 and dg.code !=?2";
			parameters.add(dataGroup.getName());
			parameters.add(view);
			parameters.add(dataGroup.getCode());
		}
		Object[] params = new Object[parameters.size()];
		List<Long> dgcount = dataGroupDao.findByHql(hql, parameters.toArray(params));
		if (dgcount.get(0) == 0) {
            return true;
        }
		return false;

	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	private boolean checkDataClassificNameUnique(DataClassific dataClassific) {
		List<Object> parameters = new LinkedList<Object>();
		String hql = "";
		DataGroup dataGroup = getDataGroup(dataClassific.getDataGroup().getCode());
		if (dataClassific.getCode() == null || dataClassific.getCode().length() == 0) {
			hql = "select count(dc.code) as dccount from DataClassific dc where dc.name=?0 and dc.dataGroup=?1";
			parameters.add(dataClassific.getName());
			parameters.add(dataGroup);
		} else {
			hql = "select count(dc.code) as dccount from DataClassific as dc where dc.name=?0 and dc.dataGroup=?1 and dc.code !=?2";
			parameters.add(dataClassific.getName());
			parameters.add(dataGroup);
			parameters.add(dataClassific.getCode());
		}
		Object[] params = new Object[parameters.size()];
		List<Long> dccount = dataClassificDao.findByHql(hql, parameters.toArray(params));
		if (dccount.get(0) == 0) {
            return true;
        }
		return false;

	}

	@Override
	@Transactional
	public void saveDataGroup(DataGroup dataGroup) {
		if (!checkDataGroupNameUnique(dataGroup)) {
			throw new EcException(EcException.Code.UNIQUENAME);
		}
		View view = getView(dataGroup.getView().getCode());
		if (null == dataGroup.getCode() || dataGroup.getCode().length() == 0) {
			dataGroup.setCode(view.getCode() + "_" + dataGroup.getName());
		}
		if (null == dataGroup.getModuleCode()) {
			dataGroup.setModuleCode(view.getModuleCode());
		}
		if (null == dataGroup.getEntityCode()) {
			dataGroup.setEntityCode(view.getEntity().getCode());
		}
		dataGroupDao.save(dataGroup);
	}

	@Override
	@Transactional
	public void saveDataClassific(DataClassific dataClassific) {
		if (!checkDataClassificNameUnique(dataClassific)) {
			throw new EcException(EcException.Code.UNIQUENAME);
		}
		DataGroup dataGroup = getDataGroup(dataClassific.getDataGroup().getCode());
		if (null == dataClassific.getCode() || dataClassific.getCode().length() == 0) {
			dataClassific.setCode(dataGroup.getCode() + "_" + dataClassific.getName());
		}
		if (null == dataGroup.getModuleCode() || null == dataGroup.getEntityCode()) {
			View view = getView(dataGroup.getView().getCode());
			dataClassific.setModuleCode(view.getModuleCode());
			dataClassific.setEntityCode(view.getEntity().getCode());
		}
		if (null == dataClassific.getModuleCode()) {
			dataClassific.setModuleCode(dataGroup.getModuleCode());
		}
		if (null == dataClassific.getEntityCode()) {
			dataClassific.setEntityCode(dataGroup.getEntityCode());
		}
		if(dataClassific.getIsDefault()){
			//更新其他数据分类信息
			if(dataClassific.getProjFlag()!=null && dataClassific.getProjFlag()){
				dataClassificDao.createNativeQuery("update PROJECT_DATA_CLASSIFIC set IS_DEFAULT = 0 where code in (SELECT classific.code FROM PROJECT_DATA_CLASSIFIC classific left join PROJECT_DATA_GROUP dataGroup on classific.DATA_GROUP_CODE = dataGroup.CODE WHERE classific.IS_DEFAULT = 1 AND dataGroup.VIEW_CODE = ? and classific.code <> ?)",new Object[]{dataGroup.getView().getCode(),dataClassific.getCode()}).executeUpdate();
			}else{
				dataClassificDao.createNativeQuery("update EC_DATA_CLASSIFIC set IS_DEFAULT = 0 where code in (SELECT classific.code FROM ec_data_classific classific left join EC_DATA_GROUP dataGroup on classific.DATA_GROUP_CODE = dataGroup.CODE WHERE classific.IS_DEFAULT = 1 AND dataGroup.VIEW_CODE = ? and classific.code <> ?)",new Object[]{dataGroup.getView().getCode(),dataClassific.getCode()}).executeUpdate();
			}		}
		dataClassificDao.save(dataClassific);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<DataGroup> findDataGroups(View view) {
		return dataGroupDao.findByHql("from DataGroup dg where dg.view = ?0 and dg.valid = true order by sort asc", view);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<DataGroup> findDataGroups(View view, String layoutName, String targetModelCode) {
		return dataGroupDao.findByHql("from DataGroup dg where dg.view = ?0 and dg.layoutName = ?1 and dg.targetModel.code = ?2 and dg.valid = true order by sort asc", view, layoutName, targetModelCode);
	}

	private List<DataGroup> findDataGroupsNoValid(View view) {
		return dataGroupDao.findByHql("from DataGroup dg where dg.view = ?0", view);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<DataClassific> findDataClassifics(DataGroup dataGroup) {
		return dataClassificDao.findByHql("from DataClassific dc where dc.dataGroup = ?0 and valid = true order by sort asc", dataGroup);
	}

	/**
	 * @param dg
	 * @return
	 */
	private List<DataClassific> findDataClassificsNoValid(DataGroup dg) {
		return dataClassificDao.findByHql("from DataClassific dc where dc.dataGroup = ?0", dg);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Page<DataClassific> findDataClassifics(Page<DataClassific> page, DataGroup dataGroup) {
		/*List<Criterion> criterions = new ArrayList<Criterion>();
		if (dataGroup == null || dataGroup.getCode() == null) {
			criterions.add(Restrictions.eq("dataGroup", null));
		} else {
			criterions.add(Restrictions.eq("dataGroup", dataGroup));
		}
		criterions.add(Restrictions.eq("valid", true));*/
//		return dataClassificDao.findByPage(page, criterions.toArray(new Criterion[criterions.size()]));
		return dataClassificDao.findByPage(page, "from DataClassific dc where dc.dataGroup = ?0 and valid = true order by sort asc", dataGroup);

	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public DataGroup getDataGroup(String code) {
		return dataGroupDao.findEntityByHql("from DataGroup where code = ?0 and valid = true", code);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public DataClassific getDataClassific(String code) {
		return dataClassificDao.findEntityByHql("from DataClassific where code = ?0 and valid = true", code);
	}
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<DataClassific> getDataClassificInView(DataClassific dataClassific, View view) {
		String viewCode = view.getCode();
		if(dataClassific.getCode()!=null){
			return dataClassificDao.findByHql("from DataClassific dataClassific inner join fetch dataClassific.dataGroup dataGroup inner join fetch dataGroup.view view where view.code=? and dataClassific.valid = true and dataClassific.isDefault = true and dataClassific.code <> ?", new Object[]{viewCode,dataClassific.getCode()});
		}else{
			return dataClassificDao.findByHql("from DataClassific dataClassific inner join fetch dataClassific.dataGroup dataGroup inner join fetch dataGroup.view view where view.code=? and dataClassific.valid = true and dataClassific.isDefault = true", new Object[]{viewCode});
		}

	}
	@Override
	@Transactional
	public void deleteDataGroup(DataGroup dataGroup) {
		if (null != dataGroup) {
			List<DataClassific> dcs = findDataClassifics(dataGroup);
			if (null != dcs && !dcs.isEmpty()) {
				throw new EcException(EcException.Code.ASS_BY_DATACLASSIFIC);
			}
			dataGroupDao.clear();
			dataGroupDao.deletePhysical(dataGroup);
			dataGroupDao.flush();
		}
	}

	@Transactional
	private void deleteDataClassific(List<DataClassific> dcs) {
		for (DataClassific dc : dcs) {
			customerConditionService.deletePhysicalByObject(dc);
			dataClassificDao.deletePhysical(dc);
		}
		dataClassificDao.flush();
	}

	@Override
	@Transactional
	public void deleteDataClassific(DataClassific dataClassific) {
		customerConditionService.deletePhysicalByObject(dataClassific);
		dataClassificDao.deletePhysical(dataClassific);
		dataClassificDao.flush();
	}

	/**
	 * 历史数据处理 ExtraView中添加propertyCode
	 */
	@Override
	@Transactional
	public void modifyExtraViewPropertyCode(Page<View> page, String moduleCode) throws Exception {
		// 处理前备份涉及的表
		backupViewConfig();
		// 编辑视图与查看视图
		if (moduleCode == null) {
			this.findViews(page, Restrictions.or(Restrictions.eq("type", ViewType.EDIT), Restrictions.eq("type", ViewType.VIEW)),
					Restrictions.eq("valid", true));
		} else {
			this.findViews(page, Restrictions.or(Restrictions.eq("type", ViewType.EDIT), Restrictions.eq("type", ViewType.VIEW)),
					Restrictions.eq("valid", true), Restrictions.like("code", moduleCode + "_", MatchMode.START));
		}
		List<View> views = page.getResult();
		for (View view : views) {
			try {
				// view = this.getView(view.getCode(), true);
				if (view == null || view.getExtraView() == null || view.getExtraView().getConfig() == null) {
                    continue;
                }
				this.modifyViewConfig(view);

			} catch (Exception e) {
			}
		}

		// 列表视图、参照视图
		page = new Page<View>();
		page.setPageSize(Integer.MAX_VALUE);
		if (moduleCode == null) {
			this.findViews(page, Restrictions.or(Restrictions.eq("type", ViewType.LIST), Restrictions.eq("type", ViewType.REFERENCE)),
					Restrictions.eq("valid", true));
		} else {
			this.findViews(page, Restrictions.or(Restrictions.eq("type", ViewType.LIST), Restrictions.eq("type", ViewType.REFERENCE)),
					Restrictions.eq("valid", true), Restrictions.like("code", moduleCode + "_", MatchMode.START));
		}
		views = page.getResult();
		for (View view : views) {
			if (view == null || view.getExtraView() == null || view.getExtraView().getConfig() == null) {
                continue;
            }
			this.modifyViewConfig(view);
		}

		Page<DataGrid> pg = new Page<DataGrid>();
		pg.setPageSize(Integer.MAX_VALUE);
		if (moduleCode == null) {
			dataGridService.findDataGrids(pg, Restrictions.eq("valid", true));
		} else {
			dataGridService.findDataGrids(pg, Restrictions.eq("valid", true), Restrictions.like("code", moduleCode + "_", MatchMode.START));
		}
		List<DataGrid> dgs = pg.getResult();
		for (DataGrid dg : dgs) {
			this.modifyDatagridConf(dg);
			dataGridService.save(dg);
		}
	}

	/**
	 * 备份EC_EXTRA_VIEW与EC_DATA_GRID
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@Override
	public void backupViewConfig() {
		if (DbUtils.getDbName().equals("sqlserver")) {
			jdbcTemplate.execute("SELECT * INTO EC_EXTRA_VIEW_bk" + DateUtil.getNoFormatDateString(null) + "  from ec_extra_view");
			jdbcTemplate.execute("SELECT * INTO EC_DATA_GRID_bk" + DateUtil.getNoFormatDateString(null) + " from ec_data_grid");
		} else {
			jdbcTemplate.execute("create table EC_EXTRA_VIEW_bk" + DateUtil.getNoFormatDateString(null) + " as select * from ec_extra_view");
			jdbcTemplate.execute("create table EC_DATA_GRID_bk" + DateUtil.getNoFormatDateString(null) + " as select * from ec_data_grid");
		}
		// SQLQuery query = viewDao.createNativeQuery("create table EC_EXTRA_VIEW_bk" +
		// DateUtil.getNoFormatDateString(null)
		// + " as select * from EC_EXTRA_VIEW");
		// query.executeUpdate();
		// SQLQuery query1 = viewDao.createNativeQuery("create table EC_DATA_GRID_bk" +
		// DateUtil.getNoFormatDateString(null)
		// + " as select * from EC_DATA_GRID");
		// query1.executeUpdate();
	}

	/**
	 * 备份EC_FIELD
	 */
	@Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
	@Override
	public void backupField() {
		String stamp = DateUtil.getNoFormatDateString(null);
		if (DbUtils.getDbName().equals("sqlserver")) {
			jdbcTemplate.execute("SELECT * INTO EC_EVENT_bk" + stamp + "  from ec_event");
			jdbcTemplate.execute("SELECT * INTO EC_VALIDATE_bk" + stamp + " from ec_validate");
			jdbcTemplate.execute("SELECT * INTO EC_FIELD_bk" + stamp + " from ec_field");
		} else {
			jdbcTemplate.execute("create table EC_EVENT_bk" + stamp + " as select * from ec_event");
			jdbcTemplate.execute("create table EC_VALIDATE_bk" + stamp + " as select * from ec_validate");
			jdbcTemplate.execute("create table EC_FIELD_bk" + stamp + " as select * from ec_field");
		}
		// SQLQuery query = viewDao.createNativeQuery("create table EC_EVENT_bk" + stamp +
		// " as select * from EC_EVENT");
		// query.executeUpdate();
		// SQLQuery query1 = viewDao.createNativeQuery("create table EC_VALIDATE_bk" + stamp +
		// " as select * from EC_VALIDATE");
		// query1.executeUpdate();
		// SQLQuery query2 = viewDao.createNativeQuery("create table EC_FIELD_bk" + stamp +
		// " as select * from EC_FIELD");
		// query2.executeUpdate();
	}

	private void modifyViewConfig(View view) throws Exception {
		if (view.getType().equals(ViewType.EDIT) || view.getType().equals(ViewType.VIEW)) {
			this.modifyEditView(view);
			this.saveExtraView(view.getExtraView(), null);
		} else if (view.getType().equals(ViewType.LIST) || view.getType().equals(ViewType.REFERENCE)) {
			this.modifyListView(view);
			this.saveExtraView(view.getExtraView(), null);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String modifyEditView(View view) throws Exception {
		Map configMap = (Map) SerializeUitls.deserialize(view.getExtraView().getConfig());
		List<Property> properties = modelService.findProperties(view.getAssModel());
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
								this.modifyElementPropertyCode(forms, view, properties);
							}
						}
					}
				}
			}
		}
		String conf = SerializeUitls.serializeAsXml(configMap);
		System.out.println(conf);
		view.getExtraView().setConfig(conf);
		return conf;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String modifyDatagridConf(DataGrid dataGrid) throws Exception {
		Map configMap = (Map) SerializeUitls.deserialize(dataGrid.getConfig());
		List<Property> properties = modelService.findProperties(dataGrid.getTargetModel());
		if (configMap != null && !configMap.isEmpty()) {
			List<Map> columns = (List<Map>) configMap.get("columns");
			if (columns != null && !columns.isEmpty()) {
				for (Map column : columns) {
					String key = (String) column.get("key");
					if (key.length() > 0) {
						String[] str = key.split("\\.");
						if (str.length > 0) {
							List<String> keys = new ArrayList<String>();
							keys.addAll(Arrays.asList(str));
							String propCode = "";
							if (keys.size() == 1) {
								String newName = keys.get(0);
								if (newName.endsWith("_start") || newName.endsWith("_end")) {
									newName = newName.substring(0, newName.lastIndexOf("_"));
								}
								for (Property property : properties) {
									if (property.getName() != null && property.getCode() != null) {
										if (property.getName().toLowerCase().equals(newName.toLowerCase())) {
											column.put("propertyCode", property.getCode());
											break;
										}
									}
								}
							} else {
								modifyPropertyCode(dataGrid.getTargetModel(), keys, column, key, null, propCode);
							}

						}
					}
				}
			}
		}
		String conf = SerializeUitls.serializeAsXml(configMap);
		dataGrid.setConfig(conf);
		return conf;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String modifyListView(View view) throws Exception {
		Map configMap = (Map) SerializeUitls.deserialize(view.getExtraView().getConfig());
		List<Property> properties = modelService.findProperties(view.getAssModel());
		if (configMap != null && !configMap.isEmpty()) {
			List<Map> columns = (List<Map>) configMap.get("columns");
			if (columns != null && !columns.isEmpty()) {
				for (int i = 0; i < columns.size(); i++) {
					Map column = columns.get(i);
					String key = (String) column.get("key");
					if (key.length() > 0) {
						String[] str = key.split("\\.");
						if (str.length > 0) {
							List<String> keys = new ArrayList<String>();
							keys.addAll(Arrays.asList(str));
							String propCode = "";
							if (keys.size() == 1) {
								String newName = keys.get(0);
								if (newName.endsWith("_start") || newName.endsWith("_end")) {
									newName = newName.substring(0, newName.lastIndexOf("_"));
								}
								for (Property property : properties) {
									if (property.getName() != null && property.getCode() != null) {
										if (property.getName().toLowerCase().equals(newName.toLowerCase())) {
											column.put("propertyCode", property.getCode());
											// this.modifyLabel(column1, (String)column.get("propertyCode"), key);
											break;
										}
									}
								}
							} else {
								modifyPropertyCode(view.getAssModel(), keys, column, key, null, propCode);
								// element.put("propertyCode", property_code);
							}

						}
					}
				}
			}
			List<Map> fastsections = (List<Map>) configMap.get("fastsections");
			if (fastsections != null && !fastsections.isEmpty()) {
				for (Map fastsection : fastsections) {
					Map content = (Map) fastsection.get("content");
					if (content != null && !content.isEmpty()) {
						List<Map> forms = (List<Map>) content.get("form");
						this.modifyElementPropertyCode(forms, view, properties);
					}
				}
			}
		}
		String conf = SerializeUitls.serializeAsXml(configMap);
		view.getExtraView().setConfig(conf);
		return conf;
	}

	/**
	 * 递归查找字段对应Property Code
	 *
	 * @param model
	 *            关联model
	 * @param names
	 *            字段
	 * @param element
	 *            包含propertyCode的节点
	 * @param originalName
	 *            原始名称
	 * @return Property Code
	 */
	@SuppressWarnings("rawtypes")
	private void modifyPropertyCode(Model model, List<String> names, Map<String, String> element, String originalName, Map labelElement, String codes)
			throws EcException {
		// String property_code="";
		if (model != null && names.size() > 0) {
			List<Property> properties = modelService.findProperties(model);
			String name = names.get(0);
			if (name.endsWith("_start") || name.endsWith("_end")) {
				name = name.substring(0, name.lastIndexOf("_"));
			}
			for (Property property : properties) {
				if (property.getName() != null && property.getCode() != null) {
					if (property.getName().toLowerCase().equals(name.toLowerCase())) {
						if (codes.isEmpty()) {
							codes = property.getCode();
						} else {
							codes = codes + "||" + property.getCode();
						}
						if (names.size() == 1) {
							// String property_code = property.getCode();
							element.put("propertyCode", codes);
							if (labelElement != null) {
								this.modifyLabelElementPropertyCode(labelElement, (String) element.get("propertyCode"), originalName);
							}
							break;
						} else {
							names.remove(0);
							if (property.getAssociatedProperty() != null) {
								modifyPropertyCode(property.getAssociatedProperty().getModel(), names, element, originalName, labelElement, codes);
								break;
							} else {
								throw new EcException("Model:" + model.getCode() + "||name:" + name + " COLUMN(ASSOCIATED_PROPERTY_CODE) IS NULL OR EMPTY!");
							}
						}
					}
				}
			}
		}
		// return property_code;
	}

	/**
	 * ExtraView获取property
	 * @param config
	 * @return
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Map<String, Property> getPropertyMap(String config) {
		if (config != null && config.length() > 0) {
			Map<String, Property> propertyMap = new HashMap<String, Property>();
			if (config != null && config.indexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>") == -1) {
				config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + config;
			}
			if (config != null && config.length() > 0) {
				XPath xpath = XPathFactory.newInstance().newXPath();
				xpath.setNamespaceContext(new NamespaceContext() {

					@SuppressWarnings("rawtypes")
					@Override
					public Iterator getPrefixes(String namespaceURI) {
						return null;
					}

					@Override
					public String getPrefix(String namespaceURI) {
						return null;
					}

					@Override
					public String getNamespaceURI(String prefix) {
						if ("ec".equals(prefix)) {
                            return "http://bap.supcon.com/xml/module/config";
                        } else {
                            return null;
                        }
					}
				});
				try {
					String path = "";
					if (config.contains("xmlns=\"http://bap.supcon.com/xml/module/config\"")) {
						path = "//ec:propertyCode";
					} else {
						path = "//propertyCode";
					}
					NodeList result = (NodeList) xpath.evaluate(path, new InputSource(new StringReader(config)), XPathConstants.NODESET);
					if (result != null && result.getLength() > 0) {
						for (int i = 0; i < result.getLength(); i++) {
							Node node = result.item(i);
							if (node.getFirstChild() != null && node.getFirstChild().getNodeValue().length() > 0) {
								String propertyCode = node.getFirstChild().getNodeValue();
								String[] codeStr = propertyCode.split("\\|\\|");
								if (codeStr.length > 0) {
									for (int j = 0; j < codeStr.length; j++) {
										Property property = modelService.getProperty(codeStr[j]);
										if (property != null) {
											propertyMap.put(codeStr[j], property);
										}
									}
								}
							}
						}
					}
				} catch (XPathExpressionException e) {
					log.error(e.getMessage(), e);
				}
			}
			return propertyMap;
		}
		return null;
	}

	/**
	 * 更新LABEL的propertyCode和key
	 *
	 * @param element
	 * @param propertyCode
	 * @param key
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void modifyLabelElementPropertyCode(Map element, String propertyCode, String key) {
		if (element != null && !element.isEmpty()) {
			String fieldType = (String) element.get("fieldType");
			if (fieldType != null && "LABEL".equals(fieldType.toUpperCase())) {
				element.put("propertyCode", propertyCode);
				element.put("key", key);
			}
		}
	}

	/**
	 * 修改element的propertyCode
	 *
	 * @param forms
	 * @param view
	 * @param properties
	 * @throws EcException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void modifyElementPropertyCode(List<Map> forms, View view, List<Property> properties) throws EcException {
		if (forms != null && !forms.isEmpty()) {
			for (int i = 0; i < forms.size(); i++) {
				Map form = forms.get(i);
				Map element = (Map) form.get("element");
				if (element == null || element.isEmpty()) {
                    continue;
                }
				String fieldType = (String) element.get("fieldType");
				if (fieldType != null && "LABEL".equals(fieldType.toUpperCase())) {
					continue;
				}
				if (element.get("complex") != null && "true".equals(element.get("complex").toString())) {
					continue;
				}
				if (element.get("fieldType") != null && "MULTSELECT".equals(element.get("fieldType").toString())) {
					String direcdirtasso = (element.get("directasso") != null) ? element.get("directasso").toString() : null;
					String indirectasso = (element.get("indirectasso") != null) ? element.get("indirectasso").toString() : null;
					if (direcdirtasso != null && indirectasso != null) {
						String propertyCode = direcdirtasso + "||" + indirectasso;
						element.put("propertyCode", propertyCode);
						if (i > 0) {
							Map form1 = forms.get(i - 1);
							String name = (element.get("name") != null) ? element.get("name").toString() : null;
							if (name != null) {
								this.modifyLabelElementPropertyCode((Map) form1.get("element"), propertyCode, name);
							}
						}
					}
					continue;
				}
				String name = (String) element.get("name");
				if (name.length() > 0) {
					String[] str = name.split("\\.");
					if (str.length > 0) {
						Map form1 = new HashMap();
						if (i > 0) {
							form1 = forms.get(i - 1);
						}
						List<String> names = new ArrayList<String>();
						names.addAll(Arrays.asList(str));
						if (view.getType().equals(ViewType.EDIT) || view.getType().equals(ViewType.VIEW)) {
							names.remove(0);
						}
						String propCode = ""; // 初始化propertyCode
						if (names.size() == 1) {
							String newName = names.get(0);
							if (newName.endsWith("_start") || newName.endsWith("_end")) {
								newName = newName.substring(0, newName.lastIndexOf("_"));
							}
							for (Property property : properties) {
								if (property.getName() != null && property.getCode() != null) {
									if (property.getName().toLowerCase().equals(newName.toLowerCase())) {
										if (propCode.isEmpty()) {
											element.put("propertyCode", property.getCode());
										} else {
											element.put("propertyCode", propCode + "||" + property.getCode());
										}
										this.modifyLabelElementPropertyCode((Map) form1.get("element"), (String) element.get("propertyCode"), name);
										break;
									}
								}
							}
						} else {
							modifyPropertyCode(view.getAssModel(), names, element, name, (Map) form1.get("element"), propCode);
							// element.put("propertyCode", property_code);
						}

					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		URL fileURL = null;
		if (null != fileURL) {
			Properties props = new Properties();
			InputStreamReader strInStream = null;
			try {
				strInStream = new InputStreamReader(fileURL.openStream(), "UTF-8");
				props.load(strInStream);
				totalNum = Integer.parseInt((String) props.get("backupview.totalnum"));
				beforeDateNum = Integer.parseInt((String) props.get("backupview.beforedatenum"));
			} catch (Exception e) {
				log.info("读取文件错误!", e);
			} finally {
				try {
					strInStream.close();
				} catch (Exception e) {
					log.info("关闭文件错误!", e);
				}
			}
		}
	}

	/**
	 * 对EC_EXTRA_VIEW 与EC_DATA_GRID进行数据处理 添加字段显示类型和显示格式等
	 *
	 * @throws Exception
	 */
	@Transactional
	@Override
	public void modifyExtraViewField(String moduleCode) throws Exception {

		modelService.modifyPropertyFieldType(moduleCode);

		backupViewConfig();
		List<View> views = new ArrayList<View>();
		if (moduleCode != null) {
			views = viewDao.findByCriteria(Restrictions.eq("valid", true), Restrictions.like("entity.code", moduleCode + "_", MatchMode.START));
		} else {
			views = viewDao.findByCriteria(Restrictions.eq("valid", true));
		}
		Page<DataGrid> pg = new Page<DataGrid>();
		pg.setPageSize(Integer.MAX_VALUE);
		List<DataGrid> grids = new ArrayList<DataGrid>();
		if (moduleCode != null) {
			grids = dataGridService.findDataGrids(pg, Restrictions.eq("valid", true), Restrictions.like("targetModel.code", moduleCode + "_", MatchMode.START))
					.getResult();
		} else {
			grids = dataGridService.findDataGrids(pg, Restrictions.eq("valid", true)).getResult();
		}
		if (views != null && !views.isEmpty()) {
			for (View view : views) {
				if (view.getExtraView() != null) {
					this.modifyExtraViewConfig(view, null);
					viewDao.saveExtraView(view.getExtraView());
				}
			}
		}
		if (grids != null && !grids.isEmpty()) {
			for (DataGrid grid : grids) {
				this.modifyExtraViewConfig(null, grid);
				dataGridService.save(grid);
			}
		}

	}

	/**
	 * 根据视图修改其配置文件
	 *
	 * @param view
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	public String modifyExtraViewConfig(View view, DataGrid dataGrid) throws ParserConfigurationException, SAXException, IOException, TransformerException {
		String config = "";
		if (view != null && view.getExtraView() != null) {
			if (view.getType().equals(ViewType.EDIT) || view.getType().equals(ViewType.VIEW)) {
				this.modifyEditConfig(view);
			} else if (view.getType().equals(ViewType.LIST) || view.getType().equals(ViewType.REFERENCE) || view.getType().equals(ViewType.MNECODE)) {
				this.modifyListConfig(view);
			}
			if (view.getExtraView() != null) {
				config = view.getExtraView().getConfig();
			}
		} else if (dataGrid != null) {
			this.modifyDataGridConfig(dataGrid);
			config = dataGrid.getConfig();
		}
		return config;
	}

	/**
	 * 递归生成propertyCode
	 *
	 * @param model
	 * @param names
	 * @param code
	 * @return
	 */
	@SuppressWarnings("unused")
	private String getPropertyCode(Model model, List<String> names, String code) {
		if (code == null) {
			code = "";
		}
		try {
			Property prop = null;
			if (names != null && !names.isEmpty()) {
				String name = names.get(0);
				if (name.endsWith("_start") || name.endsWith("_end")) {
					name = name.substring(0, name.lastIndexOf("_"));
				}
				Set<Property> properties = model.getProperties();
				for (Property property : properties) {
					if (property.getName().equals(name)) {
						prop = property;
						if (names.size() == 1) {
							code += property.getCode();
						} else {
							code += property.getCode() + "||";
						}
						break;
					}
				}
			}
			if(names !=null) {
                names.remove(0);
            }
			if (names != null && !names.isEmpty()) {
				if (prop != null && prop.getAssociatedProperty() != null) {
					code = getPropertyCode(prop.getAssociatedProperty().getModel(), names, code);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return code;
	}

	/**
	 * 修改编辑视图
	 *
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	private void modifyEditConfig(View view) throws ParserConfigurationException, SAXException, IOException, TransformerException {
		String config = null;
		ExtraView extraView = view.getExtraView();
		if (extraView != null && extraView.getConfig() != null && !extraView.getConfig().isEmpty()) {
			config = extraView.getConfig();
			Document doc = null;
			try {
				doc = XPathUtil.newDocument(config);
				// NodeList nodeList = XPathUtil.getNodeListByXPath(config, "//element");
				NodeList nodeList = doc.getDocumentElement().getElementsByTagName("element");
				if (nodeList != null && nodeList.getLength() > 0) {
					modifyNodeList(view, null, doc, nodeList);
				}
				String conf = XPathUtil.getXmlString(doc);
				view.getExtraView().setConfig(conf);
			} catch (Exception e) {
				log.info(config);
				log.error(e.getMessage());
			}
		}
	}

	/**
	 * 修改列表视图
	 *
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	private void modifyListConfig(View view) throws ParserConfigurationException, SAXException, IOException, TransformerException {
		String config = null;
		ExtraView extraView = view.getExtraView();
		if (extraView != null && extraView.getConfig() != null && !extraView.getConfig().isEmpty()) {
			config = extraView.getConfig();
			Document doc = XPathUtil.newDocument(config);
			// NodeList nodeList = XPathUtil.getNodeListByXPath(config, "//element");
			NodeList nodeList = doc.getDocumentElement().getElementsByTagName("element");
			if (nodeList != null && nodeList.getLength() > 0) {
				modifyNodeList(view, null, doc, nodeList);
			}

			NodeList columns = doc.getDocumentElement().getElementsByTagName("columns");
			if (columns != null && columns.getLength() > 0) {
				for (int i = 0; i < columns.getLength(); i++) {
					Node column = columns.item(i);
					NodeList lists = column.getChildNodes();
					if (lists != null && lists.getLength() > 0) {
						for (int j = 0; j < lists.getLength(); j++) {
							Node list = lists.item(j);
							if ("list".equals(list.getNodeName())) {
								modifyNodeList(view, null, doc, list.getChildNodes());
							}
						}
					}
				}
			}
			String conf = XPathUtil.getXmlString(doc);
			view.getExtraView().setConfig(conf);
		}
	}

	/**
	 * 修改DataGrid
	 *
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	private void modifyDataGridConfig(DataGrid dataGrid) throws ParserConfigurationException, SAXException, IOException, TransformerException {
		String config = null;
		if (dataGrid != null && dataGrid.getConfig() != null && !dataGrid.getConfig().isEmpty()) {
			config = dataGrid.getConfig();
			Document doc = XPathUtil.newDocument(config);
			// NodeList nodeList = XPathUtil.getNodeListByXPath(config, "//element");
			NodeList nodeList = doc.getDocumentElement().getElementsByTagName("list-item");
			if (nodeList != null && nodeList.getLength() > 0) {
				modifyNodeList(null, dataGrid, doc, nodeList);
			}
			String conf = XPathUtil.getXmlString(doc);
			dataGrid.setConfig(conf);
		}
	}

	/**
	 * 更新NodeList
	 *
	 * @param view
	 * @param doc
	 * @param nodeList
	 */
	@SuppressWarnings("unused")
	private void modifyNodeList(View view, DataGrid dataGrid, Document doc, NodeList nodeList) {
		Model model = null;
		if (view == null && dataGrid != null) {
			model = dataGrid.getTargetModel();
		} else if (view != null && dataGrid == null) {
			model = view.getAssModel();
		} else {
			return;
		}
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node element = nodeList.item(i);// element
			List<String> names = new ArrayList<String>();
			names.add("fieldType");
			names.add("type");
			names.add("format");
			names.add("propertyCode");
			names.add("key");
			names.add("name");
			names.add("complex");
			names.add("decimalNum");
			names.add("showType");
			names.add("showFormat");
			names.add("showTypeHasChanged");
			names.add("showFormatHasChanged");
			names.add("couple");
			names.add("precision");
			names.add("precisionHasChanged");
			Map<String, Node> nodeMap = XPathUtil.getNodeByName(element.getChildNodes(), names);
			Node fieldType = nodeMap.get("fieldType");
			if (fieldType == null) {
				if (nodeMap.get("format") != null) {
					fieldType = nodeMap.get("format");
				} else if (nodeMap.get("type") != null) {
					fieldType = nodeMap.get("type");
				} else if (nodeMap.get("showType") != null) {
					fieldType = nodeMap.get("showType");
				} else if (nodeMap.get("showFormat") != null) {
					fieldType = nodeMap.get("showFormat");
				}
			}
			Node propertyCode = nodeMap.get("propertyCode");
			Node name = nodeMap.get("key");
			if (name == null) {
				name = nodeMap.get("name");
			}
			/*
			 * Node showType = nodeMap.get("showType");
			 * Node showFormat = nodeMap.get("showFormat");
			 */
			Node decimalNum = nodeMap.get("decimalNum");

			boolean hasDecimal = false;
			if (decimalNum != null) {
				hasDecimal = true;
			}
			/*
			 * if (name != null && name.getFirstChild() != null && !name.getFirstChild().getNodeValue().isEmpty()) {
			 * String nameString = name.getFirstChild().getNodeValue();
			 * String[] nameStr = nameString.split("\\.");
			 * List<String> nameList = new ArrayList<String>();
			 * nameList.addAll(Arrays.asList(nameStr));
			 * if (view != null && (view.getType().equals(ViewType.EDIT) || view.getType().equals(ViewType.VIEW))) {
			 * nameList.remove(0);
			 * }
			 * if (nameList != null && nameList.size() > 1) {
			 * if (propertyCode != null && propertyCode.getFirstChild()!=null &&
			 * !propertyCode.getFirstChild().getNodeValue().isEmpty()) {
			 * String code = this.getPropertyCode(model, nameList, "");
			 * if(code!=null && code.length() > 0){
			 * propertyCode.getFirstChild().setNodeValue(code);
			 * }
			 * }
			 * } else if (nameList != null && nameList.size() == 1 && propertyCode == null) {
			 * String code = this.getPropertyCode(model, nameList, "");
			 * if (code != null && !code.isEmpty()) {
			 * propertyCode = XPathUtil.getElement(doc, "propertyCode", code);
			 * element.appendChild(propertyCode);// 如没有propertyCode
			 * }
			 * }
			 * }
			 */
			if (fieldType != null) {
				String fieldValue = fieldType.getFirstChild().getNodeValue();
				modifyFieldType(doc, element, nodeMap, propertyCode, hasDecimal, fieldValue);
				if (view != null && propertyCode != null && propertyCode.getFirstChild() != null && propertyCode.getFirstChild().getNodeValue().length() > 0) {
					element.appendChild(XPathUtil.getElement(doc, "couple", "true"));// 添加组合属性
				}
			}

		}
	}

	/**
	 * 在配置文件中添加新节点
	 *
	 * @param doc
	 * @param element
	 * @param nodeMap
	 * @param propertyCode
	 * @param hasDecimal
	 * @param fieldValue
	 */
	private void modifyFieldType(Document doc, Node element, Map<String, Node> nodeMap, Node propertyCode, boolean hasDecimal, String fieldValue) {
		Node decimalNum = nodeMap.get("decimalNum");
		Node showType = nodeMap.get("showType");
		Node showFormat = nodeMap.get("showFormat");
		Node showTypeHasChanged = nodeMap.get("showTypeHasChanged");
		Node showFormatHasChanged = nodeMap.get("showFormatHasChanged");
		Node couple = nodeMap.get("couple");
		Node precision = nodeMap.get("precision");
		Node precisionHasChanged = nodeMap.get("precisionHasChanged");
		// 如存在则先删除节点
		if (showType != null) {
			element.removeChild(showType);
		}
		if (showFormat != null) {
			element.removeChild(showFormat);
		}
		if (showTypeHasChanged != null) {
			element.removeChild(showTypeHasChanged);
		}
		if (showFormatHasChanged != null) {
			element.removeChild(showFormatHasChanged);
		}
		if (couple != null) {
			element.removeChild(couple);
		}
		if (precision != null) {
			element.removeChild(precision);
		}
		if (precisionHasChanged != null) {
			element.removeChild(precisionHasChanged);
		}
		// 开始添加新节点
		if ("LABEL".equals(fieldValue) && propertyCode != null) {
			Node complex = nodeMap.get("comlpex");
			if (complex != null) {
				complex.getFirstChild().setNodeValue("false");
			} else {
				Element el = XPathUtil.getElement(doc, "complex", "false");
				element.appendChild(el);
			}

			Element el0 = XPathUtil.getElement(doc, "showType", "LABEL");
			element.appendChild(el0);
			Element el1 = XPathUtil.getElement(doc, "showFormat", "TEXT");
			element.appendChild(el1);
			Element el2 = XPathUtil.getElement(doc, "showTypeHasChanged", "true");
			element.appendChild(el2);
			Element el3 = XPathUtil.getElement(doc, "showFormatHasChanged", "true");
			element.appendChild(el3);
		} else if ("LABEL".equals(fieldValue) && propertyCode == null) {
			Element el0 = XPathUtil.getElement(doc, "showType", "LABEL");
			element.appendChild(el0);
			Element el1 = XPathUtil.getElement(doc, "showFormat", "TEXT");
			element.appendChild(el1);
			Element el2 = XPathUtil.getElement(doc, "showTypeHasChanged", "true");
			element.appendChild(el2);
			Element el3 = XPathUtil.getElement(doc, "showFormatHasChanged", "true");
			element.appendChild(el3);
		} else if ("TEXTFIELD".equals(fieldValue) || "TEXT".equals(fieldValue) || "INTEGER".equals(fieldValue) || "LONG".equals(fieldValue)
				|| "DECIMAL".equals(fieldValue)) {
			Element el0 = XPathUtil.getElement(doc, "showType", "TEXTFIELD");
			element.appendChild(el0);
			Element el1 = XPathUtil.getElement(doc, "showFormat", "TEXT");
			element.appendChild(el1);
			Element el2 = XPathUtil.getElement(doc, "showTypeHasChanged", "true");
			element.appendChild(el2);
			Element el3 = XPathUtil.getElement(doc, "showFormatHasChanged", "true");
			element.appendChild(el3);
			if (hasDecimal) {
				Element el4 = XPathUtil.getElement(doc, "precision", decimalNum.getFirstChild().getNodeValue());
				element.appendChild(el4);
				Element el5 = XPathUtil.getElement(doc, "precisionHasChanged", "true");
				element.appendChild(el5);
			}
		} else if ("PASSWORDFIELD".equals(fieldValue)) {
			Element el0 = XPathUtil.getElement(doc, "showType", "PASSWORDFIELD");
			element.appendChild(el0);
			Element el1 = XPathUtil.getElement(doc, "showFormat", "TEXT");
			element.appendChild(el1);
			Element el2 = XPathUtil.getElement(doc, "showTypeHasChanged", "true");
			element.appendChild(el2);
			Element el3 = XPathUtil.getElement(doc, "showFormatHasChanged", "true");
			element.appendChild(el3);
		} else if ("TEXTAREA".equals(fieldValue) || "LONGTEXT".equals(fieldValue)) {
			Element el0 = XPathUtil.getElement(doc, "showType", "TEXTAREA");
			element.appendChild(el0);
			Element el1 = XPathUtil.getElement(doc, "showFormat", "TEXT");
			element.appendChild(el1);
			Element el2 = XPathUtil.getElement(doc, "showTypeHasChanged", "true");
			element.appendChild(el2);
			Element el3 = XPathUtil.getElement(doc, "showFormatHasChanged", "true");
			element.appendChild(el3);
		} else if ("SELECT".equals(fieldValue) || "ENUMERATE".equals(fieldValue)) {
			Element el0 = XPathUtil.getElement(doc, "showType", "SELECT");
			element.appendChild(el0);
			Element el1 = XPathUtil.getElement(doc, "showFormat", "SELECT");
			element.appendChild(el1);
			Element el2 = XPathUtil.getElement(doc, "showTypeHasChanged", "true");
			element.appendChild(el2);
			Element el3 = XPathUtil.getElement(doc, "showFormatHasChanged", "true");
			element.appendChild(el3);
		} else if ("DATE".equals(fieldValue)) {
			Element el0 = XPathUtil.getElement(doc, "showType", "DATE");
			element.appendChild(el0);
			Element el1 = XPathUtil.getElement(doc, "showFormat", "YMD");
			element.appendChild(el1);
			Element el2 = XPathUtil.getElement(doc, "showTypeHasChanged", "true");
			element.appendChild(el2);
			Element el3 = XPathUtil.getElement(doc, "showFormatHasChanged", "true");
			element.appendChild(el3);
		} else if ("DATETIME".equals(fieldValue)) {
			Element el0 = XPathUtil.getElement(doc, "showType", "DATETIME");
			element.appendChild(el0);
			Element el1 = XPathUtil.getElement(doc, "showFormat", "YMD_HMS");
			element.appendChild(el1);
			Element el2 = XPathUtil.getElement(doc, "showTypeHasChanged", "true");
			element.appendChild(el2);
			Element el3 = XPathUtil.getElement(doc, "showFormatHasChanged", "true");
			element.appendChild(el3);
		} else if ("RICHTEXT".equals(fieldValue)) {
			Element el0 = XPathUtil.getElement(doc, "showType", "RICHTEXT");
			element.appendChild(el0);
			Element el1 = XPathUtil.getElement(doc, "showFormat", "TEXT");
			element.appendChild(el1);
			Element el2 = XPathUtil.getElement(doc, "showTypeHasChanged", "true");
			element.appendChild(el2);
			Element el3 = XPathUtil.getElement(doc, "showFormatHasChanged", "true");
			element.appendChild(el3);
		} else if ("RADIO".equals(fieldValue)) {
			Element el0 = XPathUtil.getElement(doc, "showType", "RADIO");
			element.appendChild(el0);
			Element el1 = XPathUtil.getElement(doc, "showFormat", "RADIO");
			element.appendChild(el1);
			Element el2 = XPathUtil.getElement(doc, "showTypeHasChanged", "true");
			element.appendChild(el2);
			Element el3 = XPathUtil.getElement(doc, "showFormatHasChanged", "true");
			element.appendChild(el3);
		} else if ("CHECKBOX".equals(fieldValue) || "BOOLEAN".equals(fieldValue)) {
			Element el0 = XPathUtil.getElement(doc, "showType", "CHECKBOX");
			element.appendChild(el0);
			Element el1 = XPathUtil.getElement(doc, "showFormat", "CHECKBOX");
			element.appendChild(el1);
			Element el2 = XPathUtil.getElement(doc, "showTypeHasChanged", "true");
			element.appendChild(el2);
			Element el3 = XPathUtil.getElement(doc, "showFormatHasChanged", "true");
			element.appendChild(el3);
		} else if ("SELECTCOMP".equals(fieldValue) || "SYSTEMCODE".equals(fieldValue)) {
			Element el0 = XPathUtil.getElement(doc, "showType", "SELECTCOMP");
			element.appendChild(el0);
			Element el1 = XPathUtil.getElement(doc, "showFormat", "SELECTCOMP");
			element.appendChild(el1);
			Element el2 = XPathUtil.getElement(doc, "showTypeHasChanged", "true");
			element.appendChild(el2);
			Element el3 = XPathUtil.getElement(doc, "showFormatHasChanged", "true");
			element.appendChild(el3);
		} else if ("DATAGRID".equals(fieldValue)) {
			Element el0 = XPathUtil.getElement(doc, "showType", "DATAGRID");
			element.appendChild(el0);
			Element el1 = XPathUtil.getElement(doc, "showFormat", "");
			element.appendChild(el1);
			Element el2 = XPathUtil.getElement(doc, "showTypeHasChanged", "true");
			element.appendChild(el2);
			Element el3 = XPathUtil.getElement(doc, "showFormatHasChanged", "true");
			element.appendChild(el3);
		} else if ("MULTSELECT".equals(fieldValue)) {
			Element el0 = XPathUtil.getElement(doc, "showType", "MULTSELECT");
			element.appendChild(el0);
			Element el1 = XPathUtil.getElement(doc, "showFormat", "");
			element.appendChild(el1);
			Element el2 = XPathUtil.getElement(doc, "showTypeHasChanged", "true");
			element.appendChild(el2);
			Element el3 = XPathUtil.getElement(doc, "showFormatHasChanged", "true");
			element.appendChild(el3);
		} else if ("BUTTON".equals(fieldValue)) {
			Element el0 = XPathUtil.getElement(doc, "showType", fieldValue);
			element.appendChild(el0);
			Element el1 = XPathUtil.getElement(doc, "showFormat", "");
			element.appendChild(el1);
			Element el2 = XPathUtil.getElement(doc, "showTypeHasChanged", "true");
			element.appendChild(el2);
			Element el3 = XPathUtil.getElement(doc, "showFormatHasChanged", "true");
			element.appendChild(el3);
		} else if ("ATTACHMENT".equals(fieldValue)) {
			Element el0 = XPathUtil.getElement(doc, "showType", "ATTACHMENT");
			element.appendChild(el0);
			Element el1 = XPathUtil.getElement(doc, "showFormat", "ATTACHMENT");
			element.appendChild(el1);
		} else {
			Element el0 = XPathUtil.getElement(doc, "showType", "TEXTFIELD");
			element.appendChild(el0);
			Element el1 = XPathUtil.getElement(doc, "showFormat", "TEXT");
			element.appendChild(el1);
			Element el2 = XPathUtil.getElement(doc, "showTypeHasChanged", "true");
			element.appendChild(el2);
			Element el3 = XPathUtil.getElement(doc, "showFormatHasChanged", "true");
			element.appendChild(el3);
		}
	}

	@Transactional
	@Override
	public String getExtraViewFullConfig(View view) {
		return ecConfigService.getEcFullConfig(view);
	}


	@SuppressWarnings("rawtypes")
	@Override
	public void transformCustomerCondition(String moduleCode) {
		// TODO Auto-generated method stub
		List<View> views = viewDao.findByCriteria(Restrictions.eq("valid", true), Restrictions.like("code", moduleCode, MatchMode.START),
				Restrictions.or(Restrictions.eq("type", ViewType.LIST), Restrictions.eq("type", ViewType.REFERENCE)));
		for (View view : views) {
			ExtraView e = view.getExtraView();
			if (e != null) {
				Map configMap = e.getConfigMap();
				if (configMap != null && configMap.size() > 0) {
					Map map = (Map) configMap.get("listProperty");
					if (map != null && !map.isEmpty()) {
						if (map.get("isTransCondition") != null && (Boolean) map.get("isTransCondition")) {
							String sql = (String) map.get("conditionContent");
							CustomerCondition condition = new CustomerCondition();
							condition.setCode(view.getCode());
							condition.setView(view);
							condition.setSql(sql);
							condition.setValid(true);
							condition.setModuleCode(view.getModuleCode());
							condition.setEntityCode(view.getEntity().getCode());
							customerConditionService.saveCustomerCondition(condition);
						}
					}
				}
			}
		}
	}



	@Override
	public View getMainListView(Entity entity) {
		String hql = "from View where valid=true and type=? and usedForWorkFlow=? and entity.code=?";
		return viewDao.findEntityByHql(hql, ViewType.LIST, true, entity.getCode());
	}

	@Override
	public MenuOperate getMenuOperateByCode(String operateCode, Long cid) {
		List<MenuOperate> list = menuOperateService.findMenuOperates(Restrictions.eq("valid", true), Restrictions.eq("code", operateCode),
				Restrictions.eq("company.id", cid));
		MenuOperate menuOperate = null;
		if (null != list && !list.isEmpty()) {
			menuOperate = list.get(0);
		}
		return menuOperate;
	}

	/**
	 * 解析view中的fastquery信息，组织成fastQueryJson
	 *
	 * @param view
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })

	public static FastQueryJson getFastQueryJson(View view) {
		FastQueryJson fastQueryJson = new FastQueryJson();
		Map configMap = new HashMap();
		ExtraView extraView = view.getExtraView();
		if (null != extraView && null != extraView.getConfig() && null != extraView.getConfigMap() && !extraView.getConfigMap().isEmpty()) {
			configMap = extraView.getConfigMap();
		}
		List<String> columnTypes = Arrays.asList(new String[] { "DATE", "DATETIME", "TIME", "MONEY", "LONG", "INTEGER", "DECIMAL" });
		if (null != configMap && !configMap.isEmpty()) {
			Map layout = (Map) configMap.get("layout");
			if (null != layout && !layout.isEmpty()) {
				List<Map> sections = (List<Map>) layout.get("sections"); // layout下的section
				if (null != sections && !sections.isEmpty()) {
					for (Map section : sections) {
						if ("FASTQUERY".equals(section.get("regionType").toString())) {
							List<Map> cells = (List<Map>) section.get("cells");
							if (null != cells && !cells.isEmpty()) {
								StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><config><fastQueryJson><list>");
								for (Map cell : cells) {
									Map<String, Object> element = (Map<String, Object>) cell.get("element");// 获取element
									if (null != element && !element.isEmpty()) {
										int num = 1;
										if (columnTypes.contains(element.get("columnType").toString().toUpperCase())) {
											num = 2;
										}
										for (int i = 0; i < num; i++) {
											xml.append("<list-item>");
											xml.append("<name><![CDATA[" + element.get("name") + "]]></name>");
											xml.append("<columnType><![CDATA[" + element.get("columnType") + "]]></columnType>");
											if (num == 1) {
												xml.append("<exp><![CDATA[" + element.get("exp") + "]]></exp>");
											} else {
												if (i == 0) {
													xml.append("<exp><![CDATA[gequal]]></exp>");
												} else {
													xml.append("<exp><![CDATA[lequal]]></exp>");
												}
											}
											xml.append("<propertyCode><![CDATA[" + element.get("propertyCode") + "]]></propertyCode>");
											if (null != element.get("selfType")) {
												xml.append("<selfType><![CDATA[" + element.get("selfType") + "]]></selfType>");
											}
											xml.append("<partDepend><![CDATA[" + element.get("partDepend") + "]]></partDepend>");
											xml.append("<multable><![CDATA[" + element.get("multable") + "]]></multable>");
											xml.append("<containLower><![CDATA[" + element.get("containLower") + "]]></containLower>");
											xml.append("<caseSensitive><![CDATA[" + element.get("caseSensitive") + "]]></caseSensitive>");
											if (null != element.get("assPropertyName")) {
												xml.append("<assPropertyName><![CDATA[" + element.get("assPropertyName") + "]]></assPropertyName>");
											}
											if (null != element.get("assPropertyColumnName")) {
												xml.append("<assPropertyColumnName><![CDATA[" + element.get("assPropertyColumnName") + "]]></assPropertyColumnName>");
											}
											if (null != element.get("modelcode")) {
												xml.append("<modelCode><![CDATA[" + element.get("modelcode") + "]]></modelCode>");
											}
											xml.append("</list-item>");
										}
									}
								}
								xml.append("</list></fastQueryJson></config>");
								fastQueryJson.setQueryConfig(xml.toString());
								fastQueryJson.setCode(view.getCode());
								fastQueryJson.setView(view);
								fastQueryJson.setVersion(0);
							}
						}
					}
				}
			}
		}
		return fastQueryJson;

	}

	/**
	 * 通过viewCode和layoutName查询FQJ
	 *
	 * @param view
	 * @param layoutName
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@Transactional(readOnly=true)
	public FastQueryJson getFastQueryJsonByViewCodeAndLayoutName(View view, String layoutName) {
		return viewDao.getFastQueryJson(view.getCode(), layoutName);
	}

	/**
	 * 通过viewCode和layoutName查询aqj
	 *
	 * @param view
	 * @param layoutName
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@Transactional
	public AdvQueryJson getAdvQueryJsonByViewCodeAndLayoutName(View view, String layoutName) {
		return viewDao.getAdvQueryJson(view.getCode(), layoutName);
	}

	/**
	 * 通过view 查询所有的 FastQueryJson
	 *
	 * @param view
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@Transactional
	public Map<String, FastQueryJson> getFastQueryJsonFromView(View view) {
		Map<String, FastQueryJson> fastQueryJsonMap = new HashMap<String, FastQueryJson>();
		List<FastQueryJson> fastQueryJsons = viewDao.getFastQueryJsons(view.getCode());
		for (FastQueryJson fastQueryJson : fastQueryJsons) {
			fastQueryJsonMap.put(fastQueryJson.getCode(), fastQueryJson);
		}
		return fastQueryJsonMap;
	}

	/**
	 * 解析view中的advquery信息，组织成advQueryJson
	 *
	 * @param view
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static AdvQueryJson getAdvQueryJsonFromViewConfig(View view) {
		AdvQueryJson advQueryJson = new AdvQueryJson();
		Map configMap = new HashMap();
		ExtraView extraView = view.getExtraView();
		if (null != extraView && null != extraView.getConfig() && null != extraView.getConfigMap() && !extraView.getConfigMap().isEmpty()) {
			configMap = extraView.getConfigMap();
		}
		List<String> columnTypes = Arrays.asList(new String[] { "DATE", "DATETIME", "TIME", "MONEY", "LONG", "INTEGER", "DECIMAL" });
		if (null != configMap && !configMap.isEmpty()) {
			Map layout = (Map) configMap.get("layout");
			if (null != layout && !layout.isEmpty()) {
				List<Map> sections = (List<Map>) layout.get("sections"); // layout下的section
				if (null != sections && !sections.isEmpty()) {
					for (Map section : sections) {
						if ("ADVQUERY".equals(section.get("regionType").toString())) {
							List<Map> cells = (List<Map>) section.get("cells");
							if (null != cells && !cells.isEmpty()) {
								StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><config><advQueryJson><list>");
								for (Map cell : cells) {
									Map<String, Object> element = (Map<String, Object>) cell.get("element");// 获取element
									if (null != element && !element.isEmpty()) {
										int num = 1;
										if (columnTypes.contains(element.get("columnType").toString().toUpperCase())) {
											num = 2;
										}
										for (int i = 0; i < num; i++) {
											xml.append("<list-item>");
											xml.append("<name><![CDATA[" + element.get("name") + "]]></name>");
											xml.append("<columnType><![CDATA[" + element.get("columnType") + "]]></columnType>");
											if (num == 1) {
												xml.append("<exp><![CDATA[" + element.get("exp") + "]]></exp>");
											} else {
												if (i == 0) {
													xml.append("<exp><![CDATA[gequal]]></exp>");
												} else {
													xml.append("<exp><![CDATA[lequal]]></exp>");
												}
											}
											xml.append("<propertyCode><![CDATA[" + element.get("propertyCode") + "]]></propertyCode>");
											if (null != element.get("selfType")) {
												xml.append("<selfType><![CDATA[" + element.get("selfType") + "]]></selfType>");
											}
											xml.append("<partDepend><![CDATA[" + element.get("partDepend") + "]]></partDepend>");
											xml.append("<multable><![CDATA[" + element.get("multable") + "]]></multable>");
											xml.append("<containLower><![CDATA[" + element.get("containLower") + "]]></containLower>");
											xml.append("<caseSensitive><![CDATA[" + element.get("caseSensitive") + "]]></caseSensitive>");
											if (null != element.get("assPropertyName")) {
												xml.append("<assPropertyName><![CDATA[" + element.get("assPropertyName") + "]]></assPropertyName>");
											}
											if (null != element.get("modelcode")) {
												xml.append("<modelCode><![CDATA[" + element.get("modelcode") + "]]></modelCode>");
											}
											xml.append("</list-item>");
										}
									}
								}
								xml.append("</list></advQueryJson></config>");
								advQueryJson.setQueryConfig(xml.toString());
								advQueryJson.setCode(view.getCode());
								advQueryJson.setView(view);
								advQueryJson.setVersion(0);
							}
						}
					}
				}
			}
		}
		return advQueryJson;
	}

	/**
	 * 处理布局视图对应片段的按钮操作 将操作前加上布局code
	 */
	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	@Override
	@Transactional
	public void dealLayoutMenuOperate() {
		// 查询所有布局视图
		// String hql = "from View where valid=true and type=? and isShadow=false and (showType=? or showType=?)";
		String hql = "from View where valid=true and type=? or (type=? and showType!=?)";
		List<View> viewList = viewDao.findByHql(hql, ViewType.TREE, ViewType.LIST, ShowType.SINGLE);
		List<View> layoutViewList = new ArrayList<View>();
		Map<String, View> partViewMap = new HashMap<String, View>();
		for (View view : viewList) {
			if (view.getShowType().equals(ShowType.LAYOUT) || view.getShowType().equals(ShowType.LAYOUT2)) {
				if (view.getIsShadow()) {
					continue;
				}
				layoutViewList.add(view);
			} else {
				partViewMap.put(view.getCode(), view);
			}
		}
		if (null != layoutViewList && !layoutViewList.isEmpty()) {
			List<Long> dealOpIdList = new LinkedList<Long>();
			for (View view : layoutViewList) {
				String config = ecConfigService.getEcFullConfig(view);
				Map<String, Object> configMap = (Map<String, Object>) SerializeUitls.deserialize(config);
				if (null != configMap && !configMap.isEmpty()) {
					Map<String, Map> layout = (Map<String, Map>) configMap.get("layout");
					if (null != layout && !layout.isEmpty()) {
						for (Entry<String, Map> part : layout.entrySet()) {
							Map<String, String> partElements = part.getValue();
							if(partElements != null){
							if ("tree".equals(partElements.get("ctype"))) {
								String viewCode = partElements.get("treeView");
								if (viewCode != null) {
									View partView = partViewMap.get(viewCode);
									if (null == partView) {
										partView = viewDao.findEntityByHql("from View where valid=true and code=?", viewCode);
									}
									List<Button> buttons = buttonService.getButtons(partView.getCode());
									if (null != buttons && !buttons.isEmpty()) {
										for (Button button : buttons) {
											String iconCls = button.getButtonStyle();
											String buttonCode = null;
											if(button.getButtonOperationCode()!=null){
												buttonCode=button.getButtonOperationCode();
											}else{
												if (null != iconCls) {
													buttonCode = partView.getName() + "_" + button.getName() + "_" + iconCls + "_" + partView.getCode();
												} else {
													buttonCode = partView.getName() + "_" + button.getName() + "_" + partView.getCode();
												}
												button.setButtonOperationCode(buttonCode);
												buttonDao.save(button);
												buttonDao.flush();
											}
											List<MenuOperate> menuOperates = menuOperateService.findMenuOperates(Restrictions.eq("valid", true),
													Restrictions.eq("code", buttonCode));
											if (null != menuOperates && !menuOperates.isEmpty()) {
												for (MenuOperate menuOperate : menuOperates) {
													if (dealOpIdList.contains(menuOperate.getId())) {
														continue;
													}
													menuOperate.setCode(view.getCode() + "_" + buttonCode);
													menuOperateService.save(menuOperate);
													dealOpIdList.add(menuOperate.getId());
												}
											}
										}
									}
								}
							}
							}
							if (null != partElements) {
								for (Entry<String, String> partElement : partElements.entrySet()) {
									if ("vcode".equals(partElement.getKey())) {
										if (null != partElement.getValue()) {
											View partView = partViewMap.get(partElement.getValue());
											if (null == partView) {
												partView = viewDao.findEntityByHql("from View where valid=true and code=?", partElement.getValue());
											}
											String name = partView.getName();
											String viewCode = partView.getCode();
											boolean isShadowView = partView.getIsShadow();
											if (isShadowView && null != partView.getShadowView()) {
												partView = partView.getShadowView();
											}
											List<Button> buttons = buttonService.getButtons(partView.getCode());
											if (null != buttons && !buttons.isEmpty()) {
												for (Button button : buttons) {
													String iconCls = button.getButtonStyle();
													String buttonCode = null;
													if(button.getButtonOperationCode()!=null){
														buttonCode=button.getButtonOperationCode();
													}else{
														if (null != iconCls) {
															buttonCode = partView.getName() + "_" + button.getName() + "_" + iconCls + "_" + partView.getCode();
														} else {
															buttonCode = partView.getName() + "_" + button.getName() + "_" + partView.getCode();
														}
														button.setButtonOperationCode(buttonCode);
														buttonDao.save(button);
														buttonDao.flush();
													}
													if (isShadowView) {
														buttonCode = buttonCode.substring(buttonCode.indexOf("_"), buttonCode.lastIndexOf("_") + 1);
														buttonCode = name + "_" + buttonCode + "_" + name;
													}
													List<MenuOperate> menuOperates = menuOperateService.findMenuOperates(Restrictions.eq("valid", true),
															Restrictions.eq("code", buttonCode));
													if (null != menuOperates && !menuOperates.isEmpty()) {
														for (MenuOperate menuOperate : menuOperates) {
															if (dealOpIdList.contains(menuOperate.getId())) {
																continue;
															}
															menuOperate.setCode(view.getCode() + "_" + buttonCode);
															menuOperateService.save(menuOperate);
															dealOpIdList.add(menuOperate.getId());
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
			}
		}
	}

	public static String firstLatterToLowerCase(String key) {
		char fl = ((String) key).charAt(0);
		return Character.toLowerCase(fl) + ((String) key).substring(1);
	}

	public static String firstLatterToUpperCase(String key) {
		char fl = ((String) key).charAt(0);
		return Character.toUpperCase(fl) + ((String) key).substring(1);
	}

	private static String resolveMessageKey(String messageKey) {
		String[] values = messageKey.split("\\$&#");
		String newKey = values[0];
		if (newKey.startsWith("key=")) {
			newKey = newKey.substring(4);
		}
		return newKey;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void modifyShadowViewCustomSection(String viewCode, Boolean hasCustomSection) {
		viewDao.createNativeQuery("update EC_VIEW set HAS_CUSTOM_SECTION = ? WHERE IS_SHADOW = 1 and SHADOW_VIEW_CODE = ? and VALID = 1",
				new Object[] {ObjectUtils.isEmpty(hasCustomSection) ? 0 : hasCustomSection, viewCode }).executeUpdate();
	}


	@Override
	@Transactional
	public void migrateView(View srcView, View view, Map<Property, Property> hashProperty, boolean needCopyExtraView, Map<String, String> viewCodeReplaceMap,
                            Map<String, String> dgCodeReplaceMap) {

		if (null == view.getEntity().getModule()) {
			Entity entity = entityService.getEntity(view.getEntity().getCode());
			view.setEntity(entity);
		}

		View targetView = new View();
		srcView = getView(srcView.getCode(), true);
		viewDao.evict(srcView);
		String targetConfig = null;
		String config = null;
		if (needCopyExtraView && srcView.getExtraView() != null) {
			config = this.getExtraViewFullConfig(srcView);
			log.debug("替换前配置：{}", config);
			targetConfig = replaceFieldCode(config, srcView, view.getEntity().getCode() + "_" + view.getName(), view.getType());
			targetConfig = regenerateTabCode(targetConfig);
			targetConfig = regenerateLayoutCode(targetConfig);
			targetConfig = regenerateSectionCode(targetConfig);
			targetConfig = regenerateCellCode(targetConfig);
			if (ViewType.LIST.equals(srcView.getType()) || ViewType.REFERENCE.equals(srcView.getType())) {
				targetConfig = dealViewConfig(targetConfig, srcView.getCode() + "_LISTPT_ASSO_");
				targetConfig = dealViewConfig(targetConfig, srcView.getCode() + "_LISTPT_CUSTOM_");
			}
			targetConfig = rebuildViewConfig(targetConfig, hashProperty);// 替换共同复制的字段
			if (null != viewCodeReplaceMap && !viewCodeReplaceMap.isEmpty()) {
				for (Entry<String, String> entry : viewCodeReplaceMap.entrySet()) {
					targetConfig = targetConfig.replace("[" + entry.getKey() + "]", "[" + entry.getValue() + "]");
				}
			}
			log.debug("替换后配置：{}", targetConfig);
		}
		BeanUtils.copyProperties(srcView, targetView, new String[] { "entity", "moduleCode", "assModel", "fastQueryJson" });
		targetView.setEntity(view.getEntity() == null ? srcView.getEntity() : view.getEntity());
		targetView.setFastQueryJson(view.getFastQueryJson());
		targetView.setCode(null);
		targetView.setVersion(0);
		targetView.setName(view.getName());
		targetView.setTitle(view.getTitle());
		targetView.setDisplayName(view.getDisplayName());
		targetView.setAssModel(view.getAssModel());
		if (targetView.getDisplayName().equals(targetView.getTitle())) {
			String displayName = internationalService.createNewInternational(targetView.getTitle());
			targetView.setDisplayName(displayName);
		} else {
			// messageKey一样 值不同
			String title = resolveMessageKey(view.getTitle());
			String displayName = resolveMessageKey(view.getDisplayName());
			if (title.equals(displayName)) {
				displayName = internationalService.createNewInternational(view.getDisplayName());
				view.setDisplayName(displayName);
			}

		}
		targetView.setType(view.getType());
		// 复制视图时 参照权限、批量控件打印、控件打印等操作为空
		// 批量打印
		targetView.setIsBatchControlPrint(false);
		targetView.setBatchControlPrintSelectView(null);
		// 参照权限
		targetView.setIsPermission(false);
		targetView.setPermissionCode(null);
		targetView.setOperateUrl(null);
		targetView.setRefOperateName(null);
		// 控件打印
		targetView.setControlPrint(false);
		targetView.setControlName(null);
		targetView.setControlCode(null);
		targetView.setControlSetingName(null);

		ExtraView ev = null;
		if (srcView.getExtraView() != null) {
			ev = new ExtraView();
			ev.setView(targetView);
			ev.setConfig(targetConfig);
		}

		view = targetView;
		if (view.getCode() == null || view.getCode().length() == 0) {
			view.setCode(view.getEntity().getCode() + "_" + view.getName());
			if (ev != null) {
				ev.setCode(view.getCode());
			}
		}

		if (null == view.getModuleCode() || "".equals(view.getModuleCode())) {
			view.setModuleCode(view.getEntity().getModule().getCode());
		}
		if (null != view.getAssModel() && null == view.getAssModel().getModelName()) {
			Model assModel = modelService.getModel(view.getAssModel().getCode());
			view.setAssModel(assModel);
		}
		if (!view.getCustomFlag() && view.getEntity() != null && view.getEntity().getModule() != null && view.getAssModel() != null && view.getName() != null
				&& view.getEntity().getEntityName() != null && view.getEntity().getModule().getArtifact() != null && view.getAssModel().getModelName() != null) {
			if(null!= ProjectFlagHolder.getInstance().getProjFlag().get() && ProjectFlagHolder.getInstance().getProjFlag().get()){
				view.setUrl(String.format("/%s/%s/%s/%s/%s/%s", MS_SERVICE, view.getEntity().getModule().getArtifact(), view.getEntity().getEntityName(),
						View.fl(view.getAssModel().getModelName()),PROJ_FLAG, view.getName()));
			}else {
				view.setUrl(String.format("/%s/%s/%s/%s/%s", MS_SERVICE, view.getEntity().getModule().getArtifact(), view.getEntity().getEntityName(),
						View.fl(view.getAssModel().getModelName()), view.getName()));
			}
		}
		if (needCopyExtraView && srcView.getExtraView() != null) {
			view.setExtraView(ev);
		}
		View existView = getView(view.getCode());
		ExtraView existExtraView = null;
		if (existView != null) {
			existExtraView = getExtraView(existView);
			BeanUtils.copyProperties(view, existView);
			viewDao.merge(existView);
			view = existView;
		} else {
			viewDao.save(view);
		}

		viewDao.flush();
		viewDao.clear();

		List<DataGrid> dgs = null;
		List<DataGrid> targetDgs = new ArrayList<>();
		if (needCopyExtraView && view.getExtraView() != null) {
			if (view.getType() == ViewType.EDIT || view.getType() == ViewType.VIEW) {
				dgs = dataGridService.getDataGridByView(srcView, false);
				if (dgs != null && !dgs.isEmpty()) {
					for (DataGrid dg : dgs) {
						targetDgs.add(copyDatagrid(dg, targetView, viewCodeReplaceMap, dgCodeReplaceMap));
					}
				}
			}
		}

		if (needCopyExtraView && srcView.getExtraView() != null) {
			if (view.getShowType() != ShowType.LAYOUT) {
				if (ev != null && ev.getConfig() != null && ev.getConfig().length() > 0) {
					String viewConfig = ev.getConfig();
					if (viewConfig != null && viewConfig.length() > 0) {
						Map<String, Object> configMap = new EcExtraViewIntegrationUtils().ecSplitConfig(viewConfig);
						if (configMap.get("config") != null) {
							config = configMap.get("config").toString();
							if (existExtraView != null) {
								jdbcTemplate.update("update ec_extra_view set config = ? where code = ?", config, existExtraView.getCode());
							} else {
								ev.setConfig(config);
								saveExtraView(ev, null);
							}

						}
						if (configMap.get("fieldConfig") != null) {
							String fieldConfig = configMap.get("fieldConfig").toString();
							if (view.getType() != ViewType.MNECODE) {
								buttonService.saveButton(view, fieldConfig, null);
							}
							fieldService.saveFields(view, fieldConfig, null, null, null);
							if (view.getType() != ViewType.MNECODE) {
								eventService.saveEvent(view, fieldConfig);
							}
						}
					}
					if (targetDgs != null && !targetDgs.isEmpty()) {
						viewDao.flush();
						viewDao.clear();
						for (int i = 0; i < targetDgs.size(); i++) {
							DataGrid dg = targetDgs.get(i);
							String dgConfig = dg.getConfig();
							if (dgConfig != null && dgConfig.length() > 0) {
								Map<String, Object> dgConfigMap = new EcExtraViewIntegrationUtils().ecSplitConfig(dgConfig);
								if (dgConfigMap.get("config") != null) {
									dg.setConfig(dgConfigMap.get("config").toString());
									dataGridDao.update(dg);
								}

								if (dgConfigMap.get("fieldConfig") != null) {
									String fieldConfig = dgConfigMap.get("fieldConfig").toString();
									fieldService.saveFields(dg, fieldConfig, null, null, null);
									buttonService.saveButton(dg, fieldConfig, null);
								}
								// 复制datagrid中的自定义条件
								List<CustomerCondition> ccs = customerConditionService.findCustomerConditions(
										Restrictions.like("code", dgs.get(i).getCode(), MatchMode.START), Restrictions.eq("valid", true));
								viewDao.flush();
								viewDao.clear();
								if (ccs != null && !ccs.isEmpty()) {
									for (CustomerCondition cc : ccs) {
										cc.setCode(cc.getCode().replace(dgs.get(i).getCode(), dg.getCode()));
										cc.setView(view);
										cc.setDataGrid(dg);
										customerConditionService.saveCustomerCondition(cc);
									}
								}
							}
						}
					}
				}
				if (view.getType() == ViewType.LIST || view.getType() == ViewType.REFERENCE) {
					Iterator<FastQueryJson> iterator = srcView.getFastQueryJson().iterator();
					FastQueryJson fqj = iterator.hasNext() ? iterator.next() : null;
					if (fqj != null) {
						String fqjConfig = rebuildConfig(fqj.getQueryConfig(), hashProperty);// 替换复制后字段
						FastQueryJson tarFqj = null;
						if (view.getFastQueryJson() != null) {
							tarFqj = view.getFastQueryJson().iterator().next();
						}
						if (tarFqj != null) {
							tarFqj = fastQueryJsonDao.load(tarFqj.getCode());
							BeanUtils.copyProperties(fqj, tarFqj, new String[] { "code", "view", "version" });
							tarFqj.setQueryConfig(fqjConfig);
							fastQueryJsonDao.merge(tarFqj);
						} else {
							tarFqj = new FastQueryJson();
							BeanUtils.copyProperties(fqj, tarFqj, new String[] { "code", "view" });
							tarFqj.setCode(view.getCode());
							tarFqj.setView(view);
							tarFqj.setQueryConfig(fqjConfig);
							fastQueryJsonDao.save(tarFqj);
						}

					}
				}
			} else {
				saveExtraView(ev, null);
			}
		}
		if (needCopyExtraView) {
			// 复制视图中的自定义条件
			List<CustomerCondition> ccs = customerConditionService.findCustomerConditions(Restrictions.like("code", srcView.getCode(), MatchMode.START),
					Restrictions.eq("valid", true));
			// List<CustomerCondition> ccs = customerConditionService.findCustomerConditionsByCode(srcView.getCode());
			viewDao.flush();
			viewDao.clear();
			if (ccs != null && !ccs.isEmpty()) {
				for (CustomerCondition cc : ccs) {
					CustomerCondition c = new CustomerCondition();
					BeanUtils.copyProperties(cc, c);
					c.setCode(cc.getCode().replace(srcView.getCode(), view.getCode()));
					c.setView(view);
					c.setModuleCode(view.getModuleCode());
					c.setEntityCode(view.getEntity().getCode());
					customerConditionService.saveCustomerCondition(c);
				}
			}
		}
		List<DataGroup> srcDataGroups = findDataGroups(srcView);
		if (srcDataGroups != null && srcDataGroups.size() > 0) {
			for (DataGroup dgroup : srcDataGroups) {
				DataGroup newdgroup = new DataGroup();
				newdgroup.setName(dgroup.getName());
				newdgroup.setCode(view.getCode() + "_" + newdgroup.getName());
				newdgroup.setDisplayName(dgroup.getDisplayName());
				newdgroup.setIsMultiple(dgroup.getIsMultiple());
				newdgroup.setView(view);
				saveDataGroup(newdgroup);
				List<DataClassific> srcDataClassifics = findDataClassifics(dgroup);
				if (srcDataClassifics != null && srcDataClassifics.size() > 0) {
					for (DataClassific dclassific : srcDataClassifics) {
						DataClassific newdclassific = new DataClassific();
						newdclassific.setName(dclassific.getName());
						newdclassific.setCode(newdgroup.getCode() + "_" + newdclassific.getName());
						newdclassific.setDisplayName(dclassific.getDisplayName());
						newdclassific.setCondition(dclassific.getCondition());
						newdclassific.setDataGroup(newdgroup);
						saveDataClassific(newdclassific);
						CustomerCondition cc = customerConditionService.getCustomerCondition(dclassific);
						CustomerCondition newcc = new CustomerCondition();
						BeanUtils.copyProperties(cc, newcc);
						newcc.setView(view);
						newcc.setDataClassific(newdclassific);
						newcc.setCode(view.getCode() + "_" + newdclassific.getCode());
						customerConditionService.saveCustomerCondition(newcc);

					}
				}
			}
		}

	}

	private String rebuildViewConfig(String config, Map<Property, Property> hashProperty) {
		Iterator<Entry<Property, Property>> propertyIterator = hashProperty.entrySet().iterator();
		while (propertyIterator.hasNext()) {
			Entry<Property, Property> item = propertyIterator.next();
			Property src = item.getKey();
			Property tar = item.getValue();
			// property code
			String pcodeSrc = src.getCode();
			String pcodeTar = tar.getCode();
			// model code
			String modelcodeSrc = src.getModel().getCode();
			String modelcodeTar = tar.getModel().getCode();
			// entity code
			String ecodeSrc = src.getEntityCode();
			String ecodeTar = tar.getEntityCode();
			// module code
			String mcodeSrc = src.getModuleCode();
			String mcodeTar = tar.getModuleCode();
			config = config.replace(pcodeSrc + "]", pcodeTar + "]");
			config = config.replace(pcodeSrc + "||", pcodeTar + "||");
			config = config.replace(modelcodeSrc + "]", modelcodeTar + "]");
			config = config.replace(ecodeSrc + "]", ecodeTar + "]");
			config = config.replace(mcodeSrc + "]", mcodeTar + "]");
			config = config.replace("\"" + pcodeSrc + "\"", "\"" + pcodeTar + "\"");
			config = config.replace("\"" + modelcodeSrc + "\"", "\"" + modelcodeTar + "\"");
		}
		return config;
	}

	private String rebuildConfig(String config, Map<Property, Property> hashProperty) {

		Map fqMap = (Map) SerializeUitls.deserialize(config);
		if (fqMap != null) {
			List<Map> list = (List) fqMap.get("fastQueryJson");
			if (null != list && !list.isEmpty()) {
				for (Map m : list) {
					String propertyCode = (String) m.get("propertyCode");
					String assP = null;
					if (propertyCode.contains("||")) {
						String[] p = propertyCode.split("\\|\\|");
						propertyCode = p[0];
						assP = p[1];
					} else {
						assP = propertyCode;
					}
					Property src = new Property();
					src.setCode(propertyCode);
					Property tar = hashProperty.get(src);
					if (tar != null) {
						if (assP.equals(propertyCode)) {
							m.put("propertyCode", tar.getCode());
							m.put("modelCode", tar.getModel().getCode());
						} else {
							src.setCode(assP);
							Property asp = hashProperty.get(src);
							if (asp != null) {
								m.put("propertyCode", tar.getCode() + "||" + asp.getCode());
								m.put("modelCode", asp.getModel().getCode());
							} else {
								m.put("propertyCode", tar.getCode() + "||" + assP);
							}
						}

					}
				}
			}
		}
		return SerializeUitls.serializeAsXml(fqMap);
	}

	@Override
	@Transactional
	public void mergeView(View view) {
		viewDao.merge(view);
	}

	@Override
	public View load(String id) {
		return viewDao.load(id);
	}

	private void saveActionViewMapping(View view) {
		String env = "ec";
		if (null != ProjectFlagHolder.getInstance().getProjFlag().get() && ProjectFlagHolder.getInstance().getProjFlag().get()) {
			env = "proj";
		}
		actionViewService.refreshSingleViewAction(view, env);
		// String prefix = view.getUrl().replaceAll("\\.action$", "");
		// actionViewService.deleteActionViewByViewcode(view.getCode());
		// if (view.getType() != ViewType.MNECODE) {
		// actionViewService.saveActionView(new ActionView(prefix + ".action", view.getCode(),view.getName()));
		// }
		// if (view.getIsPrint()) {
		// actionViewService.saveActionView(new ActionView(prefix + "Print.action", view.getCode(),view.getName()));
		// }
		// if (view.getShowType() != ShowType.LAYOUT && view.getShowType() != ShowType.LAYOUT2) {
		// if (view.getType() == ViewType.TREE || view.getType() == ViewType.REFTREE) {
		// actionViewService.saveActionView(new ActionView(prefix + "Drag.action", view.getCode(),view.getName()));
		// actionViewService.saveActionView(new ActionView(prefix + "Sort.action", view.getCode(),view.getName()));
		// actionViewService.saveActionView(new ActionView(prefix + "Data.action", view.getCode(),view.getName()));
		// actionViewService.saveActionView(new ActionView(prefix + "FullData.action", view.getCode(),view.getName()));
		// } else if (view.getType() == ViewType.LIST) {
		// if (view.getAssModel().getIsMain() && view.getAssModel().getEntity().getWorkflowEnabled()) {
		// actionViewService.saveActionView(new ActionView(prefix + "-pending.action", view.getCode(),view.getName()));
		// }
		// actionViewService.saveActionView(new ActionView(prefix + "-query.action", view.getCode(),view.getName()));
		// actionViewService.saveActionView(new ActionView(prefix + "-getRequireData.action",
		// view.getCode(),view.getName()));
		// } else if (view.getType() == ViewType.REFERENCE) {
		// actionViewService.saveActionView(new ActionView(prefix + "-query.action", view.getCode(),view.getName()));
		// } else if (view.getType() == ViewType.MNECODE) {
		// actionViewService.saveActionView(new ActionView(prefix.replaceAll("[^/]+$", "") + "mneClient.action",
		// view.getCode(),view.getName()));
		// }
		// }
	}

	private String buildMneCodeSql(View view) {
		StringBuilder builder = new StringBuilder();
		StringBuilder join = new StringBuilder();
		builder.append("SELECT DISTINCT ");
		// 默认输出的字段
		String modelAlias = "\"" + firstLatterToLowerCase(view.getAssModel().getModelName()) + "\"";
		Property pkProperty = modelService.findPKProperty(view.getAssModel().getCode());
		// builder.append(modelAlias).append(".").append(Inflector.getInstance().columnize("id")).append(" AS \"").append("id\"");
		builder.append(modelAlias).append(".").append(getColumnName("id", view.getAssModel().getCode())).append(" AS \"").append("id\"");
		join.append(joinSQL(view, builder, modelAlias));
		builder.append(",CASE WHEN ").append(modelAlias).append(".CID=? THEN 1 ELSE 0 END \"ISCURRENTCOMPANY\"");
		builder.append(" FROM ")
				// .append(Inflector.getInstance().tableize(view.getEntity().getModule().getArtifact(),
				// view.getAssModel().getModelName()))
				.append(view.getAssModel().getTableName()).append(" ").append(modelAlias).append(" INNER JOIN ")
				.append(DbUtils.getMneTable(view.getAssModel().getTableName())).append(" \"")
				.append(firstLatterToLowerCase(view.getAssModel().getModelName())).append(".mc\"").append(" ON ").append(modelAlias).append(".")
				.append(pkProperty.getColumnName()).append(" = ").append("\"").append(firstLatterToLowerCase(view.getAssModel().getModelName()))
				.append(".mc\".").append(com.supcon.supfusion.configuration.services.utils.Inflector.getInstance().columnize(firstLatterToLowerCase(view.getAssModel().getModelName())));
		builder.append(join);
		return builder.toString();
	}

	@SuppressWarnings("unchecked")
	private String getColumnName(String name, String modelCode) {
		Set<Property> properties = modelService.getModel(modelCode).getProperties();
		for (Property p : properties) {
			if (p.getName().equals(name)) {

				return p.getColumnName();
			}
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String joinSQL(View view, StringBuilder builder, String modelAlias) {
		StringBuilder join = new StringBuilder();
		if (null == view.getExtraView()) {
			return null;
		}
		Map map = view.getExtraView().getConfigMap();
		List<String> currentEntityModelsName = new ArrayList<String>();
		List<Model> models = modelService.findModels(view.getEntity());
		for (Model m : models) {
			currentEntityModelsName.add(m.getTableName());
		}
		if (null != map) {
			Map layoutMap = (Map) map.get("layout");
			if (layoutMap != null && !layoutMap.isEmpty()) {
				List<Map> sectionList = (List<Map>) layoutMap.get("sections");
				if (sectionList != null && !sectionList.isEmpty()) {
					for (Map section : sectionList) {
						if (section.get("regionType") != null
								&& ("LISTPT".equals(section.get("regionType").toString()) || "MNECODE".equals(section.get("regionType").toString()))) {
							List<Map> cells = (List<Map>) section.get("cells");
							if (null != cells && !cells.isEmpty()) {
								Set<String> tmpAssSet = new LinkedHashSet<String>();// 防止重复
								for (Map column : cells) {
									if (null != column.get("key") && column.get("key").toString().length() > 0
											&& !"bapAttachmentInfo".equals(column.get("key")) && null != column.get("showType")
											&& !"ATTACHMENT".equals(column.get("showType")) && !"PROPERTYATTACHMENT".equals(column.get("showType"))
											&& (column.get("assoFlag") == null || !"true".equalsIgnoreCase(column.get("assoFlag").toString()))) {
										String key = (String) column.get("key");
										int lastDotPos = key.lastIndexOf('.');
										String propertyCode = (String) column.get("propertyCode");
										if (null != propertyCode && propertyCode.indexOf("||") > -1) {
											propertyCode = propertyCode.substring(propertyCode.lastIndexOf("||") + 2);
										}

										// FIXME 关联模型处理id问题，目前列表字段拖关联的字段，则会记下该关联模型id，但是存储的还是原来拖出来字段的propertyCode
										if ("id".equalsIgnoreCase(key.substring(lastDotPos + 1))) {
											String modelCode = null;
											if (null != column.get("modelCode") && column.get("modelCode").toString().length() > 0) {
												modelCode = column.get("modelCode").toString();
											}
											if (null != modelCode && !modelCode.startsWith("sysbase_1.0")) {
												propertyCode = modelCode + "_id";
											} else {
												propertyCode = propertyCode.substring(0, propertyCode.lastIndexOf("_") + 1) + "id";
											}
										}

										Property property = modelService.getProperty(propertyCode);
										String columnName = property == null ? Inflector.getInstance().columnize(key.substring(lastDotPos + 1)) : property
												.getColumnName();
										String tableAlias = lastDotPos < 0 ? modelAlias : "\"" + key.substring(0, lastDotPos) + "\"";
										String colAlias = "\"" + key + "\"";
										if (key.indexOf("status.") == -1) {
											builder.append(",").append(tableAlias).append(".").append(columnName).append(" AS ").append(colAlias);
										}
										if (lastDotPos >= 0) {
											String assKey = key.substring(0, lastDotPos);
											/* 处理关联 */
											propertyCode = (String) column.get("propertyCode");
											if (null != propertyCode && propertyCode.indexOf("||") > -1) {
												String[] assKeyTuple = assKey.split("\\.");
												String layRec = (String) column.get("layRec");
												if (null != layRec && layRec.length() > 0 && layRec.indexOf("-") > -1) {
													int aPos = layRec.lastIndexOf("-");
													String assRec = layRec.substring(0, aPos);
													String[] assRecTuple = assRec.split("-");
													String keyAlias = "";
													String parentAlias = "";
													for (int j = 0; j < assKeyTuple.length; j++) {
														StringBuilder joinPart = new StringBuilder();
														parentAlias = j == 0 ? modelAlias : "\"" + keyAlias + "\"";
														keyAlias += j == 0 ? assKeyTuple[j] : "." + assKeyTuple[j];
														if (!tmpAssSet.contains(keyAlias)) {
															String[] assTNames = assRecTuple[j].split(",");
															joinPart.append(" LEFT OUTER JOIN ");
															joinPart.append(assTNames[0]).append(" \"");
															if (assTNames[0].equalsIgnoreCase("base_status")) {
																keyAlias = "flowStatus";
															}
															joinPart.append(keyAlias).append("\"");
															joinPart.append(" ON \"");
															joinPart.append(keyAlias).append("\".").append(assTNames[1]);
															joinPart.append(" = ");
															String parentAssTName = assTNames[3];
															if ("BASE_COMPANY".equals(assTNames[0])
																	&& ("BASE_DEPARTMENT".equals(assTNames[2]) || "BASE_POSITION".equals(assTNames[2]))) {
																parentAssTName = "CID";
															}
															joinPart.append(parentAlias).append(".").append(parentAssTName);
															if (null != currentEntityModelsName && !currentEntityModelsName.isEmpty()) {
																if (currentEntityModelsName.contains(assTNames[0])) {
																	joinPart.append(" AND \"");
																	joinPart.append(keyAlias).append("\".VALID = 1");
																}
															}
															tmpAssSet.add(keyAlias);
															join.append(joinPart);
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
			}
		}
		if (view.getType() == ViewType.LIST && view.getAssModel().getIsMain() && view.getEntity().getPayCloseAttention() != null
				&& view.getEntity().getPayCloseAttention()) {
            join.append(" LEFT OUTER JOIN ").append(DbUtils.getPayAttentionTable(view.getAssModel().getTableName()))
                    .append(" \"a\" on \"a\".VALID = 1 and \"a\".")
                    // .append(Inflector.getInstance().columnize("tableInfoId")).append(" = ").append(modelAlias).append(".")
                    .append(getColumnName("tableInfoId", view.getAssModel().getCode())).append(" = ").append(modelAlias).append(".")
                    // .append(Inflector.getInstance().columnize("tableInfoId")).append(" and  \"a\".STAFF = ? ");
                    .append(getColumnName("tableInfoId", view.getAssModel().getCode())).append(" and  \"a\".STAFF = ? ");
        }
		return join.toString();
	}

	@Override
	@Transactional
	public void rebuildQueryResult(View view, List<Object> newActions) {
//		String modelName = view.getAssModel().getModelName();
//		String entityName = view.getAssModel().getEntity().getEntityName();
//		String moduleArtifact = view.getAssModel().getEntity().getModule().getArtifact();
//		String newField = generateResultField(view);
//		if (null == dispatcher) {
//			dispatcher = Dispatcher.getInstance();
//		}
//		Configuration configurattion = dispatcher.getConfigurationManager().getConfiguration();
//		RuntimeConfiguration rcfg = configurattion.getRuntimeConfiguration();
//		String nameSpace = "/" + moduleArtifact + "/" + entityName + "/" + StringUtils.firstLetterToLower(modelName);
//		String actionName = view.getName() + "-query";
//		ActionConfig aco = rcfg.getActionConfig(nameSpace, actionName);
//		if (aco != null) {
//			doRebuildResult(view, rcfg, nameSpace, actionName, newField);
//			if (view.getType() == ViewType.LIST && view.getAssModel().getIsMain() && view.getAssModel().getEntity().getWorkflowEnabled()) {
//				actionName = view.getName() + "-pending";
//				doRebuildResult(view, rcfg, nameSpace, actionName, newField);
//			}
//		} else {
//			String modelPrefix = null;
//			if (view.getAssModel().getEcVersion() != null && view.getAssModel().getEcVersion().equals("1.0")) {
//				modelPrefix = view.getAssModel().getModelName();
//			} else {
//				modelPrefix = view.getAssModel().getJpaName();
//			}
//			String className = "com.supcon.orchid." + moduleArtifact + ".actions." + modelPrefix + "Action";
//			String packageName = "com.supcon.orchid." + moduleArtifact + ".actions#orchid-" + moduleArtifact + "#" + nameSpace;
//			PackageConfig pkc = configurattion.getPackageConfig(packageName);
//			ObjectFactory objectFactory = dispatcher.getContainer().getInstance(ObjectFactory.class);
//			if (pkc != null) {
//				generateQueryPendingActionConfig(view, "query", pkc, nameSpace, rcfg, className, objectFactory, newField, newActions);
//				if (view.getType() == ViewType.LIST && view.getAssModel().getIsMain() && view.getAssModel().getEntity().getWorkflowEnabled()) {
//					generateQueryPendingActionConfig(view, "pending", pkc, nameSpace, rcfg, className, objectFactory, newField, newActions);
//				}
//			}
//		}
	}

	@Override
	@Transactional
	public void rebuildTreeDataResult(View view, List<Object> newActions) {
//		ExtraView ev = view.getExtraView();
//		String modelName = view.getAssModel().getModelName();
//		String entityName = view.getAssModel().getEntity().getEntityName();
//		String moduleArtifact = view.getAssModel().getEntity().getModule().getArtifact();
//		Map layoutMap = (Map) ev.getConfigMap().get("layout");
//		Map pageConfigMap = (Map) layoutMap.get("pageConfig");
//		String treeModelCode = pageConfigMap.get("treeModelCode").toString();
//		String result = "*._code,*._parentCode,*.sort,*.isParent,*.version,*.id,*.cid,*.layRec,*.layNo,*.parentId,*." + treeModelCode;
//		if (null == dispatcher) {
//			dispatcher = Dispatcher.getInstance();
//		}
//		Configuration configurattion = dispatcher.getConfigurationManager().getConfiguration();
//		RuntimeConfiguration rcfg = configurattion.getRuntimeConfiguration();
//		String nameSpace = "/" + moduleArtifact + "/" + entityName + "/" + StringUtils.firstLetterToLower(modelName);
//		ActionConfig aco = rcfg.getActionConfig(nameSpace, view.getName() + "Data");
//		if (aco != null) {
//			doRebuildResult(view, rcfg, nameSpace, view.getName() + "Data", result);
//			doRebuildResult(view, rcfg, nameSpace, view.getName() + "FullData", result);
//		} else {
//			String packageName = "com.supcon.orchid." + moduleArtifact + ".actions#orchid-" + moduleArtifact + "#" + nameSpace;
//			PackageConfig pkc = configurattion.getPackageConfig(packageName);
//			String modelPrefix = null;
//			if (view.getAssModel().getEcVersion() != null && view.getAssModel().getEcVersion().equals("1.0")) {
//				modelPrefix = view.getAssModel().getModelName();
//			} else {
//				modelPrefix = view.getAssModel().getJpaName();
//			}
//			String className = "com.supcon.orchid." + moduleArtifact + ".actions." + modelPrefix + "Action";
//			if (pkc != null) {
//				ObjectFactory objectFactory = dispatcher.getContainer().getInstance(ObjectFactory.class);
//				generateTreeDataActionConfig(view, "data", pkc, nameSpace, rcfg, className, objectFactory, result, newActions);
//				generateTreeDataActionConfig(view, "fulldata", pkc, nameSpace, rcfg, className, objectFactory, result, newActions);
//			}
//		}
	}

	private String generateResultField(View view) {
		String defaultPageResult = "first,hasNext,hasPre,nextPage,pageSize,pageNo,pageNos,prePage,totalCount,totalPages,treeToSurfaceMode,result.pending.id,result.pending.taskDescription,result.pending.openUrl,result.pending.userId,result.tableInfoId,result.id,result.version,result.layRec,result.status,result.tableNo,result.cid";
		StringBuffer sb = new StringBuffer();
		sb.append(defaultPageResult);
		ExtraView ev;
		if (view.getIsShadow()) {
			ev = view.getShadowView().getExtraView();
		} else {
			ev = view.getExtraView();
		}
		Map layoutMap = (Map) ev.getConfigMap().get("layout");
		List<Map> ls = (List<Map>) layoutMap.get("sections");
		for (Map lm : ls) {
			if ("LISTPT".equals(lm.get("regionType"))) {
				List<Map> lc = (List<Map>) lm.get("cells");
				for (Map lk : lc) {
					if (lk.get("isTotal") != null && "true".equals(lk.get("isTotal").toString())) {
						sb.append(",resultTotals.").append(lk.get("key"));
					}
					if (lk.get("propertyCode") != null) {
						String[] propertyarr = lk.get("propertyCode").toString().split("\\|\\|");
						Property p = propertyDao.load(propertyarr[propertyarr.length - 1]);
						if (p != null) {
							if (p.getType() != DbColumnType.SYSTEMCODE) {
								sb.append(",result.").append(lk.get("key"));
							} else {
								if (p.getMultable() || p.getSeniorSystemCode()) {
									sb.append(",result.").append(lk.get("key")).append(",result.").append(lk.get("key")).append("ForDisplay");
								} else {
									sb.append(",result.").append(lk.get("key")).append(".id,result.").append(lk.get("key")).append(".value");
								}
							}
						}
					}
				}
				break;
			}
		}
		sb.append(",result.pending.processDescription,result.pending.processId,result.pending.deploymentId");
		sb.append(",result.pending.bulkDealFlag,result.pending.activityType,result.pending.activityName");
		sb.append(",result.pending.processKey,result.pending.processVersion");
		if (view.getHasAttachment()) {
			sb.append(",result.document.id");
		}
		Model m = modelService.getModel(view.getAssModel().getCode());
		for (Property p : m.getProperties()) {
			if (p.getType() == DbColumnType.PROPERTYATTACHMENT) {
				sb.append(",result.").append(p.getName()).append("AttachementInfo,result.").append(p.getName()).append("Document.id");
			}
		}
		if (view.getAssModel().getIsMain() && view.getEntity().getPayCloseAttention() && view.getEntity().getWorkflowEnabled()) {
			sb.append(",result.isAttention");
		}
		if (view.getAssModel().getDataType() == 2) {
			sb.append(",result.layNo,result.layRec,result.parentId,result.isParent,result._code,result._parentCode,result.sort");
		}
		sb.append(",result.attrMap.*");
		return sb.toString();
	}

//	private void doRebuildResult(View view, RuntimeConfiguration rcfg, String nameSpace, String actionName, String newField) {
//		ActionConfig ac = rcfg.getActionConfig(nameSpace, actionName);
//		if (ac != null) {
//			ActionConfig.Builder acnew = new ActionConfig.Builder(ac.getPackageName(), ac.getName(), ac.getClassName());
//			acnew.methodName(ac.getMethodName());
//			acnew.addParams(ac.getParams());
//			acnew.addInterceptors(ac.getInterceptors());
//			Map<String, ResultConfig> mrc = ac.getResults();
//			Map<String, ResultConfig> mrcnew = new LinkedHashMap<String, ResultConfig>(mrc);
//			ResultConfig rc = mrc.get("success");
//			Map<String, String> mpnew = new LinkedHashMap<String, String>(rc.getParams());
//			mpnew.put("includes", newField);
//			ResultConfig.Builder rcnew = new ResultConfig.Builder(rc.getName(), rc.getClassName());
//			rcnew.addParams(mpnew);
//			mrcnew.put("success", rcnew.build());
//			if (view.getDataGridType() == 1 && view.getType() == ViewType.LIST) {
//				Map<String, String> mpxml = new LinkedHashMap<String, String>();
//				mpxml.put("location", "/views" + nameSpace + "/" + view.getName() + "-listpt-xml.ftl");
//				ResultConfig.Builder rcxml = new ResultConfig.Builder("xml", "com.supcon.orchid.container.mvc.struts2.results.BAPFreemarkerResult");
//				rcxml.addParams(mpxml);
//				mrcnew.put("xml", rcxml.build());
//			}
//			acnew.addResultConfigs(mrcnew);
//			acnew.addExceptionMappings(ac.getExceptionMappings());
//			acnew.addAllowedMethod(ac.getAllowedMethods());
//			rcfg.getActionConfigs().get(nameSpace).put(actionName, acnew.build());
//		}
//	}

	private String generateDataGridField(DataGrid dg) {
		StringBuffer sb = new StringBuffer("first,hasNext,hasPre,nextPage,pageSize,pageNo,pageNos,prePage,totalCount,totalPages");
		if (dg.getTargetModel().getDataType() == 2) {
			sb.append(",result.layNo,result.layRec,result.parentId,result.isParent,result._code,result._parentCode,result.sort");
		}
		List<String> attechKeyList = new ArrayList<String>();
		List<String> multiKeyList = new ArrayList<String>();
		Map<String, Field> dgFields = fieldService.getFields(dg);
		Map<String, Object> configMap = dataGridDao.get(dg.getCode()).getConfigMap();
		if (null != configMap && !configMap.isEmpty()) {
			Map layout = (Map) configMap.get("layout");
			if (layout != null && !layout.isEmpty()) {
				List<Map<String, Object>> sections = (List<Map<String, Object>>) layout.get("sections");
				if (null != sections && !sections.isEmpty()) {
					for (Map<String, Object> section : sections) {
						if (null != section.get("regionType") && "DATAGRID".equals(section.get("regionType").toString())) {
							List<Map<String, Object>> cells = (List<Map<String, Object>>) section.get("cells");
							for (Map<String, Object> cell : cells) {
								Field f = dgFields.get(cell.get("cellCode"));
								if (f != null) {
									if (f.getColumnType() == DbColumnType.SYSTEMCODE) {
										sb.append(",result." + f.getKey() + ".id,result." + f.getKey() + ".value");
									} else {
										sb.append(",result." + f.getKey());
									}
								}
								if (null != f && f.getShowType() == FieldType.MULTSELECT) {
									multiKeyList.add(f.getKey());
								}
								if (null != f && f.getColumnType() == DbColumnType.PROPERTYATTACHMENT) {
									attechKeyList.add(f.getKey());
								}
							}
						}
					}
				}
			}
		}
		sb.append(",result.id,result.version");
		Map<String, String> assoriKeys = dataGridService.getAsskeyByTagmodelCode(dg.getTargetModel());
		if (assoriKeys != null) {
			for (Entry<String, String> assKey : assoriKeys.entrySet()) {
				sb.append(",result." + assKey.getKey());
			}
		}
		if (multiKeyList.size() > 0) {
			for (String key : multiKeyList) {
				sb.append(",result." + key + "multiselectIDs,result." + key + "multiselectNames");
			}
		}
		if (attechKeyList.size() > 0) {
			for (String key : attechKeyList) {
				sb.append(",result." + key + "MultiFileIds,result." + key + "MultiFileNames");
			}
		}
		return sb.toString();
	}

	private String generateDataTableField(DataGrid dg) {
		StringBuffer sb = new StringBuffer("first,hasNext,hasPre,nextPage,pageSize,pageNo,pageNos,prePage,totalCount,totalPages,treeToSurfaceMode,result.pending.id,result.pending.taskDescription,result.pending.openUrl,result.pending.userId,result.tableInfoId,result.id,result.version,result.valid,result.cid,result.layRec,result.status,result.tableNo");
		if (dg.getTargetModel().getDataType() == 2) {
			sb.append(",result.layNo,result.layRec,result.parentId,result.isParent,result._code,result._parentCode,result.sort");
		}
		List<String> attechKeyList = new ArrayList<String>();
		List<String> multiKeyList = new ArrayList<String>();
		Map<String, Field> dgFields = fieldService.getFields(dg);
		Map<String, Object> configMap = dataGridDao.get(dg.getCode()).getConfigMap();
		if (null != configMap && !configMap.isEmpty()) {
			Map layout = (Map) configMap.get("layout");
			if (layout != null && !layout.isEmpty()) {
				List<Map<String, Object>> sections = (List<Map<String, Object>>) layout.get("sections");
				if (null != sections && !sections.isEmpty()) {
					for (Map<String, Object> section : sections) {
						if (null != section.get("regionType") && "LISTPT".equals(section.get("regionType").toString())) {
							List<Map<String, Object>> cells = (List<Map<String, Object>>) section.get("cells");
							for (Map<String, Object> cell : cells) {
								Field f = dgFields.get(cell.get("cellCode"));
								if (f != null) {
									Map<String, Object> fieldConfigMap = f.getConfigMap();
									if (null != fieldConfigMap && !fieldConfigMap.isEmpty()) {
										Map fieldMap = (Map) fieldConfigMap.get("field");
										if(null != fieldMap && !fieldMap.isEmpty()){
											if (fieldMap.get("isTotal") != null && "true".equals(fieldMap.get("isTotal").toString())) {
												sb.append(",resultTotals.").append(fieldMap.get("key"));
											}
										}
									}
									if (f.getColumnType() == DbColumnType.SYSTEMCODE) {
										sb.append(",result." + f.getKey() + ".id,result." + f.getKey() + ".value");
									} else {
										sb.append(",result." + f.getKey());
									}
								}
								if (null != f && f.getShowType() == FieldType.MULTSELECT) {
									multiKeyList.add(f.getKey());
								}
								if (null != f && f.getColumnType() == DbColumnType.PROPERTYATTACHMENT) {
									attechKeyList.add(f.getKey());
								}
							}
						}
					}
				}
			}
		}
		Map<String, String> assoriKeys = dataGridService.getAsskeyByTagmodelCode(dg.getTargetModel());
		if (assoriKeys != null) {
			for (Entry<String, String> assKey : assoriKeys.entrySet()) {
				sb.append(",result." + assKey.getKey());
			}
		}
		if (multiKeyList.size() > 0) {
			for (String key : multiKeyList) {
				sb.append(",result." + key + "multiselectIDs,result." + key + "multiselectNames");
			}
		}
		if (attechKeyList.size() > 0) {
			for (String key : attechKeyList) {
				sb.append(",result." + key + "AttachementInfo,result." + key + "Document.id");
			}
		}
		return sb.toString();
	}

	/**
	 * 保存增强型视图快速查询条件
	 *
	 * @param fqj
	 * @param full
	 */
	@Override
	@Transactional
	public void saveExtraFastQueryJson(FastQueryJson fqj, boolean full) {
		if (null != fqj) {
			if (full) {
				String fullConfig = ecConfigService.getEcFullConfig(fqj);
				fqj.setQueryConfig(fullConfig);
				viewDao.saveFastQueryJson(fqj);
			} else {
				View view = getView(fqj.getView().getCode());
				if (null == fqj.getCode() || fqj.getCode().length() == 0) {
					fqj.setCode(view.getCode() + System.currentTimeMillis());
				}
				fqj.setView(view);
				viewDao.saveFastQueryJson(fqj);
			}
		}
	}

	/**
	 * 保存增强型视图高级查询条件
	 *
	 * @param aqj
	 * @param full
	 */
	@Override
	@Transactional
	public void saveExtraAdvQueryJson(AdvQueryJson aqj, boolean full) {
		if (null != aqj) {
			if (full) {
				String fullConfig = ecConfigService.getEcFullConfig(aqj);
				aqj.setQueryConfig(fullConfig);
				viewDao.saveAdvQueryJson(aqj);
			} else {
				View view = getView(aqj.getView().getCode());
				if (null == aqj.getCode() || aqj.getCode().length() == 0) {
					Long currentTimes = System.currentTimeMillis();
					aqj.setCode(view.getCode() + currentTimes);
					aqj.setName(view.getName() + currentTimes);
				}
				aqj.setView(view);
				viewDao.saveAdvQueryJson(aqj);
			}
		}
	}

	/**
	 * 验证视图是否被引用为批量控件打印视图
	 *
	 * @param view
	 * @return
	 */
	private boolean checkViewIsBatchControlPrint(View view) {
		List<View> batchControlPrintSelectView = viewDao.findByHql("from View v where v.batchControlPrintSelectView.code = ?0", view.getCode());
		if (null != batchControlPrintSelectView && !batchControlPrintSelectView.isEmpty()) {
			return false;
		}
		return true;
	}

	@Override
	@Transactional
	public void changeViewProjFlag(View view, Boolean projFlag) {
		ExtraView ev = view.getExtraView();
		ev.setProjFlag(projFlag);
		extraViewDao.save(ev);
		extraViewDao.flush();

		CustomerCondition ccview = customerConditionService.getCustomerCondition(view);
		if (ccview != null) {
			ccview.setProjFlag(projFlag);
			customerConditionService.saveCustomerCondition(ccview);
		}

		List<Sql> sqls = sqlService.getSqls(view.getCode());
		for (Sql sql : sqls) {
			sql.setProjFlag(projFlag);
			sqlService.save(sql);
		}

		List<DataGroup> dgs = findDataGroups(view);
		for (DataGroup dg : dgs) {
			dg.setProjFlag(projFlag);
			Set<DataClassific> dcs = dg.getDataClassifics();
			for (DataClassific dc : dcs) {
				dc.setProjFlag(projFlag);
				CustomerCondition cc = customerConditionService.getCustomerCondition(dc);
				if (cc != null) {
					cc.setProjFlag(projFlag);
					customerConditionService.saveCustomerCondition(cc);
				}
				dataClassificDao.save(dc);
			}
			dataGroupDao.save(dg);
		}
		dataClassificDao.flush();
		dataGroupDao.flush();

		List<DataGrid> dgList = dataGridService.getDataGridByView(view, false);
		for (DataGrid dg : dgList) {
			dg.setProjFlag(projFlag);
			dataGridService.save(dg);
			CustomerCondition cc = customerConditionService.getCustomerCondition(dg);
			if (cc != null) {
				cc.setProjFlag(projFlag);
				customerConditionService.saveCustomerCondition(cc);
			}
		}

		List<FastQueryJson> fqjList =   fastQueryJsonService.findFastQueryJsons(Restrictions.eq("view.code", view.getCode()));
		for (FastQueryJson fqj : fqjList) {
			fqj.setProjFlag(projFlag);
			saveFastQueryJson(fqj);
		}

		List<AdvQueryJson> aqjList =   advQueryJsonService.findAdvQueryJsons(Restrictions.eq("view.code", view.getCode()));
		for (AdvQueryJson aqj : aqjList) {
			aqj.setProjFlag(projFlag);
			saveAdvQueryJson(aqj);
		}

		// 图表
		echartsService.changeEchartsProjFlag(view.getCode(), projFlag);
	}

	@Override
	@Transactional
	public void updatePublishTime(View view) {
		Date currentDate = new Date();
		viewDao.createQuery("update View v set v.publishTime=?0,modifyTime=?1,v.projEnabled=true where v=?2", currentDate, currentDate, view).executeUpdate();
	}
	@Override
	@Transactional
	public void updatePublishTimeEnableFalse(View view) {
		Date currentDate = new Date();
		viewDao.createQuery("update View v set v.publishTime=null,modifyTime=?0 where v=?1", currentDate, view).executeUpdate();
	}

	@Override
	@Transactional
	public void updatePublishTimeToNull(View view) {
		// TODO Auto-generated method stub
		Date currentDate = new Date();
		viewDao.createQuery("update View v set v.publishTime=null,modifyTime=?0,v.projEnabled=true where v=?1", currentDate, view).executeUpdate();
	}


	/**
	 * 处理模块中视图的打印、批量打印按钮
	 * @param view
	 * @param
	 */
	@Override
	@Transactional
	public void dealPrintButton(View view) {
		if(null != view){
			List<View> views = new ArrayList<>();
			views.add(view);
			this.dealPrintButton(view.getModuleCode(), views);
		}
	}

	/**
	 * 处理模块中视图的打印、批量打印按钮
	 * @param moduleCode
	 * @param views
	 */
	@SuppressWarnings("unchecked")
	@Override
	@Transactional
	public void dealPrintButton(String moduleCode, List<View> views) {
		if(null == views || views.isEmpty()){
			return;
		}
		Map<String,View> map = new HashMap<>();
		for(View view : views){
			map.put(view.getCode(), view);
		}
		boolean isProj = null!= ProjectFlagHolder.getInstance().getProjFlag().get() && ProjectFlagHolder.getInstance().getProjFlag().get();
		//查找所有的打印、批量打印按钮
		String buttonTableName = "EC_BUTTON";
		if(isProj){
			buttonTableName = "PROJECT_BUTTON";
		}

		StringBuilder sqlBuilder = new StringBuilder("SELECT CODE,VIEW_CODE,PROJ_FLAG FROM ");
		sqlBuilder.append(buttonTableName).append(" WHERE VALID=1 ");
		sqlBuilder.append(" AND OPERATE_TYPE IN ('PRINT','BATCH_PRINT') AND VIEW_CODE IN (:viewCodes)");

		StringBuilder sqlPreviewBuilder = new StringBuilder("SELECT CODE,VIEW_CODE,PROJ_FLAG FROM ");
		sqlPreviewBuilder.append(buttonTableName).append(" WHERE VALID=1 ");
		sqlPreviewBuilder.append(" AND OPERATE_TYPE IN ('BATCH_PRINT_PREVIEW') AND VIEW_CODE IN (:viewCodes)");

		buttonDao.clear();
		List<Object[]> buttonMaps = buttonDao.createNativeQuery(sqlBuilder.toString()).setParameterList("viewCodes", map.keySet()).list();
		List<Object[]> buttonPreviewMaps = buttonDao.createNativeQuery(sqlPreviewBuilder.toString()).setParameterList("viewCodes", map.keySet()).list();
		List<String> delButtonCodes = new ArrayList<String>();
		final List<View> addViews = new ArrayList<View>();
		final List<View> addPreviewViews = new ArrayList<View>();
		for (View view : views) {
			boolean isContaines = false;
			if (null != buttonMaps && !buttonMaps.isEmpty()) {
				for (Object[] buttonArr : buttonMaps) {
					if (null != buttonArr[1]) {
						String viewCode = buttonArr[1].toString();
						if (view.getCode().equals(viewCode)) {
							isContaines = true;
							if (!(view.getControlPrint() || view.getIsBatchControlPrint()) || (isProj && !view.getProjEnabled())) {// 没有启用打印、批量打印或者已停用的视图删除对应的按钮
								delButtonCodes.add(buttonArr[0].toString());
							} if(isProj && (null == buttonArr[2] || "0".equals(buttonArr[2].toString()))){
								delButtonCodes.add(buttonArr[0].toString());
								isContaines = false;
							}
							break;
						}
					}
				}
			}
			//对于已启用且勾选了打印、批量打印的工程期视图如PROJECT_BUTTON 中Button信息以存在则不处理，如不存在就新增Button数据
			if (!isContaines && (view.getControlPrint()|| view.getIsBatchControlPrint())) {
				if(isProj){
					if(view.getProjEnabled()){
						addViews.add(view);
					}
				} else {
					addViews.add(view);
				}
			}
		}

		for (View view : views) {
			boolean isPreviewContaines = false;
			if (null != buttonPreviewMaps && !buttonPreviewMaps.isEmpty()) {
				for (Object[] buttonArr : buttonPreviewMaps) {
					if (null != buttonArr[1]) {
						String viewCode = buttonArr[1].toString();
						if (view.getCode().equals(viewCode)&&view.getType()==ViewType.LIST) {
							isPreviewContaines = true;
							if (!(view.getIsBatchControlPrint()|| (isProj && !view.getProjEnabled()))) {// 没有启用打印、批量打印或者已停用的视图删除对应的按钮
								delButtonCodes.add(buttonArr[0].toString());
							}
							if(isProj && (null == buttonArr[2] || "0".equals(buttonArr[2].toString()))){
								delButtonCodes.add(buttonArr[0].toString());
								isPreviewContaines = false;
							}
							break;
						}
					}
				}
			}
			if (!isPreviewContaines && view.getIsBatchControlPrint()) {
				if(isProj){
					if(view.getProjEnabled()){

						addPreviewViews.add(view);
					}
				} else {
					addPreviewViews.add(view);
				}
			}
		}

		//删除已停用视图
		if(null != delButtonCodes && !delButtonCodes.isEmpty()){
			buttonDao.createNativeQuery("DELETE FROM " + buttonTableName + " WHERE CODE IN (:codes)").setParameterList("codes", delButtonCodes).executeUpdate();
		}
		if(null != addViews && !addViews.isEmpty()){
			//重新插入打印、批量打印按钮数据
			String insertSQL = "insert into " + buttonTableName + " (CODE, EC_ENV, VERSION, CREATE_TIME, VALID, ENTITY_CODE, MODULE_CODE, PROJ_FLAG, VIEW_CODE, OPERATE_TYPE, NAME,IS_SIGNATURE_CONFIG,DISPLAY_NAME, IS_PERMISSION) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			jdbcTemplate.batchUpdate(insertSQL, new BatchPreparedStatementSetter() {

				@Override
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					View v = addViews.get(i);
					String code = v.getCode() + "_";
					String opType = "PRINT";
					String name = null;
					String displayName = null;
					if(v.getControlPrint()){
						code = v.getCode() + "_" + "print_print";
						displayName = InternationalResource.get(v.getControlName());
						name = "print";
					} else if(v.getIsBatchControlPrint()){
						code = v.getCode() + "_" + "print_batchPrint";
						opType = "BATCH_PRINT";
						displayName = "ec.print.batchPrint";
						name = "batchPrint";
					}
					ps.setString(1, code);
					ps.setString(2, "product");
					ps.setInt(3, 0);
					ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
					ps.setInt(5, 1);
					ps.setString(6, v.getEntity().getCode());
					ps.setString(7, v.getModuleCode());
					ps.setBoolean(8, isProj);
					ps.setString(9, v.getCode());
					ps.setString(10, opType);
					ps.setString(11, name);
					ps.setInt(12, 1);
					ps.setString(13, displayName);
					ps.setBoolean(14, true);
				}

				@Override
				public int getBatchSize() {
					return addViews.size();
				}
			});
		}
		if(null != addPreviewViews && !addPreviewViews.isEmpty()){
			//重新插入打印、批量打印按钮数据
			String insertSQL = "insert into " + buttonTableName + " (CODE, EC_ENV, VERSION, CREATE_TIME, VALID, ENTITY_CODE, MODULE_CODE, PROJ_FLAG, VIEW_CODE, OPERATE_TYPE, NAME,IS_SIGNATURE_CONFIG,DISPLAY_NAME, IS_PERMISSION) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			jdbcTemplate.batchUpdate(insertSQL, new BatchPreparedStatementSetter() {

				@Override
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					View v = addPreviewViews.get(i);
					String code = v.getCode() + "_" + "print_batchPrint_preview";
					String opType = "BATCH_PRINT_PREVIEW";
					String name = "batchPrintPreview";
					String displayName = "ec.print.batchPrintPreview";
					ps.setString(1, code);
					ps.setString(2, "product");
					ps.setInt(3, 0);
					ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
					ps.setInt(5, 1);
					ps.setString(6, v.getEntity().getCode());
					ps.setString(7, v.getModuleCode());
					ps.setBoolean(8, isProj);
					ps.setString(9, v.getCode());
					ps.setString(10, opType);
					ps.setString(11, name);
					ps.setInt(12, 1);
					ps.setString(13, displayName);
					ps.setBoolean(14, true);
				}

				@Override
				public int getBatchSize() {
					return addPreviewViews.size();
				}
			});
		}

	}

	@Autowired
	private MsModuleServiceApi msModuleServiceApi;

	@Override
	@Transactional
	public void viewPublish(View view, ExtraView ev) {
		checkViewPublish(view.getDataGrids(), view.getExtraView());
		log.debug("======开始发布视图:" + view.getCode());
		Boolean isProj = ProjectFlagHolder.getInstance().getProjFlag().get();
		msModuleServiceApi.publishView(view.getCode(),isProj);
	}


	private void viewPretreat(View view, List<Object> newActions, String tmpPath) throws Exception {
		if (view.getType() == ViewType.EDIT) {
			String modelPrefix = null;
			Model model = view.getAssModel();
			if (model.getEcVersion() != null && "1.0".equals(model.getEcVersion())) {
				modelPrefix = model.getModelName();
			} else {
				modelPrefix = model.getJpaName();
			}
			String basePath = tmpPath + File.separator + model.getModuleCode();
			String packagePath = File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "com" + File.separator
					+ "supcon" + File.separator + "orchid" + File.separator + view.getEntity().getModule().getArtifact() + File.separator;
			String servicePath = basePath + File.separator + "service";
			String filePath = (servicePath + packagePath + "actions");
			String fileName = modelPrefix + "Action-" + view.getName() + "-validation-bap.xml";
			FileInputStream is = null;
			try {
				is = new FileInputStream(new File(filePath + File.separator + fileName));
				String validatorKey = "com.supcon.orchid." + view.getEntity().getModule().getArtifact() + ".actions." + modelPrefix + "Action/"
						+ view.getName() + "|"+view.getName();
			} catch (Exception e) {
				log.error(e.getMessage());
			} finally {
				if (is != null) {
					is.close();
				}
			}
//			view = this.getView(view.getCode(), true);
			if (view.getDataGrids().size() > 0) {
				for (DataGrid dg : view.getDataGrids()) {
					Model dgModel = dg.getTargetModel();
					String dgModelPrefix = null;
					if (dgModel.getEcVersion() != null && "1.0".equals(dgModel.getEcVersion())) {
						dgModelPrefix = dgModel.getModelName();
					} else {
						dgModelPrefix = dgModel.getJpaName();
					}
					String dgfileName = dgModelPrefix + "-" + dg.getName() + "-validation-bap.xml";
					File f = new File(filePath + File.separator + dgfileName);
					if (f.exists()) {
						FileInputStream dgis = null;
						try {
							dgis = new FileInputStream(f);
							String dgvalidatorKey = "com.supcon.orchid." + dgModel.getEntity().getModule().getArtifact() + ".entities." + dgModelPrefix + "/"
									+ view.getName() + "|" + dg.getName();

						} catch (Exception e) {
							log.error(e.getMessage());
						} finally {
							if (dgis != null) {
								dgis.close();
							}
						}
					}
				}
			}
		} else if ((view.getType() == ViewType.LIST || view.getType() == ViewType.REFERENCE)
				&& (view.getShowType() != ShowType.LAYOUT && view.getShowType() != ShowType.LAYOUT2)) {
			this.rebuildQueryResult(view, newActions);
		} else if (view.getType() == ViewType.TREE || view.getType() == ViewType.REFTREE) {
			this.rebuildTreeDataResult(view, newActions);
		}
	}


	@Override
	public void changeLayoutName(String viewCode, String oldLayoutName, String newLayoutName) {
		String updateFastQueryJson = "update FastQueryJson fqj set fqj.layoutName = ? where fqj.view.code = ? and fqj.layoutName = ?";
		String updateAdvQueryJson = "update AdvQueryJson aqj set aqj.layoutName = ? where aqj.view.code = ? and aqj.layoutName = ?";
		String updateDataGroup = "update DataGroup dg set dg.layoutName = ? where dg.view.code = ? and dg.layoutName = ?";
		viewDao.createQuery(updateFastQueryJson, newLayoutName, viewCode, oldLayoutName).executeUpdate();
		viewDao.createQuery(updateAdvQueryJson, newLayoutName, viewCode, oldLayoutName).executeUpdate();
		viewDao.createQuery(updateDataGroup, newLayoutName, viewCode, oldLayoutName).executeUpdate();
	}

	/**
	 * 视图发布弃审
	 *
	 * @param module
	 *            ,views
	 */
	private void publishRetrialMenuOperate(Module module, List<View> views) {
		Map<String, String> currentMap = new HashMap<String, String>();
		String retrial = "retrial";
		if (StringUtils.isEmpty(module.getType()) && !MIS.equals(module.getType())) {
			retrial = "retrial.action";
		}
		for (View v : views) {
			if (null == v.getRetrialFlag() || !v.getRetrialFlag()) {
				continue;
			}
			Entity entity = v.getEntity();
			if (!currentMap.containsKey(entity.getCode())) {
				String code = entity.getCode() + "_retrial";

				MenuOperate mp = menuOperateService.getFlowList(entity.getCode());
				if (null == mp) {
					continue;
				}
				List<MenuOperate> checkList = menuOperateService.getByCode(code, mp.getCid());
				if (null != checkList && checkList.size() > 0) {
					continue;
				}
				MenuOperate newOp = new MenuOperate();
				newOp.setAction(retrial);
				newOp.setCid(mp.getCid());
				newOp.setCode(code);
				newOp.setEntityCode(entity.getCode());
				newOp.setVersion(0);
				newOp.setValid(true);
				newOp.setName("foundation.common.retrial");
				newOp.setMenuInfo(mp.getMenuInfo());
				newOp.setModule(mp.getModule());
				newOp.setNamespace(mp.getNamespace());
				newOp.setUrl(newOp.getNamespace() + "/" + newOp.getAction());
				menuOperateService.save(newOp);
				currentMap.put(entity.getCode(), "");
			}
		}
	}

	@Override
	@Transactional
	public BackupView getBackupView(View view) {
		List<BackupView> backupViews = backupViewDao.findByHql("from BackupView where view = ?0 and valid = true order by code desc", view);
		return (null != backupViews && backupViews.size() > 0) ? backupViews.get(0) : null;
	}

	/**
	 * 通过backView，将关联的datagrid的valid设置为true
	 *
	 * @param backupView
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Transactional
	public void dataGridEnable(View view, BackupView backupView) {
		if (null != backupView && null != backupView.getCode()) {
			List<BackupDataGrid> backupDataGrids = dataGridService.getBackupDataGridByBackupViewCode(backupView.getCode());
			for (BackupDataGrid backupDataGrid : backupDataGrids) {
				dataGridDao.bulkExecute("update DataGrid dg set valid = true where view = ?0 and name = ?1 ", view, backupDataGrid.getName());
			}
		}
	}


	@Override
	public List<View> findByCriteria(DetachedCriteria detachedCriteria) {
		return viewDao.findByCriteria(detachedCriteria);
	}

	@Override
	@Transactional
	public View getViewByHql(String hql, String param){
		View view = (View) viewDao.createQuery(hql,param).uniqueResult();
		return view;
	}

	@Override
	public List<View> getViewsByHql(View view) {
		return null;
	}
	@Override
	@Transactional
	public void copyExtraView(View srcView, View targetView) {
		String targetConfig = null;
		String config = null;
		ExtraView ev = null;
		srcView = getView(srcView.getCode(), true);
		if (srcView.getExtraView() != null) {
			config = this.getExtraViewFullConfig(srcView);
			targetConfig = replaceFieldCode(config, srcView, targetView.getEntity().getCode() + "_" + targetView.getName(), targetView.getType());
			targetConfig = regenerateTabCode(targetConfig);
			targetConfig = regenerateLayoutCode(targetConfig);
			targetConfig = regenerateSectionCode(targetConfig);
			targetConfig = regenerateCellCode(targetConfig);
			if (ViewType.LIST.equals(srcView.getType()) || ViewType.REFERENCE.equals(srcView.getType())) {
				targetConfig = dealViewConfig(targetConfig, srcView.getCode() + "_LISTPT_ASSO_");
				targetConfig = dealViewConfig(targetConfig, srcView.getCode() + "_LISTPT_CUSTOM_");
			}
			ev = targetView.getExtraView();
			if (ev == null) {
				ev = new ExtraView();
				ev.setView(targetView);
				targetView.setExtraView(ev);
				ev.setCode(targetView.getCode());
			}
			ev.setConfig(targetConfig);
		}
		List<DataGrid> dgs = null;
		List<DataGrid> targetDgs = new ArrayList<>();
		if (targetView.getExtraView() != null) {
			if (targetView.getType() == ViewType.EDIT || targetView.getType() == ViewType.VIEW) {
				dgs = dataGridService.getDataGridByView(srcView, false);
				if (dgs != null && !dgs.isEmpty()) {
					for (DataGrid dg : dgs) {
						targetDgs.add(copyDatagrid(dg, targetView, null, null));
					}
				}
			}
			if (targetView.getShowType() != ShowType.LAYOUT) {
				if (ev != null && ev.getConfig() != null && ev.getConfig().length() > 0) {
					String viewConfig = ev.getConfig();
					if (viewConfig != null && viewConfig.length() > 0) {
						Map<String, Object> configMap = new EcExtraViewIntegrationUtils().ecSplitConfig(viewConfig);
						if (configMap.get("config") != null) {
							config = configMap.get("config").toString();
							ev.setConfig(config);
							saveExtraView(ev, null);
						}
						if (configMap.get("fieldConfig") != null) {
							String fieldConfig = configMap.get("fieldConfig").toString();
							if (targetView.getType() != ViewType.MNECODE) {
								buttonService.saveButton(targetView, fieldConfig, null);
							}
							fieldService.saveFields(targetView, fieldConfig, null, null, null);
							if (targetView.getType() != ViewType.MNECODE) {
								eventService.saveEvent(targetView, fieldConfig);
							}
						}
					}
					if (targetDgs != null && !targetDgs.isEmpty()) {
						viewDao.flush();
						viewDao.clear();
						for (int i = 0; i < targetDgs.size(); i++) {
							DataGrid dg = targetDgs.get(i);
							String dgConfig = dg.getConfig();
							if (dgConfig != null && dgConfig.length() > 0) {
								Map<String, Object> dgConfigMap = new EcExtraViewIntegrationUtils().ecSplitConfig(dgConfig);
								if (dgConfigMap.get("config") != null) {
									dg.setConfig(dgConfigMap.get("config").toString());
									dataGridDao.update(dg);
								}

								if (dgConfigMap.get("fieldConfig") != null) {
									String fieldConfig = dgConfigMap.get("fieldConfig").toString();
									fieldService.saveFields(dg, fieldConfig, null, null, null);
									buttonService.saveButton(dg, fieldConfig, null);
								}
								// 复制datagrid中的自定义条件
								List<CustomerCondition> ccs = customerConditionService.findCustomerConditions(
										Restrictions.like("code", dgs.get(i).getCode(), MatchMode.START), Restrictions.eq("valid", true));
								viewDao.flush();
								viewDao.clear();
								if (ccs != null && !ccs.isEmpty()) {
									for (CustomerCondition cc : ccs) {
										cc.setCode(cc.getCode().replace(dgs.get(i).getCode(), dg.getCode()));
										cc.setView(targetView);
										cc.setDataGrid(dg);
										customerConditionService.saveCustomerCondition(cc);
									}
								}
							}
						}
					}
				}
				if (targetView.getType() == ViewType.LIST || targetView.getType() == ViewType.REFERENCE) {
					FastQueryJson fqj = null;
					if (srcView.getFastQueryJson() != null && srcView.getFastQueryJson().size() > 0) {
						fqj = srcView.getFastQueryJson().get(0);
					}
					if (fqj != null) {
						fastQueryJsonDao.evict(fqj);
						fqj.setCode(targetView.getCode());
						fqj.setView(targetView);
						fastQueryJsonDao.save(fqj);
					}

					AdvQueryJson aqj = null;
					if (srcView.getAdvQueryJson() != null && srcView.getAdvQueryJson().size() > 0) {
						aqj = srcView.getAdvQueryJson().get(0);
					}
					if (aqj != null) {
						advQueryJsonDao.evict(aqj);
						aqj.setCode(targetView.getCode());
						aqj.setView(targetView);
						advQueryJsonDao.save(aqj);
					}
				}
			} else {
				saveExtraView(ev, null);
			}
			// 复制视图中的自定义条件
			List<CustomerCondition> ccs = customerConditionService.findCustomerConditions(Restrictions.like("code", srcView.getCode(), MatchMode.START),
					Restrictions.eq("valid", true));
			viewDao.flush();
			viewDao.clear();
			if (ccs != null && !ccs.isEmpty()) {
				for (CustomerCondition cc : ccs) {
					cc.setCode(cc.getCode().replace(srcView.getCode(), targetView.getCode()));
					cc.setView(targetView);
					customerConditionService.saveCustomerCondition(cc);
				}
			}
			List<DataGroup> src_dataGroups = findDataGroups(srcView);
			if (src_dataGroups != null && src_dataGroups.size() > 0) {
				for (DataGroup dgroup : src_dataGroups) {
					DataGroup newdgroup = new DataGroup();
					newdgroup.setName(dgroup.getName());
					newdgroup.setDisplayName(dgroup.getDisplayName());
					newdgroup.setIsMultiple(dgroup.getIsMultiple());
					newdgroup.setView(targetView);
					newdgroup.setCode(targetView.getCode() + "_" + newdgroup.getName());
					saveDataGroup(newdgroup);
					List<DataClassific> src_dataClassifics = findDataClassifics(dgroup);
					if (src_dataClassifics != null && src_dataClassifics.size() > 0) {
						for (DataClassific dclassific : src_dataClassifics) {
							DataClassific newdclassific = new DataClassific();
							newdclassific.setName(dclassific.getName());
							newdclassific.setDisplayName(dclassific.getDisplayName());
							newdclassific.setCondition(dclassific.getCondition());
							newdclassific.setDataGroup(newdgroup);
							newdclassific.setCode(newdgroup.getCode() + "_" + newdclassific.getName());
							CustomerCondition cc = customerConditionService.getCustomerCondition(dclassific);
							CustomerCondition newcc = new CustomerCondition();
							BeanUtils.copyProperties(cc, newcc);
							newcc.setView(targetView);
							newcc.setDataClassific(newdclassific);
							newcc.setCode(targetView.getCode() + "_" + newdclassific.getCode());
							saveDataClassific(newdclassific);
							customerConditionService.saveCustomerCondition(newcc);
						}
					}
				}
			}
		}
	}
	/**
	 * 检查视图是否可被删除</br>
	 * 检查项 1.是否被其它视图引用 （按钮、选择控件的参照、链接、允许查看时配置的查看视图、树视图） 2.是否被工作流关联  3.业务中心
	 * @param view
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<String> checkDeleteView(View view){
		List<String> msgs = new LinkedList<String>();

		String viewCode = view.getCode();
		String moduleCode = view.getModuleCode();

		//获取模块的依赖关系
		String moduleRelationSql = "SELECT MODULE_CODE FROM EC_MODULE_RELATION WHERE TARGET_MODULE_CODE = ?";
		List<String> relationCodes = modelDao.createNativeQuery(moduleRelationSql, moduleCode).list();
		if(null == relationCodes){
			relationCodes = new ArrayList<String>();
		}
		if(null != moduleCode){
			relationCodes.add(moduleCode);
		}
		//判断视图是否被其它视图引用
		Set<String> viewSet = new HashSet<String>();
		Set<String> dataGridSet = new HashSet<String>();

		long timestamp = System.currentTimeMillis();
		Boolean projFlag = false;
		if(view.getProjFlag()!=null&& view.getProjFlag() == true) {
			projFlag = true;
			ProjectFlagHolder.getInstance().getProjFlag().set(true);
		}

		String buttonHql = "from Button where valid=true and viewSelect.code=?0 and view.code != ?0 and moduleCode in (:relationCodes)";
		List<Object> buttonArgs = new LinkedList<Object>();
		buttonArgs.add(viewCode);
		List<Button> buttons = buttonDao.createQuery(buttonHql, buttonArgs.toArray()).setParameterList("relationCodes", relationCodes).list();
		if (buttons != null && !buttons.isEmpty()) {
			for (Button button : buttons) {
				if (button.getView() != null) {
					viewSet.add(button.getView().getCode());
				}
			}
		}
		log.info("===========查询BUTTON耗时:" + (System.currentTimeMillis() - timestamp) + "ms");

		timestamp = System.currentTimeMillis();
		String fieldHql = "from Field where moduleCode in (:relationCodes) and valid=true";
		List<Field> fields = viewDao.createQuery(fieldHql).setParameterList("relationCodes", relationCodes).list();
		if (fields != null && !fields.isEmpty()) {
			for (Field field : fields) {
				if (!StringUtils.isEmpty(field.getConfig()) && (field.getConfig().contains("<referenceview><![[CDATA[[" + viewCode)
						|| field.getConfig().contains("<linkView><![[CDATA[[" + viewCode)
						|| field.getConfig().contains("<allowviewcode><![[CDATA[[" + viewCode))) {
					if (field.getView() != null) {
						viewSet.add(field.getView().getCode());
					}
					if (field.getDataGrid() != null) {
						dataGridSet.add(field.getDataGrid().getCode());
					}
				}
			}
		}
		log.info("===========查询FIELD耗时:" + (System.currentTimeMillis() - timestamp) + "ms");

		timestamp = System.currentTimeMillis();
		String extraViewHql = "from ExtraView where view.valid=true and view.moduleCode in (:relationCodes)";
		List<ExtraView> extraViews = extraViewDao.createQuery(extraViewHql).setParameterList("relationCodes", relationCodes).list();
		if (extraViews != null && !extraViews.isEmpty()) {
			for (ExtraView extraView : extraViews) {
				if (!StringUtils.isEmpty(extraView.getConfig()) && ((extraView.getConfig().contains("<tree_model><![[CDATA[[" + viewCode)
						|| extraView.getConfig().contains("<vcode><![[CDATA[[" + viewCode)
						|| extraView.getConfig().contains("<treeView><![[CDATA[[" + viewCode)))) {
					if (extraView.getView() != null) {
						viewSet.add(extraView.getView().getCode());
					}
				}
			}
		}
		log.info("===========查询EXTRA_VIEW耗时:" + (System.currentTimeMillis() - timestamp) + "ms");

		timestamp = System.currentTimeMillis();
		String viewHql = "from View where valid=true and (batchControlPrintSelectView.code=?0 or shadowView.code=?0 or reference.code=?0) and moduleCode in (:relationCodes)";
		List<Object> viewArgs = new LinkedList<Object>();
		viewArgs.add(viewCode);
		List<View> views = viewDao.createQuery(viewHql, viewArgs.toArray()).setParameterList("relationCodes", relationCodes).list();
		if (views != null && !views.isEmpty()) {
			for (View tmp : views) {
				viewSet.add(tmp.getCode());
			}
		}
		log.info("===========查询VIEW耗时:" + (System.currentTimeMillis() - timestamp) + "ms");

		if(!viewSet.isEmpty()){
			List<View> assviews = viewDao.createQuery("from View where code in (:codes)").setParameterList("codes", viewSet).list();
			msgs.addAll(getCheckMsg(assviews, null, projFlag));
		}
		if(!dataGridSet.isEmpty()){
			List<DataGrid> dgs = dataGridDao.createQuery("from DataGrid where code in (:codes)").setParameterList("codes", dataGridSet).list();
			msgs.addAll(getCheckMsg(null, dgs, projFlag));
		}
		//检查非继承视图是否被对象类型字段关联
		if(view.getInheritType()==null){

			String refPropertySql = "from CustomPropertyModelMapping where refView.code = ?0 ";
			List<CustomPropertyModelMapping> refPropertyModelMappings = viewDao.findByHql(refPropertySql,view.getCode());
			if(null != refPropertyModelMappings && !refPropertyModelMappings.isEmpty()){
				for(CustomPropertyModelMapping customPropertyModelMapping : refPropertyModelMappings){

					Property property = customPropertyModelMapping.getProperty();
					if(null != property ){
						StringBuilder sb = new StringBuilder(InternationalResource.get(property.getModel().getEntity().getModule().getName())).append("模块-");
						sb.append(InternationalResource.get(property.getModel().getEntity().getName())).append("实体-");
						sb.append(InternationalResource.get(property.getModel().getName())).append("模型-");
						sb.append(property.getName()).append("字段");
						if(null != projFlag && projFlag == true){
							sb.append("(工程期)");
						}
						msgs.add(sb.toString());
					}
				}
			}
		}

		log.info("===========组装信息耗时:" + (System.currentTimeMillis() - timestamp) + "ms");
		timestamp = System.currentTimeMillis();
		//3.工作流
		String flowHql = "from Deployment where valid=true and entityCode = ?0";
		List<Deployment> deployments = viewDao.findByHql(flowHql, view.getEntity().getCode());
		Map<String,Deployment> deploymentMap = new HashMap<String, Deployment>();
		if(null != deployments && !deployments.isEmpty()){
			for(Deployment deployment : deployments){
				deploymentMap.put(deployment.getProcessKey(), deployment);
				if(null != deployment.getMainViewViewCode() && deployment.getMainViewViewCode().equals(view.getCode())){
					StringBuilder sb = new StringBuilder("被工作流");

					sb.append(InternationalResource.get(deployment.getName())).append("(").append(deployment.getProcessKey()).append(")主查看视图关联");
					if(null != projFlag && projFlag == true){
						sb.append("(工程期)");
					}
					if(!msgs.contains(sb.toString())){
						msgs.add(sb.toString());
					}
				}
			}
			//工作流活动
			String taskHql = "from Task where valid=true and viewCode = ? and processKey in (:processKeys)";
			List<Task> tasks = viewDao.createQuery(taskHql, viewCode).setParameterList("processKeys", deploymentMap.keySet()).list();
			Map<String,Set<String>> keyTaskNameMap = new HashMap<String, Set<String>>();
			if(null != tasks && !tasks.isEmpty()){
				for(Task task : tasks){
					Set<String> taskNames = keyTaskNameMap.get(task.getProcessKey());
					if(null == taskNames){
						taskNames = new HashSet<String>();
					}
					taskNames.add(InternationalResource.get(task.getName()));
					keyTaskNameMap.put(task.getProcessKey(), taskNames);
				}
				for(Entry<String, Set<String>> entry : keyTaskNameMap.entrySet()){
					StringBuilder sb = new StringBuilder("被工作流");
					Deployment deployment = deploymentMap.get(entry.getKey());
					sb.append(InternationalResource.get(deployment.getName())).append("(").append(deployment.getProcessKey()).append(")中");
					if(null != entry.getValue()){
						String names = "";
						for(String taskName : entry.getValue()){
							names += "、" + taskName;
						}
						if(!StringUtils.isEmpty(names)){
							sb.append(names.substring(1)).append("活动关联");
							if(null != projFlag && projFlag == true){
								sb.append("(工程期)");
							}
							if(!msgs.contains(sb.toString())){
								msgs.add(sb.toString());
							}
						}
					}
				}
			}
		}
		log.info("===========查询工作流耗时:" + (System.currentTimeMillis() - timestamp) + "ms");
		ProjectFlagHolder.getInstance().getProjFlag().set(false);
		return StringUtils.sort(msgs);
	}

	/**
	 * @Description: 复制视图时替换增强型视图中图表的编码
	 *
	 * @param: 参数描述
	 * @return: 返回结果描述
	 *
	 * @author: huning
	 * @date: 2019年4月8日 下午2:13:34
	 */
	private String regenerateEchartsCode(String config, String oldCode, String newCode) {
		if (!StringUtils.isEmpty(config) && config.indexOf("<![CDATA[ECHARTS]]>") > -1) {
			config = config.replaceAll(oldCode+"@@", newCode+"@@");
		}
		return config;
	}



	private void checkViewPublish(List<DataGrid> dataGrids, ExtraView extraView) {
		checkDataGridFildNull(dataGrids);
		checkEchartsIsNull(extraView);
	}

	@Override
	public String getListViewCodeByView(String viewCode, Long cid) {
		View viewView = getView(viewCode);
		if (null != viewView) {
			List<View> views = viewDao.createQuery("from View where entity = ? and type = ?", viewView.getEntity(), ViewType.LIST).list();
			if (!views.isEmpty()) {
				String powerCode = "";
				out: for (View v : views) {
					powerCode = v.getCode() + "_self";
					List<MenuOperate> mo = menuOperateService.getByCode(powerCode, cid);
					for (MenuOperate im : mo) {
						if (im.getPowerFlag()) {
							break out;
						}
					}
				}
				return powerCode;
			}
		} else {
			return "";
		}
		return "";
	}

	private void checkEchartsIsNull(ExtraView extraView) {
		String config = extraView.getConfig();
		if (config.indexOf("<echartCode>") > 0) {
			List<String> echartsCodes = new ArrayList<String>();
			Matcher m = pattern.matcher(config);
			while (m.find()){
				echartsCodes.add(m.group(1));
			}
//			for (String echartsCode : echartsCodes) {
//				Echarts echarts = echartsService.findEchartsByCode(echartsCode);
//				if (echarts == null) {
//					String layout = null;
//					if (echartsCode.lastIndexOf("_") > 0) {
//						layout = echartsCode.substring(echartsCode.lastIndexOf("_") + 1);
//					}
//					throw new BAPException(BAPException.Code.VIEW_NO_ECHARTS_ERROR, layout);
//				}
//			}
		}
	}

	/**
	 * @Description: 复制视图时复制图表
	 *
	 * @param: 参数描述
	 * @return: 返回结果描述
	 *
	 * @author: huning
	 * @date: 2019年4月8日 下午2:29:12
	 */
//	private void copyEchartsByViewCode(String viewCode, String copyViewCode) {
//		List<Echarts> echartsList = echartsService.findEchartsListByViewCode(viewCode, true);
//		echartsService.copyEcharts(echartsList, viewCode, copyViewCode);
//	}

	/**
	 * 将视图的xml转换为json
	 * @param view
	 */
	@Override
	@Transactional
	public void saveViewJson(View view){
		//先从数据库获取数据，防止乐观锁
		view =viewDao.load(view.getCode());
		if(view.getMobile() && view.getMobileEnableFlag()){//移动视图
			if(view.getType() == ViewType.LIST|| view.getType() == ViewType.REFERENCE){
				this.convertExtra2MobileList(view);
			} else if(view.getType() == ViewType.EXTRA){
				//this.convertExtra2MobileList(view);
			} else if(view.getType() == ViewType.EDIT||view.getType() == ViewType.VIEW){
				this.convertEdit2MobileEdit(view);
			}
		} else {//普通视图的xml转json
			if((view.getType() == ViewType.LIST || view.getType() == ViewType.REFERENCE)&& view.getShowType() != ShowType.LAYOUT2){
				this.convertList2ListJson(view, null);
			}else if((view.getType() == ViewType.LIST || view.getType() == ViewType.REFERENCE) && view.getShowType() == ShowType.LAYOUT2){
				this.convertListLayout2Json(view);
			}else if(view.getType() == ViewType.TREE){
				this.convertTree2Json(view, null);
			} else if(view.getType() == ViewType.EDIT || view.getType() == ViewType.VIEW || view.getType() == ViewType.EXTRA){
				this.convertEdit2EditJson(view);
			}
		}
		if(null != dataLocal){
			dataLocal.remove();
		}
	}





	/**
	 * 增强型视图xml转移动列表json
	 * @param view
	 */
	private void convertExtra2MobileList(View view){
		String extraConfig = ecConfigService.getEcFullConfig(view);
		String fastQeuryConfig = null;
		if(null != view.getFastQueryJson() && !view.getFastQueryJson().isEmpty()){
			fastQeuryConfig = view.getFastQueryJson().get(0).getQueryConfig();
		}
		List<DataGroup> dataGroups = dataGroupDao.findByCriteria(Restrictions.eq("valid", true),Restrictions.eq("view", view));
		String viewJSON = convertExtra2MobileList(extraConfig, fastQeuryConfig, dataGroups);
		view.getExtraView().setViewJson(viewJSON);
		viewDao.saveExtraView(view.getExtraView());
	}

	private String convertList2MobileList(View view){

		return "";
	}

	private String convertEdit2MobileEdit(View view){
		String extraConfig = ecConfigService.getEcFullConfig(view);
		List<DataGrid> dataGrids = dataGridDao.findByCriteria(Restrictions.eq("valid", true),Restrictions.eq("view", view));
		//List<DataGroup> dataGroups = dataGroupDao.findByCriteria(Restrictions.eq("valid", true),Restrictions.eq("view", view));
		String viewJSON = convertEdit2MobileEdit(extraConfig, dataGrids,view.getType());
		view.getExtraView().setViewJson(viewJSON);
		//view.getExtraView().setViewJson(viewJSON);
		viewDao.saveExtraView(view.getExtraView());
		return "";
	}

	/**
	 * 将移动编辑视图xml转为布局json
	 * @param extraConfig
	 * @param
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String convertEdit2MobileEdit(String extraConfig, List<DataGrid> dataGrids, ViewType viewType) {
		Map map = (Map) XmlUtils.convert(extraConfig);
		JSONSerializer serializer = new JSONSerializer();
		Map<String, String> pageConfig = new HashMap<String, String>();
		Map layout = (Map) map.get("layout");
		List<Map<String,Object>> mobileJsonMapList = new ArrayList<>();
		Map<String,Object> tabJsonMap = new HashMap<String, Object>();
		if(null != layout){
			pageConfig = (Map<String, String>) layout.get("pageConfig");
			if(viewType== ViewType.EDIT){
				pageConfig.put("regionType", "MOBILE-EDIT");
			}else if(viewType== ViewType.VIEW){
				pageConfig.put("regionType", "MOBILE-VIEW");
			}
			List tabs = (List) layout.get("tabs");
			if(null != tabs&!tabs.isEmpty()){
				for (Object tab : tabs) {
					Map<String,Object> mobileJsonMap = new HashMap<String, Object>();
					List<Map<String, Object>> elementList = new ArrayList<Map<String,Object>>();
					List<Map<String, Object>> datagridList = new ArrayList<Map<String,Object>>();
					//Map<String,Object> tab = (Map<String, Object>) tabs.get(0);
					List<Map> sections = (List) ((Map)tab).get("sections");
					if(null != sections){
						Map cellsMap = (Map) sections.get(0);
							if(null != cellsMap){
								elementList = (List<Map<String, Object>>) cellsMap.get("cells");
								if(null != elementList && !elementList.isEmpty()){
									for(Map<String,Object> elementMap : elementList){
										Map<String,Object> element = (Map<String, Object>) elementMap.get("element");
										if(null!=element){
											element.put("isrefselect", false);
											if(null != element.get("columnType") && "DATAGRID".equals(element.get("columnType"))){
												datagridList.add(convertDataGrid2MobileDataGrid(element));
												element = this.replaceData(element,true);
											}else if(null != element.get("columnType") && "SYSTEMCODE".equals(element.get("columnType"))){
												element = this.replaceData(element,true);
												String propertyCode=(String)element.get("propertyCode");
												if(null!=propertyCode){
												String[] propCodes=	propertyCode.split("\\|\\|");
												String propCode=propCodes[propCodes.length-1];
												Property property=	propertyDao.get(propCode);
												element.put("isTreeSystemCode", modelService.checkWhetherIsTreeSystemCode(property));
												element.put("multable",property.getMultable());
												//element.put("isOnlyLeaf", property.getOnlyLeaf());
												}
											}else{
												element = this.replaceData(element,true);
												String propertyCode=(String)element.get("propertyCode");
												if(null!=propertyCode){
													String[] propCodes=	propertyCode.split("\\|\\|");
													String propCode=propCodes[propCodes.length-1];
													Property property=	propertyDao.get(propCode);
													String columnType = (String) element.get("columnType");
													if(null != property){
														String dbColumnType = property.getType().name();
														String columnName = property.getColumnName();
														element.put("dbColumnType", dbColumnType);
														element.put("columnName", columnName);
														if(null != columnType && "LONG".equals(columnType)){
															element.put("maxValue", Long.MAX_VALUE + "");
															element.put("minValue", Long.MIN_VALUE + "");
														}else if(null != columnType && "INTEGER".equals(columnType)){
															element.put("maxValue", Integer.MAX_VALUE + "");
															element.put("minValue", Integer.MIN_VALUE + "");
														}else if(null != columnType && !"LONGTEXT".equals(columnType)){
															element.put("maxLength", getPropertyMaxLength(property));
														}
													}
												}
											}
										}
									}
								}
							}
							pageConfig.putAll((Map<String, String>) cellsMap.get("pageConfig"));
					}
					mobileJsonMap.put("tabName", InternationalResource.get((String)((Map)tab).get("namekey")));
					mobileJsonMap.put("tabCode", ((Map)tab).get("tabCode"));
					mobileJsonMap.put("dataGrid", datagridList);
					mobileJsonMap.put("elements", elementList);
					mobileJsonMap.put("pageConfig", pageConfig);
					mobileJsonMapList.add(mobileJsonMap);
				}
			}
		}
		tabJsonMap.put("tab", mobileJsonMapList);
		String json = serializer.deepSerialize(tabJsonMap);
		return json;
	}

	/**
	 * 将datagrid xml转为布局json
	 * @param
	 * @param
	 * @return
	 */
	private Map<String, Object> convertDataGrid2MobileDataGrid(Map<String,Object> elementmap) {
		Map<String, Object> dataGridMap = new HashMap<String, Object>();
		Map<String, Object> dataGridConfig = new HashMap<String, Object>();
		List<Map<String, Object>> elementList = new ArrayList<Map<String,Object>>();
		List<Map<String, Object>> buttons = new ArrayList<Map<String,Object>>();

		DataGrid dataGrid = dataGridDao.findEntityByCriteria(Restrictions.eq("valid", true),Restrictions.eq("code", elementmap.get("DataGridCode")));
		dataGridMap.put("config", elementmap);
		dataGridConfig =dataGridService.getDataGridFullConfigMap(dataGrid);

		if (null != dataGridConfig) {
			Map layout = (Map) dataGridConfig.get("layout");
			if (null != layout) {
				List<Map> sections = (List) layout.get("sections");
				if (null != sections) {
					for (Map cellsMap : sections) {
						if (null != cellsMap) {
							elementList = (List<Map<String, Object>>) cellsMap.get("cells");
							if (null != elementList && !elementList.isEmpty()) {
								for (Map<String, Object> elementMap : elementList) {
									// Map<String,Object> element = (Map<String,
									// Object>) elementMap.get("element");
									if (null != elementMap) {
										elementMap.put("isrefselect", false);
										elementMap = this.replaceData(elementMap,true);
										if(null != elementMap.get("columnType") && "SYSTEMCODE".equals(elementMap.get("columnType"))){
											String propertyCode=(String)elementMap.get("propertyCode");
											if(null!=propertyCode){
											String[] propCodes=	propertyCode.split("\\|\\|");
											String propCode=propCodes[propCodes.length-1];
											Property property=	propertyDao.get(propCode);
											elementMap.put("isTreeSystemCode", modelService.checkWhetherIsTreeSystemCode(property));
											elementMap.put("multable",property.getMultable());
											//elementMap.put("isOnlyLeaf", property.getOnlyLeaf());
											}
										}else{
											String propertyCode=(String)elementMap.get("propertyCode");
											String columnType = (String) elementMap.get("columnType");
											if(null!=propertyCode){
												String[] propCodes=	propertyCode.split("\\|\\|");
												String propCode=propCodes[propCodes.length-1];
												Property property=	propertyDao.get(propCode);
												if(null == property){
													elementMap.put("dbColumnType", null);
													elementMap.put("columnName", null);
													elementMap.put("maxLength", null);
												}else{
													String dbColumnType = property.getType().name();
													String columnName = property.getColumnName();
													elementMap.put("dbColumnType", dbColumnType);
													elementMap.put("columnName", columnName);
													if(null != columnType && "LONG".equals(columnType)){
														elementMap.put("maxValue", Long.MAX_VALUE + "");
														elementMap.put("minValue", Long.MIN_VALUE + "");
													}else if(null != columnType && "INTEGER".equals(columnType)){
														elementMap.put("maxValue", Integer.MAX_VALUE + "");
														elementMap.put("minValue", Integer.MIN_VALUE + "");
													}else if(null != columnType && !"LONGTEXT".equals(columnType)){
														elementMap.put("maxLength", getPropertyMaxLength(property));
													}
													if(null != columnType && "SYSTEMCODE".equals(columnType)){
														elementMap.put("multable", property.getMultable());
													}
												}
											}
										}
										if ("BUTTON".equals(elementMap.get("regionType"))) {
											buttons.add(elementMap);
										}
									}
								}
							}
						}
					}
				}
				Map<String, Object> listProperty=(Map<String, Object>)layout.get("listProperty");
				if(null!=listProperty){
					dataGridMap.put("isEditable", listProperty.get("isEditable"));
					dataGridMap.put("renderOver", listProperty.get("renderOver"));
					dataGridMap.put("ptPageInit", listProperty.get("ptPageInit"));
					dataGridMap.put("isTransverseShow", listProperty.get("isTransverseShow"));
				}
			}
		}
		dataGridMap.put("targetModel", dataGrid.getTargetModel().getCode());
		dataGridMap.put("elements", elementList);
		dataGridMap.put("buttons", buttons);
		dataGridMap.put("datagridName", InternationalResource.get(dataGrid.getDataGridName()));
		dataGridMap.put("name", dataGrid.getName());
		JSONSerializer serializer = new JSONSerializer();
		String json = serializer.deepSerialize(dataGridMap);
		dataGrid.setDataGridJson(json);
		dataGridDao.save(dataGrid);
		return dataGridMap;
	}

	/**
	 * 将增强型视图xml转为移动列表的布局json
	 * @param extraConfig
	 * @param fastQeuryConfig
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String convertExtra2MobileList(String extraConfig,String fastQeuryConfig,List<DataGroup> dataGroups) {
		Map map = (Map) XmlUtils.convert(extraConfig);
		JSONSerializer serializer = new JSONSerializer();
		Map<String, String> pageConfig = new HashMap<String, String>();
		List<Map<String, Object>> buttonList = new ArrayList<Map<String,Object>>();
		List<Map<String, Object>> elementList = new ArrayList<Map<String,Object>>();
		Map layout = (Map) map.get("layout");
		Map<String,Object> mobileJsonMap = new HashMap<String, Object>();
		if(null != layout){
			List buttonSections = (List) ((Map)layout).get("sections");
			if(null != buttonSections){
				Map cellsMap = (Map) buttonSections.get(0);
				if(null != cellsMap){
					buttonList = (List<Map<String, Object>>) cellsMap.get("cells");
					if(null != buttonList && !buttonList.isEmpty()){
						for(Map<String,Object> buttonMap : buttonList){
							buttonMap=this.replaceData(buttonMap,true);
							Boolean isPerssion =  (Boolean) buttonMap.get("ispermission");
							if(isPerssion != null && isPerssion){
								String operateCode = (String) buttonMap.get("buttonoperationcode");
								buttonMap.put("pc", this.getPowerCode(operateCode));
							}
						}
					}
				}
				mobileJsonMap.put("buttons", buttonList);
			}
			pageConfig = (Map<String, String>) layout.get("pageConfig");
			pageConfig.put("regionType", "MOBILE-LIST");
			List tabs = (List) layout.get("tabs");
			if(null != tabs){
				Map<String,Object> tab = (Map<String, Object>) tabs.get(0);
				List layout1 = (List) tab.get("layout");
				for(Object object : layout1){
					List sections = (List) ((Map)object).get("sections");
					if(null != sections){
						Map cellsMap = (Map) sections.get(0);
						if(null != cellsMap){
							elementList = (List<Map<String, Object>>) cellsMap.get("cells");
							if(null != elementList && !elementList.isEmpty()){
								for(Map<String,Object> elementMap : elementList){
									Map<String,Object> element = (Map<String, Object>) elementMap.get("element");
									if(null!=element){
										element = this.replaceData(element,true);
										String propertyCode = (String) element.get("propertyCode");
										if (null != propertyCode) {
											String[] propCodes = propertyCode.split("\\|\\|");
											String propCode = propCodes[propCodes.length - 1];
											Property property = propertyDao.get(propCode);
											if (null != element.get("columnType")&& "SYSTEMCODE".equals(element.get("columnType"))) {
												element.put("isTreeSystemCode",modelService.checkWhetherIsTreeSystemCode(property));
												element.put("multable",property.getMultable());
												//element.put("isOnlyLeaf", property.getOnlyLeaf());
											}
										}
									}
								}
							}
						}
						pageConfig.putAll((Map<String, String>) cellsMap.get("pageConfig"));
					}
				}
			}
		}
		mobileJsonMap.put("pageConfig", pageConfig);
		mobileJsonMap.put("list", elementList);

		Map fastQueryMap = (Map) XmlUtils.convert(fastQeuryConfig);
		if(null != fastQueryMap && null != fastQueryMap.get("fastQueryJson")){
			List<Map<String, Object>> sections = (List<Map<String, Object>>) ((Map) fastQueryMap.get("fastQueryJson")).get("sections");
			if(null != sections && !sections.isEmpty()){
				List<Map<String, Object>> queryElementList = (List<Map<String, Object>>) sections.get(0).get("cells");
				if(null != queryElementList && !queryElementList.isEmpty()){
					for(Map<String,Object> elementMap : queryElementList){
						Map<String,Object> element = (Map<String, Object>) elementMap.get("element");
						element.put("isrefselect", false);
						element = this.replaceData(element,true);
						String propertyCode=(String)element.get("propertyCode");
						if(null!=propertyCode){
						String[] propCodes=	propertyCode.split("\\|\\|");
						String propCode=propCodes[propCodes.length-1];
						Property property=	propertyDao.get(propCode);
						if(null!=property){
							element.put("columnName", property.getColumnName());
						}
						if(null != element.get("columnType") && "SYSTEMCODE".equals(element.get("columnType"))){
							element.put("isTreeSystemCode", modelService.checkWhetherIsTreeSystemCode(property));
							element.put("multable",property.getMultable());
							//element.put("isOnlyLeaf", property.getOnlyLeaf());
						}
						}
					}
				}
				mobileJsonMap.put("query", queryElementList);
			}
		}
		if(null != dataGroups && !dataGroups.isEmpty()){
			List dataclassifyList = new ArrayList();
			for(DataGroup dataGroup : dataGroups){
				Map<String,Object> dgMap = new HashMap<String, Object>();
				dgMap.put("code", dataGroup.getCode());
				dgMap.put("displayName", dataGroup.getDisplayName());
				dgMap.put("displayNameInternational", InternationalResource.get(dataGroup.getName()));
				dgMap.put("isMultiple", dataGroup.getIsMultiple());
				dgMap.put("name", dataGroup.getName());
				if(null != dataGroup.getDataClassifics() && !dataGroup.getDataClassifics().isEmpty()){
					Set<DataClassific> dataClassifics = dataGroup.getDataClassifics();
					List classifics = new ArrayList();
					for(DataClassific classific : dataClassifics){
						Map<String,Object> temp = new HashMap<String, Object>();
						temp.put("code", classific.getCode());
						temp.put("name", classific.getName());
						temp.put("isDefault", classific.getIsDefault());
						temp.put("displayName", classific.getDisplayName());
						temp.put("condition", classific.getCondition());
						classifics.add(temp);
					}
					dgMap.put("dataClassificLis", classifics);
				}
				if(!dgMap.isEmpty()){
					dataclassifyList.add(dgMap);
				}

			}
			if(!dataclassifyList.isEmpty()){
				mobileJsonMap.put("dataGroupProperty", dataclassifyList);
			}
		}
		String json = serializer.deepSerialize(mobileJsonMap);
		//System.out.println(json);
		return json;
	}

	/**
	 * 根据布局查找右边列表视图
	 * @param layoutView
	 * @return
	 */
	private View findListOfLayout(View layoutView){
		String extraConfig = ecConfigService.getEcFullConfig(layoutView);
		if(null == extraConfig || "".equals(extraConfig)){
			return null;
		}
		Map map = (Map) XmlUtils.convert(extraConfig);
		Map layout = (Map) map.get("layout");
		//右边的列表视图
		Map center = (Map) layout.get("center");
		String listViewCode = (String) center.get("vcode");
		View listView = this.getView(listViewCode);
		return listView;
	}


	/**
	 * 将xml中的视图code属性替换为视图完整属性Map
	 * @param elementMap
	 * @return
	 */
	@Deprecated
	private Map<String,Object> replaceData_old(Map<String,Object> elementMap,Boolean isMobile){
		String[] properties = new String[]{"viewview", "linkView", "allowviewcode","referenceview", "viewselect", "allowmultviewselect"};//"allowview"
		for(String pro :properties){
			if(elementMap.containsKey(pro)){
				Object value = elementMap.get(pro);
				if(value instanceof String){
					View view = viewDao.load(value.toString());
					if(null != view){
						String url = view.getUrl();
						if(elementMap.containsKey("isrefselect") && Boolean.parseBoolean(elementMap.get("isrefselect").toString())){
							if(view.getShowType() == ShowType.LAYOUT2 ){
								View listView = this.findListOfLayout(view);
								if(null == listView){
									elementMap.put("queryUrl", url + "-query");
								}else{
									elementMap.put("queryUrl", listView.getUrl() + "-query");
								}
							}else{
								elementMap.put("queryUrl", url + "-query");
							}
							if(view.getIsPermission() && null != view.getPermissionCode()){
								elementMap.put("permissionCode", view.getEntity().getCode() + "_" + view.getPermissionCode());
							}else{
								elementMap.put("permissionCode", null);
							}
							elementMap.put("iscrosscompany", null ==  elementMap.get("isgroup") ? "false" :  elementMap.get("isgroup").toString());
						}else{
							Map<String,Object> viewProMap = new HashMap<String, Object>();
							viewProMap.put("title", view.getTitle());
							viewProMap.put("code", view.getCode());
							viewProMap.put("name", view.getName());
							viewProMap.put("openType", view.getOpenType());
							viewProMap.put("url", url);
							//viewProMap.put("iscrosscompany", null ==  view.getEntity().getCrossCompanyFlag() ? "false" : view.getEntity().getCrossCompanyFlag().toString());
							viewProMap.put("iscrosscompany", null ==  elementMap.get("isgroup") ? "false" :  elementMap.get("isgroup").toString());
							if("referenceview".equals(pro)){
								if(isMobile){
									elementMap.put("isrefselect", true);
									if("sysbase_1.0_staff_ref".equals(view.getCode())){
										viewProMap.put("url", "/foundation/staff/common/staffRefvue.action");
									}else if("sysbase_1.0_department_deptref".equals(view.getCode())||"sysbase_1.0_department_departmentRefCustom".equals(view.getCode())){
										viewProMap.put("url", "/foundation/department/common/departmentRefvue.action");
									}else if("sysbase_1.0_position_position".equals(view.getCode())){
										viewProMap.put("url", "/foundation/position/common/positionRefvue.action");
									}else{
										viewProMap.put("url", view.getUrl());
									}
								}
								String[] urlSplitArr = url.split("/");
								if((url.indexOf("foundation") > 0 || url.indexOf("common") > 0) && urlSplitArr.length >= 3){
									viewProMap.put("mneType", StringUtils.firstLetterToUpper(urlSplitArr[2]));
								}else{
									viewProMap.put("mneType", "other");
								}
							}else if("viewselect".equals(pro)){
								String[] urlSplitArr = url.split("/");
								if((url.indexOf("foundation") > 0 || url.indexOf("common") > 0) && urlSplitArr.length >= 3){
									viewProMap.put("mneType", StringUtils.firstLetterToUpper(urlSplitArr[2]));
								}else{
									viewProMap.put("mneType", "other");
								}
							}else if("allowviewcode".equals(pro) || "allowmultviewselect".equals(pro) || "viewview".equals(pro) || "linkView".equals(pro)){
								View mainListView = getMainListView(view.getEntity());
								if(null != mainListView){
									viewProMap.put("pc", getPowerCode(mainListView.getCode() + "_self"));
								}
							}else{
								if(isMobile){
									viewProMap.put("url", view.getUrl());
								}
							}
							if(null != view.getOpenType() && "dialog".equals(view.getOpenType()) && "viewselect".equals(pro)){
								viewProMap.put("width", view.getWidth());
								viewProMap.put("height", view.getHeight());
								List<Button> buttons = buttonDao.findByProperty("view", view);
								List<Map<String, Object>> viewSelectButtons = new ArrayList<Map<String,Object>>();
								for(Button button: buttons){
									Map<String, Object> buttonMap = new HashMap<String, Object>();
									if(button.getConfigMap() !=null){
										buttonMap.putAll((Map<String, Object>) button.getConfigMap().get("button"));
									}else if(button.getOperateType()== OperateType.PRINT){
										List<Map<String, Object>> printButtonMap = convertPrintButton2Json(view,null);
										buttonMap.putAll(printButtonMap.get(0));
										buttonMap.put("namekey", buttonMap.get("displayname"));
									}
									if(buttonMap.get("namekey")==null){
										buttonMap.put("namekey", buttonMap.get("displayName"));
									}
									buttonMap.put("id", buttonMap.get("name"));
									if(null != buttonMap.get("isPermission") && null != buttonMap.get("permissionCode")
											&& Boolean.valueOf(buttonMap.get("isPermission").toString())){
										String buttonPermissionCode = view.getEntity().getCode() + "_" + buttonMap.get("permissionCode").toString();
										buttonMap.put("pc", getPowerCode(buttonPermissionCode));
									}
									viewSelectButtons.add(buttonMap);
								}
								viewProMap.put("viewSelectButtons", viewSelectButtons);
							}else if(null != view.getOpenType() && "dialog".equals(view.getOpenType())){
								viewProMap.put("width", view.getWidth());
								viewProMap.put("height", view.getHeight());
							}
							if("viewselect".equals(pro)){
								if(view.getAssModel().getIsMain() && view.getEntity().getWorkflowEnabled()){
									viewProMap.put("url", view.getUrl() + "?superEdit=true");
								}
							}
							elementMap.put(pro,viewProMap);
						}
					}

				}
				//break;
			}

		}
		//系统编码类型
		String columnType = (String) elementMap.get("columnType");
		String showType = (String) elementMap.get("showType");
		if(null != columnType && "SYSTEMCODE".equals(columnType) && null != showType && !"LABEL".equals(showType)){
			Map<String, Object> fillMap = (Map<String, Object>) elementMap.get("fill");
			if (fillMap != null) {
				Object fillContent = fillMap.get("fillContent");
				if(fillContent instanceof String){//TODO
					String sql = "SELECT LIST_TYPE FROM BASE_SYSTEMENTITY WHERE CODE = ? AND VALID = 1";
					List<Map<String, Object>> listType = jdbcTemplate.queryForList(sql, fillContent.toString());
					if(null != listType && listType.size() > 0){
						fillMap.put("listType", listType.get(0).get("LIST_TYPE"));
					}
				}
			}
		}
		return elementMap;
	}

	/**
	 * 将xml中的视图code属性替换为视图完整属性Map
	 * @param elementMap
	 * @return
	 */
	private Map<String,Object> replaceData(Map<String,Object> elementMap,Boolean isMobile){
		String[] properties = new String[]{"viewview", "linkView", "allowviewcode","referenceview", "viewselect", "allowmultviewselect"};//"allowview"
		for(String pro :properties){
			if(elementMap.containsKey(pro)){
				Object value = elementMap.get(pro);
				if(value instanceof String){
					View view = viewDao.load(value.toString());
					if(null != view){
						String url = view.getUrl();
						if(elementMap.containsKey("isrefselect") && Boolean.parseBoolean(elementMap.get("isrefselect").toString())){
							if(view.getShowType() == ShowType.LAYOUT2 ){
								View listView = this.findListOfLayout(view);
								if(null == listView){
									elementMap.put("queryUrl", url + "-query");
								}else{
									elementMap.put("queryUrl", listView.getUrl() + "-query");
								}
							}else{
								elementMap.put("queryUrl", url + "-query");
							}
							if(view.getIsPermission() && null != view.getPermissionCode()){
								elementMap.put("permissionCode", view.getEntity().getCode() + "_" + view.getPermissionCode());
							}else{
								elementMap.put("permissionCode", null);
							}
							elementMap.put("iscrosscompany", null ==  elementMap.get("isgroup") ? "false" :  elementMap.get("isgroup").toString());
						}else{
							Map<String,Object> viewProMap = new HashMap<String, Object>();
							viewProMap.put("title", view.getTitle());
							viewProMap.put("code", view.getCode());
							viewProMap.put("name", view.getName());
							viewProMap.put("openType", view.getOpenType());
							viewProMap.put("url", url);
							//viewProMap.put("iscrosscompany", null ==  view.getEntity().getCrossCompanyFlag() ? "false" : view.getEntity().getCrossCompanyFlag().toString());
							viewProMap.put("iscrosscompany", null ==  elementMap.get("isgroup") ? "false" :  elementMap.get("isgroup").toString());
							if("referenceview".equals(pro)){
								if(isMobile){
									elementMap.put("isrefselect", true);
									if("sysbase_1.0_staff_ref".equals(view.getCode())){
										viewProMap.put("url", "/greenDill/mobile-static/staff.html");
									}else if("sysbase_1.0_department_deptref".equals(view.getCode())||"sysbase_1.0_department_departmentRefCustom".equals(view.getCode())){
										viewProMap.put("url", "/greenDill/mobile-static/department.html");
									}else if("sysbase_1.0_position_position".equals(view.getCode())){
										viewProMap.put("url", "/greenDill/mobile-static/position.html");
									}else{
										viewProMap.put("url", view.getUrl());
									}
								}
								if (url.startsWith("/organization/#/reference?type=")) {
									viewProMap.put("mneType", StringUtils.firstLetterToUpper(url.split("type=")[1]));
									// 基础助记码默认跨公司
									viewProMap.put("iscrosscompany", true);
								} else {
									viewProMap.put("mneType", "other");
								}
							}else if("viewselect".equals(pro)){
								if (url.startsWith("/organization/#/reference?type=")) {
									viewProMap.put("mneType", StringUtils.firstLetterToUpper(url.split("type=")[1]));
								} else {
									viewProMap.put("mneType", "other");
								}
							}else if("allowviewcode".equals(pro) || "allowmultviewselect".equals(pro) || "viewview".equals(pro) || "linkView".equals(pro)){
								View mainListView = getMainListView(view.getEntity());
								if(null != mainListView){
									viewProMap.put("pc", getPowerCode(mainListView.getCode() + "_self"));
								}
							}else{
								if(isMobile){
									viewProMap.put("url", view.getUrl());
								}
							}
							if(null != view.getOpenType() && "dialog".equals(view.getOpenType()) && "viewselect".equals(pro)){
								viewProMap.put("width", view.getWidth());
								viewProMap.put("height", view.getHeight());
								List<Button> buttons = buttonDao.findByProperty("view", view);
								List<Map<String, Object>> viewSelectButtons = new ArrayList<Map<String,Object>>();
								for(Button button: buttons){
									Map<String, Object> buttonMap = new HashMap<String, Object>();
									if(button.getConfigMap() !=null){
										buttonMap.putAll((Map<String, Object>) button.getConfigMap().get("button"));
									}else if(button.getOperateType()==OperateType.PRINT){
										List<Map<String, Object>> printButtonMap = convertPrintButton2Json(view, null);
										buttonMap.putAll(printButtonMap.get(0));
										buttonMap.put("namekey", buttonMap.get("displayname"));
									}
									if(buttonMap.get("namekey")==null){
										buttonMap.put("namekey", buttonMap.get("displayName"));
									}
									buttonMap.put("id", buttonMap.get("name"));
									if(null != buttonMap.get("isPermission") && null != buttonMap.get("permissionCode")
											&& Boolean.valueOf(buttonMap.get("isPermission").toString())){
										String buttonPermissionCode = view.getEntity().getCode() + "_" + buttonMap.get("permissionCode").toString();
										buttonMap.put("pc", getPowerCode(buttonPermissionCode));
									}
									// 统一用小写ispermission
									if (buttonMap.containsKey("isPermission")) {
										buttonMap.put("ispermission", buttonMap.get("isPermission"));
										buttonMap.remove("isPermission");
									}
									viewSelectButtons.add(buttonMap);
								}
								viewProMap.put("viewSelectButtons", viewSelectButtons);
							}else if(null != view.getOpenType() && "dialog".equals(view.getOpenType())){
								viewProMap.put("width", view.getWidth());
								viewProMap.put("height", view.getHeight());
							}
							if("viewselect".equals(pro)){
								if(view.getAssModel().getIsMain() && view.getEntity().getWorkflowEnabled()){
									viewProMap.put("url", view.getUrl() + "?superEdit=true");
								}
							}
							elementMap.put(pro,viewProMap);
						}
					}

				}
				//break;
			}

		}
		//系统编码类型
		String columnType = (String) elementMap.get("columnType");
		String showType = (String) elementMap.get("showType");
		if(null != columnType && "SYSTEMCODE".equals(columnType) && null != showType && !"LABEL".equals(showType)){
			Map<String, Object> fillMap = (Map<String, Object>) elementMap.get("fill");
			Object fillContent = fillMap.get("fillContent");
			if(fillContent instanceof String){//TODO
				String sql = "SELECT LIST_TYPE FROM BASE_SYSTEMENTITY WHERE CODE = ? AND VALID = 1";
				List<Map<String, Object>> listType = jdbcTemplate.queryForList(sql, fillContent.toString());
				if(null != listType && listType.size() > 0){
					fillMap.put("listType", listType.get(0).get("LIST_TYPE"));
				}
			}
		}
		return elementMap;
	}

	/**
	 * 控件打印按钮，批量控件打印按钮转json
	 * @param view
	 * @return
	 */
	private List<Map<String,Object>> convertPrintButton2Json(View view, String parentViewCode){
		Map<String, Object> printMap = new HashMap<String,Object>();
		List<Map<String,Object>> printButtons = new ArrayList<Map<String,Object>>();
		List<Map<String,Object>> printButtonsList = new ArrayList<Map<String,Object>>();
		if((view.getControlPrint() !=null && view.getControlPrint())||(view.getIsBatchControlPrint() !=null && view.getIsBatchControlPrint())){
//			printMap.put("controlPrint",view.getControlPrint());
			Button printButton=buttonService.getButton(view.getCode()+"_print_print");
			Button batchPrintButton=buttonService.getButton(view.getCode()+"_print_batchPrint");
			Button batchPrintPreviewButton=buttonService.getButton(view.getCode()+"_print_batchPrint_preview");
			if(batchPrintButton !=null){
				batchPrintButton.setButtonStyle("dy");
				printButtons.add(MapToEntityUtils.beanToMap(batchPrintButton) );
			}
			if(batchPrintPreviewButton != null){
				batchPrintPreviewButton.setButtonStyle("dyyl");
				printButtons.add(MapToEntityUtils.beanToMap(batchPrintPreviewButton));
			}
			if(printButton !=null){
				printButton.setDisplayName((null == view.getControlName()) ? "ec.print.controlPrint" : view.getControlName());
				printButtons.add(MapToEntityUtils.beanToMap(printButton));
			}
			for(Map<String, Object> buttonMap: printButtons){
				Map<String, Object> buttonResultMap = new HashMap<String, Object>();
				Object isCustomFunc = buttonMap.get("isCustomFunc");
				if(buttonMap.containsKey("operateType") && isCustomFunc != null && (Boolean)isCustomFunc){
					Map eventMap = new HashMap<String, Object>();
					eventMap.put("name", buttonMap.get("id"));
					eventMap.put("function", buttonMap.get("funcbody"));
					eventMap.put("function_es5", buttonMap.get("function_es5"));
					buttonMap.put("events", eventMap);
				}else if(buttonMap.containsKey("operateType") && !(Boolean)buttonMap.get("isCustomFunc")){
						buttonMap = this.replaceData(buttonMap,false);
						String buttonPermissionCode="";
						if (buttonMap.get("operateType") !=null && buttonMap.get("operateType").equals(OperateType.PRINT) ){
							buttonPermissionCode = view.getCode() + "_controlPrint";
						}else if (buttonMap.get("operateType") !=null && buttonMap.get("operateType").equals(OperateType.BATCH_PRINT) ){
							buttonPermissionCode = view.getCode() + "_batch_controlPrint";
						}else if (buttonMap.get("operateType") !=null && buttonMap.get("operateType").equals(OperateType.BATCH_PRINT_PREVIEW) ){
							buttonPermissionCode = view.getCode() + "_batch_controlPrint";
						}
						buttonMap.put("pc", getPowerCode(buttonPermissionCode));
				}
				buttonResultMap=transformPrintButtonMap(buttonMap);
				String printUrl = "" ;
				if(buttonMap.get("operateType") !=null && buttonMap.get("operateType").equals(OperateType.PRINT)){
					printUrl = "/"+view.getEntity().getModule().getArtifact()+"/"+view.getEntity().getEntityName()+"/"+toLowerCaseFirstOne(view.getAssModel().getModelName())+"/printOnServer";
				}else if(buttonMap.get("operateType") !=null && (buttonMap.get("operateType").equals(OperateType.BATCH_PRINT)||buttonMap.get("operateType").equals(OperateType.BATCH_PRINT_PREVIEW))){
					printUrl = "/"+view.getEntity().getModule().getArtifact()+"/"+view.getEntity().getEntityName()+"/"+toLowerCaseFirstOne(view.getAssModel().getModelName())+"/batchPrintOnServer";
				}
				buttonResultMap.put("printUrl",printUrl);
				printButtonsList.add(buttonResultMap);
			}
		}
		return printButtonsList;
	}
	//首字母转小写
	public static String toLowerCaseFirstOne(String s){
		if(Character.isLowerCase(s.charAt(0))) {
			return s;
		} else {
			return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
		}
	}

	/**
	 * 将打印按钮map转换为与普通按钮一致
	 * @param
	 * @return
	 */
	@SuppressWarnings("AliEqualsAvoidNull")
	private Map<String, Object> transformPrintButtonMap(Map<String, Object> buttonMap){
		Map<String, Object> resultMap = new HashMap<>();
		Set<String> keySet = buttonMap.keySet();
		for (String key : keySet){
			if("view".equals(key)){
				continue;
			}else if ("isPublished".equals(key) || "isSignatureConfig".equals(key) || "regionType".equals(key) || "useInMore".equals(key) || "ecEnv".equals(key)){
				resultMap.put(key,buttonMap.get(key));
			}else{
				resultMap.put(key.toLowerCase(),buttonMap.get(key));
			}
		}
		log.debug("displayName:"+buttonMap.get("displayName"));
		resultMap.put("namekey", buttonMap.get("displayName"));
		resultMap.put("id", buttonMap.get("name"));
		return resultMap;
	}

	/**
	 * 增强型视图xml转列表json
	 * @param view
	 */
	private void convertTree2Json(View view, String parentViewCode) {
		// TODO Auto-generated method stub
		String extraConfig = ecConfigService.getEcFullConfig(view);
		if(extraConfig == null || "".equals(extraConfig)) {
			return ;
		}
		Map map = (Map) XmlUtils.convert(extraConfig);
		JSONSerializer serializer = new JSONSerializer();
		log.debug("TreeJson:" + serializer.deepSerialize(map));
		Map<String, Object> result = new HashMap<String, Object>();
		Map layoutMap = (Map) map.get("layout");
		result.put("type", "layoutTree");
		result.put("layoutmethod", "container");
		//result.put("fix_w", 200);
		result.put("layoutCode", layoutMap.get("layoutCode"));
		result.put("hasAttachment", view.getHasAttachment());
		if(layoutMap.containsKey("pageConfig")){
			Map<String, Object> pageConfigMap = (Map<String, Object>) layoutMap.get("pageConfig");
			//页面事件
			List<Map<String,Object>> events = new ArrayList<Map<String,Object>>();
			String funcbody = null;
			String funcname = null;
			String funcbody_es5 = null;
			String pattern = "function (\\w+\\([\\w,]*\\))";
 			for(String key: pageConfigMap.keySet()){
				if("ondbclickMethod".equals(key) || "onclickMethod".equals(key)){
					String tempFuncbody =  pageConfigMap.get(key).toString();
					if(null == funcbody){
						funcbody = tempFuncbody;
					}else{
						funcbody = funcbody + "@@@@" + tempFuncbody;
					}

					String tempFuncname = null;
					Pattern compile = Pattern.compile(pattern);
					Matcher matcher = compile.matcher(tempFuncbody);
					if (matcher.find()) {
						tempFuncname = key + "='"+ matcher.group(1) + "'";
						if(null == funcname){
							funcname = tempFuncname;
						}else{
							funcname = funcname + " " + tempFuncname;
						}
					}
				} else if ("ondbclickMethod_es5".equals(key) || "onclickMethod_es5".equals(key)) {
					String tempFuncbody_es5 =  (String) pageConfigMap.get(key);
					if (null == funcbody_es5) {
						funcbody_es5 = tempFuncbody_es5;
					} else {
						funcbody_es5 = funcbody_es5 + "@@@@" + tempFuncbody_es5;
					}
				} else {
					result.put(key, pageConfigMap.get(key));
				}
			}
			result.put("funcbody", funcbody);
			result.put("funcbody_es5", funcbody_es5);
			result.put("funcname", funcname);
			result.put("events", events);
		}
		//解析按钮
		if(layoutMap.containsKey("sections")){
			List<Map> sectionMaps = (List<Map>) layoutMap.get("sections");
			if(sectionMaps != null && sectionMaps.size() > 0){
				Map sectionMap = sectionMaps.get(0);
				String regionType = (String) sectionMap.get("regionType");
				if(null != regionType && "BUTTON".equals(regionType)){
					List<Map> buttonMaps = (List<Map>) sectionMap.get("cells");
					for(Map<String, Object> buttonMap: buttonMaps){
						if(buttonMap.containsKey("operatetype") && (Boolean)buttonMap.get("iscustomfunc")){
							Map eventMap = new HashMap<String, Object>();
							eventMap.put("name", buttonMap.get("id"));
							eventMap.put("function", buttonMap.get("funcbody"));
							eventMap.put("function_es5", buttonMap.get("funcbody_es5"));
							buttonMap.put("events", eventMap);
						}else if(buttonMap.containsKey("operatetype") && !(Boolean)buttonMap.get("iscustomfunc")){
							buttonMap = this.replaceData(buttonMap,false);
						}
						Boolean isPerssion =  (Boolean) buttonMap.get("ispermission");
						if(isPerssion != null && isPerssion){
							String operateCode = (String) buttonMap.get("buttonoperationcode");
							if(parentViewCode != null){
								operateCode = parentViewCode + "_" + operateCode;
							}
							buttonMap.put("pc", this.getPowerCode(operateCode));
						}else {
							buttonMap.put("pc", this.getPowerCode(parentViewCode + "_self"));
						}
					}
					result.put("buttons", buttonMaps);
				}
			}
		}

		Map<String, Object> layout = new HashMap<String, Object>();
		List<Map> layoutComponents = new ArrayList<Map>();
		layoutComponents.add(result);
		layout.put("type", "layout");
		layout.put("layoutmethod", "column");
		layout.put("title", view.getTitle());
		layout.put("components", layoutComponents);
		String json = serializer.deepSerialize(layout);
		view.getExtraView().setViewJson(json);
	}



	/**
	 * 列表视图的xml转列表json
	 * @param view
	 */
	private void convertList2ListJson(View view, String parentViewCode) {
		String extraConfig = ecConfigService.getEcFullConfig(view);
		if(null == extraConfig || "".equals(extraConfig)){
			return ;
		}
		List<DataGroup> dataGroups = dataGroupDao.findByCriteria(Restrictions.eq("valid", true),Restrictions.eq("view", view));
		Map map = (Map) XmlUtils.convert(extraConfig);
		JSONSerializer serializer = new JSONSerializer();
		Map<String, String> pageConfig = new HashMap<String, String>();//TODO
		List<Map<String, Object>> elementList = new ArrayList<Map<String,Object>>();
		Map<String, Object> elementListPT = new HashMap<String, Object>();
		Map layoutTemp = (Map) map.get("layout");
		Map<String,Object> listJsonMap = new HashMap<String, Object>();
		Map<String, Object> layout = new HashMap<String, Object>();
		listJsonMap.put("pageType", "LIST");
		listJsonMap.put("title", view.getTitle());
		listJsonMap.put("url", view.getUrl());
		listJsonMap.put("isMain", view.getAssModel().getIsMain());
		listJsonMap.put("hasAttachment", view.getHasAttachment());
		listJsonMap.put("onlyForQuery", view.getOnlyForQuery());
		//查询的配置组织
		Map<String,Object> searchComponent = new HashMap<String, Object>();
		searchComponent.put("type", "layoutSearchWidget");
		searchComponent.put("code", "query");
		searchComponent.put("layoutName", "layoutSearchWidget");
		searchComponent.put("layoutmethod", "container");
		searchComponent.put("fix_h", 150);
		//表格配置
		Map<String,Object> datagridComponent = new HashMap<String, Object>();
		datagridComponent.put("type", "layoutDatagrid");
		datagridComponent.put("layoutmethod", "container");
		datagridComponent.put("ratio_h", 100);
		datagridComponent.put("modelCode", view.getAssModel().getCode());
		datagridComponent.put("hasFastQuery", true);
		Property mainDisplayProperty = modelService.findMainDisplayProperty(view.getAssModel());
		datagridComponent.put("mainDisplayName", mainDisplayProperty.getName());

		String nameSpace = "/" + view.getEntity().getModule().getArtifact()
				+ "/" + view.getEntity().getEntityName()
				+ "/" + StringUtils.firstLetterToLower(view.getAssModel().getModelName()) + "/";
		datagridComponent.put("downloadXls", "/msService" + nameSpace + "downloadXls");
		datagridComponent.put("importMainXls", "/msService" + nameSpace + "importMainXls");
		String idPrefix = "ec_" + view.getEntity().getModule().getArtifact() + "_" + view.getEntity().getEntityName()
				+ "_" + StringUtils.firstLetterToLower(view.getAssModel().getModelName()) + "_" + view.getName();
		datagridComponent.put("idPrefix", idPrefix);
		if(null != layoutTemp){
			pageConfig = (Map<String, String>) layoutTemp.get("pageConfig");
			if(pageConfig == null) {
				return ;
			}
			List<Map<String,Object>> events = new ArrayList<Map<String,Object>>();
			for(String key: pageConfig.keySet()){
				if("onload".equals(key)){
					Map<String, Object> eventMap = new HashMap<String, Object>();
					eventMap.put("name", key);
					eventMap.put("function", pageConfig.get(key));
					events.add(eventMap);
				}
				if("onload_es5".equals(key)){
					Map<String, Object> eventMap = new HashMap<String, Object>();
					eventMap.put("name", key);
					eventMap.put("function", pageConfig.get(key));
					events.add(eventMap);
				}
			}
			layout.put("events", events);
			listJsonMap.put("events", events);
			listJsonMap.putAll(pageConfig);

//			listJsonMap.put("printButtons", printButtonMap);
			List<Map> sections = (List) layoutTemp.get("sections");
			if(sections != null){
				Map<String, Object> propertyMap = this.getPropertyInfoMap(view.getAssModel());//TODO
				for(Map cellsMap: sections){
					String regionType = cellsMap.get("regionType").toString();
					elementList = (List<Map<String, Object>>) cellsMap.get("cells");
					if(null != elementList && !elementList.isEmpty()){
						Set<String> toDeleteCodeSet = new HashSet<>();  // 待删除的
						Set<String> nonCodeSet = new HashSet<>();  // code以none结尾的
						for(Map<String,Object> elementMap : elementList){
							Map<String,Object> element = (Map<String, Object>) elementMap.get("element");
							if(element != null){
								String columnType = (String) element.get("columnType");
								element = this.replaceData(element,false);
								String propertyCode = (String) element.get("propertyCode");
								if(null != propertyCode){
									int startIndex = propertyCode.lastIndexOf('|');
									if(startIndex != -1){
										propertyCode = propertyCode.substring(startIndex + 1, propertyCode.length());
									}
									Property property = (Property) propertyMap.get(propertyCode);
									if(null != property){
										element.put("onlyLeaf", property.getOnlyLeaf());
										String dbColumnType = property.getType().name();
										String columnName = property.getColumnName();
										element.put("dbColumnType", dbColumnType);
										element.put("modelCode", property.getModel().getCode());
										element.put("columnName", columnName);
										if(null != columnType && "LONG".equals(columnType)){
											element.put("maxValue", Long.MAX_VALUE + "");
											element.put("minValue", Long.MIN_VALUE + "");
										}else if(null != columnType && "INTEGER".equals(columnType)){
											element.put("maxValue", Integer.MAX_VALUE + "");
											element.put("minValue", Integer.MIN_VALUE + "");
										}else if(null != columnType && !"LONGTEXT".equals(columnType)){
											element.put("maxLength", getPropertyMaxLength(property));
										}
									}
								}
								if(null == propertyCode && null == columnType){
									element.put("modelCode", view.getAssModel().getCode());
								}
							}else{
								String columnType = (String) elementMap.get("columnType");
								elementMap = this.replaceData(elementMap,false);
								String propertyCode = (String) elementMap.get("propertyCode");
								if(elementMap.containsKey("showType") && "PROPERTYATTACHMENT".equals(elementMap.get("showType").toString())){
									String key = elementMap.get("key").toString();
									elementMap.put("key", key + "AttachementInfo");
								}
								if(null != propertyCode){
									int startIndex = propertyCode.lastIndexOf('|');
									if(startIndex != -1){
										propertyCode = propertyCode.substring(startIndex + 1, propertyCode.length());
									}
									Property property = (Property) propertyMap.get(propertyCode);
									if(null != property){
										elementMap.put("modelCode", property.getModel().getCode());
									}
								}
								if(null == propertyCode && null == columnType){
									elementMap.put("modelCode", view.getAssModel().getCode());
								}

							}
							Boolean isPerssion =  (Boolean) elementMap.get("ispermission");
							if(isPerssion != null && isPerssion){
								String operateCode = (String) elementMap.get("buttonoperationcode");
								if(parentViewCode != null){
									operateCode = parentViewCode + "_" + operateCode;
								}
								elementMap.put("pc", this.getPowerCode(operateCode));
							}else{
								if(parentViewCode != null){
									elementMap.put("pc", this.getPowerCode(parentViewCode + "_self"));
								}else{
									elementMap.put("pc", this.getPowerCode(view.getCode() + "_self"));
								}
							}
							String code = (String) elementMap.get("code");
							toDeleteCodeSet.add(code + "_none");
							if (null != code && code.endsWith("_none")) {
								nonCodeSet.add(code);
							}
						}
						// 删除多余的none结尾
						toDeleteCodeSet.retainAll(nonCodeSet);
						Iterator<Map<String, Object>> it = elementList.iterator();
						while (it.hasNext()) {
							Map<String, Object> elementMap = it.next();
							if (toDeleteCodeSet.contains(elementMap.get("code"))) {
								it.remove();
							}

						}
					}
					if(cellsMap.get("listProperty") != null){
						datagridComponent.putAll((Map<String, String>) cellsMap.get("listProperty"));
//						elementListPT.put("elementList", elementList);
					}

					if(cellsMap.get("fastProperty") != null){
						searchComponent.putAll((Map<String, String>) cellsMap.get("fastProperty"));
					}

					if(regionType != null && "BUTTON".equals(regionType)){//按钮的信息==================================
						List<Map<String, Object>> printButtonMap = convertPrintButton2Json(view,parentViewCode);
						if(printButtonMap !=null && printButtonMap.size()>0){
							for(Map<String,Object> printbuttonmap :printButtonMap) {
								elementList.add(printbuttonmap);
							}
						}
						datagridComponent.put("buttons", elementList);
					}else if(regionType != null && "FASTQUERY".equals(regionType)){//快速查询
						searchComponent.put("fastProperty", elementList);
					}else if(regionType != null && "ADVQUERY".equals(regionType)){//高级查询
						searchComponent.put("advProperty", elementList);
					}else if(regionType != null && "LISTPT".equals(regionType)){//列表字段
						if(view.getEntity().getWorkflowEnabled() && view.getAssModel().getIsMain() && view.getType() != ViewType.REFERENCE){
							//添加工作流状态列
							Map<String, Object> taskDescription = new HashMap<String, Object>();
							taskDescription.put("width", 120);
							taskDescription.put("key", "pending.taskDescription");
							taskDescription.put("namekey", "ec.list.taskDescription");
//							taskDescription.put("label", "ec.list.taskDescription");
							taskDescription.put("columnType", "TEXT");
							taskDescription.put("showType", "TEXTFIELD");
							taskDescription.put("isreadonly", true);
							taskDescription.put("isrefselect", false);
							taskDescription.put("hide", false);
							taskDescription.put("ishide", false);
							taskDescription.put("isCount", false);
							taskDescription.put("isTotal", false);
							taskDescription.put("code", "");
							taskDescription.put("cellCode", "");
							elementList.add(taskDescription);
						}
						datagridComponent.put("fields", elementList);
					}
				}
			}
		}
		//数据分类
		if(null != dataGroups && !dataGroups.isEmpty()){
			List dataclassifyList = new ArrayList();
			for(DataGroup dataGroup : dataGroups){
				Map<String,Object> dgMap = new HashMap<String, Object>();
				dgMap.put("code", dataGroup.getCode());
				dgMap.put("displayName", dataGroup.getDisplayName());
				dgMap.put("displayNameInternational", InternationalResource.get(dataGroup.getName()));
				dgMap.put("isMultiple", dataGroup.getIsMultiple());
				dgMap.put("name", dataGroup.getName());
				if(null != dataGroup.getDataClassifics() && !dataGroup.getDataClassifics().isEmpty()){
					Set<DataClassific> dataClassifics = dataGroup.getDataClassifics();
					List classifics = new ArrayList();
					for(DataClassific classific : dataClassifics){
						Map<String,Object> temp = new HashMap<String, Object>();
						temp.put("code", classific.getCode());
						temp.put("name", classific.getName());
						temp.put("isDefault", classific.getIsDefault());
						temp.put("displayName", classific.getDisplayName());
						temp.put("condition", classific.getCondition());
						classifics.add(temp);
					}
					dgMap.put("dataClassificLis", classifics);
				}
				if(!dgMap.isEmpty()){
					dataclassifyList.add(dgMap);
				}

			}
			if(!dataclassifyList.isEmpty()){
				searchComponent.put("dataGroupProperty", dataclassifyList);
			}
		}
		List<Map> layoutComponents = new ArrayList<Map>();
		layoutComponents.add(searchComponent);
		layoutComponents.add(datagridComponent);
		layout.put("type", "layout");
		layout.put("layoutmethod", "column");
		layout.put("components", layoutComponents);
		if(view.getShowType() == ShowType.PART){
			String json = serializer.deepSerialize(layout);
			view.getExtraView().setViewJson(json);

		}else{
			List<Map> components = new ArrayList<Map>();
			components.add(layout);
			listJsonMap.put("components", components);
			log.debug("listJsonMap:"+listJsonMap);
			String json = serializer.deepSerialize(listJsonMap);
			view.getExtraView().setViewJson(json);
			viewDao.mergeExtraView(view.getExtraView());

		}
	}

	/**
	 * 编辑视图的xml转json
	 * @param view
	 */
	private void convertEdit2EditJson(View view) {
		// TODO Auto-generated method stub
		String extraConfig = ecConfigService.getEcFullConfig(view);
		if(extraConfig == null || "".equals(extraConfig)){
			return ;
		}
		Map map = (Map) XmlUtils.convert(extraConfig);
		JSONSerializer serializer = new JSONSerializer();
		log.debug("viewJson" + serializer.deepSerialize(map));
		Map layout = (Map) map.get("layout");
		//=======================1.页面信息组织 start=====================================
		Map<String,Object> editJsonMap = new HashMap<String, Object>();

		editJsonMap.put("pageType", "EDIT");
		editJsonMap.put("title", view.getTitle());
		editJsonMap.put("url", view.getUrl());
		editJsonMap.put("hasAttachment", view.getHasAttachment());
		if(ViewType.VIEW == view.getType()){
			editJsonMap.put("attachmentFlag", view.getAttachmentFlag());
		}
		editJsonMap.put("dealInfoShow", view.getDealInfoShow());
		editJsonMap.put("enableSimpleDealInfo", view.getEnableSimpleDealInfo());
		editJsonMap.put("dealInfoGroup", view.getDealInfoGroup());
		if(layout.containsKey("pageConfig")){
			Map<String, Object> pageConfigMap = (Map<String, Object>) layout.get("pageConfig");
			//页面事件
			List<Map<String,Object>> events = new ArrayList<Map<String,Object>>();
			for(String key: pageConfigMap.keySet()){
				if("onload".equals(key) || "onsave".equals(key) || "onload_es5".equals(key) || "onsave_es5".equals(key)){
					Map<String, Object> eventMap = new HashMap<String, Object>();
					eventMap.put("name", key);
					eventMap.put("function", pageConfigMap.get(key));
					events.add(eventMap);
				}else{
					editJsonMap.put(key, pageConfigMap.get(key));
				}
			}
			editJsonMap.put("events", events);
		}
		// 参照复制按钮
		Map<String, Object> referenceCopyButtonMap = new HashMap<>();
		if (view.getIsReference() && null != view.getReference()) {
			View referenceView = view.getReference();
			referenceCopyButtonMap.put("isReference", true);
			MenuOperate flowList = menuOperateService.getFlowList(view.getEntity().getCode());
			if (flowList == null) {
				throw new EcException("未发布主列表视图菜单");
			}
			String url = referenceView.getUrl() + "?" + getPowerCode(flowList.getCode());
			referenceCopyButtonMap.put("url", url);
		} else {
			referenceCopyButtonMap.put("isReference", false);
		}
		editJsonMap.put("refCopy", referenceCopyButtonMap);
		List<Map<String, Object>> mainComponents = new ArrayList<Map<String,Object>>();
		editJsonMap.put("components", mainComponents);
		//=======================1.页面信息组织 end=====================================

		//=======================2.页面按钮组件信息组织 start===============================
		Map<String, Object> mainComponent = new HashMap<String, Object>();
		List<Map<String, Object>> components = new ArrayList<Map<String,Object>>();
		mainComponent.put("type", "layout");
		mainComponent.put("layoutmethod", "column");
		mainComponent.put("components", components);
		mainComponents.add(mainComponent);

		//组织button配置
		Map<String, Object> sectionTmp = (Map<String, Object>) ((List) layout.get("sections")).get(0);
		//log.info(serializer.deepSerialize(sectionTmp));
		String regionType = sectionTmp.get("regionType").toString();
		Map<String, Object> buttonLayout = new HashMap<String, Object>();
		if(regionType != null && "BUTTON".equals(regionType)){
			buttonLayout.put("type", "button");
			buttonLayout.put("fix_h", 44);
			buttonLayout.put("layoutmethod", "container");
			List<Map<String, Object>> buttonMapComponents = new ArrayList<Map<String,Object>>();
			List<Map<String, Object>> elementListTmp =  (List<Map<String, Object>>) sectionTmp.get("cells");
			Map<String, Object> buttonMap = new HashMap<String, Object>();
			for(Map<String, Object> element: elementListTmp){
				if (element.containsKey("ispermission") && Boolean.valueOf(element.get("ispermission").toString())) {
					String permissionCode = element.get("permissionCode").toString();
					String operateCode = view.getEntity().getCode() + "_" + permissionCode;
					element.put("pc", getPowerCode(operateCode));
				}
				buttonMapComponents.add(element);
			}
			List<Map<String, Object>> printButtonMap = convertPrintButton2Json(view, null);
			if(printButtonMap !=null && printButtonMap.size()>0){
				for(Map<String,Object> printbuttonmap :printButtonMap) {
					buttonMapComponents.add(printbuttonmap);
				}
			}
			buttonLayout.put("components", buttonMapComponents);

			//无框架增强型视图，如果组态了button
			if(view.getType() == ViewType.EXTRA && buttonMapComponents.size() > 0 && !"dialog".equals(view.getOpenType())){
				components.add(buttonLayout);
			}else if(view.getType() != ViewType.EXTRA && !"dialog".equals(view.getOpenType())){
				Map<String, Object> titleLayout = new HashMap<String, Object>();
				titleLayout.put("type", "title");
				titleLayout.put("fix_h", 44);
				titleLayout.put("layoutmethod", "container");
				titleLayout.put("components", null);
				components.add(titleLayout);
				components.add(buttonLayout);
			}/*else if("dialog".equals(view.getOpenType())){
				components.add(buttonLayout);
			}*/
		}
		//组织tabs配置
		Map<String, Object> fieldLayout = new HashMap<String, Object>();
		List<Map> tabsTmp = (List)layout.get("tabs");
		List<Map> tabsComponents = new ArrayList<Map>();
		Map<String, Object> propertyMap = this.getPropertyInfoMap(view.getAssModel());
		for(Map tabMapTmp: tabsTmp){
			Map<String, Object> tabMap = new HashMap<String, Object>();
			tabMap.put("type", "layout");
			tabMap.put("tabCode", tabMapTmp.get("tabCode"));
			tabMap.put("name", tabMapTmp.get("name"));
			tabMap.put("namekey", tabMapTmp.get("namekey"));
			tabMap.put("id", tabMapTmp.get("id"));
			if(tabMapTmp.containsKey("ptRealTimeLoad")){
				tabMap.put("ptRealTimeLoad", tabMapTmp.get("ptRealTimeLoad"));
			}
			Map<String, Object> result = convertLayoutJson(view, tabMapTmp, propertyMap);
			tabMap.putAll(result);
			tabsComponents.add(tabMap);
		}
		fieldLayout.put("type", "layout");
		fieldLayout.put("layoutmethod", "tab");
		fieldLayout.put("components", tabsComponents);
		components.add(fieldLayout);

		if(view.getEntity().getWorkflowEnabled() && view.getAssModel().getIsMain()
				&& view.getType() != ViewType.EXTRA ){
			//组织工作流栏
			Map<String, Object> workFlowLayout = new HashMap<String, Object>();
			workFlowLayout.put("type", "workflow");
			workFlowLayout.put("fix_h", 95);
			workFlowLayout.put("layoutmethod", "container");
			workFlowLayout.put("components", null);
			components.add(workFlowLayout);
		}

		String json = serializer.deepSerialize(editJsonMap);
		view.getExtraView().setViewJson(json);
		viewDao.mergeExtraView(view.getExtraView());
		datagridMap.clear();
		datagridCodeSet.clear();
	}


	/**
	 * 编辑的配置字段转json
	 * @param layoutMap
	 * @return
	 */
	public Map convertLayoutJson(View view, Map layoutMap, Map<String, Object> propertyMap){
		Map<String, Object> result = new HashMap<String, Object>();
		Map<String, Object> subConfig = (Map<String, Object>) layoutMap.get("layoutProperties");
		if(subConfig == null) {
			subConfig = new HashMap<String, Object>();
		}
		List<Map> layout = new ArrayList<Map>();
		if(layoutMap.containsKey("sections")){
			List<Map> sectionsTmp = (List<Map>) layoutMap.get("sections");
			result.put("type", "layout");
			result.put("layoutmethod", "container");
			for(Map section: sectionsTmp){//pageConfig
				List<Map<String, Object>> elementList = new ArrayList<Map<String,Object>>();
				List<Map<String, Object>> propertyList = new ArrayList<Map<String,Object>>();
				List<Map<String, Object>> datagridList = new ArrayList<Map<String,Object>>();
				Map<String, Object> sectionMap = new HashMap<String, Object>();
				if((Map<String, Object>) section.get("pageConfig") != null){
					sectionMap.putAll((Map<String, Object>) section.get("pageConfig"));
				}
				sectionMap.put("type", "layoutSection");
				sectionMap.put("cssstyle", section.get("cssstyle"));
				sectionMap.put("isborder", section.get("isborder"));
				sectionMap.put("customSection", section.get("customSection"));
				sectionMap.put("name", section.get("name"));
				sectionMap.put("regionType", section.get("regionType"));
				sectionMap.put("sectionCode", section.get("sectionCode"));

				String layoutContent = (String) subConfig.get("layoutContent");
				elementList = (List<Map<String, Object>>) section.get("cells");
				if(null == elementList || elementList.isEmpty()){
					continue;
				}
				List<Map> cellComponents = new ArrayList<Map>();

				//表格还是普通字段//TODO
				if(null != layoutContent && ("datagrid".equalsIgnoreCase(layoutContent)
						|| "datatable".equalsIgnoreCase(layoutContent)
						|| "easytable".equalsIgnoreCase(layoutContent))){
					elementList = convertDatagridJson(elementList, propertyMap);
					sectionMap.put("components", elementList);
				}else if(null != layoutContent){
					for(Map<String,Object> elementMap : elementList){
						if(elementMap.containsKey("callbackbody")){
							String callbackbody = elementMap.get("callbackbody").toString();
							String callbackbody_es5 = (String) elementMap.get("callbackbody_es5");
							String callbackname = elementMap.get("callbackname").toString();
							String funcbody = null;
							String funcbody_es5 = null;
							if(elementMap.containsKey("funcbody")){
								funcbody = elementMap.get("funcbody").toString() + "@@@@" + callbackbody;
								funcbody_es5 = elementMap.get("funcbody_es5") + "@@@@" + callbackbody_es5;
							}else{
								funcbody = callbackbody;
								funcbody_es5 = callbackbody_es5;
							}
							elementMap.put("funcbody", funcbody);
							elementMap.put("funcbody_es5", funcbody_es5);

							String funcname = null;
							if(elementMap.containsKey("funcname")){
								funcname = elementMap.get("funcname").toString() + " callback='" + callbackname + "'";
							}else{
								funcname = "callback='" + callbackname + "'";;
							}
							elementMap.put("funcname", funcname);
						}
						elementMap.put("type", "layoutCell");
						Map<String,Object> element = (Map<String, Object>) elementMap.get("element");
						if(element != null){
							String propertyCode = (String) element.get("propertyCode");
							String columnType = (String) element.get("columnType");
							if(null != propertyCode){
								int startIndex = propertyCode.lastIndexOf('|');
								if(startIndex != -1){
									propertyCode = propertyCode.substring(startIndex + 1, propertyCode.length());
								}
								Property property = (Property) propertyMap.get(propertyCode);
								if(null != property){
									element.put("onlyLeaf", property.getOnlyLeaf());
									String dbColumnType = property.getType().name();
									String columnName = property.getColumnName();
									element.put("dbColumnType", dbColumnType);
									element.put("modelCode", property.getModel().getCode());
									element.put("columnName", columnName);
									if(null != columnType && "LONG".equals(columnType)){
										element.put("maxValue", Long.MAX_VALUE + "");
										element.put("minValue", Long.MIN_VALUE + "");
									}else if(null != columnType && "INTEGER".equals(columnType)){
										element.put("maxValue", Integer.MAX_VALUE + "");
										element.put("minValue", Integer.MIN_VALUE + "");
									}else if(null != columnType && !"LONGTEXT".equals(columnType)){
										element.put("maxLength", getPropertyMaxLength(property));
									}
								}
							}
							elementMap.put("element", this.replaceData(element,false));
							propertyList.add(elementMap);
						}else{
							String propertyCode = (String) elementMap.get("propertyCode");
							String columnType = (String) elementMap.get("columnType");
							if(null != propertyCode){
								int startIndex = propertyCode.lastIndexOf('|');
								if(startIndex != -1){
									propertyCode = propertyCode.substring(startIndex + 1, propertyCode.length());
								}
								Property property = (Property) propertyMap.get(propertyCode);
								if(null != property){
									elementMap.put("onlyLeaf", property.getOnlyLeaf());
									String dbColumnType = property.getType().name();
									String columnName = property.getColumnName();
									elementMap.put("dbColumnType", dbColumnType);
									elementMap.put("modelCode", property.getModel().getCode());
									elementMap.put("columnName", columnName);
									if(null != columnType && "LONG".equals(columnType)){
										elementMap.put("maxValue", Long.MAX_VALUE + "");
										elementMap.put("minValue", Long.MIN_VALUE + "");
									}else if(null != columnType && "INTEGER".equals(columnType)){
										elementMap.put("maxValue", Integer.MAX_VALUE + "");
										elementMap.put("minValue", Integer.MIN_VALUE + "");
									}else if(null != columnType && !"LONGTEXT".equals(columnType)){
										elementMap.put("maxLength", getPropertyMaxLength(property));
									}
								}
							}
							propertyList.add(elementMap);
						}

					}
				}else if(null == layoutContent){
					for(Map<String,Object> elementMap : elementList){
						if(elementMap.containsKey("callbackbody")){
							String callbackbody = elementMap.get("callbackbody").toString();
							String callbackbody_es5 = elementMap.get("callbackbody_es5").toString();
							String callbackname = elementMap.get("callbackname").toString();
							String funcbody = null;
							String funcbody_es5 = null;
							if(elementMap.containsKey("funcbody")){
								funcbody = elementMap.get("funcbody").toString() + "@@@@" + callbackbody;
								funcbody_es5 = elementMap.get("funcbody_es5") + "@@@@" + callbackbody_es5;
							}else{
								funcbody = callbackbody;
								funcbody_es5 = callbackbody_es5;
							}
							elementMap.put("funcbody", funcbody);
							elementMap.put("funcbody_es5", funcbody_es5);

							String funcname = null;
							if(elementMap.containsKey("funcname")){
								funcname = elementMap.get("funcname").toString() + " callback='" + callbackname + "'";
							}else{
								funcname = "callback='" + callbackname + "'";;
							}
							elementMap.put("funcname", funcname);
						}
						elementMap.put("type", "layoutCell");
						Map<String,Object> element = (Map<String, Object>) elementMap.get("element");
						if(element != null && element.get("columnType") != null && "datagrid".equalsIgnoreCase(element.get("columnType").toString())){
							datagridList.add(element);
						}else if(element != null){
							String columnType = (String) element.get("columnType");
							String propertyCode = (String) element.get("propertyCode");
							if(null != propertyCode){
								int startIndex = propertyCode.lastIndexOf('|');
								if(startIndex != -1){
									propertyCode = propertyCode.substring(startIndex + 1, propertyCode.length());
								}
								Property property = (Property) propertyMap.get(propertyCode);
								if(null != property){
									String dbColumnType = property.getType().name();
									String columnName = property.getColumnName();
									element.put("dbColumnType", dbColumnType);
									element.put("modelCode", property.getModel().getCode());
									element.put("columnName", columnName);
									if(null != columnType && "LONG".equals(columnType)){
										element.put("maxValue", Long.MAX_VALUE + "");
										element.put("minValue", Long.MIN_VALUE + "");
									}else if(null != columnType && "INTEGER".equals(columnType)){
										element.put("maxValue", Integer.MAX_VALUE + "");
										element.put("minValue", Integer.MIN_VALUE + "");
									}else if(null != columnType && !"LONGTEXT".equals(columnType)){
										element.put("maxLength", getPropertyMaxLength(property));
									}
								}
							}
							elementMap.put("element", this.replaceData(element,false));
							propertyList.add(elementMap);
						}else{
							String propertyCode = (String) elementMap.get("propertyCode");
							String columnType = (String) elementMap.get("columnType");
							if(null != propertyCode){
								int startIndex = propertyCode.lastIndexOf('|');
								if(startIndex != -1){
									propertyCode = propertyCode.substring(startIndex + 1, propertyCode.length());
								}
								Property property = (Property) propertyMap.get(propertyCode);
								if(null != property){
									String dbColumnType = property.getType().name();
									String columnName = property.getColumnName();
									elementMap.put("dbColumnType", dbColumnType);
									elementMap.put("modelCode", property.getModel().getCode());
									elementMap.put("columnName", columnName);
									if(null != columnType && "LONG".equals(columnType)){
										elementMap.put("maxValue", Long.MAX_VALUE + "");
										elementMap.put("minValue", Long.MIN_VALUE + "");
									}else if(null != columnType && "INTEGER".equals(columnType)){
										elementMap.put("maxValue", Integer.MAX_VALUE + "");
										elementMap.put("minValue", Integer.MIN_VALUE + "");
									}else if(null != columnType && !"LONGTEXT".equals(columnType)){
										elementMap.put("maxLength", getPropertyMaxLength(property));
									}
								}
							}
							propertyList.add(elementMap);
						}
					}
				}
				if(datagridList.size() > 0){
					Map<String,Object> element = new HashMap<String, Object>();
					datagridList = convertDatagridJson(datagridList, propertyMap);
					element.put("components", datagridList);
					element.put("type", "layout");
					element.put("layoutmethod", "container");

					propertyList.add(element);
				}
				if(propertyList.size() > 0){
					sectionMap.put("cells", propertyList);
				}
				layout.add(sectionMap);
			}

			result.putAll(subConfig);
			result.put("components", layout);
		}else if(subConfig.containsKey("layoutContent") && "searchWidget".equals(subConfig.get("layoutContent").toString())){
			//查询控件
			String layoutContent = (String) subConfig.get("layoutContent");
			if(null != layoutContent && "searchWidget".equals(layoutContent)){
				result.put("type", "layout");
				result.put("layoutmethod", "container");
				result.putAll(convertSearchWidget(view, subConfig));
			}
		}else{
			List<Map> layoutList = (List<Map>) layoutMap.get("layout");
			if(null == layoutList){
				result.putAll((Map<String, Object>) layoutMap.get("layoutProperties"));
				List<Map> tabsTmp = (List<Map>) layoutMap.get("tabs");
				List<Map> tabsComponents = new ArrayList<Map>();
				if(tabsTmp != null){
					for(Map tabMapTmp: tabsTmp){
						Map<String, Object> tabMap = new HashMap<String, Object>();
						tabMap.put("tabCode", tabMapTmp.get("tabCode"));
						tabMap.put("name", tabMapTmp.get("name"));
						tabMap.put("namekey", tabMapTmp.get("namekey"));
						tabMap.put("id", tabMapTmp.get("name"));//id没有
						tabMap.putAll(convertLayoutJson(view, tabMapTmp, propertyMap));
						tabsComponents.add(tabMap);
					}
				}
				result.put("components", tabsComponents);

				if(result.containsKey("layoutContent") && "echarts".equals(result.get("layoutContent").toString())){
					getEchartsInfo(result);
				}
			}else{
				for(Map map: layoutList){
					if(map.isEmpty()){
						return map;
					}
					layout.add(convertLayoutJson(view, map, propertyMap));
				}
				result.put("components", layout);
				result.putAll(subConfig);
			}

		}
		return result;
	}

	/**
	 * 组织查询控件的xml
	 * @param view
	 * @param config
	 * @return
	 */
	private Map<String, Object> convertSearchWidget(View view, Map config){
		String layoutName = config.containsKey("layoutname")?config.get("layoutname").toString(): null;
		if(null == layoutName){
			return config;
		}
		//记录下datagridCode.
		if(config.containsKey("datagridCode")){
			List<Map<String, String>> datagridCodes = (List<Map<String, String>>) config.get("datagridCode");
			for(Map<String, String> datagridCodeMap: datagridCodes){
				String datagridCode = datagridCodeMap.get("code");
				datagridCodeSet.add(datagridCode);
				if(datagridMap.containsKey(datagridCode)){
					Map<String, Object> listJsonMap = datagridMap.get(datagridCode);
					listJsonMap.put("hasFastQuery", true);
				}
			}
		}
		List<Map<String, Object>> components = new ArrayList<Map<String,Object>>();
		Map<String, Object> component = new HashMap<String, Object>();
		components.add(component);
		config.put("components", components);
		//快速查询
		if(config.containsKey("fqjCode")){
			String fqjCode = config.get("fqjCode").toString();
			FastQueryJson fastQueryJson = fastQueryJsonDao.findEntityByCriteria(Restrictions.eq("code", fqjCode), Restrictions.eq("layoutName", layoutName));
			Model model = fastQueryJson.getTargetModel();
			if(null != model){
				config.put("modelAlias", StringUtils.firstLetterToLower(model.getModelName()));
			}
			Map queryConfigMap = (Map) XmlUtils.convert(fastQueryJson.getQueryConfig());
			if(queryConfigMap.containsKey("fastProperty")){
				component.putAll((Map<String, String>) queryConfigMap.get("fastProperty"));
			}
			if(queryConfigMap.containsKey("fastQueryJson")){
				Map fastQueryMap = (Map) queryConfigMap.get("fastQueryJson");
				List<Map> sections = (List) fastQueryMap.get("sections");
				if(sections != null){
					Map<String, Object> propertyMap = this.getPropertyInfoMap(model);
					for(Map cellsMap: sections){
						String regionType = cellsMap.get("regionType").toString();
						List<Map<String, Object>> elementList = (List<Map<String, Object>>) cellsMap.get("cells");
						if(null != elementList && !elementList.isEmpty()){
							for(Map<String,Object> elementMap : elementList){
								Map<String,Object> element = (Map<String, Object>) elementMap.get("element");
								if(element != null){
									String columnType = (String) element.get("columnType");
									element = this.replaceData(element,false);
									String propertyCode = (String) element.get("propertyCode");
									if(null != propertyCode){
										int startIndex = propertyCode.lastIndexOf('|');
										if(startIndex != -1){
											propertyCode = propertyCode.substring(startIndex + 1, propertyCode.length());
										}
										Property property = (Property) propertyMap.get(propertyCode);
										if(null != property){
											element.put("onlyLeaf", property.getOnlyLeaf());
											String dbColumnType = property.getType().name();
											String columnName = property.getColumnName();
											element.put("dbColumnType", dbColumnType);
											element.put("modelCode", property.getModel().getCode());
											element.put("columnName", columnName);
											if(null != columnType && "LONG".equals(columnType)){
												element.put("maxValue", Long.MAX_VALUE + "");
												element.put("minValue", Long.MIN_VALUE + "");
											}else if(null != columnType && "INTEGER".equals(columnType)){
												element.put("maxValue", Integer.MAX_VALUE + "");
												element.put("minValue", Integer.MIN_VALUE + "");
											}else if(null != columnType && !"LONGTEXT".equals(columnType)){
												element.put("maxLength", getPropertyMaxLength(property));
											}
										}
									}
								}

							}
						}
						component.put("fastProperty", elementList);
					}
				}
			}
		}

		if(config.containsKey("aqjCode")){
			String aqjCode = config.get("aqjCode").toString();
			AdvQueryJson advQueryJson = advQueryJsonDao.findEntityByCriteria(Restrictions.eq("code", aqjCode), Restrictions.eq("layoutName", layoutName));
			Map queryConfigMap = (Map) XmlUtils.convert(advQueryJson.getQueryConfig());
			if(queryConfigMap.containsKey("advQueryJson")){
				Map advQueryMap = (Map) queryConfigMap.get("advQueryJson");
				List<Map> sections = (List) advQueryMap.get("sections");
				if(sections != null){
					Map<String, Object> propertyMap = this.getPropertyInfoMap(view.getAssModel());//TODO
					for(Map cellsMap: sections){
						String regionType = cellsMap.get("regionType").toString();
						List<Map<String, Object>> elementList = (List<Map<String, Object>>) cellsMap.get("cells");
						if(null != elementList && !elementList.isEmpty()){
							for(Map<String,Object> elementMap : elementList){
								Map<String,Object> element = (Map<String, Object>) elementMap.get("element");
								if(element != null){
									String columnType = (String) element.get("columnType");
									element = this.replaceData(element,false);
									String propertyCode = (String) element.get("propertyCode");
									if(null != propertyCode){
										int startIndex = propertyCode.lastIndexOf('|');
										if(startIndex != -1){
											propertyCode = propertyCode.substring(startIndex + 1, propertyCode.length());
										}
										Property property = (Property) propertyMap.get(propertyCode);
										if(null != property){
											element.put("onlyLeaf", property.getOnlyLeaf());
											String dbColumnType = property.getType().name();
											String columnName = property.getColumnName();
											element.put("dbColumnType", dbColumnType);
											element.put("modelCode", property.getModel().getCode());
											element.put("columnName", columnName);
											if(null != columnType && "LONG".equals(columnType)){
												element.put("maxValue", Long.MAX_VALUE + "");
												element.put("minValue", Long.MIN_VALUE + "");
											}else if(null != columnType && "INTEGER".equals(columnType)){
												element.put("maxValue", Integer.MAX_VALUE + "");
												element.put("minValue", Integer.MIN_VALUE + "");
											}else if(null != columnType && !"LONGTEXT".equals(columnType)){
												element.put("maxLength", getPropertyMaxLength(property));
											}
										}
									}
								}

							}
						}
						component.put("advProperty", elementList);
					}
				}
			}
		}

		List<DataGroup> dataGroups = dataGroupDao.findByCriteria(Restrictions.eq("valid", true), Restrictions.eq("view", view), Restrictions.eq("layoutName", layoutName));
		if(null != dataGroups && !dataGroups.isEmpty()){
			List dataclassifyList = new ArrayList();
			for(DataGroup dataGroup : dataGroups){
				Map<String,Object> dgMap = new HashMap<String, Object>();
				dgMap.put("dgname", InternationalResource.get(dataGroup.getName()));
				dgMap.put("dgcode", dataGroup.getCode());
				if(null != dataGroup.getDataClassifics() && !dataGroup.getDataClassifics().isEmpty()){
					Set<DataClassific> dataClassifics = dataGroup.getDataClassifics();
					List classifics = new ArrayList();
					for(DataClassific classific : dataClassifics){
						Map<String,Object> temp = new HashMap<String, Object>();
						temp.put("code", classific.getCode());
						temp.put("name", classific.getName());
						temp.put("isDefault", classific.getIsDefault());
						classifics.add(temp);
					}
					dgMap.put("dgvalue", classifics);
				}
				if(!dgMap.isEmpty()){
					dataclassifyList.add(dgMap);
				}
			}
			if(!dataclassifyList.isEmpty()){
				component.put("dataclassify", dataclassifyList);
			}
		}
		component.put("type", "layoutSearchWidget");
		return config;
	}

	//双向关联查询控件和表格
	Map<String, Map<String, Object>> datagridMap = new HashMap<String, Map<String,Object>>();
	Set<String > datagridCodeSet = new HashSet<String>();
	/**
	 * 编辑视图表格转json
	 * @param elementList
	 * @return
	 */
	private List<Map<String, Object>> convertDatagridJson(List<Map<String, Object>> elementList, Map<String, Object> propertyMap){
		List<Map<String, Object>> datagridList = new ArrayList<Map<String,Object>>();
		Map<String, Object> listJsonMap = null;
		JSONSerializer serializer = new JSONSerializer();
		for(Map<String, Object> element: elementList){
			String dataGridCode = (String) element.get("DataGridCode");
			DataGrid dataGrid = dataGridService.getDataGrid(dataGridCode);
			String config = ecConfigService.getEcFullConfig(dataGrid);
			listJsonMap = convertDatagridJson(dataGrid, config, propertyMap);
			listJsonMap.putAll(element);
			listJsonMap.put("title", dataGrid.getDataGridName());
			listJsonMap.put("code", dataGridCode);
			listJsonMap.put("modelCode", dataGrid.getTargetModel().getCode());
			listJsonMap.put("dataGridName", dataGrid.getName());
			listJsonMap.put("type", "layoutDatagrid");
			if(null != dataGrid.getDataGridType() && dataGrid.getDataGridType() == 1){
				String nameSpace="";
				if(null!= ProjectFlagHolder.getInstance().getProjFlag().get() && ProjectFlagHolder.getInstance().getProjFlag().get()) {
					nameSpace = "/" + dataGrid.getTargetModel().getEntity().getModule().getArtifact()
							+ "/" + dataGrid.getTargetModel().getEntity().getEntityName()
							+ "/" + StringUtils.firstLetterToLower(dataGrid.getTargetModel().getModelName())+"/proj" + "/";
				}else{
					nameSpace = "/" + dataGrid.getTargetModel().getEntity().getModule().getArtifact()
							+ "/" + dataGrid.getTargetModel().getEntity().getEntityName()
							+ "/" + StringUtils.firstLetterToLower(dataGrid.getTargetModel().getModelName()) + "/";
				}
				listJsonMap.put("downloadXls", "/msService" + nameSpace + "downloadXls");
				listJsonMap.put("importMainXls", "/msService" + nameSpace + "importMainXls");
				listJsonMap.put("queryUrl", nameSpace + "data-" + dataGrid.getName());
				listJsonMap.put("getRequireData", "/baseService/excel/getRequireDataByModelcode");//导出模型所有属性数据接口
			}else{
				listJsonMap.put("queryUrl", null);
			}

			String idPrefix = dataGrid.getTargetModel().getModelName() + "_" + dataGrid.getName();
			listJsonMap.put("idPrefix", idPrefix);
			if(datagridCodeSet.contains(dataGridCode)){
				listJsonMap.put("hasFastQuery", true);
			}else{
				listJsonMap.put("hasFastQuery", false);
			}
			datagridMap.put(dataGridCode, listJsonMap);
			datagridList.add(listJsonMap);
			String json = serializer.deepSerialize(listJsonMap);
			dataGrid.setDataGridJson(json);
			//dataGridDao.save(dataGrid);
		}
		return datagridList;
	}

	/**
	 * 编辑视图表格转json
	 * @param
	 * @param config
	 * @return
	 */
	private Map<String, Object> convertDatagridJson(DataGrid datagrid, String config, Map<String, Object> propertyMap){
		Map<String,Object> listJsonMap = new HashMap<String, Object>();
		Map map = (Map) XmlUtils.convert(config);
		if(map == null){
			return listJsonMap;
		}
		List<Map<String, Object>> elementList = new ArrayList<Map<String,Object>>();
		Map layout = (Map) map.get("layout");
		if(null != layout){
			Map<String, String> pageConfig = (Map<String, String>) layout.get("listProperty");
			if(null == pageConfig){
				pageConfig = new HashMap<String, String>();
			}
			pageConfig.putAll((Map<String, String>) layout.get("pageConfig"));
			List<Map> sections = (List) layout.get("sections");
			if(null != sections){
				for(Map cellsMap: sections){
					String regionType = cellsMap.get("regionType").toString();
					elementList = (List<Map<String, Object>>) cellsMap.get("cells");
					if(cellsMap.containsKey("listProperty")){
						pageConfig.putAll((Map<String, String>) cellsMap.get("listProperty"));
					}
					if(null != elementList && !elementList.isEmpty()){
						for(Map<String,Object> elementMap : elementList){
							Map<String,Object> element = (Map<String, Object>) elementMap.get("element");
							if(element != null){
								element = this.replaceData(element,false);
								if(element.containsKey("callbackbody")){
									String callbackbody = element.get("callbackbody").toString();
									String callbackbody_es5 = element.get("callbackbody_es5").toString();
									String callbackname = element.get("callbackname").toString();
									String funcbody = null;
									String funcbody_es5 = null;
									if(element.containsKey("funcbody")){
										funcbody = element.get("funcbody").toString() + "@@@@" + callbackbody;
										funcbody_es5 = element.get("funcbody_es5") + "@@@@" + callbackbody_es5;
									}else{
										funcbody = callbackbody;
										funcbody_es5 = callbackbody_es5;
									}
									element.put("funcbody", funcbody);
									element.put("funcbody_es5", funcbody_es5);

									String funcname = null;
									if(element.containsKey("funcname")){
										funcname = element.get("funcname").toString() + " callback='" + callbackname + "'";
									}else{
										funcname = "callback='" + callbackname + "'";;
									}
									element.put("funcname", funcname);
								}
							}else{
								elementMap = this.replaceData(elementMap,false);
								if(elementMap.containsKey("showType") && "PROPERTYATTACHMENT".equals(elementMap.get("showType").toString())
										&& null != regionType && "LISTPT".equals(regionType) ){
									String key = elementMap.get("key").toString();
									elementMap.put("key", key + "AttachementInfo");
								}
								if(elementMap.containsKey("callbackbody")){
									String callbackbody = elementMap.get("callbackbody").toString();
									String callbackbody_es5 = (String) elementMap.get("callbackbody_es5");
									String callbackname = elementMap.get("callbackname").toString();
									String funcbody = null;
									String funcbody_es5 = null;
									if(elementMap.containsKey("funcbody")){
										funcbody = elementMap.get("funcbody").toString() + "@@@@" + callbackbody;
										funcbody_es5 = elementMap.get("funcbody_es5") + "@@@@" + callbackbody_es5;
									}else{
										funcbody = callbackbody;
										funcbody_es5 = callbackbody_es5;
									}
									elementMap.put("funcbody", funcbody);
									elementMap.put("funcbody_es5", funcbody_es5);

									String funcname = null;
									if(elementMap.containsKey("funcname")){
										funcname = elementMap.get("funcname").toString() + " callback='" + callbackname + "'";
									}else{
										funcname = "callback='" + callbackname + "'";;
									}
									elementMap.put("funcname", funcname);
								}
								String columnType = (String) elementMap.get("columnType");
								if(null != columnType){
									String propertyCode = (String) elementMap.get("propertyCode");
									if(null != propertyCode){
										int startIndex = propertyCode.lastIndexOf('|');
										if(startIndex != -1){
											propertyCode = propertyCode.substring(startIndex + 1, propertyCode.length());
										}
										Property property = (Property) propertyMap.get(propertyCode);
										if(null == property){
											elementMap.put("dbColumnType", null);
											elementMap.put("columnName", null);
											elementMap.put("maxLength", null);
										}else{
											elementMap.put("onlyLeaf", property.getOnlyLeaf());
											String dbColumnType = property.getType().name();
											String columnName = property.getColumnName();
											elementMap.put("dbColumnType", dbColumnType);
											elementMap.put("columnName", columnName);
											elementMap.put("modelCode", property.getModel().getCode());
											if(null != columnType && "LONG".equals(columnType)){
												elementMap.put("maxValue", Long.MAX_VALUE + "");
												elementMap.put("minValue", Long.MIN_VALUE + "");
											}else if(null != columnType && "INTEGER".equals(columnType)){
												elementMap.put("maxValue", Integer.MAX_VALUE + "");
												elementMap.put("minValue", Integer.MIN_VALUE + "");
											}else if(null != columnType && !"LONGTEXT".equals(columnType)){
												elementMap.put("maxLength", getPropertyMaxLength(property));
											}
											if(null != columnType && "SYSTEMCODE".equals(columnType)){
												elementMap.put("multable", property.getMultable());
											}
										}
									}
								}
							}
							Boolean isPerssion =  (Boolean) elementMap.get("ispermission");
							if(isPerssion != null && isPerssion){
								String id = (String)elementMap.get("id");
								String style = (String)elementMap.get("buttonstyle");
								View view = datagrid.getView();
								String operateCode = view.getName() + "_" + id + "_" + style + "_" + view.getCode();
								elementMap.put("pc", this.getPowerCode(operateCode));
								elementMap.put("buttonoperationcode", operateCode);
							}else{
								elementMap.put("pc", this.getPowerCode(datagrid.getView().getCode() + "_self"));
							}
						}
					}

					if(null != regionType && "BUTTON".equals(regionType)){//按钮的信息==================================
						listJsonMap.put("buttons", elementList);
					}else if(null != regionType && "DATAGRID".equals(regionType) || "LISTPT".equals(regionType)){//表格
						listJsonMap.put("fields", elementList);
					}
				}
			}
			listJsonMap.putAll(pageConfig);
		}
		return listJsonMap;
	}

	private void convertListLayout2Json(View view){
		String extraConfig = ecConfigService.getEcFullConfig(view);
		if(null == extraConfig || "".equals(extraConfig)){
			return ;
		}
		Map map = (Map) XmlUtils.convert(extraConfig);
		JSONSerializer serializer = new JSONSerializer();
		Map layout = (Map) map.get("layout");
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("title", view.getTitle());
		result.put("pageType", view.getType());
		result.put("url", view.getUrl());
		result.put("hasAttachment", view.getHasAttachment());
		result.put("onlyForQuery", view.getOnlyForQuery());
		result.put("layoutType", "classic");
		List<Map> components = new ArrayList<Map>();
		result.put("components", components);
		Map component = new HashMap<String, Object>();
		component.put("type", "layout");
		component.put("layoutmethod", "row");
		components.add(component);
		List<Map> contents = new ArrayList<Map>();
		component.put("components", contents);

		//开始处理页面的布局json
		//左边的树形
		Map west = (Map) layout.get("west");
		String treeViewCode = (String) west.get("treeView");
		View treeView = this.getView(treeViewCode);
		this.convertTree2Json(treeView, view.getCode());
		String treeViewJson = treeView.getExtraView().getViewJson();
		Map<String, Object> treeViewMapComponents = (Map) JSONUtil.generateMapFromJson(treeViewJson);
		if(treeViewMapComponents.containsKey("components")){
			List<Map> layoutComponents = (List<Map>) treeViewMapComponents.get("components");
			Map<String, Object> treeViewMap = layoutComponents.get(0);
			treeViewMap.put("fix_w", west.get("width"));
			treeViewMap.put("treeViewCode", treeViewCode);
			contents.add(treeViewMap);
		}

		//右边的列表视图
		Map center = (Map) layout.get("center");
		String listViewCode = (String) center.get("vcode");
		View listView = this.getView(listViewCode);
		if(null != listView && listView.getIsShadow()){
			listView = listView.getShadowView();
		}
		this.convertList2ListJson(listView, view.getCode());
		String listViewJson = listView.getExtraView().getViewJson();
		Map listViewMap = (Map) JSONUtil.generateMapFromJson(listViewJson);
		if(listViewMap.containsKey("events")){
			result.put("events", listViewMap.get("events"));
		}
		contents.add(listViewMap);

		String json = serializer.deepSerialize(result);
		view.getExtraView().setViewJson(json);
		viewDao.mergeExtraView(view.getExtraView());
	}


	/**
	 * 获取操作的权限PC
	 * @param operateCode
	 * @return
	 */
	private String getPowerCode(String operateCode){
		if (null != operateCode && operateCode.length() > 0) {
			return "__pc__=" + new String(OrchidUtils.encode((operateCode + "|").getBytes()));
		}
		return "";
	}

	/**
	 * 根据模型查找字段对应的propertyMap
	 * @param model
	 * @return
	 */
	private Map<String, Object> getPropertyInfoMap(Model model){
		Map<String, Object> allPropertyMap = new HashMap<>();
		List<String> moduleCodes = moduleService.findModuleRelationAndReferenceCode(model.getModuleCode());
		moduleCodes.add("sysbase_1.0");

		String propertyHql = "from Property where moduleCode in (:relationCodes) and valid=true";
		List<Property> properties = viewDao.createQuery(propertyHql).setParameterList("relationCodes", moduleCodes).list();

		for (Property property : properties) {
			allPropertyMap.put(property.getCode(), property);
		}
		return allPropertyMap;
	}

	public Integer getPropertyMaxLength(Property property){
		if(null == property){
			return null;
		}
		if (property.getType() == DbColumnType.TEXT) {
			Integer maxLength = property.getMaxLength();
			return (null != maxLength && maxLength > 0) ? maxLength : 255;
		}
		if (property.getType() == DbColumnType.BAPCODE || property.getType() == DbColumnType.LAYER || property.getType() == DbColumnType.SUMMARY) {
			return 2000;
		}else if(property.getType() == DbColumnType.SYSTEMCODE || property.getType() == DbColumnType.PASSWORD ||
				property.getType() == DbColumnType.PICTURE || property.getType() == DbColumnType.TAGNUMBER ||
				property.getType() == DbColumnType.TIME || property.getType() == DbColumnType.COLOR) {
			return 255;
		}
		return null;
	}

	public void getEchartsInfo(Map<String, Object> map){
		String echartsCode = map.get("echartCode").toString();
		Echarts echarts = echartsService.findEchartsByCode(echartsCode);
		List<EchartsModel> echartsModels = echartsService.findEmodelsByEchartsCode(echartsCode);
		Map<String, Object > echartMap = new HashMap<String, Object>();
		EchartsXAxis xaxisUnit = null;
		List<EchartsYAxis> yaxisUnits = new ArrayList<EchartsYAxis>();
		if(null != echartsModels && echartsModels.size() > 0){
			for(EchartsModel echartModel: echartsModels){
				if(null == xaxisUnit){
					xaxisUnit = echartModel.getXaxis();
				}
				yaxisUnits.add(echartModel.getYaxis());
			}
		}
		echartMap.put("title", echarts.getTitle());
		echartMap.put("isFirstLoad", echarts.getIsFirstLoad());
		echartMap.put("isShowLegend", echarts.getIsShowLegend());
		echartMap.put("legendPosition", echarts.getLegendPosition());
		echartMap.put("isFirstLoad", echarts.getIsFirstLoad());
		echartMap.put("isShowMagicType", echarts.getIsShowMagicType());
		echartMap.put("xaxisUnit", xaxisUnit);
		echartMap.put("yaxisUnits", yaxisUnits);
		List<Event> events = echartsService.findEventsByEchartsCode(echartsCode);
		if(null != events && events.size() > 0){
			List<Map> exentList = new ArrayList<Map>();
			for(Event event: events){
				Map<String, Object > eventMap = new HashMap<String, Object>();
				eventMap.put("name", event.getName());
				eventMap.put("function", event.getFunction());
				exentList.add(eventMap);
			}
			echartMap.put("events", exentList);
		}
		map.put("echart", echartMap);
	}

	@Override
	public Object getDefaultDataClassific(String viewCode) {
		return dataClassificDao.findEntityByHql("from DataClassific where dataGroup.view.code = ? and valid = true and isDefault = true", viewCode);
	}

	@Override
	public List getDataClassificByViewCode(String viewCode) {
		List<DataGroup> dataGroups = dataGroupDao.findByCriteria(Restrictions.eq("valid", true), Restrictions.eq("view.code", viewCode));
		if (null != dataGroups && !dataGroups.isEmpty()) {
			for (DataGroup dg : dataGroups) {
				List<DataClassific> dcList = dataClassificDao.findByCriteria(Restrictions.eq("valid", true), Restrictions.eq("dataGroup.code", dg.getCode()));
				if (!dcList.isEmpty()) {
					dg.setDataClassifics(new LinkedHashSet<DataClassific>(dcList));
				}
			}
		}

		return dataGroups;
	}

	@Override
	public List getDataClassificByViewCode(String viewCode,String layoutName) {
		List<DataGroup> dataGroups = dataGroupDao.findByCriteria(Restrictions.eq("valid", true), Restrictions.eq("view.code", viewCode), Restrictions.eq("layoutName", layoutName));
		if (null != dataGroups && !dataGroups.isEmpty()) {
			for (DataGroup dg : dataGroups) {
				List<DataClassific> dcList = dataClassificDao.findByCriteria(Restrictions.eq("valid", true), Restrictions.eq("dataGroup.code", dg.getCode()));
				if (!dcList.isEmpty()) {
					dg.setDataClassifics(new LinkedHashSet<DataClassific>(dcList));
				}
			}
		}

		return dataGroups;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<CustomPropertyViewMapping> findShowedCustomProps(String modelCode, String associatedCode, String viewType, String propertyLayRec) {
		List<CustomPropertyViewMapping> rs = new ArrayList<CustomPropertyViewMapping>();
		String hql = "";
		List<Object> params = new ArrayList<Object>();
		params.add(associatedCode);
		if ("EDIT".equals(viewType)) {
			hql = "from CustomPropertyViewMapping c where c.associatedCode = ? and c.propertyLayRec is null and c.property.valid = true order by c.id";
		} else {
			//hql = "from CustomPropertyViewMapping c where c.associatedCode = ? and c.propertyLayRec is null and c.showCustom = true and c.property.valid = true order by c.id";
			hql = "from CustomPropertyViewMapping c where c.associatedCode = ? and c.propertyLayRec = ? and c.showCustom = true and c.property.valid = true order by c.id";
			params.add(propertyLayRec);
		}
		List<CustomPropertyViewMapping> viewMappings = customPropertyViewMappingDao.findByHql(hql, params.toArray());
		viewMappings = viewMappings == null ? new ArrayList<CustomPropertyViewMapping>() : viewMappings;
		boolean isSort = false;
		Map<String, CustomPropertyViewMapping> cmap = new HashMap<String, CustomPropertyViewMapping>();
		for (CustomPropertyViewMapping c : viewMappings) {
			if (c.getSort() != null) {
				isSort = true;
			}
			cmap.put(c.getProperty().getCode(), c);
		}
		List<CustomPropertyModelMapping> modelMappings = customPropertyModelMappingDao.findByHql(
				"from CustomPropertyModelMapping c where c.model.code = ? and c.enableCustom = true and c.property.valid = true order by c.id", new Object[] { modelCode });
		modelMappings = modelMappings == null ? new ArrayList<CustomPropertyModelMapping>() : modelMappings;
		for (CustomPropertyModelMapping m : modelMappings) {
			CustomPropertyViewMapping c = cmap.get(m.getProperty().getCode());
			if (c != null && c.getShowCustom() != null && c.getShowCustom()) {
				c.setFieldType(m.getFieldType());
				c.setFormat(m.getFormat());
				Property p = c.getProperty();
				p.setFillcontent(m.getFillContent());
				p.setMultable(m.getMultable());
				p.setSeniorSystemCode(m.getSeniorSystemCode());
				if (m.getAssociatedProperty() != null) {
					Hibernate.initialize(m.getAssociatedProperty());
					Hibernate.initialize(m.getAssociatedProperty().getModel());
					Hibernate.initialize(m.getAssociatedProperty().getModel().getProperties());
				}
				p.setAssociatedProperty(m.getAssociatedProperty());
				p.setAssociatedType(m.getAssociatedType());
				if (m.getRefView() != null) {
					Hibernate.initialize(m.getRefView());
					c.setRefView(m.getRefView());
				}
				c.setRelatedKey(m.getRelatedKey());
				if (!isSort) {
					c.setSort(m.getSort());
				}
				if ("LIST".equals(viewType)) {
					String propLayRec = c.getPropertyLayRec().split("\\|\\|")[0];
					if (!propLayRec.contains(".")) {
						c.setPropertyLayRec("attrMap." + p.getName());
					} else {
						propLayRec = propLayRec.substring(propLayRec.indexOf(".") + 1);
						c.setPropertyLayRec("attrMap.cp_" + (propLayRec + "." + p.getName()).replace(".", "_"));
					}
				} else if ("DATAGRID".equals(viewType)) {
					String propLayRec = c.getPropertyLayRec().split("\\|\\|")[0];
					if (!propLayRec.contains(".")) {
						c.setPropertyLayRec(p.getName());
					} else {
						propLayRec = propLayRec.substring(propLayRec.indexOf(".") + 1);
						c.setPropertyLayRec(propLayRec + "." + p.getName());
					}
				}
				Map excelFormatMap = generateStyle(p);
				if (null != excelFormatMap && null != excelFormatMap.get("format") && !excelFormatMap.get("format").toString().isEmpty()) {
					c.setExcelFormat(excelFormatMap.get("format").toString());
				}
				rs.add(c);
			} else if (c == null) {
				if ("EDIT".equals(viewType)) {
					c = new CustomPropertyViewMapping();
					c.setDisplayName(m.getDisplayName());
					c.setFieldType(m.getFieldType());
					c.setFormat(m.getFormat());
					c.setNullable(m.getNullable());
					c.setPrecision(m.getPrecision());
					Property p = m.getProperty();
					p.setFillcontent(m.getFillContent());
					p.setMultable(m.getMultable());
					p.setSeniorSystemCode(m.getSeniorSystemCode());
					if (m.getAssociatedProperty() != null) {
						Hibernate.initialize(m.getAssociatedProperty());
						Hibernate.initialize(m.getAssociatedProperty().getModel());
						Hibernate.initialize(m.getAssociatedProperty().getModel().getProperties());
					}
					p.setAssociatedProperty(m.getAssociatedProperty());
					p.setAssociatedType(m.getAssociatedType());
					c.setProperty(p);
					if (m.getRefView() != null) {
						Hibernate.initialize(m.getRefView());
						c.setRefView(m.getRefView());
					}
					c.setRelatedKey(m.getRelatedKey());
					c.setSort(m.getSort());
					rs.add(c);
				}
			}
		}
		Collections.sort(rs, new Comparator<CustomPropertyViewMapping>() {
			@Override
			public int compare(CustomPropertyViewMapping o1, CustomPropertyViewMapping o2) {
				if (o1.getSort() != null && o2.getSort() != null) {
					return o1.getSort() - o2.getSort();
				} else if (o1.getSort() != null && o2.getSort() == null) {
					return -1;
				} else if (o1.getSort() == null && o2.getSort() != null) {
					return 1;
				} else if (o1.getId() != null && o2.getId() != null) {
					return Integer.parseInt(String.valueOf(o1.getId() - o2.getId()));
				} else {
					return 0;
				}
			}
		});
		return rs;
	}

	public Map<String,Object>  generateStyle(Property p)  {
		Map<String, Object> formatMap = new HashMap<String, Object>();
		if (true) {
			if ("DECIMAL".equals(p.getType().toString())) {
				String decimalNum = null;
				if (p.getDecimalNum() != null) {
					decimalNum = (p.getDecimalNum()).toString();
				}
				int number = 2;
				if (decimalNum != null && decimalNum.length() > 0) {
					try {
						number = Integer.parseInt(decimalNum);
					} catch (NumberFormatException e) {
						log.warn(e.getMessage());
						number = 2;
					}
				}
				String showFormat = p.getFormat().toString();
				if ("PERCENT".equals(showFormat)) {
					number = number - 2;
				}

				StringBuffer sb = new StringBuffer();
				if (number == 0) {
					sb.append("0");
				} else if (number > 0) {
					sb.append("0.");
				} else if (number < 0) {
					sb.append("0");
				}
				for (int i = 0; i < number; i++) {
					sb.append("0");
				}
				if ("PERCENT".equals(showFormat)) {
					sb.append("%");
				} else {
					sb.append("");
				}
				formatMap.put("format", sb.toString());
				formatMap.put("key", p.getCode());
			}else if("DATETIME".equals(p.getType().toString())||"DATE".equals(p.getType().toString()))  {
				if(p.getFormat()!=null)  {
					String dateFormat=p.getFormat().toString();
					String  sb =new String();
					switch(dateFormat)  {
						case "Y":
						{
							sb="yyyy";
							break;
						}
						case "YM":{
							sb="yyyy-mm";
							break;
						}
						case "YMD":{
							sb="yyyy-mm-dd";
							break;
						}
						case "YMD_H":{
							sb="yyyy-mm-dd\\ HH";
							break;
						}
						case "YMD_HM":{
							sb="yyyy-mm-dd\\ HH:mm";
							break;
						}case "YMD_HMS":{
							sb="yyyy-mm-dd\\ HH:mm:ss";
							break;
						}default:{
							sb="yyyy-mm-dd\\ HH:mm:ss";
							break;
						}
					}
					formatMap.put("format", sb.toString());
					formatMap.put("key", p.getCode());
				}

			}else if ("INTEGER".equals(p.getType().toString())) {
				String showFormat = p.getFormat().toString();
				formatMap.put("key", p.getCode());
				if ("PERCENT".equals(showFormat)) {
					formatMap.put("format", "0%");
				}
			} else if ("MONEY".equals(p.getType().toString())) {
				String decimalNum = null;
				if (p.getDecimalNum() != null) {
					decimalNum = (p.getDecimalNum()).toString();
				}
				int number = 2;
				if (decimalNum != null && decimalNum.length() > 0) {
					try {
						number = Integer.parseInt(decimalNum);
					} catch (NumberFormatException e) {
						log.warn(e.getMessage());
						number = 2;
					}
				}
				String showFormat =  p.getFormat().toString();
				StringBuffer sb = new StringBuffer();
				if ("THOUSAND".equals(showFormat)) {
					sb.append("#,##");
				}
				sb.append("0");
				if (number > 0) {
					sb.append(".");
				}
				for (int i = 0; i < number; i++) {
					sb.append("0");
				}
				formatMap.put("format", sb.toString());
				formatMap.put("key", p.getCode());
			} else {
				formatMap.put("key", p.getCode());
			}
		}
		return formatMap;
	}

	@Override
	public List<CustomPropertyModelMapping> findCustomPropertyForAsso(String modelCode ,String property) {
		List<CustomPropertyModelMapping> list = customPropertyModelMappingDao.findByHql(
				"from CustomPropertyModelMapping c where c.model.code = ? and c.property.code=? and c.enableCustom=true and c.property.valid = true  order by c.sort,c.enableCustom desc,c.id", new Object[] { modelCode ,property});
		return list;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Property> getEnabledCustomProps(String modelCode) {
		List<Property> propList = new ArrayList<Property>();
		List<CustomPropertyModelMapping> modelMappings = modelDao.findByHql(
				"from CustomPropertyModelMapping c where c.model.code = ? and c.enableCustom = true and c.property.valid = true", new Object[] { modelCode });
		if (modelMappings != null && modelMappings.size() > 0) {
			for (CustomPropertyModelMapping m : modelMappings) {
				Property p = m.getProperty();
				p.setDisplayName(m.getDisplayName());
				p.setFieldType(m.getFieldType());
				p.setFormat(m.getFormat());
				p.setFillcontent(m.getFillContent());
				p.setMultable(m.getMultable());
				p.setSeniorSystemCode(m.getSeniorSystemCode());
				p.setAssociatedProperty(m.getAssociatedProperty());
				p.setAssociatedType(m.getAssociatedType());
				Hibernate.initialize(p.getModel());
				if (m.getAssociatedProperty() != null) {
					Hibernate.initialize(m.getAssociatedProperty());
					Hibernate.initialize(m.getAssociatedProperty().getModel());
					p.setAssociatedProperty(m.getAssociatedProperty());
				}
				propList.add(p);
			}
		}
		return propList;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public String findPropDisplayName(String propLayRec,String modelCode,String... env) {
		String[] propertyLayRec = null, firstProperty = null;
		if(null != modelCode && !modelCode.equals("")){
			Model model = modelService.getModel(modelCode);
			if (null != propLayRec && !propLayRec.equals("")) {
				propertyLayRec = propLayRec.split("\\|\\|");
				firstProperty = propertyLayRec[0].split("\\.");
				if (firstProperty.length > 1) {
					Model m = modelService.getModel(toUpperCaseFirstOne(firstProperty[firstProperty.length-2]));
					String propDisplayName = "";
					if(m != null){
						propDisplayName = InternationalResource.get(m.getName());
					}else{
						if(firstProperty.length>2){
							List<Property> p = modelDao
									.findByHql(
											"from Property c where c.code like ? and c.valid = true",
											new Object[] { "%"+ toUpperCaseFirstOne(firstProperty[firstProperty.length-3]) + "_" +firstProperty[firstProperty.length-2] });
							if(p != null && p.size() > 0){
								propDisplayName = InternationalResource.get(p.get(0).getDisplayName());
							}
						}else{
							return InternationalResource.get(model.getName());
						}
					}
					return propDisplayName + "." + InternationalResource.get(model.getName());
				} else {
					return InternationalResource.get(model.getName());
				}
			} else {
				return InternationalResource.get(model.getName());
			}
		}
		return null;
	}

	private static String toUpperCaseFirstOne(String s) {
		if (Character.isUpperCase(s.charAt(0)))
			return s;
		else
			return (new StringBuilder()).append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).toString();
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<CustomPropertyViewMapping> findCustomPropertyForSecret(String modelCode, String associatedCode, String viewType, String propertyLayRec) {
		List<CustomPropertyViewMapping> rs = new ArrayList<CustomPropertyViewMapping>();
		String hql = "";
		List<Object> params = new ArrayList<Object>();
		params.add(associatedCode);
		if ("EDIT".equals(viewType)) {
			hql = "from CustomPropertyViewMapping c where c.associatedCode = ? and c.propertyLayRec is null and c.property.valid = true order by c.id";
		} else {
			hql = "from CustomPropertyViewMapping c where c.associatedCode = ? and c.propertyLayRec is null and c.showCustom = true and c.property.valid = true order by c.id";
			//params.add(propertyLayRec);
		}
		List<CustomPropertyViewMapping> viewMappings = customPropertyViewMappingDao.findByHql(hql, params.toArray());
		viewMappings = viewMappings == null ? new ArrayList<CustomPropertyViewMapping>() : viewMappings;
		boolean isSort = false;
		Map<String, CustomPropertyViewMapping> cmap = new HashMap<String, CustomPropertyViewMapping>();
		for (CustomPropertyViewMapping c : viewMappings) {
			if (c.getSort() != null) {
				isSort = true;
			}
			cmap.put(c.getProperty().getCode(), c);
		}
		List<CustomPropertyModelMapping> modelMappings = customPropertyModelMappingDao.findByHql(
				"from CustomPropertyModelMapping c where c.model.code = ? and c.enableCustom = true and c.property.valid = true order by c.id", new Object[] { modelCode });
		modelMappings = modelMappings == null ? new ArrayList<CustomPropertyModelMapping>() : modelMappings;
		for (CustomPropertyModelMapping m : modelMappings) {
			CustomPropertyViewMapping c = cmap.get(m.getProperty().getCode());
			if (c != null && c.getShowCustom() != null && c.getShowCustom()) {
				c.setFieldType(m.getFieldType());
				c.setFormat(m.getFormat());
				Property p = c.getProperty();
				p.setFillcontent(m.getFillContent());
				p.setMultable(m.getMultable());
				if(m.getPrecision()!=null){
					c.setPrecision(m.getPrecision());
					p.setDecimalNum(m.getPrecision());
				}
				p.setSeniorSystemCode(m.getSeniorSystemCode());
				if (m.getAssociatedProperty() != null) {
					Hibernate.initialize(m.getAssociatedProperty());
					Hibernate.initialize(m.getAssociatedProperty().getModel());
					Hibernate.initialize(m.getAssociatedProperty().getModel().getProperties());
				}
				p.setAssociatedProperty(m.getAssociatedProperty());
				p.setAssociatedType(m.getAssociatedType());
				if (m.getRefView() != null) {
					Hibernate.initialize(m.getRefView());
					c.setRefView(m.getRefView());
				}
				c.setRelatedKey(m.getRelatedKey());
				if (!isSort) {
					c.setSort(m.getSort());
				}
				if ("LIST".equals(viewType)) {
					//String propLayRec = c.getPropertyLayRec().split("\\|\\|")[0];
					//if (!propLayRec.contains(".")) {
					c.setPropertyLayRec(p.getName());
					// String code, Object value
					//String code = c.getProperty().getCode() ;
					//Object id = 1000;
					//String displayValue = modelServiceFoundation.getMainDisplayValue(code, id);
					//} else {
					//propLayRec = propLayRec.substring(propLayRec.indexOf(".") + 1);
					//c.setPropertyLayRec("attrMap.cp_" + (propLayRec + "." + p.getName()).replace(".", "_"));
					//}
				} else if ("DATAGRID".equals(viewType)) {
					String propLayRec = c.getPropertyLayRec().split("\\|\\|")[0];
					if (!propLayRec.contains(".")) {
						c.setPropertyLayRec(p.getName());
					} else {
						propLayRec = propLayRec.substring(propLayRec.indexOf(".") + 1);
						c.setPropertyLayRec(propLayRec + "." + p.getName());
					}
				}
				Map excelFormatMap = generateStyle(p);
				if (null != excelFormatMap && null != excelFormatMap.get("format") && !excelFormatMap.get("format").toString().isEmpty()) {
					c.setExcelFormat(excelFormatMap.get("format").toString());
				}
				rs.add(c);
			} else if (c == null) {
				if ("EDIT".equals(viewType)) {
					c = new CustomPropertyViewMapping();
					c.setDisplayName(m.getDisplayName());
					c.setFieldType(m.getFieldType());
					c.setFormat(m.getFormat());
					c.setNullable(m.getNullable());
					Property p = m.getProperty();
					p.setFillcontent(m.getFillContent());
					p.setMultable(m.getMultable());
					p.setSeniorSystemCode(m.getSeniorSystemCode());
					if (m.getAssociatedProperty() != null) {
						Hibernate.initialize(m.getAssociatedProperty());
						Hibernate.initialize(m.getAssociatedProperty().getModel());
						Hibernate.initialize(m.getAssociatedProperty().getModel().getProperties());
					}
					p.setAssociatedProperty(m.getAssociatedProperty());
					p.setAssociatedType(m.getAssociatedType());
					c.setProperty(p);
					if (m.getRefView() != null) {
						Hibernate.initialize(m.getRefView());
						c.setRefView(m.getRefView());
					}
					c.setRelatedKey(m.getRelatedKey());
					c.setSort(m.getSort());
					rs.add(c);
				}
			}
		}
		Collections.sort(rs, new Comparator<CustomPropertyViewMapping>() {
			@Override
			public int compare(CustomPropertyViewMapping o1, CustomPropertyViewMapping o2) {
				if (o1.getSort() != null && o2.getSort() != null) {
					return o1.getSort() - o2.getSort();
				} else if (o1.getSort() != null && o2.getSort() == null) {
					return -1;
				} else if (o1.getSort() == null && o2.getSort() != null) {
					return 1;
				} else if (o1.getId() != null && o2.getId() != null) {
					return Integer.parseInt(String.valueOf(o1.getId() - o2.getId()));
				} else {
					return 0;
				}
			}
		});
		return rs;
	}

	@Override
	@Transactional
	public void saveConfig(View view, ExtraView ev, Map<String, Object> argsMap) {

		Boolean hasCustomSection = Boolean.valueOf(argsMap.get("hasCustomSection").toString());
		if (!hasCustomSection
				&& (ViewType.EDIT.equals(view.getType())
				|| ViewType.VIEW.equals(view.getType()) || ViewType.EXTRA
				.equals(view.getType()))) {
			List<DataGrid> dgs = dataGridService.getDataGridByView(view, false);
			if (dgs != null && dgs.size() > 0) {
				outer:
				for (DataGrid dg : dgs) {
					List<Field> fList = dg.getFields();
					if (fList != null && fList.size() > 0) {
						for (Field f : fList) {
							if (f.getCode().contains("_DATAGRID_CUSTOM_")) {
								hasCustomSection = true;
								break outer;
							}
						}
					}
				}
			}
		}
		view.setHasCustomSection(hasCustomSection);
		// 移动视图，保存后才启用
		if (view.getMobile() != null && view.getMobile()) {
			if (view.getMobileEnableFlag() == null
					|| !view.getMobileEnableFlag()) {
				view.setMobileEnableFlag(Boolean.TRUE);
			}
		}
		saveView(view);
		modifyShadowViewCustomSection(view.getCode(),
				view.getHasCustomSection());

		if (ev.getView().getFastQueryJson() != null) {
			ev.getView().setFqj(ev.getView().getFastQueryJson().get(0));
		}
		if (ev.getView().getAdvQueryJson() != null) {
			ev.getView().setAqj(ev.getView().getAdvQueryJson().get(0));
		}
	}

	@Override
	public List<View> findViewByUrl(String url) {
		List<View> views = viewDao.findByCriteria(Restrictions.eq("url", url));
		return views;
	}
	@Override
	public void saveEntity(Entity entity) {
		entityDao.save(entity);
		entityDao.flush();
		entityDao.clear();
	}
	@Override
	public void deleteCustomPropertyViewMappingsForImport(String moduleCode) {
		Query query=customPropertyViewMappingDao.createQuery("delete CustomPropertyViewMapping c where  c.property.code like ?0", moduleCode+"%");
		query.executeUpdate();
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void saveCustomPropertyViewMapping(CustomPropertyViewMapping viewMapping) {
		CustomPropertyViewMapping vm = null;
		if (viewMapping.getPropertyLayRec() != null && viewMapping.getPropertyLayRec().length() > 0) {
			vm = customPropertyViewMappingDao.findEntityByHql(
					"from CustomPropertyViewMapping c where c.property.code = ?0 and c.associatedCode = ?1 and c.propertyLayRec = ?2", new Object[] {
							viewMapping.getProperty().getCode(), viewMapping.getAssociatedCode(), viewMapping.getPropertyLayRec() });
		} else {
			vm = customPropertyViewMappingDao.findEntityByHql(
					"from CustomPropertyViewMapping c where c.property.code = ?0 and c.associatedCode = ?1 and c.propertyLayRec is null", new Object[] {
							viewMapping.getProperty().getCode(), viewMapping.getAssociatedCode() });
		}
		if (vm != null) {
			vm.setDisplayName(viewMapping.getDisplayName());
			vm.setNullable(viewMapping.getNullable());
			vm.setShowCustom(viewMapping.getShowCustom());
			vm.setColspan(viewMapping.getColspan());
			vm.setTextareaRow(viewMapping.getTextareaRow());
			vm.setReadonly(viewMapping.getReadonly());
			vm.setAlign(viewMapping.getAlign());
			vm.setPrecision(viewMapping.getPrecision());
			vm.setLength(viewMapping.getLength());
		} else {
			vm = viewMapping;
		}
		customPropertyViewMappingDao.save(vm);
	}
	@Override
	public List<CustomPropertyViewMapping> findCustomPropertyViewMappingsForExport(
			String viewCode) {
		List<CustomPropertyViewMapping> viewMappings = customPropertyViewMappingDao.findByHql(
				"from CustomPropertyViewMapping c where c.associatedCode = ?0 and c.id is not null ", new Object[] { viewCode });
		return viewMappings;
	}

	private List<String> getCheckMsg(List<View> views, List<DataGrid> dataGrids, Boolean isPro){
		List<String> msgs = new LinkedList<String>();
		if(null != views && !views.isEmpty()){
			for(View view : views){
				StringBuilder sb = new StringBuilder(InternationalResource.get(view.getEntity().getModule().getName())).append("模块-");
				sb.append(InternationalResource.get(view.getEntity().getName())).append("实体-");
				if(view.getName().endsWith("__mobile__")){
					sb.append(view.getName().replace("__mobile__", "")).append("移动视图");
				} else {
					sb.append(view.getName()).append("视图");
				}
				if (isPro) {
					sb.append("(工程期)");
				}
				msgs.add(sb.toString());
			}
		}
		if(null != dataGrids && !dataGrids.isEmpty()){
			for(DataGrid dg : dataGrids){
				if(null == dg.getView()){
					continue;
				}
				StringBuilder sb = new StringBuilder(InternationalResource.get(dg.getView().getEntity().getModule().getName())).append("模块-");
				sb.append(InternationalResource.get(dg.getView().getEntity().getName())).append("实体-");
				if(dg.getView().getName().endsWith("__mobile__")){
					sb.append(dg.getView().getName().replace("__mobile__", "")).append("移动视图-").append(dg.getName());
				} else {
					sb.append(dg.getView().getName()).append("视图-").append(dg.getName());
				}
				if (isPro) {
					sb.append("(工程期)");
				}
				msgs.add(sb.toString());
			}
		}
		return msgs;
	}
}
