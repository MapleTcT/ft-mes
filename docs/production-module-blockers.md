# 生产模块阻断项账本

本账本把 `metadata/production-module-test-cases.json` 中仍为 `BLOCKED` 的生产模块用例单独列出来，方便后续业务包到位后逐项复验。机器可读记录见 `metadata/production-module-blockers.json`，业务模块统一接入验收要求见 `metadata/business-module-intake-requirements.json` / `docs/business-module-intake-requirements.md`，业务依赖包扫描报告见 `metadata/business-dependency-package-scan.json`，业务依赖接入契约见 `metadata/business-dependency-contracts.json` / `docs/business-dependency-contracts.md`，生产导出逐目标缺口见 `metadata/production-export-gap-breakdown.json` / `docs/production-export-gap-breakdown.md`，校验命令：

```bash
make production-blocker-check
make business-module-intake-requirements-check
make business-dependency-contract-check
make production-export-gap-breakdown-check
```

该账本不替代真实前端验收，也不把当前 BLOCKED 项改为 PASS。每个阻断项变更状态前，仍必须按 `docs/production-module-functional-test-cases.md` 的动作级验收规则执行真实浏览器/API/PostgreSQL 复验。

最近一次业务依赖复验时间：`2026-06-22T01:36:29.672Z`。本次复验仍显示 `material-service` 与 `process-analysis` 为 `2/2 BLOCKED`、ready=0；material 的 `checkProdResult`、`generateProduceOutSing` 仍返回 tenant-service `503`，Nacos material/wms/warehouse/inventory 等别名健康实例均为 0；ProcessAnalysis 的 5 个追溯端点仍返回 tenant-service `503`，Nacos ProcessAnalysis/Traceability 等别名健康实例均为 0，PostgreSQL `processAnalysisTableCount/runtimeView/menu` 仍为 0。最近一次生产导出复验时间：`2026-06-22T01:50:35.404Z`，API base 为 `http://100.99.133.43:18080`，浏览器入口为同一测试环境公网地址 `http://222.88.185.146:18080`，6 个目标页面全部通过页面 smoke，仍为 `BLOCKED`、`ready=0`、`actionRequired=1`、`blocked=5`、`verifiedDataExports=0`。导出阻断已从“页面超时”收敛为“无可见/runtime 导出动作、targetExportMatches=0、downloadXls 为空或无法证明是列表数据导出”。逐目标导出缺口由 `production-export-gap-breakdown` 从 smoke 报告生成：当前 6 个目标都缺可见/runtime 导出动作，其中 WOM 与 3 个 QCS 后端 `queryExport` 返回 500/JSON，RM 目标返回 200/JSON 且不能证明是工作簿列表数据导出；WTS `workPermitList-query?exportFlag=true` 返回 `200/OLE_XLS/8704`，但真实页面没有可见导出动作、layoutJson 没有 runtime 导出动作、sourceAudit 仍归类为 `DOWNLOADXLS_IMPORT_TEMPLATE_OR_EMPTY_ENDPOINT_ONLY`，所以只能降为 `ACTION_REQUIRED`，不能按 PASS 验收。机器账本中的 `latestEvidence` 会和这三份报告的 `generatedAt`、状态和关键计数对账，防止报告刷新后 blocker 总账仍引用旧证据。

## 当前摘要

| 指标 | 数量 |
| --- | ---: |
| BLOCKED 用例 | 6 |
| 外部客户端依赖 | 1 |
| 缺服务包 | 2 |
| 服务 + 产品范围复合阻断 | 2 |
| 缺导出实现/产品决策 | 1 |

## 阻断项

| Case | 阻断类型 | 依赖/决策 | 复验入口 | PASS 条件 | 下一步 |
| --- | --- | --- | --- | --- | --- |
| PROD-010 | external-client-required | 外部 Batch client / IE ActiveX / WebSocket 推送 | `make smoke-business-page`；接入 Batch 客户端后触发真实 `batchFormulaEdit` | 真实编辑入口打开；marker 保存/提交；PostgreSQL 证明 `rm_formulas` 和配套过程/活动表写入 | 连接 Batch 客户端/服务端路径，不用假按钮替代 |
| PROD-019 | composite-service-and-scope | `material` 服务 + 独立不良数字段是否属于产品范围 | `make business-package-scan`；`make smoke-business-dependencies`；material 到位后跑 `checkProdResult/generateProduceOutSing` marker | material 包扫描出现实现候选；Nacos healthy；库存/入库目标表 marker 落库；若要求独立不良数，必须先有字段/路由/表映射 | 部署 material 仓储/库存服务；产品确认是否存在独立坏品数量入口 |
| PROD-020 | missing-service-package | `ProcessAnalysis` / Traceability 服务包、视图、菜单、表 | `make business-package-scan`；`make smoke-business-dependencies`；从 WOM 追溯按钮打开 marker 批次 | ProcessAnalysis 包扫描出现实现候选；Nacos healthy；runtime/menu/table 非 0；追溯端点 HTTP 2xx 且不再 503；PostgreSQL 目标表可查 | 补 ProcessAnalysis 包、运行时元数据、菜单权限和 PostgreSQL schema |
| PROD-021 | composite-service-and-scope | material 库存边界 + 独立不良数字段产品范围 | `make business-package-scan`；`make smoke-business-dependencies`；回归 WOM 报工已 PASS 子流程 | 已有报工子流程保持 PASS；material 包扫描和库存回写 marker 落库；独立不良数仅在产品/包提供字段后验收 | 保持现有报工回归，等 material 服务后补库存边界 |
| PROD-022 | missing-service-package | material 仓储/库存服务包和目标 stock-in 表地图 | `make business-package-scan`；`make smoke-business-dependencies`；material 到位后跑 marker 入库/库存回写 | material 包扫描出现实现候选；`serviceName=material` healthy；两个 material endpoint HTTP 2xx 且不再 503；追踪 Controller/Service/DAO/SQL；PostgreSQL 目标表有 marker | 部署 material 包，补表地图，再做真实落库验收 |
| PROD-023 | missing-export-implementation | 产品确认导出范围 + 后端数据导出实现 | `make smoke-production-export-readiness ADP_BASE_URL=http://100.99.133.43:18080 ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 PRODUCTION_EXPORT_SMOKE_OUTPUT=metadata/production-export-readiness-smoke.json ADP_PAGE_TIMEOUT_MS=240000 ADP_API_TIMEOUT_MS=30000`；恢复导出按钮后抓浏览器文件响应；sourceAudit 必须证明目标页存在导出 hook；`acceptanceContract` 必须列出每个目标的当前缺口 | 产品确认需导出的列表；运行时有导出入口；目标源码/运行时有 `exportExcel/导出` hook；后端返回非空数据文件；`download.verifiedDataExport=true`；记录文件名/大小/样本内容 | 确认导出需求，恢复或实现数据导出按钮和后端方法 |

## 更新规则

- `make production-blocker-check` 必须保证本账本与 `metadata/production-module-test-cases.json` 中的 BLOCKED 用例完全一致。
- `make production-blocker-check` 还会把本账本与 `metadata/business-dependency-readiness-smoke.json`、`metadata/business-dependency-package-scan.json`、`metadata/production-export-readiness-smoke.json` 交叉校验：material / ProcessAnalysis 阻断必须仍有 Nacos healthy=0、tenant-service 503、包扫描无实现候选等证据；导出阻断必须仍有 `verifiedDataExports=0` 的浏览器/runtime/download/sourceAudit/acceptanceContract 证据。
- 每个 blocker 必须有仓库内证据引用、复验命令、PASS 条件、下一步和明确的非解法。
- `material` 和 `ProcessAnalysis` 相关阻断项必须引用 `metadata/business-dependency-package-scan.json`、`metadata/business-dependency-readiness-smoke.json` 以及对应专项分析文档。
- `material` 和 `ProcessAnalysis` 新业务包到位前，必须先通过 `make business-dependency-contract-check` 保持接入契约与 readiness/package scan/blocker/backlog 一致；到位后按 `docs/business-dependency-contracts.md` 的准入顺序补 Nacos、端点、后端链路、目标表和 marker 落库证据。
- 新业务包导入后，先运行 `make business-package-scan`；若出现实现候选，再运行 `make smoke-business-dependencies` 和真实前端 marker 落库验收。
- `PROD-023` 导出阻断项必须引用 `metadata/production-export-readiness-smoke.json`，并通过 `make smoke-production-export-readiness` 重新捕获浏览器文件响应、目标源码/运行时 sourceAudit 和逐目标 `acceptanceContract` 后才能改状态。
- `PROD-023` 的逐目标导出缺口必须用 `make production-export-gap-breakdown` 从最新 smoke 报告生成，并用 `make production-export-gap-breakdown-check` 校验；不能手写一份会漂移的导出结论。
- 2026-06-22 复验：WTS 作业许可 `workPermitList-query` 已从后端 500 修复为 `200/OLE_XLS/8704`，但真实页面仍没有可见导出动作，layoutJson 也没有 runtime 导出动作，因此 `PROD-023` 仍为总体 BLOCKED，WTS 单项在导出报告中降为 `ACTION_REQUIRED`。
- 不允许用 HTTP 200、静态页面可打开、临时 SQL 或假按钮把 BLOCKED 项改成 PASS。
