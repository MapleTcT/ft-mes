# ProcessAnalysis Dependency Analysis

Generated at: 2026-06-20

Environment: `100.99.133.43` / `v6-2288H-V6`

Database: PostgreSQL

## Conclusion

WOM traceability cannot be accepted in the current test environment because the
called `ProcessAnalysis` tenant service is not deployed or registered.

This is not a WOM button issue. The WOM manufacturing task list exposes the
`prodprocessView` action, but that action delegates batch/material/work-order
traceability to a separate `ProcessAnalysis` module that is absent from the
current package/runtime set.

## Source Dependency

| Caller | Source path | Called service/API |
| --- | --- | --- |
| WOM manufacturing task list trace button | `modules/wom/WOM_6.1.3.1/service/src/main/resources/custom/WOM/produceTask/produceTask/makeTaskList/eventJs/customEvent.js` | `ProcessAnalysis` `/analysisParam/analysisParam/isProdprocessView` and `/processAnalysis/exelogSecond/processBatchViewOut` |
| WOM manufacturing task graph trace button | `modules/wom/WOM_6.1.3.1/service/src/main/resources/custom/WOM/produceTask/produceTask/makeTaskGraphList/eventJs/customEvent.js` | `ProcessAnalysis` `/analysisParam/analysisParam/isProdprocessView` and `/processAnalysis/exelogSecond/processBatchViewOut` |
| WOM process execution log manual statistics | `modules/wom/WOM_6.1.3.1/service/src/main/resources/custom/WOM/produceTask/processExelog/processExeLogList/eventJs/customEvent.js` | `ProcessAnalysis` `/paramStatRec/paramStatRec/manualStatProcess` and `/produceTask/paPrExeLog/paPrExeLogList-query` |
| WOM activity execution log manual statistics | `modules/wom/WOM_6.1.3.1/service/src/main/resources/custom/WOM/produceTask/actiExelog/activeExeLogList/eventJs/customEvent.js` | `ProcessAnalysis` `/paramStatRec/paramStatRec/manualStatActive` and `/produceTask/paActiExeLog/paActiExeLogList-query` |
| WOM task execution parameter analysis | `modules/wom/WOM_6.1.3.1/service/src/main/resources/custom/WOM/produceTask/prodTaskExelog/makeTaskExecuList/eventJs/customEvent.js` | `ProcessAnalysis` `/paramDetail/paramDetail/analysisiTask` |

The same caller-side references exist in recovered WOM `6.1.2.3`, `6.1.3.3`,
and `6.1.3.4` sources. The current recovered source tree contains the WOM
callers, but no matching `ProcessAnalysis` Java controller/service package.

## Runtime Evidence

Remote container inventory on `100.99.133.43` includes `WOMMs` and Nacos, but no
`ProcessAnalysis` or traceability service container.

Nacos `prod` group checks from inside `adp-mes-newbase-nacos-1`:

| Service name | Result |
| --- | --- |
| `ProcessAnalysis` | `hosts=[]` |
| `processanalysis` | `hosts=[]` |
| `PROCESSANALYSIS` | `hosts=[]` |
| `Traceability` | `hosts=[]` |
| `traceability` | `hosts=[]` |
| `WOMMs` | healthy instance at `172.25.0.55:8080` |

## Endpoint Evidence

Authenticated login to `http://100.99.133.43:18080` as `admin` succeeded using
the repository Playwright request flow. The token was used only for verification
and was not recorded.

| Method | Endpoint | Status | Result |
| --- | --- | --- | --- |
| `GET` | `/msService/ProcessAnalysis/analysisParam/analysisParam/isProdprocessView?batchNo=ADP_E2E_20260618_UNQLF_AUTO_DEAL_3_BATCH` | `503` | `can not find any tenant app service` |
| `GET` | `/msService/ProcessAnalysis/processAnalysis/exelogSecond/processBatchViewOut?...` | `503` | `can not find any tenant app service` |
| `GET` | `/msService/ProcessAnalysis/paramDetail/paramDetail/analysisiTask` | `503` | `can not find any tenant app service` |
| `GET` | `/msService/ProcessAnalysis/paramStatRec/paramStatRec/manualStatActive?activeId=1` | `503` | `can not find any tenant app service` |
| `GET` | `/msService/ProcessAnalysis/paramStatRec/paramStatRec/manualStatProcess?processId=1` | `503` | `can not find any tenant app service` |

## Re-runnable Smoke

The dependency can be rechecked without writing business data:

```bash
make smoke-business-dependencies \
  BUSINESS_DEPENDENCY_SMOKE_OUTPUT=/tmp/adp-business-dependency-readiness-smoke.json
```

The current committed report is
`metadata/business-dependency-readiness-smoke.json`; it was refreshed against
`100.99.133.43` at `2026-06-21T13:46:42.816Z` and records `process-analysis` as
`BLOCKED`: all ProcessAnalysis/traceability Nacos aliases have
`healthyHostCount=0`, five authenticated dependency endpoints return
tenant-service `503`, and PostgreSQL `processAnalysisTableCount`,
`processAnalysisRuntimeViewCount`, and `processAnalysisMenuCount` are all `0`.

`make business-dependency-readiness-check` now rejects a `READY`
ProcessAnalysis dependency unless the expected Nacos service is healthy, all
traceability endpoints return HTTP 2xx, and PostgreSQL table, runtime_view, and
menu evidence are non-zero.

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
`process-analysis` as `BLOCKED_NO_IMPLEMENTATION_CANDIDATE`: no
ProcessAnalysis/traceability implementation candidate was found in the bounded
package scan. This does not replace runtime proof after a new package arrives;
it only keeps the current missing-package conclusion reproducible.

## Database Evidence

Current PostgreSQL ProcessAnalysis-like table scan:

```sql
select table_name
from information_schema.tables
where table_schema = 'public'
  and (
    table_name ~* '^pa_'
    or table_name ~* 'process.*analysis'
    or table_name ~* 'trace'
    or table_name ~* 'process_batch'
  )
order by table_name;
```

Result:

```text
0 rows
```

Runtime metadata scan:

```sql
select count(*)
from public.runtime_view
where code ilike '%ProcessAnalysis%'
   or url ilike '%ProcessAnalysis%'
   or code ilike '%trace%'
   or url ilike '%trace%';

select count(*)
from public.rbac_menuinfo
where code ilike '%ProcessAnalysis%'
   or url ilike '%ProcessAnalysis%'
   or code ilike '%trace%'
   or url ilike '%trace%';
```

Result:

```text
runtime_view: 0
rbac_menuinfo: 0
```

## Package Evidence

The current source scan found only caller-side WOM event scripts that reference
`ProcessAnalysis`. It did not find an implementation for:

- `/ProcessAnalysis/analysisParam/analysisParam/isProdprocessView`
- `/ProcessAnalysis/processAnalysis/exelogSecond/processBatchViewOut`
- `/ProcessAnalysis/paramDetail/paramDetail/analysisiTask`
- `/ProcessAnalysis/paramStatRec/paramStatRec/manualStatActive`
- `/ProcessAnalysis/paramStatRec/paramStatRec/manualStatProcess`
- `/ProcessAnalysis/produceTask/paActiExeLog/paActiExeLogList-query`
- `/ProcessAnalysis/produceTask/paPrExeLog/paPrExeLogList-query`

The local filename scan under `/Users/zhangchu/Documents/MES包`,
`/Users/zhangchu/Downloads/ADP`, and `/Users/zhangchu/Documents/ADP` also found
no `ProcessAnalysis`, `process analysis`, or traceability module package.

## Acceptance Impact

`PROD-020` remains `BLOCKED`. The accepted WOM behavior still stands:

1. The manufacturing task list button is visible.
2. The WOM caller route and JavaScript event function are identified.
3. The target module is missing from service registration, runtime metadata, and
   PostgreSQL schema.

The blocker is the missing `ProcessAnalysis` business module package/service,
not a PostgreSQL compatibility patch that can be completed inside WOM alone.

## Next Acceptance Steps

When a later business package arrives:

1. Identify the package that provides `serviceName=ProcessAnalysis`.
2. Deploy it to the test composition and confirm Nacos `prod@@ProcessAnalysis`
   contains at least one healthy host.
3. Apply or recover the module runtime views, menu entries, permissions, and
   PostgreSQL tables.
4. Re-run the authenticated `isProdprocessView` precheck with a marker batch.
5. Open `processBatchViewOut` from the real WOM button or equivalent browser
   context.
6. Capture the target controller/service/DAO/SQL path.
7. Query the `ProcessAnalysis` target tables directly in PostgreSQL before
   changing `PROD-020` from `BLOCKED` to `PASS`.
