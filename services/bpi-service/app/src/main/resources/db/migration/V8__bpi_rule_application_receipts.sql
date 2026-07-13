ALTER TABLE bpi.bpi_outbox_events
    ADD COLUMN application_status varchar(16) NOT NULL DEFAULT 'WAITING'
        CHECK (application_status IN ('WAITING', 'APPLIED', 'REJECTED')),
    ADD COLUMN application_event_id varchar(512),
    ADD COLUMN application_deployment_id varchar(128),
    ADD COLUMN application_observed_at timestamptz,
    ADD COLUMN application_received_at timestamptz,
    ADD COLUMN application_error_code varchar(128),
    ADD COLUMN application_error_detail varchar(1000);

CREATE UNIQUE INDEX uq_bpi_outbox_application_event
    ON bpi.bpi_outbox_events (tenant_id, application_event_id)
    WHERE application_event_id IS NOT NULL;
