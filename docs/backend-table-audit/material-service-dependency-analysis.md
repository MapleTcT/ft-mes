# Material/WMS Service Acceptance

Generated at: 2026-07-10

Environment: `100.99.133.43` / `v6-2288H-V6`

Database: PostgreSQL

## Conclusion

The former `material` service blocker is resolved. The maintained source module is
`backend/source-modules/material-wms`; it is deployed as Nacos `prod@@material`
and provides the three WOM/QCS compatibility contracts required by the current
production and quality flow.

The dependency contract ID is `material-service`.

This acceptance does not change the default datasource to Oracle. It also does
not invent a standalone bad-quantity feature: that separate product decision is
still tracked by `metadata/wom-bad-quantity-analysis.json`.

## Compatibility Contracts

| Caller/action | Gateway endpoint | Result |
|---|---|---|
| WOM completion inbound | `POST /msService/public/material/produceInSingles/produceInSingl/generateProductInSingle` | PASS |
| QCS quality result | `POST /msService/material/foreign/foreign/checkProdResult` | PASS |
| WOM production issue | `POST /msService/public/material/produceOutSingle/produceOutSing/generateProduceOutSing` | PASS |

The backend chain is `MaterialWmsController -> MaterialInventoryService ->
MaterialWmsRepository -> PostgreSQL JDBC SQL`.

## Runtime Evidence

- Nacos group: `prod`
- Service: `material`
- Healthy instance: `172.25.0.60:8080`
- Runtime JAR SHA-256: `9095731dcd50cde45337ffa67ef36f2b31b791da869866d1a64712acb163b8c9`
- All three gateway probes return HTTP 2xx and none returns tenant-service `503`.
- Browser route `/msService/material/wms` returns HTTP 200 and renders marker details
  without console errors, request failures, or bad responses.

The read-only dependency report is
`metadata/business-dependency-readiness-smoke.json`. It keeps the material item
as `ACTION_REQUIRED` because that smoke deliberately does not write business
data; the marker acceptance below is the final persistence evidence.

## PostgreSQL Evidence

Target tables:

- `wms_stock_documents`
- `wms_stock_document_lines`
- `wms_batch_stocks`
- `wms_inventory_transactions`
- `wms_quality_results`

Marker `ADP_E2E_20260710074612_MATERIAL_WMS` proved:

1. Completion inbound: one document/line/transaction, `on_hand=10`,
   `hold=10`, quality `PENDING`.
2. Idempotent retry: counts and quantities unchanged.
3. Qualified callback: `available=10`, `hold=0`, quality `QUALIFIED`.
4. Production issue of 3: `on_hand=7`, `available=7`.
5. Cleanup: all marker document, line, transaction, quality and stock rows return
   to zero.

Machine-readable requests, responses, SQL and state snapshots are in
`metadata/material-wms-persistence-acceptance.json`. Browser evidence is
`metadata/material-wms-completion-inbound.png`.

## Recheck

```bash
make material-wms-test
make business-package-scan \
  BUSINESS_PACKAGE_SCAN_OUTPUT=metadata/business-dependency-package-scan.json
make smoke-business-dependencies
make acceptance-material-wms-persistence \
  ADP_BASE_URL=http://100.99.133.43:18080
```

Deployment and rollback steps are documented in
`docs/production-migration/material-wms-deployment-runbook.md`.
