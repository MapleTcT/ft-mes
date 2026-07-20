# BPI 真实 MQTT 与 WOM START/END 联合验收

验收时间：2026-07-20
目标环境：`10.11.100.17` / `v6-2288H-V6`
数据库：ADP PostgreSQL `adp` + BPI PostgreSQL `ft_mes_bpi` + JetLinks PostgreSQL `jetlinks`
唯一 marker：`BPI_LIVE_20260720_123058`
结论：`PASS_CONTROLLED_MQTT_WOM_START_END`

## 结论边界

本次首次在同一目标环境、同一 WOM 制造指令和同一 BPI 影子批次上闭合：

```text
真实 WOM 页面 start
  -> PostgreSQL production context outbox
  -> Kafka
  -> Flink production-context join
  -> 真实 MQTT 3.1.1/QoS1
  -> JetLinks 解码/双表落库/exporter
  -> START candidate
  -> 真实 BPI 页面确认
  -> ACTIVE 影子批次
  -> 低流量 END candidate
  -> 真实 BPI 页面确认
  -> CLOSED_RAW 影子批次
  -> WOM 页面 stop / inactive context
```

这证明软件链能够把实时遥测与真实 MES 生产上下文汇合为 START/END 批次事实，但 MQTT 来源仍是
受控模拟器，不是物理流量计；MES 校准证据也是测试专用证据，不是现场计量证书。因此 `G-021`
继续保持 `PARTIAL`，不能据此开放 QCS/WMS 生产写回或宣称生产 READY。

## 功能验收

| 模块 | 页面/路由 | 操作 | API / 事件 | 前端结果 | 后端与 PostgreSQL 结果 | 状态 |
|---|---|---|---|---|---|---|
| BPI 运行开关 | `/bpi/#/featureFlags` | LINE 级临时启用 `bpi.commands`，验收后恢复继承 | `POST /bpi-api/feature-flags/bpi.commands` | SET true 为 `200/r5`；INHERIT 为 `200/r6`；页面错误 0 | 最终覆盖 `active=false`，有效值恢复 GLOBAL false | PASS |
| 点位校准 | `/bpi/#/points` | 提交测试校准、同人审批负测、独立审批、规则退役后撤销 | calibration submit/approve/revoke APIs | 同人审批预期 `422`；独立审批 `200`；撤销 `200`；非预期错误 0 | `49dc2c36-ed03-42c9-97e2-ab8eadb46633` 最终 `REVOKED/r3`；目录从 0 READY 临时变 1 READY，再回 0 READY | PASS |
| 工艺拓扑 | `/bpi/#/rules` | 新建、校验、创建人发布负测、独立管理员发布 | topology create/validate/publish APIs | 校验 PASSED；创建人发布预期 `422`；独立发布 `200` | topology `17c2ab5c-41fd-40ed-a19b-378134c3f53b` 为 `PUBLISHED/r3`，绑定 `flow.instant` | PASS |
| START 规则 | `/bpi/#/rules` | 历史样本模拟、提交审批、独立批准发布并等待 Flink READY | rule simulate/submit/publish；Kafka ACTIVATE/APPLIED/READY | 页面 simulation 1 命中、0 漏检、0 误报、0 秒偏差；错误 0 | rule `1a7e8703-c928-425e-bf91-c6549ccad4fd` 达到 PUBLISHED/APPLIED/READY | PASS |
| END 规则 | `/bpi/#/rules` | 同上，阈值 `<1 m³/h` 持续 2 秒 | 同上 | 同样 1/0/0/0；错误 0 | rule `29eee850-2fa5-4007-8798-dbe3068e1929` 达到 PUBLISHED/APPLIED/READY | PASS |
| WOM 生产上下文 | `/msService/WOM/produceTask/produceTask/makeTaskList` | 对任务 `9007190226136424` 执行开始 | `POST .../updateTaskState`，`state=start` | 页面/API `200`，console/page/request failure 为 0 | outbox 产生 waitforrun inactive revision `1784522614324` 和 runing active revision `1784522627276`，均 `SENT/1` | PASS |
| START 实时边界 | `/bpi/#/candidates` | MQTT 上报 `18.6 m3/h` 五次，等待候选后打开详情并确认 | MQTT QoS1；Kafka/Flink；`POST /bpi-api/candidates/9b2aade2-a305-55c5-880c-b6a0be61349b/confirm` | 五次 PUBACK；确认返回 `200`；candidate `CONFIRMED/r2`；错误 0 | candidate key `6b74eb1a-3909-5722-bd95-a85dcff5cfcd`；batch `52427282-eb88-5645-a246-b76fe6547038` 为 `ACTIVE/r1` | PASS |
| END 实时边界 | `/bpi/#/candidates` | MQTT 上报 `0.2 m3/h` 五次，等待候选后打开详情并确认 | MQTT QoS1；Kafka/Flink；`POST /bpi-api/candidates/3ebc9c81-ec10-596d-9d77-990c83f963fa/confirm` | 五次 PUBACK；确认返回 `200`；candidate `CONFIRMED/r2`；错误 0 | 同一 batch 变为 `CLOSED_RAW/r2`，结束时间 `2026-07-20 13:00:28.902+08` | PASS |
| 数据质量处置 | `/bpi/#/dataQuality` | 对修复前 5 条 `UNIT_MISMATCH` 执行确认分派和解决 | acknowledge/resolve APIs | 两个页面命令均 `200`；最终 `RESOLVED/r7`；错误 0 | 5 条 immutable raw event 保留，生命周期含 CREATED/ACKNOWLEDGED/RESOLVED | PASS |
| WOM 结束与恢复 | WOM 制造指令页 + BPI 规则/开关页 | 执行 stop、退役两条规则、撤销测试校准、恢复开关继承和 IoT 原映射 | WOM stop；Kafka RETIRE/APPLIED/INACTIVE；feature INHERIT | 页面动作均成功，规则为 RETIRED/INACTIVE | finished inactive revision `1784524213364` 为 `SENT/1`；任务/待入库 fixture 清零；无 pending candidate 或 active batch | PASS |

## MQTT、JetLinks 与来源证据

| 目的 | trace / epoch | sequence | 值 | 结果 |
|---|---|---:|---:|---|
| 初始来源资格 | `SOURCE_READY` / `20260720123058` | 1..3 | 12.5 | 3/3 PUBACK，随后因校准版本不可变切换到测试版本 |
| 测试校准版本资格 | `SOURCE_READY_V2` / `20260720123401` | 1..3 | 12.5 | 3/3 PUBACK，MES current 为 QUALIFIED/DEVICE |
| 修复前 START | `START_LIVE` / `20260720124501` | 1..5 | 18.6 | 5/5 PUBACK；产生 5 条 UNIT_MISMATCH，不产候选 |
| 修复后 START | `START_RETRY` / `20260720125701` | 1..5 | 18.6 | 5/5 PUBACK；生成 START candidate |
| END | `END_LIVE` / `20260720130001` | 1..5 | 0.2 | 5/5 PUBACK；生成 END candidate |

JetLinks `properties_bpi_mqtt_pilot_product_01` 和
`device_log_bpi_mqtt_pilot_product_01` 均保存了上述属性事实、trace、epoch 和 sequence。验收后 IoT
校准映射恢复为 `pilot-unverified-20260714`；最新 MES 目录快照
`ed92004e-d401-4b4d-8379-8c681afa5674` 为 `UNVERIFIED`、单位 `m³/h`，因此基线继续失败关闭。

## 批次身份与落库

START candidate 以 `context_order_id + rule_version + first evidence` 形成确定性 identity；END candidate
按契约以 `batch_id + rule_version + first evidence` 形成 identity。因此 END candidate 的数据库
`order_id` 为 `NULL` 是当前契约语义，不是上下文丢失。它通过同一 `batch_id` 关联已由 START 创建的
批次，而该批次保留 WOM 指令 `BPI_LIVE_20260720_123058_TASK_TN`。

```sql
SELECT id, boundary_type, order_id, state, revision,
       candidate_key, batch_id, boundary_time
FROM bpi.bpi_batch_candidates
WHERE id IN (
  '9b2aade2-a305-55c5-880c-b6a0be61349b',
  '3ebc9c81-ec10-596d-9d77-990c83f963fa'
)
ORDER BY boundary_time;

SELECT id, batch_no, order_id, state, revision, is_shadow,
       start_time, end_time
FROM bpi.bpi_batch_instances
WHERE id = '52427282-eb88-5645-a246-b76fe6547038';

SELECT revision, action, from_state, to_state, actor_id, event_time
FROM bpi.bpi_batch_state_events
WHERE batch_id = '52427282-eb88-5645-a246-b76fe6547038'
ORDER BY revision;

SELECT boundary_type, source_event_id, signal, value_text,
       unit, quality, event_time, source
FROM bpi.bpi_boundary_evidence
WHERE batch_id = '52427282-eb88-5645-a246-b76fe6547038'
ORDER BY boundary_type, event_time;
```

结果摘要：START/END 两条 candidate 均 `CONFIRMED/r2` 并关联同一 batch；batch 为
`BPI-LINES0701-20260720-6B74EB1A`、`CLOSED_RAW/r2/is_shadow=true`；状态事件为
`SHADOW_BATCH_CREATED` 与 `END_BOUNDARY_CONFIRMED`；边界证据分别为 `18.6/GOOD` 和 `0.2/GOOD`。

WOM 上下文复验 SQL：

```sql
SELECT context_revision, active, source_state, publication_state,
       attempt_count, sent_at
FROM public.wom_bpi_production_context_outbox
WHERE order_id = 'BPI_LIVE_20260720_123058_TASK_TN'
ORDER BY context_revision;

SELECT wom_state_code, active, enabled
FROM public.wom_bpi_task_state_mappings
WHERE lower(btrim(wom_state_code)) IN (
  'wom_runstate/runing', 'wom_runstate/finished'
);
```

三条 outbox 均为 `SENT/1`；显式状态映射为 `runing=true`、`finished=false` 且均 enabled。

## 发现并修复的问题

1. 首轮高流量在 JetLinks 上报单位 `m3/h`，目录绑定为 `m³/h`。Java 服务已把两者视为等价，
   Flink 仍使用字符串直接比较，导致 5 条 `UNIT_MISMATCH` 且不产候选。
2. 提交 `308cca82` 新增流处理 `UnitSymbolNormalizer`，统一 NFKC、空白、大小写及常见立方米符号别名；
   21 条定向测试和完整 stream reactor 125 tests 通过。
3. 通过 savepoint `s3://ft-mes-bpi-checkpoints/savepoints/savepoint-d8fdb5-6077b086bf66`
   有状态升级到 deployment `ubuntu-test-v17-308cca82`；修复后同一规则生成 START 和 END 候选。
4. END 页面验收最初等待旧文本 `END`，实际抽屉标题为 `END 候选`。提交 `f669bb1e` 修复定位器，
   最终真实页面确认通过。
5. 目标 WOM 映射原先缺少 `wom_runstate/finished -> inactive`。补齐目标配置后，提交
   `f669bb1e` 又把显式 runing/finished 双映射加入启用前门禁，防止未来部署只发送 active 上下文。
6. 修复前的 `UNIT_MISMATCH` incident 通过真实数据质量页面从 OPEN/r5 经 ACKNOWLEDGED/r6
   进入 RESOLVED/r7；5 条原始事件没有删除。

## Flink 与恢复状态

- JAR SHA-256：`7fbcacf8b3a8726b1210d02f7ae122bbb993ef6c84421bfdd96e1f34aaefa11b`。
- 当前 job：`ffe9ab719bbf7250b682f77f75641f17`，`RUNNING 36/36`。
- 当前复验 checkpoint：`5533`，36/36 子任务确认；累计 58 completed、0 failed。
- service image：`ft-mes-bpi-service:20260720-calibration-evidence-13b1296c`，healthy。
- adapter、WOM context publisher、JetLinks、三 broker Kafka 和两个 PostgreSQL 均保持运行。

## 验收收尾

- START/END 规则：2 条均 `RETIRED/r5`，运行时 `INACTIVE`。
- 测试校准：`REVOKED/r3`；IoT 原校准映射和 JetLinks 已恢复。
- `bpi.commands`：LINE 覆盖 `active=false/r6`，有效值恢复 false。
- WOM fixture：task 0、wait 0；最新 finished context 为 inactive/SENT。
- SQL 历史遥测 fixture 和 golden boundary：均 0。
- pending candidate：0；active batch：0。
- 作为不可变验收证据保留：2 条 CONFIRMED candidate、1 条 CLOSED_RAW 影子批次、边界证据、
  状态事件、规则/拓扑版本、数据质量原始事件与处置审计。

## 剩余投产门槛

1. 用物理 DEVICE/GATEWAY 重复 epoch/sequence、断线重连和掉电恢复验收。
2. 由现场计量人员提交与当前目录版本精确匹配的真实证书和校验和，并由独立管理员批准。
3. 在选定产线连续运行 7-14 天，统计真实边界认同率、时间偏差、累计量偏差和数据质量分布。
4. 在影子门槛和业务签字通过前，继续保持 QCS/WMS 只读与生产写回关闭。
5. 完成真实负载下 BPI service、adapter、Kafka/Flink、PostgreSQL 的跨组件整体回切演练。

机器可读证据：
[`metadata/bpi-live-mqtt-wom-start-end-acceptance.json`](../../metadata/bpi-live-mqtt-wom-start-end-acceptance.json)。

页面证据：

- `metadata/bpi-live-mqtt-wom-start-candidate.png`
- `metadata/bpi-live-mqtt-wom-end-candidate.png`
- `metadata/bpi-live-mqtt-wom-unit-mismatch-resolved.png`
