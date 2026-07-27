# 果糖喷射到糖化工序边界验收

验收时间：2026-07-27
目标环境：`http://10.11.100.17:18080`
数据库：MES `adp`、BPI `ft_mes_bpi`，均为 PostgreSQL
代码基线：`4d41c2154e223c0aa629045cebea7ec4b0f924d4` 加本次工作树
marker：`BPI_MIN_20260727_110711`
生产批次：`BPI-LINES0701-20260727-E22B71C8`

## 验收结论

本轮针对制造指令编辑、生产线参照、工序执行详情和果糖双工序连续数据完成真实页面、
API 与 PostgreSQL 联合验收。喷射在 `16:00:20` 开始、`16:00:44` 结束，糖化在
`16:00:56` 开始、`16:01:32` 结束，工序交接间隔为 12 秒。BPI 在同一产线窗口内持久化
24 个事件、288 个采样点和 12 个属性，拒收与隔离均为 0。

所有本轮浏览器步骤的 `console error`、`page error` 和 `request failure` 均为 0。

## 前端功能验收

| 模块 | 页面/路由 | 操作步骤 | 预期结果 | 实际结果 | API | 状态 |
|---|---|---|---|---|---|---|
| 制造指令生产线参照 | WOM 制造指令编辑页，`factoryLineRef2` 弹窗 | 点击生产线名称右侧参照按钮 | 显示有效生产线，可查询和选择 | 弹窗显示 15 条生产线，节点类型均为生产线，不再出现 SQLGrammarException | WOM 生产线参照查询 | PASS |
| 制造指令业务明细 | WOM 制造指令编辑页 | 打开既有 marker 指令 | 显示工序、活动、用料和检验业务区 | `常规信息/工序活动/用料汇总/检验清单` 四页签均显示；工序区显示喷射和糖化 | WOM 制造指令详情及子表查询 | PASS |
| 工序执行列表 | `/msService/WOM/produceTask/processExelog/processExeLogList` | 查询并双击喷射记录 | 进入该记录的只读详情 | 列表显示喷射、糖化两行；双击喷射进入 ID `9007190231281109` 的详情页 | WOM 工序执行列表查询 | PASS |
| 工序执行详情 | `/msService/ProcessAnalysis/processAnalysis/processExecution/detail?processExecutionId=9007190231281109` | 查看概况、边界和信号曲线 | 显示当前/下一工序、交接时间和真实历史采样 | 显示喷射到糖化 12 秒连续交接，4 条关键序列和非空 Canvas 曲线 | `GET /processAnalysis/api/process-executions/{id}`；`GET /bpi-api/process-evidence` | PASS |
| 批次追溯 | 工序执行详情 | 点击“查看批次追溯” | 当前页进入同批次完整追溯 | 进入生产过程追溯页，加载 13 个时间轴事件、2 道工序、物料谱系和质量闭环 | `GET /processAnalysis/exelogSecond/processBatchViewOut` | PASS |

既有完成态制造指令的工序、用料和检验子表为只读，未显示新增按钮是状态约束，不是交互缺失。
新建/编辑态仍由配方和工艺路线生成这些明细，不能在完成态手工篡改。

## API 验收

| API | 请求要点 | 实际结果 | 状态 |
|---|---|---|---|
| `GET /bpi/v1/process-evidence` | tenant `1000`、plant `PLANT-01`、line `LINE-S07-01`、工序时间窗 | HTTP 200，返回 4 条筛选序列及真实样本/统计值 | PASS |
| `GET /bpi/v1/process-evidence` | 使用无权访问的产线 | HTTP 403，按令牌产线作用域拒绝 | PASS |
| `GET /bpi/v1/process-evidence` | 请求超过 24 小时的窗口 | HTTP 422，拒绝无界历史查询 | PASS |
| `GET /processAnalysis/api/process-executions/9007190231281109` | 喷射执行记录 | HTTP 200，返回下一工序糖化、12 秒交接和 BPI 上下文 | PASS |

`orderId` 是查询上下文标签，历史采样仍严格按受信租户、工厂、产线和时间窗匹配，系统不会仅凭
`orderId` 推断任意遥测天然属于某一制造指令。

## PostgreSQL 落库证据

MES 查询：

```sql
SELECT id, table_no, produce_batch_num, task_run_state,
       act_start_time, act_end_time
FROM public.wom_produce_tasks
WHERE id = 9007190231280101;

SELECT id, name, exe_order, process_run_state,
       act_start_time, act_end_time
FROM public.wom_process_exelogs
WHERE task_id = 9007190231280101
  AND id IN (9007190231281109, 9007190231282109)
ORDER BY exe_order;

SELECT EXTRACT(EPOCH FROM (
    (SELECT act_start_time FROM public.wom_process_exelogs WHERE id = 9007190231282109)
    -
    (SELECT act_end_time FROM public.wom_process_exelogs WHERE id = 9007190231281109)
))::int AS handover_seconds;
```

实际结果：

```text
task=9007190231280101
taskState=WOM_runState/finished
taskWindow=2026-07-27 16:00:20..16:01:32
process[1]=喷射, finished, 16:00:20..16:00:44
process[2]=糖化, finished, 16:00:56..16:01:32
handoverSeconds=12
bpiContext=1000/PLANT-01/LINE-S07-01, enabled=true
productionLineCandidates=15
```

BPI 查询：

```sql
WITH scoped_events AS (
    SELECT *
    FROM bpi.bpi_telemetry_events
    WHERE tenant_id = '1000'
      AND plant_id = 'PLANT-01'
      AND line_id = 'LINE-S07-01'
      AND event_time BETWEEN
          timestamptz '2026-07-27 16:00:00+08'
          AND timestamptz '2026-07-27 16:01:32+08'
), scoped_points AS (
    SELECT point.*
    FROM bpi.bpi_telemetry_points point
    JOIN scoped_events event
      ON event.id = point.telemetry_event_id
     AND event.tenant_id = point.tenant_id
)
SELECT
    (SELECT count(*) FROM scoped_events) AS events,
    (SELECT sum(accepted_point_count) FROM scoped_events) AS accepted_points,
    (SELECT count(*) FROM scoped_points) AS stored_points,
    (SELECT count(DISTINCT property_id) FROM scoped_points) AS properties;
```

实际结果：

```text
events=24
acceptedEvents=24
acceptedPoints=288
storedPoints=288
properties=12
pointRejects=0
quarantine=0
firstEvent=2026-07-27T16:00:00+08:00
lastEvent=2026-07-27T16:01:32+08:00
```

关键序列：

| propertyId | 样本数 | 最小值 | 平均值 | 最大值 |
|---|---:|---:|---:|---:|
| `jet.feed.baume` | 24 | 19.8 | 20.119 | 20.25 |
| `jet.flow.instant` | 24 | 0 | 6.263 | 19.0 |
| `saccharification.inlet.flow.instant` | 24 | 0 | 5.575 | 16.8 |
| `saccharification.liquor.baume` | 24 | 20.2 | 20.375 | 20.7 |

## 场景与安全边界

模拟器按 4 秒一帧发送 12 个点位，即每个点位每分钟 15 个样本，覆盖：

- 正常喷射到糖化交接；
- 短时流量跌落但未达到结束保持时间；
- 波美值偏离；
- 指令或阀路切换。

模拟器固定为 shadow-only，不写 WOM、QCS、WMS，也不控制 PLC/DCS。用于生产时仍必须由正式
拓扑、受控边界规则、点位校准、来源序列和人工影子验收共同放行。
