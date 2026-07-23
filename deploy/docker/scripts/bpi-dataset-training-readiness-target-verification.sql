\set ON_ERROR_STOP on

SELECT set_config('bpi.acceptance_marker', :'marker', false);

WITH target_definition AS (
    SELECT *
      FROM bpi.bpi_dataset_definitions
     WHERE tenant_id = '1000' AND dataset_code = :'marker'
), target_snapshot AS (
    SELECT snapshot.*
      FROM bpi.bpi_dataset_snapshots snapshot
      JOIN target_definition definition
        ON definition.tenant_id = snapshot.tenant_id
       AND definition.id = snapshot.dataset_id
     ORDER BY snapshot.snapshot_version DESC
     LIMIT 1
), target_registration AS (
    SELECT registration.*
      FROM bpi.bpi_dataset_mlflow_registrations registration
     WHERE registration.tenant_id = '1000'
       AND registration.source_snapshot_id = (SELECT id FROM target_snapshot)
), target_assessments AS (
    SELECT assessment.*
      FROM bpi.bpi_dataset_training_readiness_assessments assessment
     WHERE assessment.tenant_id = '1000'
       AND assessment.mlflow_registration_id = (SELECT id FROM target_registration)
), target_audits AS (
    SELECT audit.*
      FROM bpi.bpi_audit_events audit
     WHERE audit.tenant_id = '1000'
       AND audit.object_id IN (SELECT id FROM target_assessments)
       AND audit.action = 'DATASET_TRAINING_READINESS_ASSESSED'
), target_idempotency AS (
    SELECT idempotency.*
      FROM bpi.bpi_api_idempotency idempotency
     WHERE idempotency.tenant_id = '1000'
       AND idempotency.resource_path = '/bpi/v1/dataset-mlflow-registrations/' ||
           (SELECT id::text FROM target_registration) || '/training-readiness-assessments'
)
SELECT jsonb_pretty(jsonb_build_object(
    'marker', :'marker',
    'flywayVersion', (SELECT max(version::integer)
                        FROM bpi.flyway_schema_history WHERE success),
    'registration', (SELECT jsonb_build_object(
        'id', id,
        'state', state,
        'revision', revision,
        'runId', mlflow_run_id,
        'datasetDigest', dataset_digest,
        'sourceFactsVerified', registration_metadata -> 'sourceFactsVerified',
        'datasetInputVerified', registration_metadata -> 'datasetInputVerified',
        'lineageVerified', registration_metadata -> 'lineageVerified',
        'modelTrained', registration_metadata -> 'modelTrained',
        'modelRegistered', registration_metadata -> 'modelRegistered',
        'onlineInferenceEnabled', registration_metadata -> 'onlineInferenceEnabled',
        'productionActivationAllowed', registration_metadata -> 'productionActivationAllowed'
    ) FROM target_registration),
    'assessments', (SELECT jsonb_build_object(
        'rows', count(*),
        'states', jsonb_agg(state ORDER BY assessment_sequence),
        'sequences', jsonb_agg(assessment_sequence ORDER BY assessment_sequence),
        'revisions', jsonb_agg(revision ORDER BY assessment_sequence),
        'gateCounts', jsonb_agg(jsonb_array_length(gate_results)
            ORDER BY assessment_sequence),
        'blockerCounts', jsonb_agg(jsonb_array_length(blocker_codes)
            ORDER BY assessment_sequence),
        'checksums', jsonb_agg(assessment_checksum ORDER BY assessment_sequence),
        'includedSamples', jsonb_agg((observed_metrics ->> 'includedSampleCount')::integer
            ORDER BY assessment_sequence),
        'signalWindowCounts', jsonb_agg(
            jsonb_array_length(observed_metrics -> 'signalWindowFeatureRefs')
            ORDER BY assessment_sequence),
        'assessmentOnly', bool_and((phase_boundary ->> 'assessmentOnly')::boolean),
        'trainingStarted', bool_or((phase_boundary ->> 'trainingStarted')::boolean),
        'modelCreated', bool_or((phase_boundary ->> 'modelCreated')::boolean),
        'modelRegistered', bool_or((phase_boundary ->> 'modelRegistered')::boolean),
        'onlineInferenceEnabled',
            bool_or((phase_boundary ->> 'onlineInferenceEnabled')::boolean),
        'productionActivationAllowed',
            bool_or((phase_boundary ->> 'productionActivationAllowed')::boolean)
    ) FROM target_assessments),
    'audits', (SELECT jsonb_build_object(
        'rows', count(*),
        'sequences', jsonb_agg(after_revision ORDER BY created_at, id)
    ) FROM target_audits),
    'idempotency', (SELECT jsonb_build_object(
        'rows', count(*),
        'completed', count(*) FILTER (WHERE state = 'COMPLETED'),
        'statuses', jsonb_agg(response_status ORDER BY created_at)
    ) FROM target_idempotency)
));

DO $$
DECLARE
    assessment_rows integer;
    audit_rows integer;
    idempotency_rows integer;
    invalid_rows integer;
BEGIN
    SELECT count(*) INTO assessment_rows
      FROM bpi.bpi_dataset_training_readiness_assessments assessment
      JOIN bpi.bpi_dataset_mlflow_registrations registration
        ON registration.tenant_id = assessment.tenant_id
       AND registration.id = assessment.mlflow_registration_id
      JOIN bpi.bpi_dataset_snapshots snapshot
        ON snapshot.tenant_id = registration.tenant_id
       AND snapshot.id = registration.source_snapshot_id
      JOIN bpi.bpi_dataset_definitions definition
        ON definition.tenant_id = snapshot.tenant_id
       AND definition.id = snapshot.dataset_id
     WHERE assessment.tenant_id = '1000'
       AND definition.dataset_code = current_setting('bpi.acceptance_marker');
    IF assessment_rows <> 2 THEN
        RAISE EXCEPTION 'expected 2 training readiness assessments, found %', assessment_rows;
    END IF;

    SELECT count(*) INTO invalid_rows
      FROM bpi.bpi_dataset_training_readiness_assessments assessment
      JOIN bpi.bpi_dataset_mlflow_registrations registration
        ON registration.tenant_id = assessment.tenant_id
       AND registration.id = assessment.mlflow_registration_id
      JOIN bpi.bpi_dataset_snapshots snapshot
        ON snapshot.tenant_id = registration.tenant_id
       AND snapshot.id = registration.source_snapshot_id
      JOIN bpi.bpi_dataset_definitions definition
        ON definition.tenant_id = snapshot.tenant_id
       AND definition.id = snapshot.dataset_id
     WHERE assessment.tenant_id = '1000'
       AND definition.dataset_code = current_setting('bpi.acceptance_marker')
       AND (assessment.state <> 'BLOCKED'
         OR assessment.revision <> 1
         OR assessment.assessment_sequence NOT IN (1, 2)
         OR jsonb_array_length(assessment.gate_results) <> 19
         OR jsonb_array_length(assessment.blocker_codes) <> 8
         OR (assessment.observed_metrics ->> 'includedSampleCount')::integer <> 1
         OR jsonb_array_length(
             assessment.observed_metrics -> 'signalWindowFeatureRefs') <> 0
         OR (assessment.phase_boundary ->> 'assessmentOnly')::boolean IS NOT TRUE
         OR (assessment.phase_boundary ->> 'trainingStarted')::boolean IS NOT FALSE
         OR (assessment.phase_boundary ->> 'modelCreated')::boolean IS NOT FALSE
         OR (assessment.phase_boundary ->> 'modelRegistered')::boolean IS NOT FALSE
         OR (assessment.phase_boundary ->> 'onlineInferenceEnabled')::boolean IS NOT FALSE
         OR (assessment.phase_boundary ->> 'productionActivationAllowed')::boolean IS NOT FALSE
         OR NOT (assessment.blocker_codes ? 'PROCESS_SIGNAL_WINDOWS_MISSING')
         OR NOT (assessment.blocker_codes ? 'INCLUDED_SAMPLE_COUNT_BELOW_MINIMUM')
         OR NOT (assessment.blocker_codes ? 'DISTINCT_BATCH_COUNT_BELOW_MINIMUM')
         OR NOT (assessment.blocker_codes ? 'PRODUCTION_DAY_COVERAGE_BELOW_MINIMUM')
         OR NOT (assessment.blocker_codes ? 'PRODUCTION_SPLIT_GROUPS_BELOW_MINIMUM')
         OR NOT (assessment.blocker_codes ? 'EXCLUDED_RATIO_ABOVE_MAXIMUM')
         OR NOT (assessment.blocker_codes ? 'START_ACCEPTED_LABEL_COUNT_BELOW_MINIMUM')
         OR NOT (assessment.blocker_codes ? 'START_REJECTED_LABEL_COUNT_BELOW_MINIMUM')
         OR NOT EXISTS (
             SELECT 1
               FROM jsonb_array_elements(assessment.gate_results) gate
              WHERE gate ->> 'code' = 'BOUNDARY_REVIEW_LABEL_MISSING'
                AND (gate ->> 'passed')::boolean IS TRUE));
    IF invalid_rows <> 0 THEN
        RAISE EXCEPTION 'training readiness assessment contract has % invalid rows', invalid_rows;
    END IF;

    SELECT count(*) INTO invalid_rows
      FROM (
          SELECT registration.id,
                 count(DISTINCT assessment.assessment_checksum) AS checksum_count,
                 min(assessment.assessment_sequence) AS minimum_sequence,
                 max(assessment.assessment_sequence) AS maximum_sequence
            FROM bpi.bpi_dataset_training_readiness_assessments assessment
            JOIN bpi.bpi_dataset_mlflow_registrations registration
              ON registration.tenant_id = assessment.tenant_id
             AND registration.id = assessment.mlflow_registration_id
            JOIN bpi.bpi_dataset_snapshots snapshot
              ON snapshot.tenant_id = registration.tenant_id
             AND snapshot.id = registration.source_snapshot_id
            JOIN bpi.bpi_dataset_definitions definition
              ON definition.tenant_id = snapshot.tenant_id
             AND definition.id = snapshot.dataset_id
           WHERE assessment.tenant_id = '1000'
             AND definition.dataset_code = current_setting('bpi.acceptance_marker')
           GROUP BY registration.id
          HAVING count(*) <> 2
              OR count(DISTINCT assessment.assessment_checksum) <> 1
              OR min(assessment.assessment_sequence) <> 1
              OR max(assessment.assessment_sequence) <> 2
      ) invalid_sequences;
    IF invalid_rows <> 0 THEN
        RAISE EXCEPTION 'training readiness sequence/checksum contract failed';
    END IF;

    SELECT count(*) INTO audit_rows
      FROM bpi.bpi_audit_events audit
     WHERE audit.tenant_id = '1000'
       AND audit.action = 'DATASET_TRAINING_READINESS_ASSESSED'
       AND audit.object_id IN (
           SELECT assessment.id
             FROM bpi.bpi_dataset_training_readiness_assessments assessment
             JOIN bpi.bpi_dataset_mlflow_registrations registration
               ON registration.tenant_id = assessment.tenant_id
              AND registration.id = assessment.mlflow_registration_id
             JOIN bpi.bpi_dataset_snapshots snapshot
               ON snapshot.tenant_id = registration.tenant_id
              AND snapshot.id = registration.source_snapshot_id
             JOIN bpi.bpi_dataset_definitions definition
               ON definition.tenant_id = snapshot.tenant_id
              AND definition.id = snapshot.dataset_id
            WHERE definition.dataset_code = current_setting('bpi.acceptance_marker'));
    IF audit_rows <> 2 THEN
        RAISE EXCEPTION 'expected 2 readiness audit rows, found %', audit_rows;
    END IF;

    SELECT count(*) INTO idempotency_rows
      FROM bpi.bpi_api_idempotency idempotency
      JOIN bpi.bpi_dataset_mlflow_registrations registration
        ON idempotency.resource_path =
           '/bpi/v1/dataset-mlflow-registrations/' || registration.id::text ||
           '/training-readiness-assessments'
      JOIN bpi.bpi_dataset_snapshots snapshot
        ON snapshot.tenant_id = registration.tenant_id
       AND snapshot.id = registration.source_snapshot_id
      JOIN bpi.bpi_dataset_definitions definition
        ON definition.tenant_id = snapshot.tenant_id
       AND definition.id = snapshot.dataset_id
     WHERE idempotency.tenant_id = '1000'
       AND definition.dataset_code = current_setting('bpi.acceptance_marker')
       AND idempotency.state = 'COMPLETED'
       AND idempotency.response_status = 200;
    IF idempotency_rows <> 2 THEN
        RAISE EXCEPTION 'expected 2 completed readiness idempotency rows, found %',
            idempotency_rows;
    END IF;
    RAISE NOTICE 'training_readiness_contract=pass';
END $$;

DO $$
DECLARE
    target_id uuid;
    immutable_blocked boolean := false;
BEGIN
    SELECT assessment.id INTO STRICT target_id
      FROM bpi.bpi_dataset_training_readiness_assessments assessment
      JOIN bpi.bpi_dataset_mlflow_registrations registration
        ON registration.tenant_id = assessment.tenant_id
       AND registration.id = assessment.mlflow_registration_id
      JOIN bpi.bpi_dataset_snapshots snapshot
        ON snapshot.tenant_id = registration.tenant_id
       AND snapshot.id = registration.source_snapshot_id
      JOIN bpi.bpi_dataset_definitions definition
        ON definition.tenant_id = snapshot.tenant_id
       AND definition.id = snapshot.dataset_id
     WHERE assessment.tenant_id = '1000'
       AND definition.dataset_code = current_setting('bpi.acceptance_marker')
     ORDER BY assessment.assessment_sequence
     LIMIT 1;
    BEGIN
        UPDATE bpi.bpi_dataset_training_readiness_assessments
           SET assessment_reason = 'forbidden target mutation'
         WHERE tenant_id = '1000' AND id = target_id;
    EXCEPTION WHEN OTHERS THEN
        immutable_blocked := position('immutable' in lower(SQLERRM)) > 0;
    END;
    IF NOT immutable_blocked THEN
        RAISE EXCEPTION 'training readiness immutable update was not blocked';
    END IF;
    RAISE NOTICE 'training_readiness_immutable_update=blocked';
END $$;
