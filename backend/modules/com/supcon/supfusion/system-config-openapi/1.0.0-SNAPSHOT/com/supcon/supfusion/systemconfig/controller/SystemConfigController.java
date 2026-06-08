package com.supcon.supfusion.systemconfig.controller;

import com.alibaba.nacos.api.config.ConfigService;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.exception.BizErrorEnum;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.systemconfig.common.constants.Constants;
import com.supcon.supfusion.systemconfig.common.enums.CatalogTypeEnum;
import com.supcon.supfusion.systemconfig.common.enums.Element;
import com.supcon.supfusion.systemconfig.common.exception.SystemConfigErrorEnum;
import com.supcon.supfusion.systemconfig.common.exception.SystemConfigException;
import com.supcon.supfusion.systemconfig.common.util.Base64Util;
import com.supcon.supfusion.systemconfig.controller.util.XMLParse;
import com.supcon.supfusion.systemconfig.controller.vo.*;
import com.supcon.supfusion.systemconfig.service.SystemConfigService;
import com.supcon.supfusion.systemconfig.service.bo.CatalogBo;
import com.supcon.supfusion.systemconfig.service.bo.ConfigInfoBo;
import com.supcon.supfusion.systemconfig.service.bo.ConfigOptionBo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lifangyuan
 */
@Slf4j
@OpenApi(path = "/open-api/systemconfig/v1")
public class SystemConfigController {

    @Resource
    private SystemConfigService systemConfigService;

    @Resource
    private ConfigService configService;


    /**
     * 保存ocd xml文本
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/saveOcdContent")
    public CatalogsVO saveOcdContent(@Valid @RequestBody XmlContentVO xmlContentVO) throws Exception {
        List<CatalogVO> list = Lists.newArrayList();
        for (String xmlContent : xmlContentVO.getXmlList()) {
            String decodeXmlContent = Base64Util.base64Decode(xmlContent);
            CatalogVO catalogVO = XMLParse.getConfigVOByXml(decodeXmlContent);
            list.add(catalogVO);
        }
        CatalogsVO catalogsVO = new CatalogsVO();
        catalogsVO.setType(2);
        catalogsVO.setCatalogs(list);
        this.createOfXml(catalogsVO);
        return catalogsVO;
    }


    Result<Boolean> createOfXml(@Valid @RequestBody CatalogsVO catalogs) {
        Integer type = catalogs.getType();
        CatalogTypeEnum catalogType = CatalogTypeEnum.getCatalogType(type);
        CatalogBo one = systemConfigService.selectByCode(catalogType.getName());
        List<CatalogBo> catalogBos = new ArrayList<>();
        List<ConfigInfoBo> configInfoBos = new ArrayList<>();
        List<ConfigOptionBo> configOptionBos = new ArrayList<>();
        List<CatalogVO> catalog1 = catalogs.getCatalogs();
        for (CatalogVO temp : catalog1) {
            checkCatalog(temp.getAppCode(), SystemConfigErrorEnum.APP_CODE_NOT_INSERT);
            CatalogBo bo = null;
            CatalogBo tempOne = systemConfigService.selectCatalogOne(temp.getAppCode(), temp.getCode());
            List<ConfigVO> configVos = temp.getConfig();
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
                for (ConfigVO configVo : configVos) {
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
                    ConfigVO.TypeConfig typeConfig = configVo.getTypeConfig();
                    if (typeConfig != null) {
                        processTypeConfig(configOptionBos, configInfo, typeConfig);
                    }
                    if (!StringUtils.isEmpty(configVo.getExtend())) {
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

    /**
     * 注册配置
     *
     * @param catalogs
     * @return
     */
    @PostMapping("/config/catalog")
    Result<Boolean> create(@Valid @RequestBody CatalogsVO catalogs) {
        Integer type = catalogs.getType();
        CatalogTypeEnum catalogType = CatalogTypeEnum.getCatalogType(type);
        CatalogBo one = systemConfigService.selectByCode(catalogType.getName());
        List<CatalogBo> catalogBos = new ArrayList<>();
        List<ConfigInfoBo> configInfoBos = new ArrayList<>();
        List<ConfigOptionBo> configOptionBos = new ArrayList<>();
        List<CatalogVO> catalog1 = catalogs.getCatalogs();
        for (CatalogVO temp : catalog1) {
            checkCatalog(temp.getAppCode(), SystemConfigErrorEnum.APP_CODE_NOT_INSERT);
            CatalogBo bo = null;
            CatalogBo tempOne = systemConfigService.selectCatalogOne(temp.getAppCode(), temp.getCode());
            List<ConfigVO> configVos = temp.getConfig();
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
                for (ConfigVO configVo : configVos) {
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
                    ConfigVO.TypeConfig typeConfig = configVo.getTypeConfig();
                    if (typeConfig != null) {
                        processTypeConfig(configOptionBos, configInfo, typeConfig);
                    }
                    if (!StringUtils.isEmpty(configVo.getExtend())) {
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
        systemConfigService.setConfigInfoCache( catalogBos);
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), true);
    }

    private void processTypeConfig(List<ConfigOptionBo> configOptionBos, ConfigInfoBo configInfo, ConfigVO.TypeConfig typeConfig) {
        if (configInfo.getWidgetType().compareTo(Element.SELECT.getType()) == 0 && typeConfig.getIsMore() != null && typeConfig.getIsMore()) {
            configInfo.setWidgetType(Element.SELECT.getType() + 1);
        }
        if (configInfo.getWidgetType().compareTo(Element.TIME.getType()) == 0 && !Strings.isNullOrEmpty(typeConfig.getTimeFormat())) {
            configInfo.setCustom(typeConfig.getTimeFormat());
            configInfo.setWidgetType(Element.TIME.getType() + 1);
        }
        if (!StringUtils.isEmpty(typeConfig.getRemind())) {
            configInfo.setDescription(typeConfig.getRemind());
        }
        List<ConfigVO.TypeConfig.OptionalValue> optionalValues = typeConfig.getOptionalValue();
        Set<String> set = new HashSet<>();
        if (optionalValues != null) {
            for (ConfigVO.TypeConfig.OptionalValue optionalValue : optionalValues) {
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
    @DeleteMapping("/config/catalog/{appCode}")
    Result<Boolean> destroy(@PathVariable("appCode") String appCode) {
        checkCatalog(appCode, SystemConfigErrorEnum.APP_CODE_NOT_DELETE);
        systemConfigService.deleteBatchIds(appCode);
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), true);
    }

    /**
     * 删除配置
     *
     * @param appCode
     */
    @DeleteMapping("/config/catalog/{appCode}/{code}")
    Result<Boolean> deleteSystemConfig(@PathVariable("appCode") String appCode, @PathVariable("code") String code) {
        checkCatalog(appCode, SystemConfigErrorEnum.APP_CODE_NOT_DELETE);
        systemConfigService.deleteBatchIds(appCode, code);
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), true);
    }

    @GetMapping("/config/catalog/{appCode}/{code}")
    Result<ConfigsVO> getConfigByAppCode(@PathVariable("appCode") String appCode, @PathVariable("code") String code) {
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
        List<ConfigVO> collect = bos.stream().map(configInfoBo -> {
            ConfigVO configVo = new ConfigVO();
            configVo.setCode(configInfoBo.getCode());
            configVo.setName(configInfoBo.getName());
            configVo.setConfigId(configInfoBo.getId());
            if (StringUtils.isEmpty(configInfoBo.getWidgetValue()) && StringUtils.isNotEmpty(configInfoBo.getDefaultValue())) {
                String defaultValue = configInfoBo.getDefaultValue();
                String[] split = defaultValue.split(",");
                ArrayList<String> strings = new ArrayList<>(Arrays.asList(split));
                configVo.setValue(strings);
            } else if (StringUtils.isNotEmpty(configInfoBo.getWidgetValue())) {
                String value = configInfoBo.getWidgetValue();
                String[] split = value.split(",");
                ArrayList<String> strings = new ArrayList<>(Arrays.asList(split));
                configVo.setValue(strings);
            } else {
                configVo.setValue(new ArrayList<>());
            }
            return configVo;
        }).collect(Collectors.toList());
        ConfigsVO configsVo = new ConfigsVO();
        configsVo.setConfig(collect);
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), configsVo);
    }


    private ConfigOptionBo getConfigOption(ConfigInfoBo configInfoBo, ConfigVO.TypeConfig.OptionalValue optionalValue) {
        return ConfigOptionBo.builder()
                .configId(configInfoBo.getId())
                .id(IDGenerator.newInstance().generate().longValue())
                .sort(optionalValue.getOrder())
                .label(optionalValue.getLabel())
                .selectValue(optionalValue.getValue())
                .build();
    }

    private void addVerify(ConfigVO configVo, ConfigInfoBo configInfoBo) {
        List<ConfigVO.Verify> verify = configVo.getVerify();
        if (verify != null && !verify.isEmpty()) {
            for (ConfigVO.Verify temp : verify) {
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

    private ConfigInfoBo getConfigInfo(CatalogBo catalogBo, ConfigVO configVo, List<String> defaultValue) {
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
                .id(IDGenerator.newInstance().generate().longValue())
                .moduleCode(catalogBo.getCode())
                .sort(configVo.getOrder())
                .defaultValue(str.toString())
                .catalogId(catalogBo.getId())
                .name(configVo.getName())
                .widgetType(configVo.getType())
                .build();
    }

    private CatalogBo getConfigCatalog(CatalogVO catalog, Integer type) {
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

}
