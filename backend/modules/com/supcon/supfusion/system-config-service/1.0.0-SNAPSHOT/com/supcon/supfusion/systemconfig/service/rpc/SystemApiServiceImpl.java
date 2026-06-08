package com.supcon.supfusion.systemconfig.service.rpc;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.exception.BizErrorEnum;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.systemconfig.api.SystemApiService;
import com.supcon.supfusion.systemconfig.api.dto.*;
import com.supcon.supfusion.systemconfig.common.constants.Constants;
import com.supcon.supfusion.systemconfig.common.enums.CatalogTypeEnum;
import com.supcon.supfusion.systemconfig.common.enums.Element;
import com.supcon.supfusion.systemconfig.common.exception.SystemConfigErrorEnum;
import com.supcon.supfusion.systemconfig.common.exception.SystemConfigException;
import com.supcon.supfusion.systemconfig.common.util.Base64Util;
import com.supcon.supfusion.systemconfig.service.SystemConfigService;
import com.supcon.supfusion.systemconfig.service.bo.CatalogBo;
import com.supcon.supfusion.systemconfig.service.bo.ConfigInfoBo;
import com.supcon.supfusion.systemconfig.service.bo.ConfigOptionBo;
import com.supcon.supfusion.systemconfig.service.utils.XMLParseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;

import javax.validation.Valid;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author lifangyuan
 */
@Slf4j
@ServiceApiService
public class SystemApiServiceImpl extends BaseController implements SystemApiService {


    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 注册配置
     *
     * @param catalogs
     * @return
     */
    @Override
    public Result<Boolean> create(CatalogsDTO catalogs) {
        Integer type = catalogs.getType();
        CatalogTypeEnum catalogType = CatalogTypeEnum.getCatalogType(type);
        CatalogBo one = systemConfigService.selectByCode(catalogType.getName());
        List<CatalogBo> catalogBos = new ArrayList<>();
        List<ConfigInfoBo> configInfoBos = new ArrayList<>();
        List<ConfigOptionBo> configOptionBos = new ArrayList<>();
        List<CatalogDTO> catalog1 = catalogs.getCatalogs();
        for (CatalogDTO temp : catalog1) {
            checkCatalog(temp.getAppCode(), SystemConfigErrorEnum.APP_CODE_NOT_INSERT);
            CatalogBo bo = null;
            CatalogBo tempOne = systemConfigService.selectCatalogOne(temp.getAppCode(), temp.getCode());
            List<ConfigDTO> configVos = temp.getConfig();
            if (tempOne != null) {
                if (configVos == null || configVos.isEmpty()) {
                    throw new SystemConfigException(SystemConfigErrorEnum.APP_CODE_EXIST);
                }
                bo = tempOne;
            } else {
                bo = getConfigCatalog(temp, type);
                bo.setParentId(one.getId());
                catalogBos.add(bo);
            }
            if (configVos != null && !configVos.isEmpty()) {
                for (ConfigDTO configVo : configVos) {
                    ConfigInfoBo configInfo = null;
                    ConfigInfoBo bo1 = systemConfigService.selectConfigInfoOne(temp.getAppCode(), configVo.getCode());
                    if (bo1 != null) {
                        throw new SystemConfigException(SystemConfigErrorEnum.CATALOG_IS_EXIST);
                    } else {
                        List<String> defaultValue = configVo.getDefaultValue();
                        configInfo = getConfigInfo(bo, configVo, defaultValue);
                    }
                    //添加校验规则
                    addVerify(configVo, configInfo);
                    //新增配置配置项
                    ConfigDTO.TypeConfig typeConfig = configVo.getTypeConfig();
                    if (typeConfig != null) {
                        processTypeConfig(configOptionBos, configInfo, typeConfig);
                    }
                    if (!org.apache.commons.lang3.StringUtils.isEmpty(configVo.getExtend())) {
                        configInfo.setWidgetType(Element.CUSTOM.getType());
                        configInfo.setCustom(configVo.getExtend());
                    }
                    if (configInfo.getWidgetType().compareTo(Element.LONG_TEXT.getType()) == 0) {
                        configInfo.setWidgetType(Element.LONG_TEXT.getType() + 1);
                    }
                    configInfoBos.add(configInfo);
                }
            }
        }
        systemConfigService.batchInsertConfig(catalogBos, configInfoBos, configOptionBos);
        systemConfigService.setConfigInfoCache(catalogBos);
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), true);
    }

    private void processTypeConfig(List<ConfigOptionBo> configOptionBos, ConfigInfoBo configInfo, ConfigDTO.TypeConfig typeConfig) {
        if (configInfo.getWidgetType().compareTo(Element.SELECT.getType()) == 0 && typeConfig.getIsMore() != null && typeConfig.getIsMore()) {
            configInfo.setWidgetType(Element.SELECT.getType() + 1);
        }
        if (configInfo.getWidgetType().compareTo(Element.TIME.getType()) == 0 && !Strings.isNullOrEmpty(typeConfig.getTimeFormat())) {
            configInfo.setCustom(typeConfig.getTimeFormat());
            configInfo.setWidgetType(Element.TIME.getType() + 1);
        }
        if (!org.apache.commons.lang3.StringUtils.isEmpty(typeConfig.getRemind())) {
            configInfo.setDescription(typeConfig.getRemind());
        }
        List<ConfigDTO.TypeConfig.OptionalValue> optionalValues = typeConfig.getOptionalValue();
        Set<String> set = new HashSet<>();
        if (optionalValues != null) {
            for (ConfigDTO.TypeConfig.OptionalValue optionalValue : optionalValues) {
                boolean add = set.add(optionalValue.getValue());
                if (!add) {
                    throw new SystemConfigException(SystemConfigErrorEnum.OPTION_VALUE_REPEAT);
                }
                ConfigOptionBo configOption = getConfigOption(configInfo, optionalValue);
                //新增下拉选项
                configOptionBos.add(configOption);
            }
        }
    }


    private void checkCatalog(String appCode, SystemConfigErrorEnum appCodeNotInsert) {
        if (Constants.APP.equalsIgnoreCase(appCode) || Constants.SYSTEM.equalsIgnoreCase(appCode)) {
            throw new SystemConfigException(appCodeNotInsert);
        }
    }


    /**
     * 删除配置
     *
     * @param appCode
     */
    @Override
    public Result<Boolean> destroy(String appCode) {
        checkCatalog(appCode, SystemConfigErrorEnum.APP_CODE_NOT_DELETE);
        systemConfigService.deleteBatchIds(appCode);
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), true);
    }

    /**
     * 删除配置
     *
     * @param appCode
     */
    Result<Boolean> deleteSystemConfig(String appCode, String code) {
        checkCatalog(appCode, SystemConfigErrorEnum.APP_CODE_NOT_DELETE);
        systemConfigService.deleteBatchIds(appCode, code);
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), true);
    }

    @Override
    public Result<ConfigsDTO> getConfigByAppCode(String appCode, String code) {
        checkCatalog(appCode, SystemConfigErrorEnum.APP_CODE_NOT_SUPPORT);
        CatalogBo catalogBo = systemConfigService.selectCatalogOne(appCode, code);
        if (ObjectUtils.isEmpty(catalogBo)) {
            return new Result<>(BizErrorEnum.SYSTEM_DATA_NULL.getCode(), BizErrorEnum.SYSTEM_DATA_NULL.getMessage(), null);
        }
        ConfigInfoBo temp = new ConfigInfoBo();
        temp.setCatalogId(catalogBo.getId());
        List<ConfigInfoBo> bos = systemConfigService.selectConfigInfo(temp);
        if (bos.isEmpty()) {
            throw new SystemConfigException(SystemConfigErrorEnum.CATALOG_ISNOT_EXIST);
        }
        List<ConfigDTO> collect = bos.stream().map(configInfoBo -> {
            ConfigDTO configVo = new ConfigDTO();
            configVo.setCode(configInfoBo.getCode());
            configVo.setName(configInfoBo.getName());
            configVo.setConfigId(configInfoBo.getId());
            if (org.apache.commons.lang3.StringUtils.isEmpty(configInfoBo.getWidgetValue()) && org.apache.commons.lang3.StringUtils.isNotEmpty(configInfoBo.getDefaultValue())) {
                String defaultValue = configInfoBo.getDefaultValue();
                String[] split = defaultValue.split(",");
                ArrayList<String> strings = new ArrayList<>(Arrays.asList(split));
                configVo.setValue(strings);
            } else if (org.apache.commons.lang3.StringUtils.isNotEmpty(configInfoBo.getWidgetValue())) {
                String value = configInfoBo.getWidgetValue();
                String[] split = value.split(",");
                ArrayList<String> strings = new ArrayList<>(Arrays.asList(split));
                configVo.setValue(strings);
            } else {
                configVo.setValue(new ArrayList<>());
            }
            return configVo;
        }).collect(Collectors.toList());
        ConfigsDTO configsVo = new ConfigsDTO();
        configsVo.setConfig(collect);
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), configsVo);
    }

    @Override
    public ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>> getConfigInfoForFramework() {
        return systemConfigService.getConfigInfoForFramework();
    }

    @Override
    public ConcurrentHashMap<String, String> getConfigVersionForFramework() {
        return systemConfigService.getConfigVersionForFramework();
    }

    @Override
    public ConfigAndVersionDTO getConfigByVersionForFramework(ConcurrentHashMap<String, String> versionMapOfFramework) {
        return systemConfigService.getConfigByVersionForFramework(versionMapOfFramework);
    }

    @Override
    public Result<Boolean> saveOcdContent(XmlContentDTO xmlContentDTO) throws Exception {
        log.info("保存ocd文件,{}", JSON.toJSONString(xmlContentDTO));
        List<CatalogDTO> list = Lists.newArrayList();
        for (String xmlContent : xmlContentDTO.getXmlList()) {
            String decodeXmlContent = Base64Util.base64Decode(xmlContent);
            CatalogDTO catalogDTO = XMLParseService.getConfigVOByXml(decodeXmlContent);
            if (!ObjectUtils.isEmpty(catalogDTO)) {
                list.add(catalogDTO);
            }
        }
        if (ObjectUtils.isEmpty(list)) {
            throw new SystemConfigException(SystemConfigErrorEnum.MODULE_CODE_NOT_EXIST);
        }
        CatalogsDTO catalogsDTO = new CatalogsDTO();
        catalogsDTO.setType(2);
        catalogsDTO.setCatalogs(list);
        return this.createOfXml(catalogsDTO);
    }

    Result<Boolean> createOfXml(CatalogsDTO catalogs) {
        Integer type = catalogs.getType();
        CatalogTypeEnum catalogType = CatalogTypeEnum.getCatalogType(type);
        CatalogBo one = systemConfigService.selectByCode(catalogType.getName());
        List<CatalogBo> catalogBos = new ArrayList<>();
        List<ConfigInfoBo> configInfoBos = new ArrayList<>();
        List<ConfigOptionBo> configOptionBos = new ArrayList<>();
        List<CatalogDTO> catalog1 = catalogs.getCatalogs();
        for (CatalogDTO temp : catalog1) {
            checkCatalog(temp.getAppCode(), SystemConfigErrorEnum.APP_CODE_NOT_INSERT);
            CatalogBo bo = null;
            CatalogBo tempOne = systemConfigService.selectCatalogOne(temp.getAppCode(), temp.getCode());
            List<ConfigDTO> configVos = temp.getConfig();
            if (tempOne != null) {
                if (configVos == null || configVos.isEmpty()) {
                    break;
                }
                bo = tempOne;
            } else {
                bo = getConfigCatalog(temp, type);
                bo.setParentId(one.getId());
                catalogBos.add(bo);
            }
            if (configVos != null && !configVos.isEmpty()) {
                for (ConfigDTO configVo : configVos) {
                    ConfigInfoBo configInfo = null;
                    ConfigInfoBo bo1 = systemConfigService.selectConfigInfoOne(temp.getAppCode(), configVo.getCode());
                    if (bo1 != null) {
                        break;
                    } else {
                        List<String> defaultValue = configVo.getDefaultValue();
                        configInfo = getConfigInfo(bo, configVo, defaultValue);
                    }
                    //添加校验规则
                    addVerify(configVo, configInfo);
                    //新增配置配置项
                    ConfigDTO.TypeConfig typeConfig = configVo.getTypeConfig();
                    if (typeConfig != null) {
                        processTypeConfig(configOptionBos, configInfo, typeConfig);
                    }
                    if (!org.apache.commons.lang3.StringUtils.isEmpty(configVo.getExtend())) {
                        configInfo.setWidgetType(Element.CUSTOM.getType());
                        configInfo.setCustom(configVo.getExtend());
                    }
                    if (configInfo.getWidgetType().compareTo(Element.LONG_TEXT.getType()) == 0) {
                        configInfo.setWidgetType(Element.LONG_TEXT.getType() + 1);
                    }
                    configInfoBos.add(configInfo);
                }
            }
        }
        systemConfigService.batchInsertConfig(catalogBos, configInfoBos, configOptionBos);
        systemConfigService.setConfigInfoCache(catalogBos);
        systemConfigService.refreshConfigCacheCommon();
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), true);
    }


    private ConfigOptionBo getConfigOption(ConfigInfoBo configInfoBo, ConfigDTO.TypeConfig.OptionalValue optionalValue) {
        return ConfigOptionBo.builder()
                .configId(configInfoBo.getId())
                .id(IDGenerator.newInstance().generate().longValue())
                .sort(optionalValue.getOrder())
                .label(optionalValue.getLabel())
                .selectValue(optionalValue.getValue())
                .build();
    }

    private void addVerify(ConfigDTO configVo, ConfigInfoBo configInfoBo) {
        List<ConfigDTO.Verify> verify = configVo.getVerify();
        if (verify != null && !verify.isEmpty()) {
            for (ConfigDTO.Verify temp : verify) {
                if (temp.getLength() != null) {
                    configInfoBo.setMaxValue(temp.getLength());
                }
                if (temp.getMax() != null) {
                    configInfoBo.setMaxValue(temp.getMax());
                }
                if (temp.getMin() != null) {
                    configInfoBo.setMinValue(temp.getMin());
                }
                if (!StringUtils.isEmpty(temp.getRex())) {
                    configInfoBo.setRegFormat(temp.getRex());
                }
                if (temp.getIsRequire() != null) {
                    configInfoBo.setIsRequire(temp.getIsRequire());
                }
                if (temp.getIsNumber() != null && temp.getIsNumber()) {
                    configInfoBo.setCustom("number");
                }
            }
        }

    }

    private ConfigInfoBo getConfigInfo(CatalogBo catalogBo, ConfigDTO configVo, List<String> defaultValue) {
        StringBuilder str = new StringBuilder();
        if (defaultValue != null) {
            for (int i = 0; i < defaultValue.size(); i++) {
                if (i == defaultValue.size() - 1) {
                    str.append(defaultValue.get(i));
                } else {
                    str.append(defaultValue.get(i)).append(",");
                }
                i++;
            }
        }
        return ConfigInfoBo.builder()
                .code(configVo.getCode())
                .appCode(catalogBo.getAppCode())
                .moduleCode(catalogBo.getCode())
                .id(IDGenerator.newInstance().generate().longValue())
                .sort(configVo.getOrder())
                .defaultValue(str.toString())
                .catalogId(catalogBo.getId())
                .name(configVo.getName())
                .widgetType(configVo.getType()).build();
    }

    private CatalogBo getConfigCatalog(CatalogDTO catalog, Integer type) {
        return CatalogBo.builder()
                .appCode(catalog.getAppCode())
                .id(IDGenerator.newInstance().generate().longValue())
                .code(catalog.getCode())
                .sort(catalog.getOrder())
                .isHide(catalog.getHide())
                .name(catalog.getName())
                .catalogType(type)
                .build();
    }

    @Override
    public Result<Boolean> updateConfig(@Valid UpdateConfigDTO updateConfigDTO) {
        for (UpdateConfigDTO.ConfigInfoDTO configInfoDTO : updateConfigDTO.getConfigInfoDTO()) {
            List<String> values = configInfoDTO.getValue();
            StringBuilder builder = new StringBuilder();
            int i = 0;
            if (values != null && !values.isEmpty()) {
                for (String value : values) {
                    i++;
                    if (i == values.size()) {
                        builder.append(value);
                    } else {
                        builder.append(value).append(",");
                    }
                }
            } else {
                continue;
            }
            ConfigInfoBo bo = ConfigInfoBo.builder()
                    .appCode(updateConfigDTO.getModuleCode())
                    .moduleCode(updateConfigDTO.getModuleCode())
                    .code(configInfoDTO.getKey())
                    .widgetValue(builder.toString())
                    .build();
            systemConfigService.updateConfigInfoByAppCodeAndModuleCode(bo);
        }
        ConfigInfoBo bo = ConfigInfoBo.builder()
                .appCode(updateConfigDTO.getModuleCode())
                .moduleCode(updateConfigDTO.getModuleCode())
                .code(updateConfigDTO.getConfigInfoDTO().get(0).getKey())
                .build();
        ConfigInfoBo configInfoBoFormDB = systemConfigService.selectConfigInfoByModuleCodeAndAppCodeAndKey(bo);
        ConfigInfoBo configInfoBo = ConfigInfoBo.builder().id(configInfoBoFormDB.getId())
                .catalogId(configInfoBoFormDB.getCatalogId()).build();
        systemConfigService.updateConfigInfo(configInfoBo);
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), true);
    }
}
