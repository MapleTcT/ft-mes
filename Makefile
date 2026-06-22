SHELL := /bin/sh

MVN ?= mvn
PYTHON ?= python3
NODE ?= node

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
BUSINESS_PACKAGE_SCAN_ROOTS ?= /Users/zhangchu/Documents/MES包:/Users/zhangchu/Downloads/ADP/bap-server/base-Server:/Users/zhangchu/Downloads/ADP/bap-server/config:/Users/zhangchu/Downloads/ADP/Temp/static/custom
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
SYSTEMCONFIG_BUILTINS_OUTPUT ?= metadata/systemconfig-builtins-readiness-smoke.json
SYSTEMCONFIG_CONTROLLED_OUTPUT ?= metadata/systemconfig-controlled-runtime-config-acceptance.json
SYSTEMCONFIG_CONTROLLED_TARGET_MODE ?= qcs
RUNTIME_CONFIG_SMOKE_OUTPUT ?= metadata/runtime-configuration-readiness-smoke.json
CUSTOM_PROPERTY_ACCEPTANCE_OUTPUT ?= metadata/custom-property-persistence-acceptance.json
ENTITY_MODEL_CONFIG_CRUD_READINESS_OUTPUT ?= metadata/entity-model-config-crud-readiness-probe.json
WOM_START_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-start-persistence-acceptance.json
WOM_HOLD_RESTART_PERSISTENCE_OUTPUT ?= /tmp/adp-wom-hold-restart-persistence-acceptance.json
WOM_TOOLBAR_ROW_SMOKE_OUTPUT ?= metadata/wom-toolbar-row-smoke.json
WOM_TOOLBAR_ROW_SMOKE_SEED_OUTPUT ?= /tmp/adp-wom-toolbar-row-smoke-seed.json
WOM_TOOLBAR_ROW_SMOKE_SCREENSHOT ?= metadata/wom-toolbar-row-smoke.png
WOM_TOOLBAR_PAGE_TIMEOUT_MS ?= 240000
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
WOM_PUBLIC_PRODUCE_TASK_CREATED_NOOP_OUTPUT ?= metadata/wom-public-produce-task-created-noop-probe.json
WOM_QRCODE_ROUTE_PROBE_OUTPUT ?= metadata/wom-qrcode-route-probe.json
QCS_REPORT_CHAIN_MODE ?= qualified
QCS_REPORT_CHAIN_PERSISTENCE_OUTPUT ?= /tmp/adp-qcs-report-chain-persistence-acceptance.json
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

.PHONY: help ci verify verify-pom compose-config runtime-script-check sustainable-check ci-required-file-inventory ci-required-file-inventory-check ci-required-file-strict-check project-goal-acceptance-check goal-gap-register goal-gap-register-check backend-table-audit-handoff-check basic-config-coverage-check basic-config-action-matrix-check entity-model-config-crud-readiness-check test-environment-address-check test-environment-static-bundle-link-check persistence-acceptance-check production-testcase-check wom-toolbar-action-coverage-check production-blocker-check production-module-backlog-check production-action-map-check platform-validation-check runtime-smoke-reports-check business-dependency-readiness-check business-dependency-contract-check business-module-intake-requirements-check business-package-scan-check production-export-readiness-check production-export-gap-breakdown production-export-gap-breakdown-check production-source-evidence-refresh production-source-evidence-refresh-check production-migration-readiness-check production-cutover-gate-check production-rehearsal-plan production-rehearsal-plan-check production-evidence-ready-gate-regression-check runtime-patch-manifest runtime-patch-manifest-check source-module-check module-intake-precheck-regression-check module-intake-candidate-report-check source-module-test create-backend-module module-intake-check inventory inventory-check backend-dependency-inventory backend-dependency-check oracle-audit oracle-audit-check postgres-migration-index postgres-migration-check oracle-replacement-status oracle-replacement-check production-source-inventory production-target-preflight production-rowcount-compare production-checksum-compare production-db-migration-evidence-check production-db-migration-ready-check production-minio-source-inventory production-minio-target-inventory production-minio-compare production-minio-migration-evidence-check production-minio-migration-ready-check production-keycloak-source-export production-keycloak-target-export production-keycloak-compare production-keycloak-migration-evidence-check production-keycloak-migration-ready-check production-rollback-evidence-check production-rollback-ready-check production-license-strategy-check production-license-ready-check production-network-tls-check production-network-tls-ready-check production-security-hardening-check production-security-hardening-ready-check production-business-smoke-signoff-check production-business-smoke-signoff-ready-check production-nacos-runtime-patch-check production-nacos-runtime-patch-ready-check render-config prepare-runtime up-infra up down ps logs smoke-platform smoke-api smoke-menu smoke-todo smoke-organization smoke-test-environment smoke-postgres-runtime smoke-nacos-config smoke-keycloak-jwt smoke-minio-runtime smoke-business-dependencies business-package-scan smoke-production-export-readiness acceptance-organization-persistence acceptance-organization-group-persistence acceptance-organization-position-persistence acceptance-organization-position-role-persistence acceptance-organization-company-persistence acceptance-organization-person-persistence acceptance-organization-person-user-persistence acceptance-auth-user-persistence acceptance-rbac-permission-persistence acceptance-systemcode-persistence acceptance-systemconfig-persistence smoke-systemconfig-builtins acceptance-systemconfig-controlled-runtime-config smoke-runtime-configuration smoke-entity-model-config-crud-readiness acceptance-custom-property-persistence acceptance-wom-start-persistence acceptance-wom-hold-restart-persistence smoke-wom-toolbar-row acceptance-wom-stop-persistence acceptance-wom-stop-output-persistence acceptance-wom-advance-release-persistence acceptance-wom-prepare-need-persistence acceptance-wom-active-persistence acceptance-wom-active-end-persistence acceptance-wom-easy-active-persistence acceptance-wom-putin-active-persistence acceptance-wom-check-active-persistence acceptance-wom-process-start-persistence acceptance-wom-process-end-persistence acceptance-wom-process-unit-persistence acceptance-wom-manu-inspect-persistence acceptance-wom-checkoutbill-persistence probe-wom-public-produce-task-created-noop probe-wom-qrcode-route acceptance-qcs-report-chain-persistence acceptance-teaminfo-scheduleplan-persistence acceptance-craftgraph-persistence smoke-rbac-authority smoke-business smoke-business-page discover-production-actions audit-postgres-mappings audit-postgres-report

help:
	@printf '%s\n' 'FT MES development commands:'
	@printf '%s\n' '  make ci                     Run the repository CI gate locally'
	@printf '%s\n' '  make verify                 Validate Maven reactor and Docker Compose syntax'
	@printf '%s\n' '  make verify-pom             Validate parent/module POM structure'
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
	@printf '%s\n' '  make production-cutover-gate-check Validate the production no-cutover/ready gate'
	@printf '%s\n' '  make production-rehearsal-plan-check Validate production rehearsal evidence checklist'
	@printf '%s\n' '  make production-evidence-ready-gate-regression-check Ensure templates cannot satisfy production READY evidence'
	@printf '%s\n' '  make runtime-patch-manifest Regenerate runtime patch checksum manifest'
	@printf '%s\n' '  make runtime-patch-manifest-check Check runtime patch checksum manifest is fresh'
	@printf '%s\n' '  make source-module-check     Validate promoted backend source modules'
	@printf '%s\n' '  make source-module-test      Compile and test promoted backend source modules'
	@printf '%s\n' '  make create-backend-module MODULE=platform-auth [PACKAGE=com.example]'
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
	@printf '%s\n' '  make probe-wom-qrcode-route Run WOM QR code route/package probe against the test environment'
	@printf '%s\n' '  make acceptance-qcs-report-chain-persistence QCS report save/effective WOM backfill acceptance'
	@printf '%s\n' '  make acceptance-teaminfo-scheduleplan-persistence Run TeamInfo schedule-plan persistence acceptance'
	@printf '%s\n' '  make smoke-rbac-authority    Run role/user authority editor API smoke'
	@printf '%s\n' '  make smoke-business          Run API/layout smoke for restored business module routes'
	@printf '%s\n' '  make smoke-business-page     Run browser page smoke for restored business module routes'
	@printf '%s\n' '  make discover-production-actions Discover safe production create-entry UI actions'
	@printf '%s\n' '  make audit-postgres-mappings Audit mapper SQL for PostgreSQL migration risk'
	@printf '%s\n' '  make audit-postgres-report   Write a non-blocking PostgreSQL audit report'

ci: verify runtime-script-check sustainable-check ci-required-file-inventory-check ci-required-file-strict-check project-goal-acceptance-check goal-gap-register-check backend-table-audit-handoff-check basic-config-coverage-check basic-config-action-matrix-check entity-model-config-crud-readiness-check test-environment-address-check test-environment-static-bundle-link-check persistence-acceptance-check production-testcase-check wom-toolbar-action-coverage-check production-blocker-check production-module-backlog-check production-action-map-check platform-validation-check runtime-smoke-reports-check business-dependency-readiness-check business-dependency-contract-check business-module-intake-requirements-check business-package-scan-check production-export-readiness-check production-export-gap-breakdown-check production-source-evidence-refresh-check production-migration-readiness-check production-cutover-gate-check production-rehearsal-plan-check production-db-migration-evidence-check production-rollback-evidence-check production-license-strategy-check production-network-tls-check production-security-hardening-check production-business-smoke-signoff-check production-nacos-runtime-patch-check production-minio-migration-evidence-check production-keycloak-migration-evidence-check production-evidence-ready-gate-regression-check runtime-patch-manifest-check source-module-check module-intake-precheck-regression-check module-intake-candidate-report-check source-module-test inventory-check backend-dependency-check oracle-audit-check postgres-migration-check oracle-replacement-check audit-postgres-mappings

verify: verify-pom compose-config

verify-pom:
	$(MVN) -q -DskipTests validate

compose-config:
	$(COMPOSE) config --quiet

runtime-script-check:
	sh -n deploy/docker/scripts/prepare-runtime-patches.sh
	sh -n deploy/docker/scripts/build-rm-import-transaction-patch.sh
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
	$(NODE) --check deploy/docker/scripts/adp-custom-property-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-wom-start-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-wom-toolbar-row-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-wom-prepare-need-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-wom-active-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-wom-manu-inspect-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-wom-checkoutbill-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-wom-public-produce-task-created-noop-probe.js
	$(NODE) --check deploy/docker/scripts/adp-wom-qrcode-route-probe.js
	$(NODE) --check deploy/docker/scripts/adp-qcs-report-chain-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-teaminfo-scheduleplan-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-craftgraph-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-rbac-authority-smoke.js
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
	$(PYTHON) -m py_compile deploy/docker/scripts/patch-orgmanagement-rbac-permission-mapper.py
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

project-goal-acceptance-check:
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

oracle-replacement-check:
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

smoke-systemconfig-builtins:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_SYSTEMCONFIG_BUILTINS_OUTPUT=$(SYSTEMCONFIG_BUILTINS_OUTPUT) $(NODE) deploy/docker/scripts/adp-systemconfig-builtins-readiness-smoke.js

acceptance-systemconfig-controlled-runtime-config:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_SYSTEMCONFIG_CONTROLLED_OUTPUT=$(SYSTEMCONFIG_CONTROLLED_OUTPUT) ADP_SYSTEMCONFIG_CONTROLLED_TARGET_MODE=$(SYSTEMCONFIG_CONTROLLED_TARGET_MODE) $(NODE) deploy/docker/scripts/adp-systemconfig-controlled-runtime-config-acceptance.js

smoke-runtime-configuration:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_RUNTIME_CONFIG_SMOKE_OUTPUT=$(RUNTIME_CONFIG_SMOKE_OUTPUT) $(NODE) deploy/docker/scripts/adp-runtime-configuration-readiness-smoke.js

smoke-entity-model-config-crud-readiness:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ENTITY_MODEL_CONFIG_CRUD_READINESS_OUTPUT=$(ENTITY_MODEL_CONFIG_CRUD_READINESS_OUTPUT) $(NODE) deploy/docker/scripts/adp-entity-model-config-crud-readiness-probe.js

acceptance-custom-property-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_CUSTOM_PROPERTY_ACCEPTANCE_OUTPUT=$(CUSTOM_PROPERTY_ACCEPTANCE_OUTPUT) ADP_API_TIMEOUT_MS=$(ADP_API_TIMEOUT_MS) $(NODE) deploy/docker/scripts/adp-custom-property-persistence-acceptance.js

acceptance-wom-start-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_START_PERSISTENCE_OUTPUT=$(WOM_START_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-start-persistence-acceptance.js

acceptance-wom-hold-restart-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_TRANSITIONS=start,hold,restart ADP_WOM_STATE_PERSISTENCE_OUTPUT=$(WOM_HOLD_RESTART_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-start-persistence-acceptance.js

smoke-wom-toolbar-row:
	$(MAKE) acceptance-wom-hold-restart-persistence WOM_HOLD_RESTART_PERSISTENCE_OUTPUT=$(WOM_TOOLBAR_ROW_SMOKE_SEED_OUTPUT) ADP_PAGE_TIMEOUT_MS=$(WOM_TOOLBAR_PAGE_TIMEOUT_MS) ADP_GRID_TIMEOUT_MS=$(WOM_TOOLBAR_PAGE_TIMEOUT_MS)
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_PAGE_TIMEOUT_MS=$(WOM_TOOLBAR_PAGE_TIMEOUT_MS) ADP_GRID_TIMEOUT_MS=$(WOM_TOOLBAR_PAGE_TIMEOUT_MS) ADP_WOM_TOOLBAR_SEED_EVIDENCE=$(WOM_TOOLBAR_ROW_SMOKE_SEED_OUTPUT) ADP_WOM_TOOLBAR_ROW_SMOKE_OUTPUT=$(WOM_TOOLBAR_ROW_SMOKE_OUTPUT) ADP_WOM_TOOLBAR_SCREENSHOT=$(WOM_TOOLBAR_ROW_SMOKE_SCREENSHOT) $(NODE) deploy/docker/scripts/adp-wom-toolbar-row-smoke.js

acceptance-wom-stop-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_TRANSITIONS=start,stop ADP_WOM_STATE_PERSISTENCE_OUTPUT=$(WOM_STOP_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-start-persistence-acceptance.js

acceptance-wom-stop-output-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_TRANSITIONS=start,stop-output ADP_WOM_STATE_PERSISTENCE_OUTPUT=$(WOM_STOP_OUTPUT_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-start-persistence-acceptance.js

acceptance-wom-advance-release-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_TRANSITIONS=advance-release ADP_WOM_STATE_PERSISTENCE_OUTPUT=$(WOM_ADVANCE_RELEASE_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-start-persistence-acceptance.js

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

probe-wom-public-produce-task-created-noop:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_WOM_PUBLIC_PRODUCE_TASK_CREATED_NOOP_OUTPUT=$(WOM_PUBLIC_PRODUCE_TASK_CREATED_NOOP_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-public-produce-task-created-noop-probe.js

probe-wom-qrcode-route:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_WOM_QRCODE_ROUTE_PROBE_OUTPUT=$(WOM_QRCODE_ROUTE_PROBE_OUTPUT) $(NODE) deploy/docker/scripts/adp-wom-qrcode-route-probe.js

acceptance-qcs-report-chain-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_BROWSER_BASE_URL=$(ADP_BROWSER_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_QCS_REPORT_CHAIN_MODE=$(QCS_REPORT_CHAIN_MODE) ADP_QCS_REPORT_CHAIN_PERSISTENCE_OUTPUT=$(QCS_REPORT_CHAIN_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-qcs-report-chain-persistence-acceptance.js

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
