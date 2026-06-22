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

当前仓库主体是 ADP/BAP 平台运行包，不是完整 MES 业务产品包。

已恢复内容主要包括：

- 平台前端 source map 源码。
- 平台后端 sources.jar 源码。
- 运行服务反编译启动壳。
- Docker/Linux 测试部署编排。
- PostgreSQL runtime 兼容 SQL 和 patch 脚本。
- 基础模块、质量/QCS、EAM、能源等部分运行包适配痕迹。

业务层面的生产、质量、设备、能源、安环等完整产品形态，需要后续按模块继续接入、落表排查和业务 smoke。其中生产模块是当前目标的一部分，需要形成完整功能测试用例，覆盖主数据、指令/工单、备料/投料、作业许可、执行记录、报工、退料/尾料、状态流转、导入导出和落库证明。

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

当前下一步不是继续补治理层，也不是只跑静态检查。必须先按 [功能验收与落库验收规则](functional-persistence-acceptance.md) 启动或访问真实系统，通过前端页面/E2E 操作验证当前功能，并对每个会改变业务数据的动作完成 PostgreSQL 落库验收。

优先顺序：

1. 真实前端测试登录、首页、菜单、列表、查询、新增、编辑、删除/禁用、详情、导入导出、上传、配置页和业务主流程。
2. 对写动作使用唯一 marker，记录 HTTP 请求、后端 Controller/Service/Mapper/DAO 链路、目标表和 PostgreSQL 查询结果。
3. 同步更新 [前端功能测试报告](frontend-functional-test-report.md)、[后端落库验收报告](backend-table-audit/persistence-acceptance.md) 和 `metadata/persistence-acceptance.json`。
4. 对启动失败、页面空白、权限不足、接口 500、SQL 异常、接口成功但未落库等问题，先作为阻断或失败项入账，再按影响面修复。

后端落表业务排查仍然是持续线程，但它必须服务于真实功能验收和落库证明，不能替代真实页面测试。
