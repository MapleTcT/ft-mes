# 业务模块接入验收要求

本文件把生产模块当前仍为 `BLOCKED` 的 9 个功能缺口整理成后续业务包接入准入要求。机器可读账本见
`metadata/business-module-intake-requirements.json`，校验命令：

```bash
make business-module-intake-requirements-check
```

它不把任何阻断项改成 `PASS`。作用是防止后续拿到新包、补 endpoint 或补按钮时只凭页面可点、接口 `200`、或临时 SQL 就宣布完成。

## 当前结论

| 类型 | 数量 | 涉及项 | 当前要求 |
|---|---:|---|---|
| 产品范围决定 | 2 | `PROD-ACTION-006`、`PROD-ACTION-007` | 先确认制造任务手工创建/导入和 public `produceTaskCreated` 是否仍属支持范围 |
| 缺 runtime endpoint | 1 | `PROD-ACTION-008` | 恢复 WOM `printManage/generateQrCode` 等 QR/打印端点后再做真实浏览器点击验收 |
| 外部客户端 | 1 | `PROD-010` | 接入真实 Batch 客户端/ActiveX/WebSocket 流程，不能加假按钮绕过原产品路径 |
| 缺服务包 | 2 | `PROD-020`、`PROD-022` | 补 ProcessAnalysis、material 服务包，并满足 Nacos、端点、表映射和 PostgreSQL 证据 |
| 服务包加范围确认 | 2 | `PROD-019`、`PROD-021` | material 服务可用后补库存/入库边界；独立不良数必须先有产品字段/路由/表证据 |
| 缺导出实现 | 1 | `PROD-023` | 补可见导出按钮、runtime 元数据、后端 list-data export 和非空文件响应 |

## 验收总规则

- 新业务包先跑 `make module-intake-check INTAKE=/path/to/package-or-dir`。
- 包扫描继续跑 `make business-package-scan BUSINESS_PACKAGE_SCAN_OUTPUT=metadata/business-dependency-package-scan.json`。
- 默认运行路径必须继续是 PostgreSQL，Oracle 只能留在 legacy/template/source-snapshot 线索中。
- 服务包必须有 Nacos `prod` 健康实例、正常网关 endpoint、后端 Controller/Service/DAO/SQL 追踪和目标表映射。
- 写业务数据的动作必须有 `ADP_E2E_*` marker、请求 payload、响应摘要、后端链路和 PostgreSQL before/after SQL。
- 文件导出不要求落库，验收状态应写成 `NOT_APPLICABLE_FILE_EXPORT` 或等价的
  `NOT_APPLICABLE` 说明，但必须有真实浏览器 network/file evidence，证明 filename/status/size/sample workbook content。
- 任何新增缺表、缺列、类型不兼容、Oracle 方言残留，都进入幂等 SQL 或 backlog，不能通过清库重建掩盖。

## 逐项接入要求

| ID | 类型 | 需要补齐 | 必须复验 | 不能接受 |
|---|---|---|---|---|
| `PROD-ACTION-006` | 产品范围决定 | 确认 WOM 制造任务手工创建/导入入口是否在产品范围内 | 若在范围内，真实页面入口 + marker 写 `wom_produce_tasks` 和 workflow 表 | 用 `produceTaskCreated2` 代替手工入口验收 |
| `PROD-ACTION-007` | 产品范围决定 | 确认 public `produceTaskCreated` 是废弃还是恢复支持 | 废弃则保持显式失败；恢复则 marker 写 `wom_produce_tasks` | 只看 HTTP `200` 或拿 `produceTaskCreated2` 证据冒充 |
| `PROD-ACTION-008` | runtime endpoint | WOM `printManage/generateQrCode`、`generateCode`、`backfill-printInfo` | 真实 makeTaskList 工具栏点击、endpoint 不再 `404`、QR/打印文件或 PostgreSQL 元数据证据 | 只打开弹窗、只补打印机配置、endpoint 仍 `404` |
| `PROD-010` | 外部客户端 | Batch 客户端/ActiveX/WebSocket 编辑入口 | 真实外部流打开 `batchFormulaEdit`，marker 写 `rm_formulas` 等 RM 表 | 人工加平台按钮绕过客户端 |
| `PROD-019` | 服务包加范围确认 | material 库存/入库服务，必要时补独立不良数字段/路由/表证据 | `checkProdResult`/`generateProduceOutSing` 不再 `503`，marker 写库存/入库表 | 把 QCS/WOM 状态回写当库存证明 |
| `PROD-020` | 服务包 | ProcessAnalysis 追溯服务包、runtime/menu/table | 五个追溯 endpoint 正常，从真实 WOM 追溯按钮打开 marker 批次页面 | 只 patch WOM 或直接 URL 可达 |
| `PROD-021` | 服务包加范围确认 | material 库存边界和独立不良数范围确认 | 已通过报工子流程保持 PASS，再补 material marker 写库 | 用数据库 trigger 强造 WOM 耗用记录 |
| `PROD-022` | 服务包 | material warehouse/inventory 服务包和目标表图谱 | 端点、后端链路、PostgreSQL 库存/入库 marker | 用 BaseSet 物料主数据或 WOM 内部表冒充库存服务 |
| `PROD-023` | 导出实现 | 可见导出按钮、runtime export action、后端 list-data export | 浏览器下载非空 workbook，记录文件名、状态、大小、样例内容 | 空 `downloadXls`、导入模板下载、JSON 查询响应 |

## 与现有账本的关系

- `metadata/production-module-backlog.json` 是当前未闭合功能的主账本。
- `metadata/production-module-blockers.json` 是 6 个生产用例级 blocker 的主账本。
- `metadata/business-dependency-contracts.json` 继续负责 material 和 ProcessAnalysis 两个服务包契约。
- `metadata/production-export-gap-breakdown.json` 继续负责生产列表导出逐页面缺口。
- 本文件负责把这些证据统一成“新包来了以后先做什么、补完怎么验收”的入口清单。
