# Oracle 到 PostgreSQL 替换路线

## 当前状态

测试部署默认使用 PostgreSQL：

- `deploy/docker/.env.example` 已设置 `SUPOS_SYSTEM_DB_TYPE=postgresql`。
- `deploy/docker/docker-compose.yml` 在无 `.env` 时也默认指向 `postgres:5432`。
- PostgreSQL 初始化和兼容 SQL 位于 `deploy/docker/postgres/init/`。
- Runtime JAR 的 PostgreSQL mapper/DBP 注入由 `deploy/docker/scripts/patch-postgres-runtime.py` 负责。
- GitHub Actions 会验证 Maven reactor 和 Docker Compose 渲染，防止默认数据库配置回退到隐式 Oracle。

Oracle 相关配置没有删除，保留在 `deploy/docker/.env.oracle-legacy.example`，用于后续需要回连老库或对比迁移结果的场景。

## 替换原则

- 默认环境只走 PostgreSQL。
- Oracle 兼容必须显式开启，不允许成为隐式默认值。
- 业务逻辑层不直接判断数据库类型。
- 数据库方言差异集中在 Mapper、Repository、migration SQL 或数据库适配层。
- 每次迁移一个模块，必须有 smoke 或 SQL audit 证据。

## 阶段计划

### 阶段 0：运行态兼容

目标是让测试环境可用：

- 保留 Docker runtime patch。
- 继续补 `deploy/docker/postgres/init/NNN-*.sql`。
- 用页面 smoke 和接口 smoke 覆盖人员、组织、菜单、待办、业务模块入口。

### 阶段 1：源码侧 SQL 审计

目标是定位 Oracle/MySQL/SQL Server 方言残留：

```bash
make audit-postgres-mappings
```

如果只是想产出阶段性报告而不阻断当前工作：

```bash
make audit-postgres-report
```

审计脚本按 `error` / `warning` 分级。`to_char` 这类 PostgreSQL 也支持、但在 Oracle 迁移中仍值得人工确认的函数会作为 warning 记录，不作为阻断项。

同时用下面命令维护 Oracle 依赖和方言引用 backlog：

```bash
make oracle-audit
make oracle-audit-check
```

报告见 [Oracle 迁移 Backlog](oracle-migration-backlog.md)，机器可读结果在 `metadata/oracle-migration-audit.json`。

重点处理：

- `rownum`
- `sysdate` / `systimestamp`
- `nvl` / `decode`
- `from dual`
- `varchar2` / `number`
- MySQL `ifnull`、反引号、`group_concat`
- Boolean 与 `0/1` 混用

### 阶段 2：模块提升

目标是把高频维护模块从 runtime patch 转成源码构建：

- 先提升基础平台关键模块：认证、RBAC、组织、系统配置、流程。
- 每个模块在 `backend/source-modules/<module>` 建独立 Maven 模块。
- PostgreSQL SQL 放主路径；Oracle SQL 如仍需要，放 legacy profile 或隔离资源目录。

### 阶段 3：迁移脚本产品化

目标是从“补丁 SQL”变成“可审计迁移脚本”：

- 每个模块维护独立 migration 编号。
- DDL、DML、兼容 view/function 分开。
- 每次变更记录来源：原厂 init.xml、运行时报错、页面 smoke、人工业务规则。
- 可以重复执行的脚本必须保持幂等。

### 阶段 4：Oracle 退场

满足以下条件后，才能删除对应 Oracle 依赖：

- 模块源码已提升为可编译模块。
- PostgreSQL 测试环境通过接口和页面 smoke。
- Mapper audit 对该模块无阻断项。
- 没有运行时配置继续引用 Oracle host/port/service name。
- 文档中记录了旧 Oracle 脚本对应的 PostgreSQL 替代方案。

## 检查清单

- `make verify-pom`
- `make compose-config`
- `make audit-postgres-mappings`
- `make smoke-api`
- `make smoke-menu`
- `make smoke-todo`

业务包接入后还要跑：

```bash
make smoke-business
```
