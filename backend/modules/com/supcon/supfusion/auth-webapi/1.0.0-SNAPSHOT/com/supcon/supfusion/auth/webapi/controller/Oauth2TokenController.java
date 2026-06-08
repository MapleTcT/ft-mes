package com.supcon.supfusion.auth.webapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.common.utils.MD5Generator;
import com.supcon.supfusion.auth.manager.KeycliandAdminClient;
import com.supcon.supfusion.auth.manager.feign.client.TokenClient;
import com.supcon.supfusion.auth.service.OnlineUserService;
import com.supcon.supfusion.auth.service.UserService;
import com.supcon.supfusion.auth.service.cache.AuthTicketCache;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@InternalApi(path = "/inter-api/auth")
public class Oauth2TokenController extends BaseController {
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

    @GetMapping("/v1/oauth2/authorize")
    public void buildGet(@RequestParam(name = "redirectUri", required = false) String redirectUrl,
                         @RequestParam(name = "state", required = false) String state,
                         @RequestParam(name = "responseType", required = false) String responseType,
                         @CookieValue(name = "suposTicket", required = false) String ticket,
                         @RequestHeader(name="X-Forwarded-Host") String host,
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

    private void alreadyLogin(String redirectUrl, String state, String ticket, HttpServletResponse response) throws Exception {
        String code = MD5Generator.getInstance().generateValue();
        HashMap<String, String> map = new HashMap<>();
        map.put("ticket", ticket);
        map.put("state", state);
        stringRedisTemplate.opsForHash().putAll(code, map);
        stringRedisTemplate.expire(code, 10, TimeUnit.MINUTES);
        response.setStatus(HttpServletResponse.SC_FOUND);
        if (redirectUrl.contains("?")) {
            response.sendRedirect(redirectUrl + "&code=" + code + "&state=" + state);
        } else {
            response.sendRedirect(redirectUrl + "?code=" + code + "&state=" + state);
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
            errorResponse(response, 400, 100106500, "redirectUri必填");
            return false;
        }
        if (StringUtils.isEmpty(state)) {
            errorResponse(response, 400, 100106500, "state必填");
            return false;
        }
        if (StringUtils.isEmpty(responseType)) {
            errorResponse(response, 400, 100106500, "responseType必填");
            return false;
        } else if (!"code".equals(responseType)) {
            errorResponse(response, 400, 100106500, "错误的responseType");
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
