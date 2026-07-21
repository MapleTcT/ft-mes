package com.mapletct.ftmes.qcsoutbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.contract.v1.QcsInspectionDispositionV1;
import com.mapletct.ftmes.bpi.contract.v1.QcsQualityGateV1;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class QcsQualityGateProjectorTest {

    private final QcsQualityGateProjector projector = new QcsQualityGateProjector(new ObjectMapper());

    @Test
    public void projectsAcceptedGateWithCanonicalBpiReleaseIdentity() {
        QcsQualityGateV1 event = projector.project(record(1, acceptedInspections()), batch("CLOSED_RAW"));

        assertEquals("qcs-gate:1000:4001:2", event.getEventId());
        assertEquals("c1584e53-2780-4f58-bb34-9c7399a54d01", event.getBatchId());
        assertEquals("125.5", event.getReleaseQuantityDecimal());
        assertEquals("t", event.getQuantityUnit());
        assertEquals("SUGAR-FG-001", event.getMaterialCode());
        assertEquals(2, event.getInspectionsCount());
        assertEquals(
            QcsInspectionDispositionV1.QCS_INSPECTION_ACCEPTED,
            event.getInspections(1).getDisposition());
        assertEquals("QCS", event.getHeadersMap().get("source"));
        assertEquals("5001", event.getHeadersMap().get("qcs_report_id"));
    }

    @Test
    public void rejectedGateDoesNotClaimReleasedQuantity() {
        QcsQualityGateV1 event = projector.project(record(1, rejectedInspections()), batch("WAIT_QA"));

        assertEquals("", event.getReleaseQuantityDecimal());
        assertEquals(
            QcsInspectionDispositionV1.QCS_INSPECTION_REJECTED,
            event.getInspections(1).getDisposition());
    }

    @Test(expected = PermanentQcsOutboxException.class)
    public void rejectsDuplicateInspectionCodesBeforeKafkaPublication() {
        projector.project(record(1,
            "[{\"inspectionCode\":\"POL\",\"inspectionRecordId\":\"6001\","
                + "\"required\":true,\"disposition\":\"ACCEPTED\",\"finalResult\":true,"
                + "\"observedAtMs\":1784590000000},{\"inspectionCode\":\"POL\","
                + "\"inspectionRecordId\":\"6002\",\"required\":true,"
                + "\"disposition\":\"ACCEPTED\",\"finalResult\":true,"
                + "\"observedAtMs\":1784590000000}]"), batch("CLOSED_RAW"));
    }

    @Test(expected = PermanentQcsOutboxException.class)
    public void acceptedGateFailsClosedWithoutCanonicalPositiveQuantity() {
        ResolvedBpiBatch batch = batch("CLOSED_RAW");
        batch.setQuantity(BigDecimal.ZERO);
        projector.project(record(1, acceptedInspections()), batch);
    }

    static QcsQualityGateOutboxRecord record(int attemptCount, String inspections) {
        return record(attemptCount, inspections, "qcs.batch.quality-gate.v1");
    }

    static QcsQualityGateOutboxRecord record(int attemptCount, String inspections, String topic) {
        return new QcsQualityGateOutboxRecord(
            42L,
            "qcs-gate:1000:4001:2",
            "qcs-gate:1000:4001:2",
            topic,
            5001L,
            2,
            4001L,
            3001L,
            "1000",
            "PLANT-01",
            "LINE-S07-01",
            "ADP_E2E_QCS_OUTBOX_ORDER",
            "ADP_E2E_QCS_OUTBOX_BATCH",
            "qcs-inspect:4001",
            2L,
            1784590000000L,
            inspections,
            attemptCount
        );
    }

    static ResolvedBpiBatch batch(String state) {
        ResolvedBpiBatch batch = new ResolvedBpiBatch();
        batch.setId("c1584e53-2780-4f58-bb34-9c7399a54d01");
        batch.setBatchNo("BPI-QCS-0001");
        batch.setTenantId("1000");
        batch.setPlantId("PLANT-01");
        batch.setLineId("LINE-S07-01");
        batch.setOrderId("ADP_E2E_QCS_OUTBOX_ORDER");
        batch.setMaterialCode("SUGAR-FG-001");
        batch.setState(state);
        batch.setRevision(7L);
        batch.setQuantity(new BigDecimal("125.500"));
        batch.setQuantityUnit("t");
        return batch;
    }

    static String acceptedInspections() {
        return "[{\"inspectionCode\":\"POL\",\"inspectionRecordId\":\"6001\","
            + "\"required\":true,\"disposition\":\"ACCEPTED\",\"finalResult\":true,"
            + "\"observedAtMs\":1784590000000},{\"inspectionCode\":\"MOISTURE\","
            + "\"inspectionRecordId\":\"6002\",\"required\":true,"
            + "\"disposition\":\"ACCEPTED\",\"finalResult\":true,"
            + "\"observedAtMs\":1784590000000}]";
    }

    static String rejectedInspections() {
        return "[{\"inspectionCode\":\"POL\",\"inspectionRecordId\":\"6001\","
            + "\"required\":true,\"disposition\":\"ACCEPTED\",\"finalResult\":true,"
            + "\"observedAtMs\":1784590000000},{\"inspectionCode\":\"MOISTURE\","
            + "\"inspectionRecordId\":\"6002\",\"required\":true,"
            + "\"disposition\":\"REJECTED\",\"finalResult\":true,"
            + "\"observedAtMs\":1784590000000}]";
    }
}
