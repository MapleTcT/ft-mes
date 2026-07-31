# QCS 其他检验与质量巡检运行时验收

## 结论

2026-07-31 在测试环境 `http://10.11.100.17:18080` 使用 admin 真实会话，完成“其他检验”
和“质量巡检”5 个菜单页面的动作恢复与浏览器验收。15 个 list/edit/view 布局接口和 5 个
菜单页面共 20 项，结果为 20 PASS、0 FAIL、0 BLOCKED。

- 其他检验申请：新增申请、打开、关闭、批量提交、删除均可见。
- 质量巡检申请：新增申请、打开、关闭、批量提交、删除均可见。
- 其他检验报告、其他不合格品处理、质量巡检报告：恢复原包定义的删除动作。
- 两个“新增申请”均已真实点击，并分别打开正确的独立编辑页和完整表单。
- 浏览器 console error、page error、request failed、HTTP 5xx 均为 0。
- 页面不再显示 `ec.common.tableNo`、`ec.list.taskDescription` 或“默认操作”。

## 根因与修复

1. 菜单和 admin 权限已经存在，故障不属于角色授权缺失。
2. 目标 edit/view 的 `runtime_extra_view.view_json` 为空，list 页面则只保留了降级后的少量动作。
3. 原质量巡检列表脚本错误引用了其他检验数据表，直接复用会操作错误页面。
4. 恢复出的标准 ADD 动作没有对应的平台动作注册，按钮可见但点击无效。
5. 个别字段仍显示旧国际化键。

本轮通过生成器恢复 15 份原包布局，分别绑定其他检验和质量巡检的数据表；新增入口使用
显式运行时动作打开各自编辑页；同时补齐中文回退标签。固化文件如下：

- `deploy/docker/scripts/generate-business-view-runtime-sql.py`
- `deploy/docker/postgres/init/247-qcs-other-quality-inspection-runtime.sql`
- `deploy/docker/scripts/adp-qcs-other-quality-inspection-regression.js`
- `deploy/docker/scripts/test_generate_business_view_runtime_sql.py`

## 页面验收

| 页面 | 可见动作 | 新增/详情交互 | 布局接口 | 状态 |
|---|---|---|---:|---|
| 其他检验申请 | 新增申请、打开、关闭、批量提交、删除 | 新增打开 `/otherInspectEdit`，表单字段完整 | list/edit/view 均 200 | PASS |
| 其他检验报告 | 删除 | list/edit/view 布局完整 | 3/3 为 200 | PASS |
| 其他不合格品处理 | 删除 | list/edit/view 布局完整 | 3/3 为 200 | PASS |
| 质量巡检申请 | 新增申请、打开、关闭、批量提交、删除 | 新增打开 `/qualityInspectEdit`，表单字段完整 | list/edit/view 均 200 | PASS |
| 质量巡检报告 | 删除 | list/edit/view 布局完整 | 3/3 为 200 | PASS |

机器证据：`metadata/qcs-other-quality-inspection-acceptance-20260731.json`。
截图目录：`metadata/qcs-other-quality-inspection-20260731/`。

## PostgreSQL 复读

验收 SQL 按 15 个目标 `view_code` 查询 `runtime_extra_view`，将 large-object OID 还原为
UTF-8 文本后检查非空、长度及占位键：

```sql
with target(view_code) as (
  values
    ('QCS_5.0.0.0_inspect_otherInspectList'),
    ('QCS_5.0.0.0_inspect_otherInspectEdit'),
    ('QCS_5.0.0.0_inspect_otherInspectView'),
    ('QCS_5.0.0.0_inspect_qualityInspectList'),
    ('QCS_5.0.0.0_inspect_qualityInspectEdit'),
    ('QCS_5.0.0.0_inspect_qualityInspectView'),
    ('QCS_5.0.0.0_inspectReport_otherInspReportList'),
    ('QCS_5.0.0.0_inspectReport_otherInspReportEdit'),
    ('QCS_5.0.0.0_inspectReport_otherInspReportView'),
    ('QCS_5.0.0.0_inspectReport_quaInspReportList'),
    ('QCS_5.0.0.0_inspectReport_quaInspReportEdit'),
    ('QCS_5.0.0.0_inspectReport_quaInspReportView'),
    ('QCS_5.0.0.0_unQlfDeal_otherUnQlfDealList'),
    ('QCS_5.0.0.0_unQlfDeal_otherUnQlfDealEdit'),
    ('QCS_5.0.0.0_unQlfDeal_otherUnQlfDealView')
), payload as (
  select target.view_code,
         convert_from(lo_get(runtime_extra_view.view_json), 'UTF8') as body
  from target
  left join runtime_extra_view using (view_code)
)
select count(*) as expected_rows,
       count(*) filter (where body is not null) as restored_rows,
       min(octet_length(body)) as min_bytes,
       max(octet_length(body)) as max_bytes,
       count(*) filter (
         where body like '%ec.common.tableNo%'
            or body like '%ec.list.taskDescription%'
            or body like '%默认操作%'
       ) as raw_label_rows
from payload;
```

结果：

```text
expected_rows=15
restored_rows=15
min_bytes=6411
max_bytes=89017
raw_label_rows=0
```

## 验收边界

- 这次解决的是缺失按钮、无效新增入口、空白编辑/查看布局和原始文案键。
- 当前 5 个列表均为 0 项，因此没有执行打开、关闭、批量提交或删除的数据状态变更。
- 本轮没有创建或保存业务单据，业务数据落库标记为 `NOT_APPLICABLE`。
- 后续准备受控测试单据后，还需单独验收申请提交、巡检报告生成、不合格处置和 PostgreSQL
  业务表字段变化。
