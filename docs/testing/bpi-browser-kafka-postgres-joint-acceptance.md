# BPI 浏览器、Kafka/Flink 与 PostgreSQL 联合验收

## 结论

2026-07-14 在 `ubuntu-test`（Tailscale `100.99.133.43`）完成一次受控 Phase 1
联合验收，结论为 **PASS**。同一个 marker
`ADP_E2E_20260714_091536_BPI_JOINT` 从真实 BPI 页面进入 Java 服务，经过
PostgreSQL outbox、三 broker Kafka、Flink checkpoint 和应用回执，再由受控 MES
上下文与遥测形成唯一候选；候选通过真实页面确认后，PostgreSQL 中形成影子批次、
边界证据、状态事件和审计记录。

本结论不代表 BPI 产品或 MES 已可投产。拓扑/规则仍由受控验收 fixture 提供，输入
仍是确定性上下文/遥测，不是真实 `MapleTcT/iot` exporter 或 MES outbox，也没有完成
7-14 天影子运行和 QCS/WMS 写回。

机器可读证据见
[`metadata/bpi-browser-kafka-postgres-joint-acceptance.json`](../../metadata/bpi-browser-kafka-postgres-joint-acceptance.json)。

## 验收范围

| 项目 | 值 |
|---|---|
| tenant / plant / line | `1000 / PLANT-01 / LINE-S07-01` |
| topology | `TOPO-ADP_E2E_20260714_091536_BPI_JOINT@1` |
| rule | `RULE-ADP_E2E_20260714_091536_BPI_JOINT@1` |
| device | `DEVICE-ADP_E2E_20260714_091536_BPI_JOINT` |
| 历史回放边界 | `2026-07-14T09:10:00Z` |
| BPI 页面 | `http://100.99.133.43:18091` |
| Flink job | `1f55da2611f488cce0de0638d8f20b6f` |

消费者只在验收期间对白名单范围开放。验收结束后已恢复
`candidate=false`、`rule-publication-outbox=false`、
`rule-application=false` 和 deny-all scope；既有 ADP/MES Compose 没有停止或替换。

## 联合链路

| 阶段 | 实际动作与结果 | 状态 |
|---|---|---|
| 浏览器认证 | ADP 登录 `200`，BPI 页面标题“智能批次”，无 console/page/request error | PASS |
| 规则模拟 | `POST /bpi-api/rules/{id}/simulate` 返回 `202`；2 条历史观测匹配 1 条 golden boundary，误报/漏报均为 0，平均边界误差 0 | PASS |
| 规则发布 | `POST /bpi-api/rules/{id}/publish` 返回 `200`；规则为 `PUBLISHED` revision 3，outbox 创建为 `PENDING/WAITING` | PASS |
| Kafka/Flink 应用 | outbox 投递后页面最终显示“Flink 已应用”；PostgreSQL 为 `publication_status=PUBLISHED`、`application_status=APPLIED`、deployment `ubuntu-test-v1` | PASS |
| 上下文/遥测 | 向 `mes.production.context.v1` 写 1 条上下文，向 `iot.telemetry.selected.v1` 写 3 条遥测；输入 offset 被记录 | PASS |
| 候选生成 | `bpi.batch.candidate.v1` 只出现 1 条匹配候选，匹配数据质量问题为 0；候选 listener 在 PostgreSQL 只落 1 条 `PENDING` | PASS |
| 浏览器确认 | `POST /bpi-api/candidates/{id}/confirm` 返回 `200`；候选 `CONFIRMED` revision 2，影子批次 `ACTIVE` revision 1 | PASS |
| 落库核对 | 1 个 candidate、1 个 batch、2 条 boundary evidence、1 条 state event、3 条 audit event；flow=19.0 和 pump=true 均为 `GOOD` | PASS |
| 规则退场 | 从真实 rule topic 读取本次版本，发布 typed `active=false`，Flink 返回新的 `APPLIED` | PASS |
| 数据清理 | 单事务定向删除 marker；topology/rule/candidate/batch 剩余均为 0 | PASS |
| 恢复复验 | 恢复消费者默认关闭后，真实浏览器概览 `200`、SHADOW 可见、错误为 0 | PASS |

## PostgreSQL 验收

关键查询由 `deploy/bpi-runtime/sql/joint-acceptance-verify.sql` 执行。验收时的结果摘要：

| 目标表 | 验收结果 |
|---|---|
| `bpi_rule_versions` | `PUBLISHED`，revision 3 |
| `bpi_rule_simulations` | 1 条，与本次规则版本关联 |
| `bpi_outbox_events` | 1 条，publication `PUBLISHED`，application `APPLIED` |
| `bpi_inbox_events` | 应用回执和候选事件按 event/idempotency key 入箱 |
| `bpi_batch_candidates` | 1 条，页面确认后 `CONFIRMED` revision 2 |
| `bpi_batch_instances` | 1 条，`ACTIVE`、`is_shadow=true` |
| `bpi_boundary_evidence` | 2 条，分别对应流量和泵运行信号 |
| `bpi_batch_state_events` | 1 条影子批次状态事件 |
| `bpi_audit_events` | 3 条规则/候选/批次审计 |

验收 SQL 只用于查询、受控 fixture 和 marker 定向清理。业务状态变化全部由浏览器/API、
outbox/Kafka/Flink 和正式服务链路完成，没有用 SQL 直写冒充业务动作。

## 清理与安全边界

1. `run-rule-deactivation.sh` 发布与本次规则同版本的 typed inactive 事件，并等待 Flink
   `APPLIED`，避免仅删数据库后 Broadcast State 仍保留规则。
2. `joint-acceptance-cleanup.sql` 在一个事务中删除本 marker 的审计、证据、状态事件、
   幂等、inbox、候选、批次、outbox、模拟、golden、历史遥测、feature flag、规则和拓扑。
3. 清理后 topology/rule/candidate/batch 均为 0，消费者恢复默认关闭和 deny-all。
4. 报告不保存密码、token、cookie、私钥、数据库口令或 `.env` 内容。

## 发现并修复的问题

目标入口是普通 HTTP。`crypto.randomUUID()` 只在安全上下文可用，原前端点击写命令时会
在发送请求前失败。前端现优先使用 `crypto.randomUUID()`，不可用时通过
`crypto.getRandomValues()` 生成符合 v4/variant 位要求的 UUID；浏览器 E2E 显式移除
`randomUUID` 后仍能确认候选。该修复覆盖候选确认/拒绝、规则模拟/发布/重试和批次
暂停/恢复。

## 可复验入口

| 资产 | 用途 |
|---|---|
| `deploy/bpi-runtime/sql/joint-acceptance-seed.sql` | 创建唯一 marker 的受控 topology/rule/golden/history fixture |
| `deploy/bpi-runtime/scripts/browser-joint-acceptance.js` | 真实浏览器 `publish`、`confirm`、`read` 三阶段验收 |
| `deploy/bpi-streaming/scripts/run-joint-replay.sh` | 发送受控上下文/遥测并要求唯一候选 |
| `deploy/bpi-runtime/sql/joint-acceptance-verify.sql` | 查询规则、回执、候选、批次、证据和审计 |
| `deploy/bpi-streaming/scripts/run-rule-deactivation.sh` | typed inactive 与 Flink 应用确认 |
| `deploy/bpi-runtime/sql/joint-acceptance-cleanup.sql` | 单 marker 定向清理 |

原始报告位置：

- `/tmp/bpi-joint-browser-publish.json`
- `/tmp/bpi-streaming-evidence/bpi-joint-replay.json`
- `/tmp/bpi-joint-browser-confirm.json`
- `/tmp/bpi-streaming-evidence/bpi-rule-deactivation.json`
- `/tmp/bpi-joint-browser-read-after-cleanup.json`
