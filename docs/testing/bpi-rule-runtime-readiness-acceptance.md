# BPI 规则运行时就绪回执验收

## 结论

2026-07-15 本地与目标环境综合结论为
`PASS_LOCAL_AND_TARGET_FAIL_CLOSED_SOURCE_BLOCKED`。控制面规则应用状态
`APPLIED/REJECTED` 与流式评估器运行时状态 `READY/DEGRADED/INACTIVE` 已形成独立契约、
Flink 输出、Kafka 消费、PostgreSQL 字段和操作台展示。目标环境已完成 V13 扩展迁移和
V12 savepoint 到 V13 作业的有状态恢复；真实 JetLinks 点位当前未就绪，规则发布被 API
`422` 正确阻断，未创建 outbox、Kafka 规则消息、候选或批次。

这里的目标环境 PASS 只表示“升级成功且来源准入正确 fail closed”，不表示真实点位已 READY，
也不表示 BPI 已达到生产就绪。

机器记录：`metadata/bpi-rule-runtime-readiness-acceptance.json`。

## 验收矩阵

| 层 | 运行时 | 关键动作 | 实际结果 | 状态 |
|---|---|---|---|---|
| 数据面 | Flink 2.2.1 MiniCluster + Kafka 4.2 KRaft | checkpoint 前后可见性、停用、TaskManager 重启、同版本重启用 | checkpoint 前 `read_committed` 不可见；提交后得到 `APPLIED + READY`；停用得到 `APPLIED + INACTIVE`；恢复后拒绝同版本重启用；无重复回执 | PASS |
| 消费与落库 | Embedded Kafka 3.8.1 + PostgreSQL 16.13 + Flyway V13 | 事务提交/回滚、listener 重启、过期事件、精确重放、双 DLQ | 最终 `application=APPLIED`、`runtime=READY`、outbox revision `5`；application/readiness inbox 各 `3`，audit `4`；两类坏消息进入各自 DLQ | PASS |
| 操作台 | Chromium E2E + 确定性模拟器 | 打开规则抽屉，先注入 DEGRADED 再注入 READY | `APPLIED` 始终保持；运行时从 `DEGRADED` 转为 `READY`；拒绝原因和目录 revision 正确清理；浏览器错误为 0 | PASS_SIMULATOR |
| 目标升级 | `10.11.100.17`、PostgreSQL V13、Flink/Kafka 三 broker | V12 数据备份、expand-only 迁移、savepoint 捕获与恢复、恢复后 checkpoint | Flyway 最高版本 `13`；V13 服务健康；V12 savepoint 恢复为 job `40408b7907ca7b97ad750cc7d2bfb345`；12 个 topic 均无 under-replicated partition | PASS |
| 目标来源准入 | 真实 ADP 登录、BPI 页面、Java、PostgreSQL、Kafka | marker 历史回放后尝试发布绑定真实 JetLinks 点位的规则 | 模拟 `202`；发布 `422`；中文业务提示无内部错误码；数据库 `outbox/candidate/batch=0`；Kafka marker `0`；marker 定向清理完成 | PASS_FAIL_CLOSED |
| 目标运行时回执 | 真实 JetLinks 点位到 Kafka/Flink/PostgreSQL | 发布 READY 规则并等待 `APPLIED + READY/DEGRADED` | 当前目录 `readyPointCount=0`，设备 inactive、未注册、属性不可用、单位缺失、标定未验证且来源序列未启用，不能合法进入发布链路 | BLOCKED_BY_SOURCE |

## PostgreSQL 落表

V13 在 `bpi.bpi_outbox_events` 增加独立运行时字段。真实 Kafka 消费测试在清理前直接执行
JDBC 断言：

```sql
SELECT application_status,
       runtime_readiness_status,
       runtime_readiness_deployment_id,
       runtime_readiness_reason_code,
       runtime_point_catalog_event_id,
       runtime_point_catalog_source_revision,
       revision
  FROM bpi.bpi_outbox_events
 WHERE tenant_id = :tenant
   AND id = :publication_id;

SELECT source, count(*)
  FROM bpi.bpi_inbox_events
 WHERE tenant_id = :tenant
 GROUP BY source
 ORDER BY source;

SELECT action, before_revision, after_revision
  FROM bpi.bpi_audit_events
 WHERE tenant_id = :tenant
   AND object_id = :publication_id
 ORDER BY after_revision;
```

最终断言为 `APPLIED + READY / revision=5`，审计顺序包含规则应用的 REJECTED/APPLIED 和
运行时的 DEGRADED/READY。测试使用随机 `ADP_E2E_BPI_RULE_APP_KAFKA_*` tenant marker，
直接查询通过后才在 `@AfterEach` 定向删除，不保留测试脏数据。

## 目标环境落库与反证

目标 marker `ADP_E2E_20260715_184156_BPI_V13_UI` 通过真实页面执行历史回放和发布尝试。
发布前后直接查询 PostgreSQL，结果为：

```text
rule_state=SIMULATION_PASSED, rule_revision=2, simulations=1
outbox=0, candidates=0, batches=0, boundaryEvidence=0, stateEvents=0
Kafka bpi.boundary.rule.v1 marker count=0
```

这证明接口 `422` 不只是页面提示，后端事务也没有误发布。清理事务执行后，marker 的
topology、rule、golden boundary、telemetry 和临时 feature flag 均为 `0`，既有已启用的
`bpi.rule-management` 仍为 `1`。清理后重新访问概览页，登录 `200`，页面标题“智能批次”，
console/page/request 非预期错误均为 `0`，运行时 smoke 为 PASS。

当前点位目录的真实快照为：

| 字段 | 实际值 |
|---|---|
| source / instance | `JETLINKS / jetlinks-pilot-node-01` |
| source revision | `sha256:2a218d12d6ed8bea024c38f6d2e06656f20703fadf920256dc98b17c2f151ce5` |
| product / device / property | `bpi-pilot-product-01 / bpi-pilot-device-01 / flow.instant` |
| point count / READY | `1 / 0` |
| 阻断事实 | `INACTIVE`、未注册、属性不可用、单位缺失、标定未验证、来源序列未启用 |

目标环境原始证据保存在 `/data/docker/bpi-upgrade-evidence/`：

- `bpi-v13-savepoint-restore.json`
- `bpi-runtime-v13-expand.json`
- `bpi-v13-ui-localization-ADP_E2E_20260715_184156_BPI_V13_UI.json`
- `bpi-v13-ui-localization-postgres-ADP_E2E_20260715_184156_BPI_V13_UI.json`
- `bpi-v13-ui-localization-kafka-marker-count-ADP_E2E_20260715_184156_BPI_V13_UI.txt`
- `bpi-v13-ui-localization-cleanup-ADP_E2E_20260715_184156_BPI_V13_UI.json`
- `bpi-v13-ui-localization-read-ADP_E2E_20260715_184156_BPI_V13_UI.json`
- `bpi-v13-source-catalog-20260715.json`

## 复验命令

```bash
make bpi-rule-application-flink-acceptance

BPI_TEST_DATABASE_URL=jdbc:postgresql://127.0.0.1:<port>/<db> \
BPI_TEST_DATABASE_USER=<user> \
BPI_TEST_DATABASE_PASSWORD=<password> \
JAVA_HOME=/path/to/jdk17 \
mvn -f acceptance/bpi-runtime/pom.xml -pl :bpi-service -am \
  -Dtest=BpiRuleApplicationKafkaPostgresAcceptanceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

make bpi-ui-build bpi-ui-test
```

## 尚未完成

- 将 `bpi-pilot-device-01 / flow.instant` 修复为真实 READY：设备注册并激活、属性发现、单位、
  标定版本和来源序列证据全部有效。
- READY 点位下完成目标环境规则发布、Kafka 分发、Flink `APPLIED` 与运行时
  `READY/DEGRADED` 回执的同 marker 验收。
- 接入真实 `MapleTcT/iot` exporter 与 MES production context，完成 candidate、batch、证据、
  质量/WMS 写回的真实业务闭环。
- 完成 7-14 天影子运行、故障恢复和应用镜像回退演练；V13 schema 按 expand-only 设计保留，
  不执行破坏性降级。
