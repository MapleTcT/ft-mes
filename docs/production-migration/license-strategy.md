# Production License Strategy

## Status

Status: `PLANNED`

The test environment may bypass the software-dog path to reduce validation
friction. Production must not silently inherit the test bypass.

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
