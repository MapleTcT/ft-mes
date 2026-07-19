# BPI 数据质量事件工作台验收

验收时间：2026-07-19

源码基线：`b9aed8c8b524c2fd711307dc0402f55e863e2e5e` + 当前功能分支改动

状态：`PASS_LOCAL_POSTGRES_KAFKA_BROWSER_TARGET_PENDING`

## 验收边界

本轮闭合 `DataQualityEventV1 -> Kafka consumer -> PostgreSQL V19 -> Java 17 API -> Java 8 adapter -> BPI 页面` 的本地可重复链路。目标环境 `10.11.100.17` 尚未在本报告中升级到 V19，因此本地 PASS 不代表目标环境已上线。

## 功能结论

| 能力 | 验收方法 | 结果 |
|---|---|---|
| 消息准入 | 校验精确 topic/key/headers、Protobuf、payload 大小、未来时间和 scope allowlist | PASS |
| 聚合与幂等 | 相同 scoped identity 聚合；相同 event replay 不重复写 raw fact | PASS |
| 失败隔离 | 非法 payload 经重试后进入 `bpi.data-quality.dlq.v1` | PASS |
| 业务影响 | 事件关联产线、规则版本和时间重叠批次 | PASS |
| 查询与分页 | 影响批次、严重度、最后时间排序；HMAC scope-bound snapshot-cutoff keyset cursor 防篡改；截点后变化需刷新 | PASS |
| 权限 | VIEWER 可读不可处置；SHIFT_LEAD/ENGINEER/ADMIN 可执行受控命令 | PASS |
| 生命周期 | `OPEN -> ACKNOWLEDGED -> RESOLVED`，ACK 状态可重新分派 | PASS |
| 迟到与重开 | 旧迟到事件保留但不重开；解决后更新的事件重开并清理旧处置字段 | PASS |
| 原始事实 | 解决后 raw event 数量不变，生命周期和审计只追加 | PASS |
| 适配器 | `/bpi-api` 仅放行 5 个数据质量路由，替换旧 token 并转发幂等/revision 头 | PASS |
| 桌面页面 | 汇总、筛选、详情、分派、重新分派、解决、证据和时间线 | PASS |
| 移动页面 | `390x844` 无页面级横向溢出，底部导航可进入数据质量页 | PASS |

## 自动化证据

| 验证 | 结果 |
|---|---|
| `DataQualityKafkaRecordProcessorTest` | 4/4 PASS |
| `BpiDataQualityKafkaPostgresAcceptanceTest` | 2/2 PASS，真实 PostgreSQL + Embedded Kafka |
| Java 17 reactor | 6 tests，0 failure，0 error，Flyway V19 |
| 确定性模拟器 | 9/9 PASS，41 个模拟公开操作全部被测试覆盖 |
| BPI Playwright | 13/13 PASS，console/page/request error 为 0 |
| Java 8 adapter | 9/9 PASS |
| TypeScript/Vite build | PASS |
| `scripts/verify-bpi-service.py` | PASS |

首次 Java 17 runner 因测试容器未显式设置 `POSTGRES_USER` 而错误回退为系统用户 `root`，数据库认证失败。改为 PostgreSQL 默认用户后原样重跑通过；该问题属于验收编排，不是业务测试通过记录的一部分。

## PostgreSQL 断言

验收测试直接查询并断言：

```sql
SELECT state, revision, severity, event_count
FROM bpi.bpi_data_quality_incidents
WHERE tenant_id = :marker_tenant;

SELECT count(*)
FROM bpi.bpi_data_quality_incident_events
WHERE tenant_id = :marker_tenant;

SELECT action, incident_revision
FROM bpi.bpi_data_quality_incident_actions
WHERE tenant_id = :marker_tenant
ORDER BY incident_revision;

SELECT count(*)
FROM bpi.bpi_audit_events
WHERE tenant_id = :marker_tenant
  AND object_type = 'DATA_QUALITY_INCIDENT';
```

测试在每个用例结束后按 marker tenant 清理 incident、raw event、action、audit、idempotency、inbox、batch、rule 和 topology 行，不保留伪业务数据。

## 尚未闭合

1. 将 V19、Java 17 service、Java 8 adapter 和 BPI 前端部署到 `10.11.100.17`。
2. 在目标 Kafka 创建/核对 `bpi.data-quality.v1` 与 DLQ，并以精确 allowlist 启用 consumer。
3. 使用唯一 `ADP_E2E_*` marker 完成真实浏览器、API、Kafka offset、PostgreSQL 落表和清理复验。
4. 让真实 Flink 作业产生数据质量事件；本地 Embedded Kafka 不能替代真实集群证据。
