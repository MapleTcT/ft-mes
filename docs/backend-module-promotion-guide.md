# 后端模块提升指南

## 目标

把恢复出来的后端源码逐步提升为可编译、可测试、可长期维护的 Maven 模块，同时把 Oracle 依赖从默认路径中移除。

## 为什么不能直接改 `backend/modules`

`backend/modules/` 来自原 Windows 包中的 `sources.jar`，它是可信源码参考，但不是完整原厂工程：

- 缺原厂父 POM。
- 缺私服依赖和构建插件上下文。
- 存在同名多版本模块。
- 部分源码只是一段运行包依赖的源码包，不代表可直接构建。

因此，可维护代码应复制到 `backend/source-modules/<module>` 后再改，不直接覆盖恢复目录。

## 标准流程

### 1. 建模块

```bash
make create-backend-module MODULE=platform-auth PACKAGE=com.mapletct.ftmes.platform.auth
```

没有特殊包名时可以省略 `PACKAGE`：

```bash
make create-backend-module MODULE=platform-auth
```

### 2. 复制源码

从恢复目录复制最小闭环：

```text
backend/modules/<group>/<artifact>/<version>/
  -> backend/source-modules/<module>/src/main/java/
  -> backend/source-modules/<module>/src/main/resources/
```

保留恢复目录，方便后续 diff 和追溯。

### 3. 收敛依赖

新模块只声明真实需要的依赖：

- 先查看 [后端恢复模块依赖库存](backend-module-dependency-inventory.md)，确认原模块 family/layer、内部依赖、外部依赖、重复坐标和 Oracle/JDBC 风险。
- 继承根 `ft-mes-parent`。
- 使用父 POM 里的 Spring、Spring Cloud、PostgreSQL JDBC 版本。
- 不复制恢复 POM 中的私服、父工程、Oracle 驱动和无关插件。
- Oracle 只允许出现在明确的 legacy profile 或迁移说明中。
- 默认 `src/main` 不允许带入 Oracle JDBC URL、driver、Hibernate dialect 或 `mapper/oracle` 资源；`make source-module-check` 会阻断。

### 4. 处理数据库

模块提升时必须同步处理数据库：

- Mapper SQL 默认走 PostgreSQL。
- Oracle SQL 保留为参考，不进入默认运行路径，也不放入默认 `src/main`。
- 幂等 SQL 放在 `deploy/database/<module>/postgres/` 或当前 Docker init 补丁序列中。
- 更新 [Oracle 迁移 Backlog](oracle-migration-backlog.md)。
- 更新 `docs/backend-table-audit/` 中的落表报告。

### 5. 验证

至少运行：

```bash
make source-module-check
make source-module-test
make backend-dependency-check
make audit-postgres-mappings
make oracle-audit
make ci
```

有远程环境时继续跑：

```bash
make smoke-api
make smoke-menu
make smoke-todo
```

业务模块还要跑：

```bash
make smoke-business
```

## 推荐提升顺序

1. `auth` / `iam` / `rbac` / `organization`
2. `configuration` / `system-config` / `systemcode`
3. `flow` / `task-scheduler`
4. `file-server` / `printer` / `notification`
5. LIMS/QCS/Qualify 等质量域
6. 生产、设备、能源、安环等业务模块

## 完成定义

单个模块提升完成，需要满足：

- 模块存在于 `backend/source-modules/<module>`。
- 聚合 POM 已包含该模块。
- `make source-module-check` 通过。
- `make source-module-test` 通过。
- 相关恢复模块依赖已在 [后端恢复模块依赖库存](backend-module-dependency-inventory.md) 中可追溯。
- `mvn -q -DskipTests validate` 通过。
- Oracle 默认依赖、默认配置、默认 mapper/resource 路径已移除。
- 数据库表/字段映射已记录到落表报告。
- PostgreSQL smoke 或等价验证证据已记录。
