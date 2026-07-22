package com.mapletct.ftmes.bpi.interfaces.rest;

import com.mapletct.ftmes.bpi.application.ActorContextFactory;
import com.mapletct.ftmes.bpi.application.CommandResult;
import com.mapletct.ftmes.bpi.application.DatasetCatalogPublicationService;
import com.mapletct.ftmes.bpi.application.DatasetMaterializationService;
import com.mapletct.ftmes.bpi.application.DatasetMlflowRegistrationService;
import com.mapletct.ftmes.bpi.application.DatasetRetentionArchiveService;
import com.mapletct.ftmes.bpi.application.DatasetService;
import com.mapletct.ftmes.bpi.domain.DatasetCatalogPublicationView;
import com.mapletct.ftmes.bpi.domain.DatasetDefinitionView;
import com.mapletct.ftmes.bpi.domain.DatasetMaterializationView;
import com.mapletct.ftmes.bpi.domain.DatasetMlflowRegistrationView;
import com.mapletct.ftmes.bpi.domain.DatasetRetentionArchiveView;
import com.mapletct.ftmes.bpi.domain.DatasetSnapshotView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasAnyRole('BPI_VIEWER', 'BPI_SHIFT_LEAD', 'BPI_ENGINEER', 'BPI_ADMIN')")
public class DatasetController {
    private final ActorContextFactory actorContextFactory;
    private final DatasetService service;
    private final DatasetMaterializationService materializationService;
    private final DatasetCatalogPublicationService catalogPublicationService;
    private final DatasetRetentionArchiveService retentionArchiveService;
    private final DatasetMlflowRegistrationService mlflowRegistrationService;

    public DatasetController(
            ActorContextFactory actorContextFactory,
            DatasetService service,
            DatasetMaterializationService materializationService,
            DatasetCatalogPublicationService catalogPublicationService,
            DatasetRetentionArchiveService retentionArchiveService,
            DatasetMlflowRegistrationService mlflowRegistrationService) {
        this.actorContextFactory = actorContextFactory;
        this.service = service;
        this.materializationService = materializationService;
        this.catalogPublicationService = catalogPublicationService;
        this.retentionArchiveService = retentionArchiveService;
        this.mlflowRegistrationService = mlflowRegistrationService;
    }

    @GetMapping("/bpi/v1/datasets")
    public ApiResponse<List<DatasetDefinitionView>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String plantId,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {
        return ApiResponse.of(
                service.list(actorContextFactory.from(jwt), plantId, limit), request);
    }

    @PostMapping("/bpi/v1/datasets")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<DatasetDefinitionView>> createDefinition(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody DatasetDefinitionCreateCommand command,
            HttpServletRequest request) {
        return ok(service.createDefinition(actorContextFactory.from(jwt), idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    @PostMapping("/bpi/v1/datasets/{datasetId}/snapshots")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<DatasetSnapshotView>> createSnapshot(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID datasetId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody DatasetSnapshotCommand command,
            HttpServletRequest request) {
        CommandResult<DatasetSnapshotView> result = service.createSnapshot(
                actorContextFactory.from(jwt), datasetId, idempotencyKey,
                ifMatch, command, traceId(request));
        ResponseEntity.BodyBuilder response = ResponseEntity.accepted();
        if (result.replayed()) response.header("Idempotent-Replay", "true");
        return response.body(ApiResponse.of(result.data(), request));
    }

    @GetMapping("/bpi/v1/dataset-snapshots/{snapshotId}")
    public ApiResponse<DatasetSnapshotView> getSnapshot(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID snapshotId,
            HttpServletRequest request) {
        return ApiResponse.of(
                service.getSnapshot(actorContextFactory.from(jwt), snapshotId), request);
    }

    @PostMapping("/bpi/v1/dataset-snapshots/{snapshotId}/materializations")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<DatasetMaterializationView>> requestMaterialization(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID snapshotId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody DatasetMaterializationCommand command,
            HttpServletRequest request) {
        return accepted(materializationService.request(
                actorContextFactory.from(jwt), snapshotId, idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    @GetMapping("/bpi/v1/dataset-materializations/{materializationId}")
    public ApiResponse<DatasetMaterializationView> getMaterialization(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID materializationId,
            HttpServletRequest request) {
        return ApiResponse.of(materializationService.get(
                actorContextFactory.from(jwt), materializationId), request);
    }

    @PostMapping("/bpi/v1/dataset-materializations/{materializationId}/retry")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<DatasetMaterializationView>> retryMaterialization(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID materializationId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return accepted(materializationService.retry(
                actorContextFactory.from(jwt), materializationId, idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    @PostMapping("/bpi/v1/dataset-materializations/{materializationId}/catalog-publications")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<DatasetCatalogPublicationView>> requestCatalogPublication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID materializationId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return accepted(catalogPublicationService.request(
                actorContextFactory.from(jwt), materializationId, idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    @GetMapping("/bpi/v1/dataset-materializations/{materializationId}/catalog-publications")
    public ApiResponse<DatasetCatalogPublicationView> getCatalogPublicationForMaterialization(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID materializationId,
            HttpServletRequest request) {
        return ApiResponse.of(catalogPublicationService.getForMaterialization(
                actorContextFactory.from(jwt), materializationId), request);
    }

    @GetMapping("/bpi/v1/dataset-catalog-publications/{publicationId}")
    public ApiResponse<DatasetCatalogPublicationView> getCatalogPublication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID publicationId,
            HttpServletRequest request) {
        return ApiResponse.of(catalogPublicationService.get(
                actorContextFactory.from(jwt), publicationId), request);
    }

    @PostMapping("/bpi/v1/dataset-catalog-publications/{publicationId}/retry")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<DatasetCatalogPublicationView>> retryCatalogPublication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID publicationId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return accepted(catalogPublicationService.retry(
                actorContextFactory.from(jwt), publicationId, idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    @PostMapping("/bpi/v1/dataset-catalog-publications/{publicationId}/retention-archives")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<DatasetRetentionArchiveView>> requestRetentionArchive(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID publicationId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return accepted(retentionArchiveService.request(
                actorContextFactory.from(jwt), publicationId, idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    @GetMapping("/bpi/v1/dataset-catalog-publications/{publicationId}/retention-archives")
    public ApiResponse<DatasetRetentionArchiveView> getRetentionArchiveForPublication(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID publicationId,
            HttpServletRequest request) {
        return ApiResponse.of(retentionArchiveService.getForPublication(
                actorContextFactory.from(jwt), publicationId), request);
    }

    @GetMapping("/bpi/v1/dataset-retention-archives/{archiveId}")
    public ApiResponse<DatasetRetentionArchiveView> getRetentionArchive(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID archiveId,
            HttpServletRequest request) {
        return ApiResponse.of(retentionArchiveService.get(
                actorContextFactory.from(jwt), archiveId), request);
    }

    @PostMapping("/bpi/v1/dataset-retention-archives/{archiveId}/retry")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<DatasetRetentionArchiveView>> retryRetentionArchive(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID archiveId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return accepted(retentionArchiveService.retry(
                actorContextFactory.from(jwt), archiveId, idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    @PostMapping("/bpi/v1/dataset-retention-archives/{archiveId}/mlflow-registrations")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<DatasetMlflowRegistrationView>> requestMlflowRegistration(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID archiveId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return accepted(mlflowRegistrationService.request(
                actorContextFactory.from(jwt), archiveId, idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    @GetMapping("/bpi/v1/dataset-retention-archives/{archiveId}/mlflow-registrations")
    public ApiResponse<DatasetMlflowRegistrationView> getMlflowRegistrationForArchive(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID archiveId,
            HttpServletRequest request) {
        return ApiResponse.of(mlflowRegistrationService.getForArchive(
                actorContextFactory.from(jwt), archiveId), request);
    }

    @GetMapping("/bpi/v1/dataset-mlflow-registrations/{registrationId}")
    public ApiResponse<DatasetMlflowRegistrationView> getMlflowRegistration(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID registrationId,
            HttpServletRequest request) {
        return ApiResponse.of(mlflowRegistrationService.get(
                actorContextFactory.from(jwt), registrationId), request);
    }

    @PostMapping("/bpi/v1/dataset-mlflow-registrations/{registrationId}/retry")
    @PreAuthorize("hasAnyRole('BPI_ENGINEER', 'BPI_ADMIN')")
    public ResponseEntity<ApiResponse<DatasetMlflowRegistrationView>> retryMlflowRegistration(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID registrationId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ReasonCommand command,
            HttpServletRequest request) {
        return accepted(mlflowRegistrationService.retry(
                actorContextFactory.from(jwt), registrationId, idempotencyKey,
                ifMatch, command, traceId(request)), request);
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(
            CommandResult<T> result,
            HttpServletRequest request) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result.replayed()) response.header("Idempotent-Replay", "true");
        return response.body(ApiResponse.of(result.data(), request));
    }

    private <T> ResponseEntity<ApiResponse<T>> accepted(
            CommandResult<T> result,
            HttpServletRequest request) {
        ResponseEntity.BodyBuilder response = ResponseEntity.accepted();
        if (result.replayed()) response.header("Idempotent-Replay", "true");
        return response.body(ApiResponse.of(result.data(), request));
    }

    private String traceId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(TraceIdFilter.ATTRIBUTE));
    }
}
