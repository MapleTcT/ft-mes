# BPI 正式身份内部 WMS 蓝单/红单整链验收

## 结论

- 状态：`PASS_TARGET_FORMAL_IDENTITY_INTERNAL_WMS_ROUNDTRIP_CLEANED`
- 数据库：`PostgreSQL`
- 目标：`http://10.11.100.17:18080`
- marker：`ADP_BPI_FORMAL_WMS_REVERSAL_20260721190630`
- 页面：`/bpi/#/batches`
- 执行基线：`341745bab543060e2c437beb08a4c87b0acc85ea`
- 机器证据：`metadata/bpi-formal-identity-wms-roundtrip-acceptance.json`

本轮把目标环境内部 `material-wms` 的完工入库与追加式冲销闭合到同一条真实链：

`BPI 蓝单 outbox -> 隔离 Kafka -> WMS adapter -> material-wms 蓝单/库存 -> durable receipt -> INBOUNDED -> 页面申请 -> 不同管理员批准 -> BPI 红单 outbox -> 隔离 Kafka -> material-wms 红单/负库存事务 -> durable receipt -> INBOUND_REVERSED`

该结论只覆盖仓库内置 `material-wms`，不代表外部 ERP/WMS 已联调。报告中的
`safety.externalWmsReceiptExpected=false` 是验收边界，不是缺失证据的掩饰。

## 目标扩展迁移与部署

| 动作 | 保护条件 | 实际结果 | 证据 |
|---|---|---|---|
| 应用 material-wms 红单扩展迁移 | 必须显式设置 `BPI_MATERIAL_REVERSAL_MIGRATION_CONFIRM=APPLY_EXPAND_ONLY_TO_TARGET`；先备份，再只做 expand-only | `PASS_APPLIED_EXPAND_ONLY`；迁移前 5 项结构均不存在，迁移后红单关联列、两类约束、外键和唯一索引均存在；无不支持的既有单据/事务类型 | `metadata/bpi-material-wms-reversal-schema-upgrade.json` |
| 部署当前 material-wms JAR | 必须显式设置 `BPI_MATERIAL_WMS_DEPLOY_CONFIRM=DEPLOY_BACKUP_RESTART_AND_VERIFY`；旧 JAR 先备份，失败自动恢复 | `PASS_BACKUP_RESTART_VERIFIED`；新 JAR SHA-256 `de4cac373b620cddfb9a6eeefcbaebc931a706b4566bb19692f6ad68e17c01e3`；容器健康，红单路由通过鉴权门禁 | `metadata/bpi-material-wms-target-deployment.json` |

数据库备份为
`/data/docker/bpi-upgrade-backups/material-wms-reversal-20260721190000/adp-material-wms-before-reversal-20260721190000.dump`，
SHA-256 为 `b37200f5cb2a1234c06bc378d0959728bbd0ca68d920c9771c4c549c45f0e622`。
旧 JAR 保存在
`/data/docker/bpi-upgrade-backups/material-wms-runtime-20260721190451/material-wms.previous.jar`。

## 页面与 API

| 阶段 | 页面操作 / API | HTTP 与页面结果 | 业务状态 |
|---|---|---|---|
| 蓝单入库 | 隔离蓝单 topic 交给真实 WMS adapter；页面读取批次详情 | 蓝 command/receipt 各 1，DLQ 0；页面可读取 durable 单据 | batch `INBOUNDED/r4`；蓝单 `ACCEPTED/r2`，outbox `PUBLISHED/r3` |
| 申请冲销 | `admin` 在 `/bpi/#/batches` 提交 `POST /bpi-api/batches/5844c3a5-c324-467d-9b95-afa4be810b0b/wms/reversal`，`approvalMode=REQUEST` | HTTP 202；页面显示待审批，截图无横向溢出 | batch `INBOUNDED/r5`；task `PENDING_APPROVAL/r1`；红 outbox 0 |
| 同人审批反证 | 同一 `admin` 会话提交 `approvalMode=APPROVE` | HTTP 403，明确要求不同管理员；事务无第二次业务变化 | batch/task revision 不变，红 outbox 仍为 0 |
| 独立审批 | 第二个真实 ADP 会话 `legacy-ticket:bpi_reviewer_20260721190630` 批准 | HTTP 202；页面显示申请人、审批人和已发布红命令 | batch `INBOUND_REVERSING/r6`；task `PENDING_WMS/r2`；红 outbox `PUBLISHED/r3` |
| 红单完成 | 恢复 WMS adapter，消费隔离红单 topic；随后由真实 ADP 页面重新读取 | 红 command/receipt 各 1，DLQ 0；最终页面 console/page/request/BPI HTTP error 均为 0，1600px 视口无溢出 | batch `INBOUND_REVERSED/r7`；task `COMPLETED/r3`；WMS 状态 `REVERSED` |

页面截图：

- `metadata/bpi-formal-identity-wms-roundtrip-pending.png`
- `metadata/bpi-formal-identity-wms-roundtrip-approved.png`
- `metadata/bpi-formal-identity-wms-roundtrip-completed.png`

## PostgreSQL 落库

BPI 侧使用 `deploy/docker/scripts/bpi-wms-formal-roundtrip-verification.sql`，material 侧使用
`deploy/docker/scripts/bpi-wms-formal-roundtrip-material-verification.sql`。关键结果如下：

| 事实 | BPI PostgreSQL | material-wms PostgreSQL |
|---|---|---|
| 原蓝单 | command event `8b87d452-8085-4174-82f8-bc009035ec74`；document `CIN-8b87d452-8085-4174-82f8-bc009035ec74-WARE-E2E` | 蓝单最终 `REVERSED`；1 行 `12.345 kg`；原正向库存事务 `+12.345` |
| 红单 | command event `a760f905-8028-5779-a345-5a0b37971a4e`；receipt event `b7de879a-aeea-5cab-8cd5-1357546a22ef`；document `CIR-a760f905-8028-5779-a345-5a0b37971a4e-WARE-E2E` | 红单 `POSTED`，`reversal_of_document_id=31`；1 行 `12.345 kg`；冲销库存事务 `-12.345` |
| 状态事件 | 精确 4 条：`WMS_INBOUND_ACCEPTED`、`WMS_INBOUND_REVERSAL_REQUESTED`、`WMS_INBOUND_REVERSAL_APPROVED`、`WMS_INBOUND_REVERSAL_ACCEPTED` | 蓝/红两张单据及两条库存事务均唯一 |
| 最终库存 | batch `INBOUND_REVERSED/r7`，task `COMPLETED/r3` | `on_hand=0`、`available=0`、`hold=0` |

内部 material API 为：

- `POST /material/wms/completion-inbound-reversals`
- `GET /material/wms/completion-inbound-reversals/by-idempotency`

adapter 继续执行 query-first：创建响应不确定时先按同一幂等键查单，只有全部事实一致才发布
accepted receipt，不能把超时当成业务拒绝。

## Kafka 与退场

本轮为 marker 创建 10 个隔离 topic 和 2 个隔离 consumer group。最终蓝 command、蓝 receipt、红
command、红 receipt 都精确为 1；四个 DLQ、QCS topic、QCS DLQ 和两个 consumer lag 都为 0。

退场结果：

- BPI marker 关联行全部为 0。
- material-wms 单据、明细、事务和库存 marker 行全部为 0。
- 临时 person/user 已软删除，活动角色与认证绑定为 0。
- 10 个隔离 topic 和 2 个 group 已删除。
- Adapter scope、Service/Adapter/WMS Adapter 镜像 ID 精确恢复。
- 六个 Phase 2/QCS/WMS 集成开关全部恢复 `false`，基础 `.env` 未编辑。
- watchdog 未触发。

## 未关闭边界

- 真实外部 ERP/WMS endpoint、凭据、查单协议和 durable receipt 尚未接入。
- 外部系统的响应丢失、业务拒绝、补偿、对账与回滚演练尚未验收。
- 物理设备来源、正式校准以及选定产线连续 7-14 天运行仍未完成。
- 因此 `G-021` 继续保持 `PARTIAL`，所有 Phase 2 开关继续默认关闭。
