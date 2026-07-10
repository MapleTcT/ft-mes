# 业务模块接入验收要求

本文件覆盖当前仍为 `BLOCKED` 的 7 个生产缺口。机器账本为
`metadata/business-module-intake-requirements.json`：

```bash
make business-module-intake-requirements-check
```

material/WMS 与 ProcessAnalysis 均已完成，不再是待接入服务包；
`PROD-019` 和 `PROD-021` 已收敛为独立不良数量的产品范围决定。

| 类型 | 数量 | 涉及项 |
|---|---:|---|
| 产品范围决定 | 4 | `PROD-ACTION-006`、`PROD-ACTION-007`、`PROD-019`、`PROD-021` |
| 缺 runtime endpoint | 1 | `PROD-ACTION-008` |
| 外部客户端 | 1 | `PROD-010` |
| 缺服务包 | 0 | 无 |
| 缺导出实现 | 1 | `PROD-023` |

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
| `PROD-ACTION-006` | 产品范围决定 | 明确 WOM 手工创建/导入是否属于支持范围 |
| `PROD-ACTION-007` | 产品范围决定 | 明确 public `produceTaskCreated` 是废弃还是恢复 |
| `PROD-ACTION-008` | runtime endpoint | 恢复 WOM `printManage/generateQrCode`，真实点击不再 `404` |
| `PROD-010` | 外部客户端 | 通过 Batch 客户端/ActiveX/WebSocket 打开真实编辑链 |
| `PROD-019` | 产品范围决定 | 确认是否存在独立不良数量；若存在，提供字段、路由、后端和 PostgreSQL 数值表 |
| `PROD-021` | 产品范围决定 | 保持报工和 material 回归，仅决策独立不良数量范围 |
| `PROD-023` | 导出实现 | 补可见导出、后端 list-data export 和非空 workbook |

`PROD-022` 完工入库已由 material/WMS marker 验收转为 PASS，证据为
`metadata/material-wms-persistence-acceptance.json`，不再出现在接入需求中。
`PROD-020` 追溯已由 ProcessAnalysis marker 验收转为 PASS，证据为
`metadata/process-analysis-persistence-acceptance.json`。
