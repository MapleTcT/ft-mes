package com.supcon.supfusion.auth.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.auth.common.exception.AuthIdentityConfigException;
import com.supcon.supfusion.auth.common.exception.ThirdAuthErrorEnum;
import com.supcon.supfusion.auth.common.exception.ThirdAuthException;
import com.supcon.supfusion.auth.common.utils.HttpClientUtils;
import com.supcon.supfusion.auth.common.utils.HttpUtil;
import com.supcon.supfusion.auth.dao.mapper.IdentityCenterConfigMapper;
import com.supcon.supfusion.auth.dao.po.IdentityCenterConfigPO;
import com.supcon.supfusion.auth.dao.po.UserPO;
import com.supcon.supfusion.auth.service.AuthLoginLogService;
import com.supcon.supfusion.auth.service.OnlineUserService;
import com.supcon.supfusion.auth.service.ThirdAuthService;
import com.supcon.supfusion.auth.service.UserService;
import com.supcon.supfusion.auth.service.bo.*;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.supcon.supfusion.auth.common.constants.Constants.AUTH_TICKET;
import static com.supcon.supfusion.auth.common.exception.ThirdAuthErrorEnum.USER_ALREADY_BIND;

@Slf4j
@Service
public class ThirdAuthServiceImpl implements ThirdAuthService {

    @Autowired
    UserService userService;
    @Autowired
    OnlineUserService onlineUserService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    IdentityCenterConfigMapper identityCenterConfigMapper;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    private AuthLoginLogService loginLogService;


    private static final String ZHUYUN = "zhuyun";
    private static final String JINDIEYUN = "jindieyun";
    private static final String BLUETRON = "bluetron";

    @Override
    public LoginResponseBO authorize(String code, String protocolType, String realIp, String state) {
        String accessToken = null, refreshToken = null, redirectUrl = null;

        log.info("单点登录认证->授权码code: {}, realIp: {}, state {}", code, realIp, state);
        // 获取第三方配置信息
        IdentityCenterConfigBO identityCenterConfigBO = queryClientIdentityConfigInfo(protocolType);
        String clientId = identityCenterConfigBO.getAppId();
        String clientSecret = identityCenterConfigBO.getAppSecret();
        String tokenUrl = identityCenterConfigBO.getTokenUrl();
        String userInfoUrl = identityCenterConfigBO.getUserinfoUrl();
        String type = identityCenterConfigBO.getProtocolType();
        redirectUrl = identityCenterConfigBO.getRedirectUrl();
        log.info("type==={}", type);
        // 调用第三方接口获取accessToken相关数据
        ThirdToken thirdToken = getToken(code, identityCenterConfigBO);


        // 调用第三方接口获取用户相关数据
        ThirdName userInfo = getUserInfo(thirdToken, clientId, userInfoUrl, type, code);
        if (StringUtils.isNotEmpty(state) && BLUETRON.equals(type) && StringUtils.isEmpty(userInfo.name)) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("access_token", thirdToken.accessToken);
            map.put("refresh_token", thirdToken.refreshToken);
            map.put("expires_in", String.valueOf(thirdToken.expireIn));
            if (StringUtils.isNotEmpty(userInfo.thirdIdentity)) {
                map.put("thirdIdentity", userInfo.thirdIdentity);
            }
            if (StringUtils.isNotEmpty(userInfo.source)) {
                map.put("thirdSource", userInfo.source);
            }
            stringRedisTemplate.opsForHash().putAll(state, map);
            LoginResponseBO loginResponseBO = new LoginResponseBO();
            loginResponseBO.setRedirectUri(redirectUrl);
            loginResponseBO.setUserBind(false);
            return loginResponseBO;
        }
        log.info("获取用户信息接口返回结果->loginUserName : {}", userInfo.name);
        log.info("redirectUrl : {}", redirectUrl);

        // 完成supOS用户内部登录逻辑
        LoginResponseBO loginResponseBO = createUserSession(userInfo, realIp, thirdToken.accessToken, thirdToken.refreshToken, thirdToken.expireIn, type);
        loginResponseBO.setRedirectUri(redirectUrl);
        loginResponseBO.setUserBind(true);
        //记录登录日志
        loginLogService.generateLoginLog(loginResponseBO, "pc", realIp);
        return loginResponseBO;
    }

    @Override
    public LoginResponseBO queryCurrentUserInfo() {
        String userName = UserContext.getUserContext().getUserName();
        UserBO userBO = userService.findByUserName(userName);
        if (Objects.isNull(userBO)) {
            throw new ThirdAuthException(ThirdAuthErrorEnum.GET_USER_INFO_IS_NOT_EXISTED);
        }
        return convertToResponseBO(userBO);
    }


    private ThirdToken getToken(String code, IdentityCenterConfigBO identityCenterConfigBO) {

        if (ZHUYUN.equals(identityCenterConfigBO.getProtocolType())) {
            StringBuffer url = new StringBuffer();
            url.append(identityCenterConfigBO.getTokenUrl()).append("?grant_type=authorization_code").append("&code=").append(code)
                    .append("&client_id=").append(identityCenterConfigBO.getAppId())
                    .append("&client_secret=").append(identityCenterConfigBO.getAppSecret());
            log.info(url.toString());

            try {
                ResponseEntity<JSONObject> response = restTemplate.postForEntity(url.toString(), null, JSONObject.class);
                log.info("getToken from zhuyun, response body is:{}", response.getBody());
                JSONObject body = response.getBody();
                ThirdToken thirdToken = new ThirdToken();
                thirdToken.setAccessToken(body.getString("access_token"));
                thirdToken.setRefreshToken(body.getString("refresh_token"));
                thirdToken.setExpireIn(body.getInteger("expires_in"));
                return thirdToken;
            } catch (Exception e) {
                log.error("getToken from zhuyun is failed:{}", e.getMessage());
                throw new ThirdAuthException(ThirdAuthErrorEnum.GET_TOKEN_BY_ZHUYU_FAILED);
            }
        } else if (JINDIEYUN.equals(identityCenterConfigBO.getProtocolType())) {

            JSONObject json = new JSONObject();
            MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
            json.put("appId", identityCenterConfigBO.getAppId());
            json.put("secret", identityCenterConfigBO.getAppSecret());
            json.put("timestamp", System.currentTimeMillis() + "");
            json.put("scope", "app");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(type);
            headers.add("Accept", MediaType.APPLICATION_JSON.toString());
            HttpEntity<String> postEntity = new HttpEntity<>(json.toString(), headers);
            JinDieAccesstoken body = restTemplate.postForEntity(identityCenterConfigBO.getTokenUrl(), postEntity, JinDieAccesstoken.class).getBody();

            if (body.success || Objects.equals(body.errorCode, 0)) {
                ThirdToken thirdToken = new ThirdToken();
                thirdToken.setAccessToken(body.getData().accessToken);
                thirdToken.setRefreshToken(body.data.getRefreshToken());
                thirdToken.setExpireIn(body.getData().expireIn);
                log.info("金碟云获取token接口返回结果->accessToken : {}", body.data.accessToken);
                return thirdToken;
            } else {
                throw new ThirdAuthException(ThirdAuthErrorEnum.GET_USER_INFO_BY_JINDIEYUN_FAILED);
            }

        } else if (BLUETRON.equals(identityCenterConfigBO.getProtocolType())) {
            StringBuffer url = new StringBuffer();
            try {
                MultiValueMap<String, String> json = new LinkedMultiValueMap<String, String>();
                json.add("client_id", identityCenterConfigBO.getAppId());
                json.add("grant_type", "authorization_code");
                json.add("redirect_uri", identityCenterConfigBO.getRedirectUrl());
                json.add("code", code);
                log.info("body==== {}", json);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                headers.setBasicAuth(identityCenterConfigBO.getAppId(), identityCenterConfigBO.getAppSecret());
                HttpEntity<MultiValueMap<String, String>> postEntity = new HttpEntity<MultiValueMap<String, String>>(json, headers);
                ResponseEntity<JSONObject> response = restTemplate.postForEntity(identityCenterConfigBO.getTokenUrl(), postEntity, JSONObject.class);
                log.info("getToken from bluetron, response body is:{}", response.getBody());
                JSONObject body = response.getBody();
                ThirdToken thirdToken = new ThirdToken();
                thirdToken.setAccessToken(body.getString("access_token"));
                thirdToken.setRefreshToken(body.getString("refresh_token"));
                thirdToken.setExpireIn(body.getInteger("expires_in"));
                return thirdToken;
            } catch (Exception e) {
                log.error("getToken from bluetron is failed:{}", e.getMessage());
                throw new ThirdAuthException(ThirdAuthErrorEnum.GET_TOKEN_BY_BLUETROON_FAILED);
            }
        }
        return null;
    }

    public ThirdToken refreshToken(String protocolType, String clientId, String clientSecret, String clientRefreshToken, String refreshTokenUrl) {
        if (ZHUYUN.equals(protocolType)) {
            StringBuffer url = new StringBuffer();
            url.append(refreshTokenUrl).append("?grant_type=refresh_token").append("&refresh_token=").append(clientRefreshToken)
                    .append("&client_id=").append(clientId)
                    .append("&client_secret=").append(clientSecret);
            log.info(url.toString());

            try {
                ResponseEntity<JSONObject> response = restTemplate.postForEntity(url.toString(), null, JSONObject.class);
                log.info("refreshToken from zhuyun, response body is:{}", response.getBody());
                JSONObject body = response.getBody();
                ThirdToken thirdToken = new ThirdToken();
                thirdToken.setAccessToken(body.getString("access_token"));
                thirdToken.setRefreshToken(body.getString("refresh_token"));
                thirdToken.setExpireIn(body.getInteger("expires_in"));
                return thirdToken;
            } catch (Exception e) {
                log.info("refreshToken from zhuyun is failed:{}", e.getMessage());
                throw new ThirdAuthException(ThirdAuthErrorEnum.REFRESH_TOKEN_BY_ZHUYU_FAILED);
            }
        } else if (BLUETRON.equals(protocolType)) {
            StringBuffer url = new StringBuffer();
            url.append(refreshTokenUrl);
            log.info(url.toString());

            try {
                MultiValueMap<String, String> json = new LinkedMultiValueMap<String, String>();
                json.add("client_id", clientId);
                json.add("grant_type", "refresh_token");
                json.add("refresh_token", clientRefreshToken);
                log.info("body==== {}", json);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                headers.setBasicAuth(clientId, clientSecret);
                HttpEntity<MultiValueMap<String, String>> postEntity = new HttpEntity<MultiValueMap<String, String>>(json, headers);
                ResponseEntity<JSONObject> response = restTemplate.postForEntity(url.toString(), postEntity, JSONObject.class);
                log.info("refreshToken from bluetron, response body is:{}", response.getBody());
                JSONObject body = response.getBody();
                ThirdToken thirdToken = new ThirdToken();
                thirdToken.setAccessToken(body.getString("access_token"));
                thirdToken.setRefreshToken(body.getString("refresh_token"));
                thirdToken.setExpireIn(body.getInteger("expires_in"));
                return thirdToken;
            } catch (Exception e) {
                log.info("refreshToken from bluetron is failed:{}", e.getMessage());
                throw new ThirdAuthException(ThirdAuthErrorEnum.REFRESH_TOKEN_BY_ZHUYU_FAILED);
            }
        }
        return null;
    }

    public JSONObject checkTokenValid(String clientAccessToken, String checkTokenUrl) {
        StringBuffer url = new StringBuffer();
        url.append(checkTokenUrl).append("&access_token=").append(clientAccessToken);
        log.info(url.toString());

        ResponseEntity<JSONObject> response = restTemplate.getForEntity(url.toString(), JSONObject.class);
        return response.getBody();
    }

    private ThirdName getUserInfo(ThirdToken thirdToken, String clientId, String userInfoUrl, String protocolType, String ticket) {
        if (ZHUYUN.equals(protocolType)) {
            StringBuffer url = new StringBuffer();
            url.append(userInfoUrl).append("?access_token=").append(thirdToken.accessToken).append("&client_id=").append(clientId);
            log.info(url.toString());

            try {
                ResponseEntity<JSONObject> response = restTemplate.getForEntity(url.toString(), JSONObject.class);
                log.info("getUserInfo from zhuyun, response body is:{}", response.getBody());
                ThirdName thirdName = new ThirdName();
                thirdName.setName(response.getBody().getString("loginName"));
                return thirdName;
            } catch (Exception e) {
                log.error("getUserInfo from zhuyun failed:{}", e.getMessage());
                throw new ThirdAuthException(ThirdAuthErrorEnum.GET_USER_INFO_BY_ZHUYU_FAILED);
            }
        } else if (JINDIEYUN.equals(protocolType)) {
            JSONObject json = new JSONObject();
            json.put("appid", clientId);
            json.put("ticket", ticket);
            HttpEntity<String> postEntity2 = new HttpEntity<>(json.toString());

            ResponseEntity<JindieUserInfo> response = restTemplate.postForEntity(userInfoUrl + "?accessToken=" + thirdToken.accessToken, postEntity2, JindieUserInfo.class);
            JindieUserInfo body = response.getBody();
            log.info("getUserInfo from jindieyun, response body is:{}", body);

            if (body.success) {
                if (StringUtils.isNotEmpty(body.data.jobNo)) {
                    ThirdName thirdName = new ThirdName();
                    thirdName.setName(body.data.jobNo);
                    return thirdName;
                } else {
                    log.warn("user info from jindieyun is null and reset to l2102007");
                    ThirdName thirdName = new ThirdName();
                    thirdName.setName("l2102007");
                    return thirdName;
                }
            } else {
                throw new ThirdAuthException(ThirdAuthErrorEnum.GET_USER_INFO_BY_JINDIEYUN_FAILED);
            }
        } else if (BLUETRON.equals(protocolType)) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(thirdToken.getAccessToken());
                HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
                ResponseEntity<JSONObject> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, requestEntity, JSONObject.class);
                log.info("getUserInfo from bluetron, response body is:{}", response.getBody());
                String bluetronId = response.getBody().getJSONObject("data").getString("bluetronId");
                UserPO one = userService.getOne(Wrappers.lambdaQuery(UserPO.class).eq(UserPO::getThirdIdentity, bluetronId));
                ThirdName thirdName = new ThirdName();
                thirdName.setName(one == null ? null : one.getUserName());
                thirdName.setSource("bluetron");
                thirdName.setThirdIdentity(bluetronId);
                return thirdName;
            } catch (Exception e) {
                log.error("getUserInfo from bluetron failed:{}", e.getMessage());
                throw new ThirdAuthException(ThirdAuthErrorEnum.GET_USER_INFO_BY_ZHUYU_FAILED);
            }
        }
        return null;
    }

    private LoginResponseBO createUserSession(ThirdName userInfo, String realIp, String clientAccessToken, String clientRefreshToken, Integer expireIn, String protocolType) {
        UserBO userBO = userService.findByUserName(userInfo.name);
        log.info("query user from supos database, user: {}", userBO);
        if (StringUtils.isEmpty(userBO.getUserName())) {
            throw new ThirdAuthException(ThirdAuthErrorEnum.GET_USER_INFO_IS_NOT_EXISTED);
        }
        // 单点登录的用户,如果是第一次登录,则修改标识,无需重置密码
        if (userBO.getLoginFirst()) {
            UserPO updateUser = new UserPO();
            updateUser.setId(userBO.getId());
            updateUser.setLoginFirst(false);
            userService.updateById(updateUser);
        }
        LoginBO loginBO = new LoginBO();
        loginBO.setUserName(userInfo.name);
        loginBO.setPassword(userBO.getPassword());
        loginBO.setLdap(false);
        loginBO.setGrantType("password");
        JSONObject jsonObject = userService.loginKeycloak(loginBO, null, "pc");

        String uuid = UUID.randomUUID().toString().replace("-", "");
        String key = String.format(AUTH_TICKET, uuid);
        LoginResponseBO loginResponseBO = userService.convertLoginResponse(loginBO, userBO);
        if (loginResponseBO.getCurrentCompany() == null) {
            loginResponseBO.setCurrentCompany(loginResponseBO.getCompanies().get(0));
            userService.changeCurrentCompany(userInfo.name, loginResponseBO.getCompanies().get(0).getId());
        }
        loginResponseBO.setTicket(uuid);
        loginResponseBO.setProtocolType(protocolType);
        loginResponseBO.setTenantId(RpcContext.getContext().getTenantId());
        loginResponseBO.setStatus("ok");
        loginResponseBO.setUserType(userBO.getUserType());
        loginResponseBO.setAccessToken(jsonObject.getString("access_token"));
        loginResponseBO.setRefreshToken(jsonObject.getString("refresh_token"));
        Integer expires_in = jsonObject.getInteger("expires_in");
        loginResponseBO.setExpiresIn(expires_in <= expireIn ? expires_in : expireIn);
        loginResponseBO.setClientId("pc_" + RpcContext.getContext().getTenantId());
        loginResponseBO.setClientAccessToken(clientAccessToken);
        loginResponseBO.setClientRefreshToken(clientRefreshToken);
        //增加在线用户
        OnlineUserBO onlineUserBO = userService.buildOnlineUserBO(realIp, "pc", uuid, loginResponseBO);
        onlineUserService.createOnlineUser(onlineUserBO);
        //缓存ticket信息
        loginResponseBO.setLoginType("1");
        userService.cacheTicket(jsonObject, key, loginResponseBO);
        return loginResponseBO;
    }

    @Override
    public LoginResponseBO thirdIdentityUserBind(String userName, String password, String realIp, String state) {
        UserBO userBO = userService.findByUserName(userName);


        // todo
        if (StringUtils.isNotEmpty(userBO.getThirdIdentity())) {
            throw new AuthIdentityConfigException(USER_ALREADY_BIND);
        }

        String deviceType = "pc";
        LoginBO loginBO = new LoginBO();
        loginBO.setUserName(userName);
        loginBO.setPassword(password);
        loginBO.setLdap(false);
        loginBO.setGrantType("password");
        JSONObject jsonObject = userService.loginKeycloak(loginBO, null, deviceType);
        if (userBO.getLoginFirst()) {
            UserPO updateUser = new UserPO();
            updateUser.setId(userBO.getId());
            updateUser.setLoginFirst(false);
            userService.updateById(updateUser);
        }

        String uuid = UUID.randomUUID().toString().replace("-", "");
        String key = String.format(AUTH_TICKET, uuid);
        LoginResponseBO loginResponseBO = userService.convertLoginResponse(loginBO, userBO);
        loginResponseBO.setTicket(uuid);
        loginResponseBO.setTenantId(RpcContext.getContext().getTenantId());
        loginResponseBO.setStatus("ok");
        loginResponseBO.setUserType(userBO.getUserType());
        loginResponseBO.setAccessToken(jsonObject.getString("access_token"));
        loginResponseBO.setRefreshToken(jsonObject.getString("refresh_token"));
        loginResponseBO.setExpiresIn(jsonObject.getInteger("expires_in"));
        loginResponseBO.setClientId(deviceType + "_" + RpcContext.getContext().getTenantId());
        loginResponseBO.setUserType(userBO.getUserType());
        loginResponseBO.setAccessToken(jsonObject.getString("access_token"));
        loginResponseBO.setRefreshToken(jsonObject.getString("refresh_token"));
        Integer expires_in = jsonObject.getInteger("expires_in");
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(state);
        Integer expireIn = Integer.valueOf((String) entries.get("expires_in"));
        loginResponseBO.setExpiresIn(expires_in <= expireIn ? expires_in : expireIn);
        loginResponseBO.setClientId(deviceType + "_" + RpcContext.getContext().getTenantId());
        loginResponseBO.setClientAccessToken((String) entries.get("access_token"));
        loginResponseBO.setClientRefreshToken((String) entries.get("refresh_token"));
        //增加在线用户
        OnlineUserBO onlineUserBO = userService.buildOnlineUserBO(realIp, deviceType, uuid, loginResponseBO);
        onlineUserService.createOnlineUser(onlineUserBO);
        //缓存ticket信息
        loginResponseBO.setLoginType("1");
        userService.cacheTicket(jsonObject, key, loginResponseBO);
        String thirdSource = (String) entries.get("thirdSource");
        String thirdIdentity = (String) entries.get("thirdIdentity");
        try {
            IdentityCenterConfigBO identityCenterConfigBO = queryClientIdentityConfigInfo("bluetron");
            JSONObject json = new JSONObject();
            json.put("clientId", identityCenterConfigBO.getAppId());
            json.put("username", userName);
            String host = HttpClientUtils.getHostPort(identityCenterConfigBO.getTokenUrl());
            HashMap<String, String> map = new HashMap<>();
            map.put(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
            map.put(HttpHeaders.AUTHORIZATION, "Bearer " + (String) entries.get("access_token"));
            String post = HttpClientUtils.post(host + "/connect/bind", map, json.toString());
            log.info("bind user {}", post);
            stringRedisTemplate.delete(state);
        } catch (Exception e) {
            log.info("bind user failed:{}", e.getMessage());
            throw new ThirdAuthException(ThirdAuthErrorEnum.GET_TOKEN_BY_ZHUYU_FAILED);
        }
        if (StringUtils.isNotEmpty(thirdIdentity) && StringUtils.isNotEmpty(thirdSource)) {
            UserPO temp = new UserPO();

            temp.setId(userBO.getId());
            temp.setThirdIdentity(thirdIdentity);
            temp.setThirdSource(thirdSource);
            userService.updateById(temp);
        }
        return loginResponseBO;
    }

    private LoginResponseBO convertToResponseBO(UserBO userBO) {
        LoginBO loginBO = new LoginBO();
        loginBO.setUserName(userBO.getUserName());
        loginBO.setPassword(userBO.getPassword());
        loginBO.setLdap(false);
        loginBO.setGrantType("password");
        LoginResponseBO loginResponseBO = userService.convertLoginResponse(loginBO, userBO);
        loginResponseBO.setTenantId(RpcContext.getContext().getTenantId());
        if (loginBO.getLdap()) {
            loginResponseBO.setStatus("ok");
        } else {
            loginResponseBO.setStatus(userBO.getLoginFirst() ? "firstLogin" : "ok");
        }
        loginResponseBO.setUserType(userBO.getUserType());
        return loginResponseBO;
    }

    public IdentityCenterConfigBO queryClientIdentityConfigInfo(@Nullable String protocolType) {
        QueryWrapper<IdentityCenterConfigPO> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(protocolType)) {
            queryWrapper.eq("system_flag", 1);
        } else {
            queryWrapper.eq("system_flag", 0);
        }
        queryWrapper.eq("enable", 1);

        queryWrapper.eq(StringUtils.isNotEmpty(protocolType), "protocol_type", protocolType);

        List<IdentityCenterConfigPO> identityCenterConfigs = identityCenterConfigMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(identityCenterConfigs)) {
            throw new ThirdAuthException(ThirdAuthErrorEnum.GET_IDENTITY_CONFIG_INFO_FAILED);
        }
        IdentityCenterConfigBO identityCenterConfigBO = new IdentityCenterConfigBO();
        identityCenterConfigBO.setAppId(identityCenterConfigs.get(0).getAppId());
        identityCenterConfigBO.setAppSecret(identityCenterConfigs.get(0).getAppSecret());
        identityCenterConfigBO.setTokenUrl(identityCenterConfigs.get(0).getTokenUrl());
        identityCenterConfigBO.setUserinfoUrl(identityCenterConfigs.get(0).getUserinfoUrl());
        identityCenterConfigBO.setRefreshUrl(identityCenterConfigs.get(0).getRefreshUrl());
        identityCenterConfigBO.setRedirectUrl(identityCenterConfigs.get(0).getRedirectUrl());
        identityCenterConfigBO.setProtocolType(identityCenterConfigs.get(0).getProtocolType());
        identityCenterConfigBO.setLogoutUrl(identityCenterConfigs.get(0).getLogoutUrl());
        return identityCenterConfigBO;
    }


    @Data
    public static class ThirdToken {
        private String accessToken;
        private Integer expireIn;
        private String refreshToken;
    }

    @Data
    public static class ThirdName {
        private String name;
        private String source;
        private String thirdIdentity;
    }

    @Data
    public static class JinDieAccesstoken {

        private Data data;
        private Integer errorCode;
        private Boolean success;

        @lombok.Data
        public static class Data {
            private String accessToken;
            private Integer expireIn;
            private String refreshToken;
        }
    }


    @Data
    public static class JindieUserInfo {

        private Data data;
        private Object error;
        private Long errorCode;
        private Boolean success;

        @lombok.Data
        public static class Data {

            private String appid;
            private String deviceId;
            private String eid;
            private String jobNo;
            private String networkid;
            private String openid;
            private Object ticket;
            private String userid;
            private String username;

        }
    }

    @Override
    public void logout(String protocolType, String clientId, String clientSecret, String token, String logoutUrl) {
        if (BLUETRON.equals(protocolType)) {
            StringBuffer url = new StringBuffer();
            url.append(logoutUrl)
                    .append("?client_id=").append(clientId)
                    .append("&token=").append(token);
            log.info(url.toString());

            try {
                HashMap<String, String> map = new HashMap<>();
                map.put(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
                Charset charset = StandardCharsets.ISO_8859_1;
                CharsetEncoder encoder = charset.newEncoder();
                if (encoder.canEncode(clientId) && encoder.canEncode(clientSecret)) {
                    String credentialsString = clientId + ":" + clientSecret;
                    byte[] encodedBytes = Base64.getEncoder().encode(credentialsString.getBytes(charset));
                    String encodedCredentials = new String(encodedBytes, charset);
                    map.put("Authorization", "Basic " + encodedCredentials);
                }
                String result = HttpUtil.doDelete(url.toString(), map);
                log.info("bluetron logout {} ", result);
            } catch (Exception e) {
                log.info("refreshToken from bluetron is failed:{}", e.getMessage());
//                throw new ThirdAuthException(ThirdAuthErrorEnum.REFRESH_TOKEN_BY_ZHUYU_FAILED);
            }
        }
    }
}
