# BPI MQTT 接入跨仓库联合验收

## 结论

- 验收时间：2026-07-20 09:17-09:39（Asia/Shanghai）
- 目标环境：`10.11.100.17`
- 数据库：PostgreSQL
- 状态：`PASS_CONTROLLED_MQTT_KAFKA_POSTGRES_BROWSER`
- 测试类型：受控 MQTT 模拟器、真实 JetLinks、真实 Kafka/Flink、真实 MES PostgreSQL、真实 ADP 登录与页面
- 产品边界：`controlledSimulator=true`、`fieldDeviceClaimed=false`、`productionReadyClaimed=false`

本轮用两次独立 MQTT 3.1.1/QoS1 会话闭合了
`MQTT -> JetLinks 解码/落库 -> exporter durable spool -> Kafka -> Flink -> MES 来源序列/数据质量 PostgreSQL -> 浏览器`。
六条消息全部收到 PUBACK，并能在两套 PostgreSQL 中按 epoch/sequence 逐条对应。自动批次、WOM、QCS 和 WMS
写入均未开放。

## 受控输入

| 项目 | 值 |
|---|---|
| scope | `1000 / PLANT-01 / LINE-S07-01` |
| source instance | `jetlinks-pilot-node-01` |
| product / device | `bpi-mqtt-pilot-product-01 / bpi-mqtt-pilot-device-01` |
| source / canonical property | `instantFlow / flow.instant` |
| unit / value | `m3/h / 12.5` |
| 会话 1 | `ADP_BPI_MQTT_20260720_0918_EPOCH3`，`2026072003:5001..5003` |
| 会话 2 | 新 TCP/MQTT 连接，`ADP_BPI_MQTT_20260720_0918_EPOCH4`，`2026072004:1..3` |
| quality | `UNCERTAIN`，用于验证数据质量链，不作为合格生产数据 |
| calibration | `UNVERIFIED`，禁止点位进入 READY |

## 运行链证据

| 层级 | 实际结果 | 状态 |
|---|---|---|
| MQTT | 6 次 QoS1 publish，6 次 PUBACK；第二组由新连接发送且 epoch 变化 | PASS |
| JetLinks PostgreSQL | 属性表保存 6 条 `instantFlow=12.5`；设备日志保存 6 个 marker messageId、epoch 和 sequence | PASS |
| exporter | received/enqueued/published 都增加 6；failure 0、spool pending 0、health UP | PASS |
| Kafka telemetry | `iot.telemetry.selected.v1` 总 end offset `37 -> 43` | PASS |
| Flink | job `1e981b842f4693e49f3c3def0fb98cb6` 保持 `RUNNING 36/36` | PASS |
| 来源序列 consumer | lag 0；专用 DLQ 总 offset 0；current evidence 在验收窗口内为 `QUALIFIED` | PASS |
| 点位目录 | snapshot `c93a16dc-17a2-4dd0-8e10-c18f94b442da`，revision `sha256:7ad962...94ab`，1 点/0 READY | PASS_FAIL_CLOSED |
| 数据质量 consumer | 六条当前 MQTT 记录进入同一 `POINT_QUALITY_UNCERTAIN` incident；consumer lag 0，本轮 DLQ 增量 0 | PASS |
| candidate / batch | 09:17 后两表均为 0；没有生产上下文且校准未批准 | PASS_FAIL_CLOSED |

点位的 source evidence 有 30 分钟 TTL。浏览器验收时为 `QUALIFIED / DEVICE / epoch 2026072004 / 1..3 / count 3`，
且唯一 readiness blocker 是 `CALIBRATION_NOT_VERIFIED`。TTL 到期后，系统应重新把来源序列视为不可用于
READY；受控消息不是持续现场证明。

## 数据质量运行配置阻断与修复

首次下游核对发现目标 `bpi-service` 的数据质量 Kafka consumer 为关闭状态，且 scope allowlist 为 deny-all，
导致 Flink 已发布的数据质量记录停在 Kafka。该问题不是解析器或 PostgreSQL mapper 错误。

处理方式：

1. 将当前测试环境的数据质量 consumer 显式启用。
2. allowlist 只开放 `1000 / PLANT-01 / LINE-S07-01`。
3. 备份原 `.env` 到 `/home/v6/adp-deploy-backups/20260720-0935-bpi-data-quality-consumer/.env-before`。
4. 只重建 `adp-mes-newbase-bpi-service-1`，镜像仍为
   `ft-mes-bpi-service:20260720-source-sequence-v22-6b2eb3e7`；其他 ADP、Kafka、Flink 和 IoT 容器未重建。

恢复后 consumer 追平；数据质量 DLQ 保留历史 4 条基线，本轮没有新增。当前测试环境保留这个单 scope
consumer，便于继续影子验收，但这不是生产全量放开。

## 真实页面验收

| 模块 | 页面/路由 | 操作 | API | 前端结果 | 后端/数据库结果 | 状态 |
|---|---|---|---|---|---|---|
| 点位目录 | `http://10.11.100.17:18080/bpi/#/points` | 真实 ADP 登录，筛选受控点位，打开来源证据抽屉 | `GET /bpi-api/point-catalog/current`；`GET /bpi-api/point-calibrations` | 1 行；显示 ACTIVE、来源序列合格、校准阻断；抽屉显示 epoch/sequence/evidence；所有响应 200，console/page/request failure 均为 0 | `bpi_point_catalog_*` 与 `bpi_source_sequence_evidence_current` 返回同一设备和序列窗口 | PASS |
| 数据质量 | `http://10.11.100.17:18080/bpi/#/dataQuality` | 筛选并打开 `POINT_QUALITY_UNCERTAIN` 详情抽屉 | `GET /bpi-api/data-quality/incidents`；`GET /summary`；`GET /incidents/{id}` | 1 行 OPEN/WARNING；详情包含两个 epoch 的 6 条序列；所有响应 200，console/page/request failure 均为 0 | incident `357c519f-b6e9-5528-a9ad-c63c4dbc2a1c` 为 r11/event_count 11；其中本轮精确 6 条原始记录均可查 | PASS |

浏览器报告：`/tmp/bpi-mqtt-ingress-browser-20260720.json`，SHA-256
`7faff9a1bf9ddf3850ad79ed5ab797f18e6a1f578690e821a170146a09e2c26d`。两张 1440x900 截图已人工检查，
没有发现空白页、内容遮挡或不连贯重叠；截图只保留在验收机临时目录，不提交仓库。

## PostgreSQL 验收 SQL

来源序列：

```sql
SELECT status, source_event_id, sequence_origin, source_epoch,
       first_sequence, last_sequence, observation_count, revision,
       first_observed_at, last_observed_at, valid_until
FROM bpi.bpi_source_sequence_evidence_current
WHERE tenant_id = '1000'
  AND plant_id = 'PLANT-01'
  AND line_id = 'LINE-S07-01'
  AND device_id = 'bpi-mqtt-pilot-device-01';
```

浏览器快照读取到 r6，event
`source-sequence-evidence-5574e4cef5f3899c5d9349df56960676d9a9183d2083644d9b59893bcc3f0637`，
`DEVICE / 2026072004 / 1..3 / count 3`。后续周期心跳只更新 current revision，不改变本轮六条原始输入事实。

数据质量：

```sql
SELECT id, issue_code, severity, state, revision, event_count
FROM bpi.bpi_data_quality_incidents
WHERE tenant_id = '1000'
  AND device_id = 'bpi-mqtt-pilot-device-01'
  AND property_id = 'flow.instant';

SELECT source_event_id,
       headers ->> 'source_epoch' AS source_epoch,
       headers ->> 'sequence' AS sequence,
       headers ->> 'sequence_origin' AS sequence_origin,
       headers ->> 'quality_code' AS quality_code,
       detected_at
FROM bpi.bpi_data_quality_incident_events
WHERE incident_id = '357c519f-b6e9-5528-a9ad-c63c4dbc2a1c'
  AND headers ->> 'source_epoch' IN ('2026072003', '2026072004')
ORDER BY detected_at;
```

第二条查询精确返回 `5001/5002/5003/1/2/3` 六行，origin 均为 `DEVICE`，quality 均为 `UNCERTAIN`。

误写保护：

```sql
SELECT count(*)
FROM bpi.bpi_batch_candidates
WHERE created_at >= TIMESTAMPTZ '2026-07-20 09:17:00+08';

SELECT count(*)
FROM bpi.bpi_batch_instances
WHERE created_at >= TIMESTAMPTZ '2026-07-20 09:17:00+08';
```

两项均为 `0`。`bpi_feature_flags` 仍未开放任何 WOM/QCS/WMS 外部写开关。

## 剩余门槛

1. 独立计量校准批准及证书有效期治理。
2. 物理 DEVICE/GATEWAY 的同等 MQTT、断线、掉电和连续序列证明。
3. 同 scope 真实 MES production context 与 START/END 边界闭合。
4. 7-14 天影子运行、真实负载背压和跨组件整体回切。
5. 业务签字后再设计并开放 QCS/WMS 幂等写回；当前保持关闭。
