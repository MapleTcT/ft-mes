# Batch Intelligence Adapter

This Java 8 service is the only browser-facing backend for the Java 17 BPI service. It validates a
Keycloak JWT through JWKS or verifies the legacy opaque `suposTicket` through the trusted internal ADP
gateway. It maps legacy roles and server-owned plant/line scopes, signs a short-lived internal BPI
token, and proxies only the Phase 1 allowlist under `/bpi-api`.

The adapter is deliberately stateless. It owns no BPI database tables and accepts no client-provided
tenant, plant, line, upstream URL, or internal token claims. Missing role or subject-scope mappings fail
closed with HTTP 403.

The allowlist includes point-catalog reads, the admin-only snapshot import, and point-calibration
submission/list/approve/reject/revoke commands. It also exposes the scoped data-quality incident list,
summary and detail reads plus acknowledge/reassign and resolve commands; the Java 17 service remains
authoritative for RBAC, revision, idempotency and lifecycle validation. Calibration certificates are referenced by controlled URI
and SHA-256; binary certificate uploads do not pass through this adapter. Ordinary request bodies
remain capped at 64 KiB; only `POST /bpi-api/point-catalog/snapshots` has a 5 MiB cap for immutable
catalog payloads. The Java 17 service still enforces the `BPI_ADMIN` role, tenant/plant/line scope,
idempotency key, and `If-Match: 0`. The adapter does not read or write the JetLinks database.

The same explicit allowlist exposes the shadow-run acceptance lifecycle: list/detail, create, start,
batch review, complete, independent approve/reject, and cancel. These routes remain shadow-only; the
adapter cannot add arbitrary BPI paths and the Java 17 service never writes WOM, QCS, WMS, PLC, or DCS.

Required configuration:

- `BPI_ADAPTER_KEYCLOAK_JWK_SET_URI`
- `BPI_ADAPTER_KEYCLOAK_ISSUER`
- `BPI_ADAPTER_KEYCLOAK_AUDIENCE`
- `BPI_ADAPTER_LEGACY_GATEWAY_BASE_URL`
- `BPI_ADAPTER_INTERNAL_JWT_SECRET` (at least 32 bytes)
- explicit `bpi.adapter.role-mappings` and `bpi.adapter.subject-scopes`

Run its focused tests with:

```bash
mvn -pl backend/source-modules/batch-intelligence-adapter -am test
```
