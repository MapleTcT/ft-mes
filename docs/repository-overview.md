# 仓库概述

## 背景

上层 ADP 目录是一个 Windows 版本的 ADP/MES 运行交付包，包含前端静态资源、Spring Boot 服务 JAR、Nacos 配置、Nginx 配置、Windows 服务脚本、内置 JDK、Redis、Kafka、Zookeeper、Keycloak 等组件。

本仓库把其中可维护的源码和配置参考抽离出来，形成前后端分离的源码仓库。

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

运行服务以 Spring Boot 微服务为主，核心基础设施依赖包括：

- Nacos `8848`
- Gateway `8008`
- Redis `6379`
- Zookeeper `2181`
- Kafka `9092`
- WebSocket `30135`
- Keycloak
- MinIO
- 系统数据库，原配置默认偏 Oracle，也有部分模块提供 MariaDB 默认值

## 代码来源优先级

1. 真实 `*-sources.jar`：可信度最高，保留在 `backend/modules/`。
2. source map `sourcesContent`：用于前端恢复，保留在 `frontend/apps/`。
3. CFR 反编译结果：用于补齐 sources 缺失的服务启动类，保留在 `backend/decompiled-services/`。
4. 原始配置文本：脱敏后保留在 `deploy/` 和 `backend/services/*/runtime-config/`。

## 还原缺口

- 缺少原厂聚合工程的父级 Maven `pom.xml` 和模块构建顺序。
- 缺少前端原始构建工程的完整 npm/yarn lock、webpack 配置和私有依赖声明。
- 反编译代码不保证和原始源码完全一致，注释、局部变量名和部分语法结构可能已变化。
- Linux 部署还需要确认数据库类型、授权许可、Keycloak realm、MinIO 数据目录和生产端口策略。
