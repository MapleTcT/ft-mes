ALTER TABLE bpi.bpi_batch_instances
    DROP CONSTRAINT IF EXISTS bpi_batch_instances_state_check;

ALTER TABLE bpi.bpi_batch_instances
    ADD CONSTRAINT bpi_batch_instances_state_check
        CHECK (state IN (
            'ACTIVE', 'SUSPENDED', 'CLOSED_RAW', 'RECONCILING', 'AMENDING',
            'REVIEW_REQUIRED', 'WAIT_QA', 'REJECTED', 'DISPOSED', 'REWORK',
            'RELEASED', 'INBOUNDED', 'INBOUND_REVERSING', 'INBOUND_REVERSED'
        ));

ALTER TABLE bpi.bpi_wms_inbound_links
    ADD CONSTRAINT uq_bpi_wms_inbound_tenant_id UNIQUE (tenant_id, id);

CREATE TABLE bpi.bpi_wms_inbound_reversal_tasks (
    id uuid PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    batch_id uuid NOT NULL,
    original_inbound_link_id uuid NOT NULL,
    original_command_event_id uuid NOT NULL,
    original_idempotency_key varchar(256) NOT NULL,
    original_document_id varchar(256) NOT NULL,
    state varchar(24) NOT NULL
        CHECK (state IN ('PENDING_APPROVAL', 'PENDING_WMS', 'COMPLETED', 'FAILED')),
    revision bigint NOT NULL DEFAULT 1 CHECK (revision > 0),
    requested_by varchar(128) NOT NULL,
    requested_at timestamptz NOT NULL DEFAULT now(),
    request_reason varchar(500) NOT NULL,
    request_comment varchar(2000),
    decided_by varchar(128),
    decided_at timestamptz,
    decision_reason varchar(500),
    decision_comment varchar(2000),
    reversal_command_event_id uuid,
    reversal_idempotency_key varchar(256),
    reversal_receipt_event_id varchar(256),
    reversal_document_id varchar(256),
    error_code varchar(128),
    detail varchar(1000),
    observed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_bpi_wms_reversal_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_bpi_wms_reversal_batch_tenant
        FOREIGN KEY (tenant_id, batch_id)
        REFERENCES bpi.bpi_batch_instances (tenant_id, id),
    CONSTRAINT fk_bpi_wms_reversal_original_link_tenant
        FOREIGN KEY (tenant_id, original_inbound_link_id)
        REFERENCES bpi.bpi_wms_inbound_links (tenant_id, id),
    CONSTRAINT fk_bpi_wms_reversal_command_tenant
        FOREIGN KEY (tenant_id, reversal_command_event_id)
        REFERENCES bpi.bpi_outbox_events (tenant_id, id),
    CONSTRAINT uq_bpi_wms_reversal_command UNIQUE (tenant_id, reversal_command_event_id),
    CONSTRAINT uq_bpi_wms_reversal_idempotency UNIQUE (tenant_id, reversal_idempotency_key),
    CONSTRAINT uq_bpi_wms_reversal_receipt UNIQUE (tenant_id, reversal_receipt_event_id),
    CONSTRAINT chk_bpi_wms_reversal_state_facts CHECK (
        (state = 'PENDING_APPROVAL'
            AND decided_by IS NULL AND decided_at IS NULL
            AND reversal_command_event_id IS NULL AND reversal_idempotency_key IS NULL
            AND reversal_receipt_event_id IS NULL AND reversal_document_id IS NULL
            AND error_code IS NULL AND observed_at IS NULL)
        OR
        (state = 'PENDING_WMS'
            AND decided_by IS NOT NULL AND decided_at IS NOT NULL
            AND reversal_command_event_id IS NOT NULL AND reversal_idempotency_key IS NOT NULL
            AND reversal_receipt_event_id IS NULL AND reversal_document_id IS NULL
            AND error_code IS NULL AND observed_at IS NULL)
        OR
        (state = 'COMPLETED'
            AND decided_by IS NOT NULL AND decided_at IS NOT NULL
            AND reversal_command_event_id IS NOT NULL AND reversal_idempotency_key IS NOT NULL
            AND reversal_receipt_event_id IS NOT NULL AND reversal_document_id IS NOT NULL
            AND error_code IS NULL AND observed_at IS NOT NULL)
        OR
        (state = 'FAILED'
            AND decided_by IS NOT NULL AND decided_at IS NOT NULL
            AND reversal_command_event_id IS NOT NULL AND reversal_idempotency_key IS NOT NULL
            AND reversal_receipt_event_id IS NOT NULL AND reversal_document_id IS NULL
            AND error_code IS NOT NULL AND observed_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_bpi_wms_reversal_active_batch
    ON bpi.bpi_wms_inbound_reversal_tasks (tenant_id, batch_id)
    WHERE state IN ('PENDING_APPROVAL', 'PENDING_WMS');

CREATE INDEX idx_bpi_wms_reversal_history
    ON bpi.bpi_wms_inbound_reversal_tasks
        (tenant_id, batch_id, requested_at DESC, id);

-- V15 introduced lifecycle_sequence for rule activation/retirement, but its
-- unique index was unintentionally global. Reversal retries need more than one
-- command event for the same batch, while rule lifecycle uniqueness remains
-- scoped to rule publication events.
DROP INDEX IF EXISTS bpi.uq_bpi_outbox_rule_lifecycle;

CREATE UNIQUE INDEX uq_bpi_outbox_rule_lifecycle
    ON bpi.bpi_outbox_events
        (tenant_id, aggregate_type, aggregate_id, event_type, lifecycle_sequence)
    WHERE aggregate_type = 'RULE_VERSION'
      AND event_type = 'BOUNDARY_RULE_PUBLISHED';

CREATE INDEX idx_bpi_wms_reversal_outbox_dispatch
    ON bpi.bpi_outbox_events (status, next_attempt_at, created_at)
    WHERE aggregate_type = 'BATCH_INSTANCE'
      AND event_type = 'WMS_COMPLETION_INBOUND_REVERSAL_COMMAND'
      AND status IN ('PENDING', 'DISPATCHING');

CREATE OR REPLACE FUNCTION bpi.reject_shadow_wms_command()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.aggregate_type = 'BATCH_INSTANCE'
       AND NEW.event_type IN (
            'WMS_COMPLETION_INBOUND_COMMAND',
            'WMS_COMPLETION_INBOUND_REVERSAL_COMMAND'
       )
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

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON
            bpi.bpi_wms_inbound_reversal_tasks
        TO bpi_service;
    END IF;
END
$$;
