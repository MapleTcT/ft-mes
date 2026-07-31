# WTS 业务管理与统计分析动作验收

## 结论

2026-07-31 在测试环境 `http://10.11.100.17:18080` 使用 admin 真实会话，完成
“作业管理 -> 业务管理/统计分析”11 个菜单页面的动作恢复和浏览器回归。

- 11 个真实页面和 24 个 list/edit/view 运行时布局，共 35 项检查全部 PASS。
- 5 个可直接创建的作业类型均显示“新增”，点击后打开对应的非空编辑表单。
- 8 个作业类型均显示“查看详情”；动火和作业台账使用现有记录打开详情成功。
- 作业台账、盲板台账和作业统计的“导出”均触发真实下载。
- 动火“批量打印”已恢复中文标签和未选择数据校验；实际打印受测试环境本机打印控件
  授权阻断，不能标记为实际打印 PASS。
- 浏览器 console error、page error、request failed、HTTP 4xx/5xx 均为 0。

## 根因与修复

1. 业务菜单和 admin 权限已经存在，故障不属于角色授权缺失；目标页面的运行时布局和
   发布动作不完整，导致工具栏为空。
2. 编辑页依赖的子表格、模型和字段元数据未完整恢复，直接打开新增页会出现空白区域。
3. 作业台账导出缺少精确的 datagrid 和查询绑定，列表可读但导出会失败。
4. 作业统计来自旧的独立静态页面，需要补充兼容导出脚本。
5. 旧配置服务会合并原生动火批量打印动作，并把中文标签覆盖成
   `ec.print.batchPrint`；本轮关闭冲突注入，保留原打印操作码和端点，通过独立动作调用。

主要固化资产：

- `deploy/docker/scripts/generate-business-view-runtime-sql.py`
- `deploy/docker/scripts/patch-wts-runtime-compat.py`
- `deploy/docker/scripts/adp-wts-business-statistics-actions-regression.js`
- `deploy/docker/postgres/init/248-wts-business-statistics-actions-runtime.sql`
- `deploy/docker/postgres/init/249-wts-business-statistics-actions-lob-compat.sql`
- `deploy/docker/postgres/init/250-wts-ticket-edit-subgrid-runtime.sql`
- `deploy/docker/postgres/init/251-wts-ledger-export-runtime-datagrid.sql`
- `deploy/docker/postgres/init/252-wts-ledger-export-query-runtime.sql`
- `deploy/docker/postgres/init/253-wts-certificate-part-entity.sql`
- `deploy/docker/postgres/init/254-wts-firework-batch-print-render-compat.sql`
- `deploy/docker/postgres/init/255-wts-firework-native-batch-print-collision.sql`

## 页面验收

| 页面 | 路由 | 可见动作 | 实际交互 | 状态 | 边界 |
|---|---|---|---|---|---|
| 动土安全作业 | `/msService/WTS/workTicket/workTicket/soilWork` | 新增、查看详情 | 新增表单打开；无选择提示正常 | PASS | 列表无记录，详情 BLOCKED |
| 受限空间安全作业 | `/msService/WTS/workTicket/workTicket/limitSpaceWork` | 新增、查看详情 | 新增表单打开；无选择提示正常 | PASS | 列表无记录，详情 BLOCKED |
| 吊装安全作业 | `/msService/WTS/workTicket/workTicket/liftWork` | 查看详情 | 无选择提示正常 | PASS | 列表无记录，详情 BLOCKED |
| 临时用电安全作业 | `/msService/WTS/workTicket/workTicket/electricityWork` | 新增、查看详情 | 新增表单打开；无选择提示正常 | PASS | 列表无记录，详情 BLOCKED |
| 高处安全作业 | `/msService/WTS/workTicket/workTicket/heightWork` | 新增、查看详情 | 新增表单打开；无选择提示正常 | PASS | 列表无记录，详情 BLOCKED |
| 动火安全作业 | `/msService/WTS/workTicket/workTicket/firework` | 查看详情、批量打印 | 现有记录详情打开；打印未选择校验正常 | PASS | 本机打印控件未授权，实际打印 BLOCKED |
| 断路安全作业 | `/msService/WTS/workTicket/workTicket/breakWork` | 新增、查看详情 | 新增表单打开；无选择提示正常 | PASS | 列表无记录，详情 BLOCKED |
| 盲板抽堵安全作业 | `/msService/WTS/workTicket/workTicket/blockWork` | 查看详情 | 无选择提示正常 | PASS | 列表无记录，详情 BLOCKED |
| 作业台账 | `/msService/WTS/workTicket/workTicket/workList` | 查看详情、导出 | 动火详情打开；下载 `.xls` 成功 | PASS | 只读动作 |
| 盲板台账 | `/msService/WTS/blindPlateAccount/plateAccount/plateAccountList` | 导出 | 下载 `.xls` 成功 | PASS | 只读动作 |
| 作业统计 | `/msService/WTS/workTicket/assWorkTickets/workTicket` | 导出 | 仪表盘渲染；下载 `.csv` 成功 | PASS | 只读动作 |

吊装和盲板抽堵在原包中没有发布独立“新增”动作，本轮未凭空增加写入口；相应作业票应由
许可/流程入口创建。删除、审批、封票等状态动作也不属于这些只读列表的原始动作栏。

## PostgreSQL 元数据复读

按 24 个目标 `view_code` 读取 `runtime_extra_view`，将 large-object OID 还原成 UTF-8
文本后检查布局是否存在且非空：

```text
expected=24|restored=24|min_bytes=14201|max_bytes=144383
```

动火列表额外复读：

```text
print_button=true|label_normalizer=true
ec=0
runtime=false
```

这表示兼容打印动作和标签修复已写入运行时布局，原生冲突注入已仅对该动火视图关闭。

## 验收边界

- 本轮修复和验收对象是页面动作入口、详情跳转、导出和打印前置校验。
- 本轮没有保存、修改或删除作业票业务单据，业务数据落库为 `NOT_APPLICABLE`。
- 7 个空列表需要准备受控作业票后再验收真实详情内容。
- 动火实际打印需要安装并授权标准打印控件后单独复验；当前不能称为打印功能已完全可用。
- 机器证据：`metadata/wts-business-statistics-runtime-acceptance-20260731.json`。
- 截图：`metadata/wts-business-statistics-20260731/`。
