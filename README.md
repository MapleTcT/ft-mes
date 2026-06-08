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
deploy/nginx/                  # Nginx 配置参考
deploy/windows-start-reference/# Windows 启停脚本参考
metadata/                      # 恢复统计和服务清单 JSON
scripts/                       # 可重复运行的源码恢复脚本
docs/                          # 仓库概述和迁移说明
```

## 当前恢复结果

- 前端：从 `366` 个 source map 恢复 `991` 个源码文件。
- 后端源码模块：解包 `250` 个 Supcon/ADP 相关 `sources.jar`，包含 `4807` 个 Java 文件、`398` 个 XML 文件。
- 后端服务补齐：反编译 `23` 个可运行服务的业务 class，补出 `259` 个 Java 文件。
- 可运行服务：生成 `23` 个服务清单，见 `backend/services/*/README.md`。

## 重要说明

这个仓库是“源码整理仓库”，不是已经可直接 `mvn package` 或 `npm build` 的原厂工程。前端源码来自 source map，后端主体来自真实 `sources.jar`，服务启动壳来自 CFR 反编译。后续如果要变成可持续开发仓库，需要再补父级 `pom.xml`、模块依赖聚合、前端 package lock 和 Linux 启动编排。

详细说明见：

- [仓库概述](docs/repository-overview.md)
- [后端说明](backend/README.md)
- [前端说明](frontend/README.md)
- [Docker 测试部署](deploy/docker/README.md)
- [Linux 迁移说明](docs/linux-migration-notes.md)
- [恢复流程](docs/source-recovery.md)
