# BPI Test Environment Deployment Readiness

## 结论

2026-07-13 对 `ubuntu-test`（`100.99.133.43`）再次执行只读容量检查。BPI Kafka/Flink
编排资产已准备，但实机部署状态为 **BLOCKED_DISK**，尚未启动任何新容器。

## 当前证据

| 项目 | 实际值 | 门槛 | 状态 |
|---|---:|---:|---|
| 根文件系统 | 438 GiB | - | 100% used |
| 可用空间 | 538,288 KiB，约 0.51 GiB | 25 GiB | BLOCKED |
| Docker images | 134.2 GB | - | 63.24 GB reported reclaimable |
| Docker volumes | 96.36 GB | - | 6.799 GB reported reclaimable |
| Docker containers | 158 / 108 active | - | 不允许无差别清理 |
| Docker build cache | 727.9 MB | - | 727.9 MB reported reclaimable |

`docker system df` 的 reclaimable 数字只是候选，不代表可以安全删除。当前没有执行
`docker prune`、删目录、删镜像、删容器或删卷。

## 已准备的部署门禁

- 独立 Compose project，不改动现有 ADP Compose project；
- 三节点 Kafka 4.2.0 KRaft，六个 topic（含 candidate DLQ）副本 3、`min.insync.replicas=2`；
- Flink 2.2.1 Application Mode，Java 17，稳定 job JAR；
- RocksDB + MinIO/S3 增量 checkpoint；
- 只绑定 loopback 的默认诊断端口；
- 25 GiB 空闲磁盘硬门槛；
- topic、RUNNING job、TaskManager 和完成 checkpoint 的实机 smoke；
- 默认停机不删除 named volumes。

## 下一门槛

1. 用户确认可删除的历史部署副本，或为 Docker 数据目录扩容；
2. 空闲空间达到 25 GiB 后执行 `make bpi-stream-deploy-preflight`；
3. 启动集群并产生首个成功 checkpoint；
4. 执行 `make bpi-stream-cluster-replay`，确认 IoT Protobuf、Kafka offset 和候选 key；
5. 显式启用已实现的 candidate consumer 与 tenant/plant/line allowlist，完成 BPI inbox、candidate、DLQ 和 PostgreSQL marker 联合验收；
6. 执行 savepoint 升级、TaskManager 重启、broker 故障和 rollback 演练；
7. 开始 24 小时逐级负载/长稳测试。
