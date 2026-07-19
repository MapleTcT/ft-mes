const http = require('node:http');
const { FIXED_TIME, clone, createScenario, sha256 } = require('./scenario');

const JSON_TYPE = 'application/json; charset=utf-8';
const PROBLEM_TYPE = 'application/problem+json; charset=utf-8';

function meta(operationId, pagination = {}) {
  return {
    traceId: `sim-${sha256(operationId).slice(0, 16)}`,
    generatedAt: FIXED_TIME,
    snapshotAt: pagination.snapshotAt || FIXED_TIME,
    nextCursor: pagination.nextCursor || null,
  };
}

function envelope(operationId, data, pagination = {}) {
  return { data: clone(data), meta: meta(operationId, pagination) };
}

function calibrationScopeFingerprint(url) {
  return sha256(['plantId', 'lineId', 'productId', 'deviceId', 'propertyId']
    .map((name) => `${name}=${url.searchParams.get(name) || ''}`)
    .join('|'));
}

function encodeCalibrationCursor(cursor) {
  return Buffer.from(JSON.stringify(cursor), 'utf8').toString('base64url');
}

function decodeCalibrationCursor(value, expectedScopeFingerprint) {
  if (!value || value.length > 2048) throw new Error('invalid cursor');
  const decoded = JSON.parse(Buffer.from(value, 'base64url').toString('utf8'));
  if (decoded.version !== 1 || decoded.scopeFingerprint !== expectedScopeFingerprint
      || !Number.isFinite(Date.parse(decoded.snapshotAt))
      || !Number.isFinite(Date.parse(decoded.submittedAt))
      || Date.parse(decoded.submittedAt) > Date.parse(decoded.snapshotAt)
      || typeof decoded.id !== 'string' || !decoded.id) {
    throw new Error('invalid cursor');
  }
  return decoded;
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

function stableUuid(value) {
  const digest = sha256(value);
  return `${digest.slice(0, 8)}-${digest.slice(8, 12)}-5${digest.slice(13, 16)}-a${digest.slice(17, 20)}-${digest.slice(20, 32)}`;
}

function calibrationEffectiveness(calibration, now = Date.now()) {
  if (calibration.state !== 'APPROVED') return calibration.state;
  if (now < Date.parse(calibration.validFrom)) return 'NOT_YET_EFFECTIVE';
  if (now >= Date.parse(calibration.validUntil)) return 'EXPIRED';
  return 'EFFECTIVE';
}

function refreshPointCatalogReadiness(state) {
  const now = Date.now();
  state.pointCalibrations.forEach((calibration) => {
    calibration.effectivenessStatus = calibrationEffectiveness(calibration, now);
    calibration.effective = calibration.effectivenessStatus === 'EFFECTIVE';
  });
  if (!state.pointCatalog) return null;
  const observedAt = Date.parse(state.pointCatalog.snapshot.observedAt);
  state.pointCatalog.points.forEach((point) => {
    if (!point.sourceCalibrationStatus) {
      point.sourceCalibrationStatus = point.calibrationStatus || (point.calibrationVersion ? 'UNVERIFIED' : 'MISSING');
    }
    const evidence = state.pointCalibrations
      .filter((calibration) => calibration.plantId === point.plantId
        && calibration.lineId === point.lineId
        && calibration.productId === point.productId
        && calibration.deviceId === point.deviceId
        && calibration.propertyId === point.propertyId
        && calibration.calibrationVersion === point.calibrationVersion
        && calibration.effectivenessStatus === 'EFFECTIVE'
        && Date.parse(calibration.validFrom) <= observedAt)
      .sort((left, right) => Date.parse(right.decidedAt || '') - Date.parse(left.decidedAt || ''))[0];
    point.calibrationStatus = evidence ? 'VERIFIED' : point.calibrationVersion ? 'UNVERIFIED' : 'MISSING';
    point.calibrationEvidenceId = evidence?.id || null;
    point.calibrationValidUntil = evidence?.validUntil || null;
    const readinessIssues = [];
    if (!point.registered) readinessIssues.push('DEVICE_NOT_REGISTERED');
    if (point.deviceState !== 'ACTIVE') readinessIssues.push('DEVICE_NOT_ACTIVE');
    if (!point.propertyPresent) readinessIssues.push('PROPERTY_NOT_AVAILABLE');
    if (!point.unit) readinessIssues.push('UNIT_MISSING');
    if (!point.calibrationVersion || point.calibrationStatus !== 'VERIFIED') readinessIssues.push('CALIBRATION_NOT_VERIFIED');
    if (!point.sourceSequenceEnabled) readinessIssues.push('SOURCE_SEQUENCE_DISABLED');
    point.ready = readinessIssues.length === 0;
    point.readinessIssues = readinessIssues;
  });
  state.pointCatalog.snapshot.readyPointCount = state.pointCatalog.points.filter((point) => point.ready).length;
  return state.pointCatalog;
}

function compareVersions(objectType, base, target, baseContent, targetContent) {
  const changes = [];
  let changeCount = 0;
  const missing = Symbol('missing');
  const value = (item) => item === missing ? null : clone(item);
  const walk = (path, before, after) => {
    if (before !== missing && after !== missing
        && JSON.stringify(before) === JSON.stringify(after)) return;
    const beforeObject = before !== null && before !== missing && !Array.isArray(before) && typeof before === 'object';
    const afterObject = after !== null && after !== missing && !Array.isArray(after) && typeof after === 'object';
    if (beforeObject && afterObject) {
      const names = [...new Set([...Object.keys(before), ...Object.keys(after)])].sort();
      names.forEach((name) => walk(
        `${path}/${name.replaceAll('~', '~0').replaceAll('/', '~1')}`,
        Object.prototype.hasOwnProperty.call(before, name) ? before[name] : missing,
        Object.prototype.hasOwnProperty.call(after, name) ? after[name] : missing,
      ));
      return;
    }
    if (Array.isArray(before) && Array.isArray(after)) {
      const size = Math.max(before.length, after.length);
      for (let index = 0; index < size; index += 1) {
        walk(`${path}/${index}`, index < before.length ? before[index] : missing,
          index < after.length ? after[index] : missing);
      }
      return;
    }
    changeCount += 1;
    if (changes.length >= 500) return;
    changes.push({
      path: path || '/',
      changeType: before === missing ? 'ADDED' : after === missing ? 'REMOVED' : 'CHANGED',
      beforeValue: value(before),
      afterValue: value(after),
    });
  };
  walk('', baseContent, targetContent);
  const reference = (item) => ({
    id: item.id, code: item.code, version: item.version, state: item.state, checksum: item.checksum,
  });
  return {
    objectType,
    base: reference(base),
    target: reference(target),
    identical: changeCount === 0,
    changeCount,
    truncated: changeCount > changes.length,
    changes,
  };
}

function topologyValidation(definition, pointCatalog) {
  const errors = [];
  const warnings = [];
  const bindings = Array.isArray(definition?.bindings) ? definition.bindings : [];
  if (!bindings.length) {
    errors.push({ code: 'BINDINGS_REQUIRED', path: '/bindings', severity: 'ERROR', message: 'At least one JetLinks point binding is required.' });
  }
  bindings.forEach((binding, index) => {
    ['signal', 'productId', 'deviceId', 'propertyId', 'calibrationVersion'].forEach((field) => {
      if (!binding[field]) errors.push({ code: 'FIELD_REQUIRED', path: `/bindings/${index}/${field}`, severity: 'ERROR', message: `${field} is required.` });
    });
    if (!binding.expectedUnit && !binding.unit) {
      errors.push({ code: 'BINDING_UNIT_REQUIRED', path: `/bindings/${index}/expectedUnit`, severity: 'ERROR', message: 'A binding must declare expectedUnit or unit.' });
    }
    if (!pointCatalog) return;
    const point = pointCatalog?.points.find((item) => item.productId === binding.productId
      && item.deviceId === binding.deviceId && item.propertyId === binding.propertyId);
    if (!point) {
      errors.push({ code: 'POINT_CATALOG_BINDING_NOT_FOUND', path: `/bindings/${index}`, severity: 'ERROR', message: 'The binding does not exist in the current point catalog snapshot.' });
      return;
    }
    if (!point.registered) errors.push({ code: 'POINT_DEVICE_NOT_REGISTERED', path: `/bindings/${index}/deviceId`, severity: 'ERROR', message: 'The bound device is not registered.' });
    if (point.deviceState !== 'ACTIVE') errors.push({ code: 'POINT_DEVICE_NOT_ACTIVE', path: `/bindings/${index}/deviceId`, severity: 'ERROR', message: `The bound device state is ${point.deviceState}.` });
    if (!point.propertyPresent) errors.push({ code: 'POINT_PROPERTY_NOT_AVAILABLE', path: `/bindings/${index}/propertyId`, severity: 'ERROR', message: 'The bound property is absent from product metadata.' });
    const expectedUnit = binding.expectedUnit || binding.unit;
    if (!point.unit) errors.push({ code: 'POINT_UNIT_MISSING', path: `/bindings/${index}/expectedUnit`, severity: 'ERROR', message: 'The catalog point has no source unit.' });
    else if (expectedUnit && point.unit.toLowerCase() !== String(expectedUnit).toLowerCase()) errors.push({ code: 'POINT_UNIT_MISMATCH', path: `/bindings/${index}/expectedUnit`, severity: 'ERROR', message: 'Expected and source units do not match.' });
    if (point.calibrationStatus !== 'VERIFIED' || point.calibrationVersion !== binding.calibrationVersion) errors.push({ code: 'POINT_CALIBRATION_NOT_VERIFIED', path: `/bindings/${index}/calibrationVersion`, severity: 'ERROR', message: 'The requested calibration version is not verified.' });
    if (!point.sourceSequenceEnabled) errors.push({ code: 'POINT_SOURCE_SEQUENCE_DISABLED', path: `/bindings/${index}`, severity: 'ERROR', message: 'A device or gateway source epoch and sequence are required for replay-safe topology binding.' });
  });
  if (!pointCatalog) errors.push({ code: 'POINT_CATALOG_SNAPSHOT_MISSING', path: '/bindings', severity: 'ERROR', message: 'No point catalog snapshot exists for this scope.' });
  return { errors, warnings };
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
        if (!['PUBLISHED', 'RETIRED'].includes(state.rule.state)
            || !['PENDING', 'DISPATCHING'].includes(state.rule.publicationStatus)) {
          return send(res, 409, { status: 'NOT_DISPATCHING' }, 'simulationFailRulePublication');
        }
        state.rule.publicationStatus = 'FAILED';
        state.rule.publicationRevision = 11;
        state.rule.publicationAttemptCount = 5;
        state.rule.publicationTotalAttemptCount = 5;
        state.rule.publicationLastError = 'Simulated Kafka broker outage';
        return send(res, 200, { status: 'FAILED' }, 'simulationFailRulePublication');
      }
      if (req.method === 'POST' && path === '/__simulation/complete-rule-publication') {
        if (state.rule.publicationStatus === 'PUBLISHED') {
          return send(res, 200, envelope('simulationCompleteRulePublication', state.rule), 'simulationCompleteRulePublication');
        }
        if (!['PUBLISHED', 'RETIRED'].includes(state.rule.state) || state.rule.publicationStatus === 'FAILED') {
          return send(res, 409, { status: 'NOT_DISPATCHABLE' }, 'simulationCompleteRulePublication');
        }
        state.rule.publicationStatus = 'PUBLISHED';
        state.rule.publicationRevision += 1;
        state.rule.publicationAttemptCount = 1;
        state.rule.publicationTotalAttemptCount += 1;
        state.rule.publicationPublishedAt = '2026-07-12T08:00:01.000Z';
        state.rule.publicationLastError = null;
        return send(res, 200, envelope('simulationCompleteRulePublication', state.rule), 'simulationCompleteRulePublication');
      }
      if (req.method === 'POST' && path === '/__simulation/rule-application') {
        if (state.rule.publicationStatus !== 'PUBLISHED') {
          return send(res, 409, { status: 'PUBLICATION_NOT_CONFIRMED' }, 'simulationRuleApplication');
        }
        const body = await readJson(req);
        if (!['APPLIED', 'REJECTED'].includes(body.status)) {
          return send(res, 422, { status: 'INVALID_APPLICATION_STATUS' }, 'simulationRuleApplication');
        }
        if (state.rule.applicationStatus === 'APPLIED'
            || (state.rule.applicationStatus === 'REJECTED' && body.status === 'REJECTED')) {
          return send(res, 200, envelope('simulationRuleApplication', state.rule), 'simulationRuleApplication');
        }
        state.rule.applicationStatus = body.status;
        state.rule.applicationDeploymentId = body.deploymentId || 'flink-simulator-a';
        state.rule.applicationObservedAt = body.observedAt || '2026-07-12T08:00:02.000Z';
        state.rule.applicationReceivedAt = '2026-07-12T08:00:03.000Z';
        state.rule.applicationErrorCode = body.status === 'REJECTED'
          ? body.errorCode || 'RULE_APPLICATION_REJECTED'
          : null;
        state.rule.applicationErrorDetail = body.status === 'REJECTED'
          ? body.errorDetail || 'Flink rejected the simulated rule update.'
          : null;
        state.rule.publicationRevision += 1;
        return send(res, 200, envelope('simulationRuleApplication', state.rule), 'simulationRuleApplication');
      }
      if (req.method === 'POST' && path === '/__simulation/rule-runtime-readiness') {
        const operationId = 'simulationRuleRuntimeReadiness';
        if (state.rule.publicationStatus !== 'PUBLISHED') {
          return send(res, 409, { status: 'PUBLICATION_NOT_CONFIRMED' }, operationId);
        }
        const body = await readJson(req);
        if (!['READY', 'DEGRADED', 'INACTIVE'].includes(body.status)) {
          return send(res, 422, { status: 'INVALID_RUNTIME_READINESS_STATUS' }, operationId);
        }
        if (body.status !== 'READY' && (!body.reasonCode || !body.detail)) {
          return send(res, 422, { status: 'RUNTIME_READINESS_REASON_REQUIRED' }, operationId);
        }
        const observedAt = body.observedAt || '2026-07-12T08:00:04.000Z';
        const observedAtMs = Date.parse(observedAt);
        if (!Number.isFinite(observedAtMs)) {
          return send(res, 422, { status: 'INVALID_RUNTIME_READINESS_TIME' }, operationId);
        }
        const receipt = {
          eventId: body.eventId || `sha256:${sha256({
            ruleId: state.rule.id,
            status: body.status,
            reasonCode: body.reasonCode || '',
            observedAt,
          })}`,
          status: body.status,
          deploymentId: body.deploymentId || 'flink-simulator-b',
          observedAt,
          receivedAt: body.receivedAt || '2026-07-12T08:00:05.000Z',
          reasonCode: body.status === 'READY' ? null : body.reasonCode,
          detail: body.status === 'READY' ? null : body.detail,
          pointCatalogEventId: body.pointCatalogEventId || null,
          pointCatalogSourceRevision: body.pointCatalogSourceRevision || null,
        };
        const payloadChecksum = sha256(receipt);
        const existing = state.runtimeReadinessReceipts.get(receipt.eventId);
        if (existing) {
          if (existing.payloadChecksum !== payloadChecksum) {
            return send(res, 409, { status: 'RUNTIME_READINESS_REPLAY_CONFLICT' }, operationId);
          }
          return send(res, 200, envelope(operationId, state.rule), operationId);
        }
        state.runtimeReadinessReceipts.set(receipt.eventId, { payloadChecksum, receipt: clone(receipt) });
        const currentObservedAtMs = Date.parse(state.rule.runtimeReadinessObservedAt || '');
        if (Number.isFinite(currentObservedAtMs) && observedAtMs < currentObservedAtMs) {
          return send(res, 200, envelope(operationId, state.rule), operationId);
        }
        if (Number.isFinite(currentObservedAtMs) && observedAtMs === currentObservedAtMs) {
          return send(res, 409, { status: 'RUNTIME_READINESS_TIME_CONFLICT' }, operationId);
        }
        state.rule.runtimeReadinessStatus = receipt.status;
        state.rule.runtimeReadinessDeploymentId = receipt.deploymentId;
        state.rule.runtimeReadinessObservedAt = receipt.observedAt;
        state.rule.runtimeReadinessReceivedAt = receipt.receivedAt;
        state.rule.runtimeReadinessReasonCode = receipt.reasonCode;
        state.rule.runtimeReadinessDetail = receipt.detail;
        state.rule.runtimePointCatalogEventId = receipt.pointCatalogEventId;
        state.rule.runtimePointCatalogSourceRevision = receipt.pointCatalogSourceRevision;
        state.rule.publicationRevision += 1;
        return send(res, 200, envelope(operationId, state.rule), operationId);
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
        return send(res, 200, envelope('listTopologies', state.topologies), 'listTopologies');
      }
      if (req.method === 'GET' && path === '/bpi/v1/point-catalog/current') {
        refreshPointCatalogReadiness(state);
        const matchesScope = state.pointCatalog
          && state.pointCatalog.snapshot.plantId === url.searchParams.get('plantId')
          && state.pointCatalog.snapshot.lineId === url.searchParams.get('lineId');
        return send(res, 200, envelope('getCurrentPointCatalog', matchesScope ? state.pointCatalog : null), 'getCurrentPointCatalog');
      }
      if (req.method === 'GET' && path === '/bpi/v1/point-catalog/snapshots') {
        refreshPointCatalogReadiness(state);
        return send(res, 200, envelope('listPointCatalogSnapshots', state.pointCatalog ? [state.pointCatalog.snapshot] : []), 'listPointCatalogSnapshots');
      }
      if (req.method === 'POST' && path === '/bpi/v1/point-catalog/snapshots') {
        const operationId = 'importPointCatalogSnapshot';
        const context = commandContext(req, res, operationId, 0, state, path);
        if (!context) return;
        const body = await readJson(req);
        const missing = ['source', 'sourceInstance', 'sourceRevision', 'plantId', 'lineId', 'observedAt', 'points', 'reason']
          .filter((key) => body[key] === undefined || body[key] === '');
        if (missing.length || !Array.isArray(body.points)) {
          return send(res, 422, problem(422, 'Validation Failed', `Missing or invalid fields: ${missing.join(', ')}.`, operationId), operationId);
        }
        const snapshotId = stableUuid({ type: 'point-catalog', source: body.source, sourceRevision: body.sourceRevision });
        const points = body.points.map((point) => ({
            id: stableUuid({ type: 'point', snapshotId, productId: point.productId, deviceId: point.deviceId, propertyId: point.propertyId }),
            snapshotId, plantId: body.plantId, lineId: body.lineId, ...clone(point),
            sourceCalibrationStatus: point.calibrationStatus || (point.calibrationVersion ? 'UNVERIFIED' : 'MISSING'),
            calibrationStatus: point.calibrationVersion ? 'UNVERIFIED' : 'MISSING',
            calibrationEvidenceId: null, calibrationValidUntil: null, ready: false, readinessIssues: [],
          }));
        const { reason: ignoredReason, ...catalogPayload } = body;
        const snapshot = {
          id: snapshotId, source: body.source, sourceInstance: body.sourceInstance,
          sourceRevision: body.sourceRevision, plantId: body.plantId, lineId: body.lineId,
          checksum: sha256(catalogPayload), observedAt: body.observedAt,
          pointCount: points.length, readyPointCount: points.filter((point) => point.ready).length,
          importedBy: 'simulated.bpi.admin', importedAt: FIXED_TIME,
        };
        state.pointCatalog = { snapshot, points };
        refreshPointCatalogReadiness(state);
        return rememberAndSend(state, context, res, 200, envelope(operationId, state.pointCatalog), operationId);
      }
      if (req.method === 'GET' && path === '/bpi/v1/point-calibrations') {
        refreshPointCatalogReadiness(state);
        const operationId = 'listPointCalibrations';
        const rawLimit = url.searchParams.get('limit');
        const limit = rawLimit === null ? 50 : Number(rawLimit);
        if (!Number.isInteger(limit) || limit < 1 || limit > 200) {
          return send(res, 422, problem(422, 'Validation Failed', 'limit must be between 1 and 200.', operationId), operationId);
        }
        const scopeFingerprint = calibrationScopeFingerprint(url);
        let cursor = null;
        try {
          const encodedCursor = url.searchParams.get('cursor');
          cursor = encodedCursor ? decodeCalibrationCursor(encodedCursor, scopeFingerprint) : null;
        } catch (error) {
          return send(res, 422, problem(
            422,
            'Validation Failed',
            'Point calibration cursor is invalid or does not match the requested scope.',
            operationId,
          ), operationId);
        }
        const snapshotAt = cursor?.snapshotAt || new Date().toISOString();
        const snapshotTime = Date.parse(snapshotAt);
        const filters = ['plantId', 'lineId', 'productId', 'deviceId', 'propertyId'];
        const calibrations = state.pointCalibrations
          .filter((calibration) => filters.every((name) => {
            const expected = url.searchParams.get(name);
            return !expected || calibration[name] === expected;
          }))
          .filter((calibration) => Date.parse(calibration.submittedAt) <= snapshotTime)
          .filter((calibration) => !cursor
            || Date.parse(calibration.submittedAt) < Date.parse(cursor.submittedAt)
            || (Date.parse(calibration.submittedAt) === Date.parse(cursor.submittedAt)
              && calibration.id.localeCompare(cursor.id) < 0))
          .sort((left, right) => Date.parse(right.submittedAt) - Date.parse(left.submittedAt)
            || right.id.localeCompare(left.id));
        const page = calibrations.slice(0, limit);
        const last = page.at(-1);
        const nextCursor = calibrations.length > limit && last
          ? encodeCalibrationCursor({
            version: 1,
            snapshotAt,
            submittedAt: last.submittedAt,
            id: last.id,
            scopeFingerprint,
          })
          : null;
        return send(res, 200, envelope(operationId, page, { snapshotAt, nextCursor }), operationId);
      }
      if (req.method === 'POST' && path === '/bpi/v1/point-calibrations') {
        const operationId = 'submitPointCalibration';
        const context = commandContext(req, res, operationId, 0, state, path);
        if (!context) return;
        const body = await readJson(req);
        const required = ['plantId', 'lineId', 'productId', 'deviceId', 'propertyId', 'calibrationVersion',
          'certificateReference', 'certificateChecksum', 'validFrom', 'validUntil', 'reason'];
        const missing = required.filter((key) => body[key] === undefined || String(body[key]).trim() === '');
        const validFrom = Date.parse(body.validFrom);
        const validUntil = Date.parse(body.validUntil);
        if (missing.length || String(body.reason || '').trim().length < 3
            || !/^[a-f0-9]{64}$/.test(String(body.certificateChecksum || ''))
            || !Number.isFinite(validFrom) || !Number.isFinite(validUntil) || validUntil <= validFrom) {
          return send(res, 422, problem(422, 'Validation Failed', 'Point calibration payload is invalid.', operationId), operationId);
        }
        if (validUntil <= Date.now()) {
          return send(res, 422, problem(422, 'Validation Failed', 'Expired calibration evidence cannot be submitted.', operationId), operationId);
        }
        const duplicate = state.pointCalibrations.some((calibration) => calibration.plantId === body.plantId
          && calibration.lineId === body.lineId && calibration.productId === body.productId
          && calibration.deviceId === body.deviceId && calibration.propertyId === body.propertyId
          && calibration.calibrationVersion === body.calibrationVersion);
        if (duplicate) {
          return send(res, 409, problem(409, 'Point Calibration Conflict', 'The point calibration version already exists.', operationId), operationId);
        }
        const calibration = {
          id: stableUuid({ type: 'point-calibration', body }), plantId: body.plantId, lineId: body.lineId,
          productId: body.productId, deviceId: body.deviceId, propertyId: body.propertyId,
          calibrationVersion: body.calibrationVersion, certificateReference: body.certificateReference,
          certificateChecksum: body.certificateChecksum, validFrom: new Date(validFrom).toISOString(),
          validUntil: new Date(validUntil).toISOString(), state: 'PENDING', revision: 1,
          submittedBy: 'simulated.metrology.engineer', submittedAt: new Date().toISOString(),
          submitReason: body.reason, decidedBy: null, decidedAt: null, decisionReason: null,
          revokedBy: null, revokedAt: null, revokeReason: null, effective: false, effectivenessStatus: 'PENDING',
        };
        state.pointCalibrations.unshift(calibration);
        return rememberAndSend(state, context, res, 200, envelope(operationId, calibration), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/point-calibrations\/([^/]+)\/(approve|reject|revoke)$/);
      if (req.method === 'POST' && ids) {
        const action = ids[1];
        const operationId = `${action}PointCalibration`;
        const calibration = state.pointCalibrations.find((item) => item.id === ids[0]);
        if (!calibration) return send(res, 404, problem(404, 'Not Found', 'Point calibration not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, calibration.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (!body.reason || String(body.reason).trim().length < 3) {
          return send(res, 422, problem(422, 'Validation Failed', 'A reason of at least 3 characters is required.', operationId), operationId);
        }
        const expectedState = action === 'revoke' ? 'APPROVED' : 'PENDING';
        if (calibration.state !== expectedState) {
          return send(res, 409, problem(409, 'Invalid Point Calibration State', `Point calibration must be ${expectedState}.`, operationId, calibration.revision), operationId);
        }
        if (action === 'approve' && Date.parse(calibration.validUntil) <= Date.now()) {
          return send(res, 422, problem(422, 'Validation Failed', 'Expired calibration evidence cannot be approved.', operationId), operationId);
        }
        calibration.revision += 1;
        if (action === 'revoke') {
          calibration.state = 'REVOKED';
          calibration.revokedBy = 'simulated.bpi.revoker';
          calibration.revokedAt = new Date().toISOString();
          calibration.revokeReason = body.reason;
        } else {
          calibration.state = action === 'approve' ? 'APPROVED' : 'REJECTED';
          calibration.decidedBy = 'simulated.bpi.admin';
          calibration.decidedAt = new Date().toISOString();
          calibration.decisionReason = body.reason;
        }
        refreshPointCatalogReadiness(state);
        return rememberAndSend(state, context, res, 200, envelope(operationId, calibration), operationId);
      }
      if (req.method === 'POST' && path === '/bpi/v1/topologies/drafts') {
        const operationId = 'createTopologyDraft';
        const body = await readJson(req);
        const base = body.baseVersionId ? state.topologies.find((item) => item.id === body.baseVersionId) : null;
        const context = commandContext(req, res, operationId, base?.revision || 0, state, path);
        if (!context) return;
        const required = ['code', 'version', 'plantId', 'lineId', 'definition', 'reason'];
        const missing = required.filter((key) => body[key] === undefined || body[key] === '');
        if (missing.length) return send(res, 422, problem(422, 'Validation Failed', `Missing fields: ${missing.join(', ')}.`, operationId), operationId);
        if (state.topologies.some((item) => item.code === body.code && item.version === body.version)) {
          return send(res, 409, problem(409, 'Version Conflict', 'Topology code and version already exist.', operationId), operationId);
        }
        const topology = {
          id: stableUuid({ type: 'topology', code: body.code, version: body.version }),
          code: body.code, version: body.version, state: 'DRAFT', revision: 1,
          plantId: body.plantId, lineId: body.lineId, checksum: sha256(body.definition),
          definition: clone(body.definition), validationStatus: 'NOT_VALIDATED',
          validationErrors: [], validationWarnings: [], validatedBy: null, validatedAt: null,
          validatedPointCatalogSnapshotId: null, validatedPointCatalogChecksum: null,
          publishedBy: null, publishedAt: null,
        };
        state.topologies.unshift(topology);
        const response = envelope(operationId, topology);
        return rememberAndSend(state, context, res, 200, response, operationId);
      }
      ids = match(path, /^\/bpi\/v1\/topologies\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const topology = state.topologies.find((item) => item.id === ids[0]);
        if (!topology) return send(res, 404, problem(404, 'Not Found', 'Topology not found.', 'getTopologyVersion'), 'getTopologyVersion');
        return send(res, 200, envelope('getTopologyVersion', topology), 'getTopologyVersion');
      }
      ids = match(path, /^\/bpi\/v1\/topologies\/([^/]+)\/compare$/);
      if (req.method === 'GET' && ids) {
        const target = state.topologies.find((item) => item.id === ids[0]);
        const base = state.topologies.find((item) => item.id === url.searchParams.get('against'));
        if (!target || !base) return send(res, 404, problem(404, 'Not Found', 'Topology version not found.', 'compareTopologyVersions'), 'compareTopologyVersions');
        if (target.code !== base.code || target.plantId !== base.plantId || target.lineId !== base.lineId) {
          return send(res, 422, problem(422, 'Validation Failed', 'Topology versions must share code and scope before comparison.', 'compareTopologyVersions'), 'compareTopologyVersions');
        }
        return send(res, 200, envelope('compareTopologyVersions', compareVersions(
          'TOPOLOGY_VERSION', base, target, base.definition, target.definition,
        )), 'compareTopologyVersions');
      }
      ids = match(path, /^\/bpi\/v1\/topologies\/([^/]+)\/validate$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'validateTopologyDraft';
        const topology = state.topologies.find((item) => item.id === ids[0]);
        if (!topology) return send(res, 404, problem(404, 'Not Found', 'Topology not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, topology.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (!body.reason || topology.state !== 'DRAFT') return send(res, 409, problem(409, 'Invalid Topology State', 'Only a draft topology can be validated.', operationId, topology.revision), operationId);
        const result = topologyValidation(topology.definition, state.pointCatalog);
        topology.validationErrors = result.errors;
        topology.validationWarnings = result.warnings;
        topology.validationStatus = result.errors.length ? 'FAILED' : 'PASSED';
        topology.validatedBy = 'simulated.process.engineer';
        topology.validatedAt = FIXED_TIME;
        topology.validatedPointCatalogSnapshotId = state.pointCatalog?.snapshot.id || null;
        topology.validatedPointCatalogChecksum = state.pointCatalog?.snapshot.checksum || null;
        topology.revision += 1;
        return rememberAndSend(state, context, res, 200, envelope(operationId, topology), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/topologies\/([^/]+)\/publish$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'publishTopologyVersion';
        const topology = state.topologies.find((item) => item.id === ids[0]);
        if (!topology) return send(res, 404, problem(404, 'Not Found', 'Topology not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, topology.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        refreshPointCatalogReadiness(state);
        const currentValidation = topologyValidation(topology.definition, state.pointCatalog);
        if (currentValidation.errors.length) {
          const codes = [...new Set(currentValidation.errors.map((issue) => issue.code))].sort().join(', ');
          return send(res, 422, problem(422, 'Validation Failed', `Topology publication requires current READY point catalog bindings: ${codes}.`, operationId, topology.revision), operationId);
        }
        if (!body.reason || topology.state !== 'DRAFT' || topology.validationStatus !== 'PASSED'
            || !topology.validatedPointCatalogSnapshotId || !topology.validatedPointCatalogChecksum
            || topology.validatedPointCatalogSnapshotId !== state.pointCatalog?.snapshot.id
            || topology.validatedPointCatalogChecksum !== state.pointCatalog?.snapshot.checksum) {
          return send(res, 409, problem(409, 'Invalid Topology State', 'Topology must pass validation against the current point catalog snapshot.', operationId, topology.revision), operationId);
        }
        topology.state = 'PUBLISHED';
        topology.publishedBy = 'simulated.bpi.admin';
        topology.publishedAt = FIXED_TIME;
        topology.revision += 1;
        return rememberAndSend(state, context, res, 200, envelope(operationId, topology), operationId);
      }
      if (req.method === 'GET' && path === '/bpi/v1/rules') {
        return send(res, 200, envelope('listRules', state.rules), 'listRules');
      }
      if (req.method === 'POST' && path === '/bpi/v1/rules/drafts') {
        const operationId = 'createRuleDraft';
        const body = await readJson(req);
        const base = body.baseVersionId ? state.rules.find((item) => item.id === body.baseVersionId) : null;
        const context = commandContext(req, res, operationId, base?.revision || 0, state, path);
        if (!context) return;
        const topology = state.topologies.find((item) => `${item.code}@${item.version}` === body.topologyVersion && item.state === 'PUBLISHED');
        const missing = ['code', 'version', 'lineId', 'topologyVersion', 'ast', 'reason'].filter((key) => body[key] === undefined || body[key] === '');
        if (missing.length || !topology) return send(res, 422, problem(422, 'Validation Failed', missing.length ? `Missing fields: ${missing.join(', ')}.` : 'Published topology not found.', operationId), operationId);
        if (base && !['PUBLISHED', 'RETIRED'].includes(base.state)) {
          return send(res, 409, problem(409, 'Invalid Base Version', 'Only a published or retired rule can be copied.', operationId, base.revision), operationId);
        }
        const boundSignals = new Set((topology.definition.bindings || []).map((binding) => binding.signal));
        const unbound = (body.ast.conditions || []).map((condition) => condition.signal).filter((signal) => !boundSignals.has(signal));
        if (unbound.length) return send(res, 422, problem(422, 'Validation Failed', `Rule conditions reference signals not bound by the topology: ${unbound.join(', ')}.`, operationId), operationId);
        if (state.rules.some((item) => item.code === body.code && item.version === body.version)) {
          return send(res, 409, problem(409, 'Version Conflict', 'Rule code and version already exist.', operationId), operationId);
        }
        const rule = {
          id: stableUuid({ type: 'rule', code: body.code, version: body.version }), code: body.code,
          version: body.version, plantId: topology.plantId, lineId: body.lineId,
          topologyVersion: body.topologyVersion, ast: clone(body.ast), state: 'DRAFT', revision: 1,
          checksum: sha256(body.ast), latestSimulationId: null,
          approvalId: null, approvalStatus: 'NOT_REQUESTED', approvalRevision: 0,
          approvalSubmittedBy: null, approvalSubmittedAt: null,
          approvalDecidedBy: null, approvalDecidedAt: null,
          lifecycleAction: 'NOT_PUBLISHED', lifecycleSequence: 0, lifecycleActive: false,
          publicationStatus: 'NOT_PUBLISHED',
          publicationRevision: 0, publicationAttemptCount: 0, publicationTotalAttemptCount: 0,
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
        state.rules.unshift(rule);
        return rememberAndSend(state, context, res, 200, envelope(operationId, rule), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const rule = state.rules.find((item) => item.id === ids[0]);
        if (!rule) return send(res, 404, problem(404, 'Not Found', 'Rule not found.', 'getRuleVersion'), 'getRuleVersion');
        return send(res, 200, envelope('getRuleVersion', rule), 'getRuleVersion');
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)\/compare$/);
      if (req.method === 'GET' && ids) {
        const target = state.rules.find((item) => item.id === ids[0]);
        const base = state.rules.find((item) => item.id === url.searchParams.get('against'));
        if (!target || !base) return send(res, 404, problem(404, 'Not Found', 'Rule version not found.', 'compareRuleVersions'), 'compareRuleVersions');
        if (target.code !== base.code || target.plantId !== base.plantId || target.lineId !== base.lineId) {
          return send(res, 422, problem(422, 'Validation Failed', 'Rule versions must share code and scope before comparison.', 'compareRuleVersions'), 'compareRuleVersions');
        }
        return send(res, 200, envelope('compareRuleVersions', compareVersions(
          'RULE_VERSION', base, target,
          { topologyVersion: base.topologyVersion, ast: base.ast },
          { topologyVersion: target.topologyVersion, ast: target.ast },
        )), 'compareRuleVersions');
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)\/simulate$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'simulateRule';
        const rule = state.rules.find((item) => item.id === ids[0]);
        if (!rule) return send(res, 404, problem(404, 'Not Found', 'Rule not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, rule.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        const required = ['lineId', 'from', 'to', 'topologyVersion', 'calibrationVersion', 'goldenSetId'];
        const missing = required.filter((key) => body[key] === undefined);
        if (missing.length) {
          const response = problem(422, 'Validation Failed', `Missing fields: ${missing.join(', ')}.`, operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (!['DRAFT', 'SIMULATION_PASSED'].includes(rule.state)) {
          const response = problem(409, 'Invalid Rule State', `Rule cannot be simulated from state ${rule.state}.`, operationId, rule.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const simulation = {
          id: stableUuid({ type: 'simulation', ruleId: rule.id }), ruleId: rule.id, state: 'PASSED',
          checksum: sha256({ ruleChecksum: rule.checksum, input: body }),
          metrics: { matched: 42, missed: 0, falsePositive: 0, meanBoundaryErrorSeconds: 2.4 },
          inputManifest: { ...clone(body), observationCount: 18640, goldenBoundaryCount: 42 },
          emittedBoundaries: ['2026-07-12T07:59:40.000Z'],
          failureReason: null,
        };
        state.simulations.set(simulation.id, simulation);
        rule.latestSimulationId = simulation.id;
        rule.state = 'SIMULATION_PASSED';
        rule.revision += 1;
        const response = envelope(operationId, simulation);
        return rememberAndSend(state, context, res, 202, response, operationId);
      }
      ids = match(path, /^\/bpi\/v1\/rule-simulations\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const simulation = state.simulations.get(ids[0]);
        if (!simulation) return send(res, 404, problem(404, 'Not Found', 'Simulation not found.', 'getRuleSimulation'), 'getRuleSimulation');
        return send(res, 200, envelope('getRuleSimulation', simulation), 'getRuleSimulation');
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)\/submit-approval$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'submitRuleApproval';
        const rule = state.rules.find((item) => item.id === ids[0]);
        if (!rule) return send(res, 404, problem(404, 'Not Found', 'Rule not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, rule.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        const simulation = state.simulations.get(body.simulationId);
        if (rule.state !== 'SIMULATION_PASSED' || !body.reason || !simulation
            || simulation.state !== 'PASSED' || body.simulationChecksum !== simulation.checksum) {
          const response = problem(422, 'Simulation Proof Required', 'A passed simulation and matching checksum are required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        rule.state = 'PENDING_APPROVAL';
        rule.approvalId = stableUuid({ type: 'approval', ruleId: rule.id, revision: rule.revision });
        rule.approvalStatus = 'PENDING';
        rule.approvalRevision = 1;
        rule.approvalSubmittedBy = 'simulated.bpi.engineer';
        rule.approvalSubmittedAt = FIXED_TIME;
        rule.approvalDecidedBy = null;
        rule.approvalDecidedAt = null;
        rule.revision += 1;
        return rememberAndSend(state, context, res, 200, envelope(operationId, rule), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)\/reject-approval$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'rejectRuleApproval';
        const rule = state.rules.find((item) => item.id === ids[0]);
        if (!rule) return send(res, 404, problem(404, 'Not Found', 'Rule not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, rule.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (rule.state !== 'PENDING_APPROVAL' || !body.reason || String(body.reason).trim().length < 3) {
          const response = problem(409, 'Invalid Approval State', 'Rule must be pending approval.', operationId, rule.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        rule.state = 'DRAFT';
        rule.approvalStatus = 'REJECTED';
        rule.approvalRevision += 1;
        rule.approvalDecidedBy = 'simulated.bpi.admin';
        rule.approvalDecidedAt = FIXED_TIME;
        rule.revision += 1;
        return rememberAndSend(state, context, res, 200, envelope(operationId, rule), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)\/publish$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'publishRuleVersion';
        const rule = state.rules.find((item) => item.id === ids[0]);
        if (!rule) return send(res, 404, problem(404, 'Not Found', 'Rule not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, rule.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        const simulation = state.simulations.get(body.simulationId);
        if (rule.state !== 'PENDING_APPROVAL' || rule.approvalStatus !== 'PENDING'
            || !body.reason || !simulation || simulation.state !== 'PASSED'
            || body.simulationChecksum !== simulation.checksum) {
          const response = problem(422, 'Simulation Proof Required', 'A passed simulation and matching checksum are required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        rule.state = 'PUBLISHED';
        rule.approvalStatus = 'APPROVED';
        rule.approvalRevision += 1;
        rule.approvalDecidedBy = 'simulated.bpi.admin';
        rule.approvalDecidedAt = FIXED_TIME;
        rule.lifecycleAction = 'ACTIVATE';
        rule.lifecycleSequence += 1;
        rule.lifecycleActive = true;
        rule.publicationStatus = 'PENDING';
        rule.publicationRevision = 1;
        rule.publicationAttemptCount = 0;
        rule.publicationTotalAttemptCount = 0;
        rule.publicationManualRetryCount = 0;
        rule.publicationPublishedAt = null;
        rule.publicationLastRequeuedAt = null;
        rule.publicationLastError = null;
        rule.applicationStatus = 'WAITING';
        rule.applicationDeploymentId = null;
        rule.applicationObservedAt = null;
        rule.applicationReceivedAt = null;
        rule.applicationErrorCode = null;
        rule.applicationErrorDetail = null;
        rule.runtimeReadinessStatus = 'WAITING';
        rule.runtimeReadinessDeploymentId = null;
        rule.runtimeReadinessObservedAt = null;
        rule.runtimeReadinessReceivedAt = null;
        rule.runtimeReadinessReasonCode = null;
        rule.runtimeReadinessDetail = null;
        rule.runtimePointCatalogEventId = null;
        rule.runtimePointCatalogSourceRevision = null;
        rule.revision += 1;
        const response = envelope(operationId, rule);
        return rememberAndSend(state, context, res, 200, response, operationId);
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)\/retire$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'retireRuleVersion';
        const rule = state.rules.find((item) => item.id === ids[0]);
        if (!rule) return send(res, 404, problem(404, 'Not Found', 'Rule not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, rule.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        const readyToRetire = rule.state === 'PUBLISHED'
          && rule.lifecycleAction === 'ACTIVATE'
          && rule.lifecycleActive
          && rule.publicationStatus === 'PUBLISHED'
          && rule.applicationStatus === 'APPLIED'
          && ['READY', 'DEGRADED'].includes(rule.runtimeReadinessStatus);
        if (!body.reason || String(body.reason).trim().length < 3 || !readyToRetire) {
          const response = problem(409, 'Invalid Retirement State', 'Rule retirement requires Kafka PUBLISHED, Flink APPLIED and a known active runtime state.', operationId, rule.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        rule.state = 'RETIRED';
        rule.lifecycleAction = 'RETIRE';
        rule.lifecycleSequence += 1;
        rule.lifecycleActive = false;
        rule.publicationStatus = 'PENDING';
        rule.publicationRevision = 1;
        rule.publicationAttemptCount = 0;
        rule.publicationTotalAttemptCount = 0;
        rule.publicationManualRetryCount = 0;
        rule.publicationPublishedAt = null;
        rule.publicationLastRequeuedAt = null;
        rule.publicationLastError = null;
        rule.applicationStatus = 'WAITING';
        rule.applicationDeploymentId = null;
        rule.applicationObservedAt = null;
        rule.applicationReceivedAt = null;
        rule.applicationErrorCode = null;
        rule.applicationErrorDetail = null;
        rule.runtimeReadinessStatus = 'WAITING';
        rule.runtimeReadinessDeploymentId = null;
        rule.runtimeReadinessObservedAt = null;
        rule.runtimeReadinessReceivedAt = null;
        rule.runtimeReadinessReasonCode = null;
        rule.runtimeReadinessDetail = null;
        rule.runtimePointCatalogEventId = null;
        rule.runtimePointCatalogSourceRevision = null;
        rule.revision += 1;
        return rememberAndSend(state, context, res, 200, envelope(operationId, rule), operationId);
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
