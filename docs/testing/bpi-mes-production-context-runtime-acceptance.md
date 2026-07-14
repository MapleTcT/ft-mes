# BPI MES Production Context Runtime Acceptance

验收时间：2026-07-14
目标环境：`ubuntu-test` / `100.99.133.43`
数据库：ADP PostgreSQL + BPI PostgreSQL
结论：`PASS_CONTROLLED_TELEMETRY`

## 验收边界

本次证明真实 WOM 页面动作能够在同一 PostgreSQL 事务中形成 production context outbox，
经 Java 8 发布器进入 Kafka，再由持续运行的 Flink 作业与受控遥测关联，最终通过真实 BPI 页面
确认候选并落成影子批次。遥测仍由受控回放器产生，不是现场 JetLinks 设备事件，因此不能据此
宣称 BPI 已完成现场联调或可投产。

## 真实证据链

| 环节 | 入口 / API | PostgreSQL / Kafka 证据 | 结果 |
|---|---|---|---|
| WOM 页面开始 | `/msService/WOM/produceTask/produceTask/makeTaskList` -> `POST .../updateTaskState`，`state=start` | 任务 `9007190327104029` 进入 `WOM_runState/runing`，产线 `8388157374858567` | PASS |
| 上下文 outbox | WOM trigger + Java 8 dispatcher | 指令 `ADP_E2E_20260714_203900_WOM_CTX_REVFIX_TASK_TN` 产生 inactive/active 版本 `1784032711352/1784032725227`，均为 `SENT|1` | PASS |
| Kafka/Flink join | `mes.production.context.v1` + `iot.telemetry.selected.v1` | context `partition=1, offset=4`；candidate `partition=5, offset=2`；Flink checkpoint `673`，30/30 tasks running | PASS |
| 唯一候选 | `bpi.batch.candidate.v1` | `candidateKey=88db971a-dceb-570e-8b84-968ec8e145e4`，matching record `1`，data-quality issue `0` | PASS |
| 页面确认 | `POST /bpi-api/candidates/6f3638ed-5d59-5568-acba-caf13c07e3b9/confirm` | candidate `CONFIRMED/r2`；shadow batch `813f74e8-dbe9-5cb1-a07b-74b0ea08d815` 为 `ACTIVE/r1` | PASS |
| 落库复验 | BPI PostgreSQL direct SQL | 2 条 boundary evidence、1 条 state event、3 条相关 audit event | PASS |
| WOM 页面保持 | `POST .../updateTaskState`，`state=hold` | inactive revision `1784033048507`，`source_state=wom_runstate/iskeep`，`SENT|1` | PASS |
| 失活上下文拒绝 | 外部回放再次请求同一真实指令 | 最新版本为 inactive 时返回预期 `FAIL`；6 个遥测分区 offset 前后完全一致 | PASS_FAIL_CLOSED |
| 受控收尾 | typed rule deactivation + marker cleanup | Flink `APPLIED` inactive；规则/候选/批次剩余均为 0；消费者恢复 `_DENY_ALL_` | PASS |

## 发现并修复的问题

第一次外部上下文回放正确失败：旧合成验收在同一 scope 留下了高版本、仍激活的上下文，
导致新遥测被归到旧合成订单。候选虽然生成，但真实指令匹配数为 0。

修复包括：

1. 新增 `177-wom-bpi-context-revision-clock-floor.sql`，WOM 产线版本以当前毫秒时钟为下限，
   同一毫秒内继续按行锁后的 `last_revision + 1` 递增。
2. 合成联合回放在成功或失败后都发送 typed inactive closing context，避免测试上下文长期激活。
3. 外部回放显式按真实 `order_id` 选择 MES outbox context，候选匹配也必须带同一指令号。
4. 浏览器确认、SQL 复验和清理脚本支持显式真实 `order_id`，默认合成模式保持兼容。
5. Java 8 enabled-mode Spring 启动测试覆盖真实 dispatcher 构造器，防止发布器只在关闭模式可启动。
6. MES outbox 回放读取 topic 的稳定末端后，只选择同一 `order_id` 的最新版本；最新版本 inactive
   时 fail closed，不再回退到历史 active 版本。
7. 联合验收容器使用 Compose `--no-deps`，避免临时验收 JAR 路径触发常驻 JobManager 重建。

## 验证与恢复

- Stream reactor：93 tests，0 failure，1 skipped。
- MES context module：Java 8 单元/上下文测试通过；独立 PostgreSQL 验收通过。
- JavaScript、shell、Compose 和部署资产静态校验通过。
- 目标机 `mes-production-context-outbox` 为 `enabled=true/healthy`。
- BPI service 为 `healthy`，Flink 作业为 `RUNNING`，30/30 tasks running。
- 测试规则、候选、批次和 WOM fixture 已清理；outbox 审计行按设计保留。

负向验收首次使用临时 Compose 环境文件时触发了 JobManager 重建，暴露出验收编排未隔离依赖
的问题。已恢复原 `.env` 与常驻 JAR，并复验新作业 `8b55f27300e328fe31733ce24b75c8bc`
为 `RUNNING`、30/30 tasks、completed checkpoint `53`；脚本随后增加 `--no-deps`，最终负向
回放确认 JobManager 容器身份与启动时间均未变化。

## 剩余投产门槛

1. 用 `MapleTcT/iot` 真实 exporter 事件替换受控遥测，确认设备、点位、单位、质量码、
   `source_epoch/sequence` 与本次 production context 同 scope。
2. 完成真实开始/保持/重启/结束全状态映射和至少一条 END 规则。
3. 连续 7–14 天运行单线影子批次，统计人工认同率、边界偏差、累计量偏差和数据质量分布。
4. 在影子门槛通过前，QCS/WMS 只读，不启用生产写回。

机器可读证据见
[`metadata/bpi-mes-production-context-runtime-acceptance.json`](../../metadata/bpi-mes-production-context-runtime-acceptance.json)。
