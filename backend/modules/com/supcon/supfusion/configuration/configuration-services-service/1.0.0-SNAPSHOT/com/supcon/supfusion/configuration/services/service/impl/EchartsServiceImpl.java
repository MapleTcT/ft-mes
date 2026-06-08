package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.services.BaseServiceImpl;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.utils.EchartsUtils;
import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.configuration.services.utils.StringUtils;
import com.supcon.supfusion.configuration.services.dao.EchartsDaoImpl;
import com.supcon.supfusion.configuration.services.dao.EchartsModelDaoImpl;
import com.supcon.supfusion.configuration.services.service.ConditionService;
import com.supcon.supfusion.configuration.services.service.EchartsService;
import com.supcon.supfusion.configuration.services.service.EventService;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ServiceApiService("echartsService")
@Transactional
public class EchartsServiceImpl extends BaseServiceImpl<Echarts> implements EchartsService, InitializingBean {
	
	@Autowired
	private ModelService modelService;
	@Autowired
	private EchartsDaoImpl echartsDao;
	@Autowired
	private EchartsModelDaoImpl emDao;
	@Autowired
	private EventService eventService;
	@Autowired
	ConditionService conditionService;
	@Autowired
	private InternationalService internationalService;

	/**
	 * @see EchartsService#addEcharts(Echarts)
	 * @author: huning
	 */
	@Override
	@Transactional
	public void addEcharts(Echarts echarts) {
		log.info("保存图表 start..." + echarts.toString());
		addEchartsAttr(echarts);
		addEchartsModels(echarts.getCode(), echarts.getModelList());
		addEchartsEvents(echarts.getCode(), echarts.getEvents());
		log.info("保存图表 end");
	}
	
	/**
	 * @see EchartsService#delEcharts(String)
	 * @author: huning
	 */
	@Override
	@Transactional
	public void delEcharts(String code) {
		log.info("删除图表 start..." + code);
		Echarts echarts = findEchartsByCode(code);
		if (null != echarts) {
			echartsDao.deletePhysical(echarts);
			emDao.delEchartsModelsByEcode(code);
			eventService.deleteEventByView(code + "@@");
		}
		log.info("删除图表 end");
	}
	
	/**
	 * @see EchartsService#delEchartsByViewCode(String)
	 * @author: huning
	 */
	@Override
	@Transactional
	public void delEchartsByViewCode(String viewCode) {
		List<Echarts> echartsList = this.findEchartsListByViewCode(viewCode, false);
		if (echartsList != null && !echartsList.isEmpty()) {
			for (Echarts echarts : echartsList) {
				this.delEcharts(echarts.getCode());
			}
		}
	}
	
	/**
	 * @see EchartsService#findEchartsByCode(String)
	 * @author: huning
	 */
	@Override
	public Echarts findEchartsByCode(String code) {
		return echartsDao.get(code);
	}
	
	/**
	 * @see EchartsService#changeEchartsProjFlag(String, Boolean)
	 * @author: huning
	 */
	@Override
	@Transactional
	public void changeEchartsProjFlag(String viewCode, Boolean proFlag) {
		List<Echarts> echartsList = echartsDao.getListByViewCode(viewCode + "@@");
		if (proFlag != null && echartsList != null && !echartsList.isEmpty()) {
			for (Echarts echarts : echartsList) {
				List<EchartsModel> ems = findEmodelsByEchartsCode(echarts.getCode());
				for (EchartsModel em : ems) {
					em.setProjFlag(proFlag);
					emDao.save(em);
				}
				List<Event> events = findEventsByEchartsCode(echarts.getCode());
				for (Event e : events) {
					e.setProjFlag(proFlag);
					eventService.saveEvent(e);
				}
				echarts.setProjFlag(proFlag);
				echartsDao.save(echarts);
			}
		}
	}
	
	/**
	 * @see EchartsService#findEchartsListByViewCode(String)
	 * @author: huning
	 */
	@Override
	@Transactional(readOnly = true)
	public List<Echarts> findEchartsListByViewCode(String viewCode, boolean isAll) {
		List<Echarts> echartsList = echartsDao.getListByViewCode(viewCode + "@@");
		if (isAll && echartsList != null && !echartsList.isEmpty()) {
			for (Echarts echarts : echartsList) {
				List<EchartsModel> ems = findEmodelsByEchartsCode(echarts.getCode());
				List<Event> events = findEventsByEchartsCode(echarts.getCode());
				echarts.setModelList(ems);
				echarts.setEvents(events);
				echartsDao.evict(echarts);
			}
		}
		return echartsList;
	}

	/**
	 * @see EchartsService#findEmodelsByEchartsCode(String)
	 * @author: huning
	 */
	@Override
	public List<EchartsModel> findEmodelsByEchartsCode(String echartsCode) {
		List<EchartsModel> ems = emDao.findEchartsModels(echartsCode);
		EchartsUtils.dealXAxisAndYAxis(ems);
		return ems;
	}
	
	/**
	 * @see EchartsService#copyEcharts(List, String)
	 * @author: huning
	 */
	@Override
	public void copyEcharts(List<Echarts> echartsList, String viewCode, String newViewCode) {
		for (Echarts echarts : echartsList) {
			copyEchartsAttr(echarts, viewCode, newViewCode);
			copyEchartsModel(echarts.getModelList(), viewCode, newViewCode);
			copyEchartsEvents(echarts.getEvents(), viewCode, newViewCode);
		}
	}
	
	/**
	 * @Description: 保存图表属性（不包含数据源）
	 *
	 * @param: 参数描述
	 * @return: 返回结果描述
	 *
	 * @author: huning
	 * @date: 2019年1月22日 下午2:26:13
	 */
	private void addEchartsAttr(Echarts echarts) {
		String title = echarts.getTitle();
		String key = internationalService.addInternational(title);
		echarts.setTitle(key);
		echartsDao.save(echarts);
	}
	
	/**
	 * @Description: 批量保存数据源
	 *
	 * @param:echarts.code;数据源List
	 * @return：null
	 *
	 * @author: huning
	 * @date: 2019年1月16日 上午11:09:35
	 */
	private void addEchartsModels(String echartsCode, List<EchartsModel> ems) {
		long start = System.currentTimeMillis();
		log.info("批量保存echarts数据源 start...");
		emDao.delEchartsModelsByEcode(echartsCode); // 更新数据源配置前将原配置删除，防止前台删除数据库未删除问题
		if (ems == null || ems.isEmpty()) {
			return ;
		}
		log.info("批量保存echarts数据源，echartsCode=" + echartsCode);
		for (EchartsModel em : ems) {
			em.setSql(getDataSqlByEm(em));
			emDao.save(em);
		}
		log.info("批量保存echarts数据源 end time=" + (System.currentTimeMillis() - start));
	}
	
	/**
	 * @Description: 保存图表事件
	 *
	 * @param: 参数描述
	 * @return: 返回结果描述
	 *
	 * @author: huning
	 * @date: 2019年1月29日 上午11:01:19
	 */
	private void addEchartsEvents(String echartsCode, List<Event> events) {
		eventService.deleteEventByView(echartsCode + "@@");
		if (events != null && !events.isEmpty()) {
			for (Event e : events) {
				eventService.saveEvent(e);
			}
		}
	}

	/**
	 * @see EchartsService#findEventsByEchartsCodeForEc(String)
	 * @author: huning
	 */
	@Override
	public List<Event> findEventsByEchartsCode(String echartsCode) {
		List<Event> events = eventService.findEvents(Restrictions.like("code", echartsCode+"@@", MatchMode.START), Restrictions.eq("valid", true));
		return events;
	}
	
	/**
	 * @see EchartsService#findEventsMapByEchartsCode(String)
	 * @author: huning
	 */
	@Override
	public Map<String, String> findEventsMapByEchartsCode(String echartsCode) {
		Map<String, String> map = null;
		List<Event> events = findEventsByEchartsCode(echartsCode);
		if (events != null && !events.isEmpty()) {
			map = new HashMap<String, String>(events.size());
			for (Event e : events) {
				map.put(e.getName(), e.getFunction());
			}
		}
		return map;
	}
	
	/**
	 * @Description: 根据分类、系列配置获取数据源SQL
	 *
	 * @param: 参数描述
	 * @return: 参数包含?, 手写自定义条件中含可动态传递参数则站位${customerSql?}
	 *
	 * @author: huning
	 * @date: 2019年2月21日 上午9:58:51
	 */
	private String getDataSqlByEm(EchartsModel em) {
		if (em == null) {
			return null;
		}
		log.info("获取数据源SQL start...");
		Model model = modelService.getModel(em.getModelCode());
		String modelAlias = "\"" + StringUtils.firstLetterToLower(model.getModelName()) + "\"";
		if (StringUtils.isEmpty(em.getValueColumn()) || null == model) {
			return null;
		}
		String groupbySql = getGroupbySql(em);
		String customConditionsSql = getCustomConditionsSql(em.getIsCustomConditions(), em.getCustomConditions(), em.getCustomConditionsConfjson());
		StringBuffer sbfsql = new StringBuffer("select sum(");
		sbfsql.append(em.getValueColumn()).append(") as ").append(em.getValueColumn());
		if (!StringUtils.isEmpty(groupbySql)) {
			sbfsql.append(",").append(groupbySql);
		}
		sbfsql.append(" from ").append(model.getTableName()).append(" ").append(modelAlias).append(" where 1=1 ");
		if (null == model.getType() || Model.TYPE_SQL != model.getType()) {
			sbfsql.append(" and valid=1 ");
		}
		if (!StringUtils.isEmpty(customConditionsSql)) {
			sbfsql.append(" and ").append(customConditionsSql);
		}
		if (!StringUtils.isEmpty(groupbySql)) {
			sbfsql.append(" group by ").append(groupbySql);
		}
		String sql = sbfsql.toString();
		log.info("获取数据源SQL end SQL=" + sql);
		return sql;
	}
	
	/**
	 * @Description: 自定义条件SQL段
	 *
	 * @param: customConditionsConfjson
	 * @return: SQL
	 *
	 * @author: huning
	 * @date: 2019年2月16日 上午9:54:20
	 */
	private String getCustomConditionsSql(Boolean isCustomConditions, String customConditions, String customConditionsConfjson) {
		log.info("获取自定义条件SQL start...");
		String customerSql = null;
		if (Boolean.TRUE.equals(isCustomConditions)) { // 手写自定义条件
			if (StringUtils.isEmpty(customConditions)) {
				return null;
			}
			if(customConditions.indexOf("return")>-1){ // 由运行期前台参数动态决定
				customerSql = EchartsModel.DYNAMIC_CUSTOMSQL;
			} else {
				customerSql = customConditions;
			}
		} else {
			if (StringUtils.isEmpty(customConditionsConfjson)) {
				return null;
			}
			AdvQueryCondition acon = conditionService.toSql(customConditionsConfjson);
			customerSql = acon!=null?acon.getSql():null;
		}
		log.info("获取自定义条件SQL end SQL=" + customerSql);
		return customerSql;
	}
	
	/**
	 * @Description: 数据源sql 中 group by 段 
	 *
	 * @param: 参数描述
	 * @return: 返回结果描述
	 *
	 * @author: huning
	 * @date: 2019年1月17日 下午2:25:33
	 */
	private String getGroupbySql(EchartsModel em) {
		String groupbySql = null;
		String xAxisColumn = em.getClassificationColumn();
		String seriesColumn = em.getSeriesColumn();
		if (!StringUtils.isEmpty(xAxisColumn) && !StringUtils.isEmpty(seriesColumn)) {
			groupbySql = xAxisColumn + "," + seriesColumn;
		} else if (!StringUtils.isEmpty(xAxisColumn) && StringUtils.isEmpty(seriesColumn)) {
			groupbySql = xAxisColumn;
		} else if (StringUtils.isEmpty(xAxisColumn) && !StringUtils.isEmpty(seriesColumn)) {
			groupbySql = seriesColumn;
		}
		return groupbySql;
	}
	
	/**
	 * @Description: 复制图表属性
	 *
	 * @param: 参数描述
	 * @return: 返回结果描述
	 *
	 * @author: huning
	 * @date: 2019年4月10日 下午4:07:15
	 */
	private void copyEchartsAttr(Echarts echarts, String viewCode, String newViewCode) {
		Echarts echartsCopy = new Echarts();
		BeanUtils.copyProperties(echarts, echartsCopy);
		echartsCopy.setCode(echarts.getCode().replace(viewCode, newViewCode));
		addEchartsAttr(echartsCopy);
	}
	
	/**
	 * @Description: 复制图表数据源
	 *
	 * @param: 参数描述
	 * @return: 返回结果描述
	 *
	 * @author: huning
	 * @date: 2019年4月10日 下午4:07:28
	 */
	private void copyEchartsModel(List<EchartsModel> emodels, String viewCode, String newViewCode) {
		for (EchartsModel em : emodels) {
			EchartsModel emCopy = new EchartsModel();
			BeanUtils.copyProperties(em, emCopy);
			emCopy.setCode(em.getCode().replace(viewCode, newViewCode));
			emCopy.setEchartsCode(em.getEchartsCode().replace(viewCode, newViewCode));
			emDao.save(emCopy);
		}
	}
	
	/**
	 * @Description: 复制图表事件
	 *
	 * @param: 参数描述
	 * @return: 返回结果描述
	 *
	 * @author: huning
	 * @date: 2019年4月10日 下午4:07:40
	 */
	private void copyEchartsEvents(List<Event> events, String viewCode, String newViewCode) {
		for (Event event : events) {
			Event copy = new Event();
			BeanUtils.copyProperties(event, copy);
			copy.setCode(event.getCode().replace(viewCode, newViewCode));
			eventService.saveEvent(copy);
		}
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		// TODO Auto-generated method stub
	}
}
