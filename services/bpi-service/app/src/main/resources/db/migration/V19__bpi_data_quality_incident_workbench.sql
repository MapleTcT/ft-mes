CREATE TABLE bpi.bpi_data_quality_incidents (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    plant_id varchar(64) NOT NULL,
    line_id varchar(128) NOT NULL,
    source varchar(128) NOT NULL,
    device_id varchar(128) NOT NULL DEFAULT '',
    property_id varchar(128) NOT NULL DEFAULT '',
    issue_code varchar(128) NOT NULL,
    severity varchar(16) NOT NULL CHECK (severity IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL')),
    state varchar(24) NOT NULL CHECK (state IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')),
    revision bigint NOT NULL CHECK (revision > 0),
    event_count bigint NOT NULL CHECK (event_count > 0),
    first_seen timestamptz NOT NULL,
    last_seen timestamptz NOT NULL,
    last_event_id varchar(256) NOT NULL,
    last_source_event_id varchar(256),
    last_detail text NOT NULL,
    assignee varchar(128),
    acknowledged_by varchar(128),
    acknowledged_at timestamptz,
    acknowledgment_reason varchar(500),
    resolved_by varchar(128),
    resolved_at timestamptz,
    resolution_reason varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_bpi_data_quality_incident_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_bpi_data_quality_incident_identity UNIQUE
        (tenant_id, plant_id, line_id, source, device_id, property_id, issue_code),
    CONSTRAINT chk_bpi_data_quality_incident_time CHECK (last_seen >= first_seen),
    CONSTRAINT chk_bpi_data_quality_incident_workflow CHECK (
        (state = 'OPEN'
            AND assignee IS NULL
            AND acknowledged_by IS NULL AND acknowledged_at IS NULL AND acknowledgment_reason IS NULL
            AND resolved_by IS NULL AND resolved_at IS NULL AND resolution_reason IS NULL)
        OR
        (state = 'ACKNOWLEDGED'
            AND assignee IS NOT NULL
            AND acknowledged_by IS NOT NULL AND acknowledged_at IS NOT NULL AND acknowledgment_reason IS NOT NULL
            AND resolved_by IS NULL AND resolved_at IS NULL AND resolution_reason IS NULL)
        OR
        (state = 'RESOLVED'
            AND assignee IS NOT NULL
            AND acknowledged_by IS NOT NULL AND acknowledged_at IS NOT NULL AND acknowledgment_reason IS NOT NULL
            AND resolved_by IS NOT NULL AND resolved_at IS NOT NULL AND resolution_reason IS NOT NULL)
    )
);

CREATE INDEX idx_bpi_data_quality_incident_queue
    ON bpi.bpi_data_quality_incidents
       (tenant_id, plant_id, state, last_seen DESC, id DESC);

CREATE INDEX idx_bpi_data_quality_incident_line_issue
    ON bpi.bpi_data_quality_incidents
       (tenant_id, plant_id, line_id, issue_code, source, property_id);

CREATE TABLE bpi.bpi_data_quality_incident_events (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    incident_id uuid NOT NULL,
    event_id varchar(256) NOT NULL,
    source_event_id varchar(256),
    severity varchar(16) NOT NULL CHECK (severity IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL')),
    detail text NOT NULL,
    detected_at timestamptz NOT NULL,
    headers jsonb NOT NULL DEFAULT '{}'::jsonb,
    received_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_bpi_data_quality_event_incident_tenant
        FOREIGN KEY (tenant_id, incident_id)
        REFERENCES bpi.bpi_data_quality_incidents (tenant_id, id),
    CONSTRAINT uq_bpi_data_quality_event UNIQUE (tenant_id, event_id)
);

CREATE INDEX idx_bpi_data_quality_event_timeline
    ON bpi.bpi_data_quality_incident_events
       (tenant_id, incident_id, detected_at DESC, id DESC);

CREATE TABLE bpi.bpi_data_quality_incident_actions (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    incident_id uuid NOT NULL,
    incident_revision bigint NOT NULL CHECK (incident_revision > 0),
    action varchar(32) NOT NULL CHECK (action IN ('CREATED', 'REOPENED', 'ACKNOWLEDGED', 'REASSIGNED', 'RESOLVED')),
    from_state varchar(24),
    to_state varchar(24) NOT NULL CHECK (to_state IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')),
    actor_id varchar(128) NOT NULL,
    assignee varchar(128),
    reason varchar(500) NOT NULL,
    trace_id varchar(128) NOT NULL,
    action_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_bpi_data_quality_action_incident_tenant
        FOREIGN KEY (tenant_id, incident_id)
        REFERENCES bpi.bpi_data_quality_incidents (tenant_id, id),
    CONSTRAINT uq_bpi_data_quality_action_revision UNIQUE
        (tenant_id, incident_id, incident_revision)
);

CREATE INDEX idx_bpi_data_quality_action_timeline
    ON bpi.bpi_data_quality_incident_actions
       (tenant_id, incident_id, action_at DESC, id DESC);

CREATE INDEX idx_bpi_batches_data_quality_impact
    ON bpi.bpi_batch_instances
       (tenant_id, plant_id, line_id, start_time, end_time);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT, UPDATE ON bpi.bpi_data_quality_incidents TO bpi_service;
        GRANT SELECT, INSERT ON bpi.bpi_data_quality_incident_events TO bpi_service;
        GRANT SELECT, INSERT ON bpi.bpi_data_quality_incident_actions TO bpi_service;
    END IF;
END
$$;
