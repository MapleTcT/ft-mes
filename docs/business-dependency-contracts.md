# 业务依赖接入契约

机器账本为 `metadata/business-dependency-contracts.json`，校验命令：

```bash
make business-dependency-contract-check
```

当前结论：`material-service` 与 `process-analysis` 均为 `READY`，业务依赖
接入契约整体状态为 `READY`。这只代表两项核心依赖已闭合，不覆盖仍在
生产模块总账中的导出、外部 Batch 客户端和产品范围确认事项。

| 依赖 | 当前状态 | 已有或必须满足的证据 | 不能接受的替代证明 |
|---|---|---|---|
| material-service | READY | 源码模块和 Compose 编排；Nacos `prod` 健康实例；`generateProductInSingle`、`checkProdResult`、`generateProduceOutSing` 三个端点；真实浏览器；`ADP_E2E_*` PostgreSQL marker；清理回滚 | 只看 HTTP 200；只看到 BaseSet/WOM 物料表；重新切回 Oracle |
| process-analysis | READY | `backend/source-modules/process-analysis`；Nacos `prod` 健康实例；runtime/menu/table 非零；五个兼容端点；真实 WOM 追溯入口；`ADP_E2E_*` PostgreSQL marker 与清理 | WOM caller-side JS；直接 URL 可达；只有菜单没有目标服务和表；tenant-service `503` |

## Material 已验收边界

`metadata/material-wms-persistence-acceptance.json` 记录了完工入库、幂等
重试、质检释放、生产领料和清理。目标表为 `wms_stock_documents`、
`wms_stock_document_lines`、`wms_batch_stocks`、
`wms_inventory_transactions`、`wms_quality_results`。

独立不良数量不是 material/WMS 阻断。当前产品包没有对应字段、接口和
数值表；该范围仍按 `metadata/wom-bad-quantity-analysis.json` 单独决策。

## ProcessAnalysis 已验收边界

`metadata/process-analysis-persistence-acceptance.json` 记录了真实 WOM
制造任务选择、`prodprocessView` 点击、兼容预检、追溯页加载、工序/活动/
任务手工统计、PostgreSQL 快照 revision 和清理。目标表为
`pa_trace_snapshots`，运行入口由 `runtime_view` 与 `rbac_menuinfo` 提供。

浏览器 console error、失败 response 和 request failure 均为 0；Nacos、
五个兼容端点和 PostgreSQL 计数的当前证据见
`metadata/business-dependency-readiness-smoke.json`。完整后端链路见
`docs/backend-table-audit/processanalysis-dependency-analysis.md`。

## 兼容启动证据

后续业务包仍必须保留 `requiredStartupEvidence`：

- `make module-intake-check INTAKE=/path/to/package` 通过。
- 默认运行 datasource 为 PostgreSQL，Oracle 只允许 legacy/template 路径。
- Nacos 配置通过 `make render-config` 或同等过程生成。
- Docker Compose 有明确服务注册和回滚方式。
- `runtime patch manifest` 覆盖新增运行资产。
- `make smoke-business-dependencies` 证明服务注册和网关行为。
- `make audit-postgres-mappings` 无新增非 PostgreSQL 方言。

这些规则同时固化在 `requiredStartupEvidence` 中。兼容启动证据不等于
业务验收；写动作还必须通过真实页面、API、目标表和清理验证。

## ProcessAnalysis 复验顺序

1. 执行包扫描和 `make process-analysis-test`。
2. 检查 PostgreSQL datasource、Nacos、Compose、菜单/runtime 和回滚适配。
3. 执行五个认证端点探针，确认不返回 tenant-service `503`。
4. 从 WOM 真实追溯按钮打开 marker 批次。
5. 查询 PostgreSQL 目标表并确认 marker 数据已清理。
