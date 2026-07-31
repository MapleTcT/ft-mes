SHELL := /bin/sh

MVN ?= mvn
PYTHON ?= python3
NODE ?= node
JAVAC ?= javac
JAVA ?= java

DEPLOY_DIR ?= deploy/docker
COMPOSE_FILE ?= $(DEPLOY_DIR)/docker-compose.yml
ENV_FILE ?= $(DEPLOY_DIR)/.env
ENV_EXAMPLE ?= $(DEPLOY_DIR)/.env.example
COMPOSE_ENV_FILE := $(if $(wildcard $(ENV_FILE)),$(ENV_FILE),$(ENV_EXAMPLE))
COMPOSE ?= docker compose --env-file $(COMPOSE_ENV_FILE) -f $(COMPOSE_FILE)

ADP_BASE_URL ?= http://100.99.133.43:18080
ADP_BROWSER_BASE_URL ?= $(ADP_BASE_URL)
ADP_USERNAME ?= admin
ADP_PASSWORD ?= 123456
SERVICE ?=
MODULE ?=
PACKAGE ?=

POSTGRES_AUDIT_REPORT ?= /tmp/adp-postgres-mapping-audit.json
INTAKE ?=
INTAKE_REPORT ?= /tmp/adp-module-intake-precheck.json
PLATFORM_SMOKE_OUTPUT ?= /tmp/adp-platform-validation-smoke
PLATFORM_MENU_LIMIT ?= 40
ADP_PLATFORM_SECTION_TIMEOUT_MS ?= 300000
ADP_ORG_VISIBLE_TIMEOUT_MS ?= 240000
ADP_SSH_HOST ?= 100.99.133.43
ADP_SSH_USER ?= v6
ADP_SSH_CONNECT_TIMEOUT ?= 8
ADP_PAGE_TIMEOUT_MS ?= 45000
ADP_API_TIMEOUT_MS ?= 20000
TEST_ENVIRONMENT_SMOKE_OUTPUT ?= /tmp/adp-test-environment-smoke.json
TEST_ENVIRONMENT_STATIC_BUNDLE_LINK_REPORT ?= metadata/test-environment-static-bundle-link-smoke.json
POSTGRES_RUNTIME_SMOKE_OUTPUT ?= /tmp/adp-postgres-runtime-smoke.json
NACOS_CONFIG_SMOKE_OUTPUT ?= /tmp/adp-nacos-config-drift-smoke.json
KEYCLOAK_JWT_SMOKE_OUTPUT ?= /tmp/adp-keycloak-jwt-runtime-smoke.json
MINIO_RUNTIME_SMOKE_OUTPUT ?= /tmp/adp-minio-runtime-smoke.json
RUNTIME_SMOKE_EXPECTED_HOST ?= 100.99.133.43
PLATFORM_VALIDATION_REPORT ?= metadata/platform-validation-smoke.json
PLATFORM_VALIDATION_MIN_MENU_PAGES ?= 5
PLATFORM_VALIDATION_EXPECTED_BROWSER_BASE_URL ?= http://100.99.133.43:18080
BUSINESS_DEPENDENCY_SMOKE_OUTPUT ?= /tmp/adp-business-dependency-readiness-smoke.json
BUSINESS_DEPENDENCY_SMOKE_REPORT ?= metadata/business-dependency-readiness-smoke.json
BUSINESS_PACKAGE_SCAN_ROOTS ?= $(CURDIR)/backend/source-modules:/Users/zhangchu/Documents/MES包:/Users/zhangchu/Downloads/ADP/bap-server/base-Server:/Users/zhangchu/Downloads/ADP/bap-server/config:/Users/zhangchu/Downloads/ADP/Temp/static/custom
BUSINESS_PACKAGE_SCAN_OUTPUT ?= metadata/business-dependency-package-scan.json
BUSINESS_PACKAGE_SCAN_REPORT ?= metadata/business-dependency-package-scan.json
BUSINESS_PACKAGE_SCAN_NESTED_DEPTH ?= 1
ORGANIZATION_PERSISTENCE_OUTPUT ?= /tmp/adp-organization-persistence-acceptance.json
ORGANIZATION_GROUP_PERSISTENCE_OUTPUT ?= /tmp/adp-organization-group-persistence-acceptance.json
ORGANIZATION_POSITION_PERSISTENCE_OUTPUT ?= /tmp/adp-organization-position-persistence-acceptance.json
ORGANIZATION_POSITION_ROLE_PERSISTENCE_OUTPUT ?= /tmp/adp-organization-position-role-persistence-acceptance.json
ORGANIZATION_COMPANY_PERSISTENCE_OUTPUT ?= /tmp/adp-organization-company-persistence-acceptance.json
ORGANIZATION_PERSON_PERSISTENCE_OUTPUT ?= /tmp/adp-organization-person-persistence-acceptance.json
ORGANIZATION_PERSON_USER_PERSISTENCE_OUTPUT ?= /tmp/adp-organization-person-user-persistence-acceptance.json
AUTH_USER_PERSISTENCE_OUTPUT ?= /tmp/adp-auth-user-persistence-acceptance.json
RBAC_PERMISSION_PERSISTENCE_OUTPUT ?= /tmp/adp-rbac-permission-persistence-acceptance.json
SYSTEMCODE_PERSISTENCE_OUTPUT ?= /tmp/adp-systemcode-persistence-acceptance.json
SYSTEMCONFIG_PERSISTENCE_OUTPUT ?= /tmp/adp-systemconfig-persistence-acceptance.json
PATROL_TASK_PERSISTENCE_OUTPUT ?= /tmp/adp-patrol-task-persistence-acceptance.json
PATROL_EXECUTION_PERSISTENCE_OUTPUT ?= /tmp/adp-patrol-execution-persistence-acceptance.json
PATROL_HIDDEN_DANGER_PERSISTENCE_OUTPUT ?= /tmp/adp-patrol-hidden-danger-persistence-acceptance.json
PATROL_INPUT_STANDARD_PERSISTENCE_OUTPUT ?= /tmp/adp-patrol-input-standard-persistence-acceptance.json
PATROL_ROUTE_PERSISTENCE_OUTPUT ?= /tmp/adp-patrol-route-persistence-acceptance.json
PATROL_AREA_PERSISTENCE_OUTPUT ?= /tmp/adp-patrol-area-persistence-acceptance.json
PATROL_ITEM_PERSISTENCE_OUTPUT ?= /tmp/adp-patrol-item-persistence-acceptance.json
PATROL_REPORT_ACCEPTANCE_OUTPUT ?= metadata/patrol-report-acceptance.json
PATROL_GATHER_ACCEPTANCE_OUTPUT ?= metadata/patrol-gather-data-runtime-acceptance.json
PATROL_GATHER_EXPECTED_EAM_SHA256 ?=
SYSTEMCONFIG_BUILTINS_OUTPUT ?= metadata/systemconfig-builtins-readiness-smoke.json
SYSTEMCONFIG_CONTROLLED_OUTPUT ?= metadata/systemconfig-controlled-runtime-config-acceptance.json
SYSTEMCONFIG_CONTROLLED_TARGET_MODE ?= qcs
RUNTIME_CONFIG_SMOKE_OUTPUT ?= metadata/runtime-configuration-readiness-smoke.json
CUSTOM_PROPERTY_ACCEPTANCE_OUTPUT ?= metadata/custom-property-persistence-acceptance.json
ENTITY_MODEL_CONFIG_CRUD_READINESS_OUTPUT ?= metadata/entity-model-config-crud-readiness-probe.json
ENTITY_MODEL_FIELD_ACCEPTANCE_OUTPUT ?= metadata/entity-model-field-persistence-acceptance.json
ENTITY_MODEL_FIELD_TYPE_MATRIX_OUTPUT ?= metadata/entity-model-field-type-matrix-acceptance.json
ENTITY_MODEL_OBJECT_ASSOCIATION_OUTPUT ?= metadata/entity-model-object-association-acceptance.json
ENTITY_MODEL_FIELD_DELETE_OUTPUT ?= metadata/entity-model-field-delete-persistence-acceptance.json
WOM_START_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-start-persistence-acceptance.json
ADP_WOM_KEEP_FIXTURE ?= false
WOM_HOLD_RESTART_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-hold-restart-persistence-acceptance.json
WOM_TOOLBAR_ROW_SMOKE_OUTPUT ?= metadata/wom-toolbar-row-smoke.json
WOM_TOOLBAR_ROW_SMOKE_SEED_OUTPUT ?= /tmp/adp-wom-toolbar-row-smoke-seed.json
WOM_TOOLBAR_ROW_SMOKE_SCREENSHOT ?= metadata/wom-toolbar-row-smoke.png
WOM_TOOLBAR_PAGE_TIMEOUT_MS ?= 240000
WOM_PROCESS_ACTIONS_OUTPUT ?= metadata/wom-process-execution-actions-acceptance-20260731.json
WOM_PROCESS_ACTIONS_SCREENSHOT_DIR ?= metadata
WOM_STOP_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-stop-persistence-acceptance.json
WOM_STOP_OUTPUT_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-stop-output-persistence-acceptance.json
WOM_ADVANCE_RELEASE_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-advance-release-persistence-acceptance.json
WOM_PREPARE_NEED_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-prepare-need-persistence-acceptance.json
WOM_ACTIVE_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-active-persistence-acceptance.json
WOM_ACTIVE_END_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-active-end-persistence-acceptance.json
WOM_EASY_ACTIVE_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-easy-active-persistence-acceptance.json
WOM_PUTIN_ACTIVE_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-putin-active-persistence-acceptance.json
WOM_CHECK_ACTIVE_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-check-active-persistence-acceptance.json
WOM_PROCESS_START_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-process-start-persistence-acceptance.json
WOM_PROCESS_END_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-process-end-persistence-acceptance.json
WOM_PROCESS_UNIT_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-process-unit-persistence-acceptance.json
WOM_MANU_INSPECT_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-manu-inspect-persistence-acceptance.json
WOM_CHECKOUTBILL_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-checkoutbill-persistence-acceptance.json
WOM_MANUFACTURING_ORDER_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-manufacturing-order-persistence-acceptance.json
WOM_REJECT_MATERIAL_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-reject-material-persistence-acceptance.json
FACTORY_LINE_PERSISTENCE_OUTPUT ?= /tmp/adp-factory-line-persistence-acceptance.json
CORE_FLOW_RUNTIME_ROLLBACK_OUTPUT ?= metadata/core-flow-runtime-rollback-rehearsal.json
CORE_FLOW_REMOTE_ROOT ?= /home/v6/adp-mes-docker-newbase-20260611-181921
CORE_FLOW_BACKUP_TAG ?= 20260710-coreflow
BPI_RUNTIME_ROLLBACK_OUTPUT ?= metadata/bpi-runtime-image-rollback-acceptance.json
BPI_ROLLBACK_SERVICE_IMAGE ?=
BPI_ROLLBACK_ADAPTER_IMAGE ?=
BPI_ROLLBACK_JOB_JAR ?=
BPI_LOAD_CLIENT_JOB_JAR ?=
BPI_STREAM_REMOTE_ROOT ?= /home/v6/adp-bpi-stream-v15
BPI_INTEGRATED_ROLLBACK_OUTPUT ?= metadata/bpi-integrated-rollback-acceptance.json
BPI_ACCEPTANCE_TENANT_ID ?= 1000
BPI_ACCEPTANCE_PLANT_ID ?= PLANT-01
BPI_ACCEPTANCE_LINE_ID ?=
WOM_PUBLIC_PRODUCE_TASK_CREATED_RETIREMENT_OUTPUT ?= metadata/wom-public-produce-task-created-retirement-acceptance.json
WOM_PUBLIC_PRODUCE_TASK_CREATED_NOOP_OUTPUT ?= $(WOM_PUBLIC_PRODUCE_TASK_CREATED_RETIREMENT_OUTPUT)
WOM_QRCODE_ROUTE_PROBE_OUTPUT ?= metadata/wom-qrcode-route-probe.json
WOM_QRCODE_PERSISTENCE_OUTPUT ?= metadata/wom-qrcode-persistence-acceptance.json
WOM_QRCODE_BROWSER_OUTPUT ?= metadata/wom-qrcode-browser-acceptance.json
WOM_QRCODE_BROWSER_SCREENSHOT ?= /tmp/adp-wom-qrcode-browser-acceptance.png
QCS_REPORT_CHAIN_MODE ?= qualified
QCS_REPORT_CHAIN_PERSISTENCE_OUTPUT ?= /tmp/adp-qcs-report-chain-persistence-acceptance.json
MES_FULL_FLOW_OUTPUT ?= /tmp/adp-mes-full-production-flow-acceptance.json
MES_FULL_FLOW_CONFIRM ?= NO
MES_FULL_FLOW_BPI_BATCH_ID ?=
MES_FULL_FLOW_TASK_ID ?=
FRUCTOSE_PILOT_OUTPUT ?= /tmp/adp-fructose-line-pilot-acceptance.json
FRUCTOSE_FULL_FLOW_OUTPUT ?= /tmp/adp-fructose-line-full-flow-01.json
FRUCTOSE_PILOT_SCREENSHOT_DIR ?= /tmp/adp-fructose-line-pilot-screenshots
FRUCTOSE_PILOT_CONFIRM ?= NO
TEAMINFO_SCHEDULEPLAN_PERSISTENCE_OUTPUT ?= /tmp/adp-teaminfo-scheduleplan-persistence-acceptance.json
CRAFTGRAPH_PERSISTENCE_OUTPUT ?= /tmp/adp-craftgraph-persistence-acceptance.json
BUSINESS_PAGE_SMOKE_OUTPUT ?= /tmp/adp-business-page-smoke
PRODUCTION_DISCOVERY_OUTPUT ?= /tmp/adp-production-action-discovery
PRODUCTION_DISCOVERY_TARGETS ?=
PRODUCTION_DISCOVERY_CLICK_CREATE ?= true
PRODUCTION_EXPORT_SMOKE_OUTPUT ?= /tmp/adp-production-export-readiness-smoke.json
PRODUCTION_EXPORT_SMOKE_REPORT ?= metadata/production-export-readiness-smoke.json
PROD_MIGRATION_ENV ?= deploy/database/production-migration/.env
PROD_MIGRATION_REPORT_DIR ?= /tmp/adp-production-migration-preflight
PROD_MIGRATION_SOURCE_COUNTS ?= $(PROD_MIGRATION_REPORT_DIR)/source-row-counts.tsv
PROD_MIGRATION_TARGET_COUNTS ?= $(PROD_MIGRATION_REPORT_DIR)/target-row-counts.tsv
PROD_MIGRATION_SOURCE_CHECKSUMS ?= $(PROD_MIGRATION_REPORT_DIR)/source-checksums.tsv
PROD_MIGRATION_TARGET_CHECKSUMS ?= $(PROD_MIGRATION_REPORT_DIR)/target-checksums.tsv
DB_MIGRATION_EVIDENCE ?= deploy/database/production-migration/migration-evidence.example.json
MINIO_MIGRATION_ENV ?= deploy/minio/production-migration/.env
MINIO_MIGRATION_REPORT_DIR ?= /tmp/adp-minio-migration-preflight
MINIO_SOURCE_INVENTORY ?= $(MINIO_MIGRATION_REPORT_DIR)/source-object-inventory.tsv
MINIO_TARGET_INVENTORY ?= $(MINIO_MIGRATION_REPORT_DIR)/target-object-inventory.tsv
MINIO_MIGRATION_EVIDENCE ?= deploy/minio/production-migration/minio-migration-evidence.example.json
KEYCLOAK_MIGRATION_ENV ?= deploy/keycloak/production-migration/.env
KEYCLOAK_MIGRATION_REPORT_DIR ?= /tmp/adp-keycloak-migration-preflight
KEYCLOAK_SOURCE_INVENTORY ?= $(KEYCLOAK_MIGRATION_REPORT_DIR)/source-realm-inventory.json
KEYCLOAK_TARGET_INVENTORY ?= $(KEYCLOAK_MIGRATION_REPORT_DIR)/target-realm-inventory.json
KEYCLOAK_MIGRATION_EVIDENCE ?= deploy/keycloak/production-migration/keycloak-migration-evidence.example.json
ROLLBACK_EVIDENCE ?= deploy/rollback/production-migration/rollback-evidence.example.json
LICENSE_DECISION ?= deploy/license/production-migration/license-decision.example.json
NETWORK_TLS_PLAN ?= deploy/network/production-migration/network-tls-plan.example.json
SECURITY_HARDENING_PLAN ?= deploy/security/production-migration/security-hardening-plan.example.json
BUSINESS_SMOKE_SIGNOFF ?= deploy/business-smoke/production-migration/business-smoke-signoff.example.json
NACOS_RUNTIME_PATCH_EVIDENCE ?= deploy/nacos/production-migration/nacos-runtime-patch-evidence.example.json
CI_REQUIRED_FILE_INVENTORY ?= metadata/ci-required-file-inventory.json
GOAL_GAP_REGISTER ?= metadata/goal-gap-register.json
BPI_STREAM_DEPLOY_DIR ?= deploy/bpi-streaming
BPI_STREAM_ENV_FILE ?= $(BPI_STREAM_DEPLOY_DIR)/.env
BPI_STREAM_ENV_EXAMPLE ?= $(BPI_STREAM_DEPLOY_DIR)/.env.example
BPI_STREAM_COMPOSE_ENV := $(if $(wildcard $(BPI_STREAM_ENV_FILE)),$(BPI_STREAM_ENV_FILE),$(BPI_STREAM_ENV_EXAMPLE))
BPI_STREAM_COMPOSE ?= docker compose --env-file $(BPI_STREAM_COMPOSE_ENV) -f $(BPI_STREAM_DEPLOY_DIR)/docker-compose.yml

.PHONY: help ci ci-java17 verify verify-pom compose-config runtime-script-check sustainable-check ci-required-file-inventory ci-required-file-inventory-check ci-required-file-strict-check product-interaction-manual-check project-goal-acceptance-check goal-gap-register goal-gap-register-check backend-table-audit-handoff-check basic-config-coverage-check basic-config-action-matrix-check entity-model-config-crud-readiness-check test-environment-address-check test-environment-static-bundle-link-check persistence-acceptance-check production-testcase-check wom-toolbar-action-coverage-check production-blocker-check production-module-backlog-check production-action-map-check platform-validation-check runtime-smoke-reports-check business-dependency-readiness-check business-dependency-contract-check business-module-intake-requirements-check business-package-scan-check production-export-readiness-check production-export-gap-breakdown production-export-gap-breakdown-check production-source-evidence-refresh production-source-evidence-refresh-check production-migration-readiness-check production-cutover-gate-doc production-cutover-gate-check production-rehearsal-plan production-rehearsal-plan-check production-evidence-ready-gate-regression-check runtime-patch-manifest runtime-patch-manifest-check bpi-contracts-test source-module-check module-intake-precheck-regression-check module-intake-candidate-report-check source-module-test create-backend-module module-intake-check material-wms-test material-wms-package material-wms-stage-runtime acceptance-material-wms-persistence process-analysis-test process-analysis-package process-analysis-stage-runtime acceptance-process-analysis-persistence inventory inventory-check backend-dependency-inventory backend-dependency-check oracle-audit oracle-audit-check postgres-migration-index postgres-migration-check oracle-replacement-status oracle-replacement-check production-source-inventory production-target-preflight production-rowcount-compare production-checksum-compare production-db-migration-evidence-check production-db-migration-ready-check production-minio-source-inventory production-minio-target-inventory production-minio-compare production-minio-migration-evidence-check production-minio-migration-ready-check production-keycloak-source-export production-keycloak-target-export production-keycloak-compare production-keycloak-migration-evidence-check production-keycloak-migration-ready-check production-rollback-evidence-check production-rollback-ready-check production-license-strategy-check production-license-ready-check production-network-tls-check production-network-tls-ready-check production-security-hardening-check production-security-hardening-ready-check production-business-smoke-signoff-check production-business-smoke-signoff-ready-check production-nacos-runtime-patch-check production-nacos-runtime-patch-ready-check render-config prepare-runtime up-infra up down ps logs smoke-platform smoke-api smoke-menu smoke-todo smoke-organization smoke-test-environment smoke-postgres-runtime smoke-nacos-config smoke-keycloak-jwt smoke-minio-runtime smoke-business-dependencies business-package-scan smoke-production-export-readiness acceptance-organization-persistence acceptance-organization-group-persistence acceptance-organization-position-persistence acceptance-organization-position-role-persistence acceptance-organization-company-persistence acceptance-organization-person-persistence acceptance-organization-person-user-persistence acceptance-auth-user-persistence acceptance-rbac-permission-persistence acceptance-systemcode-persistence acceptance-systemconfig-persistence smoke-systemconfig-builtins acceptance-systemconfig-controlled-runtime-config smoke-runtime-configuration smoke-entity-model-config-crud-readiness acceptance-custom-property-persistence acceptance-patrol-input-standard-persistence acceptance-patrol-route-persistence acceptance-patrol-area-persistence acceptance-patrol-item-persistence acceptance-wom-manufacturing-order-persistence acceptance-wom-start-persistence acceptance-wom-hold-restart-persistence smoke-wom-toolbar-row acceptance-wom-stop-persistence acceptance-wom-stop-output-persistence acceptance-wom-advance-release-persistence acceptance-wom-prepare-need-persistence acceptance-wom-active-persistence acceptance-wom-active-end-persistence acceptance-wom-easy-active-persistence acceptance-wom-putin-active-persistence acceptance-wom-check-active-persistence acceptance-wom-process-start-persistence acceptance-wom-process-end-persistence acceptance-wom-process-unit-persistence acceptance-wom-manu-inspect-persistence acceptance-wom-checkoutbill-persistence acceptance-wom-reject-material-persistence probe-wom-public-produce-task-created-noop probe-wom-qrcode-route acceptance-qcs-report-chain-persistence acceptance-mes-full-production-flow acceptance-teaminfo-scheduleplan-persistence acceptance-craftgraph-persistence smoke-rbac-authority smoke-business smoke-business-page discover-production-actions audit-postgres-mappings audit-postgres-report
.PHONY: rehearse-core-flow-runtime-rollback bpi-runtime-image-rollback-rehearsal bpi-integrated-rollback-rehearsal
.PHONY: acceptance-entity-model-field-persistence acceptance-entity-model-field-type-matrix acceptance-entity-model-object-association acceptance-entity-model-field-delete-persistence
.PHONY: acceptance-wom-public-produce-task-created-retirement
.PHONY: acceptance-factory-line-persistence
.PHONY: acceptance-fructose-line-pilot
.PHONY: acceptance-wom-process-execution-actions
.PHONY: wom-print-test wom-print-package wom-print-stage-runtime acceptance-wom-qrcode-persistence acceptance-wom-qrcode-browser
.PHONY: rm-formula-editor-test rm-formula-editor-package rm-formula-editor-stage-runtime acceptance-rm-web-formula-editor-persistence rm-web-formula-editor-acceptance-check
.PHONY: wom-quality-reporting-test wom-quality-reporting-package wom-quality-reporting-stage-runtime acceptance-wom-quality-quantity-persistence
.PHONY: bpi-api-contract-check bpi-simulation-test bpi-service-static-check bpi-service-test bpi-service-package bpi-dataset-materializer-test bpi-dataset-catalog-publisher-test bpi-dataset-retention-archiver-test bpi-dataset-mlflow-registrar-test bpi-wms-adapter-test bpi-wms-adapter-package bpi-wms-outage-fixture-test bpi-runtime-upgrade-expand-only bpi-integrated-upgrade-expand-only bpi-material-reversal-schema-upgrade-target bpi-material-wms-deploy-target acceptance-bpi-quality-release-target acceptance-bpi-wms-reconciliation-target acceptance-bpi-wms-outage-recovery-target acceptance-bpi-force-close-target acceptance-bpi-formal-identity-force-close-target acceptance-bpi-formal-identity-wms-reversal-target acceptance-bpi-formal-identity-wms-roundtrip-target acceptance-qcs-bpi-quality-gate-target rehearse-bpi-wms-outage-recovery-target bpi-stream-static-check bpi-stream-test bpi-stream-package bpi-stream-deployment-check bpi-stream-compose-config bpi-stream-deploy-preflight bpi-stream-cluster-smoke bpi-stream-broker-failure-recovery bpi-stream-flink-rollback-rehearsal bpi-stream-cluster-replay bpi-stream-data-quality-replay bpi-stream-joint-replay bpi-stream-rule-deactivate bpi-stream-rule-lifecycle-evidence bpi-stream-postgres-replay bpi-stream-capture-savepoint bpi-stream-restore-savepoint bpi-stream-verify-savepoint bpi-rule-application-flink-acceptance bpi-production-context-test bpi-production-context-postgres-test qcs-quality-gate-outbox-test qcs-quality-gate-outbox-postgres-test qcs-quality-gate-outbox-package up-bpi-stream down-bpi-stream bpi-runtime-replay-test bpi-adapter-static-check bpi-adapter-test bpi-adapter-package bpi-ui-static-check bpi-ui-build bpi-ui-test bpi-feature-flag-governance-acceptance-check bpi-shell-menu-gate-acceptance-check up-bpi

help:
	@printf '%s\n' 'FT MES development commands:'
	@printf '%s\n' '  make ci                     Run the Java 8 repository CI gate locally'
	@printf '%s\n' '  make ci-java17              Run the Java 17 BPI service/stream/runtime gate'
	@printf '%s\n' '  make verify                 Validate Maven reactor and Docker Compose syntax'
	@printf '%s\n' '  make verify-pom             Validate parent/module POM structure'
	@printf '%s\n' '  make bpi-contracts-test     Generate and test BPI v1 Protobuf contracts'
	@printf '%s\n' '  make bpi-api-contract-check Validate BPI OpenAPI, AsyncAPI and simulation scope'
	@printf '%s\n' '  make qcs-quality-gate-outbox-test Run Java 8 QCS-to-BPI outbox tests'
	@printf '%s\n' '  make qcs-quality-gate-outbox-postgres-test Validate QCS outbox triggers on PostgreSQL 15'
	@printf '%s\n' '  make qcs-quality-gate-outbox-package Build the executable QCS outbox JAR'
	@printf '%s\n' '  make acceptance-qcs-bpi-quality-gate-target Run protected real QCS page to BPI replay acceptance'
	@printf '%s\n' '  make bpi-simulation-test    Run deterministic BPI shadow-batch API acceptance'
	@printf '%s\n' '  make bpi-service-static-check Validate BPI Java 17/PostgreSQL service boundaries'
	@printf '%s\n' '  make bpi-service-test      Run BPI service tests (Java 17; live PG test needs BPI_TEST_DATABASE_URL)'
	@printf '%s\n' '  make bpi-service-package   Build the executable BPI service JAR with Java 17'
	@printf '%s\n' '  make bpi-wms-adapter-test  Test query-first BPI to material-wms delivery with Java 17'
	@printf '%s\n' '  make bpi-wms-adapter-package Build the executable BPI WMS adapter JAR'
	@printf '%s\n' '  make acceptance-entity-model-field-type-matrix Validate PostgreSQL scalar field types and safe conversions'
	@printf '%s\n' '  make acceptance-entity-model-object-association Validate PostgreSQL OBJECT association metadata and storage'
	@printf '%s\n' '  make acceptance-entity-model-field-delete-persistence Validate soft delete and explicit PostgreSQL DROP COLUMN'
	@printf '%s\n' '  make bpi-wms-outage-fixture-test Validate the real Protobuf outage-recovery fixture'
	@printf '%s\n' '  make bpi-stream-static-check Validate the Java 17/Flink streaming module boundaries'
	@printf '%s\n' '  make bpi-stream-test       Run deterministic BPI stream replay tests with Java 17'
	@printf '%s\n' '  make bpi-stream-package    Build the deployable BPI Flink job JAR with Java 17'
	@printf '%s\n' '  make bpi-stream-deployment-check Validate isolated Kafka/Flink deployment assets'
	@printf '%s\n' '  make bpi-stream-compose-config Render the isolated Kafka/Flink Compose project'
	@printf '%s\n' '  make bpi-stream-deploy-preflight Run read-only target-host capacity and artifact gates'
	@printf '%s\n' '  make up-bpi-stream         Start only the isolated BPI Kafka/Flink project after preflight'
	@printf '%s\n' '  make bpi-stream-cluster-smoke Require replicated topics, RUNNING job and checkpoint'
	@printf '%s\n' '  make bpi-stream-broker-failure-recovery Stop one broker, prove quorum/checkpoint progress, then restore it'
	@printf '%s\n' '  make bpi-stream-flink-rollback-rehearsal Restore previous JAR from savepoint, capture state, then restore current JAR'
	@printf '%s\n' '  make bpi-stream-cluster-replay Run the legacy replay only in an explicitly isolated compatibility environment'
	@printf '%s\n' '  make bpi-stream-data-quality-replay Publish marker telemetry and require four Flink-generated quality events'
	@printf '%s\n' '  make bpi-stream-joint-replay Use the browser-published rule and emit only scoped context/telemetry'
	@printf '%s\n' '  make bpi-stream-rule-deactivate Publish typed inactive state and require a Flink APPLIED receipt'
	@printf '%s\n' '  make bpi-stream-rule-lifecycle-evidence Read ACTIVATE/RETIRE Kafka and Flink offsets without publishing'
	@printf '%s\n' '  make bpi-stream-postgres-replay Run the legacy PostgreSQL replay only with source consumers isolated'
	@printf '%s\n' '  make bpi-runtime-upgrade-expand-only Back up PostgreSQL, migrate forward and retain a rollback image'
	@printf '%s\n' '  make bpi-integrated-upgrade-expand-only Upgrade BPI inside the single ADP Compose stack with protected backups'
	@printf '%s\n' '  make bpi-dataset-materializer-test Run deterministic Parquet and object-store worker tests'
	@printf '%s\n' '  make bpi-dataset-catalog-publisher-test Run exact-source and Iceberg catalog publisher tests'
	@printf '%s\n' '  make bpi-dataset-retention-archiver-test Run Object Lock archive and recovery tests'
	@printf '%s\n' '  make bpi-dataset-mlflow-registrar-test Run immutable MLflow dataset registration tests'
	@printf '%s\n' '  make bpi-material-reversal-schema-upgrade-target Back up and add the material-wms red-document schema on the target'
	@printf '%s\n' '  make bpi-material-wms-deploy-target Back up, deploy, restart and probe only target material-wms'
	@printf '%s\n' '  make acceptance-bpi-quality-release-target Test the real ADP batch release page and adapter boundary'
	@printf '%s\n' '  make acceptance-bpi-wms-reconciliation-target Test the real admin WMS original-command reconciliation flow'
	@printf '%s\n' '  make acceptance-bpi-wms-outage-recovery-target Test browser recovery after material-wms command DLQ'
	@printf '%s\n' '  make acceptance-bpi-force-close-target Test governed batch force-close through the real page and API'
	@printf '%s\n' '  make acceptance-bpi-formal-identity-force-close-target Test two real ADP administrator sessions and PostgreSQL persistence'
	@printf '%s\n' '  make acceptance-bpi-formal-identity-wms-reversal-target Test governed WMS reversal with two ADP sessions and PostgreSQL evidence'
	@printf '%s\n' '  make acceptance-bpi-formal-identity-wms-roundtrip-target Test page to isolated Kafka, material-wms, receipt and INBOUND_REVERSED'
	@printf '%s\n' '  make rehearse-bpi-wms-outage-recovery-target Run the guarded target outage, recovery and cleanup rehearsal'
	@printf '%s\n' '  make bpi-stream-capture-savepoint Capture a non-cancelling canonical upgrade savepoint'
	@printf '%s\n' '  make bpi-stream-restore-savepoint Recreate only Flink from the persisted savepoint path'
	@printf '%s\n' '  make bpi-stream-verify-savepoint Verify restored state, new operators, topics and checkpoint'
	@printf '%s\n' '  make bpi-rule-application-flink-acceptance Run KRaft Kafka + Flink MiniCluster checkpoint/restart acceptance'
	@printf '%s\n' '  make bpi-production-context-test Test the Java 8 WOM production-context publisher'
	@printf '%s\n' '  make bpi-production-context-postgres-test Verify trigger/outbox/rollback on PostgreSQL 15'
	@printf '%s\n' '  make down-bpi-stream       Stop BPI Kafka/Flink containers and preserve volumes'
	@printf '%s\n' '  make bpi-runtime-replay-test Run IoT signal to Protobuf/PostgreSQL runtime acceptance'
	@printf '%s\n' '  make bpi-adapter-static-check Validate the Java 8 adapter trust boundaries'
	@printf '%s\n' '  make bpi-adapter-test      Test the Java 8 Keycloak/BPI adapter boundary'
	@printf '%s\n' '  make bpi-adapter-package   Build the Java 8 adapter executable JAR'
	@printf '%s\n' '  make bpi-ui-static-check  Validate the BPI console auth/API/browser boundaries'
	@printf '%s\n' '  make bpi-ui-build         Build the BPI TypeScript/Vite console'
	@printf '%s\n' '  make bpi-ui-test          Run BPI console Playwright acceptance'
	@printf '%s\n' '  make bpi-feature-flag-governance-acceptance-check Validate committed target feature-flag evidence'
	@printf '%s\n' '  make bpi-shell-menu-gate-acceptance-check Validate old MES native-menu browser/API/PostgreSQL evidence'
	@printf '%s\n' '  make up-bpi                Build and start the isolated BPI Compose profile'
	@printf '%s\n' '  make compose-config          Validate Docker Compose rendering'
	@printf '%s\n' '  make runtime-script-check    Validate smoke and runtime patch scripts parse'
	@printf '%s\n' '  make sustainable-check       Validate repository governance invariants'
	@printf '%s\n' '  make ci-required-file-inventory-check Validate CI/governance required file inventory freshness'
	@printf '%s\n' '  make ci-required-file-strict-check Fail if CI/governance required files are untracked'
	@printf '%s\n' '  make project-goal-acceptance-check Validate objective-level acceptance ledger'
	@printf '%s\n' '  make goal-gap-register-check Validate generated objective gap register'
	@printf '%s\n' '  make backend-table-audit-handoff-check Validate backend table-audit handoff/index assets'
	@printf '%s\n' '  make basic-config-coverage-check Validate G-012 basic config coverage ledger'
	@printf '%s\n' '  make basic-config-action-matrix-check Validate G-012 guarded action matrix'
	@printf '%s\n' '  make entity-model-config-crud-readiness-check Validate G-012 entity/model CRUD readiness probe'
	@printf '%s\n' '  make test-environment-address-check Validate current test host defaults and smoke metadata'
	@printf '%s\n' '  make test-environment-static-bundle-link-check Validate current browser entry static bundle link evidence'
	@printf '%s\n' '  make persistence-acceptance-check Validate functional/persistence acceptance assets'
	@printf '%s\n' '  make production-testcase-check Validate production module action test matrix'
	@printf '%s\n' '  make wom-toolbar-action-coverage-check Validate WOM makeTaskList toolbar action coverage ledger'
	@printf '%s\n' '  make production-blocker-check Validate production module blocker ledger'
	@printf '%s\n' '  make production-module-backlog-check Validate production FAIL/BLOCKED backlog ledger'
	@printf '%s\n' '  make production-action-map-check Validate production source action map'
	@printf '%s\n' '  make platform-validation-check Validate committed platform browser/API smoke report'
	@printf '%s\n' '  make runtime-smoke-reports-check Validate committed runtime smoke reports'
	@printf '%s\n' '  make business-dependency-readiness-check Validate business dependency readiness smoke report'
	@printf '%s\n' '  make business-dependency-contract-check Validate business dependency intake contracts'
	@printf '%s\n' '  make business-module-intake-requirements-check Validate blocked production module intake requirements'
	@printf '%s\n' '  make business-package-scan-check Validate missing business dependency package scan report'
	@printf '%s\n' '  make production-export-readiness-check Validate production export readiness smoke report'
	@printf '%s\n' '  make production-export-gap-breakdown-check Validate generated per-target production export gap breakdown'
	@printf '%s\n' '  make production-source-evidence-refresh Refresh production sourceEvidence after smoke reports change'
	@printf '%s\n' '  make production-migration-readiness-check Validate production migration readiness ledger'
	@printf '%s\n' '  make production-cutover-gate-doc Refresh production cutover gate document'
	@printf '%s\n' '  make production-cutover-gate-check Validate the production no-cutover/ready gate'
	@printf '%s\n' '  make production-rehearsal-plan-check Validate production rehearsal evidence checklist'
	@printf '%s\n' '  make production-evidence-ready-gate-regression-check Ensure templates cannot satisfy production READY evidence'
	@printf '%s\n' '  make runtime-patch-manifest Regenerate runtime patch checksum manifest'
	@printf '%s\n' '  make runtime-patch-manifest-check Check runtime patch checksum manifest is fresh'
	@printf '%s\n' '  make rehearse-core-flow-runtime-rollback Roll back and restore WOM core runtime patches on the test host'
	@printf '%s\n' '  make bpi-runtime-image-rollback-rehearsal Roll back BPI service/adapter, browser-smoke, then restore current images'
	@printf '%s\n' '  make bpi-integrated-rollback-rehearsal Roll back BPI service/adapter/Flink together, prove marker persistence, then restore'
	@printf '%s\n' '  make source-module-check     Validate promoted backend source modules'
	@printf '%s\n' '  make source-module-test      Compile and test promoted backend source modules'
	@printf '%s\n' '  make create-backend-module MODULE=platform-auth [PACKAGE=com.example]'
	@printf '%s\n' '  make material-wms-test       Run material WMS integration tests'
	@printf '%s\n' '  make material-wms-package    Build the executable material WMS JAR'
	@printf '%s\n' '  make material-wms-stage-runtime Copy the material WMS JAR into the Docker runtime tree'
	@printf '%s\n' '  make acceptance-material-wms-persistence Run live material WMS API/PostgreSQL marker acceptance'
	@printf '%s\n' '  make process-analysis-test   Run ProcessAnalysis integration tests'
	@printf '%s\n' '  make process-analysis-package Build the executable ProcessAnalysis JAR'
	@printf '%s\n' '  make process-analysis-stage-runtime Copy the ProcessAnalysis JAR into the Docker runtime tree'
	@printf '%s\n' '  make acceptance-process-analysis-persistence Run live trace/API/PostgreSQL marker acceptance'
	@printf '%s\n' '  make acceptance-fructose-line-pilot Run retained fructose WOM/QCS/WMS/BPI/browser acceptance'
	@printf '%s\n' '  make rm-formula-editor-test Run RM Web formula editor service tests'
	@printf '%s\n' '  make rm-formula-editor-package Build the executable RM Web formula editor JAR'
	@printf '%s\n' '  make rm-formula-editor-stage-runtime Copy the RM Web formula editor JAR into the Docker runtime tree'
	@printf '%s\n' '  make acceptance-rm-web-formula-editor-persistence Run visible RM browser/API/PostgreSQL/retry acceptance'
	@printf '%s\n' '  make rm-web-formula-editor-acceptance-check Validate committed RM Web editor acceptance evidence'
	@printf '%s\n' '  make wom-quality-reporting-test Run WOM/QCS bad-quantity integration tests'
	@printf '%s\n' '  make wom-quality-reporting-stage-runtime Copy the WOM/QCS bad-quantity JAR into the Docker runtime tree'
	@printf '%s\n' '  make acceptance-wom-quality-quantity-persistence Run browser/API/PostgreSQL bad-quantity marker acceptance'
	@printf '%s\n' '  make wom-print-test         Run WOM QR generation and print-state tests'
	@printf '%s\n' '  make wom-print-package      Build the executable WOM print JAR'
	@printf '%s\n' '  make wom-print-stage-runtime Copy the WOM print JAR into the Docker runtime tree'
	@printf '%s\n' '  make acceptance-wom-qrcode-persistence Run live WOM QR page/API/PNG/PostgreSQL marker acceptance'
	@printf '%s\n' '  make acceptance-wom-qrcode-browser Run real WOM toolbar/dialog/PNG/PostgreSQL browser acceptance'
	@printf '%s\n' '  make module-intake-check INTAKE=/path/to/package-or-dir'
	@printf '%s\n' '  make module-intake-candidate-report-check Verify committed real-package intake evidence'
	@printf '%s\n' '  make inventory               Regenerate current content inventory'
	@printf '%s\n' '  make inventory-check         Check current content inventory is fresh'
	@printf '%s\n' '  make backend-dependency-inventory Regenerate recovered backend dependency inventory'
	@printf '%s\n' '  make backend-dependency-check Check recovered backend dependency inventory is fresh'
	@printf '%s\n' '  make oracle-audit            Regenerate Oracle migration backlog'
	@printf '%s\n' '  make oracle-audit-check      Check Oracle migration backlog is fresh'
	@printf '%s\n' '  make postgres-migration-index Regenerate PostgreSQL migration index'
	@printf '%s\n' '  make postgres-migration-check Check PostgreSQL migration index is fresh'
	@printf '%s\n' '  make oracle-replacement-status Regenerate Oracle replacement status ledger'
	@printf '%s\n' '  make oracle-replacement-check Check Oracle replacement status ledger is fresh'
	@printf '%s\n' '  make production-source-inventory PROD_MIGRATION_ENV=/secure/env'
	@printf '%s\n' '  make production-target-preflight PROD_MIGRATION_ENV=/secure/env'
	@printf '%s\n' '  make production-rowcount-compare Compare source/target migration row counts'
	@printf '%s\n' '  make production-checksum-compare Compare source/target migration checksums'
	@printf '%s\n' '  make production-db-migration-evidence-check DB_MIGRATION_EVIDENCE=/secure/evidence.json'
	@printf '%s\n' '  make production-db-migration-ready-check DB_MIGRATION_EVIDENCE=/secure/evidence.json'
	@printf '%s\n' '  make production-minio-source-inventory MINIO_MIGRATION_ENV=/secure/env'
	@printf '%s\n' '  make production-minio-target-inventory MINIO_MIGRATION_ENV=/secure/env'
	@printf '%s\n' '  make production-minio-compare Compare source/target MinIO inventories'
	@printf '%s\n' '  make production-minio-migration-evidence-check MINIO_MIGRATION_EVIDENCE=/secure/evidence.json'
	@printf '%s\n' '  make production-minio-migration-ready-check MINIO_MIGRATION_EVIDENCE=/secure/evidence.json'
	@printf '%s\n' '  make production-keycloak-source-export KEYCLOAK_MIGRATION_ENV=/secure/env'
	@printf '%s\n' '  make production-keycloak-target-export KEYCLOAK_MIGRATION_ENV=/secure/env'
	@printf '%s\n' '  make production-keycloak-compare Compare source/target Keycloak realm inventories'
	@printf '%s\n' '  make production-keycloak-migration-evidence-check KEYCLOAK_MIGRATION_EVIDENCE=/secure/evidence.json'
	@printf '%s\n' '  make production-keycloak-migration-ready-check KEYCLOAK_MIGRATION_EVIDENCE=/secure/evidence.json'
	@printf '%s\n' '  make production-rollback-evidence-check ROLLBACK_EVIDENCE=/secure/evidence.json'
	@printf '%s\n' '  make production-rollback-ready-check ROLLBACK_EVIDENCE=/secure/evidence.json'
	@printf '%s\n' '  make production-license-strategy-check LICENSE_DECISION=/secure/decision.json'
	@printf '%s\n' '  make production-license-ready-check LICENSE_DECISION=/secure/decision.json'
	@printf '%s\n' '  make production-network-tls-check NETWORK_TLS_PLAN=/secure/network-tls-plan.json'
	@printf '%s\n' '  make production-network-tls-ready-check NETWORK_TLS_PLAN=/secure/network-tls-plan.json'
	@printf '%s\n' '  make production-security-hardening-check SECURITY_HARDENING_PLAN=/secure/security-hardening-plan.json'
	@printf '%s\n' '  make production-security-hardening-ready-check SECURITY_HARDENING_PLAN=/secure/security-hardening-plan.json'
	@printf '%s\n' '  make production-business-smoke-signoff-check BUSINESS_SMOKE_SIGNOFF=/secure/business-smoke-signoff.json'
	@printf '%s\n' '  make production-business-smoke-signoff-ready-check BUSINESS_SMOKE_SIGNOFF=/secure/business-smoke-signoff.json'
	@printf '%s\n' '  make production-nacos-runtime-patch-check NACOS_RUNTIME_PATCH_EVIDENCE=/secure/nacos-runtime-patch-evidence.json'
	@printf '%s\n' '  make production-nacos-runtime-patch-ready-check NACOS_RUNTIME_PATCH_EVIDENCE=/secure/nacos-runtime-patch-evidence.json'
	@printf '%s\n' '  make render-config           Render Nacos configs from deploy/docker/.env'
	@printf '%s\n' '  make prepare-runtime         Prepare static assets and runtime patch assets'
	@printf '%s\n' '  make up-infra                Start infrastructure services only'
	@printf '%s\n' '  make up                      Start the full Docker profile'
	@printf '%s\n' '  make down                    Stop the Docker profile'
	@printf '%s\n' '  make ps                      Show Docker profile status'
	@printf '%s\n' '  make logs SERVICE=gateway    Tail one service log, or all logs if SERVICE is empty'
	@printf '%s\n' '  make smoke-platform          Run platform API/menu/todo validation against ADP_BASE_URL'
	@printf '%s\n' '  make smoke-api               Run API smoke against ADP_BASE_URL'
	@printf '%s\n' '  make smoke-menu              Run browser menu smoke against ADP_BASE_URL'
	@printf '%s\n' '  make smoke-todo              Run home Todo smoke against ADP_BASE_URL'
	@printf '%s\n' '  make smoke-organization      Run organization department click/API smoke'
	@printf '%s\n' '  make smoke-test-environment  Run current test host HTTP/SSH/Docker smoke'
	@printf '%s\n' '  make smoke-postgres-runtime  Run remote PostgreSQL runtime schema smoke over SSH'
	@printf '%s\n' '  make smoke-nacos-config      Run remote Nacos config drift smoke over SSH'
	@printf '%s\n' '  make smoke-keycloak-jwt      Run remote Keycloak/JWT/Nacos runtime smoke over SSH'
	@printf '%s\n' '  make smoke-minio-runtime     Run remote MinIO bucket inventory smoke over SSH'
	@printf '%s\n' '  make smoke-systemconfig-builtins Run read-only systemconfig built-in catalog smoke'
	@printf '%s\n' '  make smoke-business-dependencies Recheck missing business service blockers'
	@printf '%s\n' '  make business-package-scan   Scan local MES/ADP packages for missing service implementation candidates'
	@printf '%s\n' '  make smoke-production-export-readiness Recheck production list export readiness'
	@printf '%s\n' '  make acceptance-organization-persistence Run organization CRUD persistence acceptance'
	@printf '%s\n' '  make acceptance-organization-group-persistence Run organization group CRUD persistence acceptance'
	@printf '%s\n' '  make acceptance-organization-position-persistence Run organization position CRUD persistence acceptance'
	@printf '%s\n' '  make acceptance-organization-position-role-persistence Run organization position-role persistence acceptance'
	@printf '%s\n' '  make acceptance-organization-company-persistence Run organization company CRUD persistence acceptance'
	@printf '%s\n' '  make acceptance-organization-person-persistence Run organization person CRUD persistence acceptance'
	@printf '%s\n' '  make acceptance-organization-person-user-persistence Run organization person create-account persistence acceptance'
	@printf '%s\n' '  make acceptance-auth-user-persistence Run auth user CRUD/status persistence acceptance'
	@printf '%s\n' '  make acceptance-rbac-permission-persistence Run RBAC role/user permission persistence acceptance'
	@printf '%s\n' '  make acceptance-systemcode-persistence Run system code dictionary CRUD persistence acceptance'
	@printf '%s\n' '  make acceptance-systemconfig-persistence Run system config catalog/value persistence acceptance'
	@printf '%s\n' '  make acceptance-patrol-hidden-danger-persistence Run abnormal PATROL -> pending EAM risk persistence acceptance'
	@printf '%s\n' '  make acceptance-patrol-input-standard-persistence Run PATROL input-standard browser/PostgreSQL CRUD acceptance'
	@printf '%s\n' '  make acceptance-patrol-route-persistence Run PATROL route browser/PostgreSQL CRUD acceptance'
	@printf '%s\n' '  make acceptance-patrol-area-persistence Run PATROL area browser/PostgreSQL CRUD acceptance'
	@printf '%s\n' '  make acceptance-patrol-item-persistence Run PATROL item browser/PostgreSQL CRUD acceptance'
	@printf '%s\n' '  make acceptance-patrol-report Run PATROL report/monitor browser/API/PostgreSQL acceptance'
	@printf '%s\n' '  make acceptance-patrol-gather-data Run PATROL Kafka/TagManagement/PostgreSQL gather-data acceptance'
	@printf '%s\n' '  make smoke-systemconfig-builtins Run built-in system config list/detail/read-only DB smoke'
	@printf '%s\n' '  make acceptance-systemconfig-controlled-runtime-config Run controlled runtime config save/read/rollback acceptance (SYSTEMCONFIG_CONTROLLED_TARGET_MODE=qcs|rm)'
	@printf '%s\n' '  make smoke-runtime-configuration Run read-only entity/runtime configuration readiness smoke'
	@printf '%s\n' '  make smoke-entity-model-config-crud-readiness Run read-only entity/model CRUD readiness probe'
	@printf '%s\n' '  make acceptance-custom-property-persistence Run custom-property model mapping marker persistence acceptance'
	@printf '%s\n' '  make acceptance-wom-start-persistence Run WOM makeTaskList start-state persistence acceptance'
	@printf '%s\n' '  make acceptance-wom-hold-restart-persistence Run WOM makeTaskList start/hold/restart persistence acceptance'
	@printf '%s\n' '  make smoke-wom-toolbar-row Run WOM makeTaskList full toolbar row-click smoke after seeding marker data'
	@printf '%s\n' '  make acceptance-wom-stop-persistence Run WOM makeTaskList start/stop minimal persistence acceptance'
	@printf '%s\n' '  make acceptance-wom-stop-output-persistence Run WOM makeTaskList start/stop output-detail persistence acceptance'
	@printf '%s\n' '  make acceptance-wom-advance-release-persistence Run WOM makeTaskList advance-release persistence acceptance'
	@printf '%s\n' '  make acceptance-wom-prepare-need-persistence Run WOM prepareMakeTaskList material-demand persistence acceptance'
	@printf '%s\n' '  make acceptance-wom-active-persistence Run WOM makeTaskBatchView activity-start persistence acceptance'
	@printf '%s\n' '  make acceptance-wom-active-end-persistence Run WOM makeTaskBatchView activity start/end persistence acceptance'
	@printf '%s\n' '  make acceptance-wom-easy-active-persistence Run WOM easyTaskOperateView endEasyActive persistence acceptance'
	@printf '%s\n' '  make acceptance-wom-putin-active-persistence Run WOM putin-detail save + active end persistence acceptance'
	@printf '%s\n' '  make acceptance-wom-check-active-persistence Run WOM check activity end + check-record persistence acceptance'
	@printf '%s\n' '  make acceptance-wom-process-start-persistence Run WOM makeTaskBatchView process-start persistence acceptance'
	@printf '%s\n' '  make acceptance-wom-process-end-persistence Run WOM makeTaskBatchView process start/end persistence acceptance'
	@printf '%s\n' '  make acceptance-wom-process-unit-persistence Run WOM processUnitEdit work-unit persistence acceptance'
	@printf '%s\n' '  make acceptance-wom-manu-inspect-persistence Run WOM makeTaskList manufacturing inspection persistence acceptance'
	@printf '%s\n' '  make acceptance-wom-checkoutbill-persistence Run WOM quality activity checkout-bill persistence acceptance'
	@printf '%s\n' '  make acceptance-factory-line-persistence Run factory architecture production-line browser/PostgreSQL acceptance'
	@printf '%s\n' '  make probe-wom-qrcode-route Run WOM QR code route/package probe against the test environment'
	@printf '%s\n' '  make acceptance-qcs-report-chain-persistence QCS report save/effective WOM backfill acceptance'
	@printf '%s\n' '  make acceptance-teaminfo-scheduleplan-persistence Run TeamInfo schedule-plan persistence acceptance'
	@printf '%s\n' '  make smoke-rbac-authority    Run role/user authority editor API smoke'
	@printf '%s\n' '  make smoke-business          Run API/layout smoke for restored business module routes'
	@printf '%s\n' '  make smoke-business-page     Run browser page smoke for restored business module routes'
	@printf '%s\n' '  make discover-production-actions Discover safe production create-entry UI actions'
	@printf '%s\n' '  make audit-postgres-mappings Audit mapper SQL for PostgreSQL migration risk'
	@printf '%s\n' '  make audit-postgres-report   Write a non-blocking PostgreSQL audit report'

ci: verify bpi-contracts-test bpi-api-contract-check bpi-simulation-test bpi-service-static-check bpi-stream-static-check bpi-stream-deployment-check bpi-production-context-test qcs-quality-gate-outbox-test bpi-adapter-static-check bpi-ui-static-check bpi-feature-flag-governance-acceptance-check bpi-shell-menu-gate-acceptance-check runtime-script-check sustainable-check ci-required-file-inventory-check ci-required-file-strict-check product-interaction-manual-check project-goal-acceptance-check goal-gap-register-check backend-table-audit-handoff-check basic-config-coverage-check basic-config-action-matrix-check entity-model-config-crud-readiness-check test-environment-address-check test-environment-static-bundle-link-check persistence-acceptance-check production-testcase-check wom-toolbar-action-coverage-check production-blocker-check production-module-backlog-check production-action-map-check platform-validation-check runtime-smoke-reports-check business-dependency-readiness-check business-dependency-contract-check business-module-intake-requirements-check business-package-scan-check production-export-readiness-check production-export-gap-breakdown-check production-source-evidence-refresh-check production-migration-readiness-check production-cutover-gate-check production-rehearsal-plan-check production-db-migration-evidence-check production-rollback-evidence-check production-license-strategy-check production-network-tls-check production-security-hardening-check production-business-smoke-signoff-check production-nacos-runtime-patch-check production-minio-migration-evidence-check production-keycloak-migration-evidence-check production-evidence-ready-gate-regression-check runtime-patch-manifest-check rm-web-formula-editor-acceptance-check source-module-check module-intake-precheck-regression-check module-intake-candidate-report-check source-module-test inventory-check backend-dependency-check oracle-audit-check postgres-migration-check oracle-replacement-check audit-postgres-mappings

ci-java17: bpi-contracts-test bpi-api-contract-check bpi-runtime-replay-test bpi-stream-test bpi-service-test bpi-dataset-mlflow-registrar-test

verify: verify-pom compose-config

verify-pom:
	$(MVN) -q -DskipTests validate

compose-config:
	$(COMPOSE) config --quiet
	$(COMPOSE) --profile bpi-ml config --quiet

runtime-script-check:
	sh -n deploy/docker/scripts/build-foundation-simulated-login-serverport-patch.sh
	sh -n deploy/bpi-streaming/scripts/create-topics.sh
	sh -n deploy/bpi-streaming/scripts/preflight.sh
	sh -n deploy/bpi-streaming/scripts/smoke-cluster.sh
	sh -n deploy/bpi-streaming/scripts/run-broker-failure-recovery.sh
	sh -n deploy/bpi-streaming/scripts/run-flink-job-rollback-rehearsal.sh
	sh -n deploy/bpi-streaming/scripts/run-replay.sh
	sh -n deploy/bpi-streaming/scripts/run-joint-replay.sh
	sh -n deploy/bpi-streaming/scripts/run-rule-deactivation.sh
	sh -n deploy/bpi-streaming/scripts/run-rule-lifecycle-evidence.sh
	sh -n deploy/bpi-streaming/scripts/run-postgres-replay.sh
	sh -n deploy/bpi-streaming/scripts/run-rule-application-flink-acceptance.sh
	sh -n deploy/bpi-streaming/scripts/start-jobmanager.sh
	sh -n deploy/bpi-streaming/scripts/capture-upgrade-savepoint.sh
	sh -n deploy/bpi-streaming/scripts/restore-from-savepoint.sh
	sh -n deploy/bpi-streaming/scripts/verify-savepoint-restore.sh
	sh -n deploy/bpi-runtime/scripts/preflight.sh
	sh -n deploy/bpi-runtime/scripts/smoke.sh
	sh -n deploy/bpi-runtime/scripts/upgrade-expand-only.sh
	sh -n deploy/docker/postgres/ensure-bpi-materializer-role.sh
	sh -n deploy/docker/postgres/ensure-bpi-catalog-publisher-role.sh
	sh -n deploy/docker/postgres/ensure-bpi-retention-archiver-role.sh
	sh -n deploy/docker/postgres/ensure-bpi-mlflow-registrar-role.sh
	sh -n deploy/minio/bootstrap-bpi-dataset-bucket.sh
	sh -n deploy/minio/bootstrap-bpi-iceberg-warehouse.sh
	sh -n deploy/minio/bootstrap-bpi-dataset-recovery-bucket.sh
	sh -n deploy/minio/bootstrap-bpi-mlflow-artifact-bucket.sh
	sh -n services/bpi-mlflow/start-mlflow.sh
	sh -n deploy/polaris/check_metastore_bootstrap.sh
	sh -n deploy/polaris/bootstrap_metastore_if_required.sh
	sh -n deploy/docker/scripts/upgrade-bpi-integrated-expand-only.sh
	sh -n deploy/docker/scripts/run-bpi-telemetry-landing-target-acceptance.sh
	$(PYTHON) -m py_compile deploy/polaris/bootstrap_bpi_catalog.py
	$(PYTHON) -m unittest deploy/polaris/test_bootstrap_bpi_catalog.py
	$(PYTHON) -m unittest deploy/polaris/test_metastore_bootstrap_gate.py
	$(PYTHON) -m py_compile scripts/verify-bpi-release-migrations.py
	$(PYTHON) -m unittest scripts/test_verify_bpi_release_migrations.py
	$(NODE) --check deploy/bpi-runtime/scripts/browser-joint-acceptance.js
	$(NODE) --check deploy/bpi-runtime/scripts/browser-live-batch-governance-acceptance.js
	$(NODE) --check deploy/bpi-runtime/scripts/browser-topology-rule-acceptance.js
	$(NODE) --check deploy/bpi-runtime/scripts/browser-point-catalog-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-bpi-mqtt-ingress-browser-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-bpi-rule-retirement-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-bpi-runtime-image-rollback-rehearsal.js
	$(NODE) --check deploy/docker/scripts/adp-bpi-integrated-rollback-rehearsal.js
	BPI_ROLLBACK_SERVICE_IMAGE=static:test BPI_ROLLBACK_ADAPTER_IMAGE=static:test BPI_ROLLBACK_JOB_JAR=/tmp/old.jar BPI_LOAD_CLIENT_JOB_JAR=/tmp/new.jar ADP_BASE_URL=http://127.0.0.1 ADP_USERNAME=static ADP_PASSWORD=static BPI_INTEGRATED_ROLLBACK_CONFIRM=ROLLBACK_BPI_SERVICE_ADAPTER_FLINK_AND_RESTORE $(NODE) deploy/docker/scripts/adp-bpi-integrated-rollback-rehearsal.js --print-remote-script > /tmp/adp-bpi-integrated-rollback-remote-script.sh
	sh -n /tmp/adp-bpi-integrated-rollback-remote-script.sh
	$(NODE) --check deploy/docker/scripts/adp-bpi-quality-release-target-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-bpi-dataset-manifest-target-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-bpi-dataset-materialization-target-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-bpi-dataset-catalog-target-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-bpi-dataset-retention-target-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-bpi-dataset-mlflow-target-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-bpi-dataset-training-readiness-target-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-bpi-field-data-coverage-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-bpi-telemetry-landing-acceptance.js
	$(PYTHON) -m py_compile deploy/docker/scripts/bpi-dataset-catalog-post-commit-failure-injection.py
	$(PYTHON) -m py_compile deploy/docker/scripts/verify-bpi-dataset-parquet-v2-object.py
	$(NODE) --check deploy/docker/scripts/run-qcs-bpi-quality-gate-target.js
	$(NODE) --check deploy/docker/scripts/adp-bpi-wms-outage-recovery-acceptance.js
	$(NODE) --check deploy/docker/scripts/generate-bpi-wms-outage-fixture.js
	$(NODE) --check deploy/docker/scripts/test-bpi-wms-outage-fixture.js
	$(NODE) --check deploy/docker/scripts/run-bpi-wms-outage-recovery-target.js
	$(NODE) --test deploy/docker/scripts/test-bpi-wms-outage-fixture.js
	$(NODE) --check deploy/docker/scripts/adp-rm-web-formula-editor-persistence-acceptance.js
	$(PYTHON) -m py_compile scripts/verify-bpi-stream-deployment.py
	$(PYTHON) -m py_compile scripts/verify-bpi-feature-flag-governance.py
	sh -n deploy/docker/scripts/prepare-runtime-patches.sh
	sh -n deploy/docker/scripts/prepare-qcs-static-assets.sh
	sh -n deploy/docker/scripts/patch-lims-qcs-inspect-report-service.sh
	sh -n deploy/docker/scripts/build-rm-import-transaction-patch.sh
	sh -n deploy/docker/scripts/build-wom-core-production-boot-jar.sh
	sh -n deploy/docker/scripts/build-wom-public-produce-created-disabled-boot-jar.sh
	sh -n deploy/database/production-migration/scripts/run-target-preflight.sh
	sh -n deploy/database/production-migration/scripts/run-source-inventory.sh
	$(PYTHON) -m py_compile deploy/database/production-migration/scripts/compare-row-counts.py
	$(PYTHON) -m py_compile deploy/database/production-migration/scripts/compare-checksums.py
	$(PYTHON) -m py_compile deploy/database/production-migration/scripts/validate-migration-evidence.py
	sh -n deploy/minio/production-migration/scripts/run-bucket-inventory.sh
	$(PYTHON) -m py_compile deploy/minio/production-migration/scripts/normalize-mc-ls-json.py
	$(PYTHON) -m py_compile deploy/minio/production-migration/scripts/compare-bucket-inventory.py
	$(PYTHON) -m py_compile deploy/minio/production-migration/scripts/validate-minio-migration-evidence.py
	sh -n deploy/keycloak/production-migration/scripts/export-realm-inventory.sh
	$(PYTHON) -m py_compile deploy/keycloak/production-migration/scripts/normalize-realm-inventory.py
	$(PYTHON) -m py_compile deploy/keycloak/production-migration/scripts/compare-realm-inventory.py
	$(PYTHON) -m py_compile deploy/keycloak/production-migration/scripts/validate-keycloak-migration-evidence.py
	$(PYTHON) -m py_compile deploy/rollback/production-migration/scripts/validate-rollback-evidence.py
	$(PYTHON) -m py_compile deploy/license/production-migration/scripts/validate-license-decision.py
	$(PYTHON) -m py_compile deploy/network/production-migration/scripts/validate-network-tls-plan.py
	$(PYTHON) -m py_compile deploy/security/production-migration/scripts/validate-security-hardening-plan.py
	$(PYTHON) -m py_compile deploy/business-smoke/production-migration/scripts/validate-business-smoke-signoff.py
	$(PYTHON) -m py_compile deploy/nacos/production-migration/scripts/validate-nacos-runtime-patch-evidence.py
	$(PYTHON) -m py_compile scripts/verify-project-goal-acceptance.py
	$(PYTHON) -m py_compile scripts/generate-goal-gap-register.py
	$(PYTHON) -m py_compile scripts/verify-backend-table-audit-handoff.py
	$(PYTHON) -m py_compile scripts/verify-basic-config-coverage.py
	$(PYTHON) -m py_compile scripts/verify-basic-config-action-matrix.py
	$(PYTHON) -m py_compile scripts/verify-entity-model-config-crud-readiness.py
	$(PYTHON) -m py_compile scripts/verify-test-environment-address.py
	$(PYTHON) -m py_compile scripts/verify-test-environment-static-bundle-link.py
	$(PYTHON) -m py_compile scripts/verify-persistence-acceptance.py
	$(PYTHON) -m py_compile scripts/verify-rm-web-formula-editor-acceptance.py
	$(PYTHON) -m py_compile scripts/verify-production-module-test-cases.py
	$(PYTHON) -m py_compile scripts/verify-wom-toolbar-action-coverage.py
	$(PYTHON) -m py_compile scripts/verify-production-module-backlog.py
	$(PYTHON) -m py_compile scripts/verify-runtime-smoke-reports.py
	$(PYTHON) -m py_compile scripts/verify-platform-validation-smoke.py
	$(PYTHON) -m py_compile scripts/verify-business-dependency-readiness.py
	$(PYTHON) -m py_compile scripts/verify-business-dependency-contracts.py
	$(PYTHON) -m py_compile scripts/verify-business-module-intake-requirements.py
	$(PYTHON) -m py_compile scripts/scan-business-dependency-packages.py
	$(PYTHON) -m py_compile scripts/verify-business-dependency-package-scan.py
	$(PYTHON) -m py_compile scripts/verify-production-export-readiness.py
	$(PYTHON) -m py_compile scripts/generate-production-export-gap-breakdown.py
	$(PYTHON) -m py_compile scripts/verify-production-migration-readiness.py
	$(PYTHON) -m py_compile scripts/refresh-production-source-evidence.py
	$(PYTHON) -m py_compile scripts/verify-production-cutover-gate.py
	$(PYTHON) -m py_compile scripts/generate-production-rehearsal-plan.py
	$(PYTHON) -m py_compile scripts/verify-production-rehearsal-plan.py
	$(PYTHON) -m py_compile scripts/verify-production-evidence-ready-gates.py
	$(PYTHON) -m py_compile scripts/verify-module-intake-precheck.py
	$(PYTHON) -m py_compile scripts/verify-module-intake-candidate-report.py
	$(PYTHON) -m py_compile scripts/generate-ci-required-file-inventory.py
	$(PYTHON) -m unittest scripts/test_generate_ci_required_file_inventory.py
	$(NODE) --check deploy/docker/scripts/adp-platform-api-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-menu-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-home-todo-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-organization-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-organization-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-organization-group-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-organization-position-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-organization-position-role-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-organization-company-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-organization-person-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-organization-person-user-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-auth-user-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-rbac-permission-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-systemcode-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-systemconfig-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-systemconfig-builtins-readiness-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-systemconfig-controlled-runtime-config-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-runtime-configuration-readiness-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-entity-model-config-crud-readiness-probe.js
	$(NODE) --check deploy/docker/scripts/adp-entity-model-field-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-entity-model-field-delete-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-custom-property-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-patrol-task-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-patrol-input-standard-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-patrol-route-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-patrol-area-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-patrol-item-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-wom-start-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-wom-manual-task-entry-persistence-acceptance.js
	$(NODE) --test deploy/docker/scripts/test-wom-manual-entry-auth-boundary.js
	$(NODE) --check deploy/docker/scripts/adp-material-wms-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-process-analysis-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-mes-full-production-flow-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-fructose-line-pilot-acceptance.js
	$(NODE) --check deploy/docker/scripts/qcs-quality-profile.js
	$(NODE) --test deploy/docker/scripts/qcs-quality-profile.test.js
	$(NODE) --check deploy/docker/scripts/adp-core-flow-runtime-rollback-rehearsal.js
	$(NODE) --check deploy/docker/scripts/adp-wom-toolbar-row-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-wom-process-execution-actions-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-wom-prepare-need-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-wom-active-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-wom-manu-inspect-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-wom-checkoutbill-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-qcs-inspect-report-config-regression.js
	$(NODE) --check deploy/docker/scripts/adp-factory-line-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-wom-public-produce-task-created-noop-probe.js
	$(NODE) --check deploy/docker/scripts/adp-wom-qrcode-route-probe.js
	$(NODE) --check deploy/docker/scripts/adp-wom-qrcode-browser-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-qcs-report-chain-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/test-qcs-display-bindings.js
	$(NODE) deploy/docker/scripts/test-qcs-display-bindings.js
	$(NODE) --check deploy/docker/scripts/test-workflow-editor-locale-compat.js
	$(NODE) deploy/docker/scripts/test-workflow-editor-locale-compat.js
	$(NODE) --check deploy/docker/scripts/test-workflow-config-entry-popup-fallback.js
	$(NODE) deploy/docker/scripts/test-workflow-config-entry-popup-fallback.js
	$(NODE) --check deploy/docker/scripts/adp-teaminfo-scheduleplan-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-craftgraph-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-rbac-authority-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-admin-permission-directory-scan.js
	$(NODE) --check deploy/docker/scripts/adp-business-module-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-business-page-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-production-action-discovery.js
	$(NODE) --check deploy/docker/scripts/adp-platform-validation-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-test-environment-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-postgres-runtime-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-nacos-config-drift-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-keycloak-jwt-runtime-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-minio-runtime-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-business-dependency-readiness-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-production-export-readiness-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-bpi-version-lifecycle-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-bpi-force-close-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-bpi-formal-identity-force-close-acceptance.js
	ADP_BASE_URL=http://127.0.0.1 ADP_USERNAME=static ADP_PASSWORD=static NODE_PATH="$(CURDIR)/frontend/apps/bpi/node_modules" $(NODE) deploy/docker/scripts/adp-bpi-formal-identity-force-close-acceptance.js --print-remote-script > /tmp/adp-bpi-formal-identity-force-close-remote-script.sh
	sh -n /tmp/adp-bpi-formal-identity-force-close-remote-script.sh
	$(NODE) --check deploy/docker/scripts/adp-bpi-wms-inbound-reversal-target-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-bpi-formal-identity-wms-reversal-acceptance.js
	$(NODE) --check deploy/docker/scripts/apply-material-wms-reversal-expand-only-target.js
	$(NODE) --check deploy/docker/scripts/deploy-material-wms-target.js
	$(NODE) deploy/docker/scripts/deploy-material-wms-target.js --print-remote-script > /tmp/adp-bpi-material-wms-deploy-remote-script.sh
	sh -n /tmp/adp-bpi-material-wms-deploy-remote-script.sh
	ADP_BASE_URL=http://127.0.0.1 ADP_USERNAME=static ADP_PASSWORD=static NODE_PATH="$(CURDIR)/frontend/apps/bpi/node_modules" $(NODE) deploy/docker/scripts/adp-bpi-formal-identity-wms-reversal-acceptance.js --print-remote-script > /tmp/adp-bpi-formal-identity-wms-reversal-remote-script.sh
	sh -n /tmp/adp-bpi-formal-identity-wms-reversal-remote-script.sh
	$(NODE) --check deploy/docker/scripts/test-adp-production-export-readiness-smoke.js
	$(NODE) deploy/docker/scripts/test-adp-production-export-readiness-smoke.js
	mkdir -p /tmp/adp-rm-export-compat-check
	$(JAVAC) -encoding UTF-8 -source 8 -target 8 -d /tmp/adp-rm-export-compat-check deploy/docker/compat/rm-export/RmFormulaExportCompatServer.java deploy/docker/compat/rm-export/RmFormulaExportCompatServerTest.java
	$(JAVA) -cp /tmp/adp-rm-export-compat-check RmFormulaExportCompatServerTest
	$(NODE) --check deploy/docker/scripts/adp-patrol-report-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-patrol-gather-data-runtime-acceptance.js
	$(PYTHON) -m py_compile deploy/docker/scripts/generate-business-view-runtime-sql.py
	$(PYTHON) -m py_compile deploy/docker/scripts/patch-rm-batch-formula-list.py
	$(PYTHON) -m unittest deploy/docker/scripts/test_patch_rm_batch_formula_list.py
	$(PYTHON) -m unittest deploy/docker/scripts/test_generate_business_view_runtime_sql.py
	$(PYTHON) -m py_compile deploy/docker/scripts/generate-module-access-workflow-sql.py
	$(PYTHON) -m unittest deploy/docker/scripts/test_generate_module_access_workflow_sql.py
	$(PYTHON) -m py_compile deploy/docker/scripts/generate-module-i18n-js.py
	$(PYTHON) -m unittest deploy/docker/scripts/test_generate_module_i18n_js.py
	$(PYTHON) -m py_compile deploy/docker/scripts/generate-module-system-code-sql.py
	$(PYTHON) -m unittest deploy/docker/scripts/test_generate_module_system_code_sql.py
	$(PYTHON) -m py_compile deploy/docker/scripts/patch-patrol-postgres-source.py
	$(PYTHON) -m unittest deploy/docker/scripts/test_patch_patrol_postgres_source.py
	$(PYTHON) -m py_compile deploy/docker/scripts/test_foundation_simulated_login_patch.py
	$(PYTHON) -m unittest deploy/docker/scripts/test_foundation_simulated_login_patch.py
	$(PYTHON) -m py_compile deploy/docker/scripts/patch-eam-patrol-runtime.py
	$(PYTHON) -m unittest deploy/docker/scripts/test_patch_eam_patrol_runtime.py
	$(PYTHON) -m unittest deploy/docker/scripts/test_configuration_postgres_model_sync.py
	$(PYTHON) -m unittest deploy/docker/scripts/test_configuration_workflow_postgres_compat.py
	$(NODE) deploy/docker/scripts/test-patrol-monitor-fallback.js
	$(NODE) deploy/docker/scripts/test-patrol-static-assets.js
	$(PYTHON) -m py_compile deploy/docker/scripts/audit-postgres-mappings.py
	$(PYTHON) -m unittest deploy/docker/scripts/test_audit_postgres_mappings.py
	$(PYTHON) -m py_compile deploy/docker/scripts/patch-orgmanagement-rbac-permission-mapper.py
	$(PYTHON) -m py_compile deploy/docker/scripts/patch-msgmanagement-notice-protocol-mapper.py
	$(PYTHON) -m py_compile deploy/docker/scripts/patch-eam-reactapi-ready.py
	$(PYTHON) -m py_compile deploy/docker/scripts/patch-wts-runtime-compat.py
	$(PYTHON) -m py_compile scripts/generate-runtime-patch-manifest.py

sustainable-check:
	$(PYTHON) scripts/verify-sustainable-repo.py

ci-required-file-inventory:
	$(PYTHON) scripts/generate-ci-required-file-inventory.py --report "$(CI_REQUIRED_FILE_INVENTORY)"

ci-required-file-inventory-check:
	$(PYTHON) scripts/generate-ci-required-file-inventory.py --report "$(CI_REQUIRED_FILE_INVENTORY)" --check

ci-required-file-strict-check:
	$(PYTHON) scripts/generate-ci-required-file-inventory.py --report "$(CI_REQUIRED_FILE_INVENTORY)" --check --strict-tracked

product-interaction-manual-check:
	$(PYTHON) scripts/verify-product-interaction-manual.py

project-goal-acceptance-check: render-config
	$(PYTHON) scripts/verify-project-goal-acceptance.py

goal-gap-register:
	$(PYTHON) scripts/generate-goal-gap-register.py

goal-gap-register-check:
	$(PYTHON) scripts/generate-goal-gap-register.py --check

backend-table-audit-handoff-check:
	$(PYTHON) scripts/verify-backend-table-audit-handoff.py

basic-config-coverage-check:
	$(PYTHON) scripts/verify-basic-config-coverage.py

basic-config-action-matrix-check:
	$(PYTHON) scripts/verify-basic-config-action-matrix.py

entity-model-config-crud-readiness-check:
	$(PYTHON) scripts/verify-entity-model-config-crud-readiness.py --report "$(ENTITY_MODEL_CONFIG_CRUD_READINESS_OUTPUT)"

test-environment-address-check:
	$(PYTHON) scripts/verify-test-environment-address.py

test-environment-static-bundle-link-check:
	$(PYTHON) scripts/verify-test-environment-static-bundle-link.py --report "$(TEST_ENVIRONMENT_STATIC_BUNDLE_LINK_REPORT)"

persistence-acceptance-check:
	$(PYTHON) scripts/verify-persistence-acceptance.py

production-testcase-check:
	$(PYTHON) scripts/verify-production-module-test-cases.py

wom-toolbar-action-coverage-check:
	$(PYTHON) scripts/verify-wom-toolbar-action-coverage.py

production-blocker-check:
	$(PYTHON) scripts/verify-production-module-blockers.py

production-module-backlog-check:
	$(PYTHON) scripts/verify-production-module-backlog.py

production-action-map-check:
	$(PYTHON) scripts/verify-production-action-map.py

platform-validation-check:
	$(PYTHON) scripts/verify-platform-validation-smoke.py --report "$(PLATFORM_VALIDATION_REPORT)" --expected-host "$(RUNTIME_SMOKE_EXPECTED_HOST)" --expected-browser-base-url "$(PLATFORM_VALIDATION_EXPECTED_BROWSER_BASE_URL)" --min-menu-pages "$(PLATFORM_VALIDATION_MIN_MENU_PAGES)"

runtime-smoke-reports-check:
	$(PYTHON) scripts/verify-runtime-smoke-reports.py --expected-host "$(RUNTIME_SMOKE_EXPECTED_HOST)"

business-dependency-readiness-check:
	$(PYTHON) scripts/verify-business-dependency-readiness.py --report "$(BUSINESS_DEPENDENCY_SMOKE_REPORT)"

business-dependency-contract-check:
	$(PYTHON) scripts/verify-business-dependency-contracts.py

business-module-intake-requirements-check:
	$(PYTHON) scripts/verify-business-module-intake-requirements.py

business-package-scan-check:
	$(PYTHON) scripts/verify-business-dependency-package-scan.py --report "$(BUSINESS_PACKAGE_SCAN_REPORT)"

production-export-readiness-check:
	$(PYTHON) scripts/verify-production-export-readiness.py --report "$(PRODUCTION_EXPORT_SMOKE_REPORT)"

production-export-gap-breakdown:
	$(PYTHON) scripts/generate-production-export-gap-breakdown.py

production-export-gap-breakdown-check:
	$(PYTHON) scripts/generate-production-export-gap-breakdown.py --check

production-source-evidence-refresh:
	$(PYTHON) scripts/refresh-production-source-evidence.py

production-source-evidence-refresh-check:
	$(PYTHON) scripts/refresh-production-source-evidence.py --check

production-migration-readiness-check:
	$(PYTHON) scripts/verify-production-migration-readiness.py

production-cutover-gate-doc:
	$(PYTHON) scripts/verify-production-cutover-gate.py --write-doc

production-cutover-gate-check:
	$(PYTHON) scripts/verify-production-cutover-gate.py

production-rehearsal-plan:
	$(PYTHON) scripts/generate-production-rehearsal-plan.py

production-rehearsal-plan-check:
	$(PYTHON) scripts/generate-production-rehearsal-plan.py --check
	$(PYTHON) scripts/verify-production-rehearsal-plan.py

runtime-patch-manifest:
	$(PYTHON) scripts/generate-runtime-patch-manifest.py

runtime-patch-manifest-check:
	$(PYTHON) scripts/generate-runtime-patch-manifest.py --check

bpi-contracts-test:
	$(MVN) -q -pl contracts/bpi-events -am test

bpi-api-contract-check:
	$(PYTHON) scripts/verify-bpi-api-contracts.py

bpi-simulation-test:
	$(NODE) --test simulation/bpi/*.test.js

bpi-service-static-check:
	$(PYTHON) scripts/verify-bpi-service.py

bpi-service-test:
	$(MVN) -f services/bpi-service/pom.xml test

bpi-service-package:
	$(MVN) -f services/bpi-service/pom.xml -DskipTests package

bpi-dataset-materializer-test:
	PYTHONPATH="$(CURDIR)/services/bpi-dataset-materializer/src" \
		$(PYTHON) -m unittest discover \
		-s services/bpi-dataset-materializer/tests -v

bpi-dataset-catalog-publisher-test:
	PYTHONPATH="$(CURDIR)/services/bpi-dataset-catalog-publisher/src" \
		$(PYTHON) -m unittest discover \
		-s services/bpi-dataset-catalog-publisher/tests -v

bpi-dataset-retention-archiver-test:
	PYTHONPATH="$(CURDIR)/services/bpi-dataset-catalog-publisher/src:$(CURDIR)/services/bpi-dataset-retention-archiver/src" \
		$(PYTHON) -m unittest discover \
		-s services/bpi-dataset-retention-archiver/tests -v

bpi-dataset-mlflow-registrar-test:
	PYTHONPATH="$(CURDIR)/services/bpi-dataset-mlflow-registrar/src" \
		$(PYTHON) -m unittest discover \
		-s services/bpi-dataset-mlflow-registrar/tests -v

bpi-wms-adapter-test:
	$(MVN) -f services/bpi-service/pom.xml -pl wms-adapter -am test

bpi-wms-adapter-package:
	$(MVN) -f services/bpi-service/pom.xml -pl wms-adapter -am -DskipTests package

bpi-wms-outage-fixture-test:
	$(NODE) --test deploy/docker/scripts/test-bpi-wms-outage-fixture.js

bpi-stream-static-check:
	$(PYTHON) scripts/verify-bpi-streaming.py

bpi-runtime-upgrade-expand-only: bpi-service-package
	@if [ ! -f "deploy/bpi-runtime/.env" ]; then printf '%s\n' 'ERROR: deploy/bpi-runtime/.env is required' >&2; exit 1; fi
	sh deploy/bpi-runtime/scripts/upgrade-expand-only.sh deploy/bpi-runtime/.env

bpi-integrated-upgrade-expand-only:
	sh deploy/docker/scripts/upgrade-bpi-integrated-expand-only.sh

acceptance-bpi-quality-release-target:
	NODE_PATH="$(CURDIR)/frontend/apps/bpi/node_modules" node deploy/docker/scripts/adp-bpi-quality-release-target-acceptance.js

acceptance-bpi-wms-reconciliation-target:
	NODE_PATH="$(CURDIR)/frontend/apps/bpi/node_modules" node deploy/docker/scripts/adp-bpi-wms-reconciliation-acceptance.js

acceptance-bpi-wms-outage-recovery-target:
	NODE_PATH="$(CURDIR)/frontend/apps/bpi/node_modules" node deploy/docker/scripts/adp-bpi-wms-outage-recovery-acceptance.js

acceptance-bpi-force-close-target:
	NODE_PATH="$(CURDIR)/frontend/apps/bpi/node_modules" node deploy/docker/scripts/adp-bpi-force-close-acceptance.js

acceptance-bpi-formal-identity-force-close-target:
	NODE_PATH="$(CURDIR)/frontend/apps/bpi/node_modules" node deploy/docker/scripts/adp-bpi-formal-identity-force-close-acceptance.js

acceptance-bpi-formal-identity-wms-reversal-target:
	NODE_PATH="$(CURDIR)/frontend/apps/bpi/node_modules" node deploy/docker/scripts/adp-bpi-formal-identity-wms-reversal-acceptance.js

bpi-material-reversal-schema-upgrade-target:
	NODE_PATH="$(CURDIR)/frontend/apps/bpi/node_modules" node deploy/docker/scripts/apply-material-wms-reversal-expand-only-target.js

bpi-material-wms-deploy-target:
	NODE_PATH="$(CURDIR)/frontend/apps/bpi/node_modules" node deploy/docker/scripts/deploy-material-wms-target.js

acceptance-bpi-formal-identity-wms-roundtrip-target:
	BPI_FORMAL_IDENTITY_FULL_ROUNDTRIP=true NODE_PATH="$(CURDIR)/frontend/apps/bpi/node_modules" node deploy/docker/scripts/adp-bpi-formal-identity-wms-reversal-acceptance.js

acceptance-qcs-bpi-quality-gate-target:
	NODE_PATH="$(CURDIR)/frontend/apps/bpi/node_modules" node deploy/docker/scripts/run-qcs-bpi-quality-gate-target.js

rehearse-bpi-wms-outage-recovery-target:
	NODE_PATH="$(CURDIR)/frontend/apps/bpi/node_modules" node deploy/docker/scripts/run-bpi-wms-outage-recovery-target.js

bpi-stream-test:
	$(MVN) -f streaming/pom.xml -pl bpi-stream-engine -am test

bpi-stream-package:
	$(MVN) -f streaming/pom.xml -pl bpi-stream-engine -am -DskipTests package

bpi-stream-deployment-check:
	$(PYTHON) scripts/verify-bpi-stream-deployment.py

bpi-stream-compose-config:
	$(BPI_STREAM_COMPOSE) config --quiet

bpi-stream-deploy-preflight: bpi-stream-package
	@if [ ! -f "$(BPI_STREAM_ENV_FILE)" ]; then printf '%s\n' 'ERROR: copy deploy/bpi-streaming/.env.example to deploy/bpi-streaming/.env and replace secrets' >&2; exit 1; fi
	sh $(BPI_STREAM_DEPLOY_DIR)/scripts/preflight.sh "$(BPI_STREAM_ENV_FILE)"

up-bpi-stream: bpi-stream-deploy-preflight
	$(BPI_STREAM_COMPOSE) up -d

bpi-stream-cluster-smoke:
	@if [ ! -f "$(BPI_STREAM_ENV_FILE)" ]; then printf '%s\n' 'ERROR: deploy/bpi-streaming/.env is required' >&2; exit 1; fi
	sh $(BPI_STREAM_DEPLOY_DIR)/scripts/smoke-cluster.sh "$(BPI_STREAM_ENV_FILE)"

bpi-stream-broker-failure-recovery:
	@if [ ! -f "$(BPI_STREAM_ENV_FILE)" ]; then printf '%s\n' 'ERROR: deploy/bpi-streaming/.env is required' >&2; exit 1; fi
	sh $(BPI_STREAM_DEPLOY_DIR)/scripts/run-broker-failure-recovery.sh "$(BPI_STREAM_ENV_FILE)"

bpi-stream-flink-rollback-rehearsal:
	@if [ ! -f "$(BPI_STREAM_ENV_FILE)" ]; then printf '%s\n' 'ERROR: deploy/bpi-streaming/.env is required' >&2; exit 1; fi
	sh $(BPI_STREAM_DEPLOY_DIR)/scripts/run-flink-job-rollback-rehearsal.sh "$(BPI_STREAM_ENV_FILE)"

bpi-stream-cluster-replay:
	@if [ ! -f "$(BPI_STREAM_ENV_FILE)" ]; then printf '%s\n' 'ERROR: deploy/bpi-streaming/.env is required' >&2; exit 1; fi
	sh $(BPI_STREAM_DEPLOY_DIR)/scripts/run-replay.sh "$(BPI_STREAM_ENV_FILE)"

bpi-stream-data-quality-replay:
	@if [ ! -f "$(BPI_STREAM_ENV_FILE)" ]; then printf '%s\n' 'ERROR: deploy/bpi-streaming/.env is required' >&2; exit 1; fi
	sh $(BPI_STREAM_DEPLOY_DIR)/scripts/run-data-quality-flink-replay.sh "$(BPI_STREAM_ENV_FILE)"

bpi-stream-joint-replay:
	@if [ ! -f "$(BPI_STREAM_ENV_FILE)" ]; then printf '%s\n' 'ERROR: deploy/bpi-streaming/.env is required' >&2; exit 1; fi
	sh $(BPI_STREAM_DEPLOY_DIR)/scripts/run-joint-replay.sh "$(BPI_STREAM_ENV_FILE)"

bpi-stream-rule-deactivate:
	@if [ ! -f "$(BPI_STREAM_ENV_FILE)" ]; then printf '%s\n' 'ERROR: deploy/bpi-streaming/.env is required' >&2; exit 1; fi
	sh $(BPI_STREAM_DEPLOY_DIR)/scripts/run-rule-deactivation.sh "$(BPI_STREAM_ENV_FILE)"

bpi-stream-rule-lifecycle-evidence:
	@if [ ! -f "$(BPI_STREAM_ENV_FILE)" ]; then printf '%s\n' 'ERROR: deploy/bpi-streaming/.env is required' >&2; exit 1; fi
	sh $(BPI_STREAM_DEPLOY_DIR)/scripts/run-rule-lifecycle-evidence.sh "$(BPI_STREAM_ENV_FILE)"

bpi-stream-postgres-replay:
	@if [ ! -f "$(BPI_STREAM_ENV_FILE)" ]; then printf '%s\n' 'ERROR: deploy/bpi-streaming/.env is required' >&2; exit 1; fi
	@if [ ! -f "$(ENV_FILE)" ]; then printf '%s\n' 'ERROR: deploy/docker/.env is required' >&2; exit 1; fi
	sh $(BPI_STREAM_DEPLOY_DIR)/scripts/run-postgres-replay.sh "$(BPI_STREAM_ENV_FILE)" "$(ENV_FILE)"

bpi-stream-capture-savepoint:
	@if [ ! -f "$(BPI_STREAM_ENV_FILE)" ]; then printf '%s\n' 'ERROR: deploy/bpi-streaming/.env is required' >&2; exit 1; fi
	sh $(BPI_STREAM_DEPLOY_DIR)/scripts/capture-upgrade-savepoint.sh "$(BPI_STREAM_ENV_FILE)"

bpi-stream-restore-savepoint:
	@if [ ! -f "$(BPI_STREAM_ENV_FILE)" ]; then printf '%s\n' 'ERROR: deploy/bpi-streaming/.env is required' >&2; exit 1; fi
	sh $(BPI_STREAM_DEPLOY_DIR)/scripts/restore-from-savepoint.sh "$(BPI_STREAM_ENV_FILE)"

bpi-stream-verify-savepoint:
	@if [ ! -f "$(BPI_STREAM_ENV_FILE)" ]; then printf '%s\n' 'ERROR: deploy/bpi-streaming/.env is required' >&2; exit 1; fi
	sh $(BPI_STREAM_DEPLOY_DIR)/scripts/verify-savepoint-restore.sh "$(BPI_STREAM_ENV_FILE)"

bpi-rule-application-flink-acceptance:
	sh $(BPI_STREAM_DEPLOY_DIR)/scripts/run-rule-application-flink-acceptance.sh

bpi-production-context-test:
	$(MVN) -pl backend/source-modules/mes-production-context-outbox -am test

bpi-production-context-postgres-test:
	sh backend/source-modules/mes-production-context-outbox/scripts/test-postgres-outbox.sh

qcs-quality-gate-outbox-test:
	$(MVN) -pl backend/source-modules/qcs-quality-gate-outbox -am test

qcs-quality-gate-outbox-postgres-test:
	sh backend/source-modules/qcs-quality-gate-outbox/scripts/test-postgres-outbox.sh

qcs-quality-gate-outbox-package:
	$(MVN) -pl backend/source-modules/qcs-quality-gate-outbox -am -DskipTests package

down-bpi-stream:
	@if [ ! -f "$(BPI_STREAM_ENV_FILE)" ]; then printf '%s\n' 'ERROR: deploy/bpi-streaming/.env is required' >&2; exit 1; fi
	$(BPI_STREAM_COMPOSE) down

bpi-runtime-replay-test:
	$(MVN) -q -f acceptance/bpi-runtime/pom.xml test

bpi-adapter-static-check:
	$(PYTHON) scripts/verify-bpi-adapter.py

bpi-adapter-test:
	$(MVN) -pl backend/source-modules/batch-intelligence-adapter -am test

bpi-adapter-package:
	$(MVN) -pl backend/source-modules/batch-intelligence-adapter -am -DskipTests package

bpi-ui-static-check:
	$(PYTHON) scripts/verify-bpi-ui.py

bpi-ui-build:
	npm --prefix frontend/apps/bpi run build

bpi-ui-test:
	npm --prefix frontend/apps/bpi run test:e2e

bpi-feature-flag-governance-acceptance-check:
	$(PYTHON) scripts/verify-bpi-feature-flag-governance.py

bpi-shell-menu-gate-acceptance-check:
	$(PYTHON) scripts/verify-bpi-shell-menu-gate.py

up-bpi: bpi-ui-build
	$(COMPOSE) --profile bpi up -d --build bpi-service bpi-adapter bpi-wms-adapter nginx

source-module-check:
	$(PYTHON) scripts/verify-source-modules.py

module-intake-precheck-regression-check:
	$(PYTHON) scripts/verify-module-intake-precheck.py

module-intake-candidate-report-check:
	$(PYTHON) scripts/verify-module-intake-candidate-report.py

source-module-test:
	$(MVN) -q -pl backend/source-modules -am test

create-backend-module:
	@test -n "$(MODULE)" || { echo "MODULE is required, e.g. make create-backend-module MODULE=platform-auth"; exit 2; }
	$(PYTHON) scripts/create-backend-source-module.py "$(MODULE)" $(if $(PACKAGE),--package "$(PACKAGE)",)

material-wms-test:
	$(MVN) -pl backend/source-modules/material-wms -am test

material-wms-package:
	$(MVN) -pl backend/source-modules/material-wms -am package -DskipTests

material-wms-stage-runtime: material-wms-package
	mkdir -p runtime/bap-server/module-Server/material/manual
	cp backend/source-modules/material-wms/target/material-wms-0.1.0-SNAPSHOT.jar runtime/bap-server/module-Server/material/manual/material-wms.jar

acceptance-material-wms-persistence:
	$(NODE) deploy/docker/scripts/adp-material-wms-persistence-acceptance.js

process-analysis-test:
	$(MVN) -pl backend/source-modules/process-analysis -am test

process-analysis-package:
	$(MVN) -pl backend/source-modules/process-analysis -am package -DskipTests

process-analysis-stage-runtime: process-analysis-package
	mkdir -p runtime/bap-server/module-Server/ProcessAnalysis/manual
	cp backend/source-modules/process-analysis/target/process-analysis-0.1.0-SNAPSHOT.jar runtime/bap-server/module-Server/ProcessAnalysis/manual/process-analysis.jar

acceptance-process-analysis-persistence:
	$(NODE) deploy/docker/scripts/adp-process-analysis-persistence-acceptance.js

rm-formula-editor-test:
	$(MVN) -pl backend/source-modules/rm-formula-editor -am test

rm-formula-editor-package:
	$(MVN) -pl backend/source-modules/rm-formula-editor -am package -DskipTests

rm-formula-editor-stage-runtime: rm-formula-editor-package
	mkdir -p runtime/bap-server/module-Server/RMFormulaEditor/manual
	cp backend/source-modules/rm-formula-editor/target/rm-formula-editor-0.1.0-SNAPSHOT.jar runtime/bap-server/module-Server/RMFormulaEditor/manual/rm-formula-editor.jar

acceptance-rm-web-formula-editor-persistence:
	$(NODE) deploy/docker/scripts/adp-rm-web-formula-editor-persistence-acceptance.js

rm-web-formula-editor-acceptance-check:
	$(PYTHON) scripts/verify-rm-web-formula-editor-acceptance.py

wom-print-test:
	$(MVN) -pl backend/source-modules/wom-print -am test

wom-print-package:
	$(MVN) -pl backend/source-modules/wom-print -am package -DskipTests

wom-print-stage-runtime: wom-print-package
	mkdir -p runtime/bap-server/module-Server/WOMPrint/manual
	cp backend/source-modules/wom-print/target/wom-print-0.1.0-SNAPSHOT.jar runtime/bap-server/module-Server/WOMPrint/manual/wom-print.jar

wom-production-entry-test:
	$(MVN) -pl backend/source-modules/wom-production-entry -am test

wom-production-entry-package:
	$(MVN) -pl backend/source-modules/wom-production-entry -am package -DskipTests

wom-production-entry-stage-runtime: wom-production-entry-package
	mkdir -p runtime/bap-server/module-Server/WOMProductionEntry/manual
	cp backend/source-modules/wom-production-entry/target/wom-production-entry-0.1.0-SNAPSHOT.jar runtime/bap-server/module-Server/WOMProductionEntry/manual/wom-production-entry.jar

acceptance-wom-production-entry-persistence:
	$(NODE) deploy/docker/scripts/adp-wom-manual-task-entry-persistence-acceptance.js

wom-quality-reporting-test:
	$(MVN) -pl backend/source-modules/wom-quality-reporting -am test

wom-quality-reporting-package:
	$(MVN) -pl backend/source-modules/wom-quality-reporting -am package -DskipTests

wom-quality-reporting-stage-runtime: wom-quality-reporting-package
	mkdir -p runtime/bap-server/module-Server/WOMQualityReporting/manual
	cp backend/source-modules/wom-quality-reporting/target/wom-quality-reporting-0.1.0-SNAPSHOT.jar runtime/bap-server/module-Server/WOMQualityReporting/manual/wom-quality-reporting.jar

acceptance-wom-quality-quantity-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) $(NODE) deploy/docker/scripts/adp-wom-quality-quantity-persistence-acceptance.js

module-intake-check:
	@test -n "$(INTAKE)" || { echo "INTAKE is required, e.g. make module-intake-check INTAKE=/path/to/package-or-dir"; exit 2; }
	$(PYTHON) scripts/precheck-module-intake.py "$(INTAKE)" --report "$(INTAKE_REPORT)"
	@printf 'Module intake precheck report: %s\n' '$(INTAKE_REPORT)'

inventory:
	$(PYTHON) scripts/generate-current-content-inventory.py

inventory-check:
	$(PYTHON) scripts/generate-current-content-inventory.py --check

backend-dependency-inventory:
	$(PYTHON) scripts/generate-backend-dependency-inventory.py

backend-dependency-check:
	$(PYTHON) scripts/generate-backend-dependency-inventory.py --check

oracle-audit:
	$(PYTHON) scripts/generate-oracle-migration-audit.py

oracle-audit-check:
	$(PYTHON) scripts/generate-oracle-migration-audit.py --check

postgres-migration-index:
	$(PYTHON) scripts/generate-postgres-migration-inventory.py

postgres-migration-check:
	$(PYTHON) scripts/generate-postgres-migration-inventory.py --check

oracle-replacement-status:
	$(PYTHON) scripts/generate-oracle-replacement-status.py

oracle-replacement-check: render-config
	$(PYTHON) scripts/generate-oracle-replacement-status.py --check

production-source-inventory:
	ADP_PROD_MIGRATION_ENV=$(PROD_MIGRATION_ENV) sh deploy/database/production-migration/scripts/run-source-inventory.sh

production-target-preflight:
	ADP_PROD_MIGRATION_ENV=$(PROD_MIGRATION_ENV) sh deploy/database/production-migration/scripts/run-target-preflight.sh

production-rowcount-compare:
	$(PYTHON) deploy/database/production-migration/scripts/compare-row-counts.py --source "$(PROD_MIGRATION_SOURCE_COUNTS)" --target "$(PROD_MIGRATION_TARGET_COUNTS)" --output-dir "$(PROD_MIGRATION_REPORT_DIR)"

production-checksum-compare:
	$(PYTHON) deploy/database/production-migration/scripts/compare-checksums.py --source "$(PROD_MIGRATION_SOURCE_CHECKSUMS)" --target "$(PROD_MIGRATION_TARGET_CHECKSUMS)" --output-dir "$(PROD_MIGRATION_REPORT_DIR)"

production-db-migration-evidence-check:
	$(PYTHON) deploy/database/production-migration/scripts/validate-migration-evidence.py --evidence "$(DB_MIGRATION_EVIDENCE)"

production-db-migration-ready-check:
	$(PYTHON) deploy/database/production-migration/scripts/validate-migration-evidence.py --evidence "$(DB_MIGRATION_EVIDENCE)" --strict-ready

production-minio-source-inventory:
	ADP_MINIO_MIGRATION_ENV=$(MINIO_MIGRATION_ENV) ADP_MINIO_INVENTORY_ROLE=source ADP_MINIO_REPORT_DIR=$(MINIO_MIGRATION_REPORT_DIR) sh deploy/minio/production-migration/scripts/run-bucket-inventory.sh

production-minio-target-inventory:
	ADP_MINIO_MIGRATION_ENV=$(MINIO_MIGRATION_ENV) ADP_MINIO_INVENTORY_ROLE=target ADP_MINIO_REPORT_DIR=$(MINIO_MIGRATION_REPORT_DIR) sh deploy/minio/production-migration/scripts/run-bucket-inventory.sh

production-minio-compare:
	$(PYTHON) deploy/minio/production-migration/scripts/compare-bucket-inventory.py --source "$(MINIO_SOURCE_INVENTORY)" --target "$(MINIO_TARGET_INVENTORY)" --output-dir "$(MINIO_MIGRATION_REPORT_DIR)"

production-minio-migration-evidence-check:
	$(PYTHON) deploy/minio/production-migration/scripts/validate-minio-migration-evidence.py --evidence "$(MINIO_MIGRATION_EVIDENCE)"

production-minio-migration-ready-check:
	$(PYTHON) deploy/minio/production-migration/scripts/validate-minio-migration-evidence.py --evidence "$(MINIO_MIGRATION_EVIDENCE)" --strict-ready

production-keycloak-source-export:
	ADP_KEYCLOAK_MIGRATION_ENV=$(KEYCLOAK_MIGRATION_ENV) ADP_KEYCLOAK_INVENTORY_ROLE=source ADP_KEYCLOAK_REPORT_DIR=$(KEYCLOAK_MIGRATION_REPORT_DIR) sh deploy/keycloak/production-migration/scripts/export-realm-inventory.sh

production-keycloak-target-export:
	ADP_KEYCLOAK_MIGRATION_ENV=$(KEYCLOAK_MIGRATION_ENV) ADP_KEYCLOAK_INVENTORY_ROLE=target ADP_KEYCLOAK_REPORT_DIR=$(KEYCLOAK_MIGRATION_REPORT_DIR) sh deploy/keycloak/production-migration/scripts/export-realm-inventory.sh

production-keycloak-compare:
	$(PYTHON) deploy/keycloak/production-migration/scripts/compare-realm-inventory.py --source "$(KEYCLOAK_SOURCE_INVENTORY)" --target "$(KEYCLOAK_TARGET_INVENTORY)" --output-dir "$(KEYCLOAK_MIGRATION_REPORT_DIR)"

production-keycloak-migration-evidence-check:
	$(PYTHON) deploy/keycloak/production-migration/scripts/validate-keycloak-migration-evidence.py --evidence "$(KEYCLOAK_MIGRATION_EVIDENCE)"

production-keycloak-migration-ready-check:
	$(PYTHON) deploy/keycloak/production-migration/scripts/validate-keycloak-migration-evidence.py --evidence "$(KEYCLOAK_MIGRATION_EVIDENCE)" --strict-ready

production-rollback-evidence-check:
	$(PYTHON) deploy/rollback/production-migration/scripts/validate-rollback-evidence.py --evidence "$(ROLLBACK_EVIDENCE)"

production-rollback-ready-check:
	$(PYTHON) deploy/rollback/production-migration/scripts/validate-rollback-evidence.py --evidence "$(ROLLBACK_EVIDENCE)" --strict-ready

production-license-strategy-check:
	$(PYTHON) deploy/license/production-migration/scripts/validate-license-decision.py --decision "$(LICENSE_DECISION)"

production-license-ready-check:
	$(PYTHON) deploy/license/production-migration/scripts/validate-license-decision.py --decision "$(LICENSE_DECISION)" --strict-ready

production-network-tls-check:
	$(PYTHON) deploy/network/production-migration/scripts/validate-network-tls-plan.py --plan "$(NETWORK_TLS_PLAN)"

production-network-tls-ready-check:
	$(PYTHON) deploy/network/production-migration/scripts/validate-network-tls-plan.py --plan "$(NETWORK_TLS_PLAN)" --strict-ready

production-security-hardening-check:
	$(PYTHON) deploy/security/production-migration/scripts/validate-security-hardening-plan.py --plan "$(SECURITY_HARDENING_PLAN)"

production-security-hardening-ready-check:
	$(PYTHON) deploy/security/production-migration/scripts/validate-security-hardening-plan.py --plan "$(SECURITY_HARDENING_PLAN)" --strict-ready

production-business-smoke-signoff-check:
	$(PYTHON) deploy/business-smoke/production-migration/scripts/validate-business-smoke-signoff.py --signoff "$(BUSINESS_SMOKE_SIGNOFF)"

production-business-smoke-signoff-ready-check:
	$(PYTHON) deploy/business-smoke/production-migration/scripts/validate-business-smoke-signoff.py --signoff "$(BUSINESS_SMOKE_SIGNOFF)" --strict-ready

production-nacos-runtime-patch-check:
	$(PYTHON) deploy/nacos/production-migration/scripts/validate-nacos-runtime-patch-evidence.py --evidence "$(NACOS_RUNTIME_PATCH_EVIDENCE)"

production-nacos-runtime-patch-ready-check:
	$(PYTHON) deploy/nacos/production-migration/scripts/validate-nacos-runtime-patch-evidence.py --evidence "$(NACOS_RUNTIME_PATCH_EVIDENCE)" --strict-ready

production-evidence-ready-gate-regression-check:
	$(PYTHON) scripts/verify-production-evidence-ready-gates.py

render-config:
	cd $(DEPLOY_DIR) && $(PYTHON) scripts/render-nacos-configs.py

prepare-runtime:
	cd $(DEPLOY_DIR) && scripts/prepare-static-placeholders.sh
	cd $(DEPLOY_DIR) && scripts/prepare-qcs-static-assets.sh
	cd $(DEPLOY_DIR) && scripts/prepare-eam-static-assets.sh
	cd $(DEPLOY_DIR) && scripts/prepare-runtime-patches.sh ../../runtime/bap-server

up-infra:
	$(COMPOSE) up -d postgres redis mongo zookeeper kafka nacos keycloak minio

up: render-config
	$(COMPOSE) up -d

down:
	$(COMPOSE) down

ps:
	$(COMPOSE) ps

logs:
	$(COMPOSE) logs --tail=200 -f $(SERVICE)

smoke-platform:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_PLATFORM_OUTPUT_DIR=$(PLATFORM_SMOKE_OUTPUT) ADP_PLATFORM_MENU_LIMIT=$(PLATFORM_MENU_LIMIT) ADP_PLATFORM_SECTION_TIMEOUT_MS=$(ADP_PLATFORM_SECTION_TIMEOUT_MS) ADP_PAGE_TIMEOUT_MS=$(ADP_PAGE_TIMEOUT_MS) ADP_API_TIMEOUT_MS=$(ADP_API_TIMEOUT_MS) ADP_ORG_VISIBLE_TIMEOUT_MS=$(ADP_ORG_VISIBLE_TIMEOUT_MS) $(NODE) deploy/docker/scripts/adp-platform-validation-smoke.js

smoke-api:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) $(NODE) deploy/docker/scripts/adp-platform-api-smoke.js

smoke-menu:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) $(NODE) deploy/docker/scripts/adp-menu-smoke.js

smoke-todo:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) $(NODE) deploy/docker/scripts/adp-home-todo-smoke.js

smoke-organization:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_ORG_VISIBLE_TIMEOUT_MS=$(ADP_ORG_VISIBLE_TIMEOUT_MS) $(NODE) deploy/docker/scripts/adp-organization-smoke.js

smoke-test-environment:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_SSH_HOST=$(ADP_SSH_HOST) ADP_SSH_USER=$(ADP_SSH_USER) ADP_SSH_CONNECT_TIMEOUT=$(ADP_SSH_CONNECT_TIMEOUT) ADP_TEST_ENVIRONMENT_SMOKE_OUTPUT=$(TEST_ENVIRONMENT_SMOKE_OUTPUT) $(NODE) deploy/docker/scripts/adp-test-environment-smoke.js

smoke-postgres-runtime:
	ADP_SSH_HOST=$(ADP_SSH_HOST) ADP_SSH_USER=$(ADP_SSH_USER) ADP_SSH_CONNECT_TIMEOUT=$(ADP_SSH_CONNECT_TIMEOUT) ADP_POSTGRES_RUNTIME_SMOKE_OUTPUT=$(POSTGRES_RUNTIME_SMOKE_OUTPUT) $(NODE) deploy/docker/scripts/adp-postgres-runtime-smoke.js

smoke-nacos-config:
	ADP_SSH_HOST=$(ADP_SSH_HOST) ADP_SSH_USER=$(ADP_SSH_USER) ADP_SSH_CONNECT_TIMEOUT=$(ADP_SSH_CONNECT_TIMEOUT) ADP_NACOS_CONFIG_SMOKE_OUTPUT=$(NACOS_CONFIG_SMOKE_OUTPUT) $(NODE) deploy/docker/scripts/adp-nacos-config-drift-smoke.js

smoke-keycloak-jwt:
	ADP_SSH_HOST=$(ADP_SSH_HOST) ADP_SSH_USER=$(ADP_SSH_USER) ADP_SSH_CONNECT_TIMEOUT=$(ADP_SSH_CONNECT_TIMEOUT) ADP_KEYCLOAK_JWT_SMOKE_OUTPUT=$(KEYCLOAK_JWT_SMOKE_OUTPUT) $(NODE) deploy/docker/scripts/adp-keycloak-jwt-runtime-smoke.js

smoke-minio-runtime:
	ADP_SSH_HOST=$(ADP_SSH_HOST) ADP_SSH_USER=$(ADP_SSH_USER) ADP_SSH_CONNECT_TIMEOUT=$(ADP_SSH_CONNECT_TIMEOUT) ADP_MINIO_RUNTIME_SMOKE_OUTPUT=$(MINIO_RUNTIME_SMOKE_OUTPUT) $(NODE) deploy/docker/scripts/adp-minio-runtime-smoke.js

smoke-business-dependencies:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_SSH_HOST=$(ADP_SSH_HOST) ADP_SSH_USER=$(ADP_SSH_USER) ADP_SSH_CONNECT_TIMEOUT=$(ADP_SSH_CONNECT_TIMEOUT) ADP_BUSINESS_DEPENDENCY_SMOKE_OUTPUT=$(BUSINESS_DEPENDENCY_SMOKE_OUTPUT) $(NODE) deploy/docker/scripts/adp-business-dependency-readiness-smoke.js

business-package-scan:
	$(PYTHON) scripts/scan-business-dependency-packages.py --roots "$(BUSINESS_PACKAGE_SCAN_ROOTS)" --output "$(BUSINESS_PACKAGE_SCAN_OUTPUT)" --nested-depth "$(BUSINESS_PACKAGE_SCAN_NESTED_DEPTH)"

smoke-production-export-readiness:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_PAGE_TIMEOUT_MS=$(ADP_PAGE_TIMEOUT_MS) ADP_API_TIMEOUT_MS=$(ADP_API_TIMEOUT_MS) ADP_PRODUCTION_EXPORT_SMOKE_OUTPUT=$(PRODUCTION_EXPORT_SMOKE_OUTPUT) $(NODE) deploy/docker/scripts/adp-production-export-readiness-smoke.js

acceptance-organization-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_ORGANIZATION_PERSISTENCE_OUTPUT=$(ORGANIZATION_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-organization-persistence-acceptance.js

acceptance-organization-group-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_ORGANIZATION_GROUP_PERSISTENCE_OUTPUT=$(ORGANIZATION_GROUP_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-organization-group-persistence-acceptance.js

acceptance-organization-position-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_ORGANIZATION_POSITION_PERSISTENCE_OUTPUT=$(ORGANIZATION_POSITION_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-organization-position-persistence-acceptance.js

acceptance-organization-position-role-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_ORGANIZATION_POSITION_ROLE_PERSISTENCE_OUTPUT=$(ORGANIZATION_POSITION_ROLE_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-organization-position-role-persistence-acceptance.js

acceptance-organization-company-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_ORGANIZATION_COMPANY_PERSISTENCE_OUTPUT=$(ORGANIZATION_COMPANY_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-organization-company-persistence-acceptance.js

acceptance-organization-person-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_ORGANIZATION_PERSON_PERSISTENCE_OUTPUT=$(ORGANIZATION_PERSON_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-organization-person-persistence-acceptance.js

acceptance-organization-person-user-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_ORGANIZATION_PERSON_USER_PERSISTENCE_OUTPUT=$(ORGANIZATION_PERSON_USER_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-organization-person-user-persistence-acceptance.js

acceptance-auth-user-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_AUTH_USER_PERSISTENCE_OUTPUT=$(AUTH_USER_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-auth-user-persistence-acceptance.js

acceptance-rbac-permission-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_RBAC_PERMISSION_PERSISTENCE_OUTPUT=$(RBAC_PERMISSION_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-rbac-permission-persistence-acceptance.js

acceptance-systemcode-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_SYSTEMCODE_PERSISTENCE_OUTPUT=$(SYSTEMCODE_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-systemcode-persistence-acceptance.js

acceptance-systemconfig-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_SYSTEMCONFIG_PERSISTENCE_OUTPUT=$(SYSTEMCONFIG_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-systemconfig-persistence-acceptance.js

.PHONY: acceptance-patrol-task-persistence acceptance-patrol-execution-persistence acceptance-patrol-hidden-danger-persistence acceptance-patrol-report acceptance-patrol-gather-data
acceptance-patrol-task-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_PAGE_TIMEOUT_MS=$(ADP_PAGE_TIMEOUT_MS) ADP_PATROL_PERSISTENCE_OUTPUT=$(PATROL_TASK_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-patrol-task-persistence-acceptance.js

acceptance-patrol-execution-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_PAGE_TIMEOUT_MS=$(ADP_PAGE_TIMEOUT_MS) ADP_PATROL_TASK_ACTION=complete ADP_PATROL_PERSISTENCE_OUTPUT=$(PATROL_EXECUTION_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-patrol-task-persistence-acceptance.js

acceptance-patrol-hidden-danger-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_PAGE_TIMEOUT_MS=$(ADP_PAGE_TIMEOUT_MS) ADP_PATROL_TASK_ACTION=hidden-danger ADP_PATROL_PERSISTENCE_OUTPUT=$(PATROL_HIDDEN_DANGER_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-patrol-task-persistence-acceptance.js

acceptance-patrol-report:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_PAGE_TIMEOUT_MS=$(ADP_PAGE_TIMEOUT_MS) ADP_PATROL_REPORT_OUTPUT=$(PATROL_REPORT_ACCEPTANCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-patrol-report-smoke.js

acceptance-patrol-gather-data:
	ADP_USERNAME=$(ADP_USERNAME) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_PATROL_GATHER_OUTPUT=$(PATROL_GATHER_ACCEPTANCE_OUTPUT) ADP_EXPECTED_EAM_SHA256=$(PATROL_GATHER_EXPECTED_EAM_SHA256) $(NODE) deploy/docker/scripts/adp-patrol-gather-data-runtime-acceptance.js

acceptance-patrol-input-standard-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_PAGE_TIMEOUT_MS=$(ADP_PAGE_TIMEOUT_MS) ADP_PATROL_INPUT_STANDARD_PERSISTENCE_OUTPUT=$(PATROL_INPUT_STANDARD_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-patrol-input-standard-persistence-acceptance.js

acceptance-patrol-route-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_PAGE_TIMEOUT_MS=$(ADP_PAGE_TIMEOUT_MS) ADP_PATROL_ROUTE_PERSISTENCE_OUTPUT=$(PATROL_ROUTE_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-patrol-route-persistence-acceptance.js

acceptance-patrol-area-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_PAGE_TIMEOUT_MS=$(ADP_PAGE_TIMEOUT_MS) ADP_PATROL_AREA_PERSISTENCE_OUTPUT=$(PATROL_AREA_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-patrol-area-persistence-acceptance.js

acceptance-patrol-item-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_PAGE_TIMEOUT_MS=$(ADP_PAGE_TIMEOUT_MS) ADP_PATROL_ITEM_PERSISTENCE_OUTPUT=$(PATROL_ITEM_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-patrol-item-persistence-acceptance.js

smoke-systemconfig-builtins:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_SYSTEMCONFIG_BUILTINS_OUTPUT=$(SYSTEMCONFIG_BUILTINS_OUTPUT) $(NODE) deploy/docker/scripts/adp-systemconfig-builtins-readiness-smoke.js

acceptance-systemconfig-controlled-runtime-config:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_SYSTEMCONFIG_CONTROLLED_OUTPUT=$(SYSTEMCONFIG_CONTROLLED_OUTPUT) ADP_SYSTEMCONFIG_CONTROLLED_TARGET_MODE=$(SYSTEMCONFIG_CONTROLLED_TARGET_MODE) $(NODE) deploy/docker/scripts/adp-systemconfig-controlled-runtime-config-acceptance.js

smoke-runtime-configuration:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_RUNTIME_CONFIG_SMOKE_OUTPUT=$(RUNTIME_CONFIG_SMOKE_OUTPUT) $(NODE) deploy/docker/scripts/adp-runtime-configuration-readiness-smoke.js

smoke-entity-model-config-crud-readiness:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ENTITY_MODEL_CONFIG_CRUD_READINESS_OUTPUT=$(ENTITY_MODEL_CONFIG_CRUD_READINESS_OUTPUT) $(NODE) deploy/docker/scripts/adp-entity-model-config-crud-readiness-probe.js

acceptance-entity-model-field-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_ENTITY_MODEL_FIELD_ACCEPTANCE_OUTPUT=$(ENTITY_MODEL_FIELD_ACCEPTANCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-entity-model-field-persistence-acceptance.js

acceptance-entity-model-field-type-matrix:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_ENTITY_MODEL_FIELD_TYPE_MATRIX_OUTPUT=$(ENTITY_MODEL_FIELD_TYPE_MATRIX_OUTPUT) $(NODE) deploy/docker/scripts/adp-entity-model-field-type-matrix-acceptance.js

acceptance-entity-model-object-association:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_ENTITY_MODEL_OBJECT_ASSOCIATION_OUTPUT=$(ENTITY_MODEL_OBJECT_ASSOCIATION_OUTPUT) $(NODE) deploy/docker/scripts/adp-entity-model-object-association-acceptance.js

acceptance-entity-model-field-delete-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_ENTITY_MODEL_FIELD_DELETE_OUTPUT=$(ENTITY_MODEL_FIELD_DELETE_OUTPUT) $(NODE) deploy/docker/scripts/adp-entity-model-field-delete-persistence-acceptance.js

acceptance-custom-property-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_CUSTOM_PROPERTY_ACCEPTANCE_OUTPUT=$(CUSTOM_PROPERTY_ACCEPTANCE_OUTPUT) ADP_API_TIMEOUT_MS=$(ADP_API_TIMEOUT_MS) $(NODE) deploy/docker/scripts/adp-custom-property-persistence-acceptance.js

rehearse-core-flow-runtime-rollback:
	ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_REMOTE_DEPLOY_ROOT=$(CORE_FLOW_REMOTE_ROOT) ADP_CORE_FLOW_BACKUP_TAG=$(CORE_FLOW_BACKUP_TAG) ADP_CORE_FLOW_ROLLBACK_OUTPUT=$(CORE_FLOW_RUNTIME_ROLLBACK_OUTPUT) $(NODE) deploy/docker/scripts/adp-core-flow-runtime-rollback-rehearsal.js

bpi-runtime-image-rollback-rehearsal:
	@ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) \
	ADP_REMOTE_DEPLOY_ROOT=$(CORE_FLOW_REMOTE_ROOT) \
	ADP_BASE_URL=$(ADP_BASE_URL) BPI_BROWSER_BASE_URL=$(ADP_BASE_URL)/bpi \
	ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) \
	BPI_ROLLBACK_SERVICE_IMAGE=$(BPI_ROLLBACK_SERVICE_IMAGE) \
	BPI_ROLLBACK_ADAPTER_IMAGE=$(BPI_ROLLBACK_ADAPTER_IMAGE) \
	BPI_RUNTIME_ROLLBACK_OUTPUT=$(BPI_RUNTIME_ROLLBACK_OUTPUT) \
	$(NODE) deploy/docker/scripts/adp-bpi-runtime-image-rollback-rehearsal.js

bpi-integrated-rollback-rehearsal:
	@ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) \
	ADP_REMOTE_DEPLOY_ROOT=$(CORE_FLOW_REMOTE_ROOT) \
	BPI_STREAM_REMOTE_ROOT=$(BPI_STREAM_REMOTE_ROOT) \
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) \
	BPI_ROLLBACK_SERVICE_IMAGE=$(BPI_ROLLBACK_SERVICE_IMAGE) \
	BPI_ROLLBACK_ADAPTER_IMAGE=$(BPI_ROLLBACK_ADAPTER_IMAGE) \
	BPI_ROLLBACK_JOB_JAR=$(BPI_ROLLBACK_JOB_JAR) \
	BPI_LOAD_CLIENT_JOB_JAR=$(BPI_LOAD_CLIENT_JOB_JAR) \
	BPI_ACCEPTANCE_TENANT_ID=$(BPI_ACCEPTANCE_TENANT_ID) \
	BPI_ACCEPTANCE_PLANT_ID=$(BPI_ACCEPTANCE_PLANT_ID) \
	BPI_ACCEPTANCE_LINE_ID=$(BPI_ACCEPTANCE_LINE_ID) \
	BPI_INTEGRATED_ROLLBACK_OUTPUT=$(BPI_INTEGRATED_ROLLBACK_OUTPUT) \
	BPI_INTEGRATED_ROLLBACK_CONFIRM=ROLLBACK_BPI_SERVICE_ADAPTER_FLINK_AND_RESTORE \
	$(NODE) deploy/docker/scripts/adp-bpi-integrated-rollback-rehearsal.js

acceptance-wom-manufacturing-order-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_OUTPUT_PATH=$(WOM_MANUFACTURING_ORDER_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-manufacturing-order-persistence-acceptance.js

acceptance-factory-line-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_PAGE_TIMEOUT_MS=$(ADP_PAGE_TIMEOUT_MS) ADP_FACTORY_LINE_PERSISTENCE_OUTPUT=$(FACTORY_LINE_PERSISTENCE_OUTPUT) NODE_PATH="$(CURDIR)/node_modules" $(NODE) deploy/docker/scripts/adp-factory-line-persistence-acceptance.js

acceptance-wom-start-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_WOM_KEEP_FIXTURE=$(ADP_WOM_KEEP_FIXTURE) ADP_WOM_START_PERSISTENCE_OUTPUT=$(WOM_START_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-start-persistence-acceptance.js

acceptance-wom-hold-restart-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_WOM_KEEP_FIXTURE=$(ADP_WOM_KEEP_FIXTURE) ADP_WOM_TRANSITIONS=start,hold,restart ADP_WOM_STATE_PERSISTENCE_OUTPUT=$(WOM_HOLD_RESTART_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-start-persistence-acceptance.js

smoke-wom-toolbar-row:
	$(MAKE) acceptance-wom-hold-restart-persistence WOM_HOLD_RESTART_PERSISTENCE_OUTPUT=$(WOM_TOOLBAR_ROW_SMOKE_SEED_OUTPUT) ADP_PAGE_TIMEOUT_MS=$(WOM_TOOLBAR_PAGE_TIMEOUT_MS) ADP_GRID_TIMEOUT_MS=$(WOM_TOOLBAR_PAGE_TIMEOUT_MS) ADP_WOM_KEEP_FIXTURE=true
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_PAGE_TIMEOUT_MS=$(WOM_TOOLBAR_PAGE_TIMEOUT_MS) ADP_GRID_TIMEOUT_MS=$(WOM_TOOLBAR_PAGE_TIMEOUT_MS) ADP_WOM_TOOLBAR_SEED_EVIDENCE=$(WOM_TOOLBAR_ROW_SMOKE_SEED_OUTPUT) ADP_WOM_TOOLBAR_ROW_SMOKE_OUTPUT=$(WOM_TOOLBAR_ROW_SMOKE_OUTPUT) ADP_WOM_TOOLBAR_SCREENSHOT=$(WOM_TOOLBAR_ROW_SMOKE_SCREENSHOT) $(NODE) deploy/docker/scripts/adp-wom-toolbar-row-smoke.js

acceptance-wom-process-execution-actions:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_PAGE_TIMEOUT_MS=$(ADP_PAGE_TIMEOUT_MS) ADP_WOM_PROCESS_ACTIONS_OUTPUT=$(WOM_PROCESS_ACTIONS_OUTPUT) ADP_WOM_PROCESS_ACTIONS_SCREENSHOT_DIR=$(WOM_PROCESS_ACTIONS_SCREENSHOT_DIR) $(NODE) deploy/docker/scripts/adp-wom-process-execution-actions-acceptance.js

acceptance-wom-stop-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_WOM_TRANSITIONS=start,stop ADP_WOM_STATE_PERSISTENCE_OUTPUT=$(WOM_STOP_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-start-persistence-acceptance.js

acceptance-wom-stop-output-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_WOM_TRANSITIONS=start,stop-output ADP_WOM_STATE_PERSISTENCE_OUTPUT=$(WOM_STOP_OUTPUT_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-start-persistence-acceptance.js

acceptance-wom-advance-release-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_WOM_TRANSITIONS=advance-release ADP_WOM_STATE_PERSISTENCE_OUTPUT=$(WOM_ADVANCE_RELEASE_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-start-persistence-acceptance.js

acceptance-wom-prepare-need-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_PREPARE_NEED_PERSISTENCE_OUTPUT=$(WOM_PREPARE_NEED_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-prepare-need-persistence-acceptance.js

acceptance-wom-active-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_ACTIVE_PERSISTENCE_OUTPUT=$(WOM_ACTIVE_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-active-persistence-acceptance.js

acceptance-wom-active-end-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_ACTIVE_ACTION=end ADP_WOM_ACTIVE_PERSISTENCE_OUTPUT=$(WOM_ACTIVE_END_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-active-persistence-acceptance.js

acceptance-wom-easy-active-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_ACTIVE_ACTION=easy-end ADP_WOM_ACTIVE_PERSISTENCE_OUTPUT=$(WOM_EASY_ACTIVE_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-active-persistence-acceptance.js

acceptance-wom-putin-active-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_ACTIVE_ACTION=putin-end ADP_WOM_ACTIVE_PERSISTENCE_OUTPUT=$(WOM_PUTIN_ACTIVE_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-active-persistence-acceptance.js

acceptance-wom-check-active-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_ACTIVE_ACTION=check-end ADP_WOM_ACTIVE_PERSISTENCE_OUTPUT=$(WOM_CHECK_ACTIVE_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-active-persistence-acceptance.js

acceptance-wom-process-start-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_ACTIVE_ACTION=process-start ADP_WOM_ACTIVE_PERSISTENCE_OUTPUT=$(WOM_PROCESS_START_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-active-persistence-acceptance.js

acceptance-wom-process-end-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_ACTIVE_ACTION=process-end ADP_WOM_ACTIVE_PERSISTENCE_OUTPUT=$(WOM_PROCESS_END_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-active-persistence-acceptance.js

acceptance-wom-process-unit-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_ACTIVE_ACTION=process-unit ADP_WOM_ACTIVE_PERSISTENCE_OUTPUT=$(WOM_PROCESS_UNIT_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-active-persistence-acceptance.js

acceptance-wom-manu-inspect-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_MANU_INSPECT_PERSISTENCE_OUTPUT=$(WOM_MANU_INSPECT_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-manu-inspect-persistence-acceptance.js

acceptance-wom-checkoutbill-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_CHECKOUTBILL_PERSISTENCE_OUTPUT=$(WOM_CHECKOUTBILL_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-checkoutbill-persistence-acceptance.js

acceptance-wom-reject-material-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_WOM_REJECT_MATERIAL_PERSISTENCE_OUTPUT=$(WOM_REJECT_MATERIAL_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-reject-material-persistence-acceptance.js

acceptance-wom-public-produce-task-created-retirement:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_WOM_PUBLIC_PRODUCE_TASK_CREATED_RETIREMENT_OUTPUT=$(WOM_PUBLIC_PRODUCE_TASK_CREATED_RETIREMENT_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-public-produce-task-created-noop-probe.js

probe-wom-public-produce-task-created-noop:
	$(MAKE) acceptance-wom-public-produce-task-created-retirement WOM_PUBLIC_PRODUCE_TASK_CREATED_RETIREMENT_OUTPUT=$(WOM_PUBLIC_PRODUCE_TASK_CREATED_NOOP_OUTPUT)

probe-wom-qrcode-route:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_QRCODE_ROUTE_PROBE_OUTPUT=$(WOM_QRCODE_ROUTE_PROBE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-qrcode-route-probe.js

acceptance-wom-qrcode-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_WOM_QRCODE_ROUTE_PROBE_OUTPUT=$(WOM_QRCODE_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-qrcode-route-probe.js

acceptance-wom-qrcode-browser:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_WOM_QRCODE_BROWSER_OUTPUT=$(WOM_QRCODE_BROWSER_OUTPUT) ADP_WOM_QRCODE_BROWSER_SCREENSHOT=$(WOM_QRCODE_BROWSER_SCREENSHOT) $(NODE) deploy/docker/scripts/adp-wom-qrcode-browser-acceptance.js

acceptance-qcs-report-chain-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_DB_SSH_TARGET=$(ADP_SSH_USER)@$(ADP_SSH_HOST) ADP_QCS_REPORT_CHAIN_MODE=$(QCS_REPORT_CHAIN_MODE) ADP_QCS_REPORT_CHAIN_PERSISTENCE_OUTPUT=$(QCS_REPORT_CHAIN_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-qcs-report-chain-persistence-acceptance.js

acceptance-mes-full-production-flow:
	@set -eu; \
	confirm="$${ADP_MES_FULL_FLOW_CONFIRM:-$(MES_FULL_FLOW_CONFIRM)}"; \
	if [ "$$confirm" != "YES" ]; then \
		echo "set MES_FULL_FLOW_CONFIRM=YES (or ADP_MES_FULL_FLOW_CONFIRM=YES) before running live MES acceptance" >&2; \
		exit 2; \
	fi; \
	ADP_BASE_URL="$${ADP_BASE_URL:-$(ADP_BASE_URL)}" \
	ADP_BROWSER_BASE_URL="$${ADP_BROWSER_BASE_URL:-$(ADP_BROWSER_BASE_URL)}" \
	ADP_USERNAME="$${ADP_USERNAME:-$(ADP_USERNAME)}" \
	ADP_PASSWORD="$${ADP_PASSWORD:-$(ADP_PASSWORD)}" \
	ADP_DB_SSH_TARGET="$${ADP_DB_SSH_TARGET:-$(ADP_SSH_USER)@$(ADP_SSH_HOST)}" \
	ADP_MES_FULL_FLOW_CONFIRM="YES" \
	ADP_MES_FULL_FLOW_OUTPUT="$${ADP_MES_FULL_FLOW_OUTPUT:-$(MES_FULL_FLOW_OUTPUT)}" \
	ADP_MES_FULL_FLOW_BPI_BATCH_ID="$${ADP_MES_FULL_FLOW_BPI_BATCH_ID:-$(MES_FULL_FLOW_BPI_BATCH_ID)}" \
	ADP_MES_FULL_FLOW_TASK_ID="$${ADP_MES_FULL_FLOW_TASK_ID:-$(MES_FULL_FLOW_TASK_ID)}" \
	$(NODE) deploy/docker/scripts/adp-mes-full-production-flow-acceptance.js

acceptance-fructose-line-pilot:
	@set -eu; \
	confirm="$${ADP_FRUCTOSE_PILOT_CONFIRM:-$(FRUCTOSE_PILOT_CONFIRM)}"; \
	if [ "$$confirm" != "YES" ]; then \
		echo "set FRUCTOSE_PILOT_CONFIRM=YES (or ADP_FRUCTOSE_PILOT_CONFIRM=YES) before running retained fructose acceptance" >&2; \
		exit 2; \
	fi; \
	ADP_BASE_URL="$${ADP_BASE_URL:-$(ADP_BASE_URL)}" \
	ADP_BROWSER_BASE_URL="$${ADP_BROWSER_BASE_URL:-$(ADP_BROWSER_BASE_URL)}" \
	ADP_USERNAME="$${ADP_USERNAME:-$(ADP_USERNAME)}" \
	ADP_PASSWORD="$${ADP_PASSWORD:-$(ADP_PASSWORD)}" \
	ADP_DB_SSH_TARGET="$${ADP_DB_SSH_TARGET:-$(ADP_SSH_USER)@$(ADP_SSH_HOST)}" \
	ADP_FRUCTOSE_PILOT_CONFIRM="YES" \
	ADP_FRUCTOSE_FULL_FLOW_OUTPUT="$${ADP_FRUCTOSE_FULL_FLOW_OUTPUT:-$(FRUCTOSE_FULL_FLOW_OUTPUT)}" \
	ADP_FRUCTOSE_PILOT_OUTPUT="$${ADP_FRUCTOSE_PILOT_OUTPUT:-$(FRUCTOSE_PILOT_OUTPUT)}" \
	ADP_FRUCTOSE_PILOT_SCREENSHOT_DIR="$${ADP_FRUCTOSE_PILOT_SCREENSHOT_DIR:-$(FRUCTOSE_PILOT_SCREENSHOT_DIR)}" \
	$(NODE) deploy/docker/scripts/adp-fructose-line-pilot-acceptance.js

acceptance-teaminfo-scheduleplan-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_TEAMINFO_SCHEDULEPLAN_PERSISTENCE_OUTPUT=$(TEAMINFO_SCHEDULEPLAN_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-teaminfo-scheduleplan-persistence-acceptance.js

acceptance-craftgraph-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_CRAFTGRAPH_PERSISTENCE_OUTPUT=$(CRAFTGRAPH_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-craftgraph-persistence-acceptance.js

smoke-rbac-authority:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) $(NODE) deploy/docker/scripts/adp-rbac-authority-smoke.js

smoke-business:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) $(NODE) deploy/docker/scripts/adp-business-module-smoke.js

smoke-business-page:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_OUTPUT_DIR=$(BUSINESS_PAGE_SMOKE_OUTPUT) $(NODE) deploy/docker/scripts/adp-business-page-smoke.js

discover-production-actions:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_PRODUCTION_DISCOVERY_OUTPUT=$(PRODUCTION_DISCOVERY_OUTPUT) ADP_PRODUCTION_DISCOVERY_TARGETS='$(PRODUCTION_DISCOVERY_TARGETS)' ADP_PRODUCTION_DISCOVERY_CLICK_CREATE=$(PRODUCTION_DISCOVERY_CLICK_CREATE) $(NODE) deploy/docker/scripts/adp-production-action-discovery.js

audit-postgres-mappings:
	$(PYTHON) deploy/docker/scripts/audit-postgres-mappings.py backend/modules deploy/docker/postgres/init

audit-postgres-report:
	-$(PYTHON) deploy/docker/scripts/audit-postgres-mappings.py backend/modules deploy/docker/postgres/init --report $(POSTGRES_AUDIT_REPORT)
	@printf 'PostgreSQL audit report: %s\n' '$(POSTGRES_AUDIT_REPORT)'
