# BPI Kafka/Flink Test Deployment

该目录为智能批次与工艺数据中心提供独立、可回滚的测试环境编排，不复用
`deploy/docker/docker-compose.yml` 中 ADP 遗留的 ZooKeeper Kafka。

## 运行边界

- Kafka 4.2.0：3 个 KRaft combined broker/controller，默认副本数 3、`min.insync.replicas=2`。
- Flink 2.2.1 / Java 17：Application Mode，1 个 JobManager、默认 2 个 TaskManager。
- RocksDB：Flink keyed state backend，增量 checkpoint 写入独立 MinIO bucket。
- BPI 作业：挂载 `bpi-stream-engine-0.1.0-SNAPSHOT-job.jar`，不改变 ADP Java 8 服务。
- MES 上下文发布器：Java 8 独立进程，从 ADP PostgreSQL 事务 outbox 发布
  `mes.production.context.v1`，不修改反编译 WOM 服务。
- 网络：Kafka 外部端口和 Flink REST 默认仅绑定 `127.0.0.1`。

这是单机测试拓扑，不是生产高可用拓扑。生产环境仍需独立 KRaft controller、Flink HA、
TLS/SASL、集中密钥管理、跨节点存储和容量压测。

## 准备

```bash
cp deploy/bpi-streaming/.env.example deploy/bpi-streaming/.env
make bpi-stream-package
make bpi-stream-deploy-preflight
```

必须修改 `.env` 中的 MinIO 密码。若需要从 Tailscale 网络访问 Kafka，将
`BPI_BIND_ADDRESS` 和 `BPI_KAFKA_ADVERTISED_HOST` 同时改为测试机 Tailscale 地址；不要把
明文 Kafka 监听器绑定到公网地址。

`MES_CONTEXT_OUTBOX_ENABLED` 默认是 `false`。启用前必须完成
[`mes-production-context-outbox/README.md`](../../backend/source-modules/mes-production-context-outbox/README.md)
中的迁移、产线绑定、状态语义和 marker 验收。未配置映射的 WOM 变化只会生成 `BLOCKED_*`
审计行，不会发布到 Kafka。

预检是只读操作，会验证 Docker/Compose、空闲磁盘、端口、Compose 渲染和作业 JAR，生成
`/tmp/bpi-streaming-preflight.json`。低于 25 GiB 可用空间时会拒绝启动。

## 启停与验收

启动 Compose 集群前，可先运行不依赖 Docker/PostgreSQL 的本地事务与恢复验收：

```bash
JAVA_HOME=/path/to/jdk17 make bpi-rule-application-flink-acceptance
```

该命令以一次性 Kafka 4.2 KRaft server 和 Flink 2.2.1 MiniCluster 验证未完成 checkpoint 的事务不可见、
成功 checkpoint 后回执可见、TaskManager 重启恢复规则终态及同版本规则禁止重新启用。它使用本地文件
checkpoint，不替代下面的三 broker、MinIO 和目标环境验收。

```bash
make up-bpi-stream
make bpi-stream-cluster-smoke
make bpi-stream-cluster-replay
make bpi-stream-postgres-replay
make down-bpi-stream
```

`up-bpi-stream` 只会启动本目录的 Compose project。`down-bpi-stream` 停止容器但保留 Kafka
和 MinIO named volumes，便于重启和 checkpoint 恢复验证。删除 named volumes 属于破坏性
操作，本仓库不提供自动命令，必须单独审批后执行。

Smoke 必须同时满足：

1. 三个 Kafka broker 正常运行；
2. 八个 BPI topic（包含 rule application 回执、回执 DLQ 与 candidate DLQ）均为副本 3、最小同步副本 2；
3. Flink 作业状态为 `RUNNING`；
4. 至少存在一个成功 checkpoint。

`bpi-stream-cluster-replay` 在 smoke 通过后生成唯一 `ADP_E2E_*` marker，向 Kafka 发布规则、
生产上下文和三条遥测，等待 read-committed 候选，验证候选只出现一次且没有 marker 关联的
数据质量错误；遥测默认间隔 2 秒以覆盖真实调度，随后发布同版本 `INACTIVE` 规则移除测试路由。输入和输出 partition/offset、
candidate key、Flink job ID 和 checkpoint ID 写入
`${BPI_REPLAY_EVIDENCE_DIR}/bpi-kafka-replay.json`。

`bpi-stream-postgres-replay` 在上述回放外再要求运行中的 BPI 服务已显式启用 candidate consumer，
并且仅允许测试租户/工厂：

```dotenv
BPI_CANDIDATE_KAFKA_ENABLED=true
BPI_CANDIDATE_KAFKA_ALLOWED_TENANT_IDS=TENANT-E2E
BPI_CANDIDATE_KAFKA_ALLOWED_PLANT_IDS=PLANT-E2E
BPI_CANDIDATE_KAFKA_ALLOWED_LINE_IDS=*
```

由于 line 包含每次唯一 marker，测试环境只在已经限定 tenant 和 plant 后允许 line `*`。流环境的
Kafka bind/advertised host 与 BPI 服务 bootstrap 地址必须使用容器可达的 Tailscale 地址。脚本会先写入
`TENANT-E2E` 的已发布拓扑/marker 规则 fixture，再执行 Flink 回放，要求 PostgreSQL inbox/candidate
均为 1、candidate 为 `PENDING`、evidence source 为 `bpi-stream-engine`，同时要求 candidate DLQ end
offset 前后不变。默认退出时只清理本 marker 的 candidate/inbox/rule；设置
`BPI_PERSISTENCE_REPLAY_KEEP_MARKER=true` 才保留供浏览器继续确认。

该 replay 证明 Kafka -> Flink -> Kafka 候选数据面。BPI 服务已经具备默认关闭的 candidate consumer
和 PostgreSQL 幂等落库实现；它仍是独立数据面测试，不等于浏览器联合链路。

## 浏览器联合验收

需要复验“浏览器规则发布到影子批次落库”时，使用唯一 marker，并先通过
`deploy/bpi-runtime/sql/joint-acceptance-seed.sql` 准备 topology/rule/golden/history fixture。
fixture 只用于验收，不是产品配置接口。runtime 的 candidate、rule publication 和 rule application
消费者只能对白名单 tenant/plant/line 临时启用。

在真实 BPI 页面完成规则模拟和发布、并确认页面显示“Flink 已应用”后执行：

```bash
make bpi-stream-joint-replay
```

该命令不再自行发布规则，而是要求 Flink 已应用的规则来自浏览器触发的 PostgreSQL outbox，
只发送受控 MES context 和遥测，并要求一个且仅一个候选、零 marker 相关数据质量问题。随后在
真实页面确认候选，并用 `deploy/bpi-runtime/sql/joint-acceptance-verify.sql` 查询候选、批次、证据、
状态事件、审计和幂等行。

验收退场顺序不可颠倒：

```bash
make bpi-stream-rule-deactivate
# 随后执行 joint-acceptance-cleanup.sql，恢复 runtime consumers 默认关闭，再跑浏览器只读 smoke
```

`bpi-stream-rule-deactivate` 从真实 rule topic 读取本次已发布版本，发布 typed `active=false`，并等待
Flink 返回新的 `APPLIED`，避免只删 PostgreSQL 后 Broadcast State 仍保留规则。完整操作、offset、
目标表和清理边界见
[`docs/testing/bpi-browser-kafka-postgres-joint-acceptance.md`](../../docs/testing/bpi-browser-kafka-postgres-joint-acceptance.md)。
WOM/QCS/WMS 写回和 7-14 天影子运行仍是后续门禁。

## 回滚

```bash
make down-bpi-stream
```

若新作业版本失败，先保留 volumes 和 MinIO checkpoint，恢复上一版 job JAR，然后重新执行
预检、启动和 smoke。带负载 TaskManager 恢复已经通过；broker 故障、savepoint 升级和整体
回滚演练仍需补齐。
