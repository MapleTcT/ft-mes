# Business Smoke Signoff Template

## Status

Status: `BLOCKED`

This template becomes a signoff artifact only after real frontend smoke and
PostgreSQL persistence evidence are attached.

Machine-readable evidence template and validator:

- `deploy/business-smoke/production-migration/business-smoke-signoff.example.json`
- `deploy/business-smoke/production-migration/scripts/validate-business-smoke-signoff.py`
- `metadata/production-module-blockers.json`
- `metadata/production-module-backlog.json`
- `metadata/basic-config-coverage.json`
- `metadata/production-export-gap-breakdown.json`

```bash
make production-business-smoke-signoff-check
make production-business-smoke-signoff-check BUSINESS_SMOKE_SIGNOFF=/secure/path/adp-business-smoke-signoff.json
make production-business-smoke-signoff-ready-check BUSINESS_SMOKE_SIGNOFF=/secure/path/adp-business-smoke-signoff.json
```

The default check validates the placeholder template only. `production-business-smoke-signoff-ready-check`
is reserved for a real production signoff record and must fail until all
required smoke suites are PASS or explicitly NOT_APPLICABLE, all required
persistence scopes are PASS, unresolved issues are closed or signed by risk
acceptance, and business, technical, database and release owners have approved.
HTTP 200 without PostgreSQL before/after evidence is not enough for a write
action.

The validator cross-checks `metadata/production-module-blockers.json`,
`metadata/production-module-backlog.json` and
`metadata/basic-config-coverage.json`, plus the per-target export gap ledger
`metadata/production-export-gap-breakdown.json`. While the production blocker
ledger has unresolved `PROD-*` cases, the backlog ledger has unresolved
action-level items such as public `produceTaskCreated` false success, G-012
basic configuration coverage has non-PASS areas, or the export gap ledger has
unresolved list export targets, the signoff must cover every blocker case id,
backlog item id, basic configuration area id and unresolved `exportTargetIds`
entry in `openIssues`. READY is rejected until basic configuration coverage is
PASS, and remaining production blocker/backlog/export target items have either
been resolved or have signed risk acceptance.
For `production-business-smoke-signoff-ready-check`, risk acceptance for any
remaining blocker/backlog/export target item must be structured as an object with
`decision=ACCEPTED`, `acceptedBy`, `signedAt` and `evidence`. A plain string is
accepted only by the placeholder/template check and is rejected for READY.
Strict READY also rejects evidence URLs or signoff frontend URLs that point at
known test-environment hosts such as `100.99.133.43`, `10.11.100.17`,
`222.88.185.146`, `ubuntu-test` or `v6-2288H-V6`; those runs can support test
evidence, but they cannot masquerade as production or cutover-rehearsal signoff.

## Environment

| Field | Value |
| --- | --- |
| Date | TBD |
| Environment | TBD |
| Frontend URL | TBD |
| Backend build / commit | TBD |
| Database | PostgreSQL |
| Tester | TBD |
| Business owner | TBD |

## Required Smoke

| Area | Required Evidence | Result |
| --- | --- | --- |
| Login / authentication | Browser smoke and API record | BLOCKED |
| User / organization / permissions | CRUD and PostgreSQL evidence | BLOCKED |
| Menu / todo / dashboard | Browser smoke with console/network record | BLOCKED |
| Basic configuration | Browser smoke, persistence proof where applicable, and `metadata/basic-config-coverage.json` | BLOCKED |
| Nacos / Keycloak / runtime patch | Config evidence and health checks | BLOCKED |
| Production main flow | Full WOM flow and persistence proof | BLOCKED |
| Quality QCS / LIMS | QCS report save/effective qualified and unqualified marker evidence, plus production or rehearsal owner signoff | BLOCKED |
| Import / export / upload if used | Browser and file evidence | BLOCKED |
| PostgreSQL gap ledger | Idempotent SQL/backlog and mapper audit review | BLOCKED |

## Signoff

| Role | Name | Decision | Date | Notes |
| --- | --- | --- | --- | --- |
| Business owner | TBD | BLOCKED | TBD | Remaining WOM/material/export blockers and production signoff are not complete |
| Technical owner | TBD | BLOCKED | TBD | Pending production migration rehearsal |
| Database owner | TBD | BLOCKED | TBD | PostgreSQL gap closure review required |
| Release owner | TBD | BLOCKED | TBD | Rollback rehearsal required |
