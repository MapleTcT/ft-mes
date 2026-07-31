# WOM 工序执行记录交互与落库验收

## 验收范围

- 环境：`http://10.11.100.17:18080`
- 页面：`/msService/WOM/produceTask/processExelog/processExeLogList`
- 数据库：PostgreSQL
- 受控工序执行记录：`9007190231282109`
- 生产批号：`BPI-LINES0701-20260727-E22B71C8`
- 工序：糖化

“指令执行记录/工序执行记录”是由制造指令和工序执行产生的历史事实，不是主数据维护页，
因此本轮没有增加“新增”和“删除”。新增应从制造指令及执行流程产生；删除历史执行记录
会破坏报工、质量、产耗和批次追溯链。页面恢复以下三个合理动作：

1. `查看详情`：查看工序边界、上下游交接以及流量/波美值证据。
2. `产耗查看`：查看投入、产出和消耗记录。
3. `工艺统计`：仅对启用参数统计、状态已完成且实际起止时间完整的工序生成统计快照。

结束工序的 `产耗录入` 和 `释放工作单元` 仍保持隐藏，避免从历史记录页修改执行事实。

## 前端验收

| 操作 | 预期 | 实际结果 | 状态 |
|---|---|---|---|
| 打开工序执行记录 | 三个动作可见，历史写动作隐藏 | `查看详情`、`产耗查看`、`工艺统计` 可见；`产耗录入` 不可见 | PASS |
| 未选择记录点击查看详情 | 给出可读提示，不打开空页 | 提示“请选择一条工序执行记录”，新页面数未增加 | PASS |
| 选择糖化记录并查看详情 | 打开真实工序详情 | HTTP 200；显示喷射到糖化的 12 秒连续交接、4 个流量/波美值测点及各 10 个样本 | PASS |
| 选择糖化记录并查看产耗 | 打开完整产耗页面 | HTTP 200；投入、产出、消耗三个页签均显示，加载结束 | PASS |
| 选择糖化记录并执行工艺统计 | 返回成功提示 | GET 返回 HTTP 200/code 200，提示“工艺参数统计完成” | PASS |

浏览器 `console error/warning`、`page error`、`request failure` 和 HTTP 4xx/5xx
均为 0。截图：

- `metadata/wom-process-execution-actions-list-20260731.png`
- `metadata/wom-process-execution-actions-detail-20260731.png`
- `metadata/wom-process-execution-actions-consumption-20260731.png`
- `metadata/wom-process-execution-actions-statistics-20260731.png`

## 后端与落库验收

调用链：

`ProcessAnalysisController.analyzeProcess`
-> `TraceabilityService.analyzeProcess/snapshot`
-> `ProcessAnalysisRepository.findProcessExecution/upsertSnapshot`
-> PostgreSQL `pa_trace_snapshots`

请求：

```text
GET /msService/ProcessAnalysis/paramStatRec/paramStatRec/manualStatProcess?processId=9007190231282109
```

验收 SQL：

```sql
SELECT id,
       tenant_id,
       source_type,
       source_id,
       task_id,
       batch_no,
       source_state,
       revision,
       updated_at
FROM public.pa_trace_snapshots
WHERE tenant_id = 'dt'
  AND source_type = 'PROCESS'
  AND source_id = 9007190231282109
ORDER BY revision;
```

实际结果：快照 `id=176`，`source_type=PROCESS`，`source_state=WOM_runState/finished`，
`revision` 从 `4` 增加到 `5`，证明接口成功后真实更新 PostgreSQL，而不是只返回 HTTP
200。验收期间临时把受控工序的 `need_param_ana` 设为 `true`，脚本结束后恢复为原值
`false`。

## 自动复验

```bash
make acceptance-wom-process-execution-actions \
  ADP_BASE_URL=http://10.11.100.17:18080 \
  ADP_BROWSER_BASE_URL=http://10.11.100.17:18080 \
  ADP_SSH_HOST=10.11.100.17 \
  ADP_PAGE_TIMEOUT_MS=120000
```

机器记录：
`metadata/wom-process-execution-actions-acceptance-20260731.json`。
