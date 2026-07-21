# BPI 正式身份完工入库冲销验收

## 结论

- 状态：`PASS_TARGET_FORMAL_IDENTITY_WMS_REVERSAL_TWO_BROWSER_SESSIONS_CLEANED`
- 目标环境：`http://10.11.100.17:18080`
- 部署源码：`80cf094a415c4ae33541f08da8b640fc15c52098`
- 数据库：PostgreSQL 15.18，Flyway V25
- marker：`ADP_BPI_FORMAL_WMS_REVERSAL_20260721175536`
- 申请人：`legacy-ticket:admin`
- 审批人：`legacy-ticket:bpi_reviewer_20260721175536`

本轮证明真实 ADP 页面、两个独立身份会话、Java 8 adapter、Java 17 service 与 PostgreSQL
能够完成“申请冲销 -> 同人审批拒绝 -> 独立审批 -> 追加红单命令”。外部 ERP/WMS 未启用，
因此本轮正确终态是 `PENDING_WMS`，不把它解释为外部红字单据已经完成。

## 运行保护

验收前六个集成开关均为 `false`。编排器通过隔离 Compose override 仅临时设置
`BPI_PHASE2_INTEGRATION_ENABLED=true`，同时保持 Protobuf HTTP ingress、Kafka、WMS outbox、
WMS adapter 和 QCS outbox 为 `false`。基础 `.env` 未编辑，service/adapter 镜像 ID 在前后完全一致，
900 秒 watchdog 未触发。验收结束后六个开关全部恢复为 `false`。

## 页面与 API

| 步骤 | 页面操作 | API | 预期 | 实际 | 状态 |
|---|---|---|---|---|---|
| 未认证读取 | 不登录读取冲销任务 | `GET /bpi-api/batches/{batchId}/wms/reversal` | 拒绝访问 | HTTP 401 | PASS |
| 申请 | `admin` 打开批次详情，点击“申请入库冲销”，填写依据并提交 | `POST /bpi-api/batches/{batchId}/wms/reversal` | 待独立审批，不创建红单 | HTTP 202；task `PENDING_APPROVAL/r1`；batch `INBOUNDED/r5` | PASS |
| 同人反证 | 申请人在同一页面点击“独立审批冲销” | 同一 POST | 职责分离拒绝且事务回滚 | HTTP 403；提示必须由不同管理员完成 | PASS |
| 独立审批 | 第二个真实 ADP 会话打开同一批次，批准并生成红单 | 同一 POST | 追加红单，原蓝单不变 | HTTP 202；task `PENDING_WMS/r2`；batch `INBOUND_REVERSING/r6` | PASS |
| 投影读取 | 审批后读取 release、reversal task 与 timeline | 三个 GET | 页面与数据库状态一致 | 原蓝单仍可见；REQUESTED/APPROVED 时间线完整 | PASS |

请求和批准均携带唯一 `Idempotency-Key` 与 `If-Match`。页面无 page error、request failure、横向
溢出或非预期 BPI HTTP error；唯一 console 403 是同人审批负测产生的预期安全记录。

## PostgreSQL 落库

| 阶段 | batch | task | 蓝单 | 红单 | state/audit/idempotency |
|---|---|---|---|---|---|
| 初始 | `INBOUNDED/r4` | 无 | event `8b49dd51-837a-4f2a-a9ad-fe8c4ca2f373`，document `ADP_BPI_FORMAL_WMS_REVERSAL_20260721175536_BLUE_DOC`，payload SHA `3ac5adfdd3a2fd1d77d902dab8c500cc1ef40f05d900230bd768499bd3c5edea` | 0 | 0/0/0 |
| 申请后 | `INBOUNDED/r5` | `PENDING_APPROVAL/r1` | ID、单号、状态与 SHA 不变 | 0 | 1/1/1 |
| 独立审批后 | `INBOUND_REVERSING/r6`，`REVERSAL_PENDING` | `PENDING_WMS/r2`，申请人与审批人不同 | ID、单号、状态与 SHA 不变 | 1 条 `PENDING`，event `abf196c7-2aa0-56d8-86b0-66d9a6d32878`，payload 994 字节 | 2/2/2 |

后端链路：

`BpiProxyController -> BpiRoutePolicy/LegacyTicketVerifier -> BatchController ->
WmsInboundReversalService -> WmsInboundReversalPostgresRepository/BatchReleasePostgresRepository`。

核验脚本：

- `deploy/docker/scripts/bpi-wms-inbound-reversal-acceptance-fixture.sql`
- `deploy/docker/scripts/bpi-wms-inbound-reversal-acceptance-verification.sql`
- `deploy/docker/scripts/bpi-wms-inbound-reversal-acceptance-cleanup.sql`

## 清理与证据

marker 的 batch、task、quality gate/link、WMS link、蓝/红 outbox、inbox、state event、audit、
idempotency 和临时 PLANT flags 均为 0。临时 person/user 已软删除，RBAC 与 auth role 活跃绑定为 0。

- 机器报告：`metadata/bpi-formal-identity-wms-reversal-acceptance.json`
- 待审批截图：`metadata/bpi-formal-identity-wms-reversal-pending.png`
- 已批准截图：`metadata/bpi-formal-identity-wms-reversal-approved.png`
- 待审批截图 SHA-256：`0fb7a0b0e7a5ad3e577238325fbdee471833691428a9be81048e8f1e7352b12f`
- 已批准截图 SHA-256：`7f6ba9bcdb376f871d68b56d99d630a6ba5147b649e02396f90365c760223210`

## 未关闭边界

外部 ERP/WMS 的红单消费、查单、响应丢失、拒绝、补偿及 durable receipt 仍未执行。只有接入真实
外部端点并取得回执后，才能把任务从 `PENDING_WMS` 验收到 `COMPLETED` 或 `FAILED`，并据此确认
`INBOUND_REVERSED` 或恢复 `INBOUNDED`。本轮没有打开 WMS outbox 或 adapter 来伪造这项证据。
