# BPI 目标测试环境部署与验收

## 结论

2026-07-14 已在 `ubuntu-test`（Tailscale `100.99.133.43`）完成 BPI 独立运行栈和流处理栈部署。此前的磁盘阻断已经解除，既有 `adp-mes-newbase` Compose 未被替换或停止。

当前环境结论为 **PASS_PHASE1_CONTROLLED**：目标环境运行健康，真实浏览器、Kafka/Flink、PostgreSQL 和带负载 TaskManager 恢复均已通过；同一 marker 的规则发布、应用回执、候选确认和影子批次落库链已经闭合。`MapleTcT/iot@357a285b` 还以独立受控 marker 跑通了目标 JetLinks EventBus、exporter、Kafka 到 Flink source。该结论只覆盖受控 Phase 1 技术链，不代表拓扑/规则产品化、真实网关/协议设备点位接入、IoT + MES context 同 marker 候选/批次、连续影子运行或生产投用完成。

机器可读证据见 [`metadata/bpi-test-environment-acceptance.json`](../../metadata/bpi-test-environment-acceptance.json)。

## 隔离边界

| 范围 | Compose project | 入口 | 结果 |
|---|---|---|---|
| 既有 ADP/MES | `adp-mes-newbase` | `http://100.99.133.43:18080` | 保持原运行栈，不由 BPI 编排接管 |
| BPI Web/Adapter/Service/PostgreSQL | `ft-mes-bpi-runtime` | `http://100.99.133.43:18091` | Web、adapter、service、PostgreSQL healthy |
| BPI Kafka/Flink/MinIO | `ft-mes-bpi-streaming` | Flink REST `http://100.99.133.43:18081` | 3 broker、2 TaskManager、MinIO 和 Flink job 正常 |

Java 17 BPI 服务端口、Java 8 adapter、PostgreSQL 和内部 JWT 均不直接暴露给浏览器。Web 只暴露同源 `/bpi-api`，Nginx 转发到 Java 8 adapter。

## 已通过验收

| 验收项 | 实际结果 | 状态 |
|---|---|---|
| Runtime smoke | Java 服务 `UP`、Web `UP`、adapter `UP`；数据库 `ft_mes_bpi`，Flyway V8，19 张 schema 表 | PASS |
| 真实浏览器 | ADP 登录 `200`，`suposTicket` cookie 存在；BPI 页面 `200`，标题/品牌/概览/空态/SHADOW 均可见 | PASS |
| 浏览器 API | `GET /bpi-api/overview?plantId=PLANT-01&onlyAbnormal=false` 返回 `200`；console/page/request error 均为 0 | PASS |
| 认证桥接 | 旧平台不透明票据经可信 gateway 验证，服务端映射角色和 tenant/plant/line，再签发短期内部 JWT | PASS |
| Kafka | 3 broker、8 个 BPI topic，副本 3，`min.insync.replicas=2` | PASS |
| Flink | `ft-mes-bpi-batch-boundary-v1` 为 `RUNNING`，30/30 task；2026-07-14 16:17 复查累计 144 个成功 checkpoint、0 失败 | PASS |
| 固定 marker 回放 | `ADP_E2E_20260714_071034_1503790` 输入规则、上下文和 3 条遥测，只产生 1 个 committed candidate，数据质量错误 0 | PASS |
| TaskManager 恢复 | 带负载重启 `bpi-taskmanager-2` 后 30/30 task 恢复，attempt `0 -> 1`，checkpoint `13 -> 14` | PASS |
| 同一 marker 联合写链 | `ADP_E2E_20260714_091536_BPI_JOINT` 完成真实浏览器规则模拟/发布、outbox、Kafka、Flink `APPLIED`、唯一候选、浏览器确认和影子批次/证据/审计落库 | PASS |
| JetLinks EventBus source | `ADP_BPI_E2E_20260714_145738_757314` 触发 exporter received/enqueued/published 增量 `1`，Kafka partition 4 offset `3 -> 4`，Flink consumer offset `4/4`、lag `0`；试点入口恢复关闭 | PASS_SOURCE_ONLY |
| 验收退场与恢复 | 发布 typed inactive 并获 Flink `APPLIED`；单事务清理后 topology/rule/candidate/batch 均为 0；消费者默认关闭，浏览器概览再次 `200` 且错误为 0 | PASS |

## 产品级剩余缺口

| 阻断项 | 原因 | 完成条件 |
|---|---|---|
| 规则/拓扑产品数据 | 本次已用受控 fixture 通过联合验收，但日常页面仍缺可审计创建/导入入口 | 建立产品化创建/导入、版本比较、审批和回滚路径；验收 fixture 不进入日常配置 |
| 现场数据 | exporter 与 WOM context 已分别在目标机通过，但 EventBus marker 不是现场网关/协议设备点位，且两端没有用同一 marker 形成 candidate/batch | 完成真实点位、单位、质量码、sequence、生产指令和 locality group 对账，并闭合 IoT + MES context 联合链 |
| 影子运行 | 尚未连续运行 7-14 天 | 达到边界人工认同率、累计量偏差和数据质量门槛 |
| 生产写回 | Phase 1 不允许直接写 WOM/QCS/WMS | 影子运行门槛通过后，再设计幂等写回、补偿和回滚验收 |

## 下一步验收顺序

1. 把本次同一 marker 联合验收固化为每次 BPI 发布前的目标环境回归基线。
2. 补 topology/rule 产品化创建或导入入口，不把受控 fixture 当成用户功能。
3. 补 broker 故障、savepoint 升级和整体回滚演练。
4. 把 `MapleTcT/iot@357a285b` 接到真实网关/协议设备点位，并与已通过的 MES production context 配置到同一试点 scope。
5. 用同一 marker 闭合真实设备 EventBus、exporter、Kafka、Flink、BPI PostgreSQL candidate/batch 和浏览器证据链。
6. 连续运行 7-14 天并达到边界认同率、累计量偏差和数据质量门槛。
7. 门槛通过后再设计 QCS/WMS 幂等写回、补偿和回滚，禁止提前改写生产状态。

## 原始报告位置

目标机保留本次可复验原始报告：

- `/tmp/bpi-runtime-smoke.json`
- `/tmp/bpi-streaming-cluster-smoke.json`
- `/tmp/bpi-streaming-evidence/bpi-kafka-replay.json`
- `/tmp/bpi-streaming-loaded-taskmanager-recovery.json`
- `/tmp/bpi-streaming-evidence/bpi-joint-replay.json`
- `/tmp/bpi-streaming-evidence/bpi-rule-deactivation.json`

本地浏览器报告为 `/tmp/bpi-target-browser-smoke.json`、`/tmp/bpi-joint-browser-publish.json`、`/tmp/bpi-joint-browser-confirm.json` 和 `/tmp/bpi-joint-browser-read-after-cleanup.json`。联合验收细节见 [BPI 浏览器、Kafka/Flink 与 PostgreSQL 联合验收](bpi-browser-kafka-postgres-joint-acceptance.md)。这些报告不包含密码、token、cookie 值或数据库连接密钥。
