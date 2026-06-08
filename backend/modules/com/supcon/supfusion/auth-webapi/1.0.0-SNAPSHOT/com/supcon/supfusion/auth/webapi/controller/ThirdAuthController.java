package com.supcon.supfusion.auth.webapi.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.supcon.supfusion.auth.common.utils.HttpClientUtils;
import com.supcon.supfusion.auth.service.ThirdAuthService;
import com.supcon.supfusion.auth.service.bo.LoginResponseBO;
import com.supcon.supfusion.auth.webapi.vo.LoginResponseVO;
import com.supcon.supfusion.auth.webapi.vo.LoginVO;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@RestController
@InternalApi(path = "/inter-api/auth")
@Api(value = "第三方认证单点登录", tags = "认证中心")
@Validated
public class ThirdAuthController {

    @Resource
    ThirdAuthService thirdAuthService;

    @GetMapping("/v1/third/authorize")
    @ApiOperation(value = "第三方认证实现单点登录", httpMethod = "GET")
    public void authorize(@ApiParam(name = "code", value = "授权码", required = false)
                          @RequestParam(value = "code", required = false) String code,
                          @RequestParam(value = "state", required = false) String state,
                          @RequestParam(value = "ticket", required = false) String ticket,
                          @RequestParam(value = "protocolType", required = false) String protocolType,
                          @RequestHeader("X-Real-IP") String realIp,
                          HttpServletResponse response) throws IOException {


        log.info("第三方认证单点登录传参: code: {}, ticket: {},realIp: {}, protocolType {}", code, ticket, realIp,protocolType);
        // 实现第三方认证
        LoginResponseBO loginResponseBO = thirdAuthService.authorize(StringUtils.isEmpty(code) ? ticket : code, protocolType, realIp,state);
        // 实体转换
        LoginResponseVO loginResponseVO = new LoginResponseVO();
        BeanUtils.copyProperties(loginResponseBO, loginResponseVO);
        // 生成cookie信息,跳转指定路径到浏览器
        ResponseCookie cookie = ResponseCookie.from("suposTicket", loginResponseVO.getTicket()) // key & value
                .httpOnly(true)		// 禁止js读取
                .secure(false)		// 在http下也传输
                .path("/")			// path
                .sameSite("Lax")	// 大多数情况也是不发送第三方 Cookie，但是导航到目标网址的 Get 请求除外
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        String host = HttpClientUtils.getHostPort(loginResponseBO.getRedirectUri());
        log.info("host===={}",host);
        if (StringUtils.isEmpty(protocolType)) {
            response.sendRedirect(host + "/#/user/oauth" + "?ticket=" + loginResponseVO.getTicket());
        } else {
            if (loginResponseBO.getUserBind()) {
                response.sendRedirect(host + "/#/user/oauth" + "?ticket=" + loginResponseVO.getTicket());
            } else {
                response.sendRedirect(host + "/#/user/login" + "?userBind=" + false + "&state=" + state);
            }
        }

    }


    @GetMapping("/v1/query/userInfo")
    @ApiOperation(value = "通过ticket查询用户信息", httpMethod = "GET")
    public String queryCurrentUserInfo() {
        // 实现查询当前用户数据
        LoginResponseBO loginResponseBO = thirdAuthService.queryCurrentUserInfo();
        // 转换实体
        LoginResponseVO loginResponseVO = new LoginResponseVO();
        BeanUtils.copyProperties(loginResponseBO, loginResponseVO);
        return JSON.toJSONString(loginResponseVO, SerializerFeature.DisableCircularReferenceDetect);
    }

    @PostMapping(value = "/v1/user/third/identity/bind", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String login(@Validated @RequestBody LoginVO loginVO, @RequestHeader("X-Real-IP") String realIp, @RequestHeader(HttpHeaders.USER_AGENT) String userAgent, @CookieValue(name = "KC_RESTART", required = false) String cookieValue, HttpServletResponse response) throws IOException {
        LoginResponseBO login = thirdAuthService.thirdIdentityUserBind(loginVO.getUserName(), loginVO.getPassword(), realIp, loginVO.getState());
        LoginResponseVO loginResponseVO = new LoginResponseVO();
        BeanUtils.copyProperties(login, loginResponseVO);
        // 生成cookie信息,跳转指定路径到浏览器
        ResponseCookie cookie = ResponseCookie.from("suposTicket", loginResponseVO.getTicket()) // key & value
                .httpOnly(true)		// 禁止js读取
                .secure(false)		// 在http下也传输
                .path("/")			// path
                .sameSite("Lax")	// 大多数情况也是不发送第三方 Cookie，但是导航到目标网址的 Get 请求除外
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return JSON.toJSONString(loginResponseVO, SerializerFeature.DisableCircularReferenceDetect);
    }
}
