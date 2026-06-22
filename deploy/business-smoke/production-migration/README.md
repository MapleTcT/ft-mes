# Production Business Smoke Signoff Evidence

This folder contains the machine-readable production business smoke signoff
template and validator. It exists because page smoke, API status codes and
static repository gates are not enough for production cutover. A production
signoff must prove real frontend behavior, write-action persistence in
PostgreSQL, unresolved-gap handling and owner approval.

## Files

| File | Purpose |
| --- | --- |
| `business-smoke-signoff.example.json` | Placeholder-only business smoke and persistence signoff template. |
| `scripts/validate-business-smoke-signoff.py` | Validates smoke scope, persistence scope, owner signoffs, production blocker/backlog/export-target coverage and READY rules. |

## Commands

```bash
make production-business-smoke-signoff-check
make production-business-smoke-signoff-check BUSINESS_SMOKE_SIGNOFF=/secure/path/adp-business-smoke-signoff.json
make production-business-smoke-signoff-ready-check BUSINESS_SMOKE_SIGNOFF=/secure/path/adp-business-smoke-signoff.json
```

The default check validates the example template shape. `production-business-smoke-signoff-ready-check`
is for a real production signoff record and fails unless all required smoke
suites are PASS or explicitly NOT_APPLICABLE, all required persistence scopes
are PASS, open issues are empty or signed risk acceptances, and every required
owner has approved.

The validator also reads `metadata/production-module-blockers.json`,
`metadata/production-module-backlog.json` and
`metadata/production-export-gap-breakdown.json`. When those ledgers contain
unresolved production cases, action-level backlog items or target export gaps,
the signoff must reference and cover every blocker case id, every backlog item
id and every unresolved `exportTargetIds` entry. A READY signoff is allowed
only after those issues are resolved or every remaining item has explicit
signed risk acceptance.
In a strict READY signoff, each unresolved issue's `riskAcceptance` must be a
signed object with `decision=ACCEPTED`, `acceptedBy`, `signedAt` and `evidence`.
Plain text such as "approved" or "accepted by business" is not sufficient for
production readiness.
The validator also reads `metadata/basic-config-coverage.json`. A READY
business smoke signoff is rejected while any G-012 basic-configuration area is
still `PARTIAL`, `BLOCKED` or `NOT_RUN`; those area ids must be visible in
`openIssues` so they cannot be hidden behind a general platform approval.
PASS smoke suites, PASS persistence scopes and APPROVED owner signoffs must
reference real production or rehearsal evidence. Evidence paths containing
`.example`, `example.`, `-example`, `_example`, `template` or `sample` are
accepted only for placeholder/template checks and are rejected as READY
evidence.
Strict READY also rejects evidence or `frontendUrl` values pointing at known
test-environment hosts, including `100.99.133.43`, `10.11.100.17`,
`222.88.185.146`, `ubuntu-test` and `v6-2288H-V6`. The current test host can
support diagnosis and test evidence, but it cannot be used as production
business signoff proof.

## Production Rule

The signoff must be based on real browser/API evidence and direct PostgreSQL
verification. Mock data, source-only inference, HTTP 200 without database
change, or a green `make ci` run cannot be used as business signoff evidence.
Evidence files may reference secure external systems, but real passwords,
tokens, private keys and production personal data must stay outside git.
