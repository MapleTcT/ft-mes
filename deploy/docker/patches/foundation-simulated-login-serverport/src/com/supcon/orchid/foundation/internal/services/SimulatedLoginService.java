package com.supcon.orchid.foundation.internal.services;

import com.alibaba.fastjson.JSONObject;
import com.supcon.orchid.foundation.config.FeignConfiguration;
import com.supcon.orchid.services.BAPException;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.security.factory.UserContextFactory;
import com.supcon.supfusion.framework.cloud.security.pojo.JwtUser;
import com.supcon.supfusion.framework.cloud.security.util.JwtTokenUtil;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class SimulatedLoginService {
    private static final Logger logger = LoggerFactory.getLogger(SimulatedLoginService.class);
    private static final String AUTH_SERVICE_ID = "auth";
    private static final String AUTH_SIMULATED_LOGIN_URL = "/service-api/auth/v1/authentication/simulated-login";
    private static final String DEFAULT_TENANT_ID = "dt";
    private static final String DEFAULT_SIMULATED_LOGIN_JWT_PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtkvoXs7oFBaeUS+I6Ha1SFbz4owQP4YZ0af/ujDAj0BWbHBbGQXbOwSr/Kw6eFPzxz2BfQTvrtaAtAoo5//ZS6S8wsGfXnwSkkXyMYrye+OJfsCGHv0FfSbSvfs6Agp+8E9A/ScB/fL/kMvvVxvr+1LeXr8Kc4R5woydaHto4CjD6ix7Jbfaq+UVS7RT2TE+TGBjpbcUfxceygVaX8lt7s48z0dcwg8gJEk4MwIVCC5iA44tZS3bXBLBUaZsx4VC5dK+4c5PXNDADWHXKF2w+U70MoB5vSR5RzADWO5slQf2h3Vt2Hb7cFPdhGBoWzrTfmdzRM+Kii8bfLxrYpz2WQIDAQAB";

    @Autowired(required = false)
    private LoadBalancerClient loadBalancerClient;

    @Autowired(required = false)
    private JwtTokenUtil jwtTokenUtil;

    @Value("${supfusion.cloud.jwt.header:Authorization}")
    private String jwtHeader;

    @Value("${supfusion.cloud.jwt.tokenHead:Bearer}")
    private String jwtTokenHead;

    @Value("${serviceCode:}")
    private String serviceCode;

    @Value("${server.address:}")
    private String serverAddress;

    @Value("${server.port:}")
    private Integer serverPort;

    @Value("${adp.simulated-login.jwt.fallback-enabled:true}")
    private Boolean simulatedLoginJwtFallbackEnabled;

    @Value("${adp.simulated-login.jwt.public-key:" + DEFAULT_SIMULATED_LOGIN_JWT_PUBLIC_KEY + "}")
    private String simulatedLoginJwtPublicKey;

    public JwtUser login(String username, Long companyId) {
        String tenantId = RpcContext.getContext().getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            tenantId = DEFAULT_TENANT_ID;
            RpcContext.getContext().setTenantId(DEFAULT_TENANT_ID);
        }
        return login(username, companyId, tenantId);
    }

    @SuppressWarnings("unchecked")
    public JwtUser login(String username, Long companyId, String tenantId) {
        ServiceInstance authServiceInstance = loadBalancerClient.choose(AUTH_SERVICE_ID);
        if (authServiceInstance == null) {
            throw new BAPException("Simulated login failed, because cannot access auth service!");
        }

        String authUrl = String.format(
                "http://%s:%d%s?username={username}&companyId={companyId}&serviceCode={serviceCode}&serverAddress={serverAddress}&serverPort={serverPort}",
                authServiceInstance.getHost(),
                authServiceInstance.getPort(),
                AUTH_SIMULATED_LOGIN_URL);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("X-Tenant-Id", tenantId);
        HttpEntity<Object> httpEntity = new HttpEntity<>(httpHeaders);

        Map<String, String> params = new HashMap<>(16);
        params.put("username", username);
        params.put("companyId", companyId.toString());
        params.put("serviceCode", serviceCode);
        params.put("serverAddress", serverAddress);
        params.put("serverPort", serverPort == null ? "" : serverPort.toString());

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        RestTemplate restTemplate = new RestTemplate(factory);
        RequestCallback requestCallback = restTemplate.httpEntityCallback(httpEntity, JSONObject.class);
        ResponseExtractor<ResponseEntity<JSONObject>> responseExtractor = restTemplate.responseEntityExtractor(JSONObject.class);
        ResponseEntity<JSONObject> responseEntity = restTemplate.execute(
                authUrl, HttpMethod.GET, requestCallback, responseExtractor, params);

        JSONObject body = responseEntity.getBody();
        String responseBody = body == null ? "request auth failed!" : body.toJSONString();
        if (!responseEntity.getStatusCode().equals(HttpStatus.OK) || body == null) {
            logger.error("Simulated login failed: " + responseBody);
            throw new BAPException("Simulated login failed, because cannot get access token! Because: " + responseBody);
        }

        JSONObject jsonObject = responseEntity.getBody();
        String accessToken = jsonObject.getString("accessToken");
        if (StringUtils.isEmpty(accessToken)) {
            logger.error("Simulated login failed: " + responseBody);
            throw new BAPException("Simulated login failed, because cannot get access token!");
        }

        JwtUser jwtUser = parseJwtUser(accessToken);
        if (jwtUser == null) {
            logger.error("Simulated login failed: " + responseBody);
            throw new BAPException("Simulated login failed, because parse access token failed!");
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(jwtUser, null, jwtUser.getAuthorities());
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            authentication.setDetails((new WebAuthenticationDetailsSource()).buildDetails(request));
            Map<String, String> feignHeaders = (Map<String, String>) request.getAttribute(FeignConfiguration.FEIGN_HEADERS);
            if (feignHeaders == null) {
                feignHeaders = new HashMap<>(2);
            }
            feignHeaders.put(jwtHeader, jwtTokenHead + " " + accessToken);
            request.setAttribute(FeignConfiguration.FEIGN_HEADERS, feignHeaders);
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserContextFactory.create(jwtUser);
        logger.info("authenticated user " + jwtUser.getUsername() + ", setting security context");
        return jwtUser;
    }

    private JwtUser parseJwtUser(String accessToken) {
        JwtUser jwtUser = jwtTokenUtil == null ? null : jwtTokenUtil.getJwtUserFromToken(accessToken);
        if (jwtUser != null || !Boolean.TRUE.equals(simulatedLoginJwtFallbackEnabled)
                || StringUtils.isBlank(simulatedLoginJwtPublicKey)) {
            return jwtUser;
        }
        try {
            JwtTokenUtil fallbackJwtTokenUtil = new JwtTokenUtil();
            setField(fallbackJwtTokenUtil, "symmetric", Boolean.FALSE);
            setField(fallbackJwtTokenUtil, "secret", simulatedLoginJwtPublicKey);
            JwtUser fallbackJwtUser = fallbackJwtTokenUtil.getJwtUserFromToken(accessToken);
            if (fallbackJwtUser != null) {
                logger.warn("Simulated login token parsed with fallback public key. Align auth simulated-login signing key before production use.");
            }
            return fallbackJwtUser;
        } catch (Exception ex) {
            logger.warn("Simulated login fallback token parsing failed.", ex);
            return null;
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = JwtTokenUtil.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
