# BPI 规则运行时就绪回执验收

## 结论

2026-07-15 本地三层验收为 `PASS_LOCAL_MULTI_LAYER_TARGET_NOT_RUN`。控制面规则应用状态
`APPLIED/REJECTED` 与流式评估器运行时状态 `READY/DEGRADED/INACTIVE` 已形成独立契约、
Flink 输出、Kafka 消费、PostgreSQL 字段和操作台展示。该结论不冒充目标环境部署或生产就绪。

机器记录：`metadata/bpi-rule-runtime-readiness-acceptance.json`。

## 验收矩阵

| 层 | 运行时 | 关键动作 | 实际结果 | 状态 |
|---|---|---|---|---|
| 数据面 | Flink 2.2.1 MiniCluster + Kafka 4.2 KRaft | checkpoint 前后可见性、停用、TaskManager 重启、同版本重启用 | checkpoint 前 `read_committed` 不可见；提交后得到 `APPLIED + READY`；停用得到 `APPLIED + INACTIVE`；恢复后拒绝同版本重启用；无重复回执 | PASS |
| 消费与落库 | Embedded Kafka 3.8.1 + PostgreSQL 16.13 + Flyway V13 | 事务提交/回滚、listener 重启、过期事件、精确重放、双 DLQ | 最终 `application=APPLIED`、`runtime=READY`、outbox revision `5`；application/readiness inbox 各 `3`，audit `4`；两类坏消息进入各自 DLQ | PASS |
| 操作台 | Chromium E2E + 确定性模拟器 | 打开规则抽屉，先注入 DEGRADED 再注入 READY | `APPLIED` 始终保持；运行时从 `DEGRADED` 转为 `READY`；拒绝原因和目录 revision 正确清理；浏览器错误为 0 | PASS_SIMULATOR |

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

- 目标环境仍保留历史 Flyway V12 证据，本次没有部署 V13。
- 目标三 broker Kafka、MinIO checkpoint/savepoint 升级与回滚还需实机演练。
- 真实 JetLinks 点位尚未 READY，不能进行真实设备遥测、MES context、candidate/batch 同 marker 闭环。
- 浏览器、Java、Kafka、Flink、PostgreSQL 的单一目标环境联合 marker 和 7-14 天影子运行仍待完成。
