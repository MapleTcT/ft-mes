# 功能验收与落库验收规则

## 目标

本项目后续功能验证必须优先证明真实系统行为，而不是只整理治理文档、只跑静态检查或只看代码推断。

当前核心目标是：

- 在真实前端页面或等效 E2E 浏览器环境测试当前系统功能。
- 对会改变业务数据的后端动作完成“是否真实落库”的 PostgreSQL 验收。
- 形成可交接、可复验、可继续推进的测试与落库验收报告。
- 必要时修复阻断问题，但不能跳过验证。

## 固定上下文

- 仓库路径：`/Users/zhangchu/Documents/ADP/adp-source-repo`
- 默认数据库：PostgreSQL。
- Oracle 只允许作为 `legacy-template-only` 或显式 `oracle-legacy` 对照路径存在，不允许重新变成默认运行路径。
- 真实测试入口优先使用 Docker/Nginx 运行包前端：`http://10.11.100.17:18080`。
- 前端恢复源码来自 source map，主要用于路由/API/组件追踪；不要把源码静态可读等同于页面功能可用。

## 执行前必须读取

每次进入功能验收或落库验收任务，先阅读：

- [项目目标和交付路线](project-objectives.md)
- [可持续开发仓库说明](sustainable-development.md)
- [当前内容迁移清单](current-content-inventory.md)
- [后端落表业务排查交接](backend-table-audit-handoff.md)
- [后端落表排查索引](backend-table-audit/00-index.md)
- [Docker 测试部署](../deploy/docker/README.md)
- `Makefile`
- 前端 `README`、`package.json`、路由入口和服务请求文件
- 后端 `README`、`pom.xml`、服务配置、Controller、Service、Mapper、SQL 和 PostgreSQL 初始化脚本

执行者必须先判断并记录：

- 前端如何启动或访问。
- 后端如何启动。
- PostgreSQL 如何启动和查询。
- 当前有哪些前端页面、菜单、路由和主要业务操作。
- 哪些操作理论上触发后端接口。
- 哪些接口理论上写入或更新数据库。

## 功能验收硬规则

- 不允许只看代码判断功能是否可用。
- 不允许只跑 `make ci`、`make runtime-script-check` 或静态扫描后声称功能完成。
- 必须通过浏览器或等效前端 E2E 方式访问页面。
- 如果前端、后端或数据库无法启动，先修复启动阻断；启动失败本身是最高优先级问题。
- HTTP 200 不等于功能通过，页面可见错误、console error、network error、权限缓存、空白页和静默失败都必须记录。
- 测试账号重新登录后仍要清理相关站点缓存或 localStorage，避免旧 token、菜单和按钮权限污染结果。

每个被测功能至少记录：

- 页面 / 路由。
- 操作步骤。
- 预期结果。
- 实际结果。
- 是否有前端 console error。
- 是否有 network error。
- 涉及的 API endpoint。
- 是否需要落库。
- 验收结论：`PASS`、`FAIL`、`BLOCKED`、`NOT_APPLICABLE`。

优先测试：

- 登录 / 认证入口。
- 首页 / Dashboard。
- 菜单导航。
- 列表页。
- 查询 / 筛选 / 分页。
- 新增表单。
- 编辑表单。
- 删除 / 禁用 / 状态变更。
- 详情页。
- 导入 / 导出。
- 文件上传。
- 配置类页面。
- 业务主流程页面。

## 落库验收硬规则

对每个会改变业务数据的前端操作，必须完成后端落库验收。

执行方法：

1. 在前端执行带唯一标识的业务动作，名称、备注或编码中加入 marker，例如 `ADP_E2E_YYYYMMDD_HHMMSS_xxx`。
2. 捕获该动作对应 HTTP 请求：method、URL、request payload、response status、response body 关键字段。
3. 追踪后端链路：controller / route、service、repository / mapper / DAO、SQL / ORM 映射、目标表。
4. 直接查询 PostgreSQL，确认数据真实落库。
5. 更新、删除、禁用、启用、状态变更类操作必须查询字段变化。
6. 记录验收 SQL 和查询结果摘要。
7. 接口成功但数据库没有变化，标记 `FAIL` 并定位原因。
8. 功能本身不应落库时，写明原因并标记 `NOT_APPLICABLE`。
9. 上游阻断无法执行时，标记 `BLOCKED` 并写清阻断点。

禁止事项：

- 不用 mock 数据冒充真实落库。
- 不只看接口 200 判断落库成功。
- 不只看 Mapper 文件判断落库成功。
- 不为了通过检查而修改报告数据。
- 不把 PostgreSQL 问题绕回 Oracle。
- 不引入运行包二进制、临时 dump、大文件或无关生成物。

## 验收资产

每次功能验收必须更新这些资产：

- [前端功能测试报告](frontend-functional-test-report.md)
- [后端落库验收报告](backend-table-audit/persistence-acceptance.md)
- [机器可读落库验收记录](../metadata/persistence-acceptance.json)

机器可读记录必须能通过：

```bash
make persistence-acceptance-check
```

`PASS` 只允许用于已经完成真实页面操作、接口记录、后端链路追踪和 PostgreSQL 查询证明的项目。未执行的验收不能写成 `PASS`。

## 状态定义

| 状态 | 含义 |
| --- | --- |
| `PASS` | 真实前端操作完成，接口和后端链路可追踪，PostgreSQL 查询证明符合预期。 |
| `FAIL` | 前端、接口或数据库实际结果与预期不符，且不是环境阻断。 |
| `BLOCKED` | 启动、权限、依赖、缺表、缺数据或上游服务阻断，导致无法完成验收。 |
| `NOT_APPLICABLE` | 该功能不改变业务数据，或本身不应落库，并已说明原因。 |
