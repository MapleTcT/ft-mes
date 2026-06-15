# 可持续开发仓库说明

## 目标

本仓库从“恢复源码仓库”升级为“可持续开发仓库”的第一步已经完成：根目录提供 Maven 父级 POM、后端/部署聚合入口、统一验证命令和 Docker 编排入口。

当前阶段不把 `backend/modules/**/META-INF/maven/**/pom.xml` 直接纳入 Maven reactor。那些 POM 来自 `sources.jar` 元数据，缺少原厂父工程、私服依赖、构建插件和模块顺序，强行纳入会得到一个表面完整但不可维护的工程。

## 开发分区

```text
pom.xml                         # 仓库父级 POM，统一 Java/依赖/插件基线
backend/pom.xml                 # 后端聚合入口
backend/source-modules/         # 新开发或已提升模块的可编译源码区
backend/modules/                # 恢复源码参考区，保持原 Maven 坐标目录
backend/decompiled-services/    # 反编译启动壳和缺失源码参考
backend/services/               # 运行服务清单
deploy/pom.xml                  # 部署资料聚合入口
deploy/docker/                  # Linux Docker Compose 测试部署
deploy/database/                # 数据库迁移和兼容策略入口
docs/runtime-validation-scope.md # 测试环境验证范围和生产迁移边界
Makefile                        # 常用开发、验证、部署命令
.github/workflows/verify.yml    # GitHub Actions 验证入口
.github/ISSUE_TEMPLATE/         # Oracle 迁移和后端落表排查模板
```

## Maven 基线

父级 POM 默认管理：

- Java 8 编译目标。
- Spring Boot `2.1.5.RELEASE`，来自恢复服务清单。
- Spring Cloud `Greenwich.SR2`。
- Spring Cloud Alibaba `2.1.1.RELEASE`。
- PostgreSQL JDBC `42.2.8`，和当前 Docker 运行补丁保持一致。
- 默认 `dependencyManagement` 不管理 Oracle JDBC；Oracle JDBC 只保留在 `oracle-legacy` profile 中，用于老库对比或显式迁移工具。
- 已提升到 `backend/source-modules` 的可编译模块不得直接声明 Oracle JDBC；默认源码路径也不能带入 Oracle 运行配置或 mapper 资源。

可用 profile：

- `postgres-first`：默认启用，表示新模块优先按 PostgreSQL 适配。
- `oracle-legacy`：仅用于保留老模块回连 Oracle 的兼容构建，默认构建不会启用。
- `business-modules`：给后续业务模块批量构建/测试预留的开关。

## 模块提升规则

后续把恢复源码或新业务包变成可编译模块时，按下面顺序推进：

1. 对来源包运行 `make module-intake-check INTAKE=/path/to/package-or-dir`。
2. 在 `backend/source-modules/<module>` 新建标准 Maven 模块。
3. 从 `backend/modules/<group>/<artifact>/<version>` 复制可信源码，不直接移动原始恢复目录。
4. 新模块继承根 `ft-mes-parent`。
5. 只声明当前模块真实需要的依赖，避免把整套运行包依赖一次性灌入。
6. DAO/Mapper 层必须同时接受 PostgreSQL smoke 或 SQL audit。
7. 业务逻辑和数据库方言分离，禁止在 service 层散落 `if oracle/postgres` 分支。
8. 提升完成后，把模块加入 `backend/source-modules/pom.xml` 的 `<modules>`。
9. Oracle 对照资料只能作为 legacy/backlog 资料保留在默认 `src/main` 外，不能进入默认构建产物。

推荐模块结构：

```text
backend/source-modules/<module>/
  pom.xml
  src/main/java/
  src/main/resources/
  src/test/java/
```

## 常用命令

```bash
make verify
make ci
make verify-pom
make compose-config
make sustainable-check
make source-module-check
make source-module-test
make create-backend-module MODULE=platform-auth
make module-intake-check INTAKE=/path/to/package-or-dir
make inventory
make inventory-check
make backend-dependency-inventory
make backend-dependency-check
make oracle-audit
make oracle-audit-check
make postgres-migration-index
make postgres-migration-check
make oracle-replacement-status
make oracle-replacement-check
make render-config
make up-infra
make up
make smoke-api
make smoke-menu
make smoke-todo
make audit-postgres-mappings
make audit-postgres-report
```

`make verify` 只验证 Maven reactor 和 Docker Compose 语法，不会启动容器，也不会修改数据库。`make ci` 会额外检查仓库治理规则、后端 source module 结构、source module 默认路径是否 Oracle-free、已提升 source module 编译测试、内容库存是否新鲜、恢复后端依赖库存是否新鲜、Oracle 迁移 backlog 是否新鲜、PostgreSQL 初始化脚本索引是否新鲜、Oracle 替换状态总账是否新鲜，以及 PostgreSQL 方言审计。

`make module-intake-check` 是新业务包和恢复模块的只读准入检查，发现默认路径 Oracle 残留会返回非 0。`make audit-postgres-mappings` 是阻断式审计，发现 Oracle/MySQL/SQL Server 方言会返回非 0。`make audit-postgres-report` 用于生成阶段性报告，不阻断当前工作。

## 后续工作

- 将高频修改的基础模块优先提升到 `backend/source-modules`。
- 提升模块前先用 `make create-backend-module MODULE=<name>` 创建标准结构，提升过程见 [后端模块提升指南](backend-module-promotion-guide.md)。
- 新业务包进入源码区前先用 `make module-intake-check INTAKE=/path/to/package-or-dir` 扫描，并建立独立模块组，不直接污染基础平台模块。
- 将 PostgreSQL 兼容补丁从 runtime patch 逐步前移到源码、Mapper 和迁移脚本。
- 给每个已提升模块补最小 smoke 或集成测试。
- 按 [后端落表业务排查交接](backend-table-audit-handoff.md) 建立模块级表字段地图。
- 新增或移除运行服务、源码包、前端应用后，运行 `make inventory` 更新 [当前内容迁移清单](current-content-inventory.md)。
- 新增、移除或修改 `backend/modules` 恢复源码 POM 后，运行 `make backend-dependency-inventory` 更新 [后端恢复模块依赖库存](backend-module-dependency-inventory.md)。
- 新增、删除或修改 Oracle/ojdbc/Oracle 方言引用后，运行 `make oracle-audit` 更新 [Oracle 迁移 Backlog](oracle-migration-backlog.md)。
- 新增、删除或修改 `deploy/docker/postgres/init/*.sql` 后，运行 `make postgres-migration-index` 更新 [PostgreSQL 迁移脚本索引](postgres-migration-index.md)。
- 更新任一 Oracle 退场相关清单后，运行 `make oracle-replacement-status` 更新 [Oracle 替换状态总账](oracle-replacement-status.md)。
