# Network, Domain And TLS Checklist

## Status

Status: `PLANNED`

Machine-readable evidence template and validator:

- `deploy/network/production-migration/network-tls-plan.example.json`
- `deploy/network/production-migration/scripts/validate-network-tls-plan.py`

```bash
make production-network-tls-check
make production-network-tls-check NETWORK_TLS_PLAN=/secure/path/adp-network-tls-plan.json
make production-network-tls-ready-check NETWORK_TLS_PLAN=/secure/path/adp-network-tls-plan.json
```

The default check validates the placeholder template only. `production-network-tls-ready-check`
is reserved for real production evidence and must fail until domains,
certificates, proxy headers, firewall rules, service exposure and validation
checks are all READY. The current office-network test address
`100.99.133.43` is runtime smoke evidence, not production domain/TLS proof.
Its latest smoke is tracked in `metadata/test-environment-smoke.json`.

## Test Runtime Smoke

```bash
make smoke-test-environment ADP_SSH_HOST=100.99.133.43 ADP_BASE_URL=http://100.99.133.43:18080
```

Latest result: `metadata/test-environment-smoke.json`.

| Host | HTTP 18080 | HTTP 18070 | SSH | Core Containers | Status |
| --- | --- | --- | --- | --- | --- |
| `100.99.133.43` | PASS | PASS | PASS | nginx, gateway, PostgreSQL, Nacos, Keycloak, MinIO | PASS |

This check is intentionally a test-environment availability smoke. It does not
prove production DNS, public HTTPS, certificate renewal, firewall policy or
reverse-proxy header correctness.

## External Entry Points

| Component | Public Exposure | Domain | TLS | Notes |
| --- | --- | --- | --- | --- |
| Frontend / gateway | TBD | TBD | Required | Production user entry |
| Keycloak | TBD | TBD | Required | Only expose if required |
| MinIO console/API | TBD | TBD | Required | Prefer private network |
| Nacos | No by default | Internal only | Internal policy | Do not expose publicly |
| PostgreSQL | No by default | Internal only | Internal policy | Private network only |

## Required Evidence

| Evidence | Required content | Strict READY rule |
| --- | --- | --- |
| Domain inventory | Public base URL, DNS owner, TTL/cutover plan, Keycloak and MinIO exposure decision | `publicBaseUrl` must be HTTPS and every domain record must be READY |
| TLS certificate | Issuer, covered domains, storage path, renewal method, expiry monitoring and renewal evidence | At least one certificate record must be READY and include renewal evidence |
| Reverse proxy | Listener, upstreams, TLS termination, `Host`, `X-Forwarded-Proto`, `X-Forwarded-For`, `X-Real-IP`, HSTS decision | Every proxy record must be READY with evidence |
| Service exposure | Frontend/gateway, Keycloak, MinIO, Nacos and PostgreSQL boundary | Frontend/gateway must be public HTTPS on 443; Nacos/PostgreSQL must not be public without signed risk acceptance |
| Firewall/security group | Public HTTPS ingress and private admin/service rules | Every rule must be READY with export or equivalent evidence |
| Validation checks | TLS expiry, renewal dry-run, external reachability, forwarded headers, HSTS and external health/login smoke | Every check must be READY |

## Checklist

- Production domain names approved.
- TLS certificate source and renewal owner defined.
- Reverse proxy headers reviewed: `Host`, `X-Forwarded-Proto`, `X-Forwarded-For`.
- HSTS decision documented.
- Firewall or security group rules reviewed.
- Admin-only services are private or VPN-only.
- Port mapping differs from test ports where required.
- Certificate renewal dry-run completed.
- `make production-network-tls-ready-check NETWORK_TLS_PLAN=/secure/path/adp-network-tls-plan.json` passes on the real evidence file.
