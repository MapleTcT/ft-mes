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

.PHONY: help verify verify-pom compose-config render-config prepare-runtime up-infra up down ps logs smoke-api smoke-menu smoke-todo smoke-business audit-postgres-mappings

help:
	@printf '%s\n' 'FT MES development commands:'
	@printf '%s\n' '  make verify                 Validate Maven reactor and Docker Compose syntax'
	@printf '%s\n' '  make verify-pom             Validate parent/module POM structure'
	@printf '%s\n' '  make compose-config          Validate Docker Compose rendering'
	@printf '%s\n' '  make render-config           Render Nacos configs from deploy/docker/.env'
	@printf '%s\n' '  make prepare-runtime         Prepare static assets and runtime patch assets'
	@printf '%s\n' '  make up-infra                Start infrastructure services only'
	@printf '%s\n' '  make up                      Start the full Docker profile'
	@printf '%s\n' '  make down                    Stop the Docker profile'
	@printf '%s\n' '  make ps                      Show Docker profile status'
	@printf '%s\n' '  make logs SERVICE=gateway    Tail one service log, or all logs if SERVICE is empty'
	@printf '%s\n' '  make smoke-api               Run API smoke against ADP_BASE_URL'
	@printf '%s\n' '  make smoke-menu              Run browser menu smoke against ADP_BASE_URL'
	@printf '%s\n' '  make smoke-todo              Run home Todo smoke against ADP_BASE_URL'
	@printf '%s\n' '  make audit-postgres-mappings Audit mapper SQL for PostgreSQL migration risk'

verify: verify-pom compose-config

verify-pom:
	$(MVN) -q -DskipTests validate

compose-config:
	$(COMPOSE) config --quiet

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

smoke-api:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) $(NODE) deploy/docker/scripts/adp-platform-api-smoke.js

smoke-menu:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) $(NODE) deploy/docker/scripts/adp-menu-smoke.js

smoke-todo:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) $(NODE) deploy/docker/scripts/adp-home-todo-smoke.js

smoke-business:
	ADP_BASE_URL=$(ADP_BASE_URL) ADP_USERNAME=$(ADP_USERNAME) ADP_PASSWORD=$(ADP_PASSWORD) $(NODE) deploy/docker/scripts/adp-business-module-smoke.js

audit-postgres-mappings:
	$(PYTHON) deploy/docker/scripts/audit-postgres-mappings.py backend/modules deploy/docker/postgres/init
