# BPI 目标测试环境部署与验收

## 结论

2026-07-14 已在 `ubuntu-test`（Tailscale `100.99.133.43`）完成 BPI 独立运行栈和流处理栈部署。此前的磁盘阻断已经解除，既有 `adp-mes-newbase` Compose 未被替换或停止。

当前结论为 **PARTIAL**：目标环境运行健康、真实浏览器只读链路、Kafka/Flink marker 数据面和带负载 TaskManager 恢复分别通过；浏览器规则发布到批次确认落库的完整写链尚未执行，不能宣称 BPI Phase 1 完成。

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

## 当前阻断

| 阻断项 | 原因 | 完成条件 |
|---|---|---|
| 规则/拓扑产品数据 | BPI PostgreSQL 当前没有可供页面操作的产线拓扑和规则；现有 UI/API 以读取、模拟、发布为主，尚缺产品化创建或导入入口 | 建立受审计的创建/导入路径，或用明确标记的验收 fixture 完成一次受控测试 |
| 完整写链 | 还没有用同一 marker 串起浏览器发布、outbox、Kafka、Flink 应用回执、PostgreSQL APPLIED/audit、候选确认和 batch/evidence/audit | 全链 API、offset、checkpoint、目标表和浏览器结果均可复验 |
| 现场数据 | 尚未接入 `MapleTcT/iot` exporter 和真实 MES production context | 完成点位、单位、质量码、sequence、生产指令和 locality group 对账 |
| 影子运行 | 尚未连续运行 7-14 天 | 达到边界人工认同率、累计量偏差和数据质量门槛 |
| 生产写回 | Phase 1 不允许直接写 WOM/QCS/WMS | 影子运行门槛通过后，再设计幂等写回、补偿和回滚验收 |

## 下一步验收顺序

1. 创建唯一 `ADP_E2E_*` marker 的 topology/rule fixture，并把 fixture 明确标记为验收资产，不作为日常产品配置方式。
2. 只为 `1000 / PLANT-01 / LINE-S07-01` 打开 candidate、rule publication 和 rule application consumer allowlist。
3. 在真实浏览器执行规则模拟和发布，记录 HTTP method、URL、payload、response、Kafka offset 和 Flink checkpoint。
4. 查询 `bpi_outbox_events`、规则应用回执、候选 inbox、`bpi_batch_candidates`、`bpi_batch_instances`、`bpi_boundary_evidence` 和 `bpi_audit_events`。
5. 在浏览器确认候选并复查批次、证据、审计和幂等行；验收结束后恢复消费者默认关闭状态。
6. 补 broker 故障、savepoint 升级和整体回滚，再接真实 IoT/MES 数据进入影子运行。

## 原始报告位置

目标机保留本次可复验原始报告：

- `/tmp/bpi-runtime-smoke.json`
- `/tmp/bpi-streaming-cluster-smoke.json`
- `/tmp/bpi-streaming-evidence/bpi-kafka-replay.json`
- `/tmp/bpi-streaming-loaded-taskmanager-recovery.json`

本地浏览器报告为 `/tmp/bpi-target-browser-smoke.json`。这些报告不包含密码、token、cookie 值或数据库连接密钥。
