# ProcessAnalysis 追溯模块恢复与验收

验收时间：2026-07-10

环境：`100.99.133.43` / `v6-2288H-V6`

数据库：PostgreSQL

## 结论

WOM 的“生产过程追溯”入口已经从缺包阻断恢复为可维护源码模块，`PROD-020` 验收状态为 `PASS`。真实浏览器从制造任务列表选择后点击 `prodprocessView`，经过兼容预检打开 ProcessAnalysis 批次追溯页；页面、API、Nacos、PostgreSQL 快照落库和清理均有 marker 证据。

机器可读证据：

- `metadata/process-analysis-persistence-acceptance.json`
- `metadata/process-analysis-trace.png`
- `metadata/business-dependency-readiness-smoke.json`
- `metadata/business-dependency-package-scan.json`

## 源码与启动边界

| 层次 | 路径/说明 |
| --- | --- |
| Maven 模块 | `backend/source-modules/process-analysis` |
| 父级聚合 | `backend/source-modules/pom.xml` |
| Docker 编排 | `deploy/docker/docker-compose.yml` 中 `ProcessAnalysis` 服务 |
| PostgreSQL 迁移 | `deploy/docker/postgres/init/175-process-analysis-traceability.sql` |
| WOM 入口 | `deploy/docker/assets/module-static/WOM/produceTask/produceTask/makeTaskList.html` |
| 验收脚本 | `deploy/docker/scripts/adp-process-analysis-persistence-acceptance.js` |

服务保持 PostgreSQL 默认运行路径，Oracle 仅保留 legacy template，不参与本模块默认启动。

## 兼容接口

模块保持旧 WOM 调用契约及网关 `/ProcessAnalysis` 前缀：

| Method | Endpoint | 验收结果 |
| --- | --- | --- |
| `GET` | `/analysisParam/analysisParam/isProdprocessView` | HTTP 200，真实批次返回可追溯 |
| `GET` | `/processAnalysis/exelogSecond/processBatchViewOut` | HTTP 200，渲染批次追溯页 |
| `GET` | `/processAnalysis/api/trace` | HTTP 200，返回工单、工序、物料、质量、WMS 时间轴 |
| `GET` | `/paramDetail/paramDetail/analysisiTask` | HTTP 200；缺少参数时返回兼容失败体，不抛空指针 |
| `GET` | `/paramStatRec/paramStatRec/manualStatProcess` | HTTP 200，幂等写入工序快照 |
| `GET` | `/paramStatRec/paramStatRec/manualStatActive` | HTTP 200，幂等写入活动快照 |
| `GET` | `/produceTask/paPrExeLog/paPrExeLogList-query` | HTTP 200，查询工序执行记录 |
| `GET` | `/produceTask/paActiExeLog/paActiExeLogList-query` | HTTP 200，查询活动执行记录 |

`metadata/business-dependency-readiness-smoke.json` 当前状态为 `READY`：两个维护依赖均可用，ProcessAnalysis 在 Nacos `prod` 组有健康实例，五个兼容探针均为 HTTP 2xx。

## PostgreSQL 落库验收

验收 marker：`ADP_E2E_20260710084011_PROCESS_ANALYSIS`

目标表：`pa_trace_snapshots`。同一业务键重复执行采用 revision 更新，不制造重复快照。本轮实际状态：

| 阶段 | 快照数 | task revision | process revision | activity revision | WMS 单据/流水 |
| --- | ---: | ---: | ---: | ---: | ---: |
| 操作前 | 0 | 0 | 0 | 0 | 0 / 0 |
| 执行统计后 | 3 | 2 | 1 | 1 | 1 / 2 |
| 清理后 | 0 | 0 | 0 | 0 | 0 / 0 |

验收 SQL：

```sql
select snapshot_type, source_id, batch_no, revision
from public.pa_trace_snapshots
where marker = 'ADP_E2E_20260710084011_PROCESS_ANALYSIS'
order by snapshot_type;

select count(*)
from public.runtime_view
where code = 'ProcessAnalysis_1.0.0_processAnalysis_processBatchViewOut';

select count(*)
from public.rbac_menuinfo
where code = 'ProcessAnalysis_1.0.0_processAnalysis_processBatchViewOut';
```

迁移、运行视图和菜单记录均已应用，运行视图与菜单计数均为正数。验收脚本最后删除 marker 产生的追溯快照和 WMS 测试数据，清理后计数为 0。

## 浏览器证据

真实页面入口为 `/msService/WOM/produceTask/produceTask/makeTaskList`。脚本把真实查询响应交给 `SupDataGrid.setDatagridData`，选中任务 `8991075113025740` 后点击实际 `#btn-prodprocessView`，弹窗打开：

`/msService/ProcessAnalysis/processAnalysis/exelogSecond/processBatchViewOut?batchNo=ADP_E2E_20260710023850_WOM_CHECKOUTBILL_BATCH&productNo=ADP_E2E_20260710023850_WOM_CHECKOUTBILL_MAT`

页面显示 10 个时间轴事件，浏览器 console error、失败 response 和 request failure 均为 0。截图见 `metadata/process-analysis-trace.png`。

## 后端链路

`ProcessAnalysisController / AnalysisParamController / ParamStatController` 调用 `TraceabilityService`，由 `TraceabilityRepository` 使用 Spring JDBC 查询 WOM、QCS、WMS 与基础物料表，并写入 `pa_trace_snapshots`。查询按产品、批次和 tenant 约束，质量明细来自 `qcs_report_coms`，WMS 只读取当前 tenant 数据。

## 复验命令

```bash
make business-package-scan
make process-analysis-test
make process-analysis-package
make acceptance-process-analysis-persistence
make smoke-business-dependencies
make business-dependency-readiness-check
```

历史上未部署时，网关曾返回 tenant-service `503`；该状态只作为恢复背景保留，不再代表当前环境。
