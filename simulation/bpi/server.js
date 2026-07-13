const http = require('node:http');
const { FIXED_TIME, clone, createScenario, sha256 } = require('./scenario');

const JSON_TYPE = 'application/json; charset=utf-8';
const PROBLEM_TYPE = 'application/problem+json; charset=utf-8';

function meta(operationId) {
  return {
    traceId: `sim-${sha256(operationId).slice(0, 16)}`,
    generatedAt: FIXED_TIME,
    snapshotAt: FIXED_TIME,
    nextCursor: null,
  };
}

function envelope(operationId, data) {
  return { data: clone(data), meta: meta(operationId) };
}

function problem(status, title, detail, operationId, currentRevision = null) {
  return {
    type: `https://ft-mes.local/problems/${status}`,
    title,
    status,
    detail,
    traceId: meta(operationId).traceId,
    currentRevision,
  };
}

function send(res, status, body, operationId, headers = {}) {
  const serialized = JSON.stringify(body);
  res.writeHead(status, {
    'Content-Type': status >= 400 ? PROBLEM_TYPE : JSON_TYPE,
    'Content-Length': Buffer.byteLength(serialized),
    'X-BPI-Operation-Id': operationId,
    ...headers,
  });
  res.end(serialized);
}

async function readJson(req) {
  const chunks = [];
  for await (const chunk of req) {
    chunks.push(chunk);
  }
  if (chunks.length === 0) return {};
  return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}

function revisionFromHeader(value) {
  const match = String(value || '').match(/(\d+)/);
  return match ? Number(match[1]) : null;
}

function commandContext(req, res, operationId, entityRevision, state, pathname) {
  const key = req.headers['idempotency-key'];
  const revision = revisionFromHeader(req.headers['if-match']);
  if (!key || revision === null) {
    send(
      res,
      428,
      problem(428, 'Precondition Required', 'Idempotency-Key and If-Match are required.', operationId),
      operationId,
    );
    return null;
  }
  const cacheKey = String(key);
  if (state.idempotency.has(cacheKey)) {
    const cached = state.idempotency.get(cacheKey);
    if (cached.method !== req.method || cached.pathname !== pathname) {
      send(res, 409, problem(409, 'Idempotency Conflict', 'Idempotency-Key was reused for another command.', operationId), operationId);
      return null;
    }
    send(res, cached.status, cached.body, operationId, { 'Idempotent-Replay': 'true' });
    return null;
  }
  if (revision !== entityRevision) {
    const body = problem(409, 'Revision Conflict', `Expected revision ${entityRevision}, received ${revision}.`, operationId, entityRevision);
    send(res, 409, body, operationId);
    return null;
  }
  return { key, cacheKey, method: req.method, pathname };
}

function rememberAndSend(state, context, res, status, body, operationId) {
  state.idempotency.set(context.cacheKey, {
    method: context.method,
    pathname: context.pathname,
    status,
    body: clone(body),
  });
  send(res, status, body, operationId);
}

function batchFromCandidate(candidate) {
  return {
    id: 'BATCH-S07-20260712-001', batchNo: 'S07-20260712-001', lineId: candidate.lineId,
    stageCode: 'EVAPORATION', orderId: candidate.orderId, materialCode: 'MAT-SYRUP-001',
    state: 'ACTIVE', revision: 1, shadow: true, startTime: candidate.boundaryTime, endTime: null,
    quantity: 12.4, quantityUnit: 't', dryMatter: 7.86, qualityGate: 'PENDING',
    wmsStatus: 'NOT_REQUESTED', ruleVersion: candidate.ruleVersion, topologyVersion: candidate.topologyVersion,
  };
}

function endCandidateFromStart(candidate) {
  const boundaryTime = '2026-07-12T08:29:40.000Z';
  return {
    id: 'CAND-END-S07-001',
    candidateKey: '93cdf206-4664-58bd-b7c9-cdbca49e74d4',
    boundaryType: 'END',
    lineId: candidate.lineId,
    orderId: candidate.orderId,
    batchId: null,
    boundaryTime,
    state: 'PENDING',
    revision: 1,
    confidence: 0.96,
    ruleVersion: 'RULE-S07-END@1.2.0',
    topologyVersion: candidate.topologyVersion,
    missingSignals: [],
    evidence: [
      {
        eventId: 'EVT-END-FLOW-001', signal: 'instantFlowBelowStopThreshold', classification: 'QUORUM',
        satisfied: true, value: 0.2, unit: 't/h', quality: 'GOOD', eventTime: boundaryTime, source: 'JetLinks',
      },
      {
        eventId: 'EVT-END-PUMP-001', signal: 'feedPumpStopped', classification: 'QUORUM',
        satisfied: true, value: true, unit: null, quality: 'GOOD', eventTime: boundaryTime, source: 'JetLinks',
      },
      {
        eventId: 'EVT-END-VALVE-001', signal: 'transferPathClosed', classification: 'QUORUM',
        satisfied: true, value: true, unit: null, quality: 'GOOD', eventTime: boundaryTime, source: 'JetLinks',
      },
    ],
    review: null,
  };
}

function candidates(state) {
  return [state.candidate, state.endCandidate].filter(Boolean);
}

function match(pathname, pattern) {
  const result = pathname.match(pattern);
  return result ? result.slice(1).map(decodeURIComponent) : null;
}

function createHandler(state) {
  return async (req, res) => {
    const url = new URL(req.url, 'http://127.0.0.1');
    const path = url.pathname;
    let ids;

    try {
      if (req.method === 'GET' && path === '/health') {
        return send(res, 200, { status: 'UP', mode: 'phase-1-shadow', externalWrites: false }, 'simulationHealth');
      }
      if (req.method === 'POST' && path === '/__simulation/reset') {
        Object.assign(state, createScenario());
        return send(res, 200, { status: 'RESET' }, 'simulationReset');
      }
      if (req.method === 'POST' && path === '/__simulation/fail-rule-publication') {
        if (state.rule.state !== 'PUBLISHED') {
          return send(res, 409, { status: 'NOT_PUBLISHED' }, 'simulationFailRulePublication');
        }
        state.rule.publicationStatus = 'FAILED';
        state.rule.publicationRevision = 11;
        state.rule.publicationAttemptCount = 5;
        state.rule.publicationTotalAttemptCount = 5;
        state.rule.publicationLastError = 'Simulated Kafka broker outage';
        return send(res, 200, { status: 'FAILED' }, 'simulationFailRulePublication');
      }
      if (req.method === 'GET' && path === '/bpi/v1/overview') {
        return send(res, 200, envelope('getBpiOverview', [state.line]), 'getBpiOverview');
      }
      ids = match(path, /^\/bpi\/v1\/lines\/([^/]+)\/current-state$/);
      if (req.method === 'GET' && ids) {
        if (ids[0] !== state.line.lineId) return send(res, 404, problem(404, 'Not Found', 'Line not found.', 'getCurrentLineState'), 'getCurrentLineState');
        return send(res, 200, envelope('getCurrentLineState', state.line), 'getCurrentLineState');
      }
      if (req.method === 'GET' && path === '/bpi/v1/candidates') {
        const requestedState = url.searchParams.get('state');
        const items = candidates(state).filter((candidate) => !requestedState || candidate.state === requestedState);
        return send(res, 200, envelope('listBatchCandidates', items), 'listBatchCandidates');
      }
      ids = match(path, /^\/bpi\/v1\/candidates\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const candidate = candidates(state).find((item) => item.id === ids[0]);
        if (!candidate) return send(res, 404, problem(404, 'Not Found', 'Candidate not found.', 'getBatchCandidate'), 'getBatchCandidate');
        return send(res, 200, envelope('getBatchCandidate', candidate), 'getBatchCandidate');
      }
      ids = match(path, /^\/bpi\/v1\/candidates\/([^/]+)\/confirm$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'confirmBatchCandidate';
        const candidate = candidates(state).find((item) => item.id === ids[0]);
        if (!candidate) return send(res, 404, problem(404, 'Not Found', 'Candidate not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, candidate.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (!body.reason || String(body.reason).trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'A reason of at least 3 characters is required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (candidate.state !== 'PENDING') {
          const response = problem(409, 'Candidate Already Processed', 'The candidate is no longer pending.', operationId, candidate.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        let batch;
        if (candidate.boundaryType === 'START') {
          if (state.batches.some((item) => item.lineId === candidate.lineId && ['ACTIVE', 'SUSPENDED'].includes(item.state))) {
            const response = problem(409, 'Open Batch Exists', 'The line already has an open batch.', operationId, candidate.revision);
            return rememberAndSend(state, context, res, 409, response, operationId);
          }
          batch = batchFromCandidate(candidate);
          state.batches.push(batch);
          state.endCandidate = endCandidateFromStart(candidate);
          state.line.currentBatchId = batch.id;
          state.line.pendingCandidates = 1;
          state.batchEvents = [
            { revision: 1, action: 'SHADOW_BATCH_CREATED', at: FIXED_TIME, actor: 'simulated.shift.lead', reason: body.reason, fromState: null, toState: 'ACTIVE' },
          ];
        } else {
          batch = state.batches.find((item) => item.lineId === candidate.lineId
            && item.orderId === candidate.orderId && item.state === 'ACTIVE');
          if (!batch) {
            const response = problem(409, 'Active Batch Not Found', 'No matching ACTIVE batch can be closed.', operationId, candidate.revision);
            return rememberAndSend(state, context, res, 409, response, operationId);
          }
          if (new Date(candidate.boundaryTime) <= new Date(batch.startTime)) {
            const response = problem(409, 'Invalid End Boundary', 'END boundary must be after START boundary.', operationId, candidate.revision);
            return rememberAndSend(state, context, res, 409, response, operationId);
          }
          batch.state = 'CLOSED_RAW';
          batch.revision += 1;
          batch.endTime = candidate.boundaryTime;
          state.line.currentBatchId = null;
          state.line.status = 'IDLE';
          state.line.pendingCandidates = 0;
          state.batchEvents.push({
            revision: batch.revision,
            action: 'END_BOUNDARY_CONFIRMED',
            at: FIXED_TIME,
            actor: 'simulated.shift.lead',
            reason: body.reason,
            fromState: 'ACTIVE',
            toState: 'CLOSED_RAW',
          });
        }
        candidate.state = 'CONFIRMED';
        candidate.revision += 1;
        candidate.batchId = batch.id;
        candidate.review = { actor: 'simulated.shift.lead', reason: body.reason, at: FIXED_TIME };
        const response = envelope(operationId, { candidate, batch });
        return rememberAndSend(state, context, res, 200, response, operationId);
      }
      ids = match(path, /^\/bpi\/v1\/candidates\/([^/]+)\/reject$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'rejectBatchCandidate';
        const candidate = candidates(state).find((item) => item.id === ids[0]);
        if (!candidate) return send(res, 404, problem(404, 'Not Found', 'Candidate not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, candidate.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (!body.reason || String(body.reason).trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'A reason of at least 3 characters is required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (candidate.state !== 'PENDING') {
          const response = problem(409, 'Candidate Already Processed', 'The candidate is no longer pending.', operationId, candidate.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        candidate.state = 'REJECTED';
        candidate.revision += 1;
        candidate.batchId = null;
        candidate.review = { actor: 'simulated.shift.lead', reason: body.reason, at: FIXED_TIME };
        state.line.pendingCandidates = 0;
        const response = envelope(operationId, candidate);
        return rememberAndSend(state, context, res, 200, response, operationId);
      }
      if (req.method === 'GET' && path === '/bpi/v1/batches') {
        return send(res, 200, envelope('listBatches', state.batches), 'listBatches');
      }
      ids = match(path, /^\/bpi\/v1\/batches\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const batch = state.batches.find((item) => item.id === ids[0]);
        if (!batch) return send(res, 404, problem(404, 'Not Found', 'Batch not found.', 'getBatch'), 'getBatch');
        return send(res, 200, envelope('getBatch', batch), 'getBatch');
      }
      ids = match(path, /^\/bpi\/v1\/batches\/([^/]+)\/(suspend|resume)$/);
      if (req.method === 'POST' && ids) {
        const batch = state.batches.find((item) => item.id === ids[0]);
        const isSuspend = ids[1] === 'suspend';
        const operationId = isSuspend ? 'suspendBatch' : 'resumeBatch';
        if (!batch) return send(res, 404, problem(404, 'Not Found', 'Batch not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, batch.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (!body.reason || String(body.reason).trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'A reason of at least 3 characters is required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        const expectedState = isSuspend ? 'ACTIVE' : 'SUSPENDED';
        const nextState = isSuspend ? 'SUSPENDED' : 'ACTIVE';
        if (batch.state !== expectedState) {
          const response = problem(
            409,
            'Invalid Batch State',
            `Batch must be ${expectedState} before it can ${ids[1]}.`,
            operationId,
            batch.revision,
          );
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const previousState = batch.state;
        batch.state = nextState;
        batch.revision += 1;
        state.line.status = isSuspend ? 'BLOCKED' : 'RUNNING';
        state.batchEvents.push({
          revision: batch.revision,
          action: isSuspend ? 'BATCH_SUSPENDED' : 'BATCH_RESUMED',
          at: FIXED_TIME,
          actor: 'simulated.shift.lead',
          reason: body.reason,
          fromState: previousState,
          toState: nextState,
        });
        return rememberAndSend(state, context, res, 200, envelope(operationId, batch), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/batches\/([^/]+)\/(evidence|balance|genealogy|timeline)$/);
      if (req.method === 'GET' && ids) {
        const batch = state.batches.find((item) => item.id === ids[0]);
        const operationIds = { evidence: 'getBatchEvidence', balance: 'getBatchBalance', genealogy: 'getBatchGenealogy', timeline: 'getBatchTimeline' };
        const operationId = operationIds[ids[1]];
        if (!batch) return send(res, 404, problem(404, 'Not Found', 'Batch not found.', operationId), operationId);
        const data = {
          evidence: {
            start: state.candidate.batchId === batch.id ? state.candidate.evidence : [],
            end: state.endCandidate?.batchId === batch.id ? state.endCandidate.evidence : [],
          },
          balance: { input: 12.4, output: 12.1, difference: 0.3, differencePercent: 2.42, status: 'WITHIN_TOLERANCE', allocations: [] },
          genealogy: { nodes: [{ id: batch.id, type: 'BATCH', label: batch.batchNo }], edges: [] },
          timeline: state.batchEvents,
        }[ids[1]];
        return send(res, 200, envelope(operationId, data), operationId);
      }
      if (req.method === 'GET' && path === '/bpi/v1/topologies') {
        return send(res, 200, envelope('listTopologies', [state.topology]), 'listTopologies');
      }
      ids = match(path, /^\/bpi\/v1\/topologies\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        if (ids[0] !== state.topology.id) return send(res, 404, problem(404, 'Not Found', 'Topology not found.', 'getTopologyVersion'), 'getTopologyVersion');
        return send(res, 200, envelope('getTopologyVersion', state.topology), 'getTopologyVersion');
      }
      if (req.method === 'GET' && path === '/bpi/v1/rules') {
        return send(res, 200, envelope('listRules', [state.rule]), 'listRules');
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        if (ids[0] !== state.rule.id) return send(res, 404, problem(404, 'Not Found', 'Rule not found.', 'getRuleVersion'), 'getRuleVersion');
        return send(res, 200, envelope('getRuleVersion', state.rule), 'getRuleVersion');
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)\/simulate$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'simulateRule';
        if (ids[0] !== state.rule.id) return send(res, 404, problem(404, 'Not Found', 'Rule not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, state.rule.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        const required = ['lineId', 'from', 'to', 'topologyVersion', 'calibrationVersion', 'goldenSetId'];
        const missing = required.filter((key) => body[key] === undefined);
        if (missing.length) {
          const response = problem(422, 'Validation Failed', `Missing fields: ${missing.join(', ')}.`, operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        const simulation = {
          id: '5b0c7926-eeac-42f0-a4ce-5d32a5cc51bb', ruleId: state.rule.id, state: 'PASSED',
          checksum: sha256({ ruleChecksum: state.rule.checksum, input: body }),
          metrics: { matched: 42, missed: 0, falsePositive: 0, meanBoundaryErrorSeconds: 2.4 },
          inputManifest: { ...clone(body), observationCount: 18640, goldenBoundaryCount: 42 },
          emittedBoundaries: ['2026-07-12T07:59:40.000Z'],
          failureReason: null,
        };
        state.simulations.set(simulation.id, simulation);
        state.rule.latestSimulationId = simulation.id;
        state.rule.state = 'SIMULATION_PASSED';
        state.rule.revision += 1;
        const response = envelope(operationId, simulation);
        return rememberAndSend(state, context, res, 202, response, operationId);
      }
      ids = match(path, /^\/bpi\/v1\/rule-simulations\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const simulation = state.simulations.get(ids[0]);
        if (!simulation) return send(res, 404, problem(404, 'Not Found', 'Simulation not found.', 'getRuleSimulation'), 'getRuleSimulation');
        return send(res, 200, envelope('getRuleSimulation', simulation), 'getRuleSimulation');
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)\/publish$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'publishRuleVersion';
        if (ids[0] !== state.rule.id) return send(res, 404, problem(404, 'Not Found', 'Rule not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, state.rule.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        const simulation = state.simulations.get(body.simulationId);
        if (!body.reason || !simulation || simulation.state !== 'PASSED' || body.simulationChecksum !== simulation.checksum) {
          const response = problem(422, 'Simulation Proof Required', 'A passed simulation and matching checksum are required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        state.rule.state = 'PUBLISHED';
        state.rule.publicationStatus = 'PENDING';
        state.rule.publicationRevision = 1;
        state.rule.publicationAttemptCount = 0;
        state.rule.publicationTotalAttemptCount = 0;
        state.rule.publicationManualRetryCount = 0;
        state.rule.publicationPublishedAt = null;
        state.rule.publicationLastRequeuedAt = null;
        state.rule.publicationLastError = null;
        state.rule.revision += 1;
        const response = envelope(operationId, state.rule);
        return rememberAndSend(state, context, res, 200, response, operationId);
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)\/publication\/retry$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'retryRulePublication';
        if (ids[0] !== state.rule.id) return send(res, 404, problem(404, 'Not Found', 'Rule not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, state.rule.publicationRevision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (!body.reason || String(body.reason).trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'A reason of at least 3 characters is required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (state.rule.publicationStatus !== 'FAILED') {
          const response = problem(409, 'Invalid Publication State', 'Only a FAILED publication can be retried.', operationId, state.rule.publicationRevision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        state.rule.publicationStatus = 'PENDING';
        state.rule.publicationRevision += 1;
        state.rule.publicationAttemptCount = 0;
        state.rule.publicationManualRetryCount += 1;
        state.rule.publicationLastRequeuedAt = FIXED_TIME;
        state.rule.publicationLastError = null;
        return rememberAndSend(state, context, res, 200, envelope(operationId, state.rule), operationId);
      }
      if (req.method === 'GET' && path === '/bpi/v1/data-quality/incidents') {
        return send(res, 200, envelope('listDataQualityIncidents', [state.incident]), 'listDataQualityIncidents');
      }
      ids = match(path, /^\/bpi\/v1\/data-quality\/incidents\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        if (ids[0] !== state.incident.id) return send(res, 404, problem(404, 'Not Found', 'Incident not found.', 'getDataQualityIncident'), 'getDataQualityIncident');
        return send(res, 200, envelope('getDataQualityIncident', state.incident), 'getDataQualityIncident');
      }
      if (req.method === 'GET' && path === '/bpi/v1/integrations/health') {
        return send(res, 200, envelope('getIntegrationHealth', state.integrations), 'getIntegrationHealth');
      }
      return send(res, 404, problem(404, 'Not Found', 'Route is not implemented by the Phase 1 simulator.', 'unknownOperation'), 'unknownOperation');
    } catch (error) {
      return send(res, 400, problem(400, 'Bad Request', error.message, 'requestParsing'), 'requestParsing');
    }
  };
}

function createBpiSimulator() {
  const state = createScenario();
  const server = http.createServer(createHandler(state));
  return { server, state };
}

function listen(server, port = 0, host = '127.0.0.1') {
  return new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(port, host, () => resolve(server.address()));
  });
}

if (require.main === module) {
  const { server } = createBpiSimulator();
  const port = Number(process.env.BPI_SIM_PORT || 19090);
  listen(server, port).then((address) => {
    console.log(`BPI Phase 1 simulator listening on http://${address.address}:${address.port}`);
  }).catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });
}

module.exports = { createBpiSimulator, listen };
