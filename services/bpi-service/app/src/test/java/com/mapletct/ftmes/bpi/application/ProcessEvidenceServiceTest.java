package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.ProcessEvidenceView;
import com.mapletct.ftmes.bpi.infrastructure.postgres.ProcessEvidencePostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.ProcessEvidencePostgresRepository.EvidenceRow;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessEvidenceServiceTest {
    private static final Instant FROM = Instant.parse("2026-07-27T08:00:00Z");
    private static final Instant TO = FROM.plusSeconds(600);
    private static final ActorContext ACTOR = new ActorContext(
            "1000", "admin", Set.of("BPI_VIEWER"), Set.of("PLANT-01"), Set.of("LINE-S07-01"));

    @Test
    void returnsStatisticsAndDeterministicDownsampleForAuthorizedProcessWindow() {
        ProcessEvidencePostgresRepository repository = mock(ProcessEvidencePostgresRepository.class);
        List<EvidenceRow> rows = new ArrayList<>();
        for (int index = 0; index < 300; index++) {
            rows.add(row("jet.flow.instant", "FLOW-JET-01", "m3/h", index, (double) index));
        }
        rows.add(row("jet.feed.baume", "DENSITY-JET-01", "Be", 10, 19.5));
        rows.add(row("jet.feed.baume", "DENSITY-JET-01", "Be", 20, 20.5));
        when(repository.list(
                eq("1000"),
                eq("PLANT-01"),
                eq("LINE-S07-01"),
                eq(FROM),
                eq(TO),
                anyList(),
                anyInt())).thenReturn(rows);

        ProcessEvidenceView view = new ProcessEvidenceService(repository).get(
                ACTOR,
                "PLANT-01",
                "LINE-S07-01",
                "ORDER-01",
                FROM,
                TO,
                List.of("jet.flow.instant", "jet.feed.baume"));

        assertThat(view.contextInferred()).isTrue();
        assertThat(view.series()).hasSize(2);
        assertThat(view.series().get(0).sourceCount()).isEqualTo(300);
        assertThat(view.series().get(0).samples()).hasSize(240);
        assertThat(view.series().get(0).samples().get(0).numericValue()).isEqualTo(0);
        assertThat(view.series().get(0).samples().get(239).numericValue()).isEqualTo(299);
        assertThat(view.series().get(0).truncated()).isTrue();
        assertThat(view.series().get(0).minimum()).isEqualTo(0);
        assertThat(view.series().get(0).average()).isEqualTo(149.5);
        assertThat(view.series().get(0).maximum()).isEqualTo(299);
        assertThat(view.series().get(1).average()).isEqualTo(20);
    }

    @Test
    void rejectsUnauthorizedScopeAndOversizedTimeWindow() {
        ProcessEvidenceService service = new ProcessEvidenceService(
                mock(ProcessEvidencePostgresRepository.class));
        ActorContext wrongLine = new ActorContext(
                "1000", "admin", Set.of("BPI_VIEWER"), Set.of("PLANT-01"), Set.of("LINE-OTHER"));

        assertThatThrownBy(() -> service.get(
                wrongLine, "PLANT-01", "LINE-S07-01", "ORDER-01", FROM, TO, List.of()))
                .isInstanceOf(BpiForbiddenException.class);
        assertThatThrownBy(() -> service.get(
                ACTOR, "PLANT-01", "LINE-S07-01", "ORDER-01",
                FROM, FROM.plusSeconds(86_401), List.of()))
                .isInstanceOf(BpiValidationException.class)
                .hasMessageContaining("24 hours");
    }

    @Test
    void rejectsMalformedOrExcessivePropertyFiltersBeforeQuerying() {
        ProcessEvidenceService service = new ProcessEvidenceService(
                mock(ProcessEvidencePostgresRepository.class));
        assertThatThrownBy(() -> service.get(
                ACTOR, "PLANT-01", "LINE-S07-01", "ORDER-01",
                FROM, TO, List.of("../raw")))
                .isInstanceOf(BpiValidationException.class);
        assertThatThrownBy(() -> service.get(
                ACTOR, "PLANT-01", "LINE-S07-01", "ORDER-01",
                FROM, TO, java.util.stream.IntStream.range(0, 17)
                        .mapToObj(index -> "point." + index).toList()))
                .isInstanceOf(BpiValidationException.class)
                .hasMessageContaining("16");
    }

    private EvidenceRow row(
            String propertyId,
            String deviceId,
            String unit,
            int offsetSeconds,
            double value) {
        return new EvidenceRow(
                deviceId,
                propertyId,
                unit,
                FROM.plusSeconds(offsetSeconds),
                value,
                null,
                null,
                "GOOD");
    }
}
