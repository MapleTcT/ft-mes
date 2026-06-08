package com.supcon.supfusion.i18n.service.api;

import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.i18n.service.config.FeignMultipartSupportConfig;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/*
 * 国际化 键值对 service-api 接口
 */
@Api(tags = "service API 相关接口")
@FeignClient(name = "i18n",contextId = "resourceService", configuration = FeignMultipartSupportConfig.class)
public interface MessageResourceService {

    String API_PREFIX = HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "i18n/v1/";

    /**
     * 模糊查询某种国际化value含有指定字符串的国际化key集合 指定语言类型
     *
     * @param value    国际值 条件字符串
     * @param language 语言类型 required = true
     * @return 返回国际化key value集合
     */
    @ApiOperation(value = "根据语言和部分国际化value模糊查询", notes = "模糊查询某种国际化value含有指定字符串的国际化key集合 区分大小写 指定语言类型")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "value", value = "查询字符", required = true, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "language", value = "语言类型 eg:zh_CN", required = true, paramType = "query", dataType = "String")
    })
    @GetMapping(API_PREFIX + "resource/search/one")
    @ResponseBody
    Map<String, String> MessageResourceSearchOne(@RequestParam("value") String value, @RequestParam("language") String language);


    /**
     * 模糊查询某种国际化value含有指定字符串的国际化key集合 默认语言类型
     *
     * @param value 国际值 条件字符串
     * @return 返回国际化key value集合
     */
    @ApiOperation(value = "根据语言和部分国际化value模糊查询", notes = "模糊查询某种国际化value含有指定字符串的国际化key集合 区分大小写 默认语言类型")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "value", value = "查询字符", required = true, paramType = "query", dataType = "String")
    })
    @GetMapping(API_PREFIX + "resource/search/all")
    @ResponseBody
    Map<String, String> MessageResourceSearchAll(@RequestParam("value") String value);

    /**
     * 模糊查询某种国际化value含有指定字符串的国际化key集合 不区分大小写 指定语言类型
     *
     * @param value    国际值 条件字符串
     * @param language 语言类型 required = true
     * @return 返回国际化key value集合
     */
    @ApiOperation(value = "根据语言和部分国际化value模糊查询", notes = "模糊查询某种国际化value含有指定字符串的国际化key集合 不区分大小写 指定语言类型")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "value", value = "查询字符", required = true, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "language", value = "语言类型 eg:zh_CN", required = true, paramType = "query", dataType = "String", example = "zh_CN")
    })
    @GetMapping(API_PREFIX + "resource/search/one/case")
    @ResponseBody
    Map<String, String> MessageResourceSearchOneMatchCase(@RequestParam("value") String value, @RequestParam(value = "language") String language);


    /**
     * 模糊查询某种国际化value含有指定字符串的国际化key集合 不区分大小写 默认语言类型
     *
     * @param value 国际值 条件字符串
     * @return 返回国际化key value集合
     */
    @ApiOperation(value = "根据语言和部分国际化value模糊查询", notes = "模糊查询某种国际化value含有指定字符串的国际化key集合 不区分大小写 默认语言类型")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "value", value = "查询字符", required = true, paramType = "query", dataType = "String")
    })
    @GetMapping(API_PREFIX + "resource/search/all/case")
    @ResponseBody
    Map<String, String> MessageResourceSearchAllMatchCase(@RequestParam("value") String value);

    /**
     * 模糊查询指定模块下某种国际化value含有指定字符串的国际化key集合 不区分大小写 默认语言类型
     *
     * @param value      国际值 条件字符串
     * @param moduleCode 模块code
     * @return 返回国际化key value集合
     */
    @ApiOperation(value = "根据模块编码和部分国际化value模糊查询 默认语言类型", notes = "模糊查询指定模块下某种国际化value含有指定字符串的国际化key集合 不区分大小写 默认语言类型")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "value", value = "查询字符", required = true, paramType = "query", dataType = "String", example = "menu"),
            @ApiImplicitParam(name = "moduleCode", value = "模块编码", required = true, paramType = "query", dataType = "String", example = "rbac")
    })
    @GetMapping(API_PREFIX + "resource/search/module/case")
    @ResponseBody
    Map<String, String> messageResourceSearchModuleMatchCase(@RequestParam("value") String value, @RequestParam(value = "moduleCode") String moduleCode);


    /**
     * 获取某个模块的所有语言类型国际化键值对
     *
     * @param moduleCode 模块code
     * @return 返回国际化key value集合
     */
    @ApiOperation(value = "根据模块编码查询这个模块的所有国际化key-value", notes = "获取某个模块的所有语言类型国际化键值对")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCode", value = "模块编码", required = true, paramType = "query", dataType = "String")
    })
    @GetMapping(API_PREFIX + "resource/module/all")
    @ResponseBody
    Map<String, Map<String, String>> MessageResourceGetByModuleCodeAllLanguage(@RequestParam(value = "moduleCode", required = true) String moduleCode);


    /**
     * 获取某个模块的指定语言类型国际化键值对  没有语言 按上下文默认语言 如果也没有 按国际化配置中的默认语言（中文）
     *
     * @param moduleCode 模块code
     * @param language   language  required = false
     * @return 返回国际化key value集合
     */
    @ApiOperation(value = "根据模块编码查询这个模块的所有国际化key-value", notes = "获取某个模块的指定语言类型国际化键值对  没有语言 按上下文默认语言 如果也没有 按国际化配置中的默认语言（中文）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCode", value = "模块编码", required = true, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "language", value = "语言类型 eg:zh_CN", required = true, paramType = "query", dataType = "String", example = "zh_CN")
    })
    @GetMapping(API_PREFIX + "resource/module/one")
    @ResponseBody
    Map<String, Map<String, String>> MessageResourceGetByModuleCodeOneLanguage(@RequestParam(value = "moduleCode", required = true) String moduleCode, @RequestParam(value = "language", required = false) String language);


    /**
     * 获取某个key的所有语言类型国际化键值对
     *
     * @param key key
     * @return 返回国际化key value集合
     */
    @ApiOperation(value = "根据单个key查询", notes = "获取某个key的所有语言类型国际化键值对")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "key", value = "国际化key", required = true, paramType = "query", dataType = "String", example = "i18n.exception.language_has_no_error")
    })
    @GetMapping(API_PREFIX + "resource/key/all")
    @ResponseBody
    Map<String, Map<String, String>> MessageResourceGetByKeyAllLanguage(@RequestParam(value = "key", required = true) String key);

    /**
     * 获取某个key的所有语言类型国际化键值对 没有语言 按上下文默认语言 如果也没有 按国际化配置中的默认语言（中文）
     *
     * @param key      key
     * @param language language  required = false
     * @return 返回国际化key value集合
     */
    @ApiOperation(value = "根据单个key查询", notes = "获取某个key的所有语言类型国际化键值对 没有语言 按上下文默认语言 如果也没有 按国际化配置中的默认语言（中文）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "key", value = "国际化key", required = true, paramType = "query", dataType = "String", example = "i18n.exception.language_has_no_error"),
            @ApiImplicitParam(name = "language", value = "语言类型 eg:zh_CN", required = false, paramType = "query", dataType = "String", example = "zh_CN")
    })
    @GetMapping(API_PREFIX + "resource/key/one")
    @ResponseBody
    Map<String, String> MessageResourceGetByKeyOneLanguage(@RequestParam(value = "key", required = true) String key, @RequestParam(value = "language", required = false) String language);


    /**
     * 获取某个key的所有语言类型国际化键值对 没有语言 按上下文默认语言 如果也没有 按国际化配置中的默认语言（中文）  不校验key是否以模块名开头
     *
     * @param key      key
     * @param language language  required = false
     * @return 返回国际化key value集合
     */
    @ApiOperation(value = "根据单个key查询", notes = "获取某个key的所有语言类型国际化键值对 没有语言 按上下文默认语言 如果也没有 按国际化配置中的默认语言（中文）  不校验key是否以模块名开头")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "key", value = "国际化key", required = true, paramType = "query", dataType = "String",example = "i18n.exception.language_has_no_error"),
            @ApiImplicitParam(name = "language", value = "语言类型 eg:zh_CN", required = false, paramType = "query", dataType = "String",example = "zh_CN")
    })
    @GetMapping(API_PREFIX + "resource/key/one/no/check")
    @ResponseBody
    Map<String, String> messageResourceGetByKeyOneLanguage(@RequestParam(value = "key", required = true) String key, @RequestParam(value = "language", required = false) String language);


    /**
     * 获取所有语言类型
     *
     * @param
     * @return 返回国际化key value集合
     */
    @ApiOperation(value = "获取所有语言类型", notes = "获取当前数据库中所有语言类型")
    @GetMapping(API_PREFIX + "resource/language/all")
    @ResponseBody
    Map<String, Map<String, Object>> getAllLanguage();

    /**
     * 获取多个模块的所有语言类型国际化
     *
     * @param moduleCodes 多个模块code
     * @return 返回国际化key value集合
     */
    @ApiOperation(value = "获取多个模块的所有语言类型国际化", notes = "获取多个模块的所有语言类型国际化")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCodes", value = "模块编码字符串数组", required = true, paramType = "query", dataType = "String[]",example = "['i18n','rbac']")
    })
    @GetMapping(API_PREFIX + "resource/modules/all")
    @ResponseBody
    Map<String, Map<String, Map<String, String>>> messageResourceGetByModuleCodesAllLanguage(@RequestParam(value = "moduleCodes", required = true) String[] moduleCodes);


    /**
     * 实体配置上载包国际化资源上传   入参
     *
     * @param map 多个模块的多个国际化资源   {moduleCode:rbac,moduleVersion:rbac202009301113} 拼接在url中
     * @return 返回 模块索引
     */
    @ApiOperation(value = "上传某个模块的国际化资源", notes = "上传某个模块的国际化资源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "map", value = "入参", required = true, paramType = "query", dataType = "map", example = "{moduleCode:rbac,moduleVersion:rbac202009301113}"),
            @ApiImplicitParam(name = "file", value = "文件对象", required = true, paramType = "query", dataType = "String", example = "MultipartFile")
    })
    @PostMapping(value = API_PREFIX + "resource/upload/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    Result messageResourceUploadProFile(@RequestPart("file") MultipartFile uploadFile, @RequestParam Map map);

    /**
     * 工程期 实体配置上载包国际化资源上载
     *
     * @param uploadFile
     * @param map
     * @return
     */
    @ApiOperation(value = "工程期上传模块国际化资源", notes = "工程期上传模块国际化资源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "map", value = "入参", required = true, paramType = "query", dataType = "map", example = "{moduleCode:rbac}"),
            @ApiImplicitParam(name = "file", value = "文件对象", required = true, paramType = "query", dataType = "String", example = "MultipartFile")
    })
    @PostMapping(value = API_PREFIX + "resource/upload/projFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    Result messageResourceUploadCustomFile(@RequestPart("file") MultipartFile uploadFile, @RequestParam Map map);

    /**
     * 选中某种类型是创建国际化key(根据模块名称创建国际化key值)  根据模块名称创建国际化key值
     */
    @ApiOperation(value = "根据模块名称创建国际化key值", notes = "根据模块名称创建国际化key值 单个")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCode", value = "模块编码", required = true, paramType = "query", dataType = "String", example = "rbac")
    })
    @GetMapping(value = {API_PREFIX + "resource/init/key"})
    String initI18nKey(@RequestParam(value = "moduleCode") String moduleCode);

    /**
     * 根据模块名称创建国际化key值 返回一批当前模块的国际化key
     * moduleCode ：模块编码
     * num：生成该模块国际化key数量
     */
    @ApiOperation(value = "根据模块名称创建国际化key值", notes = "根据模块名称创建国际化key值 批量 10000 以内")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCode", value = "模块编码", required = true, paramType = "query", dataType = "String", example = "rbac"),
            @ApiImplicitParam(name = "num", value = "模块编码", required = true, paramType = "query", dataType = "Integer", example = "2")

    })
    @GetMapping(value = {API_PREFIX + "resource/init/keys"})
    List<String> initI18nKeys(@RequestParam(value = "moduleCode") String moduleCode,
                              @RequestParam(value = "num") Integer num);

    /**
     * 新增单个国际化
     *
     * @param map 国际化key 对应的多个语言的 value 模块code
     *            {"i18n_key":"organization.base.calendar.set1898989121212212","i18n_value": {"en_US":"3333333","zh_HK":"6666","zh_CN":"9999"},"moduleCode":"organization"}
     * @return 返回新增结果
     */
    @ApiOperation(value = "新增单条国际化", notes = "新增单条国际化 国际化key必须以模块编码开头 参数map eg:{'i18n_key':'organization.base.calendar.set1898989121212212','i18n_value': {'en_US':'3333333','zh_HK':'6666','zh_CN':'9999'},'moduleCode':'organization'}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "map", value = "单条国际化所需要的信息", required = true, paramType = "body", dataType = "map", example = "{'i18n_key':'organization.base.calendar.set1898989121212212','i18n_value': {'en_US':'3333333','zh_HK':'6666','zh_CN':'9999'},'moduleCode':'organization'}"),
    })
    @PostMapping(API_PREFIX + "resource/update/one")
    @ResponseBody
    Result messageResourceAddOrUpdateOne(@RequestBody Map<String, Object> map);

    /**
     * 某个模块下的一批国际化资源
     *
     * @param map        国际化key 对应的多个语言的 value 模块code
     *                   {"organization.base.calendar.set1898989121212212":"组织日历","organization.base.calendar.set189898912121228889":"日历"}
     * @param moduleCode 模块编码  必传
     * @param language   语言类型  非必传
     * @return 返回新增结果
     */
    @ApiOperation(value = "新增某个模块下的一批国际化资源", notes = "新增某个模块下的一批国际化资源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "map", value = "某个模块下某种语言的多个键值对", required = true, paramType = "body", dataType = "map", example = "\"organization.base.calendar.set1898989121212212\":\"组织日历\",\"organization.base.calendar.set189898912121228889\":\"日历\"}"),
            @ApiImplicitParam(name = "moduleCode", value = "模块编码", required = true, paramType = "query", dataType = "String", example = "organization"),
            @ApiImplicitParam(name = "language", value = "语言类型", required = false, paramType = "query", dataType = "String", example = "zh_CN")
    })
    @PostMapping(API_PREFIX + "resource/update/list")
    @ResponseBody
    Result messageResourceAddOrUpdateList(@RequestBody Map<String, String> map, @RequestParam("moduleCode") String moduleCode, @RequestParam(value = "language", required = false) String language);

    /**
     * 删除多个国际化
     *
     * @param keys 国际化key 集合
     * @return 返回国际化删除结果
     */
    @ApiOperation(value = "删除多个国际化", notes = "删除多个国际化")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "keys", value = "国际化key数组", required = true, paramType = "query", dataType = "String[]")
    })
    @DeleteMapping(API_PREFIX + "resource/delete/keys")
    @ResponseBody
    Result messageResourceDeleteKeys(@RequestParam("keys") String[] keys);

//    /**
//     * 删除多个模块的国际化资源
//     *
//     * @param moduleCodes 模块code
//     * @return 返回模块的国际化删除结果
//     */
//    @ApiOperation(value = "删除多个模块的国际化资源", notes = "删除多个模块的国际化资源")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "moduleCodes", value = "模块编码数组", required = true, paramType = "query", dataType = "String[]")
//    })
//    @DeleteMapping(API_PREFIX + "resource/modules/delete")
//    @ResponseBody
//    Result messageResourceDeleteByModuleCodes(@RequestParam(value = "moduleCodes", required = true) String[] moduleCodes);

    /**
     * 获取所有业务模块的所有语言类型国际化 资源 （排除几个模块）
     *
     * @param moduleCodes 需要排除的多个模块code
     * @return 返回国际化key value集合
     */
    @ApiOperation(value = "获取多个业务模块的所有语言类型国际化资源", notes = "获取所有业务模块的所有语言类型国际化 资源 （排除几个模块 参数中传递要排除的模块编码）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCodes", value = "模块编码数组", required = false, paramType = "query", dataType = "String[]")
    })
    @GetMapping(API_PREFIX + "resource/modules/all/custom")
    @ResponseBody
    Map<String, Map<String, Map<String, String>>> getAllBIZModuleAllLanguageResource(@RequestParam(value = "moduleCodes", required = false) String[] moduleCodes);

    /**
     * 获取所有系统平台模块的所有语言类型国际化 资源 （排除几个模块）
     *
     * @param moduleCodes 多个模块code
     * @return 返回国际化key value集合
     */
    @ApiOperation(value = "获取多个系统模块的所有语言类型国际化资源", notes = "获取所有系统模块的所有语言类型国际化 资源 （排除几个模块 参数中传递要排除的模块编码）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCodes", value = "模块编码数组", required = false, paramType = "query", dataType = "String[]")
    })
    @GetMapping(API_PREFIX + "resource/modules/all/system")
    @ResponseBody
    Map<String, Map<String, Map<String, String>>> getAllSystemModuleAllLanguageResource(@RequestParam(value = "moduleCodes", required = false) String[] moduleCodes);

    /**
     * 获取所有模块的所有语言类型国际化
     *
     * @return 返回国际化key value集合
     */
    @ApiOperation(value = "获取所有模块的所有语言类型国际化资源", notes = "获取所有模块的所有语言类型国际化")
    @GetMapping(API_PREFIX + "resource/modules/codes/all")
    @ResponseBody
    Map<String, Map<String, Map<String, String>>> getAllModuleAllLanguageResource();


    /**
     * 获取指定模块的所有语言类型国际化
     *
     * @param moduleCodes 多个模块code
     * @return 返回国际化key value集合
     */
    @ApiOperation(value = "获取指定模块的所有语言类型国际化", notes = "获取指定模块的所有语言类型国际化 （参数中传递要的模块编码）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCodes", value = "模块编码数组", required = false, paramType = "query", dataType = "String[]")
    })
    @GetMapping(API_PREFIX + "resource/need/modules")
    @ResponseBody
    Map<String, Map<String, Map<String, String>>> getModulesResource(@RequestParam(value = "moduleCodes") String[] moduleCodes);

    /**
     * 查询某个模块的国际化资源 非运行期资源（初始化资源 ） 会有多个版本返回最新版本的资源
     *
     * @param moduleCode 模块编码
     * @return 返回 zip 资源包
     */
    @ApiOperation(value = "查询某个模块的国际化资源", notes = "查询某个模块的国际化资源 非运行期资源（初始化资源 ） 会有多个版本返回最新版本的资源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCode", value = "模块编码", required = true, paramType = "query", dataType = "String")
    })
    @GetMapping(value = API_PREFIX + "resource/download/files")
    @ResponseBody
    Map<String, Map<String, String>> downloadFiles(@RequestParam(value = "moduleCode") String moduleCode);

    /**
     * 获取某个模块的国际化资源 运行期变化的资源（custom 目录下的资源）
     *
     * @param moduleCode 模块编码
     * @return 返回 zip 资源包
     */
    @ApiOperation(value = "查询某个模块的国际化资源", notes = "获取某个模块的国际化资源 运行期变化的资源（custom 目录下的资源）")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCode", value = "模块编码", required = true, paramType = "query", dataType = "String")
    })
    @GetMapping(value = API_PREFIX + "resource/download/custom/files")
    @ResponseBody
    Map<String, Map<String, String>> downloadCustomFiles(@RequestParam(value = "moduleCode") String moduleCode);

}