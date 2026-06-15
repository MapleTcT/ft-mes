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

## 当前项目定位

当前仓库主体是 ADP/BAP 平台运行包，不是完整 MES 业务产品包。

已恢复内容主要包括：

- 平台前端 source map 源码。
- 平台后端 sources.jar 源码。
- 运行服务反编译启动壳。
- Docker/Linux 测试部署编排。
- PostgreSQL runtime 兼容 SQL 和 patch 脚本。
- 基础模块、质量/QCS、EAM、能源等部分运行包适配痕迹。

业务层面的生产、质量、设备、能源、安环等完整产品形态，需要后续按模块继续接入、落表排查和业务 smoke。

## 非目标

当前阶段不做这些事：

- 不把 `backend/modules/**/META-INF/maven/**/pom.xml` 全量纳入 Maven reactor。
- 不提交 Windows 二进制运行包、fat jar、exe、dll、内置 JDK。
- 不把 Oracle 当默认数据库继续适配。
- 不在没有业务说明和落表证据时声称完整 MES 业务闭环已完成。
- 不通过重置数据库来掩盖 PostgreSQL 兼容缺口。

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
- `scripts/verify-source-modules.py` 校验已提升后端源码模块，并阻止默认源码路径重新带入 Oracle 驱动、配置和 mapper 资源。
- `scripts/generate-current-content-inventory.py` 生成当前迁移内容库存。
- `scripts/generate-backend-dependency-inventory.py` 生成恢复后端模块依赖库存。
- `scripts/generate-oracle-migration-audit.py` 生成 Oracle 迁移 backlog。
- `scripts/generate-postgres-migration-inventory.py` 生成 PostgreSQL 初始化脚本索引。
- `scripts/generate-oracle-replacement-status.py` 生成 Oracle 替换状态总账。

后续增强：

- 每提升一个后端模块，就给模块补单元测试或最小集成测试。
- 每新增一个业务运行包，就补对应 smoke 脚本或测试清单。

### 2. Oracle 替换

目标：逐步把运行环境、Mapper、SQL、配置从 Oracle 默认切换到 PostgreSQL。

原则：

- PostgreSQL 是默认路径。
- Oracle 只允许显式 legacy profile 或 `.env.oracle-legacy.example`。
- 方言差异集中到 DAO/Mapper/migration 层。
- 可编译源码模块的默认 `src/main` 不允许带入 Oracle JDBC URL、driver、Hibernate dialect 或 `mapper/oracle` 资源。
- 每个模块必须有审计结果、迁移脚本和 smoke 证据。

### 3. 后端落表业务排查

目标：形成“页面/API/服务/Mapper/表/字段/业务含义”的映射。

这块建议交给专门线程推进，输入和产出见 [后端落表业务排查交接](backend-table-audit-handoff.md)。

### 4. 产品说明书

目标：基于真实运行模块和落表证据，逐步补产品说明。

说明书应区分：

- 平台能力：组织、权限、菜单、流程、实体配置、通知、打印、任务调度等。
- 业务模块：生产、设备、质量、能源、安环、仓储、追溯等。
- 已验证功能、仅菜单可见功能、缺数据/缺脚本功能、未接入功能。

## 验收口径

仓库级验收：

- `make verify` 通过。
- `make ci` 通过。
- `make sustainable-check` 通过。
- `make source-module-check` 通过。
- `make source-module-test` 通过。
- `make inventory-check` 通过。
- `make backend-dependency-check` 通过。
- `make oracle-audit-check` 通过。
- `make postgres-migration-check` 通过。
- `make oracle-replacement-check` 通过。
- GitHub Actions `Verify` 通过。
- 新模块能继承父 POM 并纳入 `backend/source-modules`。
- Docker Compose 默认渲染为 PostgreSQL。
- Oracle 配置只出现在 legacy 文档、模板、默认源码路径之外的对照资料或待迁移清单中。

数据库迁移验收：

- 每个模块有 PostgreSQL 迁移脚本或明确无表变更说明。
- 迁移脚本幂等。
- SQL audit 阻断项有处理记录。
- 对应页面/API smoke 通过。

业务说明验收：

- 每个业务模块有菜单/API/表/字段/流程说明。
- 每条核心业务链路有输入、操作、状态变化和数据库结果。
- 未验证或缺包的部分明确标注，不混进已完成功能。

## 推荐优先级

1. 基础平台：登录、用户、组织、权限、菜单、系统配置、流程、待办。
2. 低代码实体配置：`ec_module`、`ec_entity`、`ec_model`、`ec_field`、`ec_view` 等元数据。
3. 业务基础模块：BaseSet、TeamInfo、Qualify、TagManagement、HierarchicalMod。
4. 质量域：LIMS、QCS、LIMSDC、LIMSInterface、LIMSMaterial、LIMSSTDS。
5. 设备/能源/安环/生产等业务运行包。

## 当前下一步

建议下一条专门线程从“后端落表业务排查”开始，先做基础平台和实体配置的表级地图，再推进到质量和生产类业务模块。
