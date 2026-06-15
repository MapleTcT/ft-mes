# 测试环境验证范围

## 当前目标

当前阶段先把 ADP/MES 恢复仓库变成可持续开发和可重复验证的测试环境，不把它直接声明为完整生产可交付系统。

验证重点是平台主链路可用、PostgreSQL 默认路径可持续修补、新业务包接入时风险可追溯。

## 平台重点验证

本阶段必须持续验证这些基础能力：

- 登录和当前用户信息。
- 用户、组织、岗位、角色和权限。
- 菜单、按钮权限和前端路由。
- 待办、流程入口和基础工作台。
- 基础配置、系统字典、实体配置和低代码元数据。
- Nacos 配置渲染。
- Keycloak 认证集成。
- PostgreSQL 初始化、兼容 SQL 和 runtime patch。
- Docker Compose 启停、日志和 smoke 脚本。

这些能力是后续业务模块能否开发、测试和落表排查的前置条件。

## 平台验证命令

测试环境启动后，优先运行统一平台验证：

```bash
npm install
npx playwright install chromium
make smoke-platform
```

默认目标是 `ADP_BASE_URL=http://10.11.100.17:18080`，也可以显式指定：

```bash
make smoke-platform ADP_BASE_URL=http://10.11.100.17:18080 ADP_USERNAME=admin ADP_PASSWORD=123456
```

该命令会依次运行：

- `adp-platform-api-smoke.js`：登录、当前用户、权限、菜单、待办 API、基础配置、实体配置。
- `adp-home-todo-smoke.js`：登录页、主页框架、顶部待办、可见错误和网络错误。
- `adp-organization-smoke.js`：组织管理入口、部门树、部门详情、部门关联人员接口，并用浏览器点击一个部门节点。
- `adp-menu-smoke.js`：当前用户菜单返回的可导航页面。`make smoke-platform` 默认巡检前 40 个目标，完整页面巡检可运行 `make smoke-menu`，或用 `PLATFORM_MENU_LIMIT=0 make smoke-platform` 关闭限制。

统一报告默认写入 `/tmp/adp-platform-validation-smoke/platform-validation-summary.json`。单项报告会放在同一目录下，便于后续线程按失败项继续追查。

2026-06-15 测试环境 `10.11.100.17:18080` 验证结果：`/tmp/adp-platform-validation-org-fix-20260615/platform-validation-summary.json`，API 16/16、首页待办通过、组织管理部门点击和关联人员接口通过、菜单页面抽样 40/40 通过。

如果只是临时定位 API 或页面问题，可以继续单独运行：

```bash
make smoke-api
make smoke-todo
make smoke-organization
make smoke-menu
```

## 业务模块验证边界

生产、设备、质量、安环、能源等业务模块整体仍按初始接入验证推进：

- 服务可以启动。
- 前端菜单可以看到模块入口。
- 主要页面能打开，空白页、翻译 key、静态资源缺失要记录。
- 主要 API 可以调用，认证和网关链路正常。
- 页面或 API 触发的表、字段、Mapper 和 SQL 初步可追溯。

业务模块当前不直接承诺完整业务闭环。完整业务测试需要另起线程按模块补业务说明、测试数据、状态流转、落表证据和用户签字。

生产模块是当前阶段的例外重点，必须维护动作级测试用例矩阵：

- 用例矩阵：[生产模块功能测试用例矩阵](production-module-functional-test-cases.md)
- 机器可读记录：`metadata/production-module-test-cases.json`
- 结构校验：`make production-testcase-check`

生产模块的页面 smoke 已通过不等于业务动作通过。新增、编辑、删除、下发、审批、报工、退料、导入、质量回写、入库和状态流转，都必须按用例矩阵逐项补前端操作、接口捕获、后端链路和 PostgreSQL 查询证明。

## PostgreSQL 缺口处理

每发现一个 PostgreSQL 兼容缺口，都必须进入可追溯资产，不能靠清库重建或手工临时改库掩盖：

- 缺表：补幂等 `CREATE TABLE IF NOT EXISTS` 或进入模块 backlog。
- 缺列：补幂等 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` 或进入模块 backlog。
- 类型不兼容：补显式转换、兼容 view/function 或进入模块 backlog。
- Oracle 方言残留：补 PostgreSQL SQL、Mapper 分支迁移或进入模块 backlog。
- 缺初始化数据：补幂等 DML 或记录需要业务确认的数据字典。

默认落点优先是 `deploy/docker/postgres/init/` 的编号 SQL、`docs/oracle-migration-backlog.md`、`docs/postgres-migration-index.md` 和 `docs/backend-table-audit/`。

## 新业务包准入

新业务包或恢复模块进入可维护源码区前，先运行：

```bash
make module-intake-check INTAKE=/path/to/package-or-dir
```

该检查只读扫描目录、源码文件和 zip/jar 包，不会解包或修改候选内容。阻断项包括默认路径中的 Oracle JDBC 依赖、Oracle URL/driver/dialect、Oracle mapper/resource 路径；关注项包括二进制包和常见 SQL 方言风险。

检查结果默认写入 `/tmp/adp-module-intake-precheck.json`。如果只是考古分析、暂时不阻断当前工作，可以直接运行脚本并加 `--report-only`。

## 生产迁移前置项

下面这些不是当前测试环境闭环的一部分，生产迁移前必须单独补齐：

- 数据迁移脚本和全量/增量校验。
- 回滚方案和恢复演练。
- license/软件狗策略。
- MinIO 文件迁移。
- Keycloak 生产库和用户体系策略。
- 端口、域名、TLS 和反向代理。
- 安全加固、密码策略、密钥管理和审计。
- 每个业务模块的 smoke 签字和业务验收记录。
