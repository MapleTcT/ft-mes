package com.mapletct.ftmes.womquality;

import com.mapletct.ftmes.womquality.integration.MaterialWmsClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class WomQualityReportingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockBean
    private MaterialWmsClient materialWmsClient;

    @Before
    public void setUp() {
        jdbc.update("DELETE FROM wom_quality_quantity_events");
        jdbc.update("DELETE FROM wom_quality_quantity_reports");
        jdbc.update("DELETE FROM qcs_un_qlf_deals");
        jdbc.update("DELETE FROM qcs_inspect_reports");
        jdbc.update("DELETE FROM qcs_inspects");
        jdbc.update("DELETE FROM wom_mat_outpt_records");
        jdbc.update("DELETE FROM wom_produce_task_exelog");
        jdbc.update("DELETE FROM wom_produce_tasks");

        jdbc.update("INSERT INTO wom_produce_tasks "
            + "(id, table_no, produce_batch_num, plan_num, finish_num, task_run_state, valid) "
            + "VALUES (9000000000000001, 'TASK-001', 'BATCH-001', 10, 10, 'RUNNING', TRUE)");
        jdbc.update("INSERT INTO wom_produce_task_exelog (id, task_id, valid) "
            + "VALUES (9000000000000002, 9000000000000001, TRUE)");
        jdbc.update("INSERT INTO wom_mat_outpt_records "
            + "(id, table_no, task_exelog_id, report_num, output_num, mat_batch_num, produce_batch_num, valid) "
            + "VALUES (9000000000000003, 'OUT-001', 9000000000000002, 10, 10, 'BATCH-001', 'BATCH-001', TRUE)");
        jdbc.update("INSERT INTO qcs_inspects (id, source_id, valid) "
            + "VALUES (9000000000000004, 9000000000000001, TRUE)");
        jdbc.update("INSERT INTO qcs_inspect_reports (id, inspect_id, valid) "
            + "VALUES (9000000000000005, 9000000000000004, TRUE)");
        jdbc.update("INSERT INTO qcs_un_qlf_deals (id, report_id, valid) "
            + "VALUES (9000000000000006, 9000000000000005, TRUE)");

        when(materialWmsClient.apply(
            ArgumentMatchers.anyString(), ArgumentMatchers.anyMap(), ArgumentMatchers.anyString()))
            .thenReturn(Collections.<String, Object>singletonMap("status", "ACTIVE"));
    }

    @Test
    public void createIdempotentListAndReverseCloseThePersistenceChain() throws Exception {
        String request = createJson("REQ-001", "2");
        mockMvc.perform(post("/msService/WOM/quality-quantity/reports")
                .header("X-Tenant-Id", "default")
                .header("X-User-Name", "tester")
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").isString())
            .andExpect(jsonPath("$.data.reported_quantity").value(10))
            .andExpect(jsonPath("$.data.good_quantity").value(8))
            .andExpect(jsonPath("$.data.bad_quantity").value(2))
            .andExpect(jsonPath("$.data.wms_sync_state").value("APPLIED"))
            .andExpect(jsonPath("$.data.qcs_inspect_id").value("9000000000000004"));

        mockMvc.perform(post("/msService/WOM/quality-quantity/reports")
                .header("X-Tenant-Id", "default")
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        assertEquals(1L, count("wom_quality_quantity_reports"));
        assertEquals(new BigDecimal("8.000000"), jdbc.queryForObject(
            "SELECT good_quantity FROM wom_quality_quantity_reports", BigDecimal.class));
        assertEquals(2L, count("wom_quality_quantity_events"));

        mockMvc.perform(get("/msService/WOM/quality-quantity/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].id").value("9000000000000001"))
            .andExpect(jsonPath("$.data.items[0].bad_quantity").value(2));
        mockMvc.perform(get("/msService/WOM/quality-quantity/tasks/9000000000000001/outputs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value("9000000000000003"))
            .andExpect(jsonPath("$.data[0].active_report_id").isString());
        mockMvc.perform(get("/msService/WOM/quality-quantity/reports"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.items[0].task_id").value("9000000000000001"));
        mockMvc.perform(get("/msService/WOM/quality-quantity/reports")
                .param("taskId", "9000000000000001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1));
        mockMvc.perform(get("/msService/WOM/quality-quantity/quality-context/9000000000000004"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.qcs_inspect_id").value("9000000000000004"))
            .andExpect(jsonPath("$.data.task_id").value("9000000000000001"))
            .andExpect(jsonPath("$.data.task_no").value("TASK-001"));

        long reportId = jdbc.queryForObject(
            "SELECT id FROM wom_quality_quantity_reports", Long.class);
        long version = jdbc.queryForObject(
            "SELECT version FROM wom_quality_quantity_reports", Long.class);
        mockMvc.perform(post("/msService/WOM/quality-quantity/reports/" + reportId + "/reverse")
                .header("X-Tenant-Id", "default")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":" + version + ",\"reason\":\"录入修正\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REVERSED"))
            .andExpect(jsonPath("$.data.wms_sync_state").value("APPLIED"));

        assertEquals("REVERSED", jdbc.queryForObject(
            "SELECT status FROM wom_quality_quantity_reports", String.class));
        assertEquals(4L, count("wom_quality_quantity_events"));
    }

    @Test
    public void conflictingRequestAndExcessBadQuantityAreRejected() throws Exception {
        mockMvc.perform(post("/msService/WOM/quality-quantity/reports")
                .contentType(MediaType.APPLICATION_JSON).content(createJson("REQ-CONFLICT", "2")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/msService/WOM/quality-quantity/reports")
                .contentType(MediaType.APPLICATION_JSON).content(createJson("REQ-CONFLICT", "3")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(409));
        mockMvc.perform(post("/msService/WOM/quality-quantity/reports")
                .contentType(MediaType.APPLICATION_JSON).content(createJson("REQ-TOO-MUCH", "11")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));
        assertEquals(1L, count("wom_quality_quantity_reports"));
    }

    @Test
    public void operationalPageIsVisible() throws Exception {
        mockMvc.perform(get("/msService/WOM/quality-quantity/page"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("不良数量登记")))
            .andExpect(content().string(containsString("/msService/WOM/quality-quantity")));
    }

    @Test
    public void qualityContextRejectsInspectionWithoutManufacturingTask() throws Exception {
        mockMvc.perform(get("/msService/WOM/quality-quantity/quality-context/9999999999999999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    public void outputQuantityIsAuthoritativeWhenReportQuantityIsMissing() throws Exception {
        jdbc.update("UPDATE wom_mat_outpt_records SET report_num = NULL, output_num = 7 "
            + "WHERE id = 9000000000000003");

        mockMvc.perform(get("/msService/WOM/quality-quantity/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].reported_quantity").value(7));

        mockMvc.perform(post("/msService/WOM/quality-quantity/reports")
                .contentType(MediaType.APPLICATION_JSON).content(createJson("REQ-OUTPUT-FALLBACK", "2")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.reported_quantity").value(7))
            .andExpect(jsonPath("$.data.good_quantity").value(5));
    }

    private String createJson(String requestId, String badQuantity) {
        return "{"
            + "\"requestId\":\"" + requestId + "\","
            + "\"taskId\":\"9000000000000001\","
            + "\"sourceOutputId\":\"9000000000000003\","
            + "\"badQuantity\":" + badQuantity + ","
            + "\"unitCode\":\"kg\",\"reasonCode\":\"PROCESS_DEFECT\","
            + "\"reasonText\":\"process deviation\"}";
    }

    private long count(String table) {
        Long value = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return value == null ? 0L : value.longValue();
    }
}
