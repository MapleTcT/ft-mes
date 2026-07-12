# BPI Kafka/Flink Test Deployment

该目录为智能批次与工艺数据中心提供独立、可回滚的测试环境编排，不复用
`deploy/docker/docker-compose.yml` 中 ADP 遗留的 ZooKeeper Kafka。

## 运行边界

- Kafka 4.2.0：3 个 KRaft combined broker/controller，默认副本数 3、`min.insync.replicas=2`。
- Flink 2.2.1 / Java 17：Application Mode，1 个 JobManager、默认 2 个 TaskManager。
- RocksDB：Flink keyed state backend，增量 checkpoint 写入独立 MinIO bucket。
- BPI 作业：挂载 `bpi-stream-engine-0.1.0-SNAPSHOT-job.jar`，不改变 ADP Java 8 服务。
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

预检是只读操作，会验证 Docker/Compose、空闲磁盘、端口、Compose 渲染和作业 JAR，生成
`/tmp/bpi-streaming-preflight.json`。低于 25 GiB 可用空间时会拒绝启动。

## 启停与验收

```bash
make up-bpi-stream
make bpi-stream-cluster-smoke
make down-bpi-stream
```

`up-bpi-stream` 只会启动本目录的 Compose project。`down-bpi-stream` 停止容器但保留 Kafka
和 MinIO named volumes，便于重启和 checkpoint 恢复验证。删除 named volumes 属于破坏性
操作，本仓库不提供自动命令，必须单独审批后执行。

Smoke 必须同时满足：

1. 三个 Kafka broker 正常运行；
2. 五个 BPI topic 均为副本 3、最小同步副本 2；
3. Flink 作业状态为 `RUNNING`；
4. 至少存在一个成功 checkpoint。

该 smoke 只证明集群和作业恢复基础可用，不替代 IoT Protobuf replay、候选确认、PostgreSQL
marker、浏览器操作和长稳压测验收。

## 回滚

```bash
make down-bpi-stream
```

若新作业版本失败，先保留 volumes 和 MinIO checkpoint，恢复上一版 job JAR，然后重新执行
预检、启动和 smoke。升级前的 savepoint/restore 演练将在实机集群可启动后记录到验收报告。
