/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.openapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.openapi.vo.ResponseMsg;
import com.supcon.supfusion.configuration.services.service.DataProcessingService;
import com.supcon.supfusion.configuration.services.service.ModuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 数据处理使用
 * 
 * @author zhuyuyin
 * @version 1.0
 */
@Slf4j
@Controller
//@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "${spring.application.name}" + HttpConstants.URL_SPLITER + "v1")
public class DataProcessingController extends ConfigurationBaseController {

	private static final long serialVersionUID = 1L;

	@Autowired
	private DataProcessingService dataProcessingService;
	@Autowired
	private ModuleService moduleService;

	/**
	 * 给继承{@link }的对象添加属性
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/public/ec/dealModuleCode")
	public String dealModuleCode() throws JsonProcessingException {
		ResponseMsg response = new ResponseMsg(true);
		try{
			dataProcessingService.addModuleCodeToAllObject();
		}catch (Exception e) {
			log.error(e.getMessage(), e);
			response.setSuccess(false);
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}

	/**
	 * 初始化实体发布时间
	 * @return
	 */
//	@ResponseBody
//	@RequestMapping(value = "/public/ec/init-entityInfo")
//	public String initEntityModifyInfo() throws JsonProcessingException {
//		ResponseMsg response = new ResponseMsg(true);
//		try{
//			dataProcessingService.initEntityModifyInfo();
//		}catch (Exception e) {
//			log.error(e.getMessage(), e);
//			response.setSuccess(false);
//		}
//		ObjectMapper mapper = new ObjectMapper();
//		return mapper.writeValueAsString(response);
//	}

	@ResponseBody
	@RequestMapping(value = "/public/ec/init-fullPathName")
	public String initFullPathName() throws JsonProcessingException {
		ResponseMsg response = new ResponseMsg(true);
		try{
			dataProcessingService.initFullPathNameForTree();
		}catch (Exception e) {
			log.error(e.getMessage(), e);
			response.setSuccess(false);
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}

	@ResponseBody
	@RequestMapping(value = "/public/ec/init-leaf")
	public String initLeaf() throws JsonProcessingException {
		ResponseMsg response = new ResponseMsg(true);
		try{
			dataProcessingService.initLeafForTree();
		}catch (Exception e) {
			log.error(e.getMessage(), e);
			response.setSuccess(false);
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}
	@ResponseBody
	@RequestMapping(value = "/public/ec/init-versionProperty")
	public String initVersionProperty() throws JsonProcessingException {
		ResponseMsg response = new ResponseMsg(true);
		try{
			dataProcessingService.initVersionProperty();
		}catch (Exception e) {
			log.error(e.getMessage(), e);
			response.setSuccess(false);
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}

	@ResponseBody
	@RequestMapping(value = "/public/ec/dealListPtForAssId")
	public String dealListPtForAssId() throws JsonProcessingException {
		ResponseMsg response = new ResponseMsg(true);
		try{
			dataProcessingService.dealListPtForAssId();
		}catch (Exception e) {
			log.error(e.getMessage(), e);
			response.setSuccess(false);
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}
	
}
