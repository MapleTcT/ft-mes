# BPI 来源序列证据与硬准入验收

## 结论

状态：`PASS_TARGET_GUARD_WITH_BLOCKED_SOURCE`

2026-07-20 已把来源序列从目录中的布尔声明升级为可审计运行证据，并部署到
`10.11.100.17` 当前唯一 ADP 测试环境。JetLinks 根据 Redis 中同一绑定、同一 epoch 的严格递增序列
生成 `DISABLED / MISSING / PENDING / QUALIFIED / EXPIRED` 证据，经 Kafka Protobuf 进入 MES，
由 Flyway V22 幂等落入 PostgreSQL。点位准入只接受与当前目录快照精确匹配且未过期的
`QUALIFIED` 证据；当前现场没有真实遥测，因此页面保持 `DISABLED / BLOCKED / 0 READY`，没有伪造
生产可用状态。

## 实现契约

- IoT 资格判定要求同一 `binding fingerprint + source origin + source epoch` 至少两次严格递增序列；
  重复、倒退或 epoch 变化会重新进入 `PENDING`。
- Kafka key、Protobuf 内容地址事件 ID、消息头、tenant/plant/line allowlist 和 payload checksum 必须一致；
  无效消息回滚并进入独立 DLQ。
- MES 当前证据表以 tenant、来源实例、产线、产品、设备和绑定指纹唯一；重复心跳更新 current revision，
  首次状态变化写审计，所有交付写 inbox。
- 拓扑/影子验收要求目录快照、来源 origin、绑定指纹、证据时间和已批准校准全部匹配；默认失败关闭。
- PostgreSQL 是唯一默认数据库；Oracle 未进入本次运行链。

## 本地验证

| 验收项 | 结果 |
|---|---|
| BPI simulator | `11/11 PASS` |
| 真实 PostgreSQL 定向验收 | `17/17 PASS`，包含 Kafka 严格校验、幂等、冲突回滚和 V22 落库 |
| 后端完整 reactor | `99 tests / 0 failures / 0 errors / 1 expected skipped` |
| 前端构建 | TypeScript/Vite `PASS` |
| 浏览器 E2E | `15/15 PASS` |
| IoT 可部署包 | Maven `40/40 modules BUILD SUCCESS`；JAR SHA-256 `1edd2ce048fc46307ee5faedfe40904d29d106368788d95a641ba7dcf8c44e98` |

## 目标环境证据

| 证据 | 实际结果 | 状态 |
|---|---|---|
| Flyway | PostgreSQL `15.18`，`bpi.flyway_schema_history` 当前 V22 | PASS |
| Kafka | 证据与 DLQ 均为 3 分区、RF=3、minISR=2；证据 topic compact，DLQ 保留 30 天 | PASS |
| IoT | JetLinks/exporter/Redis/R2DBC 均 `UP`，点位目录第二次同步成功 | PASS |
| MES consumer | `ft-mes-bpi-service-source-sequence-v1` partition 1 offset `2/2`、lag 0 | PASS |
| DLQ | 三个分区 offset 均为 0 | PASS |
| PostgreSQL current | 1 行，`DISABLED`，revision `2`，最新 observedAt `2026-07-20 07:47:18.534+08` | PASS |
| Inbox / audit | 2 个内容地址 inbox 事件；首次 `DISABLED` 状态写 1 条审计 | PASS |
| 点位快照 | snapshot `c6c09084-82dd-46f8-81e4-98b336b46e65`，1 点、0 READY | PASS |
| 防误触发 | tenant `1000` 的 candidate=0、batch=0，外部写入仍关闭 | PASS |
| Flink | job `1e981b842f4693e49f3c3def0fb98cb6` 为 `RUNNING 36/36` | PASS |

目标目录 entry 已携带：

```text
origin=GATEWAY
bindingFingerprint=sha256:d5a753a203342f75c52f8ab412ccbf7cecb8251864017de7985958d90ef6f640
sourceSequenceEvidenceStatus=DISABLED
readinessIssues=CALIBRATION_NOT_VERIFIED,SOURCE_SEQUENCE_DISABLED
```

直接查库 SQL：

```sql
SELECT status, source_event_id, observed_at, revision
FROM bpi.bpi_source_sequence_evidence_current
WHERE tenant_id = '1000';

SELECT source, event_id, idempotency_key, processed_at
FROM bpi.bpi_inbox_events
WHERE source = 'iot.source-sequence.evidence.v1'
ORDER BY processed_at;

SELECT id, point_count, source_claim_ready_point_count, observed_at
FROM bpi.bpi_point_catalog_snapshots
ORDER BY imported_at DESC
LIMIT 1;
```

## 真实浏览器

入口：`http://10.11.100.17:18080/bpi/#/points`。

- ADP 登录后，`GET /bpi-api/point-catalog/current` 和
  `GET /bpi-api/point-calibrations` 均为 `200`。
- 页面显示目录 1 点、就绪 0 点；源序列证据抽屉显示 `DISABLED / r2 / BLOCKED`、来源类型、绑定指纹和
  内容地址事件 ID。
- 页面明确说明“系统保持失败关闭，不会仅凭目录中的启用声明放行”。
- console error=0、page error=0，1600x1000 视口无横向溢出；筛选和抽屉关闭交互可用。
- 固化截图：[`metadata/bpi-source-sequence-evidence-v22.png`](../../metadata/bpi-source-sequence-evidence-v22.png)。

## 回滚演练

备份目录：`/home/v6/adp-deploy-backups/20260720-073058-source-sequence-v22`，包含三套 Compose/env、
前端静态文件和迁移前 PostgreSQL custom-format dump。

MES 服务已真实执行：

```text
ft-mes-bpi-service:rollback-20260720-073058-source-sequence-v22 -> UP
ft-mes-bpi-service:20260720-source-sequence-v22-6b2eb3e7 -> UP
```

V22 为 expand-only，回滚过程中不回退数据库。JetLinks 回滚镜像已验证可执行 Java 17 runtime，未实际切换
采集容器，以避免人为制造采集空窗。当前 JetLinks 镜像为
`mapletct/jetlinks-bpi-pilot:20260720-source-sequence-beefd1d5`。

## 未完成边界

- 当前试点没有真实遥测，证据只能是 `DISABLED`；尚未证明真实网关在重连后切换 epoch 并持续单调递增。
- 当前校准为 `UNVERIFIED`；即使后续序列证据合格，校准未批准时也必须继续 BLOCKED。
- 尚未用同一生产 marker 闭合 IoT 遥测、MES production context、Flink candidate、影子 batch。
- 本次处于 SHADOW，禁止 WOM/QCS/WMS 自动写回；需先完成连续 7-14 天影子验收。
