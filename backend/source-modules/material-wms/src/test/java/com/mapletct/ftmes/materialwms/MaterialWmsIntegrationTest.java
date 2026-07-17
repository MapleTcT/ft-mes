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

    private String inboundJson(String sourceId, String sourceLineId, String quantity) {
        return "{"
            + "\"srcID\":\"" + sourceId + "\",\"srcTableNo\":\"IN-1\",\"directiveNo\":\"MO-1\","
            + "\"companyCode\":\"COMP\",\"wareCode\":\"WARE\",\"storageDate\":\"2026-07-10\","
            + "\"comeType\":\"produceIn\",\"redBlue\":\"blue\",\"detailList\":[{"
            + "\"srcPartId\":\"" + sourceLineId + "\",\"goodCode\":\"MAT\",\"batchText\":\"BATCH\","
            + "\"produceBatchNum\":\"PB\",\"placeSetCode\":\"LOC\",\"quantity\":" + quantity + "}]}";
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

    private void assertStock(String onHand, String available, String hold) {
        assertEquals(new BigDecimal(onHand), jdbc.queryForObject(
            "SELECT on_hand_quantity FROM wms_batch_stocks", BigDecimal.class));
        assertEquals(new BigDecimal(available), jdbc.queryForObject(
            "SELECT available_quantity FROM wms_batch_stocks", BigDecimal.class));
        assertEquals(new BigDecimal(hold), jdbc.queryForObject(
            "SELECT hold_quantity FROM wms_batch_stocks", BigDecimal.class));
    }
}
