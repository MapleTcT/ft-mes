# BPI 目标测试环境部署与验收

## 结论

2026-07-14 已在 `ubuntu-test`（Tailscale `100.99.133.43`）完成 BPI 独立运行栈和流处理栈部署；
2026-07-15 将拓扑/规则产品化和点位目录自动同步版本部署到同一运行栈，后续以 expand-only
迁移推进到 Flyway V16。此前的磁盘阻断已经解除，既有 `adp-mes-newbase` Compose 未被替换或停止。

2026-07-20 运行态复查确认 BPI Web、Java 8 adapter、Java 17 service 和 PostgreSQL 已并入唯一的
`adp-mes-newbase` Compose，当前公司内网页面入口为 `http://10.11.100.17:18080/bpi/`；Tailscale
`100.99.133.43` 是此前外网办公路径，早期独立
`:18091` 页面入口不再作为当前地址。Kafka/Flink/MinIO 继续由隔离的 `ft-mes-bpi-streaming`
Compose 承载。

当前环境结论为 **PASS_PHASE1_NATIVE_SHELL_GOVERNANCE**：既有真实浏览器、Kafka/Flink、PostgreSQL、
故障恢复和应用组件回退证据继续有效；源码
`df6fdb0e5ddb929626dd0ea3c81b170afbaa62a4` 在 Flyway V21 上把 `bpi.ui` 接到旧 MES
原生菜单读取点，并通过真实页面完成启用、禁用、恢复继承、审计、幂等、marker 清理、iframe 进入和
adapter 故障回退。最终测试环境保留 LINE `bpi.ui=true/active/r1`，Flink job 保持
`RUNNING 36/36`。`MapleTcT/iot@beefd1d5` 已把 JetLinks EventBus、exporter、持久化序列状态、
内容寻址点位目录和来源序列证据接入 Kafka；MES Flyway V22 消费证据并落入 PostgreSQL。目标机当前
真实 evidence 为 `DISABLED / r2`，目录仍是 1 点/0 READY，浏览器证据抽屉明确显示失败关闭；来源序列
READY 仍要求最近匹配配置的遥测先进入持久化 spool，并存在未过期的 `30m` Redis 证据。该结论只覆盖受控 Phase 1 技术链；
不代表真实网关/协议设备连续单调序列、IoT + MES context 同 marker 候选/批次、连续影子运行或生产投用完成。

早期部署基线见 [`metadata/bpi-test-environment-acceptance.json`](../../metadata/bpi-test-environment-acceptance.json)；
当前原生菜单增量验收见
[`metadata/bpi-shell-menu-gate-acceptance.json`](../../metadata/bpi-shell-menu-gate-acceptance.json)，来源序列 V22
证据见 [`metadata/bpi-source-sequence-readiness-acceptance.json`](../../metadata/bpi-source-sequence-readiness-acceptance.json)。

## 隔离边界

| 范围 | Compose project | 入口 | 结果 |
|---|---|---|---|
| ADP/MES + BPI Web/Adapter/Service/PostgreSQL | `adp-mes-newbase` | ADP `http://10.11.100.17:18080`；BPI `/bpi/` | 单套运行栈；Nginx、adapter、service、PostgreSQL 均在运行，BPI service/adapter healthy |
| BPI Kafka/Flink/MinIO | `ft-mes-bpi-streaming` | Flink REST 仍按 Compose 绑定 `http://100.99.133.43:18081` | 3 broker、2 TaskManager、MinIO 和 Flink job 正常；该诊断端口未绑定公司内网 `10.11.100.17` |

浏览器正常链路只使用同源 `/bpi-api`，Nginx 再转发到 Java 8 adapter。当前测试机仍将 service
`19091` 和 adapter `19080` 发布到主机网络，属于测试诊断暴露面；生产部署必须改为回环绑定或由
防火墙限制，PostgreSQL 和内部 JWT 不得直接暴露给浏览器。

## 已通过验收

| 验收项 | 实际结果 | 状态 |
|---|---|---|
| Runtime smoke | Java 服务 `UP`、Web `UP`、adapter `UP`；数据库 `ft_mes_bpi` 当前为 Flyway V21 | PASS |
| 真实浏览器 | ADP 登录 `200`，`suposTicket` cookie 存在；BPI 页面 `200`，标题/品牌/概览/空态/SHADOW 均可见 | PASS |
| 浏览器 API | `GET /bpi-api/overview?plantId=PLANT-01&onlyAbnormal=false` 返回 `200`；console/page/request error 均为 0 | PASS |
| 运行开关治理 | `ADP_E2E_BPI_FLAGS_20260720_034527_0cf61838` 在 `/bpi/#/featureFlags` 完成 LINE 禁用和恢复继承；PostgreSQL 清理前 `1/2/2`、清理后 `0/0/0`，桌面/移动 BPI 错误为 0 | PASS_TARGET_GOVERNED_CLEANED |
| 旧 MES 原生菜单门禁 | `ADP_E2E_BPI_SHELL_20260720_050100_df6fdb0e` 从真实页面完成 `ENABLE -> DISABLE -> INHERIT`；菜单 `28/0 -> 29/1 -> 28/0`，iframe 显示“实时生产态势”；PostgreSQL 清理前 `1/3/3`、后 `0/0/0`；adapter 停止时 Nginx 回退 gateway 原菜单，最终测试配置为 LINE active/enabled r1 | PASS_TARGET_NATIVE_SHELL_GOVERNED |
| 认证桥接 | 旧平台不透明票据经可信 gateway 验证，服务端映射角色和 tenant/plant/line，再签发短期内部 JWT | PASS |
| Kafka | 3 broker、12 个配置内 BPI 业务 topic，副本 3，`min.insync.replicas=2`；另保留 1 个 broker-chaos 验收 topic | PASS |
| Flink | `ft-mes-bpi-batch-boundary-v1` 当前为 `RUNNING`、36/36 task；`100.99.133.43:18081/jobs/overview` 复查通过。历史应用回滚的 33-task checkpoint 证据继续保留 | PASS |
| 固定 marker 回放 | `ADP_E2E_20260714_071034_1503790` 输入规则、上下文和 3 条遥测，只产生 1 个 committed candidate，数据质量错误 0 | PASS |
| TaskManager 恢复 | 带负载重启 `bpi-taskmanager-2` 后 30/30 task 恢复，attempt `0 -> 1`，checkpoint `13 -> 14` | PASS |
| 单 Broker 故障恢复 | `ADP_BPI_BROKER_CHAOS_20260719_1129` 停止 `kafka-2`；151 个分区无 unavailable/低于 minISR，marker 恰好一次，checkpoint `2481 -> 2482 -> 2483`、失败数 0；恢复后 ISR=3，标准 smoke checkpoint `2485` | PASS |
| Service/Adapter 镜像回退 | `ADP_BPI_RUNTIME_ROLLBACK_20260719_120102` 将两个组件真实回退到上一版镜像；健康、页面/API 200、浏览器错误 0，Flyway V16 和核心表计数不变；随后恢复精确 tag/image ID 并复验 | PASS |
| Flink JAR 双向回退 | `ADP_BPI_FLINK_ROLLBACK_20260719_120659` 从当前 canonical savepoint 恢复上一版 JAR，再从上一版 savepoint 恢复当前 JAR；两次均 `allowNonRestoredState=false`、33/33 task、checkpoint 推进、集群 smoke PASS | PASS |
| 同一 marker 联合写链 | `ADP_E2E_20260714_091536_BPI_JOINT` 完成真实浏览器规则模拟/发布、outbox、Kafka、Flink `APPLIED`、唯一候选、浏览器确认和影子批次/证据/审计落库 | PASS |
| JetLinks EventBus source | `ADP_BPI_E2E_20260714_145738_757314` 触发 exporter received/enqueued/published 增量 `1`，Kafka partition 4 offset `3 -> 4`，Flink consumer offset `4/4`、lag `0`；试点入口恢复关闭 | PASS_SOURCE_ONLY |
| JetLinks 点位目录自动同步 | revision `sha256:2a218d12...151ce5` 经 `iot.point-catalog.snapshot.v1` 落入 1 个 snapshot、1 个 entry、1 个幂等和 1 个审计；重复/重启不增行，毒消息进入 DLT，真实页面读取 `200` 且浏览器错误为 0 | PASS_CONTROL_WITH_BLOCKED_SOURCE |
| JetLinks 来源序列运行时证据 | `MapleTcT/iot@beefd1d5` 和 MES Flyway V22 已部署；证据 topic RF=3/minISR=2/compact，consumer lag=0、DLQ=0；PostgreSQL current 为 `DISABLED/r2`，inbox=2，目录 1 点/0 READY；真实页面/API/证据抽屉和 MES 镜像回滚往返通过 | PASS_TARGET_GUARD_WITH_BLOCKED_SOURCE |
| 拓扑/规则产品化目标验收 | `ADP_E2E_20260715_004849_BPI_PRODUCT_TARGET` 完成真实 ADP 页面拓扑创建/校验、创建人发布 `422` 门禁、独立管理员发布、规则草稿、4 条审计、4 条幂等、服务重启后页面读取 | PASS |
| 验收退场与恢复 | 发布 typed inactive 并获 Flink `APPLIED`；单事务清理后 topology/rule/candidate/batch 均为 0；消费者默认关闭，浏览器概览再次 `200` 且错误为 0 | PASS |

## 产品级剩余缺口

| 阻断项 | 原因 | 完成条件 |
|---|---|---|
| 产品级回切 | 规则/拓扑版本比较、审批、受控退役以及 service/adapter/Flink 应用组件回退已通过；尚未在真实业务负载下执行跨组件同时回切和流量恢复 | 在生产等价维护窗口用受控业务 marker 演练 runtime、Flink、Kafka consumers 和入口流量的编排回切，并完成业务签字 |
| 现场数据 | exporter、自动点位目录与 WOM context 已分别在目标机通过；试点产品/设备、`instantFlow` metadata 和单位已注册激活，但真实证书匹配、连续单调来源序列及两端同 marker candidate/batch 尚未完成 | 由现场计量人员提交与当前目录 calibrationVersion 精确匹配的真实证书；启用强制来源序列并用多条真实 DEVICE/GATEWAY 事件证明 `source_epoch + sequence` 连续单调和重连语义，等待自动新 revision 通过准入后闭合 IoT + MES context 联合链 |
| 影子运行 | 尚未连续运行 7-14 天 | 达到边界人工认同率、累计量偏差和数据质量门槛 |
| 生产写回 | Phase 1 不允许直接写 WOM/QCS/WMS | 影子运行门槛通过后，再设计幂等写回、补偿和回滚验收 |

## 下一步验收顺序

1. 把同一 marker 联合验收、单 broker 故障和应用组件双向回退固化为每次 BPI 发布前的目标环境回归基线。
2. 保持 topology/rule 页面创建、校验、独立发布、版本比较、审批、受控退役、规则绑定、运行开关治理、旧 MES 原生菜单门禁/回退和重启读取作为每次发布回归。
3. 在生产等价维护窗口继续完成 BPI 跨组件整体回切和真实业务负载演练；数据库始终采用 expand-only，不执行破坏性降级。
4. 把 `MapleTcT/iot@beefd1d5` 接到真实网关/协议设备点位，补齐真实标定，并用多条真实事件证明持久来源序列连续单调和重连换 epoch 语义，等待自动目录生成新 revision；禁止手工伪造 READY。
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
- `/tmp/bpi-runtime-v9-smoke-final.json`
- `/tmp/bpi-point-catalog-sync-scope-20260715.json`
- `/tmp/ADP_BPI_BROKER_CHAOS_20260719_1129.json`
- `/tmp/bpi-broker-chaos-post-smoke.json`
- `/tmp/ADP_BPI_FLINK_ROLLBACK_20260719_120659.json`
- `/data/docker/bpi-upgrade-backups/ADP_BPI_RUNTIME_ROLLBACK_20260719_120102`
- `/data/docker/bpi-upgrade-backups/ADP_BPI_FLINK_ROLLBACK_20260719_120659`
- `/home/v6/adp-evidence/ADP_E2E_BPI_FLAGS_20260719T194145Z_DEPLOY.txt`
- `/home/v6/adp-evidence/ADP_E2E_BPI_FLAGS_20260720_034527_0cf61838-db-before-cleanup.txt`
- `/home/v6/adp-evidence/ADP_E2E_BPI_FLAGS_20260720_034527_0cf61838-db-cleanup.txt`
- `/home/v6/adp-evidence/ADP_E2E_BPI_SHELL_20260720_050100_df6fdb0e_PRE_CLEAN.txt`
- `/home/v6/adp-evidence/ADP_E2E_BPI_SHELL_20260720_050100_df6fdb0e_CLEANUP.txt`
- `/home/v6/adp-evidence/BPI_SHELL_MENU_FINAL_20260720_df6fdb0e_PASS.txt`
- `/home/v6/bpi-deploy-backups/20260720-044859-shell-menu-df6fdb0e`
- `/home/v6/adp-deploy-backups/20260720-073058-source-sequence-v22`

本地浏览器报告包括 `/tmp/bpi-target-browser-smoke.json`、`/tmp/bpi-joint-browser-publish.json`、
`/tmp/bpi-joint-browser-confirm.json`、`/tmp/bpi-joint-browser-read-after-cleanup.json`、
`/tmp/bpi-feature-flags-browser-set.json`、`/tmp/bpi-feature-flags-browser-inherit.json`、
`/tmp/bpi-feature-flags-browser-post-relabel.json` 和
`/tmp/bpi-feature-flags-browser-post-relabel-mobile.json`。当前菜单截图已经固化为
`metadata/bpi-shell-menu-gate-final.png`、`metadata/bpi-shell-menu-gate-feature-flag.png` 和
`metadata/bpi-shell-menu-gate-mobile.png`。联合验收细节见
[BPI 浏览器、Kafka/Flink 与 PostgreSQL 联合验收](bpi-browser-kafka-postgres-joint-acceptance.md)，
点位目录自动同步见 [BPI 点位目录自动同步验收](bpi-point-catalog-kafka-sync-acceptance.md)，
产品化配置验收见 [BPI 目标环境拓扑与规则产品化验收](bpi-target-topology-rule-acceptance.md)，
应用组件回退见 [BPI 应用组件回滚验收](bpi-application-rollback-acceptance.md)，运行开关见
[BPI 运行开关治理目标验收](bpi-feature-flag-governance-acceptance.md)，原生菜单见
[BPI 旧 MES 原生菜单开关目标验收](bpi-shell-menu-gate-acceptance.md)。这些报告不包含密码、token、
cookie 值或数据库连接密钥。旧 portal `userPortal 401` 作为独立既有问题保留，不包含在 BPI 动作阶段
零错误结论中。
