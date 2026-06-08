package com.supcon.supfusion.i18n.interapi;

import com.github.xiaoymin.knife4j.annotations.DynamicParameter;
import com.github.xiaoymin.knife4j.annotations.DynamicParameters;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.service.I18nInterApiService;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 *
 * 国际化 键值对 inter-api 接口
 *
 */
@Slf4j
@Api(tags = "inter-api 模块资源版本号相关接口")
@RestController
@RequestMapping("/inter-api/i18n/v1")
@Deprecated
public class I18nVersionApiController {

    @Autowired
    private I18nInterApiService i18nInterApiService;


    /*
     *获取国际化资源版本号
     */
    @ApiOperation(value = "获取多个模块的版本号", httpMethod = "GET", response = Map.class, notes = "")
    @ApiParam(required = true, name = "moduleCodes", value = "多个模块code")
    @GetMapping(value = "/version")
    public Result resource_version_obtain(@RequestParam(value = "moduleCodes") String moduleCodes) {
        Result result = new Result();
        if(moduleCodes ==null || moduleCodes.equals(Constants.STR_NO_SPACE)){
            result.setCode(100107008);
            result.setMessage(Constants.NO_MODULE_CODE);
            return result;
        }
        String[] arr = moduleCodes.split(Constants.STR_POINT_DOU);
        if(arr==null || arr.length==0){
            result.setCode(100107008);
            result.setMessage(Constants.NO_MODULE_CODE);
            return result;
        }
        List<String> list = new ArrayList();
        for (String moduleCode:arr) {
            list.add(moduleCode);
        }
        result = i18nInterApiService.getI18nModuleVersionCode(list);
        return result;
    }

    /*
     *功能：添加国际化资源版本号
     */
    @ApiOperation(value = "添加国际化资源版本号")
    @DynamicParameters(name = "map",properties = {
            @DynamicParameter(name = "moduleCode",value = "模块code",example = "systemConfig",required = true),
            @DynamicParameter(name = "moduleVersion",value = "新增模块版本号",example = "systemConfig202007202029",required = true),
    })
    @PostMapping(value = "/version")
    public Result resource_version_POST(@RequestBody Map map) {
        Result result = new Result();
        if (map.get(Constants.MODULE_CODE) == null
                ||(map.get(Constants.MODULE_CODE) != null && map.get(Constants.MODULE_CODE).equals(Constants.STR_NO_SPACE))
        ) {
            result.setCode(100107008);
            result.setMessage(Constants.NO_MODULE_CODE);
            return result;
        }
        if (map.get(Constants.MODULE_VERSION_CODE) == null || (map.get(Constants.MODULE_VERSION_CODE) != null && map.get(Constants.MODULE_VERSION_CODE).equals(Constants.STR_NO_SPACE))
        ) {
            result.setCode(100107013);
            result.setMessage(Constants.NO_VERSION_CODE);
            return result;
        }
        result = i18nInterApiService.postI18nModuleVersionCode(map);
        return result;
    }
}