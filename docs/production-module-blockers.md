# 生产模块阻断项账本

本账本把 `metadata/production-module-test-cases.json` 中仍为 `BLOCKED` 的生产模块用例单独列出来，方便后续业务包到位后逐项复验。机器可读记录见 `metadata/production-module-blockers.json`，业务模块统一接入验收要求见 `metadata/business-module-intake-requirements.json` / `docs/business-module-intake-requirements.md`，业务依赖包扫描报告见 `metadata/business-dependency-package-scan.json`，业务依赖接入契约见 `metadata/business-dependency-contracts.json` / `docs/business-dependency-contracts.md`，生产导出逐目标缺口见 `metadata/production-export-gap-breakdown.json` / `docs/production-export-gap-breakdown.md`，校验命令：

```bash
make production-blocker-check
make business-module-intake-requirements-check
make business-dependency-contract-check
make production-export-gap-breakdown-check
```

该账本不替代真实前端验收，也不把当前 BLOCKED 项改为 PASS。每个阻断项变更状态前，仍必须按 `docs/production-module-functional-test-cases.md` 的动作级验收规则执行真实浏览器/API/PostgreSQL 复验。

最近一次业务依赖复验时间：`2026-07-10T07:16:11.006Z`。`material-service` 已有 Nacos `prod` 健康实例，三个兼容端点均通过网关且不再返回 tenant-service `503`；marker `ADP_E2E_20260710074612_MATERIAL_WMS` 已证明完工入库、质检释放、生产领料、幂等和清理，因此 `PROD-022` 已移出 blocker。`process-analysis` 仍为 `BLOCKED`：5 个端点继续返回 `503`，运行视图、菜单和 PostgreSQL 目标表仍为 0。生产导出复验仍保持原结论：WTS 单项 READY，其余目标未闭合。

## 当前摘要

| 指标 | 数量 |
| --- | ---: |
| BLOCKED 用例 | 5 |
| 外部客户端依赖 | 1 |
| 缺服务包 | 1 |
| 产品范围确认 | 2 |
| 缺导出实现/产品决策 | 1 |

## 阻断项

| Case | 阻断类型 | 依赖/决策 | 复验入口 | PASS 条件 | 下一步 |
| --- | --- | --- | --- | --- | --- |
| PROD-010 | external-client-required | 外部 Batch client / IE ActiveX / WebSocket 推送 | `make smoke-business-page`；接入 Batch 客户端后触发真实 `batchFormulaEdit` | 真实编辑入口打开；marker 保存/提交；PostgreSQL 证明 `rm_formulas` 和配套过程/活动表写入 | 连接 Batch 客户端/服务端路径，不用假按钮替代 |
| PROD-019 | product-scope-confirmation | 独立不良数量是否属于产品范围 | `make persistence-acceptance-check`；产品范围确认 | 若要求独立不良数，必须先有字段/路由/PostgreSQL 数值表并 marker 验收 | 产品确认；不得重新把已 PASS 的 material/WMS 计入阻断 |
| PROD-020 | missing-service-package | `ProcessAnalysis` / Traceability 服务包、视图、菜单、表 | `make business-package-scan`；`make smoke-business-dependencies`；从 WOM 追溯按钮打开 marker 批次 | ProcessAnalysis 包扫描出现实现候选；Nacos healthy；runtime/menu/table 非 0；追溯端点 HTTP 2xx 且不再 503；PostgreSQL 目标表可查 | 补 ProcessAnalysis 包、运行时元数据、菜单权限和 PostgreSQL schema |
| PROD-021 | product-scope-confirmation | 完整报工仅剩独立不良数量范围 | `make persistence-acceptance-check`；回归 WOM 报工和 WMS | 报工与 material/WMS 保持 PASS；独立不良数仅在产品提供字段后验收 | 完成范围决策 |
| PROD-023 | missing-export-implementation | 产品确认导出范围 + 后端数据导出实现 | `make smoke-production-export-readiness ADP_BASE_URL=http://100.99.133.43:18080 ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 PRODUCTION_EXPORT_SMOKE_OUTPUT=metadata/production-export-readiness-smoke.json ADP_PAGE_TIMEOUT_MS=240000 ADP_API_TIMEOUT_MS=30000`；恢复导出按钮后抓浏览器文件响应；sourceAudit 必须证明目标页存在导出 hook；`acceptanceContract` 必须列出每个目标的当前缺口 | 产品确认需导出的列表；运行时有导出入口；目标源码/运行时有 `exportExcel/导出` hook；后端返回非空数据文件；`download.verifiedDataExport=true`；记录文件名/大小/样本内容 | 确认导出需求，恢复或实现数据导出按钮和后端方法 |

## 更新规则

- `make production-blocker-check` 必须保证本账本与 `metadata/production-module-test-cases.json` 中的 BLOCKED 用例完全一致。
- `make production-blocker-check` 会交叉校验 material 已有健康实例/候选实现和三个可达端点，同时要求 ProcessAnalysis 继续保留 Nacos healthy=0、tenant-service 503、包扫描无实现候选的真实证据。
- 每个 blocker 必须有仓库内证据引用、复验命令、PASS 条件、下一步和明确的非解法。
- ProcessAnalysis 阻断必须引用 package scan、readiness smoke 和专项分析。material/WMS 已转由 `metadata/material-wms-persistence-acceptance.json` 作为 PASS 回归证据。
- 新业务包导入后，先运行 `make business-package-scan`；若出现实现候选，再运行 `make smoke-business-dependencies` 和真实前端 marker 落库验收。
- `PROD-023` 导出阻断项必须引用 `metadata/production-export-readiness-smoke.json`，并通过 `make smoke-production-export-readiness` 重新捕获浏览器文件响应、目标源码/运行时 sourceAudit 和逐目标 `acceptanceContract` 后才能改状态。
- `PROD-023` 的逐目标导出缺口必须用 `make production-export-gap-breakdown` 从最新 smoke 报告生成，并用 `make production-export-gap-breakdown-check` 校验；不能手写一份会漂移的导出结论。
- 2026-06-22 复验：WTS 作业许可 `workPermitList-query` 已通过真实浏览器点击 `导出` 生成 `WTS_workPermitList.xls`，`200/OLE_XLS/8704`，WTS 单项在导出报告中为 `READY`；`PROD-023` 仍为总体 BLOCKED，因为 WOM、RM 和 QCS 的 5 个导出目标尚未完成同等证据闭环。
- 不允许用 HTTP 200、静态页面可打开、临时 SQL 或假按钮把 BLOCKED 项改成 PASS。
