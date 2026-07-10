package com.mapletct.ftmes.processanalysis;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;

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
public class ProcessAnalysisIntegrationTest {

    private static final String BATCH = "ADP_E2E_PROCESS_ANALYSIS_BATCH";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Before
    public void fixture() {
        for (String table : new String[] {
            "pa_trace_snapshots", "wms_inventory_transactions", "wms_stock_document_lines",
            "wms_stock_documents", "qcs_un_qlf_deals", "qcs_report_coms", "qcs_inspect_reports", "qcs_inspects",
            "baseset_batch_infos", "wom_mat_outpt_records", "wom_output_details", "wom_putin_details",
            "wom_acti_exelogs", "wom_task_actives", "wom_process_exelogs", "wom_task_processes",
            "wom_produce_task_exelog", "wom_produce_tasks", "baseset_materials"
        }) {
            jdbc.update("DELETE FROM " + table);
        }
        Timestamp now = Timestamp.valueOf("2026-07-10 08:00:00");
        jdbc.update("INSERT INTO baseset_materials VALUES (900,'MAT-900','追溯产品',true)");
        jdbc.update("INSERT INTO wom_produce_tasks VALUES (100,'TASK-100',?,900,'WOM_runState/finished',"
            + "'QCS_checkState/reported','合格',10,10,?,?,?, ?,true)", BATCH, now, now, now, now);
        jdbc.update("INSERT INTO wom_produce_task_exelog VALUES (101,100,'TASK-EXE-101',?,900,"
            + "'WOM_runState/finished','QCS_checkState/reported','合格',10,false,?,?,?,?)", BATCH, now, now, now, now);
        jdbc.update("INSERT INTO wom_task_processes VALUES (200,100,'PROC-200','混配',1,"
            + "'WOM_runState/finished',?,?,?, ?,?,1,true)", now, now, now, now, now);
        jdbc.update("INSERT INTO wom_process_exelogs VALUES (201,100,200,'PROC-EXE-201','混配',?,"
            + "'WOM_runState/finished',false,?,?,20,?,?,true)", BATCH, now, now, now, now);
        jdbc.update("INSERT INTO wom_task_actives VALUES (300,100,200,'ACTIVE-300','投料',"
            + "'RAW-BATCH-1',901,'WOM_runState/finished','QCS_checkState/reported','合格',5,5,1,?,?,?,?,true)",
            now, now, now, now);
        jdbc.update("INSERT INTO wom_acti_exelogs VALUES (301,100,200,300,'ACTIVE-EXE-301','投料',?,"
            + "'RAW-BATCH-1',901,'WOM_runState/finished',false,5,5,400,500,?,?,?,?,true)",
            BATCH, now, now, now, now);
        jdbc.update("INSERT INTO wom_putin_details VALUES (400,'PUTIN-400',901,'RAW-BATCH-1',5,5,?,?,1,1,true,?)",
            now, now, now);
        jdbc.update("INSERT INTO wom_output_details VALUES (500,'OUTPUT-500','OUT-BATCH-1',900,10,10,?,?,1,1,?)",
            now, now, now);
        jdbc.update("INSERT INTO wom_mat_outpt_records VALUES (501,'OUTPUT-REC-501',101,201,301,900,"
            + "'OUT-BATCH-1',?,10,?,?,1,1,?,true)", BATCH, now, now, now);
        jdbc.update("INSERT INTO baseset_batch_infos VALUES (600,'BATCH-600',?,900,'WOM',?,?,"
            + "'QCS_checkState/reported','合格','PASS',true,?,?,true)", BATCH, now, now, now, now);
        jdbc.update("INSERT INTO qcs_inspects VALUES (700,'INSPECT-700',100,'QCS_sourceType/womComplete',"
            + "'TASK-100',?,900,10,'QCS_checkState/reported',false,?,?,?,true)", BATCH, now, now, now);
        jdbc.update("INSERT INTO qcs_inspect_reports VALUES (701,'REPORT-701',700,?,900,'合格',"
            + "'Qualified',false,?,?,?,true)", BATCH, now, now, now);
        jdbc.update("INSERT INTO qcs_report_coms VALUES (703,701,'含量','99.8','合格','%',99,100,"
            + "'99-100',1,?,?,true)", now, now);
        jdbc.update("INSERT INTO qcs_un_qlf_deals VALUES (702,'DEAL-702',701,?,900,'抽样偏差',?,99,?,?,?,true)",
            BATCH, now, now, now, now);
        jdbc.update("INSERT INTO wms_stock_documents VALUES (800,'default','CIN-800','COMPLETION_INBOUND','100',"
            + "'TASK-100','TASK-100','WARE',DATE '2026-07-10','POSTED','QUALIFIED',?,?)", now, now);
        jdbc.update("INSERT INTO wms_stock_document_lines VALUES (801,'default',800,'LINE-801','MAT-900',?,?,'WARE',"
            + "'LOC',10,'QUALIFIED',?,?)", BATCH, BATCH, now, now);
        jdbc.update("INSERT INTO wms_inventory_transactions VALUES (802,'default','EVENT-802','COMPLETION_INBOUND','100',"
            + "'LINE-801','MAT-900',?,?,10,10,0,10,10,0,?)", BATCH, BATCH, now);
        jdbc.update("INSERT INTO wms_inventory_transactions VALUES (803,'JWTCOMP','EVENT-803','COMPLETION_INBOUND','100',"
            + "'LINE-803','MAT-900',?,?,5,5,0,5,5,0,?)", BATCH, BATCH, now);
        jdbc.update("INSERT INTO wms_inventory_transactions VALUES (804,'JWTCOMP','EVENT-804','QUALITY_RELEASE','100',"
            + "'LINE-804','MAT-900',?,?,0,5,-5,5,5,0,?)", BATCH, BATCH, now);
    }

    @Test
    public void traceReadsRealSourceFactsAndRendersOperationalPage() throws Exception {
        mockMvc.perform(get("/analysisParam/analysisParam/isProdprocessView").param("batchNo", BATCH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.dealRes").value(true))
            .andExpect(jsonPath("$.data.taskId").value(100));

        mockMvc.perform(get("/ProcessAnalysis/analysisParam/analysisParam/isProdprocessView").param("batchNo", BATCH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.dealRes").value(true));

        mockMvc.perform(get("/processAnalysis/api/trace").param("batchNo", BATCH).param("productNo", "MAT-900"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.task.table_no").value("TASK-100"))
            .andExpect(jsonPath("$.data.summary.processCount").value(1))
            .andExpect(jsonPath("$.data.summary.activityCount").value(1))
            .andExpect(jsonPath("$.data.summary.qualityEventCount").value(4))
            .andExpect(jsonPath("$.data.summary.inventoryEventCount").value(1))
            .andExpect(jsonPath("$.data.materials.lineage[0].fromBatch").value("RAW-BATCH-1"))
            .andExpect(jsonPath("$.data.quality.reports[0].check_result").value("合格"))
            .andExpect(jsonPath("$.data.quality.reportItems[0].report_name").value("含量"));

        mockMvc.perform(get("/processAnalysis/api/trace")
                .header("Authorization", "Bearer e30.eyJ0ZW5hbnRJZCI6IkpXVENPTVAifQ.signature")
                .param("batchNo", BATCH).param("productNo", "MAT-900"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.summary.inventoryEventCount").value(2));

        mockMvc.perform(get("/processAnalysis/exelogSecond/processBatchViewOut").param("batchNo", BATCH))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("生产过程追溯")))
            .andExpect(content().string(containsString("/processAnalysis/api/trace")));
    }

    @Test
    public void legacyManualStatisticsPersistIdempotentSnapshots() throws Exception {
        mockMvc.perform(get("/paramDetail/paramDetail/analysisiTask")
                .header("X-Tenant-Id", "COMP").param("taskExeLogId", "101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.success").value(true))
            .andExpect(jsonPath("$.data.snapshot.source_type").value("TASK"));
        mockMvc.perform(get("/paramStatRec/paramStatRec/manualStatProcess")
                .header("X-Tenant-Id", "COMP").param("processId", "201"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/paramStatRec/paramStatRec/manualStatActive")
                .header("X-Tenant-Id", "COMP").param("activeId", "301"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/paramDetail/paramDetail/analysisiTask")
                .header("X-Tenant-Id", "COMP").param("taskExeLogId", "101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.snapshot.revision").value(2));

        assertEquals(3L, jdbc.queryForObject("SELECT count(*) FROM pa_trace_snapshots", Long.class).longValue());
        assertEquals(2L, jdbc.queryForObject(
            "SELECT revision FROM pa_trace_snapshots WHERE source_type='TASK'", Long.class).longValue());

        mockMvc.perform(post("/produceTask/paPrExeLog/paPrExeLogList-query")
                .header("X-Tenant-Id", "COMP"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.rows[0].source_id").value(201));
    }

    @Test
    public void missingTraceAndExecutionReturnExplicitLegacyFailures() throws Exception {
        mockMvc.perform(get("/analysisParam/analysisParam/isProdprocessView").param("batchNo", "MISSING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.dealRes").value(false));
        mockMvc.perform(get("/paramStatRec/paramStatRec/manualStatActive").param("activeId", "999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(get("/paramDetail/paramDetail/analysisiTask").param("taskExeLogId", "999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.data.success").value(false));
        mockMvc.perform(get("/paramDetail/paramDetail/analysisiTask"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.data.success").value(false));
    }
}
