package com.supcon.supfusion.i18n.interapi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.i18n.common.execption.I18nErrorEnum;
import com.supcon.supfusion.i18n.common.execption.I18nException;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.common.until.LocaleCustomUtil;
import com.supcon.supfusion.i18n.service.I18nResourceDownloadService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/*
 *
 * 国际化 键值对 inter-api 接口
 *
 */
@Api(tags = "inter-api 模块国际化资源获取相关接口")
@RestController
@RequestMapping("/inter-api/i18n/v1")
public class I18nResourceDownLoadController {

    @Autowired
    private I18nResourceDownloadService i18nResourceDownloadService;

    /*
     *  前端调用  资源下载 压缩包形式  获取多个模块的国际化资源 不要token 过来就给
     *  入参 多个模块code 和语言类型
     *  返回map嵌套map的形式 前端调用
     */
    @Deprecated
    @ApiOperation(value = "获取多个模块的指定语言类型的国际化键值对")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "map", value = "多个模块code和语言类型集合", required = true, paramType = "query",
                    example = "{'moduleCodes':'systemConfig,defaultModule','langu_code':'zh_CN'}"
            ),
    })
    @GetMapping(value = "/resource/code/modules/index")
    public Result getModulesIndex(@RequestParam String[] moduleCodeArr) {
        if (moduleCodeArr == null || (moduleCodeArr != null && !(moduleCodeArr.length > 0))) {
            throw new I18nException(I18nErrorEnum.PARAM_LOST);
        }
        List<String> moduleCodes = new ArrayList<>();
        Arrays.asList(moduleCodeArr).forEach(moduleCode -> {
            moduleCodes.add(moduleCode);
        });
        return i18nResourceDownloadService.getModulesResourceIndexs(moduleCodes);
    }

    /*
     *  前端调用  资源下载 压缩包形式  获取多个模块的国际化资源 不要token 过来就给
     *  入参 多个模块code 和语言类型
     *  返回map嵌套map的形式 前端调用
     */
    @ApiOperation(value = "获取多个模块的指定语言类型的国际化键值对")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "map", value = "多个模块code和语言类型集合", required = true, paramType = "query",
                    example = "{'moduleCodes':'systemConfig,defaultModule','langu_code':'zh_CN'}"
            ),
    })
    @GetMapping(value = "/resource/code/all/module_ids/allkeyvalues")
    public PageResult<Map<String, Map<String, String>>> downLoadResources2(@RequestParam Map map) {
        if (map.get(Constants.MODULE_CODES) == null || (map.get(Constants.MODULE_CODES) != null && map.get(Constants.MODULE_CODES).equals(Constants.STR_NO_SPACE))
                || map.get(Constants.LANGU_CODE) == null || (map.get(Constants.LANGU_CODE) != null && map.get(Constants.LANGU_CODE).equals(Constants.STR_NO_SPACE))
        ) {
            throw new I18nException(I18nErrorEnum.PARAM_LOST);
        }
        String moduleCodes = (String) map.get(Constants.MODULE_CODES);
        String languCode = (String) map.get(Constants.LANGU_CODE);
        languCode = LocaleCustomUtil.localeChange(languCode);
        List<String> moduleIds = new ArrayList<>();
        String[] mcs = moduleCodes.split(Constants.STR_POINT_DOU);
        for (String s : mcs) {
            moduleIds.add(s);
        }
        return i18nResourceDownloadService.getModulesResourceKeyValues(moduleIds, languCode);
    }

}