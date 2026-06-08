package com.supcon.supfusion.i18n.interapi;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.i18n.service.impl.MessageResourceDownloadServiceImpl;
import com.supcon.supfusion.i18n.service.impl.MessageResourceUpLoadServiceImpl;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Api(tags = "inter-api 客户端调用相关接口")
@RestController
@RequestMapping("/inter-api/i18n/v1")
public class I18nClientApiController {
	
	@Autowired
	private MessageResourceDownloadServiceImpl resourceDownloadService;
	@Autowired
	private MessageResourceUpLoadServiceImpl resourceUpLoadService;
	/*
     *  获取上传资源token
     *  入参 多个模块code  多个模块国际化资源版本号 一一对应
     *  出参 令牌
     */
    @ApiOperation(value = "判断当前模块是否可以上传资源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "map", value = "模块code和模块版本号code集合", required = true, paramType = "query",
                    example = "{'moduleCodes':'sys,i18n','moduleVersions':'sys202007161529,i18n202007161529'}"),
    })
    @GetMapping(value = "resource/code/all/module_id")
    public Result judgeUploadResource2(@RequestParam Map map) {
    	return resourceUpLoadService.judgeUploadResource2(map);
    }
	
	/*
     *  客户端资源上载
     *  入参 模块code 令牌 国际化资源版本号 接收zip包
     */
    @ApiOperation(value = "上传当前模块的zip格式国际化资源包")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "map", value = "模块code和模块版本号code和获取到的token参数集合", required = true, paramType = "query",
                    example = "{'moduleCodes':'sys, i18n','moduleVersion':'tModule202007161529','token':'defaultModule_defaultModule202007161529_b2da4c4d-2e52-443d-82ef-0a3292800e58'}"
            )
    })
    @PostMapping(value = "/resource/code/all/module_id/file")
    public Result uploadResource2(@RequestParam("file") MultipartFile[] uploadFiles, @RequestParam Map map) {
    	return resourceUpLoadService.uploadResource2(uploadFiles, map);
    }
    
    /*
     *  资源下载   获取多个模块的国际化资源 不要token 过来就给
     *  返回map嵌套map的形式
     */
	@ApiOperation(value = "获取多个模块的所有语言类型的国际化键值对")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCodes", value = "多个模块code", required = true, paramType = "query", example = "defaultModule|dt,notificationAdmin|dt"
            )
    })
    @GetMapping(value = "/resource/code/all/module_key_values")
	public Result downLoadResources(@RequestParam(value = "moduleCodes") String moduleCodes, @RequestParam(value = "useGetAllModule", required = false) String useGetAllModule) {
		return resourceDownloadService.downLoadResources(moduleCodes, useGetAllModule);
	}
	
	/*
     *  判断资源下载接口, 并返回index
     *  GET/inter-api/i18n/v1/resource/code/all/module_ids
     *  入参 模块code
     *  出参 该模块的国际化资源索引
     */
    @ApiOperation(value = "获取多个模块的索引")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCodes", value = "多个模块code", required = true, paramType = "query", example = "systemConfig,defaultModule"
            ),
    })
    @GetMapping(value = "/resource/code/all/module_codes")
    public Result<Map<String, Map<String, String>>> judgeDownLoadResource(@RequestParam(value = "moduleCodes") String moduleCodes, @RequestParam(value = "useGetAllModule", required = false) String useGetAllModule) {
    	return resourceDownloadService.judgeDownLoadResource(moduleCodes, useGetAllModule);
    }
    
    
}
