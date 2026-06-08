package com.supcon.supfusion.i18n.interapi;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.i18n.dao.vo.I18nLanguageVO;
import com.supcon.supfusion.i18n.service.I18nInterApiService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/*
 *
 * 国际化 键值对 inter-api 接口
 *
 */
@Api(tags = "inter-api 国际化语言相关接口")
@RestController
@RequestMapping("/inter-api/i18n/v1")
public class I18nLanguageApiController {

    @Autowired
    private I18nInterApiService i18nInterApiService;

    /*
     *查询所有语言类型
     */
    @ApiOperation(value = "查询所有语言类型")
    @GetMapping(value = {"/language/all","/language/all/element"})
    public PageResult<I18nLanguageVO> language_obtain() {
        return i18nInterApiService.getAllLanguage();
    }

    /*
     * 功能：修改国际化语言
     */
    @ApiOperation(value ="启用/停用国际化语言")
    @ApiImplicitParams({
            @ApiImplicitParam(
                    name = "params",
                    value = "语言类型对应集合",
                    required = true,
                    paramType = "body",allowMultiple = true,dataType = "string",
                    example = "{'langu_code':'en_US','langu_type':'英文','used':true}"),
    })
    @PutMapping(value = "/language/code")
    public void language_code(@RequestBody List<Map<String, Object>> params) {
        i18nInterApiService.updateI18nLanguage(params);
    }
}