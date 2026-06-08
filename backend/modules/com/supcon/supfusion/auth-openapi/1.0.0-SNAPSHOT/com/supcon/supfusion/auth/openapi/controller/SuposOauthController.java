package com.supcon.supfusion.auth.openapi.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.common.utils.MD5Generator;
import com.supcon.supfusion.auth.manager.KeycliandAdminClient;
import com.supcon.supfusion.auth.manager.feign.client.TokenClient;
import com.supcon.supfusion.auth.openapi.suposvo.OauthTokenVO;
import com.supcon.supfusion.auth.openapi.suposvo.OauthValidVO;
import com.supcon.supfusion.auth.openapi.suposvo.UserNameVO;
import com.supcon.supfusion.auth.openapi.vo.OauthVO;
import com.supcon.supfusion.auth.service.OnlineUserService;
import com.supcon.supfusion.auth.service.UserService;
import com.supcon.supfusion.auth.service.bo.AuthorizationBO;
import com.supcon.supfusion.auth.service.bo.LoginResponseBO;
import com.supcon.supfusion.auth.service.cache.AuthTicketCache;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.supcon.supfusion.auth.common.constants.Constants.*;

@Slf4j
@RestController
@OpenApi(path = "/open-api/supos/auth/v2")
public class SuposOauthController {
    @Resource
    private TokenClient tokenClient;

    @Resource
    private AuthTicketCache authTicketCache;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private KeycliandAdminClient keycliandAdminClient;

    @Resource
    private OnlineUserService onlineUserService;

    @Resource
    private UserService userService;



    @GetMapping("/code/authorize")
    public void buildGet(@RequestParam(name = "redirectUri", required = false) String redirectUrl,
                         @RequestParam(name = "state", required = false) String state,
                         @RequestParam(name = "responseType", required = false) String responseType,
                         @CookieValue(name = "suposTicket", required = false) String ticket,
                         @RequestHeader(name = "X-Forwarded-Host") String host,
                         HttpServletResponse response) throws Exception {
        boolean verify = verify(redirectUrl, state, responseType, response);
        if (!verify) {
            return;
        }
        String[] split = host.split(",");
        if (StringUtils.isNotEmpty(ticket)) {
            Map<Object, Object> userInfo = authTicketCache.getMapByTicket(ticket);
            if (!userInfo.isEmpty()) {
                alreadyLogin(redirectUrl, state, ticket, response);
            } else {
                noLogin(redirectUrl, state, response, split[0]);
            }
        } else {
            noLogin(redirectUrl, state, response, split[0]);
        }
    }


    @GetMapping("/code/accessToken")
    public OauthTokenVO getToken(@RequestParam(name = "grantType", required = false) String grantType,
                                 @RequestParam(name = "code", required = false) String code,
                                 @RequestParam(name = "logoutUri", required = false) String logoutUri,
                                 HttpServletResponse response) throws Exception {
        OauthVO oauthVO = new OauthVO();
        oauthVO.setCode(code);
        oauthVO.setGrantType(grantType);
        oauthVO.setLogoutUri(logoutUri);
        boolean verify = verify(oauthVO, response);
        if (!verify) {
            return null;
        }
        OauthTokenVO tokenRespon = new OauthTokenVO();
        LoginResponseBO loginResponseBO = getLoginResponseBO(oauthVO, response, tokenRespon);
        if (loginResponseBO == null) {
            return null;
        }
//        buildUserInfo(tokenRespon, "Bearer", loginResponseBO.getAccessToken());
        return tokenRespon;
    }

    @GetMapping("/code/refreshToken")
    public OauthTokenVO refreshToken(@RequestParam(name = "refreshToken",required = false) String refreshToken,
                                     @RequestHeader(name = "Authorization",required = false) String accessToken,
                                     HttpServletResponse response) throws Exception {
        if(StringUtils.isEmpty(refreshToken)){
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "refreshToken必填");
            return null;
        }
        if(StringUtils.isEmpty(accessToken)){
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "accessToken必填");
            return null;
        }
        OauthTokenVO tokenRespon = new OauthTokenVO();
        accessToken = accessToken.split(Constants.SPACE_CHAR)[1];
        AuthorizationBO authorizationBO = getAuthorizationBO(accessToken, refreshToken, tokenRespon, response);
        if (authorizationBO == null) {
            return null;
        }
//        buildUserInfo(tokenRespon, "Bearer", authorizationBO.getAccessToken());
        return tokenRespon;
    }

    @GetMapping("/username")
    public UserNameVO refreshToken(@RequestParam(name = "accessToken",required = false) String accessToken,
                                   HttpServletResponse response) throws Exception {
        if(StringUtils.isEmpty(accessToken)){
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "accessToken必填");
            return null;
        }
        String bindAccountKey = MD5Generator.getInstance().generateValue(accessToken);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(bindAccountKey);
        String ticket = (String) entries.get("ticket");
        if(StringUtils.isEmpty(ticket)){
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "accessToken无效");
            return null;
        }
        Map<Object, Object> authorizationMap = authTicketCache.getMapByTicket(ticket);
        String userName = (String) authorizationMap.get(USER_NAME);
        UserNameVO userNameVO = new UserNameVO();
        userNameVO.setUsername(userName);
        return userNameVO;
    }

    @GetMapping("/logout")
    public void validAccessToken(@RequestParam(name = "accessToken",required = false) String accessToken,
                                 HttpServletResponse response) throws Exception {
        if(StringUtils.isEmpty(accessToken)){
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "accessToken必填");
            return;
        }
        String bindAccountKey = MD5Generator.getInstance().generateValue(accessToken);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(bindAccountKey);
        String ticket = (String) entries.get("ticket");
        if(StringUtils.isEmpty(ticket)){
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "accessToken无效");
            return;
        }
        if (StringUtils.isNotBlank(ticket)) {
            userService.logout(ticket);
        }
    }

    @GetMapping("/{accessToken}/valid")
    public OauthValidVO logout(@PathVariable(name = "accessToken") String accessToken,
                               HttpServletResponse response) throws Exception {
        OauthValidVO oauthValidVO = new OauthValidVO();
        oauthValidVO.setValid(true);
        String bindAccountKey = MD5Generator.getInstance().generateValue(accessToken);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(bindAccountKey);
        String ticket = (String) entries.get("ticket");
        if (ticket == null) {
            oauthValidVO.setValid(false);
        }
        Map<Object, Object> authorizationMap = authTicketCache.getMapByTicket(ticket);
        if (authorizationMap.isEmpty()) {
            oauthValidVO.setValid(false);
        }
        return oauthValidVO;
    }

    private AuthorizationBO getAuthorizationBO(String accessToken, String refreshToken, OauthTokenVO tokenRespon, HttpServletResponse response) throws Exception {
        String bindAccountKey = MD5Generator.getInstance().generateValue(accessToken);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(bindAccountKey);
        String ticket = (String) entries.get("ticket");
        Map<Object, Object> authorizationMap = authTicketCache.getMapByTicket(ticket);
        if (authorizationMap.isEmpty()) {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "无效accessToken");
            return null;
        }
        String accessTokenCache = (String) authorizationMap.get(ACCESS_TOKEN);
        String tokenType = (String) authorizationMap.get(TOKEN_TYPE);
        String refreshTokenCache = (String) authorizationMap.get(REFRESH_TOKEN);
        String client_id = (String) authorizationMap.get(CLIENT_ID);
        String companyId = (String) authorizationMap.get(COMPANY_ID);
        String userName = (String) authorizationMap.get(USER_NAME);
        HashMap<String, String> map = new HashMap<>();
        map.put(CLIENT_ID, client_id);
        map.put(REFRESH_TOKEN, refreshTokenCache);
        map.put(GRANT_TYPE, "refresh_token");
        if (!refreshTokenCache.equals(refreshToken)) {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "无效refreshToken");
            return null;
        }
        String authenticateJSON = tokenClient.refreshToken(RpcContext.getContext().getTenantId(), map, tokenType + " " + accessTokenCache);
        if (authenticateJSON.contains("error")) {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "无效accessToken");
            return null;
        }
        AuthorizationBO authorizationBO = JSONObject.parseObject(authenticateJSON, AuthorizationBO.class);
        if (companyId != null) {
            authorizationBO.setCompanyId(Long.valueOf(companyId));
        }
        authorizationBO.setClientId(client_id);
        authorizationBO.setTenantId(RpcContext.getContext().getTenantId());
        if (!org.springframework.util.StringUtils.isEmpty(userName)) {
            authorizationBO.setUserName(userName);
        }
        authTicketCache.storeAuthorization(ticket, authorizationBO);
        tokenRespon.setAccessToken(authorizationBO.getAccessToken());
        tokenRespon.setRefreshToken(authorizationBO.getRefreshToken());
        tokenRespon.setExpiresIn(authorizationBO.getExpiresIn());
        String md5 = MD5Generator.getInstance().generateValue(authorizationBO.getAccessToken());
        HashMap<String, String> temp = new HashMap<>();
        temp.put(TICKET, ticket);
        stringRedisTemplate.opsForHash().putAll(md5, temp);
        stringRedisTemplate.opsForHash().put(String.format(TENANT_TICKET, ticket), OAUTH2_TOKEN, authorizationBO.getAccessToken());
        stringRedisTemplate.delete(bindAccountKey);
        return authorizationBO;
    }

    private LoginResponseBO getLoginResponseBO(OauthVO vo, HttpServletResponse response, OauthTokenVO tokenRespon) throws Exception {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(vo.getCode());
        if (entries.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "无效code");
            return null;
        }
        String loginTicket = (String) entries.get("ticket");
        LoginResponseBO loginResponseBO = userService.accessToken(loginTicket);
        if (loginResponseBO == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "无效code");
            return null;
        }
        tokenRespon.setAccessToken(loginResponseBO.getAccessToken());
        tokenRespon.setRefreshToken(loginResponseBO.getRefreshToken());
        tokenRespon.setExpiresIn(loginResponseBO.getExpiresIn());
        String md5 = MD5Generator.getInstance().generateValue(loginResponseBO.getAccessToken());
        HashMap<String, String> map = new HashMap<>();
        map.put("ticket", loginTicket);
        stringRedisTemplate.opsForHash().putAll(md5, map);
        String backLogoutUrl = (String) stringRedisTemplate.opsForHash().get(String.format(TENANT_TICKET, loginTicket), BACKEND_LOGOUT_URL);
        List<String> list = null;
        if (!org.springframework.util.StringUtils.isEmpty(backLogoutUrl)) {
            list = JSON.parseArray(backLogoutUrl, String.class);
            if (!org.springframework.util.StringUtils.isEmpty(vo.getLogoutUri())) {
                list.add(vo.getLogoutUri());
            }
        } else {
            list = new ArrayList<>();
            if (!org.springframework.util.StringUtils.isEmpty(vo.getLogoutUri())) {
                list.add(vo.getLogoutUri());
            }
        }
        stringRedisTemplate.opsForHash().put(String.format(TENANT_TICKET, loginTicket), BACKEND_LOGOUT_URL, JSON.toJSONString(list));
        stringRedisTemplate.opsForHash().put(String.format(TENANT_TICKET, loginTicket), OAUTH2_TOKEN, loginResponseBO.getAccessToken());
        stringRedisTemplate.delete(vo.getCode());
        return loginResponseBO;
    }

//    private void buildUserInfo(OauthTokenVO tokenRespon, String bearer, String accessToken2) {
//        String userInfo = tokenClient.getUserInfo(RpcContext.getContext().getTenantId(), bearer + " " + accessToken2);
//        JSONObject jsonObject = JSON.parseObject(userInfo);
//        tokenRespon.setCompanyCode(jsonObject.getString("company_code"));
//        tokenRespon.setPersonCode(jsonObject.getString("staff_code"));
//        tokenRespon.setUserName(jsonObject.getString("user_name"));
//        tokenRespon.setUserType(jsonObject.getIntValue("user_type"));
//    }


    private boolean verify(OauthVO vo, HttpServletResponse response) {
        if (org.springframework.util.StringUtils.isEmpty(vo.getGrantType())) {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "grantType必填");
            return false;
        }
        if ("authorization_code".equals(vo.getGrantType())) {
            if (org.springframework.util.StringUtils.isEmpty(vo.getCode())) {
                errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "code必填");
                return false;
            }
            if (org.springframework.util.StringUtils.isEmpty(vo.getLogoutUri())) {
                errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "backendLogoutUrl必填");
                return false;
            }
        } else if ("refresh_token".equals(vo.getGrantType())) {
            if (org.springframework.util.StringUtils.isEmpty(vo.getRefreshToken())) {
                errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "refreshToken必填");
                return false;
            }
        } else {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "无效grantType");
            return false;
        }
        return true;
    }

    private void errorResponse(HttpServletResponse response, Integer statusCode, Integer code, String message) {
        try {
            response.setStatus(statusCode);
            response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
            HashMap<String, Object> map = new HashMap<>();
            map.put("message", message);
            map.put("code", code);
            ObjectMapper objectMapper = new ObjectMapper();
            String remind = objectMapper.writeValueAsString(map);
            response.getWriter().println(remind);
        } catch (Exception e) {

        }
    }

    private void alreadyLogin(String redirectUrl, String state, String ticket, HttpServletResponse response) throws Exception {
        String code = MD5Generator.getInstance().generateValue();
        HashMap<String, String> map = new HashMap<>();
        map.put("ticket", ticket);
        map.put("state", state);
        stringRedisTemplate.opsForHash().putAll(code, map);
        stringRedisTemplate.expire(code, 10, TimeUnit.MINUTES);
        response.setStatus(HttpServletResponse.SC_FOUND);
        if (redirectUrl.contains("?")) {
            redirectUrl = redirectUrl + "&code=" + code + "&state=" + state;
        }else {
           redirectUrl = redirectUrl + "?code=" + code + "&state=" + state;
        }
        if(valid(redirectUrl)){
          response.sendRedirect(redirectUrl);
        }else {
            errorResponse(response, 400, 800003, "redirectUri错误");
        }
    }

    private void noLogin(@RequestParam("redirect_uri") String redirectUrl, @RequestParam("state") String state, HttpServletResponse response, String s) throws IOException {
        response.setStatus(HttpServletResponse.SC_FOUND);
        String key = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(Constants.KC_RESTART, key);
        cookie.setHttpOnly(true);
        cookie.setPath("/inter-api/auth/login");
        response.addCookie(cookie);
        String url = "http://" + s + "/#/user/login";
        response.sendRedirect(url);
        HashMap<String, String> map = new HashMap<>();
        map.put("redirectUri", redirectUrl);
        map.put("state", state);
        stringRedisTemplate.opsForHash().putAll(key, map);
        stringRedisTemplate.expire(key, 10, TimeUnit.MINUTES);
    }

    private boolean verify(String redirectUrl, String state, String responseType, HttpServletResponse response) {
        if (StringUtils.isEmpty(redirectUrl)) {
            errorResponse(response, 400, 800003, "redirectUri必填");
            return false;
        }
        if (StringUtils.isEmpty(state)) {
            errorResponse(response, 400, 800003, "state必填");
            return false;
        }
        if (StringUtils.isEmpty(responseType)) {
            errorResponse(response, 400, 800003, "responseType必填");
            return false;
        } else if (!"code".equals(responseType)) {
            errorResponse(response, 400, 800003, "错误的responseType");
            return false;
        }
        return true;
    }

    private Boolean valid(String url){
        return true;
    }
}

