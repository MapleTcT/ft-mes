# 生产模块 Backlog 账本

## 目的

本账本把生产模块真实验收中未闭合的问题落成可复验 backlog。它覆盖两类问题：

- 生产测试矩阵中的 `BLOCKED` 用例。
- 落库验收中的 `FAIL` 或 `BLOCKED` 动作，尤其是接口返回成功但 PostgreSQL 没有变化的假成功接口。
- WOM 等生产页面专项动作覆盖中没有生产用例 ID、但已经被真实点击证明阻断的动作。

机器可读记录见 `metadata/production-module-backlog.json`；后续业务包、endpoint、外部客户端和导出实现的统一接入验收要求见
`metadata/business-module-intake-requirements.json` 和 `docs/business-module-intake-requirements.md`；material-service / process-analysis 新包接入契约见
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
| Backlog 项 | 9 |
| FAIL_BACKLOG | 0 |
| BLOCKED | 9 |
| PostgreSQL 兼容缺口 | 0 |
| 模块 backlog | 9 |

## 明细

| ID | 状态 | 类型 | 关联用例/动作 | 证据 | 复验入口 | PASS 条件摘要 |
| --- | --- | --- | --- | --- | --- | --- |
| PROD-ACTION-006 | BLOCKED | product-scope-confirmation | WOM 可见手工创建/导入入口未暴露 | `metadata/persistence-acceptance.json`；`docs/frontend-functional-test-report.md` | `make discover-production-actions ADP_BROWSER_BASE_URL=http://222.88.185.146:18080`，产品确认后补真实入口或范围说明 | 若手工创建在范围内，必须有可见入口和 marker 落库；若不在范围内，必须有产品证据并继续保留 public `produceTaskCreated` no-op 已显式禁用的产品确认 backlog |
| PROD-ACTION-007 | BLOCKED | product-scope-confirmation | public `produceTaskCreated` no-op 已显式禁用，待产品确认是否仍支持 | `metadata/wom-public-produce-task-created-analysis.json`；`metadata/wom-public-produce-task-created-noop-probe.json`；`docs/backend-table-audit/wom-public-produce-task-created-analysis.md` | `make probe-wom-public-produce-task-created-noop`；恢复/废弃决策后重新调用 public endpoint 并用 marker 查 `wom_produce_tasks` | 支持则真实落库；废弃则把明确失败/禁用写入产品/API 说明；禁止用 `produceTaskCreated2` 证据替代 |
| PROD-ACTION-008 | BLOCKED | missing-runtime-endpoint | WOM `makeTaskList` 生成二维码运行包接口缺失 | `metadata/wom-toolbar-action-coverage.json`；`metadata/wom-qrcode-route-probe.json`，2026-06-22 09:27 复验仍为三端点 404 且 WOMMs jar 无匹配实现 | `make probe-wom-qrcode-route`，恢复 WOM printManage QR endpoint/package 后从真实工具栏点击复验 | `generateCode/generateQrCode/backfill-printInfo` 不再 404；浏览器点击、请求/响应、文件或 PostgreSQL QR/打印表证据闭合 |
| PROD-010 | BLOCKED | external-client-required | RM 批量配方可见显式编辑入口 | `metadata/production-module-blockers.json`；`docs/frontend-functional-test-report.md` | 外部 Batch 客户端/ActiveX/WebSocket 联调后做 marker 保存 | 真实外部编辑入口打开并写入 RM 相关表 |
| PROD-019 | BLOCKED | composite-service-and-scope | QCS 不良数和库存/入库相关回写 | `metadata/material-service-dependency-analysis.json`；`metadata/wom-bad-quantity-analysis.json` | `make smoke-business-dependencies`，补 material 服务后做库存 marker | material 服务可用并证明库存/入库目标表写入；独立不良数需产品路由/表证据 |
| PROD-020 | BLOCKED | missing-service-package | 批次、物料、工单追溯查询 | `metadata/processanalysis-dependency-analysis.json` | `make smoke-business-dependencies`，补 ProcessAnalysis 后从 WOM 追溯按钮复验 | ProcessAnalysis 服务、菜单、runtime、表均可用，追溯页面返回成功 |
| PROD-021 | BLOCKED | composite-service-and-scope | 完整工序报工、产量、不良数登记 | `metadata/production-module-test-cases.json`；`metadata/wom-bad-quantity-analysis.json` | 维持已通过报工子流程，补 material 服务后追加库存边界验收 | 报工子流程继续 PASS，库存边界和独立不良数范围闭合 |
| PROD-022 | BLOCKED | missing-service-package | 完工入库或库存回写 | `metadata/material-service-dependency-analysis.json` | 补 material/warehouse 服务后运行 marker stock-in 验收 | `checkProdResult`/`generateProduceOutSing` 不再 503，PostgreSQL 目标表有 marker |
| PROD-023 | BLOCKED | missing-export-implementation | 生产相关列表导出 | `metadata/production-export-readiness-smoke.json` | `make smoke-production-export-readiness`，实现导出后抓真实文件响应和 sourceAudit | 可见/runtime/sourceAudit 导出入口存在，后端返回非空列表数据文件 |

## 更新规则

- `metadata/persistence-acceptance.json` 中新增 `FAIL` 或 `BLOCKED` 时，必须在本账本中增加覆盖项，或者明确说明为什么已有 backlog 覆盖。
- 生产模块矩阵新增 `BLOCKED` 时，必须同步 `metadata/production-module-blockers.json` 和本账本。
- `metadata/wom-toolbar-action-coverage.json` 中没有生产用例 ID 覆盖的 `BLOCKED` 工具栏动作，必须用 `womToolbarActionIds` 在本账本中登记。
- 每个新增或变更的生产 backlog 项必须同步 `metadata/business-module-intake-requirements.json`，并通过 `make business-module-intake-requirements-check`。
- material-service / process-analysis 相关 backlog 转为 PASS 前，必须先满足 `docs/business-dependency-contracts.md` 中的接入契约，并复跑 `make business-dependency-contract-check`、`make business-package-scan` 和 `make smoke-business-dependencies`。
- 任何 backlog 项转为 PASS 前，必须把真实浏览器/API/PostgreSQL 或文件响应证据写回对应验收报告。
- PostgreSQL 缺表、缺列、类型不兼容或 Oracle 方言残留仍优先落到幂等 SQL；无法立即修复时必须在本账本或模块专项 backlog 中登记，不能靠清库重建掩盖。
