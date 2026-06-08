package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.dao.CustomerConditionDaoImpl;
import com.supcon.supfusion.configuration.services.service.CustomerConditionService;
import com.supcon.supfusion.configuration.services.service.DataGridService;
import com.supcon.supfusion.configuration.services.service.ViewService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@ServiceApiService("ec_CustomerConditionService")
@Transactional
public class CustomerConditionServiceImpl implements CustomerConditionService, InitializingBean {
	private static final Logger generateLogger = LoggerFactory.getLogger("bap.ec.generator");

	@Autowired
	private DataGridService dataGridService;

	@Autowired
	private ViewService viewService;

	@Autowired
	private CustomerConditionDaoImpl customerConditionDao;
	

	@Override
	public void afterPropertiesSet() throws Exception {
//			cache = cacheAdmin.getCache("EC_VIEW_CACHE");
	}

	@Override
	public void saveCustomerCondition(CustomerCondition condition) {
		// TODO Auto-generated method stub
		CustomerCondition old = null;
		if (condition != null && condition.getDataClassific() != null) {
			old = getCustomerCondition(condition.getDataClassific());
//			cache.remove("customerCondition_dataclassific_" + condition.getDataClassific().getCode());
		} else if (condition != null && condition.getDataGrid() != null) {
			old = getCustomerCondition(condition.getDataGrid());
//			cache.remove("customerCondition_datagrid_" + condition.getDataGrid().getCode());
		} else if (condition != null && condition.getView() != null) {
			old = getCustomerCondition(condition.getView());
//			cache.remove("customerCondition_view_" + condition.getView().getCode());
		}

		if (old != null) {
			old.setJsonCondition(condition.getJsonCondition());
			old.setSql(condition.getSql());
			old.setProjFlag(condition.getProjFlag());
			customerConditionDao.save(old);
		} else {
			customerConditionDao.save(condition);
		}

	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public CustomerCondition getCustomerCondition(View view) {
		// TODO Auto-generated method stub
		List<CustomerCondition> result = customerConditionDao.findByHql("From CustomerCondition where valid=true and view = ? and dataClassific is null",
				view);
		if (result != null && result.size() > 0) {
			return result.get(0);
		} else {
			return null;
		}
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public CustomerCondition getCustomerCondition(DataGrid datagrid) {
		// TODO Auto-generated method stub
		List<CustomerCondition> result = customerConditionDao.findByHql("From CustomerCondition where valid=true and dataGrid = ?0", datagrid);
		if (result != null && result.size() > 0) {
			return result.get(0);
		} else {
			return null;
		}

	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public CustomerCondition getCustomerCondition(DataClassific dataClassific) {
		// TODO Auto-generated method stub
		List<CustomerCondition> result = customerConditionDao.findByHql("From CustomerCondition where valid=true and dataClassific = ?0", dataClassific);
		if (result != null && result.size() > 0) {
			return result.get(0);
		} else {
			return null;
		}
	}

	@Override
	public CustomerCondition getCustomerConditionByDataGridCode(String dataGridCode) {
		// TODO Auto-generated method stub
		DataGrid dg = dataGridService.getDataGrid(dataGridCode);
		return getCustomerCondition(dg);
	}

	@Override
	public CustomerCondition getCustomerConditionByViewCode(String viewCode) {
		// TODO Auto-generated method stub
		View view = viewService.getView(viewCode);
		return getCustomerCondition(view);
	}

	@Override
	public CustomerCondition getCustomerConditionByClassificCode(String classificCode) {
		DataClassific fic = viewService.getDataClassific(classificCode);
		return getCustomerCondition(fic);
	}

	/**
	 * 根据传入对象物理删除自定义条件
	 * 
	 * @param object
	 *            只能为 {@link View} {@link DataGrid} {@link DataClassific}
	 */
	@Override
	@Transactional
	public void deletePhysicalByObject(Object object) {
		if (object != null) {
			List<CustomerCondition> conditions = null;
			if (object instanceof View) {
				conditions = customerConditionDao.findByCriteria(Restrictions.eq("view", (View) object));
			} else if (object instanceof DataGrid) {
				conditions = customerConditionDao.findByCriteria(Restrictions.eq("dataGrid", (DataGrid) object));
			} else if (object instanceof DataClassific) {
				conditions = customerConditionDao.findByCriteria(Restrictions.eq("dataClassific", (DataClassific) object));
			}
			if (conditions != null && !conditions.isEmpty()) {
				for (CustomerCondition condition : conditions) {
					customerConditionDao.deletePhysical(condition);
				}
			}
		}
	}

	/**
	 * 根据传入对象逻辑删除自定义条件
	 * 
	 * @param object
	 *            只能为 {@link View} {@link DataGrid} {@link DataClassific}
	 */
	@Override
	@Transactional
	public void deleteByObject(Object object) {
		if (object != null) {
			List<CustomerCondition> conditions = null;
			if (object instanceof View) {
				conditions = customerConditionDao.findByCriteria(Restrictions.eq("view", (View) object));
			} else if (object instanceof DataGrid) {
				conditions = customerConditionDao.findByCriteria(Restrictions.eq("dataGrid", (DataGrid) object));
			} else if (object instanceof DataClassific) {
				conditions = customerConditionDao.findByCriteria(Restrictions.eq("dataClassific", (DataClassific) object));
			}
			if (conditions != null && !conditions.isEmpty()) {
				for (CustomerCondition condition : conditions) {
					customerConditionDao.delete(condition);
				}
			}
		}
	}

	/**
	 * 根据{@link CustomerCondition}.code的起始部分查询{@link CustomerCondition}
	 * @param startCode code起始部分   完整的code不要用此方法
	 * @return
	 */
	@Transactional
	@Override
	public List<CustomerCondition> findCustomerConditionsByCode(String startCode) {
		if (startCode != null && startCode.length() > 0) {
			Set<CustomerCondition> result = new LinkedHashSet<CustomerCondition>();
//			List<CustomerCondition> conditions = customerConditionDao.findByCriteria(Restrictions.eq("valid", true),
//					Restrictions.like("code", startCode + "_", MatchMode.START));
			//List<CustomerCondition> conditions = customerConditionDao.findByHql(" from CustomerCondition cc where cc.valid = ? and cc.code like ? and ((cc.view is null or cc.view.valid = ?) and (cc.dataGrid is null or cc.dataGrid.valid = ?) and (cc.dataClassific is null or cc.dataClassific.valid = ?))", new Object[]{true, startCode + "_%",true,true,true});
			List<CustomerCondition> conditions = customerConditionDao.findByHql(" from CustomerCondition cc where cc.valid = ?0 and cc.code like ?1 and (cc.view is null or ((cc.dataGrid is null and cc.dataClassific is null) and cc.view.valid = ?2 and cc.view.customFlag = ?3))", new Object[]{true, startCode + "_%",true, false});
			result.addAll(conditions);
			conditions = customerConditionDao.findByHql(" from CustomerCondition cc where cc.valid = ?0 and cc.code like ?1 and (cc.dataGrid is null or (cc.dataGrid.valid = ?2 and cc.dataGrid.view.customFlag = ?3))", new Object[]{true, startCode + "_%",true, false});
			result.addAll(conditions);
			conditions = customerConditionDao.findByHql(" from CustomerCondition cc where cc.valid = ?0 and cc.code like ?1 and (cc.dataClassific is null or (cc.dataClassific.valid = ?2 and cc.dataClassific.dataGroup.view.customFlag = ?3))", new Object[]{true, startCode + "_%",true,false});
			result.addAll(conditions);
			
			for (Iterator<CustomerCondition> it = result.iterator(); it.hasNext();) {
				CustomerCondition cc = it.next();
				View view = null;
				if (null != cc && null != cc.getDataClassific() && null != cc.getDataClassific().getDataGroup()) {
					view = cc.getDataClassific().getDataGroup().getView();
				} else if (null != cc && null != cc.getDataGrid()) {
					view = cc.getDataGrid().getView();
				} else if (null != cc && null != cc.getView()) {
					view = cc.getView();
				}
				if(null != view && null != view.getMobile() && view.getMobile() && (null == view.getMobileEnableFlag() || (null != view.getMobileEnableFlag() && !view.getMobileEnableFlag()))) {
					it.remove();
				}
			}
			return new ArrayList<>(result);
		}
		return null;
	}
	
	public static List<CustomerCondition> findCustomerConditionsByCode(Session session, String startCode,
                                                                       List<View> views, List<DataClassific> dataClassifics, List<DataGrid> dataGrids, List<DataGroup> dataGroups, String... moduleCodes) {
		if (startCode != null && startCode.length() > 0 || null != moduleCodes ) {
			long start = System.currentTimeMillis();
			Set<CustomerCondition> result = new LinkedHashSet<CustomerCondition>();
//			List<CustomerCondition> conditions = customerConditionDao.findByCriteria(Restrictions.eq("valid", true),
//					Restrictions.like("code", startCode + "_", MatchMode.START));
			//List<CustomerCondition> conditions = customerConditionDao.findByHql(" from CustomerCondition cc where cc.valid = ? and cc.code like ? and ((cc.view is null or cc.view.valid = ?) and (cc.dataGrid is null or cc.dataGrid.valid = ?) and (cc.dataClassific is null or cc.dataClassific.valid = ?))", new Object[]{true, startCode + "_%",true,true,true});
			StringBuilder sb=new StringBuilder("");
			if(null != moduleCodes && moduleCodes.length > 0){
				for(String moduleCode : moduleCodes){
					sb.append("or cc.code like '").append(moduleCode).append("_%'");
				}
			}
			String sql = "";
			if (null != moduleCodes && moduleCodes.length > 0) {
				sql = " from CustomerCondition cc where cc.valid = ? and (cc.view is null or ((cc.dataGrid is null and cc.dataClassific is null) and cc.view.valid = ? and cc.view.customFlag = ?)) and ("+sb.substring(3)+")";
			} else {
				sql = " from CustomerCondition cc where cc.valid = ? and cc.code like '"+ startCode + "_%" +"' and (cc.view is null or ((cc.dataGrid is null and cc.dataClassific is null) and cc.view.valid = ? and cc.view.customFlag = ?))";
			}
			
			List<CustomerCondition> conditions = session.createQuery(sql).setParameter(0, true).setParameter(1, true).setParameter(2, false).setReadOnly(true).list();
			result.addAll(conditions);
			if (null != moduleCodes && moduleCodes.length > 0) {
				sql = " from CustomerCondition cc where cc.valid = ?0 and (cc.dataGrid is null or (cc.dataGrid.valid = ?1 and cc.dataGrid.view.customFlag = ?2)) and ("+sb.substring(3)+")";
			} else {
				sql = " from CustomerCondition cc where cc.valid = ?0 and cc.code like '"+ startCode + "_%" +"' and (cc.dataGrid is null or (cc.dataGrid.valid = ?1 and cc.dataGrid.view.customFlag = ?2))";
			}
			
			conditions = session.createQuery(sql).setParameter(0, true).setParameter(1, true).setParameter(2, false).setReadOnly(true).list();
			result.addAll(conditions);
			if (null != moduleCodes && moduleCodes.length > 0) {
				sql = " from CustomerCondition cc where cc.valid = ?0 and (cc.dataClassific is null or (cc.dataClassific.valid = ?1 and cc.dataClassific.dataGroup.view.customFlag = ?2)) and ("+sb.substring(3)+")";
			} else {
				sql = " from CustomerCondition cc where cc.valid = ?0 and cc.code like '"+ startCode + "_%" +"' and (cc.dataClassific is null or (cc.dataClassific.valid = ?1 and cc.dataClassific.dataGroup.view.customFlag = ?1))";
			}
			
			conditions = session.createQuery(sql).setParameter(0, true).setParameter(1, true).setParameter(2, false).setReadOnly(true).list();
			result.addAll(conditions);
			
			for (Iterator<CustomerCondition> it = result.iterator(); it.hasNext();) {
				CustomerCondition cc = it.next();
				View view = null;
				if (null != cc && null != cc.getDataClassific() ) {
					DataClassific dataClassific = (DataClassific) findInList(cc.getDataClassific().getCode(), dataClassifics);
					DataGroup dataGroup = (DataGroup) findInList(dataClassific.getDataGroup().getCode(), dataGroups);
					view = (View) findInList(dataGroup.getView().getCode(), views);
				} else if (null != cc && null != cc.getDataGrid()) {
					DataGrid dataGrid = (DataGrid) findInList(cc.getDataGrid().getCode(), dataGrids);
					view = (View) findInList(dataGrid.getView().getCode(), views);
				} else if (null != cc && null != cc.getView()) {
					view = (View) findInList(cc.getView().getCode(), views);
				}
				if(null != view && null != view.getMobile() && view.getMobile() && (null == view.getMobileEnableFlag() || (null != view.getMobileEnableFlag() && !view.getMobileEnableFlag()))) {
					it.remove();
				}
			}
			long end = System.currentTimeMillis();
			generateLogger.debug("Load {} casted {}ms.", CustomerCondition.class.getSimpleName(), (end - start));
			return new ArrayList<>(result);
		}
		return Collections.EMPTY_LIST;
	}
	
	public static Object findInList(String code, List<? extends AbstractAuditUniqueCodeEntity> entities){
		for(AbstractAuditUniqueCodeEntity entity : entities) {
			if(code.equals(entity.getCode())) {
				return entity;
			}
		}
		return null;
	}
	
	/**
	 * 根据{@link CustomerCondition}.code的查询{@link CustomerCondition}
	 * 一级缓存
	 * @param code 
	 * @return {@link CustomerCondition}
	 */
	@Transactional
	@Override
	public CustomerCondition findCustomerCondition(String code) {
		if (code != null && code.length() > 0) {
			return customerConditionDao.load(code);
		}
		return null;
	}
	
	/**
	 * 根据条件查询CustomerCondition
	 * @param criterions
	 * @return
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<CustomerCondition> findCustomerConditions(Criterion... criterions) {
		return customerConditionDao.findByCriteria(criterions);
	}
}
