# BPI API 目录

状态：Phase 0/1 合约基线
权威合约：`contracts/bpi-api/openapi.json`、`contracts/bpi-api/asyncapi.json`
首期模拟范围：`contracts/bpi-api/simulation-profile.json`
真实服务范围：`contracts/bpi-api/service-phase1-profile.json`

## 1. 使用约定

- 浏览器只访问同源 `/bpi-api`；Java 8 适配器校验旧 Keycloak token 后，以短期内部 JWT 调用 Java 17 服务的 `/bpi/v1`。
- 所有写操作必须携带 `Idempotency-Key`、`If-Match` 和业务原因；缺少前置条件返回 `428`。
- revision 过期返回 `409 application/problem+json`，响应包含 `currentRevision`。
- Phase 1 只创建 `shadow=true` 的 BPI 批次，不写 WOM、QCS、WMS，也不控制 PLC/DCS。
- 列表响应固定 `snapshotAt`，大数据列表使用 cursor，不用页码推断实时数据位置。
- `SIMULATED` 表示本地确定性模拟器已实现并纳入自动测试，不表示真实 PostgreSQL/Kafka/Flink 已验收。
- Java 17 服务当前只实现 `service-phase1-profile.json` 中的 9 个公开操作，以及候选 JSON、候选 Protobuf、遥测 3 个内部接入端点；其余模拟操作仍不能视为后端已实现。

### 1.1 Java 8 适配器边界

- 入口：`/bpi-api/**`，实现位于 `backend/source-modules/batch-intelligence-adapter`。
- 旧 token 通过 Keycloak JWKS、issuer、audience 和时间声明校验；适配器不会把旧 Bearer 原样传给 Java 17 服务。
- `tenant_id` 只从受信 JWT claim 映射；`plant_ids`、`line_ids` 和 BPI roles 只来自服务端 subject/role 配置。浏览器自报的 tenant、plant、line header 一律不转发。
- 内部 JWT 使用固定 issuer/audience，TTL 不超过 15 分钟；浏览器永远看不到内部签名密钥。
- 上游地址固定为 `BPI_ADAPTER_UPSTREAM_BASE_URL`，客户端不能控制；请求体上限为 64 KiB。
- 当前允许 GET overview/line/candidate/batch 读取，以及 POST candidate confirm。未在白名单中的 Phase 2/3 契约即使存在于 OpenAPI 也返回 403。
- 缺失 subject scope、tenant 不匹配或无批准角色映射时 fail closed 返回 403。

## 2. 同步 API

| 模块 | Method | Path | operationId | Phase 1 |
|---|---|---|---|---|
| 实时态势 | GET | `/bpi/v1/overview` | `getBpiOverview` | SIMULATED |
| 实时态势 | GET | `/bpi/v1/lines/{lineId}/current-state` | `getCurrentLineState` | SIMULATED |
| 候选批次 | GET | `/bpi/v1/candidates` | `listBatchCandidates` | SIMULATED |
| 候选批次 | GET | `/bpi/v1/candidates/{candidateId}` | `getBatchCandidate` | SIMULATED |
| 候选批次 | POST | `/bpi/v1/candidates/{candidateId}/confirm` | `confirmBatchCandidate` | SIMULATED |
| 候选批次 | POST | `/bpi/v1/candidates/{candidateId}/reject` | `rejectBatchCandidate` | CONTRACT_ONLY |
| 批次档案 | GET | `/bpi/v1/batches` | `listBatches` | SIMULATED |
| 批次档案 | GET | `/bpi/v1/batches/{batchId}` | `getBatch` | SIMULATED |
| 批次档案 | GET | `/bpi/v1/batches/{batchId}/evidence` | `getBatchEvidence` | SIMULATED |
| 批次档案 | GET | `/bpi/v1/batches/{batchId}/balance` | `getBatchBalance` | SIMULATED |
| 批次档案 | GET | `/bpi/v1/batches/{batchId}/genealogy` | `getBatchGenealogy` | SIMULATED |
| 批次档案 | GET | `/bpi/v1/batches/{batchId}/timeline` | `getBatchTimeline` | SIMULATED |
| 批次档案 | POST | `/bpi/v1/batches/{batchId}/suspend` | `suspendBatch` | CONTRACT_ONLY |
| 批次档案 | POST | `/bpi/v1/batches/{batchId}/resume` | `resumeBatch` | CONTRACT_ONLY |
| 批次档案 | POST | `/bpi/v1/batches/{batchId}/force-close` | `forceCloseBatch` | CONTRACT_ONLY |
| 批次档案 | POST | `/bpi/v1/batches/{batchId}/corrections` | `createBatchCorrection` | CONTRACT_ONLY |
| 工艺拓扑 | GET | `/bpi/v1/topologies` | `listTopologies` | CONTRACT_ONLY |
| 工艺拓扑 | GET | `/bpi/v1/topologies/{topologyId}` | `getTopologyVersion` | CONTRACT_ONLY |
| 工艺拓扑 | POST | `/bpi/v1/topologies/drafts` | `createTopologyDraft` | CONTRACT_ONLY |
| 工艺拓扑 | POST | `/bpi/v1/topologies/{topologyId}/validate` | `validateTopologyDraft` | CONTRACT_ONLY |
| 工艺拓扑 | POST | `/bpi/v1/topologies/{topologyId}/publish` | `publishTopologyVersion` | CONTRACT_ONLY |
| 边界规则 | GET | `/bpi/v1/rules` | `listRules` | SIMULATED |
| 边界规则 | GET | `/bpi/v1/rules/{ruleId}` | `getRuleVersion` | SIMULATED |
| 边界规则 | POST | `/bpi/v1/rules/drafts` | `createRuleDraft` | CONTRACT_ONLY |
| 边界规则 | POST | `/bpi/v1/rules/{ruleId}/simulate` | `simulateRule` | SIMULATED |
| 边界规则 | GET | `/bpi/v1/rule-simulations/{simulationId}` | `getRuleSimulation` | SIMULATED |
| 边界规则 | POST | `/bpi/v1/rules/{ruleId}/publish` | `publishRuleVersion` | SIMULATED |
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

## 3. 内部受信接入 API

| Method | Path | 调用方 | 权限 | 成功/隔离结果 | 持久化 |
|---|---|---|---|---|---|
| POST | `/internal/bpi/v1/candidates` | Flink/候选适配器 | `BPI_EVENT_INGEST` + tenant/plant/line scope | `201` 候选；重复事件返回原对象；冲突 `409` | inbox、candidate |
| POST | `/internal/bpi/v1/candidate-events` | Flink/Kafka consumer 的受控验收桥 | `BPI_EVENT_INGEST` + tenant/plant/line scope + 显式启用 | `201` 候选；非法 Protobuf/证据 `422`；跨 tenant `403` | inbox、candidate evidence/missing signals |
| POST | `/internal/bpi/v1/telemetry` | 仅受控 replay/验收工具 | `BPI_EVENT_INGEST` + tenant/plant/line scope + 显式启用 | `201` 接收；幂等重放 `200`；隔离 `202`；身份冲突 `409` | 短期 staging：event、point、point reject、source state、quarantine |

候选 Protobuf 入口消费 `BatchCandidateV1` wire bytes，并要求 v1 兼容新增的完整 `CandidateEvidenceV1`
快照。它默认关闭，只有设置 `BPI_CANDIDATE_PROTOBUF_HTTP_INGRESS_ENABLED=true` 才能用于 Flink 输出到
PostgreSQL 的受控 marker 验收；生产 Kafka consumer 完成后应直接复用 `CandidateEventMapper` 和
`CandidateIngestionService`，不把该 HTTP bridge 作为长期数据平面。

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
| `iot.telemetry.selected.v1` | `plantId+deviceId` | `TelemetryEnvelopeV1` | JetLinks/exporter -> BPI | CONTRACT |
| `mes.production.context.v1` | `orderId+taskId` | `ProductionContextEventV1` | WOM adapter -> BPI | CONTRACT |
| `bpi.batch.candidate.v1` | `lineId+ruleCode` | `BatchCandidateV1` | Flink -> BPI API | CONTRACT |
| `bpi.data-quality.v1` | `source+propertyId` | `DataQualityEventV1` | ingest/Flink -> BPI | CONTRACT |
| `bpi.batch.fact.v1` | `batchId` | `BatchFactV1` | BPI -> downstream | PHASE_2_RESERVED |
| `bpi.training.snapshot.v1` | `datasetId` | `TrainingSnapshotV1` | BPI -> ML pipeline | PHASE_3_RESERVED |

## 5. 首期模拟验收

模拟场景固定为 `PLANT-01 / LINE-S07-01`，从待确认 START 候选开始：

1. 查询实时态势、候选和证据。
2. 缺少命令头时确认候选，必须返回 `428`。
3. 使用 revision `3` 确认候选，生成唯一 `shadow=true` 批次。
4. 使用同一幂等键重试，返回相同结果且不生成第二个批次。
5. 使用旧 revision 和新幂等键重试，返回 `409` 和当前 revision `4`。
6. 查询批次头、证据、平衡、谱系和 append-only 时间线。
7. 回放规则，生成确定性 checksum；只有 checksum 匹配才能发布规则。
8. 查询数据质量影响和集成降级影响。

运行命令：

```bash
make bpi-api-contract-check
make bpi-simulation-test
```

模拟通过后的下一道门仍是真实 JetLinks/Kafka/Flink/PostgreSQL marker 和 UI 验收，不能用本报告替代。
