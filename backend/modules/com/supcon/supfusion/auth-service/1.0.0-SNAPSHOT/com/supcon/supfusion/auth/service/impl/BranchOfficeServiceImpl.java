package com.supcon.supfusion.auth.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.common.exception.UserErrorEnum;
import com.supcon.supfusion.auth.common.exception.UserException;
import com.supcon.supfusion.auth.common.utils.Base64Util;
import com.supcon.supfusion.auth.service.BranchOfficeService;
import com.supcon.supfusion.auth.service.UserService;
import com.supcon.supfusion.auth.service.bo.LoginBO;
import com.supcon.supfusion.auth.service.bo.LoginResponseBO;
import com.supcon.supfusion.auth.service.bo.UserBO;
import com.supcon.supfusion.auth.service.config.BranchOfficeProperties;
import com.supcon.supfusion.auth.service.config.HeadOfficeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.supcon.supfusion.auth.common.exception.UserErrorEnum.USER_PASSWORD_ERROR;

@Slf4j
@Service
public class BranchOfficeServiceImpl implements BranchOfficeService {
    @Autowired
    private BranchOfficeProperties branchOfficeProperties;
    @Autowired
    private HeadOfficeProperties headOfficeProperties;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private UserService userService;

    public UserService getUserService() {
        return userService;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void logout(String ticket) {
        // 删除已登录的分公司认证
        String key = String.format(Constants.HEAD_AUTH_TICKET_CODE_SET, ticket);
        Set<String> codes = stringRedisTemplate.opsForSet().members(key);
        if (codes != null && !codes.isEmpty()) {
            codes.add(key);
            stringRedisTemplate.delete(codes);
        }
        if (!branchOfficeProperties.getEnable()) {
            return;
        }
        // 获取ticket和code对应关系
        key = String.format(Constants.BRANCH_AUTH_TICKET_CODE, ticket);
        String code = stringRedisTemplate.opsForValue().get(key);
        if (code == null) {
            return;
        }
        // 根据code和clientSecret获取token
        StringBuilder url = new StringBuilder();
        url.append(headOfficeProperties.getAddress()).append(headOfficeProperties.getLogout())
                .append("?client_id=").append(branchOfficeProperties.getClientId())
                .append("&client_secret=").append(branchOfficeProperties.getClientSecret())
                .append("&code=").append(code);
        ResponseEntity<String> responseEntity = getRestTemplate().getForEntity(url.toString(), String.class);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return;
        }
        stringRedisTemplate.delete(key);
    }

    @Override
    public boolean refreshToken(String ticket) {
        if (!branchOfficeProperties.getEnable()) {
            return false;
        }
        // 获取ticket和code对应关系
        String key = String.format(Constants.BRANCH_AUTH_TICKET_CODE, ticket);
        String code = stringRedisTemplate.opsForValue().get(key);
        if (code == null) {
            return false;
        }
        // 根据code和clientSecret获取token
        StringBuilder url = new StringBuilder();
        url.append(headOfficeProperties.getAddress()).append(headOfficeProperties.getRefreshToken())
                .append("?client_id=").append(branchOfficeProperties.getClientId())
                .append("&client_secret=").append(branchOfficeProperties.getClientSecret())
                .append("&code=").append(code);
        ResponseEntity<String> responseEntity = getRestTemplate().getForEntity(url.toString(), String.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return responseEntity.getStatusCode() == HttpStatus.UNAUTHORIZED;
        }
        stringRedisTemplate.expire(key, 10, TimeUnit.MINUTES);
        return false;
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

    @Override
    public LoginResponseBO login(LoginBO loginBO, String realIp, String deviceType, String quatoName) {
        StringBuilder url = new StringBuilder();
        UserBO userBO = userService.findByUserName(loginBO.getUserName());
        if (userBO == null) {
            throw new UserException(UserErrorEnum.USER_NOT_EXIST);
        }
        url.append(headOfficeProperties.getAddress()).append(headOfficeProperties.getAuthorize())
                .append("?client_id=").append(branchOfficeProperties.getClientId())
                .append("&client_secret=").append(branchOfficeProperties.getClientSecret())
                .append("&username=").append(loginBO.getUserName())
                .append("&password=").append(Base64Util.encode(loginBO.getPassword()))
                .append("&grant_type=password");
        ResponseEntity<String> responseEntity = getRestTemplate().getForEntity(url.toString(), String.class);
        JSONObject jsonObject = JSONObject.parseObject(responseEntity.getBody());
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            userService.verifyLoginResult(jsonObject);
            throw new RuntimeException("系统错误");
        }
        String code = jsonObject.getString("code");
        Long companyId = Optional.ofNullable(userBO.getCurrentCompanyId()).orElse(userBO.getCompanyId());
        LoginResponseBO loginResponseBO = userService.simulateLogin(loginBO.getUserName(), companyId, realIp, quatoName, deviceType);
        String ticket = null;
        if (loginResponseBO == null || (ticket = loginResponseBO.getTicket()) == null) {
            throw new UserException(UserErrorEnum.USER_OR_PASSWORD_ERROR);
        }
        // 建立ticket和code对应关系
        String key = String.format(Constants.BRANCH_AUTH_TICKET_CODE, ticket);
        stringRedisTemplate.opsForValue().set(key, code, 10, TimeUnit.MINUTES);
        return loginResponseBO;
    }
}
