# BPI Kafka/Flink/PostgreSQL 联合回放验收

## 当前状态

联合验收入口 `make bpi-stream-postgres-replay` 已实现，状态为
`HARNESS_READY_CLUSTER_BLOCKED_DISK`。本地嵌入式真实 Kafka + PostgreSQL 16 已验证 candidate listener、
重复投递幂等和 DLQ；测试机三节点 Kafka/Flink 尚未启动，因此本报告不能标记实机 PASS。

## 验收链

```text
marker topology/rule fixture (PostgreSQL)
  -> rule/context/telemetry (Kafka)
  -> Flink BatchCandidateV1
  -> read_committed Spring Kafka listener
  -> bpi_inbox_events = 1
  -> bpi_batch_candidates = 1, PENDING
  -> candidate DLQ end offset unchanged
```

## 前置条件

- `make bpi-stream-deploy-preflight` 已通过，空闲磁盘至少 25 GiB；
- 三节点 topic、Flink RUNNING job 和 completed checkpoint smoke 已通过；
- BPI 服务镜像包含 `ce5cace` 或后续提交；
- candidate consumer 显式启用；allowlist 包含 `TENANT-E2E / PLANT-E2E / line=*`；
- 流集群 advertised host 与 BPI consumer bootstrap 在私有 Tailscale 网络互通；
- `deploy/bpi-streaming/.env` 和 `deploy/docker/.env` 均存在且不提交 Git。

## 通过标准

| 证据 | 标准 |
|---|---|
| Flink | job `RUNNING` 且存在 completed checkpoint |
| Candidate topic | marker 只有 1 个合法候选 |
| PostgreSQL inbox | candidate key 对应 1 行 |
| PostgreSQL candidate | 1 行、`PENDING`、source=`bpi-stream-engine` |
| Candidate DLQ | 回放前后 end offset 不变 |
| 清理 | 默认只删除本 marker 的 candidate/inbox/rule |

当前 `ubuntu-test` 仅 `538,288 KiB`（约 0.51 GiB）可用，低于 25 GiB 门槛。未执行 prune、删除容器、镜像、volume 或
历史目录；需要用户先明确可清理对象或扩容，之后才能执行本联合回放。
