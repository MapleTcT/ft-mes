# 果糖喷射液化至糖化试运行线验收

## 结论

2026-07-28 在测试环境完成 marker `ADP_E2E_FRUCTOSE_LINE_01` 的真实
WOM、QCS、WMS、ProcessAnalysis、BPI 和 PostgreSQL 验收，状态为 `PASS`。

本场景全部主数据和检验限值均标记为 `TEST_ONLY_DRAFT`。它用于验证产品模型、接口和落库链，
不是公司正式工艺卡、质量标准、计量证书或现场投产批准。正式上下限必须由工艺、质量和生产负责人
共同审批后另行发布。

## 建模口径

路线代码为 `FRU-JET-SACCH-V1`，计量单位为吨。

| 顺序 | 工序 | 投入 | 产出 | 数量变化 | 是否形成工序批 | 是否形成物料批 | 判定理由 |
|---:|---|---|---|---|---|---|---|
| 1 | 喷射液化 | 淀粉浆批次 | 液化液批次 | 34.4 -> 34.8 t | 是 | 是 | 液化液进入缓冲罐，按流量、波美值和交接检查独立结算 |
| 2 | 糖化 | 液化液批次 | 糖化液罐批 | 34.8 -> 35.1 t | 是 | 是 | 糖化结束形成独立罐批，需要 QCS 检验和质量放行 |

物料谱系固定为：

```text
淀粉浆批次
  --喷射液化 34.4 -> 34.8 t-->
液化液批次
  --糖化 34.8 -> 35.1 t-->
糖化液批次
```

泵送、换热、闪蒸和保温只作为工序活动及参数载体，验收确认其
`material_id` 和 `material_batch_num` 均为空，没有错误创建物料或批次。

## 检验项目

### 淀粉浆交接

标准代码 `STARCH_SLURRY_HANDOVER_V1`，门禁为 `PROCESS_HANDOVER`。

| 项目 | 数据来源 | 试运行范围 | 测试值 | 结论 |
|---|---|---:|---:|---|
| 干物含量 | LAB | 24.0-26.0 % | 25.0 % | 合格 |
| pH | LAB | 5.5-6.5 | 5.9 | 合格 |

### 液化液交接

标准代码 `LIQUEFIED_SYRUP_HANDOVER_V1`，门禁为 `PROCESS_HANDOVER`。

| 项目 | 数据来源 | 试运行范围 | 测试值 | 结论 |
|---|---|---:|---:|---|
| 液化液波美值 | BPI + LAB | 19.5-20.8 Be | 20.1 Be | 合格 |
| 液化液 pH | LAB | 5.5-6.2 | 5.8 | 合格 |
| 液化 DE 值 | LAB | 12.0-20.0 % | 15.2 % | 合格 |

### 糖化液放行

标准代码 `SACCHARIFIED_LIQUOR_RELEASE_V1`，门禁为 `QCS_RELEASE`。

| 项目 | 数据来源 | 试运行范围 | QCS 报告值 | 结论 |
|---|---|---:|---:|---|
| 糖化液波美值 | BPI + LAB | 20.0-21.0 Be | 20.6 Be | 合格 |
| 糖化液 pH | BPI + LAB | 5.4-6.2 | 5.7 | 合格 |
| 糖化 DE 值 | LAB | 95.0-98.0 % | 96.4 % | 合格 |
| 糖化液干物含量 | LAB | 28.0-35.0 % | 31.2 % | 合格 |

淀粉浆和液化液的 5 项结果作为 WOM 工序交接检查落库；糖化液 4 项结果进入真实 QCS
检验报告并触发 WMS 质量放行。这样既能验证工序内交接，也不会把每个设备动作都建成独立请检单。

## 真实业务链

| 阶段 | 前端入口或 API | 后端链路 | PostgreSQL 事实 | 结果 |
|---|---|---|---|---|
| 制造指令 | WOM 制造指令详情 | WOM task/report/execution services | `wom_produce_tasks`、`wom_produce_task_exelog` | 35.1 t，finished，已检/合格 |
| 喷射液化 | ProcessAnalysis 工序详情 | ProcessAnalysis controller -> service -> repository | `wom_task_processes`、`wom_process_exelogs` | `16:00:20..16:00:44`，finished |
| 工序交接 | 喷射与糖化详情 | ProcessAnalysis boundary read model | 同上 | 间隔 12 秒，`CONTIGUOUS` |
| 糖化 | ProcessAnalysis 工序详情 | ProcessAnalysis controller -> service -> repository | `wom_task_processes`、`wom_process_exelogs` | `16:00:56..16:01:32`，finished |
| 物料投入/产出 | WOM 工序活动 | WOM detail/record persistence | `wom_putin_details`、`wom_output_details`、`wom_mat_consum_recods`、`wom_mat_outpt_records` | 两道工序数量链闭合 |
| 工序交接检验 | WOM 检验清单 | WOM process-report checks | `wom_check_details` | 5 项合格 |
| 糖化液检验 | QCS 产品检验报告 | QCS inspect/report services | `qcs_inspects`、`qcs_inspect_reports`、`qcs_report_coms` | 4 项合格 |
| 完工入库 | material/WMS completion inbound | material WMS service -> repositories | `wms_stock_documents`、`wms_inventory_transactions`、`wms_batch_stocks` | 1 单、2 流水、35.1 t 可用 |
| 批次追溯 | ProcessAnalysis 批次追溯页 | trace controller -> service -> repository | WOM/QCS/WMS 事实表、`pa_trace_snapshots` | 2 条工序谱系、5 个快照 |
| 工艺信号 | 两张工序详情页 | BPI process-evidence read model | `bpi_telemetry_events`、`bpi_telemetry_points` | 24 events、288 points、12 properties |

WMS 两条库存流水为：

1. `COMPLETION_INBOUND`：在库 `+35.1`，待检冻结 `+35.1`。
2. `QUALITY_RELEASE`：可用 `+35.1`，冻结 `-35.1`。

最终库存为在库 `35.1`、可用 `35.1`、冻结 `0`。

## 页面验收

| 页面 | 路由 | 关键可见内容 | 浏览器结果 |
|---|---|---|---|
| WOM 制造指令 | `/msService/WOM/produceTask/produceTask/makeTaskEdit?id=9007185016254254` | 产品、批号、吨、工序活动、用料汇总、检验清单 | PASS |
| 喷射液化详情 | `/msService/ProcessAnalysis/processAnalysis/processExecution/detail?processExecutionId=770643268805888` | 喷射液化、糖化、12 秒、流量和波美值曲线 | PASS |
| 糖化详情 | `/msService/ProcessAnalysis/processAnalysis/processExecution/detail?processExecutionId=9007185016264260` | 上一工序、糖化、流量和波美值曲线 | PASS |
| QCS 报告 | `/msService/QCS/inspectReport/inspectReport/manuInspReportView?id=770643536692480` | 吨、4 个检验项目、报告值和合格结论 | PASS |
| 批次追溯 | `/msService/ProcessAnalysis/processAnalysis/exelogSecond/processBatchViewOut?...` | 两条工序谱系、质量闭环、入库和质量放行流水 | PASS |

5 个页面的 `consoleErrors`、`pageErrors`、HTTP 4xx/5xx 和
`requestFailures` 均为 0。

## BPI 信号证据

受信上下文为 `1000/PLANT-01/LINE-S07-01`，窗口为
`2026-07-27 16:00:00..16:01:32+08:00`。

| propertyId | 样本数 | 最小值 | 平均值 | 最大值 |
|---|---:|---:|---:|---:|
| `jet.feed.baume` | 24 | 19.8 | 20.119 | 20.25 |
| `jet.flow.instant` | 24 | 0 | 6.263 | 19.0 |
| `saccharification.inlet.flow.instant` | 24 | 0 | 5.575 | 16.8 |
| `saccharification.liquor.baume` | 24 | 20.2 | 20.375 | 20.7 |

总计 24 个 accepted event、288 个 accepted/stored point、12 个属性，
point reject 和 quarantine 均为 0。

## PostgreSQL 复验

```sql
select id, table_no, produce_batch_num, plan_num, finish_num,
       task_run_state, check_state, check_result
from public.wom_produce_tasks
where id = 9007185016254254;

select name, exe_order, act_start_time, act_end_time, process_run_state
from public.wom_process_exelogs
where task_id = 9007185016254254
order by exe_order;

select m.code, m.name, u.name as main_unit, pu.name as produce_unit
from public.baseset_materials m
left join public.baseset_units u on u.id = m.main_unit
left join public.baseset_units pu on pu.id = m.produce_unit
where m.code like 'ADP_E2E_FRUCTOSE_LINE_01_%';

select transaction_type, material_code, batch_no,
       on_hand_delta, available_delta, hold_delta
from public.wms_inventory_transactions
where tenant_id = '1000'
  and source_document_id = 'ADP_E2E_FRUCTOSE_LINE_01_WMS_IN'
order by id;
```

完整 SQL、API 请求摘要和浏览器断言保存在
`metadata/fructose-line-pilot-acceptance.json`。

## 重跑

该验收会保留并幂等更新 marker 数据，且只清理自身 WMS 单据、流水、库存和追溯快照。
必须显式确认：

```bash
export ADP_PASSWORD='<test-account-password>'
make acceptance-fructose-line-pilot \
  FRUCTOSE_PILOT_CONFIRM=YES \
  ADP_BASE_URL=http://10.11.100.17:18080 \
  ADP_BROWSER_BASE_URL=http://10.11.100.17:18080 \
  ADP_SSH_HOST=10.11.100.17
```

输入模型位于
`simulation/bpi/scenarios/fructose-jet-saccharification-v1.json`，自动验收入口位于
`deploy/docker/scripts/adp-fructose-line-pilot-acceptance.js`。

## 尚未证明

- 测试信号来自受控模拟，不是物理仪表或 PLC/DCS 实时采集。
- 数量变化只验证系统链路，不代表经正式物料平衡审核。
- 检验范围是试运行草案，未替代公司 QA/QC 受控标准。
- 未完成现场计量校准、连续生产、异常批、返工/回配/合批、跨班交接和生产回滚演练。
- 本次 `PASS` 只代表该受控测试场景可重复闭合，不代表系统已获正式生产批准。
