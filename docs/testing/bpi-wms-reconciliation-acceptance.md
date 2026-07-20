# BPI WMS 原单核对验收

## 结论

2026-07-21 在唯一测试环境 `10.11.100.17` 完成真实页面、Java 8 adapter、Java 17 service、
API 和 PostgreSQL 联合验收。最终结论为
`PASS_TARGET_CONTROLLED_BROWSER_API_POSTGRES_CLEANED`。

本轮修复的场景是：完工入库命令已经发布，但 BPI 长时间没有收到 durable receipt。管理员必须先核对
原单，再把**同一个 outbox event、同一个 WMS 幂等键**重新排队；系统不得创建第二条业务命令，也不得
把批次伪装成已经入库。

第一次真实页面请求暴露了 Java 8 adapter 未放行新路由，返回 403。提交
`3a00d69274b0019a42edd30abf024bce406157bc` 增加精确 allowlist 和 header 转发测试后，目标适配器
更新为 `ft-mes-bpi-adapter:20260720T174831Z-3a00d692`，最终验收通过。

## 真实操作

| 模块 | 页面/路由 | 操作 | API | 前端结果 | 后端结果 | 数据库表 | 状态 | 问题 |
|---|---|---|---|---|---|---|---|---|
| BPI 完工入库 | `/bpi/#/batches` | 选择 marker 批次，打开详情，点击“重新核对原单”，填写原因并确认 | `POST /bpi-api/batches/31176954-fd34-4f30-8610-1c4c18ba04ad/wms/reconcile` | 登录 200，请求 200，页面提示原入库命令已进入重新核对队列；console/page/request/BPI HTTP error 均为 0 | 原 event 和 WMS key 不变；link/outbox revision 变为 2，outbox 回到 PENDING，reconciliation count 为 1；批次仍为 RELEASED | `bpi_batch_instances`、`bpi_wms_inbound_links`、`bpi_outbox_events`、`bpi_api_idempotency`、`bpi_audit_events` | PASS_TARGET_CONTROLLED_CLEANED | 外部 ERP/WMS 未参与 |
| API 幂等 | 同上 | 使用同一 `Idempotency-Key` 和同一 payload 重放 | 同上 | HTTP 200，响应头 `Idempotent-Replay: true` | reconciliation count 保持 1，未重复更新 | idempotency、WMS link、outbox、audit | PASS | 无 |
| 乐观锁 | 同上 | 使用新幂等键但旧 `If-Match: 1` 再提交 | 同上 | HTTP 409，返回 `currentRevision=2` | 没有第二次 requeue 或审计写入 | 同上 | PASS_FAIL_CLOSED | 无 |
| marker 退场 | 不适用 | 定向删除夹具并恢复运行开关 | SQL cleanup；Compose service restart | 页面恢复正常数据集 | marker 相关行总残留 0；五个 Phase 2/WMS 开关恢复 false；service/adapter healthy | 上述表及临时 feature flags | PASS_CLEANED | 无 |

## HTTP 证据

- marker：`ADP_E2E_20260721_015610_WMS_RECON`
- batch：`31176954-fd34-4f30-8610-1c4c18ba04ad`
- 原 command event：`354ba04c-6c11-48eb-923c-1fe17a2269a2`
- 原 WMS 幂等键：`ADP_E2E_20260721_015610_WMS_RECON|WMS|1`
- 页面命令幂等键：`cfc37798-4606-49bf-989c-8e1d9226dc62`
- 请求 revision：`If-Match: 1`
- 首次结果：`200`，WMS link revision `2`
- 同请求重放：`200`，`Idempotent-Replay: true`
- 旧 revision：`409`，`currentRevision=2`

后端链路：

```text
BpiProxyController
  -> BpiRoutePolicy
  -> BatchController.reconcileWmsInbound
  -> BatchReleaseService.reconcileWmsInbound
  -> BatchReleasePostgresRepository.lockWmsReconciliation
  -> BatchReleasePostgresRepository.requeueWmsReconciliation
  -> BatchReleasePostgresRepository.insertWmsReconciliationAudit
```

## PostgreSQL 证据

验收库为 `ft_mes_bpi`，schema 为 `bpi`，Flyway version 为 23。核对结果：

| 字段 | 结果 |
|---|---|
| batch state / revision | `RELEASED / 3` |
| batch WMS status | `PENDING` |
| WMS link status / revision | `PENDING / 2` |
| outbox status / revision | `PENDING / 2` |
| manual retry / total attempt | `1 / 1` |
| command event identity | 与核对前相同 |
| WMS idempotency key | 与核对前相同 |
| audit rows | `1` |
| API idempotency rows | `1` |
| completion-inbound outbox rows | `1` |
| cleanup residual rows | `0` |

可复验 SQL：

- `deploy/docker/scripts/bpi-wms-reconciliation-fixture.sql`
- `deploy/docker/scripts/bpi-wms-reconciliation-verification.sql`
- `deploy/docker/scripts/bpi-wms-reconciliation-cleanup.sql`

浏览器入口：

```bash
ADP_BASE_URL=http://10.11.100.17:18080 \
ADP_USERNAME=admin \
ADP_PASSWORD='<test-password>' \
BPI_ACCEPTANCE_MARKER='<marker>' \
BPI_BATCH_ID='<batch-uuid>' \
BPI_COMMAND_EVENT_ID='<outbox-uuid>' \
BPI_WMS_IDEMPOTENCY_KEY='<original-wms-key>' \
make acceptance-bpi-wms-reconciliation-target
```

## 部署与回滚

- service：`ft-mes-bpi-service:20260720T171655Z-fb93966f1802`
- adapter：`ft-mes-bpi-adapter:20260720T174831Z-3a00d692`
- adapter JAR SHA-256：`69f6449b4b9282386dfadf297dc2fccb5e07355c412bdce81d1658ba02ef104a`
- adapter 回滚镜像：`ft-mes-bpi-adapter:rollback-20260720T174831Z-3a00d692`
- adapter 环境备份：`/home/v6/adp-releases/bpi-wms-reconciliation-3a00d692/.env.before-adapter-20260720T174831Z`

最终 `BPI_PHASE2_INTEGRATION_ENABLED`、Protobuf ingress、Kafka、WMS outbox 和 WMS adapter 均为
`false`。验收期间只临时开启总 Phase 2 门并创建两个 marker feature flag，取证后全部删除。

## 边界

本轮证明了仓库内“原命令核对/重排”的页面、API、并发、幂等、审计和 PostgreSQL 事务正确，不能据此
宣称外部 ERP/WMS 已完成联调。真实外部查单、响应丢失、服务宕机、业务冲销和补偿演练仍未执行，
`G-021` 继续保持 `PARTIAL`。

机器证据：`metadata/bpi-wms-reconciliation-target-acceptance.json`；截图：
`metadata/bpi-wms-reconciliation-target.png`。
