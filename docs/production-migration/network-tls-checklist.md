# Network, Domain And TLS Checklist

## Status

Status: `PLANNED`

## External Entry Points

| Component | Public Exposure | Domain | TLS | Notes |
| --- | --- | --- | --- | --- |
| Frontend / gateway | TBD | TBD | Required | Production user entry |
| Keycloak | TBD | TBD | Required | Only expose if required |
| MinIO console/API | TBD | TBD | Required | Prefer private network |
| Nacos | No by default | Internal only | Internal policy | Do not expose publicly |
| PostgreSQL | No by default | Internal only | Internal policy | Private network only |

## Checklist

- Production domain names approved.
- TLS certificate source and renewal owner defined.
- Reverse proxy headers reviewed: `Host`, `X-Forwarded-Proto`, `X-Forwarded-For`.
- HSTS decision documented.
- Firewall or security group rules reviewed.
- Admin-only services are private or VPN-only.
- Port mapping differs from test ports where required.
- Certificate renewal dry-run completed.
