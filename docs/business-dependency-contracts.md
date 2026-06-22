# 业务依赖接入契约

本文件定义当前阻断生产模块验收的外部业务依赖如何接入、如何复验、以及哪些证据不能算作完成。机器可读记录见
`metadata/business-dependency-contracts.json`，校验命令：

```bash
make business-dependency-contract-check
```

当前结论：`material-service` 和 `process-analysis` 仍为 `BLOCKED`。契约不把它们改成 PASS，只把后续新业务包到来后的准入标准固定下来。

| 依赖 | 阻断用例 | 当前状态 | 必须满足的接入证据 | 不能接受的替代证明 |
|---|---|---|---|---|
| material-service | `PROD-019`、`PROD-021`、`PROD-022` | BLOCKED | 包扫描出现实现候选；Nacos `prod` 至少一个健康实例；`checkProdResult`、`generateProduceOutSing` 返回 HTTP 2xx 且不再 tenant-service `503`；追踪 Controller/Service/DAO/SQL；确认库存/入库目标表；真实浏览器或 API marker 写 PostgreSQL | 只看到 BaseSet/WOM 物料表；QCS/WOM 状态回写；HTTP 200 但无库存/入库 marker |
| process-analysis | `PROD-020` | BLOCKED | 包扫描出现实现候选；Nacos `prod` 至少一个健康实例；runtime/menu/table 计数大于 0；五个追溯端点返回 HTTP 2xx 且不再 tenant-service `503`；从真实 WOM 追溯按钮或等效登录上下文打开 marker 批次页面 | WOM caller-side JS；直接 URL 可达；只有菜单没有目标服务和表 |

## 兼容启动证据

后续拿到 material 或 ProcessAnalysis 业务包时，不能只把 jar/war 复制进运行目录。每个依赖都必须在
`metadata/business-dependency-contracts.json` 的 `requiredStartupEvidence` 中保留以下证据：

- `make module-intake-check INTAKE=/path/to/package` 通过，且 Oracle 只能作为 legacy/profile/source-snapshot 线索存在。
- 默认运行 datasource 仍为 PostgreSQL，不能把 Oracle 配置重新带回默认路径。
- Nacos 配置由 `make render-config` 或同等生产渲染流程生成，并记录对应 dataId/group。
- Docker Compose 或生产编排中有明确服务注册、端口、健康检查和禁用/回滚方式。
- runtime patch manifest 已更新，`make runtime-patch-manifest-check` 能覆盖新增静态/runtime/Nacos/SQL 资产。
- `make smoke-business-dependencies` 能证明服务注册健康、认证端点返回 HTTP 2xx 且不再是 tenant-service `503`，并保留 material / ProcessAnalysis 数据库证据。
- `make audit-postgres-mappings` 不能发现新增 Oracle/MySQL/SQL Server 方言。

只有包扫描、兼容启动、真实端点和 PostgreSQL marker 落库同时成立，才能把对应 `PROD-*` 阻断项从
`BLOCKED` 改为 `PASS`。

## 接入顺序

1. 新业务包进入后，先运行 `make module-intake-check INTAKE=/path/to/package-or-dir`。
2. 运行 `make business-package-scan BUSINESS_PACKAGE_SCAN_OUTPUT=metadata/business-dependency-package-scan.json`，确认 implementation candidate。
3. 执行兼容启动适配：PostgreSQL datasource、Nacos dataId、Compose/生产服务注册、runtime patch manifest、禁用/回滚方案。
4. 部署到测试编排并确认 Nacos `prod` 注册健康实例。
5. 运行 `make smoke-business-dependencies`，确认依赖端点返回 HTTP 2xx、不再返回 tenant-service `503`，并确认 material / ProcessAnalysis 数据库证据存在。
6. 从真实前端或等效登录上下文执行 `ADP_E2E_*` marker 动作。
7. 追踪后端 Controller/Service/DAO/SQL，并直接查询 PostgreSQL 目标表。
8. 更新 `metadata/production-module-blockers.json`、`metadata/production-module-backlog.json`、`metadata/persistence-acceptance.json` 和对应文档。

## 必守规则

- 业务依赖验收不能只看 HTTP 200。
- 缺服务包不能靠临时 SQL、假按钮或 WOM 单侧 patch 伪装完成。
- 每个会写业务数据的动作必须有唯一 marker、请求 payload、响应摘要、后端链路、目标表和 SQL 查询结果。
- 新发现 PostgreSQL 缺表、缺列、类型不兼容或 Oracle 方言残留，必须落到幂等 SQL 或模块 backlog。
