# FT MES 可持续开发仓库

本仓库同时承载两条明确分离、但可逐步集成的产品线：

1. **ADP/MES 既有平台恢复与 PostgreSQL 迁移**：把 Windows 交付包中可恢复的前端、后端、配置和运行资产整理为可维护、可验证、可在 Linux/Docker 上持续演进的工程。
2. **智能批次与工艺数据中心（BPI）**：连接数采、生产上下文、工艺规则、质量和物料系统，基于 Kafka/Flink 自动识别批次边界，形成可追溯、可回放、可用于后续模型训练的生产事实。

仓库默认数据库是 **PostgreSQL**。Oracle 只允许保留在显式 legacy 模板、迁移对照和 backlog 中，不能重新成为默认运行路径。

> 当前整体状态：`IN_PROGRESS_NOT_COMPLETE`。仓库工程化和多项平台能力已经通过验收，但既有 MES 全业务产品、BPI 现场影子运行和生产迁移条件均未全部完成。任何局部测试通过都不能解释为“已可直接投产”。

## 当前主线

当前开发主线是 BPI Phase 0/1，并服务于后续核心闭环：

```text
制造指令/生产上下文
        +
JetLinks/IoT 测点
        -> Kafka 版本化事件
        -> Flink 事件时间、规则 Broadcast State、批次边界候选
        -> BPI PostgreSQL 候选/批次/证据/审计
        -> 人工确认与影子批次
        -> 后续 QCS/WMS 幂等联动
```

已实现并有本地证据的 BPI 能力包括：

- Java 8 旧平台认证适配器与 Java 17 BPI 服务边界。
- Protobuf 事件契约、兼容性基线和 API 契约门禁。
- PostgreSQL Flyway schema、遥测入库、规则/拓扑、回放模拟、候选确认和影子批次。
- 规则发布 transactional outbox、Kafka 投递状态、失败重试、乐观并发和审计。
- Flink 事件时间、生产上下文 join、规则生命周期、索引路由、边界计算、checkpoint 恢复和 exactly-once sink。
- BPI 操作台、模拟服务和浏览器 E2E。
- Flink 规则应用回执契约、消费入库和操作台状态展示已完成本地 PostgreSQL/模拟浏览器验收；在真实 Kafka/Flink checkpoint/restart/DLQ 联合验收完成前仍按 `PARTIAL` 管理。

尚未完成的关键边界：

- 目标测试服务器上的 BPI 全栈部署和远端浏览器/API/PostgreSQL 验收。
- JetLinks/IoT exporter 的真实点位映射、单位/质量码/sequence 现场核对。
- MES 生产上下文 outbox 与 Flink 的真实联调。
- 选定产线连续 7-14 天影子运行及边界人工认同率验收。
- QCS/WMS 生产写回、异常补偿、谱系与完工入库闭环。
- Iceberg/MLflow 训练数据产品和建议型模型阶段。

完整设计见 [BPI 总设计](docs/designs/batch-process-intelligence.md)，当前目标状态见 [项目总目标验收总账](docs/project-goal-acceptance.md)。

## 架构与运行边界

| 区域 | 技术基线 | 责任 |
|---|---|---|
| 既有 ADP/MES | Java 8、恢复前端、Nacos、Keycloak | 旧平台兼容、菜单/组织/权限和现有业务运行包 |
| BPI API/事务 | Java 17、Spring Boot、Flyway、PostgreSQL | 规则、拓扑、候选、批次、证据、审计和幂等 |
| BPI 流处理 | Java 17、Kafka、Flink | 事件时间、上下文 join、规则状态、批次候选和回执 |
| 前端 | 恢复前端 + 独立 BPI 操作台 | 旧平台入口和 BPI 运营交互 |
| 数据库 | PostgreSQL 默认 | 新能力和迁移目标；Oracle 仅 legacy |

Java 8 运行包不直接依赖 Java 17 的 BPI domain/repository/migration。两侧只能通过版本化 HTTP 或事件契约交互，避免为了兼容旧 JAR 把新模块重新降级。

## 仓库结构

```text
frontend/apps/                 # 恢复前端与 BPI 操作台
backend/modules/               # sources.jar 恢复源码，只读排查和迁移依据
backend/decompiled-services/   # 运行服务反编译启动壳
backend/source-modules/        # 可编译、可测试、可持续维护的源码模块
services/bpi-service/          # Java 17 BPI PostgreSQL 服务与规则运行时
streaming/bpi-stream-engine/   # Java 17 Flink 批次边界引擎
contracts/bpi-api/             # BPI OpenAPI 与实施/模拟能力清单
contracts/bpi-events/          # Protobuf 事件契约与兼容性基线
simulation/bpi/                # 无外部依赖的交互/API 模拟器
deploy/docker/                 # ADP/MES PostgreSQL-first 测试编排
deploy/bpi-streaming/          # Kafka/Flink/MinIO 流处理编排
deploy/database/               # PostgreSQL 迁移与生产迁移证据工具
docs/                          # 目标、设计、测试、落库和交接文档
metadata/                      # 机器可读验收与治理账本
scripts/                       # 构建、恢复、审计和门禁脚本
```

上层原始 Windows 包仍保留在 `../bap-server/`、`../Commands/`、`../nginx/` 和 `../Manual/`。本仓库不提交 fat jar、exe、dll、内置 JDK、数据库 dump、运行日志或真实密钥。

## 快速验证

仓库级门禁：

```bash
make verify
make ci
make sustainable-check
make project-goal-acceptance-check
make goal-gap-register-check
make persistence-acceptance-check
make source-module-test
make oracle-replacement-check
```

BPI Java 17 服务与流引擎：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -f services/bpi-service/pom.xml -pl app -am test
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -f streaming/pom.xml -pl bpi-stream-engine -am test
make bpi-api-contract-check
make bpi-simulation-test
make bpi-ui-build
```

执行真实 PostgreSQL 验收时，使用独立测试库并显式提供 `BPI_TEST_DATABASE_*`；没有真实数据库查询证据时，不得把接口 `200` 记为落库通过。

## 本地启动

既有 ADP/MES 测试栈：

```bash
cd deploy/docker
cp .env.example .env
python3 scripts/render-nacos-configs.py
docker compose --env-file .env up -d
```

BPI Kafka/Flink 栈需要先构建 shaded job，并在本地私有 `.env` 中提供部署 ID、MinIO 凭据和 JAR 路径：

```bash
make bpi-stream-package
cd deploy/bpi-streaming
docker compose --env-file .env up -d
```

具体变量、容量预检和 marker 回放见 [BPI 流处理部署说明](deploy/bpi-streaming/README.md)。不要把 `.env`、token、证书私钥或现场账号提交到 Git。

## 验收规则

本项目以“真实页面 -> HTTP -> 后端链路 -> PostgreSQL”为功能完成证据：

- 不能只凭源码、静态检查或 `make ci` 判断功能完成。
- 不能只看源码、菜单可见或静态检查判断功能可用。
- 写操作必须使用唯一 marker，并直接查询 PostgreSQL 证明新增、更新、状态变化和清理。
- Kafka `PUBLISHED` 只表示 broker 投递；Flink `APPLIED` 回执才表示规则随成功 checkpoint 进入运行状态。
- 模拟器、单元测试和本地 E2E 不能冒充测试服务器或现场产线验收。
- `PASS / FAIL / BLOCKED / NOT_APPLICABLE` 必须按事实记录，不能为了绿色报告改写结果。

固定证据入口：

- [功能验收与落库验收规则](docs/functional-persistence-acceptance.md)
- [前端功能测试报告](docs/frontend-functional-test-report.md)
- [后端落库验收报告](docs/backend-table-audit/persistence-acceptance.md)
- [机器可读落库账本](metadata/persistence-acceptance.json)
- [BPI 工程测试计划](docs/testing/bpi-engineering-test-plan.md)
- [目标缺口总账](docs/goal-gap-register.md)

## 既有资产恢复范围

- 从 `366` 个 source map 恢复约 `991` 个前端源码文件。
- 解包 `250` 个 ADP 相关 `sources.jar`，包含约 `4807` 个 Java 和 `398` 个 XML 文件。
- 对 `23` 个可运行服务补充反编译启动/壳代码和服务清单。

这些资产不是原厂完整源码工程。需要维护的模块必须逐步提升到 `backend/source-modules/`，补齐父 POM、依赖边界、测试、PostgreSQL migration 和运行验收，不能把恢复目录中的嵌套 POM 全量塞回 reactor。

## 接手顺序

1. 阅读 [项目工作指令](AGENTS.md) 和 [项目目标](docs/project-objectives.md)。
2. 查看 [项目总目标验收总账](docs/project-goal-acceptance.md) 与 [目标缺口总账](docs/goal-gap-register.md)。
3. BPI 开发先读 [BPI 总设计](docs/designs/batch-process-intelligence.md)、[交互设计](docs/designs/bpi-interaction-design.md) 和 [API 目录](docs/api/bpi-api-catalog.md)。
4. 既有业务修复先读 [后端落表排查交接](docs/backend-table-audit-handoff.md) 和对应模块审计。
5. 新业务包先执行 `make module-intake-check INTAKE=/path/to/package-or-dir`，再决定是否进入默认源码路径。

生产迁移在数据库、回滚、license、MinIO、Keycloak、Nacos/runtime、TLS、安全和业务签字证据全部完成前，必须保持 `NOT_READY_FOR_PRODUCTION_MIGRATION`。
