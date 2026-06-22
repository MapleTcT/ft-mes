# 生产导出缺口明细

源报告：`metadata/production-export-readiness-smoke.json`
源报告时间：`2026-06-22T01:50:35.404Z`
状态：`BLOCKED`

本文件由 `scripts/generate-production-export-gap-breakdown.py` 从生产导出 readiness smoke 生成。它只拆解 `PROD-023` 的逐目标缺口，不把任何导出功能标记为已完成。

## 摘要

| 指标 | 数量 |
| --- | ---: |
| 目标页面 | 6 |
| 页面可达 | 6 |
| 可见导出动作 | 0 |
| runtime 导出动作 | 0 |
| 非空下载响应 | 1 |
| 后端 query export workbook | 1 |
| 后端 query export 错误 | 4 |
| 已证明列表数据导出 | 0 |

## 逐目标缺口

| 目标 | 状态 | 页面 | 可见导出 | Runtime 导出 | 下载 | Query Export | 源码分类 | 修复分类 | 当前缺口 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| wom-make-task | BLOCKED | YES | NO | NO | 200 / 0 bytes / EMPTY | 500 / 52 bytes / JSON | DOWNLOADXLS_IMPORT_TEMPLATE_OR_EMPTY_ENDPOINT_ONLY | FIX_BACKEND_QUERY_EXPORT_ERROR_AND_EXPOSE_FRONTEND_ACTION | visibleExportAction missing from the real browser page.<br>runtimeExportAction missing from layoutJson metadata.<br>targetExportSourceHook missing; current sourceAudit.classification=DOWNLOADXLS_IMPORT_TEMPLATE_OR_EMPTY_ENDPOINT_ONLY.<br>downloadXls returned an empty response body.<br>query export endpoint /msService/WOM/produceTask/produceTask/makeTaskList-query did not return 2xx; status=500. |
| rm-batch-formula | BLOCKED | YES | NO | NO | 200 / 8704 bytes / OLE_XLS | 200 / 205 bytes / JSON | DOWNLOADXLS_IMPORT_TEMPLATE_OR_EMPTY_ENDPOINT_ONLY | ENABLE_BACKEND_QUERY_EXPORT_FILE_MODE_AND_FRONTEND_ACTION | visibleExportAction missing from the real browser page.<br>runtimeExportAction missing from layoutJson metadata.<br>targetExportSourceHook missing; current sourceAudit.classification=DOWNLOADXLS_IMPORT_TEMPLATE_OR_EMPTY_ENDPOINT_ONLY.<br>downloadXls returned a non-empty file, but browser/runtime/source evidence does not prove list-data export.<br>query export endpoint /msService/RM/formula/formula/batchFormulaList-query did not return a workbook; magic=JSON. |
| wts-work-permit | ACTION_REQUIRED | YES | NO | NO | 200 / 0 bytes / EMPTY | 200 / 8704 bytes / OLE_XLS | DOWNLOADXLS_IMPORT_TEMPLATE_OR_EMPTY_ENDPOINT_ONLY | ENABLE_TARGET_EXPORT_ACTION_AND_LIST_DATA_FILE | visibleExportAction missing from the real browser page.<br>runtimeExportAction missing from layoutJson metadata.<br>targetExportSourceHook missing; current sourceAudit.classification=DOWNLOADXLS_IMPORT_TEMPLATE_OR_EMPTY_ENDPOINT_ONLY.<br>downloadXls returned an empty response body. |
| qcs-inspect-report | BLOCKED | YES | NO | NO | 200 / 0 bytes / EMPTY | 500 / 52 bytes / JSON | NO_TARGET_EXPORT_SOURCE_FOUND | FIX_BACKEND_QUERY_EXPORT_ERROR_AND_EXPOSE_FRONTEND_ACTION | visibleExportAction missing from the real browser page.<br>runtimeExportAction missing from layoutJson metadata.<br>targetExportSourceHook missing; current sourceAudit.classification=NO_TARGET_EXPORT_SOURCE_FOUND.<br>downloadXls returned an empty response body.<br>query export endpoint /msService/QCS/inspectReport/inspectReport/manuInspReportList-query did not return 2xx; status=500. |
| qcs-unqualified-deal | BLOCKED | YES | NO | NO | 200 / 0 bytes / EMPTY | 500 / 52 bytes / JSON | NO_TARGET_EXPORT_SOURCE_FOUND | FIX_BACKEND_QUERY_EXPORT_ERROR_AND_EXPOSE_FRONTEND_ACTION | visibleExportAction missing from the real browser page.<br>runtimeExportAction missing from layoutJson metadata.<br>targetExportSourceHook missing; current sourceAudit.classification=NO_TARGET_EXPORT_SOURCE_FOUND.<br>downloadXls returned an empty response body.<br>query export endpoint /msService/QCS/unQlfDeal/unQlfDeal/manuUnQlfDealList-query did not return 2xx; status=500. |
| qcs-inspect-release | BLOCKED | YES | NO | NO | 200 / 0 bytes / EMPTY | 500 / 52 bytes / JSON | NO_TARGET_EXPORT_SOURCE_FOUND | FIX_BACKEND_QUERY_EXPORT_ERROR_AND_EXPOSE_FRONTEND_ACTION | visibleExportAction missing from the real browser page.<br>runtimeExportAction missing from layoutJson metadata.<br>targetExportSourceHook missing; current sourceAudit.classification=NO_TARGET_EXPORT_SOURCE_FOUND.<br>downloadXls returned an empty response body.<br>query export endpoint /msService/QCS/inspectRelease/inspectRelease/manuInspReleaseList-query did not return 2xx; status=500. |

## 验收口径

- 文件导出本身不应该写业务表，落库状态为 `NOT_APPLICABLE`。
- PASS 必须同时证明：真实页面有可见导出动作、layoutJson 有 runtime 导出动作、源码或 runtime 有目标导出 hook、后端返回非空 XLS/XLSX 列表数据文件。
- `downloadXls` 的导入模板、`*-query exportFlag=true` 返回 JSON/500、空文件、HTTP 200、平台通用 export 模板都不能单独证明生产列表导出可用。

## 复验命令

```bash
make smoke-production-export-readiness ADP_BASE_URL=http://100.99.133.43:18080 ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 PRODUCTION_EXPORT_SMOKE_OUTPUT=metadata/production-export-readiness-smoke.json ADP_PAGE_TIMEOUT_MS=240000 ADP_API_TIMEOUT_MS=30000
make production-export-gap-breakdown
make production-export-gap-breakdown-check
```
