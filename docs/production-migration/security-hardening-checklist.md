# Security Hardening Checklist

## Status

Status: `PLANNED`

## Account And Secret Controls

| Control | Required Evidence | Status |
| --- | --- | --- |
| Default admin password rotated | Change record and smoke proof | BLOCKED |
| Test accounts removed or scoped | Account inventory | BLOCKED |
| Service accounts use least privilege | Privilege matrix | BLOCKED |
| Database users split by service | Role grants review | BLOCKED |
| Secrets stored outside git | Secret inventory without values | BLOCKED |
| Client secrets rotated | Rotation record | BLOCKED |

## Runtime Controls

| Control | Required Evidence | Status |
| --- | --- | --- |
| Container images scanned | Scan report | BLOCKED |
| Containers run as non-root where possible | Compose or runtime proof | BLOCKED |
| Host firewall reviewed | Rule export | BLOCKED |
| Audit logs retained | Retention config | BLOCKED |
| Alerting configured | Alert rule list | BLOCKED |

## Repository Rule

Real passwords, private keys, tokens and production license materials must never
be committed. Use placeholders in this repository and store real values in the
approved secret manager.
