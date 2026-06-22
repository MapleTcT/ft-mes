# Material Service Dependency Analysis

Generated at: 2026-06-20

Environment: `100.99.133.43` / `v6-2288H-V6`

Database: PostgreSQL

## Conclusion

WOM/QCS stock-in and inventory write-back cannot be accepted in the current test
environment because the called `material` tenant service is not deployed or
registered.

This is not a frontend permission issue and not a PostgreSQL mapper issue. The
current package contains WOM/QCS/LIMS/BaseSet material master-data surfaces, but
the runtime calls point to a separate `material` warehouse/inventory service.

## Source Dependency

| Caller | Source path | Called service/API |
| --- | --- | --- |
| QCS result backfill to warehouse | `modules/wom/WOM_6.1.3.3/service/src/main/java/com/supcon/orchid/WOM/services/impl/WOMQCSServiceImpl.java` | `material` `/material/foreign/foreign/checkProdResult` |
| WOM put-in material out single | `modules/wom/WOM_6.1.3.3/service/src/main/java/com/supcon/orchid/WOM/services/impl/WOMPutInMaterialServiceImpl.java` | `material` `/public/material/produceOutSingle/produceOutSing/generateProduceOutSing` |
| WOM material consumption out single | `modules/wom/WOM_6.1.3.3/service/src/main/java/com/supcon/orchid/WOM/services/impl/WOMMatConsumRecodServiceImpl.java` | `material` `/public/material/produceOutSingle/produceOutSing/generateProduceOutSing` |

The same call pattern exists in the recovered WOM `6.1.2.3`, `6.1.3.1`, and
`6.1.3.4` sources.

## Runtime Evidence

Remote container inventory on `100.99.133.43` includes platform services,
`WOMMs`, `RMMs`, `QCS`, `LIMS`, `LIMSMaterial`, `EamMs`, `ToolMs`, `WTSs`,
`WAPS`, and other base services. It does not include a `material`, `wms`,
`warehouse`, `stock`, or `inventory` service container.

Nacos `prod` group checks from inside `adp-mes-newbase-nacos-1`:

| Service name | Result |
| --- | --- |
| `material` | `hosts=[]` |
| `MATERIAL` | `hosts=[]` |
| `wms` / `WMS` | `hosts=[]` |
| `warehouse` / `Warehouse` | `hosts=[]` |
| `inventory` / `Inventory` | `hosts=[]` |
| `WOMMs` | healthy instance at `172.25.0.55:8080` |
| `QCS` | healthy instance at `172.25.0.53:8080` |
| `RMMs` | healthy instance at `172.25.0.51:8080` |
| `LIMS` | healthy instance at `172.25.0.53:8080` |

The Nacos service list contains `LIMSMaterial`, but no `material` service:

```text
LIMSSample, LIMSInterface, LIMSMatStds, WOM, LIMSDC, LIMSRetain,
LIMSMaterial, RMMs, QCS, WOMMs, LIMSDCMan, LIMSINT, LIMSBasic,
LIMS, LIMSSteady, LIMSSTDS
```

## Endpoint Evidence

Authenticated login to `http://100.99.133.43:18080` as `admin` succeeded. The
token was used only for verification and was not recorded.

| Method | Endpoint | Status | Result |
| --- | --- | --- | --- |
| `POST` | `/msService/material/foreign/foreign/checkProdResult?srcId=1&checkResult=1` | `503` | `can not find any tenant app service` |
| `POST` | `/msService/public/material/produceOutSingle/produceOutSing/generateProduceOutSing` | `503` | `can not find any tenant app service` |
| `POST` | `/msService/BasicMs/material/foreign/foreign/checkProdResult?srcId=1&checkResult=1` | `404` | `No message available` |
| `POST` | `/msService/BaseSet/material/foreign/foreign/checkProdResult?srcId=1&checkResult=1` | `404` | `No message available` |
| `POST` | `/msService/LIMSMaterial/material/foreign/foreign/checkProdResult?srcId=1&checkResult=1` | `404` | `No message available` |
| `POST` | `/msService/LIMSMaterial/public/material/produceOutSingle/produceOutSing/generateProduceOutSing` | `404` | `No message available` |

The direct unauthenticated public endpoint also returns `503`, which matches the
authenticated result after the gateway resolves the target service.

## Re-runnable Smoke

The dependency can be rechecked without writing business data:

```bash
make smoke-business-dependencies \
  BUSINESS_DEPENDENCY_SMOKE_OUTPUT=/tmp/adp-business-dependency-readiness-smoke.json
```

The current committed report is
`metadata/business-dependency-readiness-smoke.json`; it was refreshed against
`100.99.133.43` at `2026-06-21T13:46:42.816Z` and records `material-service` as
`BLOCKED`: all material/WMS/warehouse/inventory Nacos aliases have
`healthyHostCount=0`, and the authenticated material endpoints still return
tenant-service `503`.

`make business-dependency-readiness-check` now rejects a `READY` dependency
unless the expected Nacos service is healthy, all dependency endpoints return
HTTP 2xx, and the report keeps database evidence for the target material/stock
tables.

Nacos aliases have `healthyHostCount=0` for every material/WMS/warehouse/inventory
service alias checked by the readiness smoke.

The local package/source candidate scan is also repeatable:

```bash
make business-package-scan \
  BUSINESS_PACKAGE_SCAN_OUTPUT=metadata/business-dependency-package-scan.json
```

The current report `metadata/business-dependency-package-scan.json` scans
`/Users/zhangchu/Documents/MES包` plus ADP base-Server/config/static roots in
read-only mode. The latest scan visited `58889` files, scanned `1142` outer
archives, `2139` first-party/dependency-relevant nested archives, and `429984`
archive entries; archive entry/text cap skips are `0`. It records
`material-service` as `BLOCKED_NO_IMPLEMENTATION_CANDIDATE`: no
`checkProdResult`, `generateProduceOutSing`, `produceOutSingle`, or material
service module candidate was found in the bounded package scan. This does not
replace runtime proof after a new package arrives; it only keeps the current
missing-package conclusion reproducible.

## Database Evidence

Current PostgreSQL material-like table scan:

```sql
select tablename
from pg_tables
where schemaname = 'public'
  and (
    lower(tablename) like '%material%'
    or lower(tablename) like '%stock%'
    or lower(tablename) like '%warehouse%'
    or lower(tablename) like '%inventory%'
    or lower(tablename) like '%produce_out%'
    or lower(tablename) like '%out_single%'
  )
order by tablename;
```

Representative result:

```text
baseset_material_attributes
baseset_material_classes
baseset_material_stocks
baseset_material_ware_lists
baseset_material_ware_sets
baseset_materials
baseset_warehouse_classes
baseset_warehouses
hm_material_menage
hm_material_saves
hm_stock_menage
hm_stock_menage_di
wom_output_materials
wom_put_in_materials
wom_reject_materials
wom_remain_materials
wom_task_materials
```

These are foundation material/warehouse master-data, HierarchicalMod stock
records, and WOM internal material documents. They are not a verified target
schema for the called `material` service stock-in or production-out-single
transactions.

## Package Evidence

The local package/module scan found:

- `LIMSMaterial_6.1.3.5`
- BaseSet material and warehouse master-data files
- `SpareManage_6.1.9.3`
- WOM material document code such as `WOMPutInMaterial`, `WOMOutputMaterial`,
  `WOMRejectMaterial`, and `WOMRemainMaterial`

It did not find an implementation for:

- `/material/foreign/foreign/checkProdResult`
- `/material/produceOutSingle/produceOutSing/generateProduceOutSing`
- `/public/material/produceOutSingle/produceOutSing/generateProduceOutSing`

## Acceptance Impact

`PROD-019`, `PROD-021`, and `PROD-022` should remain blocked only for the
inventory/stock-in part. The accepted QCS/WOM behavior still stands:

1. Manufacturing inspection request generation.
2. QCS report generation and report result persistence.
3. Qualified and unqualified result backfill to WOM task, wait record, execution
   log, and batch availability.
4. Automatic unqualified treatment document creation and treatment backfill.
5. WOM report-work and reject-material main flows already covered by their own
   marker evidence.

## Next Acceptance Steps

When a later business package arrives:

1. Identify the package that provides `serviceName=material`.
2. Deploy it to the test composition and confirm Nacos `prod@@material`
   contains at least one healthy host.
3. Re-run authenticated calls for `checkProdResult` and
   `generateProduceOutSing` with marker data.
4. Capture the target controller/service/DAO/SQL path.
5. Query the material stock-in, inventory, production-out-single, and sync-log
   tables directly in PostgreSQL.
6. Update `PROD-019`, `PROD-021`, `PROD-022`, and
   `metadata/persistence-acceptance.json` from `BLOCKED` to `PASS` only after
   the database writes are proven.
