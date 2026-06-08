package com.supcon.supfusion.auth.webapi.controller;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.common.exception.UserException;
import com.supcon.supfusion.auth.common.utils.Base64Util;
import com.supcon.supfusion.auth.common.utils.MD5Generator;
import com.supcon.supfusion.auth.service.UserService;
import com.supcon.supfusion.auth.service.bo.LoginBO;
import com.supcon.supfusion.auth.service.cache.AuthTicketCache;
import com.supcon.supfusion.auth.service.config.BranchOfficeProperties;
import com.supcon.supfusion.auth.service.config.HeadOfficeProperties;
import com.supcon.supfusion.auth.webapi.vo.HeadOfficeLoginInfoVO;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.supcon.supfusion.auth.common.constants.Constants.*;


@Slf4j
@RestController
@OpenApi(path = "/inter-api/auth/v1")
public class HeadOfficeController extends BaseController {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private BranchOfficeProperties branchOfficeProperties;
    @Autowired
    private HeadOfficeProperties headOfficeProperties;
    @Autowired
    private AuthTicketCache authTicketCache;
    @Autowired
    private UserService userService;
    @Value("${integration.supos.enabled:true}")
    private Boolean supOSEnabled;

    @GetMapping("/head-office/authorize")
    public Map<String, Object> authorize(@RequestParam(value = "grant_type") String grantType,
                                @RequestParam(value = "client_id") String clientId,
                                @RequestParam(value = "client_secret", required = false) String clientSecret,
                                @RequestParam(value = "username", required = false) String userName,
                                @RequestParam(value = "password", required = false) String password,
                                @CookieValue(name = "suposTicket", required = false) String ticket,
                                @RequestParam(value = "state", required = false) String state,
                                @RequestParam(value = "redirect_uri", required = false) String redirectUri,
                                @RequestHeader(name = "X-Forwarded-Host",required = false) String host) throws Exception {
        HttpServletResponse response = getResponse();
        if ("code".equals(grantType)) {
            boolean verify = verifyForCodeAuthorize(redirectUri, state, clientId, response);
            if (!verify) {
                return null;
            }
            redirectUri = Base64Util.decode(redirectUri);
            String[] split = host.split(",");
            if (StringUtils.isNotEmpty(ticket)) {
                Map<Object, Object> userInfo = authTicketCache.getMapByTicket(ticket);
                if (!userInfo.isEmpty()) {
                    alreadyLogin(redirectUri, state, ticket, response);
                } else {
                    noLogin(redirectUri, state, clientId, response, split[0]);
                }
            } else {
                noLogin(redirectUri, state, clientId, response, split[0]);
            }
            return null;
        }
        if ("password".equals(grantType)) {
            boolean verify = verifyForPasswordAuthorize(clientId, clientSecret, userName, password, response);
            if (!verify) {
                return null;
            }
            password = Base64Util.decode(password);
            LoginBO loginBO = new LoginBO();
            loginBO.setUserName(userName);
            loginBO.setPassword(password);
            loginBO.setLdap(false);
            loginBO.setGrantType("password");
            try {
                JSONObject jsonObject = userService.loginKeycloak(loginBO, null, "pc");
                userService.verifyLoginResult(jsonObject);
                if (!jsonObject.containsKey(Constants.ACCESS_TOKEN)) {
                    throw new RuntimeException("系统错误");
                }

                String code = MD5Generator.getInstance().generateValue();
                ticket = UUID.randomUUID().toString().replace("-", "");
                HashMap<String, String> map = new HashMap<>();
                map.put("ticket", ticket);
                stringRedisTemplate.opsForHash().putAll(code, map);
                stringRedisTemplate.expire(code, 10, TimeUnit.MINUTES);

                Map<String, Object> result = new HashMap<>();
                result.put("code", code);

                String key = String.format(AUTH_TICKET, ticket);
                stringRedisTemplate.opsForHash().putAll(key, result);
                stringRedisTemplate.expire(key, 10, TimeUnit.MINUTES);

                return result;
            } catch (UserException userException) {
                ErrorDefinition errorDefinition = userException.getErrorDefinition();
                errorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, errorDefinition.getCode(), errorDefinition.getMessage());
            } catch (Exception e) {
                errorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 800003, e.getMessage());
            }
            return null;
        }
        errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "不支持的类型:" + grantType);
        return null;
    }

    private boolean verifyForPasswordAuthorize(String clientId, String clientSecret, String userName, String password, HttpServletResponse response) {
        if (StringUtils.isEmpty(clientSecret)) {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "client_secret必填");
            return false;
        }
        if (StringUtils.isEmpty(userName)) {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "username必填");
            return false;
        }
        if (StringUtils.isEmpty(password)) {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "password必填");
            return false;
        }
        // 判断client是否存在
        boolean isClientExist = headOfficeProperties.getClients().stream().anyMatch(client ->
                client.getClientId().equals(clientId)
        );
        if (!isClientExist) {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "无效的client_id");
            return false;
        }
        return true;
    }


    @GetMapping("/head-office/login-info")
    public HeadOfficeLoginInfoVO loginInfo(@RequestParam("client_id") String clientId,
                                           @RequestParam("client_secret") String clientSecret,
                                           @RequestParam("code") String code) {
        HttpServletResponse response = getResponse();
        boolean verify = verifyForToken(clientId, clientSecret, code, response);
        if (!verify) {
            return null;
        }
        String ticket = (String) stringRedisTemplate.opsForHash().get(code, Constants.TICKET);
        if (ticket == null) {
            errorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 800003, "无效的code");
            return null;
        }
        Map<Object, Object> loginInfo = authTicketCache.getMapByTicket(ticket);
        String userName = (String) loginInfo.get(Constants.USER_NAME);
        String companyCode = (String) loginInfo.get(Constants.COMPANY_CODE);
        HeadOfficeLoginInfoVO headOfficeLoginInfoVO = new HeadOfficeLoginInfoVO();
        headOfficeLoginInfoVO.setUserName(userName);
        headOfficeLoginInfoVO.setCompanyCode(companyCode);
        return headOfficeLoginInfoVO;
    }

    @GetMapping("/head-office/logout")
    public void logout(@RequestParam("client_id") String clientId,
                                           @RequestParam("client_secret") String clientSecret,
                                           @RequestParam("code") String code) {
        HttpServletResponse response = getResponse();
        boolean verify = verifyForToken(clientId, clientSecret, code, response);
        if (!verify) {
            return;
        }
        String ticket = (String) stringRedisTemplate.opsForHash().get(code, Constants.TICKET);
        if (ticket == null) {
            errorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 800003, "无效的code");
            return;
        }
        userService.logout(ticket);
    }

    @GetMapping("/head-office/refresh-token")
    public void refreshToken(@RequestParam("client_id") String clientId,
                       @RequestParam("client_secret") String clientSecret,
                       @RequestParam("code") String code) {
        HttpServletResponse response = getResponse();
        boolean verify = verifyForToken(clientId, clientSecret, code, response);
        if (!verify) {
            return;
        }
        String ticket = (String) stringRedisTemplate.opsForHash().get(code, Constants.TICKET);
        if (ticket == null) {
            errorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 800003, "无效的code");
            return;
        }
        userService.refreshToken(ticket, getResponse());
        stringRedisTemplate.expire(code, 10, TimeUnit.MINUTES);

        String key = String.format(AUTH_TICKET, ticket);
        stringRedisTemplate.expire(key, 10, TimeUnit.MINUTES);
    }

    private boolean verifyForToken(String clientId, String clientSecret, String code, HttpServletResponse response) {
        if (StringUtils.isEmpty(clientId)) {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "client_id必填");
            return false;
        }
        if (StringUtils.isEmpty(clientSecret)) {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "client_secret必填");
            return false;
        }
        if (StringUtils.isEmpty(code)) {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "code必填");
            return false;
        }
        // 判断client是否存在
        boolean isClientExist = headOfficeProperties.getClients().stream().anyMatch(client ->
                client.getClientId().equals(clientId) && client.getClientSecret().equals(clientSecret)
        );
        if (!isClientExist) {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "无效的client_id");
            return false;
        }
        return true;
    }

    private void noLogin(String redirectUri, String state, String clientId, HttpServletResponse response, String host) throws IOException {
        // 判断当前会话是否已经认证，如果未认证，跳转到认证页面
        response.setStatus(HttpServletResponse.SC_FOUND);
        HttpServletRequest request = getRequest();
        String proto = Optional.ofNullable(request.getHeader("x-forwarded-proto")).orElse("http");
        String headBranchOfficeCode = UUID.randomUUID().toString();
        // 容器化部署和windows部署登陆页面有区别
        String url = proto + "://" + host + (supOSEnabled ? "/#/user/login" : "/login.html") + "?" + HEAD_BRANCH_OFFICE_CODE + "=" + headBranchOfficeCode;
        response.sendRedirect(url);
        HashMap<String, String> map = new HashMap<>();
        map.put("redirectUri", redirectUri);
        map.put("state", state);
        map.put("clientId", clientId);
        stringRedisTemplate.opsForHash().putAll(headBranchOfficeCode, map);
        stringRedisTemplate.expire(headBranchOfficeCode, 10, TimeUnit.MINUTES);
    }

    private void alreadyLogin(String redirectUri, String state, String ticket, HttpServletResponse response) throws Exception {
        String code = MD5Generator.getInstance().generateValue();
        HashMap<String, String> map = new HashMap<>();
        map.put("ticket", ticket);
        map.put("state", state);
        stringRedisTemplate.opsForHash().putAll(code, map);
        stringRedisTemplate.expire(code, 10, TimeUnit.MINUTES);
        if (redirectUri.contains("?")) {
            redirectUri = redirectUri + "&code=" + code + "&state=" + state;
        }else {
            redirectUri = redirectUri + "?code=" + code + "&state=" + state;
        }
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.sendRedirect(redirectUri);
    }

    private boolean verifyForCodeAuthorize(String redirectUrl, String state, String clientId, HttpServletResponse response) {
        if (StringUtils.isEmpty(redirectUrl)) {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "redirectUri必填");
            return false;
        }
        if (StringUtils.isEmpty(state)) {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "state必填");
            return false;
        }
        // 判断client是否存在
        boolean isClientExist = headOfficeProperties.getClients().stream().anyMatch(client ->
                client.getClientId().equals(clientId)
        );
        if (!isClientExist) {
            errorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 800003, "无效的client_id");
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
