# FT MES 与智能批次工艺数据中心

这是一个从 Windows ADP/MES 交付资产恢复、面向 Linux/Docker 和 PostgreSQL 持续演进的工程仓库，同时包含新建的智能批次与工艺数据中心（BPI）。仓库的目标不是让旧运行包“勉强启动”，而是逐步形成可编译、可测试、可部署、可落库验收、可回滚的 MES 产品代码基线。

> **当前总状态：`IN_PROGRESS_NOT_COMPLETE`。** 仓库工程化和 BPI 受控 Phase 1 联合链路已经通过目标环境真实运行验收；`MapleTcT/iot` 的 JetLinks EventBus exporter 已在目标机用受控 marker 跑通 Kafka 到 Flink source，MES production context 也已完成真实 WOM 页面、PostgreSQL outbox、Kafka/Flink join、BPI 页面确认和影子批次落库闭环。两份证据仍是独立链路：真实网关/协议设备点位尚未接入，同一 marker 的 IoT + MES context + candidate/batch 联合链、连续影子运行和生产迁移条件尚未完成。局部测试通过不能解释为“系统已可投产”。

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
| BPI 产品链 | `PARTIAL` | 契约、服务、操作台、真实 PostgreSQL、Kafka/Flink、`MapleTcT/iot@357a285b` 受控 EventBus source、真实 WOM production context 和影子批次确认均有可复验证据 | 真实设备点位的 IoT/MES 同 marker 候选/批次、连续影子运行、END 边界和 QCS/WMS 写回仍未完成 |
| 目标测试环境 | `PASS_PHASE1_MES_CONTEXT` | BPI 页面与真实 ADP 会话桥接、三 broker Kafka、Flink/MinIO、真实 WOM outbox/context join、受控遥测候选落库及独立 JetLinks EventBus source marker 均已实测 | 该状态仍不是现场或生产 READY |
| 生产迁移 | `BLOCKED` | 迁移、回滚和签字门禁已经建立 | 数据、MinIO、Keycloak、TLS、安全、license、回滚演练和业务签字均需 READY |

权威状态以 [项目总目标验收总账](docs/project-goal-acceptance.md)、[目标缺口总账](docs/goal-gap-register.md) 和 [机器可读目标账本](metadata/project-goal-acceptance.json) 为准。README 是接手入口，不替代验收证据。

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
        批次边界候选 + 数据质量 + 应用回执
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
浏览器发布规则 -> PostgreSQL outbox -> Kafka -> Flink checkpoint
-> application receipt -> PostgreSQL APPLIED/audit -> IoT/MES context
-> candidate -> 浏览器确认 -> batch/evidence/audit
```

这条链已使用同一个唯一 `ADP_E2E_*` marker 在目标环境以受控 fixture 闭合。`MapleTcT/iot@357a285b` 已在目标 JetLinks EventBus 注入受控 marker，并通过 exporter、Kafka offset 和 Flink source lag 形成独立证据；当前仓库也已补 WOM PostgreSQL 同事务触发捕获、显式 scope/state 映射和 Java 8 Kafka 发布器。两条 source 边尚未用真实设备点位和同一生产上下文 marker 汇合为 candidate/batch，任何后续改动仍必须重复全链验证，分段测试、接口 `200` 或页面可见都不能替代完整闭环。

## 已实现的 BPI 能力

- Java 8 旧平台认证适配器与 Java 17 BPI 服务边界。
- OpenAPI、Protobuf 事件契约、兼容性基线和契约门禁。
- `MapleTcT/iot@357a285b` 已实现 JetLinks 解码后属性事件的显式设备/测点映射、稳定身份、来源序列、Redis 周期、持久化磁盘缓冲、Kafka 幂等发送、Micrometer 指标和失败关闭；14 个 Java 测试、7 个部署脚本测试及 40 模块 standalone 打包通过。目标机受控 marker `ADP_BPI_E2E_20260714_145738_757314` 已通过 JetLinks EventBus、exporter、Kafka partition 4 offset `3 -> 4` 和 Flink source lag `0`，测试入口随后恢复为关闭。
- MES production context outbox 已实现 `176` 同事务捕获和 `177` 版本时钟下限、显式产线/状态映射、`BLOCKED_*` 失败关闭、Java 8 `SKIP LOCKED` 抢占、Kafka 幂等发送、重试/毒消息终止和 Micrometer 指标；目标机已通过真实 WOM `start/hold`、3 条 `SENT|1` 上下文、Flink join 和影子批次确认。
- PostgreSQL Flyway schema、遥测入库、规则/拓扑、回放模拟、候选确认、影子批次、证据和审计。
- 规则发布 transactional outbox、Kafka 投递状态、失败重试、乐观并发和规则应用回执。
- Flink 事件时间、生产上下文 join、规则生命周期、索引路由、边界计算和三个事务 sink。
- BPI 操作台、确定性模拟服务和浏览器 E2E。
- Kafka + PostgreSQL 回执消费验收：`read_committed`、回滚不可见、重启重放、精确幂等、终态防回退和 DLQ。
- Kafka 4.2 + Flink 2.2.1 MiniCluster 验收：成功 checkpoint 后回执可见、未完成事务不可见、TaskManager 重启恢复规则终态、同版本规则禁止重新启用。
- 目标测试环境独立 BPI 运行栈：真实 ADP `suposTicket` 经可信网关校验，Java 8 适配器签发短期内部 JWT，Java 17 服务读取独立 PostgreSQL。
- 目标测试环境独立流处理栈：三 broker Kafka、八个 BPI topic、Flink 2.2.1、两个 TaskManager、MinIO checkpoint、唯一 marker 回放和带负载 TaskManager 重启恢复。
- 目标环境受控联合验收：真实浏览器模拟/发布规则，PostgreSQL outbox 投递，Flink 应用回执 `APPLIED`，上下文/遥测产生唯一候选，真实浏览器确认后形成影子批次、边界证据、状态事件和审计；验收后发布 typed inactive 规则、定向清理 marker，并恢复消费者默认关闭。
- 非 HTTPS 测试入口写命令兼容：浏览器不支持 `crypto.randomUUID()` 时改用 `crypto.getRandomValues()` 生成 UUID v4，并有 E2E 覆盖。

本地 MiniCluster、目标流处理集群、目标浏览器联合写链、JetLinks EventBus source marker 和 MES context outbox PostgreSQL 验收是相互独立的证据。目标环境 EventBus source 与 WOM context 两端均已真实执行，但尚未接入真实网关/协议设备点位，也未用同一 marker 汇合到 candidate/batch；不能把两个分段 PASS 升级为现场闭环。详细 marker、offset、目标表和清理结果分别记录在 MES 与 IoT 验收报告中。

## 目标测试环境（2026-07-14）

目标机使用 Tailscale 私网地址 `100.99.133.43`。既有 ADP/MES Compose 保持原样，BPI 使用两个独立 Compose project，避免覆盖旧服务：

| 入口/运行面 | 地址或项目 | 当前结果 |
|---|---|---|
| 既有 ADP/MES | `http://100.99.133.43:18080` | 登录和会话来源；未因 BPI 部署被替换 |
| BPI 操作台 | `http://100.99.133.43:18091` | 真实浏览器加载、概览 API `200`、无 console/page/network error |
| BPI Java/PostgreSQL | `ft-mes-bpi-runtime` | Web、adapter、service、PostgreSQL 全部 healthy；Flyway V8、19 张 BPI 表 |
| Kafka/Flink/MinIO | `ft-mes-bpi-streaming` | 3 broker、8 topic、Flink job `RUNNING`、30/30 task、持续成功 checkpoint |
| 固定 marker 回放 | `ADP_E2E_20260714_071034_1503790` | 只产生 1 个候选，数据质量错误 0 |
| TaskManager 恢复 | 带负载重启 1 个 TaskManager | 30/30 task 恢复，重启后继续完成 checkpoint |
| 浏览器/Kafka/Flink/PostgreSQL 联合写链 | `ADP_E2E_20260714_091536_BPI_JOINT` | 规则发布与应用、唯一候选、影子批次、2 条证据、状态事件和审计全部 PASS |
| 真实 WOM production context 联合链 | WOM `ADP_E2E_20260714_203900_WOM_CTX_REVFIX`；replay `ADP_E2E_20260714_204100_MESCTX_REAL` | WOM 页面 `start/hold`、outbox `SENT`、Kafka context offset、Flink 唯一候选、浏览器确认和 PostgreSQL 影子批次全部 PASS；遥测为受控回放 |
| JetLinks EventBus source 链 | `ADP_BPI_E2E_20260714_145738_757314` | EventBus subscriber、exporter received/enqueued/published、Kafka partition 4 offset `3 -> 4`、Flink consumer offset `4/4` 和 lag `0` 全部 PASS；不声明 candidate/batch |
| 验收清理 | typed inactive + 定向 SQL + consumer deny-all | Flink 确认 inactive；marker topology/rule/candidate/batch 均为 0；读路径复验 PASS |

访问 BPI 前需要先在同一浏览器完成 ADP 登录，BPI 不保存或复制旧平台密码。适配器接受真实旧平台不透明会话票据，也保留严格 issuer/audience 校验的 JWT 路径；角色和租户/工厂/产线范围均由服务端映射，未配置映射时默认拒绝。

详细证据和结论边界见 [BPI 目标环境部署验收](docs/testing/bpi-test-environment-deployment-readiness.md)、[浏览器/Kafka/Flink/PostgreSQL 联合验收](docs/testing/bpi-browser-kafka-postgres-joint-acceptance.md)、[真实 WOM production context 验收](docs/testing/bpi-mes-production-context-runtime-acceptance.md)、[IoT EventBus source 验收](https://github.com/MapleTcT/iot/blob/357a285b6d269a63f21b017c18ebc802e583315a/docs/testing/bpi-shadow-pilot-eventbus-acceptance.md) 及 MES context [机器可读报告](metadata/bpi-mes-production-context-runtime-acceptance.json)。BPI 产品总目标仍为 `PARTIAL`，因为真实设备点位的 IoT/MES context 同 marker 候选/批次链、连续影子运行和生产写回尚未完成。

## 第一次接手

### 工具链

| 区域 | 基线 |
|---|---|
| 既有 ADP/MES reactor | Java 8、Maven 3.6+ |
| BPI service / Flink | Java 17、Maven 3.6.3+（推荐 Maven 3.9.x） |
| BPI 操作台 | Node.js、npm、Vite |
| 部署和真实落库验收 | Docker Compose、PostgreSQL、Python 3 |

不要用 Java 8 运行 BPI reactor，也不要为了兼容旧 JAR 把 Java 17 模块降级或并入旧服务进程。两侧只通过版本化 HTTP 或事件契约交互。

执行 `make` 前先确认实际命中的工具链，避免 shell 中的旧 Maven 覆盖已安装版本：

```bash
java -version
mvn -version
command -v java
command -v mvn
```

`mvn -version` 必须同时显示 Maven `3.6.3+` 和 Java `17`。本仓库建议固定 Maven `3.9.x`；如果 `make bpi-stream-test` 报插件要求 Maven `3.6.3`，说明当前 `PATH` 仍命中旧 Maven，不是业务代码编译失败。

### 最短可信验证

从仓库根目录执行：

```bash
make help
make verify
make ci
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

Java 服务和 Web 默认分别只监听 `127.0.0.1:19091`、`127.0.0.1:18090`，测试机可显式把 Web 改为 Tailscale 地址。Kafka 候选、规则发布和规则应用消费者默认全部关闭，必须先设置租户/工厂/产线 allowlist 才能打开。详见 [BPI 运行栈部署说明](deploy/bpi-runtime/README.md)。

不要提交 `.env`、真实密码、token、证书私钥、数据库 dump、运行日志或现场数据。

## 验收证据

| 验收面 | 当前证据 | 结论边界 |
|---|---|---|
| BPI 浏览器状态交互 | [BPI UI 验收](metadata/bpi-ui-acceptance.json) | 确定性模拟浏览器，不代表真实 Java/Kafka/Flink |
| 回执 PostgreSQL 状态迁移 | [回执落库验收](metadata/bpi-rule-application-receipt-acceptance.json) | 真实 PostgreSQL，不含真实 broker |
| Kafka 消费重启、幂等与 DLQ | [Kafka/PostgreSQL 联合验收](metadata/bpi-rule-application-kafka-postgres-acceptance.json) | 本地 Embedded Kafka + PostgreSQL，不含 Flink job |
| Flink checkpoint、事务可见性与恢复 | [Flink/Kafka 验收](metadata/bpi-rule-application-flink-kafka-acceptance.json) | 真实 Flink MiniCluster + Kafka 4.2，本地文件 checkpoint，不含 PostgreSQL/MinIO |
| 目标环境运行与分段链路 | [目标环境验收](metadata/bpi-test-environment-acceptance.json) | 浏览器只读链、Kafka/Flink 数据面和恢复测试通过 |
| 目标环境受控联合写链 | [浏览器/Kafka/Flink/PostgreSQL 联合验收](metadata/bpi-browser-kafka-postgres-joint-acceptance.json) | 同一 marker 受控 Phase 1 链通过，不含真实 IoT/MES 上下文 |
| IoT exporter 工程与受控 source 链 | [MapleTcT/iot@357a285b](https://github.com/MapleTcT/iot/commit/357a285b6d269a63f21b017c18ebc802e583315a) | 14 个 Java 测试、7 个部署脚本测试和 40 模块打包通过；目标 JetLinks EventBus marker 已到 Kafka/Flink source，测试入口恢复关闭；不含真实网关/协议设备点位和 candidate/batch |
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
- 为产品补齐可审计的拓扑/规则创建或导入入口，避免依赖手工数据库 fixture 作为日常配置方式。
- 完成 Kafka broker 故障、savepoint 升级和整套 BPI 回滚演练；当前只完成带负载 TaskManager 重启恢复。
- [MapleTcT/iot](https://github.com/MapleTcT/iot) exporter 的真实网关/协议设备点位、单位、质量码、sequence 和 locality group 映射。
- 把 `MapleTcT/iot@357a285b` exporter 与 WOM context 配到同一试点 scope，以真实设备事件替换受控 EventBus marker，并用同一 marker 闭合 candidate/batch 后连续运行 7-14 天影子批次。
- 选定产线 7-14 天影子运行、人工边界认同率和累计量偏差验收。
- QCS/WMS 幂等写回、异常补偿、谱系、完工入库闭环和后续训练数据产品。
- 既有 MES 生产、质量、仓储主链剩余页面/API/落库阻断项。

生产迁移在数据库、回滚、license、MinIO、Keycloak、Nacos/runtime、TLS、安全和业务签字证据全部完成前，必须保持 `NOT_READY_FOR_PRODUCTION_MIGRATION`。
