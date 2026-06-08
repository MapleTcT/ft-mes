package com.supcon.supfusion.systemconfig.service.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfoLocalStorage;
import com.supcon.supfusion.systemconfig.api.dto.ConfigAndVersionDTO;
import com.supcon.supfusion.systemconfig.common.constants.Constants;
import com.supcon.supfusion.systemconfig.common.util.SystemUtil;
import com.supcon.supfusion.systemconfig.dao.entity.ConfigCatalogPO;
import com.supcon.supfusion.systemconfig.dao.entity.ConfigInfoPO;
import com.supcon.supfusion.systemconfig.dao.entity.ConfigOptionPO;
import com.supcon.supfusion.systemconfig.dao.entity.ConfigVersionPO;
import com.supcon.supfusion.systemconfig.service.*;
import com.supcon.supfusion.systemconfig.service.bo.CatalogBo;
import com.supcon.supfusion.systemconfig.service.bo.ConfigInfoBo;
import com.supcon.supfusion.systemconfig.service.bo.ConfigOptionBo;
import com.supcon.supfusion.systemconfig.service.utils.collectionutil.CollectionUtil;
import com.supcon.supfusion.systemconfig.service.utils.datahelper.DateHelper;
import com.supcon.supfusion.systemconfig.service.utils.datahelper.TimeHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author lifangyuan
 */
@Slf4j
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    public static ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>
            configMap = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, String> configVersionMap = new ConcurrentHashMap<>();

    private static final String KEY_SPLIT = "/";
    private static int isInit = 0;

    @Resource
    private CatalogService catalogService;
    @Resource
    private ConfigInfoService configInfoService;
    @Resource
    private ConfigOptionService configOptionService;
    @Resource
    private ConfigVersionService configVersionService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchInsertConfig(List<CatalogBo> catalogBos, List<ConfigInfoBo> configInfoBos, List<ConfigOptionBo> configOptionBos) {
        List<ConfigCatalogPO> collect = catalogBos.stream().map(t -> {
            ConfigCatalogPO configCatalogPo = new ConfigCatalogPO();
            BeanUtils.copyProperties(t, configCatalogPo);
            return configCatalogPo;
        }).collect(Collectors.toList());
        List<ConfigInfoPO> collect1 = configInfoBos.stream().map(t -> {
            ConfigInfoPO configInfoPo = new ConfigInfoPO();
            BeanUtils.copyProperties(t, configInfoPo);
            configInfoPo.setHasRequire(t.getIsRequire());
            return configInfoPo;
        }).collect(Collectors.toList());
        List<ConfigOptionPO> collect2 = configOptionBos.stream().map(t -> {
            ConfigOptionPO configOptionPo = new ConfigOptionPO();
            BeanUtils.copyProperties(t, configOptionPo);
            return configOptionPo;
        }).collect(Collectors.toList());
        catalogService.saveBatch(collect);
        configInfoService.saveBatch(collect1);
        configOptionService.saveBatch(collect2);
    }

    @Override
    public CatalogBo selectByCode(String code) {
        ConfigCatalogPO po = catalogService.getOne(new QueryWrapper<ConfigCatalogPO>().lambda().eq(ConfigCatalogPO::getCode, code));
        if (po == null) {
            return null;
        } else {
            CatalogBo catalogBo = new CatalogBo();
            BeanUtils.copyProperties(po, catalogBo);
            return catalogBo;
        }
    }


    @Override
    public CatalogBo selectByAppCode(String appCode) {
        ConfigCatalogPO po = catalogService.getOne(new QueryWrapper<ConfigCatalogPO>().lambda().eq(ConfigCatalogPO::getAppCode, appCode));
        if (po == null) {
            return null;
        } else {
            CatalogBo catalogBo = new CatalogBo();
            BeanUtils.copyProperties(po, catalogBo);
            return catalogBo;
        }
    }

    @Override
    public CatalogBo selectCatalogOne(String appCode, String code) {
        ConfigCatalogPO po = catalogService.getOne(new QueryWrapper<ConfigCatalogPO>().lambda().eq(ConfigCatalogPO::getAppCode, appCode).eq(ConfigCatalogPO::getCode, code));
        if (po == null) {
            return null;
        } else {
            CatalogBo catalogBo = new CatalogBo();
            BeanUtils.copyProperties(po, catalogBo);
            return catalogBo;
        }
    }

    @Override
    public CatalogBo selectCatalogById(Long id) {
        ConfigCatalogPO po = catalogService.getById(id);
        if (po == null) {
            return null;
        } else {
            CatalogBo catalogBo = new CatalogBo();
            BeanUtils.copyProperties(po, catalogBo);
            return catalogBo;
        }
    }

    @Override
    public List<CatalogBo> selectParentCatalog() {
        List<ConfigCatalogPO> configCatalogPos = catalogService.list(new QueryWrapper<ConfigCatalogPO>().lambda().isNull(true, ConfigCatalogPO::getParentId).eq(ConfigCatalogPO::getHasHide, false).orderByAsc(ConfigCatalogPO::getSort));
        return configCatalogPos.stream().map(t -> {
            CatalogBo temp = new CatalogBo();
            BeanUtils.copyProperties(t, temp);
            return temp;
        }).collect(Collectors.toList());
    }

    @Override
    public List<CatalogBo> selectCatalog(CatalogBo catalogBo) {
        List<ConfigCatalogPO> configCatalogPos = catalogService.list(new QueryWrapper<ConfigCatalogPO>().lambda().eq(ConfigCatalogPO::getParentId, catalogBo.getId()).eq(ConfigCatalogPO::getHasHide, false).orderByAsc(ConfigCatalogPO::getSort));
        return configCatalogPos.stream().map(t -> {
            CatalogBo temp = new CatalogBo();
            BeanUtils.copyProperties(t, temp);
            return temp;
        }).collect(Collectors.toList());
    }


    @Override
    public ConfigInfoBo selectConfigInfoOne(String appCode, String code) {
        ConfigInfoPO configInfoPo = configInfoService.getOne(new QueryWrapper<ConfigInfoPO>().lambda().eq(ConfigInfoPO::getAppCode, appCode).eq(ConfigInfoPO::getCode, code));
        if (configInfoPo == null) {
            return null;
        } else {
            ConfigInfoBo bo = new ConfigInfoBo();
            BeanUtils.copyProperties(configInfoPo, bo);
            return bo;
        }
    }

    @Override
    public ConfigInfoBo selectConfigInfoById(Long id) {
        ConfigInfoPO configInfoPo = configInfoService.getById(id);
        if (configInfoPo == null) {
            return null;
        } else {
            ConfigInfoBo bo = new ConfigInfoBo();
            BeanUtils.copyProperties(configInfoPo, bo);
            return bo;
        }
    }

    @Override
    public List<ConfigInfoBo> selectConfigInfo(ConfigInfoBo configInfoBo) {
        List<ConfigInfoPO> configInfoPos = configInfoService.list(new QueryWrapper<ConfigInfoPO>().lambda().eq(ConfigInfoPO::getCatalogId, configInfoBo.getCatalogId()).orderByAsc(ConfigInfoPO::getSort));
        return configInfoPos.stream().map(t -> {
            ConfigInfoBo temp = new ConfigInfoBo();
            BeanUtils.copyProperties(t, temp);
            temp.setIsRequire(t.getHasRequire());
            return temp;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ConfigInfoBo> selectByCatalogIdAndKey(ConfigInfoBo configInfoBo) {
        List<ConfigInfoPO> list = configInfoService.list(new QueryWrapper<ConfigInfoPO>().lambda().eq(ConfigInfoPO::getCatalogId, configInfoBo.getCatalogId())
                .eq(ConfigInfoPO::getCode, configInfoBo.getCode()).orderByAsc(ConfigInfoPO::getSort));

        return list.stream().map(configInfoPO -> {
            ConfigInfoBo configInfoBo1 = new ConfigInfoBo();
            BeanUtils.copyProperties(configInfoPO,configInfoBo1);
            configInfoBo1.setIsRequire(configInfoPO.getHasRequire());
            return configInfoBo1;
        }).collect(Collectors.toList());
    }


    @Override
    public List<ConfigOptionBo> selectConfigOption(ConfigOptionBo configOptionBo) {
        List<ConfigOptionPO> configOptionPos = configOptionService.list(new QueryWrapper<ConfigOptionPO>().lambda().eq(ConfigOptionPO::getConfigId, configOptionBo.getConfigId()).orderByAsc(ConfigOptionPO::getSort));
        return configOptionPos.stream().map(t -> {
            ConfigOptionBo temp = new ConfigOptionBo();
            BeanUtils.copyProperties(t, temp);
            return temp;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfigInfoById(ConfigInfoBo configInfoBo) {
        ConfigInfoPO configInfoPo = new ConfigInfoPO();
        BeanUtils.copyProperties(configInfoBo, configInfoPo);
        configInfoService.updateById(configInfoPo);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatchIds(String appCode) {
        List<ConfigCatalogPO> configCatalogPos = catalogService.list(new QueryWrapper<ConfigCatalogPO>().lambda().eq(ConfigCatalogPO::getAppCode, appCode).isNotNull(ConfigCatalogPO::getParentId));
        if (configCatalogPos.isEmpty()) {
            return;
        }
        List<Long> idList = configCatalogPos.stream().map(ConfigCatalogPO::getId).collect(Collectors.toList());
        List<ConfigInfoPO> configInfoPos = configInfoService.list(new QueryWrapper<ConfigInfoPO>().lambda().in(ConfigInfoPO::getCatalogId, idList));
        if (!configInfoPos.isEmpty()) {
            List<Long> collect = configInfoPos.stream().map(ConfigInfoPO::getId).collect(Collectors.toList());
            configInfoService.remove(new QueryWrapper<ConfigInfoPO>().lambda().in(ConfigInfoPO::getCatalogId, idList));
            configOptionService.remove(new QueryWrapper<ConfigOptionPO>().lambda().in(ConfigOptionPO::getConfigId, collect));
        }
        catalogService.removeByIds(idList);
        //删除本地缓存和配置版本库
        this.deleteCacheAndVersion(configCatalogPos);
    }

    @Override
    public void deleteBatchIds(String appCode, String code) {
        ConfigCatalogPO one = catalogService.getOne(new QueryWrapper<ConfigCatalogPO>().lambda().eq(ConfigCatalogPO::getAppCode, appCode).eq(ConfigCatalogPO::getCode, code));
        if (one == null) {
           return;
        }
        catalogService.removeById(one.getId());
        configInfoService.remove(new QueryWrapper<ConfigInfoPO>().lambda().eq(ConfigInfoPO::getCatalogId, one.getId()));
        //删除本地缓存和配置版本库
        List<ConfigCatalogPO> configCatalogPOS = new ArrayList<>();
        configCatalogPOS.add(one);
        this.deleteCacheAndVersion(configCatalogPOS);
    }

    /**
     * 删除本地缓存和配置版本库
     *
     * @param configCatalogPOS
     */
    private void deleteCacheAndVersion(List<ConfigCatalogPO> configCatalogPOS) {
        String tenantId = this.getTenantId();
        for (ConfigCatalogPO configCatalogPO : configCatalogPOS) {
            String tidModuleKey = tenantId + KEY_SPLIT + configCatalogPO.getAppCode() + KEY_SPLIT + configCatalogPO.getCode();
            configVersionService.remove(new QueryWrapper<ConfigVersionPO>().lambda().eq(ConfigVersionPO::getTidModuleKey, tidModuleKey));

            //删除缓存
            ConcurrentHashMap<String, HashMap<String, Object>> moduleMap = configMap.get(tenantId);
            if (!ObjectUtils.isEmpty(moduleMap)) {
                String moduleKey = configCatalogPO.getAppCode() + KEY_SPLIT + configCatalogPO.getCode();
                moduleMap.remove(moduleKey);
            }
        }
    }

    @Override
    public List<ConfigInfoBo> getConfigByAppCode(String appCode) {
        List<ConfigInfoPO> configInfoPos = configInfoService.list(new QueryWrapper<ConfigInfoPO>().lambda().eq(ConfigInfoPO::getAppCode, appCode));
        return configInfoPos.stream().map(t -> {
            ConfigInfoBo temp = new ConfigInfoBo();
            BeanUtils.copyProperties(t, temp);
            return temp;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ConfigInfoBo> getConfigListByCatalogId(Long catalogId) {
        List<ConfigInfoPO> configInfoPOList = configInfoService.list(new QueryWrapper<ConfigInfoPO>().lambda().eq(ConfigInfoPO::getCatalogId, catalogId));
        return configInfoPOList.stream().map(t -> {
            ConfigInfoBo temp = new ConfigInfoBo();
            BeanUtils.copyProperties(t, temp);
            return temp;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ConfigOptionBo> getOptionListByConfigId(Long configId) {
        List<ConfigOptionPO> list = configOptionService.list(new QueryWrapper<ConfigOptionPO>().lambda().eq(ConfigOptionPO::getConfigId, configId));
        return list.stream().map(a -> {
            ConfigOptionBo configOptionBo = new ConfigOptionBo();
            BeanUtils.copyProperties(a, configOptionBo);
            return configOptionBo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<CatalogBo> getCatalogByKeyword() {
        List<ConfigCatalogPO> list = catalogService.list(new QueryWrapper<ConfigCatalogPO>().lambda().isNotNull(ConfigCatalogPO::getParentId));
        return list.stream().map(t -> {
            CatalogBo catalogBo = new CatalogBo();
            BeanUtils.copyProperties(t, catalogBo);
            return catalogBo;
        }).collect(Collectors.toList());
    }

    @Override
    public void setConfigInfoCache(List<CatalogBo> catalogBos) {
        String tenantId = this.getTenantId();
        if (!ObjectUtils.isEmpty(catalogBos)) {
            for (CatalogBo catalogBo : catalogBos) {
                String moduleKey = catalogBo.getAppCode() + KEY_SPLIT + catalogBo.getCode();

                //修改版本库，根据租户模块key修改版本库
                String tidModuleKey = tenantId + KEY_SPLIT + moduleKey;
                //更新版本库
                ConfigVersionPO configVersionPO1 = new ConfigVersionPO();
                configVersionPO1.setConfigVersion(tidModuleKey + KEY_SPLIT + TimeHelper.getNowTime());
                configVersionPO1.setTidModuleKey(tidModuleKey);
                configVersionService.saveOrUpdate(configVersionPO1, new QueryWrapper<ConfigVersionPO>().lambda().
                        eq(ConfigVersionPO::getTidModuleKey, tidModuleKey));
            }
        }


       /* String tenantId = this.getTenantId();
//        ConcurrentHashMap<String, HashMap<String, Object>> moduleMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, HashMap<String, Object>> moduleMap = configMap.get(tenantId);
        if (!catalogBos.isEmpty()) {
            for (CatalogBo catalogBo : catalogBos) {
                HashMap<String, Object> hashMap = new HashMap<>();
//                List<ConfigInfoBo> bos = this.getConfigByAppCode(catalogBo.getAppCode());
                List<ConfigInfoBo> bos = this.getConfigListByCatalogId(catalogBo.getId());
                if (bos != null && !bos.isEmpty()) {
                    for (ConfigInfoBo temp : bos) {
                        if (StringUtils.isEmpty(temp.getWidgetValue()) && !StringUtils.isEmpty(temp.getDefaultValue())) {
                            String defaultValue = temp.getDefaultValue();
                            String[] split = defaultValue.split(",");
                            ArrayList<String> strings = new ArrayList<>(Arrays.asList(split));
                            hashMap.put(temp.getCode(), strings);
                        } else if (!StringUtils.isEmpty(temp.getWidgetValue())) {
                            String value = temp.getWidgetValue();
                            String[] split = value.split(",");
                            ArrayList<String> strings = new ArrayList<>(Arrays.asList(split));
                            hashMap.put(temp.getCode(), strings);
                        }
                    }

                    String moduleKey = catalogBo.getAppCode() + KEY_SPLIT + catalogBo.getCode();
                    moduleMap.put(moduleKey, hashMap);

                    //修改版本库，根据租户模块key修改版本库
                    String tidModuleKey = tenantId + KEY_SPLIT + moduleKey;
                    //更新版本库
                    ConfigVersionPO configVersionPO1 = new ConfigVersionPO();
                    configVersionPO1.setConfigVersion(tidModuleKey + KEY_SPLIT + TimeHelper.getNowTime());
                    configVersionPO1.setTidModuleKey(tidModuleKey);
                    configVersionService.saveOrUpdate(configVersionPO1, new QueryWrapper<ConfigVersionPO>().lambda().
                            eq(ConfigVersionPO::getTidModuleKey, tidModuleKey));
                }

            }
            //存入本地缓存
            configMap.put(tenantId, moduleMap);
        }*/
    }

    /**
     * 获取租户id
     *
     * @return
     */
    private String getTenantId() {
        String tenantId = RpcContext.getContext().getTenantId();
        if (ObjectUtils.isEmpty(tenantId)) {
            tenantId= System.getenv("SUPOS_SUPOS_APP_TENANT_ID");
            log.info("系统租户:{}",tenantId);
            if (ObjectUtils.isEmpty(tenantId)) {
                tenantId = "dt";
            }
            log.info("当前租户id为：{}",tenantId);
        }
        return tenantId;
    }


    public void updateConfigInfo(ConfigInfoBo configInfoBoParam) {
        String tenantId = this.getTenantId();
        if (configInfoBoParam.getCatalogId() == null) {
            ConfigInfoBo configInfoBo = this.selectConfigInfoById(configInfoBoParam.getId());
            configInfoBoParam.setCatalogId(configInfoBo.getCatalogId());
        }
        CatalogBo catalogBo = this.selectCatalogById(configInfoBoParam.getCatalogId());
        String moduleKey = catalogBo.getAppCode() + KEY_SPLIT + catalogBo.getCode();

        //修改版本库，根据租户模块key修改版本库
        String tidModuleKey = tenantId + KEY_SPLIT + moduleKey;

        //更新版本库
        ConfigVersionPO configVersionPO1 = new ConfigVersionPO();
        configVersionPO1.setConfigVersion(tidModuleKey + KEY_SPLIT + TimeHelper.getNowTime());
        configVersionPO1.setTidModuleKey(tidModuleKey);
        configVersionService.saveOrUpdate(configVersionPO1, new QueryWrapper<ConfigVersionPO>().lambda().
                eq(ConfigVersionPO::getTidModuleKey, tidModuleKey));

   /*
//        ConcurrentHashMap<String, HashMap<String, Object>> moduleMap = new ConcurrentHashMap<>();
        String tenantId = this.getTenantId();
        ConcurrentHashMap<String, HashMap<String, Object>> moduleMap = configMap.get(tenantId);
        HashMap<String, Object> hashMap = new HashMap<>();

        if (configInfoBoParam.getCatalogId() == null) {
            ConfigInfoBo configInfoBo = this.selectConfigInfoById(configInfoBoParam.getId());
            configInfoBoParam.setCatalogId(configInfoBo.getCatalogId());
        }
        CatalogBo catalogBo = this.selectCatalogById(configInfoBoParam.getCatalogId());
//        List<ConfigInfoBo> bos = this.getConfigByAppCode(catalogBo.getAppCode());
        List<ConfigInfoBo> bos = this.getConfigListByCatalogId(catalogBo.getId());

        if (bos != null && !bos.isEmpty()) {
            for (ConfigInfoBo temp : bos) {
                if (StringUtils.isEmpty(temp.getWidgetValue()) && !StringUtils.isEmpty(temp.getDefaultValue())) {
                    String defaultValue = temp.getDefaultValue();
                    String[] split = defaultValue.split(",");
                    ArrayList<String> strings = new ArrayList<>(Arrays.asList(split));
                    hashMap.put(temp.getCode(), strings);
                } else if (!StringUtils.isEmpty(temp.getWidgetValue())) {
                    String value = temp.getWidgetValue();
                    String[] split = value.split(",");
                    ArrayList<String> strings = new ArrayList<>(Arrays.asList(split));
                    hashMap.put(temp.getCode(), strings);
                }
            }

            String moduleKey = catalogBo.getAppCode() + KEY_SPLIT + catalogBo.getCode();
            moduleMap.put(moduleKey, hashMap);

            //修改版本库，根据租户模块key修改版本库
            String tidModuleKey = tenantId + KEY_SPLIT + moduleKey;
            ConfigVersionPO configVersionPO = configVersionService.getOne(
                    new QueryWrapper<ConfigVersionPO>().lambda().eq(ConfigVersionPO::getTidModuleKey, tidModuleKey));
            if (ObjectUtils.isEmpty(configVersionPO)) {
                log.info("当前修改配置数据有错误,tidModuleKey:{}", tidModuleKey);
            } else {
                //更新版本库
                ConfigVersionPO configVersionPO1 = new ConfigVersionPO();
                configVersionPO1.setConfigVersion(tidModuleKey + KEY_SPLIT + TimeHelper.getNowTime());
                configVersionPO1.setTidModuleKey(tidModuleKey);
                configVersionService.saveOrUpdate(configVersionPO1, new QueryWrapper<ConfigVersionPO>().lambda().
                        eq(ConfigVersionPO::getTidModuleKey, tidModuleKey));
            }
        }

        //存入本地缓存
        configMap.put(tenantId, moduleMap);*/

    }


    private static final ScheduledExecutorService CONFIG_CACHE = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void scheduleRefreshConfigCache() {
        CONFIG_CACHE.scheduleAtFixedRate(() -> {
            log.info("定时刷新数据及版本缓存开始执行 =======");
            log.info("配置数据：{}", configMap);
            log.info("配置版本：{}", configVersionMap);
            try {
                this.refreshConfigCache();
            } catch (Exception e) {
                log.error("定时刷新版本缓存发生错误 =======", e);
            }
        }, 30 * DateHelper.SECOND_TIME, 30 * DateHelper.SECOND_TIME, TimeUnit.MILLISECONDS);
    }

    /**
     * 定时刷新数据及版本缓存
     */
    public void refreshConfigCache() {
        Set<TenantInfo> tenantInfoSet = TenantInfoLocalStorage.getAll();
        log.info("当前租户信息,tenantInfoSet:{}", JSON.toJSONString(tenantInfoSet));
        if (Constants.LINUX.equalsIgnoreCase(SystemUtil.getOS())) {
            tenantInfoSet.forEach(tenantInfo -> {
                RpcContext rpcContext = RpcContext.getContext();
                rpcContext.setTenantId(tenantInfo.getId());
                if (0 == isInit) {
                    this.initSystemConfigCommon();
                }
                this.refreshConfigCacheCommon();
            });
            isInit = 1;
        } else {
            if (0 == isInit) {
                this.initSystemConfigCommon();
                isInit = 1;
            }
            this.refreshConfigCacheCommon();
        }

    }

    @Override
    public void refreshConfigCacheCommon() {
        //版本缓存
        List<ConfigVersionPO> configVersionPOList = configVersionService.list();
        if (!ObjectUtils.isEmpty(configVersionPOList)) {
            for (ConfigVersionPO configVersionPO : configVersionPOList) {
                configVersionMap.put(configVersionPO.getTidModuleKey(), configVersionPO.getConfigVersion());
            }
        }

        //数据缓存
        List<ConfigCatalogPO> configCatalogPOList = catalogService.list();
        if (!ObjectUtils.isEmpty(configCatalogPOList)) {
            List<CatalogBo> catalogBoList = configCatalogPOList.stream().map(configCatalogPO -> {
                CatalogBo catalogBo = new CatalogBo();
                BeanUtils.copyProperties(configCatalogPO, catalogBo);
                return catalogBo;
            }).collect(Collectors.toList());
            this.handleListCatalogTask(catalogBoList);
        }
    }


    /**
     * 为框架提供配置数据
     *
     * @return
     */
    @Override
    public ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>> getConfigInfoForFramework() {
        return configMap;
    }

    /**
     * 为框架提供配置版本
     *
     * @return
     */
    @Override
    public ConcurrentHashMap<String, String> getConfigVersionForFramework() {
        return configVersionMap;
    }

    /**
     * 根据版本号得到和配置系统中有差异的版本，获取这部分有差异版本对应的配置数据
     *
     * @param versionMapOfFramework
     * @return
     */
    @Override
    public ConfigAndVersionDTO getConfigByVersionForFramework(ConcurrentHashMap<String, String> versionMapOfFramework) {
        ConfigAndVersionDTO configAndVersionDTO = new ConfigAndVersionDTO();

        Collection<String> versionOfFramework = versionMapOfFramework.keySet();
        Collection<String> versionOfLocal = configVersionMap.keySet();

        List<String> sameVersionList = CollectionUtil.getSame(versionOfFramework, versionOfLocal);
        if (!ObjectUtils.isEmpty(sameVersionList)) {
            for (String sameVersion : sameVersionList) {
                if (!versionMapOfFramework.get(sameVersion).equals(configVersionMap.get(sameVersion))) {
                    configAndVersionDTO.setIsUpdate(true);
                    configAndVersionDTO.setConfigMap(configMap);
                    configAndVersionDTO.setVersionMap(configVersionMap);
                    return configAndVersionDTO;
                }
            }
        }

        List<String> frameworkVersionList = new ArrayList<>(versionOfFramework);
        List<String> differentOfFramework = CollectionUtil.getDifferentList(frameworkVersionList, sameVersionList);
        if (!ObjectUtils.isEmpty(differentOfFramework)) {
            //删除配置数据
            configAndVersionDTO.setIsUpdate(true);
            configAndVersionDTO.setConfigMap(configMap);
            configAndVersionDTO.setVersionMap(configVersionMap);
            return configAndVersionDTO;
        }

        ArrayList<String> localVersionList = new ArrayList<>(versionOfLocal);
        List<String> differentOfLocal = CollectionUtil.getDifferentList(localVersionList, sameVersionList);
        if (!ObjectUtils.isEmpty(differentOfLocal)) {
            //该部分为新增配置数据
            configAndVersionDTO.setIsUpdate(true);
            configAndVersionDTO.setConfigMap(configMap);
            configAndVersionDTO.setVersionMap(configVersionMap);
            return configAndVersionDTO;
        }


        configAndVersionDTO.setIsUpdate(false);
        return configAndVersionDTO;
    }

    @Override
    public void updateConfigInfoByAppCodeAndModuleCode(ConfigInfoBo configInfoBo) {
        ConfigInfoPO configInfoPO = new ConfigInfoPO();
        BeanUtils.copyProperties(configInfoBo, configInfoPO);
        configInfoService.update(configInfoPO, new UpdateWrapper<ConfigInfoPO>().lambda().eq(ConfigInfoPO::getAppCode, configInfoPO.getAppCode()).
                eq(ConfigInfoPO::getModuleCode, configInfoPO.getModuleCode()).eq(ConfigInfoPO::getCode, configInfoPO.getCode()));
    }

    @Override
    public ConfigInfoBo selectConfigInfoByModuleCodeAndAppCodeAndKey(ConfigInfoBo bo) {
        ConfigInfoPO configInfoPO = configInfoService.getOne(new QueryWrapper<ConfigInfoPO>().lambda().eq(ConfigInfoPO::getCode, bo.getCode()).eq(ConfigInfoPO::getModuleCode, bo.getModuleCode()).eq(ConfigInfoPO::getAppCode, bo.getAppCode()));
        ConfigInfoBo temp = new ConfigInfoBo();
        BeanUtils.copyProperties(configInfoPO, temp);
        return temp;
    }


    /**
     * 初始化系统配置至本地缓存中
     */
    @Override
    public void initSystemConfig() {
        Set<TenantInfo> tenantInfos = TenantInfoLocalStorage.getAll();
        log.info("当前租户信息,tenantInfos:{}", JSON.toJSONString(tenantInfos));
        if (Constants.LINUX.equalsIgnoreCase(SystemUtil.getOS())) {
            tenantInfos.forEach(tenantInfo -> {
                String tenantId = tenantInfo.getId();
                RpcContext rpcContext = RpcContext.getContext();
                rpcContext.setTenantId(tenantId);

                this.initSystemConfigCommon();

            });
        } else {
            this.initSystemConfigCommon();
        }

    }

    private void initSystemConfigCommon() {
        List<ConfigCatalogPO> configCatalogPOList = catalogService.list();
        if (!ObjectUtils.isEmpty(configCatalogPOList)) {
            List<CatalogBo> catalogBoList = configCatalogPOList.stream().map(configCatalogPO -> {
                CatalogBo catalogBo = new CatalogBo();
                BeanUtils.copyProperties(configCatalogPO, catalogBo);
                return catalogBo;
            }).collect(Collectors.toList());
            this.handleListCatalog(catalogBoList);
        }
    }

    /**
     * 处理多个目录下的配置数据
     *
     * @param catalogBos
     */
    private void handleListCatalog(List<CatalogBo> catalogBos) {
        String tenantId = this.getTenantId();

        ConcurrentHashMap<String, HashMap<String, Object>> moduleMap = configMap.get(tenantId);
        if (ObjectUtils.isEmpty(moduleMap)) {
            moduleMap = new ConcurrentHashMap<>();
        }
        for (CatalogBo catalogBo : catalogBos) {
//            List<ConfigInfoBo> bos = this.getConfigByAppCode(catalogBo.getAppCode());
            String moduleKey = catalogBo.getAppCode() + KEY_SPLIT + catalogBo.getCode();
            HashMap<String, Object> hashMap = moduleMap.get(moduleKey);
            if (ObjectUtils.isEmpty(hashMap)) {
                hashMap = new HashMap<>();
            }

            List<ConfigInfoBo> bos = this.getConfigListByCatalogId(catalogBo.getId());
            if (bos != null && !bos.isEmpty()) {
                for (ConfigInfoBo temp : bos) {
                    List<String> valueInitList = new ArrayList<>();
                    List<String> finalValue = new ArrayList<>();
                    if (StringUtils.isEmpty(temp.getWidgetValue()) && !StringUtils.isEmpty(temp.getDefaultValue())) {
                        String defaultValue = temp.getDefaultValue();
                        String[] split = defaultValue.split(",");
                        valueInitList = new ArrayList<>(Arrays.asList(split));
                        if (1 == temp.getWidgetType() || 4 == temp.getWidgetType()) {
                            List<String> list = new ArrayList<>();
                            list.add(defaultValue);
                            valueInitList = list;
                        }
                       /* if (1 <= temp.getWidgetType() && temp.getWidgetType() <= 4) {
                            //根据configId查询option
                            List<ConfigOptionBo> optionBoList = this.getOptionListByConfigId(temp.getId());
                            for (String value : valueInitList) {
                                for (ConfigOptionBo configOptionBo : optionBoList) {
                                    if (value.equals(configOptionBo.getSelectValue())) {
                                        finalValue.add(configOptionBo.getLabel());
                                    }
                                }
                            }
                        } else {
                            finalValue = valueInitList;
                        }*/
                    } else if (!StringUtils.isEmpty(temp.getWidgetValue())) {
                        String value = temp.getWidgetValue();
                        String[] split = value.split(",");
                        valueInitList = new ArrayList<>(Arrays.asList(split));
                        if (1 == temp.getWidgetType() || 4 == temp.getWidgetType()) {
                            List<String> list = new ArrayList<>();
                            list.add(value);
                            valueInitList = list;
                        }
                       /* if (1 <= temp.getWidgetType() && temp.getWidgetType() <= 4) {
                            //根据configId查询option
                            List<ConfigOptionBo> optionBoList = this.getOptionListByConfigId(temp.getId());
                            for (String value1 : valueInitList) {
                                for (ConfigOptionBo configOptionBo : optionBoList) {
                                    if (value1.equals(configOptionBo.getSelectValue())) {
                                        finalValue.add(configOptionBo.getLabel());
                                    }
                                }
                            }
                        } else {
                            finalValue = valueInitList;
                        }*/
                    }
                    hashMap.put(temp.getCode(), valueInitList);
                }


                moduleMap.put(moduleKey, hashMap);

                //初始化版本库
                //根据租户模块key查询版本，有的话不进行操作，没有新增版本号
                String tidModuleKey = tenantId + KEY_SPLIT + moduleKey;
                ConfigVersionPO configVersionPO = configVersionService.getOne(
                        new QueryWrapper<ConfigVersionPO>().lambda().eq(ConfigVersionPO::getTidModuleKey, tidModuleKey));
                if (ObjectUtils.isEmpty(configVersionPO)) {
                    ConfigVersionPO configVersionPO1 = new ConfigVersionPO();
                    configVersionPO1.setConfigVersion(tidModuleKey + KEY_SPLIT + TimeHelper.getNowTime());
                    configVersionPO1.setTidModuleKey(tidModuleKey);
                    configVersionService.save(configVersionPO1);
                }
            }
        }

        //存入本地配置数据缓存
        configMap.put(tenantId, moduleMap);
    }


    /**
     * 处理多个目录下的配置数据
     *
     * @param catalogBos
     */
    private void handleListCatalogTask(List<CatalogBo> catalogBos) {
        String tenantId = this.getTenantId();

        ConcurrentHashMap<String, HashMap<String, Object>> moduleMap = configMap.get(tenantId);
        if (ObjectUtils.isEmpty(moduleMap)) {
            moduleMap = new ConcurrentHashMap<>();
        }
        for (CatalogBo catalogBo : catalogBos) {
//            List<ConfigInfoBo> bos = this.getConfigByAppCode(catalogBo.getAppCode());
            String moduleKey = catalogBo.getAppCode() + KEY_SPLIT + catalogBo.getCode();
            HashMap<String, Object> hashMap = moduleMap.get(moduleKey);
            if (ObjectUtils.isEmpty(hashMap)) {
                hashMap = new HashMap<>();
            }

            List<ConfigInfoBo> bos = this.getConfigListByCatalogId(catalogBo.getId());
            if (bos != null && !bos.isEmpty()) {
                for (ConfigInfoBo temp : bos) {
                    List<String> valueInitList = new ArrayList<>();
                    List<String> finalValue = new ArrayList<>();
                    if (StringUtils.isEmpty(temp.getWidgetValue()) && !StringUtils.isEmpty(temp.getDefaultValue())) {
                        String defaultValue = temp.getDefaultValue();
                        String[] split = defaultValue.split(",");
                        valueInitList = new ArrayList<>(Arrays.asList(split));
                        if (1 == temp.getWidgetType() || 4 == temp.getWidgetType()) {
                            List<String> list = new ArrayList<>();
                            list.add(defaultValue);
                            valueInitList = list;
                        }
                       /* if (1 <= temp.getWidgetType() && temp.getWidgetType() <= 4) {
                            //根据configId查询option
                            List<ConfigOptionBo> optionBoList = this.getOptionListByConfigId(temp.getId());
                            for (String value : valueInitList) {
                                for (ConfigOptionBo configOptionBo : optionBoList) {
                                    if (value.equals(configOptionBo.getSelectValue())) {
                                        finalValue.add(configOptionBo.getLabel());
                                    }
                                }
                            }
                        } else {
                            finalValue = valueInitList;
                        }*/
                    } else if (!StringUtils.isEmpty(temp.getWidgetValue())) {
                        String value = temp.getWidgetValue();
                        String[] split = value.split(",");
                        valueInitList = new ArrayList<>(Arrays.asList(split));
                        if (1 == temp.getWidgetType() || 4 == temp.getWidgetType()) {
                            List<String> list = new ArrayList<>();
                            list.add(value);
                            valueInitList = list;
                        }
                       /* if (1 <= temp.getWidgetType() && temp.getWidgetType() <= 4) {
                            //根据configId查询option
                            List<ConfigOptionBo> optionBoList = this.getOptionListByConfigId(temp.getId());
                            for (String value1 : valueInitList) {
                                for (ConfigOptionBo configOptionBo : optionBoList) {
                                    if (value1.equals(configOptionBo.getSelectValue())) {
                                        finalValue.add(configOptionBo.getLabel());
                                    }
                                }
                            }
                        } else {
                            finalValue = valueInitList;
                        }*/
                    }
                    hashMap.put(temp.getCode(), valueInitList);
                }

                moduleMap.put(moduleKey, hashMap);
            }
        }

        //存入本地配置数据缓存
        configMap.put(tenantId, moduleMap);
    }
}
