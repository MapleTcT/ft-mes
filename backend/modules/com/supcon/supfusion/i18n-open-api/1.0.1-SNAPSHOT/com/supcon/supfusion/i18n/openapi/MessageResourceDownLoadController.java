package com.supcon.supfusion.i18n.openapi;

import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.i18n.common.execption.I18nErrorEnum;
import com.supcon.supfusion.i18n.common.execption.I18nException;
import com.supcon.supfusion.i18n.service.I18nResourceDownloadService;
import com.supcon.supfusion.i18n.service.I18nResourceUploadService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/*
 * 国际化 键值对 open-api 接口
 */
@Slf4j
@Api(tags = "模块国际化资源获取相关接口")
@RestController
@RequestMapping("/open-api/i18n/v1")
public class MessageResourceDownLoadController {

    @Autowired
    private I18nResourceUploadService i18nResourceUploadService;
    @Autowired
    private I18nResourceDownloadService i18nResourceDownloadService;

    /*
     *  资源获取openAPI
     *  获取多个模块的国际化资源 不要token 过来就给
     *  返回map嵌套map的形式
     */
    @ApiOperation(value ="获取多个模块的所有语言类型的国际化键值对")
    @ApiImplicitParams({
            @ApiImplicitParam(name="moduleCodes",value="多个模块code",required=true,paramType="query",example = "defaultModule,notificationAdmin"
            ),
    })
    @GetMapping(value = "/resource/modules/messages")
    public Result downLoadResources(@RequestParam(value = "moduleCodes") String[] moduleCodes) {
        long time = System.currentTimeMillis();
        if(!(moduleCodes!=null && moduleCodes.length>0)){
            throw  new I18nException(I18nErrorEnum.FILE_NO_MODULE_ERROR);
        }
        Result result  =  i18nResourceDownloadService.getModulesResourcesOpenApi(moduleCodes);
        long time1 = System.currentTimeMillis();
        System.out.println("整个请求的时间："+(time1-time));
        log.info("整个请求的时间："+(time1-time));
        return result;
    }
}