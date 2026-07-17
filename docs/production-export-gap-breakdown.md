# 生产导出缺口明细

源报告：`metadata/production-export-readiness-smoke.json`
源报告时间：`2026-07-17T04:53:37.777Z`
状态：`READY`

本文件由 `scripts/generate-production-export-gap-breakdown.py` 从生产导出 readiness smoke 生成。它拆解 `PROD-023` 的逐目标缺口，并允许单个目标在真实浏览器点击和文件响应闭环后先行 READY；只要仍有未完成目标，PROD-023 总体仍保持 BLOCKED。

## 摘要

| 指标 | 数量 |
| --- | ---: |
| 目标页面 | 6 |
| 页面可达 | 6 |
| 可见导出动作 | 6 |
| 浏览器点击导出工作簿 | 6 |
| runtime 导出动作 | 6 |
| 非空下载响应 | 1 |
| 后端 query export workbook | 6 |
| 后端 query export 错误 | 0 |
| 已证明列表数据导出 | 6 |

## 逐目标缺口

| 目标 | 状态 | 页面 | 可见导出 | 浏览器点击 | Runtime 导出 | 下载 | Query Export | 源码分类 | 修复分类 | 当前缺口 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| wom-make-task | READY | YES | YES | FILE_VERIFIED / 5632 bytes / OLE_XLS | YES | 200 / 0 bytes / EMPTY | 200 / 5632 bytes / OLE_XLS | TARGET_EXPORT_HOOK_FOUND_NEEDS_BROWSER_FILE_PROOF | READY_NO_FIX_REQUIRED | - |
| rm-batch-formula | READY | YES | YES | FILE_VERIFIED / 12269 bytes / ZIP_XLSX | YES | 200 / 8704 bytes / OLE_XLS | 200 / 12270 bytes / ZIP_XLSX | TARGET_EXPORT_HOOK_FOUND_NEEDS_BROWSER_FILE_PROOF | READY_NO_FIX_REQUIRED | - |
| wts-work-permit | READY | YES | YES | FILE_VERIFIED / 8704 bytes / OLE_XLS | YES | 200 / 0 bytes / EMPTY | 200 / 8704 bytes / OLE_XLS | TARGET_EXPORT_HOOK_FOUND_NEEDS_BROWSER_FILE_PROOF | READY_NO_FIX_REQUIRED | - |
| qcs-inspect-report | READY | YES | YES | FILE_VERIFIED / 5120 bytes / OLE_XLS | YES | 200 / 0 bytes / EMPTY | 200 / 5120 bytes / OLE_XLS | TARGET_EXPORT_HOOK_FOUND_NEEDS_BROWSER_FILE_PROOF | READY_NO_FIX_REQUIRED | - |
| qcs-unqualified-deal | READY | YES | YES | FILE_VERIFIED / 4608 bytes / OLE_XLS | YES | 200 / 0 bytes / EMPTY | 200 / 4608 bytes / OLE_XLS | TARGET_EXPORT_HOOK_FOUND_NEEDS_BROWSER_FILE_PROOF | READY_NO_FIX_REQUIRED | - |
| qcs-inspect-release | READY | YES | YES | FILE_VERIFIED / 4608 bytes / OLE_XLS | YES | 200 / 0 bytes / EMPTY | 200 / 4608 bytes / OLE_XLS | TARGET_EXPORT_HOOK_FOUND_NEEDS_BROWSER_FILE_PROOF | READY_NO_FIX_REQUIRED | - |

## 验收口径

- 文件导出本身不应该写业务表，落库状态为 `NOT_APPLICABLE`。
- 单目标 PASS 必须同时证明：真实页面有可见导出动作、正常点击导出后收到非空 XLS/XLSX 文件响应、layoutJson 有 runtime 导出动作、源码或 runtime 有目标导出 hook、后端返回非空 XLS/XLSX 列表数据文件。
- `downloadXls` 的导入模板、`*-query exportFlag=true` 返回 JSON/500、空文件、HTTP 200、平台通用 export 模板都不能单独证明生产列表导出可用。

## 复验命令

```bash
make smoke-production-export-readiness ADP_BASE_URL=http://10.11.100.17:18080 ADP_BROWSER_BASE_URL=http://10.11.100.17:18080 PRODUCTION_EXPORT_SMOKE_OUTPUT=metadata/production-export-readiness-smoke.json ADP_PAGE_TIMEOUT_MS=60000 ADP_API_TIMEOUT_MS=30000
make production-export-gap-breakdown
make production-export-gap-breakdown-check
```
