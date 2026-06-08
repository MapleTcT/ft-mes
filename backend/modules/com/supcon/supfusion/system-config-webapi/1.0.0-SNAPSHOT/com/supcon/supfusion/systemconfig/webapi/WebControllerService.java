package com.supcon.supfusion.systemconfig.webapi;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.exception.BizErrorEnum;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.systemconfig.common.constants.Constants;
import com.supcon.supfusion.systemconfig.common.enums.CatalogTypeEnum;
import com.supcon.supfusion.systemconfig.common.enums.Element;
import com.supcon.supfusion.systemconfig.common.exception.SystemConfigErrorEnum;
import com.supcon.supfusion.systemconfig.common.exception.SystemConfigException;
import com.supcon.supfusion.systemconfig.service.SystemConfigService;
import com.supcon.supfusion.systemconfig.service.bo.CatalogBo;
import com.supcon.supfusion.systemconfig.service.bo.ConfigInfoBo;
import com.supcon.supfusion.systemconfig.service.bo.ConfigOptionBo;
import com.supcon.supfusion.systemconfig.webapi.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author lifangyuan
 */
@InternalApi(path = "/inter-api/systemconfig/v1")
@Slf4j
public class WebControllerService {

    @Resource
    private ConfigService configService;

    @Resource
    private SystemConfigService systemConfigService;


    @Autowired
    private MessageResourceWrapper messageResourceWrapper;

    /**
     * 获取配置分类
     *
     * @param
     * @return
     */
    @GetMapping("/config/catalog")
    Result<CatalogNamesVO> getCatalog(@RequestParam(value = "keyword", required = false) String keyword, @RequestParam(value = "type", required = false) Integer type) {
        List<CatalogNameVO> catalogs = new ArrayList<>();
        List<CatalogBo> catalogBos = systemConfigService.selectParentCatalog();
        for (CatalogBo catalogBo : catalogBos) {
            List<CatalogBo> result = systemConfigService.selectCatalog(catalogBo);
            List<CatalogNameVO> catalogNameVos = new ArrayList<>();
            for (CatalogBo temp : result) {
                String message = messageResourceWrapper.getMessageNotBlank(temp.getName());
                if (StringUtils.isEmpty(keyword) || (StringUtils.isNotEmpty(keyword) && message.contains(keyword))) {
                    CatalogNameVO catalogNameVo = CatalogNameVO.builder()
                            .name(message)
                            .catalogId(String.valueOf(temp.getId()))
                            .build();
                    if ("systemConfig.userAD".equals(temp.getName())) {
                        catalogNameVo.setModuleCode("auth.userAD");
                    }
                    if("systemConfig.passwordConfig".equals(temp.getName())){
                        catalogNameVo.setModuleCode("auth.passwordConfig");
                    }
                    if ("systemConfig.AKSK".equals(temp.getName())) {
                        catalogNameVo.setModuleCode("systemConfig.certificateMgr");
                    }
                    if ("systemConfig.baseImages".equals(temp.getName())) {
                        catalogNameVo.setModuleCode("systemConfig.mirrorMgr");
                    }
                    catalogNameVos.add(catalogNameVo);
                }
            }
            if (type == null || type == 2) {
                CatalogNameVO catalog = CatalogNameVO.builder()
                        .name(messageResourceWrapper.getMessageNotBlank(catalogBo.getName()))
                        .catalogId(String.valueOf(catalogBo.getId()))
                        .catalog(catalogNameVos).build();
                catalogs.add(catalog);
            } else {
                catalogs.addAll(catalogNameVos);
            }
        }
        return new Result<>(new CatalogNamesVO(catalogs));
    }

    /**
     * 获取配置项
     *
     * @param catalogId
     * @return
     */
    @GetMapping("/config/catalog/{catalogId}")
    Result<ConfigsVO> getConfig(@PathVariable("catalogId") Long catalogId) {
        ConfigInfoBo configInfoBo = new ConfigInfoBo();
        configInfoBo.setCatalogId(catalogId);
        List<ConfigInfoBo> configInfoBos = systemConfigService.selectConfigInfo(configInfoBo);
        List<ConfigVO> configVos = configInfoBos.stream().map(this::buildConfigVo).collect(Collectors.toList());
        return new Result<>(new ConfigsVO(configVos));
    }

    /**
     * 获取配置父类
     *
     * @param catalogId
     * @return
     */
    @GetMapping("/config/catalog/parent/{catalogId}")
    Result<CatalogParentVO> getParentCatalog(@PathVariable("catalogId") Long catalogId) {
        CatalogBo catalogBo = systemConfigService.selectCatalogById(catalogId);
        CatalogBo catalogBo1 = systemConfigService.selectCatalogById(catalogBo.getParentId());
        CatalogParentVO result = CatalogParentVO.builder()
                .parentCode(catalogBo1.getCode())
                .parentName(messageResourceWrapper.getMessageNotBlank(catalogBo1.getName()))
                .parentId(String.valueOf(catalogBo1.getId()))
                .id(String.valueOf(catalogBo.getId()))
                .name(messageResourceWrapper.getMessageNotBlank(catalogBo.getName()))
                .code(catalogBo.getCode())
                .build();
        if ("systemConfig.userAD".equals(catalogBo.getName())) {
            result.setModuleCode("auth.userAD");
        }
        return new Result<>(result);
    }

    /**
     * 获取配置项
     *
     * @return
     */
    @GetMapping("/config/catalog/{appCode}/{code}")
    Result<ConfigsVO> getConfig(@PathVariable("appCode") String appCode, @PathVariable("code") String code) {
        CatalogBo catalogBo = systemConfigService.selectCatalogOne(appCode, code);
        ConfigInfoBo configInfoBo = new ConfigInfoBo();
        configInfoBo.setCatalogId(catalogBo.getId());
        List<ConfigInfoBo> configInfoBos = systemConfigService.selectConfigInfo(configInfoBo);
        List<ConfigVO> configVos = configInfoBos.stream().map(this::buildConfigVo).collect(Collectors.toList());
        return new Result<>(new ConfigsVO(configVos));
    }

    /**
     * 根据模块code和key获取配置项
     *
     * @return
     */
    @GetMapping("/config/catalog/by/module")
    Result<Map<String, String>> getConfigByModuleCodeAndKey(@RequestParam String moduleCode,
                                                            @RequestParam String key) {
        CatalogBo catalogBo = systemConfigService.selectByCode(moduleCode);
        if (null == catalogBo) {
            throw new SystemConfigException(SystemConfigErrorEnum.APP_CODE_NOT_EXIST);
        }
        Map<String, String> configMap = new HashMap<>();
        if (key.contains(",")) {
            String[] split = key.split(",");
            for (String singleKey : split) {
                this.getConfigInfoBySingleKey(catalogBo, configMap, singleKey);
            }
        } else {
            this.getConfigInfoBySingleKey(catalogBo, configMap, key);
        }

        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(),BizErrorEnum.SYSTEM_OK.getMessage(),configMap);
    }

    private void getConfigInfoBySingleKey(CatalogBo catalogBo, Map<String, String> configMap, String singleKey) {
        ConfigInfoBo configInfoBo = new ConfigInfoBo();
        configInfoBo.setCatalogId(catalogBo.getId());
        configInfoBo.setCode(singleKey);
        List<ConfigInfoBo> configInfoBoList = systemConfigService.selectByCatalogIdAndKey(configInfoBo);
        for (ConfigInfoBo temp : configInfoBoList) {
            String value = "";
            if (StringUtils.isEmpty(temp.getWidgetValue()) && !StringUtils.isEmpty(temp.getDefaultValue())) {
                value = temp.getDefaultValue();
            } else if (!StringUtils.isEmpty(temp.getWidgetValue())) {
                value = temp.getWidgetValue();
            }
            configMap.put(singleKey, value);
        }
    }


    private ConfigVO buildConfigVo(ConfigInfoBo temp) {
        Integer type = temp.getWidgetType();
        ConfigVO configVo = ConfigVO.builder()
                .code(temp.getCode())
                .name(org.apache.commons.lang3.StringUtils.isNotEmpty(temp.getName()) ? messageResourceWrapper.getMessageNotBlank(temp.getName()) : null)
                .type(type)
                .configId(String.valueOf(temp.getId()))
                .build();
        if (org.apache.commons.lang3.StringUtils.isEmpty(temp.getWidgetValue()) && org.apache.commons.lang3.StringUtils.isNotEmpty(temp.getDefaultValue())) {
            String defaultValue = temp.getDefaultValue();
            List<String> strings = new ArrayList<>();
            if (7 == type) {
                strings.add(defaultValue);
            } else {
                String[] split = defaultValue.split(",");
                strings = Arrays.asList(split);
            }
            configVo.setValue(strings);

        } else if (org.apache.commons.lang3.StringUtils.isNotEmpty(temp.getWidgetValue())) {
            String value = temp.getWidgetValue();
            List<String> strings = new ArrayList<>();
            if (7 == type) {
                strings.add(value);
            } else {
                String[] split = value.split(",");
                strings = Arrays.asList(split);
            }
            configVo.setValue(strings);
        } else {
            configVo.setValue(new ArrayList<>());
        }


        ConfigVO.TypeConfig typeConfig = new ConfigVO.TypeConfig();
        List<ConfigVO.TypeConfig.OptionalValue> optionalValues = new ArrayList<>();

        ConfigOptionBo bo = ConfigOptionBo.builder().configId(temp.getId()).build();
        List<ConfigOptionBo> configOptionBos = systemConfigService.selectConfigOption(bo);

        for (ConfigOptionBo configOptionBo : configOptionBos) {
            ConfigVO.TypeConfig.OptionalValue optionalValue = new ConfigVO.TypeConfig.OptionalValue();
            optionalValue.setLabel(configOptionBo.getLabel());
            optionalValue.setValue(configOptionBo.getSelectValue());
            optionalValues.add(optionalValue);
        }
        processType(temp, type, configVo, typeConfig, optionalValues);
        configVo.setTypeConfig(typeConfig);
        ArrayList<ConfigVO.Verify> verifies = new ArrayList<>();
        processVerify(temp, verifies);
        configVo.setVerify(verifies);
        return configVo;
    }

    private void processVerify(ConfigInfoBo temp, ArrayList<ConfigVO.Verify> verifies) {
        if (temp.getMaxValue() != null) {
            ConfigVO.Verify verify = new ConfigVO.Verify();
            if (temp.getWidgetType().equals(Element.INPUT.getType()) || temp.getWidgetType().equals(Element.LONG_TEXT.getType() + 1)) {
                verify.setMax(temp.getMaxValue());
                String message = messageResourceWrapper.getMessageNotBlank("systemConfig.length");
                verify.setMsg(message);
            } else {
                String message = messageResourceWrapper.getMessageNotBlank("systemConfig.max");
                verify.setMax(temp.getMaxValue());
                verify.setMsg(message);

            }
            verifies.add(verify);
        }
        if (temp.getMinValue() != null) {
            ConfigVO.Verify verify = new ConfigVO.Verify();
            verify.setMin(temp.getMinValue());
            String message = messageResourceWrapper.getMessageNotBlank("systemConfig.min");
            verify.setMsg(message);
            verifies.add(verify);
        }
        if (temp.getIsRequire() != null && temp.getIsRequire()) {
            ConfigVO.Verify verify = new ConfigVO.Verify();
            verify.setIsRequire(temp.getIsRequire());
            String message = messageResourceWrapper.getMessageNotBlank("systemConfig.require");
            verify.setMsg(message);


            verifies.add(verify);
        }
        if (!StringUtils.isEmpty(temp.getCustom()) && Constants.NUMBER.equals(temp.getCustom())) {
            ConfigVO.Verify verify = new ConfigVO.Verify();
            verify.setIsNumber(true);

            String message = messageResourceWrapper.getMessageNotBlank("systemConfig.number");
            verify.setMsg(message);

            verifies.add(verify);
        }
        if (temp.getRegFormat() != null) {
            ConfigVO.Verify verify = new ConfigVO.Verify();
            verify.setRex(temp.getRegFormat());
            verify.setMsg(temp.getRegMessage());
            verifies.add(verify);

        }
    }

    private void processType(ConfigInfoBo temp, Integer type, ConfigVO configVo, ConfigVO.TypeConfig typeConfig, List<ConfigVO.TypeConfig.OptionalValue> optionalValues) {
        if (type.equals(Element.CHECKBOX.getType()) || type.equals(Element.RADIO.getType())) {
            typeConfig.setOptionalValue(optionalValues);
        } else if (type.equals(Element.SELECT.getType())) {
            typeConfig.setIsMore(Boolean.FALSE);
            typeConfig.setOptionalValue(optionalValues);
        } else if (type.equals(Element.SELECT.getType() + 1)) {
            typeConfig.setIsMore(Boolean.TRUE);
            typeConfig.setOptionalValue(optionalValues);
            configVo.setType(Element.SELECT.getType());
        } else if (type.equals(Element.TIME.getType() + 1)) {
            typeConfig.setTimeFormat(temp.getCustom());
            configVo.setType(Element.TIME.getType());
        } else if (type.equals(Element.CUSTOM.getType())) {
            configVo.setType(Element.CUSTOM.getType());
            configVo.setExtend(temp.getCustom());
        } else if (type.equals(Element.LONG_TEXT.getType() + 1)) {
            configVo.setType(Element.LONG_TEXT.getType());
        }
        if (!StringUtils.isEmpty(temp.getDescription())) {
            typeConfig.setTip(temp.getDescription());
        }
    }

    /**
     * 更新配置项
     *
     * @param config
     * @return
     */
    @PutMapping("/config/catalog/value")
    void updateConfig(@RequestBody UpdateConfigsVO config) {
        for (UpdateConfigVO updateConfig1 : config.getConfig()) {
            List<String> values = updateConfig1.getValue();
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
            }
            ConfigInfoBo bo = ConfigInfoBo.builder()
                    .id(Long.valueOf(updateConfig1.getConfigId()))
                    .widgetValue(builder.toString())
                    .build();
            systemConfigService.updateConfigInfoById(bo);
        }
        ConfigInfoBo configInfoBo = ConfigInfoBo.builder().id(config.getConfig().get(0).getConfigId())
                .catalogId(config.getCatalogId()).build();
        systemConfigService.updateConfigInfo(configInfoBo);
    }

    private void buildNacos(@RequestBody UpdateConfigsVO config) {
        if (config.getCatalogId() == null) {
            ConfigInfoBo configInfoBo = systemConfigService.selectConfigInfoById(Long.valueOf(config.getConfig().get(0).getConfigId()));
            config.setCatalogId(configInfoBo.getCatalogId());
        }
        CatalogBo catalogBo = systemConfigService.selectCatalogById(Long.valueOf(config.getCatalogId()));
        List<ConfigInfoBo> configInfoBos = systemConfigService.getConfigByAppCode(catalogBo.getAppCode());
        try {
            CatalogTypeEnum catalogType = CatalogTypeEnum.getCatalogType(catalogBo.getCatalogType());
            JSONObject jsonObject = new JSONObject();
            for (ConfigInfoBo temp : configInfoBos) {
                if (StringUtils.isEmpty(temp.getWidgetValue()) && !StringUtils.isEmpty(temp.getDefaultValue())) {
                    String defaultValue = temp.getDefaultValue();
                    String[] split = defaultValue.split(",");
                    if (split.length == 1) {
                        jsonObject.put(temp.getName(), defaultValue);
                    } else {
                        ArrayList<String> strings = new ArrayList<>(Arrays.asList(split));
                        jsonObject.put(temp.getName(), strings);
                    }
                } else if (!StringUtils.isEmpty(temp.getWidgetValue())) {
                    String value = temp.getWidgetValue();
                    String[] split = value.split(",");
                    if (split.length == 1) {
                        jsonObject.put(temp.getName(), value);
                    } else {
                        ArrayList<String> strings = new ArrayList<>(Arrays.asList(split));
                        jsonObject.put(temp.getName(), strings);
                    }
                }
            }
            boolean successs = configService.publishConfig(catalogBo.getAppCode(), catalogType.getName(), jsonObject.toString());
            log.info("sync nacos is success {} ", successs);
        } catch (NacosException e) {
            log.error("sync nacos is error", e);
        }
    }


}
