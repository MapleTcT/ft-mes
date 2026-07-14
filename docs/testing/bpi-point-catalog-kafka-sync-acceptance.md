# BPI 点位目录自动同步验收

## 结论

2026-07-15，`MapleTcT/iot@786d153a` 与 `MapleTcT/ft-mes@79ee289d` 在目标测试环境完成
`JetLinks -> Kafka -> BPI service -> PostgreSQL -> 浏览器` 自动点位目录同步验收。

结论为 `PASS_CONTROL_WITH_BLOCKED_SOURCE`：自动同步、内容身份、幂等、毒消息隔离、落库和页面读取
均通过；真实试点点位仍因设备未激活、属性 metadata 缺失、标定未验证和来源序列未就绪而保持
`BLOCKED`。本次验收不把控制链可用误写成现场数据源已就绪。

## 运行边界

| 项目 | 验收值 |
|---|---|
| IoT 来源 | JetLinks `LocalDeviceInstanceService` 与产品 metadata |
| 事件合同 | `PointCatalogSnapshotV1` Protobuf v1 |
| 来源主题 | `iot.point-catalog.snapshot.v1` |
| DLT | `iot.point-catalog.snapshot.dlq.v1` |
| Kafka key | `tenant_id\|plant_id\|line_id\|source_instance` |
| 必需 headers | `event_id`、`tenant_id`、`source_revision`、`schema_version=v1`，且每项只允许出现一次 |
| 内容身份 | 规范排序后的 scope/点位内容 SHA-256；观察时间不参与 revision |
| 容量门禁 | 每个 scope 最多 10,000 点；Protobuf 最大 5 MiB；Kafka 信封 6 MiB |
| 目标 scope | `1000 / PLANT-01 / LINE-S07-01` |
| 自动 revision | `sha256:2a218d12d6ed8bea024c38f6d2e06656f20703fadf920256dc98b17c2f151ce5` |

消费者默认关闭，并要求租户、工厂、产线三层精确 allowlist；未配置时使用 deny-all。消息使用
`read_committed`、手工同步确认、有限重试和 DLT，不能依赖接口 `200` 推断落库。

## 验收记录

| 验收项 | 实际结果 | 状态 |
|---|---|---|
| 合同兼容 | Protobuf 仅追加字段；兼容基线和 AsyncAPI 校验通过 | PASS |
| IoT 权威映射 | 设备状态、产品/属性 metadata、源单位、显式标定和来源序列按真实值映射 | PASS |
| 首次落库 | 自动 revision 保存 1 个 snapshot、1 个 entry、1 个幂等记录和 1 个审计事件 | PASS |
| 重复消息 | 同 event/revision 重放后 snapshot、entry、幂等和审计均不增加 | PASS |
| 毒消息 | 无效 payload 经有限重试进入 DLT，未产生业务快照 | PASS |
| 重启幂等 | IoT 重启后相同目录保持相同 revision/event identity，数据库仍各 1 行 | PASS |
| Broker 信封滚动应用 | 三个 broker 逐台 force-recreate 并逐台等到 healthy；42 个分区无欠副本，Flink 30/30 task 保持 RUNNING，checkpoint 推进到 955 | PASS |
| 浏览器读取 | `http://100.99.133.43:18091/#/points` 显示同一 revision；console/page/request failure 为 0 | PASS |
| 现场准入 | 1 个点、0 个 READY；设备 `notActive`、metadata/标定/序列未就绪 | BLOCKED |

## PostgreSQL 证明

验收 SQL：

```sql
SELECT tenant_id, plant_id, line_id, source, source_instance,
       source_revision, point_count, ready_point_count, imported_by
FROM bpi.bpi_point_catalog_snapshots
ORDER BY observed_at DESC
LIMIT 3;

SELECT source_revision, count(e.id) AS entries
FROM bpi.bpi_point_catalog_snapshots s
LEFT JOIN bpi.bpi_point_catalog_entries e ON e.snapshot_id = s.id
GROUP BY s.id, s.source_revision
ORDER BY max(s.observed_at) DESC
LIMIT 3;
```

目标结果摘要：

```text
1000|PLANT-01|LINE-S07-01|JETLINKS|jetlinks-pilot-node-01|
sha256:2a218d12d6ed8bea024c38f6d2e06656f20703fadf920256dc98b17c2f151ce5|1|0|
jetlinks-point-catalog-sync

sha256:2a218d12d6ed8bea024c38f6d2e06656f20703fadf920256dc98b17c2f151ce5|1
```

最新幂等记录为 `point-catalog-2a218d...151ce5 / COMPLETED / 200`；最新审计为
`jetlinks-point-catalog-sync / POINT_CATALOG_SNAPSHOT_IMPORTED / POINT_CATALOG_SNAPSHOT`。

## 浏览器证据

- 报告：`/tmp/bpi-point-catalog-sync-scope-20260715.json`
- 截图：`/tmp/bpi-point-catalog-sync-scope-20260715.png`
- 页面：`/#/points`
- API：`GET /bpi-api/point-catalog/current?plantId=PLANT-01&lineId=LINE-S07-01`
- 实际：HTTP `200`，自动来源、revision、1 个点、0 个 READY 和四项阻断均可见。

首次浏览器读取使用了隔离验收 scope，返回 `403`。真实页面暴露了 scope 配置偏差后，运行配置改为
实际 ADP scope `1000 / PLANT-01 / LINE-S07-01`，同时收紧消费者 allowlist，再次执行浏览器验收通过。

## 部署与回滚

部署前备份位于目标机 `/home/v6/bpi-deploy-backups/20260715040853`，回滚镜像为：

- `ft-mes-bpi-service:rollback-20260715-pointcatalog-pre`
- `mapletct/jetlinks-bpi-pilot:rollback-20260715-pointcatalog-pre`

同步过程中曾误带入本地忽略的 `.env`；部署预检在 IoT 重启前发现 exporter 状态不一致并中止，随后
从备份恢复三个远端 `.env`，仅追加本功能所需的非秘密键后重建服务。既有 ADP/MES Compose 未被替换，
未执行 `--delete`，未删除数据库或持久卷。

6 MiB broker 信封通过 `kafka-1 -> kafka-2 -> kafka-3` 顺序滚动重建应用，每台恢复 healthy 后才继续。
首次重建后 smoke 暴露了检查器缺陷：broker 默认值生效后，所有 topic 描述都会显示继承的
`max.message.bytes`，旧脚本却按全局出现次数判断“只允许两个 topic”。检查器已改为通过
`kafka-configs.sh` 分别读取 source/DLT 的动态 topic 配置；复验报告
`/tmp/bpi-streaming-cluster-smoke-point-catalog-envelope-20260715.json` 为 PASS，10 个 topic、RF=3、
`min.insync.replicas=2`、点位目录 6 MiB 均通过，42 个分区欠副本为 0，Flink 30/30 task 均 RUNNING。

## 后续动作

1. 在 JetLinks 注册并激活 `bpi-pilot-device-01`。
2. 为 `instantFlow` 补齐产品 metadata、真实单位和已审核标定版本。
3. 配置可持久化的设备/网关 `source_epoch + sequence`。
4. 等待自动同步生成新 revision，重新验证拓扑；禁止手工伪造 READY 快照。
5. READY 后再用真实遥测与 MES production context 的同一 marker 闭合 candidate/batch。
