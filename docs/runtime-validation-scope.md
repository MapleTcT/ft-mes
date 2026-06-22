# 测试环境验证范围

## 当前目标

当前阶段先把 ADP/MES 恢复仓库变成可持续开发和可重复验证的测试环境，不把它直接声明为完整生产可交付系统。

验证重点是平台主链路可用、PostgreSQL 默认路径可持续修补、新业务包接入时风险可追溯。

## 当前测试入口

当前办公网络下的测试环境入口：

- SSH / Docker / DB 主机：`v6@100.99.133.43`
- 前端默认入口：`http://100.99.133.43:18080`
- 辅助入口：`http://100.99.133.43:18070`
- 旧公网浏览器 fallback：`http://222.88.185.146:18080`，只保留为历史证据入口；当前平台综合 smoke 默认浏览器入口已经统一为 `http://100.99.133.43:18080`。

当前入口 smoke：

```bash
make smoke-test-environment
make runtime-smoke-reports-check
```

最新机器证据见 `metadata/test-environment-smoke.json`：`18080` / `18070`
HTTP 入口、SSH、nginx、gateway、PostgreSQL、Nacos、Keycloak 和 MinIO
核心容器均 PASS。该 smoke 不读取或写入应用账号、数据库密码、SSH key 或
MinIO secret。入口 smoke 只做 `HTTP HEAD` 状态探测，若服务不支持 HEAD 才退到
小范围 ranged GET；完整页面渲染仍由平台 smoke 和浏览器业务 smoke 验收。

`runtime-smoke-reports-check` 不访问远程环境；它只校验已落盘的测试环境、
PostgreSQL、Nacos、Keycloak/JWT 和 MinIO runtime smoke 报告是否仍是 PASS、
是否指向当前测试主机、是否覆盖关键检查，以及是否没有写入敏感凭据。远程环境
变化后必须先重新运行对应 `make smoke-*`，再跑该离线检查。

PostgreSQL 运行态 schema smoke：

```bash
make smoke-postgres-runtime
```

最新机器证据见 `metadata/postgres-runtime-smoke.json`：测试环境数据库为
PostgreSQL 15，public schema 当前 1474 张表、150 个 view；平台、组织、
RBAC、待办、低代码元数据、WOM、RM、QCS、WTS、WAPS 和批次相关 32 个关键
表均存在，15 个已知兼容列和 8 个兼容索引均 PASS。该 smoke 只执行只读查询，
不会写 marker、业务数据或数据库密码。

Nacos 运行态配置和服务注册 smoke：

```bash
make smoke-nacos-config
```

最新机器证据见 `metadata/nacos-config-drift-smoke.json`：测试环境 Nacos
group `prod` 可拉取 44 个 dataId，20 个关键 PostgreSQL/JWT/Nacos 配置检查
PASS，非注释 Oracle 残留为 0；同时验证 group `prod` 当前注册服务 91 个，
18/18 个关键平台/业务服务均存在 healthy 实例。该 smoke 会记录 local/rendered
与远端配置 hash drift，但不会输出数据库密码、JWT secret 明文或 SSH key。

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

默认目标是 `ADP_BASE_URL=http://100.99.133.43:18080`，也可以显式指定：

```bash
make smoke-platform ADP_BASE_URL=http://100.99.133.43:18080 ADP_BROWSER_BASE_URL=http://100.99.133.43:18080 ADP_USERNAME=admin ADP_PASSWORD=123456
```

当前默认浏览器入口也是 `http://100.99.133.43:18080`。只有需要复核历史慢链路
问题时，才显式把 `ADP_BROWSER_BASE_URL` 覆盖为旧公网 fallback。

`make smoke-platform` 会给每个子 smoke 加外层超时，默认
`ADP_PLATFORM_SECTION_TIMEOUT_MS=300000`。如果某个页面脚本卡住，统一报告会记录
`timedOut=true`、`timeoutMs`、`signal` 和子报告摘要，避免整体验收无限挂起。组织管理
子报告还会记录浏览器最终 URL、页面标题和 body 文本摘要；`make smoke-platform` 和
`make smoke-organization` 会把 `ADP_ORG_VISIBLE_TIMEOUT_MS=240000` 传给组织页专项脚本，
用于覆盖外出办公时 Tailscale DERP 慢链路下的大 JS 首次加载。若 API 全部 200 但 body
为空或部门节点不可见，应按前端渲染/静态资源/登录态注入问题继续查，不要直接归因到
PostgreSQL 缺表缺列。

该命令会依次运行：

- `adp-platform-api-smoke.js`：登录、当前用户、权限、菜单、待办 API、基础配置、实体配置。
- `adp-home-todo-smoke.js`：登录页、主页框架、顶部待办、可见错误和网络错误。
- `adp-organization-smoke.js`：组织管理入口、部门树、部门详情、部门关联人员接口，并用浏览器点击一个部门节点。
- `adp-menu-smoke.js`：当前用户菜单返回的可导航页面。`make smoke-platform` 默认巡检前 40 个目标，完整页面巡检可运行 `make smoke-menu`，或用 `PLATFORM_MENU_LIMIT=0 make smoke-platform` 关闭限制。

统一报告默认写入 `/tmp/adp-platform-validation-smoke/platform-validation-summary.json`。单项报告会放在同一目录下，便于后续线程按失败项继续追查。

当前可复验平台综合 smoke 证据固化在 `metadata/platform-validation-smoke.json`。`make platform-validation-check`
不访问远程环境；它只校验该报告是否指向当前测试主机、是否覆盖 platform API、首页待办、
组织部门点击、RBAC authority 和菜单页面抽样、是否全部 PASS，以及报告里是否误写入敏感凭据。
远程环境变化后必须先重新运行 `make smoke-platform` 并更新报告，再运行这个离线检查。

2026-06-15 测试环境 `10.11.100.17:18080` 验证结果：`/tmp/adp-platform-validation-org-fix-20260615/platform-validation-summary.json`，API 16/16、首页待办通过、组织管理部门点击和关联人员接口通过、菜单页面抽样 40/40 通过。2026-06-20 当前办公网络切到 `100.99.133.43`，入口 smoke 见 `metadata/test-environment-smoke.json`。2026-06-21 当前地址平台综合 smoke 已统一为 API/browser base `http://100.99.133.43:18080`，报告见 `metadata/platform-validation-smoke.json` 和 `/tmp/adp-platform-validation-1009913343-20260621052958/platform-validation-summary.json`，platform API `16/16`、首页待办、组织部门点击、RBAC authority `9/9`、菜单抽样 `40/40` 均通过。2026-06-21 组织页专项复验见 `/tmp/adp-organization-smoke-20260621044306/organization-smoke-results.json`：API base/browser base 均为 `http://100.99.133.43:18080`，部门树、部门详情、关联人员 API 均 200，浏览器最终 URL 为 `/organization/#/organizationmanage`，body 可见“组织管理/部门/办公室/关联岗位/关联人员”，未出现 `数据库操作异常`、network error、page error 或 request failed。同期静态资源对比显示服务器本机和公网地址下载主 JS 正常，而本机到 `100.99.133.43` 经 `tailscale ping` 走 DERP(lax) 且吞吐很低；遇到短时间白屏时，应先延长组织页 smoke 窗口或用公网 fallback 做链路隔离，再继续判断功能缺陷。

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
- 未闭合项 Backlog：[生产模块 Backlog 账本](production-module-backlog.md)
- Backlog 机器记录：`metadata/production-module-backlog.json`
- Backlog 校验：`make production-module-backlog-check`

生产模块的页面 smoke 已通过不等于业务动作通过。新增、编辑、删除、下发、审批、报工、退料、导入、质量回写、入库和状态流转，都必须按用例矩阵逐项补前端操作、接口捕获、后端链路和 PostgreSQL 查询证明。

落库验收中出现 `FAIL` 或 `BLOCKED` 时，也必须进入生产模块 Backlog。典型例子是 public `produceTaskCreated`：旧恢复实现会返回 `200/处理成功` 但 `wom_produce_tasks` marker 计数仍为 0；测试运行包现已显式禁用，最新探针为 `HTTP 200/code=400` 且 marker 计数仍为 0，因此只能保留为 `BLOCKED` 产品契约确认项，不得被 `produceTaskCreated2` 的 PASS 证据替代。

当前生产矩阵里仍依赖外部业务包的 blocker 可以先用只读 readiness
smoke 复验：

```bash
make smoke-business-dependencies
make business-dependency-readiness-check
```

默认目标是 `http://100.99.133.43:18080` 和 `v6@100.99.133.43`。最新报告
见 `metadata/business-dependency-readiness-smoke.json`：`material-service`
和 `process-analysis` 当前均为 `BLOCKED`，原因是 Nacos 无健康依赖实例、
认证依赖端点返回 `503`，且 `ProcessAnalysis` 的 PostgreSQL/runtime/menu
元数据仍为空。后续业务包部署后，先跑这个 smoke，确认不再是外部依赖缺失，
再进入真实前端 marker 落库验收。`business-dependency-readiness-check`
只做离线结构校验，用于保证已落盘报告和文档没有漏掉 material/ProcessAnalysis
这两个关键外部依赖。

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

`make module-intake-precheck-regression-check` 已纳入 CI，会用临时干净包和临时 Oracle 污染包回归验证 `module-intake-check` 的通过、失败和 `--report-only` 语义，防止准入检查脚本退化。

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
