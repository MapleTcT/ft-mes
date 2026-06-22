# Production Security Hardening Evidence

This folder contains the machine-readable production security hardening
template and validator. It exists because the Docker test profile intentionally
keeps several convenience defaults for repeatable validation, while production
must prove account cleanup, secret management, least privilege, image/runtime
hardening, audit logging and alerting separately.

## Files

| File | Purpose |
| --- | --- |
| `security-hardening-plan.example.json` | Placeholder-only production security hardening template. |
| `scripts/validate-security-hardening-plan.py` | Validates control coverage, READY rules and secret hygiene. |

## Commands

```bash
make production-security-hardening-check
make production-security-hardening-check SECURITY_HARDENING_PLAN=/secure/path/adp-security-hardening-plan.json
make production-security-hardening-ready-check SECURITY_HARDENING_PLAN=/secure/path/adp-security-hardening-plan.json
```

The default check validates the example template shape. `production-security-hardening-ready-check`
is for a real production evidence file and fails unless every required control
is READY.
READY security hardening evidence must reference real production records,
inventory exports, scan reports, grant reviews or alerting evidence. Paths
containing `.example`, `example.`, `-example`, `_example`, `template` or
`sample` are accepted only for placeholder/template checks and are rejected as
READY evidence.

## Production Rule

Test passwords, test license bypasses, local Compose defaults and smoke-only
accounts are not production security proof. Production evidence must reference
approved records, inventory exports, scan reports, grant reviews, audit-log
retention configuration and alert rules. Real passwords, private keys, tokens,
client secrets and license material must stay outside git.
