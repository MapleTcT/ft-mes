# BPI Kafka Cluster Replay Acceptance

## 当前结论

状态为 **LEGACY_HARNESS_SUPERSEDED / TARGET_ACCEPTED**。

2026-07-23 在 `10.11.100.17` 重新执行只读容量预检和当前仓库版本的 24-topic cluster smoke：

- 根分区可用 `76,948,936 KiB`（约 `73.38 GiB`），Docker root
  `/data/docker/docker` 可用 `1552 GiB`，25 GiB 门槛通过；
- Kafka 3 broker、24 个治理 topic、副本 3、`min.insync.replicas=2` 全部通过；
- Flink 作业 `ft-mes-bpi-batch-boundary-v1` 保持 `RUNNING`，job ID
  `40f36698aeee4aaae17eac52608c7939`，2 个 TaskManager，完成 checkpoint `14905`；
- 当前 smoke 报告：
  `/home/v6/adp-mes-backups/bpi-v35-live-67b728da/bpi-streaming-smoke-current-d6bfaa5a.json`，
  SHA256 `baaf0d963107c0f4f6f4afe5df7774d2b5779098cb18390e0f70e585ad48e6af`。

旧的 `make bpi-stream-cluster-replay` **没有在当前目标环境重跑**。它的点位目录 fixture 使用
`sha256:<marker>`、marker event ID、旧 key 和通用 headers，不满足当前 canonical payload SHA、
`point-catalog-<digest>` event ID、`tenant|plant|line|source_instance` key 及 source headers 契约。
直接运行会让当前 BPI point-catalog consumer 产生 DLQ，不能把这种结果冒充验收。

## 替代验收链

当前能力由更新后的真实目标证据闭合：

```text
real browser rule simulation/publication
  -> PostgreSQL outbox
  -> Kafka rule publication
  -> Flink APPLIED receipt
  -> scoped context/telemetry
  -> exactly one committed candidate
  -> PostgreSQL inbox/candidate/batch/evidence/state/audit
  -> real browser confirmation
  -> typed INACTIVE
  -> marker cleanup and consumer deny-all restore
```

marker `ADP_E2E_20260714_091536_BPI_JOINT` 为 11/11 PASS，详见
[`metadata/bpi-browser-kafka-postgres-joint-acceptance.json`](../../metadata/bpi-browser-kafka-postgres-joint-acceptance.json)。
Flink 数据质量的四类输出、PostgreSQL 落库、页面展示及零残留清理另见
[`metadata/bpi-flink-data-quality-acceptance.json`](../../metadata/bpi-flink-data-quality-acceptance.json)。

## 旧夹具处置

旧 Java 本地契约测试数字保留为历史证据，不改写成新的实机执行。脚本默认 fail-closed；只有点位目录和
telemetry consumer 均已隔离的专用兼容环境，才允许一次性设置：

```bash
BPI_LEGACY_REPLAY_COMPATIBILITY_ACK=ISOLATED_BPI_SOURCE_CONSUMERS \
  make bpi-stream-cluster-replay
```

该确认值不得写入 `.env` 或生产编排。当前产品验收使用 browser joint replay、数据质量 replay 和相应
PostgreSQL 清理脚本。

## 边界

- 当前证据关闭了旧的 `BLOCKED_DISK` 和 target-cluster pending 结论。
- 未证明物理 IoT 来源覆盖、JobManager HA、跨主机容灾、10 万点容量或 7-14 天现场影子运行。
- `SUPERSEDED` 表示旧夹具被新版整链取代，不表示旧脚本在当前目标执行成功。
