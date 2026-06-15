# 后端模块依赖地图

## 当前事实

恢复结果包含两层后端资产：

- `backend/modules/`：250 个 `sources.jar` 展开目录，是真实源码参考。
- `backend/services/`：基础平台 23 个运行服务清单。
- `deploy/docker/docker-compose.yml`：测试环境还编排了部分业务运行包服务，例如 `FoundationMs`、`EamMs`、`LIMS`、`WTSs`、`WAPS`、`RMMs`、`WOMMs` 等。

因此当前仓库不是单体 Maven 项目，而是“基础平台服务 + 可选业务运行包 + 恢复源码目录”的混合形态。

## 推荐分层

```text
gateway
  -> iam/auth/keycloak/rbac
  -> platform services
  -> business module services

platform services
  -> sysmanagement/basicmanagement/orgmanagement
  -> configuration/module-registry/system-config/systemcode
  -> flow/task-scheduler
  -> fileServer/minio
  -> notification suite
  -> printer/auditlog/customProperty/license/i18n/websocket

business services
  -> FoundationMs/RMMs/WOMMs/craftGraphMs
  -> EamMs/OEEMs/ToolMs/SpecialMs
  -> LIMS/LIMSDCMan/LIMSINT/LIMSMatStds
  -> WTSs/WAPS
```

## 恢复源码内的常见模块层

原厂模块命名大体按下面的依赖方向组织：

```text
*-resources
  -> 静态资源、菜单、权限、字典、初始化数据

*-common
  -> 通用常量、DTO、工具类

*-api / *-openapi
  -> Feign/API 契约

*-dao
  -> Mapper、实体、数据库访问

*-service / *-manager
  -> 业务编排和事务逻辑

*-webapi
  -> Controller/API 暴露层

*-bootstrap
  -> Spring Boot 启动入口
```

新建可编译模块时应保持这个方向：`webapi -> service/manager -> dao -> common/api/resources`。不要让 DAO 反向依赖 webapi 或启动层。

## 基础平台优先级

建议按下面顺序把恢复源码提升成可构建模块：

1. `auth` / `iam` / `rbac` / `organization`：登录、人员组织、权限菜单，是页面可用性的核心。
2. `system-config` / `systemcode` / `configuration`：菜单、编码、模块配置和动态页面依赖。
3. `flow` / `task-scheduler`：待办、流程、后台任务。
4. `file-server` / `printer` / `notification`：文件、打印、消息类外设能力。
5. 业务包模块：按现场需要逐个接入，避免一次性提升全部业务包。

## 依赖管理原则

- 新模块继承根 `pom.xml`，不要复制恢复 POM 中的父级和私服配置。
- 统一使用父 POM 管理 Spring、数据库驱动和 Maven 插件版本。
- Oracle 驱动只允许在 legacy profile 或明确标注的兼容模块中出现。
- Mapper SQL 必须先通过 `make audit-postgres-mappings` 或有等价报告。
- 运行服务的端口、Nacos Data ID、启动类以 `backend/services/<service>/README.md` 为准。

## 业务包接入模板

新业务包进来时按这个节奏接入：

1. 先把原包放到 `runtime/bap-server/module-Server/<module>` 的测试部署目录，不提交二进制。
2. 将可读源码或反编译源码整理到 `backend/modules` 或 `backend/decompiled-services`。
3. 在 `deploy/docker/docker-compose.yml` 增加服务条目，先让前端菜单可见。
4. 对数据库脚本做 PostgreSQL 兼容转换，落到 `deploy/docker/postgres/init/NNN-*.sql`。
5. 通过 smoke 后，再把高频维护代码提升到 `backend/source-modules/<module>`。
