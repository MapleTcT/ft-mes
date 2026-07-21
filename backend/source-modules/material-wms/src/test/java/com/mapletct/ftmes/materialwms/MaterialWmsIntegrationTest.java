package com.mapletct.ftmes.materialwms;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class MaterialWmsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Before
    public void cleanDatabase() {
        jdbc.update("DELETE FROM wms_quality_allocation_events");
        jdbc.update("DELETE FROM wms_quality_allocations");
        jdbc.update("DELETE FROM wms_inventory_transactions");
        jdbc.update("DELETE FROM wms_stock_document_lines");
        jdbc.update("UPDATE wms_stock_documents SET reversal_of_document_id = NULL");
        jdbc.update("DELETE FROM wms_stock_documents");
        jdbc.update("DELETE FROM wms_quality_results");
        jdbc.update("DELETE FROM wms_batch_stocks");
    }

    @Test
    public void completionInboundQualityReleaseAndIssueArePersistentAndIdempotent() throws Exception {
        String inbound = inboundJson("ADP_TEST_IN_1", "ADP_TEST_LINE_1", "10");
        mockMvc.perform(post("/public/material/produceInSingles/produceInSingl/generateProductInSingle")
                .contentType(MediaType.APPLICATION_JSON).content(inbound))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.documentType").value("COMPLETION_INBOUND"))
            .andExpect(jsonPath("$.data.idempotent").value(false));

        mockMvc.perform(post("/public/material/produceInSingles/produceInSingl/generateProductInSingle")
                .contentType(MediaType.APPLICATION_JSON).content(inbound))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.idempotent").value(true));

        assertEquals(1L, count("wms_stock_documents"));
        assertEquals(1L, count("wms_stock_document_lines"));
        assertStock("10.000000", "0.000000", "10.000000");

        mockMvc.perform(post("/material/foreign/foreign/checkProdResult")
                .header("X-Tenant-Id", "COMP")
                .param("srcId", "ADP_TEST_LINE_1")
                .param("checkResult", "BaseSet_checkResult/qualified"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.qualityStatus").value("QUALIFIED"))
            .andExpect(jsonPath("$.data.appliedLines").value(1));

        assertStock("10.000000", "10.000000", "0.000000");
        assertEquals(2L, count("wms_inventory_transactions"));

        String issue = "{"
            + "\"srcId\":\"ADP_TEST_OUT_1\",\"srcTableNo\":\"OUT-1\","
            + "\"companyCode\":\"COMP\",\"wareCode\":\"WARE\",\"storageDate\":\"2026-07-10\","
            + "\"comeType\":\"produceOut\",\"redBlue\":\"blue\",\"detailList\":[{"
            + "\"goodCode\":\"MAT\",\"batchText\":\"BATCH\",\"productBatch\":\"PB\","
            + "\"placeSetCode\":\"LOC\",\"quantity\":3}]}";
        mockMvc.perform(post("/public/material/produceOutSingle/produceOutSing/generateProduceOutSing")
                .contentType(MediaType.APPLICATION_JSON).content(issue))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.documentType").value("PRODUCTION_ISSUE"));

        mockMvc.perform(post("/public/material/produceOutSingle/produceOutSing/generateProduceOutSing")
                .contentType(MediaType.APPLICATION_JSON).content(issue))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.idempotent").value(true));

        assertStock("7.000000", "7.000000", "0.000000");
        assertEquals(2L, count("wms_stock_documents"));
        assertEquals(3L, count("wms_inventory_transactions"));

        mockMvc.perform(get("/material/wms/completion-inbounds")
                .header("X-Tenant-Id", "COMP").param("keyword", "ADP_TEST"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].quality_status").value("QUALIFIED"));
    }

    @Test
    public void qualityCallbackCanArriveBeforeCompletionInbound() throws Exception {
        mockMvc.perform(post("/material/foreign/foreign/checkProdResult")
                .header("X-Tenant-Id", "COMP")
                .param("srcId", "ADP_TEST_LINE_EARLY")
                .param("checkResult", "BaseSet_checkResult/qualified"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pendingInbound").value(true));

        mockMvc.perform(post("/material/produceInSingles/produceInSingl/generateProductInSingle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inboundJson("ADP_TEST_IN_EARLY", "ADP_TEST_LINE_EARLY", "5")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        assertStock("5.000000", "5.000000", "0.000000");
        assertEquals("QUALIFIED", jdbc.queryForObject(
            "SELECT quality_status FROM wms_stock_document_lines", String.class));
    }

    @Test
    public void sameLegacySourceDocumentCanAppendDistinctLinesWithoutDoublePosting() throws Exception {
        String first = inboundJson("ADP_TEST_MULTI", "ADP_TEST_MULTI_LINE_1", "2");
        String second = inboundJson("ADP_TEST_MULTI", "ADP_TEST_MULTI_LINE_2", "3");

        mockMvc.perform(post("/material/produceInSingles/produceInSingl/generateProductInSingle")
                .contentType(MediaType.APPLICATION_JSON).content(first))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.idempotent").value(false));
        mockMvc.perform(post("/material/produceInSingles/produceInSingl/generateProductInSingle")
                .contentType(MediaType.APPLICATION_JSON).content(second))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.idempotent").value(false));
        mockMvc.perform(post("/material/produceInSingles/produceInSingl/generateProductInSingle")
                .contentType(MediaType.APPLICATION_JSON).content(second))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.idempotent").value(true));

        assertEquals(1L, count("wms_stock_documents"));
        assertEquals(2L, count("wms_stock_document_lines"));
        assertEquals(2L, count("wms_inventory_transactions"));
        assertStock("5.000000", "0.000000", "5.000000");

        mockMvc.perform(post("/material/produceInSingles/produceInSingl/generateProductInSingle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inboundJson("ADP_TEST_MULTI", "ADP_TEST_MULTI_LINE_2", "4")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(409));
        assertStock("5.000000", "0.000000", "5.000000");
    }

    @Test
    public void badQuantityAllocationKeepsOnlyGoodQuantityAvailableAndCanBeReversed() throws Exception {
        String allocation = allocationJson("APPLY", "ALLOC-1", "REPORT-1", "LINE-ALLOC", "10", "8", "2");
        mockMvc.perform(post("/material/wms/quality-allocations")
                .header("X-Tenant-Id", "COMP")
                .contentType(MediaType.APPLICATION_JSON).content(allocation))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.pendingInbound").value(true));

        mockMvc.perform(post("/material/produceInSingles/produceInSingl/generateProductInSingle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inboundJson("IN-ALLOC", "LINE-ALLOC", "10")))
            .andExpect(status().isOk());
        assertStock("10.000000", "0.000000", "10.000000");

        mockMvc.perform(post("/material/foreign/foreign/checkProdResult")
                .header("X-Tenant-Id", "COMP")
                .param("srcId", "LINE-ALLOC")
                .param("checkResult", "BaseSet_checkResult/qualified"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.qualityStatus").value("PARTIAL"));
        assertStock("10.000000", "8.000000", "2.000000");
        assertEquals(new BigDecimal("8.000000"), jdbc.queryForObject(
            "SELECT good_quantity FROM wms_stock_document_lines", BigDecimal.class));
        assertEquals(new BigDecimal("2.000000"), jdbc.queryForObject(
            "SELECT bad_quantity FROM wms_stock_document_lines", BigDecimal.class));

        mockMvc.perform(post("/material/wms/quality-allocations")
                .header("X-Tenant-Id", "COMP")
                .contentType(MediaType.APPLICATION_JSON).content(allocation))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.idempotent").value(true));
        assertStock("10.000000", "8.000000", "2.000000");

        String reversal = allocationJson("REVERSE", "REVERSE-1", "REPORT-1", "LINE-ALLOC", "10", "8", "2");
        mockMvc.perform(post("/material/wms/quality-allocations")
                .header("X-Tenant-Id", "COMP")
                .contentType(MediaType.APPLICATION_JSON).content(reversal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REVERSED"));
        assertStock("10.000000", "10.000000", "0.000000");
        assertEquals("QUALIFIED", jdbc.queryForObject(
            "SELECT quality_status FROM wms_stock_document_lines", String.class));
        assertEquals(2L, count("wms_quality_allocation_events"));
    }

    @Test
    public void allocationAfterQualifiedInboundMovesOnlyBadQuantityBackToHold() throws Exception {
        mockMvc.perform(post("/material/produceInSingles/produceInSingl/generateProductInSingle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inboundJson("IN-ALLOC-LATE", "LINE-ALLOC-LATE", "10")))
            .andExpect(status().isOk());
        mockMvc.perform(post("/material/foreign/foreign/checkProdResult")
                .header("X-Tenant-Id", "COMP")
                .param("srcId", "LINE-ALLOC-LATE")
                .param("checkResult", "BaseSet_checkResult/qualified"))
            .andExpect(status().isOk());
        assertStock("10.000000", "10.000000", "0.000000");

        mockMvc.perform(post("/material/wms/quality-allocations")
                .header("X-Tenant-Id", "COMP")
                .contentType(MediaType.APPLICATION_JSON)
                .content(allocationJson(
                    "APPLY", "ALLOC-LATE", "REPORT-LATE", "LINE-ALLOC-LATE", "10", "7", "3")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.appliedLines").value(1));

        assertStock("10.000000", "7.000000", "3.000000");
        assertEquals("PARTIAL", jdbc.queryForObject(
            "SELECT quality_status FROM wms_stock_document_lines", String.class));
        assertEquals("PARTIAL", jdbc.queryForObject(
            "SELECT quality_status FROM wms_stock_documents", String.class));
    }

    @Test
    public void insufficientStockRollsBackWholeIssueDocument() throws Exception {
        String issue = "{"
            + "\"srcId\":\"ADP_TEST_OUT_MISSING\",\"companyCode\":\"COMP\","
            + "\"wareCode\":\"WARE\",\"storageDate\":\"2026-07-10\",\"comeType\":\"produceOut\","
            + "\"detailList\":[{\"goodCode\":\"NO_STOCK\",\"quantity\":1}]}";
        mockMvc.perform(post("/material/produceOutSingle/produceOutSing/generateProduceOutSing")
                .contentType(MediaType.APPLICATION_JSON).content(issue))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(409));

        assertEquals(0L, count("wms_stock_documents"));
        assertEquals(0L, count("wms_stock_document_lines"));
        assertEquals(0L, count("wms_batch_stocks"));
    }

    @Test
    public void operationalPageIsServedByTheModule() throws Exception {
        mockMvc.perform(get("/material/wms"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("完工入库")))
            .andExpect(content().string(containsString("/wms/completion-inbounds")));
    }

    @Test
    public void bpiCompletionInboundCanBeQueriedExactlyBeforeRetry() throws Exception {
        String idempotencyKey = "WMS_COMPLETION_INBOUND|COMP|BATCH-1|GATE-1|1";
        String commandEventId = "2ea229c2-f2bb-5da8-b84c-5b4bd00148ce";
        String inbound = bpiInboundJson(commandEventId, idempotencyKey, "10");

        mockMvc.perform(get("/material/wms/completion-inbounds/by-idempotency")
                .header("X-Tenant-Id", "COMP")
                .header("X-BPI-WMS-Key", "test-bpi-key")
                .param("sourceSystem", "BPI")
                .param("idempotencyKey", idempotencyKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(post("/material/produceInSingles/produceInSingl/generateProductInSingle")
                .header("X-Tenant-Id", "COMP")
                .header("X-BPI-WMS-Key", "test-bpi-key")
                .contentType(MediaType.APPLICATION_JSON).content(inbound))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.idempotent").value(false));

        mockMvc.perform(get("/material/wms/completion-inbounds/by-idempotency")
                .header("X-Tenant-Id", "COMP")
                .header("X-BPI-WMS-Key", "test-bpi-key")
                .param("sourceSystem", "BPI")
                .param("idempotencyKey", idempotencyKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.document.source_system").value("BPI"))
            .andExpect(jsonPath("$.data.document.source_document_id").value(commandEventId))
            .andExpect(jsonPath("$.data.document.idempotency_key").value(idempotencyKey))
            .andExpect(jsonPath("$.data.lines[0].source_system").value("BPI"))
            .andExpect(jsonPath("$.data.lines[0].unit_code").value("kg"))
            .andExpect(jsonPath("$.data.lines[0].quality_status").value("QUALIFIED"));

        mockMvc.perform(post("/material/produceInSingles/produceInSingl/generateProductInSingle")
                .header("X-Tenant-Id", "COMP")
                .header("X-BPI-WMS-Key", "test-bpi-key")
                .contentType(MediaType.APPLICATION_JSON).content(inbound))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.idempotent").value(true));

        mockMvc.perform(post("/material/produceInSingles/produceInSingl/generateProductInSingle")
                .header("X-Tenant-Id", "COMP")
                .header("X-BPI-WMS-Key", "test-bpi-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bpiInboundJson(
                    "53b38104-0f6a-59bf-a05b-5057ce399e30", idempotencyKey, "10")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(409));

        assertEquals(1L, count("wms_stock_documents"));
        assertEquals(1L, count("wms_stock_document_lines"));
        assertEquals(1L, count("wms_inventory_transactions"));
        assertStock("10.000000", "10.000000", "0.000000");
    }

    @Test
    public void bpiCompletionInboundFailsClosedWithoutIntegrationKey() throws Exception {
        mockMvc.perform(post("/material/produceInSingles/produceInSingl/generateProductInSingle")
                .header("X-Tenant-Id", "COMP")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bpiInboundJson(
                    "2ea229c2-f2bb-5da8-b84c-5b4bd00148ce", "BPI-IDEMPOTENCY", "10")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403));
        assertEquals(0L, count("wms_stock_documents"));
    }

    @Test
    public void bpiCompletionInboundReversalIsAppendOnlyPersistentAndIdempotent() throws Exception {
        String inboundKey = "WMS_COMPLETION_INBOUND|COMP|BATCH-REV|GATE-1|1";
        String inboundEventId = "2ea229c2-f2bb-5da8-b84c-5b4bd00148ce";
        mockMvc.perform(post("/material/produceInSingles/produceInSingl/generateProductInSingle")
                .header("X-Tenant-Id", "COMP")
                .header("X-BPI-WMS-Key", "test-bpi-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bpiInboundJson(inboundEventId, inboundKey, "10")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        String originalDocumentNo = jdbc.queryForObject(
            "SELECT document_no FROM wms_stock_documents WHERE idempotency_key = ?",
            String.class, inboundKey);
        String reversalKey = "WMS_COMPLETION_INBOUND_REVERSAL|COMP|BATCH-REV|GATE-1|1";
        String reversalEventId = "c9288339-f020-59cd-a50e-2e2855115582";
        String reversal = bpiReversalJson(
            reversalEventId, reversalKey, originalDocumentNo, "10");

        mockMvc.perform(post("/material/wms/completion-inbound-reversals")
                .header("X-Tenant-Id", "COMP")
                .contentType(MediaType.APPLICATION_JSON).content(reversal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(post("/material/wms/completion-inbound-reversals")
                .header("X-Tenant-Id", "COMP")
                .header("X-BPI-WMS-Key", "test-bpi-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bpiReversalJson(
                    reversalEventId, reversalKey, originalDocumentNo, "9")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(409));
        assertEquals(1L, count("wms_stock_documents"));
        assertStock("10.000000", "10.000000", "0.000000");

        mockMvc.perform(post("/material/wms/completion-inbound-reversals")
                .header("X-Tenant-Id", "COMP")
                .header("X-BPI-WMS-Key", "test-bpi-key")
                .contentType(MediaType.APPLICATION_JSON).content(reversal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.documentType")
                .value("COMPLETION_INBOUND_REVERSAL"))
            .andExpect(jsonPath("$.data.idempotent").value(false));

        mockMvc.perform(get("/material/wms/completion-inbound-reversals/by-idempotency")
                .header("X-Tenant-Id", "COMP")
                .header("X-BPI-WMS-Key", "test-bpi-key")
                .param("sourceSystem", "BPI")
                .param("idempotencyKey", reversalKey))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.document.document_type")
                .value("COMPLETION_INBOUND_REVERSAL"))
            .andExpect(jsonPath("$.data.document.source_document_id")
                .value(reversalEventId))
            .andExpect(jsonPath("$.data.document.idempotency_key").value(reversalKey))
            .andExpect(jsonPath("$.data.originalDocument.document_no")
                .value(originalDocumentNo))
            .andExpect(jsonPath("$.data.originalDocument.status").value("REVERSED"))
            .andExpect(jsonPath("$.data.lines[0].source_system").value("BPI"))
            .andExpect(jsonPath("$.data.lines[0].unit_code").value("kg"))
            .andExpect(jsonPath("$.data.transactions[0].transaction_type")
                .value("COMPLETION_INBOUND_REVERSAL"));

        mockMvc.perform(post("/material/wms/completion-inbound-reversals")
                .header("X-Tenant-Id", "COMP")
                .header("X-BPI-WMS-Key", "test-bpi-key")
                .contentType(MediaType.APPLICATION_JSON).content(reversal))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.idempotent").value(true));

        mockMvc.perform(post("/material/wms/completion-inbound-reversals")
                .header("X-Tenant-Id", "COMP")
                .header("X-BPI-WMS-Key", "test-bpi-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bpiReversalJson(
                    reversalEventId, reversalKey, originalDocumentNo, "9")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(post("/material/wms/completion-inbound-reversals")
                .header("X-Tenant-Id", "COMP")
                .header("X-BPI-WMS-Key", "test-bpi-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bpiReversalJson(
                    "4c405c19-79f1-5db8-b6f8-c54bb4684eb1",
                    reversalKey + "|OTHER", originalDocumentNo, "10")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(409));

        assertEquals(2L, count("wms_stock_documents"));
        assertEquals(2L, count("wms_stock_document_lines"));
        assertEquals(2L, count("wms_inventory_transactions"));
        assertEquals("REVERSED", jdbc.queryForObject(
            "SELECT status FROM wms_stock_documents WHERE idempotency_key = ?",
            String.class, inboundKey));
        assertEquals("POSTED", jdbc.queryForObject(
            "SELECT status FROM wms_stock_documents WHERE idempotency_key = ?",
            String.class, reversalKey));
        assertEquals(new BigDecimal("-10.000000"), jdbc.queryForObject(
            "SELECT on_hand_delta FROM wms_inventory_transactions "
                + "WHERE transaction_type = 'COMPLETION_INBOUND_REVERSAL'",
            BigDecimal.class));
        assertEquals(new BigDecimal("-10.000000"), jdbc.queryForObject(
            "SELECT available_delta FROM wms_inventory_transactions "
                + "WHERE transaction_type = 'COMPLETION_INBOUND_REVERSAL'",
            BigDecimal.class));
        assertStock("0.000000", "0.000000", "0.000000");
    }

    @Test
    public void bpiReversalWithConsumedStockRollsBackCompletely() throws Exception {
        String inboundKey = "WMS_COMPLETION_INBOUND|COMP|BATCH-CONSUMED|GATE-1|1";
        mockMvc.perform(post("/material/produceInSingles/produceInSingl/generateProductInSingle")
                .header("X-Tenant-Id", "COMP")
                .header("X-BPI-WMS-Key", "test-bpi-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bpiInboundJson(
                    "2ea229c2-f2bb-5da8-b84c-5b4bd00148ce", inboundKey, "10")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
        String originalDocumentNo = jdbc.queryForObject(
            "SELECT document_no FROM wms_stock_documents WHERE idempotency_key = ?",
            String.class, inboundKey);

        String issue = "{"
            + "\"srcId\":\"BPI_REVERSAL_CONSUMED_OUT\",\"companyCode\":\"COMP\","
            + "\"wareCode\":\"WARE\",\"storageDate\":\"2026-07-20\","
            + "\"comeType\":\"produceOut\",\"redBlue\":\"blue\",\"detailList\":[{"
            + "\"goodCode\":\"MAT\",\"batchText\":\"BATCH-1\","
            + "\"produceBatchNum\":\"BATCH-1\",\"placeSetCode\":\"LOC\","
            + "\"quantity\":3,\"unitCode\":\"kg\"}]}";
        mockMvc.perform(post("/material/produceOutSingle/produceOutSing/generateProduceOutSing")
                .contentType(MediaType.APPLICATION_JSON).content(issue))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
        assertStock("7.000000", "7.000000", "0.000000");

        String reversalKey = "WMS_COMPLETION_INBOUND_REVERSAL|COMP|BATCH-CONSUMED|GATE-1|1";
        mockMvc.perform(post("/material/wms/completion-inbound-reversals")
                .header("X-Tenant-Id", "COMP")
                .header("X-BPI-WMS-Key", "test-bpi-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bpiReversalJson(
                    "c9288339-f020-59cd-a50e-2e2855115582",
                    reversalKey, originalDocumentNo, "10")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(409));

        assertEquals(2L, count("wms_stock_documents"));
        assertEquals(2L, count("wms_stock_document_lines"));
        assertEquals(2L, count("wms_inventory_transactions"));
        assertEquals(0L, countWhere(
            "wms_stock_documents", "document_type = 'COMPLETION_INBOUND_REVERSAL'"));
        assertEquals("POSTED", jdbc.queryForObject(
            "SELECT status FROM wms_stock_documents WHERE idempotency_key = ?",
            String.class, inboundKey));
        assertStock("7.000000", "7.000000", "0.000000");
    }

    private String inboundJson(String sourceId, String sourceLineId, String quantity) {
        return "{"
            + "\"srcID\":\"" + sourceId + "\",\"srcTableNo\":\"IN-1\",\"directiveNo\":\"MO-1\","
            + "\"companyCode\":\"COMP\",\"wareCode\":\"WARE\",\"storageDate\":\"2026-07-10\","
            + "\"comeType\":\"produceIn\",\"redBlue\":\"blue\",\"detailList\":[{"
            + "\"srcPartId\":\"" + sourceLineId + "\",\"goodCode\":\"MAT\",\"batchText\":\"BATCH\","
            + "\"produceBatchNum\":\"PB\",\"placeSetCode\":\"LOC\",\"quantity\":" + quantity + "}]}";
    }

    private String bpiInboundJson(String commandEventId, String idempotencyKey, String quantity) {
        return "{"
            + "\"sourceSystem\":\"BPI\",\"idempotencyKey\":\"" + idempotencyKey + "\","
            + "\"srcID\":\"" + commandEventId + "\",\"srcTableNo\":\"BATCH-1\","
            + "\"directiveNo\":\"MO-1\",\"companyCode\":\"COMP\",\"wareCode\":\"WARE\","
            + "\"storageDate\":\"2026-07-20\",\"comeType\":\"produceIn\",\"redBlue\":\"blue\","
            + "\"detailList\":[{\"srcPartId\":\"" + commandEventId + ":1\","
            + "\"goodCode\":\"MAT\",\"batchText\":\"BATCH-1\",\"produceBatchNum\":\"BATCH-1\","
            + "\"placeSetCode\":\"LOC\",\"quantity\":" + quantity + ",\"unitCode\":\"kg\","
            + "\"checkResult\":\"BaseSet_checkResult/qualified\"}]}";
    }

    private String bpiReversalJson(
            String commandEventId,
            String idempotencyKey,
            String originalDocumentNo,
            String quantity) {
        return "{"
            + "\"sourceSystem\":\"BPI\",\"idempotencyKey\":\"" + idempotencyKey + "\","
            + "\"srcID\":\"" + commandEventId + "\","
            + "\"originalDocumentNo\":\"" + originalDocumentNo + "\","
            + "\"srcTableNo\":\"BATCH-1\",\"directiveNo\":\"MO-1\","
            + "\"companyCode\":\"COMP\",\"wareCode\":\"WARE\","
            + "\"storageDate\":\"2026-07-20\",\"comeType\":\"produceIn\","
            + "\"redBlue\":\"red\",\"detailList\":[{"
            + "\"srcPartId\":\"" + commandEventId + ":1\",\"goodCode\":\"MAT\","
            + "\"batchText\":\"BATCH-1\",\"produceBatchNum\":\"BATCH-1\","
            + "\"placeSetCode\":\"LOC\",\"quantity\":" + quantity + ","
            + "\"unitCode\":\"kg\","
            + "\"checkResult\":\"BaseSet_checkResult/qualified\"}]}";
    }

    private String allocationJson(
            String action,
            String requestId,
            String reportId,
            String sourceLineId,
            String total,
            String good,
            String bad) {
        return "{"
            + "\"requestId\":\"" + requestId + "\",\"action\":\"" + action + "\","
            + "\"qualityReportId\":\"" + reportId + "\",\"taskId\":\"TASK-1\","
            + "\"sourceLineId\":\"" + sourceLineId + "\","
            + "\"totalQuantity\":" + total + ",\"goodQuantity\":" + good + ","
            + "\"badQuantity\":" + bad + "}";
    }

    private long count(String table) {
        Long value = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return value == null ? 0L : value.longValue();
    }

    private long countWhere(String table, String predicate) {
        Long value = jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + table + " WHERE " + predicate, Long.class);
        return value == null ? 0L : value.longValue();
    }

    private void assertStock(String onHand, String available, String hold) {
        assertEquals(new BigDecimal(onHand), jdbc.queryForObject(
            "SELECT on_hand_quantity FROM wms_batch_stocks", BigDecimal.class));
        assertEquals(new BigDecimal(available), jdbc.queryForObject(
            "SELECT available_quantity FROM wms_batch_stocks", BigDecimal.class));
        assertEquals(new BigDecimal(hold), jdbc.queryForObject(
            "SELECT hold_quantity FROM wms_batch_stocks", BigDecimal.class));
    }
}
