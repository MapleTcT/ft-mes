package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.Echarts;
import com.supcon.supfusion.configuration.services.entity.EchartsModel;
import com.supcon.supfusion.configuration.services.entity.Event;

import java.util.List;
import java.util.Map;

/**
 * 
 * Copyright: Copyright (c) 2018 SUPCON
 * 
 * @ClassName: EchartsService.java
 * @Description: Echart增删改查、获取图表数据
 *
 * @version: v1.0.0
 * @author: huning
 * @date: 2018年12月25日 下午3:55:11
 */
public interface EchartsService {
	
	/**
	 * @Description: 保存图表属性（包含数据源等）
	 *
	 * @param: Echarts
	 * @return: null
	 *
	 * @author: huning
	 * @date: 2019年1月18日 上午9:46:20
	 */
	void addEcharts(Echarts echarts);
	/**
	 * @Description: 删除图表属性（包含数据源等）
	 *
	 * @param: echarts.code
	 * @return: null
	 *
	 * @author: huning
	 * @date: 2019年1月18日 上午9:46:46
	 */
	void delEcharts(String echartsCode);
	/**
	 * @Description: 根据视图删除图表属性（不包含数据源等）
	 *
	 * @param: echarts.code
	 * @return: null
	 *
	 * @author: huning
	 * @date: 2019年1月18日 上午9:46:46
	 */
	void delEchartsByViewCode(String viewCode);
	/**
	 * @Description: 根据code查询图表属性对象
	 *
	 * @param: echarts.code
	 * @return: Echarts
	 *
	 * @author: huning
	 * @date: 2019年1月18日 上午9:48:55
	 */
	Echarts findEchartsByCode(String code);
	
	/**
	 * @Description: 根据视图Code查询该视图下所有图表配置（包含数据源配置）
	 *
	 * @param: 视图Code, 是否包含数据源和事件
	 * @return: EchartsList
	 *
	 * @author: huning
	 * @date: 2019年1月21日 上午10:22:05
	 */
	List<Echarts> findEchartsListByViewCode(String viewCode, boolean isAll);
	
	/**
	 * @Description: 根据视图编码更新工程期字段
	 *
	 * @param: 参数描述
	 * @return: 返回结果描述
	 *
	 * @author: huning
	 * @date: 2019年3月29日 下午4:35:48
	 */
	void changeEchartsProjFlag(String viewCode, Boolean proFlag);
	
	/**
	 * @Description: 复制图表，视图复制时调用
	 *
	 * @param: 参数描述
	 * @return: 返回结果描述
	 *
	 * @author: huning
	 * @date: 2019年4月10日 下午3:42:01
	 */
	void copyEcharts(List<Echarts> echartsList, String viewCode, String newViewCode);
	
	/* 数据源相关接口以下 */
	/**
	 * @Description: 根据echarts.code获取数据源属性 for 组态期（不使用缓存）
	 *
	 * @param: echarts.code
	 * @return: List<EchartsModel>
	 *
	 * @author: huning
	 * @date: 2019年1月18日 上午9:51:42
	 */
	List<EchartsModel> findEmodelsByEchartsCode(String echartsCode);
	
	/* 事件相关接口以下 */
	/**
	 * @Description: 根据echarts.code获取事件列表 （不使用缓存）
	 *
	 * @param: 参数描述
	 * @return: 返回结果描述
	 *
	 * @author: huning
	 * @date: 2019年1月29日 下午2:42:44
	 */
	List<Event> findEventsByEchartsCode(String echartsCode);
	/**
	 * @Description: 根据echarts.code获取事件列表 （不使用缓存）
	 *
	 * @param: 参数描述
	 * @return: 返回结果描述
	 *
	 * @author: huning
	 * @date: 2019年1月29日 下午2:42:44
	 */
	Map<String, String> findEventsMapByEchartsCode(String echartsCode);
}
