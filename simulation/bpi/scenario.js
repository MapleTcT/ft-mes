const { createHash } = require('node:crypto');

const FIXED_TIME = '2026-07-12T08:00:00.000Z';
const SOURCE_SEQUENCE_FINGERPRINT = `sha256:${'1'.repeat(64)}`;

function clone(value) {
  return structuredClone(value);
}

function canonicalJson(value) {
  if (Array.isArray(value)) {
    return `[${value.map(canonicalJson).join(',')}]`;
  }
  if (value && typeof value === 'object') {
    return `{${Object.keys(value).sort().map((key) => `${JSON.stringify(key)}:${canonicalJson(value[key])}`).join(',')}}`;
  }
  return JSON.stringify(value);
}

function sha256(value) {
  return createHash('sha256').update(canonicalJson(value)).digest('hex');
}

function stableEventId(value, index) {
  const digest = sha256(`${value}|${index}`);
  return `${digest.slice(0, 8)}-${digest.slice(8, 12)}-5${digest.slice(13, 16)}-a${digest.slice(17, 20)}-${digest.slice(20, 32)}`;
}

function createScenario() {
  const evidence = [
    {
      eventId: 'EVT-ORDER-001', signal: 'productionOrderReleased', classification: 'REQUIRED',
      satisfied: true, value: 'MO-20260712-001', unit: null, quality: 'GOOD',
      eventTime: '2026-07-12T07:59:20.000Z', source: 'WOM',
    },
    {
      eventId: 'EVT-PUMP-001', signal: 'feedPumpRunning', classification: 'QUORUM',
      satisfied: true, value: true, unit: null, quality: 'GOOD',
      eventTime: '2026-07-12T07:59:35.000Z', source: 'JetLinks',
    },
    {
      eventId: 'EVT-FLOW-001', signal: 'instantFlowAboveThreshold', classification: 'QUORUM',
      satisfied: true, value: 18.6, unit: 't/h', quality: 'GOOD',
      eventTime: '2026-07-12T07:59:40.000Z', source: 'JetLinks',
    },
    {
      eventId: 'EVT-TANK-001', signal: 'targetTankLevelRising', classification: 'QUORUM',
      satisfied: true, value: 42.3, unit: '%', quality: 'GOOD',
      eventTime: '2026-07-12T07:59:45.000Z', source: 'JetLinks',
    },
  ];

  const candidate = {
    id: 'CAND-START-S07-001',
    candidateKey: '50a52031-6428-5481-9d75-e27e330b0154',
    boundaryType: 'START',
    lineId: 'LINE-S07-01',
    orderId: 'MO-20260712-001',
    batchId: null,
    boundaryTime: '2026-07-12T07:59:40.000Z',
    state: 'PENDING',
    revision: 3,
    confidence: 0.94,
    ruleVersion: 'RULE-S07-START@1.2.0',
    topologyVersion: 'TOPO-S07@3',
    missingSignals: [],
    evidence,
    review: null,
  };

  const lineTelemetrySamples = [
    17.2, 17.6, 18.1, 18.4, 18.8, 19.0, 18.7, 18.5, 18.3, 18.4, 18.5, 18.6,
  ].map((value, index) => ({
    eventId: stableEventId('LINE-S07-01|flow.instant', index),
    signal: 'flow.instant',
    value: value.toFixed(1),
    numericValue: value,
    unit: 't/h',
    qualityCode: 'GOOD',
    sequenceDisposition: 'IN_ORDER',
    sampleTime: new Date(Date.parse('2026-07-12T07:48:58.000Z') + index * 60_000).toISOString(),
    calibrationVersion: 'CAL-1',
  }));

  const line = {
    plantId: 'PLANT-01', lineId: 'LINE-S07-01', lineName: 'S07 制糖线',
    status: 'BLOCKED', stageCode: 'EVAPORATION', orderId: 'MO-20260712-001',
    currentBatchId: null, confidence: 0.94, instantFlow: 18.6, totalizedQuantity: 12.4,
    dataHealth: 'BAD', pendingCandidates: 1, affectedRules: 5,
    lastEventTime: '2026-07-12T07:59:58.000Z',
    telemetry: {
      topologyBound: true,
      primarySignal: 'flow.instant',
      productId: 'PRODUCT-SUGAR',
      deviceId: 'DEVICE-S07-01',
      propertyId: 'flow.instant',
      value: '18.6',
      numericValue: 18.6,
      unit: 't/h',
      qualityCode: 'GOOD',
      sequenceOrigin: 'DEVICE',
      sequenceDisposition: 'IN_ORDER',
      sampleTime: '2026-07-12T07:59:58.000Z',
      calibrationVersion: 'CAL-1',
      lagSeconds: 2,
      fresh: true,
      expectedSignalCount: 2,
      observedSignalCount: 2,
      goodSignalCount: 2,
      openIncidentCount: 5,
      criticalIncidentCount: 1,
    },
  };

  const topology = {
    id: '9f73950f-5bc3-4d95-a504-90557905d17b', code: 'TOPO-S07', version: '3',
    state: 'PUBLISHED', revision: 1, plantId: 'PLANT-01', lineId: 'LINE-S07-01',
    checksum: sha256({ code: 'TOPO-S07', version: '3', lineId: 'LINE-S07-01' }),
    validationStatus: 'PASSED', validationErrors: [], validationWarnings: [],
    validatedBy: 'process.engineer', validatedAt: '2026-07-12T07:50:00.000Z',
    validatedPointCatalogSnapshotId: '4d9c5df8-7ee0-58e2-a143-9ca5d37a7b21',
    validatedPointCatalogChecksum: sha256({ sourceRevision: 'JETLINKS-S07-20260712T075000Z' }),
    publishedBy: 'bpi.admin', publishedAt: '2026-07-12T07:55:00.000Z',
    definition: {
      localityGroup: 'LOCALITY-S07-EVAP',
      stages: [{ code: 'EVAPORATION', name: '蒸发浓缩' }],
      nodes: [
        { code: 'PUMP-S07-FEED', type: 'PUMP', name: 'S07 进料泵' },
        { code: 'TANK-S07-TARGET', type: 'TANK', name: 'S07 接收罐' },
      ],
      edges: [{ from: 'PUMP-S07-FEED', to: 'TANK-S07-TARGET' }],
      bindings: [
        { signal: 'flow.instant', productId: 'PRODUCT-SUGAR', deviceId: 'DEVICE-S07-01', propertyId: 'flow.instant', unit: 't/h', calibrationVersion: 'CAL-1' },
        { signal: 'pump.running', productId: 'PRODUCT-SUGAR', deviceId: 'DEVICE-S07-01', propertyId: 'pump.running', unit: 'bool', calibrationVersion: 'CAL-1' },
      ],
      requiredSignals: ['flow.instant', 'pump.running'],
    },
  };

  const pointCatalogSnapshot = {
    id: '4d9c5df8-7ee0-58e2-a143-9ca5d37a7b21', source: 'JETLINKS',
    sourceInstance: 'jetlinks-simulator', sourceRevision: 'JETLINKS-S07-20260712T075000Z',
    plantId: 'PLANT-01', lineId: 'LINE-S07-01',
    checksum: sha256({ sourceRevision: 'JETLINKS-S07-20260712T075000Z' }),
    observedAt: '2026-07-12T07:50:00.000Z', pointCount: 2, readyPointCount: 2,
    importedBy: 'bpi.admin', importedAt: '2026-07-12T07:51:00.000Z',
  };
  const pointCalibrations = [
    {
      id: '10b34ec0-fda2-5f03-a1b9-1b9ee0fa9a01', plantId: 'PLANT-01', lineId: 'LINE-S07-01',
      productId: 'PRODUCT-SUGAR', deviceId: 'DEVICE-S07-01', propertyId: 'flow.instant',
      calibrationVersion: 'CAL-1', certificateReference: 'urn:ft-mes:calibration:flow-cal-1',
      certificateChecksum: 'a'.repeat(64), validFrom: '2026-01-01T00:00:00.000Z',
      validUntil: '2027-07-12T00:00:00.000Z', state: 'APPROVED', revision: 2,
      submittedBy: 'simulated.metrology.engineer', submittedAt: '2026-07-10T08:00:00.000Z',
      submitReason: '提交流量计受控校准证书', decidedBy: 'simulated.bpi.admin',
      decidedAt: '2026-07-10T09:00:00.000Z', decisionReason: '独立复核证书、校验和和有效期',
      revokedBy: null, revokedAt: null, revokeReason: null, effective: true, effectivenessStatus: 'EFFECTIVE',
    },
    {
      id: 'b1e15a8a-61ba-5859-aafb-22ad58c1f302', plantId: 'PLANT-01', lineId: 'LINE-S07-01',
      productId: 'PRODUCT-SUGAR', deviceId: 'DEVICE-S07-01', propertyId: 'pump.running',
      calibrationVersion: 'CAL-1', certificateReference: 'urn:ft-mes:calibration:pump-cal-1',
      certificateChecksum: 'b'.repeat(64), validFrom: '2026-01-01T00:00:00.000Z',
      validUntil: '2027-07-12T00:00:00.000Z', state: 'APPROVED', revision: 2,
      submittedBy: 'simulated.metrology.engineer', submittedAt: '2026-07-10T08:05:00.000Z',
      submitReason: '提交泵运行信号受控校准证书', decidedBy: 'simulated.bpi.admin',
      decidedAt: '2026-07-10T09:05:00.000Z', decisionReason: '独立复核证书、校验和和有效期',
      revokedBy: null, revokedAt: null, revokeReason: null, effective: true, effectivenessStatus: 'EFFECTIVE',
    },
  ];
  const pointCatalog = {
    snapshot: pointCatalogSnapshot,
    points: [
      {
        id: '39b0cd61-e506-507c-a107-4133517ab2e9', snapshotId: pointCatalogSnapshot.id,
        plantId: 'PLANT-01', lineId: 'LINE-S07-01', localityGroup: 'LOCALITY-S07-EVAP',
        productId: 'PRODUCT-SUGAR', deviceId: 'DEVICE-S07-01', propertyId: 'flow.instant', sourcePropertyId: 'instantFlow',
        pointName: '进料瞬时流量', unit: 't/h', dataType: 'double', deviceState: 'ACTIVE',
        registered: true, propertyPresent: true, calibrationVersion: 'CAL-1',
        sourceCalibrationStatus: 'VERIFIED', calibrationStatus: 'VERIFIED',
        calibrationEvidenceId: pointCalibrations[0].id, calibrationValidUntil: pointCalibrations[0].validUntil,
        sourceSequenceEnabled: true, sourceSequenceRequired: true, sourceSequenceOrigin: 'DEVICE',
        sourceSequenceBindingFingerprint: SOURCE_SEQUENCE_FINGERPRINT,
        sourceSequenceQualified: true, sourceSequenceEvidenceStatus: 'QUALIFIED',
        sourceSequenceEpoch: 7, sourceSequenceFirst: 1001, sourceSequenceLast: 1016,
        sourceSequenceObservationCount: 16,
        sourceSequenceFirstObservedAt: '2026-07-12T07:40:00.000Z',
        sourceSequenceLastObservedAt: '2026-07-12T07:50:10.000Z',
        sourceSequenceValidUntil: '2027-07-12T07:50:10.000Z',
        sourceSequenceEvidenceEventId: 'source-sequence-evidence-simulator-device-s07-01',
        sourceSequenceEvidenceRevision: 1,
        ready: true, readinessIssues: [],
      },
      {
        id: '27315888-e7ca-56da-a24c-f54e65a9ad91', snapshotId: pointCatalogSnapshot.id,
        plantId: 'PLANT-01', lineId: 'LINE-S07-01', localityGroup: 'LOCALITY-S07-EVAP',
        productId: 'PRODUCT-SUGAR', deviceId: 'DEVICE-S07-01', propertyId: 'pump.running', sourcePropertyId: 'pumpRunning',
        pointName: '进料泵运行', unit: 'bool', dataType: 'boolean', deviceState: 'ACTIVE',
        registered: true, propertyPresent: true, calibrationVersion: 'CAL-1',
        sourceCalibrationStatus: 'VERIFIED', calibrationStatus: 'VERIFIED',
        calibrationEvidenceId: pointCalibrations[1].id, calibrationValidUntil: pointCalibrations[1].validUntil,
        sourceSequenceEnabled: true, sourceSequenceRequired: true, sourceSequenceOrigin: 'DEVICE',
        sourceSequenceBindingFingerprint: SOURCE_SEQUENCE_FINGERPRINT,
        sourceSequenceQualified: true, sourceSequenceEvidenceStatus: 'QUALIFIED',
        sourceSequenceEpoch: 7, sourceSequenceFirst: 1001, sourceSequenceLast: 1016,
        sourceSequenceObservationCount: 16,
        sourceSequenceFirstObservedAt: '2026-07-12T07:40:00.000Z',
        sourceSequenceLastObservedAt: '2026-07-12T07:50:10.000Z',
        sourceSequenceValidUntil: '2027-07-12T07:50:10.000Z',
        sourceSequenceEvidenceEventId: 'source-sequence-evidence-simulator-device-s07-01',
        sourceSequenceEvidenceRevision: 1,
        ready: true, readinessIssues: [],
      },
    ],
  };

  const sourceSequenceEvidence = new Map([[
    ['JETLINKS', 'jetlinks-simulator', 'PLANT-01', 'LINE-S07-01', 'PRODUCT-SUGAR',
      'DEVICE-S07-01', SOURCE_SEQUENCE_FINGERPRINT].join('|'),
    {
      eventId: 'source-sequence-evidence-simulator-device-s07-01',
      status: 'QUALIFIED', sequenceOrigin: 'DEVICE', sourceEpoch: 7,
      firstSequence: 1001, lastSequence: 1016, observationCount: 16,
      firstObservedAt: '2026-07-12T07:40:00.000Z',
      lastObservedAt: '2026-07-12T07:50:10.000Z',
      validUntil: '2027-07-12T07:50:10.000Z',
      observedAt: '2026-07-12T07:50:10.000Z', revision: 1,
    },
  ]]);

  const ruleBody = {
    code: 'RULE-S07-START', version: '1.2.0', plantId: 'PLANT-01', lineId: 'LINE-S07-01',
    topologyVersion: 'TOPO-S07@3',
    ast: {
      boundaryType: 'START', quorumMinimum: 2, minimumConfidence: 0.8, maxCompositePenalty: 0.25,
      timing: { allowedLatenessSeconds: 30, watermarkDelaySeconds: 10, evaluationTimeoutSeconds: 300 },
      conditions: [
        { signal: 'flow.instant', operator: 'GREATER_THAN', threshold: 15, holdSeconds: 15, maxSilenceSeconds: 30, classification: 'QUORUM', weight: 55 },
        { signal: 'pump.running', operator: 'EQUALS_TRUE', holdSeconds: 15, maxSilenceSeconds: 30, classification: 'QUORUM', weight: 45 },
      ],
    },
  };

  const rule = {
    id: '78d57d90-fdc8-4a57-a660-a1ae73c2bc96', ...ruleBody, state: 'DRAFT', revision: 7,
    checksum: sha256(ruleBody), latestSimulationId: null,
    approvalId: null, approvalStatus: 'NOT_REQUESTED', approvalRevision: 0,
    approvalSubmittedBy: null, approvalSubmittedAt: null,
    approvalDecidedBy: null, approvalDecidedAt: null,
    lifecycleAction: 'NOT_PUBLISHED', lifecycleSequence: 0, lifecycleActive: false,
    publicationStatus: 'NOT_PUBLISHED', publicationRevision: 0,
    publicationAttemptCount: 0, publicationTotalAttemptCount: 0,
    publicationManualRetryCount: 0, publicationPublishedAt: null,
    publicationLastRequeuedAt: null, publicationLastError: null,
    applicationStatus: 'NOT_PUBLISHED', applicationDeploymentId: null,
    applicationObservedAt: null, applicationReceivedAt: null,
    applicationErrorCode: null, applicationErrorDetail: null,
    runtimeReadinessStatus: 'NOT_PUBLISHED', runtimeReadinessDeploymentId: null,
    runtimeReadinessObservedAt: null, runtimeReadinessReceivedAt: null,
    runtimeReadinessReasonCode: null, runtimeReadinessDetail: null,
    runtimePointCatalogEventId: null, runtimePointCatalogSourceRevision: null,
  };

  const dataQualityIncidents = [
    {
      id: '7d555b9d-2b34-5de9-9a44-2b7bb7550d01', issueCode: 'CLOCK_DRIFT', severity: 'CRITICAL', state: 'OPEN',
      deviceId: 'DEVICE-S07-01', propertyId: 'flow.instant', eventCount: 4,
      firstSeen: '2026-07-12T07:55:00.000Z', lastSeen: '2026-07-12T07:59:50.000Z',
      lastDetail: '设备事件时间比接收时间慢 127 秒，已超过边界计算允许偏差。',
      affectedRules: ['RULE-S07-START@1.2.0'], affectedBatches: ['S07-20260712-000'],
    },
    {
      id: 'e1946a89-523f-5cc8-a0b2-861a94f47c02', issueCode: 'REQUIRED_SIGNAL_UNAVAILABLE', severity: 'ERROR', state: 'OPEN',
      deviceId: 'DEVICE-S07-01', propertyId: 'tank.level', eventCount: 3,
      firstSeen: '2026-07-12T07:56:00.000Z', lastSeen: '2026-07-12T07:59:40.000Z',
      lastDetail: '目标罐液位信号连续三个采集周期不可用，启动边界证据不完整。',
      affectedRules: ['RULE-S07-START@1.2.0'], affectedBatches: [],
    },
    {
      id: '5639d3c1-7608-5b94-a97c-27cf036cf103', issueCode: 'UNKNOWN_UNIT', severity: 'WARNING', state: 'ACKNOWLEDGED',
      deviceId: 'DEVICE-S07-02', propertyId: 'density.baume', eventCount: 2,
      firstSeen: '2026-07-12T07:52:00.000Z', lastSeen: '2026-07-12T07:58:30.000Z',
      lastDetail: '原料波美值单位未映射到受控单位目录，干物量计算已暂停。',
      affectedRules: ['RULE-S07-MATERIAL-BALANCE@1.0.0'], affectedBatches: ['S07-20260712-000'], assignee: 'process.engineer',
    },
    {
      id: 'cc5eeed7-c88c-5d73-a3da-839baea48904', issueCode: 'SEQUENCE_GAP', severity: 'WARNING', state: 'OPEN',
      deviceId: 'EDGE-S07-01', propertyId: 'source.sequence', eventCount: 5,
      firstSeen: '2026-07-12T07:48:00.000Z', lastSeen: '2026-07-12T07:57:20.000Z',
      lastDetail: '来源序列号从 81220 跳至 81227，缺失六条测点事件。',
      affectedRules: ['RULE-S07-START@1.2.0'], affectedBatches: [],
    },
    {
      id: 'a8f9dcb7-adb3-51a1-bda9-81ab87c86005', issueCode: 'SHARED_METER_UNALLOCATED', severity: 'ERROR', state: 'ACKNOWLEDGED',
      deviceId: 'METER-STEAM-01', propertyId: 'steam.totalized', eventCount: 3,
      firstSeen: '2026-07-12T07:45:00.000Z', lastSeen: '2026-07-12T07:56:50.000Z',
      lastDetail: '共享蒸汽表缺少当前生产指令的分摊键，能源耗用未写入批次平衡。',
      affectedRules: ['RULE-S07-ENERGY-ALLOC@1.0.0'], affectedBatches: ['S07-20260712-000'], assignee: 'energy.engineer',
    },
    {
      id: '4bb6b18f-c374-5aa3-84e0-e22f73edc706', issueCode: 'BUFFER_ALERT', severity: 'WARNING', state: 'RESOLVED',
      deviceId: 'FLINK-BPI-01', propertyId: 'consumer.lag', eventCount: 2,
      firstSeen: '2026-07-12T07:30:00.000Z', lastSeen: '2026-07-12T07:40:00.000Z',
      lastDetail: '数据质量消费积压超过告警阈值，扩容后已恢复。',
      affectedRules: ['RULE-S07-START@1.2.0'], affectedBatches: [], assignee: 'platform.engineer',
    },
  ].map((item) => {
    const acknowledged = ['ACKNOWLEDGED', 'RESOLVED'].includes(item.state);
    const resolved = item.state === 'RESOLVED';
    return {
      revision: resolved ? 3 : acknowledged ? 2 : 1,
      plantId: 'PLANT-01', lineId: 'LINE-S07-01', source: 'boundary-evaluation',
      affectedLines: ['LINE-S07-01'], affectedBatchCount: item.affectedBatches.length,
      assignee: item.assignee || null,
      acknowledgedBy: acknowledged ? 'simulated.shift.lead' : null,
      acknowledgedAt: acknowledged ? '2026-07-12T07:57:00.000Z' : null,
      acknowledgmentReason: acknowledged ? '确认业务影响并分派责任人处理' : null,
      resolvedBy: resolved ? 'simulated.shift.lead' : null,
      resolvedAt: resolved ? '2026-07-12T07:45:00.000Z' : null,
      resolutionReason: resolved ? '扩容消费任务并确认积压清零' : null,
      ...item,
    };
  });
  const incident = dataQualityIncidents[0];
  const dataQualityEvents = new Map(dataQualityIncidents.map((item) => [item.id,
    Array.from({ length: item.eventCount }, (_, index) => ({
      eventId: stableEventId(item.id, index),
      sourceEventId: `DQ-SOURCE-${item.issueCode}-${index + 1}`,
      severity: item.severity,
      detail: index === item.eventCount - 1 ? item.lastDetail : `${item.lastDetail}（重复观测 ${index + 1}）`,
      detectedAt: new Date(Date.parse(item.firstSeen) + index * 60_000).toISOString(),
      receivedAt: new Date(Date.parse(item.firstSeen) + index * 60_000 + 2_000).toISOString(),
      headers: {
        tenant_id: 'TENANT-01', plant_id: item.plantId, line_id: item.lineId,
        stage_code: 'EVAPORATION', issue_code: item.issueCode,
        rule_key: item.affectedRules[0] || '', batch_no: item.affectedBatches[0] || '',
      },
    }))]));
  const dataQualityLifecycle = new Map(dataQualityIncidents.map((item) => {
    const actions = [{
      revision: 1, action: 'CREATED', fromState: null, toState: 'OPEN', actorId: 'system.data-quality',
      assignee: null, reason: 'Kafka 数据质量事件首次聚合', at: item.firstSeen,
    }];
    if (['ACKNOWLEDGED', 'RESOLVED'].includes(item.state)) actions.push({
      revision: 2, action: 'ACKNOWLEDGED', fromState: 'OPEN', toState: 'ACKNOWLEDGED', actorId: item.acknowledgedBy,
      assignee: item.assignee, reason: item.acknowledgmentReason, at: item.acknowledgedAt,
    });
    if (item.state === 'RESOLVED') actions.push({
      revision: 3, action: 'RESOLVED', fromState: 'ACKNOWLEDGED', toState: 'RESOLVED', actorId: item.resolvedBy,
      assignee: item.assignee, reason: item.resolutionReason, at: item.resolvedAt,
    });
    return [item.id, actions];
  }));
  const dataQualityRecommendations = new Map(dataQualityIncidents.map((item) => [item.id, [
    '核对来源设备、网关与服务器时钟或测点元数据。',
    '确认受影响规则和批次是否需要暂停自动处理。',
    '修复后观察连续三个采集周期，并保留复核依据。',
  ]]));

  return {
    line,
    lineTelemetrySamples,
    candidate,
    endCandidate: null,
    batches: [],
    batchEvents: [],
    batchEventsById: new Map(),
    batchEvidenceById: new Map(),
    batchReleases: new Map(),
    wmsInboundReversalTasks: new Map(),
    forceCloseTasks: new Map(),
    shadowRuns: [],
    shadowRunReviews: [],
    telemetryEvents: [],
    topology,
    topologies: [topology],
    pointCatalog,
    pointCatalogHistory: [pointCatalog],
    sourceSequenceEvidence,
    pointCalibrations,
    rule,
    rules: [rule],
    simulations: new Map(),
    incident,
    dataQualityIncidents,
    dataQualityEvents,
    dataQualityLifecycle,
    dataQualityRecommendations,
    runtimeReadinessReceipts: new Map(),
    datasetDefinitions: [],
    datasetSnapshots: [],
    pendingDatasetSnapshotIds: new Set(),
    datasetMaterializations: [],
    pendingDatasetMaterializationIds: new Set(),
    datasetCatalogPublications: [],
    pendingDatasetCatalogPublicationIds: new Set(),
    datasetRetentionArchives: [],
    pendingDatasetRetentionArchiveIds: new Set(),
    datasetMlflowRegistrations: [],
    pendingDatasetMlflowRegistrationIds: new Set(),
    integrations: [
      { id: 'jetlinks-exporter', status: 'UP', businessImpact: '关键测点可用', lastSuccessAt: FIXED_TIME, lag: 1.2, revision: 5 },
      { id: 'kafka', status: 'UP', businessImpact: '事件总线正常', lastSuccessAt: FIXED_TIME, lag: 0.4, revision: 8 },
      { id: 'timescaledb', status: 'DEGRADED', businessImpact: '历史曲线可能延迟，不阻断批次事实查询', lastSuccessAt: '2026-07-12T07:58:00.000Z', lag: 120, revision: 3 },
      { id: 'wom-adapter', status: 'UP', businessImpact: '生产指令上下文有效', lastSuccessAt: FIXED_TIME, lag: 2.1, revision: 4 },
    ],
    featureFlags: [
      { id: '00000000-0000-0000-0000-000000000001', tenantId: '*', scopeType: 'GLOBAL', scopeKey: '*', flagKey: 'bpi.ui', enabled: false, active: true, revision: 1, updatedBy: 'flyway', updatedAt: FIXED_TIME, lastReason: 'Global default deny' },
      { id: '00000000-0000-0000-0000-000000000002', tenantId: '*', scopeType: 'GLOBAL', scopeKey: '*', flagKey: 'bpi.commands', enabled: false, active: true, revision: 1, updatedBy: 'flyway', updatedAt: FIXED_TIME, lastReason: 'Global default deny' },
      { id: '00000000-0000-0000-0000-000000000003', tenantId: '*', scopeType: 'GLOBAL', scopeKey: '*', flagKey: 'bpi.shadow-only', enabled: true, active: true, revision: 1, updatedBy: 'flyway', updatedAt: FIXED_TIME, lastReason: 'Phase 1 invariant' },
      { id: '00000000-0000-0000-0000-000000000004', tenantId: '*', scopeType: 'GLOBAL', scopeKey: '*', flagKey: 'bpi.rule-management', enabled: false, active: true, revision: 1, updatedBy: 'flyway', updatedAt: FIXED_TIME, lastReason: 'Global default deny' },
      { id: '00000000-0000-0000-0000-000000000005', tenantId: '*', scopeType: 'GLOBAL', scopeKey: '*', flagKey: 'bpi.auto-confirm', enabled: false, active: true, revision: 1, updatedBy: 'flyway', updatedAt: FIXED_TIME, lastReason: 'Phase 1 lock' },
      { id: '00000000-0000-0000-0000-000000000006', tenantId: '*', scopeType: 'GLOBAL', scopeKey: '*', flagKey: 'bpi.wms-link', enabled: false, active: true, revision: 1, updatedBy: 'flyway', updatedAt: FIXED_TIME, lastReason: 'Phase 1 lock' },
      { id: '3d26fd79-dd3a-5b47-ae5b-7eef90ce2101', tenantId: 'TENANT-01', scopeType: 'LINE', scopeKey: 'LINE-S07-01', flagKey: 'bpi.commands', enabled: true, active: true, revision: 1, updatedBy: 'bpi.admin', updatedAt: FIXED_TIME, lastReason: 'Enable pilot line commands' },
      { id: 'e977f6d8-dc94-5b2f-a5b0-bd4207f80802', tenantId: 'TENANT-01', scopeType: 'LINE', scopeKey: 'LINE-S07-01', flagKey: 'bpi.rule-management', enabled: true, active: true, revision: 1, updatedBy: 'bpi.admin', updatedAt: FIXED_TIME, lastReason: 'Enable pilot line rule management' },
    ],
    featureFlagAudits: [],
    idempotency: new Map(),
  };
}

module.exports = { FIXED_TIME, canonicalJson, clone, createScenario, sha256 };
