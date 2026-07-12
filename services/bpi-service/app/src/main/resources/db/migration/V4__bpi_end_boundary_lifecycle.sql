ALTER TABLE bpi.bpi_batch_instances
    DROP CONSTRAINT IF EXISTS bpi_batch_instances_state_check;

UPDATE bpi.bpi_batch_instances
   SET state = 'CLOSED_RAW', updated_at = now()
 WHERE state = 'CLOSED';

ALTER TABLE bpi.bpi_batch_instances
    ADD CONSTRAINT bpi_batch_instances_state_check
        CHECK (state IN (
            'ACTIVE', 'SUSPENDED', 'CLOSED_RAW', 'RECONCILING', 'AMENDING',
            'REVIEW_REQUIRED', 'WAIT_QA', 'REJECTED', 'DISPOSED', 'REWORK',
            'RELEASED', 'INBOUNDED'
        ));

CREATE UNIQUE INDEX uq_bpi_open_batch_per_line
    ON bpi.bpi_batch_instances (tenant_id, line_id)
    WHERE state IN ('ACTIVE', 'SUSPENDED');
