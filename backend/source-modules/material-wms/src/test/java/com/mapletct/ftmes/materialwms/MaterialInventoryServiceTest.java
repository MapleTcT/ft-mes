package com.mapletct.ftmes.materialwms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.materialwms.api.StockDocumentLineRequest;
import com.mapletct.ftmes.materialwms.api.StockDocumentRequest;
import com.mapletct.ftmes.materialwms.domain.DocumentType;
import com.mapletct.ftmes.materialwms.domain.MaterialWmsBusinessException;
import com.mapletct.ftmes.materialwms.repository.MaterialWmsRepository;
import com.mapletct.ftmes.materialwms.service.MaterialInventoryService;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MaterialInventoryServiceTest {

    @Test
    public void concurrentSourceCollisionWithDifferentIdempotencyKeyReturnsConflict() {
        MaterialWmsRepository repository = mock(MaterialWmsRepository.class);
        MaterialInventoryService service = new MaterialInventoryService(
            repository, new ObjectMapper());
        StockDocumentRequest request = request();

        Map<String, Object> winningDocument = new LinkedHashMap<String, Object>();
        winningDocument.put("id", 101L);
        winningDocument.put("source_system", "BPI");
        winningDocument.put("source_document_id", "EVENT-1");
        winningDocument.put("warehouse_code", "WARE");
        winningDocument.put("idempotency_key", "WINNING-KEY");

        when(repository.findDocumentBySource(
                "TENANT", DocumentType.COMPLETION_INBOUND, "BPI", "EVENT-1", "WARE"))
            .thenReturn(null, winningDocument);
        when(repository.findDocumentByIdempotency(
                "TENANT", DocumentType.COMPLETION_INBOUND, "BPI", "LOSING-KEY"))
            .thenReturn(null);
        when(repository.insertDocumentIfAbsent(
                eq("TENANT"), eq(DocumentType.COMPLETION_INBOUND), eq("BPI"),
                eq("LOSING-KEY"), anyString(), eq(request), any(), anyString()))
            .thenReturn(false);

        try {
            service.createCompletionInbound("TENANT", request);
            fail("Expected idempotency conflict");
        } catch (MaterialWmsBusinessException error) {
            assertEquals(409, error.getCode());
        }
    }

    private StockDocumentRequest request() {
        StockDocumentLineRequest line = new StockDocumentLineRequest();
        line.setSrcPartId("EVENT-1:1");
        line.setGoodCode("MAT-1");
        line.setBatchText("BATCH-1");
        line.setProductionBatchNo("BATCH-1");
        line.setPlaceSetCode("LOC");
        line.setQuantity(new BigDecimal("10"));
        line.setUnitCode("kg");
        line.setCheckResult("BaseSet_checkResult/qualified");

        StockDocumentRequest request = new StockDocumentRequest();
        request.setSourceSystem("BPI");
        request.setIdempotencyKey("LOSING-KEY");
        request.setSourceDocumentId("EVENT-1");
        request.setCompanyCode("COMP");
        request.setWareCode("WARE");
        request.setStorageDate("2026-07-20");
        request.setComeType("produceIn");
        request.setRedBlue("blue");
        request.setDetailList(Collections.singletonList(line));
        return request;
    }
}
