ALTER TABLE bpi.bpi_topology_versions
    ADD CONSTRAINT uq_bpi_topology_tenant_id UNIQUE (tenant_id, id);
ALTER TABLE bpi.bpi_rule_versions
    ADD CONSTRAINT uq_bpi_rule_tenant_id UNIQUE (tenant_id, id);
ALTER TABLE bpi.bpi_batch_instances
    ADD CONSTRAINT uq_bpi_batch_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE bpi.bpi_rule_versions
    DROP CONSTRAINT IF EXISTS bpi_rule_versions_topology_version_id_fkey,
    ADD CONSTRAINT fk_bpi_rule_topology_tenant
        FOREIGN KEY (tenant_id, topology_version_id)
        REFERENCES bpi.bpi_topology_versions (tenant_id, id);

ALTER TABLE bpi.bpi_batch_candidates
    DROP CONSTRAINT IF EXISTS bpi_batch_candidates_topology_version_id_fkey,
    DROP CONSTRAINT IF EXISTS bpi_batch_candidates_rule_version_id_fkey,
    DROP CONSTRAINT IF EXISTS fk_bpi_candidate_batch,
    ADD CONSTRAINT fk_bpi_candidate_topology_tenant
        FOREIGN KEY (tenant_id, topology_version_id)
        REFERENCES bpi.bpi_topology_versions (tenant_id, id),
    ADD CONSTRAINT fk_bpi_candidate_rule_tenant
        FOREIGN KEY (tenant_id, rule_version_id)
        REFERENCES bpi.bpi_rule_versions (tenant_id, id),
    ADD CONSTRAINT fk_bpi_candidate_batch_tenant
        FOREIGN KEY (tenant_id, batch_id)
        REFERENCES bpi.bpi_batch_instances (tenant_id, id);

ALTER TABLE bpi.bpi_batch_instances
    DROP CONSTRAINT IF EXISTS bpi_batch_instances_topology_version_id_fkey,
    DROP CONSTRAINT IF EXISTS bpi_batch_instances_rule_version_id_fkey,
    ADD CONSTRAINT fk_bpi_batch_topology_tenant
        FOREIGN KEY (tenant_id, topology_version_id)
        REFERENCES bpi.bpi_topology_versions (tenant_id, id),
    ADD CONSTRAINT fk_bpi_batch_rule_tenant
        FOREIGN KEY (tenant_id, rule_version_id)
        REFERENCES bpi.bpi_rule_versions (tenant_id, id);

ALTER TABLE bpi.bpi_batch_state_events
    DROP CONSTRAINT IF EXISTS bpi_batch_state_events_batch_id_fkey,
    DROP CONSTRAINT IF EXISTS bpi_batch_state_events_tenant_id_trace_id_action_key,
    ADD CONSTRAINT fk_bpi_state_batch_tenant
        FOREIGN KEY (tenant_id, batch_id)
        REFERENCES bpi.bpi_batch_instances (tenant_id, id);

CREATE INDEX idx_bpi_state_trace
    ON bpi.bpi_batch_state_events (tenant_id, trace_id, event_time DESC);

ALTER TABLE bpi.bpi_boundary_evidence
    DROP CONSTRAINT IF EXISTS bpi_boundary_evidence_batch_id_fkey,
    ADD CONSTRAINT fk_bpi_evidence_batch_tenant
        FOREIGN KEY (tenant_id, batch_id)
        REFERENCES bpi.bpi_batch_instances (tenant_id, id);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bpi_service') THEN
        EXECUTE 'GRANT USAGE ON SCHEMA bpi TO bpi_service';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA bpi TO bpi_service';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA bpi GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO bpi_service';
    END IF;
END
$$;
