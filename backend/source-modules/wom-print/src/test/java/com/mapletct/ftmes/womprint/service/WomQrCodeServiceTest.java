package com.mapletct.ftmes.womprint.service;

import com.mapletct.ftmes.womprint.api.GenerateQrCodeRequest;
import com.mapletct.ftmes.womprint.api.PrintBackfillRequest;
import com.mapletct.ftmes.womprint.domain.GenerationResult;
import com.mapletct.ftmes.womprint.domain.QrCodeRecord;
import com.mapletct.ftmes.womprint.domain.RequestSummary;
import com.mapletct.ftmes.womprint.domain.TaskContext;
import com.mapletct.ftmes.womprint.domain.WomPrintBusinessException;
import com.mapletct.ftmes.womprint.repository.WomPrintRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WomQrCodeServiceTest {

    @Mock
    private WomPrintRepository repository;

    private WomQrCodeService service;
    private TaskContext task;

    @Before
    public void setUp() {
        service = new WomQrCodeService(repository);
        task = new TaskContext(
            101L,
            "TASK-101",
            "BATCH-101",
            202L,
            303L,
            "MAT-202",
            "测试物料",
            LocalDateTime.of(2024, 1, 2, 8, 0),
            true,
            2,
            "BaseSet_validUnit/month"
        );
        when(repository.findTask(101L)).thenReturn(task);
    }

    @Test
    public void generateUsesLegacyDailySequenceAndDetailContract() {
        when(repository.lockDailySequence(eq("COMP"), any())).thenReturn(7);

        GenerationResult result = service.generate("COMP", request("REQ-101", 2));

        assertFalse(result.isIdempotent());
        assertEquals(Arrays.asList(
            "BATCH-101,24010200008,MAT-202,2024-01-02,2024-03-02,G0001",
            "BATCH-101,24010200009,MAT-202,2024-01-02,2024-03-02,G0001"
        ), result.getDetails());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QrCodeRecord>> records = ArgumentCaptor.forClass((Class) List.class);
        verify(repository).insertQrCodes(records.capture());
        assertEquals(2, records.getValue().size());
        assertEquals("24010200008", records.getValue().get(0).getQrCode());
        assertEquals(records.getValue().get(0).getDetail(), records.getValue().get(0).getQrContent());
        verify(repository).updateDailySequence(eq("COMP"), any(), eq(9));
    }

    @Test
    public void generateReturnsPersistedRowsForSameRequestId() {
        when(repository.lockDailySequence(eq("COMP"), any())).thenReturn(0);
        GenerationResult first = service.generate("COMP", request("REQ-IDEMPOTENT", 1));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QrCodeRecord>> records = ArgumentCaptor.forClass((Class) List.class);
        verify(repository).insertQrCodes(records.capture());
        String requestHash = records.getValue().get(0).getRequestHash();
        clearInvocations(repository);
        when(repository.findRequestSummary("COMP", "REQ-IDEMPOTENT"))
            .thenReturn(new RequestSummary(requestHash, 1));
        when(repository.findRequestDetails("COMP", "REQ-IDEMPOTENT"))
            .thenReturn(first.getDetails());

        GenerationResult second = service.generate("COMP", request("REQ-IDEMPOTENT", 1));

        assertTrue(second.isIdempotent());
        assertEquals(first.getDetails(), second.getDetails());
        verify(repository, never()).lockDailySequence(anyString(), any());
        verify(repository, never()).insertQrCodes(any());
    }

    @Test
    public void generateRejectsRequestIdPayloadConflict() {
        when(repository.findRequestSummary("COMP", "REQ-CONFLICT"))
            .thenReturn(new RequestSummary("different-hash", 1));

        WomPrintBusinessException exception = expectBusinessException(
            () -> service.generate("COMP", request("REQ-CONFLICT", 1))
        );

        assertEquals(409, exception.getCode());
        verify(repository, never()).lockDailySequence(anyString(), any());
    }

    @Test
    public void generateRejectsExhaustedDailySequence() {
        when(repository.lockDailySequence(eq("COMP"), any())).thenReturn(99999);

        WomPrintBusinessException exception = expectBusinessException(
            () -> service.generate("COMP", request("REQ-OVERFLOW", 1))
        );

        assertEquals(409, exception.getCode());
        verify(repository, never()).insertQrCodes(any());
        verify(repository, never()).updateDailySequence(anyString(), any(), any(Integer.class));
    }

    @Test
    public void backfillReportsUpdatedAndMissingRows() {
        PrintBackfillRequest printed = backfill("DETAIL-1", 1);
        PrintBackfillRequest missing = backfill("DETAIL-2", 0);
        when(repository.backfillPrintState("COMP", "DETAIL-1", true)).thenReturn(1);
        when(repository.backfillPrintState("COMP", "DETAIL-2", false)).thenReturn(0);

        Map<String, Object> result = service.backfill("COMP", Arrays.asList(printed, missing));

        assertEquals(2, result.get("requested"));
        assertEquals(1, result.get("updated"));
        assertEquals(1, result.get("missing"));
        verify(repository, times(1)).backfillPrintState("COMP", "DETAIL-1", true);
    }

    @Test
    public void backfillRejectsExcessiveRowsBeforeDatabaseWork() {
        WomPrintBusinessException exception = expectBusinessException(
            () -> service.backfill("COMP", Collections.nCopies(10001, backfill("DETAIL-1", 1)))
        );

        assertEquals(400, exception.getCode());
        verify(repository, never()).backfillPrintState(anyString(), anyString(), eq(true));
    }

    @Test
    public void renderQrCodeReturnsPngBytes() {
        when(repository.findQrContent("COMP", "24010200001"))
            .thenReturn("BATCH-101,24010200001,MAT-202,2024-01-02,2024-03-02,G0001");

        byte[] image = service.renderQrCode("COMP", "24010200001", 256);

        assertTrue(image.length > 100);
        assertArrayEquals(
            new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a},
            Arrays.copyOf(image, 8)
        );
    }

    @Test
    public void calculateTermUsesManufactureDateWhenValidityIsNotManaged() {
        task = new TaskContext(
            101L, "TASK-101", "BATCH-101", 202L, 303L, "MAT-202", "测试物料",
            LocalDateTime.of(2024, 1, 2, 8, 0), false, null, null
        );
        when(repository.findTask(101L)).thenReturn(task);

        assertEquals("2024-06-08", service.calculateTermOfValidity(101L, "2024-06-08"));
    }

    @Test
    public void calculateTermUsesCalendarMonthsAtMonthEnd() {
        task = taskWithValidity(1, "BaseSet_validUnit/month");
        when(repository.findTask(101L)).thenReturn(task);

        assertEquals("2024-02-29", service.calculateTermOfValidity(101L, "2024-01-31"));
    }

    @Test
    public void calculateTermUsesCalendarYearsAcrossLeapDay() {
        task = taskWithValidity(1, "BaseSet_validUnit/year");
        when(repository.findTask(101L)).thenReturn(task);

        assertEquals("2025-02-28", service.calculateTermOfValidity(101L, "2024-02-29"));
    }

    @Test
    public void printerForLineDoesNotGuessAnUnmappedPrinter() {
        Map<String, Object> result = service.printerForLine("LINE-01");

        assertEquals("LINE-01", result.get("lineId"));
        assertEquals(null, result.get("packId"));
        assertEquals(false, result.get("lineMappingConfigured"));
        verify(repository, never()).listPrinters();
    }

    private GenerateQrCodeRequest request(String requestId, int count) {
        GenerateQrCodeRequest request = new GenerateQrCodeRequest();
        request.setTaskId(101L);
        request.setManuDate("2024-01-02");
        request.setPrintCount(count);
        request.setRequestId(requestId);
        return request;
    }

    private static TaskContext taskWithValidity(int period, String unit) {
        return new TaskContext(
            101L, "TASK-101", "BATCH-101", 202L, 303L, "MAT-202", "测试物料",
            LocalDateTime.of(2024, 1, 2, 8, 0), true, period, unit
        );
    }

    private static PrintBackfillRequest backfill(String detail, int isPrint) {
        PrintBackfillRequest request = new PrintBackfillRequest();
        request.setDetail(detail);
        request.setIsPrint(isPrint);
        return request;
    }

    private static WomPrintBusinessException expectBusinessException(Runnable action) {
        try {
            action.run();
            fail("Expected WomPrintBusinessException");
            return null;
        } catch (WomPrintBusinessException exception) {
            return exception;
        }
    }
}
