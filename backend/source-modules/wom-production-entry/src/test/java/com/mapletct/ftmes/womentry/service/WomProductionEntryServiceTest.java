package com.mapletct.ftmes.womentry.service;

import com.mapletct.ftmes.womentry.api.CreateInstructionRequest;
import com.mapletct.ftmes.womentry.domain.CreateInstructionResult;
import com.mapletct.ftmes.womentry.domain.ManualTaskRequestRecord;
import com.mapletct.ftmes.womentry.domain.ProductionOption;
import com.mapletct.ftmes.womentry.domain.TaskResult;
import com.mapletct.ftmes.womentry.domain.WomEntryBusinessException;
import com.mapletct.ftmes.womentry.repository.WomProductionEntryRepository;
import com.mapletct.ftmes.womentry.support.RequestAuthContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WomProductionEntryServiceTest {

    @Mock
    private WomProductionEntryRepository repository;

    @Mock
    private WomUpstreamClient upstreamClient;

    private WomProductionEntryService service;
    private CreateInstructionRequest request;
    private ProductionOption eligibleOption;
    private TaskResult task;
    private RequestAuthContext authContext;

    @Before
    public void setUp() {
        service = new WomProductionEntryService(
            repository,
            upstreamClient,
            new ObjectMapper(),
            1,
            0,
            120
        );
        request = request("WOM-MANUAL-REQUEST-001", "BATCH-001");
        eligibleOption = option(900L);
        task = new TaskResult(
            101L,
            1,
            "TASK-001",
            "BATCH-001",
            88,
            true,
            201L,
            "/msService/WOM/produceTask/produceTask/makeTaskEdit?pendingId=201",
            "编辑"
        );
        authContext = new RequestAuthContext("Bearer test", "", "default", "zh-CN");
        when(repository.findOption("MAT-001", "FORMULA-001", 300L)).thenReturn(eligibleOption);
    }

    @Test
    public void createPersistsIdempotencyAndReturnsCreatedTask() {
        when(repository.findRequest("default", request.getRequestId())).thenReturn(null);
        when(repository.findActiveTaskByBatch("BATCH-001")).thenReturn(null, task);
        when(repository.insertRequest(eq("default"), eq(request.getRequestId()), anyString(), eq("BATCH-001"), anyString()))
            .thenReturn(true);
        when(upstreamClient.create(request, authContext))
            .thenReturn(Collections.<String, Object>singletonMap("code", 200));

        CreateInstructionResult result = service.create("default", request, authContext);

        assertFalse(result.isIdempotent());
        assertEquals(101L, result.getTask().getTaskId());
        verify(repository).markSuccess(eq("default"), eq(request.getRequestId()), eq(101L), anyString());
        verify(repository, never()).markFailed(anyString(), anyString(), anyString());
    }

    @Test
    public void createReturnsExistingTaskForSameRequest() {
        ManualTaskRequestRecord existing = new ManualTaskRequestRecord(
            "default",
            request.getRequestId(),
            WomProductionEntryService.requestHash(request),
            "BATCH-001",
            "PROCESSING",
            null,
            LocalDateTime.now()
        );
        when(repository.findRequest("default", request.getRequestId())).thenReturn(existing);
        when(repository.findActiveTaskByBatch("BATCH-001")).thenReturn(task);

        CreateInstructionResult result = service.create("default", request, authContext);

        assertTrue(result.isIdempotent());
        assertEquals(101L, result.getTask().getTaskId());
        verify(upstreamClient, never()).create(any(), any());
        verify(repository).markSuccess(eq("default"), eq(request.getRequestId()), eq(101L), anyString());
    }

    @Test
    public void createRejectsRequestIdPayloadConflict() {
        ManualTaskRequestRecord existing = new ManualTaskRequestRecord(
            "default",
            request.getRequestId(),
            "different-hash",
            "BATCH-001",
            "FAILED",
            null,
            LocalDateTime.now().minusMinutes(5)
        );
        when(repository.findRequest("default", request.getRequestId())).thenReturn(existing);

        WomEntryBusinessException exception = expectBusinessException(
            () -> service.create("default", request, authContext)
        );

        assertEquals(409, exception.getCode());
        verify(upstreamClient, never()).create(any(), any());
    }

    @Test
    public void createRejectsProductWithoutProductionUnit() {
        when(repository.findOption("MAT-001", "FORMULA-001", 300L)).thenReturn(option(null));

        WomEntryBusinessException exception = expectBusinessException(
            () -> service.create("default", request, authContext)
        );

        assertEquals(400, exception.getCode());
        assertEquals("产品未配置生产单位", exception.getMessage());
        verify(repository, never()).insertRequest(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void createRejectsDuplicateBatchFromAnotherRequest() {
        when(repository.findRequest("default", request.getRequestId())).thenReturn(null);
        when(repository.findActiveTaskByBatch("BATCH-001")).thenReturn(task);

        WomEntryBusinessException exception = expectBusinessException(
            () -> service.create("default", request, authContext)
        );

        assertEquals(409, exception.getCode());
        verify(upstreamClient, never()).create(any(), any());
    }

    @Test
    public void createRejectsConcurrentBatchClaimFromAnotherRequest() {
        when(repository.findRequest("default", request.getRequestId())).thenReturn(null);
        when(repository.findActiveTaskByBatch("BATCH-001")).thenReturn(null);
        when(repository.insertRequest(eq("default"), eq(request.getRequestId()), anyString(), eq("BATCH-001"), anyString()))
            .thenReturn(false);

        WomEntryBusinessException exception = expectBusinessException(
            () -> service.create("default", request, authContext)
        );

        assertEquals(409, exception.getCode());
        verify(repository, times(2)).insertRequest(
            eq("default"), eq(request.getRequestId()), anyString(), eq("BATCH-001"), anyString()
        );
        verify(upstreamClient, never()).create(any(), any());
    }

    @Test
    public void createRejectsRetryWhenBatchWasClaimedByAnotherRequest() {
        ManualTaskRequestRecord failed = new ManualTaskRequestRecord(
            "default",
            request.getRequestId(),
            WomProductionEntryService.requestHash(request),
            "BATCH-001",
            "FAILED",
            null,
            LocalDateTime.now().minusMinutes(5)
        );
        when(repository.findRequest("default", request.getRequestId())).thenReturn(failed);
        when(repository.findActiveTaskByBatch("BATCH-001")).thenReturn(null);
        when(repository.retryRequest("default", request.getRequestId())).thenReturn(false);

        WomEntryBusinessException exception = expectBusinessException(
            () -> service.create("default", request, authContext)
        );

        assertEquals(409, exception.getCode());
        verify(upstreamClient, never()).create(any(), any());
    }

    @Test
    public void createMarksFailedWhenUpstreamSuccessHasNoDatabaseRow() {
        when(repository.findRequest("default", request.getRequestId())).thenReturn(null);
        when(repository.findActiveTaskByBatch("BATCH-001")).thenReturn(null);
        when(repository.insertRequest(eq("default"), eq(request.getRequestId()), anyString(), eq("BATCH-001"), anyString()))
            .thenReturn(true);
        when(upstreamClient.create(request, authContext))
            .thenReturn(Collections.<String, Object>singletonMap("code", 200));

        WomEntryBusinessException exception = expectBusinessException(
            () -> service.create("default", request, authContext)
        );

        assertEquals(502, exception.getCode());
        verify(repository).markFailed(eq("default"), eq(request.getRequestId()), anyString());
        verify(repository, never()).markSuccess(anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    public void validationRejectsEndBeforeStart() {
        request.setPlanEndDate("2026-07-17 07:00:00");

        WomEntryBusinessException exception = expectBusinessException(
            () -> service.create("default", request, authContext)
        );

        assertEquals(400, exception.getCode());
        verify(repository, never()).findOption(anyString(), anyString(), anyLong());
    }

    private static CreateInstructionRequest request(String requestId, String batchCode) {
        CreateInstructionRequest value = new CreateInstructionRequest();
        value.setRequestId(requestId);
        value.setProductCode("MAT-001");
        value.setFormulaCode("FORMULA-001");
        value.setWorkLineId(300L);
        value.setPlanNum(new BigDecimal("12.5"));
        value.setPlanStartDate("2026-07-17 08:00:00");
        value.setPlanEndDate("2026-07-17 09:00:00");
        value.setBatchCode(batchCode);
        value.setNeedPack(Boolean.FALSE);
        return value;
    }

    private static ProductionOption option(Long unitId) {
        return new ProductionOption(
            100L,
            "MAT-001",
            "测试产品",
            200L,
            "FORMULA-001",
            "测试配方",
            300L,
            "LINE-001",
            "测试生产线",
            unitId,
            unitId == null ? null : "件",
            unitId == null ? null : "件"
        );
    }

    private static WomEntryBusinessException expectBusinessException(Runnable action) {
        try {
            action.run();
            fail("Expected WomEntryBusinessException");
            return null;
        } catch (WomEntryBusinessException exception) {
            return exception;
        }
    }
}
