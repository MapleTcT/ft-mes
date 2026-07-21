# QCS 到 BPI 质量门目标验收

## 结论

2026-07-21 在唯一测试环境 `10.11.100.17` 完成真实 WOM/QCS 页面、QCS PostgreSQL 事务
outbox、Java 8 sidecar、三 broker Kafka、Java 17 BPI service 和 `ft_mes_bpi` PostgreSQL 联合验收。
最终结论为 `PASS_TARGET_QCS_BPI_REPLAY_CLEANED`。

本轮关闭的是“QCS 报告已生效，但 BPI 是否真的收到且重放安全”这一条软件合同。首发把影子批次从
`CLOSED_RAW/r1` 推进到 `RELEASED/r3`；随后把同一 outbox 重新排队，第二次仍发布同一 event、同一
payload SHA-256。BPI 的批次 revision、质量门、inbox、状态事件、审计和 WMS outbox 数量前后完全不变。

## 真实操作

| 模块 | 页面/路由 | 操作 | API | 前端结果 | 后端结果 | 数据库表 | 状态 | 问题 |
|---|---|---|---|---|---|---|---|---|
| WOM 生产请检 | `/msService/WOM/produceTask/produceTask/makeTaskList` | 打开 marker 制造任务并执行请检 | `POST /msService/WOM/produceTask/produceTask/createManuInspect` | HTTP 200，QCS 请检列表出现 `manuInspect_20260721_004`；console/page/request error 均为 0 | WOM 任务、待入库、执行日志和批次写入待检状态，QCS 请检单与标准明细落库 | WOM/QCS 任务、批次、请检、标准及工作流表 | PASS | QCS 列表仍显示既有 i18n key `ec.common.tableNo`，不影响本次写链但属于待修显示缺陷 |
| QCS 报告生效 | `/msService/QCS/inspect/inspect/manuInspectList`、`/msService/QCS/inspectReport/inspectReport/manuInspReportEdit` | 两段提交请检，保存合格结果，再两段提交报告生效 | `POST .../bulkSubmit`；`POST .../batchDealReports` | 所有业务请求 HTTP 200；报告页真实渲染 marker；四类浏览器错误均为 0 | 报告 `status=99/version=4/合格`，WOM 回写已检/合格，批次回写 qualified | QCS report/component/workflow，WOM task/wait/exelog，BaseSet batch | PASS | 生效后的只读页未回显“检验结论”，但 API 与 PostgreSQL report/component 均为合格，作为独立显示缺陷保留 |
| QCS 事务 outbox 首发 | 同一真实 QCS 报告生效事务 | 生效触发 outbox，sidecar 解析唯一批次并发布 Kafka | trigger/function；`GET /internal/bpi/v1/batches/resolve`；Kafka `qcs.batch.quality-gate.v1` | 不增加额外人工页面动作 | outbox `SENT/attempt=1`；BPI `RELEASED/r3`，质量门 `ACCEPTED`，gate/link/inbox `1/1/1`，状态事件/审计 `2/2` | `qcs_bpi_quality_gate_outbox`；BPI batch/gate/link/inbox/state/audit | PASS_TARGET | 无 |
| 同事件重放 | 不适用，运维复验动作 | 将同一 outbox 定向重排并等待第二次发布 | 同 topic、同 event/idempotency/payload | 不适用 | outbox `SENT/attempt=2`；BPI 重放前后投影完全相同，`idempotent=true`；Kafka replay delta=1、DLQ=0、lag=0 | 同上 | PASS_IDEMPOTENT | 仅允许当前质量门三元组完全一致的终态重放；终态新事件仍 DEAD |
| marker 退场 | 不适用 | 恢复 `.env` 和服务，定向清理 ADP/BPI marker | cleanup SQL；Compose restore | 页面恢复正常数据集 | ADP/BPI residual 均为 0；六个相关开关均为 false；两个新镜像 healthy | 上述表及临时 batch/feature flag | PASS_CLEANED | 无 |

## HTTP 与事件证据

- marker：`ADP_E2E_20260721021607_QCS_BPI`
- WOM task：`8990060021586247`
- QCS inspect/report：`768102577255680 / 768102615504128`
- QCS event：`qcs-gate:1000:768102577255680:4`
- BPI batch：`47bf07ff-4150-41a6-a76d-be974d97b280`
- payload SHA-256：`6f121f3b8375c523a8b16345b062e45c41db83fc176eeddc02526e1872b09fe7`
- scope：`tenant=1000 / plant=PLANT-01 / line=LINE-S07-01`
- 首发：topic delta `1`、outbox attempt `1`
- 重放：topic delta `1`、outbox attempt `2`、`idempotent=true`
- 最终：topic total delta `2`、DLQ delta `0`、consumer lag `0`

后端链路：

```text
QCSInspectReportServiceImpl.batchDealReports
  -> qcs_bpi_enqueue_quality_gate / qcs_bpi_quality_gate_outbox
  -> QcsQualityGateOutboxDispatcher
  -> HttpBpiBatchResolver
  -> KafkaQcsQualityGatePublisher
  -> qcs.batch.quality-gate.v1
  -> QcsQualityGateKafkaListener
  -> BatchReleaseService.applyQualityGate
  -> BatchReleasePostgresRepository
```

## PostgreSQL 证据

| 断言 | 首发后 | 重放后 |
|---|---:|---:|
| batch state / revision | `RELEASED / 3` | `RELEASED / 3` |
| quality gate | `ACCEPTED` | `ACCEPTED` |
| quality gate rows | `1` | `1` |
| quality link rows | `1` | `1` |
| inbox rows | `1` | `1` |
| state event rows | `2` | `2` |
| audit rows | `2` | `2` |
| WMS outbox rows | `0` | `0` |

可复验入口：

```bash
QCS_BPI_CONFIRM_TARGET=YES \
NODE_PATH="$PWD/frontend/apps/bpi/node_modules" \
make acceptance-qcs-bpi-quality-gate-target
```

夹具、验证和清理 SQL：

- `deploy/docker/scripts/bpi-qcs-quality-gate-target-fixture.sql`
- `deploy/docker/scripts/bpi-qcs-quality-gate-target-verification.sql`
- `deploy/docker/scripts/bpi-qcs-quality-gate-target-cleanup.sql`

## 构建与部署

- 部署 ID：`20260721T100828-e420c7a512c4`
- BPI image：`ft-mes-bpi-service:20260721T092245-40a2c642ffdb`
- QCS sidecar image：`ft-mes-qcs-quality-gate-outbox:20260721T100828-e420c7a512c4`
- BPI executable JAR SHA-256：`f279aa27a1d9ab0e8dc8c24f2dcfc8c72ec629695efd536acacb47bfebcfccb4`
- QCS sidecar JAR SHA-256：`8bd787548390abacbd89e25613bea0f1773537e25d238681d19b9fec6c2f0a81`
- Java 8 sidecar：`20/20`；contracts：`18/18`
- Java 17 BPI app：`81/81`；WMS adapter：`9/9`；rules：`9/9`
- 真实 `ft_mes_bpi` PostgreSQL V24：`8/8`

最终 `BPI_PHASE2_INTEGRATION_ENABLED`、Protobuf ingress、Kafka、WMS outbox、WMS adapter 和
`QCS_BPI_OUTBOX_ENABLED` 均恢复为 `false`。

## 边界与待确认清理

本轮使用 `SHADOW` 批次，因此按设计 `wmsStatus=NOT_REQUESTED` 且 WMS outbox 为 0。它证明真实 QCS
页面到 BPI 的首发和重放合同，不代表外部 QCS 自动事件已经按生产制度启用，也不代表外部 ERP/WMS、
冲销、补偿、物理测点或 7-14 天连续运行已完成。

一次测试连接曾误指向平台库 `adp`，在其中建立了未被运行时使用的 `bpi` schema；测试 tenant 行已清零，
实际运行库仍为 `ft_mes_bpi`。删除 schema 属于破坏性数据库操作，未在本轮自动执行，需单独确认后清理。

机器证据：`metadata/qcs-bpi-quality-gate-target-acceptance.json`。页面截图：
`metadata/qcs-bpi-quality-gate-target-wom.png`、`metadata/qcs-bpi-quality-gate-target-list.png`、
`metadata/qcs-bpi-quality-gate-target-report.png`。
