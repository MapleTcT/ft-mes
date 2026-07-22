# BPI API 目录

状态：Phase 0/1 合约基线
权威合约：`contracts/bpi-api/openapi.json`、`contracts/bpi-api/asyncapi.json`
首期模拟范围：`contracts/bpi-api/simulation-profile.json`
真实服务范围：`contracts/bpi-api/service-phase1-profile.json`、`contracts/bpi-api/service-phase2-profile.json`

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
- Java 17 服务当前实现 `service-phase1-profile.json` 中的 54 个公开操作，以及候选 JSON、候选 Protobuf、遥测 3 个内部接入端点；其余模拟操作仍不能视为后端已实现。
- 运行开关按 `GLOBAL < TENANT < PLANT < LINE` 解析。租户 API 只允许管理员写 TENANT/PLANT/LINE 覆盖，平台默认值不可改；`bpi.shadow-only`、`bpi.auto-confirm`、`bpi.wms-link` 在 Phase 1 锁定，`bpi.ui` 在旧平台导航接入前只读。

### 1.1 Java 8 适配器边界

- 入口：`/bpi-api/**`，实现位于 `backend/source-modules/batch-intelligence-adapter`。
- 旧 token 通过 Keycloak JWKS、issuer、audience 和时间声明校验；适配器不会把旧 Bearer 原样传给 Java 17 服务。
- `tenant_id` 只从受信 JWT claim 映射；`plant_ids`、`line_ids` 和 BPI roles 只来自服务端 subject/role 配置。浏览器自报的 tenant、plant、line header 一律不转发。
- 内部 JWT 使用固定 issuer/audience，TTL 不超过 15 分钟；浏览器永远看不到内部签名密钥。
- 上游地址固定为 `BPI_ADAPTER_UPSTREAM_BASE_URL`，客户端不能控制；普通请求体上限为 64 KiB，只有点位目录快照导入 `/point-catalog/snapshots` 可使用 5 MiB 上限。
- 当前允许 GET overview/line/candidate/batch/point-catalog/point-calibration/topology/rule/simulation/data-quality/shadow-run/feature-flags 读取及 topology/rule 版本比较，以及 POST candidate confirm/reject、batch suspend/resume、point-catalog snapshot import、point-calibration submit/approve/reject/revoke、topology draft/validate/publish、rule draft/simulate/submit-approval/publish/reject-approval/retry/retire、data-quality acknowledge/resolve、shadow-run create/start/review/complete/approve/reject/cancel、feature-flag override set/inherit。Java 17 服务继续执行角色、租户、工厂、产线和功能开关校验。
- 缺失 subject scope、tenant 不匹配或无批准角色映射时 fail closed 返回 403。

## 2. 同步 API

| 模块 | Method | Path | operationId | Phase 1 |
|---|---|---|---|---|
| 实时态势 | GET | `/bpi/v1/overview` | `getBpiOverview` | SERVICE_IMPLEMENTED |
| 实时态势 | GET | `/bpi/v1/lines/{lineId}/current-state` | `getCurrentLineState` | SERVICE_IMPLEMENTED |
| 运行治理 | GET | `/bpi/v1/feature-flags` | `listFeatureFlags` | SERVICE_IMPLEMENTED；同时返回有效值/来源和选中作用域覆盖 |
| 运行治理 | POST | `/bpi/v1/feature-flags/{flagKey}` | `changeFeatureFlagOverride` | SERVICE_IMPLEMENTED；仅 BPI_ADMIN，可 SET 或 INHERIT，乐观锁和审计落库 |
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
| 批次档案 | GET | `/bpi/v1/batches/{batchId}/release` | `getBatchRelease` | SERVICE_IMPLEMENTED；读取 QCS gate/inspection、WMS 蓝单与最近冲销任务 projection，Phase 2 写入口仍默认关闭 |
| 批次档案 | POST | `/bpi/v1/batches/{batchId}/wms/reconcile` | `reconcileWmsInbound` | SERVICE_IMPLEMENTED；仅 BPI_ADMIN，可对超出安全等待期的 PENDING 原命令执行 query-first 同指令重排，event/payload/WMS 幂等键不可变 |
| 批次档案 | GET | `/bpi/v1/batches/{batchId}/wms/reversal` | `getWmsInboundReversalTask` | SERVICE_IMPLEMENTED；返回最近一次追加式完工入库冲销任务或 null |
| 批次档案 | POST | `/bpi/v1/batches/{batchId}/wms/reversal` | `commandWmsInboundReversal` | SERVICE_IMPLEMENTED；班长/管理员先 REQUEST，另一名 BPI_ADMIN APPROVE 后才追加独立红单 outbox |
| 批次档案 | POST | `/bpi/v1/batches/{batchId}/suspend` | `suspendBatch` | SERVICE_IMPLEMENTED |
| 批次档案 | POST | `/bpi/v1/batches/{batchId}/resume` | `resumeBatch` | SERVICE_IMPLEMENTED |
| 批次档案 | GET | `/bpi/v1/batches/{batchId}/force-close` | `getBatchForceCloseTask` | SERVICE_IMPLEMENTED；恢复最近一次申请/审批任务，供刷新和请求超时后确认真实结果 |
| 批次档案 | POST | `/bpi/v1/batches/{batchId}/force-close` | `forceCloseBatch` | SERVICE_IMPLEMENTED；班长/管理员先 REQUEST，另一名 BPI_ADMIN 使用原边界 APPROVE，审批前不关闭批次 |
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
| 数据质量 | GET | `/bpi/v1/data-quality/incidents` | `listDataQualityIncidents` | SERVICE_IMPLEMENTED；影响批次、严重度、最后发生时间排序，HMAC scope-bound snapshot-cutoff keyset cursor；截点后变化需刷新 |
| 数据质量 | GET | `/bpi/v1/data-quality/summary` | `getDataQualitySummary` | SERVICE_IMPLEMENTED |
| 数据质量 | GET | `/bpi/v1/data-quality/incidents/{incidentId}` | `getDataQualityIncident` | SERVICE_IMPLEMENTED；原始事件、生命周期和推荐动作 |
| 数据质量 | POST | `/bpi/v1/data-quality/incidents/{incidentId}/acknowledge` | `acknowledgeDataQualityIncident` | SERVICE_IMPLEMENTED；确认或重新分派 |
| 数据质量 | POST | `/bpi/v1/data-quality/incidents/{incidentId}/resolve` | `resolveDataQualityIncident` | SERVICE_IMPLEMENTED；仅 ACKNOWLEDGED 可解决 |
| 影子验收 | GET | `/bpi/v1/shadow-runs` | `listShadowRuns` | SERVICE_IMPLEMENTED；按受信 scope/state 读取，默认 100、上限 200 |
| 影子验收 | POST | `/bpi/v1/shadow-runs` | `createShadowRun` | SERVICE_IMPLEMENTED；钉扎 PUBLISHED rule、topology 和 point-catalog snapshot，时长 7-14 天 |
| 影子验收 | GET | `/bpi/v1/shadow-runs/{shadowRunId}` | `getShadowRun` | SERVICE_IMPLEMENTED；运行时重新计算就绪、指标和 blocker |
| 影子验收 | GET | `/bpi/v1/shadow-runs/{shadowRunId}/batch-reviews` | `listShadowRunBatchReviews` | SERVICE_IMPLEMENTED；默认只返回 ACTIVE 人工复核，可显式包含 SUPERSEDED |
| 影子验收 | POST | `/bpi/v1/shadow-runs/{shadowRunId}/batch-reviews` | `reviewShadowRunBatch` | SERVICE_IMPLEMENTED；只接受同 scope/版本且在运行窗口内的 CLOSED_RAW shadow batch |
| 影子验收 | POST | `/bpi/v1/shadow-runs/{shadowRunId}/start` | `startShadowRun` | SERVICE_IMPLEMENTED；publication/APPLIED/runtime READY/topology/current operational point catalog 全部通过才启动 |
| 影子验收 | POST | `/bpi/v1/shadow-runs/{shadowRunId}/complete` | `completeShadowRun` | SERVICE_IMPLEMENTED；最低时长与批次样本数通过才进入 EVALUATING |
| 影子验收 | POST | `/bpi/v1/shadow-runs/{shadowRunId}/approve` | `approveShadowRun` | SERVICE_IMPLEMENTED；独立管理员审批，全部指标和 CRITICAL 数据质量门通过才批准 |
| 影子验收 | POST | `/bpi/v1/shadow-runs/{shadowRunId}/reject` | `rejectShadowRun` | SERVICE_IMPLEMENTED；独立管理员可在 EVALUATING 驳回并保留证据 |
| 影子验收 | POST | `/bpi/v1/shadow-runs/{shadowRunId}/cancel` | `cancelShadowRun` | SERVICE_IMPLEMENTED；仅 DRAFT/RUNNING，可审计取消 |
| 训练数据 | GET | `/bpi/v1/datasets` | `listDatasets` | SERVICE_IMPLEMENTED；按 tenant/plant scope 返回不可变定义及最近快照 |
| 训练数据 | POST | `/bpi/v1/datasets` | `createDatasetDefinition` | SERVICE_IMPLEMENTED；仅允许受控特征/标签、固定 prediction time/cutoff/split policy |
| 训练数据 | POST | `/bpi/v1/datasets/{datasetId}/snapshots` | `createDatasetSnapshot` | SERVICE_IMPLEMENTED；202 后台任务，只接受冻结时点前已批准且有复核的影子运行 |
| 训练数据 | GET | `/bpi/v1/dataset-snapshots/{snapshotId}` | `getDatasetSnapshot` | SERVICE_IMPLEMENTED；返回 point-in-time manifest、checksum、样本排除原因和失败状态 |
| 训练数据 | POST | `/bpi/v1/dataset-snapshots/{snapshotId}/materializations` | `requestDatasetMaterialization` | SERVICE_IMPLEMENTED；仅 `MANIFEST_READY` 快照可请求 PARQUET，任务进入 `QUEUED` |
| 训练数据 | GET | `/bpi/v1/dataset-materializations/{materializationId}` | `getDatasetMaterialization` | SERVICE_IMPLEMENTED；返回 `QUEUED/WRITING/READY/FAILED`、精确对象版本 URI、SHA、行数和 schema 证据 |
| 训练数据 | POST | `/bpi/v1/dataset-materializations/{materializationId}/retry` | `retryDatasetMaterialization` | SERVICE_IMPLEMENTED；仅失败任务可按当前 revision 幂等重排队 |
| 训练数据 | POST | `/bpi/v1/dataset-materializations/{materializationId}/catalog-publications` | `requestDatasetCatalogPublication` | TARGET_ACCEPTED；只接受已校验、非空 `READY` Parquet，按服务端固定 catalog/namespace/table 合同进入 `QUEUED`；真实 ADP 桌面/移动发布、失败重试、Iceberg time-travel、post-commit fencing 恢复和清理已验收，运行开关默认关闭 |
| 训练数据 | GET | `/bpi/v1/dataset-materializations/{materializationId}/catalog-publications` | `getDatasetCatalogPublicationForMaterialization` | SERVICE_IMPLEMENTED；按源快照 tenant/plant/line scope 返回该 materialization 的独立 publication，不由 Parquet READY 推导 Iceberg READY |
| 训练数据 | GET | `/bpi/v1/dataset-catalog-publications/{publicationId}` | `getDatasetCatalogPublication` | SERVICE_IMPLEMENTED；仅 `READY` 返回 table identifier、snapshot ID、metadata location、schema/spec、行数和 checksum 对账事实 |
| 训练数据 | POST | `/bpi/v1/dataset-catalog-publications/{publicationId}/retry` | `retryDatasetCatalogPublication` | SERVICE_IMPLEMENTED；仅 `FAILED` revision 可重试，publisher 必须先 reconcile 带同一 BPI source properties 的既有 snapshot；目标 post-commit fencing 故障注入待验收 |
| 训练数据 | GET | `/bpi/v1/dataset-catalog-publications/{publicationId}/retention-archives` | `getDatasetRetentionArchiveForPublication` | SERVICE_IMPLEMENTED；按 publication tenant/plant/line scope 查询独立恢复包，不从 Iceberg READY 推导 Object Lock 已生效 |
| 训练数据 | POST | `/bpi/v1/dataset-catalog-publications/{publicationId}/retention-archives` | `requestDatasetRetentionArchive` | SERVICE_IMPLEMENTED；仅接受行数与语义 checksum 已对账的 `READY` Iceberg publication，保留模式、期限和目标桶由服务端固定 |
| 训练数据 | GET | `/bpi/v1/dataset-retention-archives/{archiveId}` | `getDatasetRetentionArchive` | SERVICE_IMPLEMENTED；返回 `QUEUED/ARCHIVING/VERIFYING/LOCKED/FAILED`、精确对象版本、保留期限与恢复校验证据 |
| 训练数据 | POST | `/bpi/v1/dataset-retention-archives/{archiveId}/retry` | `retryDatasetRetentionArchive` | SERVICE_IMPLEMENTED；仅失败任务可按当前 revision 幂等重排队，`LOCKED` 恢复包不可覆盖 |
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

数据质量生产入口是默认关闭的 `bpi.data-quality.v1` Protobuf consumer。它要求精确 topic/key/headers、
payload 与 headers 身份一致、事件时间不超前，并使用显式 tenant/plant/line allowlist；未配置时 `_DENY_ALL_`
失败关闭。有效事件按 tenant/plant/line/stage/source/device/property/issue 聚合，原始事件、生命周期和审计均
append-only。已解决事件只有在新事件 `detectedAt > resolvedAt` 时重新打开；旧迟到事件只保留事实，不改变状态。
本地 Java 17 + PostgreSQL + Embedded Kafka 已通过，目标环境部署和真实 Flink 数据质量 topic 仍待验收。

影子验收不是定时把规则“自动转正”。`createShadowRun` 固定规则、拓扑和点位目录证据；`startShadowRun`
实时复核发布、Flink 应用、运行态和 MES 校准证据。运行中每个 `reviewShadowRunBatch` 保存自动/人工起止边界、
偏差秒数、自动/参考数量、单位和复核人，重复复核采用 `ACTIVE/SUPERSEDED` 保留历史。`completeShadowRun`
只检查最短连续时长与样本量；`approveShadowRun` 再检查边界人工认同率不低于配置值且最低为 95%、累计数量偏差、
未解决 CRITICAL 数据质量事件和独立审批。任何一项失败都返回 `422`，不会写 WOM、QCS、WMS、PLC 或 DCS。
受控时间压缩测试只能验证状态机、API 和 PostgreSQL 证据，不能替代真实 7-14 天现场连续运行。

Phase 2 质量放行采用 QCS 的完整、单调 revision snapshot，而不是根据单条检验结果或接口 `200`
推断合格。批次先从 `CLOSED_RAW` 进入 `WAIT_QA`；全部 required inspection 同时 final/accepted
才进入 `RELEASED`，任一 required final/rejected 则进入 `REJECTED`。只有非影子批次且精确 scope 的
QCS/WMS 两个 flag 都启用时，RELEASED 事务才会追加一个确定性 WMS command outbox。WMS 回执必须
对应已经 `PUBLISHED` 的 command；只有 accepted 且带 durable `document_id` 才能进入 `INBOUNDED`。
代码、本地 PostgreSQL V23 及目标受控链均已验收。目标链使用认证内部 QCS Protobuf marker、三 broker
Kafka、query-first WMS adapter 和目标 `material-wms`，完成真实单据/明细/库存事务/批次库存落表、
durable receipt、`INBOUNDED/r4`、相同 QCS 重放、强制 Kafka command 重放和真实浏览器读取。验收后
所有 Phase 2 开关和 allowlist 均恢复关闭。`reconcileWmsInbound` 只处理“单据可能已创建但回执未知”或
outbox 终态失败：复用原 `commandEventId`、protobuf payload 和 WMS 幂等键，受管理员权限、
`Idempotency-Key`、WMS link `If-Match`、安全等待期和审计保护；它不是红字冲销，也不能重试 WMS
明确 `REJECTED` 的业务回执。QCS PostgreSQL 事务 outbox 和 Java 8 sidecar 已实现且默认关闭：它从
QCS report -> inspect -> WOM task -> 受控 BPI binding 获取唯一 scope，通过内部 resolver 将外部生产订单
精确关联到一个批次后才发布 Kafka；零条、多条、缺映射或快照矛盾均失败关闭。目标环境真实 QCS 页面到
Kafka/BPI/PostgreSQL 的 marker 首发与同事件重放验收已经完成，取证后开关恢复关闭且双库 marker 残留为 0。
resolver 同时返回当前质量门的外部 ID、revision 和 source event ID，
仅当三者与待发布 outbox 完全一致时，sidecar 才允许终态批次重放；终态上的新事件仍进入 DEAD，不能据此宣称
外部 QCS 主动事件已投产。该次 QCS 目标验收使用影子批次，按设计没有生成 WMS command；后续受控批次已
补齐内部 `material-wms` 正向入库、服务停机/DLQ/恢复和蓝单/红单整链。外部 ERP/WMS 冲销与外部实例
故障补偿仍未激活。QCS 证据见 `docs/testing/qcs-bpi-quality-gate-target-acceptance.md`，内部 WMS 整链见
`docs/testing/bpi-formal-identity-wms-roundtrip-acceptance.md`。

WMS adapter 的 query-first 合同还要求“创建响应不确定”后立即以原幂等键再查一次：查到且单据头、
明细、物料、批次、仓库、库位、数量、单位和质量状态全部一致才发送 accepted receipt；查无或查单失败
继续抛 transient 交给 Kafka 重试，不能转换成业务拒绝。4xx 业务错误也必须先查单，排除外部已提交但
返回冲突后才能发送 rejected receipt。普通入库与红字冲销软件协议矩阵已通过 `28/28` adapter
测试，证据见 `docs/testing/bpi-external-wms-protocol-contract-acceptance.md`；真实外部实例仍未验收。

内部 `material-wms` 已补充追加式完工入库冲销持久化合同。adapter 只允许通过
`POST /material/wms/completion-inbound-reversals` 创建 BPI 红字单，并通过
`GET /material/wms/completion-inbound-reversals/by-idempotency` 精确查单。请求必须携带新的 command
event、独立 idempotency key、原入库单号以及与原单完全一致的物料、批次、仓库、库位、数量和单位；
库存已消耗或事实冲突时整事务失败，原蓝字单仍为 `POSTED`。本地持久化合同为 `12/12 PASS`，证据见
`docs/testing/material-wms-completion-inbound-reversal-contract-acceptance.md`。独立 Protobuf command/receipt、
query-first adapter、四个隔离主题、BPI 四眼审批、V25 事务 outbox、接受/拒绝回执与 PostgreSQL 状态机
均已完成本地验收。目标 marker `ADP_BPI_FORMAL_WMS_REVERSAL_20260721190630` 又通过两个真实 ADP
会话、隔离 Kafka、目标 WMS adapter、目标 `material-wms` 和 BPI/material 两个 PostgreSQL 库，闭合
蓝单 `INBOUNDED/r4`、申请/职责分离审批、红单 durable receipt、`INBOUND_REVERSED/r7`、净库存归零、
浏览器终态与定向清理。完整证据见
`docs/testing/bpi-formal-identity-wms-roundtrip-acceptance.md`。该结论只代表内部 `material-wms`，
外部 ERP/WMS 端点仍未联调，因此不改变 Phase 2 默认关闭和 `G-021 PARTIAL` 结论。

## 3. 内部受信接入 API

| Method | Path | 调用方 | 权限 | 成功/隔离结果 | 持久化 |
|---|---|---|---|---|---|
| GET | `/internal/bpi/v1/batches/resolve` | QCS quality-gate sidecar | `BPI_INTEGRATION_INGEST` + tenant/plant/line scope + Phase 2 allowlist + 该 scope 的 `bpi.qcs-link` | 唯一匹配 `200`，并返回当前质量门外部 ID/revision/source event ID；无匹配 `404`；同 scope/order 多批次 `409`；未启用或越权 `403` | 只读，不落库 |
| POST | `/internal/bpi/v1/candidates` | Flink/候选适配器 | `BPI_EVENT_INGEST` + tenant/plant/line scope | `201` 候选；重复事件返回原对象；冲突 `409` | inbox、candidate |
| POST | `/internal/bpi/v1/candidate-events` | Flink/Kafka consumer 的受控验收桥 | `BPI_EVENT_INGEST` + tenant/plant/line scope + 显式启用 | `201` 候选；非法 Protobuf/证据 `422`；跨 tenant `403` | inbox、candidate evidence/missing signals |
| POST | `/internal/bpi/v1/telemetry` | 仅受控 replay/验收工具 | `BPI_EVENT_INGEST` + tenant/plant/line scope + 显式启用 | `201` 接收；幂等重放 `200`；隔离 `202`；身份冲突 `409` | 短期 staging：event、point、point reject、source state、quarantine |
| POST | `/internal/bpi/v1/qcs-quality-gates` | QCS adapter 的受控 Protobuf bridge | `BPI_INTEGRATION_INGEST` + tenant/plant/line scope + Phase 2/QCS 双门禁 | `201` projection；非法 snapshot `422`；重放冲突 `409`；未启用 `403` | inbox、quality gate/link、batch state、audit、可选 WMS outbox |
| POST | `/internal/bpi/v1/wms-inbound-receipts` | WMS adapter 的受控 Protobuf bridge | `BPI_INTEGRATION_INGEST` + tenant/plant/line scope + Phase 2 门禁 | `201` projection；PUBLISHED 前回执 `409`；未知状态或 accepted 缺 document id `422` | inbox、WMS link、batch state、audit |
| POST | `/internal/bpi/v1/wms-inbound-reversal-receipts` | WMS adapter 的受控红单 Protobuf bridge | `BPI_INTEGRATION_INGEST` + tenant/plant/line scope + Phase 2 门禁 | `201` task；红单 PUBLISHED 前回执 `409`；accepted 缺 red document id 或 rejected 缺 error code 返回 `422` | inbox、reversal task、batch state、audit；原蓝单不变 |

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
| `bpi.data-quality.v1` | `tenantId|lineId|sourceEventId|propertyId|issueCode` | `DataQualityEventV1`；payload 另含 `plantId/deviceId/headers.stage` | ingest/Flink -> BPI consumer/PostgreSQL | LOCAL_KAFKA_POSTGRES_ACCEPTED_TARGET_PENDING |
| `bpi.data-quality.dlq.v1` | 原 partition/key | 原 `DataQualityEventV1` bytes + DLT headers | BPI consumer -> 运维处置 | LOCAL_KAFKA_POSTGRES_ACCEPTED_TARGET_PENDING |
| `qcs.batch.quality-gate.v1` | `batchId|qualityGateId` | `QcsQualityGateV1` | QCS PostgreSQL outbox/Java 8 sidecar -> BPI inbox/PostgreSQL | TARGET_QCS_PAGE_KAFKA_POSTGRES_ACCEPTED_REPLAY_CLEANED_DISABLED_BY_DEFAULT |
| `qcs.batch.quality-gate.dlq.v1` | 原 partition/key | 原 `QcsQualityGateV1` bytes + DLT headers | BPI consumer -> 运维处置 | TARGET_ZERO_DLQ_ACCEPTED_DISABLED_BY_DEFAULT |
| `bpi.wms.completion-inbound-command.v1` | `tenantId|plantId|batchId` | `WmsCompletionInboundCommandV1` | BPI transactional outbox -> WMS adapter | TARGET_CONTROLLED_KAFKA_MATERIAL_WMS_ACCEPTED_DISABLED_BY_DEFAULT |
| `bpi.wms.completion-inbound-command.dlq.v1` | 原 partition/key | 原 `WmsCompletionInboundCommandV1` bytes + DLT headers | WMS adapter -> 运维处置 | TARGET_TOPIC_CREATED_DISABLED_BY_DEFAULT |
| `wms.completion-inbound.receipt.v1` | `commandEventId` | `WmsCompletionInboundReceiptV1` | WMS adapter -> BPI inbox/PostgreSQL | TARGET_CONTROLLED_KAFKA_POSTGRES_ACCEPTED_DISABLED_BY_DEFAULT |
| `wms.completion-inbound.receipt.dlq.v1` | 原 partition/key | 原 `WmsCompletionInboundReceiptV1` bytes + DLT headers | BPI consumer -> 运维处置 | TARGET_TOPIC_CREATED_DISABLED_BY_DEFAULT |
| `bpi.wms.completion-inbound-reversal-command.v1` | `tenantId|plantId|batchId` | `WmsCompletionInboundReversalCommandV1` | BPI transactional outbox -> WMS adapter | TARGET_INTERNAL_MATERIAL_WMS_KAFKA_ACCEPTED_DISABLED_BY_DEFAULT |
| `bpi.wms.completion-inbound-reversal-command.dlq.v1` | 原 partition/key | 原 `WmsCompletionInboundReversalCommandV1` bytes + DLT headers | WMS adapter -> 运维处置 | TARGET_ISOLATED_ZERO_DLQ_ACCEPTED_DISABLED_BY_DEFAULT |
| `wms.completion-inbound-reversal.receipt.v1` | `commandEventId` | `WmsCompletionInboundReversalReceiptV1` | WMS adapter -> BPI inbox/PostgreSQL | TARGET_INTERNAL_MATERIAL_WMS_DURABLE_RECEIPT_ACCEPTED_DISABLED_BY_DEFAULT |
| `wms.completion-inbound-reversal.receipt.dlq.v1` | 原 partition/key | 原 `WmsCompletionInboundReversalReceiptV1` bytes + DLT headers | BPI consumer -> 运维处置 | TARGET_ISOLATED_ZERO_DLQ_ACCEPTED_DISABLED_BY_DEFAULT |
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
14. 查询六类数据质量影响汇总、筛选队列、原始事件和业务影响。
15. 对同一事件执行确认分派、重新分派和解决，验证 revision、幂等重放、原始事件不删除和生命周期追加。
16. 查询集成降级影响。

运行命令：

```bash
make bpi-api-contract-check
make bpi-simulation-test
```

模拟通过后的下一道门仍是真实 JetLinks/Kafka/Flink/PostgreSQL marker 和 UI 验收，不能用本报告替代。
