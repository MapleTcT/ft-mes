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

ADP_BASE_URL ?= http://10.11.100.17:18080
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
ORGANIZATION_PERSISTENCE_OUTPUT ?= /tmp/adp-organization-persistence-acceptance.json
ORGANIZATION_GROUP_PERSISTENCE_OUTPUT ?= /tmp/adp-organization-group-persistence-acceptance.json
BUSINESS_PAGE_SMOKE_OUTPUT ?= /tmp/adp-business-page-smoke
PRODUCTION_DISCOVERY_OUTPUT ?= /tmp/adp-production-action-discovery
PRODUCTION_DISCOVERY_TARGETS ?=
PRODUCTION_DISCOVERY_CLICK_CREATE ?= true

.PHONY: help ci verify verify-pom compose-config runtime-script-check sustainable-check project-goal-acceptance-check persistence-acceptance-check production-testcase-check production-action-map-check production-migration-readiness-check source-module-check source-module-test create-backend-module module-intake-check inventory inventory-check backend-dependency-inventory backend-dependency-check oracle-audit oracle-audit-check postgres-migration-index postgres-migration-check oracle-replacement-status oracle-replacement-check render-config prepare-runtime up-infra up down ps logs smoke-platform smoke-api smoke-menu smoke-todo smoke-organization acceptance-organization-persistence acceptance-organization-group-persistence smoke-rbac-authority smoke-business smoke-business-page discover-production-actions audit-postgres-mappings audit-postgres-report

help:
	@printf '%s\n' 'FT MES development commands:'
	@printf '%s\n' '  make ci                     Run the repository CI gate locally'
	@printf '%s\n' '  make verify                 Validate Maven reactor and Docker Compose syntax'
	@printf '%s\n' '  make verify-pom             Validate parent/module POM structure'
	@printf '%s\n' '  make compose-config          Validate Docker Compose rendering'
	@printf '%s\n' '  make runtime-script-check    Validate smoke and runtime patch scripts parse'
	@printf '%s\n' '  make sustainable-check       Validate repository governance invariants'
	@printf '%s\n' '  make project-goal-acceptance-check Validate objective-level acceptance ledger'
	@printf '%s\n' '  make persistence-acceptance-check Validate functional/persistence acceptance assets'
	@printf '%s\n' '  make production-testcase-check Validate production module action test matrix'
	@printf '%s\n' '  make production-action-map-check Validate production source action map'
	@printf '%s\n' '  make production-migration-readiness-check Validate production migration readiness ledger'
	@printf '%s\n' '  make source-module-check     Validate promoted backend source modules'
	@printf '%s\n' '  make source-module-test      Compile and test promoted backend source modules'
	@printf '%s\n' '  make create-backend-module MODULE=platform-auth [PACKAGE=com.example]'
	@printf '%s\n' '  make module-intake-check INTAKE=/path/to/package-or-dir'
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
	@printf '%s\n' '  make acceptance-organization-persistence Run organization CRUD persistence acceptance'
	@printf '%s\n' '  make acceptance-organization-group-persistence Run organization group CRUD persistence acceptance'
	@printf '%s\n' '  make smoke-rbac-authority    Run role/user authority editor API smoke'
	@printf '%s\n' '  make smoke-business          Run API/layout smoke for restored business module routes'
	@printf '%s\n' '  make smoke-business-page     Run browser page smoke for restored business module routes'
	@printf '%s\n' '  make discover-production-actions Discover safe production create-entry UI actions'
	@printf '%s\n' '  make audit-postgres-mappings Audit mapper SQL for PostgreSQL migration risk'
	@printf '%s\n' '  make audit-postgres-report   Write a non-blocking PostgreSQL audit report'

ci: verify runtime-script-check sustainable-check project-goal-acceptance-check persistence-acceptance-check production-testcase-check production-action-map-check production-migration-readiness-check source-module-check source-module-test inventory-check backend-dependency-check oracle-audit-check postgres-migration-check oracle-replacement-check audit-postgres-mappings

verify: verify-pom compose-config

verify-pom:
	$(MVN) -q -DskipTests validate

compose-config:
	$(COMPOSE) config --quiet

runtime-script-check:
	sh -n deploy/database/production-migration/scripts/run-target-preflight.sh
	$(NODE) --check deploy/docker/scripts/adp-platform-api-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-menu-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-home-todo-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-organization-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-organization-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-organization-group-persistence-acceptance.js
	$(NODE) --check deploy/docker/scripts/adp-rbac-authority-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-business-module-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-business-page-smoke.js
	$(NODE) --check deploy/docker/scripts/adp-production-action-discovery.js
	$(NODE) --check deploy/docker/scripts/adp-platform-validation-smoke.js
	$(PYTHON) -m py_compile deploy/docker/scripts/patch-orgmanagement-rbac-permission-mapper.py
	$(PYTHON) -m py_compile deploy/docker/scripts/patch-eam-reactapi-ready.py

sustainable-check:
	$(PYTHON) scripts/verify-sustainable-repo.py

project-goal-acceptance-check:
	$(PYTHON) scripts/verify-project-goal-acceptance.py

persistence-acceptance-check:
	$(PYTHON) scripts/verify-persistence-acceptance.py

production-testcase-check:
	$(PYTHON) scripts/verify-production-module-test-cases.py

production-action-map-check:
	$(PYTHON) scripts/verify-production-action-map.py

production-migration-readiness-check:
	$(PYTHON) scripts/verify-production-migration-readiness.py

source-module-check:
	$(PYTHON) scripts/verify-source-modules.py

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
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_PLATFORM_OUTPUT_DIR=$(PLATFORM_SMOKE_OUTPUT) ADP_PLATFORM_MENU_LIMIT=$(PLATFORM_MENU_LIMIT) $(NODE) deploy/docker/scripts/adp-platform-validation-smoke.js

smoke-api:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) $(NODE) deploy/docker/scripts/adp-platform-api-smoke.js

smoke-menu:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) $(NODE) deploy/docker/scripts/adp-menu-smoke.js

smoke-todo:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) $(NODE) deploy/docker/scripts/adp-home-todo-smoke.js

smoke-organization:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) $(NODE) deploy/docker/scripts/adp-organization-smoke.js

acceptance-organization-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_ORGANIZATION_PERSISTENCE_OUTPUT=$(ORGANIZATION_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-organization-persistence-acceptance.js

acceptance-organization-group-persistence:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) ADP_ORGANIZATION_GROUP_PERSISTENCE_OUTPUT=$(ORGANIZATION_GROUP_PERSISTENCE_OUTPUT) $(NODE) deploy/docker/scripts/adp-organization-group-persistence-acceptance.js

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
