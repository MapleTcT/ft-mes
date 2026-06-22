# Production Cutover Gate

This file is generated from `metadata/production-cutover-gate.json`.
Regenerate it with `python3 scripts/verify-production-cutover-gate.py --write-doc`.

It is a no-cutover control surface, not a production approval.

## Summary

| Field | Value |
| --- | --- |
| Generated At | `2026-06-22T11:28:19+00:00` |
| Repo Commit | `d06ae37a4bd875291d4009f640f905be17c53b30` |
| Database | `PostgreSQL` |
| Status | `NOT_READY_FOR_PRODUCTION_CUTOVER` |
| Gates | `9` |
| Ready / Planned / Blocked / Not Started | `0 / 8 / 1 / 0` |
| Production Blockers | `6` |
| Production Backlog Items | `9` |

## Release Rule

Do not cut over production while this report status is NOT_READY_FOR_PRODUCTION_CUTOVER. A READY_FOR_PRODUCTION_CUTOVER status requires every gate to be READY, productionBlockers=0, productionBacklogItems=0, strict business smoke signoff, and real evidence paths instead of example templates.

## Gate Matrix

| Gate | Status | Ready Evidence Commands | Blocking Reason |
| --- | --- | --- | --- |
| postgres-data-migration | `PLANNED` | `ADP_PROD_MIGRATION_ENV=/secure/path/adp-prod-migration.env make production-source-inventory`<br>`ADP_PROD_MIGRATION_ENV=/secure/path/adp-prod-migration.env make production-target-preflight`<br>`make production-rowcount-compare PROD_MIGRATION_REPORT_DIR=/secure/path/adp-production-migration-preflight`<br>`make production-checksum-compare PROD_MIGRATION_REPORT_DIR=/secure/path/adp-production-migration-preflight`<br>`make production-db-migration-ready-check DB_MIGRATION_EVIDENCE=/secure/path/adp-db-migration-evidence.json` | Production source inventory, production checksum outputs, strict database migration evidence, migration rehearsal and rollback evidence are missing. |
| rollback-plan | `PLANNED` | `make production-rollback-ready-check ROLLBACK_EVIDENCE=/secure/path/adp-rollback-evidence.json` | Only the rollback evidence template exists; real restore and cutback rehearsal evidence is missing. |
| license-strategy | `PLANNED` | `make production-license-ready-check LICENSE_DECISION=/secure/path/adp-license-decision.json` | Production license decision and legal/operational signoff are missing. |
| minio-file-migration | `PLANNED` | `ADP_MINIO_MIGRATION_ENV=/secure/path/adp-minio-migration.env make production-minio-source-inventory`<br>`ADP_MINIO_MIGRATION_ENV=/secure/path/adp-minio-migration.env make production-minio-target-inventory`<br>`make production-minio-compare MINIO_MIGRATION_REPORT_DIR=/secure/path/adp-minio-migration-preflight`<br>`make production-minio-migration-ready-check MINIO_MIGRATION_EVIDENCE=/secure/path/adp-minio-migration-evidence.json` | Production bucket inventory, object compare, strict MinIO migration evidence and migration dry-run are missing. |
| keycloak-production-db | `PLANNED` | `ADP_KEYCLOAK_MIGRATION_ENV=/secure/path/adp-keycloak-migration.env make production-keycloak-source-export`<br>`ADP_KEYCLOAK_MIGRATION_ENV=/secure/path/adp-keycloak-migration.env make production-keycloak-target-export`<br>`make production-keycloak-compare KEYCLOAK_MIGRATION_REPORT_DIR=/secure/path/adp-keycloak-migration-preflight`<br>`make production-keycloak-migration-ready-check KEYCLOAK_MIGRATION_EVIDENCE=/secure/path/adp-keycloak-migration-evidence.json` | Production realm export/import, database restore rehearsal, strict Keycloak migration evidence and secret rotation evidence are missing. |
| nacos-runtime-patch | `PLANNED` | `make runtime-patch-manifest-check`<br>`make smoke-nacos-config ADP_SSH_HOST=/production-or-rehearsal-host`<br>`make production-nacos-runtime-patch-ready-check NACOS_RUNTIME_PATCH_EVIDENCE=/secure/path/adp-nacos-runtime-patch-evidence.json` | Production Nacos export/diff, strict Nacos/runtime patch evidence, signed patch package and rollback rehearsal are missing. |
| ports-domain-tls | `PLANNED` | `make production-network-tls-ready-check NETWORK_TLS_PLAN=/secure/path/adp-network-tls-plan.json` | Production domain, TLS certificate, proxy and firewall evidence are missing. |
| security-hardening | `PLANNED` | `make production-security-hardening-ready-check SECURITY_HARDENING_PLAN=/secure/path/adp-security-hardening-plan.json` | Production hardening evidence is missing. |
| business-smoke-signoff | `BLOCKED` | `make business-package-scan`<br>`make smoke-business-dependencies ADP_BASE_URL=/production-or-rehearsal-url`<br>`make smoke-production-export-readiness ADP_BASE_URL=/production-or-rehearsal-url`<br>`make production-business-smoke-signoff-ready-check BUSINESS_SMOKE_SIGNOFF=/secure/path/adp-business-smoke-signoff.json` | Production module blocker ledger still contains 6 BLOCKED cases and production backlog still contains 9 unresolved items. |

## Source Evidence

| Ledger | Path | Status | Metrics |
| --- | --- | --- | --- |
| `migrationReadiness` | `metadata/production-migration-readiness.json` | `NOT_READY_FOR_PRODUCTION_MIGRATION` | blocked=1, notStarted=0, planned=8, ready=0, totalTracks=9 |
| `testEnvironmentSmoke` | `metadata/test-environment-smoke.json` | `PASS` | baseUrl=http://100.99.133.43:18080, expectedContainerCount=6, fail=0, pass=9, runningExpectedContainers=6, sshHost=100.99.133.43, totalChecks=9 |
| `platformValidationSmoke` | `metadata/platform-validation-smoke.json` | `PASS` | baseUrl=http://100.99.133.43:18080, browserBaseUrl=http://100.99.133.43:18080, failed=0, ok=True, passed=6, total=6 |
| `postgresRuntimeSmoke` | `metadata/postgres-runtime-smoke.json` | `PASS` | databaseSizeBytes=590593383, expectedColumns=15, expectedIndexes=8, expectedTables=32, fail=0, missingExpectedTables=0, pass=8, presentExpectedColumns=15, presentExpectedIndexes=8, presentExpectedTables=32, publicTableCount=1474, publicViewCount=150, status=PASS, totalChecks=8 |
| `nacosConfigSmoke` | `metadata/nacos-config-drift-smoke.json` | `PASS` | criticalChecks=20, criticalFail=0, criticalPass=20, dataIds=44, drifted=27, exactMatches=17, expectedServices=18, failedServices=0, healthyServices=18, missingLocal=0, missingRemote=0, nacosServiceCount=91, oracleResidueFiles=0, remoteFetched=44, status=PASS |
| `keycloakJwtSmoke` | `metadata/keycloak-jwt-runtime-smoke.json` | `PASS` | checks=19, clientScopes=10, expectedClients=2, fail=0, gatewayMenuTargets=374, keycloakPublicKeySha256Prefix=9e9cea84527f841b, nacosHealthyKeycloakHosts=2, nacosJwtSha256Prefix=9e9cea84527f841b, pass=19, status=PASS, suposMappers=17 |
| `minioRuntimeSmoke` | `metadata/minio-runtime-smoke.json` | `PASS` | bucketCount=2, bucketsWithObjects=2, fail=0, inspectedBucketCount=2, pass=8, status=PASS, totalChecks=8, totalObjects=31, totalSizeBytes=104285 |
| `productionBlockers` | `metadata/production-module-blockers.json` | `BLOCKED` | blockedCases=6, blockers=6 |
| `productionBacklog` | `metadata/production-module-backlog.json` | `BLOCKED` | blocked=9, failBacklog=0, totalItems=9 |
| `businessDependencyReadiness` | `metadata/business-dependency-readiness-smoke.json` | `BLOCKED` | baseUrl=http://100.99.133.43:18080, blocked=2, dependencies=2, ready=0, sshHost=100.99.133.43 |
| `businessPackageScan` | `metadata/business-dependency-package-scan.json` | `BLOCKED_NO_IMPLEMENTATION_CANDIDATE` | archiveEntriesScanned=429984, archivesScanned=1142, blockedDependencies=2, candidateDependencies=0, filesVisited=58889, nestedArchivesScanned=2139 |
| `productionExportReadiness` | `metadata/production-export-readiness-smoke.json` | `BLOCKED` | actionRequired=0, blocked=5, pagePass=6, ready=1, runtimeExportActions=1, targets=6, verifiedDataExports=1, visibleExportActions=1 |

## Verification

```bash
make production-cutover-gate-check
make production-rehearsal-plan-check
make production-evidence-ready-gate-regression-check
```

Do not change this document by hand. Update `metadata/production-cutover-gate.json` or its source ledgers, regenerate the document, then run the verification commands.
