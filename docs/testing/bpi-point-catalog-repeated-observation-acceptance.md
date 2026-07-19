# BPI 点位目录重复观测与准入撤销验收

## 结论

2026-07-19，目标测试环境 `10.11.100.17` 完成
`JetLinks -> Kafka -> BPI service -> PostgreSQL -> ADP 浏览器` 重复观测验收，状态为
`PASS_CONTROL_WITH_BLOCKED_SOURCE`。

修复前，点位目录以内容 SHA-256 同时充当 revision、event ID 和数据库唯一键。运行时来源序列证据
从 `true` 恢复为 `false` 时，目录内容会回到旧 revision，消息被当成历史重复而无法撤销 READY。
修复后：

- `sourceRevision` 继续只标识规范化目录内容；
- event ID 增加 `observedAt`，同一内容可以形成新的不可变观测；
- PostgreSQL 唯一键增加 `observed_at`；
- MES 消费者兼容旧 event ID，允许 IoT/MES 滚动升级；
- 当前目录仍按最新 `observed_at` 读取，来源证据失效后会失败关闭。

## 真实链路

| 阶段 | JetLinks 配置 | Kafka | PostgreSQL 最新值 | 结论 |
|---|---|---|---|---|
| 基线恢复 | `requireSourceSequence=false`、ingress 关闭 | 目录新观测已消费，lag 0 | revision `sha256:f67db5...04743`、`source_sequence_enabled=false` | PASS |
| 受控启用 | gateway epoch + sequence，ingress 临时开启 | 遥测分区 2 offset `2 -> 4` | revision `sha256:63ec7e...835f7`、`source_sequence_enabled=true`、READY `0/1` | PASS |
| 最终撤销 | 恢复 `requireSourceSequence=false`、ingress 关闭 | 同旧内容 revision 再次同步 | revision `sha256:f67db5...04743`、`source_sequence_enabled=false`、READY `0/1` | PASS |
| 入口关闭 | 使用有效 JetLinks token POST 受控入口 | 不产生消息 | HTTP `404` | PASS |

受控阶段 marker 为 `ADP_BPI_REPEAT_SEQUENCE_20260719_1106`。两条请求均返回 HTTP `200`，
EventBus subscribers 均为 3；exporter 的接收、发布和点位目录时间戳均推进，错误字段为空。
最终状态没有把受控入口留在测试环境。

## PostgreSQL 证明

目标 Flyway 已升级到 V16，唯一约束为：

```sql
UNIQUE (tenant_id, source, source_instance, plant_id, line_id,
        source_revision, observed_at)
```

验收 SQL：

```sql
SELECT s.source_revision, count(*), min(s.observed_at), max(s.observed_at),
       array_agg(e.source_sequence_enabled ORDER BY s.observed_at)
FROM bpi.bpi_point_catalog_snapshots s
JOIN bpi.bpi_point_catalog_entries e ON e.snapshot_id = s.id
GROUP BY s.source_revision
ORDER BY max(s.observed_at) DESC;
```

结果摘要：

```text
sha256:f67db5...04743 | 3 | 2026-07-19 10:21:59+08 | 2026-07-19 11:08:53+08 | {false,false,false}
sha256:63ec7e...835f7 | 2 | 2026-07-19 10:34:34+08 | 2026-07-19 11:07:24+08 | {true,true}
```

这证明同一 revision 能保存后续观测，且控制面能够从 `true` 再次撤销为 `false`。最新快照仍为
1 点、0 READY，因为设备未注册/激活、属性 metadata 和单位缺失、标定未验证。

## 浏览器证明

最终 marker `ADP_BPI_REPEAT_FINAL_20260719_110916` 通过真实 ADP 登录访问
`http://10.11.100.17:18080/bpi/#/points`：

- `GET /bpi-api/point-catalog/current?plantId=PLANT-01&lineId=LINE-S07-01` 返回 `200`；
- 页面显示最新 false revision 和六项失败关闭原因；
- console error、page error、request failure 均为 0；
- 报告 `/tmp/ADP_BPI_REPEAT_FINAL_20260719_110916-point-catalog.json`；
- 截图 `/tmp/ADP_BPI_REPEAT_FINAL_20260719_110916-point-catalog.png`。

## 构建、回滚与未完成边界

- IoT JAR SHA-256：`ee9553ba4889459404d930ff5f8231c0a9a61a84f03ae1550b5af031efbe8326`；
- JetLinks 镜像：`mapletct/jetlinks-bpi-pilot:20260719-repeat-observation`；
- BPI service 镜像：`ft-mes-bpi-service:20260719-repeat-observation`；
- IoT 回滚目录：`/data/docker/bpi-upgrade-backups/20260719-repeat-observation-iot`；
- MES 回滚目录：`/data/docker/bpi-upgrade-backups/20260719-repeat-observation-mes`；
- 受控验收证据：`/data/docker/bpi-upgrade-backups/ADP_BPI_REPEAT_SEQUENCE_20260719_1106`。

本次证明了控制链可以正确启用和撤销来源序列准入，不代表现场点位已经 READY。G-021 继续保持
`PARTIAL`；真实设备注册/激活、metadata、单位、标定、连续单调来源序列、同 scope WOM context 与
candidate/batch、7-14 天影子运行仍需继续闭合。
