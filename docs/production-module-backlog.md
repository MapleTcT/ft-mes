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

该门禁不负责把问题自动变成 `PASS`。它只保证每个未闭合项都有证据、复验入口、PASS 条件、下一步和非解法，避免后续开发把 HTTP 200、空下载、缺服务或外部客户端缺失误判为功能完成。当前未闭合 backlog 为 `0`，不代表生产迁移和现场签字已完成。

## 当前总览

| 指标 | 数量 |
| --- | ---: |
| Backlog 项 | 0 |
| FAIL_BACKLOG | 0 |
| BLOCKED | 0 |
| PostgreSQL 兼容缺口 | 0 |
| 模块 backlog | 0 |

## 明细

| ID | 状态 | 类型 | 关联用例/动作 | 证据 | 复验入口 | PASS 条件摘要 |
| --- | --- | --- | --- | --- | --- | --- |
| 无 | - | - | 当前生产模块未闭合 backlog 为 0 | `metadata/production-module-backlog.json` | `make production-module-backlog-check` | 后续出现 `FAIL/BLOCKED` 必须重新登记 |

## 本轮关闭

| ID | 状态 | 关闭证据 | 结论 |
| --- | --- | --- | --- |
| PROD-ACTION-007 | NOT_APPLICABLE | `metadata/wom-public-produce-task-created-retirement-acceptance.json`；`metadata/wom-public-produce-task-created-analysis.json` | 2026-07-21 正式废弃旧 public 创建契约。marker `ADP_E2E_20260721104747_PUBLIC_PRODUCE_RETIRED` 证明 `HTTP 200/code=400`、已废弃提示、PostgreSQL `0 -> 0`；真实浏览器确认 `makeTaskList -> 新建指令单` 替代路径可用且 0 错误。这是退役契约 PASS，不是创建功能 PASS。 |
| PROD-010 | PASS | `metadata/rm-web-formula-editor-acceptance.json`；`metadata/production-module-test-cases.json` | 2026-07-17 已新增可维护 `rm-formula-editor` 源码、可见 `Web编辑` 入口、PostgreSQL 修订/投递账本和安全 Nginx 鉴权。marker `ADP_E2E_20260717120436_RM_WEB_FORMULA` 连续三轮完成创建、幂等、两次更新、失败/重试、六表回读、桌面/移动端和清理；不再依赖 IE ActiveX/localhost:4433。现场 Batch/DCS 端点仍需切换前联调签字。 |
| PROD-ACTION-008 | PASS | `metadata/wom-qrcode-browser-acceptance.json`；`metadata/wom-qrcode-persistence-acceptance.json`；`metadata/wom-qrcode-route-probe.json` | 2026-07-17 已新增可维护 `wom-print` 源码、PostgreSQL 表和正常网关路由；真实 `makeTaskList` 鼠标点击、两张二维码渲染、幂等/冲突、PNG、打印状态回填和 marker 清理均通过。当前无打印机配置，物理出纸单独标记为 NOT_APPLICABLE。 |
| PROD-ACTION-006 | PASS | `metadata/wom-manual-task-entry-acceptance.json`；`metadata/persistence-acceptance.json` | 2026-07-17 已新增可维护 `wom-production-entry` 源码、列表可见入口、PostgreSQL 请求账本和活动批号部分唯一索引；真实按钮点击、主数据选择、创建、同请求幂等、重复批号拒绝、待办打开、提交生效、软删除回滚和清理 9/9 通过。 |
| PROD-019 | PASS | `metadata/wom-quality-quantity-persistence-acceptance.json`；`docs/backend-table-audit/wom-bad-quantity-analysis.md` | 2026-07-17 已新增 WOM/QCS 可见“不良数量”入口、PostgreSQL 数量/事件账本和 WMS 分配；marker 证明 10/2/8、幂等、冲销和清理。 |
| PROD-021 | PASS | `metadata/wom-quality-quantity-persistence-acceptance.json`；`metadata/production-module-test-cases.json` | 完整报工剩余缺口关闭：报工总数保持 WOM 权威来源，独立合格/不良数量和 WMS 可用/冻结数量真实落库。 |

## 更新规则

- `metadata/persistence-acceptance.json` 中新增 `FAIL` 或 `BLOCKED` 时，必须在本账本中增加覆盖项，或者明确说明为什么已有 backlog 覆盖。
- 生产模块矩阵新增 `BLOCKED` 时，必须同步 `metadata/production-module-blockers.json` 和本账本。
- `metadata/wom-toolbar-action-coverage.json` 中没有生产用例 ID 覆盖的 `BLOCKED` 工具栏动作，必须用 `womToolbarActionIds` 在本账本中登记。
- 每个新增或变更的生产 backlog 项必须同步 `metadata/business-module-intake-requirements.json`，并通过 `make business-module-intake-requirements-check`。
- ProcessAnalysis 与 material-service 已满足 `docs/business-dependency-contracts.md` 的接入契约；后续分别由 `make process-analysis-test`、`make material-wms-test` 和 marker 落库验收做回归。
- 生产列表导出 `PROD-023` 已于 2026-07-17 完成 6/6 真实浏览器和工作簿验收并移出 backlog；发布回归继续使用 `metadata/production-export-readiness-smoke.json`。
- 任何 backlog 项转为 PASS 前，必须把真实浏览器/API/PostgreSQL 或文件响应证据写回对应验收报告。
- PostgreSQL 缺表、缺列、类型不兼容或 Oracle 方言残留仍优先落到幂等 SQL；无法立即修复时必须在本账本或模块专项 backlog 中登记，不能靠清库重建掩盖。
