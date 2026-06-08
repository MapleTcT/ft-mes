package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.configuration.services.dao.EventDaoImpl;
import com.supcon.supfusion.configuration.services.entity.DataGrid;
import com.supcon.supfusion.configuration.services.entity.Event;
import com.supcon.supfusion.configuration.services.entity.View;
import com.supcon.supfusion.configuration.services.service.EventService;
import com.supcon.supfusion.configuration.services.utils.DBColumnNames;
import com.supcon.supfusion.configuration.services.utils.SerializeUitls;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 实体配置信息事件操作实现
 * 
 * 
 * @author fangzhibin
 * @version $Id$
 */
@ServiceApiService("ec_EventService")
@Transactional
public class EventServiceImpl implements EventService {

	private EventDaoImpl eventDao;
	@Override
	@Transactional
	public void saveEvent(Event event) {
		if(Boolean.TRUE.equals(ProjectFlagHolder.getInstance().getProjFlag().get())){
			event.setProjFlag(true);
		}
		eventDao.flush();
		eventDao.save(event);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Event getEvent(String eventCode) {
		return eventDao.load(eventCode);
	}
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Event getRuntimeEvent(String code) {
		StringBuffer sql = new StringBuffer();
		sql.append(" select " + DBColumnNames.COLUMN_NAMES.get("EVENT") + " from runtime_event ");
		sql.append(" where code ='" + code + "'");
		List<Event> eventList = eventDao.createNativeQuery(sql.toString()).addEntity(Event.class).list();
		if (eventList.size() > 0) {
//			Event o =(Event)eventList.get(0);
			return eventList.get(0);
		} else {
			return null;
		}
	}

	@Override
	@Transactional
	public void deleteEvent(Event event) {
		eventDao.deletePhysical(event);
	}

	@Override
	@Transactional
	public void deleteEvent(String eventCode) {
		Event event = getEvent(eventCode);
		if (null != event) {
			eventDao.deletePhysical(event);
		}
	}

	@Override
	@Transactional
	public void deleteEventByField(String fieldCode) {
		List<Event> events = eventDao.findByHql("from Event where field.code = ?0", fieldCode);
		if (null != events && !events.isEmpty()) {
			for (Event e : events) {
				deleteEvent(e);
			}
		}
	}
	@Override
	@Transactional
	public void deleteEventByView(String viewCode) {
		List<Event> events = eventDao.findByHql("from Event where code like ?0", viewCode + "%");
		if (null != events && !events.isEmpty()) {
			for (Event e : events) {
				deleteEvent(e);
			}
		}
	}

	@Override
	@Transactional
	public void deleteEventByButton(String buttonCode) {
		List<Event> events = eventDao.findByHql("from Event where button.code = ?", buttonCode);
		if (null != events && !events.isEmpty()) {
			for (Event e : events) {
				deleteEvent(e);
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Transactional
	@Override
	public void saveEvent(Object object, String fieldConfig) {
		String code = "";
		String moduleCode = null;
		String entityCode = null;
		if (object instanceof View) {
			View view = (View) object;
			code = view.getCode();
			moduleCode = view.getModuleCode();
			entityCode = view.getEntity().getCode();
		} else if (object instanceof DataGrid) {
			DataGrid grid = (DataGrid) object;
			code = grid.getCode();
			moduleCode = grid.getModuleCode();
			entityCode = grid.getEntityCode();
		}
		if (fieldConfig != null && !fieldConfig.isEmpty()) {
			Map fieldsMap = (Map) SerializeUitls.deserialize(fieldConfig);
			if (fieldsMap != null && !fieldsMap.isEmpty()) {
				List<Map> events = (List<Map>) fieldsMap.get("events");
				if (events != null && !events.isEmpty()) {
					for (Map<String, String> event : events) {
						String type = event.get("name").toString().split("=")[0];
						if (code.contains(View.MOBILE_VIEW_SUFFIX) && !("onchange".equalsIgnoreCase(type.trim())
								|| "onclick".equalsIgnoreCase(type.trim()) || "onload".equalsIgnoreCase(type.trim()) || "onsave".equalsIgnoreCase(type.trim()))) {
							continue;
						}
						Event e = getEvent(code + "_" + event.get("layoutCode").toString() + "_" + type);
						if (null == e) {
							e = new Event();
							e.setVersion(0);
						}
						e.setCode(code + "_" + event.get("layoutCode").toString() + "_" + type);
						e.setName(event.get("name").toString());
						if (null == event.get("function")) {
							e.setFunction("");
						} else {
							e.setFunction(String.valueOf(event.get("function")));
						}
						if (null == event.get("function_es5")) {
							e.setFunction_es5("");
						} else {
							e.setFunction_es5(String.valueOf(event.get("function_es5")));
						}
						e.setLayoutCode(event.get("layoutCode").toString());
						e.setModuleCode(moduleCode);
						e.setEntityCode(entityCode);
						saveEvent(e);
					}
				}
			}
		}
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<Event> getEventsByLayoutCode(String layoutCode) {
		return eventDao.findByCriteria(Restrictions.eq("layoutCode", layoutCode));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<Event> getEventsByFastQueryJsonCode(String fqjCode) {
		String hql = "select event from Event event where event.field.fastQueryJson.code=? and field.valid = true";
		return eventDao.createQuery(hql, fqjCode).list();
	}
	
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<Event> getEventsByAdvQueryJsonCode(String aqjCode) {
		String hql = "select event from Event event where event.field.advQueryJson.code=? and field.valid = true ";
		return eventDao.createQuery(hql, aqjCode).list();
	}
	
	@Autowired
	public void setEventDao(EventDaoImpl eventDao) {
		this.eventDao = eventDao;
	}

	/**
	 * 根据条件查询Event
	 * 
	 * @param criterions
	 * @return
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Event> findEvents(Criterion... criterions) {
		return eventDao.findByCriteria(criterions);
	}

	@Override
	public int copyToRuntimeEvent(String code) {
		StringBuffer sql = new StringBuffer();
		sql.append("insert into runtime_event(" + DBColumnNames.COLUMN_NAMES.get("EVENT") + ") ");
		sql.append(" select " + DBColumnNames.COLUMN_NAMES.get("EVENT") + " from project_event ");
		sql.append(" where code ='" + code + "'");
		int n = eventDao.createNativeQuery(sql.toString()).executeUpdate();
		return n;
	}

	@Override
	public void deleteEventFromRuntime(String eventcode) {
		StringBuffer sql = new StringBuffer();
		sql.append("delete from runtime_event " +"where code ='" + eventcode + "'");
		eventDao.createNativeQuery(sql.toString()).executeUpdate();
	}
	
}
