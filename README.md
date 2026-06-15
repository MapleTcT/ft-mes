# ADP MES Recovered Source Repository

本仓库是从上层 Windows 运行包整理出来的源码仓库，目标是把 ADP/MES 的可读源码、运行服务清单、前后端边界和 Linux 迁移依据集中管理。

原始包仍保留在上层目录：

- `../bap-server/`
- `../Commands/`
- `../nginx/`
- `../Manual/`

本仓库不保存 Windows 二进制运行时、原始 fat jar、exe、dll、内置 JDK、Redis/Nginx 可执行文件等产物。

## 仓库结构

```text
frontend/apps/                 # 从 source map 恢复的前端源码
backend/modules/               # 从 Maven *-sources.jar 解出的后端源码模块
backend/decompiled-services/   # 从可运行服务 JAR 反编译补齐的启动/壳代码
backend/services/              # 23 个可运行服务的清单和服务级配置参考
deploy/nacos-config/           # 脱敏后的 Nacos 配置参考
deploy/docker/                 # Linux Docker Compose 测试部署编排
deploy/database/               # 数据库迁移和 Oracle/PostgreSQL 兼容策略
deploy/nginx/                  # Nginx 配置参考
deploy/windows-start-reference/# Windows 启停脚本参考
metadata/                      # 恢复统计和服务清单 JSON
scripts/                       # 可重复运行的源码恢复脚本
docs/                          # 仓库概述和迁移说明
pom.xml                        # 可持续开发父级 Maven POM
Makefile                       # 开发、部署、smoke 验证入口
.github/                       # CI、PR 模板和专项 issue 模板
```

## 当前恢复结果

- 前端：从 `366` 个 source map 恢复 `991` 个源码文件。
- 后端源码模块：解包 `250` 个 Supcon/ADP 相关 `sources.jar`，包含 `4807` 个 Java 文件、`398` 个 XML 文件。
- 后端服务补齐：反编译 `23` 个可运行服务的业务 class，补出 `259` 个 Java 文件。
- 可运行服务：生成 `23` 个服务清单，见 `backend/services/*/README.md`。

## 重要说明

这个仓库已经具备可持续开发的基础入口：根 `pom.xml` 提供 Maven 父级和依赖管理，`backend/source-modules/` 用于承接后续提升为可编译的新模块，`Makefile` 收敛 Maven、Docker Compose 和 smoke 验证命令。

仍需注意：前端源码来自 source map，后端主体来自真实 `sources.jar`，服务启动壳来自 CFR 反编译；恢复目录本身仍不是原厂完整工程。后续应按模块逐步把高频维护代码提升到 `backend/source-modules/`，而不是直接把 `backend/modules/**/META-INF/maven/**/pom.xml` 全量纳入构建。

## 快速验证

```bash
make verify
make ci
make verify-pom
make compose-config
make sustainable-check
make persistence-acceptance-check
make source-module-check
make source-module-test
make inventory-check
make backend-dependency-check
make oracle-audit-check
make postgres-migration-check
make oracle-replacement-check
```

启动测试环境仍以 `deploy/docker/README.md` 为准：

```bash
cd deploy/docker
cp .env.example .env
python3 scripts/render-nacos-configs.py
docker compose --env-file .env up -d
```

详细说明见：

- [仓库概述](docs/repository-overview.md)
- [项目目标和交付路线](docs/project-objectives.md)
- [功能验收与落库验收规则](docs/functional-persistence-acceptance.md)
- [前端功能测试报告](docs/frontend-functional-test-report.md)
- [当前内容迁移清单](docs/current-content-inventory.md)
- [Oracle 迁移 Backlog](docs/oracle-migration-backlog.md)
- [Oracle 替换状态总账](docs/oracle-replacement-status.md)
- [PostgreSQL 迁移脚本索引](docs/postgres-migration-index.md)
- [后端模块提升指南](docs/backend-module-promotion-guide.md)
- [可持续开发仓库说明](docs/sustainable-development.md)
- [后端模块依赖地图](docs/backend-module-dependency-map.md)
- [后端恢复模块依赖库存](docs/backend-module-dependency-inventory.md)
- [Oracle 到 PostgreSQL 替换路线](docs/oracle-to-postgres-transition.md)
- [后端落表业务排查交接](docs/backend-table-audit-handoff.md)
- [后端落库验收报告](docs/backend-table-audit/persistence-acceptance.md)
- [后端说明](backend/README.md)
- [前端说明](frontend/README.md)
- [Docker 测试部署](deploy/docker/README.md)
- [Linux 迁移说明](docs/linux-migration-notes.md)
- [恢复流程](docs/source-recovery.md)
