package com.supcon.supfusion.configuration.services.openapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.configuration.services.utils.JSONUtil;
import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.openapi.utils.DtoUtils;
import com.supcon.supfusion.configuration.services.openapi.vo.ResponseMsg;
import com.supcon.supfusion.configuration.services.service.EchartsService;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.configuration.services.service.ViewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Copyright: Copyright (c) 2018 SUPCON
 * 
 * @ClassName: EchartsAction.java
 * @Description: 图表控件控制类
 *
 * @version: v1.0.0
 * @author: huning
 * @date: 2018年12月29日 下午3:35:06
 */
@Slf4j
@Controller
//@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "${spring.application.name}" + HttpConstants.URL_SPLITER + "v1")
public class EchartsController extends ConfigurationBaseController {
	
	@Resource
	private EchartsService echartsService;
	@Resource
	private ViewService viewService;
	@Resource
	private ModelService modelService;


	@RequestMapping({"/ec/echarts/manage"})
	public String manage(ModelMap map, String viewCode, String echartsCode, Boolean isEdit) {
		Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
		if(null !=isProj &&isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
            map.addAttribute("isProj", true);
        } else {
        	map.addAttribute("isProj", false);
        }
		View view = null;
		Echarts echarts = null;
		Map<String, String> eventsMap = null;
		Map<String, List<Property>> propertiesMap = null;
		if (!StringUtils.isEmpty(viewCode)) {
			view = viewService.getView(viewCode);
			List<Model> models = modelService.findModels(view.getEntity());
			map.addAttribute("models", models);
			propertiesMap = new HashMap<String, List<Property>>();
			for (Model model : models) {
				List<Property> properties = modelService.findProperties(model);
				propertiesMap.put(model.getCode(), properties);
			}
			if (!StringUtils.isEmpty(echartsCode)) {
				if (isEdit) { // 修改
					echarts = echartsService.findEchartsByCode(echartsCode);
				} else { // 新增，须删除原来保存图表却未保存视图的图表
					echartsService.delEcharts(echartsCode);
				}
				if (null != echarts) {
					echarts.setModelList(echartsService.findEmodelsByEchartsCode(echarts.getCode()));
					eventsMap = echartsService.findEventsMapByEchartsCode(echarts.getCode());
				} else {
					echarts = new Echarts(echartsCode);
					List emodels = new ArrayList<EchartsModel>(Arrays.asList(new EchartsModel()));
					echarts.setModelList(emodels);
				}
			}
		}
		map.addAttribute("view", view);
		map.addAttribute("echarts", echarts);
		map.addAttribute("eventsMap", eventsMap);
		map.addAttribute("propertiesMap", propertiesMap);
		if(isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(false);
        }
		return "echarts/config-echarts";
	}

	@ResponseBody
	@RequestMapping(value = "/ec/echarts/save")
	public String save(HttpServletRequest request, @RequestParam("emodelsJson") String emodelsJson, @RequestParam("eventJson")String eventJson) throws JsonProcessingException {
		Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
		if(null !=isProj &&isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
		Echarts echarts = DtoUtils.getEchartsVO(request);
		// 数据源
		echarts.setModelList(getEmodelsByJson(emodelsJson));
		// 事件
		echarts.setEvents(getEventsByJson(eventJson));
		// 保存图表
		echartsService.addEcharts(echarts);
		ResponseMsg response = new ResponseMsg(true);
		if(isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(false);
        }
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}

	/**
	 * @Description: Json格式数据源转换为List
	 *
	 * @param: 参数描述
	 * @return: 返回结果描述
	 *
	 * @author: huning
	 * @date: 2019年1月22日 下午2:24:51
	 */
	public List<EchartsModel> getEmodelsByJson(String json) {
		List<EchartsModel> emodels = null;
		try {
			emodels = (List<EchartsModel>) JSONUtil.generateObjectFromJson(json, EchartsModel.class, null);
		} catch (Exception e) {}
		return emodels;
	}
	
	/**
	 * 
	 * @Description: Json格式事件转换为List
	 *
	 * @param: 参数描述
	 * @return: 返回结果描述
	 *
	 * @author: huning
	 * @date: 2019年1月29日 上午11:05:46
	 */
	public List<Event> getEventsByJson(String json) {
		List<Event> events = null;
		try {
			events = (List<Event>) JSONUtil.generateObjectFromJson(json, Event.class, null);
		} catch (Exception e) {}
		return events;
	}
	
}
