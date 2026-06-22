# WOM/QCS Bad Quantity Field Audit

Generated at: 2026-06-20

Database: PostgreSQL on `100.99.133.43`

## Conclusion

The recovered production and QCS packages do not expose a standalone bad-quantity registration field, endpoint, runtime button, or dedicated target table.

What exists and has already passed functional persistence acceptance:

| Capability | Current Evidence |
|---|---|
| QCS inspect quantity | `qcs_inspects.quantity` stores inspection request quantity. |
| QCS result judgement | `qcs_inspect_reports.check_result` and `qcs_report_coms.check_result` store qualified or unqualified result values. |
| Automatic unqualified treatment | `qcs_inspect_reports.un_qlf_deal_flag=true` plus `qcs_un_qlf_deals.report_id` prove treatment document generation. |
| WOM quality backfill | `wom_produce_tasks.check_result`, `wom_produce_task_exelog.check_result`, `wom_wait_put_records.check_result`, and `baseset_batch_infos.check_result/is_available/active_batch_state_id` prove qualified/unqualified backfill. |
| Treatment decision backfill | `qcs_un_qlf_deals.act_bat_state_id` and WOM `rejects_deal_id` prove noDeal/degradedRelease/return treatment write-back. |

What is still blocked:

| Blocker | Reason |
|---|---|
| Independent bad quantity entry | No recovered QCS/WOM table column, runtime property, menu operation, or source endpoint maps to a standalone bad quantity field. |
| Inventory or stock-in write-back | WOM calls the missing `material` tenant service at `/material/foreign/foreign/checkProdResult` and `/public/material/produceOutSingle/produceOutSing/generateProduceOutSing`; current Nacos `prod` group has no `material` service instance. See `material-service-dependency-analysis.md` and `metadata/material-service-dependency-analysis.json`. |

## Source Evidence

`QCSUnQlfDeal` is a treatment document, not a quantity detail record:

| Source | Relevant Fields |
|---|---|
| `QCSUnQlfDeal.java` | `reportId`, `prodId`, `tableTypeId`, `unQlfReason`, `dealDeptId`, `dealerId`, `dealTime`, `memoField`, `batchCode`, `busiTypeId` |
| `QCSUnQlfDealController.java` | save and submit endpoints for `manuUnQlfDealEdit/View`, plus public `/public/QCS/unQlfDeal/unQlfDeal/createUnQlfDeal` |
| `QCSUnQlfDealServiceImpl.createUnQlfDealForOtherModule` | requires an effective unqualified report, then creates a treatment document based on `manuUnqlf/purchUnqlf/otherUnqlf` workflow config |
| `WOMQCSServiceImpl.checkReportBackfillWom` | unqualified report returns `needRejectReport=1`; code comment still says bad product treatment is a follow-up path |

## Runtime Evidence

Runtime views exist for:

| View Code | Meaning |
|---|---|
| `QCS_5.0.0.0_inspectReport_manuInspReportList/Edit/View` | 产品检验报告列表/编辑/查看 |
| `QCS_5.0.0.0_unQlfDeal_manuUnQlfDealList/Edit/View` | 产品不合格品处理列表/编辑/查看 |

`rbac_menuoperate` for `QCS_5.0.0.0_unQlfDeal_manuUnQlfDealList` exposes query and delete operations only. It does not expose a list-level add button for manual bad quantity entry.

## PostgreSQL Evidence

Explicit bad or defect column audit:

```sql
select table_name,column_name,data_type
from information_schema.columns
where table_schema='public'
  and (table_name like 'qcs_%' or table_name like 'wom_%')
  and (
    lower(column_name) like '%bad%'
    or lower(column_name) like '%defect%'
    or lower(column_name) like '%scrap%'
    or lower(column_name) like '%unqualified%'
    or lower(column_name) like '%unqlf%'
    or lower(column_name) like '%un_qlf%'
  )
order by table_name, ordinal_position;
```

Current result:

| table_name | column_name | data_type |
|---|---|---|
| `qcs_inspect_reports` | `un_qlf_deal_flag` | boolean |
| `qcs_table_types` | `un_qlf_code_rule` | oid |
| `qcs_table_types` | `un_qlf_code_rule_text_backup` | text |
| `qcs_un_qlf_deals` | `un_qlf_reason` | character varying |

Quantity and result-related columns:

| Table | Relevant Columns |
|---|---|
| `qcs_inspects` | `quantity`, `numberparama..f` |
| `qcs_inspect_reports` | `check_result`, `un_qlf_deal_flag`, `numberparama..f` |
| `qcs_report_coms` | `check_result`, `numberparama..f` |
| `qcs_un_qlf_deals` | no numeric quantity column |
| `wom_produce_tasks` | `finish_num`, `plan_num`, `check_result`, `rejects_deal_id`, `reject_system` |
| `wom_produce_task_exelog` | `finish_num`, `check_result`, `rejects_deal_id` |
| `wom_task_actives` | `plan_quantity`, `standard_quantity`, `sum_num`, `check_result`, `rejects_deal_id`, `reject_system` |
| `baseset_batch_infos` | `check_result`, `is_available`, `active_batch_state_id`, `rejects_deal_id` |

## Acceptance Impact

`PROD-019` and `PROD-021` remain `BLOCKED`, but the blocker is now precise:

1. QCS unqualified report, automatic treatment document, degraded release, return, and WOM backfill are already accepted.
2. The current recovered product shape does not contain a standalone bad quantity entry to execute.
3. If the product manual or a later business package defines a dedicated bad quantity function, it must provide the route, field, and table mapping before it can be accepted.
4. Inventory or stock-in write-back remains blocked by the missing `material` service package and cannot be inferred from QCS/WOM state tables; the service-level evidence is recorded in `material-service-dependency-analysis.md`.
