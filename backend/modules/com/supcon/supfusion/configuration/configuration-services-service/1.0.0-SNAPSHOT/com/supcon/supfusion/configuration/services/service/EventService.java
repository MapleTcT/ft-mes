package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.Event;
import org.hibernate.criterion.Criterion;

import java.util.List;

/**
 * 配置信息事件处理接口
 * 
 * 
 * @author fangzhibin
 * @version $Id$
 */
public interface EventService {

	/**
	 * 保存事件
	 * @param event
	 */
	void saveEvent(Event event); 
	
	/**
	 * 获取事件
	 * @param eventCode
	 * @return
	 */
	Event getEvent(String eventCode);
	/**
	 * 获取runtime事件
	 * @param eventCode
	 * @return
	 */
	Event getRuntimeEvent(String eventCode);
	/**
	 * 单条数据复制到runtime_event中
	 * @param code
	 * 
	 */
	public int copyToRuntimeEvent(String code);
	/**
	 * 删除event信息fromRunTime
	 * @param printTemplate
	 */
	public void deleteEventFromRuntime(String eventcode);
	/**
	 * 删除事件
	 * @param event
	 */
	void deleteEvent(Event event);
	
	/**
	 * 
	 * @param eventCode
	 */
	void deleteEvent(String eventCode);
	
	void deleteEventByField(String fieldCode);

	void deleteEventByButton(String buttonCode);

	void saveEvent(Object object, String fieldConfig);

	/**
	 * 根据布局code查找Event
	 * @param layoutCode
	 * @return
	 */
	List<Event> getEventsByLayoutCode(String layoutCode);
	/**
	 * 根据FastQueryJsonCode查找Event
	 * @param fqjCode
	 * @return
     */
    List<Event> getEventsByFastQueryJsonCode(String fqjCode);
    /**
     * 根据AdvQueryJsonCode查找Event
     * @param aqjCode
     * @return
     */
    List<Event> getEventsByAdvQueryJsonCode(String aqjCode);
	/**
	 * 根据条件查询Event
	 * @param criterions
	 * @return
	 */
	List<Event> findEvents(Criterion... criterions);
	
	/**
	 *  根据视图code删除包含Event
	 * @param viewCode
	 */
	void deleteEventByView(String viewCode);
}
