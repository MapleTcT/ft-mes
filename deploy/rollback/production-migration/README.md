# Production Rollback Evidence

This folder contains the machine-readable rollback evidence template and
validator for production migration rehearsal. It does not run rollback commands.

## Files

| File | Purpose |
| --- | --- |
| `rollback-evidence.example.json` | Placeholder-only rollback rehearsal evidence template. |
| `scripts/validate-rollback-evidence.py` | Validates component coverage, READY rules, artifact paths and secret hygiene. |

## Commands

```bash
make production-rollback-evidence-check
make production-rollback-evidence-check ROLLBACK_EVIDENCE=/secure/path/adp-rollback-evidence.json
make production-rollback-ready-check ROLLBACK_EVIDENCE=/secure/path/adp-rollback-evidence.json
```

The default check validates the example template shape. `production-rollback-ready-check`
is for a real rehearsal record and fails unless every rollback component is `READY`.
READY rollback backup and rehearsal evidence must reference real rehearsal or
production records. Paths containing `.example`, `example.`, `-example`,
`_example`, `template` or `sample` are accepted only for placeholder/template
checks and are rejected as READY evidence.

## Required Rollback Components

- PostgreSQL target database
- MinIO objects
- Keycloak realm and users
- Nacos configuration
- Runtime patch set
- Domain, port and TLS entry

The production migration readiness track must remain `PLANNED` until a real
evidence file passes `production-rollback-ready-check` and the rehearsal output
is referenced from the production migration readiness ledger.
