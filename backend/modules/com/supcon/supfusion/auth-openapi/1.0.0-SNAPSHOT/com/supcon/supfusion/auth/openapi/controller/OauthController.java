package com.supcon.supfusion.auth.openapi.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.common.utils.BijectionUtils;
import com.supcon.supfusion.auth.common.utils.MD5Generator;
import com.supcon.supfusion.auth.manager.KeycliandAdminClient;
import com.supcon.supfusion.auth.manager.feign.client.TokenClient;
import com.supcon.supfusion.auth.openapi.vo.OauthTokenVO;
import com.supcon.supfusion.auth.openapi.vo.OauthVO;
import com.supcon.supfusion.auth.openapi.vo.RegisterOauthClientVO;
import com.supcon.supfusion.auth.service.IdentityCenterConfigService;
import com.supcon.supfusion.auth.service.OnlineUserService;
import com.supcon.supfusion.auth.service.UserService;
import com.supcon.supfusion.auth.service.bo.AuthorizationBO;
import com.supcon.supfusion.auth.service.bo.LoginResponseBO;
import com.supcon.supfusion.auth.service.bo.RegisterOauthClientBo;
import com.supcon.supfusion.auth.service.cache.AuthTicketCache;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.supcon.supfusion.auth.common.constants.Constants.*;

@Slf4j
@RestController
@OpenApi(path = "/open-api/auth/v2")
public class OauthController extends BaseController {
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
    @Autowired
    IdentityCenterConfigService identityCenterConfigService;


    @PostMapping("/oauth2/token")
    public OauthTokenVO getToken(@RequestBody OauthVO vo, @RequestHeader(name = "Authorization", required = false) String accessToken,
                                 HttpServletResponse response) throws Exception {
        boolean verify = verify(vo, accessToken, response);
        if (!verify) {
            return null;
        }
        OauthTokenVO tokenRespon = new OauthTokenVO();
        if ("authorization_code".equals(vo.getGrantType())) {
            LoginResponseBO loginResponseBO = getLoginResponseBO(vo, response, tokenRespon);
            if (loginResponseBO == null) {
                return null;
            }
            buildUserInfo(tokenRespon, "Bearer", loginResponseBO.getAccessToken());
            return tokenRespon;
        } else if ("refresh_token".equals(vo.getGrantType())) {
            accessToken = accessToken.split(Constants.SPACE_CHAR)[1];
            AuthorizationBO authorizationBO = getAuthorizationBO(accessToken, vo.getRefreshToken(), tokenRespon, response);
            if (authorizationBO == null) {
                return null;
            }
            buildUserInfo(tokenRespon, "Bearer", authorizationBO.getAccessToken());
            return tokenRespon;
        } else {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 100106500, "无效grant_type)");
            return null;
        }
    }

    @ApiOperation("oauth client　第三方注册")
    @PostMapping("/oauth2/identity/provider/instances")
    public JSONObject oauthRegister(@RequestBody @Valid RegisterOauthClientVO vo, @RequestHeader(name = "X-Forwarded-Host") String host) {
        RegisterOauthClientBo registerOauthClientBo = BijectionUtils.apply(vo, RegisterOauthClientBo::new);

        String redirectUri = identityCenterConfigService.registerOauth2Client(registerOauthClientBo, host.split(",")[0]);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("redirectUri", redirectUri);
        return jsonObject;
    }


    private AuthorizationBO getAuthorizationBO(String accessToken, String refreshToken, OauthTokenVO tokenRespon, HttpServletResponse response) throws Exception {
        String bindAccountKey = MD5Generator.getInstance().generateValue(accessToken);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(bindAccountKey);
        String ticket = (String) entries.get("ticket");
        Map<Object, Object> authorizationMap = authTicketCache.getMapByTicket(ticket);
        if (authorizationMap.isEmpty()) {
            errorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 100106403, "无效accessToken");
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
            errorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 100106403, "无效refreshToken");
            return null;
        }
        String authenticateJSON = tokenClient.refreshToken(RpcContext.getContext().getTenantId(), map, tokenType + " " + accessTokenCache);
        if (authenticateJSON.contains("error")) {
            errorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 100106403, "无效accessToken");
            return null;
        }
        AuthorizationBO authorizationBO = JSONObject.parseObject(authenticateJSON, AuthorizationBO.class);
        if (companyId != null) {
            authorizationBO.setCompanyId(Long.valueOf(companyId));
        }
        authorizationBO.setClientId(client_id);
        authorizationBO.setTenantId(RpcContext.getContext().getTenantId());
        if (!StringUtils.isEmpty(userName)) {
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
            errorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 100106403, "无效code");
            return null;
        }
        String loginTicket = (String) entries.get("ticket");
        LoginResponseBO loginResponseBO = userService.accessToken(loginTicket);
        if (loginResponseBO == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            errorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 100106403, "无效code");
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
        if (!StringUtils.isEmpty(backLogoutUrl)) {
            list = JSON.parseArray(backLogoutUrl, String.class);
            if (!StringUtils.isEmpty(vo.getLogoutUri())) {
                list.add(vo.getLogoutUri());
            }
        } else {
            list = new ArrayList<>();
            if (!StringUtils.isEmpty(vo.getLogoutUri())) {
                list.add(vo.getLogoutUri());
            }
        }
        stringRedisTemplate.opsForHash().put(String.format(TENANT_TICKET, loginTicket), BACKEND_LOGOUT_URL, JSON.toJSONString(list));
        stringRedisTemplate.opsForHash().put(String.format(TENANT_TICKET, loginTicket), OAUTH2_TOKEN, loginResponseBO.getAccessToken());
        stringRedisTemplate.delete(vo.getCode());
        return loginResponseBO;
    }

    private void buildUserInfo(OauthTokenVO tokenRespon, String bearer, String accessToken2) {
        String userInfo = tokenClient.getUserInfo(RpcContext.getContext().getTenantId(), bearer + " " + accessToken2);
        JSONObject jsonObject = JSON.parseObject(userInfo);
        tokenRespon.setCompanyCode(jsonObject.getString("company_code"));
        tokenRespon.setPersonCode(jsonObject.getString("staff_code"));
        tokenRespon.setUsername(jsonObject.getString("user_name"));
        tokenRespon.setUserType(jsonObject.getIntValue("user_type"));
    }


    private boolean verify(OauthVO vo, String accessToken, HttpServletResponse response) {
        if ("authorization_code".equals(vo.getGrantType())) {
            if (StringUtils.isEmpty(vo.getCode())) {
                errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 100106500, "code必填");
                return false;
            }
            if (StringUtils.isEmpty(vo.getLogoutUri())) {
                errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 100106500, "logoutUri必填");
                return false;
            }
        } else if ("refresh_token".equals(vo.getGrantType())) {
            if (StringUtils.isEmpty(vo.getRefreshToken())) {
                errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 100106500, "refreshToken必填");
                return false;
            }
            if (StringUtils.isEmpty(accessToken)) {
                errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 100106500, "Authorization必填");
                return false;
            }
        } else {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 100106500, "无效grantType");
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
}
