# 生产模块 Backlog 账本

## 目的

本账本把生产模块真实验收中未闭合的问题落成可复验 backlog。它覆盖两类问题：

- 生产测试矩阵中的 `BLOCKED` 用例。
- 落库验收中的 `FAIL` 或 `BLOCKED` 动作，尤其是接口返回成功但 PostgreSQL 没有变化的假成功接口。
- WOM 等生产页面专项动作覆盖中没有生产用例 ID、但已经被真实点击证明阻断的动作。

机器可读记录见 `metadata/production-module-backlog.json`；后续业务包、endpoint、外部客户端和导出实现的统一接入验收要求见
`metadata/business-module-intake-requirements.json` 和 `docs/business-module-intake-requirements.md`；material-service / process-analysis 接入契约见
`metadata/business-dependency-contracts.json` 和 `docs/business-dependency-contracts.md`。校验命令：

```bash
make production-module-backlog-check
make business-module-intake-requirements-check
make business-dependency-contract-check
```

该门禁不负责把问题自动变成 `PASS`。它只保证每个未闭合项都有证据、复验入口、PASS 条件、下一步和非解法，避免后续开发把 HTTP 200、空下载、缺服务或外部客户端缺失误判为功能完成。

## 当前总览

| 指标 | 数量 |
| --- | ---: |
| Backlog 项 | 6 |
| FAIL_BACKLOG | 0 |
| BLOCKED | 6 |
| PostgreSQL 兼容缺口 | 0 |
| 模块 backlog | 6 |

## 明细

| ID | 状态 | 类型 | 关联用例/动作 | 证据 | 复验入口 | PASS 条件摘要 |
| --- | --- | --- | --- | --- | --- | --- |
| PROD-ACTION-006 | BLOCKED | product-scope-confirmation | WOM 可见手工创建/导入入口未暴露 | `metadata/persistence-acceptance.json`；`docs/frontend-functional-test-report.md` | `make discover-production-actions ADP_BROWSER_BASE_URL=http://10.11.100.17:18080`，产品确认后补真实入口或范围说明 | 若手工创建在范围内，必须有可见入口和 marker 落库；若不在范围内，必须有产品证据并继续保留 public `produceTaskCreated` no-op 已显式禁用的产品确认 backlog |
| PROD-ACTION-007 | BLOCKED | product-scope-confirmation | public `produceTaskCreated` no-op 已显式禁用，待产品确认是否仍支持 | `metadata/wom-public-produce-task-created-analysis.json`；`metadata/wom-public-produce-task-created-noop-probe.json`；`docs/backend-table-audit/wom-public-produce-task-created-analysis.md` | `make probe-wom-public-produce-task-created-noop`；恢复/废弃决策后重新调用 public endpoint 并用 marker 查 `wom_produce_tasks` | 支持则真实落库；废弃则把明确失败/禁用写入产品/API 说明；禁止用 `produceTaskCreated2` 证据替代 |
| PROD-ACTION-008 | BLOCKED | missing-runtime-endpoint | WOM `makeTaskList` 生成二维码运行包接口缺失 | `metadata/wom-toolbar-action-coverage.json`；`metadata/wom-qrcode-route-probe.json`，2026-06-22 09:27 复验仍为三端点 404 且 WOMMs jar 无匹配实现 | `make probe-wom-qrcode-route`，恢复 WOM printManage QR endpoint/package 后从真实工具栏点击复验 | `generateCode/generateQrCode/backfill-printInfo` 不再 404；浏览器点击、请求/响应、文件或 PostgreSQL QR/打印表证据闭合 |
| PROD-010 | BLOCKED | external-client-required | RM 批量配方可见显式编辑入口 | `metadata/production-module-blockers.json`；`docs/frontend-functional-test-report.md` | 外部 Batch 客户端/ActiveX/WebSocket 联调后做 marker 保存 | 真实外部编辑入口打开并写入 RM 相关表 |
| PROD-019 | BLOCKED | product-scope-confirmation | 独立 QCS 不良数量登记范围确认 | `metadata/wom-bad-quantity-analysis.json`；`metadata/material-wms-persistence-acceptance.json` | 产品确认独立不良数量是否属于支持范围 | 若需要，必须提供真实字段/路由/后端/PostgreSQL 数值表并 marker 验收；material/WMS 保持 PASS |
| PROD-021 | BLOCKED | product-scope-confirmation | 完整报工仅剩独立不良数量范围确认 | `metadata/production-module-test-cases.json`；`metadata/wom-bad-quantity-analysis.json`；`metadata/material-wms-persistence-acceptance.json` | 保持报工和 material/WMS 回归，产品确认独立不良数量范围 | 只在产品确认需要时新增字段/路由/表并 marker 验收 |

## 更新规则

- `metadata/persistence-acceptance.json` 中新增 `FAIL` 或 `BLOCKED` 时，必须在本账本中增加覆盖项，或者明确说明为什么已有 backlog 覆盖。
- 生产模块矩阵新增 `BLOCKED` 时，必须同步 `metadata/production-module-blockers.json` 和本账本。
- `metadata/wom-toolbar-action-coverage.json` 中没有生产用例 ID 覆盖的 `BLOCKED` 工具栏动作，必须用 `womToolbarActionIds` 在本账本中登记。
- 每个新增或变更的生产 backlog 项必须同步 `metadata/business-module-intake-requirements.json`，并通过 `make business-module-intake-requirements-check`。
- ProcessAnalysis 与 material-service 已满足 `docs/business-dependency-contracts.md` 的接入契约；后续分别由 `make process-analysis-test`、`make material-wms-test` 和 marker 落库验收做回归。
- 生产列表导出 `PROD-023` 已于 2026-07-17 完成 6/6 真实浏览器和工作簿验收并移出 backlog；发布回归继续使用 `metadata/production-export-readiness-smoke.json`。
- 任何 backlog 项转为 PASS 前，必须把真实浏览器/API/PostgreSQL 或文件响应证据写回对应验收报告。
- PostgreSQL 缺表、缺列、类型不兼容或 Oracle 方言残留仍优先落到幂等 SQL；无法立即修复时必须在本账本或模块专项 backlog 中登记，不能靠清库重建掩盖。
