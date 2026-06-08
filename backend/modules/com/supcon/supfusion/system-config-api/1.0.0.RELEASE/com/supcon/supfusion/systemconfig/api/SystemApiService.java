package com.supcon.supfusion.systemconfig.api;


import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.systemconfig.api.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lifangyuan
 */
@FeignClient(name = "systemconfig")
public interface SystemApiService {
    String API_PREFIX = "/service-api/systemconfig/v2";


    /**
     * 注册配置
     *
     * @param catalogs
     * @return
     */
    @PostMapping(API_PREFIX + "/config/catalog")
    Result<Boolean> create(@Validated @RequestBody CatalogsDTO catalogs);


    /**
     * 删除配置
     *
     * @param appCode
     * @return true 成功
     */
    @DeleteMapping(API_PREFIX + "/config/catalog/{appCode}")
    Result<Boolean> destroy(@PathVariable("appCode") String appCode);

    /**
     * 根据appCode code 获取配置项详情
     *
     * @param appCode
     * @param code
     * @return
     */
    @GetMapping(API_PREFIX + "/config/catalog/{appCode}/{code}")
    @ResponseBody
    Result<ConfigsDTO> getConfigByAppCode(@PathVariable("appCode") String appCode, @PathVariable("code") String code);

    /**
     * 为框架提供配置数据
     *
     * @return
     */
    @GetMapping(API_PREFIX + "/config/catalog/getConfigInfo")
    @ResponseBody
    ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>> getConfigInfoForFramework();

    /**
     * 为框架提供配置版本
     */
    @GetMapping(API_PREFIX + "/config/catalog/configVersion")
    @ResponseBody
    ConcurrentHashMap<String, String> getConfigVersionForFramework();

    /**
     * 根据版本号得到和配置系统中有差异的版本，获取这部分有差异版本对应的配置数据
     *
     * @return
     */
    @PostMapping(API_PREFIX + "/config/catalog/getConfigByVersion")
    @ResponseBody
    ConfigAndVersionDTO getConfigByVersionForFramework(@RequestBody ConcurrentHashMap<String, String> versionMapOfFramework);

    /**
     * 保存ocd xml文本
     *
     * @return
     */
    @PostMapping(API_PREFIX + "/config/catalog/saveOcdContent")
    Result<Boolean> saveOcdContent(@Valid @RequestBody XmlContentDTO xmlContentVO) throws Exception;

    /**
     * 修改配置项
     */
    @PutMapping(API_PREFIX + "/config/catalog/updateConfig")
    @ResponseBody
    Result<Boolean> updateConfig(@Valid @RequestBody UpdateConfigDTO updateConfigDTO);
}
