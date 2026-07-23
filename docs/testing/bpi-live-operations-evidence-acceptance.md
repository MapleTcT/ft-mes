# BPI 实时生产态势目标验收

## 结论

2026-07-23 在唯一测试栈 `10.11.100.17 / adp-mes-newbase` 完成 BPI Phase 3C-F
实时运行事实投影的受控 MQTT、JetLinks、Kafka、PostgreSQL、API 和真实浏览器验收。状态为
`PASS_TARGET_CONTROLLED_MQTT_KAFKA_POSTGRES_LIVE_BROWSER_CLEANED`。

本轮证明 `/bpi/#/overview` 不再显示前端示意值：页面上的 `12.5 m3/h`、采样时间、质量、
来源序列、点位覆盖、最近 15 分钟样本和运行判据均来自 PostgreSQL 事实。接口返回成功后又直接
查询了原始遥测表和 V35 latest 投影，不能用 HTTP 200 替代落库结论。

输入仍是受控 MQTT 模拟器和临时校准，不是物理流量计或正式计量证书。当前测试线还存在 7 个
历史未解决数据质量事件且没有活动生产指令，因此服务端诚实返回 `dataHealth=BAD`、
`status=BLOCKED` 和生产上下文 `WARN`。这不是本轮链路失败，也不能为了截图好看而清除非 marker
历史事件。`G-021` 继续保持 `PARTIAL`。

## 运行身份

| 项目 | 值 |
|---|---|
| marker | `ADP_E2E_20260723_BPI_LIVE_V35_07` |
| 页面 | `http://10.11.100.17:18080/bpi/#/overview` |
| scope | `1000 / PLANT-01 / LINE-S07-01` |
| 数据库 | PostgreSQL 15.18 / `ft_mes_bpi` / schema `bpi` |
| Flyway | V35 |
| BPI service | `67b728dabedbf350499accabd85929952733086e` |
| Java 8 adapter / 验收加固 | `316e04d9` |
| 前端路由修复 | `8622196d` |
| IoT exporter | `21618eae` |
| 远端证据 | `/home/v6/adp-mes-backups/bpi-v35-live-67b728da/acceptance-ADP_E2E_20260723_BPI_LIVE_V35_07` |

目标镜像为：

- `ft-mes-bpi-service:20260723t115310z-67b728dabedb`
- `ft-mes-bpi-adapter:20260723-live-evidence-316e04d9`
- `mapletct/jetlinks-bpi-pilot:20260723-evidence-sync-21618eae`

## 产品链

```text
受控 MQTT 3.1.1 / QoS1
  -> JetLinks 产品、设备和点位映射
  -> iot.telemetry.selected.v1
  -> BPI scoped Kafka consumer
  -> TelemetryIngestionService
  -> bpi_telemetry_events / bpi_telemetry_points
  -> 同事务 UPSERT bpi_telemetry_point_latest
  -> OverviewService / OverviewPostgresRepository
  -> Java 8 adapter /bpi-api
  -> /bpi/#/overview 与点位事实抽屉
```

## 验收矩阵

| 验收项 | 预期 | 实际 | 状态 |
|---|---|---|---|
| V35 latest 投影 | 最新接受点位只保留一行，不被旧样本倒退 | window 2 events/2 points；latest 1 行，值 `12.5 m3/h`、`GOOD/IN_ORDER` | PASS |
| Overview API | 返回真实主信号、值、单位、时间和健康度 | `GET /bpi-api/overview` 200；精确物理点位和临时校准匹配 | PASS |
| Live evidence API | 返回最近真实样本、服务端判据和事件 | `GET /bpi-api/lines/LINE-S07-01/live-evidence` 200；5 samples、6 PASS、1 WARN | PASS |
| 真实页面 | 行内显示受控值和单位，可打开点位事实 | 显示 `12.5 m3/h · flow.instant`；抽屉显示物理点位、趋势、判据和事件 | PASS |
| Hash 路由 | 同页从影子验收切到 overview | 修复 `hashchange` 同步，回归测试通过，目标页面可见 | PASS |
| 浏览器错误 | 无 console/page/request/non-2xx | 36 个响应全为 2xx；四类错误计数均为 0 | PASS |
| 影子窗口覆盖 | 两条窗口遥测全部被接受 | pinned/observed/sequence/calibration/GOOD 均 `1/1`；event/observation `2/2` | PASS |
| 安全边界 | 不训练、不推断、不写外部系统 | model/training/inference/production activation/WMS writes 均为 `0/false` | PASS |
| 清理与恢复 | marker 清零，运行周期恢复 | 7 类残留均为 0；来源序列 `10m`、点位目录 `5m` | PASS |

影子任务的 `readyForApproval=false` 是正确结果：短窗口仍缺最小时长、真实复核批次、边界认同率和
累计量偏差门槛。本轮只验收实时事实读取，不降低批准政策。

## API 与落库

```http
GET /bpi-api/overview?plantId=PLANT-01&onlyAbnormal=false
GET /bpi-api/lines/LINE-S07-01/live-evidence?plantId=PLANT-01&windowMinutes=15&limit=120
```

核心 PostgreSQL 结果：

| 事实 | 实际 |
|---|---:|
| window event rows | 2 |
| window point rows | 2 |
| rejected rows | 0 |
| latest projection rows | 1 |
| latest value / unit | `12.5 / m3/h` |
| quality / disposition | `GOOD / IN_ORDER` |
| sequence | `4,5` |
| live evidence samples | 5 |

V35 latest 行由 `TelemetryPostgresRepository` 与原始事件/点位在同一事务内写入。覆盖条件按
`sample_time`、`source_epoch`、`sequence` 递进，迟到事实可以保留在历史表，但不能倒退在线态。
表级映射与复验 SQL见
`docs/backend-table-audit/bpi-live-telemetry-projection.md`。

## 浏览器证据

- `metadata/bpi-live-operations-overview-target.png`
- `metadata/bpi-live-operations-drawer-target.png`
- Overview 截图 SHA-256：
  `97daafcd967eaadf5347f9f53d31e4d75cadfd0e80dd3f47bf977139cd9922ad`
- Drawer 截图 SHA-256：
  `00c4d49b2b6d6f8ac737920f89e44626704c8dce0127020c11bdd048a89dd678`

桌面抽屉几何为 `left=760/right=1440/width=680`，没有溢出。Overview 截图保留
`BAD/BLOCKED`，抽屉保留历史事件和生产上下文警告，避免用伪造绿色状态替代真实运行事实。

## 诊断与回归

目标验收先后暴露并关闭了这些真实问题：

1. 点位目录与来源序列证据原先共用调度，现已拆成独立周期。
2. 目标 `application-bpi-pilot.yml` 缺少来源证据周期绑定，已同步当前配置。
3. runner 恢复 `.env` 后仍继承临时 shell 变量，现会 unset 并核对容器实际环境。
4. Java 8 adapter 未放行 `live-evidence`，现有精确 allowlist 和路由测试。
5. 前端只在首次加载读取 hash，现监听 `hashchange` 并有同页路由回归。
6. Ubuntu 26.04 的 Playwright 托管浏览器版本漂移，runner 现会在修改 IoT 配置前预检浏览器。

最终通过的是 `_07`；前六轮均只作为诊断，不被写成 PASS。每轮失败后的 marker 数据均被定向清理，
IoT 周期也恢复到基线。

## 边界与下一门槛

- 本轮是受控 MQTT 软件整链，不是物理 DEVICE/GATEWAY 资格。
- 临时校准不是计量人员批准的现场证书。
- 单点、5 条短窗口样本不代表十万点容量或连续稳定性。
- 仍需断电重连、epoch/sequence 恢复、多点/多产线容量和 7-14 天现场运行。
- 仍需至少 200 个真实复核批次、7 个生产日、100 accepted 和 10 rejected START 标签。
- 外部 ERP/WMS、全站灾备、MLflow 生产 RBAC/SSO/TLS/HA 仍未完成。
- 全部训练、模型、在线推断、自动确认和生产激活路径继续关闭。
