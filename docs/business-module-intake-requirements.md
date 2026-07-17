# 业务模块接入验收要求

本文件覆盖当前仍为 `BLOCKED` 的 3 个生产缺口。机器账本为
`metadata/business-module-intake-requirements.json`：

```bash
make business-module-intake-requirements-check
```

material/WMS 与 ProcessAnalysis 均已完成，不再是待接入服务包；
`PROD-019` 和 `PROD-021` 已收敛为独立不良数量的产品范围决定。

| 类型 | 数量 | 涉及项 |
|---|---:|---|
| 产品范围决定 | 3 | `PROD-ACTION-007`、`PROD-019`、`PROD-021` |
| 缺 runtime endpoint | 0 | 无 |
| 外部客户端 | 0 | 无 |
| 缺服务包 | 0 | 无 |
| 缺导出实现 | 0 | 无 |

## 验收总规则

- 新包先跑 `make module-intake-check INTAKE=/path/to/package-or-dir`。
- 默认运行路径继续使用 PostgreSQL，Oracle 只保留 legacy/template。
- 写业务数据必须有 `ADP_E2E_*` marker、真实浏览器、API、后端链路、
  PostgreSQL before/after SQL 和清理证据。
- 文件导出使用 `NOT_APPLICABLE_FILE_EXPORT`，但必须证明真实 file/workbook
  response。
- 不能用临时 SQL、假按钮、HTTP 200 或 mock 数据替代业务验收。

## 逐项要求

| ID | 类型 | 下一步 |
|---|---|---|
| `PROD-ACTION-007` | 产品范围决定 | 明确 public `produceTaskCreated` 是废弃还是恢复 |
| `PROD-019` | 产品范围决定 | 确认是否存在独立不良数量；若存在，提供字段、路由、后端和 PostgreSQL 数值表 |
| `PROD-021` | 产品范围决定 | 保持报工和 material 回归，仅决策独立不良数量范围 |

`PROD-022` 完工入库已由 material/WMS marker 验收转为 PASS，证据为
`metadata/material-wms-persistence-acceptance.json`，不再出现在接入需求中。
`PROD-020` 追溯已由 ProcessAnalysis marker 验收转为 PASS，证据为
`metadata/process-analysis-persistence-acceptance.json`。
`PROD-023` 生产列表导出已在 `http://10.11.100.17:18080` 完成 6/6
真实浏览器、运行时元数据、后端 file/workbook 响应和 sourceAudit 验收，状态
`READY`，证据为 `metadata/production-export-readiness-smoke.json`。

`PROD-010` RM 批控配方 Web 编辑已于 2026-07-17 转为 PASS：真实列表显示
`Web编辑`，marker `ADP_E2E_20260717120436_RM_WEB_FORMULA` 连续三轮完成创建、
幂等、两次修订、失败重试、六表 PostgreSQL 回读和清理。现场 Batch/DCS endpoint
及签字是生产切换前置，不再作为业务模块接入缺口。

## 本轮关闭

`PROD-ACTION-006` 已于 2026-07-17 关闭：`wom-production-entry` 源码模块、
PostgreSQL 请求账本、网关路由和 `makeTaskList` 可见按钮已部署。真实浏览器完成
创建、同请求幂等、重复批号拒绝、部分唯一并发保护、待办打开、提交生效、软删除回滚和清理 9/9 验收；证据为
`metadata/wom-manual-task-entry-acceptance.json` 和
`metadata/wom-manual-task-entry-*.png`。

`PROD-ACTION-008` 已于 2026-07-17 关闭：`wom-print` 源码模块、PostgreSQL
迁移和网关路由已部署，真实 `makeTaskList` 鼠标点击生成两张二维码，浏览器检查
`10/10`、API/PostgreSQL 检查 `10/10`，marker 清理后为 0。证据：
`metadata/wom-qrcode-browser-acceptance.json`、
`metadata/wom-qrcode-persistence-acceptance.json`。当前没有打印机配置，因此物理出纸
为 `NOT_APPLICABLE`，不影响二维码生成和打印状态回填的 PASS 结论。
