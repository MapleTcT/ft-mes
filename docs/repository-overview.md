# 仓库概述

## 背景

上层 ADP 目录是一个 Windows 版本的 ADP/MES 运行交付包，包含前端静态资源、Spring Boot 服务 JAR、Nacos 配置、Nginx 配置、Windows 服务脚本、内置 JDK、Redis、Kafka、Zookeeper、Keycloak 等组件。

本仓库把其中可维护的源码和配置参考抽离出来，形成前后端分离的源码仓库。

当前仓库已经增加可持续开发入口：根 `pom.xml` 作为 Maven 父级，`backend/source-modules/` 用于承接后续可编译后端模块，`Makefile` 收敛验证、部署和 smoke 命令，`.github/` 提供 CI、PR 模板和专项 issue 模板。

## 前端边界

前端位于 `frontend/apps/`，按静态资源目录中的应用拆分。源码来自 source map，因此当前更适合作为阅读、重构和二次工程化的起点，而不是直接构建产物。

主要前端应用包括：

- `auth`
- `bap`
- `greenDill`
- `notification`
- `organization`
- `print`
- `supplant`
- `systemcode`
- `systemconfig`
- `taskscheduler`
- `theme`

原 Windows 包的 Nginx 监听 `8080`，静态目录是 `bap-server/bap-workspace/bap-static`，并把 `/api`、`/inter-api`、`/open-api`、`/msService` 代理到后端网关 `127.0.0.1:8008`。

## 后端边界

后端位于 `backend/`，分为源码模块和运行服务两层。

- `backend/modules/`：业务源码模块，来自 Maven `sources.jar`。
- `backend/decompiled-services/`：运行服务 JAR 中的启动类和少量壳代码反编译结果。
- `backend/services/`：运行服务清单和服务级配置参考。
- `backend/source-modules/`：后续新开发或已提升模块的可编译源码区。

运行服务以 Spring Boot 微服务为主，核心基础设施依赖包括：

- Nacos `8848`
- Gateway `8008`
- Redis `6379`
- Zookeeper `2181`
- Kafka `9092`
- WebSocket `30135`
- Keycloak
- MinIO
- 系统数据库，原运行包配置偏 Oracle；当前 Linux 测试编排已经转为 PostgreSQL-first，Oracle 作为 legacy profile 保留。

## 代码来源优先级

1. 真实 `*-sources.jar`：可信度最高，保留在 `backend/modules/`。
2. source map `sourcesContent`：用于前端恢复，保留在 `frontend/apps/`。
3. CFR 反编译结果：用于补齐 sources 缺失的服务启动类，保留在 `backend/decompiled-services/`。
4. 原始配置文本：脱敏后保留在 `deploy/` 和 `backend/services/*/runtime-config/`。

## 还原缺口

- 原厂聚合工程和模块构建顺序仍缺失；本仓库父级 `pom.xml` 是后续可持续开发入口，不等同于原厂完整 reactor。
- 缺少前端原始构建工程的完整 npm/yarn lock、webpack 配置和私有依赖声明。
- 反编译代码不保证和原始源码完全一致，注释、局部变量名和部分语法结构可能已变化。
- Linux 生产化部署还需要确认授权许可、Keycloak realm、MinIO 数据目录、生产端口策略和模块级数据库迁移脚本。

可持续开发和数据库迁移见：

- [项目目标和交付路线](project-objectives.md)
- [可持续开发仓库说明](sustainable-development.md)
- [后端模块依赖地图](backend-module-dependency-map.md)
- [Oracle 到 PostgreSQL 替换路线](oracle-to-postgres-transition.md)
- [后端落表业务排查交接](backend-table-audit-handoff.md)
