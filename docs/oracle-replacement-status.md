# Oracle 替换状态总账

本文件由 `scripts/generate-oracle-replacement-status.py` 生成，用于把 Oracle 退场相关证据聚合到一个可审计状态页。

## 摘要

- CI 阻断问题：`0`。
- 迁移缺口：`1`。
- 关注项：`2`。
- 计划项：`1`。
- 已提升源码模块：`1`。
- Oracle backlog 引用：`729`。
- 直接 Oracle 依赖：`2`。
- PostgreSQL migration 脚本：`69`。
- PostgreSQL mapper audit：`0` error / `3` warning。
- 机器可读清单：`metadata/oracle-replacement-status.json`。

## 状态矩阵

| ID | Status | Blocking | Evidence | Next Action |
| --- | --- | --- | --- | --- |
| runtime-default-postgresql | pass | no | content inventory default=postgresql, compose postgres default=True, env example postgres=True | 保持 `.env.example` 和 Compose 默认值指向 PostgreSQL。 |
| oracle-legacy-only | watch | no | Oracle migration backlog has 729 tracked references. | 逐模块清理 backlog；删除引用前必须保留 PostgreSQL 替代证据。 |
| backend-direct-oracle-deps | gap | no | 250 recovered modules, 2 direct Oracle dependencies, 4 JDBC dependencies. | 模块提升时优先处理直接 Oracle JDBC 依赖，默认路径只保留 PostgreSQL。 |
| mapper-postgres-audit | pass | no | errors=0, warnings=3, findings=3 | 任何 error 级方言必须先迁移；warning 级 `to_char` 保留人工确认记录。 |
| postgres-migration-governance | pass | no | 69 scripts, range=001-069, highRisk=0, watch=44 | 新增 SQL 只能追加编号并保持幂等；watch 语句在 PR 中解释。 |
| recovered-source-inventory | pass | no | 250 source jars, 991 frontend files, 53 compose services. | 新增包、服务或 source map 后运行 `make inventory`。 |
| source-module-promotion | watch | no | `backend/source-modules` currently declares 1 buildable modules. | 按 auth/rbac/organization/configuration/workflow 顺序提升高频维护模块。 |
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
| documentation-or-workflow | 120 |
| frontend-row-index-noise | 4 |
| legacy-ojdbc-dependency | 6 |
| legacy-oracle-sql-resource | 160 |
| postgres-compat-reference | 1 |
| postgres-conversion-tooling | 26 |
| recovered-source-backlog | 266 |
| runtime-config-backlog | 2 |
| runtime-patch-backlog | 9 |
| tooling-or-audit-code | 113 |

## 直接 Oracle 依赖模块

| Module | Oracle deps | Path |
| --- | --- | --- |
| com.supcon.supfusion.flow:flow-common | 1 | backend/modules/com/supcon/supfusion/flow/flow-common/1.0.0-RELEASE/META-INF/maven/com.supcon.supfusion.flow/flow-common/pom.xml |
| com.supcon.supfusion:auth-upgrade | 1 | backend/modules/com/supcon/supfusion/auth-upgrade/1.0.1.RELEASE/META-INF/maven/com.supcon.supfusion/auth-upgrade/pom.xml |

## PostgreSQL Mapper Audit Top Files

| File | Findings |
| --- | --- |
| 014-ui-smoke-runtime-fixups.sql | 1 |
| 016-notification-printer-runtime-fixups.sql | 1 |
| 021-notification-postgres-idempotent-fixups.sql | 1 |

## 使用规则

- 这个总账不是替代各专项报告，而是把专项报告的当前结论串起来。
- `blocking=yes` 的项目会导致 `make oracle-replacement-check` 失败或应阻断合并。
- `gap` 表示长期目标尚未完成，但不一定阻断当前仓库治理提交。
- Oracle 退场前必须同时满足依赖、配置、SQL、migration、smoke 证据。
