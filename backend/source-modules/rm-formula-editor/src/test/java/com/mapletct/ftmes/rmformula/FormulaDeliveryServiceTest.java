package com.mapletct.ftmes.rmformula;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.rmformula.api.DeliveryRequest;
import com.mapletct.ftmes.rmformula.repository.FormulaEditorRepository;
import com.mapletct.ftmes.rmformula.service.FormulaDeliveryService;
import com.mapletct.ftmes.rmformula.service.FormulaEditorService;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FormulaDeliveryServiceTest {
    @Mock
    private FormulaEditorRepository repository;
    @Mock
    private FormulaEditorService editorService;
    @Mock
    private RestTemplate restTemplate;
    private FormulaDeliveryService service;

    @Before
    public void setUp() {
        service = new FormulaDeliveryService(
                repository, editorService, restTemplate, new ObjectMapper(),
                "http://batch.example/formulas", "token", false, "disabled");
    }

    @Test
    public void recordsAcknowledgedHttpDelivery() {
        DeliveryRequest request = request("REQ-RM-DELIVERY-001");
        Map<String, Object> revision = revision(41L, 11L);
        Map<String, Object> delivery = delivery(51L, "ACKNOWLEDGED", 1);
        when(repository.deliveryByRequest("default", request.getRequestId())).thenReturn(Collections.emptyMap());
        when(repository.revision(11L, null)).thenReturn(revision);
        when(editorService.detail(11L)).thenReturn(formula(11L));
        when(repository.createDelivery(eq("default"), eq(request.getRequestId()), eq(11L), eq(41L),
                eq("http://batch.example/formulas"), any(String.class))).thenReturn(51L);
        when(restTemplate.exchange(eq("http://batch.example/formulas"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"accepted\":true,\"status\":\"ACKNOWLEDGED\"}"));
        when(repository.delivery(51L)).thenReturn(delivery);
        when(repository.deliveryAttempts(51L)).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.publish("default", 11L, request);

        assertEquals("ACKNOWLEDGED", result.get("state"));
        verify(repository).recordDeliveryAttempt(eq(51L), eq("ACKNOWLEDGED"), eq(200), any(String.class), eq(null));
    }

    @Test
    public void recordsFailedAttemptSoItCanBeRetried() {
        DeliveryRequest request = request("REQ-RM-DELIVERY-002");
        Map<String, Object> revision = revision(42L, 12L);
        Map<String, Object> delivery = delivery(52L, "FAILED", 1);
        when(repository.deliveryByRequest("default", request.getRequestId())).thenReturn(Collections.emptyMap());
        when(repository.revision(12L, null)).thenReturn(revision);
        when(editorService.detail(12L)).thenReturn(formula(12L));
        when(repository.createDelivery(eq("default"), eq(request.getRequestId()), eq(12L), eq(42L),
                eq("http://batch.example/formulas"), any(String.class))).thenReturn(52L);
        when(restTemplate.exchange(eq("http://batch.example/formulas"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpServerErrorException(
                        HttpStatus.SERVICE_UNAVAILABLE, "down", "retry".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        when(repository.delivery(52L)).thenReturn(delivery);
        when(repository.deliveryAttempts(52L)).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.publish("default", 12L, request);

        assertEquals("FAILED", result.get("state"));
        verify(repository).recordDeliveryAttempt(eq(52L), eq("FAILED"), eq(503), eq("retry"), any(String.class));
    }

    @Test
    public void retryUsesThePersistedVersionedPayload() {
        Map<String, Object> failed = delivery(53L, "FAILED", 1);
        Map<String, Object> acknowledged = delivery(53L, "ACKNOWLEDGED", 2);
        when(repository.delivery(53L)).thenReturn(failed, acknowledged);
        when(repository.revision(11L, 41L)).thenReturn(revision(41L, 11L));
        when(repository.deliveryPayload(53L)).thenReturn(
                "{\"contractVersion\":\"ft-mes.rm-formula.v1\",\"idempotencyKey\":\"REQ-RM-DELIVERY\","
                        + "\"revision\":{\"id\":41},\"formula\":{\"id\":11,\"formulaCode\":\"ORIGINAL\"}}");
        when(restTemplate.exchange(eq("http://batch.example/formulas"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"accepted\":true}"));
        when(repository.deliveryAttempts(53L)).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.retry(53L);

        assertEquals("ACKNOWLEDGED", result.get("state"));
        ArgumentCaptor<HttpEntity> entity = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq("http://batch.example/formulas"), eq(HttpMethod.POST), entity.capture(), eq(String.class));
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) entity.getValue().getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> formula = (Map<String, Object>) body.get("formula");
        assertEquals("ORIGINAL", formula.get("formulaCode"));
        verify(editorService, never()).detail(anyLong());
    }

    private static DeliveryRequest request(String requestId) {
        DeliveryRequest request = new DeliveryRequest();
        request.setRequestId(requestId);
        return request;
    }

    private static Map<String, Object> revision(long id, long formulaId) {
        Map<String, Object> revision = new LinkedHashMap<String, Object>();
        revision.put("id", id);
        revision.put("formulaId", formulaId);
        revision.put("revisionNo", 1);
        return revision;
    }

    private static Map<String, Object> formula(long id) {
        Map<String, Object> formula = new LinkedHashMap<String, Object>();
        formula.put("id", id);
        formula.put("formulaCode", "WEB-FORMULA");
        return formula;
    }

    private static Map<String, Object> delivery(long id, String state, int attempts) {
        Map<String, Object> delivery = new LinkedHashMap<String, Object>();
        delivery.put("id", id);
        delivery.put("formulaId", 11L);
        delivery.put("revisionId", 41L);
        delivery.put("requestId", "REQ-RM-DELIVERY");
        delivery.put("state", state);
        delivery.put("attempts", attempts);
        return delivery;
    }
}
