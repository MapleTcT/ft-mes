# WOM 投入明细与消耗记录专项分析

## 结论

历史 marker `ADP_E2E_20260616021612_WOM_ACTIVE` 证明原始投料路径只写
`wom_putin_details`，不会自动生成 `wom_mat_consum_recods`。这不是 PostgreSQL 缺表、缺列或
Oracle 方言兼容问题，而是恢复源码中活动执行记录被跳过后，既有消耗生成器没有输入：

1. `WOMProduceTaskServiceImpl.startActive` 对 `RM_activeType/putin` 设置 `noCreateActiExe=true`，人工投料活动开始时不生成 `WOMActiExelog`。
2. `WOMProduceTaskServiceImpl.endActive` 只遍历已有 `WOMActiExelog` 调用 `actiExelogService.generateInOutRecordByAvtiveRecord(actiExelog)`。
3. `WOMActiExelogServiceImpl.generateInOutRecordByAvtiveRecord` 才是自动生成 `WOMMatConsumRecod` 的入口，且需要 `WOMActiExelog` 上的 `actualNum/useNum/material/putinDetailId` 等字段。
4. `WOMProduceTaskServiceImpl.toProcfeedback` 对人工投料只校验 `wom_putin_details` 并处理尾料，生成生产用料单的 `generatePutInMaterialBill(...)` 逻辑在当前源码中被注释为“不生成投料单/不生成生产用料单”。

2026-07-27 已通过受控 WOM 核心源码补丁关闭该缺口：

- `build-wom-core-production-boot-jar.sh` 在投料明细成功保存、活动结束时，逐明细检查现有执行记录，
  仅补建缺失的 `WOMActiExelog`，并覆盖同一未完成活动下的全部有效报工单。
- 随后的原有 `generateInOutRecordByAvtiveRecord(...)` 负责生成物料消耗，不使用数据库触发器伪造业务事实。
- `ADP_E2E_20260727_MES_FULL_CLOSED_10` 保留完全不续跑基线；最新 marker
  `ADP_E2E_20260727_MES_FULL_CLOSED_11` 已精确证明 `wom_putin_details=1`、
  投料 `wom_acti_exelogs=1`、`wom_mat_consum_recods=1`，并确认全链产出明细/产出台账 `2/2`。
- 当前状态：`CLOSED_BY_SERVICE_PATCH_AND_REAL_ACCEPTANCE`。

## 源码证据

来源源包：

`/Users/zhangchu/Documents/ADP/mes-modules-source-repo/modules/wom/WOM_6.1.3.4`

关键链路：

| 位置 | 证据 |
| --- | --- |
| `WOMProduceTaskServiceImpl.java:10039-10048` | 注释说明人工投料、人工产出、人工投配料、管道投配料、管道产出、管道投料活动开始不生成活动执行记录；`RM_activeType/putin` 在 `noCreateActiExe` 内 |
| `WOMProduceTaskServiceImpl.java:10307-10312` | `endActive` 先查询 `WOMActiExelog`，再调用 `toProcfeedback` |
| `WOMProduceTaskServiceImpl.java:10361-10392` | 只有遍历 `actiExelogs` 时才调用 `actiExelogService.generateInOutRecordByAvtiveRecord(actiExelog)` |
| `WOMProduceTaskServiceImpl.java:10447-10472` | 人工投料结束只校验存在 `WOMPutinDetail`；生成投料单逻辑被注释为“不生成投料单” |
| `WOMProduceTaskServiceImpl.java:10473-10516` | 人工投料结束处理尾料记录，不创建 `WOMMatConsumRecod` |
| `WOMActiExelogServiceImpl.java:6363-6425` | `generateInOutRecordByAvtiveRecord` 对投料/投配料活动生成 `WOMMatConsumRecod`，但输入是 `WOMActiExelog` |
| `WOMPutinDetailServiceImpl.java:6001-6003` | 删除投入明细时关联删除 `WOMMatConsumRecod` 的代码也被注释，说明二者不是当前 save/endActive 路径的强制同步关系 |

## 历史真实库验证

远端环境：`10.11.100.17`，PostgreSQL 容器：`adp-mes-newbase-postgres-1`。

验收 marker：

`ADP_E2E_20260616021612_WOM_ACTIVE`

复核 SQL：

```sql
SELECT 'activeExelogCount', count(*)
FROM public.wom_acti_exelogs
WHERE task_active_id = 8388157617258688
UNION ALL
SELECT 'matConsumCount', count(*)
FROM public.wom_mat_consum_recods
WHERE put_mat_detail_id = 755716019803392
   OR putin_material_detail_id = 755716019803392;
```

结果：

| 项 | 结果 |
| --- | ---: |
| `activeExelogCount` | 0 |
| `matConsumCount` | 0 |

该历史结果解释了修复前根因，不能再作为当前产品行为。

## 当前真实库验证

当前 marker：

`ADP_E2E_20260727_MES_FULL_CLOSED_11`

关键结果：

| 项 | 结果 |
| --- | ---: |
| `wom_putin_details` | 1 |
| finished `wom_acti_exelogs` | 1 |
| `wom_mat_consum_recods` | 1 |
| 全链 `wom_output_details` | 2 |
| 全链 `wom_mat_outpt_records` | 2 |
| 页面可见 warning/error | 0 |

完整 SQL、请求、响应和最终制造/QCS/WMS/追溯状态见
`metadata/mes-full-production-flow-current.json`。验收脚本现在把活动执行记录或消耗记录缺失直接判为
`FAIL`，不再降级成非阻断 issue。

## 后续处理规则

1. 保持服务层生成，不改成 PostgreSQL 触发器。
2. 保持输入明细、活动执行和消耗记录的同批次/同活动联查。
3. 每次生产部署 WOM 包时必须设置 `ADP_WOM_REQUIRE_CHECKSUMS=true`，提供输入 JAR、两份源码的
   SHA-256，并复跑完整 marker 流程；本地准备允许无 checksum，但构建日志必须输出实际哈希。
4. 现场 Batch/DCS 接入后，用真实仓库、货位、单位和库存接口再做同等验收。
