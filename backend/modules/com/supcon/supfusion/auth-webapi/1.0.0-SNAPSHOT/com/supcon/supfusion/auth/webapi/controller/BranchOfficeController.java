package com.supcon.supfusion.auth.webapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.common.useragent.UserAgent;
import com.supcon.supfusion.auth.common.useragent.UserAgentUtil;
import com.supcon.supfusion.auth.manager.PersonServiceAdapter;
import com.supcon.supfusion.auth.service.BranchOfficeService;
import com.supcon.supfusion.auth.service.UserService;
import com.supcon.supfusion.auth.service.bo.LoginResponseBO;
import com.supcon.supfusion.auth.service.config.BranchOfficeProperties;
import com.supcon.supfusion.auth.service.config.HeadOfficeProperties;
import com.supcon.supfusion.auth.webapi.vo.HeadOfficeLoginInfoVO;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@Slf4j
@RestController
@OpenApi(path = "/inter-api/auth/v1")
public class BranchOfficeController extends BaseController {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private BranchOfficeProperties branchOfficeProperties;
    @Autowired
    private HeadOfficeProperties headOfficeProperties;
    @Autowired
    private UserService userService;
    @Resource
    private PersonServiceAdapter personServiceAdapter;
    @Value("${integration.supos.enabled:true}")
    private Boolean supOSEnabled;
    @Autowired
    private BranchOfficeService branchOfficeService;

    @GetMapping("/branch-office/authorize/callback")
    public void callback(@RequestParam String code,
                         @RequestParam String state,
                         @RequestHeader("X-Real-IP") String realIp,
                         @RequestHeader(HttpHeaders.USER_AGENT) String userAgent) throws Exception {
        HttpServletResponse response = getResponse();
        // 根据code和clientSecret获取token
        StringBuilder getTokenUrl = new StringBuilder();
        getTokenUrl.append(headOfficeProperties.getAddress()).append(headOfficeProperties.getLoginInfo())
                .append("?client_id=").append(branchOfficeProperties.getClientId())
                .append("&client_secret=").append(branchOfficeProperties.getClientSecret())
                .append("&code=").append(code);
        ResponseEntity<HeadOfficeLoginInfoVO> responseEntity = getRestTemplate().getForEntity(getTokenUrl.toString(), HeadOfficeLoginInfoVO.class);
        if (responseEntity.getStatusCode() != HttpStatus.OK
                || Optional.ofNullable(responseEntity.getBody()).map(o -> o.getUserName() == null).orElse(true)) {
            errorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 800003, "无效的code");
            return;
        }
        // 模拟认证
        HeadOfficeLoginInfoVO headOfficeLoginInfoVO = responseEntity.getBody();
        UserAgent userAgentInfo = UserAgentUtil.parse(userAgent);
        String quatoName = "";
        String deviceType = "";
        if (userAgentInfo.isMobile()) {
            quatoName = "MAX_MOBILE_LOGIN";
            deviceType = "mobile";
        } else {
            quatoName = "MAX_PC_LOGIN";
            deviceType = "pc";
        }
        String companyCode = headOfficeLoginInfoVO.getCompanyCode();
        Long companyId = personServiceAdapter.getCompanyIdByCode(companyCode);
        if (companyId == null) {
            errorResponse(getResponse(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 800003, "can't find company [code=" + companyCode + "]");
            return;
        }
        LoginResponseBO loginResponseBO = userService.simulateLogin(headOfficeLoginInfoVO.getUserName(), companyId, realIp, quatoName, deviceType);
        String ticket = null;
        if (loginResponseBO == null || (ticket = loginResponseBO.getTicket()) == null) {
            return;
        }
        // 建立ticket和code对应关系
        String key = String.format(Constants.BRANCH_AUTH_TICKET_CODE, ticket);
        stringRedisTemplate.opsForValue().set(key, code, 10, TimeUnit.MINUTES);
        // 根据state获取原请求路径
        String originUrl = stringRedisTemplate.opsForValue().get(state);
        if (originUrl == null) {
            originUrl = "/";
        }
        stringRedisTemplate.delete(state);
        // 携带token重定向到跳转url
        String ssoUri = "/greenDill/static/license/sso.html?suposTicket=" + ticket + "&redirectUri=" + URLEncoder.encode(originUrl, "UTF-8");
        response.sendRedirect(ssoUri);
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

    @GetMapping("/branch-office/authorize/login")
    public void login(@RequestParam(required = false) String redirectUrl,
                      @RequestHeader("x-forwarded-proto") String proto,
                      @RequestHeader("x-forwarded-host") String host) throws Exception {
        HttpServletResponse response = getResponse();
        String url = proto + "://" + host;
        if (StringUtils.isNotEmpty(redirectUrl) && !"null".equals(redirectUrl)) {
            url += redirectUrl;
        }
        // 容器化部署和windows部署登陆页面有区别
        String redirectUri = headOfficeProperties.getAddress() + (supOSEnabled ? "/#/user/login" : "/login.html") + "?redirectUrl=" + URLEncoder.encode(url,"UTF-8");
        response.sendRedirect(redirectUri);
    }

    private RestTemplate getRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM));
        restTemplate.getMessageConverters().add(mappingJackson2HttpMessageConverter);
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse clientHttpResponse) throws IOException {
                return true;
            }

            @Override
            public void handleError(ClientHttpResponse clientHttpResponse) throws IOException {

            }
        });
        return restTemplate;
    }
}
