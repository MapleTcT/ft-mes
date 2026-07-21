package com.mapletct.ftmes.qcsoutbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@ConditionalOnProperty(name = "qcs.bpi.outbox.enabled", havingValue = "true")
public class HttpBpiBatchResolver implements BpiBatchResolver {

    private final QcsQualityGateOutboxProperties properties;
    private final QcsInternalJwtIssuer jwtIssuer;
    private final RestTemplate restTemplate;

    public HttpBpiBatchResolver(
            QcsQualityGateOutboxProperties properties,
            QcsInternalJwtIssuer jwtIssuer,
            RestTemplate qcsBpiRestTemplate) {
        this.properties = properties;
        this.jwtIssuer = jwtIssuer;
        this.restTemplate = qcsBpiRestTemplate;
    }

    @Override
    public ResolvedBpiBatch resolve(QcsQualityGateOutboxRecord record) {
        URI uri = UriComponentsBuilder
            .fromHttpUrl(trimSlash(properties.getBpiBaseUrl()) + "/internal/bpi/v1/batches/resolve")
            .queryParam("plantId", record.getPlantId())
            .queryParam("lineId", record.getLineId())
            .queryParam("orderId", record.getSourceOrderId())
            .build()
            .encode()
            .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtIssuer.issue(record));
        try {
            ResponseEntity<BpiBatchResolutionResponse> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<Void>(headers), BpiBatchResolutionResponse.class);
            BpiBatchResolutionResponse envelope = response.getBody();
            ResolvedBpiBatch batch = envelope == null ? null : envelope.getData();
            if (batch == null) throw new RetryableQcsOutboxException("BPI batch resolver returned an empty response");
            validateIdentity(record, batch);
            return batch;
        } catch (HttpStatusCodeException error) {
            int status = error.getRawStatusCode();
            String message = "BPI batch resolver returned HTTP " + status;
            if (status == 400 || status == 403 || status == 409 || status == 422) {
                throw new PermanentQcsOutboxException(message, error);
            }
            throw new RetryableQcsOutboxException(message, error);
        } catch (RestClientException error) {
            throw new RetryableQcsOutboxException("BPI batch resolver request failed", error);
        }
    }

    private static void validateIdentity(QcsQualityGateOutboxRecord record, ResolvedBpiBatch batch) {
        if (!equal(record.getTenantId(), batch.getTenantId())
                || !equal(record.getPlantId(), batch.getPlantId())
                || !equal(record.getLineId(), batch.getLineId())
                || !equal(record.getSourceOrderId(), batch.getOrderId())) {
            throw new PermanentQcsOutboxException("Resolved BPI batch identity does not match the QCS outbox scope");
        }
        if (blank(batch.getId())) {
            throw new PermanentQcsOutboxException("Resolved BPI batch does not contain an id");
        }
    }

    private static String trimSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private static boolean equal(String left, String right) {
        return left != null && left.equals(right);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
