# PATROL 共享巡检模块恢复验收

- 验收日期：`2026-07-17`
- 原厂产品：`PATROL_1.0.0` / `6.0.4.0`
- 当前源码接入提交：`mes-modules-patrol-intake@0b8f37ebd13f3a9b72ae1a02cd59713e59fb68c0`
- 目标运行构件源码提交：`8971a8770452c2bb55261f330284cd3dd26824b1`
- 主仓库异常隐患补丁基线：`21812424ab512a69407474180b4f7c9a5110c600` + 当前工作树
- 承载服务：`EamMs`
- 目标环境：`v6@10.11.100.17`
- 默认数据库：PostgreSQL
- 当前结论：`TARGET_HIDDEN_DANGER_PERSISTENCE_PASS`

机器可读记录见 [PATROL 恢复验收](../../metadata/patrol-module-recovery-acceptance.json)；可重放脚本见
`deploy/docker/scripts/adp-patrol-task-persistence-acceptance.js`。

## 结论

PATROL 原厂源码、PostgreSQL 迁移、运行元数据、菜单权限、工作流和运行 JAR 已部署到测试环境。
真实浏览器先完成“新增计划 -> 生成任务 -> 普通查询 -> 批量取消 -> PostgreSQL 回读 -> 页面复显”
闭环；随后又完成“新增计划 -> 生成任务 -> 下发 -> 执行 -> 录入结果 -> 完成 -> PostgreSQL 回读 -> 页面复显”
闭环；本轮继续完成“异常结果 -> 完成任务 -> 异常汇总 -> 生成隐患 -> PostgreSQL 关联 -> 重复提交幂等 ->
EAM 台账复显”闭环。执行 marker `ADP_E2E_20260716210839_PATROL_EXECUTION` 的 32 项断言、异常隐患 marker
`ADP_E2E_20260717003024_PATROL_HIDDEN_DANGER` 的 45 项断言全部 PASS，浏览器 console、page error、PATROL network
failure 均为 0，取消分支也在其后独立回归 PASS。

这证明共享巡检的输入标准、路线/区域/项目配置、计划/任务状态和现场执行结果链在 PostgreSQL 上可用，
也再次确认设备巡检、工艺巡检和安环巡检共用同一 PATROL 数据域。它不等于整个巡检产品已经全部验收：
异常结果到 EAM 待治理隐患的生成链已经可用，统计监控页面仍需继续逐页闭合。由于完整 SESH 模块没有交付，
本次 `PATROL_COMPATIBILITY_PENDING` 记录不能被解释为隐患整改、复查和销项治理流程已恢复。

## 来源与构建

| 检查项 | 证据 | 结果 |
|---|---|---|
| 原始包 | `PATROL_6.0.4.0.zip` | PASS |
| 原包 SHA-256 | `1214f11302545d29ec2d611c49cb6bfe87aac3faf1344a94cb69eea9884b3394` | PASS |
| 完整性 | 1113 文件、455 Java、267 Web、9 SQL | PASS |
| PATROL core | `9b9d0367d1acc3e89d38adb556c1941f2d37284f093cded4bb2edc2b573beb3e` | PASS |
| PATROL api | `34b4bce83a4f6af9cb19ebd5a25336b019001b84a384cffeb7df9b777bbf6c6c` | PASS |
| PATROL service | `94c3289a063272da90c7eb3544a23cee200bcf2b210fa9f0594b50db75db0510` | PASS |

模块仍以 Java 8 构建；供应商包没有测试源码，因此构建 PASS 只证明编译和依赖解析，业务结论来自
下面的真实页面/API/PostgreSQL 验收。

## PostgreSQL

目标库按顺序应用并重复执行以下幂等迁移：

1. `178-patrol-technical-tables.sql`
2. `179-patrol-runtime-metadata.sql`
3. `180-patrol-runtime-compat-and-seed.sql`
4. `181-patrol-menu-app-permissions.sql`
5. `182-patrol-workflow-runtime.sql`
6. `183-patrol-system-codes.sql`
7. `184-patrol-ui-runtime-metadata.sql`
8. `185-patrol-task-persistence-compat.sql`
9. `186-patrol-hidden-danger-eam-risk-compat.sql`

第 186 号迁移补齐 EAM 风险模型投影、PATROL 异常明细关联、`SESHRM_riskResource/005` 系统编码及中英文资源。
真实 i18n 运行包把 `supfusion_i18n_resource.modifier` 映射为 `java.util.Date`，旧建库脚本却使用 varchar；迁移会
识别并修复存量字符型字段。在线改列后只需重启一次 i18n 清理旧 prepared plan，新建 Docker 环境在 i18n 启动前
完成迁移，不存在连接缓存切换。

`deploy/docker/postgres/verify/001-patrol-acceptance.sql` 在目标 PostgreSQL 的当前结果：

| 验收面 | 数量 | 状态 |
|---|---:|---|
| 实体表 | 37 | PASS |
| 运行模块 / 实体 / 模型 | 1 / 7 / 27 | PASS |
| 视图 / 条件 / SQL 定义 | 74 / 79 / 209 | PASS |
| 字段 / 数据表格 / 按钮 / 事件 | 1368 / 23 / 75 / 699 | PASS |
| 系统编码实体 / 值 | 19 / 58 | PASS |
| 菜单 / 操作 / 工作流 | 24 / 102 / 2 | PASS |

隔离 PostgreSQL 克隆上的幂等执行和禁用/恢复回滚演练均为 PASS。目标机没有执行破坏性回滚：
该操作会暂时关闭 PATROL 菜单、操作和工作流，必须在明确维护窗口并获得确认后单独执行。

## 运行部署

| 检查项 | 结果 |
|---|---|
| 部署 JAR | `runtime/bap-server/module-Server/EamMs/manual/EamMs-1.0.0.jar` |
| 部署 SHA-256 | `af01d6a750ed4a812bc2028c2c5279453fd7bd03177f71b890a044f74e97f753` |
| 基线 JAR SHA-256 | `b44e4c9c4b79f23621f58d2a51a28450d9cc4225fb4f19586283fc228d49da38` |
| EamMs | 容器运行，PATROL 请求真实命中 |
| PostgreSQL | 容器 healthy |
| Nginx | 配置 `nginx -t` PASS，兼容规则已热加载 |
| 回滚 JAR | `/data/docker/adp-patrol-20260716/EamMs-1.0.0.patrol-postgres-queryfix2-20260716.jar`，SHA-256 `97d3a265...e43d2ab` |
| 结果录入脚本 | `enteringResultEdit/body.js`、`body-es5.js`，目标与仓库 SHA-256 均为 `5c2f6653...b4a360`；空/null 结果不会再被转换为数值 0 |

前端兼容规则只处理旧框架的两个无效错误日志：

- 国际化 API 在参数不是数组时完成空 Promise，不再产生悬挂 Promise。
- 系统编码组件重复移除已不存在选项时保持原有 no-op 语义，不再误报 console error。

PATROL 六种任务状态 `未下发/已下发/执行中/已超期/已完成/已取消` 已由真实系统编码 API 返回，
没有用前端降噪掩盖缺失字典。

## 真实页面与落库

验收脚本使用 `admin` 会话打开真实 PATROL 页面，不使用 mock：

| 动作 | 页面 / API | PostgreSQL | 结果 |
|---|---|---|---|
| 新增巡检计划 | `potrolPlanList`；`POST .../patrolPlan/submit` | `mp_patrol_plans.id=6675852707251024` | PASS |
| 绑定执行人员 | 同一保存事务 | `mp_plan_staffs` 有效关系 1 条 | PASS |
| 生成巡检任务 | `POST .../createTaskEdit/submit` | `mp_create_tasks.id=6675852713018192` | PASS |
| 查询巡检任务 | 点击真实“查询”；`POST .../potrolTaskList-query` | 页面出现 `patrolTask_20260716_009` | PASS |
| 批量取消 | `batchChangeList` 选择真实行；`GET .../taskStateUpdate` | `mp_potrol_tasks.id=6675852717278032` | PASS |
| 任务明细默认值 | 生成任务事务 | `mp_task_details.id=6675852722815824` | PASS |
| 取消后复显 | 再次点击真实“查询” | 同一行显示“已取消” | PASS |
| 下发任务 | `GET .../taskStateUpdate?changeState=PATROL_taskState/issued` | `mp_potrol_tasks.id=6676470908830544` | PASS |
| 进入执行中 | 同端点切换 `running`，打开 `enteringResultList` | 页面显示 `patrolTask_20260717_006` 为“执行中” | PASS |
| 加载巡检明细 | `POST .../data-dg1584600022503` | `mp_task_details.id=6676470914171728` | PASS |
| 录入结果并完成 | `POST .../enteringResultEdit/submit` | 任务和明细同时更新 | PASS |
| 完成后复显 | 再次查询 `potrolTaskList` | 同一行显示“已完成” | PASS |
| 录入异常结果 | `enteringResultEdit` 保存 `99.99/异常` | `mp_task_details.id=6676867603743568` | PASS |
| 生成待治理隐患 | `abnormalSummary` 选择明细、点击按钮并确认 | `ses_hrm_riskhandles.id=6676868002956112`，明细 `fault_id` 同步 | PASS |
| 重复生成幂等 | 再次调用 `createHiddenDanger` | `createdCount=0/reusedCount=1`，风险计数仍为 1 | PASS |
| EAM 台账复显 | `riskRecord` DataGrid | 同一 `PATROL-RISK-*` 出现，来源显示“巡检” | PASS |

状态变更返回：

```json
{"code":200,"data":"SUCCESS","message":"操作成功"}
```

核心查库结果：

```text
id=6675852717278032
table_no=patrolTask_20260716_009
task_state=PATROL_taskState/cancelled
remark=ADP_E2E_20260716155413_PATROL_CANCELLED_BY_UI
version=1
valid=true
```

关联断言同时确认：

- `patrol_plan` 与 `patrol_plan_id` 都指向计划 `6675852707251024`。
- `work_route` 指向路线 `6675485506913104`。
- 明细 `valid=true/version=0/task_detail_state=PATROL_taskDetailState/pending`。
- 计划关联不一致数为 0，明细生命周期空值数为 0。
- 5 张阶段截图均生成；最终报告为
  `/tmp/adp-patrol-task-persistence-post-idempotency.json`。

## 现场执行与完成（2026-07-17）

机器记录：`metadata/patrol-execution-persistence-acceptance.json`；可重放命令：

```bash
make acceptance-patrol-execution-persistence \
  ADP_BASE_URL=http://10.11.100.17:18080 \
  ADP_SSH_HOST=10.11.100.17
```

真实页面使用 marker `ADP_E2E_20260716210839_PATROL_EXECUTION`，保存请求为：

```text
POST /msService/PATROL/patrolTask/potrolTask/enteringResultEdit/submit?id=6676470908830544
HTTP 200, code=200, success=true, taskState=completed
```

PostgreSQL 直接查询确认：

```text
task.id=6676470908830544
task.table_no=patrolTask_20260717_006
task.task_state=PATROL_taskState/completed
task.actual_start_time=2026-07-17 05:08:46
task.actual_end_time=2026-07-17 05:08:47.501
task.complete_staff=1
task.version=2
detail.id=6676470914171728
detail.concluse=12.34
detail.real_value=PATROL_realValue/normal
detail.complete_user=1
detail.complete_date=2026-07-17 05:08:47.501
detail.version=1
```

结果编辑页此前请求到空 `body.js`，导致自动判定函数缺失。仓库现已恢复原产品判定语义，并补上空值、
数字范围、全角/半角比较符、OR 规则、字符相等判断及空结果拒绝回归；目标 Nginx 以精确静态路由返回该脚本。
浏览器 5 张阶段截图均生成，32/32 断言通过，console/page/request/screenshot failure 都为 0。

测试脚本保留中断轮次的审计：前两次未完成任务使用真实 `taskStateUpdate` 取消，后三次业务已完成但因
自动化时序/旧控件错误判为失败，其任务作为审计记录保留，没有直接删库。最终通过轮次将 SupSelect 的
500ms 延迟布局纳入真实交互等待，且没有屏蔽任何浏览器错误。随后取消动作 marker
`ADP_E2E_20260716210929_PATROL` 再次回归 PASS。

## 异常生成隐患与 EAM 台账（2026-07-17）

机器记录：`metadata/patrol-hidden-danger-persistence-acceptance.json`；可重放命令：

```bash
make acceptance-patrol-hidden-danger-persistence \
  ADP_BASE_URL=http://10.11.100.17:18080 \
  ADP_SSH_HOST=10.11.100.17
```

真实页面使用 marker `ADP_E2E_20260717003024_PATROL_HIDDEN_DANGER`。用户在异常汇总表格真实勾选
明细 `6676867603743568`，点击“生成隐患”并在 Ant Modal 中点击“确认”；浏览器捕获请求：

```text
POST /msService/PATROL/patrolTask/taskDetail/createHiddenDanger
ids=6676867603743568,
HTTP 200, createdCount=1, riskId=6676868002956112
```

PostgreSQL 直接查询确认：

```text
detail.is_fault=true
detail.fault_id=6676868002956112
detail.fault_table_no=PATROL-RISK-6676868002956112
risk.status=1
risk.valid=1
risk.version=0
risk.risk_mode=PATROL_COMPATIBILITY_PENDING
risk.risk_source=SESHRM_riskResource/005
risk.finder=1
```

第二次请求返回 `createdCount=0/reusedCount=1`，数据库仍只有一条同 ID 风险记录。随后真实 EAM 隐患记录页
`POST .../data-dg1578550214154` 返回 HTTP 200，表格出现同一单号、发现人、时间和内容，隐患来源通过系统编码和
i18n 显示为“巡检”。本轮 45/45 断言、8 张截图、console/page/request/screenshot failure 全部通过。

兼容实现保留原有 SESH Feign 路径：未来真实 SESH 服务上传并发布后仍走原产品链；当前 SESH 缺失时才创建
可审计的 EAM 待治理记录。因此本项是异常发现和移交闭环，不伪装成完整隐患治理闭环。

## 已恢复前置数据

| 类型 | ID | Marker / 内容 | 状态 |
|---|---:|---|---|
| 路线 | `6675485506913104` | `ADP_E2E_202607162045_PATROL_ROUTE_UPDATED` | PASS |
| 区域 | `6675531420975952` | `ADP_E2E_202607162112_PATROL_AREA` | PASS |
| 巡检项目 | `6675549930308432` | `ADP_E2E_202607162116_PATROL_ITEM` | PASS |

## 剩余验收

| 功能 | 当前状态 | 下一步 |
|---|---|---|
| 输入标准新增/编辑/删除 | PASS | 保持真实页面和 PostgreSQL 回归 |
| 路线/区域/项目完整 CRUD | PASS | 保持行操作、CRUD、关联和软删除回归 |
| 任务下发/执行/完成 | PASS | 保持真实状态机和取消分支回归 |
| 执行结果 | PASS | 保持任务/明细同 marker 落库和页面复显回归 |
| 异常生成隐患与 EAM 复显 | PASS | 保持异常 marker、幂等、明细关联、来源翻译和页面复显回归 |
| 隐患整改/复查/销项 | BLOCKED_SESH_NOT_INSTALLED | 若纳入目标，取得真实 SESH 包并验收完整状态机；不得用待治理兼容记录冒充 |
| 统计和监控 | PENDING | 逐页清理 SQL/显示错误并核对聚合值 |
| 目标机回滚演练 | REQUIRES_CONFIRMATION | 维护窗口内禁用、验证、重新应用 PATROL 迁移；共享 EAM/SESH 兼容数据不做破坏性删除 |

EamMs 日志仍有 `getDataGridsByViewCode找不到viewCode:PATROL_1.0.0_patrolPlan_createTaskEdit`
警告；本次任务生成、落库和复显不受影响，但应在后续元数据清理中确认该编辑视图是否本来就没有
DataGrid，避免保留无意义告警。

## EMS 边界

四个 EMS 源码包不包含在 PATROL 的可用结论内。它们直接依赖缺失的
`Indicator 6.0.4.0` api/core，且原包没有 PostgreSQL 初始化 SQL；在真实依赖和迁移补齐前继续保持
`BLOCKED_MISSING_INDICATOR`。
