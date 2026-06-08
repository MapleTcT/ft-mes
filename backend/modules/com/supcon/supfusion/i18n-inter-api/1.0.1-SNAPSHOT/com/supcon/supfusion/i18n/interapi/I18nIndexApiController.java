package com.supcon.supfusion.i18n.interapi;

import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.i18n.service.I18nInterApiService;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/*
 *
 * 国际化 键值对 inter-api 接口
 *
 */
@Slf4j
@Api(tags = "inter-api 模块资源索引相关接口")
@RestController
@RequestMapping("/inter-api/i18n/v1")
@Deprecated
public class I18nIndexApiController {

    @Autowired
    private I18nInterApiService i18nInterApiService;


    /*
     *获取当前国际化资源索引
     */
    @ApiOperation(value = "获取指定模块国际化资源索引")
    @ApiImplicitParams({
            @ApiImplicitParam(name="moduleCode",value="模块code",required=true,paramType="query"),
    })
    @GetMapping(value = "/index")
    public Result resource_index(@RequestParam(value = "moduleCode") String moduleCode) {
        Result result = i18nInterApiService.getI18nModuleIndexCode(moduleCode);
        return result;
    }

    /*
     *功能：更新某个模块国际化资源索引
     */
    @ApiOperation(value = "更新指定模块国际化资源索引")
    @ApiImplicitParams({
            @ApiImplicitParam(name="moduleCode",value="模块code",required=true,paramType="query"),
    })
    @PostMapping(value = "/index")
    public Result resource_index_addition(@RequestParam(value = "moduleCode") String moduleCode) {
        Result result = i18nInterApiService.postI18nModuleIndexCode(moduleCode);
        return result;
    }

}