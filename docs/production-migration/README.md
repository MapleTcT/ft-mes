# Production Migration Handoff

This directory contains handoff templates for production migration preparation.
They do not mean the system is ready for production cutover.

Authoritative status remains:

- `docs/production-migration-readiness.md`
- `metadata/production-migration-readiness.json`
- `docs/production-cutover-gate.md`
- `metadata/production-cutover-gate.json`
- `metadata/production-rehearsal-plan.json`
- `make production-migration-readiness-check`
- `make production-cutover-gate-check`
- `make production-rehearsal-plan-check`

Each template is designed to be filled during rehearsal or production planning:

| File | Purpose |
| --- | --- |
| `../production-cutover-gate.md` | Generated no-cutover/cutover gate summary for handoff review |
| `rehearsal-plan.md` | Generated execution checklist for collecting one production rehearsal evidence set |
| `../../deploy/database/production-migration/README.md` | Source inventory, target PostgreSQL preflight, and row-count comparison entry |
| `../../deploy/minio/production-migration/README.md` | MinIO bucket/object inventory and comparison entry |
| `../../deploy/keycloak/production-migration/README.md` | Keycloak source/target realm inventory and comparison entry |
| `../../deploy/rollback/production-migration/README.md` | Rollback evidence manifest and readiness validation entry |
| `../../deploy/license/production-migration/README.md` | Production license decision and readiness validation entry |
| `../../deploy/network/production-migration/README.md` | Production domain, TLS, reverse proxy, firewall and service exposure validation entry |
| `../../deploy/security/production-migration/README.md` | Production security hardening control evidence and readiness validation entry |
| `../../deploy/business-smoke/production-migration/README.md` | Production business smoke, persistence and owner signoff validation entry |
| `rollback-runbook.md` | Cutover rollback steps and rehearsal evidence |
| `license-strategy.md` | Production license, bypass and audit decisions |
| `minio-migration-runbook.md` | Bucket/object migration and validation |
| `keycloak-production-runbook.md` | Realm, user, client and JWT key migration |
| `nacos-runtime-patch-runbook.md` | Nacos/rendered config and runtime patch productionization |
| `network-tls-checklist.md` | Domain, port, reverse proxy and TLS boundary |
| `security-hardening-checklist.md` | Account, secret, database and container hardening |
| `business-smoke-signoff-template.md` | Business smoke and persistence signoff |
