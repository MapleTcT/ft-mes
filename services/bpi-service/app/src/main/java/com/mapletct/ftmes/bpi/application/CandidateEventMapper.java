package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.contract.validation.BpiContractValidator;
import com.mapletct.ftmes.bpi.contract.validation.ContractViolation;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.contract.v1.CandidateEvidenceV1;
import com.mapletct.ftmes.bpi.domain.BoundaryType;
import com.mapletct.ftmes.bpi.domain.EvidenceView;
import com.mapletct.ftmes.bpi.interfaces.rest.CandidateIngestRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class CandidateEventMapper {

    public CandidateIngestRequest toRequest(ActorContext actor, BatchCandidateV1 event) {
        List<ContractViolation> violations = BpiContractValidator.validate(event);
        if (!violations.isEmpty()) {
            throw new BpiValidationException(describe(violations));
        }
        if (!actor.tenantId().equals(event.getTenantId())) {
            throw new BpiForbiddenException("Candidate event tenant does not match the trusted token.");
        }
        if (event.getEvidenceCount() == 0) {
            throw new BpiValidationException("Candidate event requires detailed evidence for persistence.");
        }
        String topologyCode = event.getHeadersMap().get("topology_code");
        if (topologyCode == null || topologyCode.isBlank()) {
            throw new BpiValidationException("Candidate event header topology_code is required.");
        }
        UUID candidateKey;
        try {
            candidateKey = UUID.fromString(event.getCandidateKey());
        } catch (IllegalArgumentException error) {
            throw new BpiValidationException("Candidate key must be a UUID.");
        }
        List<EvidenceView> evidence = new ArrayList<>();
        for (CandidateEvidenceV1 item : event.getEvidenceList()) {
            evidence.add(new EvidenceView(
                    item.getEventId(),
                    item.getSignal(),
                    item.getClassification(),
                    item.getSatisfied(),
                    nullable(item.getValue()),
                    nullable(item.getUnit()),
                    item.getQualityCode(),
                    Instant.ofEpochMilli(item.getEventTimeMs()),
                    item.getSource()));
        }
        return new CandidateIngestRequest(
                event.getEventId(),
                candidateKey,
                event.getPlantId(),
                event.getLineId(),
                boundaryType(event),
                nullable(event.getContextOrderId()),
                Instant.ofEpochMilli(event.getBoundaryEventTimeMs()),
                event.getRuleCode(),
                event.getRuleVersion(),
                topologyCode,
                event.getTopologyVersion(),
                BigDecimal.valueOf(event.getConfidence()),
                List.copyOf(evidence),
                event.getMissingSignalsList());
    }

    private static BoundaryType boundaryType(BatchCandidateV1 event) {
        return switch (event.getBoundaryType()) {
            case START -> BoundaryType.START;
            case END -> BoundaryType.END;
            default -> throw new BpiValidationException("Candidate boundary type must be START or END.");
        };
    }

    private static String nullable(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String describe(List<ContractViolation> violations) {
        ContractViolation first = violations.get(0);
        return "Invalid candidate event at " + first.getPath() + ": " + first.getMessage();
    }
}
