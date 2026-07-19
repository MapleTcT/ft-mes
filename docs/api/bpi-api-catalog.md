# BPI API 目录

状态：Phase 0/1 合约基线
权威合约：`contracts/bpi-api/openapi.json`、`contracts/bpi-api/asyncapi.json`
首期模拟范围：`contracts/bpi-api/simulation-profile.json`
真实服务范围：`contracts/bpi-api/service-phase1-profile.json`

## 1. 使用约定

- 浏览器只访问同源 `/bpi-api`；Java 8 适配器校验旧 Keycloak token 后，以短期内部 JWT 调用 Java 17 服务的 `/bpi/v1`。
- 所有写操作必须携带 `Idempotency-Key`、`If-Match` 和业务原因；缺少前置条件返回 `428`。
- revision 过期返回 `409 application/problem+json`，响应包含 `currentRevision`。
- Phase 1 只在 BPI 内创建 `shadow=true` 批次；START 确认创建 `ACTIVE`，END 确认关闭同一批次为
  `CLOSED_RAW`。它不写 WOM、QCS、WMS，也不控制 PLC/DCS。
- 列表响应固定 `snapshotAt`，大数据列表使用 cursor，不用页码推断实时数据位置。
- `SIMULATED` 表示本地确定性模拟器已实现并纳入自动测试，不表示真实 PostgreSQL/Kafka/Flink 已验收。
- 事件状态 `JOB_WIRED` 表示 Kafka/Flink 作业图、Harness 和事务 sink 已接线，不表示真实 broker、Flink HA、
  checkpoint storage 或端到端 PostgreSQL 已验收；只有完成实机证据后才能标记 `CLUSTER_ACCEPTED`。
- `LOCAL_FLINK_MINICLUSTER_KAFKA_ACCEPTED` 表示真实本地 Flink MiniCluster 和 Kafka 已验证 checkpoint
  事务可见性与任务重启恢复；它不包含目标集群、MinIO、浏览器或 PostgreSQL 联合链路。
- `SERVICE_IMPLEMENTED` 表示确定性模拟器和 Java 17/PostgreSQL 服务均已实现；它仍不等于目标环境浏览器联合验收。
- Java 17 服务当前实现 `service-phase1-profile.json` 中的 37 个公开操作，以及候选 JSON、候选 Protobuf、遥测 3 个内部接入端点；其余模拟操作仍不能视为后端已实现。

### 1.1 Java 8 适配器边界

- 入口：`/bpi-api/**`，实现位于 `backend/source-modules/batch-intelligence-adapter`。
- 旧 token 通过 Keycloak JWKS、issuer、audience 和时间声明校验；适配器不会把旧 Bearer 原样传给 Java 17 服务。
- `tenant_id` 只从受信 JWT claim 映射；`plant_ids`、`line_ids` 和 BPI roles 只来自服务端 subject/role 配置。浏览器自报的 tenant、plant、line header 一律不转发。
- 内部 JWT 使用固定 issuer/audience，TTL 不超过 15 分钟；浏览器永远看不到内部签名密钥。
- 上游地址固定为 `BPI_ADAPTER_UPSTREAM_BASE_URL`，客户端不能控制；普通请求体上限为 64 KiB，只有点位目录快照导入 `/point-catalog/snapshots` 可使用 5 MiB 上限。
- 当前允许 GET overview/line/candidate/batch/point-catalog/point-calibration/topology/rule/simulation 读取及 topology/rule 版本比较，以及 POST candidate confirm/reject、batch suspend/resume、point-catalog snapshot import、point-calibration submit/approve/reject/revoke、topology draft/validate/publish、rule draft/simulate/submit-approval/publish/reject-approval/retry/retire。Java 17 服务继续执行角色、租户、工厂、产线和功能开关校验。
- 缺失 subject scope、tenant 不匹配或无批准角色映射时 fail closed 返回 403。

## 2. 同步 API

| 模块 | Method | Path | operationId | Phase 1 |
|---|---|---|---|---|
| 实时态势 | GET | `/bpi/v1/overview` | `getBpiOverview` | SERVICE_IMPLEMENTED |
| 实时态势 | GET | `/bpi/v1/lines/{lineId}/current-state` | `getCurrentLineState` | SERVICE_IMPLEMENTED |
| 候选批次 | GET | `/bpi/v1/candidates` | `listBatchCandidates` | SERVICE_IMPLEMENTED |
| 候选批次 | GET | `/bpi/v1/candidates/{candidateId}` | `getBatchCandidate` | SERVICE_IMPLEMENTED |
| 候选批次 | POST | `/bpi/v1/candidates/{candidateId}/confirm` | `confirmBatchCandidate` | SERVICE_IMPLEMENTED |
| 候选批次 | POST | `/bpi/v1/candidates/{candidateId}/reject` | `rejectBatchCandidate` | SERVICE_IMPLEMENTED |
| 批次档案 | GET | `/bpi/v1/batches` | `listBatches` | SERVICE_IMPLEMENTED |
| 批次档案 | GET | `/bpi/v1/batches/{batchId}` | `getBatch` | SERVICE_IMPLEMENTED |
| 批次档案 | GET | `/bpi/v1/batches/{batchId}/evidence` | `getBatchEvidence` | SERVICE_IMPLEMENTED |
| 批次档案 | GET | `/bpi/v1/batches/{batchId}/balance` | `getBatchBalance` | SIMULATED |
| 批次档案 | GET | `/bpi/v1/batches/{batchId}/genealogy` | `getBatchGenealogy` | SIMULATED |
| 批次档案 | GET | `/bpi/v1/batches/{batchId}/timeline` | `getBatchTimeline` | SERVICE_IMPLEMENTED |
| 批次档案 | POST | `/bpi/v1/batches/{batchId}/suspend` | `suspendBatch` | SERVICE_IMPLEMENTED |
| 批次档案 | POST | `/bpi/v1/batches/{batchId}/resume` | `resumeBatch` | SERVICE_IMPLEMENTED |
| 批次档案 | POST | `/bpi/v1/batches/{batchId}/force-close` | `forceCloseBatch` | CONTRACT_ONLY |
| 批次档案 | POST | `/bpi/v1/batches/{batchId}/corrections` | `createBatchCorrection` | CONTRACT_ONLY |
| 点位准入 | GET | `/bpi/v1/point-catalog/snapshots` | `listPointCatalogSnapshots` | SERVICE_IMPLEMENTED |
| 点位准入 | GET | `/bpi/v1/point-catalog/current` | `getCurrentPointCatalog` | SERVICE_IMPLEMENTED；无分页参数保持旧版全量响应；显式分页使用不可变 snapshot + HMAC scope-bound keyset cursor，默认 100、上限 200，支持服务端 search |
| 点位准入 | POST | `/bpi/v1/point-catalog/snapshots` | `importPointCatalogSnapshot` | SERVICE_IMPLEMENTED |
| 校准治理 | GET | `/bpi/v1/point-calibrations` | `listPointCalibrations` | SERVICE_IMPLEMENTED；`snapshotAt` + scope-bound keyset cursor，默认 50、上限 200 |
| 校准治理 | POST | `/bpi/v1/point-calibrations` | `submitPointCalibration` | SERVICE_IMPLEMENTED |
| 校准治理 | POST | `/bpi/v1/point-calibrations/{calibrationId}/approve` | `approvePointCalibration` | SERVICE_IMPLEMENTED |
| 校准治理 | POST | `/bpi/v1/point-calibrations/{calibrationId}/reject` | `rejectPointCalibration` | SERVICE_IMPLEMENTED |
| 校准治理 | POST | `/bpi/v1/point-calibrations/{calibrationId}/revoke` | `revokePointCalibration` | SERVICE_IMPLEMENTED |
| 工艺拓扑 | GET | `/bpi/v1/topologies` | `listTopologies` | SERVICE_IMPLEMENTED |
| 工艺拓扑 | GET | `/bpi/v1/topologies/{topologyId}` | `getTopologyVersion` | SERVICE_IMPLEMENTED |
| 工艺拓扑 | GET | `/bpi/v1/topologies/{topologyId}/compare` | `compareTopologyVersions` | SERVICE_IMPLEMENTED |
| 工艺拓扑 | POST | `/bpi/v1/topologies/drafts` | `createTopologyDraft` | SERVICE_IMPLEMENTED |
| 工艺拓扑 | POST | `/bpi/v1/topologies/{topologyId}/validate` | `validateTopologyDraft` | SERVICE_IMPLEMENTED |
| 工艺拓扑 | POST | `/bpi/v1/topologies/{topologyId}/publish` | `publishTopologyVersion` | SERVICE_IMPLEMENTED |
| 边界规则 | GET | `/bpi/v1/rules` | `listRules` | SERVICE_IMPLEMENTED |
| 边界规则 | GET | `/bpi/v1/rules/{ruleId}` | `getRuleVersion` | SERVICE_IMPLEMENTED |
| 边界规则 | GET | `/bpi/v1/rules/{ruleId}/compare` | `compareRuleVersions` | SERVICE_IMPLEMENTED |
| 边界规则 | POST | `/bpi/v1/rules/drafts` | `createRuleDraft` | SERVICE_IMPLEMENTED |
| 边界规则 | POST | `/bpi/v1/rules/{ruleId}/simulate` | `simulateRule` | SERVICE_IMPLEMENTED |
| 边界规则 | GET | `/bpi/v1/rule-simulations/{simulationId}` | `getRuleSimulation` | SERVICE_IMPLEMENTED |
| 边界规则 | POST | `/bpi/v1/rules/{ruleId}/submit-approval` | `submitRuleApproval` | SERVICE_IMPLEMENTED |
| 边界规则 | POST | `/bpi/v1/rules/{ruleId}/reject-approval` | `rejectRuleApproval` | SERVICE_IMPLEMENTED |
| 边界规则 | POST | `/bpi/v1/rules/{ruleId}/publish` | `publishRuleVersion` | SERVICE_IMPLEMENTED |
| 边界规则 | POST | `/bpi/v1/rules/{ruleId}/publication/retry` | `retryRulePublication` | SERVICE_IMPLEMENTED |
| 边界规则 | POST | `/bpi/v1/rules/{ruleId}/retire` | `retireRuleVersion` | SERVICE_IMPLEMENTED |
| 数据质量 | GET | `/bpi/v1/data-quality/incidents` | `listDataQualityIncidents` | SIMULATED |
| 数据质量 | GET | `/bpi/v1/data-quality/incidents/{incidentId}` | `getDataQualityIncident` | SIMULATED |
| 数据质量 | POST | `/bpi/v1/data-quality/incidents/{incidentId}/acknowledge` | `acknowledgeDataQualityIncident` | CONTRACT_ONLY |
| 数据质量 | POST | `/bpi/v1/data-quality/incidents/{incidentId}/resolve` | `resolveDataQualityIncident` | CONTRACT_ONLY |
| 训练数据 | GET | `/bpi/v1/datasets` | `listDatasets` | CONTRACT_ONLY |
| 训练数据 | POST | `/bpi/v1/datasets/{datasetId}/snapshots` | `createDatasetSnapshot` | CONTRACT_ONLY |
| 训练数据 | GET | `/bpi/v1/dataset-snapshots/{snapshotId}` | `getDatasetSnapshot` | CONTRACT_ONLY |
| 集成运行 | GET | `/bpi/v1/integrations/health` | `getIntegrationHealth` | SIMULATED |
| 集成运行 | POST | `/bpi/v1/integrations/{integrationId}/checks` | `runIntegrationCheck` | CONTRACT_ONLY |
| 审计记录 | GET | `/bpi/v1/audit/events` | `listAuditEvents` | CONTRACT_ONLY |
| 审计记录 | GET | `/bpi/v1/audit/events/{auditId}` | `getAuditEvent` | CONTRACT_ONLY |

`confirmBatchCandidate` 按候选 `boundaryType` 执行两个受控事务：

- `START`：按 tenant/line 加事务锁，拒绝已有 `ACTIVE/SUSPENDED` 批次的重复启动，生成唯一影子批次；
- `END`：锁定同 tenant/plant/line/order 的 `ACTIVE` 批次，要求结束时间晚于开始时间，写入 END 证据并
  迁移到 `CLOSED_RAW`。两种路径都使用候选 revision、幂等键、功能开关和审计。

`importPointCatalogSnapshot` 只允许 `BPI_ADMIN` 通过受控 API 导入来自 JetLinks/exporter 的不可变状态快照，
不得由 BPI 直连或修改 JetLinks 数据库。每个点记录 product/device、JetLinks 原 `sourcePropertyId`、exporter
规范化 `propertyId`、设备激活与注册状态、属性存在性、
单位、校准版本/来源声明和源序列能力；同一请求可按幂等键安全重放。来源的 `calibrationStatus=VERIFIED`
只作为审计声明，不能放行点位。校准证据必须在 MES 中由工程师提交证书引用、SHA-256、校准版本和有效期，
再由非提交人管理员批准；过期、未生效、驳回或撤销后都动态变为 `UNVERIFIED`。设备或网关级 source epoch/sequence 是
P1 批次信号的硬准入条件，缺失时 `ready=false` 且拓扑校验返回 `POINT_SOURCE_SEQUENCE_DISABLED` error，
不能降级成 warning。`getCurrentPointCatalog` 用于页面和拓扑校验读取当前作用域快照，没有快照时明确返回空数据，
不能生成伪点位。

`createTopologyDraft` 支持全新版本和从已发布版本复制，拓扑定义包含节点、方向路径、JetLinks
`productId/deviceId/propertyId` 绑定、单位、校准版本和必需语义信号。`validateTopologyDraft` 在发布前同时校验悬空路径、
有向环、共享测点分配、重复信号、未绑定必需信号，以及当前点位快照中的 product/device/property、设备激活/注册、
单位和校准状态。通过后把结构化结果、拓扑 checksum、点位快照 ID/checksum、操作者和 revision 写入 PostgreSQL。
`publishTopologyVersion` 只允许独立于创建人的 `BPI_ADMIN` 发布校验通过、拓扑 checksum 未变化且仍钉扎同一
点位快照的草稿；发布后版本不可变。`createRuleDraft` 只引用已发布拓扑，并拒绝 AST 中未绑定的语义信号。

`simulateRule` 不是固定成功桩。它按规则作用域读取 PostgreSQL `bpi_telemetry_points` 的校准测点，调用与在线
候选相同的 `batch-rule-runtime`，再与 `bpi_rule_golden_boundaries` 的人工边界按容差匹配。空窗口、空金标准集
和超过 100,000 个观测值均返回 `422` 并回滚幂等预留。结果写入 `bpi_rule_simulations`，并把规则推进到
`SIMULATION_PASSED` 或退回 `DRAFT`。

`submitRuleApproval` 只接受当前规则最近一次 `PASSED` 模拟的 simulationId 和 checksum，并把规则、审批申请和
`RULE_APPROVAL_SUBMITTED` 审计在同一事务推进到 `PENDING_APPROVAL`。`publishRuleVersion` 只允许
`BPI_ADMIN` 处理仍为 `PENDING` 的申请，批准人必须同时不同于规则创建人和审批提交人；发布前重新核对当前
点位目录，revision、checksum、作用域、审批申请或模拟证明不匹配均 fail closed。成功后审批决定、规则、
`RULE_PUBLISHED` 审计和 Kafka outbox 事件在同一事务提交。`rejectRuleApproval` 由不同于提交人的管理员把申请
标记为 `REJECTED`，规则退回 `DRAFT`，必须重新模拟后才能再次提交。
`retryRulePublication` 仅允许 `BPI_ADMIN` 把 `FAILED` 事件重新入队；它使用独立、单调递增的发布 revision
执行并发控制，保留累计尝试/人工重试计数，并写入 `RULE_PUBLICATION_REQUEUED` 审计。

`retireRuleVersion` 仅允许 `BPI_ADMIN` 退役已经达到 Kafka `PUBLISHED`、Flink `APPLIED` 且运行态为
`READY` 或 `DEGRADED` 的规则。命令将规则推进到 `RETIRED`，同时追加新的 `active=false` 生命周期事件；
只有收到该事件对应的 `APPLIED` 与 `INACTIVE` 回执后，替代版本才允许发布。回滚通过从 `RETIRED`
历史版本创建新草稿完成，不会重新激活不可变旧版本。

`compareTopologyVersions` 和 `compareRuleVersions` 只比较同 tenant、同 code、同 plant/line 作用域的版本；
规则差异覆盖拓扑引用和 AST，拓扑差异覆盖受控 definition。返回稳定 JSON Pointer、增加/删除/修改类型和
前后值，最多返回前 500 项并显式标记 `truncated`，不允许跨作用域比较泄露配置。

规则响应同时返回 `publicationStatus` 与 `applicationStatus`。前者只证明 transactional outbox 到 Kafka broker 的
分发状态；后者由 `bpi.boundary.rule-application.v1` 回执驱动，包含 Flink deployment、观察/接收时间和拒绝错误。
只有 `applicationStatus=APPLIED` 才能被操作台解释为运行态已应用；`WAITING`、`REJECTED` 和 `NOT_TRACKED` 均不得
冒充在线生效。

## 3. 内部受信接入 API

| Method | Path | 调用方 | 权限 | 成功/隔离结果 | 持久化 |
|---|---|---|---|---|---|
| POST | `/internal/bpi/v1/candidates` | Flink/候选适配器 | `BPI_EVENT_INGEST` + tenant/plant/line scope | `201` 候选；重复事件返回原对象；冲突 `409` | inbox、candidate |
| POST | `/internal/bpi/v1/candidate-events` | Flink/Kafka consumer 的受控验收桥 | `BPI_EVENT_INGEST` + tenant/plant/line scope + 显式启用 | `201` 候选；非法 Protobuf/证据 `422`；跨 tenant `403` | inbox、candidate evidence/missing signals |
| POST | `/internal/bpi/v1/telemetry` | 仅受控 replay/验收工具 | `BPI_EVENT_INGEST` + tenant/plant/line scope + 显式启用 | `201` 接收；幂等重放 `200`；隔离 `202`；身份冲突 `409` | 短期 staging：event、point、point reject、source state、quarantine |

候选 Protobuf 入口消费 `BatchCandidateV1` wire bytes，并要求 v1 兼容新增的完整 `CandidateEvidenceV1`
快照。它默认关闭，只有设置 `BPI_CANDIDATE_PROTOBUF_HTTP_INGRESS_ENABLED=true` 才能用于 Flink 输出到
PostgreSQL 的受控 marker 验收；生产 Kafka consumer 已直接复用 `CandidateEventMapper` 和
`CandidateIngestionService`，HTTP bridge 不再承担长期数据平面职责。

遥测入口使用 `(tenantId,eventId)` 作为全局重放身份，并使用
`(tenantId,gatewayId,deviceId,sourceEpoch,sequence)` 约束源序列身份。一个 envelope 中的非法点只进入
`bpi_telemetry_point_rejects`，其余合法点仍可落入事实表；envelope 级错误和 source epoch 回退进入
`bpi_telemetry_quarantine`，不得按零值参与后续规则计算。该 HTTP 入口默认关闭，只有设置
`BPI_TELEMETRY_HTTP_INGRESS_ENABLED=true` 才能用于受控 replay 验收；它不属于生产数据平面，也不得由
JetLinks exporter 长期直连。生产路径仍是 `iot.telemetry.selected.v1` + Protobuf `TelemetryEnvelopeV1` + Flink，
原始测点权威数据保留在 JetLinks/时序库，BPI PostgreSQL 最终只保存事件索引、聚合和业务事实。

## 4. 事件 API

| Topic | Key | Protobuf message | 方向 | 当前状态 |
|---|---|---|---|---|
| `iot.telemetry.selected.v1` | `plantId+deviceId` | `TelemetryEnvelopeV1` | JetLinks/exporter -> BPI | JOB_WIRED |
| `mes.production.context.v1` | `orderId+taskId` | `ProductionContextEventV1` | WOM adapter -> BPI | JOB_WIRED |
| `bpi.boundary.rule-publication.v1` | `tenantId+lineId+ruleCode+ruleVersion` | `BoundaryRulePublicationV1` | BPI outbox -> Flink Broadcast State | JOB_WIRED |
| `bpi.boundary.rule-application.v1` | `publicationEventId` | `BoundaryRuleApplicationV1` | Flink exactly-once checkpoint sink -> BPI inbox/PostgreSQL/audit | LOCAL_FLINK_MINICLUSTER_KAFKA_ACCEPTED_POSTGRES_CHAIN_SEPARATE |
| `bpi.boundary.rule-application.dlq.v1` | 原 publication key | 原 `BoundaryRuleApplicationV1` bytes + DLT headers | BPI consumer -> 运维处置 | LOCAL_KAFKA_POSTGRES_ACCEPTED_TARGET_PENDING |
| `bpi.batch.candidate.v1` | `lineId+ruleCode` | `BatchCandidateV1` | Flink -> BPI Kafka consumer -> PostgreSQL | CONSUMER_WIRED_LIVE_BLOCKED |
| `bpi.batch.candidate.dlq.v1` | 原 partition/key | 原 `BatchCandidateV1` bytes + DLT headers | BPI consumer -> 运维处置 | CONSUMER_WIRED_LIVE_BLOCKED |
| `bpi.data-quality.v1` | `source+propertyId` | `DataQualityEventV1` | ingest/Flink -> BPI | JOB_WIRED |
| `bpi.batch.fact.v1` | `batchId` | `BatchFactV1` | BPI -> downstream | PHASE_2_RESERVED |
| `bpi.training.snapshot.v1` | `datasetId` | `TrainingSnapshotV1` | BPI -> ML pipeline | PHASE_3_RESERVED |

## 5. 首期模拟验收

模拟场景固定为 `PLANT-01 / LINE-S07-01`，从待确认 START 候选开始：

1. 查询实时态势、候选和证据。
2. 缺少命令头时确认候选，必须返回 `428`。
3. 使用 revision `3` 确认候选，生成唯一 `shadow=true` 批次。
4. 使用同一幂等键重试，返回相同结果且不生成第二个批次。
5. 使用旧 revision 和新幂等键重试，返回 `409` 和当前 revision `4`。
6. START 确认后生成独立 END 候选；确认 END 后同一批次变为 `CLOSED_RAW/r2`，记录 endTime、END 证据和
   `END_BOUNDARY_CONFIRMED`，相同 key 重放不重复关闭。
7. 独立场景暂停 `ACTIVE` 批次并确认变为 `SUSPENDED/r2`，重复暂停和跨命令复用 key 返回 `409`。
8. 恢复 `SUSPENDED` 批次并确认变为 `ACTIVE/r3`，时间线追加暂停和恢复事件。
9. 查询批次头、START/END 证据、平衡、谱系和 append-only 时间线。
10. 查询无点位快照时的 fail-closed 状态；导入点位目录快照并按相同幂等键重放，快照不能重复生成。
11. 查询点位快照列表和当前作用域点位，验证 ready/blocker 结果。
12. 查询作用域拓扑和测点绑定；只有绑定存在且 ready 的点位时校验通过，并记录快照 ID/checksum。
13. 回放规则，生成确定性 checksum；只有 checksum 匹配才能发布规则。
14. 查询数据质量影响和集成降级影响。

运行命令：

```bash
make bpi-api-contract-check
make bpi-simulation-test
```

模拟通过后的下一道门仍是真实 JetLinks/Kafka/Flink/PostgreSQL marker 和 UI 验收，不能用本报告替代。
