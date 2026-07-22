const assert = require('node:assert/strict');
const { after, before, test } = require('node:test');
const path = require('node:path');
const fs = require('node:fs');
const { createBpiSimulator, listen } = require('./server');

const profile = JSON.parse(fs.readFileSync(path.join(__dirname, '../../contracts/bpi-api/simulation-profile.json'), 'utf8'));
const RULE_ID = '78d57d90-fdc8-4a57-a660-a1ae73c2bc96';
const SOURCE_SEQUENCE_FINGERPRINT = `sha256:${'2'.repeat(64)}`;
const covered = new Set();
let server;
let baseUrl;
let simulatorState;

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
  ({ server, state: simulatorState } = createBpiSimulator());
  const address = await listen(server);
  baseUrl = `http://127.0.0.1:${address.port}`;
});

after(async () => {
  await new Promise((resolve, reject) => server.close((error) => (error ? reject(error) : resolve())));
  assert.deepEqual([...covered].sort(), [...profile.operationIds].sort(), 'every simulated operation must be exercised');
});

test('feature flag overrides are scoped, versioned, idempotent and phase locked', async () => {
  let result = await request('POST', '/__simulation/reset');
  assert.equal(result.response.status, 200);

  result = await request('GET', '/bpi/v1/feature-flags?plantId=PLANT-01&lineId=LINE-S07-01&scopeType=LINE');
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.length, 6);
  let commands = result.json.data.find((item) => item.flagKey === 'bpi.commands');
  assert.equal(commands.effectiveEnabled, true);
  assert.equal(commands.effectiveScopeType, 'LINE');
  assert.equal(commands.overrideActive, true);
  assert.equal(commands.overrideRevision, 1);
  const ui = result.json.data.find((item) => item.flagKey === 'bpi.ui');
  assert.equal(ui.effectiveEnabled, false);
  assert.equal(ui.editable, true);
  assert.equal(ui.enforcementStatus, 'ENFORCED');
  const wmsLink = result.json.data.find((item) => item.flagKey === 'bpi.wms-link');
  assert.equal(wmsLink.effectiveEnabled, false);
  assert.equal(wmsLink.editable, false);
  assert.equal(wmsLink.enforcementStatus, 'PHASE_LOCKED');

  const disableHeaders = commandHeaders('feature-commands-disable-0001', 1);
  const disableBody = {
    scopeType: 'LINE', plantId: 'PLANT-01', lineId: 'LINE-S07-01',
    mode: 'SET', enabled: false, reason: '验收产线命令开关显式禁用',
  };
  result = await request('POST', '/bpi/v1/feature-flags/bpi.commands', {
    headers: disableHeaders,
    body: disableBody,
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.effectiveEnabled, false);
  assert.equal(result.json.data.overrideRevision, 2);
  const disabledResponse = result.json;

  result = await request('POST', '/bpi/v1/feature-flags/bpi.commands', {
    headers: disableHeaders,
    body: disableBody,
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.deepEqual(result.json, disabledResponse);
  assert.equal(simulatorState.featureFlagAudits.length, 1);

  result = await request('POST', '/bpi/v1/feature-flags/bpi.commands', {
    headers: commandHeaders('feature-commands-stale-0002', 1),
    body: { ...disableBody, enabled: true, reason: '过期页面尝试重新启用运行命令' },
  });
  assert.equal(result.response.status, 409);
  assert.equal(result.json.currentRevision, 2);
  assert.equal(simulatorState.featureFlagAudits.length, 1);

  result = await request('POST', '/bpi/v1/feature-flags/bpi.commands', {
    headers: commandHeaders('feature-commands-inherit-0003', 2),
    body: {
      scopeType: 'LINE', plantId: 'PLANT-01', lineId: 'LINE-S07-01',
      mode: 'INHERIT', reason: '移除产线覆盖并恢复平台默认继承',
    },
  });
  assert.equal(result.response.status, 200);
  commands = result.json.data;
  assert.equal(commands.overrideActive, false);
  assert.equal(commands.overrideRevision, 3);
  assert.equal(commands.effectiveEnabled, false);
  assert.equal(commands.effectiveScopeType, 'GLOBAL');
  assert.equal(simulatorState.featureFlagAudits.length, 2);
  assert.deepEqual(simulatorState.featureFlagAudits.map((item) => item.action), [
    'FEATURE_FLAG_DISABLED',
    'FEATURE_FLAG_OVERRIDE_REMOVED',
  ]);

  result = await request('POST', '/bpi/v1/feature-flags/bpi.wms-link', {
    headers: commandHeaders('feature-wms-locked-0004', 0),
    body: {
      scopeType: 'LINE', plantId: 'PLANT-01', lineId: 'LINE-S07-01',
      mode: 'SET', enabled: true, reason: '阶段锁定项不得通过模拟页面启用',
    },
  });
  assert.equal(result.response.status, 422);
  assert.match(result.json.detail, /Phase 2/);
  assert.equal(simulatorState.featureFlagAudits.length, 2);
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

test('force-close is recoverable, idempotent and requires a separate approval step', async () => {
  let result = await request('POST', '/__simulation/reset');
  assert.equal(result.response.status, 200);

  result = await request('POST', '/bpi/v1/candidates/CAND-START-S07-001/confirm', {
    headers: commandHeaders('force-close-confirm-0001', 3),
    body: { reason: '创建批次用于强制结束双人审批验收' },
  });
  assert.equal(result.response.status, 200);
  const batch = result.json.data.batch;
  const boundaryTime = '2026-07-12T08:20:00.000Z';

  result = await request('GET', `/bpi/v1/batches/${batch.id}/force-close`);
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data, null);

  result = await request('POST', `/bpi/v1/batches/${batch.id}/force-close`, {
    body: { reason: '设备故障需按现场停机时间结束', boundaryTime, approvalMode: 'REQUEST' },
  });
  assert.equal(result.response.status, 428);

  const requestHeaders = commandHeaders('force-close-request-0002', 1);
  result = await request('POST', `/bpi/v1/batches/${batch.id}/force-close`, {
    headers: requestHeaders,
    body: { reason: '设备故障需按现场停机时间结束', boundaryTime, approvalMode: 'REQUEST' },
  });
  assert.equal(result.response.status, 202);
  assert.equal(result.json.data.state, 'PENDING_APPROVAL');
  assert.equal(result.json.data.batchRevision, 2);
  const pendingTask = result.json;

  result = await request('POST', `/bpi/v1/batches/${batch.id}/force-close`, {
    headers: requestHeaders,
    body: { reason: '设备故障需按现场停机时间结束', boundaryTime, approvalMode: 'REQUEST' },
  });
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.deepEqual(result.json, pendingTask);

  result = await request('POST', `/bpi/v1/batches/${batch.id}/suspend`, {
    headers: commandHeaders('force-close-suspend-blocked-0003', 2),
    body: { reason: '待审批期间不允许切换运行状态' },
  });
  assert.equal(result.response.status, 409);

  result = await request('POST', `/bpi/v1/batches/${batch.id}/force-close`, {
    headers: commandHeaders('force-close-mismatch-0004', 2),
    body: { reason: '管理员核对时不得修改边界', boundaryTime: '2026-07-12T08:21:00.000Z', approvalMode: 'APPROVE' },
  });
  assert.equal(result.response.status, 409);

  result = await request('POST', `/bpi/v1/batches/${batch.id}/force-close`, {
    headers: commandHeaders('force-close-approve-0005', 2),
    body: { reason: '独立复核设备停机记录和流量归零时间', boundaryTime, approvalMode: 'APPROVE' },
  });
  assert.equal(result.response.status, 202);
  assert.equal(result.json.data.state, 'COMPLETED');
  assert.equal(result.json.data.batchRevision, 3);
  assert.equal(result.json.data.requestedBy, 'simulated.shift.lead');
  assert.equal(result.json.data.decidedBy, 'simulated.bpi.admin');

  result = await request('GET', `/bpi/v1/batches/${batch.id}`);
  assert.equal(result.json.data.state, 'CLOSED_RAW');
  assert.equal(result.json.data.revision, 3);
  assert.equal(result.json.data.endTime, boundaryTime);

  result = await request('GET', `/bpi/v1/batches/${batch.id}/timeline`);
  assert.deepEqual(result.json.data.map((item) => `${item.revision}|${item.action}`), [
    '1|SHADOW_BATCH_CREATED',
    '2|BATCH_FORCE_CLOSE_REQUESTED',
    '3|BATCH_FORCE_CLOSED',
  ]);
  result = await request('GET', '/bpi/v1/lines/LINE-S07-01/current-state');
  assert.equal(result.json.data.status, 'IDLE');
  assert.equal(result.json.data.currentBatchId, null);
});

test('batch release projection distinguishes quality and WMS business states', async () => {
  let result = await request('POST', '/__simulation/reset');
  assert.equal(result.response.status, 200);

  result = await request('POST', '/__simulation/prepare-batch-release');
  assert.equal(result.response.status, 200);
  assert.equal(result.json.preparedBatchCount, 6);
  const batchIds = result.json.batchIds;
  const expected = {
    closedRaw: { batch: 'CLOSED_RAW', gate: null, wms: null, document: null },
    waiting: { batch: 'WAIT_QA', gate: 'WAITING', wms: null, document: null },
    rejected: { batch: 'REJECTED', gate: 'REJECTED', wms: null, document: null },
    wmsPending: { batch: 'RELEASED', gate: 'ACCEPTED', wms: 'PENDING', document: null },
    wmsFailed: { batch: 'RELEASED', gate: 'ACCEPTED', wms: 'REJECTED', document: null },
    inbounded: { batch: 'INBOUNDED', gate: 'ACCEPTED', wms: 'ACCEPTED', document: 'WMS-IN-ADP-E2E-0001' },
  };

  for (const [key, projection] of Object.entries(expected)) {
    result = await request('GET', `/bpi/v1/batches/${batchIds[key]}/release`);
    assert.equal(result.response.status, 200);
    assert.equal(result.response.headers.get('x-bpi-operation-id'), 'getBatchRelease');
    assert.equal(result.json.data.batch.state, projection.batch);
    assert.equal(result.json.data.qualityGate?.state ?? null, projection.gate);
    assert.equal(result.json.data.wmsInbound?.status ?? null, projection.wms);
    assert.equal(result.json.data.wmsInbound?.documentId ?? null, projection.document);
  }

  const pendingBefore = { ...simulatorState.batchReleases.get(batchIds.wmsPending).wmsInbound };
  const reconcileHeaders = commandHeaders('wms-reconcile-same-command-0001', pendingBefore.revision);
  result = await request('POST', `/bpi/v1/batches/${batchIds.wmsPending}/wms/reconcile`, {
    headers: reconcileHeaders,
    body: { reason: '回执超时，按原命令和幂等键重新核对 WMS 单据' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.response.headers.get('x-bpi-operation-id'), 'reconcileWmsInbound');
  assert.equal(result.json.data.wmsInbound.commandEventId, pendingBefore.commandEventId);
  assert.equal(result.json.data.wmsInbound.idempotencyKey, pendingBefore.idempotencyKey);
  assert.equal(result.json.data.wmsInbound.outboxStatus, 'PENDING');
  assert.equal(result.json.data.wmsInbound.reconciliationCount, 1);
  assert.equal(result.json.data.wmsInbound.revision, pendingBefore.revision + 1);
  assert.equal(result.json.data.wmsInbound.reconciliationAllowed, false);
  assert.equal(result.json.data.wmsInbound.reconciliationBlockedReason, 'OUTBOX_BUSY');

  result = await request('POST', `/bpi/v1/batches/${batchIds.wmsPending}/wms/reconcile`, {
    headers: reconcileHeaders,
    body: { reason: '回执超时，按原命令和幂等键重新核对 WMS 单据' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.equal(result.json.data.wmsInbound.reconciliationCount, 1);

  result = await request('POST', `/bpi/v1/batches/${batchIds.wmsPending}/wms/reconcile`, {
    headers: commandHeaders('wms-reconcile-stale-revision-0002', pendingBefore.revision),
    body: { reason: '旧投影版本不得重复排队' },
  });
  assert.equal(result.response.status, 409);
  assert.equal(result.json.currentRevision, pendingBefore.revision + 1);

  result = await request('GET', `/bpi/v1/batches/${batchIds.waiting}/evidence`);
  assert.equal(result.json.data.start.length, 2);
  assert.equal(result.json.data.end.length, 2);
  result = await request('GET', `/bpi/v1/batches/${batchIds.wmsFailed}/timeline`);
  assert.equal(result.json.data.at(-1).action, 'WMS_INBOUND_REJECTED');
  assert.equal(simulatorState.batchReleases.size, 6);
});

test('completion inbound reversal preserves the blue document and closes through an append-only red command', async () => {
  let result = await request('POST', '/__simulation/reset');
  assert.equal(result.response.status, 200);
  result = await request('POST', '/__simulation/prepare-batch-release');
  assert.equal(result.response.status, 200);
  const batchId = result.json.batchIds.inbounded;
  const release = simulatorState.batchReleases.get(batchId);
  const originalBlue = structuredClone(release.wmsInbound);

  result = await request('GET', `/bpi/v1/batches/${batchId}/wms/reversal`);
  assert.equal(result.response.status, 200);
  assert.equal(result.response.headers.get('x-bpi-operation-id'), 'getWmsInboundReversalTask');
  assert.equal(result.json.data, null);

  const requestHeaders = commandHeaders('wms-reversal-request-0001', release.batch.revision);
  const requestBody = { reason: '原完工入库业务单据录入错误，申请红单冲销', approvalMode: 'REQUEST' };
  result = await request('POST', `/bpi/v1/batches/${batchId}/wms/reversal`, {
    headers: requestHeaders,
    body: requestBody,
  });
  assert.equal(result.response.status, 202);
  assert.equal(result.response.headers.get('x-bpi-operation-id'), 'commandWmsInboundReversal');
  assert.equal(result.json.data.state, 'PENDING_APPROVAL');
  assert.equal(result.json.data.originalDocumentId, originalBlue.documentId);
  assert.equal(result.json.data.originalCommandEventId, originalBlue.commandEventId);
  assert.equal(result.json.data.requestedBy, 'simulated.shift.lead');
  const requestSnapshot = result.json;

  result = await request('POST', `/bpi/v1/batches/${batchId}/wms/reversal`, {
    headers: requestHeaders,
    body: requestBody,
  });
  assert.equal(result.response.status, 202);
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.deepEqual(result.json, requestSnapshot);

  result = await request('POST', `/bpi/v1/batches/${batchId}/wms/reversal`, {
    headers: commandHeaders('wms-reversal-approve-0002', release.batch.revision),
    body: { reason: '独立复核原蓝单、物料、数量和申请依据一致', approvalMode: 'APPROVE' },
  });
  assert.equal(result.response.status, 202);
  assert.equal(result.json.data.state, 'PENDING_WMS');
  assert.equal(result.json.data.decidedBy, 'simulated.bpi.admin');
  assert.notEqual(result.json.data.requestedBy, result.json.data.decidedBy);
  assert.notEqual(result.json.data.reversalCommandEventId, originalBlue.commandEventId);
  assert.notEqual(result.json.data.reversalIdempotencyKey, originalBlue.idempotencyKey);
  assert.equal(release.batch.state, 'INBOUND_REVERSING');
  assert.equal(release.batch.wmsStatus, 'REVERSAL_PENDING');
  assert.deepEqual(release.wmsInbound, originalBlue);

  result = await request('POST', '/__simulation/complete-wms-inbound-reversal', {
    body: { batchId, status: 'ACCEPTED', reversalDocumentId: 'WMS-RED-ADP-E2E-0001' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.batch.state, 'INBOUND_REVERSED');
  assert.equal(result.json.data.batch.wmsStatus, 'REVERSED');
  assert.equal(result.json.data.wmsInboundReversal.state, 'COMPLETED');
  assert.equal(result.json.data.wmsInboundReversal.reversalDocumentId, 'WMS-RED-ADP-E2E-0001');
  assert.deepEqual(result.json.data.wmsInbound, originalBlue);

  result = await request('GET', `/bpi/v1/batches/${batchId}/release`);
  assert.equal(result.json.data.wmsInbound.documentId, originalBlue.documentId);
  assert.equal(result.json.data.wmsInboundReversal.originalDocumentId, originalBlue.documentId);
  assert.equal(result.json.data.wmsInboundReversal.state, 'COMPLETED');
  assert.deepEqual(simulatorState.batchEventsById.get(batchId).slice(-3).map((item) => item.action), [
    'WMS_INBOUND_REVERSAL_REQUESTED',
    'WMS_INBOUND_REVERSAL_APPROVED',
    'WMS_INBOUND_REVERSAL_ACCEPTED',
  ]);
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
      sourceSequenceRequired: true,
      sourceSequenceOrigin: 'DEVICE',
      sourceSequenceBindingFingerprint: SOURCE_SEQUENCE_FINGERPRINT,
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
  assert.deepEqual(result.json.data.points[0].readinessIssues, [
    'CALIBRATION_NOT_VERIFIED', 'SOURCE_SEQUENCE_EVIDENCE_MISSING',
  ]);

  result = await request('POST', '/__simulation/source-sequence-evidence', {
    body: {
      productId: 'PRODUCT-SUGAR', deviceId: 'DEVICE-S07-CALIBRATION',
      bindingFingerprint: SOURCE_SEQUENCE_FINGERPRINT,
      status: 'QUALIFIED', sequenceOrigin: 'DEVICE',
    },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.affectedPointCount, 1);
  assert.equal(result.json.data.catalog.points[0].sourceSequenceQualified, true);
  assert.deepEqual(result.json.data.catalog.points[0].readinessIssues, ['CALIBRATION_NOT_VERIFIED']);

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

test('point calibration cursor keeps a stable scope-bound snapshot', async () => {
  let result = await request('POST', '/__simulation/reset');
  assert.equal(result.response.status, 200);

  result = await request('GET', '/bpi/v1/point-calibrations?plantId=PLANT-01&lineId=LINE-S07-01&limit=1');
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.length, 1);
  assert.ok(result.json.meta.nextCursor);
  const firstId = result.json.data[0].id;
  const snapshotAt = result.json.meta.snapshotAt;
  const cursor = result.json.meta.nextCursor;

  await new Promise((resolve) => setTimeout(resolve, 5));
  const now = Date.now();
  result = await request('POST', '/bpi/v1/point-calibrations', {
    headers: commandHeaders('calibration-pagination-new-0001', 0),
    body: {
      plantId: 'PLANT-01',
      lineId: 'LINE-S07-01',
      productId: 'PRODUCT-SUGAR',
      deviceId: 'DEVICE-PAGINATION-NEW',
      propertyId: 'flow.instant',
      calibrationVersion: 'CAL-PAGINATION-NEW-1',
      certificateReference: 'urn:ft-mes:test:calibration:pagination-new-1',
      certificateChecksum: 'c'.repeat(64),
      validFrom: new Date(now - 60_000).toISOString(),
      validUntil: new Date(now + 86_400_000).toISOString(),
      reason: '验证快照之后提交的记录不会混入后续页',
    },
  });
  assert.equal(result.response.status, 200);
  const afterSnapshotId = result.json.data.id;

  result = await request('GET', `/bpi/v1/point-calibrations?plantId=PLANT-01&lineId=LINE-S07-01&limit=1&cursor=${encodeURIComponent(cursor)}`);
  assert.equal(result.response.status, 200);
  assert.equal(result.json.meta.snapshotAt, snapshotAt);
  assert.equal(result.json.meta.nextCursor, null);
  assert.equal(result.json.data.length, 1);
  assert.notEqual(result.json.data[0].id, firstId);
  assert.notEqual(result.json.data[0].id, afterSnapshotId);

  result = await request('GET', '/bpi/v1/point-calibrations?plantId=PLANT-01&lineId=LINE-S07-01&limit=1');
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data[0].id, afterSnapshotId);

  result = await request('GET', `/bpi/v1/point-calibrations?plantId=PLANT-01&lineId=LINE-S07-01&productId=OTHER&cursor=${encodeURIComponent(cursor)}`);
  assert.equal(result.response.status, 422);
  assert.match(result.json.detail, /does not match/);
  result = await request('GET', '/bpi/v1/point-calibrations?plantId=PLANT-01&lineId=LINE-S07-01&limit=201');
  assert.equal(result.response.status, 422);
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
      sourceSequenceRequired: true, sourceSequenceOrigin: 'DEVICE',
      sourceSequenceBindingFingerprint: SOURCE_SEQUENCE_FINGERPRINT,
    }],
  };
  const pointHeaders = commandHeaders('import-point-catalog-0001', 0);
  pointResult = await request('POST', '/bpi/v1/point-catalog/snapshots', { headers: pointHeaders, body: pointSnapshot });
  assert.equal(pointResult.response.status, 200);
  assert.equal(pointResult.json.data.snapshot.readyPointCount, 0);
  assert.deepEqual(pointResult.json.data.points[0].readinessIssues, ['SOURCE_SEQUENCE_EVIDENCE_MISSING']);
  const firstPointImport = pointResult.json;
  pointResult = await request('POST', '/bpi/v1/point-catalog/snapshots', { headers: pointHeaders, body: pointSnapshot });
  assert.equal(pointResult.response.headers.get('idempotent-replay'), 'true');
  assert.deepEqual(pointResult.json, firstPointImport);

  pointResult = await request('POST', '/__simulation/source-sequence-evidence', {
    body: {
      productId: 'PRODUCT-SUGAR', deviceId: 'DEVICE-S07-01',
      bindingFingerprint: SOURCE_SEQUENCE_FINGERPRINT,
      status: 'QUALIFIED', sequenceOrigin: 'DEVICE',
    },
  });
  assert.equal(pointResult.response.status, 200);
  assert.equal(pointResult.json.data.catalog.snapshot.readyPointCount, 1);

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
  let result = await request('POST', '/__simulation/reset');
  assert.equal(result.response.status, 200);

  result = await request('GET', '/bpi/v1/data-quality/incidents?plantId=PLANT-01');
  assert.equal(result.json.data.length, 6);
  assert.equal(result.json.data[0].affectedBatchCount, 1);

  result = await request('GET', '/bpi/v1/data-quality/incidents?plantId=PLANT-01&state=OPEN&search=clock');
  assert.equal(result.json.data.length, 1);
  const incident = result.json.data[0];
  assert.equal(incident.issueCode, 'CLOCK_DRIFT');
  assert.deepEqual(incident.affectedRules, ['RULE-S07-START@1.2.0']);

  result = await request('GET', '/bpi/v1/data-quality/summary?plantId=PLANT-01&lineId=LINE-S07-01');
  assert.deepEqual({
    open: result.json.data.open,
    acknowledged: result.json.data.acknowledged,
    resolved: result.json.data.resolved,
    critical: result.json.data.critical,
    affectedBatches: result.json.data.affectedBatches,
  }, { open: 3, acknowledged: 2, resolved: 1, critical: 1, affectedBatches: 1 });
  assert.equal(result.json.data.issueCounts.CLOCK_DRIFT, 1);

  result = await request('GET', `/bpi/v1/data-quality/incidents/${incident.id}`);
  assert.equal(result.json.data.incident.state, 'OPEN');
  assert.equal(result.json.data.events.length, 4);
  assert.equal(result.json.data.lifecycle.length, 1);
  assert.equal(result.json.data.recommendedActions.length, 3);

  result = await request('POST', `/bpi/v1/data-quality/incidents/${incident.id}/acknowledge`, {
    body: { assignee: 'shift.lead', reason: '确认时钟漂移并分派处理' },
  });
  assert.equal(result.response.status, 428);

  const acknowledgeHeaders = commandHeaders('ack-data-quality-0001', 1);
  result = await request('POST', `/bpi/v1/data-quality/incidents/${incident.id}/acknowledge`, {
    headers: acknowledgeHeaders,
    body: { assignee: 'shift.lead', reason: '确认时钟漂移并分派处理' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'ACKNOWLEDGED');
  assert.equal(result.json.data.revision, 2);
  const acknowledgedResponse = result.json;

  result = await request('POST', `/bpi/v1/data-quality/incidents/${incident.id}/acknowledge`, {
    headers: acknowledgeHeaders,
    body: { assignee: 'shift.lead', reason: '确认时钟漂移并分派处理' },
  });
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.deepEqual(result.json, acknowledgedResponse);

  result = await request('POST', `/bpi/v1/data-quality/incidents/${incident.id}/acknowledge`, {
    headers: commandHeaders('reassign-data-quality-0002', 2),
    body: { assignee: 'platform.engineer', reason: '转交平台工程师校时并复核网关' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.assignee, 'platform.engineer');
  assert.equal(result.json.data.revision, 3);

  result = await request('POST', `/bpi/v1/data-quality/incidents/${incident.id}/resolve`, {
    headers: commandHeaders('resolve-data-quality-0003', 3),
    body: { reason: '完成 NTP 校时并连续三个采集周期复核正常' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'RESOLVED');
  assert.equal(result.json.data.revision, 4);

  result = await request('GET', `/bpi/v1/data-quality/incidents/${incident.id}`);
  assert.equal(result.json.data.events.length, 4, 'raw events remain immutable after resolution');
  assert.deepEqual(result.json.data.lifecycle.map((item) => item.action), [
    'CREATED', 'ACKNOWLEDGED', 'REASSIGNED', 'RESOLVED',
  ]);

  result = await request('GET', '/bpi/v1/data-quality/summary?plantId=PLANT-01');
  assert.equal(result.json.data.open, 2);
  assert.equal(result.json.data.acknowledged, 2);
  assert.equal(result.json.data.resolved, 2);
  assert.equal(result.json.data.critical, 0);

  result = await request('GET', '/bpi/v1/integrations/health');
  const timescale = result.json.data.find((item) => item.id === 'timescaledb');
  assert.equal(timescale.status, 'DEGRADED');
  assert.match(timescale.businessImpact, /不阻断批次事实查询/);
});

test('dataset manifests stay immutable while versioned Parquet materialization is tracked separately', async () => {
  let result = await request('POST', '/__simulation/reset');
  assert.equal(result.response.status, 200);
  result = await request('POST', '/__simulation/prepare-dataset-manifest');
  assert.equal(result.json.preparedReviewCount, 3);
  const ruleVersionId = result.json.ruleVersionId;

  result = await request('GET', '/bpi/v1/datasets?plantId=PLANT-01&limit=100');
  assert.equal(result.response.status, 200);
  assert.deepEqual(result.json.data, []);

  const definitionBody = {
    datasetCode: 'BPI-START-BOUNDARY',
    version: '1.0.0',
    name: '启动边界受控训练清单',
    plantId: 'PLANT-01',
    lineIds: ['LINE-S07-01'],
    predictionTimePolicy: 'AUTOMATIC_BATCH_START',
    featureCutoffPolicy: 'AT_OR_BEFORE_PREDICTION_TIME',
    featureRefs: [
      'batch.order_id', 'batch.material_code', 'batch.stage_code',
      'rule.version_id', 'topology.version_id', 'point_catalog.snapshot_id',
    ],
    labelRefs: [
      'review.manual_start_time', 'review.manual_end_time',
      'review.reference_quantity', 'review.boundary_acceptance',
      'review.quantity_acceptance',
    ],
    maxLabelDelayHours: 48,
    minimumConfidence: 0.8,
    splitPolicy: 'PRODUCTION_TIME',
    reason: '建立 Phase 3A point-in-time 数据集清单',
  };
  const definitionHeaders = commandHeaders('dataset-definition-0001', 0);
  result = await request('POST', '/bpi/v1/datasets', {
    headers: definitionHeaders,
    body: definitionBody,
  });
  assert.equal(result.response.status, 200);
  const definition = result.json.data;
  assert.equal(definition.revision, 1);
  assert.equal(definition.latestSnapshot, null);
  assert.match(definition.checksum, /^[a-f0-9]{64}$/);

  result = await request('POST', '/bpi/v1/datasets', {
    headers: definitionHeaders,
    body: definitionBody,
  });
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.equal(result.json.data.id, definition.id);

  const snapshotBody = {
    freezeAt: '2026-07-12T08:00:00.000Z',
    lineIds: ['LINE-S07-01'],
    predictionTimePolicy: 'AUTOMATIC_BATCH_START',
    ruleVersionIds: [ruleVersionId],
    excludeLowConfidence: true,
    reason: '冻结受控影子复核样本并生成不可变清单',
  };
  const snapshotHeaders = commandHeaders('dataset-snapshot-0001', definition.revision);
  result = await request('POST', `/bpi/v1/datasets/${definition.id}/snapshots`, {
    headers: snapshotHeaders,
    body: snapshotBody,
  });
  assert.equal(result.response.status, 202);
  assert.equal(result.json.data.state, 'QUEUED');
  assert.equal(result.json.data.materializationState, 'NOT_STARTED');
  const queued = result.json.data;

  result = await request('POST', `/bpi/v1/datasets/${definition.id}/snapshots`, {
    headers: snapshotHeaders,
    body: snapshotBody,
  });
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.equal(result.json.data.id, queued.id);
  assert.equal(result.json.data.state, 'QUEUED');

  result = await request('GET', `/bpi/v1/dataset-snapshots/${queued.id}`);
  assert.equal(result.response.status, 200);
  const ready = result.json.data;
  assert.equal(ready.state, 'MANIFEST_READY');
  assert.equal(ready.includedCount, 1);
  assert.equal(ready.excludedCount, 2);
  assert.equal(ready.exclusionSummary.CONFIDENCE_BELOW_THRESHOLD, 1);
  assert.equal(ready.exclusionSummary.LABEL_DELAY_EXCEEDED, 1);
  assert.equal(ready.manifest.phaseBoundary.deliveryState, 'MANIFEST_ONLY');
  assert.equal(ready.manifest.phaseBoundary.materializationState, 'NOT_STARTED');
  assert.equal(ready.manifest.phaseBoundary.artifactUri, null);
  assert.equal(ready.manifest.phaseBoundary.icebergReady, false);
  assert.equal(ready.manifest.phaseBoundary.mlflowRegistered, false);
  assert.equal(ready.manifest.phaseBoundary.modelTrained, false);
  assert.equal(ready.manifest.samples.every((sample) => sample.predictionTime === sample.featureCutoffTime), true);
  assert.equal(ready.manifest.samples.every((sample) => !Object.keys(sample.features)
    .some((reference) => reference.startsWith('review.'))), true);
  assert.match(ready.manifestChecksum, /^[a-f0-9]{64}$/);

  const materializationHeaders = commandHeaders('dataset-materialization-0001', ready.revision);
  const materializationBody = {
    artifactFormat: 'PARQUET',
    reason: '生成独立的版本锁定 Parquet 数据集对象',
  };
  result = await request('POST', `/bpi/v1/dataset-snapshots/${ready.id}/materializations`, {
    headers: materializationHeaders,
    body: materializationBody,
  });
  assert.equal(result.response.status, 202);
  assert.equal(result.json.data.state, 'QUEUED');
  assert.equal(result.json.data.revision, 1);
  assert.equal(result.json.data.attemptCount, 0);
  const materializationId = result.json.data.id;
  const queuedMaterializationResponse = result.json;

  result = await request('POST', `/bpi/v1/dataset-snapshots/${ready.id}/materializations`, {
    headers: materializationHeaders,
    body: materializationBody,
  });
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.deepEqual(result.json, queuedMaterializationResponse);

  result = await request('GET', `/bpi/v1/dataset-materializations/${materializationId}`);
  assert.equal(result.json.data.state, 'WRITING');
  assert.equal(result.json.data.revision, 2);
  assert.equal(result.json.data.attemptCount, 1);

  result = await request('POST', '/__simulation/fail-dataset-materialization', {
    body: {
      materializationId,
      failureCode: 'SIMULATED_MINIO_TIMEOUT',
      failureDetail: '模拟 MinIO 写入超时，用于失败重排队验收。',
    },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'FAILED');
  assert.equal(result.json.data.revision, 3);
  assert.equal(result.json.data.artifactUri, null);

  result = await request('GET', `/bpi/v1/dataset-snapshots/${ready.id}`);
  assert.equal(result.json.data.materializationState, 'FAILED');
  assert.equal(result.json.data.latestMaterialization.failureCode, 'SIMULATED_MINIO_TIMEOUT');
  assert.equal(result.json.data.manifest.phaseBoundary.materializationState, 'NOT_STARTED');

  const retryHeaders = commandHeaders('dataset-materialization-retry-0002', 3);
  result = await request('POST', `/bpi/v1/dataset-materializations/${materializationId}/retry`, {
    headers: retryHeaders,
    body: { reason: '对象存储恢复后重新排队生成 Parquet' },
  });
  assert.equal(result.response.status, 202);
  assert.equal(result.json.data.state, 'QUEUED');
  assert.equal(result.json.data.revision, 4);
  const retriedResponse = result.json;

  result = await request('POST', `/bpi/v1/dataset-materializations/${materializationId}/retry`, {
    headers: retryHeaders,
    body: { reason: '对象存储恢复后重新排队生成 Parquet' },
  });
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.deepEqual(result.json, retriedResponse);

  result = await request('GET', `/bpi/v1/dataset-materializations/${materializationId}`);
  assert.equal(result.json.data.state, 'WRITING');
  assert.equal(result.json.data.revision, 5);
  assert.equal(result.json.data.attemptCount, 2);
  result = await request('GET', `/bpi/v1/dataset-materializations/${materializationId}`);
  const materialized = result.json.data;
  assert.equal(materialized.state, 'READY');
  assert.equal(materialized.revision, 6);
  assert.equal(materialized.rowCount, ready.includedCount);
  assert.match(materialized.contentSha256, /^[a-f0-9]{64}$/);
  assert.match(materialized.artifactUri, /^s3:\/\/bpi-datasets\/datasets\/.*\.parquet\?versionId=.+$/);
  assert.equal(materialized.objectKey.endsWith(`${materialized.contentSha256}.parquet`), true);
  assert.equal(materialized.artifactMetadata.objectContentVerified, true);
  assert.equal(materialized.artifactMetadata.simulationOnly, true);
  assert.equal(materialized.artifactMetadata.icebergReady, false);
  assert.equal(materialized.artifactMetadata.mlflowRegistered, false);
  assert.equal(materialized.artifactMetadata.modelTrained, false);

  result = await request('GET', `/bpi/v1/dataset-snapshots/${ready.id}`);
  assert.equal(result.json.data.materializationState, 'READY');
  assert.equal(result.json.data.artifactUri, materialized.artifactUri);
  assert.equal(result.json.data.latestMaterialization.id, materializationId);
  assert.equal(result.json.data.manifest.phaseBoundary.materializationState, 'NOT_STARTED');
  assert.equal(result.json.data.manifest.phaseBoundary.artifactUri, null);
  assert.equal(result.json.data.manifest.phaseBoundary.icebergReady, false);
  assert.equal(result.json.data.manifest.phaseBoundary.mlflowRegistered, false);
  assert.equal(result.json.data.manifest.phaseBoundary.modelTrained, false);

  result = await request('GET', `/bpi/v1/dataset-materializations/${materializationId}/catalog-publications`);
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data, null);

  const publicationHeaders = commandHeaders('dataset-catalog-publication-0001', materialized.revision);
  const publicationBody = { reason: '把精确 Parquet 版本发布到 Iceberg REST Catalog' };
  result = await request('POST', `/bpi/v1/dataset-materializations/${materializationId}/catalog-publications`, {
    headers: publicationHeaders,
    body: publicationBody,
  });
  assert.equal(result.response.status, 202);
  assert.equal(result.json.data.state, 'QUEUED');
  assert.equal(result.json.data.sourceContentSha256, materialized.contentSha256);
  assert.equal(result.json.data.sourceObjectVersionId, materialized.artifactMetadata.objectVersionId);
  const publicationId = result.json.data.id;
  const queuedPublicationResponse = result.json;

  result = await request('POST', `/bpi/v1/dataset-materializations/${materializationId}/catalog-publications`, {
    headers: publicationHeaders,
    body: publicationBody,
  });
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.deepEqual(result.json, queuedPublicationResponse);

  result = await request('GET', `/bpi/v1/dataset-materializations/${materializationId}/catalog-publications`);
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.id, publicationId);
  assert.equal(result.json.data.state, 'QUEUED');

  result = await request('GET', `/bpi/v1/dataset-catalog-publications/${publicationId}`);
  assert.equal(result.json.data.state, 'COMMITTING');
  assert.equal(result.json.data.revision, 2);
  assert.equal(result.json.data.attemptCount, 1);

  result = await request('POST', '/__simulation/fail-dataset-catalog-publication', {
    body: {
      publicationId,
      failureCode: 'SIMULATED_POLARIS_TIMEOUT',
      failureDetail: '模拟 Polaris 目录提交超时，用于失败重排队验收。',
    },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'FAILED');
  assert.equal(result.json.data.revision, 3);

  const publicationRetryHeaders = commandHeaders('dataset-catalog-publication-retry-0002', 3);
  result = await request('POST', `/bpi/v1/dataset-catalog-publications/${publicationId}/retry`, {
    headers: publicationRetryHeaders,
    body: { reason: 'Polaris 恢复后重新排队并复验目录快照' },
  });
  assert.equal(result.response.status, 202);
  assert.equal(result.json.data.state, 'QUEUED');
  assert.equal(result.json.data.revision, 4);

  result = await request('GET', `/bpi/v1/dataset-catalog-publications/${publicationId}`);
  assert.equal(result.json.data.state, 'COMMITTING');
  result = await request('GET', `/bpi/v1/dataset-catalog-publications/${publicationId}`);
  assert.equal(result.json.data.state, 'VERIFYING');
  result = await request('GET', `/bpi/v1/dataset-catalog-publications/${publicationId}`);
  const published = result.json.data;
  assert.equal(published.state, 'READY');
  assert.equal(published.revision, 7);
  assert.equal(published.attemptCount, 2);
  assert.equal(published.icebergSnapshotId, '9223372036854775001');
  assert.equal(published.verifiedRowCount, materialized.rowCount);
  assert.equal(published.catalogMetadata.catalogSnapshotVerified, true);
  assert.equal(published.catalogMetadata.sourceVersionVerified, true);
  assert.match(published.semanticChecksum, /^[a-f0-9]{64}$/);
  assert.match(published.tableIdentifier, /^ft_mes_bpi\.bpi_training\.tenant_[a-f0-9]{16}\.dataset_[a-f0-9]+$/);

  result = await request('GET', `/bpi/v1/dataset-catalog-publications/${publicationId}/retention-archives`);
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data, null);

  result = await request('POST', `/bpi/v1/dataset-catalog-publications/${publicationId}/retention-archives`, {
    headers: commandHeaders('dataset-retention-archive-stale-0001', published.revision - 1),
    body: { reason: '冻结精确数据集恢复包并验证对象保留' },
  });
  assert.equal(result.response.status, 409);
  assert.equal(result.json.currentRevision, published.revision);

  const archiveHeaders = commandHeaders('dataset-retention-archive-0002', published.revision);
  const archiveBody = { reason: '冻结精确数据集恢复包并验证对象保留' };
  result = await request('POST', `/bpi/v1/dataset-catalog-publications/${publicationId}/retention-archives`, {
    headers: archiveHeaders,
    body: archiveBody,
  });
  assert.equal(result.response.status, 202);
  assert.equal(result.json.data.state, 'QUEUED');
  assert.equal(result.json.data.revision, 1);
  assert.equal(result.json.data.catalogVerifiedRowCount, published.verifiedRowCount);
  assert.equal(result.json.data.catalogSemanticChecksum, published.semanticChecksum);
  const archiveId = result.json.data.id;
  const queuedArchiveResponse = result.json;

  result = await request('POST', `/bpi/v1/dataset-catalog-publications/${publicationId}/retention-archives`, {
    headers: archiveHeaders,
    body: archiveBody,
  });
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.deepEqual(result.json, queuedArchiveResponse);

  result = await request('GET', `/bpi/v1/dataset-catalog-publications/${publicationId}/retention-archives`);
  assert.equal(result.json.data.id, archiveId);
  assert.equal(result.json.data.state, 'QUEUED');

  result = await request('GET', `/bpi/v1/dataset-retention-archives/${archiveId}`);
  assert.equal(result.json.data.state, 'ARCHIVING');
  assert.equal(result.json.data.revision, 2);
  assert.equal(result.json.data.attemptCount, 1);
  assert.equal(result.json.data.retentionMode, 'GOVERNANCE');
  assert.equal(result.json.data.legalHoldEnabled, false);

  result = await request('POST', '/__simulation/fail-dataset-retention-archive', {
    body: {
      archiveId,
      failureCode: 'SIMULATED_OBJECT_LOCK_TIMEOUT',
      failureDetail: '模拟 Object Lock 写入超时，用于失败重排队验收。',
    },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'FAILED');
  assert.equal(result.json.data.revision, 3);
  assert.equal(result.json.data.archiveBucket, null);

  const archiveRetryHeaders = commandHeaders('dataset-retention-archive-retry-0003', 3);
  result = await request('POST', `/bpi/v1/dataset-retention-archives/${archiveId}/retry`, {
    headers: archiveRetryHeaders,
    body: { reason: '对象锁服务恢复后重新归档并执行恢复校验' },
  });
  assert.equal(result.response.status, 202);
  assert.equal(result.json.data.state, 'QUEUED');
  assert.equal(result.json.data.revision, 4);
  const retriedArchiveResponse = result.json;

  result = await request('POST', `/bpi/v1/dataset-retention-archives/${archiveId}/retry`, {
    headers: archiveRetryHeaders,
    body: { reason: '对象锁服务恢复后重新归档并执行恢复校验' },
  });
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.deepEqual(result.json, retriedArchiveResponse);

  result = await request('GET', `/bpi/v1/dataset-retention-archives/${archiveId}`);
  assert.equal(result.json.data.state, 'ARCHIVING');
  assert.equal(result.json.data.revision, 5);
  assert.equal(result.json.data.attemptCount, 2);
  result = await request('GET', `/bpi/v1/dataset-retention-archives/${archiveId}`);
  assert.equal(result.json.data.state, 'VERIFYING');
  assert.equal(result.json.data.revision, 6);
  assert.equal(result.json.data.archiveBucket, 'bpi-dataset-recovery');
  assert.match(result.json.data.sourceArchiveVersionId, /^[0-9a-f-]{36}$/);
  assert.match(result.json.data.archiveManifestSha256, /^[a-f0-9]{64}$/);
  result = await request('GET', `/bpi/v1/dataset-retention-archives/${archiveId}`);
  const lockedArchive = result.json.data;
  assert.equal(lockedArchive.state, 'LOCKED');
  assert.equal(lockedArchive.revision, 7);
  assert.equal(lockedArchive.verifiedRowCount, published.verifiedRowCount);
  assert.equal(lockedArchive.verifiedSemanticChecksum, published.semanticChecksum);
  assert.equal(lockedArchive.archiveMetadata.objectLockVerified, true);
  assert.equal(lockedArchive.archiveMetadata.recoveryVerified, true);
  assert.equal(lockedArchive.archiveMetadata.mlflowRegistered, false);
  assert.equal(lockedArchive.archiveMetadata.modelTrained, false);

  result = await request('POST', `/bpi/v1/dataset-retention-archives/${archiveId}/retry`, {
    headers: commandHeaders('dataset-retention-archive-locked-0004', lockedArchive.revision),
    body: { reason: 'LOCKED 恢复包不可重新排队或覆盖' },
  });
  assert.equal(result.response.status, 409);
  assert.equal(result.json.currentRevision, lockedArchive.revision);

  result = await request('GET', `/bpi/v1/dataset-retention-archives/${archiveId}/mlflow-registrations`);
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data, null);

  result = await request('POST', `/bpi/v1/dataset-retention-archives/${archiveId}/mlflow-registrations`, {
    headers: commandHeaders('dataset-mlflow-registration-stale-0001', lockedArchive.revision - 1),
    body: { reason: '登记精确恢复对象为 MLflow Dataset Input' },
  });
  assert.equal(result.response.status, 409);
  assert.equal(result.json.currentRevision, lockedArchive.revision);

  const registrationHeaders = commandHeaders('dataset-mlflow-registration-0002', lockedArchive.revision);
  const registrationBody = { reason: '登记精确恢复对象为 MLflow Dataset Input' };
  result = await request('POST', `/bpi/v1/dataset-retention-archives/${archiveId}/mlflow-registrations`, {
    headers: registrationHeaders,
    body: registrationBody,
  });
  assert.equal(result.response.status, 202);
  assert.equal(result.json.data.state, 'QUEUED');
  assert.equal(result.json.data.revision, 1);
  assert.equal(result.json.data.registrarVersion, 'bpi-dataset-mlflow-registrar/0.1.0');
  assert.equal(result.json.data.trackingProfile, 'bpi-mlflow-dataset-v1');
  assert.equal(result.json.data.datasetDigest, published.semanticChecksum.slice(0, 16));
  const registrationId = result.json.data.id;
  const queuedRegistrationResponse = result.json;

  result = await request('POST', `/bpi/v1/dataset-retention-archives/${archiveId}/mlflow-registrations`, {
    headers: registrationHeaders,
    body: registrationBody,
  });
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.deepEqual(result.json, queuedRegistrationResponse);

  result = await request('GET', `/bpi/v1/dataset-retention-archives/${archiveId}/mlflow-registrations`);
  assert.equal(result.json.data.id, registrationId);
  assert.equal(result.json.data.state, 'QUEUED');

  result = await request('GET', `/bpi/v1/dataset-mlflow-registrations/${registrationId}`);
  assert.equal(result.json.data.state, 'REGISTERING');
  assert.equal(result.json.data.revision, 2);
  assert.equal(result.json.data.attemptCount, 1);

  result = await request('POST', '/__simulation/fail-dataset-mlflow-registration', {
    body: {
      registrationId,
      failureCode: 'SIMULATED_MLFLOW_TIMEOUT',
      failureDetail: '模拟 MLflow Tracking 超时，用于失败重排队验收。',
    },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'FAILED');
  assert.equal(result.json.data.revision, 3);
  assert.equal(result.json.data.mlflowRunId, null);

  const registrationRetryHeaders = commandHeaders('dataset-mlflow-registration-retry-0003', 3);
  result = await request('POST', `/bpi/v1/dataset-mlflow-registrations/${registrationId}/retry`, {
    headers: registrationRetryHeaders,
    body: { reason: 'MLflow Tracking 恢复后重新登记并复验血缘' },
  });
  assert.equal(result.response.status, 202);
  assert.equal(result.json.data.state, 'QUEUED');
  assert.equal(result.json.data.revision, 4);
  const retriedRegistrationResponse = result.json;

  result = await request('POST', `/bpi/v1/dataset-mlflow-registrations/${registrationId}/retry`, {
    headers: registrationRetryHeaders,
    body: { reason: 'MLflow Tracking 恢复后重新登记并复验血缘' },
  });
  assert.equal(result.response.headers.get('idempotent-replay'), 'true');
  assert.deepEqual(result.json, retriedRegistrationResponse);

  result = await request('GET', `/bpi/v1/dataset-mlflow-registrations/${registrationId}`);
  assert.equal(result.json.data.state, 'REGISTERING');
  assert.equal(result.json.data.revision, 5);
  assert.equal(result.json.data.attemptCount, 2);
  result = await request('GET', `/bpi/v1/dataset-mlflow-registrations/${registrationId}`);
  const registeredDataset = result.json.data;
  assert.equal(registeredDataset.state, 'REGISTERED');
  assert.equal(registeredDataset.revision, 6);
  assert.match(registeredDataset.mlflowRunId, /^[a-f0-9]{32}$/);
  assert.match(registeredDataset.mlflowArtifactUri, /^mlflow-artifacts:\//);
  assert.equal(
    registeredDataset.mlflowDatasetSource,
    `s3://${lockedArchive.archiveBucket}/${lockedArchive.sourceArchiveObjectKey}?versionId=${lockedArchive.sourceArchiveVersionId}`,
  );
  assert.equal(registeredDataset.registrationMetadata.datasetInputVerified, true);
  assert.equal(registeredDataset.registrationMetadata.lineageVerified, true);
  assert.equal(registeredDataset.registrationMetadata.modelTrained, false);
  assert.equal(registeredDataset.registrationMetadata.modelRegistered, false);
  assert.equal(registeredDataset.registrationMetadata.onlineInferenceEnabled, false);
  assert.equal(registeredDataset.registrationMetadata.productionActivationAllowed, false);

  result = await request('POST', `/bpi/v1/dataset-mlflow-registrations/${registrationId}/retry`, {
    headers: commandHeaders('dataset-mlflow-registration-registered-0004', registeredDataset.revision),
    body: { reason: 'REGISTERED 数据集输入不可再次排队或转为模型' },
  });
  assert.equal(result.response.status, 409);
  assert.equal(result.json.currentRevision, registeredDataset.revision);

  result = await request('POST', `/bpi/v1/dataset-materializations/${materializationId}/retry`, {
    headers: commandHeaders('dataset-materialization-ready-retry-0003', materialized.revision),
    body: { reason: 'READY 对象必须保持不可变，禁止再次排队' },
  });
  assert.equal(result.response.status, 409);
  assert.equal(result.json.currentRevision, materialized.revision);

  result = await request('POST', `/bpi/v1/datasets/${definition.id}/snapshots`, {
    headers: commandHeaders('dataset-snapshot-0002', definition.revision),
    body: snapshotBody,
  });
  const secondId = result.json.data.id;
  result = await request('GET', `/bpi/v1/dataset-snapshots/${secondId}`);
  assert.equal(result.json.data.manifestChecksum, ready.manifestChecksum,
    'snapshot identity must not change the deterministic manifest checksum');

  result = await request('GET', '/bpi/v1/datasets?plantId=PLANT-01&limit=100');
  assert.equal(result.json.data.length, 1);
  assert.equal(result.json.data[0].latestSnapshot.snapshotVersion, 2);
  assert.equal(result.json.data[0].latestSnapshot.state, 'MANIFEST_READY');
});

test('shadow run acceptance is version-pinned, review-driven and fail-closed on critical data quality', async () => {
  let result = await request('POST', '/__simulation/reset');
  assert.equal(result.response.status, 200);
  result = await request('POST', '/__simulation/prepare-shadow-run');
  assert.equal(result.json.preparedBatchCount, 10);

  const batches = (await request('GET', '/bpi/v1/batches?plantId=PLANT-01')).json.data;
  assert.equal(batches.length, 10);

  async function createRun(code) {
    const response = await request('POST', '/bpi/v1/shadow-runs', {
      headers: commandHeaders(`create-${code}`, 0),
      body: {
        runCode: code,
        name: `${code} acceptance`,
        plantId: 'PLANT-01',
        lineId: 'LINE-S07-01',
        ruleVersionId: RULE_ID,
        minimumDurationDays: 7,
        minimumReviewedBatches: 10,
        boundaryToleranceSeconds: 60,
        minimumBoundaryAgreement: 0.95,
        quantityTolerancePercent: 2,
        reason: '建立受控影子运行验收任务',
      },
    });
    assert.equal(response.response.status, 200);
    assert.equal(response.json.data.state, 'DRAFT');
    assert.equal(response.json.data.readiness.ready, true);
    return response.json.data;
  }

  async function startAndReviewAll(run, keyPrefix) {
    let response = await request('POST', `/bpi/v1/shadow-runs/${run.id}/start`, {
      headers: commandHeaders(`${keyPrefix}-start`, run.revision),
      body: { reason: '确认固定版本运行就绪并启动观察' },
    });
    assert.equal(response.response.status, 200);
    run = response.json.data;
    assert.equal(run.state, 'RUNNING');
    assert.equal(run.metrics.durationGatePassed, true);

    response = await request('GET', `/bpi/v1/shadow-runs/${run.id}/batch-reviews`);
    assert.deepEqual(response.json.data, []);

    for (let index = 0; index < batches.length; index += 1) {
      const batch = batches[index];
      const headers = commandHeaders(`${keyPrefix}-review-${index + 1}`, run.revision);
      response = await request('POST', `/bpi/v1/shadow-runs/${run.id}/batch-reviews`, {
        headers,
        body: {
          batchId: batch.id,
          manualStartTime: batch.startTime,
          manualEndTime: batch.endTime,
          referenceQuantity: batch.quantity,
          quantityUnit: batch.quantityUnit,
          reason: `人工复核第 ${index + 1} 个批次边界与累计量`,
        },
      });
      assert.equal(response.response.status, 200);
      run = response.json.data.run;
      assert.equal(response.json.data.review.startBoundaryAccepted, true);
      assert.equal(response.json.data.review.quantityWithinTolerance, true);
      if (index === 0) {
        const replay = await request('POST', `/bpi/v1/shadow-runs/${run.id}/batch-reviews`, {
          headers,
          body: {
            batchId: batch.id,
            manualStartTime: batch.startTime,
            manualEndTime: batch.endTime,
            referenceQuantity: batch.quantity,
            quantityUnit: batch.quantityUnit,
            reason: '人工复核第 1 个批次边界与累计量',
          },
        });
        assert.equal(replay.response.headers.get('idempotent-replay'), 'true');
        assert.equal(replay.json.data.run.revision, run.revision);
      }
    }
    assert.equal(run.metrics.reviewedBatchCount, 10);
    assert.equal(run.metrics.boundaryAgreement, 1);
    assert.equal(run.metrics.cumulativeQuantityDeviationPercent, 0);
    response = await request('GET', `/bpi/v1/shadow-runs/${run.id}/batch-reviews`);
    assert.equal(response.json.data.length, 10);
    return run;
  }

  result = await request('GET', '/bpi/v1/shadow-runs?plantId=PLANT-01');
  assert.deepEqual(result.json.data, []);
  let approvedRun = await createRun('SHADOW-ACCEPT-APPROVE');
  result = await request('GET', `/bpi/v1/shadow-runs/${approvedRun.id}`);
  assert.equal(result.json.data.ruleVersion, 'RULE-S07-START@1.2.0');
  assert.equal(result.json.data.pointCatalogSnapshotId, '4d9c5df8-7ee0-58e2-a143-9ca5d37a7b21');

  approvedRun = await startAndReviewAll(approvedRun, 'approve-flow');
  result = await request('POST', `/bpi/v1/shadow-runs/${approvedRun.id}/complete`, {
    headers: commandHeaders('approve-flow-complete', approvedRun.revision),
    body: { reason: '观察周期和人工复核样本均已满足' },
  });
  assert.equal(result.response.status, 200);
  approvedRun = result.json.data;
  assert.equal(approvedRun.state, 'EVALUATING');
  assert.equal(approvedRun.readyForApproval, false);
  assert.ok(approvedRun.blockers.includes('UNRESOLVED_CRITICAL_DATA_QUALITY'));

  result = await request('POST', `/bpi/v1/shadow-runs/${approvedRun.id}/approve`, {
    headers: commandHeaders('approve-flow-blocked', approvedRun.revision),
    body: { reason: '尝试在严重数据质量事件未解决时批准' },
  });
  assert.equal(result.response.status, 422);
  assert.match(result.json.detail, /UNRESOLVED_CRITICAL_DATA_QUALITY/);

  const critical = (await request('GET', '/bpi/v1/data-quality/incidents?plantId=PLANT-01&state=OPEN&search=clock')).json.data[0];
  result = await request('POST', `/bpi/v1/data-quality/incidents/${critical.id}/acknowledge`, {
    headers: commandHeaders('shadow-critical-ack', critical.revision),
    body: { assignee: 'platform.engineer', reason: '确认时钟漂移并安排校时' },
  });
  assert.equal(result.response.status, 200);
  result = await request('POST', `/bpi/v1/data-quality/incidents/${critical.id}/resolve`, {
    headers: commandHeaders('shadow-critical-resolve', result.json.data.revision),
    body: { reason: '完成 NTP 校时并连续采集复核通过' },
  });
  assert.equal(result.response.status, 200);

  result = await request('GET', `/bpi/v1/shadow-runs/${approvedRun.id}`);
  approvedRun = result.json.data;
  assert.equal(approvedRun.readyForApproval, true);
  result = await request('POST', `/bpi/v1/shadow-runs/${approvedRun.id}/approve`, {
    headers: commandHeaders('approve-flow-final', approvedRun.revision),
    body: { reason: '独立管理员复核版本、样本、指标和数据质量后批准' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'APPROVED');
  assert.equal(result.json.data.revision, 14);

  let rejectedRun = await createRun('SHADOW-ACCEPT-REJECT');
  rejectedRun = await startAndReviewAll(rejectedRun, 'reject-flow');
  result = await request('POST', `/bpi/v1/shadow-runs/${rejectedRun.id}/complete`, {
    headers: commandHeaders('reject-flow-complete', rejectedRun.revision),
    body: { reason: '完成观察并提交独立评估' },
  });
  rejectedRun = result.json.data;
  result = await request('POST', `/bpi/v1/shadow-runs/${rejectedRun.id}/reject`, {
    headers: commandHeaders('reject-flow-final', rejectedRun.revision),
    body: { reason: '独立管理员认为当前样本代表性不足，驳回本轮验收' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'REJECTED');

  const cancelledRun = await createRun('SHADOW-ACCEPT-CANCEL');
  result = await request('POST', `/bpi/v1/shadow-runs/${cancelledRun.id}/cancel`, {
    headers: commandHeaders('cancel-flow-final', cancelledRun.revision),
    body: { reason: '生产计划切换，取消尚未启动的验收任务' },
  });
  assert.equal(result.response.status, 200);
  assert.equal(result.json.data.state, 'CANCELLED');

  result = await request('GET', '/bpi/v1/shadow-runs?plantId=PLANT-01&state=APPROVED');
  assert.equal(result.json.data.length, 1);
  assert.equal(result.json.data[0].runCode, 'SHADOW-ACCEPT-APPROVE');
});
