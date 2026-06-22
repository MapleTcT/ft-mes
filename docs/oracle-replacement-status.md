# Oracle 替换状态总账

本文件由 `scripts/generate-oracle-replacement-status.py` 生成，用于把 Oracle 退场相关证据聚合到一个可审计状态页。

## 摘要

- Generated At：`2026-06-22T13:40:24+00:00`。
- Repo Commit：`fe11653c9a07232d8314597395b9e5d20505f8d2`。
- CI 阻断问题：`0`。
- 迁移缺口：`1`。
- 关注项：`2`。
- 计划项：`1`。
- 已提升源码模块：`1`。
- 源码模块 Oracle 禁入：`pass`。
- Oracle backlog 引用：`958`。
- 直接 Oracle 依赖：`2`。
- PostgreSQL migration 脚本：`171`。
- PostgreSQL mapper audit：`0` error / `0` warning。
- 运行配置 active Oracle-like 默认行：`0`。
- Oracle audit 未分类引用：`0`。
- Oracle audit commit recorded：`True`。
- 机器可读清单：`metadata/oracle-replacement-status.json`。

## 状态矩阵

| ID | Status | Blocking | Evidence | Next Action |
| --- | --- | --- | --- | --- |
| runtime-default-postgresql | pass | no | content inventory default=postgresql, compose postgres default=True, env example postgres=True | 保持 `.env.example` 和 Compose 默认值指向 PostgreSQL。 |
| parent-pom-oracle-legacy-profile | pass | no | defaultOracleDeps=0, legacyProfile=True, legacyOracleDeps=1 | Oracle JDBC 只能放在 `oracle-legacy` profile；默认父 POM 只管理 PostgreSQL/JDK 基线。 |
| runtime-config-no-oracle-defaults | pass | no | activeOracle=0, source=0, rendered=0, files=88 | Nacos source templates and rendered configs must default to PostgreSQL; Oracle-like defaults can only remain in comments, backlog, or explicit legacy templates. |
| oracle-legacy-only | watch | no | Oracle migration backlog has 958 tracked references. | 逐模块清理 backlog；删除引用前必须保留 PostgreSQL 替代证据。 |
| oracle-audit-current-and-classified | pass | no | generatedAt=2026-06-22T13:39:15+00:00, repoCommit=fe11653c9a07232d8314597395b9e5d20505f8d2, unclassified=0, findingCount=958, categoryTotal=958 | 先运行 `make oracle-audit`；新增 Oracle 引用必须分类到 backlog、legacy、tooling 或文档路径。 |
| backend-direct-oracle-deps | gap | no | 250 recovered modules, 2 direct Oracle dependencies, 4 JDBC dependencies. | 模块提升时优先处理直接 Oracle JDBC 依赖，默认路径只保留 PostgreSQL。 |
| mapper-postgres-audit | pass | no | errors=0, warnings=0, findings=0 | 任何 error 级方言必须先迁移；warning 级 `to_char` 保留人工确认记录。 |
| postgres-migration-governance | pass | no | 171 scripts, range=001-171, highRisk=0, watch=60 | 新增 SQL 只能追加编号并保持幂等；watch 语句在 PR 中解释。 |
| recovered-source-inventory | pass | no | 250 source jars, 991 frontend files, 53 compose services. | 新增包、服务或 source map 后运行 `make inventory`。 |
| source-module-promotion | watch | no | `backend/source-modules` currently declares 1 buildable modules. | 按 auth/rbac/organization/configuration/workflow 顺序提升高频维护模块。 |
| source-module-oracle-policy | pass | no | Source module verification passed. Modules: 1. | 修复 `backend/source-modules` 中的 Oracle JDBC、Oracle 默认配置、Oracle dialect 或 mapper/oracle 资源后重新运行 `make source-module-check`。 |
| backend-table-audit | planned | no | `docs/backend-table-audit-handoff.md` and issue template exist; detailed table maps remain future work. | 专门线程输出页面/API/服务/Mapper/表/字段映射，避免混进平台工程化任务。 |

## 恢复资产计数

| Area | Count |
| --- | --- |
| backendSourceJars | 250 |
| backendJavaFiles | 4807 |
| backendXmlFiles | 398 |
| decompiledServices | 23 |
| frontendSourceMaps | 366 |
| frontendRecoveredFiles | 991 |
| composeServices | 53 |
| businessRuntimeServices | 14 |

## Oracle Backlog 分类

| Category | References |
| --- | --- |
| allowed-legacy-contract | 6 |
| decompiled-runtime-backlog | 16 |
| documentation-or-workflow | 186 |
| frontend-row-index-noise | 4 |
| legacy-ojdbc-dependency | 6 |
| legacy-oracle-sql-resource | 160 |
| postgres-compat-reference | 6 |
| postgres-conversion-tooling | 35 |
| recovered-source-backlog | 266 |
| runtime-patch-backlog | 9 |
| tooling-or-audit-code | 264 |

## 直接 Oracle 依赖模块

| Module | Oracle deps | Path |
| --- | --- | --- |
| com.supcon.supfusion.flow:flow-common | 1 | backend/modules/com/supcon/supfusion/flow/flow-common/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.flow/flow-common/pom.xml |
| com.supcon.supfusion:auth-upgrade | 1 | backend/modules/com/supcon/supfusion/auth-upgrade/1.0.1.RELEASE/META-INF/maven/com.supcon.supfusion/auth-upgrade/pom.xml |

## 直接 Oracle 依赖退场动作

| Module | Dependency | Action | Verification |
| --- | --- | --- | --- |
| com.supcon.supfusion.flow:flow-common | com.oracle.jdbc:ojdbc7 | 提升为 source module 时删除直接 Oracle JDBC；common/DTO 模块默认不应打开厂商连接，需要数据库连接时由 DAO/service 层通过父 POM 管理 PostgreSQL JDBC。 | 提升模块后运行 `make source-module-check`、`make source-module-test`，并用流程/待办 smoke 覆盖调用链。 |
| com.supcon.supfusion:auth-upgrade | com.oracle.jdbc:ojdbc6 | 把 Oracle driver 从默认 POM 移到显式 legacy profile 或独立迁移工具；默认 PostgreSQL 目标库写入使用标准 `java.sql` API，避免 `oracle.sql.*`。 | 提升模块后运行 `make source-module-check`、`make source-module-test`，并用登录/currentuser 或升级任务 dry-run 验证。 |

## PostgreSQL Mapper Audit Top Files

当前没有 mapper/sql 方言发现。

## 运行配置 Oracle 默认扫描

当前 Nacos source/rendered 配置 active 行没有 Oracle-like fallback；扫描 `88` 个文件、`1620` 行 active 配置。

## 使用规则

- 这个总账不是替代各专项报告，而是把专项报告的当前结论串起来。
- `blocking=yes` 的项目会导致 `make oracle-replacement-check` 失败或应阻断合并。
- `gap` 表示长期目标尚未完成，但不一定阻断当前仓库治理提交。
- Oracle 退场前必须同时满足依赖、配置、SQL、migration、smoke 证据。
