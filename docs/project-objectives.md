# 项目目标和交付路线

## 总目标

把当前 Windows 交付包恢复出的 ADP/MES 资产，整理成可以长期维护、持续开发、持续验证的代码仓库，并把默认数据库路线从 Oracle 切换到 PostgreSQL。

这个目标不是简单“能启动”。最终应达到：

- 代码仓库能承接新模块开发。
- 后端模块能逐步从恢复源码提升为可编译源码。
- Docker 测试环境默认 PostgreSQL，不依赖 Oracle 授权。
- Oracle 兼容只作为显式 legacy 路径保留，并且可以按模块退场。
- 平台功能和业务模块功能有清晰边界。
- 后续后端落表、业务表含义、页面/API 对表关系可以独立线程持续排查。

当前测试环境的验证边界见 [测试环境验证范围](runtime-validation-scope.md)。本阶段优先闭合平台登录、用户、组织、权限、菜单、待办、基础配置、Nacos、Keycloak、PostgreSQL 和 runtime patch；生产模块必须补完整功能测试用例和真实前端验收记录，不能只停留在菜单/API 可见。

功能验收和后端落库验收必须遵循 [功能验收与落库验收规则](functional-persistence-acceptance.md)。后续不能只补治理层、只跑静态检查或只看代码推断功能可用；涉及写业务数据的前端动作必须用唯一 marker 通过 PostgreSQL 查询证明真实落库。

总目标完成状态见 [项目总目标验收总账](project-goal-acceptance.md)。该总账把可持续开发仓库、Oracle 替换、平台验证、生产模块完整验证、PostgreSQL 缺口治理和生产迁移前置项统一到机器可读账本 `metadata/project-goal-acceptance.json`，并由 `make project-goal-acceptance-check` 校验。总账为 `IN_PROGRESS_NOT_COMPLETE` 时，不能宣称当前目标已全部完成。

当前仍未闭合的目标缺口见 [目标缺口总账](goal-gap-register.md) 和
`metadata/goal-gap-register.json`，由 `make goal-gap-register-check` 校验。后续新线程接手业务包、
后端落表或生产迁移时，应先看这份总账，避免把局部页面/API smoke 通过误判为整体完成。

## 当前项目定位

当前仓库不再只有单一的“运行包恢复”定位，而是包含两条受同一验收体系约束的产品线：

1. **既有 ADP/MES 平台恢复线**：维护登录、组织、权限、菜单、配置、生产/质量等恢复资产，持续把可维护模块提升为源码模块并迁移到 PostgreSQL。
2. **BPI 新产品线**：建设智能批次与工艺数据中心，连接 JetLinks/IoT、MES 生产上下文、Kafka/Flink、PostgreSQL、QCS 和 WMS，先完成影子批次，再进入生产闭环和训练数据产品。

既有仓库主体仍来自 ADP/BAP 平台运行包，不等于原厂完整 MES 业务产品；BPI 则是本仓库中按可编译源码、版本化契约和真实验收新建的产品模块，不能与恢复代码的完整度混为一谈。

已恢复内容主要包括：

- 平台前端 source map 源码。
- 平台后端 sources.jar 源码。
- 运行服务反编译启动壳。
- Docker/Linux 测试部署编排。
- PostgreSQL runtime 兼容 SQL 和 patch 脚本。
- 基础模块、质量/QCS、EAM、能源等部分运行包适配痕迹。

业务层面的生产、质量、设备、能源、安环等完整产品形态，需要后续按模块继续接入、落表排查和业务 smoke。其中生产模块是当前目标的一部分，需要形成完整功能测试用例，覆盖主数据、指令/工单、备料/投料、作业许可、执行记录、报工、退料/尾料、状态流转、导入导出和落库证明。

## BPI 产品目标

BPI 的产品目标不是做一个监控大屏，而是把数采信号变成可审计的生产事实：

- 用生产指令、阀门路径、设备状态、流量、液位和物料/配方切换共同判断批次边界。
- 以事件时间、checkpoint、幂等 inbox/outbox 和版本化规则保证可回放与可解释。
- 自动形成批次、工艺参数、物料/能源耗用、质量证据和谱系，为后续 QCS/WMS 联动提供权威输入。
- 保留人工确认、拒绝、修订和异常救援入口，首期只运行影子批次，不直接改写 WOM/QCS/WMS 生产状态。
- 为 Iceberg/MLflow 训练数据产品保留 point-in-time、版本、质量码、校准和标签来源，禁止用无法追溯的聚合结果训练模型。

当前 BPI 已从设计进入目标环境实施：事件/API 契约、Java 17 PostgreSQL 服务、Java 8 适配器、操作台、模拟器、遥测入库、版本化规则/拓扑、候选/影子批次、Flink 事件时间与 Broadcast State、规则发布 transactional outbox、失败重试和审计已经具备可复验证据。拓扑/规则不再依赖日常 SQL fixture：页面已支持新建或复制拓扑/规则版本，拓扑发布前校验路径、环、JetLinks 产品/设备/属性、单位、校准和必需信号，独立管理员发布后版本不可变；Flyway V1-V9、真实 PostgreSQL marker 和 7 条浏览器 E2E 已通过。目标测试环境已运行独立 Java/PostgreSQL 与 Kafka/Flink/MinIO Compose；真实 ADP 会话、三 broker Kafka、Flink job、MinIO checkpoint、固定 marker 回放和带负载 TaskManager 恢复均已通过。2026-07-14 先以 marker `ADP_E2E_20260714_091536_BPI_JOINT` 闭合真实浏览器规则模拟/发布、outbox、Kafka、Flink `APPLIED`、候选入库、浏览器确认和影子批次；随后又以真实 WOM marker `ADP_E2E_20260714_203900_WOM_CTX_REVFIX` 和 replay marker `ADP_E2E_20260714_204100_MESCTX_REAL` 闭合页面 `start/hold`、PostgreSQL context outbox、Kafka offset、Flink join、唯一候选、浏览器确认及影子批次落库，并完成 typed inactive、定向清理和消费者默认关闭恢复。该验收同时修复了合成上下文残留与 WOM 版本域冲突，新增 `177` 版本时钟下限。`MapleTcT/iot@786d153a` 进一步补齐可观测遥测 exporter、JetLinks 权威点位目录 publisher 和受控试点编排；受控遥测 marker `ADP_BPI_E2E_20260714_145738_757314` 已证明 EventBus、Kafka 与 Flink source，19 个 exporter Java 测试和 7 个部署脚本测试通过。2026-07-15 目标环境先以 marker `ADP_E2E_20260715_004849_BPI_PRODUCT_TARGET` 完成 Flyway V9、真实页面拓扑创建/校验、职责分离发布、规则草稿、PostgreSQL 审计/幂等和重启读取，再升级到 Flyway V12，以 marker `ADP_E2E_20260715_POINTCAT_02` 完成真实页面点位快照导入和准入门禁。随后自动目录 revision `sha256:2a218d12...151ce5` 又闭合 `JetLinks -> Kafka -> BPI service -> PostgreSQL -> 浏览器`，并验证重复不增行、毒消息 DLT、重启幂等和精确 scope allowlist。marker `ADP_E2E_20260715_0532_BPI_SOURCE_SEQUENCE` 进一步在 PostgreSQL 16.13 和 `8/8` 浏览器 E2E 中证明设备/网关来源序列是硬准入条件，Exporter 回退序列不能把点位提升为 READY。目标试点产品 metadata 为空，设备仍为 `notActive` 且未注册，标定和来源序列未验证，因此同步控制链结论为 PASS、数据源结论保持 BLOCKED。实时 IoT 遥测仍未与 WOM context 用同一真实 marker 形成 candidate/batch。因此 BPI 总目标保持 `PARTIAL`：先修复试点设备/属性/单位/标定/序列准入，等待自动同步生成新 revision，再以同 scope 的真实设备事件和 MES context 形成同 marker 候选/批次、连续运行 7-14 天，最后进入 QCS/WMS 写回。

本批 marker `ADP_E2E_20260715_0532_BPI_SOURCE_SEQUENCE` 已继续部署到目标环境：真实 ADP 页面
`http://100.99.133.43:18091/#/points` 的目录 GET 为 `200`，PostgreSQL 15.18 同一自动 revision 为
1 点/0 READY 且 `source_sequence_enabled=false`，页面显示五项阻断并保持 `BLOCKED`，浏览器错误为 0。
该结果只证明准入门禁在目标环境生效，不改变现场数据源尚未就绪和 G-021 保持 `PARTIAL` 的结论。

权威设计和验收入口：

- [BPI 总设计](designs/batch-process-intelligence.md)
- [BPI 交互设计](designs/bpi-interaction-design.md)
- [BPI API 目录](api/bpi-api-catalog.md)
- [BPI 工程测试计划](testing/bpi-engineering-test-plan.md)
- `metadata/project-goal-acceptance.json` 中的 `G-021`

## 非目标

当前阶段不做这些事：

- 不把 `backend/modules/**/META-INF/maven/**/pom.xml` 全量纳入 Maven reactor。
- 不提交 Windows 二进制运行包、fat jar、exe、dll、内置 JDK。
- 不把 Oracle 当默认数据库继续适配。
- 不在没有业务说明和落表证据时声称完整 MES 业务闭环已完成。
- 不通过重置数据库来掩盖 PostgreSQL 兼容缺口。
- 不把生产迁移前置项混入当前测试环境闭环；数据迁移、回滚、license、MinIO、Keycloak 生产库、TLS、安全加固和业务签字需要单独补。

## 工作流

### 1. 仓库工程化

目标：让仓库具备持续开发入口。

已具备：

- 根 `pom.xml` 父级 POM。
- `backend/source-modules/` 可编译源码承接区。
- `deploy/docker/` PostgreSQL-first Compose 编排。
- `Makefile` 统一验证、部署、smoke 命令。
- GitHub Actions 验证 Maven reactor 和 Compose 语法。
- `scripts/verify-sustainable-repo.py` 验证仓库治理硬约束。
- `scripts/create-backend-source-module.py` 创建标准后端源码模块。
- `scripts/precheck-module-intake.py` 对新业务包或恢复模块做只读准入预检。
- `metadata/module-intake-latest-basic-modules.json` / `docs/module-intake-latest-basic-modules.md` 记录真实 `MES包/最新基础模块` 的 report-only 准入结果、扫描覆盖和阻断项。
- `scripts/verify-source-modules.py` 校验已提升后端源码模块，并阻止默认源码路径重新带入 Oracle 驱动、配置和 mapper 资源。
- `scripts/generate-current-content-inventory.py` 生成当前迁移内容库存。
- `scripts/generate-backend-dependency-inventory.py` 生成恢复后端模块依赖库存。
- `scripts/generate-oracle-migration-audit.py` 生成 Oracle 迁移 backlog。
- `scripts/generate-postgres-migration-inventory.py` 生成 PostgreSQL 初始化脚本索引和 watch 语句说明。
- `scripts/generate-oracle-replacement-status.py` 生成 Oracle 替换状态总账。
- `deploy/docker/scripts/adp-platform-validation-smoke.js` 汇总平台 API、菜单和待办 smoke，输出统一平台验证报告。

后续增强：

- 每提升一个后端模块，就给模块补单元测试或最小集成测试。
- 每新增一个业务运行包，先跑 `make module-intake-check INTAKE=/path/to/package-or-dir`，再补对应 smoke 脚本或测试清单。
- 已存在的 `MES包/最新基础模块` 先按准入账本处理 7z 不可检查包和 `DataSet_6.1.2.2` 中的 `jdbc:oracle:thin` 阻断项，再进入默认源码区。
- 每次测试环境修复后，优先跑 `make smoke-platform` 形成平台验证报告，再进入业务模块 smoke。

### 2. Oracle 替换

目标：逐步把运行环境、Mapper、SQL、配置从 Oracle 默认切换到 PostgreSQL。

原则：

- PostgreSQL 是默认路径。
- Oracle 只允许显式 legacy profile 或 `.env.oracle-legacy.example`。
- 父 POM 默认 `dependencyManagement` 不管理 Oracle JDBC，Oracle 驱动只能在 `oracle-legacy` profile 下出现。
- 方言差异集中到 DAO/Mapper/migration 层。
- 可编译源码模块的默认 `src/main` 不允许带入 Oracle JDBC URL、driver、Hibernate dialect 或 `mapper/oracle` 资源。
- 每个模块必须有审计结果、迁移脚本和 smoke 证据。
- 每个 PostgreSQL 缺表、缺列、类型不兼容或 Oracle 方言残留，都落到幂等 SQL 或模块 backlog，不用清库重建掩盖；`make postgres-migration-check` 会阻断清库类高风险语句，并要求 watch 语句是 `DROP ... IF EXISTS` 或 `DELETE ... WHERE`。

### 3. 后端落表业务排查

目标：形成“页面/API/服务/Mapper/表/字段/业务含义”的映射。

这块建议交给专门线程推进，输入和产出见 [后端落表业务排查交接](backend-table-audit-handoff.md)。

### 4. 产品说明书

目标：基于真实运行模块和落表证据，逐步补产品说明。

说明书应区分：

- 平台能力：组织、权限、菜单、流程、实体配置、通知、打印、任务调度等。
- 业务模块：生产、设备、质量、能源、安环、仓储、追溯等。
- 已验证功能、仅菜单可见功能、缺数据/缺脚本功能、未接入功能。
- 生产模块的完整功能测试用例、业务前置数据、页面/API/表证据和未通过项 backlog。

### 5. 功能验收和落库证明

目标：把“页面能不能用”和“业务动作有没有真实写库”拆成可复验的证据。

固定产出：

- [前端功能测试报告](frontend-functional-test-report.md)
- [后端落库验收报告](backend-table-audit/persistence-acceptance.md)
- [机器可读落库验收记录](../metadata/persistence-acceptance.json)
- [生产模块 Backlog 账本](production-module-backlog.md)
- [机器可读生产模块 Backlog](../metadata/production-module-backlog.json)
- [业务模块接入验收要求](business-module-intake-requirements.md)
- [机器可读业务模块接入要求](../metadata/business-module-intake-requirements.json)
- [生产迁移就绪账本](production-migration-readiness.md)
- [机器可读生产迁移就绪记录](../metadata/production-migration-readiness.json)
- [生产切换总闸门](production-cutover-gate.md)
- [机器可读生产切换总闸门](../metadata/production-cutover-gate.json)

原则：

- 必须用真实前端页面或等效 E2E 操作，不用源码静态阅读代替功能测试。
- 必须记录 console error、network error、API、payload、response 和页面实际结果。
- 对新增、编辑、删除、禁用、启用、状态变更等动作，必须追踪 Controller/Service/Mapper/SQL/目标表，并查询 PostgreSQL。
- `PASS` 只代表真实前端操作、后端链路和 PostgreSQL 证据都齐全；未执行、被阻断或不落库功能必须分别标记 `BLOCKED` 或 `NOT_APPLICABLE`。

## 验收口径

仓库级验收：

- `make verify` 通过。
- `make ci` 通过。
- `make sustainable-check` 通过。
- `make project-goal-acceptance-check` 通过。
- `make goal-gap-register-check` 通过。
- `make source-module-check` 通过。
- `make source-module-test` 通过。
- `make runtime-script-check` 通过。
- `make persistence-acceptance-check` 通过。
- `make production-testcase-check` 通过。
- `make production-blocker-check` 通过。
- `make production-module-backlog-check` 通过。
- `make business-module-intake-requirements-check` 通过。
- `make production-action-map-check` 通过。
- `make production-migration-readiness-check` 通过。
- `make module-intake-check` 对新接入业务包或模块通过，或已有 report-only 证据和 backlog。
- `make module-intake-precheck-regression-check` 通过，证明准入预检能拦住 Oracle POM 和 `mapper/oracle` 默认路径污染。
- `make module-intake-candidate-report-check` 通过，证明真实候选包准入阻断项和扫描覆盖已进入仓库账本。
- `make inventory-check` 通过。
- `make backend-dependency-check` 通过。
- `make oracle-audit-check` 通过。
- `make postgres-migration-check` 通过。
- `make oracle-replacement-check` 通过。
- GitHub Actions `Verify` 通过。
- 新模块能继承父 POM 并纳入 `backend/source-modules`。
- Docker Compose 默认渲染为 PostgreSQL。
- 父 POM 默认依赖管理不暴露 Oracle JDBC。
- Oracle 配置只出现在 legacy 文档、模板、默认源码路径之外的对照资料或待迁移清单中。

数据库迁移验收：

- 每个模块有 PostgreSQL 迁移脚本或明确无表变更说明。
- 迁移脚本幂等。
- SQL audit 阻断项有处理记录。
- 对应页面/API smoke 通过。
- 平台修复后的 `make smoke-platform` 报告通过，或失败项已进入幂等 SQL/backlog。

业务说明验收：

- 每个业务模块有菜单/API/表/字段/流程说明。
- 生产模块有完整功能测试用例矩阵，并按真实前端操作逐项验收。
- 生产模块未闭合项必须进入 Backlog；接口 `200` 但不落库的动作不得混入已完成项。
- 每条核心业务链路有输入、操作、状态变化和数据库结果。
- 每个会改变业务数据的前端动作都有 PostgreSQL 落库验收记录。
- 未验证或缺包的部分明确标注，不混进已完成功能。

生产迁移验收：

- [生产迁移就绪账本](production-migration-readiness.md) 覆盖 PostgreSQL 数据迁移脚本、回滚方案、license 策略、MinIO 文件迁移、Keycloak 生产库策略、Nacos/runtime patch 生产化、端口/域名/TLS、安全加固和业务 smoke 签字。
- `metadata/production-migration-readiness.json` 通过 `make production-migration-readiness-check`。
- 整体状态保持 `NOT_READY_FOR_PRODUCTION_MIGRATION`，直到所有轨道都有脚本、演练或签字证据。
- 生产迁移脚本、回滚脚本和配置模板不得提交真实密码、token、证书私钥。

## 推荐优先级

1. 基础平台：登录、用户、组织、权限、菜单、系统配置、流程、待办。
2. 低代码实体配置：`ec_module`、`ec_entity`、`ec_model`、`ec_field`、`ec_view` 等元数据。
3. 业务基础模块：BaseSet、TeamInfo、Qualify、TagManagement、HierarchicalMod。
4. 质量域：LIMS、QCS、LIMSDC、LIMSInterface、LIMSMaterial、LIMSSTDS。
5. 生产模块完整功能测试用例和主流程验收。
6. 设备/能源/安环等业务运行包。

## 当前下一步

当前只保留两条有明确完成条件的执行主线：

### 主线 A：BPI Phase 0/1

1. 保持已通过的同一 marker `UI -> Outbox -> Kafka -> Flink -> application receipt -> PostgreSQL -> candidate confirm -> batch/evidence/audit` 联合验收作为每次发布的回归基线。
2. 保持已通过的目标环境 Flyway V12、点位目录自动同步、真实 ADP 会话、页面拓扑创建/校验、独立发布、规则绑定、PostgreSQL revision 和重启读取作为发布回归；继续补版本比较、审批和产品级回退，日常配置不得回退到 SQL fixture 或手工伪造 READY 快照。
3. 完成 broker 故障、savepoint 升级和 BPI 整体回滚演练；当前目标环境已完成带负载 TaskManager 重启恢复和单 marker 清理恢复。
4. 以 `MapleTcT/iot@786d153a` 和已实现的 `mes-production-context-outbox` 为基线配置试点产线；当前 `bpi-pilot-device-01` 已自动进入点位目录，但必须先完成 JetLinks 注册/激活、`instantFlow` metadata、单位、标定和 source sequence 治理，等待新 revision 自动同步后重新校验拓扑。
5. MES 上下文真实链和 IoT source 分段链均已通过；点位准入变为 READY 后，用真实设备事件替换受控 EventBus marker，并与 WOM context 使用同一 marker 完成 Kafka、Flink、BPI PostgreSQL candidate/batch 和浏览器证据链，再连续运行 7-14 天影子批次。
6. 达到边界人工认同率、累计量偏差和数据质量门槛后，才进入 QCS/WMS 写回。

### 主线 B：既有生产/质量核心链

1. 继续按 [功能验收与落库验收规则](functional-persistence-acceptance.md) 对真实页面执行唯一 marker 测试。
2. 闭合制造指令、投料/报工、请检、合格/不合格处置、完工入库和批次追溯，而不是扩散到低优先级模块。
3. 同步更新前端报告、后端落库报告和机器可读账本；接口 `200` 但未落库必须记为 `FAIL`。

后端落表业务排查和仓库治理继续作为两条主线的支撑，不再替代产品功能开发、真实运行和落库证明。
