# Production License Strategy Evidence

This folder contains the machine-readable production license decision template
and validator. It exists because the Docker test profile may bypass the
software-dog path, while production must not silently inherit that bypass.

## Files

| File | Purpose |
| --- | --- |
| `license-decision.example.json` | Placeholder-only production license decision template. |
| `scripts/validate-license-decision.py` | Validates decision coverage, READY rules and secret hygiene. |

## Commands

```bash
make production-license-strategy-check
make production-license-strategy-check LICENSE_DECISION=/secure/path/adp-license-decision.json
make production-license-ready-check LICENSE_DECISION=/secure/path/adp-license-decision.json
```

The default check validates the example template shape. `production-license-ready-check`
is for a real production decision record and fails unless the production mode,
expiry behavior, emergency path, config backup/restore, and monitoring/audit
rules are all signed off.
READY license evidence must reference real production decisions, vendor/support
records, rehearsal output or approved audit records. Paths containing
`.example`, `example.`, `-example`, `_example`, `template` or `sample` are
accepted only for placeholder/template checks and are rejected as READY
evidence.

## Production Rule

The strategy must remain `PLANNED` until the production owner decides whether
the system will use formal vendor license material, a supported license service
integration, or a time-bound emergency bypass with explicit approval and audit.
Real license keys, tokens, dongle secrets and contract files must stay outside
git.
