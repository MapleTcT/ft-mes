const assert = require('node:assert/strict');
const { after, before, test } = require('node:test');
const path = require('node:path');
const fs = require('node:fs');
const { createBpiSimulator, listen } = require('./server');

const profile = JSON.parse(fs.readFileSync(path.join(__dirname, '../../contracts/bpi-api/simulation-profile.json'), 'utf8'));
const RULE_ID = '78d57d90-fdc8-4a57-a660-a1ae73c2bc96';
const covered = new Set();
let server;
let baseUrl;

async function request(method, route, { headers = {}, body } = {}) {
  const response = await fetch(`${baseUrl}${route}`, {
    method,
    headers: body === undefined ? headers : { 'Content-Type': 'application/json', ...headers },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const operationId = response.headers.get('x-bpi-operation-id');
  if (profile.operationIds.includes(operationId)) covered.add(operationId);
  return { response, json: await response.json() };
}

function commandHeaders(key, revision) {
  return { 'Idempotency-Key': key, 'If-Match': String(revision) };
}

before(async () => {
  ({ server } = createBpiSimulator());
  const address = await listen(server);
  baseUrl = `http://127.0.0.1:${address.port}`;
});

after(async () => {
  await new Promise((resolve, reject) => server.close((error) => (error ? reject(error) : resolve())));
  assert.deepEqual([...covered].sort(), [...profile.operationIds].sort(), 'every simulated operation must be exercised');
});

test('candidate confirmation creates exactly one idempotent shadow batch', async () => {
  let result = await request('GET', '/bpi/v1/overview?plantId=PLANT-01');
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data[0].pendingCandidates, 1);

  result = await request('GET', '/bpi/v1/lines/LINE-S07-01/current-state');
  assert.equal(result.json.data.currentBatchId, null);

  result = await request('GET', '/bpi/v1/candidates?plantId=PLANT-01');
  assert.equal(result.json.data.length, 1);

  result = await request('GET', '/bpi/v1/candidates/CAND-START-S07-001');
  assert.equal(result.json.data.revision, 3);
  assert.equal(result.json.data.evidence.filter((item) => item.satisfied).length, 4);

  result = await request('POST', '/bpi/v1/candidates/CAND-START-S07-001/confirm', { body: { reason: '班长确认启动边界' } });
  assert.equal(result.response.status, 428);

  const headers = commandHeaders('confirm-candidate-0001', 3);
  result = await request('POST', '/bpi/v1/candidates/CAND-START-S07-001/confirm', {
    headers,
    body: { reason: '班长确认启动边界' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.candidate.revision, 4);
  assert.equal(result.json.data.batch.shadow, true);
  const firstResponse = result.json;

  result = await request('POST', '/bpi/v1/candidates/CAND-START-S07-001/confirm', {
    headers,
    body: { reason: '班长确认启动边界' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.deepEqual(result.json, firstResponse);

  result = await request('POST', '/bpi/v1/candidates/CAND-START-S07-001/confirm', {
    headers: commandHeaders('confirm-candidate-stale-0002', 3),
    body: { reason: '旧页面重复确认' },
  });
  assert.equal(result.response.status, 409);
  assert.equal(result.json.currentRevision, 4);

  result = await request('GET', '/bpi/v1/batches?plantId=PLANT-01');
  assert.equal(result.json.data.length, 1);
  const batchId = result.json.data[0].id;

  result = await request('GET', `/bpi/v1/batches/${batchId}`);
  assert.equal(result.json.data.batchNo, 'S07-20260712-001');

  result = await request('GET', `/bpi/v1/batches/${batchId}/evidence`);
  assert.equal(result.json.data.start.length, 4);
  assert.equal(result.json.data.end.length, 0);

  result = await request('GET', '/bpi/v1/candidates?plantId=PLANT-01&state=PENDING');
  assert.equal(result.json.data.length, 1);
  assert.equal(result.json.data[0].boundaryType, 'END');

  result = await request('GET', `/bpi/v1/batches/${batchId}/balance`);
  assert.equal(result.json.data.status, 'WITHIN_TOLERANCE');

  result = await request('GET', `/bpi/v1/batches/${batchId}/genealogy`);
  assert.equal(result.json.data.nodes.length, 1);

  result = await request('GET', `/bpi/v1/batches/${batchId}/timeline`);
  assert.equal(result.json.data[0].action, 'SHADOW_BATCH_CREATED');
});

test('batch lifecycle suspends and resumes with revision and append-only events', async () => {
  let result = await request('POST', '/__simulation/reset');
  assert.equal(result.response.status, 200);

  result = await request('POST', '/bpi/v1/candidates/CAND-START-S07-001/confirm', {
    headers: commandHeaders('lifecycle-confirm-0001', 3),
    body: { reason: '创建批次用于状态闭环验收' },
  });
  assert.equal(result.response.status, 200);
  const batch = result.json.data.batch;

  result = await request('POST', `/bpi/v1/batches/${batch.id}/suspend`, {
    body: { reason: '上游制造指令上下文已过期' },
  });
  assert.equal(result.response.status, 428);

  const suspendHeaders = commandHeaders('lifecycle-suspend-0001', 1);
  result = await request('POST', `/bpi/v1/batches/${batch.id}/suspend`, {
    headers: suspendHeaders,
    body: { reason: '上游制造指令上下文已过期' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'SUSPENDED');
  assert.equal(result.json.data.revision, 2);
  const suspended = result.json;

  result = await request('POST', `/bpi/v1/batches/${batch.id}/suspend`, {
    headers: suspendHeaders,
    body: { reason: '上游制造指令上下文已过期' },
  });
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.deepEqual(result.json, suspended);

  result = await request('POST', `/bpi/v1/batches/${batch.id}/resume`, {
    headers: suspendHeaders,
    body: { reason: '错误复用暂停命令键' },
  });
  assert.equal(result.response.status, 409);
  assert.match(result.json.detail, /reused/);

  result = await request('POST', `/bpi/v1/batches/${batch.id}/suspend`, {
    headers: commandHeaders('lifecycle-repeat-suspend-0002', 2),
    body: { reason: '重复暂停不应成功' },
  });
  assert.equal(result.response.status, 409);
  assert.equal(result.json.currentRevision, 2);

  result = await request('POST', `/bpi/v1/batches/${batch.id}/resume`, {
    headers: commandHeaders('lifecycle-resume-0003', 2),
    body: { reason: '上游制造指令上下文已经恢复' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'ACTIVE');
  assert.equal(result.json.data.revision, 3);

  result = await request('GET', `/bpi/v1/batches/${batch.id}/timeline`);
  assert.deepEqual(result.json.data.map((item) => `${item.revision}|${item.action}`), [
    '1|SHADOW_BATCH_CREATED',
    '2|BATCH_SUSPENDED',
    '3|BATCH_RESUMED',
  ]);
  result = await request('GET', '/bpi/v1/lines/LINE-S07-01/current-state');
  assert.equal(result.json.data.status, 'RUNNING');
});

test('end candidate closes the matching shadow batch as CLOSED_RAW', async () => {
  let result = await request('POST', '/__simulation/reset');
  assert.equal(result.response.status, 200);

  result = await request('POST', '/bpi/v1/candidates/CAND-START-S07-001/confirm', {
    headers: commandHeaders('end-flow-start-0001', 3),
    body: { reason: '确认启动边界并建立待结束批次' },
  });
  assert.equal(result.response.status, 200);
  const batchId = result.json.data.batch.id;

  result = await request('GET', '/bpi/v1/candidates?plantId=PLANT-01&state=PENDING');
  assert.equal(result.json.data.length, 1);
  assert.equal(result.json.data[0].id, 'CAND-END-S07-001');
  assert.equal(result.json.data[0].boundaryType, 'END');

  result = await request('POST', '/bpi/v1/candidates/CAND-END-S07-001/confirm', {
    body: { reason: '流量归零且泵阀路径停止，确认结束边界' },
  });
  assert.equal(result.response.status, 428);

  const headers = commandHeaders('end-flow-confirm-0002', 1);
  result = await request('POST', '/bpi/v1/candidates/CAND-END-S07-001/confirm', {
    headers,
    body: { reason: '流量归零且泵阀路径停止，确认结束边界' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.candidate.state, 'CONFIRMED');
  assert.equal(result.json.data.candidate.batchId, batchId);
  assert.equal(result.json.data.batch.state, 'CLOSED_RAW');
  assert.equal(result.json.data.batch.revision, 2);
  assert.equal(result.json.data.batch.endTime, '2026-07-12T08:29:40.000Z');
  const firstResponse = result.json;

  result = await request('POST', '/bpi/v1/candidates/CAND-END-S07-001/confirm', {
    headers,
    body: { reason: '流量归零且泵阀路径停止，确认结束边界' },
  });
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.deepEqual(result.json, firstResponse);

  result = await request('GET', `/bpi/v1/batches/${batchId}/evidence`);
  assert.equal(result.json.data.start.length, 4);
  assert.equal(result.json.data.end.length, 3);
  assert.equal(result.json.data.end[0].signal, 'instantFlowBelowStopThreshold');

  result = await request('GET', `/bpi/v1/batches/${batchId}/timeline`);
  assert.deepEqual(result.json.data.map((item) => `${item.revision}|${item.action}`), [
    '1|SHADOW_BATCH_CREATED',
    '2|END_BOUNDARY_CONFIRMED',
  ]);

  result = await request('GET', '/bpi/v1/lines/LINE-S07-01/current-state');
  assert.equal(result.json.data.status, 'IDLE');
  assert.equal(result.json.data.currentBatchId, null);
  assert.equal(result.json.data.pendingCandidates, 0);
});

test('candidate rejection is idempotent and never creates a shadow batch', async () => {
  let result = await request('POST', '/__simulation/reset');
  assert.equal(result.response.status, 200);

  const headers = commandHeaders('reject-candidate-0001', 3);
  result = await request('POST', '/bpi/v1/candidates/CAND-START-S07-001/reject', {
    headers,
    body: { reason: '现场确认该边界为流量波动误判' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'REJECTED');
  assert.equal(result.json.data.revision, 4);
  assert.equal(result.json.data.batchId, null);
  const firstResponse = result.json;

  result = await request('POST', '/bpi/v1/candidates/CAND-START-S07-001/reject', {
    headers,
    body: { reason: '现场确认该边界为流量波动误判' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.deepEqual(result.json, firstResponse);

  result = await request('POST', '/bpi/v1/candidates/CAND-START-S07-001/confirm', {
    headers,
    body: { reason: '现场确认该边界为流量波动误判' },
  });
  assert.equal(result.response.status, 409);
  assert.match(result.json.detail, /reused/);

  result = await request('GET', '/bpi/v1/candidates?plantId=PLANT-01&state=PENDING');
  assert.deepEqual(result.json.data, []);
  result = await request('GET', '/bpi/v1/batches?plantId=PLANT-01');
  assert.deepEqual(result.json.data, []);
  result = await request('GET', '/bpi/v1/overview?plantId=PLANT-01');
  assert.equal(result.json.data[0].pendingCandidates, 0);
});

test('topology and rule comparisons return deterministic JSON Pointer changes', async () => {
  let result = await request('POST', '/__simulation/reset');
  assert.equal(result.response.status, 200);

  const topology = (await request('GET', '/bpi/v1/topologies?plantId=PLANT-01')).json.data[0];
  const topologyDefinition = structuredClone(topology.definition);
  topologyDefinition.localityGroup = 'LOCALITY-S07-EVAP-V2';
  result = await request('POST', '/bpi/v1/topologies/drafts', {
    headers: commandHeaders('compare-topology-draft-0001', topology.revision),
    body: {
      code: topology.code,
      version: '4',
      plantId: topology.plantId,
      lineId: topology.lineId,
      baseVersionId: topology.id,
      definition: topologyDefinition,
      reason: '建立拓扑差异测试版本',
    },
  });
  assert.equal(result.response.status, 200);
  const targetTopology = result.json.data;
  result = await request('GET', `/bpi/v1/topologies/${targetTopology.id}/compare?against=${topology.id}`);
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.objectType, 'TOPOLOGY_VERSION');
  assert.equal(result.json.data.changeCount, 1);
  assert.deepEqual(result.json.data.changes.map((change) => `${change.path}|${change.changeType}`), [
    '/localityGroup|CHANGED',
  ]);

  const rule = (await request('GET', '/bpi/v1/rules?plantId=PLANT-01')).json.data[0];
  const ast = structuredClone(rule.ast);
  ast.conditions[0].threshold = 17;
  result = await request('POST', '/bpi/v1/rules/drafts', {
    headers: commandHeaders('compare-rule-draft-0002', 0),
    body: {
      code: rule.code,
      version: '1.3.0',
      lineId: rule.lineId,
      topologyVersion: rule.topologyVersion,
      ast,
      reason: '建立规则差异测试版本',
    },
  });
  assert.equal(result.response.status, 200);
  const targetRule = result.json.data;
  result = await request('GET', `/bpi/v1/rules/${targetRule.id}/compare?against=${rule.id}`);
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.objectType, 'RULE_VERSION');
  assert.equal(result.json.data.changeCount, 1);
  assert.deepEqual(result.json.data.changes.map((change) => `${change.path}|${change.changeType}`), [
    '/ast/conditions/0/threshold|CHANGED',
  ]);
});

test('point calibration approval is authoritative and revoke immediately removes readiness', async () => {
  let result = await request('POST', '/__simulation/reset');
  assert.equal(result.response.status, 200);

  const now = Date.now();
  const validFrom = new Date(now - 60 * 60 * 1000).toISOString();
  const validUntil = new Date(now + 24 * 60 * 60 * 1000).toISOString();
  const pointSnapshot = {
    source: 'JETLINKS',
    sourceInstance: 'simulation-calibration-governance',
    sourceRevision: `SIM-CALIBRATION-${now}`,
    plantId: 'PLANT-01',
    lineId: 'LINE-S07-01',
    observedAt: new Date(now).toISOString(),
    reason: '验证源端校准声明不能绕过 MES 证据审批',
    points: [{
      localityGroup: 'LOCALITY-S07-EVAP',
      productId: 'PRODUCT-SUGAR',
      deviceId: 'DEVICE-S07-CALIBRATION',
      propertyId: 'flow.instant',
      sourcePropertyId: 'instantFlow',
      pointName: '校准治理验收瞬时流量',
      unit: 't/h',
      dataType: 'double',
      deviceState: 'ACTIVE',
      registered: true,
      propertyPresent: true,
      calibrationVersion: 'CAL-E2E-APPROVE-1',
      calibrationStatus: 'VERIFIED',
      sourceSequenceEnabled: true,
    }],
  };
  result = await request('POST', '/bpi/v1/point-catalog/snapshots', {
    headers: commandHeaders('calibration-import-point-0001', 0),
    body: pointSnapshot,
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.snapshot.readyPointCount, 0);
  assert.equal(result.json.data.points[0].sourceCalibrationStatus, 'VERIFIED');
  assert.equal(result.json.data.points[0].calibrationStatus, 'UNVERIFIED');
  assert.deepEqual(result.json.data.points[0].readinessIssues, ['CALIBRATION_NOT_VERIFIED']);

  result = await request('GET', '/bpi/v1/point-calibrations?plantId=PLANT-01&lineId=LINE-S07-01');
  assert.equal(result.response.status, 200);
  assert.ok(result.json.data.length >= 2);

  const calibrationPayload = {
    plantId: 'PLANT-01',
    lineId: 'LINE-S07-01',
    productId: 'PRODUCT-SUGAR',
    deviceId: 'DEVICE-S07-CALIBRATION',
    propertyId: 'flow.instant',
    calibrationVersion: 'CAL-E2E-APPROVE-1',
    certificateReference: 'urn:ft-mes:test:calibration:approve-1',
    certificateChecksum: 'a'.repeat(64),
    validFrom,
    validUntil,
    reason: '提交独立计量证据进行审批',
  };
  result = await request('POST', '/bpi/v1/point-calibrations', {
    headers: commandHeaders('calibration-submit-0002', 0),
    body: calibrationPayload,
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'PENDING');
  assert.equal(result.json.data.effectivenessStatus, 'PENDING');
  const calibrationId = result.json.data.id;

  result = await request('GET', '/bpi/v1/point-catalog/current?plantId=PLANT-01&lineId=LINE-S07-01');
  assert.equal(result.json.data.snapshot.readyPointCount, 0);

  result = await request('POST', `/bpi/v1/point-calibrations/${calibrationId}/approve`, {
    headers: commandHeaders('calibration-approve-0003', 1),
    body: { reason: '独立计量管理员复核证书与点位一致' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'APPROVED');
  assert.equal(result.json.data.effectivenessStatus, 'EFFECTIVE');
  assert.equal(result.json.data.revision, 2);

  result = await request('GET', '/bpi/v1/point-catalog/current?plantId=PLANT-01&lineId=LINE-S07-01');
  assert.equal(result.json.data.snapshot.readyPointCount, 1);
  assert.equal(result.json.data.points[0].calibrationStatus, 'VERIFIED');
  assert.equal(result.json.data.points[0].calibrationEvidenceId, calibrationId);

  result = await request('POST', `/bpi/v1/point-calibrations/${calibrationId}/revoke`, {
    headers: commandHeaders('calibration-revoke-0004', 2),
    body: { reason: '证书复核发现异常，立即撤销放行资格' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'REVOKED');
  assert.equal(result.json.data.effectivenessStatus, 'REVOKED');
  assert.equal(result.json.data.revision, 3);

  result = await request('GET', '/bpi/v1/point-catalog/current?plantId=PLANT-01&lineId=LINE-S07-01');
  assert.equal(result.json.data.snapshot.readyPointCount, 0);
  assert.equal(result.json.data.points[0].calibrationStatus, 'UNVERIFIED');

  result = await request('POST', '/bpi/v1/point-calibrations', {
    headers: commandHeaders('calibration-submit-reject-0005', 0),
    body: {
      ...calibrationPayload,
      calibrationVersion: 'CAL-E2E-REJECT-1',
      certificateReference: 'urn:ft-mes:test:calibration:reject-1',
      certificateChecksum: 'b'.repeat(64),
      reason: '提交一份待拒绝的计量证据',
    },
  });
  assert.equal(result.response.status, 200);
  const rejectedCalibrationId = result.json.data.id;

  result = await request('POST', `/bpi/v1/point-calibrations/${rejectedCalibrationId}/reject`, {
    headers: commandHeaders('calibration-reject-0006', 1),
    body: { reason: '证书校验值与原件不一致，拒绝放行' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'REJECTED');
  assert.equal(result.json.data.effectivenessStatus, 'REJECTED');
  assert.equal(result.json.data.revision, 2);
});

test('rule simulation checksum gates publication', async () => {
  let pointResult = await request('POST', '/__simulation/reset');
  assert.equal(pointResult.response.status, 200);
  pointResult = await request('GET', '/bpi/v1/point-catalog/current?plantId=PLANT-01&lineId=LINE-S07-01');
  assert.equal(pointResult.response.status, 200);
  assert.equal(pointResult.json.data.snapshot.readyPointCount, 2);
  pointResult = await request('GET', '/bpi/v1/point-catalog/snapshots?plantId=PLANT-01&lineId=LINE-S07-01');
  assert.equal(pointResult.json.data.length, 1);

  const pointSnapshot = {
    source: 'JETLINKS', sourceInstance: 'simulation-import', sourceRevision: 'SIM-POINTS-0001',
    plantId: 'PLANT-01', lineId: 'LINE-S07-01', observedAt: '2026-07-12T07:58:00.000Z',
    reason: '导入模拟点位目录',
    points: [{
      localityGroup: 'LOCALITY-S07-EVAP', productId: 'PRODUCT-SUGAR', deviceId: 'DEVICE-S07-01',
      propertyId: 'flow.instant', sourcePropertyId: 'instantFlow', pointName: '进料瞬时流量', unit: 't/h', dataType: 'double',
      deviceState: 'ACTIVE', registered: true, propertyPresent: true,
      calibrationVersion: 'CAL-1', calibrationStatus: 'VERIFIED', sourceSequenceEnabled: true,
    }],
  };
  const pointHeaders = commandHeaders('import-point-catalog-0001', 0);
  pointResult = await request('POST', '/bpi/v1/point-catalog/snapshots', { headers: pointHeaders, body: pointSnapshot });
  assert.equal(pointResult.response.status, 200);
  assert.equal(pointResult.json.data.snapshot.readyPointCount, 1);
  const firstPointImport = pointResult.json;
  pointResult = await request('POST', '/bpi/v1/point-catalog/snapshots', { headers: pointHeaders, body: pointSnapshot });
  assert.equal(pointResult.response.headers.get('idempotent-replay'), 'true');
  assert.deepEqual(pointResult.json, firstPointImport);

  let topologyResult = await request('GET', '/bpi/v1/topologies?plantId=PLANT-01&lineId=LINE-S07-01');
  assert.equal(topologyResult.json.data.length, 1);
  assert.equal(topologyResult.json.data[0].code, 'TOPO-S07');
  topologyResult = await request('GET', `/bpi/v1/topologies/${topologyResult.json.data[0].id}`);
  assert.equal(topologyResult.json.data.definition.bindings.length, 2);

  let result = await request('GET', '/bpi/v1/rules?plantId=PLANT-01');
  assert.equal(result.json.data[0].revision, 7);

  result = await request('GET', `/bpi/v1/rules/${RULE_ID}`);
  assert.equal(result.json.data.state, 'DRAFT');

  const simulationInput = {
    lineId: 'LINE-S07-01',
    from: '2026-07-01T00:00:00.000Z',
    to: '2026-07-12T00:00:00.000Z',
    topologyVersion: 'TOPO-S07@3',
    calibrationVersion: 'CAL-S07@2',
    goldenSetId: 'GOLDEN-S07-2026Q2',
  };
  result = await request('POST', `/bpi/v1/rules/${RULE_ID}/simulate`, {
    headers: commandHeaders('simulate-rule-0001', 7), body: simulationInput,
  });
  assert.equal(result.response.status, 202);
  assert.equal(result.json.data.state, 'PASSED');
  assert.equal(result.json.data.metrics.missed, 0);
  assert.equal(result.json.data.metrics.falsePositive, 0);
  assert.equal(result.json.data.inputManifest.observationCount, 18640);
  assert.deepEqual(result.json.data.emittedBoundaries, ['2026-07-12T07:59:40.000Z']);
  assert.match(result.json.data.checksum, /^[a-f0-9]{64}$/);
  const simulation = result.json.data;

  result = await request('GET', `/bpi/v1/rule-simulations/${simulation.id}`);
  assert.deepEqual(result.json.data, simulation);

  result = await request('POST', `/bpi/v1/rules/${RULE_ID}/submit-approval`, {
    headers: commandHeaders('submit-rule-bad-0001', 8),
    body: { reason: '提交审批', simulationId: simulation.id, simulationChecksum: 'bad-checksum' },
  });
  assert.equal(result.response.status, 422);

  result = await request('POST', `/bpi/v1/rules/${RULE_ID}/submit-approval`, {
    headers: commandHeaders('submit-rule-good-0002', 8),
    body: { reason: '提交审批', simulationId: simulation.id, simulationChecksum: simulation.checksum },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'PENDING_APPROVAL');
  assert.equal(result.json.data.approvalStatus, 'PENDING');
  assert.equal(result.json.data.revision, 9);

  result = await request('POST', `/bpi/v1/rules/${RULE_ID}/reject-approval`, {
    headers: commandHeaders('reject-rule-approval-0003', 9),
    body: { reason: '现场复核要求重新调整保持时间' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'DRAFT');
  assert.equal(result.json.data.approvalStatus, 'REJECTED');
  assert.equal(result.json.data.revision, 10);

  result = await request('POST', `/bpi/v1/rules/${RULE_ID}/simulate`, {
    headers: commandHeaders('simulate-rule-after-reject-0004', 10), body: simulationInput,
  });
  assert.equal(result.response.status, 202);
  const approvedSimulation = result.json.data;
  assert.equal(result.json.data.state, 'PASSED');

  result = await request('POST', `/bpi/v1/rules/${RULE_ID}/submit-approval`, {
    headers: commandHeaders('submit-rule-after-reject-0005', 11),
    body: {
      reason: '重新回放后提交审批',
      simulationId: approvedSimulation.id,
      simulationChecksum: approvedSimulation.checksum,
    },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'PENDING_APPROVAL');
  assert.equal(result.json.data.revision, 12);

  result = await request('POST', `/bpi/v1/rules/${RULE_ID}/publish`, {
    headers: commandHeaders('publish-rule-good-0006', 12),
    body: {
      reason: '独立管理员批准发布',
      simulationId: approvedSimulation.id,
      simulationChecksum: approvedSimulation.checksum,
    },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'PUBLISHED');
  assert.equal(result.json.data.approvalStatus, 'APPROVED');
  assert.equal(result.json.data.publicationStatus, 'PENDING');
  assert.equal(result.json.data.publicationRevision, 1);
  assert.equal(result.json.data.publicationAttemptCount, 0);
  assert.equal(result.json.data.publicationTotalAttemptCount, 0);
  assert.equal(result.json.data.revision, 13);
  assert.equal(result.json.data.applicationStatus, 'WAITING');
  assert.equal(result.json.data.runtimeReadinessStatus, 'WAITING');

  result = await request('POST', '/__simulation/fail-rule-publication');
  assert.equal(result.response.status, 200);
  result = await request('POST', `/bpi/v1/rules/${RULE_ID}/publication/retry`, {
    headers: commandHeaders('retry-publication-0001', 11),
    body: { reason: 'Kafka 集群恢复并完成连通性检查' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.publicationStatus, 'PENDING');
  assert.equal(result.json.data.publicationRevision, 12);
  assert.equal(result.json.data.publicationAttemptCount, 0);
  assert.equal(result.json.data.publicationTotalAttemptCount, 5);
  assert.equal(result.json.data.publicationManualRetryCount, 1);
  assert.equal(result.json.data.publicationLastError, null);
  assert.equal(result.json.data.applicationStatus, 'WAITING');

  result = await request('POST', '/__simulation/complete-rule-publication');
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.publicationStatus, 'PUBLISHED');
  assert.equal(result.json.data.publicationRevision, 13);
  assert.equal(result.json.data.applicationStatus, 'WAITING');

  result = await request('POST', '/__simulation/rule-application', {
    body: {
      status: 'REJECTED',
      deploymentId: 'flink-simulator-a',
      errorCode: 'RULE_WINDOW_EXCEEDS_STATE_TTL',
      errorDetail: 'rule window exceeds state TTL',
    },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.applicationStatus, 'REJECTED');
  assert.equal(result.json.data.applicationErrorCode, 'RULE_WINDOW_EXCEEDS_STATE_TTL');
  assert.equal(result.json.data.publicationRevision, 14);

  result = await request('POST', '/__simulation/rule-application', {
    body: { status: 'APPLIED', deploymentId: 'flink-simulator-b' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.applicationStatus, 'APPLIED');
  assert.equal(result.json.data.applicationDeploymentId, 'flink-simulator-b');
  assert.equal(result.json.data.applicationErrorCode, null);
  assert.equal(result.json.data.applicationErrorDetail, null);
  assert.equal(result.json.data.publicationRevision, 15);
  assert.equal(result.json.data.runtimeReadinessStatus, 'WAITING');

  result = await request('POST', '/__simulation/rule-runtime-readiness', {
    body: {
      eventId: 'readiness-degraded-0001',
      status: 'DEGRADED',
      deploymentId: 'flink-simulator-b',
      observedAt: '2026-07-12T08:00:04.000Z',
      reasonCode: 'POINT_SOURCE_SEQUENCE_DISABLED',
      detail: 'device source sequence evidence is missing',
      pointCatalogEventId: 'catalog-event-41',
      pointCatalogSourceRevision: 'sha256:catalog-41',
    },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.applicationStatus, 'APPLIED');
  assert.equal(result.json.data.runtimeReadinessStatus, 'DEGRADED');
  assert.equal(result.json.data.runtimeReadinessReasonCode, 'POINT_SOURCE_SEQUENCE_DISABLED');
  assert.equal(result.json.data.publicationRevision, 16);

  result = await request('POST', '/__simulation/rule-runtime-readiness', {
    body: {
      eventId: 'readiness-ready-delayed-0002',
      status: 'READY',
      deploymentId: 'flink-simulator-b',
      observedAt: '2026-07-12T08:00:20.000Z',
      receivedAt: '2026-07-12T08:05:00.000Z',
      pointCatalogEventId: 'catalog-event-42',
      pointCatalogSourceRevision: 'sha256:catalog-42',
    },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.applicationStatus, 'APPLIED');
  assert.equal(result.json.data.runtimeReadinessStatus, 'READY');
  assert.equal(result.json.data.runtimeReadinessReceivedAt, '2026-07-12T08:05:00.000Z');
  assert.equal(result.json.data.runtimeReadinessReasonCode, null);
  assert.equal(result.json.data.publicationRevision, 17);

  const duplicateReady = {
    eventId: 'readiness-ready-delayed-0002',
    status: 'READY',
    deploymentId: 'flink-simulator-b',
    observedAt: '2026-07-12T08:00:20.000Z',
    receivedAt: '2026-07-12T08:05:00.000Z',
    pointCatalogEventId: 'catalog-event-42',
    pointCatalogSourceRevision: 'sha256:catalog-42',
  };
  result = await request('POST', '/__simulation/rule-runtime-readiness', { body: duplicateReady });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.runtimeReadinessStatus, 'READY');
  assert.equal(result.json.data.publicationRevision, 17);

  result = await request('POST', '/__simulation/rule-runtime-readiness', {
    body: {
      eventId: 'readiness-stale-degraded-0003',
      status: 'DEGRADED',
      observedAt: '2026-07-12T08:00:10.000Z',
      reasonCode: 'STALE_POINT_CATALOG',
      detail: 'an older delayed receipt must not replace READY',
    },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.runtimeReadinessStatus, 'READY');
  assert.equal(result.json.data.publicationRevision, 17);

  result = await request('POST', '/__simulation/rule-runtime-readiness', {
    body: { ...duplicateReady, status: 'DEGRADED', reasonCode: 'REPLAY_MUTATION', detail: 'mutated duplicate' },
  });
  assert.equal(result.response.status, 409);
  assert.equal(result.json.status, 'RUNTIME_READINESS_REPLAY_CONFLICT');

  result = await request('POST', `/bpi/v1/rules/${RULE_ID}/retire`, {
    headers: commandHeaders('retire-rule-0007', 13),
    body: { reason: '发布替代版本前按变更单退役当前规则' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'RETIRED');
  assert.equal(result.json.data.revision, 14);
  assert.equal(result.json.data.lifecycleAction, 'RETIRE');
  assert.equal(result.json.data.lifecycleSequence, 2);
  assert.equal(result.json.data.lifecycleActive, false);
  assert.equal(result.json.data.publicationStatus, 'PENDING');
  assert.equal(result.json.data.applicationStatus, 'WAITING');
  assert.equal(result.json.data.runtimeReadinessStatus, 'WAITING');

  result = await request('POST', `/bpi/v1/rules/${RULE_ID}/retire`, {
    headers: commandHeaders('retire-rule-0007', 13),
    body: { reason: '发布替代版本前按变更单退役当前规则' },
  });
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.equal(result.json.data.lifecycleSequence, 2);

  result = await request('POST', '/__simulation/complete-rule-publication');
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.publicationStatus, 'PUBLISHED');
  assert.equal(result.json.data.publicationRevision, 2);

  result = await request('POST', '/__simulation/rule-application', {
    body: { status: 'APPLIED', deploymentId: 'flink-simulator-b' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.applicationStatus, 'APPLIED');
  assert.equal(result.json.data.publicationRevision, 3);

  result = await request('POST', '/__simulation/rule-runtime-readiness', {
    body: {
      eventId: 'readiness-inactive-0004',
      status: 'INACTIVE',
      observedAt: '2026-07-12T08:00:30.000Z',
      reasonCode: 'RULE_INACTIVE',
      detail: 'published rule version is inactive',
    },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.runtimeReadinessStatus, 'INACTIVE');
  assert.equal(result.json.data.applicationStatus, 'APPLIED');
  assert.equal(result.json.data.publicationRevision, 4);
  const retiredRule = result.json.data;

  result = await request('POST', '/bpi/v1/rules/drafts', {
    headers: commandHeaders('rollback-rule-draft-0008', 14),
    body: {
      code: retiredRule.code,
      version: '1.2.1',
      lineId: retiredRule.lineId,
      topologyVersion: retiredRule.topologyVersion,
      baseVersionId: retiredRule.id,
      ast: retiredRule.ast,
      reason: '从已退役稳定版本创建受控回滚草稿',
    },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'DRAFT');
  assert.equal(result.json.data.lifecycleAction, 'NOT_PUBLISHED');
  assert.equal(result.json.data.lifecycleSequence, 0);

  result = await request('POST', '/__simulation/rule-application', {
    body: { status: 'REJECTED', errorCode: 'STALE_REPLAY' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.applicationStatus, 'APPLIED');
  assert.equal(result.json.data.runtimeReadinessStatus, 'INACTIVE');
  assert.equal(result.json.data.publicationRevision, 4);

  result = await request('POST', '/__simulation/fail-rule-publication');
  assert.equal(result.response.status, 409);
  assert.equal(result.json.status, 'NOT_DISPATCHING');
});

test('data quality and integration impact remain visible', async () => {
  let result = await request('GET', '/bpi/v1/data-quality/incidents?plantId=PLANT-01');
  assert.equal(result.json.data[0].issueCode, 'CLOCK_DRIFT');
  assert.deepEqual(result.json.data[0].affectedRules, ['RULE-S07-START']);

  result = await request('GET', '/bpi/v1/data-quality/incidents/DQ-S07-FLOW-001');
  assert.equal(result.json.data.state, 'OPEN');

  result = await request('GET', '/bpi/v1/integrations/health');
  const timescale = result.json.data.find((item) => item.id === 'timescaledb');
  assert.equal(timescale.status, 'DEGRADED');
  assert.match(timescale.businessImpact, /不阻断批次事实查询/);
});
