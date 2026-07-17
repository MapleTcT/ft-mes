package com.mapletct.ftmes.womentry.service;

import com.mapletct.ftmes.womentry.api.CreateInstructionRequest;
import com.mapletct.ftmes.womentry.domain.WomEntryBusinessException;
import com.mapletct.ftmes.womentry.support.RequestAuthContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WomUpstreamClient {

    private static final String CREATE_PATH =
        "/msService/WOM/produceTask/produceTask/produceTaskCreated2";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String upstreamBaseUrl;

    public WomUpstreamClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${wom-production-entry.upstream-base-url:http://gateway:8008}") String upstreamBaseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.upstreamBaseUrl = stripTrailingSlash(upstreamBaseUrl);
    }

    public Map<String, Object> create(CreateInstructionRequest request, RequestAuthContext authContext) {
        if (isBlank(authContext.getAuthorization()) && isBlank(authContext.getCookie())) {
            throw new WomEntryBusinessException(401, "登录状态已失效，请重新登录");
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("prodCode", request.getProductCode().trim());
        payload.put("formulaCode", request.getFormulaCode().trim());
        payload.put("workLineId", request.getWorkLineId());
        payload.put("planNum", request.getPlanNum());
        payload.put("planStartDate", request.getPlanStartDate().trim());
        payload.put("planEndDate", request.getPlanEndDate().trim());
        payload.put("batchCode", request.getBatchCode().trim());
        payload.put("needPack", Boolean.TRUE.equals(request.getNeedPack()));
        List<Map<String, Object>> body = new ArrayList<Map<String, Object>>();
        body.add(payload);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        copyHeader(headers, HttpHeaders.AUTHORIZATION, authContext.getAuthorization());
        copyHeader(headers, HttpHeaders.COOKIE, authContext.getCookie());
        copyHeader(headers, HttpHeaders.ACCEPT_LANGUAGE, authContext.getAcceptLanguage());
        copyHeader(headers, "X-Tenant-Id", authContext.getTenantId());

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                upstreamBaseUrl + CREATE_PATH,
                HttpMethod.POST,
                new HttpEntity<List<Map<String, Object>>>(body, headers),
                new ParameterizedTypeReference<Map<String, Object>>() { }
            );
            Map<String, Object> responseBody = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || responseBody == null) {
                throw new WomEntryBusinessException(502, "WOM 制造指令服务未返回有效结果");
            }
            int code = asInt(responseBody.get("code"));
            if (code != 200) {
                throw new WomEntryBusinessException(code > 0 ? code : 502, responseMessage(responseBody));
            }
            return responseBody;
        } catch (WomEntryBusinessException exception) {
            throw exception;
        } catch (HttpStatusCodeException exception) {
            throw new WomEntryBusinessException(
                exception.getRawStatusCode(),
                readErrorMessage(exception.getResponseBodyAsString()),
                exception
            );
        } catch (RestClientException exception) {
            throw new WomEntryBusinessException(502, "WOM 制造指令服务暂不可用", exception);
        }
    }

    private String readErrorMessage(String responseBody) {
        if (isBlank(responseBody)) {
            return "WOM 制造指令服务调用失败";
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(responseBody, Map.class);
            return responseMessage(payload);
        } catch (Exception ignored) {
            return responseBody.length() > 300 ? responseBody.substring(0, 300) : responseBody;
        }
    }

    private static String responseMessage(Map<String, Object> payload) {
        Object message = payload.get("message");
        if (message == null) {
            message = payload.get("msg");
        }
        String value = message == null ? "WOM 制造指令创建失败" : String.valueOf(message).trim();
        return value.isEmpty() ? "WOM 制造指令创建失败" : value;
    }

    private static int asInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static void copyHeader(HttpHeaders headers, String name, String value) {
        if (!isBlank(value)) {
            headers.set(name, value.trim());
        }
    }

    private static String stripTrailingSlash(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
