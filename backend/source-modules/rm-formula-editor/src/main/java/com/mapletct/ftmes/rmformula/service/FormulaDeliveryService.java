package com.mapletct.ftmes.rmformula.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.rmformula.api.DeliveryRequest;
import com.mapletct.ftmes.rmformula.domain.RmFormulaBusinessException;
import com.mapletct.ftmes.rmformula.repository.FormulaEditorRepository;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class FormulaDeliveryService {
    private static final Pattern REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{8,80}");
    private final FormulaEditorRepository repository;
    private final FormulaEditorService editorService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String deliveryUrl;
    private final String deliveryToken;
    private final boolean simulatorEnabled;
    private final String simulatorSecret;

    public FormulaDeliveryService(FormulaEditorRepository repository,
                                  FormulaEditorService editorService,
                                  RestTemplate restTemplate,
                                  ObjectMapper objectMapper,
                                  @Value("${rm-formula-editor.delivery-url:}") String deliveryUrl,
                                  @Value("${rm-formula-editor.delivery-token:}") String deliveryToken,
                                  @Value("${rm-formula-editor.simulator-enabled:false}") boolean simulatorEnabled,
                                  @Value("${rm-formula-editor.simulator-secret:disabled}") String simulatorSecret) {
        this.repository = repository;
        this.editorService = editorService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.deliveryUrl = text(deliveryUrl);
        this.deliveryToken = text(deliveryToken);
        this.simulatorEnabled = simulatorEnabled;
        this.simulatorSecret = text(simulatorSecret);
    }

    @Transactional
    public Map<String, Object> publish(String tenant, long formulaId, DeliveryRequest request) {
        String requestId = validateRequest(request);
        repository.lock(tenant + "|delivery-request|" + requestId);
        Map<String, Object> previous = repository.deliveryByRequest(tenant, requestId);
        if (!previous.isEmpty()) {
            if (((Number) previous.get("formulaId")).longValue() != formulaId) {
                throw new RmFormulaBusinessException(409, "投递请求编号属于其他配方");
            }
            return deliveryDetail(((Number) previous.get("id")).longValue(), true);
        }

        Map<String, Object> revision = repository.revision(formulaId, request.getRevisionId());
        if (revision.isEmpty()) {
            throw new RmFormulaBusinessException(409, "配方尚未形成可投递的 Web 修订版本");
        }
        Map<String, Object> formula = editorService.detail(formulaId);
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("contractVersion", "ft-mes.rm-formula.v1");
        payload.put("idempotencyKey", requestId);
        payload.put("revision", revision);
        payload.put("formula", formula);
        long deliveryId = repository.createDelivery(
                tenant, requestId, formulaId, ((Number) revision.get("id")).longValue(), deliveryUrl, json(payload));
        attempt(deliveryId, requestId, revision, payload);
        return deliveryDetail(deliveryId, false);
    }

    @Transactional
    public Map<String, Object> retry(long deliveryId) {
        repository.lock("formula-delivery|" + deliveryId);
        Map<String, Object> delivery = repository.delivery(deliveryId);
        if (delivery.isEmpty()) {
            throw new RmFormulaBusinessException(404, "投递记录不存在");
        }
        if ("ACKNOWLEDGED".equals(delivery.get("state"))) {
            return deliveryDetail(deliveryId, true);
        }
        long formulaId = ((Number) delivery.get("formulaId")).longValue();
        long revisionId = ((Number) delivery.get("revisionId")).longValue();
        Map<String, Object> revision = repository.revision(formulaId, revisionId);
        if (revision.isEmpty()) {
            throw new RmFormulaBusinessException(409, "投递对应的配方修订不存在");
        }
        Map<String, Object> payload = storedPayload(repository.deliveryPayload(deliveryId));
        attempt(deliveryId, String.valueOf(delivery.get("requestId")), revision, payload);
        return deliveryDetail(deliveryId, false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> delivery(long deliveryId) {
        Map<String, Object> delivery = repository.delivery(deliveryId);
        if (delivery.isEmpty()) {
            throw new RmFormulaBusinessException(404, "投递记录不存在");
        }
        return deliveryDetail(deliveryId, false);
    }

    private void attempt(long deliveryId, String requestId, Map<String, Object> revision, Map<String, Object> payload) {
        if (deliveryUrl.isEmpty()) {
            repository.recordDeliveryAttempt(
                    deliveryId, "CONFIG_REQUIRED", null, null, "未配置 RM_FORMULA_DELIVERY_URL");
            return;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Idempotency-Key", requestId);
        headers.set("X-ADP-Formula-Revision", String.valueOf(revision.get("id")));
        if (!deliveryToken.isEmpty()) {
            headers.setBearerAuth(deliveryToken);
        }
        if (simulatorEnabled && !simulatorSecret.isEmpty()) {
            headers.set("X-ADP-Simulator-Secret", simulatorSecret);
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    deliveryUrl, HttpMethod.POST, new HttpEntity<Map<String, Object>>(payload, headers), String.class);
            String body = response.getBody() == null ? "" : response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && acknowledged(body)) {
                repository.recordDeliveryAttempt(deliveryId, "ACKNOWLEDGED", response.getStatusCodeValue(), body, null);
            } else {
                repository.recordDeliveryAttempt(
                        deliveryId, "FAILED", response.getStatusCodeValue(), body, "Batch/DCS 未返回确认标识");
            }
        } catch (HttpStatusCodeException exception) {
            repository.recordDeliveryAttempt(deliveryId, "FAILED", exception.getRawStatusCode(),
                    exception.getResponseBodyAsString(), exception.getMessage());
        } catch (RestClientException exception) {
            repository.recordDeliveryAttempt(deliveryId, "FAILED", null, null, exception.getMessage());
        }
    }

    private boolean acknowledged(String body) {
        if (body == null || body.trim().isEmpty()) {
            return false;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(body, Map.class);
            Object status = result.get("status");
            Object code = result.get("code");
            return Boolean.TRUE.equals(result.get("accepted"))
                    || Boolean.TRUE.equals(result.get("success"))
                    || "ACK".equalsIgnoreCase(String.valueOf(status))
                    || "ACKNOWLEDGED".equalsIgnoreCase(String.valueOf(status))
                    || "200".equals(String.valueOf(code));
        } catch (IOException exception) {
            return false;
        }
    }

    private Map<String, Object> deliveryDetail(long deliveryId, boolean idempotent) {
        Map<String, Object> result = new LinkedHashMap<String, Object>(repository.delivery(deliveryId));
        List<Map<String, Object>> attempts = repository.deliveryAttempts(deliveryId);
        result.put("attemptHistory", attempts == null ? Collections.emptyList() : attempts);
        result.put("idempotent", idempotent);
        result.put("adapter", simulatorEnabled ? "TEST_SIMULATOR" : "EXTERNAL_HTTP");
        return result;
    }

    private static String validateRequest(DeliveryRequest request) {
        if (request == null || !REQUEST_ID.matcher(text(request.getRequestId())).matches()) {
            throw new RmFormulaBusinessException(400, "投递请求编号格式不正确");
        }
        return text(request.getRequestId());
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize formula delivery", exception);
        }
    }

    private Map<String, Object> storedPayload(String payloadJson) {
        if (payloadJson == null || payloadJson.trim().isEmpty()) {
            throw new RmFormulaBusinessException(409, "投递记录缺少原始版本报文");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);
            return payload;
        } catch (IOException exception) {
            throw new RmFormulaBusinessException(409, "投递记录的原始版本报文不可读取");
        }
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
