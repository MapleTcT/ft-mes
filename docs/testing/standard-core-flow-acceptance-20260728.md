# MES 标准核心链路验收与冻结建议

## 1. 结论

- 验收时间：2026-07-28
- 测试环境：`http://10.11.100.17:18080`
- 数据库：PostgreSQL
- 代码基线：`dc56ed8f32bbae0cef472ce5ea7383a7f81c0e3c`
- 场景 marker：`STD_CORE_20260728_194300`
- 总结：18 项，15 PASS、1 FAIL、1 BLOCKED、1 NOT_APPLICABLE
- 总体判定：`CONDITIONAL_PASS`

制造指令、投料/报工、请检、合格/不合格处置、完工入库和批次追溯已经在真实页面、
业务 API 和 PostgreSQL 上闭合。BPI 也已通过受控 MQTT 消息走完
JetLinks -> Kafka -> Flink -> PostgreSQL -> 候选确认 -> 影子批次关闭，但当前批次仍是
`stage=UNASSIGNED`、`material=null`、`quantity=0`。因此可以冻结 MES 手工执行核心版，
暂时不能把 BPI 自动批次版标记为正式生产就绪。

## 2. 验收总表

| 模块 | 页面/路由 | 操作 | API | 前端结果 | 后端结果 | 数据库表 | 验收状态 | 问题 |
|---|---|---|---|---|---|---|---|---|
| 登录与导航 | `/`、制造指令菜单、BPI 菜单 | 登录并进入核心页面 | 登录、当前用户菜单 | 页面可达，目标操作无 console/page/network 错误 | 会话和菜单正常 | 不适用 | PASS | 未做本轮权限矩阵扩展 |
| 制造指令 | `/msService/WOM/produceTask/produceTask/makeTaskList`、`/manual-entry/page` | 新建、幂等重放、重复批号拦截、生效、受控删除 | `POST /msService/WOM/produceTask/manual-entry/create` 等 | 真实表单和待办页可操作；移动端无横向溢出；9/9 PASS | 指令、请求幂等、待办和流程记录一致 | `wom_manual_task_requests`、`wom_produce_tasks`、`wfm_task_pending`、`wf_deal_info` | PASS | 无 |
| 指令详情 | `makeTaskEdit?id=9007185760124474` | 查看工序活动、用料汇总、检验清单 | 指令 data 和三个明细 datagrid | 逐条选择工序后显示泵送、换热、闪蒸、保温；三种物料和最终质量标准可见 | 2 工序、3 物料、4 设备活动、最终检验配置已持久化 | WOM 工序、活动、物料和质量配置表 | PASS | 设备活动按工序选择后显示，属于主从表交互 |
| 任务开始 | `makeTaskList` | 选择指令并点击开始 | `POST .../updateTaskState` | 状态变为执行中，浏览器错误为 0 | 任务、等待记录、执行日志进入运行态 | WOM 任务、等待、报工和执行日志表 | PASS | 无 |
| 工序开始 | `makeTaskBatchView?id=9007185760124474` | 开始喷射液化工序 | `POST .../taskProcess/start/{id}` | 工序状态刷新为执行中 | 工序执行、报工头和等待记录落库 | `wom_task_processes`、`wom_process_exelogs`、`wom_proc_reports`、`wom_wait_put_records` | PASS | 无 |
| 投料 | 同上 | 启动投料活动并保存批次投料 | `POST .../remainMaterialView/save` | 投料页面保存成功，错误数组为 0 | 投料明细和消耗记录各 1 条 | `wom_putin_details`、`wom_mat_consum_recods`、活动执行表 | PASS | 无 |
| 报工/产出 | `easyTaskOperateView?id=9007185760124474` | 结束产出活动并登记产出 | `POST .../endEasyActive/{id}` 等 | 页面状态和产出数据刷新正常 | 产出明细、物料产出记录各 2 条 | `wom_output_details`、`wom_mat_outpt_records`、活动执行表 | PASS | 无 |
| 合格请检 | QCS 制造检验列表、报告编辑/只读页 | 请检、生成报告、保存、生效 | `bulkSubmit`、`batchDealReports` | 页面复显合格，浏览器错误为 0 | 报告生效，WOM 与批次回写合格/可用 | QCS 检验/报告/明细、WOM、`baseset_batch_infos` | PASS | 无 |
| 不合格处置 | 同上 | 将独立测试批判为不合格并生效 | 同上 | 页面复显不合格，浏览器错误为 0 | 处理单状态 88；批次不可用；取证后夹具清理 | 上述表及 `qcs_un_qlf_deals` | PASS | 处置单后续人工结案分支未纳入本次主链 |
| 工序结束/任务完工 | 工序页、制造指令列表和完工报工弹窗 | 结束工序并完成指令 | `POST .../taskProcess/end/{id}`、`addOutputByOutPutDetails` | 工序和指令均显示已完成 | 任务完成数 35.1，实际起止时间完整，活动/工序无未完成项 | WOM 任务、工序、活动、执行和报工表 | PASS | 无 |
| 完工入库与放行 | `/msService/material/wms` | 完工入库、幂等重放、合格放行、领料扣减 | 完工入库、`checkProdResult`、生产领料接口 | 入库详情可打开；无 console/network 错误 | 10 待检 -> 10 可用 -> 领料 3 -> 余量 7；夹具清理 | `wms_stock_documents`、`wms_stock_document_lines`、`wms_inventory_transactions`、`wms_batch_stocks` | PASS | 无 |
| 不合格库存门禁 | 同上 | 入库后判不合格，再尝试领料 | 同上 | 页面可查看不合格库存 | 10 全部冻结、可用 0；领料返回业务 409，库存未变化 | WMS 单据、事务、质量和批次库存表 | PASS | 无 |
| 批次追溯 | `processBatchViewOut?batchNo=STD_CORE_20260728_194300_BATCH` | 查看生产、质量、入库和谱系 | `GET .../api/trace` | 追溯页、喷射和糖化执行详情均可打开；首/末工序边界文案正确 | 淀粉浆 -> 液化液 -> 糖化液两条谱系，QCS/WMS 一致 | WOM、QCS、WMS、`pa_trace_snapshots` | PASS | 无 |
| BPI 真实协议链 | `/bpi/#/candidates` | 发布 57 条 MQTT 事件，共 285 点 | JetLinks MQTT、Kafka/Flink、BPI 查询 | START/END 候选均从真实 BPI 页面确认 | 57 事件/285 点接收，0 reject；规则回放 matched=1/missed=0/falsePositive=0 | BPI telemetry、source sequence、候选表 | PASS | 来源是受控 MQTT 模拟器，不是现场 PLC |
| BPI 影子批次 | 同上、`/bpi/#/batches` | 确认 START 和 END 边界 | `POST /bpi-api/candidates/{id}/confirm` | 两个候选确认成功，页面截图留证 | 影子批次 `ACTIVE -> CLOSED_RAW/r2`，关联 WOM 单号，两条状态事件 | `bpi_batch_candidates`、`bpi_batch_instances`、`bpi_batch_state_events` | PASS | `END` 候选自身没有 orderId，但通过 batchId 关闭同一批次 |
| BPI 生产上下文 | BPI 批次详情 | 检查工序、物料、数量、单位 | BPI batch GET | 页面可读，但业务字段不完整 | `stage=UNASSIGNED`、`material=null`、`quantity=0` | `bpi_batch_instances` | FAIL | 尚不能自动生成可结算、可请检、可入库的真实 MES 批次 |
| 现场设备源 | 现场 PLC/DCS/仪表 | 校准后连续采集并验证断网、漂移、乱序 | 现场 MQTT/JetLinks | 未执行 | 未执行现场 24h/72h 影子运行 | 不适用 | BLOCKED | 需要点位表、设备连接、计量校准和现场窗口 |
| BPI 直写 QCS/WMS | BPI 影子批次 | 检查自动请检和自动入库副作用 | QCS/WMS adapter | 本轮影子模式不展示直写动作 | `quality_gate=NOT_APPLICABLE`、`wms_status=NOT_REQUESTED`，符合隔离设计 | 不适用 | NOT_APPLICABLE | 只有生产上下文和现场门禁通过后才允许启用 |

## 3. 保留场景

本轮保留了一个可在页面复核的果糖场景：

| 对象 | 标识/结果 |
|---|---|
| 制造指令 | `STD_CORE_20260728_194300_TASK_TN` |
| 生产批次 | `STD_CORE_20260728_194300_BATCH` |
| 工序 | 喷射液化 -> 12 秒交接 -> 糖化 |
| 中间品谱系 | 淀粉浆 34.4 t -> 液化液 34.8 t -> 糖化液 35.1 t |
| 设备活动 | 泵送、换热、闪蒸、保温；不单独创建物料 |
| 最终检验 | 波美值 20.6、pH 5.7、DE 96.4、干物 31.2，均合格 |
| 完工库存 | 糖化液 35.1 t，可用 35.1 t，冻结 0 |
| BPI 影子批次 | `BPI-LINES0701-20260728-24860E28`，`CLOSED_RAW/r2` |

数据来源边界：制造指令生命周期、投料、报工、请检、质量判定、完工入库和追溯由真实
页面/API 执行；为了把原单工序夹具扩成“喷射液化 + 糖化”，第二工序、三种物料谱系、
试运行质量标准和四项最终检验明细由带 marker 和所有权保护的受控 SQL 场景夹具建立，
随后再从 WOM/QCS/追溯页面和 PostgreSQL 复验。因此这部分证明“运行页面可读且关联正确”，
不等于配方、工艺路线和质量标准的全部主数据 CRUD 页面已经验收。

试运行质量标准带 `TEST_ONLY_DRAFT=true`。它用于证明产品链路，不替代公司受控 QA/QC
标准，也不代表数量已经通过正式物料平衡和计量审核。

## 4. PostgreSQL 终态复验

2026-07-28 22:24 在目标机直接复读：

```text
STD_CORE_20260728_194300_TASK_TN | finished | 35.100000 | 已检 | 合格
STD_CORE_20260728_194300_STARCH_SLURRY_BATCH | qualified | available
STD_CORE_20260728_194300_LIQUEFIED_SYRUP_BATCH | qualified | available
STD_CORE_20260728_194300_BATCH | qualified | available
BPI-LINES0701-20260728-24860E28 | CLOSED_RAW | UNASSIGNED | material=null | quantity=0
```

BPI 批次 `75a20cb3-a840-58b3-88dd-c7ce9b513786` 有两条状态事件：
`SHADOW_BATCH_CREATED: null -> ACTIVE` 和
`END_BOUNDARY_CONFIRMED: ACTIVE -> CLOSED_RAW`。

## 5. 标准产品冻结范围

建议第一版只保留：

1. 登录、组织、RBAC、系统编码和平台运行依赖。
2. 物料、批次、计量单位、工厂/产线、配方和质量标准主数据。
3. WOM 制造指令、工序执行、投料、报工、完工。
4. QCS 请检、报告、合格/不合格判定和不合格处置入口。
5. 物料 WMS 完工入库、质量冻结/放行、领料门禁。
6. ProcessAnalysis 批次谱系和工序执行详情。
7. BPI adapter/service、JetLinks、Kafka、Flink、PostgreSQL 的最小影子链。

建议延后：设备全生命周期、能源、安环、巡检、OEE、作业票、高级导入导出、BPI
数据集/训练/模型管理和非核心外部集成。延后是产品范围决策，不是删除源码。

## 6. 冻结门槛与周期建议

| 门槛 | 当前状态 | 冻结前动作 |
|---|---|---|
| MES 核心正常链 | PASS | 将本场景纳入标准分支回归 |
| 合格/不合格分支 | PASS | 补处置单最终结案权限和页面复验 |
| BPI 业务上下文 | FAIL | 把 WOM 工序、物料、单位和计划数量投影到 BPI，并用累计流量与波美值计算交接量 |
| 现场数据源 | BLOCKED | 选 1 条线完成点位、校准、时钟、乱序/断网和 24h/72h 影子运行 |
| 异常与恢复 | PARTIAL | 补服务重启、网络中断、传感器漂移、返工、合批/分批和回滚演练 |
| 运维发布 | NOT_TESTED_THIS_ROUND | 完成备份恢复、升级回退、监控告警和权限审计 |

按精简范围估算：MES 手工核心标准候选还需约 5-8 个有效工作日用于范围裁剪、重复回归、
安装/回退手册和权限复验；包含 BPI 自动批次的标准候选还需额外 10-15 个有效工作日，
且前提是现场设备、点位和联调窗口按时提供。这里是工程净时，不是承诺上线日期。

## 7. 证据与复验入口

- 机器记录：`metadata/standard-core-flow-acceptance-20260728.json`
- 全流程：`/tmp/std-core-20260728-194300/full-flow.json`
- 人工指令：`/tmp/std-core-20260728-194300/manual-entry/wom-manual-entry-results.json`
- 不合格质量：`/tmp/std-core-20260728-194300/qcs-unqualified/qcs-report-chain.json`
- WMS 合格/不合格：`/tmp/std-core-20260728-194300/wms-qualified-regression/acceptance.json`、
  `/tmp/std-core-20260728-194300/wms-unqualified/acceptance.json`
- 果糖试运行：`/tmp/std-core-20260728-194300/fructose-pilot.json`
- BPI 实时影子链：`/tmp/std-core-20260728-194300/bpi-live/`

`/tmp` 中保存原始请求、响应、SQL 摘要和截图，不提交临时运行包、数据库 dump 或大文件。
