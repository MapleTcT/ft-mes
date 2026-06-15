# Production Migration Handoff

This directory contains handoff templates for production migration preparation.
They do not mean the system is ready for production cutover.

Authoritative status remains:

- `docs/production-migration-readiness.md`
- `metadata/production-migration-readiness.json`
- `make production-migration-readiness-check`

Each template is designed to be filled during rehearsal or production planning:

| File | Purpose |
| --- | --- |
| `rollback-runbook.md` | Cutover rollback steps and rehearsal evidence |
| `license-strategy.md` | Production license, bypass and audit decisions |
| `minio-migration-runbook.md` | Bucket/object migration and validation |
| `keycloak-production-runbook.md` | Realm, user, client and JWT key migration |
| `nacos-runtime-patch-runbook.md` | Nacos/rendered config and runtime patch productionization |
| `network-tls-checklist.md` | Domain, port, reverse proxy and TLS boundary |
| `security-hardening-checklist.md` | Account, secret, database and container hardening |
| `business-smoke-signoff-template.md` | Business smoke and persistence signoff |
