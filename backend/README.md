# Backend

后端分为两类源码：

1. `modules/`：来自 `../bap-server/assembly/repository/maven` 的 `*-sources.jar`，这是优先可信来源。
2. `decompiled-services/`：来自 `../bap-server/base-Server/*/*.jar` 的业务 class 反编译结果，主要用于补齐启动类、网关壳代码和 websocket 类。

## 可运行服务

运行包中实际存在 `23` 个服务 JAR：

```text
auditlog
baseService
basicmanagement
configuration
customProperty
fileServer
flow
gateway
i18n
iam
license
notification-admin
notification-apiserver
notification-app
notification-engine
notification-mobile
notification-sms-jincang
operatetools
orgmanagement
printer
sysmanagement
task-scheduler
websocket
```

每个服务在 `backend/services/<service>/README.md` 中记录：

- 原始 JAR 路径
- Main-Class / Start-Class
- Spring Boot 版本
- JAR 内配置入口
- Nacos Data ID
- 解析到的端口和应用名
- 嵌入依赖数量

## 源码组织

`backend/modules/` 保持 Maven 仓库坐标风格：

```text
backend/modules/com/supcon/supfusion/<artifact>/<version>/
backend/modules/com/supcon/greendill/<...>/<artifact>/<version>/
```

`backend/decompiled-services/` 按运行服务组织：

```text
backend/decompiled-services/<service>/src/main/java/
backend/decompiled-services/<service>/src/main/resources/
```

优先阅读顺序建议：

1. 从 `backend/services/<service>/README.md` 看运行边界。
2. 到 `backend/modules/` 找业务模块源码。
3. 到 `backend/decompiled-services/<service>/` 找启动类和服务壳代码。
4. 到 `deploy/nacos-config/` 查看脱敏配置。
