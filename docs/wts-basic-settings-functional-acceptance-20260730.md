# WTS 基础设置新增表单验收

## 结论

2026-07-30 在测试环境 `http://10.11.100.17:18080` 使用 admin 的真实权限目录，
逐项验收“作业管理 -> 基础设置”下 11 个子菜单。修复后 11/11 的新增入口均能打开
非空表单，按钮、字段或明细表可交互，浏览器 console、page error、HTTP 4xx/5xx 和
请求失败均为 0。

本轮另选“作业时间约束”完成新增、修改和删除的真实页面操作，并直接查询 PostgreSQL。
新增和修改均真实落入 `wts_hour_limits`；删除按旧平台约定将 `valid` 更新为 `false`。

## 根因与修复

1. 11 个菜单依赖的 46 份 list/edit/view/reference 运行时布局未完整恢复，点击新增时只能
   打开标题和底部按钮，表单主体为空。
2. `ec_extra_view.view_json` 中仍有 Oracle CLOB 文本，而旧 Hibernate PostgreSQL 映射
   需要 large-object OID。
3. 危害识别库的新增和修改共用了旧“编辑”操作权限，且新增/默认标签的国际化配置缺失。
4. 风险与安全措施库的恢复布局仍调用旧脚本入口，页面打开后会触发未定义函数。

修复固化在：

- `deploy/docker/postgres/init/230-wts-basic-settings-action-runtime-json.sql`
- `deploy/docker/postgres/init/231-wts-basic-settings-action-lob-compat.sql`
- `deploy/docker/postgres/init/232-wts-hazid-action-permissions.sql`
- `deploy/docker/postgres/init/233-wts-hazid-i18n-labels.sql`
- `deploy/docker/postgres/init/234-wts-risk-safe-measures-script-entrypoints.sql`

## 页面验收

| 子菜单 | 页面/路由 | 新增交互 | 表单主要内容 | 可见控件 | console/network | 状态 |
|---|---|---|---|---:|---|---|
| 作业位置设置 | `/msService/WTS/workRegion/region/workRegionList` | 弹窗 | 区域编码、区域名称、坐标、备注、作业地点明细 | 4 | 0/0 | PASS |
| 作业人资质设置 | `/msService/WTS/qualifiSet/qualifiSet/qualifyList` | 弹窗 | 作业票类型、具备条件、启用、资质明细 | 5 | 0/0 | PASS |
| 危害识别库 | `/msService/WTS/hazidLib/hazid/hazidLibList` | 弹窗 | 作业票类型、候选值、风险级别、默认、备注 | 1 | 0/0 | PASS |
| 风险与安全措施库 | `/msService/WTS/riskSafeMeasures/riskSafey/riskSafeyLisit` | 新页面 | 作业票类型、确认类型、启用、权限、措施明细 | 7 | 0/0 | PASS |
| 气体分析库 | `/msService/WTS/gasAnalysis/gasAnalysis/gasAnalysisList` | 弹窗 | 作业票、启用、签名、气体上下限明细 | 4 | 0/0 | PASS |
| 验收标准库 | `/msService/WTS/checkCriteriaLib/checkCriteria/checkCriteriaList` | 弹窗 | 作业票、启用、备注、验收内容明细 | 3 | 0/0 | PASS |
| 作业时间约束 | `/msService/WTS/hourLimit/hourLimit/hourLimitList` | 弹窗 | 作业类型、时间间隔、启用、备注 | 4 | 0/0 | PASS |
| 气体分析流程设置 | `/msService/WTS/wfGasAnalysis/wfGasAnalysis/wfGasAnalysisList` | 弹窗 | 作业类型、作业等级、默认流程 | 6 | 0/0 | PASS |
| 审批配置 | `/msService/WTS/approveConfig/approveConfig/approveConfigList` | 新页面 | 作业类型、工作流版本、启用、备注 | 4 | 0/0 | PASS |
| 路径脚本 | `/msService/WTS/wfPathFunction/wfPathFunction/wfPathFuncList` | 弹窗 | 作业类型、脚本名称、激活路径、参数、启用 | 3 | 0/0 | PASS |
| 定制化配置 | `/msService/WTS/customizedConfig/ccListFilter/listFilterList` | 弹窗 | 规则名称、列表名称、自定义条件、启用、备注 | 5 | 0/0 | PASS |

## 真实落库验收

### 请求

唯一 marker：
`ADP_E2E_20260730_WTS_BASIC_1785414128217`

新增：

```text
POST /msService/WTS/hourLimit/hourLimit/hourLimitEdit/submit
workType=WTS_workType/soilWork
limitHour=2.5
isOn=true
remark=ADP_E2E_20260730_WTS_BASIC_1785414128217
```

修改：

```text
POST /msService/WTS/hourLimit/hourLimit/hourLimitEdit/submit?id=6715071776309408
limitHour=3
remark=ADP_E2E_20260730_WTS_BASIC_1785414128217_UPDATED
```

响应为 HTTP 200、`dealSuccessFlag=true`、`operate=edit`、`id=6715071776309408`。

删除：

```text
POST /msService/WTS/hourLimit/hourLimit/delete?ids=6715071776309408@1
```

响应为 HTTP 200、`dealSuccessFlag=true`、`operate=delete`，页面显示“删除成功”且列表不再
显示该记录。

### 后端链路

```text
WTSHourLimitController.submit
  -> WTSHourLimitServiceImpl.submit
  -> WTSHourLimitServiceImpl.saveHourLimit
  -> WTSHourLimitDaoImpl / ExtendedGenericDaoImpl.save|merge
  -> wts_hour_limits
```

删除走
`WTSHourLimitServiceImpl.deleteHourLimit`，旧平台使用
`update WTSHourLimit set valid=false where id in(:ids)` 完成软删除。

### PostgreSQL 复读

修改后：

```sql
select id, work_type, limit_hour, is_on, remark, valid, version,
       to_char(modify_time, 'YYYY-MM-DD HH24:MI:SS')
from public.wts_hour_limits
where remark = 'ADP_E2E_20260730_WTS_BASIC_1785414128217_UPDATED';
```

结果：

```text
6715071776309408 | WTS_workType/soilWork | 3.000000 | true |
ADP_E2E_20260730_WTS_BASIC_1785414128217_UPDATED | true | 1 |
2026-07-30 20:22:35
```

删除后：

```sql
select id, valid, remark
from public.wts_hour_limits
where id = 6715071776309408;
```

结果：

```text
6715071776309408 | false |
ADP_E2E_20260730_WTS_BASIC_1785414128217_UPDATED
```

## 验收边界

- 11 个子菜单均完成真实浏览器的新增表单渲染验收。
- “作业时间约束”完成新增、修改、软删除和 PostgreSQL 复读。
- 其余 10 个子菜单本轮没有保存业务记录，因此不能把“表单可用”解释为全部业务规则和
  全字段落库均已验收。
- 测试 marker 已通过业务删除入口清理为 `valid=false`，不再出现在有效列表。

机器记录：
`metadata/wts-basic-settings-runtime-acceptance-20260730.json`。
