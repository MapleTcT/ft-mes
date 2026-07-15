ALTER TABLE bpi.bpi_outbox_events
    ADD COLUMN runtime_readiness_status varchar(16) NOT NULL DEFAULT 'WAITING'
        CHECK (runtime_readiness_status IN ('WAITING', 'READY', 'DEGRADED', 'INACTIVE')),
    ADD COLUMN runtime_readiness_event_id varchar(512),
    ADD COLUMN runtime_readiness_deployment_id varchar(128),
    ADD COLUMN runtime_readiness_observed_at timestamptz,
    ADD COLUMN runtime_readiness_received_at timestamptz,
    ADD COLUMN runtime_readiness_reason_code varchar(128),
    ADD COLUMN runtime_readiness_detail varchar(1000),
    ADD COLUMN runtime_point_catalog_event_id varchar(512),
    ADD COLUMN runtime_point_catalog_source_revision varchar(128);

CREATE UNIQUE INDEX uq_bpi_outbox_runtime_readiness_event
    ON bpi.bpi_outbox_events (tenant_id, runtime_readiness_event_id)
    WHERE runtime_readiness_event_id IS NOT NULL;
