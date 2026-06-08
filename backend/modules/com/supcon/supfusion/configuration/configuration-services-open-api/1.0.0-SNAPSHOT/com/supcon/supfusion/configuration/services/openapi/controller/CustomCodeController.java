/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.openapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.configuration.services.entity.Entity;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.openapi.utils.ResponseUtils;
import com.supcon.supfusion.configuration.services.openapi.vo.ResponseMsg;
import com.supcon.supfusion.configuration.services.service.CustomCodeService;
import com.supcon.supfusion.configuration.services.service.EntityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * 
 * 
 * @author fangzhibin
 * @version $Id$
 */
@Slf4j
@Controller
//@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "${spring.application.name}" + HttpConstants.URL_SPLITER + "v1")
public class CustomCodeController extends ConfigurationBaseController {

	private static final long serialVersionUID = -8217423307440096618L;

	@Resource
	private CustomCodeService customCodeService;

	@Resource
	private EntityService entityService;
	private Object monitor = new Object();

	/**
	 * 进入自定义代码管理主框架
	 * 
	 * @return
	 */
	@RequestMapping(value = "/ec/customCode/manage")
	public String manage(ModelMap map, @RequestParam("entityCode") String entityCode) {
		if (null != entityCode && !entityCode.isEmpty()) {
			Entity entity = entityService.getEntity(entityCode);
			map.addAttribute("entity", entity);
			map.addAttribute("entityCode", entityCode);
		}
		return "customCode/manage";
	}

	/**
	 * 进入自定义代码管理列表页面
	 * 
	 * @return
	 */
	@RequestMapping(value = "/ec/customCode/list")
	public String list(@RequestParam("entityCode") String entityCode, @RequestParam("type") int type,
					   @Nullable @RequestParam("path") String path,
					   @Nullable @RequestParam("id") Long id,
					   @Nullable @RequestParam("filterName") String filterName) {
		String result = customCodeService.buildCustomCodeTree(entityCode, path, id, type, filterName);
		ResponseUtils.render(getResponse(), ResponseUtils.JSON_TYPE, result);
		return null;
	}
	
	/**
	 * 进入自定义代码编辑
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/customCode/editContent")
	public String edit(@RequestParam("path") String path) {
		return customCodeService.getFileContent(path);
	}

	/**
	 * 自定义代码保存
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/customCode/saveContent")
	public ResponseMsg save(@RequestParam("codeContent") String codeContent, @Nullable @RequestParam("jses5") String jses5,
					   @RequestParam("path") String path, @RequestParam("entityCode") String entityCode,
					   @Nullable @RequestParam("publishEnabled") boolean publishEnabled,
					   @Nullable @RequestParam("hotDeploy") boolean hotDeploy) throws Exception {
		customCodeService.saveFileContent(codeContent, jses5, path, entityCode, publishEnabled);
		if(hotDeploy){
			synchronized (monitor) {
				hotDeploy(path, entityCode);
			}			
		}
		ResponseMsg msg = new ResponseMsg(true);
		return msg;
	}

	/**
	 * 加载自定义代码树形列表
	 * 
	 * @return
	 */
	@RequestMapping(value = "/ec/customCode/loadCustomCodeList")
	public String loadCustomCodeList(ModelMap map, @RequestParam("entityCode") String entityCode, @RequestParam("type") int type) {
		map.addAttribute("entityCode", entityCode);
		map.addAttribute("type", type);
		return "customCode/customCodeTree";
	}
	
	/**
	 * java代码热更新
	 * 
	 */
	private void hotDeploy(String path, String entityCode) {
		if(path.endsWith("java")){
			customCodeService.compileAndDeploy(path, entityCode);	
		}		
	}

	@ResponseBody
	@RequestMapping(value = "/ec/customCode/down-log")
	public String downloadLog(HttpServletResponse response) {
		File file = new File("bap-workspace" + File.separator +"baplogs","compileError.log");
		if (!file.exists()) {
			response.setHeader("content-type", "application/octet-stream");
			response.setContentType("application/octet-stream;charset=ISO8859-1");
			response.addHeader("Content-Disposition", "attachment;filename=compileError.log");
			byte[] buffer = new byte[1024];
			FileInputStream fis = null;
			BufferedInputStream bis = null;
			try {
				fis = new FileInputStream(file);
				bis = new BufferedInputStream(fis);
				OutputStream os = response.getOutputStream();
				int i = bis.read(buffer);
				while (i != -1) {
					os.write(buffer, 0, i);
					i = bis.read(buffer);
				}
				return "successfully";
			} catch (Exception e) {
				return "failed";
			} finally {
				if (bis != null) {
					try {
						bis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return "";
	}

	/**
	 * BAP代码热更新
	 * 
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/ec/customCode/bapHotDeploy")
	public String bapHotDeploy(@RequestParam("targetPath") String targetPath, @RequestParam("packagePath") String packagePath) throws Exception {
		ResponseMsg response = new ResponseMsg(true);
		try {
			customCodeService.bapCodeDeploy(targetPath, packagePath);
		} catch (Exception e) {
			response.setSuccess(false);
		}
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(response);
	}

}
