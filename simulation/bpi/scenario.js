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

  const ruleBody = {
    code: 'RULE-S07-START', version: '1.2.0', topologyVersion: 'TOPO-S07@3',
    ast: {
      all: [{ signal: 'productionOrderReleased', equals: true }],
      quorum: { minimum: 2, of: ['feedPumpRunning', 'instantFlowAboveThreshold', 'targetTankLevelRising'] },
      holdSeconds: 15,
    },
  };

  const rule = {
    id: 'RULE-S07-START', ...ruleBody, state: 'DRAFT', revision: 7,
    checksum: sha256(ruleBody), latestSimulationId: null,
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
    batches: [],
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
