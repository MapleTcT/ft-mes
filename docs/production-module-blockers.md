# 生产模块阻断项账本

本账本把 `metadata/production-module-test-cases.json` 中仍为 `BLOCKED` 的生产模块用例单独列出来，方便后续业务包到位后逐项复验。机器可读记录见 `metadata/production-module-blockers.json`，业务模块统一接入验收要求见 `metadata/business-module-intake-requirements.json` / `docs/business-module-intake-requirements.md`，业务依赖包扫描报告见 `metadata/business-dependency-package-scan.json`，业务依赖接入契约见 `metadata/business-dependency-contracts.json` / `docs/business-dependency-contracts.md`，生产导出逐目标缺口见 `metadata/production-export-gap-breakdown.json` / `docs/production-export-gap-breakdown.md`，校验命令：

```bash
make production-blocker-check
make business-module-intake-requirements-check
make business-dependency-contract-check
make production-export-gap-breakdown-check
```

该账本不替代真实前端验收。每个阻断项变更状态前，仍必须按 `docs/production-module-functional-test-cases.md` 的动作级验收规则执行真实浏览器/API/PostgreSQL 复验。

最近一次 WOM/QCS 独立不良数量复验时间：`2026-07-17T14:10:28Z`。`material-service`、`process-analysis` 和 `wom-quality-reporting` 均在当前测试环境运行，并已通过真实 marker 落库验收。`metadata/production-export-readiness-smoke.json` 记录生产列表导出在当前测试入口 `http://10.11.100.17:18080` 完成 6/6 真实浏览器点击和有效工作簿响应复验，状态为 `READY`。RM Web 配方编辑、独立不良数量及完整报工均已移出 blocker；现场 Batch/DCS 端点签字仍是生产切换前置条件，不重新计为页面功能 blocker。

## 当前摘要

| 指标 | 数量 |
| --- | ---: |
| BLOCKED 用例 | 0 |
| 外部客户端依赖 | 0 |
| 缺服务包 | 0 |
| 产品范围确认 | 0 |
| 缺导出实现/产品决策 | 0 |

## 阻断项

| Case | 阻断类型 | 依赖/决策 | 复验入口 | PASS 条件 | 下一步 |
| --- | --- | --- | --- | --- | --- |
| 无 | - | 当前生产矩阵 `44 PASS / 0 BLOCKED` | `make production-blocker-check` | 阻断账本与矩阵保持一致 | 继续管理生产切换门禁，不把它们混入页面功能阻断 |

## 更新规则

- `make production-blocker-check` 必须保证本账本与 `metadata/production-module-test-cases.json` 中的 BLOCKED 用例完全一致。
- `make production-blocker-check` 会交叉校验 material 与 ProcessAnalysis 都有健康实例、实现候选和可达端点；`PROD-020` 已从阻断账本移除，回归证据为 `metadata/process-analysis-persistence-acceptance.json`。
- 每个 blocker 必须有仓库内证据引用、复验命令、PASS 条件、下一步和明确的非解法。
- ProcessAnalysis 与 material/WMS 分别由 `metadata/process-analysis-persistence-acceptance.json`、`metadata/material-wms-persistence-acceptance.json` 作为 PASS 回归证据。
- 新业务包导入后，先运行 `make business-package-scan`；若出现实现候选，再运行 `make smoke-business-dependencies` 和真实前端 marker 落库验收。
- `PROD-023` 已于 2026-07-17 关闭；`make smoke-production-export-readiness ADP_BASE_URL=http://10.11.100.17:18080 ADP_BROWSER_BASE_URL=http://10.11.100.17:18080` 必须继续作为发布回归，结果需保持 `READY`、6/6 有效工作簿和零后端导出错误。
- `PROD-010` 已于 2026-07-17 关闭；发布回归使用 `make acceptance-rm-web-formula-editor-persistence` 和 `make rm-web-formula-editor-acceptance-check`。验收 overlay 仅允许在隔离测试时启用，结束后必须恢复空 `RM_FORMULA_DELIVERY_URL`、关闭模拟器并复核容器环境。
- `PROD-019/021` 已于 2026-07-17 关闭；发布回归使用 `make wom-quality-reporting-test`、`make material-wms-test` 和 `make acceptance-wom-quality-quantity-persistence`，证据为 `metadata/wom-quality-quantity-persistence-acceptance.json`。
- 逐目标导出状态必须用 `make production-export-gap-breakdown` 从最新 smoke 报告生成，并用 `make production-export-gap-breakdown-check` 校验；不能手写一份会漂移的导出结论。
- 不允许用 HTTP 200、静态页面可打开、临时 SQL 或假按钮把 BLOCKED 项改成 PASS。
