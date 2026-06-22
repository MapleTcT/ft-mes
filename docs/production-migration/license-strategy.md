# Production License Strategy

## Status

Status: `PLANNED`

The test environment may bypass the software-dog path to reduce validation
friction. Production must not silently inherit the test bypass.

## Tooling

| Asset | Purpose | Status |
| --- | --- | --- |
| `deploy/license/production-migration/license-decision.example.json` | Placeholder-only production license decision template | READY |
| `deploy/license/production-migration/scripts/validate-license-decision.py` | Validates decision coverage, READY rules and secret hygiene | READY |

```bash
make production-license-strategy-check
make production-license-strategy-check LICENSE_DECISION=/secure/path/adp-license-decision.json
make production-license-ready-check LICENSE_DECISION=/secure/path/adp-license-decision.json
```

`production-license-strategy-check` validates structure and required decision
coverage. `production-license-ready-check` is intentionally stricter and fails
until the production owner signs a real decision record. A production decision
must explicitly choose one of:

- formal vendor license;
- supported license service integration;
- approved, time-bound emergency bypass with audit and rollback procedure.

## Decision Checklist

| Decision | Required Answer | Evidence |
| --- | --- | --- |
| Formal license procurement or integration | TBD | Contract, license file or vendor confirmation |
| License expiry behavior | TBD | Test result or vendor document |
| Emergency bypass allowed | TBD | Approval owner and audit rule |
| Redis/Nacos/gateway license backup | TBD | Backup and restore procedure |
| Monitoring and alerting | TBD | Alert rule and runbook |

## Production Rules

- No real license key, token or dongle secret is committed to this repository.
- Any emergency bypass must have an approver, start time, end time and audit log.
- Test bypass settings must be reviewed before promoting configs.
- License failure mode must be tested in a non-production rehearsal.
- `testBypassPromotedToProduction` must remain `false` in the production decision evidence.
