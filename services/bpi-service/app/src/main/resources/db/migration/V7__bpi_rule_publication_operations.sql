ALTER TABLE bpi.bpi_outbox_events
    ADD COLUMN revision bigint NOT NULL DEFAULT 1 CHECK (revision >= 1),
    ADD COLUMN total_attempt_count integer NOT NULL DEFAULT 0 CHECK (total_attempt_count >= 0),
    ADD COLUMN manual_retry_count integer NOT NULL DEFAULT 0 CHECK (manual_retry_count >= 0),
    ADD COLUMN last_requeued_at timestamptz,
    ADD COLUMN last_requeued_by varchar(128);

UPDATE bpi.bpi_outbox_events
   SET total_attempt_count = attempt_count;
