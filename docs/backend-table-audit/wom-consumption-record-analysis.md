# WOM 投入明细与消耗记录专项分析

## 结论

`ADP_E2E_20260616021612_WOM_ACTIVE` 的真实前端验收已经证明投入明细会落到 `wom_putin_details`，但不会自动生成 `wom_mat_consum_recods`。这不是本轮发现的 PostgreSQL 缺表、缺列或 Oracle 方言兼容问题，而是当前 WOM 源码路径的业务设计结果：

1. `WOMProduceTaskServiceImpl.startActive` 对 `RM_activeType/putin` 设置 `noCreateActiExe=true`，人工投料活动开始时不生成 `WOMActiExelog`。
2. `WOMProduceTaskServiceImpl.endActive` 只遍历已有 `WOMActiExelog` 调用 `actiExelogService.generateInOutRecordByAvtiveRecord(actiExelog)`。
3. `WOMActiExelogServiceImpl.generateInOutRecordByAvtiveRecord` 才是自动生成 `WOMMatConsumRecod` 的入口，且需要 `WOMActiExelog` 上的 `actualNum/useNum/material/putinDetailId` 等字段。
4. `WOMProduceTaskServiceImpl.toProcfeedback` 对人工投料只校验 `wom_putin_details` 并处理尾料，生成生产用料单的 `generatePutInMaterialBill(...)` 逻辑在当前源码中被注释为“不生成投料单/不生成生产用料单”。

因此，当前验收状态应写为：

- `wom_putin_details` 投入明细落库：`PASS`。
- `wom_mat_consum_recods` 自动生成：`NOT_APPLICABLE_FOR_CURRENT_PUTIN_PATH`，除非产品确认人工投料活动结束也必须生成消耗记录。

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

## 真实库验证

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

既然人工投料活动没有活动执行记录，`generateInOutRecordByAvtiveRecord` 没有可处理输入，因此本次不能把 `wom_mat_consum_recods=0` 写成 PostgreSQL 落库失败。

## 后续处理规则

如果业务确认“人工投料活动结束必须生成消耗记录”，不能只用 PostgreSQL 触发器硬补。推荐先补业务设计：

1. 明确 `wom_mat_consum_recods` 应关联 `WOMActiExelog` 还是直接关联 `WOMPutinDetail`。
2. 明确 `report_num`、`putin_num`、仓库、货位、人员、同步状态、库存接口调用的字段来源。
3. 再决定是在 Java 服务层补活动执行记录/消耗记录生成，还是新增明确的 PostgreSQL 兼容补丁。
4. 补充独立验收脚本，断言 `wom_putin_details` 和 `wom_mat_consum_recods` 两条链路同时成立。

在业务确认前，生产模块剩余优先级应继续放在请检、质量判定、不良数、完工入库和库存链路的真实前端落库验收。
