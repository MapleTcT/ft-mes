# Backend Source Modules

这里是后续可持续开发的后端源码区。

`backend/modules/` 保存恢复出来的 `sources.jar` 源码参考；这里保存已经提升为标准 Maven 模块、可以参与构建和测试的源码。

## 创建模块

```bash
make create-backend-module MODULE=platform-auth
make create-backend-module MODULE=platform-auth PACKAGE=com.mapletct.ftmes.platform.auth
```

脚手架会：

- 创建 `backend/source-modules/<module>/pom.xml`。
- 创建 `src/main/java`、`src/main/resources`、`src/test/java`。
- 更新 `backend/source-modules/pom.xml` 的 `<modules>`。
- 默认继承根 `ft-mes-parent`。
- 默认使用 PostgreSQL runtime 依赖。

## 校验

```bash
make source-module-check
make backend-dependency-check
make ci
```

校验会阻止：

- 模块目录有 POM 但没有加入聚合 POM。
- 模块没有继承根父 POM。
- 模块 `artifactId` 和目录名不一致。
- 模块直接声明 Oracle JDBC。
- 模块默认数据库类型写成 Oracle。

## 提升原则

1. 从 `backend/modules/<group>/<artifact>/<version>` 复制可信源码到新模块。
2. 保留原恢复目录不动，方便对比和回溯。
3. 复制前先查看 [后端恢复模块依赖库存](../../docs/backend-module-dependency-inventory.md)，确认原模块依赖、重复坐标和 Oracle/JDBC 风险。
4. 先提升 `common/api/dao/service/webapi` 中最小可构建闭环。
5. DAO/Mapper 层优先 PostgreSQL，Oracle SQL 只能保留在 legacy 说明或迁移 backlog。
6. 每个已提升模块补最小测试或 smoke 证据。
