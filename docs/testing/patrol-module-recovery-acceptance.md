# PATROL 共享巡检模块恢复验收

- 验收日期：`2026-07-16`
- 原厂产品：`PATROL_1.0.0` / `6.0.4.0`
- 当前源码接入提交：`mes-modules-patrol-intake@0b8f37ebd13f3a9b72ae1a02cd59713e59fb68c0`
- 目标运行构件源码提交：`8971a8770452c2bb55261f330284cd3dd26824b1`
- 主仓库验收基线：`299b0c8dd3993a819a58bf589d7b39bcbbfea580`
- 承载服务：`EamMs`
- 目标环境：`v6@10.11.100.17`
- 默认数据库：PostgreSQL
- 当前结论：`TARGET_TASK_PERSISTENCE_PASS`

机器可读记录见 [PATROL 恢复验收](../../metadata/patrol-module-recovery-acceptance.json)；可重放脚本见
`deploy/docker/scripts/adp-patrol-task-persistence-acceptance.js`。

## 结论

PATROL 原厂源码、PostgreSQL 迁移、运行元数据、菜单权限、工作流和运行 JAR 已部署到测试环境。
真实浏览器已完成“新增计划 -> 生成任务 -> 普通查询 -> 批量取消 -> PostgreSQL 回读 -> 页面复显”
闭环。重新应用 178-185 后，marker `ADP_E2E_20260716155413_PATROL` 的 22 项断言全部 PASS，浏览器 console、page error 和
PATROL network failure 均为 0。

这证明共享巡检的计划/任务状态链在 PostgreSQL 上可用，也再次确认设备巡检、工艺巡检和安环巡检
共用同一 PATROL 数据域。它不等于整个巡检产品已经全部验收：输入标准 CRUD、路线/区域/项目全量
编辑、现场执行结果、异常处置和统计页面仍需继续逐页闭合。

## 来源与构建

| 检查项 | 证据 | 结果 |
|---|---|---|
| 原始包 | `PATROL_6.0.4.0.zip` | PASS |
| 原包 SHA-256 | `1214f11302545d29ec2d611c49cb6bfe87aac3faf1344a94cb69eea9884b3394` | PASS |
| 完整性 | 1113 文件、455 Java、267 Web、9 SQL | PASS |
| PATROL core | `7cb79a90f0fc781e9063a534a1825cb48fff753b077db142eccc43eecdf66461` | PASS |
| PATROL api | `718a0efc664e0f88ee0926616e540343f6c5e02f472ee1182a398ede02ef5a14` | PASS |
| PATROL service | `ecddc8350d281a6255b4d55e0b0c5039a7addcbfc2d3ace23853079ad752aecd` | PASS |

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
| 部署 SHA-256 | `97d3a265bdc1b6d6a3018a808dda765f65ccbab975decd715777e56d1e43d2ab` |
| 基线 JAR SHA-256 | `b44e4c9c4b79f23621f58d2a51a28450d9cc4225fb4f19586283fc228d49da38` |
| EamMs | 容器运行，PATROL 请求真实命中 |
| PostgreSQL | 容器 healthy |
| Nginx | 配置 `nginx -t` PASS，兼容规则已热加载 |
| 最近备份 | `/data/docker/adp-patrol-20260716/20260716233613-nginx-edit-list-fix` |

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

## 已恢复前置数据

| 类型 | ID | Marker / 内容 | 状态 |
|---|---:|---|---|
| 路线 | `6675485506913104` | `ADP_E2E_202607162045_PATROL_ROUTE_UPDATED` | PASS |
| 区域 | `6675531420975952` | `ADP_E2E_202607162112_PATROL_AREA` | PASS |
| 巡检项目 | `6675549930308432` | `ADP_E2E_202607162116_PATROL_ITEM` | PASS |

## 剩余验收

| 功能 | 当前状态 | 下一步 |
|---|---|---|
| 输入标准新增/编辑/删除 | PENDING | 真实页面 marker + `mp_input_standards` 回读 |
| 路线/区域/项目完整 CRUD | PARTIAL | 补编辑、删除、关联变化和页面复显 |
| 任务下发/执行/完成 | PENDING | 按原状态机走真实业务入口，不直接改库 |
| 执行结果和异常处置 | PENDING | 验证明细结果、异常记录、任务完成回写 |
| 统计和监控 | PENDING | 逐页清理 SQL/显示错误并核对聚合值 |
| 目标机回滚演练 | REQUIRES_CONFIRMATION | 维护窗口内禁用、验证、重新应用 178-185 |

EamMs 日志仍有 `getDataGridsByViewCode找不到viewCode:PATROL_1.0.0_patrolPlan_createTaskEdit`
警告；本次任务生成、落库和复显不受影响，但应在后续元数据清理中确认该编辑视图是否本来就没有
DataGrid，避免保留无意义告警。

## EMS 边界

四个 EMS 源码包不包含在 PATROL 的可用结论内。它们直接依赖缺失的
`Indicator 6.0.4.0` api/core，且原包没有 PostgreSQL 初始化 SQL；在真实依赖和迁移补齐前继续保持
`BLOCKED_MISSING_INDICATOR`。
