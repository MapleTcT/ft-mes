package com.mapletct.ftmes.bpi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.VersionComparisonService;
import com.mapletct.ftmes.bpi.domain.VersionComparisonView;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VersionComparisonServiceTest {
    private final VersionComparisonService service = new VersionComparisonService(new ObjectMapper());

    @Test
    void reportsStableJsonPointersForAddedRemovedAndChangedValues() {
        VersionComparisonView result = service.compare(
                "RULE_VERSION",
                reference("1.0.0"),
                Map.of("threshold", 10, "bindings", List.of("flow", "pump"), "obsolete", true),
                reference("1.1.0"),
                Map.of("threshold", 12, "bindings", List.of("flow", "valve"), "enabled", true));

        assertThat(result.identical()).isFalse();
        assertThat(result.changeCount()).isEqualTo(4);
        assertThat(result.truncated()).isFalse();
        assertThat(result.changes())
                .extracting(change -> change.path() + "|" + change.changeType())
                .containsExactly(
                        "/bindings/1|CHANGED",
                        "/enabled|ADDED",
                        "/obsolete|REMOVED",
                        "/threshold|CHANGED");
    }

    @Test
    void marksCanonicalContentAsIdentical() {
        Map<String, Object> content = Map.of("a", 1, "nested", Map.of("b", true));

        VersionComparisonView result = service.compare(
                "TOPOLOGY_VERSION", reference("1"), content, reference("2"), content);

        assertThat(result.identical()).isTrue();
        assertThat(result.changeCount()).isZero();
        assertThat(result.changes()).isEmpty();
    }

    private VersionComparisonView.VersionReference reference(String version) {
        return new VersionComparisonView.VersionReference(
                UUID.randomUUID(), "CONTROLLED", version, "DRAFT", "c".repeat(64));
    }
}
