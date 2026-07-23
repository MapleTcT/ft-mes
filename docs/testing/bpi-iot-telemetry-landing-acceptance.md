# BPI IoT 遥测落表目标验收

## 结论

2026-07-23 在唯一测试栈 `10.11.100.17 / adp-mes-newbase` 完成 BPI Phase 3C-E
受控遥测落表的真实 MQTT、JetLinks、Kafka、PostgreSQL、API 和浏览器验收。状态为
`PASS_TARGET_CONTROLLED_MQTT_KAFKA_POSTGRES_BROWSER_CLEANED`。

本轮首次把 `iot.telemetry.selected.v1` 的受控 Protobuf 遥测直接落入 BPI PostgreSQL，并在影子
运行详情中显示窗口内点位、事件、质量、校准和来源序列覆盖。验收同时证明运行窗口开始前的预热
遥测不会被计入当前影子运行。

本轮输入来自受控 MQTT 试点设备，不是物理现场设备；临时校准只用于验收，不是正式计量证书。
因此 `G-021` 继续保持 `PARTIAL`，模型训练、注册、在线推断和生产激活均保持关闭。

## 运行身份

| 项目 | 值 |
|---|---|
| marker | `BPI_TLANDING_20260723_094606` |
| shadow run | `63c90d77-cb4a-4982-be55-74378399742a` |
| 产品实现 | `8c9c4192b17953c48208efd31ef6528de04d96c6` |
| 验收脚本 | `988868f539cfd9ed5b0127edb621e799a509bad0` |
| 数据库 | PostgreSQL 15.18 / `ft_mes_bpi` / schema `bpi` |
| Flyway | V34，V33 -> V34 expand-only |
| 页面 | `http://10.11.100.17:18080/bpi/#/shadowRuns` |
| scope | `1000 / PLANT-01 / LINE-S07-01` |
| Kafka group | `ft-mes-bpi-service-telemetry-v1` |

升级报告为 `metadata/bpi-integrated-upgrade-v34-target.json`，SHA-256 为
`af015338793972426f4b940f2ff78b76df6be4bc43ef2e25c0056797137a4774`。

## 产品链

```text
MQTT 3.1.1 / QoS1
  -> JetLinks 受控设备与 exporter
  -> Kafka iot.telemetry.selected.v1
  -> TelemetryKafkaListener
  -> TelemetryKafkaRecordProcessor
  -> TelemetryIngestionService
  -> TelemetryPostgresRepository
  -> bpi_telemetry_events / points / rejects / source_state
  -> ShadowRunPostgresRepository
  -> ShadowRunTelemetryCoverage
  -> Java 8 adapter /bpi-api
  -> BPI 影子运行页面
```

Kafka 消费器默认关闭且默认无 scope；目标验收只允许上述单一 tenant/plant/line。消息必须同时通过
topic、Protobuf 合同、`plantId|deviceId` key、payload 大小和精确 scope 校验。事务提交成功后才手工
ack；毒消息进入 `iot.telemetry.selected.dlq.v1`。

## 验收矩阵

| 验收项 | 预期 | 实际 | 状态 |
|---|---|---|---|
| V34 expand-only 升级 | V33 -> V34，三个核心服务健康 | Flyway 34；service/adapter/WMS adapter 均 healthy | PASS |
| 受控 MQTT 预热 | 3 条 QoS1 均 PUBACK 并落库 | epoch `20260723094606`，sequence `1..3`，3/3 PUBACK | PASS |
| 来源证据 | 同 epoch 严格递增并达到 QUALIFIED | `DEVICE/QUALIFIED/1..5/count=5` | PASS |
| 影子运行创建 | 页面创建 `DRAFT/r1` | POST 200，run ID 与 PostgreSQL 一致 | PASS |
| 预热窗口隔离 | DRAFT 与启动前不计预热事件 | 创建和启动后、窗口消息发送前均为 0 | PASS |
| 影子运行启动 | 页面启动 `RUNNING/r2` | POST 200，`started_at` 持久化 | PASS |
| 窗口内 MQTT | 2 条 QoS1 均 PUBACK 并落库 | sequence `4,5`，2/2 PUBACK | PASS |
| 遥测覆盖投影 | 1 个固定点位全部覆盖 | pinned/observed/sequence/calibration/GOOD 均为 `1/1` | PASS |
| 序列与质量 | 无 GAP/乱序/拒绝 | event/observation `2/2`，reject/gap/out-of-order `0/0/0` | PASS |
| 浏览器与 API | 页面可见且无前端/网络错误 | 29 个 BPI 响应全为 2xx，三类浏览器错误均为 0 | PASS |
| 取消影子运行 | 页面取消并保存终态 | `CANCELLED/r3` | PASS |
| 精确清理与恢复 | marker 清零，试点配置恢复 | marker 行 0；5m/10m、未验证校准与 UNCERTAIN 已恢复 | PASS |

## 页面与 API

真实 `admin` 会话执行：

```http
POST /bpi-api/shadow-runs
POST /bpi-api/shadow-runs/63c90d77-cb4a-4982-be55-74378399742a/start
GET  /bpi-api/shadow-runs/63c90d77-cb4a-4982-be55-74378399742a
POST /bpi-api/shadow-runs/63c90d77-cb4a-4982-be55-74378399742a/cancel
```

创建、启动、读取和取消均为 HTTP 200，依次得到 `DRAFT/r1`、`RUNNING/r2`、
`fullyCovered=true` 和 `CANCELLED/r3`。浏览器共观察到 29 个 BPI 响应，非 2xx、console error、
page error 和 request failure 均为 0。

桌面 `1440x900` 页面显示“现场遥测落表”和五项 `1/1`、事件 `2/2`、间隙/乱序 `0/0`。
移动 viewport/body/document 均为 390px，抽屉宽 390px；验收脚本滚动到遥测区并验证几何后截图，
没有横向溢出。

截图：

- `metadata/bpi-iot-telemetry-landing-desktop-target.png`
- `metadata/bpi-iot-telemetry-landing-mobile-target.png`

## PostgreSQL 落表

窗口内 SQL 以 shadow run 的 `started_at` 同时约束：

- `bpi_telemetry_events.event_time >= started_at`
- `bpi_telemetry_events.created_at >= started_at`
- `bpi_telemetry_points.sample_time >= started_at`
- `bpi_telemetry_points.created_at >= started_at`

这样既排除事件时间较早的预热数据，也排除验收窗口前已经落库的数据。V34 为该联查增加三个索引，
不新增重复采集表或 Oracle 路径。

| PostgreSQL 事实 | 实际 |
|---|---|
| 窗口内 event rows | 2 |
| 窗口内 point rows | 2 |
| rejected rows | 0 |
| sequence | `4,5` |
| sequence disposition | `IN_ORDER,IN_ORDER` |
| quality | `GOOD,GOOD` |
| value | `12.5,12.5` |
| calibration | `pilot-telemetry-BPI_TLANDING_20260723_094606` |
| 最终 shadow run | `CANCELLED/r3` |
| 外部 WMS 写入 | 0 |

核心目标表：

```text
bpi.bpi_telemetry_source_state
bpi.bpi_telemetry_events
bpi.bpi_telemetry_points
bpi.bpi_telemetry_point_rejects
bpi.bpi_shadow_runs
bpi.bpi_point_catalog_snapshots
bpi.bpi_point_catalog_entries
bpi.bpi_source_sequence_evidence_current
bpi.bpi_point_calibrations
bpi.bpi_topology_versions
bpi.bpi_rule_versions
bpi.bpi_audit_events
bpi.bpi_api_idempotency
```

验收 SQL 为
`deploy/docker/scripts/bpi-telemetry-landing-acceptance-verification.sql`。它直接查询窗口事件、点位、
拒绝记录、目录固定版本、来源、质量、校准、序列和模型/WMS 副作用，不以接口 200 代替查库。

## Kafka 与恢复

- topic：`iot.telemetry.selected.v1`
- DLQ：`iot.telemetry.selected.dlq.v1`
- 6 partitions，3 个活动 consumer，最终总 lag 为 0
- `auto.offset.reset=latest`，`read_committed`，并发数 3
- scope 只允许 `1000 / PLANT-01 / LINE-S07-01`
- consumer、HTTP ingress 和生产激活均默认关闭

受控验收曾遇到五个时序/环境问题，均已修复并进入脚本回归：

1. 来源证据实际由点位目录同步周期调度，脚本现同时临时缩短两个周期并精确恢复。
2. 覆盖在第一条事件后已为 true，脚本现同时等待期望 event/observation 数。
3. JetLinks healthy 早于 MQTT 网关稳定 PUBACK，脚本现增加稳定等待、超时和 fresh-epoch 重试。
4. Ubuntu 26.04 无法安装 Playwright 固定浏览器，脚本支持显式 Chromium 149 路径。
5. 移动截图最初只捕获抽屉顶部，脚本现滚动到遥测区并检查可见几何。

## 精确清理

取证后先按 marker 清理，恢复 IoT 映射，再执行一次 post-restore 清理。最终：

```text
marker telemetry rows = 0
shadow runs = 0
rules = 0
topologies = 0
calibrations = 0
catalogs = 0
```

JetLinks 保持 healthy。IoT 周期恢复为 `5m/10m`，映射恢复
`pilot-unverified-20260714 / calibrationVerified=false / defaultQuality=UNCERTAIN`，含敏感信息的
临时环境备份已删除。来源 current 是带 TTL 的运行心跳，不被伪装为业务历史。

## 边界

- 本轮证明受控 MQTT 能通过真实 JetLinks/Kafka/BPI 服务落入 PostgreSQL，并在真实页面按窗口读取。
- 本轮不证明物理 DEVICE/GATEWAY、断电重连、正式计量证书或现场 7-14 天连续运行。
- 本轮只覆盖一个点位、一个短窗口，不代表生产容量和多产线高基数性能。
- 没有启动模型训练、模型登记、在线推断、生产激活或外部 WMS 写入。
- 下一门槛是使用正式点位和校准重复同一脚本，再进入 7-14 天、200 个复核批次和 7 个生产日积累。

完整机器证据为 `metadata/bpi-iot-telemetry-landing-acceptance.json`。
