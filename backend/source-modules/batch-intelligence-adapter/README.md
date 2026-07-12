# Batch Intelligence Adapter

This Java 8 service is the only browser-facing backend for the Java 17 BPI service. It validates the
existing MES Keycloak token through JWKS, maps legacy roles and server-owned plant/line scopes, signs a
short-lived internal BPI token, and proxies only the Phase 1 allowlist under `/bpi-api`.

The adapter is deliberately stateless. It owns no BPI database tables and accepts no client-provided
tenant, plant, line, upstream URL, or internal token claims. Missing role or subject-scope mappings fail
closed with HTTP 403.

Required configuration:

- `BPI_ADAPTER_KEYCLOAK_JWK_SET_URI`
- `BPI_ADAPTER_KEYCLOAK_ISSUER`
- `BPI_ADAPTER_KEYCLOAK_AUDIENCE`
- `BPI_ADAPTER_INTERNAL_JWT_SECRET` (at least 32 bytes)
- explicit `bpi.adapter.role-mappings` and `bpi.adapter.subject-scopes`

Run its focused tests with:

```bash
mvn -pl backend/source-modules/batch-intelligence-adapter -am test
```
