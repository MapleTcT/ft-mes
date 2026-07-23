# BPI Phase 3C-F 实时运行事实投影设计

## 1. 问题与目标

当前 BPI “实时生产态势”虽然具备页面和 API，但仍存在三处不能用于现场验收的演示行为：

- `OverviewService` 不读取遥测，`instantFlow` 永远为 `null`；
- `dataHealth` 固定返回 `GOOD`，无法暴露超时、坏质量、序列异常或未解决事件；
- 产线详情中的 15 分钟曲线和运行判据由前端写死，不对应 PostgreSQL 事实。

本阶段建立一个只读、可审计、适合高频轮询的实时运行事实投影，使页面能够回答：

1. 这条产线最后一次真实遥测是什么时间；
2. 当前拓扑要求的关键工艺信号是否全部到达且仍新鲜；
3. 最新值、单位、质量码、校准版本和来源序列处置是什么；
4. 是否存在未解决的数据质量事件；
5. 最近窗口的趋势是否来自真实遥测，而不是前端示意数据。

本阶段不声明物理设备、正式校准、7-14 天稳定性、模型训练或生产激活已经完成。

## 2. 数据边界

新增 Flyway V35 表 `bpi.bpi_telemetry_point_latest`，每个
`tenant + plant + line + product + device + property` 仅保留最后一条已接受遥测：

- 原始事实仍由 `bpi_telemetry_events` 和 `bpi_telemetry_points` 保存；
- 最新态与原始点在同一数据库事务内写入；
- 只有更晚的 `sample_time`，或同一采样时间下更高的 `source_epoch/sequence` 才能覆盖；
- `OUT_OF_ORDER`、旧 epoch 或迟到样本仍可进入历史事实，但不能倒退最新态；
- V35 不扫描或回填历史大表，部署后由新进入的真实遥测自然建立最新态；
- 表只提供在线态势读取，不成为数据集、训练或审计事实的替代来源。

该投影让 5 秒轮询只读取每点一行，而不是反复扫描高容量遥测历史。

## 3. 服务端健康判定

配置 `bpi.overview.telemetry-freshness`，默认 `2m`。服务端对每条产线统一计算：

| 条件 | `dataHealth` | 运行状态影响 |
|---|---|---|
| 未解决 CRITICAL/ERROR、BAD/STALE、GAP/OUT_OF_ORDER | `BAD` | `BLOCKED` |
| 无当前拓扑、必需点未到齐、遥测超时、UNCERTAIN/SUBSTITUTED、一般未解决事件 | `PARTIAL` | 有运行批次时 `DEGRADED` |
| 必需点全部新鲜且 GOOD，无未解决事件 | `GOOD` | 有运行批次时 `RUNNING` |
| 无运行批次且健康 | `GOOD` | `IDLE` |

批次本身为 `SUSPENDED` 时始终为 `BLOCKED`。`onlyAbnormal=true` 必须返回
`BAD/PARTIAL`、`BLOCKED/DEGRADED` 或有待审候选的产线，不能被固定 `GOOD` 隐藏。

当前拓扑的 `requiredSignals[0]` 是主显示信号；未声明 required signal 时使用第一条 binding。
主信号的产品、设备、属性和单位来自已发布拓扑，不使用属性名猜测流量。

## 4. API

保持现有接口向后兼容并扩展字段：

- `GET /bpi/v1/overview?plantId=...&onlyAbnormal=...`
- `GET /bpi/v1/lines/{lineId}/current-state`

`LineState` 新增：

- `plantId`
- `telemetry`：主信号、最新值/单位/质量/时间、时延、预期/已观察/GOOD 点数、
  未解决/严重事件数和序列处置。

新增：

- `GET /bpi/v1/lines/{lineId}/live-evidence?plantId=...&windowMinutes=15&limit=120`

响应包含当前 `LineState`、主信号真实趋势、服务端判据和未解决数据质量事件摘要。
`windowMinutes` 限制为 `1..1440`，`limit` 限制为 `2..500`。所有查询继续执行 token
tenant/plant/line scope 校验。

## 5. 前端交互

- 态势表将“瞬时流量”改为“关键工艺值”，显示服务端主信号、值、真实单位和质量；
- 增加遥测时效与点位覆盖，不再把无数据渲染为 `0 t/h`；
- 删除固定的五段工艺条；
- 详情抽屉调用 `live-evidence`，只绘制 API 返回的真实样本；
- 无样本时显示明确空态，不生成示意柱；
- 运行判据逐条展示服务端 `PASS/FAIL` 及实际解释；
- 未解决事件提供 issue code、严重度、状态、最后发生时间和说明；
- 桌面和 390px 移动视口都不得溢出或遮挡。

## 6. 验收矩阵

| 场景 | 预期 |
|---|---|
| 首条 GOOD 遥测 | V35 最新态插入，Overview 显示真实值/单位/时间 |
| 更新遥测 | 最新态更新，趋势保留两条历史事实 |
| 迟到/乱序遥测 | 历史事实保留，最新态不倒退 |
| BAD/STALE 或 GAP | `dataHealth=BAD`，产线进入 `BLOCKED` |
| 遥测超过 freshness | `dataHealth=PARTIAL`，运行产线为 `DEGRADED` |
| 未解决 CRITICAL 事件 | Overview 与详情同时显示阻断 |
| 事件解决 | 健康度按剩余事实恢复 |
| 无真实样本 | 页面显示空态，不显示伪造趋势或固定判据 |
| scope 越权 | HTTP 403，不泄露其他产线 |

动态验收必须使用唯一 marker，经真实页面、API 和 PostgreSQL 验证 V35 最新态、原始趋势、
健康状态与清理结果。目标环境仍保持所有训练、推断、自动确认和外部写回开关关闭。
