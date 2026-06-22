# Production Network, Domain And TLS Evidence

This folder contains the machine-readable production network/TLS evidence
template and validator. It exists because the Docker test profile currently
uses test ports and HTTP defaults; production cutover needs separate proof for
domains, TLS certificates, reverse proxies, service exposure and firewall
rules.

## Files

| File | Purpose |
| --- | --- |
| `network-tls-plan.example.json` | Placeholder-only production network/TLS plan template. |
| `scripts/validate-network-tls-plan.py` | Validates service exposure, TLS readiness, firewall coverage and secret hygiene. |

## Commands

```bash
make production-network-tls-check
make production-network-tls-check NETWORK_TLS_PLAN=/secure/path/adp-network-tls-plan.json
make production-network-tls-ready-check NETWORK_TLS_PLAN=/secure/path/adp-network-tls-plan.json
```

The default check validates the example template shape. `production-network-tls-ready-check`
is for a real production evidence file and fails unless domains, certificates,
reverse proxies, service exposure, firewall rules and validation checks are all
READY.
READY network/TLS evidence must reference real production or rehearsal records.
Paths containing `.example`, `example.`, `-example`, `_example`, `template` or
`sample` are accepted only for placeholder/template checks and are rejected as
READY evidence.

## Production Rule

The test host and ports are not production network proof. Production must
record the real public base URL, certificate issuer and renewal evidence,
reverse-proxy headers, HSTS decision, service exposure boundary and firewall or
security-group evidence. Real private keys, passwords, tokens and certificate
bundles must stay outside git.
