# BPI 数据质量事件工作台验收

验收时间：2026-07-19

部署源码：`297e0aaffeae69dfe68cb2f3b23a62c4910625d7`

状态：`PASS_TARGET_POSTGRES_KAFKA_BROWSER_CLEANUP`

## 验收边界

本轮闭合 `DataQualityEventV1 -> 目标 Kafka 4.2 -> Java 17 consumer -> PostgreSQL V19 ->
Java 17 API -> Java 8 adapter -> 真实 ADP 登录后的 BPI 页面`。唯一 marker 完成
`OPEN/r1 -> ACKNOWLEDGED/r2 -> RESOLVED/r3`，随后数据库定向清理、consumer 关闭并恢复
`_DENY_ALL_`。Kafka 追加日志中的 marker 按消息系统语义保留，不以破坏性方式删除。

该 PASS 证明数据质量事件工作台在测试环境可部署、可处置、可落库和可清理；不代表真实 Flink 作业
已经自动产生此类事件，也不替代选定产线 7-14 天连续影子运行和业务签字。

## 功能结论

| 能力 | 验收方法 | 结果 |
|---|---|---|
| 消息准入 | 校验精确 topic/key/headers、Protobuf、payload 大小、未来时间和 scope allowlist | PASS |
| 聚合与幂等 | 相同 scoped identity 聚合；相同 event replay 不重复写 raw fact | PASS |
| 失败隔离 | 非法 payload 经重试后进入 `bpi.data-quality.dlq.v1` | PASS |
| 业务影响 | 事件关联产线、规则版本和时间重叠批次 | PASS |
| 查询与分页 | 影响批次、严重度、最后时间排序；HMAC scope-bound snapshot-cutoff keyset cursor 防篡改 | PASS |
| 权限 | VIEWER 可读不可处置；SHIFT_LEAD/ENGINEER/ADMIN 可执行受控命令 | PASS |
| 生命周期 | `OPEN -> ACKNOWLEDGED -> RESOLVED`，ACK 状态可重新分派 | PASS |
| 迟到与重开 | 旧迟到事件保留但不重开；解决后更新事件重开并清理旧处置字段 | PASS |
| 原始事实 | 解决后 raw event 数量不变，生命周期和审计只追加 | PASS |
| 适配器 | `/bpi-api` 仅放行 5 个数据质量路由，替换旧 token 并转发幂等/revision 头 | PASS |
| 桌面页面 | 汇总、筛选、详情、分派、重新分派、解决、证据和时间线 | PASS |
| 移动页面 | `390x844` 无页面级横向溢出，底部导航可进入数据质量页 | PASS |
| 目标全链 | 真实 ADP 登录、Kafka marker、API 状态机、PostgreSQL 直查、浏览器零错误与清理 | PASS |

## 本地自动化证据

| 验证 | 结果 |
|---|---|
| `DataQualityKafkaRecordProcessorTest` | 4/4 PASS |
| `BpiDataQualityKafkaPostgresAcceptanceTest` | 2/2 PASS，真实 PostgreSQL + Embedded Kafka |
| Java 17 reactor | 6 tests，0 failure，0 error，Flyway V19 |
| 确定性模拟器 | 9/9 PASS，41 个模拟公开操作全部被测试覆盖 |
| BPI Playwright | 13/13 PASS，console/page/request error 为 0 |
| Java 8 adapter | 数据质量聚焦 9/9；完整模块 18/18 PASS |
| TypeScript/Vite build | PASS |
| `scripts/verify-bpi-service.py` | PASS |

首次 Java 17 runner 因测试容器未显式设置 `POSTGRES_USER` 而错误回退为系统用户 `root`，数据库
认证失败。改为 PostgreSQL 默认用户后原样重跑通过；该问题属于验收编排，不计入业务 PASS。
最终回归还发现一只实现早期保留的 V19 本地容器与当前迁移校验和不同；未执行 Flyway `repair`，而是
启动自动回收的全新 PostgreSQL 16，从 V1 到 V19 干净迁移后原样跑完 6/6，以当前源码为验收基准。

## 目标环境部署

| 项目 | 证据 |
|---|---|
| 地址/Compose project | `10.11.100.17`；唯一 ADP project `adp-mes-newbase` |
| 部署前备份 | `/home/v6/bpi-deploy-backups/20260719-212455-data-quality-v19-297e0aaf`；含 custom dump、restore list、Compose、受保护环境、前端、迁移和镜像清单 |
| PostgreSQL | 15.18；`bpi.flyway_schema_history` 最新 `19|t`；V19 SQL 本地/目标 SHA-256 一致 |
| Java 17 service | `ft-mes-bpi-service:20260719-data-quality-v19-297e0aaf`；image `sha256:50355dee...346a3`；healthy/UP |
| Java 8 adapter | `ft-mes-bpi-adapter:20260719-data-quality-v19-297e0aaf`；image `sha256:611881ab...2d37`；healthy |
| BPI 前端 | `/bpi/` HTTP 200；`index-CYGrgfzV.js`、`index-Beow3bJt.css` 已部署 |
| Compose 健康 | 65 个容器，62 running，0 unhealthy；3 个 exited 均为退出码 0 的一次性任务 |

目标主机不能从 GitHub SSH 拉取，部署使用 `git archive` 导出的 `297e0aaf` 源码子集和本地已测试、
哈希一致的架构无关 JAR 构建运行镜像。部署未引入运行包、dump 或密钥到 Git 仓库。

## Kafka 启用策略

`bpi.data-quality.v1` 和新建 DLQ 均为 6 分区、复制因子 3、`min.insync.replicas=2`；DLQ
retention 为 30 天。源 topic 在上线前已有 34 条历史消息。为避免新消费者把未知历史消息误判并灌入
DLQ，先在 consumer 关闭和 deny-all 状态下把组 `ft-mes-bpi-service-data-quality-v1` 初始化到当时
各分区末端，再以精确 `1000 / PLANT-01 / LINE-S07-01` allowlist 临时启用 marker 验收。

| 分区 | 初始化 offset | 验收后 committed/log end | lag |
|---|---:|---:|---:|
| 0 | 10 | 10/10 | 0 |
| 1 | 2 | 2/2 | 0 |
| 2 | 5 | 6/6 | 0 |
| 3 | 2 | 2/2 | 0 |
| 4 | 8 | 8/8 | 0 |
| 5 | 7 | 7/7 | 0 |

验收结束后 consumer 无活跃成员；DLQ 六分区 end offset 均为 0。

## 唯一 Marker 全链

Marker：`ADP_E2E_DQ_20260719_215100_297E0AAF`

Incident：`e2f5e042-5c3a-5370-99a1-935fd56d664d`

| 阶段 | 实际结果 |
|---|---|
| Kafka 生产 | `bpi.data-quality.v1` partition 2 offset 5；event ID 为 marker + `_EVENT_1` |
| 消费落库 | committed offset 由 5 到 6；incident `OPEN/r1`；raw event 1、inbox 1、`CREATED` 1 |
| 浏览器确认 | 真实 ADP 登录 HTTP 200；分派给 marker owner；`ACKNOWLEDGED/r2` |
| 浏览器解决 | `POST .../resolve` HTTP 200；`RESOLVED/r3`；raw event 仍为 1 |
| 生命周期 | `CREATED@r1, ACKNOWLEDGED@r2, RESOLVED@r3` |
| 审计/幂等 | audit 3；API idempotency 2，均 `COMPLETED` |
| 前端错误 | console 0、page error 0、request failure 0；数据质量 API 全部 2xx |

首次浏览器执行已经成功提交 ACK，但 runner 的状态文本 locator 同时命中两处 `ACKNOWLEDGED`，
因此在 ACK 后停止。脚本改为作用域内精确 locator 并保持可恢复后，第二次从已确认状态继续解决，最终
完整生命周期和数据库 revision 均符合预期。这是验收脚本选择器问题，不是接口回滚或重复业务动作。

浏览器报告 SHA-256：
`343c45c47869e142d6a0c055c0996bdf579a2c9abbdc4ebbf1dd20870a7474a7`

截图 SHA-256：
`e868892b8fa3175255d880b937c029d0a645c0580f89e2fad007e9b05cc61b80`

## PostgreSQL 断言与清理

处置完成、清理前直接查询并确认：

```sql
SELECT state, revision, event_count, assignee, acknowledged_by, resolved_by
FROM bpi.bpi_data_quality_incidents
WHERE id = 'e2f5e042-5c3a-5370-99a1-935fd56d664d';

SELECT action, incident_revision
FROM bpi.bpi_data_quality_incident_actions
WHERE incident_id = 'e2f5e042-5c3a-5370-99a1-935fd56d664d'
ORDER BY incident_revision;
```

结果为 `RESOLVED/r3`、raw 1、action 3、audit 3、idempotency 2、inbox 1。清理在 consumer 再次
关闭后以事务执行，并复验 incident、raw event、action、audit、idempotency、inbox 均为 0。最终配置：

```text
BPI_DATA_QUALITY_KAFKA_ENABLED=false
BPI_DATA_QUALITY_KAFKA_ALLOWED_TENANT_IDS=_DENY_ALL_
BPI_DATA_QUALITY_KAFKA_ALLOWED_PLANT_IDS=_DENY_ALL_
BPI_DATA_QUALITY_KAFKA_ALLOWED_LINE_IDS=_DENY_ALL_
```

## 仍未闭合

1. 真实 Flink 作业自动产生数据质量事件的生产路径尚未验收；本轮 marker 由受控 Protobuf producer 发送。
2. 选定真实产线仍需 7-14 天连续影子运行、告警质量统计、运维演练和业务签字。
3. 上述两项不影响工作台目标环境验收 PASS，但继续阻止 BPI 总目标升级为生产 READY。
