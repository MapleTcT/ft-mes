# WOM public produceTaskCreated 退役决策与验收

## 结论

`POST /msService/public/WOM/produceTask/produceTask/produceTaskCreated` 已正式废弃，不再是受支持的生产工单/制造指令单创建入口。为了兼容旧调用方快速发现迁移问题，URL 继续保留，但固定返回 `HTTP 200/code=400` 和“已废弃”业务提示，且绝不落库。

原恢复源码的 `WOMProduceTaskServiceImpl.creatProTask` 创建主体被整段注释，未打补丁时会返回“处理成功”，但 PostgreSQL `wom_produce_tasks` 没有任何变化。恢复可见 WOM 版本和当前维护仓库的全量字符串检索只找到服务端定义和历史分析，没有前端或业务模块调用方。旧代码还包含模拟登录、字符串拼接 SQL 和主工单/包装子工单的长事务，直接复活会带回越权、SQL 注入和半成功风险。

受支持的产品入口为：

- 认证后的日计划集成：`POST /msService/WOM/produceTask/produceTask/produceTaskCreated2`。
- 认证后的前端人工录入：`makeTaskList -> 新建指令单 -> /msService/WOM/produceTask/manual-entry/page`。

## 真实验收

2026-07-21 在 `10.11.100.17` 执行 marker
`ADP_E2E_20260721104747_PUBLIC_PRODUCE_RETIRED`，机器证据为
`metadata/wom-public-produce-task-created-retirement-acceptance.json`。

| 验收项 | 实际结果 |
| --- | --- |
| 旧 API | `HTTP 200/code=400`，提示“public produceTaskCreated 已废弃且不会创建制造指令单” |
| PostgreSQL | 按 `table_no/produce_batch_num/day_plan_ids` 查 marker，前后均为 `0`，即 `0 -> 0` |
| 真实浏览器 | `makeTaskList=200`，“新建指令单”可见，`manual-entry/page` 成功加载 2 个产品选项 |
| 前端错误 | console/page/request/network error 均为 `0` |
| 运行包 | SHA-256 `d8a6d32fc67ef861ad8355bd03b842727a75ece252410c0ab567223907b3514a` |
| 实现提交 | `6fac3120734ec14a343cae9ccf232e440c3b1dc8` |
| 回退点 | `/home/v6/adp-mes-docker-newbase-20260611-181921/runtime/bap-server/module-Server/WOMMs/manual/WOMMs-1.0.0.jar.bak-public-retirement-20260721184111` |

复验命令：

```bash
make acceptance-wom-public-produce-task-created-retirement \
  ADP_BASE_URL=http://10.11.100.17:18080 \
  ADP_BROWSER_BASE_URL=http://10.11.100.17:18080 \
  ADP_SSH_HOST=10.11.100.17
```

`make probe-wom-public-produce-task-created-noop` 作为旧命令别名保留，但现在执行的也是退役合同验收。历史假成功证据仍保留在 `metadata/wom-public-produce-task-created-noop-probe.json`，不能用来宣称当前接口可创建任务。

## 产品边界

`PROD-ACTION-007` 已按 `NOT_APPLICABLE` 关闭：这是“废弃契约验收 PASS”，不是“制造指令创建 PASS”。真实创建能力继续由 `produceTaskCreated2` 和“新建指令单”两条独立 marker 证据承担。如果未来重新提出对外公开创建 API，必须新建带认证、幂等键、请求账本、明确 payload schema 和 PostgreSQL 事务验收的 v2 契约，不允许把本旧实现去注释后直接启用。
