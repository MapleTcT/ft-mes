ALTER TABLE bpi.bpi_outbox_events
    DROP CONSTRAINT IF EXISTS bpi_outbox_events_tenant_id_aggregate_type_aggregate_id_eve_key;

ALTER TABLE bpi.bpi_outbox_events
    ADD COLUMN lifecycle_action varchar(16) NOT NULL DEFAULT 'ACTIVATE'
        CHECK (lifecycle_action IN ('ACTIVATE', 'RETIRE')),
    ADD COLUMN lifecycle_sequence bigint NOT NULL DEFAULT 1
        CHECK (lifecycle_sequence > 0),
    ADD COLUMN lifecycle_active boolean NOT NULL DEFAULT true;

CREATE UNIQUE INDEX uq_bpi_outbox_rule_lifecycle
    ON bpi.bpi_outbox_events
        (tenant_id, aggregate_type, aggregate_id, event_type, lifecycle_sequence);

CREATE INDEX idx_bpi_outbox_rule_lifecycle_latest
    ON bpi.bpi_outbox_events
        (tenant_id, aggregate_id, lifecycle_sequence DESC)
    WHERE aggregate_type = 'RULE_VERSION'
      AND event_type = 'BOUNDARY_RULE_PUBLISHED';

CREATE UNIQUE INDEX uq_bpi_rule_single_published_version
    ON bpi.bpi_rule_versions (tenant_id, plant_id, line_id, rule_code)
    WHERE state = 'PUBLISHED'
      AND plant_id IS NOT NULL
      AND line_id IS NOT NULL;
