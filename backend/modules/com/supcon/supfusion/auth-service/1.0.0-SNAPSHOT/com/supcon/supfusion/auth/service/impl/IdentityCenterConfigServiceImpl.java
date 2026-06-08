package com.supcon.supfusion.auth.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.supcon.supfusion.auth.common.exception.AuthIdentityConfigErrorEnum;
import com.supcon.supfusion.auth.common.exception.AuthIdentityConfigException;
import com.supcon.supfusion.auth.common.utils.BijectionUtils;
import com.supcon.supfusion.auth.common.utils.HttpClientUtils;
import com.supcon.supfusion.auth.dao.mapper.IdentityCenterConfigMapper;
import com.supcon.supfusion.auth.dao.po.IdentityCenterConfigPO;
import com.supcon.supfusion.auth.service.IdentityCenterConfigService;
import com.supcon.supfusion.auth.service.bo.IdentityCenterConfigBO;
import com.supcon.supfusion.auth.service.bo.RegisterOauthClientBo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 王海峰
 * @since 2021-05-08
 */
@Slf4j
@Service
public class IdentityCenterConfigServiceImpl implements IdentityCenterConfigService {


    static final ImmutableSet internalOauthName = ImmutableSet.of("bluetron"
            //        ,"dingding","wechat"
    );

    @Autowired
    private IdentityCenterConfigMapper identityCenterConfigMapper;

    /**
     * 根据id查询IdentityCenterConfig
     *
     * @return
     */
    @Override
    public IdentityCenterConfigBO getIdentityCenterConfigById(Long id) {
        return BijectionUtils.apply(identityCenterConfigMapper.selectById(id), IdentityCenterConfigBO::new);
    }

    /**
     * 查询IdentityCenterConfig列表
     *
     * @param keyword
     * @return
     */
    @Override
    public List<IdentityCenterConfigBO> listIdentityCenterConfig(String keyword) {

        return BijectionUtils.applys(identityCenterConfigMapper
                .selectList(new QueryWrapper<IdentityCenterConfigPO>().like(StringUtils.isNoneEmpty(keyword),
                        "system_name",
                        keyword
                )), IdentityCenterConfigBO::new);
    }


    /**
     * 新增IdentityCenterConfig
     *
     * @param identityCenterConfig
     */
    @Override
    @Transactional
    public Integer addIdentityCenterConfig(IdentityCenterConfigBO identityCenterConfig) {

        IdentityCenterConfigPO config = BijectionUtils.apply(identityCenterConfig, IdentityCenterConfigPO::new);

        //oauth and system name should unqic
        QueryWrapper<IdentityCenterConfigPO> last = new QueryWrapper<IdentityCenterConfigPO>()
                .eq("system_name", identityCenterConfig.getSystemName())
                .last(" for update ");
        IdentityCenterConfigPO old = identityCenterConfigMapper.selectOne(last);
        //todo delete=1 is always
        if (old == null) {

        } else {
            config.setId(old.getId());
            if (!old.getValid()) {
                config.setValid(true);
                return identityCenterConfigMapper.updateById(config);
            }
            throw new AuthIdentityConfigException(AuthIdentityConfigErrorEnum.OAUTH_NAME_CONFIG_EXIST);
        }

        return identityCenterConfigMapper.insert(config);
    }

    /**
     * 更新IdentityCenterConfig
     */
    @Override
    @Transactional
    public Integer updateIdentityCenterConfig(IdentityCenterConfigBO identityCenterConfig) {
        IdentityCenterConfigPO config = BijectionUtils.apply(identityCenterConfig, IdentityCenterConfigPO::new);

        //oauth and system name should unqic
        QueryWrapper<IdentityCenterConfigPO> last = new QueryWrapper<IdentityCenterConfigPO>()
                .eq("system_name", identityCenterConfig.getSystemName())
                .last(" for update ");
        IdentityCenterConfigPO identityCenterConfigs = identityCenterConfigMapper.selectOne(last);

        if (identityCenterConfigs != null && !Objects.equals(identityCenterConfigs.getId(), identityCenterConfig.getId())) {
            throw new AuthIdentityConfigException(AuthIdentityConfigErrorEnum.OAUTH_NAME_CONFIG_EXIST);
        }
        return identityCenterConfigMapper.updateById(config);
    }

    /**
     * 根据id删除IdentityCenterConfig
     */
    @Override
    @Transactional
    public Integer deleteIdentityCenterConfigById(Long id) {
        return identityCenterConfigMapper.deleteById(id);
    }

    @Override
    public Integer updateIdentityCenterConfigStatus(IdentityCenterConfigBO target) {

        Long id = target.getId();
        Objects.requireNonNull(id);
        Boolean enable = Objects.requireNonNull(target.getEnable());
        IdentityCenterConfigPO currentConfig = identityCenterConfigMapper.selectById(id);

        if (null == currentConfig) {
            return 1;
        } else {
            currentConfig.setEnable(target.getEnable());

            if (currentConfig.getSystemFlag()) {
                // dothing
            } else {
                // most enable 1 at external,close other if enabled

                if (enable) {
                    List<IdentityCenterConfigPO> list = identityCenterConfigMapper.selectList(new QueryWrapper<>());
                    list.stream()
                            .filter(x -> !x.getSystemFlag())
                            .filter(IdentityCenterConfigPO::getEnable)
                            .forEach(x -> {
                                x.setEnable(false);
                                identityCenterConfigMapper.updateById(x);
                            });
                }
            }


        }

        return identityCenterConfigMapper.updateById(currentConfig);
    }

    @Override
    public Integer deleteIdentityCenterConfigByIds(Long[] id) {
        if (ArrayUtils.isEmpty(id)) {
            return 0;
        } else {
            return identityCenterConfigMapper.deleteBatchIds(Arrays.stream(id).collect(Collectors.toList()));
        }
    }


    @Override
    public Map<String, List<IdentityCenterConfigBO>> getCurrentAuthConfig() {

        List<IdentityCenterConfigPO> configs = identityCenterConfigMapper.selectList(Wrappers.lambdaQuery(IdentityCenterConfigPO.class).eq(IdentityCenterConfigPO::getEnable, true).orderByDesc(IdentityCenterConfigPO::getCreateTime));

        Map<Boolean, List<IdentityCenterConfigBO>> collect = BijectionUtils.applys(configs, IdentityCenterConfigBO::new)
                .stream().collect(Collectors.groupingBy(IdentityCenterConfigBO::getSystemFlag));

        Map<String, List<IdentityCenterConfigBO>> map = Maps.newHashMap();
        collect.computeIfPresent(true, (k, v) -> v);
        collect.putIfAbsent(true, Collections.emptyList());
        collect.computeIfPresent(false, (k, v) -> v.stream().filter(IdentityCenterConfigBO::getEnable).collect(Collectors.toList()));
        collect.putIfAbsent(false, Collections.emptyList());
        collect.forEach((k, v) -> {
            if (k) {
                map.put("internal", v);
            } else {
                map.put("external", v);
            }
        });
        map.compute("external", (k, v) -> {
                    v.stream().filter(x -> Objects.equals(x.getProtocolType(), "zhuyun")).forEach(x -> {
                        String oauthUrl = x.getOauthUrl();
                        if (StringUtils.isNotEmpty(oauthUrl)) {
                            String[] split = oauthUrl.split("\\?");
                            String base = split[0];
                            Map<String, Object> paramMap = new HashMap<>();
                            if (split.length > 1) {
                                paramMap = HttpClientUtils.getUrlParams(split[1]);
                            }
                            paramMap.compute("redirect_uri", (s, o) -> x.getRedirectUrl());
                            paramMap.compute("response_type", (s, o) -> "code");
                            paramMap.compute("state", (s, o) -> RandomStringUtils.randomNumeric(5));
                            paramMap.compute("client_id", (s, o) -> x.getAppId());

                            String urlParamsByMap = HttpClientUtils.getUrlParamsByMap(paramMap);
                            x.setOauthUrl(base + "?" + urlParamsByMap);

                        }
                        String logoutUrl = x.getLogoutUrl();
                        if (StringUtils.isNotEmpty(logoutUrl)) {
                            Map<String, Object> paramMap = new HashMap<>();
                            String[] split = logoutUrl.split("\\?");
                            String base = split[0];
                            if (split.length > 1) {
                                paramMap = HttpClientUtils.getUrlParams(split[1]);
                            }

                            paramMap.compute("redirctToUrl", (s, o) -> String.format("%s", HttpClientUtils.getHostPort(x.getRedirectUrl())));
                            paramMap.compute("redirectToLogin", (s, o) -> "true");
                            paramMap.compute("entityId", (s, o) -> x.getAppId());

                            x.setLogoutUrl(base + "?" + HttpClientUtils.getUrlParamsByMap(paramMap));
                        }

                    });
                    v.stream().filter(x -> Objects.equals(x.getProtocolType(), "jindieyun")).forEach(x -> {
                        String qrcodeUrl = x.getQrcodeUrl();
                        String appId = x.getAppId();
                        String qrcodeAppid = x.getQrcodeAppid();
                        HashMap<String, Object> paramMap = new HashMap<String, Object>() {{
                            put("appid", qrcodeAppid);
                            try {
                                put("redirect_uri", URLEncoder.encode(x.getRedirectUrl(), "UTF-8"));
                            } catch (UnsupportedEncodingException e) {

                            }
                        }};
                        x.setQrcodeUrl(qrcodeUrl + "?" + HttpClientUtils.getUrlParamsByMap(paramMap));
                    });
                    return v;
                }
        );

        map.compute("internal", (k, v) -> {
                    v.stream().filter(x -> Objects.equals(x.getProtocolType(), "bluetron")).forEach(x -> {
                        String oauthUrl = x.getOauthUrl();
                        if (StringUtils.isNotEmpty(oauthUrl)) {
                            String[] split = oauthUrl.split("\\?");
                            String base = split[0];

                            Map<String, Object> paramMap = new HashMap<>();
                            if (split.length > 1) {
                                paramMap = HttpClientUtils.getUrlParams(split[1]);
                            }
                            paramMap.compute("redirect_uri", (s, o) -> x.getRedirectUrl());
                            paramMap.compute("response_type", (s, o) -> "code");
                            paramMap.compute("state", (s, o) -> RandomStringUtils.randomNumeric(5));
                            paramMap.compute("client_id", (s, o) -> x.getAppId());
                            paramMap.compute("scope", (s, o) -> "openid profile");

                            String urlParamsByMap = HttpClientUtils.getUrlParamsByMap(paramMap);
                            x.setOauthUrl(base + "?" + urlParamsByMap);

                        }
                    });
                    return v;
                }
        );

        return map;
    }


    @Override
    public String registerOauth2Client(RegisterOauthClientBo bo,String host) {

        String oauthName = bo.getClientName();

        String appId = bo.getClientId();
        String appSecret = bo.getClientSecret();
        // lanzuoyun
        String protocolType = bo.getProtocolType();

        String authorizationUri = bo.getAuthorizationUri();
        String logoutUri = bo.getLogoutUri();
        String tokenUri = bo.getTokenUri();
        String userinfoUri = bo.getUserinfoUri();

        // internal,external
        String clientType = bo.getClientType();

        boolean sysFlag = false;
        if (Objects.equals(clientType, "internal")) {
            sysFlag = true;
            if (!internalOauthName.contains(protocolType)) {
                throw new AuthIdentityConfigException(AuthIdentityConfigErrorEnum.UNSUPPORT_INTERNEL_OAUTH_NAME);
            }
        } else {
            throw new AuthIdentityConfigException(AuthIdentityConfigErrorEnum.UNSUPPORT_OPENAPI_EXTERNAL);
        }

        String redirectUrl = "http://"+host + "/inter-api/auth/v1/third/authorize?protocolType=" + protocolType;
        Boolean enable = bo.getEnable();
        IdentityCenterConfigBO identityCenterConfigBO = new IdentityCenterConfigBO()
                .setEnable(enable)
                .setProtocolType(protocolType)
                .setSystemFlag(sysFlag)
                .setOauthName("蓝卓云认证")
                .setSystemName(oauthName)
                .setRedirectUrl(redirectUrl)
                .setAppId(appId)
                .setAppSecret(appSecret)
                .setOauthUrl(authorizationUri)
                .setLogoutUrl(logoutUri)
                .setTokenUrl(tokenUri)
                .setUserinfoUrl(userinfoUri);

        addIdentityCenterConfig(identityCenterConfigBO);
        return redirectUrl;
    }
}
