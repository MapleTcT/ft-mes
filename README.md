# FT MES 与智能批次工艺数据中心

这是一个从 Windows ADP/MES 交付资产恢复、面向 Linux/Docker 和 PostgreSQL 持续演进的工程仓库，同时包含新建的智能批次与工艺数据中心（BPI）。仓库的目标不是让旧运行包“勉强启动”，而是逐步形成可编译、可测试、可部署、可落库验收、可回滚的 MES 产品代码基线。

> **当前总状态：`IN_PROGRESS_NOT_COMPLETE`。** 仓库工程化和 BPI 受控 Phase 1 联合链路已经通过目标环境真实运行验收；当前目标数据库仍是 Flyway V12，JetLinks 点位目录已通过 Kafka 自动同步到 PostgreSQL，并由真实页面读取同一内容 revision。来源序列现已成为 READY 和拓扑发布的硬门槛，目标环境页面/API/PostgreSQL 已复验当前点位保持 0 READY。本地仓库进一步完成 Flyway V13，把控制面 `APPLIED/REJECTED` 与运行时 `READY/DEGRADED/INACTIVE` 拆为独立回执，并通过 Flink checkpoint、真实 Kafka 消费、PostgreSQL 落库和 8 条浏览器 E2E；V13 尚未部署目标环境，不能替代 savepoint/历史规则迁移和目标 marker 验收。试点设备仍未注册/激活，产品 metadata、标定和来源序列未就绪，因此本轮结论仍是“同步控制链 PASS、数据源 BLOCKED”，不能发布为批次规则点位。IoT 遥测和 MES production context 的分段链已有证据，但同一真实 marker 的 IoT + MES context + candidate/batch 联合链、连续影子运行和生产迁移条件尚未完成。PATROL 共享巡检已部署到目标 PostgreSQL/EamMs，输入标准、路线/区域/巡检项、计划/任务、正常/异常结果、异常生成待治理隐患及 EAM 台账复显均已通过真实页面/API/PostgreSQL marker；完整 SESH 整改/复查/销项和统计仍未闭合。四个 EMS 源码包已恢复，但被缺失的 Indicator `6.0.4.0` 和 PostgreSQL 迁移阻断。局部测试通过不能解释为“系统已可投产”。

## 项目定位

仓库承载两条边界明确、逐步集成的产品线：

| 产品线 | 目标 | 当前边界 |
|---|---|---|
| ADP/MES 恢复与 PostgreSQL 迁移 | 把恢复前端、后端、配置和运行资产提升为可维护工程，逐步闭合生产、质量、仓储等业务 | 恢复资产不等于原厂完整源码；每项功能必须重新做页面、API 和落库验收 |
| 智能批次与工艺数据中心（BPI） | 连接数采、生产上下文、工艺规则、质量和物料系统，自动识别批次边界并形成可追溯生产事实 | Phase 1 只生成影子批次，不直接改写 WOM、QCS 或 WMS 生产状态 |

默认数据库是 **PostgreSQL**。Oracle 只允许存在于显式 `legacy-template-only` 模板、迁移对照和 backlog 中，不允许重新成为默认运行路径。

## 当前完成度

| 范围 | 状态 | 已有证据 | 继续完成的门槛 |
|---|---|---|---|
| 可持续开发仓库 | `READY` | 根父 POM、源码模块边界、CI、Compose、依赖/文件库存和 PostgreSQL-first 门禁 | 新模块持续补测试、迁移和库存 |
| 既有 ADP/MES 平台 | `PARTIAL` | 登录、组织、权限、菜单及部分生产/质量功能有真实页面和 PostgreSQL marker 证据 | 生产矩阵仍有阻断项，业务链尚未全部闭合 |
| PATROL 共享巡检 | `TARGET_HIDDEN_DANGER_PASS_PARTIAL` | 455 个 Java 文件构建 PASS；目标 37 表、24 菜单、102 操作、2 工作流验收 PASS；EamMs JAR SHA `af01d6a7...97f753`；异常隐患 marker `ADP_E2E_20260717003024_PATROL_HIDDEN_DANGER` 为 45/45 PASS，明细关联、幂等和 EAM 来源“巡检”复显均有证据 | 继续统计监控；完整隐患治理需真实 SESH；目标回滚需维护窗口确认 |
| EMS 能源管理 | `BLOCKED_MISSING_INDICATOR` | `supEMS`、`energyPlan`、`EnergyConBase`、`EnergyPred` 四个源码包和依赖关系已恢复 | 取得 Indicator `6.0.4.0` api/core，补 PostgreSQL 迁移，逐服务构建与验收 |
| BPI 产品链 | `PARTIAL` | 契约、目标环境 Flyway V12、点位准入硬门禁、`MapleTcT/iot@41239b4e` 自动目录、强制来源序列与 `30m` 运行时证据门禁、MES Kafka 消费落库、可审计拓扑/规则产品化、本地 V13 独立运行时回执、真实 PostgreSQL、Kafka/Flink、WOM production context 和影子批次确认均有可复验证据 | V13 尚未部署目标；当前试点点位仍 BLOCKED；真实设备点位的连续单调序列、IoT/MES 同 marker 候选/批次、连续影子运行、END 边界和 QCS/WMS 写回仍未完成 |
| 目标测试环境 | `PASS_PHASE1_POINT_CATALOG_SYNC` | BPI 页面与真实 ADP 会话桥接、三 broker/十 topic Kafka、Flink/MinIO、JetLinks 自动点位目录、真实 WOM outbox/context join、受控遥测候选落库均已实测 | 该状态仍不是现场或生产 READY |
| 生产迁移 | `BLOCKED` | 迁移、回滚和签字门禁已经建立 | 数据、MinIO、Keycloak、TLS、安全、license、回滚演练和业务签字均需 READY |

权威状态以 [项目总目标验收总账](docs/project-goal-acceptance.md)、[目标缺口总账](docs/goal-gap-register.md)、[模块包缺口审计](docs/module-package-gap-audit.md)、[PATROL 恢复验收](docs/testing/patrol-module-recovery-acceptance.md) 和 [机器可读目标账本](metadata/project-goal-acceptance.json) 为准。当前模块包审计确认 PATROL 已从“部署中”进入“异常发现到 EAM 待治理台账 PASS、统计和完整 SESH 治理继续验收”；四个 EMS 源码包已恢复，但 `Indicator 6.0.4.0`、`packConfigManag`、`SESGISConfig` 仍是依赖缺口；WMS 与 ProcessAnalysis 已由可维护自研模块接续。README 是接手入口，不替代验收证据。

## 当前开发主线

当前优先建设 BPI Phase 0/1，并为后续 MES 核心闭环提供可信批次事实：

```text
JetLinks/IoT 测点 + MES 生产指令/生产上下文
                    |
                    v
            Kafka 版本化事件契约
                    |
                    v
       Flink 事件时间 + 规则 Broadcast State
                    |
                    v
       批次边界候选 + 数据质量 + 双运行回执
                    |
                    v
      BPI PostgreSQL 候选/批次/证据/审计
                    |
                    v
             人工确认与影子批次
                    |
                    v
       后续 QCS/WMS 幂等联动与批次谱系
```

MES 目标业务链保持为：

```text
制造指令 -> 投料/报工 -> 请检 -> 合格/不合格处置 -> 完工入库 -> 批次追溯
```

BPI Phase 1 只有在选定产线连续运行 7-14 天，并通过边界人工认同率、累计量偏差和数据质量门槛后，才允许进入 QCS/WMS 生产写回阶段。

当前最短交付主线只有一条：

```text
浏览器发布规则 -> 当前点位目录重验 -> PostgreSQL outbox -> Kafka -> Flink checkpoint
-> 独立 application receipt + runtime readiness receipt -> PostgreSQL APPLIED/READY/audit -> IoT/MES context
-> candidate -> 浏览器确认 -> batch/evidence/audit
```

这条链已使用同一个唯一 `ADP_E2E_*` marker 在目标环境以受控 fixture 闭合。`MapleTcT/iot@41239b4e` 保留已验收的 JetLinks EventBus、exporter、Kafka offset 和 Flink source 证据，并从 JetLinks 权威注册/metadata 生成内容寻址点位快照，经 Kafka 自动落入 BPI PostgreSQL；当前版本进一步保证只有强制 DEVICE/GATEWAY 来源序列，且最近匹配配置的遥测先进入持久化 spool、Redis 证据未过期时才可进入 READY。当前仓库也已补 WOM PostgreSQL 同事务触发捕获、显式 scope/state 映射和 Java 8 Kafka 发布器。两条实时 source 边尚未用真实设备点位和同一生产上下文 marker 汇合为 candidate/batch，任何后续改动仍必须重复全链验证，分段测试、接口 `200` 或页面可见都不能替代完整闭环。

## 已实现的 BPI 能力

- Java 8 旧平台认证适配器与 Java 17 BPI 服务边界。
- OpenAPI、Protobuf 事件契约、兼容性基线和契约门禁。
- `MapleTcT/iot@41239b4e` 已实现 JetLinks 解码后遥测 exporter 和权威点位目录 publisher：显式设备/测点映射、稳定身份、来源序列、Redis 周期、持久化磁盘缓冲、Kafka 幂等发送、内容寻址目录 revision、Micrometer 指标和失败关闭；只有 `requireSourceSequence=true`、成对 header、来源为 DEVICE/GATEWAY，并且最近真实格式遥测已进入 spool、`30m` Redis 配置指纹证据未过期时才发布来源序列 READY。26 个 exporter Java 测试、7 个部署脚本测试、19 模块 reactor 测试及 40 模块完整打包通过。目标机受控遥测 marker `ADP_BPI_E2E_20260714_145738_757314` 已通过 EventBus、Kafka 和 Flink source，测试入口随后恢复为关闭；2026-07-15 新镜像又完成 JetLinks 单容器部署、真实页面/API/PostgreSQL、严重日志、容器隔离和旧镜像回滚往返，当前无真实来源流量，证据键为 0、点位保持 0 READY。
- MES production context outbox 已实现 `176` 同事务捕获和 `177` 版本时钟下限、显式产线/状态映射、`BLOCKED_*` 失败关闭、Java 8 `SKIP LOCKED` 抢占、Kafka 幂等发送、重试/毒消息终止和 Micrometer 指标；目标机已通过真实 WOM `start/hold`、3 条 `SENT|1` 上下文、Flink join 和影子批次确认。
- PostgreSQL Flyway schema、遥测入库、规则/拓扑、回放模拟、候选确认、影子批次、证据和审计。
- 拓扑/规则产品化：页面可新建或复制版本，拓扑发布前校验路径、环、JetLinks 产品/设备/属性、单位、校准和必需信号；独立管理员发布后版本不可变，规则草稿只能引用已发布拓扑及其绑定信号。Flyway V1-V9、真实 PostgreSQL marker 和 7 条浏览器 E2E 已通过；目标环境 marker `ADP_E2E_20260715_004849_BPI_PRODUCT_TARGET` 又验证了真实 ADP 会话、V9 落库、创建人发布拒绝、独立发布和服务重启后读取。
- 点位目录准入：Flyway V10-V12 保存不可变来源快照和源属性/规范属性身份；拓扑校验固定快照 ID/checksum，设备注册/激活、属性、单位、标定或设备/网关级来源序列不满足时失败关闭，发布时再次原子检查快照仍为当前版本。Exporter 自增序列只允许影子观测，不能把点位提升为 `READY`。手工 marker `ADP_E2E_20260715_POINTCAT_02` 已验证页面写链；自动链又以 revision `sha256:2a218d...151ce5` 通过 JetLinks、Kafka、MES 消费、PostgreSQL 幂等/DLT/审计、重启和真实页面读取。marker `ADP_E2E_20260715_0532_BPI_SOURCE_SEQUENCE` 进一步通过本地 PostgreSQL 16.13、8 条浏览器 E2E、目标 PostgreSQL 15.18 和真实 ADP 页面证明来源序列硬门槛已生效；当前试点点位保持 `BLOCKED`。
- 规则运行时目录准入：规则发布 Protobuf 固化 `productId` 和 `calibrationVersion`，Java 服务在发布事务内重验当前目录；Flink 订阅 `iot.point-catalog.snapshot.v1`，仅把全部绑定 READY 的规则 UPSERT 到 evaluator。目录降级会 DELETE 规则并清空待决窗口，旧 timer 不再产候选，恢复后从新观测重新累积。控制面 `APPLIED/REJECTED` 与运行时 `READY/DEGRADED/INACTIVE` 已形成独立 Protobuf、Flink sink、Kafka source/DLQ、Flyway V13 字段和 UI 状态列；目标 V13 部署、savepoint 迁移和新版本规则复发尚未执行。
- 规则发布 transactional outbox、Kafka 投递状态、失败重试、乐观并发、规则应用回执和独立运行时就绪回执。
- Flink 事件时间、生产上下文 join、规则生命周期、索引路由、边界计算和三个事务 sink。
- BPI 操作台、确定性模拟服务和 8 条浏览器 E2E；页面保持 `APPLIED` 时可独立显示 `DEGRADED -> READY`。
- Kafka + PostgreSQL 回执消费验收：`read_committed`、回滚不可见、重启重放、`DEGRADED -> READY` 落库、旧事件抑制、精确幂等和双 source DLQ。
- Kafka 4.2 + Flink 2.2.1 MiniCluster 验收：成功 checkpoint 后 `APPLIED + READY` 可见、未完成事务不可见、停用提交 `APPLIED + INACTIVE`、TaskManager 重启恢复规则终态、同版本规则禁止重新启用且两类回执无重复。
- 目标测试环境独立 BPI 运行栈：真实 ADP `suposTicket` 经可信网关校验，Java 8 适配器签发短期内部 JWT，Java 17 服务读取独立 PostgreSQL。
- 目标测试环境独立流处理栈：三 broker Kafka、十个 BPI topic、Flink 2.2.1、两个 TaskManager、MinIO checkpoint、唯一 marker 回放和带负载 TaskManager 重启恢复。
- 目标环境受控联合验收：真实浏览器模拟/发布规则，PostgreSQL outbox 投递，Flink 应用回执 `APPLIED`，上下文/遥测产生唯一候选，真实浏览器确认后形成影子批次、边界证据、状态事件和审计；验收后发布 typed inactive 规则、定向清理 marker，并恢复消费者默认关闭。
- 非 HTTPS 测试入口写命令兼容：浏览器不支持 `crypto.randomUUID()` 时改用 `crypto.getRandomValues()` 生成 UUID v4，并有 E2E 覆盖。

本地 MiniCluster、目标流处理集群、目标浏览器联合写链、JetLinks EventBus source marker 和 MES context outbox PostgreSQL 验收是相互独立的证据。目标环境 EventBus source 与 WOM context 两端均已真实执行，但尚未接入真实网关/协议设备点位，也未用同一 marker 汇合到 candidate/batch；不能把两个分段 PASS 升级为现场闭环。详细 marker、offset、目标表和清理结果分别记录在 MES 与 IoT 验收报告中。

## 目标测试环境（更新至 2026-07-17）

当前 ADP/PATROL 运维与验收入口为公司内网 `10.11.100.17`。下表中的 BPI 证据保留其 2026-07-15 验收时使用的 Tailscale 地址 `100.99.133.43`，本轮没有重新执行 BPI 联合链，不能把 PATROL 的新验收时间套用到旧 BPI 证据。既有 ADP/MES Compose 保持原样，BPI 使用两个独立 Compose project，避免覆盖旧服务：

| 入口/运行面 | 地址或项目 | 当前结果 |
|---|---|---|
| 既有 ADP/MES + PATROL | `http://10.11.100.17:18080` | 当前公司内网入口；PATROL 配置、任务执行、异常结果、待治理隐患生成和 EAM 台账复显链 PASS |
| BPI 操作台（历史验收入口） | `http://100.99.133.43:18091` | 2026-07-15 真实浏览器证据；本轮未复跑 |
| BPI Java/PostgreSQL | `ft-mes-bpi-runtime` | Web、adapter、service、PostgreSQL 全部 healthy；Flyway V12、21 张 BPI 表 |
| Kafka/Flink/MinIO | `ft-mes-bpi-streaming` | 3 broker、10 topic、Flink job `RUNNING`、30/30 task、持续成功 checkpoint |
| 固定 marker 回放 | `ADP_E2E_20260714_071034_1503790` | 只产生 1 个候选，数据质量错误 0 |
| TaskManager 恢复 | 带负载重启 1 个 TaskManager | 30/30 task 恢复，重启后继续完成 checkpoint |
| 浏览器/Kafka/Flink/PostgreSQL 联合写链 | `ADP_E2E_20260714_091536_BPI_JOINT` | 规则发布与应用、唯一候选、影子批次、2 条证据、状态事件和审计全部 PASS |
| 真实 WOM production context 联合链 | WOM `ADP_E2E_20260714_203900_WOM_CTX_REVFIX`；replay `ADP_E2E_20260714_204100_MESCTX_REAL` | WOM 页面 `start/hold`、outbox `SENT`、Kafka context offset、Flink 唯一候选、浏览器确认和 PostgreSQL 影子批次全部 PASS；遥测为受控回放 |
| JetLinks EventBus source 链 | `ADP_BPI_E2E_20260714_145738_757314` | EventBus subscriber、exporter received/enqueued/published、Kafka partition 4 offset `3 -> 4`、Flink consumer offset `4/4` 和 lag `0` 全部 PASS；不声明 candidate/batch |
| 拓扑/规则产品化链 | `ADP_E2E_20260715_004849_BPI_PRODUCT_TARGET` | 真实页面拓扑创建/校验、创建人发布拒绝、独立管理员发布、规则草稿、PostgreSQL 审计/幂等及服务重启后读取全部 PASS |
| 点位目录准入硬门禁 | `ADP_E2E_20260715_POINTCAT_02` | 真实页面导入和幂等重放 PASS；拓扑被四项硬错误阻断且重启后仍可读；试点设备状态保持 BLOCKED |
| JetLinks 点位目录自动同步 | `sha256:2a218d12...151ce5` | `iot.point-catalog.snapshot.v1` -> BPI service -> PostgreSQL -> `/#/points` PASS；重复不增行、毒消息进 DLT；1 点/0 READY |
| 验收清理 | typed inactive + 定向 SQL + consumer deny-all | Flink 确认 inactive；marker topology/rule/candidate/batch 均为 0；读路径复验 PASS |

访问 BPI 前需要先在同一浏览器完成 ADP 登录，BPI 不保存或复制旧平台密码。适配器接受真实旧平台不透明会话票据，也保留严格 issuer/audience 校验的 JWT 路径；角色和租户/工厂/产线范围均由服务端映射，未配置映射时默认拒绝。

详细证据和结论边界见 [规则运行时就绪回执验收](docs/testing/bpi-rule-runtime-readiness-acceptance.md)、[点位目录自动同步验收](docs/testing/bpi-point-catalog-kafka-sync-acceptance.md)、[BPI 点位目录准入验收](docs/testing/bpi-point-catalog-readiness-acceptance.md)、[BPI 目标环境部署验收](docs/testing/bpi-test-environment-deployment-readiness.md)、[目标环境拓扑/规则产品化验收](docs/testing/bpi-target-topology-rule-acceptance.md)、[浏览器/Kafka/Flink/PostgreSQL 联合验收](docs/testing/bpi-browser-kafka-postgres-joint-acceptance.md)、[真实 WOM production context 验收](docs/testing/bpi-mes-production-context-runtime-acceptance.md)、[IoT EventBus source 验收](https://github.com/MapleTcT/iot/blob/41239b4e4f2fdb431f1ec3765e7321b5fdfd0f19/docs/testing/bpi-shadow-pilot-eventbus-acceptance.md)、[IoT 来源序列准入验收](https://github.com/MapleTcT/iot/blob/41239b4e4f2fdb431f1ec3765e7321b5fdfd0f19/docs/testing/bpi-source-sequence-qualification-acceptance.md)、[IoT 运行时序列证据验收](https://github.com/MapleTcT/iot/blob/41239b4e4f2fdb431f1ec3765e7321b5fdfd0f19/docs/testing/bpi-runtime-source-sequence-evidence-acceptance.md) 及 [自动同步机器记录](metadata/bpi-point-catalog-kafka-sync-acceptance.json)。BPI 产品总目标仍为 `PARTIAL`，因为目标 V13 尚未部署、当前试点点位未就绪，真实设备点位的连续单调来源序列、IoT/MES context 同 marker 候选/批次链、连续影子运行和生产写回尚未完成。

## 第一次接手

### 工具链

| 区域 | 基线 |
|---|---|
| 既有 ADP/MES reactor | Java 8、Maven 3.6+ |
| BPI service / Flink | Java 17、Maven 3.6.3+（推荐 Maven 3.9.x） |
| BPI 操作台 | Node.js、npm、Vite |
| 部署和真实落库验收 | Docker Compose、PostgreSQL、Python 3 |

不要用 Java 8 运行 BPI reactor，也不要为了兼容旧 JAR 把 Java 17 模块降级或并入旧服务进程。两侧只通过版本化 HTTP 或事件契约交互。

执行相应门禁前先确认实际命中的工具链，避免 shell 中的旧 Maven 覆盖已安装版本：

```bash
java -version
mvn -version
command -v java
command -v mvn
```

`make ci` 使用 Java 8 主仓库基线；`make ci-java17` 必须由 Maven `3.6.3+` 和 Java `17` 执行。本仓库建议固定 Maven `3.9.x`；如果 BPI 门禁报插件要求 Maven `3.6.3` 或 JDK `17`，说明当前 `PATH` / `JAVA_HOME` 仍命中旧工具链，不是业务代码编译失败。

### 最短可信验证

从仓库根目录执行：

```bash
make help
make verify
make ci

JAVA_HOME=$(/usr/libexec/java_home -v 17) \
make ci-java17
```

本地验证真实 Flink checkpoint 与 Kafka 事务边界：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
make bpi-rule-application-flink-acceptance
```

该命令默认启动测试进程内的一次性 Kafka 4.2 KRaft server 和 Flink MiniCluster，不需要 Docker 或 PostgreSQL。Linux 请把 `JAVA_HOME` 换成实际 JDK 17 路径；也可用 `BPI_TEST_KAFKA_BOOTSTRAP_SERVERS` 指向专用外部 Kafka。报告默认写入 `/tmp/bpi-rule-application-flink-kafka-acceptance.json`。

常用 BPI 验证入口：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
  mvn -f services/bpi-service/pom.xml -pl app -am test

JAVA_HOME=$(/usr/libexec/java_home -v 17) \
  mvn -f streaming/pom.xml -pl bpi-stream-engine -am test

make bpi-api-contract-check
make bpi-simulation-test
make bpi-ui-build
```

目标环境受控联合验收需要先准备唯一 marker 的 topology/rule fixture，并仅对一个
tenant/plant/line 打开消费者白名单。执行顺序为：

```bash
# 先在真实 BPI 页面完成规则模拟和发布，再运行流处理回放
make bpi-stream-joint-replay

# 浏览器确认候选并查库后，先从 Flink Broadcast State 移除该规则
make bpi-stream-rule-deactivate
```

最后必须运行 `deploy/bpi-runtime/sql/joint-acceptance-cleanup.sql` 定向清理 marker，
恢复 runtime consumer 默认关闭，并重新执行浏览器只读 smoke。不要在生产环境直接
使用验收 fixture；完整操作和证据要求见联合验收报告。

### Kafka + PostgreSQL 回执验收

使用独立测试库并显式提供测试凭据：

```bash
export BPI_TEST_DATABASE_URL='jdbc:postgresql://localhost:5432/bpi_acceptance'
export BPI_TEST_DATABASE_USER='bpi_acceptance'
export BPI_TEST_DATABASE_PASSWORD='本地私密值'

JAVA_HOME=$(/usr/libexec/java_home -v 17) \
mvn -f acceptance/bpi-runtime/pom.xml -pl :bpi-service -am \
  -Dtest=BpiRuleApplicationKafkaPostgresAcceptanceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

这个测试证明 Kafka 消费端与 PostgreSQL 的事务、幂等和 DLQ 边界；它与 Flink MiniCluster 测试是两份互补证据，不能拼接成未经执行的“浏览器到数据库全链路通过”。

## 本地启动

### 既有 ADP/MES 测试栈

```bash
cd deploy/docker
cp .env.example .env
python3 scripts/render-nacos-configs.py
docker compose --env-file .env up -d
```

### BPI Kafka/Flink 测试栈

先构建 shaded job，再准备本地私有配置：

```bash
make bpi-stream-package
cp deploy/bpi-streaming/.env.example deploy/bpi-streaming/.env
make bpi-stream-deploy-preflight
make up-bpi-stream
make bpi-stream-cluster-smoke
```

需要停止时执行 `make down-bpi-stream`；该命令保留 Kafka 和 MinIO named volumes，便于做重启与 checkpoint 恢复验证。详细变量、容量预检和 marker 回放见 [BPI 流处理部署说明](deploy/bpi-streaming/README.md)。

### BPI Java/PostgreSQL 运行栈

Java 17 服务与其 PostgreSQL 数据库使用独立 Compose 项目，不覆盖既有 ADP/MES 编排：

```bash
make bpi-service-package
make bpi-adapter-package
make bpi-ui-build
cp deploy/bpi-runtime/.env.example deploy/bpi-runtime/.env
# 修改所有 change-me 值和私网 Kafka 地址
sh deploy/bpi-runtime/scripts/preflight.sh deploy/bpi-runtime/.env
docker compose --env-file deploy/bpi-runtime/.env \
  -f deploy/bpi-runtime/docker-compose.yml up -d --build
sh deploy/bpi-runtime/scripts/smoke.sh deploy/bpi-runtime/.env
```

Java 服务和 Web 默认分别只监听 `127.0.0.1:19091`、`127.0.0.1:18090`，测试机可显式把 Web 改为 Tailscale 地址。Kafka 候选、点位目录、规则发布和规则应用消费者默认全部关闭，必须先设置租户/工厂/产线 allowlist 才能打开。详见 [BPI 运行栈部署说明](deploy/bpi-runtime/README.md)。

不要提交 `.env`、真实密码、token、证书私钥、数据库 dump、运行日志或现场数据。

## 验收证据

| 验收面 | 当前证据 | 结论边界 |
|---|---|---|
| 拓扑/规则产品化 | [本地产品化验收](metadata/bpi-topology-rule-productization-acceptance.json)、[目标环境产品化验收](metadata/bpi-target-topology-rule-acceptance.json) | Flyway V1-V9、本地 PostgreSQL/浏览器和目标环境真实 ADP 页面/API/PostgreSQL/重启读取均通过；真实点位和产品级回退仍未覆盖 |
| 点位目录准入与拓扑门禁 | [目标环境验收](metadata/bpi-point-catalog-readiness-acceptance.json) | Flyway V10-V12、真实页面/API/PostgreSQL/幂等/重启读取通过；控制 PASS，但试点设备当前仍 BLOCKED |
| 点位目录自动同步 | [Kafka 同步验收](metadata/bpi-point-catalog-kafka-sync-acceptance.json) | JetLinks 权威目录、Protobuf、Kafka、MES 消费、PostgreSQL、DLT、重启幂等和真实浏览器读取通过；数据源仍 BLOCKED |
| 规则运行时就绪回执 | [多层验收](metadata/bpi-rule-runtime-readiness-acceptance.json) | 本地 Flink/Kafka、Kafka/PostgreSQL 和模拟浏览器三层通过；目标 V13 未部署，不冒充单一全链 marker |
| BPI 浏览器状态交互 | [BPI UI 验收](metadata/bpi-ui-acceptance.json) | 8/8 确定性模拟浏览器，不代表真实 Java/Kafka/Flink |
| 回执 PostgreSQL 状态迁移 | [回执落库验收](metadata/bpi-rule-application-receipt-acceptance.json) | 真实 PostgreSQL，不含真实 broker |
| Kafka 消费重启、幂等与 DLQ | [Kafka/PostgreSQL 联合验收](metadata/bpi-rule-application-kafka-postgres-acceptance.json) | 本地 Embedded Kafka + PostgreSQL/Flyway V13，覆盖 application/readiness 双 source，不含 Flink job |
| Flink checkpoint、事务可见性与恢复 | [Flink/Kafka 验收](metadata/bpi-rule-application-flink-kafka-acceptance.json) | 真实 Flink MiniCluster + Kafka 4.2，覆盖 application/readiness 双 sink；本地文件 checkpoint，不含 PostgreSQL/MinIO |
| 目标环境运行与分段链路 | [目标环境验收](metadata/bpi-test-environment-acceptance.json) | 浏览器只读链、Kafka/Flink 数据面和恢复测试通过 |
| 目标环境受控联合写链 | [浏览器/Kafka/Flink/PostgreSQL 联合验收](metadata/bpi-browser-kafka-postgres-joint-acceptance.json) | 同一 marker 受控 Phase 1 链通过，不含真实 IoT/MES 上下文 |
| IoT exporter 与目录 publisher | [MapleTcT/iot@41239b4e](https://github.com/MapleTcT/iot/commit/41239b4e4f2fdb431f1ec3765e7321b5fdfd0f19) | 26 个 exporter Java 测试、7 个部署脚本测试、19 模块 reactor 测试和 40 模块完整打包通过；强制来源序列与运行时 TTL 证据门禁已部署，目标遥测 marker 已到 Kafka/Flink source，自动目录已到 MES PostgreSQL；当前点位仍 0 READY，不含真实连续单调来源序列和 candidate/batch |
| MES production context 工程链 | [真实运行验收](docs/testing/bpi-mes-production-context-runtime-acceptance.md) | 目标机真实 WOM 页面、outbox、Kafka/Flink、候选确认、影子批次和收尾恢复通过 |
| 现场真实链 | [项目总目标验收总账](docs/project-goal-acceptance.md) | 真实网关/协议设备点位的 IoT/MES 同 marker 候选/批次和 7-14 天影子运行未完成，BPI 总目标保持 `PARTIAL` |

证据等级从低到高为：静态/单元测试、模拟浏览器、真实 PostgreSQL、本地 Kafka + PostgreSQL、本地 Flink + Kafka、目标集群全链路、现场影子运行。每一级只证明自己实际执行的边界，不能用两份分离测试冒充一条没有跑过的联合链路。

## 验收原则

功能完成必须形成“真实页面 -> HTTP -> 后端链路 -> PostgreSQL”证据：

- 不能只凭源码、静态检查或 `make ci` 判断功能完成；菜单可见和接口 `200` 也不能单独证明业务完成。
- 写操作使用唯一 `ADP_E2E_*` marker，并直接查询 PostgreSQL 验证新增、更新、状态变化和清理。
- 记录页面/路由、操作步骤、console error、network error、API、payload、response、Controller/Service/Mapper/SQL 和目标表。
- Kafka `PUBLISHED` 只说明 broker 已接收；Flink `APPLIED` 回执才说明规则随成功 checkpoint 进入运行状态。
- 使用 `PASS / FAIL / BLOCKED / NOT_APPLICABLE` 如实记录，禁止为了绿色报告改写结果。
- 模拟器、本地 MiniCluster 和单元测试不能冒充目标服务器或现场产线验收。

固定入口：

- [功能验收与落库验收规则](docs/functional-persistence-acceptance.md)
- [前端功能测试报告](docs/frontend-functional-test-report.md)
- [后端落库验收报告](docs/backend-table-audit/persistence-acceptance.md)
- [机器可读落库账本](metadata/persistence-acceptance.json)
- [BPI 工程测试计划](docs/testing/bpi-engineering-test-plan.md)
- [目标缺口总账](docs/goal-gap-register.md)

## 仓库结构

```text
frontend/apps/                 恢复前端与 BPI 操作台
backend/modules/               sources.jar 恢复源码，只读排查和迁移依据
backend/decompiled-services/   运行服务反编译启动壳
backend/source-modules/        可编译、可测试、可持续维护的源码模块
services/bpi-service/          Java 17 BPI PostgreSQL 服务与规则运行时
streaming/bpi-stream-engine/   Java 17 Flink 批次边界引擎
contracts/bpi-api/             BPI OpenAPI 与实施/模拟能力清单
contracts/bpi-events/          Protobuf 事件契约与兼容性基线
simulation/bpi/                无外部依赖的交互/API 模拟器
deploy/docker/                 ADP/MES PostgreSQL-first 测试编排
deploy/bpi-runtime/            BPI Java 17 服务与独立 PostgreSQL 编排
deploy/bpi-streaming/          Kafka/Flink/MinIO 流处理编排
deploy/database/               PostgreSQL 迁移与生产迁移证据工具
docs/                          目标、设计、测试、落库和交接文档
metadata/                      机器可读验收与治理账本
scripts/                       构建、恢复、审计和门禁脚本
```

原始 Windows 包仍保留在仓库上层的 `../bap-server/`、`../Commands/`、`../nginx/` 和 `../Manual/`，不进入默认源码 reactor。

## 恢复资产边界

- 从 `366` 个 source map 恢复约 `991` 个前端源码文件。
- 解包 `250` 个 ADP 相关 `sources.jar`，包含约 `4807` 个 Java 和 `398` 个 XML 文件。
- 对 `23` 个可运行服务补充反编译启动壳和服务清单。

这些数字描述“可排查资产”，不代表全部模块已经可编译或可维护。需要持续开发的模块必须逐步提升到 `backend/source-modules/`，补齐父 POM、依赖边界、测试、PostgreSQL migration 和运行验收。

## 接手顺序

1. 阅读 [项目工作指令](AGENTS.md)、[项目目标](docs/project-objectives.md) 和本 README 的状态边界。
2. 查看 [项目总目标验收总账](docs/project-goal-acceptance.md) 与 [目标缺口总账](docs/goal-gap-register.md)。
3. BPI 开发先读 [BPI 总设计](docs/designs/batch-process-intelligence.md)、[交互设计](docs/designs/bpi-interaction-design.md) 和 [API 目录](docs/api/bpi-api-catalog.md)。
4. 既有业务修复先读 [后端落表排查交接](docs/backend-table-audit-handoff.md) 和对应模块审计。
5. 新业务包先执行 `make module-intake-check INTAKE=/path/to/package-or-dir`，再决定是否进入默认源码路径。

## 当前未闭合事项

- 保持已完成的目标环境同一 marker 浏览器发布、outbox、Kafka、Flink 应用回执、PostgreSQL、候选确认、批次/证据/审计联合验收为发布回归基线。
- 保持 Flyway V12、点位目录、拓扑/规则产品化、真实 ADP 会话、PostgreSQL marker 和重启读取为每次发布回归基线；把本地已通过的 V13 独立运行时回执部署到目标环境。
- 完成 V13 目标部署、Kafka broker 故障、savepoint 升级和整套 BPI 回滚演练；当前只完成带负载 TaskManager 重启恢复。
- 在 JetLinks 注册并激活 `bpi-pilot-device-01`，补 `instantFlow` 产品 metadata、单位和标定，并用真实事件证明连续单调来源序列及重连 epoch 语义；等待自动同步生成新 revision 后重新校验拓扑，禁止手工伪造 READY 快照。
- 把 `MapleTcT/iot@41239b4e` exporter 与 WOM context 配到同一试点 scope，先用多条真实设备事件证明 `source_epoch + sequence` 连续单调和重连语义，再替换受控 EventBus marker，并用同一 marker 闭合 candidate/batch 后连续运行 7-14 天影子批次。
- 选定产线 7-14 天影子运行、人工边界认同率和累计量偏差验收。
- QCS/WMS 幂等写回、异常补偿、谱系、完工入库闭环和后续训练数据产品。
- 既有 MES 生产、质量、仓储主链剩余页面/API/落库阻断项。

生产迁移在数据库、回滚、license、MinIO、Keycloak、Nacos/runtime、TLS、安全和业务签字证据全部完成前，必须保持 `NOT_READY_FOR_PRODUCTION_MIGRATION`。
