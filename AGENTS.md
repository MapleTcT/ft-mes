# ADP/MES 项目工作指令

## 当前核心目标

本项目当前阶段以功能验收优先，不以治理文档、静态扫描或代码推断作为完成标准。

2026-06-16 起，本项目执行任何测试、修复、模块接入或交付验收时，都必须把“真实前端功能测试 + 后端业务动作 PostgreSQL 落库证明”作为最高优先级。治理文档、静态检查、接口 200、Mapper 推断和页面能打开都只是辅助证据，不能替代真实页面操作和数据库查询。

当前任务口径：

- 不是继续补治理层。
- 不是只跑静态检查。
- 不是只整理代码或报告。
- 必须先验证真实前端行为，再证明后端写动作是否真实落到 PostgreSQL。
- 必要修复可以做，但修复后仍必须回到真实页面、接口、后端链路和数据库查询证据。

每次进入测试、修复、模块接入或交付验收时，必须优先完成下面四件事：

1. 在前端真实测试当前系统功能。
2. 对后端完成“业务动作是否真实落库”的 PostgreSQL 验收。
3. 形成可交接、可复验、可继续推进的测试与落库验收报告。
4. 必要时修复阻断问题，但不能跳过验证、不能只凭代码推断。

如果前端无法启动、后端无法启动、数据库无法连接、页面空白、权限不足、接口 500 或 SQL 异常，不能跳到写文档结束；这些问题本身就是最高优先级阻断项，必须先修复或按 `BLOCKED` 写入验收资产。

## 固定仓库上下文

- 仓库路径：`/Users/zhangchu/Documents/ADP/adp-source-repo`
- 本轮任务声明的 `main` 同步基线：`b6b5ecd5cd0dbb5c425a4e79fee110b9158ce854`
- 默认数据库：PostgreSQL。
- Oracle 只允许作为 `legacy-template-only` 或显式 `oracle-legacy` 路径存在，不允许重新变成默认运行路径。
- 功能验收规则入口：`docs/functional-persistence-acceptance.md`
- 前端验收报告：`docs/frontend-functional-test-report.md`
- 后端落库验收报告：`docs/backend-table-audit/persistence-acceptance.md`
- 机器可读验收记录：`metadata/persistence-acceptance.json`

## 执行前必须读取

开始功能验收或落库验收前，先阅读并确认：

- `docs/project-objectives.md`
- `docs/sustainable-development.md`
- `docs/current-content-inventory.md`
- `docs/backend-table-audit-handoff.md`
- `docs/backend-table-audit/00-index.md`
- `Makefile`
- `deploy/docker/` 下 Docker Compose、环境变量和 README
- 前端 README、`package.json`、路由入口、服务请求文件
- 后端 README、`pom.xml`、application 配置、Controller、Service、Mapper、DAO 和 SQL 初始化脚本

必须先判断并记录：

- 前端如何启动或访问。
- 后端如何启动。
- PostgreSQL 如何启动和查询。
- 当前有哪些前端页面、菜单、路由和主要业务操作。
- 哪些操作理论上应该触发后端接口。
- 哪些接口理论上应该写入或更新数据库。
- 实际仓库 HEAD；如果任务声明的 `main` 同步基线和本地 HEAD 不一致，先记录差异，不直接覆盖或回滚用户改动。

## 真实前端测试要求

- 不允许只看代码判断功能是否可用。
- 不允许只跑 `make ci`、静态扫描或文档检查后声称功能完成。
- 必须通过浏览器或等效前端 E2E 访问页面。
- 如果前端、后端或数据库无法启动，先修复启动阻断；启动失败本身是最高优先级问题。
- HTTP 200 不等于功能通过，console error、network error、空白页、权限缓存、可见系统错误和静默失败都必须记录。

每个被测功能至少记录：

- 页面 / 路由
- 操作步骤
- 预期结果
- 实际结果
- 前端 console error
- network error
- API endpoint
- 是否需要落库
- 验收结论：`PASS` / `FAIL` / `BLOCKED` / `NOT_APPLICABLE`

优先测试：

- 登录 / 认证入口
- 首页 / Dashboard
- 菜单导航
- 列表页
- 查询 / 筛选 / 分页
- 新增表单
- 编辑表单
- 删除 / 禁用 / 状态变更
- 详情页
- 导入 / 导出
- 文件上传
- 配置类页面
- 业务主流程页面
- 生产模块完整功能页面和主流程动作

## 后端落库验收要求

对每个会改变业务数据的前端操作，必须完成后端落库验收：

1. 在前端执行一个带唯一标识的业务动作，marker 格式建议为 `ADP_E2E_YYYYMMDD_HHMMSS_xxx`。
2. 捕获 HTTP 请求的 method、URL、request payload、response status 和 response body 关键字段。
3. 追踪 controller / route、service、repository / mapper / DAO、SQL / ORM 映射和目标表。
4. 直接查询 PostgreSQL，确认数据是否真实落库。
5. 更新、删除、禁用、启用、状态变更类操作必须查询字段变化。
6. 记录 SQL 查询语句和查询结果摘要。
7. 接口成功但数据库没有变化，必须标记 `FAIL` 并定位原因。
8. 功能本身不应落库时，写明原因并标记 `NOT_APPLICABLE`。
9. 上游阻断无法执行时，标记 `BLOCKED` 并写清阻断点。

禁止事项：

- 不用 mock 数据冒充真实落库。
- 不只看接口 200 判断落库成功。
- 不只看 mapper 文件判断落库成功。
- 不为了通过检查而修改报告数据。
- 不把 PostgreSQL 问题绕回 Oracle。
- 不引入运行包二进制、临时 dump、大文件或无关生成物。

## 验收资产和门禁

每次功能验收后必须同步更新：

- `docs/frontend-functional-test-report.md`
- `docs/backend-table-audit/persistence-acceptance.md`
- `metadata/persistence-acceptance.json`

这些资产必须可交接、可复验、可继续推进。每条记录都要能回答页面/路由、操作步骤、预期结果、实际结果、console error、network error、API endpoint、是否需要落库、后端入口、目标表、验收 SQL 和最终状态。

并至少运行：

```bash
make persistence-acceptance-check
make sustainable-check
```

涉及仓库结构、依赖、SQL、模块接入或默认数据库路径时，还必须运行相关 Makefile 门禁，优先使用 `make ci`。

`PASS` 只允许用于真实页面操作、接口记录、后端链路追踪和 PostgreSQL 查询证明都齐全的项目。未执行、被阻断或不落库的功能必须分别标记为 `BLOCKED` 或 `NOT_APPLICABLE`。

报告不是收尾动作，而是证据账本。启动失败、页面空白、权限不足、接口 500、SQL 异常、接口 200 但未落库，都必须进入验收资产，不能因为问题未修复就跳过记录。
