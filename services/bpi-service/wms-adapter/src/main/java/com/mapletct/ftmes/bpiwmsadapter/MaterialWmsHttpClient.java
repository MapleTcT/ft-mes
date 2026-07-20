package com.mapletct.ftmes.bpiwmsadapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MaterialWmsHttpClient implements MaterialWmsGateway {

    private static final int MAX_RESPONSE_BYTES = 1_048_576;
    private static final String SOURCE_SYSTEM = "BPI";
    private static final String API_KEY_HEADER = "X-BPI-WMS-Key";
    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final BpiWmsAdapterProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Autowired
    public MaterialWmsHttpClient(
            BpiWmsAdapterProperties properties,
            ObjectMapper objectMapper,
            RestTemplateBuilder builder) {
        this(properties, objectMapper, builder
                .rootUri(withoutTrailingSlash(properties.materialBaseUrl()))
                .connectTimeout(properties.requestTimeout())
                .readTimeout(properties.requestTimeout())
                .build());
    }

    MaterialWmsHttpClient(
            BpiWmsAdapterProperties properties,
            ObjectMapper objectMapper,
            RestTemplate restTemplate) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    RestTemplate restTemplate() {
        return restTemplate;
    }

    @Override
    public Optional<MaterialWmsDocument> findByIdempotency(
            String tenantId, String idempotencyKey) {
        String path = UriComponentsBuilder
                .fromPath("/material/wms/completion-inbounds/by-idempotency")
                .queryParam("sourceSystem", SOURCE_SYSTEM)
                .queryParam("idempotencyKey", idempotencyKey)
                .build()
                .toUriString();
        Envelope envelope = exchange(
                HttpMethod.GET, path, tenantId, new HttpEntity<Void>(headers(tenantId)), true);
        if (envelope.code() == 404) {
            return Optional.empty();
        }
        JsonNode document = requiredObject(envelope.data(), "document");
        List<MaterialWmsDocument.Line> lines = new ArrayList<>();
        JsonNode lineNodes = envelope.data().path("lines");
        if (!lineNodes.isArray()) {
            throw new MaterialWmsTransientException("material-wms lookup did not return a lines array.");
        }
        for (JsonNode line : lineNodes) {
            lines.add(new MaterialWmsDocument.Line(
                    text(line, "source_system"),
                    text(line, "source_line_id"),
                    text(line, "material_code"),
                    text(line, "batch_no"),
                    text(line, "production_batch_no"),
                    text(line, "warehouse_code"),
                    text(line, "location_code"),
                    decimal(line, "quantity"),
                    text(line, "unit_code"),
                    text(line, "quality_status")));
        }
        return Optional.of(new MaterialWmsDocument(
                longValue(document, "id"),
                text(document, "document_no"),
                text(document, "source_system"),
                text(document, "source_document_id"),
                text(document, "idempotency_key"),
                text(document, "warehouse_code"),
                text(document, "status"),
                text(document, "quality_status"),
                List.copyOf(lines)));
    }

    @Override
    public void createCompletionInbound(MaterialWmsCreateRequest request) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("srcPartId", request.sourceLineId());
        line.put("goodCode", request.materialCode());
        line.put("batchText", request.batchNo());
        line.put("produceBatchNum", request.productionBatchNo());
        line.put("placeSetCode", request.locationCode());
        line.put("quantity", request.quantity());
        line.put("unitCode", request.unitCode());
        line.put("checkResult", "BaseSet_checkResult/qualified");
        line.put("detailMemo", request.memo());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceSystem", SOURCE_SYSTEM);
        payload.put("idempotencyKey", request.idempotencyKey());
        payload.put("srcID", request.sourceDocumentId());
        payload.put("srcTableNo", request.sourceDocumentNo());
        payload.put("directiveNo", request.directiveNo());
        payload.put("companyCode", request.companyCode());
        payload.put("userName", "bpi-wms-adapter");
        payload.put("wareCode", request.warehouseCode());
        payload.put("storageDate", request.storageDate().toString());
        payload.put("comeType", "produceIn");
        payload.put("redBlue", "blue");
        payload.put("handRemarks", request.memo());
        payload.put("detailList", List.of(line));

        HttpHeaders headers = headers(request.tenantId());
        headers.setContentType(MediaType.APPLICATION_JSON);
        exchange(
                HttpMethod.POST,
                "/material/produceInSingles/produceInSingl/generateProductInSingle",
                request.tenantId(),
                new HttpEntity<Map<String, Object>>(payload, headers),
                false);
    }

    private Envelope exchange(
            HttpMethod method,
            String path,
            String tenantId,
            HttpEntity<?> entity,
            boolean lookup) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(path, method, entity, String.class);
            return parseEnvelope(response.getStatusCode().value(), response.getBody(), lookup);
        } catch (HttpStatusCodeException error) {
            return parseEnvelope(
                    error.getStatusCode().value(), error.getResponseBodyAsString(), lookup);
        } catch (RestClientException error) {
            throw new MaterialWmsTransientException(
                    "material-wms request failed for tenant " + tenantId, error);
        }
    }

    private Envelope parseEnvelope(int httpStatus, String body, boolean lookup) {
        if (body == null || body.isBlank()) {
            if (lookup && httpStatus == 404) {
                return new Envelope(404, null, "not found");
            }
            throw new MaterialWmsTransientException(
                    "material-wms returned an empty response with HTTP " + httpStatus + ".");
        }
        if (body.length() > MAX_RESPONSE_BYTES) {
            throw new MaterialWmsTransientException("material-wms response exceeds the safety limit.");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            int code = root.path("code").isInt() ? root.path("code").asInt() : httpStatus;
            String message = root.path("message").asText(root.path("msg").asText("material-wms error"));
            if (lookup && code == 404) {
                return new Envelope(404, null, message);
            }
            if (httpStatus >= 500 || code >= 500) {
                throw new MaterialWmsTransientException(
                        "material-wms transient error " + code + ": " + message);
            }
            if (httpStatus >= 400 || code != 200) {
                throw new MaterialWmsBusinessException("MATERIAL_WMS_" + code, message);
            }
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) {
                throw new MaterialWmsTransientException("material-wms success response has no data.");
            }
            return new Envelope(code, data, message);
        } catch (MaterialWmsBusinessException | MaterialWmsTransientException error) {
            throw error;
        } catch (Exception error) {
            throw new MaterialWmsTransientException("material-wms returned invalid JSON.", error);
        }
    }

    private HttpHeaders headers(String tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set(TENANT_HEADER, tenantId);
        headers.set(API_KEY_HEADER, properties.materialApiKey());
        return headers;
    }

    private static JsonNode requiredObject(JsonNode parent, String field) {
        JsonNode value = parent.path(field);
        if (!value.isObject()) {
            throw new MaterialWmsTransientException("material-wms response field " + field + " is invalid.");
        }
        return value;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isValueNode() || value.asText().isBlank()) {
            throw new MaterialWmsTransientException("material-wms response field " + field + " is required.");
        }
        return value.asText();
    }

    private static long longValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.canConvertToLong()) {
            throw new MaterialWmsTransientException("material-wms response field " + field + " is invalid.");
        }
        return value.asLong();
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        try {
            return new BigDecimal(node.path(field).asText());
        } catch (Exception error) {
            throw new MaterialWmsTransientException(
                    "material-wms response field " + field + " is invalid.", error);
        }
    }

    private static String withoutTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private record Envelope(int code, JsonNode data, String message) {
    }
}
