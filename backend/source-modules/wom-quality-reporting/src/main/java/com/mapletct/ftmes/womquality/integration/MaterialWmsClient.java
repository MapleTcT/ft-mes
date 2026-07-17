package com.mapletct.ftmes.womquality.integration;

import com.mapletct.ftmes.womquality.domain.WomQualityBusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MaterialWmsClient {

    private final RestTemplate restTemplate;
    private final String endpoint;

    public MaterialWmsClient(@Value("${wom.quality.wms-url}") String endpoint) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(requestFactory);
        this.endpoint = endpoint;
    }

    public Map<String, Object> apply(String tenantId, Map<String, Object> report, String action) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        String reportId = id(report.get("id"));
        payload.put("requestId", reportId + ":" + action);
        payload.put("action", action);
        payload.put("qualityReportId", reportId);
        payload.put("taskId", id(report.get("task_id")));
        payload.put("sourceLineId", id(report.get("source_output_id")));
        payload.put("totalQuantity", decimal(report.get("reported_quantity")));
        payload.put("goodQuantity", decimal(report.get("good_quantity")));
        payload.put("badQuantity", decimal(report.get("bad_quantity")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-Id", tenantId);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                endpoint, new HttpEntity<Map<String, Object>>(payload, headers), Map.class);
            Map<?, ?> body = response.getBody();
            int code = body == null || body.get("code") == null
                ? response.getStatusCodeValue() : Integer.parseInt(String.valueOf(body.get("code")));
            if (!response.getStatusCode().is2xxSuccessful() || code != 200) {
                Object message = body == null ? null : body.get("message");
                throw new WomQualityBusinessException(
                    code == 200 ? 502 : code,
                    "WMS 数量分配失败: " + (message == null ? response.getStatusCodeValue() : message));
            }
            Object data = body.get("data");
            if (data instanceof Map) {
                Map<String, Object> result = new LinkedHashMap<String, Object>();
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) data).entrySet()) {
                    result.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                return result;
            }
            return new LinkedHashMap<String, Object>();
        } catch (WomQualityBusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new WomQualityBusinessException(502, "WMS 数量分配接口不可用: " + exception.getMessage());
        }
    }

    private static String id(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            throw new WomQualityBusinessException(500, "WMS 同步缺少来源标识");
        }
        return String.valueOf(value);
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(String.valueOf(value));
    }
}
