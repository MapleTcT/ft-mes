package com.mapletct.ftmes.rmformula;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.rmformula.api.FormulaSaveRequest;
import com.mapletct.ftmes.rmformula.domain.RmFormulaBusinessException;
import com.mapletct.ftmes.rmformula.repository.FormulaEditorRepository;
import com.mapletct.ftmes.rmformula.service.FormulaEditorService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FormulaEditorServiceTest {
    @Mock
    private FormulaEditorRepository repository;
    private FormulaEditorService service;

    @Before
    public void setUp() {
        service = new FormulaEditorService(repository, new ObjectMapper());
        when(repository.materialExists(1001L)).thenReturn(true);
    }

    @Test
    public void createsFormulaChildrenAndRevision() {
        FormulaSaveRequest request = request("REQ-RM-FORMULA-001");
        Map<String, Object> formula = formula(9101L, 0);
        when(repository.revisionByRequest("default", request.getRequestId())).thenReturn(Collections.emptyMap());
        when(repository.formulaCodeUsed(request.getFormulaCode(), null)).thenReturn(false);
        when(repository.nextFormulaId()).thenReturn(9101L);
        when(repository.nextProcessId()).thenReturn(9201L);
        when(repository.nextActivityId()).thenReturn(9301L);
        when(repository.formula(9101L)).thenReturn(formula);
        when(repository.processes(9101L)).thenReturn(Collections.emptyList());
        when(repository.activities(9101L)).thenReturn(Collections.emptyList());
        when(repository.latestRevision(9101L)).thenReturn(Collections.emptyMap());
        when(repository.latestDelivery(9101L)).thenReturn(Collections.emptyMap());
        when(repository.insertRevision(eq("default"), eq(9101L), eq(request.getRequestId()), anyString(), eq(0), anyString()))
                .thenReturn(9401L);

        Map<String, Object> result = service.create("default", request);

        assertEquals(9401L, ((Number) result.get("revisionId")).longValue());
        assertEquals(Long.valueOf(9201L), request.getProcesses().get(0).getId());
        assertEquals(Long.valueOf(9301L), request.getActivities().get(0).getId());
        verify(repository).insertFormula(9101L, request);
        verify(repository).saveProcess(9101L, request.getProcesses().get(0));
        verify(repository).saveActivity(9101L, 9201L, request.getActivities().get(0));
    }

    @Test
    public void rejectsStaleVersionBeforeChangingChildren() {
        FormulaSaveRequest request = request("REQ-RM-FORMULA-002");
        request.setExpectedVersion(3);
        request.getProcesses().get(0).setId(9202L);
        request.getActivities().get(0).setId(9302L);
        when(repository.revisionByRequest("default", request.getRequestId())).thenReturn(Collections.emptyMap());
        when(repository.formula(9102L)).thenReturn(formula(9102L, 4));
        when(repository.formulaCodeUsed(request.getFormulaCode(), 9102L)).thenReturn(false);
        when(repository.processBelongs(9202L, 9102L)).thenReturn(true);
        when(repository.activityBelongs(9302L, 9102L)).thenReturn(true);
        when(repository.updateFormula(9102L, 3, request)).thenReturn(0);

        try {
            service.update("default", 9102L, request);
            fail("Expected optimistic lock conflict");
        } catch (RmFormulaBusinessException exception) {
            assertEquals(409, exception.getCode());
        }

        verify(repository, never()).retireProcesses(anyLong());
        verify(repository, never()).insertRevision(anyString(), anyLong(), anyString(), anyString(), any(Integer.class), anyString());
    }

    @Test
    public void rejectsActivityWhoseProcessKeyIsMissing() {
        FormulaSaveRequest request = request("REQ-RM-FORMULA-003");
        request.getActivities().get(0).setProcessKey("missing-process");
        when(repository.revisionByRequest("default", request.getRequestId())).thenReturn(Collections.emptyMap());
        when(repository.formulaCodeUsed(request.getFormulaCode(), null)).thenReturn(false);
        when(repository.nextFormulaId()).thenReturn(9103L);
        when(repository.nextProcessId()).thenReturn(9203L);
        when(repository.nextActivityId()).thenReturn(9303L);

        try {
            service.create("default", request);
            fail("Expected process reference validation");
        } catch (RmFormulaBusinessException exception) {
            assertEquals(400, exception.getCode());
            assertTrue(exception.getMessage().contains("工序"));
        }

        verify(repository, never()).insertFormula(anyLong(), any(FormulaSaveRequest.class));
    }

    @Test
    public void rejectsUnknownProductBeforeWritingFormula() {
        FormulaSaveRequest request = request("REQ-RM-FORMULA-004");
        request.setProductId(9999L);
        when(repository.materialExists(9999L)).thenReturn(false);

        try {
            service.create("default", request);
            fail("Expected product reference validation");
        } catch (RmFormulaBusinessException exception) {
            assertEquals(400, exception.getCode());
            assertTrue(exception.getMessage().contains("产品"));
        }

        verify(repository, never()).insertFormula(anyLong(), any(FormulaSaveRequest.class));
    }

    private static FormulaSaveRequest request(String requestId) {
        FormulaSaveRequest request = new FormulaSaveRequest();
        request.setRequestId(requestId);
        request.setFormulaCode("WEB-FORMULA-001");
        request.setFormulaName("Web formula");
        request.setFormulaEdition("1.0");
        request.setProductId(1001L);
        FormulaSaveRequest.ProcessInput process = new FormulaSaveRequest.ProcessInput();
        process.setClientKey("process-1");
        process.setName("Mixing");
        request.setProcesses(Collections.singletonList(process));
        FormulaSaveRequest.ActivityInput activity = new FormulaSaveRequest.ActivityInput();
        activity.setClientKey("activity-1");
        activity.setProcessKey("process-1");
        activity.setName("Charge material");
        request.setActivities(Collections.singletonList(activity));
        return request;
    }

    private static Map<String, Object> formula(long id, int version) {
        Map<String, Object> formula = new LinkedHashMap<String, Object>();
        formula.put("id", id);
        formula.put("version", version);
        formula.put("formulaCode", "WEB-FORMULA-001");
        formula.put("formulaName", "Web formula");
        return formula;
    }
}
