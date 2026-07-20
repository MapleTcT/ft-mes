CREATE TABLE bpi.bpi_quality_gates (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    batch_id uuid NOT NULL,
    external_gate_id varchar(256) NOT NULL,
    external_revision bigint NOT NULL CHECK (external_revision > 0),
    source_event_id varchar(256) NOT NULL,
    payload_checksum varchar(128) NOT NULL,
    state varchar(16) NOT NULL CHECK (state IN ('WAITING', 'ACCEPTED', 'REJECTED')),
    release_quantity numeric(24,6),
    quantity_unit varchar(32),
    material_code varchar(128),
    observed_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_bpi_quality_gate_batch_tenant
        FOREIGN KEY (tenant_id, batch_id)
        REFERENCES bpi.bpi_batch_instances (tenant_id, id),
    CONSTRAINT chk_bpi_quality_gate_release_quantity
        CHECK (release_quantity IS NULL OR release_quantity > 0),
    CONSTRAINT uq_bpi_quality_gate_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uq_bpi_quality_gate_batch UNIQUE (tenant_id, batch_id),
    CONSTRAINT uq_bpi_quality_gate_external UNIQUE (tenant_id, external_gate_id),
    CONSTRAINT uq_bpi_quality_gate_event UNIQUE (tenant_id, source_event_id)
);

CREATE TABLE bpi.bpi_quality_links (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    batch_id uuid NOT NULL,
    quality_gate_id uuid NOT NULL,
    inspection_code varchar(128) NOT NULL,
    inspection_record_id varchar(256) NOT NULL,
    required boolean NOT NULL,
    disposition varchar(16) NOT NULL CHECK (disposition IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    final_result boolean NOT NULL,
    observed_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_bpi_quality_link_batch_tenant
        FOREIGN KEY (tenant_id, batch_id)
        REFERENCES bpi.bpi_batch_instances (tenant_id, id),
    CONSTRAINT fk_bpi_quality_link_gate_tenant
        FOREIGN KEY (tenant_id, quality_gate_id)
        REFERENCES bpi.bpi_quality_gates (tenant_id, id),
    CONSTRAINT uq_bpi_quality_link_gate_code
        UNIQUE (tenant_id, quality_gate_id, inspection_code)
);

CREATE INDEX idx_bpi_quality_links_batch
    ON bpi.bpi_quality_links (tenant_id, batch_id, required, disposition);

ALTER TABLE bpi.bpi_outbox_events
    ADD CONSTRAINT uq_bpi_outbox_tenant_id UNIQUE (tenant_id, id);

CREATE TABLE bpi.bpi_wms_inbound_links (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    batch_id uuid NOT NULL,
    command_event_id uuid NOT NULL,
    idempotency_key varchar(256) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    receipt_event_id varchar(256),
    document_id varchar(256),
    error_code varchar(128),
    detail varchar(1000),
    observed_at timestamptz,
    revision bigint NOT NULL DEFAULT 1 CHECK (revision > 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_bpi_wms_inbound_batch_tenant
        FOREIGN KEY (tenant_id, batch_id)
        REFERENCES bpi.bpi_batch_instances (tenant_id, id),
    CONSTRAINT fk_bpi_wms_inbound_outbox_tenant
        FOREIGN KEY (tenant_id, command_event_id)
        REFERENCES bpi.bpi_outbox_events (tenant_id, id),
    CONSTRAINT uq_bpi_wms_inbound_batch UNIQUE (tenant_id, batch_id),
    CONSTRAINT uq_bpi_wms_inbound_idempotency UNIQUE (tenant_id, idempotency_key),
    CONSTRAINT uq_bpi_wms_inbound_receipt UNIQUE (tenant_id, receipt_event_id)
);

CREATE INDEX idx_bpi_wms_inbound_status
    ON bpi.bpi_wms_inbound_links (tenant_id, status, updated_at DESC);

CREATE INDEX idx_bpi_wms_outbox_dispatch
    ON bpi.bpi_outbox_events (status, next_attempt_at, created_at)
    WHERE aggregate_type = 'BATCH_INSTANCE'
      AND event_type = 'WMS_COMPLETION_INBOUND_COMMAND'
      AND status IN ('PENDING', 'DISPATCHING');

CREATE OR REPLACE FUNCTION bpi.reject_shadow_wms_command()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.aggregate_type = 'BATCH_INSTANCE'
       AND NEW.event_type = 'WMS_COMPLETION_INBOUND_COMMAND'
       AND NOT EXISTS (
            SELECT 1
              FROM bpi.bpi_batch_instances batch
             WHERE batch.tenant_id = NEW.tenant_id
               AND batch.id = NEW.aggregate_id
               AND batch.is_shadow = false
       ) THEN
        RAISE EXCEPTION 'WMS completion-inbound commands require a non-shadow batch';
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_bpi_reject_shadow_wms_command
BEFORE INSERT OR UPDATE OF tenant_id, aggregate_type, aggregate_id, event_type
ON bpi.bpi_outbox_events
FOR EACH ROW
EXECUTE FUNCTION bpi.reject_shadow_wms_command();

INSERT INTO bpi.bpi_feature_flags
    (id, tenant_id, scope_type, scope_key, flag_key, enabled, revision,
     updated_by, active, last_reason)
VALUES
    ('00000000-0000-0000-0000-000000000007', '*', 'GLOBAL', '*',
     'bpi.qcs-link', false, 1, 'flyway', true,
     'Phase 2 QCS quality-gate ingestion is disabled until integration acceptance')
ON CONFLICT (tenant_id, scope_type, scope_key, flag_key) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON
            bpi.bpi_quality_gates,
            bpi.bpi_quality_links,
            bpi.bpi_wms_inbound_links
        TO bpi_service;
    END IF;
END
$$;
