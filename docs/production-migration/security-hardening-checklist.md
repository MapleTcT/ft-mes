# Security Hardening Checklist

## Status

Status: `PLANNED`

Machine-readable evidence template and validator:

- `deploy/security/production-migration/security-hardening-plan.example.json`
- `deploy/security/production-migration/scripts/validate-security-hardening-plan.py`

```bash
make production-security-hardening-check
make production-security-hardening-check SECURITY_HARDENING_PLAN=/secure/path/adp-security-hardening-plan.json
make production-security-hardening-ready-check SECURITY_HARDENING_PLAN=/secure/path/adp-security-hardening-plan.json
```

The default check validates the placeholder template only. `production-security-hardening-ready-check`
is reserved for real production evidence and must fail until every required
account, secret, database, container, network, audit and monitoring control is
READY. Test passwords, smoke accounts, license seed/bypass settings and local
Compose defaults are not production hardening evidence.

## Account And Secret Controls

| Control | Required Evidence | Status |
| --- | --- | --- |
| Default admin password rotated | Change record and smoke proof | BLOCKED |
| Test accounts removed or scoped | Account inventory | BLOCKED |
| Service accounts use least privilege | Privilege matrix | BLOCKED |
| Database users split by service | Role grants review | BLOCKED |
| Secrets stored outside git | Secret inventory without values | BLOCKED |
| Client secrets rotated | Rotation record | BLOCKED |
| Test runtime bypasses disabled | Nacos/Compose/runtime patch review | BLOCKED |

## Runtime Controls

| Control | Required Evidence | Status |
| --- | --- | --- |
| Container images scanned | Scan report | BLOCKED |
| Containers run as non-root where possible | Compose or runtime proof | BLOCKED |
| Host firewall reviewed | Rule export | BLOCKED |
| Audit logs retained | Retention config | BLOCKED |
| Alerting configured | Alert rule list | BLOCKED |

## Required Evidence

| Control | Required content | Strict READY rule |
| --- | --- | --- |
| Default admin password rotation | Change record and post-rotation smoke without secret values | READY evidence attached |
| Test account cleanup | Account inventory, disabled test users and approved break-glass owner | READY evidence attached |
| Test runtime bypass cleanup | License seed, simulated login and debug bypass review | READY evidence attached or formally risk accepted |
| Service account least privilege | Service account inventory and privilege matrix | READY evidence attached |
| Database user split | PostgreSQL runtime/migration/audit role grants review | READY evidence attached |
| Secret management | Secret inventory without values and approved secret manager path | READY evidence attached |
| Client secret rotation | Keycloak client-secret rotation and login/API smoke | READY evidence attached |
| Image scan | Scanner report and accepted exception list | READY evidence attached |
| Container runtime user | Non-root policy or signed exception per service | READY evidence attached |
| Firewall review | Firewall/security-group export cross-linked to network/TLS plan | READY evidence attached |
| Audit retention | Application, gateway, Keycloak, DB and host log retention config | READY evidence attached |
| Alerting | Authentication, service health, DB error, license, certificate and storage alert rules | READY evidence attached |

## Repository Rule

Real passwords, private keys, tokens and production license materials must never
be committed. Use placeholders in this repository and store real values in the
approved secret manager.

The real production evidence file must pass:

```bash
make production-security-hardening-ready-check SECURITY_HARDENING_PLAN=/secure/path/adp-security-hardening-plan.json
```
