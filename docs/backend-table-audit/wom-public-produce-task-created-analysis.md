# WOM public produceTaskCreated 专项分析

## 结论

`POST /msService/public/WOM/produceTask/produceTask/produceTaskCreated` 当前不能作为生产工单/制造指令单创建入口验收。测试机 `100.99.133.43` 的恢复源码中，原 `creatProTask` 创建主体被整段注释，旧运行态会返回 `HTTP 200/code=200/处理成功` 但 PostgreSQL `wom_produce_tasks` 前后 marker 计数均为 `0`。本轮已在测试运行包中把该 public 入口显式禁用，防止继续出现“成功但不落库”的假阳性。

当前该风险已有可复跑探针：

```bash
ADP_DB_SSH_PASSWORD=*** make probe-wom-public-produce-task-created-noop
```

最新机器证据见 `metadata/wom-public-produce-task-created-noop-probe.json`。本轮
`generatedAt=2026-06-22T07:29:05.735Z`，marker
`ADP_E2E_20260622072905_PUBLIC_PRODUCE_NOOP` 确认接口返回
`HTTP 200/code=400` 和“已禁用”业务消息，查库计数仍从 `0` 到 `0`，状态为
`EXPLICIT_REJECTION_NO_PERSISTENCE`。

该问题不是 PostgreSQL 缺表、缺列、类型兼容或 Oracle 方言残留。WOM 源包 `WOM_6.1.3.4` 中 `WOMProduceTaskController.produceTaskCreated` 仍会调用 `WOMProduceTaskServiceImpl.creatProTask`，但 `creatProTask` 的主体创建逻辑整段被注释，最终直接 `return Result.success("处理成功")`。测试环境运行包补丁见 `deploy/docker/scripts/build-wom-public-produce-created-disabled-boot-jar.sh`，远端原包备份为 `/home/v6/adp-mes-docker-newbase-20260611-181921/runtime/bap-server/module-Server/WOMMs/manual/WOMMs-1.0.0.jar.bak-public-disabled-20260622152535`。

当前已验收的生产指令单生成通道是 `POST /msService/WOM/produceTask/produceTask/produceTaskCreated2`。它经 `WOMFormCreatServiceImpl.creatProduceTask` 生成制造指令单、启动 `makeTaskFlow`，并已用 marker `ADP_E2E_20260619195003_PRODUCE_CREATED2_FIX6` 证明 `wom_produce_tasks`、DI、活动、质量、批次、待办、RBAC 流权限和 JBPM execution 真实落库。

## 证据

| 证据项 | 结果 |
| --- | --- |
| 前端/API 探针 | `POST /msService/public/WOM/produceTask/produceTask/produceTaskCreated` |
| marker | `ADP_E2E_20260622011811_PUBLIC_PRODUCE_NOOP` |
| HTTP 响应 | `200`，业务码 `400`，响应体为 `public produceTaskCreated 已禁用：当前恢复版本不会创建制造指令单，请使用 /msService/WOM/produceTask/produceTask/produceTaskCreated2 或恢复经产品确认的落库实现。` |
| PostgreSQL 验证 | `wom_produce_tasks` 中按 `table_no/produce_batch_num/day_plan_ids` 查 marker，前后均为 `0` |
| 证据文件 | `metadata/wom-public-produce-task-created-noop-probe.json` |
| 源码入口 | `WOMProduceTaskController.produceTaskCreated -> WOMProduceTaskServiceImpl.creatProTask` |
| 源码结论 | `creatProTask` 中从解析 payload 到 `save(...)` 的主体均被注释，最后直接返回成功；测试运行包已临时 patch 为显式禁用 |

## 处理方向

| 路径 | 要求 |
| --- | --- |
| 恢复为真实接口 | 恢复或重写 `creatProTask`，明确 payload 合同，复用 `produceTaskCreated2` 的工作流/表写入边界，重新跑 marker API、浏览器入口和 PostgreSQL 落库验收 |
| 废弃兼容接口 | 不再返回假成功；改为明确失败、禁用或 410，并从产品文档/API 文档中说明制造指令单由 `produceTaskCreated2` 或上游日计划集成生成 |
| 保持现状 | 只能作为 BLOCKED/backlog 保留，禁止用该接口作为任何创建能力的 PASS 证据 |

## Backlog 条目

| ID | 问题 | 处理方向 |
| --- | --- | --- |
| PROD-ACTION-007 | public `produceTaskCreated` 已显式禁用，等待产品确认是否仍是对外承诺接口；恢复源码中的创建主体仍是 no-op 风险 | 先运行 `make probe-wom-public-produce-task-created-noop` 刷新显式禁用证据；产品确认该接口是否仍对外承诺；若承诺则恢复实现并重测，若废弃则把禁用响应写入 API/产品说明 |
