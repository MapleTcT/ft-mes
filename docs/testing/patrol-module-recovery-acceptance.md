# PATROL 共享巡检模块恢复验收

- 验收日期：`2026-07-17`
- 原厂产品：`PATROL_1.0.0` / `6.0.4.0`
- 当前源码接入提交：`mes-modules-patrol-intake@0b8f37ebd13f3a9b72ae1a02cd59713e59fb68c0`
- 目标运行构件源码提交：`mes-modules-patrol-intake@0b8f37ebd13f3a9b72ae1a02cd59713e59fb68c0` + 当前 PostgreSQL 兼容补丁
- 主仓库报告验收基线：`a008baeb8055a8daea953141f4ea068272d80a68` + 当前工作树
- 承载服务：`EamMs`
- 目标环境：`v6@10.11.100.17`
- 默认数据库：PostgreSQL
- 当前结论：`TARGET_GATHER_RUNTIME_PASS_EXTERNAL_TAG_DATA_BLOCKED`

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
异常结果到 EAM 待治理隐患的生成链已经可用，8 个统计/监控页面已完成真实浏览器、API 和 PostgreSQL
聚合对账：7 个 PASS，采集误差分析因不存在启用的误差基准巡检项而为 1 个 NOT_APPLICABLE；23 项聚合
校验为 22 PASS、1 NOT_APPLICABLE。监控页当前验收的是路线/任务数据和明确的降级界面，不等于 GIS 定位与
轨迹回放已恢复。由于完整 SESH 与 SESGISConfig 模块没有交付，本次 `PATROL_COMPATIBILITY_PENDING` 记录不能
被解释为隐患整改、复查和销项治理流程已恢复，监控降级页也不能被解释为真实地图能力已恢复。

## 来源与构建

| 检查项 | 证据 | 结果 |
|---|---|---|
| 原始包 | `PATROL_6.0.4.0.zip` | PASS |
| 原包 SHA-256 | `1214f11302545d29ec2d611c49cb6bfe87aac3faf1344a94cb69eea9884b3394` | PASS |
| 完整性 | 1113 文件、455 Java、267 Web、9 SQL | PASS |
| PATROL core | `f56950e06b3bc4ac40d887287044d0f2824231384c3bb03bfb2070695e3f7293` | PASS |
| PATROL api | `ebff69b088e31d17aef534584ab33750ab9a2b96e24b001653ecb655c0fc4be1` | PASS |
| PATROL service | `e2cb13cbdf8a10bd8d0335b78a6fa12b9d22dca513c81079da73164e514f939a` | PASS |

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
| 部署 SHA-256 | `78cac278ccaf69fd62fccd7704d4b40cbac9cc5d8398a1f1d9d5493d22a47979` |
| 基线 JAR SHA-256 | `b44e4c9c4b79f23621f58d2a51a28450d9cc4225fb4f19586283fc228d49da38` |
| EamMs | 容器运行，PATROL 请求真实命中 |
| PostgreSQL | 容器 healthy |
| Nginx | 配置 `nginx -t` PASS，兼容规则已热加载 |
| 回滚 JAR | `/data/docker/adp-patrol-gather-20260717-1025/EamMs-1.0.0.pre-simlogin-7c0ebd.jar`，SHA-256 `7c0ebd70...d7284` |
| 冷启动 | `404.544s` 后完成 `Started EamMsApplication`，PATROL Kafka 分区重新分配 |
| 结果录入脚本 | `enteringResultEdit/body.js`、`body-es5.js`，目标与仓库 SHA-256 均为 `5c2f6653...b4a360`；空/null 结果不会再被转换为数值 0 |

前端兼容规则只处理旧框架的两个无效错误日志：

- 国际化 API 在参数不是数组时完成空 Promise，不再产生悬挂 Promise。
- 系统编码组件重复移除已不存在选项时保持原有 no-op 语义，不再误报 console error。

PATROL 六种任务状态 `未下发/已下发/执行中/已超期/已完成/已取消` 已由真实系统编码 API 返回，
没有用前端降噪掩盖缺失字典。

构建阶段曾有一个未进入最终候选的手工 `zip -u` 包把 Spring Boot 嵌套 JAR 压缩，EamMs 随即重启失败；
已按哈希回滚到 `99e5c346...ad0aa` 并恢复。最终包改由受控脚本生成，强制检查外层/内层 CRC、重复条目和
嵌套 JAR 的 `ZIP_STORED`，再从已启动的 `7c0ebd70...d7284` 基线生成。该过程没有修改 PostgreSQL 业务数据。

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

## 统计、异常概览与运行监控（2026-07-17）

机器记录：`metadata/patrol-report-acceptance.json`；可重放命令：

```bash
make acceptance-patrol-report \
  ADP_BASE_URL=http://10.11.100.17:18080 \
  ADP_SSH_HOST=10.11.100.17 \
  ADP_PAGE_TIMEOUT_MS=90000
```

脚本先登录真实前端、逐页点击真实“查询”，记录请求/响应和浏览器错误，再通过 SSH 直接查询 PostgreSQL
核对同一统计口径。页面标题已全部翻译，8 个页面均无 console、page、network 或 request error：

| 页面 | 关键 API / 数据 | 结果 |
|---|---|---|
| 巡检结果汇总 | `potrolResultSummary-query`，API/数据库均为 16 | PASS |
| 异常巡检结果汇总 | `abnormalSummary-query`，API/数据库均为 12 | PASS |
| 巡检任务概述 | `data-dg1586331011160`，API/数据库均为 22 | PASS |
| 异常巡检情况概述 | 已移交 6、待处理 6、已关闭 0 | PASS |
| 巡检任务完成情况 | 总数 22、完成 16、未完成 6、完成率 0.7273、按时率 1 | PASS |
| 设备平稳率 | 样本 16、正常 4、异常 12 | PASS |
| 采集数据误差分析 | 已选择真实路线和区域；启用误差基准项 0、可分析明细 0 | NOT_APPLICABLE |
| 巡检监控地图 | 路线 1、任务 28、执行中 0、超期 0；降级边界可见 | PASS_WITH_FALLBACK |

本轮修复了三个实际阻断：兼容国际化脚本现在翻译 `document.title` 并监听整个 document；Nginx 为旧页面的
`assets/images/comp_icon.png` 提供精确别名；后端异常处理统计把 `PATROL_COMPATIBILITY_PENDING` 识别为待处理，
不再把 6 条已移交风险静默漏掉。修复后 `getFaultDealStatus` 返回 `dealing=0/pending=6/complete=0`，与
`ses_hrm_riskhandles` 直接查询一致。

采集误差分析不是接口失败：当前库没有任何 `is_error_bench=true` 的启用巡检项，也没有同时具备
`gather_data` 和人工结论的可分析明细，因此该页不能造数冒充 PASS。监控页已证明路线/任务聚合和降级提示可用；
真实地图定位与轨迹回放继续由缺失的 `SESGISConfig` 服务包阻断。

## Kafka 采集与测点计算链（2026-07-17）

机器记录：`metadata/patrol-gather-data-runtime-acceptance.json`；可重放命令：

```bash
make acceptance-patrol-gather-data \
  ADP_SSH_HOST=10.11.100.17 \
  PATROL_GATHER_EXPECTED_EAM_SHA256=78cac278ccaf69fd62fccd7704d4b40cbac9cc5d8398a1f1d9d5493d22a47979
```

验收向真实 `topic.kafka.PATROL.gatherData` 写入 5 条带唯一 marker 的消息，并读取同一消费者组、EamMs 日志和
PostgreSQL。消费前后 topic/consumer offset 为 `20 -> 25`，最终 lag 为 0；空数据、非法任务 ID、非法工作项列表和
非法工作项 ID 四类消息均被隔离，合法已完成任务继续调用 TagManagement：

| 检查项 | 证据 | 结果 |
|---|---|---|
| 运行构件 | 部署/期望 SHA-256 均为 `78cac278...a47979`，容器 `running`、restart 0 | PASS |
| Kafka 绑定 | `topic.kafka.PATROL.gatherData-0` 已分配，offset `20 -> 25`，lag 0 | PASS |
| 消息健壮性 | 4 类异常输入均有独立 guard 日志，未触发外层消费者失败 | PASS |
| 模拟登录 | 首轮出现公钥回退与 `authenticated user admin`；重放复用消费线程上下文且无登录失败 | PASS |
| 请求时间窗 | `2026-07-17 08:30:05` 到 `08:31:05`，使用 `HH` 24 小时制 | PASS |
| TagManagement | 请求真实测点 `TAG_ADP_E2E_202607162116_PATROL_ITEM`，返回 `result=false/values=[]` | PASS_WITH_EXTERNAL_DATA_BLOCKER |
| PostgreSQL | `tmm_tags` 总数/匹配数均为 0；`mp_task_details.id=6676867603743568` 身份不变、`gather_data` 仍为 null | BLOCKED_BY_EXTERNAL_DATA |

因此技术链结论是 `PASS_WITH_EXTERNAL_DATA_BLOCKER`：消费者、认证、模块发现、Feign 请求、异常隔离和待写入行定位均
已通过；中位数值不能落库的唯一真实原因是测试环境没有测点元数据/历史值。模拟登录失败日志现只记录响应字段名，
机器报告也明确声明 `tokenValuesCaptured=false`，不再把 JWT 写入可交接证据。

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
| 统计报表与异常概览 | PASS | 保持 7 个真实页面和 22 项 API/PostgreSQL 聚合对账回归 |
| 采集数据误差分析 | NOT_APPLICABLE_NO_ERROR_BENCHMARK_ITEM | 配置真实误差基准巡检项，产生非空 `gather_data` 后复验图表数值 |
| Kafka 采集与测点计算链 | PASS_WITH_EXTERNAL_DATA_BLOCKER | 接入至少一个启用且有历史值的真实 TagManagement 测点，复验中位数写入 `mp_task_details.gather_data` |
| 监控路线/任务降级视图 | PASS_WITH_FALLBACK | 保持路线/任务 API 与 PostgreSQL 聚合对账 |
| GIS 定位与轨迹回放 | BLOCKED_SESGISCONFIG_NOT_INSTALLED | 取得真实 SESGISConfig 服务包后恢复地图、定位和轨迹回放并做浏览器验收 |
| 目标机回滚演练 | REQUIRES_CONFIRMATION | 维护窗口内禁用、验证、重新应用 PATROL 迁移；共享 EAM/SESH 兼容数据不做破坏性删除 |

EamMs 日志仍有 `getDataGridsByViewCode找不到viewCode:PATROL_1.0.0_patrolPlan_createTaskEdit`
警告；本次任务生成、落库和复显不受影响，但应在后续元数据清理中确认该编辑视图是否本来就没有
DataGrid，避免保留无意义告警。冷启动期间异步 i18n 上传还会记录
`/bap-workspace/i18n_temp/EamMs.zip (No such file or directory)`；PATROL 随后正常注册并提供请求，该告警目前不阻断
功能，但应在部署目录规范化时处理。

## EMS 边界

四个 EMS 源码包不包含在 PATROL 的可用结论内。它们直接依赖缺失的
`Indicator 6.0.4.0` api/core，且原包没有 PostgreSQL 初始化 SQL；在真实依赖和迁移补齐前继续保持
`BLOCKED_MISSING_INDICATOR`。
