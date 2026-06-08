package com.supcon.supfusion.systemconfig.service;


import com.supcon.supfusion.systemconfig.api.dto.ConfigAndVersionDTO;
import com.supcon.supfusion.systemconfig.common.enums.CatalogTypeEnum;
import com.supcon.supfusion.systemconfig.service.bo.CatalogBo;
import com.supcon.supfusion.systemconfig.service.bo.ConfigInfoBo;
import com.supcon.supfusion.systemconfig.service.bo.ConfigOptionBo;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lifangyuan
 */
public interface SystemConfigService {

    /**
     * 批量插入配置分类 配置项 配置项下拉
     *
     * @param catalogBos
     * @param configInfoBos
     * @param configOptionBos
     */
    void batchInsertConfig(List<CatalogBo> catalogBos, List<ConfigInfoBo> configInfoBos, List<ConfigOptionBo> configOptionBos);


    /**
     * 根据业务标识获取配置分类
     *
     * @param code
     * @return
     */
    CatalogBo selectByCode(String code);

    /**
     * 根据appCode获取配置分类
     *
     * @param appCode
     * @return
     */
    CatalogBo selectByAppCode(String appCode);


    /**
     * 根据appCode获取配置分类
     *
     * @param appCode
     * @param code
     * @return
     */
    CatalogBo selectCatalogOne(String appCode, String code);

    /**
     * 根据id获取配置分类
     *
     * @param id
     * @return
     */
    CatalogBo selectCatalogById(Long id);

    /**
     * 查询配置分类
     *
     * @param catalogBo
     * @return
     */
    List<CatalogBo> selectCatalog(CatalogBo catalogBo);


    /**
     * 查询父配置分类
     *
     * @param
     * @return
     */
    List<CatalogBo> selectParentCatalog();


    /**
     * 根据业务标识获取配置项
     *
     * @param code
     * @param appCode
     * @return
     */
    ConfigInfoBo selectConfigInfoOne(String appCode, String code);


    /**
     * 根据业务标识获取配置项
     *
     * @param id
     * @return
     */
    ConfigInfoBo selectConfigInfoById(Long id);

    /**
     * 查询配置项
     *
     * @param configInfoBo
     * @return
     */
    List<ConfigInfoBo> selectConfigInfo(ConfigInfoBo configInfoBo);

    /**
     * 根据目录id和key获取配置项
     * @param configInfoBo
     * @return
     */
    List<ConfigInfoBo> selectByCatalogIdAndKey(ConfigInfoBo configInfoBo);


    /**
     * 查询配置下拉
     *
     * @param configOptionBo
     * @return
     */
    List<ConfigOptionBo> selectConfigOption(ConfigOptionBo configOptionBo);

    /**
     * 更新配置项
     *
     * @param configInfoBo
     */
    void updateConfigInfoById(ConfigInfoBo configInfoBo);


    /**
     * 批量删除配置
     *
     * @param appCode
     * @return
     */
    void deleteBatchIds(String appCode);


    /**
     * 批量删除配置fenlei
     *
     * @param appCode
     * @param code
     * @return
     */
    void deleteBatchIds(String appCode, String code);


    /**
     * 根据appCode  获取配置项
     *
     * @param appCode
     * @return
     */
    List<ConfigInfoBo> getConfigByAppCode(String appCode);

    /**
     * 根据catalogId获取配置项
     *
     * @return
     */
    List<ConfigInfoBo> getConfigListByCatalogId(Long catalogId);

    /**
     * 根据configId获取option选项
     *
     * @return
     */
    List<ConfigOptionBo> getOptionListByConfigId(Long configId);

    List<CatalogBo> getCatalogByKeyword();

    /**
     * 将系统配置存入本地缓存
     *
     * @param catalogBos
     */
    void setConfigInfoCache(List<CatalogBo> catalogBos);

    /**
     * 将系统配置存入本地缓存
     *
     * @param config
     */
    void updateConfigInfo(ConfigInfoBo config);

    /**
     * 初始化配置数据到本地缓存中
     */
    void initSystemConfig();


    /**
     * 定时任务刷新数据及版本缓存
     */
    void scheduleRefreshConfigCache();

    /**
     * 为框架提供配置数据
     *
     * @return
     */
    ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>> getConfigInfoForFramework();


    /**
     * 为框架提供配置版本
     *
     * @return
     */
    ConcurrentHashMap<String, String> getConfigVersionForFramework();

    /**
     * 根据版本号得到和配置系统中有差异的版本，获取这部分有差异版本对应的配置数据
     *
     * @param versionMapOfFramework
     * @return
     */
    ConfigAndVersionDTO getConfigByVersionForFramework(ConcurrentHashMap<String, String> versionMapOfFramework);

    /**
     * 根据appCode和moduleCode修改配置项
     *
     * @param bo
     */
    void updateConfigInfoByAppCodeAndModuleCode(ConfigInfoBo bo);

    /**
     *根据appCode和moduleCode和key查询配置项
     * @param bo
     * @return
     */
    ConfigInfoBo selectConfigInfoByModuleCodeAndAppCodeAndKey(ConfigInfoBo bo);

    void refreshConfigCacheCommon();
}
