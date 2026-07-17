# 生产模块阻断项账本

本账本把 `metadata/production-module-test-cases.json` 中仍为 `BLOCKED` 的生产模块用例单独列出来，方便后续业务包到位后逐项复验。机器可读记录见 `metadata/production-module-blockers.json`，业务模块统一接入验收要求见 `metadata/business-module-intake-requirements.json` / `docs/business-module-intake-requirements.md`，业务依赖包扫描报告见 `metadata/business-dependency-package-scan.json`，业务依赖接入契约见 `metadata/business-dependency-contracts.json` / `docs/business-dependency-contracts.md`，生产导出逐目标缺口见 `metadata/production-export-gap-breakdown.json` / `docs/production-export-gap-breakdown.md`，校验命令：

```bash
make production-blocker-check
make business-module-intake-requirements-check
make business-dependency-contract-check
make production-export-gap-breakdown-check
```

该账本不替代真实前端验收，也不把当前 BLOCKED 项改为 PASS。每个阻断项变更状态前，仍必须按 `docs/production-module-functional-test-cases.md` 的动作级验收规则执行真实浏览器/API/PostgreSQL 复验。

最近一次生产导出复验时间：`2026-07-17T04:53:37.777Z`。`material-service` 与 `process-analysis` 已有 Nacos `prod` 健康实例，并已分别通过真实 marker 落库验收。生产列表导出在当前测试入口 `http://10.11.100.17:18080` 完成 6/6 真实浏览器点击、运行时元数据、目标 sourceAudit 和有效工作簿响应复验，状态为 `READY`，证据为 `metadata/production-export-readiness-smoke.json`，因此 `PROD-023` 已移出 blocker。

## 当前摘要

| 指标 | 数量 |
| --- | ---: |
| BLOCKED 用例 | 3 |
| 外部客户端依赖 | 1 |
| 缺服务包 | 0 |
| 产品范围确认 | 2 |
| 缺导出实现/产品决策 | 0 |

## 阻断项

| Case | 阻断类型 | 依赖/决策 | 复验入口 | PASS 条件 | 下一步 |
| --- | --- | --- | --- | --- | --- |
| PROD-010 | external-client-required | 外部 Batch client / IE ActiveX / WebSocket 推送 | `make smoke-business-page`；接入 Batch 客户端后触发真实 `batchFormulaEdit` | 真实编辑入口打开；marker 保存/提交；PostgreSQL 证明 `rm_formulas` 和配套过程/活动表写入 | 连接 Batch 客户端/服务端路径，不用假按钮替代 |
| PROD-019 | product-scope-confirmation | 独立不良数量是否属于产品范围 | `make persistence-acceptance-check`；产品范围确认 | 若要求独立不良数，必须先有字段/路由/PostgreSQL 数值表并 marker 验收 | 产品确认；不得重新把已 PASS 的 material/WMS 计入阻断 |
| PROD-021 | product-scope-confirmation | 完整报工仅剩独立不良数量范围 | `make persistence-acceptance-check`；回归 WOM 报工和 WMS | 报工与 material/WMS 保持 PASS；独立不良数仅在产品提供字段后验收 | 完成范围决策 |

## 更新规则

- `make production-blocker-check` 必须保证本账本与 `metadata/production-module-test-cases.json` 中的 BLOCKED 用例完全一致。
- `make production-blocker-check` 会交叉校验 material 与 ProcessAnalysis 都有健康实例、实现候选和可达端点；`PROD-020` 已从阻断账本移除，回归证据为 `metadata/process-analysis-persistence-acceptance.json`。
- 每个 blocker 必须有仓库内证据引用、复验命令、PASS 条件、下一步和明确的非解法。
- ProcessAnalysis 与 material/WMS 分别由 `metadata/process-analysis-persistence-acceptance.json`、`metadata/material-wms-persistence-acceptance.json` 作为 PASS 回归证据。
- 新业务包导入后，先运行 `make business-package-scan`；若出现实现候选，再运行 `make smoke-business-dependencies` 和真实前端 marker 落库验收。
- `PROD-023` 已于 2026-07-17 关闭；`make smoke-production-export-readiness ADP_BASE_URL=http://10.11.100.17:18080 ADP_BROWSER_BASE_URL=http://10.11.100.17:18080` 必须继续作为发布回归，结果需保持 `READY`、6/6 有效工作簿和零后端导出错误。
- 逐目标导出状态必须用 `make production-export-gap-breakdown` 从最新 smoke 报告生成，并用 `make production-export-gap-breakdown-check` 校验；不能手写一份会漂移的导出结论。
- 不允许用 HTTP 200、静态页面可打开、临时 SQL 或假按钮把 BLOCKED 项改成 PASS。
