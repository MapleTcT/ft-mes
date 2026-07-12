const assert = require('node:assert/strict');
const { after, before, test } = require('node:test');
const path = require('node:path');
const fs = require('node:fs');
const { createBpiSimulator, listen } = require('./server');

const profile = JSON.parse(fs.readFileSync(path.join(__dirname, '../../contracts/bpi-api/simulation-profile.json'), 'utf8'));
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

test('rule simulation checksum gates publication', async () => {
  let result = await request('GET', '/bpi/v1/rules?plantId=PLANT-01');
  assert.equal(result.json.data[0].revision, 7);

  result = await request('GET', '/bpi/v1/rules/RULE-S07-START');
  assert.equal(result.json.data.state, 'DRAFT');

  const simulationInput = {
    lineId: 'LINE-S07-01',
    from: '2026-07-01T00:00:00.000Z',
    to: '2026-07-12T00:00:00.000Z',
    topologyVersion: 'TOPO-S07@3',
    calibrationVersion: 'CAL-S07@2',
    goldenSetId: 'GOLDEN-S07-2026Q2',
  };
  result = await request('POST', '/bpi/v1/rules/RULE-S07-START/simulate', {
    headers: commandHeaders('simulate-rule-0001', 7), body: simulationInput,
  });
  assert.equal(result.response.status, 202);
  assert.equal(result.json.data.state, 'PASSED');
  assert.match(result.json.data.checksum, /^[a-f0-9]{64}$/);
  const simulation = result.json.data;

  result = await request('GET', `/bpi/v1/rule-simulations/${simulation.id}`);
  assert.deepEqual(result.json.data, simulation);

  result = await request('POST', '/bpi/v1/rules/RULE-S07-START/publish', {
    headers: commandHeaders('publish-rule-bad-0001', 8),
    body: { reason: '审批发布', simulationId: simulation.id, simulationChecksum: 'bad-checksum' },
  });
  assert.equal(result.response.status, 422);

  result = await request('POST', '/bpi/v1/rules/RULE-S07-START/publish', {
    headers: commandHeaders('publish-rule-good-0002', 8),
    body: { reason: '审批发布', simulationId: simulation.id, simulationChecksum: simulation.checksum },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'PUBLISHED');
  assert.equal(result.json.data.revision, 9);
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
