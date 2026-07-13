const { createHash } = require('node:crypto');

const FIXED_TIME = '2026-07-12T08:00:00.000Z';

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

  const line = {
    lineId: 'LINE-S07-01', lineName: 'S07 制糖线', status: 'RUNNING', stageCode: 'EVAPORATION',
    orderId: 'MO-20260712-001', currentBatchId: null, confidence: 0.94,
    instantFlow: 18.6, totalizedQuantity: 12.4, dataHealth: 'PARTIAL', pendingCandidates: 1,
    affectedRules: 1, lastEventTime: '2026-07-12T07:59:58.000Z',
  };

  const topology = {
    id: '9f73950f-5bc3-4d95-a504-90557905d17b', code: 'TOPO-S07', version: '3',
    state: 'PUBLISHED', revision: 1, plantId: 'PLANT-01', lineId: 'LINE-S07-01',
    checksum: sha256({ code: 'TOPO-S07', version: '3', lineId: 'LINE-S07-01' }),
    definition: {
      localityGroup: 'LOCALITY-S07-EVAP',
      stages: [{ code: 'EVAPORATION', name: '蒸发浓缩' }],
      nodes: [
        { code: 'PUMP-S07-FEED', type: 'PUMP', name: 'S07 进料泵' },
        { code: 'TANK-S07-TARGET', type: 'TANK', name: 'S07 接收罐' },
      ],
      bindings: [
        { signal: 'flow.instant', deviceId: 'DEVICE-S07-01', propertyId: 'flow.instant', unit: 't/h', calibrationVersion: 'CAL-1' },
        { signal: 'pump.running', deviceId: 'DEVICE-S07-01', propertyId: 'pump.running', unit: 'bool', calibrationVersion: 'CAL-1' },
      ],
    },
  };

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
    publicationStatus: 'NOT_PUBLISHED', publicationRevision: 0,
    publicationAttemptCount: 0, publicationTotalAttemptCount: 0,
    publicationManualRetryCount: 0, publicationPublishedAt: null,
    publicationLastRequeuedAt: null, publicationLastError: null,
  };

  const incident = {
    id: 'DQ-S07-FLOW-001', issueCode: 'CLOCK_DRIFT', severity: 'WARNING', state: 'OPEN', revision: 2,
    source: 'JetLinks', propertyId: 'flow.instant', affectedLines: ['LINE-S07-01'],
    affectedRules: ['RULE-S07-START'], affectedBatches: [], eventCount: 4,
    firstSeen: '2026-07-12T07:55:00.000Z', lastSeen: '2026-07-12T07:59:50.000Z',
  };

  return {
    line,
    candidate,
    endCandidate: null,
    batches: [],
    batchEvents: [],
    topology,
    rule,
    simulations: new Map(),
    incident,
    integrations: [
      { id: 'jetlinks-exporter', status: 'UP', businessImpact: '关键测点可用', lastSuccessAt: FIXED_TIME, lag: 1.2, revision: 5 },
      { id: 'kafka', status: 'UP', businessImpact: '事件总线正常', lastSuccessAt: FIXED_TIME, lag: 0.4, revision: 8 },
      { id: 'timescaledb', status: 'DEGRADED', businessImpact: '历史曲线可能延迟，不阻断批次事实查询', lastSuccessAt: '2026-07-12T07:58:00.000Z', lag: 120, revision: 3 },
      { id: 'wom-adapter', status: 'UP', businessImpact: '生产指令上下文有效', lastSuccessAt: FIXED_TIME, lag: 2.1, revision: 4 },
    ],
    idempotency: new Map(),
  };
}

module.exports = { FIXED_TIME, canonicalJson, clone, createScenario, sha256 };
