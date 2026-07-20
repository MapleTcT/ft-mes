# BPI 批次受控强制结束目标验收

## 结论

2026-07-21 在唯一 ADP 测试环境 `10.11.100.17` 完成真实页面、API、Java 8 adapter、
Java 17 BPI service 和 PostgreSQL 15.18/Flyway V24 联合验收。marker
`ADP_E2E_20260721053253_BPI_FORCE_CLOSE` 从 `ACTIVE/r1` 经申请进入
`ACTIVE/r2 + PENDING_APPROVAL/r1`，再由不同管理员批准进入
`CLOSED_RAW/r3 + COMPLETED/r2`。取证后 marker 和临时 `bpi.commands` 覆盖均清理为 0。

状态：`PASS_TARGET_CONTROLLED_BROWSER_API_POSTGRES_CLEANED`。

## 运行边界

- 页面：`http://10.11.100.17:18080/bpi/#/batches`
- Compose：`adp-mes-newbase`
- BPI 数据库：`ft_mes_bpi`，schema `bpi`，Flyway V24
- service 源码：`bcf40e2f91a18163fb8523598d06a980414d9ce6`
- adapter 源码：`1962f599b3ea90b1863548f45998f6e0fa89cc1d`
- 申请人：`legacy-ticket:admin`
- 审批人：`ADP_E2E_BPI_ADMIN_20260721053253`
- 业务边界时间：`2026-07-20T21:31:53Z`

验收只对受控影子夹具开放人工命令。Phase 2、QCS、Kafka 写回、WMS outbox 和 WMS adapter
开关在验收前后均为 false。

## 缺陷与修复

首次真实页面验收发现 force-close 详情 GET 和命令 POST 被 Java 8 adapter 返回 `403`。Java 17
服务、V24 表和前端实现均已部署，但 `BpiRoutePolicy` 未加入精确 force-close 路由，旧单元测试还把
该 POST 断言为拒绝。

修复后 adapter 只放行：

- `GET /batches/{uuid}/force-close`
- `POST /batches/{uuid}/force-close`

`/force-close/export`、`/force-close/approve` 等嵌套路径仍拒绝。代理测试同时证明固定 upstream、
替换旧 bearer、转发 `Idempotency-Key`/`If-Match` 并原样保留上游 `202`。目标机干净
Maven 3.9.9/JDK 8 全量测试为 `31/31 PASS`。同一精确源码的 Maven 3.9.9/JDK 17 反应堆为
`115` 项、失败/错误为 `0/0`；其中 `43` 项环境门控 PostgreSQL 测试在干净容器中跳过，本场景另有
下述真实目标 PostgreSQL 证据，不能用这 43 项跳过替代落库结论。

## 页面与 API 验收

| 步骤 | 请求/页面动作 | 预期 | 实际 | 状态 |
|---|---|---|---|---|
| 未认证读取 | `GET /bpi-api/batches/{id}/force-close` | 拒绝匿名访问 | HTTP `401` | PASS |
| 申请强制结束 | 页面点击“申请强制结束”，提交原因和边界时间 | 只创建待审批任务，不关批 | HTTP `202`；task `PENDING_APPROVAL/r1`；batch `ACTIVE/r2` | PASS |
| 待审批页面 | 重新读取批次抽屉 | 显示申请人、原因、边界和审批动作 | 页面完整显示，`1600x1000` 无横向溢出 | PASS |
| 同人审批反证 | 同一 `admin` 点击批准 | 职责分离失败关闭 | HTTP `403`，提示必须由其他管理员完成 | PASS |
| 独立管理员批准 | 不同 `BPI_ADMIN` 调同一 command | 完成任务并按批准边界关批 | HTTP `202`；task `COMPLETED/r2`；batch `CLOSED_RAW/r3` | PASS |
| 完成态页面 | 刷新批次抽屉 | 不再显示申请/批准动作，时间线完整 | 显示 REQUESTED、CLOSED 两条事件，无动作按钮 | PASS |

同人审批产生一条浏览器 console 403 和一条 BPI HTTP 403，均与受控安全负测精确对应；
`unexpectedConsoleErrors=0`、`pageErrors=0`、`requestFailures=0`、
`unexpectedBpiHttpErrors=0`。

## PostgreSQL 验收

申请后执行：

```sql
\i deploy/docker/scripts/bpi-force-close-acceptance-verification.sql
```

结果摘要：

- batch 为 `ACTIVE/r2`，`end_time IS NULL`
- task 为 `PENDING_APPROVAL/r1`
- state event 和 audit 均只有 `BATCH_FORCE_CLOSE_REQUESTED`
- idempotency 行数为 1
- quality gate、WMS link、outbox 均为 0

独立批准后用同一查询复验：

- batch 为 `CLOSED_RAW/r3`，`end_time` 等于批准边界
- task 为 `COMPLETED/r2`
- `requested_by != decided_by`
- state event revision 为 `2,3`
- audit revision 为 `1->2,2->3`
- idempotency 行数为 2
- quality gate、WMS link、outbox 仍均为 0

目标表：

- `bpi.bpi_batch_instances`
- `bpi.bpi_batch_force_close_tasks`
- `bpi.bpi_batch_state_events`
- `bpi.bpi_audit_events`
- `bpi.bpi_api_idempotency`

清理脚本 `deploy/docker/scripts/bpi-force-close-acceptance-cleanup.sql` 返回
`residualRows=0`，没有删除非 marker 业务数据。

## 证据

- 机器记录：`metadata/bpi-force-close-target-acceptance.json`
- 待审批截图：`metadata/bpi-force-close-pending-target.png`
- 完成截图：`metadata/bpi-force-close-completed-target.png`
- 远端原始证据：`/home/v6/adp-evidence/ADP_E2E_20260721053253_BPI_FORCE_CLOSE`
- 浏览器报告 SHA-256：`d7c579ff30d91bcf909dead3eb427420ad4c00226a892083febdd985fdd70d7c`
- 待审批 PostgreSQL SHA-256：`bc55794c9a89be5dcd3685102230ad4d37b840b7bc7aabcfb5a677f7c8de4f16`
- 完成 PostgreSQL SHA-256：`e96baa02f51cdddcf1239820625ec9fbc6e5372e5d2a9924e092d4ed18654690`

## 未关闭范围

本轮证明受控人工兜底的产品和持久化合同，不把它解释为自动批次边界已经现场投产。物理设备
END 证据、正式身份系统中的第二管理员会话、真实连续 7-14 天、外部 QCS 以及外部 ERP/WMS
仍按 G-021 的生产准入门禁推进。
